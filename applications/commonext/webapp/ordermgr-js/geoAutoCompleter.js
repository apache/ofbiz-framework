/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

function getCountryList() {
    countryTargetField = jQuery('#shipToCountryGeo');
    countryDivToPopulate = jQuery('#shipToCountries');
    countryHiddenTarget = jQuery('#shipToCountryGeoId');
    jQuery.ajax({
        url: "getCountryList",
        type: "POST",
        async: false,
        success: callCountryAutocompleter
    });
}

function callCountryAutocompleter(data) {
    countryList = data.countryList;
    countryTargetField.autocomplete({source: countryList, select: setKeyAsParameterAndGetStateList});
}

function setKeyAsParameterAndGetStateList(event, ui) {
    countryHiddenTarget.value = ui.item;
    getAssociatedStateListForAutoComplete();
}

function getAssociatedStateListForAutoComplete() {
    stateTargetField = jQuery('#shipToStateProvinceGeo');
    stateDivToPopulate = jQuery('#shipToStates');
    stateHiddenTarget = jQuery('#shipToStateProvinceGeoId');
    jQuery.ajax({
        url: "getAssociatedStateList",
        type: "POST",
        data: jQuery('#shippingForm').serialize(),
        async: false,
        success: function(data) {callStateAutocompleter(data); }
    });
}

function callStateAutocompleter(data){
    stateList = data.stateList;
    if (stateList.size() <= 1) {
        jQuery('#shipToStateProvinceGeo').value = "No States/Provinces exists";
        jQuery('#shipToStateProvinceGeoId').value = "_NA_";
        jQuery("#shipStates").fadeOut("fast");
        jQuery("#advice-required-shipToStateProvinceGeo").fadeOut("fast");
        jQuery("#shipToStateProvinceGeo").off("blur");
    } else {
        jQuery('#shipToStateProvinceGeo').value = "";
        jQuery('#shipToStateProvinceGeoId').value = "";
        jQuery("#shipStates").fadeIn("fast");
        jQuery("#shipToStateProvinceGeo").on("blur", function() {
            if (jQuery('#shipToStateProvinceGeo').val() == "") {
                jQuery("#advice-required-shipToStateProvinceGeo").fadeIn("fast");
            }
        });
    }
    stateTargetField.autocomplete({source: stateList, select: setKeyAsParameter});
}

function setKeyAsParameter(event, ui) {
    stateHiddenTarget.value = ui.item;
}

//Generic function for fetching country's associated state list.
function getAssociatedStateList(countryId, stateId, errorId, divId) {
    var countryGeoId = jQuery("#" + countryId).val();
    var requestToSend = "getAssociatedStateList";
    if (jQuery('#orderViewed').length) {
        requestToSend = "/ordermgr/control/getAssociatedStateList"
    }
    jQuery.ajax({
        url: requestToSend,
        type: "POST",
        data: {countryGeoId: countryGeoId},
        success: function(data) {
            if (data._ERROR_MESSAGE_ ) {
                // no data found/ error occurred
                return;
            }
            stateList = data.stateList;
            var stateSelect = jQuery("#" + stateId);
            stateSelect.find("option").remove();
            jQuery.each(stateList, function(state) {
                geoValues = this.split(': ');
                stateSelect.append(jQuery('<option value = '+geoValues[1]+' >'+geoValues[0]+'</option>'));
            });

            if (stateList.length <= 1) {
                if (jQuery("#" + divId).is(':visible') || jQuery("#" + errorId).is(':visible')) {
                    jQuery("#divId").fadeOut("fast");
                    jQuery("#errorId").fadeOut("fast");
                    jQuery("#stateId").off("blur");
                }
            } else {
                jQuery("#divId").fadeIn("fast");
                jQuery("#stateId").on("blur", function() {
                    if (jQuery("#" + stateId).val() == "") {
                        jQuery("#errorId").fadeIn("fast")
                    }
                });
            }
        }
    });
}