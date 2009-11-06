<form id="addGiftCertificate" action="<@ofbizUrl>addGiftCertificateSurvey</@ofbizUrl>" method="post">
  <fieldset>
    <input type="hidden" name="quantity" value="1" />
    <input type="hidden" name="surveyId" value="1000" />
    <label>${uiLabelMap.OrderSelectGiftAmount}</label>
    <div>
      <input type="radio" name="add_product_id" id="productId_10" value="GC-001-C10" checked="checked" />
      <label for="productId_10">$10</label>
    </div>
    <div>
      <input type="radio" name="add_product_id" id="productId_25" value="GC-001-C25" />
      <label for="productId_25">$25</label>
    </div>
    <div>
      <input type="radio" name="add_product_id" id="productId_50" value="GC-001-C50" />
      <label for="productId_50">$50</label>
    </div>
    <div>
      <input type="radio" name="add_product_id" id="productId_100" value="GC-001-C100" />
      <label for="productId_100">$100</label>
    </div>
    <div>
      <label for="emailAddress">${uiLabelMap.OrderRecipientEmailAdd}</label>
      <input type="text" id="emailAddress" name="answers_1002" value="" />
    </div>
    <div>
      <label for="recipientName">${uiLabelMap.OrderRecipientName}</label>
      <input type="text" id="recipientName" name="answers_1001" value="" />
    </div>
    <div>
      <label for="senderName">${uiLabelMap.OrderSenderName}</label>
      <input type="text" id="senderName" name="answers_1000" value="" />
    <div>
      <label for="message">${uiLabelMap.OrderGiftMessage}:</label>
      <textarea id="message" name="answers_1003"></textarea>
    </div>
    <div>
      <input type="submit" value="${uiLabelMap.CommonSubmit}" />
    </div>
  </fieldset>
</form>
