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

<#if currentPortalPage?has_content>
    <#if configurePortalPage?has_content>
		<div id="manage-portal-toolbar">
  			<ul>
      			<li id="add-column">
        		<a href="<@ofbizUrl>addPortalPageColumn?portalPageId=${currentPortalPage.portalPageId}&configurePortalPage=true&originalPortalPageId=${parameters.originalPortalPageId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonAddColumn}</a>
      			</li>
  			</ul>
  			<br class="clear"/>
		</div>
    </#if>
<table width="100%">
  <#if configurePortalPage?has_content>
    <tr> 
      <#list portalPageColumnList?if_exists as portalPageColumn>
        <td class="manage-portal-column-toolbar" style="vertical-align: top; <#if portalPageColumn.columnWidthPercentage?has_content> width:${portalPageColumn.columnWidthPercentage}%;</#if>">
          <ul>
            <li id="delete-column">
              <a href="<@ofbizUrl>deletePortalPageColumn?portalPageId=${currentPortalPage.portalPageId}&columnSeqId=${portalPageColumn.columnSeqId}&configurePortalPage=true&originalPortalPageId=${parameters.originalPortalPageId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonDelete}</a>
            </li>
            <li id="add-portlet">
              <a href="<@ofbizUrl>AddPortlet?portalPageId=${currentPortalPage.portalPageId}&columnSeqId=${portalPageColumn.columnSeqId}&originalPortalPageId=${parameters.originalPortalPageId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonAddAPortlet}</a>
            </li>
            <li id="column-width">
              <select name="setColWidth" onchange="window.location=this.value;">
                <option value="">${uiLabelMap.CommonSetColumnWidth}</option>
                <option <#if portalPageColumn.columnWidthPercentage?default(0) == 10> selected</#if> value="<@ofbizUrl>updatePortalPageColumn?portalPageId=${currentPortalPage.portalPageId}&columnSeqId=${portalPageColumn.columnSeqId}&columnWidthPercentage=10&configurePortalPage=true&originalPortalPageId=${parameters.originalPortalPageId}</@ofbizUrl>">10%</option>
                <option <#if portalPageColumn.columnWidthPercentage?default(0) == 20> selected</#if> value="<@ofbizUrl>updatePortalPageColumn?portalPageId=${currentPortalPage.portalPageId}&columnSeqId=${portalPageColumn.columnSeqId}&columnWidthPercentage=20&configurePortalPage=true&originalPortalPageId=${parameters.originalPortalPageId}</@ofbizUrl>">20%</option>
                <option <#if portalPageColumn.columnWidthPercentage?default(0) == 30> selected</#if> value="<@ofbizUrl>updatePortalPageColumn?portalPageId=${currentPortalPage.portalPageId}&columnSeqId=${portalPageColumn.columnSeqId}&columnWidthPercentage=30&configurePortalPage=true&originalPortalPageId=${parameters.originalPortalPageId}</@ofbizUrl>">30%</option>
                <option <#if portalPageColumn.columnWidthPercentage?default(0) == 40> selected</#if> value="<@ofbizUrl>updatePortalPageColumn?portalPageId=${currentPortalPage.portalPageId}&columnSeqId=${portalPageColumn.columnSeqId}&columnWidthPercentage=40&configurePortalPage=true&originalPortalPageId=${parameters.originalPortalPageId}</@ofbizUrl>">40%</option>
                <option <#if portalPageColumn.columnWidthPercentage?default(0) == 50> selected</#if> value="<@ofbizUrl>updatePortalPageColumn?portalPageId=${currentPortalPage.portalPageId}&columnSeqId=${portalPageColumn.columnSeqId}&columnWidthPercentage=50&configurePortalPage=true&originalPortalPageId=${parameters.originalPortalPageId}</@ofbizUrl>">50%</option>
                <option <#if portalPageColumn.columnWidthPercentage?default(0) == 60> selected</#if> value="<@ofbizUrl>updatePortalPageColumn?portalPageId=${currentPortalPage.portalPageId}&columnSeqId=${portalPageColumn.columnSeqId}&columnWidthPercentage=60&configurePortalPage=true&originalPortalPageId=${parameters.originalPortalPageId}</@ofbizUrl>">60%</option>
                <option <#if portalPageColumn.columnWidthPercentage?default(0) == 70> selected</#if> value="<@ofbizUrl>updatePortalPageColumn?portalPageId=${currentPortalPage.portalPageId}&columnSeqId=${portalPageColumn.columnSeqId}&columnWidthPercentage=70&configurePortalPage=true&originalPortalPageId=${parameters.originalPortalPageId}</@ofbizUrl>">70%</option>
                <option <#if portalPageColumn.columnWidthPercentage?default(0) == 80> selected</#if> value="<@ofbizUrl>updatePortalPageColumn?portalPageId=${currentPortalPage.portalPageId}&columnSeqId=${portalPageColumn.columnSeqId}&columnWidthPercentage=80&configurePortalPage=true&originalPortalPageId=${parameters.originalPortalPageId}</@ofbizUrl>">80%</option>
                <option <#if portalPageColumn.columnWidthPercentage?default(0) == 90> selected</#if> value="<@ofbizUrl>updatePortalPageColumn?portalPageId=${currentPortalPage.portalPageId}&columnSeqId=${portalPageColumn.columnSeqId}&columnWidthPercentage=90&configurePortalPage=true&originalPortalPageId=${parameters.originalPortalPageId}</@ofbizUrl>">90%</option>
                <option <#if portalPageColumn.columnWidthPercentage?default(0) == 100> selected</#if> value="<@ofbizUrl>updatePortalPageColumn?portalPageId=${currentPortalPage.portalPageId}&columnSeqId=${portalPageColumn.columnSeqId}&columnWidthPercentage=100&configurePortalPage=true&originalPortalPageId=${parameters.originalPortalPageId}</@ofbizUrl>">100%</option>
              </select>
            </li>
          </ul>
        </td>
        <#if portalPageColumn_has_next>
          <td>&nbsp;</td>
        </#if>
      </#list>
    </tr>
  </#if>
  <tr>
    <#list portalPageColumnList?if_exists as portalPageColumn>
      <td style="vertical-align: top; <#if portalPageColumn.columnWidthPercentage?has_content> width:${portalPageColumn.columnWidthPercentage}%;</#if>">
      <#assign firstInColumn = true/>
      <#list portalPagePortletViewList as portlet>
        <#if (!portlet.columnSeqId?has_content && portalPageColumn_index == 0) || (portlet.columnSeqId?if_exists == portalPageColumn.columnSeqId)>
          <#if portlet.screenName?has_content>
            <#if configurePortalPage?has_content>
              <#assign portletUrlLink = "portalPageId="+currentPortalPage.portalPageId+"&amp;portalPortletId="+portlet.portalPortletId+"&amp;portletSeqId="+portlet.portletSeqId+"&amp;configurePortalPage=true&amp;originalPortalPageId="+parameters.originalPortalPageId/>
      
              <div class="portlet-config">
              <div class="portlet-config-title-bar">
                <ul>
                  <li class="title">Portlet : ${portlet.portletName}</li>
                  <li class="remove"><a href="<@ofbizUrl>deletePortalPagePortlet?${portletUrlLink}</@ofbizUrl>" title="${uiLabelMap.CommonRemovePortlet}">&nbsp;&nbsp;&nbsp;</a></li>

                  <#if (portlet.editFormName?has_content && portlet.editFormLocation?has_content)>
                    <li class="edit"><a href="<@ofbizUrl>EditPortlet?${portletUrlLink}</@ofbizUrl>" title="edit">&nbsp;&nbsp;&nbsp;</a></li>
                  </#if>  

                  <#if portlet_has_next> <#-- TODO: this doesn't take into account that later items in the list might be in a different column -->
                    <li class="move-down"><a href="<@ofbizUrl>updatePortalPagePortlet?${portletUrlLink}&amp;sequenceNum=${portlet.sequenceNum?default(0) + 1}</@ofbizUrl>" title="${uiLabelMap.CommonMovePortletDown}">&nbsp;&nbsp;&nbsp;</a></li>
                  </#if>  
                  <#if !firstInColumn>
                    <li class="move-up"><a href="<@ofbizUrl>updatePortalPagePortlet?${portletUrlLink}&amp;sequenceNum=${portlet.sequenceNum?default(1)-1}</@ofbizUrl>" title="${uiLabelMap.CommonMovePortletUp}">&nbsp;&nbsp;&nbsp;</a></li>
                  </#if>  
                  <#if portalPageColumn_has_next>
                    <li class="move-right"><a href="<@ofbizUrl>updatePortalPagePortlet?${portletUrlLink}&amp;columnSeqId=${portalPageColumnList[portalPageColumn_index+1].columnSeqId}</@ofbizUrl>" title="${uiLabelMap.CommonMovePortletRight}">&nbsp;&nbsp;&nbsp;</a></li>
                  </#if>  
                  <#if (portalPageColumn_index > 0)>
                    <li class="move-left"><a href="<@ofbizUrl>updatePortalPagePortlet?${portletUrlLink}&amp;columnSeqId=${portalPageColumnList[portalPageColumn_index-1].columnSeqId}</@ofbizUrl>" title="${uiLabelMap.CommonMovePortletLeft}">&nbsp;&nbsp;&nbsp;</a></li>
                  </#if>  
                  <#if !firstInColumn>
                    <li class="move-top"><a href="<@ofbizUrl>updatePortalPagePortlet?${portletUrlLink}&amp;sequenceNum=0</@ofbizUrl>" title="${uiLabelMap.CommonMovePortletTop}">&nbsp;&nbsp;&nbsp;</a></li>
                  </#if>  
                  <#if portlet_has_next> <#-- TODO: this doesn't take into account that later items in the list might be in a different column -->
                    <li class="move-bottom"><a href="<@ofbizUrl>updatePortalPagePortlet?${portletUrlLink}&amp;sequenceNum=${portalPagePortletViewList.size()}</@ofbizUrl>" title="${uiLabelMap.CommonMovePortletBottom}">&nbsp;&nbsp;&nbsp;</a></li>
                  </#if>  
                  <#if (portalPages.size() > 1)>
                    <li>
                    <select name="moveToPortal" onchange="window.location=this[this.selectedIndex].value;">
                      <option value="">${uiLabelMap.CommonMoveToPortalPage}</option>
  
                      <#list portalPages as portalPage>
                        <#if (currentPortalPage.portalName != portalPage.portalName)> 
                          <option value="<@ofbizUrl>movePortletToPortalPage?${portletUrlLink}&amp;newPortalPageId=${portalPage.portalPageId}</@ofbizUrl>">${portalPage.portalName}</option>
                        </#if>
                      </#list>                          
                    </select>
                    </li>
                  </#if>
                </ul>
                <br class="clear"/>
              </div> 
              <div class="screenlet-body">
            </#if>
        
            <#assign screenFileName = portlet.screenLocation + "#" + portlet.screenName/>
            <div>
            ${setRequestAttribute("portalPageId", currentPortalPage.portalPageId)}
            ${setRequestAttribute("portalPortletId", portlet.portalPortletId)}
            ${setRequestAttribute("portletSeqId", portlet.portletSeqId)}
            
            ${screens.render(screenFileName)}
            </div>
          
            <#if configurePortalPage?has_content>
                </div>
              </div>
            </#if>
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