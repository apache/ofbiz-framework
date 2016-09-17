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
<div class="screenlet">
  <div class="screenlet-title-bar">
    <h3>${uiLabelMap.ProductSearchProducts}, ${uiLabelMap.ProductSearchFor}:</h3>
  </div>
  <div class="screenlet-body">
    <#list searchConstraintStrings as searchConstraintString>
      <div>&nbsp;<a href="<@ofbizUrl>keywordsearch?removeConstraint=${searchConstraintString_index}&amp;clearSearch=N&amp;SEARCH_CATEGORY_ID=${parameters.SEARCH_CATEGORY_ID!}</@ofbizUrl>" class="buttontext">X</a>&nbsp;${searchConstraintString}</div>
    </#list>
    <span class="label">${uiLabelMap.CommonSortedBy}:</span>${searchSortOrderString}
    <div><a href="<@ofbizUrl>advancedsearch?SEARCH_CATEGORY_ID=${(requestParameters.SEARCH_CATEGORY_ID)!}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonRefineSearch}</a></div>

    <#if !productIds?has_content>
      <br /><h2>&nbsp;${uiLabelMap.ProductNoResultsFound}.</h2>
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

    <table cellspacing="0" class="basic-table">
        <tr>
          <td><label><input type="checkbox" name="selectAll" value="0" class="selectAll" form="products"/> <b>${uiLabelMap.ProductProduct}</b></label></td>
          <td align="right">
            <b>
            <#if 0 < viewIndex?int>
              <#if parameters.ACTIVE_PRODUCT?has_content && parameters.GOOGLE_SYNCED?has_content && parameters.DISCONTINUED_PRODUCT?has_content>
                <a href="<@ofbizUrl>keywordsearch/~VIEW_INDEX=${viewIndex-1}/~VIEW_SIZE=${viewSize}/~clearSearch=N/~PAGING=${paging}/~noConditionFind=${noConditionFind}/~ACTIVE_PRODUCT=${parameters.ACTIVE_PRODUCT}/~GOOGLE_SYNCED=${parameters.GOOGLE_SYNCED}/~DISCONTINUED_PRODUCT=${parameters.DISCONTINUED_PRODUCT}/~productStoreId=${parameters.productStoreId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonPrevious}</a> |
              <#else>
                <a href="<@ofbizUrl>keywordsearch/~VIEW_INDEX=${viewIndex-1}/~VIEW_SIZE=${viewSize}/~clearSearch=N/~PAGING=${paging}/~noConditionFind=${noConditionFind}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonPrevious}</a>
              </#if>
            </#if>
            <#if 0 < listSize?int>
              ${lowIndex+1} - ${highIndex} ${uiLabelMap.CommonOf} ${listSize}
            </#if>
            <#if highIndex?int < listSize?int>
                <#if parameters.ACTIVE_PRODUCT?has_content && parameters.GOOGLE_SYNCED?has_content && parameters.DISCONTINUED_PRODUCT?has_content>
              |     <a href="<@ofbizUrl>keywordsearch/~VIEW_INDEX=${viewIndex+1}/~VIEW_SIZE=${viewSize}/~clearSearch=N/~PAGING=${paging}/~noConditionFind=${noConditionFind}/~ACTIVE_PRODUCT=${parameters.ACTIVE_PRODUCT}/~GOOGLE_SYNCED=${parameters.GOOGLE_SYNCED}/~DISCONTINUED_PRODUCT=${parameters.DISCONTINUED_PRODUCT}/~productStoreId=${parameters.productStoreId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonNext}</a>
                <#else>
                    <a href="<@ofbizUrl>keywordsearch/~VIEW_INDEX=${viewIndex+1}/~VIEW_SIZE=${viewSize}/~clearSearch=N/~PAGING=${paging}/~noConditionFind=${noConditionFind}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonNext}</a>
                </#if>
            </#if>
            <#if paging == "Y">
              <#if parameters.ACTIVE_PRODUCT?has_content && parameters.GOOGLE_SYNCED?has_content && parameters.DISCONTINUED_PRODUCT?has_content>
                <a href="<@ofbizUrl>keywordsearch/~VIEW_INDEX=0/~VIEW_SIZE=99999/~clearSearch=N/~PAGING=N/~noConditionFind=${noConditionFind}/~ACTIVE_PRODUCT=${parameters.ACTIVE_PRODUCT}/~GOOGLE_SYNCED=${parameters.GOOGLE_SYNCED}/~DISCONTINUED_PRODUCT=${parameters.DISCONTINUED_PRODUCT}/~productStoreId=${parameters.productStoreId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonPagingOff}</a>
              <#else>
                <a href="<@ofbizUrl>keywordsearch/~VIEW_INDEX=0/~VIEW_SIZE=99999/~clearSearch=N/~PAGING=N/~noConditionFind=${noConditionFind}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonPagingOff}</a>
              </#if>
            <#else>
                <#if parameters.ACTIVE_PRODUCT?has_content && parameters.GOOGLE_SYNCED?has_content && parameters.DISCONTINUED_PRODUCT?has_content>
                    <a href="<@ofbizUrl>keywordsearch/~VIEW_INDEX=0/~VIEW_SIZE=${previousViewSize}/~clearSearch=N/~PAGING=Y/~noConditionFind=${noConditionFind}/~ACTIVE_PRODUCT=${parameters.ACTIVE_PRODUCT}/~GOOGLE_SYNCED=${parameters.GOOGLE_SYNCED}/~DISCONTINUED_PRODUCT=${parameters.DISCONTINUED_PRODUCT}/~productStoreId=${parameters.productStoreId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonPagingOn}</a>
                <#else>
                    <a href="<@ofbizUrl>keywordsearch/~VIEW_INDEX=0/~VIEW_SIZE=${previousViewSize}/~clearSearch=N/~PAGING=Y/~noConditionFind=${noConditionFind}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonPagingOn}</a>
                </#if>
            </#if>
            </b>
          </td>
        </tr>
        <tr><td colspan="2"><hr /></td></tr>
    </table>

    <form method="post" name="products" id="products">
      <input type="hidden" name="productStoreId" value="${parameters.productStoreId!}" />
      <table cellspacing="0" class="basic-table">
        <#assign listIndex = lowIndex>
        <#assign rowClass = "2">
        <#list productIds as productId><#-- note that there is no boundary range because that is being done before the list is put in the content -->
          <#assign product = delegator.findOne("Product", Static["org.apache.ofbiz.base.util.UtilMisc"].toMap("productId", productId), false)>
          <tr valign="middle"<#if rowClass == "1"> class="alternate-row"</#if>>
            <td>
              <input type="checkbox" name="selectResult" value="${productId}" onchange="checkProductToBagTextArea(this, '${productId}');"/>
              <a href="<@ofbizUrl>EditProduct?productId=${productId}</@ofbizUrl>" class="buttontext">[${productId}] ${(product.internalName)!}</a>
            </td>
          </tr>
          <#-- toggle the row color -->
          <#if rowClass == "2">
            <#assign rowClass = "1">
          <#else>
            <#assign rowClass = "2">
          </#if>
        </#list>
      </table>
    </form>

    <table cellspacing="0" class="basic-table">
        <tr><td colspan="2"><hr /></td></tr>
        <tr>
          <td align="right">
            <b>
            <#if 0 < viewIndex?int>
              <#if parameters.ACTIVE_PRODUCT?has_content && parameters.GOOGLE_SYNCED?has_content && parameters.DISCONTINUED_PRODUCT?has_content>
                <a href="<@ofbizUrl>keywordsearch/~VIEW_INDEX=${viewIndex-1}/~VIEW_SIZE=${viewSize}/~clearSearch=N/~PAGING=${paging}/~noConditionFind=${noConditionFind}/~ACTIVE_PRODUCT=${parameters.ACTIVE_PRODUCT}/~GOOGLE_SYNCED=${parameters.GOOGLE_SYNCED}/~DISCONTINUED_PRODUCT=${parameters.DISCONTINUED_PRODUCT}/~productStoreId=${parameters.productStoreId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonPrevious}</a> |
              <#else>
                <a href="<@ofbizUrl>keywordsearch/~VIEW_INDEX=${viewIndex-1}/~VIEW_SIZE=${viewSize}/~clearSearch=N/~PAGING=${paging}/~noConditionFind=${noConditionFind}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonPrevious}</a>
              </#if>
            </#if>
            <#if 0 < listSize?int>
              ${lowIndex+1} - ${highIndex} ${uiLabelMap.CommonOf} ${listSize}
            </#if>
            <#if highIndex?int < listSize?int>
                <#if parameters.ACTIVE_PRODUCT?has_content && parameters.GOOGLE_SYNCED?has_content && parameters.DISCONTINUED_PRODUCT?has_content>
              |     <a href="<@ofbizUrl>keywordsearch/~VIEW_INDEX=${viewIndex+1}/~VIEW_SIZE=${viewSize}/~clearSearch=N/~PAGING=${paging}/~noConditionFind=${noConditionFind}/~ACTIVE_PRODUCT=${parameters.ACTIVE_PRODUCT}/~GOOGLE_SYNCED=${parameters.GOOGLE_SYNCED}/~DISCONTINUED_PRODUCT=${parameters.DISCONTINUED_PRODUCT}/~productStoreId=${parameters.productStoreId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonNext}</a>
                <#else>
                    <a href="<@ofbizUrl>keywordsearch/~VIEW_INDEX=${viewIndex+1}/~VIEW_SIZE=${viewSize}/~clearSearch=N/~PAGING=${paging}/~noConditionFind=${noConditionFind}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonNext}</a>
                </#if>
            </#if>
            <#if paging == "Y">
              <#if parameters.ACTIVE_PRODUCT?has_content && parameters.GOOGLE_SYNCED?has_content && parameters.DISCONTINUED_PRODUCT?has_content>
                <a href="<@ofbizUrl>keywordsearch/~VIEW_INDEX=0/~VIEW_SIZE=99999/~clearSearch=N/~PAGING=N/~noConditionFind=${noConditionFind}/~ACTIVE_PRODUCT=${parameters.ACTIVE_PRODUCT}/~GOOGLE_SYNCED=${parameters.GOOGLE_SYNCED}/~DISCONTINUED_PRODUCT=${parameters.DISCONTINUED_PRODUCT}/~productStoreId=${parameters.productStoreId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonPagingOff}</a>
              <#else>
                <a href="<@ofbizUrl>keywordsearch/~VIEW_INDEX=0/~VIEW_SIZE=99999/~clearSearch=N/~PAGING=N/~noConditionFind=${noConditionFind}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonPagingOff}</a>
              </#if>
            <#else>
                <#if parameters.ACTIVE_PRODUCT?has_content && parameters.GOOGLE_SYNCED?has_content && parameters.DISCONTINUED_PRODUCT?has_content>
                    <a href="<@ofbizUrl>keywordsearch/~VIEW_INDEX=0/~VIEW_SIZE=${previousViewSize}/~clearSearch=N/~PAGING=Y/~noConditionFind=${noConditionFind}/~ACTIVE_PRODUCT=${parameters.ACTIVE_PRODUCT}/~GOOGLE_SYNCED=${parameters.GOOGLE_SYNCED}/~DISCONTINUED_PRODUCT=${parameters.DISCONTINUED_PRODUCT}/~productStoreId=${parameters.productStoreId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonPagingOn}</a>
                <#else>
                    <a href="<@ofbizUrl>keywordsearch/~VIEW_INDEX=0/~VIEW_SIZE=${previousViewSize}/~clearSearch=N/~PAGING=Y/~noConditionFind=${noConditionFind}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonPagingOn}</a>
                </#if>
            </#if>
            </b>
          </td>
        </tr>
    </table>
    </#if>
  </div>
</div>
