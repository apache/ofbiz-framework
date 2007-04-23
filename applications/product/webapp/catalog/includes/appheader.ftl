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

<#assign selected = headerItem?default("void")>

<div id="app-navigation">
  <h2>${uiLabelMap.ProductCatalogManagerApplication}</h2>
  <ul>
    <li<#if selected = "main"> class="selected"</#if>><a href="<@ofbizUrl>main</@ofbizUrl>">${uiLabelMap.ProductMain}</a></li>
    <li<#if selected = "featurecats"> class="selected"</#if>><a href="<@ofbizUrl>EditFeatureCategories</@ofbizUrl>">${uiLabelMap.ProductFeatureCats}</a></li>
    <li<#if selected = "promos"> class="selected"</#if>><a href="<@ofbizUrl>FindProductPromo</@ofbizUrl>">${uiLabelMap.ProductPromos}</a></li>
    <li<#if selected = "pricerules"> class="selected"</#if>><a href="<@ofbizUrl>FindProductPriceRules</@ofbizUrl>">${uiLabelMap.ProductPriceRules}</a></li>
    <li<#if selected = "store"> class="selected"</#if>><a href="<@ofbizUrl>FindProductStore</@ofbizUrl>">${uiLabelMap.ProductStores}</a></li>
    <li<#if selected = "thesaurus"> class="selected"</#if>><a href="<@ofbizUrl>editKeywordThesaurus</@ofbizUrl>">${uiLabelMap.ProductThesaurus}</a></li>
    <li<#if selected = "reviews"> class="selected"</#if>><a href="<@ofbizUrl>pendingReviews</@ofbizUrl>">${uiLabelMap.ProductReviews}</a></li>
    <li<#if selected = "configs"> class="selected"</#if>><a href="<@ofbizUrl>FindProductConfigItems</@ofbizUrl>">${uiLabelMap.ProductConfigItems}</a></li>
    <li<#if selected = "Subscription"> class="selected"</#if>><a href="<@ofbizUrl>FindSubscription</@ofbizUrl>">${uiLabelMap.ProductSubscriptions}</a></li>
    <li<#if selected = "shipping"> class="selected"</#if>><a href="<@ofbizUrl>ListShipmentMethodTypes</@ofbizUrl>">${uiLabelMap.ProductShipping}</a></li>
    <#if userLogin?has_content>
      <li class="opposed"><a href="<@ofbizUrl>logout</@ofbizUrl>">${uiLabelMap.CommonLogout}</a></li>
    <#else>
      <li class="opposed"><a href="<@ofbizUrl>${checkLoginUrl?if_exists}</@ofbizUrl>">${uiLabelMap.CommonLogin}</a></li>
    </#if>
  </ul>
  <br class="clear" />
</div>
