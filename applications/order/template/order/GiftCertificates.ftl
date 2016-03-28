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

<form id="addGiftCertificate" action="<@ofbizUrl>addGiftCertificateSurvey</@ofbizUrl>" method="post">
  <fieldset>
    <#if surveyId?? && surveyId?has_content>
      <input type="hidden" name="quantity" value="1" />
      <input type="hidden" name="surveyId" value="${surveyId!}" />
      <#if giftCardProductList?has_content>
        <label>${uiLabelMap.OrderSelectGiftAmount}</label>
        <#list giftCardProductList?sort_by("price") as giftCardProduct>
          <div>
            <input type="radio" name="add_product_id" id="productId_${giftCardProduct.price!}" value="${giftCardProduct.productId!}" checked="checked" />
            <label for="productId_${giftCardProduct.price!}"> ${giftCardProduct.productId!}&nbsp;:&nbsp;${giftCardProduct.productName!}&nbsp;:&nbsp;<@ofbizCurrency amount=giftCardProduct.price! isoCode=currencyUomId/></label>
          </div>
        </#list>
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
      <#else>
        <label>${uiLabelMap.OrderNoGiftCertificatesFound}</label>
      </#if>
    <#else>
      <label>${uiLabelMap.OrderNoProductStoreFinAccountSettingsFound}</label>
    </#if>
  </fieldset>
</form>

