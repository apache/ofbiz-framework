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

<#if orderHeader?has_content>

<div class="screenlet">
    <div class="screenlet-title-bar">
      <ul>
        <li class="h3">&nbsp;${uiLabelMap.OrderNotes}</li>
        <#if security.hasEntityPermission("ORDERMGR", "_NOTE", session)>
          <li><a href="<@ofbizUrl>createnewnote?${paramString}</@ofbizUrl>">${uiLabelMap.OrderNotesCreateNew}</a></li>
        </#if>
      </ul>
      <br class="clear"/>
    </div>
    <div class="screenlet-body">
      <table class="basic-table" cellspacing='0'>
        <tr>
          <td>
            <#if orderNotes?has_content>
            <table class="basic-table" cellspacing='0'>
              <#list orderNotes as note>
                <tr>
                  <td valign="top" width="35%">
                    <div>&nbsp;<span class="label">${uiLabelMap.CommonBy}</span>&nbsp;${Static["org.ofbiz.party.party.PartyHelper"].getPartyName(delegator, note.noteParty, true)}</div>
                    <div>&nbsp;<span class="label">${uiLabelMap.CommonAt}</span>&nbsp;${note.noteDateTime?string?if_exists}</div>
                  </td>
                  <td valign="top" width="50%">
                    ${note.noteInfo?if_exists}
                  </td>
                  <td align="right" valign="top" width="15%">
                    <#if note.internalNote?if_exists == "N">
                        ${uiLabelMap.OrderPrintableNote}
                        <a href="<@ofbizUrl>updateOrderNote?orderId=${orderId}&noteId=${note.noteId}&internalNote=Y</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderNotesPrivate}</a>
                    </#if>
                    <#if note.internalNote?if_exists == "Y">
                        ${uiLabelMap.OrderNotPrintableNote}
                        <a href="<@ofbizUrl>updateOrderNote?orderId=${orderId}&noteId=${note.noteId}&internalNote=N</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderNotesPublic}</a>
                    </#if>
                  </td>
                </tr>
                <#if note_has_next>
                  <tr><td colspan="3"><hr></td></tr>
                </#if>
              </#list>
            </table>
            <#else>
              <span class="label">&nbsp;${uiLabelMap.OrderNoNotes}.</span>
            </#if>
          </td>
        </tr>
      </table>
    </div>
</div>
</#if>