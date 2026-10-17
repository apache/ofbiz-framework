# PRD: Apache OFBiz Framework Overview (Agent Orientation Document)

## 1. Introduction / Overview

Apache OFBiz is an open-source Enterprise Resource Planning (ERP) system written in Java (JDK 17, Gradle build). This repository (`ofbiz-framework`, branch `trunk`) is the upstream Apache project. It contains both a **framework layer** (the technical engines: entity persistence, service orchestration, web/REST serving, security) and an **applications layer** (business functionality: order management, accounting, product catalog, party/customer management, manufacturing, HR, content, marketing, work effort).

This PRD is a **descriptive overview PRD, not a feature PRD**. The product already exists and ships. Its purpose is to give AI agents (and new developers) a verifiable mental model of the system: what the product is, which modules exist, how they fit together, and how to prove the model is correct by running real commands. Each user story below is an orientation/verification task an agent can execute and check, ending in a full-stack build-and-run validation.

## 2. Goals

- Give an AI agent an accurate, current map of the repository's two layers (framework + applications) and supporting directories (themes, docker, docs, buildSrc).
- Define verifiable checkpoints (build, test, run) that prove an agent's understanding matches reality.
- Document the core architectural engines (Entity Engine, Service Engine, Widget/Screen layer, REST API) as navigational entry points.
- Keep every claim traceable to the working tree — no folklore.

## 3. User Stories

### US-001: Map the framework layer
**Description:** As an AI agent, I want a verified map of the 16 framework components so that I can locate the correct engine for any technical task.

**Acceptance Criteria:**
- [ ] Enumerate `monolith/framework/` components: base, catalina, common, datafile, entity, entityext, minilang, resources, rest-api, security, service, start, testtools, webapp, webtools, widget
- [ ] For each of the 4 core engines (entity, service, webapp, rest-api): identify its main Java package root and one entry-point class (e.g. `monolith/framework/entity/src/main/java/org/apache/ofbiz/entity/...`)
- [ ] Note per-component convention: each has `ofbiz-component.xml`, most have `config/`, `servicedef/`, `entitydef/`, `testdef/`
- [ ] Map is written to a notes file with file-path citations

### US-002: Map the applications layer
**Description:** As an AI agent, I want a verified map of the 12 application components so that I can route business-domain questions to the right module.

**Acceptance Criteria:**
- [ ] Enumerate `monolith/applications/`: accounting, commonext, content, datamodel, humanres, manufacturing, marketing, order, party, product, securityext, workeffort
- [ ] Identify `monolith/applications/datamodel` as the shared entity-definition home (entitydef XML files, not Java)
- [ ] For `order` and `accounting`: list their `servicedef/` XML files as the service-surface inventory
- [ ] Map is written to a notes file with file-path citations

### US-003: Understand the component lifecycle
**Description:** As an AI agent, I want to understand how components are loaded (`component-load.xml` chains, `ofbiz-component.xml` descriptors) so that I can add or trace component configuration.

**Acceptance Criteria:**
- [ ] Explain the role of `monolith/framework/component-load.xml` and `monolith/applications/component-load.xml`
- [ ] Explain what `ofbiz-component.xml` declares (classpath, entities, services, webapps)
- [ ] Locate the Java code in `monolith/framework/base` or `monolith/framework/start` that parses these descriptors, cited by file:line
- [ ] Explanation written to notes with citations

### US-004: Understand the Entity Engine contract
**Description:** As an AI agent, I want to understand how entities are defined in XML (`entitydef/`) and consumed via the Entity Engine so that I can answer data-model questions without guessing.

**Acceptance Criteria:**
- [ ] Show one concrete entity definition from `monolith/applications/datamodel` (name, fields, relations) and the file it lives in
- [ ] Identify the dispatcher/delegator classes in `monolith/framework/entity` that load and serve entity definitions
- [ ] Identify the database-agnostic fieldtype mechanism (`monolith/framework/entity/fieldtype/`)
- [ ] Notes file updated with citations

### US-005: Understand the Service Engine contract
**Description:** As an AI agent, I want to understand how services are declared in `servicedef/` XML and invoked (Java, Minilang, ECAs) so that I can trace business logic flows.

**Acceptance Criteria:**
- [ ] Show one concrete service definition (name, location/invoke attributes) from an applications component
- [ ] Identify the service dispatcher entry point in `monolith/framework/service`, cited by file:line
- [ ] Briefly state what Minilang is (`monolith/framework/minilang`) and when it is used vs. Java services
- [ ] Notes file updated with citations

### US-006: Understand the web and API surfaces
**Description:** As an AI agent, I want to understand the three interaction surfaces — Widget screens, webtools, and REST API — so that I can route UI vs. API tasks correctly.

**Acceptance Criteria:**
- [ ] Identify `monolith/framework/widget` (screen/form/menu widgets) and `monolith/framework/webapp` (request handling) roles
- [ ] Identify `monolith/framework/webtools` as the admin/back-office surface
- [ ] For `monolith/framework/rest-api`: identify the OpenAPI/swagger publication path (contains its own `package.json`/node tooling) and how endpoints map to services
- [ ] Notes file updated with citations

### US-007: End-to-end verification of the orientation model
**Description:** As a QA engineer, I want an automated end-to-end verification (build → unit tests → system start → REST probe) so that the orientation model is proven against a running system.

**Acceptance Criteria:**
- [ ] `./gradlew build` completes with exit code 0
- [ ] `./gradlew testIntegration` (or repo-equivalent test task) runs and passes
- [ ] System starts via `./gradlew ofbiz` (default port 8443/8080 per config) and responds to an HTTPS request on the webapp root
- [ ] REST API endpoint documented in `monolith/framework/rest-api` responds (at minimum, a 401/403 proving the route exists, not 404)
- [ ] Failure path: attempting an unauthorized admin action returns the configured auth error, not a stack trace leak
- [ ] All results recorded in the notes file with command output excerpts

## 4. Functional Requirements

- FR-1: The system must build from a clean checkout with JDK 17 using `./gradlew build`.
- FR-2: The framework must load components in the order declared by `component-load.xml` files, driven by each component's `ofbiz-component.xml`.
- FR-3: The Entity Engine must serve entity definitions from XML `entitydef/` files across all loaded components, with database portability via `fieldtype/` mappings.
- FR-4: The Service Engine must expose services declared in `servicedef/` XML, implemented in Java or Minilang, through a common dispatcher.
- FR-5: The REST API layer (`monolith/framework/rest-api`) must publish and serve endpoints mapped to framework services.
- FR-6: The web layer must render back-office screens via the Widget framework (`monolith/framework/widget` + `monolith/framework/webapp`), including the webtools admin surface.
- FR-7: The applications layer must deliver ERP business domains (order, accounting, product, party, manufacturing, humanres, content, marketing, workeffort) on top of the framework engines with no direct engine-internal coupling beyond the published APIs.

## 5. Non-Goals (Out of Scope)

- No new features, refactors, or code changes — this PRD is descriptive.
- No coverage of downstream deployments, hotwax/plugins ecosystems, or third-party forks.
- No performance benchmarking or capacity planning.
- No migration planning (e.g. Minilang retirement, Java version bumps).
- No documentation rewrite of the official ofbiz.apache.org docs.

## 6. Design Considerations

- Audience is AI agents: every claim must be re-verifiable by a shell command or file read in this working tree.
- Notes files produced by US-001..006 are the durable artifact; this PRD is the contract for them.

## 7. Technical Considerations

- JDK 17 required (`build.gradle`: `sourceCompatibility = JavaVersion.VERSION_17`).
- Gradle wrapper is the only supported build entry point.
- `monolith/framework/rest-api` ships its own Node/npm tooling (swagger-ui-dist) — not a pure-Java component.
- Demo data loading (`./gradlew loadDemo`) is prerequisite for meaningful runtime checks in US-007.

## 8. Success Metrics

- An agent that completes US-001..007 can answer "where is X" for any engine, component, entity, or service with a file path, verified by spot-check.
- Build + integration test + runtime probe all green on the agent's machine.
- Zero unverifiable claims in the notes files (every statement carries a citation).

## 9. Open Questions

- Should the orientation notes live in `docs/` (committed) or a scratch location (agent-private)?
- Is US-007's runtime probe acceptable in CI-sandboxes without network/Docker, or is build+test-only the fallback gate?
- Should themes (`themes/`) and docker (`docker/`) get their own orientation stories, or remain one-line mentions?

---

**Status:** draft — awaiting user review.
