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
<div id="payCreditCard" style="display:none">
  <table border="0" width="100%">
    <tr>
      <td colspan="4">&nbsp;</td>
    </tr>
    <tr>
      <td width="100%" align="center" colspan="4">
        <b>${uiLabelMap.WebPosTransactionTotalDue} <span id="creditCardTotalDue"/></b>
      </td>
    </tr>
    <tr>
      <td width="100%" align="center" colspan="4">
        <b>${uiLabelMap.WebPosPayCreditCardTotal} <span id="creditCardTotalPaid"/></b>
        <a id="removeCreditCardTotalPaid" href="javascript:void(0);"><img src="/images/collapse.gif"></a>
      </td>
    </tr>
    <tr>
      <td width="100%" align="center" colspan="4">
        ${uiLabelMap.WebPosPayCreditCardSwipe} <input type="checkbox" id="swipeCard" name="swipeCard" value="Y" checked="checked">
      </td>
    </tr>
    <tr id="showSwipeData">
      <td width="25%" align="right">
        ${uiLabelMap.WebPosPayCreditCardSwipeData}
      </td>
      <td width="75%" align="left" colspan="3">
        <input type="password" id="swipeData" name="swipeData" size="50" value=""/>
      </td>
    </tr>
    <tr id="showCreditCardData1" style="display:none">
      <td width="25%" align="right">
        ${uiLabelMap.WebPosPayCreditCardFirstName}
      </td>
      <td width="25%" align="left">
        <input type="text" id="firstName" name="firstName" size="20" maxlength="60" value=""/>
      </td>
      <td width="25%" align="right">
        ${uiLabelMap.WebPosPayCreditCardLastName}
      </td>
      <td width="25%" align="left">
        <input type="text" id="lastName" name="lastName" size="20" maxlength="60" value=""/></td>
      </td>
    </tr>
    <tr id="showCreditCardData2" style="display:none">
      <td width="25%" align="right">
        ${uiLabelMap.WebPosPayCreditCardNum}
      </td>
      <td width="25%" align="left">
        <input type="text" id="cardNum" name="cardNum" size="20" maxlength="30" value=""/>
      </td>
      <td width="25%" align="right">
        ${uiLabelMap.WebPosPayCreditCardExp}
      </td>
      <td width="25%" align="left">
        <#assign expMonth = "">
        <#assign expYear = "">
        <select id="expMonth" name="expMonth">
          <option value="01" <#if expMonth?default('') == '01'> selected="selected"</#if>>${uiLabelMap.CommonJanuary}</option>
          <option value="02" <#if expMonth?default('') == '02'> selected="selected"</#if>>${uiLabelMap.CommonFebruary}</option>
          <option value="03" <#if expMonth?default('') == '03'> selected="selected"</#if>>${uiLabelMap.CommonMarch}</option>
          <option value="04" <#if expMonth?default('') == '04'> selected="selected"</#if>>${uiLabelMap.CommonApril}</option>
          <option value="05" <#if expMonth?default('') == '05'> selected="selected"</#if>>${uiLabelMap.CommonMay}</option>
          <option value="06" <#if expMonth?default('') == '06'> selected="selected"</#if>>${uiLabelMap.CommonJune}</option>
          <option value="07" <#if expMonth?default('') == '07'> selected="selected"</#if>>${uiLabelMap.CommonJuly}</option>
          <option value="08" <#if expMonth?default('') == '08'> selected="selected"</#if>>${uiLabelMap.CommonAugust}</option>
          <option value="09" <#if expMonth?default('') == '09'> selected="selected"</#if>>${uiLabelMap.CommonSeptember}</option>
          <option value="10" <#if expMonth?default('') == '10'> selected="selected"</#if>>${uiLabelMap.CommonOctober}</option>
          <option value="11" <#if expMonth?default('') == '11'> selected="selected"</#if>>${uiLabelMap.CommonNovember}</option>
          <option value="12" <#if expMonth?default('') == '12'> selected="selected"</#if>>${uiLabelMap.CommonDecember}</option>
        </select>
        <select id="expYear" name="expYear">
        <#assign ccExprYear = requestParameters.expYear!>
        <#if ccExprYear?has_content>
          <option value="${ccExprYear!}">${ccExprYear!}</option>
        </#if>
        ${screens.render("component://common/widget/CommonScreens.xml#ccyears")}
        </select>
      </td>
    </tr>
    <tr>
      <td width="25%" align="right">
        ${uiLabelMap.WebPosPayCreditCardType}
      </td>
      <td width="25%" align="left">
        <select id="cardType" name="cardType">
          ${screens.render("component://common/widget/CommonScreens.xml#cctypes")}
        </select>
      </td>
      <td width="25%" align="right">
        ${uiLabelMap.WebPosPayCreditCardSecurityCode}
      </td>
      <td width="25%" align="left">
        <input type="text" id="securityCode" name="securityCode" size="5" maxlength="10" value=""/>
        <input type="hidden" name="track2" id="track2"/>
      </td>
    </tr>
    <tr>
      <td width="25%" align="right">
        ${uiLabelMap.WebPosPayCreditCard}
      </td>
      <td width="25%" align="left">
        <input type="text" id="amountCreditCard" name="amountCreditCard" size="10" value=""/>
      </td>
      <td width="25%" align="right">
        ${uiLabelMap.WebPosPayCreditCardPostalCode}
      </td>
      <td width="25%" align="left">
        <input type="text" id="postalCode" name="postalCode" value=""/>
      </td>
    </tr>
    <tr>
      <td width="25%" align="right">
        ${uiLabelMap.WebPosPayCreditCardRefNum}
      </td>
      <td width="25%" align="left">
        <input type="text" id="refNumCreditCard" name="refNum" size="10" value=""/>
      </td>
      <td width="50%" colspan="2">&nbsp;</td>
    </tr>
    <tr>
      <td width="100%" colspan="4">&nbsp;</td>
    </tr>
    <tr>
      <td colspan="4" align="center">
        <input type="submit" value="${uiLabelMap.CommonConfirm}" id="payCreditCardConfirm"/>
        <input type="submit" value="${uiLabelMap.CommonCancel}" id="payCreditCardCancel"/>
      </td>
    </tr>
    <tr>
      <td colspan="4"><div class="errorPosMessage"><span id="payCreditCardFormServerError"/></div></td>
    </tr>
  </table>
</div>