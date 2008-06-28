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
    	displayShippingOptionPanel();
    });

    Event.observe($('openShippingPanel'), 'click', function() {
    	displayShippingPanel();
    });

    // Shipping Options
    Event.observe($('editBilling'), 'click', function() {
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
    	displayOrderSubmitPanel();
    });
});

// Cart
function displayShippingPanel() {
    if(!$('editShippingPanel').visible()) {
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
    if(!$('editCartPanel').visible()) {
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
    if(!$('editShippingOptionPanel').visible()) {
        Effect.BlindDown('editShippingOptionPanel', {duration: 0.5});
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
}

// Billing
function displayBillingPanel() {
    if(!$('editBillingPanel').visible()) {
        Effect.BlindDown('editBillingPanel', {duration: 0.5});
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
}

// Order Submit
function displayOrderSubmitPanel() {
    if(!$('orderSubmitPanel').visible()) {
        Effect.BlindDown('orderSubmitPanel', {duration: 0.5});
        Effect.BlindUp('editCartPanel', {duration: 0.5});
        Effect.BlindUp('editShippingPanel', {duration: 0.5});
        Effect.BlindUp('editShippingOptionPanel', {duration: 0.5});
        Effect.BlindUp('editBillingPanel', {duration: 0.5});

        //Effect.Fade('billingSummaryPanel', {duration: 0.5});
        Effect.Appear('cartSummaryPanel', {duration: 0.5});
        Effect.Appear('shippingSummaryPanel', {duration: 0.5});
        Effect.Appear('shippingOptionSummaryPanel', {duration: 0.5});
        Effect.Appear('billingSummaryPanel', {duration: 0.5});
    }    	
}