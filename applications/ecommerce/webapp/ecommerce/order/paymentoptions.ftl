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
    <div class="screenlet-header">
        <div class='boxhead'>&nbsp;${uiLabelMap.AccountingPaymentInformation}</div>
    </div>
    <div class="screenlet-body">
        
          <#-- initial screen show a list of options -->
          <form method="post" action="<@ofbizUrl>setPaymentInformation</@ofbizUrl>" name="${parameters.formNameValue}">
            <table width="100%" border="0" cellpadding="1" cellspacing="0">
              <#if productStorePaymentMethodTypeIdMap.GIFT_CARD?exists>
              <tr>
                <td width='5%' nowrap><input type="checkbox" name="addGiftCard" value="Y" <#if addGiftCard?exists && addGiftCard == "Y">checked</#if>/></td>
                <td width='95%' nowrap><div class="tabletext">${uiLabelMap.AccountingCheckGiftCard}</div></td>
              </tr>
              <tr><td colspan="2"><hr class="sepbar"/></td></tr>
              </#if>
              <#if productStorePaymentMethodTypeIdMap.EXT_OFFLINE?exists>
              <tr>
                <td width='5%' nowrap><input type="radio" name="paymentMethodTypeId" value="EXT_OFFLINE" <#if paymentMethodTypeId?exists && paymentMethodTypeId == "EXT_OFFLINE">checked</#if>/></td>
                <td width='95%'nowrap><div class="tabletext">${uiLabelMap.OrderPaymentOfflineCheckMoney}</div></td>
              </tr>
              <tr><td colspan="2"><hr class="sepbar"/></td></tr>
              </#if>
              <#if productStorePaymentMethodTypeIdMap.CREDIT_CARD?exists>
              <tr>
                <td width='5%' nowrap><input type="radio" name="paymentMethodTypeId" value="CREDIT_CARD" <#if paymentMethodTypeId?exists && paymentMethodTypeId == "CREDIT_CARD">checked</#if>/></td>
                <td width='95%' nowrap><div class="tabletext">${uiLabelMap.AccountingVisaMastercardAmexDiscover}</div></td>
              </tr>
              <tr><td colspan="2"><hr class="sepbar"/></td></tr>
              </#if>
              <#if productStorePaymentMethodTypeIdMap.EFT_ACCOUNT?exists>
              <tr>
                <td width='5%' nowrap><input type="radio" name="paymentMethodTypeId" value="EFT_ACCOUNT" <#if paymentMethodTypeId?exists && paymentMethodTypeId == "EFT_ACCOUNT">checked</#if>/></td>
                <td width='95%' nowrap><div class="tabletext">${uiLabelMap.AccountingAHCElectronicCheck}</div></td>
              </tr>
              </#if>
              <tr>
                <td align="center" colspan="2">
                  <input type="submit" class="smallsubmit" value="${uiLabelMap.CommonContinue}">
                </td>
              </tr>
            </table>
          </form>
    </div>
</div>
