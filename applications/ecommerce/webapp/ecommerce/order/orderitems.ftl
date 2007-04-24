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

<#-- NOTE: this template is used for the orderstatus screen in ecommerce AND for order notification emails through the OrderNoticeEmail.ftl file -->
<#-- the "urlPrefix" value will be prepended to URLs by the ofbizUrl transform if/when there is no "request" object in the context -->
<#if baseEcommerceSecureUrl?exists><#assign urlPrefix = baseEcommerceSecureUrl/></#if>

<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxlink">
            <#assign numColumns = 8>
            <#if maySelectItems?default("N") == "Y">
                <#assign numColumns = 11>
                <a href="javascript:document.addCommonToCartForm.add_all.value='true';document.addCommonToCartForm.submit()" class="submenutext">${uiLabelMap.EcommerceAddAlltoCart}</a><a href="javascript:document.addCommonToCartForm.add_all.value='false';document.addCommonToCartForm.submit()" class="submenutext">${uiLabelMap.EcommerceAddCheckedToCart}</a><a href="<@ofbizUrl>createShoppingListFromOrder?orderId=${orderHeader.orderId}&frequency=6&intervalNumber=1&shoppingListTypeId=SLT_AUTO_REODR</@ofbizUrl>" class="submenutextright">${uiLabelMap.OrderSendMeThisEveryMonth}</a>
            </#if>
        </div>
        <div class="boxhead">&nbsp; ${uiLabelMap.OrderOrderItems}</div>
    </div>
    <div class="screenlet-body">
        <table width="100%" border="0" cellpadding="0">
          <tr align="left" valign="bottom">
            <td width="35%" align="left"><span class="tableheadtext"><b>${uiLabelMap.EcommerceProduct}</b></span></td>               
            <#if maySelectItems?default("N") == "Y">
              <td width="10%" align="right"><span class="tableheadtext"><b>${uiLabelMap.OrderQtyOrdered}</b></span></td>
              <td width="10%" align="right"><span class="tableheadtext"><b>${uiLabelMap.OrderQtyPicked}</b></span></td>
              <td width="10%" align="right"><span class="tableheadtext"><b>${uiLabelMap.OrderQtyShipped}</b></span></td>
              <td width="10%" align="right"><span class="tableheadtext"><b>${uiLabelMap.OrderQtyCanceled}</b></span></td>
            <#else>
              <td width="10%" align="right">&nbsp;</td>
              <td width="10%" align="right">&nbsp;</td>
              <td width="10%" align="right">&nbsp;</td>
              <td width="10%" align="right"><span class="tableheadtext"><b>${uiLabelMap.OrderQtyOrdered}</b></span></td>
            </#if>
            <td width="10%" align="right"><span class="tableheadtext"><b>${uiLabelMap.EcommerceUnitPrice}</b></span></td>
            <td width="10%" align="right"><span class="tableheadtext"><b>${uiLabelMap.OrderAdjustments}</b></span></td>
            <td width="10%" align="right"><span class="tableheadtext"><b>${uiLabelMap.CommonSubtotal}</b></span></td>
            <#if maySelectItems?default("N") == "Y">
              <td colspan="3" width="5%" align="right">&nbsp;</td>
            </#if>
          </tr>
          <#list orderItems as orderItem>
            <#-- get info from workeffort and calculate rental quantity, if it was a rental item -->
            <#assign rentalQuantity = 1> <#-- no change if no rental item -->
            <#if orderItem.orderItemTypeId == "RENTAL_ORDER_ITEM" && workEfforts?exists>
                <#list workEfforts as workEffort>
                    <#if workEffort.workEffortId == orderItem.orderItemSeqId>
                        <#assign rentalQuantity = localOrderReadHelper.getWorkEffortRentalQuantity(workEffort)>
                        <#assign workEffortSave = workEffort>
                      <#break>
                      </#if>
                  </#list>
              <#else> 
                  <#assign WorkOrderItemFulfillments = orderItem.getRelated("WorkOrderItemFulfillment")?if_exists>
                  <#if WorkOrderItemFulfillments?has_content>
                    <#list WorkOrderItemFulfillments as WorkOrderItemFulfillment>
                      <#assign workEffortSave = WorkOrderItemFulfillment.getRelatedOneCache("WorkEffort")?if_exists>
                      <#break>
                     </#list>
                  </#if>
            </#if>
            <tr><td colspan="${numColumns}"><hr class="sepbar"/></td></tr>
            <tr>
              <#if !orderItem.productId?exists || orderItem.productId == "_?_">
                <td valign="top">
                  <b><div class="tabletext"> &gt;&gt; ${orderItem.itemDescription}</div></b>
                </td>
              <#else>
                <#assign product = orderItem.getRelatedOneCache("Product")?if_exists/> <#-- should always exist because of FK constraint, but just in case -->
                <td valign="top">
                  <div class="tabletext">
                    <a href="<@ofbizUrl>product?product_id=${orderItem.productId}</@ofbizUrl>" class="linktext">${orderItem.productId} - ${orderItem.itemDescription}</a>
                  </div>
                  <div class="tabletext" style="font-size: xx-small;">
                    <#if product?has_content>
                      <#if product.piecesIncluded?exists && product.piecesIncluded?long != 0>
                          [${uiLabelMap.EcommercePieces}: ${product.piecesIncluded}]
                      </#if>
                      <#if (product.quantityIncluded?exists && product.quantityIncluded?double != 0) || product.quantityUomId?has_content>
                        <#assign quantityUom = product.getRelatedOneCache("QuantityUom")?if_exists/>
                          [${uiLabelMap.CommonQuantity}: ${product.quantityIncluded?if_exists} ${((quantityUom.abbreviation)?default(product.quantityUomId))?if_exists}]
                      </#if>
                      <#if (product.weight?exists && product.weight?double != 0) || product.weightUomId?has_content>
                        <#assign weightUom = product.getRelatedOneCache("WeightUom")?if_exists/>
                          [${uiLabelMap.CommonWeight}: ${product.weight?if_exists} ${((weightUom.abbreviation)?default(product.weightUomId))?if_exists}]
                      </#if>
                      <#if (product.productHeight?exists && product.productHeight?double != 0) || product.heightUomId?has_content>
                        <#assign heightUom = product.getRelatedOneCache("HeightUom")?if_exists/>
                          [${uiLabelMap.CommonHeight}: ${product.productHeight?if_exists} ${((heightUom.abbreviation)?default(product.heightUomId))?if_exists}]
                      </#if>
                      <#if (product.productWidth?exists && product.productWidth?double != 0) || product.widthUomId?has_content>
                        <#assign widthUom = product.getRelatedOneCache("WidthUom")?if_exists/>
                          [${uiLabelMap.CommonWidth}: ${product.productWidth?if_exists} ${((widthUom.abbreviation)?default(product.widthUomId))?if_exists}]
                      </#if>
                      <#if (product.productDepth?exists && product.productDepth?double != 0) || product.depthUomId?has_content>
                        <#assign depthUom = product.getRelatedOneCache("DepthUom")?if_exists/>
                          [${uiLabelMap.CommonDepth}: ${product.productDepth?if_exists} ${((depthUom.abbreviation)?default(product.depthUomId))?if_exists}]
                      </#if>
                    </#if>
                  </div>
                  <#if maySelectItems?default("N") == "Y">
                    <#assign returns = orderItem.getRelated("ReturnItem")?if_exists>
                    <#if returns?has_content>
                      <#list returns as return>
                        <#assign returnHeader = return.getRelatedOne("ReturnHeader")>
                        <#if returnHeader.statusId != "RETURN_CANCELLED">
                          <#if returnHeader.statusId == "RETURN_REQUESTED" || returnHeader.statusId == "RETURN_APPROVED">
                            <#assign displayState = "Return Pending">
                          <#else>
                            <#assign displayState = "Returned">
                          </#if>
                          <div class="tabletext"><font color="red"><b>${displayState}</b></font> (#${return.returnId})</div>
                        </#if>
                      </#list>
                    </#if>
                  </#if>
                </td>
                <#if !(maySelectItems?default("N") == "Y")>
                  <td>&nbsp;</td>
                  <td>&nbsp;</td>
                  <td>&nbsp;</td>
                </#if>
                <td align="right" valign="top">
                  <div class="tabletext">${orderItem.quantity?string.number}</div>                        
                </td>
                <#if maySelectItems?default("N") == "Y">
                <td align="right" valign="top">
                  <#assign pickedQty = localOrderReadHelper.getItemPickedQuantityBd(orderItem)>
                  <div class="tabletext"><#if pickedQty gt 0 && orderHeader.statusId == "ORDER_APPROVED"><font color="red">${pickedQty?default(0)?string.number}</font><#else>${pickedQty?default(0)?string.number}</#if>
                  </div>
                </td>
                <td align="right" valign="top">
                  <#assign shippedQty = localOrderReadHelper.getItemShippedQuantity(orderItem)>
                  <div class="tabletext">${shippedQty?default(0)?string.number}</div>
                </td>
                <td align="right" valign="top">
                  <#assign canceledQty = localOrderReadHelper.getItemCanceledQuantity(orderItem)>
                  <div class="tabletext">${canceledQty?default(0)?string.number}</div>
                </td>
                </#if>
                <td align="right" valign="top">
                  <div class="tabletext"><@ofbizCurrency amount=orderItem.unitPrice isoCode=currencyUomId/></div>
                </td>
                <td align="right" valign="top">
                  <div class="tabletext"><@ofbizCurrency amount=localOrderReadHelper.getOrderItemAdjustmentsTotal(orderItem) isoCode=currencyUomId/></div>
                </td>
                <td align="right" valign="top">
                <#if workEfforts?exists>
                   <div class="tabletext"><@ofbizCurrency amount=localOrderReadHelper.getOrderItemTotal(orderItem)*rentalQuantity isoCode=currencyUomId/></div>
                <#else>                                          
                  <div class="tabletext"><@ofbizCurrency amount=localOrderReadHelper.getOrderItemTotal(orderItem) isoCode=currencyUomId/></div>
                </#if>
                </td>                    
                <#if maySelectItems?default("N") == "Y">
                  <td>&nbsp;</td>
                  <#if (orderHeader.statusId != "ORDER_SENT" && orderItem.statusId != "ITEM_COMPLETED" && orderItem.statusId != "ITEM_CANCELLED" && pickedQty == 0)>
                    <td><a href="<@ofbizUrl>cancelOrderItem?orderId=${orderItem.orderId}&amp;orderItemSeqId=${orderItem.orderItemSeqId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonCancel}</a></td>
                  <#else>
                    <td>&nbsp;</td>
                  </#if>
                  <td>
                    <input name="item_id" value="${orderItem.orderItemSeqId}" type="checkbox"/>
                  </td>
                </#if>
              </#if>
            </tr>
            <#-- show info from workeffort if it was a rental item -->
            <#if orderItem.orderItemTypeId == "RENTAL_ORDER_ITEM">
                <#if workEffortSave?exists>
                      <tr><td>&nbsp;</td><td colspan="${numColumns}"><div class="tabletext">${uiLabelMap.CommonFrom}: ${workEffortSave.estimatedStartDate?string("yyyy-MM-dd")} ${uiLabelMap.CommonUntil} ${workEffortSave.estimatedCompletionDate?string("yyyy-MM-dd")} ${uiLabelMap.CommonFor} ${workEffortSave.reservPersons} ${uiLabelMap.CommonPerson}(s).</div></td></tr>
                  </#if>
            </#if>
            <#-- now show adjustment details per line item -->
            <#assign itemAdjustments = localOrderReadHelper.getOrderItemAdjustments(orderItem)>
            <#list itemAdjustments as orderItemAdjustment>
              <tr>
                <td align="right">
                  <div class="tabletext" style="font-size: xx-small;">
                    <b><i>${uiLabelMap.EcommerceAdjustment}</i>:</b> <b>${localOrderReadHelper.getAdjustmentType(orderItemAdjustment)}</b>&nbsp;
                    <#if orderItemAdjustment.description?has_content>: ${orderItemAdjustment.description}</#if>

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
                      <#if orderItemAdjustment.sourcePercentage?exists><b>${uiLabelMap.EcommerceRate}:</b> ${orderItemAdjustment.sourcePercentage}</#if>
                      <#if orderItemAdjustment.customerReferenceId?has_content><b>${uiLabelMap.CustomerTaxID}:</b> ${orderItemAdjustment.customerReferenceId}</#if>
                      <#if orderItemAdjustment.exemptAmount?exists><b>${uiLabelMap.EcommerceExemptAmount}:</b> ${orderItemAdjustment.exemptAmount}</#if>
                    </#if>
                  </div>
                </td>
                <td colspan="5">&nbsp;</td>
                <td align="right">
                  <div class="tabletext" style="font-size: xx-small;"><@ofbizCurrency amount=localOrderReadHelper.getOrderItemAdjustmentTotal(orderItem, orderItemAdjustment) isoCode=currencyUomId/></div>
                </td>
                <td>&nbsp;</td>
                <#if maySelectItems?default("N") == "Y"><td colspan="3">&nbsp;</td></#if>
              </tr>
            </#list>

            <#-- show the order item ship group info -->
            <#assign orderItemShipGroupAssocs = orderItem.getRelated("OrderItemShipGroupAssoc")?if_exists>
            <#if orderItemShipGroupAssocs?has_content>
              <#list orderItemShipGroupAssocs as shipGroupAssoc>
                <#assign shipGroup = shipGroupAssoc.getRelatedOne("OrderItemShipGroup")?if_exists>
                <#assign shipGroupAddress = (shipGroup.getRelatedOne("PostalAddress"))?if_exists>
                <tr>
                  <td align="right">
                    <div class="tabletext" style="font-size: xx-small;"><b><i>${uiLabelMap.OrderShipGroup}</i>:</b> [${shipGroup.shipGroupSeqId}] ${shipGroupAddress.address1?default("N/A")}</div>
                  </td>
                  <td align="right">
                    <div class="tabletext" style="font-size: xx-small;">${shipGroupAssoc.quantity?string.number}</div>
                  </td>
                  <td colspan="${numColumns - 2}">&nbsp;</td>
                </tr>
              </#list>
            </#if>

           </#list>
           <#if orderItems?size == 0 || !orderItems?has_content>
             <tr><td colspan="${numColumns}"><font color="red">${uiLabelMap.OrderSalesOrderLookupFailed}</font></td></tr>
           </#if>

          <tr><td colspan="${numColumns}"><hr class="sepbar"/></td></tr>
          <tr>
            <td align="right" colspan="7"><div class="tabletext"><b>${uiLabelMap.CommonSubtotal}</b></div></td>
            <td align="right"><div class="tabletext"><@ofbizCurrency amount=orderSubTotal isoCode=currencyUomId/></div></td>
            <#if maySelectItems?default("N") == "Y"><td colspan="3">&nbsp;</td></#if>
          </tr>
          <#list headerAdjustmentsToShow as orderHeaderAdjustment>
            <tr>
              <td align="right" colspan="7"><div class="tabletext"><b>${localOrderReadHelper.getAdjustmentType(orderHeaderAdjustment)}</b></div></td>
              <td align="right"><div class="tabletext"><@ofbizCurrency amount=localOrderReadHelper.getOrderAdjustmentTotal(orderHeaderAdjustment) isoCode=currencyUomId/></div></td>
              <#if maySelectItems?default("N") == "Y"><td colspan="3">&nbsp;</td></#if>
            </tr>
          </#list>
          <tr>
            <td align="right" colspan="7"><div class="tabletext"><b>${uiLabelMap.OrderShippingAndHandling}</b></div></td>
            <td align="right"><div class="tabletext"><@ofbizCurrency amount=orderShippingTotal isoCode=currencyUomId/></div></td>
            <#if maySelectItems?default("N") == "Y"><td colspan="3">&nbsp;</td></#if>
          </tr>
          <tr>
            <td align="right" colspan="7"><div class="tabletext"><b>${uiLabelMap.OrderSalesTax}</b></div></td>
            <td align="right"><div class="tabletext"><@ofbizCurrency amount=orderTaxTotal isoCode=currencyUomId/></div></td>
            <#if maySelectItems?default("N") == "Y"><td colspan="3">&nbsp;</td></#if>
          </tr>

          <tr>
            <td colspan="3"></td>
            <#if maySelectItems?default("N") == "Y">
                <td colspan="${numColumns - 6}"><hr class="sepbar"/></td>
                <td colspan="3">&nbsp;</td>
            <#else>
                <td colspan="${numColumns - 3}"><hr class="sepbar"/></td>
            </#if>
          </tr>
          <tr>
            <td align="right" colspan="7"><div class="tabletext"><b>${uiLabelMap.OrderGrandTotal}</b></div></td>
            <td align="right">
              <div class="tabletext"><@ofbizCurrency amount=orderGrandTotal isoCode=currencyUomId/></div>
            </td>
            <#if maySelectItems?default("N") == "Y"><td colspan="3">&nbsp;</td></#if>
          </tr>
        </table>
    </div>
</div>
