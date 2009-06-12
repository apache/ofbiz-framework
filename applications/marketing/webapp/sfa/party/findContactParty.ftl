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

<div class="screenlet">
  <div class="screenlet-title-bar">
    <ul>
      <li class="h3">${uiLabelMap.SfaFindContacts}</li>
      <#if parameters.hideFields?default("N") == "Y">
        <li><a href="<@ofbizUrl>FindContacts?hideFields=N${paramList}</@ofbizUrl>">${uiLabelMap.CommonShowLookupFields}</a></li>
      <#else>
        <#if partyList?exists><li><a href="<@ofbizUrl>FindContacts?hideFields=Y${paramList}</@ofbizUrl>">${uiLabelMap.CommonHideFields}</a></li></#if>
        <li><a href="javascript:document.lookupparty.submit();">${uiLabelMap.PartyLookupParty}</a></li>
      </#if>
    </ul>
    <br/><br/>
  </div>
  <#if parameters.hideFields?default("N") != "Y">
    <div class="screenlet-body">
      <#-- NOTE: this form is setup to allow a search by partial partyId or userLoginId; to change it to go directly to
          the viewprofile page when these are entered add the follow attribute to the form element:

           onsubmit="javascript:lookupParty('<@ofbizUrl>viewprofile</@ofbizUrl>');"
       -->
      <form method="post" name="lookupparty" action="<@ofbizUrl>FindContacts</@ofbizUrl>" class="basic-form">
        <input type="hidden" name="lookupFlag" value="Y"/>
        <input type="hidden" name="hideFields" value="Y"/>
        <input type="hidden" name="roleTypeId" value="${roleTypeId}"/>
        <table cellspacing="0">
          <tr>
            <td class="label">${uiLabelMap.PartyContactInformation}</td>
            <td>
              <input type="radio" name="extInfo" value="N" onclick="javascript:refreshInfo();" <#if extInfo == "N">checked="checked"</#if>/>${uiLabelMap.CommonNone}&nbsp;
              <input type="radio" name="extInfo" value="P" onclick="javascript:refreshInfo();" <#if extInfo == "P">checked="checked"</#if>/>${uiLabelMap.PartyPostal}&nbsp;
              <input type="radio" name="extInfo" value="T" onclick="javascript:refreshInfo();" <#if extInfo == "T">checked="checked"</#if>/>${uiLabelMap.PartyTelecom}&nbsp;
              <input type="radio" name="extInfo" value="O" onclick="javascript:refreshInfo();" <#if extInfo == "O">checked="checked"</#if>/>${uiLabelMap.CommonOther}&nbsp;
            </td>
          </tr>
          <tr>
            <td class="label">${uiLabelMap.PartyPartyId}</td>
            <td><input type="text" name="partyId" value="${parameters.partyId?if_exists}"/></td>
          </tr>
          <tr>
            <td class="label">${uiLabelMap.PartyLastName}</td>
            <td><input type="text" name="lastName" value="${parameters.lastName?if_exists}"/></td>
          </tr>
          <tr>
            <td class="label">${uiLabelMap.PartyFirstName}</td>
            <td><input type="text" name="firstName" value="${parameters.firstName?if_exists}"/></td>
          </tr>
          <#if extInfo == "P">
            <tr><td colspan="3"><hr/></td></tr>
            <tr>
              <td class="label">${uiLabelMap.CommonAddress1}</td>
              <td><input type="text" name="address1" value="${parameters.address1?if_exists}"/></td>
            </tr>
            <tr>
              <td class="label">${uiLabelMap.CommonAddress2}</td>
              <td><input type="text" name="address2" value="${parameters.address2?if_exists}"/></td>
            </tr>
            <tr>
              <td class="label">${uiLabelMap.CommonCity}</td>
              <td><input type="text" name="city" value="${parameters.city?if_exists}"/></td>
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
              <td class="label">${uiLabelMap.PartyPostalCode}</td>
              <td><input type="text" name="postalCode" value="${parameters.postalCode?if_exists}"/></td>
            </tr>
          </#if>
          <#if extInfo == "T">
            <tr><td colspan="3"><hr/></td></tr>
            <tr>
              <td class="label">${uiLabelMap.PartyCountryCode}</td>
              <td><input type="text" name="countryCode" value="${parameters.countryCode?if_exists}"/></td>
            </tr>
            <tr>
              <td class="label">${uiLabelMap.PartyAreaCode}</td>
              <td><input type="text" name="areaCode" value="${parameters.areaCode?if_exists}"/></td>
            </tr>
            <tr>
              <td class="label">${uiLabelMap.PartyContactNumber}</td>
              <td><input type="text" name="contactNumber" value="${parameters.contactNumber?if_exists}"/></td>
            </tr>
          </#if>
          <#if extInfo == "O">
            <tr><td colspan="3"><hr/></td></tr>
            <tr>
              <td class="label">${uiLabelMap.PartyContactInformation}</td>
              <td><input type="text" name="infoString" value="${parameters.infoString?if_exists}"/></td>
            </tr>
          </#if>
          <tr><td colspan="3"><hr/></td></tr>
          <tr>
            <td>&nbsp;</td>
            <td>
              <input type="submit" value="${uiLabelMap.CommonFind}" onClick="javascript:document.lookupparty.submit();"/>
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
