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


<#macro fieldErrors fieldName>
  <#if errorMessageList?has_content>
    <#assign fieldMessages = Static["org.ofbiz.base.util.MessageString"].getMessagesForField(fieldName, true, errorMessageList)>
    <ul>
      <#list fieldMessages as errorMsg>
        <li class="errorMessage">${errorMsg}</li>
      </#list>
    </ul>
  </#if>
</#macro>
<#macro fieldErrorsMulti fieldName1 fieldName2 fieldName3 fieldName4>
  <#if errorMessageList?has_content>
    <#assign fieldMessages = Static["org.ofbiz.base.util.MessageString"].getMessagesForField(fieldName1, fieldName2, fieldName3, fieldName4, true, errorMessageList)>
    <ul>
      <#list fieldMessages as errorMsg>
        <li class="errorMessage">${errorMsg}</li>
      </#list>
    </ul>
  </#if>
</#macro>

<div class="screenlet">
  <div class="screenlet-title-bar">
     <div class="h3">${uiLabelMap.PartyBasicInformation}</div>
  </div>
  <div class="screenlet-body">
  <form name="${parameters.formNameValue}" id="quickAnonProcessCustomer" method="post" action="<@ofbizUrl>quickAnonProcessCustomerSettings</@ofbizUrl>">
  <input type="hidden" name="partyId" value="${parameters.partyId?if_exists}"/>
  <input type="hidden" name="shippingContactMechId" value="${parameters.shippingContactMechId?if_exists}"/>
  <input type="hidden" name="billingContactMechId" value="${parameters.billingContactMechId?if_exists}"/>
  <input type="hidden" name="shippingContactMechPurposeTypeId" value="${parameters.shippingContactMechPurposeTypeId?if_exists}"/>
  <input type="hidden" name="billingContactMechPurposeTypeId" value="${parameters.billingContactMechPurposeTypeId?if_exists}"/>

  <table width="100%" border="0" cellpadding="1" cellspacing="0">
     <tr>
        <td width="50%">
           <table width="100%" border="0" cellpadding="1" cellspacing="0">
              <tr>
                 <td width="26%" align="right" valign="top"></td>
                 <td width="2%">&nbsp;</td>
                 <td width="72%">&nbsp;</td>
              </tr>
              <tr>
                 <td width="26%" align="right" valign="top"><div class="tableheadtext">${uiLabelMap.PartyNameAndConactInfo}</div></td>
                 <td width="2%">&nbsp;</td>
                 <td width="72%">&nbsp;</td>
              </tr>
              <tr>
                <td width="26%" align="right"><div class="tabletext">${uiLabelMap.PartyFirstName}</div></td>
                <td width="2%">&nbsp;</td>
                <td width="72%">
                  <@fieldErrors fieldName="firstName"/>
                    <input type="text" class="inputBox required" name="firstName" id="firstName" value="${parameters.firstName?if_exists}" size="30" maxlength="30"/>*<span id="advice-required-firstName" class="required" style="display:none">(${uiLabelMap.CommonRequired})</span>
                </td>
              </tr>
              <tr>
                <td width="26%" align="right"><div class="tabletext">${uiLabelMap.PartyMiddleInitial}</div></td>
                <td width="2%">&nbsp;</td>
                <td width="72%">
                  <input type="text" class="inputBox"  name="middleName" value="${parameters.middleName?if_exists}" size="4" maxlength="4"/>
                </td>
              </tr>
              <tr>
                <td width="26%" align="right"><div class="tabletext">${uiLabelMap.PartyLastName} </div></td>
                <td width="2%">&nbsp;</td>
                <td width="72%">
                  <@fieldErrors fieldName="lastName"/>
                  <input type="text" class="inputBox required" name="lastName" value="${parameters.lastName?if_exists}" size="30" maxlength="30"/>*<span id="advice-required-lastName" class="required" style="display:none">(${uiLabelMap.CommonRequired})</span>
                </td>
              </tr>
              <tr>
                <td width="26%" align="right" valign="top"><div class="tabletext"></div></td>
                <td width="2%">&nbsp;</td>
                <td width="72%"><div class="tabletext">[${uiLabelMap.PartyCountryCode}] [${uiLabelMap.PartyAreaCode}] [${uiLabelMap.PartyContactNumber}] [${uiLabelMap.PartyExtension}]</div></td>
              </tr>
              <tr>
                <td width="10%" align="right"><div class="tabletext">${uiLabelMap.PartyHomePhone}</div></td>
                <td width="2%">&nbsp;</td>
                <td width="88%">
                  <@fieldErrorsMulti fieldName1="homeCountryCode" fieldName2="homeAreaCode" fieldName3="homeContactNumber" fieldName4="homeExt"/>
                  <div>
                    <input type="hidden" name="homePhoneContactMechId" value="${parameters.homePhoneContactMechId?if_exists}"/>
                    <input type="text" class="inputBox required" name="homeCountryCode" value="${parameters.homeCountryCode?if_exists}" size="4" maxlength="10"/>
                    -&nbsp;<input type="text" class="inputBox required" name="homeAreaCode" value="${parameters.homeAreaCode?if_exists}" size="4" maxlength="10"/>
                    -&nbsp;<input type="text" class="inputBox required" name="homeContactNumber" value="${parameters.homeContactNumber?if_exists}" size="15" maxlength="15"/>
                    -&nbsp;<input type="text" class="inputBox" name="homeExt" value="${parameters.homeExt?if_exists}" size="6" maxlength="10"/> *
                  </div>
                </td>
              </tr>
              <tr>
                <td width="26%" align="right"><div class="tabletext">${uiLabelMap.PartyBusinessPhone}</div></td>
                <td width="2%">&nbsp;</td>
                <td width="72%">
                  <input type="hidden" name="workPhoneContactMechId" value="${parameters.workPhoneContactMechId?if_exists}"/>
                  <input type="text" class="inputBox" name="workCountryCode" value="${parameters.workCountryCode?if_exists}" size="4" maxlength="10"/>
                  -&nbsp;<input type="text" class="inputBox" name="workAreaCode" value="${parameters.workAreaCode?if_exists}" size="4" maxlength="10"/>
                  -&nbsp;<input type="text" class="inputBox" name="workContactNumber" value="${parameters.workContactNumber?if_exists}" size="15" maxlength="15"/>
                  -&nbsp;<input type="text" class='inputBox' name="workExt" value="${parameters.workExt?if_exists}" size="6" maxlength="10"/>
                </td>
              </tr>
              <tr>
                <td width="26%" align="right"><div class="tabletext">${uiLabelMap.PartyEmailAddress}</div></td>
                <td width="2%">&nbsp;</td>
                <td width="72%">
                  <@fieldErrors fieldName="emailAddress"/>
                  <input type="hidden" name="emailContactMechId" value="${parameters.emailContactMechId?if_exists}"/>
                  <input type="text" class="inputBox required validate-email" name="emailAddress" value="${parameters.emailAddress?if_exists}" size="40" maxlength="255"/> *
                </td>
              </tr>
            </table>
         </td>
     </tr>
     <tr>
        <td colspan="3" align="center"><hr /></td>
     </tr>
     <tr>
        <td width="50%">
           <table width="100%" border="0" cellpadding="1" cellspacing="0">
              <tr>
                 <td width="26%" align="right" valign="top"></td>
                 <td width="2%">&nbsp;</td>
                 <td width="72%">&nbsp;</td>
              </tr>
              <tr>
                 <td width="26%" align="right" valign="top"><div class="tableheadtext">${uiLabelMap.OrderShippingAddress}</div></td>
                 <td width="2%">&nbsp;</td>
                 <td width="72%">&nbsp;</td>
              </tr>
              <tr>
                <td width="26%" align="right"><div class="tabletext">${uiLabelMap.PartyToName}</div></td>
                <td width="2%">&nbsp;</td>
                <td width="72%">
                  <@fieldErrors fieldName="shipToName"/>
                  <input type="text" class="inputBox" name="shipToName" id="shipToName" value="${parameters.shipToName?if_exists}" size="30" maxlength="30"/>
                </td>
              </tr>
              <tr>
                <td width="26%" align="right"><div class="tabletext">${uiLabelMap.PartyAttentionName}</div></td>
                <td width="2%">&nbsp;</td>
                <td width="72%">
                  <@fieldErrors fieldName="shipToAttnName"/>
                  <input type="text" class="inputBox" id="shipToAttnName" name="shipToAttnName" value="${parameters.shipToAttnName?if_exists}" size="30" maxlength="30"/>
                </td>
              </tr>
              <tr>
                 <td width="26%" align="right" valign="middle"><div class="tabletext">${uiLabelMap.PartyAddressLine1}</div></td>
                 <td width="2%">&nbsp;</td>
                 <td width="72%">
                    <@fieldErrors fieldName="shipToAddress1"/>
                    <input type="text" class="inputBox required" size="30" maxlength="30" id="shipToAddress1" name="shipToAddress1" value="${parameters.shipToAddress1?if_exists}" />
                 *</td>
              </tr>
              <tr>
                 <td width="26%" align="right" valign="middle"><div class="tabletext">${uiLabelMap.PartyAddressLine2}</div></td>
                 <td width="2%">&nbsp;</td>
                 <td width="72%">
                    <input type="text" class="inputBox" size="30" maxlength="30" id="shipToAddress2" name="shipToAddress2" value="${parameters.shipToAddress2?if_exists}" />
                 </td>
              </tr>
              <tr>
                 <td width="26%" align="right" valign="middle"><div class="tabletext">${uiLabelMap.PartyCity}</div></td>
                 <td width="2%">&nbsp;</td>
                 <td width="72%">
                    <@fieldErrors fieldName="shipToCity"/>
                    <input type="text" class="inputBox required" size="30" maxlength="30" id="shipToCity" name="shipToCity" value="${parameters.shipToCity?if_exists}" />
                 *</td>
              </tr>
              <tr>
                 <td width="26%" align="right" valign="middle"><div class="tabletext">${uiLabelMap.PartyState}</div></td>
                 <td width="2%">&nbsp;</td>
                 <td width="72%">
                    <@fieldErrors fieldName="shipToStateProvinceGeoId"/>
                    <select name="shipToStateProvinceGeoId" id="shipToStateProvinceGeoId" class="selectBox">
                    <#if (parameters.shipToStateProvinceGeoId)?exists>
                       <option>${parameters.shipToStateProvinceGeoId}</option>
                       <option value="${parameters.shipToStateProvinceGeoId}">---</option>
                    <#else>
                       <option value="">${uiLabelMap.PartyNoState}</option>
                    </#if>
                       ${screens.render("component://common/widget/CommonScreens.xml#states")}
                    </select>
                 *</td>
              </tr>
              <tr>
                 <td width="26%" align="right" valign="middle"><div class="tabletext">${uiLabelMap.PartyZipCode}</div></td>
                 <td width="2%">&nbsp;</td>
                 <td width="72%">
                    <@fieldErrors fieldName="shipToPostalCode"/>
                    <input type="text" class="inputBox required" size="12" maxlength="10" id="shipToPostalCode" name="shipToPostalCode" value="${parameters.shipToPostalCode?if_exists}" />
                 *</td>
              </tr>
              <tr>
                 <td width="26%" align="right" valign="middle"><div class="tabletext">${uiLabelMap.CommonCountry}</div></td>
                 <td width="2%">&nbsp;</td>
                 <td width="72%">
                    <@fieldErrors fieldName="shipToCountryGeoId"/>
                    <select name="shipToCountryGeoId" id="shipToCountryGeoId" class="selectBox">
                    <#if (parameters.shipToCountryGeoId)?exists>
                       <option>${parameters.shipToCountryGeoId}</option>
                       <option value="${parameters.shipToCountryGeoId}">---</option>
                    </#if>
                       ${screens.render("component://common/widget/CommonScreens.xml#countries")}
                    </select>
                 *</td>
              </tr>
            </table>
         </td>

        <td width="50%">
           <table width="100%" border="0" cellpadding="1" cellspacing="0">
              <tr>
                <td align="center" valign="top" colspan="3">
                  <div class="tabletext">
                    <input type="checkbox" class="checkbox" id="useShippingPostalAddressForBilling" name="useShippingPostalAddressForBilling" value="Y"/>
                    ${uiLabelMap.FacilityBillingAddressSameShipping}
                  </div>
                </td>
              </tr>
              <tr>
                 <td width="26%" align="right" valign="top"><div class="tableheadtext">${uiLabelMap.PartyBillingAddress}</div></td>
                 <td width="2%">&nbsp;</td>
                 <td width="72%">&nbsp;</td>
              </tr>
              <tr>
                <td width="26%" align="right"><div class="tabletext">${uiLabelMap.PartyToName}</div></td>
                <td width="2%">&nbsp;</td>
                <td width="72%">
                  <@fieldErrors fieldName="billToName"/>
                  <input type="text" class="inputBox" id="billToName" name="billToName" value="${parameters.billToName?if_exists}" size="30" maxlength="30"/>
                </td>
              </tr>
              <tr>
                <td width="26%" align="right"><div class="tabletext">${uiLabelMap.PartyAttentionName}</div></td>
                <td width="2%">&nbsp;</td>
                <td width="72%">
                  <@fieldErrors fieldName="billToAttnName"/>
                  <input type="text" class="inputBox" id="billToAttnName" name="billToAttnName" value="${parameters.billToAttnName?if_exists}" size="30" maxlength="30"/>
                </td>
              </tr>
              <tr>
                 <td width="26%" align="right" valign="middle"><div class="tabletext">${uiLabelMap.PartyAddressLine1}</div></td>
                 <td width="2%">&nbsp;</td>
                 <td width="72%">
                    <@fieldErrors fieldName="billToAddress1"/>
                    <input type="text" class="inputBox required" id="billToAddress1" size="30" maxlength="30" name="billToAddress1" value="${parameters.billToAddress1?if_exists}" />
                 *</td>
              </tr>
              <tr>
                 <td width="26%" align="right" valign="middle"><div class="tabletext">${uiLabelMap.PartyAddressLine2}</div></td>
                 <td width="2%">&nbsp;</td>
                 <td width="72%">
                    <input type="text" class="inputBox" id="billToAddress2" size="30" maxlength="30" name="billToAddress2" value="${parameters.billToAddress2?if_exists}" />
                 </td>
              </tr>
              <tr>
                 <td width="26%" align="right" valign="middle"><div class="tabletext">${uiLabelMap.PartyCity}</div></td>
                 <td width="2%">&nbsp;</td>
                 <td width="72%">
                    <@fieldErrors fieldName="billToCity"/>
                    <input type="text" class="inputBox required" id="billToCity" size="30" maxlength="30" name="billToCity" value="${parameters.billToCity?if_exists}" />
                 *</td>
              </tr>
              <tr>
                 <td width="26%" align="right" valign="middle"><div class="tabletext">${uiLabelMap.PartyState}</div></td>
                 <td width="2%">&nbsp;</td>
                 <td width="72%">
                    <@fieldErrors fieldName="billToStateProvinceGeoId"/>
                    <select name="billToStateProvinceGeoId" id="billToStateProvinceGeoId" class="selectBox">
                    <#if (parameters.billToStateProvinceGeoId)?exists>
                       <option>${parameters.billToStateProvinceGeoId}</option>
                       <option value="${parameters.billToStateProvinceGeoId}">---</option>
                    <#else>
                       <option value="">${uiLabelMap.PartyNoState}</option>
                    </#if>
                       ${screens.render("component://common/widget/CommonScreens.xml#states")}
                    </select>
                 *</td>
              </tr>
              <tr>
                 <td width="26%" align="right" valign="middle"><div class="tabletext">${uiLabelMap.PartyZipCode}</div></td>
                 <td width="2%">&nbsp;</td>
                 <td width="72%">
                    <@fieldErrors fieldName="billToPostalCode"/>
                    <input type="text" class="inputBox required" size="12" maxlength="10" id="billToPostalCode" name="billToPostalCode" value="${parameters.billToPostalCode?if_exists}" />
                 *</td>
              </tr>
              <tr>
                 <td width="26%" align="right" valign="middle"><div class="tabletext">${uiLabelMap.CommonCountry}</div></td>
                 <td width="2%">&nbsp;</td>
                 <td width="72%">
                    <@fieldErrors fieldName="billToCountryGeoId"/>
                    <select name="billToCountryGeoId" id="billToCountryGeoId" class="selectBox">
                    <#if (parameters.billToCountryGeoId)?exists>
                       <option>${parameters.billToCountryGeoId}</option>
                       <option value="${parameters.billToCountryGeoId}">---</option>
                    </#if>
                       ${screens.render("component://common/widget/CommonScreens.xml#countries")}
                    </select>
                 *</td>
              </tr>
            </table>
         </td>
      </tr>
      <tr>
         <td colspan="3" align="center">&nbsp;</td>
      </tr>
      <tr>
         <td colspan="3" align="center"><input type="submit" class="smallsubmit" value="${uiLabelMap.CommonContinue}"/></td>
      </tr>
  </table>
  </form>
  </div>
</div>
