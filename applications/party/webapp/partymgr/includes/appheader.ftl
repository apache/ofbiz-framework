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

<#assign selected = headerItem?default("void")>
<div id="app-navigation">
  <h2>${uiLabelMap.PartyManagerApplication}</h2>
  <ul>
    <li<#if selected == "main"> class="selected"</#if>><a href="<@ofbizUrl>main</@ofbizUrl>">${uiLabelMap.CommonMain}</a></li>
    <li<#if selected == "find"> class="selected"</#if>><a href="<@ofbizUrl>findparty</@ofbizUrl>">${uiLabelMap.CommonFind}</a></li>
    <#if security.hasEntityPermission("PARTYMGR", "_CREATE", session)>
      <li<#if selected == "create"> class="selected"</#if>><a href="<@ofbizUrl>createnew</@ofbizUrl>">${uiLabelMap.CommonCreate}</a></li>
    </#if>
    <#if security.hasEntityPermission("PARTYMGR", "_UPDATE", session)>
      <li<#if selected == "link"> class="selected"</#if>><a href="<@ofbizUrl>linkparty</@ofbizUrl>">${uiLabelMap.PartyLink}</a></li>
    </#if>
    <li<#if selected == "comm"> class="selected"</#if>><a href="<@ofbizUrl>FindCommunicationEvents</@ofbizUrl>">${uiLabelMap.PartyCommunications}</a></li>
    <li<#if selected == "visits"> class="selected"</#if>><a href="<@ofbizUrl>showvisits</@ofbizUrl>">${uiLabelMap.PartyVisits}</a></li>
    <li<#if selected == "classification"> class="selected"</#if>><a href="<@ofbizUrl>showclassgroups</@ofbizUrl>">${uiLabelMap.PartyClassifications}</a></li>
    <#if security.hasEntityPermission("SECURITY", "_VIEW", session)>
      <li<#if selected == "security"> class="selected"</#if>><a href="<@ofbizUrl>FindSecurityGroup</@ofbizUrl>">${uiLabelMap.CommonSecurity}</a></li>
    </#if>
    <li<#if selected == "addrmap"> class="selected"</#if>><a href="<@ofbizUrl>addressMatchMap</@ofbizUrl>">${uiLabelMap.PageTitleAddressMatchMap}</a></li>
    <#if userLogin?has_content>
      <li class="opposed"><a href="<@ofbizUrl>logout</@ofbizUrl>">${uiLabelMap.CommonLogout}</a></li>
    <#else>
      <li class="opposed"><a href="<@ofbizUrl>${checkLoginUrl?if_exists}</@ofbizUrl>">${uiLabelMap.CommonLogin}</a></li>
    </#if>
  </ul>
  <br class="clear" />
</div>
