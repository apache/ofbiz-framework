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


import org.apache.ofbiz.base.util.UtilDateTime
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
    } else if (partyAcctgPreference.oldOrderSequenceEnumId == 'ODRSQ_ENF_SEQ') {
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
    if (productStore) orderId += productStore.orderNumberPrefix
    if (partyAcctgPreference) orderId += partyAcctgPreference.orderIdPrefix
    orderId += orderIdTemp.toString()

    Map result = success()
    result.orderId = orderId

    return result
}

}
