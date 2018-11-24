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
<#if shipment??>

    <#-- JS to populate the quantity_o_# field required by the chained issueOrderItemToShipment service -->
    <script type="application/javascript">
      function populateQuantities(rowCount) {
        for (var x = 0; x <= rowCount; x++) {
          var quantityAcceptedInput = document.getElementById('quantityAccepted_o_' + x);
          var quantityInput = document.getElementById('quantity_o_' + x);
          if (quantityAcceptedInput != null && quantityInput != null) {
            quantityInput.value = quantityAcceptedInput.value;
          }
        }
      }
    </script>

    <#assign productId = parameters.productId!/>
<div class="screenlet">
    <div class="screenlet-title-bar">
        <ul>
            <li class="h3">${uiLabelMap.ProductReceiveInventoryAgainstPurchaseOrder}</li>
        </ul>
        <br class="clear"/>
    </div>
    <div class="screenlet-body">
    
    <#if ! isPurchaseShipment>
        <div class="errorMessage">
            <#assign uiLabelWithVar=uiLabelMap.ProductErrorShipmentNotPurchaseShipment?interpret><@uiLabelWithVar/>
        </div>
    <#elseif orderId?has_content && !orderHeader??>
        <div class="errorMessage">
            <#assign uiLabelWithVar=uiLabelMap.ProductErrorOrderIdNotFound?interpret><@uiLabelWithVar/>
        </div>
    <#elseif orderHeader?? && orderHeader.orderTypeId != "PURCHASE_ORDER">
        <div class="errorMessage">
            <#assign uiLabelWithVar=uiLabelMap.ProductErrorOrderNotPurchaseOrder?interpret><@uiLabelWithVar/>
        </div>
    <#elseif ProductReceiveInventoryAgainstPurchaseOrderProductNotFound??>
        <div class="errorMessage">
            <#assign uiLabelWithVar=uiLabelMap.ProductReceiveInventoryAgainstPurchaseOrderProductNotFound?interpret><@uiLabelWithVar/>
            <script type="application/javascript">window.onload=function(){showErrorAlert("${uiLabelMap.CommonErrorMessage2}","<@uiLabelWithVar/>");};</script>
        </div>
    <#elseif ProductReceiveInventoryAgainstPurchaseOrderQuantityExceedsAvailableToReceive??>
        <div class="errorMessage">
            <#assign uiLabelWithVar=uiLabelMap.ProductReceiveInventoryAgainstPurchaseOrderQuantityExceedsAvailableToReceive?interpret><@uiLabelWithVar/>
            <script type="application/javascript">window.onload=function(){showErrorAlert("${uiLabelMap.CommonErrorMessage2}","<@uiLabelWithVar/>");};</script>
        </div>
    </#if>
    <#if ProductReceiveInventoryAgainstPurchaseOrderQuantityGoesToBackOrder??>
        <div class="errorMessage" style="color:green">
            <#assign uiLabelWithVar=uiLabelMap.ProductReceiveInventoryAgainstPurchaseOrderQuantityGoesToBackOrder?interpret><@uiLabelWithVar/>
            <script type="application/javascript">window.onload=function(){showErrorAlert("${uiLabelMap.CommonErrorMessage2}","<@uiLabelWithVar/>");};</script>
        </div>
    </#if>
</#if>

<form name="ReceiveInventoryAgainstPurchaseOrder" action="<@ofbizUrl>ReceiveInventoryAgainstPurchaseOrder</@ofbizUrl>">
  <input type="hidden" name="clearAll" value="Y"/>
  <table class="basic-table" cellspacing="0">
    <tr>
      <td class="label">${uiLabelMap.ProductShipmentId}</td>
      <td><input type="text" size="20" name="shipmentId" value="${shipmentId!}"/></td>
    </tr>
    <tr>
      <td class="label">${uiLabelMap.ProductOrderId}</td>
      <td><@htmlTemplate.lookupField value="${orderId!}" formName="ReceiveInventoryAgainstPurchaseOrder" name="purchaseOrderId" id="purchaseOrderId" fieldFormName="LookupOrderHeaderAndShipInfo"/></td>
    </tr>
    <tr>
      <td class="label">${uiLabelMap.ProductOrderShipGroupId}</td>
      <td><input type="text" size="20" name="shipGroupSeqId" value="${shipGroupSeqId!}"/></td>
    </tr>
    <tr>
      <td>&nbsp;</td>
      <td><input type="submit" value="${uiLabelMap.CommonSelect}" class="smallSubmit"/></td>
    </tr>
  </table>
</form>

<#if shipment??>
    <#if isPurchaseShipment>

        <#assign itemsAvailableToReceive = totalAvailableToReceive?default(0) &gt; 0/>
        <#if orderItemDatas??>
            <br />
            <#assign rowCount = 0>
            <#assign totalReadyToReceive = 0/>
            <form action="<@ofbizUrl>issueOrderItemToShipmentAndReceiveAgainstPO?clearAll=Y</@ofbizUrl>" method="post" name="selectAllForm">
                <input type="hidden" name="facilityId" value="${facilityId}"/>
                <input type="hidden" name="purchaseOrderId" value="${orderId}"/>
                <input type="hidden" name="shipmentId" value="${shipmentId}" />
                <input type="hidden" name="_useRowSubmit" value="Y"/>
                <table cellspacing="0" class="basic-table">
                    <tr class="header-row">
                        <td>${uiLabelMap.ProductProduct}</td>

                        <#-- Must use the uiLabelMap[""] notation since the label key has . in it -->
                        <td>${uiLabelMap["GoodIdentificationType.description.UPCA"]}</td>
                        <td>${uiLabelMap.OrderOrder}</td>
                        <td>${uiLabelMap.OrderCancelled}</td>
                        <td>${uiLabelMap.OrderBackOrdered}</td>
                        <td>${uiLabelMap.CommonReceived}</td>
                        <td>${uiLabelMap.ProductOpenQuantity}</td>
                        <td>${uiLabelMap.ProductBackOrders}</td>
                        <#if itemsAvailableToReceive>
                            <td>${uiLabelMap.CommonReceive}</td>
                            <td>${uiLabelMap.ProductInventoryItemType}</td>
                            <td colspan="2" align="right">
                                <div><label>${uiLabelMap.CommonAll}<input type="checkbox" name="selectAll" value="${uiLabelMap.CommonY}" checked class="selectAll" onclick="highlightAllRows(this, 'orderItemData_tableRow_', 'selectAllForm');" /></label></div>
                            </td>
                        </#if>
                    </tr>
                    <#assign alt_row = false>
                    <#list orderItemDatas! as orderItemData>
                        <#assign orderItem = orderItemData.orderItem>
                        <#assign product = orderItemData.product!>
                        <#assign itemShipGroupSeqId = orderItemData.shipGroupSeqId!>
                        <#assign totalQuantityReceived = orderItemData.totalQuantityReceived?default(0)>
                        <#assign availableToReceive = orderItemData.availableToReceive?default(0)>
                        <#assign backOrderedQuantity = orderItemData.backOrderedQuantity?default(0)>
                        <#assign fulfilledReservations = orderItemData.fulfilledReservations>
                        <tr id="orderItemData_tableRow_${rowCount}" valign="middle"<#if alt_row> class="alternate-row"</#if>>
                            <td>${(product.internalName)!} [${orderItem.productId?default("N/A")}]</div></td>
                            <td>
                                <div>
                                    <#assign upcaLookup = Static["org.apache.ofbiz.base.util.UtilMisc"].toMap("productId", product.productId, "goodIdentificationTypeId", "UPCA")/>
                                    <#assign upca = delegator.findOne("GoodIdentification", upcaLookup, true)!/>
                                    <#if upca?has_content>
                                        ${upca.idValue!}
                                    </#if>
                                </div>
                            </td>
                            <td>${orderItem.quantity}</td>
                            <td>${orderItem.cancelQuantity?default(0)}</td>
                            <td>
                                <div ${(backOrderedQuantity &gt; 0)?string(" errorMessage","")}">
                                    ${backOrderedQuantity}
                                </div>
                            </td>
                            <td>${totalQuantityReceived}</td>
                            <td>${orderItem.quantity - orderItem.cancelQuantity?default(0) - totalQuantityReceived}</td>
                            <td>
                                <div>
                                    <#if fulfilledReservations?has_content>
                                        <#list fulfilledReservations?sort_by("orderId") as fulfilledReservation>
                                            ${fulfilledReservation.orderId}<br />
                                        </#list>
                                    </#if>
                                </div>
                            </td>
                            <#if availableToReceive &gt; 0 >
                                <td>
                                    <input type="hidden" name="productId_o_${rowCount}" value="${(product.productId)!}"/>
                                    <input type="hidden" name="facilityId_o_${rowCount}" value="${facilityId}"/>
                                    <input type="hidden" name="shipmentId_o_${rowCount}" value="${shipmentId}"/>
                                    <input type="hidden" name="orderId_o_${rowCount}" value="${orderItem.orderId}"/>
                                    <input type="hidden" name="shipGroupSeqId_o_${rowCount}" value="${itemShipGroupSeqId!}"/>
                                    <input type="hidden" name="orderItemSeqId_o_${rowCount}" value="${orderItem.orderItemSeqId}"/>
                                    <input type="hidden" name="unitCost_o_${rowCount}" value="${orderItem.unitPrice?default(0)}"/>
                                    <input type="hidden" name="currencyUomId_o_${rowCount}" value="${currencyUomId?default("")}"/>
                                    <input type="hidden" name="ownerPartyId_o_${rowCount}" value="${(facility.ownerPartyId)!}"/>
                                    <input type="hidden" name="datetimeReceived_o_${rowCount}" value="${now}"/>
                                    <input type="hidden" name="quantityRejected_o_${rowCount}" value="0"/>
                                    <#-- quantity field required by the chained issueOrderItemToShipment service -->
                                    <input type="hidden" name="quantity_o_${rowCount}" id="quantity_o_${rowCount}" value=""/>
                                    <#if itemQuantitiesToReceive?? && itemQuantitiesToReceive.get(orderItem.orderItemSeqId)??>
                                        <#assign quantityToReceive = itemQuantitiesToReceive.get(orderItem.orderItemSeqId)>
                                    <#else>
                                        <#assign quantityToReceive = 0>
                                    </#if>
                                    <#assign totalReadyToReceive = totalReadyToReceive + quantityToReceive/>
                                    <input type="text" size="5" name="quantityAccepted_o_${rowCount}" id="quantityAccepted_o_${rowCount}" value="${quantityToReceive}"/>
                                </td>
                                <td>
                                    <select name="inventoryItemTypeId_o_${rowCount}">
                                      <#list inventoryItemTypes as inventoryItemType>
                                      <option value="${inventoryItemType.inventoryItemTypeId}"
                                          <#if (facility.defaultInventoryItemTypeId?has_content) && (inventoryItemType.inventoryItemTypeId == facility.defaultInventoryItemTypeId)>
                                              selected="selected"
                                          </#if>
                                      >${inventoryItemType.get("description",locale)?default(inventoryItemType.inventoryItemTypeId)}</option>
                                      </#list>
                                    </select>
                                </td>
                                <td align="right">
                                    <a href="<@ofbizUrl>ReceiveInventoryAgainstPurchaseOrder?shipmentId=${shipmentId}&amp;purchaseOrderId=${orderId}&amp;productId=${product.productId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonClear}</a>
                                </td>
                                <td align="right">
                                  <input type="checkbox" name="_rowSubmit_o_${rowCount}" value="Y" onclick="highlightRow(this,'orderItemData_tableRow_${rowCount}');" />
                                </td>
                                <#assign rowCount = rowCount + 1>
                            </#if>
                        </tr>
                        <#-- toggle the row color -->
                        <#assign alt_row = !alt_row>
                    </#list>
                    <#if itemsAvailableToReceive>
                        <tr>
                            <td colspan="11" align="right">
                                <a href="<@ofbizUrl>ReceiveInventoryAgainstPurchaseOrder?shipmentId=${shipmentId}&amp;purchaseOrderId=${orderId}&amp;clearAll=Y</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonClearAll}</a>
                            </td>
                            <td align="right">
                                <a class="smallSubmit" href="javascript:populateQuantities(${rowCount - 1});document.selectAllForm.submit();">${uiLabelMap.ProductReceiveItem}</a>
                            </td>
                        </tr>
                        <tr>
                            <td colspan="12" align="right">
                                <input form="orderForceCompletePurchaseOrder" type="submit" value="${uiLabelMap.OrderForceCompletePurchaseOrder}" class="smallSubmit"/>
                            </td>
                        </tr>
                    </#if>
                </table>
                <input type="hidden" name="_rowCount" value="${rowCount}" />
            </form>
            <form id="orderForceCompletePurchaseOrder" method="post" action="<@ofbizUrl>completePurchaseOrder</@ofbizUrl>">
                <input type="hidden" name="orderId" value="${orderId}"/>
                <input type="hidden" name="facilityId" value="${facilityId}"/>
                <input type="hidden" name="shipmentId" value="${shipmentId}"/>
                <input type="hidden" name="clearAll" value="Y"/>
            </form>
        </#if>
        <#if itemsAvailableToReceive && totalReadyToReceive < totalAvailableToReceive>
            <h3>${uiLabelMap.ProductReceiveInventoryAddProductToReceive}</h3>
            <form name="addProductToReceive" method="post" action="<@ofbizUrl>ReceiveInventoryAgainstPurchaseOrder</@ofbizUrl>">
                <input type="hidden" name="shipmentId" value="${shipmentId}"/>
                <input type="hidden" name="purchaseOrderId" value="${orderId}"/>
                <div>
                    <span class="label">${uiLabelMap.ProductProductId}/${uiLabelMap.ProductGoodIdentification}</span>
                    <input type="text" size="20" id="productId" name="productId" value=""/>
                        @
                    <input type="text"  name="quantity" size="6" maxlength="6" value="1" tabindex="0"/>
                    <input type="submit" value="${uiLabelMap.CommonAdd}" class="smallSubmit"/>
                </div>
            </form>
            <script type="application/javascript">
                document.getElementById('productId').focus();
            </script>
        </#if>
    </#if>
    </div>
</div>
<#elseif parameters.shipmentId?has_content>
  <h3>${uiLabelMap.ProductShipmentNotFoundId}: [${shipmentId!}]</h3>
</#if>
