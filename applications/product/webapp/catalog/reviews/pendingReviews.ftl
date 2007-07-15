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
              <#if (((review.postedAnonymous)!"") == "Y")><option value="test">${uiLabelMap.CommonY}</option></#if>
              <#if (((review.postedAnonymous)!"") == "N")><option value="N">${uiLabelMap.CommonN}</option></#if>
              <option></option>
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
