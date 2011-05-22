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
function cartItemSelectedEvents(focusOnQuantity) {
    jQuery('#itemQuantity').bind('keypress', function(event) {
        code = event.keyCode ? event.keyCode : event.which;
        if (code.toString() == 13) {
            updateCartItem();
            itemQuantityFocus();
            return false;
        } 
    });
    
    jQuery('#modifyPrice').bind('keypress', function(event) {
        code = event.keyCode ? event.keyCode : event.which;
        if (code.toString() == 13) {
            modifyPrice();
            itemQuantityFocus();
            return false;
        } 
    });
    
    jQuery('#incrementQuantity').bind('click', function(event) {
        incrementItemQuantity();
        return false;
    });
    
    jQuery('#decrementQuantity').bind('click', function(event) {
        decrementItemQuantity();
        return false;
    });
    
    if (focusOnQuantity == undefined) {
        focusOnQuantity = 'Y';
    }
    if (focusOnQuantity == 'Y') {
        itemQuantityFocus();
    }
}

function incrementItemQuantity() {
    var qnt = parseInt(jQuery('#itemQuantity').val());
    if (!isNaN(qnt)) {
        qnt = qnt + 1;
        jQuery('#itemQuantity').val(qnt);
        updateCartItem();
    }
    itemQuantityFocus();
    return false;
}

function decrementItemQuantity() {
    var qnt = parseInt(jQuery('#itemQuantity').val());
    if (!isNaN(qnt) && qnt > 1) {
        qnt = qnt - 1;
        jQuery('#itemQuantity').val(qnt);
        updateCartItem();
    }
    itemQuantityFocus();
    return false;
}

function itemQuantityFocus() {
    hideOverlayDiv();
    jQuery('#itemQuantity').focus();
    return false;
}

function modifyPrice() {
    pleaseWait('Y');
    var param = 'cartLineIdx=' + jQuery('#cartLineIdx').val() + '&price=' + jQuery('#modifyPrice').val();
    jQuery.ajax({url: 'ModifyPrice',
        data: param,
        type: 'post',
        async: false,
        success: function(data) {
            getResultOfModifyPrice(data);
        },
        error: function(data) {
            getResultOfModifyPrice(data);
        }
    });
    pleaseWait('N');
}

function getResultOfModifyPrice(data) {
    var serverError = getServerError(data);
    if (serverError != "") {
        jQuery('#errors').fadeIn('slow', function() {
            jQuery('#errors').html(serverError);
        });
        jQuery('#itemQuantity').focus();
    } else {
        clearErrorMessages();
        jQuery('#itemQuantity').focus();
        updateCart();
    }
}

function updateCartItem() {
    pleaseWait('Y');
    var param = 'cartLineIndex=' + jQuery('#lineIndex').val() + '&quantity=' + jQuery('#itemQuantity').val();
    jQuery.ajax({url: 'UpdateCartItem',
        data: param,
        type: 'post',
        async: false,
        success: function(data) {
            getResultOfUpdateCartItem(data);
        },
        error: function(data) {
            getResultOfUpdateCartItem(data);
        }
    });
    pleaseWait('N');
}

function getResultOfUpdateCartItem(data) {
    var serverError = getServerError(data);
    if (serverError != "") {
        jQuery('#errors').fadeIn('slow', function() {
            jQuery('#errors').html(serverError);
        });
        jQuery('#itemQuantity').focus();
    } else {
        clearErrorMessages();
        jQuery('#itemQuantity').focus();
        updateCart();
    }
}

function deleteCartItem(cartLineIndex) {
    pleaseWait('Y');
    var param = 'cartLineIndex=' + cartLineIndex;
    jQuery.ajax({url: 'DeleteCartItem',
        data: param,
        type: 'post',
        async: false,
        success: function(data) {
            getResultOfDeleteCartItem(data);
        },
        error: function(data) {
            getResultOfDeleteCartItem(data);
        }
    });
    pleaseWait('N');
}

function getResultOfDeleteCartItem(data) {
    var serverError = getServerError(data);
    if (serverError != "") {
        jQuery('#errors').fadeIn('slow', function() {
            jQuery('#errors').html(serverError);
        });
        jQuery('#productToSearch').focus();
    } else {
        clearErrorMessages();
        jQuery('#productToSearch').focus();
        updateCart();
        updateCartItemSelected(null, 'N');
    }
}