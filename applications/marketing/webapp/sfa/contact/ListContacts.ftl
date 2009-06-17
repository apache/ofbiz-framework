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
      <#if (partyListSize > 0)>
        <#if (partyListSize > highIndex)>
          <li><a class="nav-next button-col" href="<@ofbizUrl>FindContacts?VIEW_SIZE=${viewSize}&amp;VIEW_INDEX=${viewIndex+1}&amp;hideFields=Y${paramList}</@ofbizUrl>">${uiLabelMap.CommonNext}</a></li>
        <#else>
          <li class="disabled button-col">${uiLabelMap.CommonNext}</li>
        </#if>
        <li>${lowIndex} - ${highIndex} ${uiLabelMap.CommonOf} ${partyListSize}</li>
        <#if (viewIndex > 0)>
          <li><a class="nav-previous button-col" href="<@ofbizUrl>FindContacts?VIEW_SIZE=${viewSize}&amp;VIEW_INDEX=${viewIndex-1}&amp;hideFields=Y${paramList}</@ofbizUrl>">${uiLabelMap.CommonPrevious}</a></li>
        <#else>
          <li class="disabled button-col">${uiLabelMap.CommonPrevious}</li>
        </#if>
      </#if>
    </ul>
  </div>
  <div class="screenlet-body">
    <h2>${uiLabelMap.SfaFindResults}</h2>
    <#if partyList?exists>
      <#if partyList?has_content>
        <table class="basic-table hover-bar" cellspacing="0">
          <tr class="header-row-2">
            <td>${uiLabelMap.PartyPartyId}</td>
            <td>${uiLabelMap.PartyName}</td>
            <td>${uiLabelMap.SfaVCard}</td>
          </tr>
          <#assign alt_row = false>
          <#list partyList as partyRow>
            <tr valign="middle"<#if alt_row> class="alternate-row"</#if>>
              <td><a href="<@ofbizUrl>viewprofile?partyId=${partyRow.partyId}</@ofbizUrl>">${partyRow.partyId}</a></td>
              <td>
                <#if partyRow.getModelEntity().isField("lastName") && lastName?has_content>
                  <a href="<@ofbizUrl>viewprofile?partyId=${partyRow.partyId}</@ofbizUrl>">${partyRow.lastName}<#if partyRow.firstName?has_content>, ${partyRow.firstName}</#if></a>
                <#else>
                  <#assign partyName = Static["org.ofbiz.party.party.PartyHelper"].getPartyName(partyRow, true)>
                  <#if partyName?has_content>
                    <a href="<@ofbizUrl>viewprofile?partyId=${partyRow.partyId}</@ofbizUrl>">${partyName}</a>
                  <#else>
                    (${uiLabelMap.PartyNoNameFound})
                  </#if>
                </#if>
              </td>
              <td><a href="<@ofbizUrl>createVCardFromContact?partyId=${partyRow.partyId}</@ofbizUrl>">${uiLabelMap.SfaVCard}<a></td>
            </tr>
            <#-- toggle the row color -->
            <#assign alt_row = !alt_row>
          </#list>
        </table>
      <#else>
        ${uiLabelMap.PartyNoPartiesFound}
      </#if>
    </#if>
  </div>
</div>