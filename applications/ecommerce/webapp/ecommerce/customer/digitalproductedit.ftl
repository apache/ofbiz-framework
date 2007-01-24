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

<#assign productPrice = productPriceList[0]?if_exists/>

<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxhead">${uiLabelMap.PageTitleDigitalProductEdit}</div>
    </div>
    <div class="screenlet-body">
<#if !supplierProduct?has_content && parameters.productId?has_content>
  <div><h3>${uiLabelMap.EcommerceMessage1} [${parameters.productId}] ${uiLabelMap.EcommerceMessage2}</h3></div>
<#else>

    <#if !supplierProduct?exists>
      <p class="head1">${uiLabelMap.EcommerceAddNewDigitalProduct}</p>
      <form method="post" action="<@ofbizUrl>createCustomerDigitalDownloadProduct</@ofbizUrl>" name="editdigitaluploadform" style="margin: 0;">
        <input type="hidden" name="productStoreId" value="${productStore.productStoreId}">
    <#else>
      <p class="head1">${uiLabelMap.EcommerceUpdateDigitalProduct}</p>
      <form method="post" action="<@ofbizUrl>updateCustomerDigitalDownloadProduct</@ofbizUrl>" name="editdigitaluploadform" style="margin: 0;">
        <input type="hidden" name="productId" value="${parameters.productId}">
        <input type="hidden" name="currencyUomId" value="${parameters.currencyUomId}">
        <input type="hidden" name="minimumOrderQuantity" value="${parameters.minimumOrderQuantity}">
        <input type="hidden" name="availableFromDate" value="${parameters.availableFromDate}">
    </#if>
    &nbsp;<a href="<@ofbizUrl>digitalproductlist</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonBackToList}</a>

    <table width="90%" border="0" cellpadding="2" cellspacing="0">
    <tr>
      <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.ProductProductName}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%"><input type="text" class="inputBox" size="30" maxlength="60" name="productName" value="${(product.productName)?if_exists}"/>*</td>
    </tr>
    <tr>
      <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.ProductDescription}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%"><input type="text" class="inputBox" size="30" maxlength="60" name="description" value="${(product.description)?if_exists}"/></td>
    </tr>
    <tr>
      <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.ProductPrice}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%"><input type="text" class="inputBox" size="30" maxlength="60" name="price" value="${(productPrice.price)?if_exists}"/>*</td>
    </tr>
    <tr>
      <td width="26%" align="right" valign="top"><div class="tabletext">&nbsp;</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%"><a href="javascript:document.editdigitaluploadform.submit()" class="buttontext">${uiLabelMap.CommonSave}</a></td>
    </tr>
  </table>
  </form>
</#if>
    </div>
</div>

<#if supplierProduct?has_content>
<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxhead">${uiLabelMap.EcommerceDigitalProductFiles}</div>
    </div>
    <div class="screenlet-body">
        <#list productContentAndInfoList as productContentAndInfo>
            <div class="tabletext">
              ${productContentAndInfo.contentName} (${uiLabelMap.CommonSince}: ${productContentAndInfo.fromDate})
              <a href="<@ofbizUrl>removeCustomerDigitalDownloadProductFile?contentId=${productContentAndInfo.contentId}&amp;productContentTypeId=${productContentAndInfo.productContentTypeId}&amp;fromDate=${productContentAndInfo.fromDate}&amp;productId=${parameters.productId}&amp;currencyUomId=${parameters.currencyUomId}&amp;minimumOrderQuantity=${parameters.minimumOrderQuantity}&amp;availableFromDate=${parameters.availableFromDate}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonRemove}</a>
            </div>
        </#list>
    
        <div><hr class="sepbar"/></div>
        <div class="tableheadtext">${uiLabelMap.EcommerceDigitalAddFromMyFiles}</div>
        <div>
        <form method="post" action="<@ofbizUrl>addCustomerDigitalDownloadProductFile</@ofbizUrl>" name="adddigitaluploadfile" style="margin: 0;">
          <input type="hidden" name="productId" value="${parameters.productId}">
          <input type="hidden" name="currencyUomId" value="${parameters.currencyUomId}">
          <input type="hidden" name="minimumOrderQuantity" value="${parameters.minimumOrderQuantity}">
          <input type="hidden" name="availableFromDate" value="${parameters.availableFromDate}">
          <select name="contentId" class="selectBox">
            <#list ownerContentAndRoleList as ownerContentAndRole>
              <option value="${ownerContentAndRole.contentId}">${ownerContentAndRole.contentName}</option>
            </#list>
          </select>
          <a href="javascript:document.adddigitaluploadfile.submit()" class="buttontext">${uiLabelMap.CommonAdd}</a>
        </form>
        </div>
    &nbsp;<a href="<@ofbizUrl>digitalproductlist</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonBackToList}</a>
    </div>
</div>
</#if>
