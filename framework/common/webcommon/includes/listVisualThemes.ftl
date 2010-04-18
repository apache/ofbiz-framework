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
<!--
<style type="text/css">
.screenlet {
margin: 1em;
}
</style>
-->
<div class="screenlet" style="margin: 1em;">
  <div class="screenlet-title-bar">
    <ul>
      <li class="h3">${uiLabelMap.CommonVisualThemes}</li>
      <li><a href="<@ofbizUrl>main</@ofbizUrl>">${uiLabelMap.CommonDone}</a></li>
    </ul>
    <br class="clear"/>
  </div>
  <#if visualThemes?has_content>
    <#assign orderByList = Static["org.ofbiz.base.util.UtilMisc"].toList("visualThemeId")/>
    <table cellspacing="0" class="basic-table">
      <#list visualThemes as visualTheme>
        <#assign screenshots = delegator.findByAnd("VisualThemeResource", Static["org.ofbiz.base.util.UtilMisc"].toMap(
                                        "visualThemeId", "${visualTheme.visualThemeId}",
                                        "resourceTypeEnumId", "VT_SCREENSHOT"), orderByList)>
        <tr<#if visualTheme.visualThemeId == visualThemeId> class="selected"</#if>>
          <td>
            <form name="SetUserPreferences_${visualTheme.visualThemeId}" method="post" action="<@ofbizUrl>setUserPreference</@ofbizUrl>">
              <input type="hidden" name="userPrefGroupTypeId" value="GLOBAL_PREFERENCES"/>
              <input type="hidden" name="userPrefTypeId" value="VISUAL_THEME"/>
              <input type="hidden" name="userPrefValue" value="${visualTheme.visualThemeId}"/>
            </form>
            <a href="javascript:document.SetUserPreferences_${visualTheme.visualThemeId}.submit()">${visualTheme.get("description", locale)?default(visualTheme.visualThemeId)}</a>
          </td>
          <td>
            <#if visualTheme.visualThemeId == visualThemeId>${uiLabelMap.CommonVisualThemeSelected}<#else>&nbsp;</#if>
          </td>
          <td>
            <#if screenshots?has_content>
              <#list screenshots as screenshot>
                <a href="<@ofbizContentUrl>${screenshot.resourceValue}</@ofbizContentUrl>"><img src="<@ofbizContentUrl>${screenshot.resourceValue}</@ofbizContentUrl>" width="150" alt=""/></a>
              </#list>
           <#else>
              ${uiLabelMap.CommonVisualThemeNoScreenshots}
            </#if>
          </td>
        </tr>
      </#list>
    </table>
  </#if>
</div>
