> Repository role: `coldtrace-backend` is the brownfield application. Extracted
> services are owned by their independent repositories; `sprint1/` pins them as
> Git submodules for reproducible integration. Clone with `--recurse-submodules`.

# ColdTrace Platform

Spring Boot backend for the ColdTrace final project.

The project follows a layered, bounded-context backend architecture. The backend
exposes organization-scoped REST APIs for identity access, asset management,
monitoring, alerts, reports, and maintenance management while preserving
ColdTrace-specific deployment on Google Cloud Run and Cloud SQL.

## Technology Stack

- Java 26
- Spring Boot 4.0.6
- Spring Web MVC
- Spring Data JPA
- Bean Validation
- MySQL
- Lombok
- SpringDoc OpenAPI
- Google Cloud SQL Java Connector

## Architecture

The backend is organized around Spring Boot bounded contexts:

- Bounded contexts split into `domain`, `application`, `infrastructure`, and
  `interfaces`.
- Thin REST controllers with request/response resources and transform
  assemblers.
- Application command/query services returning shared result/error contracts.
- Domain repository contracts implemented by JPA persistence adapters.
- Localized REST errors through Spring message bundles.
- OpenAPI metadata and JWT bearer authentication configured in shared infrastructure.
- Architecture evidence under `docs`.

Security is handled by a dedicated IAM scope. Authentication uses BCrypt
password hashing, stateless JWT bearer tokens, a Spring Security filter chain,
and public access only for sign-in, organization bootstrap sign-up, and API
documentation routes. Business APIs remain organization-scoped through route
parameters while authenticated context is introduced.

The planned IAM scope also includes Google and Apple social login. Those
providers validate external identity only; ColdTrace must still link the
provider subject to a local user, organization, role, and JWT session. Provider
secrets and Apple private keys must stay in environment/provider-console
configuration, never in Angular source or committed files.

## Architecture Docs

The `docs` folder documents the ColdTrace backend architecture:

- `docs/software-architecture.dsl`: Structurizr C4 model for the ColdTrace
  solution.
- `docs/class-diagram.puml`: PlantUML class diagram focused on backend bounded
  contexts and shared components.
- `docs/user-stories.md`: API-facing technical stories and acceptance criteria.

## Branching

Use Git Flow for sprint work.

- `main`: stable release branch used by Cloud Run continuous deployment.
- `develop`: integration branch for completed sprint work.
- `feature/<scope>`: technical work without a Technical Story.
- `feature/TSxx-<scope>`: work linked to a Technical Story.
- `release/<version>`: release preparation branch from `develop` into `main`.

Release tags use the `v<major>.<minor>.<patch>` format.

## Run Locally

Local development uses MySQL and the `dev` profile.

Create or reuse a local MySQL database named:

```text
coldtrace-platform
```

Default local credentials are:

```properties
DATABASE_USER=root
DATABASE_PASSWORD=root
```

Run the backend locally:

```bash
cp .env.example .env.local
# Fill .env.local with your local provider-console values.
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Google and Apple authentication are disabled until provider client IDs are
configured. For local social-auth validation, keep provider-console values in
`.env.local`. The `dev` profile imports that file automatically, and Git ignores
it.

```properties
GOOGLE_OAUTH_CLIENT_ID=<google-web-client-id>
GOOGLE_OAUTH_CLIENT_SECRET=<google-web-client-secret>
GOOGLE_OAUTH_REDIRECT_URI=http://localhost:4200
APPLE_OAUTH_CLIENT_ID=<apple-services-id>
APPLE_OAUTH_REDIRECT_URI=<configured-apple-redirect-uri>
APPLE_TEAM_ID=<apple-team-id>
APPLE_KEY_ID=<apple-key-id>
APPLE_PRIVATE_KEY=<apple-private-key-p8-content-with-escaped-newlines>
```

For Apple, `APPLE_PRIVATE_KEY` is the `.p8` file content, not the local file
path. Convert it to a single-line value before pasting it into `.env.local`:

```bash
awk '{printf "%s\\n", $0}' /path/to/AuthKey_XXXXXXXXXX.p8
```

`APPLE_OAUTH_REDIRECT_URI` must match the return URL sent by the frontend and
registered in Apple Developer. Apple web return URLs must use HTTPS, so local
Apple testing usually uses the deployed Vercel frontend or an HTTPS tunnel.

The application starts on port `8080`.

Local Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

Local OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

Swagger authentication flow:

1. Execute `POST /api/v1/authentication/sign-in` with an existing ColdTrace
   user.
2. Copy the `token` value from the response.
3. Click **Authorize** in Swagger UI and paste only the raw JWT value in
   `bearerAuth`. Swagger sends it as `Authorization: Bearer <token>`.
4. Use the returned `organizationId` when trying organization-scoped endpoints.

Authentication, Swagger/OpenAPI, public subscription plans, and Stripe webhooks
are public. Business endpoints such as reports, incidents, assets, maintenance,
AI assistance, subscriptions, and billing management require Bearer JWT.

## AI Assistance Configuration

TS18 adds the Spring AI foundation used by future AI-assisted incident,
dashboard, and compliance use cases. The backend owns provider selection,
prompt templates, structured output conversion, validation, timeout handling,
and provider error mapping. Frontend applications must call ColdTrace backend
endpoints added by later tickets instead of calling AI providers directly.

Local development can use Ollama without an API token:

```bash
brew install ollama
ollama pull gemma3:4b

export AI_MODEL_PROVIDER=ollama
export AI_MODEL_NAME=gemma3:4b
export OLLAMA_BASE_URL=http://localhost:11434
export AI_REQUEST_TIMEOUT=30s
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Production or deployed academic environments can use OpenAI through the same
Spring AI abstraction:

```bash
export AI_MODEL_PROVIDER=openai
export AI_MODEL_NAME=gpt-5.4-mini
export OPENAI_API_KEY=<project-api-key>
export AI_REQUEST_TIMEOUT=30s
```

Keep `OPENAI_API_KEY` and any provider keys in environment variables or the
deployment secret manager. Do not commit provider keys, paste them in frontend
code, or include them in screenshots. If the configured provider is unavailable,
disabled, unsupported, times out, or returns invalid structured output, the AI
application service returns a controlled failure instead of exposing raw model
text.

AI report summaries are generated on demand from persisted report metrics and
related evidence. They are advisory and do not mutate source report metrics:

```bash
curl -X POST \
  http://localhost:8080/api/v1/organizations/{organizationId}/reports/{reportId}/ai-summary \
  -H "Authorization: Bearer <jwt>"
```

AI dashboard interpretations are generated on demand from persisted
organization metrics, readings, incidents, reports, and available maintenance
evidence. The request body is optional and can include a specific operator
question:

```bash
curl -X POST \
  http://localhost:8080/api/v1/organizations/{organizationId}/dashboard/ai-interpretation \
  -H "Authorization: Bearer <jwt>" \
  -H "Content-Type: application/json" \
  -d '{"question":"What should I review first?"}'
```

## Billing Plan Catalog

TS24 exposes a public, read-only catalog for Base, Operations, and Compliance AI:

```bash
curl -i http://localhost:8080/api/v1/subscription-plans
```

Expected result:

- `200 OK` without a bearer token.
- Three active plans ordered by monthly price.
- Each plan includes `code`, `displayName`, `monthlyPriceCents`, `currency`,
  optional `stripePriceId`, `usageLimits`, `featureFlags`, `recommended`, and
  `visible`.

Stripe price identifiers are optional configuration values, not hardcoded live
billing credentials:

```bash
export STRIPE_OPERATIONS_PRICE_ID=<stripe-test-price-id>
export STRIPE_COMPLIANCE_AI_PRICE_ID=<stripe-test-price-id>
```

## Organization Subscription and Entitlements

TS25 exposes the current subscription and backend-computed entitlements for one
organization:

```bash
curl -i http://localhost:8080/api/v1/organizations/1/subscription \
  -H "Authorization: Bearer <jwt>"
```

Expected result for an existing organization:

- `200 OK` with `status`, `provider`, the current `plan`, supported `usage`
  counters, and an `entitlements` list.
- New organizations are initialized on the Base plan during organization
  creation or sign-up.
- Existing organizations without a subscription are backfilled on application
  startup; if a subscription is still missing, the API returns `404`.
- Unknown organizations return `404`.
- If the stored plan code is not present in the plan catalog, the API returns a
  controlled `404` for the missing plan.

TS29 uses the same backend-computed entitlements before restricted writes or
paid AI calls. Direct API calls are rejected with `409 Conflict` when the
current plan does not allow the operation. The `ProblemDetail` body includes
`entitlementKey`, `planCode`, `subscriptionStatus`, `limit`, `used`,
`remaining`, `lockedReason`, and `requiredPlanCode` when those values are
available.

Current backend checks cover organization-scoped creation of locations, assets,
IoT devices, users, reports, maintenance schedules, technical service requests,
dashboard AI interpretation, AI incident guidance, and AI report summaries.

## Stripe Checkout Sessions

TS26 creates provider-hosted Stripe Checkout sessions for paid plan upgrades:

```bash
curl -i http://localhost:8080/api/v1/organizations/1/billing/checkout-sessions \
  -H "Authorization: Bearer <jwt>" \
  -H "Content-Type: application/json" \
  -d '{"targetPlanCode":"operations"}'
```

Required environment for a real test-mode Stripe redirect:

```bash
export STRIPE_SECRET_KEY=<stripe-test-secret-key>
export STRIPE_OPERATIONS_PRICE_ID=<stripe-test-price-id>
export STRIPE_COMPLIANCE_AI_PRICE_ID=<stripe-test-price-id>
export BILLING_CHECKOUT_SUCCESS_URL="http://localhost:4200/settings/billing?checkout=success&session_id={CHECKOUT_SESSION_ID}"
export BILLING_CHECKOUT_CANCEL_URL="http://localhost:4200/settings/billing?checkout=cancel"
```

Expected result:

- `200 OK` with `provider`, `sessionId`, `checkoutUrl`, and `targetPlanCode`.
- The backend creates the session in Stripe subscription mode and attaches
  `organizationId` and `targetPlanCode` metadata for the later webhook sync.
- The Base plan is rejected with `409` because it remains free and local.
- Paid plans without a configured Stripe price id return `409`.
- Missing Stripe secret key or redirect URLs return `503`.
- ColdTrace never receives card numbers; checkout is hosted by Stripe.

## Stripe Webhook Subscription Sync

TS27 synchronizes local organization subscriptions from signed Stripe webhooks.
The webhook endpoint is public for Stripe delivery but does not trust unsigned
JSON. It verifies the raw request body with `Stripe-Signature` and the backend
webhook signing secret before changing any ColdTrace subscription record:

```text
POST /api/v1/billing/stripe/webhooks
```

Required environment for local test-mode validation:

```bash
export STRIPE_WEBHOOK_SECRET=<stripe-webhook-signing-secret>
```

When using the Stripe CLI locally, forward events to the backend and copy the
printed `whsec_...` value into `STRIPE_WEBHOOK_SECRET`:

```bash
stripe listen --forward-to localhost:8080/api/v1/billing/stripe/webhooks
```

Handled events:

- `checkout.session.completed`
- `customer.subscription.updated`
- `customer.subscription.deleted`
- `invoice.paid`
- `invoice.payment_failed`

Expected behavior:

- Valid signed events update the organization subscription by Stripe customer,
  subscription id, checkout metadata, or configured price id.
- The backend records processed event ids, so repeated webhook deliveries are
  acknowledged without duplicate state mutations.
- Unsupported signed events are safely acknowledged as ignored.
- Missing or invalid `Stripe-Signature` returns `400`.
- Missing `STRIPE_WEBHOOK_SECRET` returns `503`.
- Checkout success in the frontend is only advisory until the webhook updates
  the local subscription state.

## Stripe Customer Portal Sessions

TS28 creates provider-hosted Stripe Customer Portal sessions so organization
administrators can manage payment methods, invoices, cancellations, and
subscription billing directly in Stripe instead of ColdTrace collecting payment
details:

```bash
curl -i -X POST http://localhost:8080/api/v1/organizations/1/billing/portal-sessions \
  -H "Authorization: Bearer <jwt>"
```

Required environment for a real test-mode portal redirect:

```bash
export STRIPE_SECRET_KEY=<stripe-test-secret-key>
export BILLING_CUSTOMER_PORTAL_RETURN_URL="http://localhost:4200/settings/billing?portal=return"
```

Expected result:

- `200 OK` with `provider`, `sessionId`, `portalUrl`, and `organizationId`.
- The organization must already have a Stripe customer id stored in its local
  subscription record.
- Free-plan organizations that have not completed Stripe Checkout return `409`.
- Missing Stripe secret key or portal return URL returns `503`.
- ColdTrace never creates custom invoice or card-management screens; Stripe
  Customer Portal owns that workflow.

## Production Deployment

The production backend is deployed on Google Cloud Run and uses Google Cloud SQL
for MySQL.

```text
Google Cloud project: coldtrace-499222
Cloud Run service: coldtrace-platform
Region: us-central1
Cloud SQL instance: coldtrace-mysql
Cloud SQL connection name: coldtrace-499222:us-central1:coldtrace-mysql
Database name: coldtrace-platform
Database user: coldtrace_app
Artifact Registry repository: coldtrace-docker
Docker image: us-central1-docker.pkg.dev/coldtrace-499222/coldtrace-docker/coldtrace-platform:latest
```

Production environment variables:

```properties
SPRING_PROFILES_ACTIVE=prod
DATABASE_NAME=coldtrace-platform
DATABASE_USER=coldtrace_app
DATABASE_PASSWORD=<cloud-sql-user-password>
INSTANCE_CONNECTION_NAME=coldtrace-499222:us-central1:coldtrace-mysql
JWT_SECRET=<at-least-32-byte-hs256-secret>
JWT_EXPIRATION_DAYS=7
GOOGLE_OAUTH_CLIENT_ID=<google-web-client-id>
GOOGLE_OAUTH_CLIENT_SECRET=<google-web-client-secret>
GOOGLE_OAUTH_REDIRECT_URI=<configured-google-origin-redirect-uri>
APPLE_OAUTH_CLIENT_ID=<apple-services-id>
APPLE_OAUTH_REDIRECT_URI=<configured-apple-redirect-uri>
APPLE_TEAM_ID=<apple-team-id>
APPLE_KEY_ID=<apple-key-id>
APPLE_PRIVATE_KEY=<apple-private-key-p8-content>
CORS_ALLOWED_ORIGIN_PATTERNS=https://coldtrace-frontend-liard.vercel.app,https://coldtrace-frontend-*.vercel.app,https://coldtrace-frontend-git-*-mauricio-pajes-projects.vercel.app
AI_MODEL_PROVIDER=openai
AI_MODEL_NAME=gpt-5.4-mini
OPENAI_API_KEY=<openai-project-api-key>
AI_REQUEST_TIMEOUT=30s
STRIPE_OPERATIONS_PRICE_ID=<stripe-test-price-id>
STRIPE_COMPLIANCE_AI_PRICE_ID=<stripe-test-price-id>
STRIPE_SECRET_KEY=<stripe-test-secret-key>
STRIPE_WEBHOOK_SECRET=<stripe-webhook-signing-secret>
BILLING_CHECKOUT_SUCCESS_URL=https://<frontend-domain>/settings/billing?checkout=success&session_id={CHECKOUT_SESSION_ID}
BILLING_CHECKOUT_CANCEL_URL=https://<frontend-domain>/settings/billing?checkout=cancel
BILLING_CUSTOMER_PORTAL_RETURN_URL=https://<frontend-domain>/settings/billing?portal=return
```

For Apple in production, `APPLE_PRIVATE_KEY` must be configured as a protected
environment variable or Secret Manager-backed variable containing the `.p8`
content, not as a filesystem path.

The production JDBC URL is configured in `application-prod.properties` and uses
the Cloud SQL Java Connector:

```properties
spring.datasource.url=jdbc:mysql:///${DATABASE_NAME}?cloudSqlInstance=${INSTANCE_CONNECTION_NAME}&socketFactory=com.google.cloud.sql.mysql.SocketFactory&cloudSqlRefreshStrategy=lazy&serverTimezone=UTC
```

DevOps configuration is documented in `docs/devops.md`. The repository includes:

- `Dockerfile` for the production Cloud Run container.
- `.dockerignore` for reproducible image contexts.
- `.env.example` for local environment setup without committed secrets.
- `.github/workflows/backend-ci.yml` for pull-request and branch CI.
- `.github/workflows/cloud-run-deploy.yml` for manual GitHub Actions deploys
  after Google Cloud Workload Identity secrets are configured.
- `cloudbuild.yaml` for reproducible Google Cloud Build deployments.

The existing Cloud Build trigger can keep using:

```text
Branch regex: ^main$
Build type: Dockerfile
Dockerfile location: /Dockerfile
```

For a manual Cloud Build deployment:

```bash
gcloud builds submit --config cloudbuild.yaml --project coldtrace-499222
```

Production API:

```text
https://coldtrace-platform-dtbzbm7bta-uc.a.run.app
```

Production Swagger UI:

```text
https://coldtrace-platform-dtbzbm7bta-uc.a.run.app/swagger-ui/index.html
```

Production OpenAPI JSON:

```text
https://coldtrace-platform-dtbzbm7bta-uc.a.run.app/v3/api-docs
```

In production Swagger UI, first call `POST /api/v1/authentication/sign-in`, then
use **Authorize** with the returned JWT before executing protected endpoints.
If a protected request returns `401`, verify that the token was added and that
the route uses the `organizationId` from the authenticated user response.

## Connect to Cloud SQL Locally

Install the Cloud SQL Auth Proxy:

```bash
brew install cloud-sql-proxy
```

Start the proxy from a terminal and keep it running while using MySQL Workbench:

```bash
env -u GOOGLE_APPLICATION_CREDENTIALS cloud-sql-proxy \
  --gcloud-auth \
  --quota-project coldtrace-499222 \
  --address 127.0.0.1 \
  --port 3307 \
  coldtrace-499222:us-central1:coldtrace-mysql
```

Use these MySQL Workbench values:

```text
Connection method: Standard TCP/IP
Hostname: 127.0.0.1
Port: 3307
Username: coldtrace_app
Password: cloud-sql user password
Default schema: coldtrace-platform
```

The `env -u GOOGLE_APPLICATION_CREDENTIALS` prefix avoids using credentials from
another Google Cloud project when this machine has that variable configured.

## Verification

Package the backend:

```bash
./mvnw -q -DskipTests package
```

AI resolution plan generation smoke check, replacing the ids with an open or
acknowledged incident that belongs to the organization:

```bash
curl -i -X POST http://localhost:8080/api/v1/organizations/1/incidents/1/ai-resolution-plans
```

Expected result for an existing active incident when the configured AI provider
returns valid structured output:

```text
HTTP/1.1 201
{
  "status": "pending",
  "summary": "...",
  "probableCause": "...",
  "recommendedSteps": [...],
  "correctiveActionDraft": "...",
  "resolutionNotesDraft": "..."
}
```

The generated plan is persisted as pending and the incident remains open or
acknowledged until a later human approval flow resolves it.

AI resolution plan approval smoke check, replacing the ids with a pending plan
that belongs to an open or acknowledged incident:

```bash
curl -i -X POST \
  http://localhost:8080/api/v1/organizations/1/incidents/1/ai-resolution-plans/1/approvals \
  -H 'Content-Type: application/json' \
  -d '{
    "approvedBy": "operations.manager@coldtrace.test",
    "finalCorrectiveAction": "Moved inventory to backup freezer and recalibrated the affected sensor.",
    "finalResolutionNotes": "Temperature returned to safe range after transfer and recalibration."
  }'
```

Expected result for a valid pending plan:

```text
HTTP/1.1 200
{
  "status": "approved",
  "approvedBy": "operations.manager@coldtrace.test",
  "finalCorrectiveAction": "...",
  "finalResolutionNotes": "..."
}
```

The incident is resolved by backend lifecycle rules during approval. A missing
plan should return `404`; an approved, rejected, or already resolved incident
approval should return `409` without applying a second resolution.

AI resolution plan rejection smoke check, replacing the ids with a pending plan
that belongs to an open or acknowledged incident:

```bash
curl -i -X POST \
  http://localhost:8080/api/v1/organizations/1/incidents/1/ai-resolution-plans/1/rejections \
  -H 'Content-Type: application/json' \
  -d '{
    "rejectedBy": "operations.manager@coldtrace.test",
    "rejectionReason": "Plan requires on-site compressor inspection before closure."
  }'
```

Expected result for a valid pending plan:

```text
HTTP/1.1 200
{
  "status": "rejected",
  "rejectedBy": "operations.manager@coldtrace.test",
  "rejectionReason": "..."
}
```

The incident remains open or acknowledged after rejection. A rejected or
approved plan should return `409` if a second decision is attempted.

AI resolution plan history smoke check:

```bash
curl -i http://localhost:8080/api/v1/organizations/1/incidents/1/ai-resolution-plans
```

Expected result for an existing incident with generated plans:

```text
HTTP/1.1 200
[
  {
    "status": "pending"
  }
]
```

An unknown incident or an incident from another organization should return
`404`. A resolved incident should return `409` for generation. Provider
timeouts, unavailable providers, and invalid structured output should return
`504`, `503`, and `502` respectively without creating partial plans.

AI smoke validation for this ticket requires packaging plus starting the backend
with either the local Ollama configuration or deployed OpenAI environment
variables.

The current repository does not add automated project tests yet, so release
verification is done with packaging, Swagger/OpenAPI checks, and API smoke flows
against the deployed Cloud Run service.

Production OpenAPI smoke check:

```bash
curl -I https://coldtrace-platform-dtbzbm7bta-uc.a.run.app/v3/api-docs
```

Expected result:

```text
HTTP/2 200
```

Protected route smoke check without a token:

```bash
curl -i https://coldtrace-platform-dtbzbm7bta-uc.a.run.app/organizations
```

Expected result:

```text
HTTP/2 401
```

CORS preflight smoke check:

```bash
curl -i -X OPTIONS https://coldtrace-platform-dtbzbm7bta-uc.a.run.app/organizations \
  -H "Origin: https://coldtrace-frontend.vercel.app" \
  -H "Access-Control-Request-Method: GET"
```

Social provider configuration smoke check:

```bash
curl -i -X POST http://localhost:8080/api/v1/authentication/social/google/token-exchange \
  -H "Content-Type: application/json" \
  -d '{"idToken":"invalid"}'
```

Expected result without `GOOGLE_OAUTH_CLIENT_ID`:

```text
HTTP/1.1 503
code: SOCIAL_PROVIDER_CONFIGURATION_MISSING
```

Expected result with provider configuration but an invalid token:

```text
HTTP/1.1 401
code: PROVIDER_VALIDATION_FAILED
```

Google success or onboarding smoke check:

```bash
export GOOGLE_AUTHORIZATION_CODE=<real-google-authorization-code-from-configured-client>
curl -i -X POST http://localhost:8080/api/v1/authentication/social/google/token-exchange \
  -H "Content-Type: application/json" \
  -d "{\"authorizationCode\":\"${GOOGLE_AUTHORIZATION_CODE}\",\"redirectUri\":\"http://localhost:4200\"}"
```

Expected result when the provider subject is already linked, or when the
verified provider email matches an existing ColdTrace user and can be linked:

```text
HTTP/1.1 200
```

Expected result when the provider token is valid but no ColdTrace user can be
linked:

```text
HTTP/1.1 422
code: SOCIAL_IDENTITY_REQUIRES_ONBOARDING
```

Password reset request smoke check:

```bash
curl -i -X POST http://localhost:8080/api/v1/password-reset-requests \
  -H "Content-Type: application/json" \
  -d '{"email":"david@coldtrace.example"}'
```

Expected result:

```text
HTTP/1.1 202
```

The response is intentionally generic and does not reveal whether the submitted
email belongs to a ColdTrace user. Existing users generate persisted reset
metadata with a hashed token only; the raw token and email delivery are not
exposed by this academic endpoint.

## Package Map

```text
com.acme.coldtrace.platform
|-- aiassistance
|-- iam
|-- assetmanagement
|-- monitoring
|-- alerts
|-- reports
|-- maintenancemanagement
`-- shared
```

Context responsibilities:

- `aiassistance`: Spring AI provider configuration, backend-owned prompt
  templates, structured advisory output, validation, timeout handling, and
  controlled provider failures.
- `iam`: organizations, users, roles, organization sign-up, email/password authentication, JWT sessions, password reset requests, and Google/Apple external identity links.
- `assetmanagement`: locations, gateways, assets, IoT devices, and asset settings.
- `monitoring`: sensor readings and random demo reading generation.
- `alerts`: incidents, notifications, acknowledgement, escalation, corrective action, and resolution.
- `reports`: operational reports.
- `maintenancemanagement`: maintenance schedules and technical service requests.
- `billing`: public subscription plan catalog, pricing metadata, and future organization subscription state.
- `shared`: reusable application, persistence, domain, and REST support.

Password reset requests are accepted through a public endpoint because the user
does not have a valid JWT during the recovery flow. The API returns a generic
accepted response and records only hashed token metadata for existing users;
email delivery and password update confirmation remain outside this ticket.
Authentication uses ColdTrace JWT sessions; business APIs still preserve
organization ownership through route parameters.

## API Overview

Identity access:

| Path | Operations |
| --- | --- |
| `/authentication/sign-in` | `POST` |
| `/authentication/social/{provider}/token-exchange` | `POST` |
| `/password-reset-requests` | `POST` |
| `/organization-sign-ups` | `POST` |
| `/organizations` | `GET`, `POST` |
| `/roles` | `GET` |
| `/organizations/{organizationId}/users` | `GET`, `POST` |
| `/organizations/{organizationId}/users/{userId}/role` | `PATCH` |

Asset management:

| Path | Operations |
| --- | --- |
| `/organizations/{organizationId}/locations` | `GET`, `GET /{locationId}`, `POST`, `PUT /{locationId}` |
| `/organizations/{organizationId}/gateways` | `GET`, `GET /{gatewayId}`, `POST`, `PUT /{gatewayId}` |
| `/organizations/{organizationId}/assets` | `GET`, `GET /{assetId}`, `POST`, `PUT /{assetId}` |
| `/organizations/{organizationId}/iot-devices` | `GET`, `GET /{iotDeviceId}`, `POST`, `PUT /{iotDeviceId}` |
| `/organizations/{organizationId}/asset-settings` | `GET` |
| `/organizations/{organizationId}/asset-settings/default` | `PUT` |
| `/organizations/{organizationId}/assets/{assetId}/settings` | `GET`, `PUT` |

Monitoring:

| Path | Operations |
| --- | --- |
| `/organizations/{organizationId}/sensor-readings` | `GET`, `GET /{sensorReadingId}`, `POST` |
| `/organizations/{organizationId}/sensor-readings/demo-generations` | `POST` |

Alerts:

| Path | Operations |
| --- | --- |
| `/organizations/{organizationId}/incidents` | `GET`, `GET /{incidentId}`, `POST` |
| `/organizations/{organizationId}/incidents/{incidentId}/acknowledgements` | `POST` |
| `/organizations/{organizationId}/incidents/{incidentId}/escalation` | `PATCH` |
| `/organizations/{organizationId}/incidents/{incidentId}/corrective-action` | `PATCH` |
| `/organizations/{organizationId}/incidents/{incidentId}/resolutions` | `POST` |
| `/organizations/{organizationId}/incidents/{incidentId}/ai-resolution-plans` | `GET`, `POST` |
| `/organizations/{organizationId}/incidents/{incidentId}/ai-resolution-plans/{planId}/approvals` | `POST` |
| `/organizations/{organizationId}/incidents/{incidentId}/ai-resolution-plans/{planId}/rejections` | `POST` |
| `/organizations/{organizationId}/incidents/{incidentId}/notifications` | `GET` |
| `/organizations/{organizationId}/notifications` | `GET` |

The incident routes are also available with the `/api/v1` prefix for the Sprint
4 AI contract, for example
`/api/v1/organizations/{organizationId}/incidents/{incidentId}/ai-resolution-plans`.

Reports:

| Path | Operations |
| --- | --- |
| `/organizations/{organizationId}/reports` | `GET`, `GET /{reportId}`, `POST` |
| `/organizations/{organizationId}/reports/{reportId}/ai-summary` | `POST` |

AI assistance:

| Path | Operations |
| --- | --- |
| `/organizations/{organizationId}/dashboard/ai-interpretation` | `POST` |

Billing:

| Path | Operations |
| --- | --- |
| `/api/v1/subscription-plans` | `GET` |
| `/api/v1/organizations/{organizationId}/subscription` | `GET` |
| `/api/v1/organizations/{organizationId}/billing/checkout-sessions` | `POST` |
| `/api/v1/organizations/{organizationId}/billing/portal-sessions` | `POST` |

The subscription plan catalog is public and read-only so the landing page can
show the same Base, Operations, and Compliance AI definitions as the app. The
same route is also available without the `/api/v1` prefix for local API
compatibility. Organization subscription responses are protected and include
the current plan, subscription status, supported usage counters, and backend
entitlement decisions for limits and paid features. Checkout sessions are
protected, use provider-hosted Stripe Checkout, and do not mutate the local
subscription until the later signed webhook synchronization ticket. Customer
Portal sessions are protected, require an existing Stripe customer id, and
return a temporary provider URL for self-service billing management.
Restricted backend operations also call the billing entitlement ACL before
persisting resources or invoking AI providers, returning `409 Conflict` with
plan-limit metadata when a direct API request exceeds the current plan.

Maintenance management:

| Path | Operations |
| --- | --- |
| `/organizations/{organizationId}/maintenance-schedules` | `GET`, `GET /{maintenanceScheduleId}`, `POST`, `PATCH /{maintenanceScheduleId}` |
| `/organizations/{organizationId}/technical-service-requests` | `GET`, `GET /{technicalServiceRequestId}`, `POST`, `PATCH /{technicalServiceRequestId}` |

## Telemetry Notes

Sensor readings can be created manually or generated internally for demo data.
The random generation endpoint is:

```text
POST /organizations/{organizationId}/sensor-readings/demo-generations
```

Each IoT device declares the values it supports in `measurementParameters`.
When creating a manual reading, only send fields supported by that device. For
example, a device that supports `temperature` and `humidity` should not receive
extra motion, image, battery, or signal values in the request body.
