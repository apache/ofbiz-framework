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

import org.apache.ofbiz.order.shoppingcart.ShoppingCartEvents
import org.apache.ofbiz.product.config.ProductConfigWorker
import org.apache.ofbiz.product.store.ProductStoreWorker
import org.apache.ofbiz.base.util.*

currencyUomId = ShoppingCartEvents.getCartObject(request).getCurrency()
product = context.product

if (product) {
    configWrapper = ProductConfigWorker.getProductConfigWrapper(product.productId, currencyUomId, request)
    ProductConfigWorker.fillProductConfigWrapper(configWrapper, request)

    if (!configWrapper.isCompleted()) {
        configId = request.getParameter("configId")
        if (configId) {
            configWrapper.loadConfig(delegator, configId)
        } else {
            configWrapper.setDefaultConfig()
        }
    }
    ProductConfigWorker.storeProductConfigWrapper(configWrapper, delegator)
    if (!ProductStoreWorker.isStoreInventoryAvailable(request, configWrapper, 1.0)) {
        context.productNotAvailable = "Y"
    }
    context.configwrapper = configWrapper
    context.configId = configWrapper.getConfigId()
    context.totalPrice = configWrapper.getTotalPrice()
    context.renderSingleChoiceWithRadioButtons = "Y"
    context.showOffsetPrice = "Y"
}
