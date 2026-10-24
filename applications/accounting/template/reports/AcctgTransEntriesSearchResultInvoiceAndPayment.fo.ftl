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
    <fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">
        <fo:layout-master-set>
            <fo:simple-page-master master-name="11x17-landscape" page-width="17in" page-height="11in"
                margin-top="0.1in" margin-bottom="0.5in" margin-left="0.5in" margin-right="0.5in">
                <fo:region-body margin-top="1in" margin-bottom="0.5in"/>
            </fo:simple-page-master>
        </fo:layout-master-set>
        <fo:page-sequence master-reference="11x17-landscape">
            <fo:flow flow-name="xsl-region-body" font-family="Helvetica">
                <fo:block text-align="center">${screens.render("component://order/widget/ordermgr/OrderPrintScreens.xml#CompanyLogo")}</fo:block>
                <#if acctgTransEntryList?has_content>
                    <fo:block>${uiLabelMap.AccountingAcctgTransEntriesFor}
                        <#assign partyName = (delegator.findOne("PartyNameView", {"partyId" : parameters.get('ApplicationDecorator|organizationPartyId')}, false))!>
                        <#if "PERSON" == (partyName.partyTypeId)!>
                            ${(partyName.firstName)!} ${(partyName.lastName)!}
                        <#elseif "PARTY_GROUP" == (partyName.partyTypeId)!>
                            ${(partyName.groupName)!}
                        </#if>
                    </fo:block>
                    <fo:block space-before="4mm">
                        <fo:table table-layout="fixed" width="100%">
                            <fo:table-column column-width="22mm"/>
                            <fo:table-column column-width="22mm"/>
                            <fo:table-column column-width="50mm"/>
                            <fo:table-column column-width="25mm"/>
                            <fo:table-column column-width="25mm"/>
                            <fo:table-column column-width="45mm"/>
                            <fo:table-column column-width="22mm"/>
                            <fo:table-column column-width="45mm"/>
                            <fo:table-column column-width="30mm"/>
                            <fo:table-column column-width="30mm"/>
                            <fo:table-header>
                                <fo:table-row background-color="#BFBFBF" font-weight="bold">
                                    <fo:table-cell border=".1mm solid"><fo:block text-align="center" font-size="6pt">${uiLabelMap.FormFieldTitle_transactionDate}</fo:block></fo:table-cell>
                                    <fo:table-cell border=".1mm solid"><fo:block text-align="center" font-size="6pt">${uiLabelMap.FormFieldTitle_accountCode}</fo:block></fo:table-cell>
                                    <fo:table-cell border=".1mm solid"><fo:block text-align="center" font-size="6pt">${uiLabelMap.FormFieldTitle_accountName}</fo:block></fo:table-cell>
                                    <fo:table-cell border=".1mm solid"><fo:block text-align="center" font-size="6pt">${uiLabelMap.FormFieldTitle_invoiceId}</fo:block></fo:table-cell>
                                    <fo:table-cell border=".1mm solid"><fo:block text-align="center" font-size="6pt">${uiLabelMap.FormFieldTitle_paymentId}</fo:block></fo:table-cell>
                                    <fo:table-cell border=".1mm solid"><fo:block text-align="center" font-size="6pt">${uiLabelMap.CommonPartyId}</fo:block></fo:table-cell>
                                    <fo:table-cell border=".1mm solid"><fo:block text-align="center" font-size="6pt">${uiLabelMap.AccountingOriginalCurrency}</fo:block></fo:table-cell>
                                    <fo:table-cell border=".1mm solid"><fo:block text-align="center" font-size="6pt">${uiLabelMap.FormFieldTitle_exchangeRate}</fo:block></fo:table-cell>
                                    <fo:table-cell border=".1mm solid"><fo:block text-align="center" font-size="6pt">${uiLabelMap.FormFieldTitle_debit}</fo:block></fo:table-cell>
                                    <fo:table-cell border=".1mm solid"><fo:block text-align="center" font-size="6pt">${uiLabelMap.FormFieldTitle_credit}</fo:block></fo:table-cell>
                                </fo:table-row>
                            </fo:table-header>
                            <fo:table-body>
                                <#list acctgTransEntryList as acctgTransEntry>
                                    <fo:table-row>
                                        <fo:table-cell border=".1mm solid"><fo:block text-align="center" font-size="6pt"><#if acctgTransEntry.transactionDate?has_content>${acctgTransEntry.transactionDate?date}</#if></fo:block></fo:table-cell>
                                        <fo:table-cell border=".1mm solid"><fo:block font-size="6pt">${(acctgTransEntry.accountCode)!}</fo:block></fo:table-cell>
                                        <fo:table-cell border=".1mm solid"><fo:block font-size="6pt">${(acctgTransEntry.accountName)!}</fo:block></fo:table-cell>
                                        <fo:table-cell border=".1mm solid"><fo:block text-align="center" font-size="6pt">${(acctgTransEntry.invoiceId)!}</fo:block></fo:table-cell>
                                        <fo:table-cell border=".1mm solid"><fo:block text-align="center" font-size="6pt">${(acctgTransEntry.paymentId)!}</fo:block></fo:table-cell>
                                        <fo:table-cell border=".1mm solid"><fo:block text-align="center" font-size="6pt">${(acctgTransEntry.partyId)!}</fo:block></fo:table-cell>
                                        <fo:table-cell border=".1mm solid"><fo:block text-align="center" font-size="6pt">${(acctgTransEntry.origCurrencyUomId)!}</fo:block></fo:table-cell>
                                        <fo:table-cell border=".1mm solid"><fo:block text-align="center" font-size="6pt"><#if acctgTransEntry.origAmount?has_content>${acctgTransEntry.origAmount}/${acctgTransEntry.amount} ${(acctgTransEntry.origCurrencyUomId)!}/${(acctgTransEntry.currencyUomId)!}</#if></fo:block></fo:table-cell>
                                        <fo:table-cell border=".1mm solid"><fo:block text-align="right" font-size="6pt"><#if "D" == (acctgTransEntry.debitCreditFlag)!><@ofbizCurrency amount=acctgTransEntry.amount isoCode=acctgTransEntry.currencyUomId/></#if></fo:block></fo:table-cell>
                                        <fo:table-cell border=".1mm solid"><fo:block text-align="right" font-size="6pt"><#if "C" == (acctgTransEntry.debitCreditFlag)!><@ofbizCurrency amount=acctgTransEntry.amount isoCode=acctgTransEntry.currencyUomId/></#if></fo:block></fo:table-cell>
                                    </fo:table-row>
                                </#list>
                            </fo:table-body>
                        </fo:table>
                    </fo:block>
                <#else>
                    <fo:block text-align="center">${uiLabelMap.AccountingNoAcctgTransFound}</fo:block>
                </#if>
            </fo:flow>
        </fo:page-sequence>
    </fo:root>
</#escape>
