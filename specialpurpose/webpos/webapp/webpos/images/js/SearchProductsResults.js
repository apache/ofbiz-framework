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
    jQuery('#searchByProductIdValue').bind('keypress', function(event) {
        code = event.keyCode ? event.keyCode : event.which;   
        if (code.toString() == 13) {
            productSearchAgain();
            return false;
        }
        if (code.toString() == 27) {
            productToSearchFocus();
            return false;
        }
    });
    
    jQuery('#searchByProductName').bind('keypress', function(event) {
        code = event.keyCode ? event.keyCode : event.which;   
        if (code.toString() == 13) {
            productSearchAgain();
            return false;
        }
        if (code.toString() == 27) {
            productToSearchFocus();
            return false;
        }
    });
    
    jQuery('#searchByProductDescription').bind('keypress', function(event) {
        code = event.keyCode ? event.keyCode : event.which;   
        if (code.toString() == 13) {
            productSearchAgain();
            return false;
        }
        if (code.toString() == 27) {
            productToSearchFocus();
            return false;
        }
    });
    
    jQuery('#searchProductsResultsSearch').bind('click', function(event) {
        productSearchAgain();
    });
        
    jQuery('#searchProductsResultsCancel').bind('click', function(event) {
        jQuery('#searchProductsResults').hide();
        productToSearchFocus();
    });
});

function buildProductsResults(products, fromProductSearch) {
    var alt_row = false;
    var idx = 1;
    var tableList = "";
    jQuery.each(products, function(i, product) {
        if (alt_row) {
            classType = "class=\"pos-cart-even\"";
        } else {
            classType = "class=\"pos-cart-odd\"";
        }
        productName = checkNull(product.productName);
        productDescription = checkNull(product.description);
        tableList = tableList + "<tr " + classType + ">";
        tableList = tableList + "<td><input type=\"hidden\" name=\"selectedProductId\" value=\"" + product.productId + "\"/></td>";
        tableList = tableList + "<td><a href=\"javascript:addItem(\'" + product.productId + "\', '1', 'Y');\">" + product.productId + "</a></td>";
        tableList = tableList + "<td><a href=\"javascript:addItem(\'" + product.productId + "\', '1', 'Y');\">" + productName + "</a></td>";
        tableList = tableList + "<td><a href=\"javascript:addItem(\'" + product.productId + "\', '1', 'Y');\">" + productDescription + "</a></td>";
        tableList = tableList + "</tr>";
        alt_row = !alt_row;
        idx++;
    });
    jQuery('#searchProductsResultsList').html(tableList);
    jQuery('#searchProductsResults').show();
    if (fromProductSearch == 'Y') {
        if (jQuery('#searchBy').val() == 'productName') {
            jQuery('#searchByProductIdValue').val("");
            jQuery('#searchByProductName').val(jQuery('#productToSearch').val());
            jQuery('#searchByProductDescription').val("");
            jQuery('#searchByProductName').focus();
        }
        if (jQuery('#searchBy').val() == 'productDescription') {
            jQuery('#searchByProductIdValue').val("");
            jQuery('#searchByProductName').val("");
            jQuery('#searchByProductDescription').val(jQuery('#productToSearch').val());
            jQuery('#searchByProductDescription').focus();
        }
        if (jQuery('#searchBy').val() == 'idValue') {
            jQuery('#searchByProductIdValue').val(jQuery('#productToSearch').val());
            jQuery('#searchByProductName').val("");
            jQuery('#searchByProductDescription').val("");
            jQuery('#searchByProductIdValue').focus();
        }
    }
}

function productSearchAgain() {
    pleaseWait('Y');
    var param = 'goodIdentificationTypeId=' + jQuery('#goodIdentificationTypeId').val() +
                '&searchByProductName=' + jQuery('#searchByProductName').val() +
                '&searchByProductDescription=' + jQuery('#searchByProductDescription').val() +
                '&searchByProductIdValue=' + jQuery('#searchByProductIdValue').val();
    jQuery.ajax({url: 'FindProducts',
        data: param,
        type: 'post',
        async: false,
        success: function(data) {
            var products = data.productsList;
            buildProductsResults(products, 'N');
        },
        error: function(data) {
            alert("Error during product searching again");
        }
    });
    pleaseWait('N');
}