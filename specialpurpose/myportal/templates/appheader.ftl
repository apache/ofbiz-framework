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
  <h2>${uiLabelMap.PageTitleMyPortal} ${partyNameView.personalTitle?if_exists} ${partyNameView.firstName?if_exists} ${partyNameView.middleName?if_exists} ${partyNameView.lastName?if_exists} ${partyNameView.groupName?if_exists}</h2>
  <ul>
    <li>
  <ul>
    <#if portalPages?has_content>
      <#list portalPages as page>
        <li<#if selected = "${page.portalPageId}"> class="selected"</#if>><a href="<@ofbizUrl>showPortalPage?portalPageId=${page.portalPageId}</@ofbizUrl>">${page.portalPageName}</a></li>
      </#list>
    </#if>
    <li class="opposed"><a href="<@ofbizUrl>logout</@ofbizUrl>">${uiLabelMap.CommonLogout}</a></li>
    <li class="opposed"><a href="http://docs.ofbiz.org/display/OFBENDUSER/My+Portal?decorator=printable" url-mode="plain" target-window="new">${uiLabelMap.CommonHelp}</a></li>
    <li class="opposed"><a href="<@ofbizUrl>ManagePortalPages?parentPortalPageId=MYPORTAL</@ofbizUrl>">${uiLabelMap.CommonPreferences}</a></li>
  </ul>
  </li>
  </ul>
  <br class="clear" />
</div>