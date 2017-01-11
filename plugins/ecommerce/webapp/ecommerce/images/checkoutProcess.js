/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
var isCartStepValidate = false;
var isShipStepValidate = false;
var isShipOptionStepValidate = false;
var isBillStepValidate = false;

jQuery(document).ready(function(){
    // Cart
    var validateCart = jQuery("#cartForm");
    validateCart.validate();

    var validateShip = jQuery("#shippingForm");
    validateShip.validate();

    var validateShipOption = jQuery("#shippingOptionForm");
    validateShipOption.validate();

    var validateBill = jQuery("#billingForm");
    validateBill.validate();

    // Goto Edit Cart Panel
    jQuery('#openCartPanel').click(function() {
        showEditCartPanel();
        updateShippingSummary();
    });

    // Update Shipping Address
    jQuery('#savePartyAndShippingContact').click(function() {
        if (validateShip.valid()) {
            jQuery('#savePartyAndShippingContact').fadeOut('fast');
            jQuery('#processingShippingOptions').fadeIn('fast');
            if (createUpdateCustomerAndShippingAddress()){
                showEditShippingOptionPanel();
            }
            jQuery('#processingShippingOptions').fadeOut('fast');
            jQuery('#savePartyAndShippingContact').fadeIn('fast');
        }
    });

    // Goto Edit Shipping  panel
    jQuery('#updateShoppingCart').click(function() {
        if (validateCart.valid()) {
            showEditShippingPanel();
        }
    });
    // Goto Edit Shipping Panel
    jQuery('#openShippingPanel').click(function() {
        showEditShippingPanel();
        setShippingOption();
    });

    // Set Shipping Method to card and goto Billing step
    jQuery('#saveShippingMethod').click(function() {
        jQuery('#saveShippingMethod').fadeOut('fast');
        jQuery('#processingBilling').fadeIn('fast');
        if (setShippingOption()){
            showEditBillingPanel();
        }
        jQuery('#processingBilling').fadeOut('fast');
        jQuery('#saveShippingMethod').fadeIn('fast');
    });

    jQuery('#openShippingOptionPanel').click(function() {
        showEditShippingOptionPanel();
        updateBillingSummary();
    });

    // Billing
    jQuery('#openBillingPanel').click(function() {
        showEditBillingPanel();
    });

    jQuery('#savePaymentAndBillingContact').click(function() {
        if (validateBill.valid()) {
            jQuery('#savePaymentAndBillingContact').fadeOut('fast');
            jQuery('#processingOrderSubmitPanel').fadeIn('fast');
            if (processBillingAndPayment()) {
                showOrderSubmitPanel();
            }
            jQuery('#processingOrderSubmitPanel').fadeOut('fast');
            jQuery('#savePaymentAndBillingContact').fadeIn('fast');
        }
    });

    // For Billing Address Same As Shipping
    jQuery('#useShippingAddressForBilling').click(function() {
        useShippingAddressForBillingToggle();
        validateBill.valid();
    });

    // Initiate Observing Edit Cart Events
    initCartProcessObservers();

    jQuery('#processOrderButton').click(function(){
        processOrder();
    });

    if (jQuery('#shippingForm').length) {
        // Get associate states for Shipping Information
        jQuery('#shipToCountryGeoId').change(function(){
            getAssociatedStateList('shipToCountryGeoId', 'shipToStateProvinceGeoId', 'advice-required-shipToStateProvinceGeoId', 'shipToStates');
        });
        if (jQuery('#userLoginId').length) {
            var stateValue = jQuery('#shipToStateProvinceGeoId').val();
            getAssociatedStateList('shipToCountryGeoId', 'shipToStateProvinceGeoId', 'advice-required-shipToStateProvinceGeoId', 'shipToStates');
            jQuery('#shipToStateProvinceGeoId').val(stateValue);
            stateValue = jQuery('#billToStateProvinceGeoId').val();
            getAssociatedStateList('billToCountryGeoId', 'billToStateProvinceGeoId', 'advice-required-billToStateProvinceGeoId', 'billToStates');
            jQuery('#billToStateProvinceGeoId').val(stateValue);
        } else {
            getAssociatedStateList('shipToCountryGeoId', 'shipToStateProvinceGeoId', 'advice-required-shipToStateProvinceGeoId', 'shipToStates');
            getAssociatedStateList('billToCountryGeoId', 'billToStateProvinceGeoId', 'advice-required-billToStateProvinceGeoId', 'billToStates');
        }
    }
    if (jQuery('#billingForm').length) {
        // Get associate states for Billing Information
        jQuery('#billToCountryGeoId').change(function() {
            getAssociatedStateList('billToCountryGeoId', 'billToStateProvinceGeoId', 'advice-required-billToStateProvinceGeoId', 'billToStates');
        });
    }
});

// Check server side error
function getServerError(data) {
    var serverErrorHash = [];
    var serverError = "";
    if (data._ERROR_MESSAGE_LIST_ != undefined) {
        serverErrorHash = data._ERROR_MESSAGE_LIST_;
        jQuery.each(serverErrorHash, function(i, error) {
            var encodedErrorMessage = jQuery('<div/>').text(error.message).html();
            serverError += encodedErrorMessage + '<br/>';
        });
    }
    if (data._ERROR_MESSAGE_ != undefined) {
        serverError = jQuery('<div/>').text(data._ERROR_MESSAGE_).html();
    }
    return serverError;
}

// Begin Show/Hide Step panels

function hideEditCartPanel() {
    if (jQuery('#editCartPanel').is(':visible')) {
        jQuery('#editCartPanel').slideUp();
        jQuery('#cartSummaryPanel').slideDown();
    }
}
function hideEditShippingPanel() {
     if (jQuery('#editShippingPanel').is(':visible')) {
         jQuery('#editShippingPanel').slideUp();
         jQuery('#shippingSummaryPanel').slideDown();
     }
}
function hideEditShippingOptionPanel() {
     if (jQuery('#editShippingOptionPanel').is(':visible')) {
         jQuery('#editShippingOptionPanel').slideUp();
         jQuery('#shippingOptionSummaryPanel').slideDown();
     }
}
function hideEditBillingPanel() {
    if (jQuery('#editBillingPanel').is(':visible')) {
        jQuery('#editBillingPanel').slideUp();
        jQuery('#billingSummaryPanel').slideDown();
    }
}
function hideOrderSubmitPanel() {
    if (jQuery('#orderSubmitPanel').is(':visible')) {
        jQuery('#orderSubmitPanel').slideUp();
        jQuery('#processingOrderButton').slideDown();

    }
}

function showEditCartPanel() {
    if (!jQuery('#editCartPanel').is(':visible') ) {
        jQuery('#cartSummaryPanel').slideUp();
        hideEditShippingPanel();
        hideEditShippingOptionPanel();
        hideEditBillingPanel();
        hideOrderSubmitPanel();
        jQuery('#editCartPanel').slideDown();
    }
}

function showEditShippingPanel() {
     if (!jQuery('#editShippingPanel').is(':visible') ) {
         jQuery('#shippingSummaryPanel').slideUp();
         hideEditCartPanel();
         hideEditShippingOptionPanel();
         hideEditBillingPanel();
         hideOrderSubmitPanel();
         jQuery('#editShippingPanel').slideDown();

     }
}

function showEditShippingOptionPanel() {
     if (!jQuery('#editShippingOptionPanel').is(':visible') ) {
         jQuery('#shippingOptionSummaryPanel').slideUp();
         hideEditCartPanel();
         hideEditShippingPanel();
         hideEditBillingPanel();
         hideOrderSubmitPanel();
         jQuery('#editShippingOptionPanel').slideDown();
     }
}

function showEditBillingPanel() {
    if (!jQuery('#editBillingPanel').is(':visible') ) {
         jQuery('#billingSummaryPanel').slideUp();
         hideEditCartPanel();
         hideEditShippingPanel();
         hideEditShippingOptionPanel();
         hideOrderSubmitPanel();
         jQuery('#editBillingPanel').slideDown();
    }
    if (jQuery('#shipToContactMechId').val() != jQuery('#billToContactMechId').val()) {
        jQuery('#useShippingAddressForBilling').val('false');
        jQuery('#billingAddress').slideDown();
        jQuery('#useShippingAddressForBilling').val('N');
    }
}

function showOrderSubmitPanel() {
    if (!jQuery('#orderSubmitPanel').is(':visible')) {
         hideEditCartPanel();
         hideEditShippingPanel();
         hideEditShippingOptionPanel();
         hideEditBillingPanel();
         jQuery('#orderSubmitPanel').slideDown();
    }
}

// End Show/Hide Step panels

function createUpdateCustomerAndShippingAddress() {
    var result = false;
    jQuery.ajax({
        url: 'createUpdateShippingAddress',
        type: 'POST',
        async: false,
        data: jQuery('#shippingForm').serialize(),
        success: function(json) {
                var serverError = getServerError(json);
                if (!serverError) {
                    jQuery('#shippingFormServerError').fadeOut('fast');
                    // Process Shipping data response.
                    jQuery('#shipToPartyId').val(json.partyId);
                    jQuery('#billToPartyId').val(json.partyId);
                    jQuery('#shipToContactMechId').val(json.contactMechId);
                    jQuery('#shipToPhoneContactMechId').val(json.shipToPhoneContactMechId);
                    jQuery('#emailContactMechId').val(json.emailContactMechId);
                    //jQuery('#completedShippingMethod').html(json.shippingDescription);
                    updateShippingSummary();
                    getShipOptions();
                    result = true;
                } else {
                    jQuery('#shippingFormServerError').html(serverError);
                    result = false;
                }
        },
        error: function(error) {
            if (error != "") {
                jQuery('#shippingFormServerError').html(error);
            }
            result = false;
        }
    });
    return result;
}

function getShipOptions() {
    var shipOptions = null;
    var optionList = [];
    var result = false;
    if (jQuery('#shipMethod').val() == "" || jQuery('#shipMethod').val() == null) {
        jQuery.ajax({
            url: 'getShipOptions',
            type: 'POST',
            async: false,
            success: function(json) {
                var serverError = getServerError(json);
                if (!serverError) {
                        jQuery('#shippingFormServerError').fadeOut('fast');
                        isShipStepValidate = true;
                        shipOptions = json.shippingOptions;
                        var shipMethod = jQuery('#shipMethod');
                        shipMethod.find("option").remove();
                        jQuery.each(shipOptions, function(shipOption) {
                            if (this.productStoreShipMethId){
                                shipMethod.append(jQuery("<option value = " + this.shippingMethod + ":" + this.productStoreShipMethId + " > " + this.shippingDesc  + " </option>"));
                            } else {
                                shipMethod.append(jQuery("<option value = " + this.shippingMethod + " > " + this.shippingDesc  + " </option>"));
                            }
                        });
                        result = true;
                    } else {
                        jQuery('#shippingFormServerError').html(serverError);
                        result = false;
                    }
            },
            error: function(error) {
                if (error != "") {
                    jQuery('#shippingFormServerError').fadeIn('fast');
                    jQuery('#shippingFormServerError').html(error);
                    isShipStepValidate = false;
                }
                result = false;
            }
        });
    }
    return result;
}

// Shipping option
function setShippingOption() {
    var shipTotal = null;
    var shipMethod = null;
    var result = false;
    jQuery('#shippingOptionFormServerError').fadeOut('fast');
    jQuery.ajax({
        url: 'setShippingOption',
        type: 'POST',
        async: false,
        data: jQuery('#shippingOptionForm').serialize(),
        success: function(json) {
            var serverError = getServerError(json);
            if (!serverError) {
            shipTotal = json.shippingTotal;
                isShipOptionStepValidate = true;
                jQuery('#selectedShipmentOption').html(json.shippingDescription);
                //jQuery('#shippingDescription').value = json.shippingDescription;
                //jQuery('#shippingTotal').val(json.shippingTotal);
                //jQuery('#cartGrandTotal').val(json.cartGrandTotal);
                //jQuery('#totalSalesTax').val(json.totalSalesTax);
                result = true;
            } else {
                jQuery('#shippingFormServerError').html(serverError);
                result = false;
            }
        },
        error: function(error) {
            if(error != "") {
                jQuery('#shippingOptionFormServerError').fadeIn('fast');
                jQuery('#shippingOptionFormServerError').html(error);
                isShipOptionStepValidate = false;
            }
            result = false;
        }
    });
    updateCartData();
    return result;
}
// Billing
function useShippingAddressForBillingToggle() {
    if (jQuery('#useShippingAddressForBilling').is(':checked') ) {
        jQuery('#billToAddress1').val(jQuery('#shipToAddress1').val());
        jQuery('#billToAddress2').val(jQuery('#shipToAddress2').val());
        jQuery('#billToCity').val(jQuery('#shipToCity').val());
        jQuery('#billToPostalCode').val(jQuery('#shipToPostalCode').val());
        jQuery('#billToCountryGeoId').val(jQuery('#shipToCountryGeoId').val());
        getAssociatedStateList('billToCountryGeoId', 'billToStateProvinceGeoId','advice-required-billToStateProvinceGeoId','billToStates');
        jQuery('#useShippingAddressForBilling').val("Y");
        jQuery('#billToStateProvinceGeoId').val(jQuery('#shipToStateProvinceGeoId').val());
        jQuery('#billingAddress').slideUp();
    } else {
        jQuery('#billingAddress').slideDown();
        jQuery('#useShippingAddressForBilling').val("N");
    }
}
function processBillingAndPayment() {
    var result = false;
    jQuery.ajax({
        url: 'createUpdateBillingAndPayment',
        type: 'POST',
        data: jQuery('#billingForm').serialize(),
        async: false,
        success: function(json) {
            var serverError = getServerError(json);
            if (!serverError) {
                    jQuery('#billingFormServerError').fadeOut('fast');
                    isBillStepValidate = true;
                    jQuery('#billToContactMechId').val(json.contactMechId);
                    jQuery('#paymentMethodId').val(json.paymentMethodId);
                    jQuery('#billToPhoneContactMechId').val(json.billToPhoneContactMechId);
                    updateBillingSummary();
                    result = true;
                } else {
                    jQuery('#billingFormServerError').html(serverError);
                    result = false;
                }
        },
        error: function(error) {
            if(error != "") {
                jQuery('#billingFormServerError').fadeIn('fast');
                jQuery('#billingFormServerError').html(error);
                isBillStepValidate = false;
            }
            result = false;
        }
    });
    return result;

}
function initCartProcessObservers() {
    var cartForm = jQuery('#cartForm');
    jQuery('#productPromoCode').change(function() {
        addPromoCode();
    });
    jQuery('#updateShoppingCart').click(function() {
        showEditShippingPanel();
    });
    jQuery('#openCartPanel').click(function() {
        showEditCartPanel();
        updateShippingSummary();
    });
    var inputs = cartForm.find('input[type=text]');
    inputs.each(function(e) {
        if(this.id != 'productPromoCode' && this.id != undefined) {
            jQuery(this).change(function() {
                cartItemQtyChanged(this);
            });
        }
    });
    var links = jQuery("#cartForm a[id^='removeItemLink_']");
    jQuery.each(links, function() {
        jQuery(this).bind('click', function(){
            removeItem(this);
        });
    });
    if (jQuery('#initializedCompletedCartDiscount').length && jQuery('#initializedCompletedCartDiscount').val() == 0) {
        jQuery('#completedCartDiscountRow').hide();
    }
}
function addPromoCode() {
    jQuery.ajax({
        url: 'silentAddPromoCode',
        type: 'POST',
        data: {"productPromoCodeId" : jQuery('#productPromoCode').val()},
        success: function(json) {
            var serverError = getServerError(json);
            if (!serverError) {
                jQuery('#cartFormServerError').fadeOut('fast');
                updateCartData();
            } else {
                jQuery('#shippingFormServerError').html(serverError);
                result = false;
            }
        },
        error: function(error) {
            if(error != "") {
                jQuery('#cartFormServerError').fadeIn('fast');
                jQuery('#cartFormServerError').html(error);
            }
        }
    });
}

function getProductLineItemIndex(event, productId) {
    var itemIndex = null;
    var productIdParam = "productId=" + productId;
    var formValues = jQuery('#cartForm').serialize() + "&" + productIdParam;
    jQuery.ajax({
        url: 'getShoppingCartItemIndex',
        type: 'POST',
        async: false,
        data: formValues,
        success: function(json) {
            var serverError = getServerError(json);
            if (!serverError) {
                itemIndex = json.itemIndex;
            } else {
                jQuery('#shippingFormServerError').html(serverError);
            }
        }
    });
    return itemIndex;
}

function removeItem(elmt) {
    var removeElement = elmt;
    var elementId = removeElement.id;
    var qtyId = elementId.replace('removeItemLink_', 'qty_');
    var productIdElementId =  elementId.replace('removeItemLink_', 'cartLineProductId_');
    var productId = jQuery("#" + productIdElementId).val();
    var itemIndex = getProductLineItemIndex(elmt, productId);
    var formValues = "update_" + itemIndex + "= 0";
    if (jQuery(qtyId).val() == '' || isNaN(jQuery(qtyId).val())) {
        jQuery(qtyId).val("0");
    }
    updateCartData(qtyId, formValues, 0, itemIndex);
}

function cartItemQtyChanged(elmt) {
    var qtyElement = elmt;
    var elementId = qtyElement.id;
    var productIdElementId = elementId.replace('qty_', 'cartLineProductId_');
    var productId = jQuery("#" + productIdElementId).val();
    if (jQuery(qtyElement).val() && jQuery(qtyElement).val() >= 0 && !isNaN(jQuery(qtyElement).val())) {
        var itemIndex = getProductLineItemIndex(elmt, productId);
        qtyParam = "update_" + itemIndex +"="+jQuery(qtyElement).val();
        var formValues = jQuery('#cartForm').serialize() + '&' + qtyParam;
        updateCartData(elementId, formValues, qtyElement.value, itemIndex);
    }
}

function updateCartData(elementId, formValues, itemQty, itemIndex) {
    jQuery.ajax({
        url: 'cartItemQtyUpdate',
        type: 'POST',
        data: formValues,
        success: function(json) {
            var serverError = getServerError(json);
            if (!serverError) {
                    if (json.totalQuantity == 0) {
                        jQuery('#emptyCartCheckoutPanel').show();
                        jQuery('#checkoutPanel').hide();
                        jQuery('#microCartNotEmpty').hide();
                        jQuery('#microCartEmpty').show();
                        jQuery('#quickCheckoutEnabled').hide();
                        jQuery('#quickCheckoutDisabled').show();
                        jQuery('#onePageCheckoutEnabled').hide();
                        jQuery('#onePageCheckoutDisabled').show();
                        jQuery('#microCartPayPalCheckout').hide();
                    } else {
                        // Replace whole cart panel with updated cart values for updating line item in case of gift item is added or remove in cart after applying coupon code
                        // No need to calculate individual value for shopping cart when whole cart is updating
                        jQuery.ajax({
                            url: 'UpdateCart',
                            type: 'POST',
                            cache: false,
                            success: function(data) {
                                jQuery('#cartPanel').html(data);
                                initCartProcessObservers();
                            }
                        });
                    }
                } else {
                    jQuery('#shippingFormServerError').html(serverError);
                    result = false;
                }
            }
    });
}
function processOrder() {
    jQuery('#processOrderButton').disabled = true ;
    jQuery('#processOrderButton').fadeOut('fast');
    jQuery('#processingOrderButton').fadeIn('fast');
    jQuery('#orderSubmitForm').submit();
}
function getAssociatedBillingStateList(formName, divId) {
    var optionList = [];
    jQuery.ajax({
        url: "getAssociatedStateList",
        data: jQuery(formName).serialize(),
        async: false,
        success: function(transport) {
            stateList = data.stateList;
            var billingStates = jQuery("#" + divId);
            billingStates.find("option").remove();
            jQuery.each(stateList, function(state) {
                geoVolues = this.split(': ');
                billingStates.append(jQuery("<option value = " + geoVolues[1] + " >" + geoVolues[0] + "</option>"));
            });
        }
    });
}

function updateShippingSummary() {
    var fullName = jQuery('#firstName').val() + " " +jQuery('#lastName').val();
    var extension = "";
    if (jQuery('#shipToExtension').val()) {
        extension = "-" + jQuery('#shipToExtension').val();
        }
    var shippingContactPhoneNumber = jQuery('#shipToCountryCode').val()+ "-" + jQuery('#shipToAreaCode').val()
        + "-" + jQuery('#shipToContactNumber').val() + extension;
    jQuery('#completedShipToAttn').html("Attn: " + fullName);
    jQuery('#completedShippingContactNumber').html(shippingContactPhoneNumber);
    jQuery('#completedEmailAddress').html(jQuery('#emailAddress').val());
    jQuery('#completedShipToAddress1').html(jQuery('#shipToAddress1').val());
    jQuery('#completedShipToAddress2').html(jQuery('#shipToAddress2').val());
    if (jQuery('#shipToStateProvinceGeoId').val() == "_NA_") {
        var shipToGeo = jQuery('#shipToCity').val()+", "+jQuery('#shipToCountryGeoId').val()+" "+jQuery('#shipToPostalCode').val();
    }
    else {
        var shipToGeo = jQuery('#shipToCity').val()+","+jQuery('#shipToStateProvinceGeoId').val() +" "+jQuery('#shipToCountryGeoId').val()+" "+jQuery('#shipToPostalCode').val();
    }
    jQuery('#completedShipToGeo').html(shipToGeo);
    // set shipToContactMechId in Billing form.
    jQuery('#shipToContactMechIdInBillingForm').val(jQuery('#shipToContactMechId').val());
}

function updateBillingSummary() {
    var fullName = jQuery('#firstNameOnCard').val() + " " +jQuery('#lastNameOnCard').val();
    jQuery('#completedBillToAttn').html("Attn: " + fullName);
    var extension = "";
    if (jQuery('#billToExtension').val()) {
        extension = "-" + jQuery('#billToExtension').val();
        }
    var billToPhoneNumber = jQuery('#billToCountryCode').val() + "-" + jQuery('#billToAreaCode').val() + "-" + jQuery('#billToContactNumber').val() + extension;
    jQuery('#completedBillToPhoneNumber').html(billToPhoneNumber);
    var cardNumber = "CC#:XXXXXXXXXXXX"+jQuery('#cardNumber').val().replace('-','').slice(12,16);
    jQuery('#completedCCNumber').html(cardNumber);
    var expiryDate = "Expires:"+jQuery('#expMonth').val()+"/"+jQuery('#expYear').val();
    jQuery('#completedExpiryDate').html(expiryDate);
    jQuery('#completedBillToAddress1').html(jQuery('#billToAddress1').val());
    jQuery('#completedBillToAddress2').html(jQuery('#billToAddress2').val());
    if (jQuery('#billToStateProvinceGeoId').val() == "_NA_") {
        var billToGeo = jQuery('#billToCity').val()+", "+jQuery('#billToCountryGeoId').val()+" "+jQuery('#billToPostalCode').val();
    }
    else {
        var billToGeo = jQuery('#billToCity').val()+", "+jQuery('#billToStateProvinceGeoId').val() +" "+jQuery('#billToCountryGeoId')+" "+jQuery('#billToPostalCode').val();
    }
    jQuery('#completedBillToGeo').html(billToGeo);
    jQuery('#paymentMethod').html(jQuery('#paymentMethodTypeId').val());
    jQuery('#billToContactMechIdInShipingForm').val(jQuery('#billToContactMechId'));
}

