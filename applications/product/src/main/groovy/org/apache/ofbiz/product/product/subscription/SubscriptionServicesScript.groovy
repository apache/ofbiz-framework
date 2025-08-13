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
package org.apache.ofbiz.product.product.subscription

import org.apache.ofbiz.entity.GenericValue

/**
 * Create a Subscription
 */
Map createSubscription() {
    GenericValue newEntity = makeValue('Subscription')
    String subscriptionId = parameters.subscriptionId ?: delegator.getNextSeqId('Subscription')
    newEntity.subscriptionId = subscriptionId

    // lookup the product subscription resource (if exists)
    if (parameters.subscriptionResourceId && parameters.productId) {
        GenericValue resource = from('ProductSubscriptionResource')
            .where(subscriptionResourceId: parameters.subscriptionResourceId,
                   productId: parameters.productId )
            .filterByDate()
            .orderBy('-fromDate')
            .queryFirst()
        if (resource) {
            newEntity.setNonPKFields(resource.getAllFields())
        }
    }

    newEntity.setNonPKFields(parameters)
    newEntity.create()

    return success(subscriptionId: subscriptionId)
}

/**
 * Check if a party has a subscription
 */
Map isSubscribed() {
    Map result = success()

    Map serviceContext = [entityName: 'Subscription',
        inputFields: parameters,
        filterByDate: parameters.filterByDate ?: 'Y']
    Map serviceResult = run service: 'performFindList', with: serviceContext

    Boolean found = serviceResult.list
    if (serviceResult.list) {
        result.subscriptionId = serviceResult.list[0].subscriptionId
    }
    result.isSubscribed = found
    return result
}

/**
 * Get Subscription data
 */
Map getSubscription() {
    Map result = success()

    GenericValue subscription = from('Subscription')
        .where(parameters)
        .queryOne()
    result.subscriptionId = parameters.subscriptionId
    if (subscription) {
        result.subscription = subscription
    }
    return result
}

/**
 * Create (when not exist) or update (when exist) a Subscription attribute
 */
Map updateSubscriptionAttribute() {
    GenericValue lookedUpValue = from('SubscriptionAttribute')
        .where(parameters)
        .queryOne()
    if (lookedUpValue) {
        lookedUpValue.setNonPKFields(parameters)
        lookedUpValue.store()
    } else {
        GenericValue newEntity = makeValue('SubscriptionAttribute')
        newEntity.setPKFields(parameters)
        newEntity.setNonPKFields(parameters)
        newEntity.create()
    }
    return success(subscriptionId: parameters.subscriptionId)
}

/**
 * Subscription permission checking logic
 */
Map subscriptionPermissionCheck() {
    parameters.primaryPermission = 'CATALOG'
    Map result = run service: 'genericBasePermissionCheck', with: parameters
    // Backwards compatibility - check for non-existent CATALOG_READ permission
    result.hasPermission = result.hasPermission ||
            (parameters.mainAction == 'VIEW' &&
                    security.hasPermission('CATALOG_READ', userLogin))
    return result
}
