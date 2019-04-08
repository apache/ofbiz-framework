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

<#if currentSearchCategory??>
  <div id="layeredNav" class="screenlet">
    <div class="screenlet-title-bar">
      <ul>
        <li class="h3">${uiLabelMap.EcommerceLayeredNavigation}</li>
      </ul>
    </div>
    <#escape x as x?xml>
      <#if productCategory.productCategoryId != currentSearchCategory.productCategoryId>
        <#assign currentSearchCategoryName = categoryContentWrapper.get("CATEGORY_NAME", "html")?string />
        <#list searchConstraintStrings as searchConstraintString>
          <#if searchConstraintString.indexOf(currentSearchCategoryName) != -1>
            <div id="searchConstraints">&nbsp;
              <a href="<@ofbizUrl>category/~category_id=${productCategoryId}?removeConstraint=${searchConstraintString_index}&amp;clearSearch=N<#if previousCategoryId??>&amp;searchCategoryId=${previousCategoryId}</#if></@ofbizUrl>"
                  class="buttontext">X</a>
                <#noescape>&nbsp;${searchConstraintString}</#noescape>
            </div>
          </#if>
        </#list>
      </#if>
    </#escape>
    <#list searchConstraintStrings as searchConstraintString>
      <#if searchConstraintString.indexOf("Category: ") = -1 && searchConstraintString != "Exclude Variants">
        <div id="searchConstraints">&nbsp;
          <a href="<@ofbizUrl>category/~category_id=${productCategoryId}?removeConstraint=${searchConstraintString_index}&amp;clearSearch=N<#if currentSearchCategory??>&amp;searchCategoryId=${currentSearchCategory.productCategoryId}</#if></@ofbizUrl>"
              class="buttontext">
            X
          </a>&nbsp;
          ${searchConstraintString}
        </div>
      </#if>
    </#list>
    <#if showSubCats>
      <div id="searchFilter">
        <strong>${uiLabelMap.ProductCategories}</strong>
        <ul>
          <#list subCategoryList as category>
            <#assign subCategoryContentWrapper = category.categoryContentWrapper />
            <#assign categoryName = subCategoryContentWrapper.get("CATEGORY_NAME", "html")!?string />
            <li>
              <a href="<@ofbizUrl>category/~category_id=${productCategoryId}?SEARCH_CATEGORY_ID${index}=${category.productCategoryId}&amp;searchCategoryId=${category.productCategoryId}&amp;clearSearch=N</@ofbizUrl>">
                ${categoryName!} (${category.count})
              </a>
            </li>
          </#list>
        </ul>
      </div>
    </#if>
    <#if showColors>
      <div id="searchFilter">
        <strong>${colorFeatureType.description}</strong>
        <ul>
          <#list colors as color>
            <li>
              <a href="<@ofbizUrl>category/~category_id=${productCategoryId}?pft_${color.productFeatureTypeId}=${color.productFeatureId}&amp;clearSearch=N<#if currentSearchCategory??>&amp;searchCategoryId=${currentSearchCategory.productCategoryId}</#if></@ofbizUrl>">
                ${color.description} (${color.featureCount})
              </a>
            </li>
          </#list>
        </ul>
      </div>
    </#if>
    <#if showPriceRange>
      <div id="searchFilter">
        <strong>${uiLabelMap.EcommercePriceRange}</strong>
        <ul>
          <#list priceRangeList as priceRange>
            <li>
              <a href="<@ofbizUrl>category/~category_id=${productCategoryId}?LIST_PRICE_LOW=${priceRange.low}&amp;LIST_PRICE_HIGH=${priceRange.high}&amp;clearSearch=N<#if currentSearchCategory??>&amp;searchCategoryId=${currentSearchCategory.productCategoryId}</#if></@ofbizUrl>">
                <@ofbizCurrency amount=priceRange.low /> - <@ofbizCurrency amount=priceRange.high /> (${priceRange.count})
              </a>
            <li>
          </#list>
        </ul>
      </div>
    </#if>
  </div>
</#if>
