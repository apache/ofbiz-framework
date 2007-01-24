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

<#if !creditCard?has_content>
  <#assign creditCard = requestParameters>
</#if>
<tr><td colspan="3"><hr class="sepbar"/></td></tr>
<tr>
  <td width="26%" align="right" valign="top"><div class="tableheadtext">${uiLabelMap.AccountingCreditCardInformation}</div></td>
  <td width="5">&nbsp;</td>
  <td width="74%"><input type="hidden" name="paymentMethodId" value="${parameters.paymentMethodId?if_exists}"/>&nbsp;</td>
</tr>
<tr>
  <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.AccountingCompanyNameOnCard}</div></td>
  <td width="5">&nbsp;</td>
  <td width="74%"><input type="text" class='inputBox' size="30" maxlength="60" name="companyNameOnCard" value="${creditCard.companyNameOnCard?if_exists}"/></td>
</tr>
<tr>
  <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.AccountingPrefixCard}</div></td>
  <td width="5">&nbsp;</td>
  <td width="74%">
  <select name="titleOnCard" class="selectBox">
    <option value="">Select One</option>
    <option<#if ((creditCard.titleOnCard)?default("") == "Mr.")> selected</#if>>${uiLabelMap.CommonTitleMr}</option>
    <option<#if ((creditCard.titleOnCard)?default("") == "Mrs.")> selected</#if>>${uiLabelMap.CommonTitleMrs}</option>
    <option<#if ((creditCard.titleOnCard)?default("") == "Ms.")> selected</#if>>${uiLabelMap.CommonTitleMs}</option>
    <option<#if ((creditCard.titleOnCard)?default("") == "Dr.")> selected</#if>>${uiLabelMap.CommonTitleDr}</option>
  </select>
  </td>
</tr>
<tr>
  <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.AccountingFirstNameCard}</div></td>
  <td width="5">&nbsp;</td>
  <td width="74%">
    <input type="text" class="inputBox" size="20" maxlength="60" name="firstNameOnCard" value="${(creditCard.firstNameOnCard)?if_exists}"/>*
  </td>
</tr>
<tr>
  <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.AccountingMiddleNameCard}</div></td>
  <td width="5">&nbsp;</td>
  <td width="74%">
    <input type="text" class="inputBox" size="15" maxlength="60" name="middleNameOnCard" value="${(creditCard.middleNameOnCard)?if_exists}"/>
  </td>
</tr>
<tr>
  <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.AccountingLastNameCard}</div></td>
  <td width="5">&nbsp;</td>
  <td width="74%">
    <input type="text" class="inputBox" size="20" maxlength="60" name="lastNameOnCard" value="${(creditCard.lastNameOnCard)?if_exists}"/>*
  </td>
</tr>
<tr>
  <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.AccountingSuffixCard}</div></td>
  <td width="5">&nbsp;</td>
  <td width="74%">
    <select name="suffixOnCard" class="selectBox">
      <option value="">Select One</option>
      <option<#if ((creditCard.suffixOnCard)?default("") == "Jr.")> selected</#if>>Jr.</option>
      <option<#if ((creditCard.suffixOnCard)?default("") == "Sr.")> selected</#if>>Sr.</option>
      <option<#if ((creditCard.suffixOnCard)?default("") == "I")> selected</#if>>I</option>
      <option<#if ((creditCard.suffixOnCard)?default("") == "II")> selected</#if>>II</option>
      <option<#if ((creditCard.suffixOnCard)?default("") == "III")> selected</#if>>III</option>
      <option<#if ((creditCard.suffixOnCard)?default("") == "IV")> selected</#if>>IV</option>
      <option<#if ((creditCard.suffixOnCard)?default("") == "V")> selected</#if>>V</option>
    </select>
  </td>
</tr>
<tr>
  <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.AccountingCardType}</div></td>
  <td width="5">&nbsp;</td>
  <td width="74%">
  <select name="cardType" class="selectBox">
    <#if creditCard.cardType?exists>
    <option>${creditCard.cardType}</option>
    <option value="${creditCard.cardType}">---</option>
    </#if>
    ${screens.render("component://common/widget/CommonScreens.xml#cctypes")}
  </select>*
  </td>
</tr>
<tr>
  <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.AccountingCardNumber}</div></td>
  <td width="5">&nbsp;</td>
  <td width="74%"><input type="text" class="inputBox" size="20" maxlength="30" name="cardNumber" value="${creditCard.cardNumber?if_exists}"/>*</td>
</tr>
<tr>
  <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.AccountingExpirationDate}</div></td>
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
  </select>*
  </td>
</tr>
<tr>
  <td width="26%" align="right" valign=middle><div class="tabletext">${uiLabelMap.CommonDescription}</div></td>
  <td width="5">&nbsp;</td>
  <td width="74%">
    <input type="text" class="inputBox" size="20" maxlength="30" name="description" value="${creditCard.description?if_exists}"/>
  </td>
</tr>
