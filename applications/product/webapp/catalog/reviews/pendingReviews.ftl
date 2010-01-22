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
<script language="JavaScript" type="text/javascript">
    function changeReviewStatus(statusId) {
        document.selectAllForm.statusId.value = statusId;
        document.selectAllForm.submit();
    }
</script>
<div class="screenlet">
    <div class="screenlet-title-bar">
        <h3>${uiLabelMap.ProductReviewsPendingApproval}</h3>
    </div>
    <div class="screenlet-body">
        <#if !pendingReviews?has_content>
            <h3>${uiLabelMap.ProductReviewsNoPendingApproval}</h3>
        <#else>
            <form method='POST' action='<@ofbizUrl>updateProductReview</@ofbizUrl>' name="selectAllForm">
                <input type="hidden" name="_useRowSubmit" value="Y">
                <input type="hidden" name="_checkGlobalScope" value="Y">
                <input type="hidden" name="statusId" value="">
                <div align="right">
                    <input type="button" value="${uiLabelMap.CommonUpdate}" onClick="javascript:changeReviewStatus('PRR_PENDING')">
                    <input type="button" value="${uiLabelMap.ProductPendingReviewUpdateAndApprove}" onClick="javascript:changeReviewStatus('PRR_APPROVED')">
                    <input type="button" value="${uiLabelMap.CommonDelete}" onClick="javascript:changeReviewStatus('PRR_DELETED')">
                </div>
                <table cellspacing="0" class="basic-table">
                  <tr class="header-row">
                    <td><b>${uiLabelMap.ProductPendingReviewDate}</b></td>
                    <td><b>${uiLabelMap.ProductPendingReviewBy}</b></td>
                    <td><b>${uiLabelMap.CommonIsAnonymous}</b></td>
                    <td><b>${uiLabelMap.ProductProductId}</b></td>
                    <td><b>${uiLabelMap.ProductRating}</b></td>
                    <td><b>${uiLabelMap.CommonStatus}</b></td>
                    <td><b>${uiLabelMap.ProductReviews}</b></td>
                    <td align="right">
                        <span class="label">${uiLabelMap.CommonAll}</span>
                        <input type="checkbox" name="selectAll" value="${uiLabelMap.CommonY}" onclick="javascript:toggleAll(this, 'selectAllForm');highlightAllRows(this, 'review_tableRow_', 'selectAllForm');">
                    </td>
                  </tr>
                <#assign rowCount = 0>
                <#assign rowClass = "2">
                <#list pendingReviews as review>
                <#if review.userLoginId?has_content>
                <#assign postedUserLogin = review.getRelatedOne("UserLogin")>
                <#assign party = postedUserLogin.getRelatedOne("Party")>
                <#assign partyTypeId = party.get("partyTypeId")>
                <#if partyTypeId == "PERSON">
                    <#assign postedPerson = postedUserLogin.getRelatedOne("Person")>
                <#else>
                    <#assign postedPerson = postedUserLogin.getRelatedOne("PartyGroup")>
                </#if>
                </#if>
                  <tr id="review_tableRow_${rowCount}" valign="middle"<#if rowClass == "1"> class="alternate-row"</#if>>
                      <td>
                          <input type="hidden" name="productReviewId_o_${rowCount}" value="${review.productReviewId}">
                          ${review.postedDateTime?if_exists}
                      </td>
                      <#if postedPerson?has_content>
                      <#if postedPerson.firstName?has_content && postedPerson.lastName?has_content>
                          <td>${postedPerson.firstName} ${postedPerson.lastName}</td>
                      <#else>
                          <td>${postedPerson.groupName}</td>
                      </#if>
                      </#if>
                      <td>
                          <select name='postedAnonymous_o_${rowCount}'>
                              <option>${review.postedAnonymous?default("N")}</option>
                              <option value="${review.postedAnonymous?default("N")}">----</option>
                              <option value="N">${uiLabelMap.CommonN}</option>
                              <option value="Y">${uiLabelMap.CommonY}</option>
                          </select>
                      </td>
                      <td>${review.getRelatedOne("Product").internalName?if_exists}<br/><a class="buttontext" href="<@ofbizUrl>EditProduct?productId=${review.productId}</@ofbizUrl>">${review.productId}</a></td>
                      <td>
                          <input type="text" size='3' name="productRating_o_${rowCount}" value="${review.productRating?if_exists?string}">
                      </td>
                      <td>${review.getRelatedOne("StatusItem").get("description", locale)}</td>
                      <td>
                         <textarea name="productReview_o_${rowCount}" rows="5" cols="30" wrap="hard">${review.productReview?if_exists}</textarea>
                      </td>
                      <td align="right">
                        <input type="checkbox" name="_rowSubmit_o_${rowCount}" value="Y" onclick="javascript:checkToggle(this, 'selectAllForm');highlightRow(this,'review_tableRow_${rowCount}');">
                      </td>
                  </tr>
                <#assign rowCount = rowCount + 1>
                <#-- toggle the row color -->
                <#if rowClass == "2">
                    <#assign rowClass = "1">
                <#else>
                    <#assign rowClass = "2">
                </#if>
                </#list>
                <input type="hidden" name="_rowCount" value="${rowCount}">
                </table>
            </form>
        </#if>
    </div>
</div>
