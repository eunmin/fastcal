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

### 선택 (OAuth2)

| 변수명 | 설명 | 기본값 |
|--------|------|--------|
| `OAUTH2_ENABLED` | OAuth2 활성화 여부 | `false` |
| `OAUTH2_ISSUER_URI` | Keycloak issuer URI | |

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
