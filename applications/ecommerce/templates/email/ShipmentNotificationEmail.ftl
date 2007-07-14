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

<#if baseEcommerceSecureUrl?exists><#assign urlPrefix = baseEcommerceSecureUrl/></#if>
<div class="screenlet">
  <div class="screenlet-header">  
     <div class="boxhead">&nbsp; ${uiLabelMap.ShippedItems}</div>
  </div>   
  <div class="screenlet-body">
    <table width="100%" border="0" cellpadding="0">
      <tr align="left" valign="bottom">
        <td width="35%" align="left"><span class="tableheadtext"><b>${uiLabelMap.EcommerceProduct}</b></span></td>               
        <td width="10%" align="right"><span class="tableheadtext"><b>${uiLabelMap.OrderQuantity}</b></span></td>
      </tr>
      <tr><td colspan="10"><hr class="sepbar"/></td></tr>
      <#list shipmentPackages as shipmentPackage>
        <#assign shipmentPackageContents = shipmentPackage.getRelated("ShipmentPackageContent")>
        <#list shipmentPackageContents as shipmentPackageContent>
          <#assign shipmentItem = shipmentPackageContent.getRelatedOne("ShipmentItem")>
          <#assign productId = shipmentItem.productId>
          <#assign product = shipmentItem.getRelatedOne("Product")>
          <tr>
            <td colspan="1" valign="top"> ${productId?if_exists} - ${product.internalName?if_exists}</td>   
            <td align="right" valign="top"> ${shipmentItem.quantity?if_exists}</td>   
          </tr>
        </#list>
        <tr><td colspan="10"><hr class="sepbar"/></td></tr>
      </#list>
    </table>
  </div>
</div>
