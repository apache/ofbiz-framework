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
jQuery(document).ready(function() {
    jQuery('#removeCreditCardTotalPaid').bind('click', function(event) {
        pleaseWait('Y');
        var param = 'clearCash=N&clearCheck=N&clearGift=N&clearCredit=Y';
        jQuery.ajax({url: 'ClearPayment',
            data: param,
            type: 'post',
            async: false,
            success: function(data) {
                getResultOfCreditCardClearPayment(data);
            },
            error: function(data) {
                getResultOfCreditCardClearPayment(data);
            }
        });
        pleaseWait('N');
        productToSearchFocus();
        return false;
    });
    
    jQuery('#swipeCard').bind('click', function(event) {
        jQuery('#payCreditCard').show();
        if (jQuery('#swipeCard').val() == 'Y') {
            jQuery('#swipeCard').val('N');
            jQuery('#swipeCard').checked = false;
            jQuery('#showSwipeData').hide();
            jQuery('#showCreditCardData1').show();
            jQuery('#showCreditCardData2').show();
            jQuery('#firstName').focus();
        } else {
            jQuery('#swipeCard').val('Y');
            jQuery('#swipeCard').checked = true;
            jQuery('#showSwipeData').show();
            jQuery('#showCreditCardData1').hide();
            jQuery('#showCreditCardData2').hide();
            jQuery('#swipeData').focus();
        }
        return false;
    });
    
    jQuery('#swipeData').bind('keypress', function(event) {
        code = event.keyCode ? event.keyCode : event.which;
        if (code.toString() == 13) {
            payCreditCardConfirm();
            return false;
        }
        if (code.toString() == 27) {
            productToSearchFocus();
            return false;
        }
    });
    
    jQuery('#firstName').bind('keypress', function(event) {
        code = event.keyCode ? event.keyCode : event.which;
        if (code.toString() == 13) {
            payCreditCardConfirm();
            return false;
        }
        if (code.toString() == 27) {
            productToSearchFocus();
            return false;
        }
    });
    
    jQuery('#lastName').bind('keypress', function(event) {
        code = event.keyCode ? event.keyCode : event.which;
        if (code.toString() == 13) {
            payCreditCardConfirm();
            return false;
        }
        if (code.toString() == 27) {
            productToSearchFocus();
            return false;
        }
    });
    
    jQuery('#cardNum').bind('keypress', function(event) {
        code = event.keyCode ? event.keyCode : event.which;
        if (code.toString() == 13) {
            payCreditCardConfirm();
            return false;
        }
        if (code.toString() == 27) {
            productToSearchFocus();
            return false;
        }
    });
    
    jQuery('#securityCode').bind('keypress', function(event) {
        code = event.keyCode ? event.keyCode : event.which;
        if (code.toString() == 13) {
            payCreditCardConfirm();
            return false;
        }
        if (code.toString() == 27) {
            productToSearchFocus();
            return false;
        }
    });
    
    jQuery('#amountCreditCard').bind('keypress', function(event) {
        code = event.keyCode ? event.keyCode : event.which;
        if (code.toString() == 13) {
            payCreditCardConfirm();
            return false;
        }
        if (code.toString() == 27) {
            productToSearchFocus();
            return false;
        }
    });
    
    jQuery('#postalCode').bind('keypress', function(event) {
        code = event.keyCode ? event.keyCode : event.which;
        if (code.toString() == 13) {
            payCreditCardConfirm();
            return false;
        }
        if (code.toString() == 27) {
            productToSearchFocus();
            return false;
        }
    });

    jQuery('#refNumCreditCard').bind('keypress', function(event) {
        code = event.keyCode ? event.keyCode : event.which;
        if (code.toString() == 13) {
            payCreditCardConfirm();
            return false;
        }
        if (code.toString() == 27) {
            productToSearchFocus();
            return false;
        }
    });
    
    jQuery('#payCreditCardConfirm').bind('click', function(event) {
        payCreditCardConfirm();
        return false;
    });
    
    jQuery('#payCreditCardCancel').bind('click', function(event) {
        productToSearchFocus();
        return false;
    });
});

function payCreditCardConfirm() {
    pleaseWait('Y');
    var param = 'swipeCard=' + jQuery('#swipeCard').val() + '&swipeData=' + jQuery('#swipeData').val() + '&cardType=' + jQuery('#cardType').val() +
                '&amountCreditCard=' + jQuery('#amountCreditCard').val() + '&refNum=' + jQuery('#refNumCreditCard').val() + '&cardNum=' + jQuery('#cardNum').val() +
                '&expMonth=' + jQuery('#expMonth').val() + '&expYear=' + jQuery('#expYear').val() + '&securityCode=' + jQuery('#securityCode').val() +
                '&postalCode=' + jQuery('#postalCode').val() + '&firstName=' + jQuery('#firstName').val() + '&lastName=' + jQuery('#lastName').val() +
                '&track2=' + jQuery('#track2').val();
    jQuery.ajax({url: 'PayCreditCard',
        data: param,
        type: 'post',
        async: false,
        success: function(data) {
            getResultOfPayCreditCard(data);
        },
        error: function(data) {
            getResultOfPayCreditCard(data);
        }
    });
    pleaseWait('N');
}

function payCreditCard(cleanErrors) {
    if (cleanErrors == undefined) {
        cleanErrors = "Y";
    } 
    if (cleanErrors == "Y") {
        hideOverlayDiv();
        jQuery('#payCreditCardFormServerError').html("");
        jQuery('#swipeCard').val('Y');
        jQuery('#swipeCard').checked = true;
        jQuery('#swipeData').val('');
        jQuery('#firstName').val('');
        jQuery('#lastName').val('');
        jQuery('#cardNum').val('');
        jQuery('#securityCode').val('');
        jQuery('#postalCode').value = '';
        jQuery('#refNumCreditCard').val('');
        jQuery('#amountCreditCard').val('');
        jQuery('#showSwipeData').show();
        jQuery('#showCreditCardData1').hide();
        jQuery('#showCreditCardData2').hide();
    }
    jQuery('#creditCardTotalDue').html(jQuery('#totalDueFormatted').val());
    jQuery('#creditCardTotalPaid').html(jQuery('#totalCreditFormatted').val());
    jQuery('#payCreditCard').show();
    if (jQuery('#swipeCard').val() == 'Y') {
        jQuery('#swipeData').focus();
    } else {
        jQuery('#firstName').focus();
    }
    return false;
}

function getResultOfPayCreditCard(data) {
    var serverError = getServerError(data);
    if (serverError != "") {
        jQuery('#payCreditCardFormServerError').html(serverError);
        payCreditCard('N');
    } else {
        clearErrorMessages();
        updateCart();
        productToSearchFocus();
    }
}

function getResultOfCreditCardClearPayment(data) {
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