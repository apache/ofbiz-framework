/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * 'License'); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * 'AS IS' BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.util.EntityUtil
import org.apache.ofbiz.service.ServiceUtil

import java.sql.Timestamp


/**
 * Service to create a rate amount value, if a existing value is present expire it before
 */
def updateRateAmount() {
    GenericValue newEntity = delegator.makeValidValue('RateAmount', parameters)
    if (!newEntity.rateCurrencyUomId) {
        newEntity.rateCurrencyUomId = UtilProperties.getPropertyValue('general.properties', 'currency.uom.id.default')
    }
    if (!newEntity.fromDate) newEntity.fromDate = UtilDateTime.nowTimestamp()
    newEntity.thruDate = null

    //Check if the entry is already exist with a different rate else expire the older to create the new one
    boolean updating = false
    GenericValue rateAmountLookedUpValue = from('RateAmount').where('rateTypeId', newEntity.rateTypeId,
            'emplPositionTypeId', newEntity.emplPositionTypeId,
            'rateCurrencyUomId', newEntity.rateCurrencyUomId,
            'workEffortId', newEntity.workEffortId,
            'periodTypeId', newEntity.periodTypeId,
            'partyId', newEntity.partyId).filterByDate().queryFirst()
    if (rateAmountLookedUpValue) {
        updating = (rateAmountLookedUpValue.fromDate.compareTo(newEntity.fromDate) == 0)
        if (rateAmountLookedUpValue.rateAmount != rateAmount) {
            result = run service: 'expireRateAmount', with: rateAmountLookedUpValue.getAllFields()
            if (ServiceUtil.isError(result)) return result
        } else {
            return error(UtilProperties.getMessage('AccountingErrorUiLabels', 'AccountingUpdateRateAmountAlreadyExist', locale))
        }
    }
    if (updating) newEntity.store()
    else newEntity.create()
    return success()
}

/**
 * Service to expire a rate amount value
 */
def expireRateAmount() {
    GenericValue lookedUpValue = delegator.makeValidValue('RateAmount', parameters)
    if (!lookedUpValue.rateCurrencyUomId) {
        lookedUpValue.rateCurrencyUomId = UtilProperties.getPropertyValue('general.properties', 'currency.uom.id.default')
    }
    lookedUpValue = from('RateAmount').where(lookedUpValue).queryOne()
    if (lookedUpValue) {
        Timestamp previousDay = UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), 5, -1)
        lookedUpValue.thruDate = UtilDateTime.getDayEnd(previousDay)
        lookedUpValue.store()
    } else {
        return error(UtilProperties.getMessage('AccountingErrorUiLabels', 'AccountingDeleteRateAmount', locale))
    }
    return success()
}
/**
 * Information to update the specific customer code after change service deleteRateAmount to expireRateAmount
 * @return
 */
def deleteRateAmount() {
    return error('delete rate amount isn\'t possible, please update your code with service name "expireRateAmount" instead "deleteRateAmount"')
}

def updatePartyRate() {
    List<GenericValue> partyRates = from('PartyRate').where([partyId: partyId, rateTypeId: rateTypeId]).queryList()
    if (partyRates) {
        GenericValue partyRate = EntityUtil.getFirst(partyRates)
        partyRate.thruDate = UtilDateTime.nowTimestamp()
    }
    GenericValue newEntity = delegator.makeValidValue('PartyRate', parameters)
    if (!newEntity.fromDate) newEntity.fromDate = UtilDateTime.nowTimestamp()
    newEntity.create()

    //check other default rate to desactive them
    if ('Y' == newEntity.defaultRate) {
        partyRates = from('PartyRate').where([partyId: partyId, defaultRate: 'Y']).queryList()
        partyRates.each { partyDefaultRate ->
            partyDefaultRate.defaultRate = 'N'
            partyDefaultRate.store()
        }
    }
    if (parameters.rateAmount) {
        result = run service: 'updateRateAmount', with: parameters
        if (ServiceUtil.isError(result)) return result
    }
    return success()
}
def deletePartyRate() {
    return error('delete party rate isn\'t possible, please update your code with service name "expirePartyRate" instead "deletePartyRate"')
}
def expirePartyRate() {
    GenericValue lookedUpValue = from('PartyRate').where(parameters).queryOne()
    if (lookedUpValue) {
        lookedUpValue.thruDate = UtilDateTime.nowTimestamp()
        lookedUpValue.store()

        //expire related rate amount
        if (parameters.rateAmountFromDate) {
        parameters.fromDate = parameters.rateAmountFromDate;
        result = run service: 'expireRateAmount', with: parameters
        if (ServiceUtil.isError(result)) return result
        }
    }
    return success()
}

// Get the applicable rate amount value
def getRateAmount() {
    /* Search for the applicable rate from most specific to most general in the RateAmount entity
    Defaults for periodTypeId is per hour and default currency is the currency in general.properties
    The order is:
    1. for specific rateTypeId, workEffortId (workEffort)
    2. for specific rateTypeId, partyId (party)
    3. for specific rateTypeId, emplPositionTypeId (emplPositionType)
    4. for specific rateTypeId (rateType)

    Then, the results are filtered to improve the result. If you pass a workEffortId and a partyId,
    the service will first search the list of all the rateAmount with the specified workEffortId. Then, if
    there is at least one rateAmount with same partyId than the one in the parameter in the list, the list will
    be reduced to those entries.
    At the end, the first record of the list is chosen.

    For a easier debugging time, there is a log triggered when no records are found for the input. This log
    shows up when there are rateAmounts corresponding to the input parameters without the rateCurrencyUomId and
    the periodTypeId.*/
    String serviceName = null
    String level = "rateTypeId"
    if (parameters.workEffortId && parameters.workEffortId != '_NA_') {
        // workeffort level
        level = 'workEffortId'
        serviceName = 'getRatesAmountsFromWorkEffortId'
    } else if (parameters.partyId && parameters.partyId != '_NA_') {
        // party level
        level='partyId'
        serviceName = 'getRatesAmountsFromPartyId'
    } else if (parameters.emplPositionTypeId && parameters.emplPositionTypeId != '_NA_') {
        // party level
        level = 'emplPositionTypeId'
        serviceName = 'getRatesAmountsFromEmplPositionTypeId'
    }
    if (serviceName) {
        Map serviceContextMap = new HashMap<>(parameters)
        if (!serviceContextMap.rateCurrencyUomId) {
            serviceContextMap.rateCurrencyUomId = UtilProperties.getPropertyValue('general.properties', 'currency.uom.id.default', 'USD')
        }
        Map result = run service: serviceName, with: serviceContextMap
        serviceContextMap.ratesList = result.ratesList
        result = run service: 'filterRateAmountList', with: serviceContextMap
        ratesList = result.filteredRatesList
    }

    if (!ratesList) {
        ratesList = from('RateAmount').where([rateTypeId: parameters.rateTypeId]).queryList();
        Map serviceContextMap = new HashMap<>(parameters)
        serviceContextMap.ratesList = ratesList
        Map result = run service: 'filterRateAmountList', with: serviceContextMap
        ratesList = EntityUtil.filterByDate(result.filteredRatesList)
    }

    if (!ratesList) {
        rateType = from('RateAmount').where(parameters).queryOne()
        logError('A valid rate amount could not be found for rateType: ' + rateType?.description)
    }

    // We narrowed as much as we could the result, now returning the first record of the list
    Map result = success()
    if (ratesList) {
        rateAmount = ratesList[0]
        if (! rateAmount.rateAmount) rateAmount.rateAmount = BigDecimal.ZERO
        result.rateAmount = rateAmount.rateAmount
        result.periodTypeId = rateAmount.periodTypeId
        result.rateCurrencyUomId = rateAmount.rateCurrencyUomId
        result.level = level
        result.fromDate = rateAmount.fromDate
    }
    return result
}

//Generic fonction to resolve a rate amount from a pk field
def getRatesAmountsFrom(String field) {
    String entityName = null
    if (field == 'workEffortId') entityName = 'WorkEffort'
    if (field == 'partyId') entityName = 'Party'
    if (field == 'emplPositionTypeId') entityName = 'EmplPositionType'

    Map condition = [rateTypeId: parameters.rateTypeId,
                     periodTypeId: parameters.periodTypeId,
                     rateCurrencyUomId: parameters.rateCurrencyUomId]
    condition.put(field, parameters.get(field))
    List ratesList = from('RateAmount').where(condition).filterByDate().queryList()
    if (!ratesList) {
        GenericValue periodType = from('PeriodType').where(parameters).queryOne()
        GenericValue rateType = from('RateType').where(parameters).queryOne()
        GenericValue partyNameView = from('PartyNameView').where(parameters).queryOne()
        logError('A valid rate entry could be found for rateType:' + rateType.description + ', ' + entityName + ':' + parameters.get(field)
                + ', party: ' + partyNameView.lastName + partyNameView.middleName + partyNameView.firstName + partyNameView.groupName
                + ' However.....not for the period:' + periodType.description + ' and currency:' + parameters.rateCurrencyUomId)
    }
    Map result = success()
    result.ratesList = ratesList
    return result
}
// Get all the rateAmount for a given workEffortId
def getRatesAmountsFromWorkEffortId() {
    return getRatesAmountsFrom('workEffortId')
}
// Get all the rateAmount for a given partyId
def getRatesAmountsFromPartyId() {
    return getRatesAmountsFrom('partyId')
}
// Get all the rateAmount for a given emplPositionTypeId
def getRatesAmountsFromEmplPositionTypeId() {
    return getRatesAmountsFrom('emplPositionTypeId')
}

//Filter a list of rateAmount. The result is the most heavily-filtered non-empty list
def filterRateAmountList() {
    if (!parameters.ratesList) {
        logWarning('The list parameters.ratesList was empty, not processing any further')
        return success()
    }
    //Check if there is a more specific rate
    Map filterMap = [:]
    if (parameters.workEffortId) {
        filterMap.workEffortId = parameters.workEffortId
    }
    if (parameters.partyId) {
        filterMap.partyId = parameters.partyId
    }
    if (parameters.emplPositionTypeId) {
        filterMap.emplPositionTypeId = parameters.emplPositionTypeId
    }
    if (parameters.rateTypeId) {
        filterMap.rateTypeId = parameters.rateTypeId
    }
    List tempRatesFilteredList = EntityUtil.filterByAnd(parameters.ratesList, filterMap)
    List ratesList = []
    if (tempRatesFilteredList) {
        ratesList = tempRatesFilteredList
    }
    Map result = success()
    result.filteredRatesList = ratesList
    return result
}