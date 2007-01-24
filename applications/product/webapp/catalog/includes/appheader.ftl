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

<#if (requestAttributes.uiLabelMap)?exists><#assign uiLabelMap = requestAttributes.uiLabelMap></#if>
<#if (requestAttributes.security)?exists><#assign security = requestAttributes.security></#if>
<#if (requestAttributes.userLogin)?exists><#assign userLogin = requestAttributes.userLogin></#if>
<#if (requestAttributes.checkLoginUrl)?exists><#assign checkLoginUrl = requestAttributes.checkLoginUrl></#if>

<#assign unselectedLeftClassName = "headerButtonLeft">
<#assign unselectedRightClassName = "headerButtonRight">
<#assign selectedLeftClassMap = {page.headerItem?default("void") : "headerButtonLeftSelected"}>
<#assign selectedRightClassMap = {page.headerItem?default("void") : "headerButtonRightSelected"}>

<div class="apptitle">${uiLabelMap.ProductCatalogManagerApplication}</div>
<div class="row">
  <div class="col"><a href="<@ofbizUrl>main</@ofbizUrl>" class="${selectedLeftClassMap.main?default(unselectedLeftClassName)}">${uiLabelMap.ProductMain}</a></div>  
  <div class="col"><a href="<@ofbizUrl>EditFeatureCategories</@ofbizUrl>" class="${selectedLeftClassMap.featurecats?default(unselectedLeftClassName)}">${uiLabelMap.ProductFeatureCats}</a></div>
  <div class="col"><a href="<@ofbizUrl>FindProductPromo</@ofbizUrl>" class="${selectedLeftClassMap.promos?default(unselectedLeftClassName)}">${uiLabelMap.ProductPromos}</a></div>
  <div class="col"><a href="<@ofbizUrl>FindProductPriceRules</@ofbizUrl>" class="${selectedLeftClassMap.pricerules?default(unselectedLeftClassName)}">${uiLabelMap.ProductPriceRules}</a></div>
  <div class="col"><a href="<@ofbizUrl>FindProductStore</@ofbizUrl>" class="${selectedLeftClassMap.store?default(unselectedLeftClassName)}">${uiLabelMap.ProductStores}</a></div>
  <div class="col"><a href="<@ofbizUrl>editKeywordThesaurus</@ofbizUrl>" class="${selectedLeftClassMap.thesaurus?default(unselectedLeftClassName)}">${uiLabelMap.ProductThesaurus}</a></div>
  <div class="col"><a href="<@ofbizUrl>pendingReviews</@ofbizUrl>" class="${selectedLeftClassMap.reviews?default(unselectedLeftClassName)}">${uiLabelMap.ProductReviews}</a></div>
  <div class="col"><a href="<@ofbizUrl>FindProductConfigItems</@ofbizUrl>" class="${selectedLeftClassMap.configs?default(unselectedLeftClassName)}">${uiLabelMap.ProductConfigItems}</a></div>
  <div class="col"><a href="<@ofbizUrl>FindSubscription</@ofbizUrl>" class="${selectedLeftClassMap.Subscription?default(unselectedLeftClassName)}">${uiLabelMap.ProductSubscriptions}</a></div>

  <#if userLogin?has_content>
    <div class="col-right"><a href="<@ofbizUrl>logout</@ofbizUrl>" class="${selectedRightClassMap.logout?default(unselectedRightClassName)}">${uiLabelMap.CommonLogout}</a></div>
  <#else>
    <div class="col-right"><a href="<@ofbizUrl>${checkLoginUrl?if_exists}</@ofbizUrl>" class="${selectedRightClassMap.login?default(unselectedRightClassName)}">${uiLabelMap.CommonLogin}</a></div>
  </#if>
  <div class="col-fill">&nbsp;</div>
</div>
