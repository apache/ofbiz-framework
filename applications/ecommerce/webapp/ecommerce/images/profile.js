var validateNewUser = null;
var validateEditUser = null;
var validatePostalAddress = null;
Event.observe(window, 'load', function() {
    if ($('newUserForm')) {
        validateNewUser = new Validation('newUserForm', {immediate: true, onSubmit: false});
        Event.observe($('emailAddress'), 'blur', setUserNameFromEmail);
        Event.observe('useShippingAddressForBilling', 'click', useShippingAddressAsBillingToggle);
        Event.observe($('submitNewUserForm'), 'click', submitValidNewUser);
    }
    if ($('editUserForm')) {
        validateEditUser = new Validation('editUserForm', {immediate: true, onSubmit: false});
        Event.observe($('submitEditUserForm'), 'click', submitValidEditUser);
    }
    if ($('emailAddress')) { 
        inPlaceEditEmail('emailAddress');
    }
    if ($('addAddress')) {
        validatePostalAddress = new Validation('createPostalAddressForm', {immediate: true, onSubmit: false});
    }
    if ($('submitPostalAddressForm')) {
        Event.observe($('submitPostalAddressForm'), 'click', submitValidPostalAddress);
    }
});

function submitValidNewUser() {
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
        $('billToStateProvinceGeoId').value = $F('shipToStateProvinceGeoId');
        $('billToPostalCode').value = $F('shipToPostalCode');
        $('billToCountryCode').value = $F('shipToCountryCode');
        $('billToAreaCode').value = $F('shipToAreaCode');
        $('billToContactNumber').value = $F('shipToContactNumber');
        $('billToExtension').value = $F('shipToExtension');
        Effect.BlindUp($('billingAddress'), {duration: 0.3});
    } else {
        Effect.BlindDown($('billingAddress'), {duration: 0.3});
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
        var url = 'updatePartyEmailAddress?emailContactMechId='+ $('updatedEmailContactMechId').value;
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
                        if (data.emailContactMechId != undefined) {
                            $('updatedEmailContactMechId').value = data.emailContactMechId;
                            $('updatedEmailAddress').value = data.emailContactMech.infoString;
                        } else  {
                            $('emailAddress').update(oldEmailAddress);
                        }
                        inPlaceEditEmail('emailAddress');
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