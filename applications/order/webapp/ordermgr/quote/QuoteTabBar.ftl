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
 *@version    $Rev$
 *@since      2.2
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
