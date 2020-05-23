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
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil;

def checkAndCreateWorkEffort() {
    Map result = success()
    /*
     * if needed create some WorkEfforts and remember their IDs:
     * estimatedShipDate: estimatedShipWorkEffId
     * estimatedArrivalDate: estimatedArrivalWorkEffId
     */
    GenericValue lookedUpValue = from("Shipment").where(parameters).queryOne()
    if (parameters.estimatedShipDate) {
        Map shipWorkEffortMap = [workEffortName: "Shipment #${parameters.shipmentId} ${parameters.primaryOrderId} Ship",
                                 currentStatusId: "CAL_TENTATIVE",
                                 workEffortPurposeTypeId: "WEPT_WAREHOUSING",
                                 estimatedStartDate: parameters.estimatedShipDate,
                                 estimatedCompletionDate: parameters.estimatedShipDate,
                                 facilityId: parameters.originFacilityId,
                                 quickAssignPartyId: userLogin.partyId]
        if (["OUTGOING_SHIPMENT", "SALES_SHIPMENT", "PURCHASE_RETURN"].contains(shipmentTypeId)) {
            shipWorkEffortMap.workEffortTypeId = "SHIPMENT_OUTBOUND"
        }
        Map serviceResult = run service: "createWorkEffort", with: shipWorkEffortMap
        if (!ServiceUtil.isSuccess(serviceResult)) return error(serviceResult.errorMessage)
        lookedUpValue.estimatedShipWorkEffId = serviceResult.workEffortId
        if (parameters.partyIdFrom) {
            serviceResult = run service: "assignPartyToWorkEffort", with: [workEffortId: lookedUpValue.estimatedShipWorkEffId,
                                                           partyId: parameters.partyIdFrom,
                                                           roleTypeId: "CAL_ATTENDEE",
                                                           statusId: "CAL_SENT"]
            if (!ServiceUtil.isSuccess(serviceResult)) return error(serviceResult.errorMessage)
        }
    }
    if (parameters.estimatedArrivalDate) {
        Map arrivalWorkEffortMap = [workEffortName: "Shipment #${parameters.shipmentId} ${parameters.primaryOrderId} Arrival",
                                    currentStatusId: "CAL_TENTATIVE",
                                    workEffortPurposeTypeId: "WEPT_WAREHOUSING",
                                    estimatedStartDate: parameters.estimatedArrivalDate,
                                    estimatedCompletionDate: parameters.estimatedArrivalDate,
                                    facilityId: parameters.destinationFacilityId,
                                    quickAssignPartyId: userLogin.partyId]
        if (["INCOMING_SHIPMENT", "PURCHASE_SHIPMENT", "SALES_RETURN"].contains(shipmentTypeId)) {
            arrivalWorkEffortMap.workEffortTypeId = "SHIPMENT_INBOUND"
        }
        Map serviceResult = run service: "createWorkEffort", with: arrivalWorkEffortMap
        if (!ServiceUtil.isSuccess(serviceResult)) return error(serviceResult.errorMessage)
        lookedUpValue.estimatedArrivalWorkEffId = serviceResultAD.workEffortId
        if (parameters.partyIdTo) {
            serviceResult = run service: "assignPartyToWorkEffort", with: [workEffortId: lookedUpValue.estimatedArrivalWorkEffId,
                                                           partyId: parameters.partyIdTo,
                                                           roleTypeId: "CAL_ATTENDEE",
                                                           statusId: "CAL_SENT"]
            if (!ServiceUtil.isSuccess(serviceResult)) return error(serviceResult.errorMessage)
        }
    }
    lookedUpValue.store()
    return result
}
def checkAndUpdateWorkEffort() {
    Map result = success()
    GenericValue lookedUpValue = from("Shipment").where(parameters).queryOne()
    // Check the pickup and delivery dates for changes and update the corresponding WorkEfforts
    if ((parameters.estimatedShipDate && parameters.estimatedShipDate != lookedUpValue.estimatedShipDate)
            || (parameters.originFacilityId && parameters.originFacilityId != lookedUpValue.originFacilityId)
            || (parameters.statusId && parameters.statusId != lookedUpValue.statusId
            && ["SHIPMENT_CANCELLED", "SHIPMENT_PACKED", "SHIPMENT_SHIPPED"].contains(parameters.statusId))) {
        GenericValue estShipWe = from("WorkEffort").where(workEffortId: lookedUpValue.estimatedShipWorkEffId).queryOne()
        if (estShipWe) {
            estShipWe.estimatedStartDate = parameters.estimatedShipDate
            estShipWe.estimatedCompletionDate = parameters.estimatedShipDate
            estShipWe.facilityId = parameters.originFacilityId
            if ((parameters.statusId) && (parameters.statusId != lookedUpValue.statusId)) {
                if (parameters.statusId == "SHIPMENT_CANCELLED") {
                    estShipWe.currentStatusId = "CAL_CANCELLED"
                }
                if (parameters.statusId == "SHIPMENT_PACKED") {
                    estShipWe.currentStatusId = "CAL_CONFIRMED"
                }
                if (parameters.statusId == "SHIPMENT_SHIPPED") {
                    estShipWe.currentStatusId = "CAL_COMPLETED"
                }
            }
            Map estShipWeUpdMap = [:]
            estShipWeUpdMap << estShipWe
            Map serviceResult = run service: "updateWorkEffort", with: estShipWeUpdMap
            if (!ServiceUtil.isSuccess(serviceResult)) return error(serviceResult.errorMessage)

        }
    }
    if ((parameters.estimatedArrivalDate
            && parameters.estimatedArrivalDate != lookedUpValue.estimatedArrivalDate)
            || (parameters.destinationFacilityId
            && parameters.destinationFacilityId != lookedUpValue.destinationFacilityId)) {
        GenericValue estimatedArrivalWorkEffort = from("WorkEffort")
                .where(workEffortId: lookedUpValue.estimatedArrivalWorkEffId)
                .queryOne()
        if (estimatedArrivalWorkEffort) {
            estimatedArrivalWorkEffort.estimatedStartDate = parameters.estimatedArrivalDate
            estimatedArrivalWorkEffort.estimatedCompletionDate = parameters.estimatedArrivalDate
            estimatedArrivalWorkEffort.facilityId = parameters.destinationFacilityId
            Map serviceResult = run service: "updateWorkEffort", with: estimatedArrivalWorkEffort
            if (!ServiceUtil.isSuccess(serviceResult)) return error(serviceResult.errorMessage)
        }
    }
    // if the partyIdTo or partyIdFrom has changed, add WEPAs
    //TODO REFACTORING
    if (parameters.partyIdFrom
            && parameters.partyIdFrom != lookedUpValue.partyIdFrom
            && lookedUpValue.estimatedShipWorkEffId) {
        Map assignPartyToWorkEffortShip = [workEffortId: lookedUpValue.estimatedShipWorkEffId,
                                           partyId: parameters.partyIdFrom]
        List existingShipWepas = from("WorkEffortPartyAssignment")
                .where(assignPartyToWorkEffortShip)
                .filterByDate()
                .queryList()
        if (!existingShipWepas) {
            assignPartyToWorkEffortShip.roleTypeId = "CAL_ATTENDEE"
            assignPartyToWorkEffortShip.statusId = "CAL_SENT"
            Map serviceResult = run service: "assignPartyToWorkEffort", with: assignPartyToWorkEffortShip
            if (!ServiceUtil.isSuccess(serviceResult)) return error(serviceResult.errorMessage)
        }
    }
    if (parameters.partyIdTo
            && parameters.partyIdTo != lookedUpValue.partyIdTo
            && lookedUpValue.estimatedArrivalWorkEffId) {
        Map assignPartyToWorkEffortArrival = [workEffortId: lookedUpValue.estimatedArrivalWorkEffId,
                                              partyId: parameters.partyIdTo]
        List existingArrivalWepas = from("WorkEffortPartyAssignment")
                .where(assignPartyToWorkEffortArrival)
                .filterByDate()
                .queryList()
        if (!existingArrivalWepas) {
            assignPartyToWorkEffortArrival.roleTypeId = "CAL_ATTENDEE"
            assignPartyToWorkEffortArrival.statusId = "CAL_SENT"
            serviceResult = run service: "assignPartyToWorkEffort", with: assignPartyToWorkEffortArrival
            if (!ServiceUtil.isSuccess(serviceResult)) return error(serviceResult.errorMessage)
        }
    }
    return result
}
