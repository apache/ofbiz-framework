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
<h2>${uiLabelMap.ProductAdvancedSearchInCategory}</h2>
<form id="advtokeywordsearchform" method="post" action="<@ofbizUrl>keywordsearch</@ofbizUrl>">
  <fieldset class="inline">
    <input type="hidden" name="VIEW_SIZE" value="10" />
    <input type="hidden" name="PAGING" value="Y" />
    <input type="hidden" name="SEARCH_CATALOG_ID" value="${currentCatalogId}" />
    <#if searchCategory?has_content>
      <input type="hidden" name="SEARCH_CATEGORY_ID" value="${searchCategoryId?if_exists}" />
      <label>${uiLabelMap.ProductCategory}</label>
      <p>${(searchCategory.description)?if_exists}</p>
      <div>
        <label>${uiLabelMap.ProductIncludeSubCategories}</label>
        <label for="SEARCH_SUB_CATEGORIES_YES">${uiLabelMap.CommonYes}</label> <input type="radio" name="SEARCH_SUB_CATEGORIES" id="SEARCH_SUB_CATEGORIES_YES" value="Y" checked="checked" />
        <label for="SEARCH_SUB_CATEGORIES_NO">${uiLabelMap.CommonNo}</label> <input type="radio" name="SEARCH_SUB_CATEGORIES" id="SEARCH_SUB_CATEGORIES_NO" value="N" />
      </div>
    </#if>
    <div>
      <label for="SEARCH_STRING">${uiLabelMap.ProductKeywords}</label>
      <input type="text" name="SEARCH_STRING" id="SEARCH_STRING"  size="20" value="${requestParameters.SEARCH_STRING?if_exists}" />
      <label for="SEARCH_OPERATOR_ANY">${uiLabelMap.CommonAny}</label> <input type="radio" name="SEARCH_OPERATOR" id="SEARCH_OPERATOR_ANY" value="OR" <#if searchOperator == "OR">checked="checked"</#if> />
      <label for="SEARCH_OPERATOR_ALL">${uiLabelMap.CommonAll}</label> <input type="radio" name="SEARCH_OPERATOR" id="SEARCH_OPERATOR_ALL" value="AND" <#if searchOperator == "AND">checked="checked"</#if> />
    </div>
    <#list productFeatureTypeIdsOrdered as productFeatureTypeId>
      <#assign findPftMap = Static["org.ofbiz.base.util.UtilMisc"].toMap("productFeatureTypeId", productFeatureTypeId)>
      <#assign productFeatureType = delegator.findOne("ProductFeatureType", findPftMap, true)>
      <#assign productFeatures = productFeaturesByTypeMap[productFeatureTypeId]>
      <div>
        <label for="pft_${productFeatureTypeId}">${(productFeatureType.get("description",locale))?if_exists}</label>
        <select name="pft_${productFeatureTypeId}" id="pft_${productFeatureTypeId}">
          <option value="">- ${uiLabelMap.CommonSelectAny} -</option>
          <#list productFeatures as productFeature>
            <option value="${productFeature.productFeatureId}">${productFeature.description?default(productFeature.productFeatureId)}</option>
          </#list>
        </select>
      </div>
    </#list>
    <div>
      <label for="sortOrder">${uiLabelMap.ProductSortedBy}</label>
      <select name="sortOrder" id="sortOrder">
        <option value="SortKeywordRelevancy">${uiLabelMap.ProductKeywordRelevancy}</option>
        <option value="SortProductField:productName">${uiLabelMap.ProductProductName}</option>
        <option value="SortProductField:totalQuantityOrdered">${uiLabelMap.ProductPopularityByOrders}</option>
        <option value="SortProductField:totalTimesViewed">${uiLabelMap.ProductPopularityByViews}</option>
        <option value="SortProductField:averageCustomerRating">${uiLabelMap.ProductCustomerRating}</option>
        <option value="SortProductPrice:LIST_PRICE">${uiLabelMap.ProductListPrice}</option>
        <option value="SortProductPrice:DEFAULT_PRICE">${uiLabelMap.ProductDefaultPrice}</option>
        <#if productFeatureTypes?? && productFeatureTypes?has_content>
          <#list productFeatureTypes as productFeatureType>
            <option value="SortProductFeature:${productFeatureType.productFeatureTypeId}">${productFeatureType.description?default(productFeatureType.productFeatureTypeId)}</option>
          </#list>
        </#if>
      </select>
      <label for="sortAscendingHigh">${uiLabelMap.EcommerceLowToHigh}</label> <input type="radio" name="sortAscending" id="sortAscendingHigh" value="Y" checked="checked" />
      <label for="sortAscendingLow">${uiLabelMap.EcommerceHighToLow}</label> <input type="radio" name="sortAscending" id="sortAscendingLow" value="N" />
    </div>
    <#if searchConstraintStrings?has_content>
      <div>
        <label>${uiLabelMap.ProductLastSearch}</label>
          <#list searchConstraintStrings as searchConstraintString>
            <p>${searchConstraintString}</p>
          </#list>
          <p>${uiLabelMap.ProductSortedBy}: ${searchSortOrderString}</p>
          <div>
            <label for="clearSearchNew">${uiLabelMap.ProductNewSearch}</label><input type="radio" name="clearSearch" id="clearSearchNew" value="Y" checked="checked" />
            <label for="clearSearchRefine">${uiLabelMap.ProductRefineSearch}</label><input type="radio" name="clearSearch" id="clearSearchRefine" value="N" />
          </div>
      </div>
    </#if>
    <div>
      <input type="submit" name="submit" class="button" value="${uiLabelMap.CommonFind}" />
    </div>
    <#if searchOptionsHistoryList?has_content>
      <h2>${uiLabelMap.OrderLastSearches}...</h2>
      <div>
        <a href="<@ofbizUrl>clearSearchOptionsHistoryList</@ofbizUrl>" class="button">${uiLabelMap.OrderClearSearchHistory}</a>
        <h3>${uiLabelMap.OrderClearSearchHistoryNote}</h3>
      </div>
      <#list searchOptionsHistoryList as searchOptions>
      <#-- searchOptions type is ProductSearchSession.ProductSearchOptions -->
        <div>
          <p>${uiLabelMap.EcommerceSearchNumber}${searchOptions_index + 1}</p>
          <a href="<@ofbizUrl>setCurrentSearchFromHistoryAndSearch?searchHistoryIndex=${searchOptions_index}&amp;clearSearch=N</@ofbizUrl>" class="button">${uiLabelMap.CommonSearch}</a>
          <a href="<@ofbizUrl>setCurrentSearchFromHistory?searchHistoryIndex=${searchOptions_index}</@ofbizUrl>" class="button">${uiLabelMap.CommonRefine}</a>
        </div>
        <#assign constraintStrings = searchOptions.searchGetConstraintStrings(false, delegator, locale)>
        <#list constraintStrings as constraintString>
          <p> - ${constraintString}</p>
        </#list>
        <#if searchOptions_has_next>
          
        </#if>
      </#list>
    </#if>
  </fieldset>
</form>
