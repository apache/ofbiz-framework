<#--
 *  Copyright (c) 2003 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a 
 *  copy of this software and associated documentation files (the "Software"), 
 *  to deal in the Software without restriction, including without limitation 
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 *  and/or sell copies of the Software, and to permit persons to whom the 
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included 
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS 
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT 
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR 
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *@author     Jacopo Cappellato (tiz@sastau.it)
 *@version    $Rev$
-->

<div class="screenlet">
    <div class="screenlet-header">
        <div style="float: right;">
            <#if maySelectItems?default("N") == "Y">
                <a href="javascript:document.addCommonToCartForm.add_all.value='true';document.addCommonToCartForm.submit()" class="submenutext">${uiLabelMap.EcommerceAddAlltoCart}</a>
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
