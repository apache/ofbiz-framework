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
<#assign unselectedClassName = "tabButton">
<#assign selectedClassMap = {page.tabButtonItem?default("void") : "tabButtonSelected"}>

<#if quote?has_content>
    <div class='tabContainer'>
        <a href="<@ofbizUrl>ViewQuote?quoteId=${quote.quoteId}</@ofbizUrl>" class="${selectedClassMap.ViewQuote?default(unselectedClassName)}">${uiLabelMap.OrderViewQuote}</a>
        <a href="<@ofbizUrl>EditQuote?quoteId=${quote.quoteId}</@ofbizUrl>" class="${selectedClassMap.EditQuote?default(unselectedClassName)}">${uiLabelMap.OrderOrderQuote}</a>
        <a href="<@ofbizUrl>ListQuoteRoles?quoteId=${quote.quoteId}</@ofbizUrl>" class="${selectedClassMap.ListQuoteRoles?default(unselectedClassName)}">${uiLabelMap.OrderOrderQuoteRoles}</a>
        <a href="<@ofbizUrl>ListQuoteItems?quoteId=${quote.quoteId}</@ofbizUrl>" class="${selectedClassMap.ListQuoteItems?default(unselectedClassName)}">${uiLabelMap.OrderOrderQuoteItems}</a>
        <a href="<@ofbizUrl>ListQuoteAttributes?quoteId=${quote.quoteId}</@ofbizUrl>" class="${selectedClassMap.ListQuoteAttributes?default(unselectedClassName)}">${uiLabelMap.OrderOrderQuoteAttributes}</a>
        <#if security.hasEntityPermission("ORDERMGR", "_QUOTE_PRICE", session)>
        <a href="<@ofbizUrl>ListQuoteCoefficients?quoteId=${quote.quoteId}</@ofbizUrl>" class="${selectedClassMap.ListQuoteCoefficients?default(unselectedClassName)}">${uiLabelMap.OrderOrderQuoteCoefficients}</a>
        <a href="<@ofbizUrl>ManageQuotePrices?quoteId=${quote.quoteId}</@ofbizUrl>" class="${selectedClassMap.ManageQuotePrices?default(unselectedClassName)}">${uiLabelMap.OrderOrderQuotePrices}</a>
        <a href="<@ofbizUrl>ListQuoteAdjustments?quoteId=${quote.quoteId}</@ofbizUrl>" class="${selectedClassMap.ListQuoteAdjustments?default(unselectedClassName)}">${uiLabelMap.OrderOrderQuoteAdjustments}</a>
        <a href="<@ofbizUrl>ViewQuoteProfit?quoteId=${quote.quoteId}</@ofbizUrl>" class="${selectedClassMap.ViewQuoteProfit?default(unselectedClassName)}">${uiLabelMap.OrderViewQuoteProfit}</a>
        </#if>
        <a href="<@ofbizUrl>ListQuoteWorkEfforts?quoteId=${quote.quoteId}</@ofbizUrl>" class="${selectedClassMap.QuoteWorkEfforts?default(unselectedClassName)}">${uiLabelMap.OrderOrderQuoteWorkEfforts}</a>
    </div>
</#if>
