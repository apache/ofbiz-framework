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
                    <#if note.noteParty?has_content>
                      <div>&nbsp;<span class="label">${uiLabelMap.CommonBy}</span>&nbsp;${Static["org.apache.ofbiz.party.party.PartyHelper"].getPartyName(delegator, note.noteParty, true)}</div>
                    </#if>
                    <div>&nbsp;<span class="label">${uiLabelMap.CommonAt}</span>&nbsp;<#if note.noteDateTime?has_content>${Static["org.apache.ofbiz.base.util.UtilFormatOut"].formatDateTime(note.noteDateTime, "", locale, timeZone)!}</#if></div>
                  </td>
                  <td valign="top" width="50%">
                    ${note.noteInfo?replace("\n", "<br/>")}
                  </td>
                  <td align="right" valign="top" width="15%">
                    <#if "N" == note.internalNote!>
                        ${uiLabelMap.OrderPrintableNote}
                        <form name="privateNotesForm_${note_index}" method="post" action="<@ofbizUrl>updateOrderNote</@ofbizUrl>">
                          <input type="hidden" name="orderId" value="${orderId}"/>
                          <input type="hidden" name="noteId" value="${note.noteId}"/>
                          <input type="hidden" name="internalNote" value="Y"/>
                          <a href="javascript:document.privateNotesForm_${note_index}.submit()" class="buttontext">${uiLabelMap.OrderNotesPrivate}</a>
                        </form>
                    </#if>
                    <#if "Y" == note.internalNote!>
                        ${uiLabelMap.OrderNotPrintableNote}
                        <form name="publicNotesForm_${note_index}" method="post" action="<@ofbizUrl>updateOrderNote</@ofbizUrl>">
                          <input type="hidden" name="orderId" value="${orderId}"/>
                          <input type="hidden" name="noteId" value="${note.noteId}"/>
                          <input type="hidden" name="internalNote" value="N"/>
                          <a href="javascript:document.publicNotesForm_${note_index}.submit()" class="buttontext">${uiLabelMap.OrderNotesPublic}</a>
                        </form>
                    </#if>
                  </td>
                </tr>
                <#if note_has_next>
                  <tr><td colspan="3"><hr/></td></tr>
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
