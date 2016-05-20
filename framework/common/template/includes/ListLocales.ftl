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
<div class="lists screenlet">
  <div class="screenlet-title-bar">
    <ul>
      <li class="h3">${uiLabelMap.CommonLanguageTitle}</li>
      <li><a href="<@ofbizUrl>main</@ofbizUrl>">${uiLabelMap.CommonCancel}</a></li>
    </ul>
    <br class="clear"/>
  </div>
  <table cellspacing="0" class="basic-table hover-bar">
    <#assign altRow = true>
    <#assign availableLocales = Static["org.ofbiz.base.util.UtilMisc"].availableLocales()/>
    <#list availableLocales as availableLocale>
        <#assign altRow = !altRow>
        <#assign langAttr = availableLocale.toString()?replace("_", "-")>
        <#assign langDir = "ltr">
        <#if "ar.iw"?contains(langAttr?substring(0, 2))>
            <#assign langDir = "rtl">
        </#if>
        <tr <#if altRow>class="alternate-row"</#if>>
            <td lang="${langAttr}" dir="${langDir}">
                <a href="<@ofbizUrl>setSessionLocale</@ofbizUrl>?newLocale=${availableLocale.toString()}">${availableLocale.getDisplayName(availableLocale)} &nbsp;&nbsp;&nbsp;-&nbsp;&nbsp;&nbsp; [${langAttr}]</a>
            </td>
        </tr>
    </#list>
  </table>
</div>
