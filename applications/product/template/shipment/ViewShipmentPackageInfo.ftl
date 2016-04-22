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
<div class="screenlet">
    <div class="screenlet-body">
      <table cellspacing="0" cellpadding="2" class="basic-table">
        <tr class="header-row">
          <td>${uiLabelMap.ProductPackage}</td>
          <td>${uiLabelMap.CommonCreated}</td>
          <td>&nbsp;</td>
          <td>&nbsp;</td>
        </tr>
        <#assign alt_row = false>
        <#list shipmentPackageDatas as shipmentPackageData>
          <#assign shipmentPackage = shipmentPackageData.shipmentPackage>
          <#assign shipmentPackageContents = shipmentPackageData.shipmentPackageContents!>
          <#assign shipmentPackageRouteSegs = shipmentPackageData.shipmentPackageRouteSegs!>
          <#assign weightUom = shipmentPackageData.weightUom!>
          <tr valign="middle"<#if alt_row> class="alternate-row"</#if>>
            <td>${shipmentPackage.shipmentPackageSeqId}</td>
            <td>${(shipmentPackage.dateCreated.toString())!}</td>
            <td><span class="label">${uiLabelMap.ProductWeight}</span> ${shipmentPackage.weight!}</td>
            <td><span class="label">${uiLabelMap.ProductWeightUnit}</span> <#if weightUom?has_content>${weightUom.get("description",locale)}<#else>${shipmentPackage.weightUomId!}</#if></td>
          </tr>
          <#list shipmentPackageContents as shipmentPackageContent>
            <tr valign="middle"<#if alt_row> class="alternate-row"</#if>>
              <td>&nbsp;</td>
              <td><span class="label">${uiLabelMap.ProductItem}</span> ${shipmentPackageContent.shipmentItemSeqId}</td>
              <td><span class="label">${uiLabelMap.ProductQuantity}</span> ${shipmentPackageContent.quantity!}</td>
              <td>&nbsp;</td>
            </tr>
          </#list>
          <#list shipmentPackageRouteSegs as shipmentPackageRouteSeg>
            <tr valign="middle"<#if alt_row> class="alternate-row"</#if>>
              <td>&nbsp;</td>
              <td><span class="label">${uiLabelMap.ProductRouteSegment}</span> ${shipmentPackageRouteSeg.shipmentRouteSegmentId}</td>
              <td><span class="label">${uiLabelMap.ProductTracking}</span> ${shipmentPackageRouteSeg.trackingCode!}</td>
              <td><span class="label">${uiLabelMap.ProductBox}</span> ${shipmentPackageRouteSeg.boxNumber!}</td>
            </tr>
          </#list>
          <#-- toggle the row color -->
          <#assign alt_row = !alt_row>
        </#list>
      </table>
    </div>
</div>
</#if>