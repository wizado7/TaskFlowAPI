# TaskFlow Kubernetes Manifests

This directory contains Kustomize manifests for enterprise-style delivery:
a shared `base` plus environment overlays for separate branch clusters.

For production deployments, prefer the Helm chart in `helm/taskflow`.

## Structure

```text
k8s/base             shared application manifests without bundled infrastructure
k8s/bundles/dev      optional dev/demo PostgreSQL and monitoring bundle
k8s/overlays/dev     develop branch / dev cluster
k8s/overlays/stage   staging branch / stage cluster
k8s/overlays/prod    main branch / prod cluster
```

## Deploy

```bash
kubectl --context taskflow-dev apply -k k8s/overlays/dev
kubectl --context taskflow-stage apply -k k8s/overlays/stage
kubectl --context taskflow-prod apply -k k8s/overlays/prod
```

Recommended mapping:

```text
develop -> dev cluster / taskflow-dev namespace
staging -> stage cluster / taskflow-stage namespace
main    -> prod cluster / taskflow-prod namespace
```

## Secrets

The `dev` overlay includes `secret.example.yaml` with placeholders for local/demo
use. `stage` and `prod` use External Secrets Operator manifests and expect the
organization to provide a `ClusterSecretStore` named
`taskflow-cluster-secret-store`.

An example Vault-backed store is available at:

```text
k8s/external-secrets/cluster-secret-store.example.yaml
```

Do not commit real secrets.

## Production Shape

The `prod` overlay is intended for enterprise deployment:

- application services run in Kubernetes;
- PostgreSQL is external or managed by the organization;
- bundled Prometheus/Grafana are removed from the raw prod overlay;
- app secrets are materialized through External Secrets Operator;
- ingress, TLS, and centralized monitoring are expected to be owned by the
  platform team.

## Bundle Policy

Bundled PostgreSQL and monitoring are enabled only by the `dev` overlay through
`k8s/bundles/dev`. `stage` and `prod` overlays use external database endpoints
and externalized secrets.

## Organization Deployment Notes

Platform teams usually override:

- image registry and image tags;
- namespace and RBAC;
- ingress class, TLS, and domains;
- storage class and PVC sizes;
- secrets through Vault, External Secrets, Sealed Secrets, or CI variables;
- Prometheus/Grafana integration if centralized monitoring already exists.
