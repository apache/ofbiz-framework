<#--
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
<#assign unselectedClassName = "">
<#assign selectedClassMap = {page.tabButtonItem?default("void") : "selected"}>
<#if productStoreId?has_content>
    <br/>
    <div class="button-bar tab-bar">
        <ul>
            <li><a href="<@ofbizUrl>EditProductStore?productStoreId=${productStoreId}</@ofbizUrl>" class="${selectedClassMap.EditProductStore?default(unselectedClassName)}">${uiLabelMap.ProductStore}</a></li>
            <li><a href="<@ofbizUrl>EditProductStoreRoles?productStoreId=${productStoreId}</@ofbizUrl>" class="${selectedClassMap.EditProductStoreRoles?default(unselectedClassName)}">${uiLabelMap.PartyRoles}</a></li>
            <li><a href="<@ofbizUrl>EditProductStorePromos?productStoreId=${productStoreId}</@ofbizUrl>" class="${selectedClassMap.EditProductStorePromos?default(unselectedClassName)}">${uiLabelMap.ProductPromos}</a></li>
            <li><a href="<@ofbizUrl>EditProductStoreCatalogs?productStoreId=${productStoreId}</@ofbizUrl>" class="${selectedClassMap.EditProductStoreCatalogs?default(unselectedClassName)}">${uiLabelMap.ProductCatalogs}</a></li>
            <li><a href="<@ofbizUrl>EditProductStoreWebSites?viewProductStoreId=${productStoreId}</@ofbizUrl>" class="${selectedClassMap.EditProductStoreWebSites?default(unselectedClassName)}">${uiLabelMap.ProductWebSites}</a></li>
            <!-- The tax stuff is in the Tax Authority area of the accounting manager, need to re-do this screen to list current tax entries and link to the accmgr screens <a href="<@ofbizUrl>EditProductStoreTaxSetup?productStoreId=${productStoreId}</@ofbizUrl>" class="${selectedClassMap.EditProductStoreTaxSetup?default(unselectedClassName)}">${uiLabelMap.ProductSalesTax}</a> -->
            <li><a href="<@ofbizUrl>EditProductStoreShipSetup?productStoreId=${productStoreId}</@ofbizUrl>" class="${selectedClassMap.EditProductStoreShipSetup?default(unselectedClassName)}">${uiLabelMap.OrderShipping}</a></li>
            <li><a href="<@ofbizUrl>EditProductStoreShipmentCostEstimates?productStoreId=${productStoreId}</@ofbizUrl>" class="${selectedClassMap.EditProductStoreShipmentCostEstimates?default(unselectedClassName)}">${uiLabelMap.ProductViewEstimates}</a></li>
            <li><a href="<@ofbizUrl>EditProductStorePaySetup?productStoreId=${productStoreId}</@ofbizUrl>" class="${selectedClassMap.EditProductStorePaySetup?default(unselectedClassName)}">${uiLabelMap.AccountingPayments}</a></li>
            <li><a href="<@ofbizUrl>EditProductStoreFinAccountSettings?productStoreId=${productStoreId}</@ofbizUrl>" class="${selectedClassMap.EditProductStoreFinAccountSettings?default(unselectedClassName)}">${uiLabelMap.CommonFinAccounts}</a></li>
            <li><a href="<@ofbizUrl>EditProductStoreEmails?productStoreId=${productStoreId}</@ofbizUrl>" class="${selectedClassMap.EditProductStoreEmails?default(unselectedClassName)}">${uiLabelMap.CommonEmails}</a></li>
            <li><a href="<@ofbizUrl>EditProductStoreSurveys?productStoreId=${productStoreId}</@ofbizUrl>" class="${selectedClassMap.EditProductStoreSurveys?default(unselectedClassName)}">${uiLabelMap.CommonSurveys}</a></li>
            <li><a href="<@ofbizUrl>editProductStoreKeywordOvrd?productStoreId=${productStoreId}</@ofbizUrl>" class="${selectedClassMap.EditProductStoreKeywordOvrd?default(unselectedClassName)}">${uiLabelMap.ProductOverride}</a></li>
            <li><a href="<@ofbizUrl>ViewProductStoreSegments?productStoreId=${productStoreId}</@ofbizUrl>" class="${selectedClassMap.ViewProductStoreSegments?default(unselectedClassName)}">${uiLabelMap.ProductSegments}</a></li>
            <li><a href="<@ofbizUrl>EditProductStoreVendorPayments?productStoreId=${productStoreId}</@ofbizUrl>" class="${selectedClassMap.EditProductStoreVendorPayments?default(unselectedClassName)}">${uiLabelMap.ProductVendorPayments}</a></li>
            <li><a href="<@ofbizUrl>EditProductStoreVendorShipments?productStoreId=${productStoreId}</@ofbizUrl>" class="${selectedClassMap.EditProductStoreVendorShipments?default(unselectedClassName)}">${uiLabelMap.ProductVendorShipments}</a></li>
        </ul>
      <br/>
    </div>
    <br/>
</#if>