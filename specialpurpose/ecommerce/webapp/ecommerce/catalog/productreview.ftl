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
