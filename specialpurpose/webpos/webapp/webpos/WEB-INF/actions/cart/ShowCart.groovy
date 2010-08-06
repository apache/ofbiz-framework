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

import org.ofbiz.webpos.WebPosEvents;
import org.ofbiz.webpos.session.WebPosSession;

webPosSession = WebPosEvents.getWebPosSession(request, null);
if (webPosSession) {
    shoppingCart = webPosSession.getCart();
} else {
    shoppingCart = null;
}

// Get the Cart and Prepare Size
if (shoppingCart) {
    context.shoppingCartSize = shoppingCart.size();
} else {
    context.shoppingCartSize = 0;
}
context.shoppingCart = shoppingCart;

//check if a parameter is passed
if (request.getAttribute("add_product_id") != "") {
    add_product_id = request.getParameter("add_product_id");
    product = delegator.findOne("Product", [productId : add_product_id], true);
    context.product = product;
}
