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
    Event.observe($('editShipping'), 'click', function() {
        if (validateCart.validate()) {
            displayShippingPanel();
            isCartStepValidate = true;
        }
    });

    Event.observe($('openCartPanel'), 'click', function() {
        displayCartPanel();
    });

    // Shipping
    Event.observe($('editShippingOptions'), 'click', function() {
        if (isCartStepValidate) {
            if (validateShip.validate()) {
                processShippingAddress();
                displayShippingOptionPanel();
                isShipStepValidate = true;
            }
        }
    });

    Event.observe($('openShippingPanel'), 'click', function() {
        if (isCartStepValidate) {
            if (isShipStepValidate) {
                displayShippingPanel();
            }
        }
    });

    // Shipping Options
    Event.observe($('editBilling'), 'click', function() {
        if (isCartStepValidate && isShipStepValidate) {
            if (validateShipOption.validate()) {
                setShippingOption();
                getAssociatedBillingStateList(); 
                displayBillingPanel();
                isShipOptionStepValidate = true;
            }
        }
    });

    Event.observe($('openShippingOptionPanel'), 'click', function() {
        if (isCartStepValidate && isShipStepValidate && isShipOptionStepValidate) {
            displayShippingOptionPanel();
        }
    });

    // Billing
    Event.observe($('openBillingPanel'), 'click', function() {
        if (isBillStepValidate) {
            displayBillingPanel();
        }  
    });

    Event.observe($('openOrderSubmitPanel'), 'click', function() {
        if (isCartStepValidate && isShipStepValidate && isShipOptionStepValidate) {
            if (validateBill.validate()) {
                processBillingAndPayment();
                displayOrderSubmitPanel();
                isBillStepValidate = true;
            }
        }
    });
    
    // For Billing Address Same As Shipping
    Event.observe('useShippingAddressForBilling', 'click', function() {
        getAssociatedBillingStateList();
        useShippingAddressForBillingToggle();
    });
    
    // Initiate Observing Edit Cart Events
    initCartProcessObservers();

    Event.observe('processOrderButton', 'click', processOrder);

    // Autocompleter for shipping panel
    Event.observe($('shipToCountryGeo'), 'focus', getCountryList);
    Event.observe($('shipToCountryGeo'), 'change', isCountryChanged);
    Event.observe($('shipToCountryGeo'), 'blur', splitCountryNameFromIds);
    
    Event.observe($('shipToStateProvinceGeo'), 'focus', getAssociatedStateList);
    Event.observe($('shipToStateProvinceGeo'), 'blur', splitStateNameFromIds);

    // Get associate states for billing panel
    Event.observe($('billToCountryGeoId'), 'change', getAssociatedBillingStateList);
});

// Check server side error
function getServerError(data) {
    var servreErrorHash = [];
    var serverError = "";
    if (data._ERROR_MESSAGE_LIST_ != undefined) {
        servreErrorHash = data._ERROR_MESSAGE_LIST_;
        servreErrorHash.each(function(error) {
            serverError += error.message;
        });
    }
    if (data._ERROR_MESSAGE_ != undefined) {
        serverError = data._ERROR_MESSAGE_; 
    }
    return serverError;    
}

// Cart
function displayShippingPanel() {
    if (!$('editShippingPanel').visible()) {
        Effect.BlindDown('editShippingPanel', {duration: 0.5});
        Effect.BlindUp('editCartPanel', {duration: 0.5});
        Effect.BlindUp('editShippingOptionPanel', {duration: 0.5});
        Effect.BlindUp('editBillingPanel', {duration: 0.5});
        Effect.BlindUp('orderSubmitPanel', {duration: 0.5});

        Effect.Fade('shippingSummaryPanel', {duration: 0.5});
        Effect.Appear('cartSummaryPanel', {duration: 0.5});
        Effect.Appear('shippingOptionSummaryPanel', {duration: 0.5});
        Effect.Appear('billingSummaryPanel', {duration: 0.5});
        //Effect.Appear('orderSubmitPanel', {duration: 0.5});
    }
}
function displayCartPanel() {
    if (!$('editCartPanel').visible()) {
        Effect.BlindDown('editCartPanel', {duration: 0.5});
        Effect.BlindUp('editShippingPanel', {duration: 0.5});
        Effect.BlindUp('editShippingOptionPanel', {duration: 0.5});
        Effect.BlindUp('editBillingPanel', {duration: 0.5});
        Effect.BlindUp('orderSubmitPanel', {duration: 0.5});

        Effect.Fade('cartSummaryPanel', {duration: 0.5});
        Effect.Appear('shippingSummaryPanel', {duration: 0.5});
        Effect.Appear('shippingOptionSummaryPanel', {duration: 0.5});
        Effect.Appear('billingSummaryPanel', {duration: 0.5});
        //Effect.Appear('orderSubmitPanel', {duration: 0.5});
    }
}

// Shipping
function displayShippingOptionPanel() {
    if (!$('editShippingOptionPanel').visible() && isShipStepValidate) {
        Effect.BlindDown('editShippingOptionPanel', {duration: 0.5});
        Effect.BlindDown('shippingCompleted', {duration: 0.5});
        Effect.BlindUp('editCartPanel', {duration: 0.5});
        Effect.BlindUp('editShippingPanel', {duration: 0.5});
        Effect.BlindUp('editBillingPanel', {duration: 0.5});
        Effect.BlindUp('orderSubmitPanel', {duration: 0.5});

        Effect.Fade('shippingOptionSummaryPanel', {duration: 0.5});
        Effect.Appear('cartSummaryPanel', {duration: 0.5});
        Effect.Appear('shippingSummaryPanel', {duration: 0.5});
        Effect.Appear('billingSummaryPanel', {duration: 0.5});
        //Effect.Appear('orderSubmitPanel', {duration: 0.5});
    }
    setDataInShippingCompleted();
}

// Billing
function displayBillingPanel() {
    if (!$('editBillingPanel').visible() && isShipOptionStepValidate) {
        Effect.BlindDown('editBillingPanel', {duration: 0.5});
        Effect.BlindDown('shippingOptionCompleted', {duration: 0.5});
        Effect.BlindUp('editCartPanel', {duration: 0.5});
        Effect.BlindUp('editShippingPanel', {duration: 0.5});
        Effect.BlindUp('editShippingOptionPanel', {duration: 0.5});
        Effect.BlindUp('orderSubmitPanel', {duration: 0.5});

        Effect.Fade('billingSummaryPanel', {duration: 0.5});
        Effect.Appear('cartSummaryPanel', {duration: 0.5});
        Effect.Appear('shippingSummaryPanel', {duration: 0.5});
        Effect.Appear('shippingOptionSummaryPanel', {duration: 0.5});
        //Effect.Appear('orderSubmitPanel', {duration: 0.5});
    }
    setDataInShippingOptionCompleted();
}

// Order Submit
function displayOrderSubmitPanel() {
    if (!$('orderSubmitPanel').visible() && isBillStepValidate) {
        Effect.BlindDown('orderSubmitPanel', {duration: 0.5});
        Effect.BlindDown('billingCompleted', {duration: 0.5});
        Effect.BlindUp('editCartPanel', {duration: 0.5});
        Effect.BlindUp('editShippingPanel', {duration: 0.5});
        Effect.BlindUp('editShippingOptionPanel', {duration: 0.5});
        Effect.BlindUp('editBillingPanel', {duration: 0.5});

        Effect.Appear('cartSummaryPanel', {duration: 0.5});
        Effect.Appear('shippingSummaryPanel', {duration: 0.5});
        Effect.Appear('shippingOptionSummaryPanel', {duration: 0.5});
        Effect.Appear('billingSummaryPanel', {duration: 0.5});
        Effect.Fade('processingOrderButton', {duration: 0.5});
        Effect.Appear('processOrderButton', {duration: 0.5});
    }
    setDataInBillingCompleted();
}

function processShippingAddress() {
    var shipOptions = null;
    var optionList = [];
    new Ajax.Request('/ecommerce/control/createUpdateShippingAddress', {
        asynchronous: false, 
        onSuccess: function(transport) {
            var data = transport.responseText.evalJSON(true);
            var serverError = getServerError(data);
            if(serverError != "") {
                Effect.Appear('shippingFormServerError');
                $('shippingFormServerError').update(serverError);
                isShipStepValidate = false;
            } else {
                Effect.Fade('shippingFormServerError');
                isShipStepValidate = true;
                shipOptions = data.shippingOptions;
                shipOptions.each( function(shipOption) {
                    optionList.push("<option value = " + shipOption.shippingMethod + " > " + shipOption.shippingDesc + " </option>");
                });
                $('shipMethod').update(optionList);
                // Process Shipping data response.
                $('shippingPartyId').value = data.partyId;
                $('shippingContactMechId').value = data.shippingContactMechId;
                $('phoneContactMechId').value = data.phoneContactMechId;
                $('emailContactMechId').value = data.emailContactMechId;
                $('completedShippingMethod').update(data.shippingDescription);
            }
        }, parameters: $('shippingForm').serialize(), requestHeaders: {Accept: 'application/json'}
    });
}

function setDataInShippingCompleted() {
    var fullName = $('firstName').value + " " +$('lastName').value;
    var shippingContactPhoneNumber = $F('shippingCountryCode')+ "-" + $F('shippingAreaCode') 
            + "-" + $F('shippingContactNumber')+ "-" + $F('shippingExtension');
    $('completedShipToAttn').update("Attn: " + fullName);
    $('completedShippingContactNumber').update(shippingContactPhoneNumber);
    $('completedEmailAddress').update($('emailAddress').value);
    $('completedShipToAddress1').update($F('shipToAddress1'));
    $('completedShipToAddress2').update($('shipToAddress2').value);
    var shipToGeo = $('shipToCity').value+","+$('shipToStateProvinceGeoId').value +" "+$('shipToCountryGeoId').value+" "+$('shipToPostalCode').value;
    $('completedShipToGeo').update(shipToGeo);
    // set shippingContactMechId in Billing form.
    $('shippingContactMechIdInBillingForm').value = $F('shippingContactMechId');
}

// Shipping option
var shipTotal = null;
var shipMethod = null;
function setShippingOption() {
    new Ajax.Request('/ecommerce/control/setShippingOption', {
        asynchronous: false,
        onSuccess: function(transport) {
            var data = transport.responseText.evalJSON(true);
            shipMethod = data.shippingDescription;
            shipTotal = data.shippingTotal;
            var serverError = getServerError(data);
            if(serverError != "") {
                Effect.Appear('shippingOptionFormServerError');
                $('shippingOptionFormServerError').update(serverError);
                isShipOptionStepValidate = false;
            } else {
                Effect.Fade('shippingOptionFormServerError');
                isShipOptionStepValidate = true;
                $('shippingDescription').value = data.shippingDescription;
                $('shippingTotal').value = data.shippingTotal;
                $('cartGrandTotal').value = data.cartGrandTotal;
                $('totalSalesTax').value = data.totalSalesTax; 
            }
        }, parameters: $('shippingOptionForm').serialize(), requestHeaders: {Accept: 'application/json'}
    });
    updateCartData();
}

function setDataInShippingOptionCompleted() {
    $('selectedShipmentOption').update($('shipMethod').value);
}

// Billing
function useShippingAddressForBillingToggle() {
    $('billToStateProvinceGeoId').value = $F('shipToStateProvinceGeoId');
    if ($('useShippingAddressForBilling').checked) {
        $('billToAddress1').value = $F('shipToAddress1');
        $('billToAddress2').value = $F('shipToAddress2');
        $('billToCity').value = $F('shipToCity');
        $('billToPostalCode').value = $F('shipToPostalCode');
        $('billToCountryGeoId').value = $F('shipToCountryGeoId');
        Effect.BlindUp($('billingAddress'), {duration: 0.3});
    } else {
        Effect.BlindDown($('billingAddress'), {duration: 0.3});
    }
}

function processBillingAndPayment() {
    new Ajax.Request('/ecommerce/control/createUpdateBillingAndPayment', {
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
            }
        }, parameters: $('billingForm').serialize(), requestHeaders: {Accept: 'application/json'}
    });
}

function setDataInBillingCompleted() {
    var fullName = $F('firstNameOnCard') + " " +$F('lastNameOnCard');
    $('completedBillToAttn').update("Attn: " + fullName);
    var cardNumber = "CC#:XXXXXXXXXXXX"+$F('cardNumber').gsub('-','').slice(12,16);
    $('completedCCNumber').update(cardNumber);
    var expiryDate = "Expires:"+$F('expMonth')+"/"+$F('expYear');
    $('completedExpiryDate').update(expiryDate);
    $('completedBillToAddress1').update($F('billToAddress1'));
    $('completedBillToAddress2').update($F('billToAddress2'));
    var billToGeo = $F('billToCity')+","+$F('billToStateProvinceGeoId') +" "+$F('billToCountryGeoId')+" "+$F('billToPostalCode');
    $('completedBillToGeo').update(billToGeo);
    $('paymentMethod').update($F('paymentMethodTypeId'));
}

function initCartProcessObservers() {
    var cartForm = $('cartForm');
    Event.observe($('productPromoCode'), 'change', addPromoCode);
    var inputs = cartForm.getInputs('text');
    inputs.each(function(e) {
        if(e.id != 'productPromoCode') {
            Event.observe(e, 'keyup', cartItemQtyChanged);
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
    new Ajax.Request('/ecommerce/control/silentAddPromoCode', {
        asynchronous: false, 
        onSuccess: function(transport) {
            var data = transport.responseText.evalJSON(true);
            if (data._ERROR_MESSAGE_LIST_ != undefined) {
                $('cartFormServerError').update(data._ERROR_MESSAGE_LIST_);
            } else if (data._ERROR_MESSAGE_ != undefined) {
                $('cartFormServerError').update(data._ERROR_MESSAGE_);  
            } else {
                $('cartDiscountValue').update(data.displayOrderAdjustmentsTotalCurrencyFormatted);
                $('cartSubTotal').update(data.subTotalCurrencyFormatted);
                $('cartTotalShipping').update(data.totalShippingCurrencyFormatted);
                $('cartTotalSalesTax').update(data.totalSalesTaxCurrencyFormatted);
                $('cartDisplayGrandTotal').update(data.displayGrandTotalCurrencyFormatted);
                // update summary
                $('completedCartSubTotal').update(data.subTotalCurrencyFormatted);
                $('completedCartTotalShipping').update(data.totalShippingCurrencyFormatted);
                $('completedCartTotalSalesTax').update(data.totalSalesTaxCurrencyFormatted);
                $('completedCartDisplayGrandTotal').update(data.displayGrandTotalCurrencyFormatted);
                $('completedCartDiscount').update(data.displayOrderAdjustmentsTotalCurrencyFormatted); 
                if (data.displayDiscountTotal < 0) {
                    $('completedCartDiscountRow').show();
                } else {
                    $('completedCartDiscountRow').hide();
                }
                if (elementId !=undefined ) {
                    if ($(elementId).value == 0) {
                        var cartItemRowId = elementId.sub('qty_','cartItemRow_');
                        $(cartItemRowId).remove();
                        var cartItemDisplayRowId = elementId.sub('qty_','cartItemDisplayRow_');
                        $(cartItemDisplayRowId).remove();
                    }
                }
            }
        },
        parameters: {productPromoCodeId:$F('productPromoCode')}
    });
}

function getProductLineItemIndex(event, productId) {
    var itemIndex = null;
    var productIdParam = "productId=" + productId;
    var formValues = $('cartForm').serialize() + "&" + productIdParam;
    new Ajax.Request('/ecommerce/control/getShoppingCartItemIndex', {
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
    updateCartData(qtyId, formValues, 0, itemIndex); 
}

function cartItemQtyChanged(event) {
    var qtyElement = Event.element(event);
    var elementId = qtyElement.id;
    var productIdElementId = elementId.sub('qty_', 'cartLineProductId_');
    var productId = $(productIdElementId).value;
    if (qtyElement.value >= 0 && !isNaN(qtyElement.value)) {
        var itemIndex = getProductLineItemIndex(event, productId);
        var formValues = $('cartForm').serialize();
        updateCartData(elementId, formValues, qtyElement.value, itemIndex);
    } else {
        qtyElement.value = "";  
    }
}

function updateCartData(elementId, formValues, itemQty, itemIndex) {
    new Ajax.Request('/ecommerce/control/cartItemQtyUpdate', {
        asynchronous: true, 
        onSuccess: function(transport) {
            var data = transport.responseText.evalJSON(true);
            if (data.totalQuantity == 0) {
                $('emptyCartCheckoutPanel').show();
                $('checkoutPanel').hide();
            } else {
                // Used for edit cart
                $('cartSubTotal').update(data.subTotalCurrencyFormatted);
                $('cartDiscountValue').update(data.displayOrderAdjustmentsTotalCurrencyFormatted);
                $('cartTotalShipping').update(data.totalShippingCurrencyFormatted);
                $('cartTotalSalesTax').update(data.totalSalesTaxCurrencyFormatted);
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

var countryChange = true;
var autoCompleteCountries = null;
var countryList = [];

function getCountryList() {
    if (countryChange) {
    new Ajax.Request("getCountryList",
        { asynchronous: false,
          onSuccess: function(transport) {
          var data = transport.responseText.evalJSON(true);
          countryList = data.countryList;
          autoCompleteCountries = new Autocompleter.Local("shipToCountryGeo", "shipToCountries", countryList, {partialSearch: false});
        }
    });
    countryChange = false;
    }
}

function splitCountryNameFromIds() {
    $('shipToStateProvinceGeo').value = "";
    geoValues = $('shipToCountryGeo').value.split(': ');
    if(geoValues) {
        $('shipToCountryGeo').value = geoValues[0];
        if(geoValues[1] != 'undefined' || geoValues[1] != null) {
            countryList.each(function(country){
                geo = country.split(': ');
                if(geoValues[0] == geo[0]){
                    geoValues[1] = geo[1];
                }
            });
        }
        $('shipToCountryGeoId').value = geoValues[1];
    }
}

var stateChange = true;
var autoCompleteStates = null;
var stateList = [];

function getAssociatedStateList() {
    if (stateChange) {
    new Ajax.Request("getAssociatedStateList",
        { asynchronous: false,
          parameters: $('shippingForm').serialize(),
          onSuccess: function(transport) {
          var data = transport.responseText.evalJSON(true);
          stateList = data.stateList;
          autoCompleteStates = new Autocompleter.Local("shipToStateProvinceGeo", "shipToStates", stateList, {partialSearch: false});
        }
    });
    stateChange = false;
    }
}

function splitStateNameFromIds() {
    geoValues = $('shipToStateProvinceGeo').value.split(': ');
    if(geoValues) {
        $('shipToStateProvinceGeo').value = geoValues[0];
        if(geoValues[1] != 'undefined' || geoValues[1] != null) {
            stateList.each(function(state){
                geo = state.split(': ');
                if(geoValues[0] == geo[0]){
                    geoValues[1] = geo[1];
                }
            });
            $('shipToStateProvinceGeoId').value = geoValues[1];
        }
    }
}

function getAssociatedBillingStateList() {
    var optionList = [];
    new Ajax.Request("getAssociatedStateList",
    { asynchronous: false,
        parameters: $('billingForm').serialize(),
        onSuccess: function(transport) {
          var data = transport.responseText.evalJSON(true);
          stateList = data.stateList;
          stateList.each(function(state) {
            geoVolues = state.split(': ');
            optionList.push("<option value = " + geoVolues[1] + " >"+geoVolues[0]+"</option>");
          });
          $('billToStateProvinceGeoId').update(optionList);
        }
    });
}
// This way associated state list will be fetched only when a user do any changes to country
function isCountryChanged() {
    stateChange = true;
}