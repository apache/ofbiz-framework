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
<script language="JavaScript">
    function setNow(field) { eval('document.selectAllForm.' + field + '.value="${nowTimestamp}"'); }
</script>
<h1>${title}</h1>
        <#if invalidProductId?exists>
            <div class="errorMessage">${invalidProductId}</div>
        </#if>
        <div class="button-bar">
          <a href="<@ofbizUrl>EditFacility</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductNewFacility}</a>
        </div>
        <#-- Receiving Results -->
        <#if receivedItems?has_content>
          <h3>${uiLabelMap.ProductReceiptPurchaseOrder} ${purchaseOrder.orderId}</h3>
          <hr/>
          <table class="basic-table" cellspacing="0">
            <tr class="header-row">
              <td>${uiLabelMap.ProductShipmentId}</td>
              <td>${uiLabelMap.ProductReceipt}</td>
              <td>${uiLabelMap.CommonDate}</td>
              <td>${uiLabelMap.ProductPo}</td>
              <td>${uiLabelMap.ProductLine}</td>
              <td>${uiLabelMap.ProductProductId}</td>
              <td>${uiLabelMap.ProductPerUnitPrice}</td>
              <td>${uiLabelMap.CommonRejected}</td>
              <td>${uiLabelMap.CommonAccepted}</td>
              <td></td>
            </tr>
            <#list receivedItems as item>
              <form name="cancelReceivedItemsForm_${item_index}" method="post" action="<@ofbizUrl>cancelReceivedItems</@ofbizUrl>">
                <input type="hidden" name="receiptId" value ="${(item.receiptId)?if_exists}"/>
                <input type="hidden" name="purchaseOrderId" value ="${(item.orderId)?if_exists}"/>
                <input type="hidden" name="facilityId" value ="${facilityId?if_exists}"/>
                <tr>
                  <td><a href="<@ofbizUrl>ViewShipment?shipmentId=${item.shipmentId?if_exists}</@ofbizUrl>" class="buttontext">${item.shipmentId?if_exists} ${item.shipmentItemSeqId?if_exists}</a></td>
                  <td>${item.receiptId}</td>
                  <td>${item.getString("datetimeReceived").toString()}</td>
                  <td><a href="/ordermgr/control/orderview?orderId=${item.orderId}" class="buttontext">${item.orderId}</a></td>
                  <td>${item.orderItemSeqId}</td>
                  <td>${item.productId?default("Not Found")}</td>
                  <td>${item.unitCost?default(0)?string("##0.00")}</td>
                  <td>${item.quantityRejected?default(0)?string.number}</td>
                  <td>${item.quantityAccepted?string.number}</td>
                  <td>
                    <#if (item.quantityAccepted?int > 0 || item.quantityRejected?int > 0)>
                      <a href="javascript:document.cancelReceivedItemsForm_${item_index}.submit();" class="buttontext">${uiLabelMap.CommonCancel}</a>
                    </#if>
                  </td>
                </tr>
              </form>
            </#list>
            <tr><td colspan="10"><hr/></td></tr>
          </table>
          <br/>
        </#if>

        <#-- Single Product Receiving -->
        <#if requestParameters.initialSelected?exists && product?has_content>
          <form method="post" action="<@ofbizUrl>receiveSingleInventoryProduct</@ofbizUrl>" name="selectAllForm">
            <table class="basic-table" cellspacing="0">
              <#-- general request fields -->
              <input type="hidden" name="facilityId" value="${requestParameters.facilityId?if_exists}"/>
              <input type="hidden" name="purchaseOrderId" value="${requestParameters.purchaseOrderId?if_exists}"/>
              <#-- special service fields -->
              <input type="hidden" name="productId" value="${requestParameters.productId?if_exists}"/>
              <#if purchaseOrder?has_content>
              <#assign unitCost = firstOrderItem.unitPrice?default(standardCosts.get(firstOrderItem.productId)?default(0))/>
              <input type="hidden" name="orderId" value="${purchaseOrder.orderId}"/>
              <input type="hidden" name="orderItemSeqId" value="${firstOrderItem.orderItemSeqId}"/>
              <tr>
                <td width="14%">&nbsp;</td>
                <td width="6%" align="right" nowrap class="label">${uiLabelMap.ProductPurchaseOrder}</td>
                <td width="6%">&nbsp;</td>
                <td width="74%">
                  <b>${purchaseOrder.orderId}</b>&nbsp;/&nbsp;<b>${firstOrderItem.orderItemSeqId}</b>
                  <#if 1 < purchaseOrderItemsSize>
                    (${uiLabelMap.ProductMultipleOrderItemsProduct} - ${purchaseOrderItemsSize}:1 ${uiLabelMap.ProductItemProduct})
                  <#else>
                    (${uiLabelMap.ProductSingleOrderItemProduct} - 1:1 ${uiLabelMap.ProductItemProduct})
                  </#if>
                </td>
              </tr>
              </#if>
              <tr>
                <td width="14%">&nbsp;</td>
                <td width="6%" align="right" nowrap class="label">${uiLabelMap.ProductProductId}</td>
                <td width="6%">&nbsp;</td>
                <td width="74%">
                  <b>${requestParameters.productId?if_exists}</b>
                </td>
              </tr>
              <tr>
                <td width="14%">&nbsp;</td>
                <td width="6%" align="right" nowrap class="label">${uiLabelMap.ProductProductName}</td>
                <td width="6%">&nbsp;</td>
                <td width="74%">
                  <a href="/catalog/control/EditProduct?productId=${product.productId}${externalKeyParam?if_exists}" target="catalog" class="buttontext">${product.internalName?if_exists}</a>
                </td>
              </tr>
              <tr>
                <td width="14%">&nbsp;</td>
                <td width="6%" align="right" nowrap class="label">${uiLabelMap.ProductProductDescription}</td>
                <td width="6%">&nbsp;</td>
                <td width="74%">
                  ${product.description?if_exists}
                </td>
              </tr>
              <tr>
                <td width="14%">&nbsp;</td>
                <td width="6%" align="right" nowrap class="label">${uiLabelMap.ProductItemDescription}</td>
                <td width="6%">&nbsp;</td>
                <td width="74%">
                  <input type="text" name="itemDescription" size="30" maxlength="60"/>
                </td>
              </tr>
              <tr>
                <td width="14%">&nbsp;</td>
                <td width="6%" align="right" nowrap class="label">${uiLabelMap.ProductInventoryItemType}</td>
                <td width="6%">&nbsp;</td>
                <td width="74%">
                  <select name="inventoryItemTypeId" size="1">
                    <#list inventoryItemTypes as nextInventoryItemType>
                      <option value="${nextInventoryItemType.inventoryItemTypeId}"
                        <#if (facility.defaultInventoryItemTypeId?has_content) && (nextInventoryItemType.inventoryItemTypeId == facility.defaultInventoryItemTypeId)>
                          selected="selected"
                        </#if>
                      >${nextInventoryItemType.get("description",locale)?default(nextInventoryItemType.inventoryItemTypeId)}</option>
                    </#list>
                  </select>
                </td>
              </tr>
              <tr>
                <td colspan="4">&nbsp;</td>
              </tr>
              <tr>
                <td width="14%">&nbsp;</td>
                <td width="6%" align="right" nowrap class="label">${uiLabelMap.ProductFacilityOwner}</td>
                <td width="6%">&nbsp;</td>
                <td width="74%">
                    <input type="text" name="ownerPartyId" value="" size="20" maxlength="20">
                    <a href="javascript:call_fieldlookup2(document.selectAllForm.ownerPartyId, 'LookupPartyName');"><img src="<@ofbizContentUrl>/images/fieldlookup.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="${uiLabelMap.CommonClickHereForFieldLookup}"/></a>
                </td>
              </tr>
              <tr>
                <td width="14%">&nbsp;</td>
                <td width="6%" align="right" nowrap class="label">${uiLabelMap.ProductDateReceived}</td>
                <td width="6%">&nbsp;</td>
                <td width="74%">
                  <input type="text" name="datetimeReceived" size="24" value="${nowTimestamp}">
                  <#-- <a href="#" onclick="setNow("datetimeReceived")" class="buttontext">[Now]</a> -->
                </td>
              </tr>

              <#-- facility location(s) -->
              <#assign facilityLocations = (product.getRelatedByAnd("ProductFacilityLocation", Static["org.ofbiz.base.util.UtilMisc"].toMap("facilityId", facilityId)))?if_exists/>
              <tr>
                <td width="14%">&nbsp;</td>
                <td width="6%" align="right" nowrap class="label">${uiLabelMap.ProductFacilityLocation}</td>
                <td width="6%">&nbsp;</td>
                <td width="74%">
                  <#if facilityLocations?has_content>
                    <select name="locationSeqId">
                      <#list facilityLocations as productFacilityLocation>
                        <#assign facility = productFacilityLocation.getRelatedOneCache("Facility")/>
                        <#assign facilityLocation = productFacilityLocation.getRelatedOne("FacilityLocation")?if_exists/>
                        <#assign facilityLocationTypeEnum = (facilityLocation.getRelatedOneCache("TypeEnumeration"))?if_exists/>
                        <option value="${productFacilityLocation.locationSeqId}"><#if facilityLocation?exists>${facilityLocation.areaId?if_exists}:${facilityLocation.aisleId?if_exists}:${facilityLocation.sectionId?if_exists}:${facilityLocation.levelId?if_exists}:${facilityLocation.positionId?if_exists}</#if><#if facilityLocationTypeEnum?exists>(${facilityLocationTypeEnum.get("description",locale)})</#if>[${productFacilityLocation.locationSeqId}]</option>
                      </#list>
                      <option value="">${uiLabelMap.ProductNoLocation}</option>
                    </select>
                  <#else>
                    <input type="text" name="locationSeqId" size="20" maxlength="20"/>
                        <a href="javascript:call_fieldlookup2(document.selectAllForm.locationSeqId,'LookupFacilityLocation<#if parameters.facilityId?exists>?facilityId=${facilityId}</#if>');">
                            <img src="<@ofbizContentUrl>/images/fieldlookup.gif</@ofbizContentUrl>" width="15" height="14" border="0" alt="${uiLabelMap.CommonClickHereForFieldLookup}"/>
                        </a>
                  </#if>
                </td>
              </tr>
              <tr>
                <td width="14%">&nbsp;</td>
                <td width="6%" align="right" nowrap class="label">${uiLabelMap.ProductRejectedReason}</td>
                <td width="6%">&nbsp;</td>
                <td width="74%">
                  <select name="rejectionId" size="1">
                    <option></option>
                    <#list rejectReasons as nextRejection>
                      <option value="${nextRejection.rejectionId}">${nextRejection.get("description",locale)?default(nextRejection.rejectionId)}</option>
                    </#list>
                  </select>
                </td>
              </tr>
              <tr>
                <td width="14%">&nbsp;</td>
                <td width="6%" align="right" nowrap class="label">${uiLabelMap.ProductQuantityRejected}</td>
                <td width="6%">&nbsp;</td>
                <td width="74%">
                  <input type="text" name="quantityRejected" size="5" value="0" />
                </td>
              </tr>
              <tr>
                <td width="14%">&nbsp;</td>
                <td width="6%" align="right" nowrap class="label">${uiLabelMap.ProductQuantityAccepted}</td>
                <td width="6%">&nbsp;</td>
                <td width="74%">
                  <input type="text" name="quantityAccepted" size="5" value="${defaultQuantity?default(1)?string.number}"/>
                </td>
              </tr>
              <tr>
                <td width="14%">&nbsp;</td>
                <td width="6%" align="right" nowrap class="label">${uiLabelMap.ProductPerUnitPrice}</td>
                <td width="6%">&nbsp;</td>
                <td width="74%">
                  <#-- get the default unit cost -->
                  <#if (!unitCost?exists || unitCost == 0.0)><#assign unitCost = standardCosts.get(product.productId)?default(0)/></#if>
                  <input type="text" name="unitCost" size="10" value="${unitCost}"/>
                </td>
              </tr>
              <tr>
                <td colspan="2">&nbsp;</td>
                <td colspan="2"><input type="submit" value="${uiLabelMap.CommonReceive}"></td>
              </tr>
            </table>
            <script language="JavaScript">
              document.selectAllForm.quantityAccepted.focus();
            </script>
          </form>

        <#-- Select Shipment Screen -->
        <#elseif requestParameters.initialSelected?exists && !requestParameters.shipmentId?exists>
          <h3>${uiLabelMap.ProductSelectShipmentReceive}</h3>
          <form method="post" action="<@ofbizUrl>ReceiveInventory</@ofbizUrl>" name="selectAllForm">
            <#-- general request fields -->
            <input type="hidden" name="facilityId" value="${requestParameters.facilityId?if_exists}"/>
            <input type="hidden" name="purchaseOrderId" value="${requestParameters.purchaseOrderId?if_exists}"/>
            <input type="hidden" name="initialSelected" value="Y"/>
            <input type="hidden" name="partialReceive" value="${partialReceive?if_exists}"/>
            <table class="basic-table" cellspacing="0">
              <#list shipments?if_exists as shipment>
                <#assign originFacility = shipment.getRelatedOneCache("OriginFacility")?if_exists/>
                <#assign destinationFacility = shipment.getRelatedOneCache("DestinationFacility")?if_exists/>
                <#assign statusItem = shipment.getRelatedOneCache("StatusItem")/>
                <#assign shipmentType = shipment.getRelatedOneCache("ShipmentType")/>
                <#assign shipmentDate = shipment.estimatedArrivalDate?if_exists/>
                <tr>
                  <td><hr/></td>
                </tr>
                <tr>
                  <td>
                    <table class="basic-table" cellspacing="0">
                      <tr>
                        <td width="5%" nowrap><input type="radio" name="shipmentId" value="${shipment.shipmentId}"></td>
                        <td width="5%" nowrap>${shipment.shipmentId}</td>
                        <td>${shipmentType.get("description",locale)?default(shipmentType.shipmentTypeId?default(""))}</td>
                        <td>${statusItem.get("description",locale)?default(statusItem.statusId?default("N/A"))}</td>
                        <td>${(originFacility.facilityName)?if_exists} [${shipment.originFacilityId?if_exists}]</td>
                        <td>${(destinationFacility.facilityName)?if_exists} [${shipment.destinationFacilityId?if_exists}]</td>
                        <td style="white-space: nowrap;">${(shipment.estimatedArrivalDate.toString())?if_exists}</td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </#list>
              <tr>
                <td><hr/></td>
              </tr>
              <tr>
                <td>
                  <table class="basic-table" cellspacing="0">
                    <tr>
                      <td width="5%" nowrap><input type="radio" name="shipmentId" value="_NA_"></td>
                      <td width="5%" nowrap>${uiLabelMap.ProductNoSpecificShipment}</td>
                      <td colspan="5"></td>
                    </tr>
                  </table>
                </td>
              </tr>
              <tr>
                <td>&nbsp;<a href="javascript:document.selectAllForm.submit();" class="buttontext">${uiLabelMap.ProductReceiveSelectedShipment}</a></td>
              </tr>
            </table>
          </form>

        <#-- Multi-Item PO Receiving -->
        <#elseif requestParameters.initialSelected?exists && purchaseOrder?has_content>
          <input type="hidden" id="getConvertedPrice" value="<@ofbizUrl secure="${request.isSecure()?string}">getConvertedPrice"</@ofbizUrl> />
          <input type="hidden" id="alertMessage" value="${uiLabelMap.ProductChangePerUnitPrice}" />
          <form method="post" action="<@ofbizUrl>receiveInventoryProduct</@ofbizUrl>" name="selectAllForm">
            <#-- general request fields -->
            <input type="hidden" name="facilityId" value="${requestParameters.facilityId?if_exists}"/>
            <input type="hidden" name="purchaseOrderId" value="${requestParameters.purchaseOrderId?if_exists}"/>
            <input type="hidden" name="initialSelected" value="Y"/>
            <#if shipment?has_content>
            <input type="hidden" name="shipmentIdReceived" value="${shipment.shipmentId}"/>
            </#if>
            <input type="hidden" name="_useRowSubmit" value="Y"/>
            <#assign rowCount = 0/>
            <table class="basic-table" cellspacing="0">
              <#if !purchaseOrderItems?exists || purchaseOrderItemsSize == 0>
                <tr>
                  <td colspan="2">${uiLabelMap.ProductNoItemsPoReceive}.</td>
                </tr>
              <#else/>
                <tr>
                  <td>
                    <h3>${uiLabelMap.ProductReceivePurchaseOrder} #${purchaseOrder.orderId}</h3>
                    <#if shipment?has_content>
                    <h3>${uiLabelMap.ProductShipmentId} #${shipment.shipmentId}</h3>
                    <span>Set Shipment As Received</span>&nbsp;
                    <input type="checkbox" name="forceShipmentReceived" value="Y"/>
                    </#if>
                  </td>
                  <td align="right">
                    ${uiLabelMap.CommonSelectAll}
                    <input type="checkbox" name="selectAll" value="Y" onclick="javascript:toggleAll(this, 'selectAllForm');"/>
                  </td>
                </tr>
                <#list purchaseOrderItems as orderItem>
                  <#assign defaultQuantity = orderItem.quantity - receivedQuantities[orderItem.orderItemSeqId]?double/>
                  <#assign itemCost = orderItem.unitPrice?default(0)/>
                  <#assign salesOrderItem = salesOrderItems[orderItem.orderItemSeqId]?if_exists/>
                  <#if shipment?has_content>
                    <#if shippedQuantities[orderItem.orderItemSeqId]?exists>
                      <#assign defaultQuantity = shippedQuantities[orderItem.orderItemSeqId]?double - receivedQuantities[orderItem.orderItemSeqId]?double/>
                    <#else>
                      <#assign defaultQuantity = 0/>
                    </#if>
                  </#if>
                  <#if 0 < defaultQuantity>
                  <#assign orderItemType = orderItem.getRelatedOne("OrderItemType")/>
                  <input type="hidden" name="orderId_o_${rowCount}" value="${orderItem.orderId}"/>
                  <input type="hidden" name="orderItemSeqId_o_${rowCount}" value="${orderItem.orderItemSeqId}"/>
                  <input type="hidden" name="facilityId_o_${rowCount}" value="${requestParameters.facilityId?if_exists}"/>
                  <input type="hidden" name="datetimeReceived_o_${rowCount}" value="${nowTimestamp}"/>
                  <#if shipment?exists && shipment.shipmentId?has_content>
                    <input type="hidden" name="shipmentId_o_${rowCount}" value="${shipment.shipmentId}"/>
                  </#if>
                  <#if salesOrderItem?has_content>
                    <input type="hidden" name="priorityOrderId_o_${rowCount}" value="${salesOrderItem.orderId}"/>
                    <input type="hidden" name="priorityOrderItemSeqId_o_${rowCount}" value="${salesOrderItem.orderItemSeqId}"/>
                  </#if>

                  <tr>
                    <td colspan="2"><hr/></td>
                  </tr>
                  <tr>
                    <td>
                      <table class="basic-table" cellspacing="0">
                        <tr>
                          <#if orderItem.productId?exists>
                            <#assign product = orderItem.getRelatedOneCache("Product")/>
                            <input type="hidden" name="productId_o_${rowCount}" value="${product.productId}"/>
                            <td width="45%">
                                ${orderItem.orderItemSeqId}:&nbsp;<a href="/catalog/control/EditProduct?productId=${product.productId}${externalKeyParam?if_exists}" target="catalog" class="buttontext">${product.productId}&nbsp;-&nbsp;${orderItem.itemDescription?if_exists}</a> : ${product.description?if_exists}
                            </td>
                          <#else>
                            <td width="45%">
                                <b>${orderItemType.get("description",locale)}</b> : ${orderItem.itemDescription?if_exists}&nbsp;&nbsp;
                                <input type="text" size="12" name="productId_o_${rowCount}"/>
                                <a href="/catalog/control/EditProduct?externalLoginKey=${externalLoginKey}" target="catalog" class="buttontext">${uiLabelMap.ProductCreateProduct}</a>
                            </td>
                          </#if>
                          <td align="right">${uiLabelMap.ProductLocation}:</td>
                          <#-- location(s) -->
                          <td align="right">
                            <#assign facilityLocations = (orderItem.getRelatedByAnd("ProductFacilityLocation", Static["org.ofbiz.base.util.UtilMisc"].toMap("facilityId", facilityId)))?if_exists/>
                            <#if facilityLocations?has_content>
                              <select name="locationSeqId_o_${rowCount}">
                                <#list facilityLocations as productFacilityLocation>
                                  <#assign facility = productFacilityLocation.getRelatedOneCache("Facility")/>
                                  <#assign facilityLocation = productFacilityLocation.getRelatedOne("FacilityLocation")?if_exists/>
                                  <#assign facilityLocationTypeEnum = (facilityLocation.getRelatedOneCache("TypeEnumeration"))?if_exists/>
                                  <option value="${productFacilityLocation.locationSeqId}"><#if facilityLocation?exists>${facilityLocation.areaId?if_exists}:${facilityLocation.aisleId?if_exists}:${facilityLocation.sectionId?if_exists}:${facilityLocation.levelId?if_exists}:${facilityLocation.positionId?if_exists}</#if><#if facilityLocationTypeEnum?exists>(${facilityLocationTypeEnum.get("description",locale)})</#if>[${productFacilityLocation.locationSeqId}]</option>
                                </#list>
                                <option value="">${uiLabelMap.ProductNoLocation}</option>
                              </select>
                            <#else>
                              <input type="text" name="locationSeqId_o_${rowCount}" size="12"/>
                                  <a href="javascript:call_fieldlookup2(document.selectAllForm.locationSeqId_o_${rowCount},'LookupFacilityLocation<#if parameters.facilityId?exists>?facilityId=${facilityId}</#if>');">
                                      <img src="<@ofbizContentUrl>/images/fieldlookup.gif</@ofbizContentUrl>" width="15" height="14" border="0" alt=${uiLabelMap.CommonClickHereForFieldLookup}/>
                                  </a>
                            </#if>
                          </td>
                          <td align="right">${uiLabelMap.ProductQtyReceived} :</td>
                          <td align="right">
                            <input type="text" name="quantityAccepted_o_${rowCount}" size="6" value=<#if partialReceive?exists>"0"<#else>"${defaultQuantity?string.number}"</#if>/>
                          </td>
                        </tr>
                        <tr>
                          <td width="45%">
                            ${uiLabelMap.ProductInventoryItemType} :&nbsp;
                            <select name="inventoryItemTypeId_o_${rowCount}" size="1">
                              <#list inventoryItemTypes as nextInventoryItemType>
                              <option value="${nextInventoryItemType.inventoryItemTypeId}"
                               <#if (facility.defaultInventoryItemTypeId?has_content) && (nextInventoryItemType.inventoryItemTypeId == facility.defaultInventoryItemTypeId)>
                                selected="selected"
                              </#if>
                              >${nextInventoryItemType.get("description",locale)?default(nextInventoryItemType.inventoryItemTypeId)}</option>
                              </#list>
                            </select>
                          </td>
                          <td align="right">${uiLabelMap.ProductRejectionReason} :</td>
                          <td align="right">
                            <select name="rejectionId_o_${rowCount}" size="1">
                              <option></option>
                              <#list rejectReasons as nextRejection>
                              <option value="${nextRejection.rejectionId}">${nextRejection.get("description",locale)?default(nextRejection.rejectionId)}</option>
                              </#list>
                            </select>
                          </td>
                          <td align="right">${uiLabelMap.ProductQtyRejected} :</td>
                          <td align="right">
                            <input type="text" name="quantityRejected_o_${rowCount}" value="0" size="6"/>
                          </td>
                          <tr>
                            <td colspan="4" align="right">${uiLabelMap.OrderQtyOrdered} :</td>
                            <td align="right">
                              <input type="text" class="inputBox" name="quantityOrdered" value="${orderItem.quantity}" size="6" maxlength="20" disabled/>
                            </td>
                          </tr>
                        </tr>
                        <tr>
                          <td>&nbsp;</td>
                          <td align="right">${uiLabelMap.ProductFacilityOwner}:</td>
                          <td align="right"><input type="text" name="ownerPartyId_o_${rowCount}" size="20" maxlength="20" value="${facility.ownerPartyId}"/></td>
                          <#if currencyUomId != orderCurrencyUomId>
                            <td>${uiLabelMap.ProductPerUnitPriceOrder}:</td>
                            <td>
                              <input type="hidden" name="orderCurrencyUomId_o_${rowCount}" value="${orderCurrencyUomId?if_exists}" />
                              <input type="text" id="orderCurrencyUnitPrice_${rowCount}" name="orderCurrencyUnitPrice_o_${rowCount}" value="${orderCurrencyUnitPriceMap[orderItem.orderItemSeqId]}" onchange="javascript:getConvertedPrice(orderCurrencyUnitPrice_${rowCount}, '${orderCurrencyUomId}', '${currencyUomId}', '${rowCount}', '${orderCurrencyUnitPriceMap[orderItem.orderItemSeqId]}', '${itemCost}');" size="6" maxlength="20" />
                              ${orderCurrencyUomId?if_exists}
                            </td>
                            <td>${uiLabelMap.ProductPerUnitPriceFacility}:</td>
                            <td>
                              <input type="hidden" name="currencyUomId_o_${rowCount}" value="${currencyUomId?if_exists}" />
                              <input type="text" id="unitCost_${rowCount}" name="unitCost_o_${rowCount}" value="${itemCost}" readonly size="6" maxlength="20" />
                              ${currencyUomId?if_exists}
                            </td>
                          <#else>
                            <td>${uiLabelMap.ProductPerUnitPrice}:</td>
                            <td align="right">
                              <input type="hidden" name="currencyUomId_o_${rowCount}" value="${currencyUomId?if_exists}" />
                              <input type="text" name="unitCost_o_${rowCount}" value="${itemCost}" size="6" maxlength="20" />
                              ${currencyUomId?if_exists}
                            </td>
                          </#if>
                        </tr>
                      </table>
                    </td>
                    <td align="right">
                      <input type="checkbox" name="_rowSubmit_o_${rowCount}" value="Y" onclick="javascript:checkToggle(this, 'selectAllForm');"/>
                    </td>
                  </tr>
                  <#assign rowCount = rowCount + 1>
                  </#if>
                </#list>
                <tr>
                  <td colspan="2">
                    <hr/>
                  </td>
                </tr>
                <#if rowCount == 0>
                  <tr>
                    <td colspan="2">${uiLabelMap.ProductNoItemsPo} #${purchaseOrder.orderId} ${uiLabelMap.ProductToReceive}.</td>
                  </tr>
                  <tr>
                    <td colspan="2" align="right">
                      <a href="<@ofbizUrl>ReceiveInventory?facilityId=${requestParameters.facilityId?if_exists}</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductReturnToReceiving}</a>
                    </td>
                  </tr>
                <#else>
                  <tr>
                    <td colspan="2" align="right">
                      <a href="javascript:document.selectAllForm.submit();" class="buttontext">${uiLabelMap.ProductReceiveSelectedProduct}</a>
                    </td>
                  </tr>
                </#if>
              </#if>
            </table>
            <input type="hidden" name="_rowCount" value="${rowCount}"/>
          </form>
          <script language="JavaScript" type="text/javascript">selectAll('selectAllForm');</script>

        <#-- Initial Screen -->
        <#else>
          <h2>${uiLabelMap.ProductReceiveItem}</h2>
          <form name="selectAllForm" method="post" action="<@ofbizUrl>ReceiveInventory</@ofbizUrl>">
            <input type="hidden" name="facilityId" value="${requestParameters.facilityId?if_exists}"/>
            <input type="hidden" name="initialSelected" value="Y"/>
            <table class="basic-table" cellspacing="0">
              <tr>
                <td class="label">${uiLabelMap.ProductPurchaseOrderNumber}</td>
                <td>
                  <input type="text" name="purchaseOrderId" size="20" maxlength="20" value="${requestParameters.purchaseOrderId?if_exists}">
                    <a href="javascript:call_fieldlookup2(document.selectAllForm.purchaseOrderId,'LookupPurchaseOrderHeaderAndShipInfo');">
                      <img src="<@ofbizContentUrl>/images/fieldlookup.gif</@ofbizContentUrl>" width="15" height="14" border="0" alt="${uiLabelMap.CommonClickHereForFieldLookup}"/>
                    </a>
                    <span class="tooltip">${uiLabelMap.ProductLeaveSingleProductReceiving}</span>
                </td>
              </tr>
              <tr>
                <td class="label">${uiLabelMap.ProductProductId}</td>
                <td>
                  <input type="text" name="productId" size="20" maxlength="20" value="${requestParameters.productId?if_exists}"/>
                    <a href="javascript:call_fieldlookup2(document.selectAllForm.productId,'LookupProduct');">
                      <img src="<@ofbizContentUrl>/images/fieldlookup.gif"</@ofbizContentUrl>" width="15" height="14" border="0" alt="${uiLabelMap.CommonClickHereForFieldLookup}"/>
                    </a>
                    <span class="tooltip">${uiLabelMap.ProductLeaveEntirePoReceiving}</span>
                </td>
              </tr>
              <tr>
                <td>&nbsp;</td>
                <td>
                  <a href="javascript:document.selectAllForm.submit();" class="buttontext">${uiLabelMap.ProductReceiveProduct}</a>
                </td>
              </tr>
            </table>
          </form>
        </#if>
