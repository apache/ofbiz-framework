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

<div class="screenlet">
  <#assign shoppingCart = sessionAttributes.shoppingCart?if_exists>
  <div class="screenlet-header">
    <div class="boxhead">${uiLabelMap.OrderCheckout}</div>
  </div>
  <div class="screenlet-body">
    <#if shoppingCart?has_content && shoppingCart.size() gt 0>
      <div id="checkoutPanel" align="center">

<#-- ========================================================================================================================== -->
        <div id="cartPanel" class="screenlet">
          <div class="screenlet-header"><div class="boxhead">${uiLabelMap.EcommerceStep} 1: ${uiLabelMap.PageTitleShoppingCart}</div></div>
          <div id="cartSummaryPanel" class="screenlet-body" style="display: none;">
            <div><h3><span><a class="buttontext" href="javascript:void(0);" id="openCartPanel">${uiLabelMap.EcommerceClickHereToEdit}</a></span></h3></div>
            <div align="center"><h3>${uiLabelMap.OrderShoppingCart} ${uiLabelMap.EcommerceSummary}</h3></div>
            <table width="75%" cellspacing="0" cellpadding="1" border="0">
              <thead>
                <tr>
                  <td><div><b>${uiLabelMap.OrderItem}</b></div></td>
                  <td><div><b>${uiLabelMap.CommonDescription}</b></div></td>
                  <td align="center"><div><b>${uiLabelMap.EcommerceUnitPrice}</b></div></td>
                  <td align="center"><div><b>${uiLabelMap.OrderQuantity}</b></div></td>
                  <td align="center"><div><b>${uiLabelMap.EcommerceAdjustments}</b></div></td>
                  <td align="right"><div><b>${uiLabelMap.EcommerceItemTotal}</b></div></td>
                </tr>
                <tr><td colspan="6"><hr/></td></tr>
              </thead>
              <tbody>
                <#assign itemCount = 0>
                <#list shoppingCart.items() as cartLine>
                  <#assign cartLineIndex = itemCount>
                  <#if cartLine.getProductId()?exists>
                    <#if cartLine.getParentProductId()?exists>
                      <#assign parentProductId = cartLine.getParentProductId()/>
                    <#else>
                      <#assign parentProductId = cartLine.getProductId()/>
                    </#if>
                    <#assign smallImageUrl = Static["org.ofbiz.product.product.ProductContentWrapper"].getProductContentAsText(cartLine.getProduct(), "SMALL_IMAGE_URL", locale, dispatcher)?if_exists>
                    <#if !smallImageUrl?string?has_content><#assign smallImageUrl = ""></#if>
                  </#if>
                  <tr id="cartItemDisplayRow_${cartLineIndex}">
                    <td><div><img src="<@ofbizContentUrl>${requestAttributes.contentPathPrefix?if_exists}${smallImageUrl}</@ofbizContentUrl>" align="center" height="20" hspace="0" vspace="0" width="20"></div></td>
                    <td><div>${cartLine.getName()?if_exists}</div></td>
                    <td align="center"><div>${cartLine.getDisplayPrice()}</div></td>
                    <td align="center"><div><span id="completedCartItemQty_${cartLineIndex}">${cartLine.getQuantity()?string.number}</span></div></td>
                    <td align="center"><div><span id="completedCartItemAdjustment_${cartLineIndex}"><@ofbizCurrency amount=cartLine.getOtherAdjustments() isoCode=shoppingCart.getCurrency()/></span></div></td>
                    <td align="right"><div id="completedCartItemSubTotal_${cartLineIndex}"><@ofbizCurrency amount=cartLine.getDisplayItemSubTotal() isoCode=shoppingCart.getCurrency()/></div></td>
                  </tr>
                  <tr><td colspan="6"><hr/></td></tr>
                  <#assign itemCount = itemCount + 1>
                </#list>
                <tr id="completedCartSubtotalRow">
                  <td colspan="4"></td>
                  <td><div align="right"><b>${uiLabelMap.CommonSubtotal}:</b></div></td>
                  <td><div id="completedCartSubTotal" align="right"><@ofbizCurrency amount=shoppingCart.getSubTotal() isoCode=shoppingCart.getCurrency()/></div></td>
                </tr>
                <#assign orderAdjustmentsTotal = 0>
                <#list shoppingCart.getAdjustments() as cartAdjustment>
                  <#assign orderAdjustmentsTotal = orderAdjustmentsTotal + Static["org.ofbiz.order.order.OrderReadHelper"].calcOrderAdjustment(cartAdjustment, shoppingCart.getSubTotal())>
                </#list>
                <tr id="completedCartDiscountRow">
                  <td colspan="4"><input type="hidden" value="${orderAdjustmentsTotal}" id="initializedCompletedCartDiscount"/></td>
                  <td><div align="right"><b>${uiLabelMap.ProductDiscount}:</b></div></td>
                  <td><div id="completedCartDiscount" align="right"><@ofbizCurrency amount=orderAdjustmentsTotal isoCode=shoppingCart.getCurrency()/></div></td>
                </tr>
                <tr>
                  <td colspan="4"></td>
                  <td><div align="right"><b>${uiLabelMap.OrderShippingAndHandling}:</b></div></td>
                  <td><div id="completedCartTotalShipping" align="right"><@ofbizCurrency amount=shoppingCart.getTotalShipping() isoCode=shoppingCart.getCurrency()/></div></td>
                </tr>
                <tr>
                  <td colspan="4"></td>
                  <td><div align="right"><b>${uiLabelMap.OrderSalesTax}:</b></div></td>
                  <td><div id="completedCartTotalSalesTax" align="right"><@ofbizCurrency amount=shoppingCart.getTotalSalesTax() isoCode=shoppingCart.getCurrency()/></div></td>
                </tr>
                <tr>
                  <td colspan="4"></td>
                  <td><div align="right"><b>${uiLabelMap.OrderGrandTotal}:</b></div></td>
                  <td><div id="completedCartDisplayGrandTotal" align="right"><@ofbizCurrency amount=shoppingCart.getDisplayGrandTotal() isoCode=shoppingCart.getCurrency()/></div></td>
                </tr>
              </tbody>
            </table>
          </div>

<#-- ============================================================= -->
          <div id="editCartPanel" class="screenlet-body">
            <form name="cartForm" id="cartForm" method="post" action="<@ofbizUrl></@ofbizUrl>">
              <input type="hidden" name="removeSelected" value="false">
              <div id="cartFormServerError" class="errorMessage"></div>
              <table width="75%" cellspacing="0" cellpadding="1" border="0">
                <thead>
                  <tr>
                    <td><div><b>${uiLabelMap.OrderItem}</b></div></td>
                    <td><div><b>${uiLabelMap.CommonDescription}</b></div></td>
                    <td align="center"><div><b>${uiLabelMap.EcommerceUnitPrice}</b></div></td>
                    <td align="center"><div><b>${uiLabelMap.OrderQuantity}</b></div></td>
                    <td align="center"><div><b>${uiLabelMap.EcommerceAdjustments}</b></div></td>
                    <td align="center"><div><b>${uiLabelMap.EcommerceItemTotal}</b></div></td>
                    <td align="right"><div><b>${uiLabelMap.FormFieldTitle_removeButton}</b></div></td>
                  </tr>
                  <tr><td colspan="7"><hr/></td></tr>
                </thead>
                <tbody id="updateBody">
                  <#assign itemCount = 0>
                  <#list shoppingCart.items() as cartLine>
                    <#assign cartLineIndex = itemCount>
                    <#assign productId = cartLineIndex>
                    <tr id="cartItemRow_${cartLineIndex}">
                      <td style="padding: 1px;" valign="top">
                        <#if cartLine.getProductId()?exists>
                          <#if cartLine.getParentProductId()?exists>
                            <#assign parentProductId = cartLine.getParentProductId()/>
                          <#else>
                            <#assign parentProductId = cartLine.getProductId()/>
                          </#if>
                          <#assign smallImageUrl = Static["org.ofbiz.product.product.ProductContentWrapper"].getProductContentAsText(cartLine.getProduct(), "SMALL_IMAGE_URL", locale, dispatcher)?if_exists>
                          <#if !smallImageUrl?string?has_content><#assign smallImageUrl = ""></#if>
                          <#if smallImageUrl?string?has_content>
                            <img src="<@ofbizContentUrl>${requestAttributes.contentPathPrefix?if_exists}${smallImageUrl}</@ofbizContentUrl>" border="0" height="50" hspace="0" vspace="0" width="50"/>
                          </#if>
                        </#if>
                      </td>
                      <td><div>${cartLine.getName()?if_exists}</div></td>
                      <td align="center"><div id="itemUnitPrice_${cartLineIndex}"><@ofbizCurrency amount=cartLine.getDisplayPrice() isoCode=shoppingCart.getCurrency()/></div></td>
                      <td align="center">
                        <#if cartLine.getIsPromo()>
                          ${cartLine.getQuantity()?string.number}
                        <#else>
                          <input type="hidden" name="cartLineProductId" id="cartLineProductId_${cartLineIndex}" value="${cartLine.getProductId()}">
                          <div>
                            <span>
                              <input type="text" name="update${cartLineIndex}" id="qty_${cartLineIndex}" value="${cartLine.getQuantity()?string.number}" size="6" class="required validate-number">
                            </span>
                            <label for="qty_${cartLineIndex}"><span id="advice-required-qty_${cartLineIndex}" style="display:none;" class="errorMessage"> (required)</span></label>
                          </div>
                        </#if>
                      </td>
                      <#if !cartLine.getIsPromo()>
                        <td nowrap align="center"><div id="addPromoCode_${cartLineIndex}" class="tabletext"><@ofbizCurrency amount=cartLine.getOtherAdjustments() isoCode=shoppingCart.getCurrency()/></div></td>
                      <#else>
                        <td nowrap align="center"><div class="tabletext"><@ofbizCurrency amount=cartLine.getOtherAdjustments() isoCode=shoppingCart.getCurrency()/></div></td>
                      </#if>
                      <td align="center"><div id="displayItem_${cartLineIndex}"><@ofbizCurrency amount=cartLine.getDisplayItemSubTotal() isoCode=shoppingCart.getCurrency()/></div></td>
                      <#if !cartLine.getIsPromo()>
                        <td align="right"><a href="javascript:void(0);"><img id="remove_${cartLineIndex?if_exists}" src="<@ofbizContentUrl>/ecommerce/images/remove.png</@ofbizContentUrl>" border="0" height="30" hspace="0" vspace="0" width="40"></a></td>
                      </#if>
                    </tr>
                    <tr><td colspan="7"><hr/></td></tr>
                    <#assign itemCount = itemCount + 1>
                  </#list>
                  <tr>
                    <td colspan="4"></td>
                    <td><div align="right"><b>${uiLabelMap.CommonSubtotal}:</b></div></td>
                    <td><div id="cartSubTotal" align="center"><@ofbizCurrency amount=shoppingCart.getSubTotal() isoCode=shoppingCart.getCurrency()/></div></td>
                  </tr>
                  <tr>
                    <td colspan="4"><div>${uiLabelMap.EcommerceEnterPromoCode}:<input id="productPromoCode" name="productPromoCode" size="22" type="text" value=""/></div></td>
                    <td><div id="cartDiscount" align="right"><b>${uiLabelMap.ProductDiscount}:</b></div></td>
                    <td>
                      <div id="cartDiscountValue" align="center">
                        <#assign orderAdjustmentsTotal = 0>
                        <#list shoppingCart.getAdjustments() as cartAdjustment>
                          <#assign orderAdjustmentsTotal = orderAdjustmentsTotal + Static["org.ofbiz.order.order.OrderReadHelper"].calcOrderAdjustment(cartAdjustment, shoppingCart.getSubTotal())>
                        </#list>
                        <@ofbizCurrency amount=orderAdjustmentsTotal isoCode=shoppingCart.getCurrency()/>
                      </div>
                    </td>
                  </tr>
                  <tr>
                    <td colspan="4"></td>
                    <td><div align="right"><b>${uiLabelMap.OrderShippingAndHandling}:</b></div></td>
                    <td><div id="cartTotalShipping" align="center"><@ofbizCurrency amount=shoppingCart.getTotalShipping() isoCode=shoppingCart.getCurrency()/></div>
                  </tr>
                  <tr>
                    <td colspan="4"></td>
                    <td><div align="right"><b>${uiLabelMap.OrderSalesTax}:</b></div></td>
                    <td><div id="cartTotalSalesTax" align="center"><@ofbizCurrency amount=shoppingCart.getTotalSalesTax() isoCode=shoppingCart.getCurrency()/></div></td>
                  </tr>
                  <tr>
                    <td colspan="4"></td>
                    <td><div align="right"><b>${uiLabelMap.OrderGrandTotal}:</b></div></td>
                    <td><div id="cartDisplayGrandTotal" align="center"><@ofbizCurrency amount=shoppingCart.getDisplayGrandTotal() isoCode=shoppingCart.getCurrency()/></div></td>
                  </tr>
                </tbody>
              </table>
            </form>
            <div align="right">
              <h3><span><a class="buttontext" href="javascript:void(0);" id="updateShoppingCart" >${uiLabelMap.EcommerceContinueToStep} 2</a></span></h3>
              <h3><span><a class="buttontext" style="display: none" href="javascript:void(0);" id="processingShipping">${uiLabelMap.EcommercePleaseWait}....</a></span></h3>
            </div>
          </div>
        </div>

<#-- ========================================================================================================================== -->
        <div id="shippingPanel" class="screenlet">
          <div class="screenlet-header"><div class="boxhead">${uiLabelMap.EcommerceStep} 2: ${uiLabelMap.FacilityShipping}</div></div>
          <div id="shippingSummaryPanel" class="screenlet-body" style="display: none;">
            <div><h3><span><a class="buttontext" href="javascript:void(0);" id="openShippingPanel">${uiLabelMap.EcommerceClickHereToEdit}</a></span></h3></div>
            <div id="shippingCompleted">
              <div align="center" id="openShippingAndPersonlDetail"><h3>${uiLabelMap.FacilityShipping} ${uiLabelMap.EcommerceSummary}</h3></div>
              <table>
                <tbody>
                  <tr>
                    <td valign="top" width="10%">${uiLabelMap.OrderShipTo}:</td>
                    <td valign="top" width="30%">
                      <div>
                        <div id="completedShipToAttn"></div>
                        <div id="completedShippingContactNumber"></div>
                        <div id="completedEmailAddress"></div>
                      </div>
                    </td>
                    <td width="10%" valign="top">${uiLabelMap.EcommerceLocation}:</td>
                    <td width="50%" valign="top">
                      <div>
                        <div id="completedShipToAddress1"></div>
                        <div id="completedShipToAddress2"></div>
                        <div id="completedShipToGeo"></div>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

<#-- ============================================================= -->
          <div id="editShippingPanel" class="screenlet-body" style="display: none;">
            <form name="shippingForm" id="shippingForm" action="<@ofbizUrl>createUpdateShippingAddress</@ofbizUrl>" method="post">
              <input type="hidden" id="shipToContactMechId" name="shipToContactMechId" value="${shipToContactMechId?if_exists}"/>
              <input type="hidden" id="billToContactMechIdInShipingForm" name="billToContactMechId" value="${billToContactMechId?if_exists}"/>
              <input type="hidden" id="shipToPartyId" name="partyId" value="${parameters.partyId?if_exists}"/>
              <input type="hidden" id="shipToPhoneContactMechId" name="shipToPhoneContactMechId" value="${(shipToTelecomNumber.contactMechId)?if_exists}"/>
              <input type="hidden" id="emailContactMechId" name="emailContactMechId" value="${emailContactMechId?if_exists}"/>
              <input type="hidden" name="roleTypeId" value="CUSTOMER"/>
              <input type="hidden" id="shipToPhoneContactMechPurposeTypeId" name="contactMechPurposeTypeId" value="PHONE_SHIPPING"/>
              <#if userLogin?exists>
                <input type="hidden" name="keepAddressBook" value="Y"/>
                <input type="hidden" name="setDefaultShipping" value="Y"/>
                <input type="hidden" name="userLoginId" id="userLoginId" value="${userLogin.userLoginId!}"/>
                <#assign productStoreId = Static["org.ofbiz.product.store.ProductStoreWorker"].getProductStoreId(request)/>
                <input type="hidden" name="productStoreId" value="${productStoreId?if_exists}"/>
              <#else>
                <input type="hidden" name="keepAddressBook" value="N"/>
              </#if>
              <div id="shippingFormServerError" class="errorMessage"></div>
              <table>
                <tr>
                  <td width="40%" valign="top">
                    <div class="form-row">
                      <div class="field-label">
                        <label for="firstName">${uiLabelMap.PartyFirstName}*
                          <span id="advice-required-firstName" style="display: none" class="errorMessage"> (required)</span>
                        </label>
                      </div>
                      <div><input id="firstName" name="firstName" class="required" type="text" value="${firstName?if_exists}"/></div>
                    </div>
                    <div class="form-row">
                      <div class="field-label">
                        <label for="lastName">${uiLabelMap.PartyLastName}*
                          <span id="advice-required-lastName" style="display:none" class="errorMessage"> (required)</span>
                        </label>
                      </div>
                      <div><input id="lastName" name="lastName" class="required" type="text" value="${lastName?if_exists}"/></div>
                    </div>
                    <div class="form-row">
                      <div class="field-label">
                        <label for="countryCode">${uiLabelMap.PartyCountry}*
                          <span id="advice-required-shipToCountryCode" style="display:none" class="errorMessage"> (required)</span>
                        </label>
                        <label for="areaCode">${uiLabelMap.PartyAreaCode}*<span id="advice-required-shipToAreaCode" style="display:none" class="errorMessage"> (required)</span></label>
                        <label for="contactNumber">${uiLabelMap.PartyContactNumber}*<span id="advice-required-shipToContactNumber" style="display:none" class="errorMessage"> (required)</span></label>
                        <label for="extension">${uiLabelMap.PartyExtension}</label>
                      </div>
                      <div>
                      <#if shipToTelecomNumber?has_content>
                        <div>
                          <input name="countryCode" class="required" id="shipToCountryCode" value="${shipToTelecomNumber.countryCode?if_exists}" size="5" maxlength=3> -
                          <input name="areaCode" class="required" id="shipToAreaCode" value="${shipToTelecomNumber.areaCode?if_exists}")}" size="5" maxlength=3> -
                          <input name="contactNumber" class="required" id="shipToContactNumber" value="${shipToTelecomNumber.contactNumber?if_exists}")}" size="10" maxlength=7> -
                          <input name="extension" id="shipToExtension" value="${shipToExtension?if_exists}")}" size="5" maxlength=3>
                        </div>
                      <#else>
                        <div>
                          <input name="countryCode" class="required" id="shipToCountryCode" value="${parameters.countryCode?if_exists}" size="5" maxlength=3> -
                          <input name="areaCode" class="required" id="shipToAreaCode" value="${parameters.areaCode?if_exists}" size="5" maxlength=3> -
                          <input name="contactNumber" class="required" id="shipToContactNumber" value="${parameters.contactNumber?if_exists}" size="10" maxlength=7> -
                          <input name="extension" id="shipToExtension" value="${parameters.extension?if_exists}" size="5" maxlength=3>
                        </div>
                      </#if>
                    </div>
                    <div class="form-row">
                      <div class="field-label">
                        <label for="emailAddress">${uiLabelMap.PartyEmailAddress}*
                          <span id="advice-required-emailAddress" style="display:none" class="errorMessage"> (required)</span>
                        </label>
                      </div>
                      <div>
                        <input id="emailAddress" name="emailAddress" class="required validate-email" maxlength="255" size="40" type="text" value="${emailAddress?if_exists}"/>
                      </div>
                    </div>
                    </td><td width="20%"></td><td>
                    <div class="form-row">
                      <div class="field-label">
                        <label for="shipToAddress1">${uiLabelMap.PartyAddressLine1}*<span id="advice-required-shipToAddress1" class="custom-advice errorMessage" style="display:none"> (required)</span></label>
                      </div>
                      <div>
                        <input id="shipToAddress1" name="shipToAddress1" class="required" type="text" value="${shipToAddress1?if_exists}" maxlength="255" size="40"/>
                      </div>
                    </div>
                    <div class="form-row">
                      <div class="field-label"><label for="address2">${uiLabelMap.PartyAddressLine2}</label></div>
                      <div><input id="shipToAddress2" name="shipToAddress2" type="text" value="${shipToAddress2?if_exists}" maxlength="255" size="40"/></div>
                    </div>
                    <div class="form-row">
                      <div class="field-label">
                        <label for="shipToCity">${uiLabelMap.CommonCity}*<span id="advice-required-shipToCity" class="custom-advice errorMessage" style="display:none"> (required)</span></label>
                      </div>
                      <div><input id="shipToCity" name="shipToCity" class="required" type="text" value="${shipToCity?if_exists}" maxlength="255" size="40"/></div>
                    </div>
                    <div class="form-row">
                      <div class="field-label">
                        <label for="shipToPostalCode">${uiLabelMap.PartyZipCode}*<span id="advice-required-shipToPostalCode" class="custom-advice errorMessage" style="display:none"> (required)</span></label>
                      </div>
                      <div><input id="shipToPostalCode" name="shipToPostalCode" class="required" type="text" value="${shipToPostalCode?if_exists}" size="12" maxlength="10"/></div>
                    </div>
                    <div class="form-row">
                      <div class="field-label">
                        <label for="shipToCountryGeoId">${uiLabelMap.PartyCountry}<span class="requiredLabel">*<span id="advice-required-shipToCountryGeo" style="display:none" class="errorMessage"> (required)</span></label>
                      </div>
                      <div>
                        <select name="countryGeoId" id="shipToCountryGeoId">
                          <#if shipToCountryGeoId??>
                            <option value="${shipToCountryGeoId!}">${shipToCountryProvinceGeo!(shipToCountryGeoId!)}</option>
                          </#if>
                          ${screens.render("component://common/widget/CommonScreens.xml#countries")}
                        </select>
                      </div>
                    </div>
                    <div id="shipToStates" class="form-row">
                      <div class="field-label">
                        <label for="state">${uiLabelMap.CommonState}*<span id="advice-required-shipToStateProvinceGeoId" style="display:none" class="errorMessage">(required)</span></label>
                      </div>
                      <div>
                        <select id="shipToStateProvinceGeoId" name="shipToStateProvinceGeoId">
                          <#if shipToStateProvinceGeoId?has_content>
                            <option value='${shipToStateProvinceGeoId!}'>${shipToStateProvinceGeo!(shipToStateProvinceGeoId!)}</option>
                          <#else>
                            <option value="_NA_">${uiLabelMap.PartyNoState}</option>
                          </#if>
                          ${screens.render("component://common/widget/CommonScreens.xml#states")}
                        </select>
                      </div>
                    </div>
                  </td>
                </tr>
              </table>
            </form>
            <div align="right">
              <h3><span><a class="buttontext" href="javascript:void(0);" id="savePartyAndShippingContact">${uiLabelMap.EcommerceContinueToStep} 3</a></span></h3>
              <h3><span><a class="buttontext" style="display:none" href="javascript:void(0);" id="processingShippingOptions">${uiLabelMap.EcommercePleaseWait}....</a></span></h3>
            </div>
          </div>
        </div>

<#-- ========================================================================================================================== -->
        <div id="shippingOptionPanel" class="screenlet">
          <div class="screenlet-header"><div class="boxhead">${uiLabelMap.EcommerceStep} 3: ${uiLabelMap.PageTitleShippingOptions}</div></div>
          <div id="shippingOptionSummaryPanel" class="screenlet-body" style="display: none;">
            <div><h3><span><a class="buttontext" href="javascript:void(0);" id="openShippingOptionPanel">${uiLabelMap.EcommerceClickHereToEdit}</a></span></h3></div>
            <div class="completed" id="shippingOptionCompleted">
              <div align="center" id="openShippingOption"><h3>${uiLabelMap.FacilityShipping} ${uiLabelMap.ContentSurveyOption} ${uiLabelMap.EcommerceSummary}</h3></div>
              <table cellpadding="0" cellspacing="0">
                <tbody>
                  <tr>
                    <td valign="top">${uiLabelMap.CommonMethod}:&nbsp;</td>
                    <td valign="top"><div id="selectedShipmentOption"></div></td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

<#-- ============================================================= -->
          <div id="editShippingOptionPanel" class="screenlet-body" style="display: none;">
            <form name="shippingOptionForm" id="shippingOptionForm" action="<@ofbizUrl></@ofbizUrl>" method="post">
              <div id="shippingOptionFormServerError" class="errorMessage"></div>
              <table>
                <tr>
                  <td>
                    <div class="form-row">
                      <div class="field-label">
                        <label for="shipmethod">${uiLabelMap.OrderSelectShippingMethod}<span class="requiredLabel">*<span id="advice-required-shipping_method" class="custom-advice" style="display:none" class="errorMessage"> (required)</span></label>
                      </div>
                      <select id="shipMethod" name="shipMethod" class="required"></select>
                    </div>
                  </td>
                </tr>
              </table>
            </form>
            <div align="right">
              <h3><span><a class="buttontext" href="javascript:void(0);" id="saveShippingMethod">${uiLabelMap.EcommerceContinueToStep} 4</a></span></h3>
              <h3><span><a class="buttontext" style="display:none" href="javascript:void(0);" id="processingBilling">${uiLabelMap.EcommercePleaseWait}....</a></span></h3>
            </div>
          </div>
        </div>

<#-- ========================================================================================================================== -->
        <div id="billingPanel" class="screenlet">
          <div class="screenlet-header"><div class="boxhead">${uiLabelMap.EcommerceStep} 4: ${uiLabelMap.AccountingBilling}</div></div>
          <div id="billingSummaryPanel" class="screenlet-body" style="display: none;">
            <div><h3><span><a class="buttontext" href="javascript:void(0);" id="openBillingPanel">${uiLabelMap.EcommerceClickHereToEdit}</a></span></h3></div>
            <div class="completed" id="billingCompleted">
              <div align="center" id="openBillingAndPersonlDetail"><h3>${uiLabelMap.AccountingBilling} ${uiLabelMap.CommonAnd} ${uiLabelMap.AccountingPayment} ${uiLabelMap.EcommerceSummary}</h3></div>
              <table width="35%" align="center">
                <tbody>
                  <tr>
                    <td valign="top" width="5%">${uiLabelMap.OrderBillUpTo}:</td>
                    <td valign="top" width="20%">
                      <div>
                        <div id="completedBillToAttn"></div>
                        <div id="completedBillToPhoneNumber"></div>
                        <div id="completedCCNumber"></div>
                        <div id="completedExpiryDate"></div>
                      </div>
                    </td>
                    <td valign="top" width="5%">${uiLabelMap.EcommerceLocation}:</td>
                    <td valign="top" width="50%">
                      <div>
                        <div id="completedBillToAddress1"></div>
                        <div id="completedBillToAddress2"></div>
                        <div id="completedBillToGeo"></div>
                      </div>
                    </td>
                  </tr>
                  <tr>
                    <td valign="top">${uiLabelMap.AccountingPaymentMethodId}:</td>
                    <td valign="top"><div><div id="paymentMethod"></div></div></td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

<#-- ============================================================= -->

          <div id="editBillingPanel" class="screenlet-body" style="display: none;">
            <form name="billingForm" id="billingForm" class="theform" action="<@ofbizUrl></@ofbizUrl>" method="post">
              <input type="hidden" id ="billToContactMechId" name="billToContactMechId" value="${billToContactMechId?if_exists}"/>
              <input type="hidden" id="shipToContactMechIdInBillingForm" name="shipToContactMechId" value="${shipToContactMechId?if_exists}"/>
              <input type="hidden" id="paymentMethodId" name="paymentMethodId" value="${paymentMethodId?if_exists}"/>
              <input type="hidden" id="paymentMethodTypeId" name="paymentMethodTypeId" value="${paymentMethodTypeId?default("CREDIT_CARD")}"/>
              <input type="hidden" id="billToPartyId" name="partyId" value="${parameters.partyId?if_exists}"/>
              <input type="hidden" name="expireDate" value="${expireDate?if_exists}"/>
              <input type="hidden" name="roleTypeId" value="CUSTOMER"/>
              <input type="hidden" id="billToPhoneContactMechPurposeTypeId" name="contactMechPurposeTypeId" value="PHONE_BILLING"/>
              <input type="hidden" id="billToPhoneContactMechId" name="billToPhoneContactMechId" value="${(billToTelecomNumber.contactMechId)?if_exists}"/>
              <#if userLogin?exists>
                <input type="hidden" name="keepAddressBook" value="Y"/>
                <input type="hidden" name="setDefaultBilling" value="Y"/>
                <#assign productStoreId = Static["org.ofbiz.product.store.ProductStoreWorker"].getProductStoreId(request)/>
                <input type="hidden" name="productStoreId" value="${productStoreId?if_exists}"/>
              <#else>
                <input type="hidden" name="keepAddressBook" value="N"/>
              </#if>
              <div id="billingFormServerError" class="errorMessage"></div>
              <table>
                <tr>
                  <td valign="top">
                    <div class="form-row">
                      <div class="field-label">
                        <label for="cardFirstname">${uiLabelMap.PartyFirstName}*<span id="advice-required-firstNameOnCard" style="display: none;" class="errorMessage"> (required)</span></label>
                      </div>
                      <div>
                        <input id="firstNameOnCard" name="firstNameOnCard" class="required" type="text" value="${firstNameOnCard?if_exists}"/>
                      </div>
                    </div>
                    <div class="form-row">
                      <div class="field-label">
                        <label for="cardLastName">${uiLabelMap.PartyLastName}*<span id="advice-required-lastNameOnCard" style="display: none;" class="errorMessage"> (required)</span></label>
                      </div>
                      <div>
                        <input id="lastNameOnCard" name="lastNameOnCard" class="required" type="text" value="${lastNameOnCard?if_exists}"/>
                      </div>
                    </div>
                    <div class="form-row">
                      <div class="field-label">
                        <label for="cardType">${uiLabelMap.AccountingCardType}*<span id="advice-required-cardType" style="display: none;" class="errorMessage"> (required)</span></label>
                      </div>
                      <div>
                        <select name="cardType" id="cardType">
                          <#if cardType?has_content>
                            <option label="${cardType?if_exists}" value="${cardType?if_exists}">${cardType?if_exists}</option>
                          </#if>
                          ${screens.render("component://common/widget/CommonScreens.xml#cctypes")}
                        </select>
                      </div>
                    </div>
                    <div class="form-row">
                      <div class="field-label">
                        <label for="cardNumber">${uiLabelMap.AccountingCardNumber}*<span id="advice-required-cardNumber" style="display: none;" class="errorMessage"> (required)</span></label>
                      </div>
                      <div>
                        <input id="cardNumber" autocomplete="off" name="cardNumber" class="required" type="text" value="${cardNumber?if_exists}" size=30 maxlength=16/>
                      </div>
                    </div>
                    <div class="form-row">
                      <div class="field-label">
                        <label for="CVV2">CVV2</label>
                      </div>
                      <div>
                        <input id="CVV2" autocomplete="off" name="cardSecurityCode" size="4" type="text" maxlength="4" value=""/>
                      </div>
                    </div>
                    <div class="form-row">
                      <div class="field-label">
                        <label for="expirationdate">${uiLabelMap.AccountingExpirationDate}*<span id="advice-validate-expMonth" class="custom-advice" style="display:none" class="errorMessage"> (required)</span></label>
                      </div>
                    </div>
                    <div class="form-row">
                      <span><label for="expMonth">${uiLabelMap.CommonMonth}:*<span id="advice-required-expMonth" style="display:none" class="errorMessage"> (required)</span></label></span>
                      <span><label for="expYear">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;${uiLabelMap.CommonYear}:*<span id="advice-required-expYear" style="display:none" class="errorMessage"> (required)</span></label></span><br>
                      <span>
                        <select id="expMonth" name="expMonth" class="required">
                          <#if expMonth?has_content>
                            <option label="${expMonth?if_exists}" value="${expMonth?if_exists}">${expMonth?if_exists}</option>
                          </#if>
                          ${screens.render("component://common/widget/CommonScreens.xml#ccmonths")}
                        </select>
                      </span>
                      <span>
                        <select id="expYear" name="expYear" class="required">
                          <#if expYear?has_content>
                            <option value="${expYear?if_exists}">${expYear?if_exists}</option>
                          </#if>
                          ${screens.render("component://common/widget/CommonScreens.xml#ccyears")}
                        </select>
                      </span>
                    </div>
                    <div class="form-row">
                      <div class="field-label">
                        <label for="countryCode">${uiLabelMap.PartyCountry}*
                          <span id="advice-required-billToCountryCode" style="display:none" class="errorMessage"> (required)</span>
                        </label>
                        <label for="areaCode">${uiLabelMap.PartyAreaCode}*<span id="advice-required-billToAreaCode" style="display:none" class="errorMessage"> (required)</span></label>
                        <label for="contactNumber">${uiLabelMap.PartyContactNumber}*<span id="advice-required-billToContactNumber" style="display:none" class="errorMessage"> (required)</span></label>
                        <label for="extension">${uiLabelMap.PartyExtension}</label>
                      </div>
                      <#if billToTelecomNumber?has_content>
                        <div>
                          <input name="countryCode" class="required" id="billToCountryCode" value="${billToTelecomNumber.countryCode?if_exists}" size="5" maxlength=3> -
                          <input name="areaCode" class="required" id="billToAreaCode" value="${billToTelecomNumber.areaCode?if_exists}" size="5" maxlength=3> -
                          <input name="contactNumber" class="required" id="billToContactNumber" value="${billToTelecomNumber.contactNumber?if_exists}" size="10" maxlength=7> -
                          <input name="extension" id="billToExtension" value="${billToExtension?if_exists}" size="5" maxlength=3>
                        </div>
                      <#else>
                        <div>
                          <input name="countryCode" class="required" id="billToCountryCode" value="${parameters.countryCode?if_exists}" size="5" maxlength=3> -
                          <input name="areaCode" class="required" id="billToAreaCode" value="${parameters.areaCode?if_exists}" size="5" maxlength=3> -
                          <input name="contactNumber" class="required" id="billToContactNumber" value="${parameters.contactNumber?if_exists}" size="10" maxlength=7> -
                          <input name="extension" id="billToExtension" value="${parameters.extension?if_exists}" size="5" maxlength=3>
                        </div>
                      </#if>
                    </div>
                  </td>
                  <td width="20%">&nbsp;</td>
                  <td valign="top">
                    <div class="form-row">
                      <div><input class="checkbox" id="useShippingAddressForBilling" name="useShippingAddressForBilling" type="checkbox" value="Y" <#if useShippingAddressForBilling?has_content && useShippingAddressForBilling?default("")=="Y">checked</#if>>${uiLabelMap.FacilityBillingAddressSameShipping}</div>
                    </div>
                    <div id="billingAddress" <#if useShippingAddressForBilling?has_content && useShippingAddressForBilling?default("")=="Y">style="display:none"</#if>>
                      <div class="form-row">
                        <div class="field-label">
                          <label for="address1">${uiLabelMap.PartyAddressLine1}*<span id="advice-required-billToAddress1" style="display:none" class="errorMessage"> (required)</span></label>
                        </div>
                        <div>
                          <input id="billToAddress1" name="billToAddress1" class="required" size=30 type="text" value="${billToAddress1?if_exists}"/>
                        </div>
                      </div>
                      <div class="form-row">
                        <div class="field-label">
                          <label for="address2" style="margin-top: 9px;">${uiLabelMap.PartyAddressLine2}</label>
                        </div>
                        <div>
                          <input id="billToAddress2" name="billToAddress2" type="text" value="${billToAddress2?if_exists}" size=30/>
                        </div>
                      </div>
                      <div class="form-row">
                        <div class="field-label">
                          <label for="city">${uiLabelMap.CommonCity}*<span id="advice-required-billToCity" style="display:none" class="errorMessage"> (required)</span></label>
                        </div>
                        <div>
                          <input id="billToCity" name="billToCity" class="required" type="text" value="${billToCity?if_exists}"/>
                        </div>
                      </div>
                      <div class="form-row">
                        <div class="field-label">
                          <label for="billToPostalCode">${uiLabelMap.PartyZipCode}*<span id="advice-required-billToPostalCode" style="display:none" class="errorMessage"> (required)</span></label>
                        </div>
                        <div>
                          <input id="billToPostalCode" name="billToPostalCode" class="required" type="text" value="${billToPostalCode?if_exists}" size="12" maxlength="10"/>
                        </div>
                      </div>
                      <div class="form-row">
                        <div class="field-label">
                          <label for="billToCountryGeoId">${uiLabelMap.PartyCountry}*<span id="advice-required-billToCountryGeoId" style="display:none" class="errorMessage"> (required)</span></label>
                        </div>
                        <div>
                          <select name="countryGeoId" id="billToCountryGeoId">
                            <#if billToCountryGeoId??>
                              <option value='${billToCountryGeoId!}'>${billToCountryProvinceGeo!(billToCountryGeoId!)}</option>
                            </#if>
                            ${screens.render("component://common/widget/CommonScreens.xml#countries")}
                          </select>
                        </div>
                      </div>
                      <div id="billToStates" class="form-row">
                        <div class="field-label">
                          <label for="state">${uiLabelMap.CommonState}*<span id="advice-required-billToStateProvinceGeoId" style="display:none" class="errorMessage"> (required)</span></label>
                        </div>
                        <div>
                          <select id="billToStateProvinceGeoId" name="billToStateProvinceGeoId">
                            <#if billToStateProvinceGeoId?has_content>
                              <option value='${billToStateProvinceGeoId!}'>${billToStateProvinceGeo!(billToStateProvinceGeoId!)}</option>
                            <#else>
                              <option value="_NA_">${uiLabelMap.PartyNoState}</option>
                            </#if>
                          </select>
                        </div>
                      </div>
                    </div>
                  </td>
                </tr>
              </table>
            </form>
            <div align="right">
              <h3><span><a class="buttontext" href="javascript:void(0);" id="savePaymentAndBillingContact">${uiLabelMap.EcommerceContinueToStep} 5</a></span></h3>
              <h3><span><a class="buttontext" href="javascript:void(0);" style="display: none;" id="processingOrderSubmitPanel">${uiLabelMap.EcommercePleaseWait}....</a></span></h3>
            </div>
          </div>
        </div>

<#-- ========================================================================================================================== -->
        <div class="screenlet">
          <div class="screenlet-header"><div class="boxhead">${uiLabelMap.EcommerceStep} 5: ${uiLabelMap.OrderSubmitOrder}</div></div>
          <div id="orderSubmitPanel" style="display: none;">
            <form name="orderSubmitForm" id="orderSubmitForm" action="<@ofbizUrl>onePageProcessOrder</@ofbizUrl>" method="post">
              <div align="right">
                <input type="button" id="processOrderButton" name="processOrderButton" value="${uiLabelMap.OrderSubmitOrder}" class="mediumSubmit">
                <input type="button" style="display: none;" id="processingOrderButton" name="processingOrderButton" value="${uiLabelMap.OrderSubmittingOrder}" class="mediumSubmit">
              </div>
            </form>
          </div>
        </div>
      </div>
    </#if>

<#-- ========================================================================================================================== -->
    <div id="emptyCartCheckoutPanel" align="center" <#if shoppingCart?has_content && shoppingCart.size() gt 0> style="display: none;"</#if>>
      <div>
        <div class="screenlet-header"><div class="boxhead">${uiLabelMap.EcommerceStep} 1: ${uiLabelMap.PageTitleShoppingCart}</div></div><br/>
        <div>You currently have no items in your cart. Click <a href="<@ofbizUrl>main</@ofbizUrl>">here</a> to view our products.</div>
      </div><br/>
      <div>
        <div class="screenlet-header"><div class="boxhead">${uiLabelMap.EcommerceStep} 2: ${uiLabelMap.FacilityShipping}</div></div>
      </div><br/>
      <div>
        <div class="screenlet-header"><div class="boxhead">${uiLabelMap.EcommerceStep} 3: ${uiLabelMap.PageTitleShippingOptions}</div></div>
      </div><br/>
      <div>
        <div class="screenlet-header"><div class="boxhead">${uiLabelMap.EcommerceStep} 4: ${uiLabelMap.AccountingBilling}</div></div>
      </div><br/>
      <div>
        <div class="screenlet-header"><div class="boxhead">${uiLabelMap.EcommerceStep} 5: ${uiLabelMap.OrderSubmitOrder}</div></div>
      </div>
    </div>
  </div>
</div>