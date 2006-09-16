<#--

Copyright 2001-2006 The Apache Software Foundation

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.
-->

<script language="JavaScript" type="text/javascript">
function lookupParty(click) {
    partyIdValue = document.lookupparty.partyId.value;
    userLoginIdValue = document.lookupparty.userlogin_id.value;
    if (partyIdValue.length > 1 || userLoginIdValue.length > 1) {
        document.lookupparty.action = "<@ofbizUrl>viewprofile</@ofbizUrl>";
    } else {
        document.lookupparty.action = "<@ofbizUrl>findparty</@ofbizUrl>";
    }

    if (click) {
        document.lookupparty.submit();
    }
    return true;
}
function refreshInfo() {
    document.lookupparty.lookupFlag.value = "N";
    document.lookupparty.hideFields.value = "N";
    document.lookupparty.submit();
}
</script>

<#assign extInfo = parameters.extInfo?default("N")>
<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxlink">
            <div class="tabletext">
              <#if parameters.hideFields?default("N") == "Y">
                <a href="<@ofbizUrl>findparty?hideFields=N${paramList}</@ofbizUrl>" class="submenutextright">${uiLabelMap.CommonShowLookupFields}</a>
              <#else>
                <#if partyList?exists><a href="<@ofbizUrl>findparty?hideFields=Y${paramList}</@ofbizUrl>" class="submenutext">${uiLabelMap.CommonHideFields}</a></#if>
                <a href="javascript:document.lookupparty.submit();" class="submenutextright">${uiLabelMap.PartyLookupParty}</a>
              </#if>
            </div>
        </div>
        <div class="boxhead">&nbsp;${uiLabelMap.PartyFindParty}</div>
    </div>
  <#if parameters.hideFields?default("N") != "Y">
    <div class="screenlet-body">
      <form method="post" name="lookupparty" action="<@ofbizUrl>findparty</@ofbizUrl>" onsubmit="javascript:lookupParty();">
          <input type="hidden" name="lookupFlag" value="Y"/>
          <input type="hidden" name="hideFields" value="Y"/>
          <table border="0" cellspacing="0" cellpadding="2">
            <tr>
              <td width="25%" align="right" nowrap><div class="tableheadtext">${uiLabelMap.PartyContactInformation} :</div></td>
              <td width="5%">&nbsp;</td>
              <td nowrap>
                <div class="tabletext">
                  <input type="radio" name="extInfo" value="N" onclick="javascript:refreshInfo();" <#if extInfo == "N">checked="checked"</#if>/>${uiLabelMap.CommonNone}&nbsp;
                  <input type="radio" name="extInfo" value="P" onclick="javascript:refreshInfo();" <#if extInfo == "P">checked="checked"</#if>/>${uiLabelMap.PartyPostal}&nbsp;
                  <input type="radio" name="extInfo" value="T" onclick="javascript:refreshInfo();" <#if extInfo == "T">checked="checked"</#if>/>${uiLabelMap.PartyTelecom}&nbsp;
                  <input type="radio" name="extInfo" value="O" onclick="javascript:refreshInfo();" <#if extInfo == "O">checked="checked"</#if>/>${uiLabelMap.CommonOther}&nbsp;
                </div>
              </td>
            </tr>
            <tr>
              <td width="25%" align="right"><div class="tableheadtext">${uiLabelMap.PartyPartyId} :</div></td>
              <td width="5%">&nbsp;</td>
              <td><input type="text" class="inputBox" name="partyId"/></td>
            </tr>
            <tr>
              <td width="25%" align="right"><div class="tableheadtext">${uiLabelMap.PartyUserLogin} :</div></td>
              <td width="5%">&nbsp;</td>
              <td><input type="text" class="inputBox" name="userLoginId" value="${parameters.userLoginId?if_exists}"/></td>
            </tr>
            <tr>
              <td width="25%" align="right"><div class="tableheadtext">${uiLabelMap.PartyLastName} :</div></td>
              <td width="5%">&nbsp;</td>
              <td><input type="text" class="inputBox" name="lastName" value="${parameters.lastName?if_exists}"/></td>
            </tr>
            <tr>
              <td width="25%" align="right"><div class="tableheadtext">${uiLabelMap.PartyFirstName} :</div></td>
              <td width="5%">&nbsp;</td>
              <td><input type="text" class="inputBox" name="firstName" value="${parameters.firstName?if_exists}"/></td>
            </tr>
            <tr>
              <td width="25%" align="right"><div class="tableheadtext">${uiLabelMap.PartyPartyGroupName} :</div></td>
              <td width="5%">&nbsp;</td>
              <td><input type="text" class="inputBox" name="groupName" value="${parameters.groupName?if_exists}"/></td>
            </tr>
            <tr>
              <td width="25%" align="right"><div class="tableheadtext">${uiLabelMap.PartyRoleType} :</div></td>
              <td width="5%">&nbsp;</td>
              <td>
                <select name="roleTypeId" class="selectBox">
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
              <tr><td colspan="3"><hr class="sepbar"/></td></tr>
              <tr>
                <td width="25%" align="right"><div class="tableheadtext">${uiLabelMap.CommonAddress1} :</div></td>
                <td width="5%">&nbsp;</td>
                <td><input type="text" class="inputBox" name="address1" value="${parameters.address1?if_exists}"/></td>
              </tr>
              <tr>
                <td width="25%" align="right"><div class="tableheadtext">${uiLabelMap.CommonAddress2} :</div></td>
                <td width="5%">&nbsp;</td>
                <td><input type="text" class="inputBox" name="address2" value="${parameters.address2?if_exists}"/></td>
              </tr>
              <tr>
                <td width="25%" align="right"><div class="tableheadtext">${uiLabelMap.CommonCity} :</div></td>
                <td width="5%">&nbsp;</td>
                <td><input type="text" class="inputBox" name="city" value="${parameters.city?if_exists}"/></td>
              </tr>
              <tr>
                <td width="25%" align="right"><div class="tableheadtext">${uiLabelMap.CommonStateProvince} :</div></td>
                <td width="5%">&nbsp;</td>
                <td>
                  <select name="stateProvinceGeoId" class="selectBox">
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
                <td width="25%" align="right"><div class="tableheadtext">${uiLabelMap.PartyPostalCode} :</div></td>
                <td width="5%">&nbsp;</td>
                <td><input type="text" class="inputBox" name="postalCode" value="${parameters.postalCode?if_exists}"/></td>
              </tr>
            </#if>
            <#if extInfo == "T">
              <tr><td colspan="3"><hr class="sepbar"/></td></tr>
              <tr>
                <td width="25%" align="right"><div class="tableheadtext">${uiLabelMap.PartyCountryCode} :</div></td>
                <td width="5%">&nbsp;</td>
                <td><input type="text" class="inputBox" name="countryCode" value="${parameters.countryCode?if_exists}"/></td>
              </tr>
              <tr>
                <td width="25%" align="right"><div class="tableheadtext">${uiLabelMap.PartyAreaCode} :</div></td>
                <td width="5%">&nbsp;</td>
                <td><input type="text" class="inputBox" name="areaCode" value="${parameters.areaCode?if_exists}"/></td>
              </tr>
              <tr>
                <td width="25%" align="right"><div class="tableheadtext">${uiLabelMap.PartyContactNumber} :</div></td>
                <td width="5%">&nbsp;</td>
                <td><input type="text" class="inputBox" name="contactNumber" value="${parameters.contactNumber?if_exists}"/></td>
              </tr>
            </#if>
            <#if extInfo == "O">
              <tr><td colspan="3"><hr class="sepbar"/></td></tr>
              <tr>
                <td width="25%" align="right"><div class="tableheadtext">${uiLabelMap.PartyContactInfoList} :</div></td>
                <td width="5%">&nbsp;</td>
                <td><input type="text" class="inputBox" name="infoString" value="${parameters.infoString?if_exists}"/></td>
              </tr>
            </#if>
            <tr><td colspan="3"><hr class="sepbar"/></td></tr>
            <tr>
              <td colspan="3" align="center">
                <div class="tabletext">
                  <a href="javascript:document.lookupparty.submit();" class="buttontext">${uiLabelMap.PartyLookupParty}</a>
                  <a href="<@ofbizUrl>findparty?showAll=Y&amp;hideFields=Y&amp;lookupFlag=Y</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonShowAllRecords}</a>
                </div>
              </td>
            </tr>
          </table>
          <input type="image" src="<@ofbizContentUrl>/images/spacer.gif</@ofbizContentUrl>" onClick="javascript:document.lookupparty.submit();"/>
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
    <table border="0" width="100%" cellspacing="0" cellpadding="0" class="boxoutside">
      <tr>
        <td width="100%">
          <table width="100%" border="0" cellspacing="0" cellpadding="0" class="boxtop">
            <tr>
              <td width="50%"><div class="boxhead">${uiLabelMap.PartyPartiesFound}</div></td>
              <td width="50%">
                 <div class="boxhead" align="right">
                  <#if (partyListSize > 0)>
                    <#if (viewIndex > 1)>
                      <a href="<@ofbizUrl>findparty?VIEW_SIZE=${viewSize}&amp;VIEW_INDEX=${viewIndex-1}&amp;hideFields=${parameters.hideFields?default("N")}${paramList}</@ofbizUrl>" class="submenutext">${uiLabelMap.CommonPrevious}</a>
                    <#else>
                      <span class="submenutextdisabled">${uiLabelMap.CommonPrevious}</span>
                    </#if>
                    <#if (partyListSize > 0)>
                      <span class="submenutextinfo">${lowIndex} - ${highIndex} ${uiLabelMap.CommonOf} ${partyListSize}</span>
                    </#if>
                    <#if (partyListSize > highIndex)>
                      <a href="<@ofbizUrl>findparty?VIEW_SIZE=${viewSize}&amp;VIEW_INDEX=${viewIndex+1}&amp;hideFields=${parameters.hideFields?default("N")}${paramList}</@ofbizUrl>" class="submenutextright">${uiLabelMap.CommonNext}</a>
                    <#else>
                      <span class="submenutextrightdisabled">${uiLabelMap.CommonNext}</span>
                    </#if>
                  </#if>
                  &nbsp;
                </div>
              </td>
            </tr>
          </table>

          <table width="100%" border="0" cellpadding="0" cellspacing="0">
            <tr>
              <td><div class="tableheadtext">${uiLabelMap.PartyPartyId}</div></td>
              <td><div class="tableheadtext">${uiLabelMap.PartyUserLogin}</div></td>
              <td><div class="tableheadtext">${uiLabelMap.PartyName}</div></td>
              <#if extInfo?default("") == "P">
                <td><div class="tableheadtext">${uiLabelMap.PartyPostalCode}</div></td>
              </#if>
              <#if extInfo?default("") == "T">
                <td><div class="tableheadtext">${uiLabelMap.PartyAreaCode}</div></td>
              </#if>
              <td><div class="tableheadtext">${uiLabelMap.PartyType}</div></td>
              <td>&nbsp;</td>
            </tr>
            <tr><td colspan="6"><hr class="sepbar"/></td></tr>
            <#if partyList?has_content>
              <#assign rowClass = "viewManyTR2">
              <#list partyList as partyRow>
                <#assign partyType = partyRow.getRelatedOne("PartyType")?if_exists>
                <tr class="${rowClass}">
                  <td><a href="<@ofbizUrl>viewprofile?partyId=${partyRow.partyId}</@ofbizUrl>" class="linktext">${partyRow.partyId}</a></td>
                  <td>
                    <div class="tabletext">
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
                    </div>
                  </td>
                  <td>
                    <div class="tabletext">
                      <#if partyRow.containsKey("lastName")>
                        <#if partyRow.lastName?has_content>
                          ${partyRow.lastName}<#if partyRow.firstName?has_content>, ${partyRow.firstName}</#if>
                        <#else>
                          (${uiLabelMap.PartyNoNameFound})
                        </#if>
                      <#elseif partyRow.containsKey("groupName")>
                        <#if partyRow.groupName?has_content>
                          ${partyRow.groupName}
                        <#else>
                          (${uiLabelMap.PartyNoNameFound})
                        </#if>
                      <#else>
                        ${Static["org.ofbiz.party.party.PartyHelper"].getPartyName(partyRow, true)}
                      </#if>
                    </div>
                  </td>
                  <#if extInfo?default("") == "P">
                    <td><div class="tabletext">${partyRow.postalCode?if_exists}</div></td>
                  </#if>
                  <#if extInfo?default("") == "T">
                    <td><div class="tabletext">${partyRow.areaCode?if_exists}</div></td>
                  </#if>
                  <td><div class="tabletext"><#if partyType.description?exists>${partyType.get("description", locale)}<#else>???</#if></div></td>
                  <td align="right">
                    <!-- this is all on one line so that no break will be inserted -->
                    <div class="tabletext"><nobr>
                      <a href="<@ofbizUrl>viewprofile?partyId=${partyRow.partyId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonDetails}</a>&nbsp;
                      <#if security.hasRolePermission("ORDERMGR", "_VIEW", "", "", session)>
                        <a href="/ordermgr/control/findorders?lookupFlag=Y&amp;hideFields=Y&amp;partyId=${partyRow.partyId + externalKeyParam}" class="buttontext">${uiLabelMap.OrderOrders}</a>&nbsp;
                      </#if>
                      <#if security.hasEntityPermission("ORDERMGR", "_CREATE", session)>
                        <a href="/ordermgr/control/checkinits?partyId=${partyRow.partyId + externalKeyParam}" class="buttontext">${uiLabelMap.OrderNewOrder}</a>&nbsp;
                      </#if>
                    </nobr></div>
                  </td>
                </tr>
                <#-- toggle the row color -->
                <#if rowClass == "viewManyTR2">
                  <#assign rowClass = "viewManyTR1">
                <#else>
                  <#assign rowClass = "viewManyTR2">
                </#if>
              </#list>
            <#else>
              <tr>
                <td colspan="5">
                  <span class="head3">${uiLabelMap.PartyNoPartiesFound}</span>
                  &nbsp;&nbsp;<a href="<@ofbizUrl>createnew</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonCreateNew}</a>
                </td>
              </tr>
            </#if>
            <#if lookupErrorMessage?exists>
              <tr>
                <td colspan="5"><div class="head3">${lookupErrorMessage}</div></td>
              </tr>
            </#if>
          </table>
        </td>
      </tr>
    </table>
  </#if>
