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

<div id="manage-portal-toolbar">
  <ul>
    <#if currentPortalPage.portalPageName?has_content>
      <li id="portal-page-name">
        ${currentPortalPage.portalPageName}
      </li>
    </#if>

    <#if (portalPages.size() > 1)>
      <li id="portal-page-select">
        <select name="selectPortal" onchange="window.location=this[this.selectedIndex].value;">
          <option>${uiLabelMap.CommonSelectPortalPage}</option>
          <#list portalPages as portalPage>
            <#if (currentPortalPage.portalPageId != portalPage.portalPageId)>
              <option value="<@ofbizUrl>dashboard?portalPageId=${portalPage.portalPageId}</@ofbizUrl>">${portalPage.portalPageName}</option>
            </#if>
          </#list>
        </select>
      </li>
    </#if>

    <#if configurePortalPage?has_content>
      <li id="add-portlet">
        <a href="<@ofbizUrl>AddPortlet?portalPageId=${currentPortalPage.portalPageId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonAddAPortlet}</a>
      </li>
    </#if>

  </ul>
  <br class="clear"/>
</div>