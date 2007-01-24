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

<#if (requestAttributes.uiLabelMap)?exists><#assign uiLabelMap = requestAttributes.uiLabelMap></#if>
<#if (requestAttributes.security)?exists><#assign security = requestAttributes.security></#if>
<#if (requestAttributes.userLogin)?exists><#assign userLogin = requestAttributes.userLogin></#if>
<#if (requestAttributes.checkLoginUrl)?exists><#assign checkLoginUrl = requestAttributes.checkLoginUrl></#if>

<#assign unselectedLeftClassName = "headerButtonLeft">
<#assign unselectedRightClassName = "headerButtonRight">
<#assign selectedLeftClassMap = {(page.headerItem)?default("void") : "headerButtonLeftSelected"}>
<#assign selectedRightClassMap = {(page.headerItem)?default("void") : "headerButtonRightSelected"}>

<div class="apptitle">&nbsp;${uiLabelMap.PartyManagerApplication}&nbsp;</div>
<div class="row">
  <div class="col"><a href="<@ofbizUrl>main</@ofbizUrl>" class="${selectedLeftClassMap.main?default(unselectedLeftClassName)}">${uiLabelMap.CommonMain}</a></div>
  <div class="col"><a href="<@ofbizUrl>findparty</@ofbizUrl>" class="${selectedLeftClassMap.find?default(unselectedLeftClassName)}">${uiLabelMap.CommonFind}</a></div>
  <#if security.hasEntityPermission("PARTYMGR", "_CREATE", session)>
    <div class="col"><a href="<@ofbizUrl>createnew</@ofbizUrl>" class="${selectedLeftClassMap.create?default(unselectedLeftClassName)}">${uiLabelMap.CommonCreate}</a></div>
  </#if>
  <#if security.hasEntityPermission("PARTYMGR", "_UPDATE", session)>
    <div class="col"><a href="<@ofbizUrl>linkparty</@ofbizUrl>" class="${selectedLeftClassMap.link?default(unselectedLeftClassName)}">${uiLabelMap.PartyLink}</a></div>
  </#if>
  <div class="col"><a href="<@ofbizUrl>FindCommunicationEvents</@ofbizUrl>" class="${selectedLeftClassMap.comm?default(unselectedLeftClassName)}">${uiLabelMap.PartyCommunications}</a></div>
  <div class="col"><a href="<@ofbizUrl>showvisits</@ofbizUrl>" class="${selectedLeftClassMap.visits?default(unselectedLeftClassName)}">${uiLabelMap.PartyVisits}</a></div>
  <div class="col"><a href="<@ofbizUrl>showclassgroups</@ofbizUrl>" class="${selectedLeftClassMap.classification?default(unselectedLeftClassName)}">${uiLabelMap.PartyClassifications}</a></div>
  <#if security.hasEntityPermission("SECURITY", "_VIEW", session)>
    <div class="col"><a href="<@ofbizUrl>FindSecurityGroup</@ofbizUrl>" class="${selectedLeftClassMap.security?default(unselectedLeftClassName)}">${uiLabelMap.CommonSecurity}</a></div>
  </#if>
  <#if userLogin?has_content>
    <div class="col-right"><a href="<@ofbizUrl>logout</@ofbizUrl>" class="${unselectedRightClassName}">${uiLabelMap.CommonLogout}</a></div>
  <#else>
    <div class="col-right"><a href='<@ofbizUrl>${checkLoginUrl?if_exists}</@ofbizUrl>' class='${selectedRightClassMap.login?default(unselectedRightClassName)}'>${uiLabelMap.CommonLogin}</a></div>
  </#if>
  <div class="col-right"><a href="<@ofbizUrl>addressMatchMap</@ofbizUrl>" class="${selectedLeftClassMap.addrmap?default(unselectedLeftClassName)}">${uiLabelMap.PageTitleAddressMatchMap}</a></div>
  <div class="col-fill">&nbsp;</div>
</div>
