import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityConditionBuilder
import org.apache.ofbiz.entity.util.EntityUtil

import java.sql.Timestamp

Map autoReserveWorkEffortInventory() {
    String productionRunId = parameters.productionRunId

    GenericValue productionRun = from('WorkEffort')
            .where(workEffortId: productionRunId)
            .queryOne()

    if (!productionRun) {
        return error("No production run found with ID: ${productionRunId}")
    }

    List<GenericValue> taskList = from('WorkEffort')
            .where(workEffortParentId: productionRunId)
            .queryList()

    List<String> workEffortIds = EntityUtil.getFieldListFromEntityList(taskList, 'workEffortId', true)

    EntityConditionBuilder exprBldr = new EntityConditionBuilder()
    EntityCondition condition = exprBldr.AND {
        IN(workEffortId: workEffortIds)
        EQUALS(workEffortGoodStdTypeId: 'PRUNT_PROD_NEEDED')
    }

    List<GenericValue> components = from('WorkEffortGoodStandard')
            .where(condition)
            .orderBy('productId')
            .filterByDate()
            .queryList()

    String facilityId = productionRun?.facilityId

    // Reserve inventory for task components
    components.each { component ->
        Map<String, Object> serviceCtx = [:]
        serviceCtx.workEffortId = component?.workEffortId
        serviceCtx.productId = component?.productId
        serviceCtx.facilityId = facilityId
        run service: 'reserveWorkEffortInventoryItem', with: serviceCtx
    }

    return success("Inventory reservation completed for production run ${productionRunId}")
}

Map reserveWorkEffortInventoryItem() {
    String inventoryItemId = parameters.inventoryItemId
    String productId = parameters.productId
    String facilityId = parameters.facilityId
    String workEffortId = parameters.workEffortId
    BigDecimal quantity = BigDecimal.ZERO

    if (parameters.quantity) {
        quantity = (BigDecimal) parameters.quantity
    }

    GenericValue component = from('WorkEffortGoodStandard')
            .where(workEffortId: workEffortId,
                    workEffortGoodStdTypeId: 'PRUNT_PROD_NEEDED',
                    productId: productId)
            .filterByDate()
            .queryFirst()

    if (!component) {
        return error("Component not found for productId: ${productId} and workEffortId: ${workEffortId}")
    }

    Map<String, Object> taskQtyResult = run service: 'getTaskQuantities', with: [productId: productId,
                                                                                  workEffortId: workEffortId,
                                                                                  wegsProductId: productId]
    BigDecimal totalReserved = taskQtyResult.totalReserved ?: BigDecimal.ZERO
    BigDecimal totalIssued = taskQtyResult.totalIssued ?: BigDecimal.ZERO

    BigDecimal qtyToBeReserved = component.estimatedQuantity - totalReserved - totalIssued

    if (qtyToBeReserved > 0) {
        Map<String, Object> avail = run service: 'getProductInventoryAvailable', with: [productId: productId,
                                                                                         facilityId: facilityId]
        BigDecimal atp = avail.availableToPromiseTotal ?: BigDecimal.ZERO

        if (qtyToBeReserved > 0) {
            if (quantity > 0) {
                if (quantity > qtyToBeReserved) {
                    quantity = qtyToBeReserved
                }
                quantity = (atp > 0 && atp <= quantity) ? atp : quantity
            } else {
                quantity = (atp > 0 && atp <= qtyToBeReserved) ? atp : qtyToBeReserved
            }

            Map<String, Object> reserveInventoryCtx = [productId: component.productId,
                                                       workEffortId: workEffortId,
                                                       requireInventory: 'N',
                                                       facilityId: facilityId,
                                                       wegsProductId: productId,
                                                       inventoryItemId: inventoryItemId,
                                                       quantity: quantity]

            if (parameters.inventoryItemId) {
                reserveInventoryCtx.inventoryItemId = parameters.inventoryItemId
            }

            run service: 'reserveProductInventoryByFacility', with: reserveInventoryCtx

            qtyToBeReserved -= quantity
        }
    }

    return success()
}

Map getTaskQuantities() {
    String productId = parameters.productId
    String workEffortId = parameters.workEffortId
    String wegsProductId = parameters.wegsProductId

    List<String> productIds = [productId]
    // TODO: Add substitute product IDs to productIds

    EntityConditionBuilder exprBldr = new EntityConditionBuilder()
    EntityCondition condition = exprBldr.AND {
        IN(productId: productIds)
        EQUALS(workEffortId: workEffortId)
    }
    List<GenericValue> issuances = from('WorkEffortAndInventoryAssign')
            .where(condition)
            .queryList()

    BigDecimal totalIssued = BigDecimal.ZERO
    issuances.each { issuance ->
        totalIssued += issuance.quantity ?: BigDecimal.ZERO
    }

    List<GenericValue> reservations = from('WorkEffortInvRes')
            .where('workEffortId', workEffortId,
                    'wegsProductId', wegsProductId)
            .queryList()

    BigDecimal totalReserved = BigDecimal.ZERO
    reservations.each { res ->
        totalReserved += res.quantity ?: BigDecimal.ZERO
    }

    return [totalReserved: totalReserved, totalIssued: totalIssued]
}

Map reserveWorkEffortInventory() {
    String workEffortId = parameters.workEffortId
    String wegsProductId = parameters.wegsProductId
    String inventoryItemId = parameters.inventoryItemId
    BigDecimal quantity = parameters.quantity ?: BigDecimal.ZERO
    BigDecimal quantityNotAvailable = parameters.quantityNotAvailable ?: BigDecimal.ZERO

    if (!workEffortId || !inventoryItemId || quantity <= 0) {
        return error('Missing required parameters or invalid quantity.')
    }

    GenericValue existingRes = from('WorkEffortInvRes')
            .where(workEffortId: workEffortId, inventoryItemId: inventoryItemId, wegsProductId: wegsProductId)
            .queryOne()

    if (existingRes) {
        // Update quantity and quantityNotAvailable
        BigDecimal oldQty = existingRes.quantity ?: BigDecimal.ZERO
        BigDecimal oldNotAvail = existingRes.quantityNotAvailable ?: BigDecimal.ZERO

        existingRes.set('quantity', oldQty + quantity)
        existingRes.set('quantityNotAvailable', oldNotAvail + quantityNotAvailable)
        existingRes.store()
    } else {
        // Create new WorkEffortInvRes record
        Set<String> validFields = delegator.getModelEntity('WorkEffortInvRes').getAllFieldNames() as Set
        Map<String, Object> filteredParams = parameters.findAll { k, v -> validFields.contains(k) }

        GenericValue newWorkEffortInvRes = delegator.makeValue('WorkEffortInvRes', filteredParams)

        Timestamp nowTs = UtilDateTime.nowTimestamp()
        newWorkEffortInvRes.set('createdDatetime', nowTs)

        if (!newWorkEffortInvRes.reservedDatetime) {
            newWorkEffortInvRes.set('reservedDatetime', nowTs)
        }

        newWorkEffortInvRes.create()
    }

    return success()
}

Map releaseProductionRunTaskComponent() {
    String workEffortId = parameters.workEffortId
    String wegsProductId = parameters.wegsProductId
    String inventoryItemId = parameters.inventoryItemId

    if (!workEffortId || !wegsProductId) {
        return error('Missing required parameters: workEffortId and/or wegsProductId')
    }

    EntityConditionBuilder exprBldr = new EntityConditionBuilder()
    EntityCondition condition = exprBldr.AND {
        EQUALS(workEffortId: workEffortId)
        EQUALS(wegsProductId: wegsProductId)
        if (inventoryItemId) {
            EQUALS(inventoryItemId: inventoryItemId)
        }
    }

    List<GenericValue> reservations = from('WorkEffortInvRes')
            .where(condition)
            .queryList()

    reservations.each { reservation ->
        GenericValue inventoryItem = from('InventoryItem')
                .where('inventoryItemId', reservation.inventoryItemId)
                .queryOne()

        if (inventoryItem?.inventoryItemTypeId == 'SERIALIZED_INV_ITEM') {
            Map updateInventoryItemCtx = [inventoryItemId: reservation?.inventoryItemId,
                                          statusId: 'INV_AVAILABLE']
            run service: 'updateInventoryItem', with: updateInventoryItemCtx
        } else {
            Map createInventoryItemDetailCtx = [inventoryItemId: reservation?.inventoryItemId,
                                                workEffortId: reservation.workEffortId,
                                                availableToPromiseDiff: reservation.quantity]
            run service: 'createInventoryItemDetail', with: createInventoryItemDetailCtx
        }
        reservation.remove()
    }

    return success()
}
