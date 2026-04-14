# CLAUDE.md — TopLinks Codebase Guide

This file provides guidance for AI assistants working on the TopLinks codebase.

---

## Project Overview

**TopLinks** is a Spring Boot web application for file upload, storage, and sharing. Key capabilities:

- Multi-file upload with optional AES-256 encryption
- Cloudflare R2 cloud storage with automatic local filesystem fallback
- Password-protected share links with download limits and expiration
- Google OAuth2 + local email/password authentication
- QR code generation for shares
- Share audit logging with IP-based rate limiting
- Scheduled cleanup of expired shares

---

## Technology Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.5.0 |
| Web / MVC | Spring MVC, Thymeleaf |
| Security | Spring Security, OAuth2 Client (Google) |
| ORM | MyBatis Plus 3.5.7 |
| Database | MySQL 8.0 |
| Cache | Spring Cache (simple in dev, Redis in prod) |
| Storage | Cloudflare R2 (AWS S3 SDK v2) + local fallback |
| Build | Maven 3.x |
| Utilities | Lombok, Apache Commons Lang3, ZXing (QR) |

---

## Repository Layout

```
toplinks/
├── src/
│   ├── main/
│   │   ├── java/com/yuz/toplinks/
│   │   │   ├── config/          # Spring beans, Security, Redis configs
│   │   │   ├── controller/      # MVC and REST controllers
│   │   │   ├── entity/          # JPA/MyBatis entities (Tlk*, SysUser)
│   │   │   ├── filter/          # Servlet filters (Cloudflare HTTPS detection)
│   │   │   ├── mapper/          # MyBatis Plus mapper interfaces
│   │   │   ├── model/           # Request/Response DTOs
│   │   │   ├── scheduler/       # Scheduled tasks
│   │   │   └── service/         # Business logic
│   │   └── resources/
│   │       ├── application.properties        # Base config (profile = local)
│   │       ├── application-local.properties  # Local dev (port 8081, simple cache)
│   │       ├── application-test.properties   # Test (port 8080, toplinks_test DB)
│   │       ├── application-prod.properties   # Production (Redis, HTTPS proxy)
│   │       ├── static/                       # CSS, JS, images
│   │       └── templates/                    # Thymeleaf HTML templates
│   └── test/java/com/yuz/toplinks/           # JUnit 5 tests
├── doc/
│   ├── create.sql                  # Full schema with seed categories
│   ├── migration_add_share_table.sql
│   ├── migration_add_share_audit_v2.sql
│   └── alter.sql                   # Miscellaneous schema changes
├── .github/workflows/ci.yml        # GitHub Actions CI
├── pom.xml
└── README.md                       # Chinese-language setup guide
```

---

## Build & Run

```bash
# Compile and package
mvn clean package

# Run locally (requires MySQL + toplinks DB)
mvn spring-boot:run

# Run with explicit profile
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local"
```

### Required Environment Variables

**Google OAuth2 (set to any value to disable the OAuth button in dev):**
```
GOOGLE_CLIENT_ID=<Cloud Console client ID>
GOOGLE_CLIENT_SECRET=<Cloud Console client secret>
```

**Cloudflare R2 (omit entirely to fall back to local `uploads/` dir):**
```
CLOUDFLARE_ACCOUNT_ID=<account ID>
CLOUDFLARE_R2_ACCESS_KEY=<R2 access key>
CLOUDFLARE_R2_SECRET_KEY=<R2 secret key>
CLOUDFLARE_R2_BUCKET=toplinks
CLOUDFLARE_R2_PUBLIC_URL=https://your-r2-domain.com
```

**Database (defaults shown; override for prod):**
```
DB_USERNAME=root
DB_PASSWORD=root
DB_URL=jdbc:mysql://localhost:3306/toplinks
```

**Redis (production only):**
```
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
```

---

## Testing

```bash
# Run all tests (requires MySQL with toplinks_test database)
mvn clean verify -Dspring.profiles.active=test

# Override DB credentials
DB_USERNAME=root DB_PASSWORD=root mvn clean verify -Dspring.profiles.active=test
```

### Test Database Setup

```sql
CREATE DATABASE toplinks_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- Schema is auto-applied via test profile Spring Boot startup
```

### Existing Test Files

| File | Purpose |
|---|---|
| `ToplinksApplicationTests` | Spring context smoke test |
| `config/SecurityConfigTest` | Security configuration tests |
| `service/CloudflareStorageServiceTest` | R2 storage service (mocked) |
| `service/FileServiceAutoDetectTest` | Auto category detection via reflection |
| `service/UserServiceTest` | User registration and OAuth handling |

CI runs on push/PR to `main` via `.github/workflows/ci.yml` using a MySQL 8.0 service container.

---

## Database Migrations

Apply SQL scripts in this order for a fresh install:

1. `doc/create.sql` — creates all tables and seeds default categories
2. `doc/migration_add_share_table.sql` — adds `TLK_SHARE`
3. `doc/migration_add_share_audit_v2.sql` — adds `TLK_SHARE_AUDIT_LOG`
4. `doc/alter.sql` — additional schema alterations

---

## Key Entities

All domain entities extend `BaseEntity` which provides:
- `createTime`, `updateTime` — auto-managed timestamps
- `createBy`, `updateBy` — audit fields
- `status` — enable/disable flag
- `deleted` — soft-delete flag (`@TableLogic`)

| Entity | Table | Description |
|---|---|---|
| `SysUser` | `SYS_USER` | User accounts (local + Google OAuth) |
| `TlkCategory` | `TLK_CATEGORY` | File categories with Bootstrap icons |
| `TlkFile` | `TLK_FILE` | File metadata (hash, UID, encryption flag, cloud URL) |
| `TlkShare` | `TLK_SHARE` | Share tokens with password/expiry/download limits |
| `TlkShareAuditLog` | `TLK_SHARE_AUDIT_LOG` | Per-access audit trail |

Entity name prefixes: `Tlk*` for domain entities, `Sys*` for system/infrastructure entities.

---

## Service Layer Conventions

- Business logic lives exclusively in `service/`; controllers are thin
- `@Transactional` on any service method that writes to DB
- `@Cacheable` / `@CacheEvict` annotations manage Spring Cache entries
- Cache region names: `categories`, `filesByCategory`, `fileByUid`, `users`
- Custom exceptions are thrown from services and caught by `GlobalControllerAdvice`
- Inject dependencies via constructor (Lombok `@RequiredArgsConstructor`)

### Key Services

| Service | Responsibility |
|---|---|
| `FileService` | Upload, MD5 deduplication, AES encryption/decryption, category auto-detection |
| `CloudflareStorageService` | R2 upload/download; falls back to `FileStorageService` |
| `ShareService` | Create/verify/invalidate shares, atomic download counter |
| `ShareAuditService` | Log access attempts, enforce IP rate limiting (5 failures/hour) |
| `UserService` | Registration, `UserDetailsService` impl, OAuth user provisioning |
| `CategoryService` | Cached category listing |
| `QrCodeService` | ZXing-based QR code PNG generation |

---

## Controller & Routing

### Web Pages (Thymeleaf)

| Path | Controller | Auth |
|---|---|---|
| `/` | `HomeController.index()` | Public |
| `/category/{id}` | `HomeController.category()` | Public |
| `/file/{uid}` | `FileController.fileDetail()` | Public |
| `/file/{uid}/download` | `FileController.downloadFile()` | Public |
| `/upload` | `FileController.uploadPage()` | Required |
| `/share/{token}` | `ShareController` | Public |
| `/auth/login` | `AuthController.loginPage()` | Public |
| `/auth/register` | `AuthController.registerPage()` | Public |

### REST API (`/api/...`)

| Method | Path | Auth |
|---|---|---|
| `GET` | `/api/categories` | Public |
| `POST` | `/api/share` | Required |
| `GET` | `/api/share?fileId={id}` | Required |
| `GET` | `/api/share/my` | Required |
| `GET` | `/api/share/{id}/audit` | Required |
| `GET` | `/api/share/{id}/stats` | Required |
| `DELETE` | `/api/share/{id}` | Required |
| `POST` | `/api/share/{id}/disable` | Required |

Use `@PreAuthorize("isAuthenticated()")` (not inline `hasRole`) on REST endpoints that require login.

---

## Security Configuration

Defined in `config/SecurityConfig.java`:

- Public paths: `/`, `/category/**`, `/file/**`, `/share/**`, `/auth/**`, `/static/**`, `/api/categories`
- Authenticated paths: `/upload`, `/api/share/**` (write ops)
- OAuth2 login via Google; success handler registers new OAuth users automatically
- BCrypt password encoding (bean in `AppConfig.java`)
- Custom filter `CloudflareHttpsFilter` detects HTTPS behind Cloudflare proxy using the `CF-Visitor` header

---

## Caching Strategy

Cache TTLs are configured in `config/RedisConfig.java`:

| Cache Name | TTL |
|---|---|
| `categories` | 10 minutes |
| `filesByCategory` | 2 minutes |
| `fileByUid` | 5 minutes |
| `users` | 30 minutes |

Local dev and test profiles use Spring's simple in-memory cache (no Redis required).

---

## File Encryption

When a user supplies an encryption password during upload:
1. A PBKDF2-derived AES-256 key is generated from the password
2. The file is encrypted with AES-256-CBC before upload
3. The encryption flag and key material are stored in `TlkFile`
4. Downloads decrypt on-the-fly using the stored key

---

## Coding Conventions

- **Naming:** `PascalCase` classes, `camelCase` methods/variables, `UPPER_SNAKE_CASE` constants
- **Entity prefixes:** `Tlk*` (domain), `Sys*` (system)
- **Lombok:** Use `@Data`, `@RequiredArgsConstructor`, `@Builder` — do not write boilerplate getters/setters manually
- **Null safety:** Check for null before operations; prefer `Optional` for nullable service returns
- **Comments/Javadoc:** Comments in the existing codebase are written in Chinese — maintain this convention
- **Pagination:** Controllers accept `page` (1-indexed) and `pageSize` query params; default page size is 12
- **Soft delete:** Never hard-delete domain records; set `deleted = true` or `status = inactive`
- **SQL:** Always use MyBatis Plus parameterized queries — never concatenate SQL strings

---

## Common Pitfalls

- The `uploads/` directory is git-ignored. It must exist locally for local storage to work.
- `application.properties` sets `spring.profiles.active=local` as the default — override explicitly in prod.
- Google OAuth requires real credentials; setting env vars to any non-empty value disables the OAuth UI button but won't break startup.
- Tests use a real MySQL instance (`toplinks_test`), not H2. Ensure MySQL is running before `mvn verify`.
- R2 storage silently falls back to local storage if `CLOUDFLARE_ACCOUNT_ID` is not set; check logs for the active storage backend.
- The scheduled cleanup job (`ShareCleanupScheduler`) runs every hour — do not rely on it for immediate share invalidation in tests.
