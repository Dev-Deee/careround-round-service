# careround-round-service

Ward rounds, patient reviews, clinical notes and emergency episodes. HTTP 8085, DB `careround_round_db`.

- **REST:** `/api/v1/rounds`, `/api/v1/reviews`, `/api/v1/notes`, `/api/v1/emergencies`.
- **Events (transactional outbox → Kafka, record key = outbox idempotency key):** `PATIENT_EMERGENCY_DECLARED`,
  `PATIENT_EMERGENCY_RESOLVED`, `PATIENT_REVIEW_COMPLETED`, each with `patientId`, `wardId`, `occurredAt`.
- **gRPC clients:** patient-service (`PatientExists`, gives the ward for emergency events), ward-service (`WardExists`
  on round creation, charge-nurse checks), care-task-service (pending task ids before a review resolves).
- Round creation verifies the ward (404 unknown, 503 unreachable). Declaring an emergency never blocks on a lookup:
  if patient-service is down the episode is still created and the event carries `wardId: null`.
- **Postman:** `postman/` (Emergency flow).

## Environment variables

Copy `.env.example` to `.env` (gitignored) and fill in real values. `main()` loads `.env` into system properties;
plain environment variables work too.

| Variable | Purpose |
|---|---|
| `DB_USERNAME` | Postgres user (root docker-compose: careround-postgres) |
| `DB_PASSWORD` | Postgres password |
| `JWT_PUBLIC_KEY` | RSA public key used to verify access tokens; same value in every service. PEM on one line, `|` for newlines |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka (root docker-compose: careround-kafka) |

## Run

Needs the root `docker-compose.yml` stack (Postgres, Kafka, Redis) and a `.env`.

```bash
./mvnw clean package -DskipTests && java -jar target/careround-round-service-0.0.1-SNAPSHOT.jar
```

Clients reach the REST API through the gateway on `http://localhost:8080`; the service port also works directly.
