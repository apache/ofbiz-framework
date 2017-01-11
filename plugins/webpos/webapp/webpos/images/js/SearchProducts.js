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
    productToSearchFocus();
    
    jQuery('#productToSearch').bind('keypress', function(event) {
        code = event.keyCode ? event.keyCode : event.which;
        if (code.toString() == 13) {
            productSearch();
            return false;
        }
    });
    
    jQuery('#searchBy').bind('change', function(event) {
        productToSearchFocus();
        return false;
    });
    
    jQuery('#productSearchConfirm').bind('click', function(event) {
        productSearch();
        return false;
    });
});

function productToSearchFocus() {
    hideOverlayDiv();
    jQuery('#productToSearch').focus();
    return false;
}

function productSearch() {
    pleaseWait('Y');
    var param = 'goodIdentificationTypeId=' + jQuery('#goodIdentificationTypeId').val();
    if (jQuery('#searchBy').val() == "productName") {
        param = param + '&searchByProductName=' + jQuery('#productToSearch').val();
    } else if (jQuery('#searchBy').val() == "productDescription") {
        param = param + '&searchByProductDescription=' + jQuery('#productToSearch').val();
    } else if (jQuery('#searchBy').val() == "idValue") {
        param = param + '&searchByProductIdValue=' + jQuery('#productToSearch').val();
    }
    jQuery.ajax({url: 'FindProducts',
        data: param,
        type: 'post',
        async: false,
        success: function(data) {
            var products = data.productsList;
            // automatically add item to cart if returned only one product
            if (products.length == 1) {
                addItem(products[0].productId, '1', 'Y');
            } else {
                buildProductsResults(products, 'Y');
            }
        },
        error: function(data) {
            alert("Error during product searching");
        }
    });
    pleaseWait('N');
}