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
<div class="head1">${uiLabelMap.ProductAdvancedSearchinCategory}</div>
<br/>
<form name="advtokeywordsearchform" method="post" action="<@ofbizUrl>keywordsearch</@ofbizUrl>" style="margin: 0;">
  <input type="hidden" name="VIEW_SIZE" value="25"/>
  <table border="0" wdith="100%">
    <#if searchCategory?has_content>
        <input type="hidden" name="SEARCH_CATEGORY_ID" value="${searchCategoryId?if_exists}"/>
        <tr>
          <td align="right" valign="middle">
            <div class="tabletext">${uiLabelMap.ProductCategory}:</div>
          </td>
          <td valign="middle">
            <div class="tabletext">
              <b>"${(searchCategory.description)?if_exists}" [${(searchCategory.productCategoryId)?if_exists}]</b> ${uiLabelMap.ProductIncludeSubCategories}?
              ${uiLabelMap.CommonYes}<input type="radio" name="SEARCH_SUB_CATEGORIES" value="Y" checked/>
              ${uiLabelMap.CommonNo}<input type="radio" name="SEARCH_SUB_CATEGORIES" value="N"/>
            </div>
          </td>
        </tr>
    <#else>
        <tr>
           <td align="right" valign="top">
             <div class="tabletext">${uiLabelMap.ProductCatalog}:</div>
           </td>
           <td valign="middle">
             <div class="tabletext">
                <select class="selectBox" name="SEARCH_CATALOG_ID">
                  <option value="">- ${uiLabelMap.ProductAnyCatalog} -</option>
                  <#list prodCatalogs as prodCatalog>
                    <#assign displayDesc = prodCatalog.catalogName?default("${uiLabelMap.ProductNoDescription}")>
                      <#if 18 < displayDesc?length>
                        <#assign displayDesc = displayDesc[0..15] + "...">
                      </#if>
                      <option value="${prodCatalog.prodCatalogId}">${displayDesc} [${prodCatalog.prodCatalogId}]</option>
                  </#list>
                </select>
             </div>
           </td>
        </tr>    
        <tr>
          <td align="right" valign="top">
            <div class="tabletext">${uiLabelMap.ProductCategory}:</div>
          </td>
          <td valign="middle">
            <div class="tabletext">
              <input type="text" class="inputBox" name="SEARCH_CATEGORY_ID" size="20" maxlength="20" value="${requestParameters.SEARCH_CATEGORY_ID?if_exists}"/>
              <a href="javascript:call_fieldlookup2(document.advtokeywordsearchform.SEARCH_CATEGORY_ID,'LookupProductCategory');"><img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt='Click here For Field Lookup'/></a>
              ${uiLabelMap.ProductIncludeSubCategories}?
              ${uiLabelMap.CommonYes}<input type="radio" name="SEARCH_SUB_CATEGORIES" value="Y" checked="checked"/>
              ${uiLabelMap.CommonNo}<input type="radio" name="SEARCH_SUB_CATEGORIES" value="N"/>
              ${uiLabelMap.CommonInclude}<input type="radio" name="SEARCH_CATEGORY_EXC" value="" checked="checked"/>
              ${uiLabelMap.CommonExclude}<input type="radio" name="SEARCH_CATEGORY_EXC" value="Y"/>
              ${uiLabelMap.CommonAlwaysInclude}<input type="radio" name="SEARCH_CATEGORY_EXC" value="N"/>
            </div>
          </td>
        </tr>
    </#if>
    <tr>
      <td align="right" valign="top">
        <div class="tabletext">${uiLabelMap.ProductKeywords}:</div>
      </td>
      <td valign="middle">
        <div class="tabletext">
          <input type="text" class="inputBox" name="SEARCH_STRING" size="40" value="${requestParameters.SEARCH_STRING?if_exists}"/>&nbsp;
          ${uiLabelMap.CommonAny}<input type="radio" name="SEARCH_OPERATOR" value="OR" <#if searchOperator == "OR">checked</#if>/>
          ${uiLabelMap.CommonAll}<input type="radio" name="SEARCH_OPERATOR" value="AND" <#if searchOperator == "AND">checked</#if>/>
        </div>
      </td>
    </tr>
    <tr>
      <td align="right" valign="top">
        <div class="tabletext">${uiLabelMap.ProductFeatureCategory} ${uiLabelMap.CommonIds}:</div>
      </td>
      <td valign="middle">
        <div>
          <input type="text" class="inputBox" name="SEARCH_PROD_FEAT_CAT1" size="15" value="${requestParameters.SEARCH_PROD_FEAT_CAT1?if_exists}"/>&nbsp;
              ${uiLabelMap.CommonInclude}<input type="radio" name="SEARCH_PROD_FEAT_CAT_EXC1" value="" checked="checked"/>
              ${uiLabelMap.CommonExclude}<input type="radio" name="SEARCH_PROD_FEAT_CAT_EXC1" value="Y"/>
              ${uiLabelMap.CommonAlwaysInclude}<input type="radio" name="SEARCH_PROD_FEAT_CAT_EXC1" value="N"/>
        </div>
        <div>
          <input type="text" class="inputBox" name="SEARCH_PROD_FEAT_CAT2" size="15" value="${requestParameters.SEARCH_PROD_FEAT_CAT2?if_exists}"/>&nbsp;
              ${uiLabelMap.CommonInclude}<input type="radio" name="SEARCH_PROD_FEAT_CAT_EXC2" value="" checked="checked"/>
              ${uiLabelMap.CommonExclude}<input type="radio" name="SEARCH_PROD_FEAT_CAT_EXC2" value="Y"/>
              ${uiLabelMap.CommonAlwaysInclude}<input type="radio" name="SEARCH_PROD_FEAT_CAT_EXC2" value="N"/>
        </div>
        <div>
          <input type="text" class="inputBox" name="SEARCH_PROD_FEAT_CAT3" size="15" value="${requestParameters.SEARCH_PROD_FEAT_CAT3?if_exists}"/>&nbsp;
              ${uiLabelMap.CommonInclude}<input type="radio" name="SEARCH_PROD_FEAT_CAT_EXC3" value="" checked="checked"/>
              ${uiLabelMap.CommonExclude}<input type="radio" name="SEARCH_PROD_FEAT_CAT_EXC3" value="Y"/>
              ${uiLabelMap.CommonAlwaysInclude}<input type="radio" name="SEARCH_PROD_FEAT_CAT_EXC3" value="N"/>
        </div>
      </td>
    </tr>
    <tr>
      <td align="right" valign="top">
        <div class="tabletext">${uiLabelMap.ProductFeatureGroup} ${uiLabelMap.CommonIds}:</div>
      </td>
      <td valign="middle">
        <div>
          <input type="text" class="inputBox" name="SEARCH_PROD_FEAT_GRP1" size="15" value="${requestParameters.SEARCH_PROD_FEAT_GRP1?if_exists}"/>&nbsp;
              ${uiLabelMap.CommonInclude}<input type="radio" name="SEARCH_PROD_FEAT_GRP_EXC1" value="" checked="checked"/>
              ${uiLabelMap.CommonExclude}<input type="radio" name="SEARCH_PROD_FEAT_GRP_EXC1" value="Y"/>
              ${uiLabelMap.CommonAlwaysInclude}<input type="radio" name="SEARCH_PROD_FEAT_GRP_EXC1" value="N"/>
        </div>
        <div>
          <input type="text" class="inputBox" name="SEARCH_PROD_FEAT_GRP2" size="15" value="${requestParameters.SEARCH_PROD_FEAT_GRP2?if_exists}"/>&nbsp;
              ${uiLabelMap.CommonInclude}<input type="radio" name="SEARCH_PROD_FEAT_GRP_EXC2" value="" checked="checked"/>
              ${uiLabelMap.CommonExclude}<input type="radio" name="SEARCH_PROD_FEAT_GRP_EXC2" value="Y"/>
              ${uiLabelMap.CommonAlwaysInclude}<input type="radio" name="SEARCH_PROD_FEAT_GRP_EXC2" value="N"/>
        </div>
        <div>
          <input type="text" class="inputBox" name="SEARCH_PROD_FEAT_GRP3" size="15" value="${requestParameters.SEARCH_PROD_FEAT_GRP3?if_exists}"/>&nbsp;
              ${uiLabelMap.CommonInclude}<input type="radio" name="SEARCH_PROD_FEAT_GRP_EXC3" value="" checked="checked"/>
              ${uiLabelMap.CommonExclude}<input type="radio" name="SEARCH_PROD_FEAT_GRP_EXC3" value="Y"/>
              ${uiLabelMap.CommonAlwaysInclude}<input type="radio" name="SEARCH_PROD_FEAT_GRP_EXC3" value="N"/>
        </div>
      </td>
    </tr>
    
    <tr>
      <td align="right" valign="top">
        <div class="tabletext">${uiLabelMap.ProductFeatures} ${uiLabelMap.CommonIds}:</div>
      </td>
      <td valign="middle">
        <div>
          <input type="text" class="inputBox" name="SEARCH_FEAT1" size="15" value="${requestParameters.SEARCH_FEAT1?if_exists}"/>&nbsp;
              ${uiLabelMap.CommonInclude}<input type="radio" name="SEARCH_FEAT_EXC1" value="" checked="checked"/>
              ${uiLabelMap.CommonExclude}<input type="radio" name="SEARCH_FEAT_EXC1" value="Y"/>
              ${uiLabelMap.CommonAlwaysInclude}<input type="radio" name="SEARCH_FEAT_EXC1" value="N"/>
        </div>
        <div>
          <input type="text" class="inputBox" name="SEARCH_FEAT2" size="15" value="${requestParameters.SEARCH_FEAT2?if_exists}"/>&nbsp;
              ${uiLabelMap.CommonInclude}<input type="radio" name="SEARCH_FEAT_EXC2" value="" checked="checked"/>
              ${uiLabelMap.CommonExclude}<input type="radio" name="SEARCH_FEAT_EXC2" value="Y"/>
              ${uiLabelMap.CommonAlwaysInclude}<input type="radio" name="SEARCH_FEAT_EXC2" value="N"/>
        </div>
        <div>
          <input type="text" class="inputBox" name="SEARCH_FEAT3" size="15" value="${requestParameters.SEARCH_FEAT3?if_exists}"/>&nbsp;
              ${uiLabelMap.CommonInclude}<input type="radio" name="SEARCH_FEAT_EXC3" value="" checked="checked"/>
              ${uiLabelMap.CommonExclude}<input type="radio" name="SEARCH_FEAT_EXC3" value="Y"/>
              ${uiLabelMap.CommonAlwaysInclude}<input type="radio" name="SEARCH_FEAT_EXC3" value="N"/>
        </div>
      </td>
    </tr>
    <tr>
      <td align="right" valign="top">
        <div>${uiLabelMap.ListPriceRange}:</div>
      </td>
      <td valign="middle">
        <div>
          <input type="text" class="inputBox" name="LIST_PRICE_LOW" size="8" value="${requestParameters.LIST_PRICE_LOW?if_exists}"/>&nbsp;
          <input type="text" class="inputBox" name="LIST_PRICE_HIGH" size="8" value="${requestParameters.LIST_PRICE_HIGH?if_exists}"/>&nbsp;
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
              <option value="${productFeature.productFeatureId}">${productFeature.description?default("${uiLabelMap.ProductNoDescription}")} [${productFeature.productFeatureId}]</option>
              </#list>
            </select>
          </div>
        </td>
      </tr>
    </#list>
    <tr>
      <td align="right" valign="middle">
        <div class="tabletext">${uiLabelMap.ProductSupplier}:</div>
      </td>
      <td valign="middle">
        <div class="tabletext">
          <select name="SEARCH_SUPPLIER_ID" class="selectBox">
            <option value="">- ${uiLabelMap.CommonSelectAny} -</option>
            <#list supplerPartyRoleAndPartyDetails as supplerPartyRoleAndPartyDetail>
              <option value="${supplerPartyRoleAndPartyDetail.partyId}">${supplerPartyRoleAndPartyDetail.groupName?if_exists} ${supplerPartyRoleAndPartyDetail.firstName?if_exists} ${supplerPartyRoleAndPartyDetail.lastName?if_exists} [${supplerPartyRoleAndPartyDetail.partyId}]</option>
            </#list>
          </select>
        </div>
      </td>
    </tr>
    <tr>
      <td align="right" valign="middle">
        <div class="tabletext">${uiLabelMap.CommonSortedBy}:</div>
      </td>
      <td valign="middle">
        <div class="tabletext">
          <select name="sortOrder" class="selectBox">
            <option value="SortKeywordRelevancy">${uiLabelMap.ProductKeywordRelevancy}</option>
            <option value="SortProductField:productName">${uiLabelMap.ProductProductName}</option>
            <option value="SortProductField:internalName">${uiLabelMap.ProductInternalName}</option>
            <option value="SortProductField:totalQuantityOrdered">${uiLabelMap.ProductPopularityByOrders}</option>
            <option value="SortProductField:totalTimesViewed">${uiLabelMap.ProductPopularityByViews}</option>
            <option value="SortProductField:averageCustomerRating">${uiLabelMap.ProductCustomerRating}</option>
            <option value="SortProductPrice:LIST_PRICE">${uiLabelMap.ProductListPrice}</option>
            <option value="SortProductPrice:DEFAULT_PRICE">${uiLabelMap.ProductDefaultPrice}</option>
            <option value="SortProductPrice:AVERAGE_COST">${uiLabelMap.ProductAverageCost}</option>
            <option value="SortProductPrice:MINIMUM_PRICE">${uiLabelMap.ProductMinimumPrice}</option>
            <option value="SortProductPrice:MAXIMUM_PRICE">${uiLabelMap.ProductMaximumPrice}</option>
          </select>
          ${uiLabelMap.ProductLowToHigh}<input type="radio" name="sortAscending" value="Y" checked/>
          ${uiLabelMap.ProductHighToLow}<input type="radio" name="sortAscending" value="N"/>
        </div>
      </td>
    </tr>
    <tr>
      <td align="right" valign="middle">
        ${uiLabelMap.ProductPrioritizeProductsinCategory}:
      </td>
      <td valign="middle">
        <input type="text" class="inputBox" name="PRIORITIZE_CATEGORY_ID" size="20" maxlength="20" value="${requestParameters.PRIORITIZE_CATEGORY_ID?if_exists}"/>
        <a href="javascript:call_fieldlookup2(document.advtokeywordsearchform.PRIORITIZE_CATEGORY_ID,'LookupProductCategory');"><img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt='Click here For Field Lookup'/></a>
      </td>
    </tr>
    <#if searchConstraintStrings?has_content>
      <tr>
        <td align="right" valign="top">
          <div class="tabletext">${uiLabelMap.ProductLastSearch}</div>
        </td>
        <td valign="top">
            <#list searchConstraintStrings as searchConstraintString>
                <div class="tabletext">&nbsp;-&nbsp;${searchConstraintString}</div>
            </#list>
            <div class="tabletext">${uiLabelMap.CommonSortedBy}: ${searchSortOrderString}</div>
            <div class="tabletext">
              ${uiLabelMap.ProductNewSearch}<input type="radio" name="clearSearch" value="Y" checked="checked"/>
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
</form>
