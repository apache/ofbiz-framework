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
<script language="JavaScript" type="text/javascript">
function shipBillAddr() {
    if (document.${parameters.formNameValue}.useShippingPostalAddressForBilling.checked) {
       document.${parameters.formNameValue}.billToName.value = document.${parameters.formNameValue}.shipToName.value;
       document.${parameters.formNameValue}.billToAttnName.value = document.${parameters.formNameValue}.shipToAttnName.value;
       document.${parameters.formNameValue}.billToAddress1.value = document.${parameters.formNameValue}.shipToAddress1.value;
       document.${parameters.formNameValue}.billToAddress2.value = document.${parameters.formNameValue}.shipToAddress2.value;
       document.${parameters.formNameValue}.billToCity.value = document.${parameters.formNameValue}.shipToCity.value;
       document.${parameters.formNameValue}.billToStateProvinceGeoId.value = document.${parameters.formNameValue}.shipToStateProvinceGeoId.value;
       document.${parameters.formNameValue}.billToPostalCode.value = document.${parameters.formNameValue}.shipToPostalCode.value;
       document.${parameters.formNameValue}.billToCountryGeoId.value = document.${parameters.formNameValue}.shipToCountryGeoId.value;
       
       document.${parameters.formNameValue}.billToName.disabled = true;
       document.${parameters.formNameValue}.billToAttnName.disabled = true;
       document.${parameters.formNameValue}.billToAddress1.disabled = true;
       document.${parameters.formNameValue}.billToAddress2.disabled = true;
       document.${parameters.formNameValue}.billToCity.disabled = true;
       document.${parameters.formNameValue}.billToStateProvinceGeoId.disabled = true;                                   
       document.${parameters.formNameValue}.billToPostalCode.disabled = true;
       document.${parameters.formNameValue}.billToCountryGeoId.disabled = true;                                   
    } else {
       document.${parameters.formNameValue}.billToName.disabled = false;
       document.${parameters.formNameValue}.billToAttnName.disabled = false;
       document.${parameters.formNameValue}.billToAddress1.disabled = false;
       document.${parameters.formNameValue}.billToAddress2.disabled = false;
       document.${parameters.formNameValue}.billToCity.disabled = false;
       document.${parameters.formNameValue}.billToStateProvinceGeoId.disabled = false;                                   
       document.${parameters.formNameValue}.billToPostalCode.disabled = false;
       document.${parameters.formNameValue}.billToCountryGeoId.disabled = false;                                   
       document.${parameters.formNameValue}.billingContactMechId.value = "";  
    }
}
</script>

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
  <div class="screenlet-header">
     <div class="boxhead">&nbsp;${uiLabelMap.PartyBasicInformation}</div>
  </div>
  <div class="screenlet-body">
  <form name="${parameters.formNameValue}" method="post" action="<@ofbizUrl>quickAnonProcessCustomerSettings</@ofbizUrl>">
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
                  <input type="text" class="inputBox" name="firstName" value="${parameters.firstName?if_exists}" size="30" maxlength="30"/>
                *</td>
              </tr>
              <tr>
                <td width="26%" align="right"><div class="tabletext">${uiLabelMap.PartyMiddleInitial}</div></td>
                <td width="2%">&nbsp;</td>
                <td width="72%">
                  <input type="text" class="inputBox" name="middleName" value="${parameters.middleName?if_exists}" size="4" maxlength="4"/>
                </td>
              </tr>
              <tr>
                <td width="26%" align="right"><div class="tabletext">${uiLabelMap.PartyLastName} </div></td>
                <td width="2%">&nbsp;</td>
                <td width="72%">
                  <@fieldErrors fieldName="lastName"/>
                  <input type="text" class="inputBox" name="lastName" value="${parameters.lastName?if_exists}" size="30" maxlength="30"/>
                *</td>
              </tr>
              <tr>
                <td width="26%" align="right" valign="top"><div class="tabletext"></div></td>
                <td width="2%">&nbsp;</td>
                <td width="72%"><div class="tabletext">[${uiLabelMap.PartyCountryCode}] [${uiLabelMap.PartyAreaCode}] [${uiLabelMap.PartyContactNumber}] [${uiLabelMap.PartyExtension}]</div></td>
              </tr>
              <tr>
                <td width="26%" align="right"><div class="tabletext">${uiLabelMap.PartyHomePhone}</div></td>
                <td width="2%">&nbsp;</td>
                <td width="72%">
                  <@fieldErrorsMulti fieldName1="homeCountryCode" fieldName2="homeAreaCode" fieldName3="homeContactNumber" fieldName4="homeExt"/>
                  <input type="hidden" name="homePhoneContactMechId" value="${parameters.homePhoneContactMechId?if_exists}"/>
                  <input type="text" class="inputBox" name="homeCountryCode" value="${parameters.homeCountryCode?if_exists}" size="4" maxlength="10"/>
                  -&nbsp;<input type="text" class="inputBox" name="homeAreaCode" value="${parameters.homeAreaCode?if_exists}" size="4" maxlength="10"/>
                  -&nbsp;<input type="text" class="inputBox" name="homeContactNumber" value="${parameters.homeContactNumber?if_exists}" size="15" maxlength="15"/>
                  -&nbsp;<input type="text" class='inputBox' name="homeExt" value="${parameters.homeExt?if_exists}" size="6" maxlength="10"/> *
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
                  <input type="text" class="inputBox" name="emailAddress" value="${parameters.emailAddress?if_exists}" size="40" maxlength="255"/> *
                </td>
              </tr>
            </table>
         </td>
     </tr>
     <tr>
        <td colspan="3" align="center"><hr class="sepbar"/></td>
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
                  <input type="text" class="inputBox" name="shipToName" value="${parameters.shipToName?if_exists}" size="30" maxlength="30"/>
                </td>
              </tr>
              <tr>
                <td width="26%" align="right"><div class="tabletext">${uiLabelMap.PartyAttentionName}</div></td>
                <td width="2%">&nbsp;</td>
                <td width="72%">
                  <@fieldErrors fieldName="shipToAttnName"/>
                  <input type="text" class="inputBox" name="shipToAttnName" value="${parameters.shipToAttnName?if_exists}" size="30" maxlength="30"/>
                </td>
              </tr>
              <tr>
                 <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.PartyAddressLine1}</div></td>
                 <td width="2%">&nbsp;</td>
                 <td width="72%">
                    <@fieldErrors fieldName="shipToAddress1"/>
                    <input type="text" class="inputBox" size="30" maxlength="30" name="shipToAddress1" value="${parameters.shipToAddress1?if_exists}">
                 *</td>
              </tr>
              <tr>
                 <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.PartyAddressLine2}</div></td>
                 <td width="2%">&nbsp;</td>
                 <td width="72%">
                    <input type="text" class="inputBox" size="30" maxlength="30" name="shipToAddress2" value="${parameters.shipToAddress2?if_exists}">
                 </td>
              </tr>
              <tr>
                 <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.PartyCity}</div></td>
                 <td width="2%">&nbsp;</td>
                 <td width="72%">
                    <@fieldErrors fieldName="shipToCity"/>
                    <input type="text" class="inputBox" size="30" maxlength="30" name="shipToCity" value="${parameters.shipToCity?if_exists}">
                 *</td>
              </tr>
              <tr>
                 <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.PartyState}</div></td>
                 <td width="2%">&nbsp;</td>
                 <td width="72%">
                    <@fieldErrors fieldName="shipToStateProvinceGeoId"/>
                    <select name="shipToStateProvinceGeoId" class="selectBox">
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
                 <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.PartyZipCode}</div></td>
                 <td width="2%">&nbsp;</td>
                 <td width="72%">
                    <@fieldErrors fieldName="shipToPostalCode"/>
                    <input type="text" class="inputBox" size="12" maxlength="10" name="shipToPostalCode" value="${parameters.shipToPostalCode?if_exists}">
                 *</td>
              </tr>
              <tr>
                 <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.PartyCountry}</div></td>
                 <td width="2%">&nbsp;</td>
                 <td width="72%">
                    <@fieldErrors fieldName="shipToCountryGeoId"/>
                    <select name="shipToCountryGeoId" class="selectBox">
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
                    <input type="checkbox" name="useShippingPostalAddressForBilling" value="Y" <#if useShippingPostalAddressForBilling?exists>checked</#if>  onClick="javascript:shipBillAddr()"/>
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
                  <input type="text" class="inputBox" name="billToName" value="${parameters.billToName?if_exists}" size="30" maxlength="30"/>
                </td>
              </tr>
              <tr>
                <td width="26%" align="right"><div class="tabletext">${uiLabelMap.PartyAttentionName}</div></td>
                <td width="2%">&nbsp;</td>
                <td width="72%">
                  <@fieldErrors fieldName="billToAttnName"/>
                  <input type="text" class="inputBox" name="billToAttnName" value="${parameters.billToAttnName?if_exists}" size="30" maxlength="30"/>
                </td>
              </tr>
              <tr>
                 <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.PartyAddressLine1}</div></td>
                 <td width="2%">&nbsp;</td>
                 <td width="72%">
                    <@fieldErrors fieldName="billToAddress1"/>
                    <input type="text" class="inputBox" size="30" maxlength="30" name="billToAddress1" value="${parameters.billToAddress1?if_exists}">
                 *</td>
              </tr>
              <tr>
                 <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.PartyAddressLine2}</div></td>
                 <td width="2%">&nbsp;</td>
                 <td width="72%">
                    <input type="text" class="inputBox" size="30" maxlength="30" name="billToAddress2" value="${parameters.billToAddress2?if_exists}">
                 </td>
              </tr>
              <tr>
                 <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.PartyCity}</div></td>
                 <td width="2%">&nbsp;</td>
                 <td width="72%">
                    <@fieldErrors fieldName="billToCity"/>
                    <input type="text" class="inputBox" size="30" maxlength="30" name="billToCity" value="${parameters.billToCity?if_exists}">
                 *</td>
              </tr>
              <tr>
                 <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.PartyState}</div></td>
                 <td width="2%">&nbsp;</td>
                 <td width="72%">
                    <@fieldErrors fieldName="billToStateProvinceGeoId"/>
                    <select name="billToStateProvinceGeoId" class="selectBox">
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
                 <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.PartyZipCode}</div></td>
                 <td width="2%">&nbsp;</td>
                 <td width="72%">
                    <@fieldErrors fieldName="billToPostalCode"/>
                    <input type="text" class="inputBox" size="12" maxlength="10" name="billToPostalCode" value="${parameters.billToPostalCode?if_exists}">
                 *</td>
              </tr>
              <tr>
                 <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.PartyCountry}</div></td>
                 <td width="2%">&nbsp;</td>
                 <td width="72%">
                    <@fieldErrors fieldName="billToCountryGeoId"/>
                    <select name="billToCountryGeoId" class="selectBox">
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
