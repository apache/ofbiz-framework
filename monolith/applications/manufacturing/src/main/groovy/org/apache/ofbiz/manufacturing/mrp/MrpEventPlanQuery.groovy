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
import java.time.LocalDate
import java.util.regex.Pattern

import org.apache.ofbiz.base.util.ObjectType
import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.entity.Delegator
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.entity.util.EntityQuery
import org.apache.ofbiz.entity.util.EntityUtil

final class MrpEventPlanQuery {

    private static final Pattern DATE_ONLY_PATTERN = ~/\d{4}-\d{2}-\d{2}/
    private static final String RESOURCE = 'ManufacturingUiLabels'
    private static final int MAX_MRP_EVENT_VIEW_ROWS = 5000

    private MrpEventPlanQuery() { }

    /*
     * Calculates how many raw MrpEventView rows can be loaded before grouping them into product/warehouse plans.
     * The service may need more raw rows than the requested page size because multiple ledger rows collapse into one
     * planner-facing row.
     *
     * Example: a single plan for GZ-8544 at WebStoreWarehouse may need INITIAL_QOH, SALES_ORDER, and PROP_PUR_O_RECP
     * rows to calculate one final running balance. Loading only pageSize raw rows could therefore return too few plans
     * after grouping. This method over-fetches enough raw rows for the requested page window, but caps the query so one
     * request does not load too many MRP event rows.
     *
     * The 500-row floor protects very small page sizes. For example, pageSize 1 would otherwise load only 100 raw rows,
     * which may not be enough after raw event rows collapse into grouped plan rows.
     *
     * Input example:
     * pageIndex = 2
     * pageSize = 10
     *
     * Output example:
     * 3000
     */
    static int candidateRowLimitForPlanGrouping(int pageIndex, int pageSize) {
        int pageWindow = Math.max(1, pageIndex + 1)
        return Math.max(500, Math.min(pageWindow * pageSize * 100, MAX_MRP_EVENT_VIEW_ROWS)) as int
    }

    /*
     * Detail loader for one selected product/facility plan. Unlike the list loader, this narrows the raw event stream
     * to the selected warehouse plus product-level context rows so the detail timeline can explain that one plan.
     *
     * Product-level rows, such as organization-level forecasts, are retained so the detail view can explain why
     * warehouse planning changed.
     *
     * Input example:
     * filters = [mrpId: '10022', productId: 'GZ-8544', facilityId: 'WebStoreWarehouse']
     *
     * Output example:
     * [
     *   MrpEventView[mrpId: '10022', productId: 'GZ-8544', facilityId: 'WebStoreWarehouse'],
     *   MrpEventView[mrpId: '10022', productId: 'GZ-8544', facilityId: null]
     * ]
     */
    static List loadSelectedPlanEvents(Delegator delegator, Map filters, int candidateLimit, Locale locale = Locale.ENGLISH) {
        Map eventFilters = filters.findAll { String key, Object value ->
            UtilValidate.isNotEmpty(value)
        }
        List events = loadMrpEventViewRows(delegator, eventFilters, candidateLimit, true, locale)
        return events.findAll { GenericValue event ->
            String facilityId = event.getString('facilityId')
            facilityId == filters.facilityId || UtilValidate.isEmpty(facilityId)
        }
    }

    /*
     * List loader for many product/facility plans. Unlike the selected-plan loader, this loads a bounded candidate set
     * that can be grouped into multiple planner-facing warehouse rows.
     *
     * It applies optional facility or facility-group filters before the builder groups rows into product/warehouse
     * plans.
     *
     * Input example:
     * filters = [mrpId: '10022', facilityGroupId: 'WH_GROUP']
     *
     * Output example:
     * [
     *   MrpEventView[mrpId: '10022', productId: 'GZ-8544', facilityId: 'WebStoreWarehouse'],
     *   MrpEventView[mrpId: '10022', productId: 'WG-1111', facilityId: 'PlantOne']
     * ]
     */
    static List loadEventPlanListEvents(Delegator delegator, Map filters, int candidateLimit, Locale locale = Locale.ENGLISH) {
        Map eventFilters = filters.findAll { String key, Object value ->
            UtilValidate.isNotEmpty(value)
        }
        List events = loadMrpEventViewRows(delegator, eventFilters, candidateLimit, true, locale)
        if (UtilValidate.isNotEmpty(filters.facilityId)) {
            return events.findAll { GenericValue event ->
                String facilityId = event.getString('facilityId')
                facilityId == filters.facilityId || UtilValidate.isEmpty(facilityId)
            }
        }
        if (UtilValidate.isNotEmpty(filters.facilityGroupId)) {
            Set facilityIds = activeFacilityIdsForGroup(delegator, filters.facilityGroupId)
            if (UtilValidate.isEmpty(facilityIds)) {
                return []
            }
            return events.findAll { GenericValue event ->
                String facilityId = event.getString('facilityId')
                facilityIds.contains(facilityId) || UtilValidate.isEmpty(facilityId)
            }
        }
        return events
    }

    /*
     * Executes the bounded MrpEventView query before any product/facility grouping or planner-facing shaping is applied.
     *
     * Input example:
     * filters = [mrpId: '10022', productId: 'GZ-8544']
     * candidateLimit = 1000
     *
     * Output example:
     * [
     *   MrpEventView[mrpEventTypeId: 'INITIAL_QOH', quantity: 18],
     *   MrpEventView[mrpEventTypeId: 'SALES_ORDER', quantity: -20],
     *   MrpEventView[mrpEventTypeId: 'PROP_PUR_O_RECP', quantity: 50]
     * ]
     */
    static List loadMrpEventViewRows(Delegator delegator, Map filters, int candidateLimit, boolean includeProductLevelEvents,
                                     Locale locale = Locale.ENGLISH) {
        List conditions = buildMrpEventViewConditions(delegator, filters, includeProductLevelEvents, locale)
        /*
         * Keep the DB read bounded here as well as at the service layer. Grouping and timeline calculation happen in
         * memory, so direct callers must not be able to accidentally load a very large MrpEventView result set.
         */
        int queryLimit = Math.max(1, Math.min(candidateLimit, MAX_MRP_EVENT_VIEW_ROWS))
        return EntityQuery.use(delegator).from('MrpEventView')
                .select('mrpId', 'productId', 'eventDate', 'mrpEventTypeId', 'facilityId', 'facilityIdTo', 'quantity',
                        'eventName', 'isLate', 'billOfMaterialLevel')
                .where(EntityCondition.makeCondition(conditions, EntityOperator.AND))
                .orderBy('productId', 'facilityId', 'mrpId', 'eventDate', 'mrpEventTypeId')
                .maxRows(queryLimit)
                .queryList()
    }

    /*
     * Loads reference rows needed to turn raw MrpEventView rows into planner-facing rows. Product, Facility,
     * ProductFacility, MrpEventType, and StatusItem records are batched here to avoid N+1 queries while preparing each
     * grouped plan timeline.
     *
     * Input example:
     * events = [MrpEventView[productId: 'GZ-8544', facilityId: 'WebStoreWarehouse', mrpEventTypeId: 'SALES_ORDER']]
     *
     * Output example:
     * [
     *   products: ['GZ-8544': Product[productId: 'GZ-8544']],
     *   facilities: ['WebStoreWarehouse': Facility[facilityId: 'WebStoreWarehouse']],
     *   productFacilities: ['GZ-8544|WebStoreWarehouse': ProductFacility[minimumStock: 20]],
     *   eventTypes: ['SALES_ORDER': MrpEventType[mrpEventTypeId: 'SALES_ORDER']],
     *   statuses: ['MRP_OPEN': StatusItem[statusId: 'MRP_OPEN']]
     * ]
     */
    static Map loadEventPlanReferenceData(Delegator delegator, List events, Collection extraFacilityIds = []) {
        Set productIds = events*.getString('productId').findAll { UtilValidate.isNotEmpty(it) } as Set
        Set facilityIds = (events*.getString('facilityId').findAll { UtilValidate.isNotEmpty(it) } +
                extraFacilityIds.findAll { UtilValidate.isNotEmpty(it) }) as Set
        Set eventTypeIds = events*.getString('mrpEventTypeId').findAll { UtilValidate.isNotEmpty(it) } as Set
        return [
                products: UtilValidate.isNotEmpty(productIds) ?
                        EntityUtil.lookupById(delegator, 'Product', 'productId', productIds, false) : [:],
                facilities: UtilValidate.isNotEmpty(facilityIds) ?
                        EntityUtil.lookupById(delegator, 'Facility', 'facilityId', facilityIds, false) : [:],
                productFacilities: loadProductFacilitiesByKey(delegator, productIds, facilityIds),
                eventTypes: UtilValidate.isNotEmpty(eventTypeIds) ?
                        EntityUtil.lookupById(delegator, 'MrpEventType', 'mrpEventTypeId', eventTypeIds) : [:],
                statuses: UtilValidate.isNotEmpty(events) ?
                        EntityUtil.lookupById(delegator, 'StatusItem', 'statusId',
                                [MrpEventPlanBuilder.MRP_OPEN_STATUS_ID]) : [:]
        ]
    }

    /*
     * Converts service filters into MrpEventView conditions. When no mrpId is supplied, it defaults to the latest MRP
     * run with a durable run log so Event Plans opens on the newest available ledger.
     *
     * Input example:
     * filters = [productId: 'GZ-8544', facilityId: 'WebStoreWarehouse', dateFrom: '2026-08-01']
     *
     * Output example:
     * [
     *   mrpId == latestMrpId,
     *   productId == 'GZ-8544',
     *   facilityId == 'WebStoreWarehouse' OR facilityId is product-level,
     *   eventDate >= Timestamp('2026-08-01 00:00:00')
     * ]
     */
    static List buildMrpEventViewConditions(Delegator delegator, Map filters, boolean includeProductLevelEvents,
                                            Locale locale = Locale.ENGLISH) {
        List conditions = []
        if (UtilValidate.isNotEmpty(filters.mrpId)) {
            conditions.add(EntityCondition.makeCondition('mrpId', filters.mrpId))
        } else {
            String latestMrpId = latestMrpIdFromRunLog(delegator)
            conditions.add(EntityCondition.makeCondition('mrpId', latestMrpId ?: '__NO_MATCHING_MRP__'))
        }
        if (UtilValidate.isNotEmpty(filters.productId)) {
            conditions.add(EntityCondition.makeCondition('productId', filters.productId))
        }
        if (UtilValidate.isNotEmpty(filters.facilityId)) {
            conditions.add(buildFacilityEventCondition(filters.facilityId, includeProductLevelEvents))
        }
        if (UtilValidate.isNotEmpty(filters.facilityGroupId)) {
            Set facilityIds = activeFacilityIdsForGroup(delegator, filters.facilityGroupId)
            conditions.add(UtilValidate.isNotEmpty(facilityIds) ? buildFacilityEventCondition(facilityIds,
                    includeProductLevelEvents) : EntityCondition.makeCondition('facilityId', '__NO_MATCHING_FACILITY__'))
        }
        if (UtilValidate.isNotEmpty(filters.mrpEventTypeId)) {
            conditions.add(EntityCondition.makeCondition('mrpEventTypeId', filters.mrpEventTypeId))
        }
        if (UtilValidate.isNotEmpty(filters.eventDate)) {
            if (DATE_ONLY_PATTERN.matcher(filters.eventDate.toString()).matches()) {
                conditions.add(EntityCondition.makeCondition('eventDate', EntityOperator.GREATER_THAN_EQUAL_TO,
                        parseEventDateBoundary(filters.eventDate, false, locale)))
                conditions.add(EntityCondition.makeCondition('eventDate', EntityOperator.LESS_THAN,
                        parseEventDateBoundary(filters.eventDate, true, locale)))
            } else {
                conditions.add(EntityCondition.makeCondition('eventDate', parseEventDateBoundary(filters.eventDate, false, locale)))
            }
        }
        Timestamp dateFrom = parseEventDateBoundary(filters.dateFrom, false, locale)
        if (dateFrom != null) {
            conditions.add(EntityCondition.makeCondition('eventDate', EntityOperator.GREATER_THAN_EQUAL_TO, dateFrom))
        }
        Timestamp dateTo = parseEventDateBoundary(filters.dateTo, true, locale)
        if (dateTo != null) {
            conditions.add(EntityCondition.makeCondition('eventDate', EntityOperator.LESS_THAN, dateTo))
        }
        String queryText = UtilValidate.isNotEmpty(filters.query) ? filters.query.toString().trim() :
                (UtilValidate.isNotEmpty(filters.search) ? filters.search.toString().trim() : null)
        if (UtilValidate.isNotEmpty(queryText)) {
            conditions.add(EntityCondition.makeCondition(buildMrpEventSearchConditions(delegator, queryText),
                    EntityOperator.OR))
        }
        return conditions
    }

    /*
     * Expands Event Plans free-text search across raw MRP event fields plus related product/facility display fields.
     * Product and Facility lookups are explicit because MrpEventView stores ids, not display names.
     *
     * Input example:
     * queryText = 'gizmo'
     *
     * Output example:
     * [
     *   upperLikeAny(['mrpId', 'productId', 'facilityId', 'mrpEventTypeId', 'eventName'], 'gizmo'),
     *   productId IN ['GZ-8544'],
     *   facilityId IN ['WebStoreWarehouse']
     * ]
     */
    static List buildMrpEventSearchConditions(Delegator delegator, String queryText) {
        List conditions = [
                EntityUtil.upperLikeAny(['mrpId', 'productId', 'facilityId', 'mrpEventTypeId', 'eventName'], queryText)
        ]
        Set matchingProductIds = EntityUtil.searchIds(delegator, 'Product', 'productId',
                ['productId', 'productName', 'internalName'], queryText, 1000)
        if (UtilValidate.isNotEmpty(matchingProductIds)) {
            conditions.add(EntityCondition.makeCondition('productId', EntityOperator.IN, matchingProductIds as List))
        }
        Set matchingFacilityIds = EntityUtil.searchIds(delegator, 'Facility', 'facilityId',
                ['facilityId', 'facilityName'], queryText,
                [EntityCondition.makeCondition('facilityTypeId', EntityOperator.IN, ['WAREHOUSE', 'PLANT'])], 1000)
        if (UtilValidate.isNotEmpty(matchingFacilityIds)) {
            conditions.add(EntityCondition.makeCondition('facilityId', EntityOperator.IN, matchingFacilityIds as List))
        }
        return conditions
    }

    /*
     * Finds the latest MRP id from durable run history so the list endpoint has a useful default when no mrpId filter is
     * supplied.
     *
     * Output example:
     * '10022'
     */
    static String latestMrpIdFromRunLog(Delegator delegator) {
        return EntityQuery.use(delegator).from('MrpRunLog')
                .select('mrpId')
                .where(EntityCondition.makeCondition('mrpId', EntityOperator.NOT_EQUAL, null))
                .orderBy('-startedAt', '-createdStamp')
                .maxRows(25)
                .queryList()
                .find { GenericValue runLog -> UtilValidate.isNotEmpty(runLog.getString('mrpId')) }
                ?.getString('mrpId')
    }

    /*
     * Builds the facility condition used by list/detail queries. Detail views include product-level rows, such as
     * organization-level forecasts, alongside warehouse-specific rows.
     *
     * Input example:
     * facilityIdOrIds = 'WebStoreWarehouse'
     * includeProductLevelEvents = true
     *
     * Output example:
     * facilityId == 'WebStoreWarehouse' OR facilityId is null OR facilityId == ''
     */
    static EntityCondition buildFacilityEventCondition(Object facilityIdOrIds, boolean includeProductLevelEvents) {
        EntityCondition facilityCondition = facilityIdOrIds instanceof Collection ?
                EntityCondition.makeCondition('facilityId', EntityOperator.IN, facilityIdOrIds as List) :
                EntityCondition.makeCondition('facilityId', facilityIdOrIds)
        if (!includeProductLevelEvents) {
            return facilityCondition
        }
        return EntityCondition.makeCondition([
                facilityCondition,
                EntityCondition.makeCondition('facilityId', null),
                EntityCondition.makeCondition('facilityId', '')
        ], EntityOperator.OR)
    }

    /*
     * Expands a facility group filter into active member facilities before the main MrpEventView query is built.
     *
     * Input example:
     * facilityGroupId = 'WH_GROUP'
     *
     * Output example:
     * ['WebStoreWarehouse', 'PlantOne'] as Set
     */
    static Set activeFacilityIdsForGroup(Delegator delegator, Object facilityGroupId) {
        if (UtilValidate.isEmpty(facilityGroupId)) {
            return [] as Set
        }
        return EntityQuery.use(delegator).from('FacilityGroupMember')
                .select('facilityId')
                .where(facilityGroupId: facilityGroupId)
                .filterByDate()
                .queryList()
                *.getString('facilityId')
                .findAll { UtilValidate.isNotEmpty(it) } as Set
    }

    /*
     * Reads product-facility stock policy rows keyed by product and warehouse so the builder can calculate safety-stock
     * attention without querying inside each grouped plan.
     *
     * Input example:
     * productIds = ['GZ-8544']
     * facilityIds = ['WebStoreWarehouse']
     *
     * Output example:
     * ['GZ-8544|WebStoreWarehouse': ProductFacility[minimumStock: 20, reorderQuantity: 50]]
     */
    private static Map loadProductFacilitiesByKey(Delegator delegator, Collection productIds, Collection facilityIds) {
        if (UtilValidate.isEmpty(productIds) || UtilValidate.isEmpty(facilityIds)) {
            return [:]
        }
        return EntityQuery.use(delegator).from('ProductFacility')
                .select('productId', 'facilityId', 'minimumStock', 'reorderQuantity', 'daysToShip')
                .where(EntityCondition.makeCondition([
                        EntityCondition.makeCondition('productId', EntityOperator.IN, productIds as List),
                        EntityCondition.makeCondition('facilityId', EntityOperator.IN, facilityIds as List)
                ], EntityOperator.AND))
                .queryList()
                .collectEntries { GenericValue productFacility ->
                    String key = MrpEventPlanBuilder.productFacilityLookupKey(productFacility.getString('productId'),
                            productFacility.getString('facilityId'))
                    [(key): productFacility]
                }
    }

    /*
     * Parses date/timestamp filters for EntityQuery conditions. Date-only end filters use an exclusive next-day boundary
     * so the whole selected date is included.
     *
     * Input example:
     * value = '2026-08-01'
     * exclusiveEnd = true
     *
     * Output example:
     * Timestamp('2026-08-02 00:00:00')
     */
    private static Timestamp parseEventDateBoundary(Object value, boolean exclusiveEnd, Locale locale = Locale.ENGLISH) {
        if (UtilValidate.isEmpty(value)) {
            return null
        }
        try {
            if (DATE_ONLY_PATTERN.matcher(value.toString()).matches()) {
                LocalDate date = LocalDate.parse(value.toString())
                return Timestamp.valueOf((exclusiveEnd ? date.plusDays(1) : date).atStartOfDay())
            }
            return (Timestamp) ObjectType.simpleTypeOrObjectConvert(value, 'Timestamp', null, null)
        } catch (Exception e) {
            throw new IllegalArgumentException(UtilProperties.getMessage(RESOURCE, 'ManufacturingMrpEventDateFilterInvalid',
                    [value: value], locale ?: Locale.ENGLISH))
        }
    }

}
