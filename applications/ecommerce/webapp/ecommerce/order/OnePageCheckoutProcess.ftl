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
    <div class="screenlet-body" style="text-align: center;">
      <#if shoppingCart?has_content && shoppingCart.size() gt 0>
        <div id="checkoutPanel" class="form-container" align="center" style="border: 1px solid #333333; height: auto;">
          <div id="cartPanel" class="screenlet">
            <div class="screenlet-header"><div class="boxhead" align="left">Step 1: ${uiLabelMap.PageTitleShoppingCart}</div></div>
            <div id="cartSummaryPanel" style="display: none;">
              <div align="left" style="width: auto; padding: 10px 40px 30px 40px;"><a href="javascript:void(0);" id="openCartPanel"><h3>Click here to edit</h3></a></div>
              <div id="cartSummary" style="display: none;">
                Shopping cart summary.
              </div>
            </div>
            <div id="editCartPanel">
              <form name="cartForm" id="cartForm" method="post" action="<@ofbizUrl></@ofbizUrl>">
                Shopping cart information.
              </form>
              <div><h3><span class="editStep"><a href="javascript:void(0);" id="editShipping"><h3>Continue for step 2</h3></a></span></h3></div>              
            </div>
          </div>

          <div id="shippingPanel" class="screenlet">
            <div class="screenlet-header"><div class="boxhead" align="left">Step 2: Shipping</div></div>
            <div id="shippingSummaryPanel">
              <div align="left" style="width: auto; padding: 10px 40px 30px 40px;"><a href="javascript:void(0);" id="openShippingPanel"><h3>Click here to edit</h3></a></div>
                <div id="shippingSummary">
                  <div class="completed" style="display:none" id="shippingCompleted">
                    <a href="javascript:void(0);" id="openShippingAndPersonlDetail">
                      <h3>Shipping Summary</h3>
                    </a>
                    <table  cellpadding="0" cellspacing="0">
                      <tbody>
                        <tr>
                          <td  style=" padding: 6px; width: 60px;" valign="top">Ship To:</td>
                          <td  style=" padding: 6px; width: 60px;" valign="top">
                            <div>
                              <div id="completedShipToAttn"></div>
                              <div id="completedShippingContactNumber"></div>
                              <div id="completedEmailAddress"></div>
                            </div>
                          </td>
                          <td style=" padding: 6px; width: 60px;" valign="top">Location:</td>
                          <td  style="padding: 6px; width: 60px;" valign="top">
                            <div>    
                              <div id="completedShipToAddress1"></div>
                              <div id="completedShipToAddress2"></div>
                              <div id="completedShipToGeo"></div>
                            </div>
                          </td>
                        </tr>
                        <tr><td colspan="10"><hr class="sepbar"/></td></tr>
                      </tbody>
                    </table>
                  </div>
              </div>
            </div>
            <div id="editShippingPanel" style="display: none;">
              <form name="shippingForm" id="shippingForm" action="<@ofbizUrl>createUpdateShippingAddress</@ofbizUrl>" method="post">
                <input type="hidden" id="shippingContactMechId" name="shippingContactMechId" value="${parameters.shippingContactMechId?if_exists}"/>
                <input type="hidden" name="contactMechPurposeTypeId" value="SHIPPING_LOCATION"/>
                <input type="hidden" id="shippingPartyId" name="partyId" value="${parameters.partyId?if_exists}"/>
                <input type="hidden" name="userLogin" value="${parameters.userLogin?if_exists}"/>
                <input type="hidden" id="phoneContactMechId" name="phoneContactMechId" value="${parameters.phoneContactMechId?if_exists}"/>
                <input type="hidden" id="emailContactMechId" name="emailContactMechId" value="${parameters.emailContactMechId?if_exists}"/>
                  <div class="screenlet">
                      <div class="screenlet-header">
                        <div class='boxhead'>&nbsp;${uiLabelMap.PartyNameAndShippingAddress}</div>
                      </div>
                      <div class="screenlet-body">
                        <div class="theform validation-advice" id="shippingContactAndMethodTypeServerError"></div>
                          <table id="shippingTable">
                            <tr><td>
                              <fieldset class="left">
                                <div class="form-row">
                                  <div class="field-label">
                                     <label for="firstName1">${uiLabelMap.PartyFirstName}<span class="requiredLabel"> *</span><span id="advice-required-firstName" class="custom-advice" style="display:none">(required)</span></label>
                                  </div>
                                  <div class="field-widget">
                                    <input id="firstName" name="firstName" class="required" type="text" value="${parameters.firstName?if_exists}"/>
                                  </div>
                                </div>
                                <div class="form-row">
                                  <div class="field-label">
                                    <label for="lastName1">${uiLabelMap.PartyLastName}<span class="requiredLabel"> *</span><span id="advice-required-lastName" class="custom-advice" style="display:none">(required)</span></label>
                                  </div>
                                  <div class="field-widget">
                                     <input id="lastName" name="lastName" class="required" type="text" value="${parameters.lastName?if_exists}"/>
                                  </div>
                                </div>
                                <div class="form-row">
                                  <div class="field-label">
                                    <label for="countryCode">Country Code<span class="requiredLabel"> *</span><span id="advice-required-shippingContactPhoneNumber" class="custom-advice" style="display:none">(required)</span><span id="advice-validate-phone-shippingContactPhoneNumber" class="custom-advice" style="display:none">(required)</span></label>
                                    <input name="countryCode" class="input_mask mask_phone required validate-phone" id="shippingCountryCode" value="${parameters.countryCode?if_exists}" size="3" maxlength=3>
                                  </div>
                                  <div class="field-label">
                                    <label for="areaCode">Area Code<span class="requiredLabel"> *</span><span id="advice-required-shippingContactPhoneNumber" class="custom-advice" style="display:none">(required)</span><span id="advice-validate-phone-shippingContactPhoneNumber" class="custom-advice" style="display:none">(required)</span></label>
                                    <input name="areaCode" class="input_mask mask_phone required validate-phone" id="shippingAreaCode" value="${parameters.areaCode?if_exists}" size="3" maxlength=4>
                                  </div>
                                  <div class="field-label">
                                    <label for="contactNumber">Contact Number<span class="requiredLabel"> *</span><span id="advice-required-shippingContactPhoneNumber" class="custom-advice" style="display:none">(required)</span><span id="advice-validate-phone-shippingContactPhoneNumber" class="custom-advice" style="display:none">(required)</span></label>
                                    <input name="contactNumber" class="input_mask mask_phone required validate-phone" id="shippingContactNumber" value="${parameters.contactNumber?if_exists}" size="5" maxlength=6>
                                  </div>
                                  <div class="field-label">
                                    <label for="extension">Extention<span class="requiredLabel"> *</span><span id="advice-required-shippingContactPhoneNumber" class="custom-advice" style="display:none">(required)</span><span id="advice-validate-phone-shippingContactPhoneNumber" class="custom-advice" style="display:none">(required)</span></label>
                                    <input name="extension" class="input_mask mask_phone required validate-phone" id="shippingExtension" value="${parameters.extension?if_exists}" size="3" maxlength=3>
                                  </div>
                                </div>
                                <div class="form-row">
                                  <div class="field-label">
                                    <label for="emailAddress">${uiLabelMap.PartyEmailAddress}<span class="requiredLabel"> *</span><span id="advice-required-emailAddress" class="custom-advice" style="display:none">(required)</span><span id="advice-validate-email-emailAddress" class="custom-advice" style="display:none">(required)</span></label>
                                  </div>
                                  <div class="field-widget">
                                    <input id="emailAddress" name="emailAddress" class="required validate-email" type="text" value="${parameters.emailAddress?if_exists}"/>
                                  </div>
                                </div>
                              </fieldset>
                              <fieldset class="right">
                            <div class="form-row">
                              <div class="field-label">
                                <label for="shipToAddress1">${uiLabelMap.FormFieldTitleStreetAddress}<span class="requiredLabel"> *</span><span id="advice-required-shipToAddress1" class="custom-advice" style="display:none">(required)</span><span id="advice-validate-no-po-address-shipToAddress1" class="custom-advice" style="display:none">(No PO or APO Address)</span></label>
                              </div>
                              <div class="field-widget">
                                <input id="shipToAddress1" name="shipToAddress1" class="required validate-no-po-address" type="text" value="${parameters.shipToAddress1?if_exists}"/>
                              </div>
                            </div>
                            <div class="form-row">
                              <div class="field-label">
                                <label for="address2">${uiLabelMap.FormFieldTitleStreetAddress2}</label>
                              </div>
                              <div class="field-widget">
                                <input id="shipToAddress2" name="shipToAddress2" type="text" value="${parameters.shipToAddress2?if_exists}" />
                              </div>
                            </div>
                            <div class="form-row">
                              <div class="field-label">
                                <label for="city">${uiLabelMap.CommonCity}<span class="requiredLabel"> *</span><span id="advice-required-shipToCity" class="custom-advice" style="display:none">(required)</span></label>
                              </div>
                              <div class="field-widget">
                                <input id="shipToCity" name="shipToCity" class="required" type="text" value="${parameters.shipToCity?if_exists}" />
                              </div>
                            </div>
                            <div class="form-row">
                              <div class="field-label">
                                <label for="state">${uiLabelMap.CommonState}<span class="requiredLabel"> *</span></label>
                              </div>
                              <div class="field-label" style="clear:both;"> 
                                  <select class="required" id="shipToStateProvinceGeoId" name="shipToStateProvinceGeoId">
                                    <#if (parameters.shipToStateProvinceGeoId)?exists>
                                      <option>${parameters.shipToStateProvinceGeoId}</option>
                                        <option value="${parameters.shipToStateProvinceGeoId}"</option>
                                    <#else>
                                      <option value="CA">CA - California</option>
                                    </#if>
                                    ${screens.render("component://common/widget/CommonScreens.xml#states")}
                                  </select>
                              </div> 
                            </div>
                            <div class="form-row">
                              <div class="field-label">
                                <label for="shipToPostalCode">&nbsp;&nbsp;&nbsp;&nbsp;${uiLabelMap.FormFieldTitleZipCode}<span class="requiredLabel"> *</span><span id="advice-required-shipToPostalCode" class="custom-advice" style="display:none">(required)</span><span id="advice-validate-zip-shipToPostalCode" class="custom-advice" style="display:none">(required)</span></label>
                              </div>
                              <div class="field-widget">
                                <input id="shipToPostalCode" name="shipToPostalCode" class="required validate-zip input_mask mask_zip" type="text" value="${parameters.shipToPostalCode?if_exists}" maxlength=8/>
                              </div>
                            </div>
                            <div class="form-row">
                              <div class="form-label">${uiLabelMap.PartyCountry}</div>
                                <div class="form-field">
                                  <select name="shipToCountryGeoId" id="shipToCountryGeoId" class="selectBox">
                                    <#if (parameters.shipToCountryGeoId)?exists>
                                      <option>${parameters.shipToCountryGeoId}</option>
                                      <option value="${parameters.shipToCountryGeoId}">---</option>
                                    </#if>
                                    ${screens.render("component://common/widget/CommonScreens.xml#countries")}
                                  </select>*
                                </div>
                              </div>
                          </fieldset>
                        </td></tr>
                      </table>
                    </div>
                  </div>
              </form>
              <div><h3><span class="editStep"><a href="javascript:void(0);" id="editShippingOptions"><h3>Continue for step 3</h3></a></span></h3></div>              
            </div>
          </div>

          <div id="shippingOptionPanel" class="screenlet">
            <div class="screenlet-header"><div class="boxhead" align="left">Step 3: Shipping Options</div></div>
            <div id="shippingOptionSummaryPanel">
              <div align="left" style="width: auto; padding: 10px 40px 30px 40px;"><a href="javascript:void(0);" id="openShippingOptionPanel"><h3>Click here to edit</h3></a></div>
              <div id="shippingOptionSummary">
                <div class="completed" style="display:none" id="shippingOptionCompleted">
                  <a href="javascript:void(0);" id="openShippingOption">
                      <h3>Shipping Option Summary</h3>
                  </a>
                  <table cellpadding="0" cellspacing="0">
                    <tbody>
                      <tr>
                        <td style=" padding: 6px; width: 60px;" valign="top">Shipment Option:</td>
                        <td style="padding: 6px; width: 60px;" valign="top">
                          <div id="selectedShipmentOption"></div>
                        </td>
                      </tr>
                      <tr><td colspan="10"><hr class="sepbar"/></td></tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </div>

            <div id="editShippingOptionPanel" style="display: none;">
            <#if shoppingCart.getPartyId()?exists>
              <form name="shippingOptionForm" id="shippingOptionForm" action="<@ofbizUrl></@ofbizUrl>" method="post">
                <table id="shippingTable">
                  <tr><td>
                    <fieldset class="center">
                      <div class="form-row">
                        <div class="field-label">
                          <label for="shipmethod">${uiLabelMap.FormFieldTitleShippingMethod}<span class="requiredLabel"> *</span><span id="advice-required-shipping_method" class="custom-advice" style="display:none">(required)</span></label>
                        </div>
                        <#if shoppingCart?exists>
                          <#assign shippingEstWpr = Static["org.ofbiz.order.shoppingcart.shipping.ShippingEstimateWrapper"].getWrapper(dispatcher, shoppingCart, 0)>
                          <#assign carrierShipmentMethodList = shippingEstWpr.getShippingMethods()>
                          <#if shoppingCart.getShipmentMethodTypeId()?exists && shoppingCart.getCarrierPartyId()?exists>
                            <#assign chosenShippingMethod = shoppingCart.getShipmentMethodTypeId() + '@' + shoppingCart.getCarrierPartyId()>
                          </#if>
                        </#if>
                        <div class="field-widget">
                          <select id="shipMethod" name="shipMethod" class="required">
                            <#list carrierShipmentMethodList as carrierShipmentMethod>
                              <#assign shippingEst = shippingEstWpr.getShippingEstimate(carrierShipmentMethod)?default(-1)>
                              <#if (shippingEst > -1)>
                                <#assign shippingMethod = carrierShipmentMethod.shipmentMethodTypeId + "@" + carrierShipmentMethod.partyId>                                        
                                <option value="${shippingMethod}" 
                                  <#if ((chosenShippingMethod)?exists && chosenShippingMethod == shippingMethod)>SELECTED</#if>>
                                  ${carrierShipmentMethod.description?if_exists}
                                  <#if shippingEst?has_content> - 
                                    <#if (shippingEst > -1)?exists> 
                                      <@ofbizCurrency amount=shippingEst isoCode=shoppingCart.getCurrency()/> 
                                    <#else> 
                                      ${uiLabelMap.OrderCalculatedOffline} 
                                    </#if> 
                                  </#if> 
                                </option>
                              </#if>                                         
                            </#list> 
                          </select>
                        </div>
                      </div>
                    </fieldset>
                  </td></tr>
                </table>
              </form>
              </#if>
              <div><h3><span class="editStep"><a href="javascript:void(0);" id="editBilling"><h3>Continue for step 4</h3></a></span></h3></div>
            </div>
          </div>

          <div id="billingPanel" class="screenlet">
            <div class="screenlet-header"><div class="boxhead" align="left">Step 4: Billing</div></div>
            <div id="billingSummaryPanel">
              <div align="left" style="width: auto; padding: 10px 40px 30px 40px;"><a href="javascript:void(0);" id="openBillingPanel"><h3>Click here to edit</h3></a></div>
              <div class="completed" id="billingCompleted" style="display: none;">
                <a href="javascript:void(0);" id="openBillingAndPersonlDetail">
                  <h3>Billing and Payment Summary</h3>
                </a>
                <table  cellpadding="0" cellspacing="0">
                  <tbody>
                    <tr>
                      <td  style=" padding: 6px; width: 60px;" valign="top">Bill To:</td>
                      <td  style=" padding: 6px; width: 60px;" valign="top">
                        <div>
                          <div id="completedBillToAttn"></div>
                          <div id="completedCCNumber"></div>
                          <div id="completedExpiryDate"></div>
                        </div>
                      </td>
                      <td style=" padding: 6px; width: 60px;" valign="top">Location:</td>
                      <td  style="padding: 6px; width: 60px;" valign="top">
                        <div>    
                          <div id="completedBillToAddress1"></div>
                          <div id="completedBillToAddress2"></div>
                          <div id="completedBillToGeo"></div>
                        </div>
                      </td>
                    </tr>
                    <tr>
                      <td style=" padding: 6px; width: 60px;" valign="top">Payment Method:</td>
                      <td  style="padding: 6px; width: 80px;" valign="top">
                        <div>
                          <div id="paymentMethod"></div>
                        </div> 
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
            <div id="editBillingPanel" style="display: none;">
              <form name="billingForm" id="billingForm" class="theform" action="<@ofbizUrl></@ofbizUrl>" method="post">
                Billing and Payment Detail.
                <input type="hidden" id ="billToContactMechId" name="billToContactMechId" value="${parameters.billToContactMechId?if_exists}"/>
                <input type="hidden" id="shippingContactMechIdInBillingForm" name="shippingContactMechId" value="${parameters.shippingContactMechId?if_exists}"/>
                <input type="hidden" id="paymentMethodId" name="paymentMethodId" value="${parameters.paymentMethodId?if_exists}"/>
                <input type="hidden" id="paymentMethodTypeId" name="paymentMethodTypeId" value="CREDIT_CARD"/>
                <input type="hidden" id="billingPartyId" name="partyId" value="${parameters.partyId?if_exists}"/>
                <input type="hidden" name="userLogin" value="${parameters.userLogin?if_exists}"/>
                <input type="hidden" name="expireDate" value="${parameters.expireDate?if_exists}"/>
                <input type="hidden" id="cardType" name="cardType" value="Visa"/>
                <div class="panelBody" id="billingId">
                  <table id="billingTable">
                    <tr><td>
                      <div style="clear:both"></div>
                      <fieldset class="left">
                        <span style="float:left;  margin-top:10px; width:300px"></span>
                        <div class="form-row">
                          <span>
                            <label for="cardname">First Name<span class="requiredLabel"> *</span><span id="advice-required-firstNameOnCard" class="custom-advice" style="display:none">(req)</span></label>
                            <input id="firstNameOnCard" name="firstNameOnCard" class="required" type="text" value="${parameters.firstNameOnCard?if_exists}" />
                          </span>
                          <span>
                            <label for="cardname">Last Name<span class="requiredLabel"> *</span><span id="advice-required-lastNameOnCard" class="custom-advice" style="display:none">(req)</span></label>
                            <input id="lastNameOnCard" name="lastNameOnCard" class="required" type="text" value="${parameters.lastNameOnCard?if_exists}" />
                          </span>
                        </div>
                        <div class="form-row">
                          <div class="field-label">
                            <label for="cardNumber">${uiLabelMap.AccountingCardNumber} (no spaces)<span class="requiredLabel"> *</span><span id="advice-required-cardNumber" class="custom-advice" style="display:none">(req)</span><span id="advice-validate-creditcard-cardNumber" class="custom-advice" style="display:none">(req)</span></label>
                          </div>
                          <div class="field-widget">
                            <input id="cardNumber" autocomplete="off" name="cardNumber" class="validate-creditcard" type="text" value="${parameters.cardNumber?if_exists}" size=30 maxlength=16  />
                          </div>
                        </div>
                        <div class="form-row">
                          <div class="field-label">
                            <label for="CVV2">CVV2 (no spaces)<span class="requiredLabel"> *</span><span id="advice-required-cardNumber" class="custom-advice" style="display:none">(req)</span><span id="advice-validate-creditcard-cardNumber" class="custom-advice" style="display:none">(req)</span></label>
                          </div>
                          <div class="field-widget">
                            <input id="CVV2" autocomplete="off" name="cardSecurityCode" class="required validate-cvv2" size="4" type="text" maxlength="4" value=""/>
                          </div>
                        </div>
                        <div class="form-row">
                          <div class="field-label">
                            <label for="expirationdate">${uiLabelMap.AccountingExpirationDate}<span class="requiredLabel"> *</span><span id="advice-validate-creditcard-expiration-expMonth" class="custom-advice" style="display:none">(required)</span></label>
                          </div>
                          <div class="form-field">
                            <div>
                              <span>
                                Month: <select id="expMonth" name="expMonth" class="validate-creditcard-expiration">
                                  <#if parameters.expMonth?has_content>
                                    <option label="${parameters.expMonth?if_exists}" value="${parameters.expMonth?if_exists}">${parameters.expMonth?if_exists}</option>
                                  </#if>
                                  ${screens.render("component://common/widget/CommonScreens.xml#ccmonths")}
                                </select>
                              </span> 
                              <span>
                                Year: <select id="expYear" name="expYear">
                                  <#if parameters.expYear?has_content>
                                    <option value="${parameters.expYear?if_exists}">${parameters.expYear?if_exists}</option>
                                  </#if>
                                  ${screens.render("component://common/widget/CommonScreens.xml#ccyears")}
                                </select>
                              </span>
                            </div>
                          </div>
                        </div>
                      </fieldset>  
                      <fieldset class="right">
                        <div class="form-row">
                          <span>
                            <input class="checkbox" id="useShippingAddressForBilling" name="useShippingAddressForBilling" type="checkbox" value="Y" <#if parameters.useShippingAddressForBilling?has_content && parameters.useShippingAddressForBilling?default("")=="Y">checked</#if>>
                            <span style="font-size:10px; float:left; padding:0px 0px 0px 6px; margin-top:10px; width:300px">${uiLabelMap.FacilityBillingAddressSameShipping}</span>
                          </span>
                        </div>
                        <div style="clear: both"></div>
                        <div id="billingAddress" <#if parameters.useShippingAddressForBilling?has_content && parameters.useShippingAddressForBilling?default("")=="Y">style="display:none"</#if>>
                          <div class="form-row">
                            <div class="field-label">
                              <label for="address1">${uiLabelMap.PartyAddressLine1}<span class="requiredLabel"> *</span><span id="advice-required-billToAddress1" class="custom-advice" style="display:none">(required)</span></label>
                            </div>
                          <div class="field-widget">
                            <input id="billToAddress1" name="billToAddress1" class="required" size=30 type="text" value="${parameters.billToAddress1?if_exists}" />
                          </div>
                          <div class="field-label">
                            <label for="address2" style="margin-top: 9px;">${uiLabelMap.PartyAddressLine2}</label>
                          </div>
                          <div class="field-widget">
                            <input id="billToAddress2" name="billToAddress2" type="text" value="${parameters.billToAddress2?if_exists}" size=30/>
                          </div>
                        </div>
                        <div class="form-row">
                          <div class="field-label">                
                            <label for="city">${uiLabelMap.CommonCity}<span class="requiredLabel"> *</span><span id="advice-required-billToCity" class="custom-advice" style="display:none">(required)</span></label>
                          </div>
                          <div class="field-widget">
                            <input id="billToCity" name="billToCity" class="required" type="text" value="${parameters.billToCity?if_exists}" />
                          </div>
                        </div>
                        <div class="form-row">
                          <div>
                            <span>
                              <div class="field-label">
                                <label for="state">${uiLabelMap.CommonState}<span class="requiredLabel"> *</span></label>
                              </div>
                            </span>
                          </div>
                          <div class="field-label" style="clear:both;"> 
                            <span style="margin-top:-8px;">
                              <select class="required" id="billToStateProvinceGeoId" name="billToStateProvinceGeoId">
                                <#if parameters.billToStateProvinceGeoId?has_content>
                                  <option>${parameters.billToStateProvinceGeoId}</option>
                                  <option value="${parameters.billToStateProvinceGeoId}">---</option>
                                <#else>
                                  <option value="CA">CA - California</option>
                                </#if>
                                ${screens.render("component://common/widget/CommonScreens.xml#states")}
                              </select>
                            </span>
                          </div>

                          <div class="form-row">
                            <div class="field-label">
                              <label for="billToPostalCode">${uiLabelMap.PartyZipCode}<span class="requiredLabel"> *</span><span id="advice-required-billToPostalCode" class="custom-advice" style="display:none">(required)</span><span id="advice-validate-billToPostalCode" class="custom-advice" style="display:none">(required)</span></label>   
                            </div>
                            <div class="field-widget">
                              <input id="billToPostalCode" name="billToPostalCode" class="required validate-zip input_mask mask_zip" type="text" value="${parameters.billToPostalCode?if_exists}" />
                            </div>
                          </div>
                          <div class="form-row">
                            <div class="form-label">${uiLabelMap.PartyCountry}</div>
                            <div class="form-field">
                              <select name="billToCountryGeoId" id="billToCountryGeoId" class="selectBox">
                                <#if (parameters.billToCountryGeoId)?exists>
                                  <option>${parameters.billToCountryGeoId}</option>
                                  <option value="${parameters.billToCountryGeoId}">---</option>
                                </#if>
                                ${screens.render("component://common/widget/CommonScreens.xml#countries")}
                              </select>*
                            </div>
                          </div>
                        </div>
                      </div>    
                    </fieldset>
                  </td></tr>
                </table>
              </div>
              </form>
              <div><h3><span class="editStep"><a href="javascript:void(0);" id="openOrderSubmitPanel"><h3>Continue for step 5</h3></a></span></h3></div>
            </div>
          </div>
          <div id="" class="screenlet">
            <div class="screenlet-header"><div class="boxhead" align="left">Step 5: Submit Order</div></div>
            <div id="orderSubmitPanel" style="display: none;">
              <form name="orderSubmitForm" id="orderSubmitForm" action="<@ofbizUrl></@ofbizUrl>" method="post">
              </form>
              <div><h3><span class="editStep"><a href="javascript:void(0);" id=""><h3>Submit Order.</h3></a></span></h3></div>  
            </div>
          </div>
        </div>
      </#if>

      <div id="emptyCartCheckoutPanel" align="center" <#if shoppingCart?has_content && shoppingCart.size() gt 0> style="display: none; border: 1px solid #333333; height: auto;"</#if>>
        <div>${uiLabelMap.OrderCheckout}</div>
        <div>
          <div><span style="display: none"><a href="javascript:void(0);"><img src="<@ofbizContentUrl></@ofbizContentUrl>"></a></span></div>
          <div>STEP 1: Confirm Totals</div><br>
          <div>You currently have no items in your cart. Click <a href="<@ofbizUrl>main</@ofbizUrl>">here</a> to view our products.</div>
        </div>
        <div>
          <div><span style="display: none"><a href="javascript:void(0);"><img src="<@ofbizContentUrl></@ofbizContentUrl>"></a></span></div>
          <div>STEP 2: Shipping</div>
        </div>
        <div>
          <div><span style="display: none"><a href="javascript:void(0);"><img src="<@ofbizContentUrl></@ofbizContentUrl>"></a></span></div>
          <div>STEP 3: Billing</div>          
        </div>
      </div>
    </div>
  </div>
