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
<div class="screenlet">
    <div class="screenlet-title-bar">
        <div class="boxlink">
            <#if "Y" == maySelectItems?default("N")>
                <a href="javascript:document.addCommonToCartForm.add_all.value='true';document.addCommonToCartForm.submit()" class="buttontext">${uiLabelMap.OrderAddAllToCart}</a>
            </#if>
        </div>
        <div class="h3">${uiLabelMap.OrderOrderQuoteItems}</div>
    </div>
    <div class="screenlet-body">
        <table cellspacing="0" class="basic-table">
            <tr valign="bottom" class="header-row">
                <td width="15%">${uiLabelMap.ProductItem}</td>
                <td width="20%">${uiLabelMap.ProductProduct}</td>
                <td width="10%" align="right">${uiLabelMap.ProductQuantity}</td>
                <td width="10%" align="right">${uiLabelMap.OrderSelAmount}</td>
                <td width="8%" align="right">${uiLabelMap.OrderOrderQuoteUnitPrice}</td>
                <td width="7%" align="right">${uiLabelMap.OrderOrderQuoteLeadTimeDays}</td>
                <td width="10%" align="right">${uiLabelMap.CommonComments}</td>
                <td width="10%" align="right">${uiLabelMap.OrderAdjustments}</td>
                <td width="10%" align="right">${uiLabelMap.CommonSubtotal}</td>
            </tr>
            <tr valign="bottom" class="header-row">
                <td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;${uiLabelMap.OrderOrderTermType}</td>
                <td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;${uiLabelMap.OrderOrderTermValue}</td>
                <td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;${uiLabelMap.OrderOrderTermDays}</td>
                <td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;${uiLabelMap.OrderQuoteTermDescription}</td>
                <td></td>
                <td></td>
                <td></td>
                <td></td>
                <td align="right">&nbsp;</td>
            </tr>
            <#assign totalQuoteAmount = 0.0>
            <#assign alt_row = false/>
            <#list quoteItems as quoteItem>
                <#assign selectedAmount = quoteItem.selectedAmount?default(1)>
                <#if selectedAmount == 0>
                    <#assign selectedAmount = 1/>
                </#if>
                <#assign quoteItemAmount = quoteItem.quoteUnitPrice?default(0) * quoteItem.quantity?default(0) * selectedAmount>
                <#assign quoteItemAdjustments = quoteItem.getRelated("QuoteAdjustment", null, null, false)>
                <#assign totalQuoteItemAdjustmentAmount = 0.0>
                <#list quoteItemAdjustments as quoteItemAdjustment>
                    <#assign totalQuoteItemAdjustmentAmount = quoteItemAdjustment.amount?default(0) + totalQuoteItemAdjustmentAmount>
                </#list>
                <#assign totalQuoteItemAmount = quoteItemAmount + totalQuoteItemAdjustmentAmount>
                <#assign totalQuoteAmount = totalQuoteAmount + totalQuoteItemAmount>
                
                <tr <#if alt_row>class="alternate-row" </#if>>
                    <td >
                        <div>
                        <#if showQuoteManagementLinks?? && "N" == quoteItem.isPromo?default("N") && "QUO_CREATED" == quote.statusId>
                            <a href="<@ofbizUrl>EditQuoteItem?quoteId=${quoteItem.quoteId}&amp;quoteItemSeqId=${quoteItem.quoteItemSeqId}</@ofbizUrl>" class="buttontext">${quoteItem.quoteItemSeqId}</a>
                        <#else>
                            ${quoteItem.quoteItemSeqId}
                        </#if>
                        </div>
                        <#assign quoteTerms = EntityQuery.use(delegator).from("QuoteTerm").where("quoteId", quoteItem.quoteId!, "quoteItemSeqId", quoteItem.quoteItemSeqId!).queryList()!>
                    </td>
                    <td valign="top">
                        <div>
                            <#if quoteItem.productId??>
                              <#assign product = quoteItem.getRelatedOne("Product", false)/>
                              ${(product.internalName)!}&nbsp;
                            </#if>
                            <#if showQuoteManagementLinks??>
                                <a href="/catalog/control/EditProduct?productId=${quoteItem.productId!}" class="buttontext">
                                  <#if quoteItem.productId??>
                                    ${quoteItem.productId}
                                  <#else>
                                    ${uiLabelMap.ProductCreateProduct}
                                  </#if>
                                </a>
                            <#else>
                                <a href="<@ofbizUrl>product?product_id=${quoteItem.productId!}</@ofbizUrl>" class="buttontext">${quoteItem.productId!}</a>
                            </#if>
                        </div>
                    </td>
                    <td align="right" valign="top">${quoteItem.quantity!}</td>
                    <td align="right" valign="top">${quoteItem.selectedAmount!}</td>
                    <td align="right" valign="top"><@ofbizCurrency amount=quoteItem.quoteUnitPrice isoCode=quote.currencyUomId/></td>
                    <td align="right" valign="top">${quoteItem.leadTimeDays!}</td>
                    <td align="right" valign="top">${quoteItem.comments!}</td>
                    <td align="right" valign="top"><@ofbizCurrency amount=totalQuoteItemAdjustmentAmount isoCode=quote.currencyUomId/></td>
                    <td align="right" valign="top"><@ofbizCurrency amount=totalQuoteItemAmount isoCode=quote.currencyUomId/></td>
                </tr>
                <#list quoteTerms as quoteTerm>
                <#assign termDescription = delegator.findOne("TermType",{"termTypeId":quoteTerm.termTypeId}, false)>
                <tr <#if alt_row>class="alternate-row" </#if>>
                    <td valign="top">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;${termDescription.description!}</td>
                    <td valign="top">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;${quoteTerm.termValue!}</td>
                    <td valign="top"><#if quoteTerm.termDays??>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;${quoteTerm.termDays!}</#if></td>
                    <td valign="top"><#if quoteTerm.description??>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;${quoteTerm.description}</#if></td>
                    <td align="right" valign="top"></td>
                    <td align="right" valign="top"></td>
                    <td align="right" valign="top"></td>
                    <td align="right" valign="top"></td>
                    <td align="right" valign="top"></td>
                </tr>
                </#list>
                <#-- now show adjustment details per line item -->
                <#list quoteItemAdjustments as quoteItemAdjustment>
                    <#assign adjustmentType = quoteItemAdjustment.getRelatedOne("OrderAdjustmentType", false)>
                    <tr <#if alt_row>class="alternate-row" </#if>>
                        <td align="right" colspan="6"><span class="label">${adjustmentType.get("description",locale)!}</span></td>
                        <td align="right"><@ofbizCurrency amount=quoteItemAdjustment.amount isoCode=quote.currencyUomId/></td>
                        <td>&nbsp;</td>
                    </tr>
                </#list>
                <#-- toggle the row color -->
                <#assign alt_row = !alt_row>
            </#list>
            <tr><td colspan="10"><hr /></td></tr>
            <tr>
                <td align="right" colspan="8" class="label">${uiLabelMap.CommonSubtotal}</td>
                <td align="right"><@ofbizCurrency amount=totalQuoteAmount isoCode=quote.currencyUomId/></td>
            </tr>
            <tr><td colspan="6"></td><td colspan="7"><hr /></td></tr>
            <#assign totalQuoteHeaderAdjustmentAmount = 0.0>
            <#assign findAdjustment = false>
            <#list quoteAdjustments as quoteAdjustment>
                <#assign adjustmentType = quoteAdjustment.getRelatedOne("OrderAdjustmentType", false)>
                <#if !quoteAdjustment.quoteItemSeqId??>
                    <#assign totalQuoteHeaderAdjustmentAmount = quoteAdjustment.amount?default(0) + totalQuoteHeaderAdjustmentAmount>
                    <tr>
                      <td align="right" colspan="7"><span class="label">${adjustmentType.get("description",locale)!}</span></td>
                      <td align="right"><@ofbizCurrency amount=quoteAdjustment.amount isoCode=quote.currencyUomId/></td>
                    </tr>
                </#if>
                <#assign findAdjustment = true>
            </#list>
            <#assign grandTotalQuoteAmount = totalQuoteAmount + totalQuoteHeaderAdjustmentAmount>
            <#if findAdjustment>
            <tr><td colspan="5"></td><td colspan="7"><hr /></td></tr>
            </#if>
            <tr>
                <td align="right" colspan="8" class="label">${uiLabelMap.OrderGrandTotal}</td>
                <td align="right">
                    <@ofbizCurrency amount=grandTotalQuoteAmount isoCode=quote.currencyUomId/>
                </td>
            </tr>
        </table>
    </div>
</div>
