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

import org.apache.ofbiz.product.store.ProductStoreWorker
import org.apache.ofbiz.webapp.control.LoginWorker

context.autoUserLogin = session.getAttribute("autoUserLogin")
context.autoLogoutUrl = LoginWorker.makeLoginUrl(request, "autoLogout")

previousParams = session.getAttribute("_PREVIOUS_PARAMS_")
if (previousParams) {
    previousParams = UtilHttp.stripNamedParamsFromQueryString(previousParams, ['USERNAME', 'PASSWORD'])
    previousParams = "?" + previousParams
} else {
    previousParams = ""
}
context.previousParams = previousParams

productStoreId = ProductStoreWorker.getProductStoreId(request)
productStore = ProductStoreWorker.getProductStore(productStoreId, delegator)

if (productStore) {
    facilityId = productStore.getString("inventoryFacilityId")

    if (facilityId) {
        context.posTerminals = from("PosTerminal").where("facilityId", facilityId).orderBy("posTerminalId").queryList()
    } else {
        context.posTerminals = from("PosTerminal").orderBy("posTerminalId").queryList()
    }
}
