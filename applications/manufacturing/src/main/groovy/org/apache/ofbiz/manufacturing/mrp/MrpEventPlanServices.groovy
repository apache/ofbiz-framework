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

import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.ws.rs.util.RestApiUtil
import org.apache.ofbiz.ws.rs.util.RestQueryOptions

/*
 * Builds reusable event-plan data from MrpEventView instead of the FindInventoryEventPlan screen query.
 *
 * Flow:
 * 1. Parse framework REST query options and identify list/detail filters.
 * 2. Query/load raw MRP event rows and reference data through MrpEventPlanQuery.
 * 3. Build planner-facing product/warehouse plan rows through MrpEventPlanBuilder.
 * 4. Apply framework-compatible sorting and paging before returning the REST response.
 */
Map findMrpEventPlans() {
    Locale serviceLocale = binding.hasVariable('locale') ? locale : Locale.ENGLISH
    // 1. Parse REST query options and identify list/detail mode.
    RestQueryOptions queryOptions
    try {
        queryOptions = RestQueryOptions.fromParameters(parameters)
    } catch (IllegalArgumentException e) {
        return ServiceUtil.returnError(e.message)
    }
    Map filters = queryOptions.filters
    String relativeRequestPath = RestApiUtil.getRelativeRequestPath(binding)
    boolean detailRequest = UtilValidate.isNotEmpty(filters.productId) && UtilValidate.isNotEmpty(filters.facilityId)
    List events
    try {
        /*
         * 2. Query/load raw MrpEventView rows. The detail flow loads one selected plan; the list flow loads candidates
         * for grouping and planner filters.
         */
        int candidateRowLimit = MrpEventPlanQuery.candidateRowLimitForPlanGrouping(queryOptions.pageIndex,
                queryOptions.pageSize)
        events = detailRequest ? MrpEventPlanQuery.loadSelectedPlanEvents(delegator, filters, candidateRowLimit, serviceLocale) :
                MrpEventPlanQuery.loadEventPlanListEvents(delegator, filters, candidateRowLimit, serviceLocale)
    } catch (IllegalArgumentException e) {
        return ServiceUtil.returnError(e.message)
    }
    if (detailRequest) {
        if (UtilValidate.isEmpty(events)) {
            return ServiceUtil.returnSuccess() + RestApiUtil.getPagedResult('eventPlans', [], queryOptions, 0L,
                    relativeRequestPath)
        }
        // 3a. Build one planner-facing detail row.
        Map eventPlanReferenceData = MrpEventPlanQuery.loadEventPlanReferenceData(delegator, events,
                [filters.facilityId])
        GenericValue firstEvent = events.first()
        Map groupContext = MrpEventPlanBuilder.buildWarehouseEventPlanContext(MrpEventPlanBuilder.eventPlanGroupKey([
                mrpId: firstEvent?.getString('mrpId') ?: filters.mrpId,
                productId: filters.productId,
                facilityId: filters.facilityId
        ]), events, eventPlanReferenceData, serviceLocale)
        Map plan = MrpEventPlanBuilder.buildEventPlanResponseRow(groupContext, serviceLocale)
        List detailPage = queryOptions.pageIndex == 0 ? [plan] : []
        // 4a. Return detail through the same paged response contract as list.
        return ServiceUtil.returnSuccess() + RestApiUtil.getPagedResult('eventPlans', detailPage, queryOptions, 1L,
                relativeRequestPath)
    }
    // 3b. Build planner-facing product/warehouse rows before computed filters and sorting.
    Map eventPlanReferenceData = MrpEventPlanQuery.loadEventPlanReferenceData(delegator, events)
    Map groupedContexts = MrpEventPlanBuilder.buildWarehouseEventPlanContexts(events, eventPlanReferenceData, serviceLocale)
    List eventPlans = groupedContexts.values()
            .findAll { Map groupContext -> UtilValidate.isNotEmpty(groupContext.facilityId) }
            .collect { Map groupContext -> MrpEventPlanBuilder.buildEventPlanResponseRow(groupContext, serviceLocale) }
            .findAll { Map plan -> MrpEventPlanBuilder.matchesEventPlanFilters(plan, filters) }
    try {
        eventPlans = RestApiUtil.sortMapRows(eventPlans, queryOptions.sort, MrpEventPlanBuilder.EVENT_PLAN_SORT_FIELDS,
                MrpEventPlanBuilder.buildEventPlanFallbackComparator())
    } catch (IllegalArgumentException e) {
        return ServiceUtil.returnError(e.message)
    }
    // 4b. Page the built rows using framework REST metadata and links.
    List pagedPlans = RestApiUtil.pageList(eventPlans, queryOptions.pageIndex, queryOptions.pageSize)
    return ServiceUtil.returnSuccess() + RestApiUtil.getPagedResult('eventPlans', pagedPlans, queryOptions,
            eventPlans.size() as long, relativeRequestPath)
}
