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
package org.apache.ofbiz.manufacturing.api

import static org.junit.jupiter.api.Assertions.assertThrows

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.util.EntityQuery
import org.apache.ofbiz.manufacturing.mrp.MrpEventPlanBuilder
import org.apache.ofbiz.manufacturing.mrp.MrpEventPlanQuery
import org.apache.ofbiz.manufacturing.mrp.MrpServices
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.ws.rs.util.RestApiUtil
import org.apache.ofbiz.ws.rs.util.RestQueryOptions
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest

import java.sql.Timestamp

@SuppressWarnings('MethodCount')
class MrpPlanningServicesTests {

    @Test
    void testEventPlanFiltersIgnoreUnsupportedFrameworkFilterFields() {
        Map filters = [
                mrpId: '10010',
                productId: 'GZ-8544',
                attentionOnly: true,
                unsupportedFilter: 'SHOULD_NOT_CHANGE_RESULTS'
        ]
        Map matchingPlan = [mrpId: '10010', productId: 'GZ-8544', actionNeeded: true]
        Map nonAttentionPlan = [mrpId: '10010', productId: 'GZ-8544', actionNeeded: false]

        assert MrpEventPlanBuilder.matchesEventPlanFilters(matchingPlan, filters)
        assert !MrpEventPlanBuilder.matchesEventPlanFilters(nonAttentionPlan, filters)
    }

    @Test
    void testStringBooleanFiltersMatchRestQueryParameters() {
        Map cleanPlan = [actionNeeded: false]

        assert !MrpEventPlanBuilder.matchesEventPlanFilters(cleanPlan, [attentionOnly: 'true'])
        assert MrpEventPlanBuilder.matchesEventPlanFilters(cleanPlan, [attentionOnly: 'false'])
    }

    @Test
    void testRunSortMapsMrpNameToFrameworkOrderBy() {
        Script services = runLogData()

        assert services.resolveMrpRunOrderBy('mrpName') == ['mrpName', '-startedAt', '-createdStamp']
    }

    @Test
    void testRunSortRejectsUnsupportedField() {
        Script services = runLogData()

        IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
            services.resolveMrpRunOrderBy('notAField')
        }
        assert exception.message == 'Unsupported sort field: notAField'
    }

    @Test
    void testEventPlansSortByProductIdDescending() {
        List planContexts = [
                [productId: 'MAT-A', facilityId: 'WH1', mrpId: '10010'],
                [productId: 'MAT-C', facilityId: 'WH1', mrpId: '10010'],
                [productId: 'MAT-B', facilityId: 'WH1', mrpId: '10010']
        ]
        List sortedPlans = RestApiUtil.sortMapRows(planContexts, '-productId', MrpEventPlanBuilder.EVENT_PLAN_SORT_FIELDS,
                MrpEventPlanBuilder.buildEventPlanFallbackComparator())

        assert sortedPlans*.productId == ['MAT-C', 'MAT-B', 'MAT-A']
    }

    @Test
    void testEventPlansAttentionOnlyFiltersRowsWithActionSignals() {
        Map attentionPlan = [attentionTypes: ['BELOW_SAFETY_STOCK'], actionNeeded: true]
        Map cleanPlan = [attentionTypes: [], actionNeeded: false]

        assert MrpEventPlanBuilder.matchesEventPlanFilters(attentionPlan, [attentionOnly: 'true'])
        assert !MrpEventPlanBuilder.matchesEventPlanFilters(cleanPlan, [attentionOnly: 'true'])
        assert MrpEventPlanBuilder.matchesEventPlanFilters(cleanPlan, [attentionOnly: 'false'])
    }

    @Test
    void testEventPlansAttentionTypeFiltersSpecificSignals() {
        Map latePlan = [attentionTypes: ['LATE_EVENT', 'BELOW_SAFETY_STOCK'], actionNeeded: true]
        Map setupPlan = [attentionTypes: ['SETUP_ISSUE'], actionNeeded: true]

        assert MrpEventPlanBuilder.matchesEventPlanFilters(latePlan, [attentionType: 'LATE_EVENT'])
        assert MrpEventPlanBuilder.matchesEventPlanFilters(latePlan, [attentionType: 'BELOW_SAFETY_STOCK'])
        assert !MrpEventPlanBuilder.matchesEventPlanFilters(setupPlan, [attentionType: 'LATE_EVENT'])
    }

    @Test
    void testFrameworkNavigationMetadataAndLinksUseRequestPath() {
        MockHttpServletRequest request = new MockHttpServletRequest()
        request.setRequestURI('/rest/mrp-planning/runs')
        request.setQueryString('pageIndex=1&pageSize=1&statusId=SERVICE_FINISHED')
        RestQueryOptions queryOptions = RestQueryOptions.fromParameters([
                pageIndex: 1,
                pageSize: 1,
                statusId: 'SERVICE_FINISHED'
        ])

        Map result = RestApiUtil.getPagedResult('runs', [], queryOptions, 3L, RestApiUtil.getRelativeRequestPath(request))

        assert result.previousPageCount == 1L
        assert result.nextPageCount == 1L
        assert result.links instanceof Map
        assert result.links.self.href == '/rest/mrp-planning/runs?statusId=SERVICE_FINISHED&pageIndex=1&pageSize=1'
        assert result.links.prev.href == '/rest/mrp-planning/runs?statusId=SERVICE_FINISHED&pageIndex=0&pageSize=1'
        assert result.links.next.href == '/rest/mrp-planning/runs?statusId=SERVICE_FINISHED&pageIndex=2&pageSize=1'
    }

    @Test
    void testComputedMrpListsAcceptMaxFrameworkPageSize() {
        Script eventPlanServices = eventPlanData([pageIndex: 0, pageSize: 100])
        Map eventPlans
        withRecordingEntityQueries([
                MrpRunLog: new RecordingQuery([]),
                MrpEventView: new RecordingQuery([])
        ]) {
            eventPlans = eventPlanServices.findMrpEventPlans()
        }

        assert ServiceUtil.isSuccess(eventPlans)
        assert eventPlans.pageSize == 100
    }

    @Test
    void testComputedMrpListsRejectOversizedFrameworkPageSize() {
        Script eventPlanServices = eventPlanData([pageIndex: 0, pageSize: 101])
        Map eventPlans = eventPlanServices.findMrpEventPlans()

        assert ServiceUtil.isError(eventPlans)
        assert ServiceUtil.getErrorMessage(eventPlans).contains('pageSize must be between 1 and 100')
    }

    @Test
    void testLoadMrpEventsConstrainsDefaultCandidateLoadToLatestMrpIdAndLimit() {
        RecordingQuery latestRunQuery = new RecordingQuery([mrpEvent([mrpId: null]), mrpEvent([mrpId: '10030'])])
        RecordingQuery eventQuery = new RecordingQuery([])
        withRecordingEntityQueries([
                MrpRunLog: latestRunQuery,
                MrpEventView: eventQuery
        ]) {
            MrpEventPlanQuery.loadMrpEventViewRows(null, [:], 1000, false)
        }

        assert latestRunQuery.entityName == 'MrpRunLog'
        assert latestRunQuery.whereCondition.toString().contains('mrpId')
        assert latestRunQuery.maxRowsValue == 25
        assert eventQuery.entityName == 'MrpEventView'
        assert eventQuery.maxRowsValue == 1000
        assert eventQuery.whereCondition.toString().contains('10030')
    }

    @Test
    void testLoadMrpEventsPreservesExplicitMrpIdWhileApplyingLimit() {
        RecordingQuery eventQuery = new RecordingQuery([])
        withRecordingEntityQueries([MrpEventView: eventQuery]) {
            MrpEventPlanQuery.loadMrpEventViewRows(null, [mrpId: '10010'], 500, false)
        }

        assert eventQuery.entityName == 'MrpEventView'
        assert eventQuery.maxRowsValue == 500
        assert eventQuery.whereCondition.toString().contains('10010')
    }

    @Test
    void testLoadMrpEventsCapsDirectCallerCandidateLimit() {
        RecordingQuery eventQuery = new RecordingQuery([])
        withRecordingEntityQueries([MrpEventView: eventQuery]) {
            MrpEventPlanQuery.loadMrpEventViewRows(null, [mrpId: '10010'], 50000, false)
        }

        assert eventQuery.maxRowsValue == 5000
    }

    @Test
    void testLoadMrpEventsUsesMinimumCandidateLimit() {
        RecordingQuery eventQuery = new RecordingQuery([])
        withRecordingEntityQueries([MrpEventView: eventQuery]) {
            MrpEventPlanQuery.loadMrpEventViewRows(null, [mrpId: '10010'], 0, false)
        }

        assert eventQuery.maxRowsValue == 1
    }

    @Test
    void testCandidateEventLimitAccountsForRequestedPageWindow() {
        assert MrpEventPlanQuery.candidateRowLimitForPlanGrouping(0, 10) == 1000
        assert MrpEventPlanQuery.candidateRowLimitForPlanGrouping(2, 10) == 3000
        assert MrpEventPlanQuery.candidateRowLimitForPlanGrouping(10, 100) == 5000
    }

    @Test
    void testEventPlanDetailUsesProductLevelForecastInTimeline() {
        GenericValue forecast = mrpEvent([
                mrpId: '10022',
                productId: 'GZ-8544',
                eventDate: Timestamp.valueOf('2026-07-03 10:00:00.000'),
                mrpEventTypeId: 'SALES_FORECAST',
                facilityId: null,
                quantity: -5G
        ])
        GenericValue warehouseDemand = mrpEvent([
                mrpId: '10022',
                productId: 'GZ-8544',
                eventDate: Timestamp.valueOf('2026-07-03 10:00:00.000'),
                mrpEventTypeId: 'SALES_ORDER_SHIP',
                facilityId: 'WebStoreWarehouse',
                quantity: -3G
        ])
        RecordingQuery eventQuery = new RecordingQuery([forecast, warehouseDemand])
        Map enrichment = emptyEnrichment()
        enrichment.facilities.WebStoreWarehouse = mrpEvent([facilityId: 'WebStoreWarehouse', facilityName: 'Web Store Warehouse'])

        List events
        withRecordingEntityQueries([MrpEventView: eventQuery]) {
            events = MrpEventPlanQuery.loadSelectedPlanEvents(null, [
                    mrpId: '10022',
                    productId: 'GZ-8544',
                    facilityId: 'WebStoreWarehouse'
            ], 5000)
        }
        Map groupContext = MrpEventPlanBuilder.buildWarehouseEventPlanContext('10022|GZ-8544|WebStoreWarehouse', events, enrichment)

        assert eventQuery.whereCondition.toString().contains('WebStoreWarehouse')
        assert groupContext.timeline*.mrpEventTypeId == ['SALES_FORECAST', 'SALES_ORDER_SHIP']
        assert MrpEventPlanBuilder.buildEventPlanResponseRow(groupContext).attentionTypes.contains('FORECAST_ALLOCATION')
    }

    @Test
    void testInitialQohSortsBeforeSameRunSupplyEvenWhenTimestampIsLater() {
        GenericValue proposedPurchase = mrpEvent([
                mrpId: '10022',
                productId: 'GZ-8544',
                eventDate: Timestamp.valueOf('2026-07-02 16:35:25.523'),
                mrpEventTypeId: 'PROP_PUR_O_RECP',
                facilityId: 'WebStoreWarehouse',
                quantity: 50G
        ])
        GenericValue initialQoh = mrpEvent([
                mrpId: '10022',
                productId: 'GZ-8544',
                eventDate: Timestamp.valueOf('2026-07-02 16:35:25.524'),
                mrpEventTypeId: 'INITIAL_QOH',
                facilityId: 'WebStoreWarehouse',
                quantity: 18G
        ])

        assert MrpEventPlanBuilder.compareMrpTimelineEvents(initialQoh, proposedPurchase) < 0
    }

    @Test
    void testFallbackSortKeepsDomainOrderingAndNullsLast() {
        List planContexts = [
                [productId: 'MAT-B', facilityId: 'WH1', mrpId: '10010'],
                [productId: 'MAT-A', facilityId: 'WH2', mrpId: '10010'],
                [productId: 'MAT-A', facilityId: 'WH1', mrpId: '10010']
        ]
        List sortedPlans = RestApiUtil.sortMapRows(planContexts, null, MrpEventPlanBuilder.EVENT_PLAN_SORT_FIELDS,
                MrpEventPlanBuilder.buildEventPlanFallbackComparator())

        assert sortedPlans*.productId == ['MAT-A', 'MAT-A', 'MAT-B']
        assert sortedPlans*.facilityId == ['WH1', 'WH2', 'WH1']
    }

    @Test
    void testEventIdIsStableForMapAndGenericValueInputs() {
        Map event = [
                mrpId: '10022',
                productId: 'GZ-8544',
                eventDate: Timestamp.valueOf('2026-07-02 16:35:25.524'),
                mrpEventTypeId: 'INITIAL_QOH',
                facilityId: 'WebStoreWarehouse'
        ]

        assert MrpEventPlanBuilder.buildTimelineEventId(event) == MrpEventPlanBuilder.buildTimelineEventId(mrpEvent(event))
    }

    @Test
    void testTimelineBalancesStartWithInitialQohEvenWhenSupplyTimestampIsEarlier() {
        GenericValue proposedPurchase = mrpEvent([
                mrpId: '10022',
                productId: 'GZ-8544',
                eventDate: Timestamp.valueOf('2026-07-02 16:35:25.523'),
                mrpEventTypeId: 'PROP_PUR_O_RECP',
                facilityId: 'WebStoreWarehouse',
                quantity: 50G
        ])
        GenericValue initialQoh = mrpEvent([
                mrpId: '10022',
                productId: 'GZ-8544',
                eventDate: Timestamp.valueOf('2026-07-02 16:35:25.524'),
                mrpEventTypeId: 'INITIAL_QOH',
                facilityId: 'WebStoreWarehouse',
                quantity: 18G
        ])

        Map groupContext = MrpEventPlanBuilder.buildWarehouseEventPlanContext('10022|GZ-8544|WebStoreWarehouse',
                [proposedPurchase, initialQoh], emptyEnrichment())

        assert groupContext.timeline*.mrpEventTypeId == ['INITIAL_QOH', 'PROP_PUR_O_RECP']
        assert groupContext.timeline*.runningBalance == [18G, 68G]
        assert MrpEventPlanBuilder.buildEventPlanResponseRow(groupContext).lowestRunningBalance == 18G
    }

    @Test
    void testBuildMrpRunResponseRowReturnsMachineReadableFallbackRunDuration() {
        Script services = runLogData()
        GenericValue runLog = mrpEvent([
                mrpRunLogId: 'MRP_RUN_LOG_10030',
                runStatusId: 'SERVICE_FINISHED',
                startedAt: Timestamp.valueOf('2026-07-02 19:25:00.000'),
                finishedAt: Timestamp.valueOf('2026-07-02 19:25:00.274')
        ])

        assert services.buildMrpRunResponseRow(runLog, null).durationMillis == 274L
    }

    @Test
    void testRunLogMapUsesDurableRunFields() {
        Script services = runLogData()
        GenericValue runLog = mrpEvent([
                mrpRunLogId: 'MRP_RUN_LOG_10030',
                mrpId: '10030',
                jobId: '12204',
                mrpName: 'Thursday Evening Run',
                facilityId: 'WebStoreWarehouse',
                defaultYearsOffset: 1L,
                runByUserLoginId: 'admin',
                runStatusId: 'SERVICE_FINISHED',
                startedAt: Timestamp.valueOf('2026-07-02 19:25:00.000'),
                finishedAt: Timestamp.valueOf('2026-07-02 19:25:00.274'),
                durationMillis: 274L
        ])
        GenericValue status = mrpEvent([
                statusId: 'SERVICE_FINISHED',
                description: 'Finished'
        ])

        Map result = services.buildMrpRunResponseRow(runLog, status)

        assert result.runId == 'MRP_RUN_LOG_10030'
        assert result.mrpId == '10030'
        assert result.jobId == '12204'
        assert result.mrpName == 'Thursday Evening Run'
        assert result.facilityId == 'WebStoreWarehouse'
        assert result.defaultYearsOffset == 1
        assert result.runAsUser == 'admin'
        assert result.statusDescription == 'Finished'
        assert result.durationMillis == 274L
    }

    @Test
    void testRunLogMapExposesFailureDetailsForFailedRuns() {
        Script services = runLogData()
        GenericValue runLog = mrpEvent([
                mrpRunLogId: 'MRP_RUN_LOG_FAILED_10030',
                mrpName: 'Invalid facility run',
                facilityId: 'MissingWarehouse',
                runStatusId: 'SERVICE_FAILED',
                failureReason: 'SERVICE_ERROR',
                failureMessage: 'Facility or manufacturing facility is not available.',
                startedAt: Timestamp.valueOf('2026-07-02 19:25:00.000'),
                finishedAt: Timestamp.valueOf('2026-07-02 19:25:00.274'),
                durationMillis: 274L
        ])
        GenericValue status = mrpEvent([
                statusId: 'SERVICE_FAILED',
                description: 'Failed'
        ])

        Map result = services.buildMrpRunResponseRow(runLog, status)

        assert result.statusId == 'SERVICE_FAILED'
        assert result.statusDescription == 'Failed'
        assert result.failureReason == 'SERVICE_ERROR'
        assert result.failureMessage == 'Facility or manufacturing facility is not available.'
    }

    @Test
    void testCreateMrpRunLogAndTrackerBeforeQueueingMrpDoesNotCreateRunLogWithJobSandboxId() {
        String source = new File('applications/manufacturing/src/main/java/org/apache/ofbiz/manufacturing/mrp/MrpServices.java').text
        String methodBody = source.substring(source.indexOf('public static Map<String, Object> createMrpRunLogAndTrackerBeforeQueueingMrp'),
                source.indexOf('/**\n     * Launch executeMrp'))

        assert methodBody.contains('createMrpRunLogContext.put("mrpRunLogId", mrpRunLogId)')
        assert methodBody.contains('createMrpRunLogContext.put("userLogin", userLogin)')
        assert !methodBody.contains('createMrpRunLogContext.put("jobId"')
    }

    @Test
    void testCreateMrpRunLogAndTrackerBeforeQueueingMrpCreatesQueueTimeRunLogWithoutUpdateBranch() {
        String source = new File('applications/manufacturing/src/main/java/org/apache/ofbiz/manufacturing/mrp/MrpServices.java').text
        String methodBody = source.substring(source.indexOf('public static Map<String, Object> createMrpRunLogAndTrackerBeforeQueueingMrp'),
                source.indexOf('/**\n     * Launch executeMrp'))

        assert methodBody.contains('"createMrpRunLog"')
        assert !methodBody.contains('"updateMrpRunLog"')
    }

    @Test
    void testMrpRunRestEndpointUsesGenericLaunchService() {
        String restApiXml = new File('applications/manufacturing/api/mrp-planning.rest.xml').text
        String controllerXml = new File('applications/manufacturing/webapp/manufacturing/WEB-INF/controller.xml').text
        String requestMap = controllerXml.substring(controllerXml.indexOf('<request-map uri="runMrpGo">'),
                controllerXml.indexOf('</request-map>', controllerXml.indexOf('<request-map uri="runMrpGo">')))

        assert restApiXml.contains('<service name="launchMrpRun"/>')
        assert !restApiXml.contains('<service name="runMrpForPlanning"/>')
        assert requestMap.contains('<event type="service" invoke="launchMrpRun"/>')
        assert !requestMap.contains('invoke="executeMrp" path="async"')
    }

    @Test
    void testMrpReadServicesUseDirectGroovyServiceScripts() {
        String servicesXml = new File('applications/manufacturing/servicedef/services_mrp.xml').text

        assert servicesXml.contains('component://manufacturing/src/main/groovy/org/apache/ofbiz/manufacturing/mrp/MrpEventPlanServices.groovy')
        assert servicesXml.contains('component://manufacturing/src/main/groovy/org/apache/ofbiz/manufacturing/mrp/MrpRunLogServices.groovy')
        assert !servicesXml.contains(['MrpPlanningServices', 'groovy'].join('.'))
        assert !servicesXml.contains(['MrpEventPlanBuilder', 'groovy'].join('.'))
        assert !servicesXml.contains(['MrpRunLogData', 'groovy'].join('.'))
    }

    @Test
    void testMrpRunMutatingServicesOwnCreatePermissionAndInputValidation() {
        String servicesXml = new File('applications/manufacturing/servicedef/services_mrp.xml').text
        String executeServiceXml = servicesXml.substring(servicesXml.indexOf('<service name="executeMrp"'),
                servicesXml.indexOf('<service name="createMrpRunLog"'))
        String prepareServiceXml = servicesXml.substring(servicesXml.indexOf('<service name="createMrpRunLogAndTrackerBeforeQueueingMrp"'),
                servicesXml.indexOf('<service name="launchMrpRun"'))
        String launchServiceXml = servicesXml.substring(servicesXml.indexOf('<service name="launchMrpRun"'),
                servicesXml.indexOf('<service name="initMrpEvents"'))
        String source = new File('applications/manufacturing/src/main/java/org/apache/ofbiz/manufacturing/mrp/MrpServices.java').text
        String methodBody = source.substring(source.indexOf('public static Map<String, Object> launchMrpRun'),
                source.indexOf('private static GenericValue findLaunchedMrpJob'))
        String startRunLogBody = source.substring(source.indexOf('private static String startMrpRunLog'),
                source.indexOf('private static String failureReasonFromResult'))

        assert executeServiceXml.contains('<permission-service service-name="manufacturingPermissionService" main-action="CREATE"/>')
        assert prepareServiceXml.contains('<permission-service service-name="manufacturingPermissionService" main-action="CREATE"/>')
        assert launchServiceXml.contains('<permission-service service-name="manufacturingPermissionService" main-action="CREATE"/>')
        assert methodBody.contains('UtilValidate.isEmpty(facilityId) && UtilValidate.isEmpty(facilityGroupId)')
        assert methodBody.contains('ManufacturingMrpFacilityNotAvailable')
        assert startRunLogBody.contains('"JOB_T_SCHEDULED".equals(jobTracker.getString("statusId"))')
        assert startRunLogBody.contains('"statusId", "JOB_T_RUNNING"')
        assert startRunLogBody.contains('dispatcher.runSync("updateJobTracker", updateJobTrackerContext, 60, true)')
    }

    @Test
    void testRunContextMapUsesDurableRunLog() {
        Script services = runLogData()
        GenericValue runLog = mrpEvent([
                mrpRunLogId: 'MRP_RUN_LOG_10030',
                mrpId: '10030',
                jobId: '12204',
                mrpName: 'Thursday Evening Run',
                facilityId: 'WebStoreWarehouse',
                runByUserLoginId: 'admin',
                statusId: 'SERVICE_FINISHED',
                startedAt: Timestamp.valueOf('2026-07-02 19:25:00.000'),
                finishedAt: Timestamp.valueOf('2026-07-02 19:25:00.274'),
                durationMillis: 274L
        ])
        GenericValue status = mrpEvent([statusId: 'SERVICE_FINISHED', description: 'Finished'])

        Map result = services.buildMrpRunResponseRow(runLog, status)

        assert result.runId == 'MRP_RUN_LOG_10030'
        assert result.mrpId == '10030'
        assert result.jobId == '12204'
        assert result.mrpName == 'Thursday Evening Run'
        assert result.facilityId == 'WebStoreWarehouse'
        assert result.runAsUser == 'admin'
        assert result.statusDescription == 'Finished'
        assert result.durationMillis == 274L
    }

    @Test
    void testEventPlanAttentionSummaryExplainsBelowMinimumStock() {
        GenericValue initialQoh = mrpEvent([
                mrpId: '10022',
                productId: 'GZ-8544',
                eventDate: Timestamp.valueOf('2026-07-02 16:35:25.524'),
                mrpEventTypeId: 'INITIAL_QOH',
                facilityId: 'WebStoreWarehouse',
                quantity: 18G
        ])
        Map enrichment = emptyEnrichment()
        enrichment.productFacilities['GZ-8544|WebStoreWarehouse'] = mrpEvent([
                productId: 'GZ-8544',
                facilityId: 'WebStoreWarehouse',
                minimumStock: 20G,
                reorderQuantity: 50G,
                daysToShip: 1L
        ])

        Map groupContext = MrpEventPlanBuilder.buildWarehouseEventPlanContext('10022|GZ-8544|WebStoreWarehouse', [initialQoh], enrichment)
        Map plan = MrpEventPlanBuilder.buildEventPlanResponseRow(groupContext)

        assert plan.statusDescription == 'Needs attention'
        assert plan.actionNeeded
        assert plan.attentionTypes == ['BELOW_SAFETY_STOCK']
        assert plan.primaryAttentionType == 'BELOW_SAFETY_STOCK'
        assert plan.attentionSummary == 'Current below safety stock'
        assert !plan.containsKey('riskReasonCode')
        assert !plan.containsKey('riskReason')
        assert !plan.containsKey('hasRisk')
    }

    @Test
    void testEventPlanAttentionSummaryExplainsLateSupplyBeforeStockPolicy() {
        GenericValue proposedPurchase = mrpEvent([
                mrpId: '10022',
                productId: 'GZ-8544',
                eventDate: Timestamp.valueOf('2026-07-02 16:35:25.523'),
                mrpEventTypeId: 'PROP_PUR_O_RECP',
                facilityId: 'WebStoreWarehouse',
                quantity: 50G,
                isLate: 'Y'
        ])
        GenericValue initialQoh = mrpEvent([
                mrpId: '10022',
                productId: 'GZ-8544',
                eventDate: Timestamp.valueOf('2026-07-02 16:35:25.524'),
                mrpEventTypeId: 'INITIAL_QOH',
                facilityId: 'WebStoreWarehouse',
                quantity: 18G
        ])
        Map enrichment = emptyEnrichment()
        enrichment.productFacilities['GZ-8544|WebStoreWarehouse'] = mrpEvent([
                productId: 'GZ-8544',
                facilityId: 'WebStoreWarehouse',
                minimumStock: 20G
        ])

        Map groupContext = MrpEventPlanBuilder.buildWarehouseEventPlanContext('10022|GZ-8544|WebStoreWarehouse',
                [proposedPurchase, initialQoh], enrichment)
        Map plan = MrpEventPlanBuilder.buildEventPlanResponseRow(groupContext)

        assert plan.actionNeeded
        assert plan.attentionTypes == ['LATE_EVENT', 'BELOW_SAFETY_STOCK']
        assert plan.primaryAttentionType == 'LATE_EVENT'
        assert plan.attentionSummary == 'Proposed supply is late; Current below safety stock'
        assert !plan.containsKey('riskReasonCode')
        assert !plan.containsKey('riskReason')
    }

    @Test
    void testBuildEventPlanResponseRowIncludesAttentionMetadataForPlannerChips() {
        GenericValue initialQoh = mrpEvent([
                mrpId: '10022',
                productId: 'GZ-8544',
                eventDate: Timestamp.valueOf('2026-07-02 16:35:25.524'),
                mrpEventTypeId: 'INITIAL_QOH',
                facilityId: 'WebStoreWarehouse',
                quantity: 18G,
                isLate: 'N'
        ])
        GenericValue proposedPurchase = mrpEvent([
                mrpId: '10022',
                productId: 'GZ-8544',
                eventDate: Timestamp.valueOf('2026-07-02 16:35:25.525'),
                mrpEventTypeId: 'PROP_PUR_O_RECP',
                facilityId: 'WebStoreWarehouse',
                quantity: 50G,
                isLate: 'Y'
        ])
        Map groupContext = MrpEventPlanBuilder.buildWarehouseEventPlanContext('10022|GZ-8544|WebStoreWarehouse',
                [initialQoh, proposedPurchase], enrichmentWithProductFacility(20G))

        Map plan = MrpEventPlanBuilder.buildEventPlanResponseRow(groupContext)

        assert plan.actionNeeded
        assert plan.attentionSeverity == 'HIGH'
        assert plan.attentionTypes.contains('LATE_EVENT')
        assert plan.attentionTypes.contains('BELOW_SAFETY_STOCK')
        assert plan.primaryAttentionType == 'LATE_EVENT'
        assert plan.attentionSummary.contains('late')
        assert plan.inlineGuidance
    }

    @Test
    void testEventPlanDoesNotNeedAttentionWhenOnTimeProposedSupplyRecoversCurrentBelowMinimum() {
        GenericValue initialQoh = mrpEvent([
                mrpId: '10022',
                productId: 'GZ-8544',
                eventDate: Timestamp.valueOf('2026-07-02 16:35:25.524'),
                mrpEventTypeId: 'INITIAL_QOH',
                facilityId: 'WebStoreWarehouse',
                quantity: 18G,
                isLate: 'N'
        ])
        GenericValue proposedPurchase = mrpEvent([
                mrpId: '10022',
                productId: 'GZ-8544',
                eventDate: Timestamp.valueOf('2026-07-02 16:35:25.525'),
                mrpEventTypeId: 'PROP_PUR_O_RECP',
                facilityId: 'WebStoreWarehouse',
                quantity: 50G,
                isLate: 'N'
        ])
        Map enrichment = enrichmentWithProductFacility(20G)

        Map groupContext = MrpEventPlanBuilder.buildWarehouseEventPlanContext('10022|GZ-8544|WebStoreWarehouse',
                [initialQoh, proposedPurchase], enrichment)
        Map plan = MrpEventPlanBuilder.buildEventPlanResponseRow(groupContext)

        assert plan.statusDescription == 'On track'
        assert !plan.actionNeeded
        assert !plan.attentionTypes.contains('BELOW_SAFETY_STOCK')
        assert !plan.containsKey('riskReasonCode')
        assert !plan.containsKey('riskReason')
    }

    @Test
    void testEventPlanNeedsAttentionWhenCurrentBelowMinimumAndNoProposedSupplyExists() {
        GenericValue initialQoh = mrpEvent([
                mrpId: '10022',
                productId: 'GZ-8544',
                eventDate: Timestamp.valueOf('2026-07-02 16:35:25.524'),
                mrpEventTypeId: 'INITIAL_QOH',
                facilityId: 'WebStoreWarehouse',
                quantity: 18G,
                isLate: 'N'
        ])
        Map enrichment = enrichmentWithProductFacility(20G)

        Map groupContext = MrpEventPlanBuilder.buildWarehouseEventPlanContext('10022|GZ-8544|WebStoreWarehouse', [initialQoh], enrichment)
        Map plan = MrpEventPlanBuilder.buildEventPlanResponseRow(groupContext)

        assert plan.actionNeeded
        assert plan.attentionTypes == ['BELOW_SAFETY_STOCK']
        assert plan.attentionSeverity == 'MEDIUM'
        assert plan.attentionSummary == 'Current below safety stock'
        assert plan.inventoryContext.projectedBalance == 18G
    }

    @Test
    void testEventPlanPrioritizesLateAttentionWhenLateSupplyRecoversBelowMinimum() {
        GenericValue initialQoh = mrpEvent([
                mrpId: '10022',
                productId: 'GZ-8544',
                eventDate: Timestamp.valueOf('2026-07-02 16:35:25.524'),
                mrpEventTypeId: 'INITIAL_QOH',
                facilityId: 'WebStoreWarehouse',
                quantity: 18G,
                isLate: 'N'
        ])
        GenericValue proposedPurchase = mrpEvent([
                mrpId: '10022',
                productId: 'GZ-8544',
                eventDate: Timestamp.valueOf('2026-07-02 16:35:25.525'),
                mrpEventTypeId: 'PROP_PUR_O_RECP',
                facilityId: 'WebStoreWarehouse',
                quantity: 50G,
                isLate: 'Y'
        ])
        Map enrichment = enrichmentWithProductFacility(20G)

        Map groupContext = MrpEventPlanBuilder.buildWarehouseEventPlanContext('10022|GZ-8544|WebStoreWarehouse',
                [initialQoh, proposedPurchase], enrichment)
        Map plan = MrpEventPlanBuilder.buildEventPlanResponseRow(groupContext)

        assert plan.actionNeeded
        assert plan.attentionTypes == ['LATE_EVENT', 'BELOW_SAFETY_STOCK']
        assert plan.primaryAttentionType == 'LATE_EVENT'
        assert plan.attentionSeverity == 'HIGH'
    }

    @Test
    void testEventPlanNeedsAttentionWhenFinalBalanceBelowMinimumAfterPlanDoesNotRecover() {
        GenericValue initialQoh = mrpEvent([
                mrpId: '10022',
                productId: 'GZ-8544',
                eventDate: Timestamp.valueOf('2026-07-02 16:35:25.524'),
                mrpEventTypeId: 'INITIAL_QOH',
                facilityId: 'WebStoreWarehouse',
                quantity: 18G,
                isLate: 'N'
        ])
        GenericValue salesDemand = mrpEvent([
                mrpId: '10022',
                productId: 'GZ-8544',
                eventDate: Timestamp.valueOf('2026-07-03 10:00:00.000'),
                mrpEventTypeId: 'SALES_ORDER_SHIP',
                facilityId: 'WebStoreWarehouse',
                quantity: -5G,
                isLate: 'N'
        ])
        Map enrichment = enrichmentWithProductFacility(20G)

        Map groupContext = MrpEventPlanBuilder.buildWarehouseEventPlanContext('10022|GZ-8544|WebStoreWarehouse',
                [initialQoh, salesDemand], enrichment)
        Map plan = MrpEventPlanBuilder.buildEventPlanResponseRow(groupContext)

        assert plan.actionNeeded
        assert plan.attentionTypes == ['BELOW_SAFETY_STOCK']
        assert plan.attentionSeverity == 'MEDIUM'
        assert plan.attentionSummary == 'Projected below safety stock'
        assert plan.inventoryContext.projectedBalance == 18G
        assert plan.inventoryContext.lowestRunningBalance == 13G
    }

    @Test
    void testEventPlanMarksNegativeRunningBalanceAsHighAttentionSeverity() {
        GenericValue initialQoh = mrpEvent([
                mrpId: '10022',
                productId: 'GZ-8544',
                eventDate: Timestamp.valueOf('2026-07-02 16:35:25.524'),
                mrpEventTypeId: 'INITIAL_QOH',
                facilityId: 'WebStoreWarehouse',
                quantity: 3G,
                isLate: 'N'
        ])
        GenericValue salesDemand = mrpEvent([
                mrpId: '10022',
                productId: 'GZ-8544',
                eventDate: Timestamp.valueOf('2026-07-03 10:00:00.000'),
                mrpEventTypeId: 'SALES_ORDER_SHIP',
                facilityId: 'WebStoreWarehouse',
                quantity: -10G,
                isLate: 'N'
        ])
        Map enrichment = enrichmentWithProductFacility(2G)

        Map groupContext = MrpEventPlanBuilder.buildWarehouseEventPlanContext('10022|GZ-8544|WebStoreWarehouse',
                [initialQoh, salesDemand], enrichment)
        Map plan = MrpEventPlanBuilder.buildEventPlanResponseRow(groupContext)

        assert plan.actionNeeded
        assert plan.attentionTypes == ['BELOW_SAFETY_STOCK']
        assert plan.attentionSeverity == 'HIGH'
        assert plan.inventoryContext.lowestRunningBalance == -7G
    }

    @Test
    void testEventPlanMarksNegativeRunningBalanceEvenWithoutMinimumStockPolicy() {
        GenericValue initialQoh = mrpEvent([
                mrpId: '10022',
                productId: 'GZ-8544',
                eventDate: Timestamp.valueOf('2026-07-02 16:35:25.524'),
                mrpEventTypeId: 'INITIAL_QOH',
                facilityId: 'WebStoreWarehouse',
                quantity: 3G,
                isLate: 'N'
        ])
        GenericValue salesDemand = mrpEvent([
                mrpId: '10022',
                productId: 'GZ-8544',
                eventDate: Timestamp.valueOf('2026-07-03 10:00:00.000'),
                mrpEventTypeId: 'SALES_ORDER_SHIP',
                facilityId: 'WebStoreWarehouse',
                quantity: -10G,
                isLate: 'N'
        ])

        Map groupContext = MrpEventPlanBuilder.buildWarehouseEventPlanContext('10022|GZ-8544|WebStoreWarehouse',
                [initialQoh, salesDemand], emptyEnrichment())
        Map plan = MrpEventPlanBuilder.buildEventPlanResponseRow(groupContext)

        assert plan.actionNeeded
        assert plan.attentionTypes == ['BELOW_SAFETY_STOCK']
        assert plan.attentionSeverity == 'HIGH'
        assert plan.inventoryContext.lowestRunningBalance == -7G
    }

    @Test
    void testPlannerVisibleTimelineRowsHideInternalRequiredMrpMarker() {
        List timeline = [
                [mrpEventTypeId: 'INITIAL_QOH', eventTypeLabel: 'Initial QOH'],
                [mrpEventTypeId: 'REQUIRED_MRP', eventTypeLabel: 'Stock policy trigger'],
                [mrpEventTypeId: 'PROP_PUR_O_RECP', eventTypeLabel: 'Proposed purchase']
        ]

        List plannerVisibleTimelineRows = MrpEventPlanBuilder.plannerVisibleTimelineRows(timeline)

        assert plannerVisibleTimelineRows*.mrpEventTypeId == ['INITIAL_QOH', 'PROP_PUR_O_RECP']
    }

    @Test
    void testProposedSupplyTimelineExposesRequirementReferenceFromEventName() {
        GenericValue proposedPurchase = mrpEvent([
                mrpId: '10022',
                productId: 'GZ-8544',
                eventDate: Timestamp.valueOf('2026-07-02 16:35:25.523'),
                mrpEventTypeId: 'PROP_PUR_O_RECP',
                facilityId: 'WebStoreWarehouse',
                quantity: 50G,
                eventName: '*10026 (2026-07-02 16:35:25.523)*'
        ])

        List timeline = MrpEventPlanBuilder.buildRunningBalanceTimeline([proposedPurchase], emptyEnrichment(), null)

        assert timeline.first().requirementId == '10026'
        assert timeline.first().requirementDate == Timestamp.valueOf('2026-07-02 16:35:25.523')
    }

    @Test
    void testSameRunStockPolicyProposalIsNotMarkedLate() {
        Timestamp runStartedAt = Timestamp.valueOf('2026-07-02 16:35:25.524')
        Timestamp requirementStartDate = Timestamp.valueOf('2026-07-02 16:35:25.523')
        GenericValue stockPolicyTriggerEvent = mrpEvent([
                mrpEventTypeId: 'REQUIRED_MRP',
                quantity: 0G
        ])

        assert !MrpServices.isProposedOrderLate(requirementStartDate, runStartedAt, stockPolicyTriggerEvent)
    }

    @Test
    void testDatedDemandProposalStillMarksLateWhenStartIsBeforeRun() {
        Timestamp runStartedAt = Timestamp.valueOf('2026-07-02 16:35:25.524')
        Timestamp requirementStartDate = Timestamp.valueOf('2026-07-01 16:35:25.524')
        GenericValue salesDemand = mrpEvent([
                mrpEventTypeId: 'SALES_ORDER_SHIP',
                quantity: -2G
        ])

        assert MrpServices.isProposedOrderLate(requirementStartDate, runStartedAt, salesDemand)
    }

    private static Map enrichmentWithProductFacility(BigDecimal minimumStock, BigDecimal reorderQuantity = 50G,
            Long daysToShip = 1L) {
        Map enrichment = emptyEnrichment()
        enrichment.productFacilities['GZ-8544|WebStoreWarehouse'] = mrpEvent([
                productId: 'GZ-8544',
                facilityId: 'WebStoreWarehouse',
                minimumStock: minimumStock,
                reorderQuantity: reorderQuantity,
                daysToShip: daysToShip
        ])
        return enrichment
    }

    private static Script eventPlanData(Map parameters = [:], MockHttpServletRequest request = null) {
        Binding binding = new Binding([delegator: null, parameters: parameters ?: [:]])
        if (request != null) {
            binding.setVariable('request', request)
        }
        return new GroovyShell(MrpPlanningServicesTests.classLoader, binding).parse(
                new File('applications/manufacturing/src/main/groovy/org/apache/ofbiz/manufacturing/mrp/MrpEventPlanServices.groovy'))
    }

    private static Script runLogData(Map parameters = [:], MockHttpServletRequest request = null) {
        Binding binding = new Binding([delegator: null, parameters: parameters ?: [:]])
        if (request != null) {
            binding.setVariable('request', request)
        }
        return new GroovyShell(MrpPlanningServicesTests.classLoader, binding).parse(
                new File('applications/manufacturing/src/main/groovy/org/apache/ofbiz/manufacturing/mrp/MrpRunLogServices.groovy'))
    }

    private static void withRecordingEntityQueries(Map<String, RecordingQuery> queriesByEntityName, Closure work) {
        EntityQuery.metaClass.static.use = { Object delegator ->
            return new RecordingQuerySource(queriesByEntityName)
        }
        try {
            work.call()
        } finally {
            GroovySystem.metaClassRegistry.removeMetaClass(EntityQuery)
        }
    }

    private static GenericValue mrpEvent(Map fields) {
        return new StubGenericValue(fields)
    }

    private static Map emptyEnrichment() {
        return [
                products: [:],
                facilities: [:],
                productFacilities: [:],
                eventTypes: [:],
                statuses: [:]
        ]
    }

    private static class StubGenericValue extends GenericValue {

        private final Map fields

        StubGenericValue(Map fields) {
            this.fields = fields
        }

        @Override
        Object get(String name) {
            return fields[name]
        }

        @Override
        Object get(Object key) {
            return fields[key]
        }

        @Override
        BigDecimal getBigDecimal(String name) {
            Object value = fields[name]
            if (value == null) {
                return null
            }
            return value instanceof BigDecimal ? value : new BigDecimal(value.toString())
        }

        @Override
        String getString(String name) {
            Object value = fields[name]
            return value != null ? value.toString() : null
        }

        @Override
        Timestamp getTimestamp(String name) {
            Object value = fields[name]
            if (value == null) {
                return null
            }
            return value instanceof Timestamp ? value : Timestamp.valueOf(value.toString())
        }

        @Override
        Long getLong(String name) {
            Object value = fields[name]
            if (value == null) {
                return null
            }
            return value instanceof Long ? value : Long.valueOf(value.toString())
        }

        Object getProperty(String name) {
            if (fields.containsKey(name)) {
                return fields[name]
            }
            return super.getProperty(name)
        }

    }

    private static class RecordingQuery {

        final List rows
        String entityName
        List selectFields
        Object whereCondition
        List orderByFields
        Integer maxRowsValue

        RecordingQuery(List rows) {
            this.rows = rows
        }

        RecordingQuery select(String... fields) {
            selectFields = fields as List
            return this
        }

        RecordingQuery where(Object condition) {
            whereCondition = condition
            return this
        }

        RecordingQuery orderBy(String... fields) {
            orderByFields = fields as List
            return this
        }

        RecordingQuery maxRows(int maxRows) {
            maxRowsValue = maxRows
            return this
        }

        @SuppressWarnings('UnusedMethodParameter')
        RecordingQuery cache(boolean useCache) {
            return this
        }

        GenericValue queryFirst() {
            return rows ? rows.first() : null
        }

        List queryList() {
            return rows
        }

    }

    private static class RecordingQuerySource {

        private final Map<String, RecordingQuery> queriesByEntityName

        RecordingQuerySource(Map<String, RecordingQuery> queriesByEntityName) {
            this.queriesByEntityName = queriesByEntityName
        }

        RecordingQuery from(String entityName) {
            RecordingQuery query = queriesByEntityName[entityName]
            assert query != null
            query.entityName = entityName
            return query
        }

    }

}
