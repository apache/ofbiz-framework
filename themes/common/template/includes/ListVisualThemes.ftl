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
  <li class="h2" style="padding-top:1em">${uiLabelMap.CommonVisualThemeUsage}</li>
  <#assign currentVisualThemeId = visualTheme.getVisualThemeId()/>
  <#if visualThemes?has_content>
    <table cellspacing="0" class="basic-table">
      <#list visualThemes as visualTheme>
        <#assign visualThemeId = visualTheme.getVisualThemeId()/>
        <tr<#if visualThemeId == currentVisualThemeId> class="selected"</#if>>
          <td>
            <form name="SetUserPreferences_${visualThemeId}" method="post"
                action="<@ofbizUrl>selectTheme</@ofbizUrl>">
              <input type="hidden" name="userPrefGroupTypeId" value="GLOBAL_PREFERENCES"/>
              <input type="hidden" name="userPrefTypeId" value="VISUAL_THEME"/>
              <input type="hidden" name="userPrefValue" value="${visualThemeId}"/>
            </form>
            <a href="javascript:document.forms['SetUserPreferences_${visualThemeId}'].submit()">
              ${visualTheme.getDisplayName(context)?default(visualThemeId)} ${visualTheme.getDescription(context)!}
            </a>
          </td>
          <td>
            <#if visualThemeId == currentVisualThemeId>${uiLabelMap.CommonVisualThemeSelected}<#else>&nbsp;</#if>
          </td>
          <td>
            <#if visualTheme.getScreenshots()?has_content>
              <#list visualTheme.getScreenshots() as screenshot>
                <a data-featherlight="<@ofbizContentUrl>${screenshot}</@ofbizContentUrl>"><img
                    src="<@ofbizContentUrl>${screenshot}</@ofbizContentUrl>" width="150"
                    alt=""/></a>
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
