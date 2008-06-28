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
                <div id="shippingSummary"><a href="javascript:void(0);" id="openShippingAndPersonlDetail">
                <h3>Shipping Summary</h3></a>
                  <div class="completed" style="display:none" id="shippingCompleted">
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
              <form name="shippingForm" id="shippingForm" action="<@ofbizUrl>createUpdateCustomerAndShippingContact</@ofbizUrl>" method="post">
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
                                <input id="shipToPostalCode" name="shipToPostalCode" class="required validate-zip input_mask mask_zip" type="text" value="${parameters.shipToPostalCode?if_exists}"/>
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
              <div id="shippingOptionSummary" style="display: none;">
               Shipping Methods summary.
              </div>
            </div>
            <div id="editShippingOptionPanel" style="display: none;">
              <form name="shippingOptionForm" id="shippingOptionForm" action="<@ofbizUrl></@ofbizUrl>" method="post">
                Shipping Methods.
              </form>
              <div><h3><span class="editStep"><a href="javascript:void(0);" id="editBilling"><h3>Continue for step 4</h3></a></span></h3></div>
            </div>
          </div>

          <div id="billingPanel" class="screenlet">
            <div class="screenlet-header"><div class="boxhead" align="left">Step 4: Billing</div></div>
            <div id="billingSummaryPanel">
              <div align="left" style="width: auto; padding: 10px 40px 30px 40px;"><a href="javascript:void(0);" id="openBillingPanel"><h3>Click here to edit</h3></a></div>
              <div id="billingSummary" style="display: none;">
                Billing and Payment summary.
              </div>
            </div>
            <div id="editBillingPanel" style="display: none;">
              <form name="billingForm" id="billingForm" class="theform" action="<@ofbizUrl></@ofbizUrl>" method="post">
                Billing and Payment Detail.
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
