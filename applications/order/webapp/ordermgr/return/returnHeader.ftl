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

<div class="screenlet">
    <div class="screenlet-title-bar">
        <ul>
            <li class="h3">${uiLabelMap.PageTitleReturnHeader}</li>
        </ul>
        <br class="clear"/>
    </div>
    <div class="screenlet-body">
        <#if returnHeader?exists>
            <form name="returnhead" method="post" action="<@ofbizUrl>updateReturn</@ofbizUrl>">
            <input type="hidden" name="returnId" value="${returnHeader.returnId}">
            <input type="hidden" name="returnHeaderTypeId" value="CUSTOMER_RETURN"/>
            <input type="hidden" name="currentStatusId" value="${returnHeader.statusId?if_exists}">
        <#else>
            <form name="returnhead" method="post" action="<@ofbizUrl>createReturn</@ofbizUrl>">
            <input type="hidden" name="returnHeaderTypeId" value="CUSTOMER_RETURN"/>
        </#if>

        <table cellspacing="0" class="basic-table">
          <#if returnHeader?exists>
          <tr>
            <td width='14%'>&nbsp;</td>
            <td width='6%' align='right' nowrap class="label">${uiLabelMap.OrderReturnId}</td>
            <td width='6%'>&nbsp;</td>
            <td width='74%' align='left'>${returnHeader.returnId}</td>
          </tr>
          </#if>
          <tr>
            <td width='14%'>&nbsp;</td>
            <td width='6%' align='right' nowrap class="label">${uiLabelMap.CommonCurrency}</td>
            <td width='6%'>&nbsp;</td>
            <td width='74%'>
          <#if returnHeader?exists>
              ${returnHeader.currencyUomId?if_exists}
          <#else>
             <select name="currencyUomId">
                <#if (orderHeader?has_content) && (orderHeader.currencyUom?has_content)>
                  <option value="${orderHeader.currencyUom}" selected>${orderHeader.getRelatedOne("Uom").getString("description",locale)}</option>
                  <option value="${orderHeader.currencyUom}">---</option>
                <#elseif defaultCurrency?has_content>
                  <option value="${defaultCurrency.uomId}" selected>${defaultCurrency.getString("description")}</option>
                  <option value="${defaultCurrency.uomId}">---</option>
                </#if>
                <#if currencies?has_content>
                  <#list currencies as currency>
                    <option value="${currency.uomId}">${currency.get("description",locale)}</option>
                  </#list>
                </#if>
             </select>
          </#if>
            </td>
          </tr>
          <tr>
            <td width='14%'>&nbsp;</td>
            <td width='6%' align='right' nowrap class="label">${uiLabelMap.OrderEntryDate}</td>
            <td width='6%'>&nbsp;</td>
            <td width='74%'>
              <#if returnInfo.entryDate?exists>
                <#assign entryDate = returnInfo.get("entryDate").toString()>
              </#if>
              <input type='text' size='25' name='entryDate' value='${entryDate?if_exists}'>
              <a href="javascript:call_cal(document.returnhead.entryDate, '');"><img src='/images/cal.gif' width='16' height='16' border='0' alt='Calendar'></a>
            </td>
          </tr>
          <tr>
            <td width='14%'>&nbsp;</td>
            <td width='6%' align='right' nowrap class="label">${uiLabelMap.OrderReturnFromParty}</td>
            <td width='6%'>&nbsp;</td>
            <td width='74%'>
              <input type='text' size='20' name='fromPartyId' value='${returnInfo.fromPartyId?if_exists}'>
              <a href="javascript:call_fieldlookup2(document.returnhead.fromPartyId,'LookupPartyName');">
                <img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt="${uiLabelMap.CommonClickHereForFieldLookup}"/>
              </a>
            </td>
          </tr>
          <tr>
            <td width='14%'>&nbsp;</td>
            <td width='6%' align='right' nowrap class="label">${uiLabelMap.OrderReturnToFacility}</td>
            <td width='6%'>&nbsp;</td>
            <td width='74%'>
              <select name='destinationFacilityId'>
                <#if currentFacility?exists>
                  <option value="${currentFacility.facilityId}">${currentFacility.facilityName?default(currentFacility.facilityId)}</option>
                  <option value="${currentFacility.facilityId}">---</option>
                </#if>
                <option value="">${uiLabelMap.FacilityNoFacility}</option>
                <#list facilityList as facility>
                  <option value="${facility.facilityId}" <#if (facilityList?size == 1)>selected</#if>>${facility.facilityName?default(facility.facilityId)}</option>
                </#list>
            </td>
          </tr>
          <tr>
            <td width='14%'>&nbsp;</td>
            <td width='6%' align='right' nowrap class="label">${uiLabelMap.AccountingBillingAccount}</td>
            <td width='6%'>&nbsp;</td>
            <td width='74%'>
              <#if billingAccountList?has_content>
                <select name='billingAccountId'>
                  <#if currentAccount?exists>
                    <option value="${currentAccount.billingAccountId}">${currentAccount.billingAccountId}: ${currentAccount.description?if_exists}</option>
                    <option value="${currentAccount.billingAccountId}">---</option>
                  </#if>
                  <option value="">${uiLabelMap.AccountingNewBillingAccount}</option>
                  <#list billingAccountList as ba>
                    <option value="${ba.billingAccountId}">${ba.billingAccountId}: ${ba.description?if_exists}</option>
                  </#list>
                </select>
              <#else>
                <input type='text' size='20' name='billingAccountId'>
              </#if>
            </td>
          </tr>
          <tr>
            <td width='14%'>&nbsp;</td>
            <td width='6%' align='right' nowrap class="label">${uiLabelMap.FormFieldTitle_paymentMethodId}</td>
            <td width='6%'>&nbsp;</td>
            <td width='74%'>
              <#if creditCardList?exists || eftAccountList?exists>
                <select name='paymentMethodId'>
                  <#if currentCreditCard?exists>
                    <option value="${currentCreditCard.paymentMethodId}">CC:&nbsp;${Static["org.ofbiz.party.contact.ContactHelper"].formatCreditCard(currentCreditCard)}</option>
                  </#if>
                  <#if currentEftAccount?exists>
                    <option value="${currentEftAccount.paymentMethodId}">EFT:&nbsp;${currentEftAccount.nameOnAccount?if_exists}, ${currentEftAccount.accountNumber?if_exists}</option>
                  </#if>
                  <option value=""></option>
                  <#if creditCardList?has_content>
                    <#list creditCardList as creditCardPm>
                      <#assign creditCard = creditCardPm.getRelatedOne("CreditCard")>
                      <option value="${creditCard.paymentMethodId}">CC:&nbsp;${Static["org.ofbiz.party.contact.ContactHelper"].formatCreditCard(creditCard)}</option>
                    </#list>
                  </#if>
                  <#if eftAccountList?has_content>
                    <#list eftAccountList as eftAccount>
                      <option value="${eftAccount.paymentMethodId}">EFT:&nbsp;${eftAccount.nameOnAccount?if_exists}, ${eftAccount.accountNumber?if_exists}</option>
                    </#list>
                  </#if>
                </select>
              <#else>
                <input type='text' size='20' name='paymentMethodId' value="${returnHeader?if_exists.paymentMethodId?if_exists}"/>
              </#if>
              <#if (returnHeader.fromPartyId)?has_content>
                <a href="/partymgr/control/editcreditcard?partyId=${returnHeader.fromPartyId}${externalKeyParam}" target="partymgr" class="smallSubmit">${uiLabelMap.AccountingCreateNewCreditCard}</a>
              </#if>
            </td>
          </tr>
          <tr>
            <td width='14%'>&nbsp;</td>
            <td width='6%' align='right' nowrap class="label">${uiLabelMap.OrderReturnNeedsAutoReceive}</td>
            <td width='6%'>&nbsp;</td>
            <td width='74%'>
              <select name='needsInventoryReceive'>
                <#if needsInventoryReceive?exists>
                  <#if "Y" == needsInventoryReceive>
                    <option selected="selected" value="${needsInventoryReceive}">${uiLabelMap.CommonYes}</option>
                  <#elseif "N" == needsInventoryReceive>
                    <option selected="selected" value="${needsInventoryReceive}">${uiLabelMap.CommonNo}</option>
                  </#if>
                  <option value="${needsInventoryReceive}">---</option>
                </#if>
                <option value="Y">${uiLabelMap.CommonYes}</option>
                <option value="N">${uiLabelMap.CommonNo}</option>
              </select>
            </td>
          </tr>
        <#if returnHeader?has_content>
          <tr>
            <td width='14%'>&nbsp;</td>
            <td width='6%' align='right' nowrap class="label">${uiLabelMap.CommonReturnStatus}</td>
            <td width='6%'>&nbsp;</td>
            <td width='74%'>
              <select name="statusId">
                <#if currentStatus?exists>
                  <option value="${currentStatus.statusId}">${currentStatus.get("description",locale)}</option>
                  <option value="${currentStatus.statusId}">---</option>
                </#if>
                <#list returnStatus as status>
                  <option value="${status.statusIdTo}">${status.get("transitionName",locale)}</option>
                </#list>
              </select>
            </td>
          </tr>
          <tr>
            <td width='14%'>&nbsp;</td>
            <td width='6%' align='right' nowrap class="label">${uiLabelMap.FormFieldTitle_createdBy}</td>
            <td width='6%'>&nbsp;</td>
            <td width='74%'>${returnHeader.createdBy?default("Unknown")}</td>
          </tr>
          <tr>
            <td width='14%'>&nbsp;</td>
            <td width='6%' align='right' valign='top' nowrap class="label">${uiLabelMap.OrderReturnFromAddress}</td>
            <td width='6%'>&nbsp;</td>
            <td width='74%'>
              <div>
              <#if (addressEditable)>
                <#list addresses as address >
                  <@displayAddress postalAddress = address.postalAddress editable = true/>
                </#list>
                <input type='radio' name="originContactMechId" value="" <#if (!postalAddressFrom?has_content)> checked="checked"</#if>>${uiLabelMap.CommonNoAddress}
              <#else>
                 <#if (postalAddressFrom?has_content)>
                   <@displayAddress postalAddress = postalAddressFrom editable = false />
                 <#else>
                   ${uiLabelMap.CommonNoAddress}
                 </#if>
              </#if>
              </div>
            </td>
          </tr>
          <tr>
            <td width='14%'>&nbsp;</td>
            <td width='6%' align='right' valign='top' nowrap class="label">${uiLabelMap.OrderReturnToAddress}</td>
            <td width='6%'>&nbsp;</td>
            <td width='74%'>
              <#if (postalAddressTo?has_content)>
                <@displayAddress postalAddress = postalAddressTo editable=false />
              </#if>
            </td>
          </tr>
          <tr>
            <td width='14%'>&nbsp;</td>
            <td width='6%'>&nbsp;</td>
            <td width='6%'>&nbsp;</td>
            <td width='74%'>
              <input type="submit" value="${uiLabelMap.CommonUpdate}"/>
            </td>
          </tr>
        <#else>
          <input type="hidden" name="statusId" value="RETURN_REQUESTED">
          <tr>
            <td width='14%'>&nbsp;</td>
            <td width='6%'>&nbsp;</td>
            <td width='6%'>&nbsp;</td>
            <td width='74%'>
              <input type="submit" value="${uiLabelMap.CommonCreateNew}"/>
            </td>
          </tr>
        </#if>
        </table>
        <#macro displayAddress postalAddress editable>
            <#if postalAddress?has_content>
                    <div>
                      <#if (editable)>
                        <input type='radio' name="originContactMechId" value="${postalAddress.contactMechId?if_exists}"
                          <#if ( postalAddressFrom?has_content && postalAddressFrom.contactMechId?default("") == postalAddress.contactMechId)>checked="checked"</#if>>
                      </#if>
                      <#if postalAddress.toName?has_content><span class="label">${uiLabelMap.CommonTo}</span>&nbsp;${postalAddress.toName}<br/></#if>
                      <#if postalAddress.attnName?has_content><span class="label">${uiLabelMap.CommonAttn}</span>&nbsp;${postalAddress.attnName}<br/></#if>
                      <#if postalAddress.address1?has_content>&nbsp;&nbsp;&nbsp;&nbsp;${postalAddress.address1}<br/></#if>
                      <#if postalAddress.address2?has_content>&nbsp;&nbsp;&nbsp;&nbsp;${postalAddress.address2}<br/></#if>
                      <#if postalAddress.city?has_content>&nbsp;&nbsp;&nbsp;&nbsp;${postalAddress.city}</#if>
                      <#if postalAddress.stateProvinceGeoId?has_content>&nbsp;${postalAddress.stateProvinceGeoId}</#if>
                      <#if postalAddress.postalCode?has_content>&nbsp;${postalAddress.postalCode}</#if>
                      <#if postalAddress.countryGeoId?has_content><br/>&nbsp;&nbsp;&nbsp;&nbsp;${postalAddress.countryGeoId}</#if>
                    </div>
            </#if>
        </#macro>
    </div>
</div>