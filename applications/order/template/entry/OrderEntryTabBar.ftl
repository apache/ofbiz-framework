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

<div class="screenlet">
  <div class="screenlet-title-bar">
    <ul>
      <li class="h3">
        ${uiLabelMap.CommonCreate}&nbsp;
        <#if "PURCHASE_ORDER" == shoppingCart.getOrderType()>
            ${uiLabelMap.OrderPurchaseOrder}
        <#else>
            ${uiLabelMap.OrderSalesOrder}
        </#if>
      </li>
      <div class="basic-nav">
        <ul>
          <#if "PURCHASE_ORDER" == shoppingCart.getOrderType()>
            <#if "_NA_" == shoppingCart.getOrderPartyId() || (shoppingCart.size() = 0)>
              <li class="disabled">${uiLabelMap.OrderFinalizeOrder}</li>
            <#else>
              <li><a href="<@ofbizUrl>finalizeOrder?finalizeMode=purchase&amp;finalizeReqCustInfo=false&amp;finalizeReqShipInfo=false&amp;finalizeReqOptions=false&amp;finalizeReqPayInfo=false</@ofbizUrl>">${uiLabelMap.OrderFinalizeOrder}</a></li>
            </#if>
          <#else>
            <#if shoppingCart.size() = 0>
              <li class="disabled">${uiLabelMap.OrderQuickFinalizeOrder}</li>
              <li class="disabled">${uiLabelMap.OrderFinalizeOrderDefault}</li>
              <li class="disabled">${uiLabelMap.OrderFinalizeOrder}</li>
            <#else>
              <li><a href="<@ofbizUrl>quickcheckout</@ofbizUrl>">${uiLabelMap.OrderQuickFinalizeOrder}</a></li>
              <li><a href="<@ofbizUrl>finalizeOrder?finalizeMode=default</@ofbizUrl>">${uiLabelMap.OrderFinalizeOrderDefault}</a></li>
              <li><a href="<@ofbizUrl>finalizeOrder?finalizeMode=init</@ofbizUrl>">${uiLabelMap.OrderFinalizeOrder}</a></li>
            </#if>
          </#if>
    
          <#if (shoppingCart.size() > 0)>
            <li><a href="javascript:document.cartform.submit()">${uiLabelMap.OrderRecalculateOrder}</a></li>
            <li><a href="javascript:removeSelected();">${uiLabelMap.OrderRemoveSelected}</a></li>
          <#else>
            <li class="disabled">${uiLabelMap.OrderRecalculateOrder}</li>
            <li class="disabled">${uiLabelMap.OrderRemoveSelected}</li>
          </#if>
          <li><a href="<@ofbizUrl>emptycart</@ofbizUrl>">${uiLabelMap.OrderClearOrder}</a></li>
        </ul>
      </div>
    </ul>
    <br class="clear">
  </div>
</div>
