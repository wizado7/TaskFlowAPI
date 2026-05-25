# TaskFlow Environment Overlays

These overlays are intended for branch-based enterprise delivery.

## Recommended Mapping

```text
develop/staging work can run in separate clusters or separate namespaces.
Production should usually be isolated in its own cluster.
```

```text
develop -> dev cluster   -> namespace taskflow-dev
staging -> stage cluster -> namespace taskflow-stage
main    -> prod cluster  -> namespace taskflow-prod
```

## Deploy

Development:

```bash
kubectl --context taskflow-dev apply -k k8s/overlays/dev
```

Stage:

```bash
kubectl --context taskflow-stage apply -k k8s/overlays/stage
```

Production:

```bash
kubectl --context taskflow-prod apply -k k8s/overlays/prod
```

## CI/CD Example

```text
develop branch -> kubectl --context taskflow-dev apply -k k8s/overlays/dev
staging branch -> kubectl --context taskflow-stage apply -k k8s/overlays/stage
main branch    -> kubectl --context taskflow-prod apply -k k8s/overlays/prod
```

## Secrets

The `dev` overlay includes `secret.example.yaml` for local/demo use.
It also includes `grafana-admin.example.yaml` with demo Grafana credentials.

For `stage` and `prod`, secrets are expected to come from External Secrets
Operator. The overlays reference a `ClusterSecretStore` named
`taskflow-cluster-secret-store`.

Create the store once per cluster, usually by the platform team:

```bash
kubectl apply -f k8s/external-secrets/cluster-secret-store.example.yaml
```

The store example uses Vault and must be adapted to the organization's provider.

If the organization does not use External Secrets Operator, create equivalent
Kubernetes secrets before applying:

```bash
kubectl --context taskflow-stage -n taskflow-stage create secret generic task-tracker-secrets \
  --from-literal=JWT_SECRET='...' \
  --from-literal=INTERNAL_API_TOKEN='...' \
  --from-literal=AUTH_DB_PASSWORD='...' \
  --from-literal=USER_DB_PASSWORD='...' \
  --from-literal=TASK_DB_PASSWORD='...' \
  --from-literal=CLIENT_DB_PASSWORD='...' \
  --from-literal=GOOGLE_CLIENT_ID='...' \
  --from-literal=GOOGLE_CLIENT_SECRET='...' \
  --from-literal=APP_BOOTSTRAP_ADMIN_EMAIL='...' \
  --from-literal=APP_BOOTSTRAP_ADMIN_PASSWORD='...'
```

Monitoring also expects a real Grafana secret when Grafana is deployed:

```bash
kubectl --context taskflow-prod -n taskflow-prod create secret generic grafana-admin \
  --from-literal=admin-user='admin' \
  --from-literal=admin-password='...'
```

The raw `prod` overlay removes bundled PostgreSQL and monitoring resources.
Update `k8s/overlays/prod/patch-configmap.yaml` with organization-managed
database endpoints and `patch-networkpolicy.yaml` with the real private DB CIDR.

## What Differs By Environment

- namespace;
- image tags;
- OAuth redirect URLs;
- infrastructure model: dev includes bundled PostgreSQL/monitoring, stage/prod
  use external database endpoints and external secrets;
- replica counts;
- resource requests/limits;
- production storage sizes;
- JWT expiration defaults.
