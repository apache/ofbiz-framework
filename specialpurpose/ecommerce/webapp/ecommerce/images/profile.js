var validateNewUser = null;
var validateEditUser = null;
var validatePostalAddress = null;
Event.observe(window, 'load', function() {
    if ($('newUserForm')) {
        validateNewUser = new Validation('newUserForm', {immediate: true, onSubmit: false});
        addValidations();
        Event.observe($('emailAddress'), 'change', setUserNameFromEmail);
        Event.observe('useShippingAddressForBilling', 'click', useShippingAddressAsBillingToggle);
        Event.observe($('submitNewUserForm'), 'click', submitValidNewUser);
        // Get associate states for Shipping Information
        Event.observe($('shipToCountryGeoId'), 'change', function(){
            getAssociatedStateList('shipToCountryGeoId', 'shipToStateProvinceGeoId', 'advice-required-shipToStateProvinceGeoId', 'shipToStates');
        });
        // Get associate states for Billing Information
        Event.observe($('billToCountryGeoId'), 'change', function() {
            getAssociatedStateList('billToCountryGeoId', 'billToStateProvinceGeoId', 'advice-required-billToStateProvinceGeoId', 'billToStates');
        });
        getAssociatedStateList('shipToCountryGeoId', 'shipToStateProvinceGeoId', 'advice-required-shipToStateProvinceGeoId', 'shipToStates');
        getAssociatedStateList('billToCountryGeoId', 'billToStateProvinceGeoId', 'advice-required-billToStateProvinceGeoId', 'billToStates');
    }
    if ($('editUserForm')) {
        validateEditUser = new Validation('editUserForm', {immediate: true, onSubmit: false});
        Event.observe($('submitEditUserForm'), 'click', submitValidEditUser);
    }
    if (!$('newUserForm') && !$('editUserForm')) {
        if ($('emailAddress')) {
            inPlaceEditEmail('emailAddress');
        }
    }
    if ($('addAddress')) {
        validatePostalAddress = new Validation('createPostalAddressForm', {immediate: true, onSubmit: false});
    }
    if ($('submitPostalAddressForm')) {
        Event.observe($('submitPostalAddressForm'), 'click', submitValidPostalAddress);
    }

    if ($('shipToPhoneRequired')) {
        Event.observe($('shipToCountryCode'), 'blur', function() {
            validatePhoneNumber('shipToPhoneRequired', 'shipToCountryCode', 'shipToAreaCode', 'shipToContactNumber');
        });
        Event.observe($('shipToAreaCode'), 'blur', function() {
            validatePhoneNumber('shipToPhoneRequired', 'shipToAreaCode', 'shipToCountryCode', 'shipToContactNumber');
        });
        Event.observe($('shipToContactNumber'), 'blur', function() {
            validatePhoneNumber('shipToPhoneRequired', 'shipToContactNumber', 'shipToCountryCode', 'shipToAreaCode');
        });
    }
    if ($('billToPhoneRequired')) {
        Event.observe($('billToCountryCode'), 'blur', function() {
            validatePhoneNumber('billToPhoneRequired', 'billToCountryCode', 'billToAreaCode', 'billToContactNumber');
        });
        Event.observe($('billToAreaCode'), 'blur', function() {
            validatePhoneNumber('billToPhoneRequired', 'billToAreaCode', 'billToCountryCode', 'billToContactNumber');
        });
        Event.observe($('billToContactNumber'), 'blur', function() {
            validatePhoneNumber('billToPhoneRequired', 'billToContactNumber', 'billToCountryCode', 'billToAreaCode');
        });
    }
    if ($('createPostalAddressForm')) {
        // Get associate states for Postal Address Information
        Event.observe($('countryGeoId'), 'change', function() {
            getAssociatedStateList('countryGeoId', 'stateProvinceGeoId', 'advice-required-stateProvinceGeoId', 'states');
        });
        getAssociatedStateList('countryGeoId', 'stateProvinceGeoId', 'advice-required-stateProvinceGeoId', 'states');
    }
    if ($('editBillToPostalAddress')) {
        // Get associate states for Billing Information
        Event.observe($('billToCountryGeoId'), 'change', function() {
            getAssociatedStateList('billToCountryGeoId', 'billToStateProvinceGeoId', 'advice-required-billToStateProvinceGeoId', 'billToStates');
        });
        if($('billToStateProvinceGeoId').value == "_NA_"){
            getAssociatedStateList('billToCountryGeoId', 'billToStateProvinceGeoId', 'advice-required-billToStateProvinceGeoId', 'billToStates'); 	
        } else {
            stateValue = $('billToStateProvinceGeoId').value;
            getAssociatedStateList('billToCountryGeoId', 'billToStateProvinceGeoId', 'advice-required-billToStateProvinceGeoId', 'billToStates');
            $('billToStateProvinceGeoId').value = stateValue;	
        }
    }
    if ($('editShipToPostalAddress')) {
        // Get associate states for Shipping Information
        Event.observe($('shipToCountryGeoId'), 'change', function(){
            getAssociatedStateList('shipToCountryGeoId', 'shipToStateProvinceGeoId', 'advice-required-shipToStateProvinceGeoId', 'shipToStates');
        });
        if($('shipToStateProvinceGeoId').value == "_NA_"){
            getAssociatedStateList('shipToCountryGeoId', 'shipToStateProvinceGeoId', 'advice-required-shipToStateProvinceGeoId', 'shipToStates');
        } else {
            var stateValue = $('shipToStateProvinceGeoId').value;
            getAssociatedStateList('shipToCountryGeoId', 'shipToStateProvinceGeoId', 'advice-required-shipToStateProvinceGeoId', 'shipToStates');
            $('shipToStateProvinceGeoId').value = stateValue;
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
    if (($(focusedTextId).value == "")) {
        Effect.Appear(errorDivId, {duration: 0.5});
    } else if (($(textToCheck1).value != "") && ($(textToCheck2).value != "" )) {
       Effect.Fade(errorDivId, {duration: 0.5}); 
    }
}

function submitValidNewUser() {
	validatePhoneNumber('shipToPhoneRequired', 'shipToContactNumber', 'shipToCountryCode', 'shipToAreaCode');
	validatePhoneNumber('billToPhoneRequired', 'billToContactNumber', 'billToCountryCode', 'billToAreaCode');
    if (validateNewUser.validate()) {
        $('newUserForm').submit();
    }
}

function submitValidEditUser() {
    if (validateEditUser.validate()) {
        $('editUserForm').submit();
    }
}

function submitValidPostalAddress() {
    if (validatePostalAddress.validate()) {
        $('createPostalAddressForm').submit();
    }
}

function setUserNameFromEmail() {
    if ($('username').value == "") {
        $('username').value = $F('emailAddress');
    }
}

function useShippingAddressAsBillingToggle() {
    if ($('useShippingAddressForBilling').checked) {
        $('billToAddress1').value = $F('shipToAddress1');
        $('billToAddress2').value = $F('shipToAddress2');
        $('billToCity').value = $F('shipToCity');
        $('billToCountryGeoId').value = $F('shipToCountryGeoId');
        getAssociatedStateList('billToCountryGeoId', 'billToStateProvinceGeoId', 'advice-required-billToStateProvinceGeoId', 'billToStates');
        $('billToStateProvinceGeoId').value = $F('shipToStateProvinceGeoId');
        $('billToPostalCode').value = $F('shipToPostalCode');
        
        $('billToAddress1').disabled = true ;
        $('billToAddress2').disabled = true ;
        $('billToCity').disabled = true ;
        $('billToCountryGeoId').disabled = true ;
        $('billToStateProvinceGeoId').disabled = true ;
        $('billToPostalCode').disabled = true;
        copyShipToBillAddress();
        hideErrorMessage();
    } else {
    	validBillingAddress();
        stopObservingShipToBillAddress();
        $('billToAddress1').disabled = false ;
        $('billToAddress2').disabled = false ;
        $('billToCity').disabled = false ;
        $('billToCountryGeoId').disabled = false ;
        $('billToStateProvinceGeoId').disabled = false ;
        $('billToPostalCode').disabled = false;
    }
}

function getServerError(data) {
    var serverErrorHash = [];
    var serverError = "";
    if (data._ERROR_MESSAGE_LIST_ != undefined) {
        serverErrorHash = data._ERROR_MESSAGE_LIST_;
    
        serverErrorHash.each(function(error) {
            if (error.message != undefined) {
                serverError += error.message;
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

function inPlaceEditEmail(e) {
    if ($('updatedEmailContactMechId')) {
        var url = 'updatePartyEmailAddress?contactMechId='+ $('updatedEmailContactMechId').value;
        var errorId = 'serverError_' + $('updatedEmailContactMechId').value;
        var oldEmailAddress = $('updatedEmailAddress').value;
        var editor = new Ajax.InPlaceEditor(e, url, {clickToEditText: 'click here to change your email', paramName: 'emailAddress', htmlResponse: false, updateAfterRequestCall: true,
            onComplete: function (transport) {
                if (transport != undefined) {
                    var data = transport.responseText.evalJSON(true);
                    var serverError = getServerError(data);
                    if (serverError != "") {
                        Effect.Appear(errorId);
                        $(errorId).update(serverError);
                        $('emailAddress').update(oldEmailAddress);
                    } else {
                        Effect.Fade(errorId);
                        if (data.contactMechId != undefined) {
                            $('updatedEmailContactMechId').value = data.contactMechId;
                            $('updatedEmailAddress').value = data.emailAddress;
                        } else  {
                            $('emailAddress').update(oldEmailAddress);
                        }
                        inPlaceEditEmail('emailAddress');
                        $(errorId).id = 'serverError_' + $('updatedEmailContactMechId').value;
                        editor.dispose();
                    }
                }
            }
        });
    }
}

function createPartyPostalAddress(e) {
    formId = 'createPostalAddressForm';
    var validateEditPostalAddress = new Validation(formId, {immediate: true, onSubmit: false});
    errorId = 'serverError';
    popupId = 'displayCreateAddressForm';
    if (validateEditPostalAddress.validate()) {
        new Ajax.Request('createPartyPostalAddress', {
            asynchronous: false, 
            onSuccess: function(transport) {
                var data = transport.responseText.evalJSON(true);
                var serverError = getServerError(data);
                if (serverError != "") {
                    Effect.Appear(errorId);
                    Effect.Appear(popupId);
                    $(errorId).update(serverError);
                } else {
                    Effect.Fade(popupId);
                    Effect.Fade(errorId);
                    $('refreshRequestForm').submit();
                }
            }, parameters: $(formId).serialize(), requestHeaders: {Accept: 'application/json'}
        });
    }
}

function updatePartyPostalAddress(e) {
    contactMechId = e.split('_')[1];
    formId = 'editPostalAddress_' + contactMechId;
    var validateEditPostalAddress = new Validation(formId, {immediate: true, onSubmit: false});
    errorId = 'serverError_' + contactMechId;
    popupId = 'displayEditAddressForm_' + contactMechId;
    if (validateEditPostalAddress.validate()) {
        new Ajax.Request('updatePartyPostalAddress', {
            asynchronous: false, 
            onSuccess: function(transport) {
                var data = transport.responseText.evalJSON(true);
                var serverError = getServerError(data);
                if (serverError != "") {
                    Effect.Appear(errorId);
                    Effect.Appear(popupId);
                    $(errorId).update(serverError);
                } else {
                    Effect.Fade(popupId);
                    Effect.Fade(errorId);
                    $('refreshRequestForm').submit();
                }
            }, parameters: $(formId).serialize(), requestHeaders: {Accept: 'application/json'}
        });
    }
}

function updatePartyShipToPostalAddress(e) {
    formId = 'editShipToPostalAddress';
    var validateEditPostalAddress = new Validation(formId, {immediate: true, onSubmit: false});
    errorId = 'shipToServerError';
    popupId = 'displayEditShipToPostalAddress';
    if (validateEditPostalAddress.validate()) {
        new Ajax.Request('updatePartyPostalAddress', {
            asynchronous: false, 
            onSuccess: function(transport) {
                var data = transport.responseText.evalJSON(true);
                var serverError = getServerError(data);
                if (serverError != "") {
                    Effect.Appear(errorId);
                    Effect.Appear(popupId);
                    $(errorId).update(serverError);
                } else {
                    Effect.Fade(popupId);
                    Effect.Fade(errorId);
                    $('refreshRequestForm').submit();
                }
            }, parameters: $(formId).serialize(), requestHeaders: {Accept: 'application/json'}
        });
    }
}

function updatePartyBillToPostalAddress(e) {
    formId = 'editBillToPostalAddress';
    var validateEditPostalAddress = new Validation(formId, {immediate: true, onSubmit: false});
    errorId = 'billToServerError';
    popupId = 'displayEditBillToPostalAddress';
    if (validateEditPostalAddress.validate()) {
        new Ajax.Request('updatePartyPostalAddress', {
            asynchronous: false, 
            onSuccess: function(transport) {
                var data = transport.responseText.evalJSON(true);
                var serverError = getServerError(data);
                if (serverError != "") {
                    Effect.Appear(errorId);
                    Effect.Appear(popupId);
                    $(errorId).update(serverError);
                } else {
                    Effect.Fade(popupId);
                    Effect.Fade(errorId);
                    $('refreshRequestForm').submit();
                }
            }, parameters: $(formId).serialize(), requestHeaders: {Accept: 'application/json'}
        });
    }
}

function validBillingAddress () {
    Event.observe($('billToAddress1'), 'blur', function() {
        if ($('billToAddress1').value == "") {
            Effect.Appear('advice-required-billToAddress1');
        }
    });
    Event.observe($('billToStateProvinceGeoId'), 'blur', function() {
        if ($('billToStateProvinceGeoId').value == "") {
            Effect.Appear('advice-required-billToStateProvinceGeoId');
        }
    });
    Event.observe($('billToCity'), 'blur', function() {
        if ($('billToCity').value == "") {
            Effect.Appear('advice-required-billToCity');
        }
    });
    Event.observe($('billToPostalCode'), 'blur', function() {
        if ($('billToPostalCode').value == "") {
            Effect.Appear('advice-required-billToPostalCode');
        }
    });
    Event.observe($('billToCountryGeoId'), 'blur', function() {
        if ($('billToCountryGeoId').value == "") {
            Effect.Appear('advice-required-billToCountryGeoId');
        }
    });
}

function hideErrorMessage() {
    Effect.Fade('advice-required-billToAddress1');
    Effect.Fade('advice-required-billToStateProvinceGeoId');
    Effect.Fade('advice-required-billToCity');
    Effect.Fade('advice-required-billToPostalCode');
    Effect.Fade('advice-required-billToCountryGeoId');
    Effect.Fade('billToPhoneRequired');
}

function copyShipToBillAddress() {
    Event.observe($('shipToAddress1'), 'change', function() {
        $('billToAddress1').value = $F('shipToAddress1')
    });
    Event.observe($('shipToAddress2'), 'change', function() {
        $('billToAddress2').value = $F('shipToAddress2')
    });
    Event.observe($('shipToCity'), 'change', function() {
        $('billToCity').value = $F('shipToCity')
    });
    Event.observe($('shipToStateProvinceGeoId'), 'change', function() {
        $('billToStateProvinceGeoId').value = $F('shipToStateProvinceGeoId')
    });
    Event.observe($('shipToCountryGeoId'), 'change', copyShipToCountryToBillToCountry);
    Event.observe($('shipToPostalCode'), 'change', function() {
        $('billToPostalCode').value = $F('shipToPostalCode')
    });
}

function stopObservingShipToBillAddress() {
    Event.stopObserving($('shipToAddress1'), 'change', "");
    Event.stopObserving($('shipToAddress2'), 'change', "");
    Event.stopObserving($('shipToCity'), 'change', "");
    Event.stopObserving($('shipToStateProvinceGeoId'), 'change', "");
    Event.stopObserving($('shipToCountryGeoId'), 'change', copyShipToCountryToBillToCountry);
    Event.stopObserving($('shipToPostalCode'), 'change', "");
}

function addValidations() {
    Validation.add('validate-password', "", {
        minLength : 5,
        notOneOf : ['password','PASSWORD'],
        notEqualToField : 'username'
    });
    Validation.add('validate-passwordVerify', "", {
        equalToField : 'password'
    });
}

function showState(id) {
    if ($('editPostalAddress_'+id)) {
        // Get associate states for Postal Address Information
        Event.observe($('countryGeoId_'+id), 'change', function() {
            getAssociatedStateList('countryGeoId_'+id, 'stateProvinceGeoId_'+id, 'advice-required-stateProvinceGeoId_'+id, 'states_'+id);
        });
        if ($('stateProvinceGeoId_'+id).value == "_NA_") {
            getAssociatedStateList('countryGeoId_'+id, 'stateProvinceGeoId_'+id, 'advice-required-stateProvinceGeoId_'+id, 'states_'+id);    	
        } else {
            var stateValue = $('stateProvinceGeoId_'+id).value;
            getAssociatedStateList('countryGeoId_'+id, 'stateProvinceGeoId_'+id, 'advice-required-stateProvinceGeoId_'+id, 'states_'+id);
            $('stateProvinceGeoId_'+id).value = stateValue;    	
        }
    }
}

function copyShipToCountryToBillToCountry(){
    $('billToCountryGeoId').value = $F('shipToCountryGeoId');
    getAssociatedStateList('billToCountryGeoId', 'billToStateProvinceGeoId', 'advice-required-billToStateProvinceGeoId', 'billToStates');	
}