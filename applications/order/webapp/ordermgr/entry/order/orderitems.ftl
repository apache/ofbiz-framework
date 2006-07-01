<#--
 *  Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
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

<div class="screenlet">
    <div class="screenlet-header">
        <div style="float: right;">
            <#if maySelectItems?default(false)>
                <a href="javascript:document.addOrderToCartForm.add_all.value="true";document.addOrderToCartForm.submit()" class="lightbuttontext">${uiLabelMap.EcommerceAddAlltoCart}</a>
                <a href="javascript:document.addOrderToCartForm.add_all.value="false";document.addOrderToCartForm.submit()" class="lightbuttontext">${uiLabelMap.EcommerceAddCheckedtoCart}</a>
            </#if>
        </div>
        <div class="boxhead">&nbsp;${uiLabelMap.OrderOrderItems}</div>
    </div>
    <div class="screenlet-body">
        <table width="100%" border="0" cellpadding="0">
          <tr align="left" valign="bottom">
            <td width="65%" align="left"><span class="tableheadtext"><b>${uiLabelMap.ProductProduct}</b></span></td>
            <td width="5%" align="right"><span class="tableheadtext"><b>${uiLabelMap.OrderQuantity}</b></span></td>
            <td width="10%" align="right"><span class="tableheadtext"><b>${uiLabelMap.CommonUnitPrice}</b></span></td>
            <td width="10%" align="right"><span class="tableheadtext"><b>${uiLabelMap.OrderAdjustments}</b></span></td>
            <td width="10%" align="right"><span class="tableheadtext"><b>${uiLabelMap.OrderSubTotal}</b></span></td>
          </tr>
          <#list orderItems?if_exists as orderItem>
            <#assign itemType = orderItem.getRelatedOne("OrderItemType")>
            <tr><td colspan="6"><hr class="sepbar"/></td></tr>
            <tr>     
              <#if orderItem.productId?exists && orderItem.productId == "_?_">           
                <td colspan="1" valign="top">    
                  <b><div class="tabletext"> &gt;&gt; ${orderItem.itemDescription}</div></b>
                </td>
              <#else>                  
                <td valign="top">                      
                  <div class="tabletext"> 
                    <#if orderItem.productId?exists>                       
                      <a href="<@ofbizUrl>product?product_id=${orderItem.productId}</@ofbizUrl>" class="buttontext">${orderItem.productId} - ${orderItem.itemDescription}</a>
                    <#else>                                                    
                      <b>${itemType.description}</b> : ${orderItem.itemDescription?if_exists}
                    </#if>
                  </div>
                  
                </td>
                <td align="right" valign="top">
                  <div class="tabletext" nowrap>${orderItem.quantity?string.number}</div>
                </td>
                <td align="right" valign="top">
                  <div class="tabletext" nowrap><@ofbizCurrency amount=orderItem.unitPrice isoCode=currencyUomId/></div>
                </td>
                <td align="right" valign="top">
                  <div class="tabletext" nowrap><@ofbizCurrency amount=localOrderReadHelper.getOrderItemAdjustmentsTotal(orderItem) isoCode=currencyUomId/></div>
                </td>
                <td align="right" valign="top" nowrap>
                  <div class="tabletext"><@ofbizCurrency amount=localOrderReadHelper.getOrderItemSubTotal(orderItem) isoCode=currencyUomId/></div>
                </td>                    
                <#if maySelectItems?default(false)>
                  <td>                                 
                    <input name="item_id" value="${orderItem.orderItemSeqId}" type="checkbox">
                  </td>
                </#if>
              </#if>
            </tr>
            <#-- show info from workeffort if it was a rental item -->
            <#if orderItem.orderItemTypeId == "RENTAL_ORDER_ITEM">
                <#assign WorkOrderItemFulfillments = orderItem.getRelated("WorkOrderItemFulfillment")?if_exists>
                <#if WorkOrderItemFulfillments?has_content>
                    <#list WorkOrderItemFulfillments as WorkOrderItemFulfillment>
                        <#assign workEffort = WorkOrderItemFulfillment.getRelatedOneCache("WorkEffort")?if_exists>
                          <tr><td>&nbsp;</td><td>&nbsp;</td><td colspan="8"><div class="tabletext">${uiLabelMap.CommonFrom}: ${workEffort.estimatedStartDate?string("yyyy-MM-dd")} ${uiLabelMap.CommonTo}: ${workEffort.estimatedCompletionDate?string("yyyy-MM-dd")} ${uiLabelMap.EcommerceNbrPersons}: ${workEffort.reservPersons}</div></td></tr>
                        <#break><#-- need only the first one -->
                    </#list>
                </#if>
            </#if>

            <#-- now show adjustment details per line item -->
            <#assign itemAdjustments = localOrderReadHelper.getOrderItemAdjustments(orderItem)>
            <#list itemAdjustments as orderItemAdjustment>
              <tr>
                <td align="right">
                  <div class="tabletext" style="font-size: xx-small;">
                    <b><i>${uiLabelMap.OrderAdjustment}</i>:</b> <b>${localOrderReadHelper.getAdjustmentType(orderItemAdjustment)}</b>&nbsp;
                    <#if orderItemAdjustment.description?has_content>: ${orderItemAdjustment.get("description",locale)}</#if>

                    <#if orderItemAdjustment.orderAdjustmentTypeId == "SALES_TAX">
                      <#if orderItemAdjustment.primaryGeoId?has_content>
                        <#assign primaryGeo = orderItemAdjustment.getRelatedOneCache("PrimaryGeo")/>
                        <b>${uiLabelMap.OrderJurisdiction}:</b> ${primaryGeo.geoName} [${primaryGeo.abbreviation?if_exists}]
                        <#if orderItemAdjustment.secondaryGeoId?has_content>
                          <#assign secondaryGeo = orderItemAdjustment.getRelatedOneCache("SecondaryGeo")/>
                          (<b>in:</b> ${secondaryGeo.geoName} [${secondaryGeo.abbreviation?if_exists}])
                        </#if>
                      </#if>
                      <#if orderItemAdjustment.sourcePercentage?exists><b>${uiLabelMap.OrderRate}:</b> ${orderItemAdjustment.sourcePercentage}%</#if>
                      <#if orderItemAdjustment.customerReferenceId?has_content><b>${uiLabelMap.OrderCustomerTaxId}:</b> ${orderItemAdjustment.customerReferenceId}</#if>
                      <#if orderItemAdjustment.exemptAmount?exists><b>${uiLabelMap.OrderExemptAmount}:</b> ${orderItemAdjustment.exemptAmount}</#if>
                    </#if>
                  </div>
                </td>
                <td>&nbsp;</td>
                <td>&nbsp;</td>
                <td align="right">
                  <div class="tabletext" style="font-size: xx-small;"><@ofbizCurrency amount=localOrderReadHelper.getOrderItemAdjustmentTotal(orderItem, orderItemAdjustment) isoCode=currencyUomId/></div>
                </td>
                <td>&nbsp;</td>
                <td>&nbsp;</td>
                <#if maySelectItems?default(false)><td>&nbsp;</td></#if>
              </tr>
            </#list>
           </#list>
           <#if !orderItems?has_content>
             <tr><td><font color="red">${uiLabelMap.checkhelpertotalsdonotmatchordertotal}</font></td></tr>
           </#if>

          <tr><td colspan="8"><hr class="sepbar"/></td></tr>
          <tr>
            <td align="right" colspan="4"><div class="tabletext"><b>${uiLabelMap.OrderSubTotal}</b></div></td>
            <td align="right" nowrap><div class="tabletext">&nbsp;<#if orderSubTotal?exists><@ofbizCurrency amount=orderSubTotal isoCode=currencyUomId/></#if></div></td>
          </tr>              
          <#list headerAdjustmentsToShow?if_exists as orderHeaderAdjustment>
            <tr>
              <td align="right" colspan="4"><div class="tabletext"><b>${localOrderReadHelper.getAdjustmentType(orderHeaderAdjustment)}</b></div></td>
              <td align="right" nowrap><div class="tabletext"><@ofbizCurrency amount=localOrderReadHelper.getOrderAdjustmentTotal(orderHeaderAdjustment) isoCode=currencyUomId/></div></td>
            </tr>
          </#list>                 
          <tr>
            <td align="right" colspan="4"><div class="tabletext"><b>${uiLabelMap.FacilityShippingAndHandling}</b></div></td>
            <td align="right" nowrap><div class="tabletext"><#if orderShippingTotal?exists><@ofbizCurrency amount=orderShippingTotal isoCode=currencyUomId/></#if></div></td>
          </tr>              
          <tr>
            <td align="right" colspan="4"><div class="tabletext"><b>${uiLabelMap.OrderSalesTax}</b></div></td>
            <td align="right" nowrap><div class="tabletext"><#if orderTaxTotal?exists><@ofbizCurrency amount=orderTaxTotal isoCode=currencyUomId/></#if></div></td>
          </tr>
          
          <tr><td colspan=2></td><td colspan="8"><hr class="sepbar"/></td></tr>
          <tr>
            <td align="right" colspan="4"><div class="tabletext"><b>${uiLabelMap.OrderGrandTotal}</b></div></td>
            <td align="right" nowrap>
              <div class="tabletext"><#if orderGrandTotal?exists><@ofbizCurrency amount=orderGrandTotal isoCode=currencyUomId/></#if></div>
            </td>
          </tr>
        </table>
    </div>
</div>
