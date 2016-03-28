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

<#if security.hasEntityPermission("ORDERMGR", "_CREATE", session) || security.hasEntityPermission("ORDERMGR", "_PURCHASE_CREATE", session)>
  <div class="screenlet-body">
      <table width="100%" border="0" cellspacing="0" cellpadding="0" class="boxbottom">
        <form name="checkoutsetupform" method="post" action="<@ofbizUrl>createCustomer</@ofbizUrl>">
        <input type="hidden" name="finalizeMode" value="cust" />
        <input type="hidden" name="finalizeReqNewShipAddress" value="true" />
        <tr>
          <td>
            <table width="100%" border="0" cellpadding="1" cellspacing="0">
              <tr>
                <td width="26%" align="right"><div>${uiLabelMap.CommonTitle}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" name="personalTitle" value="${requestParameters.personalTitle!}" size="10" maxlength="30"/>
                </td>
              </tr>
              <tr>
                <td width="26%" align="right"><div>${uiLabelMap.PartyFirstName}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" name="firstName" value="${requestParameters.firstName!}" size="30" maxlength="30"/>
                *</td>
              </tr>
              <tr>
                <td width="26%" align="right"><div>${uiLabelMap.PartyMiddleInitial}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" name="middleName" value="${requestParameters.middleName!}" size="4" maxlength="4"/>
                </td>
              </tr>
              <tr>
                <td width="26%" align="right"><div>${uiLabelMap.PartyLastName}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" name="lastName" value="${requestParameters.lastName!}" size="30" maxlength="30"/>
                *</td>
              </tr>
              <tr>
                <td width="26%" align="right"><div>${uiLabelMap.PartySuffix}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" name="suffix" value="${requestParameters.suffix!}" size="10" maxlength="30"/>
                </td>
              </tr>
              <tr>
                <td colspan="3">&nbsp;</td>
              </tr>
              <tr>
                <td width="26%" align="right"><div>${uiLabelMap.PartyHomePhone}<br/>${uiLabelMap.OrderAllowSolicitation}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" name="homeCountryCode" value="${requestParameters.homeCountryCode!}" size="4" maxlength="10"/>
                  -&nbsp;<input type="text" name="homeAreaCode" value="${requestParameters.homeAreaCode!}" size="4" maxlength="10"/>*
                  -&nbsp;<input type="text" name="homeContactNumber" value="${requestParameters.homeContactNumber!}" size="15" maxlength="15"/>*
                  &nbsp;ext&nbsp;<input type="text" name="homeExt" value="${requestParameters.homeExt!}" size="6" maxlength="10"/>
                  <br/>
                  <select name="homeSol">
                    <#if (((requestParameters.homeSol)!"") == "Y")><option value="Y">${uiLabelMap.CommonY}</option></#if>
                    <#if (((requestParameters.homeSol)!"") == "N")><option value="N">${uiLabelMap.CommonN}</option></#if>
                    <option></option>
                    <option value="Y">${uiLabelMap.CommonY}</option>
                    <option value="N">${uiLabelMap.CommonN}</option>
                  </select>
                </td>
              </tr>
              <tr>
                <td width="26%" align="right"><div>${uiLabelMap.PartyBusinessPhone}<br/>${uiLabelMap.OrderAllowSolicitation}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" name="workCountryCode" value="${requestParameters.CUSTOMER_WORK_COUNTRY!}" size="4" maxlength="10"/>
                  -&nbsp;<input type="text" name="workAreaCode" value="${requestParameters.CUSTOMER_WORK_AREA!}" size="4" maxlength="10"/>
                  -&nbsp;<input type="text" name="workContactNumber" value="${requestParameters.CUSTOMER_WORK_CONTACT!}" size="15" maxlength="15"/>
                  &nbsp;ext&nbsp;<input type="text" name="workExt" value="${requestParameters.CUSTOMER_WORK_EXT!}" size="6" maxlength="10"/>
                  <br/>
                  <select name="workSol">
                    <#if (((requestParameters.workSol)!"") == "Y")><option value="Y">${uiLabelMap.CommonY}</option></#if>
                    <#if (((requestParameters.workSol)!"") == "N")><option value="N">${uiLabelMap.CommonN}</option></#if>
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
                <td width="26%" align="right"><div>${uiLabelMap.PartyEmailAddress}<br/>${uiLabelMap.OrderAllowSolicitation}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" name="emailAddress" value="" size="60" maxlength="255" />
                  <br/>
                  <select name="emailSol">
                    <#if (((requestParameters.emailSol)!"") == "Y")><option value="Y">${uiLabelMap.CommonY}</option></#if>
                    <#if (((requestParameters.emailSol)!"") == "N")><option value="N">${uiLabelMap.CommonN}</option></#if>
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
                <td width="26%" align="right"><div>${uiLabelMap.CommonUsername}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" name="userLoginId" value="${requestParameters.USERNAME!}" size="20" maxlength="250"/>
                </td>
              </tr>
              </form>
            </table>
          </td>
        </tr>
      </table>
   </div>
<br />
<#else>
  <h3>${uiLabelMap.OrderViewPermissionError}</h3>
</#if>
