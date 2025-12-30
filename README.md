# fastcal

Spring WebFlux 기반의 CalDAV 서버

## 기술 스택

- **Framework**: Spring Boot 3.4, WebFlux (Netty)
- **Database**: PostgreSQL (R2DBC)
- **Cache**: Redis + Caffeine (L1/L2 캐시)
- **Resilience**: Resilience4j Circuit Breaker

## 환경변수

### 필수

| 변수명 | 설명 | 예시 |
|--------|------|------|
| `DATABASE_URL` | R2DBC PostgreSQL URL | `r2dbc:postgresql://localhost:5432/fastcal` |
| `DATABASE_USERNAME` | DB 사용자명 | `fastcal` |
| `DATABASE_PASSWORD` | DB 비밀번호 | |
| `REDIS_HOST` | Redis 호스트 | `localhost` |
| `REDIS_PORT` | Redis 포트 | `6379` |
| `APP_CORS_ALLOWED_ORIGINS` | CORS 허용 도메인 | `https://cal.example.com` |

### LDAP 인증

| 변수명 | 설명 | 기본값 |
|--------|------|--------|
| `LDAP_ENABLED` | LDAP 활성화 여부 | `true` |
| `LDAP_URL` | LDAP 서버 URL | `ldap://localhost:389` |
| `LDAP_BASE_DN` | Base DN | `dc=fastcal,dc=local` |
| `LDAP_USER_DN_PATTERN` | 사용자 DN 패턴 | `uid={0},ou=People` |
| `LDAP_MANAGER_DN` | 관리자 DN | `cn=admin,dc=fastcal,dc=local` |
| `LDAP_MANAGER_PASSWORD` | 관리자 비밀번호 | `admin123` |

### 선택 (OAuth2 Resource Server)

| 변수명 | 설명 | 기본값 |
|--------|------|--------|
| `OAUTH2_ENABLED` | OAuth2 활성화 여부 | `false` |
| `OAUTH2_ISSUER_URI` | Keycloak issuer URI | |

**OAuth2 설정 시 주의사항:**

- JWT Resource Server로 동작하며, Bearer 토큰 인증을 지원합니다
- 사용자 식별에 `email` 클레임을 사용합니다 (LDAP 인증과 동일한 식별자)
- Keycloak 클라이언트 스코프에서 `email` 클레임이 JWT에 포함되도록 설정해야 합니다
- CalDAV 클라이언트는 OAuth2를 지원하지 않으므로, CalDAV 접근은 LDAP Basic Auth를 사용해야 합니다
- OAuth2는 웹 클라이언트나 API 접근용으로만 사용하세요

### 선택 (Actuator)

| 변수명 | 설명 |
|--------|------|
| `actuator.admin.username` | Actuator 인증 사용자명 |
| `actuator.admin.password` | Actuator 인증 비밀번호 |

## 실행

```bash
$ cp .env.example .env
# .env 파일에 환경변수 입력
$ docker-compose up -d
$ ./gradlew bootRun
```

## 지원 CalDAV 프로토콜

**RFC 4791 (CalDAV)** 기반

| 메서드 | 기능 |
|--------|------|
| `PROPFIND` | 캘린더/이벤트 속성 조회 |
| `PROPPATCH` | 속성 수정 |
| `MKCALENDAR` | 캘린더 생성 |
| `REPORT` | calendar-query, calendar-multiget, sync-collection |
| `GET/PUT/DELETE` | 이벤트 CRUD |

**DAV 지원**: 1, 2, 3, calendar-access, calendar-schedule, access-control


## 라이선스

Apache License 2.0
