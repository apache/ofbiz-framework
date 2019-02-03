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
<#assign docLangAttr = locale.toString()?replace("_", "-")>
<#assign langDir = "ltr">
<#if "ar.iw"?contains(docLangAttr?substring(0, 2))>
    <#assign langDir = "rtl">
</#if>
<html lang="${docLangAttr}" dir="${langDir}" xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <title>${layoutSettings.companyName}: <#if (titleProperty)?has_content>${uiLabelMap[titleProperty]}<#else>${title!}</#if></title>
    <#if layoutSettings.shortcutIcon?has_content>
      <#assign shortcutIcon = layoutSettings.shortcutIcon/>
    <#elseif layoutSettings.VT_SHORTCUT_ICON?has_content>
      <#assign shortcutIcon = layoutSettings.VT_SHORTCUT_ICON/>
    </#if>
    <#if shortcutIcon?has_content>
        <link rel="shortcut icon" href="<@ofbizContentUrl>${StringUtil.wrapString(shortcutIcon)+".ico"}</@ofbizContentUrl>" type="image/x-icon">
        <link rel="icon" href="<@ofbizContentUrl>${StringUtil.wrapString(shortcutIcon)+".png"}</@ofbizContentUrl>" type="image/png">
        <link rel="icon" sizes="32x32" href="<@ofbizContentUrl>${StringUtil.wrapString(shortcutIcon)+"-32.png"}</@ofbizContentUrl>" type="image/png">
        <link rel="icon" sizes="64x64" href="<@ofbizContentUrl>${StringUtil.wrapString(shortcutIcon)+"-64.png"}</@ofbizContentUrl>" type="image/png">
        <link rel="icon" sizes="96x96" href="<@ofbizContentUrl>${StringUtil.wrapString(shortcutIcon)+"-96.png"}</@ofbizContentUrl>" type="image/png">
    </#if>
    <#if layoutSettings.VT_HDR_JAVASCRIPT?has_content>
        <#list layoutSettings.VT_HDR_JAVASCRIPT as javaScript>
            <script src="<@ofbizContentUrl>${StringUtil.wrapString(javaScript)}</@ofbizContentUrl>" type="application/javascript"></script>
        </#list>
    </#if>
    <#if layoutSettings.javaScripts?has_content>
      <#--layoutSettings.javaScripts is a list of java scripts. -->
      <#-- use a Set to make sure each javascript is declared only once, but iterate the list to maintain the correct order -->
      <#assign javaScriptsSet = Static["org.apache.ofbiz.base.util.UtilMisc"].toSet(layoutSettings.javaScripts)/>
      <#list layoutSettings.javaScripts as javaScript>
        <#if javaScriptsSet.contains(javaScript)>
          <#assign nothing = javaScriptsSet.remove(javaScript)/>
          <script src="<@ofbizContentUrl>${StringUtil.wrapString(javaScript)}</@ofbizContentUrl>" type="application/javascript"></script>
        </#if>
      </#list>
    </#if>

    <#if layoutSettings.styleSheets?has_content>
        <#--layoutSettings.styleSheets is a list of style sheets. So, you can have a user-specified "main" style sheet, AND a component style sheet.-->
        <#list layoutSettings.styleSheets as styleSheet>
            <link rel="stylesheet" href="<@ofbizContentUrl>${StringUtil.wrapString(styleSheet)}</@ofbizContentUrl>" type="text/css"/>
        </#list>
    </#if>
    <#if layoutSettings.VT_STYLESHEET?has_content>
        <#list layoutSettings.VT_STYLESHEET as styleSheet>
            <link rel="stylesheet" href="<@ofbizContentUrl>${StringUtil.wrapString(styleSheet)}</@ofbizContentUrl>" type="text/css"/>
        </#list>
    </#if>
    <#if layoutSettings.rtlStyleSheets?has_content && "rtl" == langDir>
        <#--layoutSettings.rtlStyleSheets is a list of rtl style sheets.-->
        <#list layoutSettings.rtlStyleSheets as styleSheet>
            <link rel="stylesheet" href="<@ofbizContentUrl>${StringUtil.wrapString(styleSheet)}</@ofbizContentUrl>" type="text/css"/>
        </#list>
    </#if>
    <#if layoutSettings.VT_RTL_STYLESHEET?has_content && "rtl" == langDir>
        <#list layoutSettings.VT_RTL_STYLESHEET as styleSheet>
            <link rel="stylesheet" href="<@ofbizContentUrl>${StringUtil.wrapString(styleSheet)}</@ofbizContentUrl>" type="text/css"/>
        </#list>
    </#if>
    <#if layoutSettings.VT_EXTRA_HEAD?has_content>
        <#list layoutSettings.VT_EXTRA_HEAD as extraHead>
            ${extraHead}
        </#list>
    </#if>
    <#if lastParameters??><#assign parametersURL = "&amp;" + lastParameters></#if>
    <#if layoutSettings.WEB_ANALYTICS?has_content>
      <script type="application/javascript">
        <#list layoutSettings.WEB_ANALYTICS as webAnalyticsConfig>
          ${StringUtil.wrapString(webAnalyticsConfig.webAnalyticsCode!)}
        </#list>
      </script>
    </#if>
</head>
<#if layoutSettings.headerImageLinkUrl??>
  <#assign logoLinkURL = "${layoutSettings.headerImageLinkUrl}">
<#else>
  <#assign logoLinkURL = "${layoutSettings.commonHeaderImageLinkUrl}">
</#if>
<#assign organizationLogoLinkURL = "${layoutSettings.organizationLogoLinkUrl!}">
<body>
  <#include "component://common-theme/template/ImpersonateBanner.ftl"/>
  <div id="wait-spinner" style="display:none">
    <div id="wait-spinner-image"></div>
  </div>
  <div class="page-container">
  <div class="hidden">
    <a href="#column-container" title="${uiLabelMap.CommonSkipNavigation}" accesskey="2">
      ${uiLabelMap.CommonSkipNavigation}
    </a>
  </div>
  <div id="masthead">
    <ul>
      <#if layoutSettings.headerImageUrl??>
        <#assign headerImageUrl = layoutSettings.headerImageUrl>
      <#elseif layoutSettings.commonHeaderImageUrl??>
        <#assign headerImageUrl = layoutSettings.commonHeaderImageUrl>
      <#elseif layoutSettings.VT_HDR_IMAGE_URL??>
        <#assign headerImageUrl = layoutSettings.VT_HDR_IMAGE_URL>
      </#if>
      <#if headerImageUrl??>
        <#if organizationLogoLinkURL?has_content>
            <li class="org-logo-area"><a href="<@ofbizUrl>${logoLinkURL}</@ofbizUrl>"><img alt="${layoutSettings.companyName}" src="<@ofbizContentUrl>${StringUtil.wrapString(organizationLogoLinkURL)}</@ofbizContentUrl>"></a></li>
            <#else>
            <li class="logo-area"><a href="<@ofbizUrl>${logoLinkURL}</@ofbizUrl>"><img alt="${layoutSettings.companyName}" src="<@ofbizContentUrl>${StringUtil.wrapString(headerImageUrl)}</@ofbizContentUrl>"/></a></li>
        </#if>
      </#if>
      <#if layoutSettings.middleTopMessage1?has_content && layoutSettings.middleTopMessage1 != " ">
        <li>
        <div class="last-system-msg">
        <center>${layoutSettings.middleTopHeader!}</center>
        <a href="${layoutSettings.middleTopLink1!}">${layoutSettings.middleTopMessage1!}</a><br/>
        <a href="${layoutSettings.middleTopLink2!}">${layoutSettings.middleTopMessage2!}</a><br/>
        <a href="${layoutSettings.middleTopLink3!}">${layoutSettings.middleTopMessage3!}</a>
        </div>
        </li>
      </#if>
      <li class="preference-area">
          <ul>
          <#if userLogin??>
            <#if layoutSettings.topLines?has_content>
              <#list layoutSettings.topLines as topLine>
                <#if topLine.text??>
                  <li>${topLine.text}<a href="${StringUtil.wrapString(topLine.url!)}${StringUtil.wrapString(externalKeyParam)}">${topLine.urlText!}</a></li>
                <#elseif topLine.dropDownList??>
                  <li><#include "component://common-theme/template/includes/InsertDropDown.ftl"/></li>
                <#else>
                  <li>${topLine!}</li>
                </#if>
              </#list>
            <#else>
              <li>${userLogin.userLoginId}</li>
            </#if>
            <li><a href="<@ofbizUrl>logout</@ofbizUrl>">${uiLabelMap.CommonLogout}</a></li>
          <#else>
            <li>${uiLabelMap.CommonWelcome}! <a href="<@ofbizUrl>${checkLoginUrl}</@ofbizUrl>">${uiLabelMap.CommonLogin}</a></li>
          </#if>
          <#---if webSiteId?? && requestAttributes._CURRENT_VIEW_?? && helpTopic??-->
          <#if parameters.componentName?? && requestAttributes._CURRENT_VIEW_?? && helpTopic??>
            <#include "component://common-theme/template/includes/HelpLink.ftl" />
            <li><a <#if pageAvail?has_content>class="alert"</#if> href="javascript:lookup_popup1('showHelp?helpTopic=${helpTopic}&amp;portalPageId=${(parameters.portalPageId!)?html}','help' ,500,500);">${uiLabelMap.CommonHelp}</a></li>
          </#if>
          </ul>
      </li>
    </ul>
  </div>
  <#--<br class="clear" />-->
