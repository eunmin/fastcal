# fastcal

Spring WebFlux 기반의 CalDAV 서버

## 기술 스택

- **Framework**: Spring Boot 3.4, WebFlux (Netty)
- **Database**: PostgreSQL (R2DBC)
- **Cache**: Redis + Caffeine (L1/L2 캐시)
- **Resilience**: Resilience4j Circuit Breaker

## 실행

```bash
# 로컬 환경
docker-compose up -d
./gradlew bootRun 
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
