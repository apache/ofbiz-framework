package org.apache.ofbiz.accounting.fixedasset

import java.math.RoundingMode
import java.sql.Timestamp

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityConditionBuilder

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

/**
 * create a FixedAssetMaint
 */
Map createFixedAssetMaint() {
    GenericValue newEntity = makeValue('FixedAssetMaint', parameters)
    String maintTemplateWorkEffortId
    newEntity.maintHistSeqId = delegator.getNextSeqId('FixedAssetMaint')
    GenericValue fixedAsset = from('FixedAsset').where(parameters).queryOne()
    if (parameters.productMaintSeqId) {
        GenericValue productMaint = from('ProductMaint')
                .where(productId: fixedAsset.instanceOfProductId,
                        productMaintSeqId: parameters.productMaintSeqId)
                .queryOne()
        newEntity.productMaintTypeId = productMaint.productMaintTypeId
        maintTemplateWorkEffortId = productMaint.maintTemplateWorkEffortId ?: parameters.maintTemplateWorkEffortId
    }
    if (maintTemplateWorkEffortId) {
        String workEffortId = delegator.getNextSeqId('WorkEffort')
        run service: 'duplicateWorkEffort', with:
                [oldWorkEffortId: maintTemplateWorkEffortId,
                 workEffortId: workEffortId,
                 duplicateWorkEffortAssocs: 'Y',
                 duplicateWorkEffortNotes: 'Y',
                 duplicateWorkEffortContents: 'Y',
                 duplicateWorkEffortAssignmentRates: 'Y']
        newEntity.scheduleWorkEffortId = workEffortId
    } else {
        // Create the WorkEffort and Maintenance WorkEffort entity
        Map maintWorkEffortMap = [workEffortTypeId: 'TASK',
                                  workEffortName: label('AccountingUiLabels', 'AccountingFixedAssetMaintWorkEffortName'),
                                  workEffortPurposeTypeId: 'WEPT_MAINTENANCE',
                                  currentStatusId: 'CAL_TENTATIVE',
                                  quickAssignPartyId: userLogin.partyId,
                                  fixedAssetId: fixedAsset.fixedAssetId,
                                  estimatedStartDate: parameters.estimatedStartDate,
                                  estimatedCompletionDate: parameters.estimatedCompletionDate]
        maintWorkEffortMap.description = delegator.getRelatedOne('ProductMaintType', newEntity, true)?.description
        Map serviceResult = run service: 'createWorkEffort', with: maintWorkEffortMap
        newEntity.scheduleWorkEffortId = serviceResult.workEffortId
    }
    newEntity.create()
    run service: 'autoAssignFixedAssetPartiesToMaintenance', with: [fixedAssetId: fixedAsset.fixedAssetId,
                                                                    workEffortId: newEntity.scheduleWorkEffortId]
    return success([maintHistSeqId: newEntity.maintHistSeqId])
}

/**
 * update an existing FixedAsset Maintenance
 */
Map updateFixedAssetMaint() {
    GenericValue lookedUpValue = from('FixedAssetMaint').where(parameters).queryOne()
    String oldStatusId = lookedUpValue.statusId
    lookedUpValue.setNonPKFields(parameters)
    GenericValue fixedAsset = from('FixedAsset').where(parameters).cache().queryOne()
    if (parameters.productMaintSeqId) {
        GenericValue productMaint = from('ProductMaint')
                .where(productId: fixedAsset.instanceOfProductId,
                        productMaintSeqId: parameters.productMaintSeqId)
                .queryOne()
        lookedUpValue.productMaintTypeId = productMaint.productMaintTypeId
    }
    lookedUpValue.store()
    if (lookedUpValue.statusId == 'FAM_COMPLETED'
            && oldStatusId != lookedUpValue.statusId) {
        GenericValue workEffort = from('WorkEffort')
                .where(workEffortId: lookedUpValue.scheduleWorkEffortId)
                .cache()
                .queryOne()
        if (workEffort && !workEffort.actualCompletionDate && workEffort.currentStatusId != 'CAL_COMPLETED') {
            Timestamp nowTimestamp = UtilDateTime.nowTimestamp()
            run service: 'updateWorkEffort', with: [workEffortId: workEffort.workEffortId,
                                                    currentStatusId: 'CAL_ACCEPTED']
            run service: 'updateWorkEffort', with: [workEffortId: workEffort.workEffortId,
                                                    actualCompletionDate: nowTimestamp,
                                                    currentStatusId: 'CAL_COMPLETED']
            delegator.storeByCondition('WorkEffortPartyAssignment',
                    [thruDate: nowTimestamp],
                    EntityCondition.makeCondition([
                            EntityCondition.makeCondition('workEffortId', workEffort.workEffortId),
                            EntityCondition.makeConditionDate('fromDate', 'thruDate')]))
        }
    }
    return success(oldStatusId: oldStatusId)
}

/**
 * Create Fixed Asset Maintenances From A Meter Reading
 */
Map createMaintsFromMeterReading() {
    if (parameters.maintHistSeqId) {
        return success()
    }
    GenericValue fixedAsset = from('FixedAsset').where(parameters).cache().queryOne()
    if (!fixedAsset.instanceOfProductId) {
        return success()
    }
    from('ProductMaint')
            .where(productId: fixedAsset.instanceOfProductId,
                    intervalMeterTypeId: parameters.productMeterTypeId)
            .queryList()
            .each { p ->
                long repeatCount = p.repeatCount ?: 0L
                EntityCondition cond = new EntityConditionBuilder().AND {
                    EQUALS(fixedAssetId: fixedAsset.fixedAssetId)
                    EQUALS(intervalMeterTypeId: p.intervalMeterTypeId)
                    EQUALS(productMaintTypeId: p.productMaintTypeId)
                    NOT_EQUAL(statusId: 'FAM_CANCELLED')
                }
                List maintList = from('FixedAssetMaint')
                        .where(cond)
                        .queryList()
                long listSize = maintList ? maintList.size() : 0L

                BigDecimal maxIntervalQty = maintList ? maintList*.intervalQuantity.max() : 0

                BigDecimal nextIntervalQty = maxIntervalQty + p.intervalQuantity
                if (parameters.meterValue &&
                        (nextIntervalQty > parameters.meterValue ||
                            (repeatCount > 0 && listSize < repeatCount))) {
                    run service: 'createFixedAssetMaint', with: [*  : p.getAllFields(),
                                                                 fixedAssetId: fixedAsset.fixedAssetId,
                                                                 intervalQuantity: parameters.meterValue,
                                                                 statusId: 'FAM_CREATED']
                }
            }
    return success()
}

/**
 * Create Fixed Asset Maintenances From A Product Maint Time Interval
 */
Map createMaintsFromTimeInterval() {
    Timestamp nowTimestamp = UtilDateTime.nowTimestamp()
    EntityCondition cond = new EntityConditionBuilder().AND {
        NOT_EQUAL(instanceOfProductId: null)
        EQUALS(actualEndOfLife: null)
    }
    Map timeSwitch = [TF_day: Calendar.DAY_OF_MONTH,
                      TF_mon: Calendar.MONTH,
                      TF_yr: Calendar.YEAR]
    from('FixedAsset')
    .where(cond)
    .queryList()
    .each { f ->
        cond = new EntityConditionBuilder().AND {
                EQUALS(productId: f.instanceOfProductId)
                LIKE(intervalUomId: 'TF_%')
        }
        from('ProductMaint')
                .where(cond)
                .queryList()
                .each { p ->
                    if (timeSwitch.containsKey(p.intervalUomId)) {
                        long repeatCount = p.repeatCount ?: 0
                        long intervalQuantity = p.intervalQuantity ?: 0
                        Calendar calendar = Calendar.instance()
                        calendar.setTime(nowTimestamp)
                        calendar.add(timeSwitch[p.intervalUomId], -intervalQuantity)
                        cond = new EntityConditionBuilder().AND {
                            EQUALS(fixedAssetId: f.fixedAssetId)
                            EQUALS(intervalUomId: p.intervalUomId)
                            EQUALS(productMaintTypeId: p.productMaintTypeId)
                            NOT_EQUAL(statusId: 'FAM_CANCELLED')
                        }
                        List<GenericValue> maintList = from('FixedAssetMaintWorkEffort')
                                .where(cond)
                                .orderBy('maintHistSeqId')
                                .queryList()
                        long listSize = maintList ? maintList.size() : 0
                        Timestamp lastSvcDate = maintList ? maintList.last().actualCompletionDate : null
                        if (lastSvcDate && lastSvcDate.before(calendar.getTime())
                                && (repeatCount <= 0 || listSize < repeatCount)) {
                            run service: 'createFixedAssetMaint', with: [*: p.getAllFields(),
                                                                         fixedAssetId: f.fixedAssetId,
                                                                         statusId: 'FAM_CREATED']
                        }
                    }
                }
    }
    return success()
}

/**
 * Create a FixedAsset Maintenance Order
 */
Map createFixedAssetMaintOrder() {
    GenericValue lookedUpValue = from('OrderHeader').where(parameters).queryOne()
    if (!lookedUpValue) {
        return error(label('AccountingUiLabels', 'AccountingOrderWithIdNotFound', parameters))
    }

    // Check if user has not passed in orderItemSeqId then get list of OrderItems from database and default to first item
    if (parameters.orderItemSeqId) {
        lookedUpValue = from('OrderItem').where(parameters).queryOne()
        if (!lookedUpValue) {
            return error(label('AccountingUiLabels', 'AccountingOrderItemWithIdNotFound', parameters))
        }
    } else {
        parameters.orderItemSeqId = from('OrderItem').where(orderId: lookedUpValue.orderId).queryList()?.orderItemSeqId
    }
    delegator.create('FixedAssetMaintOrder', parameters)
    return success()
}

/**
 * Auto-assign Fixed Asset Parties to a Fixed Asset Maintenance
 */
Map autoAssignFixedAssetPartiesToMaintenance() {
    from('PartyFixedAssetAssignAndRole')
            .where(fixedAssetId: parameters.fixedAssetId,
                    parentTypeId: 'FAM_ASSIGNEE')
            .filterByDate()
            .queryList()
    .each {
        run service: 'assignPartyToWorkEffort', with: [partyId: it.partyId,
                                                       roleTypeId: it.roleTypeId,
                                                       workEffortId: parameters.workEffortId,
                                                       statusId: 'PRTYASGN_ASSIGNED']
    }
    return success()
}

/**
 * Calculate straight line depreciation to Fixed Asset[ (PC-SV)/expLife ]
 */
Map straightLineDepreciation() {
    BigDecimal depreciationTotal = 0
    List assetDepreciationInfoList = []
    List assetDepreciationTillDate = []
    List assetNBVAfterDepreciation = []
    BigDecimal purchaseCost = parameters.purchaseCost
    BigDecimal expEndOfLifeYear = parameters.expEndOfLifeYear ?: 0
    BigDecimal assetAcquiredYear = parameters.assetAcquiredYear ?: 0

    GenericValue fixedAsset = from('FixedAsset').where(parameters).queryOne()
    if (!fixedAsset) {
        return error(label('AccountingErrorUiLabels', 'AccountingFixedAssetNotFound'))
    }
    BigDecimal depreciation = fixedAsset.depreciation ?: 0
    if (parameters.usageYears > 0) {
        //FORMULA :  depreciation = (purchaseCost - salvageValue) / (expectedEndOfLife - dateAcquired)
        int numberOfYears = parameters.expEndOfLifeYear - parameters.assetAcquiredYear
        if (numberOfYears > 0) {
            depreciation = (purchaseCost - parameters.salvageValue) / numberOfYears
            depreciation.setScale(2, RoundingMode.HALF_EVEN)
            int intUsageYears =  (numberOfYears < parameters.intUsageYears) ? parameters.intUsageYears : numberOfYears
            for (int i = 0; i < intUsageYears; i++) {
                purchaseCost -= depreciation
                depreciationTotal += depreciation
                assetDepreciationTillDate << depreciation
                assetNBVAfterDepreciation << purchaseCost
                assetDepreciationInfoList << [year: i,
                    depreciation: depreciation,
                    depreciationTotal: depreciationTotal,
                    nbv: purchaseCost]
            }
        }
    }

    if (!assetDepreciationTillDate) {
        assetDepreciationTillDate << depreciation
        assetNBVAfterDepreciation << purchaseCost
        assetDepreciationInfoList << [year: parameters.assetAcquiredYear,
            depreciation: depreciation,
            depreciationTotal: depreciationTotal,
            nbv: purchaseCost]
    }
    logInfo "Using straight line formula depreciation calculated for fixedAsset (${parameters.fixedAssetId}) is ${depreciation}"

    // Next depreciation based on actual depreciation history
    BigDecimal nextDepreciationAmount = 0

    // FORMULA : depreciation = (purchaseCost - salvageValue - pastDepreciations) / remainingYears
    int usageYears = parameters.intUsageYears ?: 0
    BigDecimal remainingYears  = expEndOfLifeYear - assetAcquiredYear - usageYears
    if (remainingYears > 0) {
        nextDepreciationAmount = ((fixedAsset.purchaseCost ?: 0) - usageYears - (fixedAsset.depreciation ?: 0)) / remainingYears
        nextDepreciationAmount.setScale(2, RoundingMode.HALF_EVEN)
    }
    return success([assetDepreciationTillDate: assetDepreciationTillDate,
        assetNBVAfterDepreciation: assetNBVAfterDepreciation,
        assetDepreciationInfoList: assetDepreciationInfoList,
        nextDepreciationAmount: nextDepreciationAmount,
        plannedPastDepreciationTotal: depreciationTotal - (fixedAsset.depreciation ?: 0)])
}

/**
 * Calculate double declining balance depreciation to Fixed Asset
 */
Map doubleDecliningBalanceDepreciation() {
    BigDecimal expEndOfLifeYear = parameters.expEndOfLifeYear ?: 0
    BigDecimal assetAcquiredYear = parameters.assetAcquiredYear ?: 0
    int usageYears = parameters.usageYears ?: 0
    BigDecimal purchaseCost = parameters.purchaseCost ?: 0
    BigDecimal salvageValue = parameters.salvageValue ?: 0
    BigDecimal depreciation = 0

    // Next depreciation based on actual depreciation history
    BigDecimal nextDepreciationAmount = 0
    GenericValue fixedAsset = from('FixedAsset').where(parameters).queryOne()
    if (fixedAsset) {
        depreciation = fixedAsset.depreciation ?: 0
        BigDecimal remainingYears  = expEndOfLifeYear - assetAcquiredYear - usageYears
        if (remainingYears > 0) {
            nextDepreciationAmount = 2 * (purchaseCost - salvageValue - depreciation) / remainingYears
        }
    }

    List assetDepreciationTillDate = []
    List assetNBVAfterDepreciation = []
    List assetDepreciationInfoList = []
    BigDecimal depreciationTotal = 0
    if (usageYears > 0 && fixedAsset) {
        BigDecimal depreciationYear = assetAcquiredYear
        for (int i = 0; i < usageYears; i++) {
            int numberOfYears = expEndOfLifeYear - assetAcquiredYear
            if (numberOfYears > 0) {
                depreciation = (purchaseCost - salvageValue) * 2 / numberOfYears
            }
            assetAcquiredYear++
            purchaseCost -= depreciation
            depreciationTotal += depreciation
            assetDepreciationTillDate << depreciation
            assetNBVAfterDepreciation << purchaseCost
            assetDepreciationInfoList << [nbv: purchaseCost,
                                          year: depreciationYear,
                                          depreciation: depreciation,
                                          depreciationTotal: depreciationTotal]
        }

        if (!assetDepreciationTillDate) {
            assetDepreciationTillDate << 0
            assetNBVAfterDepreciation << purchaseCost
            assetDepreciationInfoList << [nbv: purchaseCost,
                                          year: assetAcquiredYear,
                                          depreciation: 0,
                                          depreciationTotal: depreciationTotal]
        }
    }
    logInfo "Using double decline formula depreciation calculated for fixedAsset (${parameters.fixedAssetId}) is ${assetDepreciationTillDate}"
    return success([assetDepreciationTillDate: assetDepreciationTillDate,
                    assetNBVAfterDepreciation: assetNBVAfterDepreciation,
                    assetDepreciationInfoList: assetDepreciationInfoList,
                    nextDepreciationAmount: nextDepreciationAmount,
                    plannedPastDepreciationTotal: depreciationTotal - depreciation])
}

/**
 * Service to calculate the yearly depreciation from dateAcquired year to current financial year
 */
Map calculateFixedAssetDepreciation() {
    GenericValue fixedAsset = from('FixedAsset').where(parameters).queryOne()
    if (!fixedAsset) {
        return error(label('ManufacturingUiLabels', 'ManufacturingFixedAssetNotExist'))
    }
    String expEndOfLifeYear, assetAcquiredYear

    // Extract asset end of life year from field expectedEndOfLife
    if (fixedAsset.expectedEndOfLife) {
        expEndOfLifeYear = fixedAsset.expectedEndOfLife.toString().substring(0, 4)
    } else {
        return success(label('AccountingUiLabels', 'AccountingExpEndOfLifeIsEmpty'))
    }

    // Extract asset acquired year from field dateAcquired
    if (fixedAsset.expectedEndOfLife) {
        assetAcquiredYear = fixedAsset.dateAcquired.toString().substring(0, 4)
    } else {
        return success(label('AccountingUiLabels', 'AccountingDateAcquiredIsEmpty'))
    }

    // if any asset's salvage value is empty then set it by 0
    BigDecimal salvageValue = fixedAsset.salvageValue ?: 0.0

    // Get running year
    String currentYear = UtilDateTime.nowDateString().substring(0, 4)

    // Calculate asset's total run in years
    int usageYears = currentYear.toInteger() - assetAcquiredYear.toInteger()

    GenericValue fixedAssetDepMethod = from('FixedAssetDepMethod')
            .where(fixedAssetId: parameters.fixedAssetId)
            .filterByDate()
            .queryFirst()
    if (fixedAssetDepMethod) {
        GenericValue customMethod = fixedAssetDepMethod.getRelatedOne('CustomMethod', true)
        logInfo "Depreciation service name for the FixedAsset ${parameters.fixedAssetId} is ${customMethod.customMethodName}"
        Map serviceResult = run service: customMethod.customMethodName, with: [
                fixedAssetId: parameters.fixedAssetId,
                expEndOfLifeYear: expEndOfLifeYear,
                assetAcquiredYear: assetAcquiredYear,
                purchaseCost: fixedAsset.purchaseCost,
                salvageValue: salvageValue,
                usageYears: usageYears]
        logInfo "Asset's depreciation calculated till date are ${serviceResult.assetDepreciationTillDate}"
        logInfo "Asset's Net Book Values (NBV) from acquired date after deducting depreciation are ${serviceResult.assetNBVAfterDepreciation}"
        return serviceResult
    }
    return error(label('AccountingUiLabels', 'AccountingFixedAssetDepreciationMethodNotFound'))
}

/**
 * If the accounting transaction is a depreciation transaction for a fixed asset, update the depreciation amount in the FixedAsset entity.
 */
Map checkUpdateFixedAssetDepreciation() {
    GenericValue acctgTrans = from('AcctgTrans').where(parameters).queryOne()
    if (!acctgTrans &&
            acctgTrans.acctgTransTypeId == 'DEPRECIATION'
            && acctgTrans.fixedAssetId) {
        GenericValue fixedAsset = acctgTrans.getRelatedOne('FixedAsset', true)
        BigDecimal depreciation = 0
        boolean nonValidUom = false
        from('AcctgTransEntry')
                .where(debitCreditFlag: 'C',
                        acctgTransId: acctgTrans.acctgTransId)
                .queryList()
                .each {
                    if (!fixedAsset.purchaseCostUomId) {
                        logWarning "Found empty purchaseCostUomId for FixedAsset [${fixedAsset.fixedAssetId}]:" +
                                " setting it to ${creditTransaction.currencyUomId} to match the one used in the gl."
                        fixedAsset.purchaseCostUomId = it.currencyUomId
                        fixedAsset.store()
                    }
                    if (fixedAsset.purchaseCostUomId == it.currencyUomId) {
                        depreciation += it.amount
                    } else {
                        nonValidUom = true
                    }
                }
        if (nonValidUom) {
            return failure("Found an accounting transaction for depreciation of FixedAsset [${fixedAsset.fixedAssetId}] with a currency that " +
                    "doesn't match the currency used in the fixed asset: the depreciation total in the fixed asset will not be updated.")
        }

        fixedAsset.depreciation = fixedAsset.depreciation ?: 0
        fixedAsset.depreciation += depreciation
        fixedAsset.store()
    }
    return success()
}
