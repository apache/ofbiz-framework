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
-->

<#assign unselectedClassName = "tabButton">
<#assign selectedClassMap = {page.tabButtonItem?default("void") : "tabButtonSelected"}>

<#if custRequest?exists>
<div class="tabContainer">
  <a href="<@ofbizUrl>ViewRequest?custRequestId=${custRequest.custRequestId}</@ofbizUrl>" class="${selectedClassMap.ViewRequest?default(unselectedClassName)}">${uiLabelMap.OrderViewRequest}</a>
  <a href="<@ofbizUrl>EditRequest?custRequestId=${custRequest.custRequestId}</@ofbizUrl>" class="${selectedClassMap.request?default(unselectedClassName)}">${uiLabelMap.OrderRequest}</a>
  <a href="<@ofbizUrl>requestroles?custRequestId=${custRequest.custRequestId}</@ofbizUrl>" class="${selectedClassMap.requestroles?default(unselectedClassName)}">${uiLabelMap.OrderRequestRoles}</a>
  <a href="<@ofbizUrl>requestitems?custRequestId=${custRequest.custRequestId}</@ofbizUrl>" class="${selectedClassMap.requestitems?default(unselectedClassName)}">${uiLabelMap.OrderRequestItems}</a>
</div>
<#if custRequestItem?exists>
  <div class="tabContainer">
    <a href="<@ofbizUrl>EditRequestItem?custRequestId=${custRequest.custRequestId}&custRequestItemSeqId=${custRequestItem.custRequestItemSeqId}</@ofbizUrl>" class="${selectedClassMap.requestitem?default(unselectedClassName)}">${uiLabelMap.OrderRequestItem}</a>
    <a href="<@ofbizUrl>requestitemnotes?custRequestId=${custRequest.custRequestId}&custRequestItemSeqId=${custRequestItem.custRequestItemSeqId}</@ofbizUrl>" class="${selectedClassMap.requestitemnotes?default(unselectedClassName)}">${uiLabelMap.OrderNotes}</a>
    <#if custRequest.custRequestTypeId = "RF_QUOTE">
    <a href="<@ofbizUrl>RequestItemQuotes?custRequestId=${custRequest.custRequestId}&custRequestItemSeqId=${custRequestItem.custRequestItemSeqId}</@ofbizUrl>" class="${selectedClassMap.requestitemquotes?default(unselectedClassName)}">${uiLabelMap.OrderOrderQuotes}</a>
    </#if>
    <a href="<@ofbizUrl>requestitemrequirements?custRequestId=${custRequest.custRequestId}&custRequestItemSeqId=${custRequestItem.custRequestItemSeqId}</@ofbizUrl>" class="${selectedClassMap.requestitemrequirements?default(unselectedClassName)}">${uiLabelMap.OrderRequirements}</a>
    <a href="<@ofbizUrl>EditRequestItemWorkEfforts?custRequestId=${custRequest.custRequestId}&custRequestItemSeqId=${custRequestItem.custRequestItemSeqId}</@ofbizUrl>" class="${selectedClassMap.EditRequestItemWorkEfforts?default(unselectedClassName)}">${uiLabelMap.WorkEffortWorkEfforts}</a>
  </div>
</#if>
</#if>
