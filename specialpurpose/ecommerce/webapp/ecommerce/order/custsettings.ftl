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
    <div class="screenlet-header">
        <div class="boxhead">&nbsp;${uiLabelMap.PartyBasicInformation}</div>
    </div>
    <div class="screenlet-body">
        <form name="${parameters.formNameValue}" method="post" action="<@ofbizUrl>processCustomerSettings</@ofbizUrl>">
           <input type="hidden" name="partyId" value="${parameters.partyId?if_exists}"/>
           <table width="100%" border="0" cellpadding="1" cellspacing="0">
              <tr>
                <td width="26%" align="right"><div class="tabletext">${uiLabelMap.CommonTitle}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" name="personalTitle" value="${parameters.personalTitle?if_exists}" size="10" maxlength="30"/>
                </td>
              </tr>
              <tr>
                <td width="26%" align="right"><div class="tabletext">${uiLabelMap.PartyFirstName}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" name="firstName" value="${parameters.firstName?if_exists}" size="30" maxlength="30"/>
                *</td>
              </tr>
              <tr>
                <td width="26%" align="right"><div class="tabletext">${uiLabelMap.PartyMiddleInitial}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" name="middleName" value="${parameters.middleName?if_exists}" size="4" maxlength="4"/>
                </td>
              </tr>
              <tr>
                <td width="26%" align="right"><div class="tabletext">${uiLabelMap.PartyLastName} </div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" name="lastName" value="${parameters.lastName?if_exists}" size="30" maxlength="30"/>
                *</td>
              </tr>
              <tr>
                <td width="26%" align="right"><div class="tabletext">${uiLabelMap.PartySuffix}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" name="suffix" value="${parameters.suffix?if_exists}" size="10" maxlength="30"/>
                </td>
              </tr>
              <tr>
                <td colspan="3">&nbsp;</td>
              </tr>
              <tr>
                <td width="26%" align="right"><div class="tabletext">${uiLabelMap.PartyHomePhone}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="hidden" name="homePhoneContactMechId" value="${parameters.homePhoneContactMechId?if_exists}"/>
                  <input type="text" class="inputBox" name="homeCountryCode" value="${parameters.homeCountryCode?if_exists}" size="4" maxlength="10"/>
                  -&nbsp;<input type="text" class="inputBox" name="homeAreaCode" value="${parameters.homeAreaCode?if_exists}" size="4" maxlength="10"/>
                  -&nbsp;<input type="text" class="inputBox" name="homeContactNumber" value="${parameters.homeContactNumber?if_exists}" size="15" maxlength="15"/>
                  -&nbsp;<input type="text" class='inputBox' name="homeExt" value="${parameters.homeExt?if_exists}" size="6" maxlength="10"/> *
                </td>
              </tr>
              <tr>
                <td width="26%" align="right" valign="top"><div class="tabletext"></div></td>
                <td width="5">&nbsp;</td>
	  		    <td width="74%"><div class="tabletext">[${uiLabelMap.PartyCountryCode}] [${uiLabelMap.PartyAreaCode}] [${uiLabelMap.PartyContactNumber}] [${uiLabelMap.PartyExtension}]</div></td>
	  		  </tr>
              <tr>
                <td width="26%" align="right"><div class="tabletext">(${uiLabelMap.PartyAllowSolicitation}?)</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                <select name="homeSol" class="selectBox">
                    <#if (((parameters.homeSol)!"") == "Y")><option value="Y">${uiLabelMap.CommonY}</option></#if>
                    <#if (((parameters.homeSol)!"") == "N")><option value="N">${uiLabelMap.CommonN}</option></#if>
                    <option></option>
                    <option value="Y">${uiLabelMap.CommonY}</option>
                    <option value="N">${uiLabelMap.CommonN}</option>
                  </select>
                </td>
              </tr>
              <tr>
                <td colspan="3">&nbsp;</td>
              </tr>
              <tr>
                <td width="26%" align="right"><div class="tabletext">${uiLabelMap.PartyBusinessPhone}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="hidden" name="workPhoneContactMechId" value="${parameters.workPhoneContactMechId?if_exists}"/>
                  <input type="text" class="inputBox" name="workCountryCode" value="${parameters.workCountryCode?if_exists}" size="4" maxlength="10"/>
                  -&nbsp;<input type="text" class="inputBox" name="workAreaCode" value="${parameters.workAreaCode?if_exists}" size="4" maxlength="10"/>
                  -&nbsp;<input type="text" class="inputBox" name="workContactNumber" value="${parameters.workContactNumber?if_exists}" size="15" maxlength="15"/>
                  -&nbsp;<input type="text" class='inputBox' name="workExt" value="${parameters.workExt?if_exists}" size="6" maxlength="10"/>
                </td>
              </tr>
              <tr>
                <td width="26%" align="right" valign="top"><div class="tabletext"></div></td>
                <td width="5">&nbsp;</td>
	  		    <td width="74%"><div class="tabletext">[${uiLabelMap.PartyCountryCode}] [${uiLabelMap.PartyAreaCode}] [${uiLabelMap.PartyContactNumber}] [${uiLabelMap.PartyExtension}]</div></td>
	  		  </tr>
              <tr>
                <td width="26%" align="right"><div class="tabletext">${uiLabelMap.PartyEmailAddress}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="hidden" name="emailContactMechId" value="${parameters.emailContactMechId?if_exists}"/>
                  <input type="text" class="inputBox" name="emailAddress" value="${parameters.emailAddress?if_exists}" size="60" maxlength="255"/> *
                </td>
              </tr>
              <tr>
                <td width="26%" align="right"><div class="tabletext">(${uiLabelMap.PartyAllowSolicitation}?)</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <select name="emailSol" class="selectBox">
                    <#if (((parameters.emailSol)!"") == "Y")><option value="Y">${uiLabelMap.CommonY}</option></#if>
                    <#if (((parameters.emailSol)!"") == "N")><option value="N">${uiLabelMap.CommonN}</option></#if>
                    <option></option>
                    <option value="Y">${uiLabelMap.CommonY}</option>
                    <option value="N">${uiLabelMap.CommonN}</option>
                  </select>
                </td>
              </tr>
              <tr>
                <td colspan="3">&nbsp;</td>
              </tr>
              <tr>
                <td colspan="3" align="center"><input type="submit" class="smallsubmit" value="${uiLabelMap.CommonContinue}"/></td>
              </tr>
            </table>
        </form>
    </div>
</div>
