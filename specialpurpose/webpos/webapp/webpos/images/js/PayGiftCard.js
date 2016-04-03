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
    jQuery('#removeGiftCardTotalPaid').bind('click', function(event) {
        pleaseWait('Y');
        var param = 'clearCash=N&clearCheck=N&clearGift=Y&clearCredit=N';
        jQuery.ajax({url: 'ClearPayment',
            data: param,
            type: 'post',
            async: false,
            success: function(data) {
                getResultOfCashClearPayment(data);
            },
            error: function(data) {
                getResultOfCashClearPayment(data);
            }
        });
        pleaseWait('N');
        productToSearchFocus();
        return false;
    });
    
    jQuery('#amountGiftCard').bind('keypress', function(event) {
        code = event.keyCode ? event.keyCode : event.which;
        if (code.toString() == 13) {
            payGiftCardConfirm();
            return false;
        }
        if (code.toString() == 27) {
            productToSearchFocus();
            return false;
        }
    });
    
    jQuery('#refNumGiftCard').bind('keypress', function(event) {
        code = event.keyCode ? event.keyCode : event.which;
        if (code.toString() == 13) {
            payGiftCardConfirm();
            return false;
        }
        if (code.toString() == 27) {
            productToSearchFocus();
            return false;
        }
    });
    
    jQuery('#payGiftCardConfirm').bind('click', function(event) {
        payGiftCardConfirm();
        return false;
    });
    
    jQuery('#payGiftCardCancel').bind('click', function(event) {
        productToSearchFocus();
        return false;
    });
});

function payGiftCardConfirm() {
    pleaseWait('Y');
    var param = 'amountGiftCard=' + jQuery('#amountGiftCard').val() + '&refNum=' + jQuery('#refNumGiftCard').val();
    jQuery.ajax({url: 'PayGiftCard',
        data: param,
        type: 'post',
        async: false,
        success: function(data) {
            getResultOfPayGiftCard(data);
        },
        error: function(data) {
            getResultOfPayGiftCard(data);
        }
    });
    pleaseWait('N');
}

function payGiftCard(cleanErrors) {
    if (cleanErrors == undefined) {
        cleanErrors = "Y";
    } 
    if (cleanErrors == "Y") {
        hideOverlayDiv();
        jQuery('#payGiftCardFormServerError').html("");
        jQuery('#amountGiftCard').val("");
        jQuery('#refNumGiftCard').val("");
    }
    jQuery('#giftCardTotalDue').html(jQuery('#totalDueFormatted').val());
    jQuery('#giftCardTotalPaid').html(jQuery('#totalGiftFormatted').val());
    jQuery('#payGiftCard').show();
    jQuery('#amountGiftCard').focus();
    return false;
}

function getResultOfPayGiftCard(data) {
    var serverError = getServerError(data);
    if (serverError != "") {
        jQuery('#payGiftCardFormServerError').html(serverError);
        payGiftCard('N');
    } else {
        clearErrorMessages();
        updateCart();
        productToSearchFocus();
    }
}

function getResultOfGiftCardClearPayment(data) {
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