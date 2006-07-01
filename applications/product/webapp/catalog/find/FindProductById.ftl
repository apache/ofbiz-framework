<#--
 *  Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
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
 *@author     David E. Jones
 *@created    May 22 2001
 *@version    1.0
-->


<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxhead">${uiLabelMap.ProductFindProductWithIdValue}</div>
    </div>
    <div class="screenlet-body">
        <form name="idsearchform" method="post" action="<@ofbizUrl>FindProductById</@ofbizUrl>" style="margin: 0;">
          <div class="tabletext">${uiLabelMap.CommonId} ${uiLabelMap.CommonValue}: <input type="text" name="idValue" size="20" maxlength="50" value="${idValue?if_exists}">&nbsp;<a href="javascript:document.idsearchform.submit()" class="buttontext">${uiLabelMap.CommonFind}</a></div>
        </form>
    </div>
</div>


<div class="head1">${uiLabelMap.ProductSearchResultsWithIdValue}: [${idValue?if_exists}]</div>


<#if !goodIdentifications?has_content && !idProduct?has_content>
    <br/>
    <div class="head2">&nbsp;${uiLabelMap.ProductNoResultsFound}.</div>
<#else/>
  <table cellpadding="2">
    <#if idProduct?has_content>
        <td>
          <div class="tabletext"><b>[${idProduct.productId}]</b></div>
        </td>
        <td>&nbsp;&nbsp;</td>
        <td>
            <a href="<@ofbizUrl>EditProduct?productId=${idProduct.productId}</@ofbizUrl>" class="buttontext">${(idProduct.internalName)?if_exists}</a>
            <span class="tabletext">(${uiLabelMap.ProductSearchResultsFound})</span>
        </td>
    </#if>
    <#list goodIdentifications as goodIdentification>
        <#assign product = goodIdentification.getRelatedOneCache("Product")/>
        <#assign goodIdentificationType = goodIdentification.getRelatedOneCache("GoodIdentificationType")/>
        <tr>
            <td>
                <div class="tabletext"><b>[${product.productId}]</b></div>
            </td>
            <td>&nbsp;&nbsp;</td>
            <td>
                <a href="<@ofbizUrl>EditProduct?productId=${product.productId}</@ofbizUrl>" class="buttontext">${(product.internalName)?if_exists}</a>
                <span class="tabletext">(${uiLabelMap.ProductSearchResultsFound} <b>${goodIdentificationType.get("description",locale)?default(goodIdentification.goodIdentificationTypeId)}</b>.)</span>
            </td>
        </tr>
    </#list>
  </table>
</#if>
