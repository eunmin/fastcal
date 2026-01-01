package com.fastcal.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoders;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.firewall.ServerWebExchangeFirewall;
import org.springframework.security.web.server.firewall.StrictServerWebExchangeFirewall;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.ReactiveAuthenticationManagerAdapter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

  @Value("${app.cors.allowed-origins:http://localhost:8080}")
  private String allowedOrigins;

  @Value("${actuator.admin.username:}")
  private String actuatorUsername;

  @Value("${actuator.admin.password:}")
  private String actuatorPassword;

  @Value("${oauth2.enabled:false}")
  private boolean oauth2Enabled;

  @Value("${oauth2.issuer-uri:}")
  private String oauth2IssuerUri;

  @Value("${ldap.enabled:true}")
  private boolean ldapEnabled;

  @Value("${ldap.url:ldap://localhost:389}")
  private String ldapUrl;

  @Value("${ldap.base-dn:dc=fastcal,dc=local}")
  private String ldapBaseDn;

  @Value("${ldap.user-dn-pattern:uid={0},ou=People}")
  private String ldapUserDnPattern;

  @Value("${ldap.manager-dn:}")
  private String ldapManagerDn;

  @Value("${ldap.manager-password:}")
  private String ldapManagerPassword;

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.of(allowedOrigins.split(",")));
    configuration.setAllowedMethods(List.of(
        "GET", "POST", "PUT", "DELETE", "OPTIONS",
        "PROPFIND", "REPORT", "MKCALENDAR", "PROPPATCH"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public LdapContextSource ldapContextSource() {
    LdapContextSource contextSource = new LdapContextSource();
    contextSource.setUrl(ldapUrl);
    contextSource.setBase(ldapBaseDn);
    if (!ldapManagerDn.isEmpty()) {
      contextSource.setUserDn(ldapManagerDn);
      contextSource.setPassword(ldapManagerPassword);
    }
    contextSource.afterPropertiesSet();
    return contextSource;
  }

  @Bean
  public LdapAuthenticationProvider ldapAuthenticationProvider(LdapContextSource contextSource) {
    BindAuthenticator authenticator = new BindAuthenticator(contextSource);
    authenticator.setUserDnPatterns(new String[]{ldapUserDnPattern});

    DefaultLdapAuthoritiesPopulator authoritiesPopulator =
        new DefaultLdapAuthoritiesPopulator(contextSource, "");
    authoritiesPopulator.setDefaultRole("ROLE_USER");
    authoritiesPopulator.setIgnorePartialResultException(true);

    return new LdapAuthenticationProvider(authenticator, authoritiesPopulator);
  }

  @Bean
  public ReactiveAuthenticationManager ldapReactiveAuthenticationManager(
      LdapAuthenticationProvider ldapAuthenticationProvider) {
    ProviderManager providerManager = new ProviderManager(ldapAuthenticationProvider);
    return new ReactiveAuthenticationManagerAdapter(providerManager);
  }

  @Bean
  public SecurityWebFilterChain securityFilterChain(
      ServerHttpSecurity http,
      ReactiveAuthenticationManager ldapReactiveAuthenticationManager,
      PasswordEncoder passwordEncoder) {

    ReactiveAuthenticationManager actuatorAuthManager = createActuatorAuthManager(passwordEncoder);

    DelegatingAuthenticationManager basicAuthManager =
        new DelegatingAuthenticationManager(ldapReactiveAuthenticationManager, actuatorAuthManager);

    http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .authorizeExchange(exchanges -> exchanges
            .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .pathMatchers("/.well-known/caldav", "/.well-known/caldav/").permitAll()
            .pathMatchers("/internal/actuator/health").permitAll()
            .pathMatchers("/internal/actuator/**").hasRole("ACTUATOR")
            .pathMatchers("/calendars/**", "/principals/**").authenticated()
            .anyExchange().authenticated());

    if (oauth2Enabled && !oauth2IssuerUri.isEmpty()) {
      http.oauth2ResourceServer(oauth2 -> oauth2
          .jwt(jwt -> jwt
              .jwtDecoder(jwtDecoder())
              .jwtAuthenticationConverter(jwtAuthenticationConverter())
          )
      );
    }

    http.httpBasic(httpBasic -> httpBasic
        .authenticationManager(basicAuthManager)
        .securityContextRepository(NoOpServerSecurityContextRepository.getInstance()));

    return http.build();
  }

  private ReactiveAuthenticationManager createActuatorAuthManager(PasswordEncoder passwordEncoder) {
    if (actuatorUsername.isEmpty() || actuatorPassword.isEmpty()) {
      return authentication -> Mono.empty();
    }

    ReactiveUserDetailsService actuatorUserDetailsService = username -> {
      if (actuatorUsername.equals(username)) {
        return Mono.just(
            User.withUsername(actuatorUsername)
                .password(passwordEncoder.encode(actuatorPassword))
                .roles("ACTUATOR")
                .build());
      }
      return Mono.empty();
    };

    return authentication -> actuatorUserDetailsService.findByUsername(authentication.getName())
        .filter(user -> passwordEncoder.matches(
            (String) authentication.getCredentials(), user.getPassword()))
        .map(user -> (Authentication) new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
            user, authentication.getCredentials(), user.getAuthorities()))
        .switchIfEmpty(Mono.error(new org.springframework.security.authentication.BadCredentialsException("Invalid credentials")));
  }

  @Bean
  public ReactiveJwtDecoder jwtDecoder() {
    if (oauth2Enabled && !oauth2IssuerUri.isEmpty()) {
      return ReactiveJwtDecoders.fromIssuerLocation(oauth2IssuerUri);
    }
    return token -> Mono.empty();
  }

  private ReactiveJwtAuthenticationConverterAdapter jwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setPrincipalClaimName("preferred_username");
    return new ReactiveJwtAuthenticationConverterAdapter(converter);
  }

  private static class DelegatingAuthenticationManager implements ReactiveAuthenticationManager {
    private final ReactiveAuthenticationManager primaryManager;
    private final ReactiveAuthenticationManager actuatorManager;

    DelegatingAuthenticationManager(
        ReactiveAuthenticationManager primaryManager,
        ReactiveAuthenticationManager actuatorManager) {
      this.primaryManager = primaryManager;
      this.actuatorManager = actuatorManager;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
      return actuatorManager.authenticate(authentication)
          .switchIfEmpty(primaryManager.authenticate(authentication))
          .onErrorResume(e -> primaryManager.authenticate(authentication));
    }
  }

  @Bean
  public ServerWebExchangeFirewall serverWebExchangeFirewall() {
    StrictServerWebExchangeFirewall firewall = new StrictServerWebExchangeFirewall();
    firewall.setAllowedHttpMethods(List.of(
        HttpMethod.GET, HttpMethod.HEAD, HttpMethod.POST, HttpMethod.PUT,
        HttpMethod.DELETE, HttpMethod.OPTIONS, HttpMethod.PATCH,
        HttpMethod.valueOf("PROPFIND"), HttpMethod.valueOf("PROPPATCH"),
        HttpMethod.valueOf("MKCALENDAR"), HttpMethod.valueOf("REPORT")
    ));
    return firewall;
  }
}
