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

<hr class="sepbar"/>
<div class="tabletext"><b>${uiLabelMap.ProductNote}:</b> ${uiLabelMap.ProductNoteKeywordSearch}</div>
<hr class="sepbar"/>

${screens.render("component://product/widget/catalog/ProductScreens.xml#CreateVirtualWithVariantsFormInclude")}

<hr class="sepbar"/>

<div class="tabletext">
<form method="post" action="<@ofbizUrl>searchRemoveFromCategory</@ofbizUrl>" name="searchRemoveFromCategory">
  <b>${uiLabelMap.ProductRemoveResultsFrom} </b> ${uiLabelMap.ProductCategory}:
  <input type="text" class="inputBox" name="SE_SEARCH_CATEGORY_ID" size="20" maxlength="20"/>
  <a href="javascript:call_fieldlookup2(document.searchRemoveFromCategory.SE_SEARCH_CATEGORY_ID,'LookupProductCategory');"><img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt='Click here For Field Lookup'/></a>
  <input type="hidden" name="clearSearch" value="N">
  <input type="submit" value="${uiLabelMap.CommonRemove}" class="smallSubmit"><br/>
</form>
</div>

<hr class="sepbar"/>

<div class="tabletext">
<form method="post" action="<@ofbizUrl>searchExpireFromCategory</@ofbizUrl>" name="searchExpireFromCategory">
  <b>${uiLabelMap.ProductExpireResultsFrom} </b> ${uiLabelMap.ProductCategory}:
  <input type="text" class="inputBox" name="SE_SEARCH_CATEGORY_ID" size="20" maxlength="20"/>
  <a href="javascript:call_fieldlookup2(document.searchExpireFromCategory.SE_SEARCH_CATEGORY_ID,'LookupProductCategory');"><img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt='Click here For Field Lookup'/></a>
  ${uiLabelMap.CommonThru}<input type="text" size="25" name="thruDate" class="inputBox"><a href="javascript:call_cal(document.searchExpireFromCategory.thruDate, null);"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"></a>
  <input type="hidden" name="clearSearch" value="N">
  <input type="submit" value="${uiLabelMap.CommonExpire}" class="smallSubmit"><br/>
</form>
</div>

<hr class="sepbar"/>

<div class="tabletext">
<form method="post" action="<@ofbizUrl>searchAddToCategory</@ofbizUrl>" name="searchAddToCategory">
  <b>${uiLabelMap.ProductAddResultsTo} </b> ${uiLabelMap.ProductCategory}:
  <input type="text" class="inputBox" name="SE_SEARCH_CATEGORY_ID" size="20" maxlength="20"/>
  <a href="javascript:call_fieldlookup2(document.searchAddToCategory.SE_SEARCH_CATEGORY_ID,'LookupProductCategory');"><img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt='Click here For Field Lookup'/></a>
  ${uiLabelMap.CommonFrom}<input type="text" size="25" name="fromDate" class="inputBox"><a href="javascript:call_cal(document.searchAddToCategory.fromDate, null);"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"></a>
  <input type="hidden" name="clearSearch" value="N">
  <input type="submit" value="${uiLabelMap.ProductAddToCategory}" class="smallSubmit"><br/>
</form>
</div>

<hr class="sepbar"/>

<div class="tabletext">
<form method="post" action="<@ofbizUrl>searchAddFeature</@ofbizUrl>" name="searchAddFeature">
  <b>${uiLabelMap.ProductAddFeatureToResults}:</b><br/>
  ${uiLabelMap.ProductFeatureId}<input type="text" size="10" name="productFeatureId" value="" class="inputBox">
  ${uiLabelMap.CommonFrom}<input type="tex"t size="25" name="fromDate" class="inputBox"><a href="javascript:call_cal(document.searchAddFeature.fromDate, null);"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"></a> 
  ${uiLabelMap.CommonThru}<input type="text" size="25" name="thruDate" class="inputBox"><a href="javascript:call_cal(document.searchAddFeature.thruDate, null);"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"></a>
  <br/>
  ${uiLabelMap.CommonAmount}<input type="text" size="5" name="amount" value="" class="inputBox">
  ${uiLabelMap.CommonSequence}<input type="text" size="5" name="sequenceNum" value="" class="inputBox">
  ${uiLabelMap.ProductFeatureApplicationType}
    ${uiLabelMap.ProductCategoryId}:
    <select name='productFeatureApplTypeId' size='1' class='selectBox'>
       <#list applicationTypes as applicationType>
           <#assign displayDesc = applicationType.get("description", locale)?default("No Description")>
           <#if 18 < displayDesc?length>
               <#assign displayDesc = displayDesc[0..15] + "...">
           </#if>
           <option value="${applicationType.productFeatureApplTypeId}">${displayDesc}</option>
       </#list>
  </select>
  <input type="hidden" name="clearSearch" value="N">
  <input type="submit" value="${uiLabelMap.ProductAddFeature}" class="smallSubmit"><br/>
</form>
</div>

<hr class="sepbar"/>
<div class="tabletext">
<form method="post" action="<@ofbizUrl>searchRemoveFeature</@ofbizUrl>" name="searchRemoveFeature">
  <b>${uiLabelMap.ProductRemoveFeatureFromResults}:</b><br/>
  ${uiLabelMap.ProductFeatureId}<input type="text" size="10" name="productFeatureId" value="" class="inputBox">
  <input type="hidden" name="clearSearch" value="N">
  <input type="submit" value="${uiLabelMap.ProductRemoveFeature}" class="smallSubmit"><br/>
</form>
</div>

<hr class="sepbar"/>
<div class="tabletext">
<form method="post" action="" name="searchShowParams">
  <#assign searchParams = Static["org.ofbiz.product.product.ProductSearchSession"].makeSearchParametersString(session)>
  <div><b>${uiLabelMap.ProductPlainSearchParameters}:</b><input type="text" size="60" name="searchParameters" value="${searchParams}" class="inputBox"></div>
  <div><b>${uiLabelMap.ProductHtmlSearchParameters}:</b><input type="text" size="60" name="searchParameters" value="${searchParams?html}" class="inputBox"></div>
  <input type="hidden" name="clearSearch" value="N">
</form>
</div>

<hr class="sepbar"/>
<div class="tabletext">
  <b>${uiLabelMap.ProductSearchExportProductList}:</b> <a href="<@ofbizUrl>searchExportProductList?clearSearch=N</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductSearchExport}</a>
</div>
</#if>
