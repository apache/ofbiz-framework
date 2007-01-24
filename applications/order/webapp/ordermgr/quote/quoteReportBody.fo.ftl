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
        <fo:block>
            <fo:table font-size="9pt">
                <fo:table-column column-width="40pt"/>
                <fo:table-column column-width="160pt"/>
                <fo:table-column column-width="58pt"/>
                <fo:table-column column-width="58pt"/>
                <fo:table-column column-width="58pt"/>
                <fo:table-column column-width="58pt"/>
                <fo:table-column column-width="58pt"/>
                <fo:table-header>
                    <fo:table-row>
                        <fo:table-cell border-bottom="thin solid grey"><fo:block font-weight="bold">${uiLabelMap.ProductItem}</fo:block></fo:table-cell>
                        <fo:table-cell border-bottom="thin solid grey"><fo:block font-weight="bold">${uiLabelMap.ProductProduct}</fo:block></fo:table-cell>
                        <fo:table-cell border-bottom="thin solid grey"><fo:block font-weight="bold" text-align="right">${uiLabelMap.ProductQuantity}</fo:block></fo:table-cell>
                        <fo:table-cell border-bottom="thin solid grey"><fo:block font-weight="bold" text-align="right">${uiLabelMap.OrderAmount}</fo:block></fo:table-cell>
                        <fo:table-cell border-bottom="thin solid grey"><fo:block font-weight="bold" text-align="right">${uiLabelMap.OrderOrderQuoteUnitPrice}</fo:block></fo:table-cell>
                        <fo:table-cell border-bottom="thin solid grey"><fo:block font-weight="bold" text-align="right">${uiLabelMap.OrderAdjustments}</fo:block></fo:table-cell>
                        <fo:table-cell border-bottom="thin solid grey"><fo:block font-weight="bold" text-align="right">${uiLabelMap.CommonSubtotal}</fo:block></fo:table-cell>
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
                                <fo:block>${(product.internalName)?if_exists} [${quoteItem.productId?if_exists}]</fo:block>
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
            
            
            
            
            <fo:block text-align="right">
                <fo:table>
                    <fo:table-column column-width="100pt"/>
                    <fo:table-column column-width="100pt"/>
                    <fo:table-body>
                        <fo:table-row>
                            <fo:table-cell padding="2pt">
                                <fo:block font-weight="bold" text-align="right">${uiLabelMap.CommonSubtotal}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell padding="2pt">
                                <fo:block text-align="right"><@ofbizCurrency amount=totalQuoteAmount isoCode=quote.currencyUomId/></fo:block>
                            </fo:table-cell>
                        </fo:table-row>
                        <#assign totalQuoteHeaderAdjustmentAmount = 0.0>
                        <#list quoteAdjustments as quoteAdjustment>
                            <#assign adjustmentType = quoteAdjustment.getRelatedOne("OrderAdjustmentType")>
                            <#if !quoteAdjustment.quoteItemSeqId?exists>
                                <#assign totalQuoteHeaderAdjustmentAmount = quoteAdjustment.amount?default(0) + totalQuoteHeaderAdjustmentAmount>
                                <fo:table-row>
                                    <fo:table-cell padding="2pt">
                                        <fo:block font-weight="bold" text-align="right">${adjustmentType.get("description", locale)?if_exists}</fo:block>
                                    </fo:table-cell>
                                    <fo:table-cell padding="2pt">
                                        <fo:block text-align="right"><@ofbizCurrency amount=quoteAdjustment.amount isoCode=quote.currencyUomId/></fo:block>
                                    </fo:table-cell>
                                </fo:table-row>
                            </#if>
                        </#list>
                        <#assign grandTotalQuoteAmount = totalQuoteAmount + totalQuoteHeaderAdjustmentAmount>
                        <fo:table-row>
                            <fo:table-cell padding="2pt">
                                <fo:block font-weight="bold" text-align="right">${uiLabelMap.OrderGrandTotal}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell padding="2pt">
                                <fo:block text-align="right"><@ofbizCurrency amount=grandTotalQuoteAmount isoCode=quote.currencyUomId/></fo:block>
                            </fo:table-cell>
                        </fo:table-row>
                    </fo:table-body>
                </fo:table>
            </fo:block>
        </fo:block>
</#escape>
