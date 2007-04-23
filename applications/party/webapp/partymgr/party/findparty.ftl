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

<#assign extInfo = parameters.extInfo?default("N")>

<!-- begin findParty.ftl -->
<div id="findPartyParameters" class="screenlet">
  <div class="screenlet-title-bar">
    <ul>
      <h3>${uiLabelMap.PartyFindParty}</h3>
      <#if parameters.hideFields?default("N") == "Y">
        <li><a href="<@ofbizUrl>findparty?hideFields=N${paramList}</@ofbizUrl>">${uiLabelMap.CommonShowLookupFields}</a></li>
      <#else>
        <#if partyList?exists><li><a href="<@ofbizUrl>findparty?hideFields=Y${paramList}</@ofbizUrl>">${uiLabelMap.CommonHideFields}</a></li></#if>
        <li><a href="javascript:document.lookupparty.submit();">${uiLabelMap.PartyLookupParty}</a></li>
      </#if>
    </ul>
	<br class="clear" />
  </div>
  <#if parameters.hideFields?default("N") != "Y">
    <div class="screenlet-body">
      <form method="post" name="lookupparty" action="<@ofbizUrl>findparty</@ofbizUrl>" class="basic-form" onsubmit="javascript:lookupParty('<@ofbizUrl>viewprofile</@ofbizUrl>');">
        <input type="hidden" name="lookupFlag" value="Y"/>
        <input type="hidden" name="hideFields" value="Y"/>
        <table cellspacing="0">
          <tr>
            <td class="label">${uiLabelMap.PartyContactInformation} :</td>
            <td>
              <input type="radio" name="extInfo" value="N" onclick="javascript:refreshInfo();" <#if extInfo == "N">checked="checked"</#if>/>${uiLabelMap.CommonNone}&nbsp;
              <input type="radio" name="extInfo" value="P" onclick="javascript:refreshInfo();" <#if extInfo == "P">checked="checked"</#if>/>${uiLabelMap.PartyPostal}&nbsp;
              <input type="radio" name="extInfo" value="T" onclick="javascript:refreshInfo();" <#if extInfo == "T">checked="checked"</#if>/>${uiLabelMap.PartyTelecom}&nbsp;
              <input type="radio" name="extInfo" value="O" onclick="javascript:refreshInfo();" <#if extInfo == "O">checked="checked"</#if>/>${uiLabelMap.CommonOther}&nbsp;
            </td>
          </tr>
          <tr>
            <td class="label">${uiLabelMap.PartyPartyId} :</td>
            <td><input type="text" name="partyId" value="${parameters.partyId?if_exists}"/></td>
          </tr>
          <tr>
            <td class="label">${uiLabelMap.PartyUserLogin} :</td>
            <td><input type="text" name="userLoginId" value="${parameters.userLoginId?if_exists}"/></td>
          </tr>
          <tr>
            <td class="label">${uiLabelMap.PartyLastName} :</td>
            <td><input type="text" name="lastName" value="${parameters.lastName?if_exists}"/></td>
          </tr>
          <tr>
            <td class="label">${uiLabelMap.PartyFirstName} :</td>
            <td><input type="text" name="firstName" value="${parameters.firstName?if_exists}"/></td>
          </tr>
          <tr>
            <td class="label">${uiLabelMap.PartyPartyGroupName} :</td>
            <td><input type="text" name="groupName" value="${parameters.groupName?if_exists}"/></td>
          </tr>
          <tr>
            <td class="label">${uiLabelMap.PartyRoleType} :</td>
            <td>
              <select name="roleTypeId">
                <#if currentRole?has_content>
                  <option value="${currentRole.roleTypeId}">${currentRole.get("description",locale)}</option>
                  <option value="${currentRole.roleTypeId}">---</option>
                </#if>
                <option value="ANY">${uiLabelMap.CommonAnyRoleType}</option>
                <#list roleTypes as roleType>
                  <option value="${roleType.roleTypeId}">${roleType.get("description",locale)}</option>
                </#list>
              </select>
            </td>
          </tr>
          <#if extInfo == "P">
            <tr><td colspan="3"><hr/></td></tr>
            <tr>
              <td class="label">${uiLabelMap.CommonAddress1} :</td>
              <td><input type="text" name="address1" value="${parameters.address1?if_exists}"/></td>
            </tr>
            <tr>
              <td class="label">${uiLabelMap.CommonAddress2} :</td>
              <td><input type="text" name="address2" value="${parameters.address2?if_exists}"/></td>
            </tr>
            <tr>
              <td class="label">${uiLabelMap.CommonCity} :</td>
              <td><input type="text" name="city" value="${parameters.city?if_exists}"/></td>
            </tr>
            <tr>
              <td class="label">${uiLabelMap.CommonStateProvince} :</td>
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
              <td class="label">${uiLabelMap.PartyPostalCode} :</td>
              <td><input type="text" name="postalCode" value="${parameters.postalCode?if_exists}"/></td>
            </tr>
          </#if>
          <#if extInfo == "T">
            <tr><td colspan="3"><hr/></td></tr>
            <tr>
              <td class="label">${uiLabelMap.PartyCountryCode} :</td>
              <td><input type="text" name="countryCode" value="${parameters.countryCode?if_exists}"/></td>
            </tr>
            <tr>
              <td class="label">${uiLabelMap.PartyAreaCode} :</td>
              <td><input type="text" name="areaCode" value="${parameters.areaCode?if_exists}"/></td>
            </tr>
            <tr>
              <td class="label">${uiLabelMap.PartyContactNumber} :</td>
              <td><input type="text" name="contactNumber" value="${parameters.contactNumber?if_exists}"/></td>
            </tr>
          </#if>
          <#if extInfo == "O">
            <tr><td colspan="3"><hr/></td></tr>
            <tr>
              <td class="label">${uiLabelMap.PartyContactInfoList} :</td>
              <td><input type="text" name="infoString" value="${parameters.infoString?if_exists}"/></td>
            </tr>
          </#if>
          <tr><td colspan="3"><hr/></td></tr>
          <tr>
            <td>&nbsp;</td>
            <td>
              <input type="submit" value="${uiLabelMap.PartyLookupParty}" onClick="javascript:document.lookupparty.submit();"/>
              <a href="<@ofbizUrl>findparty?showAll=Y&amp;hideFields=Y&amp;lookupFlag=Y</@ofbizUrl>" class="smallSubmit">${uiLabelMap.CommonShowAllRecords}</a>
            </td>
          </tr>
        </table>
      </form>
    </div>
  </#if>
</div>

<#if parameters.hideFields?default("N") != "Y">
  <script language="JavaScript" type="text/javascript">
    <!--//
      document.lookupparty.partyId.focus();
    //-->
  </script>
</#if>
<#if partyList?exists>
  <br/>
  <div id="findPartyResults" class="screenlet">
    <div class="screenlet-title-bar">
      <ul>
        <h3>${uiLabelMap.PartyPartiesFound}</h3>
          <#if (partyListSize > 0)>
            <#if (partyListSize > highIndex)>
              <li><a class="nav-next" href="<@ofbizUrl>findparty?VIEW_SIZE=${viewSize}&amp;VIEW_INDEX=${viewIndex+1}&amp;hideFields=${parameters.hideFields?default("N")}${paramList}</@ofbizUrl>">${uiLabelMap.CommonNext}</a></li>
            <#else>
              <li class="disabled">${uiLabelMap.CommonNext}</li>
            </#if>
            <li>${lowIndex} - ${highIndex} ${uiLabelMap.CommonOf} ${partyListSize}</li>
            <#if (viewIndex > 0)>
              <li><a class="nav-previous" href="<@ofbizUrl>findparty?VIEW_SIZE=${viewSize}&amp;VIEW_INDEX=${viewIndex-1}&amp;hideFields=${parameters.hideFields?default("N")}${paramList}</@ofbizUrl>">${uiLabelMap.CommonPrevious}</a></li>
            <#else>
              <li class="disabled">${uiLabelMap.CommonPrevious}</li>
            </#if>
          </#if>
      </ul>
      <br class="clear" />
    </div>
    <#if partyList?has_content>
      <table class="basic-table" cellspacing="0">
        <tr class="header-row">
          <td>${uiLabelMap.PartyPartyId}</td>
          <td>${uiLabelMap.PartyUserLogin}</td>
          <td>${uiLabelMap.PartyName}</td>
          <#if extInfo?default("") == "P" >
            <td>${uiLabelMap.PartyCity}</td>
          </#if>
          <#if extInfo?default("") == "P">
            <td>${uiLabelMap.PartyPostalCode}</td>
          </#if>
          <#if extInfo?default("") == "T">
            <td>${uiLabelMap.PartyAreaCode}</td>
          </#if>
          <td>${uiLabelMap.PartyType}</td>
          <td>&nbsp;</td>
        </tr>
        <#assign rowClass = "2">
        <#list partyList as partyRow>
          <#assign partyType = partyRow.getRelatedOne("PartyType")?if_exists>
          <tr<#if rowClass == "1"> class="alternate-row"</#if>>
            <td><a href="<@ofbizUrl>viewprofile?partyId=${partyRow.partyId}</@ofbizUrl>">${partyRow.partyId}</a></td>
            <td>
              <#if partyRow.containsKey("userLoginId")>
                ${partyRow.userLoginId?default("N/A")}
              <#else>
                <#assign userLogins = partyRow.getRelated("UserLogin")>
                <#if (userLogins.size() > 0)>
                  <#if (userLogins.size() > 1)>
                    (${uiLabelMap.CommonMany})
                  <#else>
                  <#assign userLogin = userLogins.get(0)>
                    ${userLogin.userLoginId}
                  </#if>
                <#else>
                  (${uiLabelMap.CommonNone})
                </#if>
              </#if>
            </td>
            <td>
              <#if partyRow.lastName?has_content>
                ${partyRow.lastName}<#if partyRow.firstName?has_content>, ${partyRow.firstName}</#if>
              <#elseif partyRow.groupName?has_content>
                ${partyRow.groupName}
              <#else>
                <#assign partyName = Static["org.ofbiz.party.party.PartyHelper"].getPartyName(partyRow, true)>
                <#if partyName?has_content>
                  ${partyName}
                <#else>
                  (${uiLabelMap.PartyNoNameFound})
                </#if>
              </#if>
            </td>
            <#if extInfo?default("") == "T">
              <td>${partyRow.areaCode?if_exists}</td>
            </#if>
            <#if extInfo?default("") == "P" >
               <td>${partyRow.city?if_exists}, ${partyRow.stateProvinceGeoId?if_exists}</td>
            </#if>
            <#if extInfo?default("") == "P">
              <td>${partyRow.postalCode?if_exists}</td>
            </#if>
            <td><#if partyType.description?exists>${partyType.get("description", locale)}<#else>???</#if></td>
          <td class="button-col align-float">
              <a href="<@ofbizUrl>viewprofile?partyId=${partyRow.partyId}</@ofbizUrl>">${uiLabelMap.CommonDetails}</a>
              <#if security.hasRolePermission("ORDERMGR", "_VIEW", "", "", session)>
                <a href="/ordermgr/control/findorders?lookupFlag=Y&amp;hideFields=Y&amp;partyId=${partyRow.partyId + externalKeyParam}">${uiLabelMap.OrderOrders}</a>
                <a href="/ordermgr/control/FindQuote?partyId=${partyRow.partyId + externalKeyParam}">${uiLabelMap.OrderOrderQuotes}</a>
              </#if>
              <#if security.hasEntityPermission("ORDERMGR", "_CREATE", session)>
                <a href="/ordermgr/control/checkinits?partyId=${partyRow.partyId + externalKeyParam}">${uiLabelMap.OrderNewOrder}</a>
                <a href="/ordermgr/control/EditQuote?partyId=${partyRow.partyId + externalKeyParam}">${uiLabelMap.OrderNewQuote}</a>
              </#if>
            </td>
          </tr>
          <#-- toggle the row color -->
          <#if rowClass == "2">
            <#assign rowClass = "1">
          <#else>
            <#assign rowClass = "2">
          </#if>
        </#list>
      </table>
    <#else>
      <div class="screenlet-body">
        <span class="head3">${uiLabelMap.PartyNoPartiesFound}</span>
        <a href="<@ofbizUrl>createnew</@ofbizUrl>" class="smallSubmit">${uiLabelMap.CommonCreateNew}</a>
      </div>
    </#if>
    <#if lookupErrorMessage?exists>
      <div><h3>${lookupErrorMessage}</h3></div>
    </#if>
  </div>
</#if>
<!-- end findParty.ftl -->
