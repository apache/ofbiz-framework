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

<#if shoppingCart?has_content && shoppingCart.size() &gt; 0>
  <h3>${uiLabelMap.EcommerceStep} 1: ${uiLabelMap.PageTitleShoppingCart}</h3>
  <div id="cartSummaryPanel" style="display: none;">
    <a href="javascript:void(0);" id="openCartPanel" class="button">${uiLabelMap.EcommerceClickHereToEdit}</a>
    <table id="cartSummaryPanel_cartItems" summary="This table displays the list of item added into Shopping Cart.">
      <thead>
        <tr>
          <th id="orderItem">${uiLabelMap.OrderItem}</th>
          <th id="description">${uiLabelMap.CommonDescription}</th>
          <th id="unitPrice">${uiLabelMap.EcommerceUnitPrice}</th>
          <th id="quantity">${uiLabelMap.OrderQuantity}</th>
          <th id="adjustment">${uiLabelMap.EcommerceAdjustments}</th>
          <th id="itemTotal">${uiLabelMap.EcommerceItemTotal}</th>
        </tr>
      </thead>
      <tfoot>
        <tr id="completedCartSubtotalRow">
          <th id="subTotal" scope="row" colspan="5">${uiLabelMap.CommonSubtotal}</th>
          <td headers="subTotal" id="completedCartSubTotal">
            <@ofbizCurrency amount=shoppingCart.getSubTotal() isoCode=shoppingCart.getCurrency() />
          </td>
        </tr>
        <#assign orderAdjustmentsTotal = 0 />
        <#list shoppingCart.getAdjustments() as cartAdjustment>
          <#assign orderAdjustmentsTotal = orderAdjustmentsTotal +
              Static["org.apache.ofbiz.order.order.OrderReadHelper"]
              .calcOrderAdjustment(cartAdjustment, shoppingCart.getSubTotal()) />
        </#list>
        <tr id="completedCartDiscountRow">
          <th id="productDiscount" scope="row" colspan="5">${uiLabelMap.ProductDiscount}</th>
          <td headers="productDiscount" id="completedCartDiscount">
            <input type="hidden" value="${orderAdjustmentsTotal}" id="initializedCompletedCartDiscount" />
            <@ofbizCurrency amount=orderAdjustmentsTotal isoCode=shoppingCart.getCurrency() />
          </td>
        </tr>
        <tr>
          <th id="shippingAndHandling" scope="row" colspan="5">${uiLabelMap.OrderShippingAndHandling}</th>
          <td headers="shippingAndHandling" id="completedCartTotalShipping">
            <@ofbizCurrency amount=shoppingCart.getTotalShipping() isoCode=shoppingCart.getCurrency() />
          </td>
        </tr>
        <tr>
          <th id="salesTax" scope="row" colspan="5">${uiLabelMap.OrderSalesTax}</th>
          <td headers="salesTax" id="completedCartTotalSalesTax">
            <@ofbizCurrency amount=shoppingCart.getTotalSalesTax() isoCode=shoppingCart.getCurrency() />
          </td>
        </tr>
        <tr>
          <th id="grandTotal" scope="row" colspan="5">${uiLabelMap.OrderGrandTotal}</th>
          <td headers="grandTotal" id="completedCartDisplayGrandTotal">
            <@ofbizCurrency amount=shoppingCart.getDisplayGrandTotal() isoCode=shoppingCart.getCurrency() />
          </td>
        </tr>
      </tfoot>
      <tbody>
        <#list shoppingCart.items() as cartLine>
          <#if cartLine.getProductId()??>
            <#if cartLine.getParentProductId()??>
              <#assign parentProductId = cartLine.getParentProductId() />
            <#else>
              <#assign parentProductId = cartLine.getProductId() />
            </#if>
            <#assign smallImageUrl = Static["org.apache.ofbiz.product.product.ProductContentWrapper"]
                .getProductContentAsText(cartLine.getProduct(), "SMALL_IMAGE_URL", locale, dispatcher, "url")! />
            <#if !smallImageUrl?string?has_content><#assign smallImageUrl = "" /></#if>
          </#if>
          <tr id="cartItemDisplayRow_${cartLine_index}">
            <td headers="orderItem">
              <img src="<@ofbizContentUrl>${requestAttributes.contentPathPrefix!}${smallImageUrl}</@ofbizContentUrl>"
                  alt = "Product Image" /></td>
            <td headers="description">${cartLine.getName()!}</td>
            <td headers="unitPrice">${cartLine.getDisplayPrice()}</td>
            <td headers="quantity">
              <span id="completedCartItemQty_${cartLine_index}">${cartLine.getQuantity()?string.number}</span>
            </td>
            <td headers="adjustment">
              <span id="completedCartItemAdjustment_${cartLine_index}">
                <@ofbizCurrency amount=cartLine.getOtherAdjustments() isoCode=shoppingCart.getCurrency() />
              </span>
            </td>
            <td headers="itemTotal" align="right">
              <span id="completedCartItemSubTotal_${cartLine_index}">
                <@ofbizCurrency amount=cartLine.getDisplayItemSubTotal() isoCode=shoppingCart.getCurrency() />
              </span>
            </td>
          </tr>
        </#list>
      </tbody>
    </table>
  </div>
  <div id="editCartPanel">
    <form id="cartForm" method="post" action="<@ofbizUrl></@ofbizUrl>">
      <fieldset>
        <input type="hidden" name="removeSelected" value="false" />
        <div id="cartFormServerError" class="errorMessage"></div>
        <table id="editCartPanel_cartItems">
          <thead>
            <tr>
              <th id="editOrderItem">${uiLabelMap.OrderItem}</th>
              <th id="editDescription">${uiLabelMap.CommonDescription}</th>
              <th id="editUnitPrice">${uiLabelMap.EcommerceUnitPrice}</th>
              <th id="editQuantity">${uiLabelMap.OrderQuantity}</th>
              <th id="editAdjustment">${uiLabelMap.EcommerceAdjustments}</th>
              <th id="editItemTotal">${uiLabelMap.EcommerceItemTotal}</th>
              <th id="removeItem">${uiLabelMap.FormFieldTitle_removeButton}</th>
            </tr>
          </thead>
          <tfoot>
            <tr>
              <th scope="row" colspan="6">${uiLabelMap.CommonSubtotal}</th>
              <td id="cartSubTotal">
                <@ofbizCurrency amount=shoppingCart.getSubTotal() isoCode=shoppingCart.getCurrency() />
              </td>
            </tr>
            <tr>
              <th scope="row" colspan="6">${uiLabelMap.ProductDiscount}</th>
              <td id="cartDiscountValue">
                <#assign orderAdjustmentsTotal = 0  />
                <#list shoppingCart.getAdjustments() as cartAdjustment>
                  <#assign orderAdjustmentsTotal = orderAdjustmentsTotal +
                      Static["org.apache.ofbiz.order.order.OrderReadHelper"]
                      .calcOrderAdjustment(cartAdjustment, shoppingCart.getSubTotal()) />
                </#list>
                <@ofbizCurrency amount=orderAdjustmentsTotal isoCode=shoppingCart.getCurrency() />
              </td>
            </tr>
            <tr>
              <th scope="row" colspan="6">${uiLabelMap.OrderShippingAndHandling}</th>
              <td id="cartTotalShipping">
                <@ofbizCurrency amount=shoppingCart.getTotalShipping() isoCode=shoppingCart.getCurrency() />
              </td>
            </tr>
            <tr>
              <th scope="row" colspan="6">${uiLabelMap.OrderSalesTax}</th>
              <td id="cartTotalSalesTax">
                <@ofbizCurrency amount=shoppingCart.getTotalSalesTax() isoCode=shoppingCart.getCurrency() />
              </td>
            </tr>
            <tr>
              <th scope="row" colspan="6">${uiLabelMap.OrderGrandTotal}</th>
              <td id="cartDisplayGrandTotal">
                <@ofbizCurrency amount=shoppingCart.getDisplayGrandTotal() isoCode=shoppingCart.getCurrency() />
              </td>
            </tr>
          </tfoot>
          <tbody id="updateBody">
            <#list shoppingCart.items() as cartLine>
              <tr id="cartItemRow_${cartLine_index}">
                <td headers="editOrderItem">
                  <#if cartLine.getProductId()??>
                    <#if cartLine.getParentProductId()??>
                      <#assign parentProductId = cartLine.getParentProductId() />
                    <#else>
                      <#assign parentProductId = cartLine.getProductId() />
                    </#if>
                    <#assign smallImageUrl = Static["org.apache.ofbiz.product.product.ProductContentWrapper"]
                        .getProductContentAsText(cartLine.getProduct(), "SMALL_IMAGE_URL",
                        locale, dispatcher, "url")! />
                    <#if !smallImageUrl?string?has_content><#assign smallImageUrl = "" /></#if>
                    <#if smallImageUrl?string?has_content>
                      <img src="<@ofbizContentUrl>${requestAttributes.contentPathPrefix!}${smallImageUrl}</@ofbizContentUrl>"
                          alt="Product Image" />
                    </#if>
                  </#if>
                </td>
                <td headers="editDescription">${cartLine.getName()!}</td>
                <td headers="editUnitPrice" id="itemUnitPrice_${cartLine_index}">
                  <@ofbizCurrency amount=cartLine.getDisplayPrice() isoCode=shoppingCart.getCurrency() />
                </td>
                <td headers="editQuantity">
                  <#if cartLine.getIsPromo()>
                    ${cartLine.getQuantity()?string.number}
                  <#else>
                    <input type="hidden" name="cartLineProductId" id="cartLineProductId_${cartLine_index}"
                        value="${cartLine.getProductId()}" />
                    <input type="text" name="update${cartLine_index}" id="qty_${cartLine_index}"
                        value="${cartLine.getQuantity()?string.number}" class="required validate-number" />
                    <span id="advice-required-qty_${cartLine_index}" style="display:none;" class="errorMessage">
                      (${uiLabelMap.CommonRequired})
                    </span>
                    <span id="advice-validate-number-qty_${cartLine_index}" style="display:none;" class="errorMessage">
                      (${uiLabelMap.CommonPleaseEnterValidNumberInThisField})
                    </span>
                  </#if>
                </td>
                <#if !cartLine.getIsPromo()>
                  <td headers="editAdjustment" id="addPromoCode_${cartLine_index}">
                    <@ofbizCurrency amount=cartLine.getOtherAdjustments() isoCode=shoppingCart.getCurrency() />
                  </td>
                <#else>
                  <td headers="editAdjustment">
                    <@ofbizCurrency amount=cartLine.getOtherAdjustments() isoCode=shoppingCart.getCurrency() />
                  </td>
                </#if>
                <td headers="editItemTotal" id="displayItem_${cartLine_index}">
                  <@ofbizCurrency amount=cartLine.getDisplayItemSubTotal() isoCode=shoppingCart.getCurrency() />
                </td>
                <#if !cartLine.getIsPromo()>
                  <td>
                    <a id="removeItemLink_${cartLine_index}" href="javascript:void(0);">
                      <img id="remove_${cartLine_index}"
                          src="<@ofbizContentUrl>/ecommerce/images/remove.png</@ofbizContentUrl>"
                          alt="Remove Item Image" />
                    </a>
                  </td>
                </#if>
              </tr>
            </#list>
          </tbody>
        </table>
      </fieldset>
      <fieldset id="productPromoCodeFields">
        <div>
          <label for="productPromoCode">${uiLabelMap.EcommerceEnterPromoCode}</label>
          <input id="productPromoCode" name="productPromoCode" type="text" value="" />
        </div>
      </fieldset>
      <fieldset>
        <a href="javascript:void(0);" class="button" id="updateShoppingCart" >
          ${uiLabelMap.EcommerceContinueToStep} 2
        </a>
        <a style="display: none" class="button" href="javascript:void(0);" id="processingShipping">
          ${uiLabelMap.EcommercePleaseWait}....
        </a>
      </fieldset>
    </form>
  </div>
</#if>
