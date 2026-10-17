# LOWLEVEL.md — Apache OFBiz Implementation Reference

> Companion to [ARCHITECTURE.md](ARCHITECTURE.md). This is the "where is X in the code" reference: per-engine internals, entry classes, lifecycles, and file lookup tables. All paths relative to repo root; line numbers valid for `trunk` as of 2026-10-15.

---

## 1. Startup & Component Loading

**Sequence:**
1. `./gradlew ofbiz` → JVM main: `monolith/framework/start/src/main/java/org/apache/ofbiz/base/start/Start.java` (`Start:54`).
2. Start loads configuration and boots "containers" in order — most importantly the embedded Tomcat container: `monolith/framework/catalina/src/main/java/org/apache/ofbiz/catalina/container/CatalinaContainer.java`.
3. Components are discovered via `component-load.xml` chains at `monolith/framework/component-load.xml` and `monolith/applications/component-load.xml` (each loads its directory's components in listed order).
4. Each component is parsed from its `ofbiz-component.xml` by `monolith/framework/base/src/main/java/org/apache/ofbiz/base/component/ComponentConfig.java` (`ComponentConfig:425`). One descriptor declares everything a component contributes:

| XML element | Parsed into | Consumed by |
|---|---|---|
| `classpath` | `ClasspathInfo` | classloader |
| `entity-resource` | `EntityResourceInfo` (ComponentConfig.java:451) | Entity Engine ModelReader |
| `service-resource` | `ServiceResourceInfo` | Service Engine |
| `webapp` | `WebappInfo` | CatalinaContainer (mounts webapp) |
| `test-suite` | `TestSuiteInfo` | testtools |
| `keystore`, `uel-mapping`, `depends-on`, `resource-loader` | respective Info classes | base/security/util |

**Convention:** every component directory has `ofbiz-component.xml`, most have `config/`, `servicedef/`, `entitydef/`, `testdef/`.

---

## 2. Request Lifecycle (Web Tier)

All in `monolith/framework/webapp/src/main/java/org/apache/ofbiz/webapp/`:

1. **Entry:** `control/ControlServlet.java` (`ControlServlet:61`) — single master servlet per webapp; sets `delegator`, `dispatcher`, `security` as request attributes. (`monolith/applications/product/.../SeoControlServlet.java` extends it for SEO URLs.)
2. **Filters:** `control/ContextFilter.java`, `control/ControlFilter.java`, `control/SameSiteFilter.java`, `control/TokenFilter.java` — session/auth/token hardening before control.
3. **Routing:** `control/RequestHandler.java` resolves the URI against the webapp's `controller.xml` (parsed by `control/ConfigXMLReader.java`) — request-map defines: URI → security (`LoginCheck`) → event (service call or Java/Groovy event method) → response/view.
4. **Auth during request:** `control/LoginWorker.java` (login/logout events), `control/ExternalLoginKeysManager.java` (cross-webapp key exchange), `control/JWTManager.java` (token creation/validation).
5. **View render:** widget screens (`widget/` dirs per component) rendered via `monolith/framework/widget/src/main/java/org/apache/ofbiz/widget/renderer/ScreenRenderer.java` (`ScreenRenderer:88`) using the active theme's `ScreenStringRenderer` (`ScreenStringRenderer:32`) — FreeMarker-based, from `themes/`.

**Where to trace a URL:** find the webapp (component's `ofbiz-component.xml` `webapp` mount-point) → its `webapp/WEB-INF/controller.xml` → request-map → event service name → servicedef XML → implementation class.

---

## 3. Service Engine Internals

All in `monolith/framework/service/src/main/java/org/apache/ofbiz/service/` unless noted.

**Invocation path:**
- `ServiceContainer.getLocalDispatcher(name, delegator)` — `ServiceContainer.java:83`; caches dispatchers (`DISPATCHER_CACHE`), tenant-suffixed names.
- `LocalDispatcher.runSync/runAsync(name, ctx)` — `LocalDispatcher.java:35` interface.
- `ServiceDispatcher` — `ServiceDispatcher.java:97` (private ctor, per-delegator singleton). On init: creates `GenericEngineFactory`, reads ServiceGroup + ServiceEca configs, gets `Security` via `SecurityFactory` (line 106), clears dead `ServiceSemaphore` rows for this instance id (line 113), starts `JobManager` (line 129), optional `JmsListenerFactory` (line 138).

**Engine selection** (by `engine-name` attribute of the service definition), files in `service/engine/`:

| Engine | File | Runs services defined as |
|---|---|---|
| `java` | `StandardJavaEngine.java` | static Java methods (location = class, invoke = method) |
| `entity-auto` | `EntityAutoEngine.java` | CRUD auto-generated from entity name |
| `groovy` | `GroovyEngine.java` | Groovy scripts |
| `script` / `route` / `interface` | `ScriptEngine.java` / `RouteEngine.java` / `InterfaceEngine.java` | JSR-223 scripts / route scripts / interface-only defs |
| `soap` | `SOAPClientEngine.java` | remote SOAP calls |
| `clojure` | `StandardClojureEngine.java` | Clojure |
| tracked wrapper | `TrackedServiceEngine.java` | metrics-wrapped invocations |

**Async/scheduling:** `service/job/JobManager.java` (`JobManager:74`) — DB-backed job sandbox; `runAsync` enqueues; poller threads execute; semaphores serialize named services.
**ECAs:** `ServiceEcaUtil` + eca XML files — pre/post/on-error hooks around invocation.
**Service definitions:** `servicedef/*.xml` per component, registered via `service-resource` in `ofbiz-component.xml`; model = `service/ModelService.java`.

**Helper:** `monolith/framework/entityext/src/main/java/org/apache/ofbiz/entityext/EntityServiceFactory.java` — named dispatcher `entity-<delegatorName>` for entity-internal service calls.

---

## 4. Entity Engine Internals

All in `monolith/framework/entity/src/main/java/org/apache/ofbiz/entity/` unless noted.

- **API:** `Delegator.java:52` (interface) — `findOne`, `findByAnd`, `storeValue`, `removeByAnd`, etc. Impl: `GenericDelegator.java` — protected constructors, factory-only (`GenericDelegator:198-201`); thread-local user/session identifier stacks (`pushUserIdentifier`, line 146) for audit context.
- **Model loading:** `model/ModelReader.java` — per model-reader name (default `main`); constructor `ModelReader:105-130` merges resources from `monolith/framework/entity/config/entityengine.xml` + every component's `entity-resource` where reader name matches; results cached (`READERS` map, `getEntityCache`).
- **Definitions:** `monolith/applications/datamodel/entitydef/*-entitymodel.xml` — 10 files, **904 entities** (accounting, content, humanres, manufacturing, marketing, order, party, product, shipment, workeffort). Components may also define local entities in their own `entitydef/`.
- **Dialects:** `monolith/framework/entity/fieldtype/fieldtype{postgres,mysql,oracle,mssql,sybase,h2,hsql,firebird,sapdb,advantage,axion,daffodil}.xml` — map abstract field types (`id-ne`, `currency-amount`…) to SQL types per database.
- **Config:** `monolith/framework/entity/config/entityengine.xml` — delegators, datasources, entity-cache settings, fieldtype list per datasource.
- **DAO layer:** `GenericDAO` / helper classes in `entity/` generate SQL from `ModelEntity`/`ModelField`/`ModelViewEntity`.

---

## 5. Minilang

`monolith/framework/minilang/src/main/java/org/apache/ofbiz/minilang/`:
- `SimpleMethod.java` — one simple-method = one service; XML DSL.
- `operation/` — individual operations (entity ops, service calls, field setting, branching).
- `method/` — method-level constructs; `SimpleMapProcessor.java` — input validation maps.
- `MiniLangValidate.java` — definition-time validation; `artifact/` — artifact info for dependency tracking.
Minilang services are declared in servicedef XML pointing at a simple-methods XML file via `location`/`invoke`.

---

## 6. Widget / Screen Rendering

- Definitions: per-component `widget/` dirs — screens (`*Screens.xml`), forms (`*Forms.xml`), menus (`*Menus.xml`).
- Renderer: `monolith/framework/widget/.../renderer/ScreenRenderer.java:88`; abstraction `ScreenStringRenderer.java:32`.
- Actual HTML text renderers live in themes — `themes/common-theme` is the base; others (bluelight, flatgrey, helveticus, rainbowstone, tomahawk) override FreeMarker macros.
- Recent trunk change (OFBIZ-13675): `FtlWriter` calls every macro through one constant template — see `monolith/framework/widget/.../renderer/`.

---

## 7. REST API Internals

`monolith/framework/rest-api/src/main/java/org/apache/ofbiz/ws/rs/` (JAX-RS):

| Piece | File | Role |
|---|---|---|
| Bootstrap | `listener/ApiContextListener.java` | servlet-context init (dispatcher/delegator wiring) |
| Auth | `security/auth/APIAuthFilter.java`, `HttpBasicAuthFilter.java`, `APICorsFilter.java` | request auth + CORS |
| Dispatch | `util/ServiceRequestWorker.java`, `RestServiceUtil.java` | HTTP request → service invocation |
| Responses | `util/RestListResponseBuilder.java`, `response/Success.java`, `response/Error.java`, `core/ResponseStatus.java` | uniform JSON envelopes; pagination |
| Config | `core/OFBizApiConfig.java`, `util/OpenApiUtil.java` | OpenAPI document generation |
| Cleanup | `filters/ServiceContextCleanupFilter.java` | per-request context teardown |

Webapp content under `monolith/framework/rest-api/webapp/` (`docs/`, `rest-api/`); Swagger UI served from npm-installed `node_modules` (component has own `package.json`, `build.gradle`).

---

## 8. Security Internals

`monolith/framework/security/src/main/java/org/apache/ofbiz/security/`:
- `Security.java` + `SecurityFactory.java` — permission checks against party/security permission entities (obtained with a `Delegator`, see `ServiceDispatcher.java:106`).
- `CsrfUtil.java`, `CsrfDefenseStrategy.java`, `NoCsrfDefenseStrategy.java`, `ICsrfDefenseStrategy.java` — CSRF token strategies.
- `SecuredUpload.java` — upload validation; `SecuredFreemarker.java` — template sandboxing.
- `SecurityUtil.java` — helpers.
Crypto/hashing via Apache Shiro (single consolidated version — trunk commit `5b1856083d`, OFBIZ-13685).
Web-side pieces live in monolith/framework/webapp control (see §2 item 4).

---

## 9. Testing

- Per-component `testdef/` suites (XML) executed by `monolith/framework/testtools` harness; framework components carry their own (e.g. `monolith/framework/entity/testdef/`).
- JUnit tests colocated: `src/test/java/...` (e.g. `monolith/framework/entity/src/test/java/org/apache/ofbiz/entity/test/EntityTestSuite.java`).
- Gradle: `./gradlew test` (unit), integration tasks in root build.gradle; archived reports via `monolith/framework/webtools/test-report-archive.gradle`.
- Entity test mode: `GenericDelegator` has `testMode`/`testOperations` deque (`GenericDelegator.java:131-135`) to record and roll back operations in tests.

---

## 10. Quick Lookup — "Where Is X?"

| Looking for | Go to |
|---|---|
| URL routing for a page | `<component>/webapp/<webapp>/WEB-INF/controller.xml` |
| Service implementation | `<component>/servicedef/*.xml` → `location` + `invoke` attrs → class/method or minilang XML |
| Entity fields | `monolith/applications/datamodel/entitydef/<domain>-entitymodel.xml` (904 entities) |
| DB connection config | `monolith/framework/entity/config/entityengine.xml` |
| New DB dialect | `monolith/framework/entity/fieldtype/` |
| Component registration | `<component>/ofbiz-component.xml` |
| Load order | `monolith/framework/component-load.xml`, `monolith/applications/component-load.xml` |
| Login flow | `monolith/framework/webapp/.../control/LoginWorker.java` |
| REST endpoint → service mapping | `monolith/framework/rest-api/src/main/java/org/apache/ofbiz/ws/rs/util/ServiceRequestWorker.java` |
| Async job scheduling | `monolith/framework/service/.../job/JobManager.java` |
| Theme HTML macros | `themes/<theme>/` (base: `themes/common-theme`) |
| Gradle task definitions | root `build.gradle` + `buildSrc/` |
| Runtime state/logs | `runtime/` |

---

*Generated 2026-10-15. Citations from working tree (`trunk`); re-verify line numbers after pulls.*
