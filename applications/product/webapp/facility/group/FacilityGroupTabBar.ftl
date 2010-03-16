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

<#if facilityGroupId?has_content>
    <div class="button-bar tab-bar">
        <ul>
            <li<#if selected="EditFacilityGroup"> class="selected"</#if>><a href="<@ofbizUrl>EditFacilityGroup?facilityGroupId=${facilityGroupId}</@ofbizUrl>">${uiLabelMap.ProductFacilityGroup}</a></li>
            <li<#if selected="EditFacilityGroupRollup"> class="selected"</#if>><a href="<@ofbizUrl>EditFacilityGroupRollup?facilityGroupId=${facilityGroupId}</@ofbizUrl>">${uiLabelMap.ProductRollups}</a></li>
            <li<#if selected="EditFacilityGroupMembers"> class="selected"</#if>><a href="<@ofbizUrl>EditFacilityGroupMembers?facilityGroupId=${facilityGroupId}</@ofbizUrl>">${uiLabelMap.ProductFacilities}</a></li>
            <li<#if selected="EditFacilityGroupRoles"> class="selected"</#if>><a href="<@ofbizUrl>EditFacilityGroupRoles?facilityGroupId=${facilityGroupId}</@ofbizUrl>">${uiLabelMap.PartyRoles}</a></li>
        </ul>
        <br />
    </div>
</#if>