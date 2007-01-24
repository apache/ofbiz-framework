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
    <div class="screenlet-header">
        <div class="boxlink">
            <#if maySelectItems?default("N") == "Y">
                <a href="javascript:document.addCommonToCartForm.add_all.value='true';document.addCommonToCartForm.submit()" class="buttontext">${uiLabelMap.EcommerceAddAlltoCart}</a>
            </#if>
        </div>
        <div class="boxhead">&nbsp; ${uiLabelMap.OrderOrderQuoteItems}</div>
    </div>
    <div class="screenlet-body">
        <table width="100%" border="0" cellpadding="0">
            <tr align="left" valign="bottom">
                <td width="10%" align="left"><span class="tableheadtext"><b>${uiLabelMap.ProductItem}</b></span></td>
                <td width="35%" align="left"><span class="tableheadtext"><b>${uiLabelMap.EcommerceProduct}</b></span></td>
                <td width="10%" align="right"><span class="tableheadtext"><b>${uiLabelMap.ProductQuantity}</b></span></td>
                <td width="10%" align="right"><span class="tableheadtext"><b>${uiLabelMap.OrderAmount}</b></span></td>
                <td width="10%" align="right"><span class="tableheadtext"><b>${uiLabelMap.OrderOrderQuoteUnitPrice}</b></span></td>
                <td width="10%" align="right"><span class="tableheadtext"><b>${uiLabelMap.OrderAdjustments}</b></span></td>
                <td width="10%" align="right"><span class="tableheadtext"><b>${uiLabelMap.CommonSubtotal}</b></span></td>
                <td width="5%" align="right">&nbsp;</td>
            </tr>
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

                <tr><td colspan="10"><hr class="sepbar"/></td></tr>
                <tr>
                    <td valign="top">
                        <div class="tabletext" style="font-size: xx-small;">
                        <#if showQuoteManagementLinks?exists && quoteItem.isPromo?default("N") == "N">
                            <a href="<@ofbizUrl>EditQuoteItem?quoteId=${quoteItem.quoteId}&amp;quoteItemSeqId=${quoteItem.quoteItemSeqId}</@ofbizUrl>" class="buttontext">${quoteItem.quoteItemSeqId}</a>
                        <#else>
                            ${quoteItem.quoteItemSeqId}
                        </#if>
                        </div>
                    </td>
                    <td valign="top">
                        <div class="tabletext">
                            ${(product.internalName)?if_exists}&nbsp;
                            <#if showQuoteManagementLinks?exists>
                                <a href="/catalog/control/EditProduct?productId=${quoteItem.productId?if_exists}" class="buttontext">${quoteItem.productId?if_exists}</a>
                            <#else>
                                <a href="<@ofbizUrl>product?product_id=${quoteItem.productId?if_exists}</@ofbizUrl>" class="buttontext">${quoteItem.productId?if_exists}</a>
                            </#if>
                        </div>
                    </td>
                    <td align="right" valign="top"><div class="tabletext">${quoteItem.quantity?if_exists}</div></td>
                    <td align="right" valign="top"><div class="tabletext">${quoteItem.selectedAmount?if_exists}</div></td>
                    <td align="right" valign="top"><div class="tabletext"><@ofbizCurrency amount=quoteItem.quoteUnitPrice isoCode=quote.currencyUomId/></div></td>
                    <td align="right" valign="top"><div class="tabletext"><@ofbizCurrency amount=totalQuoteItemAdjustmentAmount isoCode=quote.currencyUomId/></div></td>                    
                    <td align="right" valign="top"><div class="tabletext"><@ofbizCurrency amount=totalQuoteItemAmount isoCode=quote.currencyUomId/></div></td>                    
                </tr>
                <#-- now show adjustment details per line item -->
                <#list quoteItemAdjustments as quoteItemAdjustment>
                    <#assign adjustmentType = quoteItemAdjustment.getRelatedOne("OrderAdjustmentType")>
                    <tr>
                        <td align="right" colspan="5"><div class="tabletext" style="font-size: xx-small;"><b>${adjustmentType.get("description",locale)?if_exists}</b></div></td>
                        <td align="right">
                          <div class="tabletext" style="font-size: xx-small;"><@ofbizCurrency amount=quoteItemAdjustment.amount isoCode=quote.currencyUomId/></div>
                        </td>
                        <td>&nbsp;</td>
                    </tr>
                </#list>
            </#list>

            <tr><td colspan="10"><hr class="sepbar"/></td></tr>
            <tr>
                <td align="right" colspan="6"><div class="tabletext"><b>${uiLabelMap.CommonSubtotal}</b></div></td>
                <td align="right"><div class="tabletext"><@ofbizCurrency amount=totalQuoteAmount isoCode=quote.currencyUomId/></div></td>
            </tr>
            <tr><td colspan="5"></td><td colspan="6"><hr class="sepbar"/></td></tr>
            <#assign totalQuoteHeaderAdjustmentAmount = 0.0>
            <#list quoteAdjustments as quoteAdjustment>
                <#assign adjustmentType = quoteAdjustment.getRelatedOne("OrderAdjustmentType")>
                <#if !quoteAdjustment.quoteItemSeqId?exists>
                    <#assign totalQuoteHeaderAdjustmentAmount = quoteAdjustment.amount?default(0) + totalQuoteHeaderAdjustmentAmount>
                    <tr>
                      <td align="right" colspan="6"><div class="tabletext"><b>${adjustmentType.get("description",locale)?if_exists}</b></div></td>
                      <td align="right"><div class="tabletext"><@ofbizCurrency amount=quoteAdjustment.amount isoCode=quote.currencyUomId/></div></td>
                    </tr>
                </#if>
            </#list>
            <#assign grandTotalQuoteAmount = totalQuoteAmount + totalQuoteHeaderAdjustmentAmount>

            <tr><td colspan="5"></td><td colspan="6"><hr class="sepbar"/></td></tr>
            <tr>
                <td align="right" colspan="6"><div class="tabletext"><b>${uiLabelMap.OrderGrandTotal}</b></div></td>
                <td align="right">
                    <div class="tabletext"><@ofbizCurrency amount=grandTotalQuoteAmount isoCode=quote.currencyUomId/></div>
                </td>
            </tr>
        </table>
    </div>
</div>
