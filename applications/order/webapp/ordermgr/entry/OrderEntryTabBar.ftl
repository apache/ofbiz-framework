<#--

Copyright 2001-2006 The Apache Software Foundation

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.
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

