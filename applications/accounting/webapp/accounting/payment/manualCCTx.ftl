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

    <#-- reference number -->
    <#if txType?default("") == "PRDS_PAY_CREDIT" || txType?default("") == "PRDS_PAY_CAPTURE" || txType?default("") == "PRDS_PAY_RELEASE">
      ${setRequestAttribute("validTx", "true")}
      <#assign validTx = true>
      <tr><td colspan="3"><hr/></td></tr>
      <tr>
        <td width="26%" align="right" valign=middle><b>${uiLabelMap.AccountingReferenceNumber}</b></td>
        <td width="5">&nbsp;</td>
        <td width="74%">
          <input type="text" size="30" maxlength="60" name="referenceNum">
        (*)</td>
      </tr>
    </#if>

    <#-- manual credit card information -->
    <#if txType?default("") == "PRDS_PAY_CREDIT" || txType?default("") == "PRDS_PAY_AUTH">
      ${setRequestAttribute("validTx", "true")}
      <script language="JavaScript" type="text/javascript">
      <!-- //
        document.manualTxForm.action = "<@ofbizUrl>processManualCcTx</@ofbizUrl>";
      // -->
      </script>
      <tr><td colspan="3"><hr></td></tr>
      <tr>
        <td width="26%" align="right" valign=middle><b>${uiLabelMap.PartyFirstName}</b></td>
        <td width="5">&nbsp;</td>
        <td width="74%">
          <input type="text" size="30" maxlength="60" name="firstName" value="${(person.firstName)?if_exists}">
        *</td>
      </tr>
      <tr>
        <td width="26%" align="right" valign=middle><b>${uiLabelMap.PartyLastName}</b></td>
        <td width="5">&nbsp;</td>
        <td width="74%">
          <input type="text" size="30" maxlength="60" name="lastName" value="${(person.lastName)?if_exists}">
        *</td>
      </tr>
      <tr>
        <td width="26%" align="right" valign=middle><b>${uiLabelMap.PartyEmailAddress}</b></td>
        <td width="5">&nbsp;</td>
        <td width="74%">
          <input type="text" size="30" maxlength="60" name="infoString" value="">
        *</td>
      </tr>
      <tr><td colspan="3"><hr></td></tr>

    <tr>
      <td width="26%" align="right" valign="top"><b>${uiLabelMap.AccountingCompanyNameCard}</b></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <input type="text" size="30" maxlength="60" name="companyNameOnCard" value="${(creditCard.companyNameOnCard)?if_exists}">
      </td>
    </tr>
    <tr>
      <td width="26%" align="right" valign="top"><b>${uiLabelMap.AccountingPrefixCard}</b></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <select name="titleOnCard">
          <option value="">${uiLabelMap.CommonSelectOne}</option>
          <option<#if ((creditCard.titleOnCard)?default("") == "Mr.")> checked</#if>>Mr.</option>
          <option<#if ((creditCard.titleOnCard)?default("") == "Mrs.")> checked</#if>>Mrs.</option>
          <option<#if ((creditCard.titleOnCard)?default("") == "Ms.")> checked</#if>>Ms.</option>
          <option<#if ((creditCard.titleOnCard)?default("") == "Dr.")> checked</#if>>Dr.</option>
        </select>
      </td>
    </tr>
    <tr>
      <td width="26%" align="right" valign="top"><b>${uiLabelMap.AccountingFirstNameCard}</b></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <input type="text" size="20" maxlength="60" name="firstNameOnCard" value="${(creditCard.firstNameOnCard)?if_exists}">
      *</td>
    </tr>
    <tr>
      <td width="26%" align="right" valign="top"><b>${uiLabelMap.AccountingMiddleNameCard}</b></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <input type="text" size="15" maxlength="60" name="middleNameOnCard" value="${(creditCard.middleNameOnCard)?if_exists}">
      </td>
    </tr>
    <tr>
      <td width="26%" align="right" valign="top"><b>${uiLabelMap.AccountingLastNameCard}</b></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <input type="text" size="20" maxlength="60" name="lastNameOnCard" value="${(creditCard.lastNameOnCard)?if_exists}">
      *</td>
    </tr>
    <tr>
      <td width="26%" align="right" valign="top"><b>${uiLabelMap.AccountingSuffixCard}</b></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <select name="suffixOnCard">
          <option value="">${uiLabelMap.CommonSelectOne}</option>
          <option<#if ((creditCard.suffixOnCard)?default("") == "Jr.")> checked</#if>>Jr.</option>
          <option<#if ((creditCard.suffixOnCard)?default("") == "Sr.")> checked</#if>>Sr.</option>
          <option<#if ((creditCard.suffixOnCard)?default("") == "I")> checked</#if>>I</option>
          <option<#if ((creditCard.suffixOnCard)?default("") == "II")> checked</#if>>II</option>
          <option<#if ((creditCard.suffixOnCard)?default("") == "III")> checked</#if>>III</option>
          <option<#if ((creditCard.suffixOnCard)?default("") == "IV")> checked</#if>>IV</option>
          <option<#if ((creditCard.suffixOnCard)?default("") == "V")> checked</#if>>V</option>
        </select>
      </td>
    </tr>

      <tr>
        <td width="26%" align="right" valign=middle><b>${uiLabelMap.AccountingCardType}</b></td>
        <td width="5">&nbsp;</td>
        <td width="74%">
          <select name="cardType">
            <#if ((creditCard.cardType)?exists)>
              <option>${creditCard.cardType}</option>
              <option value="${creditCard.cardType}">---</option>
            </#if>
            ${screens.render("component://common/widget/CommonScreens.xml#cctypes")}
          </select>
        *</td>
      </tr>
      <tr>
        <td width="26%" align="right" valign=middle><b>${uiLabelMap.AccountingCardNumber}</b></td>
        <td width="5">&nbsp;</td>
        <td width="74%">
          <input type="text" size="20" maxlength="30" name="cardNumber" value="${(creditCard.cardNumber)?if_exists}">
        *</td>
      </tr>
      <tr>
        <td width="26%" align="right" valign=middle><b>${uiLabelMap.AccountingSecurityCodeCard}</b></td>
        <td width="5">&nbsp;</td>
        <td width="74%">
          <input type="text" size="5" maxlength="10" name="cardSecurityCode" value="">
        </td>
      </tr>
      <tr>
        <td width="26%" align="right" valign=middle><b>${uiLabelMap.AccountingExpirationDate}</b></td>
        <td width="5">&nbsp;</td>
        <td width="74%">
          <#assign expMonth = "">
          <#assign expYear = "">
          <#if creditCard?exists && creditCard.expireDate?exists>
            <#assign expDate = creditCard.expireDate>
            <#if (expDate?exists && expDate.indexOf("/") > 0)>
              <#assign expMonth = expDate.substring(0,expDate.indexOf("/"))>
              <#assign expYear = expDate.substring(expDate.indexOf("/")+1)>
            </#if>
          </#if>
          <select name="expMonth">
            <#if creditCard?has_content && expMonth?has_content>
              <#assign ccExprMonth = expMonth>
            <#else>
              <#assign ccExprMonth = requestParameters.expMonth?if_exists>
            </#if>
            <#if ccExprMonth?has_content>
              <option value="${ccExprMonth?if_exists}">${ccExprMonth?if_exists}</option>
            </#if>
            ${screens.render("component://common/widget/CommonScreens.xml#ccmonths")}
          </select>
          <select name="expYear">
            <#if creditCard?has_content && expYear?has_content>
              <#assign ccExprYear = expYear>
            <#else>
              <#assign ccExprYear = requestParameters.expYear?if_exists>
            </#if>
            <#if ccExprYear?has_content>
              <option value="${ccExprYear?if_exists}">${ccExprYear?if_exists}</option>
            </#if>
            ${screens.render("component://common/widget/CommonScreens.xml#ccyears")}
          </select>
        *</td>
      </tr>
      <tr><td colspan="3"><hr></td></tr>

      <#-- first / last name -->
      <tr>
        <td width="26%" align="right" valign=middle><b>${uiLabelMap.PartyFirstName}</b></td>
        <td width="5">&nbsp;</td>
        <td width="74%">
          <input type="text" size="30" maxlength="30" name="firstName" value="${(person.firstName)?if_exists}" <#if requestParameters.useShipAddr?exists>disabled</#if>>
        *</td>
      </tr>
      <tr>
        <td width="26%" align="right" valign=middle><b>${uiLabelMap.PartyLastName}</b></td>
        <td width="5">&nbsp;</td>
        <td width="74%">
          <input type="text" size="30" maxlength="30" name="lastName" value="${(person.lastName)?if_exists}" <#if requestParameters.useShipAddr?exists>disabled</#if>>
        *</td>
      </tr>

      <#-- credit card address -->
      <tr>
        <td width="26%" align="right" valign=middle><b>${uiLabelMap.AccountingBillToAddress1}</b></td>
        <td width="5">&nbsp;</td>
        <td width="74%">
          <input type="text" size="30" maxlength="30" name="address1" value="${(postalFields.address1)?if_exists}" <#if requestParameters.useShipAddr?exists>disabled</#if>>
        *</td>
      </tr>
      <tr>
        <td width="26%" align="right" valign=middle><b>${uiLabelMap.AccountingBillToAddress2}</b></td>
        <td width="5">&nbsp;</td>
        <td width="74%">
          <input type="text" size="30" maxlength="30" name="address2" value="${(postalFields.address2)?if_exists}" <#if requestParameters.useShipAddr?exists>disabled</#if>>
        </td>
      </tr>
      <tr>
        <td width="26%" align="right" valign=middle><b>${uiLabelMap.CommonCity}</b></td>
        <td width="5">&nbsp;</td>
        <td width="74%">
          <input type="text" size="30" maxlength="30" name="city" value="${(postalFields.city)?if_exists}" <#if requestParameters.useShipAddr?exists>disabled</#if>>
        *</td>
      </tr>
      <tr>
        <td width="26%" align="right" valign=middle><b>${uiLabelMap.CommonStateProvince}</b></td>
        <td width="5">&nbsp;</td>
        <td width="74%">
          <select name="stateProvinceGeoId" <#if requestParameters.useShipAddr?exists>disabled</#if>>
            <#if (postalFields.stateProvinceGeoId)?exists>
              <option>${postalFields.stateProvinceGeoId}</option>
              <option value="${postalFields.stateProvinceGeoId}">---</option>
            <#else>
              <option value="">${uiLabelMap.CommonNone} ${uiLabelMap.CommonState}</option>
            </#if>
            ${screens.render("component://common/widget/CommonScreens.xml#states")}
          </select>
        </td>
      </tr>
      <tr>
        <td width="26%" align="right" valign=middle><b>${uiLabelMap.CommonZipPostalCode}</b></td>
        <td width="5">&nbsp;</td>
        <td width="74%">
          <input type="text" size="12" maxlength="10" name="postalCode" value="${(postalFields.postalCode)?if_exists}" <#if requestParameters.useShipAddr?exists>disabled</#if>>
        *</td>
      </tr>
      <tr>
        <td width="26%" align="right" valign=middle><b>${uiLabelMap.CommonCountry}</b></td>
        <td width="5">&nbsp;</td>
        <td width="74%">
          <select name="countryGeoId" <#if requestParameters.useShipAddr?exists>disabled</#if>>
            <#if (postalFields.countryGeoId)?exists>
              <option>${postalFields.countryGeoId}</option>
              <option value="${postalFields.countryGeoId}">---</option>
            </#if>
            ${screens.render("component://common/widget/CommonScreens.xml#countries")}
          </select>
        *</td>
      </tr>
    </#if>
