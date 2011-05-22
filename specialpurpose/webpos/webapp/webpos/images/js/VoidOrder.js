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
    jQuery('#orderId').bind('keypress', function(event) {
        code = event.keyCode ? event.keyCode : event.which;
        if (code.toString() == 13) {
            voidOrderConfirm();
            return false;
        }
        if (code.toString() == 27) {
            productToSearchFocus();
            return false;
        }
    });
    
    jQuery('#voidOrderConfirm').bind('click', function(event) {
        voidOrderConfirm();
        return false;
    });
    
    jQuery('#voidOrderCancel').bind('click', function(event) {
        productToSearchFocus();
        return false;
    });
});

function voidOrderConfirm() {
    pleaseWait('Y');
    var param = 'orderId=' + jQuery('#orderId').val();
    jQuery.ajax({url: 'VoidOrder',
        data: param,
        type: 'post',
        async: false,
        success: function(data) {
            getResultOfVoidOrder(data);
        },
        error: function(data) {
            getResultOfVoidOrder(data);
        }
    });
    pleaseWait('N');
}

function voidOrder(cleanErrors) {
    if (cleanErrors == undefined) {
        cleanErrors = "Y";
    }
    if (cleanErrors == "Y") {
        hideOverlayDiv();
        jQuery('#voidOrderFormServerError').html("");
        jQuery('#orderId').val("");
    }
    jQuery('#voidOrder').show();
    jQuery('#orderId').focus();
}

function getResultOfVoidOrder(data) {
    var serverError = getServerError(data);
    if (serverError != "") {
        jQuery('#voidOrderFormServerError').html(serverError);
        voidOrder('N');
    } else {
        clearErrorMessages();
        updateCart();
        productToSearchFocus();
    }
}