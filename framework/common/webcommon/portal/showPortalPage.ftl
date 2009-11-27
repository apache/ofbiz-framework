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
<#if portalPage?has_content>
<table width="100%">
  <tr>
    <#list portalPageColumns?if_exists as portalPageColumn>
      <td style="vertical-align: top; <#if portalPageColumn.columnWidthPercentage?has_content> width:${portalPageColumn.columnWidthPercentage}%;</#if>" id="${portalPageColumn.columnSeqId}" name="portalColumn">
      <#assign firstInColumn = true/>
      <#list portalPagePortlets as portlet>
        <#if (!portlet.columnSeqId?has_content && portalPageColumn_index == 0) || (portlet.columnSeqId?if_exists == portalPageColumn.columnSeqId)>
          <#if portlet.screenName?has_content>
            <#assign portletFields = '<input name="portalPageId" value="' + portlet.portalPageId + '" type="hidden"/><input name="portalPortletId" value="' + portlet.portalPortletId + '" type="hidden"/><input name="portletSeqId" value="' + portlet.portletSeqId  + '" type="hidden"/>'>
            <form method="post" action="<@ofbizUrl>movePortletToPortalPage</@ofbizUrl>" name="movePP_${portlet_index}">${portletFields}<input name="newPortalPageId" value="${portlet.portalPageId}" type="hidden"/></form>
            <div id="${portlet_index}" name="portalPortlet" class="noClass">
            ${setRequestAttribute("portalPageId", portalPage.portalPageId)}
            ${setRequestAttribute("portalPortletId", portlet.portalPortletId)}
            ${setRequestAttribute("portletSeqId", portlet.portletSeqId)}
            ${screens.render(portlet.screenLocation, portlet.screenName)}
            ${screens.setRenderFormUniqueSeq(portlet_index)}
            </div>
            <#-- DragNDrop is only activated, when the portal Page isn't the Default page -->
            <#if portalPage.originalPortalPageId?has_content><script>setMousePointer("${portlet_index}")</script></#if>
            <#if portalPage.originalPortalPageId?has_content><script type="text/javascript">makeDragable("${portlet_index}");</script></#if>
            <#if portalPage.originalPortalPageId?has_content><script type="text/javascript">makeDroppable("${portlet_index}");</script></#if>
            <form method="post" action="<@ofbizUrl>updatePortalPagePortletAjax</@ofbizUrl>" name="freeMove_${portlet_index}">${portletFields}<input name="columnSeqId" value="${portalPageColumn.columnSeqId}" type="hidden"/><input name="mode" value="RIGHT" type="hidden"/></form>
          </#if>
          <#assign firstInColumn = false/>
        </#if>
      </#list>
      </td>
      <#if portalPageColumn_has_next>
        <td>&nbsp;</td>
      </#if>
    </#list>
  </tr>
</table>
<#else/>
<h2>No portal page data found. You may not have the necessary seed or other data for it.</h2>
</#if>
