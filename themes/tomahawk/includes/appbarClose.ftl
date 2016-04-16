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
<#assign appModelMenu = Static["org.ofbiz.widget.model.MenuFactory"].getMenuFromLocation(applicationMenuLocation,applicationMenuName)>
<#if person?has_content>
  <#assign userName = (person.firstName!) + " " + (person.middleName!) + " " + person.lastName!>
<#elseif partyGroup?has_content>
  <#assign userName = partyGroup.groupName!>
<#elseif userLogin??>
  <#assign userName = userLogin.userLoginId>
<#else>
  <#assign userName = "">
</#if>
<#if defaultOrganizationPartyGroupName?has_content>
  <#assign orgName = " - " + defaultOrganizationPartyGroupName!>
<#else>
  <#assign orgName = "">
</#if>

<#if appModelMenu.getModelMenuItemByName(headerItem)??>
  <#if headerItem!="main">
    <div id="app-nav-selected-item">
      ${appModelMenu.getModelMenuItemByName(headerItem).getTitle(context)}
    </div>
  </#if>
</#if>

<#if parameters.portalPageId?has_content && !appModelMenu.getModelMenuItemByName(headerItem)?? && userLogin??>
    <#assign findMap = Static["org.ofbiz.base.util.UtilMisc"].toMap("portalPageId", parameters.portalPageId)>
    <#assign portalPage = delegator.findOne("PortalPage", findMap, true)!>
    <#if portalPage?has_content>
      <div id="app-nav-selected-item">
        ${portalPage.get("portalPageName", locale)!}
      </div>
    </#if>
</#if>

<div id="control-area">
  <ul id="preferences-menu">
    <#if userLogin??>
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
    <#if userLogin??>
      <#--if webSiteId?? && requestAttributes._CURRENT_VIEW_?? && helpTopic??-->
      <#if parameters.componentName?? && requestAttributes._CURRENT_VIEW_?? && helpTopic??>
        <#include "component://common/webcommon/includes/helplink.ftl" />
        <li><a class="help-link <#if pageAvail?has_content> alert</#if>" href="javascript:lookup_popup1('showHelp?helpTopic=${helpTopic}&amp;portalPageId=${parameters.portalPageId!}','help' ,500,500);" title="${uiLabelMap.CommonHelp}"></a></li>
      </#if>
      <li><a href="<@ofbizUrl>logout</@ofbizUrl>">${uiLabelMap.CommonLogout}</a></li>
      <li><a href="<@ofbizUrl>ListVisualThemes</@ofbizUrl>">${uiLabelMap.CommonVisualThemes}</a></li>
    <#else>
      <li><a href="<@ofbizUrl>${checkLoginUrl}</@ofbizUrl>">${uiLabelMap.CommonLogin}</a></li>
    </#if>
    <li <#if companyListSize?default(0) &lt;= 1>class="language"</#if>><a href="<@ofbizUrl>ListLocales</@ofbizUrl>">${uiLabelMap.CommonLanguageTitle}</a></li>
    <#if userLogin?exists>
      <#if userLogin.partyId?exists>
        <li class="user"><a href="/partymgr/control/viewprofile?partyId=${userLogin.partyId}&externalLoginKey=${externalLoginKey}">${userName}</a>&nbsp;&nbsp;&nbsp;&nbsp;</li>
        <#assign size = companyListSize?default(0)>
        <#if size &gt; 1>
            <#assign currentCompany = delegator.findOne("PartyNameView", {"partyId" : organizationPartyId}, false)>
            <#if currentCompany?exists>
                <li class="user">
                    <a href="<@ofbizUrl>ListSetCompanies</@ofbizUrl>">${currentCompany.groupName} &nbsp;- </a>
                </li>
            </#if>
        </#if>
      <#else>
        <li class="user">${userName}</li>
      </#if>
    </#if>
  </ul>
</div>
</div>
<div class="clear">
</div>

<#if userLogin??>
<script type="text/javascript">
  var mainmenu = new DropDownMenu(jQuery('#main-navigation'));
  var appmenu = new DropDownMenu(jQuery('#app-navigation'));
</script>
</#if>
