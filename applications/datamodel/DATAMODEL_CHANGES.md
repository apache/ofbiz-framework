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
Apache OFBiz® Data Model Changes
=============
Apache OFBiz follows **The Universal Data Model** by **Len Silverston**, with a grain of salt.

The following file contains information about the data model changes in the Apache OFBiz.
The detailed description of migration scripts specified here can be found at [Revisions Requiring Data Migration - upgrade ofbiz](https://cwiki.apache.org/confluence/x/LoBr) page.

##Changes with OFBiz 17
Field types "id-ne", "id-long-ne" & "id-vlong-ne" has been removed. Use "id", "id-long" and "id-vlong" instead (detailed description at [OFBIZ-9351](https://issues.apache.org/jira/browse/OFBIZ-9351)).

###Entity Changes
No changes

###Field Changes

| Entity  | Field  | Action | IsPK | Revision |
|:------------- |:---------------:|:---------------:|:---------------:| -------------:|
| MarketingCampaignPrice | fromDate | Added | Yes | R1805961 |
| MarketingCampaignPrice | thruDate | Added | No | R1805961 |
| MarketingCampaignPromo | fromDate | Added | Yes | R1805961 |
| MarketingCampaignPromo | thruDate | Added | No | R1805961 |
| MarketingCampaignRole | fromDate | Added | Yes | R1805961 |
| MarketingCampaignRole | thruDate | Added | No | R1805961 |
| Product | manufacturerPartyId | Removed | No| R1804408 |
| SecurityGroupPermission | fromDate | Added | Yes | R1812383 |
| SecurityGroupPermission | thruDate | Added | No | R1812383 |

###Migration Scripts
1. Updated sql-type for date-time and time field in fieldtypemysql.xml file  
R1793300 "Update msyql sql-type for datetime field-type to support Fractional Seconds in Time Values    
Please upgrade mysql to at least 5.6.4 or higher.  
After upgrade run 'generateMySqlFileWithAlterTableForTimestamps' service, groupName is required field for this service,  
It will generate sql file with alter query statement for date-time and time field at location "${ofbiz.home}/runtime/tempfiles/<groupName>.sql"  
You can use execute sql statement from any of the mysql batch command.    
    
##Changes between OFBiz 9 to OFBiz 16

###Entity Changes
**Added 77 new entities**

1. JobRequisition
2. ProductAverageCostType
3. WorkEffortSurveyAppl
4. WorkEffortIcalData
5. WebSiteContactList
6. WebAnalyticsType
7. WebAnalyticsConfig
8. UserLoginSecurityQuestion
9. UomGroup
10. TrainingRequest
11. ThirdPartyLogin
12. TestFieldType
13. TestingSubtype
14. TestingStatus
15. TestingRemoveAll
16. TestingItem
17. TestingCrypto
18. SystemProperty
19. ShipmentGatewayUsps
20. ShipmentGatewayUps
21. ShipmentGatewayFedex
22. ShipmentGatewayDhl
23. ShipmentGatewayConfig
24. ShipmentGatewayConfigType
25. ReturnContactMech
26. QuoteNote
27. ProductPromoContent
28. ProductPromoContentType
29. ProductGroupOrder
30. ProductCostComponentCalc
31. CostComponentCalc
32. PayPalPaymentMethod
33. PaymentGroupType
34. PaymentGroup
35. PaymentGroupMember
36. PaymentGatewayConfig
37. PaymentGatewayConfigType
38. PaymentGatewayWorldPay
39. PaymentGatewaySecurePay
40. PaymentGatewaySagePay
41. PaymentGatewayOrbital
42. PaymentGatewayEway
43. PaymentGatewayCyberSource
44. PaymentGatewayAuthorizeNet
45. PaymentGatewayIDEAL
46. PaymentContentType
47. PaymentContent
48. OAuth2LinkedIn
49. OAuth2GitHub
50. JobManagerLock
51. JobInterviewType
52. JobInterview
53. JavaResource
54. InvoiceNote
55. InvoiceItemAssocType
56. InvoiceItemAssoc
57. InvoiceContentType
58. InvoiceContent
59. GlAccountCategoryType
60. GlAccountCategoryMember
61. GlAccountCategory
62. GitHubUser
63. FixedAssetTypeGlAccount
64. FacilityContent
65. ExcelImportHistory
66. EmplLeaveReasonType
67. EbayShippingMethod
68. EbayConfig
69. CountryAddressFormat
70. ContentSearchResult
71. ContentSearchConstraint
72. ContentKeyword
73. CheckAccount
74. AgreementFacilityAppl
75. AgreementContentType
76. AgreementContent

**Removed 8 entities**

1. DepreciationMethod
2. FixedAssetMaintMeter
3. OagisMessageErrorInfo
4. OagisMessageInfo
5. SalesOpportunityTrackingCode
6. SimpleSalesTaxLookup
7. TestBlob
8. WorkEffortAssignmentRate


###Field Changes
| Entity  | Field  | Action | IsPK | Revision |
|:------------- |:---------------:|:---------------:|:---------------:| -------------:|
| AcctgTransAttribute | attrDescription | Added | No| |
| AcctgTransEntry | inventoryItemId | Added | No| |
| AcctgTransTypeAttr | description | Added | No| |
| BenefitType | parentTypeId | Added | No| |
| BenefitType | hasTable | Added | No| |
| BudgetAttribute | attrDescription | Added | No| |
| BudgetItemAttribute | attrDescription | Added | No| |
| BudgetItemTypeAttr | description | Added | No| |
| BudgetStatus | changeByUserLoginId | Added | No| |
| BudgetTypeAttr | description | Added | No| |
| CommunicationEventRole | statusId | Added | No| |
| CommunicationEventType | contactMechTypeId | Added | No| |
| ContactListCommStatus | partyId | Added | No| |
| ContactListCommStatus | messageId | Added | No| |
| ContactListCommStatus | changeByUserLoginId | Added | No| |
| ContactMechAttribute | attrDescription | Added | No| |
| ContactMechTypeAttr | description | Added | No| |
| DeductionType | parentTypeId | Added | No| |
| DeductionType | hasTable | Added | No| |
| DocumentAttribute | attrDescription | Added | No| |
| DocumentTypeAttr | description | Added | No| |
| EmploymentApp | approverPartyId | Added | No| |
| EmploymentApp | jobRequisitionId | Added | No| |
| EmploymentAppSourceType | parentTypeId | Added | No| |
| EmploymentAppSourceType | hasTable | Added | No| |
| EmplPositionClassType | parentTypeId | Added | No| |
| EmplPositionClassType | hasTable | Added | No| |
| EmplPositionType | parentTypeId | Added | No| |
| EmplPositionType | hasTable | Added | No| |
| EmplPositionType | partyId | Removed | No| |
| EmplPositionType | roleTypeId | Removed | No| |
| FinAccountAttribute | attrDescription | Added | No| |
| FinAccountTransAttribute | attrDescription | Added | No| |
| FinAccountTrans | glReconciliationId | Added | No| |
| FinAccountTrans | statusId | Added | No| |
| FinAccountTransTypeAttr | description | Added | No| |
| FinAccountTypeAttr | description | Added | No| |
| FinAccountStatus | changeByUserLoginId | Added | No| |
| FixedAsset | acquireOrderId | Added | No| |
| FixedAsset | acquireOrderItemSeqId | Added | No| |
| FixedAssetAttribute | attrDescription | Added | No| |
| FixedAssetTypeAttr | description | Added | No| |
| GlAccount | externalId | Added | No| |
| GlAccount | openingBalance | Added | No| |
| GlReconciliation | createdDate | Added | No| |
| GlReconciliation | lastModifiedDate | Added | No| |
| GlReconciliation | statusId | Added | No| |
| GlReconciliation | openingBalance | Added | No| |
| InventoryItemAttribute | attrDescription | Added | No| |
| InventoryItemStatus | changeByUserLoginId | Added | No| |
| InventoryItemTypeAttr | description | Added | No| |
| InvoiceAttribute | attrDescription | Added | No| |
| InvoiceItemAttribute | attrDescription | Added | No| |
| InvoiceItemTypeAttr | description | Added | No| |
| InvoiceStatus | changeByUserLoginId | Added | No| |
| InvoiceTypeAttr | description | Added | No| |
| InvoiceTermAttribute | attrDescription | Added | No| |
| JobSandbox | currentRetryCount | Added | No| |
| JobSandbox | tempExprId | Added | No| |
| JobSandbox | currentRecurrenceCount | Added | No| |
| JobSandbox | maxRecurrenceCount | Added | No| |
| JobSandbox | jobResult | Added | No| |
| OrderAdjustment | amountAlreadyIncluded | Added | No| |
| OrderAdjustment | isManual | Added | No| |
| OrderAdjustment | oldPercentage | Added | No| |
| OrderAdjustment | oldAmountPerQuantity | Added | No| |
| OrderAdjustment | lastModifiedDate | Added | No| |
| OrderAdjustment | lastModifiedByUserLogin | Added | No| |
| OrderAdjustmentAttribute | attrDescription | Added | No| |
| OrderAdjustmentTypeAttr | description | Added | No| |
| OrderAttribute | attrDescription | Added | No| |
| OrderItem | supplierProductId | Added | No| |
| OrderItem | cancelBackOrderDate | Added | No| |
| OrderItem | changeByUserLoginId | Added | No| |
| OrderItemAttribute | attrDescription | Added | No| |
| OrderItemShipGroup | facilityId | Added | No| |
| OrderItemShipGroup | estimatedShipDate | Added | No| |
| OrderItemShipGroup | estimatedDeliveryDate | Added | No| |
| OrderItemShipGrpInvRes | priority | Added | No| |
| OrderItemShipGrpInvRes | oldPickStartDate | Added | No| |
| OrderItemTypeAttr | description | Added | No| | 
| OrderTermAttribute | attrDescription | Added | No| |
| OrderPaymentPreference | track2 | Added | No| |
| OrderPaymentPreference | swipedFlag | Added | No| |
| OrderPaymentPreference | lastModifiedDate | Added | No| |
| OrderPaymentPreference | lastModifiedByUserLogin | Added | No| |
| OrderShipment | shipGroupSeqId | Added | No| |
| OrderTypeAttr | description | Added | No| |
| PartyAcctgPreference | orderSequenceEnumId | Removed | No| |
| PartyAcctgPreference | quoteSequenceEnumId | Removed | No| |
| PartyAcctgPreference | invoiceSequenceEnumId | Removed | No| |
| PartyAcctgPreference | oldOrderSequenceEnumId | Added | No| |
| PartyAcctgPreference | oldQuoteSequenceEnumId | Added | No| |
| PartyAcctgPreference | oldInvoiceSequenceEnumId | Added | No| |
| PartyAcctgPreference | orderSeqCustMethId | Added | No| |
| PartyQual | infoString | Removed | No| |
| PartyQual | institutionInternalId | Removed | No| |
| PartyQual | institutionPartyId | Removed | No| |
| PartyQual | partyQualId | Removed | No| |
| PartyRate | percentageUsed | Added | No| |
| PartyRate | rate | Removed | No| |
| PartyResume | contentId | Added | No| |
| PaymentAttribute | attrDescription | Added | No| |
| PaymentGatewayResponse | gatewayCvResult | Added | No| |
| PaymentMethod | finAccountId | Added | No| |
| PaymentTypeAttr | description | Added | No| |
| PerfRatingType | parentTypeId | Added | No| |
| PerfRatingType | hasTable | Added | No| |
| PerfReview | payHistoryRoleTypeIdTo | Removed | No| |
| PerfReview | payHistoryRoleTypeIdFrom | Removed | No| |
| PerfReview | payHistoryPartyIdTo | Removed | No| |
| PerfReview | payHistoryPartyIdFrom | Removed | No| |
| PerfReview | payHistoryFromDate | Removed | No| |
| PerfReviewItemType | parentTypeId | Added | No| |
| PerfReviewItemType | hasTable | Added | No| |
| PersonTraining | trainingRequestId | Added | No| |
| PersonTraining | workEffortId | Added | No| |
| PersonTraining | approverId | Added | No| |
| PersonTraining | approvalStatus | Added | No| |
| PersonTraining | reason | Added | No| |
| PostalAddress | houseNumber | Added | No| |
| PostalAddress | houseNumberExt | Added | No| |
| PostalAddress | cityGeoId | Added | No| |
| PostalAddress | municipalityGeoId | Added | No| |
| PostalAddress | geoPointId | Added | No| |
| PosTerminal | terminalName | Added | No| |
| PosTerminalInternTx | reasonEnumId | Added | No| |
| Product | releaseDate | Added | No| |
| Product | originalImageUrl | Added | No| |
| Product | inventoryItemTypeId | Added | No| |
| Product | shippingWeight | Added | No| |
| Product | productWeight | Added | No| |
| Product | diameterUomId | Added | No| |
| Product | productDiameter | Added | No| |
| Product | virtualVariantMethodEnum | Added | No| |
| Product | defaultShipmentBoxTypeId | Added | No| |
| Product | lotIdFilledIn | Added | No| |
| Product | orderDecimalQuantity | Added | No| |
| Product | weight | Removed | No| |
| Product | taxCategory | Removed | No| |
| Product | taxVatCode | Removed | No| |
| Product | taxDutyCode | Removed | No| |
| ProductAttribute | attrDescription | Added | No| |
| ProductAverageCost | productAverageCostTypeId | Added | No| |
| ProductAverageCost | facilityId | Added | No| |
| ProductContent | sequenceNum | Added | No| |
| ProductKeyword | keywordTypeId | Added | No| |
| ProductKeyword | statusId | Added | No| |
| ProductRole | sequenceNum | Added | No| |
| ProductStore | balanceResOnOrderCreation | Added | No| |
| ProductStore | defaultTimeZoneString | Added | No| |
| ProductStore | oldStyleSheet | Added | No| |
| ProductStore | oldHeaderLogo | Added | No| |
| ProductStore | oldHeaderRightBackground | Added | No| |
| ProductStore | oldHeaderMiddleBackground | Added | No| |
| ProductStore | styleSheet | Removed | No| |
| ProductStore | headerLogo | Removed | No| |
| ProductStore | headerRightBackground | Removed | No| |
| ProductStore | headerMiddleBackground | Removed | No| |
| ProductStorePaymentSetting | paymentCustomMethodId | Added | No| |
| ProductStorePaymentSetting | paymentGatewayConfigId | Added | No| |
| ProductStoreShipmentMeth | shipmentCustomMethodId | Added | No| |
| ProductStoreShipmentMeth | shipmentGatewayConfigId | Added | No| |
| ProductStoreShipmentMeth | allowancePercent | Added | No| |
| ProductStoreShipmentMeth | minimumPrice | Added | No| |
| ProductTypeAttribute | attrDescription | Added | No|
| QuoteAdjustment | lastModifiedDate | Added | No| |
| QuoteAdjustment | lastModifiedByUserLogin | Added | No| |
| QuoteAttribute | attrDescription | Added | No| |
| QuoteItem | leadTimeDays | Added | No| |
| QuoteRole | fromDate | Added | Yes| |
| QuoteRole | thruDate | Added | No| |
| QuoteTerm | termDays | Added | No| |
| QuoteTerm | textValue | Added | No| |
| QuoteTerm | description | Added | No| |
| QuoteTermAttribute | attrDescription | Added | No| |
| QuoteTypeAttr | description | Added | No| |
| RequirementAttribute | changeByUserLoginId | Added | No| |
| RequirementStatus | changeByUserLoginId | Added | No| |
| ResponsibilityType | parentTypeId | Added | No| |
| ResponsibilityType | hasTable | Added | No| |
| ReturnAdjustment | createdByUserLoginId | Added | No| |
| ReturnAdjustment | lastModifiedDate | Added | No| |
| ReturnAdjustment | lastModifiedByUserLogin | Added | No| |
| ReturnHeader | supplierRmaId | Added | No| |
| ReturnItemResponse | finAccountTransId | Added | No| |
| ReturnStatus | changeByUserLoginId | Added | No| |
| SalaryStep | fromDate | Added | Yes| |
| SalaryStep | thruDate | Added | No| |
| SalaryStep | createdByUserLoginId | Added | No| |
| SalaryStep | lastModifiedByUserLogin | Added | No| |
| SalesOpportunity | nextStepDate | Added | No| |
| ServiceSemaphore | lockedByInstanceId | Added | No| |
| ShoppingListItem | modifiedPrice | Added | No| |
| SkillType | parentTypeId | Added | No| |
| SkillType | hasTable | Added | No| |
| SupplierProduct | shippingPrice | Added | No| |
| SupplierProduct | supplierCommissionPerc | Removed | No| |
| TaxAuthorityRateProduct | isTaxInShippingPrice | Added | No| |
| TerminationType | parentTypeId | Added | No| |
| TerminationType | hasTable | Added | No| |
| TestingNodeMember | extendFromDate | Added | No| |
| TestingNodeMember | extendThruDate | Added | No| |
| TimeEntry | planHour | Added | No| |
| Timesheet | approvedByUserLoginId | Added | No| |
| TrainingClassType | parentTypeId | Added | No| |
| TrainingClassType | hasTable | Added | No| |
| UnemploymentClaim | thruDate | Added | No| |
| UserLogin | externalAuthId | Added | No| |
| UserLogin | userLdapDn | Added | No| |
| UserLogin | disabledBy | Added | No| |
| ValueLinkKey | createdByUserLogin | Added | No| |
| WebSite | visualThemeSetId | Added | No| |
| WebSite | hostedPathAlias | Added | No| |
| WebSite | isDefault | Added | No| |
| WebSite | displayMaintenancePage | Added | No| |
| WebSitePathAlias| fromDate | Added | Yes | R1738588 |
| WebSitePathAlias| thruDate | Added | No | R1738588 |
| WorkEffort | tempExprId | Added | No| |
| WorkEffort | sequenceNum | Added | No| |
| WorkEffortAttribute | attrDescription | Added | No| |
| WorkEffortAssocAttribute | attrDescription | Added | No| |
| WorkEffortAssocTypeAttr | description | Added | No| |
| WorkEffortContactMech | fromDate | Added | Yes| |
| WorkEffortContactMech | thruDate | Added | No| |
| WorkEffortFixedAssetAssign | availabilityStatusId | Added | No| |
| WorkEffortPartyAssignment | assignedByUserLoginId | Added | No| |
| WorkEffortPurposeType | parentTypeId | Added | No| |
| WorkEffortStatus | reason | Added | No| |
| WorkEffortTypeAttr | description | Added | No| |
| WorkOrderItemFulfillment | shipGroupSeqId | Added | No| |
