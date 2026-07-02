/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License") you may not use this file except in compliance
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
package org.apache.ofbiz.product.shipment

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.widget.model.ThemeFactory
import org.apache.ofbiz.widget.renderer.VisualTheme
import org.apache.ofbiz.widget.renderer.macro.MacroScreenRenderer
import org.apache.ofbiz.widget.renderer.ScreenRenderer
import org.apache.ofbiz.widget.renderer.fo.FoFormRenderer
import org.apache.ofbiz.webapp.view.ApacheFopWorker
import org.apache.ofbiz.base.util.collections.MapStack
import java.io.StringWriter
import java.io.ByteArrayOutputStream
import javax.xml.transform.stream.StreamSource
import java.io.StringReader
import org.apache.fop.apps.Fop

/*
 * Migrate all element present on entity OldPicklistStatusHistory to entity PickListStatus
 * Update service created 2019-09
 */
Map migrateOldPicklistStatusHistoryToPickListStatus() {
    List<GenericValue> oldPicklistStatusHistories = delegator.findAll('OldPicklistStatusHistory', false)
    oldPicklistStatusHistories.each {
        GenericValue picklistStatus = makeValue('PicklistStatus')
        picklistStatus.statusId = it.statusId
        picklistStatus.statusIdTo = it.statusIdTo
        picklistStatus.picklistId = it.picklistId
        picklistStatus.changeByUserLoginId = it.changeUserLoginId
        picklistStatus.statusDate = it.changeDate
        picklistStatus.create()
        it.remove()
    }
    return success()
}

/**
 * Retrieves a list of orders available to be picked for a given facility.
 * Wraps existing findOrdersToPickMove service.
 */
Map getOrdersToPick() {
    Map result = ServiceUtil.returnSuccess()

    Map findResult = runService('findOrdersToPickMove', [facilityId: context.facilityId, userLogin: context.userLogin])
    if (ServiceUtil.isError(findResult)) {
        return findResult
    }

    // Transform pickMoveInfoList to a simpler list for the PWA
    List orderList = []
    if (findResult.pickMoveInfoList) {
        findResult.pickMoveInfoList.each { pickMoveInfo ->
            if (pickMoveInfo.orderReadyToPickInfoList) {
                pickMoveInfo.orderReadyToPickInfoList.each { orderReadyInfo ->
                    GenericValue orderHeader = orderReadyInfo.orderHeader
                    if (orderHeader) {
                        if (!orderList.any { it.orderId == orderHeader.orderId }) {
                            orderList << [
                                orderId: orderHeader.orderId,
                                orderDate: orderHeader.orderDate,
                                statusId: orderHeader.statusId,
                                grandTotal: orderHeader.grandTotal
                            ]
                        }
                    }
                }
            }
        }
    }

    result.orderList = orderList
    return result
}

/**
 * Retrieves details of a specific picklist, including items sorted by location.
 * Wraps existing getPickAndPackReportInfo service.
 */
Map getPicklistDetails() {
    Map result = ServiceUtil.returnSuccess()

    Map reportResult = runService('getPickAndPackReportInfo', [
        picklistId: context.picklistId,
        userLogin: context.userLogin
    ])

    if (ServiceUtil.isError(reportResult)) {
        return reportResult
    }

    // The reportResult contains a complex structure of picklistBins and their items.
    // We will extract and flatten this for the PWA.
    Map picklistDetails = [:]
    picklistDetails.picklistId = context.picklistId

    List items = []
    if (reportResult.picklistInfo && reportResult.picklistInfo.picklistBinInfoList) {
        reportResult.picklistInfo.picklistBinInfoList.each { binInfo ->
            if (binInfo.picklistItemInfoList) {
                binInfo.picklistItemInfoList.each { itemInfo ->
                    items << [
                        picklistBinId: binInfo.picklistBin?.picklistBinId,
                        orderId: itemInfo.orderItem?.orderId,
                        orderItemSeqId: itemInfo.orderItem?.orderItemSeqId,
                        shipGroupSeqId: itemInfo.picklistItem?.shipGroupSeqId,
                        inventoryItemId: itemInfo.picklistItem?.inventoryItemId,
                        itemStatusId: itemInfo.picklistItem?.itemStatusId,
                        productId: itemInfo.product?.productId,
                        productName: itemInfo.product?.internalName,
                        quantity: itemInfo.picklistItem?.quantity,
                        locationSeqId: itemInfo.inventoryItemAndLocation?.locationSeqId,
                        area: itemInfo.inventoryItemAndLocation?.areaId,
                        aisle: itemInfo.inventoryItemAndLocation?.aisleId,
                        section: itemInfo.inventoryItemAndLocation?.sectionId,
                        level: itemInfo.inventoryItemAndLocation?.levelId
                    ]
                }
            }
        }
    }

    result.picklistDetails = picklistDetails
    result.picklistDetails.items = items
    return result
}

Map getPickingPicklists() {
    Map result = ServiceUtil.returnSuccess()
    String facilityId = context.facilityId
    String statusId = context.statusId
    String searchQuery = context.searchQuery

    // Query picklists
    def conds = [EntityCondition.makeCondition("facilityId", EntityOperator.EQUALS, facilityId)]
    
    // Status Filter:
    // If statusId is "COMPLETED", show PICKLIST_PICKED and PICKLIST_CANCELLED.
    // If statusId is "ALL", show all statuses.
    // If statusId is empty (default), show active picklists only: PICKLIST_INPUT and PICKLIST_PRINTED.
    // Otherwise, match the statusId directly.
    if (statusId == "COMPLETED") {
        conds.add(EntityCondition.makeCondition("statusId", EntityOperator.IN, ["PICKLIST_PICKED", "PICKLIST_CANCELLED"]))
    } else if (statusId && statusId != "ALL") {
        conds.add(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, statusId))
    } else if (!statusId || statusId == "") {
        conds.add(EntityCondition.makeCondition("statusId", EntityOperator.IN, ["PICKLIST_INPUT", "PICKLIST_PRINTED"]))
    }

    // Search Query:
    if (searchQuery && searchQuery.trim() != "") {
        String q = searchQuery.trim()
        Set<String> searchPicklistIds = [] as Set
        
        // 1. Search by picklistId directly
        List picklistsById = from("Picklist")
            .where(EntityCondition.makeCondition("picklistId", EntityOperator.LIKE, "%" + q + "%"))
            .queryList()
        picklistsById.each { searchPicklistIds.add(it.picklistId) }

        // 2. Search by primaryOrderId (Order ID)
        List binsByOrder = from("PicklistBin")
            .where(EntityCondition.makeCondition("primaryOrderId", EntityOperator.LIKE, "%" + q + "%"))
            .queryList()
        binsByOrder.each { searchPicklistIds.add(it.picklistId) }

        if (searchPicklistIds) {
            conds.add(EntityCondition.makeCondition("picklistId", EntityOperator.IN, searchPicklistIds))
        } else {
            // Force empty results if query doesn't match anything
            result.picklistList = []
            return result
        }
    }

    def query = from("Picklist")
        .where(EntityCondition.makeCondition(conds, EntityOperator.AND))
        .orderBy("picklistDate DESC")
        
    // Cap at 20 rows if NOT searching
    if (!searchQuery || searchQuery.trim() == "") {
        query = query.maxRows(20)
    }

    List picklists = query.queryList()

    List picklistList = []
    picklists.each { picklist ->
        // Find bins
        List bins = from("PicklistBin").where("picklistId", picklist.picklistId).queryList()
        int totalOrders = bins.size()

        // Find items
        int totalItems = 0
        int pickedItems = 0
        bins.each { bin ->
            List items = from("PicklistItem").where("picklistBinId", bin.picklistBinId).queryList()
            items.each { item ->
                totalItems += item.quantity ?: 0
                if (item.itemStatusId == "PICKITEM_COMPLETED") {
                    pickedItems += item.quantity ?: 0
                }
            }
        }

        picklistList << [
            picklistId: picklist.picklistId,
            statusId: picklist.statusId,
            picklistDate: picklist.picklistDate,
            totalOrders: totalOrders,
            totalItems: totalItems,
            pickedItems: pickedItems
        ]
    }

    result.picklistList = picklistList
    return result
}

Map cancelPickingPicklist() {
    Map result = ServiceUtil.returnSuccess()
    runService("updatePicklist", [
        picklistId: context.picklistId,
        statusId: "PICKLIST_CANCELLED",
        userLogin: context.userLogin
    ])
    return result
}

Map getPickingPicklistPdf() {
    Map result = ServiceUtil.returnSuccess()
    String picklistId = context.picklistId

    // Retrieve picklist and transition status
    GenericValue picklist = from("Picklist").where("picklistId", picklistId).queryOne()
    if (!picklist) {
        return ServiceUtil.returnError("Picklist not found with ID: " + picklistId)
    }

    if (picklist.statusId == "PICKLIST_INPUT") {
        runService("updatePicklist", [
            picklistId: picklistId,
            statusId: "PICKLIST_PRINTED",
            userLogin: context.userLogin
        ])
    }

    // Programmatic FO rendering
    VisualTheme visualTheme = ThemeFactory.resolveVisualTheme(null)
    MapStack screenContext = MapStack.create()
    screenContext.put("locale", context.locale ?: Locale.getDefault())
    screenContext.put("picklistId", picklistId)
    screenContext.put("userLogin", context.userLogin)

    // Set up parameters map for screens and actions
    Map parameters = [:]
    parameters.put("picklistId", picklistId)
    parameters.put("userLogin", context.userLogin)
    screenContext.put("parameters", parameters)
    // Render the report screen to FO string
    StringWriter writer = new StringWriter()
    MacroScreenRenderer foScreenStringRenderer = new MacroScreenRenderer(
        visualTheme.getModelTheme().getType("screenfop"),
        visualTheme.getModelTheme().getScreenRendererLocation("screenfop")
    )
    ScreenRenderer screens = new ScreenRenderer(writer, screenContext, foScreenStringRenderer)
    screens.populateContextForService(dctx, screenContext)
    screens.getContext().put("formStringRenderer", new FoFormRenderer())
    screens.render("component://product/widget/facility/FacilityScreens.xml#PicklistReport.fo")

    // Convert FO String to PDF byte array
    ByteArrayOutputStream baos = new ByteArrayOutputStream()
    StreamSource src = new StreamSource(new StringReader(writer.toString()))
    Fop fop = ApacheFopWorker.createFopInstance(baos, "application/pdf")
    ApacheFopWorker.transform(src, null, fop)

    baos.flush()
    byte[] pdfBytes = baos.toByteArray()
    baos.close()

    result.pdfBase64 = Base64.getEncoder().encodeToString(pdfBytes)
    return result
}

