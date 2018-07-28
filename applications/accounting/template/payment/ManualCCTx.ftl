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
    <#if "PRDS_PAY_CREDIT" == txType?default("") || "PRDS_PAY_CAPTURE" == txType?default("") ||
         "PRDS_PAY_RELEASE" == txType?default("") || "PRDS_PAY_REFUND" == txType?default("")>
      ${setRequestAttribute("validTx", "true")}
      <#assign validTx = true>
      <tr><td colspan="3"><hr /></td></tr>
      <tr>
        <td width="26%" align="right" valign="middle"><b>${uiLabelMap.AccountingReferenceNumber}</b></td>
        <td width="5">&nbsp;</td>
        <td width="74%">
          <input type="text" size="30" maxlength="60" name="referenceNum" />
          <span class="tooltip">${uiLabelMap.CommonRequired}</span>
        </td>
      </tr>
      <tr>
        <td width="26%" align="right" valign="middle"><b>${uiLabelMap.FormFieldTitle_orderPaymentPreferenceId}</b></td>
        <td width="5">&nbsp;</td>
        <td width="74%">
          <input type="text" size="20" maxlength="20" name="orderPaymentPreferenceId" />
          <span class="tooltip">${uiLabelMap.CommonRequired}</span>
        </td>
      </tr>
    </#if>
    <#-- manual credit card information -->
    <#if "PRDS_PAY_RELEASE" == txType?default("")>
      <tr><td>
      ${setRequestAttribute("validTx", "true")}
      <script type="application/javascript">
      <!-- //
        document.manualTxForm.action = "<@ofbizUrl>processReleaseTransaction</@ofbizUrl>";
      // -->
      </script>
      </td></tr>
    </#if>
    <#if "PRDS_PAY_REFUND" == txType?default("")>
      <tr><td>
      ${setRequestAttribute("validTx", "true")}
      <script type="application/javascript">
      <!-- //
        document.manualTxForm.action = "<@ofbizUrl>processRefundTransaction</@ofbizUrl>";
      // -->
      </script>
      </td></tr>
    </#if>
    <#if "PRDS_PAY_CREDIT" == txType?default("") || "PRDS_PAY_AUTH" == txType?default("")>
      <tr><td>
      ${setRequestAttribute("validTx", "true")}
      <script type="application/javascript">
      <!-- //
        document.manualTxForm.action = "<@ofbizUrl>processManualCcTx</@ofbizUrl>";
      // -->
      </script>
      </td></tr>
      <tr><td colspan="3"><hr/></td></tr>
      <tr>
        <td width="26%" align="right" valign="middle"><b>${uiLabelMap.PartyFirstName}</b></td>
        <td width="5">&nbsp;</td>
        <td width="74%">
          <input type="text" size="30" maxlength="60" name="firstName" value="${(person.firstName)!}" />
          <span class="tooltip">${uiLabelMap.CommonRequired}</span>
        </td>
      </tr>
      <tr>
        <td width="26%" align="right" valign="middle"><b>${uiLabelMap.PartyLastName}</b></td>
        <td width="5">&nbsp;</td>
        <td width="74%">
          <input type="text" size="30" maxlength="60" name="lastName" value="${(person.lastName)!}" />
          <span class="tooltip">${uiLabelMap.CommonRequired}</span>
        </td>
      </tr>
      <tr>
        <td width="26%" align="right" valign="middle"><b>${uiLabelMap.PartyEmailAddress}</b></td>
        <td width="5">&nbsp;</td>
        <td width="74%">
          <input type="text" size="30" maxlength="60" name="infoString" value="" />
          <span class="tooltip">${uiLabelMap.CommonRequired}</span>
        </td>
      </tr>
      <tr><td colspan="3"><hr/></td></tr>
      <#assign showToolTip = "true">
      ${screens.render("component://accounting/widget/CommonScreens.xml#creditCardFields")}
      <tr><td colspan="3"><hr/></td></tr>
      <#-- first / last name -->
      <tr>
        <td width="26%" align="right" valign="middle"><b>${uiLabelMap.PartyFirstName}</b></td>
        <td width="5">&nbsp;</td>
        <td width="74%">
          <input type="text" size="30" maxlength="30" name="firstName" value="${(person.firstName)!}" <#if requestParameters.useShipAddr??>disabled</#if> />
          <span class="tooltip">${uiLabelMap.CommonRequired}</span>
        </td>
      </tr>
      <tr>
        <td width="26%" align="right" valign="middle"><b>${uiLabelMap.PartyLastName}</b></td>
        <td width="5">&nbsp;</td>
        <td width="74%">
          <input type="text" size="30" maxlength="30" name="lastName" value="${(person.lastName)!}" <#if requestParameters.useShipAddr??>disabled</#if> />
          <span class="tooltip">${uiLabelMap.CommonRequired}</span>
        </td>
      </tr>
      <#-- credit card address -->
      <tr>
        <td width="26%" align="right" valign="middle"><b>${uiLabelMap.AccountingBillToAddress1}</b></td>
        <td width="5">&nbsp;</td>
        <td width="74%">
          <input type="text" size="30" maxlength="30" name="address1" value="${(postalFields.address1)!}" <#if requestParameters.useShipAddr??>disabled</#if> />
          <span class="tooltip">${uiLabelMap.CommonRequired}</span>
        </td>
      </tr>
      <tr>
        <td width="26%" align="right" valign="middle"><b>${uiLabelMap.AccountingBillToAddress2}</b></td>
        <td width="5">&nbsp;</td>
        <td width="74%">
          <input type="text" size="30" maxlength="30" name="address2" value="${(postalFields.address2)!}" <#if requestParameters.useShipAddr??>disabled</#if> />
        </td>
      </tr>
      <tr>
        <td width="26%" align="right" valign="middle"><b>${uiLabelMap.CommonCity}</b></td>
        <td width="5">&nbsp;</td>
        <td width="74%">
          <input type="text" size="30" maxlength="30" name="city" value="${(postalFields.city)!}" <#if requestParameters.useShipAddr??>disabled</#if> />
          <span class="tooltip">${uiLabelMap.CommonRequired}</span>
        </td>
      </tr>
      <tr>
        <td width="26%" align="right" valign="middle"><b>${uiLabelMap.CommonStateProvince}</b></td>
        <td width="5">&nbsp;</td>
        <td width="74%">
          <select name="stateProvinceGeoId" <#if requestParameters.useShipAddr??>disabled</#if>>
            <#if (postalFields.stateProvinceGeoId)??>
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
        <td width="26%" align="right" valign="middle"><b>${uiLabelMap.CommonZipPostalCode}</b></td>
        <td width="5">&nbsp;</td>
        <td width="74%">
          <input type="text" size="12" maxlength="10" name="postalCode" value="${(postalFields.postalCode)!}" <#if requestParameters.useShipAddr??>disabled</#if> />
          <span class="tooltip">${uiLabelMap.CommonRequired}</span>
        </td>
      </tr>
      <tr>
        <td width="26%" align="right" valign="middle"><b>${uiLabelMap.CommonCountry}</b></td>
        <td width="5">&nbsp;</td>
        <td width="74%">
          <select name="countryGeoId" <#if requestParameters.useShipAddr??>disabled</#if>>
            <#if (postalFields.countryGeoId)??>
              <option>${postalFields.countryGeoId}</option>
              <option value="${postalFields.countryGeoId}">---</option>
            </#if>
            ${screens.render("component://common/widget/CommonScreens.xml#countries")}
          </select>
          <span class="tooltip">${uiLabelMap.CommonRequired}</span>
        </td>
      </tr>
    </#if>
