package com.fastcal.config;

import com.fastcal.domain.model.vo.Email;
import com.fastcal.domain.repository.UserRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.firewall.ServerWebExchangeFirewall;
import org.springframework.security.web.server.firewall.StrictServerWebExchangeFirewall;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

  @Value("${app.cors.allowed-origins:http://localhost:8080}")
  private String allowedOrigins;

  @Value("${actuator.admin.username:}")
  private String actuatorUsername;

  @Value("${actuator.admin.password:}")
  private String actuatorPassword;

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
  public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http,
      ReactiveUserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {

    UserDetailsRepositoryReactiveAuthenticationManager authManager =
        new UserDetailsRepositoryReactiveAuthenticationManager(userDetailsService);
    authManager.setPasswordEncoder(passwordEncoder);

    UserDetailsRepositoryReactiveAuthenticationManager actuatorAuthManager =
        new UserDetailsRepositoryReactiveAuthenticationManager(actuatorUserDetailsService(passwordEncoder));
    actuatorAuthManager.setPasswordEncoder(passwordEncoder);

    return http
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .httpBasic(httpBasic -> httpBasic
            .authenticationManager(new DelegatingAuthenticationManager(authManager, actuatorAuthManager))
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance()))
        .authorizeExchange(exchanges -> exchanges
            .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .pathMatchers("/.well-known/caldav", "/.well-known/caldav/").permitAll()
            .pathMatchers("/internal/actuator/health").permitAll()
            .pathMatchers("/internal/actuator/**").hasRole("ACTUATOR")
            .pathMatchers("/calendars/**", "/principals/**").authenticated()
            .anyExchange().authenticated())
        .build();
  }

  private ReactiveUserDetailsService actuatorUserDetailsService(PasswordEncoder passwordEncoder) {
    if (actuatorUsername.isEmpty() || actuatorPassword.isEmpty()) {
      return username -> reactor.core.publisher.Mono.empty();
    }
    return username -> {
      if (actuatorUsername.equals(username)) {
        return reactor.core.publisher.Mono.just(
            User.withUsername(actuatorUsername)
                .password(passwordEncoder.encode(actuatorPassword))
                .roles("ACTUATOR")
                .build());
      }
      return reactor.core.publisher.Mono.empty();
    };
  }

  private static class DelegatingAuthenticationManager implements org.springframework.security.authentication.ReactiveAuthenticationManager {
    private final UserDetailsRepositoryReactiveAuthenticationManager primaryManager;
    private final UserDetailsRepositoryReactiveAuthenticationManager actuatorManager;

    DelegatingAuthenticationManager(
        UserDetailsRepositoryReactiveAuthenticationManager primaryManager,
        UserDetailsRepositoryReactiveAuthenticationManager actuatorManager) {
      this.primaryManager = primaryManager;
      this.actuatorManager = actuatorManager;
    }

    @Override
    public reactor.core.publisher.Mono<org.springframework.security.core.Authentication> authenticate(
        org.springframework.security.core.Authentication authentication) {
      return actuatorManager.authenticate(authentication)
          .onErrorResume(e -> primaryManager.authenticate(authentication));
    }
  }

  @Bean
  public ReactiveUserDetailsService userDetailsService(UserRepository userRepository) {
    return email -> userRepository.findByEmail(Email.of(email))
        .filter(com.fastcal.domain.model.User::isEnabled)
        .map(user -> User.withUsername(user.getEmail().getValue())
            .password(user.getPassword().getValue())
            .roles("USER")
            .build())
        .switchIfEmpty(reactor.core.publisher.Mono.error(
            new UsernameNotFoundException("User not found: " + email)));
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
