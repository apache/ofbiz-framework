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

/*
 * This script is also referenced by the ecommerce's screens and
 * should not contain order component's specific code.
 */

import org.ofbiz.base.util.*;
import org.ofbiz.entity.*;
import org.ofbiz.entity.util.*;
import org.ofbiz.service.*;
import org.ofbiz.product.product.ProductContentWrapper;
import org.ofbiz.product.config.ProductConfigWorker;
import org.ofbiz.product.catalog.*;
import org.ofbiz.product.store.*;
import org.ofbiz.order.shoppingcart.*;


//either optProduct, optProductId or productId must be specified
product = request.getAttribute("optProduct");
optProductId = request.getAttribute("optProductId");
productId = product?.productId ?: optProductId ?: request.getAttribute("productId");

webSiteId = CatalogWorker.getWebSiteId(request);
catalogId = CatalogWorker.getCurrentCatalogId(request);
cart = ShoppingCartEvents.getCartObject(request);
productStore = null;
productStoreId = null;
facilityId = null;
if (cart.isSalesOrder()) {
    productStore = ProductStoreWorker.getProductStore(request);
    productStoreId = productStore.productStoreId;
    context.productStoreId = productStoreId;
    facilityId = productStore.inventoryFacilityId;
}
autoUserLogin = session.getAttribute("autoUserLogin");
userLogin = session.getAttribute("userLogin");

context.remove("daysToShip");
context.remove("averageRating");
context.remove("numRatings");
context.remove("totalPrice");

// get the product entity
if (!product && productId) {
    product = delegator.findByPrimaryKeyCache("Product", [productId : productId]);
}
if (product) {
    //if order is purchase then don't calculate available inventory for product.
    if (cart.isSalesOrder()) {
        resultOutput = dispatcher.runSync("getInventoryAvailableByFacility", [productId : product.productId, facilityId : facilityId, useCache : true]);
        totalAvailableToPromise = resultOutput.availableToPromiseTotal;
        if (totalAvailableToPromise && totalAvailableToPromise.doubleValue() > 0) {
            productFacility = delegator.findByPrimaryKeyCache("ProductFacility", [productId : product.productId, facilityId : facilityId]);
            if (productFacility?.daysToShip != null) {
                context.daysToShip = productFacility.daysToShip;
            }
        }
    } else {
       supplierProducts = delegator.findByAndCache("SupplierProduct", [productId : product.productId], ["-availableFromDate"]);
       supplierProduct = EntityUtil.getFirst(supplierProducts);
       if (supplierProduct?.standardLeadTimeDays != null) {
           standardLeadTimeDays = supplierProduct.standardLeadTimeDays;
           daysToShip = standardLeadTimeDays + 1;
           context.daysToShip = daysToShip;
       }
    }
    // make the productContentWrapper
    productContentWrapper = new ProductContentWrapper(product, request);
    context.productContentWrapper = productContentWrapper;
}

categoryId = null;
reviews = null;
if (product) {
    categoryId = parameters.category_id ?: request.getAttribute("productCategoryId");

    // get the product price
    if (cart.isSalesOrder()) {
        // sales order: run the "calculateProductPrice" service
        priceContext = [product : product, currencyUomId : cart.getCurrency(),
                autoUserLogin : autoUserLogin, userLogin : userLogin];
        priceContext.webSiteId = webSiteId;
        priceContext.prodCatalogId = catalogId;
        priceContext.productStoreId = productStoreId;
        priceContext.agreementId = cart.getAgreementId();
        priceContext.partyId = cart.getPartyId();  // IMPORTANT: otherwise it'll be calculating prices using the logged in user which could be a CSR instead of the customer
        priceContext.checkIncludeVat = "Y";
        priceMap = dispatcher.runSync("calculateProductPrice", priceContext);

        context.price = priceMap;
    } else {
        // purchase order: run the "calculatePurchasePrice" service
        priceContext = [product : product, currencyUomId : cart.getCurrency(),
                partyId : cart.getPartyId(), userLogin : userLogin];
        priceMap = dispatcher.runSync("calculatePurchasePrice", priceContext);

        context.price = priceMap;
    }

    // get aggregated product totalPrice
    if ("AGGREGATED".equals(product.productTypeId)) {
        configWrapper = ProductConfigWorker.getProductConfigWrapper(productId, cart.getCurrency(), request);
        if (configWrapper) {
            configWrapper.setDefaultConfig();
            context.totalPrice = configWrapper.getTotalPrice();
        }
    }

    // get the product review(s)
    reviews = product.getRelatedCache("ProductReview", null, ["-postedDateTime"]);
}

// get the average rating
if (reviews) {
    totalProductRating = 0;
    numRatings = 0;
    reviews.each { productReview ->
        productRating = productReview.productRating;
        if (productRating) {
            totalProductRating += productRating;
            numRatings++;
        }
    }
    if (numRatings) {
        context.averageRating = totalProductRating/numRatings;
        context.numRatings = numRatings;
    }
}

// an example of getting features of a certain type to show
sizeProductFeatureAndAppls = delegator.findByAnd("ProductFeatureAndAppl", [productId : productId, productFeatureTypeId : "SIZE"], ["sequenceNum", "defaultSequenceNum"]);

context.product = product;
context.categoryId = categoryId;
context.productReviews = reviews;
context.sizeProductFeatureAndAppls = sizeProductFeatureAndAppls;
