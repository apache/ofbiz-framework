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
    Event.observe($('editShipping'), 'click', function() {
        displayShippingPanel();
    });

    Event.observe($('openCartPanel'), 'click', function() {
        displayCartPanel();
    });

    // Shipping
    Event.observe($('editShippingOptions'), 'click', function() {
        processShippingAddress();
        displayShippingOptionPanel();
    });

    Event.observe($('openShippingPanel'), 'click', function() {
        displayShippingPanel();
    });

    // Shipping Options
    Event.observe($('editBilling'), 'click', function() {
        // TODO : Will be uncomment soon.
        //setShippingOption(); 
        displayBillingPanel();
    });

    Event.observe($('openShippingOptionPanel'), 'click', function() {
        displayShippingOptionPanel();
    });

    // Billing
    Event.observe($('openBillingPanel'), 'click', function() {
        displayBillingPanel();
    });

    Event.observe($('openOrderSubmitPanel'), 'click', function() {
        processBillingAndPayment();
        displayOrderSubmitPanel();
    });
    
    //  For Billing Address Same As Shipping
    Event.observe('useShippingAddressForBilling', 'click', useShippingAddressForBillingToggel);
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
    new Ajax.Request('/ecommerce/control/createUpdateShippingAddress', {
        asynchronous: false, 
        onSuccess: function(transport) {
            var data = transport.responseText.evalJSON(true);
            console.log(data);
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