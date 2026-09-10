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
package org.apache.ofbiz.accounting.agreement

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ModelService
import org.apache.ofbiz.service.ServiceUtil

/**
 * Copy an existing Agreement
 */

Map copyAgreement() {
    GenericValue agreement = from('Agreement').where(parameters).queryOne()
    if (!agreement) {
        return error(label('AccountingUiLabels', 'AccountingAgreementNotFound', parameters))
    }
    Map serviceResult = success()
    Map createAgreementInMap = dispatcher.getDispatchContext().makeValidContext('createAgreement', ModelService.IN_PARAM, agreement)
    Map result = run service: 'createAgreement', with: createAgreementInMap
    if (ServiceUtil.isError(result)) {
        return result
    }
    String agreementIdTo = result.agreementId
    List agreementItems = agreement.getRelated('AgreementItem', null, null, false)
    agreementItems.each { agreementItem ->
        Map createAgreementItemInMap = dispatcher.getDispatchContext().makeValidContext('createAgreementItem',
                ModelService.IN_PARAM, agreementItem)
        createAgreementItemInMap.agreementId = agreementIdTo
        result = run service: 'createAgreementItem', with: createAgreementItemInMap
    }
    if (parameters.copyAgreementTerms == 'Y') {
        List agreementTerms = agreement.getRelated('AgreementTerm', null, null, false)
        agreementTerms.each { agreementTerm ->
            Map createAgreementTermInMap = dispatcher.getDispatchContext().makeValidContext('createAgreementTerm',
                    ModelService.IN_PARAM, agreementTerm)
            createAgreementTermInMap.agreementId = agreementIdTo
            createAgreementTermInMap.remove('agreementTermId')
            result = run service: 'createAgreementTerm', with: createAgreementTermInMap
        }
    }
    if (parameters.copyAgreementProducts == 'Y') {
        List agreementProductAppls = agreement.getRelated('AgreementProductAppl', null, null, false)
        agreementProductAppls.each { agreementProductAppl ->
            Map createAgreementProductApplInMap = dispatcher.getDispatchContext().makeValidContext('createAgreementProductAppl',
                    ModelService.IN_PARAM, agreementProductAppl)
            createAgreementProductApplInMap.agreementId = agreementIdTo
            result = run service: 'createAgreementProductAppl', with: createAgreementProductApplInMap
        }
    }
    if (parameters.copyAgreementFacilities == 'Y') {
        List agreementFacilityAppls = agreement.getRelated('AgreementFacilityAppl', null, null, false)
        agreementFacilityAppls.each { agreementFacilityAppl ->
            Map createAgreementFacilityApplInMap = dispatcher.getDispatchContext().makeValidContext('createAgreementFacilityAppl',
                    ModelService.IN_PARAM, agreementFacilityAppl)
            createAgreementFacilityApplInMap.agreementId = agreementIdTo
            result = run service: 'createAgreementFacilityAppl', with: createAgreementFacilityApplInMap
        }
    }
    if (parameters.copyAgreementParties == 'Y') {
        List agreementPartyApplics = agreement.getRelated('AgreementPartyApplic', null, null, false)
        agreementPartyApplics.each { agreementPartyApplic ->
            Map createAgreementPartyApplicInMap = dispatcher.getDispatchContext().makeValidContext('createAgreementPartyApplic',
                    ModelService.IN_PARAM, agreementPartyApplic)
            createAgreementPartyApplicInMap.agreementId = agreementIdTo
            result = run service: 'createAgreementPartyApplic', with: createAgreementPartyApplicInMap
        }
    }
    serviceResult.agreementId = agreementIdTo
    return serviceResult
}
