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

<div class="button-bar tab-bar">
    <ul>
        <li<#if selected="FindRequirements"> class="selected"</#if>><a href="<@ofbizUrl>FindRequirements</@ofbizUrl>">${uiLabelMap.OrderRequirements}</a></li>
        <li<#if selected="ApproveRequirements"> class="selected"</#if>><a href="<@ofbizUrl>ApproveRequirements</@ofbizUrl>">${uiLabelMap.OrderApproveRequirements}</a></li>
        <li<#if selected="ApprovedProductRequirementsByVendor"> class="selected"</#if>><a href="<@ofbizUrl>ApprovedProductRequirementsByVendor</@ofbizUrl>">${uiLabelMap.OrderApprovedProductRequirementsByVendor}</a></li>
        <li<#if selected="ApprovedProductRequirements"> class="selected"</#if>><a href="<@ofbizUrl>ApprovedProductRequirements</@ofbizUrl>">${uiLabelMap.OrderApprovedProductRequirements}</a></li>
    </ul>
    <br/>
</div>