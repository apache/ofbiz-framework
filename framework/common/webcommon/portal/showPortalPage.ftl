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
      <td style="vertical-align: top; <#if portalPageColumn.columnWidthPercentage?has_content> ${uiLabelMap.CommonWidth}:${portalPageColumn.columnWidthPercentage}%;</#if>">
      <#assign firstInColumn = true/>
      <#list portalPagePortlets as portlet>
        <#if (!portlet.columnSeqId?has_content && portalPageColumn_index == 0) || (portlet.columnSeqId?if_exists == portalPageColumn.columnSeqId)>
          <#if portlet.screenName?has_content>
            <div>
            ${setRequestAttribute("portalPageId", portalPage.portalPageId)}
            ${setRequestAttribute("portalPortletId", portlet.portalPortletId)}
            ${setRequestAttribute("portletSeqId", portlet.portletSeqId)}
            ${screens.render(portlet.screenLocation, portlet.screenName)}
            </div>
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