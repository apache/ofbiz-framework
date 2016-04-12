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

<#--
Generates PDF of multiple checks in two styles: one check per page, multiple checks per page
Note that this must be customized to fit specific check layouts. The layout here is copied
by hand from a real template using a ruler.
-->
<#escape x as x?xml>

<fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">
    <fo:layout-master-set>
    <#-- define the margins of the check layout here -->
        <fo:simple-page-master master-name="checks" page-height="27.9cm" page-width="21.6cm">
            <fo:region-body/>
        </fo:simple-page-master>

    </fo:layout-master-set>

    <fo:page-sequence master-reference="checks">
        <fo:flow flow-name="xsl-region-body">
            <#if !security.hasEntityPermission("ACCOUNTING", "_PRINT_CHECKS", session)>
                <fo:block padding="20pt">${uiLabelMap.AccountingPrintChecksPermissionError}</fo:block>
            <#else>
                <#if payments.size() == 0>
                    <fo:block padding="20pt">${uiLabelMap.AccountingPaymentCheckMessage1}</fo:block>
                </#if>
                <#list payments as payment>
                    <#assign paymentApplications = payment.getRelated("PaymentApplication", null, null, false)>
                    <fo:block font-size="10pt" break-before="page"> <#-- this produces a page break if this block cannot fit on the current page -->
                        
                        <#-- the check: note that the format is fairly precise -->
                        
                        <#-- this seems to be the only way to force a fixed height in fop -->
                        <fo:table table-layout="fixed" width="100%">
                            <fo:table-column column-width="100%"/>
                            <fo:table-body>
                                <fo:table-row height="8.85cm">
                                    <fo:table-cell>
                                        <fo:table table-layout="fixed" width="100%">
                                            <fo:table-column column-width="17.7cm"/>
                                            <fo:table-column column-width="3cm"/>
                                            <fo:table-body>
                                                <fo:table-row>
                                                    <fo:table-cell><fo:block/></fo:table-cell>
                                                    <fo:table-cell>
                                                        <fo:block padding-before="2.2cm">${payment.effectiveDate?date?string.short}</fo:block>
                                                    </fo:table-cell>
                                                </fo:table-row>
                                                <fo:table-row>
                                                    <fo:table-cell padding-before="0.8cm">
                                                        <fo:block margin-left="3.0cm">
                                                            <#assign toPartyNameResult = dispatcher.runSync("getPartyNameForDate", Static["org.ofbiz.base.util.UtilMisc"].toMap("partyId", payment.partyIdTo, "compareDate", payment.effectiveDate, "userLogin", userLogin))/>
                                                            ${toPartyNameResult.fullName?default("Name Not Found")}
                                                        </fo:block>
                                                    </fo:table-cell>
                                                    <fo:table-cell padding-before="0.8cm">
                                                        <fo:block>**${payment.getBigDecimal("amount").setScale(decimals, rounding).toString()}</fo:block>
                                                    </fo:table-cell>
                                                </fo:table-row>
                                                <fo:table-row>
                                                    <fo:table-cell number-columns-spanned="2">
                                                        <#assign amount = Static["org.ofbiz.base.util.UtilNumber"].formatRuleBasedAmount(payment.getDouble("amount"), "%dollars-and-hundredths", locale).toUpperCase()>
                                                        <fo:block padding-before="0.4cm" margin-left="1.3cm">${amount}<#list 1..(100-amount.length()) as x>*</#list></fo:block>
                                                    </fo:table-cell>
                                                </fo:table-row>
                                            </fo:table-body>
                                        </fo:table>
                                    </fo:table-cell>
                                </fo:table-row>
                            </fo:table-body>
                        </fo:table>
                        <#-- payment applications (twice: both blocks are exactly the same) -->
                    
                        <#-- this seems to be the only way to force a fixed height in fop -->
                        <fo:table table-layout="fixed" width="100%">
                            <fo:table-column column-width="100%"/>
                            <fo:table-body>
                                <fo:table-row height="9.3cm">
                                    <fo:table-cell>
                                        <fo:table table-layout="fixed" margin-left="5pt" margin-right="5pt" width="100%">
                                            <fo:table-column column-width="3cm"/>
                                            <fo:table-column column-width="3cm"/>
                                            <fo:table-column column-width="3cm"/>
                                            <fo:table-column column-width="3cm"/>
                                            <fo:table-column column-width="3cm"/>
                                            <fo:table-column column-width="3cm"/>
                                            <fo:table-column column-width="3cm"/>
                                            <fo:table-header>
                                                <fo:table-row>
                                                    <fo:table-cell padding="3pt" number-columns-spanned="3" text-align="center">
                                                        <fo:block text-align="center">
                                                            <#assign toPartyNameResult = dispatcher.runSync("getPartyNameForDate", Static["org.ofbiz.base.util.UtilMisc"].toMap("partyId", payment.partyIdTo, "compareDate", payment.effectiveDate, "userLogin", userLogin))/>
                                                            ${toPartyNameResult.fullName?default("Name Not Found")}
                                                        </fo:block>
                                                    </fo:table-cell>
                                                    <fo:table-cell padding="3pt" number-columns-spanned="4" text-align="center">
                                                        <fo:block text-align="center">${payment.effectiveDate?date?string.short}</fo:block>
                                                    </fo:table-cell>
                                                </fo:table-row>
                                                <fo:table-row>
                                                    <fo:table-cell padding="3pt">
                                                        <fo:block font-weight="bold">${uiLabelMap.CommonDate}</fo:block>
                                                    </fo:table-cell>
                                                    <fo:table-cell padding="3pt">
                                                        <fo:block font-weight="bold">${uiLabelMap.CommonType}</fo:block>
                                                    </fo:table-cell>
                                                    <fo:table-cell padding="3pt">
                                                        <fo:block font-weight="bold">${uiLabelMap.AccountingReferenceNumber}</fo:block>
                                                    </fo:table-cell>
                                                    <fo:table-cell padding="3pt"><fo:block/></fo:table-cell>
                                                    <fo:table-cell padding="3pt">
                                                        <fo:block font-weight="bold" text-align="right">${uiLabelMap.AccountingPaymentOriginalAmount}</fo:block>
                                                    </fo:table-cell>
                                                    <fo:table-cell padding="3pt">
                                                        <fo:block font-weight="bold" text-align="right">${uiLabelMap.AccountingBalanceDue}</fo:block>
                                                    </fo:table-cell>
                                                    <fo:table-cell padding="3pt">
                                                        <fo:block font-weight="bold" text-align="right">${uiLabelMap.AccountingPayment}</fo:block>
                                                    </fo:table-cell>
                                                </fo:table-row>
                                            </fo:table-header>
                                            <fo:table-body>
                                                <#list paymentApplications as paymentApplication>
                                                    <#assign invoice = paymentApplication.getRelatedOne("Invoice", false)!>
                                                    <fo:table-row>
                                                        <fo:table-cell padding="3pt">
                                                            <fo:block>${payment.effectiveDate?date?string.short}</fo:block>
                                                        </fo:table-cell>
                                                        <fo:table-cell padding="3pt">
                                                            <fo:block><#if invoice??>${uiLabelMap.AccountingInvoice} : ${invoice.invoiceId}</#if></fo:block>
                                                        </fo:table-cell>
                                                        <fo:table-cell padding="3pt">
                                                            <fo:block>
                                                                <#if invoice??>${invoice.referenceNumber!}</#if>
                                                                ${paymentApplication.taxAuthGeoId!}
                                                            </fo:block>
                                                        </fo:table-cell>
                                                        <fo:table-cell padding="3pt"><fo:block/></fo:table-cell>
                                                        <fo:table-cell padding="3pt"><fo:block/></fo:table-cell>
                                                        <fo:table-cell padding="3pt"><fo:block/></fo:table-cell>
                                                        <fo:table-cell padding="3pt">
                                                            <fo:block text-align="end">${paymentApplication.getBigDecimal("amountApplied").setScale(decimals, rounding).toString()}</fo:block>
                                                        </fo:table-cell>
                                                    </fo:table-row>
                                                        <#if invoice.invoiceTypeId! == "PAYROL_INVOICE">
                                                            <#assign InvoiceItems = invoice.getRelated("InvoiceItem", null, null, false)!>
                                                            <#assign PayrolGroups = PayrolGroup!>
                                                            <#list PayrolGroups as payrolGroup>
                                                                <#assign fontSize = "75%">
                                                                <#assign lineStyle = "dashed">
                                                                <#assign sumQuantity = 0>
                                                                <#assign sumAmount = 0>
                                                                <#assign sumSubTotal = 0>
                                                                <#list InvoiceItems as invoiceItem>
                                                                    <#assign invoiceItemType = invoiceItem.getRelatedOne("InvoiceItemType", false)!>
                                                                    <#assign quantity = 0>
                                                                    <#assign amount = 0>
                                                                    <#assign subTotal = 0>
                                                                    <#if invoiceItemType.parentTypeId == payrolGroup.invoiceItemTypeId>
                                                                        <#if invoiceItem.quantity?has_content><#assign quantity = invoiceItem.quantity!><#else><#assign quantity = 0></#if>
                                                                        <#if invoiceItem.amount?has_content><#assign amount = invoiceItem.amount!><#else><#assign amount = 0></#if>
                                                                        <#if amount != 0 && quantity == 0 ><#assign quantity = 1></#if>
                                                                        <#assign subTotal = quantity * amount>
                                                                        <#assign sumQuantity = sumQuantity + quantity>
                                                                        <#assign sumAmount = sumAmount + amount>
                                                                        <#assign sumSubTotal = sumSubTotal + subTotal>
                                                                    </#if>
                                                                </#list>
                                                                <#if sumSubTotal != 0>
                                                                    <fo:table-row font-size="${fontSize}">
                                                                        <fo:table-cell padding="3pt"><fo:block/></fo:table-cell>
                                                                        <fo:table-cell padding="3pt" number-columns-spanned="3" border-bottom-style="${lineStyle}">
                                                                            <fo:block font-weight="bold">${payrolGroup.description!}</fo:block>
                                                                        </fo:table-cell>
                                                                        <fo:table-cell padding="3pt" border-bottom-style="${lineStyle}" >
                                                                            <fo:block font-weight="bold" text-align="center">Quantity</fo:block>
                                                                        </fo:table-cell>
                                                                        <fo:table-cell padding="3pt" border-bottom-style="${lineStyle}" >
                                                                            <fo:block font-weight="bold" text-align="center">Amount</fo:block>
                                                                        </fo:table-cell>
                                                                        <fo:table-cell padding="3pt" border-bottom-style="${lineStyle}" >
                                                                            <fo:block font-weight="bold" text-align="center">Sum</fo:block>
                                                                        </fo:table-cell>
                                                                    </fo:table-row>
                                                                </#if>
                                                                <#assign sumQuantity = 0>
                                                                <#assign sumAmount = 0>
                                                                <#assign sumSubTotal = 0>
                                                                <#list InvoiceItems as invoiceItem>
                                                                    <#assign invoiceItemType = invoiceItem.getRelatedOne("InvoiceItemType", false)!>
                                                                    <#assign subTotal = 0>
                                                                    <#if invoiceItemType.parentTypeId == payrolGroup.invoiceItemTypeId>
                                                                        <#if invoiceItem.quantity?has_content><#assign quantity = invoiceItem.quantity!><#else><#assign quantity = 0></#if>
                                                                        <#if invoiceItem.amount?has_content><#assign amount = invoiceItem.amount!><#else><#assign amount = 0></#if>
                                                                        <#if amount != 0 && quantity == 0 ><#assign quantity = 1></#if>
                                                                        <#assign subTotal = quantity * amount>
                                                                        <#assign sumQuantity = sumQuantity + quantity>
                                                                        <#assign sumAmount = sumAmount + amount>
                                                                        <#assign sumSubTotal = sumSubTotal + subTotal>
                                                                        <fo:table-row font-size="${fontSize}">
                                                                            <fo:table-cell padding="3pt"><fo:block/></fo:table-cell>
                                                                            <fo:table-cell padding="3pt" number-columns-spanned="3"><fo:block>${invoiceItemType.description!}</fo:block></fo:table-cell>
                                                                            <fo:table-cell padding="3pt"><fo:block text-align="center">${quantity!}</fo:block></fo:table-cell>
                                                                            <fo:table-cell padding="3pt"><fo:block text-align="center">${amount!}</fo:block></fo:table-cell>
                                                                            <fo:table-cell padding="3pt"><fo:block text-align="center">${subTotal!}</fo:block></fo:table-cell>
                                                                        </fo:table-row>
                                                                    </#if>
                                                            </#list>
                                                            <#assign sumQuantity = 0>
                                                            <#assign sumAmount = 0>
                                                            <#assign sumSubTotal = 0>
                                                            <#list InvoiceItems as invoiceItem>
                                                                <#assign invoiceItemType = invoiceItem.getRelatedOne("InvoiceItemType", false)!>
                                                                <#assign subTotal = 0>
                                                                <#if invoiceItemType.parentTypeId == payrolGroup.invoiceItemTypeId>
                                                                    <#if invoiceItem.quantity?has_content><#assign quantity = invoiceItem.quantity!><#else><#assign quantity = 0></#if>
                                                                    <#if invoiceItem.amount?has_content><#assign amount = invoiceItem.amount!><#else><#assign amount = 0></#if>
                                                                    <#if amount != 0 && quantity == 0><#assign quantity = 1></#if>
                                                                    <#assign subTotal = quantity * amount>
                                                                    <#assign sumQuantity = sumQuantity + quantity>
                                                                    <#assign sumAmount = sumAmount + amount>
                                                                    <#assign sumSubTotal = sumSubTotal + subTotal>
                                                                </#if>
                                                            </#list>
                                                            <#if sumSubTotal != 0>
                                                                <fo:table-row font-size="${fontSize}">
                                                                    <fo:table-cell padding="3pt"><fo:block/></fo:table-cell>
                                                                    <fo:table-cell padding="3pt" number-columns-spanned="3" border-top-style="${lineStyle}"><fo:block/></fo:table-cell>
                                                                    <fo:table-cell padding="3pt" border-top-style="${lineStyle}"><fo:block text-align="center">${sumQuantity!}</fo:block></fo:table-cell>
                                                                    <fo:table-cell padding="3pt" border-top-style="${lineStyle}"><fo:block text-align="center">${sumAmount!}</fo:block></fo:table-cell>
                                                                    <fo:table-cell padding="3pt" border-top-style="solid" border-bottom-style="${lineStyle}"><fo:block text-align="right">${sumSubTotal!}</fo:block></fo:table-cell>
                                                                </fo:table-row>
                                                                <fo:table-row font-size="${fontSize}">
                                                                    <fo:table-cell padding="3pt"><fo:block/></fo:table-cell>
                                                                </fo:table-row>
                                                            </#if>
                                                        </#list>
                                                        <fo:table-row font-size="${fontSize}">
                                                            <fo:table-cell padding="3pt"><fo:block/></fo:table-cell>
                                                            <fo:table-cell padding="3pt" number-columns-spanned="6" border-top-style="solid"><fo:block/></fo:table-cell>
                                                        </fo:table-row>
                                                    </#if>
                                                </#list>
                                                <fo:table-row>
                                                    <fo:table-cell padding="3pt" number-columns-spanned="6">
                                                        <fo:block text-align="end">${uiLabelMap.AccountingCheckAmount}</fo:block>
                                                    </fo:table-cell>
                                                    <fo:table-cell padding="3pt">
                                                        <fo:block text-align="end">${payment.getBigDecimal("amount").setScale(decimals, rounding).toString()}</fo:block>
                                                    </fo:table-cell>
                                                </fo:table-row>
                                            </fo:table-body>
                                        </fo:table>
                                    </fo:table-cell>
                                </fo:table-row>
                            </fo:table-body>
                        </fo:table>

                        <#-- copy of above -->
                        <#-- this seems to be the only way to force a fixed height in fop -->
                        <fo:table table-layout="fixed" width="100%">
                            <fo:table-column column-width="100%"/>
                            <fo:table-body>
                                <fo:table-row height="9.3cm">
                                    <fo:table-cell>
                                        <fo:table table-layout="fixed" margin-left="5pt" margin-right="5pt" width="100%">
                                            <fo:table-column column-width="3cm"/>
                                            <fo:table-column column-width="3cm"/>
                                            <fo:table-column column-width="3cm"/>
                                            <fo:table-column column-width="3cm"/>
                                            <fo:table-column column-width="3cm"/>
                                            <fo:table-column column-width="3cm"/>
                                            <fo:table-column column-width="3cm"/>
                                            <fo:table-header>
                                                <fo:table-row>
                                                    <fo:table-cell padding="3pt" number-columns-spanned="3" text-align="center">
                                                        <fo:block text-align="center">
                                                            ${Static["org.ofbiz.party.party.PartyHelper"].getPartyName(delegator, payment.partyIdTo, false)}
                                                        </fo:block>
                                                    </fo:table-cell>
                                                    <fo:table-cell padding="3pt" number-columns-spanned="4" text-align="center">
                                                        <fo:block text-align="center">${payment.effectiveDate?date?string.short}</fo:block>
                                                    </fo:table-cell>
                                                </fo:table-row>
                                                <fo:table-row>
                                                    <fo:table-cell padding="3pt">
                                                        <fo:block font-weight="bold">${uiLabelMap.CommonDate}</fo:block>
                                                    </fo:table-cell>
                                                    <fo:table-cell padding="3pt">
                                                        <fo:block font-weight="bold">${uiLabelMap.CommonType}</fo:block>
                                                    </fo:table-cell>
                                                    <fo:table-cell padding="3pt">
                                                        <fo:block font-weight="bold">${uiLabelMap.AccountingReferenceNumber}</fo:block>
                                                    </fo:table-cell>
                                                    <fo:table-cell padding="3pt"><fo:block/></fo:table-cell>
                                                    <fo:table-cell padding="3pt">
                                                        <fo:block font-weight="bold" text-align="right">${uiLabelMap.AccountingPaymentOriginalAmount}</fo:block>
                                                    </fo:table-cell>
                                                    <fo:table-cell padding="3pt">
                                                        <fo:block font-weight="bold" text-align="right">${uiLabelMap.AccountingBalanceDue}</fo:block>
                                                    </fo:table-cell>
                                                    <fo:table-cell padding="3pt">
                                                        <fo:block font-weight="bold" text-align="right">${uiLabelMap.AccountingPayment}</fo:block>
                                                    </fo:table-cell>
                                                </fo:table-row>
                                            </fo:table-header>
                                            <fo:table-body>
                                                <#list paymentApplications as paymentApplication>
                                                    <#assign invoice = paymentApplication.getRelatedOne("Invoice", false)!>
                                                    <fo:table-row>
                                                        <fo:table-cell padding="3pt">
                                                            <fo:block>${payment.effectiveDate?date?string.short}</fo:block>
                                                        </fo:table-cell>
                                                        <fo:table-cell padding="3pt">
                                                            <fo:block><#if invoice??>${uiLabelMap.AccountingInvoice} : ${invoice.invoiceId}</#if></fo:block>
                                                        </fo:table-cell>
                                                        <fo:table-cell padding="3pt">
                                                            <fo:block>
                                                                <#if invoice??>${invoice.referenceNumber!}</#if>
                                                                ${paymentApplication.taxAuthGeoId!}
                                                            </fo:block>
                                                        </fo:table-cell>
                                                        <fo:table-cell padding="3pt"><fo:block/></fo:table-cell>
                                                        <fo:table-cell padding="3pt"><fo:block/></fo:table-cell>
                                                        <fo:table-cell padding="3pt"><fo:block/></fo:table-cell>
                                                        <fo:table-cell padding="3pt">
                                                            <fo:block text-align="end">${paymentApplication.getBigDecimal("amountApplied").setScale(decimals, rounding).toString()}</fo:block>
                                                        </fo:table-cell>
                                                    </fo:table-row>
                                                    
                                                    <#if invoice.invoiceTypeId! == "PAYROL_INVOICE">
                                                        <#assign InvoiceItems = invoice.getRelated("InvoiceItem", null, null, false)!>
                                                        <#assign PayrolGroups = PayrolGroup!>
                                                        <#list PayrolGroups as payrolGroup>
                                                            <#assign fontSize = "75%">
                                                            <#assign lineStyle = "dashed">
                                                            <#assign sumQuantity = 0>
                                                            <#assign sumAmount = 0>
                                                            <#assign sumSubTotal = 0>
                                                            <#list InvoiceItems as invoiceItem>
                                                                <#assign invoiceItemType = invoiceItem.getRelatedOne("InvoiceItemType", false)!>
                                                                <#assign quantity = 0>
                                                                <#assign amount = 0>
                                                                <#assign subTotal = 0>
                                                                <#if invoiceItemType.parentTypeId == payrolGroup.invoiceItemTypeId>
                                                                    <#if invoiceItem.quantity?has_content><#assign quantity = invoiceItem.quantity!><#else><#assign quantity = 0></#if>
                                                                    <#if invoiceItem.amount?has_content><#assign amount = invoiceItem.amount!><#else><#assign amount = 0></#if>
                                                                    <#if amount != 0 && quantity == 0 ><#assign quantity = 1></#if>
                                                                    <#assign subTotal = quantity * amount>
                                                                    <#assign sumQuantity = sumQuantity + quantity>
                                                                    <#assign sumAmount = sumAmount + amount>
                                                                    <#assign sumSubTotal = sumSubTotal + subTotal>
                                                                </#if>
                                                            </#list>
                                                            <#if sumSubTotal != 0>
                                                                <fo:table-row font-size="${fontSize}">
                                                                    <fo:table-cell padding="3pt"><fo:block/></fo:table-cell>
                                                                    <fo:table-cell padding="3pt" number-columns-spanned="3" border-bottom-style="${lineStyle}">
                                                                        <fo:block font-weight="bold">${payrolGroup.description!}</fo:block>
                                                                    </fo:table-cell>
                                                                    <fo:table-cell padding="3pt" border-bottom-style="${lineStyle}" >
                                                                        <fo:block font-weight="bold" text-align="center">Quantity</fo:block>
                                                                    </fo:table-cell>
                                                                    <fo:table-cell padding="3pt" border-bottom-style="${lineStyle}" >
                                                                        <fo:block font-weight="bold" text-align="center">Amount</fo:block>
                                                                    </fo:table-cell>
                                                                    <fo:table-cell padding="3pt" border-bottom-style="${lineStyle}" >
                                                                        <fo:block font-weight="bold" text-align="center">Sum</fo:block>
                                                                    </fo:table-cell>
                                                                </fo:table-row>
                                                            </#if>
                                                            <#assign sumQuantity = 0>
                                                            <#assign sumAmount = 0>
                                                            <#assign sumSubTotal = 0>
                                                            <#list InvoiceItems as invoiceItem>
                                                                <#assign invoiceItemType = invoiceItem.getRelatedOne("InvoiceItemType", false)!>
                                                                <#assign subTotal = 0>
                                                                <#if invoiceItemType.parentTypeId == payrolGroup.invoiceItemTypeId>
                                                                    <#if invoiceItem.quantity?has_content><#assign quantity = invoiceItem.quantity!><#else><#assign quantity = 0></#if>
                                                                    <#if invoiceItem.amount?has_content><#assign amount = invoiceItem.amount!><#else><#assign amount = 0></#if>
                                                                    <#if amount != 0 && quantity == 0 ><#assign quantity = 1></#if>
                                                                    <#assign subTotal = quantity * amount>
                                                                    <#assign sumQuantity = sumQuantity + quantity>
                                                                    <#assign sumAmount = sumAmount + amount>
                                                                    <#assign sumSubTotal = sumSubTotal + subTotal>
                                                                    <fo:table-row font-size="${fontSize}">
                                                                        <fo:table-cell padding="3pt"><fo:block/></fo:table-cell>
                                                                        <fo:table-cell padding="3pt" number-columns-spanned="3"><fo:block>${invoiceItemType.description!}</fo:block></fo:table-cell>
                                                                        <fo:table-cell padding="3pt"><fo:block text-align="center">${quantity!}</fo:block></fo:table-cell>
                                                                        <fo:table-cell padding="3pt"><fo:block text-align="center">${amount!}</fo:block></fo:table-cell>
                                                                        <fo:table-cell padding="3pt"><fo:block text-align="center">${subTotal!}</fo:block></fo:table-cell>
                                                                    </fo:table-row>
                                                                </#if>
                                                            </#list>
                                                            <#assign sumQuantity = 0>
                                                            <#assign sumAmount = 0>
                                                            <#assign sumSubTotal = 0>
                                                            <#list InvoiceItems as invoiceItem>
                                                                <#assign invoiceItemType = invoiceItem.getRelatedOne("InvoiceItemType", false)!>
                                                                <#assign subTotal = 0>
                                                                <#if invoiceItemType.parentTypeId == payrolGroup.invoiceItemTypeId>
                                                                    <#if invoiceItem.quantity?has_content><#assign quantity = invoiceItem.quantity!><#else><#assign quantity = 0></#if>
                                                                    <#if invoiceItem.amount?has_content><#assign amount = invoiceItem.amount!><#else><#assign amount = 0></#if>
                                                                    <#if amount != 0 && quantity == 0><#assign quantity = 1></#if>
                                                                    <#assign subTotal = quantity * amount>
                                                                    <#assign sumQuantity = sumQuantity + quantity>
                                                                    <#assign sumAmount = sumAmount + amount>
                                                                    <#assign sumSubTotal = sumSubTotal + subTotal>
                                                                </#if>
                                                            </#list>
                                                            <#if sumSubTotal != 0>
                                                                <fo:table-row font-size="${fontSize}">
                                                                    <fo:table-cell padding="3pt"><fo:block/></fo:table-cell>
                                                                    <fo:table-cell padding="3pt" number-columns-spanned="3" border-top-style="${lineStyle}"><fo:block/></fo:table-cell>
                                                                    <fo:table-cell padding="3pt" border-top-style="${lineStyle}"><fo:block text-align="center">${sumQuantity!}</fo:block></fo:table-cell>
                                                                    <fo:table-cell padding="3pt" border-top-style="${lineStyle}"><fo:block text-align="center">${sumAmount!}</fo:block></fo:table-cell>
                                                                    <fo:table-cell padding="3pt" border-top-style="solid" border-bottom-style="${lineStyle}"><fo:block text-align="right">${sumSubTotal!}</fo:block></fo:table-cell>
                                                                </fo:table-row>
                                                                <fo:table-row font-size="${fontSize}">
                                                                    <fo:table-cell padding="3pt"><fo:block/></fo:table-cell>
                                                                </fo:table-row>
                                                            </#if>
                                                        </#list>
                                                        <fo:table-row font-size="${fontSize}">
                                                            <fo:table-cell padding="3pt"><fo:block/></fo:table-cell>
                                                            <fo:table-cell padding="3pt" number-columns-spanned="6" border-top-style="solid"><fo:block/></fo:table-cell>
                                                        </fo:table-row>
                                                    </#if>
                                                </#list>
                                                <fo:table-row>
                                                    <fo:table-cell padding="3pt" number-columns-spanned="6">
                                                        <fo:block text-align="end">${uiLabelMap.AccountingCheckAmount}</fo:block>
                                                    </fo:table-cell>
                                                    <fo:table-cell padding="3pt">
                                                        <fo:block text-align="end">${payment.getBigDecimal("amount").setScale(decimals, rounding).toString()}</fo:block>
                                                    </fo:table-cell>
                                                </fo:table-row>
                                            </fo:table-body>
                                        </fo:table>
                                    </fo:table-cell>
                                </fo:table-row>
                            </fo:table-body>
                        </fo:table>
                    </fo:block>
                </#list>
            </#if> <#-- security if -->
        </fo:flow>
    </fo:page-sequence>
</fo:root>
</#escape>
