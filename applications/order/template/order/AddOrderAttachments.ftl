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
<div class="screenlet add-order-attachments">
  <div class="screenlet-title-bar">
    Add Order Attachments
  </div>
  <div class="screenlet-body">
    <form name="AddOrderAttachmentsForm" method="post" action="<@ofbizUrl>addOrderAttachments</@ofbizUrl>" enctype="multipart/form-data">
      <input type="hidden" name="orderId" value="${parameters.orderId!}">
      <input type="hidden" name="fromDate" value="${fromDate!}">
      <div>
        ${uiLabelMap.OrderOrderId}
        <input name="orderId" type="text" value="${parameters.orderId!}" disabled/>
      </div>
      <div>
        ${uiLabelMap.OrderOrderItemSeqId}
        <select name="orderItemSeqId">
          <#if orderItems?has_content>
            <#list orderItems as orderItem>
              <option name="orderItemSeqId" value="${orderItem.orderItemSeqId!}">${orderItem.orderItemSeqId!}</option>
            </#list>
          </#if>
        </select>
      </div>
      <div>
        OrderContentTypeId
        <select name="orderContentTypeId">
          <#if orderContentTypes?has_content>
            <#list orderContentTypes as orderContentType>
              <option name="orderContentTypeId" value="${orderContentType.orderContentTypeId!}">${orderContentType.description!}</option>
            </#list>
          </#if>
        </select>
      </div>
      <div>
        <input type="file" name="uploadedFile" class="required" size="25">
      </div>
      <div>
        <button type="submit">
          ${uiLabelMap.PartyAttachFile}
        </button>
      </div>
    </form>
  </div>
</div>
