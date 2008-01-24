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

<#if routingTask?exists>
<div class="button-bar tab-bar">
    <ul>
        <li<#if selected == "editRoutingTask"> class="selected"</#if>><a href="<@ofbizUrl>EditRoutingTask?workEffortId=${routingTask.workEffortId}</@ofbizUrl>">${uiLabelMap.ManufacturingEditRoutingTask}</a></li>
        <li<#if selected == "editRoutingTaskCosts"> class="selected"</#if>><a href="<@ofbizUrl>EditRoutingTaskCosts?workEffortId=${routingTask.workEffortId}</@ofbizUrl>">${uiLabelMap.ManufacturingListRoutings}</a></li>
        <li<#if selected == "listRoutingTaskProducts"> class="selected"</#if>><a href="<@ofbizUrl>ListRoutingTaskProducts?workEffortId=${routingTask.workEffortId}</@ofbizUrl>">${uiLabelMap.ManufacturingListProducts}</a></li>
        <li<#if selected == "editRoutingTaskFixedAssets"> class="selected"</#if>><a href="<@ofbizUrl>EditRoutingTaskFixedAssets?workEffortId=${routingTask.workEffortId}</@ofbizUrl>">${uiLabelMap.ManufacturingRoutingTaskFixedAssets}</a></li>
    </ul>
    <br class="clear"/>
</div>
</#if>