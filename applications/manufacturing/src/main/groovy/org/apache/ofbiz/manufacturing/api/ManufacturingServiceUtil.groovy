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
package org.apache.ofbiz.manufacturing.api

import org.apache.ofbiz.entity.GenericValue

final class ManufacturingServiceUtil {

    private ManufacturingServiceUtil() { }

    static String displayProductName(GenericValue product) {
        return product?.productName ?: product?.internalName ?: product?.productId
    }

    static String displayFixedAssetName(GenericValue fixedAsset) {
        return fixedAsset?.fixedAssetName ?: fixedAsset?.fixedAssetId
    }

    static String displayFacilityName(GenericValue facility) {
        return facility?.facilityName ?: facility?.facilityId
    }

    static String displayRoleTypeDescription(GenericValue roleType) {
        return roleType?.description ?: roleType?.roleTypeId
    }

    static String displayStatusDescription(GenericValue status) {
        return status?.description ?: status?.statusId
    }

    static String displayUomDescription(GenericValue uom) {
        return uom?.description ?: uom?.uomId
    }

}
