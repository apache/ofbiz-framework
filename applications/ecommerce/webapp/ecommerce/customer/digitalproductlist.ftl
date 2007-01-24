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
    <div class="screenlet-header">
        <div class="boxhead">${uiLabelMap.PageTitleDigitalProductList}</div>
    </div>
    <div class="screenlet-body">
       <div>&nbsp;<a href="<@ofbizUrl>digitalproductedit</@ofbizUrl>" class="buttontext">${uiLabelMap.EcommerceDigitalNewProduct}</a></div>
        <table width="100%" cellpadding="1" cellspacing="0" border="0">
          <tr>
            <td width="30%">
              <div class="tabletext"><b>${uiLabelMap.ProductProductName}</b></div>
            </td>
            <td width="5">&nbsp;</td>
            <td width="45%">
              <div class="tabletext"><b>${uiLabelMap.CommonDescription}</b></div>
            </td>
            <td width="5">&nbsp;</td>
            <td width="20%">&nbsp;</td>                
          </tr>
          <#list supplierProductList as supplierProduct>
            <#assign product = supplierProduct.getRelatedOneCache("Product")/>
            <tr><td colspan="5"><hr class="sepbar"/></td></tr>
            <tr>
              <td>
                <div class="tabletext">${(product.productName)?if_exists}</div>
              </td>
              <td width="5">&nbsp;</td>
              <td>
                <div class="tabletext">${(product.description)?if_exists}</div>
              </td>
              <td width="5">&nbsp;</td>
              <td align="right">
                <a href="<@ofbizUrl>digitalproductedit?productId=${supplierProduct.productId}&amp;currencyUomId=${supplierProduct.currencyUomId}&amp;minimumOrderQuantity=${supplierProduct.minimumOrderQuantity}&amp;availableFromDate=${supplierProduct.availableFromDate}</@ofbizUrl>" class="buttontext">Edit</a>
              </td>
            </tr>
          </#list>
          <#if !supplierProductList?has_content>
            <tr><td colspan="5"><div class="head3">${uiLabelMap.EcommerceNoDigitalProductsFound}</div></td></tr>
          </#if>
        </table>
    </div>
</div>

<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxhead">${uiLabelMap.EcommerceDigitalProductPurchaseHistoryCommission}</div>
    </div>
    <div class="screenlet-body">
       <div>&nbsp;<a href="<@ofbizUrl>digitalproductedit</@ofbizUrl>" class="buttontext">${uiLabelMap.EcommerceDigitalNewProduct}</a></div>
    </div>
</div>
