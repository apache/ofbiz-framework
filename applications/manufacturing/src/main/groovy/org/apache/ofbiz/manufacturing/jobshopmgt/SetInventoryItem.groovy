import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityConditionBuilder

String workEffortId = parameters.workEffortId
String productId = parameters.productId
BigDecimal requiredQuantity = new BigDecimal(parameters.requiredQuantity ?: '0')
String wegsProductId = parameters.wegsProductId

List<Map> inventoryItemList = []
List<Map> reservedInventoryItemList = []
List<String> possibleProductIds = [productId]

String buildFacilityLocation(GenericValue facilityLocation) {
    if (!facilityLocation) {
        return ''
    }
    List<String> parts = [
            facilityLocation.areaId ?: '',
            facilityLocation.aisleId ?: '',
            facilityLocation.sectionId ?: '',
            facilityLocation.levelId ?: '',
            facilityLocation.positionId ?: ''
    ]
    return parts.join(':')
}

if (workEffortId && productId) {
    GenericValue workEffort = from('WorkEffort')
            .where('workEffortId', workEffortId)
            .queryOne()

    BigDecimal totalReservedQuantity = BigDecimal.ZERO

    // TODO: Add substitute and alternate product support here

    List<GenericValue> workEffortInvReservations = from('WorkEffortInvResAndItem')
            .where('wegsProductId', wegsProductId)
            .queryList()

    workEffortInvReservations.each { weInvReservation ->
        totalReservedQuantity = totalReservedQuantity.add(weInvReservation.quantity ?: BigDecimal.ZERO)

        GenericValue reservedInventoryItem = from('InventoryItem')
                .where('inventoryItemId', weInvReservation.inventoryItemId)
                .queryOne()

        if (reservedInventoryItem) {
            Map reservationMap = [:]
            GenericValue product = reservedInventoryItem.getRelatedOne('Product', false)

            reservationMap.internalName = product?.internalName
            reservationMap.productName = product?.productName
            reservationMap.association = 'Main'
            reservationMap.inventoryItemId = reservedInventoryItem.inventoryItemId
            reservationMap.availableToPromiseTotal = reservedInventoryItem.availableToPromiseTotal
            reservationMap.reservedQuantity = weInvReservation.quantity

            GenericValue facilityLocation = from('FacilityLocation')
                    .where('facilityId', reservedInventoryItem.getString('facilityId'),
                            'locationSeqId', reservedInventoryItem.getString('locationSeqId'))
                    .queryOne()

            reservationMap.facilityLocation = buildFacilityLocation(facilityLocation)

            reservedInventoryItemList.add(reservationMap)
        }
    }

    if (requiredQuantity > 0 && workEffort?.facilityId) {
        EntityConditionBuilder exprBldr = new EntityConditionBuilder()
        EntityCondition itemReqCondition = exprBldr.AND {
            IN(productId: possibleProductIds)
            EQUALS(facilityId: workEffort.facilityId)
            GREATER_THAN(availableToPromiseTotal: BigDecimal.ZERO)
        }

        List<GenericValue> inventoryItems = from('InventoryItem')
                .where(itemReqCondition)
                .orderBy('inventoryItemId')
                .queryList()

        inventoryItems.each { inventoryItem ->
            Map inventoryItemMap = [:]
            GenericValue product = inventoryItem.getRelatedOne('Product', false)

            inventoryItemMap.inventoryItemId = inventoryItem.inventoryItemId
            inventoryItemMap.association = 'Main'
            inventoryItemMap.productId = inventoryItem.productId
            inventoryItemMap.facilityId = inventoryItem.facilityId
            inventoryItemMap.internalName = product?.internalName
            inventoryItemMap.productName = product?.productName
            inventoryItemMap.availableToPromiseTotal = inventoryItem.availableToPromiseTotal

            GenericValue facilityLocation = from('FacilityLocation')
                    .where('facilityId', inventoryItem.getString('facilityId'),
                            'locationSeqId', inventoryItem.getString('locationSeqId'))
                    .queryOne()

            inventoryItemMap.facilityLocation = buildFacilityLocation(facilityLocation)

            inventoryItemList.add(inventoryItemMap)
        }
    }
}

context.inventoryItems = inventoryItemList
context.reservedInventoryItems = reservedInventoryItemList
