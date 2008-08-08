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
    getCountryList();
    Event.observe('shipToCountryGeo', 'change', function() {
        $('shipToStateProvinceGeo').value = "";
    });
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
    getAssociatedStateList();
}

function getAssociatedStateList() {
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
    new Autocompleter.Local(stateTargetField, stateDivToPopulate, $H(geos), { partialSearch: false, afterUpdateElement: setKeyAsParameter });
}

function setKeyAsParameter(text, li) {
    stateHiddenTarget.value = li.id;
}