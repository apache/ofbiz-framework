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

<div id="address-match-map" class="screenlet">
  <div class="screenlet-title-bar">
    <ul>
      <li class="h3">${uiLabelMap.PageTitleAddressMatches}</li>
      <li><a href="<@ofbizUrl>addressMatchMap</@ofbizUrl>">${uiLabelMap.PageTitleAddressMatchMap}</a></li>
    </ul>
    <br class="clear"/>
  </div>
  <div class="screenlet-body">
    <table class="basic-table" cellspacing="0">
      <form name="matchform" method="post" action="<@ofbizUrl>findAddressMatch?match=true</@ofbizUrl>">
        <tr>
          <td class="label">${uiLabelMap.PartyLastName}</td>
          <td><input type="text" name="lastName" class="required" value="${parameters.lastName!}"/><span class="tooltip">${uiLabelMap.CommonRequired}</span></td>
        </tr>
        <tr>
          <td class="label">${uiLabelMap.PartyFirstName}</td>
          <td><input type="text" name="firstName" class="required" value="${parameters.firstName!}"/><span class="tooltip">${uiLabelMap.CommonRequired}</span></td>
        </tr>
        <tr>
          <td class="label">${uiLabelMap.CommonAddress1}</td>
          <td><input type="text" name="address1" class="required" value="${parameters.address1!}"/><span class="tooltip">${uiLabelMap.CommonRequired}</span></td>
        </tr>
        <tr>
          <td class="label">${uiLabelMap.CommonAddress2}</td>
          <td><input type="text" name="address2" value="${parameters.address2!}"/></td>
        </tr>
        <tr>
          <td class="label">${uiLabelMap.CommonCity}</td>
          <td><input type="text" name="city" class="required" value="${parameters.city!}"/><span class="tooltip">${uiLabelMap.CommonRequired}</span></td>
        </tr>
        <tr>
          <td class="label">${uiLabelMap.CommonStateProvince}</td>
          <td>
            <select name="stateProvinceGeoId">
              <#if currentStateGeo?has_content>
                <option value="${currentStateGeo.geoId}">${currentStateGeo.geoName?default(currentStateGeo.geoId)}</option>
                <option value="${currentStateGeo.geoId}">---</option>
              </#if>
              <option value="ANY">${uiLabelMap.CommonAnyStateProvince}</option>
              ${screens.render("component://common/widget/CommonScreens.xml#states")}
            </select>
          </td>
        </tr>
        <tr>
          <td class="label">${uiLabelMap.PartyZipCode}</td>
          <td><input type="text" name="postalCode" class="required" value="${parameters.postalCode!}"/><span class="tooltip">${uiLabelMap.CommonRequired}</span></td>
        </tr>
        <tr>
            <td></td>
            <td><input type="submit" value="${uiLabelMap.PageTitleFindMatches}" /></td>
        </tr>
      </form>
      <#if match?has_content>
        <tr><td colspan="5">&nbsp;</td></tr>
        <tr>
          <td colspan="2">
            <#if matches?has_content>
              <table cellspacing="0" class="basic-table">
                <tr>
                  <td class="label" colspan="7">${uiLabelMap.PartyAddressMatching} ${lastName} / ${firstName} @ ${addressString}</td>
                </tr>
                <tr class="header-row">
                  <td>${uiLabelMap.PartyLastName}</td>
                  <td>${uiLabelMap.PartyFirstName}</td>
                  <td>${uiLabelMap.CommonAddress1}</td>
                  <td>${uiLabelMap.CommonAddress2}</td>
                  <td>${uiLabelMap.CommonCity}</td>
                  <td>${uiLabelMap.PartyZipCode}</td>
                  <td>${uiLabelMap.PartyPartyId}</td>
                </tr>
                <#list matches as match>
                  <#assign person = match.getRelatedOne("Party", false).getRelatedOne("Person", false)!>
                  <#assign group = match.getRelatedOne("Party", false).getRelatedOne("PartyGroup", false)!>
                  <tr>
                    <#if person?has_content>
                      <td>${person.lastName}</td>
                      <td>${person.firstName}</td>
                    <#elseif group?has_content>
                      <td colspan="2">${group.groupName}</td>
                    <#else>
                      <td colspan="2">${uiLabelMap.PartyUnknown}</td>
                    </#if>
                    <td>${Static["org.apache.ofbiz.party.party.PartyWorker"].makeMatchingString(delegator, match.address1)}</td>
                    <td>${Static["org.apache.ofbiz.party.party.PartyWorker"].makeMatchingString(delegator, match.address2?default("N/A"))}</td>
                    <td>${match.city}</td>
                    <td>${match.postalCode}</td>
                    <td class="button-col"><a href="<@ofbizUrl>viewprofile?partyId=${match.partyId}</@ofbizUrl>">${match.partyId}</a></td>
                  </tr>
                </#list>
              </table>
            <#else>
              ${uiLabelMap.PartyNoMatch}
            </#if>
          </td>
        </tr>
      </#if>
    </table>
  </div>
</div>