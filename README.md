# ELRICS Patient Management Portal

A hospital patient-management web app: a static HTML/CSS/vanilla-JS frontend (unchanged look and feel) backed by a
**Java 17 / Spring Boot 3 / MySQL 8** REST API with JWT authentication.

> Originally an assessment that asked for Django + DRF. By explicit request the backend is implemented in
> Java/Spring Boot instead; the business requirements and the frontend are preserved.

```
.
├── (frontend)          login.html, register.html, dashboard.html, patients.html, css/, js/, assets/
└── backend/            Spring Boot application (pom.xml, src/, .env.example, db/setup.sql)
```

## Technology stack

| Area | Choice |
|---|---|
| Language / runtime | Java 17 (Temurin 17.0.17+10) |
| Framework | Spring Boot 3.3.5 (Web, Data JPA, Security, Validation) |
| Build | Maven 3.9+ |
| Database | MySQL 8.x, Spring Data JPA / Hibernate, Flyway migrations |
| Auth | Spring Security, stateless JWT access token (jjwt 0.12.6), BCrypt |
| API docs | springdoc-openapi (Swagger UI) |
| Tests | JUnit 5, Mockito, Spring Boot Test / MockMvc |
| Frontend | HTML5, CSS3, ES-module vanilla JS (no build step) |

## Requirements

* JDK 17 (`java -version`) and Maven 3.9+ (`mvn -v`)
* MySQL 8.x running locally
* Any static web server for the frontend (Python is enough)

## 1. Database setup

```bash
# edit the password in backend/db/setup.sql first (CHANGE_ME), then:
mysql -u root -p < backend/db/setup.sql
```

This creates database `elrics_portal` and user `elrics_user`. Tables are created by Flyway on first start
(`backend/src/main/resources/db/migration/V1__init_schema.sql`). With `createDatabaseIfNotExist=true` the application
can also create the empty database itself if its MySQL user has the privilege.

## 2. Configuration

```bash
cd backend
cp .env.example .env      # then edit .env
```

| Variable | Purpose | Default |
|---|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME` | MySQL location | `localhost`, `3306`, `elrics_portal` |
| `DB_USERNAME` | MySQL user | `root` |
| `DB_PASSWORD` | MySQL password | **required** |
| `JWT_SECRET` | HMAC signing key, ≥ 32 characters (`openssl rand -base64 48`) | **required** |
| `JWT_EXPIRATION_MS` | Access-token lifetime | `3600000` (1 h) |
| `SERVER_PORT` | API port | `8080` |
| `CORS_ALLOWED_ORIGINS` | Comma-separated frontend origins | `localhost`/`127.0.0.1` on 8000, 5500, 3000 |
| `HIBERNATE_DDL_AUTO` | Schema handling | `validate` |
| `SWAGGER_ENABLED` | Expose Swagger UI | `true` |

`.env` is git-ignored. Real environment variables work too and take precedence. **Never commit secrets.**

## 3. Run the backend

```bash
cd backend
mvn spring-boot:run          # development
# or
mvn clean package && java -jar target/patient-portal-1.0.0.jar
```

API: <http://localhost:8080/api> · Swagger UI: <http://localhost:8080/swagger-ui.html>

## 4. Run the frontend

Serve the repository root over HTTP (opening the files with `file://` will not work because of ES modules and CORS):

```bash
python3 -m http.server 8000      # from the repository root
```

Open <http://localhost:8000/login.html>. The API base URL is set in `js/utils/constants.js`
(`http://localhost:8080/api`, or define `window.ELRICS_API_BASE_URL` before the scripts load). If you serve the frontend
from another origin/port, add it to `CORS_ALLOWED_ORIGINS`.

## Initial administrator & authentication flow

1. Open `register.html` ("Create Administrator Account") and register. **Only the first registration succeeds**; every
   later attempt gets HTTP 403. This is enforced in the service layer and by a unique constraint in MySQL.
2. Log in on `login.html` with the **username or email** and password → the API returns a JWT.
3. The frontend stores the token (`sessionStorage`, or `localStorage` when "Remember me" is ticked) and sends
   `Authorization: Bearer <token>` on every protected call.
4. A 401 (expired/invalid token) clears the stored session and redirects to the login page. Logout clears the token.
   Pages `dashboard.html` / `patients.html` redirect to login when no token exists; a browser refresh keeps the session.

## REST API

All routes except register/login/Swagger require a valid token. Errors always look like
`{ "timestamp", "status", "error", "message", "errors": {field: message}, "path" }`.

| Method & path | Description | Success |
|---|---|---|
| `POST /api/auth/register` | Create the initial admin (`fullName, email, username, password`) | 201 (403 if an admin exists) |
| `POST /api/auth/login` | `{username, password}` (username or email) → `{token, tokenType, expiresInSeconds, user}` | 200 (401 bad credentials) |
| `GET /api/auth/me` | Current administrator | 200 |
| `POST /api/patients` | Create patient | 201 (400 invalid, 409 duplicate) |
| `GET /api/patients` | List all patients, newest first | 200 |
| `GET /api/patients/{id}` | Patient details | 200 (404) |
| `PUT /api/patients/{id}` | Update patient | 200 (400, 404, 409) |
| `DELETE /api/patients/{id}` | Delete patient | 204 (404) |
| `GET /api/patients/search?query=` | Match name, patient ID, phone or email | 200 |
| `GET /api/dashboard/stats` | `totalPatients, todaysAdmissions, pendingReviews, dischargedToday, currentlyAdmitted, recentActivity[]` | 200 |

Missing/invalid token → **401**; authenticated without the `ADMIN` role → **403**.

**Patient fields** (match the form in `patients.html`): `fullName*, age* (0-150), gender* (Male|Female|Other),
phone*, email, address, bloodGroup* (A+…O-), status (Pending|Admitted|Under Review|Discharged, default Pending),
emergencyContact`. Responses use `id` = the generated public identifier (`PT-000001`), which is also the `{id}` in the paths.
Duplicates are rejected on email and on the same full name + phone.

## Testing

```bash
cd backend
mvn clean test       # unit tests + (when MySQL is reachable) the end-to-end API test
mvn clean package    # build the runnable jar (also runs the tests)
```

* Unit tests (no database needed): `JwtServiceTest`, `AuthServiceTest`, `PatientServiceTest`.
* `ApiIntegrationTest` exercises the full flow (first admin only once, login, wrong password, 401s, patient CRUD, search,
  dashboard stats) against a real MySQL schema. It uses database `elrics_portal_test` and the `TEST_DB_HOST/PORT/NAME/USERNAME/PASSWORD`
  variables (defaults: `localhost:3306`, user `root`, empty password) and is **skipped automatically if MySQL is not reachable**.

## Assumptions & known limitations

* The frontend logs in and registers with a `username`, so users have `username` + `email` (both unique); login accepts either.
* The patient form has no diagnosis field; `status` (Pending / Admitted / Under Review / Discharged) is the medical-state field.
  "Today's admissions/discharges" are derived from timestamps recorded when a patient's status changes to Admitted/Discharged.
* Patient lists are returned unpaginated (the frontend renders the full table).
* Logout is client-side (stateless JWT, no server-side revocation list); tokens are valid until they expire. There is no refresh-token flow.
* No login rate-limiting/lockout is implemented.
* The token lives in browser storage, as the static frontend allows; keep the frontend free of XSS (all dynamic values are escaped).
* Use HTTPS and a strong `JWT_SECRET` in any real deployment; consider turning Swagger off (`SWAGGER_ENABLED=false`).
