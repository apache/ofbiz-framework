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
    <fo:block font-size="14pt" font-weight="bold" text-align="center">${uiLabelMap.AccountingDepositSlip}</fo:block>
    <fo:block font-size="12pt" text-align="left"  font-weight="bold">
         <#if paymentGroup?has_content>
            ${uiLabelMap.AccountingPaymentGroupId} : ${parameters.paymentGroupId!}
        <#else>
            ${uiLabelMap.FormFieldTitle_finAccountTransId} : ${parameters.finAccountTransId!}
        </#if>
    </fo:block>
    <fo:block font-size="12pt" text-align="left">
        <#if paymentGroup?has_content>
            ${uiLabelMap.AccountingPaymentGroupName} : ${paymentGroup.paymentGroupName!}
        </#if>
    </fo:block>
    <fo:block><fo:leader/></fo:block>
    <fo:block space-after.optimum="10pt" font-size="10pt">
        <fo:table table-layout="fixed" width="100%">
            <fo:table-column column-width="75pt"/>
            <fo:table-column column-width="100pt"/>
            <fo:table-column column-width="135pt"/>
            <fo:table-column column-width="150pt"/>
            <fo:table-column column-width="60pt"/>
            <fo:table-header>
                <fo:table-row font-weight="bold">
                    <fo:table-cell padding="2pt" background-color="#D4D0C8" border="1pt solid" border-width=".1mm">
                        <fo:block>${uiLabelMap.FormFieldTitle_paymentId}</fo:block>
                    </fo:table-cell>
                    <fo:table-cell padding="2pt" background-color="#D4D0C8" border="1pt solid" border-width=".1mm">
                        <fo:block text-align="center">${uiLabelMap.AccountingReferenceNumber}</fo:block>
                    </fo:table-cell>
                    <fo:table-cell padding="2pt" background-color="#D4D0C8" border="1pt solid" border-width=".1mm">
                        <fo:block text-align="right">${uiLabelMap.AccountingPaymentMethod}</fo:block>
                    </fo:table-cell>
                    <fo:table-cell padding="2pt" background-color="#D4D0C8" border="1pt solid" border-width=".1mm">
                        <fo:block text-align="right">${uiLabelMap.AccountingFromParty}</fo:block>
                    </fo:table-cell>
                    <fo:table-cell padding="2pt" background-color="#D4D0C8" border="1pt solid" border-width=".1mm">
                        <fo:block text-align="right">${uiLabelMap.AccountingAmount}</fo:block>
                    </fo:table-cell>
                </fo:table-row>
            </fo:table-header>
            <fo:table-body>
                <#if payments?has_content>
                    <#assign totalAmount = 0>
                    <#list payments as payment>
                        <#if payment.paymentTypeId?has_content>
                            <#assign paymentType = delegator.findOne("PaymentType", {"paymentTypeId" : payment.paymentTypeId}, false)/>
                        </#if>
                        <#if payment.partyIdFrom?has_content>
                            <#assign partyName = delegator.findOne("PartyNameView", {"partyId" : payment.partyIdFrom}, false)/>
                        </#if>
                        <#if payment.paymentMethodTypeId?has_content>
                            <#assign paymentMethodType = delegator.findOne("PaymentMethodType", {"paymentMethodTypeId" : payment.paymentMethodTypeId}, false)/>
                            <#if "CREDIT_CARD" == payment.paymentMethodTypeId!>
                                <#assign creditCard = delegator.findOne("CreditCard", {"paymentMethodId" : payment.paymentMethodId}, false)/>
                            </#if>
                        </#if>
                        <fo:table-row>
                            <fo:table-cell padding="2pt" border="1pt solid" border-width=".1mm">
                                <fo:block>${payment.paymentId!}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell padding="2pt" border="1pt solid" border-width=".1mm">
                                <fo:block text-align="center">
                                    ${payment.paymentRefNum!}
                                </fo:block>
                            </fo:table-cell>
                            <fo:table-cell padding="2pt" border="1pt solid" border-width=".1mm">
                                <fo:block text-align="right">${paymentMethodType.description!} <#if creditCard?has_content && creditCard??>(${creditCard.cardType!})</#if></fo:block>
                            </fo:table-cell>
                            <fo:table-cell padding="2pt" border="1pt solid" border-width=".1mm">
                                <fo:block text-align="right">
                                    <#if partyName?has_content>
                                        <#if "PERSON" == partyName.partyTypeId>
                                            ${(partyName.firstName)!} ${(partyName.lastName)!}
                                        <#elseif "PARTY_GROUP" == (partyName.partyTypeId)!>
                                            ${(partyName.groupName)!}
                                        </#if>
                                    </#if>
                                </fo:block>
                            </fo:table-cell>
                            <fo:table-cell padding="2pt" border="1pt solid" border-width=".1mm">
                                <fo:block text-align="right">
                                    <@ofbizCurrency amount=payment.amount! isoCode=payment.currencyUomId!/>
                                    <#assign totalAmount = totalAmount + payment.amount!/>
                                    <#assign currencyUomId = payment.currencyUomId!/>
                                </fo:block>
                            </fo:table-cell>
                        </fo:table-row>
                    </#list>
                    <fo:table-row>
                        <fo:table-cell number-columns-spanned="3"><fo:block/></fo:table-cell>
                        <fo:table-cell padding="4pt" background-color="#D4D0C8" font-weight="bold">
                            <fo:block text-align="right">
                                ${uiLabelMap.FormFieldTitle_totalAmount}
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell padding="4pt" background-color="#D4D0C8">
                            <fo:block text-align="right">
                                <@ofbizCurrency amount=totalAmount! isoCode=currencyUomId!/>
                            </fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                <#else>
                    <fo:table-row>
                        <fo:table-cell number-columns-spanned="2"><fo:block/></fo:table-cell>
                        <fo:table-cell padding="2pt">
                            <fo:block>${uiLabelMap.CommonNoRecordFound}</fo:block>
                        </fo:table-cell>
                        <fo:table-cell number-columns-spanned="2"><fo:block/></fo:table-cell>
                    </fo:table-row>
                </#if>
            </fo:table-body>
        </fo:table>
    </fo:block>
</#escape>
