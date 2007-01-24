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
<#assign searchOptionsHistoryList = Static["org.ofbiz.product.product.ProductSearchSession"].getSearchOptionsHistoryList(session)>
<#assign currentCatalogId = Static["org.ofbiz.product.catalog.CatalogWorker"].getCurrentCatalogId(request)>
<div class="head1">${uiLabelMap.ProductAdvancedSearchinCategory}</div>
<br/>
<form name="advtokeywordsearchform" method="post" action="<@ofbizUrl>keywordsearch</@ofbizUrl>" style="margin: 0;">
  <input type="hidden" name="VIEW_SIZE" value="10"/>
  <table border="0" wdith="100%">
    <input type="hidden" name="SEARCH_CATALOG_ID" value="${currentCatalogId}">
    <#if searchCategory?has_content>
        <input type="hidden" name="SEARCH_CATEGORY_ID" value="${searchCategoryId?if_exists}"/>
        <tr>
          <td align="right" valign="middle">
            <div class="tabletext">${uiLabelMap.ProductCategory}:</div>
          </td>
          <td valign="middle">
            <div class="tabletext">
              <b>"${(searchCategory.description)?if_exists}". </b>${uiLabelMap.ProductIncludeSubcategories}
              ${uiLabelMap.CommonYes}<input type="radio" name="SEARCH_SUB_CATEGORIES" value="Y" checked/>
              ${uiLabelMap.CommonNo}<input type="radio" name="SEARCH_SUB_CATEGORIES" value="N"/>
            </div>
          </td>
        </tr>
    </#if>
    <tr>
      <td align="right" valign="middle">
        <div class="tabletext">${uiLabelMap.ProductKeywords}:</div>
      </td>
      <td valign="middle">
        <div class="tabletext">
          <input type="text" class="inputBox" name="SEARCH_STRING" size="40" value="${requestParameters.SEARCH_STRING?if_exists}">&nbsp;
          ${uiLabelMap.CommonAny}<input type="radio" name="SEARCH_OPERATOR" value="OR" <#if searchOperator == "OR">checked</#if>>
          ${uiLabelMap.CommonAll}<input type="radio" name="SEARCH_OPERATOR" value="AND" <#if searchOperator == "AND">checked</#if>>
        </div>
      </td>
    </tr>
    <#list productFeatureTypeIdsOrdered as productFeatureTypeId>
      <#assign findPftMap = Static["org.ofbiz.base.util.UtilMisc"].toMap("productFeatureTypeId", productFeatureTypeId)>
      <#assign productFeatureType = delegator.findByPrimaryKeyCache("ProductFeatureType", findPftMap)>
      <#assign productFeatures = productFeaturesByTypeMap[productFeatureTypeId]>
      <tr>
        <td align="right" valign="middle">
          <div class="tabletext">${(productFeatureType.get("description",locale))?if_exists}:</div>
        </td>
        <td valign="middle">
          <div class="tabletext">
            <select class="selectBox" name="pft_${productFeatureTypeId}">
              <option value="">- ${uiLabelMap.CommonSelectAny} -</option>
              <#list productFeatures as productFeature>
              <option value="${productFeature.productFeatureId}">${productFeature.description?default(productFeature.productFeatureId)}</option>
              </#list>
            </select>
          </div>
        </td>
      </tr>
    </#list>
    <tr>
      <td align="right" valign="middle">
        <div class="tabletext">${uiLabelMap.ProductSortedBy}:</div>
      </td>
      <td valign="middle">
        <div class="tabletext">
          <select name="sortOrder" class="selectBox">
            <option value="SortKeywordRelevancy">${uiLabelMap.ProductKeywordRelevency}</option>
            <option value="SortProductField:productName">${uiLabelMap.ProductProductName}</option>
            <option value="SortProductField:totalQuantityOrdered">${uiLabelMap.ProductPopularityByOrders}</option>
            <option value="SortProductField:totalTimesViewed">${uiLabelMap.ProductPopularityByViews}</option>
            <option value="SortProductField:averageCustomerRating">${uiLabelMap.ProductCustomerRating}</option>
            <option value="SortProductPrice:LIST_PRICE">${uiLabelMap.ProductListPrice}</option>
            <option value="SortProductPrice:DEFAULT_PRICE">${uiLabelMap.ProductDefaultPrice}</option>
          </select>
          ${uiLabelMap.EcommerceLowToHigh}<input type="radio" name="sortAscending" value="Y" checked/>
          ${uiLabelMap.EcommerceHighToLow}<input type="radio" name="sortAscending" value="N"/>
        </div>
      </td>
    </tr>
    <#if searchConstraintStrings?has_content>
      <tr>
        <td align="right" valign="top">
          <div class="tabletext">${uiLabelMap.ProductLastSearch}:</div>
        </td>
        <td valign="top">
            <#list searchConstraintStrings as searchConstraintString>
                <div class="tabletext">&nbsp;-&nbsp;${searchConstraintString}</div>
            </#list>
            <div class="tabletext">${uiLabelMap.ProductSortedBy}: ${searchSortOrderString}</div>
            <div class="tabletext">
              ${uiLabelMap.ProductNewSearch}<input type="radio" name="clearSearch" value="Y" checked/>
              ${uiLabelMap.ProductRefineSearch}<input type="radio" name="clearSearch" value="N"/>
            </div>
        </td>
      </tr>
    </#if>
    <tr>
      <td>
        <div class="tabletext">
          <a href="javascript:document.advtokeywordsearchform.submit()" class="buttontext">${uiLabelMap.CommonFind}</a>
        </div>
      </td>
    </tr>
  </table>
  
  <#if searchOptionsHistoryList?has_content>
    <hr class="sepbar"/>
  
    <div class="head2">${uiLabelMap.EcommerceLastSearches}...</div>
  
    <div class="tabletext">
      <a href="<@ofbizUrl>clearSearchOptionsHistoryList</@ofbizUrl>" class="buttontext">${uiLabelMap.EcommerceClearSearchHistory}</a>
	${uiLabelMap.EcommerceClearSearchHistoryNote}
    </div>
    <#list searchOptionsHistoryList as searchOptions>
    <#-- searchOptions type is ProductSearchSession.ProductSearchOptions -->
        <div class="tabletext">
          <b>${uiLabelMap.EcommerceSearchNumber}${searchOptions_index + 1}</b>
          <a href="<@ofbizUrl>setCurrentSearchFromHistoryAndSearch?searchHistoryIndex=${searchOptions_index}&clearSearch=N</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonSearch}</a>
          <a href="<@ofbizUrl>setCurrentSearchFromHistory?searchHistoryIndex=${searchOptions_index}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonRefine}</a>
        </div>
        <#assign constraintStrings = searchOptions.searchGetConstraintStrings(false, delegator, locale)>
        <#list constraintStrings as constraintString>
          <div class="tabletext">&nbsp;-&nbsp;${constraintString}</div>
        </#list>
        <#if searchOptions_has_next>
          <hr class="sepbar"/>
        </#if>
    </#list>
  </#if>
</form>
