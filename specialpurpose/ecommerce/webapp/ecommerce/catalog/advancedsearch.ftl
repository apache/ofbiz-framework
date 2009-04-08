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
<#assign searchOptionsHistoryList = Static["org.ofbiz.product.product.ProductSearchSession"].getSearchOptionsHistoryList(session)/>
<#assign currentCatalogId = Static["org.ofbiz.product.catalog.CatalogWorker"].getCurrentCatalogId(request)/>
<h1>${uiLabelMap.ProductAdvancedSearchInCategory}</h1>
<br/>
<form name="advtokeywordsearchform" method="post" action="<@ofbizUrl>keywordsearch</@ofbizUrl>" style="margin: 0;">
  <input type="hidden" name="VIEW_SIZE" value="10"/>
  <input type="hidden" name="PAGING" value="Y"/>
  <table width="100%">
    <input type="hidden" name="SEARCH_CATALOG_ID" value="${currentCatalogId}">
    <#if searchCategory?has_content>
        <input type="hidden" name="SEARCH_CATEGORY_ID" value="${searchCategoryId?if_exists}"/>
        <tr valign="middle">
          <td class="label">${uiLabelMap.ProductCategory}</td>
          <td>${(searchCategory.description)?if_exists}</td>
          <td>&nbsp;</td>
        </tr>
        <tr valign="middle">
          <td class="label">${uiLabelMap.ProductIncludeSubCategories}</td>
          <td>
              ${uiLabelMap.CommonYes} <input type="radio" name="SEARCH_SUB_CATEGORIES" value="Y" checked="checked"/>
              ${uiLabelMap.CommonNo} <input type="radio" name="SEARCH_SUB_CATEGORIES" value="N"/>
          </td>
        </tr>
    </#if>
    <tr valign="middle">
      <td class="label">${uiLabelMap.ProductKeywords}</td>
      <td><input type="text" name="SEARCH_STRING" size="40" value="${requestParameters.SEARCH_STRING?if_exists}">
          ${uiLabelMap.CommonAny} <input type="radio" name="SEARCH_OPERATOR" value="OR" <#if searchOperator == "OR">checked</#if>>
          ${uiLabelMap.CommonAll} <input type="radio" name="SEARCH_OPERATOR" value="AND" <#if searchOperator == "AND">checked</#if>>
      </td>
    </tr>
    <#list productFeatureTypeIdsOrdered as productFeatureTypeId>
      <#assign findPftMap = Static["org.ofbiz.base.util.UtilMisc"].toMap("productFeatureTypeId", productFeatureTypeId)>
      <#assign productFeatureType = delegator.findByPrimaryKeyCache("ProductFeatureType", findPftMap)>
      <#assign productFeatures = productFeaturesByTypeMap[productFeatureTypeId]>
      <tr valign="middle">
        <td class="label">${(productFeatureType.get("description",locale))?if_exists}</td>
        <td>
            <select name="pft_${productFeatureTypeId}">
              <option value="">- ${uiLabelMap.CommonSelectAny} -</option>
              <#list productFeatures as productFeature>
              <option value="${productFeature.productFeatureId}">${productFeature.description?default(productFeature.productFeatureId)}</option>
              </#list>
            </select>
        </td>
      </tr>
    </#list>
    <tr valign="middle">
      <td class="label">${uiLabelMap.ProductSortedBy}</td>
      <td>
          <select name="sortOrder" class="selectBox">
            <option value="SortKeywordRelevancy">${uiLabelMap.ProductKeywordRelevancy}</option>
            <option value="SortProductField:productName">${uiLabelMap.ProductProductName}</option>
            <option value="SortProductField:totalQuantityOrdered">${uiLabelMap.ProductPopularityByOrders}</option>
            <option value="SortProductField:totalTimesViewed">${uiLabelMap.ProductPopularityByViews}</option>
            <option value="SortProductField:averageCustomerRating">${uiLabelMap.ProductCustomerRating}</option>
            <option value="SortProductPrice:LIST_PRICE">${uiLabelMap.ProductListPrice}</option>
            <option value="SortProductPrice:DEFAULT_PRICE">${uiLabelMap.ProductDefaultPrice}</option>
          </select>
          ${uiLabelMap.EcommerceLowToHigh} <input type="radio" name="sortAscending" value="Y" checked="checked"/>
          ${uiLabelMap.EcommerceHighToLow} <input type="radio" name="sortAscending" value="N"/>
      </td>
    </tr>
    <#if searchConstraintStrings?has_content>
      <tr valign="top">
        <td class="label">${uiLabelMap.ProductLastSearch}</td>
        <td>
            <#list searchConstraintStrings as searchConstraintString>
                <div>&nbsp;-&nbsp;${searchConstraintString}</div>
            </#list>
            <div>${uiLabelMap.ProductSortedBy}: ${searchSortOrderString}</div>
            <div>
              ${uiLabelMap.ProductNewSearch}<input type="radio" name="clearSearch" value="Y" checked="checked"/>
              ${uiLabelMap.ProductRefineSearch}<input type="radio" name="clearSearch" value="N"/>
            </div>
        </td>
      </tr>
    </#if>
    <tr>
      <td>&nbsp;</td>
      <td><a href="javascript:document.advtokeywordsearchform.submit()" class="buttontext">${uiLabelMap.CommonFind}</a></td>
    </tr>
  </table>

  <#if searchOptionsHistoryList?has_content>
    <hr/>

    <h2>${uiLabelMap.OrderLastSearches}...</h2>

    <div>
      <a href="<@ofbizUrl>clearSearchOptionsHistoryList</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderClearSearchHistory}</a>
	  ${uiLabelMap.OrderClearSearchHistoryNote}
    </div>
    <#list searchOptionsHistoryList as searchOptions>
    <#-- searchOptions type is ProductSearchSession.ProductSearchOptions -->
        <div>
          <b>${uiLabelMap.EcommerceSearchNumber}${searchOptions_index + 1}</b>
          <a href="<@ofbizUrl>setCurrentSearchFromHistoryAndSearch?searchHistoryIndex=${searchOptions_index}&clearSearch=N</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonSearch}</a>
          <a href="<@ofbizUrl>setCurrentSearchFromHistory?searchHistoryIndex=${searchOptions_index}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonRefine}</a>
        </div>
        <#assign constraintStrings = searchOptions.searchGetConstraintStrings(false, delegator, locale)>
        <#list constraintStrings as constraintString>
          <div>&nbsp;-&nbsp;${constraintString}</div>
        </#list>
        <#if searchOptions_has_next>
          <hr/>
        </#if>
    </#list>
  </#if>
</form>
