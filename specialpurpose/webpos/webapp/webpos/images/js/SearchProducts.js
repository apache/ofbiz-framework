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

jQuery(document).ready( function() { 

    // Autocompleter for good identification field
    var productsIdent = [];
    var productsIdIdent = [];
            
    jQuery( "#productGoodIdentification" ).autocomplete({
        minLength: 2,
        source: function( request, response ) {
            var term = request.term;
            term = 'productGoodIdentification=' + term;
            jQuery.ajax({
                url: "FindProductsByIdentification",
                type: 'POST',
                async: false,
                data: term,
                success: function(data) {
                    productsIdent = data.productsList;
                    productsIdIdent = data.productsId;
                    response( productsIdent );    
                }
    
            });
        },
        select: function( event, ui ) {
            var identValues = ui.item.value;
    
            jQuery.each(productsIdent, function(product) {
                if (identValues == this) {
                    document.getElementById('add_product_id').value = productsIdIdent[product];
                    return false;
                }
            });
        }
    });

    // Autocompleter for search by field
    jQuery('#searchBy').change( function(s) {
        document.getElementById('add_product_id').value = '';
        document.getElementById('productToSearch').value = '';
        jQuery('#productToSearch').focus();
    });
    
    jQuery( "#productToSearch" ).autocomplete({
        minLength: 2,
        source: function( request, response ) {
            var term = request.term;
            term = 'searchBy=' + document.getElementById('searchBy').value + '&productToSearch=' + term;
            jQuery.ajax({
                url: "FindProducts",
                async: false,
                type: 'POST',
                data: term,
                success: function(data) {
                    products = data.productsList;
                    productsId = data.productsId;
                    response( products );    
                }
    
            });
        },
        select: function( event, ui ) {
            var productToSearchValues = ui.item.value;
    
            jQuery.each(products, function(product){
                if (productToSearchValues == this) {
                    document.getElementById('add_product_id').value = productsId[product];
                    return false;
                }
            });
        }
    });
    
});