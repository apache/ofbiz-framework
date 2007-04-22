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
    <div class="screenlet-header">
        <div class="boxlink">
            <#if security.hasEntityPermission("ORDERMGR", "_UPDATE", session)>
                <#if orderHeader?has_content && orderHeader.statusId != "ORDER_CANCELLED">
                    <div class="tabletext">
                      <#if orderHeader.statusId != "ORDER_COMPLETED">
                        <#--
                        <a href="<@ofbizUrl>cancelOrderItem?${paramString}</@ofbizUrl>" class="submenutext">${uiLabelMap.OrderCancelAllItems}</a>
                        -->
                        <a href="<@ofbizUrl>editOrderItems?${paramString}</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderEditItems}</a>
                      </#if>
                      <a href="<@ofbizUrl>loadCartFromOrder?${paramString}&finalizeMode=init</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderCreateAsNewOrder}</a>
                      <#if returnableItems?has_content>
                        <a href="<@ofbizUrl>quickreturn?orderId=${orderId}&amp;party_id=${partyId?if_exists}&amp;returnHeaderTypeId=${returnHeaderTypeId}</@ofbizUrl>"  class="buttontext">${uiLabelMap.OrderCreateReturn}</a>
                      </#if>
                    </div>
                </#if>
            </#if>
        </div>
        <div class="boxhead">&nbsp;${uiLabelMap.OrderOrderItems}</div>
    </div>
    <div class="screenlet-body">
        <table width="100%" border="0" cellpadding="0" cellspacing="0">
          <tr align="left" valign=bottom>
            <td width="30%" align="left"><div class="tableheadtext">${uiLabelMap.ProductProduct}</div></td>
            <td width="30%" align="left"><div class="tableheadtext">${uiLabelMap.CommonStatus}</div></td>
            <td width="5%" align="center"><div class="tableheadtext">${uiLabelMap.OrderQuantity}</div></td>
            <td width="10%" align="right"><div class="tableheadtext">${uiLabelMap.OrderUnitList}</div></td>
            <td width="10%" align="right"><div class="tableheadtext">${uiLabelMap.OrderAdjustments}</div></td>
            <td width="10%" align="right"><div class="tableheadtext">${uiLabelMap.OrderSubTotal}</div></td>
            <td width="5%">&nbsp;</td>
          </tr>
          <#if !orderItemList?has_content>
            <tr><td><font color="red">${uiLabelMap.checkhelper_sales_order_lines_lookup_failed}</font></td></tr>
          <#else>
            <#list orderItemList as orderItem>
              <#assign orderItemContentWrapper = Static["org.ofbiz.order.order.OrderContentWrapper"].makeOrderContentWrapper(orderItem, request)>
              <#assign orderItemShipGrpInvResList = orderReadHelper.getOrderItemShipGrpInvResList(orderItem)>
              <#if orderHeader.orderTypeId == "SALES_ORDER"><#assign pickedQty = orderReadHelper.getItemPickedQuantityBd(orderItem)></#if>
              <tr><td colspan="8"><hr class="sepbar"/></td></tr>
              <tr>
                <#assign orderItemType = orderItem.getRelatedOne("OrderItemType")?if_exists>
                <#assign productId = orderItem.productId?if_exists>
                <#if productId?exists && productId == "shoppingcart.CommentLine">
                  <td colspan="1" valign="top">
                    <b><div class="tabletext"> &gt;&gt; ${orderItem.itemDescription}</div></b>
                  </td>
                <#else>
                  <td valign="top">
                    <#if productId?has_content>
                      <#assign product = orderItem.getRelatedOneCache("Product")>
                    </#if>
                    <div class="tabletext">
                      <#if productId?exists>
                        ${orderItem.productId?default("N/A")} - ${orderItem.itemDescription?if_exists}
                        <#if (product.salesDiscontinuationDate)?exists && Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp().after(product.salesDiscontinuationDate)>
                          <br/><span style="color: red;">${uiLabelMap.OrderItemDiscontinued}: ${product.salesDiscontinuationDate}</span>
                        </#if>
                      <#elseif orderItemType?exists>
                        ${orderItemType.description} - ${orderItem.itemDescription?if_exists}
                      <#else>
                        ${orderItem.itemDescription?if_exists}
                      </#if>
                    </div>
                    <#if productId?exists>
                      <div class="tabletext" style="margin-top: 15px; margin-left: 10px;">
                        <a href="/catalog/control/EditProduct?productId=${productId}" class="buttontext" target="_blank">${uiLabelMap.ProductCatalog}</a>
                        <a href="/ecommerce/control/product?product_id=${productId}" class="buttontext" target="_blank">${uiLabelMap.EcommerceEcommerce}</a>
                        <#if orderItemContentWrapper.get("IMAGE_URL")?has_content>
                          <a href="<@ofbizUrl>viewimage?orderId=${orderId}&amp;orderItemSeqId=${orderItem.orderItemSeqId}&amp;orderContentTypeId=IMAGE_URL</@ofbizUrl>" target="_orderImage" class="buttontext">${uiLabelMap.OrderViewImage}</a>
                        </#if>
                      </div>

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
                        <div class="tabletext" style="margin-top: 15px; margin-left: 20px;">
                            <table cellspacing="0" cellpadding="0" border="0">
                              <tr><td style="text-align: right; padding-bottom: 10px;">
                                  <a class="buttontext" href="/catalog/control/EditProductInventoryItems?productId=${productId}&amp;showAllFacilities=Y&amp;externalLoginKey=${externalLoginKey}" target="_blank">${uiLabelMap.ProductInventory}</a>
                              </td><td>&nbsp;</td></tr>
                              <tr><td align="left">${uiLabelMap.OrderRequiredForSO}</td>
                                <td style="padding-left: 15px; text-align: left;">${requiredQuantity}</td></tr>
                              <tr><td align="left">${uiLabelMap.ProductInInventory} ${uiLabelMap.ProductQoh}</td>
                                <td style="padding-left: 15px; text-align: left;">${qohQuantity} (${uiLabelMap.ProductAtp}: ${atpQuantity})</td></tr>
                              <#if (product != null) && (product.productTypeId != null) && (product.productTypeId == "MARKETING_PKG_AUTO")>
                                <tr><td align="left">${uiLabelMap.ProductMarketingPackageQOH}</td>
                                  <td style="padding-left: 15px; text-align: left;">${mktgPkgQOH} (${uiLabelMap.ProductAtp}: ${mktgPkgATP})</td></tr>
                              </#if>
                              <tr><td align="left">${uiLabelMap.OrderOnOrder}</td>
                                <td style="padding-left: 15px; text-align: left;">${onOrderQuantity}</td></tr>
                              <tr><td align="left">${uiLabelMap.OrderInProduction}</td>
                                <td style="padding-left: 15px; text-align: left;">${inProductionQuantity}</td></tr>
                              <tr><td align="left">${uiLabelMap.OrderUnplanned}</td>
                                <td style="padding-left: 15px; text-align: left;">${unplannedQuantity}</td></tr>
                            </table>
                        </div>
                      </#if>

                    </#if>
                  </td>

                  <#-- now show status details per line item -->
                  <#assign currentItemStatus = orderItem.getRelatedOne("StatusItem")>
                  <td align="left" colspan="1" valign="top">
                    <div class="tabletext">${uiLabelMap.CommonCurrent}: ${currentItemStatus.get("description",locale)?default(currentItemStatus.statusId)}</div>
                    <#assign orderItemStatuses = orderReadHelper.getOrderItemStatuses(orderItem)>
                    <#list orderItemStatuses as orderItemStatus>
                      <#assign loopStatusItem = orderItemStatus.getRelatedOne("StatusItem")>
                      <div class="tabletext">
                        ${orderItemStatus.statusDatetime.toString()} : ${loopStatusItem.get("description",locale)?default(orderItemStatus.statusId)}
                      </div>
                    </#list>
                    <#assign returns = orderItem.getRelated("ReturnItem")?if_exists>
                    <#if returns?has_content>
                      <#list returns as returnItem>
                        <#assign returnHeader = returnItem.getRelatedOne("ReturnHeader")>
                        <#if returnHeader.statusId != "RETURN_CANCELLED">
                          <div class="tabletext">
                            <font color="red"><b>${uiLabelMap.OrderReturned}</b></font> #<a href="<@ofbizUrl>returnMain?returnId=${returnItem.returnId}</@ofbizUrl>" class="buttontext">${returnItem.returnId}</a>
                          </div>
                        </#if>
                      </#list>
                    </#if>
                  </td>

                  <#-- QUANTITY -->
                  <td align="right" valign="top" nowrap="nowrap">
                    <table>
                      <tr valign="top">
                        <td>
                        <#assign remainingQuantity = (orderItem.quantity?default(0) - orderItem.cancelQuantity?default(0))>
                        <#assign shippedQuantity = orderReadHelper.getItemShippedQuantity(orderItem)>
                        <#-- to compute shortfall amount, sum up the orderItemShipGrpInvRes.quantityNotAvailable -->
                        <#assign shortfalledQuantity = 0/>
                        <#list orderItemShipGrpInvResList as orderItemShipGrpInvRes>
                          <#if (orderItemShipGrpInvRes.quantityNotAvailable?has_content && orderItemShipGrpInvRes.quantityNotAvailable > 0)>
                            <#assign shortfalledQuantity = shortfalledQuantity + orderItemShipGrpInvRes.quantityNotAvailable/>
                          </#if>
                        </#list>
                          <div class="tabletext">${uiLabelMap.OrderOrdered}:&nbsp;${orderItem.quantity?default(0)?string.number}&nbsp;&nbsp;</div>
                          <div class="tabletext">${uiLabelMap.OrderCancelled}:&nbsp;${orderItem.cancelQuantity?default(0)?string.number}&nbsp;&nbsp;</div>
                          <div class="tabletext">${uiLabelMap.OrderRemaining}:&nbsp;${remainingQuantity}&nbsp;&nbsp;</div>
                          <div class="tabletext">${uiLabelMap.OrderShortfalled}:&nbsp;${shortfalledQuantity}&nbsp;&nbsp;</div>
                        </td>
                        <td>
                          <div class="tabletext">${uiLabelMap.OrderShipRequest}
                          :&nbsp;${orderReadHelper.getItemReservedQuantity(orderItem)}&nbsp;&nbsp;</div>
                          <#if orderHeader.orderTypeId == "SALES_ORDER">                         
                          	<div class="tabletext"><#if pickedQty gt 0 && orderHeader.statusId == "ORDER_APPROVED"><font color="red">${uiLabelMap.OrderQtyPicked}:&nbsp;${pickedQty?default(0)?string.number}</font><#else>${uiLabelMap.OrderQtyPicked}:&nbsp;${pickedQty?default(0)?string.number}</#if>&nbsp;&nbsp;</div>
                          </#if>	
                          <div class="tabletext">${uiLabelMap.OrderQtyShipped}:&nbsp;${shippedQuantity}&nbsp;&nbsp;</div>
                          <div class="tabletext">${uiLabelMap.OrderOutstanding}:&nbsp;
                          <#-- Make sure digital goods without shipments don't always remainn "outstanding": if item is completed, it must have no outstanding quantity.  -->
                          <#if (orderItem.statusId != null) && (orderItem.statusId == "ITEM_COMPLETED")>0<#else>${orderItem.quantity?default(0) - orderItem.cancelQuantity?default(0) - shippedQuantity}</#if>&nbsp;&nbsp;</div>
                          <div class="tabletext">${uiLabelMap.OrderInvoiced}:&nbsp;${orderReadHelper.getOrderItemInvoicedQuantity(orderItem)}&nbsp;&nbsp;</div>
                          <div class="tabletext">${uiLabelMap.OrderReturned}:&nbsp;${returnQuantityMap.get(orderItem.orderItemSeqId)?default(0)}&nbsp;&nbsp;</div>
                        </td>
                      </tr>
                    </table>
                  </td>

                  <td align="right" valign="top" nowrap="nowrap">
                    <div class="tabletext"><@ofbizCurrency amount=orderItem.unitPrice isoCode=currencyUomId/> / <@ofbizCurrency amount=orderItem.unitListPrice isoCode=currencyUomId/></div>
                  </td>
                  <td align="right" valign="top" nowrap="nowrap">
                    <div class="tabletext"><@ofbizCurrency amount=Static["org.ofbiz.order.order.OrderReadHelper"].getOrderItemAdjustmentsTotal(orderItem, orderAdjustments, true, false, false) isoCode=currencyUomId/></div>
                  </td>
                  <td align="right" valign="top" nowrap="nowrap">
                    <#if orderItem.statusId != "ITEM_CANCELLED">
                      <div class="tabletext"><@ofbizCurrency amount=Static["org.ofbiz.order.order.OrderReadHelper"].getOrderItemSubTotal(orderItem, orderAdjustments) isoCode=currencyUomId/></div>
                    <#else>
                      <div class="tabletext"><@ofbizCurrency amount=0.00 isoCode=currencyUomId/></div>
                    </#if>
                  </td>
                  <td>&nbsp;</td>
                  <td align="right" valign="top">
                    &nbsp;
                  </td>
                </#if>
              </tr>

              <#-- show info from workeffort -->
              <#assign workOrderItemFulfillments = orderItem.getRelated("WorkOrderItemFulfillment")?if_exists>
              <#if workOrderItemFulfillments?has_content>
                  <#list workOrderItemFulfillments as workOrderItemFulfillment>
                      <#assign workEffort = workOrderItemFulfillment.getRelatedOneCache("WorkEffort")>
                      <tr>
                        <td>&nbsp;</td>
                        <td colspan="9">
                          <div class="tabletext">
                            <#if orderItem.orderItemTypeId != "RENTAL_ORDER_ITEM">
                              <b><i>${uiLabelMap.ManufacturingProductionRun}</i>:</b>
                              <a href="/manufacturing/control/ShowProductionRun?productionRunId=${workEffort.workEffortId}&amp;externalLoginKey=${externalLoginKey}" class="buttontext" style="font-size: xx-small;">${workEffort.workEffortId}</a>&nbsp;
                            </#if>
                            ${uiLabelMap.CommonFrom}: ${workEffort.estimatedStartDate?string("yyyy-MM-dd")} ${uiLabelMap.CommonTo}: ${workEffort.estimatedCompletionDate?string("yyyy-MM-dd")} ${uiLabelMap.OrderNumberOfPersons}: ${workEffort.reservPersons?default("")}
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
                  <tr>
                    <td>&nbsp;</td>
                    <td colspan="9">
                      <div class="tabletext">
                        <b><i>${uiLabelMap.OrderLinkedToOrderItem} (${linkedOrderItem.orderItemAssocTypeId})</i>:</b>
                        <a href="/ordermgr/control/orderview?orderId=${linkedOrderId}" class="buttontext" style="font-size: xx-small;">${linkedOrderId}/${linkedOrderItemSeqId}</a>&nbsp;
                      </div>
                    </td>
                  </tr>
                </#list>
              </#if>
              <#if linkedOrderItemsFrom?has_content>
                <#list linkedOrderItemsFrom as linkedOrderItem>
                  <#assign linkedOrderId = linkedOrderItem.orderId>
                  <#assign linkedOrderItemSeqId = linkedOrderItem.orderItemSeqId>
                  <tr>
                    <td>&nbsp;</td>
                    <td colspan="9">
                      <div class="tabletext">
                        <b><i>${uiLabelMap.OrderLinkedFromOrderItem} (${linkedOrderItem.orderItemAssocTypeId})</i>:</b>
                        <a href="/ordermgr/control/orderview?orderId=${linkedOrderId}" class="buttontext" style="font-size: xx-small;">${linkedOrderId}/${linkedOrderItemSeqId}</a>&nbsp;
                      </div>
                    </td>
                  </tr>
                </#list>
              </#if>

              <#-- show linked requirements -->
              <#assign linkedRequirements = orderItem.getRelated("OrderRequirementCommitment")?if_exists>

              <#if linkedRequirements?has_content>
                <#list linkedRequirements as linkedRequirement>
                  <tr>
                    <td>&nbsp;</td>
                    <td colspan="9">
                      <div class="tabletext">
                        <b><i>${uiLabelMap.OrderLinkedToRequirement}</i>:</b>
                        <a href="<@ofbizUrl>EditRequirement?requirementId=${linkedRequirement.requirementId}</@ofbizUrl>" class="buttontext" style="font-size: xx-small;">${linkedRequirement.requirementId}</a>&nbsp;
                      </div>
                    </td>
                  </tr>
                </#list>
              </#if>

              <#-- show linked quote -->
              <#assign linkedQuote = orderItem.getRelatedOneCache("QuoteItem")?if_exists>

              <#if linkedQuote?has_content>
                <tr>
                  <td>&nbsp;</td>
                  <td colspan="9">
                    <div class="tabletext">
                      <b><i>${uiLabelMap.OrderLinkedToQuote}</i>:</b>
                      <a href="<@ofbizUrl>EditQuoteItem?quoteId=${linkedQuote.quoteId}&amp;quoteItemSeqId=${linkedQuote.quoteItemSeqId}</@ofbizUrl>" class="buttontext" style="font-size: xx-small;">${linkedQuote.quoteId}-${linkedQuote.quoteItemSeqId}</a>&nbsp;
                    </div>
                  </td>
                </tr>
              </#if>

              <#-- now show adjustment details per line item -->
              <#assign orderItemAdjustments = Static["org.ofbiz.order.order.OrderReadHelper"].getOrderItemAdjustmentList(orderItem, orderAdjustments)>
              <#if orderItemAdjustments?exists && orderItemAdjustments?has_content>
                <#list orderItemAdjustments as orderItemAdjustment>
                  <#assign adjustmentType = orderItemAdjustment.getRelatedOneCache("OrderAdjustmentType")>
                  <tr>
                    <td align="right" colspan="2">					
                      <div class="tabletext" style="font-size: xx-small;">
                        <b><i>${uiLabelMap.OrderAdjustment}</i>:</b> <b>${adjustmentType.get("description",locale)}</b>:
                        ${orderItemAdjustment.get("description",locale)?if_exists} 
                        <#if orderItemAdjustment.comments?has_content>(${orderItemAdjustment.comments?default("")})</#if>
                        <#if orderItemAdjustment.productPromoId?has_content><a href="/catalog/control/EditProductPromo?productPromoId=${orderItemAdjustment.productPromoId}&amp;externalLoginKey=${externalLoginKey}">${orderItemAdjustment.getRelatedOne("ProductPromo").getString("promoName")}</a></#if>
                        <#if orderItemAdjustment.orderAdjustmentTypeId == "SALES_TAX">
                          <#if orderItemAdjustment.primaryGeoId?has_content>
                            <#assign primaryGeo = orderItemAdjustment.getRelatedOneCache("PrimaryGeo")/>
	                        <#if primaryGeo.geoName?has_content>
	                            <b>${uiLabelMap.OrderJurisdiction}:</b> ${primaryGeo.geoName} [${primaryGeo.abbreviation?if_exists}]
	                        </#if>
                            <#if orderItemAdjustment.secondaryGeoId?has_content>
                              <#assign secondaryGeo = orderItemAdjustment.getRelatedOneCache("SecondaryGeo")/>
                              (<b>${uiLabelMap.CommonIn}:</b> ${secondaryGeo.geoName} [${secondaryGeo.abbreviation?if_exists}])
                            </#if>
                          </#if>
                          <#if orderItemAdjustment.sourcePercentage?exists><b>${uiLabelMap.OrderRate}:</b> ${orderItemAdjustment.sourcePercentage?string("0.######")}</#if>
                          <#if orderItemAdjustment.customerReferenceId?has_content><b>${uiLabelMap.OrderCustomerTaxId}:</b> ${orderItemAdjustment.customerReferenceId}</#if>
                          <#if orderItemAdjustment.exemptAmount?exists><b>${uiLabelMap.OrderExemptAmount}:</b> ${orderItemAdjustment.exemptAmount}</#if>
                        </#if>
                      </div>
                    </td>
                    <td>&nbsp;</td>
                    <td>&nbsp;</td>
                    <td align="right">
                      <div class="tabletext" style="font-size: xx-small;">
                        <@ofbizCurrency amount=Static["org.ofbiz.order.order.OrderReadHelper"].calcItemAdjustment(orderItemAdjustment, orderItem) isoCode=currencyUomId/>
                      </div>
                     </td>
                    <td>&nbsp;</td>
                  </tr>
                </#list>
              </#if>

              <#-- now show price info per line item -->
              <#assign orderItemPriceInfos = orderReadHelper.getOrderItemPriceInfos(orderItem)>
              <#if orderItemPriceInfos?exists && orderItemPriceInfos?has_content>
                <tr><td>&nbsp;</td></tr>
                <#list orderItemPriceInfos as orderItemPriceInfo>
                  <tr>
                    <td align="right" colspan="2">
                      <div class="tabletext" style="font-size: xx-small;"><b><i>${uiLabelMap.ProductPriceRuleNameId}</i>:</b> [${orderItemPriceInfo.productPriceRuleId?if_exists}:${orderItemPriceInfo.productPriceActionSeqId?if_exists}] ${orderItemPriceInfo.description?if_exists}</div>
                    </td>
                    <td>&nbsp;</td>
                    <td align="right">
                      <div class="tabletext" style="font-size: xx-small;">
                        <@ofbizCurrency amount=orderItemPriceInfo.modifyAmount isoCode=currencyUomId/>
                      </div>
                    </td>
                    <td>&nbsp;</td>
                    <td>&nbsp;</td>
                  </tr>
                </#list>
              </#if>

              <#-- now show survey information per line item -->
              <#assign orderItemSurveyResponses = Static["org.ofbiz.order.order.OrderReadHelper"].getOrderItemSurveyResponse(orderItem)>
              <#if orderItemSurveyResponses?exists && orderItemSurveyResponses?has_content>
                <#list orderItemSurveyResponses as survey>
                  <tr>
                    <td align="right" colspan="2">
                      <div class="tabletext" style="font-size: xx-small;">
                        <b><i>${uiLabelMap.CommonSurveys}</i>:</b>
                          <a href="/content/control/ViewSurveyResponses?surveyResponseId=${survey.surveyResponseId}&amp;surveyId=${survey.surveyId}<#if survey.partyId?exists>&amp;partyId=${survey.partyId}</#if>&amp;externalLoginKey=${externalLoginKey}" class="buttontext" style="font-size: xx-small;">${survey.surveyId}</a>
                      </div>
                    </td>
                    <td>&nbsp;</td>
                    <td>&nbsp;</td>
                    <td>&nbsp;</td>
                    <td>&nbsp;</td>
                  </tr>
                </#list>
              </#if>

              <#-- display the ship before/after dates -->
              <#if orderItem.shipAfterDate?exists>
              <tr>
                <td align="right" colspan="2">
                  <div class="tabletext" style="font-size: xx-small;"><b><i>${uiLabelMap.OrderShipAfterDate}</i>:</b> ${orderItem.shipAfterDate?string.short}</div>
                </td>
              </tr>
              </#if>
              <#if orderItem.shipBeforeDate?exists>
              <tr>
                <td align="right" colspan="2">
                  <div class="tabletext" style="font-size: xx-small;"><b><i>${uiLabelMap.OrderShipBeforeDate}</i>:</b> ${orderItem.shipBeforeDate?string.short}</div>
                </td>
              </tr>
              </#if>

              <#-- now show ship group info per line item -->
              <#assign orderItemShipGroupAssocs = orderItem.getRelated("OrderItemShipGroupAssoc")?if_exists>
              <#if orderItemShipGroupAssocs?has_content>
                <#list orderItemShipGroupAssocs as shipGroupAssoc>
                  <#assign shipGroup = shipGroupAssoc.getRelatedOne("OrderItemShipGroup")>
                  <#assign shipGroupAddress = shipGroup.getRelatedOne("PostalAddress")?if_exists>
                  <tr>
                    <td align="right" colspan="2">
                      <div class="tabletext" style="font-size: xx-small;"><b><i>${uiLabelMap.OrderShipGroup}</i>:</b> [${shipGroup.shipGroupSeqId}] ${shipGroupAddress.address1?default("${uiLabelMap.OrderNotShipped}")}</div>
                    </td>
                    <td align="center">
                      <div class="tabletext" style="font-size: xx-small;">${shipGroupAssoc.quantity?string.number}&nbsp;</div>
                    </td>
                    <td>&nbsp;</td>
                    <td>&nbsp;</td>
                    <td>&nbsp;</td>
                    <td>&nbsp;</td>
                    <td align="right" valign="top">
                      &nbsp;
                    </td>
                  </tr>
                </#list>
              </#if>

              <#-- now show inventory reservation info per line item -->
              <#if orderItemShipGrpInvResList?exists && orderItemShipGrpInvResList?has_content>
                <#list orderItemShipGrpInvResList as orderItemShipGrpInvRes>
                  <tr>
                    <td align="right" colspan="2">
                      <div class="tabletext" style="font-size: xx-small;">
                        <b><i>${uiLabelMap.FacilityInventory}</i>:</b>
                          <a href="/facility/control/EditInventoryItem?inventoryItemId=${orderItemShipGrpInvRes.inventoryItemId}&amp;externalLoginKey=${externalLoginKey}" class="buttontext" style="font-size: xx-small;">${orderItemShipGrpInvRes.inventoryItemId}</a>
                        <b><i>${uiLabelMap.OrderShipGroup}</i>:</b> ${orderItemShipGrpInvRes.shipGroupSeqId}
                      </div>
                    </td>
                    <td align="center">
                      <div class="tabletext" style="font-size: xx-small;">${orderItemShipGrpInvRes.quantity?string.number}&nbsp;</div>
                    </td>
                    <td class="tabletext">
                      <#if (orderItemShipGrpInvRes.quantityNotAvailable?has_content && orderItemShipGrpInvRes.quantityNotAvailable > 0)>
                        <span style="color: red;">[${orderItemShipGrpInvRes.quantityNotAvailable?string.number}&nbsp;${uiLabelMap.OrderBackOrdered}]</span>
                      </#if>
                      &nbsp;
                    </td>
                    <td>&nbsp;</td>
                    <td>&nbsp;</td>
                  </tr>
                </#list>
              </#if>

              <#-- now show planned shipment info per line item -->
              <#assign orderShipments = orderItem.getRelated("OrderShipment")?if_exists>
              <#if orderShipments?has_content>
                <#list orderShipments as orderShipment>
                  <tr>
                    <td align="right" colspan="2">
                      <div class="tabletext" style="font-size: xx-small;"><b><i>${uiLabelMap.OrderPlannedInShipment}</i>: </b><a target="facility" href="/facility/control/ViewShipment?shipmentId=${orderShipment.shipmentId}&amp;externalLoginKey=${externalLoginKey}" class="buttontext" style="font-size: xx-small;">${orderShipment.shipmentId}</a>: ${orderShipment.shipmentItemSeqId}</div>
                    </td>
                    <td align="center">
                      <div class="tabletext" style="font-size: xx-small;">${orderShipment.quantity?string.number}&nbsp;</div>
                    </td>
                    <td>&nbsp;</td>
                    <td>&nbsp;</td>
                    <td>&nbsp;</td>
                    <td>&nbsp;</td>
                    <td>&nbsp;</td>
                  </tr>
                </#list>
              </#if>

              <#-- now show item issuances (shipment) per line item -->
              <#assign itemIssuances = itemIssuancesPerItem.get(orderItem.get("orderItemSeqId"))?if_exists>
              <#if itemIssuances?has_content>
                <#list itemIssuances as itemIssuance>
                  <tr>
                    <td align="right" colspan="2">
                      <div class="tabletext" style="font-size: xx-small;">
                        <#if itemIssuance.shipmentId?has_content>
                          <b><i>${uiLabelMap.OrderIssuedToShipmentItem}</i>:</b>
                          <a target="facility" href="/facility/control/ViewShipment?shipmentId=${itemIssuance.shipmentId}&amp;externalLoginKey=${externalLoginKey}" class="buttontext" style="font-size: xx-small;">${itemIssuance.shipmentId}</a>:${itemIssuance.shipmentItemSeqId?if_exists}
                        <#else>
                          <b><i>${uiLabelMap.OrderIssuedWithoutShipment}</i></b>
                        </#if>
                      </div>
                    </td>
                    <td align="center">
                      <div class="tabletext" style="font-size: xx-small;">${itemIssuance.quantity?string.number}&nbsp;</div>
                    </td>
                    <td>&nbsp;</td>
                    <td>&nbsp;</td>
                    <td>&nbsp;</td>
                  </tr>
                </#list>
              </#if>

               <#-- now show item issuances (inventory item) per line item -->
               <#if itemIssuances?has_content>
                <#list itemIssuances as itemIssuance>
                  <tr>
                    <td align="right" colspan="2">
                      <div class="tabletext" style="font-size: xx-small;">
                        <#if itemIssuance.inventoryItemId?has_content>
                          <#assign inventoryItem = itemIssuance.getRelatedOne("InventoryItem")/>
                          <b><i>${uiLabelMap.FacilityInventory}</i>:</b>
                            <a href="/facility/control/EditInventoryItem?inventoryItemId=${itemIssuance.inventoryItemId}&amp;externalLoginKey=${externalLoginKey}" class="buttontext" style="font-size: xx-small;">${itemIssuance.inventoryItemId}</a>
                            <b><i>${uiLabelMap.OrderShipGroup}</i>:</b> ${itemIssuance.shipGroupSeqId}
                            <#if (inventoryItem.serialNumber?has_content)><br><b><i>${uiLabelMap.SerialNumber}</li>:</b> ${inventoryItem.serialNumber}&nbsp;</#if>                                                      

                        </#if>
                      </div>
                    </td>
                    <td align="center">
                      <div class="tabletext" style="font-size: xx-small;">${itemIssuance.quantity?string.number}&nbsp;</div>
                    </td>
                    <td>&nbsp;</td>
                    <td>&nbsp;</td>
                    <td>&nbsp;</td>
                  </tr>
                </#list>
              </#if>
            </#list>
          </#if>
          <tr><td colspan="8"><hr class="sepbar"/></td></tr>
          <#list orderHeaderAdjustments as orderHeaderAdjustment>
            <#assign adjustmentType = orderHeaderAdjustment.getRelatedOne("OrderAdjustmentType")>
            <#assign adjustmentAmount = Static["org.ofbiz.order.order.OrderReadHelper"].calcOrderAdjustment(orderHeaderAdjustment, orderSubTotal)>
            <#if adjustmentAmount != 0>
              <tr>
                <td align="right" colspan="5">					
                  <div class="tabletext"><b>${adjustmentType.get("description",locale)}</b> ${orderHeaderAdjustment.comments?if_exists}  ${orderHeaderAdjustment.get("description")?if_exists} : </div>
                </td>
                <td align="right" nowrap="nowrap">
                  <div class="tabletext"><@ofbizCurrency amount=adjustmentAmount isoCode=currencyUomId/></div>
                </td>
                <td>&nbsp;</td>
              </tr>
            </#if>
          </#list>

          <#-- subtotal -->
          <tr><td colspan=1></td><td colspan="8"><hr class="sepbar"/></td></tr>
          <tr>
            <td align="right" colspan="5"><div class="tabletext"><b>${uiLabelMap.OrderItemsSubTotal}</b></div></td>
            <td align="right" nowrap="nowrap"><div class="tabletext"><@ofbizCurrency amount=orderSubTotal isoCode=currencyUomId/></div></td>
          </tr>

          <#-- other adjustments -->
          <tr>
            <td align="right" colspan="5"><div class="tabletext"><b>${uiLabelMap.OrderTotalOtherOrderAdjustments}</b></div></td>
            <td align="right" nowrap="nowrap"><div class="tabletext"><@ofbizCurrency amount=otherAdjAmount isoCode=currencyUomId/></div></td>
          </tr>

          <#-- shipping adjustments -->
          <tr>
            <td align="right" colspan="5"><div class="tabletext"><b>${uiLabelMap.OrderTotalShippingAndHandling}</b></div></td>
            <td align="right" nowrap="nowrap"><div class="tabletext"><@ofbizCurrency amount=shippingAmount isoCode=currencyUomId/></div></td>
          </tr>

              <#-- tax adjustments -->
          <tr>
            <td align="right" colspan="5"><div class="tabletext"><b>${uiLabelMap.OrderTotalSalesTax}</b></div></td>
            <td align="right" nowrap="nowrap"><div class="tabletext"><@ofbizCurrency amount=taxAmount isoCode=currencyUomId/></div></td>
          </tr>

          <#-- grand total -->
          <tr>
            <td align="right" colspan="5"><div class="tabletext"><b>${uiLabelMap.OrderTotalDue}</b></div></td>
            <td align="right" nowrap="nowrap">
              <div class="tabletext"><@ofbizCurrency amount=grandTotal isoCode=currencyUomId/></div>
            </td>
          </tr>
        </table>
    </div>
</div>

</#if>
