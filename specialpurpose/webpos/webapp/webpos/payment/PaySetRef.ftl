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

<div id="panel">
  <form method="post" action="<@ofbizUrl>PaySetRef</@ofbizUrl>" name="PaySetRefForm">
    <table border="0">
      <tr>
        <td>&nbsp;</td>
        <td>&nbsp;</td>
        <td><b><u>${uiLabelMap.WebPosPaymentSetRef}</u></b></td>
      </tr>
      <#if (checkAmount?default(0) > 0.00)>
      <tr>
        <td><b>${(paymentCheck.get("description", locale))?if_exists}</b></td>
        <td align="right"><@ofbizCurrency amount=checkAmount isoCode=shoppingCart.getCurrency()/></td>
        <td align="center"><input type="text" name="refNumCheck" id="refNumCheck" value="${requestParameters.refNumCheck?if_exists}"/></td>
      </tr>
      </#if>
      <#if (giftAmount?default(0) > 0.00)>
      <tr>
        <td><b>${(paymentGift.get("description", locale))?if_exists}</b></td>
        <td align="right"><@ofbizCurrency amount=giftAmount isoCode=shoppingCart.getCurrency()/></td>
        <td align="center"><input type="text" name="refNumGift" id="refNumGift" value="${requestParameters.refNumGift?if_exists}"/></td>
      </tr>
      </#if>
      <#if (creditAmount?default(0) > 0.00)>
      <tr>
        <td><b>${(paymentCredit.get("description", locale))?if_exists}</b></td>
        <td align="right"><@ofbizCurrency amount=creditAmount isoCode=shoppingCart.getCurrency()/></td>
        <td align="center"><input type="text" name="refNumCredit" id="refNumCredit" value="${requestParameters.refNumCredit?if_exists}"/></td>
      </tr>
      </#if>
      <tr>
        <td colspan="3">&nbsp;</td>
      </tr>
      <tr>
        <td colspan="3" align="center">
          <input type="submit" value="${uiLabelMap.CommonConfirm}" name="confirm"/>
          <input type="submit" value="${uiLabelMap.CommonCancel}"/>
        </td>
      </tr>
    </table>
  </form>
</div>
<script language="javascript" type="text/javascript">
    document.PaySetRefForm.refNumCheck.focus();
</script>