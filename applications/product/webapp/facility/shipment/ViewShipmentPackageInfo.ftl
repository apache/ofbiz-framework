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
<#if shipmentPackageDatas?has_content>
  <br/>
  <table width="100%" cellspacing="0" cellpadding="2" border="1">
    <tr>
      <td><div class="tableheadtext">${uiLabelMap.ProductPackage}</div></td>
      <td><div class="tableheadtext">${uiLabelMap.CommonCreated}</div></td>
      <td><div class="tableheadtext">&nbsp;</div></td>
      <td><div class="tableheadtext">&nbsp;</div></td>
    </tr>
    <#list shipmentPackageDatas as shipmentPackageData>
      <#assign shipmentPackage = shipmentPackageData.shipmentPackage>
      <#assign shipmentPackageContents = shipmentPackageData.shipmentPackageContents?if_exists>
      <#assign shipmentPackageRouteSegs = shipmentPackageData.shipmentPackageRouteSegs?if_exists>
      <#assign weightUom = shipmentPackageData.weightUom?if_exists>
      <tr>
        <td><div class="tabletext">${shipmentPackage.shipmentPackageSeqId}</div></td>
        <td><div class="tabletext">${(shipmentPackage.dateCreated.toString())?if_exists}</div></td>
        <td><span class="tabletext">${uiLabelMap.ProductWeight} : ${shipmentPackage.weight?if_exists}</span></td>
        <td><span class="tabletext">${uiLabelMap.ProductWeightUnit} : <#if weightUom?has_content>${weightUom.get("description",locale)}<#else>${shipmentPackage.weightUomId?if_exists}</#if></span></td>
      </tr>
      <#list shipmentPackageContents as shipmentPackageContent>
        <tr>
          <td><div class="tabletext">&nbsp;</div></td>
          <td><div class="tabletext">${uiLabelMap.ProductItem} :${shipmentPackageContent.shipmentItemSeqId}</div></td>
          <td><div class="tabletext">${uiLabelMap.ProductQuantity} :${shipmentPackageContent.quantity?if_exists}</div></td>
          <td><div class="tabletext">&nbsp;</div></td>
        </tr>
      </#list>
      <#list shipmentPackageRouteSegs as shipmentPackageRouteSeg>
        <tr>
          <td><div class="tabletext">&nbsp;</div></td>
          <td><div class="tabletext">${uiLabelMap.ProductRouteSegment} :${shipmentPackageRouteSeg.shipmentRouteSegmentId}</div></td>
          <td><span class="tabletext">${uiLabelMap.ProductTracking} : ${shipmentPackageRouteSeg.trackingCode?if_exists}</span></td>
          <td><span class="tabletext">${uiLabelMap.ProductBox} : ${shipmentPackageRouteSeg.boxNumber?if_exists}</span></td>
        </tr>
      </#list>
    </#list>
  </table>
</#if>
