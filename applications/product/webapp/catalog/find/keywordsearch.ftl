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

<h1>${uiLabelMap.ProductSearchProducts}, <span class="head2">${uiLabelMap.ProductSearchFor}:</span></h1>
<#list searchConstraintStrings as searchConstraintString>
    <div class="tabletext">&nbsp;<a href="<@ofbizUrl>keywordsearch?removeConstraint=${searchConstraintString_index}&clearSearch=N</@ofbizUrl>" class="buttontext">X</a>&nbsp;${searchConstraintString}</div>
</#list>
<div class="tabletext">${uiLabelMap.CommonSortedBy}: ${searchSortOrderString}</div>
<div class="tabletext"><a href="<@ofbizUrl>advancedsearch?SEARCH_CATEGORY_ID=${(requestParameters.SEARCH_CATEGORY_ID)?if_exists}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonRefine} ${uiLabelMap.CommonSearch}</a></div>

<#if !productIds?has_content>
  <br/><h2>&nbsp;${uiLabelMap.ProductNoResultsFound}.</h2>
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
    
    function toggleAll(e) {
        var cform = document.products;
        var len = cform.elements.length;
        for (var i = 0; i < len; i++) {
            var element = cform.elements[i];
            if (element.name == "selectResult" && element.checked != e.checked) {
                toggle(element);
            }
        }
    }
    
    function toggle(e) {
        e.checked = !e.checked;
    }

    function exportToeBay() {
        document.products.action="<@ofbizUrl>ProductsExportToEbay</@ofbizUrl>";
        document.products.submit();
    }
</script>

<table border="0" width="100%" cellpadding="2">
    <tr>
      <td align="left"><input type="checkbox" name="selectAll" value="0" onclick="javascript:toggleAll(this);"> <b>${uiLabelMap.ProductProduct}</b></td>
      <td align="right">
        <b>
        <#if 0 < viewIndex?int>
          <a href="<@ofbizUrl>keywordsearch/~VIEW_INDEX=${viewIndex-1}/~VIEW_SIZE=${viewSize}/~clearSearch=N/~PAGING=${paging}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonPrevious}</a> |
        </#if>
        <#if 0 < listSize?int>
          <span class="tabletext">${lowIndex+1} - ${highIndex} ${uiLabelMap.CommonOf} ${listSize}</span>
        </#if>
        <#if highIndex?int < listSize?int>
          | <a href="<@ofbizUrl>keywordsearch/~VIEW_INDEX=${viewIndex+1}/~VIEW_SIZE=${viewSize}/~clearSearch=N/~PAGING=${paging}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonNext}</a>
        </#if>
        <#if paging == "Y">
          <a href="<@ofbizUrl>keywordsearch/~VIEW_INDEX=0/~VIEW_SIZE=99999/~clearSearch=N/~PAGING=N</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonPagingOff}</a>
        <#else>
          <a href="<@ofbizUrl>keywordsearch/~VIEW_INDEX=0/~VIEW_SIZE=${previousViewSize}/~clearSearch=N/~PAGING=Y</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonPagingOn}</a>
        </#if>
        </b>
      </td>
    </tr>
    <tr><td colspan="2"><hr class="sepbar"/></td></tr>
</table>

<form method="post" name="products">
  <table width="100%" cellpadding="0" cellspacing="0">
    <#assign listIndex = lowIndex>
    <#list productIds as productId><#-- note that there is no boundary range because that is being done before the list is put in the content -->
      <#assign product = delegator.findByPrimaryKey("Product", Static["org.ofbiz.base.util.UtilMisc"].toMap("productId", productId))>
      <tr>
        <td>
          <input type="checkbox" name="selectResult" value="${productId}" onchange="checkProductToBagTextArea(this, '${productId}');"/>
          <a href="<@ofbizUrl>EditProduct?productId=${productId}</@ofbizUrl>" class="buttontext">[${productId}] ${(product.internalName)?if_exists}</a>
        </td>
      </tr>
    </#list>
  </table>
<form>

<table border="0" width="100%" cellpadding="2">
    <tr>
      <td align="right">
        <b>
        <#if 0 < viewIndex?int>
          <a href="<@ofbizUrl>keywordsearch/~VIEW_INDEX=${viewIndex-1}/~VIEW_SIZE=${viewSize}/~clearSearch=N/~PAGING=${paging}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonPrevious}</a> |
        </#if>
        <#if 0 < listSize?int>
          <span class="tabletext">${lowIndex+1} - ${highIndex} ${uiLabelMap.CommonOf} ${listSize}</span>
        </#if>
        <#if highIndex?int < listSize?int>
          | <a href="<@ofbizUrl>keywordsearch/~VIEW_INDEX=${viewIndex+1}/~VIEW_SIZE=${viewSize}/~clearSearch=N/~PAGING=${paging}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonNext}</a>
        </#if>
        <#if paging == "Y">
          <a href="<@ofbizUrl>keywordsearch/~VIEW_INDEX=0/~VIEW_SIZE=99999/~clearSearch=N/~PAGING=N</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonPagingOff}</a>
        <#else>
          <a href="<@ofbizUrl>keywordsearch/~VIEW_INDEX=0/~VIEW_SIZE=${previousViewSize}/~clearSearch=N/~PAGING=Y</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonPagingOn}</a>
        </#if>
        </b>
      </td>
    </tr>
</table>
</#if>
