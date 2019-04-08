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
    jQuery('#amountInOut').bind('keypress', function(event) {
        code = event.keyCode ? event.keyCode : event.which;
        if (code.toString() == 13) {
            paidOutAndInConfirm();
            return false;
        }
        if (code.toString() == 27) {
            productToSearchFocus();
            return false;
        }
    });
    
    jQuery('#reasonCommentInOut').bind('keypress', function(event) {
        code = event.keyCode ? event.keyCode : event.which;
        if (code.toString() == 13) {
            paidOutAndInConfirm();
            return false;
        }
        if (code.toString() == 27) {
            productToSearchFocus();
            return false;
        }
    });
    
    jQuery('#paidOutAndInConfirm').bind('click', function(event) {
        paidOutAndInConfirm();
        return false;
    });
    
    jQuery('#paidOutAndInCancel').bind('click', function(event) {
        productToSearchFocus();
        return false;
    });
});

function paidOutAndInConfirm() {
    pleaseWait('Y');
    var param = 'type=' + jQuery('#paidType').val() + '&amountInOut=' + jQuery('#amountInOut').val() + 
                '&reasonIn=' + jQuery('#reasIn').val() + '&reasonOut=' + jQuery('#reasOut').val() + 
                '&reasonCommentInOut=' + jQuery('#reasonCommentInOut').val();
   jQuery.ajax({url: 'PaidOutAndIn',
        data: param,
        type: 'post',
        async: false,
        success: function(data) {
            getResultOfPaidOutAndIn(data);
        },
        error: function(data) {
            getResultOfPaidOutAndIn(data);
        }
    });
    pleaseWait('N');
}

function paidOutAndIn(cleanErrors, paidInOut) {
    if (cleanErrors == undefined) {
        cleanErrors = "Y";
    }
    if (cleanErrors == "Y") {
        hideOverlayDiv();
        jQuery('#paidOutAndInFormServerError').html("");
        jQuery('#amountInOut').val("");
        jQuery('#reasonCommentInOut').val("");
    }
    jQuery('#paidType').val(paidInOut);
    jQuery('#paidOutAndIn').show();
    if (paidInOut == 'IN') {
        jQuery('#reasonIn').show();
        jQuery('#amountPaidIn').show();
        jQuery('#reasonOut').hide();
        jQuery('#amountPaidOut').hide();
    } else {
        jQuery('#reasonIn').hide();
        jQuery('#amountPaidIn').hide();
        jQuery('#reasonOut').show();
        jQuery('#amountPaidOut').show();
    }
    jQuery('#amountInOut').focus();
}

function getResultOfPaidOutAndIn(data) {
    var serverError = getServerError(data);
    if (serverError != "") {
        jQuery('#paidOutAndInFormServerError').html(serverError);
        paidOutAndIn('N', jQuery('#paidType').val());
    } else {
        clearErrorMessages();
        updateCart();
        productToSearchFocus();
    }
}