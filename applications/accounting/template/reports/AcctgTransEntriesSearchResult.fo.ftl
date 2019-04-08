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
                        <#if partyName.partyTypeId == "PERSON">
                            ${(partyName.firstName)!} ${(partyName.lastName)!}
                        <#elseif (partyName.partyTypeId)! == "PARTY_GROUP">
                            ${(partyName.groupName)!}
                        </#if>
                    </fo:block>
                    <fo:block></fo:block>
                    <fo:block>
                        <fo:table>
                            <fo:table-column column-width="20mm"/>
                            <fo:table-column column-width="20mm"/>
                            <fo:table-column column-width="15mm"/>
                            <fo:table-column column-width="35mm"/>
                            <fo:table-column column-width="25mm"/>
                            <fo:table-column column-width="25mm"/>
                            <fo:table-column column-width="15mm"/>
                            <fo:table-column column-width="20mm"/>
                            <fo:table-column column-width="35mm"/>
                            <fo:table-column column-width="15mm"/>
                            <fo:table-column column-width="15mm"/>
                            <fo:table-column column-width="15mm"/>
                            <fo:table-column column-width="15mm"/>
                            <fo:table-column column-width="15mm"/>
                            <fo:table-column column-width="25mm"/>
                            <fo:table-column column-width="20mm"/>
                            <fo:table-header>
                                <fo:table-cell border="1pt solid" border-width=".1mm">
                                    <fo:block text-align="center" font-size="6pt">${uiLabelMap.AccountingAcctgTrans}</fo:block>
                                </fo:table-cell>
                                <fo:table-cell border="1pt solid" border-width=".1mm">
                                    <fo:block text-align="center" font-size="6pt">${uiLabelMap.FormFieldTitle_transactionDate}</fo:block>
                                </fo:table-cell>
                                <fo:table-cell border="1pt solid" border-width=".1mm">
                                    <fo:block text-align="center" font-size="6pt">${uiLabelMap.FormFieldTitle_glAccountId}</fo:block>
                                </fo:table-cell>
                                <fo:table-cell border="1pt solid" border-width=".1mm">
                                    <fo:block text-align="center" font-size="6pt">${uiLabelMap.FormFieldTitle_glAccountClassId}</fo:block>
                                </fo:table-cell>
                                <fo:table-cell border="1pt solid" border-width=".1mm">
                                    <fo:block text-align="center" font-size="6pt">${uiLabelMap.FormFieldTitle_invoiceId}</fo:block>
                                </fo:table-cell>
                                <fo:table-cell border="1pt solid" border-width=".1mm">
                                    <fo:block text-align="center" font-size="6pt">${uiLabelMap.FormFieldTitle_paymentId} - (${uiLabelMap.AccountingPaymentType})</fo:block>
                                </fo:table-cell>
                                <fo:table-cell border="1pt solid" border-width=".1mm">
                                    <fo:block text-align="center" font-size="6pt">${uiLabelMap.FormFieldTitle_workEffortId}</fo:block>
                                </fo:table-cell>
                                <fo:table-cell border="1pt solid" border-width=".1mm">
                                    <fo:block text-align="center" font-size="6pt">${uiLabelMap.FormFieldTitle_shipmentId}</fo:block>
                                </fo:table-cell>
                                <fo:table-cell border="1pt solid" border-width=".1mm">
                                    <fo:block text-align="center" font-size="6pt">${uiLabelMap.CommonPartyId}</fo:block>
                                </fo:table-cell>
                                <fo:table-cell border="1pt solid" border-width=".1mm">
                                    <fo:block text-align="center" font-size="6pt">${uiLabelMap.AccountingProductId}</fo:block>
                                </fo:table-cell>
                                <fo:table-cell border="1pt solid" border-width=".1mm">
                                    <fo:block text-align="center" font-size="6pt">${uiLabelMap.FormFieldTitle_isPosted}</fo:block>
                                </fo:table-cell>
                                <fo:table-cell border="1pt solid" border-width=".1mm">
                                    <fo:block text-align="center" font-size="6pt">${uiLabelMap.FormFieldTitle_postedDate}</fo:block>
                                </fo:table-cell>
                                <fo:table-cell border="1pt solid" border-width=".1mm">
                                    <fo:block text-align="center" font-size="6pt">${uiLabelMap.AccountingCreditDebitFlag}</fo:block>
                                </fo:table-cell>
                                <fo:table-cell border="1pt solid" border-width=".1mm">
                                <fo:block text-align="center" font-size="6pt">${uiLabelMap.AccountingAmount}</fo:block>
                                </fo:table-cell>
                                <fo:table-cell border="1pt solid" border-width=".1mm">
                                    <fo:block text-align="center" font-size="6pt">${uiLabelMap.FormFieldTitle_acctgTransTypeId}</fo:block>
                                </fo:table-cell>
                                <fo:table-cell border="1pt solid" border-width=".1mm">
                                    <fo:block text-align="center" font-size="6pt">${uiLabelMap.FormFieldTitle_glFiscalTypeId}</fo:block>
                                </fo:table-cell>
                            </fo:table-header>
                            <fo:table-body>
                                <#list acctgTransEntryList as acctgTransEntry>
                                    <fo:table-row>
                                        <fo:table-cell border="1pt solid" border-width=".1mm">
                                            <fo:block text-align="center" font-size="5pt">${(acctgTransEntry.acctgTransId)!} - ${(acctgTransEntry.acctgTransEntrySeqId)!}</fo:block>        
                                        </fo:table-cell>
                                        <fo:table-cell border="1pt solid" border-width=".1mm">
                                            <fo:block text-align="center" font-size="5pt">
                                                <#assign dateFormat = Static["java.text.DateFormat"].LONG/>
                                                <#assign transactionDate = Static["java.text.DateFormat"].getDateInstance(dateFormat, locale).format((acctgTransEntry.transactionDate)!)/>
                                                ${(transactionDate)!}
                                            </fo:block>
                                        </fo:table-cell>
                                        <fo:table-cell border="1pt solid" border-width=".1mm">
                                            <fo:block text-align="center" font-size="5pt">${(acctgTransEntry.glAccountId)!}</fo:block>        
                                        </fo:table-cell>
                                        <fo:table-cell border="1pt solid" border-width=".1mm">
                                            <fo:block text-align="center" font-size="5pt">
                                                <#if (acctgTransEntry.glAccountClassId)??>
                                                    <#assign glAccountClass = (delegator.findOne("GlAccountClass", {"glAccountClassId" : (acctgTransEntry.glAccountClassId)!}, false))!/>
                                                    <#if (glAccountClass?has_content)>${(glAccountClass.description)!}</#if>
                                                </#if>
                                            </fo:block>        
                                        </fo:table-cell>
                                        <fo:table-cell border="1pt solid" border-width=".1mm">
                                            <fo:block text-align="center" font-size="5pt">
                                                ${(acctgTransEntry.invoiceId)!}
                                            </fo:block>     
                                        </fo:table-cell>
                                        <fo:table-cell border="1pt solid" border-width=".1mm">
                                            <fo:block text-align="center" font-size="5pt">
                                                <#if (acctgTransEntry.paymentId)??>
                                                    <#assign paymentType = (delegator.findOne("Payment", {"paymentId" : (acctgTransEntry.paymentId)!}, false)).getRelatedOne("PaymentType", false)/>
                                                    ${(acctgTransEntry.paymentId)!}<#if (paymentType?has_content)> -(${(paymentType.description)!})</#if>
                                                </#if>
                                            </fo:block>
                                        </fo:table-cell>
                                        <fo:table-cell border="1pt solid" border-width=".1mm">
                                            <fo:block text-align="center" font-size="5pt">${(acctgTransEntry.workEffortId)!}</fo:block>        
                                        </fo:table-cell>
                                        <fo:table-cell border="1pt solid" border-width=".1mm">
                                            <fo:block text-align="center" font-size="5pt">
                                                ${(acctgTransEntry.shipmentId)!}
                                            </fo:block>        
                                        </fo:table-cell>
                                        <fo:table-cell border="1pt solid" border-width=".1mm">
                                            <fo:block text-align="center" font-size="5pt">${(acctgTransEntry.partyId)!}</fo:block>    
                                        </fo:table-cell>
                                        <fo:table-cell border="1pt solid" border-width=".1mm">
                                            <fo:block text-align="center" font-size="5pt">${(acctgTransEntry.productId)!}</fo:block>        
                                        </fo:table-cell>
                                        <fo:table-cell border="1pt solid" border-width=".1mm">
                                            <fo:block text-align="center" font-size="5pt">${(acctgTransEntry.isPosted)!}</fo:block>        
                                        </fo:table-cell>
                                        <fo:table-cell border="1pt solid" border-width=".1mm">
                                            <fo:block text-align="center" font-size="5pt">
                                                <#if acctgTransEntry.postedDate?has_content>
                                                    <#assign dateFormat = Static["java.text.DateFormat"].LONG>
                                                    <#assign postedDate = Static["java.text.DateFormat"].getDateInstance(dateFormat,locale).format((acctgTransEntry.postedDate)!)>
                                                    ${postedDate}
                                                </#if>
                                            </fo:block>        
                                        </fo:table-cell>
                                        <fo:table-cell border="1pt solid" border-width=".1mm">
                                            <fo:block text-align="center" font-size="5pt">${(acctgTransEntry.debitCreditFlag)!}</fo:block>        
                                        </fo:table-cell>
                                        <fo:table-cell border="1pt solid" border-width=".1mm">
                                            <fo:block text-align="center" font-size="5pt"><#if acctgTransEntry.amount??><@ofbizCurrency amount=(acctgTransEntry.amount)! isoCode=(acctgTransEntry.currencyUomId)!/></#if></fo:block>        
                                        </fo:table-cell>
                                        <fo:table-cell border="1pt solid" border-width=".1mm">
                                            <fo:block text-align="center" font-size="5pt">
                                                <#if (acctgTransEntry.acctgTransTypeId)??>
                                                    <#assign acctgTransType = (delegator.findOne("AcctgTransType", {"acctgTransTypeId" : (acctgTransEntry.acctgTransTypeId)!}, false))!/>
                                                    <#if acctgTransType?has_content>${acctgTransType.description}</#if>
                                                </#if>
                                            </fo:block>        
                                        </fo:table-cell>
                                        <fo:table-cell border="1pt solid" border-width=".1mm">
                                            <fo:block text-align="left" font-size="5pt">
                                                <#if (acctgTransEntry.glFiscalTypeId)??>
                                                    <#assign glFiscalType = (delegator.findOne("GlFiscalType", {"glFiscalTypeId" : (acctgTransEntry.glFiscalTypeId)!}, false))!/>
                                                    ${(glFiscalType.description)!}
                                                </#if>
                                            </fo:block>        
                                        </fo:table-cell>
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
