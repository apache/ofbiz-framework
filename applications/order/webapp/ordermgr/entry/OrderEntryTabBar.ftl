<#--
 *  Copyright (c) 2003 The Open For Business Project - www.ofbiz.org
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
 *@author     David E. Jones (jonesde@ofbiz.org)
 *@author     Andy Zeneski (jaz@ofbiz.org)
 *@author     Jean-Luc.Malet@nereide.biz (migration to uiLabelMap)
 *@version    $Rev$
 *@since      2.2
-->


<div class="boxtop">
    <div class="boxhead-right" align="right">
        <a href="<@ofbizUrl>emptycart</@ofbizUrl>" class="submenutext">${uiLabelMap.OrderClearOrder}</a>
        <#if (shoppingCart.size() > 0)>
            <a href="javascript:document.cartform.submit()" class="submenutext">${uiLabelMap.OrderRecalculateOrder}</a>
            <a href="javascript:removeSelected();" class="submenutext">${uiLabelMap.OrderRemoveSelected}</a>
            <#if shoppingCart.getOrderType() == "PURCHASE_ORDER">
                <a href="<@ofbizUrl>finalizeOrder?finalizeMode=purchase&finalizeReqCustInfo=false&finalizeReqShipInfo=false&finalizeReqOptions=false&finalizeReqPayInfo=false</@ofbizUrl>" class="submenutextright">${uiLabelMap.OrderFinalizeOrder}</a>
            <#else>
                <a href="<@ofbizUrl>quickcheckout</@ofbizUrl>" class="submenutext">${uiLabelMap.OrderQuickFinalizeOrder}</a>
                <a href="<@ofbizUrl>finalizeOrder?finalizeMode=default</@ofbizUrl>" class="submenutext">${uiLabelMap.OrderFinalizeOrderDefault}</a>
                <a href="<@ofbizUrl>finalizeOrder?finalizeMode=init</@ofbizUrl>" class="submenutextright">${uiLabelMap.OrderFinalizeOrder}</a>
            </#if>
        <#else>
            <span class="submenutextdisabled">${uiLabelMap.OrderRecalculateOrder}</span>
            <span class="submenutextdisabled">${uiLabelMap.OrderRemoveSelected}</span>
            <span class="submenutextdisabled">${uiLabelMap.OrderQuickFinalizeOrder}</span>
            <span class="submenutextdisabled">${uiLabelMap.OrderFinalizeOrderDefault}</span>
            <span class="submenutextrightdisabled">${uiLabelMap.OrderFinalizeOrder}</span>
        </#if>
    </div>
    <div class="boxhead-left">
        &nbsp;${uiLabelMap.CommonCreate}
        <#if shoppingCart.getOrderType() == "PURCHASE_ORDER">
            ${uiLabelMap.OrderPurchaseOrder}
        <#else>
            ${uiLabelMap.OrderSalesOrder}
        </#if>
    </div>
    <div class="boxhead-fill">&nbsp;</div>
</div>

