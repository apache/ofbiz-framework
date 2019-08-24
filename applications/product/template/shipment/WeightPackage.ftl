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
  <#if !(showWarningForm)>
    <div class="screenlet">
      <div class="screenlet-title-bar">
        <ul>
          <li class="h3">${uiLabelMap.ProductWeighPackageOnly}&nbsp;in&nbsp;${facility.facilityName!} [${(facility.facilityId)!}]</li>
        </ul>
        <br class="clear"/>
      </div>
      <div class="screenlet-body">
        <#if invoiceIds?has_content>
          <div>
            ${uiLabelMap.CommonView} <a href="<@ofbizUrl>/PackingSlip.pdf?shipmentId=${shipmentId}</@ofbizUrl>" target="_blank" class="buttontext">${uiLabelMap.ProductPackingSlip}</a> ${uiLabelMap.CommonOr}
            ${uiLabelMap.CommonView} <a href="<@ofbizUrl>/ShipmentBarCode.pdf?shipmentId=${shipmentId}</@ofbizUrl>" target="_blank" class="buttontext">${uiLabelMap.ProductBarcode}</a> ${uiLabelMap.CommonFor} ${uiLabelMap.ProductShipmentId} <a href="<@ofbizUrl>/ViewShipment?shipmentId=${shipmentId}</@ofbizUrl>" class="buttontext">${shipmentId}</a>
          </div>
          <#if invoiceIds?? && invoiceIds?has_content>
            <div>
              <p>${uiLabelMap.AccountingInvoices}:</p>
              <ul>
                <#list invoiceIds as invoiceId>
                  <li>
                    ${uiLabelMap.CommonNbr}<a href="/accounting/control/invoiceOverview?invoiceId=${invoiceId}${StringUtil.wrapString(externalKeyParam)}" target="_blank" class="buttontext">${invoiceId}</a>
                    (<a href="/accounting/control/invoice.pdf?invoiceId=${invoiceId}${StringUtil.wrapString(externalKeyParam)}" target="_blank" class="buttontext">${uiLabelMap.CommonPdf}</a>)
                  </li>
                </#list>
              </ul>
            </div>
          </#if>
        </#if>
        <#if shipmentPackageRouteSegList?? && shipmentPackageRouteSegList?has_content>
          <#list shipmentPackageRouteSegList as shipmentPackageRouteSeg>
            <form name="viewShipmentPackageRouteSegLabelImageForm_${shipmentPackageRouteSeg_index}" method="post" action="<@ofbizUrl>viewShipmentPackageRouteSegLabelImage</@ofbizUrl>">
              <input type="hidden" name="shipmentId" value ="${(shipmentPackageRouteSeg.shipmentId)!}"/>
              <input type="hidden" name ="shipmentPackageSeqId" value = "${(shipmentPackageRouteSeg.shipmentPackageSeqId)!}"/>
              <input type="hidden" name="shipmentRouteSegmentId" value ="${(shipmentPackageRouteSeg.shipmentRouteSegmentId)!}"/>
              <div>
                <span class="label">${uiLabelMap.ProductPackage}</span> ${(shipmentPackageRouteSeg.shipmentPackageSeqId)!}
                <#if shipmentPackageRouteSeg.labelImage??>
                  <a href="javascript:document.viewShipmentPackageRouteSegLabelImageForm_${shipmentPackageRouteSeg_index}.submit();" class="buttontext">${uiLabelMap.ProductViewLabelImage}</a>
                </#if>
              </div>
            </form>
          </#list>
        </#if>
        <br />
        <#if !(orderId?has_content)>
          <form name="selectOrderForm" method="post" action="<@ofbizUrl>WeightPackageOnly</@ofbizUrl>">
            <input type="hidden" name="facilityId" value="${(facility.facilityId)!}" />
            <table cellspacing="0" class="basic-table">
              <tr>
                <td width="25%" align="right"><span class="label">${uiLabelMap.ProductOrderId}</span></td>
                <td width="1">&nbsp;</td>
                <td width="25%">
                  <input type="text" name="orderId" size="20" maxlength="20" value="${primaryOrderId!}"/>
                  /
                  <input type="text" name="shipGroupSeqId" size="6" maxlength="6" value="${shipGroupSeqId?default("00001")}"/>
                </td>
                <td>&nbsp;</td>
              </tr>
              <tr>
                <td colspan="2">&nbsp;</td>
                <td colspan="2">
                  <a href="javascript:document.selectOrderForm.action='<@ofbizUrl>PackOrder</@ofbizUrl>';document.selectOrderForm.submit();" class="buttontext">${uiLabelMap.ProductPackOrder}</a>
                  <a href="javascript:document.selectOrderForm.submit();" class="buttontext">${uiLabelMap.ProductWeighPackageOnly}</a>
                </td>
              </tr>
            </table>
          </form>
          <br />
          <!-- select picklist bin form -->
          <form name="selectPicklistBinForm" method="post" action="<@ofbizUrl>WeightPackageOnly</@ofbizUrl>" style="margin: 0;">
            <input type="hidden" name="facilityId" value="${(facility.facilityId)!}" />
            <table cellspacing="0" class="basic-table">
              <tr>
                <td width="25%" align='right'><span class="label">${uiLabelMap.FormFieldTitle_picklistBinId}</span></td>
                <td width="1">&nbsp;</td>
                <td width="25%">
                  <input type="text" name="picklistBinId" size="29" maxlength="60" value="${picklistBinId!}"/>
                </td>
                <td>&nbsp;</td>
              </tr>
              <tr>
                <td colspan="2">&nbsp;</td>
                <td colspan="1">
                  <a href="javascript:document.selectPicklistBinForm.action='<@ofbizUrl>PackOrder</@ofbizUrl>';document.selectPicklistBinForm.submit();" class="buttontext">${uiLabelMap.ProductPackOrder}</a>
                  <a href="javascript:document.selectPicklistBinForm.submit();" class="buttontext">${uiLabelMap.ProductWeighPackageOnly}</a>
                </td>
              </tr>
            </table>
          </form>
        <#else>
          <#assign packedLines = weightPackageSession.getPackedLines(orderId)/>
          <#if !(shipmentPackages?has_content)>
            <#if packedLines?has_content>
              <table class="basic-table" cellpadding="2" cellspacing='0'>
                <tr>
                  <th>
                    ${uiLabelMap.ProductPackedWeight} <#if defaultWeightUom?has_content>(${defaultWeightUom.get("abbreviation",locale)!})</#if>:
                  </th>
                  <th>
                    ${uiLabelMap.CommonDimension} <#if defaultDimensionUom?has_content>(${defaultDimensionUom.get("abbreviation",locale)!})</#if>:
                  </th>
                  <th>
                    ${uiLabelMap.ProductPackageInputBox}:
                  </th>
                </tr>
                <#list packedLines as packedLine>
                  <form name="updateWeightPackageForm_${packedLine.getWeightPackageSeqId()}" method="post" action="<@ofbizUrl>updatePackedLine</@ofbizUrl>">
                    <input type="hidden" name="orderId" value ="${orderId!}"/>
                    <input type="hidden" name = "facilityId" value = "${(facility.facilityId)!}"/>
                    <input type="hidden" name="weightPackageSeqId" value ="${packedLine.getWeightPackageSeqId()}"/>
                    <tr>
                      <td>
                        <span class="label">
                          ${uiLabelMap.ProductPackage} ${packedLine.getWeightPackageSeqId()}
                          <input type="text" size="7" name="packageWeight" value="${(packedLine.getPackageWeight())!}" />
                        </span>
                      </td>
                      <td>
                        <span class="label">${uiLabelMap.CommonLength}<input type="text" name="packageLength" value="${(packedLine.getPackageLength())!}" size="5"/></span>
                        <span class="label">${uiLabelMap.ProductWidth}<input type="text" name="packageWidth" value="${(packedLine.getPackageWidth())!}" size="5"/></span>
                        <span class="label">${uiLabelMap.PartyHeight}<input type="text" name="packageHeight" value="${(packedLine.getPackageHeight())!}" size="5"/></span>
                      </td>
                      <td>
                        <select name="shipmentBoxTypeId">
                          <#if shipmentBoxTypes?has_content>
                            <#assign shipmentBoxTypeId = "${(packedLine.getShipmentBoxTypeId())!}"/>
                            <#list shipmentBoxTypes as shipmentBoxType>
                              <#if "${shipmentBoxType.shipmentBoxTypeId}" == shipmentBoxTypeId>
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
                      <td align="right"><input type="submit" value="${uiLabelMap.CommonUpdate}" /></td>
                      <td align="right"><a href="javascript:document.updateWeightPackageForm_${packedLine.getWeightPackageSeqId()}.action='<@ofbizUrl>deletePackedLine</@ofbizUrl>';document.updateWeightPackageForm_${packedLine.getWeightPackageSeqId()}.submit();" class="buttontext">${uiLabelMap.CommonDelete}</a></div>
                    </tr>
                  </form>
                </#list>
              </table>
              <div align="right">
                <a href="javascript:document.completePackageForm.submit()" class="buttontext">${uiLabelMap.ProductComplete}</a>
              </div>
              <form name="completePackageForm" method ="post" action="<@ofbizUrl>completePackage</@ofbizUrl>">
                <input type="hidden" name="orderId" value="${orderId!}"/>
                <input type="hidden" name="shipGroupSeqId" value="${shipGroupSeqId!}"/>
                <input type="hidden" name="facilityId" value="${(facility.facilityId)!}"/>
                <input type="hidden" name="weightUomId" value="${defaultWeightUomId}"/>
                <input type="hidden" name="dimensionUomId" value="${defaultDimensionUomId}"/>
                <input type="hidden" name="shipmentId" value="${(shipment.shipmentId)!}"/>
                <input type="hidden" name="invoiceId" value="${(invoice.invoiceId)!}"/>
                <input type="hidden" name="estimatedShippingCost" value="${estimatedShippingCost!}"/>
                <input type="hidden" name="newEstimatedShippingCost" value="${newEstimatedShippingCost!}"/>
              </form>
            </#if>
            <#if (orderedQuantity > packedLines.size())>
            <table class="basic-table" cellpadding="2" cellspacing='0'>
              <form name="weightPackageForm" method ="post" action="<@ofbizUrl>setPackageInfo</@ofbizUrl>">
                <input type="hidden" name = "shipGroupSeqId" value = "${shipGroupSeqId!}"/>
                <input type="hidden" name = "facilityId" value = "${(facility.facilityId)!}"/>
                <input type="hidden" name = "orderId" value = "${orderId!}"/>
                <#assign packedLines = weightPackageSession.getPackedLines(orderId)/>
                <#if packedLines?has_content>
                  <hr/>
                </#if>
                <tr>
                  <td>
                    <span class="label">${uiLabelMap.ProductPackedWeight} <#if defaultWeightUom?has_content>(${defaultWeightUom.get("abbreviation",locale)!})</#if>:
                      <br />
                      ${uiLabelMap.ProductPackage}
                      <input type="text" size="7" name="packageWeight" value=""/>
                    </span>
                  </td>
                  <td>
                    <span class="label">${uiLabelMap.CommonDimension} <#if defaultDimensionUom?has_content>(${defaultDimensionUom.get("abbreviation",locale)!})</#if>:</span>
                    <br />
                    <span class="label">${uiLabelMap.CommonLength}<input type="text" name="packageLength" value="" size="5"/></span>
                    <span class="label">${uiLabelMap.ProductWidth}<input type="text" name="packageWidth" value="" size="5"/></span>
                    <span class="label">${uiLabelMap.PartyHeight}<input type="text" name="packageHeight" value="" size="5"/></span>
                  </td>
                  <td>
                    <span class="label">${uiLabelMap.ProductPackageInputBox}:</span>
                    <br />
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
                </tr>
              </form>
            </table>
            </#if>
          <#else>
            <table class="basic-table" cellpadding="2" cellspacing='0'> 
             <tr>
                <th>
                 ${uiLabelMap.ProductPackedWeight} <#if defaultWeightUom?has_content>(${defaultWeightUom.get("abbreviation",locale)!})</#if>:
                </th>
                 <th>
                  ${uiLabelMap.CommonDimension} <#if defaultDimensionUom?has_content>(${defaultDimensionUom.get("abbreviation",locale)!})</#if>:
                </th>
                <th>
                  ${uiLabelMap.ProductPackageInputBox}:
               </th>
              </tr>
              <form name="completePackForm" method="post" action="<@ofbizUrl>shipNow</@ofbizUrl>">
                <input type="hidden" name="orderId" value="${orderId!}"/>
                <input type="hidden" name="shipGroupSeqId" value="${shipGroupSeqId!}"/>
                <input type="hidden" name="facilityId" value="${(facility.facilityId)!}"/>
                <input type="hidden" name="shipmentId" value="${(shipment.shipmentId)!}"/>
                <input type="hidden" name="invoiceId" value="${(invoice.invoiceId)!}"/> 
                <#list shipmentPackages?sort_by("shipmentPackageSeqId") as shipmentPackage>
                  <tr>
                    <td>
                      <span class="label">
                        ${uiLabelMap.ProductPackage} ${(shipmentPackage_index + 1)}
                        <input type="text" size="7" readonly="readonly" name="packageWeight" value="${(shipmentPackage.weight)!}" />
                      </span>
                    </td>
                    <td>
                      <span class="label">${uiLabelMap.CommonLength}<input type="text" readonly="readonly" name="packageLength" value="${(shipmentPackage.boxLength)!}" size="5"/></span>
                      <span class="label">${uiLabelMap.ProductWidth}<input type="text" readonly="readonly" name="packageWidth" value="${(shipmentPackage.boxWidth)!}" size="5"/></span>
                      <span class="label">${uiLabelMap.PartyHeight}<input type="text" readonly="readonly" name="packageHeight" value="${(shipmentPackage.boxHeight)!}" size="5"/></span>
                    </td>
                    <td>
                      <#if (shipmentPackage.shipmentBoxTypeId)?has_content>
                        <#assign shipmentBoxType = delegator.findOne("ShipmentBoxType", Static["org.apache.ofbiz.base.util.UtilMisc"].toMap("shipmentBoxTypeId", shipmentPackage.shipmentBoxTypeId), true)>
                      </#if>
                      <input type="text" readonly="readonly" name="shipmentBoxTypeId" value="${(shipmentBoxType.description)!}" size="50"/>
                    </td>
                  </tr>
                </#list>
              </form>
            </table>
            <div align="right">
              <a href="javascript:document.completePackForm.submit()" class="buttontext">${uiLabelMap.ProductComplete}</a>
            </div>
          </#if>
        </#if>
      </div>
    </div>
  <#else>
    <div class="screenlet">
      <div class="screenlet-title-bar">
        <ul>
          <li class="h3">${uiLabelMap.WebtoolsWarningLogLevel}:</li>
        </ul>
        <br class="clear"/>
      </div>
      <div class="screenlet-body">
        <div>
          <h3>${uiLabelMap.FacilityWarningMessageThereIsMuchDifferenceInShippingCharges}&nbsp;[${uiLabelMap.FacilityEstimatedShippingCharges} = <@ofbizCurrency amount=estimatedShippingCost! isoCode=shipment.currencyUomId!/>, ${uiLabelMap.FacilityActualShippingCharges} = <@ofbizCurrency amount=newEstimatedShippingCost! isoCode=shipment.currencyUomId!/>]</h3>
        </div>
        <form name="shipNowForm" method="post" action="<@ofbizUrl>shipNow</@ofbizUrl>">
          <input type="hidden" name="orderId" value="${orderId!}"/>
          <input type="hidden" name="shipGroupSeqId" value="${shipGroupSeqId!}"/>
          <input type="hidden" name="facilityId" value="${(facility.facilityId)!}"/>
          <input type="hidden" name="shipmentId" value="${(shipment.shipmentId)!}"/>
        </form>
        <form name="holdShipmentForm" method="post" action="<@ofbizUrl>HoldShipment</@ofbizUrl>">
          <input type="hidden" name="orderId" value="${orderId!}"/>
          <input type="hidden" name="shipGroupSeqId" value="${shipGroupSeqId!}"/>
          <input type="hidden" name="facilityId" value="${(facility.facilityId)!}"/>
        </form>
        <div>
          <a href="javascript:document.shipNowForm.submit()" class="buttontext">${uiLabelMap.FacilityShip} ${uiLabelMap.CommonNow}</a>
          &nbsp;
          <a href="javascript:document.holdShipmentForm.submit()" class="buttontext">${uiLabelMap.FacilityHoldShipment}</a>
        </div>
      </div>
    </div>
  </#if>
</#if>
