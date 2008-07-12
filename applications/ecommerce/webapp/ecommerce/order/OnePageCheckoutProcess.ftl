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
          <div id="cartPanel" class="screenlet">
            <div class="screenlet-header"><div class="boxhead" align="left">Step 1: ${uiLabelMap.PageTitleShoppingCart}</div></div>
            <div id="cartSummaryPanel" class="screenlet-body" style="display: none;">
              <div align="left"><h3><span><a class="buttontext" href="javascript:void(0);" id="openCartPanel">Click here to edit</a></span></h3></div>
              <div align="center"><h2>${uiLabelMap.OrderShoppingCart} ${uiLabelMap.EcommerceSummary}</h2></div>
                  <table width="75%" cellspacing="0" cellpadding="1" border="0">
                    <thead>
                      <tr>
                        <td align="left"><div><b>${uiLabelMap.OrderItem}</b></div></td>
                        <td align="left"><div><b>${uiLabelMap.CommonDescription}</b></div></td>
                        <td align="center"><div><b>${uiLabelMap.EcommerceUnitPrice}</b></div></td>
                        <td align="center"><div><b>${uiLabelMap.OrderQuantity}</b></div></td>
                        <td align="center"><div><b>${uiLabelMap.EcommerceAdjustments}</b></div></td>
                        <td align="right"><div><b>${uiLabelMap.EcommerceItemTotal}</b></div></td>
                      </tr>
                      <tr><td colspan="6"><hr class="sepbar"/></td></tr>
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
                          <#if !smallImageUrl?has_content><#assign smallImageUrl = ""></#if>
                        </#if>
                        <tr id="cartItemDisplayRow_${cartLineIndex}">
                          <td align="left"><div><img src="<@ofbizContentUrl>${requestAttributes.contentPathPrefix?if_exists}${smallImageUrl}</@ofbizContentUrl>" align="center" height="20" hspace="0" vspace="0" width="20"></div></td>
                          <td align="left"><div>${cartLine.getName()?if_exists}</div> 
                          <td align="center"><div>@${cartLine.getDisplayPrice()}</div></td>
                          <td align="center"><div><span id="completedCartItemQty_${cartLineIndex}">${cartLine.getQuantity()?string.number}</span></div></td>
                          <td align="center"><div><span id="completedCartItemAdjustment_${cartLineIndex}">${cartLine.getOtherAdjustments()?string.number}</span></div></td>
                          <td align="right"><div id="completedCartItemSubTotal_${cartLineIndex}"><@ofbizCurrency amount=cartLine.getDisplayItemSubTotalNoAdj() isoCode=shoppingCart.getCurrency()/></div></td>
                        </tr>
                        <tr><td colspan="6"><hr class="sepbar"/></td></tr>
                        <#assign itemCount = itemCount + 1>
                      </#list>
                      <tr id="completedCartSubtotalRow">
                        <td colspan="4"></td>
                        <td><div align="right"><b>${uiLabelMap.CommonSubtotal}:</b></div></td>
                        <#assign initializedSubTotal = shoppingCart.getDisplaySubTotal() - shoppingCart.getProductPromoTotal()>
                        <td><div id="completedCartSubTotal" align="right"><@ofbizCurrency amount=initializedSubTotal isoCode=shoppingCart.getCurrency()/></div></td>
                      </tr>
                      <tr id="completedCartDiscountRow">
                        <input type="hidden" value="<b>${shoppingCart.getProductPromoTotal()}</b>" id="initializedCompletedCartDiscount"/>
                        <td colspan="4"></td>
                        <td><div align="right"><b>${uiLabelMap.ProductDiscount}:</b></div></td>
                        <td><div id="completedCartDiscount" align="right"><@ofbizCurrency amount=shoppingCart.getProductPromoTotal() isoCode=shoppingCart.getCurrency()/></div></td>
                      </tr>
                      <tr>
                        <td colspan="4"></td>
                        <td><div align="right"><b>${uiLabelMap.OrderShippingAndHandling}:</b></div></td>
                        <td>
                          <div id="completedCartTotalShipping" align="right">
                            <#if (shoppingCart.getTotalShipping() > 0.0)>
                              <@ofbizCurrency amount=shoppingCart.getTotalShipping() isoCode=shoppingCart.getCurrency()/>
                            <#else>$0.00
                            </#if>
                          </div>
                        </td>
                      </tr>
                      <tr>
                        <td colspan="4"></td>
                        <td><div align="right"><b>${uiLabelMap.OrderSalesTax}:</b></div></td>
                        <td>
                          <div id="completedCartTotalSalesTax" align="right">
                            <#if (shoppingCart.getTotalSalesTax() > 0.0)>
                              <@ofbizCurrency amount=shoppingCart.getTotalSalesTax() isoCode=shoppingCart.getCurrency()/>
                            <#else>$0.00
                            </#if>
                          </div>
                        </td>
                      </tr>
                      <tr>
                        <td colspan="4"></td>
                        <td><div align="right"><b>${uiLabelMap.OrderGrandTotal}:</b></div></td>
                        <td><div id="completedCartDisplayGrandTotal" align="right"><@ofbizCurrency amount=shoppingCart.getDisplayGrandTotal() isoCode=shoppingCart.getCurrency()/></div></td>
                      </tr>
                    </tbody>
                  </table>
            </div>
            <div id="editCartPanel" class="screenlet-body">
              <form name="cartForm" id="cartForm" method="post" action="<@ofbizUrl></@ofbizUrl>">
                  <input type="hidden" name="removeSelected" value="false">
                  <div id="cartFormServerError"></div>
                      <table width="75%" cellspacing="0" cellpadding="1" border="0">
                        <thead>
                          <tr>
                            <td align="left"><div><b>${uiLabelMap.OrderItem}</b></div></td>
                            <td align="left"><div><b>${uiLabelMap.CommonDescription}</b></div></td>
                            <td align="center"><div><b>${uiLabelMap.EcommerceUnitPrice}</b></div></td>
                            <td align="center"><div><b>${uiLabelMap.OrderQuantity}</b></div></td>
                            <td align="center"><div><b>${uiLabelMap.EcommerceAdjustments}</b></div></td>
                            <td align="center"><div><b>${uiLabelMap.EcommerceItemTotal}</b></div></td>
                            <td align="right"><div><b>${uiLabelMap.FormFieldTitle_removeButton}</b></div></td>
                          </tr>
                          <tr><td colspan="7"><hr class="sepbar"/></td></tr>
                        </thead>
                        <tbody id="updateBody">
                          <#assign itemCount = 0>
                          <#list shoppingCart.items() as cartLine>
                            <#assign cartLineIndex = itemCount>
                            <#assign productId = cartLineIndex>
                            <tr id="cartItemRow_${cartLineIndex}">
                              <div id="updateArea">
                                <td style="padding: 1px;" align="left" valign="top">
                                  <#if cartLine.getProductId()?exists>
                                    <#if cartLine.getParentProductId()?exists>
                                      <#assign parentProductId = cartLine.getParentProductId()/>
                                    <#else>
                                      <#assign parentProductId = cartLine.getProductId()/>
                                    </#if>
                                    <#assign smallImageUrl = Static["org.ofbiz.product.product.ProductContentWrapper"].getProductContentAsText(cartLine.getProduct(), "SMALL_IMAGE_URL", locale, dispatcher)?if_exists>
                                    <#if !smallImageUrl?has_content><#assign smallImageUrl = ""></#if>
                                    <#if smallImageUrl?has_content>
                                      <img src="<@ofbizContentUrl>${requestAttributes.contentPathPrefix?if_exists}${smallImageUrl}</@ofbizContentUrl>" border="0" height="50" hspace="0" vspace="0" width="50"/>
                                    </#if>
                                  </#if>
                                </td>
                                <td align="left"><div>${cartLine.getName()?if_exists}</div></td>
                                <td align="center"><div id="itemUnitPrice_${cartLineIndex}">
                                  <@ofbizCurrency amount=cartLine.getDisplayPrice() isoCode=shoppingCart.getCurrency() rounding=2/></div>
                                </td>
                                <td align="center">
                                  <#if cartLine.getIsPromo()>
                                    ${cartLine.getQuantity()?string.number}
                                  <#else>
                                    <input type="hidden" name="cartLineProductId" id="cartLineProductId_${cartLineIndex}" value="${cartLine.getProductId()}">
                                    <div>
                                      <label for="qty_${cartLineIndex}">
                                        <span id="advice-required-qty_${cartLineIndex}" style="display:none;">Quantity required.</span>
                                      </label>
                                      <span>
                                        <input type="text" name="update_${cartLineIndex}" id="qty_${cartLineIndex}" value="${cartLine.getQuantity()?string.number}" size="6" class="inputBox required validate-number"><span></span>
                                      </span>
                                    </div>
                                  </#if>
                                </td>
                                <#if !cartLine.getIsPromo()>
                                  <td nowrap align="center"><div id="addPromoCode_${cartLineIndex}" class="tabletext"><@ofbizCurrency amount=cartLine.getOtherAdjustments() isoCode=shoppingCart.getCurrency()/></div></td>
                                <#else>
                                  <td nowrap align="center"><div class="tabletext"><@ofbizCurrency amount=cartLine.getOtherAdjustments() isoCode=shoppingCart.getCurrency()/></div></td>
                                </#if>                                
                                <#if cartLine.getIsPromo()>
                                  <td align="center">FREE</td>
                                <#else>
                                  <td align="center"><div id="displayItem_${cartLineIndex}"><@ofbizCurrency amount=cartLine.getDisplayItemSubTotalNoAdj() isoCode=shoppingCart.getCurrency()/></div></td>
                                </#if>
                                <#if !cartLine.getIsPromo()>
                                  <td align="right"><a href="javascript:void(0);"><img id="remove_${cartLineIndex?if_exists}" src="<@ofbizContentUrl>/ecommerce/images/remove.png</@ofbizContentUrl>" border="0" height="30" hspace="0" vspace="0" width="40"></a></td>
                                </#if>
                              </div>
                            </tr>
                            <tr><td colspan="7"><hr class="sepbar"/></td></tr>
                            <#assign itemCount = itemCount + 1>
                          </#list>                      
                              <tr>
                                <td colspan="4"></td>
                                <td><div align="right"><b>${uiLabelMap.CommonSubtotal}:</b></div></td>
                                <#assign initializedSubTotal = shoppingCart.getDisplaySubTotal() - shoppingCart.getProductPromoTotal()>
                                <td><div id="cartSubTotal" align="center"><@ofbizCurrency amount=initializedSubTotal isoCode=shoppingCart.getCurrency()/></div></td>
                              </tr>
                              <tr>
                                <td colspan="4">
                                  <div>${uiLabelMap.EcommerceEnterPromoCode}:
                                    <input id="productPromoCode" class="inputBox" name="productPromoCode" size="22" type="text" value=""/>
                                  </div>
                                </td>
                                <td><div id="cartDiscount" align="right"><b>${uiLabelMap.ProductDiscount}:</b></div></td>
                                <td>
                                  <div id="cartDiscountValue" align="center"><@ofbizCurrency amount=shoppingCart.getProductPromoTotal() isoCode=shoppingCart.getCurrency()/></div>
                                </td>
                              </tr>
                              <tr>
                                <td colspan="4"></td>
                                <td><div align="right"><b>${uiLabelMap.OrderShippingAndHandling}:</b></div></td>
                                <td>
                                  <div id="cartTotalShipping" align="center">
                                    <#if (shoppingCart.getTotalShipping() > 0.0)>
                                      <@ofbizCurrency amount=shoppingCart.getTotalShipping() isoCode=shoppingCart.getCurrency()/>
                                    <#else>$0.00
                                    </#if>
                                  </div>
                                </td>
                              </tr>
                              <tr>
                                <td colspan="4"></td>
                                <td><div align="right"><b>${uiLabelMap.OrderSalesTax}:</b></div></td>
                                <td>
                                  <div id="cartTotalSalesTax" align="center">
                                    <#if (shoppingCart.getTotalSalesTax() > 0.0)>
                                      <@ofbizCurrency amount=shoppingCart.getTotalSalesTax() isoCode=shoppingCart.getCurrency()/>
                                    <#else>$0.00
                                    </#if>
                                  </div>
                                </td>
                              </tr>
                              <tr>
                                <td colspan="4"></td>
                                <td><div align="right"><b>${uiLabelMap.OrderGrandTotal}:</b></div></td>
                                <td>
                                  <div id="cartDisplayGrandTotal" align="center">
                                    <@ofbizCurrency amount=shoppingCart.getDisplayGrandTotal() isoCode=shoppingCart.getCurrency()/>
                                  </div>
                                </td>
                              </tr>
                          </tbody>
                        </table>
              </form>
              <div align="right"><h3><span><a class="buttontext" href="javascript:void(0);" id="editShipping">Continue for step 2</a></span></h3></div>              
            </div>
          </div>

          <div id="shippingPanel" class="screenlet">
            <div class="screenlet-header"><div class="boxhead" align="left">Step 2: Shipping</div></div>
            <div id="shippingSummaryPanel" class="screenlet-body">
              <div align="left"><h3><span><a class="buttontext" href="javascript:void(0);" id="openShippingPanel">Click here to edit</a></span></h3></div>
                  <div style="display:none" id="shippingCompleted">
                    <a href="javascript:void(0);" id="openShippingAndPersonlDetail">
                      <h3>Shipping Summary</h3>
                    </a>
                    <table>
                      <tbody>
                        <tr>
                          <td valign="top" width="10%">Ship To:</td>
                          <td valign="top" width="30%">
                            <div>
                              <div id="completedShipToAttn"></div>
                              <div id="completedShippingContactNumber"></div>
                              <div id="completedEmailAddress"></div>
                            </div>
                          </td>
                          <td width="10%" valign="top">Location:</td>
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
            <div id="editShippingPanel" class="screenlet-body" style="display: none;">
              <form name="shippingForm" id="shippingForm" action="<@ofbizUrl>createUpdateShippingAddress</@ofbizUrl>" method="post">
                <input type="hidden" id="shippingContactMechId" name="shippingContactMechId" value="${parameters.shippingContactMechId?if_exists}"/>
                <input type="hidden" name="contactMechPurposeTypeId" value="SHIPPING_LOCATION"/>
                <input type="hidden" id="shippingPartyId" name="partyId" value="${parameters.partyId?if_exists}"/>
                <input type="hidden" name="userLogin" value="${parameters.userLogin?if_exists}"/>
                <input type="hidden" id="phoneContactMechId" name="phoneContactMechId" value="${parameters.phoneContactMechId?if_exists}"/>
                <input type="hidden" id="emailContactMechId" name="emailContactMechId" value="${parameters.emailContactMechId?if_exists}"/>
                <div id="shippingFormServerError" class="validation-advice"></div>
                          <table>
                            <tr><td width="40%" valign="top">
                                <div class="form-row">
                                  <div class="field-label">
                                     <label for="firstName">${uiLabelMap.PartyFirstName}<span>*</span>
                                       <span id="advice-required-firstName" style="display: none">(required)</span>
                                     </label>
                                  </div>
                                  <div class="field-widget">
                                    <input id="firstName" name="firstName" class="inputBox required" type="text" value="${parameters.firstName?if_exists}"/>
                                  </div>
                                </div>
                                <div class="form-row">
                                  <div class="field-label">
                                    <label for="lastName">${uiLabelMap.PartyLastName}<span>*</span>
                                      <span id="advice-required-lastName" style="display:none">(required)</span>
                                    </label>
                                  </div>
                                  <div class="field-widget">
                                    <input id="lastName" name="lastName" class="inputBox required" type="text" value="${parameters.lastName?if_exists}"/>
                                  </div>
                                </div>
                                <div class="form-row">
                                  <div class="field-label">
                                    <label for="countryCode">${uiLabelMap.PartyCountry}<span>*</span>
                                      <span id="advice-required-shippingCountryCode" style="display:none">(required)</span>
                                    </label>
                                    <label for="areaCode">${uiLabelMap.PartyAreaCode}<span>*</span><span id="advice-required-shippingAreaCode" style="display:none">(required)</span></label>
                                    <label for="contactNumber">${uiLabelMap.PartyContactNumber}<span>*</span><span id="advice-required-shippingContactNumber" style="display:none">(required)</span></label>
                                    <label for="extension">${uiLabelMap.PartyExtension}</label>
                                  </div>
                                  <div class="field-widget">
                                    <input name="countryCode" class="inputBox required" id="shippingCountryCode" value="${parameters.countryCode?if_exists}" size="5" maxlength=3> - 
                                    <input name="areaCode" class="inputBox required" id="shippingAreaCode" value="${parameters.areaCode?if_exists}" size="5" maxlength=3> - 
                                    <input name="contactNumber" class="inputBox required" id="shippingContactNumber" value="${parameters.contactNumber?if_exists}" size="10" maxlength=7> - 
                                    <input name="extension" class="inputBox" id="shippingExtension" value="${parameters.extension?if_exists}" size="5" maxlength=3>
                                  </div>
                                </div>
                                <div class="form-row">
                                  <div class="field-label">
                                    <label for="emailAddress">${uiLabelMap.PartyEmailAddress}<span>*</span>
                                      <span id="advice-required-emailAddress" style="display:none">(required)</span>
                                    </label>
                                  </div>
                                  <div class="field-widget">
                                    <input id="emailAddress" name="emailAddress" class="inputBox required validate-email" maxlength="255" size="40" type="text" value="${parameters.emailAddress?if_exists}"/>
                                  </div>
                                </div>
                            </td><td width="20%"></td><td>
                              <div class="form-row">
                                <div class="field-label">
                                  <label for="shipToAddress1">${uiLabelMap.PartyAddressLine1}<span>*</span><span id="advice-required-shipToAddress1" class="custom-advice" style="display:none">(required)</span></label>
                                </div>
                                <div class="field-widget">
                                  <input id="shipToAddress1" name="shipToAddress1" class="inputBox required" type="text" value="${parameters.shipToAddress1?if_exists}" maxlength="255" size="40"/>
                                </div>
                              </div>
                            <div class="form-row">
                              <div class="field-label"><label for="address2">${uiLabelMap.PartyAddressLine2}</label></div>
                              <div class="field-widget">
                                <input id="shipToAddress2" name="shipToAddress2" class="inputBox" type="text" value="${parameters.shipToAddress2?if_exists}" maxlength="255" size="40"/>
                              </div>
                            </div>
                            <div class="form-row">
                              <div class="field-label">
                                <label for="shipToCity">${uiLabelMap.CommonCity}<span>*</span><span id="advice-required-shipToCity" class="custom-advice" style="display:none">(required)</span></label>
                              </div>
                              <div class="field-widget">
                                <input id="shipToCity" name="shipToCity" class="inputBox required" type="text" value="${parameters.shipToCity?if_exists}" maxlength="255" size="40"/>
                              </div>
                            </div>
                            <div class="form-row">
                              <div class="field-label">
                                <label for="shipToPostalCode">${uiLabelMap.PartyZipCode}<span>*</span><span id="advice-required-shipToPostalCode" class="custom-advice" style="display:none">(required)</span></label>
                              </div>
                              <div class="field-widget">
                                <input id="shipToPostalCode" name="shipToPostalCode" class="inputBox required" type="text" value="${parameters.shipToPostalCode?if_exists}" size="12" maxlength="10"/>
                              </div>
                            </div>
                            <div class="form-row">
                              <div class="field-label">
                                <label for="shipToCountryGeoId">${uiLabelMap.PartyCountry}<span class="requiredLabel"> *</span><span id="advice-required-shipToCountryGeo" style="display:none">(required)</span></label>
                              </div>
                              <div class="field-widget">
                                <div>
                                  <input name="shipToCountryGeo" id="shipToCountryGeo" size="30" class="inputBox required" type="text" value="${parameters.shipToCountryGeo?if_exists}"/>
                                  <input name="countryGeoId" id="shipToCountryGeoId" type="hidden" value="${parameters.countryGeoId?if_exists}"/>
                                  <div id="shipToCountries" class="autocomplete" style="display:none"></div> 
                                </div>
                              </div>
                            </div>
                            <div class="form-row">
                              <div class="field-label">
                                <label for="state">${uiLabelMap.CommonState}<span>*</span><span id="advice-required-shipToStateProvinceGeo" style="display:none">(required)</span></label>
                              </div>
                              <div class="field-widget"> 
                                <div>
                                  <input name="shipToStateProvinceGeo" id="shipToStateProvinceGeo" size="30" class="inputBox required" type="text" value="${parameters.shipToStateProvinceGeo?if_exists}"/>
                                  <input name="shipToStateProvinceGeoId" id="shipToStateProvinceGeoId" type="hidden" value="${parameters.shipToStateProvinceGeoId?if_exists}"/>
                                  <div id="shipToStates" class="autocomplete" style="display:none"></div> 
                                </div>
                              </div> 
                            </div>
                        </td></tr>
                      </table>
              </form>
              <div align="right"><h3><span><a class="buttontext" href="javascript:void(0);" id="editShippingOptions">Continue for step 3</a></span></h3></div>              
            </div>
          </div>

          <div id="shippingOptionPanel" class="screenlet">
            <div class="screenlet-header"><div class="boxhead" align="left">Step 3: Shipping Options</div></div>
            <div id="shippingOptionSummaryPanel" class="screenlet-body">
              <div align="left"><h3><span><a class="buttontext" href="javascript:void(0);" id="openShippingOptionPanel">Click here to edit</a></span></h3></div>
                <div class="completed" style="display:none" id="shippingOptionCompleted">
                  <a href="javascript:void(0);" id="openShippingOption">
                      <h3>Shipping Option Summary</h3>
                  </a>
                  <table cellpadding="0" cellspacing="0">
                    <tbody>
                      <tr>
                        <td valign="top">Shipment Option:</td>
                        <td valign="top">
                          <div id="selectedShipmentOption"></div>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
            </div>

            <div id="editShippingOptionPanel" class="screenlet-body" style="display: none;">
              <form name="shippingOptionForm" id="shippingOptionForm" action="<@ofbizUrl></@ofbizUrl>" method="post">
                <div id="shippingOptionFormServerError" class="validation-advice"></div>
                <table>
                  <tr><td>
                      <div class="form-row">
                        <div class="field-label">
                          <label for="shipmethod">${uiLabelMap.OrderSelectShippingMethod}<span class="requiredLabel"> *</span><span id="advice-required-shipping_method" class="custom-advice" style="display:none">(required)</span></label>
                        </div>
                        <select id="shipMethod" name="shipMethod" class="required"></select>
                      </div>
                  </td></tr>
                </table>
              </form>
              <div align="right"><h3><span><a class="buttontext" href="javascript:void(0);" id="editBilling">Continue for step 4</a></span></h3></div>
            </div>
          </div>

          <div id="billingPanel" class="screenlet">
            <div class="screenlet-header"><div class="boxhead" align="left">Step 4: Billing</div></div>
            <div id="billingSummaryPanel" class="screenlet-body">
              <div align="left"><h3><span><a class="buttontext" href="javascript:void(0);" id="openBillingPanel">Click here to edit</a></span></h3></div>
              <div class="completed" id="billingCompleted" style="display: none;">
                <a href="javascript:void(0);" id="openBillingAndPersonlDetail">
                  <h3>Billing and Payment Summary</h3>
                </a>
                <table width="35%" align="center">
                  <tbody>
                    <tr>
                      <td valign="top" width="5%">Bill To:</td>
                      <td valign="top" width="20%">
                        <div>
                          <div id="completedBillToAttn"></div>
                          <div id="completedCCNumber"></div>
                          <div id="completedExpiryDate"></div>
                        </div>
                      </td>
                      <td valign="top" width="5%">Location:</td>
                      <td valign="top" width="50%">
                        <div>    
                          <div id="completedBillToAddress1"></div>
                          <div id="completedBillToAddress2"></div>
                          <div id="completedBillToGeo"></div>
                        </div>
                      </td>
                    </tr>
                    <tr>
                      <td valign="top">Payment Method:</td>
                      <td valign="top">
                        <div>
                          <div id="paymentMethod"></div>
                        </div> 
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
            <div id="editBillingPanel" class="screenlet-body" style="display: none;">
              <form name="billingForm" id="billingForm" class="theform" action="<@ofbizUrl></@ofbizUrl>" method="post">
                <input type="hidden" id ="billToContactMechId" name="billToContactMechId" value="${parameters.billToContactMechId?if_exists}"/>
                <input type="hidden" id="shippingContactMechIdInBillingForm" name="shippingContactMechId" value="${parameters.shippingContactMechId?if_exists}"/>
                <input type="hidden" id="paymentMethodId" name="paymentMethodId" value="${parameters.paymentMethodId?if_exists}"/>
                <input type="hidden" id="paymentMethodTypeId" name="paymentMethodTypeId" value="CREDIT_CARD"/>
                <input type="hidden" id="billingPartyId" name="partyId" value="${parameters.partyId?if_exists}"/>
                <input type="hidden" name="userLogin" value="${parameters.userLogin?if_exists}"/>
                <input type="hidden" name="expireDate" value="${parameters.expireDate?if_exists}"/>
                <input type="hidden" id="cardType" name="cardType" value="Visa"/>
                <div id="billingFormServerError" class="validation-advice"></div>
                  <table>
                    <tr><td valign="top">
                        <div class="form-row">
                          <div class="field-label">
                            <label for="cardFirstname">${uiLabelMap.PartyFirstName}<span>*</span><span id="advice-required-firstNameOnCard" style="display: none;">(required)</span></label>
                          </div>
                          <div class="field-widget">
                            <input id="firstNameOnCard" name="firstNameOnCard" class="inputBox required" type="text" value="${parameters.firstNameOnCard?if_exists}"/>
                          </div>
                        </div>
                        <div class="form-row">
                          <div class="field-label">
                            <label for="cardLastName">${uiLabelMap.PartyLastName}<span>*</span><span id="advice-required-lastNameOnCard" style="display: none;">(required)</span></label>
                          </div>
                          <div class="field-widget">
                            <input id="lastNameOnCard" name="lastNameOnCard" class="inputBox required" type="text" value="${parameters.lastNameOnCard?if_exists}"/>
                          </div>
                        </div>
                        <div class="form-row">
                          <div class="field-label">
                            <label for="cardNumber">${uiLabelMap.AccountingCardNumber}<span>*</span><span id="advice-required-cardNumber" style="display: none;">(required)</span></label>
                          </div>
                          <div class="field-widget">
                            <input id="cardNumber" autocomplete="off" name="cardNumber" class="inputBox required" type="text" value="${parameters.cardNumber?if_exists}" size=30 maxlength=16  />
                          </div>
                        </div>
                        <div class="form-row">
                          <div class="field-label">
                            <label for="CVV2">CVV2<span>*</span><span id="advice-required-CVV2" style="display:none">(required)</span></label>
                          </div>
                          <div class="field-widget">
                            <input id="CVV2" autocomplete="off" name="cardSecurityCode" class="inputBox required" size="4" type="text" maxlength="4" value=""/>
                          </div>
                        </div>
                        <div class="form-row">
                          <div class="field-label">
                            <label for="expirationdate">${uiLabelMap.AccountingExpirationDate}<span>*</span><span id="advice-validate-expMonth" class="custom-advice" style="display:none">(required)</span></label>
                          </div>
                        </div>
                        <div class="form-row">
                          <span>
                            <label for="expMonth">${uiLabelMap.CommonMonth}:<span>*</span><span id="advice-required-expMonth" style="display:none">(required)</span></label>
                          </span>
                          <span>
                            <label for="expYear">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;${uiLabelMap.CommonYear}:<span>*</span><span id="advice-required-expYear" style="display:none">(required)</span></label>
                          </span>
                          <br>
                          <span>
                            <select id="expMonth" name="expMonth" class="inputBox required">
                              <#if parameters.expMonth?has_content>
                                <option label="${parameters.expMonth?if_exists}" value="${parameters.expMonth?if_exists}">${parameters.expMonth?if_exists}</option>
                              </#if>
                              ${screens.render("component://common/widget/CommonScreens.xml#ccmonths")}
                            </select>
                          </span>
                          <span>
                            <select id="expYear" name="expYear" class="inputBox required">
                              <#if parameters.expYear?has_content>
                                <option value="${parameters.expYear?if_exists}">${parameters.expYear?if_exists}</option>
                              </#if>
                              ${screens.render("component://common/widget/CommonScreens.xml#ccyears")}
                            </select>
                          </span>
                        </div>
                    </td><td width="20%">&nbsp;</td><td valign="top"> 
                        <div class="form-row">
                          <div class="field-widget">
                            <input class="checkbox" id="useShippingAddressForBilling" name="useShippingAddressForBilling" type="checkbox" value="Y" <#if parameters.useShippingAddressForBilling?has_content && parameters.useShippingAddressForBilling?default("")=="Y">checked</#if>> ${uiLabelMap.FacilityBillingAddressSameShipping}
                          </div>
                        </div>
                        <div id="billingAddress" <#if parameters.useShippingAddressForBilling?has_content && parameters.useShippingAddressForBilling?default("")=="Y">style="display:none"</#if>>
                          <div class="form-row">
                            <div class="field-label">
                              <label for="address1">${uiLabelMap.PartyAddressLine1}<span> *</span><span id="advice-required-billToAddress1" style="display:none">(required)</span></label>
                            </div>
                            <div class="field-widget">
                              <input id="billToAddress1" name="billToAddress1" class="inputBox required" size=30 type="text" value="${parameters.billToAddress1?if_exists}"/>
                            </div>
                          </div>
                          <div class="form-row">
                            <div class="field-label">
                              <label for="address2" style="margin-top: 9px;">${uiLabelMap.PartyAddressLine2}</label>
                            </div>
                            <div class="field-widget">
                              <input id="billToAddress2" name="billToAddress2" class="inputBox" type="text" value="${parameters.billToAddress2?if_exists}" size=30/>
                            </div>
                          </div>
                          <div class="form-row">
                            <div class="field-label">                
                              <label for="city">${uiLabelMap.CommonCity}<span>*</span><span id="advice-required-billToCity" style="display:none">(required)</span></label>
                            </div>
                            <div class="field-widget">
                              <input id="billToCity" name="billToCity" class="inputBox required" type="text" value="${parameters.billToCity?if_exists}"/>
                            </div>
                          </div>
                          <div class="form-row">
                            <div class="field-label">
                              <label for="billToPostalCode">${uiLabelMap.PartyZipCode}<span>*</span><span id="advice-required-billToPostalCode" style="display:none">(required)</span></label>   
                            </div>
                            <div class="field-widget">
                              <input id="billToPostalCode" name="billToPostalCode" class="inputBox required" type="text" value="${parameters.billToPostalCode?if_exists}" size="12" maxlength="10"/>
                            </div>
                          </div>
                          <div class="form-row">
                            <div class="field-label">
                              <label for="billToCountryGeoId">${uiLabelMap.PartyCountry}<span>*</span><span id="advice-required-billToCountryGeoId" style="display:none">(required)</span></label>
                            </div>
                            <div class="field-widget">
                              <select name="countryGeoId" id="billToCountryGeoId" class="required selectBox">
                                <#if (parameters.countryGeoId)?exists>
                                  <option>${parameters.countryGeoId}</option>
                                  <option value="${parameters.countryGeoId}">---</option>
                                </#if>
                                ${screens.render("component://common/widget/CommonScreens.xml#countries")}
                              </select>
                            </div>
                          </div>
                          <div class="form-row">
                            <div class="field-label">
                               <label for="state">${uiLabelMap.CommonState}<span>*</span><span id="advice-required-billToStateProvinceGeoId" style="display:none">(required)</span></label>
                            </div>
                            <div class="field-widget"> 
                              <select id="billToStateProvinceGeoId" name="billToStateProvinceGeoId" class="required selectBox">
                                <#if parameters.billToStateProvinceGeoId?has_content>
                                  <option>${parameters.billToStateProvinceGeoId}</option>
                                  <option value="${parameters.billToStateProvinceGeoId}">---</option>
                                <#else>
                                  <option value="">${uiLabelMap.PartyNoState}</option>
                                </#if>
                              </select>
                            </div>
                          </div>
                        </div>
                    </td></tr>
                  </table>
              </form>
              <div align="right"><h3><span><a class="buttontext" href="javascript:void(0);" id="openOrderSubmitPanel">Continue for step 5</a></span></h3></div>
            </div>
          </div>
          <div class="screenlet">
            <div class="screenlet-header"><div class="boxhead" align="left">Step 5: Submit Order</div></div>
            <div id="orderSubmitPanel" style="display: none;">
              <form name="orderSubmitForm" id="orderSubmitForm" action="<@ofbizUrl>onePageProcessOrder</@ofbizUrl>" method="post">
                <div align="right">
                  <input type="button" id="processOrderButton" name="processOrderButton" value="${uiLabelMap.OrderSubmitOrder}" class="mediumSubmit">
                  <input type="button" id="processingOrderButton" name="processingOrderButton" value="${uiLabelMap.OrderSubmittingOrder}" class="mediumSubmit">
                </div>              
              </form>
            </div>
          </div>
        </div>
      </#if>

      <div id="emptyCartCheckoutPanel" align="center" <#if shoppingCart?has_content && shoppingCart.size() gt 0> style="display: none;"</#if>>
        <div>
          <div class="screenlet-header"><div class="boxhead" align="left">Step 1: ${uiLabelMap.PageTitleShoppingCart}</div></div><br/>
          <div>You currently have no items in your cart. Click <a href="<@ofbizUrl>main</@ofbizUrl>">here</a> to view our products.</div>
        </div><br/>
        <div>
          <div class="screenlet-header"><div class="boxhead" align="left">Step 2: Shipping</div></div>
        </div><br/>
        <div>
          <div class="screenlet-header"><div class="boxhead" align="left">Step 3: Shipping Options</div></div>
        </div><br/>
        <div>
          <div class="screenlet-header"><div class="boxhead" align="left">Step 4: Billing</div></div>
        </div><br/>
        <div>
          <div class="screenlet-header"><div class="boxhead" align="left">Step 5: Submit Order</div></div>
        </div>
      </div>
    </div>
  </div>
