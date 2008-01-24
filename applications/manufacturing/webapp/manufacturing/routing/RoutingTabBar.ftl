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

<#if routing?exists>
<div class="button-bar tab-bar">
    <ul>
        <li<#if selected == "editRouting"> class="selected"</#if>><a href="<@ofbizUrl>EditRouting?workEffortId=${routing.workEffortId}</@ofbizUrl>">${uiLabelMap.ManufacturingEditRouting}</a></li>
        <li<#if selected == "routingTaskAssoc"> class="selected"</#if>><a href="<@ofbizUrl>EditRoutingTaskAssoc?workEffortId=${routing.workEffortId}</@ofbizUrl>">${uiLabelMap.ManufacturingEditRoutingTaskAssoc}</a></li>
        <li<#if selected == "routingProductLink"> class="selected"</#if>><a href="<@ofbizUrl>EditRoutingProductLink?workEffortId=${routing.workEffortId}</@ofbizUrl>">${uiLabelMap.ManufacturingEditRoutingProductLink}</a></li>
    </ul>
    <br class="clear"/>
</div>
</#if>