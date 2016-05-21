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

<#if canNotView>
  <p><h3>${uiLabelMap.AccountingEFTNotBelongToYou}.</h3></p>
&nbsp;<a href="<@ofbizUrl>${donePage}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonGoBack}</a>
<#else>
    <#if !eftAccount??>
      <h1>${uiLabelMap.AccountingAddNewEftAccount}</h1>
      <form method="post" action="<@ofbizUrl>createEftAccount?DONE_PAGE=${donePage}</@ofbizUrl>" name="editeftaccountform" style="margin: 0;">
    <#else>
      <h1>${uiLabelMap.PageTitleEditEFTAccount}</h1>
      <form method="post" action="<@ofbizUrl>updateEftAccount?DONE_PAGE=${donePage}</@ofbizUrl>" name="editeftaccountform" style="margin: 0;">
        <input type="hidden" name="paymentMethodId" value="${paymentMethodId}" />
    </#if>
    &nbsp;<a href="<@ofbizUrl>${donePage}</@ofbizUrl>" class="button">${uiLabelMap.CommonGoBack}</a>
    &nbsp;<a href="javascript:document.editeftaccountform.submit()" class="button">${uiLabelMap.CommonSave}</a>
    <p/>
    <table width="90%" border="0" cellpadding="2" cellspacing="0">
    <tr>
      <td width="26%" align="right" valign="top"><div>${uiLabelMap.AccountingNameOnAccount}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <input type="text" class="inputBox" size="30" maxlength="60" name="nameOnAccount" value="${eftAccountData.nameOnAccount!}" />
      *</td>
    </tr>
    <tr>
      <td width="26%" align="right" valign="top"><div>${uiLabelMap.AccountingCompanyNameOnAccount}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <input type="text" class="inputBox" size="30" maxlength="60" name="companyNameOnAccount" value="${eftAccountData.companyNameOnAccount!}" />
      </td>
    </tr>
    <tr>
      <td width="26%" align="right" valign="top"><div>${uiLabelMap.AccountingBankName}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <input type="text" class="inputBox" size="30" maxlength="60" name="bankName" value="${eftAccountData.bankName!}" />
      *</td>
    </tr>
    <tr>
      <td width="26%" align="right" valign="top"><div>${uiLabelMap.AccountingRoutingNumber}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <input type="text" class="inputBox" size="10" maxlength="30" name="routingNumber" value="${eftAccountData.routingNumber!}" />
      *</td>
    </tr>
    <tr>
      <td width="26%" align="right" valign="top"><div>${uiLabelMap.AccountingAccountType}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <select name="accountType" class="selectBox">
          <option>${eftAccountData.accountType!}</option>
          <option></option>
          <option>${uiLabelMap.CommonChecking}</option>
          <option>${uiLabelMap.CommonSavings}</option>
        </select>
      *</td>
    </tr>
    <tr>
      <td width="26%" align="right" valign="top"><div>${uiLabelMap.AccountingAccountNumber}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <input type="text" class="inputBox" size="20" maxlength="40" name="accountNumber" value="${eftAccountData.accountNumber!}" />
      *</td>
    </tr>
    <tr>
      <td width="26%" align="right" valign="top"><div>${uiLabelMap.CommonDescription}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <input type="text" class="inputBox" size="30" maxlength="60" name="description" value="${paymentMethodData.description!}" />
      </td>
    </tr>

    <tr>
      <td width="26%" align="right" valign="top"><div>${uiLabelMap.PartyBillingAddress}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <#-- Removed because is confusing, can add but would have to come back here with all data populated as before...
        <a href="<@ofbizUrl>editcontactmech</@ofbizUrl>" class="buttontext">
          [Create New Address]</a>&nbsp;&nbsp;
        -->
        <table width="100%" border="0" cellpadding="1">
        <#if curPostalAddress??>
          <tr>
            <td align="right" valign="top" width="1%">
              <input type="radio" name="contactMechId" value="${curContactMechId}" checked="checked" />
            </td>
            <td valign="top" width="80%">
              <div><b>${uiLabelMap.PartyUseCurrentAddress}:</b></div>
              <#list curPartyContactMechPurposes as curPartyContactMechPurpose>
                <#assign curContactMechPurposeType = curPartyContactMechPurpose.getRelatedOne("ContactMechPurposeType", true)>
                <div>
                  <b>${curContactMechPurposeType.get("description",locale)!}</b>
                  <#if curPartyContactMechPurpose.thruDate??>
                    (${uiLabelMap.CommonExpire}:${curPartyContactMechPurpose.thruDate.toString()})
                  </#if>
                </div>
              </#list>
              <div>
                <#if curPostalAddress.toName??><b>${uiLabelMap.CommonTo}:</b> ${curPostalAddress.toName}<br /></#if>
                <#if curPostalAddress.attnName??><b>${uiLabelMap.PartyAddrAttnName}:</b> ${curPostalAddress.attnName}<br /></#if>
                ${curPostalAddress.address1!}<br />
                <#if curPostalAddress.address2??>${curPostalAddress.address2}<br /></#if>
                ${curPostalAddress.city}<#if curPostalAddress.stateProvinceGeoId?has_content>,&nbsp;${curPostalAddress.stateProvinceGeoId}</#if>&nbsp;${curPostalAddress.postalCode}
                <#if curPostalAddress.countryGeoId??><br />${curPostalAddress.countryGeoId}</#if>
              </div>
              <div>(${uiLabelMap.CommonUpdated}:&nbsp;${(curPartyContactMech.fromDate.toString())!})</div>
              <#if curPartyContactMech.thruDate??><div><b>${uiLabelMap.CommonDelete}:&nbsp;${curPartyContactMech.thruDate.toString()}</b></div></#if>
            </td>
          </tr>
        <#else>
           <#-- <tr>
            <td valign="top" colspan="2">
              <div>${uiLabelMap.PartyNoBillingAddress}</div>
            </td>
          </tr> -->
        </#if>
          <#-- is confusing
          <tr>
            <td valign="top" colspan="2">
              <div><b>${uiLabelMap.EcommerceMessage3}</b></div>
            </td>
          </tr>
          -->
          <#list postalAddressInfos as postalAddressInfo>
            <#assign contactMech = postalAddressInfo.contactMech>
            <#assign partyContactMechPurposes = postalAddressInfo.partyContactMechPurposes>
            <#assign postalAddress = postalAddressInfo.postalAddress>
            <#assign partyContactMech = postalAddressInfo.partyContactMech>
            <tr>
              <td align="right" valign="top" width="1%">
                <input type="radio" name="contactMechId" value="${contactMech.contactMechId}" />
              </td>
              <td valign="top" width="80%">
                <#list partyContactMechPurposes as partyContactMechPurpose>
                    <#assign contactMechPurposeType = partyContactMechPurpose.getRelatedOne("ContactMechPurposeType", true)>
                    <div>
                      <b>${contactMechPurposeType.get("description",locale)!}</b>
                      <#if partyContactMechPurpose.thruDate??>(${uiLabelMap.CommonExpire}:${partyContactMechPurpose.thruDate})</#if>
                    </div>
                </#list>
                <div>
                  <#if postalAddress.toName??><b>${uiLabelMap.CommonTo}:</b> ${postalAddress.toName}<br /></#if>
                  <#if postalAddress.attnName??><b>${uiLabelMap.PartyAddrAttnName}:</b> ${postalAddress.attnName}<br /></#if>
                  ${postalAddress.address1!}<br />
                  <#if postalAddress.address2??>${postalAddress.address2}<br /></#if>
                  ${postalAddress.city}<#if postalAddress.stateProvinceGeoId?has_content>,&nbsp;${postalAddress.stateProvinceGeoId}</#if>&nbsp;${postalAddress.postalCode}
                  <#if postalAddress.countryGeoId??><br />${postalAddress.countryGeoId}</#if>
                </div>
                <div>(${uiLabelMap.CommonUpdated}:&nbsp;${(partyContactMech.fromDate.toString())!})</div>
                <#if partyContactMech.thruDate??><div><b>${uiLabelMap.CommonDelete}:&nbsp;${partyContactMech.thruDate.toString()}</b></div></#if>
              </td>
            </tr>
          </#list>
          <#if !postalAddressInfos?has_content && !curContactMech??>
              <tr><td colspan="2"><div>${uiLabelMap.PartyNoContactInformation}.</div></td></tr>
          </#if>
        </table>
      </td>
    </tr>
  </table>
  </form>
  &nbsp;<a href="<@ofbizUrl>${donePage}</@ofbizUrl>" class="button">${uiLabelMap.CommonGoBack}</a>
  &nbsp;<a href="javascript:document.editeftaccountform.submit()" class="button">${uiLabelMap.CommonSave}</a>  
</#if>

