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

<div id="subscription-summary" class="screenlet">
  <div class="screenlet-title-bar">
    <span class="h3">${uiLabelMap.ProductSubscriptions}</span>
  </div>
  <div class="screenlet-body">
    <table width="100%" cellspacing="0" cellpadding="2">
      <thead>
      <tr class="header-row">
        <td>
          <div class="tableheadtext">${uiLabelMap.ProductSubscription} ${uiLabelMap.CommonId}</div>
        </td>
        <td>
          <div class="tableheadtext">${uiLabelMap.ProductSubscription} ${uiLabelMap.CommonType}</div>
        </td>
        <td>
          <div class="tableheadtext">${uiLabelMap.CommonDescription}</div>
        </td>
        <td>
          <div class="tableheadtext">${uiLabelMap.ProductProductName}</div>
        </td>
        <td>
          <div class="tableheadtext">${uiLabelMap.CommonFromDate}</div>
        </td>
        <td>
          <div class="tableheadtext">${uiLabelMap.CommonThruDate}</div>
        </td>
      </tr>
      <tr>
        <td colspan="6">
          <hr/>
        </td>
      </tr>
      </thead>
      <tbody>
      <#list subscriptionList as subscription>
      <tr>
        <td>${subscription.subscriptionId}</td>
        <td>
          <#assign subscriptionType = subscription.getRelatedOne('SubscriptionType', false)!>
                            ${(subscriptionType.description)?default(subscription.subscriptionTypeId?default('N/A'))}
        </td>
        <td>${subscription.description!}</td>
        <td>
          <#assign product = subscription.getRelatedOne('Product', false)!>
          <#if product?has_content>
            <#assign productName = Static['org.apache.ofbiz.product.product.ProductContentWrapper']
                .getProductContentAsText(product, 'PRODUCT_NAME', request, "html")!>
            <a href="<@ofbizUrl>product?product_id=${product.productId}</@ofbizUrl>" class="linktext">
              ${productName?default(product.productId)}
            </a>
          </#if>
        </td>
        <td>${subscription.fromDate!}</td>
        <td>${subscription.thruDate!}</td>
      </tr>
      </#list>
      </tbody>
    </table>
  </div>
</div>

