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
  <table class="basic-table hover-bar" cellspacing="3">
    <tr class="header-row">
      <td>${uiLabelMap.WebtoolsLabelManagerRow}</td>
      <td>${uiLabelMap.WebtoolsLabelManagerKey}</td>
      <td>${uiLabelMap.WebtoolsLabelManagerFileName}</td>
      <td>${uiLabelMap.WebtoolsLabelManagerReferences}</td>
      <#list localesFound as localeFound>
        <#assign showLocale = true>
        <#if parameters.labelLocaleName?? && parameters.labelLocaleName != "" && parameters.labelLocaleName != localeFound>
          <#assign showLocale = false>
        </#if>
        <#if showLocale == true>
          <#assign locale = Static["org.ofbiz.base.util.UtilMisc"].parseLocale(localeFound)!/>
          <#if locale?? && locale?has_content>
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
    <#if parameters.searchLabels??>
      <#assign rowNum = "2">
      <#assign rowNumber = 1>
      <#assign totalLabels = 0>
      <#assign missingLabels = 0>
      <#assign existingLabels = 0>
      <#assign previousKey = "">
      <#list labelsList as labelList>
        <#assign label = labels.get(labelList)>
        <#assign labelKey = label.labelKey>
        <#assign totalLabels = totalLabels + 1>
        <#if references??>
          <#assign referenceNum = 0>
          <#assign reference = references.get(labelKey)!>
          <#if reference?? && reference?has_content>
            <#assign referenceNum = reference.size()>
          </#if>
        </#if>
        <#assign showLabel = true>
        <#if parameters.onlyMissingTranslations?? && parameters.onlyMissingTranslations == "Y" 
            && parameters.labelLocaleName?? && parameters.labelLocaleName != "">
          <#assign labelValue = label.getLabelValue(parameters.labelLocaleName)!>
          <#if labelValue?? && labelValue?has_content>
            <#assign value = labelValue.getLabelValue()!>
            <#if value?? && value?has_content>
              <#assign showLabel = false>
            </#if>
          </#if>
        </#if>
        <#if showLabel && parameters.onlyNotUsedLabels?? && parameters.onlyNotUsedLabels == "Y" && (referenceNum > 0)>
            <#assign showLabel = false>
        </#if>
        <#if showLabel && parameters.labelKey?? && parameters.labelKey != "" && parameters.labelKey != label.labelKey>
          <#assign showLabel = false>
        </#if>
        <#if showLabel && parameters.labelFileName?? && parameters.labelFileName != "" && parameters.labelFileName != label.fileName>
          <#assign showLabel = false>
        </#if>
        <#if showLabel == true>
          <tr <#if rowNum == "1">class="alternate-row"</#if>>
            <td>${rowNumber}</td>
            <td><a href="<@ofbizUrl>UpdateLabel?sourceKey=${labelKey}&amp;sourceFileName=${label.fileName}&amp;sourceKeyComment=${label.labelKeyComment!}</@ofbizUrl>" <#if previousKey == labelKey>class="submenutext"</#if>>${label.labelKey}</a></td>
            <td>${label.fileName}</td>
            <td><a href="<@ofbizUrl>ViewReferences?sourceKey=${labelKey}&amp;labelFileName=${label.fileName}</@ofbizUrl>">${uiLabelMap.WebtoolsLabelManagerReferences}</a></td>
            <#list localesFound as localeFound>
              <#assign labelVal = label.getLabelValue(localeFound)!>
              <#assign showLocale = true>
              <#if parameters.labelLocaleName?? && parameters.labelLocaleName != "" && parameters.labelLocaleName != localeFound>
                <#assign showLocale = false>
              </#if>
              <#if showLocale>
                <#if labelVal?has_content>
                  <td>${labelVal.getLabelValue()}</td>
                  <#assign existingLabels = existingLabels + 1>
                <#else>
                  <td>&nbsp;</td>
                  <#assign missingLabels = missingLabels + 1>
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
          <#assign rowNumber = rowNumber + 1>
        </#if>
      </#list>
      <tr class="header-row">
        <td/>
        <td>${uiLabelMap.WebtoolsLabelStatsTotal}: ${totalLabels}</td>
        <td colspan="2">
          ${uiLabelMap.WebtoolsLabelStatsExist}:<br />
          ${uiLabelMap.WebtoolsLabelStatsMissing}:
        </td>
        <td>
          ${existingLabels}<br />
          ${missingLabels}
        </td>
      </tr>
    </#if>
  </table>
</div>
