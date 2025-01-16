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
package org.apache.ofbiz.order.customer

static Map shipToAddress(Map parameters) {
    Map processedMap = [:]
    if (parameters.shipToContactMechId) {
        processedMap.contactMechId = parameters.shipToContactMechId
    }
    if (parameters.shipToName) {
        processedMap.toName = parameters.shipToName
    }
    if (parameters.shipToAttnName) {
        processedMap.attnName = parameters.shipToAttnName
    }
    if (parameters.shipToAddress1) {
        processedMap.address1 = parameters.shipToAddress1
    } else {
        return error(label('PartyUiLabels', 'PartyAddressLine1MissingError'))
    }
    if (parameters.shipToAddress2) {
        processedMap.address2 = parameters.shipToAddress2
    }
    if (parameters.shipToCity) {
        processedMap.city = parameters.shipToCity
    } else {
        return error(label('PartyUiLabels', 'PartyCityMissing'))
    }
    if (parameters.shipToStateProvinceGeoId) {
        processedMap.stateProvinceGeoId = parameters.shipToStateProvinceGeoId
    } else {
        return error(label('PartyUiLabels', 'PartyStateMissingError'))
    }
    if (parameters.shipToPostalCode) {
        processedMap.postalCode = parameters.shipToPostalCode
    } else {
        return error(label('PartyUiLabels', 'PartyPostalInformationNotFound'))
    }
    if (parameters.shipToCountryGeoId) {
        processedMap.countryGeoId = parameters.shipToCountryGeoId
    } else {
        return error(label('PartyUiLabels', 'PartyCountryMissing'))
    }
    return processedMap
}

static Map billToAddress(Map parameters) {
    Map processedMap = [:]
    if (parameters.billToContactMechId) {
        processedMap.contactMechId = parameters.billToContactMechId
    }
    if (parameters.billToName) {
        processedMap.toName = parameters.billToName
    }
    if (parameters.billToAttnName) {
        processedMap.attnName = parameters.billToAttnName
    }
    if (parameters.billToAddress1) {
        processedMap.address1 = parameters.billToAddress1
    } else {
        return error(label('PartyUiLabels', 'PartyAddressLine1MissingError'))
    }
    if (parameters.billToAddress2) {
        processedMap.address2 = parameters.billToAddress2
    }
    if (parameters.billToCity) {
        processedMap.city = parameters.billToCity
    } else {
        return error(label('PartyUiLabels', 'PartyCityMissing'))
    }
    if (parameters.billToStateProvinceGeoId) {
        processedMap.stateProvinceGeoId = parameters.billToStateProvinceGeoId
    } else {
        return error(label('PartyUiLabels', 'PartyStateMissingError'))
    }
    if (parameters.billToPostalCode) {
        processedMap.postalCode = parameters.billToPostalCode
    } else {
        return error(label('PartyUiLabels', 'PartyPostalInformationNotFound'))
    }
    if (parameters.billToCountryGeoId) {
        processedMap.countryGeoId = parameters.billToCountryGeoId
    } else {
        return error(label('PartyUiLabels', 'PartyCountryMissing'))
    }
    return processedMap
}