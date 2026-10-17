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
package org.apache.ofbiz.manufacturing.mrp

import java.sql.Timestamp
import java.time.Duration

import org.apache.ofbiz.base.util.DateRange
import org.apache.ofbiz.base.util.UtilMisc
import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.entity.util.EntityQuery
import org.apache.ofbiz.entity.util.EntityUtil
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.ws.rs.util.RestApiUtil
import org.apache.ofbiz.ws.rs.util.RestQueryOptions

/*
 * Builds paged MRP run history from durable MrpRunLog rows.
 *
 * Flow:
 * 1. Parse REST query options and translate supported sort fields.
 * 2. Build MrpRunLog filters for status, job, MRP id, and history window.
 * 3. Query/count paged MrpRunLog rows and batch-load status descriptions.
 * 4. Build response rows and return framework paging metadata.
 */
Map findMrpRuns() {
    RestQueryOptions queryOptions
    try {
        queryOptions = RestQueryOptions.fromParameters(parameters)
    } catch (IllegalArgumentException e) {
        return ServiceUtil.returnError(e.message)
    }
    Map filters = queryOptions.filters
    List orderBy
    try {
        orderBy = resolveMrpRunOrderBy(queryOptions.sort)
    } catch (IllegalArgumentException e) {
        return ServiceUtil.returnError(e.message)
    }
    List conditions = []
    if (UtilValidate.isNotEmpty(filters.statusId)) {
        conditions.add(EntityCondition.makeCondition('runStatusId', filters.statusId))
    }
    if (UtilValidate.isNotEmpty(filters.jobId)) {
        conditions.add(EntityCondition.makeCondition('jobId', filters.jobId))
    }
    if (UtilValidate.isNotEmpty(filters.mrpId)) {
        conditions.add(EntityCondition.makeCondition('mrpId', filters.mrpId))
    }
    Timestamp historyFromDate = runHistoryFromDate(filters)
    if (historyFromDate) {
        conditions.add(EntityCondition.makeCondition('startedAt', EntityOperator.GREATER_THAN_EQUAL_TO, historyFromDate))
    }

    EntityQuery query = EntityQuery.use(delegator).from('MrpRunLog')
    if (conditions) {
        query.where(EntityCondition.makeCondition(conditions, EntityOperator.AND))
    }
    long totalCount = query.queryCount()
    List rows = query.orderBy(orderBy)
            .queryPagedList(queryOptions.pageIndex, queryOptions.pageSize)
            .getData()
    Map statuses = rows ? EntityUtil.lookupById(delegator, 'StatusItem', 'statusId',
            rows*.runStatusId.findAll { it } as Set) : [:]
    List runs = rows.collect { GenericValue runLog ->
        buildMrpRunResponseRow(runLog, statuses[runLog.getString('runStatusId')])
    }
    String relativeRequestPath = RestApiUtil.getRelativeRequestPath(binding)
    return ServiceUtil.returnSuccess() + RestApiUtil.getPagedResult('runs', runs, queryOptions, totalCount,
            relativeRequestPath)
}

Timestamp runHistoryFromDate(Map filters) {
    Integer daysBack = filters.daysBack instanceof Integer ? filters.daysBack : null
    if (daysBack && daysBack > 0) {
        long millisBack = Duration.ofDays(daysBack.longValue()).toMillis()
        return new Timestamp(System.currentTimeMillis() - millisBack)
    }
    return filters.fromDate as Timestamp
}

Map buildMrpRunResponseRow(GenericValue runLog, GenericValue status) {
    Locale serviceLocale = binding.hasVariable('locale') ? locale : Locale.ENGLISH
    Timestamp startDateTime = runLog.getTimestamp('startedAt')
    Timestamp finishDateTime = runLog.getTimestamp('finishedAt')
    Long durationMillis = runLog.getLong('durationMillis')
    if (durationMillis == null && startDateTime != null && finishDateTime != null) {
        durationMillis = new DateRange(startDateTime, finishDateTime).durationInMillis()
    }
    [
            runId: runLog.getString('mrpRunLogId'),
            mrpRunLogId: runLog.getString('mrpRunLogId'),
            jobId: runLog.getString('jobId'),
            mrpId: runLog.getString('mrpId'),
            mrpName: runLog.getString('mrpName') ?: UtilProperties.getMessage('ManufacturingUiLabels',
                    'ManufacturingMrpRunDefaultName', serviceLocale),
            facilityGroupId: runLog.getString('facilityGroupId'),
            facilityId: runLog.getString('facilityId'),
            defaultYearsOffset: UtilMisc.toIntegerObject(runLog.get('defaultYearsOffset')) ?: 1,
            statusId: runLog.getString('runStatusId'),
            statusDescription: status?.getString('description'),
            failureReason: runLog.getString('failureReason'),
            failureMessage: runLog.getString('failureMessage'),
            runTime: durationMillis != null ? Duration.ofMillis(durationMillis).toString() : null,
            durationMillis: durationMillis,
            startDateTime: startDateTime,
            finishDateTime: finishDateTime,
            runAsUser: runLog.getString('runByUserLoginId')
    ]
}

List resolveMrpRunOrderBy(String sortExpression) {
    return RestApiUtil.resolveOrderBy(sortExpression, [
            runId: 'mrpRunLogId',
            mrpRunLogId: 'mrpRunLogId',
            mrpId: 'mrpId',
            mrpName: 'mrpName',
            facilityId: 'facilityId',
            statusId: 'runStatusId',
            startDateTime: 'startedAt',
            finishDateTime: 'finishedAt',
            durationMillis: 'durationMillis',
            jobId: 'jobId'
    ], ['-startedAt', '-createdStamp'])
}
