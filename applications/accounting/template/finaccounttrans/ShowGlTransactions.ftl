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

<div id="displayGlTransactions_${finAccountTrans.finAccountTransId}" style="display: none;width: 1150px;">
  <#assign acctgTransAndEntries = dispatcher.runSync("getAssociatedAcctgTransEntriesWithFinAccountTrans", Static["org.ofbiz.base.util.UtilMisc"].toMap("finAccountTransId", finAccountTrans.finAccountTransId, "userLogin", userLogin))/>
  <#assign acctgTransAndEntries = acctgTransAndEntries.acctgTransAndEntries>
   <table class="basic-table hover-bar" cellspacing="0" style"width :">
     <tr class="header-row-2">
       <th>${uiLabelMap.FormFieldTitle_acctgTransId}</th>
       <th>${uiLabelMap.FormFieldTitle_acctgTransEntrySeqId}</th>
       <th>${uiLabelMap.FormFieldTitle_isPosted}</th>
       <th>${uiLabelMap.FormFieldTitle_glFiscalTypeId}</th>
       <th>${uiLabelMap.FormFieldTitle_acctgTransType}</th>
       <th>${uiLabelMap.FormFieldTitle_transactionDate}</th>
       <th>${uiLabelMap.FormFieldTitle_postedDate}</th>
       <th>${uiLabelMap.FormFieldTitle_glJournal}</th>
       <th>${uiLabelMap.FormFieldTitle_transTypeDescription}</th>
       <th>${uiLabelMap.FormFieldTitle_invoiceId}</th>
       <th>${uiLabelMap.FormFieldTitle_glAccountId}</th>
       <th>${uiLabelMap.FormFieldTitle_productId}</th>
       <th>${uiLabelMap.FormFieldTitle_debitCreditFlag}</th>
       <th>${uiLabelMap.AccountingAmount}</th>
       <th>${uiLabelMap.FormFieldTitle_origAmount}</th>
       <th>${uiLabelMap.FormFieldTitle_organizationPartyId}</th>
       <th>${uiLabelMap.FormFieldTitle_glAccountType}</th>
       <th>${uiLabelMap.FormFieldTitle_accountCode}</th>
       <th>${uiLabelMap.FormFieldTitle_accountName}</th>
       <th>${uiLabelMap.AccountingGlAccountClass}</th>
       <th>${uiLabelMap.PartyParty}</th>
       <th>${uiLabelMap.FormFieldTitle_reconcileStatusId}</th>
       <th>${uiLabelMap.FormFieldTitle_acctgTransEntryTypeId}</th>
     </tr>
     <#list acctgTransAndEntries as acctgTransEntry>
     <tr>
       <td>${acctgTransEntry.acctgTransId!}</td>
       <td>${acctgTransEntry.acctgTransEntrySeqId!}</td>
       <td>${acctgTransEntry.isPosted!}</td>
       <td>
         <#if acctgTransEntry.glFiscalTypeId?has_content>
           <#assign glFiscalType = delegator.findOne("GlFiscalType", {"glFiscalTypeId" : acctgTransEntry.glFiscalTypeId}, false)/>
             ${glFiscalType.description!}
         </#if>
       </td>
       <td>
         <#if acctgTransEntry.acctgTransTypeId?has_content>
           <#assign acctgTransType = delegator.findOne("AcctgTransType", {"acctgTransTypeId" : acctgTransEntry.acctgTransTypeId}, false)/>
             ${acctgTransType.description!}
         </#if>
       </td>
       <td>${acctgTransEntry.transactionDate!}</td>
       <td>${acctgTransEntry.postedDate!}</td>
       <td>${acctgTransEntry.glJournal!}</td>
       <td>${acctgTransEntry.transTypeDescription!}</td>
       <td>${acctgTransEntry.invoiceId!}</td>
       <td>${acctgTransEntry.glAccountId!}</td>
       <td>${acctgTransEntry.productId!}</td>
       <td>${acctgTransEntry.debitCreditFlag!}</td>
       <td>${acctgTransEntry.amount!}</td>
       <td>${acctgTransEntry.origAmount!}</td>
       <td>
         <#if acctgTransEntry.organizationPartyId?has_content>
           <#assign partyName = delegator.findOne("PartyNameView", {"partyId" : acctgTransEntry.organizationPartyId}, false)>
           <#if partyName.partyTypeId == "PERSON">
             ${partyName.firstName!} ${partyName.lastName!}
           <#elseif (partyName.partyTypeId) != "PARTY_GROUP">
             ${partyName.groupName!}
           </#if>
         </#if>
       </td>
       <td>
         <#if (acctgTransEntry.glAccountTypeId)?has_content>
           <#assign glAccountType = delegator.findOne("GlAccountType", {"glAccountTypeId" : acctgTransEntry.glAccountTypeId}, false)>
             ${glAccountType.description!}
         </#if>
       </td>
       <td>${acctgTransEntry.accountCode!}</td>
       <td>${acctgTransEntry.accountName!}</td>
       <td>
         <#if acctgTransEntry.glAccountClassId?has_content>
           <#assign glAccountClass = delegator.findOne("GlAccountClass", {"glAccountClassId" : acctgTransEntry.glAccountClassId}, false)/>
             ${glAccountClass.description!}
         </#if>
       </td>
       <td>${acctgTransEntry.partyId!}</td>
       <td>
         <#if acctgTransEntry.reconcileStatusId?has_content>
           <#assign status = delegator.findOne("StatusItem", {"statusId" : finAccountTrans.statusId}, true)>
             ${status.description!}
         </#if>
       </td>
       <td>
         <#if acctgTransEntry.acctgTransEntryTypeId?has_content>
           <#assign acctgTransEntryType = delegator.findOne("AcctgTransEntryType", {"acctgTransEntryTypeId" : acctgTransEntry.acctgTransEntryTypeId}, true)>
             ${acctgTransEntryType.description!}
         </#if>
       </td>
     </tr>
   </#list>
  </table>
</div>       
