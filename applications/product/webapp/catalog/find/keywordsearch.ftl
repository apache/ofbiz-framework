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

<div class="head1">${uiLabelMap.ProductSearchProducts}, <span class="head2">${uiLabelMap.ProductSearchFor}:</span></div>
<#list searchConstraintStrings as searchConstraintString>
    <div class="tabletext">&nbsp;<a href="<@ofbizUrl>keywordsearch?removeConstraint=${searchConstraintString_index}&clearSearch=N</@ofbizUrl>" class="buttontext">X</a>&nbsp;${searchConstraintString}</div>
</#list>
<div class="tabletext">${uiLabelMap.CommonSortedBy}: ${searchSortOrderString}</div>
<div class="tabletext"><a href="<@ofbizUrl>advancedsearch?SEARCH_CATEGORY_ID=${(requestParameters.SEARCH_CATEGORY_ID)?if_exists}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonRefine} ${uiLabelMap.CommonSearch}</a></div>

<#if !productIds?has_content>
  <br/><div class="head2">&nbsp;${uiLabelMap.ProductNoResultsFound}.</div>
</#if>

<#if productIds?has_content>
<table border="0" width="100%" cellpadding="2">
    <tr>
      <td align="right">
        <b>
        <#if 0 < viewIndex?int>
          <a href="<@ofbizUrl>keywordsearch/~VIEW_INDEX=${viewIndex-1}/~VIEW_SIZE=${viewSize}/~clearSearch=N</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonPrevious}</a> |
        </#if>
        <#if 0 < listSize?int>
          <span class="tabletext">${lowIndex+1} - ${highIndex} ${uiLabelMap.CommonOf} ${listSize}</span>
        </#if>
        <#if highIndex?int < listSize?int>
          | <a href="<@ofbizUrl>keywordsearch/~VIEW_INDEX=${viewIndex+1}/~VIEW_SIZE=${viewSize}/~clearSearch=N</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonNext}</a>
        </#if>
        </b>
      </td>
    </tr>
</table>
</#if>

<#if productIds?has_content>
<script language="JavaScript" type="text/javascript">
    function checkProductToBagTextArea(field, idValue) {
        fullValue = idValue + "\n";
        tempStr = document.forms["quickCreateVirtualWithVariants"].elements["variantProductIdsBag"].value;
        if (field.checked) {
            if (tempStr.length > 0 && tempStr.substring(tempStr.length-1, tempStr.length) != "\n") {
                tempStr = tempStr + "\n";
            }
            document.forms["quickCreateVirtualWithVariants"].elements["variantProductIdsBag"].value = tempStr + fullValue;
        } else {
            start = document.forms["quickCreateVirtualWithVariants"].elements["variantProductIdsBag"].value.indexOf(fullValue);
            if (start >= 0) {
                end = start + fullValue.length;
                document.forms["quickCreateVirtualWithVariants"].elements["variantProductIdsBag"].value = tempStr.substring(0, start) + tempStr.substring(end, tempStr.length);
                //document.forms["quickCreateVirtualWithVariants"].elements["variantProductIdsBag"].value += start + ", " + end + "\n";
            }
        }
    }
</script>
<center>
  <table width="100%" cellpadding="0" cellspacing="0">
    <#assign listIndex = lowIndex>
    <#list productIds as productId><#-- note that there is no boundary range because that is being done before the list is put in the content -->
      <#assign product = delegator.findByPrimaryKey("Product", Static["org.ofbiz.base.util.UtilMisc"].toMap("productId", productId))>
      <tr>
        <td>
          <input type="checkbox" name="selectResult${productId_index}" onchange="checkProductToBagTextArea(this, '${productId}');"/>
          <a href="<@ofbizUrl>EditProduct?productId=${productId}</@ofbizUrl>" class="buttontext">[${productId}] ${(product.internalName)?if_exists}</a>
        </td>
      </tr>
    </#list>
  </table>
</center>
</#if>

<#if productIds?has_content>
<table border="0" width="100%" cellpadding="2">
    <tr>
      <td align="right">
        <b>
        <#if 0 < viewIndex?int>
          <a href="<@ofbizUrl>keywordsearch/~VIEW_INDEX=${viewIndex-1}/~VIEW_SIZE=${viewSize}/~clearSearch=N</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonPrevious}]</a> |
        </#if>
        <#if 0 < listSize?int>
          <span class="tabletext">${lowIndex+1} - ${highIndex} ${uiLabelMap.CommonOf} ${listSize}</span>
        </#if>
        <#if highIndex?int < listSize?int>
          | <a href="<@ofbizUrl>keywordsearch/~VIEW_INDEX=${viewIndex+1}/~VIEW_SIZE=${viewSize}/~clearSearch=N</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonNext}]</a>
        </#if>
        </b>
      </td>
    </tr>
</table>

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
  <div><b>Plain Search Parameters:</b><input type="text" size="60" name="searchParameters" value="${searchParams}" class="inputBox"></div>
  <div><b>HTML Search Parameters:</b><input type="text" size="60" name="searchParameters" value="${searchParams?html}" class="inputBox"></div>
  <input type="hidden" name="clearSearch" value="N">
</form>
</div>

<hr class="sepbar"/>
<div class="tabletext">
<form method="post" action="<@ofbizUrl>searchExportProductList</@ofbizUrl>" name="searchRemoveFeature">
  <b>${uiLabelMap.ProductSearchExportProductList}:</b>
  <input type="hidden" name="clearSearch" value="N">
  <input type="submit" value="${uiLabelMap.ProductSearchExport}" class="smallSubmit"><br/>
</form>
</div>
</#if>
