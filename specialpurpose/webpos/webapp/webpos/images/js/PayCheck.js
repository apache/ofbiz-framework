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
    jQuery('#removeCheckTotalPaid').bind('click', function(event) {
        pleaseWait('Y');
        var param = 'clearCash=N&clearCheck=Y&clearGift=Y&clearCredit=N';
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
    
    jQuery('#amountCheck').bind('keypress', function(event) {
        code = event.keyCode ? event.keyCode : event.which;
        if (code.toString() == 13) {
            payCheckConfirm();
            return false;
        }
        if (code.toString() == 27) {
            productToSearchFocus();
            return false;
        }
    });
    
    jQuery('#refNumCheck').bind('keypress', function(event) {
        code = event.keyCode ? event.keyCode : event.which;
        if (code.toString() == 13) {
            payCheckConfirm();
            return false;
        } 
        if (code.toString() == 27) {
            productToSearchFocus();
            return false;
        }
    });
    
    jQuery('#payCheckConfirm').bind('click', function(event) {
        payCheckConfirm();
        return false;
    });
    
    jQuery('#payCheckCancel').bind('click', function(event) {
        productToSearchFocus();
        return false;
    });
});

function payCheckConfirm() {
    pleaseWait('Y');
    var param = 'amountCheck=' + jQuery('#amountCheck').val() + '&refNum=' + jQuery('#refNumCheck').val();
    jQuery.ajax({url: 'PayCheck',
        data: param,
        type: 'post',
        async: false,
        success: function(data) {
            getResultOfPayCheck(data);
        },
        error: function(data) {
            getResultOfPayCheck(data);
        }
    });
    pleaseWait('N');
}

function payCheck(cleanErrors) {
    if (cleanErrors == undefined) {
        cleanErrors = "Y";
    } 
    if (cleanErrors == "Y") {
        hideOverlayDiv();
        jQuery('#payCheckFormServerError').html("");
        jQuery('#amountCheck').val("");
        jQuery('#refNumCheck').val("");
    }
    jQuery('#checkTotalDue').html(jQuery('#totalDueFormatted').val());
    jQuery('#checkTotalPaid').html(jQuery('#totalCheckFormatted').val());
    jQuery('#payCheck').show();
    jQuery('#amountCheck').focus();
    return false;
}

function getResultOfPayCheck(data) {
    var serverError = getServerError(data);
    if (serverError != "") {
        jQuery('#payCheckFormServerError').html(serverError);
        payCheck('N');
    } else {
        clearErrorMessages();
        updateCart();
        productToSearchFocus();
    }
}

function getResultOfCheckClearPayment(data) {
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