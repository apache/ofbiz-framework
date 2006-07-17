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

<#assign unselectedClassName = "tabButton">
<#assign selectedClassMap = {page.tabButtonItem?default("void") : "tabButtonSelected"}>

<#if requirement?exists>
<div class='tabContainer'>
    <a href="<@ofbizUrl>EditRequirement?requirementId=${requirement.requirementId}</@ofbizUrl>" class="${selectedClassMap.EditRequirement?default(unselectedClassName)}">${uiLabelMap.OrderRequirement}</a>
    <a href="<@ofbizUrl>ListRequirementCustRequests?requirementId=${requirement.requirementId}</@ofbizUrl>" class="${selectedClassMap.ListRequirementCustRequests?default(unselectedClassName)}">${uiLabelMap.OrderRequests}</a>
    <a href="<@ofbizUrl>ListRequirementOrders?requirementId=${requirement.requirementId}</@ofbizUrl>" class="${selectedClassMap.ListRequirementOrdersTab?default(unselectedClassName)}">${uiLabelMap.OrderOrders}</a>
</div>
</#if>
