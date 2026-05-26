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

<#assign lastLocaleId = (userLogin.lastLocale)!>
<#assign selectedLocaleId = lastLocaleId>
<#if !(selectedLocaleId?has_content)>
  <#assign selectedLocaleId = locale.toString()>
</#if>
<#assign lastTimeZoneId = (userLogin.lastTimeZone)!>
<#assign selectedTimeZoneId = lastTimeZoneId>
<#if !(selectedTimeZoneId?has_content) && timeZone??>
  <#assign selectedTimeZoneId = timeZone.getID()>
</#if>
<#assign displayStyle = Static["java.util.TimeZone"].LONG>
<#assign availableLocales = Static["org.apache.ofbiz.base.util.UtilMisc"].availableLocales()>
<#assign availableTimeZones = Static["org.apache.ofbiz.base.util.UtilDateTime"].availableTimeZones()>

<#assign lastLocaleDisplay = uiLabelMap.CommonNA>
<#if lastLocaleId?has_content>
  <#assign lastLocaleObject = Static["org.apache.ofbiz.base.util.UtilMisc"].parseLocale(lastLocaleId)>
  <#assign lastLocaleDisplay = lastLocaleObject.getDisplayName(locale) + " [" + lastLocaleId?replace("_", "-") + "]">
</#if>

<#assign lastTimeZoneDisplay = uiLabelMap.CommonNA>
<#if lastTimeZoneId?has_content>
  <#assign lastTimeZone = Static["java.util.TimeZone"].getTimeZone(lastTimeZoneId)>
  <#assign lastTimeZoneDisplay = lastTimeZone.getDisplayName(lastTimeZone.useDaylightTime(), displayStyle, locale) + " (" + lastTimeZone.getID() + ")">
</#if>

<div class="screenlet">
  <div class="screenlet-title-bar">
    <ul>
      <li class="h3">${uiLabelMap.CommonProfile}</li>
    </ul>
    <br class="clear"/>
  </div>
  <div class="screenlet-body">
    <table cellspacing="0" class="basic-table">
      <tr>
        <td class="label">${uiLabelMap.CommonUserLoginId}</td>
        <td>${((userLogin.userLoginId)!uiLabelMap.CommonNA)}</td>
      </tr>
      <tr>
        <td class="label">${uiLabelMap.CommonUsername}</td>
        <td>${((userLogin.userFullName)!uiLabelMap.CommonNA)}</td>
      </tr>
      <tr>
        <td class="label">${uiLabelMap.CommonLanguageTitle}</td>
        <td>${lastLocaleDisplay!}</td>
      </tr>
      <tr>
        <td class="label">${uiLabelMap.CommonTimeZone}</td>
        <td>${lastTimeZoneDisplay!}</td>
      </tr>
    </table>
  </div>
</div>

<div class="screenlet">
  <div class="screenlet-title-bar">
    <ul>
      <li class="h3">${uiLabelMap.CommonLanguageTitle}</li>
    </ul>
    <br class="clear"/>
  </div>
  <div class="screenlet-body">
    <form method="post" action="<@ofbizUrl>setUserLocale</@ofbizUrl>">
      <div>
        <select name="newLocale">
          <#list availableLocales as availableLocale>
            <#assign availableLocaleId = availableLocale.toString()>
            <#assign langAttr = availableLocaleId?replace("_", "-")>
            <option value="${availableLocaleId}"<#if availableLocaleId == selectedLocaleId!> selected="selected"</#if>>
              ${availableLocale.getDisplayName(availableLocale)!} [${langAttr!}]
            </option>
          </#list>
        </select>
      </div>
      <div>
        <input type="submit" value="${uiLabelMap.CommonUpdate}"/>
      </div>
    </form>
  </div>
</div>

<div class="screenlet">
  <div class="screenlet-title-bar">
    <ul>
      <li class="h3">${uiLabelMap.CommonTimeZone}</li>
    </ul>
    <br class="clear"/>
  </div>
  <div class="screenlet-body">
    <form method="get" action="<@ofbizUrl>setUserTimeZone</@ofbizUrl>">
      <div>
        <select name="tzId">
          <#list availableTimeZones as availableTz>
            <#assign availableTzId = availableTz.getID()>
            <option value="${availableTzId}"<#if availableTzId == selectedTimeZoneId!> selected="selected"</#if>>
              ${availableTz.getDisplayName(availableTz.useDaylightTime(), displayStyle, locale)} (${availableTzId})
            </option>
          </#list>
        </select>
      </div>
      <div>
        <input name="submitButton" type="submit" value="${uiLabelMap.CommonUpdate}"/>
      </div>
    </form>
  </div>
</div>
