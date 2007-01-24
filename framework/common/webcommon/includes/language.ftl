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
<#assign uiLabelMap = requestAttributes.uiLabelMap>
<#assign locale = Static["org.ofbiz.base.util.UtilHttp"].getLocale(session)>

<table border="0" width='100%' cellspacing='0' cellpadding='0' class='boxoutside'>
  <tr>
    <td width='100%'>
      <table width='100%' border='0' cellspacing='0' cellpadding='0' class='boxtop'>
        <tr>
          <td valign='middle' align='center'>
            <div class="boxhead">${uiLabelMap.CommonLanguageTitle}</div>
          </td>
        </tr>
      </table>
    </td>
  </tr>
  <tr>
    <td width='100%'>
      <table width='100%' border='0' cellspacing='0' cellpadding='0' class='boxbottom'>
        <tr>
          <td align='center'>
            <form method="post" name="chooseLanguage" action="<@ofbizUrl>setSessionLocale</@ofbizUrl>" style="margin: 0;">
              <select name="locale" class="selectBox">
                <#assign initialDisplayName = locale.getDisplayName(locale)>
                <#if 18 < initialDisplayName?length>
                  <assign initialDisplayName = initialDisplayName[0..15] + "...">
                </#if>
                <option value="${requestAttributes.locale.toString()}">${initialDisplayName}</option>
                <option value="${requestAttributes.locale.toString()}">----</option>
                <#list requestAttributes.availableLocales as availableLocale>
                  <#assign displayName = availableLocale.getDisplayName(locale)>
                  <#if 18 < displayName?length>
                    <#assign displayName = displayName[0..15] + "...">
                  </#if>
                  <option value="${availableLocale.toString()}">${displayName}</option>
                </#list>
              </select>
              <div><a href="javascript:document.chooseLanguage.submit()" class="buttontext">${uiLabelMap.CommonChooseLanguage}</a></div>
            </form>
          </td>
        </tr>
      </table>
    </td>
  </tr>
</table>
<br/>
