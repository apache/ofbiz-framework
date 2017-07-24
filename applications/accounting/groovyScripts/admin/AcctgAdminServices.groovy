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

import java.sql.Timestamp
import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.entity.condition.EntityConditionBuilder
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.util.EntityUtil
import org.apache.ofbiz.service.ModelService


def createPartyAcctgPreference() {
    //check that the party is an INTERNAL_ORGANIZATION, as defined in PartyRole
    partyRole = select().from('PartyRole').where([partyId:parameters.partyId,roleTypeId:"INTERNAL_ORGANIZATIO"]).queryOne()
    if (!partyRole) {
        String errorMessage = UtilProperties.getMessage("AccountingUiLabels","AccountingPartyMustBeInternalOrganization", locale)
        logError(errorMessage)
        return error(errorMessage)
    }
    //Does not check if the Party is actually a company because real people have to pay taxes too

    //TODO: maybe check to make sure that all fields are not null
    newEntity = delegator.makeValidValue("PartyAcctgPreference", parameters)
    delegator.create(newEntity)
    return success()
}

def getPartyAccountingPreferences() {
    Map result = success()
    GenericValue aggregatedPartyAcctgPref = delegator.makeValidValue("PartyAcctgPreference", parameters)
    String currentOrganizationPartyId = parameters.organizationPartyId
    Boolean containsEmptyFields = true

    while (currentOrganizationPartyId && containsEmptyFields) {
        GenericValue currentPartyAcctgPref = select().from('PartyAcctgPreference').where([partyId:currentOrganizationPartyId]).queryOne()
        containsEmptyFields = false
        if (currentPartyAcctgPref) {
            for (String entityKey : currentPartyAcctgPref.keySet()) {
                Object entityValue = currentPartyAcctgPref.get(entityKey)
                if (entityValue) {
                    aggregatedPartyAcctgPref.put(entityKey,entityValue)
                } else {
                    containsEmptyFields = true
                }
            }
        } else {
            containsEmptyFields = true
        }
        List<GenericValue> parentPartyRelationships = select().from('PartyRelationship').where([partyIdTo:currentOrganizationPartyId, partyRelationshipTypeId:"GROUP_ROLLUP", roleTypeIdFrom:"_NA_", roleTypeIdTo:"_NA_"]).filterByDate().queryList()
        if (parentPartyRelationships) {
            GenericValue parentPartyRelationship = EntityUtil.getFirst(parentPartyRelationships)
            currentOrganizationPartyId = parentPartyRelationship.partyIdFrom
        } else {
            currentOrganizationPartyId = null
        }
    }

    if (aggregatedPartyAcctgPref) {
        aggregatedPartyAcctgPref.put("partyId",parameters.organizationPartyId)
        result.put("partyAccountingPreference", aggregatedPartyAcctgPref)
    }
    return result
}

def setAcctgCompany() {
    Map result = success()
    //Set user preference
    GenericValue partyAcctgPreference = select().from('PartyAcctgPreference').where([partyId: parameters.organizationPartyId]).queryOne()
    if (partyAcctgPreference) {
        result = runService("setUserPreference", [userPrefValue: parameters.organizationPartyId, userPrefGroupTypeId: "GLOBAL_PREFERENCES", userPrefTypeId: "ORGANIZATION_PARTY"])
    }
    result.put("organizationPartyId", parameters.organizationPartyId)

    return result
}

def updateFXConversion() {
    //Set the FX rate changes as of now
    Timestamp nowTimestamp = parameters.asOfTimestamp
    if (!nowTimestamp) {
        nowTimestamp = UtilDateTime.nowTimestamp()
    }

    //find the existing exchange rates for this currency pair
    exprBldr = new EntityConditionBuilder()
    condition = exprBldr.AND() {
        EQUALS(uomId: parameters.uomId)
        EQUALS(uomIdTo: parameters.uomIdTo)
    }
    if (parameters.purposeEnumId) {
        condition = exprBldr.AND(condition) {
            EQUALS(purposeEnumId: parameters.purposeEnumId)
        }
    }
    List<GenericValue> uomConversions = select().from('UomConversionDated').where(condition).orderBy("-fromDate").filterByDate().queryList()

    //expire all of them
    for (GenericValue uomConversion : uomConversions) {
        if (!parameters.fromDate) {
            uomConversion.put("thruDate", nowTimestamp)
        } else {
            uomConversion.put("thruDate", parameters.fromDate)
        }
    }
    delegator.storeAll(uomConversions)

    //now create a new conversion relationship
    Map createParams = dispatcher.getDispatchContext().makeValidContext("createUomConversionDated", ModelService.IN_PARAM, parameters)
    if (!parameters.fromDate) {
        createParams.put("fromDate", nowTimestamp)
    }
    result = runService("createUomConversionDated", createParams)

    return success()
}

def getFXConversion() {
    Map result = success()
    Timestamp asOfTimestamp = parameters.asOfTimestamp
    if (!asOfTimestamp) {
        asOfTimestamp = UtilDateTime.nowTimestamp()
    }

    //find the existing exchange rates
    exprBldr = new EntityConditionBuilder()
    thruDateCondition = exprBldr.OR() {
        EQUALS(thruDate: null)
        GREATER_THAN_EQUAL_TO(thruDate: asOfTimestamp)
    }
    condition = exprBldr.AND(thruDateCondition) {
        EQUALS(uomId: parameters.uomId)
        EQUALS(uomIdTo: parameters.uomIdTo)
        LESS_THAN_EQUAL_TO(fromDate: asOfTimestamp)
    }
    if (parameters.purposeEnumId) {
        condition = exprBldr.AND(condition) {
            EQUALS(purposeEnumId: parameters.purposeEnumId)
        }
    }
    List<GenericValue> rates = select().from('UomConversionDated').where(condition).orderBy("-fromDate").filterByDate().queryList()

    BigDecimal conversionRate
    int decimalScale = 2
    int roundingMode = BigDecimal.ROUND_HALF_UP
    if (rates) {
        conversionFactor = EntityUtil.getFirst(rates).getBigDecimal("conversionFactor")
        BigDecimal originalValue = BigDecimal.ONE
        conversionRate = originalValue.divide(conversionFactor, decimalScale, roundingMode)
    } else {
        String errorMessage = "Could not find conversion rate"
        logError(errorMessage)
        return error(errorMessage)
    }
    result.put("conversionRate",conversionRate)
    return result
}