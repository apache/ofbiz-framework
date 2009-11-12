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

<#if currentSearchCategory?exists>
  <div id="layeredNav" class="screenlet">
    <h3>Layered Navigation</h3>
    <#escape x as x?xml>
      <#if productCategory.productCategoryId != currentSearchCategory.productCategoryId>
        <#assign currentSearchCategoryName = categoryContentWrapper.get("CATEGORY_NAME")?string />
        <#list searchConstraintStrings as searchConstraintString>
          <#if searchConstraintString.indexOf(currentSearchCategoryName) != -1>
            <div id="searchConstraints">&nbsp;<a href="<@ofbizUrl>category/~category_id=${productCategoryId}?removeConstraint=${searchConstraintString_index}&clearSearch=N<#if previousCategoryId?exists>&searchCategoryId=${previousCategoryId}</#if></@ofbizUrl>" class="buttontext">X</a><#noescape>&nbsp;${searchConstraintString}</#noescape></div>
          </#if>
        </#list>
      </#if>
    </#escape>
    <#list searchConstraintStrings as searchConstraintString>
      <#if searchConstraintString.indexOf("Category: ") = -1 && searchConstraintString != "Exclude Variants">
        <div id="searchConstraints">&nbsp;<a href="<@ofbizUrl>category/~category_id=${productCategoryId}?removeConstraint=${searchConstraintString_index}&clearSearch=N<#if currentSearchCategory?exists>&searchCategoryId=${currentSearchCategory.productCategoryId}</#if></@ofbizUrl>" class="buttontext">X</a>&nbsp;${searchConstraintString}</div>
      </#if>
    </#list>
    <#if showSubCats>
      <div id="searchFilter">
        <strong>${uiLabelMap.ProductCategories}</strong>
        <ul>
          <#list subCategoryList as category>
            <#assign subCategoryContentWrapper = category.categoryContentWrapper />
            <#assign categoryName = subCategoryContentWrapper.get("CATEGORY_NAME")?if_exists?string />
            <li><a href="<@ofbizUrl>category/~category_id=${productCategoryId}?SEARCH_CATEGORY_ID${index}=${category.productCategoryId}&searchCategoryId=${category.productCategoryId}&clearSearch=N</@ofbizUrl>">${categoryName?if_exists} (${category.count})</li>
          </#list>
        </ul>
      </div>
    </#if>
    <#if showColors>
      <div id="searchFilter">
        <strong>${colorFeatureType.description}</strong>
        <ul>
          <#list colors as color>
            <li><a href="<@ofbizUrl>category/~category_id=${productCategoryId}?pft_${color.productFeatureTypeId}=${color.productFeatureId}&clearSearch=N<#if currentSearchCategory?exists>&searchCategoryId=${currentSearchCategory.productCategoryId}</#if></@ofbizUrl>">${color.description} (${color.featureCount})</li>
          </#list>
        </ul>
      </div>
    </#if>
    <#if showPriceRange>
      <div id="searchFilter">
        <strong>${uiLabelMap.EcommercePriceRange}</strong>
        <ul>
          <#list priceRangeList as priceRange>
            <li><a href="<@ofbizUrl>category/~category_id=${productCategoryId}?LIST_PRICE_LOW=${priceRange.low}&LIST_PRICE_HIGH=${priceRange.high}&clearSearch=N<#if currentSearchCategory?exists>&searchCategoryId=${currentSearchCategory.productCategoryId}</#if></@ofbizUrl>"><@ofbizCurrency amount=priceRange.low /> - <@ofbizCurrency amount=priceRange.high /> (${priceRange.count})</a><li>
          </#list>
        </ul>
      </div>
    </#if>
  </div>
</#if>