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
package org.apache.ofbiz.product.product.promo

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceErrorException

Map createProductPromoCond() {
    if (parameters.carrierShipmentMethod) {
        parameters.otherValue = parameters.carrierShipmentMethod
    }
    GenericValue newEntity = makeValue('ProductPromoCond', parameters)
    delegator.setNextSubSeqId(newEntity, 'productPromoCondSeqId', 2, 1)
    newEntity.create()
    return success([productPromoCondSeqId: newEntity.productPromoCondSeqId])
}

Map updateProductPromoCond() {
    Map fieldsToSet = new HashMap(parameters)
    if (parameters.carrierShipmentMethod) {
        fieldsToSet.otherValue = parameters.carrierShipmentMethod
    }
    try {
        update('ProductPromoCond').where(parameters).set(fieldsToSet)
    } catch (ServiceErrorException e) {
        fail(label('ServiceErrorUiLabels', 'ServiceValueNotFound'))
    }
    return success()
}
