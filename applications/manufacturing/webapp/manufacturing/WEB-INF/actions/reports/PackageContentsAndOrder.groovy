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

import java.util.*;
import org.ofbiz.entity.*;
import org.ofbiz.base.util.*;
import org.ofbiz.entity.util.*;
import org.ofbiz.order.order.OrderReadHelper;
import org.ofbiz.order.order.OrderContentWrapper;
import org.ofbiz.product.category.CategoryWorker;

if (!UtilValidate.isEmpty(productCategoryIdPar)) {
    category = delegator.findByPrimaryKey("ProductCategory", UtilMisc.toMap("productCategoryId", productCategoryIdPar));
    context.put("category", category);
}
if (!UtilValidate.isEmpty(productFeatureTypeIdPar)) {
    featureType = delegator.findByPrimaryKey("ProductFeatureType", UtilMisc.toMap("productFeatureTypeId", productFeatureTypeIdPar));
    context.put("featureType", featureType);
}
packageContents = delegator.findByAnd("ShipmentPackageContent", UtilMisc.toMap("shipmentId", shipmentId));

Map packagesMap = new HashMap();
if (packageContents != null) {
    packageContentsIt = packageContents.iterator();
    while (packageContentsIt.hasNext()) {
        packageContent = packageContentsIt.next();

        orderShipments = delegator.findByAnd("OrderShipment", UtilMisc.toMap("shipmentId", shipmentId, "shipmentItemSeqId", packageContent.getString("shipmentItemSeqId")));
        orderShipment = EntityUtil.getFirst(orderShipments);
        orderItem = delegator.findByPrimaryKey("OrderItem", UtilMisc.toMap("orderId", orderShipment.getString("orderId"), "orderItemSeqId", orderShipment.getString("orderItemSeqId")));
        product = orderItem.getRelatedOne("Product");
        // verify if the product is a member of the given category (based on the report's parameter)
        if (!UtilValidate.isEmpty(productCategoryIdPar)) {
            if (!isProductInCategory(delegator, product.getString("productId"), productCategoryIdPar)) {
                // the production run's product is not a member of the given category, skip it
                continue;
            }
        }

        if (!packagesMap.containsKey(packageContent.getString("shipmentPackageSeqId"))) {
            OrderReadHelper orh = new OrderReadHelper(delegator, orderItem.getString("orderId"));
            packagesMap.put(packageContent.getString("shipmentPackageSeqId"),
                            UtilMisc.toMap("packageId", packageContent.getString("shipmentPackageSeqId"),
                                           "party", orh.getPlacingParty(),
                                           "address", orh.getShippingAddress(),
                                           "orderHeader", orh.getOrderHeader(),
                                           "orderShipment", orderShipment,
                                           "components", new ArrayList()));
        }
        OrderContentWrapper orderContentWrapper = OrderContentWrapper.makeOrderContentWrapper(orderItem, request);
        String imageUrl = orderContentWrapper.get("IMAGE_URL");
        packageMap = (Map)packagesMap.get(packageContent.getString("shipmentPackageSeqId"));
        components = (List)packageMap.get("components");
        components.add(UtilMisc.toMap("product", product, "orderItem", orderItem, "imageUrl", imageUrl));
    }
}
context.put("packages", packagesMap.values());
