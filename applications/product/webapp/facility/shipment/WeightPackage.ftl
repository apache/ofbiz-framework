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

<#if security.hasEntityPermission("FACILITY", "_VIEW", session)>
  <div class="screenlet">
    <div class="screenlet-title-bar">
      <ul>
        <li class="h3">${uiLabelMap.ProductWeightPackageOnly}&nbsp;in&nbsp;${facility.facilityName?if_exists} [${(facility.facilityId)?if_exists}]</li>
      </ul>
      <br class="clear"/>
    </div>
    <div class="screenlet-body">
      <#assign packedLines = weightPackageSession.getPackedLines(orderId)/>
      <#if packedLines?has_content>
        <table class="basic-table" cellpadding="2" cellspacing='0'>
          <tr>
            <th>
              ${uiLabelMap.ProductPackedWeight} (${("uiLabelMap.ProductShipmentUomAbbreviation_" + defaultWeightUomId)?eval}):
            </th>
            <th>
              ${uiLabelMap.CommonDimension} (${("uiLabelMap.ProductShipmentUomAbbreviation_" + defaultDimensionUomId)?eval}):
            </th>
            <th>
              ${uiLabelMap.ProductPackageInputBox}:
            </th>
          </tr>
          <#list packedLines as packedLine>
              <form name="updateWeightPackageForm_${packedLine.getWeightPackageSeqId()}" method="post" action="<@ofbizUrl>updatePackedLine</@ofbizUrl>">
                <input type="hidden" name="orderId" value ="${orderId?if_exists}"/>
                <input type="hidden" name = "facilityId" value = "${(facility.facilityId)?if_exists}"/>
                <input type="hidden" name="weightPackageSeqId" value ="${packedLine.getWeightPackageSeqId()}"/>
                <tr>
                  <td>
                    <span class="label">
                      ${uiLabelMap.ProductPackage} ${packedLine.getWeightPackageSeqId()}
                      <input type="text" size="7" name="packageWeight" value="${(packedLine.getPackageWeight())?if_exists}">
                    </span>
                  </td>
                  <td>
                    <span class="label">${uiLabelMap.CommonLength}<input type="text" name="packageLength" value="${(packedLine.getPackageLength())?if_exists}" size="5"/></span>
                    <span class="label">${uiLabelMap.ProductWidth}<input type="text" name="packageWidth" value="${(packedLine.getPackageWidth())?if_exists}" size="5"/></span>
                    <span class="label">${uiLabelMap.PartyHeight}<input type="text" name="packageHeight" value="${(packedLine.getPackageHeight())?if_exists}" size="5"/></span>
                  </td>
                  <td>
                    <select name="shipmentBoxTypeId">
                      <#if shipmentBoxTypes?has_content>
                        <#assign shipmentBoxTypeId = "${(packedLine.getShipmentBoxTypeId())?if_exists}"/>
                        <#list shipmentBoxTypes as shipmentBoxType>
                          <#if shipmentBoxTypeId == "${shipmentBoxType.shipmentBoxTypeId}">
                            <option value="${shipmentBoxType.shipmentBoxTypeId}">${shipmentBoxType.description}</option>
                          </#if>
                        </#list>
                        <option value=""></option>
                        <#list shipmentBoxTypes as shipmentBoxType>
                          <option value="${shipmentBoxType.shipmentBoxTypeId}">${shipmentBoxType.description}</option>
                        </#list>
                      </#if>
                    </select>
                  </td>
                  <td align="right"><a href="javascript:document.updateWeightPackageForm_${packedLine.getWeightPackageSeqId()}.submit()" class="buttontext">${uiLabelMap.CommonUpdate}</a></td>
                  <td align="right"><a href="javascript:document.updateWeightPackageForm_${packedLine.getWeightPackageSeqId()}.action='<@ofbizUrl>deletePackedLine</@ofbizUrl>';document.updateWeightPackageForm_${packedLine.getWeightPackageSeqId()}.submit();" class="buttontext">${uiLabelMap.CommonDelete}</a></div>
                </tr>
              </form>
          </#list>
        </table>
        <div align="right">
          <a href="javascript:document.completePackageForm.submit()" class="buttontext">${uiLabelMap.ProductComplete}</a>
        </div>
        <form name="completePackageForm" method ="post" action="<@ofbizUrl>completePackage</@ofbizUrl>">
          <input type="hidden" name="orderId" value="${orderId?if_exists}"/>
          <input type="hidden" name="shipGroupSeqId" value="${shipGroupSeqId?if_exists}"/>
          <input type="hidden" name="facilityId" value="${(facility.facilityId)?if_exists}"/>
          <input type="hidden" name="weightUomId" value="${defaultWeightUomId}"/>
          <input type="hidden" name="dimensionUomId" value="${defaultDimensionUomId}"/>
          <input type="hidden" name="shipmentId" value="${(shipment.shipmentId)?default("")}"/>
          <input type="hidden" name="invoiceId" value="${(invoice.invoiceId)?default("")}"/>
          <input type="hidden" name="estimatedShippingCost" value="${estimatedShippingCost?if_exists}"/>
        </form>
      </#if>
      <table class="basic-table" cellpadding="2" cellspacing='0'>
        <form name="weightPackageForm" method ="post" action="<@ofbizUrl>setPackageInfo</@ofbizUrl>">
          <input type="hidden" name = "shipGroupSeqId" value = "${shipGroupSeqId?if_exists}"/>
          <input type="hidden" name = "facilityId" value = "${(facility.facilityId)?if_exists}"/>
          <input type="hidden" name = "orderId" value = "${orderId?if_exists}"/>
          <#assign packedLines = weightPackageSession.getPackedLines(orderId)/>
          <#if packedLines?has_content>
            <hr>
          </#if>
          <tr>
            <td>
             <span class="label">${uiLabelMap.ProductPackedWeight} (${("uiLabelMap.ProductShipmentUomAbbreviation_" + defaultWeightUomId)?eval}):
                <br/>
                ${uiLabelMap.ProductPackage}
                <input type="text" size="7" name="packageWeight" value=""/>
              </span>
            </td>
            <td>
              <span class="label">${uiLabelMap.CommonDimension} (${("uiLabelMap.ProductShipmentUomAbbreviation_" + defaultDimensionUomId)?eval}):</span>
              <br/>
              <span class="label">${uiLabelMap.CommonLength}<input type="text" name="packageLength" value="" size="5"/></span>
              <span class="label">${uiLabelMap.ProductWidth}<input type="text" name="packageWidth" value="" size="5"/></span>
              <span class="label">${uiLabelMap.PartyHeight}<input type="text" name="packageHeight" value="" size="5"/></span>
            </td>
            <td>
              <span class="label">${uiLabelMap.ProductPackageInputBox}:</span>
              <br/>
              <select name="shipmentBoxTypeId">
                <#if shipmentBoxTypes?has_content>
                  <option value=""></option>
                  <#list shipmentBoxTypes as shipmentBoxType>
                    <option value="${shipmentBoxType.shipmentBoxTypeId}">${shipmentBoxType.description}</option>
                  </#list>
                </#if>
              </select>
            </td>
            <td align="right"><a href="javascript:document.weightPackageForm.submit()" class="buttontext">${uiLabelMap.ProductNextPackage}</a></td>
          </form>
        </table>
      </div>
    </div>
  </div>
</#if>