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

  <div id="partyInformation" class="screenlet">
    <div class="screenlet-title-bar">
      <ul>
        <#if lookupPerson?has_content>
          <li class="head3">${uiLabelMap.PartyPersonalInformation}</li>
          <#if security.hasEntityPermission("PARTYMGR", "_UPDATE", session)>
            <li><a href="<@ofbizUrl>editperson?partyId=${party.partyId}</@ofbizUrl>">${uiLabelMap.CommonUpdate}</a></li>
          </#if>
        </#if>
        <#if lookupGroup?has_content>
          <#assign lookupPartyType = party.getRelatedOneCache("PartyType")>
          <li class="head3">${uiLabelMap.PartyPartyGroupInformation}</li>
          <#if security.hasEntityPermission("PARTYMGR", "_UPDATE", session)>
            <li><a href="<@ofbizUrl>editpartygroup?partyId=${party.partyId}</@ofbizUrl>">${uiLabelMap.CommonUpdate}</a></li>
          </#if>
        </#if>
      </ul>
      <br class="clear" />
    </div>
    <div class="screenlet-body">
      <#if lookupPerson?has_content>
        <table class="basic-table" cellspacing="0">
          <tr>
            <td class="label">${uiLabelMap.PartyName}</td>
            <td>
              ${lookupPerson.personalTitle?if_exists}
              ${lookupPerson.firstName?if_exists}
              ${lookupPerson.middleName?if_exists}
              ${lookupPerson.lastName?if_exists}
              ${lookupPerson.suffix?if_exists}
            </td>
          </tr>
          <#if lookupPerson.nickname?has_content>
            <tr><td class="label">${uiLabelMap.PartyNickname}</td><td>${lookupPerson.nickname}</td></tr>
          </#if>
            <#if lookupPerson.gender?has_content>
            <tr><td class="label">${uiLabelMap.PartyGender}</td><td>${lookupPerson.gender}</td></tr>
          </#if>
          <#if lookupPerson.birthDate?has_content>
            <tr><td class="label">${uiLabelMap.PartyBirthDate}</td><td>${lookupPerson.birthDate.toString()}</td></tr>
          </#if>
          <#if lookupPerson.height?has_content>
            <tr><td class="label">${uiLabelMap.PartyHeight}</td><td>${lookupPerson.height}</td></tr>
          </#if>
          <#if lookupPerson.weight?has_content>
            <tr><td class="label">${uiLabelMap.PartyWeight}</td><td>${lookupPerson.weight}</td></tr>
          </#if>
          <#if lookupPerson.mothersMaidenName?has_content>
            <tr><td class="label">${uiLabelMap.PartyMothersMaidenName}</td><td>${lookupPerson.mothersMaidenName}</td></tr>
          </#if>
          <#if lookupPerson.maritalStatus?has_content>
            <tr><td class="label">${uiLabelMap.PartyMaritalStatus}</td><td>${lookupPerson.maritalStatus}</td></tr>
          </#if>
          <#if lookupPerson.socialSecurityNumber?has_content>
            <tr><td class="label">${uiLabelMap.PartySocialSecurityNumber}</td><td>${lookupPerson.socialSecurityNumber}</td></tr>
          </#if>
          <#if lookupPerson.passportNumber?has_content>
            <tr><td class="label">${uiLabelMap.PartyPassportNumber}</td><td>${lookupPerson.passportNumber}</td></tr>
          </#if>
          <#if lookupPerson.passportExpireDate?has_content>
            <tr><td class="label">${uiLabelMap.PartyPassportExpire}</td><td>${lookupPerson.passportExpireDate.toString()}</td></tr>
          </#if>
          <#if lookupPerson.totalYearsWorkExperience?has_content>
            <tr><td class="label">${uiLabelMap.PartyYearsWork}</td><td>${lookupPerson.totalYearsWorkExperience}</td></tr>
          </#if>
          <#if lookupPerson.comments?has_content>
            <tr><td class="label">${uiLabelMap.PartyComments}</td><td>${lookupPerson.comments}</td></tr>
          </#if>
        </table>
      <#elseif lookupGroup?has_content>
        <div>${lookupGroup.groupName} (${(lookupPartyType.get("description",locale))?if_exists})</div>
      <#else>
        <div>${uiLabelMap.PartyInformationNotFound}</div>
      </#if>
      <#if partyNameHistoryList?has_content>
        <div><hr/></div>
        <div>${uiLabelMap.PartyHistoryName}</div>
        <#list partyNameHistoryList as partyNameHistory>
          <#if lookupPerson?has_content>
            <div>${uiLabelMap.PartyHistoryWas}: ${partyNameHistory.personalTitle?if_exists} ${partyNameHistory.firstName?if_exists} ${partyNameHistory.middleName?if_exists} ${partyNameHistory.lastName?if_exists} ${partyNameHistory.suffix?if_exists} (${uiLabelMap.PartyHistoryChanged}: ${partyNameHistory.changeDate})</div>
          <#elseif lookupGroup?has_content>
            <div>${uiLabelMap.PartyHistoryWas}: ${partyNameHistory.groupName?if_exists} (${uiLabelMap.PartyHistoryChanged}: ${partyNameHistory.changeDate})</div>
          </#if>
        </#list>
      </#if>
    </div>
  </div>