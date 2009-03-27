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
      <td class="manage-portal-column-toolbar" style="vertical-align: top; <#if portalPageColumn.columnWidthPercentage?has_content> ${uiLabelMap.CommonWidth}:${portalPageColumn.columnWidthPercentage}%;</#if>">
        <hr/>
        <ul>
          <li id="delete-column"><form method="post" action="<@ofbizUrl>deletePortalPageColumn</@ofbizUrl>" onSubmit="javascript:submitFormDisableSubmits(this)" name="delPortalPageId_${portalPageColumn_index}"><input name="portalPageId" value="${portalPage.portalPageId}" type="hidden"/><input name="columnSeqId" value="${portalPageColumn.columnSeqId}" type="hidden"/><input name="parentPortalPageId" value="${parameters.parentPortalPageId}" type="hidden"/></form><a class="buttontext" href="javascript:document.delPortalPageId_${portalPageColumn_index}.submit()">${uiLabelMap.CommonRemove}</a></li>
          <li id="add-portlet"><form method="post" action="<@ofbizUrl>AddPortlet</@ofbizUrl>" onSubmit="javascript:submitFormDisableSubmits(this)" name="addPortlet_${portalPageColumn_index}"><input name="portalPageId" value="${portalPage.portalPageId}" type="hidden"/><input name="columnSeqId" value="${portalPageColumn.columnSeqId}" type="hidden"/><input name="parentPortalPageId" value="${parameters.parentPortalPageId}" type="hidden"/></form><a class="buttontext" href="javascript:document.addPortlet_${portalPageColumn_index}.submit()">${uiLabelMap.CommonAddAPortlet}</a></li>
          <li id="column-width">
	        <select name="setColWidth" onchange="window.location=this.value;">
	          <option value="">${uiLabelMap.CommonSetColumnWidth}</option>
	          <option <#if portalPageColumn.columnWidthPercentage?default(0) == 10> selected</#if> value="<@ofbizUrl>updatePortalPageColumn?portalPageId=${portalPage.portalPageId}&columnSeqId=${portalPageColumn.columnSeqId}&columnWidthPercentage=10&parentPortalPageId=${parameters.parentPortalPageId}</@ofbizUrl>">10%</option>
	          <option <#if portalPageColumn.columnWidthPercentage?default(0) == 20> selected</#if> value="<@ofbizUrl>updatePortalPageColumn?portalPageId=${portalPage.portalPageId}&columnSeqId=${portalPageColumn.columnSeqId}&columnWidthPercentage=20&parentPortalPageId=${parameters.parentPortalPageId}</@ofbizUrl>">20%</option>
	          <option <#if portalPageColumn.columnWidthPercentage?default(0) == 30> selected</#if> value="<@ofbizUrl>updatePortalPageColumn?portalPageId=${portalPage.portalPageId}&columnSeqId=${portalPageColumn.columnSeqId}&columnWidthPercentage=30&parentPortalPageId=${parameters.parentPortalPageId}</@ofbizUrl>">30%</option>
	          <option <#if portalPageColumn.columnWidthPercentage?default(0) == 40> selected</#if> value="<@ofbizUrl>updatePortalPageColumn?portalPageId=${portalPage.portalPageId}&columnSeqId=${portalPageColumn.columnSeqId}&columnWidthPercentage=40&parentPortalPageId=${parameters.parentPortalPageId}</@ofbizUrl>">40%</option>
	          <option <#if portalPageColumn.columnWidthPercentage?default(0) == 50> selected</#if> value="<@ofbizUrl>updatePortalPageColumn?portalPageId=${portalPage.portalPageId}&columnSeqId=${portalPageColumn.columnSeqId}&columnWidthPercentage=50&parentPortalPageId=${parameters.parentPortalPageId}</@ofbizUrl>">50%</option>
	          <option <#if portalPageColumn.columnWidthPercentage?default(0) == 60> selected</#if> value="<@ofbizUrl>updatePortalPageColumn?portalPageId=${portalPage.portalPageId}&columnSeqId=${portalPageColumn.columnSeqId}&columnWidthPercentage=60&parentPortalPageId=${parameters.parentPortalPageId}</@ofbizUrl>">60%</option>
	          <option <#if portalPageColumn.columnWidthPercentage?default(0) == 70> selected</#if> value="<@ofbizUrl>updatePortalPageColumn?portalPageId=${portalPage.portalPageId}&columnSeqId=${portalPageColumn.columnSeqId}&columnWidthPercentage=70&parentPortalPageId=${parameters.parentPortalPageId}</@ofbizUrl>">70%</option>
	          <option <#if portalPageColumn.columnWidthPercentage?default(0) == 80> selected</#if> value="<@ofbizUrl>updatePortalPageColumn?portalPageId=${portalPage.portalPageId}&columnSeqId=${portalPageColumn.columnSeqId}&columnWidthPercentage=80&parentPortalPageId=${parameters.parentPortalPageId}</@ofbizUrl>">80%</option>
	          <option <#if portalPageColumn.columnWidthPercentage?default(0) == 90> selected</#if> value="<@ofbizUrl>updatePortalPageColumn?portalPageId=${portalPage.portalPageId}&columnSeqId=${portalPageColumn.columnSeqId}&columnWidthPercentage=90&parentPortalPageId=${parameters.parentPortalPageId}</@ofbizUrl>">90%</option>
	          <option <#if portalPageColumn.columnWidthPercentage?default(0) == 100> selected</#if> value="<@ofbizUrl>updatePortalPageColumn?portalPageId=${portalPage.portalPageId}&columnSeqId=${portalPageColumn.columnSeqId}&columnWidthPercentage=100&parentPortalPageId=${parameters.parentPortalPageId}</@ofbizUrl>">100%</option>
	        </select>
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
      <td style="vertical-align: top; <#if portalPageColumn.columnWidthPercentage?has_content> ${uiLabelMap.CommonWidth}:${portalPageColumn.columnWidthPercentage}%;</#if>">
      <#assign firstInColumn = true/>
      <#list portalPagePortletViewList as portlet>
        <#if (!portlet.columnSeqId?has_content && portalPageColumn_index == 0) || (portlet.columnSeqId?if_exists == portalPageColumn.columnSeqId)>
          <#if portlet.screenName?has_content>
              <#assign portletFields = '<input name="portalPageId" value="' + portlet.portalPageId + '" type="hidden"/><input name="portalPortletId" value="' + portlet.portalPortletId + '" type="hidden"/><input name="portletSeqId" value="' + portlet.portletSeqId  + '" type="hidden"/>'>
              <div class="portlet-config">
              <div class="portlet-config-title-bar">
                <#list portalPages as portalPageList>
                  <#if portalPage.portalPageId != portalPageList.portalPageId>
                    <form method="post" action="<@ofbizUrl>movePortletToPortalPage</@ofbizUrl>" name="movePP_${portlet_index}_${portalPageList_index}">
                      ${portletFields}
                      <input name="newPortalPageId" value="${portalPageList.portalPageId}" type="hidden"/>
                    </form>
                  </#if>
                </#list>                          
                <ul>
                  <li class="title">Portlet : ${portlet.portletName?if_exists} [${portlet.portalPortletId}]</li>
                  <li class="remove"><form method="post" action="<@ofbizUrl>deletePortalPagePortlet</@ofbizUrl>" name="removePP_${portlet_index}">${portletFields}</form><a href="javascript:document.removePP_${portlet_index}.submit()">&nbsp;&nbsp;&nbsp;</a></li>

                  <#if (portlet.editFormName?has_content && portlet.editFormLocation?has_content)>
                    <li class="edit"><form method="post" action="<@ofbizUrl>ManagePortalPages</@ofbizUrl>" name="editPP_${portlet_index}">${portletFields}<input name="editAttributes" value="Y" type="hidden"/></form><a href="javascript:document.editPP_${portlet_index}.submit()">&nbsp;&nbsp;&nbsp;</a></li>
                  </#if>  

                  <#if !firstInColumn>
                    <li class="move-up"><form method="post" action="<@ofbizUrl>updatePortalPagePortletSeq</@ofbizUrl>" name="moveUpPP_${portlet_index}">${portletFields}<input name="mode" value="UP" type="hidden"/></form><a href="javascript:document.moveUpPP_${portlet_index}.submit()">&nbsp;&nbsp;&nbsp;</a></li>
                  </#if>  
                  <#if portlet_has_next>
                    <li class="move-down"><form method="post" action="<@ofbizUrl>updatePortalPagePortletSeq</@ofbizUrl>" name="moveDownPP_${portlet_index}">${portletFields}<input name="mode" value="DOWN" type="hidden"/></form><a href="javascript:document.moveDownPP_${portlet_index}.submit()">&nbsp;&nbsp;&nbsp;</a></li>
                  </#if>  
                  <#if portalPageColumn_has_next>
                    <li class="move-right"><form method="post" action="<@ofbizUrl>updatePortalPagePortlet</@ofbizUrl>" name="moveRightPP_${portlet_index}">${portletFields}<input name="columnSeqId" value="${portalPageColumnList[portalPageColumn_index+1].columnSeqId}" type="hidden"/><input name="mode" value="RIGHT" type="hidden"/></form><a href="javascript:document.moveRightPP_${portlet_index}.submit()">&nbsp;&nbsp;&nbsp;</a></li>
                  </#if>  
                  <#if (portalPageColumn_index > 0)>
                    <li class="move-left"><form method="post" action="<@ofbizUrl>updatePortalPagePortlet</@ofbizUrl>" name="moveLeftPP_${portlet_index}">${portletFields}<input name="columnSeqId" value="${portalPageColumnList[portalPageColumn_index-1].columnSeqId}" type="hidden"/><input name="mode" value="LEFT" type="hidden"/></form><a href="javascript:document.moveLeftPP_${portlet_index}.submit()">&nbsp;&nbsp;&nbsp;</a></li>
                  </#if>  
                  <#if !firstInColumn>
                    <li class="move-top"><form method="post" action="<@ofbizUrl>updatePortalPagePortletSeq</@ofbizUrl>" name="moveTopPP_${portlet_index}">${portletFields}<input name="mode" value="TOP" type="hidden"/></form><a href="javascript:document.moveTopPP_${portlet_index}.submit()">&nbsp;&nbsp;&nbsp;</a></li>
                  </#if>  
                  <#if portlet_has_next>
                    <li class="move-bottom"><form method="post" action="<@ofbizUrl>updatePortalPagePortletSeq</@ofbizUrl>" name="moveBottomPP_${portlet_index}">${portletFields}<input name="mode" value="BOTTOM" type="hidden"/></form><a href="javascript:document.moveBottomPP_${portlet_index}.submit()">&nbsp;&nbsp;&nbsp;</a></li>
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
    </#list>
  </tr>
</table>

<#else/>
<h2>No portal page data found. You may not have the necessary seed or other data for it.</h2>
</#if>