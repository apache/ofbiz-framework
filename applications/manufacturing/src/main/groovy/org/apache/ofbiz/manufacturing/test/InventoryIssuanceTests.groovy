package org.apache.ofbiz.manufacturing.test

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.service.testtools.OFBizTestCase
import java.math.RoundingMode
import org.apache.ofbiz.base.util.Debug
import org.apache.ofbiz.base.util.UtilDateTime

class InventoryIssuanceTests extends InventoryIssuanceTestSupport {

    public InventoryIssuanceTests(String name) {
        super(name)
    }

    @Override
    void setUp() {
        super.setUp()
        userLogin = from('UserLogin').where('userLoginId', 'system').queryOne()

        // Target Purge: Clean up only the items and reservations we own.
        delegator.removeByCondition('WorkEffortInvRes', EntityCondition.makeCondition('productId', EntityOperator.LIKE, 'II_%'))
        delegator.removeByCondition('WorkEffortInventoryAssign',
                EntityCondition.makeCondition('inventoryItemId', EntityOperator.LIKE, 'II_%'))
        delegator.removeByCondition('InventoryItemDetail',
                EntityCondition.makeCondition('inventoryItemId', EntityOperator.LIKE, 'II_%'))
        delegator.removeByCondition('InventoryItem',
                EntityCondition.makeCondition('inventoryItemId', EntityOperator.LIKE, 'II_%'))

        ensureProductBackbone()
        ensureFacilityPolicies()
        delegator.clearAllCaches()
        Debug.logInfo('InventoryIssuanceTests: Environment purged for II_% items.', 'InventoryIssuanceTests')
    }

    // --- Category 1 & 2: Basic & Force Issuance ---

    void testS1_1_StrictIssuanceFailure() {
        String facId = 'WH_S1_1'
        storeFacility([facilityId: facId, facilityName: facId, facilityTypeId: 'WAREHOUSE',
            ownerPartyId: 'II_COMPANY', autoReservePrun: 'Y', allowInventoryReallocation: 'Y', reconcilePrunBackorders: 'N'])
        setUpInventory(facId, 'II_MAT_A_COST', 'II_MAT_A_S1_1', 100.0)
        // Request PR for 60 units (Needs 120 MAT_A).
        String weId = setUpProductionRun('II_PROD_MANUF', 60.0, facId)

        // Run in a new transaction so an error-return does NOT poison the outer tx.
        Map result = dispatcher.runSync('issueProductionRunTask', [
                workEffortId: weId, failIfItemsAreNotAvailable: 'Y', userLogin: userLogin
        ], 0, true)
        assert ServiceUtil.isError(result)
    }

    void testS1_2_ForceWithReallocation() {
        String facId = 'WH_S1_2'
        storeFacility([facilityId: facId, facilityName: facId, facilityTypeId: 'WAREHOUSE',
            ownerPartyId: 'II_COMPANY', autoReservePrun: 'Y', allowInventoryReallocation: 'Y', reconcilePrunBackorders: 'N'])
        setUpInventory(facId, 'II_MAT_A_COST', 'II_MAT_A_S1_2', 100.0)
        setUpInventory(facId, 'II_MAT_C_COST', 'II_MAT_C_S1_2', 100.0)
        // Setup Affected reservation: Needs 50 PR -> 100 MAT_A Reserved. (Pool = 100, so Affected reservation gets all).
        String affectedWeId = setUpProductionRun('II_PROD_MANUF', 50.0, facId)
        assertInventoryIntegrity('II_MAT_A_COST', affectedWeId, 0.0, 100.0, 0.0, facId)

        // Setup issuing task: Needs 25 PR -> 50 MAT_A. Pool is empty.
        String issuingWeId = setUpProductionRun('II_PROD_MANUF', 25.0, facId)

        // Action: Force issue. Should reallocate 50 from affected reservation.
        Map result = dispatcher.runSync('issueProductionRunTask', [
                workEffortId: issuingWeId, failIfItemsAreNotAvailable: 'N', userLogin: userLogin
        ])
        assert ServiceUtil.isSuccess(result)

        // Verify integrity: issuing task has 50 issued. Affected reservation has 50 issued, 50 reserved, 0 ATP.
        // ATP is 0 because the 50 remaining reserved units are backordered (QNA=50).
        assertInventoryIntegrity('II_MAT_A_COST', issuingWeId, 50.0, 0.0, 0.0, facId)
        assertInventoryIntegrity('II_MAT_A_COST', affectedWeId, 0.0, 50.0, 0.0, facId)

        // Verify Affected reservation's Backorder
        GenericValue affectedRes = from('WorkEffortInvRes').where('workEffortId', affectedWeId, 'productId', 'II_MAT_A_COST')
                .queryFirst()
        assert scaleQuantity(affectedRes.getBigDecimal('quantityNotAvailable')) == scaleQuantity(50.0) :
                'Affected reservation should be backordered by 50'
    }

    // --- Category 4: Real-time Reconciliation (SECA) ---

    void testS4_1_RealTimeReconciliationOnReceipt() {
        String facId = 'WH_RECON_Y'
        storeFacility([facilityId: facId, facilityName: facId, facilityTypeId: 'WAREHOUSE',
            ownerPartyId: 'II_COMPANY', autoReservePrun: 'Y', allowInventoryReallocation: 'Y', reconcilePrunBackorders: 'Y'])

        setUpInventory(facId, 'II_MAT_C_COST', 'II_MAT_C_RECON', 0.0)
        String weId = setUpProductionRun('II_PROD_MANUF', 20.0, facId)

        GenericValue res = from('WorkEffortInvRes').where('workEffortId', weId, 'productId', 'II_MAT_C_COST').queryFirst()
        assert res != null : 'Reservation should exist'
        assert scaleQuantity(res.getBigDecimal('quantityNotAvailable')) == scaleQuantity(20.0) :
                'Should be 20 backordered initially'
        assert res.inventoryItemId != null : 'Backorder should be anchored to a logical inventory item'

        String newItemId = 'II_MAT_C_RECON_RCPT'
        delegator.create('InventoryItem', [
                inventoryItemId: newItemId, productId: 'II_MAT_C_COST', facilityId: facId,
                inventoryItemTypeId: 'NON_SERIAL_INV_ITEM', ownerPartyId: 'II_COMPANY',
                quantityOnHandTotal: 0.0, availableToPromiseTotal: 0.0, accountingQuantityTotal: 0.0,
                datetimeReceived: UtilDateTime.nowTimestamp()
        ])
        dispatcher.runSync('createInventoryItemDetail', [
                inventoryItemId: newItemId, availableToPromiseDiff: 50.0,
                quantityOnHandDiff: 50.0, accountingQuantityDiff: 50.0, userLogin: userLogin
        ])
        Map balanceResult = dispatcher.runSync('balanceInventoryItems', [inventoryItemId: newItemId, userLogin: userLogin])
        assert ServiceUtil.isSuccess(balanceResult)

        assertInventoryIntegrity('II_MAT_C_COST', weId, 0.0, 20.0, 30.0, facId)

        res = from('WorkEffortInvRes').where('workEffortId', weId, 'productId', 'II_MAT_C_COST').queryFirst()
        assert scaleQuantity(res.getBigDecimal('quantityNotAvailable')) == scaleQuantity(0.0) :
                'Reservation should now be fully available (QNA=0)'
        assert res.inventoryItemId == newItemId : 'Reservation should be tied to the newly received item'
    }

    void testS4_2_ProductionChainAutoSatisfaction() {
        String facId = 'WH_CHAIN_RECON'
        storeFacility([facilityId: facId, facilityName: facId, facilityTypeId: 'WAREHOUSE',
            ownerPartyId: 'II_COMPANY', autoReservePrun: 'Y', allowInventoryReallocation: 'Y', reconcilePrunBackorders: 'Y'])

        setUpInventory(facId, 'II_MAT_E_RAW', 'II_MAT_E_RAW_S4_2', 100.0)
        setUpInventory(facId, 'II_MAT_D_PRODUCED', 'II_MAT_D_RECON', 0.0)

        String prATaskId = setUpProductionRun('II_PROD_CHAIN', 10.0, facId)

        GenericValue resA = from('WorkEffortInvRes').where('workEffortId', prATaskId, 'productId', 'II_MAT_D_PRODUCED').queryFirst()
        assert resA != null : 'Reservation for PR-A should exist'
        assert scaleQuantity(resA.getBigDecimal('quantityNotAvailable')) == scaleQuantity(10.0) :
                'PR-A should be backordered for 10 units'

        String prBTaskId = setUpProductionRun('II_MAT_D_PRODUCED', 15.0, facId)
        GenericValue taskB = from('WorkEffort').where('workEffortId', prBTaskId).queryOne()
        String prBId = taskB.workEffortParentId

        dispatcher.runSync('changeProductionRunTaskStatus', [
            productionRunId: prBId, workEffortId: prBTaskId,
            statusId: 'PRUN_RUNNING', userLogin: userLogin
        ])

        GenericValue resB = from('WorkEffortInvRes').where('workEffortId', prBTaskId, 'productId', 'II_MAT_E_RAW').queryFirst()
        assert resB != null : 'Reservation for PR-B should exist'

        Map issueResult = dispatcher.runSync('issueProductionRunTask', [
            workEffortId: prBTaskId, userLogin: userLogin
        ])
        assert ServiceUtil.isSuccess(issueResult)

        dispatcher.runSync('changeProductionRunTaskStatus', [
            productionRunId: prBId, workEffortId: prBTaskId,
            statusId: 'PRUN_COMPLETED', userLogin: userLogin
        ])

        Map produceResult = dispatcher.runSync('productionRunProduce', [
            workEffortId: prBId, quantity: 15.0, userLogin: userLogin
        ])
        assert ServiceUtil.isSuccess(produceResult)
        String newInventoryItemId = ((List) produceResult.inventoryItemIds)[0]

        resA = from('WorkEffortInvRes').where('workEffortId', prATaskId, 'productId', 'II_MAT_D_PRODUCED').queryFirst()
        assert scaleQuantity(resA.getBigDecimal('quantityNotAvailable')) == scaleQuantity(0.0) :
                'PR-A\'s reservation should now be satisfied (QNA=0)'
        assert resA.inventoryItemId == newInventoryItemId : 'PR-A\'s reservation should be tied to the item produced by PR-B'

        assertInventoryIntegrity('II_MAT_D_PRODUCED', prATaskId, 0.0, 10.0, 5.0, facId)
    }

    // --- Category 3: Backorders & Residuals ---

    void testS3_2_NegativeReservationResolution() {
        String facId = 'WH_S3_2'
        storeFacility([facilityId: facId, facilityName: facId, facilityTypeId: 'WAREHOUSE',
            ownerPartyId: 'II_COMPANY', autoReservePrun: 'Y', allowInventoryReallocation: 'Y', reconcilePrunBackorders: 'N'])

        setUpInventory(facId, 'II_MAT_A_COST', 'II_MAT_A_S3_2', 100.0)
        setUpInventory(facId, 'II_MAT_C_COST', 'II_MAT_C_S3_2', 0.0)
        String weId = setUpProductionRun('II_PROD_MANUF', 20.0, facId)

        GenericValue res = from('WorkEffortInvRes').where('workEffortId', weId, 'productId', 'II_MAT_C_COST').queryFirst()
        assert scaleQuantity(res.getBigDecimal('quantityNotAvailable')) == scaleQuantity(20.0) :
                'Initial state should be 20 backordered'

        dispatcher.runSync('createInventoryItemDetail', [
                inventoryItemId: 'II_MAT_C_S3_2', quantityOnHandDiff: 50.0,
                availableToPromiseDiff: 50.0, accountingQuantityDiff: 50.0, userLogin: userLogin
        ])

        Map result = dispatcher.runSync('issueProductionRunTask', [
                workEffortId: weId, failIfItemsAreNotAvailable: 'N', userLogin: userLogin
        ])
        assert ServiceUtil.isSuccess(result)

        assertInventoryIntegrity('II_MAT_C_COST', weId, 20.0, 0.0, 30.0, facId)
    }

    void testS3_4_ImpactPlanAudit() {
        String facId = 'WH_S3_4'
        storeFacility([facilityId: facId, facilityName: facId, facilityTypeId: 'WAREHOUSE',
            ownerPartyId: 'II_COMPANY', autoReservePrun: 'Y', allowInventoryReallocation: 'Y', reconcilePrunBackorders: 'N'])
        setUpInventory(facId, 'II_MAT_A_COST', 'II_MAT_A_S3_4', 100.0)
        setUpInventory(facId, 'II_MAT_C_COST', 'II_MAT_C_S3_4', 100.0)

        String v1 = setUpProductionRun('II_PROD_MANUF', 12.5, facId)
        setUpProductionRun('II_PROD_MANUF', 12.5, facId)
        setUpProductionRun('II_PROD_MANUF', 25.0, facId)

        String t4 = setUpProductionRun('II_PROD_MANUF', 7.5, facId)

        Map impactResult = dispatcher.runSync('getProductionRunTaskForceIssueImpact', [
            workEffortId: t4, facilityId: facId, productId: 'II_MAT_A_COST', userLogin: userLogin
        ])
        assert ServiceUtil.isSuccess(impactResult)

        List impactList = (List) impactResult.impactList
        List issuePlan = (List) impactResult.inventoryIssuePlan

        BigDecimal totalImpact = impactList.inject(BigDecimal.ZERO) { sum, it -> sum + (it.quantity ?: BigDecimal.ZERO) }
        assert scaleQuantity(totalImpact) == scaleQuantity(15.0) : 'Issuing task should reallocate 15 units'
        assert issuePlan.size() == 0 : 'Pool should have 0 available'

        Map v1Impact = (Map) impactList.find { it.impactedWorkEffortId == v1 }
        assert v1Impact != null : 'Affected reservation 1 should be in the Impact List'
        assert v1Impact.type == 'REALLOCATED' : 'Affected reservation 1 impact type should be REALLOCATED'
    }

    void testS3_5_AffectedReservationSanityGuard() {
        String facId = 'WH_S3_5'
        storeFacility([facilityId: facId, facilityName: facId, facilityTypeId: 'WAREHOUSE',
            ownerPartyId: 'II_COMPANY', autoReservePrun: 'Y', allowInventoryReallocation: 'Y', reconcilePrunBackorders: 'N'])
        setUpInventory(facId, 'II_MAT_A_COST', 'II_MAT_A_S3_5', 20.0)
        setUpInventory(facId, 'II_MAT_C_COST', 'II_MAT_C_S3_5', 100.0)
        String affectedWeId = setUpProductionRun('II_PROD_MANUF', 10.0, facId)
        dispatcher.runSync('reserveWorkEffortInventoryItem', [
            workEffortId: affectedWeId, productId: 'II_MAT_A_COST', facilityId: facId,
            inventoryItemId: null, quantity: 20.0, quantityNotAvailable: 20.0,
            requireInventory: 'N', userLogin: userLogin
        ])

        int resCount = from('WorkEffortInvRes').where('workEffortId', affectedWeId, 'productId', 'II_MAT_A_COST').queryCount()
        assert resCount == 2 : 'Affected reservation should have 2 records (Dirty State)'

        String issuingWeId = setUpProductionRun('II_PROD_MANUF', 5.0, facId)

        List impactList = [[
            productId: 'II_MAT_A_COST', impactType: 'Production Task', impactId: affectedWeId,
            inventoryItemId: 'II_MAT_A_S3_5', quantity: 10.0, type: 'REALLOCATED', impactedWorkEffortId: affectedWeId
        ]]

        Map reallocResult = dispatcher.runSync('reallocateAndIssueInventory', [
                workEffortId: issuingWeId, impactList: impactList, userLogin: userLogin
        ])
        assert ServiceUtil.isSuccess(reallocResult)

        int finalResCount = from('WorkEffortInvRes').where('workEffortId', affectedWeId, 'productId', 'II_MAT_A_COST').queryCount()
        assert finalResCount == 1 : 'Affected reservation should now have exactly 1 record (Sanity Guard worked)'

        GenericValue remainingRes = from('WorkEffortInvRes').where('workEffortId', affectedWeId, 'productId', 'II_MAT_A_COST').queryFirst()
        BigDecimal totalCommited = (remainingRes.getBigDecimal('quantity') ?: BigDecimal.ZERO)
        assert scaleQuantity(totalCommited) == scaleQuantity(20.0) : 'Total commitment should match Requirement (20)'
    }

    void testS3_6_ExplicitReallocationWorkflow() {
        String facId = 'WH_S3_6'
        storeFacility([facilityId: facId, facilityName: facId, facilityTypeId: 'WAREHOUSE',
            ownerPartyId: 'II_COMPANY', autoReservePrun: 'Y', allowInventoryReallocation: 'Y', reconcilePrunBackorders: 'N'])
        setUpInventory(facId, 'II_MAT_A_COST', 'II_MAT_A_S3_6', 100.0)
        setUpInventory(facId, 'II_MAT_C_COST', 'II_MAT_C_S3_6', 100.0)

        String affectedWeId = setUpProductionRun('II_PROD_MANUF', 50.0, facId)
        assertInventoryIntegrity('II_MAT_A_COST', affectedWeId, 0.0, 100.0, 0.0, facId)

        String issuingWeId = setUpProductionRun('II_PROD_MANUF', 25.0, facId)

        Map impactResult = dispatcher.runSync('getProductionRunTaskForceIssueImpact', [
            workEffortId: issuingWeId, facilityId: facId, userLogin: userLogin
        ])
        assert ServiceUtil.isSuccess(impactResult)
        List impactList = (List) impactResult.impactList
        BigDecimal plannedReallocation = impactList.inject(BigDecimal.ZERO) { sum, it -> sum + (it.quantity ?: BigDecimal.ZERO) }
        assert scaleQuantity(plannedReallocation) == scaleQuantity(50.0) : 'Audit should plan to reallocate 50 units'

        Map reallocResult = dispatcher.runSync('reallocateAndIssueInventory', [
            workEffortId: issuingWeId, impactList: impactList, userLogin: userLogin
        ])
        assert ServiceUtil.isSuccess(reallocResult)

        assertInventoryIntegrity('II_MAT_A_COST', issuingWeId, 50.0, 0.0, 0.0, facId)

        List affectedResList = from('WorkEffortInvRes').where('workEffortId', affectedWeId, 'productId', 'II_MAT_A_COST').queryList()
        BigDecimal affectedQna = affectedResList.inject(BigDecimal.ZERO) { sum, it -> sum + (it.getBigDecimal('quantityNotAvailable') ?: 0) }
        assert scaleQuantity(affectedQna) == scaleQuantity(50.0) : 'Affected reservation should be backordered by 50 units'

        GenericValue invItem = from('InventoryItem').where('inventoryItemId', 'II_MAT_A_S3_6').queryOne()
        assert scaleQuantity(invItem.getBigDecimal('quantityOnHandTotal')) == scaleQuantity(50.0) : 'Physical QOH should be 50'
        assert scaleQuantity(invItem.getBigDecimal('accountingQuantityTotal')) == scaleQuantity(50.0) : 'Accounting Total should be 50'
    }

    void testS4_1_ReleaseWithRestore() {
        String facId = 'WH_S4_1'
        storeFacility([facilityId: facId, facilityName: facId, facilityTypeId: 'WAREHOUSE',
            ownerPartyId: 'II_COMPANY', autoReservePrun: 'Y', allowInventoryReallocation: 'Y', reconcilePrunBackorders: 'N'])
        setUpInventory(facId, 'II_MAT_A_COST', 'II_MAT_A_S4_1', 100.0)
        String weId = setUpProductionRun('II_PROD_MANUF', 10.0, facId)

        long countBefore = from('InventoryItemDetail').where('inventoryItemId', 'II_MAT_A_S4_1').queryCount()

        Map result = dispatcher.runSync('releaseProductionRunTaskComponent', [
                workEffortId: weId, productId: 'II_MAT_A_COST',
                inventoryItemId: 'II_MAT_A_S4_1', appendInventoryItemDetail: true, userLogin: userLogin
        ])
        assert ServiceUtil.isSuccess(result)

        long countAfter = from('InventoryItemDetail').where('inventoryItemId', 'II_MAT_A_S4_1').queryCount()
        assert countAfter == countBefore + 1 : 'InventoryItemDetail record should be created for restoration'

        GenericValue res = from('WorkEffortInvRes').where('workEffortId', weId, 'productId', 'II_MAT_A_COST').queryOne()
        assert res == null
    }

    void testS4_2_ReleaseSatisfaction() {
        String weId = setUpProductionRun('II_PROD_MANUF', 10.0)

        GenericValue resRecord = from('WorkEffortInvRes').where('workEffortId', weId, 'productId', 'II_MAT_A_COST').queryFirst()
        assert resRecord != null : 'Reservation should have been created by setUpProductionRun'
        String reservedItemId = resRecord.inventoryItemId

        long countBefore = from('InventoryItemDetail').where('inventoryItemId', reservedItemId).queryCount()

        Map result = dispatcher.runSync('releaseProductionRunTaskComponent', [
                workEffortId: weId, productId: 'II_MAT_A_COST',
                inventoryItemId: reservedItemId, appendInventoryItemDetail: false, userLogin: userLogin
        ])
        assert ServiceUtil.isSuccess(result)

        long countAfter = from('InventoryItemDetail').where('inventoryItemId', reservedItemId).queryCount()
        assert countAfter == countBefore : 'InventoryItemDetail record should NOT be created for satisfaction'

        GenericValue res = from('WorkEffortInvRes').where('workEffortId', weId, 'productId', 'II_MAT_A_COST').queryOne()
        assert res == null
    }

    // --- Category 5: Hardened Policy & Reconciliation ---

    void testS5_1_PolicyGateFail() {
        String itemId = 'II_INV_TRAD_S5_1'
        setUpInventory('WH_TRADITIONAL', 'II_MAT_A_COST', itemId, 100.0)
        setUpInventory('WH_TRADITIONAL', 'II_MAT_C_COST', 'II_INV_TRAD_C_S5_1', 100.0)

        String affectedWeId = setUpProductionRun('II_PROD_MANUF', 50.0, 'WH_TRADITIONAL')
        assertInventoryIntegrity('II_MAT_A_COST', affectedWeId, 0.0, 100.0, 0.0, 'WH_TRADITIONAL')

        String issuingWeId = setUpProductionRun('II_PROD_MANUF', 10.0, 'WH_TRADITIONAL')

        Map result = dispatcher.runSync('issueProductionRunTask', [
                workEffortId: issuingWeId, failIfItemsAreNotAvailable: 'N', userLogin: userLogin
        ], 0, true)

        assert ServiceUtil.isError(result)
        assert ServiceUtil.getErrorMessage(result).contains('forbids inventory reallocation') :
                'Error message should contain reallocation policy violation'
    }

    void testS5_2_APIPolicyEnforcement() {
        String itemId = 'II_INV_TRAD_S5_2'
        String issuingWeId = setUpProductionRun('II_PROD_MANUF', 10.0, 'WH_TRADITIONAL')
        setUpInventory('WH_TRADITIONAL', 'II_MAT_A_COST', itemId, 100.0)
        setUpInventory('WH_TRADITIONAL', 'II_MAT_C_COST', 'II_INV_TRAD_C_S5_2', 100.0)

        List impactList = [[
            productId: 'II_MAT_A_COST', impactType: 'Production Task', impactId: 'SOME_AFFECTED_TASK',
            inventoryItemId: itemId, quantity: 10.0, type: 'REALLOCATED', impactedWorkEffortId: 'SOME_AFFECTED_TASK'
        ]]

        Map reallocResult = dispatcher.runSync('reallocateAndIssueInventory', [
                workEffortId: issuingWeId, impactList: impactList, facilityId: 'WH_TRADITIONAL', userLogin: userLogin
        ], 0, true)

        assert ServiceUtil.isError(reallocResult)
        assert reallocResult.errorMessage.contains('Policy Violation')
    }

    void testS5_3_NightlyReconciliation() {
        String itemIdA = 'II_INV_FLUID_A_S5_3'
        String itemIdC = 'II_INV_FLUID_C_S5_3'
        setUpInventory('WH_FLUID', 'II_MAT_A_COST', itemIdA, 100.0)
        setUpInventory('WH_FLUID', 'II_MAT_C_COST', itemIdC, 0.0)
        String affectedWeId = setUpProductionRun('II_PROD_MANUF', 10.0, 'WH_FLUID')
        dispatcher.runSync('reserveWorkEffortInventoryItem', [
            workEffortId: affectedWeId, productId: 'II_MAT_A_COST', facilityId: 'WH_FLUID',
            inventoryItemId: itemIdA, quantity: 20.0, quantityNotAvailable: 20.0,
            requireInventory: 'N', userLogin: userLogin
        ])

        String backorderWeId = setUpProductionRun('II_PROD_MANUF', 5.0, 'WH_FLUID')
        dispatcher.runSync('issueProductionRunTask', [workEffortId: backorderWeId, userLogin: userLogin])

        dispatcher.runSync('createInventoryItemDetail', [
                inventoryItemId: itemIdC, quantityOnHandDiff: 50.0,
                availableToPromiseDiff: 50.0, accountingQuantityDiff: 50.0, userLogin: userLogin
        ])

        dispatcher.runSync('reconcileInventoryForProductionJobs', [userLogin: userLogin])

        GenericValue resA = from('WorkEffortInvRes').where('workEffortId', affectedWeId, 'productId', 'II_MAT_A_COST').queryFirst()
        assert resA != null : 'Auditor reservation should exist'
        assert scaleQuantity(resA.getBigDecimal('quantityNotAvailable')) == scaleQuantity(0.0) :
                'Auditor should have purged phantom debt'

        GenericValue boRes = from('WorkEffortInvRes').where('workEffortId', backorderWeId, 'productId', 'II_MAT_C_COST').queryFirst()
        assert boRes != null : 'Satisfier reservation should exist'
        assert scaleQuantity(boRes.getBigDecimal('quantityNotAvailable')) == scaleQuantity(0.0) :
                'Satisfier should have settled the debt'
        assert scaleQuantity(boRes.getBigDecimal('quantity')) == scaleQuantity(5.0) :
                'Physical reservation should now be 5'
        assert (boRes.getBigDecimal('quantityNotAvailable') ?: BigDecimal.ZERO) == BigDecimal.ZERO
    }

    void testS5_4_ReconFluidPolicy() {
        String itemId = 'II_INV_FLUID_A_S5_4'
        setUpInventory('WH_FLUID', 'II_MAT_A_COST', itemId, 5.0)
        setUpInventory('WH_FLUID', 'II_MAT_C_COST', 'II_INV_FLUID_C_S5_4', 100.0)
        setUpInventory('WH_FLUID', 'II_MAT_A_COST', 'II_INV_FLUID_B_S5_4', 0.0, false)

        String affectedWeId = setUpProductionRun('II_PROD_MANUF', 5.0, 'WH_FLUID')

        Map issueAffectedResult = dispatcher.runSync('issueProductionRunTaskComponent', [
            workEffortId: affectedWeId, productId: 'II_MAT_A_COST', quantity: 5.0,
            failIfItemsAreNotAvailable: 'N', userLogin: userLogin
        ])
        assert ServiceUtil.isSuccess(issueAffectedResult)

        dispatcher.runSync('reserveWorkEffortInventoryItem', [
            workEffortId: affectedWeId, productId: 'II_MAT_A_COST', facilityId: 'WH_FLUID',
            inventoryItemId: itemId, quantity: 7.0, quantityNotAvailable: 7.0,
            requireInventory: 'N', userLogin: userLogin
        ])

        String boWeId = setUpProductionRun('II_PROD_MANUF', 5.0, 'WH_FLUID')

        dispatcher.runSync('reconcileInventoryForProductionJobs', [userLogin: userLogin])

        List resList = from('WorkEffortInvRes').where('workEffortId', affectedWeId).queryList()
        BigDecimal totalQna = resList.inject(BigDecimal.ZERO) { sum, it -> sum + (it.getBigDecimal('quantityNotAvailable') ?: 0) }
        assert scaleQuantity(totalQna) == scaleQuantity(5.0) : 'Aggressive Auditor should have purged excess logical debt to match estimate ceiling'

        dispatcher.runSync('createInventoryItemDetail', [
                inventoryItemId: 'II_INV_FLUID_B_S5_4', quantityOnHandDiff: 20.0,
                availableToPromiseDiff: 20.0, accountingQuantityDiff: 20.0, userLogin: userLogin
        ])
        dispatcher.runSync('reconcileInventoryForProductionJobs', [userLogin: userLogin])

        GenericValue boRes = from('WorkEffortInvRes').where('workEffortId', boWeId, 'productId', 'II_MAT_A_COST').queryFirst()
        assert boRes != null : 'Backorder reservation should exist for boWeId'
        assert scaleQuantity(boRes.getBigDecimal('quantityNotAvailable')) == scaleQuantity(0.0) :
                'Satisfier should have settled the debt in Fluid Warehouse'
    }

    void testS5_5_ReconTraditionalPolicy() {
        String itemId = 'II_INV_TRAD_A_S5_5'
        setUpInventory('WH_TRADITIONAL', 'II_MAT_A_COST', itemId, 20.0)
        setUpInventory('WH_TRADITIONAL', 'II_MAT_C_COST', 'II_INV_TRAD_C_S5_5', 100.0)
        setUpInventory('WH_TRADITIONAL', 'II_MAT_A_COST', 'II_INV_TRAD_B_S5_5', 0.0, false)

        String weId = setUpProductionRun('II_PROD_MANUF', 5.0, 'WH_TRADITIONAL')

        Map issueScrapResult = dispatcher.runSync('issueProductionRunTaskComponent', [
            workEffortId: weId, productId: 'II_MAT_A_COST', quantity: 12.0,
            failIfItemsAreNotAvailable: 'Y', userLogin: userLogin
        ])
        assert ServiceUtil.isSuccess(issueScrapResult)

        dispatcher.runSync('reserveWorkEffortInventoryItem', [
            workEffortId: weId, productId: 'II_MAT_A_COST', facilityId: 'WH_TRADITIONAL',
            inventoryItemId: itemId, quantity: 2.0, quantityNotAvailable: 2.0,
            requireInventory: 'N', userLogin: userLogin
        ])

        String boWeId = setUpProductionRun('II_PROD_MANUF', 5.0, 'WH_TRADITIONAL')
        dispatcher.runSync('issueProductionRunTask', [workEffortId: boWeId, userLogin: userLogin])
        dispatcher.runSync('createInventoryItemDetail', [
                inventoryItemId: 'II_INV_TRAD_B_S5_5', quantityOnHandDiff: 20.0,
                availableToPromiseDiff: 20.0, accountingQuantityDiff: 20.0, userLogin: userLogin
        ])

        dispatcher.runSync('reconcileInventoryForProductionJobs', [userLogin: userLogin])

        GenericValue res = from('WorkEffortInvRes').where('workEffortId', weId, 'productId', 'II_MAT_A_COST',
                'inventoryItemId', itemId).queryFirst()
        assert res != null : 'Reservation record should exist'
        assert scaleQuantity(res.getBigDecimal('quantityNotAvailable')) == scaleQuantity(2.0) :
                'Conservative Auditor should have spared the scrap backorder'

        List boResList = from('WorkEffortInvRes').where('workEffortId', boWeId, 'productId', 'II_MAT_A_COST').queryList()
        assert !boResList.isEmpty() : 'Reservation should exist for boWeId'
        BigDecimal boQna = boResList.inject(BigDecimal.ZERO) { sum, it -> sum + (it.getBigDecimal('quantityNotAvailable') ?: 0) }
        assert scaleQuantity(boQna) == scaleQuantity(2.0) : 'Satisfier should NOT have settled in Traditional Warehouse'
    }

    void testS5_6_ReconSafePolicy() {
        setUpInventory('WH_SAFE', 'II_MAT_A_COST', 'INV_SAFE_A', 20.0)
        setUpInventory('WH_SAFE', 'II_MAT_C_COST', 'INV_SAFE_C_HEAL', 0.0)
        String weId = setUpProductionRun('II_PROD_MANUF', 5.0, 'WH_SAFE')
        Map issueScrapResult = dispatcher.runSync('issueProductionRunTaskComponent', [
            workEffortId: weId, productId: 'II_MAT_A_COST', quantity: 12.0,
            failIfItemsAreNotAvailable: 'Y', userLogin: userLogin
        ])
        assert ServiceUtil.isSuccess(issueScrapResult)
        dispatcher.runSync('reserveWorkEffortInventoryItem', [
            workEffortId: weId, productId: 'II_MAT_A_COST', facilityId: 'WH_SAFE',
            inventoryItemId: 'INV_SAFE_A', quantity: 5.0, quantityNotAvailable: 5.0,
            requireInventory: 'N', userLogin: userLogin
        ])

        String boWeId = setUpProductionRun('II_PROD_MANUF', 5.0, 'WH_SAFE')
        dispatcher.runSync('issueProductionRunTask', [workEffortId: boWeId, userLogin: userLogin])
        dispatcher.runSync('createInventoryItemDetail', [
                inventoryItemId: 'INV_SAFE_C_HEAL', quantityOnHandDiff: 20.0,
                availableToPromiseDiff: 20.0, accountingQuantityDiff: 20.0, userLogin: userLogin
        ])

        dispatcher.runSync('reconcileInventoryForProductionJobs', [userLogin: userLogin])

        List resList = from('WorkEffortInvRes').where('workEffortId', weId, 'productId', 'II_MAT_A_COST').queryList()
        BigDecimal totalQna = resList.inject(BigDecimal.ZERO) { sum, it -> sum + (it.getBigDecimal('quantityNotAvailable') ?: 0) }
        assert scaleQuantity(totalQna) == scaleQuantity(0.0) : 'Safe Mode (Aggressive Auditor) should have purged the excess'

        List boResList = from('WorkEffortInvRes').where('workEffortId', boWeId, 'productId', 'II_MAT_C_COST').queryList()
        BigDecimal boQna = boResList.inject(BigDecimal.ZERO) { sum, it -> sum + (it.getBigDecimal('quantityNotAvailable') ?: 0) }
        assert scaleQuantity(boQna) == scaleQuantity(0.0) : 'Safe Mode (Conservative Satisfier) should have healed backorder'
    }

    void testM1_ManualLotReallocationSuccess() {
        setUpInventory('WH_FLUID', 'II_MAT_A_COST', 'II_MAN_FLUID', 10.0)
        GenericValue item = from('InventoryItem').where('inventoryItemId', 'II_MAN_FLUID').queryOne()
        item.set('locationSeqId', 'FLOC_01')
        item.set('lotId', 'LOT_MANUAL_01')
        item.store()

        dispatcher.runSync('reserveWorkEffortInventoryItem', [
            workEffortId: 'WE_AFFECTED', productId: 'II_MAT_A_COST', facilityId: 'WH_FLUID',
            inventoryItemId: 'II_MAN_FLUID', quantity: 10.0, userLogin: userLogin
        ])

        Map result = dispatcher.runSync('issueProductionRunTaskComponent', [
            workEffortId: 'WE_ISSUING_FLUID', productId: 'II_MAT_A_COST',
            lotId: 'LOT_MANUAL_01', quantity: 5.0, failIfItemsAreNotAvailable: 'N', userLogin: userLogin
        ])
        assert ServiceUtil.isSuccess(result)

        assertInventoryIntegrity('II_MAT_A_COST', 'WE_ISSUING_FLUID', 5.0, 0.0, 0.0, 'WH_FLUID')
        GenericValue affectedRes = from('WorkEffortInvRes').where(workEffortId: 'WE_AFFECTED').queryFirst()
        assert scaleQuantity(affectedRes.getBigDecimal('quantityNotAvailable')) == scaleQuantity(5.0) :
                'Affected reservation should be backordered by 5'
    }

    void testM2_ManualForceNegative() {
        setUpInventory('WH_FLUID', 'II_MAT_A_COST', 'II_MAN_FLUID', 0.0)

        Map result = dispatcher.runSync('issueProductionRunTaskComponent', [
            workEffortId: 'WE_ISSUING_FLUID', productId: 'II_MAT_A_COST',
            inventoryItemId: 'II_MAN_FLUID', quantity: 10.0,
            failIfItemsAreNotAvailable: 'N', failIfItemsAreNotOnHand: 'N', userLogin: userLogin
        ])
        assert ServiceUtil.isSuccess(result)

        GenericValue item = from('InventoryItem').where(inventoryItemId: 'II_MAN_FLUID').queryOne()
        assert scaleQuantity(item.getBigDecimal('quantityOnHandTotal')) == scaleQuantity(-10.0) : 'QOH should be -10'
    }

    void testM3_UserStrictnessOverridesFacilityFluidity() {
        setUpInventory('WH_FLUID', 'II_MAT_A_COST', 'II_MAN_FLUID', 10.0)
        GenericValue item = from('InventoryItem').where('inventoryItemId', 'II_MAN_FLUID').queryOne()
        item.set('lotId', 'LOT_MANUAL_01')
        item.store()
        dispatcher.runSync('reserveWorkEffortInventoryItem', [
            workEffortId: 'WE_AFFECTED', productId: 'II_MAT_A_COST', facilityId: 'WH_FLUID',
            inventoryItemId: 'II_MAN_FLUID', quantity: 10.0, userLogin: userLogin
        ])

        Map result = dispatcher.runSync('issueProductionRunTaskComponent', [
            workEffortId: 'WE_ISSUING_FLUID', productId: 'II_MAT_A_COST',
            lotId: 'LOT_MANUAL_01', quantity: 5.0, failIfItemsAreNotAvailable: 'Y', userLogin: userLogin
        ], 0, true)

        assert ServiceUtil.isError(result)
        String error = ServiceUtil.getErrorMessage(result)
        assert error.contains('Materials Not Available')
                || error.contains('shortfall')
                || error.contains('promised to other tasks') :
                "Error should contain 'Materials Not Available' or 'promised to other tasks'. Got: ${error}"
    }

    void testM4_FacilityStrictnessOverridesUserFluidity() {
        setUpInventory('WH_TRADITIONAL', 'II_MAT_A_COST', 'II_MAN_STRICT', 10.0)
        GenericValue item = from('InventoryItem').where('inventoryItemId', 'II_MAN_STRICT').queryOne()
        item.set('locationSeqId', 'TLOC_01')
        item.store()
        dispatcher.runSync('reserveWorkEffortInventoryItem', [
            workEffortId: 'WE_AFFECTED', productId: 'II_MAT_A_COST', facilityId: 'WH_TRADITIONAL',
            inventoryItemId: 'II_MAN_STRICT', quantity: 10.0, userLogin: userLogin
        ])

        Map result = dispatcher.runSync('issueProductionRunTaskComponent', [
            workEffortId: 'WE_ISSUING_STRICT', productId: 'II_MAT_A_COST',
            locationSeqId: 'TLOC_01', quantity: 5.0, failIfItemsAreNotAvailable: 'N', userLogin: userLogin
        ], 0, true)

        assert ServiceUtil.isError(result)
        String error = ServiceUtil.getErrorMessage(result)
        assert error.contains('Materials Not Available')
                || error.contains('shortfall')
                || error.contains('forbids inventory reallocation') :
                "Error should contain 'Materials Not Available' or 'forbids inventory reallocation'. Got: ${error}"
    }

    void testM6_SelfConsumptionSuccessInStrictMode() {
        setUpInventory('WH_TRADITIONAL', 'II_MAT_A_COST', 'II_MAN_STRICT', 10.0)

        dispatcher.runSync('reserveWorkEffortInventoryItem', [
            workEffortId: 'WE_ISSUING_STRICT', productId: 'II_MAT_A_COST', facilityId: 'WH_TRADITIONAL',
            inventoryItemId: 'II_MAN_STRICT', quantity: 10.0, userLogin: userLogin
        ])

        Map result = dispatcher.runSync('issueProductionRunTaskComponent', [
            workEffortId: 'WE_ISSUING_STRICT', productId: 'II_MAT_A_COST',
            inventoryItemId: 'II_MAN_STRICT', quantity: 5.0, failIfItemsAreNotAvailable: 'Y', userLogin: userLogin
        ])

        assert ServiceUtil.isSuccess(result)
        assertInventoryIntegrity('II_MAT_A_COST', 'WE_ISSUING_STRICT', 5.0, 5.0, 0.0, 'WH_TRADITIONAL')
    }

    void testS5_7_NullPolicyDefaultsToStrict() {
        String facId = 'WH_NULL_POLICY'
        storeFacility([
            facilityId: facId, facilityName: facId, facilityTypeId: 'WAREHOUSE',
            ownerPartyId: 'II_COMPANY', autoReservePrun: 'Y'
        ])

        String issuingWeId = setUpProductionRun('II_PROD_MANUF', 10.0, facId)

        Map impactResult = dispatcher.runSync('getProductionRunTaskForceIssueImpact', [
            workEffortId: issuingWeId, facilityId: facId, productId: 'II_MAT_A_COST', userLogin: userLogin
        ])

        assert ServiceUtil.isSuccess(impactResult)
        assert impactResult.policyViolation != null : 'Should contain a policyViolation message'
        assert impactResult.policyViolation.contains('forbidden') : 'Message should explain that reallocation is forbidden'
    }

    void testFacilityPolicyPersistence() {
        String facId = 'WH_POLICY_TEST'
        storeFacility([
            facilityId: facId, facilityName: facId, facilityTypeId: 'WAREHOUSE',
            ownerPartyId: 'II_COMPANY', autoReservePrun: 'Y',
            allowInventoryReallocation: 'Y', reconcilePrunBackorders: 'Y'
        ])

        GenericValue facility = from('Facility').where('facilityId', facId).queryOne()
        assert facility != null : 'Facility should be created'
        assert facility.allowInventoryReallocation == 'Y' : 'allowInventoryReallocation should be Y'
        assert facility.reconcilePrunBackorders == 'Y' : 'reconcilePrunBackorders should be Y'

        facility.set('allowInventoryReallocation', 'N')
        facility.set('reconcilePrunBackorders', 'N')
        facility.store()

        facility.refresh()
        assert facility.allowInventoryReallocation == 'N' : 'allowInventoryReallocation should be N after update'
        assert facility.reconcilePrunBackorders == 'N' : 'reconcilePrunBackorders should be N after update'
        assert facility.allowInventoryReallocation == 'N'
    }

}

class InventoryIssuancePolicyMatrixTests extends InventoryIssuanceTestSupport {

    InventoryIssuancePolicyMatrixTests(String name) {
        super(name)
    }

    @Override
    void setUp() {
        super.setUp()
        userLogin = from('UserLogin').where('userLoginId', 'system').queryOne()

        delegator.removeByCondition('WorkEffortInvRes', EntityCondition.makeCondition('productId', EntityOperator.LIKE, 'II_%'))
        delegator.removeByCondition('WorkEffortInventoryAssign',
                EntityCondition.makeCondition('inventoryItemId', EntityOperator.LIKE, 'II_%'))
        delegator.removeByCondition('InventoryItemDetail',
                EntityCondition.makeCondition('inventoryItemId', EntityOperator.LIKE, 'II_%'))
        delegator.removeByCondition('InventoryItem',
                EntityCondition.makeCondition('inventoryItemId', EntityOperator.LIKE, 'II_%'))

        ensureProductBackbone()
        ensureFacilityPolicies()
        delegator.clearAllCaches()
        Debug.logInfo('InventoryIssuancePolicyMatrixTests: Environment purged for II_% items.',
                'InventoryIssuancePolicyMatrixTests')
    }

    void testS6_1_ManualReserve_IssueDefault_Success() {
        String weId = setUpNoAutoReserveProductionRun('II_S6_1', 100.0)
        dispatcher.runSync('reserveWorkEffortInventoryItem', [
            workEffortId: weId, productId: 'II_MAT_A_COST', facilityId: 'WH_NO_AUTO_RES',
            inventoryItemId: 'II_S6_1', quantity: 20.0, userLogin: userLogin
        ])

        Map result = dispatcher.runSync('issueProductionRunTask', [workEffortId: weId, userLogin: userLogin])
        assert ServiceUtil.isSuccess(result)
        assertInventoryIntegrity('II_MAT_A_COST', weId, 20.0, 0.0, 80.0, 'WH_NO_AUTO_RES')
    }

    void testS6_2_ManualReserve_IssueFailIfAvail_Y_Success() {
        String weId = setUpNoAutoReserveProductionRun('II_S6_2', 100.0)
        dispatcher.runSync('reserveWorkEffortInventoryItem', [
            workEffortId: weId, productId: 'II_MAT_A_COST', facilityId: 'WH_NO_AUTO_RES',
            inventoryItemId: 'II_S6_2', quantity: 20.0, userLogin: userLogin
        ])

        Map result = dispatcher.runSync('issueProductionRunTask', [
            workEffortId: weId, failIfItemsAreNotAvailable: 'Y', userLogin: userLogin
        ])
        assert ServiceUtil.isSuccess(result)
        assertInventoryIntegrity('II_MAT_A_COST', weId, 20.0, 0.0, 80.0, 'WH_NO_AUTO_RES')
    }

    void testS6_3_ManualReserve_IssueFailIfAvail_N_Success() {
        String weId = setUpNoAutoReserveProductionRun('II_S6_3', 100.0)
        dispatcher.runSync('reserveWorkEffortInventoryItem', [
            workEffortId: weId, productId: 'II_MAT_A_COST', facilityId: 'WH_NO_AUTO_RES',
            inventoryItemId: 'II_S6_3', quantity: 20.0, userLogin: userLogin
        ])

        Map result = dispatcher.runSync('issueProductionRunTask', [
            workEffortId: weId, failIfItemsAreNotAvailable: 'N', failIfItemsAreNotOnHand: 'N', userLogin: userLogin
        ])
        assert ServiceUtil.isSuccess(result)
        assertInventoryIntegrity('II_MAT_A_COST', weId, 20.0, 0.0, 80.0, 'WH_NO_AUTO_RES')
    }

    void testS6_4_NoReserve_DirectIssueDefault_Success() {
        String weId = setUpNoAutoReserveProductionRun('II_S6_4', 100.0)

        Map result = dispatcher.runSync('issueProductionRunTask', [workEffortId: weId, userLogin: userLogin])
        assert ServiceUtil.isSuccess(result)
        assertInventoryIntegrity('II_MAT_A_COST', weId, 20.0, 0.0, 80.0, 'WH_NO_AUTO_RES')
    }

    void testS6_5_NoReserve_DirectIssueFailIfAvail_Y_Success() {
        String weId = setUpNoAutoReserveProductionRun('II_S6_5', 100.0)

        Map result = dispatcher.runSync('issueProductionRunTask', [
            workEffortId: weId, failIfItemsAreNotAvailable: 'Y', userLogin: userLogin
        ])
        assert ServiceUtil.isSuccess(result)
        assertInventoryIntegrity('II_MAT_A_COST', weId, 20.0, 0.0, 80.0, 'WH_NO_AUTO_RES')
    }

    void testS6_6_NoReserve_DirectIssueFailIfAvail_N_Success() {
        String weId = setUpNoAutoReserveProductionRun('II_S6_6', 100.0)

        Map result = dispatcher.runSync('issueProductionRunTask', [
            workEffortId: weId, failIfItemsAreNotAvailable: 'N', failIfItemsAreNotOnHand: 'N', userLogin: userLogin
        ])
        assert ServiceUtil.isSuccess(result)
        assertInventoryIntegrity('II_MAT_A_COST', weId, 20.0, 0.0, 80.0, 'WH_NO_AUTO_RES')
    }

    void testS6_7_Insufficient_ManualReserve_IssueDefault_Fail() {
        String weId = setUpNoAutoReserveProductionRun('II_S6_7', 0.0)
        dispatcher.runSync('reserveWorkEffortInventoryItem', [
            workEffortId: weId, productId: 'II_MAT_A_COST', facilityId: 'WH_NO_AUTO_RES',
            inventoryItemId: 'II_S6_7', quantity: 20.0, userLogin: userLogin
        ])

        Map result = dispatcher.runSync('issueProductionRunTask', [workEffortId: weId, userLogin: userLogin], 0, true)
        assert ServiceUtil.isError(result)
    }

    void testS6_8_Insufficient_ManualReserve_IssueFailIfAvail_Y_Fail() {
        String weId = setUpNoAutoReserveProductionRun('II_S6_8', 0.0)
        dispatcher.runSync('reserveWorkEffortInventoryItem', [
            workEffortId: weId, productId: 'II_MAT_A_COST', facilityId: 'WH_NO_AUTO_RES',
            inventoryItemId: 'II_S6_8', quantity: 20.0, userLogin: userLogin
        ])

        Map result = dispatcher.runSync('issueProductionRunTask', [
            workEffortId: weId, failIfItemsAreNotAvailable: 'Y', userLogin: userLogin
        ], 0, true)
        assert ServiceUtil.isError(result)
    }

    void testS6_9_Insufficient_ManualReserve_IssueFailIfAvail_N_Success() {
        String weId = setUpNoAutoReserveProductionRun('II_S6_9', 0.0)
        dispatcher.runSync('reserveWorkEffortInventoryItem', [
            workEffortId: weId, productId: 'II_MAT_A_COST', facilityId: 'WH_NO_AUTO_RES',
            inventoryItemId: 'II_S6_9', quantity: 20.0, userLogin: userLogin
        ])

        Map result = dispatcher.runSync('issueProductionRunTask', [
            workEffortId: weId, failIfItemsAreNotAvailable: 'N', failIfItemsAreNotOnHand: 'N', userLogin: userLogin
        ])
        assert ServiceUtil.isSuccess(result)
        assertInventoryIntegrity('II_MAT_A_COST', weId, 20.0, 0.0, -20.0, 'WH_NO_AUTO_RES')
    }

    void testS6_10_Insufficient_NoReserve_DirectIssueDefault_Fail() {
        String weId = setUpNoAutoReserveProductionRun('II_S6_10', 0.0)
        Map result = dispatcher.runSync('issueProductionRunTask', [workEffortId: weId, userLogin: userLogin], 0, true)
        assert ServiceUtil.isError(result)
    }

    void testS6_11_Insufficient_NoReserve_DirectIssueFailIfAvail_Y_Fail() {
        String weId = setUpNoAutoReserveProductionRun('II_S6_11', 0.0)
        Map result = dispatcher.runSync('issueProductionRunTask', [
            workEffortId: weId, failIfItemsAreNotAvailable: 'Y', userLogin: userLogin
        ], 0, true)
        assert ServiceUtil.isError(result)
    }

    void testS6_12_Insufficient_NoReserve_DirectIssueFailIfAvail_N_Success() {
        String weId = setUpNoAutoReserveProductionRun('II_S6_12', 0.0)
        Map result = dispatcher.runSync('issueProductionRunTask', [
            workEffortId: weId, failIfItemsAreNotAvailable: 'N', failIfItemsAreNotOnHand: 'N', userLogin: userLogin
        ])
        assert ServiceUtil.isSuccess(result)
        assertInventoryIntegrity('II_MAT_A_COST', weId, 20.0, 0.0, -20.0, 'WH_NO_AUTO_RES')
    }

}

class InventoryIssuanceTestSupport extends OFBizTestCase {

    protected GenericValue userLogin

    protected InventoryIssuanceTestSupport(String name) {
        super(name)
    }

    protected void storeFacility(Map fields) {
        GenericValue gv = delegator.makeValue('Facility', fields)
        delegator.createOrStore(gv)
    }

    protected void storeProduct(Map fields) {
        GenericValue gv = delegator.makeValue('Product', fields)
        delegator.createOrStore(gv)
    }

    protected void storeProductAssoc(Map fields) {
        GenericValue gv = delegator.makeValue('ProductAssoc', fields)
        delegator.createOrStore(gv)
    }

    protected void ensureProductBackbone() {
        storeProduct([productId: 'II_MAT_D_PRODUCED', productTypeId: 'FINISHED_GOOD',
            isVirtual: 'N', isVariant: 'N', internalName: 'Produced Material D'])
        storeProduct([productId: 'II_MAT_E_RAW', productTypeId: 'FINISHED_GOOD',
            isVirtual: 'N', isVariant: 'N', internalName: 'Raw Material E'])
        storeProduct([productId: 'II_PROD_CHAIN', productTypeId: 'FINISHED_GOOD',
            isVirtual: 'N', isVariant: 'N', internalName: 'Chain Test Product'])

        java.sql.Timestamp fromDate = java.sql.Timestamp.valueOf('2020-01-01 00:00:00')
        storeProductAssoc([productId: 'II_PROD_CHAIN', productIdTo: 'II_MAT_D_PRODUCED',
            productAssocTypeId: 'MANUF_COMPONENT', fromDate: fromDate, quantity: 1.0])
        storeProductAssoc([productId: 'II_MAT_D_PRODUCED', productIdTo: 'II_MAT_E_RAW',
            productAssocTypeId: 'MANUF_COMPONENT', fromDate: fromDate, quantity: 1.0])
    }

    protected void ensureFacilityPolicies() {
        storeFacility([facilityId: 'II_WH', facilityName: 'Test Warehouse', facilityTypeId: 'WAREHOUSE',
            ownerPartyId: 'II_COMPANY', autoReservePrun: 'Y', allowInventoryReallocation: 'Y', reconcilePrunBackorders: 'N'])
        storeFacility([facilityId: 'WH_FLUID', facilityName: 'Fluid Warehouse', facilityTypeId: 'WAREHOUSE',
            ownerPartyId: 'II_COMPANY', autoReservePrun: 'Y', allowInventoryReallocation: 'Y', reconcilePrunBackorders: 'Y'])
        storeFacility([facilityId: 'WH_TRADITIONAL', facilityName: 'Traditional Warehouse',
            facilityTypeId: 'WAREHOUSE', ownerPartyId: 'II_COMPANY', autoReservePrun: 'Y', allowInventoryReallocation: 'N',
            reconcilePrunBackorders: 'N'])
        storeFacility([facilityId: 'WH_SAFE', facilityName: 'Safe Recovery Warehouse',
            facilityTypeId: 'WAREHOUSE', ownerPartyId: 'II_COMPANY', autoReservePrun: 'Y', allowInventoryReallocation: 'N',
            reconcilePrunBackorders: 'Y'])
        storeFacility([facilityId: 'WH_NO_AUTO_RES', facilityName: 'No Auto Reservation Warehouse',
            facilityTypeId: 'WAREHOUSE', ownerPartyId: 'II_COMPANY', autoReservePrun: 'N',
            allowInventoryReallocation: 'Y', reconcilePrunBackorders: 'N'])
    }

    protected void setUpInventory(String facilityId, String productId, String itemId, BigDecimal quantity,
                                  boolean purgeExisting = true) {
        userLogin = userLogin ?: from('UserLogin').where('userLoginId', 'system').queryOne()

        if (purgeExisting && !from('InventoryItem').where('inventoryItemId', itemId).queryOne()) {
            List items = from('InventoryItem').where('productId', productId, 'facilityId', facilityId).queryList()
            List itemIds = (List) items.inventoryItemId
            if (itemIds) {
                delegator.removeByCondition('InventoryItemDetail',
                        EntityCondition.makeCondition('inventoryItemId', EntityOperator.IN, itemIds))
                delegator.removeByCondition('WorkEffortInventoryAssign',
                        EntityCondition.makeCondition('inventoryItemId', EntityOperator.IN, itemIds))
                delegator.removeByCondition('WorkEffortInvRes',
                        EntityCondition.makeCondition('inventoryItemId', EntityOperator.IN, itemIds))
                delegator.removeByCondition('InventoryItem',
                        EntityCondition.makeCondition('inventoryItemId', EntityOperator.IN, itemIds))
            }
            delegator.removeByCondition('WorkEffortInvRes', EntityCondition.makeCondition('productId', productId))
        }

        if (!from('InventoryItem').where('inventoryItemId', itemId).queryOne()) {
            delegator.create('InventoryItem', [
                inventoryItemId: itemId, productId: productId, facilityId: facilityId,
                inventoryItemTypeId: 'NON_SERIAL_INV_ITEM', ownerPartyId: 'II_COMPANY',
                quantityOnHandTotal: 0.0, availableToPromiseTotal: 0.0, accountingQuantityTotal: 0.0,
                datetimeReceived: UtilDateTime.nowTimestamp()
            ])
        }

        if (quantity != 0) {
            delegator.create('InventoryItemDetail', [
                inventoryItemDetailSeqId: delegator.getNextSeqId('InventoryItemDetail'),
                inventoryItemId: itemId,
                availableToPromiseDiff: quantity,
                quantityOnHandDiff: quantity,
                accountingQuantityDiff: quantity,
                effectiveDate: UtilDateTime.nowTimestamp()
            ])
        }

        delegator.clearAllCaches()
        dispatcher.runSync('reconcileGlobalReservations',
                [inventoryItemId: itemId, amountToIssue: 0.0, userLogin: userLogin])
    }

    // --- Helper Methods ---

    protected BigDecimal scaleQuantity(Object quantity) {
        BigDecimal value = (quantity ?: BigDecimal.ZERO) as BigDecimal
        return value.setScale(2, RoundingMode.HALF_UP)
    }

    protected void assertInventoryIntegrity(String productId, String workEffortId, BigDecimal expectedIssued,
                                      BigDecimal expectedRes, BigDecimal expectedAtp = null,
                                      String facilityId = 'II_WH') {
        List assignments = from('WorkEffortInventoryAssign')
                .where('workEffortId', workEffortId)
                .queryList()
        BigDecimal totalIssued = assignments.inject(BigDecimal.ZERO) { sum, item ->
            GenericValue inventoryItem = from('InventoryItem').where('inventoryItemId', item.inventoryItemId).queryOne()
            if (inventoryItem && inventoryItem.productId == productId) {
                return sum + (item.getBigDecimal('quantity') ?: BigDecimal.ZERO)
            }
            return sum
        }
        assert totalIssued.setScale(2, RoundingMode.HALF_UP) == expectedIssued.setScale(2, RoundingMode.HALF_UP) :
                "Physical Issuance Mismatch for ${productId}"

        List resRecords = from('WorkEffortInvRes')
                .where('workEffortId', workEffortId, 'productId', productId)
                .queryList()
        BigDecimal totalRes = resRecords.inject(BigDecimal.ZERO) { sum, res ->
            BigDecimal qty = res.getBigDecimal('quantity') ?: BigDecimal.ZERO
            BigDecimal qna = res.getBigDecimal('quantityNotAvailable') ?: BigDecimal.ZERO
            return sum + (qty - qna)
        }
        assert totalRes.setScale(2, RoundingMode.HALF_UP) == expectedRes.setScale(2, RoundingMode.HALF_UP) :
                "Net Reservation (Physical) Mismatch for ${productId}"

        List inventoryItems = from('InventoryItem').where('productId', productId, 'facilityId', facilityId)
                .cache(false).queryList()
        BigDecimal totalAtp = inventoryItems.inject(BigDecimal.ZERO) { sum, item ->
            sum + (item.getBigDecimal('availableToPromiseTotal') ?: BigDecimal.ZERO)
        }
        BigDecimal totalQoh = inventoryItems.inject(BigDecimal.ZERO) { sum, item ->
            sum + (item.getBigDecimal('quantityOnHandTotal') ?: BigDecimal.ZERO)
        }
        BigDecimal totalAccounting = inventoryItems.inject(BigDecimal.ZERO) { sum, item ->
            sum + (item.getBigDecimal('accountingQuantityTotal') ?: BigDecimal.ZERO)
        }

        BigDecimal globalDebt = from('WorkEffortInvRes').where('productId', productId, 'inventoryItemId', null).queryList()
                .inject(BigDecimal.ZERO) { sum, it ->
                    BigDecimal qty = it.getBigDecimal('quantity') ?: 0
                    BigDecimal qna = it.getBigDecimal('quantityNotAvailable') ?: 0
                    return sum + (qty - qna)
                }

        BigDecimal trueTotalAtp = totalAtp - globalDebt

        if (expectedAtp != null) {
            assert trueTotalAtp.setScale(2, RoundingMode.HALF_UP) == expectedAtp.setScale(2, RoundingMode.HALF_UP) :
                    "ATP Global Pool Mismatch for ${productId}"
        }

        assert totalAccounting.setScale(2, RoundingMode.HALF_UP) == totalQoh.setScale(2, RoundingMode.HALF_UP) :
                "Ledger Sync (Accounting vs QOH) Mismatch for ${productId}"

        GenericValue wegs = from('WorkEffortGoodStandard')
                .where('workEffortId', workEffortId, 'productId', productId, 'workEffortGoodStdTypeId', 'PRUNT_PROD_NEEDED')
                .queryFirst()
        if (wegs) {
            BigDecimal needed = wegs.getBigDecimal('estimatedQuantity') ?: BigDecimal.ZERO
            if (totalIssued >= needed && needed > 0) {
                assert wegs.statusId == 'WEGS_COMPLETED' :
                        "Logical Satisfaction Failure: WorkEffortGoodStandard should be WEGS_COMPLETED for fully issued ${productId}"
            } else if (totalIssued > 0) {
                assert wegs.statusId != 'WEGS_COMPLETED' :
                        "Logical Satisfaction Failure: WorkEffortGoodStandard should NOT be WEGS_COMPLETED for partially issued ${productId}"
            }
        }
    }

    protected String setUpProductionRun(String productId, BigDecimal quantity, String facilityId = 'II_WH') {
        userLogin = userLogin ?: from('UserLogin').where('userLoginId', 'system').queryOne()
        assert userLogin != null : 'UserLogin \'system\' must exist for tests'
        Map createResult = dispatcher.runSync('createProductionRun', [
                productId: productId, pRQuantity: quantity,
                startDate: UtilDateTime.nowTimestamp(),
                facilityId: facilityId, userLogin: userLogin
        ])
        String productionRunId = (String) createResult.productionRunId

        Map statusResult = dispatcher.runSync('changeProductionRunStatus', [
                productionRunId: productionRunId, statusId: 'PRUN_DOC_PRINTED',
                userLogin: userLogin
        ])
        assert ServiceUtil.isSuccess(statusResult)

        List tasks = from('WorkEffort').where('workEffortParentId', productionRunId).orderBy('workEffortId').queryList()
        return tasks ? (String) tasks[0].workEffortId : productionRunId
    }

    protected String setUpNoAutoReserveProductionRun(String matAItemId, BigDecimal matAQuantity) {
        storeFacility([facilityId: 'WH_NO_AUTO_RES', facilityName: 'No Auto Reservation Warehouse',
            facilityTypeId: 'WAREHOUSE', ownerPartyId: 'II_COMPANY', autoReservePrun: 'N',
            allowInventoryReallocation: 'Y', reconcilePrunBackorders: 'N'])
        setUpInventory('WH_NO_AUTO_RES', 'II_MAT_A_COST', matAItemId, matAQuantity)
        setUpInventory('WH_NO_AUTO_RES', 'II_MAT_C_COST', "${matAItemId}_C", 100.0)
        return setUpProductionRun('II_PROD_MANUF', 10.0, 'WH_NO_AUTO_RES')
    }

}
