# Food Waste Platform Backend

Spring Boot backend for the Food Waste Platform frontend.

## Stack
- Spring Boot 3
- Spring Security with JWT
- MySQL for users and signup OTPs
- JSON file persistence for donations, requests, content, settings, and backups

## API
The frontend can keep using the same base URL:

`http://localhost:4000/api`

Public routes:
- `GET /api/health`
- `POST /api/auth/request-signup-otp`
- `POST /api/auth/signup-with-otp`
- `POST /api/auth/signup`
- `POST /api/auth/login`

Protected routes:
- `GET /api/auth/me`
- `GET|POST|PATCH|DELETE /api/donations`
- `GET /api/requests`
- `GET /api/analytics/*`
- `GET|POST|PATCH|DELETE /api/content/*`
- `GET|PUT /api/settings/*`
- `GET|POST /api/admin/*`
- `GET|PATCH|DELETE /api/users/*`

## Run
1. Configure MySQL and run [`sql/schema.sql`](/C:/Users/rocks/OneDrive/Desktop/FCH/FRONTEND/FRONTEND/backend/sql/schema.sql).
2. Copy `.env.example` values into your environment.
3. Start with Maven:

```bash
mvn spring-boot:run
```

The current machine has Java installed, but Maven is not available on `PATH`, so local compilation was not completed in this session.
