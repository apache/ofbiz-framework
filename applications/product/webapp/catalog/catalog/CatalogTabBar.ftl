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

<#assign unselectedClassName = "tabButton">
<#assign selectedClassMap = {page.tabButtonItem?default("void") : "tabButtonSelected"}>
<#if prodCatalogId?exists>
    <div class="tabContainer">
        <a href="<@ofbizUrl>EditProdCatalog?prodCatalogId=${prodCatalogId}</@ofbizUrl>" class="${selectedClassMap.ProductCatalog?default(unselectedClassName)}">${uiLabelMap.ProductCatalog}</a>
        <a href="<@ofbizUrl>EditProdCatalogStores?prodCatalogId=${prodCatalogId}</@ofbizUrl>" class="${selectedClassMap.ProductStores?default(unselectedClassName)}">${uiLabelMap.ProductStores}</a>
        <a href="<@ofbizUrl>EditProdCatalogParties?prodCatalogId=${prodCatalogId}</@ofbizUrl>" class="${selectedClassMap.PartyParties?default(unselectedClassName)}">${uiLabelMap.PartyParties}</a>
        <a href="<@ofbizUrl>EditProdCatalogCategories?prodCatalogId=${prodCatalogId}</@ofbizUrl>" class="${selectedClassMap.ProductCategories?default(unselectedClassName)}">${uiLabelMap.ProductCategories}</a>
    </div>
</#if>
