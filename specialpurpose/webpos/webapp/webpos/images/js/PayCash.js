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
    jQuery('#removeCashTotalPaid').click(function(event) {
        pleaseWait('Y');
        var param = 'clearCash=Y&clearCheck=N&clearGift=N&clearCredit=N';
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
    
    jQuery('#amountCash').keypress(function(event) {
        code = event.keyCode ? event.keyCode : event.which;
        if (code.toString() == 13) {
            payCashConfirm();
            return false;
        }
        if (code.toString() == 27) {
            productToSearchFocus();
            return false;
        }
    });
    
    jQuery('#payCashConfirm').click(function(event) {
        payCashConfirm();
        return false;
    });

    jQuery('#payCashCancel').click(function(event) {
        productToSearchFocus();
        return false;
    });
});

function payCashConfirm() {
    pleaseWait('Y');
    var param = 'amountCash=' + jQuery('#amountCash').val();
    jQuery.ajax({url: 'PayCash',
        data: param,
        type: 'post',
        async: false,
        success: function(data) {
            getResultOfPayCash(data);
        },
        error: function(data) {
            getResultOfPayCash(data);
        }
    });
    pleaseWait('N');
}

function payCash(cleanErrors) {
    if (cleanErrors == undefined) {
        cleanErrors = "Y";
    } 
    if (cleanErrors == "Y") {
        hideOverlayDiv();
        jQuery('#payCashFormServerError').html("");
        jQuery('#amountCash').val("");
    }
    jQuery('#cashTotalDue').html(jQuery('#totalDueFormatted').val());
    jQuery('#cashTotalPaid').html(jQuery('#totalCashFormatted').val());
    jQuery('#payCash').show();
    jQuery('#amountCash').focus();
    return false;
}

function getResultOfPayCash(data) {
    var serverError = getServerError(data);
    if (serverError != "") {
        jQuery('#payCashFormServerError').html(serverError);
        payCash('N');
    } else {
        clearErrorMessages();
        updateCart();
        productToSearchFocus();
    }
}

function getResultOfCashClearPayment(data) {
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