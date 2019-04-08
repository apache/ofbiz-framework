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
 import org.apache.ofbiz.entity.util.EntityUtil
 
 productStoreId = null
 
productStore = EntityUtil.getFirst(delegator.findByAnd("ProductStore", [payToPartyId: partyId], null, false))
if(productStore){
    productStoreId = productStore.productStoreId
}
context.productStoreId = productStoreId
context.productStore = productStore

if("website".equals(tabButtonItemTop)){
    if(productStoreId != null){
        webSite = EntityUtil.getFirst(delegator.findByAnd("WebSite", [productStoreId: productStoreId], null, false))
        context.showScreen = "origin"
    }else{
        request.setAttribute("_ERROR_MESSAGE_", "Product Store not set!")
        context.showScreen = "message"
        return
    }
    context.webSite = webSite
}
