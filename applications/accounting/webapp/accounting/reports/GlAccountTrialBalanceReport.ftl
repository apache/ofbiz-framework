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
<div>
    <#if glAcctgTrialBalanceList?has_content>
        <div>
            <form name="glAccountTrialBalanceReport" id="glAccountTrialBalanceReport">
                <div>
                    <a href="<@ofbizUrl>GlAccountTrialBalanceReportPdf.pdf?organizationPartyId=${organizationPartyId}&timePeriod=${parameters.timePeriod}&isPosted=${parameters.isPosted}&glAccountId=${parameters.glAccountId}</@ofbizUrl>" class="buttontext">${uiLabelMap.AccountingInvoicePDF}</a>
                </div>
                <div style="width: 1210px;">
                    <table border=1>
                        <tr><th></th></tr>
                        <tr><th colspan=9 style="height: 30px;" ALIGN="center">${uiLabelMap.AccountingSubsidiaryLedger}</th></tr>
                        <tr><th colspan=9 style="height: 30px;" ALIGN="left">${uiLabelMap.FormFieldTitle_companyName} : ${(currentOrganization.groupName)!}</th></tr>
                        <tr><th colspan=9 style="height: 30px;" ALIGN="left">${uiLabelMap.AccountingTimePeriod} : <#if currentTimePeriod?has_content>${(currentTimePeriod.fromDate)!} ${uiLabelMap.CommonTo} ${(currentTimePeriod.thruDate)!}</#if></th></tr>
                        <tr><th colspan=9 style="height: 30px;" ALIGN="left">${uiLabelMap.AccountingGlAccountNameAndGlAccountCode} : ${(glAccount.accountCode)!} - ${(glAccount.accountName)!}</th></tr>
                        <tr>
                            <th>${uiLabelMap.Party}</th>
                            <th>${uiLabelMap.FormFieldTitle_transactionDate}</th>
                            <th>${uiLabelMap.AccountingAccountTransactionId}</th>
                            <th>${uiLabelMap.CommonDescription}</th>
                            <th>${uiLabelMap.AccountingTypeOfTheCurrency}</th>
                            <th>${uiLabelMap.AccountingOriginalCurrency}</th>
                            <th>${uiLabelMap.AccountingDebitAmount}</th>
                            <th>${uiLabelMap.AccountingCreditAmount}</th>
                            <th>${uiLabelMap.AccountingDebitOrCreditOfBalance}</th>
                            <th>${uiLabelMap.AccountingBalanceOfTheAccount}</th>
                        </tr>
                        <tr><th colspan=1><th colspan=1><th colspan=3>${uiLabelMap.AccountingTheBalanceOfLastYear}<th colspan=1></th></th></th></th><th></th><th ALIGN="center"><#if (isDebitAccount)??>${uiLabelMap.AccountingDebitFlag}<#else>${uiLabelMap.AccountingCreditFlag}</#if></th><th ALIGN="center">${(openingBalance)!}</th></tr>
                        <#list glAcctgTrialBalanceList as glAcctgTrialBalance>
                        
                            <#assign acctgTransAndEntries = glAcctgTrialBalance.acctgTransAndEntries/>
                            <#if acctgTransAndEntries?has_content>
                                <#list acctgTransAndEntries as acctgTransAndEntry>
                                <tr>
                                    <td>
                                        <#assign partyNameFrom = (delegator.findOne("PartyNameView", {"partyId" : acctgTransAndEntry.organizationPartyId}, true))!>
                                        <a href="/partymgr/control/viewprofile?partyId=${(partyNameFrom.partyId)!}&organizationPartyId=${(partyNameFrom.partyId)!}">${(partyNameFrom.firstName)!} ${(partyNameFrom.lastName)!} ${(partyNameFrom.groupName)!}</a>
                                    </td>
                                    <td ALIGN="center">${(acctgTransAndEntry.transactionDate)!}</td>
                                    <td ALIGN="center">${(acctgTransAndEntry.acctgTransId)!}</td>
                                    <td ALIGN="center">${(acctgTransAndEntry.transDescription)!}</td>
                                    <td ALIGN="center">${(acctgTransAndEntry.currencyUomId)!}</td>
                                    <td ALIGN="center">${(acctgTransAndEntry.origCurrencyUomId)!}</td>
                                    <td ALIGN="center"><#if (acctgTransAndEntry.debitCreditFlag)! == "D">${(acctgTransAndEntry.amount)!}<#else>0</#if></td>
                                    <td ALIGN="center"><#if (acctgTransAndEntry.debitCreditFlag)! == "C">${(acctgTransAndEntry.amount)!}<#else>0</#if></td>
                                    <td ALIGN="center"></td>
                                    <td ALIGN="center"></td>
                                </tr>
                                </#list>
                                <tr><td colspan = 1 ALIGN="center" ALIGN="center"><td colspan = 1 ALIGN="center"><td colspan=3 ALIGN="center"><b>${uiLabelMap.AccountingTotalOfTheCurrentMonth}</b></td></td></td><td ALIGN="center" colspan=1><b>${(glAcctgTrialBalance.debitTotal)!}</b></td><td ALIGN="center" colspan=1><b>${(glAcctgTrialBalance.creditTotal)!}</b></td><td ALIGN="center" colspan=1><b><#if (isDebitAccount)??>${uiLabelMap.AccountingDebitFlag}<#else>${uiLabelMap.AccountingCreditFlag}</#if></b></td><td ALIGN="center" colspan=1><b>${(glAcctgTrialBalance.debitCreditDifference)!}</b></td></tr>
                                <tr><td colspan = 1 ALIGN="center"><td ALIGN="center" colspan = 1><td ALIGN="center" colspan=3><b>${uiLabelMap.AccountingTotalOfYearToDate}</b></td></td></td></td><td ALIGN="center" colspan=1><b>${glAcctgTrialBalance.totalOfYearToDateDebit}</b></td><td ALIGN="center" colspan=1><b>${glAcctgTrialBalance.totalOfYearToDateCredit}</b></td><td ALIGN="center" colspan=1><b><#if (isDebitAccount)??>${uiLabelMap.AccountingDebitFlag}<#else>${uiLabelMap.AccountingCreditFlag}</#if></b></td><td ALIGN="center" colspan=1><b>${(glAcctgTrialBalance.balanceOfTheAcctgForYear)!}</b></td></tr>
                            </#if>
                        </#list>
                    </table>
                </div>
            </form>
        </div>
    <#else>
        ${uiLabelMap.AccountingNoRecordFound}
    </#if>
</div>
