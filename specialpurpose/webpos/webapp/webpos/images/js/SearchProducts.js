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

Event.observe(window, 'load', function() {

    // Autocompleter for good identification field
    var j = 0;
    var autoCompleteIdent = null;
    var productsIdent = [];
    var productsIdIdent = [];
    var previousIdent = '';
    Event.observe($('productGoodIdentification'), 'focus', function(s) {
        var ident = $('productGoodIdentification').value;
        if (j == 0 || previousIdent != ident) {
            var pars = 'productGoodIdentification' + $('productGoodIdentification').value;
            new Ajax.Request("FindProductsByIdentification",
            {
                asynchronous: false,
                parameters: pars,
                onSuccess: function(transport) {
                var data = transport.responseText.evalJSON(true);
                productsIdent = data.productsList;
                productsIdIdent = data.productsId;
                autoComplete = new Autocompleter.Local('productGoodIdentification', 'productsIdent', productsIdent, {partialSearch: false});
            }
        });
        previousIdent = ident;
        j++;
        }
    });

    Event.observe($('productGoodIdentification'), 'blur', function(s) {
        identValues = $('productGoodIdentification').value;
        var l = 0;
        productsIdent.each(function(product) {
            if (identValues == product) {
                $('add_product_id').value = productsIdIdent[l];
                throw $break;
            }
            l++;
        });
    });

    // Autocompleter for search by field
    Event.observe($('searchBy'), 'change', function(s) {
        $('add_product_id').value = '';
        $('productToSearch').value = '';
        Form.Element.focus('productToSearch');
    });

    var i = 0;
    var autoComplete = null;
    var products = [];
    var productsId = [];
    var previousSearchBy = '';
    Event.observe($('productToSearch'), 'focus', function(s) {
        var searchBy = $('searchBy').value;
        if (i == 0 || previousSearchBy != searchBy) {
            var pars = 'searchBy=' + $('searchBy').value + '&productToSearch=' + $('productToSearch').value;
            new Ajax.Request("FindProducts",
            {
                asynchronous: false,
                parameters: pars,
                onSuccess: function(transport) {
                var data = transport.responseText.evalJSON(true);
                products = data.productsList;
                productsId = data.productsId;
                autoComplete = new Autocompleter.Local('productToSearch', 'products', products, {partialSearch: false});
            }
        });
        previousSearchBy = searchBy;
        i++;
        }
    });

    Event.observe($('productToSearch'), 'blur', function(s) {
        productToSearchValues = $('productToSearch').value;
        var p = 0;
        products.each(function(product){
            if (productToSearchValues == product) {
                $('add_product_id').value = productsId[p];
                throw $break;
            }
            p++;
        });
    });
});