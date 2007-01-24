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
    if (document.${parameters.formNameValue}.usePostalAddress_0ForContactMechPurpose_1.checked) {
       document.${parameters.formNameValue}.toName_1.value = document.${parameters.formNameValue}.toName_0.value ;
       document.${parameters.formNameValue}.attnName_1.value = document.${parameters.formNameValue}.attnName_0.value ;
       document.${parameters.formNameValue}.address1_1.value = document.${parameters.formNameValue}.address1_0.value ;
       document.${parameters.formNameValue}.address2_1.value = document.${parameters.formNameValue}.address2_0.value ;
       document.${parameters.formNameValue}.city_1.value = document.${parameters.formNameValue}.city_0.value ;
       document.${parameters.formNameValue}.stateProvinceGeoId_1.value = document.${parameters.formNameValue}.stateProvinceGeoId_0.value ;
       document.${parameters.formNameValue}.postalCode_1.value = document.${parameters.formNameValue}.postalCode_0.value ;
       document.${parameters.formNameValue}.countryGeoId_1.value = document.${parameters.formNameValue}.countryGeoId_0.value ;
       
       document.${parameters.formNameValue}.toName_1.disabled = true;
       document.${parameters.formNameValue}.attnName_1.disabled = true;
       document.${parameters.formNameValue}.address1_1.disabled = true;
       document.${parameters.formNameValue}.address2_1.disabled = true;
       document.${parameters.formNameValue}.city_1.disabled = true;
       document.${parameters.formNameValue}.stateProvinceGeoId_1.disabled = true;                                   
       document.${parameters.formNameValue}.postalCode_1.disabled = true;
       document.${parameters.formNameValue}.countryGeoId_1.disabled = true;                                   
    } else {
       document.${parameters.formNameValue}.toName_1.disabled = false;
       document.${parameters.formNameValue}.attnName_1.disabled = false;
       document.${parameters.formNameValue}.address1_1.disabled = false;
       document.${parameters.formNameValue}.address2_1.disabled = false;
       document.${parameters.formNameValue}.city_1.disabled = false;
       document.${parameters.formNameValue}.stateProvinceGeoId_1.disabled = false;                                   
       document.${parameters.formNameValue}.postalCode_1.disabled = false;
       document.${parameters.formNameValue}.countryGeoId_1.disabled = false;                                   
       document.${parameters.formNameValue}.postalAddressContactMechId_1.value = "";  
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
  <input type="hidden" name="postalAddressContactMechId_0" value="${parameters.shippingContactMechId?if_exists}"/>
  <input type="hidden" name="postalAddressContactMechId_1" value="${parameters.billingContactMechId?if_exists}"/>
  <input type="hidden" name="contactMechPurposeTypeId_0" value="${parameters.contactMechPurposeTypeId_0?if_exists}"/>
  <input type="hidden" name="contactMechPurposeTypeId_1" value="${parameters.contactMechPurposeTypeId_1?if_exists}"/>
  
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
                  <@fieldErrors fieldName="toName_0"/>
                  <input type="text" class="inputBox" name="toName_0" value="${parameters.toName_0?if_exists}" size="30" maxlength="30"/>
                </td>
              </tr>
              <tr>
                <td width="26%" align="right"><div class="tabletext">${uiLabelMap.PartyAttentionName}</div></td>
                <td width="2%">&nbsp;</td>
                <td width="72%">
                  <@fieldErrors fieldName="attnName_0"/>
                  <input type="text" class="inputBox" name="attnName_0" value="${parameters.attnName_0?if_exists}" size="30" maxlength="30"/>
                </td>
              </tr>
              <tr>
                 <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.PartyAddressLine1}</div></td>
                 <td width="2%">&nbsp;</td>
                 <td width="72%">
                    <@fieldErrors fieldName="address1_0"/>
                    <input type="text" class="inputBox" size="30" maxlength="30" name="address1_0" value="${parameters.address1_0?if_exists}">
                 *</td>
              </tr>
              <tr>
                 <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.PartyAddressLine2}</div></td>
                 <td width="2%">&nbsp;</td>
                 <td width="72%">
                    <input type="text" class="inputBox" size="30" maxlength="30" name="address2_0" value="${parameters.address2_0?if_exists}">
                 </td>
              </tr>
              <tr>
                 <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.PartyCity}</div></td>
                 <td width="2%">&nbsp;</td>
                 <td width="72%">
                    <@fieldErrors fieldName="city_0"/>
                    <input type="text" class="inputBox" size="30" maxlength="30" name="city_0" value="${parameters.city_0?if_exists}">
                 *</td>
              </tr>
              <tr>
                 <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.PartyState}</div></td>
                 <td width="2%">&nbsp;</td>
                 <td width="72%">
                    <@fieldErrors fieldName="stateProvinceGeoId_0"/>
                    <select name="stateProvinceGeoId_0" class="selectBox">
                    <#if (parameters.stateProvinceGeoId_0)?exists>
                       <option>${parameters.stateProvinceGeoId_0}</option>
                       <option value="${parameters.stateProvinceGeoId_0}">---</option>
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
                    <@fieldErrors fieldName="postalCode_0"/>
                    <input type="text" class="inputBox" size="12" maxlength="10" name="postalCode_0" value="${parameters.postalCode_0?if_exists}">
                 *</td>
              </tr>
              <tr>
                 <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.PartyCountry}</div></td>
                 <td width="2%">&nbsp;</td>
                 <td width="72%">
                    <@fieldErrors fieldName="countryGeoId_0"/>
                    <select name="countryGeoId_0" class="selectBox">
                    <#if (parameters.countryGeoId_0)?exists>
                       <option>${parameters.countryGeoId_0}</option>
                       <option value="${parameters.countryGeoId_0}">---</option>
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
                    <input type="checkbox" name="usePostalAddress_0ForContactMechPurpose_1" value="Y" <#if usePostalAddress_0ForContactMechPurpose_1?exists>checked</#if>  onClick="javascript:shipBillAddr()"/>
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
                  <@fieldErrors fieldName="toName_1"/>
                  <input type="text" class="inputBox" name="toName_1" value="${parameters.toName_1?if_exists}" size="30" maxlength="30"/>
                </td>
              </tr>
              <tr>
                <td width="26%" align="right"><div class="tabletext">${uiLabelMap.PartyAttentionName}</div></td>
                <td width="2%">&nbsp;</td>
                <td width="72%">
                  <@fieldErrors fieldName="attnName_1"/>
                  <input type="text" class="inputBox" name="attnName_1" value="${parameters.attnName_1?if_exists}" size="30" maxlength="30"/>
                </td>
              </tr>
              <tr>
                 <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.PartyAddressLine1}</div></td>
                 <td width="2%">&nbsp;</td>
                 <td width="72%">
                    <@fieldErrors fieldName="address1_1"/>
                    <input type="text" class="inputBox" size="30" maxlength="30" name="address1_1" value="${parameters.address1_1?if_exists}">
                 *</td>
              </tr>
              <tr>
                 <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.PartyAddressLine2}</div></td>
                 <td width="2%">&nbsp;</td>
                 <td width="72%">
                    <input type="text" class="inputBox" size="30" maxlength="30" name="address2_1" value="${parameters.address2_1?if_exists}">
                 </td>
              </tr>
              <tr>
                 <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.PartyCity}</div></td>
                 <td width="2%">&nbsp;</td>
                 <td width="72%">
                    <@fieldErrors fieldName="city_1"/>
                    <input type="text" class="inputBox" size="30" maxlength="30" name="city_1" value="${parameters.city_1?if_exists}">
                 *</td>
              </tr>
              <tr>
                 <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.PartyState}</div></td>
                 <td width="2%">&nbsp;</td>
                 <td width="72%">
                    <@fieldErrors fieldName="stateProvinceGeoId_1"/>
                    <select name="stateProvinceGeoId_1" class="selectBox">
                    <#if (parameters.stateProvinceGeoId_1)?exists>
                       <option>${parameters.stateProvinceGeoId_1}</option>
                       <option value="${parameters.stateProvinceGeoId_1}">---</option>
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
                    <@fieldErrors fieldName="postalCode_1"/>
                    <input type="text" class="inputBox" size="12" maxlength="10" name="postalCode_1" value="${parameters.postalCode_1?if_exists}">
                 *</td>
              </tr>
              <tr>
                 <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.PartyCountry}</div></td>
                 <td width="2%">&nbsp;</td>
                 <td width="72%">
                    <@fieldErrors fieldName="countryGeoId_1"/>
                    <select name="countryGeoId_1" class="selectBox">
                    <#if (parameters.countryGeoId_1)?exists>
                       <option>${parameters.countryGeoId_1}</option>
                       <option value="${parameters.countryGeoId_1}">---</option>
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
