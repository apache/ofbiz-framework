<#--
 *  Copyright (c) 2004 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *@author     Andy Zeneski (jaz@ofbiz.org)
 *@version    $Rev$
 *@since      3.0
-->

    <#-- reference number -->
    <#if txType?default("") == "PRDS_PAY_CREDIT" || txType?default("") == "PRDS_PAY_CAPTURE" || txType?default("") == "PRDS_PAY_RELEASE">
      ${setRequestAttribute("validTx", "true")}
      <#assign validTx = true>
      <tr><td colspan="3"><hr class="sepbar"></td></tr>
      <tr>
        <td width="26%" align="right" valign=middle><div class="tableheadtext">${uiLabelMap.AccountingReferenceNumber}</div></td>
        <td width="5">&nbsp;</td>
        <td width="74%">
          <input type="text" class="inputBox" size="30" maxlength="60" name="referenceNum">
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
      <tr><td colspan="3"><hr class="sepbar"></td></tr>
      <tr>
        <td width="26%" align="right" valign=middle><div class="tableheadtext">${uiLabelMap.PartyFirstName}</div></td>
        <td width="5">&nbsp;</td>
        <td width="74%">
          <input type="text" class="inputBox" size="30" maxlength="60" name="firstName" value="${(person.firstName)?if_exists}">
        *</td>
      </tr>
      <tr>
        <td width="26%" align="right" valign=middle><div class="tableheadtext">${uiLabelMap.PartyLastName}</div></td>
        <td width="5">&nbsp;</td>
        <td width="74%">
          <input type="text" class="inputBox" size="30" maxlength="60" name="lastName" value="${(person.lastName)?if_exists}">
        *</td>
      </tr>
      <tr>
        <td width="26%" align="right" valign=middle><div class="tableheadtext">${uiLabelMap.PartyEmailAddress}</div></td>
        <td width="5">&nbsp;</td>
        <td width="74%">
          <input type="text" class="inputBox" size="30" maxlength="60" name="infoString" value="">
        *</td>
      </tr>
      <tr><td colspan="3"><hr class="sepbar"></td></tr>

    <tr>
      <td width="26%" align="right" valign="top"><div class="tableheadtext">${uiLabelMap.AccountingCompanyNameCard}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <input type="text" class='inputBox' size="30" maxlength="60" name="companyNameOnCard" value="${(creditCard.companyNameOnCard)?if_exists}">
      </td>
    </tr>
    <tr>
      <td width="26%" align="right" valign="top"><div class="tableheadtext">${uiLabelMap.AccountingPrefixCard}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <select name="titleOnCard" class="selectBox">
          <option value="">${uiLabelMap.CommonSelectOne}</option>
          <option<#if ((creditCard.titleOnCard)?default("") == "Mr.")> checked</#if>>Mr.</option>
          <option<#if ((creditCard.titleOnCard)?default("") == "Mrs.")> checked</#if>>Mrs.</option>
          <option<#if ((creditCard.titleOnCard)?default("") == "Ms.")> checked</#if>>Ms.</option>
          <option<#if ((creditCard.titleOnCard)?default("") == "Dr.")> checked</#if>>Dr.</option>
        </select>
      </td>
    </tr>
    <tr>
      <td width="26%" align="right" valign="top"><div class="tableheadtext">${uiLabelMap.AccountingFirstNameCard}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <input type="text" class="inputBox" size="20" maxlength="60" name="firstNameOnCard" value="${(creditCard.firstNameOnCard)?if_exists}">
      *</td>
    </tr>
    <tr>
      <td width="26%" align="right" valign="top"><div class="tableheadtext">${uiLabelMap.AccountingMiddleNameCard}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <input type="text" class="inputBox" size="15" maxlength="60" name="middleNameOnCard" value="${(creditCard.middleNameOnCard)?if_exists}">
      </td>
    </tr>
    <tr>
      <td width="26%" align="right" valign="top"><div class="tableheadtext">${uiLabelMap.AccountingLastNameCard}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <input type="text" class="inputBox" size="20" maxlength="60" name="lastNameOnCard" value="${(creditCard.lastNameOnCard)?if_exists}">
      *</td>
    </tr>
    <tr>
      <td width="26%" align="right" valign="top"><div class="tableheadtext">${uiLabelMap.AccountingSuffixCard}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <select name="suffixOnCard" class="selectBox">
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
        <td width="26%" align="right" valign=middle><div class="tableheadtext">${uiLabelMap.AccountingCardType}</div></td>
        <td width="5">&nbsp;</td>
        <td width="74%">
          <select name="cardType" class="selectBox">
            <#if ((creditCard.cardType)?exists)>
              <option>${creditCard.cardType}</option>
              <option value="${creditCard.cardType}">---</option>
            </#if>
            ${screens.render("component://common/widget/CommonScreens.xml#cctypes")}
          </select>
        *</td>
      </tr>
      <tr>
        <td width="26%" align="right" valign=middle><div class="tableheadtext">${uiLabelMap.AccountingCardNumber}</div></td>
        <td width="5">&nbsp;</td>
        <td width="74%">
          <input type="text" class="inputBox" size="20" maxlength="30" name="cardNumber" value="${(creditCard.cardNumber)?if_exists}">
        *</td>
      </tr>
      <tr>
        <td width="26%" align="right" valign=middle><div class="tableheadtext">${uiLabelMap.AccountingSecurityCodeCard}</div></td>
        <td width="5">&nbsp;</td>
        <td width="74%">
          <input type="text" size="5" maxlength="10" name="cardSecurityCode" value="">
        </td>
      </tr>
      <tr>
        <td width="26%" align="right" valign=middle><div class="tableheadtext">${uiLabelMap.AccountingExpirationDate}</div></td>
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
          <select name="expMonth" class='selectBox'>
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
          <select name="expYear" class='selectBox'>
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
      <tr><td colspan="3"><hr class="sepbar"></td></tr>

      <#-- first / last name -->
      <tr>
        <td width="26%" align="right" valign=middle><div class="tableheadtext">${uiLabelMap.PartyFirstName}</div></td>
        <td width="5">&nbsp;</td>
        <td width="74%">
          <input type="text" class="inputBox" size="30" maxlength="30" name="firstName" value="${(person.firstName)?if_exists}" <#if requestParameters.useShipAddr?exists>disabled</#if>>
        *</td>
      </tr>
      <tr>
        <td width="26%" align="right" valign=middle><div class="tableheadtext">${uiLabelMap.PartyLastName}</div></td>
        <td width="5">&nbsp;</td>
        <td width="74%">
          <input type="text" class="inputBox" size="30" maxlength="30" name="lastName" value="${(person.lastName)?if_exists}" <#if requestParameters.useShipAddr?exists>disabled</#if>>
        *</td>
      </tr>

      <#-- credit card address -->
      <tr>
        <td width="26%" align="right" valign=middle><div class="tableheadtext">${uiLabelMap.AccountingBillToAddress1}</div></td>
        <td width="5">&nbsp;</td>
        <td width="74%">
          <input type="text" class="inputBox" size="30" maxlength="30" name="address1" value="${(postalFields.address1)?if_exists}" <#if requestParameters.useShipAddr?exists>disabled</#if>>
        *</td>
      </tr>
      <tr>
        <td width="26%" align="right" valign=middle><div class="tableheadtext">${uiLabelMap.AccountingBillToAddress2}</div></td>
        <td width="5">&nbsp;</td>
        <td width="74%">
          <input type="text" class="inputBox" size="30" maxlength="30" name="address2" value="${(postalFields.address2)?if_exists}" <#if requestParameters.useShipAddr?exists>disabled</#if>>
        </td>
      </tr>
      <tr>
        <td width="26%" align="right" valign=middle><div class="tableheadtext">${uiLabelMap.CommonCity}</div></td>
        <td width="5">&nbsp;</td>
        <td width="74%">
          <input type="text" class="inputBox" size="30" maxlength="30" name="city" value="${(postalFields.city)?if_exists}" <#if requestParameters.useShipAddr?exists>disabled</#if>>
        *</td>
      </tr>
      <tr>
        <td width="26%" align="right" valign=middle><div class="tableheadtext">${uiLabelMap.CommonStateProvince}</div></td>
        <td width="5">&nbsp;</td>
        <td width="74%">
          <select name="stateProvinceGeoId" class="selectBox" <#if requestParameters.useShipAddr?exists>disabled</#if>>
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
        <td width="26%" align="right" valign=middle><div class="tableheadtext">${uiLabelMap.CommonZipPostalCode}</div></td>
        <td width="5">&nbsp;</td>
        <td width="74%">
          <input type="text" class="inputBox" size="12" maxlength="10" name="postalCode" value="${(postalFields.postalCode)?if_exists}" <#if requestParameters.useShipAddr?exists>disabled</#if>>
        *</td>
      </tr>
      <tr>
        <td width="26%" align="right" valign=middle><div class="tableheadtext">${uiLabelMap.CommonCountry}</div></td>
        <td width="5">&nbsp;</td>
        <td width="74%">
          <select name="countryGeoId" class="selectBox" <#if requestParameters.useShipAddr?exists>disabled</#if>>
            <#if (postalFields.countryGeoId)?exists>
              <option>${postalFields.countryGeoId}</option>
              <option value="${postalFields.countryGeoId}">---</option>
            </#if>
            ${screens.render("component://common/widget/CommonScreens.xml#countries")}
          </select>
        *</td>
      </tr>
    </#if>
