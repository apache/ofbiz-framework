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
  <p><h3>${uiLabelMap.AccountingCardInfoNotBelongToYou}.</h3></p>
&nbsp;<a href="<@ofbizUrl>authview/${donePage}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonBack}]</a>
<#else>
    <#if !giftCard?exists>
      <p class="head1">${uiLabelMap.AccountingAddNewGiftCard}</p>
      <form method="post" action="<@ofbizUrl>createGiftCard?DONE_PAGE=${donePage}</@ofbizUrl>" name="editgiftcardform" style="margin: 0;">
    <#else>
      <p class="head1">${uiLabelMap.AccountingEditGiftCard}</p>
      <form method="post" action="<@ofbizUrl>updateGiftCard?DONE_PAGE=${donePage}</@ofbizUrl>" name="editgiftcardform" style="margin: 0;">
        <input type="hidden" name="paymentMethodId" value="${paymentMethodId}">
    </#if>
    &nbsp;<a href="<@ofbizUrl>${donePage}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonCancelDone}]</a>
    &nbsp;<a href="javascript:document.editgiftcardform.submit()" class="buttontext">[${uiLabelMap.CommonSave}]</a>

    <table width="90%" border="0" cellpadding="2" cellspacing="0">
    <tr>
      <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.AccountingCardNumber}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <#if giftCardData?has_content && giftCardData.cardNumber?has_content>
          <#assign pcardNumberDisplay = "">
          <#assign pcardNumber = giftCardData.cardNumber?if_exists>
          <#if pcardNumber?has_content>
            <#assign psize = pcardNumber?length - 4>
            <#if 0 < psize>
              <#list 0 .. psize-1 as foo>
                <#assign pcardNumberDisplay = pcardNumberDisplay + "*">
              </#list>
              <#assign pcardNumberDisplay = pcardNumberDisplay + pcardNumber[psize .. psize + 3]>
            <#else>
              <#assign pcardNumberDisplay = pcardNumber>
            </#if>
          </#if>
        </#if>
        <input type="text" class="inputBox" size="20" maxlength="60" name="cardNumber" value="${pcardNumberDisplay?if_exists}">
      </td>
    </tr>
    <tr>
      <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.AccountingPINNumber}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <input type="password" class="inputBox" size="10" maxlength="60" name="pinNumber" value="${giftCardData.pinNumber?if_exists}">
      </td>
    </tr>
    <tr>
      <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.AccountingExpirationDate}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <#assign expMonth = "">
        <#assign expYear = "">
        <#if giftCardData?exists && giftCardData.expireDate?exists>
          <#assign expDate = giftCard.expireDate>
          <#if (expDate?exists && expDate.indexOf("/") > 0)>
            <#assign expMonth = expDate.substring(0,expDate.indexOf("/"))>
            <#assign expYear = expDate.substring(expDate.indexOf("/")+1)>
          </#if>
        </#if>
        <select name="expMonth" class="selectBox" onchange="javascript:makeExpDate();">
          <#if giftCardData?has_content && expMonth?has_content>
            <#assign ccExprMonth = expMonth>
          <#else>
            <#assign ccExprMonth = requestParameters.expMonth?if_exists>
          </#if>
          <#if ccExprMonth?has_content>
            <option value="${ccExprMonth?if_exists}">${ccExprMonth?if_exists}</option>
          </#if>
          ${screens.render("component://common/widget/CommonScreens.xml#ccmonths")}
        </select>
        <select name="expYear" class="selectBox" onchange="javascript:makeExpDate();">
          <#if giftCard?has_content && expYear?has_content>
            <#assign ccExprYear = expYear>
          <#else>
            <#assign ccExprYear = requestParameters.expYear?if_exists>
          </#if>
          <#if ccExprYear?has_content>
            <option value="${ccExprYear?if_exists}">${ccExprYear?if_exists}</option>
          </#if>
          ${screens.render("component://common/widget/CommonScreens.xml#ccyears")}
        </select>
      </td>
    </tr>
    <tr>
      <td width="26%" align="right" valign="top"><div class="tabletext">${uiLabelMap.CommonDescription}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <input type="text" class="inputBox" size="30" maxlength="60" name="description" value="${paymentMethodData.description?if_exists}">
      </td>
    </tr>
  </table>
  </form>

  &nbsp;<a href="<@ofbizUrl>${donePage}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonCancelDone}]</a>
  &nbsp;<a href="javascript:document.editgiftcardform.submit()" class="buttontext">[${uiLabelMap.CommonSave}]</a>
</#if>
