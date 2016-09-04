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

<script language="JavaScript" type="text/javascript">
    function clearLine(facilityId, orderId, orderItemSeqId, productId, shipGroupSeqId, inventoryItemId, packageSeqId) {
        document.clearPackLineForm.facilityId.value = facilityId;
        document.clearPackLineForm.orderId.value = orderId;
        document.clearPackLineForm.orderItemSeqId.value = orderItemSeqId;
        document.clearPackLineForm.productId.value = productId;
        document.clearPackLineForm.shipGroupSeqId.value = shipGroupSeqId;
        document.clearPackLineForm.inventoryItemId.value = inventoryItemId;
        document.clearPackLineForm.packageSeqId.value = packageSeqId;
        document.clearPackLineForm.submit();
    }
</script>

<#if security.hasEntityPermission("FACILITY", "_VIEW", session)>
    <#assign showInput = requestParameters.showInput?default("Y")>
    <#assign hideGrid = requestParameters.hideGrid?default("N")>

    <#if (requestParameters.forceComplete?has_content && !invoiceIds?has_content)>
        <#assign forceComplete = "true">
        <#assign showInput = "Y">
    </#if>

    <div class="screenlet">
        <div class="screenlet-title-bar">
            <ul>
                <li class="h3">${uiLabelMap.ProductPackOrder}&nbsp;in&nbsp;${facility.facilityName!} [${facilityId!}]</li>
            </ul>
            <br class="clear"/>
        </div>
        <div class="screenlet-body">
            <#if invoiceIds?has_content>
                <div>
                ${uiLabelMap.CommonView} <a href="<@ofbizUrl>/PackingSlip.pdf?shipmentId=${shipmentId}</@ofbizUrl>" target="_blank" class="buttontext">${uiLabelMap.ProductPackingSlip}</a> ${uiLabelMap.CommonOr}
                ${uiLabelMap.CommonView} <a href="<@ofbizUrl>/ShipmentBarCode.pdf?shipmentId=${shipmentId}</@ofbizUrl>" target="_blank" class="buttontext">${uiLabelMap.ProductBarcode}</a> ${uiLabelMap.CommonFor} ${uiLabelMap.ProductShipmentId} <a href="<@ofbizUrl>/ViewShipment?shipmentId=${shipmentId}</@ofbizUrl>" class="buttontext">${shipmentId}</a>
                </div>
                <#if invoiceIds?exists && invoiceIds?has_content>
                <div>
                    <p>${uiLabelMap.AccountingInvoices}:</p>
                    <ul>
                    <#list invoiceIds as invoiceId>
                      <li>
                        ${uiLabelMap.CommonNbr}<a href="/accounting/control/invoiceOverview?invoiceId=${invoiceId}${StringUtil.wrapString(externalKeyParam)}" target="_blank" class="buttontext">${invoiceId}</a>
                        (<a href="/accounting/control/invoice.pdf?invoiceId=${invoiceId}${StringUtil.wrapString(externalKeyParam)}" target="_blank" class="buttontext">PDF</a>)
                      </li>
                    </#list>
                    </ul>
                </div>
                </#if>
            </#if>
            <br />

            <!-- select order form -->
            <form name="selectOrderForm" method="post" action="<@ofbizUrl>PackOrder</@ofbizUrl>">
              <input type="hidden" name="facilityId" value="${facilityId!}" />
              <table cellspacing="0" class="basic-table">
                <tr>
                  <td width="25%" align="right"><span class="label">${uiLabelMap.ProductOrderId}</span></td>
                  <td width="1">&nbsp;</td>
                  <td width="25%">
                    <input type="text" name="orderId" size="20" maxlength="20" value="${orderId!}"/>
                    /
                    <input type="text" name="shipGroupSeqId" size="6" maxlength="6" value="${shipGroupSeqId?default("00001")}"/>
                  </td>
                  <td><label><span class="label">${uiLabelMap.ProductHideGrid}</span>&nbsp;<input type="checkbox" name="hideGrid" value="Y" <#if (hideGrid == "Y")>checked=""</#if> /></label></td>
                  <td>&nbsp;</td>
                </tr>
                <tr>
                  <td colspan="2">&nbsp;</td>
                  <td colspan="2">
                    <input type="image" src="<@ofbizContentUrl>/images/spacer.gif</@ofbizContentUrl>" onclick="javascript:document.selectOrderForm.submit();" />
                    <a href="javascript:document.selectOrderForm.submit();" class="buttontext">${uiLabelMap.ProductPackOrder}</a>
                    <a href="javascript:document.selectOrderForm.action='<@ofbizUrl>WeightPackageOnly</@ofbizUrl>';document.selectOrderForm.submit();" class="buttontext">${uiLabelMap.ProductWeighPackageOnly}</a>
                  </td>
                </tr>
              </table>
            </form>
            <br />

            <!-- select picklist bin form -->
            <form name="selectPicklistBinForm" method="post" action="<@ofbizUrl>PackOrder</@ofbizUrl>" style="margin: 0;">
              <input type="hidden" name="facilityId" value="${facilityId!}" />
              <table cellspacing="0" class="basic-table">
                <tr>
                  <td width="25%" align='right'><span class="label">${uiLabelMap.FormFieldTitle_picklistBinId}</span></td>
                  <td width="1">&nbsp;</td>
                  <td width="25%">
                    <input type="text" name="picklistBinId" size="29" maxlength="60" value="${picklistBinId!}"/>
                  </td>
                  <td><label><span class="label">${uiLabelMap.ProductHideGrid}</span>&nbsp;<input type="checkbox" name="hideGrid" value="Y" <#if (hideGrid == "Y")>checked=""</#if> /></label></td>
                  <td>&nbsp;</td>
                </tr>
                <tr>
                  <td colspan="2">&nbsp;</td>
                  <td colspan="1">
                    <input type="image" src="<@ofbizContentUrl>/images/spacer.gif</@ofbizContentUrl>" onclick="javascript:document.selectPicklistBinForm.submit();" />
                    <a href="javascript:document.selectPicklistBinForm.submit();" class="buttontext">${uiLabelMap.ProductPackOrder}</a>
                    <a href="javascript:document.selectPicklistBinForm.action='<@ofbizUrl>WeightPackageOnly</@ofbizUrl>';document.selectPicklistBinForm.submit();" class="buttontext">${uiLabelMap.ProductWeighPackageOnly}</a>
                  </td>
                </tr>
              </table>
            </form>
            <form name="clearPackForm" method="post" action="<@ofbizUrl>ClearPackAll</@ofbizUrl>">
              <input type="hidden" name="orderId" value="${orderId!}"/>
              <input type="hidden" name="shipGroupSeqId" value="${shipGroupSeqId!}"/>
              <input type="hidden" name="facilityId" value="${facilityId!}"/>
            </form>
            <form name="incPkgSeq" method="post" action="<@ofbizUrl>SetNextPackageSeq</@ofbizUrl>">
              <input type="hidden" name="orderId" value="${orderId!}"/>
              <input type="hidden" name="shipGroupSeqId" value="${shipGroupSeqId!}"/>
              <input type="hidden" name="facilityId" value="${facilityId!}"/>
            </form>
            <form name="clearPackLineForm" method="post" action="<@ofbizUrl>ClearPackLine</@ofbizUrl>">
                <input type="hidden" name="facilityId"/>
                <input type="hidden" name="orderId"/>
                <input type="hidden" name="orderItemSeqId"/>
                <input type="hidden" name="productId"/>
                <input type="hidden" name="shipGroupSeqId"/>
                <input type="hidden" name="inventoryItemId"/>
                <input type="hidden" name="packageSeqId"/>
            </form>
        </div>
    </div>

    <#if showInput != "N" && ((orderHeader?exists && orderHeader?has_content))>
    <div class="screenlet">
        <div class="screenlet-title-bar">
            <ul>
                <li class="h3">${uiLabelMap.ProductOrderId} ${uiLabelMap.CommonNbr}<a href="/ordermgr/control/orderview?orderId=${orderId}">${orderId}</a> / ${uiLabelMap.ProductOrderShipGroupId} #${shipGroupSeqId}</li>
            </ul>
            <br class="clear"/>
        </div>
        <div class="screenlet-body">
              <#if orderItemShipGroup?has_content>
                <#if (orderItemShipGroup.contactMechId)?has_content>
                  <#assign postalAddress = orderItemShipGroup.getRelatedOne("PostalAddress", false)>
                </#if>
                <#assign carrier = orderItemShipGroup.carrierPartyId?default("N/A")>
                <table cellpadding="4" cellspacing="4" class="basic-table">
                  <tr>
                    <td valign="top">
                      <#if postalAddress?exists >
                        <span class="label">${uiLabelMap.ProductShipToAddress}</span>
                        <br />
                        ${uiLabelMap.CommonTo}: ${postalAddress.toName?default("")}
                        <br />
                        <#if postalAddress.attnName?has_content>
                            ${uiLabelMap.CommonAttn}: ${postalAddress.attnName}
                            <br />
                        </#if>
                        ${postalAddress.address1}
                        <br />
                        <#if postalAddress.address2?has_content>
                            ${postalAddress.address2}
                            <br />
                        </#if>
                        ${postalAddress.city!}, ${postalAddress.stateProvinceGeoId!} ${postalAddress.postalCode!}
                        <br />
                        ${postalAddress.countryGeoId!}
                        <br />
                      </#if>
                    </td>
                    <td>&nbsp;</td>
                    <td valign="top">
                      <span class="label">${uiLabelMap.ProductCarrierShipmentMethod}</span>
                      <br />
                      <#if carrier == "USPS">
                        <#assign color = "red">
                      <#elseif carrier == "UPS">
                        <#assign color = "green">
                      <#else>
                        <#assign color = "black">
                      </#if>
                      <#if carrier != "_NA_">
                        <font color="${color}">${carrier}</font>
                        &nbsp;
                      </#if>
                      <#assign description = (delegator.findOne("ShipmentMethodType", {"shipmentMethodTypeId":orderItemShipGroup.shipmentMethodTypeId}, false)).description>
                      ${description!"??"}
                      <br />
                      <span class="label">${uiLabelMap.ProductEstimatedShipCostForShipGroup}</span>
                      <br />
                      <#if shipmentCostEstimateForShipGroup?exists>
                          <@ofbizCurrency amount=shipmentCostEstimateForShipGroup isoCode=orderReadHelper.getCurrency()!/>
                          <br />
                      </#if>
                    </td>
                    <td>&nbsp;</td>
                    <td valign="top">
                      <span class="label">${uiLabelMap.OrderInstructions}</span>
                      <br />
                      ${orderItemShipGroup.shippingInstructions?default("(${uiLabelMap.CommonNone})")}
                    </td>
                  </tr>
                </table>
              </#if>

              <!-- manual per item form -->
              <#if showInput != "N">
                <hr />
                <form name="singlePackForm" method="post" action="<@ofbizUrl>ProcessPackOrder</@ofbizUrl>">
                  <input type="hidden" name="packageSeq" value="${packingSession.getCurrentPackageSeq()}"/>
                  <input type="hidden" name="orderId" value="${orderId}"/>
                  <input type="hidden" name="shipGroupSeqId" value="${shipGroupSeqId}"/>
                  <input type="hidden" name="facilityId" value="${facilityId!}"/>
                  <input type="hidden" name="hideGrid" value="${hideGrid}"/>
                  <table cellpadding="2" cellspacing="0" class="basic-table">
                    <tr>
                      <td>
                        <div>
                            <span class="label">${uiLabelMap.ProductProductNumber}</span>
                            <input type="text" name="productId" size="20" maxlength="20" value=""/>
                            @
                            <input type="text" name="quantity" size="6" maxlength="6" value="1"/>
                            <a href="javascript:document.singlePackForm.submit();" class="buttontext">${uiLabelMap.ProductPackItem}</a>
                        </div>
                      </td>
                      <td>
                          <span class="label">${uiLabelMap.ProductCurrentPackageSequence}</span>
                          ${packingSession.getCurrentPackageSeq()}
                          <input type="button" value="${uiLabelMap.ProductNextPackage}" onclick="javascript:document.incPkgSeq.submit();" />
                      </td>
                    </tr>
                  </table>
                </form>
              </#if>

              <!-- auto grid form -->
              <#assign itemInfos = packingSession.getItemInfos()!>
              <#if showInput != "N" && hideGrid != "Y" && itemInfos?has_content>
                <br />
                <form name="multiPackForm" method="post" action="<@ofbizUrl>ProcessBulkPackOrder</@ofbizUrl>">
                  <input type="hidden" name="facilityId" value="${facilityId!}" />
                  <input type="hidden" name="orderId" value="${orderId!}" />
                  <input type="hidden" name="shipGroupSeqId" value="${shipGroupSeqId!}" />
                  <input type="hidden" name="originFacilityId" value="${facilityId!}" />
                  <input type="hidden" name="hideGrid" value="${hideGrid}"/>

                  <table class="basic-table" cellspacing='0'>
                    <tr class="header-row">
                      <td>&nbsp;</td>
                      <td>${uiLabelMap.ProductItem} ${uiLabelMap.CommonNbr}</td>
                      <td>${uiLabelMap.ProductProductId}</td>
                      <td>${uiLabelMap.ProductInternalName}</td>
                      <td align="right">${uiLabelMap.ProductOrderedQuantity}</td>
                      <td align="right">${uiLabelMap.ProductQuantityShipped}</td>
                      <td align="right">${uiLabelMap.ProductPackedQty}</td>
                      <td>&nbsp;</td>
                      <td align="center">${uiLabelMap.ProductPackQty}</td>
                      <td align="center">${uiLabelMap.ProductPackedWeight}&nbsp;(${("uiLabelMap.ProductShipmentUomAbbreviation_" + defaultWeightUomId)?eval})</td>
                      <td align="center">${uiLabelMap.ProductPackage}</td>
                      <td align="right">&nbsp;<b>*</b>&nbsp;${uiLabelMap.ProductPackages}</td>
                    </tr>

                    <#if (itemInfos?has_content)>
                      <#assign rowKey = 1>
                      <#list itemInfos as itemInfo>
                        <#assign orderItem = itemInfo.orderItem/>
                        <#assign shippedQuantity = orderReadHelper.getItemShippedQuantity(orderItem)!>
                        <#assign orderItemQuantity = itemInfo.quantity/>
                        <#assign orderProduct = orderItem.getRelatedOne("Product", false)!/>
                        <#assign product = Static["org.apache.ofbiz.product.product.ProductWorker"].findProduct(delegator, itemInfo.productId)!/>
                        <#assign inputQty = orderItemQuantity - packingSession.getPackedQuantity(orderId, orderItem.orderItemSeqId, shipGroupSeqId, itemInfo.productId)>
                        <tr>
                          <td><input type="checkbox" name="sel_${rowKey}" value="Y" <#if (inputQty >0)>checked=""</#if>/></td>
                          <td>${orderItem.orderItemSeqId}</td>
                          <td>
                              ${orderProduct.productId?default("N/A")}
                              <#if orderProduct.productId != product.productId>
                                  &nbsp;${product.productId?default("N/A")}
                              </#if>
                          </td>
                          <td>
                              <a href="/catalog/control/EditProduct?productId=${orderProduct.productId!}${StringUtil.wrapString(externalKeyParam)}" class="buttontext" target="_blank">${(orderProduct.internalName)!}</a>
                              <#if orderProduct.productId != product.productId>
                                  &nbsp;[<a href="/catalog/control/EditProduct?productId=${product.productId!}${StringUtil.wrapString(externalKeyParam)}" class="buttontext" target="_blank">${(product.internalName)!}</a>]
                              </#if>
                          </td>
                          <td align="right">${orderItemQuantity}</td>
                          <td align="right">${shippedQuantity?default(0)}</td>
                          <td align="right">${packingSession.getPackedQuantity(orderId, orderItem.orderItemSeqId, shipGroupSeqId, itemInfo.productId)}</td>
                          <td>&nbsp;</td>
                          <td align="center">
                            <input type="text" size="7" name="qty_${rowKey}" value="${inputQty}" />
                          </td>
                          <td align="center">
                            <input type="text" size="7" name="wgt_${rowKey}" value="" />
                          </td>
                          <td align="center">
                            <select name="pkg_${rowKey}">
                              <#if packingSession.getPackageSeqIds()?exists>
                                <#list packingSession.getPackageSeqIds() as packageSeqId>
                                  <option value="${packageSeqId}">${uiLabelMap.ProductPackage} ${packageSeqId}</option>
                                </#list>
                                <#assign nextPackageSeqId = packingSession.getPackageSeqIds().size() + 1>
                                <option value="${nextPackageSeqId}">${uiLabelMap.ProductNextPackage}</option>
                              <#else>
                                <option value="1">${uiLabelMap.ProductPackage} 1</option>
                                <option value="2">${uiLabelMap.ProductPackage} 2</option>
                                <option value="3">${uiLabelMap.ProductPackage} 3</option>
                                <option value="4">${uiLabelMap.ProductPackage} 4</option>
                                <option value="5">${uiLabelMap.ProductPackage} 5</option>
                              </#if>
                            </select>
                          </td>
                          <td align="right">
                            <input type="text" size="7" name="numPackages_${rowKey}" value="1" />
                          </td>
                          <input type="hidden" name="prd_${rowKey}" value="${itemInfo.productId!}"/>
                          <input type="hidden" name="ite_${rowKey}" value="${orderItem.orderItemSeqId}"/>
                        </tr>
                        <#assign rowKey = rowKey + 1>
                      </#list>
                    </#if>
                    <tr><td colspan="10">&nbsp;</td></tr>
                    <tr>
                      <td colspan="12" align="right">
                        <input type="submit" value="${uiLabelMap.ProductPackItem}" />
                        &nbsp;
                        <input type="button" value="${uiLabelMap.CommonClear} (${uiLabelMap.CommonAll})" onclick="javascript:document.clearPackForm.submit();"/>
                      </td>
                    </tr>
                  </table>
                </form>
                <br />
              </#if>

              <!-- complete form -->
              <#if showInput != "N">
                <form name="completePackForm" method="post" action="<@ofbizUrl>CompletePack</@ofbizUrl>">
                  <input type="hidden" name="orderId" value="${orderId!}"/>
                  <input type="hidden" name="shipGroupSeqId" value="${shipGroupSeqId!}"/>
                  <input type="hidden" name="facilityId" value="${facilityId!}"/>
                  <input type="hidden" name="forceComplete" value="${forceComplete?default('false')}"/>
                  <input type="hidden" name="weightUomId" value="${defaultWeightUomId}"/>
                  <input type="hidden" name="showInput" value="N"/>
                  <hr/>
                  <table class="basic-table" cellpadding="2" cellspacing='0'>
                    <tr>
                        <#assign packageSeqIds = packingSession.getPackageSeqIds()/>
                        <#if packageSeqIds?has_content>
                            <td>
                                <span class="label">${uiLabelMap.ProductPackedWeight} (${("uiLabelMap.ProductShipmentUomAbbreviation_" + defaultWeightUomId)?eval}):</span>
                                <br />
                                <#list packageSeqIds as packageSeqId>
                                    ${uiLabelMap.ProductPackage} ${packageSeqId}
                                    <input type="text" size="7" name="packageWeight_${packageSeqId}" value="${packingSession.getPackageWeight(packageSeqId?int)!}" />
                                    <br />
                                </#list>
                                <#if orderItemShipGroup?has_content>
                                    <input type="hidden" name="shippingContactMechId" value="${orderItemShipGroup.contactMechId!}"/>
                                    <input type="hidden" name="shipmentMethodTypeId" value="${orderItemShipGroup.shipmentMethodTypeId!}"/>
                                    <input type="hidden" name="carrierPartyId" value="${orderItemShipGroup.carrierPartyId!}"/>
                                    <input type="hidden" name="carrierRoleTypeId" value="${orderItemShipGroup.carrierRoleTypeId!}"/>
                                    <input type="hidden" name="productStoreId" value="${productStoreId!}"/>
                                </#if>
                            </td>
                            <#if carrierShipmentBoxTypes?has_content>
                              <td>
                                <span class="label">${uiLabelMap.ProductShipmentBoxType}</span>
                                <br/>
                                <#list packageSeqIds as packageSeqId>
                                  <select name="boxType_${packageSeqId}">
                                    <option value=""></option>
                                    <#list carrierShipmentBoxTypes as carrierShipmentBoxType>
                                      <#assign shipmentBoxType = carrierShipmentBoxType.getRelatedOne("ShipmentBoxType", false) />
                                      <option value="${shipmentBoxType.shipmentBoxTypeId}">${shipmentBoxType.description?default(shipmentBoxType.shipmentBoxTypeId)}</option>
                                    </#list>
                                  </select>
                                  <br/>
                                </#list>
                              </td>
                            </#if>
                        </#if>
                        <td nowrap="nowrap">
                            <span class="label">${uiLabelMap.ProductAdditionalShippingCharge}:</span>
                            <br />
                            <input type="text" name="additionalShippingCharge" value="${packingSession.getAdditionalShippingCharge()!}" size="20"/>
                            <#if packageSeqIds?has_content>
                                <a href="javascript:document.completePackForm.action='<@ofbizUrl>calcPackSessionAdditionalShippingCharge</@ofbizUrl>';document.completePackForm.submit();" class="buttontext">${uiLabelMap.ProductEstimateShipCost}</a>
                                <br />
                            </#if>
                        </td>
                      <td>
                        <span class="label">${uiLabelMap.ProductHandlingInstructions}:</span>
                        <br />
                        <textarea name="handlingInstructions" rows="2" cols="30">${packingSession.getHandlingInstructions()!}</textarea>
                      </td>
                      <td align="right">
                        <div>
                          <#assign buttonName = "${uiLabelMap.ProductComplete}">
                          <#if forceComplete?default("false") == "true">
                            <#assign buttonName = "${uiLabelMap.ProductCompleteForce}">
                          </#if>
                          <input type="button" value="${buttonName}" onclick="javascript:document.completePackForm.submit();"/>
                        </div>
                      </td>
                    </tr>
                  </table>
                  <br />
                </form>
              </#if>
        </div>
    </div>

    <!-- display items in packages, per packed package and in order -->
    <#assign linesByPackageResultMap = packingSession.getPackingSessionLinesByPackage()!>
    <#assign packageMap = linesByPackageResultMap.get("packageMap")!>
    <#assign sortedKeys = linesByPackageResultMap.get("sortedKeys")!>
    <#if ((packageMap?has_content) && (sortedKeys?has_content))>
      <div class="screenlet">
        <div class="screenlet-title-bar">
            <ul>
                <li class="h3">${uiLabelMap.ProductPackages} : ${sortedKeys.size()!}</li>
            </ul>
            <br class="clear"/>
        </div>
          <div class="screenlet-body">
            <#list sortedKeys as key>
              <#assign packedLines = packageMap.get(key)>
              <#if packedLines?has_content>
                <br />
                <#assign packedLine = packedLines.get(0)!>
                <span class="label" style="font-size:1.2em">${uiLabelMap.ProductPackage}&nbsp;${packedLine.getPackageSeq()!}</span>
                <br />
                <table class="basic-table" cellspacing='0'>
                  <tr class="header-row">
                    <td>${uiLabelMap.ProductItem} ${uiLabelMap.CommonNbr}</td>
                    <td>${uiLabelMap.ProductProductId}</td>
                    <td>${uiLabelMap.ProductProductDescription}</td>
                    <td>${uiLabelMap.ProductInventoryItem} ${uiLabelMap.CommonNbr}</td>
                    <td align="right">${uiLabelMap.ProductPackedQty}</td>
                    <td align="right">${uiLabelMap.ProductPackedWeight}&nbsp;(${("uiLabelMap.ProductShipmentUomAbbreviation_" + defaultWeightUomId)?eval})&nbsp;(${uiLabelMap.ProductPackage})</td>
                    <td align="right">${uiLabelMap.ProductPackage} ${uiLabelMap.CommonNbr}</td>
                    <td>&nbsp;</td>
                  </tr>
                  <#list packedLines as line>
                    <#assign product = Static["org.apache.ofbiz.product.product.ProductWorker"].findProduct(delegator, line.getProductId())/>
                    <tr>
                      <td>${line.getOrderItemSeqId()}</td>
                      <td>${line.getProductId()?default("N/A")}</td>
                      <td>
                          <a href="/catalog/control/EditProduct?productId=${line.getProductId()!}${StringUtil.wrapString(externalKeyParam)}" class="buttontext" target="_blank">${product.internalName!?default("[N/A]")}</a>
                      </td>
                      <td>${line.getInventoryItemId()}</td>
                      <td align="right">${line.getQuantity()}</td>
                      <td align="right">${line.getWeight()} (${packingSession.getPackageWeight(line.getPackageSeq()?int)!})</td>
                      <td align="right">${line.getPackageSeq()}</td>
                      <td align="right"><a href="javascript:clearLine('${facilityId}', '${line.getOrderId()}', '${line.getOrderItemSeqId()}', '${line.getProductId()?default("")}', '${line.getShipGroupSeqId()}', '${line.getInventoryItemId()}', '${line.getPackageSeq()}')" class="buttontext">${uiLabelMap.CommonClear}</a></td>
                    </tr>
                  </#list>
                </table>
              </#if>
            </#list>
          </div>
      </div>
    </#if>

    <!-- packed items display -->
    <#assign packedLines = packingSession.getLines()!>
    <#if packedLines?has_content>
      <div class="screenlet">
          <div class="screenlet-title-bar">
              <ul>
                  <li class="h3">${uiLabelMap.ProductItems} (${uiLabelMap.ProductPackages}): ${packedLines.size()!}</li>
              </ul>
              <br class="clear"/>
          </div>
          <div class="screenlet-body">
            <table class="basic-table" cellspacing='0'>
              <tr class="header-row">
                  <td>${uiLabelMap.ProductItem} ${uiLabelMap.CommonNbr}</td>
                  <td>${uiLabelMap.ProductProductId}</td>
                  <td>${uiLabelMap.ProductProductDescription}</td>
                  <td>${uiLabelMap.ProductInventoryItem} ${uiLabelMap.CommonNbr}</td>
                  <td align="right">${uiLabelMap.ProductPackedQty}</td>
                  <td align="right">${uiLabelMap.ProductPackedWeight}&nbsp;(${("uiLabelMap.ProductShipmentUomAbbreviation_" + defaultWeightUomId)?eval})&nbsp;(${uiLabelMap.ProductPackage})</td>
                  <td align="right">${uiLabelMap.ProductPackage} ${uiLabelMap.CommonNbr}</td>
                  <td>&nbsp;</td>
              </tr>
              <#list packedLines as line>
                  <#assign product = Static["org.apache.ofbiz.product.product.ProductWorker"].findProduct(delegator, line.getProductId())/>
                  <tr>
                      <td>${line.getOrderItemSeqId()}</td>
                      <td>${line.getProductId()?default("N/A")}</td>
                      <td>
                          <a href="/catalog/control/EditProduct?productId=${line.getProductId()!}${StringUtil.wrapString(externalKeyParam)}" class="buttontext" target="_blank">${product.internalName!?default("[N/A]")}</a>
                      </td>
                      <td>${line.getInventoryItemId()}</td>
                      <td align="right">${line.getQuantity()}</td>
                      <td align="right">${line.getWeight()} (${packingSession.getPackageWeight(line.getPackageSeq()?int)!})</td>
                      <td align="right">${line.getPackageSeq()}</td>
                      <td align="right"><a href="javascript:clearLine('${facilityId}', '${line.getOrderId()}', '${line.getOrderItemSeqId()}', '${line.getProductId()?default("")}', '${line.getShipGroupSeqId()}', '${line.getInventoryItemId()}', '${line.getPackageSeq()}')" class="buttontext">${uiLabelMap.CommonClear}</a></td>
                  </tr>
              </#list>
            </table>
          </div>
      </div>
    </#if>
  </#if>

  <#if orderId?has_content>
    <script language="javascript" type="text/javascript">
      document.singlePackForm.productId.focus();
    </script>
  <#else>
    <script language="javascript" type="text/javascript">
      document.selectOrderForm.orderId.focus();
    </script>
  </#if>
<#else>
  <h3>${uiLabelMap.ProductFacilityViewPermissionError}</h3>
</#if>
