# membership

Spring Boot + PostgreSQL membership REST service (`/api/memberships`) — tracks members, their class
type (yoga/pilates/strength), and one or more class schedules (daily/weekly, with a day-of-week and
time for weekly schedules). One of two application workloads running on the self-managed Kubernetes
cluster provisioned by [`infra`](https://github.com/sr-biker/infra) — see that repo's README for the
overall architecture, and [`contacts-micro-service`](https://github.com/sr-biker/-contacts-micro-service)
for the sibling app this one was scaffolded to mirror structurally.

## Where this fits

- **Runtime**: deployed to the prod cluster as the Argo CD `Application` `membership`, which renders
  `helm/membership` directly from this repo (`values-prod.yaml`) — Argo CD auto-syncs and self-heals
  on every push to `main`. Routes on the shared ingress controller under `/api/memberships`
  (path-scoped, not catch-all — `contacts-micro-service` already owns `/`; the path matches this app's
  own controller mapping exactly, so no rewrite-target annotation is needed).
- **Data**: its own database (`membership`) on the **same** shared PostgreSQL RDS instance
  `contacts-micro-service` uses (provisioned by `infra`'s `modules/rds`) — a separate database, not a
  separate RDS instance. Created once, by hand, via `psql` over SSM (no migration tool yet — see
  `schema.sql`, which is the one deliberate exception to `ddl-auto: validate` in prod).
- **Image**: built (arm64, matching the cluster's Graviton nodes) and pushed to the `infra`-provisioned
  ECR repo `membership`. `infra`'s `membership-pipeline` CodePipeline (`Source` → `Build` only — no
  Deploy stage, since Argo CD owns that; see `infra`'s README) builds this repo and publishes to ECR
  on push. Bumping this repo's own `values-prod.yaml` `image.tag` and pushing is what actually
  changes the deployed version — the pipeline building a new image doesn't do that by itself.
- **Credentials**: same command-override pattern as `contacts-micro-service` — no Kubernetes Secret,
  no CSI driver (no IRSA/EKS Pod Identity on this self-managed cluster). The container's own entrypoint
  fetches `DB_USERNAME`/`DB_PASSWORD` from the shared `/rds/postgres/credentials` Secrets Manager
  secret at startup, using the node's instance-profile credentials.
- **Image pulls**: via `imagePullSecrets`, refreshed every 6h by an in-cluster CronJob (same gap as
  `contacts-micro-service`: no `ecr-credential-provider` on kubelet).
- **Auth**: write endpoints (`POST`/`PUT`/`DELETE` on `/api/memberships`) require a valid JWT issued by
  Keycloak (`infra`'s `helm/keycloak`, realm `apps`); `GET` and `/actuator/**` stay open regardless
  (k8s health probes hit `/actuator/**` without a token). See `src/main/java/com/senthil/membership/config/SecurityConfig.java`
  — enforcement is entirely driven by whether `spring.security.oauth2.resourceserver.jwt.issuer-uri`
  is set (unset in local `docker-compose`, so nothing's enforced there; set via Helm values in
  `kind`/prod). The autoconfigured `JwtDecoder` is lazy (`SupplierJwtDecoder`), so an unreachable
  Keycloak doesn't crash the app at startup — confirmed directly, not assumed.

## Data model

- `Membership`: `memberName`, `email`, `classType` (`YOGA` / `PILATES` / `STRENGTH`), and a list of
  `Schedule`s (cascaded, one-to-many).
- `Schedule`: `frequency` (`DAILY` / `WEEKLY`), `dayOfWeek` (only meaningful for `WEEKLY`), `time`.

Note: `schedules` is eagerly fetched (not the JPA default `LAZY`) — `open-in-view` is deliberately
`false` (see `application.yml`), and this is a REST controller returning entities directly, so Jackson
serializes the response *after* the transaction/session has already closed. `@Transactional` on the
controller does **not** fix this (the proxy commits before response serialization runs); eager fetch
was the actual fix, found by testing locally against real Postgres before it shipped to prod.

## Local development

```bash
docker compose up --build
curl http://localhost:8080/api/memberships
```

`docker-compose.yml` runs the app against a local Postgres, no AWS calls.

## Deploying a change

Push to `main`, bump `helm/membership/values-prod.yaml`'s `image.tag` to a newly-built-and-pushed ECR
tag, push again. Argo CD auto-syncs within its poll interval (or force it via
`argocd.argoproj.io/refresh=hard` annotation over SSM — see `infra`'s CLAUDE.md for the exact command
pattern, since there's no direct network path to the cluster).
