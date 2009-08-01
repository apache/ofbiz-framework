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

<script language="JavaScript" type="text/javascript">
<!--
function togglefinAccountTransId(master) {
    var form = document.selectAllForm;
    var finAccountTransList = form.elements.length;
    for (var i = 0; i < finAccountTransList; i++) {
        var element = form.elements[i];
        if (element.type == "checkbox") {
            element.checked = master.checked;
        }
    }
    getFinAccountTransRunningTotal(master);
}

function getFinAccountTransRunningTotal(e) {
    var form = document.selectAllForm;
    var finAccountTransList = form.elements.length;
    var isSingle = true;
    var isAllSelected = true;
    for (var i = 0; i < finAccountTransList; i++) {
        var element = form.elements[i];
        if (element.name.startsWith("_rowSubmit_o_")) {
            if (element.checked) {
                isSingle = false;
            } else {
                isAllSelected = false;
            }
        }
    }
    if (!($(e).checked)) {
        $('checkAllTransactions').checked = false;
    } else if (isAllSelected) {
        $('checkAllTransactions').checked = true;
    }
    if (!isSingle) {
        new Ajax.Request('getFinAccountTransRunningTotal', {
            asynchronous: false,
            onSuccess: function(transport) {
                var data = transport.responseText.evalJSON(true);
                $('showFinAccountTransRunningTotal').update(data.finAccountTransRunningTotal);
            }, parameters: $('listFinAccTra').serialize(), requestHeaders: {Accept: 'application/json'}
        });
    } else {
        $('showFinAccountTransRunningTotal').update("");
    }
}
-->
</script>

<div class="screenlet screenlet-body">
  <#if finAccountTransList?has_content && parameters.noConditionFind?exists && parameters.noConditionFind == 'Y'>
    <#if !grandTotal?exists>
      <div>
        <span class="label">${uiLabelMap.AccountingRunningTotal} :</span>
        <span class="label" id="showFinAccountTransRunningTotal"></span>
      </div>
      <form id="listFinAccTra" name="selectAllForm" method="post" action="<@ofbizUrl>reconcileFinAccountTrans?clearAll=Y</@ofbizUrl>">
        <input name="_useRowSubmit" type="hidden" value="Y"/>
        <input name="finAccountId" type="hidden" value="${parameters.finAccountId}"/>
        <input name="statusId" type="hidden" value="${parameters.statusId?if_exists}">
    </#if>
        <table class="basic-table hover-bar" cellspacing="0">
          <#-- Header Begins -->
          <tr class="header-row-2">
            <th>${uiLabelMap.FormFieldTitle_finAccountTransId}</th>
            <th>${uiLabelMap.FormFieldTitle_finAccountTransTypeId}</th>
            <th>${uiLabelMap.PartyParty}</th>
            <th>${uiLabelMap.FormFieldTitle_transactionDate}</th>
            <th>${uiLabelMap.FormFieldTitle_entryDate}</th>
            <th>${uiLabelMap.CommonAmount}</th>
            <th>${uiLabelMap.FormFieldTitle_paymentId}</th>
            <th>${uiLabelMap.OrderPaymentType}</th>
            <th>${uiLabelMap.FormFieldTitle_paymentMethodTypeId}</th>
            <th>${uiLabelMap.CommonStatus}</th>
            <th>${uiLabelMap.CommonComments}</th>
            <#if grandTotal?exists>
              <th>${uiLabelMap.AccountingCancelTransactionStatus}</th>
            <#else>
              <th>
                ${uiLabelMap.CommonSelectAll} <input name="selectAll" type="checkbox" value="N" id="checkAllTransactions" onclick="javascript:togglefinAccountTransId(this);"/>
              </th>
            </#if>
          </tr>
          <#-- Header Ends-->
          <#assign alt_row = false>
          <#list finAccountTransList as finAccountTrans>
            <#assign payment = "">
            <#assign payments = "">
            <#if finAccountTrans.paymentId?has_content>
              <#assign payment = delegator.findOne("Payment", {"paymentId" : finAccountTrans.paymentId}, true)>
            <#else>
              <#assign payments = delegator.findByAnd("Payment", {"finAccountTransId" : finAccountTrans.finAccountTransId})>
            </#if>
            <#assign finAccountTransType = delegator.findOne("FinAccountTransType", {"finAccountTransTypeId" : finAccountTrans.finAccountTransTypeId}, true)>
            <#if finAccountTrans.statusId?has_content>
              <#assign status = delegator.findOne("StatusItem", {"statusId" : finAccountTrans.statusId}, true)>
            </#if>
            <#if payment?has_content && payment.paymentTypeId?has_content>
              <#assign paymentType = delegator.findOne("PaymentType", {"paymentTypeId" : payment.paymentTypeId}, true)>
            </#if>
            <#if payment?has_content && payment.paymentMethodTypeId?has_content>
              <#assign paymentMethodType = delegator.findOne("PaymentMethodType", {"paymentMethodTypeId" : payment.paymentMethodTypeId}, true)>
            </#if>
            <#if finAccountTrans.partyId?has_content>
              <#assign partyName = (delegator.findOne("PartyNameView", {"partyId" : finAccountTrans.partyId}, true))!>
            </#if>
            <tr valign="middle"<#if alt_row> class="alternate-row"</#if>>
              <td>
                <#if payments?has_content>
                  <a id="togglePayment_${finAccountTrans.finAccountTransId}" href="javascript:void(0)"><img src="<@ofbizContentUrl>/images/expand.gif</@ofbizContentUrl>"/></a> ${finAccountTrans.finAccountTransId}
                  <div id="displayPayments_${finAccountTrans.finAccountTransId}" class="popup" style="display: none;width: 500px;">
                    <div align="right">
                      <input class="popup_closebox buttontext" type="button" value="X"/>
                    </div>
                    <table class="basic-table hover-bar" cellspacing="0" style"width :">
                      <tr class="header-row-2">
                        <th>${uiLabelMap.FormFieldTitle_paymentId}</th>
                        <th>${uiLabelMap.OrderPaymentType}</th>
                        <th>${uiLabelMap.FormFieldTitle_paymentMethodTypeId}</th>
                        <th>${uiLabelMap.CommonAmount}</th>
                        <th>${uiLabelMap.PartyPartyFrom}</th>
                        <th>${uiLabelMap.PartyPartyTo}</th>
                      </tr>
                      <#list payments as payment>
                        <#if payment?exists && payment.paymentTypeId?has_content>
                          <#assign paymentType = delegator.findOne("PaymentType", {"paymentTypeId" : payment.paymentTypeId}, true)>
                        </#if>
                        <#if payment?has_content && payment.paymentMethodTypeId?has_content>
                          <#assign paymentMethodType = delegator.findOne("PaymentMethodType", {"paymentMethodTypeId" : payment.paymentMethodTypeId}, true)>
                        </#if>
                        <tr valign="middle"<#if alt_row> class="alternate-row"</#if>>
                          <td>${payment.paymentId?if_exists}</td>
                          <td><#if paymentType?has_content>${paymentType.description?if_exists}</#if></td>
                          <td><#if paymentMethodType?has_content>${paymentMethodType.description?if_exists}</#if></td>
                          <td>${payment.amount?if_exists}</td>
                          <td>${payment.partyIdFrom?if_exists}</td>
                          <td>${payment.partyIdTo?if_exists}</td>
                        </tr>
                      </#list>
                    </table>
                  </div>
                  <script type="text/javascript">
                    new Popup('displayPayments_${finAccountTrans.finAccountTransId}','togglePayment_${finAccountTrans.finAccountTransId}', {modal: true, position: 'center', trigger: 'click'})
                  </script>
                  <a href="<@ofbizUrl>DepositSlip.pdf?finAccountTransId=${finAccountTrans.finAccountTransId}</@ofbizUrl>" class="buttontext">${uiLabelMap.AccountingDepositSlip}</a>
                <#else>
                  ${finAccountTrans.finAccountTransId}
                </#if>
              </td>
              <td>${finAccountTransType.description?if_exists}</td>
              <td>${(partyName.firstName)!} ${(partyName.lastName)!} ${(partyName.groupName)!}</td>
              <td>${finAccountTrans.transactionDate?if_exists}</td>
              <td>${finAccountTrans.entryDate?if_exists}</td>
              <td>${finAccountTrans.amount?if_exists}</td>
              <td>
                <#if finAccountTrans.paymentId?has_content>
                  <a href="<@ofbizUrl>paymentOverview?paymentId=${finAccountTrans.paymentId}</@ofbizUrl>">${finAccountTrans.paymentId}</a>
                </#if>
              </td>
              <td><#if paymentType?has_content>${paymentType.description?if_exists}</#if></td>
              <td><#if paymentMethodType?has_content>${paymentMethodType.description?if_exists}</#if></td>
              <td><#if status?has_content>${status.description?if_exists}</#if></td>
              <td>${finAccountTrans.comments?if_exists}</td>
              <#if grandTotal?exists>
                <td>
                  <#if finAccountTrans.statusId?has_content && finAccountTrans.statusId == 'FINACT_TRNS_CREATED'>
                    <form name="cancelFinAccountTrans_${finAccountTrans.finAccountTransId}" method="post" action="<@ofbizUrl>setFinAccountTransStatus</@ofbizUrl>">
                      <input name="noConditionFind" type="hidden" value="Y"/>
                      <input name="finAccountTransId" type="hidden" value="${finAccountTrans.finAccountTransId}"/>
                      <input name="finAccountId" type="hidden" value="${finAccountTrans.finAccountId}"/>
                      <input name="statusId" type="hidden" value="FINACT_TRNS_CANCELED"/>
                      <input class="buttontext" type="submit" value="${uiLabelMap.CommonCancel}"/> 
                    </form>
                  </#if>
                </td>
              <#else>
                <input name="finAccountTransId_o_${finAccountTrans_index}" type="hidden" value="${finAccountTrans.finAccountTransId}"/>
                <input name="organizationPartyId_o_${finAccountTrans_index}" type="hidden" value="${defaultOrganizationPartyId}"/>
                <td>
                  <input id="finAccountTransId_${finAccountTrans_index}" name="_rowSubmit_o_${finAccountTrans_index}" type="checkbox" value="Y" onclick="javascript:getFinAccountTransRunningTotal('finAccountTransId_${finAccountTrans_index}');"/>
                </td>
                <#if finAccountTrans.finAccountTransTypeId="ADJUSTMENT">
            </tr>
            <tr>  
                  <td>
                    <select name="debitCreditFlag_o_${finAccountTrans_index}">
                      <option value="D">${uiLabelMap.FormFieldTitle_debit}</option>
                      <option value="C">${uiLabelMap.FormFieldTitle_credit}</option>
                    </select>
                  </td>
                  <td>
                    <select name="glAccountId_o_${finAccountTrans_index}" style="width: 50%">
                      <#list glAccountOrgAndClassList as glAccountOrgAndClass>
                        <option value="${glAccountOrgAndClass.glAccountId}">${glAccountOrgAndClass.accountCode} - ${glAccountOrgAndClass.accountName} [${glAccountOrgAndClass.glAccountId}]</option>
                      </#list>
                    </select>
                  </td>
                </#if>
              </#if>
            </tr>
            
            <#-- toggle the row color -->
            <#assign alt_row = !alt_row>
          </#list>
    <#if !grandTotal?exists>
          <div align="right">
             <input id="submitButton" type="submit" onclick="javascript:document.selectAllForm.submit();" value="${uiLabelMap.AccountingReconcile}"/>
          <div>      
        </table>
      </form>
    <#else>
        </table>
    </#if>
    <#if grandTotal?exists>
      <table border="1" class="basic-table">
        <tr>
          <th>${uiLabelMap.FormFieldTitle_grandTotal} / ${uiLabelMap.AccountingNumberOfTransaction}</th>
          <th>${uiLabelMap.AccountingCreatedGrandTotal} / ${uiLabelMap.AccountingNumberOfTransaction}</th>
          <th>${uiLabelMap.AccountingApprovedGrandTotal} / ${uiLabelMap.AccountingNumberOfTransaction}</th>
          <th>${uiLabelMap.AccountingCreatedApprovedGrandTotal} / ${uiLabelMap.AccountingNumberOfTransaction}</th>
        </tr>
        <tr>
          <td>${grandTotal} / ${searchedNumberOfRecords}</td>
          <td>${createdGrandTotal} / ${totalCreatedTransactions}</td>
          <td>${approvedGrandTotal} / ${totalApprovedTransactions}</td>
          <td>${createdApprovedGrandTotal} / ${totalCreatedApprovedTransactions}</td>
        </tr>
      </table>
    </#if>
  <#else>
    <h2>${uiLabelMap.AccountingNoRecordFound}</h2>  
  </#if>
</div>
