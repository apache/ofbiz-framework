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
