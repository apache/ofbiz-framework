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

<#-- cms menu bar -->
<div id="cmsmenu" style="margin-bottom: 8px;">
    <#if (content?has_content)>
        <a href="javascript:void(0);" onclick="javascript:callDocument(true, '${content.contentId}', '', 'ELECTRONIC_TEXT');" class="tabButton">${uiLabelMap.ContentQuickSubContent}</a>
        <a href="javascript:void(0);" onclick="javascript:callPathAlias('${content.contentId}');" class="selected">${uiLabelMap.ContentPathAlias}</a>
        <a href="javascript:void(0);" onclick="javascript:callMetaInfo('${content.contentId}');" class="tabButton">${uiLabelMap.ContentMetaTags}</a>
    </#if>
</div>

<#if (content?has_content)>
    <div style="margin-bottom: 8px;">
        New <b>PathAlias</b> attached from WebSite: <b>${webSite.webSiteId}</b> to Content: <b>${content.contentId}</b></b>
    </div>
</#if>

<table>
  <tr><td>
    <table border="2" cellpadding="2" cellspacing="4" class="basic-table">
      <#if (aliases?has_content)>
          <tr class="header-row">
            <td>Content ID</td>
            <td>Path Alias</td>
            <td>Map Key</td>
            <td>From Date</td>
            <td>Thru Date</td>
            <td>&nbsp;</td>
          </tr>
        <#list aliases as alias>
            <tr>
              <td class="alternate-row">${alias.contentId?default("")}</td>
              <td class="alternate-row">${alias.pathAlias}</td>
              <td class="alternate-row">${alias.mapKey?default("")}</td>
              <td class="alternate-row">${alias.fromDate?default("")}</td>
              <td class="alternate-row">${alias.thruDate?default("")}</td>
              <td><a href="javascript:void(0);" onclick="javascript:pathRemove('${webSiteId}', '${alias.pathAlias}', '${alias.fromDate}', '${contentId}');" class="buttontext">Remove</a></td>
            </tr>
        </#list>
      <#else>
        <tr>
          <td colspan="5">No aliases currently defined.</td>
        </tr>
      </#if>
    </table>
  </td></tr>

  <tr><td>
    <form name="cmspathform" method="post" action="<@ofbizUrl>/createWebSitePathAliasJson</@ofbizUrl>" style="margin: 0;">
        <input type="hidden" name="webSiteId" value="${webSiteId}"/>
        <input type="hidden" name="contentId" value="${contentId}"/>
        <table>
            <tr><td colspan="2">&nbsp;</td></tr>
            <tr>
                <td class="label">Content</td>
                <td>${content.contentName?default(content.contentId)}</td>
            </tr>
            <tr><td colspan="2">&nbsp;</td></tr>
            <tr>
                <td class="label">Path Alias</td>
                <td><input type="text" name="pathAlias" value="" /></td>
            </tr>
            <tr>
                <td class="label">Map Key</td>
                <td><input type="text" name="mapKey" value="" /></td>
            </tr>
            <tr>
                <td class="label">From Date</td>
                <td><@htmlTemplate.renderDateTimeField name="fromDate" event="" action="" className="" alert="" title="" value="${Static['org.ofbiz.base.util.UtilDateTime'].nowTimestamp()}" size="20" maxlength="50" id="fromDate" dateType="timestamp" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/></td>
            </tr>
            <tr>
                <td class="label">Thru Date</td>
                <td><@htmlTemplate.renderDateTimeField name="thruDate" event="" action="" className="" alert="" title="" value="" size="20" maxlength="50" id="thruDate" dateType="timestamp" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/></td>
            </tr>
            <tr>
                <td colspan="2" align="center"><input id="submit" type="button" onclick="javascript:pathSave('${contentId}');" class="smallSubmit" value="Create"/></td>
            </tr>
        </table>
    </form>
  </td></tr>
</table>