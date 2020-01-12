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

<script type="application/javascript">
    function paginateOrderList(viewSize, viewIndex) {
        document.paginationForm.viewSize.value = viewSize;
        document.paginationForm.viewIndex.value = viewIndex;
        document.paginationForm.submit();
    }
</script>

<div class="screenlet">
  <div class="screenlet-title-bar">
    <ul>
      <li class="h3">${uiLabelMap.OrderOrderFound}</li>
      <#if (orderList?has_content && 0 < orderList?size)>
        <#if (orderListSize > highIndex)>
          <li><a href="javascript:paginateOrderList('${viewSize}', '${viewIndex+1}')">${uiLabelMap.CommonNext}</a></li>
        <#else>
          <li><span class="disabled">${uiLabelMap.CommonNext}</span></li>
        </#if>
        <#if (orderListSize > 0)>
          <li><span>${lowIndex} - ${highIndex} ${uiLabelMap.CommonOf} ${orderListSize}</span></li>
        </#if>
        <#if (viewIndex > 1)>
          <li><a href="javascript:paginateOrderList('${viewSize}', '${viewIndex-1}')">${uiLabelMap.CommonPrevious}</a></li>
        <#else>
          <li><span class="disabled">${uiLabelMap.CommonPrevious}</span></li>
        </#if>
      </#if>
    </ul>
  </div>
  <div class="screenlet-body">
    <form name="paginationForm" method="post" action="<@ofbizUrl>viewProductOrder</@ofbizUrl>">
      <input type="hidden" name="viewSize"/>
      <input type="hidden" name="viewIndex"/>
      <#if paramIdList?? && paramIdList?has_content>
        <#list paramIdList as paramIds>
          <#assign paramId = paramIds.split("=")/>
          <#if "productId" == paramId[0]>
            <#assign productId = paramId[1]/>
          </#if>
          <input type="hidden" name="${paramId[0]}" value="${paramId[1]}"/>
        </#list>
      </#if>
    </form>
    <table class="basic-table hover-bar" cellspacing='0'>
      <tr class="header-row">
        <td>${uiLabelMap.OrderOrderId}</td>
        <td>${uiLabelMap.FormFieldTitle_itemStatusId}</td>
        <td>${uiLabelMap.FormFieldTitle_orderItemSeqId}</td>
        <td>${uiLabelMap.OrderDate}</td>
        <td>${uiLabelMap.OrderUnitPrice}</td>
        <td>${uiLabelMap.OrderQuantity}</td>
        <td>${uiLabelMap.OrderOrderType}</td>
      </tr>
      <#if orderList?has_content && productId??>
        <#list orderList as order>
          <#assign orderItems = EntityQuery.use(delegator).from("OrderItem").where("orderId", order.orderId!, "productId", productId!).queryList()!/>
          <#list orderItems as orderItem>
            <tr>
              <td><a href="<@ofbizUrl controlPath="/ordermgr/control">orderview?orderId=${orderItem.orderId}</@ofbizUrl>" class='buttontext'>${orderItem.orderId}</a></td>
              <#assign currentItemStatus = orderItem.getRelatedOne("StatusItem", false)/>
              <td>${currentItemStatus.get("description",locale)?default(currentItemStatus.statusId)}</td>
              <td>${orderItem.orderItemSeqId}</td>
              <td>${order.orderDate}</td>
              <td>${orderItem.unitPrice}</td>
              <td>${orderItem.quantity}</td>
              <#assign currentOrderType = order.getRelatedOne("OrderType", false)/>
              <td>${currentOrderType.get("description",locale)?default(currentOrderType.orderTypeId)}</td>
            </tr>
          </#list>
        </#list>
      <#else>
        <tr>
          <td colspan='4'><h3>${uiLabelMap.OrderNoOrderFound}</h3></td>
        </tr>
      </#if>
    </table>
  </div>
</div>
