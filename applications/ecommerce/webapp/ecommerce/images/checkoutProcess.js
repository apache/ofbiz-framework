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
 
Event.observe(window, 'load', function() {
	// Cart
    var isCartStepValidate = false;
    var isShipStepValidate = false;
    var isShipOptionStepValidate = false;
    var isBillStepValidate = false;
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
    
    //  For Billing Address Same As Shipping
    Event.observe('useShippingAddressForBilling', 'click', useShippingAddressForBillingToggel);
    
    // Initiate Observing Edit Cart Events
    initCartProcessObservers();
});

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
    if (!$('editShippingOptionPanel').visible()) {
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
    if (!$('editBillingPanel').visible()) {
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
    if (!$('orderSubmitPanel').visible()) {
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
            shipOptions = data.shippingOptions;
            shipOptions.each( function(shipOption) {
                optionList.push("<option value = " + shipOption.shippingMethod + " > " + shipOption.shippingDesc + " </option>");
            });
            $('shipMethod').update(optionList);
            if (data._ERROR_MESSAGE_LIST_ != undefined) {
                console.log(data._ERROR_MESSAGE_LIST_);
            } else if (data._ERROR_MESSAGE_ != undefined) {
                if (data._ERROR_MESSAGE_LIST_ == "SessionTimedOut") {
                    console.log('session time out');
                }
                console.log(data._ERROR_MESSAGE_); 
            } else {
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
            console.log(data);
            shipMethod = data.shippingDescription;
            shipTotal = data.shippingTotal;
            if (data._ERROR_MESSAGE_LIST_ != undefined) {
                console.log(data._ERROR_MESSAGE_LIST_);
            } else if (data._ERROR_MESSAGE_ != undefined) {
                if (data._ERROR_MESSAGE_LIST_ == "SessionTimedOut") {
                    console.log('session time out');
                }
                console.log(data._ERROR_MESSAGE_); 
            } else {
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
    var shipOpt = shipMethod +'-'+shipTotal;
    $('selectedShipmentOption').update(shipOpt);
}

// Billing
function useShippingAddressForBillingToggel() {
    if($('useShippingAddressForBilling').checked) {
        $('billToAddress1').value = $F('shipToAddress1');
        $('billToAddress2').value = $F('shipToAddress2');
        $('billToCity').value = $F('shipToCity');
        $('billToStateProvinceGeoId').value = $F('shipToStateProvinceGeoId');
        $('billToPostalCode').value = $F('shipToPostalCode');
        $('billToCountryGeoId').value = $F('shipToCountryGeoId');
        Effect.BlindUp($('billingAddress'), {duration: 0.5});
    } else {
        Effect.BlindDown($('billingAddress'), {duration: 0.5});
    }
}

function processBillingAndPayment() {
    new Ajax.Request('/ecommerce/control/createUpdateBillingAndPayment', {
        asynchronous: false, 
        onSuccess: function(transport) {
            var data = transport.responseText.evalJSON(true);
            console.log(data);
            if (data._ERROR_MESSAGE_LIST_ != undefined) {
                console.log(data._ERROR_MESSAGE_LIST_);
            } else if (data._ERROR_MESSAGE_ != undefined) {
                if (data._ERROR_MESSAGE_LIST_="SessionTimedOut") {
                    console.log('session time out');
                }
                console.log(data._ERROR_MESSAGE_);
            } else {
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
    var cartFormElement = $('cartForm');
    var inputs = cartFormElement.getInputs('text');
    inputs.each( function(e) {
        Event.observe(e, 'keyup', cartItemQtyChanged);
    });
    var removeLinks = cartFormElement.getElementsByTagName('a');
    var links = $A(removeLinks);
    links.each( function(e) {
        Event.observe(e, 'click', removeItem);
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
                $('cartDiscountValue').update(data.displayDiscountTotalCurrencyFormatted);
                $('cartTotalShipping').update(data.totalShippingCurrencyFormatted);
                $('cartTotalSalesTax').update(data.totalSalesTaxCurrencyFormatted);
                $('cartDisplayGrandTotal').update(data.displayGrandTotalCurrencyFormatted);
                // Used for summary 
                $('completedCartSubTotal').update(data.subTotalCurrencyFormatted);
                $('completedCartTotalShipping').update(data.totalShippingCurrencyFormatted);
                $('completedCartTotalSalesTax').update(data.totalSalesTaxCurrencyFormatted);
                $('completedCartDisplayGrandTotal').update(data.displayGrandTotalCurrencyFormatted);
                $('completedCartDiscount').update(data.displayDiscountTotalCurrencyFormatted);
                if (elementId != undefined && $(elementId).value != "") {
                    if (itemQty == 0) {
                        var cartItemRowId = elementId.sub('qty_','cartItemRow_');
                        $(cartItemRowId).remove();
                        var cartItemDisplayRowId = elementId.sub('qty_','cartItemDisplayRow_');
                        $(cartItemDisplayRowId).remove();
                    } else {
                        var itemsHash = $H(data.cartItemData);
                        var lineTotalId = elementId.sub('qty_','displayItem_');
                        var lineItemTotal = itemsHash.get("displayItemSubTotalCurrencyFormatted_"+itemIndex);
                        $(lineTotalId).update(lineItemTotal);
                        var completedLineItemQtyId =  elementId.sub('qty_','completedCartItemQty_');
                        $(completedLineItemQtyId).update($(elementId).value);
                        var completedCartItemSubTotalId = elementId.sub('qty_','completedCartItemSubTotal_');
                        $(completedCartItemSubTotalId).update(lineItemTotal);
                    }
                }
            }
        },
        parameters: formValues
    });
}