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
    <#if orderHeader?has_content>
        <fo:table table-layout="fixed" border-spacing="3pt">
            <fo:table-column column-width="3in"/>
            <fo:table-column column-width="1in"/>
            <fo:table-column column-width="1in"/>
            <fo:table-column column-width="1in"/>
            <fo:table-column column-width="1in"/>
            <fo:table-header>
                <fo:table-row>
                    <fo:table-cell>
                        <fo:block font-weight="bold">${uiLabelMap.OrderProduct}</fo:block>
                    </fo:table-cell>
                    <fo:table-cell><fo:block></fo:block></fo:table-cell>
                    <fo:table-cell text-align="right">
                        <fo:block font-weight="bold">${uiLabelMap.OrderQuantity}</fo:block>
                    </fo:table-cell>
                    <fo:table-cell text-align="right">
                        <fo:block font-weight="bold">${uiLabelMap.OrderUnitList}</fo:block>
                    </fo:table-cell>
                    <fo:table-cell text-align="right">
                        <fo:block font-weight="bold">${uiLabelMap.OrderSubTotal}</fo:block>
                    </fo:table-cell>
                </fo:table-row>
            </fo:table-header>
            <fo:table-body>
                <#list orderItemList as orderItem>
                    <#assign orderItemType = orderItem.getRelatedOne("OrderItemType", false)!>
                    <#assign productId = orderItem.productId!>
                    <#assign remainingQuantity = (orderItem.quantity?default(0) - orderItem.cancelQuantity?default(0))>
                    <#assign itemAdjustment = Static["org.apache.ofbiz.order.order.OrderReadHelper"].getOrderItemAdjustmentsTotal(orderItem, orderAdjustments, true, false, false)>
                    <#assign internalImageUrl = Static["org.apache.ofbiz.product.imagemanagement.ImageManagementHelper"].getInternalImageUrl(request, productId!)!>
                    <fo:table-row>
                        <fo:table-cell>
                            <fo:block>
                                <#if orderItem.supplierProductId?has_content>
                                    ${orderItem.supplierProductId} - ${orderItem.itemDescription!}
                                <#elseif productId??>
                                    ${orderItem.productId?default("N/A")} - ${orderItem.itemDescription!}
                                <#elseif orderItemType??>
                                    ${orderItemType.get("description",locale)} - ${orderItem.itemDescription!}
                                <#else>
                                    ${orderItem.itemDescription!}
                                </#if>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block>
                                <#if orderHeader.orderTypeId == "PURCHASE_ORDER">
                                    <#if internalImageUrl?has_content>
                                        <fo:external-graphic src="${internalImageUrl}" overflow="hidden" content-width="100"/>
                                    </#if>
                                </#if>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell text-align="right">
                            <fo:block>${remainingQuantity}</fo:block>
                        </fo:table-cell>
                        <fo:table-cell text-align="right">
                            <fo:block><@ofbizCurrency amount=orderItem.unitPrice isoCode=currencyUomId/></fo:block>
                        </fo:table-cell>
                        <fo:table-cell text-align="right">
                            <fo:block>
                                <#if orderItem.statusId != "ITEM_CANCELLED">
                                    <@ofbizCurrency amount=Static["org.apache.ofbiz.order.order.OrderReadHelper"].getOrderItemSubTotal(orderItem, orderAdjustments) isoCode=currencyUomId/>
                                <#else>
                                    <@ofbizCurrency amount=0.00 isoCode=currencyUomId/>
                                </#if>
                            </fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                    <#if itemAdjustment != 0>
                        <fo:table-row>
                            <fo:table-cell number-columns-spanned="2">
                                <fo:block text-indent="0.2in">
                                    <fo:inline font-style="italic">${uiLabelMap.OrderAdjustments}</fo:inline>
                                    : <@ofbizCurrency amount=itemAdjustment isoCode=currencyUomId/>
                                </fo:block>
                            </fo:table-cell>
                        </fo:table-row>
                    </#if>
                </#list>
                <#list orderHeaderAdjustments as orderHeaderAdjustment>
                    <#assign adjustmentType = orderHeaderAdjustment.getRelatedOne("OrderAdjustmentType", false)>
                    <#assign adjustmentAmount = Static["org.apache.ofbiz.order.order.OrderReadHelper"].calcOrderAdjustment(orderHeaderAdjustment, orderSubTotal)>
                    <#if adjustmentAmount != 0>
                        <fo:table-row>
                            <fo:table-cell><fo:block></fo:block></fo:table-cell>
                            <fo:table-cell><fo:block></fo:block></fo:table-cell>
                            <fo:table-cell number-columns-spanned="2">
                                <fo:block font-weight="bold">
                                    ${adjustmentType.get("description",locale)} :
                                    <#if orderHeaderAdjustment.get("description")?has_content>
                                        (${orderHeaderAdjustment.get("description")!})
                                    </#if>
                                </fo:block>
                            </fo:table-cell>
                            <fo:table-cell text-align="right">
                                <fo:block><@ofbizCurrency amount=adjustmentAmount isoCode=currencyUomId/></fo:block>
                            </fo:table-cell>
                        </fo:table-row>
                    </#if>
                </#list>
                <#-- summary of order amounts -->
                <fo:table-row>
                    <fo:table-cell><fo:block></fo:block></fo:table-cell>
                    <fo:table-cell><fo:block></fo:block></fo:table-cell>
                    <fo:table-cell number-columns-spanned="2">
                        <fo:block font-weight="bold">${uiLabelMap.OrderItemsSubTotal}</fo:block>
                    </fo:table-cell>
                    <fo:table-cell text-align="right">
                        <fo:block><@ofbizCurrency amount=orderSubTotal isoCode=currencyUomId/></fo:block>
                    </fo:table-cell>
                </fo:table-row>
                <#if otherAdjAmount != 0>
                    <fo:table-row>
                        <fo:table-cell><fo:block></fo:block></fo:table-cell>
                        <fo:table-cell><fo:block></fo:block></fo:table-cell>
                        <fo:table-cell number-columns-spanned="2">
                            <fo:block font-weight="bold">${uiLabelMap.OrderTotalOtherOrderAdjustments}</fo:block>
                        </fo:table-cell>
                        <fo:table-cell text-align="right">
                            <fo:block><@ofbizCurrency amount=otherAdjAmount isoCode=currencyUomId/></fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                </#if>
                <#if shippingAmount != 0>
                    <fo:table-row>
                        <fo:table-cell><fo:block></fo:block></fo:table-cell>
                        <fo:table-cell><fo:block></fo:block></fo:table-cell>
                        <fo:table-cell number-columns-spanned="2">
                            <fo:block font-weight="bold">${uiLabelMap.OrderTotalShippingAndHandling}</fo:block>
                        </fo:table-cell>
                        <fo:table-cell text-align="right">
                            <fo:block><@ofbizCurrency amount=shippingAmount isoCode=currencyUomId/></fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                </#if>
                <#if taxAmount != 0>
                    <fo:table-row>
                        <fo:table-cell><fo:block></fo:block></fo:table-cell>
                        <fo:table-cell><fo:block></fo:block></fo:table-cell>
                        <fo:table-cell number-columns-spanned="2">
                            <fo:block font-weight="bold">${uiLabelMap.OrderTotalSalesTax}</fo:block>
                        </fo:table-cell>
                        <fo:table-cell text-align="right">
                            <fo:block><@ofbizCurrency amount=taxAmount isoCode=currencyUomId/></fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                </#if>
                <#if grandTotal != 0>
                    <fo:table-row>
                        <fo:table-cell><fo:block></fo:block></fo:table-cell>
                        <fo:table-cell><fo:block></fo:block></fo:table-cell>
                        <fo:table-cell number-columns-spanned="2" background-color="#EEEEEE">
                            <fo:block font-weight="bold">${uiLabelMap.OrderTotalDue}</fo:block>
                        </fo:table-cell>
                        <fo:table-cell text-align="right">
                            <fo:block><@ofbizCurrency amount=grandTotal isoCode=currencyUomId/></fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                </#if>
                <#-- notes -->
                <#if orderNotes?has_content>
                    <#if showNoteHeadingOnPDF>
                        <fo:table-row>
                            <fo:table-cell number-columns-spanned="3">
                                <fo:block font-weight="bold">${uiLabelMap.OrderNotes}</fo:block>
                                <fo:block>
                                    <fo:leader leader-length="19cm" leader-pattern="rule"/>
                                </fo:block>
                            </fo:table-cell>
                        </fo:table-row>
                    </#if>
                    <#list orderNotes as note>
                        <#if (note.internalNote?has_content) && (note.internalNote != "Y")>
                            <fo:table-row>
                                <fo:table-cell number-columns-spanned="1">
                                    <fo:block>${note.noteInfo!}</fo:block>
                                </fo:table-cell>
                                <fo:table-cell number-columns-spanned="2">
                                    <fo:block>
                                    <#if note.noteParty?has_content>
                                        <#assign notePartyNameResult = dispatcher.runSync("getPartyNameForDate", Static["org.apache.ofbiz.base.util.UtilMisc"].toMap("partyId", note.noteParty, "compareDate", note.noteDateTime, "lastNameFirst", "Y", "userLogin", userLogin))/>
                                        ${uiLabelMap.CommonBy}: ${notePartyNameResult.fullName?default("${uiLabelMap.OrderPartyNameNotFound}")}
                                    </#if>
                                    </fo:block>
                                </fo:table-cell>
                                <fo:table-cell number-columns-spanned="1">
                                    <fo:block>${uiLabelMap.CommonAt}: ${note.noteDateTime?string!}</fo:block>
                                </fo:table-cell>
                            </fo:table-row>
                        </#if>
                    </#list>
                </#if>
            </fo:table-body>
        </fo:table>
    </#if>
</#escape>
