<#--
$Id: $

Copyright 2001-2006 The Apache Software Foundation

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.
-->
<?xml version="1.0" encoding="UTF-8" ?>
<fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">

<fo:layout-master-set>
    <fo:simple-page-master master-name="main" 
            margin-top="0.5in" margin-bottom="1in" margin-left="1in" margin-right="1in">
        <fo:region-body margin-top="3.5in"/>
        <fo:region-before extent="3.5in"/>
        <fo:region-after extent="1in"/>
    </fo:simple-page-master>
</fo:layout-master-set>

<#if quote?exists>
<fo:page-sequence master-reference="main">

    <fo:static-content flow-name="xsl-region-before" font-size="9pt">
        <#-- a nest of tables to put company information on left and invoice information on the right -->
        <fo:table>
            <fo:table-column column-width="3.5in"/>
            <fo:table-column column-width="3in"/>
            <fo:table-body>
                <fo:table-row>
                    <#-- Top Left cell -->
                    <fo:table-cell>
                        ${screens.render("component://order/widget/ordermgr/OrderPrintForms.xml#CompanyLogo")}
                    </fo:table-cell>
                    <#-- Top Right cell -->
                    <fo:table-cell>
                    </fo:table-cell>
                </fo:table-row>
            </fo:table-body>
        </fo:table>

        <#-- Inserts a newline.  white-space-collapse="false" specifies that the stuff inside fo:block is to repeated verbatim -->
        <fo:block white-space-collapse="false"> </fo:block> 

        <fo:table border-spacing="3pt">
            <fo:table-column column-width="3.75in"/>
            <fo:table-column column-width="3.75in"/>
            <fo:table-body>
                <fo:table-row>
                    <fo:table-cell>
                        <fo:block>
                            <#if quote.partyId?exists>
                                <#assign quotePartyNameResult = dispatcher.runSync("getPartyNameForDate", Static["org.ofbiz.base.util.UtilMisc"].toMap("partyId", quote.partyId, "compareDate", quote.issueDate, "userLogin", userLogin))/>
                                <fo:block font-weight="bold">${quotePartyNameResult.fullName?default("[${uiLabelMap.OrderPartyNameNotFound}]")}</fo:block>
                            <#else>
                                <fo:block font-weight="bold">[${uiLabelMap.OrderPartyNameNotFound}]</fo:block>
                            </#if>
                        </fo:block>
                    </fo:table-cell>
                </fo:table-row>
                <fo:table-row>
                    <fo:table-cell>
                        <fo:block>
                            <fo:block font-weight="bold">${uiLabelMap.OrderAddress}: </fo:block>
                            <#if toPostalAddress?exists>
                            <fo:block font-weight="bold">${toPostalAddress.address1?if_exists}</fo:block>
                            <fo:block font-weight="bold">${toPostalAddress.address2?if_exists}</fo:block>
                            <fo:block font-weight="bold">${toPostalAddress.city}<#if toPostalAddress.stateProvinceGeoId?has_content>, ${toPostalAddress.stateProvinceGeoId}</#if> ${toPostalAddress.postalCode?if_exists}</fo:block>
                            </#if>
                        </fo:block>
                    </fo:table-cell>
                </fo:table-row>
            </fo:table-body>
        </fo:table>

        <fo:block white-space-collapse="false"> </fo:block> 

        <fo:table border-spacing="3pt">
            <fo:table-column column-width="1.5in"/>
            <fo:table-column column-width="3.75in"/>
            <fo:table-body>
                <fo:table-row>
                    <fo:table-cell><fo:block font-weight="bold">${uiLabelMap.CommonType}</fo:block></fo:table-cell>
                    <fo:table-cell><fo:block>${(quoteType.get("description",locale))?default(quote.quoteTypeId?if_exists)}</fo:block></fo:table-cell>
                </fo:table-row>
                <fo:table-row>
                    <fo:table-cell><fo:block font-weight="bold">${uiLabelMap.OrderOrderQuoteId}</fo:block></fo:table-cell>
                    <fo:table-cell><fo:block>${quote.quoteId}</fo:block></fo:table-cell>
                </fo:table-row>
                <fo:table-row>
                    <fo:table-cell><fo:block font-weight="bold">${uiLabelMap.CommonStatus}</fo:block></fo:table-cell>
                    <fo:table-cell><fo:block>${(statusItem.get("description", locale))?default(quote.statusId?if_exists)}</fo:block></fo:table-cell>
                </fo:table-row>
                <fo:table-row>
                    <fo:table-cell><fo:block font-weight="bold">${uiLabelMap.OrderOrderQuoteName}</fo:block></fo:table-cell>
                    <fo:table-cell><fo:block>${quote.quoteName?if_exists}</fo:block></fo:table-cell>
                </fo:table-row>
                <fo:table-row>
                    <fo:table-cell><fo:block font-weight="bold">${uiLabelMap.CommonDescription}</fo:block></fo:table-cell>
                    <fo:table-cell><fo:block>${quote.description?if_exists}</fo:block></fo:table-cell>
                </fo:table-row>
                <fo:table-row>
                    <fo:table-cell><fo:block font-weight="bold">${uiLabelMap.CommonCurrency}</fo:block></fo:table-cell>
                    <fo:table-cell><fo:block><#if currency?exists>${currency.get("description",locale)?default(quote.currencyUomId?if_exists)}</#if></fo:block></fo:table-cell>
                </fo:table-row>
                <fo:table-row>
                    <fo:table-cell><fo:block font-weight="bold">${uiLabelMap.OrderOrderQuoteIssueDate}</fo:block></fo:table-cell>
                    <fo:table-cell><fo:block>${(quote.issueDate.toString())?if_exists}</fo:block></fo:table-cell>
                </fo:table-row>
                <fo:table-row>
                    <fo:table-cell><fo:block font-weight="bold">${uiLabelMap.CommonValidFromDate}</fo:block></fo:table-cell>
                    <fo:table-cell><fo:block>${(quote.validFromDate.toString())?if_exists}</fo:block></fo:table-cell>
                </fo:table-row>
                <fo:table-row>
                    <fo:table-cell><fo:block font-weight="bold">${uiLabelMap.CommonValidThruDate}</fo:block></fo:table-cell>
                    <fo:table-cell><fo:block>${(quote.validThruDate.toString())?if_exists}</fo:block></fo:table-cell>
                </fo:table-row>
            </fo:table-body>
        </fo:table>
        
        
    </fo:static-content>

    <#-- Footer -->
    <fo:static-content flow-name="xsl-region-after">
        <#-- displays page number.  "theEnd" is an id of a fo:block at the very end -->
        <fo:block font-size="10pt" text-align="center">${uiLabelMap.CommonPage} <fo:page-number/> ${uiLabelMap.CommonOf} <fo:page-number-citation ref-id="theEnd"/></fo:block>
    </fo:static-content>

    <fo:flow flow-name="xsl-region-body" font-family="Helvetica">
        <fo:block space-after.optimum="10pt" font-size="8pt">
            <fo:table>
                <fo:table-column column-width="40pt"/>
                <fo:table-column column-width="200pt"/>
                <fo:table-column column-width="50pt"/>
                <fo:table-column column-width="50pt"/>
                <fo:table-column column-width="50pt"/>
                <fo:table-column column-width="50pt"/>
                <fo:table-column column-width="50pt"/>
                <fo:table-header>
                    <fo:table-row font-weight="bold">
                        <fo:table-cell border-bottom="thin solid grey"><fo:block>${uiLabelMap.ProductItem}</fo:block></fo:table-cell>
                        <fo:table-cell border-bottom="thin solid grey"><fo:block>${uiLabelMap.ProductProduct}</fo:block></fo:table-cell>
                        <fo:table-cell border-bottom="thin solid grey"><fo:block text-align="right">${uiLabelMap.ProductQuantity}</fo:block></fo:table-cell>
                        <fo:table-cell border-bottom="thin solid grey"><fo:block text-align="right">${uiLabelMap.OrderAmount}</fo:block></fo:table-cell>
                        <fo:table-cell border-bottom="thin solid grey"><fo:block text-align="right">${uiLabelMap.OrderOrderQuoteUnitPrice}</fo:block></fo:table-cell>
                        <fo:table-cell border-bottom="thin solid grey"><fo:block text-align="right">${uiLabelMap.OrderAdjustments}</fo:block></fo:table-cell>
                        <fo:table-cell border-bottom="thin solid grey"><fo:block text-align="right">${uiLabelMap.CommonSubtotal}</fo:block></fo:table-cell>
                    </fo:table-row>
                </fo:table-header>
                <fo:table-body>
                    <#assign rowColor = "white">
                    <#assign totalQuoteAmount = 0.0>
                    <#list quoteItems as quoteItem>
                        <#if quoteItem.productId?exists>
                            <#assign product = quoteItem.getRelatedOne("Product")>
                        </#if>
                        <#assign quoteItemAmount = quoteItem.quoteUnitPrice?default(0) * quoteItem.quantity?default(0)>
                        <#assign quoteItemAdjustments = quoteItem.getRelated("QuoteAdjustment")>
                        <#assign totalQuoteItemAdjustmentAmount = 0.0>
                        <#list quoteItemAdjustments as quoteItemAdjustment>
                            <#assign totalQuoteItemAdjustmentAmount = quoteItemAdjustment.amount?default(0) + totalQuoteItemAdjustmentAmount>
                        </#list>
                        <#assign totalQuoteItemAmount = quoteItemAmount + totalQuoteItemAdjustmentAmount>
                        <#assign totalQuoteAmount = totalQuoteAmount + totalQuoteItemAmount>

                        <fo:table-row>
                            <fo:table-cell padding="2pt" background-color="${rowColor}">
                                <fo:block>${quoteItem.quoteItemSeqId}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell padding="2pt" background-color="${rowColor}">
                                <fo:block>${(product.internalName)?xml?if_exists} [${quoteItem.productId?if_exists}]</fo:block>
                            </fo:table-cell>
                            <fo:table-cell padding="2pt" background-color="${rowColor}">
                                <fo:block text-align="right">${quoteItem.quantity?if_exists}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell padding="2pt" background-color="${rowColor}">
                                <fo:block text-align="right">${quoteItem.selectedAmount?if_exists}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell padding="2pt" background-color="${rowColor}">
                                <fo:block text-align="right"><@ofbizCurrency amount=quoteItem.quoteUnitPrice isoCode=quote.currencyUomId/></fo:block>
                            </fo:table-cell>
                            <fo:table-cell padding="2pt" background-color="${rowColor}">
                                <fo:block text-align="right"><@ofbizCurrency amount=totalQuoteItemAdjustmentAmount isoCode=quote.currencyUomId/></fo:block>
                            </fo:table-cell>
                            <fo:table-cell padding="2pt" background-color="${rowColor}">
                                <fo:block text-align="right"><@ofbizCurrency amount=totalQuoteItemAmount isoCode=quote.currencyUomId/></fo:block>
                            </fo:table-cell>

                        </fo:table-row>
                        <#list quoteItemAdjustments as quoteItemAdjustment>
                            <#assign adjustmentType = quoteItemAdjustment.getRelatedOne("OrderAdjustmentType")>
                            <fo:table-row>
                                <fo:table-cell padding="2pt" background-color="${rowColor}">
                                </fo:table-cell>
                                <fo:table-cell padding="2pt" background-color="${rowColor}">
                                </fo:table-cell>
                                <fo:table-cell padding="2pt" background-color="${rowColor}">
                                </fo:table-cell>
                                <fo:table-cell padding="2pt" background-color="${rowColor}">
                                </fo:table-cell>
                                <fo:table-cell padding="2pt" background-color="${rowColor}">
                                    <fo:block font-size="7pt" text-align="right">${adjustmentType.get("description",locale)?if_exists}</fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="2pt" background-color="${rowColor}">
                                    <fo:block font-size="7pt" text-align="right"><@ofbizCurrency amount=quoteItemAdjustment.amount isoCode=quote.currencyUomId/></fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="2pt" background-color="${rowColor}">
                                </fo:table-cell>
                            </fo:table-row>
                        </#list>

                        <#if rowColor == "white">
                            <#assign rowColor = "#D4D0C8">
                        <#else>
                            <#assign rowColor = "white">
                        </#if>        
                    </#list>          
                </fo:table-body>
            </fo:table>
            
            
            
            
            <fo:block space-after.optimum="10pt" font-size="8pt">
                <fo:table>
                    <fo:table-column column-width="390pt"/>
                    <fo:table-column column-width="50pt"/>
                    <fo:table-column column-width="50pt"/>
                    <fo:table-body>
                        <fo:table-row>
                            <fo:table-cell></fo:table-cell>
                            <fo:table-cell padding="2pt">
                                <fo:block font-weight="bold" text-align="right">${uiLabelMap.CommonSubtotal}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell padding="2pt">
                                <fo:block font-weight="bold" text-align="right"><@ofbizCurrency amount=totalQuoteAmount isoCode=quote.currencyUomId/></fo:block>
                            </fo:table-cell>
                        </fo:table-row>
                        <#assign totalQuoteHeaderAdjustmentAmount = 0.0>
                        <#list quoteAdjustments as quoteAdjustment>
                            <#assign adjustmentType = quoteAdjustment.getRelatedOne("OrderAdjustmentType")>
                            <#if !quoteAdjustment.quoteItemSeqId?exists>
                                <#assign totalQuoteHeaderAdjustmentAmount = quoteAdjustment.amount?default(0) + totalQuoteHeaderAdjustmentAmount>
                                <fo:table-row>
                                    <fo:table-cell></fo:table-cell>
                                    <fo:table-cell padding="2pt">
                                        <fo:block font-weight="bold" text-align="right">${adjustmentType.get("description", locale)?if_exists}</fo:block>
                                    </fo:table-cell>
                                    <fo:table-cell padding="2pt">
                                        <fo:block font-weight="bold" text-align="right"><@ofbizCurrency amount=quoteAdjustment.amount isoCode=quote.currencyUomId/></fo:block>
                                    </fo:table-cell>
                                </fo:table-row>
                            </#if>
                        </#list>
                        <#assign grandTotalQuoteAmount = totalQuoteAmount + totalQuoteHeaderAdjustmentAmount>
                        <fo:table-row>
                            <fo:table-cell></fo:table-cell>
                            <fo:table-cell padding="2pt">
                                <fo:block font-weight="bold" text-align="right">${uiLabelMap.OrderGrandTotal}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell padding="2pt">
                                <fo:block font-weight="bold" text-align="right"><@ofbizCurrency amount=grandTotalQuoteAmount isoCode=quote.currencyUomId/></fo:block>
                            </fo:table-cell>
                        </fo:table-row>
                    </fo:table-body>
                </fo:table>
            </fo:block>
        </fo:block>
        <#-- marks the end of the pages and used to identify page-number at the end -->
        <fo:block id="theEnd"/>
    </fo:flow>
</fo:page-sequence>
<#else>
<fo:page-sequence master-reference="main">
    <fo:flow flow-name="xsl-region-body" font-family="Helvetica">
        <fo:block font-size="14pt">${uiLabelMap.OrderNoQuoteFound}</fo:block>
    </fo:flow>
</fo:page-sequence>
</#if>
</fo:root>

