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
package org.apache.ofbiz.product.facility.facility

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil

/**
 * Create or update GeoPoint assigned to facility
 */
Map createUpdateFacilityGeoPoint() {
    if (parameters.geoPointId) {
        Map serviceResult = run service: 'updateGeoPoint', with: parameters
        return serviceResult
    }
    Map serviceResult = run service: 'createGeoPoint', with: parameters
    if (!ServiceUtil.isSuccess(serviceResult)) {
        return serviceResult
    }
    String geoPointId = serviceResult.geoPointId
    GenericValue facility = from('Facility').where(parameters).queryOne()
    facility.geoPointId = geoPointId
    facility.store()
    return success()
}
