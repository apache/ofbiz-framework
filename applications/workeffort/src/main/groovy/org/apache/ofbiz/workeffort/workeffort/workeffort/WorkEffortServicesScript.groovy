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
package org.apache.ofbiz.workeffort.workeffort.workeffort

import org.apache.ofbiz.entity.model.ModelEntity
import org.apache.ofbiz.entity.util.EntityQuery
import org.apache.ofbiz.workeffort.workeffort.WorkEffortPartyAssignmentServices

import java.sql.Timestamp

import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.model.ModelKeyMap
import org.apache.ofbiz.entity.util.EntityUtil
import org.apache.ofbiz.service.ServiceUtil

Map checkAndCreateWorkEffort() {
    Map result = success()
    /*
     * if needed create some WorkEfforts and remember their IDs:
     * estimatedShipDate: estimatedShipWorkEffId
     * estimatedArrivalDate: estimatedArrivalWorkEffId
     */
    GenericValue lookedUpValue = from('Shipment').where(parameters).queryOne()
    if (parameters.estimatedShipDate) {
        Map shipWorkEffortMap = [workEffortName: "Shipment #${parameters.shipmentId} ${parameters.primaryOrderId} Ship",
                                 currentStatusId: 'CAL_TENTATIVE',
                                 workEffortPurposeTypeId: 'WEPT_WAREHOUSING',
                                 estimatedStartDate: parameters.estimatedShipDate,
                                 estimatedCompletionDate: parameters.estimatedShipDate,
                                 facilityId: parameters.originFacilityId,
                                 quickAssignPartyId: userLogin.partyId]
        if (['OUTGOING_SHIPMENT', 'SALES_SHIPMENT', 'PURCHASE_RETURN'].contains(shipmentTypeId)) {
            shipWorkEffortMap.workEffortTypeId = 'SHIPMENT_OUTBOUND'
        }
        Map serviceResult = run service: 'createWorkEffort', with: shipWorkEffortMap
        if (!ServiceUtil.isSuccess(serviceResult)) {
            return error(serviceResult.errorMessage)
        }
        lookedUpValue.estimatedShipWorkEffId = serviceResult.workEffortId
        if (parameters.partyIdFrom) {
            serviceResult = run service: 'assignPartyToWorkEffort', with: [workEffortId: lookedUpValue.estimatedShipWorkEffId,
                                                           partyId: parameters.partyIdFrom,
                                                           roleTypeId: 'CAL_ATTENDEE',
                                                           statusId: 'CAL_SENT']
            if (!ServiceUtil.isSuccess(serviceResult)) {
                return error(serviceResult.errorMessage)
            }
        }
    }
    if (parameters.estimatedArrivalDate) {
        Map arrivalWorkEffortMap = [workEffortName: "Shipment #${parameters.shipmentId} ${parameters.primaryOrderId} Arrival",
                                    currentStatusId: 'CAL_TENTATIVE',
                                    workEffortPurposeTypeId: 'WEPT_WAREHOUSING',
                                    estimatedStartDate: parameters.estimatedArrivalDate,
                                    estimatedCompletionDate: parameters.estimatedArrivalDate,
                                    facilityId: parameters.destinationFacilityId,
                                    quickAssignPartyId: userLogin.partyId]
        if (['INCOMING_SHIPMENT', 'PURCHASE_SHIPMENT', 'SALES_RETURN'].contains(shipmentTypeId)) {
            arrivalWorkEffortMap.workEffortTypeId = 'SHIPMENT_INBOUND'
        }
        Map serviceResult = run service: 'createWorkEffort', with: arrivalWorkEffortMap
        if (!ServiceUtil.isSuccess(serviceResult)) {
            return error(serviceResult.errorMessage)
        }
        lookedUpValue.estimatedArrivalWorkEffId = serviceResultAD.workEffortId
        if (parameters.partyIdTo) {
            serviceResult = run service: 'assignPartyToWorkEffort', with: [workEffortId: lookedUpValue.estimatedArrivalWorkEffId,
                                                           partyId: parameters.partyIdTo,
                                                           roleTypeId: 'CAL_ATTENDEE',
                                                           statusId: 'CAL_SENT']
            if (!ServiceUtil.isSuccess(serviceResult)) {
                return error(serviceResult.errorMessage)
            }
        }
    }
    lookedUpValue.store()
    return result
}

Map checkAndUpdateWorkEffort() {
    Map result = success()
    GenericValue lookedUpValue = from('Shipment').where(parameters).queryOne()
    // Check the pickup and delivery dates for changes and update the corresponding WorkEfforts
    if ((parameters.estimatedShipDate && parameters.estimatedShipDate != lookedUpValue.estimatedShipDate)
            || (parameters.originFacilityId && parameters.originFacilityId != lookedUpValue.originFacilityId)
            || (parameters.statusId && parameters.statusId != lookedUpValue.statusId
            && ['SHIPMENT_CANCELLED', 'SHIPMENT_PACKED', 'SHIPMENT_SHIPPED'].contains(parameters.statusId))) {
        GenericValue estShipWe = from('WorkEffort').where(workEffortId: lookedUpValue.estimatedShipWorkEffId).queryOne()
        if (estShipWe) {
            estShipWe.estimatedStartDate = parameters.estimatedShipDate
            estShipWe.estimatedCompletionDate = parameters.estimatedShipDate
            estShipWe.facilityId = parameters.originFacilityId
            if ((parameters.statusId) && (parameters.statusId != lookedUpValue.statusId)) {
                switch (parameters.statusId) {
                    case 'SHIPMENT_CANCELLED':
                        estShipWe.currentStatusId = 'CAL_CANCELLED'
                        break
                    case 'SHIPMENT_PACKED':
                        estShipWe.currentStatusId = 'CAL_CONFIRMED'
                        break
                    case 'SHIPMENT_SHIPPED':
                        estShipWe.currentStatusId = 'CAL_COMPLETED'
                        break
                }
            }
            Map estShipWeUpdMap = [:]
            estShipWeUpdMap << estShipWe
            Map serviceResult = run service: 'updateWorkEffort', with: estShipWeUpdMap
            if (!ServiceUtil.isSuccess(serviceResult)) {
                return error(serviceResult.errorMessage)
            }
        }
    }
    if ((parameters.estimatedArrivalDate
            && parameters.estimatedArrivalDate != lookedUpValue.estimatedArrivalDate)
            || (parameters.destinationFacilityId
            && parameters.destinationFacilityId != lookedUpValue.destinationFacilityId)) {
        GenericValue estimatedArrivalWorkEffort = from('WorkEffort')
                .where(workEffortId: lookedUpValue.estimatedArrivalWorkEffId)
                .queryOne()
        if (estimatedArrivalWorkEffort) {
            estimatedArrivalWorkEffort.estimatedStartDate = parameters.estimatedArrivalDate
            estimatedArrivalWorkEffort.estimatedCompletionDate = parameters.estimatedArrivalDate
            estimatedArrivalWorkEffort.facilityId = parameters.destinationFacilityId
            Map serviceResult = run service: 'updateWorkEffort', with: estimatedArrivalWorkEffort
            if (!ServiceUtil.isSuccess(serviceResult)) {
                return error(serviceResult.errorMessage)
            }
        }
    }
    // if the partyIdTo or partyIdFrom has changed, add WEPAs
    //TODO REFACTORING
    if (parameters.partyIdFrom
            && parameters.partyIdFrom != lookedUpValue.partyIdFrom
            && lookedUpValue.estimatedShipWorkEffId) {
        Map assignPartyToWorkEffortShip = [workEffortId: lookedUpValue.estimatedShipWorkEffId,
                                           partyId: parameters.partyIdFrom]
        List existingShipWepas = from('WorkEffortPartyAssignment')
                .where(assignPartyToWorkEffortShip)
                .filterByDate()
                .queryList()
        if (!existingShipWepas) {
            assignPartyToWorkEffortShip.roleTypeId = 'CAL_ATTENDEE'
            assignPartyToWorkEffortShip.statusId = 'CAL_SENT'
            Map serviceResult = run service: 'assignPartyToWorkEffort', with: assignPartyToWorkEffortShip
            if (!ServiceUtil.isSuccess(serviceResult)) {
                return error(serviceResult.errorMessage)
            }
        }
    }
    if (parameters.partyIdTo
            && parameters.partyIdTo != lookedUpValue.partyIdTo
            && lookedUpValue.estimatedArrivalWorkEffId) {
        Map assignPartyToWorkEffortArrival = [workEffortId: lookedUpValue.estimatedArrivalWorkEffId,
                                              partyId: parameters.partyIdTo]
        List existingArrivalWepas = from('WorkEffortPartyAssignment')
                .where(assignPartyToWorkEffortArrival)
                .filterByDate()
                .queryList()
        if (!existingArrivalWepas) {
            assignPartyToWorkEffortArrival.roleTypeId = 'CAL_ATTENDEE'
            assignPartyToWorkEffortArrival.statusId = 'CAL_SENT'
            serviceResult = run service: 'assignPartyToWorkEffort', with: assignPartyToWorkEffortArrival
            if (!ServiceUtil.isSuccess(serviceResult)) {
                return error(serviceResult.errorMessage)
            }
        }
    }
    return result
}

/**
 * Create Work Effort and assign to a party with a role
 */
Map createWorkEffortAndPartyAssign() {
    GenericValue partyRole = from('PartyRole').where(parameters).cache().queryOne()
    if (!partyRole) {
        GenericValue roleType = from('RoleType').where(parameters).cache().queryOne()
        return error(label('PartyErrorUiLabels', 'PartyRoleAssociationRequired', [partyId: parameters.partyId,
                                                                                  roleDescription: roleType.get('description', locale)]))
    }
    Map serviceResult = run service: 'createWorkEffort', with: parameters
    String workEffortId  = serviceResult.workEffortId

    run service: 'createWorkEffortPartyAssignment', with: [*: parameters,
                                                           workEffortId: workEffortId,
                                                           assignedByUserLoginId: userLogin.userLoginId]
    return success([workEffortId: workEffortId])
}

/**
 * Create Work Effort
 * @return Success response containing the workEffortId, error response otherwise.
 */
Map createWorkEffort() {
    GenericValue workEffort = makeValue('WorkEffort', parameters)
    workEffort.workEffortId = workEffort.workEffortId ?: delegator.getNextSeqId('WorkEffort')
    String errMsg = UtilValidate.checkValidDatabaseId(workEffort.workEffortId)
    if (errMsg) {
        return error(errMsg)
    }

    Timestamp now = UtilDateTime.nowTimestamp()
    workEffort.setFields([lastStatusUpdate: now,
                          lastModifiedDate: now,
                          createdDate: now,
                          revisionNumber: 1,
                          lastModifiedByUserLogin: userLogin.userLoginId,
                          createdByUserLogin: userLogin.userLoginId])
    workEffort.create()

    // create new status entry, and set lastStatusUpdate date
    run service: 'createWorkEffortStatus', with: [workEffortId: workEffort.workEffortId,
                                                  statusId: workEffort.currentStatusId,
                                                  statusDatetime: now,
                                                  setByUserLogin: userLogin.userLoginId]

    return success([workEffortId: workEffort.workEffortId])
}

/**
 * Update Work Effort
 * @return Success, error response otherwise.
 */
Map updateWorkEffort() {
    GenericValue workEffort = from('WorkEffort').where(parameters).queryOne()
    Timestamp now = UtilDateTime.nowTimestamp()

    // check if the status change is a valid change
    if (parameters.currentStatusId && workEffort.currentStatusId &&
            parameters.currentStatusId != workEffort.currentStatusId) {
        Map statusValidChange = [statusId: workEffort.currentStatusId, statusIdTo: parameters.currentStatusId]
        if (from('StatusValidChange').where(statusValidChange).queryCount() == 0) {
            return error(label('WorkEffortUiLabels', 'WorkEffortStatusChangeNotValid', statusValidChange))
        }
        run service: 'createWorkEffortStatus', with: [*: parameters,
                                                      statusId: parameters.currentStatusId,
                                                      statusDatetime: now,
                                                      setByUserLogin: userLogin.userLoginId]
    }

    // only save if something has changed
    GenericValue workEffortOrigin = workEffort.clone()
    workEffort.setNonPKFields(parameters)
    if (workEffortOrigin != workEffort) {
        workEffort.lastModifiedDate = now
        workEffort.lastModifiedByUserLogin = userLogin.userLoginId
        workEffort.revisionNumber = (workEffort.revisionNumber ?: 0) + 1
        workEffort.store()
    }
    return success()
}

/**
 * Delete Work Effort
 * @return Success, error response otherwise.
 */
Map deleteWorkEffort() {
    // check permissions before moving on: if update or delete logged in user must be associated OR have corresponding UPDATE or DELETE permissions
    if (from('WorkEffortPartyAssignment')
                    .where(workEffortId: parameters.workEffortId,
                    partyId: userLogin.partyId)
                    .queryCount() == 0 &&
                    security.hasPermission('WORKEFFORTMGR_DELETE', userLogin)) {
        return error(label('WorkEffortUiLabels', 'WorkEffortDeletePermissionError'))
    }

    GenericValue workEffort = from('WorkEffort').where(parameters).queryOne()

    // Remove associated/dependent entries from other entities here
    ['WorkEffortKeyword', 'WorkEffortAttribute',
     'WorkOrderItemFulfillment', 'FromWorkEffortAssoc',
     'ToWorkEffortAssoc', 'NoteData', 'RecurrenceInfo',
     'RuntimeData', 'WorkEffortPartyAssignment',
     'WorkEffortFixedAssetAssign', 'WorkEffortSkillStandard',
     'WorkEffortStatus', 'WorkEffortContent'].each {
        workEffort.removeRelated(it)
    }
    workEffort.remove()
    return success()
}

/**
 * Copy Work Effort
 * @return Success response containing the workEffortId, error response otherwise.
 */
Map copyWorkEffort() {
    GenericValue sourceWorkEffort = from('WorkEffort').where(workEffortId: parameters.sourceWorkEffortId).queryOne()
    if (!sourceWorkEffort) {
        return error(label('WorkEffortUiLabels', 'WorkEffortNotFound', [errorString: parameters.sourceWorkEffortId]))
    }
    Map serviceResult = run service: 'createWorkEffort', with: [*           : sourceWorkEffort.getAllFields(),
                                                                workEffortId: parameters.targetWorkEffortId]
    GenericValue targetWorkEffort = from('WorkEffort').where(workEffortId: serviceResult.workEffortId).queryOne()
    if (parameters.copyWorkEffortAssocs == 'Y') {
        run service: 'copyWorkEffortAssocs', with: [*                 : parameters,
                                                    targetWorkEffortId: targetWorkEffort.workEffortId]
    }

    if (parameters.copyRelatedValues == 'Y') {
        excludeExpiredRelations = parameters.excludeExpiredRelations
        delegator.getModelEntity('WorkEffort').getRelationsManyList().each {
            if (it.getRelEntityName() != 'WorkEffortAssoc') {
                String relationName = it.getCombinedName()
                ModelKeyMap keyMap = it.findKeyMap('workEffortId')
                if (keyMap) {
                    String relationWorkEffortId = keyMap.getRelFieldName()
                    List<GenericValue> relationValues = sourceWorkEffort.getRelated(relationName, null, null, false)
                    if (parameters.excludeExpiredRelations == 'Y') {
                        relationValues = EntityUtil.filterByDate(relationValues)
                    }
                    relationValues.each { relationValue ->
                        GenericValue targetRelationValue = relationValue.clone()
                        targetRelationValue[relationWorkEffortId] = targetWorkEffort.workEffortId
                        if (!from(targetRelationValue.getEntityName()).where(targetRelationValue.getAllFields()).queryOne()) {
                            targetRelationValue.create()
                        }
                    }
                }
            }
        }
    }
    return success(workEffortId: targetWorkEffort.workEffortId)
}

/**
 * Duplicate a WorkEffort
 * @return Success response containing the workEffortId, error response otherwise.
 */
Map duplicateWorkEffort() {
    if ((parameters.removeWorkEffortAssocs == 'Y' ||
            parameters.removeWorkEffortContents == 'Y' ||
            parameters.removeWorkEffortNotes == 'Y' ||
            parameters.removeWorkEffortAssignmentRates == 'Y') &&
            security.hasPermission('WORKEFFORTMGR_DELETE', userLogin)) {
        return error(label('WorkEffortUiLabels', 'WorkEffortDeletePermissionError'))
    }
    String workEffortId = parameters.workEffortId ?: delegator.getNextSeqId('WorkEffort')
    GenericValue oldWorkEffort = from('WorkEffort').where(workEffortId: parameters.oldWorkEffortId).queryOne()
    GenericValue duplicateWorkEffort = oldWorkEffort.clone()
    duplicateWorkEffort.workEffortId = workEffortId

    // Check the status to give to the new WorkEffort
    if (parameters.statusId) {
        duplicateWorkEffort.currentStatusId = parameters.statusId
    } else {
        GenericValue oldStatus = from('StatusItem').where(statusId: oldWorkEffort.currentStatusId).cache().queryOne()
        GenericValue firstStatus = from('StatusItem')
                .where(statusTypeId: oldStatus.statusTypeId)
                .orderBy('sequenceId')
                .queryFirst()
        duplicateWorkEffort.currentStatusId = firstStatus.statusId
    }

    // Create the new WorkEffort from the old one and the status
    run service: 'createWorkEffort', with: duplicateWorkEffort.getAllFields()

    if (parameters.duplicateWorkEffortAssocs == 'Y') {
        ['From', 'To'].each {
            duplicateWorkEffortAssoc('WorkEffortAssoc', oldWorkEffort.workEffortId, workEffortId, "workEffortId$it")
        }
    }

    if (parameters.duplicateWorkEffortNotes == 'Y') {
        duplicateWorkEffortAssoc('WorkEffortNote', oldWorkEffort.workEffortId, workEffortId)
    }
    if (parameters.duplicateWorkEffortContents == 'Y') {
        duplicateWorkEffortAssoc('WorkEffortContent', oldWorkEffort.workEffortId, workEffortId)
    }
    if (parameters.duplicateWorkEffortAssignmentRates == 'Y') {
        duplicateWorkEffortAssoc('RateAmount', oldWorkEffort.workEffortId, workEffortId)
    }
    Map removeWorkEffortMap = [workEffortId: oldWorkEffort.workEffortId]
    if (parameters.removeWorkEffortAssocs == 'Y') {
        delegator.removeByAnd('WorkEffortAssoc', removeWorkEffortMap)
    }
    if (parameters.removeWorkEffortNotes == 'Y') {
        delegator.removeByAnd('WorkEffortNote', removeWorkEffortMap)
    }
    if (parameters.removeWorkEffortContents == 'Y') {
        delegator.removeByAnd('WorkEffortContent', removeWorkEffortMap)
    }
    if (parameters.removeWorkEffortAssignmentRates == 'Y') {
        delegator.removeByAnd('RateAmount', removeWorkEffortMap)
    }
    return success([workEffortId: workEffortId])
}

/**
 * duplicate entity relation during a workEffort duplication process
 */
void duplicateWorkEffortAssoc(String relationEntityName, String oldWorkEffortId,
                              String workEffortId, String relationFieldName = 'workEffortId') {
    ModelEntity modelEntity = delegator.getModelEntity(relationFieldName)
    EntityQuery entities = from(relationEntityName)
            .where((relationFieldName): oldWorkEffortId)
    if (modelEntity.getField('fromDate')) {
        entities.filterByDate()
    }
    entities.queryList()
            .each { oldAssoc ->
                GenericValue duplicateAssoc = oldAssoc.clone()
                duplicateAssoc.(relationFieldName) = workEffortId
                duplicateAssoc.create()
            }
}

/**
 * For a custRequest accepted link it to a workEffort and duplicate content
 * @return Success, error response otherwise.
 */
Map assocAcceptedCustRequestToWorkEffort() {
    // check status of customer request if valid
    GenericValue custRequet = from('CustRequest').where(parameters).cache().queryOne()
    if (custRequet.statusId != 'CRQ_ACCEPTED') {
        return error(label('CommonUiLabels', 'CommonErrorStatusNotValid'))
    }

    // create customer request / work effort relation
    run service: 'createWorkEffortRequest', with: parameters

    // update status of customer request
    run service: 'setCustRequestStatus', with: [*: parameters,
                                                statusId: 'CRQ_REVIEWED']

    // duplicate content on the workEffort
    from('CustRequestContent')
            .where(custRequestId: parameters.custRequestId)
            .getFieldList('contentId')
            .each {
                run service: 'createWorkEffortContent', with: [*: parameters,
                                                               contentId: it,
                                                               workEffortContentTypeId: 'SUPPORTING_MEDIA']
            }
    return success()
}

/**
 * Assign Party to Work Effort
 * @return Success, error response otherwise.
 */
Map assignPartyToWorkEffort() {
    // check if the requested party Assignment already exist
    GenericValue assignment = from('WorkEffortPartyAssignment')
            .where(workEffortId: parameters.workEffortId,
                    partyId: parameters.partyId,
                    roleTypeId: parameters.roleTypeId)
            .filterByDate()
            .queryFirst()
    if (assignment) {
        return error(label('WorkEffortUiLabels', 'WorkEffortPartyAssignmentError', parameters))
    }

    run service: 'ensurePartyRole', with: parameters
    Timestamp now = UtilDateTime.nowTimestamp()
    parameters.fromDate = parameters.fromDate ?: now
    Map result = run service: 'createWorkEffortPartyAssignment', with: [*: parameters,
                                                                        statusDatetime: parameters.statusId ? now : null,
                                                                        assignedByUserLoginId: userLogin.userLoginId]
    assignment = from('WorkEffortPartyAssignment').where(parameters).queryOne()
    if (parameters.statusId) {
        WorkEffortPartyAssignmentServices.updateWorkflowEngine(assignment, userLogin, dispatcher)
    }
    return success([fromDate: result.fromDate])
}

/**
 * Update WorkEffortPartyAssignment entity
 * @return Success, error response otherwise.
 */
Map updatePartyToWorkEffortAssignment() {
    Map assignment = from('WorkEffortPartyAssignment').where(parameters).queryOne()
    if (assignment && parameters.statusId
            && parameters.statusId != assignment.statusId) {
        WorkEffortPartyAssignmentServices.updateWorkflowEngine(assignment, userLogin, dispatcher)
        parameters.statusDatetime = UtilDateTime.nowTimestamp()
    }
    Map result = run service: 'updateWorkEffortPartyAssignment', with: parameters
    return result
}

/**
 * Quick Assign Party To WorkEffort
 * @return Success, error response otherwise.
 */
Map quickAssignPartyToWorkEffort() {
    // add a party assignment for the creator of the event, use the list method and let the EE do the update or create...
    run service: 'ensurePartyRole', with: [partyId: parameters.quickAssignPartyId,
                                           roleTypeId: parameters.roleTypeId]
    run service: 'createWorkEffortPartyAssignment', with: [*: parameters,
                                                           partyId: parameters.quickAssignPartyId,
                                                           statusId: 'PRTYASGN_ASSIGNED']
    return success()
}

/**
 * Quick Assign Party To WorkEffort
 * if relationEntityName is present create the contactMech type related
 * and if partyId is also present, link it
 * @return Success response containing the contactMechId, error response otherwise.
 */
Map createWorkEffortContactMech() {
    String contactMechId = parameters.contactMechId
    if (!contactMechId && parameters.relationEntityName) {
        String serviceName = 'create' + (parameters.partyId ? 'Party' : '') + parameters.relationEntityName
        Map serviceResult = run service: serviceName, with: parameters
        contactMechId = serviceResult.contactMechId
    }

    if (!contactMechId) {
        if (!parameters.contactMechTypeId) {
            return error(label('WorkEffortUiLabels', 'WorkEffortRequiredFieldMissingContactMechIdOrContactMechTypeId'))
        }
        String serviceName = 'create' + (parameters.relationEntityName ?: 'ContactMech')
        Map serviceResult = run service: serviceName, with: parameters
        contactMechId = serviceResult.contactMechId
    }

    GenericValue workEffortContactMech = makeValue('WorkEffortContactMech', [*: parameters,
                                                                             fromDate: UtilDateTime.nowTimestamp(),
                                                                             contactMechId: contactMechId])
    workEffortContactMech.create()
    return success([contactMechId: contactMechId])
}

/**
 * Update a WorkEffortContactMech and ContactMech related
 * @return Successs response containing the contactMechId and oldContactMechId,, error response otherwise.
 */
Map updateWorkEffortContactMech() {
    GenericValue workEffortContactMech = from('WorkEffortContactMech')
            .where(workEffortId: parameters.workEffortId,
                    contactMechId: parameters.contactMechId)
            .filterByDate()
            .queryFirst()
    if (!workEffortContactMech) {
        return error(label('WorkEffortUiLabels', 'WorkEffortCannotUpdateContactInfo'))
    }
    GenericValue newWorkEffortContactMech = workEffortContactMech.clone()
    // If we already have a new contactMechId don't update ContactMech
    String newContactMechId = parameters.newContactMechId
    if (!newContactMechId && parameters.relationEntityName) {
        String serviceName = 'update' + parameters.relationEntityName
        Map serviceResult = run service: serviceName, with: parameters
        newContactMechId = serviceResult.contactMechId
    }
    if (!newContactMechId) {
        Map serviceResult = run service: 'updateContactMech', with: parameters
        newContactMechId = serviceResult.contactMechId
    }

    if (newContactMechId != parameters.contactMechId) {
        Timestamp now = UtilDateTime.nowTimestamp()
        newWorkEffortContactMech.fromDate = now
        workEffortContactMech.thruDate = now
        newWorkEffortContactMech.contactMechId = newContactMechId
        newWorkEffortContactMech.create()
        workEffortContactMech.store()
    }
    return success([contactMechId: newContactMechId, oldContactMechId: workEffortContactMech.contactMechId])
}
