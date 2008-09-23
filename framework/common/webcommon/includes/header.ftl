<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
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

<#if (requestAttributes.person)?exists><#assign person = requestAttributes.person></#if>
<#if (requestAttributes.partyGroup)?exists><#assign partyGroup = requestAttributes.partyGroup></#if>

<#assign docLangAttr = locale.toString()?replace("_", "-")>
<#assign langDir = "ltr">
<#if "ar.iw"?contains(docLangAttr?substring(0, 2))>
    <#assign langDir = "rtl">
</#if>
<html lang="${docLangAttr}" dir="${langDir}" xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <title>${layoutSettings.companyName}: <#if (page.titleProperty)?has_content>${uiLabelMap[page.titleProperty]}<#else>${(page.title)?if_exists}</#if></title>
    <#if layoutSettings.shortcutIcon?has_content>
      <#assign shortcutIcon = layoutSettings.shortcutIcon/>
    <#elseif layoutSettings.VT_SHORTCUT_ICON?has_content>    
      <#assign shortcutIcon = layoutSettings.VT_SHORTCUT_ICON.get(0)/>
    </#if>
    <#if shortcutIcon?has_content>
      <link rel="shortcut icon" href="<@ofbizContentUrl>${shortcutIcon}</@ofbizContentUrl>" />
    </#if>
    <#if layoutSettings.javaScripts?has_content>
        <#--layoutSettings.javaScripts is a list of java scripts. -->
        <#-- use a Set to make sure each javascript is declared only once, but iterate the list to maintain the correct order -->
        <#assign javaScriptsSet = Static["org.ofbiz.base.util.UtilMisc"].toSet(layoutSettings.javaScripts)/>
        <#list layoutSettings.javaScripts as javaScript>
            <#if javaScriptsSet.contains(javaScript)>
                <#assign nothing = javaScriptsSet.remove(javaScript)/>
                <script type="text/javascript" src="<@ofbizContentUrl>${javaScript}</@ofbizContentUrl>" type="text/javascript"></script>
            </#if>
        </#list>
    </#if>
    <#if layoutSettings.VT_HDR_JAVASCRIPT?has_content>
        <#list layoutSettings.VT_HDR_JAVASCRIPT as javaScript>
            <script type="text/javascript" src="<@ofbizContentUrl>${javaScript}</@ofbizContentUrl>" type="text/javascript"></script>
        </#list>
    </#if>
    <#if layoutSettings.styleSheets?has_content>
        <#--layoutSettings.styleSheets is a list of style sheets. So, you can have a user-specified "main" style sheet, AND a component style sheet.-->
        <#list layoutSettings.styleSheets as styleSheet>
            <link rel="stylesheet" href="<@ofbizContentUrl>${styleSheet}</@ofbizContentUrl>" type="text/css"/>
        </#list>
    </#if>
    <#if layoutSettings.VT_STYLESHEET?has_content>
        <#list layoutSettings.VT_STYLESHEET as styleSheet>
            <link rel="stylesheet" href="<@ofbizContentUrl>${styleSheet}</@ofbizContentUrl>" type="text/css"/>
        </#list>
    </#if>
    <#if layoutSettings.rtlStyleSheets?has_content && langDir == "rtl">
        <#--layoutSettings.rtlStyleSheets is a list of rtl style sheets.-->
        <#list layoutSettings.rtlStyleSheets as styleSheet>
            <link rel="stylesheet" href="<@ofbizContentUrl>${styleSheet}</@ofbizContentUrl>" type="text/css"/>
        </#list>
    </#if>
    <#if layoutSettings.VT_RTL_STYLESHEET?has_content && langDir == "rtl">
        <#list layoutSettings.VT_RTL_STYLESHEET as styleSheet>
            <link rel="stylesheet" href="<@ofbizContentUrl>${styleSheet}</@ofbizContentUrl>" type="text/css"/>
        </#list>
    </#if>
    ${layoutSettings.extraHead?if_exists}
    <#if layoutSettings.VT_EXTRA_HEAD?has_content>
        <#list layoutSettings.VT_EXTRA_HEAD as extraHead>
            ${extraHead}
        </#list>
    </#if>
</head>
<#if layoutSettings.headerImageLinkUrl?exists>
  <#assign logoLinkURL = "${layoutSettings.headerImageLinkUrl}">
<#else>
  <#assign logoLinkURL = "${layoutSettings.commonHeaderImageLinkUrl}">
</#if>
<body>
  <div class="hidden">
    <a href="#column-container" title="${uiLabelMap.CommonSkipNavigation}" accesskey="2">
      ${uiLabelMap.CommonSkipNavigation}
    </a>
  </div>
  <div id="masthead">
    <ul>
      <#if (userPreferences.COMPACT_HEADER)?default("N") == "Y">
        <li class="logo-area">
          <#if shortcutIcon?has_content>
            <a href="<@ofbizUrl>${logoLinkURL}</@ofbizUrl>"><img src="<@ofbizContentUrl>${shortcutIcon}</@ofbizContentUrl>"/></a>
          </#if>
        </li>
        <li>
          <#if person?has_content>
            ${uiLabelMap.CommonWelcome} ${person.firstName?if_exists} ${person.lastName?if_exists} [${userLogin.userLoginId}]
          <#elseif partyGroup?has_content>
            ${uiLabelMap.CommonWelcome} ${partyGroup.groupName?if_exists} [${userLogin.userLoginId}]
          <#else>
            ${uiLabelMap.CommonWelcome}!
          </#if>
        </li>
        <li>${nowTimestamp?datetime?string.short}</li>
        <li class="control-area">
          <p class="collapsed"><a href="setUserPreference?userPrefGroupId=GLOBAL_PREFERENCES&amp;userPrefTypeId=COMPACT_HEADER&amp;userPrefValue=N">&nbsp;&nbsp;</a></p>
        </li>
      <#else>
        <#if layoutSettings.headerImageUrl?exists>
          <#assign headerImageUrl = layoutSettings.headerImageUrl>
        <#elseif layoutSettings.commonHeaderImageUrl?exists>
          <#assign headerImageUrl = layoutSettings.commonHeaderImageUrl>
        <#elseif layoutSettings.VT_HDR_IMAGE_URL?exists>
          <#assign headerImageUrl = layoutSettings.VT_HDR_IMAGE_URL.get(0)>
        </#if>
        <#if headerImageUrl?exists>
          <li class="logo-area"><a href="<@ofbizUrl>${logoLinkURL}</@ofbizUrl>"><img alt="${layoutSettings.companyName}" src="<@ofbizContentUrl>${headerImageUrl}</@ofbizContentUrl>"/></a></li>
        </#if>
        <li class="control-area"<#if layoutSettings.headerRightBackgroundUrl?has_content> background="${layoutSettings.headerRightBackgroundUrl}"</#if>>
          <#if userLogin?exists>
            <p class="expanded">
              <a href="<@ofbizUrl>LookupVisualThemes</@ofbizUrl>" class="buttontext">${uiLabelMap.VisualThemes}</a>
              <a href="setUserPreference?userPrefGroupId=GLOBAL_PREFERENCES&amp;userPrefTypeId=COMPACT_HEADER&amp;userPrefValue=Y">&nbsp;&nbsp;</a>
            </p>
          </#if>
          <p>
          <#if person?has_content>
            ${uiLabelMap.CommonWelcome} ${person.firstName?if_exists} ${person.lastName?if_exists} [${userLogin.userLoginId}]
          <#elseif partyGroup?has_content>
            ${uiLabelMap.CommonWelcome} ${partyGroup.groupName?if_exists} [${userLogin.userLoginId}]
          <#else>
            ${uiLabelMap.CommonWelcome}!
          </#if>
          </p>
          <p>${nowTimestamp?datetime?string.short}</p>
          <p>${uiLabelMap.CommonLanguageTitle} : ${locale.getDisplayName(locale)} <a href="<@ofbizUrl>LookupLocales</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonChange}</a></p>
          <p>${timeZone.getDisplayName(timeZone.useDaylightTime(), Static["java.util.TimeZone"].LONG, locale)} <a href="<@ofbizUrl>LookupTimezones</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonChange}</a></p>
        </li>
      </#if>
    </ul>
    <br class="clear" />
  </div>
