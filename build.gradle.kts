plugins {
    id("org.springframework.boot") version "3.4.0"
    id("io.spring.dependency-management") version "1.1.6"
    java
}

group = "com.fastcal"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2024.0.0")
    }
}

dependencies {
    // Spring WebFlux (Netty)
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // R2DBC PostgreSQL
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.postgresql:r2dbc-postgresql:1.0.4.RELEASE")
    implementation("org.postgresql:postgresql")

    // Redis (Reactive)
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")

    // Caffeine Cache
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    // iCal4j for iCalendar parsing
    implementation("org.mnode.ical4j:ical4j:3.2.14")

    // XML Processing (StAX - Woodstox for high performance)
    implementation("com.fasterxml.woodstox:woodstox-core:6.5.1")

    // Jackson for JSON
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml")

    // Validation
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Security
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    // LDAP
    implementation("org.springframework.security:spring-security-ldap")
    implementation("com.unboundid:unboundid-ldapsdk:7.0.0")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")

    // Actuator for health check
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Rate Limiting
    implementation("com.bucket4j:bucket4j-core:8.7.0")

    // Resilience4j Circuit Breaker (Reactive)
    implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-reactor-resilience4j")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.springframework.security:spring-security-test")

    // Testcontainers
    testImplementation("org.testcontainers:testcontainers:1.19.8")
    testImplementation("org.testcontainers:junit-jupiter:1.19.8")
    testImplementation("org.testcontainers:postgresql:1.19.8")
    testImplementation("org.testcontainers:r2dbc:1.19.8")
}

tasks.withType<Test> {
    useJUnitPlatform()
    // Docker 29.0.0+ 호환성을 위한 API 버전 설정
    systemProperty("api.version", "1.44")
}
