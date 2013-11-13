jQuery(document).ready(function() {
    jQuery('#removePinTotalPaid').click(function(event) {
        pleaseWait('Y');
        var param = 'clearPin=Y&clearCheck=N&clearGift=N&clearCredit=N';
        jQuery.ajax({url: 'ClearPayment',
            data: param,
            type: 'post',
            async: false,
            success: function(data) {
                getResultOfPinClearPayment(data);
            },
            error: function(data) {
                getResultOfPinClearPayment(data);
            }
        });
        pleaseWait('N');
        productToSearchFocus();
        return false;
    });
    
    jQuery('#amoutPin').keypress(function(event) {
        code = event.keyCode ? event.keyCode : event.which;
        if (code.toString() == 13) {
            payPinConfirm();
            return false;
        }
        if (code.toString() == 27) {
            productToSearchFocus();
            return false;
        }
    });
    
    jQuery('#payPinConfirm').click(function(event) {
        payPinConfirm();
        return false;
    });

    jQuery('#payPinCancel').click(function(event) {
        productToSearchFocus();
        return false;
    });
});

function payPinConfirm() {
    pleaseWait('Y');
    var param = 'amoutPin=' + jQuery('#amoutPin').val();
    jQuery.ajax({url: 'PayPin',
        data: param,
        type: 'post',
        async: false,
        success: function(data) {
            getResultOfPayPin(data);
        },
        error: function(data) {
            getResultOfPayPin(data);
        }
    });
    pleaseWait('N');
}

function payPin(cleanErrors) {
    if (cleanErrors == undefined) {
        cleanErrors = "Y";
    } 
    if (cleanErrors == "Y") {
        hideOverlayDiv();
        jQuery('#payPinFormServerError').html("");
        jQuery('#amoutPin').val("");
    }
    jQuery('#cashTotalDue').html(jQuery('#totalDueFormatted').val());
    jQuery('#cashTotalPaid').html(jQuery('#totalPinFormatted').val());
    jQuery('#payPin').show();
    jQuery('#amoutPin').focus();
    return false;
}

function getResultOfPayPin(data) {
    var serverError = getServerError(data);
    if (serverError != "") {
        jQuery('#payPinFormServerError').html(serverError);
        payPin('N');
    } else {
        clearErrorMessages();
        updateCart();
        productToSearchFocus();
    }
}

function getResultOfPinClearPayment(data) {
    var serverError = getServerError(data);
    if (serverError != "") {
        jQuery('#errors').fadeIn('slow', function() {
            jQuery('#errors').html(serverError);
        });
    } else {
        clearErrorMessages();
        updateCart();
    }
}