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

Event.observe(window, 'load', function() {
    // Cart
    var validateCart = new Validation('cartForm', {immediate: true, onSubmit: false});
    var validateShip = new Validation('shippingForm', {immediate: true, onSubmit: false});
    var validateShipOption = new Validation('shippingOptionForm', {immediate: true, onSubmit: false});
    var validateBill = new Validation('billingForm', {immediate: true, onSubmit: false});

    // Goto Edit Cart Panel
    Event.observe($('openCartPanel'), 'click', function() {
        showEditCartPanel();
        updateShippingSummary();
    });

    // Update Shipping Address
    Event.observe($('savePartyAndShippingContact'), 'click', function() {
        if (validateShip.validate()) {
            Effect.Fade('savePartyAndShippingContact', {duration: 0.0});
            Effect.Appear('processingShippingOptions', {duration: 0.0});
            if (createUpdateCustomerAndShippingAddress()){
                showEditShippingOptionPanel();
            }
            Effect.Fade('processingShippingOptions', {duration: 0.0});
            Effect.Appear('savePartyAndShippingContact', {duration: 0.0});
        }
    });

    // Goto Edit Shipping  panel
    Event.observe($('updateShoppingCart'), 'click', function() {
        if (validateCart.validate()) {
            showEditShippingPanel();
        }
    });
    // Goto Edit Shipping Panel
    Event.observe($('openShippingPanel'), 'click', function() {
        showEditShippingPanel();
        setShippingOption();
    });

    // Set Shipping Method to card and goto Billing step
    Event.observe($('saveShippingMethod'), 'click', function() {
        Effect.Fade('saveShippingMethod', {duration: 0.0});
        Effect.Appear('processingBilling', {duration: 0.0});
        if (setShippingOption()){
            showEditBillingPanel();
        }
        Effect.Fade('processingBilling', {duration: 0.0});
        Effect.Appear('saveShippingMethod', {duration: 0.0});
    });

    Event.observe($('openShippingOptionPanel'), 'click', function() {
        showEditShippingOptionPanel();
        updateBillingSummary();
    });

    // Billing
    Event.observe($('openBillingPanel'), 'click', function() {
        showEditBillingPanel();
    });

    Event.observe($('savePaymentAndBillingContact'), 'click', function() {
        if (validateBill.validate()) {
            Effect.Fade('savePaymentAndBillingContact', {duration: 0.0});
            Effect.Appear('processingOrderSubmitPanel', {duration: 0.0});
            if (processBillingAndPayment()) {
                showOrderSubmitPanel();
            }
            Effect.Fade('processingOrderSubmitPanel', {duration: 0.0});
            Effect.Appear('savePaymentAndBillingContact', {duration: 0.0});
        }
    });

    // For Billing Address Same As Shipping
    Event.observe('useShippingAddressForBilling', 'click', function() {
        useShippingAddressForBillingToggle();
        validateBill.validate();
    });

    // Initiate Observing Edit Cart Events
    initCartProcessObservers();

    Event.observe('processOrderButton', 'click', processOrder);

    if ($('shippingForm')) {
        // Get associate states for Shipping Information
        Event.observe($('shipToCountryGeoId'), 'change', function(){
            getAssociatedStateList('shipToCountryGeoId', 'shipToStateProvinceGeoId', 'advice-required-shipToStateProvinceGeoId', 'shipToStates');
        });
        if ($('userLoginId')) {
            var stateValue = $('shipToStateProvinceGeoId').value;
            getAssociatedStateList('shipToCountryGeoId', 'shipToStateProvinceGeoId', 'advice-required-shipToStateProvinceGeoId', 'shipToStates');
            $('shipToStateProvinceGeoId').value = stateValue;
            stateValue = $('billToStateProvinceGeoId').value;
            getAssociatedStateList('billToCountryGeoId', 'billToStateProvinceGeoId', 'advice-required-billToStateProvinceGeoId', 'billToStates');
            $('billToStateProvinceGeoId').value = stateValue;
        } else {
            getAssociatedStateList('shipToCountryGeoId', 'shipToStateProvinceGeoId', 'advice-required-shipToStateProvinceGeoId', 'shipToStates');
            getAssociatedStateList('billToCountryGeoId', 'billToStateProvinceGeoId', 'advice-required-billToStateProvinceGeoId', 'billToStates');
        }
    }
    if ($('billingForm')) {
        // Get associate states for Billing Information
        Event.observe($('billToCountryGeoId'), 'change', function() {
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
        serverErrorHash.each(function(error) {
            serverError += error.message;
        });
    }
    if (data._ERROR_MESSAGE_ != undefined) {
        serverError = data._ERROR_MESSAGE_;
    }
    return serverError;
}

// Begin Show/Hide Step panels

function hideEditCartPanel() {
    if ($('editCartPanel').visible() ) {
        Effect.BlindUp('editCartPanel',{duration: 0.0});
        Effect.BlindDown('cartSummaryPanel',{duration: 0.0});
    }
}
function hideEditShippingPanel() {
     if ($('editShippingPanel').visible()) {
         Effect.BlindUp('editShippingPanel', {duration: 0.0});
         Effect.BlindDown('shippingSummaryPanel', {duration: 0.0});
     }
}
function hideEditShippingOptionPanel() {
     if ($('editShippingOptionPanel').visible()) {
         Effect.BlindUp('editShippingOptionPanel', {duration: 0.0});
         Effect.BlindDown('shippingOptionSummaryPanel', {duration: 0.0});
     }
}
function hideEditBillingPanel() {
    if ($('editBillingPanel').visible()) {
        Effect.BlindUp('editBillingPanel', {duration: 0.0});
        Effect.BlindDown('billingSummaryPanel', {duration: 0.0});
    }
}
function hideOrderSubmitPanel() {
    if ($('orderSubmitPanel').visible()) {
        Effect.BlindUp('orderSubmitPanel', {duration: 0.0});
        Effect.Fade('processingOrderButton', {duration: 0.0});

    }
}

function showEditCartPanel() {
    if (!$('editCartPanel').visible() ) {
        Effect.BlindUp('cartSummaryPanel',{duration: 0.0});
        hideEditShippingPanel();
        hideEditShippingOptionPanel();
        hideEditBillingPanel();
        hideOrderSubmitPanel();
        Effect.BlindDown('editCartPanel',{duration: 0.0});
    }
}

function showEditShippingPanel() {
     if (!$('editShippingPanel').visible()) {
         Effect.BlindUp('shippingSummaryPanel', {duration: 0.0});
         hideEditCartPanel();
         hideEditShippingOptionPanel();
         hideEditBillingPanel();
         hideOrderSubmitPanel();
         Effect.BlindDown('editShippingPanel');

     }
}

function showEditShippingOptionPanel() {
     if (!$('editShippingOptionPanel').visible()) {
         Effect.BlindUp('shippingOptionSummaryPanel', {duration: 0.0});
         hideEditCartPanel();
         hideEditShippingPanel();
         hideEditBillingPanel();
         hideOrderSubmitPanel();
         Effect.BlindDown('editShippingOptionPanel', {duration: 0.0});
     }
}

function showEditBillingPanel() {

    if (!$('editBillingPanel').visible()) {
         Effect.BlindUp('billingSummaryPanel', {duration: 0.0});
         hideEditCartPanel();
         hideEditShippingPanel();
         hideEditShippingOptionPanel();
         hideOrderSubmitPanel();
         Effect.BlindDown('editBillingPanel', {duration: 0.0});
    }
    if ($F('shipToContactMechId') != $F('billToContactMechId')) {
        $('useShippingAddressForBilling').checked = false;
        Effect.BlindDown($('billingAddress'), {duration: 0.3});
        $('useShippingAddressForBilling').value = "N";
    }
}

function showOrderSubmitPanel() {
    if (!$('orderSubmitPanel').visible()) {
         hideEditCartPanel();
         hideEditShippingPanel();
         hideEditShippingOptionPanel();
         hideEditBillingPanel();
         Effect.BlindDown('orderSubmitPanel', {duration: 0.0});
    }
}

// End Show/Hide Step panels


function createUpdateCustomerAndShippingAddress() {
    var result = false;

    new Ajax.Request('createUpdateShippingAddress', {
        asynchronous: false,
        onSuccess: function(transport) {
            var data = transport.responseText.evalJSON(true);
            var serverError = getServerError(data);
            if (serverError != "") {
                $('shippingFormServerError').update(serverError);
            } else {
                Effect.Fade('shippingFormServerError');
                // Process Shipping data response.
                $('shipToPartyId').value = data.partyId;
                $('billToPartyId').value = data.partyId;
                $('shipToContactMechId').value = data.shipToContactMechId;
                $('shipToPhoneContactMechId').value = data.shipToPhoneContactMechId;
                $('emailContactMechId').value = data.emailContactMechId;
                //$('completedShippingMethod').update(data.shippingDescription);
                updateShippingSummary();
                getShipOptions();
                result = true;
            }
        }, parameters: $('shippingForm').serialize(), requestHeaders: {Accept: 'application/json'}
    });
    return result;
}

function getShipOptions() {
    var result = false;
    var shipOptions = null;
    var optionList = [];
    if ($F('shipMethod') == "" || $F('shipMethod') == null) {
	    new Ajax.Request('getShipOptions', {
            asynchronous: false,
            onSuccess: function(transport) {
                var data = transport.responseText.evalJSON(true);
                var serverError = getServerError(data);
                if (serverError != "") {
                    Effect.Appear('shippingFormServerError');
                    $('shippingFormServerError').update(serverError);
                    isShipStepValidate = false;
                } else {
                    Effect.Fade('shippingFormServerError');
                    isShipStepValidate = true;
                    shipOptions = data.shippingOptions;
                    shipOptions.each( function(shipOption) {
                        if (shipOption.productStoreShipMethId){
                            optionList.push("<option value = " + shipOption.shippingMethod + ":" + shipOption.productStoreShipMethId + " > " + shipOption.shippingDesc  + " </option>");
                        } else {
                            optionList.push("<option value = " + shipOption.shippingMethod + " > " + shipOption.shippingDesc  + " </option>");
                        }
                    });
                    $('shipMethod').update(optionList);
                    result = true;
                }
            }, requestHeaders: {Accept: 'application/json'}
        });
    }
    return result;
}

// Shipping option
function setShippingOption() {
    var result = false;
    var shipTotal = null;
    var shipMethod = null;
    Effect.Fade('shippingOptionFormServerError');
    new Ajax.Request('setShippingOption', {
        asynchronous: false,
        onSuccess: function(transport) {
            var data = transport.responseText.evalJSON(true);
            shipTotal = data.shippingTotal;
            var serverError = getServerError(data);
            if(serverError != "") {
                Effect.Appear('shippingOptionFormServerError');
                $('shippingOptionFormServerError').update(serverError);
                isShipOptionStepValidate = false;
            } else {
                isShipOptionStepValidate = true;
                $('selectedShipmentOption').update(data.shippingDescription);
                //$('shippingDescription').value = data.shippingDescription;
                //$('shippingTotal').value = data.shippingTotal;
                //$('cartGrandTotal').value = data.cartGrandTotal;
                //$('totalSalesTax').value = data.totalSalesTax;
                result = true;
            }
        }, parameters: $('shippingOptionForm').serialize(), requestHeaders: {Accept: 'application/json'}
    });
    updateCartData();
    return result;
}

// Billing
function useShippingAddressForBillingToggle() {
    if ($('useShippingAddressForBilling').checked) {
        $('billToAddress1').value = $F('shipToAddress1');
        $('billToAddress2').value = $F('shipToAddress2');
        $('billToCity').value = $F('shipToCity');
        $('billToPostalCode').value = $F('shipToPostalCode');
        $('billToCountryGeoId').value = $F('shipToCountryGeoId');
        getAssociatedStateList('billToCountryGeoId', 'billToStateProvinceGeoId','advice-required-billToStateProvinceGeoId','billToStates');
        $('useShippingAddressForBilling').value = "Y";
        $('billToStateProvinceGeoId').value = $F('shipToStateProvinceGeoId');
        Effect.BlindUp($('billingAddress'), {duration: 0.3});
    } else {
        Effect.BlindDown($('billingAddress'), {duration: 0.3});
        $('useShippingAddressForBilling').value = "N";
    }
}

function processBillingAndPayment() {
    var result = false;
    new Ajax.Request('createUpdateBillingAndPayment', {
        asynchronous: false,
        onSuccess: function(transport) {
            var data = transport.responseText.evalJSON(true);
            var serverError = getServerError(data);
            if(serverError != "") {
                Effect.Appear('billingFormServerError');
                $('billingFormServerError').update(serverError);
                isBillStepValidate = false;
            } else {
                Effect.Fade('billingFormServerError');
                isBillStepValidate = true;
                $('billToContactMechId').value = data.billToContactMechId;
                $('paymentMethodId').value = data.paymentMethodId;
                $('billToPhoneContactMechId').value = data.billToPhoneContactMechId;
                updateBillingSummary();
                result = true;
            }
        }, parameters: $('billingForm').serialize(), requestHeaders: {Accept: 'application/json'}
    });
    return result;

}

function initCartProcessObservers() {
    var cartForm = $('cartForm');
    Event.observe($('productPromoCode'), 'change', addPromoCode);
    var inputs = cartForm.getInputs('text');
    inputs.each(function(e) {
        if(e.id != 'productPromoCode') {
            Event.observe(e, 'change', cartItemQtyChanged);
        }
    });
    var removeLinks = cartForm.getElementsByTagName('a');
    var links = $A(removeLinks);
    links.each( function(e) {
        Event.observe(e, 'click', removeItem);
    });
    if ($('initializedCompletedCartDiscount') != undefined && $('initializedCompletedCartDiscount').value == 0) {
        $('completedCartDiscountRow').hide();
    }
}

function addPromoCode() {
    new Ajax.Request('silentAddPromoCode', {
        asynchronous: false,
        onSuccess: function(transport) {
            var data = transport.responseText.evalJSON(true);
            var serverError = getServerError(data);
            if(serverError != "") {
                Effect.Appear('cartFormServerError');
                $('cartFormServerError').update(serverError);
            } else {
                Effect.Fade('cartFormServerError');
                updateCartData();
            }
        },
        parameters: {productPromoCodeId:$F('productPromoCode')}
    });
}

function getProductLineItemIndex(event, productId) {
    var itemIndex = null;
    var productIdParam = "productId=" + productId;
    var formValues = $('cartForm').serialize() + "&" + productIdParam;
    new Ajax.Request('getShoppingCartItemIndex', {
        asynchronous: false,
        onSuccess: function(transport) {
            var data = transport.responseText.evalJSON(true);
            itemIndex = data.itemIndex;
        },
        parameters: formValues
    });
    return itemIndex;
}

function removeItem(event) {
    var removeElement = Event.element(event);
    var elementId = removeElement.id;
    var qtyId = elementId.sub('remove_', 'qty_');
    var productIdElementId =  elementId.sub('remove_', 'cartLineProductId_');
    var productId = $(productIdElementId).value;
    var itemIndex = getProductLineItemIndex(event,productId);
    var formValues = "update_" + itemIndex + "= 0";
    if ($(qtyId).value == '' || isNaN(qtyId.value)) {
        $(qtyId).value = 0;
    }
    updateCartData(qtyId, formValues, 0, itemIndex);
}

function cartItemQtyChanged(event) {
    var qtyElement = Event.element(event);
    var elementId = qtyElement.id;
    var productIdElementId = elementId.sub('qty_', 'cartLineProductId_');
    var productId = $(productIdElementId).value;
    if (qtyElement.value >= 0 && !isNaN(qtyElement.value)) {
        var itemIndex = getProductLineItemIndex(event, productId);
        qtyParam = "update_" + itemIndex +"="+qtyElement.value;
        var formValues = $('cartForm').serialize() + '&' + qtyParam;
        updateCartData(elementId, formValues, qtyElement.value, itemIndex);
    } else {
        qtyElement.value = "";
    }
}

function updateCartData(elementId, formValues, itemQty, itemIndex) {
    new Ajax.Request('cartItemQtyUpdate', {
        asynchronous: true,
        onSuccess: function(transport) {
            var data = transport.responseText.evalJSON(true);
            if (data.totalQuantity == 0) {
                $('emptyCartCheckoutPanel').show();
                $('checkoutPanel').hide();
                $('microCartNotEmpty').hide();
                $('microCartEmpty').show();
                $('quickCheckoutEnabled').hide();
                $('quickCheckoutDisabled').show();
                $('onePageCheckoutEnabled').hide();
                $('onePageCheckoutDisabled').show();
            } else {
                // Used for edit cart
                $('microCartQuantity').update(data.totalQuantity);
                $('cartSubTotal').update(data.subTotalCurrencyFormatted);
                $('cartDiscountValue').update(data.displayOrderAdjustmentsTotalCurrencyFormatted);
                $('cartTotalShipping').update(data.totalShippingCurrencyFormatted);
                $('cartTotalSalesTax').update(data.totalSalesTaxCurrencyFormatted);
                $('microCartTotal').update(data.displayGrandTotalCurrencyFormatted);
                $('cartDisplayGrandTotal').update(data.displayGrandTotalCurrencyFormatted);
                // Used for summary
                $('completedCartSubTotal').update(data.subTotalCurrencyFormatted);
                $('completedCartTotalShipping').update(data.totalShippingCurrencyFormatted);
                $('completedCartTotalSalesTax').update(data.totalSalesTaxCurrencyFormatted);
                $('completedCartDisplayGrandTotal').update(data.displayGrandTotalCurrencyFormatted);
                $('completedCartDiscount').update(data.displayOrderAdjustmentsTotalCurrencyFormatted);
                if (elementId != undefined && $(elementId).value != "") {
                    if (itemQty == 0) {
                        var cartItemRowId = elementId.sub('qty_','cartItemRow_');
                        $(cartItemRowId).remove();
                        var cartItemDisplayRowId = elementId.sub('qty_','cartItemDisplayRow_');
                        $(cartItemDisplayRowId).remove();
                    } else {
                        var itemsHash = $H(data.cartItemData);
                        var lineTotalId = elementId.sub('qty_','displayItem_');
                        var lineDiscountTotalId = elementId.sub('qty_','addPromoCode_');
                        var lineItemTotal = itemsHash.get("displayItemSubTotalCurrencyFormatted_"+itemIndex);
                        var lineItemAdjustment = itemsHash.get("displayItemAdjustment_"+itemIndex);
                        $(lineTotalId).update(lineItemTotal);
                        $(lineDiscountTotalId).update(lineItemAdjustment);
                        var completedLineItemQtyId =  elementId.sub('qty_','completedCartItemQty_');
                        $(completedLineItemQtyId).update($(elementId).value);
                        $('completedCartItemAdjustment_'+itemIndex).update(lineItemAdjustment);
                        var completedCartItemSubTotalId = elementId.sub('qty_','completedCartItemSubTotal_');
                        $(completedCartItemSubTotalId).update(lineItemTotal);
                    }
                }
            }
        },
        parameters: formValues
    });
}

function processOrder() {
    $('processOrderButton').disabled = true ;
    Effect.Fade('processOrderButton', {duration: 0.1});
    Effect.Appear('processingOrderButton', {duration: 0.1});
    $('orderSubmitForm').submit();
}

function getAssociatedBillingStateList(formName, divId) {
    var optionList = [];
    new Ajax.Request("getAssociatedStateList", {
        asynchronous: false,
        parameters: $(formName).serialize(),
        onSuccess: function(transport) {
            var data = transport.responseText.evalJSON(true);
            stateList = data.stateList;
            stateList.each(function(state) {
                geoVolues = state.split(': ');
                optionList.push("<option value = "+geoVolues[1]+" >"+geoVolues[0]+"</option>");
            });
            $(divId).update(optionList);
        }
    });
}

function updateShippingSummary() {
    var fullName = $('firstName').value + " " +$('lastName').value;
    var extension = "";
    if ($F('shipToExtension')) {
        extension = "-" + $F('shipToExtension');
        }
    var shippingContactPhoneNumber = $F('shipToCountryCode')+ "-" + $F('shipToAreaCode')
        + "-" + $F('shipToContactNumber') + extension;
    $('completedShipToAttn').update("Attn: " + fullName);
    $('completedShippingContactNumber').update(shippingContactPhoneNumber);
    $('completedEmailAddress').update($('emailAddress').value);
    $('completedShipToAddress1').update($F('shipToAddress1'));
    $('completedShipToAddress2').update($('shipToAddress2').value);
    if ($('shipToStateProvinceGeoId').value == "_NA_") {
        var shipToGeo = $('shipToCity').value+", "+$('shipToCountryGeoId').value+" "+$('shipToPostalCode').value;
    }
    else {
        var shipToGeo = $('shipToCity').value+","+$('shipToStateProvinceGeoId').value +" "+$('shipToCountryGeoId').value+" "+$('shipToPostalCode').value;
    }
    $('completedShipToGeo').update(shipToGeo);
    // set shipToContactMechId in Billing form.
    $('shipToContactMechIdInBillingForm').value = $F('shipToContactMechId');
}

function updateBillingSummary() {
	var fullName = $F('firstNameOnCard') + " " +$F('lastNameOnCard');
    $('completedBillToAttn').update("Attn: " + fullName);
    var extension = "";
    if ($F('billToExtension')) {
        extension = "-" + $F('billToExtension');
        }
    var billToPhoneNumber = $F('billToCountryCode') + "-" + $F('billToAreaCode') + "-" + $F('billToContactNumber') + extension;
    $('completedBillToPhoneNumber').update(billToPhoneNumber);
    var cardNumber = "CC#:XXXXXXXXXXXX"+$F('cardNumber').gsub('-','').slice(12,16);
    $('completedCCNumber').update(cardNumber);
    var expiryDate = "Expires:"+$F('expMonth')+"/"+$F('expYear');
    $('completedExpiryDate').update(expiryDate);
    $('completedBillToAddress1').update($F('billToAddress1'));
    $('completedBillToAddress2').update($F('billToAddress2'));
    if ($F('billToStateProvinceGeoId') == "_NA_") {
        var billToGeo = $F('billToCity')+", "+$F('billToCountryGeoId')+" "+$F('billToPostalCode');
    }
    else {
        var billToGeo = $F('billToCity')+", "+$F('billToStateProvinceGeoId') +" "+$F('billToCountryGeoId')+" "+$F('billToPostalCode');
    }
    $('completedBillToGeo').update(billToGeo);
    $('paymentMethod').update($F('paymentMethodTypeId'));
    $('billToContactMechIdInShipingForm').value = $F('billToContactMechId');
}

