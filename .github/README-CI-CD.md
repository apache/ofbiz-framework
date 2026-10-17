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

- `pr-type-label.yml`
  Sets the type label (`bug`, `enhancement`, `feature`, `documentation`) of a PR from its title prefix (Fixed, Improved, Implemented, Documented), so that `release.yml` can group the release notes
  → Trigger: PR opened, reopened or retitled on `trunk` and `release*` + manual run to label merged PRs

### Workflow behavior

- `push` → uses the workflow from the target branch  
- `pull_request` → uses the workflow from the source branch  
- `pull_request_target` → always uses the workflow from `trunk` (default branch)
- `schedule` → always uses `trunk`

Workflows are maintained on all branches (`trunk` and `release*`) using the same triggers.

New branches inherit workflow files from `trunk` at creation time.

`scorecard.yml` runs only on `trunk` (default branch).

## Release notes

`release.yml` configures the release notes GitHub generates: PRs are grouped by their type label, and Dependabot PRs and PRs labeled `dependencies` are left out.

GitHub reads `release.yml` from the tag being released, so it must be on the release branch before the tag is created.

To label the PRs merged before `pr-type-label.yml` existed, run it manually from the Actions tab with the date of the previous release, first as a dry run.

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