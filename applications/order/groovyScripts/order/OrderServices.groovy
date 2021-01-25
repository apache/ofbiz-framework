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


import groovy.time.TimeCategory
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.condition.EntityConditionBuilder

import java.sql.Timestamp

/**
 * Service to get the next OrderId
 */
def getNextOrderId() {
    GenericValue partyAcctgPreference
    GenericValue customMethod
    String customMethodName

    partyAcctgPreference = from('PartyAcctgPreference').where(context).queryOne()
    logInfo "In getNextOrderId partyId is [$parameters.partyId], partyAcctgPreference: $partyAcctgPreference"

    if (partyAcctgPreference) {
        customMethod = partyAcctgPreference.getRelatedOne('OrderCustomMethod', true)
    } else {
        logWarning "Acctg preference not defined for partyId [$parameters.partyId]"
    }

    if (customMethod) {
        customMethodName = customMethod.customMethodName
    } else if (partyAcctgPreference && partyAcctgPreference.oldOrderSequenceEnumId == 'ODRSQ_ENF_SEQ') {
        customMethodName = 'orderSequence_enforced'
    }

    if (customMethodName) {
        Map customMethodMap = [*: parameters]
        Map result = run service: customMethodName, with: customMethodMap
        orderIdTemp = result.orderId
    } else {
        logInfo 'In getNextOrderId sequence by Standard'
        // default to the default sequencing: ODRSQ_STANDARD
        orderIdTemp = parameters.orderId ?: delegator.getNextSeqId('OrderHeader')
    }

    GenericValue productStore = null
    if (parameters.productStoreId) {
        productStore = from('ProductStore').where(context).queryOne()
    }

    // use orderIdTemp along with the orderIdPrefix to create the real ID
    String orderId = ''
    if (productStore) orderId += productStore.orderNumberPrefix ?: ""
    if (partyAcctgPreference) orderId += partyAcctgPreference.orderIdPrefix ?: ""
    orderId += orderIdTemp.toString()

    Map result = success()
    result.orderId = orderId

    return result
}

/**
 * Service to get Summary Information About Orders for a Customer
 */
def getOrderedSummaryInformation() {
    /*
    // The permission checking is commented out to make this service work also when triggered from ecommerce
    if (!security.hasEntityPermission('ORDERMGR', '_VIEW', session && !parameters.partyId.equals(userLogin.partyId))) {
        Map result = error('To get order summary information you must have the ORDERMGR_VIEW permission, or
        be logged in as the party to get the summary information for.')
        return result
    }
    */
    Timestamp fromDate = null, thruDate = null
    Date now = new Date()
    if (monthsToInclude) {
        use(TimeCategory) {
            thruDate = now.toTimestamp()
            fromDate = (now - monthsToInclude.months).toTimestamp()
        }
    }

    roleTypeId = roleTypeId ?: 'PLACING_CUSTOMER'
    orderTypeId = orderTypeId ?: 'SALES_ORDER'
    statusId = statusId ?: 'ORDER_COMPLETED'

    //find the existing exchange rates
    exprBldr = new EntityConditionBuilder()

    def condition = exprBldr.AND() {
        EQUALS(partyId: partyId)
        EQUALS(roleTypeId: roleTypeId)
        EQUALS(orderTypeId: orderTypeId)
        EQUALS(statusId: statusId)
    }

    if (fromDate) {
        condition = exprBldr.AND(condition) {
            condition
            exprBldr.OR() {
                GREATER_THAN_EQUAL_TO(orderDate: fromDate)
                EQUALS(orderDate: null)
            }
        }
    }

    if (thruDate) {
        condition = exprBldr.AND(condition) {
            condition
            exprBldr.OR() {
                LESS_THAN_EQUAL_TO(orderDate: thruDate)
                EQUALS(orderDate: null)
            }
        }
    }

    orderInfo = select('partyId', 'roleTypeId', 'totalGrandAmount', 'totalSubRemainingAmount', 'totalOrders')
            .from('OrderHeaderAndRoleSummary').where(condition).queryFirst()

    // first set the required OUT fields to zero
    result = success()
    result.totalGrandAmount = orderInfo ? orderInfo.totalGrandAmount : BigDecimal.ZERO
    result.totalSubRemainingAmount = orderInfo ? orderInfo.totalSubRemainingAmount : BigDecimal.ZERO
    result.totalOrders = orderInfo ? orderInfo.totalOrders : 0l

    return result
}
