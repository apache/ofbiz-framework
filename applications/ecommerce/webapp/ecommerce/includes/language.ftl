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

<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxhead">${uiLabelMap.CommonLanguageTitle}</div>
    </div>
    <div class="screenlet-body" style="text-align: center;">
        <form method="post" name="chooseLanguage" action="<@ofbizUrl>setSessionLocale</@ofbizUrl>" style="margin: 0;">
          <select name="locale" class="selectBox">
            <#assign initialDisplayName = locale.getDisplayName(locale)>
            <#if 18 < initialDisplayName?length>
              <#assign initialDisplayName = initialDisplayName[0..15] + "...">
            </#if>
            <option value="${locale.toString()}">${initialDisplayName}</option>
            <option value="${locale.toString()}">----</option>
            <#list availableLocales as availableLocale>
              <#assign displayName = availableLocale.getDisplayName(locale)>
              <#if 18 < displayName?length>
                <#assign displayName = displayName[0..15] + "...">
              </#if>
              <option value="${availableLocale.toString()}">${displayName}</option>
            </#list>
          </select>
          <div><a href="javascript:document.chooseLanguage.submit()" class="buttontext">${uiLabelMap.CommonChange}</a></div>
        </form>
    </div>
</div>
