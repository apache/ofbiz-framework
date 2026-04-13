# GitHub CI/CD Configuration

## Workflows

- `gradle.yml`  
  Build and checks (style, Javadoc)  
  → Trigger: push / PR on `trunk` and `release*`

- `codeql-analysis.yml`  
  Security analysis (Java + JavaScript)  
  → Trigger: push / PR + weekly on `trunk` and `release*`

- `docker-image.yml`  
  Build and push images to `ghcr.io/apache/ofbiz`  
  → Trigger: push on `trunk` / `release*` + tags

- `dependency-review.yml`  
  Vulnerability scanning for dependencies in PRs  
  → Trigger: all PRs

- `scorecard.yml`  
  OpenSSF security scorecard  
  → Trigger: `trunk` + weekly

- `asf-allowlist-check.yml`
  Verifies all GitHub Actions refs are on the ASF allowlist
  → Trigger: push / PR on `.github/` path

### Workflow behavior

- `push` → uses the workflow from the target branch  
- `pull_request` → uses the workflow from the source branch  
- `schedule` → always uses `trunk`

Workflows are maintained on all branches (`trunk` and `release*`) using the same triggers.

New branches inherit workflow files from `trunk` at creation time.

`scorecard.yml` runs only on `trunk` (default branch).

## Dependabot

Read **only from `trunk`**.

Updates:
- GitHub Actions
- Docker base images
- Gradle dependencies
- NPM (`themes/common-theme/.../js`)

Each ecosystem includes:
- one configuration for `trunk`
- one configuration for each `release*` branch

## New release branch checklist

Before creating a new release branch from `trunk`, update `dependabot.yml` (on `trunk`) by adding a `target-branch` entry for:
- gradle
- npm
- github-actions
- docker

Then create the release branch.

Dependabot will automatically keep the new branch up to date.