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

<#if orderHeader?has_content>
    <div class="screenlet">
        <div class="screenlet-title-bar">
            <ul>
                <li class="h3">&nbsp;${uiLabelMap.OrderOrderItems}</li>
            </ul>
            <br class="clear" />
        </div>
        <div class="screenlet-body">
            <table class="order-items basic-table" cellspacing='0'>
                <tr valign="bottom" class="header-row">
                    <td width="30%">${uiLabelMap.ProductProduct}</td>
                    <td width="33%">${uiLabelMap.CommonStatus}</td>
                    <td width="5%">${uiLabelMap.OrderQuantity}</td>
                    <td width="10%" align="right">${uiLabelMap.OrderUnitList}</td>
                    <td width="10%" align="right">${uiLabelMap.OrderAdjustments}</td>
                    <td width="10%" align="right">${uiLabelMap.OrderSubTotal}</td>
                    <td width="2%">&nbsp;</td>
                </tr>
                <#if !orderItemList?has_content>
                    <tr>
                        <td colspan="7">
                            <font color="red">${uiLabelMap.checkhelper_sales_order_lines_lookup_failed}</font>
                        </td>
                    </tr>
                <#else>
                    <#assign itemClass = "2">
                    <#list orderItemList as orderItem>
                        <#assign orderItemShipGrpInvResList = orderReadHelper.getOrderItemShipGrpInvResList(orderItem)>
                        <#if "SALES_ORDER" == orderHeader.orderTypeId><#assign pickedQty = orderReadHelper.getItemPickedQuantityBd(orderItem)></#if>
                        <tr<#if "1" == itemClass> class="alternate-row"</#if>>
                            <#assign orderItemType = orderItem.getRelatedOne("OrderItemType", false)!>
                            <#assign productId = orderItem.productId!>
                            <#if productId?? && "shoppingcart.CommentLine" == productId>
                                <td colspan="7" valign="top" class="label"> &gt;&gt; ${orderItem.itemDescription}</td>
                            <#else>
                                <td colspan="7">
                                    <div class="order-item-description">
                                        <#if orderItem.supplierProductId?has_content>
                                            ${orderItem.supplierProductId} - ${orderItem.itemDescription!}
                                        <#elseif productId??>
                                            ${orderItem.productId?default("N/A")} - ${orderItem.itemDescription!}
                                            <#if (product.salesDiscontinuationDate)?? && Static["org.apache.ofbiz.base.util.UtilDateTime"].nowTimestamp().after(product.salesDiscontinuationDate)>
                                                <br />
                                                <span style="color: red;">${uiLabelMap.OrderItemDiscontinued}: ${Static["org.apache.ofbiz.base.util.UtilFormatOut"].formatDateTime(product.salesDiscontinuationDate, "", locale, timeZone)!}</span>
                                            </#if>
                                        <#elseif orderItemType??>
                                            ${orderItemType.description} - ${orderItem.itemDescription!}
                                        <#else>
                                            ${orderItem.itemDescription!}
                                        </#if>
                                        <#assign orderItemAttributes = orderItem.getRelated("OrderItemAttribute", null, null, false)/>
                                        <#if orderItemAttributes?has_content>
                                            <ul>
                                            <#list orderItemAttributes as orderItemAttribute>
                                                <li>
                                                    ${orderItemAttribute.attrName} : ${orderItemAttribute.attrValue}
                                                </li>
                                            </#list>
                                            </ul>
                                        </#if>
                                    </div>
                                    <div style="float:right;">
                                        <#assign downloadContents = EntityQuery.use(delegator).from("OrderItemAndProductContentInfo").where( "orderId", orderItem.orderId!, "orderItemSeqId", orderItem.orderItemSeqId!, "productContentTypeId", "DIGITAL_DOWNLOAD", "statusId", "ITEM_COMPLETED").queryList()!/>
                                        <#if downloadContents?has_content>
                                            <#list downloadContents as downloadContent>
                                                <a href="<@ofbizUrl controlPath="/content/control">ViewSimpleContent?contentId=${downloadContent.contentId}</@ofbizUrl>" class="buttontext" target="_blank">${uiLabelMap.ContentDownload}</a>&nbsp;
                                            </#list>
                                        </#if>
                                        <a href="<@ofbizUrl controlPath="/catalog/control">EditProduct?productId=${productId}</@ofbizUrl>" class="buttontext" target="_blank">${uiLabelMap.ProductCatalog}</a>
                                        <a href="<@ofbizUrl controlPath="/ecommerce/control">product?product_id=${productId}</@ofbizUrl>" class="buttontext" target="_blank">${uiLabelMap.OrderEcommerce}</a>
                                    </div>
                                </td>
                            </#if>
                        </tr>
                        <tr<#if "1" == itemClass> class="alternate-row"</#if>>
                            <#if productId?? && "shoppingcart.CommentLine" == productId>
                                <td colspan="7" valign="top" class="label"> &gt;&gt; ${orderItem.itemDescription}</td>
                            <#else>
                                <td valign="top">
                                    <#if productId?has_content>
                                        <#assign product = orderItem.getRelatedOne("Product", true)>
                                    </#if>
                                    <#if productId??>
                                        <#-- INVENTORY -->
                                        <#if (orderHeader.statusId != "ORDER_COMPLETED") && availableToPromiseMap?? && quantityOnHandMap?? && availableToPromiseMap.get(productId)?? && quantityOnHandMap.get(productId)??>
                                            <#assign quantityToProduce = 0>
                                            <#assign atpQuantity = availableToPromiseMap.get(productId)?default(0)>
                                            <#assign qohQuantity = quantityOnHandMap.get(productId)?default(0)>
                                            <#assign mktgPkgATP = mktgPkgATPMap.get(productId)?default(0)>
                                            <#assign mktgPkgQOH = mktgPkgQOHMap.get(productId)?default(0)>
                                            <#assign requiredQuantity = requiredProductQuantityMap.get(productId)?default(0)>
                                            <#assign onOrderQuantity = onOrderProductQuantityMap.get(productId)?default(0)>
                                            <#assign inProductionQuantity = productionProductQuantityMap.get(productId)?default(0)>
                                            <#assign unplannedQuantity = requiredQuantity - qohQuantity - inProductionQuantity - onOrderQuantity - mktgPkgQOH>
                                            <#if unplannedQuantity < 0><#assign unplannedQuantity = 0></#if>
                                            <div class="screenlet order-item-inventory">
                                                <div class="screenlet-body">
                                                    <table cellspacing="0" cellpadding="0" border="0">
                                                        <tr>
                                                            <td style="text-align: right; padding-bottom: 10px;">
                                                                <a class="buttontext"
                                                                   href="/catalog/control/EditProductInventoryItems?productId=${productId}&amp;showAllFacilities=Y${StringUtil.wrapString(externalKeyParam)}"
                                                                   target="_blank">${uiLabelMap.ProductInventory}</a>
                                                            </td>
                                                            <td>&nbsp;</td>
                                                        </tr>
                                                        <tr>
                                                            <td>${uiLabelMap.OrderRequiredForSO}</td>
                                                            <td style="padding-left: 15px; text-align: left;">${requiredQuantity}</td>
                                                        </tr>
                                                        <#if availableToPromiseByFacilityMap?? && quantityOnHandByFacilityMap?? && quantityOnHandByFacilityMap.get(productId)?? && availableToPromiseByFacilityMap.get(productId)??>
                                                            <#assign atpQuantityByFacility = availableToPromiseByFacilityMap.get(productId)?default(0)>
                                                            <#assign qohQuantityByFacility = quantityOnHandByFacilityMap.get(productId)?default(0)>
                                                            <tr>
                                                                <td>
                                                                    ${uiLabelMap.ProductInInventory} [${facility.facilityName!}] ${uiLabelMap.ProductQoh}
                                                                </td>
                                                                <td style="padding-left: 15px; text-align: left;">
                                                                    ${qohQuantityByFacility} (${uiLabelMap.ProductAtp}: ${atpQuantityByFacility})
                                                                </td>
                                                            </tr>
                                                        </#if>
                                                        <tr>
                                                            <td>
                                                                ${uiLabelMap.ProductInInventory} [${uiLabelMap.CommonAll} ${uiLabelMap.ProductFacilities}] ${uiLabelMap.ProductQoh}
                                                            </td>
                                                            <td style="padding-left: 15px; text-align: left;">
                                                                ${qohQuantity} (${uiLabelMap.ProductAtp}: ${atpQuantity})
                                                            </td>
                                                        </tr>
                                                        <#if (product?has_content) && (product.productTypeId?has_content) && Static["org.apache.ofbiz.entity.util.EntityTypeUtil"].hasParentType(delegator, "ProductType", "productTypeId", product.productTypeId, "parentTypeId", "MARKETING_PKG")>
                                                            <tr>
                                                                <td>${uiLabelMap.ProductMarketingPackageQOH}</td>
                                                                <td style="padding-left: 15px; text-align: left;">
                                                                    ${mktgPkgQOH} (${uiLabelMap.ProductAtp}: ${mktgPkgATP})
                                                                </td>
                                                            </tr>
                                                        </#if>
                                                        <tr>
                                                            <td>${uiLabelMap.OrderOnOrder}</td>
                                                            <td style="padding-left: 15px; text-align: left;">${onOrderQuantity}</td>
                                                        </tr>
                                                        <tr>
                                                            <td>${uiLabelMap.OrderInProduction}</td>
                                                            <td style="padding-left: 15px; text-align: left;">${inProductionQuantity}</td>
                                                        </tr>
                                                        <tr>
                                                            <td>${uiLabelMap.OrderUnplanned}</td>
                                                            <td style="padding-left: 15px; text-align: left;">${unplannedQuantity}</td>
                                                        </tr>
                                                    </table>
                                                </div>
                                            </div>
                                        </#if>
                                    </#if>
                                </td>
                                <#-- now show status details per line item -->
                                <#assign currentItemStatus = orderItem.getRelatedOne("StatusItem", false)>
                                <td colspan="1" valign="top">
                                    <#if "SALES_ORDER" == orderHeader.orderTypeId!>
                                      <table>
                                          <tr>
                                              <td class="label">
                                                  <span class="label">${uiLabelMap.OrderReserveAfterDate}</span>
                                              </td>
                                              <td>
                                                  ${orderItem.reserveAfterDate!}
                                              </td>
                                          </tr>
                                      </table>
                                    </#if>

                                    <div class="screenlet order-item-status-list<#if currentItemStatus.statusCode?has_content> ${currentItemStatus.statusCode}</#if>">
                                        <div class="screenlet-body">
                                            <div class="current-status">
                                                <span class="label">${uiLabelMap.CommonCurrent}</span>&nbsp;${currentItemStatus.get("description",locale)?default(currentItemStatus.statusId)}
                                            </div>
                                            <#if ("ITEM_CREATED" == (currentItemStatus.statusId) && "ORDER_APPROVED" == (orderHeader.statusId)) && security.hasEntityPermission("ORDERMGR", "_UPDATE", session)>
                                                <div>
                                                    <a href="javascript:document.OrderApproveOrderItem_${orderItem.orderItemSeqId?default("")}.submit()" class="buttontext">${uiLabelMap.OrderApproveItem}</a>
                                                    <form name="OrderApproveOrderItem_${orderItem.orderItemSeqId?default("")}" method="post" action="<@ofbizUrl>changeOrderItemStatus</@ofbizUrl>">
                                                        <input type="hidden" name="statusId" value="ITEM_APPROVED"/>
                                                        <input type="hidden" name="orderId" value="${orderId!}"/>
                                                        <input type="hidden" name="orderItemSeqId" value="${orderItem.orderItemSeqId!}"/>
                                                    </form>
                                                </div>
                                            </#if>
                                            <#assign orderItemStatuses = orderReadHelper.getOrderItemStatuses(orderItem)>
                                            <#list orderItemStatuses as orderItemStatus>
                                                <#assign loopStatusItem = orderItemStatus.getRelatedOne("StatusItem", false)>
                                                <div><#if orderItemStatus.statusDatetime?has_content>${Static["org.apache.ofbiz.base.util.UtilFormatOut"].formatDateTime(orderItemStatus.statusDatetime, "", locale, timeZone)!}&nbsp;&nbsp;</#if>${loopStatusItem.get("description",locale)?default(orderItemStatus.statusId)}</div>
                                            </#list>
                                        </div>
                                    </div>
                                    <#assign returns = orderItem.getRelated("ReturnItem", null, null, false)!>
                                    <#if returns?has_content>
                                        <#list returns as returnItem>
                                            <#assign returnHeader = returnItem.getRelatedOne("ReturnHeader", false)>
                                            <#if returnHeader.statusId != "RETURN_CANCELLED">
                                                <font color="red">${uiLabelMap.OrderReturned}</font>
                                                ${uiLabelMap.CommonNbr}<a href="<@ofbizUrl>returnMain?returnId=${returnItem.returnId}</@ofbizUrl>" class="buttontext">${returnItem.returnId}</a>
                                            </#if>
                                        </#list>
                                    </#if>
                                </td>
                                <#-- QUANTITY -->
                                <td valign="top" nowrap="nowrap">
                                    <div class="screenlet order-item-quantity">
                                        <div class="screenlet-body">
                                            <table>
                                                <tr valign="top">
                                                    <#assign shippedQuantity = orderReadHelper.getItemShippedQuantity(orderItem)>
                                                    <#assign shipmentReceipts = EntityQuery.use(delegator).from("ShipmentReceipt").where("orderId", orderHeader.getString("orderId")!, "orderItemSeqId", orderItem.orderItemSeqId!).queryList()!/>
                                                    <#assign totalReceived = 0.0>
                                                    <#if shipmentReceipts?? && shipmentReceipts?has_content>
                                                        <#list shipmentReceipts as shipmentReceipt>
                                                            <#if shipmentReceipt.quantityAccepted?? && shipmentReceipt.quantityAccepted?has_content>
                                                                <#assign  quantityAccepted = shipmentReceipt.quantityAccepted>
                                                                <#assign totalReceived = quantityAccepted + totalReceived>
                                                            </#if>
                                                            <#if shipmentReceipt.quantityRejected?? && shipmentReceipt.quantityRejected?has_content>
                                                                <#assign  quantityRejected = shipmentReceipt.quantityRejected>
                                                                <#assign totalReceived = quantityRejected + totalReceived>
                                                            </#if>
                                                        </#list>
                                                    </#if>
                                                    <#if product?has_content && product.productTypeId?has_content && "SERVICE" == product.productTypeId && "ITEM_COMPLETED" == currentItemStatus.statusId>
                                                        <#assign shippedQuantity = orderItem.quantity?default(0)/>
                                                        <#assign totalReceived = orderItem.quantity?default(0)>
                                                    </#if>
                                                    <#if "PURCHASE_ORDER" == orderHeader.orderTypeId>
                                                        <#assign remainingQuantity = ((orderItem.quantity?default(0) - orderItem.cancelQuantity?default(0)) - totalReceived?double)>
                                                    <#else>
                                                        <#assign remainingQuantity = ((orderItem.quantity?default(0) - orderItem.cancelQuantity?default(0)) - shippedQuantity?double)>
                                                    </#if>
                                                    <#-- to compute shortfall amount, sum up the orderItemShipGrpInvRes.quantityNotAvailable -->
                                                    <#assign shortfalledQuantity = 0/>
                                                    <#list orderItemShipGrpInvResList as orderItemShipGrpInvRes>
                                                        <#if (orderItemShipGrpInvRes.quantityNotAvailable?has_content && orderItemShipGrpInvRes.quantityNotAvailable > 0)>
                                                            <#assign shortfalledQuantity = shortfalledQuantity + orderItemShipGrpInvRes.quantityNotAvailable/>
                                                        </#if>
                                                    </#list>
                                                    <td><b>${uiLabelMap.OrderOrdered}</b></td>
                                                    <td>${orderItem.quantity?default(0)?string.number}</td>
                                                    <td><b>${uiLabelMap.OrderShipRequest}</b></td>
                                                    <td>${orderReadHelper.getItemReservedQuantity(orderItem)}</td>
                                                </tr>
                                                <tr valign="top">
                                                    <td><b>${uiLabelMap.OrderCancelled}</b></td>
                                                    <td>${orderItem.cancelQuantity?default(0)?string.number}</td>
                                                    <#if "SALES_ORDER" == orderHeader.orderTypeId>
                                                        <#if pickedQty gt 0 && "ORDER_APPROVED" == orderHeader.statusId>
                                                            <td><font color="red"><b>${uiLabelMap.OrderQtyPicked}</b></font></td>
                                                            <td><font color="red">${pickedQty?default(0)?string.number}</font></td>
                                                        <#else>
                                                            <td><b>${uiLabelMap.OrderQtyPicked}</b></td>
                                                            <td>${pickedQty?default(0)?string.number}</td>
                                                        </#if>
                                                    <#else>
                                                        <td>&nbsp;</td>
                                                        <td>&nbsp;</td>
                                                    </#if>
                                                </tr>
                                                <tr valign="top">
                                                    <td><b>${uiLabelMap.OrderRemaining}</b></td>
                                                    <td>${remainingQuantity}</td>
                                                    <#if "PURCHASE_ORDER" == orderHeader.orderTypeId>
                                                        <td><b>${uiLabelMap.OrderPlannedInReceive}</b></td>
                                                        <td>${totalReceived}</td>
                                                    <#else>
                                                        <td><b>${uiLabelMap.OrderQtyShipped}</b></td>
                                                        <td>${shippedQuantity}</td>
                                                    </#if>
                                                </tr>
                                                <tr valign="top">
                                                    <td><b>${uiLabelMap.OrderShortfalled}</b></td>
                                                    <td>${shortfalledQuantity}</td>
                                                    <td><b>${uiLabelMap.OrderOutstanding}</b></td>
                                                    <td>
                                                        <#-- Make sure digital goods without shipments don't always remainn "outstanding": if item is completed, it must have no outstanding quantity.  -->
                                                        <#if (orderItem.statusId?has_content) && ("ITEM_COMPLETED" == orderItem.statusId)>
                                                            0
                                                        <#elseif "PURCHASE_ORDER" == orderHeader.orderTypeId>
                                                            ${(orderItem.quantity?default(0) - orderItem.cancelQuantity?default(0)) - totalReceived?double}
                                                        <#elseif "SALES_ORDER" == orderHeader.orderTypeId>
                                                            ${(orderItem.quantity?default(0) - orderItem.cancelQuantity?default(0)) - shippedQuantity?double}
                                                        </#if>
                                                    </td>
                                                </tr>
                                                <tr valign="top">
                                                    <td><b>${uiLabelMap.OrderInvoiced}</b></td>
                                                    <td>${orderReadHelper.getOrderItemInvoicedQuantity(orderItem)}</td>
                                                    <td><b>${uiLabelMap.OrderReturned}</b></td>
                                                    <td>${returnQuantityMap.get(orderItem.orderItemSeqId)?default(0)}</td>
                                                </tr>
                                            </table>
                                        </div>
                                    </div>
                                </td>
                                <td align="right" valign="top" nowrap="nowrap">
                                    <@ofbizCurrency amount=orderItem.unitPrice isoCode=currencyUomId/>
                                    / <@ofbizCurrency amount=orderItem.unitListPrice isoCode=currencyUomId/>
                                </td>
                                <td align="right" valign="top" nowrap="nowrap">
                                    <@ofbizCurrency amount=Static["org.apache.ofbiz.order.order.OrderReadHelper"].getOrderItemAdjustmentsTotal(orderItem, orderAdjustments, true, false, false) isoCode=currencyUomId/>
                                </td>
                                <td align="right" valign="top" nowrap="nowrap">
                                    <#if orderItem.statusId != "ITEM_CANCELLED">
                                        <@ofbizCurrency amount=Static["org.apache.ofbiz.order.order.OrderReadHelper"].getOrderItemSubTotal(orderItem, orderAdjustments) isoCode=currencyUomId/>
                                    <#else>
                                        <@ofbizCurrency amount=0.00 isoCode=currencyUomId/>
                                    </#if>
                                </td>
                                <td>&nbsp;</td>
                            </#if>
                        </tr>
                        <#-- show info from workeffort -->
                        <#assign workOrderItemFulfillments = orderItem.getRelated("WorkOrderItemFulfillment", null, null, false)!>
                        <#if workOrderItemFulfillments?has_content>
                            <#list workOrderItemFulfillments as workOrderItemFulfillment>
                                <#assign workEffort = workOrderItemFulfillment.getRelatedOne("WorkEffort", true)>
                                <tr<#if "1" == itemClass> class="alternate-row"</#if>>
                                    <td>&nbsp;</td>
                                    <td colspan="6">
                                        <#if orderItem.orderItemTypeId != "RENTAL_ORDER_ITEM">
                                            <span class="label">${uiLabelMap.ManufacturingProductionRun}</span>
                                            <a href="<@ofbizUrl controlPath="/manufacturing/control">ShowProductionRun?productionRunId=${workEffort.workEffortId}</@ofbizUrl>"
                                                class="buttontext">${workEffort.workEffortId}</a>
                                            ${uiLabelMap.OrderCurrentStatus}
                                            ${(delegator.findOne("StatusItem", Static["org.apache.ofbiz.base.util.UtilMisc"].toMap("statusId", workEffort.getString("currentStatusId")), true).get("description",locale))!}
                                        <#else>
                                            ${uiLabelMap.CommonFrom}
                                            : <#if workEffort.estimatedStartDate?has_content>${Static["org.apache.ofbiz.base.util.UtilFormatOut"].formatDate(workEffort.estimatedStartDate, "", locale, timeZone)!}</#if> ${uiLabelMap.CommonTo}
                                            : <#if workEffort.estimatedCompletionDate?has_content>${Static["org.apache.ofbiz.base.util.UtilFormatOut"].formatDate(workEffort.estimatedCompletionDate, "", locale, timeZone)!}</#if> ${uiLabelMap.OrderNumberOfPersons}
                                            : ${workEffort.reservPersons?default("")}
                                        </#if>
                                    </td>
                                </tr>
                            </#list>
                        </#if>
                        <#-- show linked order lines -->
                        <#assign linkedOrderItemsTo = EntityQuery.use(delegator).from("OrderItemAssoc").where("orderId", orderItem.getString("orderId"), "orderItemSeqId", orderItem.getString("orderItemSeqId")).queryList()!>
                        <#assign linkedOrderItemsFrom = EntityQuery.use(delegator).from("OrderItemAssoc").where("toOrderId", orderItem.getString("orderId")!, "toOrderItemSeqId", orderItem.getString("orderItemSeqId")!).queryList()!>
                        <#if linkedOrderItemsTo?has_content>
                            <#list linkedOrderItemsTo as linkedOrderItem>
                                <#assign linkedOrderId = linkedOrderItem.toOrderId>
                                <#assign linkedOrderItemSeqId = linkedOrderItem.toOrderItemSeqId>
                                <#assign linkedOrderItemValue = linkedOrderItem.getRelatedOne("ToOrderItem", false)>
                                <#assign linkedOrderItemValueStatus = linkedOrderItemValue.getRelatedOne("StatusItem", false)>
                                <#assign description = linkedOrderItem.getRelatedOne("OrderItemAssocType", false).getString("description")/>
                                <tr<#if "1" == itemClass> class="alternate-row"</#if>>
                                    <td>&nbsp;</td>
                                    <td colspan="6">
                                        <span class="label">${uiLabelMap.OrderLinkedToOrderItem}</span>&nbsp;(${description!})
                                        <a href="<@ofbizUrl controlPath="/ordermgr/control">orderview?orderId=${linkedOrderId}</@ofbizUrl>"
                                           class="buttontext">${linkedOrderId}/${linkedOrderItemSeqId}</a>&nbsp;${linkedOrderItemValueStatus.description!}
                                    </td>
                                </tr>
                            </#list>
                        </#if>
                        <#if linkedOrderItemsFrom?has_content>
                            <#list linkedOrderItemsFrom as linkedOrderItem>
                                <#assign linkedOrderId = linkedOrderItem.orderId>
                                <#assign linkedOrderItemSeqId = linkedOrderItem.orderItemSeqId>
                                <#assign linkedOrderItemValue = linkedOrderItem.getRelatedOne("FromOrderItem", false)>
                                <#assign linkedOrderItemValueStatus = linkedOrderItemValue.getRelatedOne("StatusItem", false)>
                                <#assign description = linkedOrderItem.getRelatedOne("OrderItemAssocType", false).getString("description")/>
                                <tr<#if "1" == itemClass> class="alternate-row"</#if>>
                                    <td>&nbsp;</td>
                                    <td colspan="6">
                                        <span class="label">${uiLabelMap.OrderLinkedFromOrderItem}</span>&nbsp;(${description!})
                                        <a href="<@ofbizUrl controlPath="/ordermgr/control">orderview?orderId=${linkedOrderId}</@ofbizUrl>"
                                           class="buttontext">${linkedOrderId}/${linkedOrderItemSeqId}</a>&nbsp;${linkedOrderItemValueStatus.description!}
                                    </td>
                                </tr>
                            </#list>
                        </#if>
                        <#-- show linked requirements -->
                        <#assign linkedRequirements = orderItem.getRelated("OrderRequirementCommitment", null, null, false)!>
                        <#if linkedRequirements?has_content>
                            <#list linkedRequirements as linkedRequirement>
                                <tr<#if "1" == itemClass> class="alternate-row"</#if>>
                                    <td>&nbsp;</td>
                                    <td colspan="6">
                                        <span class="label">${uiLabelMap.OrderLinkedToRequirement}</span>&nbsp;
                                        <a href="<@ofbizUrl>EditRequirement?requirementId=${linkedRequirement.requirementId}</@ofbizUrl>"
                                           class="buttontext">${linkedRequirement.requirementId}</a>&nbsp;
                                    </td>
                                </tr>
                            </#list>
                        </#if>
                        <#-- show linked quote -->
                        <#assign linkedQuote = orderItem.getRelatedOne("QuoteItem", true)!>
                        <#if linkedQuote?has_content>
                            <tr<#if "1" == itemClass> class="alternate-row"</#if>>
                                <td>&nbsp;</td>
                                <td colspan="6">
                                    <span class="label">${uiLabelMap.OrderLinkedToQuote}</span>&nbsp;
                                    <a href="<@ofbizUrl>EditQuoteItem?quoteId=${linkedQuote.quoteId}&amp;quoteItemSeqId=${linkedQuote.quoteItemSeqId}</@ofbizUrl>"
                                       class="buttontext">${linkedQuote.quoteId}-${linkedQuote.quoteItemSeqId}</a>&nbsp;
                                </td>
                            </tr>
                        </#if>
                        <#-- now show adjustment details per line item -->
                        <#assign orderItemAdjustments = Static["org.apache.ofbiz.order.order.OrderReadHelper"].getOrderItemAdjustmentList(orderItem, orderAdjustments)>
                        <#if orderItemAdjustments?? && orderItemAdjustments?has_content>
                            <#list orderItemAdjustments as orderItemAdjustment>
                                <#assign adjustmentType = orderItemAdjustment.getRelatedOne("OrderAdjustmentType", true)>
                                <tr<#if "1" == itemClass> class="alternate-row"</#if>>
                                    <td align="right" colspan="2">
                                        <span class="label">${uiLabelMap.OrderAdjustment}</span>&nbsp;${adjustmentType.get("description",locale)}
                                        ${StringUtil.wrapString(orderItemAdjustment.get("description",locale)!)}
                                        <#if orderItemAdjustment.comments?has_content>
                                            (${orderItemAdjustment.comments?default("")})
                                        </#if>
                                        <#if orderItemAdjustment.productPromoId?has_content>
                                            <a href="<@ofbizUrl controlPath="/catalog/control">EditProductPromo?productPromoId=${orderItemAdjustment.productPromoId}</@ofbizUrl>"
                                                >${orderItemAdjustment.getRelatedOne("ProductPromo", false).getString("promoName")}</a>
                                        </#if>
                                        <#if "SALES_TAX" == orderItemAdjustment.orderAdjustmentTypeId>
                                            <#if orderItemAdjustment.primaryGeoId?has_content>
                                                <#assign primaryGeo = orderItemAdjustment.getRelatedOne("PrimaryGeo", true)/>
                                                <#if primaryGeo.geoName?has_content>
                                                    <span class="label">${uiLabelMap.OrderJurisdiction}</span>&nbsp;${primaryGeo.geoName} [${primaryGeo.abbreviation!}]
                                                </#if>
                                                <#if orderItemAdjustment.secondaryGeoId?has_content>
                                                    <#assign secondaryGeo = orderItemAdjustment.getRelatedOne("SecondaryGeo", true)/>
                                                    <span class="label">${uiLabelMap.CommonIn}</span>&nbsp;${secondaryGeo.geoName} [${secondaryGeo.abbreviation!}])
                                                </#if>
                                            </#if>
                                            <#if orderItemAdjustment.sourcePercentage??>
                                                <span class="label">${uiLabelMap.OrderRate}</span>&nbsp;${orderItemAdjustment.sourcePercentage?string("0.######")}
                                            </#if>
                                            <#if orderItemAdjustment.customerReferenceId?has_content>
                                                <span class="label">${uiLabelMap.OrderCustomerTaxId}</span>&nbsp;${orderItemAdjustment.customerReferenceId}
                                            </#if>
                                            <#if orderItemAdjustment.exemptAmount??>
                                                <span class="label">${uiLabelMap.OrderExemptAmount}</span>&nbsp;${orderItemAdjustment.exemptAmount}
                                            </#if>
                                        </#if>
                                    </td>
                                    <td>&nbsp;</td>
                                    <td>&nbsp;</td>
                                    <td align="right">
                                        <#if orderItemAdjustment.amountAlreadyIncluded?has_content>
                                            <@ofbizCurrency amount=orderItemAdjustment.amountAlreadyIncluded isoCode=currencyUomId/>
                                        <#else>
                                            <@ofbizCurrency amount=Static["org.apache.ofbiz.order.order.OrderReadHelper"].calcItemAdjustment(orderItemAdjustment, orderItem) isoCode=currencyUomId/>
                                        </#if>
                                    </td>
                                    <td colspan="2">&nbsp;</td>
                                </tr>
                            </#list>
                        </#if>
                        <#-- now show price info per line item -->
                        <#assign orderItemPriceInfos = orderReadHelper.getOrderItemPriceInfos(orderItem)>
                        <#if orderItemPriceInfos?? && orderItemPriceInfos?has_content>
                            <tr<#if "1" == itemClass> class="alternate-row"</#if>>
                                <td colspan="7">&nbsp;</td>
                            </tr>
                            <#list orderItemPriceInfos as orderItemPriceInfo>
                                <tr<#if "1" == itemClass> class="alternate-row"</#if>>
                                    <td align="right" colspan="2">
                                        <span class="label">${uiLabelMap.ProductPriceRuleNameId}</span>&nbsp;[${orderItemPriceInfo.productPriceRuleId!}:${orderItemPriceInfo.productPriceActionSeqId!}]
                                        ${orderItemPriceInfo.description!}
                                    </td>
                                    <td>&nbsp;</td>
                                    <td align="right">
                                        <@ofbizCurrency amount=orderItemPriceInfo.modifyAmount isoCode=currencyUomId/>
                                    </td>
                                    <td colspan="3">&nbsp;</td>
                                </tr>
                            </#list>
                        </#if>
                        <#-- now show survey information per line item -->
                        <#assign orderItemSurveyResponses = Static["org.apache.ofbiz.order.order.OrderReadHelper"].getOrderItemSurveyResponse(orderItem)>
                        <#if orderItemSurveyResponses?? && orderItemSurveyResponses?has_content>
                            <#list orderItemSurveyResponses as survey>
                                <tr<#if "1" == itemClass> class="alternate-row"</#if>>
                                    <td align="right" colspan="2">
                                        <span class="label">${uiLabelMap.CommonSurveys}</span>&nbsp;
                                        <a href="<@ofbizUrl controlPath="/content/control">ViewSurveyResponses?surveyResponseId=${survey.surveyResponseId}&amp;surveyId=${survey.surveyId}</@ofbizUrl>"
                                           class="buttontext">${survey.surveyId}</a>
                                    </td>
                                    <td colspan="5">&nbsp;</td>
                                </tr>
                            </#list>
                        </#if>
                        <#-- display the ship estimated/before/after dates -->
                        <#if orderItem.estimatedShipDate??>
                            <tr<#if "1" == itemClass> class="alternate-row"</#if>>
                                <td align="right" colspan="2">
                                    <span class="label">${uiLabelMap.OrderEstimatedShipDate}</span>&nbsp;${Static["org.apache.ofbiz.base.util.UtilFormatOut"].formatDate(orderItem.estimatedShipDate, "", locale, timeZone)!}
                                </td>
                                <td colspan="5">&nbsp;</td>
                            </tr>
                        </#if>
                        <#if orderItem.estimatedDeliveryDate??>
                            <tr<#if "1" == itemClass> class="alternate-row"</#if>>
                                <td align="right" colspan="2">
                                    <span class="label">${uiLabelMap.OrderOrderQuoteEstimatedDeliveryDate}</span>&nbsp;${Static["org.apache.ofbiz.base.util.UtilFormatOut"].formatDate(orderItem.estimatedDeliveryDate, "", locale, timeZone)!}
                                </td>
                                <td colspan="5">&nbsp;</td>
                            </tr>
                        </#if>
                        <#if orderItem.shipAfterDate??>
                            <tr<#if "1" == itemClass> class="alternate-row"</#if>>
                                <td align="right" colspan="2">
                                    <span class="label">${uiLabelMap.OrderShipAfterDate}</span>&nbsp;${Static["org.apache.ofbiz.base.util.UtilFormatOut"].formatDate(orderItem.shipAfterDate, "", locale, timeZone)!}
                                </td>
                                <td colspan="5">&nbsp;</td>
                            </tr>
                        </#if>
                        <#if orderItem.shipBeforeDate??>
                            <tr<#if "1" == itemClass> class="alternate-row"</#if>>
                                <td align="right" colspan="2">
                                    <span class="label">${uiLabelMap.OrderShipBeforeDate}</span>&nbsp;${Static["org.apache.ofbiz.base.util.UtilFormatOut"].formatDate(orderItem.shipBeforeDate, "", locale, timeZone)!}
                                </td>
                                <td colspan="5">&nbsp;</td>
                            </tr>
                        </#if>
                        <#-- now show ship group info per line item -->
                        <#assign orderItemShipGroupAssocs = orderItem.getRelated("OrderItemShipGroupAssoc", null, null, false)!>
                        <#if orderItemShipGroupAssocs?has_content>
                            <#list orderItemShipGroupAssocs as shipGroupAssoc>
                                <#assign shipGroup = shipGroupAssoc.getRelatedOne("OrderItemShipGroup", false)>
                                <#assign shipGroupAddress = shipGroup.getRelatedOne("PostalAddress", false)!>
                                <tr<#if "1" == itemClass> class="alternate-row"</#if>>
                                    <td align="right" colspan="2">
                                        <span class="label">${uiLabelMap.OrderShipGroup}</span>&nbsp;[${shipGroup.shipGroupSeqId}]
                                        ${shipGroupAddress.address1?default("${uiLabelMap.OrderNotShipped}")}
                                    </td>
                                    <td align="center">
                                        ${shipGroupAssoc.quantity?string.number}&nbsp;
                                    </td>
                                    <td colspan="4">&nbsp;</td>
                                </tr>
                            </#list>
                        </#if>
                        <#-- now show inventory reservation info per line item -->
                        <#if orderItemShipGrpInvResList?? && orderItemShipGrpInvResList?has_content>
                            <#list orderItemShipGrpInvResList as orderItemShipGrpInvRes>
                                <tr<#if "1" == itemClass> class="alternate-row"</#if>>
                                    <td align="right" colspan="2">
                                        <span class="label">${uiLabelMap.CommonInventory}</span>&nbsp;
                                        <a href="<@ofbizUrl controlPath="/facility/control">EditInventoryItem?inventoryItemId=${orderItemShipGrpInvRes.inventoryItemId}</@ofbizUrl>"
                                           class="buttontext">${orderItemShipGrpInvRes.inventoryItemId}</a>
                                        <span class="label">${uiLabelMap.OrderShipGroup}</span>&nbsp;${orderItemShipGrpInvRes.shipGroupSeqId}
                                    </td>
                                    <td align="center">
                                        ${orderItemShipGrpInvRes.quantity?string.number}&nbsp;
                                    </td>
                                    <td>
                                        <#if (orderItemShipGrpInvRes.quantityNotAvailable?has_content && orderItemShipGrpInvRes.quantityNotAvailable > 0)>
                                            <span style="color: red;">
                                                [${orderItemShipGrpInvRes.quantityNotAvailable?string.number}&nbsp;${uiLabelMap.OrderBackOrdered}]
                                            </span>
                                            <#--<a href="<@ofbizUrl>balanceInventoryItems?inventoryItemId=${orderItemShipGrpInvRes.inventoryItemId}&amp;orderId=${orderId}&amp;priorityOrderId=${orderId}&amp;priorityOrderItemSeqId=${orderItemShipGrpInvRes.orderItemSeqId}</@ofbizUrl>" class="buttontext" style="font-size: xx-small;">Raise Priority</a> -->
                                        </#if>
                                        &nbsp;
                                    </td>
                                    <td colspan="3">&nbsp;</td>
                                </tr>
                            </#list>
                        </#if>
                        <#-- now show planned shipment info per line item -->
                        <#assign orderShipments = orderItem.getRelated("OrderShipment", null, null, false)!>
                        <#if orderShipments?has_content>
                            <#list orderShipments as orderShipment>
                                <tr<#if "1" == itemClass> class="alternate-row"</#if>>
                                    <td align="right" colspan="2">
                                        <span class="label">${uiLabelMap.OrderPlannedInShipment}</span>&nbsp;<a
                                            target="facility"
                                            href="<@ofbizUrl controlPath="/facility/control">ViewShipment?shipmentId=${orderShipment.shipmentId}</@ofbizUrl>"
                                            class="buttontext">${orderShipment.shipmentId}</a>: ${orderShipment.shipmentItemSeqId}
                                    </td>
                                    <td align="center">
                                        ${orderShipment.quantity?string.number}&nbsp;
                                    </td>
                                    <td colspan="4">&nbsp;</td>
                                </tr>
                            </#list>
                        </#if>
                        <#-- now show item issuances (shipment) per line item -->
                        <#assign itemIssuances = itemIssuancesPerItem.get(orderItem.get("orderItemSeqId"))!>
                        <#if itemIssuances?has_content>
                            <#list itemIssuances as itemIssuance>
                            <tr<#if "1" == itemClass> class="alternate-row"</#if>>
                                <td align="right" colspan="2">
                                    <#if itemIssuance.shipmentId?has_content>
                                        <span class="label">${uiLabelMap.OrderIssuedToShipmentItem}</span>&nbsp;
                                        <a target="facility"
                                           href="<@ofbizUrl controlPath="/facility/control">ViewShipment?shipmentId=${itemIssuance.shipmentId}</@ofbizUrl>"
                                           class="buttontext">${itemIssuance.shipmentId}</a>: ${itemIssuance.shipmentItemSeqId!}
                                    <#else>
                                        <span class="label">${uiLabelMap.OrderIssuedWithoutShipment}</span>
                                    </#if>
                                </td>
                                <td align="center">
                                    ${itemIssuance.quantity?default(0) - itemIssuance.cancelQuantity?default(0)}&nbsp;
                                </td>
                                <td colspan="4">&nbsp;</td>
                            </tr>
                            </#list>
                        </#if>
                        <#-- now show item issuances (inventory item) per line item -->
                        <#if itemIssuances?has_content>
                            <#list itemIssuances as itemIssuance>
                                <tr<#if "1" == itemClass> class="alternate-row"</#if>>
                                    <td align="right" colspan="2">
                                        <#if itemIssuance.inventoryItemId?has_content>
                                            <#assign inventoryItem = itemIssuance.getRelatedOne("InventoryItem", false)/>
                                            <span class="label">${uiLabelMap.CommonInventory}</span>
                                            <a href="<@ofbizUrl controlPath="/facility/control">EditInventoryItem?inventoryItemId=${itemIssuance.inventoryItemId}</@ofbizUrl>"
                                               class="buttontext">${itemIssuance.inventoryItemId}</a>
                                            <span class="label">${uiLabelMap.OrderShipGroup}</span>&nbsp;${itemIssuance.shipGroupSeqId!}
                                            <#if (inventoryItem.serialNumber?has_content)>
                                                <br />
                                                <span class="label">${uiLabelMap.ProductSerialNumber}</span>&nbsp;${inventoryItem.serialNumber}&nbsp;
                                            </#if>
                                        </#if>
                                    </td>
                                    <td align="center">
                                        ${itemIssuance.quantity?default(0) - itemIssuance.cancelQuantity?default(0)}
                                    </td>
                                    <td colspan="4">&nbsp;</td>
                                </tr>
                            </#list>
                        </#if>
                        <#-- now show shipment receipts per line item -->
                        <#assign shipmentReceipts = orderItem.getRelated("ShipmentReceipt", null, null, false)!>
                        <#if shipmentReceipts?has_content>
                            <#list shipmentReceipts as shipmentReceipt>
                                <tr<#if "1" == itemClass> class="alternate-row"</#if>>
                                    <td align="right" colspan="2">
                                        <#if shipmentReceipt.shipmentId?has_content>
                                            <span class="label">${uiLabelMap.OrderShipmentReceived}</span>&nbsp;
                                            <a target="facility"
                                               href="<@ofbizUrl controlPath="/facility/control">ViewShipment?shipmentId=${shipmentReceipt.shipmentId}</@ofbizUrl>"
                                               class="buttontext">${shipmentReceipt.shipmentId}</a>:${shipmentReceipt.shipmentItemSeqId!}
                                        </#if>
                                        &nbsp;<#if shipmentReceipt.datetimeReceived?has_content>${Static["org.apache.ofbiz.base.util.UtilFormatOut"].formatDateTime(shipmentReceipt.datetimeReceived, "", locale, timeZone)!}</#if>&nbsp;
                                        <span class="label">${uiLabelMap.CommonInventory}</span>&nbsp;
                                        <a href="<@ofbizUrl controlPath="/facility/control">EditInventoryItem?inventoryItemId=${shipmentReceipt.inventoryItemId}</@ofbizUrl>"
                                           class="buttontext">${shipmentReceipt.inventoryItemId}</a>
                                    </td>
                                    <td align="center">
                                        ${shipmentReceipt.quantityAccepted?string.number}&nbsp;/&nbsp;${shipmentReceipt.quantityRejected?default(0)?string.number}
                                    </td>
                                    <td colspan="4">&nbsp;</td>
                                </tr>
                            </#list>
                        </#if>
                        <#if orderItem.comments?has_content>
                          <tr<#if "1" == itemClass> class="alternate-row"</#if>>
                            <td>&nbsp;</td>
                            <td>
                              <div class= "screenlet">
                                <div class = "screenlet-body">
                                  <table>
                                    <tr>
                                      <td align="right">
                                        <span class="label">${uiLabelMap.CommonComments}</span>
                                      </td>
                                      <td align="">
                                        <span class="label">${uiLabelMap.CommonCurrent}:</span>&nbsp;${orderItem.comments}
                                        <#assign orderItemSeqId = orderItem.orderItemSeqId!>
                                        <#if comments?has_content>
                                          <hr/>
                                          <#list comments as comm>
                                            <#if comm.orderItemSeqId?has_content && orderItemSeqId?has_content && comm.orderItemSeqId == orderItemSeqId>
                                              <#if comm.changeComments?has_content>
                                                <div>
                                                ${(comm.changeComments)!}
                                                &nbsp;
                                                <#if comm.changeDatetime?has_content>${Static["org.apache.ofbiz.base.util.UtilFormatOut"].formatDateTime(comm.changeDatetime, "", locale, timeZone)?default("0000-00-00 00:00:00")}</#if>  &nbsp;  ${uiLabelMap.CommonBy} -  [${(comm.changeUserLogin)!}]
                                                </div>
                                              </#if>
                                            </#if>
                                          </#list>
                                        </#if>
                                      </td>
                                    </tr>
                                  </table>
                                </div>
                              </div>
                            </td>
                            <td></td>
                            <td></td>
                            <td></td>
                            <td></td>
                            <td></td>
                          </tr>
                        </#if>
                        <#if "2" == itemClass>
                            <#assign itemClass = "1">
                        <#else>
                            <#assign itemClass = "2">
                        </#if>
                    </#list>
                </#if>
                <tr><td colspan="7"><hr /></td></tr>
                <#list orderHeaderAdjustments as orderHeaderAdjustment>
                    <#assign adjustmentType = orderHeaderAdjustment.getRelatedOne("OrderAdjustmentType", false)>
                    <#assign adjustmentAmount = Static["org.apache.ofbiz.order.order.OrderReadHelper"].calcOrderAdjustment(orderHeaderAdjustment, orderSubTotal)>
                    <#if adjustmentAmount != 0>
                        <tr>
                            <td align="right" colspan="5">
                                <#if orderHeaderAdjustment.comments?has_content>${orderHeaderAdjustment.comments} - </#if>
                                <#if orderHeaderAdjustment.description?has_content>${orderHeaderAdjustment.description} - </#if>
                                <span class="label">${adjustmentType.get("description", locale)}</span>
                            </td>
                            <td align="right" nowrap="nowrap">
                                <@ofbizCurrency amount=adjustmentAmount isoCode=currencyUomId/>
                            </td>
                            <td>&nbsp;</td>
                        </tr>
                    </#if>
                </#list>
                <#-- subtotal -->
                <tr>
                    <td colspan="1"></td>
                    <td colspan="6"><hr /></td>
                </tr>
                <tr>
                    <td align="right" colspan="5">
                        <span class="label">${uiLabelMap.OrderItemsSubTotal}</span>
                    </td>
                    <td align="right" nowrap="nowrap">
                        <@ofbizCurrency amount=orderSubTotal isoCode=currencyUomId/>
                    </td>
                    <td>&nbsp;</td>
                </tr>
                <#-- other adjustments -->
                <tr>
                    <td align="right" colspan="5">
                        <span class="label">${uiLabelMap.OrderTotalOtherOrderAdjustments}</span>
                    </td>
                    <td align="right" nowrap="nowrap">
                        <@ofbizCurrency amount=otherAdjAmount isoCode=currencyUomId/>
                    </td>
                    <td>&nbsp;</td>
                </tr>
                <#-- shipping adjustments -->
                <tr>
                    <td align="right" colspan="5">
                        <span class="label">${uiLabelMap.OrderTotalShippingAndHandling}</span>
                    </td>
                    <td align="right" nowrap="nowrap">
                        <@ofbizCurrency amount=shippingAmount isoCode=currencyUomId/>
                    </td>
                    <td>&nbsp;</td>
                </tr>
                <#-- tax adjustments -->
                <tr>
                    <td align="right" colspan="5">
                        <span class="label">${uiLabelMap.OrderTotalSalesTax}</span>
                    </td>
                    <td align="right" nowrap="nowrap">
                        <@ofbizCurrency amount=taxAmount isoCode=currencyUomId/>
                    </td>
                    <td>&nbsp;</td>
                </tr>
                <#-- grand total -->
                <tr>
                    <td align="right" colspan="5">
                        <span class="label">${uiLabelMap.OrderTotalDue}</span>
                    </td>
                    <td align="right" nowrap="nowrap">
                        <@ofbizCurrency amount=grandTotal isoCode=currencyUomId/>
                    </td>
                    <td>&nbsp;</td>
                </tr>
            </table>
        </div>
    </div>
</#if>
