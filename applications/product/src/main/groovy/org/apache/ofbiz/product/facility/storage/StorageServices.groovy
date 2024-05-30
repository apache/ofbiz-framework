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
package org.apache.ofbiz.product.facility.storage

import org.apache.ofbiz.entity.GenericValue

/**
 * Create a Facility Location
 */
Map createFacilityLocation() {
    GenericValue newEntity = makeValue('FacilityLocation', parameters)

    String locationSeqId = "${parameters.areaId ?: ''}${parameters.aisleId ?: ''}${parameters.sectionId ?: ''}" +
    "${parameters.levelId ?: ''}${parameters.positionId ?: ''}"

    if (locationSeqId) {
        int i = 1
        String nextLocationSeqId = locationSeqId
        while (from('FacilityLocation')
                .where([locationSeqId: nextLocationSeqId,
                        facilityId: parameters.facilityId])
                .queryOne()) {
            nextLocationSeqId = "${locationSeqId}_${i++}"
        }
        locationSeqId = nextLocationSeqId
    } else {
        locationSeqId = delegator.getNextSeqId('FacilityLocation')
    }

    newEntity.locationSeqId = locationSeqId
    newEntity.create()
    return success(locationSeqId: newEntity.locationSeqId)
}
