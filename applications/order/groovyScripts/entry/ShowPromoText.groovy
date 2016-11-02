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

import org.apache.ofbiz.base.util.UtilMisc
import org.apache.ofbiz.order.shoppingcart.product.ProductPromoWorker
import org.apache.ofbiz.order.shoppingcart.*

shoppingCart = ShoppingCartEvents.getCartObject(request)
mode = shoppingCart.getOrderType()

promoShowLimit = 3

if ("SALES_ORDER".equals(mode)) {
    //Get Promo Text Data
    productPromosAll = ProductPromoWorker.getStoreProductPromos(delegator, dispatcher, request)
    //Make sure that at least one promo has non-empty promoText
    showPromoText = false
    promoToShow = 0
    productPromosAllShowable = new ArrayList(productPromosAll.size())
    productPromosAll.each { productPromo ->
        promoText = productPromo.promoText
        if (promoText  && !"N".equals(productPromo.showToCustomer)) {
            showPromoText = true
            promoToShow++
            productPromosAllShowable.add(productPromo)
        }
    }

    // now slim it down to promoShowLimit
    productPromosRandomTemp = new ArrayList(productPromosAllShowable)
    productPromos = null
    if (productPromosRandomTemp.size() > promoShowLimit) {
        productPromos = new ArrayList(promoShowLimit)
        for (i = 0; i < promoShowLimit; i++) {
            randomIndex = Math.round(java.lang.Math.random() * (productPromosRandomTemp.size() - 1)) as int
            productPromos.add(productPromosRandomTemp.remove(randomIndex))
        }
    } else {
        productPromos = productPromosRandomTemp
    }

    context.promoShowLimit = promoShowLimit
    context.productPromosAllShowable = productPromosAllShowable
    context.productPromos = productPromos
    context.showPromoText = showPromoText
    context.promoToShow = promoToShow
} else {
    context.showPromoText = false
}
