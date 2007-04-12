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
        <a href="javascript:void(0);" onclick="javascript:callEditor(true, '${content.contentId}', '', 'ELECTRONIC_TEXT');" class="tabButton">Quick Sub-Content</a>
        <a href="javascript:void(0);" onclick="javascript:callPathAlias('${content.contentId}');" class="tabButtonSelected">Path Alias</a>
        <a href="javascript:void(0);" onclick="javascript:callMetaInfo('${content.contentId}');" class="tabButton">Meta Tags</a>
    </#if>
</div>

<#if (content?has_content)>
    <div class="tabletext" style="margin-bottom: 8px;">
        New <b>PathAlias</b> attached from WebSite: <b>${webSite.webSiteId}</b> to Content: <b>${content.contentId}</b></b>
    </div>
</#if>

<table>
  <tr><td>
    <table border="1" cellpadding="2" cellspacing="0" class="calendarTable">
      <tr>
        <td><div class="tableheadtext">Web Site ID</div></td>
        <td><div class="tableheadtext">Path Alias</div></td>
        <td><div class="tableheadtext">Alias To</div></td>
        <td><div class="tableheadtext">Content ID</div></td>
        <td><div class="tableheadtext">Map Key</div></td>
        <td>&nbsp;</td>
      </tr>
      <#if (aliases?has_content)>
        <#list aliases as alias>
            <tr>
              <td><div class="tabletext">${alias.webSiteId}</div></td>
              <td><div class="tabletext">${alias.pathAlias}</div></td>
              <td><div class="tabletext">${alias.aliasTo?default("N/A")}</div></td>
              <td><div class="tabletext">${alias.contentId?default("N/A")}</div></td>
              <td><div class="tabletext">${alias.mapKey?default("N/A")}</div></td>
              <td><a href="javascript:void(0);" onclick="javascript:pathRemove('${webSiteId}', '${alias.pathAlias}', '${contentId}');" class="buttontext">Remove</a></td>
            </tr>
        </#list>
      <#else>
        <tr>
          <td colspan="5"><div class="tabletext">No aliases currently defined.</div></td>
        </tr>
      </#if>
    </table>
  </td></tr>

  <tr><td>
    <form name="cmspathform" method="post" action="<@ofbizUrl>/createWebSitePathAliasJson</@ofbizUrl>" style="margin: 0;">
        <table>
            <tr><td colspan="2">&nbsp;</td></tr>
            <tr>
                <td><div class="tableheadtext">Web Site</div></td>
                <td><div class="tabletext">${webSite.siteName?default(webSite.webSiteId)}</div></td>
                <input type="hidden" name="webSiteId" value="${webSiteId}"/>
            </tr>
            <tr>
                <td><div class="tableheadtext">Content</div></td>
                <td><div class="tabletext">${content.contentName?default(content.contentId)}</div></td>
                <input type="hidden" name="contentId" value="${contentId}"/>
            </tr>
            <tr><td colspan="2">&nbsp;</td></tr>
            <tr>
                <td><div class="tableheadtext">Path Alias</div></td>
                <td><input type="text" class="inputBox" name="pathAlias" value=""></td>
            </tr>
            <tr>
                <td><div class="tableheadtext">Map Key</div></td>
                <td><input type="text" class="inputBox" name="mapKey" value=""></td>
            </tr>
            <tr>
                <td colspan="2" align="center"><input id="submit" type="button" onclick="javascript:pathSave('${contentId}');" class="smallSubmit" value="Create"/></td>
            </tr>
        </table>
    </form>
  </td></tr>
</table>