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
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <title><#if title?has_content>${title}<#elseif titleProperty?has_content>${uiLabelMap.get(titleProperty)}</#if>: ${(productStore.storeName)?if_exists}</title>
  <#if layoutSettings.VT_SHORTCUT_ICON?has_content>
    <#assign shortcutIcon = layoutSettings.VT_SHORTCUT_ICON.get(0)/>
  <#elseif layoutSettings.shortcutIcon?has_content>
    <#assign shortcutIcon = layoutSettings.shortcutIcon/>
  </#if>
  <#if shortcutIcon?has_content>
    <link rel="shortcut icon" href="<@ofbizContentUrl>${StringUtil.wrapString(shortcutIcon)}</@ofbizContentUrl>" />
  </#if>
  <#if layoutSettings.javaScripts?has_content>
    <#--layoutSettings.javaScripts is a list of java scripts. -->
    <#-- use a Set to make sure each javascript is declared only once, but iterate the list to maintain the correct order -->
    <#assign javaScriptsSet = Static["org.ofbiz.base.util.UtilMisc"].toSet(layoutSettings.javaScripts)/>
    <#list layoutSettings.javaScripts as javaScript>
      <#if javaScriptsSet.contains(javaScript)>
        <#assign nothing = javaScriptsSet.remove(javaScript)/>
        <script type="text/javascript" src="<@ofbizContentUrl>${StringUtil.wrapString(javaScript)}</@ofbizContentUrl>"></script>
      </#if>
    </#list>
  </#if>
  <#if layoutSettings.VT_HDR_JAVASCRIPT?has_content>
    <#list layoutSettings.VT_HDR_JAVASCRIPT as javaScript>
      <script type="text/javascript" src="<@ofbizContentUrl>${StringUtil.wrapString(javaScript)}</@ofbizContentUrl>"></script>
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
  <#if layoutSettings.rtlStyleSheets?has_content && langDir == "rtl">
    <#--layoutSettings.rtlStyleSheets is a list of rtl style sheets.-->
    <#list layoutSettings.rtlStyleSheets as styleSheet>
      <link rel="stylesheet" href="<@ofbizContentUrl>${StringUtil.wrapString(styleSheet)}</@ofbizContentUrl>" type="text/css"/>
    </#list>
  </#if>
  <#if layoutSettings.VT_RTL_STYLESHEET?has_content && langDir == "rtl">
    <#list layoutSettings.VT_RTL_STYLESHEET as styleSheet>
      <link rel="stylesheet" href="<@ofbizContentUrl>${StringUtil.wrapString(styleSheet)}</@ofbizContentUrl>" type="text/css"/>
    </#list>
  </#if>
  ${layoutSettings.extraHead?if_exists}
  <#if layoutSettings.VT_EXTRA_HEAD?has_content>
    <#list layoutSettings.VT_EXTRA_HEAD as extraHead>
      ${extraHead}
    </#list>
  </#if>

  <#-- Append CSS for catalog -->
  <#if catalogStyleSheet?exists>
    <link rel="stylesheet" href="${StringUtil.wrapString(catalogStyleSheet)}" type="text/css"/>
  </#if>
  <#-- Append CSS for tracking codes -->
  <#if sessionAttributes.overrideCss?exists>
    <link rel="stylesheet" href="${StringUtil.wrapString(sessionAttributes.overrideCss)}" type="text/css"/>
  </#if>
  <#-- Meta tags if defined by the page action -->
  <#if metaDescription?exists>
    <meta name="description" content="${metaDescription}"/>
  </#if>
  <#if metaKeywords?exists>
    <meta name="keywords" content="${metaKeywords}"/>
  </#if>
</head>
