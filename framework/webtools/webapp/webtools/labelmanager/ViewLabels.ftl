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
<div class="screenlet-body">
  <#if parameters.searchLabels?exists>
    <a href="<@ofbizUrl>UpdateLabel</@ofbizUrl>" class="buttontext">${uiLabelMap.WebtoolsLabelManagerAddNew}</a>
    <a href="<@ofbizUrl>SaveLabelsToXmlFile?labelFileName=${parameters.labelFileName?if_exists}</@ofbizUrl>" class="buttontext">${uiLabelMap.WebtoolsLabelManagerUpdateFile}</a>
  </#if>
  <table class="basic-table hover-bar" cellspacing="3">
    <tr class="header-row">
      <td>${uiLabelMap.WebtoolsLabelManagerKey}</td>
      <td>${uiLabelMap.WebtoolsLabelManagerFileName}</td>
      <td>${uiLabelMap.WebtoolsLabelManagerReferences}</td>
      <#list localesFound as localeFound>
        <#assign showLocale = true>
        <#if parameters.labelLocaleName?exists && parameters.labelLocaleName != "" && parameters.labelLocaleName != localeFound>
          <#assign showLocale = false>
        </#if>
        <#if showLocale == true>
          <#assign locale = Static["org.ofbiz.base.util.UtilMisc"].parseLocale(localeFound)?if_exists/>
          <#if locale?exists && locale?has_content>
            <#assign langAttr = localeFound.toString()?replace("_", "-")>
            <#assign langDir = "ltr">
            <#if "ar.iw"?contains(langAttr?substring(0, 2))>
              <#assign langDir = "rtl">
            </#if>
            <td lang="${langAttr}" dir="${langDir}">
              ${locale.getDisplayName(locale)}
            </td>
          <#else>
            <td>${localeFound}</td>
          </#if>
        </#if>
      </#list>
    </tr>
    <#if parameters.searchLabels?exists>
      <#assign rowNum = "2">
      <#assign previousKey = "">
      <#list labelsList as labelList>
        <#assign label = labels.get(labelList)>
        <#assign showLabel = true>
        <#if parameters.labelKey?exists && parameters.labelKey != "" && parameters.labelKey != label.labelKey>
          <#assign showLabel = false>
        </#if>
        <#if showLabel && parameters.labelFileName?exists && parameters.labelFileName != "" && parameters.labelFileName != label.fileName>
          <#assign showLabel = false>
        </#if>
        <#if showLabel && parameters.labelComponentName?exists && parameters.labelComponentName != "" && parameters.labelComponentName != label.componentName>
          <#assign showLabel = false>
        </#if>
        <#if showLabel == true>
          <#assign labelKey = label.labelKey>
          <tr <#if rowNum == "1">class="alternate-row"</#if>>
            <td><a href="<@ofbizUrl>UpdateLabel?sourceKey=${labelKey}&sourceFileName=${label.fileName}&sourceKeyComment=${label.labelKeyComment?if_exists}</@ofbizUrl>" <#if previousKey == labelKey>class="submenutext"</#if>>${label.labelKey}</a></td>
            <td>${label.fileName}</td>
            <#assign referenceNum = 0>
            <#assign reference = references.get(labelKey)?if_exists>
            <#if reference?exists && reference?has_content>
                 <#assign referenceNum = reference.size()>
            </#if>
            <td align="center">${referenceNum}</td>
            <#list localesFound as localeFound>
              <#assign labelVal = label.getLabelValue(localeFound)?if_exists>
              <#assign showLocale = true>
              <#if parameters.labelLocaleName?exists && parameters.labelLocaleName != "" && parameters.labelLocaleName != localeFound>
                <#assign showLocale = false>
              </#if>
              <#if showLocale == true>
                <#if labelVal?has_content>
                  <td>${labelVal.getLabelValue()}</td>
                <#else>
                  <td>&nbsp;</td>
                </#if>
              </#if>
            </#list>
          </tr>
          <#if rowNum == "2">
            <#assign rowNum = "1">
          <#else>
            <#assign rowNum = "2">
          </#if>
          <#assign previousKey = labelKey>
        </#if>
      </#list> 
    </#if>
  </table>
</div>
