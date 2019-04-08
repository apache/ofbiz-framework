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
var variantData = null;

jQuery(document).ready(function() {
    jQuery('#variantQuantity').bind('keypress', function(event) {
        code = event.keyCode ? event.keyCode : event.which;
        if (code.toString() == 13) {
            variantConfirmed();
            return false;
        }
        if (code.toString() == 27) {
            productToSearchFocus();
            return false;
        }
    });
    
    jQuery('#chooseVariantConfirm').bind('click', function(event) {
        variantConfirmed();
        return false;
    });
    
    jQuery('#chooseVariantCancel').bind('click', function(event) {
        productToSearchFocus();
        return false;
    });
});

function variantConfirmed() {
    pleaseWait('Y');
    if (jQuery('#variant').val() == 'Y') {
        var param = 'add_product_id=' + jQuery('#variantProductId').val() + '&quantity=' + jQuery('#variantQuantity').val() +
                    '&add_amount=' + jQuery('#amount').val();
        jQuery.ajax({url: 'AddToCart',
            data: param,
            type: 'post',
            async: false,
            success: function(data) {
                getResultOfVariantAddItem(data);
            },
            error: function(data) {
                getResultOfVariantAddItem(data);
            }
        });
    } else {
        jQuery.ajax({url: 'AddToCart',
            data: jQuery('#ChooseVariantForm').serialize(),
            type: 'post',
            async: false,
            success: function(data) {
               getResultOfVariantAddItem(data);
            },
            error: function(data) {
                getResultOfVariantAddItem(data);
            }
        });
    }
    pleaseWait('N');
}

function getResultOfVariantAddItem(data) {
    var serverError = getServerError(data);
    if (serverError != "") {
        jQuery('#errors').fadeIn('slow', function() {
            jQuery('#errors').html(serverError);
        });
    } else {
        clearErrorMessages();
        updateCart();
        productToSearchFocus();
    }
}

function chooseVariant(cleanErrors, data) {
    if (cleanErrors == undefined) {
        cleanErrors = "Y";
    } 
    if (cleanErrors == "Y") {
        hideOverlayDiv();
        jQuery('#chooseVariantFormServerError').html("");
        jQuery('#variantQuantity').val("1");
    }
    variantData = data;
    
    var requireAmount = data.product.requireAmount;
    if (requireAmount == null) {
        requireAmount = "N";
    }
    if (requireAmount == "Y") {
        jQuery('#addAmount').show();
    } else {
        jQuery('#addAmount').hide();
    }
    
    var virtualVariantMethodEnum = data.product.virtualVariantMethodEnum;
    if (virtualVariantMethodEnum == null) {
        virtualVariantMethodEnum = "VV_VARIANTTREE";
    }
    if (virtualVariantMethodEnum == "VV_VARIANTTREE") {
        selectVariant(variantData, 'Y');
    }
    if (virtualVariantMethodEnum == "VV_FEATURETREE") {
        selectFeature(variantData, 'Y', data.product.productId);
    }
}

function selectVariant(data, firstTime) {
    var variantTree = data.variantTree;
    var variantTreeSize = data.variantTreeSize;
    var featureOrder = data.featureOrder;
    var featureTypes = data.featureTypes;
    var variantSampleList = data.variantSampleList;
    var selectList = "";
    var featureValue = "";
    if (variantTree != undefined && variantTreeSize > 0) {
        if (featureOrder != undefined && featureOrder.length > 0) {
            if (variantSampleList != undefined && variantSampleList.length > 0) {
                if (featureTypes != undefined) {
                    var idx = 0;
                    var previousFeatureValue = "";
                    jQuery.each(featureOrder, function(i, actualFeature) {
                        var feature = featureTypes[actualFeature];
                        if (idx == 0) {
                            selectList = selectList + feature + " ";
                            selectList = selectList + "<select id=\"FT" + feature + "\" name=\"FT" + feature + "\">";
                            for (i = 0; i < variantSampleList.length; i++) {
                                selectList = selectList + "<option"
                                if (firstTime == 'N') {
                                    var selectedFeatureValue = "#FT" + feature;
                                    featureValue = jQuery(selectedFeatureValue).val();
                                    if (featureValue == variantSampleList[i]) {
                                        selectList = selectList + " selected";
                                    }
                                } else {
                                    featureValue = variantSampleList[0];
                                }
                                selectList = selectList + ">" + variantSampleList[i] + "</option>";
                            }
                            selectList = selectList + "</select><br/>";
                        } else {
                            var features = variantTree[previousFeatureValue];
                            selectList = selectList + feature + " ";
                            selectList = selectList + "<select id=\"FT" + feature + "\" name=\"FT" + feature + "\">";
                            for (var key in features) {
                                selectList = selectList + "<option"
                                if (firstTime == 'N') {
                                    var selectedFeatureValue = "#FT" + feature;
                                    var featureValue = jQuery(selectedFeatureValue).val();
                                    if (featureValue == key) {
                                        selectList = selectList + " selected";
                                    }
                                }
                                selectList = selectList + ">" + key + "</option>";
                            }
                            selectList = selectList + "</select><br/>";
                        }
                        idx++;
                        previousFeatureValue = featureValue;
                    });
                    jQuery('#features').html(selectList);
                    var idx = 0;
                    var features = variantTree;
                    jQuery.each(featureOrder, function(i, actualFeature) {
                        var feature = featureTypes[actualFeature];
                        featureValue = "FT" + feature;
                        jQuery('#' + featureValue).bind('change', function(event) {
                            selectVariant(variantData, 'N');
                            return false;
                        });
                        var featValue = jQuery('#' + featureValue).val();
                        idx++;
                        for (var key in features) {
                            if (key == featValue) {
                                if (idx == featureOrder.length) {
                                    jQuery('#variantProductId').val(features[key]);
                                    var param = 'productId=' + jQuery('#variantProductId').val();
                                    jQuery.ajax({url: 'GetProductAndPrice',
                                        data: param,
                                        type: 'post',
                                        async: false,
                                        success: function(data) {
                                            getResultOfGetProductAndPrice(data);
                                        },
                                        error: function(data) {
                                            getResultOfGetProductAndPrice(data);
                                        }
                                    });
                                } else {
                                    features = features[key];
                                }
                            }
                        }
                    });
                }
            }
        }
    }
    jQuery('#variant').val('Y');
    jQuery('#features').show();    
    jQuery('#chooseVariant').show();    
    jQuery('#variantQuantity').focus();
}

function selectFeature(data, firstTime, productId) {
    var featureLists = data.featureLists;
    if (featureLists != undefined && featureLists.length > 0) {
        var selectList = "";
        for (i = 0; i < featureLists.length; i++) {
            featureList = featureLists[i];
            for (j = 0; j < featureList.length; j++) {
                var feature = featureList[j];
                if (j == 0) {
                    selectList = selectList + feature.description + " ";
                    selectList = selectList + "<select id=\"FT" + feature.productFeatureTypeId + "\" name=\"FT" + feature.productFeatureTypeId + "\">";
                } else {
                    selectList = selectList + "<option value='";
                    selectList = selectList + feature.productFeatureId;
                    selectList = selectList + "'>";
                    selectList = selectList + feature.description;
                    if (feature.price != null) {
                        var amount = getFormattedProductFeaturePrice(feature.productFeatureId, "DEFAULT_PRICE", feature.currencyUomId);
                        selectList = selectList + " (+ " + amount + ") ";
                    }
                    selectList = selectList + "</option>";
                }
            }
            selectList = selectList + "</select><br/>";
        }
        jQuery('#variantProductId').val(productId);
        jQuery('#features').html(selectList);
    }
    jQuery('#variant').val('N');
    jQuery('#features').show();
    jQuery('#chooseVariant').show();
    jQuery('#variantQuantity').focus();
}

function getResultOfGetProductAndPrice(data) {
    var serverError = getServerError(data);
    if (serverError != "") {
        jQuery('#chooseVariantFormServerError').update(serverError);
        chooseVariant('N', data);
    } else {
        clearErrorMessages();
        var product = data.product;
        var price = data.price;
        var currencyUomId = data.currencyUomId;
        if (product.productName != null) {
            jQuery('#variantProductDescription').val(product.productName);
        } else if (product.productDescription != null) {
            jQuery('#variantProductDescription').val(product.productDescription);
        } else if (product.longDescription != null) {
            jQuery('#variantProductDescription').val(product.longDescription);
        }
        var formattedPrice = getFormattedAmount(price, currencyUomId);
        jQuery('#variantProductPrice').val(formattedPrice);
    }
}

function getFormattedProductFeaturePrice(productFeatureIdIn, productPriceTypeIdIn, currencyUomIdIn) {
    var formattedAmount = "";
    var param = 'productFeatureId=' + productFeatureIdIn + '&productPriceTypeId=' + productPriceTypeIdIn + '&currencyUomId=' + currencyUomIdIn;
    jQuery.ajax({url: 'GetFormattedProductFeaturePrice',
        data: param,
        type: 'post',
        async: false,
        success: function(data) {
            formattedAmount = getResultOfGetFormattedProductFeaturePrice(data);
        },
        error: function(data) {
            formattedAmount = getResultOfGetFormattedProductFeaturePrice(data);
        }
    });
    return formattedAmount;
}

function getResultOfGetFormattedProductFeaturePrice(data) {
    return data.formattedFeaturePrice;
}