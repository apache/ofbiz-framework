<#--
 *  Copyright (c) 2003-2005 The Open For Business Project - www.ofbiz.org
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
 *@author     Andy Zeneski (jaz@ofbiz.org)
 *@version    $Rev$
 *@since      2.1
-->
<#if requestParameters.product_id?exists>
  <form name="reviewProduct" method="post" action="<@ofbizUrl>createProductReview</@ofbizUrl>">
    <input type="hidden" name="productStoreId" value="${productStore.productStoreId}">
    <input type="hidden" name="productId" value="${requestParameters.product_id}">
    <input type="hidden" name="product_id" value="${requestParameters.product_id}">
    <input type="hidden" name="category_id" value="${requestParameters.category_id}">
    <table border="0" width="100%" cellpadding="2" cellspacing='0'>
      <tr>
        <td valign="top">
          <div class="tabletext">
            <b>${uiLabelMap.EcommerceRating}:</b>
            &nbsp;1&nbsp;<input type="radio" name="productRating" value="1.0">
            &nbsp;2&nbsp;<input type="radio" name="productRating" value="2.0">
            &nbsp;3&nbsp;<input type="radio" name="productRating" value="3.0">
            &nbsp;4&nbsp;<input type="radio" name="productRating" value="4.0">
            &nbsp;5&nbsp;<input type="radio" name="productRating" value="5.0">
          </div>
        <td>
      </tr>
      <tr>
        <td>
          <div class="tabletext">
            <b>${uiLabelMap.EcommercePostAnonymous}:</b>
            &nbsp;${uiLabelMap.CommonYes}&nbsp;<input type="radio" name="postedAnonymous" value="true">
            &nbsp;${uiLabelMap.CommonNo}&nbsp;<input type="radio" name="postedAnonymous" value="false" CHECKED>
          </div>
        </td>
      </tr>
      <tr>
        <td>
          <div class="tabletext"><b>${uiLabelMap.CommonReview}:</b>
        </td>
      </tr>
      <tr>
        <td>
          <textarea class="textAreaBox" name="productReview" cols="40" wrap="hard"></textarea>
        </td>
      </tr>
      <tr>
        <td>
          <a href="javascript:document.reviewProduct.submit();" class="buttontext">[${uiLabelMap.CommonSave}]</a>&nbsp;
          <a href="<@ofbizUrl>product?product_id=${requestParameters.product_id}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonCancel}]</a>
        </td>
      </tr>
    </table>
  </form>
<#else>
  <h2>${uiLabelMap.ProductCannotReviewUnKnownProduct}.</h2>
</#if>
