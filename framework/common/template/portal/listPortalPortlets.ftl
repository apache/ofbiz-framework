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

<div class="screenlet">
  <div class="screenlet-title-bar">
    <ul>
      <li class="h3">${uiLabelMap.CommonAvailablePortlets}</li>
      <li><a href="<@ofbizUrl>ManagePortalPages?portalPageId=${parameters.portalPageId}&amp;parentPortalPageId=${parameters.parentPortalPageId}</@ofbizUrl>">${uiLabelMap.CommonCancel}</a></li>
    </ul>
    <br class="clear"/>
  </div>
  <div class="screenlet-body">
  <#if portalPortlets?has_content>
    <#assign orderByList = Static["org.ofbiz.base.util.UtilMisc"].toList("portalPortletId")/>

    <table cellspacing="20" class="basic-table">
      <#assign leftColumn = true/>
      <#list portalPortlets as portalPortlet>
        <#if leftColumn==true>
        <tr>
        </#if>
          <td>
            <div class="h3">
              <form method="post" action="<@ofbizUrl>createPortalPagePortlet</@ofbizUrl>" onsubmit="javascript:submitFormDisableSubmits(this)" name="createPortalPortlet_${portalPortlet.portalPortletId}"><input name="portalPortletId" value="${portalPortlet.portalPortletId}" type="hidden"/><input name="portalPageId" value="${parameters.portalPageId}" type="hidden"/><input name="columnSeqId" value="${parameters.columnSeqId}" type="hidden"/></form><a class="buttontext" href="javascript:document.createPortalPortlet_${portalPortlet.portalPortletId}.submit()">${uiLabelMap.CommonAdd}: ${portalPortlet.portletName}</a>
            </div>
            <div>
              ${portalPortlet.description!}
            </div>
            <div>
              ${parameters.portletCategoryId}
            </div>
          </td>
          <td>
            <#if portalPortlet.screenshot?has_content>
              <div class="screenshot">
                <a href="<@ofbizContentUrl>${portalPortlet.screenshot}</@ofbizContentUrl>"><img src="<@ofbizContentUrl>${portalPortlet.screenshot}</@ofbizContentUrl>" width="250" alt=""/></a>
              </div>
            </#if>
          </td>
        <#if leftColumn==true>
          <td width="10%">
          </td>
        </#if>
        <#if leftColumn==false>
        </tr>
        </#if>
        <#assign leftColumn = !leftColumn/>
      </#list>
    </table>
    <#else/>
    <h2>${uiLabelMap.CommonNoPortletsInCategory}</h2>
  </#if>
  </div>
</div>
