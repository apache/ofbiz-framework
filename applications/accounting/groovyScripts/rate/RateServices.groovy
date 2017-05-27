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
import org.apache.ofbiz.base.util.UtilValidate
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
    if (!newEntity.fromDate) newEntity.fromDate = UtilDateTime.getDayStart(UtilDateTime.nowTimestamp())
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
            Map deleteRateAmountMap = dispatcher.getDispatchContext().makeValidContext('deleteRateAmount', 'IN', rateAmountLookedUpValue)
            result = run service: 'deleteRateAmount', with: deleteRateAmountMap
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
def deleteRateAmount() {
    GenericValue lookedUpValue = delegator.makeValidValue('RateAmount', parameters)
    if (!lookedUpValue.rateCurrencyUomId) {
        lookedUpValue.rateCurrencyUomId = UtilProperties.getPropertyValue('general.properties', 'currency.uom.id.default')
    }
    lookedUpValue = from('RateAmount').where(lookedUpValue.getFields(lookedUpValue.getModelEntity().getPkFieldNames())).queryOne()
    if (lookedUpValue) {
        Timestamp previousDay = UtilDateTime.adjustTimestamp(UtilDateTime.nowTimestamp(), 5, -1)
        lookedUpValue.thruDate = UtilDateTime.getDayEnd(previousDay)
        lookedUpValue.store()
    } else {
        return error(UtilProperties.getMessage('AccountingErrorUiLabels', 'AccountingDeleteRateAmount', locale))
    }
    return success()
}

def updatePartyRate() {
    List<GenericValue> partyRates = from('PartyRate').where([partyId: partyId, rateTypeId: rateTypeId]).queryList()
    if (UtilValidate.isNotEmpty(partyRates)) {
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
        Map createRateAmountMap = dispatcher.getDispatchContext().makeValidContext('updateRateAmount', 'IN', parameters)
        result = run service: 'updateRateAmount', with: createRateAmountMap
        if (ServiceUtil.isError(result)) return result
    }
    return success()
}

def deletePartyRate() {
    GenericValue lookedUpValue = from('PartyRate').where([partyId: partyId, rateTypeId: rateTypeId, fromDate: fromDate]).queryOne()
    if (lookedUpValue) {
        lookedUpValue.thruDate = UtilDateTime.nowTimestamp()
        lookedUpValue.store()

        //expire related rate amount
        Map deleteRateAmountMap = dispatcher.getDispatchContext().makeValidContext('deleteRateAmount', 'IN', parameters)
        result = run service: 'deleteRateAmount', with: deleteRateAmountMap
        if (ServiceUtil.isError(result)) return result
    }
    return success()
}
