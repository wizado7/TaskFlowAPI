# TaskFlow API

TaskFlow API is a microservice backend for task tracking, project boards, sprint/backlog workflows, CRM boards, users, notifications, file attachments, OAuth login, and real-time synchronization.

The repository also contains deployment assets for Docker Compose, Kubernetes, Helm, monitoring, and enterprise CI/CD.

## Stack

- Java 17
- Spring Boot 3.2
- Spring Security
- Spring Cloud Gateway
- PostgreSQL
- Flyway
- JWT
- OAuth2 Google Login
- WebSocket real-time updates
- Docker Compose
- Kubernetes and Kustomize
- Helm
- Prometheus and Grafana
- GitHub Actions CI/CD

## Services

| Service | Port | Responsibility |
| --- | ---: | --- |
| `api-gateway` | `8080` | Public API entry point, routing, JWT validation, Swagger aggregation in dev |
| `auth-service` | `8081` | Registration, login, refresh tokens, JWT issuing, Google OAuth2, admin bootstrap |
| `user-service` | `8082` | User profiles, avatars, notifications, internal user endpoints |
| `task-service` | `8083` | Projects, boards, columns, tasks, sprints, backlog, comments, attachments, WebSocket events |
| `client-service` | `8084` | CRM clients, CRM board workflow, comments, attachments |

All public API endpoints are versioned under:

```text
/api/v1
```

## Repository Layout

```text
api-gateway/          Spring Cloud Gateway
auth-service/         Authentication and OAuth2 service
user-service/         User profile and notification service
task-service/         Project, board and task service
client-service/       CRM client service
k8s/                  Kustomize base, overlays and external secret examples
helm/taskflow/        Enterprise Helm chart
.github/workflows/    CI/CD and branch policy workflows
.github/BRANCHING.md  Branching and environment promotion rules
docs/                 API and architecture notes
WEB_UI/               Local copy of the web frontend, ignored by git
Mob_app/              Local copy of the Flutter mobile app, ignored by git
```

## Local Requirements

- Docker Desktop
- Java 17
- Maven 3.9+
- Git

Optional:

- kubectl
- Helm
- Yandex Cloud CLI

## Local Configuration

Create a local `.env` from the example:

```bash
cp .env.example .env
```

On Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

Set at least these values before running locally:

```text
JWT_SECRET
INTERNAL_API_TOKEN
GOOGLE_CLIENT_ID
GOOGLE_CLIENT_SECRET
APP_BOOTSTRAP_ADMIN_EMAIL
APP_BOOTSTRAP_ADMIN_PASSWORD
```

Local database credentials may stay simple for development. Do not commit real secrets.

## Run Locally With Docker Compose

Build and start all backend services:

```bash
docker compose up -d --build
```

Check containers:

```bash
docker compose ps
```

View logs:

```bash
docker compose logs -f api-gateway auth-service user-service task-service client-service
```

Stop services:

```bash
docker compose down
```

Remove local database volumes only when you intentionally want to delete local data:

```bash
docker compose down -v
```

## Local API URLs

Use the API Gateway for frontend and mobile clients:

```text
http://localhost:8080
```

Useful local endpoints:

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/refresh
GET  /api/v1/users/me
GET  /api/v1/projects
GET  /api/v1/boards
GET  /api/v1/tasks
GET  /api/v1/clients
GET  /ws/task-events
```

Swagger UI is enabled only for the `dev` profile:

```text
http://localhost:8080/swagger-ui.html
```

## Build and Test

Run all backend tests:

```bash
mvn -B verify
```

Run a specific service with dependencies:

```bash
mvn -B -pl task-service -am test
```

## Authentication

TaskFlow uses JWT access tokens and refresh tokens.

JWT settings are shared by all backend services through `JWT_SECRET`. The signing and validation algorithm is fixed to `HS256` across services, so all services must receive the same secret.

Google OAuth2 login is configured through:

```text
GOOGLE_CLIENT_ID
GOOGLE_CLIENT_SECRET
GOOGLE_REDIRECT_URI
FRONTEND_OAUTH_SUCCESS_URL
```

Public registration is controlled by:

```text
PUBLIC_REGISTRATION_ENABLED
```

It is intended to be enabled in `dev` and disabled or tightly controlled in `stage` and `prod`.

## Internal Service Calls

Internal endpoints are protected with:

```text
INTERNAL_API_TOKEN
```

This token is used for service-to-service calls, for example project invite notifications from `task-service` to `user-service`.

## Files and Attachments

Current local storage directories are configurable:

```text
USER_AVATARS_DIR
TASK_ATTACHMENTS_DIR
CLIENT_ATTACHMENTS_DIR
```

For a production enterprise deployment, file storage should be moved to an object storage backed file service, with signed URLs and CDN integration.

## Real-Time Updates

Task and board synchronization uses WebSocket events through:

```text
/ws/task-events
```

Allowed WebSocket origins are configured with:

```text
WEBSOCKET_ALLOWED_ORIGIN_PATTERNS
```

## Branching Model

The repository uses three long-lived branches:

| Branch | Environment | Namespace |
| --- | --- | --- |
| `develop` | `dev` | `taskflow-dev` |
| `staging` | `stage` | `taskflow-stage` |
| `main` | `prod` | `taskflow-prod` |

Promotion flow:

```text
feature/*, fix/*, infra/*, dependabot/* -> develop -> staging -> main
```

See [.github/BRANCHING.md](.github/BRANCHING.md) for the full policy.

## CI/CD

The main workflow is:

```text
.github/workflows/enterprise-ci-cd.yml
```

It runs:

- branch and supply-chain policy checks;
- dependency review on pull requests;
- Maven build and tests;
- Kustomize render validation;
- Helm lint and template validation;
- repository vulnerability scan;
- Docker image build;
- SBOM and provenance generation on push;
- image vulnerability scan;
- deployment through Helm on push.

Deployment mapping:

```text
develop -> dev
staging -> stage
main    -> prod
```

The deployment job uses GitHub OIDC for Yandex Cloud access. It does not require a long-lived kubeconfig secret.

## GitHub Environment Configuration

Create GitHub Environments:

```text
dev
stage
prod
```

Required environment variables:

```text
YC_CLOUD_ID
YC_FOLDER_ID
YC_K8S_CLUSTER_ID
YC_K8S_ENDPOINT_FLAG
YC_SERVICE_ACCOUNT_ID
YC_OIDC_AUDIENCE
YC_CLI_SHA256
```

Required environment secrets:

```text
HELM_VALUES_B64
```

`HELM_VALUES_B64` is a base64-encoded private Helm values file for the target environment.

On Windows PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("helm/taskflow/values-prod.yaml"))
```

The `prod` environment should require manual approval.

## Kubernetes and Helm

Kustomize overlays:

```text
k8s/overlays/dev
k8s/overlays/stage
k8s/overlays/prod
```

Helm chart:

```text
helm/taskflow
```

Validate Kustomize:

```bash
kubectl kustomize k8s/overlays/dev
kubectl kustomize k8s/overlays/stage
kubectl kustomize k8s/overlays/prod
```

Deploy with Helm manually:

```bash
helm upgrade --install taskflow ./helm/taskflow \
  --namespace taskflow-dev \
  --create-namespace \
  -f helm/taskflow/values-dev.yaml
```

See [helm/taskflow/README.md](helm/taskflow/README.md) for detailed Helm configuration.

## Secrets Policy

Do not commit real secrets.

Allowed in git:

- `.env.example`
- `secret.example.yaml`
- Helm `values-*.example.yaml`
- ExternalSecret examples

Ignored by git:

- `.env`
- `.env.*`
- real Kubernetes secret manifests
- private Helm values files
- kubeconfig files

For stage and production, use GitHub Environment Secrets, Kubernetes Secrets created by the platform, External Secrets Operator, Vault, Sealed Secrets, or an organization-managed secret manager.

## Monitoring

The project includes Prometheus and Grafana configuration for Kubernetes and Helm.

Bundled monitoring is intended for development, demo, or standalone deployments. In production, monitoring credentials and persistence must be configured by the organization.

## Error Format

API errors use a shared response shape:

```json
{
  "timestamp": "2026-02-02T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation error",
  "path": "/api/v1/..."
}
```

## Production Notes

Before production release:

- keep the repository private;
- protect `develop`, `staging`, and `main`;
- require pull requests and CI checks;
- enable manual approval for `prod`;
- use external managed PostgreSQL for stage and production;
- store secrets outside git;
- configure ingress, TLS, DNS, backups, monitoring, log collection, and alerting;
- move file storage to object storage or a dedicated file service for CDN integration.
