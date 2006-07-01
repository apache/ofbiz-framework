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
        <div class="boxhead">&nbsp; ${uiLabelMap.OrderRequestItems}</div>
    </div>
    <div class="screenlet-body">
        <table width="100%" border="0" cellpadding="0">
            <tr align="left" valign="bottom">
                <td width="10%" align="left"><span class="tableheadtext"><b>${uiLabelMap.ProductItem}</b></span></td>
                <td width="35%" align="left"><span class="tableheadtext"><b>${uiLabelMap.EcommerceProduct}</b></span></td>
                <td width="10%" align="right"><span class="tableheadtext"><b>${uiLabelMap.ProductQuantity}</b></span></td>
                <td width="10%" align="right"><span class="tableheadtext"><b>${uiLabelMap.OrderAmount}</b></span></td>
                <td width="10%" align="right"><span class="tableheadtext"><b>${uiLabelMap.OrderRequestMaximumAmount}</b></span></td>
                <td width="5%" align="right">&nbsp;</td>
            </tr>
            <#list requestItems as requestItem>
                <#if requestItem.productId?exists>
                    <#assign product = requestItem.getRelatedOne("Product")>
                </#if>

                <tr><td colspan="6"><hr class="sepbar"/></td></tr>
                <tr>
                    <td valign="top">
                        <div class="tabletext" style="font-size: xx-small;">
                        <#if showRequestManagementLinks?exists>
                            <a href="<@ofbizUrl>EditRequestItem?custRequestId=${requestItem.custRequestId}&amp;custRequestItemSeqId=${requestItem.custRequestItemSeqId}</@ofbizUrl>" class="buttontext">${requestItem.custRequestItemSeqId}</a>
                        <#else>
                            ${requestItem.custRequestItemSeqId}
                        </#if>
                        </div>
                    </td>
                    <td valign="top">
                        <div class="tabletext">
                            ${(product.internalName)?if_exists}&nbsp;
                            <#if showRequestManagementLinks?exists>
                                <a href="/catalog/control/EditProduct?productId=${requestItem.productId?if_exists}" class="buttontext">${requestItem.productId?if_exists}</a>
                            <#else>
                                <a href="<@ofbizUrl>product?product_id=${requestItem.productId?if_exists}</@ofbizUrl>" class="buttontext">${requestItem.productId?if_exists}</a>
                            </#if>
                        </div>
                    </td>
                    <td align="right" valign="top"><div class="tabletext">${requestItem.quantity?if_exists}</div></td>
                    <td align="right" valign="top"><div class="tabletext">${requestItem.selectedAmount?if_exists}</div></td>
                    <td align="right" valign="top"><div class="tabletext"><@ofbizCurrency amount=requestItem.maximumAmount isoCode=request.maximumAmountUomId/></div></td>
                </tr>
            </#list>
        </table>
    </div>
</div>
