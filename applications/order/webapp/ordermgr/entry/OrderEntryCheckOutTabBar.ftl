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

<#if stepTitleId?exists>
    <#assign stepTitle = uiLabelMap.get(stepTitleId)>
</#if>
<div class="boxtop">
    <div class="boxhead-right">
        <#list checkoutSteps as checkoutStep>
            <#assign stepUiLabel = uiLabelMap.get(checkoutStep.label)>
            <#if checkoutStep.enabled == "N">
                <span class="buttontextdisabled">${stepUiLabel}</span>
            <#else>
                <a href="<@ofbizUrl>${checkoutStep.uri}</@ofbizUrl>" class="buttontext">${stepUiLabel}</a>
            </#if>
        </#list>
        <#if isLastStep == "N">
            <a href="javascript:document.checkoutsetupform.submit();" class="buttontext">${uiLabelMap.CommonContinue}</a>
        <#else>
            <a href="<@ofbizUrl>processorder</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderCreateOrder}</a>
        </#if>
    </div>
    <div class="boxhead-left" align="left">
        <#if shoppingCart.getOrderType() == "PURCHASE_ORDER">
            ${uiLabelMap.OrderPurchaseOrder}
        <#else>
            ${uiLabelMap.OrderSalesOrder}
        </#if>
        :&nbsp;${stepTitle?if_exists}
    </div>
    <div class="boxhead-fill">&nbsp;</div>
</div>
