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
        <br class="clear"/>
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
            <tr><td colspan="7"><font color="red">${uiLabelMap.checkhelper_sales_order_lines_lookup_failed}</font></td></tr>
          <#else>
            <#assign itemClass = "2">
            <#list orderItemList as orderItem>
              <#assign orderItemContentWrapper = Static["org.ofbiz.order.order.OrderContentWrapper"].makeOrderContentWrapper(orderItem, request)>
              <#assign orderItemShipGrpInvResList = orderReadHelper.getOrderItemShipGrpInvResList(orderItem)>
              <#if orderHeader.orderTypeId == "SALES_ORDER"><#assign pickedQty = orderReadHelper.getItemPickedQuantityBd(orderItem)></#if>
              <tr<#if itemClass == "1"> class="alternate-row"</#if>>
                <#assign orderItemType = orderItem.getRelatedOne("OrderItemType")?if_exists>
                <#assign productId = orderItem.productId?if_exists>
                <#if productId?exists && productId == "shoppingcart.CommentLine">
                  <td colspan="7" valign="top" class="label"> &gt;&gt; ${orderItem.itemDescription}</td>
                <#else>
                  <td colspan="7">
                    <div class="order-item-description">
                      <#if productId?exists>
                        ${orderItem.productId?default("N/A")} - ${orderItem.itemDescription?if_exists}
                        <#if (product.salesDiscontinuationDate)?exists && Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp().after(product.salesDiscontinuationDate)>
                          <br /><span style="color: red;">${uiLabelMap.OrderItemDiscontinued}: ${product.salesDiscontinuationDate}</span>
                        </#if>
                      <#elseif orderItemType?exists>
                        ${orderItemType.description} - ${orderItem.itemDescription?if_exists}
                      <#else>
                        ${orderItem.itemDescription?if_exists}
                      </#if>
                    </div>
                      <div style="float:right;">
                        <a href="/catalog/control/EditProduct?productId=${productId}" class="buttontext" target="_blank">${uiLabelMap.ProductCatalog}</a>
                        <a href="/ecommerce/control/product?product_id=${productId}" class="buttontext" target="_blank">${uiLabelMap.OrderEcommerce}</a>
                        <#if orderItemContentWrapper.get("IMAGE_URL")?has_content>
                          <a href="<@ofbizUrl>viewimage?orderId=${orderId}&amp;orderItemSeqId=${orderItem.orderItemSeqId}&amp;orderContentTypeId=IMAGE_URL</@ofbizUrl>" target="_orderImage" class="buttontext">${uiLabelMap.OrderViewImage}</a>
                        </#if>
                      </div>
                  </td>
                </#if>
              </tr>
              <tr<#if itemClass == "1"> class="alternate-row"</#if>>
                <#if productId?exists && productId == "shoppingcart.CommentLine">
                  <td colspan="7" valign="top" class="label"> &gt;&gt; ${orderItem.itemDescription}</td>
                <#else>
                  <td valign="top">
                    <#if productId?has_content>
                      <#assign product = orderItem.getRelatedOneCache("Product")>
                    </#if>
                    <#if productId?exists>

                      <#-- INVENTORY -->
                      <#if (orderHeader.statusId != "ORDER_COMPLETED") && availableToPromiseMap?exists && quantityOnHandMap?exists && availableToPromiseMap.get(productId)?exists && quantityOnHandMap.get(productId)?exists>
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
                        <div class="screenlet order-item-inventory"><div class="screenlet-body">
                            <table cellspacing="0" cellpadding="0" border="0">
                              <tr><td style="text-align: right; padding-bottom: 10px;">
                                  <a class="buttontext" href="/catalog/control/EditProductInventoryItems?productId=${productId}&amp;showAllFacilities=Y&amp;externalLoginKey=${externalLoginKey}" target="_blank">${uiLabelMap.ProductInventory}</a>
                              </td><td>&nbsp;</td></tr>
                              <tr><td>${uiLabelMap.OrderRequiredForSO}</td>
                                <td style="padding-left: 15px; text-align: left;">${requiredQuantity}</td></tr>
                              <#if availableToPromiseByFacilityMap?exists && quantityOnHandByFacilityMap?exists && quantityOnHandByFacilityMap.get(productId)?exists && availableToPromiseByFacilityMap.get(productId)?exists>
                                <#assign atpQuantityByFacility = availableToPromiseByFacilityMap.get(productId)?default(0)>
                                <#assign qohQuantityByFacility = quantityOnHandByFacilityMap.get(productId)?default(0)>
                                <tr><td>${uiLabelMap.ProductInInventory} [${facility.facilityName?if_exists}] ${uiLabelMap.ProductQoh}</td>
                                  <td style="padding-left: 15px; text-align: left;">${qohQuantityByFacility} (${uiLabelMap.ProductAtp}: ${atpQuantityByFacility})</td></tr>
                              </#if>
                              <tr><td>${uiLabelMap.ProductInInventory} [${uiLabelMap.CommonAll} ${uiLabelMap.ProductFacilities}] ${uiLabelMap.ProductQoh}</td>
                                <td style="padding-left: 15px; text-align: left;">${qohQuantity} (${uiLabelMap.ProductAtp}: ${atpQuantity})</td></tr>
                              <#if (product != null) && (product.productTypeId != null) && Static["org.ofbiz.common.CommonWorkers"].hasParentType(delegator, "ProductType", "productTypeId", product.productTypeId, "parentTypeId", "MARKETING_PKG")>
                                <tr><td>${uiLabelMap.ProductMarketingPackageQOH}</td>
                                  <td style="padding-left: 15px; text-align: left;">${mktgPkgQOH} (${uiLabelMap.ProductAtp}: ${mktgPkgATP})</td></tr>
                              </#if>
                              <tr><td>${uiLabelMap.OrderOnOrder}</td>
                                <td style="padding-left: 15px; text-align: left;">${onOrderQuantity}</td></tr>
                              <tr><td>${uiLabelMap.OrderInProduction}</td>
                                <td style="padding-left: 15px; text-align: left;">${inProductionQuantity}</td></tr>
                              <tr><td>${uiLabelMap.OrderUnplanned}</td>
                                <td style="padding-left: 15px; text-align: left;">${unplannedQuantity}</td></tr>
                            </table>
                        </div></div>
                      </#if>
                    </#if>
                  </td>

                  <#-- now show status details per line item -->
                  <#assign currentItemStatus = orderItem.getRelatedOne("StatusItem")>
                  <td colspan="1" valign="top">
                    <div class="screenlet order-item-status-list<#if currentItemStatus.statusCode?has_content> ${currentItemStatus.statusCode}</#if>"><div class="screenlet-body">
                    <div class="current-status"><span class="label">${uiLabelMap.CommonCurrent}</span>&nbsp;${currentItemStatus.get("description",locale)?default(currentItemStatus.statusId)}</div>
                    <#assign orderItemStatuses = orderReadHelper.getOrderItemStatuses(orderItem)>
                    <#list orderItemStatuses as orderItemStatus>
                      <#assign loopStatusItem = orderItemStatus.getRelatedOne("StatusItem")>
                      <div>
                        ${(orderItemStatus.statusDatetime.toString())?if_exists}&nbsp;&nbsp;${loopStatusItem.get("description",locale)?default(orderItemStatus.statusId)}
                      </div>
                    </#list>
                    </div></div>
                    <#assign returns = orderItem.getRelated("ReturnItem")?if_exists>
                    <#if returns?has_content>
                      <#list returns as returnItem>
                        <#assign returnHeader = returnItem.getRelatedOne("ReturnHeader")>
                        <#if returnHeader.statusId != "RETURN_CANCELLED">
                          <div>
                            <font color="red">${uiLabelMap.OrderReturned}</font> #<a href="<@ofbizUrl>returnMain?returnId=${returnItem.returnId}</@ofbizUrl>" class="buttontext">${returnItem.returnId}</a>
                          </div>
                        </#if>
                      </#list>
                    </#if>
                  </td>
                  <#-- QUANTITY -->
                  <td align="right" valign="top" nowrap="nowrap">
                    <div class="screenlet order-item-quantity"><div class="screenlet-body"><table>
                      <tr valign="top">
                        <#assign shippedQuantity = orderReadHelper.getItemShippedQuantity(orderItem)>
                        <#assign shipmentReceipts = delegator.findByAnd("ShipmentReceipt", {"orderId" : orderHeader.getString("orderId"), "orderItemSeqId" : orderItem.orderItemSeqId})/>
                        <#assign totalReceived = 0.0>
                        <#if shipmentReceipts?exists && shipmentReceipts?has_content>
                          <#list shipmentReceipts as shipmentReceipt>
                            <#if shipmentReceipt.quantityAccepted?exists && shipmentReceipt.quantityAccepted?has_content>
                              <#assign  quantityAccepted = shipmentReceipt.quantityAccepted>
                              <#assign totalReceived = quantityAccepted + totalReceived>
                            </#if>
                            <#if shipmentReceipt.quantityRejected?exists && shipmentReceipt.quantityRejected?has_content>
                              <#assign  quantityRejected = shipmentReceipt.quantityRejected>
                              <#assign totalReceived = quantityRejected + totalReceived>
                            </#if>
                          </#list>
                        </#if>
                        <#if orderHeader.orderTypeId == "PURCHASE_ORDER">
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
                      <#if orderHeader.orderTypeId == "SALES_ORDER">
                        <#if pickedQty gt 0 && orderHeader.statusId == "ORDER_APPROVED">
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
                          <td><b>${uiLabelMap.OrderQtyShipped}</b></td>
                          <td>${shippedQuantity}</td>
                       </tr>
                       <tr valign="top">
                          <td><b>${uiLabelMap.OrderShortfalled}</b></td>
                          <td>${shortfalledQuantity}</td>
                          <td><b>${uiLabelMap.OrderOutstanding}</b></td>
                          <td>
                          <#-- Make sure digital goods without shipments don't always remainn "outstanding": if item is completed, it must have no outstanding quantity.  -->
                          <#if (orderItem.statusId != null) && (orderItem.statusId == "ITEM_COMPLETED")>
                          0
                          <#elseif orderHeader.orderTypeId == "PURCHASE_ORDER">
                            ${(orderItem.quantity?default(0) - orderItem.cancelQuantity?default(0)) - totalReceived?double}
                          <#elseif orderHeader.orderTypeId == "SALES_ORDER">
                            ${(orderItem.quantity?default(0) - orderItem.cancelQuantity?default(0)) - shippedQuantity?double}
                          </#if>
                          </td>
                       </tr>
                       <tr valign="top">
                       </tr>
                          <td><b>${uiLabelMap.OrderInvoiced}</b></td>
                          <td>${orderReadHelper.getOrderItemInvoicedQuantity(orderItem)}</td>
                          <td><b>${uiLabelMap.OrderReturned}</b></td>
                          <td>${returnQuantityMap.get(orderItem.orderItemSeqId)?default(0)}</td>
                        </td>
                      </tr>
                    </table></div></div>
                  </td>

                  <td align="right" valign="top" nowrap="nowrap">
                    <@ofbizCurrency amount=orderItem.unitPrice isoCode=currencyUomId/> / <@ofbizCurrency amount=orderItem.unitListPrice isoCode=currencyUomId/>
                  </td>
                  <td align="right" valign="top" nowrap="nowrap">
                    <@ofbizCurrency amount=Static["org.ofbiz.order.order.OrderReadHelper"].getOrderItemAdjustmentsTotal(orderItem, orderAdjustments, true, false, false) isoCode=currencyUomId/>
                  </td>
                  <td align="right" valign="top" nowrap="nowrap">
                    <#if orderItem.statusId != "ITEM_CANCELLED">
                      <@ofbizCurrency amount=Static["org.ofbiz.order.order.OrderReadHelper"].getOrderItemSubTotal(orderItem, orderAdjustments) isoCode=currencyUomId/>
                    <#else>
                      <@ofbizCurrency amount=0.00 isoCode=currencyUomId/>
                    </#if>
                  </td>
                  <td>&nbsp;</td>
                </#if>
              </tr>

              <#-- show info from workeffort -->
              <#assign workOrderItemFulfillments = orderItem.getRelated("WorkOrderItemFulfillment")?if_exists>
              <#if workOrderItemFulfillments?has_content>
                  <#list workOrderItemFulfillments as workOrderItemFulfillment>
                      <#assign workEffort = workOrderItemFulfillment.getRelatedOneCache("WorkEffort")>
                      <tr<#if itemClass == "1"> class="alternate-row"</#if>>
                        <td>&nbsp;</td>
                        <td colspan="6">
                          <div>
                            <#if orderItem.orderItemTypeId != "RENTAL_ORDER_ITEM">
                              <span class="label">${uiLabelMap.ManufacturingProductionRun}</span>
                              <a href="/manufacturing/control/ShowProductionRun?productionRunId=${workEffort.workEffortId}&amp;externalLoginKey=${externalLoginKey}" class="buttontext">${workEffort.workEffortId}</a>&nbsp;
                              ${uiLabelMap.OrderCurrentStatus}&nbsp;${(delegator.findByPrimaryKeyCache("StatusItem", Static["org.ofbiz.base.util.UtilMisc"].toMap("statusId", workEffort.getString("currentStatusId"))).get("description",locale))?if_exists}
                            <#else>
                            ${uiLabelMap.CommonFrom}: ${workEffort.estimatedStartDate?string("yyyy-MM-dd")} ${uiLabelMap.CommonTo}: ${workEffort.estimatedCompletionDate?string("yyyy-MM-dd")} ${uiLabelMap.OrderNumberOfPersons}: ${workEffort.reservPersons?default("")}
                            </#if>
                          </div>
                        </td>
                      </tr>
                      <#break><#-- need only the first one -->
                  </#list>
              </#if>

              <#-- show linked order lines -->
              <#assign linkedOrderItemsTo = delegator.findByAnd("OrderItemAssoc", Static["org.ofbiz.base.util.UtilMisc"].toMap("orderId", orderItem.getString("orderId"),
                                                                                                                               "orderItemSeqId", orderItem.getString("orderItemSeqId")))>
              <#assign linkedOrderItemsFrom = delegator.findByAnd("OrderItemAssoc", Static["org.ofbiz.base.util.UtilMisc"].toMap("toOrderId", orderItem.getString("orderId"),
                                                                                                                                 "toOrderItemSeqId", orderItem.getString("orderItemSeqId")))>
              <#if linkedOrderItemsTo?has_content>
                <#list linkedOrderItemsTo as linkedOrderItem>
                  <#assign linkedOrderId = linkedOrderItem.toOrderId>
                  <#assign linkedOrderItemSeqId = linkedOrderItem.toOrderItemSeqId>
                  <#assign linkedOrderItemValue = linkedOrderItem.getRelatedOne("ToOrderItem")>
                  <#assign linkedOrderItemValueStatus = linkedOrderItemValue.getRelatedOne("StatusItem")>
                  <#assign description = linkedOrderItem.getRelatedOne("OrderItemAssocType").getString("description")/>
                  <tr<#if itemClass == "1"> class="alternate-row"</#if>>
                    <td>&nbsp;</td>
                    <td colspan="6">
                      <div>
                        <span class="label">${uiLabelMap.OrderLinkedToOrderItem}</span>&nbsp;(${description?if_exists})
                        <a href="/ordermgr/control/orderview?orderId=${linkedOrderId}" class="buttontext">${linkedOrderId}/${linkedOrderItemSeqId}</a>&nbsp;${linkedOrderItemValueStatus.description?if_exists}
                      </div>
                    </td>
                  </tr>
                </#list>
              </#if>
              <#if linkedOrderItemsFrom?has_content>
                <#list linkedOrderItemsFrom as linkedOrderItem>
                  <#assign linkedOrderId = linkedOrderItem.orderId>
                  <#assign linkedOrderItemSeqId = linkedOrderItem.orderItemSeqId>
                  <#assign linkedOrderItemValue = linkedOrderItem.getRelatedOne("FromOrderItem")>
                  <#assign linkedOrderItemValueStatus = linkedOrderItemValue.getRelatedOne("StatusItem")>
                  <#assign description = linkedOrderItem.getRelatedOne("OrderItemAssocType").getString("description")/>
                  <tr<#if itemClass == "1"> class="alternate-row"</#if>>
                    <td>&nbsp;</td>
                    <td colspan="6">
                      <div>
                        <span class="label">${uiLabelMap.OrderLinkedFromOrderItem}</span>&nbsp;(${description?if_exists})
                        <a href="/ordermgr/control/orderview?orderId=${linkedOrderId}" class="buttontext">${linkedOrderId}/${linkedOrderItemSeqId}</a>&nbsp;${linkedOrderItemValueStatus.description?if_exists}
                      </div>
                    </td>
                  </tr>
                </#list>
              </#if>

              <#-- show linked requirements -->
              <#assign linkedRequirements = orderItem.getRelated("OrderRequirementCommitment")?if_exists>

              <#if linkedRequirements?has_content>
                <#list linkedRequirements as linkedRequirement>
                <tr<#if itemClass == "1"> class="alternate-row"</#if>>
                    <td>&nbsp;</td>
                    <td colspan="6">
                      <div>
                        <span class="label">${uiLabelMap.OrderLinkedToRequirement}</span>&nbsp;
                        <a href="<@ofbizUrl>EditRequirement?requirementId=${linkedRequirement.requirementId}</@ofbizUrl>" class="buttontext">${linkedRequirement.requirementId}</a>&nbsp;
                      </div>
                    </td>
                  </tr>
                </#list>
              </#if>

              <#-- show linked quote -->
              <#assign linkedQuote = orderItem.getRelatedOneCache("QuoteItem")?if_exists>

              <#if linkedQuote?has_content>
                <tr<#if itemClass == "1"> class="alternate-row"</#if>>
                  <td>&nbsp;</td>
                  <td colspan="6">
                    <div>
                      <span class="label">${uiLabelMap.OrderLinkedToQuote}</span>&nbsp;
                      <a href="<@ofbizUrl>EditQuoteItem?quoteId=${linkedQuote.quoteId}&amp;quoteItemSeqId=${linkedQuote.quoteItemSeqId}</@ofbizUrl>" class="buttontext">${linkedQuote.quoteId}-${linkedQuote.quoteItemSeqId}</a>&nbsp;
                    </div>
                  </td>
                </tr>
              </#if>

              <#-- now show adjustment details per line item -->
              <#assign orderItemAdjustments = Static["org.ofbiz.order.order.OrderReadHelper"].getOrderItemAdjustmentList(orderItem, orderAdjustments)>
              <#if orderItemAdjustments?exists && orderItemAdjustments?has_content>
                <#list orderItemAdjustments as orderItemAdjustment>
                  <#assign adjustmentType = orderItemAdjustment.getRelatedOneCache("OrderAdjustmentType")>
                  <tr<#if itemClass == "1"> class="alternate-row"</#if>>
                    <td align="right" colspan="2">
                      <div>
                        <span class="label">${uiLabelMap.OrderAdjustment}</span>&nbsp;${adjustmentType.get("description",locale)}
                        ${orderItemAdjustment.get("description",locale)?if_exists}
                        <#if orderItemAdjustment.comments?has_content>(${orderItemAdjustment.comments?default("")})</#if>
                        <#if orderItemAdjustment.productPromoId?has_content><a href="/catalog/control/EditProductPromo?productPromoId=${orderItemAdjustment.productPromoId}&amp;externalLoginKey=${externalLoginKey}">${orderItemAdjustment.getRelatedOne("ProductPromo").getString("promoName")}</a></#if>
                        <#if orderItemAdjustment.orderAdjustmentTypeId == "SALES_TAX">
                          <#if orderItemAdjustment.primaryGeoId?has_content>
                            <#assign primaryGeo = orderItemAdjustment.getRelatedOneCache("PrimaryGeo")/>
                            <#if primaryGeo.geoName?has_content>
                                <span class="label">${uiLabelMap.OrderJurisdiction}</span>&nbsp;${primaryGeo.geoName} [${primaryGeo.abbreviation?if_exists}]
                            </#if>
                            <#if orderItemAdjustment.secondaryGeoId?has_content>
                              <#assign secondaryGeo = orderItemAdjustment.getRelatedOneCache("SecondaryGeo")/>
                              <span class="label">${uiLabelMap.CommonIn}</span>&nbsp;${secondaryGeo.geoName} [${secondaryGeo.abbreviation?if_exists}])
                            </#if>
                          </#if>
                          <#if orderItemAdjustment.sourcePercentage?exists><span class="label">${uiLabelMap.OrderRate}</span>&nbsp;${orderItemAdjustment.sourcePercentage?string("0.######")}</#if>
                          <#if orderItemAdjustment.customerReferenceId?has_content><span class="label">${uiLabelMap.OrderCustomerTaxId}</span>&nbsp;${orderItemAdjustment.customerReferenceId}</#if>
                          <#if orderItemAdjustment.exemptAmount?exists><span class="label">${uiLabelMap.OrderExemptAmount}</span>&nbsp;${orderItemAdjustment.exemptAmount}</#if>
                        </#if>
                      </div>
                    </td>
                    <td>&nbsp;</td>
                    <td>&nbsp;</td>
                    <td align="right">
                      <div>
                        <@ofbizCurrency amount=Static["org.ofbiz.order.order.OrderReadHelper"].calcItemAdjustment(orderItemAdjustment, orderItem) isoCode=currencyUomId/>
                      </div>
                     </td>
                    <td colspan="2">&nbsp;</td>
                  </tr>
                </#list>
              </#if>

              <#-- now show price info per line item -->
              <#assign orderItemPriceInfos = orderReadHelper.getOrderItemPriceInfos(orderItem)>
              <#if orderItemPriceInfos?exists && orderItemPriceInfos?has_content>
                <tr<#if itemClass == "1"> class="alternate-row"</#if>><td colspan="7">&nbsp;</td></tr>
                <#list orderItemPriceInfos as orderItemPriceInfo>
                  <tr<#if itemClass == "1"> class="alternate-row"</#if>>
                    <td align="right" colspan="2">
                      <div><span class="label">${uiLabelMap.ProductPriceRuleNameId}</span>&nbsp;[${orderItemPriceInfo.productPriceRuleId?if_exists}:${orderItemPriceInfo.productPriceActionSeqId?if_exists}] ${orderItemPriceInfo.description?if_exists}</div>
                    </td>
                    <td>&nbsp;</td>
                    <td align="right">
                      <div>
                        <@ofbizCurrency amount=orderItemPriceInfo.modifyAmount isoCode=currencyUomId/>
                      </div>
                    </td>
                    <td colspan="3">&nbsp;</td>
                  </tr>
                </#list>
              </#if>

              <#-- now show survey information per line item -->
              <#assign orderItemSurveyResponses = Static["org.ofbiz.order.order.OrderReadHelper"].getOrderItemSurveyResponse(orderItem)>
              <#if orderItemSurveyResponses?exists && orderItemSurveyResponses?has_content>
                <#list orderItemSurveyResponses as survey>
                  <tr<#if itemClass == "1"> class="alternate-row"</#if>>
                    <td align="right" colspan="2">
                      <div>
                        <span class="label">${uiLabelMap.CommonSurveys}</span>&nbsp;
                        <a href="/content/control/ViewSurveyResponses?surveyResponseId=${survey.surveyResponseId}&amp;surveyId=${survey.surveyId}&amp;externalLoginKey=${externalLoginKey}" class="buttontext">${survey.surveyId}</a>
                      </div>
                    </td>
                    <td colspan="5">&nbsp;</td>
                  </tr>
                </#list>
              </#if>

              <#-- display the ship estimated/before/after dates -->
              <#if orderItem.estimatedShipDate?exists>
                <tr<#if itemClass == "1"> class="alternate-row"</#if>>
                  <td align="right" colspan="2">
                    <div><span class="label">${uiLabelMap.OrderEstimatedShipDate}</span>&nbsp;${orderItem.estimatedShipDate?string.short}</div>
                  </td>
                  <td colspan="5">&nbsp;</td>
                </tr>
              </#if>
              <#if orderItem.estimatedDeliveryDate?exists>
              <tr<#if itemClass == "1"> class="alternate-row"</#if>>
                <td align="right" colspan="2">
                  <div><span class="label">${uiLabelMap.OrderOrderQuoteEstimatedDeliveryDate}</span>&nbsp;${orderItem.estimatedDeliveryDate?string.short}</div>
                </td>
                <td colspan="5">&nbsp;</td>
              </tr>
              </#if>
              <#if orderItem.shipAfterDate?exists>
              <tr<#if itemClass == "1"> class="alternate-row"</#if>>
                <td align="right" colspan="2">
                  <div><span class="label">${uiLabelMap.OrderShipAfterDate}</span>&nbsp;${orderItem.shipAfterDate?string.short}</div>
                </td>
                <td colspan="5">&nbsp;</td>
              </tr>
              </#if>
              <#if orderItem.shipBeforeDate?exists>
              <tr<#if itemClass == "1"> class="alternate-row"</#if>>
                <td align="right" colspan="2">
                  <div><span class="label">${uiLabelMap.OrderShipBeforeDate}</span>&nbsp;${orderItem.shipBeforeDate?string.short}</div>
                </td>
                <td colspan="5">&nbsp;</td>
              </tr>
              </#if>

              <#-- now show ship group info per line item -->
              <#assign orderItemShipGroupAssocs = orderItem.getRelated("OrderItemShipGroupAssoc")?if_exists>
              <#if orderItemShipGroupAssocs?has_content>
                <#list orderItemShipGroupAssocs as shipGroupAssoc>
                  <#assign shipGroup = shipGroupAssoc.getRelatedOne("OrderItemShipGroup")>
                  <#assign shipGroupAddress = shipGroup.getRelatedOne("PostalAddress")?if_exists>
                  <tr<#if itemClass == "1"> class="alternate-row"</#if>>
                    <td align="right" colspan="2">
                      <div><span class="label">${uiLabelMap.OrderShipGroup}</span>&nbsp;[${shipGroup.shipGroupSeqId}] ${shipGroupAddress.address1?default("${uiLabelMap.OrderNotShipped}")}</div>
                    </td>
                    <td align="center">
                      <div>${shipGroupAssoc.quantity?string.number}&nbsp;</div>
                    </td>
                    <td colspan="4">&nbsp;</td>
                  </tr>
                </#list>
              </#if>

              <#-- now show inventory reservation info per line item -->
              <#if orderItemShipGrpInvResList?exists && orderItemShipGrpInvResList?has_content>
                <#list orderItemShipGrpInvResList as orderItemShipGrpInvRes>
                  <tr<#if itemClass == "1"> class="alternate-row"</#if>>
                    <td align="right" colspan="2">
                      <div>
                          <span class="label">${uiLabelMap.FacilityInventory}</span>&nbsp;
                          <a href="/facility/control/EditInventoryItem?inventoryItemId=${orderItemShipGrpInvRes.inventoryItemId}&amp;externalLoginKey=${externalLoginKey}" class="buttontext">${orderItemShipGrpInvRes.inventoryItemId}</a>
                          <span class="label">${uiLabelMap.OrderShipGroup}</span>&nbsp;${orderItemShipGrpInvRes.shipGroupSeqId}
                      </div>
                    </td>
                    <td align="center">
                      <div>${orderItemShipGrpInvRes.quantity?string.number}&nbsp;</div>
                    </td>
                    <td>
                      <#if (orderItemShipGrpInvRes.quantityNotAvailable?has_content && orderItemShipGrpInvRes.quantityNotAvailable > 0)>
                        <span style="color: red;">[${orderItemShipGrpInvRes.quantityNotAvailable?string.number}&nbsp;${uiLabelMap.OrderBackOrdered}]</span>
                        <#--<a href="<@ofbizUrl>balanceInventoryItems?inventoryItemId=${orderItemShipGrpInvRes.inventoryItemId}&amp;orderId=${orderId}&amp;priorityOrderId=${orderId}&amp;priorityOrderItemSeqId=${orderItemShipGrpInvRes.orderItemSeqId}</@ofbizUrl>" class="buttontext" style="font-size: xx-small;">Raise Priority</a> -->
                      </#if>
                      &nbsp;
                    </td>
                    <td colspan="3">&nbsp;</td>
                  </tr>
                </#list>
              </#if>

              <#-- now show planned shipment info per line item -->
              <#assign orderShipments = orderItem.getRelated("OrderShipment")?if_exists>
              <#if orderShipments?has_content>
                <#list orderShipments as orderShipment>
                  <tr<#if itemClass == "1"> class="alternate-row"</#if>>
                    <td align="right" colspan="2">
                      <div><span class="label">${uiLabelMap.OrderPlannedInShipment}</span>&nbsp;<a target="facility" href="/facility/control/ViewShipment?shipmentId=${orderShipment.shipmentId}&amp;externalLoginKey=${externalLoginKey}" class="buttontext">${orderShipment.shipmentId}</a>: ${orderShipment.shipmentItemSeqId}</div>
                    </td>
                    <td align="center">
                      <div>${orderShipment.quantity?string.number}&nbsp;</div>
                    </td>
                    <td colspan="4">&nbsp;</td>
                  </tr>
                </#list>
              </#if>

              <#-- now show item issuances (shipment) per line item -->
              <#assign itemIssuances = itemIssuancesPerItem.get(orderItem.get("orderItemSeqId"))?if_exists>
              <#if itemIssuances?has_content>
                <#list itemIssuances as itemIssuance>
                  <tr<#if itemClass == "1"> class="alternate-row"</#if>>
                    <td align="right" colspan="2">
                      <div>
                        <#if itemIssuance.shipmentId?has_content>
                          <span class="label">${uiLabelMap.OrderIssuedToShipmentItem}</span>&nbsp;
                          <a target="facility" href="/facility/control/ViewShipment?shipmentId=${itemIssuance.shipmentId}&amp;externalLoginKey=${externalLoginKey}" class="buttontext">${itemIssuance.shipmentId}</a>: ${itemIssuance.shipmentItemSeqId?if_exists}
                        <#else>
                          <span class="label">${uiLabelMap.OrderIssuedWithoutShipment}</span>
                        </#if>
                      </div>
                    </td>
                    <td align="center">
                      <div>${itemIssuance.quantity?default(0) - itemIssuance.cancelQuantity?default(0)}&nbsp;</div>
                    </td>
                    <td colspan="4">&nbsp;</td>
                  </tr>
                </#list>
              </#if>

              <#-- now show item issuances (inventory item) per line item -->
              <#if itemIssuances?has_content>
                <#list itemIssuances as itemIssuance>
                  <tr<#if itemClass == "1"> class="alternate-row"</#if>>
                    <td align="right" colspan="2">
                      <div>
                        <#if itemIssuance.inventoryItemId?has_content>
                          <#assign inventoryItem = itemIssuance.getRelatedOne("InventoryItem")/>
                          <span class="label">${uiLabelMap.FacilityInventory}</span>
                          <a href="/facility/control/EditInventoryItem?inventoryItemId=${itemIssuance.inventoryItemId}&amp;externalLoginKey=${externalLoginKey}" class="buttontext">${itemIssuance.inventoryItemId}</a>
                          <span class="label">${uiLabelMap.OrderShipGroup}</span>&nbsp;${itemIssuance.shipGroupSeqId?if_exists}
                          <#if (inventoryItem.serialNumber?has_content)><br><span class="label">${uiLabelMap.ProductSerialNumber}</span>&nbsp;${inventoryItem.serialNumber}&nbsp;</#if>
                        </#if>
                      </div>
                    </td>
                    <td align="center">
                      <div>${itemIssuance.quantity?default(0) - itemIssuance.cancelQuantity?default(0)}</div>
                    </td>
                    <td colspan="4">&nbsp;</td>
                  </tr>
                </#list>
              </#if>

              <#-- now show shipment receipts per line item -->
              <#assign shipmentReceipts = orderItem.getRelated("ShipmentReceipt")?if_exists>
              <#if shipmentReceipts?has_content>
                <#list shipmentReceipts as shipmentReceipt>
                  <tr<#if itemClass == "1"> class="alternate-row"</#if>>
                    <td align="right" colspan="2">
                      <div>
                        <#if shipmentReceipt.shipmentId?has_content>
                          <span class="label">${uiLabelMap.OrderShipmentReceived}</span>&nbsp;
                          <a target="facility" href="/facility/control/ViewShipment?shipmentId=${shipmentReceipt.shipmentId}&amp;externalLoginKey=${externalLoginKey}" class="buttontext">${shipmentReceipt.shipmentId}</a>:${shipmentReceipt.shipmentItemSeqId?if_exists}
                        </#if>
                          &nbsp;${shipmentReceipt.datetimeReceived}&nbsp;
                          <span class="label">${uiLabelMap.FacilityInventory}</span>&nbsp;
                          <a href="/facility/control/EditInventoryItem?inventoryItemId=${shipmentReceipt.inventoryItemId}&amp;externalLoginKey=${externalLoginKey}" class="buttontext">${shipmentReceipt.inventoryItemId}</a>
                      </div>
                    </td>
                    <td align="center">
                      <div>${shipmentReceipt.quantityAccepted?string.number}&nbsp;/&nbsp;${shipmentReceipt.quantityRejected?default(0)?string.number}</div>
                    </td>
                    <td colspan="4">&nbsp;</td>
                  </tr>
                </#list>
              </#if>
            <#if itemClass == "2">
                <#assign itemClass = "1">
            <#else>
                <#assign itemClass = "2">
            </#if>

            </#list>
          </#if>
          <tr><td colspan="7"><hr /></td></tr>
          <#list orderHeaderAdjustments as orderHeaderAdjustment>
            <#assign adjustmentType = orderHeaderAdjustment.getRelatedOne("OrderAdjustmentType")>
            <#assign adjustmentAmount = Static["org.ofbiz.order.order.OrderReadHelper"].calcOrderAdjustment(orderHeaderAdjustment, orderSubTotal)>
            <#if adjustmentAmount != 0>
              <tr>
                <td align="right" colspan="5">
                  <div><span class="label">${adjustmentType.get("description",locale)}</span>&nbsp;${orderHeaderAdjustment.comments?if_exists}  ${orderHeaderAdjustment.get("description")?if_exists} : </div>
                </td>
                <td align="right" nowrap="nowrap">
                  <div><@ofbizCurrency amount=adjustmentAmount isoCode=currencyUomId/></div>
                </td>
                <td>&nbsp;</td>
              </tr>
            </#if>
          </#list>

          <#-- subtotal -->
          <tr><td colspan="1"></td><td colspan="6"><hr /></td></tr>
          <tr>
            <td align="right" colspan="5"><div><span class="label">${uiLabelMap.OrderItemsSubTotal}</span></div></td>
            <td align="right" nowrap="nowrap"><div><@ofbizCurrency amount=orderSubTotal isoCode=currencyUomId/></div></td>
            <td>&nbsp;</td>
          </tr>

          <#-- other adjustments -->
          <tr>
            <td align="right" colspan="5"><div><span class="label">${uiLabelMap.OrderTotalOtherOrderAdjustments}</span></div></td>
            <td align="right" nowrap="nowrap"><div><@ofbizCurrency amount=otherAdjAmount isoCode=currencyUomId/></div></td>
            <td>&nbsp;</td>
          </tr>

          <#-- shipping adjustments -->
          <tr>
            <td align="right" colspan="5"><div><span class="label">${uiLabelMap.OrderTotalShippingAndHandling}</span></div></td>
            <td align="right" nowrap="nowrap"><div><@ofbizCurrency amount=shippingAmount isoCode=currencyUomId/></div></td>
            <td>&nbsp;</td>
          </tr>

          <#-- tax adjustments -->
          <tr>
            <td align="right" colspan="5"><div><span class="label">${uiLabelMap.OrderTotalSalesTax}</span></div></td>
            <td align="right" nowrap="nowrap"><div><@ofbizCurrency amount=taxAmount isoCode=currencyUomId/></div></td>
            <td>&nbsp;</td>
          </tr>

          <#-- grand total -->
          <tr>
            <td align="right" colspan="5"><div><span class="label">${uiLabelMap.OrderTotalDue}</span></div></td>
            <td align="right" nowrap="nowrap">
              <div><@ofbizCurrency amount=grandTotal isoCode=currencyUomId/></div>
            </td>
            <td>&nbsp;</td>
          </tr>
        </table>
    </div>
</div>
</#if>
