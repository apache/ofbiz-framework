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

<#if (requestAttributes.uiLabelMap)?exists><#assign uiLabelMap = requestAttributes.uiLabelMap></#if>
<#assign unselectedClassName = "tabButton">
<#assign selectedClassMap = {page.tabButtonItem?default("void") : "tabButtonSelected"}>

<div class="tabContainer">
    <a href="<@ofbizUrl>/AuthorizeTransaction</@ofbizUrl>" class="${selectedClassMap.authorizetransactiontab?default(unselectedClassName)}">${uiLabelMap.AccountingAuthorize}</a>
    <a href="<@ofbizUrl>/CaptureTransaction</@ofbizUrl>" class="${selectedClassMap.capturetransactiontab?default(unselectedClassName)}">${uiLabelMap.AccountingCapture}</a>
    <a href="<@ofbizUrl>/FindGatewayResponses</@ofbizUrl>" class="${selectedClassMap.gatewayresponsestab?default(unselectedClassName)}">${uiLabelMap.AccountingGatewayResponses}</a>
    <a href="<@ofbizUrl>/ManualTransaction</@ofbizUrl>" class="${selectedClassMap.manualtransactiontab?default(unselectedClassName)}">${uiLabelMap.AccountingManualTransaction}</a>
</div>
