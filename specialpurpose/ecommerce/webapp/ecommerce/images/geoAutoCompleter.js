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

Event.observe(window, 'load', function() {
    // Autocompleter for shipping panel
    // Preventing getCountryList() from calling and not removed all autocompleter functions so that we can reuse in future.
    //getCountryList();
});

function getCountryList() {
    countryTargetField = $('shipToCountryGeo');
    countryDivToPopulate = $('shipToCountries');
    countryHiddenTarget = $('shipToCountryGeoId');
    new Ajax.Request("getCountryList", {
        asynchronous: false,
        onSuccess: callCountryAutocompleter
    });
}

function callCountryAutocompleter(transport) {
    var geos = new Hash();
    var data = transport.responseText.evalJSON(true);
    countryList = data.countryList;
    countryList.each(function(country) {
        var countryName = country.split(': ');
        geos.set(countryName[1], countryName[0]);
    });
    new Autocompleter.Local(countryTargetField, countryDivToPopulate, $H(geos), { partialSearch: false, afterUpdateElement: setKeyAsParameterAndGetStateList});
}

function setKeyAsParameterAndGetStateList(text, li) {
    countryHiddenTarget.value = li.id;
    getAssociatedStateListForAutoComplete();
}

function getAssociatedStateListForAutoComplete() {
    stateTargetField = $('shipToStateProvinceGeo');
    stateDivToPopulate = $('shipToStates');
    stateHiddenTarget = $('shipToStateProvinceGeoId');
    new Ajax.Request("getAssociatedStateList", {
        asynchronous: false,
        parameters: $('shippingForm').serialize(),
        onSuccess: callStateAutocompleter
    });
}

function callStateAutocompleter(transport) {
    var geos = new Hash();
    var data = transport.responseText.evalJSON(true);
    stateList = data.stateList;
    stateList.each(function(state) {
        var stateName = state.split(': ');
        geos.set(stateName[1], stateName[0]);
    });
    if (stateList.size() <= 1) {
        $('shipToStateProvinceGeo').value = "No States/Provinces exists";
        $('shipToStateProvinceGeoId').value = "_NA_";
        Effect.Fade('shipStates', {duration: 0.0});
        Effect.Fade('advice-required-shipToStateProvinceGeo', {duration: 0.0});
        Event.stopObserving($('shipToStateProvinceGeo'), 'blur');
    } else {
        $('shipToStateProvinceGeo').value = "";
        $('shipToStateProvinceGeoId').value = "";
        Effect.Appear('shipStates', {duration: 0.0});
        Event.observe($('shipToStateProvinceGeo'), 'blur', function() {
            if ($('shipToStateProvinceGeo').value == "") {
                Effect.Appear('advice-required-shipToStateProvinceGeo', {duration: 0.0});
            }
        });
    }
    new Autocompleter.Local(stateTargetField, stateDivToPopulate, $H(geos), { partialSearch: false, afterUpdateElement: setKeyAsParameter });
}

function setKeyAsParameter(text, li) {
    stateHiddenTarget.value = li.id;
}

//Generic function for fetching country's associated state list.
function getAssociatedStateList(countryId, stateId, errorId, divId) {
    var optionList = [];
    var requestToSend = "getAssociatedStateList";
    if ($('orderViewed')) {
        requestToSend = "/ordermgr/control/getAssociatedStateList"
    }
    new Ajax.Request(requestToSend, {
        asynchronous: false,
        parameters: {countryGeoId:$F(countryId)},
        onSuccess: function(transport) {
            var data = transport.responseText.evalJSON(true);
            stateList = data.stateList;
            stateList.each(function(state) {
                geoValues = state.split(': ');
                optionList.push("<option value = "+geoValues[1]+" >"+geoValues[0]+"</option>");
            });
            $(stateId).update(optionList);
            if (stateList.size() <= 1) {
                if ($(divId).visible() || $(errorId).visible()) {
                    Effect.Fade(divId, {duration: 0.0});
                    Effect.Fade(errorId, {duration: 0.0});
                    Event.stopObserving(stateId, 'blur');
                }
            } else {
                Effect.Appear(divId, {duration: 0.0});
                Event.observe(stateId, 'blur', function() {
                    if ($F(stateId) == "") {
                        Effect.Appear(errorId, {duration: 0.0});
                    }
                });
            }
        }
    });
}