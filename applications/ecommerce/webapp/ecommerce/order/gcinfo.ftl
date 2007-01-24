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

<#-- gift card fields -->
  <input type="hidden" name="addGiftCard" value="Y"/>
  <#assign giftCard = giftCard?if_exists>
  <#if paymentMethodTypeId?if_exists != "GIFT_CARD">
    <tr>
      <td colspan="3"><hr class="sepbar"/></td>
    </tr>
  </#if>
  <tr>
    <td width="26%" align="right" valign="top"><div class="tableheadtext">${uiLabelMap.AccountingGiftCardInformation}</div></td>
    <td width="5">&nbsp;</td>
    <td width="74%">&nbsp;</td>
  </tr>
  <tr>
    <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.AccountingGiftCardNumber}</div></td>
    <td width="5">&nbsp;</td>
    <td width="74%">
      <input type="text" class="inputBox" size="20" maxlength="60" name="giftCardNumber" value="${giftCard.cardNumber?if_exists}"/>
    *</td>
  </tr>
  <tr>
    <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.AccountingPINNumber}</div></td>
    <td width="5">&nbsp;</td>
    <td width="74%">
      <input type="text" class="inputBox" size="10" maxlength="60" name="giftCardPin" value="${giftCard.pinNumber?if_exists}"/>
    *</td>
  </tr>
  <tr>
    <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.CommonDescription}</div></td>
    <td width="5">&nbsp;</td>
    <td width="74%">
      <input type="text" class="inputBox" size="30" maxlength="60" name="description" value="${giftCard.description?if_exists}"/>
    </td>
  </tr>
  <#if paymentMethodTypeId?if_exists != "GIFT_CARD">
    <tr>
      <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.AccountingAmountToUse}</div></td>
      <td width="5">&nbsp;</td>
      <td width="74%">
        <input type="text" class="inputBox" size="5" maxlength="10" name="giftCardAmount" value="${giftCard.pinNumber?if_exists}"/>
      *</td>
    </tr>
  </#if>
