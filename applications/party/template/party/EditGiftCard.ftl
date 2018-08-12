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

<!-- begin EditGiftCard.ftl -->
<div class="screenlet">
  <div class="screenlet-title-bar">
    <#if !giftCard??>
      <h3>${uiLabelMap.AccountingCreateGiftCard}</h3>
    <#else>
      <h3>${uiLabelMap.AccountingEditGiftCard}</h3>
    </#if>
  </div>
  <div class="screenlet-body">
    <#if !giftCard??>
      <form method="post" action="<@ofbizUrl>createGiftCard?DONE_PAGE=${donePage}</@ofbizUrl>" name="editgiftcardform" style="margin: 0;">
    <#else>
      <form method="post" action="<@ofbizUrl>updateGiftCard?DONE_PAGE=${donePage}</@ofbizUrl>" name="editgiftcardform" style="margin: 0;">
        <input type="hidden" name="paymentMethodId" value="${paymentMethodId}" />
    </#if>
        <input type="hidden" name="partyId" value="${partyId}"/>
        <div class="button-bar">
          <a href="<@ofbizUrl>${donePage}?partyId=${partyId}</@ofbizUrl>" class="smallSubmit">${uiLabelMap.CommonCancelDone}</a>
          <a href="javascript:document.editgiftcardform.submit()" class="smallSubmit">${uiLabelMap.CommonSave}</a>
        </div>
        <table class="basic-table" cellspacing="0">
        <tr>
          <td class="label">${uiLabelMap.AccountingCardNumber}</td>
          <td>
            <input type="text" size="20" maxlength="60" name="cardNumber" value="${giftCardData.cardNumber!}" />
          </td>
        </tr>
        <tr>
          <td class="label">${uiLabelMap.AccountingPinNumber}</td>
          <td>
            <input type="text" size="10" maxlength="60" name="pinNumber" value="${giftCardData.pinNumber!}" />
          </td>
        </tr>
        <tr>
          <td class="label">${uiLabelMap.CommonExpireDate}</td>
          <td>
            <#assign expMonth = "">
            <#assign expYear = "">
            <#if giftCardData?? && giftCardData.expireDate??>
              <#assign expDate = giftCard.expireDate>
              <#if (expDate?? && expDate.indexOf("/") > 0)>
                <#assign expMonth = expDate.substring(0,expDate.indexOf("/"))>
                <#assign expYear = expDate.substring(expDate.indexOf("/")+1)>
              </#if>
            </#if>
            <select name="expMonth" onchange="javascript:makeExpDate();">
              <#if giftCardData?has_content && expMonth?has_content>
                <#assign ccExprMonth = expMonth>
              <#else>
                <#assign ccExprMonth = requestParameters.expMonth!>
              </#if>
              <#if ccExprMonth?has_content>
                <option value="${ccExprMonth!}">${ccExprMonth!}</option>
              </#if>
              ${screens.render("component://common/widget/CommonScreens.xml#ccmonths")}
            </select>
            <select name="expYear" onchange="javascript:makeExpDate();">
              <#if giftCard?has_content && expYear?has_content>
                <#assign ccExprYear = expYear>
              <#else>
                <#assign ccExprYear = requestParameters.expYear!>
              </#if>
              <#if ccExprYear?has_content>
                <option value="${ccExprYear!}">${ccExprYear!}</option>
              </#if>
              ${screens.render("component://common/widget/CommonScreens.xml#ccyears")}
            </select>
          </td>
        </tr>
        <tr>
          <td class="label">${uiLabelMap.CommonDescription}</td>
          <td>
            <input type="text" size="30" maxlength="60" name="description" value="${paymentMethodData.description!}" />
          </td>
        </tr>
        </table>
        <div class="button-bar">
          <a href="<@ofbizUrl>${donePage}?partyId=${partyId}</@ofbizUrl>" class="smallSubmit">${uiLabelMap.CommonCancelDone}</a>
          <input type="submit" value="${uiLabelMap.CommonSave}"/>
        </div>
      </form>
  </div>
</div>
<!-- end EditGiftCard.ftl -->
