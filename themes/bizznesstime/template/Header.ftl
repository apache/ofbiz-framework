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
<#if (requestAttributes.person)??><#assign person = requestAttributes.person></#if>
<#if (requestAttributes.partyGroup)??><#assign partyGroup = requestAttributes.partyGroup></#if>
<#assign docLangAttr = locale.toString()?replace("_", "-")>
<#assign langDir = "ltr">
<#if "ar.iw"?contains(docLangAttr?substring(0, 2))>
    <#assign langDir = "rtl">
</#if>
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="${docLangAttr}" lang="${docLangAttr}" dir="${langDir}">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta http-equiv="Content-Style-Type" content="text/css" />
    <meta http-equiv="Content-Script-Type" content="text/javascript" />
    <meta http-equiv="pragma" content="no-cache" />
    <meta http-equiv="cache-control" content="no-cache" />
    <meta http-equiv="expires" content="0" />
    <meta http-equiv="imagetoolbar" content="false" />

    <title>${layoutSettings.companyName}: <#if (page.titleProperty)?has_content>${uiLabelMap[page.titleProperty]}<#else>${(page.title)!}</#if></title>

    <meta name="robots" content="index, follow" />
    <meta name="googlebot" content="index,follow" />
    <meta name="description" content="" />
    <meta name="keywords" content="" />
    <meta name="copyright" content="" />
    <meta name="MSSmartTagsPreventParsing" content="true" />
    <meta name="author" content="" />

    <link rel="start" href="" title="" />
    <#if layoutSettings.shortcutIcon?has_content>
      <#assign shortcutIcon = layoutSettings.shortcutIcon/>
    <#elseif layoutSettings.VT_SHORTCUT_ICON?has_content>
      <#assign shortcutIcon = layoutSettings.VT_SHORTCUT_ICON.get(0)/>
    </#if>
    <#if shortcutIcon?has_content>
    <link rel="shortcut icon" href="<@ofbizContentUrl>${StringUtil.wrapString(shortcutIcon)}</@ofbizContentUrl>" />
    </#if>
    <#if layoutSettings.styleSheets?has_content>
        <#list layoutSettings.styleSheets as styleSheet>
            <link rel="stylesheet" href="<@ofbizContentUrl>${StringUtil.wrapString(styleSheet)}</@ofbizContentUrl>" media="screen,projection" type="text/css" charset="UTF-8"/>
        </#list>
    </#if>
    <#if userLogin?has_content>
    <#if layoutSettings.VT_STYLESHEET?has_content>
        <#list layoutSettings.VT_STYLESHEET as styleSheet>
            <link rel="stylesheet" href="<@ofbizContentUrl>${StringUtil.wrapString(styleSheet)}</@ofbizContentUrl>" media="screen,projection" type="text/css" charset="UTF-8"/>
        </#list>
    </#if>
    <#else>
        <link rel="stylesheet" href="/bizznesstime/css/login.css" type="text/css"/>
    </#if>
    <#if layoutSettings.rtlStyleSheets?has_content && langDir == "rtl">
        <#--layoutSettings.rtlStyleSheets is a list of rtl style sheets.-->
        <#list layoutSettings.rtlStyleSheets as styleSheet>
            <link rel="stylesheet" href="<@ofbizContentUrl>${StringUtil.wrapString(styleSheet)}</@ofbizContentUrl>" media="screen,projection" type="text/css" charset="UTF-8"/>
        </#list>
    </#if>
    <#if layoutSettings.VT_RTL_STYLESHEET?has_content && langDir == "rtl">
        <#list layoutSettings.VT_RTL_STYLESHEET as styleSheet>
            <link rel="stylesheet" href="<@ofbizContentUrl>${StringUtil.wrapString(styleSheet)}</@ofbizContentUrl>" media="screen,projection" type="text/css" charset="UTF-8"/>
        </#list>
    </#if>
    ${layoutSettings.extraHead!}
    <#if layoutSettings.VT_EXTRA_HEAD?has_content>
        <#list layoutSettings.VT_EXTRA_HEAD as extraHead>
            ${extraHead}
        </#list>
    </#if>
    
    <#if layoutSettings.VT_HDR_JAVASCRIPT?has_content>
        <#list layoutSettings.VT_HDR_JAVASCRIPT as javaScript>
            <script src="<@ofbizContentUrl>${StringUtil.wrapString(javaScript)}</@ofbizContentUrl>" type="text/javascript"></script>
        </#list>
    </#if>
    <#if layoutSettings.javaScripts?has_content>
      <#--layoutSettings.javaScripts is a list of java scripts. -->
      <#-- use a Set to make sure each javascript is declared only once, but iterate the list to maintain the correct order -->
      <#assign javaScriptsSet = Static["org.ofbiz.base.util.UtilMisc"].toSet(layoutSettings.javaScripts)/>
      <#list layoutSettings.javaScripts as javaScript>
        <#if javaScriptsSet.contains(javaScript)>
          <#assign nothing = javaScriptsSet.remove(javaScript)/>
          <script src="<@ofbizContentUrl>${StringUtil.wrapString(javaScript)}</@ofbizContentUrl>" type="text/javascript"></script>
        </#if>
      </#list>
    </#if>

    <#if layoutSettings.WEB_ANALYTICS?has_content>
      <script language="JavaScript" type="text/javascript">
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
<div id="wrap">
  <div id="wait-spinner" style="display:none">
    <div id="wait-spinner-image"></div>
  </div>
  <div id="header">
    <#if organizationLogoLinkURL?has_content>
        <div id="org-logo-area"><a href="<@ofbizUrl>${logoLinkURL}</@ofbizUrl>"><img alt="${layoutSettings.companyName}" src="<@ofbizContentUrl>${StringUtil.wrapString(organizationLogoLinkURL)}</@ofbizContentUrl>" height="43px"></a></div>
        <#else>
        <div id="logo"></div>
    </#if>
    <div id="shelf"></div>
    <div id="controls">
            <span id="prefBtn">
                <a href="#" class="contracted">${uiLabelMap.CommonPreferences}</a>
                <span id="preferences" style="display:none">
                    <a href="<@ofbizUrl>ListLocales</@ofbizUrl>" id="language">${uiLabelMap.CommonLanguageTitle} - ${locale.getDisplayName(locale)}</a>
                    <a href="<@ofbizUrl>ListTimezones</@ofbizUrl>" id="timezone">${nowTimestamp?datetime?string.short} - ${timeZone.getDisplayName(timeZone.useDaylightTime(), Static["java.util.TimeZone"].LONG, locale)}</a>
                    <a href="<@ofbizUrl>ListVisualThemes</@ofbizUrl>" id="theme">${uiLabelMap.CommonVisualThemes}</a>
                    <#if userLogin?exists>
                        <#if userLogin.partyId?exists>
                            <#assign size = companyListSize?default(0)>
                            <#if size &gt; 1>
                                <#assign currentCompany = delegator.findOne("PartyNameView", {"partyId" : organizationPartyId}, false)>
                                <#if currentCompany?exists>
                                    <a href="<@ofbizUrl>ListSetCompanies</@ofbizUrl>" id="company">${uiLabelMap.CommonCompany}</a>
                                </#if>
                            </#if>
                        </#if>
                    </#if>
                </span>
            </span>
            <span>
            <#if person?has_content>
              ${uiLabelMap.CommonWelcome},  ${person.firstName!} ${person.lastName!} ( ${userLogin.userLoginId} )
            <#elseif partyGroup?has_content>
              ${uiLabelMap.CommonWelcome},  ${partyGroup.groupName!} ( ${userLogin.userLoginId} )
            <#else>
              ${uiLabelMap.CommonWelcome}
            </#if>
            <#if userLogin?exists>
                <#if userLogin.partyId?exists>
                <#assign size = companyListSize?default(0)>
                <#if size &gt; 1>
                    <#assign currentCompany = delegator.findOne("PartyNameView", {"partyId" : organizationPartyId}, false)>
                    <#if currentCompany?exists>
                        &nbsp;- ${currentCompany.groupName}
                    </#if>
                </#if>
              </#if>
            </#if>
            </span>
            <span><a href="<@ofbizUrl>logout</@ofbizUrl>">${uiLabelMap.CommonLogout}</a></span>
            
            <#--if webSiteId?? && requestAttributes._CURRENT_VIEW_?? && helpTopic??-->
            <#if parameters.componentName?? && requestAttributes._CURRENT_VIEW_?? && helpTopic??>
              <#include "component://common/template/includes/HelpLink.ftl" />
              <span><a href="javascript:lookup_popup1('showHelp?helpTopic=${helpTopic}&amp;portalPageId=${parameters.portalPageId!}','help' ,500,500);">${uiLabelMap.CommonHelp}</a></span>
           </#if>
           <#if layoutSettings.middleTopMessage1?has_content && layoutSettings.middleTopMessage1 != " ">
             <span id="last-system-msg">
               <center>${layoutSettings.middleTopHeader!}</center>
               <a href="${layoutSettings.middleTopLink1!}">${layoutSettings.middleTopMessage1!}</a><br/>
               <a href="${layoutSettings.middleTopLink2!}">${layoutSettings.middleTopMessage2!}</a><br/>
               <a href="${layoutSettings.middleTopLink3!}">${layoutSettings.middleTopMessage3!}</a>
             </span>
           </#if>
    </div>
  </div>
