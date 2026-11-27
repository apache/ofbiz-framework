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

# Upgrading the OFBiz applications

This file lists what you need to change in your configuration, custom code and data when you upgrade the OFBiz
applications to a new major release. Start with the framework notes in [../UPGRADING.md](../UPGRADING.md), which also
cover the environment. The application entity and field changes, with their migration services, are in
[datamodel/DATAMODEL_CHANGES.adoc](datamodel/DATAMODEL_CHANGES.adoc).

When a pull request changes or removes a public API of an application, add its entry here in the same pull request.

## Upgrading from 24.09 to the next major release (trunk)

### Removed integrations

These integrations were removed with their services, entities, seed data, properties, screens and requests. There is
no replacement in OFBiz; a project that still uses one has to keep its own copy.

| Integration | Pull request |
|---|---|
| Payment gateways: Authorize.Net, ClearCommerce, CyberSource, Eway, Orbital, PayPal, Payflow Pro, PCCharge, RiTA, SagePay, SecurePay, WorldPay | [#1371](https://github.com/apache/ofbiz-framework/pull/1371) |
| Shipping carriers: DHL, FedEx, UPS, USPS | [#1335](https://github.com/apache/ofbiz-framework/pull/1335), [#1377](https://github.com/apache/ofbiz-framework/pull/1377) |
| Taxware and ZIPsales tax | [#1380](https://github.com/apache/ofbiz-framework/pull/1380) |
| ValueLink gift cards | [#1149](https://github.com/apache/ofbiz-framework/pull/1149) |
| eBay seed data (`EBAY_SALES_CHANNEL`, `EXT_EBAY`, `PCCT_EBAY_ROOT`) | [#1493](https://github.com/apache/ofbiz-framework/pull/1493) |
| BIRT order report events | [#2476](https://github.com/apache/ofbiz-framework/pull/2476) |
| Work effort iCalendar publishing (`/iCalendar`), with the `WorkEffortIcalData` entity and the `ICAL_URL`, `ICAL_MEMBER` and `PUBLISH_PROPS` seed data | [OFBIZ-13816](https://issues.apache.org/jira/browse/OFBIZ-13816) |

### Services

* `productAvailabalityByFacility` and its request are removed, and the mini-lang file
  `component://order/minilang/order/OrderServices.xml` is replaced by Groovy
  ([#870](https://github.com/apache/ofbiz-framework/pull/870)).
* `findOrders` no longer takes `cardNumber`, since an encrypted field can't be searched by exact match
  ([#2055](https://github.com/apache/ofbiz-framework/pull/2055)).
* `createBOMAssoc` no longer returns `errorMessage` ([#1531](https://github.com/apache/ofbiz-framework/pull/1531)).
* `createCustomTimePeriod` no longer takes `organizationPartyId` (commit 281f31bed0,
  [OFBIZ-13418](https://issues.apache.org/jira/browse/OFBIZ-13418)).
* `timesheetUpdatePermission` is removed; use `timesheetPermissionCheck` instead
  ([#2599](https://github.com/apache/ofbiz-framework/pull/2599)).
* `createWorkEffortICalData`, `updateWorkEffortICalData`, `deleteWorkEffortICalData`, `workEffortICalendarPermission`,
  `getICalWorkEfforts` and `getPartyICalUrl` are removed with the iCalendar webapp
  ([OFBIZ-13816](https://issues.apache.org/jira/browse/OFBIZ-13816)).
* Email address verification works end to end ([OFBIZ-13858](https://issues.apache.org/jira/browse/OFBIZ-13858)).
  `createEmailAddressVerification` takes `partyId` and `contactMechId` and returns the `token`; only its SHA-256 is
  stored, and `EmailAddressVerification` gains `partyId` and `contactMechId`. `sendVerifyEmailAddressNotification`
  and the ECA that called it are replaced by `sendEmailAddressVerification` (`partyId`, `contactMechId`,
  `productStoreId`), which sends the store's `PRDS_EMAIL_VERIFY` email with a link to
  `/control/verifyEmailAddress?token=` in the webapp of the store's web site. `verifyEmailAddress` takes `token`,
  sets `PartyContactMech.verified` to Y and consumes the token; map a `verifyEmailAddress` request with
  `auth="false"` calling it in your store webapp. Changing an email address leaves the new `PartyContactMech`
  unverified. Custom `PRDS_EMAIL_VERIFY` body screens must use `parameters.token` instead of `parameters.verifyHash`.
  The Java method `ContactMechServices.createEmailAddressVerification` is removed. The `verified` flag of
  `PartyContactMech` can no longer be set through the contact mech create and update services: only
  `verifyEmailAddress` sets it, and a changed address starts unverified. `verified=Y` values stored before this
  change were never proven (they could be set freely), so projects that rely on the flag should clear them.
  `createEmailAddressVerification` and `sendEmailAddressVerification` need a logged-in owner of the address or the
  party contact mech permission (`partyContactMechPermissionCheck`), and the latter takes an optional `webSiteId`
  of the product store. It sends nothing when a verification for the same address was issued less than
  `email_verification.resend.minutes` ago (security.properties, default 5, 0 to disable), and it gives the body screen
  `storeName` and `partyName` besides `token`. The labels `PartyEmailAddressVerificationExpired` and
  `PartyRequiredFieldMissingVerifyHash` are removed.
* Portlets for employees are checked with the new `employeePermissionCheck` service of party, which passes for a party
  with the `EMPLOYEE` role or a user with `PORTALPAGE_ADMIN`, instead of `portalPermissionIsEmployee`, which the
  applications do not define. The `ListCustomerInvoices`, `ListCustRequests`, `ListCustomerOrders` and
  `ListCustReturns` portlets, which used `portalPermissionIsCustomer`, are removed with their screens and forms, as is
  the ordermgr `EditRequestCustomer` request
  ([OFBIZ-13825](https://issues.apache.org/jira/browse/OFBIZ-13825)).
* The content `LatestResponses`, `EditBlogImage`, `EditBlogText` and `RespondBlog` requests, which rendered screens of
  a plugin, are removed ([OFBIZ-13867](https://issues.apache.org/jira/browse/OFBIZ-13867)).
* The customer request emails link to `/control/ViewRequest?custRequestId=...` in the webapp of the web site the
  email is sent for, or else of the first web site of the request's product store, and leave the link out when there
  is none; map a `ViewRequest` request there ([OFBIZ-13825](https://issues.apache.org/jira/browse/OFBIZ-13825)).

### Custom code

* `EntityPermissionChecker` moves from `org.apache.ofbiz.entityext.permission` to
  `org.apache.ofbiz.content.permission` ([#1235](https://github.com/apache/ofbiz-framework/pull/1235)).
* `ShoppingCart`:
  * the contact mechanism methods allow several contact mechanisms per purpose and have new signatures (commit
    e67062a6ab, [OFBIZ-13199](https://issues.apache.org/jira/browse/OFBIZ-13199)).
  * `ensureItemsQuantity` is removed (commit 45f6a3e247,
    [OFBIZ-13318](https://issues.apache.org/jira/browse/OFBIZ-13318)).
* `CheckOutEvents.checkExternalCheckout` is removed: it only returned "paypal" for the PayflowPro gateway removed in
  [OFBIZ-13446](https://issues.apache.org/jira/browse/OFBIZ-13446), and "success" otherwise
  ([OFBIZ-13862](https://issues.apache.org/jira/browse/OFBIZ-13862)).
* `ProductConfigWrapper.ConfigItem` and `ConfigOption` are static nested classes with new constructors, and the
  `BOMNode` tree methods changed ([#2509](https://github.com/apache/ofbiz-framework/pull/2509)).
* Manufacturing:
  * `MrpServices.processBomComponent` takes a `facilityId` ([#1474](https://github.com/apache/ofbiz-framework/pull/1474)).
  * `ProposedOrder.calculateStartDate` changed ([#1427](https://github.com/apache/ofbiz-framework/pull/1427)).
  * `TechDataServices.dayStartCapacityAvailable` changed ([#896](https://github.com/apache/ofbiz-framework/pull/896)).
* The carrier and gateway classes listed under "Removed integrations" are gone, together with
  `ShipmentServices.quickScheduleShipmentRouteSegment`, the carrier parameters of `WeightPackageSession`,
  `UtilValidate.isValueLinkCard` and `CatalogWorker.getCatalogTopEbayCategoryId`.
* `OrderViewWebSecure.groovy` lets only the customer who created an order view it when the screen sets
  `customerOrderView` to Y, instead of when the webapp's dispatcher is named `ecommerce`; set it in the order
  screens of a customer facing web site ([OFBIZ-13867](https://issues.apache.org/jira/browse/OFBIZ-13867)).
* `ProductEvents.tellAFriend` no longer falls back to a default body screen; set `bodyScreenLocation` on the
  `PRDS_TELL_FRIEND` email setting of the product store
  ([OFBIZ-13867](https://issues.apache.org/jira/browse/OFBIZ-13867)).
* `ICalConverter`, `ICalRecurConverter`, `ICalWorker` and `ICalHandlerFactory` in
  `org.apache.ofbiz.workeffort.workeffort` are removed with the iCalendar webapp
  ([OFBIZ-13816](https://issues.apache.org/jira/browse/OFBIZ-13816)).
* Several web sites in one webapp, told apart by `WebSite.hostedPathAlias`, are still supported by the link building
  in `RequestHandler` and by the `cms` event, but no servlet filter that enables them ships any more. Such a webapp
  needs its own filter, mapped after the `ContextFilter`, that:
  * sets the `MULTI_SITE_ENABLED` servlet context attribute;
  * selects the `WebSite` whose `hostedPathAlias` matches the first path segment, or else the one with `isDefault`
    set to Y;
  * sets the `webSiteId` request and session attributes and the `displayMaintenancePage` session attribute;
  * wraps the request so that `getPathInfo` no longer contains the alias segment.

  The `ControlServlet` of that webapp must be mapped to `/*`
  ([OFBIZ-13819](https://issues.apache.org/jira/browse/OFBIZ-13819)).

### Data

* Run `migratePersonMaritalStatusType` to move `Person.maritalStatusEnumId`, now `oldMaritalStatusEnumId`, to the
  new `MaritalStatusType` ([OFBIZ-13329](https://issues.apache.org/jira/browse/OFBIZ-13329)). The `MARITAL_STATUS`
  enumerations are removed.
* `FinAccount.finAccountCode` is no longer encrypted. Run `decryptFinAccountCodes` to turn the existing values back
  into plain text ([#2055](https://github.com/apache/ofbiz-framework/pull/2055)).
* The `BLOG_TPL_TOPLEFT` and `BLOG_TPL_TOPCENTER` data resources render
  `component://content/widget/forum/BlogTemplates.xml`. Load the seed data again, or update their `objectInfo`, so that
  existing blog entries keep rendering ([OFBIZ-13870](https://issues.apache.org/jira/browse/OFBIZ-13870)).
* The tables of the removed entities are not dropped. Drop them once you no longer need their data; the list is in
  [datamodel/DATAMODEL_CHANGES.adoc](datamodel/DATAMODEL_CHANGES.adoc).
