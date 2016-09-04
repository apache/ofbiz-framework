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

<!-- begin EditEftAccount.ftl -->
<div class="screenlet">
  <div class="screenlet-title-bar">
    <#if !eftAccount??>
      <h3>${uiLabelMap.AccountingAddNewEftAccount}</h3>
    <#else>
      <h3>${uiLabelMap.PageTitleEditEftAccount}</h3>
    </#if>
  </div>
  <div class="screenlet-body">
        <div class="button-bar">
          <a href="<@ofbizUrl>${donePage}?partyId=${partyId}</@ofbizUrl>" class="smallSubmit">${uiLabelMap.CommonCancelDone}</a>
          <a href="javascript:document.editeftaccountform.submit()" class="smallSubmit">${uiLabelMap.CommonSave}</a>
        </div>
    <#if !eftAccount??>
      <form method="post" action='<@ofbizUrl>createEftAccount?DONE_PAGE=${donePage}</@ofbizUrl>' name="editeftaccountform" style='margin: 0;'>
    <#else>
      <form method="post" action='<@ofbizUrl>updateEftAccount?DONE_PAGE=${donePage}</@ofbizUrl>' name="editeftaccountform" style='margin: 0;'>
        <input type="hidden" name='paymentMethodId' value='${paymentMethodId}' />
    </#if>
        <input type="hidden" name="partyId" value="${partyId}"/>
        <table class="basic-table" cellspacing="0">
        <tr>
          <td class="label">${uiLabelMap.AccountingNameAccount}</td>
          <td>
            <input type="text" class='required' size="30" maxlength="60" name="nameOnAccount" value="${eftAccountData.nameOnAccount!}" />
            <span class="tooltip">${uiLabelMap.CommonRequired}</span>
          </td>
        </tr>
        <tr>
          <td class="label">${uiLabelMap.AccountingCompanyNameAccount}</td>
          <td>
            <input type="text" size="30" maxlength="60" name="companyNameOnAccount" value="${eftAccountData.companyNameOnAccount!}" />
          </td>
        </tr>
        <tr>
          <td class="label">${uiLabelMap.AccountingBankName}</td>
          <td>
            <input type="text" class='required' size="30" maxlength="60" name="bankName" value="${eftAccountData.bankName!}" />
            <span class="tooltip">${uiLabelMap.CommonRequired}</span>
          </td>
        </tr>
        <tr>
          <td class="label">${uiLabelMap.AccountingRoutingNumber}</td>
          <td>
            <input type="text" class='required' size="10" maxlength="30" name="routingNumber" value="${eftAccountData.routingNumber!}" />
            <span class="tooltip">${uiLabelMap.CommonRequired}</span>
          </td>
        </tr>
        <tr>
          <td class="label">${uiLabelMap.AccountingAccountType}</td>
          <td>
            <select name="accountType" class='required'>
              <option>${eftAccountData.accountType!}</option>
              <option></option>
              <option>${uiLabelMap.CommonChecking}</option>
              <option>${uiLabelMap.CommonSavings}</option>
            </select>
            <span class="tooltip">${uiLabelMap.CommonRequired}</span>
          </td>
        </tr>
        <tr>
          <td class="label">${uiLabelMap.AccountingAccountNumber}</td>
          <td>
            <input type="text" class='required' size="20" maxlength="40" name="accountNumber" value="${eftAccountData.accountNumber!}" />
            <span class="tooltip">${uiLabelMap.CommonRequired}</span>
          </td>
        </tr>
        <tr>
          <td class="label">${uiLabelMap.CommonDescription}</td>
          <td>
            <input type="text" class='required' size="30" maxlength="60" name="description" value="${paymentMethodData.description!}" />
            <span class="tooltip">${uiLabelMap.CommonRequired}</span>
          </td>
        </tr>
        <tr>
          <td class="label">${uiLabelMap.PartyBillingAddress}</td>
          <td>
            <#-- Removed because is confusing, can add but would have to come back here with all data populated as before...
            <a href="<@ofbizUrl>editcontactmech</@ofbizUrl>" class="smallSubmit">
              [Create New Address]</a>&nbsp;&nbsp;
            -->
            <table cellspacing="0">
            <#if curPostalAddress??>
              <tr>
                <td class="button-col">
                  <input type="radio" name="contactMechId" value="${curContactMechId}" checked="checked" />
                </td>
                <td>
                  <p><b>${uiLabelMap.PartyUseCurrentAddress}:</b></p>
                  <#list curPartyContactMechPurposes as curPartyContactMechPurpose>
                    <#assign curContactMechPurposeType = curPartyContactMechPurpose.getRelatedOne("ContactMechPurposeType", true)>
                    <p><b>${curContactMechPurposeType.get("description",locale)!}</b></p>
                    <#if curPartyContactMechPurpose.thruDate??>
                      <p>(${uiLabelMap.CommonExpire}:${curPartyContactMechPurpose.thruDate.toString()})</p>
                    </#if>
                  </#list>
                  <#if curPostalAddress.toName??><p><b>${uiLabelMap.CommonTo}:</b> ${curPostalAddress.toName}</p></#if>
                  <#if curPostalAddress.attnName??><p><b>${uiLabelMap.PartyAddrAttnName}:</b> ${curPostalAddress.attnName}</p></#if>
                  <#if curPostalAddress.address1??><p>${curPostalAddress.address1}</p></#if>
                  <#if curPostalAddress.address2??><p>${curPostalAddress.address2}</p></#if>
                  <p>${curPostalAddress.city}<#if curPostalAddress.stateProvinceGeoId?has_content>,&nbsp;${curPostalAddress.stateProvinceGeoId}</#if>&nbsp;${curPostalAddress.postalCode}</p>
                  <#if curPostalAddress.countryGeoId??><p>${curPostalAddress.countryGeoId}</p></#if>
                  <p>(${uiLabelMap.CommonUpdated}:&nbsp;${(curPartyContactMech.fromDate.toString())!})</p>
                  <#if curPartyContactMech.thruDate??><p><b>${uiLabelMap.CommonDelete}:&nbsp;${curPartyContactMech.thruDate.toString()}</b></p></#if>
                </td>
              </tr>
            <#else>
            </#if>
              <#list postalAddressInfos as postalAddressInfo>
                <#assign contactMech = postalAddressInfo.contactMech>
                <#assign partyContactMechPurposes = postalAddressInfo.partyContactMechPurposes>
                <#assign postalAddress = postalAddressInfo.postalAddress>
                <#assign partyContactMech = postalAddressInfo.partyContactMech>
                <tr>
                  <td class="button-col">
                    <input type='radio' name='contactMechId' value='${contactMech.contactMechId}' />
                  </td>
                  <td>
                    <#list partyContactMechPurposes as partyContactMechPurpose>
                      <#assign contactMechPurposeType = partyContactMechPurpose.getRelatedOne("ContactMechPurposeType", true)>
                      <p><b>${contactMechPurposeType.get("description",locale)!}</b></p>
                      <#if partyContactMechPurpose.thruDate??><p>(${uiLabelMap.CommonExpire}:${partyContactMechPurpose.thruDate})</p></#if>
                    </#list>
                    <#if postalAddress.toName??><p><b>${uiLabelMap.CommonTo}:</b> ${postalAddress.toName}</p></#if>
                    <#if postalAddress.attnName??><p><b>${uiLabelMap.PartyAddrAttnName}:</b> ${postalAddress.attnName}</p></#if>
                    <#if postalAddress.address1??><p>${postalAddress.address1}</p></#if>
                    <#if postalAddress.address2??><p>${postalAddress.address2}</p></#if>
                    <p>${postalAddress.city}<#if postalAddress.stateProvinceGeoId?has_content>,&nbsp;${postalAddress.stateProvinceGeoId}</#if>&nbsp;${postalAddress.postalCode}</p>
                    <#if postalAddress.countryGeoId??><p>${postalAddress.countryGeoId}</p></#if>
                    <p>(${uiLabelMap.CommonUpdated}:&nbsp;${(partyContactMech.fromDate.toString())!})</p>
                    <#if partyContactMech.thruDate??><p><b>${uiLabelMap.CommonDelete}:&nbsp;${partyContactMech.thruDate.toString()}</b></p></#if>
                  </td>
                </tr>
              </#list>
              <#if !postalAddressInfos?has_content && !curContactMech??>
                  <tr><td colspan='2'>${uiLabelMap.PartyNoContactInformation}.</td></tr>
              </#if>
            </table>
          </td>
        </tr>
        </table>
      </form>
      <div class="button-bar">
        <a href="<@ofbizUrl>backHome</@ofbizUrl>" class="smallSubmit">${uiLabelMap.CommonCancelDone}</a>
        <a href="javascript:document.editeftaccountform.submit()" class="smallSubmit">${uiLabelMap.CommonSave}</a>
      </div>
  </div>
</div>
<!-- end EditEftAccount.ftl -->