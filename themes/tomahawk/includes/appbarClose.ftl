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
<#assign appModelMenu = Static["org.ofbiz.widget.menu.MenuFactory"].getMenuFromLocation(applicationMenuLocation,applicationMenuName,delegator,dispatcher)>
<#if person?has_content>
  <#assign userName = person.firstName?if_exists + " " + person.middleName?if_exists + " " + person.lastName?if_exists>
<#elseif partyGroup?has_content>
  <#assign userName = partyGroup.groupName?if_exists>
<#elseif userLogin?exists>
  <#assign userName = userLogin.userLoginId>
<#else>
  <#assign userName = "">
</#if>
<#if defaultOrganizationPartyGroupName?has_content>
  <#assign orgName = " - " + defaultOrganizationPartyGroupName?if_exists>
<#else>
  <#assign orgName = "">
</#if>

<#if appModelMenu.getModelMenuItemByName(headerItem)?exists>
  <#if headerItem!="main">
    <div id="app-nav-selected-item">
      ${appModelMenu.getModelMenuItemByName(headerItem).getTitle(context)}
    </div>
  </#if>
</#if>

<#if parameters.portalPageId?has_content && !appModelMenu.getModelMenuItemByName(headerItem)?exists && userLogin?exists>
    <#assign findMap = Static["org.ofbiz.base.util.UtilMisc"].toMap("portalPageId", parameters.portalPageId)>
    <#assign portalPage = delegator.findOne("PortalPage", findMap, true)?if_exists>
    <#if portalPage?has_content>
      <div id="app-nav-selected-item">
        ${portalPage.get("portalPageName", locale)?if_exists}
      </div>
    </#if>
</#if>

<div id="control-area">
  <ul id="preferences-menu">
    <#if userLogin?exists>
      <#if (userPreferences.COMPACT_HEADER)?default("N") == "Y">
        <li class="collapsed"><a href="javascript:document.setUserPreferenceCompactHeaderN.submit()">&nbsp;</a>
          <form name="setUserPreferenceCompactHeaderN" method="post" action="<@ofbizUrl>setUserPreference</@ofbizUrl>">
            <input type="hidden" name="userPrefGroupTypeId" value="GLOBAL_PREFERENCES"/>
            <input type="hidden" name="userPrefTypeId" value="COMPACT_HEADER"/>
            <input type="hidden" name="userPrefValue" value="N"/>
          </form>
        </li>
      <#else>
        <li class="expanded"><a href="javascript:document.setUserPreferenceCompactHeaderY.submit()">&nbsp;</a>
          <form name="setUserPreferenceCompactHeaderY" method="post" action="<@ofbizUrl>setUserPreference</@ofbizUrl>">
            <input type="hidden" name="userPrefGroupTypeId" value="GLOBAL_PREFERENCES"/>
            <input type="hidden" name="userPrefTypeId" value="COMPACT_HEADER"/>
            <input type="hidden" name="userPrefValue" value="Y"/>
          </form>
        </li>
      </#if>
    </#if>
    <#if userLogin?exists>
      <#--if webSiteId?exists && requestAttributes._CURRENT_VIEW_?exists && helpTopic?exists-->
      <#if parameters.componentName?exists && requestAttributes._CURRENT_VIEW_?exists && helpTopic?exists>
        <#include "component://common/webcommon/includes/helplink.ftl" />
        <li><a class="help-link <#if pageAvail?has_content> alert</#if>" href="javascript:lookup_popup1('showHelp?helpTopic=${helpTopic}&amp;portalPageId=${parameters.portalPageId?if_exists}','help' ,500,500);" title="${uiLabelMap.CommonHelp}"></a></li>
      </#if>
      <li><a href="<@ofbizUrl>logout</@ofbizUrl>">${uiLabelMap.CommonLogout}</a></li>
      <li><a href="<@ofbizUrl>ListVisualThemes</@ofbizUrl>">${uiLabelMap.CommonVisualThemes}</a></li>
    <#else>
      <li><a href="<@ofbizUrl>${checkLoginUrl}</@ofbizUrl>">${uiLabelMap.CommonLogin}</a></li>
    </#if>
    <li class="first"><a href="<@ofbizUrl>ListLocales</@ofbizUrl>">${uiLabelMap.CommonLanguageTitle}</a></li>
    <#if userLogin?exists>
      <#if orgName?has_content>
        <li class="org">${orgName}</li>
      </#if>
      <#if userLogin.partyId?exists>
        <li class="user"><a href="<@ofbizUrl>passwordChange</@ofbizUrl>">${userName}</a></li>
      <#else>
        <li class="user">${userName}</li>
      </#if>
    </#if>
  </ul>
</div>
</div>
<div class="clear">
</div>

<#if userLogin?exists>
<script type="text/javascript">
  var mainmenu = new DropDownMenu(jQuery('#main-navigation'));
  var appmenu = new DropDownMenu(jQuery('#app-navigation'));
</script>
</#if>
