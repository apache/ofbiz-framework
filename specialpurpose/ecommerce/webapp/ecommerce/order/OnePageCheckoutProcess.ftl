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

<div>
  <#assign shoppingCart = sessionAttributes.shoppingCart?if_exists>
  <h1>${uiLabelMap.OrderCheckout}</h1>
    <#if shoppingCart?has_content && shoppingCart.size() gt 0>
      <div id="checkoutPanel">

<#-- ========================================================================================================================== -->
        <div id="cartPanel" class="screenlet">
          <h3>${uiLabelMap.EcommerceStep} 1: ${uiLabelMap.PageTitleShoppingCart}</h3>
          <div id="cartSummaryPanel" style="display: none;">
            <div class="buttons"><a href="javascript:void(0);" id="openCartPanel">${uiLabelMap.EcommerceClickHereToEdit}</a></div>
            <table id="cartSummaryPanel_cartItems">
              <thead>
                <tr>
                  <th>${uiLabelMap.OrderItem}</th>
                  <th>${uiLabelMap.CommonDescription}</th>
                  <th>${uiLabelMap.EcommerceUnitPrice}</th>
                  <th>${uiLabelMap.OrderQuantity}</th>
                  <th>${uiLabelMap.EcommerceAdjustments}</th>
                  <th>${uiLabelMap.EcommerceItemTotal}</th>
                </tr>
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
                    <td><img src="<@ofbizContentUrl>${requestAttributes.contentPathPrefix?if_exists}${smallImageUrl}</@ofbizContentUrl>" alt = "Product Image" /></td>
                    <td>${cartLine.getName()?if_exists}</td>
                    <td>${cartLine.getDisplayPrice()}</td>
                    <td><span id="completedCartItemQty_${cartLineIndex}">${cartLine.getQuantity()?string.number}</span></td>
                    <td><span id="completedCartItemAdjustment_${cartLineIndex}"><@ofbizCurrency amount=cartLine.getOtherAdjustments() isoCode=shoppingCart.getCurrency()/></span></td>
                    <td align="right"><span id="completedCartItemSubTotal_${cartLineIndex}"><@ofbizCurrency amount=cartLine.getDisplayItemSubTotal() isoCode=shoppingCart.getCurrency()/></span></td>
                  </tr>
                  <#assign itemCount = itemCount + 1>
                </#list>
              </tbody>
            </table>
            <table id="cartSummaryPanel_cartTotals">
              <tbody>
                <tr id="completedCartSubtotalRow">
                  <th scope="row">${uiLabelMap.CommonSubtotal}</th>
                  <td><div id="completedCartSubTotal"><@ofbizCurrency amount=shoppingCart.getSubTotal() isoCode=shoppingCart.getCurrency()/></div></td>
                </tr>
                <#assign orderAdjustmentsTotal = 0>
                <#list shoppingCart.getAdjustments() as cartAdjustment>
                  <#assign orderAdjustmentsTotal = orderAdjustmentsTotal + Static["org.ofbiz.order.order.OrderReadHelper"].calcOrderAdjustment(cartAdjustment, shoppingCart.getSubTotal())>
                </#list>
                <tr id="completedCartDiscountRow">
                  <th scope="row">${uiLabelMap.ProductDiscount}</th>
                  <td><input type="hidden" value="${orderAdjustmentsTotal}" id="initializedCompletedCartDiscount"/><div id="completedCartDiscount"><@ofbizCurrency amount=orderAdjustmentsTotal isoCode=shoppingCart.getCurrency()/></div></td>
                </tr>
                <tr>
                  <th scope="row">${uiLabelMap.OrderShippingAndHandling}</th>
                  <td><div id="completedCartTotalShipping"><@ofbizCurrency amount=shoppingCart.getTotalShipping() isoCode=shoppingCart.getCurrency()/></div></td>
                </tr>
                <tr>
                  <th scope="row">${uiLabelMap.OrderSalesTax}</th>
                  <td><div id="completedCartTotalSalesTax"><@ofbizCurrency amount=shoppingCart.getTotalSalesTax() isoCode=shoppingCart.getCurrency()/></div></td>
                </tr>
                <tr>
                  <th scope="row">${uiLabelMap.OrderGrandTotal}</th>
                  <td><div id="completedCartDisplayGrandTotal"><@ofbizCurrency amount=shoppingCart.getDisplayGrandTotal() isoCode=shoppingCart.getCurrency()/></div></td>
                </tr>
              </tbody>
            </table>
          </div>

<#-- ============================================================= -->
          <div id="editCartPanel">
            <form id="cartForm" method="post" action="<@ofbizUrl></@ofbizUrl>">
                <fieldset>
                  <input type="hidden" name="removeSelected" value="false" />
                  <div id="cartFormServerError" class="errorMessage"></div>
                  <table id="editCartPanel_cartItems">
                    <thead>
                      <tr>
                        <th>${uiLabelMap.OrderItem}</th>
                        <th>${uiLabelMap.CommonDescription}</th>
                        <th>${uiLabelMap.EcommerceUnitPrice}</th>
                        <th>${uiLabelMap.OrderQuantity}</th>
                        <th>${uiLabelMap.EcommerceAdjustments}</th>
                        <th>${uiLabelMap.EcommerceItemTotal}</th>
                        <th>${uiLabelMap.FormFieldTitle_removeButton}</th>
                      </tr>
                    </thead>
                    <tbody id="updateBody">
                      <#assign itemCount = 0>
                      <#list shoppingCart.items() as cartLine>
                        <#assign cartLineIndex = itemCount>
                        <#assign productId = cartLineIndex>
                        <tr id="cartItemRow_${cartLineIndex}">
                          <td>
                            <#if cartLine.getProductId()?exists>
                              <#if cartLine.getParentProductId()?exists>
                                <#assign parentProductId = cartLine.getParentProductId()/>
                              <#else>
                                <#assign parentProductId = cartLine.getProductId()/>
                              </#if>
                              <#assign smallImageUrl = Static["org.ofbiz.product.product.ProductContentWrapper"].getProductContentAsText(cartLine.getProduct(), "SMALL_IMAGE_URL", locale, dispatcher)?if_exists>
                              <#if !smallImageUrl?string?has_content><#assign smallImageUrl = ""></#if>
                              <#if smallImageUrl?string?has_content>
                                <img src="<@ofbizContentUrl>${requestAttributes.contentPathPrefix?if_exists}${smallImageUrl}</@ofbizContentUrl>" alt="Product Image" />
                              </#if>
                            </#if>
                          </td>
                          <td>${cartLine.getName()?if_exists}</td>
                          <td><div id="itemUnitPrice_${cartLineIndex}"><@ofbizCurrency amount=cartLine.getDisplayPrice() isoCode=shoppingCart.getCurrency()/></div></td>
                          <td>
                            <#if cartLine.getIsPromo()>
                              ${cartLine.getQuantity()?string.number}
                            <#else>
                              <input type="hidden" name="cartLineProductId" id="cartLineProductId_${cartLineIndex}" value="${cartLine.getProductId()}" />
                              <input type="text" name="update${cartLineIndex}" id="qty_${cartLineIndex}" value="${cartLine.getQuantity()?string.number}" class="required validate-number" />
                              <span id="advice-required-qty_${cartLineIndex}" style="display:none;" class="errorMessage"> (required)</span>
                            </#if>
                          </td>
                          <#if !cartLine.getIsPromo()>
                            <td><div id="addPromoCode_${cartLineIndex}"><@ofbizCurrency amount=cartLine.getOtherAdjustments() isoCode=shoppingCart.getCurrency()/></div></td>
                          <#else>
                            <td><@ofbizCurrency amount=cartLine.getOtherAdjustments() isoCode=shoppingCart.getCurrency()/></td>
                          </#if>
                          <td><div id="displayItem_${cartLineIndex}"><@ofbizCurrency amount=cartLine.getDisplayItemSubTotal() isoCode=shoppingCart.getCurrency()/></div></td>
                          <#if !cartLine.getIsPromo()>
                            <td><a href="javascript:void(0);"><img id="remove_${cartLineIndex?if_exists}" src="<@ofbizContentUrl>/ecommerce/images/remove.png</@ofbizContentUrl>" alt="Remove Item Image"/></a></td>
                          </#if>
                        </tr>
                        <#assign itemCount = itemCount + 1>
                      </#list>
                    </tbody>
                  </table>
                  <table id="editCartPanel_cartTotals">
                    <tbody>
                      <tr>
                        <th scope="row">${uiLabelMap.CommonSubtotal}</th>
                        <td><div id="cartSubTotal"><@ofbizCurrency amount=shoppingCart.getSubTotal() isoCode=shoppingCart.getCurrency()/></div></td>
                      </tr>
                      <tr>
                        <th scope="row">${uiLabelMap.ProductDiscount}</th>
                        <td>
                          <div id="cartDiscountValue">
                            <#assign orderAdjustmentsTotal = 0>
                            <#list shoppingCart.getAdjustments() as cartAdjustment>
                              <#assign orderAdjustmentsTotal = orderAdjustmentsTotal + Static["org.ofbiz.order.order.OrderReadHelper"].calcOrderAdjustment(cartAdjustment, shoppingCart.getSubTotal())>
                            </#list>
                            <@ofbizCurrency amount=orderAdjustmentsTotal isoCode=shoppingCart.getCurrency()/>
                          </div>
                        </td>
                      </tr>
                      <tr>
                        <th scope="row">${uiLabelMap.OrderShippingAndHandling}</th>
                        <td><div id="cartTotalShipping"><@ofbizCurrency amount=shoppingCart.getTotalShipping() isoCode=shoppingCart.getCurrency()/></div></td>
                      </tr>
                      <tr>
                        <th scope="row">${uiLabelMap.OrderSalesTax}</th>
                        <td><div id="cartTotalSalesTax"><@ofbizCurrency amount=shoppingCart.getTotalSalesTax() isoCode=shoppingCart.getCurrency()/></div></td>
                      </tr>
                      <tr>
                        <th scope="row">${uiLabelMap.OrderGrandTotal}</th>
                        <td><div id="cartDisplayGrandTotal"><@ofbizCurrency amount=shoppingCart.getDisplayGrandTotal() isoCode=shoppingCart.getCurrency()/></div></td>
                      </tr>
                    </tbody>
                  </table>
                  </fieldset>
                  <fieldset id="productPromoCodeFields">
                    <legend></legend>
                    <div>
                      <label for="productPromoCode">${uiLabelMap.EcommerceEnterPromoCode}</label>
                      <input id="productPromoCode" name="productPromoCode" type="text" value=""/>
                    </div>
                  </fieldset>
            </form>
            <div class="buttons">
              <a href="javascript:void(0);" id="updateShoppingCart" >${uiLabelMap.EcommerceContinueToStep} 2</a>
              <a style="display: none" href="javascript:void(0);" id="processingShipping">${uiLabelMap.EcommercePleaseWait}....</a>
            </div>
          </div>
        </div>

<#-- ========================================================================================================================== -->
        <div id="shippingPanel" class="screenlet">
          <h3>${uiLabelMap.EcommerceStep} 2: ${uiLabelMap.FacilityShipping}</h3>
          <div id="shippingSummaryPanel" style="display: none;">
            <div class="buttons"><a href="javascript:void(0);" id="openShippingPanel">${uiLabelMap.EcommerceClickHereToEdit}</a></div>
            <div id="shippingCompleted">
              <h3 id="openShippingAndPersonlDetail">${uiLabelMap.FacilityShipping} ${uiLabelMap.EcommerceSummary}</h3>
              <ul>
                <li>
                  <h4>${uiLabelMap.OrderShipTo}</h4>
                  <ul>
                    <li id="completedShipToAttn"></li>
                    <li id="completedShippingContactNumber"></li>
                    <li id="completedEmailAddress"></li>
                  </ul>
                </li>
                <li>
                  <h4>${uiLabelMap.EcommerceLocation}</h4>
                  <ul>
                    <li id="completedShipToAddress1"></li>
                    <li id="completedShipToAddress2"></li>
                    <li id="completedShipToGeo"></li>
                  </ul>
                </li>
              </ul>
            </div>
          </div>

<#-- ============================================================= -->
          <div id="editShippingPanel" style="display: none;">
            <form id="shippingForm" action="<@ofbizUrl>createUpdateShippingAddress</@ofbizUrl>" method="post">
                <fieldset>
                  <input type="hidden" id="shipToContactMechId" name="shipToContactMechId" value="${shipToContactMechId?if_exists}"/>
                  <input type="hidden" id="billToContactMechIdInShipingForm" name="billToContactMechId" value="${billToContactMechId?if_exists}"/>
                  <input type="hidden" id="shipToPartyId" name="partyId" value="${partyId?if_exists}"/>
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
              </fieldset>
                  <div id="shippingFormServerError" class="errorMessage"></div>
                  <fieldset id="shipToName">
                      <div>
                        <label for="firstName">${uiLabelMap.PartyFirstName}*
                          <span id="advice-required-firstName" style="display: none" class="errorMessage"> (required)</span>
                        </label>
                        <input id="firstName" name="firstName" class="required" type="text" value="${firstName?if_exists}"/>
                      </div>
                      <div>
                        <label for="lastName">${uiLabelMap.PartyLastName}*
                          <span id="advice-required-lastName" style="display:none" class="errorMessage"> (required)</span>
                        </label>
                        <input id="lastName" name="lastName" class="required" type="text" value="${lastName?if_exists}"/>
                      </div>
                  </fieldset>
                  <div>
                  <#if shipToTelecomNumber?has_content>
                      <span>
                          <label for="shipToCountryCode">${uiLabelMap.PartyCountry}*
                              <span id="advice-required-shipToCountryCode" style="display:none" class="errorMessage"> (required)</span>
                          </label>    
                          <input name="countryCode" class="required" id="shipToCountryCode" value="${shipToTelecomNumber.countryCode?if_exists}" size="5" maxlength="3" /> -
                      </span>
                      <span>
                          <label for="shipToAreaCode">${uiLabelMap.PartyAreaCode}*
                              <span id="advice-required-shipToAreaCode" style="display:none" class="errorMessage"> (required)</span>
                          </label>
                          <input name="areaCode" class="required" id="shipToAreaCode" value="${shipToTelecomNumber.areaCode?if_exists}" size="5" maxlength="3" /> -
                      </span>
                      <span>
                          <label for="shipToContactNumber">${uiLabelMap.PartyContactNumber}*
                              <span id="advice-required-shipToContactNumber" style="display:none" class="errorMessage"> (required)</span>
                          </label>
                          <input name="contactNumber" class="required" id="shipToContactNumber" value="${shipToTelecomNumber.contactNumber?if_exists}" size="10" maxlength="7" /> -
                      </span>
                      <span>
                          <label for="shipToExtension">${uiLabelMap.PartyExtension}</label>
                          <input name="extension" id="shipToExtension" value="${shipToExtension?if_exists}" size="5" maxlength="3" />
                      </span>
                  <#else>
                      <span>
                          <label for="shipToCountryCode">${uiLabelMap.PartyCountry}*
                              <span id="advice-required-shipToCountryCode" style="display:none" class="errorMessage"> (required)</span>
                          </label>
                          <input name="countryCode" class="required" id="shipToCountryCode" value="${parameters.countryCode?if_exists}" size="5" maxlength="3" /> -
                      </span>
                      <span>
                          <label for="shipToAreaCode">${uiLabelMap.PartyAreaCode}*
                              <span id="advice-required-shipToAreaCode" style="display:none" class="errorMessage"> (required)</span>
                          </label>
                          <input name="areaCode" class="required" id="shipToAreaCode" value="${parameters.areaCode?if_exists}" size="5" maxlength="3" /> -
                      </span>
                      <span>
                          <label for="shipToContactNumber">${uiLabelMap.PartyContactNumber}*
                              <span id="advice-required-shipToContactNumber" style="display:none" class="errorMessage"> (required)</span>
                          </label>
                          <input name="contactNumber" class="required" id="shipToContactNumber" value="${parameters.contactNumber?if_exists}" size="10" maxlength="7" /> -
                      </span>
                      <span>
                          <label for="shipToExtension">${uiLabelMap.PartyExtension}</label>
                          <input name="extension" id="shipToExtension" value="${parameters.extension?if_exists}" size="5" maxlength="3" />
                      </span>
                  </#if>
                  </div>
                  <div>
                      <span>
                          <label for="emailAddress">${uiLabelMap.PartyEmailAddress}*
                            <span id="advice-required-emailAddress" style="display:none" class="errorMessage"> (required)</span>
                          </label>
                          <input id="emailAddress" name="emailAddress" class="required validate-email" maxlength="255" size="40" type="text" value="${emailAddress?if_exists}"/>
                      </span>
                  </div>
                    <div>
                        <span>
                            <label for="shipToAddress1">${uiLabelMap.PartyAddressLine1}*
                                <span id="advice-required-shipToAddress1" class="custom-advice errorMessage" style="display:none"> (required)</span>
                            </label>
                            <input id="shipToAddress1" name="shipToAddress1" class="required" type="text" value="${shipToAddress1?if_exists}" maxlength="255" size="40"/>
                        </span>
                    </div>
                    <div>
                        <span>
                          <label for="shipToAddress2">${uiLabelMap.PartyAddressLine2}</label>
                          <input id="shipToAddress2" name="shipToAddress2" type="text" value="${shipToAddress2?if_exists}" maxlength="255" size="40"/>
                        </span>
                    </div>
                    <div>
                        <span>
                            <label for="shipToCity">${uiLabelMap.CommonCity}*
                                <span id="advice-required-shipToCity" class="custom-advice errorMessage" style="display:none"> (required)</span>
                            </label>
                            <input id="shipToCity" name="shipToCity" class="required" type="text" value="${shipToCity?if_exists}" maxlength="255" size="40"/>
                        </span>
                    </div>
                    <div>
                        <span>
                            <label for="shipToPostalCode">${uiLabelMap.PartyZipCode}*
                                <span id="advice-required-shipToPostalCode" class="custom-advice errorMessage" style="display:none"> (required)</span>
                            </label>
                            <input id="shipToPostalCode" name="shipToPostalCode" class="required" type="text" value="${shipToPostalCode?if_exists}" size="12" maxlength="10"/>
                        </span>
                    </div>
                    <div>
                        <span>
                            <label for="shipToCountryGeoId">${uiLabelMap.PartyCountry}*
                                <span id="advice-required-shipToCountryGeo" style="display:none" class="errorMessage"> (required)</span>
                            </label>
                            <select name="countryGeoId" id="shipToCountryGeoId">
                              <#if shipToCountryGeoId??>
                                <option value="${shipToCountryGeoId!}">${shipToCountryProvinceGeo!(shipToCountryGeoId!)}</option>
                              </#if>
                              ${screens.render("component://common/widget/CommonScreens.xml#countries")}
                            </select>
                        </span>
                    </div>
                    <div id="shipToStates">
                        <span>
                            <label for="shipToStateProvinceGeoId">${uiLabelMap.CommonState}*
                                <span id="advice-required-shipToStateProvinceGeoId" style="display:none" class="errorMessage">(required)</span>
                            </label>
                            <select id="shipToStateProvinceGeoId" name="shipToStateProvinceGeoId">
                              <#if shipToStateProvinceGeoId?has_content>
                                <option value='${shipToStateProvinceGeoId!}'>${shipToStateProvinceGeo!(shipToStateProvinceGeoId!)}</option>
                              <#else>
                                <option value="_NA_">${uiLabelMap.PartyNoState}</option>
                              </#if>
                              ${screens.render("component://common/widget/CommonScreens.xml#states")}
                            </select>
                        </span>
                    </div>
            </form>
            <div class="buttons">
              <span><a href="javascript:void(0);" id="savePartyAndShippingContact">${uiLabelMap.EcommerceContinueToStep} 3</a></span>
              <span><a style="display:none" href="javascript:void(0);" id="processingShippingOptions">${uiLabelMap.EcommercePleaseWait}....</a></span>
            </div>
          </div>
        </div>

<#-- ========================================================================================================================== -->
        <div id="shippingOptionPanel" class="screenlet">
          <div class="screenlet-header"><div class="boxhead">${uiLabelMap.EcommerceStep} 3: ${uiLabelMap.PageTitleShippingOptions}</div></div>
          <div id="shippingOptionSummaryPanel" class="screenlet-body" style="display: none;">
            <div class="buttons"><span><a href="javascript:void(0);" id="openShippingOptionPanel">${uiLabelMap.EcommerceClickHereToEdit}</a></span></div>
            <div class="completed" id="shippingOptionCompleted">
              <div id="openShippingOption"><h3>${uiLabelMap.FacilityShipping} ${uiLabelMap.ContentSurveyOption} ${uiLabelMap.EcommerceSummary}</h3></div>
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
            <form id="shippingOptionForm" action="<@ofbizUrl></@ofbizUrl>" method="post">
              <fieldset>
                  <div id="shippingOptionFormServerError" class="errorMessage"></div>
                  <div>
                    <span>
                      <label for="shipMethod">${uiLabelMap.OrderSelectShippingMethod}*
                          <span id="advice-required-shipping_method" class="custom-advice" style="display:none"> (required)</span>
                      </label>
                      <select id="shipMethod" name="shipMethod" class="required">
                          <option value=""></option>
                      </select>
                    </span>
                  </div>
              </fieldset>
            </form>
            <div class="buttons">
              <span><a href="javascript:void(0);" id="saveShippingMethod">${uiLabelMap.EcommerceContinueToStep} 4</a></span>
              <span><a style="display:none" href="javascript:void(0);" id="processingBilling">${uiLabelMap.EcommercePleaseWait}....</a></span>
            </div>
          </div>
        </div>

<#-- ========================================================================================================================== -->
        <div id="billingPanel" class="screenlet">
          <div class="screenlet-header"><div class="boxhead">${uiLabelMap.EcommerceStep} 4: ${uiLabelMap.AccountingBilling}</div></div>
          <div id="billingSummaryPanel" class="screenlet-body" style="display: none;">
            <div class="buttons"><span><a href="javascript:void(0);" id="openBillingPanel">${uiLabelMap.EcommerceClickHereToEdit}</a></span></div>
            <div class="completed" id="billingCompleted">
              <div id="openBillingAndPersonlDetail"><h3>${uiLabelMap.AccountingBilling} ${uiLabelMap.CommonAnd} ${uiLabelMap.AccountingPayment} ${uiLabelMap.EcommerceSummary}</h3></div>
              <table width="35%">
                <tbody>
                  <tr>
                    <td valign="top">${uiLabelMap.OrderBillUpTo}:</td>
                    <td valign="top">
                      <div>
                        <div id="completedBillToAttn"></div>
                        <div id="completedBillToPhoneNumber"></div>
                        <div id="completedCCNumber"></div>
                        <div id="completedExpiryDate"></div>
                      </div>
                    </td>
                    <td valign="top">${uiLabelMap.EcommerceLocation}:</td>
                    <td valign="top">
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
            <form id="billingForm" class="theform" action="<@ofbizUrl></@ofbizUrl>" method="post">
              <fieldset class="col">
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
                        <div>
                            <span>
                                <label for="firstNameOnCard">${uiLabelMap.PartyFirstName}*
                                    <span id="advice-required-firstNameOnCard" style="display: none;" class="errorMessage"> (required)</span>
                                </label>
                                <input id="firstNameOnCard" name="firstNameOnCard" class="required" type="text" value="${firstNameOnCard?if_exists}"/>
                            </span>
                            <span>
                                <label for="lastNameOnCard">${uiLabelMap.PartyLastName}*
                                    <span id="advice-required-lastNameOnCard" style="display: none;" class="errorMessage"> (required)</span>
                                </label>
                                <input id="lastNameOnCard" name="lastNameOnCard" class="required" type="text" value="${lastNameOnCard?if_exists}"/>
                            </span>
                        </div>
                        <div>  
                          <#if billToTelecomNumber?has_content>
                            <span>
                                <label for="billToCountryCode">${uiLabelMap.PartyCountry}*
                                    <span id="advice-required-billToCountryCode" style="display:none" class="errorMessage"> (required)</span>
                                </label>
                                <input name="countryCode" class="required" id="billToCountryCode" value="${billToTelecomNumber.countryCode?if_exists}" size="5" maxlength="3" /> -
                            </span>
                            <span>
                                <label for="billToAreaCode">${uiLabelMap.PartyAreaCode}*
                                    <span id="advice-required-billToAreaCode" style="display:none" class="errorMessage"> (required)</span>
                                </label>
                                <input name="areaCode" class="required" id="billToAreaCode" value="${billToTelecomNumber.areaCode?if_exists}" size="5" maxlength="3" /> -
                            </span>
                            <span>
                                <label for="billToContactNumber">${uiLabelMap.PartyContactNumber}*
                                    <span id="advice-required-billToContactNumber" style="display:none" class="errorMessage"> (required)</span>
                                </label>
                                <input name="contactNumber" class="required" id="billToContactNumber" value="${billToTelecomNumber.contactNumber?if_exists}" size="10" maxlength="7" /> -
                            </span>
                            <span>
                                <label for="billToExtension">${uiLabelMap.PartyExtension}</label>
                                <input name="extension" id="billToExtension" value="${billToExtension?if_exists}" size="5" maxlength="3" />
                            </span>
                          <#else>
                            <span>
                                <label for="billToCountryCode">${uiLabelMap.PartyCountry}*
                                    <span id="advice-required-billToCountryCode" style="display:none" class="errorMessage"> (required)</span>
                                </label>
                                <input name="countryCode" class="required" id="billToCountryCode" value="${parameters.countryCode?if_exists}" size="5" maxlength="3" /> -
                            </span>
                            <span>
                                <label for="billToAreaCode">${uiLabelMap.PartyAreaCode}*
                                    <span id="advice-required-billToAreaCode" style="display:none" class="errorMessage"> (required)</span>
                                </label>
                                <input name="areaCode" class="required" id="billToAreaCode" value="${parameters.areaCode?if_exists}" size="5" maxlength="3" /> -
                            </span>
                            <span>
                                <label for="billToContactNumber">${uiLabelMap.PartyContactNumber}*
                                    <span id="advice-required-billToContactNumber" style="display:none" class="errorMessage"> (required)</span>
                                </label>
                                <input name="contactNumber" class="required" id="billToContactNumber" value="${parameters.contactNumber?if_exists}" size="10" maxlength="7" /> -
                            </span>
                            <span>
                                <label for="billToExtension">${uiLabelMap.PartyExtension}</label>
                                <input name="extension" id="billToExtension" value="${parameters.extension?if_exists}" size="5" maxlength="3" />
                            </span>
                          </#if>
                        </div>
                        <div>
                            <span>
                                <label for="cardType">${uiLabelMap.AccountingCardType}*<span id="advice-required-cardType" style="display: none;" class="errorMessage"> (required)</span></label>
                                <select name="cardType" id="cardType">
                                  <#if cardType?has_content>
                                    <option label="${cardType?if_exists}" value="${cardType?if_exists}">${cardType?if_exists}</option>
                                  </#if>
                                  ${screens.render("component://common/widget/CommonScreens.xml#cctypes")}
                                </select>
                            </span>
                        </div>
                        <div>
                            <span>
                                <label for="cardNumber">${uiLabelMap.AccountingCardNumber}*
                                    <span id="advice-required-cardNumber" style="display: none;" class="errorMessage"> (required)</span>
                                </label>
                                <input id="cardNumber" name="cardNumber" class="required" type="text" value="${cardNumber?if_exists}" size="30" maxlength="16"/>
                            </span>
                            <span>
                                <label for="CVV2">CVV2</label>
                                <input id="CVV2" name="cardSecurityCode" size="4" type="text" maxlength="4" value=""/>
                            </span>
                        </div>
                        <div>
                          <span>
                            <label for="expMonth">${uiLabelMap.CommonMonth}:*
                                <span id="advice-required-expMonth" style="display:none" class="errorMessage"> (required)</span>
                            </label>
                            <select id="expMonth" name="expMonth" class="required">
                              <#if expMonth?has_content>
                                <option label="${expMonth?if_exists}" value="${expMonth?if_exists}">${expMonth?if_exists}</option>
                              </#if>
                              ${screens.render("component://common/widget/CommonScreens.xml#ccmonths")}
                            </select>
                          </span>
                          <span>
                            <label for="expYear">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;${uiLabelMap.CommonYear}:*
                                <span id="advice-required-expYear" style="display:none" class="errorMessage"> (required)</span>
                            </label>
                            <select id="expYear" name="expYear" class="required">
                              <#if expYear?has_content>
                                <option value="${expYear?if_exists}">${expYear?if_exists}</option>
                              </#if>
                              ${screens.render("component://common/widget/CommonScreens.xml#ccyears")}
                            </select>
                          </span>
                        </div>
                    </fieldset>
                    <fieldset class="col">
                        <div>
                            <span>
                                <input class="checkbox" id="useShippingAddressForBilling" name="useShippingAddressForBilling" type="checkbox" value="Y" <#if useShippingAddressForBilling?has_content && useShippingAddressForBilling?default("")=="Y">checked</#if> />${uiLabelMap.FacilityBillingAddressSameShipping}
                            </span>
                        </div>
                        <div id="billingAddress" <#if useShippingAddressForBilling?has_content && useShippingAddressForBilling?default("")=="Y">style="display:none"</#if>>
                          <div>
                            <span>
                              <label for="billToAddress1">${uiLabelMap.PartyAddressLine1}*
                                <span id="advice-required-billToAddress1" style="display:none" class="errorMessage"> (required)</span>
                              </label>
                              <input id="billToAddress1" name="billToAddress1" class="required" size="30" type="text" value="${billToAddress1?if_exists}"/>
                            </span>
                          </div>
                          <div>
                            <span>
                              <label for="billToAddress2" style="margin-top: 9px;">${uiLabelMap.PartyAddressLine2}</label>
                              <input id="billToAddress2" name="billToAddress2" type="text" value="${billToAddress2?if_exists}" size="30"/>
                            </span>
                          </div>
                          <div>
                            <span>
                              <label for="billToCity">${uiLabelMap.CommonCity}*
                                <span id="advice-required-billToCity" style="display:none" class="errorMessage"> (required)</span>
                              </label>
                              <input id="billToCity" name="billToCity" class="required" type="text" value="${billToCity?if_exists}"/>
                            </span>
                          </div>
                          <div>
                            <span>
                              <label for="billToPostalCode">${uiLabelMap.PartyZipCode}*
                                <span id="advice-required-billToPostalCode" style="display:none" class="errorMessage"> (required)</span>
                              </label>
                              <input id="billToPostalCode" name="billToPostalCode" class="required" type="text" value="${billToPostalCode?if_exists}" size="12" maxlength="10"/>
                            </span>
                          </div>
                          <div>
                            <span>
                              <label for="billToCountryGeoId">${uiLabelMap.PartyCountry}*
                                <span id="advice-required-billToCountryGeoId" style="display:none" class="errorMessage"> (required)</span>
                              </label>
                              <select name="countryGeoId" id="billToCountryGeoId">
                                <#if billToCountryGeoId??>
                                  <option value='${billToCountryGeoId!}'>${billToCountryProvinceGeo!(billToCountryGeoId!)}</option>
                                </#if>
                                ${screens.render("component://common/widget/CommonScreens.xml#countries")}
                              </select>
                            </span>
                          </div>
                          <div id="billToStates">
                            <span>
                              <label for="billToStateProvinceGeoId">${uiLabelMap.CommonState}*
                                <span id="advice-required-billToStateProvinceGeoId" style="display:none" class="errorMessage"> (required)</span>
                              </label>
                              <select id="billToStateProvinceGeoId" name="billToStateProvinceGeoId">
                                <#if billToStateProvinceGeoId?has_content>
                                  <option value='${billToStateProvinceGeoId!}'>${billToStateProvinceGeo!(billToStateProvinceGeoId!)}</option>
                                <#else>
                                  <option value="_NA_">${uiLabelMap.PartyNoState}</option>
                                </#if>
                              </select>
                            </span>
                        </div>
                    </div>
              </fieldset>
            </form>
            <div class="buttons">
              <span><a href="javascript:void(0);" id="savePaymentAndBillingContact">${uiLabelMap.EcommerceContinueToStep} 5</a></span>
              <span><a href="javascript:void(0);" style="display: none;" id="processingOrderSubmitPanel">${uiLabelMap.EcommercePleaseWait}....</a></span>
            </div>
          </div>
        </div>

<#-- ========================================================================================================================== -->
        <div class="screenlet">
          <div class="screenlet-header"><div class="boxhead">${uiLabelMap.EcommerceStep} 5: ${uiLabelMap.OrderSubmitOrder}</div></div>
          <div id="orderSubmitPanel" style="display: none;">
            <form id="orderSubmitForm" action="<@ofbizUrl>onePageProcessOrder</@ofbizUrl>" method="post">
              <fieldset>
                  <div class="buttons">
                    <input type="button" id="processOrderButton" name="processOrderButton" value="${uiLabelMap.OrderSubmitOrder}" />
                    <input type="button" style="display: none;" id="processingOrderButton" name="processingOrderButton" value="${uiLabelMap.OrderSubmittingOrder}" />
                  </div>
              </fieldset>
            </form>
          </div>
        </div>
      </div>
    </#if>

<#-- ========================================================================================================================== -->
    <div id="emptyCartCheckoutPanel" <#if shoppingCart?has_content && shoppingCart.size() gt 0> style="display: none;"</#if>>
      <div>
        <div class="screenlet-header"><div class="boxhead">${uiLabelMap.EcommerceStep} 1: ${uiLabelMap.PageTitleShoppingCart}</div></div><br />
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