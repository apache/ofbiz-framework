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

<#if routing?exists>
<div class='tabContainer'>
    <a href="<@ofbizUrl>EditRouting?workEffortId=${routing.workEffortId}</@ofbizUrl>" class="${selectedClassMap.editRouting?default(unselectedClassName)}">${uiLabelMap.ManufacturingEditRouting}</a>
    <a href="<@ofbizUrl>EditRoutingTaskAssoc?workEffortId=${routing.workEffortId}</@ofbizUrl>" class="${selectedClassMap.routingTaskAssoc?default(unselectedClassName)}">${uiLabelMap.ManufacturingEditRoutingTaskAssoc}</a>
    <a href="<@ofbizUrl>EditRoutingProductLink?workEffortId=${routing.workEffortId}</@ofbizUrl>" class="${selectedClassMap.routingProductLink?default(unselectedClassName)}">${uiLabelMap.ManufacturingEditRoutingProductLink}</a>
</div>
</#if>