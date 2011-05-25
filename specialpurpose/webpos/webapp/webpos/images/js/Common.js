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
    activateHotKeys();
});

function updateCart() {
    jQuery('#CartList').load('ShowCart'); 
}

function updateCustomerAddress() {
    jQuery('#CustomerAddressList').load('CustomerAddress');
}

function updateCartItemSelected(lineIndex, focusOnQnt) {
    if (lineIndex == null) {
        lineIndex = 0;
    }
    jQuery('#CartItemSelected').load('ShowCartItemSelected', {cartLineIndex:lineIndex, focusOnQuantity:focusOnQnt});
}

function addItem(product_Id, qnt, updCart) {
    pleaseWait('Y');
    var param = 'productId=' + product_Id;
    jQuery.ajax({url: 'GetProductType',
        data: param,
        type: 'post',
        async: false,   
        success: function(data) {
            getResultOfCheckProductType(data, product_Id, qnt, updCart);
        },
        error: function(data) {
            getResultOfCheckProductType(data, product_Id, qnt, updCart);
        }
    });
    pleaseWait('N');
}

function getResultOfCheckProductType(data, productId, qnt, updCart) {
    var serverError = getServerError(data);
    if (serverError != "") {
        jQuery('#errors').fadeIn('slow', function() {
            jQuery('#errors').html(serverError);
        });
    } else {
        var isVirtual = data.product.isVirtual;
        if (isVirtual == null) {
            isVirtual = "N";
        }
        if (isVirtual == "N") {
            var param = 'add_product_id=' + productId + "&quantity=" + qnt;
            jQuery.ajax({url: 'AddToCart',
                data: param,
                type: 'post',
                async: false,        
                success: function(data) {
                    getResultOfAddItem(data, updCart);
                },
                error: function(data) {
                    getResultOfAddItem(data, updCart);
                }
            });
        } else {
            chooseVariant('Y', data);
        }
    }
}

function getResultOfAddItem(data, updCart) {
    var serverError = getServerError(data);
    if (serverError != "") {
        jQuery('#errors').fadeIn('slow', function() {
            jQuery('#errors').html(serverError);
        });
    } else {
        if (updCart == "Y") {
            jQuery('#errors').fadeOut('slow');
            updateCart();
            if (data.itemId != undefined) {
                updateCartItemSelected(data.itemId, 'N');
            }
            jQuery('#productToSearch').val('');
            jQuery('#productToSearch').focus();
        }
    }
}

// Check server side error
function getServerError(data) {
    var serverErrorHash = [];
    var serverError = "";
    if (data._ERROR_MESSAGE_LIST_ != undefined) {
        serverErrorHash = data._ERROR_MESSAGE_LIST_;
        jQuery.each(serverErrorHash, function(i, error) {
          if (error != undefined) {
              if (error.message != undefined) {
                  serverError += error.message;
              } else {
                  serverError += error;
              }
            }
        });
    }
    if (data._ERROR_MESSAGE_ != undefined) {
        serverError = data._ERROR_MESSAGE_;
    }
    return serverError;
}

function hideOverlayDiv() {
    jQuery('#payCash').hide();
    jQuery('#payCheck').hide();
    jQuery('#payGiftCard').hide();
    jQuery('#payCreditCard').hide();
    jQuery('#openTerminal').hide();
    jQuery('#closeTerminal').hide();
    jQuery('#paidOutAndIn').hide();
    jQuery('#promo').hide();
    jQuery('#voidOrder').hide();
    jQuery('#chooseVariant').hide();
    jQuery('#searchProductsResults').hide();
    jQuery('#searchPartiesResults').hide();
    jQuery('#editAddress').hide();
    pleaseWait('N');
}

function pleaseWait(wait) {
    if (wait == "Y") {
        jQuery('#pleaseWait').show();
    }
    else {
        jQuery('#pleaseWait').hide();
    }
}

function getFormattedAmount(amountStr, currencyStr) {
    var formattedAmount = "";
    var param = 'amount=' + amountStr + "&currencyId=" + currencyStr;
    jQuery.ajax({url: 'GetFormattedAmount',
        data: param,
        type: 'post',
        async: false,
        success: function(data) {
            formattedAmount = getResultOfGetFormattedAmount(data);
        },
        error: function(data) {
            formattedAmount = getResultOfGetFormattedAmount(data);
        }
    });
    return formattedAmount;
}

function getResultOfGetFormattedAmount(data) {
    return data.formattedAmount;
}

function emptyCart() {
    pleaseWait('Y');
    jQuery.ajax({url: 'EmptyCart',
        type: 'post',
        async: false,
        success: function(data) {
            getResultOfEmptyCart(data);
        },
        error: function(data) {
            getResultOfEmptyCart(data);
        }
    });
    pleaseWait('N');
    return false;
}

function getResultOfEmptyCart(data) {
    var serverError = getServerError(data);
    if (serverError != "") {
        jQuery('#errors').fadeIn('slow', function() {
            jQuery('#errors').html(serverError);
        });
    } else {
        jQuery('#errors').fadeOut('slow');
        updateCart();
        updateCartItemSelected(null, 'N');
        updateCustomerAddress();
        hideOverlayDiv();
        clearSearchResults();
    }
    productToSearchFocus();
}

function clearSearchResults() {
    jQuery('#productToSearch').val( '');
    jQuery('#partyToSearch').val('');
}

function checkNull(fieldToCheck) {
    if (fieldToCheck == null) {
        return "";
    } else {
        return fieldToCheck;
    }
}

function clearErrorMessages() {
    jQuery('#errors').fadeOut('slow');
}