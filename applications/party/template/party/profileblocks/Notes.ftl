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

  <div id="partyNotes" class="screenlet">
    <div class="screenlet-title-bar">
      <ul>
        <li class="h3">${uiLabelMap.CommonNotes}</li>
        <#if security.hasEntityPermission("PARTYMGR", "_NOTE", session)>
          <li><a href="<@ofbizUrl>AddPartyNote?partyId=${partyId}</@ofbizUrl>">${uiLabelMap.CommonCreateNew}</a></li>
        </#if>
      </ul>
      <br class="clear" />
    </div>
    <div class="screenlet-body">
      <#if notes?has_content>
        <table width="100%" border="0" cellpadding="1">
          <#list notes as noteRef>
            <tr>
              <td>
                <div><b>${uiLabelMap.FormFieldTitle_noteName}: </b>${noteRef.noteName!}</div>
                <#if noteRef.noteParty?has_content>
                  <div><b>${uiLabelMap.CommonBy}: </b>${Static["org.apache.ofbiz.party.party.PartyHelper"].getPartyName(delegator, noteRef.noteParty, true)}</div>
                </#if>
                <div><b>${uiLabelMap.CommonAt}: </b>${noteRef.noteDateTime.toString()}</div>
              </td>
              <td>
                ${noteRef.noteInfo}
              </td>
            </tr>
            <#if noteRef_has_next>
              <tr><td colspan="2"><hr/></td></tr>
            </#if>
          </#list>
        </table>
      <#else>
        ${uiLabelMap.PartyNoNotesForParty}
      </#if>
    </div>
  </div>
