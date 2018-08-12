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

<div id="findEmployee" class="screenlet">
    <div class="screenlet-title-bar">
        <ul>
            <li class="h3">${uiLabelMap.CommonFind} ${uiLabelMap.HumanResEmployee}</li>
            <#if parameters.hideFields?default("N") == "Y">
                <li><a href="<@ofbizUrl>findEmployees?hideFields=N${paramList}</@ofbizUrl>">${uiLabelMap.CommonShowLookupFields}</a></li>
            <#else>
            <#if partyList??><li><a href="<@ofbizUrl>findEmployees?hideFields=Y${paramList}</@ofbizUrl>">${uiLabelMap.CommonHideFields}</a></li></#if>
                <li><a href="javascript:document.lookupparty.submit();">${uiLabelMap.PartyLookupParty}</a></li>
            </#if>
        </ul>
        <br class="clear"/>
    </div>
    <#if parameters.hideFields?default("N") != "Y">
    <div class="screenlet-body">
      <#-- NOTE: this form is setup to allow a search by partial partyId or userLoginId; to change it to go directly to
          the viewprofile page when these are entered add the follow attribute to the form element:

           onsubmit="javascript:lookupparty('<@ofbizUrl>viewprofile</@ofbizUrl>');"
       -->
        <form method="post" name="lookupparty" action="<@ofbizUrl>findEmployees</@ofbizUrl>" class="basic-form">
            <input type="hidden" name="lookupFlag" value="Y"/>
            <input type="hidden" name="hideFields" value="Y"/>
            <table cellspacing="0">
                <tr><td class="label">${uiLabelMap.PartyContactInformation}</td>
                    <td><label><input type="radio" name="extInfo" value="N" onclick="javascript:refreshInfo();" <#if extInfo == "N">checked="checked"</#if>/>${uiLabelMap.CommonNone}</label>&nbsp;
                        <label><input type="radio" name="extInfo" value="P" onclick="javascript:refreshInfo();" <#if extInfo == "P">checked="checked"</#if>/>${uiLabelMap.PartyPostal}</label>&nbsp;
                        <label><input type="radio" name="extInfo" value="T" onclick="javascript:refreshInfo();" <#if extInfo == "T">checked="checked"</#if>/>${uiLabelMap.PartyTelecom}</label>&nbsp;
                        <label><input type="radio" name="extInfo" value="O" onclick="javascript:refreshInfo();" <#if extInfo == "O">checked="checked"</#if>/>${uiLabelMap.CommonOther}</label>&nbsp;
                    </td>
                </tr>
                <tr><td class='label'>${uiLabelMap.PartyPartyId}</td>
                    <td>
                      <@htmlTemplate.lookupField value='${requestParameters.partyId!}' formName="lookupparty" name="partyId" id="partyId" fieldFormName="LookupPerson"/>
                    </td>
                </tr>
                <tr><td class="label">${uiLabelMap.PartyUserLogin}</td>
                    <td><input type="text" name="userLoginId" value="${parameters.userLoginId!}"/></td>
                </tr>
                <tr><td class="label">${uiLabelMap.PartyLastName}</td>
                    <td><input type="text" name="lastName" value="${parameters.lastName!}"/></td>
                </tr>
                <tr><td class="label">${uiLabelMap.PartyFirstName}</td>
                    <td><input type="text" name="firstName" value="${parameters.firstName!}"/></td>
                </tr>
                <tr><td><input type="hidden" name="groupName" value="${parameters.groupName!}"/></td></tr>
                <tr><td><input type="hidden" name="roleTypeId" value="EMPLOYEE"/></td></tr>
            <#if extInfo == "P">
                <tr><td colspan="3"><hr /></td></tr><tr>
                    <td class="label">${uiLabelMap.CommonAddress1}</td>
                    <td><input type="text" name="address1" value="${parameters.address1!}"/></td>
                </tr>
                <tr><td class="label">${uiLabelMap.CommonAddress2}</td>
                    <td><input type="text" name="address2" value="${parameters.address2!}"/></td>
                </tr>
                <tr><td class="label">${uiLabelMap.CommonCity}</td>
                    <td><input type="text" name="city" value="${parameters.city!}"/></td>
                </tr>
                <tr><td class="label">${uiLabelMap.CommonStateProvince}</td>
                    <td><select name="stateProvinceGeoId">
                        <#if currentStateGeo?has_content>
                            <option value="${currentStateGeo.geoId}">${currentStateGeo.geoName?default(currentStateGeo.geoId)}</option>
                            <option value="${currentStateGeo.geoId}">---</option>
                        </#if>
                            <option value="ANY">${uiLabelMap.CommonAnyStateProvince}</option>
                            ${screens.render("component://common/widget/CommonScreens.xml#states")}
                        </select>
                    </td>
                </tr>
                <tr><td class="label">${uiLabelMap.PartyPostalCode}</td>
                    <td><input type="text" name="postalCode" value="${parameters.postalCode!}"/></td>
                </tr>
            </#if>
            <#if extInfo == "T">
                <tr><td colspan="3"><hr /></td></tr>
                <tr><td class="label">${uiLabelMap.CommonCountryCode}</td>
                    <td><input type="text" name="countryCode" value="${parameters.countryCode!}"/></td>
                </tr>
                <tr><td class="label">${uiLabelMap.PartyAreaCode}</td>
                    <td><input type="text" name="areaCode" value="${parameters.areaCode!}"/></td>
                </tr>
                <tr><td class="label">${uiLabelMap.PartyContactNumber}</td>
                    <td><input type="text" name="contactNumber" value="${parameters.contactNumber!}"/></td>
                </tr>
            </#if>
            <#if extInfo == "O">
                <tr><td colspan="3"><hr /></td></tr>
                <tr><td class="label">${uiLabelMap.PartyContactInformation}</td>
                    <td><input type="text" name="infoString" value="${parameters.infoString!}"/></td>
                </tr>
            </#if>
                <tr><td colspan="3"><hr /></td></tr>
                <tr align="center">
                    <td>&nbsp;</td>
                    <td><input type="submit" value="${uiLabelMap.PartyLookupParty}"/>
                        <a href="<@ofbizUrl>findEmployees?roleTypeId=EMPLOYEE&amp;hideFields=Y&amp;lookupFlag=Y</@ofbizUrl>" class="smallSubmit">${uiLabelMap.CommonShowAllRecords}</a>
                    </td>
                </tr>
            </table>
        </form>
    </div>
    </#if>
</div>
    <#if parameters.hideFields?default("N") != "Y">
        <script type="application/javascript">
    <!--//
      document.lookupparty.partyId.focus();
    //-->
        </script>
    </#if>
    <#if partyList??>
    <br />
    <div id="findEmployeeResults" class="screenlet">
        <div class="screenlet-title-bar">
            <ul>
                <li class="h3">${uiLabelMap.PartyPartiesFound}</li>
                <#if (partyListSize > 0)>
                    <#if (partyListSize > highIndex)>
                        <li><a class="nav-next" href="<@ofbizUrl>findEmployees?VIEW_SIZE=${viewSize}&amp;VIEW_INDEX=${viewIndex-1}&amp;hideFields=${parameters.hideFields?default("N")}${paramList}</@ofbizUrl>">${uiLabelMap.CommonNext}</a></li>
                    <#else>
                        <li class="disabled">${uiLabelMap.CommonNext}</li>
                    </#if>
                    <li>${lowIndex} - ${highIndex} ${uiLabelMap.CommonOf} ${partyListSize}</li>
                    <#if (viewIndex > 0)>
                        <li><a class="nav-previous" href="<@ofbizUrl>findEmployees?VIEW_SIZE=${viewSize}&amp;VIEW_INDEX=${viewIndex-1}&amp;hideFields=${parameters.hideFields?default("N")}${paramList}</@ofbizUrl>">${uiLabelMap.CommonPrevious}</a></li>
                    <#else>
                        <li class="disabled">${uiLabelMap.CommonPrevious}</li>
                    </#if>
                </#if>
            </ul>
            <br class="clear"/>
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
            <#assign alt_row = false>
            <#list partyList as partyRow>
            <#assign partyType = partyRow.getRelatedOne("PartyType", false)!>
            <tr valign="middle"<#if alt_row> class="alternate-row"</#if>>
                <td><a href="<@ofbizUrl>EmployeeProfile?partyId=${partyRow.partyId}</@ofbizUrl>">${partyRow.partyId}</a></td>
                <td><#if partyRow.containsKey("userLoginId")>
                        ${partyRow.userLoginId?default("N/A")}
                    <#else>
                    <#assign userLogins = partyRow.getRelated("UserLogin", null, null, false)>
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
                <td><#if partyRow.getModelEntity().isField("lastName") && lastName?has_content>
                        ${partyRow.lastName}<#if partyRow.firstName?has_content>, ${partyRow.firstName}</#if>
                    <#elseif partyRow.getModelEntity().isField("groupName") && partyRow.groupName?has_content>
                        ${partyRow.groupName}
                    <#else>
                    <#assign partyName = Static["org.apache.ofbiz.party.party.PartyHelper"].getPartyName(partyRow, true)>
                    <#if partyName?has_content>
                        ${partyName}
                    <#else>
                        (${uiLabelMap.PartyNoNameFound})
                    </#if>
                    </#if>
                </td>
                <#if extInfo?default("") == "T">
                    <td>${partyRow.areaCode!}</td>
                </#if>
                <#if extInfo?default("") == "P" >
                    <td>${partyRow.city!}, ${partyRow.stateProvinceGeoId!}</td>
                </#if>
                <#if extInfo?default("") == "P">
                    <td>${partyRow.postalCode!}</td>
                </#if>
                <td><#if partyType.description??>${partyType.get("description", locale)}<#else>???</#if></td>
                <td class="button-col align-float">
                    <a href="<@ofbizUrl>EmployeeProfile?partyId=${partyRow.partyId}</@ofbizUrl>">${uiLabelMap.CommonDetails}</a>
                </td>
            </tr>
          <#-- toggle the row color -->
            <#assign alt_row = !alt_row>
            </#list>
        </table>
    <#else>
        <div class="screenlet-body">
            <span class="h3">${uiLabelMap.PartyNoPartiesFound}</span>
        </div>
    </#if>
    <#if lookupErrorMessage??>
        <div><h3>${lookupErrorMessage}</h3></div>
    </#if>
        <div>&nbsp;</div>
    </div>
    </#if>
<!-- end findEmployees.ftl -->
