/*
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
*/
function partyKeyEvents() {
    jQuery('#partyToSearch').bind('keypress', function(event) {
        code = event.keyCode ? event.keyCode : event.which;
        if (code.toString() == 13) {
            partySearch();
            return false;
        }
    });
    
    jQuery('#billingAddressSelected').bind('click', function(event) {
        jQuery('#billingAddress').show();
        jQuery('#shippingAddress').hide();
        jQuery('#billingLocation').val("Y");
        jQuery('#shippingLocation').val("N");
        customerAddressSelected();
        return false;
    });
    
    jQuery('#shippingAddressSelected').bind('click', function(event) {
        jQuery('#billingAddress').hide();
        jQuery('#shippingAddress').show();
        jQuery('#billingLocation').val("N");
        jQuery('#shippingLocation').val("Y");
        customerAddressSelected();
        return false;
    });
    
    jQuery('#searchPartyBy').bind('change', function(event) {
        partyToSearchFocus();
    });
    
    jQuery('#partySearchConfirm').bind('click', function(event) {
        partySearch();
        return false;
    });    
}

function customerAddressSelected() {
    if (jQuery('#billingLocation').val() == 'Y') {
        jQuery("#billingAddressSelected").addClass("selectedAddress");
        jQuery("#shippingAddressSelected").removeClass("selectedAddress");
        jQuery('#shipMethodPanel').hide();
        jQuery('#billingPanel').show();
    } else {            
        jQuery("#billingAddressSelected").removeClass("selectedAddress");
        jQuery("#shippingAddressSelected").addClass("selectedAddress");
        if (jQuery('#shipToSelected').val() != undefined && jQuery('#shipToSelected').val() == 'Y') {
            getShipMethods();
        }
    }
}

function getShipMethods() {
    var result = false;
    if (jQuery('#shipMethod').val() == "" || jQuery('#shipMethod').val() == null) {
        jQuery.ajax({url: 'GetShipMethods',
            type: 'post',
            success: function(data) {
                getResultOfGetShipMethods(data);
            },
            error: function(data) {
                getResultOfGetShipMethods(data);
            }
        });
    }
    jQuery('#billingPanel').hide();
    jQuery('#shipMethodPanel').show();
    jQuery('#shipMethod').bind('change', function(event) {
        setShipMethod();
    });
    return result;
}

function getResultOfGetShipMethods(data) {
    var result = false;
    var shipOptions = null; 
    var optionList = "";
    var serverError = getServerError(data);
    if (serverError != "") {
        jQuery('#shipMethodFormServerError').fadeIn('slow', function() {
            jQuery('#shipMethodFormServerError').html(serverError);
        });
    } else {
        jQuery('#shipMethodFormServerError').fadeOut('slow');
        shipOptions = data.shippingOptions;
        optionList = optionList + "<select id='shipMethod' name='shipMethod'>";
        jQuery(shipOptions).each(function(idx, shipOption) {
            if (shipOption.productStoreShipMethId){
                optionList = optionList + "<option value = '" + shipOption.shippingMethod + ":" + shipOption.productStoreShipMethId + "'> " + shipOption.shippingDesc  + " </option>";
            } else {
                optionList = optionList + "<option value = '" + shipOption.shippingMethod + "'> " + shipOption.shippingDesc  + " </option>";
            }
        });
        optionList = optionList + "</select>";
        jQuery('#shipMethodSelection').html(optionList);
        result = true;
    }
    return result;
}

function setShipMethod() {
    pleaseWait('Y');
    var result = false;
    var shipTotal = null;
    var shipMethod = null;
    var param = 'shipMethod=' + jQuery('#shipMethod').val();
    jQuery('#shipMethodFormServerError').fadeOut('slow');
    jQuery.ajax({url: 'SetShipMethod',
        data: param,
        type: 'post',
        async: false,
        success: function(data) {
            result = getResultOfSetShippingMethod(data);
        },
        error: function(data) {
            result = getResultOfSetShippingMethod(data);
        }
    });
    updateCart();
    pleaseWait('N');
    return result;
}

function getResultOfSetShippingMethod(data) {
    var result = false;
    shipTotal = data.shippingTotal;
    var serverError = getServerError(data);
    if (serverError != "") {
        jQuery('#shippingOptionFormServerError').fadeIn('slow', function() {
            jQuery('#shippingOptionFormServerError').html(serverError);
        });
        isShipOptionStepValidate = false;
    } else {
        isShipOptionStepValidate = true;
        jQuery('#selectedShipmentOption').html(data.shippingDescription);
        result = true;
    }
    return result;
}

function setParty(partyId, contactMechId, contactMechPurposeTypeId) {
    pleaseWait('Y');
    var param = 'partyId=' + partyId + '&contactMechId=' + contactMechId + '&contactMechPurposeTypeId=' + contactMechPurposeTypeId;
    jQuery.ajax({url: 'SetPartyToCart',
        data: param,
        type: 'post',
        async: false,
        success: function(data) {
            getResultOfSetParty(data);
        },
        error: function(data) {
            getResultOfSetParty(data);
        }
    });
    updateCustomerAddress();
    customerAddressSelected();
    hideOverlayDiv();
    pleaseWait('N');
}

function partyToSearchFocus() {
    hideOverlayDiv();
    jQuery('#partyToSearch').focus();
    return false;
}

function getResultOfSetParty(data) {
    var serverError = getServerError(data);
    if (serverError != "") {
        jQuery('#errors').fadeIn('slow', function() {
            jQuery('#errors').html(serverError);
        });
    } else {
        clearErrorMessages();
        jQuery('#partyToSearch').val('');
        updateCart();
        partyToSearchFocus();
    }
}

function partySearch() {
    pleaseWait('Y');
    var param = 'partyIdentificationTypeId=' + jQuery('#partyIdentificationTypeId').val() + 
                '&billingLocation=' + jQuery('#billingLocation').val() +
                '&shippingLocation=' + jQuery('#shippingLocation').val();
    if (jQuery('#searchPartyBy').val() == "lastName") {
        param = param + '&searchByPartyLastName=' + jQuery('#partyToSearch').val();
    } else if (jQuery('#searchPartyBy').val() == "firstName") {
        param = param + '&searchByPartyFirstName=' + jQuery('#partyToSearch').val();
    } else if (jQuery('#searchPartyBy').val() == "idValue") {
        param = param + '&searchByPartyIdValue=' + jQuery('#partyToSearch').val();
    }
    jQuery.ajax({url: 'FindParties',
        data: param,
        type: 'post',
        async: false,
        success: function(data) {
            var parties = data.partiesList;
            // automatically add party to cart if returned only one party
            if (parties.length == 1) {
                setParty(parties[0].partyId, parties[0].contactMechId, parties[0].contactMechPurposeTypeId);
            } else {
                buildPartiesResults(parties, 'Y');
            }
        },
        error: function(data) {
            alert("Error during party searching");
        }
    });
    pleaseWait('N');
}