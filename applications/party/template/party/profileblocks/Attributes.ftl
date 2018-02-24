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

  <div id="partyAttributes" class="screenlet">
    <div class="screenlet-title-bar">
      <ul>
        <li class="h3">${uiLabelMap.PartyAttributes}</li>
        <#if security.hasEntityPermission("PARTYMGR", "_CREATE", session)>
          <li><a href="<@ofbizUrl>editPartyAttribute?partyId=${party.partyId!}&amp;CANCEL_PAGE=${DONE_PAGE!}</@ofbizUrl>">${uiLabelMap.CommonCreateNew}</a></li>
        </#if>
      </ul>
      <br class="clear"/>
    </div>
    <div class="screenlet-body">
      <#if attributes?has_content>
        <table class="basic-table hover-bar" cellspacing="0">
            <tr class="header-row">
              <td>${uiLabelMap.CommonName}</td>
              <td>${uiLabelMap.CommonValue}</td>
              <td>&nbsp;</td>
            </tr>
          <#assign alt_row = false>
          <#list attributes as attr>
            <tr valign="middle"<#if alt_row> class="alternate-row"</#if>>
              <td>
                ${attr.attrName!}
              </td>
              <td>
                ${attr.attrValue!}
              </td>
              <td class="button-col">
                <a href="<@ofbizUrl>editPartyAttribute?partyId=${partyId!}&attrName=${attr.attrName!}</@ofbizUrl>">${uiLabelMap.CommonEdit}</a>
              </td>
            </tr>
            <#-- toggle the row color -->
            <#assign alt_row = !alt_row>
          </#list>
        </table>
      <#else>
        ${uiLabelMap.PartyNoPartyAttributesFound}
      </#if>
    </div>
  </div>