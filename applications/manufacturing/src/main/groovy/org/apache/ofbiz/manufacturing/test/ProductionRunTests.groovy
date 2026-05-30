package org.apache.ofbiz.manufacturing.test

import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.service.testtools.OFBizTestCase
import org.apache.ofbiz.base.util.UtilDateTime
import java.sql.Timestamp

class ProductionRunTests extends OFBizTestCase {

    ProductionRunTests(String name) {
        super(name)
    }

    void testProductionRunCreation() {
        GenericValue userLogin = from('UserLogin').where('userLoginId', 'TestManufAdmin').queryOne()
        String productId = 'PROD_MANUF'
        String facilityId = 'WebStoreWarehouse'
        BigDecimal quantity = new BigDecimal('5.0')
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp()
        Timestamp productionRunStartDate = UtilDateTime.addDaysToTimestamp(nowTimestamp, 1)

        Map serviceCtx = [
                userLogin: userLogin,
                productId: productId,
                pRQuantity: quantity,
                startDate: productionRunStartDate,
                facilityId: facilityId,
        ]
        Map serviceResult = dispatcher.runSync('createProductionRun', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        String productionRunId = serviceResult.productionRunId
        assert productionRunId

        GenericValue productionRunHeader = from('WorkEffort').where('workEffortId', productionRunId).queryOne()
        List<GenericValue> productionRunTasks = from('WorkEffort').where('workEffortParentId', productionRunId).queryList()
        GenericValue productionRunTask = productionRunTasks ? productionRunTasks[0] : null

        assert productionRunHeader.workEffortTypeId == 'PROD_ORDER_HEADER'
        assert productionRunHeader.facilityId == 'WebStoreWarehouse'
        assert productionRunHeader.currentStatusId == 'PRUN_CREATED'
        assert productionRunTask.currentStatusId == 'PRUN_CREATED'
        assert productionRunHeader.quantityToProduce == new BigDecimal('5.0')

        List<GenericValue> productionRunProducts = from('WorkEffortGoodStandard').where('workEffortId', productionRunId).queryList()
        GenericValue productionRunProduct = productionRunProducts ? productionRunProducts[0] : null

        assert productionRunProduct
        assert productionRunProduct.productId == productId
        assert productionRunProduct.workEffortGoodStdTypeId == 'PRUN_PROD_DELIV'
        assert productionRunProduct.estimatedQuantity == new BigDecimal('5.0')

        assert productionRunTask
        assert productionRunTask.workEffortTypeId == 'PROD_ORDER_TASK'
        assert productionRunTask.fixedAssetId == 'WORKCENTER_COST'
        assert productionRunTask.currentStatusId == 'PRUN_CREATED'
        assert productionRunTask.estimatedSetupMillis == 600000.00
        assert productionRunTask.estimatedMilliSeconds == 300000.00

        List<GenericValue> costCalcs = from('WorkEffortCostCalc').where('workEffortId', productionRunTask.workEffortId).queryList()
        GenericValue costCalc = costCalcs ? costCalcs[0] : null
        assert costCalc
        assert costCalc.costComponentTypeId == 'OTHER_COST'
        assert costCalc.costComponentCalcId == 'TASK_COST_CALC'

        List<GenericValue> productionRunMaterials = from('WorkEffortGoodStandard').where('workEffortId', productionRunTask.workEffortId).queryList()
        GenericValue productionRunMaterialA = productionRunMaterials ? productionRunMaterials[0] : null
        GenericValue productionRunMaterialB = productionRunMaterials ? productionRunMaterials[1] : null

        assert productionRunMaterialA
        assert productionRunMaterialA.workEffortGoodStdTypeId == 'PRUNT_PROD_NEEDED'
        assert productionRunMaterialA.estimatedQuantity == quantity * 2

        assert productionRunMaterialB
        assert productionRunMaterialB.workEffortGoodStdTypeId == 'PRUNT_PROD_NEEDED'
        assert productionRunMaterialB.estimatedQuantity == quantity * 3
    }

    void testProductionRunScheduleConfirm() {
        GenericValue userLogin = from('UserLogin').where('userLoginId', 'TestManufAdmin').queryOne()
        String productId = 'PROD_MANUF'
        String facilityId = 'WebStoreWarehouse'
        BigDecimal quantity = new BigDecimal('5.0')
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp()
        Timestamp productionRunStartDate = UtilDateTime.addDaysToTimestamp(nowTimestamp, 1)

        Map serviceCtx = [
                userLogin: userLogin,
                productId: productId,
                pRQuantity: quantity,
                startDate: productionRunStartDate,
                facilityId: facilityId,
        ]
        Map serviceResult = dispatcher.runSync('createProductionRun', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        String productionRunId = serviceResult.productionRunId

        dispatcher.runSync('changeProductionRunStatus', [userLogin: userLogin, productionRunId: productionRunId, statusId: 'PRUN_SCHEDULED'])
        
        GenericValue productionRunHeader = from('WorkEffort').where('workEffortId', productionRunId).queryOne()
        List<GenericValue> productionRunTasks = from('WorkEffort').where('workEffortParentId', productionRunId).queryList()
        GenericValue productionRunTask = productionRunTasks ? productionRunTasks[0] : null

        assert productionRunHeader.currentStatusId == 'PRUN_SCHEDULED'
        assert productionRunTask.currentStatusId == 'PRUN_SCHEDULED'

        dispatcher.runSync('changeProductionRunStatus', [userLogin: userLogin, productionRunId: productionRunId, statusId: 'PRUN_DOC_PRINTED'])

        productionRunHeader = from('WorkEffort').where('workEffortId', productionRunId).queryOne()
        productionRunTasks = from('WorkEffort').where('workEffortParentId', productionRunId).queryList()
        productionRunTask = productionRunTasks ? productionRunTasks[0] : null

        assert productionRunHeader.currentStatusId == 'PRUN_DOC_PRINTED'
        assert productionRunTask.currentStatusId == 'PRUN_DOC_PRINTED'
    }

    void testProductionRunDateChange() {
        GenericValue userLogin = from('UserLogin').where('userLoginId', 'TestManufAdmin').queryOne()
        String productId = 'PROD_MANUF'
        String facilityId = 'WebStoreWarehouse'
        BigDecimal quantity = new BigDecimal('5.0')
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp()
        Timestamp productionRunStartDate = UtilDateTime.addDaysToTimestamp(nowTimestamp, 1)

        Map serviceCtx = [
                userLogin: userLogin,
                productId: productId,
                pRQuantity: quantity,
                startDate: productionRunStartDate,
                facilityId: facilityId,
        ]
        Map serviceResult = dispatcher.runSync('createProductionRun', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        String productionRunId = serviceResult.productionRunId

        dispatcher.runSync('changeProductionRunStatus', [userLogin: userLogin, productionRunId: productionRunId, statusId: 'PRUN_SCHEDULED'])

        Timestamp productionRunNewStartDate = UtilDateTime.addDaysToTimestamp(nowTimestamp, 2)
        dispatcher.runSync('updateProductionRun', [userLogin: userLogin, productionRunId: productionRunId, estimatedStartDate: productionRunNewStartDate])

        GenericValue productionRunHeader = from('WorkEffort').where('workEffortId', productionRunId).queryOne()
        List<GenericValue> productionRunTasks = from('WorkEffort').where('workEffortParentId', productionRunId).queryList()
        GenericValue productionRunTask = productionRunTasks ? productionRunTasks[0] : null

        assert productionRunHeader.estimatedStartDate == productionRunNewStartDate || productionRunHeader.estimatedStartDate.getTime() == productionRunNewStartDate.getTime()
        assert productionRunTask.estimatedStartDate == productionRunNewStartDate || productionRunTask.estimatedStartDate.getTime() == productionRunNewStartDate.getTime()
    }

    void testProductionRunCancelled() {
        GenericValue userLogin = from('UserLogin').where('userLoginId', 'TestManufAdmin').queryOne()
        String productId = 'PROD_MANUF'
        String facilityId = 'WebStoreWarehouse'
        BigDecimal quantity = new BigDecimal('5.0')
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp()
        Timestamp productionRunStartDate = UtilDateTime.addDaysToTimestamp(nowTimestamp, 1)

        Map serviceCtx = [
                userLogin: userLogin,
                productId: productId,
                pRQuantity: quantity,
                startDate: productionRunStartDate,
                facilityId: facilityId,
        ]
        Map serviceResult = dispatcher.runSync('createProductionRun', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        String productionRunId = serviceResult.productionRunId

        dispatcher.runSync('quickChangeProductionRunStatus', [userLogin: userLogin, productionRunId: productionRunId, statusId: 'PRUN_SCHEDULED'])
        dispatcher.runSync('cancelProductionRun', [userLogin: userLogin, productionRunId: productionRunId])

        GenericValue productionRunHeader = from('WorkEffort').where('workEffortId', productionRunId).queryOne()
        List<GenericValue> productionRunTasks = from('WorkEffort').where('workEffortParentId', productionRunId).queryList()
        GenericValue productionRunTask = productionRunTasks ? productionRunTasks[0] : null

        assert productionRunHeader.currentStatusId == 'PRUN_CANCELLED'
        assert productionRunTask.currentStatusId == 'PRUN_CANCELLED'

        List<GenericValue> productionRunProducts = from('WorkEffortGoodStandard').where('workEffortId', productionRunId).queryList()
        GenericValue productionRunProduct = productionRunProducts ? productionRunProducts[0] : null
        assert productionRunProduct
        assert productionRunProduct.workEffortGoodStdTypeId == 'PRUN_PROD_DELIV'
        assert productionRunProduct.statusId == 'WEGS_CANCELLED'

        List<GenericValue> productionRunMaterials = from('WorkEffortGoodStandard').where('workEffortId', productionRunTask.workEffortId).queryList()
        GenericValue productionRunMaterialA = productionRunMaterials ? productionRunMaterials[0] : null
        GenericValue productionRunMaterialB = productionRunMaterials ? productionRunMaterials[1] : null

        assert productionRunMaterialA
        assert productionRunMaterialA.workEffortGoodStdTypeId == 'PRUNT_PROD_NEEDED'
        assert productionRunMaterialA.statusId == 'WEGS_CANCELLED'
        assert productionRunMaterialB
        assert productionRunMaterialB.workEffortGoodStdTypeId == 'PRUNT_PROD_NEEDED'
        assert productionRunMaterialB.statusId == 'WEGS_CANCELLED'
    }

    void testProductionRunQuickIssueAndProduce() {
        GenericValue userLogin = from('UserLogin').where('userLoginId', 'TestManufAdmin').queryOne()
        String productId = 'PROD_MANUF'
        String facilityId = 'WebStoreWarehouse'
        BigDecimal quantity = new BigDecimal('2.0')
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp()
        Timestamp productionRunStartDate = UtilDateTime.addDaysToTimestamp(nowTimestamp, 1)

        Map serviceCtx = [
                userLogin: userLogin,
                productId: productId,
                pRQuantity: quantity,
                startDate: productionRunStartDate,
                facilityId: facilityId,
        ]
        Map serviceResult = dispatcher.runSync('createProductionRun', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        String productionRunId = serviceResult.productionRunId

        dispatcher.runSync('quickChangeProductionRunStatus', [userLogin: userLogin, productionRunId: productionRunId, statusId: 'PRUN_DOC_PRINTED'])
        dispatcher.runSync('quickStartAllProductionRunTasks', [userLogin: userLogin, productionRunId: productionRunId])

        List<GenericValue> productionRunTasks = from('WorkEffort').where('workEffortParentId', productionRunId).queryList()
        GenericValue productionRunTask = productionRunTasks ? productionRunTasks[0] : null

        List<GenericValue> consumedMaterials = from('WorkEffortGoodStandard').where('workEffortId', productionRunTask.workEffortId).queryList()
        Map componentsLocationMap = [:]
        for (GenericValue consumedMaterial : consumedMaterials) {
            String materialId = consumedMaterial.productId
            List<GenericValue> locations = from('ProductFacilityLocation').where('productId', materialId, 'facilityId', facilityId).queryList()
            if (locations) {
                componentsLocationMap[materialId] = locations[0].locationSeqId
            }
        }

        Map issueAndProduceCtx = [
            userLogin: userLogin,
            workEffortId: productionRunId,
            inventoryItemTypeId: 'NON_SERIAL_INV_ITEM',
            lotId: 'LOT12345',
            componentsLocationMap: componentsLocationMap,
            quantity: new BigDecimal('1.0')
        ]
        dispatcher.runSync('productionRunDeclareAndProduce', issueAndProduceCtx)

        GenericValue productionRunHeader = from('WorkEffort').where('workEffortId', productionRunId).queryOne()
        productionRunTasks = from('WorkEffort').where('workEffortParentId', productionRunId).queryList()
        productionRunTask = productionRunTasks ? productionRunTasks[0] : null

        assert productionRunHeader.currentStatusId == 'PRUN_RUNNING'
        assert productionRunTask.currentStatusId == 'PRUN_RUNNING'

        List<GenericValue> inventoryProduced = from('WorkEffortAndInventoryProduced').where('workEffortId', productionRunId).queryList()
        BigDecimal materialProducedTotal = BigDecimal.ZERO
        for (GenericValue produced : inventoryProduced) { materialProducedTotal = materialProducedTotal.add(produced.getBigDecimal("quantityOnHandTotal") ?: BigDecimal.ZERO) }
        
        List<GenericValue> inventoryConsumed = from('WorkEffortAndInventoryAssign').where('workEffortId', productionRunTask.workEffortId).queryList()
        BigDecimal materialAConsumedTotal = BigDecimal.ZERO
        BigDecimal materialBConsumedTotal = BigDecimal.ZERO
        for (GenericValue consumed : inventoryConsumed) {
            if (consumed.productId == 'MAT_A_COST') {
                materialAConsumedTotal = materialAConsumedTotal.add(consumed.getBigDecimal("quantity") ?: BigDecimal.ZERO)
            } else if (consumed.productId == 'MAT_B_COST') {
                materialBConsumedTotal = materialBConsumedTotal.add(consumed.getBigDecimal("quantity") ?: BigDecimal.ZERO)
            }
        }

        assert materialAConsumedTotal == new BigDecimal('2.0')
        assert materialBConsumedTotal == new BigDecimal('3.0')
        assert materialProducedTotal == new BigDecimal('1.0')
        assert productionRunHeader.quantityProduced == new BigDecimal('1.0')

        // Do it again
        dispatcher.runSync('productionRunDeclareAndProduce', issueAndProduceCtx)
        dispatcher.runSync('quickChangeProductionRunStatus', [userLogin: userLogin, productionRunId: productionRunId, statusId: 'PRUN_COMPLETED'])

        productionRunHeader = from('WorkEffort').where('workEffortId', productionRunId).queryOne()
        productionRunTasks = from('WorkEffort').where('workEffortParentId', productionRunId).queryList()
        productionRunTask = productionRunTasks ? productionRunTasks[0] : null

        assert productionRunHeader.currentStatusId == 'PRUN_COMPLETED'
        assert productionRunTask.currentStatusId == 'PRUN_COMPLETED'

        inventoryProduced = from('WorkEffortAndInventoryProduced').where('workEffortId', productionRunId).queryList()
        materialProducedTotal = BigDecimal.ZERO
        for (GenericValue produced : inventoryProduced) { materialProducedTotal = materialProducedTotal.add(produced.getBigDecimal("quantityOnHandTotal") ?: BigDecimal.ZERO) }
        
        inventoryConsumed = from('WorkEffortAndInventoryAssign').where('workEffortId', productionRunTask.workEffortId).queryList()
        materialAConsumedTotal = BigDecimal.ZERO
        materialBConsumedTotal = BigDecimal.ZERO
        for (GenericValue consumed : inventoryConsumed) {
            if (consumed.productId == 'MAT_A_COST') {
                materialAConsumedTotal = materialAConsumedTotal.add(consumed.getBigDecimal("quantity") ?: BigDecimal.ZERO)
            } else if (consumed.productId == 'MAT_B_COST') {
                materialBConsumedTotal = materialBConsumedTotal.add(consumed.getBigDecimal("quantity") ?: BigDecimal.ZERO)
            }
        }

        assert materialAConsumedTotal == new BigDecimal('4.0')
        assert materialBConsumedTotal == new BigDecimal('6.0')
        assert materialProducedTotal == new BigDecimal('2.0')
        assert productionRunHeader.quantityProduced == new BigDecimal('2.0')

        Map costResult = dispatcher.runSync('getProductionRunCost', [userLogin: userLogin, workEffortId: productionRunId])
        BigDecimal totalCost = costResult.totalCost
        assert totalCost != null

        List<GenericValue> acctgTrans = from('AcctgTrans').where('workEffortId', productionRunId).queryList()
        // we can check if trans exist
        assert acctgTrans.size() > 0
    }

    void testQuickRunProductionRun() {
        GenericValue userLogin = from('UserLogin').where('userLoginId', 'TestManufAdmin').queryOne()
        String productId = 'PROD_MANUF'
        String facilityId = 'WebStoreWarehouse'
        BigDecimal quantity = new BigDecimal('1.0')
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp()
        Timestamp productionRunStartDate = UtilDateTime.addDaysToTimestamp(nowTimestamp, 1)

        Map serviceCtx = [
                userLogin: userLogin,
                productId: productId,
                pRQuantity: quantity,
                startDate: productionRunStartDate,
                facilityId: facilityId,
        ]
        Map serviceResult = dispatcher.runSync('createProductionRun', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        String productionRunId = serviceResult.productionRunId

        dispatcher.runSync('quickChangeProductionRunStatus', [userLogin: userLogin, productionRunId: productionRunId, statusId: 'PRUN_DOC_PRINTED'])
        dispatcher.runSync('quickChangeProductionRunStatus', [userLogin: userLogin, productionRunId: productionRunId, statusId: 'PRUN_COMPLETED'])

        GenericValue productionRunHeader = from('WorkEffort').where('workEffortId', productionRunId).queryOne()
        List<GenericValue> productionRunTasks = from('WorkEffort').where('workEffortParentId', productionRunId).queryList()
        GenericValue productionRunTask = productionRunTasks ? productionRunTasks[0] : null

        assert productionRunHeader.currentStatusId == 'PRUN_COMPLETED'
        assert productionRunTask.currentStatusId == 'PRUN_COMPLETED'

        Map costResult = dispatcher.runSync('getProductionRunCost', [userLogin: userLogin, workEffortId: productionRunId])
        assert costResult.totalCost
    }

    void testQuickCloseProductionRun() {
        GenericValue userLogin = from('UserLogin').where('userLoginId', 'TestManufAdmin').queryOne()
        String productId = 'PROD_MANUF'
        String facilityId = 'WebStoreWarehouse'
        BigDecimal quantity = new BigDecimal('1.0')
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp()
        Timestamp productionRunStartDate = UtilDateTime.addDaysToTimestamp(nowTimestamp, 1)

        Map serviceCtx = [
                userLogin: userLogin,
                productId: productId,
                pRQuantity: quantity,
                startDate: productionRunStartDate,
                facilityId: facilityId,
        ]
        Map serviceResult = dispatcher.runSync('createProductionRun', serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        String productionRunId = serviceResult.productionRunId

        dispatcher.runSync('quickChangeProductionRunStatus', [userLogin: userLogin, productionRunId: productionRunId, statusId: 'PRUN_DOC_PRINTED'])
        dispatcher.runSync('quickChangeProductionRunStatus', [userLogin: userLogin, productionRunId: productionRunId, statusId: 'PRUN_CLOSED'])

        GenericValue productionRunHeader = from('WorkEffort').where('workEffortId', productionRunId).queryOne()
        List<GenericValue> productionRunTasks = from('WorkEffort').where('workEffortParentId', productionRunId).queryList()
        GenericValue productionRunTask = productionRunTasks ? productionRunTasks[0] : null

        assert productionRunHeader.currentStatusId == 'PRUN_CLOSED'
        assert productionRunTask.currentStatusId == 'PRUN_CLOSED'
    }

    void testCreateProductionRunForOrder() {
        GenericValue userLogin = from('UserLogin').where('userLoginId', 'admin').queryOne()
        String productId = 'PROD_MANUF'

        Map serviceResult = dispatcher.runSync('createTestSalesOrderSingle', [userLogin: userLogin, productId: productId])
        assert ServiceUtil.isSuccess(serviceResult)
        String orderId = serviceResult.orderId

        GenericValue orderItem = from('OrderItem').where('orderId', orderId).queryFirst()
        GenericValue originalOrderItemShipGrpInvRes = from('OrderItemShipGrpInvRes').where('orderId', orderId, 'orderItemSeqId', orderItem.orderItemSeqId).queryFirst()

        Map prCtx = [
            userLogin: userLogin,
            orderId: orderId,
        ]
        Map prResult = dispatcher.runSync('createProductionRunsForOrder', prCtx)
        assert ServiceUtil.isSuccess(prResult)
        List<String> productionRuns = prResult.productionRuns
        String productionRunId = productionRuns ? productionRuns[0] : null
        assert productionRunId

        GenericValue productionRunHeader = from('WorkEffort').where('workEffortId', productionRunId).queryOne()
        List<GenericValue> productionRunTasks = from('WorkEffort').where('workEffortParentId', productionRunId).queryList()
        GenericValue productionRunTask = productionRunTasks ? productionRunTasks[0] : null

        assert productionRunHeader.workEffortTypeId == 'PROD_ORDER_HEADER'
        assert productionRunHeader.facilityId == 'WebStoreWarehouse'
        assert productionRunHeader.currentStatusId == 'PRUN_CREATED'
        assert productionRunTask.currentStatusId == 'PRUN_CREATED'
        assert productionRunHeader.quantityToProduce == new BigDecimal('1.0')

        List<GenericValue> productionRunProducts = from('WorkEffortGoodStandard').where('workEffortId', productionRunId).queryList()
        GenericValue productionRunProduct = productionRunProducts ? productionRunProducts[0] : null

        assert productionRunProduct
        assert productionRunProduct.productId == productId
        assert productionRunProduct.workEffortGoodStdTypeId == 'PRUN_PROD_DELIV'
        assert productionRunProduct.estimatedQuantity == new BigDecimal('1.0')

        GenericValue workOrderItemFulfillment = from('WorkOrderItemFulfillment').where('workEffortId', productionRunId, 'orderId', orderId).queryFirst()
        assert workOrderItemFulfillment
        assert workOrderItemFulfillment.orderItemSeqId

        dispatcher.runSync('quickChangeProductionRunStatus', [userLogin: userLogin, productionRunId: productionRunId, statusId: 'PRUN_DOC_PRINTED'])
        dispatcher.runSync('quickChangeProductionRunStatus', [userLogin: userLogin, productionRunId: productionRunId, statusId: 'PRUN_CLOSED'])

        GenericValue producedMaterial = from('WorkEffortAndInventoryProduced').where('workEffortId', productionRunHeader.workEffortId).queryFirst()
        GenericValue newOrderItemShipGrpInvRes = from('OrderItemShipGrpInvRes').where('orderId', orderId, 'orderItemSeqId', orderItem.orderItemSeqId).queryFirst()

        assert newOrderItemShipGrpInvRes
        assert newOrderItemShipGrpInvRes.inventoryItemId != originalOrderItemShipGrpInvRes.inventoryItemId
        assert producedMaterial.inventoryItemId == newOrderItemShipGrpInvRes.inventoryItemId
    }

    void testCreateProductionRunForRequirement() {
        GenericValue userLogin = from('UserLogin').where('userLoginId', 'TestSupplyAdmin').queryOne()
        String productId = 'PROD_MANUF'
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp()
        Timestamp startDate = UtilDateTime.addDaysToTimestamp(nowTimestamp, 1)

        Map reqCtx = [
            userLogin: userLogin,
            productId: productId,
            requirementTypeId: 'INTERNAL_REQUIREMENT',
            facilityId: 'WebStoreWarehouse',
            requirementStartDate: startDate
        ]
        Map reqResult = dispatcher.runSync('createRequirement', reqCtx)
        assert ServiceUtil.isSuccess(reqResult)
        String requirementId = reqResult.requirementId

        dispatcher.runSync('updateRequirement', [userLogin: userLogin, requirementId: requirementId, statusId: 'REQ_APPROVED'])

        // Manually trigger the ECA service because tests run in an uncommitted transaction
        Map prunResult = dispatcher.runSync('createProductionRunFromRequirement', [userLogin: userLogin, requirementId: requirementId])
        assert ServiceUtil.isSuccess(prunResult)
        String productionRunId = prunResult.productionRunId

        GenericValue productionRunHeader = from('WorkEffort').where('workEffortId', productionRunId).queryOne()
        assert productionRunHeader

        List<GenericValue> productionRunTasks = from('WorkEffort').where('workEffortParentId', productionRunId).queryList()
        GenericValue productionRunTask = productionRunTasks ? productionRunTasks[0] : null

        assert productionRunHeader.workEffortTypeId == 'PROD_ORDER_HEADER'
        assert productionRunHeader.facilityId == 'WebStoreWarehouse'
        assert productionRunHeader.currentStatusId == 'PRUN_CREATED'
        assert productionRunTask.currentStatusId == 'PRUN_CREATED'
        assert productionRunHeader.quantityToProduce == new BigDecimal('1.0')

        List<GenericValue> productionRunProducts = from('WorkEffortGoodStandard').where('workEffortId', productionRunHeader.workEffortId).queryList()
        GenericValue productionRunProduct = productionRunProducts ? productionRunProducts[0] : null

        assert productionRunProduct
        assert productionRunProduct.productId == productId
        assert productionRunProduct.workEffortGoodStdTypeId == 'PRUN_PROD_DELIV'
        assert productionRunProduct.estimatedQuantity == new BigDecimal('1.0')
    }
}
