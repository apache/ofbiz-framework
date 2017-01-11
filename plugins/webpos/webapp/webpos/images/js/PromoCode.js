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
    jQuery('#promoCode').bind('keypress', function(event) {
        code = event.keyCode ? event.keyCode : event.which;
        if (code.toString() == 13) {
            promoCodeConfirm();
            return false;
        }
        if (code.toString() == 27) {
            productToSearchFocus();
            return false;
        }
    });
    
    jQuery('#promoCodeConfirm').bind('click', function(event) {
        promoCodeConfirm();
        return false;
    });
    
    jQuery('#promoCodeCancel').bind('click', function(event) {
        productToSearchFocus();
        return false;
    });
});

function promoCodeConfirm() {
    pleaseWait('Y');
    var param = 'promoCode=' + jQuery('#promoCode').val();
      jQuery.ajax({url: 'PromoCode',
            data: param,
            type: 'post',
            async: false,
            success: function(data) {
                getResultOfPromoCode(data);
            },
            error: function(data) {
                  getResultOfPromoCode(data);
            }
      });
    pleaseWait('N');
}

function promoCode(cleanErrors) {
    if (cleanErrors == undefined) {
        cleanErrors = "Y";
    }
    if (cleanErrors == "Y") {
        hideOverlayDiv();
        jQuery('#promoCodeFormServerError').html("");
        jQuery('#promoCode').val("");
    }
    jQuery('#promo').show();
    jQuery('#promoCode').focus();
}

function getResultOfPromoCode(date) {
    var serverError = getServerError(data);
    if (serverError != "") {
        jQuery('#promoCodeFormServerError').html(serverError);
        promoCode('N');
    } else {
        clearErrorMessages();
        updateCart();
        productToSearchFocus();
    }
}