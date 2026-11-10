<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

# Upgrading Apache OFBiz

This file lists what you need to change in your environment, configuration, custom code and data when you upgrade
the OFBiz framework and themes to a new major release. Each layer keeps its own notes, next to the code they describe:

* the framework and themes: this file
* the applications: [applications/UPGRADING.md](applications/UPGRADING.md), with the application data model changes
  in [applications/datamodel/DATAMODEL_CHANGES.adoc](applications/datamodel/DATAMODEL_CHANGES.adoc)
* the plugins: [UPGRADING.md](https://github.com/apache/ofbiz-plugins/blob/trunk/UPGRADING.md) in the ofbiz-plugins
  repository

The complete list of pull requests that change or remove a public API is in the "API changes" section of the
[release notes](https://github.com/apache/ofbiz-framework/releases) GitHub generates for each release, from the
`api-change` label. The notes below explain what to do about the changes that need action, and also cover the few
that were committed without a pull request.

When a pull request changes or removes a public API, add its entry here (or in the applications or plugins file) in
the same pull request.

## Upgrading from 24.09 to the next major release (trunk)

### Environment

* Java 25 replaces Java 17: OFBiz is compiled for Java 25 and needs a JDK 25 to build and run
  ([OFBIZ-13791](https://issues.apache.org/jira/browse/OFBIZ-13791)). Install a JDK 25 and point `JAVA_HOME` at it,
  and move your own CI, Docker images and IDE settings to it as well.
* Java compiler warnings now fail the build ([OFBIZ-13793](https://issues.apache.org/jira/browse/OFBIZ-13793)), and
  this includes the Java code of your own plugins and components. Fix the warnings, or build with
  `gradlew -PXlint:none` to turn the lint warnings and the failure off.
* Gradle 9 replaces Gradle 8. Run `gradle/init-gradle-wrapper.sh` (or `init-gradle-wrapper.ps1`) again to get the
  new wrapper.
* The embedded Tomcat moves from 9 to 11, and with it from Java EE (`javax.*`) to Jakarta EE 11 (`jakarta.*`):
  Servlet 6.1, Jersey 4 and Spring 7 ([OFBIZ-12989](https://issues.apache.org/jira/browse/OFBIZ-12989),
  [#2428](https://github.com/apache/ofbiz-framework/pull/2428)). See "Custom code" below.
* Groovy moves from 3 to 6, and its Maven group from `org.codehaus.groovy` to `org.apache.groovy`
  ([#2417](https://github.com/apache/ofbiz-framework/pull/2417)).
* Derby support is removed and H2 is the default embedded database
  ([#1221](https://github.com/apache/ofbiz-framework/pull/1221)). If you ran OFBiz on Derby, export your data with
  the entity export tools on 24.09 and import it into H2 or an external database after the upgrade.
* The ical4j dependency and the extra `packages.atlassian.com` Maven repository are removed
  ([OFBIZ-13816](https://issues.apache.org/jira/browse/OFBIZ-13816)).

### Configuration

* `entityengine.xml`: the `localderby*` datasources and the `derby` field type are removed; use the `localh2*`
  datasources instead. `fieldtypederby.xml`, `runtime/data/derby.properties` and the `jdbc-password.derby-*` keys are
  gone ([#1221](https://github.com/apache/ofbiz-framework/pull/1221)).
* `web.xml` of custom webapps: switch to the Jakarta EE namespace and schema, and remove the
  `org.apache.ofbiz.webapp.control.CacheFilter` filter, which no longer exists
  ([#2482](https://github.com/apache/ofbiz-framework/pull/2482)).
* `log4j2.xml`: the `OFBizDynamicThresholdFilter` and the `packages="org.apache.ofbiz.base.log4j"` attribute are
  removed; a custom `log4j2.xml` that still uses them fails to load
  ([#2426](https://github.com/apache/ofbiz-framework/pull/2426)).
* `controller.xml`: the `http` view handler ([#2210](https://github.com/apache/ofbiz-framework/pull/2210)) and the
  `screenxls` view handler ([#1973](https://github.com/apache/ofbiz-framework/pull/1973)) are removed. Use `screenxlsx`
  for spreadsheets.
* Screen, form, grid, menu and tree locations must be `component://` locations. To load them from a plain file path,
  set `allowFilePaths` in `security.properties`
  ([#1650](https://github.com/apache/ofbiz-framework/pull/1650)).
* REST: services marked `export="true"` are no longer published as REST endpoints. Declare the endpoints you need in a
  `*.rest.xml` file in your component's `api` directory, as described in the rest-api component documentation
  ([#1609](https://github.com/apache/ofbiz-framework/pull/1609)). The rest-api component now lives in
  `framework/rest-api`, so remove `plugins/rest-api` from your checkout
  ([#1357](https://github.com/apache/ofbiz-framework/pull/1357)).
* Service definitions: the `action` attribute of `<service>` is removed from `services.xsd`
  ([#1354](https://github.com/apache/ofbiz-framework/pull/1354)).
* `serviceengine.xml`: the `clojure` engine is removed
  ([OFBIZ-13798](https://issues.apache.org/jira/browse/OFBIZ-13798)). Services declared with `engine="clojure"` no
  longer load; port them to Groovy or Java, or keep `StandardClojureEngine` from release24.09 in your own plugin
  with the `org.clojure:clojure` dependency and an `<engine>` entry.
* The `datafile` component and the Webtools Data File tools (`viewdatafile`) are removed, with `datafiles.xsd` and
  the `ModelDataFile` cache setting ([OFBIZ-13815](https://issues.apache.org/jira/browse/OFBIZ-13815)).
  Convert `<data-file>` imports to entity XML or a custom service, or keep the component from release24.09 as a
  plugin.
* SOAP is removed ([OFBIZ-13821](https://issues.apache.org/jira/browse/OFBIZ-13821)); it had been disabled since
  [OFBIZ-12212](https://issues.apache.org/jira/browse/OFBIZ-12212). The `soap` event handler and service engine, the
  `main-soap` service location, the webtools `SOAPService` request and WSDL view, `framework/service/config/axis2` and
  the `secureSoapAnswer` property are gone, and `SOAPService` is removed from `http.request-map.list` in
  `url.properties`. The `<namespace>` element of `<service>` is removed from `services.xsd`; delete it from your
  service definitions. Expose services through a `*.rest.xml` file of the rest-api component instead.
* RMI is removed ([OFBIZ-13821](https://issues.apache.org/jira/browse/OFBIZ-13821)); it had been disabled since
  [OFBIZ-6942](https://issues.apache.org/jira/browse/OFBIZ-6942). The `rmi` service engine, the `main-rmi` and
  `rita-rmi` service locations, the `rmi` start loader with its `rmi-dispatcher` and `naming-container` containers,
  `rmi.properties`, the RMI keystores (`ofbizrmi.jks`, `ofbizrmi-truststore.jks`, `rmitrust.jks`), the
  `ofbiz.rmi.keyStore*` properties of `jsse.properties` and `remotedispatcher.exportall` are gone. Remove `rmi` from
  the `loaders` of your own containers and from any `-Dofbiz.start.loaders` value; services declared with
  `engine="rmi"` no longer run. Use the REST API for remote access to services.
* Entity definitions: `encrypt="salt"` is removed; use `encrypt="true"`, which now gives the same randomized
  encryption ([#2055](https://github.com/apache/ofbiz-framework/pull/2055)).
* Widgets: `<if-entity-permission>` is removed from `widget-common.xsd`
  ([#1235](https://github.com/apache/ofbiz-framework/pull/1235)).
* Tests: `<groovy-test-suite>` is removed from `test-suite.xsd` (commit 5a67b40971), and the `createTestReports`
  Gradle task is replaced by `createTestReport` and `createFramedTestReport`
  ([#1603](https://github.com/apache/ofbiz-framework/pull/1603)).
* Security: the login services check `SECURITY_CREATE` and `SECURITY_PWD_UPDATE` instead of `PARTYMGR_CREATE` and
  `PARTYMGR_UPDATE` to create a user login for another party or change another user's password. Grant the new
  permissions to the security groups that need them
  ([#1276](https://github.com/apache/ofbiz-framework/pull/1276)).
* Webtools: the JSP entity tools are removed: the `ModelWriter` and `ModelGroupWriter` requests and the
  `tablesMySql`, `dataMySql`, `droptablesMySql`, `droptablesplain`, `ModelWriter` and `ModelGroupWriter` views
  ([OFBIZ-13817](https://issues.apache.org/jira/browse/OFBIZ-13817)). Use "Induce Model XML from Database", the entity
  reference and the SQL processor instead.
* Webtools: the `TestService`, `streamTest`, `yahoo` and `chain` sample requests are removed, as are the
  `serviceStreamTest` test service and the `serviceStreamInterface` service interface
  ([OFBIZ-13817](https://issues.apache.org/jira/browse/OFBIZ-13817)).
* EntitySync no longer synchronizes data ([OFBIZ-13820](https://issues.apache.org/jira/browse/OFBIZ-13820)):
  the remote push and pull and the offline export and import are removed, with the `runEntitySync`,
  `runPullEntitySync`, `pullAndReportEntitySyncData`, `storeEntitySyncData`, `remote*EntitySyncData*`,
  `runOfflineEntitySync`, `loadOfflineEntitySyncData`, `updateOfflineEntitySync` and `resetEntitySyncStatus`
  services, the `entity-sync-rmi` service location and the Webtools Entity Sync Status page. The EntitySync records
  stay and can still select the entities to export in the Webtools XML Data Export page; to move data between
  instances, use the XML export and import.

### Custom code

* Jakarta EE: replace `javax.servlet`, `javax.el`, `javax.mail`, `javax.transaction`, `javax.ws.rs` and
  `javax.inject` with their `jakarta.*` packages ([OFBIZ-12989](https://issues.apache.org/jira/browse/OFBIZ-12989),
  [#1527](https://github.com/apache/ofbiz-framework/pull/1527),
  [#2413](https://github.com/apache/ofbiz-framework/pull/2413)). Tools such as OpenRewrite or the Eclipse
  Transformer can do most of this. The `request` attribute of the `userLogin` service is now a
  `jakarta.servlet.http.HttpServletRequest`.
* Entity engine:
  * `Delegator` gains `createAllByBatchProcess`, and `GenericHelper` gains `createAll`
    ([#857](https://github.com/apache/ofbiz-framework/pull/857)); custom implementations must add them.
  * `EntityQuery.use` takes a `DelegatorProvider`; recompile code that calls it
    ([#923](https://github.com/apache/ofbiz-framework/pull/923)).
  * `GenericDelegator` does its start-up work in `initialize()`; a custom `DelegatorFactory` must call it
    ([#2537](https://github.com/apache/ofbiz-framework/pull/2537)).
  * `ModelEntity` and `ModelViewEntity` lose most public constructors; use
    `ModelEntity.createFromDatabaseMetaData` ([#2536](https://github.com/apache/ofbiz-framework/pull/2536)).
  * Condition lists and field maps are immutable, `EntityUtil.makeFields` takes `Object...`, and the varargs
    `EntityFieldMap` constructor is removed ([#2462](https://github.com/apache/ofbiz-framework/pull/2462)).
  * `EntityDataLoader.loadData` returns `long` ([#1460](https://github.com/apache/ofbiz-framework/pull/1460)).
* Service engine:
  * `LocalDispatcher` gains `runAsyncTracked` ([#904](https://github.com/apache/ofbiz-framework/pull/904)) and
    `initDispatchContext` (commit b3ff76a0a8, [OFBIZ-13153](https://issues.apache.org/jira/browse/OFBIZ-13153)).
  * `ModelService` validation methods take a `LocalDispatcher`
    ([#869](https://github.com/apache/ofbiz-framework/pull/869)), and `getAction`/`setAction` are removed
    ([#1354](https://github.com/apache/ofbiz-framework/pull/1354)).
  * `ServiceUtil.getPartyIdCheckSecurity` is removed ([#1260](https://github.com/apache/ofbiz-framework/pull/1260)).
  * In Groovy services, `makeValue` returns a `GenericValue`, and `success` and `error` return a `Map`
    ([#2096](https://github.com/apache/ofbiz-framework/pull/2096)).
  * The SOAP classes are removed: `SOAPEventHandler`, `SOAPClientEngine` and `SoapSerializer`, together with
    `ModelService.toWSDL`, `getWSDL`, `getTypes`, `getNameSpace`, `setNameSpace`, `TNS` and `XSD`,
    `ModelParam.getWSDLPart` and `DispatchContext.getWSDL`
    ([OFBIZ-13821](https://issues.apache.org/jira/browse/OFBIZ-13821)). The axis2, wsdl4j, `javax.xml.soap-api` and
    `geronimo-jaxrpc_1.1_spec` libraries are no longer on the classpath.
  * The RMI classes are removed: the `org.apache.ofbiz.service.rmi` package (`RemoteDispatcher` and its
    implementation, `RmiServiceContainer`, `RmiServiceEngine`, the SSL socket factories and `ExampleRemoteClient`),
    `NamingServiceContainer` and `RMIExtendedSocketFactory`
    ([OFBIZ-13821](https://issues.apache.org/jira/browse/OFBIZ-13821)).
* Security:
  * A custom `Security` implementation is registered through the new `SecurityProvider` SPI instead of
    `Security.setDelegator` ([#2477](https://github.com/apache/ofbiz-framework/pull/2477)).
  * `SSLUtil` takes a trust `KeyStore` instead of a boolean, and the accept-any-certificate options of `SSLUtil`,
    `HttpClient` and `URLConnector` are removed ([#2190](https://github.com/apache/ofbiz-framework/pull/2190)).
  * `DesCrypt` and the legacy `EntityCrypto` and `HashCrypt` formats are removed
    ([#2167](https://github.com/apache/ofbiz-framework/pull/2167)). See "Data" below.
  * `JWTManager.validateToken` takes a `Delegator`, and `getJWTKey` is removed
    ([#918](https://github.com/apache/ofbiz-framework/pull/918)).
  * `UtilCodec.encodeUrl(String, Map)` and `SecuredUpload.isValidText(String, List, boolean)` are removed
    ([#2367](https://github.com/apache/ofbiz-framework/pull/2367)).
* Widgets and rendering:
  * Several model classes are final or have restricted constructors: forms
    ([#2485](https://github.com/apache/ofbiz-framework/pull/2485),
    [#2483](https://github.com/apache/ofbiz-framework/pull/2483)), screens, menus, trees and themes
    ([#2493](https://github.com/apache/ofbiz-framework/pull/2493)). Extend them through the widget XML instead.
  * The visitors and renderers gain abstract methods for the date range picker
    ([#879](https://github.com/apache/ofbiz-framework/pull/879)) and verbatim FreeMarker
    ([#2334](https://github.com/apache/ofbiz-framework/pull/2334)), and the FreeMarker rendering methods take a
    `Locale` ([#933](https://github.com/apache/ofbiz-framework/pull/933)).
  * The macro renderers no longer compile strings as templates
    ([#2334](https://github.com/apache/ofbiz-framework/pull/2334),
    [#2330](https://github.com/apache/ofbiz-framework/pull/2330)).
  * The HTML menu and tree renderers are removed; use the macro renderers
    ([#2478](https://github.com/apache/ofbiz-framework/pull/2478)).
  * `LookupField.getTargetParameterList` is removed ([#2461](https://github.com/apache/ofbiz-framework/pull/2461)).
  * `UelFunctions.getFunctionMapper` returns `UelFunctions.Functions`, which is final, and `setFunction` is removed
    (commit c7f1ea4d18, [OFBIZ-13372](https://issues.apache.org/jira/browse/OFBIZ-13372)).
* REST: `OpenApiUtil`, `OFBizApiConfig`, the REST `Error` response and the authentication and OpenAPI resources
  changed signatures, and the classes behind `export="true"` endpoints are removed
  ([#1568](https://github.com/apache/ofbiz-framework/pull/1568),
  [#1575](https://github.com/apache/ofbiz-framework/pull/1575),
  [#1609](https://github.com/apache/ofbiz-framework/pull/1609),
  [#1675](https://github.com/apache/ofbiz-framework/pull/1675),
  [#1790](https://github.com/apache/ofbiz-framework/pull/1790),
  [#2474](https://github.com/apache/ofbiz-framework/pull/2474)).
* Other framework classes:
  * `EntitySyncContext` and all `EntitySyncServices` methods except `cleanSyncRemoveInfo`; the entity names of an
    EntitySync are available from `EntityGroupUtil.getEntityNamesByEntitySync`
    ([OFBIZ-13820](https://issues.apache.org/jira/browse/OFBIZ-13820)).
  * minilang simple-map operations ([#2499](https://github.com/apache/ofbiz-framework/pull/2499)).
  * `ArtifactInfoFactory` ([#2504](https://github.com/apache/ofbiz-framework/pull/2504)).
  * `JmsListenerFactory` ([#2517](https://github.com/apache/ofbiz-framework/pull/2517)).
  * the temporal expressions ([#2518](https://github.com/apache/ofbiz-framework/pull/2518)).
  * `FlexibleServletAccessor` and `ConfigXMLReader`
    ([#2498](https://github.com/apache/ofbiz-framework/pull/2498)).
  * the null entity sentinels ([#2535](https://github.com/apache/ofbiz-framework/pull/2535)).
  * `OfbizUrlBuilder` ([#841](https://github.com/apache/ofbiz-framework/pull/841)).
  * `UtilHttp.getMultiPartParameterMap` ([#2429](https://github.com/apache/ofbiz-framework/pull/2429)).
  * `FindServices.createConditionList` ([#860](https://github.com/apache/ofbiz-framework/pull/860)).
  * the JUnit suite wrapper ([#1648](https://github.com/apache/ofbiz-framework/pull/1648)).
  * the image classes, now on jdom2 ([#819](https://github.com/apache/ofbiz-framework/pull/819)).
  * the unused members removed by [#2482](https://github.com/apache/ofbiz-framework/pull/2482).
  * the `org.apache.ofbiz.datafile` package ([OFBIZ-13815](https://issues.apache.org/jira/browse/OFBIZ-13815)).
  * the `org.apache.ofbiz.webapp.webdav` package, with the `requestHandlerFactoryClass` servlet parameter it read
    ([OFBIZ-13816](https://issues.apache.org/jira/browse/OFBIZ-13816)).
  * the EOModel export: `UtilPlist`, `ModelEntity.writeEoModelText` and `createEoModelMap`, the `createEoModelMap`
    and `writeServiceCallGraphEoModel` methods of the webtools artifact info classes,
    `ServiceEcaAction.getShortDisplayDescription`, and the `exportEntityEoModelBundle` and
    `exportServiceEoModelBundle` services and their webtools pages
    ([OFBIZ-13817](https://issues.apache.org/jira/browse/OFBIZ-13817)).

### Themes

These apply to custom themes and to themes that override the common-theme macro libraries:

* `renderTextField` gains `type`, `pattern`, `required`, `step`, `min` and `max` parameters, and `renderAsterisks`
  drops `requiredStyle` ([#862](https://github.com/apache/ofbiz-framework/pull/862),
  [#965](https://github.com/apache/ofbiz-framework/pull/965)).
* `renderScreenletBegin` gains a `screenHeader` parameter ([#1342](https://github.com/apache/ofbiz-framework/pull/1342)).
* The theme header no longer gets `person` and `partyGroup`, and `defaultOrganizationPartyGroupName` is renamed
  `defaultOrganizationName` ([#1270](https://github.com/apache/ofbiz-framework/pull/1270)).
* JavaScript libraries come from npm, so the `/common/js/jquery/...` paths are gone
  ([#1146](https://github.com/apache/ofbiz-framework/pull/1146)), as are the `moment-timezone` files
  ([#879](https://github.com/apache/ofbiz-framework/pull/879)). jQuery moves from 3 to 4, which removes deprecated
  jQuery APIs.
* The `ServiceNotification` email screen moves to `component://common/widget/EmailScreens.xml`
  ([#1398](https://github.com/apache/ofbiz-framework/pull/1398)).

### Data

* Encrypted fields: values written with the older cipher configuration still decrypt, but run the `reencryptFields`
  service after the upgrade to rewrite them with the current one
  ([#2055](https://github.com/apache/ofbiz-framework/pull/2055); see "Entity field encryption" in the security
  documentation).
* `EntityCrypto` no longer reads values and keys in the salted-base64 and legacy hash formats that came before its
  Shiro-based handler, nor keys encrypted with `DesCrypt`
  ([#2167](https://github.com/apache/ofbiz-framework/pull/2167)). If your database was created by an old OFBiz
  release, make sure it holds none of these before you upgrade.
* New framework entities, created automatically at start-up: `JobTracker`, `TrackedJobResult` and their views,
  `MailSmtpConfig`, `MailDkimConfig`, `SecretAuditLog` and `ShortenedPath`.
* The OLAP/BI entity group (`EntityGroup` 6800) is removed from the seed data
  ([#2479](https://github.com/apache/ofbiz-framework/pull/2479)).
* `DATAFILE_MAINT` is no longer seeded; existing permission rows for it are harmless and can be deleted
  ([OFBIZ-13815](https://issues.apache.org/jira/browse/OFBIZ-13815)).
* EntitySync: the run status, history and remote target fields (`runStatusId`, `targetServiceName`,
  `targetDelegatorName`, `forPullOnly`, `forPushOnly` and the `EntitySyncHistory` records) are no longer written.
  Delete any `JobSandbox` record that runs `runEntitySync` or `runPullEntitySync`. Job 8200 (`cleanSyncRemoveInfo`)
  stays ([OFBIZ-13820](https://issues.apache.org/jira/browse/OFBIZ-13820)).
