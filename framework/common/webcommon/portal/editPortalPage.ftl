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
    <#list portalPageColumnList?if_exists as portalPageColumn>
      <td class="manage-portal-column-toolbar" style="vertical-align: top; <#if portalPageColumn.columnWidthPercentage?has_content> width:${portalPageColumn.columnWidthPercentage}%;</#if>">
        <hr />
        <ul>
          <li id="delete-column"><form method="post" action="<@ofbizUrl>deletePortalPageColumn${Adm?if_exists}</@ofbizUrl>" onsubmit="javascript:submitFormDisableSubmits(this)" name="delPortalPageId_${portalPageColumn_index}"><input name="portalPageId" value="${portalPage.portalPageId}" type="hidden"/><input name="columnSeqId" value="${portalPageColumn.columnSeqId}" type="hidden"/><input name="parentPortalPageId" value="${parameters.parentPortalPageId}" type="hidden"/></form><a class="buttontext" href="javascript:document.delPortalPageId_${portalPageColumn_index}.submit()">${uiLabelMap.CommonRemove}</a></li>
          <li id="add-portlet"><form method="post" action="<@ofbizUrl>AddPortlet${Adm?if_exists}</@ofbizUrl>" onsubmit="javascript:submitFormDisableSubmits(this)" name="addPortlet_${portalPageColumn_index}"><input name="portalPageId" value="${portalPage.portalPageId}" type="hidden"/><input name="columnSeqId" value="${portalPageColumn.columnSeqId}" type="hidden"/><input name="parentPortalPageId" value="${parameters.parentPortalPageId}" type="hidden"/></form><a class="buttontext" href="javascript:document.addPortlet_${portalPageColumn_index}.submit()">${uiLabelMap.CommonAddAPortlet}</a></li>
          <li id="column-width">
           <form method="post" action="<@ofbizUrl>updatePortalPageColumn${Adm?if_exists}</@ofbizUrl>" onsubmit="javascript:submitFormDisableSubmits(this)" name="updatePortalPageColum_${portalPageColumn_index}">
            <input name="portalPageId" value="${portalPage.portalPageId}" type="hidden"/>
            <input name="columnSeqId" value="${portalPageColumn.columnSeqId}" type="hidden"/>
            <input name="parentPortalPageId" value="${parameters.parentPortalPageId?if_exists}" type="hidden"/>
            <select name="columnWidthPercentage" onchange="javascript:document.updatePortalPageColum_${portalPageColumn_index}.submit()">
              <option value="">${uiLabelMap.CommonSetColumnWidth}</option>
              <option <#if portalPageColumn.columnWidthPercentage?default(0) == 10> selected="selected"</#if> value="25">25%</option>
              <option <#if portalPageColumn.columnWidthPercentage?default(0) == 20> selected="selected"</#if> value="50">50%</option>
              <option <#if portalPageColumn.columnWidthPercentage?default(0) == 30> selected="selected"</#if> value="75">75%</option>
            </select>
           </form>
          </li>
        </ul>
      </td>
      <#if portalPageColumn_has_next>
        <td>&nbsp;</td>
      </#if>
    </#list>
  </tr>
  <tr>
    <#list portalPageColumnList?if_exists as portalPageColumn>
      <td style="vertical-align: top; <#if portalPageColumn.columnWidthPercentage?has_content> width:${portalPageColumn.columnWidthPercentage}%;</#if>" id="portalColumn_${portalPageColumn.columnSeqId}">
      <#assign firstInColumn = true/>
      <#list portalPagePortletViewList as portlet>
        <#if (!portlet.columnSeqId?has_content && portalPageColumn_index == 0) || (portlet.columnSeqId?if_exists == portalPageColumn.columnSeqId)>
          <#if portlet.screenName?has_content>
              <#assign portletFields = '<input name="portalPageId" value="' + portlet.portalPageId + '" type="hidden"/><input name="portalPortletId" value="' + portlet.portalPortletId + '" type="hidden"/><input name="portletSeqId" value="' + portlet.portletSeqId  + '" type="hidden"/>'>
              <div class="portlet-config" id="portalPortlet_${portlet_index}" onmouseover="javascript:this.style.cursor='move';">
              <div class="portlet-config-title-bar">
                  <script type="text/javascript">makeDragable("${portlet_index}");</script>
                  <script type="text/javascript">makeDroppable("${portlet_index}");</script>
                  <form method="post" action="<@ofbizUrl>updatePortalPagePortletAjax</@ofbizUrl>" name="freeMove_${portlet_index}">${portletFields}<input name="columnSeqId" value="${portalPageColumnList[portalPageColumn_index].columnSeqId}" type="hidden"/><input name="mode" value="RIGHT" type="hidden"/></form>
                <#list portalPages as portalPageList>
                  <#if portalPage.portalPageId != portalPageList.portalPageId>
                    <form method="post" action="<@ofbizUrl>movePortletToPortalPage${Adm?if_exists}</@ofbizUrl>" name="movePP_${portlet_index}_${portalPageList_index}">
                      ${portletFields}
                      <input name="newPortalPageId" value="${portalPageList.portalPageId}" type="hidden"/>
                    </form>
                  </#if>
                </#list>
                <ul>
                  <li class="title">Portlet : ${portlet.portletName?if_exists} [${portlet.portalPortletId}]</li>
                  <li class="remove"><form method="post" action="<@ofbizUrl>deletePortalPagePortlet${Adm?if_exists}</@ofbizUrl>" name="removePP_${portlet_index}">${portletFields}</form><a href="javascript:document.removePP_${portlet_index}.submit()">&nbsp;&nbsp;&nbsp;</a></li>

                  <#if (portlet.editFormName?has_content && portlet.editFormLocation?has_content)>
                    <li class="edit"><form method="post" action="<@ofbizUrl>ManagePortalPages${Adm?if_exists}</@ofbizUrl>" name="editPP_${portlet_index}">${portletFields}<input name="editAttributes" value="Y" type="hidden"/></form><a href="javascript:document.editPP_${portlet_index}.submit()">&nbsp;&nbsp;&nbsp;</a></li>
                  </#if>

                  <#if !firstInColumn>
                    <li class="move-up"><form method="post" action="<@ofbizUrl>updatePortalPagePortletSeq${Adm?if_exists}</@ofbizUrl>" name="moveUpPP_${portlet_index}">${portletFields}<input name="mode" value="UP" type="hidden"/></form><a href="javascript:document.moveUpPP_${portlet_index}.submit()">&nbsp;&nbsp;&nbsp;</a></li>
                  </#if>
                  <#if portlet_has_next>
                    <li class="move-down"><form method="post" action="<@ofbizUrl>updatePortalPagePortletSeq${Adm?if_exists}</@ofbizUrl>" name="moveDownPP_${portlet_index}">${portletFields}<input name="mode" value="DOWN" type="hidden"/></form><a href="javascript:document.moveDownPP_${portlet_index}.submit()">&nbsp;&nbsp;&nbsp;</a></li>
                  </#if>
                  <#if portalPageColumn_has_next>
                    <li class="move-right"><form method="post" action="<@ofbizUrl>updatePortalPagePortlet${Adm?if_exists}</@ofbizUrl>" name="moveRightPP_${portlet_index}">${portletFields}<input name="columnSeqId" value="${portalPageColumnList[portalPageColumn_index+1].columnSeqId}" type="hidden"/><input name="mode" value="RIGHT" type="hidden"/></form><a href="javascript:document.moveRightPP_${portlet_index}.submit()">&nbsp;&nbsp;&nbsp;</a></li>
                  </#if>
                  <#if (portalPageColumn_index > 0)>
                    <li class="move-left"><form method="post" action="<@ofbizUrl>updatePortalPagePortlet${Adm?if_exists}</@ofbizUrl>" name="moveLeftPP_${portlet_index}">${portletFields}<input name="columnSeqId" value="${portalPageColumnList[portalPageColumn_index-1].columnSeqId}" type="hidden"/><input name="mode" value="LEFT" type="hidden"/></form><a href="javascript:document.moveLeftPP_${portlet_index}.submit()">&nbsp;&nbsp;&nbsp;</a></li>
                  </#if>
                  <#if !firstInColumn>
                    <li class="move-top"><form method="post" action="<@ofbizUrl>updatePortalPagePortletSeq${Adm?if_exists}</@ofbizUrl>" name="moveTopPP_${portlet_index}">${portletFields}<input name="mode" value="TOP" type="hidden"/></form><a href="javascript:document.moveTopPP_${portlet_index}.submit()">&nbsp;&nbsp;&nbsp;</a></li>
                  </#if>
                  <#if portlet_has_next>
                    <li class="move-bottom"><form method="post" action="<@ofbizUrl>updatePortalPagePortletSeq${Adm?if_exists}</@ofbizUrl>" name="moveBottomPP_${portlet_index}">${portletFields}<input name="mode" value="BOTTOM" type="hidden"/></form><a href="javascript:document.moveBottomPP_${portlet_index}.submit()">&nbsp;&nbsp;&nbsp;</a></li>
                  </#if>
                  <#if (portalPages.size() > 1)>
                    <li>
                    <select name="moveToPortal" onchange="javascript:(document.forms['movePP_${portlet_index}_' + this.selectedIndex.toString()].newPortalPageId = this[this.selectedIndex].value),(document.forms['movePP_${portlet_index}_' + this.selectedIndex.toString()].submit())">
                      <option value="">${uiLabelMap.CommonMoveToPortalPage}</option>
                      <#list portalPages as portalPageList>
                          <#if portalPage.portalPageId != portalPageList.portalPageId>
                            <option value="${portalPageList.portalPageId}">${portalPageList.portalPageName?if_exists}</option>
                          </#if>
                      </#list>
                    </select>
                    </li>
                  </#if>
                </ul>
                <br class="clear"/>
              </div>
              <div class="screenlet-body">
            <div>
            ${setRequestAttribute("portalPageId", portalPage.portalPageId)}
            ${setRequestAttribute("portalPortletId", portlet.portalPortletId)}
            ${setRequestAttribute("portletSeqId", portlet.portletSeqId)}
            ${screens.render(portlet.screenLocation, portlet.screenName)}
            </div>

                </div>
              </div>
          </#if>
          <#assign firstInColumn = false/>
        </#if>
      </#list>
      <#if portalPageColumn_has_next>
        <td>&nbsp;</td>
      </#if>
      </td>
    </#list>
  </tr>
</table>

<#else/>
<h2>${uiLabelMap.CommonNoPortalPageDataFound}</h2>
</#if>