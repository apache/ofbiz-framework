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

// PackageContentsAndOrder
// ReportB

import org.apache.ofbiz.order.order.OrderReadHelper
import org.apache.ofbiz.order.order.OrderContentWrapper

if (productCategoryIdPar) {
    category = from("ProductCategory").where("productCategoryId", productCategoryIdPar).queryOne()
    context.category = category
}
if (productFeatureTypeIdPar) {
    featureType = from("ProductFeatureType").where("productFeatureTypeId", productFeatureTypeIdPar).queryOne()
    context.featureType = featureType
}
packageContents = from("ShipmentPackageContent").where("shipmentId", shipmentId).queryList()

packagesMap = [:]
if (packageContents) {
    packageContents.each { packageContent ->
        orderShipment = from("OrderShipment").where("shipmentId", shipmentId, "shipmentItemSeqId", packageContent.shipmentItemSeqId).queryFirst()
        orderItem = from("OrderItem").where("orderId", orderShipment.orderId, "orderItemSeqId", orderShipment.orderItemSeqId).queryOne()
        product = orderItem.getRelatedOne("Product", false)
        // verify if the product is a member of the given category (based on the report's parameter)
        if (productCategoryIdPar) {
            if (!isProductInCategory(delegator, product.productId, productCategoryIdPar)) {
                // the production run's product is not a member of the given category, skip it
                return
            }
        }

        if (!packagesMap.containsKey(packageContent.shipmentPackageSeqId)) {
            OrderReadHelper orh = new OrderReadHelper(delegator, orderItem.orderId)
            packagesMap.put(packageContent.shipmentPackageSeqId,
                            [packageId : packageContent.shipmentPackageSeqId,
                             party : orh.getPlacingParty(),
                             address : orh.getShippingAddress(),
                             orderHeader : orh.getOrderHeader(),
                             orderShipment : orderShipment,
                             components : []])
        }
        OrderContentWrapper orderContentWrapper = OrderContentWrapper.makeOrderContentWrapper(orderItem, request)
        String imageUrl = orderContentWrapper.get("IMAGE_URL", "url")
        packageMap = (Map)packagesMap.packageContent.shipmentPackageSeqId
        components = (List)packageMap.components
        components.add([product : product, orderItem : orderItem, imageUrl : imageUrl])
    }
}
context.packages = packagesMap.values()
