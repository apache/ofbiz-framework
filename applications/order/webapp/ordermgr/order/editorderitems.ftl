<#--
 *  Copyright (c) 2003-2005 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *@author     Andy Zeneski (jaz@ofbiz.org)
 *@author     Jean-Luc.Malet@nereide.biz (migration to uiLabelMap)
 *@version    $Rev$
 *@since      2.2
-->

<#if orderHeader?has_content>

<div class="screenlet">
    <div class="screenlet-header">
        <div style="float: right;">
            <#if security.hasEntityPermission("ORDERMGR", "_UPDATE", session)>
                <#if orderHeader?has_content && orderHeader.statusId != "ORDER_CANCELLED" && orderHeader.statusId != "ORDER_COMPLETED">
                    <div class="tabletext"><a href="<@ofbizUrl>cancelOrderItem?${paramString}</@ofbizUrl>" class="submenutext">${uiLabelMap.OrderCancelAllItems}</a><a href="<@ofbizUrl>orderview?${paramString}</@ofbizUrl>" class="submenutextright">${uiLabelMap.OrderViewOrder}</a></div>
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
            <td width="5%" align="right"><div class="tableheadtext">${uiLabelMap.OrderQuantity}</div></td>
            <td width="10%" align="right"><div class="tableheadtext">${uiLabelMap.OrderUnitPrice}</div></td>
            <td width="10%" align="right"><div class="tableheadtext">${uiLabelMap.OrderAdjustments}</div></td>
            <td width="10%" align="right"><div class="tableheadtext">${uiLabelMap.OrderSubTotal}</div></td>
            <td width="5%">&nbsp;</td>
          </tr>
          <#if !orderItemList?has_content>
            <tr><td><font color="red">${uiLabelMap.checkhelper_sales_order_lines_lookup_failed}</font></td></tr>
          <#else>
            <form name="updateItemInfo" method="post" action="<@ofbizUrl>updateOrderItems?${paramString}</@ofbizUrl>"/>
            <#list orderItemList as orderItem>
              <#assign orderItemContentWrapper = Static["org.ofbiz.order.order.OrderContentWrapper"].makeOrderContentWrapper(orderItem, request)>
              <tr><td colspan="8"><hr class="sepbar"></td></tr>
              <tr>
                <#assign orderItemType = orderItem.getRelatedOne("OrderItemType")?if_exists>
                <#assign productId = orderItem.productId?if_exists>
                <#if productId?exists && productId == "shoppingcart.CommentLine">
                  <td colspan="1" valign="top">
                    <b><div class="tabletext"> &gt;&gt; ${orderItem.itemDescription}</div></b>
                  </td>
                <#else>
                  <td valign="top">
                    <div class="tabletext">
                      <#if productId?exists>
                        ${orderItem.productId?default("N/A")} - ${orderItem.itemDescription?if_exists}
                      <#elseif orderItemType?exists>
                        ${orderItemType.description} - ${orderItem.itemDescription?if_exists}
                      <#else>
                        ${orderItem.itemDescription?if_exists}
                      </#if>
                    </div>
                    <#if productId?exists>
                      <div class="tabletext">
                        <a href="/catalog/control/EditProduct?productId=${productId}" class="buttontext" target="_blank">${uiLabelMap.ProductCatalog}</a>
                        <a href="/ecommerce/control/product?product_id=${productId}" class="buttontext" target="_blank">${uiLabelMap.EcommerceEcommerce}</a>
                        <#if orderItemContentWrapper.get("IMAGE_URL")?has_content>
                          <a href="<@ofbizUrl>viewimage?orderId=${orderId}&orderItemSeqId=${orderItem.orderItemSeqId}&orderContentTypeId=IMAGE_URL</@ofbizUrl>" target="_orderImage" class="buttontext">${uiLabelMap.OrderViewImage}</a>
                        </#if>
                      </div>
                    </#if>
                  </td>

                  <#-- now show status details per line item -->
                  <#assign currentItemStatus = orderItem.getRelatedOne("StatusItem")>
                  <td align="left" colspan="1">
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
                  <td align="right" valign="top" nowrap>
                    <#assign remainingQuantity = (orderItem.quantity?default(0) - orderItem.cancelQuantity?default(0))>
                    <div class="tabletext">${uiLabelMap.OrderOrdered}:&nbsp;${orderItem.quantity?default(0)?string.number}&nbsp;&nbsp;</div>
                    <div class="tabletext">${uiLabelMap.OrderCancelled}:&nbsp;${orderItem.cancelQuantity?default(0)?string.number}&nbsp;&nbsp;</div>
                    <div class="tabletext">${uiLabelMap.OrderRemaining}:&nbsp;${remainingQuantity}&nbsp;&nbsp;</div>
                  </td>
                  <td align="right" valign="top" nowrap>
                    <div class="tabletext" nowrap>
                      <input style="textBox" type="text" size="8" name="ipm_${orderItem.orderItemSeqId}" value="<@ofbizAmount amount=orderItem.unitPrice/>"/>
                      &nbsp;<input type="checkbox" name="opm_${orderItem.orderItemSeqId}" value="Y"/>
                    </div>
                  </td>
                  <td align="right" valign="top" nowrap>
                    <div class="tabletext" nowrap><@ofbizCurrency amount=Static["org.ofbiz.order.order.OrderReadHelper"].getOrderItemAdjustmentsTotal(orderItem, orderAdjustments, true, false, false) isoCode=currencyUomId/></div>
                  </td>
                  <td align="right" valign="top" nowrap>
                    <#if orderItem.statusId != "ITEM_CANCELLED">
                      <div class="tabletext" nowrap><@ofbizCurrency amount=Static["org.ofbiz.order.order.OrderReadHelper"].getOrderItemSubTotal(orderItem, orderAdjustments) isoCode=currencyUomId/></div>
                    <#else>
                      <div class="tabletext" nowrap><@ofbizCurrency amount=0.00 isoCode=currencyUomId/></div>
                    </#if>
                  </td>
                  <td>&nbsp;</td>
                  <td align="right" valign="top" nowrap>
                    <#if (security.hasEntityPermission("ORDERMGR", "_ADMIN", session) && orderItem.statusId != "ITEM_CANCELLED" && orderItem.statusId != "ITEM_COMPLETED") || (security.hasEntityPermission("ORDERMGR", "_UPDATE", session) && orderItem.statusId != "ITEM_CANCELLED" && orderItem.statusId != "ITEM_COMPLETED" && orderHeader.statusId != "ORDER_SENT")>
                      <div class="tabletext"><a href="<@ofbizUrl>cancelOrderItem?item_seq=${orderItem.orderItemSeqId}&${paramString}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonCancelAll}</a></div>
                    <#else>
                      &nbsp;
                    </#if>
                  </td>
                </#if>
              </tr>

              <#-- now show adjustment details per line item -->
              <#assign orderItemAdjustments = Static["org.ofbiz.order.order.OrderReadHelper"].getOrderItemAdjustmentList(orderItem, orderAdjustments)>
              <#if orderItemAdjustments?exists && orderItemAdjustments?has_content>
                <#list orderItemAdjustments as orderItemAdjustment>
                  <#assign adjustmentType = orderItemAdjustment.getRelatedOneCache("OrderAdjustmentType")>
                  <tr>
                    <td align="right" colspan="2">
                      <div class="tabletext" style="font-size: xx-small;">
                        <b><i>${uiLabelMap.OrderAdjustment}</i>:</b> <b>${adjustmentType.get("description",locale)}</b>:
                        ${orderItemAdjustment.get("description",locale)?if_exists} (${orderItemAdjustment.comments?default("")})

                        <#if orderItemAdjustment.orderAdjustmentTypeId == "SALES_TAX">
                          <#if orderItemAdjustment.primaryGeoId?has_content>
                            <#assign primaryGeo = orderItemAdjustment.getRelatedOneCache("PrimaryGeo")/>
                            <b>${uiLabelMap.OrderJurisdiction}:</b> ${primaryGeo.geoName} [${primaryGeo.abbreviation?if_exists}]
                            <#if orderItemAdjustment.secondaryGeoId?has_content>
                              <#assign secondaryGeo = orderItemAdjustment.getRelatedOneCache("SecondaryGeo")/>
                              (<b>in:</b> ${secondaryGeo.geoName} [${secondaryGeo.abbreviation?if_exists}])
                            </#if>
                          </#if>
                          <#if orderItemAdjustment.sourcePercentage?exists><b>Rate:</b> ${orderItemAdjustment.sourcePercentage}</#if>
                          <#if orderItemAdjustment.customerReferenceId?has_content><b>Customer Tax ID:</b> ${orderItemAdjustment.customerReferenceId}</#if>
                          <#if orderItemAdjustment.exemptAmount?exists><b>Exempt Amount:</b> ${orderItemAdjustment.exemptAmount}</#if>
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

              <#-- now show ship group info per line item -->
              <#assign orderItemShipGroupAssocs = orderItem.getRelated("OrderItemShipGroupAssoc")?if_exists>
              <#if orderItemShipGroupAssocs?has_content>
                <tr><td>&nbsp;</td></tr>
                <#list orderItemShipGroupAssocs as shipGroupAssoc>
                  <#assign shipGroup = shipGroupAssoc.getRelatedOne("OrderItemShipGroup")>
                  <#assign shipGroupAddress = shipGroup.getRelatedOne("PostalAddress")?if_exists>
                  <tr>
                    <td align="right" colspan="2">
                      <div class="tabletext" style="font-size: xx-small;"><b><i>${uiLabelMap.OrderShipGroup}</i>:</b> [${shipGroup.shipGroupSeqId}] ${shipGroupAddress.address1?default("${uiLabelMap.OrderNotShipped}")}</div>
                    </td>
                    <td align="center">
                      <div class="tabletext" style="font-size: xx-small;"><input type="text" style="textBox" name="iqm_${shipGroupAssoc.orderItemSeqId}:${shipGroupAssoc.shipGroupSeqId}" size="6" value="${shipGroupAssoc.quantity?string.number}"/></div>
                    </td>
                    <td>&nbsp;</td>
                    <td>&nbsp;</td>
                    <td>&nbsp;</td>
                    <td>&nbsp;</td>
                    <td align="right" valign="top" nowrap>
                      <#assign itemStatusOkay = (orderItem.statusId != "ITEM_CANCELLED" && orderItem.statusId != "ITEM_COMPLETED" && (shipGroupAssoc.cancelQuantity?default(0) < shipGroupAssoc.quantity?default(0)))>
                      <#if (security.hasEntityPermission("ORDERMGR", "_ADMIN", session) && itemStatusOkay) || (security.hasEntityPermission("ORDERMGR", "_UPDATE", session) && itemStatusOkay && orderHeader.statusId != "ORDER_SENT")>
                        <div class="tabletext"><a href="<@ofbizUrl>cancelOrderItem?item_seq=${orderItem.orderItemSeqId}&group_seq=${shipGroup.shipGroupSeqId}&${paramString}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonCancel}</a></div>
                      <#else>
                        &nbsp;
                      </#if>
                    </td>
                  </tr>
                </#list>
              </#if>
            </#list>

            <tr>
              <td align="right" colspan="8">
                <input type="submit" value="${uiLabelMap.OrderUpdateItems}"/>
              </td>
            </tr>
            </form>
          </#if>
          <tr><td colspan="8"><hr class="sepbar"></td></tr>
          <#list orderHeaderAdjustments as orderHeaderAdjustment>
            <#assign adjustmentType = orderHeaderAdjustment.getRelatedOne("OrderAdjustmentType")>
            <#assign adjustmentAmount = Static["org.ofbiz.order.order.OrderReadHelper"].calcOrderAdjustment(orderHeaderAdjustment, orderSubTotal)>
            <#if adjustmentAmount != 0>
              <tr>
                <td align="right" colspan="5">
                  <div class="tabletext"><b>${adjustmentType.get("description",locale)}</b> : ${orderHeaderAdjustment.comments?if_exists}</div>
                </td>
                <td align="right" nowrap>
                  <div class="tabletext"><@ofbizCurrency amount=adjustmentAmount isoCode=currencyUomId/></div>
                </td>
                <td>&nbsp;</td>
              </tr>
            </#if>
          </#list>

          <#-- add new adjustment -->
          <#if security.hasEntityPermission("ORDERMGR", "_UPDATE", session) && orderHeader.statusId != "ORDER_COMPLETED" && orderHeader.statusId != "ORDER_CANCELLED" && orderHeader.statusId != "ORDER_REJECTED">
            <tr>
              <form name="addAdjustmentForm" method="post" action="<@ofbizUrl>addOrderAdjustment?${paramString}</@ofbizUrl>">
                <input type="hidden" name="comments" value="Added manually by [${userLogin.userLoginId}]"/>
                <td align="right" colspan="5">
                  <select name="orderAdjustmentTypeId" class="selectBox">
                    <#list orderAdjustmentTypes as type>
                      <option value="${type.orderAdjustmentTypeId}">${type.get("description",locale)?default(type.orderAdjustmentTypeId)}</option>
                    </#list>
                  </select>
                </td>
                <td align="right">
                  <input type="text" name="amount" size="6" value="<@ofbizAmount amount=0.00/>" class="inputBox"/>
                </td>
                <td align="right" colspan="2">
                  <a href="javascript:document.addAdjustmentForm.submit();" class="buttontext">${uiLabelMap.CommonAdd}</a>
                </td>
              </form>
            </tr>
          </#if>

          <#-- subtotal -->
          <tr><td colspan=1></td><td colspan="8"><hr class="sepbar"></td></tr>
          <tr>
            <td align="right" colspan="5"><div class="tabletext"><b>${uiLabelMap.OrderItemsSubTotal}</b></div></td>
            <td align="right" nowrap><div class="tabletext"><@ofbizCurrency amount=orderSubTotal isoCode=currencyUomId/></div></td>
          </tr>

          <#-- other adjustments -->
          <tr>
            <td align="right" colspan="5"><div class="tabletext"><b>${uiLabelMap.OrderTotalOtherOrderAdjustments}</b></div></td>
            <td align="right" nowrap><div class="tabletext"><@ofbizCurrency amount=otherAdjAmount isoCode=currencyUomId/></div></td>
          </tr>

          <#-- shipping adjustments -->
          <tr>
            <td align="right" colspan="5"><div class="tabletext"><b>${uiLabelMap.OrderTotalShippingAndHandling}</b></div></td>
            <td align="right" nowrap><div class="tabletext"><@ofbizCurrency amount=shippingAmount isoCode=currencyUomId/></div></td>
          </tr>

              <#-- tax adjustments -->
          <tr>
            <td align="right" colspan="5"><div class="tabletext"><b>${uiLabelMap.OrderTotalSalesTax}</b></div></td>
            <td align="right" nowrap><div class="tabletext"><@ofbizCurrency amount=taxAmount isoCode=currencyUomId/></div></td>
          </tr>

          <#-- grand total -->
          <tr>
            <td align="right" colspan="5"><div class="tabletext"><b>${uiLabelMap.OrderTotalDue}</b></div></td>
            <td align="right" nowrap>
              <div class="tabletext"><@ofbizCurrency amount=grandTotal isoCode=currencyUomId/></div>
            </td>
          </tr>
        </table>
    </div>
</div>

</#if>
