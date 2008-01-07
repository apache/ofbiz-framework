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
<#assign selected = tabButtonItem?default("void")>

<#if quote?has_content>
    <div class="button-bar tab-bar">
        <ul>
            <li<#if selected="ViewQuote"> class="selected"</#if>><a href="<@ofbizUrl>ViewQuote?quoteId=${quote.quoteId}</@ofbizUrl>">${uiLabelMap.OrderViewQuote}</a></li>
            <li<#if selected="EditQuote"> class="selected"</#if>><a href="<@ofbizUrl>EditQuote?quoteId=${quote.quoteId}</@ofbizUrl>">${uiLabelMap.OrderOrderQuote}</a></li>
            <li<#if selected="ListQuoteRoles"> class="selected"</#if>><a href="<@ofbizUrl>ListQuoteRoles?quoteId=${quote.quoteId}</@ofbizUrl>">${uiLabelMap.OrderOrderQuoteRoles}</a></li>
            <li<#if selected="ListQuoteItems"> class="selected"</#if>><a href="<@ofbizUrl>ListQuoteItems?quoteId=${quote.quoteId}</@ofbizUrl>">${uiLabelMap.OrderOrderQuoteItems}</a></li>
            <li<#if selected="ListQuoteAttributes"> class="selected"</#if>><a href="<@ofbizUrl>ListQuoteAttributes?quoteId=${quote.quoteId}</@ofbizUrl>">${uiLabelMap.OrderOrderQuoteAttributes}</a></li>
            <#if security.hasEntityPermission("ORDERMGR", "_QUOTE_PRICE", session)>
            <li<#if selected="ListQuoteCoefficients"> class="selected"</#if>><a href="<@ofbizUrl>ListQuoteCoefficients?quoteId=${quote.quoteId}</@ofbizUrl>">${uiLabelMap.OrderOrderQuoteCoefficients}</a></li>
            <li<#if selected="ManageQuotePrices"> class="selected"</#if>><a href="<@ofbizUrl>ManageQuotePrices?quoteId=${quote.quoteId}</@ofbizUrl>">${uiLabelMap.OrderOrderQuotePrices}</a></li>
            <li<#if selected="ListQuoteAdjustments"> class="selected"</#if>><a href="<@ofbizUrl>ListQuoteAdjustments?quoteId=${quote.quoteId}</@ofbizUrl>">${uiLabelMap.OrderOrderQuoteAdjustments}</a></li>
            <li<#if selected="ViewQuoteProfit"> class="selected"</#if>><a href="<@ofbizUrl>ViewQuoteProfit?quoteId=${quote.quoteId}</@ofbizUrl>">${uiLabelMap.OrderViewQuoteProfit}</a></li>
            </#if>
            <li<#if selected="QuoteWorkEfforts"> class="selected"</#if>><a href="<@ofbizUrl>ListQuoteWorkEfforts?quoteId=${quote.quoteId}</@ofbizUrl>">${uiLabelMap.OrderOrderQuoteWorkEfforts}</a></li>
        </ul>
        <br/>
    </div>
</#if>
