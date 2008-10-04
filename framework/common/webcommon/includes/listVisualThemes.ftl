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

<style type="text/css">
.screenlet {
margin: 1em;
}
</style>

<div class="screenlet">
  <div class="screenlet-title-bar">
    <ul>
      <li class="h3">${uiLabelMap.VisualThemes}</li>
      <li><a href="<@ofbizUrl>main</@ofbizUrl>">${uiLabelMap.CommonCancel}</a></li>
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
            <#-- Not too sure about displaying the theme ID - it might confuse the users -->
            <#-- <a href="<@ofbizUrl>updateVisualTheme?userPrefGroupId=GLOBAL_PREFERENCES&amp;userPrefTypeId=VISUAL_THEME&amp;userPrefValue=${visualTheme.visualThemeId}</@ofbizUrl>">${visualTheme.description} [${visualTheme.visualThemeId}]</a> -->
            <a href="<@ofbizUrl>setUserPreference?userPrefGroupId=GLOBAL_PREFERENCES&amp;userPrefTypeId=VISUAL_THEME&amp;userPrefValue=${visualTheme.visualThemeId}</@ofbizUrl>">${visualTheme.description}</a>
          </td>
          <td>
            <#if visualTheme.visualThemeId == visualThemeId>${uiLabelMap.VisualThemeSelected}<#else>&nbsp;</#if>
          </td>
          <td>
            <#if screenshots?has_content>
              <#list screenshots as screenshot>
                <a href="<@ofbizContentUrl>${screenshot.resourceValue}</@ofbizContentUrl>"><img src="<@ofbizContentUrl>${screenshot.resourceValue}</@ofbizContentUrl>" width="150"/></a>
              </#list>
           <#else>
              ${uiLabelMap.VisualThemeNoScreenshots}
            </#if>
          </td>
        </tr>
      </#list>
    </table>
  </#if>
</div>
