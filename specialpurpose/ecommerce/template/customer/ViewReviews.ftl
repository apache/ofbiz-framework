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

<#if reviews?has_content>
<div class="screenlet">
  <h3>${uiLabelMap.ProductReviews}</h3>
  <div class="screenlet-body">
    <table width="100%" border="0" cellpadding="1">
      <tr>
        <th>${uiLabelMap.EcommerceSentDate}</th>
        <th>${uiLabelMap.ProductProductId}</th>
        <th>${uiLabelMap.ProductReviews}</th>
        <th>${uiLabelMap.ProductRating}</th>
        <th>${uiLabelMap.CommonIsAnonymous}</th>
        <th>${uiLabelMap.CommonStatus}</th>
      </tr>
      <#list reviews as review>
        <tr>
          <td>${review.postedDateTime!}</td>
          <td><a href="<@ofbizCatalogAltUrl productId=review.productId/>">${review.productId}</a></td>
          <td>${review.productReview!}</td>
          <td>${review.productRating}</td>
          <td>${review.postedAnonymous!}</td>
          <td>${review.getRelatedOne("StatusItem", false).get("description", locale)}</td>
        </tr>
      </#list>
    </table>
  </div>
</div>
</#if>
