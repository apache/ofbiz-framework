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

<table class="tableButtons" cellspacing="5">
    <tr>
        <td>
            <a href="<@ofbizUrl>AddPayCash</@ofbizUrl>" class="posButton">${uiLabelMap.WebPosButtonPayCash}</a>
        </td>
        <td>
            <a href="<@ofbizUrl>AddPayCheck</@ofbizUrl>" class="posButton">${uiLabelMap.WebPosButtonPayCheck}</a>
        </td>
        <td>
            <a href="<@ofbizUrl>AddPayGiftCard</@ofbizUrl>" class="posButton">${uiLabelMap.WebPosButtonPayGiftCard}</a>
        </td>
    </tr>
    <tr>
        <td>
            <a href="<@ofbizUrl>AddPayCreditCard</@ofbizUrl>" class="posButton">${uiLabelMap.WebPosButtonPayCreditCard}</a>
        </td>
        <td>
            <#if (totalDue = 0.00)>
                <a href="<@ofbizUrl>PayFinish</@ofbizUrl>" class="posButton">${uiLabelMap.WebPosButtonPayFinish}</a>
            <#else>
                <span class="disabled">${uiLabelMap.WebPosButtonPayFinish}</span>
            </#if>
        </td>
        <td>
            <a href="<@ofbizUrl>AddPaySetRef</@ofbizUrl>" class="posButton">${uiLabelMap.WebPosButtonPaySetRef}</a>
        </td>
    </tr>
    <tr>
        <td>
            <#if (totalPayments > 0.00)>
                <a href="<@ofbizUrl>AddClearPayment</@ofbizUrl>" class="posButton">${uiLabelMap.WebPosButtonPayClear}</a>
            <#else>
                <span class="disabled">${uiLabelMap.WebPosButtonPayClear}</span>
            </#if>
        </td>
        <td>
            <#if (totalPayments > 0.00)>
                <a href="<@ofbizUrl>PayClearAll</@ofbizUrl>" class="posButton">${uiLabelMap.WebPosButtonPayClearAll}</a>
            <#else>
                <span class="disabled">${uiLabelMap.WebPosButtonPayClearAll}</span>
            </#if>
        </td>
        <td>
            <a href="<@ofbizUrl>main</@ofbizUrl>" class="posButton">${uiLabelMap.WebPosButtonMain}</a>
        </td>
    </tr>
</table>
<#if cart?has_content>
    <#if (totalDue > 0.00)>
        <div class="errorPosMessage">
          <p>${uiLabelMap.WebPosTransactionTotalDue} <@ofbizCurrency amount=totalDue isoCode=cart.getCurrency()/></p>
        </div>
    <#else>
        <div class="errorPosMessage">
          <p>${uiLabelMap.WebPosCompleteSale}</p>
        </div>
    </#if>
</#if>