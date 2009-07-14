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


  <h3>${uiLabelMap.AccountingPaymentInformation}</h3>
  <#-- initial screen show a list of options -->
  <form id="editPaymentOptions" method="post" action="<@ofbizUrl>setPaymentInformation</@ofbizUrl>" name="${parameters.formNameValue}">
     <fieldset>
       <#if productStorePaymentMethodTypeIdMap.GIFT_CARD?exists>
         <div>
           <input type="checkbox" name="addGiftCard" value="Y" <#if addGiftCard?exists && addGiftCard == "Y">checked</#if>/>
           <label for="addGiftCard">${uiLabelMap.AccountingCheckGiftCard}</label>
         </div>
       </#if>
       <#if productStorePaymentMethodTypeIdMap.EXT_OFFLINE?exists>
         <div>
           <input type="radio" id="paymentMethodTypeId_EXT_OFFLINE" name="paymentMethodTypeId" value="EXT_OFFLINE" <#if paymentMethodTypeId?exists && paymentMethodTypeId == "EXT_OFFLINE">checked</#if>/>
           <label for="paymentMethodTypeId_EXT_OFFLINE">${uiLabelMap.OrderPaymentOfflineCheckMoney}</label>
         </div>
       </#if>
       <#if productStorePaymentMethodTypeIdMap.CREDIT_CARD?exists>
         <div>
           <input type="radio" id="paymentMethodTypeId_CREDIT_CARD" name="paymentMethodTypeId" value="CREDIT_CARD" <#if paymentMethodTypeId?exists && paymentMethodTypeId == "CREDIT_CARD">checked</#if>/>
           <label for="paymentMethodTypeId_CREDIT_CARD">${uiLabelMap.AccountingVisaMastercardAmexDiscover}</label>
         </div>
       </#if>
       <#if productStorePaymentMethodTypeIdMap.EFT_ACCOUNT?exists>
         <div>
           <input type="radio" id="paymentMethodTypeId_EFT_ACCOUNT" name="paymentMethodTypeId" value="EFT_ACCOUNT" <#if paymentMethodTypeId?exists && paymentMethodTypeId == "EFT_ACCOUNT">checked</#if>/>
           <label for="paymentMethodTypeId_EFT_ACCOUNT" class="tabletext">${uiLabelMap.AccountingAHCElectronicCheck}</label>
         </div>
       </#if>
       <#if productStorePaymentMethodTypeIdMap.EXT_PAYPAL?exists>
         <div>
           <input type="radio" id="paymentMethodTypeId_EXT_PAYPAL" name="paymentMethodTypeId" value="EXT_PAYPAL" <#if paymentMethodTypeId?exists && paymentMethodTypeId == "EXT_PAYPAL">checked</#if>/>
           <label for="paymentMethodTypeId_EXT_PAYPAL" class="tabletext">${uiLabelMap.AccountingPayWithPayPal}</label>
         </div>
       </#if>
       <div class="buttons">
         <input type="submit" value="${uiLabelMap.CommonContinue}"/>
       </div>
     </fieldset>
  </form>
