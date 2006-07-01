<#--
 *  Copyright (c) 2004-2005 The Open For Business Project - www.ofbiz.org
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
 *@since      3.1
-->

<div class="head1">${uiLabelMap.ProductReviewsPendingApproval}</div>
<br/>

<#if !pendingReviews?has_content>
  <div class="head3">${uiLabelMap.CommonNo} ${uiLabelMap.ProductReviewsPendingApproval}</div>
</#if>

<#list pendingReviews as review>
  <form name="prr_${review.productReviewId}" method="post" action="<@ofbizUrl>updateProductReview</@ofbizUrl>">
    <input type="hidden" name="productReviewId" value="${review.productReviewId}">
    <table border="0" cellpadding="2">
      <#assign postedUserLogin = review.getRelatedOne("UserLogin")>
      <#assign postedPerson = postedUserLogin.getRelatedOne("Person")>
      <tr>
        <td colspan="2"><hr class="sepbar"></td>
      </tr>
      <tr>
        <td><div class="tableheadtext">${uiLabelMap.CommonPostedDate}:</div></td>
        <td><div class="tabletext">${review.postedDateTime?if_exists}</div></td>
      </tr>
      <tr>
        <td><div class="tableheadtext">${uiLabelMap.CommonPostedBy}:</div>
        <td><div class="tabletext">${postedPerson.firstName} ${postedPerson.lastName}</div></td>
      </tr>
      <tr>
        <td><div class="tableheadtext">${uiLabelMap.ProductRating}:</div>
        <td>
          <input type="text" name="productRating" class="textBox" size="5" value="${review.productRating?if_exists?string}">
        </td>
      </tr>
      <tr>
        <td><div class="tableheadtext">${uiLabelMap.CommonIsAnonymous}:</div></td>
        <td>
          <div class="tabletext">
            <select name="postedAnonymous" class="selectBox">
              <option>${review.postedAnonymous?default("N")}</option>
              <option value="${review.postedAnonymous?default("N")}">----</option>
              <option value="N">${uiLabelMap.CommonN}</option>
			  <option value="Y">${uiLabelMap.CommonY}</option>              
            </select>
          </div>
        </td>
      </tr>
      <tr>
        <td><div class="tableheadtext">${uiLabelMap.CommonStatus}:</div></td>
        <td>
          <div class="tabletext">
            <select name="statusId" class="selectBox">
              <option value="PRR_PENDING">${uiLabelMap.PendingReviewPendingApproval}</option>
              <option value="PRR_APPROVED">${uiLabelMap.PendingReviewApprove}</option>
              <option value="PRR_DELETED">${uiLabelMap.PendingReviewDelete}</option>
            </select>
          </div>
        </td>
      </tr>
      <tr>
        <td><div class="tableheadtext">${uiLabelMap.ProductReviews}:</div>
        <td>
          <textarea class="textAreaBox" name="productReview" rows="5" cols="40" wrap="hard">${review.productReview?if_exists}</textarea>
        </td>
      </tr>
      <tr>
        <td>&nbsp;</td>
        <td><input type="submit" value="${uiLabelMap.CommonSave}">
      </tr>
    </table>
  </form>
</#list>
