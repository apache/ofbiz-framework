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

<#if security.hasEntityPermission("PARTYMGR", "_VIEW", session)>
<#-- Main Heading -->
<#if partyClassificationGroup?has_content>
<div class="tabContainer">
    <a href="<@ofbizUrl>EditPartyClassificationGroup?partyClassificationGroupId=${partyClassificationGroup.partyClassificationGroupId}</@ofbizUrl>" class="${selectedClassMap.EditPartyClassificationGroup?default(unselectedClassName)}">${uiLabelMap.PartyClassificationGroups}</a>
    <a href="<@ofbizUrl>EditPartyClassificationGroupParties?partyClassificationGroupId=${partyClassificationGroup.partyClassificationGroupId}</@ofbizUrl>" class="${selectedClassMap.EditPartyClassificationGroupParties?default(unselectedClassName)}">${uiLabelMap.Parties}</a>
</div>
</#if>

<#else>
  <div class="head2">${uiLabelMap.PartyMgrViewPermissionError}</div>
</#if>
