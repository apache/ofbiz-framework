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
package org.apache.ofbiz.common

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityConditionBuilder
import org.apache.ofbiz.service.ServiceUtil

/**
 * Moves a PortalPortlet from the actual portalPage to a different one
 * @return Success response
 */
Map movePortletToPortalPage() {
    Map checkIsOwner = checkOwnerShip()
    if (ServiceUtil.isError(checkIsOwner)) {
        return checkIsOwner
    }
    GenericValue sourcePortalPagePortlet = from('PortalPagePortlet').where(parameters).cache().queryOne()
    GenericValue targetPortalPortlet = makeValue('PortalPagePortlet', [*: parameters,
                                                                       portalPageId: copyIfRequiredSystemPage(),
                                                                       columnNum: 1])
    delegator.setNextSubSeqId(targetPortalPortlet, 'portletSeqId', 5, 1)
    targetPortalPortlet.create()
    sourcePortalPagePortlet.remove()
}

/**
 * Add a new Column to a PortalPage
 * @return Success response with the columnSeqId created
 */
Map addPortalPageColumn() {
    Map checkIsOwner = checkOwnerShip()
    if (ServiceUtil.isError(checkIsOwner)) {
        return checkIsOwner
    }
    GenericValue portalPageColumn = makeValue('PortalPageColumn', parameters)
    if (!portalPageColumn.columnSeqId) {
        delegator.setNextSubSeqId(portalPageColumn, 'columnSeqId', 5, 1)
    }
    portalPageColumn.create()
    return success([columnSeqId: portalPageColumn.columnSeqId])
}

/**
 * Delete a Column from a PortalPage
 * @return Success response after delete
 */
Map deletePortalPageColumn() {
    Map checkIsOwner = checkOwnerShip()
    if (ServiceUtil.isError(checkIsOwner)) {
        return checkIsOwner
    }
    GenericValue column = from('PortalPageColumn').where(parameters).queryOne()
    column.getRelated('PortalPagePortlet', null, null, false).each {
        run service: 'deletePortalPagePortlet', with: it.getAllFields()
    }
    column.remove()
    return success()
}
/**
 * Add a registered PortalPortlet to a PortalPage
 * @return Success response with the portletSeqId created
 */
Map createPortalPagePortlet() {
    Map checkIsOwner = checkOwnerShip()
    if (ServiceUtil.isError(checkIsOwner)) {
        return checkIsOwner
    }
    GenericValue portalPagePortlet = makeValue('PortalPagePortlet', parameters)
    GenericValue lastPortalPagePortlet = from('PortalPagePortlet')
            .where(portalPageId: parameters.portalPageId)
            .orderBy('-sequenceNum')
            .queryFirst()
    portalPagePortlet.sequenceNum = lastPortalPagePortlet ? lastPortalPagePortlet.sequenceNum + 1 : 1
    delegator.setNextSubSeqId(portalPagePortlet, 'portletSeqId', 5, 1)
    portalPagePortlet.create()
    return success([portletSeqId: portalPagePortlet.portletSeqId])
}

/**
 * Delete a PortalPortlet from a PortalPageColumn
 * @return Success response after delete
 */
Map deletePortalPagePortlet() {
    Map checkIsOwner = checkOwnerShip()
    if (ServiceUtil.isError(checkIsOwner)) {
        return checkIsOwner
    }
    GenericValue portalPagePortlet = from('PortalPagePortlet').where(parameters).queryOne()
    if (portalPagePortlet) {
        delegator.removeByAnd('PortletAttribute', [portalPageId: portalPagePortlet.portalPageId,
                                                   portalPortletId: portalPagePortlet.portalPortletId,
                                                   portletSeqId: portalPagePortlet.portletSeqId])
        portalPagePortlet.remove()
    }
    return success()
}

/**
 * Get all attributes of a Portlet either by providing userLogin or portalPageId with portalPortletId
 * @return Success response with all attributes
 */
Map getPortletAttributes() {
    if (!parameters.ownerUserLoginId && !parameters.portalPageId) {
        return error('Service getPortletAttributes did not receive either ownerUserLoginId OR portalPageId')
    }
    if (parameters.ownerUserLoginId) {
        GenericValue portalPagePortlet = from('PortalPageAndPortlet')
                .where(ownerUserLoginId: parameters.ownerUserLoginId,
                        portalPortletId: parameters.portalPortletId)
                .queryFirst()
        parameters.portalPageId = portalPagePortlet.portalPageId
    }
    EntityCondition condition = new EntityConditionBuilder().AND {
        EQUALS(portalPageId: parameters.portalPageId)
        EQUALS(portalPortletId: parameters.portalPortletId)
        if (parameters.portletSeqId) {
            EQUALS(portletSeqId: parameters.portletSeqId)
        }
    }
    Map attributeMap = [:]
    from ('PortletAttribute')
            .where(condition)
            .queryList()
            .each {
                    attributeMap.(it.attrName) = it.attrValue
            }
    return success([attributeMap: attributeMap])
}

/**
 * Create a new Portal Page
 * @return Success response after creation with the portalPageId
 */
Map createPortalPage() {
    GenericValue newPortalPage = makeValue('PortalPage', parameters)
    newPortalPage.portalPageId = newPortalPage.portalPageId ?: delegator.getNextSeqId('PortalPage')
    newPortalPage.ownerUserLoginId = parameters.userLogin.userLoginId
    if (! newPortalPage.sequenceNum) {
        delegator.setNextSubSeqId(newPortalPage, 'sequenceNum', 5, 1)
    }
    newPortalPage.create()
    return success([portalPageId: newPortalPage.portalPageId])
}

/**
 * Delete a new Portal Page
 * @return Success response after delete
 */
Map deletePortalPage() {
    Map checkIsOwner = checkOwnerShip()
    if (ServiceUtil.isError(checkIsOwner)) {
        return checkIsOwner
    }
    GenericValue portalPage = from('PortalPage').where(parameters).queryOne()
    if (portalPage.originalPortalPageId) {
        GenericValue originalPortalPage = from('PortalPage').where(portalPageId: portalPage.originalPortalPageId).cache().queryOne()
        GenericValue portalPageToUpdate = from('PortalPage')
                .where(sequenceNum: originalPortalPage.sequenceNum,
                        ownerUserLoginId: userLogin.userLoginId,
                        parentPortalPageId: parameters.parentPortalPageId)
                .queryFirst()
        if (portalPageToUpdate) {
            run service: 'updatePortalPage', with: [*: portalPageToUpdate.getAllFields(),
                                                    portalPageId: portalPageToUpdate.portalPageId,
                                                    sequenceNum: portalPage.sequenceNum]
        }
    }
    portalPage.removeRelated('PortalPageColumn')
    portalPage.removeRelated('PortalPagePortlet')
    portalPage.remove()
    return success()
}

/**
 * Update the portal page sequence numbers
 * @return Success response after update
 */
Map updatePortalPageSeq() {
    Map checkIsOwner = checkOwnerShip()
    if (ServiceUtil.isError(checkIsOwner)) {
        return checkIsOwner
    }
    GenericValue portalPage = from('PortalPage').where(parameters).queryOne()
    String mode = parameters.mode
    String orderBy = (['UP', 'BOT'].contains(mode) ? '-' : '') + 'sequenceNum'
    EntityCondition condition = new EntityConditionBuilder().AND {
        EQUALS(ownerUserLoginId: userLogin.userLoginId)
        if (['UP', 'TOP'].contains(mode)) {
            LESS_THAN(sequenceNum: portalPage.sequenceNum)
        } else {
            GREATER_THAN(sequenceNum: portalPage.sequenceNum)
        }
        EQUALS(ownerUserLoginId: userLogin.userLoginId)
        OR {
            EQUALS(parentPortalPageId: parameters.parentPortalPageId)
            EQUALS(originalPortalPageId: parameters.parentPortalPageId)
        }
    }
    GenericValue updatePortalPage = from('PortalPage').where(condition).orderBy(orderBy).queryFirst()

    if (updatePortalPage) {
        Long previousSequenceNum = portalPage.sequenceNum
        portalPage.sequenceNum = updatePortalPage.sequenceNum
        portalPage.store()
        run service: 'updatePortalPage', with: [*: updatePortalPage.getAllFields(),
                                                sequenceNum: previousSequenceNum]
    }
    return success()
}

/**
 * Updates a portlet Seq No for the Drag and Drop Feature
 * @return Success response after move
 */
Map updatePortletSeqDragDrop() {
    Map checkIsOwner = checkOwnerShip()
    if (ServiceUtil.isError(checkIsOwner)) {
        return checkIsOwner
    }
    // update Portlet Seq with Drag & Drop
    parameters.portalPageId = parameters.o_portalPageId
    GenericValue originPp = from('PortalPagePortlet')
            .where(portalPageId: parameters.o_portalPageId,
                    portalPortletId: parameters.o_portalPortletId,
                    portletSeqId: parameters.o_portletSeqId)
            .queryOne()
    if (!originPp) {
        return error('')
    }
    String columnSeqId = parameters.destinationColumn ?: originPp.columnSeqId
    GenericValue destiPp = from('PortalPagePortlet')
            .where(portalPageId: parameters.d_portalPageId,
                    portalPortletId: parameters.d_portalPortletId,
                    portletSeqId: parameters.d_portletSeqId)
            .queryOne()

    int newSequenceNo = 0
    if (parameters.mode != 'NEW') {
        EntityCondition condition = new EntityConditionBuilder().AND {
            EQUALS(portalPageId: parameters.portalPageId)
            EQUALS(columnSeqId: columnSeqId)
            if (parameters.mode == 'DRAGDROPBEFORE') {
                GREATER_THAN_EQUAL_TO(sequenceNum: destiPp.sequenceNum)
                if (originPp.sequenceNum) {
                    LESS_THAN(sequenceNum: originPp.sequenceNum)
                }
            } else {
                GREATER_THAN_EQUAL_TO(sequenceNum: originPp.sequenceNum)
                if (destiPp.sequenceNum) {
                    LESS_THAN(sequenceNum: destiPp.sequenceNum)
                }
            }
        }

        newSequenceNo = destiPp.sequenceNum
        int increase = parameters.mode == 'DRAGDROPBEFORE' ? 1 : -1
        from('PortalPagePortlet')
                .where(condition)
                .orderBy((parameters.mode == 'DRAGDROPBEFORE' ? '' : '-') + 'sequenceNum')
                .queryList()
                .each {
                    if (it.sequenceNum) {
                        it.sequenceNum = it.sequenceNum + increase
                        increase += increase
                    } else {
                        it.sequenceNum = newSequenceNo
                    }
                    it.store()
                }
    }
    originPp.columnSeqId = columnSeqId
    originPp.sequenceNum = newSequenceNo
    originPp.store()
    return success()
}

/**
 * Duplicate content of portalPage, portalPageColumn, portalPagePortlet, portletAttribute,
 * this method should be call with parameters.toPortalPageId and portalPage in context
 * @return Success response
 */
Map duplicatePortalPageDetails() {
    logInfo("duplicate portalPage detail from parameters.toPortalPageId ${parameters.fromPortalPageId}" +
            " to new portalPageId=${parameters.toPortalPageId}")
    if (parameters.toPortalPageId) {
        from('PortalPageColumn')
                .where(portalPageId: parameters.fromPortalPageId)
                .queryList()
                .each {
                    run service: 'addPortalPageColumn', with: [*: it.getAllFields(),
                                                               portalPageId: parameters.toPortalPageId]
                }
        from('PortalPagePortlet')
                .where(portalPageId: parameters.fromPortalPageId)
                .queryList()
                .each {
                    run service: 'createPortalPagePortlet', with: [*: it.getAllFields(),
                                                                   portalPageId: parameters.toPortalPageId]
                    from('PortletAttribute')
                            .where(portalPageId: parameters.fromPortalPageId,
                                    portalPortletId: it.portalPortletId,
                                    portletSeqId: it.portletSeqId)
                            .queryList()
                            .each { attr ->
                                attr.portalPageId = parameters.toPortalPageId
                                attr.create()
                            }
                }
    }
    return success()
}

/**
 * Check the ownership of a Portal Page
 */
private Map checkOwnerShip() {
    if (!parameters.portalPageId) {
        return [:]
    }
    GenericValue portalPage = from('PortalPage').where(parameters).cache().queryOne()
    if (!portalPage) {
        return error(label('CommonUiLabels', 'PortalPageNotFound', parameters))
    }

    // only page owner or user with MYPORTALBASE_ADMIN can modify the page detail
    if (portalPage.ownerUserLoginId != userLogin.userLoginId &&
            !security.hasPermission('MYPORTALBASE_ADMIN', userLogin)) {
        return error(label('CommonUiLabels', 'PortalPageNotOwned', portalPage))
    }
    return success()
}

/**
 * Check if the page is a system page, then copy before allowing
 */
private String copyIfRequiredSystemPage() {
    GenericValue portalPage = from('PortalPage').where(parameters).cache().queryOne()
    Map serviceResult = [:]
    if (portalPage && portalPage.ownerUserLoginId == '_NA_' && from('PortalPage')
            .where(originalPortalPageId: parameters.portalPageId,
                    ownerUserLoginId: userLogin.userLoginId)
            .queryCount() == 0 ) {
        // copy the portal page
        serviceResult = run service: 'createPortalPage', with: [*: portalPage.getAllFields(),
                                                                portalPageId: null,
                                                                originalPortalPageId: portalPage.portalPageId,
                                                                ownerUserLoginId: userLogin.userLoginId]
        run service: 'duplicatePortalPageDetails', with: [fromPortalPageId: parameters.portalPageId,
                                                          toPortalPageId: serviceResult.portalPageId]
    }
    return serviceResult ? serviceResult.portalPageId : portalPage?.portalPageId
}
