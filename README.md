# OFBiz Modernization Program

This repository is an ERP modernization program: the **Apache OFBiz monolith** (current, production system) living alongside the **target-state microservices architecture** that will progressively replace it via a strangler migration.

This README describes the program repository. The monolith's own upstream README lives at [monolith/README.md](monolith/README.md).

---

## Repository Layout

```
.
├── ARCHITECTURE.md              # Current monolith architecture (3-tier descriptive)
├── LOWLEVEL.md                  # Current monolith implementation reference
├── FUTURESTATE-ARCHITECTURE.md  # Target microservices architecture (design)
├── FUTURESTATE-LOWLEVEL.md      # Target-state implementation reference (blueprint)
├── adr/                         # Architecture Decision Records (6: current state)
├── tasks/                       # PRDs (agent orientation / program work)
├── monolith/                    # Apache OFBiz — the current system, unmodified
│   ├── framework/               #   16 framework components (entity, service, webapp…)
│   ├── applications/            #   12 business applications (order, accounting…)
│   ├── themes/  docker/  docs/  #   UI themes, container support, docs
│   └── build.gradle, gradlew…   #   Gradle build (self-contained)
├── microservices/               # Future state — one folder per service
│   ├── party-svc/ … marketing-svc/    # 11 domain services
│   └── identity-svc/ gateway/ search-svc/ notification-svc/ audit-svc/   # 5 platform
└── .codegraph/ build/ .gradle/  # local tooling artifacts (not tracked)
```

## Documentation Map

| Question | Document |
|---|---|
| What does the monolith require (FR/NFR)? | [PRD.md](PRD.md) |
| How does the current system work? | [ARCHITECTURE.md](ARCHITECTURE.md) + [LOWLEVEL.md](LOWLEVEL.md) |
| Where is X in the monolith code? | [LOWLEVEL.md](LOWLEVEL.md) §10 lookup table |
| What are we moving to? | [FUTURESTATE-ARCHITECTURE.md](FUTURESTATE-ARCHITECTURE.md) |
| How will the target be built? | [FUTURESTATE-LOWLEVEL.md](FUTURESTATE-LOWLEVEL.md) |
| What is the delivery plan (phases/stories/gates)? | [IMPLEMENTATION.md](IMPLEMENTATION.md) |
| What is the project plan (timeline/staffing/risks)? | [PROJECT-PLAN.md](PROJECT-PLAN.md) |
| Why was X decided? | [adr/](adr/) + FUTURESTATE-ARCHITECTURE.md §2 decision record |
| What work is planned? | [tasks/](tasks/) |

## Current State: The Monolith

Apache OFBiz — Java 17, Gradle, single-JVM ERP: 28 components, 904 entities (XML-declared), Service Engine (9 engine types), Entity Engine (12 DB dialects), Widget/FreeMarker UI, embedded Tomcat. Full detail: [ARCHITECTURE.md](ARCHITECTURE.md).

### Run the monolith (docker demo)

```bash
cd monolith
docker build --target runtime -t ofbiz-docker .
cd docker/examples/postgres-demo
docker compose up -d
# → https://localhost:8443/webtools/control/main  (login admin/ofbiz)
# Self-signed cert: Advanced → Proceed. First boot loads demo data (~2 min).
```

Known first-boot gotchas (fixed on this branch): mounted hook scripts need exec bits; `OFBIZ_HOST` must match the name your browser uses (`localhost`).

Native (non-docker): `cd monolith && ./gradlew ofbiz` (requires JDK 17; run `sh gradle/init-gradle-wrapper.sh` once to fetch the wrapper jar).

## Future State: Microservices

**16 independently deployable services** — Java 21, Spring Boot 3.3, clean architecture (domain/application/adapters/infrastructure per service), database-per-service, Kafka event backbone (outbox pattern), Kubernetes + Istio mesh, Spring Cloud Gateway, Spring Authorization Server (OIDC), Angular 17+ SPA frontend.

| Group | Services |
|---|---|
| Domain (11) | party, product, order, billing, finance, inventory, manufacturing, content, workeffort, humanres, marketing |
| Platform (5) | identity, gateway, search, notification, audit |

Each `microservices/<svc>/` folder is the future home of that service (multi-repo split is open question §11.1). Service purposes, owned entity slices, API surfaces, and event flows: FUTURESTATE-ARCHITECTURE.md §5–§6.

## Migration Roadmap (strangler)

| Phase | Scope |
|---|---|
| 0 | Platform: K8s + mesh, Kafka, gateway (routing to monolith), identity, observability |
| 1 | Party (+ audit) — zero inbound dependencies |
| 2 | Product + Inventory (+ search) |
| 3 | Order |
| 4 | Billing + Finance (+ notification) |
| 5 | Long tail: manufacturing, workeffort, humanres, content, marketing |
| 6 | Decommission monolith |

Cutover/rollback gates per phase: FUTURESTATE-ARCHITECTURE.md §10.

## Status

- Current: monolith runs (docker demo verified on this machine)
- Target: **design complete, zero implementation** — per-service specs are the next step
- Note: fork CI workflows moved with `monolith/.github` (root-path-bound, would need path fixes to re-enable)

## Contributing

Program docs (root) and the monolith follow upstream OFBiz conventions respectively. New architecture work starts as a spec against FUTURESTATE-LOWLEVEL.md, not as code.
