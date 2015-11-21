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
<#escape x as x?xml>
    <#if glAcctgTrialBalanceList?has_content>
        <fo:table table-layout="fixed" border="1pt solid" border-width=".1mm" width="19cm">
            <fo:table-column column-number="1" column-width="60%"/>
            <fo:table-column column-number="2" column-width="40%"/>
            <fo:table-header>
                <fo:table-cell border="1pt solid" border-width=".1mm">
                    <fo:block text-align="center" font-style="normal">${uiLabelMap.AccountingSubsidiaryLedger}</fo:block>
                </fo:table-cell>
            </fo:table-header>
            <fo:table-body>
                <fo:table-row>
                    <fo:table-cell width="19cm" border="1pt solid" border-width=".1mm">
                        <fo:block text-align="start" font-weight="normal">${uiLabelMap.FormFieldTitle_companyName} : ${(currentOrganization.groupName)!}</fo:block>
                    </fo:table-cell>
                </fo:table-row>
                <fo:table-row>
                    <fo:table-cell width="16cm" border="1pt solid" border-width=".1mm">
                        <fo:block text-align="start">${uiLabelMap.AccountingTimePeriod} : ${(currentTimePeriod.fromDate)!} ${uiLabelMap.CommonTo} ${(currentTimePeriod.thruDate)!}</fo:block>        
                    </fo:table-cell>
                </fo:table-row>
                <fo:table-row>
                    <fo:table-cell border="1pt solid" border-width=".1mm">
                        <fo:block text-align="start" space-after=".01in">${uiLabelMap.AccountingGlAccountNameAndGlAccountCode} : ${(glAccount.accountCode)!} - ${(glAccount.accountName)!}</fo:block><fo:block text-align="right"> ${uiLabelMap.CommonPage} - <fo:page-number-citation ref-id="theEnd"/></fo:block>        
                    </fo:table-cell>
                </fo:table-row>
            </fo:table-body>
        </fo:table>
        <fo:table table-layout="fixed" border="1pt solid" border-width=".1mm" width="19cm">
            <fo:table-column/>
            <fo:table-column/>
            <fo:table-column/>
            <fo:table-column/>
            <fo:table-column/>
            <fo:table-column/>
            <fo:table-column/>
            <fo:table-column/>
            <fo:table-column/>
            <fo:table-header>
                <fo:table-cell border="1pt solid" border-width=".1mm">
                    <fo:block text-align="center">${uiLabelMap.FormFieldTitle_transactionDate}</fo:block>
                </fo:table-cell>
                <fo:table-cell border="1pt solid" border-width=".1mm">
                    <fo:block text-align="center">${uiLabelMap.AccountingAccountTransactionId}</fo:block>
                </fo:table-cell>
                <fo:table-cell border="1pt solid" border-width=".1mm">
                    <fo:block text-align="center">${uiLabelMap.CommonDescription}</fo:block>
                </fo:table-cell>
                <fo:table-cell border="1pt solid" border-width=".1mm">
                    <fo:block text-align="center">${uiLabelMap.AccountingTypeOfTheCurrency}</fo:block>
                </fo:table-cell>
                <fo:table-cell border="1pt solid" border-width=".1mm">
                    <fo:block text-align="center">${uiLabelMap.AccountingOriginalCurrency}</fo:block>
                </fo:table-cell>
                <fo:table-cell border="1pt solid" border-width=".1mm">
                    <fo:block text-align="center">${uiLabelMap.AccountingDebitAmount}</fo:block>
                </fo:table-cell>
                <fo:table-cell border="1pt solid" border-width=".1mm">
                    <fo:block text-align="center">${uiLabelMap.AccountingCreditAmount}</fo:block>
                </fo:table-cell>
                <fo:table-cell border="1pt solid" border-width=".1mm">
                    <fo:block text-align="center">${uiLabelMap.AccountingDebitOrCreditOfBalance}</fo:block>
                </fo:table-cell>
                <fo:table-cell border="1pt solid" border-width=".1mm">
                    <fo:block text-align="center">${uiLabelMap.AccountingBalanceOfTheAccount}</fo:block>
                </fo:table-cell>
            </fo:table-header>
            <fo:table-body>
                <fo:table-row  border="1pt solid" border-width=".1mm"><fo:table-cell  border="1pt solid" border-width=".1mm" number-columns-spanned="1"></fo:table-cell><fo:table-cell  border="1pt solid" border-width=".1mm" number-columns-spanned="1"></fo:table-cell><fo:table-cell  border="1pt solid" border-width=".1mm" number-columns-spanned="3" text-align="center" ><fo:block text-align="center">${uiLabelMap.AccountingTheBalanceOfLastYear}</fo:block></fo:table-cell><fo:table-cell  border="1pt solid" border-width=".1mm" number-columns-spanned="1"></fo:table-cell><fo:table-cell border="1pt solid" border-width=".1mm" number-columns-spanned="1"></fo:table-cell><fo:table-cell  border="1pt solid" border-width=".1mm" number-columns-spanned="1"><fo:block text-align="center"><#if (isDebitAccount)>${uiLabelMap.AccountingDebitFlag}<#else>${uiLabelMap.AccountingCreditFlag}</#if></fo:block></fo:table-cell><fo:table-cell border="1pt solid" border-width=".1mm" number-columns-spanned="1"><fo:block text-align="center">${(openingBalance)!}</fo:block></fo:table-cell></fo:table-row>
                <#list glAcctgTrialBalanceList as glAcctgTrialBalance>
                    <#assign acctgTransAndEntries = glAcctgTrialBalance.acctgTransAndEntries/>
                    <#if (acctgTransAndEntries)?has_content>
                        <#list acctgTransAndEntries as acctgTransAndEntry>
                            <fo:table-row border="1pt solid" border-width=".1mm">
                                <fo:table-cell border="1pt solid" border-width=".1mm">
                                    <fo:block text-align="center">
                                        <#if acctgTransAndEntry.transactionDate?has_content>
                                            <#assign dateFormat = Static["java.text.DateFormat"].LONG/>
                                            <#assign transactionDate = Static["java.text.DateFormat"].getDateInstance(dateFormat, locale).format((acctgTransAndEntry.transactionDate)!)/>
                                            ${(transactionDate)!}
                                        </#if>
                                    </fo:block>
                                </fo:table-cell>
                                <fo:table-cell border="1pt solid" border-width=".1mm">
                                    <fo:block text-align="center">${(acctgTransAndEntry.acctgTransId)!}</fo:block>
                                </fo:table-cell>
                                <fo:table-cell  border="1pt solid" border-width=".1mm">
                                    <fo:block text-align="center">${(acctgTransAndEntry.transDescription)!}</fo:block>
                                </fo:table-cell>
                                <fo:table-cell  border="1pt solid" border-width=".1mm">
                                    <fo:block text-align="center">${(acctgTransAndEntry.currencyUomId)!}</fo:block>
                                </fo:table-cell>
                                <fo:table-cell  border="1pt solid" border-width=".1mm">
                                    <fo:block text-align="center">${(acctgTransAndEntry.origCurrencyUomId)!}</fo:block>
                                </fo:table-cell>
                                <fo:table-cell  border="1pt solid" border-width=".1mm">
                                    <fo:block text-align="center"><#if (acctgTransAndEntry.debitCreditFlag)! == "D">${(acctgTransAndEntry.amount)!}<#else>0</#if></fo:block>
                                </fo:table-cell>
                                <fo:table-cell  border="1pt solid" border-width=".1mm">
                                    <fo:block text-align="center"><#if (acctgTransAndEntry.debitCreditFlag)! == "C">${(acctgTransAndEntry.amount)!}<#else>0</#if></fo:block>
                                </fo:table-cell>
                                <fo:table-cell  border="1pt solid" border-width=".1mm">
                                    <fo:block text-align="center"></fo:block>
                                </fo:table-cell>
                                <fo:table-cell border="1pt solid" border-width=".1mm">
                                    <fo:block text-align="center"></fo:block>
                                </fo:table-cell>
                            </fo:table-row>
                        </#list>
                        <fo:table-row  border="1pt solid" border-width=".1mm"><fo:table-cell  border="1pt solid" border-width=".1mm" number-columns-spanned="1"></fo:table-cell><fo:table-cell  border="1pt solid" border-width=".1mm" number-columns-spanned="1"></fo:table-cell><fo:table-cell  border="1pt solid" border-width=".1mm" number-columns-spanned="3" text-align="center" ><fo:block text-align="center">${uiLabelMap.AccountingTotalOfTheCurrentMonth}</fo:block></fo:table-cell><fo:table-cell  border="1pt solid" border-width=".1mm" number-columns-spanned="1"><fo:block text-align="center">${(glAcctgTrialBalance.debitTotal)!}</fo:block></fo:table-cell><fo:table-cell border="1pt solid" border-width=".1mm" number-columns-spanned="1"><fo:block text-align="center">${(glAcctgTrialBalance.creditTotal)!}</fo:block></fo:table-cell><fo:table-cell  border="1pt solid" border-width=".1mm" number-columns-spanned="1"><fo:block text-align="center"><#if (isDebitAccount)>${uiLabelMap.AccountingDebitFlag}<#else>${uiLabelMap.AccountingCreditFlag}</#if></fo:block></fo:table-cell><fo:table-cell  border="1pt solid" border-width=".1mm" number-columns-spanned="1"><fo:block text-align="center">${(glAcctgTrialBalance.balance)!}</fo:block></fo:table-cell></fo:table-row>
                        <fo:table-row  border="1pt solid" border-width=".1mm"><fo:table-cell  border="1pt solid" border-width=".1mm" number-columns-spanned="1"></fo:table-cell><fo:table-cell  border="1pt solid" border-width=".1mm" number-columns-spanned="1"></fo:table-cell><fo:table-cell  border="1pt solid" border-width=".1mm" number-columns-spanned="3" text-align="center" ><fo:block text-align="center">${uiLabelMap.AccountingTotalOfYearToDate}</fo:block></fo:table-cell><fo:table-cell  border="1pt solid" border-width=".1mm" number-columns-spanned="1"><fo:block text-align="center">${glAcctgTrialBalance.totalOfYearToDateDebit}</fo:block></fo:table-cell><fo:table-cell border="1pt solid" border-width=".1mm" number-columns-spanned="1"><fo:block text-align="center">${glAcctgTrialBalance.totalOfYearToDateCredit}</fo:block></fo:table-cell><fo:table-cell  border="1pt solid" border-width=".1mm" number-columns-spanned="1"><fo:block text-align="center"><#if (isDebitAccount)>${uiLabelMap.AccountingDebitFlag}<#else>${uiLabelMap.AccountingCreditFlag}</#if></fo:block></fo:table-cell><fo:table-cell  border="1pt solid" border-width=".1mm" number-columns-spanned="1"><fo:block text-align="center">${(glAcctgTrialBalance.balanceOfTheAcctgForYear)!}</fo:block></fo:table-cell></fo:table-row>
                    </#if>
                </#list>
            </fo:table-body>
        </fo:table>
    <#else>
        ${uiLabelMap.CommonNoRecordFound}
    </#if>
</#escape>
