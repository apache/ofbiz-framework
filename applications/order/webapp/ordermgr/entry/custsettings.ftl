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
<table border="0" width="100%" cellspacing="0" cellpadding="0" class="boxoutside">
  <tr>
    <td width="100%">
      <table width="100%" border="0" cellspacing="0" cellpadding="0" class="boxtop">
        <tr>
          <td align="left">
            <div class="boxhead">&nbsp;${uiLabelMap.OrderOrderEntryCustomerInfo}</div>
          </td> 
          <td nowrap align="right">
            <div class="tabletext">
              <a href="<@ofbizUrl>orderentry</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderOrderItems}</a>

              <a href="<@ofbizUrl>setCustomer</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonRefresh}</a>
              <a href="javascript:document.checkoutsetupform.submit();" class="buttontext">${uiLabelMap.CommonContinue}</a>
            </div>
          </td>         
        </tr>
      </table>
    </td>
  </tr>
  <tr>
    <td width="100%">
      <table width="100%" border="0" cellspacing="0" cellpadding="0" class="boxbottom">
        <form name="checkoutsetupform" method="post" action="<@ofbizUrl>createCustomer</@ofbizUrl>">
        <input type="hidden" name="finalizeMode" value="cust">
        <input type="hidden" name="finalizeReqNewShipAddress" value="true">
        <tr>
          <td>
            <table width="100%" border="0" cellpadding="1" cellspacing="0">              
              <tr>
                <td width="26%" align="right"><div class="tabletext">${uiLabelMap.CommonTitle}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" name="personalTitle" value="${requestParameters.personalTitle?if_exists}" size="10" maxlength="30"/>
                </td>
              </tr>
              <tr>
                <td width="26%" align="right"><div class="tabletext">${uiLabelMap.PartyFirstName}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" name="firstName" value="${requestParameters.firstName?if_exists}" size="30" maxlength="30"/>
                *</td>
              </tr>
              <tr>
                <td width="26%" align="right"><div class="tabletext">${uiLabelMap.PartyMiddleInitial}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" name="middleName" value="${requestParameters.middleName?if_exists}" size="4" maxlength="4"/>
                </td>
              </tr>
              <tr>
                <td width="26%" align="right"><div class="tabletext">${uiLabelMap.PartyLastName}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" name="lastName" value="${requestParameters.lastName?if_exists}" size="30" maxlength="30"/>
                *</td>
              </tr>
              <tr>
                <td width="26%" align="right"><div class="tabletext">${uiLabelMap.PartySuffix}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" name="suffix" value="${requestParameters.suffix?if_exists}" size="10" maxlength="30"/>
                </td>
              </tr>                          
              <tr>
                <td colspan="3">&nbsp;</td>
              </tr>              
              <tr>
                <td width="26%" align="right"><div class="tabletext">${uiLabelMap.PartyHomePhone}<BR>${uiLabelMap.CommonAllowSolicitation}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" name="homeCountryCode" value="${requestParameters.homeCountryCode?if_exists}" size="4" maxlength="10"/>
                  -&nbsp;<input type="text" class="inputBox" name="homeAreaCode" value="${requestParameters.homeAreaCode?if_exists}" size="4" maxlength="10"/>*
                  -&nbsp;<input type="text" class="inputBox" name="homeContactNumber" value="${requestParameters.homeContactNumber?if_exists}" size="15" maxlength="15"/>*
                  &nbsp;ext&nbsp;<input type="text" class="inputBox" name="homeExt" value="${requestParameters.homeExt?if_exists}" size="6" maxlength="10"/>
                  <BR>
                  <select name="homeSol" class="selectBox">
                    <option>${requestParameters.homeSol?default("Y")}</option>
                    <option></option><option value="Y">${uiLabelMap.CommonY}</option><option value="N">${uiLabelMap.CommonN}</option>
                  </select>
                </td>
              </tr>
              <tr>
                <td width="26%" align="right"><div class="tabletext">${uiLabelMap.PartyBusinessPhone}<BR>${uiLabelMap.CommonAllowSolicitation}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" name="workCountryCode" value="${requestParameters.CUSTOMER_WORK_COUNTRY?if_exists}" size="4" maxlength="10"/>
                  -&nbsp;<input type="text" class="inputBox" name="workAreaCode" value="${requestParameters.CUSTOMER_WORK_AREA?if_exists}" size="4" maxlength="10"/>
                  -&nbsp;<input type="text" class="inputBox" name="workContactNumber" value="${requestParameters.CUSTOMER_WORK_CONTACT?if_exists}" size="15" maxlength="15"/>
                  &nbsp;ext&nbsp;<input type="text" class="inputBox" name="workExt" value="${requestParameters.CUSTOMER_WORK_EXT?if_exists}" size="6" maxlength="10"/>
                  <BR>
                  <select name="workSol" class="selectBox">
                    <option>${requestParameters.workSol?default("Y")}</option>
                    <option></option><option value="Y">${uiLabelMap.CommonY}</option><option value="N">${uiLabelMap.CommonN}</option>
                  </select>
                </td>
              </tr>                            
              <tr>
                <td colspan="3">&nbsp;</td>
              </tr>                            
              <tr>
                <td width="26%" align="right"><div class="tabletext">${uiLabelMap.PartyEmailAddress}<BR>${uiLabelMap.CommonAllowSolicitation}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" name="emailAddress" value="" size="60" maxlength="255"> 
                  <BR>
                  <select name="emailSol" class="selectBox">
                    <option>${requestParameters.emailSol?default("Y")}</option>
                    <option></option><option value="Y">${uiLabelMap.CommonY}</option><option value="N">${uiLabelMap.CommonN}</option>
                  </select>
                </td>
              </tr>               
              <tr>
                <td colspan="3">&nbsp;</td>
              </tr>              
              <tr>
                <td width="26%" align="right"><div class="tabletext">${uiLabelMap.CommonUsername}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" name="userLoginId" value="${requestParameters.USERNAME?if_exists}" size="20" maxlength="250"/>
                </td>
              </tr> 
              <#--  
              <tr>
                <td width="26%" align="right"><div class="tabletext">${uiLabelMap.OrderPassword}</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="password" class="inputBox" name="PASSWORD" value="" size="20" maxlength="50">
                *</td>
              </tr>
              <tr>
                <td width="26%" align="right"><div class="tabletext">${uiLabelMap.OrderConfirm} Password</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="password" class="inputBox" name="CONFIRM_PASSWORD" value="" size="20" maxlength="50">
                *</td>
              </tr>
              <tr>
                <td width="26%" align="right"><div class="tabletext">${uiLabelMap.OrderPassword} Hint</div></td>
                <td width="5">&nbsp;</td>
                <td width="74%">
                  <input type="text" class="inputBox" name="PASSWORD_HINT" value="${requestParameters.PASSWORD_HINT?if_exists}" size="40" maxlength="100">
                </td>
              </tr> 
              --> 
              </form>                                       
            </table>        
          </td>
        </tr>
      </table>
    </td>
  </tr>
</table>

<br/>
<#else>
  <h3>${uiLabelMap.OrderViewPermissionError}</h3>
</#if>
