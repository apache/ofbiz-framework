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

import java.nio.charset.StandardCharsets
import java.sql.Timestamp

import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.base.util.UtilGenerics
import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.base.util.collections.MapComparator
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.manufacturing.api.ManufacturingServiceUtil

/*
 * Converts raw MrpEventView ledger rows into grouped product/warehouse plan rows.
 *
 * Pipeline:
 * 1. buildWarehouseEventPlanContexts groups raw rows by MRP run, product, and facility.
 * 2. buildWarehouseEventPlanContext builds the working context for one product/warehouse group.
 * 3. buildRunningBalanceTimeline calculates running inventory balances and timeline metadata.
 * 4. buildEventPlanResponseRow shapes the context into the service response row.
 * 5. buildAttentionSignals and buildInventoryContext add planner action and inventory facts.
 */
final class MrpEventPlanBuilder {

    static final String MRP_OPEN_STATUS_ID = 'MRP_OPEN'
    private static final String RESOURCE = 'ManufacturingUiLabels'
    static final Set EVENT_PLAN_SORT_FIELDS = [
            'productId',
            'productName',
            'facilityId',
            'facilityName',
            'statusId',
            'startingQoh',
            'lowestRunningBalance',
            'minimumStock'
    ] as Set

    private static final Map SEVERITY_PRIORITY = [HIGH: 0, MEDIUM: 1].asImmutable()
    private static final List SETUP_SIGNAL_KEYWORDS = ['SETUP', 'BOM', 'ROUTING', 'SUPPLIER', 'ERROR'].asImmutable()

    private MrpEventPlanBuilder() { }

    // Event Plan Pipeline

    /*
     * Raw MRP events are ledger rows, so group them before presenting planner-ready balances. The same product can have
     * demand in WH1 and PLANT1, each with its own ProductFacility minimum stock. Grouping by product/facility prevents
     * those balances from being mixed before attention metadata is calculated.
     *
     * Input shape:
     * [
     *   [mrpId: '10022', productId: 'GZ-8544', facilityId: 'WebStoreWarehouse', quantity: 18],
     *   [mrpId: '10022', productId: 'GZ-8544', facilityId: 'WebStoreWarehouse', quantity: -20],
     *   [mrpId: '10022', productId: 'GZ-8544', facilityId: 'WebStoreWarehouse', quantity: 50]
     * ]
     *
     * Output shape:
     * [
     *   '10022|GZ-8544|WebStoreWarehouse': [
     *     mrpId: '10022',
     *     productId: 'GZ-8544',
     *     facilityId: 'WebStoreWarehouse',
     *     startingQoh: 18,
     *     timeline: [...],
     *     hasInitialQoh: true
     *   ]
     * ]
     *
     * Example grouped plan:
     * GZ-8544 at WebStoreWarehouse
     *   starting QOH: 18
     *   demand: -20
     *   proposed supply: +50
     *   final balance: 48
     *   attention: on track / needs attention
     */
    static Map buildWarehouseEventPlanContexts(List events, Map eventPlanReferenceData, Locale locale = Locale.ENGLISH) {
        return events.groupBy { GenericValue event -> [event.getString('mrpId'), event.getString('productId')].join('|') }
                .collectEntries { String productKey, List productEvents ->
                    List productLevelEvents = productEvents.findAll { GenericValue event ->
                        UtilValidate.isEmpty(event.getString('facilityId'))
                    }
                    productEvents.findAll { GenericValue event -> UtilValidate.isNotEmpty(event.getString('facilityId')) }
                            .groupBy { GenericValue event -> eventPlanGroupKey(event) }
                            .collectEntries { String key, List warehouseEvents ->
                                [(key): buildWarehouseEventPlanContext(key, warehouseEvents + productLevelEvents,
                                        eventPlanReferenceData, locale)]
                            }
                }
    }

    /*
     * Prepares one product/warehouse plan context from a grouped MRP event stream. The context centralizes ids, display
     * names, stock policy, timeline balances, and setup flags so the final service row can be assembled without
     * re-querying or recalculating those values.
     *
     * Input example:
     * key = '10022|GZ-8544|WebStoreWarehouse'
     * events = [
     *   [mrpEventTypeId: 'INITIAL_QOH', quantity: 18],
     *   [mrpEventTypeId: 'SALES_ORDER', quantity: -20],
     *   [mrpEventTypeId: 'PROP_PUR_O_RECP', quantity: 50]
     * ]
     *
     * Output example:
     * [
     *   key: '10022|GZ-8544|WebStoreWarehouse',
     *   mrpId: '10022',
     *   productId: 'GZ-8544',
     *   productName: 'Demo Gizmo',
     *   facilityId: 'WebStoreWarehouse',
     *   facilityName: 'Web Store Warehouse',
     *   minimumStock: 20,
     *   startingQoh: 18,
     *   timeline: [[quantity: 18, runningBalance: 18], [quantity: -20, runningBalance: -2]],
     *   hasInitialQoh: true,
     *   hasSetupSignal: false
     * ]
     */
    static Map buildWarehouseEventPlanContext(String key, List events, Map eventPlanReferenceData, Locale locale = Locale.ENGLISH) {
        List sortedEvents = events.toSorted({ GenericValue left, GenericValue right ->
            compareMrpTimelineEvents(left, right)
        } as Comparator)
        GenericValue firstEvent = UtilValidate.isNotEmpty(sortedEvents) ? sortedEvents.first() : null
        /*
         * Product-level forecast rows can have no facility; preserve the grouped key parts as a fallback for context
         * ids.
         */
        String[] keyParts = key.split('\\|', -1)
        String productIdFromKey = keyParts.length > 1 && UtilValidate.isNotEmpty(keyParts[1]) ? keyParts[1] : null
        String facilityIdFromKey = keyParts.length > 2 && UtilValidate.isNotEmpty(keyParts[2]) ? keyParts[2] : null
        String mrpId = firstEvent?.getString('mrpId')
        String productId = firstEvent?.getString('productId') ?: productIdFromKey
        String facilityId = firstEvent?.getString('facilityId') ?: facilityIdFromKey
        GenericValue product = eventPlanReferenceData.products[productId]
        GenericValue facility = eventPlanReferenceData.facilities[facilityId]
        GenericValue productFacility = eventPlanReferenceData.productFacilities[productFacilityLookupKey(productId, facilityId)]
        BigDecimal minimumStock = productFacility?.getBigDecimal('minimumStock')
        BigDecimal reorderQuantity = productFacility?.getBigDecimal('reorderQuantity')
        Long productFacilityDaysToShip = productFacility?.getLong('daysToShip')
        Long daysToShip = productFacilityDaysToShip != null ? productFacilityDaysToShip :
                facility?.getLong('defaultDaysToShip')
        List timeline = buildRunningBalanceTimeline(sortedEvents, eventPlanReferenceData, productFacility, locale)
        return [
                key: key,
                mrpId: mrpId,
                productId: productId,
                productName: ManufacturingServiceUtil.displayProductName(product),
                facilityId: facilityId,
                facilityName: ManufacturingServiceUtil.displayFacilityName(facility),
                minimumStock: minimumStock,
                reorderQuantity: reorderQuantity,
                daysToShip: daysToShip,
                startingQoh: sortedEvents.find { GenericValue event ->
                    event.getString('mrpEventTypeId') == 'INITIAL_QOH'
                }?.getBigDecimal('quantity'),
                timeline: timeline,
                events: plannerVisibleTimelineRows(sortedEvents),
                hasInitialQoh: sortedEvents.any { GenericValue event -> event.getString('mrpEventTypeId') == 'INITIAL_QOH' },
                hasSetupSignal: sortedEvents.any { GenericValue event ->
                    String eventDisplayDescription = event.getString('eventName')?.toUpperCase()
                    event.getString('mrpEventTypeId') == 'ERROR' ||
                            (UtilValidate.isNotEmpty(eventDisplayDescription) && SETUP_SIGNAL_KEYWORDS.any {
                                eventDisplayDescription.contains(it)
                            })
                }
        ]
    }

    // Timeline Preparation

    // Hides internal planning markers from planner-facing evidence while keeping them in balance calculations.
    static List plannerVisibleTimelineRows(List events) {
        return events.findAll { event ->
            String mrpEventTypeId = event instanceof GenericValue ? event.getString('mrpEventTypeId') :
                    event.mrpEventTypeId
            mrpEventTypeId != 'REQUIRED_MRP'
        }
    }

    /*
     * Calculates running balances and timeline metadata for one grouped plan. REQUIRED_MRP markers stay in the
     * arithmetic even though plannerVisibleTimelineRows hides them from planner-facing evidence.
     *
     * Input example:
     * events = [
     *   [mrpEventTypeId: 'INITIAL_QOH', quantity: 18],
     *   [mrpEventTypeId: 'SALES_ORDER', quantity: -20],
     *   [mrpEventTypeId: 'PROP_PUR_O_RECP', quantity: 50]
     * ]
     *
     * Output example:
     * [
     *   [mrpEventTypeId: 'INITIAL_QOH', quantity: 18, runningBalance: 18, belowMinimumStock: true],
     *   [mrpEventTypeId: 'SALES_ORDER', quantity: -20, runningBalance: -2, belowMinimumStock: true],
     *   [mrpEventTypeId: 'PROP_PUR_O_RECP', quantity: 50, runningBalance: 48, belowMinimumStock: false]
     * ]
     */
    static List buildRunningBalanceTimeline(List events, Map eventPlanReferenceData, GenericValue productFacility,
                                             Locale locale = Locale.ENGLISH) {
        BigDecimal runningBalance = BigDecimal.ZERO
        BigDecimal minimumStock = productFacility?.getBigDecimal('minimumStock')
        return events.collect { GenericValue event ->
            String mrpId = event.getString('mrpId')
            String mrpEventTypeId = event.getString('mrpEventTypeId')
            String productId = event.getString('productId')
            String facilityId = event.getString('facilityId')
            String eventName = event.getString('eventName')
            Timestamp eventDate = event.getTimestamp('eventDate')
            BigDecimal quantity = event.getBigDecimal('quantity') ?: BigDecimal.ZERO
            runningBalance = runningBalance.add(quantity)
            GenericValue eventType = eventPlanReferenceData.eventTypes[mrpEventTypeId]
            GenericValue product = eventPlanReferenceData.products[productId]
            GenericValue facility = eventPlanReferenceData.facilities[facilityId]
            String facilityName = ManufacturingServiceUtil.displayFacilityName(facility)
            if (UtilValidate.isEmpty(facilityName) && mrpEventTypeId == 'SALES_FORECAST' &&
                    UtilValidate.isEmpty(facilityId)) {
                facilityName = 'Organization-level'
            }
            GenericValue rowProductFacility = eventPlanReferenceData.productFacilities[productFacilityLookupKey(productId,
                    facilityId)]
            BigDecimal rowMinimumStock = rowProductFacility?.getBigDecimal('minimumStock') ?: minimumStock
            Long rowProductFacilityDaysToShip = rowProductFacility?.getLong('daysToShip')
            Map requirementReference = extractProposedSupplyRequirementReference(event)
            [
                    eventId: buildTimelineEventId(event),
                    mrpId: mrpId,
                    mrpEventTypeId: mrpEventTypeId,
                    eventTypeLabel: eventTypeDisplayLabel(mrpEventTypeId, eventType, quantity, locale),
                    eventDescription: eventDisplayDescription(event, eventType, quantity, locale),
                    eventDate: eventDate,
                    quantity: quantity,
                    runningBalance: runningBalance,
                    isLate: event.getString('isLate') == 'Y',
                    sourceId: eventName,
                    requirementId: requirementReference.requirementId,
                    requirementDate: requirementReference.requirementDate,
                    statusId: MRP_OPEN_STATUS_ID,
                    statusDescription: ManufacturingServiceUtil.displayStatusDescription(
                            eventPlanReferenceData.statuses[MRP_OPEN_STATUS_ID]) ?: label('ManufacturingMrpOpen', locale),
                    productId: productId,
                    productName: ManufacturingServiceUtil.displayProductName(product),
                    facilityId: facilityId,
                    facilityName: facilityName,
                    minimumStock: rowMinimumStock,
                    reorderQuantity: rowProductFacility?.getBigDecimal('reorderQuantity'),
                    daysToShip: rowProductFacilityDaysToShip != null ? rowProductFacilityDaysToShip :
                            facility?.getLong('defaultDaysToShip'),
                    startingQoh: null,
                    belowMinimumStock: rowMinimumStock != null && runningBalance < rowMinimumStock
            ]
        }.with { List rows ->
            BigDecimal firstQoh = rows.find { Map row -> row.mrpEventTypeId == 'INITIAL_QOH' }?.quantity
            rows.each { Map row -> row.startingQoh = firstQoh }
            rows
        }
    }

    // Response Row Shaping

    /*
     * Converts one prepared product/warehouse context into the Event Plans response row consumed by clients.
     *
     * Input example:
     * groupContext = [
     *   productId: 'GZ-8544',
     *   facilityId: 'WebStoreWarehouse',
     *   startingQoh: 18,
     *   minimumStock: 20,
     *   timeline: [[quantity: 18, runningBalance: 18], [quantity: -20, runningBalance: -2]]
     * ]
     *
     * Output example:
     * [
     *   planId: 'mrp_...',
     *   productId: 'GZ-8544',
     *   facilityId: 'WebStoreWarehouse',
     *   statusDescription: 'Needs attention',
     *   startingQoh: 18,
     *   lowestRunningBalance: -2,
     *   actionNeeded: true,
     *   attentionTypes: ['BELOW_SAFETY_STOCK'],
     *   inventoryContext: [projectedBalance: 48, lowestRunningBalance: -2],
     *   evidence: [...],
     *   timeline: [...]
     * ]
     */
    static Map buildEventPlanResponseRow(Map groupContext, Locale locale = Locale.ENGLISH) {
        Map inventory = buildInventoryContext(groupContext.timeline.first() ?: [:], groupContext)
        List planSignals = buildAttentionSignals(groupContext, locale)
        List actionSignals = planSignals.findAll { Map signal -> signal.actionNeeded != false }
        Map primarySignal = actionSignals.min { Map signal -> signal.severityPriority } ?: [:]
        return [
                planId: buildEventPlanId(groupContext),
                mrpId: groupContext.mrpId,
                productId: groupContext.productId,
                productName: groupContext.productName,
                facilityId: groupContext.facilityId,
                facilityName: groupContext.facilityName,
                statusId: MRP_OPEN_STATUS_ID,
                statusDescription: UtilValidate.isNotEmpty(actionSignals) ? label('ManufacturingMrpNeedsAttention', locale) :
                        label('ManufacturingMrpOnTrack', locale),
                minimumStock: groupContext.minimumStock,
                reorderQuantity: groupContext.reorderQuantity,
                daysToShip: groupContext.daysToShip,
                startingQoh: groupContext.startingQoh,
                lowestRunningBalance: inventory.lowestRunningBalance,
                actionNeeded: UtilValidate.isNotEmpty(actionSignals),
                attentionSeverity: primarySignal.severity,
                attentionTypes: actionSignals*.type,
                primaryAttentionType: primarySignal.type,
                attentionSummary: actionSignals*.summary.findAll { it }.join('; '),
                inlineGuidance: primarySignal.inlineGuidance,
                inventoryContext: inventory,
                evidence: plannerVisibleTimelineRows(groupContext.timeline),
                timeline: groupContext.timeline
        ]
    }

    // Planner Attention

    /*
     * Produces planner-action chips and guidance from the prepared timeline context.
     *
     * Input example:
     * groupContext = [
     *   minimumStock: 20,
     *   timeline: [[mrpEventTypeId: 'SALES_ORDER', runningBalance: -2, belowMinimumStock: true]]
     * ]
     *
     * Output example:
     * [
     *   [
     *     type: 'BELOW_SAFETY_STOCK',
     *     severity: 'HIGH',
     *     actionNeeded: true,
     *     summary: 'Projected below safety stock',
     *     inlineGuidance: [
     *       recommendation: 'Review projected inventory because the running balance falls below minimum stock.'
     *     ]
     *   ]
     * ]
     */
    static List buildAttentionSignals(Map groupContext, Locale locale = Locale.ENGLISH) {
        List signals = []
        if (groupContext.timeline.any { Map row -> row.isLate }) {
            signals.add(buildAttentionSignal('LATE_EVENT', 'HIGH',
                    label('ManufacturingMrpLateProposedSupply', locale),
                    label('ManufacturingMrpLateProposedSupplyGuidance', locale), locale))
        }
        if (groupContext.timeline.any { Map row -> row.mrpEventTypeId == 'ERROR' }) {
            signals.add(buildAttentionSignal('ENGINE_ERROR', 'HIGH',
                    label('ManufacturingMrpEngineError', locale),
                    label('ManufacturingMrpEngineErrorGuidance', locale), locale))
        }
        Map safetyStockEvent = findUnresolvedSafetyStockEvent(groupContext)
        if (UtilValidate.isNotEmpty(safetyStockEvent)) {
            boolean currentBelowSafetyStock = safetyStockEvent.mrpEventTypeId == 'INITIAL_QOH'
            signals.add(buildAttentionSignal('BELOW_SAFETY_STOCK', attentionSeverityFor('BELOW_SAFETY_STOCK', safetyStockEvent),
                    currentBelowSafetyStock ? label('ManufacturingMrpCurrentBelowSafetyStock', locale) :
                            label('ManufacturingMrpProjectedBelowSafetyStock', locale),
                    currentBelowSafetyStock
                            ? label('ManufacturingMrpCurrentBelowSafetyStockGuidance', locale)
                            : label('ManufacturingMrpProjectedBelowSafetyStockGuidance', locale), locale))
        } else {
            Map recoveredSafetyStockEvent = groupContext.timeline.findAll { Map row -> row.belowMinimumStock }
                    .min { Map row -> row.runningBalance ?: BigDecimal.ZERO }
            if (UtilValidate.isNotEmpty(recoveredSafetyStockEvent)) {
                signals.add(buildAttentionSignal('BELOW_SAFETY_STOCK', attentionSeverityFor('BELOW_SAFETY_STOCK',
                        recoveredSafetyStockEvent), null, null, locale, [actionNeeded: false]))
            }
        }
        // Missing initial QOH usually points to setup data, not a normal inventory movement.
        if ((!groupContext.hasInitialQoh && UtilValidate.isNotEmpty(groupContext.facilityId)) ||
                groupContext.hasSetupSignal) {
            signals.add(buildAttentionSignal('SETUP_ISSUE', 'HIGH',
                    label('ManufacturingMrpSetupIssue', locale),
                    label('ManufacturingMrpSetupIssueGuidance', locale), locale))
        }
        if (groupContext.timeline.any { Map row -> isOrganizationLevelForecast(row) }) {
            signals.add(buildAttentionSignal('FORECAST_ALLOCATION', 'MEDIUM',
                    label('ManufacturingMrpForecastAllocation', locale),
                    label('ManufacturingMrpForecastAllocationGuidance', locale), locale))
        }
        return signals.unique { Map signal -> signal.type }
    }

    // Filtering And Sorting

    // Applies MRP-specific filters after framework paging/sort parameters have been parsed.
    static boolean matchesEventPlanFilters(Map plan, Map filters) {
        if (Boolean.valueOf(filters.attentionOnly?.toString()) && !plan.actionNeeded) {
            return false
        }
        if (UtilValidate.isNotEmpty(filters.attentionType) && !(filters.attentionType in (plan.attentionTypes ?: []))) {
            return false
        }
        String queryText = UtilValidate.isNotEmpty(filters.query) ? filters.query.toString().trim() :
                (UtilValidate.isNotEmpty(filters.search) ? filters.search.toString().trim() : null)
        return matchesEventPlanSearch(plan, queryText)
    }

    // Provides deterministic fallback ordering when no framework sort is requested.
    static Comparator<Map<String, Object>> buildEventPlanFallbackComparator() {
        return { Map left, Map right ->
            new MapComparator(['productId', 'facilityId', 'mrpId']).compare(UtilGenerics.cast(left),
                    UtilGenerics.cast(right))
        } as Comparator
    }

    // Applies the same free-text query to already-prepared rows after grouping and computed fields are available.
    static boolean matchesEventPlanSearch(Map row, String queryText) {
        if (UtilValidate.isEmpty(queryText)) {
            return true
        }
        String needle = queryText.toUpperCase()
        return [row.productId, row.productName, row.facilityId, row.facilityName, row.exceptionType,
                row.eventDescription, row.mrpId].find { Object value ->
            value?.toString()?.toUpperCase()?.contains(needle)
        } != null
    }

    // Timeline Ordering And Stable IDs

    // Ensures running balances start with initial QOH before same-run demand/supply events are applied.
    static int compareMrpTimelineEvents(GenericValue left, GenericValue right) {
        Map leftFields = [
                eventSortBucket: left.getString('mrpEventTypeId') == 'INITIAL_QOH' ? 0 : 1,
                eventDate: left.getTimestamp('eventDate'),
                mrpEventTypeId: left.getString('mrpEventTypeId'),
                eventName: left.getString('eventName')
        ]
        Map rightFields = [
                eventSortBucket: right.getString('mrpEventTypeId') == 'INITIAL_QOH' ? 0 : 1,
                eventDate: right.getTimestamp('eventDate'),
                mrpEventTypeId: right.getString('mrpEventTypeId'),
                eventName: right.getString('eventName')
        ]
        return new MapComparator(['eventSortBucket', 'eventDate', 'mrpEventTypeId', 'eventName']).compare(
                UtilGenerics.cast(leftFields), UtilGenerics.cast(rightFields))
    }

    // Builds a stable id for computed timeline rows, which do not have a single DB primary key.
    static String buildTimelineEventId(Object event) {
        Object eventDate = event instanceof GenericValue ? event.getTimestamp('eventDate') : event.eventDate
        String eventDateKey = eventDate instanceof Timestamp ? Long.toString(eventDate.time) : eventDate?.toString()
        return encodeCompositeId(['event',
                event instanceof GenericValue ? event.getString('mrpId') : event.mrpId,
                event instanceof GenericValue ? event.getString('productId') : event.productId,
                event instanceof GenericValue ? event.getString('facilityId') : event.facilityId,
                eventDateKey,
                event instanceof GenericValue ? event.getString('mrpEventTypeId') : event.mrpEventTypeId])
    }

    // Builds a stable id for computed product/warehouse plan rows.
    static String buildEventPlanId(Map groupContext) {
        return encodeCompositeId(['plan', groupContext.mrpId, groupContext.productId, groupContext.facilityId])
    }

    // Creates an in-memory grouping key for mrpId/productId/facilityId.
    static String eventPlanGroupKey(Object value) {
        if (value instanceof GenericValue) {
            return [value.getString('mrpId'), value.getString('productId'), value.getString('facilityId') ?: ''].join('|')
        }
        return [value.mrpId, value.productId, value.facilityId ?: ''].join('|')
    }

    // Creates a lookup key for batched ProductFacility reference rows.
    static String productFacilityLookupKey(Object productId, Object facilityId) {
        return [productId ?: '', facilityId ?: ''].join('|')
    }

    // Internal Attention Helpers

    // Organization-level forecasts need planner allocation before warehouse plans can be trusted.
    private static boolean isOrganizationLevelForecast(Map event) {
        return event?.mrpEventTypeId == 'SALES_FORECAST' && UtilValidate.isEmpty(event.facilityId)
    }

    // Finds the first unresolved balance problem that still needs planner attention.
    private static Object findUnresolvedSafetyStockEvent(Map groupContext) {
        if (UtilValidate.isEmpty(groupContext?.timeline)) {
            return null
        }

        Map negativeBalanceEvent = groupContext.timeline
                .findAll { Map event -> event.runningBalance != null && event.runningBalance < BigDecimal.ZERO }
                .min { Map event -> event.runningBalance ?: BigDecimal.ZERO }
        if (UtilValidate.isNotEmpty(negativeBalanceEvent)) {
            return negativeBalanceEvent
        }

        if (groupContext.minimumStock == null) {
            return null
        }

        Map finalEvent = finalPlannerVisibleTimelineRow(groupContext.timeline)
        if (finalEvent?.runningBalance != null && finalEvent.runningBalance < groupContext.minimumStock) {
            return finalEvent
        }

        Map firstBelowMinimum = groupContext.timeline.find { Map event -> event.belowMinimumStock }
        if (UtilValidate.isEmpty(firstBelowMinimum)) {
            return null
        }

        if (firstBelowMinimum.mrpEventTypeId != 'INITIAL_QOH') {
            return firstBelowMinimum
        }

        return hasOnTimeProposedSupplyRecovery(groupContext) ? null : firstBelowMinimum
    }

    // Uses the last user-visible event when deciding whether the plan recovers.
    private static Map finalPlannerVisibleTimelineRow(List timeline) {
        List rows = plannerVisibleTimelineRows(timeline)
        return UtilValidate.isNotEmpty(rows) ? rows.last() : timeline.last()
    }

    // A below-minimum starting balance is acceptable if on-time proposed supply recovers the plan.
    private static boolean hasOnTimeProposedSupplyRecovery(Map groupContext) {
        Map finalEvent = finalPlannerVisibleTimelineRow(groupContext.timeline)
        if (finalEvent?.runningBalance == null || finalEvent.runningBalance < groupContext.minimumStock) {
            return false
        }
        return groupContext.timeline.any { Map event ->
            event.mrpEventTypeId in ['PROP_PUR_O_RECP', 'PROP_MANUF_O_RECP'] &&
                    event.quantity > BigDecimal.ZERO && !event.isLate
        }
    }

    /*
     * Creates the normalized attention-chip payload used by filters, summaries, and inline planner guidance.
     *
     * Input example:
     * type = 'BELOW_SAFETY_STOCK'
     * severity = 'HIGH'
     * summary = 'Projected below safety stock'
     * recommendation = 'Review projected inventory because the running balance falls below minimum stock.'
     * fields = [actionNeeded: true]
     *
     * Output example:
     * [
     *   type: 'BELOW_SAFETY_STOCK',
     *   severity: 'HIGH',
     *   severityPriority: 0,
     *   summary: 'Projected below safety stock',
     *   actionNeeded: true,
     *   inlineGuidance: [
     *     recommendation: 'Review projected inventory because the running balance falls below minimum stock.',
     *     source: 'OFBiz MRP event ledger'
     *   ]
     * ]
     */
    private static Map buildAttentionSignal(String type, String severity, String summary, String recommendation,
                                            Locale locale = Locale.ENGLISH, Map fields = [:]) {
        Integer severityPriority = SEVERITY_PRIORITY.containsKey(severity) ? SEVERITY_PRIORITY[severity] : 2
        Map signal = [
                type: type,
                severity: severity,
                severityPriority: severityPriority,
                summary: summary,
                actionNeeded: fields.actionNeeded != false
        ]
        if (UtilValidate.isNotEmpty(recommendation)) {
            signal.inlineGuidance = [
                    recommendation: recommendation,
                    source: label('ManufacturingMrpEventLedgerSource', locale)
            ]
        }
        return signal
    }

    // Internal Row And Label Helpers

    /*
     * Collects inventory balance facts separately from display-oriented attention summaries.
     *
     * Input example:
     * summary = [runningBalance: 48]
     * groupContext = [
     *   productId: 'GZ-8544',
     *   facilityId: 'WebStoreWarehouse',
     *   startingQoh: 18,
     *   minimumStock: 20,
     *   reorderQuantity: 50,
     *   timeline: [[runningBalance: 18], [runningBalance: -2, belowMinimumStock: true], [runningBalance: 48]]
     * ]
     *
     * Output example:
     * [
     *   productId: 'GZ-8544',
     *   facilityId: 'WebStoreWarehouse',
     *   startingQoh: 18,
     *   minimumStock: 20,
     *   reorderQuantity: 50,
     *   projectedBreachAmount: 22,
     *   projectedBalance: 48,
     *   lowestRunningBalance: -2,
     *   breachDate: null
     * ]
     */
    private static Map buildInventoryContext(Map summary, Map groupContext) {
        BigDecimal lowestBalance = groupContext.timeline*.runningBalance.findAll { it != null }.min()
        Map breach = groupContext.timeline.find { Map row -> row.belowMinimumStock }
        BigDecimal projectedBreachAmount = null
        if (breach?.runningBalance != null && groupContext.minimumStock != null) {
            projectedBreachAmount = groupContext.minimumStock.subtract(breach.runningBalance)
        }
        return [
                productId: groupContext.productId,
                productName: groupContext.productName,
                facilityId: groupContext.facilityId,
                facilityName: groupContext.facilityName,
                startingQoh: groupContext.startingQoh,
                minimumStock: groupContext.minimumStock,
                reorderQuantity: groupContext.reorderQuantity,
                daysToShip: groupContext.daysToShip,
                projectedBreachAmount: projectedBreachAmount,
                projectedBalance: summary.runningBalance,
                lowestRunningBalance: lowestBalance,
                breachDate: breach?.eventDate
        ]
    }

    // Uses MrpEventType.description from seed data, except for the zero-quantity stock policy marker.
    private static String eventTypeDisplayLabel(Object mrpEventTypeId, GenericValue eventType, BigDecimal quantity = null,
                                                Locale locale = Locale.ENGLISH) {
        if (isStockPolicyTrigger(mrpEventTypeId, quantity)) {
            return label('ManufacturingMrpStockPolicyTrigger', locale)
        }
        String description = eventType?.getString('description')
        return UtilValidate.isNotEmpty(description) ? description : mrpEventTypeId
    }

    // Explains synthetic stock-policy markers differently from ordinary event descriptions.
    private static String eventDisplayDescription(GenericValue event, GenericValue eventType, BigDecimal quantity,
                                                  Locale locale = Locale.ENGLISH) {
        if (isStockPolicyTrigger(event.getString('mrpEventTypeId'), quantity)) {
            return label('ManufacturingMrpStockPolicyTriggerDescription', locale)
        }
        String eventName = event.getString('eventName')
        return UtilValidate.isNotEmpty(eventName) ? eventName : eventType?.getString('description')
    }

    // Identifies the internal marker created when current inventory is below minimum stock.
    private static boolean isStockPolicyTrigger(Object mrpEventTypeId, BigDecimal quantity) {
        return mrpEventTypeId?.toString() == 'REQUIRED_MRP' && quantity != null && quantity == BigDecimal.ZERO
    }

    // Extracts the covered requirement id/date from proposed-supply event names generated by the MRP engine.
    private static Map extractProposedSupplyRequirementReference(GenericValue event) {
        if (!(event?.getString('mrpEventTypeId') in ['PROP_PUR_O_RECP', 'PROP_MANUF_O_RECP'])) {
            return [:]
        }
        String eventName = event.getString('eventName')
        if (UtilValidate.isEmpty(eventName)) {
            return [:]
        }
        return (eventName =~ /^\*(\S+)\s+\((.+)\)\*$/).with { matcher ->
            matcher.matches() ? [
                    requirementId: matcher.group(1),
                    requirementDate: Timestamp.valueOf(matcher.group(2))
            ] : [:]
        }
    }

    // Converts attention types into a severity used by the planner chips.
    private static String attentionSeverityFor(String exceptionType, Map event) {
        if (exceptionType in ['ENGINE_ERROR', 'SETUP_ISSUE', 'LATE_EVENT']) {
            return 'HIGH'
        }
        return exceptionType == 'BELOW_SAFETY_STOCK' && event.runningBalance != null &&
                event.runningBalance < BigDecimal.ZERO ? 'HIGH' : 'MEDIUM'
    }

    // Encodes composite ids so delimiters inside product/facility ids cannot make ambiguous service ids.
    private static String encodeCompositeId(List parts) {
        String payload = parts.collect { it?.toString() ?: '' }.join('\u001F')
        return 'mrp_' + Base64.urlEncoder.withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8))
    }

    private static String label(String key, Locale locale, Map parameters = [:]) {
        return UtilProperties.getMessage(RESOURCE, key, parameters, locale ?: Locale.ENGLISH)
    }

}
