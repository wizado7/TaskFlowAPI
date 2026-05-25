# Branching and Environments

The repository uses a promotion flow with three long-lived branches:

| Branch | Environment | Namespace | Purpose |
| --- | --- | --- | --- |
| `develop` | `dev` | `taskflow-dev` | Integration and daily testing |
| `staging` | `stage` | `taskflow-stage` | Pre-production acceptance testing |
| `main` | `prod` | `taskflow-prod` | Production releases |

## Normal Flow

1. Create work branches from `develop`:

   ```bash
   git checkout develop
   git pull
   git checkout -b feature/short-description
   ```

2. Open a pull request into `develop`.
3. After merge, GitHub Actions deploys `develop` to `dev`.
4. Promote tested changes by opening a pull request from `develop` into `staging`.
5. After merge, GitHub Actions deploys `staging` to `stage`.
6. Promote release-ready changes by opening a pull request from `staging` into `main`.
7. After merge, GitHub Actions deploys `main` to `prod`.

## Hotfix Flow

Critical fixes can use `hotfix/*` branches:

```bash
git checkout main
git pull
git checkout -b hotfix/short-description
```

Open the hotfix pull request into `main`. After production is fixed, merge or cherry-pick
the same hotfix back into `staging` and `develop`.

## Required GitHub Settings

Create GitHub Environments:

- `dev`
- `stage`
- `prod`

Configure each environment with its own variables and secrets. The `prod` environment
should require manual approval before deployment.

Enable branch protection for `develop`, `staging` and `main`:

- Require pull requests before merging.
- Require status checks to pass.
- Require the `Branch policy` workflow.
- Require the `Enterprise CI/CD` workflow checks.
- Restrict direct pushes to `main`.
- Disable force pushes.

The branch policy workflow enforces this promotion path:

```text
feature/*, fix/*, chore/*, docs/*, refactor/*, test/*, infra/*, dependabot/* -> develop
develop, release/*, hotfix/* -> staging
staging, hotfix/* -> main
```
