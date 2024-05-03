/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License") you may not use this file except in compliance
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
package org.apache.ofbiz.accounting.tax

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.entity.GenericValue

import java.util.regex.Pattern

/**
 * create a PartyTaxAuthInfo
 * @return Success, error response otherwise.
 */
Map createPartyTaxAuthInfo() {
    GenericValue taxAuthority = from('TaxAuthority').where(parameters).queryOne()
    if (!taxAuthority) {
        return error(label('PartyUiLabels', 'PartyTaxAuthPartyAndGeoNotAvailable'))
    }
    String errorMesg = validatePartyTaxIdInline()
    if (errorMesg) {
        return error(errorMesg)
    }
    GenericValue partyAuthInfo = makeValue('PartyTaxAuthInfo', parameters)
    partyAuthInfo.fromDate = partyAuthInfo.fromDate ?: UtilDateTime.nowTimestamp()
    partyAuthInfo.create()
    return success()
}
/**
 * update a PartyTaxAuthInfo
 * @return Success, error response otherwise.
 */
Map updatePartyTaxAuthInfo() {
    String errorMesg = validatePartyTaxIdInline()
    if (errorMesg) {
        return error(errorMesg)
    }
    GenericValue partyAuthInfo = from('PartyTaxAuthInfo').where(parameters).queryOne()
    if (partyAuthInfo) {
        partyAuthInfo.setNonPKFields(parameters, false)
        partyAuthInfo.store()
    }
    return success()
}

/**
 * @return error message if party tax id not match the tax pattern
 */
String validatePartyTaxIdInline() {
    GenericValue taxAuthority = from('TaxAuthority').where(parameters).queryOne()
    if (taxAuthority && taxAuthority.taxIdFormatPattern && parameters.partyTaxId &&
            !Pattern.compile(taxAuthority.taxIdFormatPattern).matcher(parameters.partyTaxId).find()) {
        return label('AccountingErrorUiLabels', 'AccountingTaxIdInvalidFormat', [parameters: parameters, taxAuthority: taxAuthority])
    }
    return ''
}

/**
 * Create a Customer PartyTaxAuthInfo
 * @return Success, error response otherwise.
 */
Map createCustomerTaxAuthInfo() {
    List taxAuthPartyGeoIds = org.apache.ofbiz.base.util.StringUtil.split(parameters.taxAuthPartyGeoIds, '::')
    parameters.taxAuthPartyId = taxAuthPartyGeoIds[0]
    parameters.taxAuthGeoId = taxAuthPartyGeoIds[1]
    run service: 'createPartyTaxAuthInfo', with: parameters
    return success()
}
