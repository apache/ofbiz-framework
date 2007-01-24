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
<#if requestParameters.paymentMethodTypeId?has_content>
   <#assign paymentMethodTypeId = "${requestParameters.paymentMethodTypeId?if_exists}">
</#if>
<script language="JavaScript" type="text/javascript">

dojo.require("dojo.event.*");
dojo.require("dojo.io.*");

dojo.addOnLoad(init);

function init() {
    getPaymentInformation();
    dojo.event.connect("around", "processOrder", "aroundSubmitOrder");
    var paymentForm = document.setPaymentInformation;
}

function aroundSubmitOrder(invocation) {
    var formToSubmit = document.setPaymentInformation;
    var paymentMethodTypeOption = document.setPaymentInformation.paymentMethodTypeOptionList.options[document.setPaymentInformation.paymentMethodTypeOptionList.selectedIndex].value;
    if(paymentMethodTypeOption == "none"){
        document.setPaymentInformation.action = "<@ofbizUrl>quickAnonAddGiftCardToCart</@ofbizUrl>";
    }
    
    dojo.io.bind({ url: formToSubmit.action, load: function(type, evaldObj){
       if(type == "load"){
           if(paymentMethodTypeOption == "EXT_OFFLINE"){
               var result = invocation.proceed();
               return result;
           }else {
               if(paymentMethodTypeOption == "none"){
                   document.getElementById("noPaymentMethodSelectedError").innerHTML = "${uiLabelMap.EcommerceMessagePleaseSelectPaymentMethod}";
                   return result;
               } else {
                   document.getElementById("paymentInfoSection").innerHTML = evaldObj;
               }
               if(formToSubmit.paymentMethodId.value != "") {
                   var result = invocation.proceed();
                   return result;
               }
           }
       }        
    },formNode: document.setPaymentInformation});
}

function getGCInfo() {
    if (document.setPaymentInformation.addGiftCard.checked) {
      dojo.io.bind({url: "<@ofbizUrl>quickAnonGcInfo</@ofbizUrl>",
        load: function(type, data, evt){
          if(type == "load"){
            document.getElementById("giftCardSection").innerHTML = data;
          }        
        },mimetype: "text/html"});    
    } else {
        document.getElementById("giftCardSection").innerHTML = "";
    }
}

function getPaymentInformation() {
  document.getElementById("noPaymentMethodSelectedError").innerHTML = "";
  var paymentMethodTypeOption = document.setPaymentInformation.paymentMethodTypeOptionList.options[document.setPaymentInformation.paymentMethodTypeOptionList.selectedIndex].value;
  var connectionObject;
   if(paymentMethodTypeOption.length > 0){
      if(paymentMethodTypeOption == "CREDIT_CARD"){
        dojo.io.bind({url: "<@ofbizUrl>quickAnonCcInfo</@ofbizUrl>",
          load: function(type, data, evt){
            if(type == "load"){document.getElementById("paymentInfoSection").innerHTML = data;}        
          },mimetype: "text/html"});
        document.setPaymentInformation.paymentMethodTypeId.value = "CREDIT_CARD";
        document.setPaymentInformation.action = "<@ofbizUrl>quickAnonEnterCreditCard</@ofbizUrl>";
      } else if(paymentMethodTypeOption == "EFT_ACCOUNT"){
        dojo.io.bind({url: "<@ofbizUrl>quickAnonEftInfo</@ofbizUrl>",
          load: function(type, data, evt){
            if(type == "load"){document.getElementById("paymentInfoSection").innerHTML = data;}        
          },mimetype: "text/html"});
         document.setPaymentInformation.paymentMethodTypeId.value = "EFT_ACCOUNT";
        document.setPaymentInformation.action = "<@ofbizUrl>quickAnonEnterEftAccount</@ofbizUrl>";
      } else if(paymentMethodTypeOption == "EXT_OFFLINE"){
        document.setPaymentInformation.paymentMethodTypeId.value = "EXT_OFFLINE";
        document.getElementById("paymentInfoSection").innerHTML = "";
        document.setPaymentInformation.action = "<@ofbizUrl>quickAnonEnterExtOffline</@ofbizUrl>";
      } else {
        document.setPaymentInformation.paymentMethodTypeId.value = "none";
        document.getElementById("paymentInfoSection").innerHTML = "";
      }
   }    
}
</script>
<form id="setPaymentInformation" type="POST" action="<@ofbizUrl>quickAnonAddGiftCardToCart</@ofbizUrl>" name="setPaymentInformation">
<div class="screenlet">
    <div class="screenlet-header">
        <div class='boxhead'>&nbsp;${uiLabelMap.AccountingPaymentInformation}</div>
    </div>
    <div class="screenlet-body">
          <#if requestParameters.singleUsePayment?default("N") == "Y">
            <input type="hidden" name="singleUsePayment" value="Y"/>
            <input type="hidden" name="appendPayment" value="Y"/>
          </#if>
          <input type="hidden" name="contactMechTypeId" value="POSTAL_ADDRESS"/>
          <input type="hidden" name="partyId" value="${partyId?if_exists}"/>
          <input type="hidden" name="paymentMethodTypeId" value="${paymentMethodTypeId?if_exists}"/>
          <input type="hidden" name="createNew" value="Y"/>
          <#if session.getAttribute("billingContactMechId")?exists>
            <input type="hidden" name="contactMechId" value="${session.getAttribute("billingContactMechId")?if_exists}"/>
          </#if>

          <table width="100%" border="0" cellpadding="1" cellspacing="0">
              <tr><td colspan="3"><div class="errorMessage" id="noPaymentMethodSelectedError"></div></td></tr>
              <tr>
                 <td width="26%" align="right" valign="top"><div class="tableheadtext">${uiLabelMap.OrderSelectPaymentMethod}</div></td>
                 <td colspan="2">
                   <select name="paymentMethodTypeOptionList" class="selectBox"  onChange="javascript:getPaymentInformation();">
                       <option value="none">Select One</option>
                     <#if productStorePaymentMethodTypeIdMap.CREDIT_CARD?exists>
                       <option value="CREDIT_CARD" <#if (parameters.paymentMethodTypeId?default("") == "CREDIT_CARD")> selected</#if>>${uiLabelMap.AccountingVisaMastercardAmexDiscover}</option>
                     </#if> 
                     <#if productStorePaymentMethodTypeIdMap.EFT_ACCOUNT?exists>
                       <option value="EFT_ACCOUNT" <#if (parameters.paymentMethodTypeId?default("") == "EFT_ACCOUNT")> selected</#if>>${uiLabelMap.AccountingAHCElectronicCheck}</option>
                     </#if>
                     <#if productStorePaymentMethodTypeIdMap.EXT_OFFLINE?exists>
                       <option value="EXT_OFFLINE" <#if (parameters.paymentMethodTypeId?default("") == "EXT_OFFLINE")> selected</#if>>${uiLabelMap.OrderPaymentOfflineCheckMoney}</option>
                     </#if>
                   </select>                 
                 </td>
              </tr>
              <tr><td nowrap colspan="3"><div id="paymentInfoSection"></div></td></tr>
              <tr><td colspan="3"><hr class="sepbar"/></td></tr>
              <#-- gift card fields -->
              <#if productStorePaymentMethodTypeIdMap.GIFT_CARD?exists>
              <tr>
                <td width='26%' nowrap align="right">
                  <input type="checkbox" id="addGiftCard" name="addGiftCard" value="Y" onClick="javascript:getGCInfo();"/>
                </td>
                <td colspan="2" nowrap><div class="tabletext">${uiLabelMap.AccountingCheckGiftCard}</div></td>
              </tr>
              <tr><td colspan="3"><div id="giftCardSection"></div></td></tr>
              </#if>
          </table>
    </div>
</div>
</form>
