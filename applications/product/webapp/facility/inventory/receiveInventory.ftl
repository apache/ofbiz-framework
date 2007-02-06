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

<#if invalidProductId?exists>
<div class="errorMessage">${invalidProductId}</div>
</#if>

<div class="head1">${uiLabelMap.ProductReceiveInventory} <span class="head2">${uiLabelMap.CommonInto}&nbsp;<#if facility?has_content>"${facility.facilityName?default("Not Defined")}"</#if> [${uiLabelMap.CommonId} :${facility.facilityId?if_exists}]</span></div>
<a href="<@ofbizUrl>EditFacility</@ofbizUrl>" class="buttontext">[${uiLabelMap.ProductNewFacility}]</a>

<script language="JavaScript">
    function setNow(field) { eval('document.selectAllForm.' + field + '.value="${Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp().toString()}"'); }
</script>

<div>&nbsp;</div>

<#-- Receiving Results -->
<#if receivedItems?has_content>
  <table width="100%" border="0" cellpadding="2" cellspacing="0">
    <tr><td colspan="9"><div class="head3">${uiLabelMap.ProductReceiptPurchaseOrder} #${purchaseOrder.orderId}</div></td></tr>
    <tr><td colspan="9"><hr class="sepbar"></td></tr>
    <tr>
      <td><div class="tableheadtext">${uiLabelMap.ProductShipmentId}#</div></td>
      <td><div class="tableheadtext">${uiLabelMap.ProductReceipt}#</div></td>
      <td><div class="tableheadtext">${uiLabelMap.CommonDate}</div></td>
      <td><div class="tableheadtext">${uiLabelMap.ProductPo} #</div></td>
      <td><div class="tableheadtext">${uiLabelMap.ProductLine} #</div></td>
      <td><div class="tableheadtext">${uiLabelMap.ProductProductId}</div></td>
      <td><div class="tableheadtext">${uiLabelMap.ProductPerUnitPrice}</div></td>
      <td><div class="tableheadtext">${uiLabelMap.CommonRejected}</div></td>
      <td><div class="tableheadtext">${uiLabelMap.CommonAccepted}</div></td>
    </tr>
    <tr><td colspan="9"><hr class="sepbar"></td></tr>
    <#list receivedItems as item>
      <tr>
        <td><div class="tabletext"><a href="<@ofbizUrl>ViewShipment?shipmentId=${item.shipmentId?if_exists}</@ofbizUrl>" class="buttontext">${item.shipmentId?if_exists}</a></div></td>
        <td><div class="tabletext">${item.receiptId}</div></td>
        <td><div class="tabletext">${item.getString("datetimeReceived").toString()}</div></td>
        <td><div class="tabletext"><a href="/ordermgr/control/orderview?orderId=${item.orderId}" class="buttontext">${item.orderId}</a></div></td>
        <td><div class="tabletext">${item.orderItemSeqId}</div></td>
        <td><div class="tabletext">${item.productId?default("Not Found")}</div></td>
        <td><div class="tabletext">${item.unitCost?default(0)?string("##0.00")}</td>
        <td><div class="tabletext">${item.quantityRejected?default(0)?string.number}</div></td>
        <td><div class="tabletext">${item.quantityAccepted?string.number}</div></td>
      </tr>
    </#list>
    <tr><td colspan="9"><hr class="sepbar"></td></tr>
  </table>
  <br/>
</#if>

<#-- Single Product Receiving -->
<#if requestParameters.initialSelected?exists && product?has_content>
  <form method="post" action="<@ofbizUrl>receiveSingleInventoryProduct</@ofbizUrl>" name="selectAllForm" style="margin: 0;">
    <table border="0" cellpadding="2" cellspacing="0">
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
        <td width="6%" align="right" nowrap><div class="tabletext">${uiLabelMap.ProductPurchaseOrder}</div></td>
        <td width="6%">&nbsp;</td>
        <td width="74%">
          <b>${purchaseOrder.orderId}</b>&nbsp;/&nbsp;<b>${firstOrderItem.orderItemSeqId}</b>
          <#if 1 < purchaseOrderItemsSize>
            <span class="tabletext">(${uiLabelMap.ProductMultipleOrderItemsProduct} - ${purchaseOrderItemsSize}:1 ${uiLabelMap.ProductItemProduct})</span>
          <#else>
            <span class="tabletext">(${uiLabelMap.ProductSingleOrderItemProduct} - 1:1 ${uiLabelMap.ProductItemProduct})<span>
          </#if>
        </td>
      </tr>
      </#if>
      <tr>
        <td width="14%">&nbsp;</td>
        <td width="6%" align="right" nowrap><div class="tabletext">${uiLabelMap.ProductProductId}</div></td>
        <td width="6%">&nbsp;</td>
        <td width="74%">
          <b>${requestParameters.productId?if_exists}</b>
        </td>
      </tr>
      <tr>
        <td width="14%">&nbsp;</td>
        <td width="6%" align="right" nowrap><div class="tabletext">${uiLabelMap.ProductProductName}</div></td>
        <td width="6%">&nbsp;</td>
        <td width="74%">
          <div class="tabletext"><a href="/catalog/control/EditProduct?productId=${product.productId}${externalKeyParam?if_exists}" target="catalog" class="buttontext">${product.internalName?if_exists}</a></div>
        </td>
      </tr>
      <tr>
        <td width="14%">&nbsp;</td>
        <td width="6%" align="right" nowrap><div class="tabletext">${uiLabelMap.ProductProductDescription}</div></td>
        <td width="6%">&nbsp;</td>
        <td width="74%">
          <div class="tabletext">${product.description?if_exists}</div>
        </td>
      </tr>
      <tr>
        <td width="14%">&nbsp;</td>
        <td width="6%" align="right" nowrap><div class="tabletext">${uiLabelMap.ProductItemDescription}</div></td>
        <td width="6%">&nbsp;</td>
        <td width="74%">
          <input type="text" name="itemDescription" size="30" maxlength="60" class="inputBox"/>
        </td>
      </tr>
      <tr>
        <td width="14%">&nbsp;</td>
        <td width="6%" align="right" nowrap><div class="tabletext">${uiLabelMap.ProductInventoryItemType} </div></td>
        <td width="6%">&nbsp;</td>
        <td width="74%">
          <select name="inventoryItemTypeId" size="1" class="selectBox">
            <#list inventoryItemTypes as nextInventoryItemType>
              <option value="${nextInventoryItemType.inventoryItemTypeId}"
                <#if (facility.defaultInventoryItemTypeId?has_content) && (nextInventoryItemType.inventoryItemTypeId == facility.defaultInventoryItemTypeId)>
                  SELECTED
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
        <td width="6%" align="right" nowrap><div class="tabletext">${uiLabelMap.ProductFacilityOwner}</div></td>
        <td width="6%">&nbsp;</td>
        <td width="74%">
            <input type="text" name="ownerPartyId" value="" size="20" maxlength="20" class="inputBox">
            <a href="javascript:call_fieldlookup2(document.selectAllForm.ownerPartyId, 'LookupPartyName');"><img src="<@ofbizContentUrl>/images/fieldlookup.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Lookup"/></a>            
        </td>
      </tr>
      <tr>
        <td width="14%">&nbsp;</td>
        <td width="6%" align="right" nowrap><div class="tabletext">${uiLabelMap.ProductDateReceived}</div></td>
        <td width="6%">&nbsp;</td>
        <td width="74%">
          <input type="text" name="datetimeReceived" size="24" value="${Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp().toString()}" class="inputBox">
          <#-- <a href="#" onclick="setNow("datetimeReceived")" class="buttontext">[Now]</a> -->
        </td>
      </tr>

      <#-- facility location(s) -->
      <#assign facilityLocations = (product.getRelatedByAnd("ProductFacilityLocation", Static["org.ofbiz.base.util.UtilMisc"].toMap("facilityId", facilityId)))?if_exists/>
      <tr>
        <td width="14%">&nbsp;</td>
        <td width="6%" align="right" nowrap><div class="tabletext">${uiLabelMap.ProductFacilityLocation}</div></td>
        <td width="6%">&nbsp;</td>
        <td width="74%">
          <#if facilityLocations?has_content>
            <select name="locationSeqId" class="selectBox">
              <#list facilityLocations as productFacilityLocation>
                <#assign facility = productFacilityLocation.getRelatedOneCache("Facility")/>
                <#assign facilityLocation = productFacilityLocation.getRelatedOne("FacilityLocation")?if_exists/>
                <#assign facilityLocationTypeEnum = (facilityLocation.getRelatedOneCache("TypeEnumeration"))?if_exists/>
                <option value="${productFacilityLocation.locationSeqId}"><#if facilityLocation?exists>${facilityLocation.areaId?if_exists}:${facilityLocation.aisleId?if_exists}:${facilityLocation.sectionId?if_exists}:${facilityLocation.levelId?if_exists}:${facilityLocation.positionId?if_exists}</#if><#if facilityLocationTypeEnum?exists>(${facilityLocationTypeEnum.get("description",locale)})</#if>[${productFacilityLocation.locationSeqId}]</option>
              </#list>
              <option value="">${uiLabelMap.ProductNoLocation}</option>
            </select>
          <#else>
            <input type="text" name="locationSeqId" size="20" maxlength="20" class="inputBox"/>
            <span class="tabletext">
                <a href="javascript:call_fieldlookup2(document.selectAllForm.locationSeqId,'LookupFacilityLocation<#if parameters.facilityId?exists>?facilityId=${facilityId}</#if>');">
                    <img src="<@ofbizContentUrl>/images/fieldlookup.gif"</@ofbizContentUrl>" width="15" height="14" border="0" alt="Click here For Field Lookup"/>
                </a>
            </span>
          </#if>
        </td>
      </tr>
      <tr>
        <td width="14%">&nbsp;</td>
        <td width="6%" align="right" nowrap><div class="tabletext">${uiLabelMap.ProductRejectedReason}</div></td>
        <td width="6%">&nbsp;</td>
        <td width="74%">
          <select name="rejectionId" size="1" class="selectBox">
            <option></option>
            <#list rejectReasons as nextRejection>
              <option value="${nextRejection.rejectionId}">${nextRejection.get("description",locale)?default(nextRejection.rejectionId)}</option>
            </#list>
          </select>
        </td>
      </tr>
      <tr>
        <td width="14%">&nbsp;</td>
        <td width="6%" align="right" nowrap><div class="tabletext">${uiLabelMap.ProductQuantityRejected}</div></td>
        <td width="6%">&nbsp;</td>
        <td width="74%">
          <input type="text" name="quantityRejected" size="5" value="0" class="inputBox"/>
        </td>
      </tr>
      <tr>
        <td width="14%">&nbsp;</td>
        <td width="6%" align="right" nowrap><div class="tabletext">${uiLabelMap.ProductQuantityAccepted}</div></td>
        <td width="6%">&nbsp;</td>
        <td width="74%">
          <input type="text" name="quantityAccepted" size="5" value="${defaultQuantity?default(1)?string.number}" class="inputBox"/>
        </td>
      </tr>
      <tr>
        <td width="14%">&nbsp;</td>
        <td width="6%" align="right" nowrap><div class="tabletext">${uiLabelMap.ProductPerUnitPrice}</div></td>
        <td width="6%">&nbsp;</td>
        <td width="74%">
          <#-- get the default unit cost -->
          <#if (!unitCost?exists || unitCost == 0.0)><#assign unitCost = standardCosts.get(product.productId)?default(0)/></#if>
          <input type="text" name="unitCost" size="10" value="${unitCost}" class="inputBox"/>
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
  <form method="post" action="<@ofbizUrl>ReceiveInventory</@ofbizUrl>" name="selectAllForm" style="margin: 0;">
    <#-- general request fields -->
    <input type="hidden" name="facilityId" value="${requestParameters.facilityId?if_exists}"/>
    <input type="hidden" name="purchaseOrderId" value="${requestParameters.purchaseOrderId?if_exists}"/>
    <input type="hidden" name="initialSelected" value="Y"/>
    <table width="100%" border="0" cellpadding="2" cellspacing="0">
      <tr>
        <td>
          <div class="head3">${uiLabelMap.ProductSelectShipmentReceive}</div>
        </td>
      </tr>
      <#list shipments?if_exists as shipment>
        <#assign originFacility = shipment.getRelatedOneCache("OriginFacility")?if_exists/>
        <#assign destinationFacility = shipment.getRelatedOneCache("DestinationFacility")?if_exists/>
        <#assign statusItem = shipment.getRelatedOneCache("StatusItem")/>
        <#assign shipmentType = shipment.getRelatedOneCache("ShipmentType")/>
        <#assign shipmentDate = shipment.estimatedArrivalDate?if_exists/>
        <tr>
          <td><hr class="sepbar"></td>
        </tr>
        <tr>
          <td>
            <table width="100%" border="0" cellpadding="2" cellspacing="0">
              <tr>
                <td width="5%" nowrap><input type="radio" name="shipmentId" value="${shipment.shipmentId}"></td>
                <td width="5%" nowrap><div class="tabletext">${shipment.shipmentId}</div></td>
                <td><div class="tabletext">${shipmentType.get("description",locale)?default(shipmentType.shipmentTypeId?default(""))}</div></td>
                <td><div class="tabletext">${statusItem.get("description",locale)?default(statusItem.statusId?default("N/A"))}</div></td>
                <td><div class="tabletext">${(originFacility.facilityName)?if_exists} [${shipment.originFacilityId?if_exists}]</div></td>
                <td><div class="tabletext">${(destinationFacility.facilityName)?if_exists} [${shipment.destinationFacilityId?if_exists}]</div></td>
                <td><div class="tabletext"><span style="white-space: nowrap;">${(shipment.estimatedArrivalDate.toString())?if_exists}</span></div></td>
              </tr>
            </table>
          </td>
        </tr>
      </#list>
      <tr>
        <td><hr class="sepbar"></td>
      </tr>
      <tr>
        <td>
          <table width="100%" border="0" cellpadding="2" cellspacing="0">
            <tr>
              <td width="5%" nowrap><input type="radio" name="shipmentId" value="_NA_"></td>
              <td width="5%" nowrap><div class="tabletext">${uiLabelMap.ProductNoSpecificShipment}</div></td>
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
  <form method="post" action="<@ofbizUrl>receiveInventoryProduct</@ofbizUrl>" name="selectAllForm" style="margin: 0;">
    <#-- general request fields -->
    <input type="hidden" name="facilityId" value="${requestParameters.facilityId?if_exists}"/>
    <input type="hidden" name="purchaseOrderId" value="${requestParameters.purchaseOrderId?if_exists}"/>
    <input type="hidden" name="initialSelected" value="Y"/>
    <input type="hidden" name="_useRowSubmit" value="Y"/>
    <#assign now = Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp().toString()/>
    <#assign rowCount = 0/>
    <table width="100%" border="0" cellpadding="2" cellspacing="0">
      <#if !purchaseOrderItems?exists || purchaseOrderItemsSize == 0>
        <tr>
          <td colspan="2"><div class="tableheadtext">${uiLabelMap.ProductNoItemsPoReceive}.</div></td>
        </tr>
      <#else/>
        <tr>
          <td>
            <div class="head3">${uiLabelMap.ProductReceivePurchaseOrder} #${purchaseOrder.orderId}</div>
            <#if shipment?has_content>
            <div class="head3">${uiLabelMap.ProductShipmentId} #${shipment.shipmentId}</div>
            </#if>
          </td>
          <td align="right">
            <span class="tableheadtext">${uiLabelMap.CommonSelectAll}</span>&nbsp;
            <input type="checkbox" name="selectAll" value="${uiLabelMap.CommonY}" onclick="javascript:toggleAll(this, 'selectAllForm');"/>
          </td>
        </tr>
        <#list purchaseOrderItems as orderItem>
          <#assign defaultQuantity = orderItem.quantity - receivedQuantities[orderItem.orderItemSeqId]?double/>
          <#assign itemCost = orderItem.unitPrice?default(0)/>
          <#assign salesOrderItem = salesOrderItems[orderItem.orderItemSeqId]?if_exists/>
          <#if shipment?has_content>
          <#assign defaultQuantity = shippedQuantities[orderItem.orderItemSeqId]?double - receivedQuantities[orderItem.orderItemSeqId]?double/>
          </#if>
          <#if 0 < defaultQuantity>
          <#assign orderItemType = orderItem.getRelatedOne("OrderItemType")/>
          <input type="hidden" name="orderId_o_${rowCount}" value="${orderItem.orderId}"/>
          <input type="hidden" name="orderItemSeqId_o_${rowCount}" value="${orderItem.orderItemSeqId}"/>
          <input type="hidden" name="facilityId_o_${rowCount}" value="${requestParameters.facilityId?if_exists}"/>
          <input type="hidden" name="datetimeReceived_o_${rowCount}" value="${now}"/>
          <#if shipment?exists && shipment.shipmentId?has_content>
            <input type="hidden" name="shipmentId_o_${rowCount}" value="${shipment.shipmentId}"/>
          </#if>
          <#if salesOrderItem?has_content>
            <input type="hidden" name="priorityOrderId_o_${rowCount}" value="${salesOrderItem.orderId}"/>
            <input type="hidden" name="priorityOrderItemSeqId_o_${rowCount}" value="${salesOrderItem.orderItemSeqId}"/>
          </#if>

          <tr>
            <td colspan="2"><hr class="sepbar"></td>
          </tr>
          <tr>
            <td>
              <table width="100%" border="0" cellpadding="2" cellspacing="0">
                <tr>
                  <#if orderItem.productId?exists>
                    <#assign product = orderItem.getRelatedOneCache("Product")/>
                    <input type="hidden" name="productId_o_${rowCount}" value="${product.productId}"/>
                    <td width="45%">
                      <div class="tabletext">
                        ${orderItem.orderItemSeqId}:&nbsp;<a href="/catalog/control/EditProduct?productId=${product.productId}${externalKeyParam?if_exists}" target="catalog" class="buttontext">${product.productId}&nbsp;-&nbsp;${orderItem.itemDescription?if_exists}</a> : ${product.description?if_exists}
                      </div>
                    </td>
                  <#else>
                    <td width="45%">
                      <div class="tabletext">
                        <b>${orderItemType.get("description",locale)}</b> : ${orderItem.itemDescription?if_exists}&nbsp;&nbsp;
                        <input type="text" class="inputBox" size="12" name="productId_o_${rowCount}"/>
                        <a href="/catalog/control/EditProduct?externalLoginKey=${externalLoginKey}" target="catalog" class="buttontext">${uiLabelMap.ProductCreateProduct}</a>
                      </div>
                    </td>
                  </#if>
                  <td align="right">
                    <div class="tableheadtext">${uiLabelMap.ProductLocation}:</div>
                  </td>
                  <#-- location(s) -->
                  <td align="right">
                    <#assign facilityLocations = (orderItem.getRelatedByAnd("ProductFacilityLocation", Static["org.ofbiz.base.util.UtilMisc"].toMap("facilityId", facilityId)))?if_exists/>
                    <#if facilityLocations?has_content>
                      <select name="locationSeqId_o_${rowCount}" class="selectBox">
                        <#list facilityLocations as productFacilityLocation>
                          <#assign facility = productFacilityLocation.getRelatedOneCache("Facility")/>
                          <#assign facilityLocation = productFacilityLocation.getRelatedOne("FacilityLocation")?if_exists/>
                          <#assign facilityLocationTypeEnum = (facilityLocation.getRelatedOneCache("TypeEnumeration"))?if_exists/>
                          <option value="${productFacilityLocation.locationSeqId}"><#if facilityLocation?exists>${facilityLocation.areaId?if_exists}:${facilityLocation.aisleId?if_exists}:${facilityLocation.sectionId?if_exists}:${facilityLocation.levelId?if_exists}:${facilityLocation.positionId?if_exists}</#if><#if facilityLocationTypeEnum?exists>(${facilityLocationTypeEnum.get("description",locale)})</#if>[${productFacilityLocation.locationSeqId}]</option>
                        </#list>
                        <option value="">${uiLabelMap.ProductNoLocation}</option>
                      </select>
                    <#else>
                      <input type="text" class="inputBox" name="locationSeqId_o_${rowCount}" size="12"/>
                      <span class="tabletext">
                          <a href="javascript:call_fieldlookup2(document.selectAllForm.locationSeqId_o_${rowCount},'LookupFacilityLocation<#if parameters.facilityId?exists>?facilityId=${facilityId}</#if>');">
                              <img src="<@ofbizContentUrl>/images/fieldlookup.gif"</@ofbizContentUrl>" width="15" height="14" border="0" alt="Click here For Field Lookup"/>
                          </a>
                      </span>
                    </#if>
                  </td>
                  <td align="right">
                    <div class="tableheadtext">${uiLabelMap.ProductQtyReceived} :</div>
                  </td>
                  <td align="right">
                    <input type="text" class="inputBox" name="quantityAccepted_o_${rowCount}" size="6" value="${defaultQuantity?string.number}"/>
                  </td>
                </tr>
                <tr>
                  <td width="45%">
                    <span class="tableheadtext">${uiLabelMap.ProductInventoryItemType} :</span>&nbsp;&nbsp;
                    <select name="inventoryItemTypeId_o_${rowCount}" size="1" class="selectBox">
                      <#list inventoryItemTypes as nextInventoryItemType>
                      <option value="${nextInventoryItemType.inventoryItemTypeId}"
                       <#if (facility.defaultInventoryItemTypeId?has_content) && (nextInventoryItemType.inventoryItemTypeId == facility.defaultInventoryItemTypeId)>
                        SELECTED
                      </#if>
                      >${nextInventoryItemType.get("description",locale)?default(nextInventoryItemType.inventoryItemTypeId)}</option>
                      </#list>
                    </select>
                  </td>
                  <td align="right">
                    <div class="tableheadtext">${uiLabelMap.ProductRejectionReason} :</div>
                  </td>
                  <td align="right">
                    <select name="rejectionId_o_${rowCount}" size="1" class="selectBox">
                      <option></option>
                      <#list rejectReasons as nextRejection>
                      <option value="${nextRejection.rejectionId}">${nextRejection.get("description",locale)?default(nextRejection.rejectionId)}</option>
                      </#list>
                    </select>
                  </td>
                  <td align="right">
                    <div class="tableheadtext">${uiLabelMap.ProductQtyRejected} :</div>
                  </td>
                  <td align="right">
                    <input type="text" class="inputBox" name="quantityRejected_o_${rowCount}" value="0" size="6"/>
                  </td>
                </tr>
                <tr>
                  <td>&nbsp;</td>
                  <td align="right"><span class="tableheadtext">${uiLabelMap.ProductFacilityOwner}:</span></td>
                  <td align="right"><input type="text" class="inputBox" name="ownerPartyId_o_${rowCount}" size="20" maxlength="20" value="${facility.ownerPartyId}"/></td>
                  <td align="right">
                    <div class="tableheadtext">${uiLabelMap.ProductPerUnitPrice} :</div>
                  </td>
                  <td align="right">
                    <input type="hidden" name="currencyUomId_o_${rowCount}" value="${currencyUomId?if_exists}"/>
                    <input type="text" class="inputBox" name="unitCost_o_${rowCount}" value="${itemCost}" size="6" maxlength="20"/>
                    <span class="tabletext">${currencyUomId?if_exists}</span>
                  </td>
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
            <hr class="sepbar">
          </td>
        </tr>
        <#if rowCount == 0>
          <tr>
            <td colspan="2">
              <div class="tabletext">${uiLabelMap.ProductNoItemsPo} #${purchaseOrder.orderId} ${uiLabelMap.ProductToReceive}.</div>
            </td>
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
  <form name="selectAllForm" method="post" action="<@ofbizUrl>ReceiveInventory</@ofbizUrl>" style="margin: 0;">
    <input type="hidden" name="facilityId" value="${requestParameters.facilityId?if_exists}"/>
    <input type="hidden" name="initialSelected" value="Y"/>
    <table border="0" cellpadding="2" cellspacing="0">
      <tr><td colspan="4"><div class="head3">${uiLabelMap.ProductReceiveItem}</div></td></tr>
      <tr>
        <td width="25%" align="right"><div class="tabletext">${uiLabelMap.ProductPurchaseOrderNumber}</div></td>
        <td>&nbsp;</td>
        <td width="25%">
          <input type="text" class="inputBox" name="purchaseOrderId" size="20" maxlength="20" value="${requestParameters.purchaseOrderId?if_exists}">
          <span class="tabletext">
            <a href="javascript:call_fieldlookup2(document.selectAllForm.purchaseOrderId,'LookupPurchaseOrderHeaderAndShipInfo');">
              <img src="<@ofbizContentUrl>/images/fieldlookup.gif"</@ofbizContentUrl>" width="15" height="14" border="0" alt="Click here For Field Lookup"/>
            </a>
          </span>
        </td>
        <td><div class="tabletext">&nbsp;(${uiLabelMap.ProductLeaveSingleProductReceiving})</div></td>
      </tr>
      <tr>
        <td width="25%" align="right"><div class="tabletext">${uiLabelMap.ProductProductId}</div></td>
        <td>&nbsp;</td>
        <td width="25%">
          <input type="text" class="inputBox" name="productId" size="20" maxlength="20" value="${requestParameters.productId?if_exists}"/>
          <span class="tabletext">
            <a href="javascript:call_fieldlookup2(document.selectAllForm.productId,'LookupProduct');">
              <img src="<@ofbizContentUrl>/images/fieldlookup.gif"</@ofbizContentUrl>" width="15" height="14" border="0" alt="Click here For Field Lookup"/>
            </a>
          </span>
        </td>
        <td><div class="tabletext">&nbsp;(${uiLabelMap.ProductLeaveEntirePoReceiving})</div></td>
      </tr>
      <tr>
        <td colspan="2">&nbsp;</td>
        <td colspan="2">
          <a href="javascript:document.selectAllForm.submit();" class="buttontext">${uiLabelMap.ProductReceiveProduct}</a>
        </td>
      </tr>
    </table>
  </form>
</#if>
