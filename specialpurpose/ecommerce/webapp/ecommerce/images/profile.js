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

jQuery(document).ready( function() {
    
    // register a new user/customer
    if (document.getElementById('newUserForm')) {
        jQuery("#newUserForm").validate();
        
        jQuery("#emailAddress").change(setUserNameFromEmail);
        useShippingAddressAsBillingToggle();
        
        jQuery("#useShippingAddressForBilling").click(useShippingAddressAsBillingToggle);
        jQuery("#submitNewUserForm").click(submitValidNewUser);
        // Get associate states for Shipping Information
        jQuery("#shipToCountryGeoId").change( function(){
            getAssociatedStateList('shipToCountryGeoId', 'shipToStateProvinceGeoId', 'advice-required-shipToStateProvinceGeoId', 'shipToStates');
        });
        // Get associate states for Billing Information
        jQuery("#billToCountryGeoId").change( function(){
            getAssociatedStateList('billToCountryGeoId', 'billToStateProvinceGeoId', 'advice-required-billToStateProvinceGeoId', 'billToStates');
        });
        getAssociatedStateList('shipToCountryGeoId', 'shipToStateProvinceGeoId', 'advice-required-shipToStateProvinceGeoId', 'shipToStates');
        getAssociatedStateList('billToCountryGeoId', 'billToStateProvinceGeoId', 'advice-required-billToStateProvinceGeoId', 'billToStates');
    }
    
    // edit user information form validation
    if (document.getElementById('editUserForm')) {
        jQuery("#editUserForm").validate();
    }
    
    // add Address validation
    if (document.getElementById('addAddress')) {
        jQuery("#createPostalAddressForm").validate();
    }

    // special validation on blur for phone number fields
    if (document.getElementById('shipToPhoneRequired')) {
        jQuery("#shipToCountryCode").blur( function() {
            validatePhoneNumber('shipToPhoneRequired', 'shipToCountryCode', 'shipToAreaCode', 'shipToContactNumber');
        });
        jQuery("#shipToAreaCode").blur( function() {
            validatePhoneNumber('shipToPhoneRequired', 'shipToAreaCode', 'shipToCountryCode', 'shipToContactNumber');
        });
        jQuery("#shipToContactNumber").blur( function() {
            validatePhoneNumber('shipToPhoneRequired', 'shipToContactNumber', 'shipToCountryCode', 'shipToAreaCode');
        });
    }
    if (document.getElementById('billToPhoneRequired')) {
        jQuery("#billToCountryCode").blur( function() {
            validatePhoneNumber('billToPhoneRequired', 'billToCountryCode', 'billToAreaCode', 'billToContactNumber');
        });
        jQuery("#billToAreaCode").blur( function() {
            validatePhoneNumber('billToPhoneRequired', 'billToAreaCode', 'billToCountryCode', 'billToContactNumber');
        });
        jQuery("#billToContactNumber").blur( function() {
            validatePhoneNumber('billToPhoneRequired', 'billToContactNumber', 'billToCountryCode', 'billToAreaCode');
        });
    }
    
    // postal address validation and geo autocomplete
    if (document.getElementById('createPostalAddressForm')) {
        jQuery("#createPostalAddressForm").validate();
        
        // Get associate states for Postal Address Information
        jQuery("#countryGeoId").change( function() {
            getAssociatedStateList('countryGeoId', 'stateProvinceGeoId', 'advice-required-stateProvinceGeoId', 'states');
        });
        getAssociatedStateList('countryGeoId', 'stateProvinceGeoId', 'advice-required-stateProvinceGeoId', 'states');
    }
    if (document.getElementById('editBillToPostalAddress')) {
        // Get associate states for Billing Information
        jQuery("#billToCountryGeoId").change( function() {
            getAssociatedStateList('billToCountryGeoId', 'billToStateProvinceGeoId', 'advice-required-billToStateProvinceGeoId', 'billToStates');
        });
        if(document.getElementById('billToStateProvinceGeoId').value == "_NA_"){
            getAssociatedStateList('billToCountryGeoId', 'billToStateProvinceGeoId', 'advice-required-billToStateProvinceGeoId', 'billToStates');
        } else {
            stateValue = document.getElementById('billToStateProvinceGeoId').value;
            getAssociatedStateList('billToCountryGeoId', 'billToStateProvinceGeoId', 'advice-required-billToStateProvinceGeoId', 'billToStates');
            document.getElementById('billToStateProvinceGeoId').value = stateValue;
        }
    }
    if (document.getElementById ('editShipToPostalAddress')) {
        // Get associate states for Shipping Information
        jQuery("#shipToCountryGeoId").change( function() {
            getAssociatedStateList('shipToCountryGeoId', 'shipToStateProvinceGeoId', 'advice-required-shipToStateProvinceGeoId', 'shipToStates');
        });
        if(document.getElementById('shipToStateProvinceGeoId').value == "_NA_"){
            getAssociatedStateList('shipToCountryGeoId', 'shipToStateProvinceGeoId', 'advice-required-shipToStateProvinceGeoId', 'shipToStates');
        } else {
            var stateValue = document.getElementById('shipToStateProvinceGeoId').value;
            getAssociatedStateList('shipToCountryGeoId', 'shipToStateProvinceGeoId', 'advice-required-shipToStateProvinceGeoId', 'shipToStates');
            document.getElementById('shipToStateProvinceGeoId').value = stateValue;
        }
    }
});

/*
 * This function is used for validation of Phone number with only 1 error message instead of multiple (for eg: required) on label.
 * It takes following parameters :-
 * 1) errorDivId : div to display error,
 * 2) focusedTextId : Text box, last focused,
 * 3) textToCheck1, textToCheck2 : Other text boxes to be check.
 */
function validatePhoneNumber(errorDivId, focusedTextId, textToCheck1, textToCheck2) {
    if ((document.getElementById(focusedTextId).value == "")) {
        jQuery("#" + errorDivId).fadeIn("fast");
    } else if ((document.getElementById(textToCheck1).value != "") && (document.getElementById(textToCheck2).value != "" )) {
        jQuery("#" + errorDivId).fadeOut("fast");
    }
}

function submitValidNewUser() {
   
    validatePhoneNumber('shipToPhoneRequired', 'shipToContactNumber', 'shipToCountryCode', 'shipToAreaCode');
    validatePhoneNumber('billToPhoneRequired', 'billToContactNumber', 'billToCountryCode', 'billToAreaCode');
    if (jQuery("#newUserForm").valid()) {
        document.getElementById('newUserForm').submit();
    }
}

function submitValidEditUser() {
        document.getElementById('editUserForm').submit();
}

function submitValidPostalAddress() {
        document.getElementById('createPostalAddressForm').submit();
}

function setUserNameFromEmail() {
    if (document.getElementById('username').value == "") {
        document.getElementById('username').value = document.getElementById('emailAddress').value;
    }
}

function useShippingAddressAsBillingToggle() {
    if (document.getElementById('useShippingAddressForBilling').checked) {
        document.getElementById('billToAddress1').value = document.getElementById('shipToAddress1').value;
        document.getElementById('billToAddress2').value = document.getElementById('shipToAddress2').value;
        document.getElementById('billToCity').value = document.getElementById('shipToCity').value;
        document.getElementById('billToCountryGeoId').value = document.getElementById('shipToCountryGeoId').value;
        getAssociatedStateList('billToCountryGeoId', 'billToStateProvinceGeoId', 'advice-required-billToStateProvinceGeoId', 'billToStates');
        document.getElementById('billToStateProvinceGeoId').value = document.getElementById('shipToStateProvinceGeoId').value;
        document.getElementById('billToPostalCode').value = document.getElementById('shipToPostalCode').value;

        document.getElementById('billToAddress1').disabled = true ;
        document.getElementById('billToAddress2').disabled = true ;
        document.getElementById('billToCity').disabled = true ;
        document.getElementById('billToCountryGeoId').disabled = true ;
        document.getElementById('billToStateProvinceGeoId').disabled = true ;
        document.getElementById('billToPostalCode').disabled = true;
        copyShipToBillAddress();
        hideErrorMessage();
    } else {
        stopObservingShipToBillAddress();
        document.getElementById('billToAddress1').disabled = false ;
        document.getElementById('billToAddress2').disabled = false ;
        document.getElementById('billToCity').disabled = false ;
        document.getElementById('billToCountryGeoId').disabled = false ;
        document.getElementById('billToStateProvinceGeoId').disabled = false ;
        document.getElementById('billToPostalCode').disabled = false;
    }
}

function getServerError(data) {
    var serverErrorHash = [];
    var serverError = "";
    if (data._ERROR_MESSAGE_LIST_ != undefined) {
        serverErrorHash = data._ERROR_MESSAGE_LIST_;

        var CommonErrorMessage2 = getJSONuiLabel("CommonUiLabels", "CommonErrorMessage2");
        showErrorAlert(CommonErrorMessage2, serverErrorHash);
        jQuery.each(serverErrorHash, function(error, message){
            if (error != undefined) {
                serverError += message;
            }
        });

        if (serverError == "") {
            serverError = serverErrorHash;
        }
    }
    if (data._ERROR_MESSAGE_ != undefined) {
        serverError += data._ERROR_MESSAGE_;
    }
    return serverError;
}

function doAjaxRequest(formId, errorId, popupId, requestUrl) {
    if (jQuery("#" + formId).valid()) {    
        jQuery.ajax({
            url: requestUrl,
            type: 'POST',
            async: false,
            data: jQuery("#" + formId).serialize(),
            success: function(data) {
                var serverError = getServerError(data);
                if (serverError != "") {
                    jQuery("#" + errorId).fadeIn("fast");
                    jQuery("#" + popupId).fadeIn("fast");
                    jQuery("#" + errorId).html(serverError);
                } else {
                    jQuery("#" + errorId).fadeIn("slow");
                    jQuery("#" + popupId).fadeIn("slow");
                    document.getElementById('refreshRequestForm').submit();
                }
            }
            
        });
        
    }
}

function createPartyPostalAddress(e) {
    formId = 'createPostalAddressForm';
    errorId = 'serverError';
    popupId = 'displayCreateAddressForm';
    requestUrl = 'createPartyPostalAddress';
    
    doAjaxRequest(formId, errorId, popupId, requestUrl);
    
    
}

function updatePartyPostalAddress(e) {
    contactMechId = e.split('_')[1];
    formId = 'editPostalAddress_' + contactMechId;
    errorId = 'serverError_' + contactMechId;
    popupId = 'displayEditAddressForm_' + contactMechId;
    requestUrl = 'updatePartyPostalAddress';
    
    doAjaxRequest(formId, errorId, popupId, requestUrl);
}

function updatePartyShipToPostalAddress(e) {
    formId = 'editShipToPostalAddress';
    errorId = 'shipToServerError';
    popupId = 'displayEditShipToPostalAddress';
    requestUrl = 'updatePartyPostalAddress';
    
    doAjaxRequest(formId, errorId, popupId, requestUrl);
}

function updatePartyBillToPostalAddress(e) {
    formId = 'editBillToPostalAddress';
    errorId = 'billToServerError';
    popupId = 'displayEditBillToPostalAddress';
    requestUrl = 'updatePartyPostalAddress';
    
    doAjaxRequest(formId, errorId, popupId, requestUrl);
}

function hideErrorMessage() {
    jQuery('#advice-required-billToAddress1').fadeOut("fast");
    jQuery('#advice-required-billToStateProvinceGeoId').fadeOut("fast");
    jQuery('#advice-required-billToCity').fadeOut("fast");
    jQuery('#advice-required-billToPostalCode').fadeOut("fast");;
    jQuery('#advice-required-billToCountryGeoId').fadeOut("fast");
    jQuery('#billToPhoneRequired').fadeOut("fast");
}

function copyShipToBillAddress() {
    jQuery("#shipToAddress1").change( function() {
         document.getElementById('billToAddress1').value = document.getElementById('shipToAddress1').value;
    });
    jQuery("#shipToAddress2").change( function() {
        document.getElementById('billToAddress2').value = document.getElementById('shipToAddress2').value;
    });
    jQuery("#shipToCity").change( function() {
        document.getElementById('billToCity').value = document.getElementById('shipToCity').value;
    });
    jQuery("#shipToStateProvinceGeoId").change( function() {
        document.getElementById('billToStateProvinceGeoId').value = document.getElementById('shipToStateProvinceGeoId').value;
    });
    
    jQuery("#shipToCountryGeoId").change(copyShipToCountryToBillToCountry);
    jQuery("#shipToPostalCode").change( function() {
        document.getElementById('billToPostalCode').value = document.getElementById('shipToPostalCode').value;
    });
}

function stopObservingShipToBillAddress() {
    jQuery('#shipToAddress1').unbind('change');
    jQuery('shipToAddress2').unbind('change');
    jQuery('shipToCity').unbind('change');
    jQuery('shipToStateProvinceGeoId').unbind('change');
    jQuery('shipToCountryGeoId').unbind('change', copyShipToCountryToBillToCountry);
    jQuery('shipToPostalCode').unbind('change');
}

function showState(id) {
    if (document.getElementById('editPostalAddress_' + id)) {
        // Get associate states for Postal Address Information
        jQuery("#countryGeoId_" + id).change( function() {
            getAssociatedStateList('countryGeoId_'+id, 'stateProvinceGeoId_'+id, 'advice-required-stateProvinceGeoId_'+id, 'states_'+id);
        });
        
        if (document.getElementById('stateProvinceGeoId_'+id).value == "_NA_") {
            getAssociatedStateList('countryGeoId_'+id, 'stateProvinceGeoId_'+id, 'advice-required-stateProvinceGeoId_'+id, 'states_'+id);
        } else {
            var stateValue = document.getElementById('stateProvinceGeoId_'+id).value;
            getAssociatedStateList('countryGeoId_'+id, 'stateProvinceGeoId_'+id, 'advice-required-stateProvinceGeoId_'+id, 'states_'+id);
            document.getElementById('stateProvinceGeoId_'+id).value = stateValue;
        }
    }
}

function copyShipToCountryToBillToCountry(){
    document.getElementById('billToCountryGeoId').value = document.getElementById('shipToCountryGeoId').value;
    getAssociatedStateList('billToCountryGeoId', 'billToStateProvinceGeoId', 'advice-required-billToStateProvinceGeoId', 'billToStates');
}