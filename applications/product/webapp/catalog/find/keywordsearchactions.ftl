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

<#if productIds?has_content>
    <hr />
    <span class="label"><b>${uiLabelMap.ProductNote}:</b></span> ${uiLabelMap.ProductNoteKeywordSearch}
    <hr />

    ${screens.render("component://product/widget/catalog/ProductScreens.xml#CreateVirtualWithVariantsFormInclude")}

    <hr />

    <div>
        <form method="post" action="<@ofbizUrl>searchRemoveFromCategory</@ofbizUrl>" name="searchRemoveFromCategory">
          <span class="label">${uiLabelMap.ProductRemoveResultsFrom} ${uiLabelMap.ProductCategory}:</span>
          <input type="text" name="SE_SEARCH_CATEGORY_ID" size="20" maxlength="20"/>
          <a href="javascript:call_fieldlookup2(document.searchRemoveFromCategory.SE_SEARCH_CATEGORY_ID,'LookupProductCategory');"><img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt="${uiLabelMap.CommonClickHereForFieldLookup}"/></a>
          <input type="hidden" name="clearSearch" value="N">
          <input type="submit" value="${uiLabelMap.CommonRemove}" class="smallSubmit">
          <br />
        </form>
    </div>

    <hr />

    <div>
        <form method="post" action="<@ofbizUrl>searchExpireFromCategory</@ofbizUrl>" name="searchExpireFromCategory">
          <span class="label">${uiLabelMap.ProductExpireResultsFrom} ${uiLabelMap.ProductCategory}:</span>
          <input type="text" name="SE_SEARCH_CATEGORY_ID" size="20" maxlength="20"/>
          <a href="javascript:call_fieldlookup2(document.searchExpireFromCategory.SE_SEARCH_CATEGORY_ID,'LookupProductCategory');"><img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt="${uiLabelMap.CommonClickHereForFieldLookup}"/></a>
          <span class="label">${uiLabelMap.CommonThru}</span><input type="text" size="25" name="thruDate"><a href="javascript:call_cal(document.searchExpireFromCategory.thruDate, null);"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"></a>
          <input type="hidden" name="clearSearch" value="N">
          <input type="submit" value="${uiLabelMap.CommonExpire}" class="smallSubmit">
          <br />
        </form>
    </div>

    <hr />

    <div>
        <form method="post" action="<@ofbizUrl>searchAddToCategory</@ofbizUrl>" name="searchAddToCategory">
          <span class="label">${uiLabelMap.ProductAddResultsTo} ${uiLabelMap.ProductCategory}:</span>
          <input type="text" name="SE_SEARCH_CATEGORY_ID" size="20" maxlength="20"/>
          <a href="javascript:call_fieldlookup2(document.searchAddToCategory.SE_SEARCH_CATEGORY_ID,'LookupProductCategory');"><img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt="${uiLabelMap.CommonClickHereForFieldLookup}"/></a>
          <span class="label">${uiLabelMap.CommonFrom}</span><input type="text" size="25" name="fromDate"><a href="javascript:call_cal(document.searchAddToCategory.fromDate, null);"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"></a>
          <input type="hidden" name="clearSearch" value="N">
          <input type="submit" value="${uiLabelMap.ProductAddToCategory}" class="smallSubmit">
          <br />
        </form>
    </div>

    <hr />

    <div>
        <form method="post" action="<@ofbizUrl>searchAddFeature</@ofbizUrl>" name="searchAddFeature">
          <span class="label">${uiLabelMap.ProductAddFeatureToResults}:</span><br />
          <span class="label">${uiLabelMap.ProductFeatureId}</span><input type="text" size="10" name="productFeatureId" value="">
          <span class="label">${uiLabelMap.CommonFrom}</span><input type="tex"t size="25" name="fromDate"><a href="javascript:call_cal(document.searchAddFeature.fromDate, null);"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"></a>
          <span class="label">${uiLabelMap.CommonThru}</span><input type="text" size="25" name="thruDate"><a href="javascript:call_cal(document.searchAddFeature.thruDate, null);"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"></a>
          <br />
          <span class="label">${uiLabelMap.CommonAmount}</span><input type="text" size="5" name="amount" value="">
          <span class="label">${uiLabelMap.CommonSequence}</span><input type="text" size="5" name="sequenceNum" value="">
          <span class="label">${uiLabelMap.ProductFeatureApplicationType}</span>
          <span class="label">${uiLabelMap.ProductCategoryId}:</span>
          <select name='productFeatureApplTypeId' size='1'>
               <#list applicationTypes as applicationType>
                   <#assign displayDesc = applicationType.get("description", locale)?default("No Description")>
                   <#if 18 < displayDesc?length>
                       <#assign displayDesc = displayDesc[0..15] + "...">
                   </#if>
                   <option value="${applicationType.productFeatureApplTypeId}">${displayDesc}</option>
               </#list>
          </select>
          <input type="hidden" name="clearSearch" value="N">
          <input type="submit" value="${uiLabelMap.ProductAddFeature}" class="smallSubmit">
          <br />
        </form>
    </div>

    <hr />

    <div>
        <form method="post" action="<@ofbizUrl>searchRemoveFeature</@ofbizUrl>" name="searchRemoveFeature">
          <span class="label">${uiLabelMap.ProductRemoveFeatureFromResults}:</span><br />
          <span class="label">${uiLabelMap.ProductFeatureId}</span><input type="text" size="10" name="productFeatureId" value="">
          <input type="hidden" name="clearSearch" value="N">
          <input type="submit" value="${uiLabelMap.ProductRemoveFeature}" class="smallSubmit">
          <br />
        </form>
    </div>

    <hr />

    <div>
        <form method="post" action="" name="searchShowParams">
          <#assign searchParams = Static["org.ofbiz.product.product.ProductSearchSession"].makeSearchParametersString(session)>
          <span class="label">${uiLabelMap.ProductPlainSearchParameters}:</span><input type="text" size="60" name="searchParameters" value="${StringUtil.wrapString(searchParams)}">
          <br />
          <span class="label">${uiLabelMap.ProductHtmlSearchParameters}:</span><input type="text" size="60" name="searchParameters" value="${StringUtil.wrapString(searchParams)?html}">
          <input type="hidden" name="clearSearch" value="N">
        </form>
    </div>

    <hr />

    <div>
      <span class="label">${uiLabelMap.ProductSearchExportProductList}:</span><a href="<@ofbizUrl>searchExportProductList?clearSearch=N</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductSearchExport}</a>
    </div>
</#if>
