# TaskFlow Helm Chart

Enterprise deployment package for TaskFlow.

## Values Model

The default `values.yaml` is enterprise-safe: it does not create databases,
monitoring, or application secrets. Environment examples define the intended
shape:

- `values-dev.example.yaml` - bundled PostgreSQL and monitoring for dev/demo.
- `values-stage.example.yaml` - external databases and external secrets.
- `values-prod.example.yaml` - external databases, external secrets, stricter
  replicas/resources.

## Install

```bash
helm upgrade --install taskflow ./helm/taskflow \
  --namespace taskflow \
  --create-namespace \
  -f values-prod.yaml
```

Create a private values file from the example:

```bash
cp helm/taskflow/values-prod.example.yaml helm/taskflow/values-prod.yaml
```

`values-prod.yaml` is ignored by git.

The chart refuses to render with placeholder secrets by default. For local demos
only, you can explicitly opt in:

```yaml
global:
  allowInsecureDefaults: true
```

## Production Values

Keep real secrets outside git:

```yaml
secrets:
  create: false
  existingSecret: taskflow-secrets
```

The secret must contain:

- `JWT_SECRET`
- `INTERNAL_API_TOKEN`
- `AUTH_DB_PASSWORD`
- `USER_DB_PASSWORD`
- `TASK_DB_PASSWORD`
- `CLIENT_DB_PASSWORD`
- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`
- `APP_BOOTSTRAP_ADMIN_EMAIL`
- `APP_BOOTSTRAP_ADMIN_PASSWORD`

## Environment Deploys

Development/demo with bundled PostgreSQL and monitoring:

```bash
cp helm/taskflow/values-dev.example.yaml helm/taskflow/values-dev.yaml
helm upgrade --install taskflow ./helm/taskflow \
  --namespace taskflow-dev \
  --create-namespace \
  -f helm/taskflow/values-dev.yaml
```

Stage with organization-managed secrets and databases:

```bash
cp helm/taskflow/values-stage.example.yaml helm/taskflow/values-stage.yaml
helm upgrade --install taskflow ./helm/taskflow \
  --namespace taskflow-stage \
  --create-namespace \
  -f helm/taskflow/values-stage.yaml
```

Production:

```bash
cp helm/taskflow/values-prod.example.yaml helm/taskflow/values-prod.yaml
helm upgrade --install taskflow ./helm/taskflow \
  --namespace taskflow-prod \
  --create-namespace \
  -f helm/taskflow/values-prod.yaml
```

For CI/CD, store the private values file as a GitHub Environment Secret named
`HELM_VALUES_B64`:

```bash
base64 -w 0 helm/taskflow/values-prod.yaml
```

On Windows PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("helm/taskflow/values-prod.yaml"))
```

## GitHub OIDC Deploy

The enterprise workflow uses GitHub OIDC and Yandex Cloud Workload Identity
Federation instead of a long-lived kubeconfig secret. Configure these values per
GitHub Environment (`dev`, `stage`, `prod`):

Environment variables:

- `YC_CLOUD_ID`
- `YC_FOLDER_ID`
- `YC_K8S_CLUSTER_ID`
- `YC_K8S_ENDPOINT_FLAG` (`--external` for GitHub-hosted runners, `--internal`
  for self-hosted runners inside the private network)
- `YC_SERVICE_ACCOUNT_ID`
- `YC_OIDC_AUDIENCE`
- `YC_CLI_SHA256`

Environment secrets:

- `HELM_VALUES_B64`

The Yandex Cloud service account linked to the workload identity federation must
have only the roles required to read the target Kubernetes cluster credentials
and update resources in the target namespace. Protect the `prod` GitHub
Environment with required reviewers.

## External Databases

If the organization provides managed PostgreSQL:

```yaml
postgresql:
  enabled: false
externalDatabases:
  enabled: true
  auth:
    url: jdbc:postgresql://auth-db.example.com:5432/auth_db
    username: auth
```

Passwords are still read from the Kubernetes secret.

## Secret Sources

For enterprise deployments, create `taskflow-secrets` through the platform
secret manager, for example External Secrets Operator, Vault, Sealed Secrets, or
CI/CD environment secrets. The application chart should not contain real secret
values.

## Monitoring

Prometheus and Grafana are disabled by default. Enable bundled monitoring only
for dev/demo or standalone deployments:

```yaml
monitoring:
  enabled: true
```

When bundled Grafana is enabled in production, do not use chart defaults. Provide
a real `grafana-admin` Secret or override `monitoring.grafana.admin.password`
from a private values file.

## Network Policies

NetworkPolicy is enabled in the production example. Keep it aligned with the
organization's ingress controller namespaces and required external egress:

```yaml
networkPolicy:
  enabled: true
  externalDatabaseCidrs:
    - 10.0.0.0/8
```
