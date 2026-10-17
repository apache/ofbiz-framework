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
                    <a href="<@ofbizUrl>GlAccountTrialBalanceReportPdf.pdf?timePeriod=${parameters.timePeriod}&amp;isPosted=${parameters.isPosted}&amp;glAccountId=${parameters.glAccountId}</@ofbizUrl>" target="_BLANK" class="buttontext">${uiLabelMap.AccountingInvoicePDF}</a>
                </div>
                <h3>${uiLabelMap.AccountingSubsidiaryLedger}</h3>
                <h3>${uiLabelMap.FormFieldTitle_companyName} : ${(currentOrganization.groupName)!}</h3>
                <h3>${uiLabelMap.AccountingTimePeriod} : <#if currentTimePeriod?has_content>${(currentTimePeriod.fromDate)!} ${uiLabelMap.CommonTo} ${(currentTimePeriod.thruDate)!}</#if></h3>
                <h3>${uiLabelMap.AccountingGlAccountNameAndGlAccountCode} : ${(glAccount.accountCode)!} - ${(glAccount.accountName)!}</h3>
                <div style="width: 1210px;">
                    <table border=2>
                        <tr>
                            <td align="left"><b>${uiLabelMap.FormFieldTitle_transactionDate}</b></td>
                            <td align="left"><b>${uiLabelMap.AccountingAccountTransactionId}</b></td>
                            <td align="left"><b>${uiLabelMap.CommonDescription}</b></td>
                            <td align="left"><b>${uiLabelMap.AccountingTypeOfTheCurrency}</b></td>
                            <td align="left"><b>${uiLabelMap.AccountingOriginalCurrency}</b></td>
                            <td align="right"><b>${uiLabelMap.AccountingDebitAmount}</b></td>
                            <td align="right"><b>${uiLabelMap.AccountingCreditAmount}</b></td>
                            <td align="right"><b>${uiLabelMap.AccountingDebitOrCreditOfBalance}</b></td>
                            <td align="right"><b>${uiLabelMap.AccountingBalanceOfTheAccount}</b></td>
                        </tr>
                        <tr class="header-row">
                            <td colspan=2></td>
                            <td colspan=3 align="center"><b>${uiLabelMap.AccountingTheBalanceOfLastYear}</b></td>
                            <td colspan=2></td>
                            <td ALIGN="right"><b><#if (isDebitAccount)>${uiLabelMap.AccountingDebitFlag}<#else>${uiLabelMap.AccountingCreditFlag}</#if></b></td>
                            <td ALIGN="right">${(openingBalance)!}</td>
                        </tr>
                        <#list glAcctgTrialBalanceList as glAcctgTrialBalance>
                        
                            <#assign acctgTransAndEntries = glAcctgTrialBalance.acctgTransAndEntries/>
                            <#if acctgTransAndEntries?has_content>
                                <#list acctgTransAndEntries as acctgTransAndEntry>
                                <tr>
                                    <td ALIGN="left">${(acctgTransAndEntry.transactionDate)!}</td>
                                    <td ALIGN="left">${(acctgTransAndEntry.acctgTransId)!}</td>
                                    <td ALIGN="left">${(acctgTransAndEntry.transDescription)!}</td>
                                    <td ALIGN="left">${(acctgTransAndEntry.currencyUomId)!}</td>
                                    <td ALIGN="left">${(acctgTransAndEntry.origCurrencyUomId)!}</td>
                                    <td ALIGN="right"><#if "D" == (acctgTransAndEntry.debitCreditFlag)!>${(acctgTransAndEntry.amount)!}<#else>0</#if></td>
                                    <td ALIGN="right"><#if "C" == (acctgTransAndEntry.debitCreditFlag)!>${(acctgTransAndEntry.amount)!}<#else>0</#if></td>
                                    <td ALIGN="right"></td>
                                    <td ALIGN="right"></td>
                                </tr>
                                </#list>
                                <tr class="header-row">
                                    <td colspan=2></td>
                                    <td colspan=3 ALIGN="center"><b>${uiLabelMap.AccountingTotalOfTheCurrentMonth}</b></td>
                                    <td ALIGN="right" colspan=1><b>${(glAcctgTrialBalance.debitTotal)!}</b></td>
                                    <td ALIGN="right" colspan=1><b>${(glAcctgTrialBalance.creditTotal)!}</b></td>
                                    <td ALIGN="right" colspan=1><b><#if (isDebitAccount)>${uiLabelMap.AccountingDebitFlag}<#else>${uiLabelMap.AccountingCreditFlag}</#if></b></td>
                                    <td ALIGN="right" colspan=1><b>${(glAcctgTrialBalance.balance)!}</b></td>
                                </tr>
                                <tr class="header-row">
                                    <td colspan=2></td>
                                    <td ALIGN="center" colspan=3><b>${uiLabelMap.AccountingTotalOfYearToDate}</b></td>
                                    <td ALIGN="right"><b>${glAcctgTrialBalance.totalOfYearToDateDebit}</b></td>
                                    <td ALIGN="right"><b>${glAcctgTrialBalance.totalOfYearToDateCredit}</b></td>
                                    <td ALIGN="right"><b><#if (isDebitAccount)>${uiLabelMap.AccountingDebitFlag}<#else>${uiLabelMap.AccountingCreditFlag}</#if></b></td>
                                    <td ALIGN="right"><b>${(glAcctgTrialBalance.balanceOfTheAcctgForYear)!}</b></td>
                                </tr>
                            </#if>
                        </#list>
                    </table>
                </div>
            </form>
        </div>
    <#else>
        ${uiLabelMap.CommonNoRecordFound}
    </#if>
</div>
