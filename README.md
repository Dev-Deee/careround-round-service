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

## Read side (rounds)

| Endpoint | Who | Notes |
|---|---|---|
| `GET /api/v1/rounds?wardId&doctorId&status&from&to&page&size` | ADMIN any; DOCTOR own; NURSE = ward's active charge nurse | `doctorId` matches the round's `conductedBy`. A doctor's `doctorId` defaults to themselves (another doctor's → 403). A nurse must give `wardId` (400) and be its active charge nurse (403; ward-service down → 503). `from`/`to` on `createdAt`, `from < to`. Sort `createdAt` desc (older rounds without it last), page/size 20, max 100, `{items, page, size, total}`. |
| `GET /api/v1/rounds/{publicId}/patients` | any authenticated | The round's reviews, one per patient (same items as `GET /api/v1/reviews?roundPublicId=`). |

**Round fields added:** `createdBy`, `conductedBy`, `createdAt`, `startedAt`, `endedAt`, `reviewCounts`
`{pending, suspended, resolved}` (carried-forward / discharged reviews are not counted). `conductedBy` is the
creator if a doctor; for a nurse-created round it is the first doctor to resolve a review; a carried-forward
round keeps the original round's doctor. `startedAt` = creation (rounds start `IN_PROGRESS`; there is no start
endpoint). `endedAt` is set when the round becomes `COMPLETED` (ended, or every review terminal).

**Ids.** Reviews add `roundPublicId`; `PATIENT_REVIEW_COMPLETED` adds `roundPublicId`. **Deprecated (kept):**
review `roundId` and the event's `roundId` (round internal id) → use `roundPublicId`. A note's `roundId` is the
round's **publicId**; create rejects an unknown one with 404.

**PRD path → built path.**

| PRD | Built |
|---|---|
| `GET /doctors/{id}/rounds` | `GET /api/v1/rounds?doctorId=` |
| `GET /rounds/{id}/patients` | `GET /api/v1/rounds/{publicId}/patients` |
| — (ward round history) | `GET /api/v1/rounds?wardId=` |

**Older rows** have null `created_by`, `conducted_by`, `created_at`, `started_at`, `ended_at`; they still list
(sorted last) but do not match `doctorId` or `from`/`to`. One-off backfill (not run):

```sql
-- careround_round_db: best-effort conductedBy from the resolving doctor of each round's reviews
UPDATE rounds r SET conducted_by = x.doc FROM (
  SELECT round_id, MIN(resolved_by_doctor_id::text)::uuid AS doc FROM round_patient_reviews
  WHERE resolved_by_doctor_id IS NOT NULL GROUP BY round_id) x
WHERE r.id = x.round_id AND r.conducted_by IS NULL;
-- notes whose round_id holds a round's internal id instead of its publicId (check before running)
UPDATE notes n SET round_id = r.public_id FROM rounds r WHERE n.round_id = r.id;
```

## Errors

Every error uses the body `{"status", "error", "message", "timestamp"}`.

- A path no endpoint maps (e.g. a collection root with a trailing slash, or a wrong sub-path) → **404**
  `No endpoint <METHOD> <path>`. A wrong HTTP method on a mapped path → 405, unsupported content type → 415,
  malformed JSON → 400.
- ward-service unreachable during a charge-nurse check (round end, round list) → 503.
- Anything unexpected → 500 with a generic message; the exception is logged at ERROR with its stack trace.

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
