# PRD.md — Apache OFBiz Monolith (AI-Ready Product Requirements)

> **What this is:** complete functional and non-functional requirements for the CURRENT monolith (`monolith/`), written for AI agents and new engineers. Every requirement is **descriptive of shipped behavior** — traceable to code in the working tree, not aspirational.
> **Alignment:** ARCHITECTURE.md (structure), LOWLEVEL.md (implementation detail), adr/ (decisions), FUTURESTATE-ARCHITECTURE.md (replacement target). See §9 traceability matrix.
> **Sizing evidence** (from tree, 2026-10-16): 28 components · 904 entities · 3,548 service definitions · 20+ mounted webapps · 12 DB dialects · 39 UiLabel catalogs · Java 17 / Gradle / embedded Tomcat 10.1.

---

## 1. Overview

Apache OFBiz is an open-source ERP suite: order management, accounting/ledger, product catalog, inventory/shipping, manufacturing (MRP), CRM/party, HR, content management, marketing, and work management — delivered as one JVM deployable with three engines (Entity, Service, Web/Widget) plus a JAX-RS REST API. Business behavior is largely XML-declared (entities, services, screens, controllers) and executed by generic Java engines.

**Product goals (as-built):**
- G1: Single-command deployable ERP on any of 12 relational databases.
- G2: Business logic invokable uniformly (services) regardless of implementation language (Java, Minilang, Groovy, entity-auto).
- G3: Extensible by downstream components without framework code changes.
- G4: Back-office UI (widget screens + themes) AND headless API (REST/SOAP) from the same service surface.

## 2. Users & Personas

| Persona | Uses | Surface |
|---|---|---|
| Back-office operator | order entry, invoicing, catalog, warehouse ops | webapp screens (ordermgr, ap/ar, catalog, facility, partymgr…) |
| Administrator | config, data tools, entity/service browsers | webtools (`/webtools`) |
| API consumer / integrator | headless integration, custom frontends | `/rest` (JAX-RS, OpenAPI), SOAP endpoints |
| Downstream developer | builds custom OFBiz components | component-loading API, entity/service/widget frameworks |
- Localization: UI label catalogs (39 UiLabels XML files) with per-request locale resolution.

## 3. Functional Requirements — Framework (engines)

### FR-1 Component & Startup
- FR-1.1: System must bootstrap via `Start` containers, loading components declared in `component-load.xml` chains (framework → applications → plugins dir).
- FR-1.2: Each component must self-describe via `ofbiz-component.xml` (classpath, `entity-resource`, `service-resource`, `webapp`, `test-suite`, `keystore`, `uel-mapping`, `depends-on`).
- FR-1.3: Component load order must honor `depends-on` declarations (failure otherwise).
- FR-1.4: Embedded Tomcat (`CatalinaContainer`) must mount every declared webapp at its `mount-point`.

### FR-2 Entity Engine (data tier)
- FR-2.1: System must load entity definitions from XML (`entitydef/`) of all loaded components into a per-model-reader cache (904 entities in `monolith/applications/datamodel`; components may add local entities).
- FR-2.2: `Delegator` API must provide CRUD + find (`findOne/findByAnd/storeValue/removeByAnd/…`) with no SQL in calling code.
- FR-2.3: Field types must map per-database via `monolith/framework/entity/fieldtype/` XML (12 dialects: postgres, mysql, oracle, mssql, sybase, h2, hsql, firebird, sapdb, advantage, axion, daffodil).
- FR-2.4: Datasources/delegators must be configurable in `entityengine.xml` without code changes.
- FR-2.5: View entities must express joins at mapping level (`ModelViewEntity`).
- FR-2.6: Entity caches must be configurable per cache name in `entityengine.xml`.

### FR-3 Service Engine (application tier)
- FR-3.1: Business operations must be XML-declared services (`servicedef/`, 3,548 across components) with declared in/out parameters.
- FR-3.2: Invocation must go through `LocalDispatcher.runSync/runAsync` → per-delegator `ServiceDispatcher` → engine by `engine-name` (java, entity-auto, groovy, script, route, interface, soap, clojure, tracked).
- FR-3.3: `entity-auto` services must auto-generate CRUD from entity name.
- FR-3.4: ECAs (event-condition-actions) must run pre/post/on-error hooks around invocations.
- FR-3.5: Async execution must persist jobs in the DB-backed `JobManager` sandbox with poller execution; service semaphores must serialize named services and self-clean dead-instance locks on restart.
- FR-3.6: JMS listeners must be attachable via `JmsListenerFactory`.
- FR-3.7: Transactions must wrap service invocations with rollback propagation into the Entity Engine.

### FR-4 Web Control & UI
- FR-4.1: Each webapp must route through `ControlServlet` → `RequestHandler` → `controller.xml` request-maps (URI → security → event → view).
- FR-4.2: Views must render widget screens/forms/menus (XML) through theme FreeMarker renderers (6 themes; common-theme base).
- FR-4.3: The system must serve the webtools admin webapp (entity/service browsers, data tools, label manager).
- FR-4.4: Host-header allowlist (`security.properties: host-headers-allowed`) must reject requests with non-listed domains.
- FR-4.5: Static content URL prefixes must be configurable (`url.properties: content.url.prefix.*`).

### FR-5 REST API
- FR-5.1: JAX-RS application (`org.apache.ofbiz.ws.rs`) must map HTTP requests to service invocations (`ServiceRequestWorker`).
- FR-5.2: Auth must support JWT token filter, HTTP Basic, and CORS filter.
- FR-5.3: Responses must use uniform success/error envelopes with pagination (`RestListResponseBuilder`).
- FR-5.4: OpenAPI documentation must be generated (`OpenApiUtil`) and served with Swagger UI.

### FR-6 Security
- FR-6.1: Authentication must support form login (`LoginWorker`), external login key exchange, JWT (`JWTManager`), HTTP Basic on REST.
- FR-6.2: Authorization must check the `Security` interface (party/permission entities) in services and screens; controller request-maps must enforce login via security chains.
- FR-6.3: Password hashing must use Apache Shiro (single version).
- FR-6.4: CSRF protection must apply configurable strategies (`CsrfUtil` + defense strategies).
- FR-6.5: Uploads must be validated (`SecuredUpload`); FreeMarker templates sandboxed (`SecuredFreemarker`); token/SameSite filters active on requests.
- FR-6.6: A secret-audit container must record secret usage events via delegator.

### FR-7 i18n / Localization
- FR-7.1: UI strings must resolve from UiLabel catalogs (39 files) by request locale.
- FR-7.2: Multi-timezone support must be user-selectable (webtools timezone list).

## 4. Functional Requirements — Business Domains (monolith/applications/)

Service counts = declared services per component (evidence, from tree).

### FR-8 Order Management (`monolith/applications/order` — 465 services)
- FR-8.1: Order lifecycle: quote → order → approval → completion/cancellation with status machine.
- FR-8.2: Returns and order adjustments (charges, promotions) management.
- FR-8.3: Quote creation and conversion to orders.
- FR-8.4: Requirements and cart services.

### FR-9 Accounting — Billing (`monolith/applications/accounting` — 705 services total; billing slice)
- FR-9.1: Invoice issue/post/settle; payments capture/void/refund; billing accounts.
- FR-9.2: Payment gateway integration framework (auth/capture/reverse flows per gateway config).

### FR-10 Accounting — Ledger
- FR-10.1: GL accounts, journal postings (`AcctgTrans`), fiscal periods, trial balance.
- FR-10.2: Auto-posting rules from invoices/payments (ECA-driven).
- FR-10.3: FinAccounts (financial accounts) with balances.

### FR-11 Product & Catalog (`monolith/applications/product` — 835 services)
- FR-11.1: Product create/update with features, categories, associations, variants.
- FR-11.2: Pricing: price rules, promotions, currencies.
- FR-11.3: Product stores, catalogs, SEO configuration.
- FR-11.4: Subscriptions, rentals, agreements where enabled.

### FR-12 Inventory & Shipment (shipment entitymodel + product facility slice)
- FR-12.1: Facilities/warehouses, inventory items, stock adjustments, physical counts.
- FR-12.2: Reservations against orders; oversell prevention via reservation checks.
- FR-12.3: Shipment creation, packing, routing; shipment status tracking.

### FR-13 Party / CRM (`monolith/applications/party` — 266 services)
- FR-13.1: Party (person/group) CRUD, roles, relationships, contact mechanisms with purposes.
- FR-13.2: Party classification, groups, note/attribute management.

### FR-14 Manufacturing (`monolith/applications/manufacturing` — 127 services)
- FR-14.1: BOM management (manufacturing BOM trees).
- FR-14.2: Routing/tasks for production runs.
- FR-14.3: MRP runs (MrpServices/ProposedOrder) scheduled via JobManager.

### FR-15 Content Management (`monolith/applications/content` — 289 services)
- FR-15.1: Content trees, data resources (documents, URLs), CMS sites/mapping.
- FR-15.2: Content roles, permissions, and publishing workflows.

### FR-16 Work Effort (`monolith/applications/workeffort` — 167 services)
- FR-16.1: Projects/tasks/activities with statuses, priorities, associations.
- FR-16.2: Timesheets, calendar/iCalendar webapp (`/iCalendar`).

### FR-17 Human Resources (`monolith/applications/humanres` — 131 services)
- FR-17.1: Employee positions, employments, responsibilities.
- FR-17.2: Skills, training, payroll inputs, benefits.

### FR-18 Marketing (`monolith/applications/marketing` — 110 services)
- FR-18.1: Campaigns, tracking codes, contact lists.
- FR-18.2: Opportunities/forecasts (SFA webapp `/sfa`).

## 5. Non-Functional Requirements

| ID | Category | Requirement (verifiable against the system) |
|---|---|---|
| NFR-1 | Portability | Builds and runs on JDK 17 (`sourceCompatibility`) with any of the 12 fieldtype-declared databases; no DB vendor SQL outside dialect files |
| NFR-2 | Deployability | Single-command start (`gradlew ofbiz`); docker image via Dockerfile targets `runtime`/`demo`; embedded Tomcat — no external app server required |
| NFR-3 | Performance | Entity caches configurable per entity/view; async service execution frees request threads (JobManager); widget macro rendering via single constant template (ADR-006) — no shipped TPS/latency SLAs; performance is deployment-tuned |
| NFR-4 | Availability | Stateless web tier (session-scoped auth only); DB-persisted state; service semaphores self-release dead-instance locks on restart |
| NFR-5 | Scalability | Horizontal: replicate JVM behind LB against shared RDBMS; vertical caches; no sharding support |
| NFR-6 | Security | Host-header allowlist; CSRF strategies; secured uploads/templates; Shiro hashing; JWT/Basic/token auth chains (see FR-6) |
| NFR-7 | Observability | Uniform `Debug`/Module logging across all components; service metrics (`TrackedServiceEngine`); webtools runtime stats; test report archiving in CI |
| NFR-8 | Extensibility | New components plug in via `ofbiz-component.xml` without framework edits (ADR-001); Minilang/Groovy/Java service implementations interchangeable |
| NFR-9 | Maintainability | Declarative XML surface greppable and diffable; loaders fail-fast on bad descriptors; style gates (checkstyle/codenarc) on push |
| NFR-10 | Internationalization | 39 UiLabel catalogs; per-request locale; multi-timezone; multi-currency in accounting/product pricing |
| NFR-11 | Auditability | ServerHit visit tracking; secret-audit events; entity audit via ECAs where configured |
| NFR-12 | Compatibility | Jakarta Servlets on Tomcat 10.1; JAX-RS + OpenAPI REST; SOAP services preserved; FreeMarker theming |
| NFR-13 | Testability | Per-component `testdef/` suites via testtools; JUnit colocated; `testMode` entity-recording in GenericDelegator for rollback-safe tests |
| NFR-14 | Data integrity | Transactions span service+entity layers; view entities enforce mapping-level joins; cross-context references validated at service layer |

## 6. Constraints & Environment

- C-1: JDK 17 required (not 21 — that is the target state only, FUTURESTATE docs).
- C-2: Gradle wrapper is the only supported build entry; wrapper jar fetched by `gradle/init-gradle-wrapper.sh` (not committed).
- C-3: `monolith/framework/rest-api` carries Node/npm tooling (swagger-ui) — first build needs network for npm install.
- C-4: Demo data (`loadDemo`) needed for meaningful runtime exploration.
- C-5: License Apache 2.0; NOTICE/LICENSE attribution preserved.

## 7. Success Criteria (for agents working this codebase)

- SC-1: Any FR above traces to implementing code within one LOWLEVEL.md lookup (§10 table) or one `servicedef`/`entitydef` grep.
- SC-2: `cd monolith && ./gradlew build` green.
- SC-3: Docker demo boots and serves webtools/ordermgr/catalog with demo data (README quickstart).
- SC-4: Changes to services/entities follow XML-first convention (ADR-001); style gates pass.

## 8. Out of Scope (this PRD)

- Future-state microservices requirements → FUTURESTATE-ARCHITECTURE.md.
- Plugins ecosystem (birt, ecommerce, solr plugins) — not in this repo's tree.
- Operational runbooks (deployment tuning, DB sizing).

## 9. Traceability / Document Alignment Matrix

| This PRD section | Aligned document | Alignment contract |
|---|---|---|
| §3 FR-1..7 | ARCHITECTURE.md §5.1–5.6, §9; LOWLEVEL.md §1–8 | Engine names, class/line citations unchanged |
| §4 FR-8..18 | ARCHITECTURE.md §5.9, applications inventory | Domain list + entity counts (904) match |
| §5 NFR-3 | ADR-006 (FtlWriter) | Perf claims limited to shipped mechanisms |
| §5 NFR-6 | ARCHITECTURE.md §9; ADR-004 (Shiro) | Security surface identical |
| §6 C-1 | ARCHITECTURE.md §8 vs FUTURESTATE §2 D10 | Java 17 (current) vs 21 (target) — no conflation |
| §8 | FUTURESTATE-ARCHITECTURE.md | Target requirements live only there |
| Agent orientation tasks | tasks/prd-ofbiz-framework-overview.md | Distinct scope — that PRD orients agents; this PRD specifies the product |

**Alignment verification (2026-10-16):** entity counts (904), service counts (3,548), component counts (28), dialect list (12), themes (6), and engine lists re-derived from tree this session — consistent across PRD.md, ARCHITECTURE.md, LOWLEVEL.md.

## 10. Glossary

- **Delegator** — Entity Engine façade for all persistence calls.
- **Dispatcher (Local/Service)** — Service Engine invocation path.
- **ECA** — event-condition-action hook around service calls.
- **Minilang** — XML service DSL (`monolith/framework/minilang`).
- **Widget** — XML screen/form/menu definition rendered via theme FreeMarker.
- **Component** — loadable module declared by `ofbiz-component.xml`.

---

*Generated 2026-10-16 from working-tree analysis (`monolith/`). Requirements are descriptive of the current implementation; behavior changes require updating this PRD plus the aligned document per §9.*
