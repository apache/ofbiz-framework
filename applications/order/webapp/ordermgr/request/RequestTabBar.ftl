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
