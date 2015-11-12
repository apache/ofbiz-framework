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
import org.ofbiz.entity.util.EntityUtilProperties

configList = []
str = EntityUtilProperties.getPropertyValue("autoUpdateToGoogleBase", "autoUpdateGoogleBase.productStoreId", delegator)
productStoreIds = str.split(",")
str = EntityUtilProperties.getPropertyValue("autoUpdateToGoogleBase", "autoUpdateGoogleBase.productStore.option.outOfStock", delegator)
outOfStock = str.split(",")
str = EntityUtilProperties.getPropertyValue("autoUpdateToGoogleBase", "autoUpdateGoogleBase.productStore.option.backInStock", delegator)
backInStock = str.split(",")
str = EntityUtilProperties.getPropertyValue("autoUpdateToGoogleBase", "autoUpdateGoogleBase.productStore.webSiteUrl", delegator)
webSiteUrl = str.split(",")
str = EntityUtilProperties.getPropertyValue("autoUpdateToGoogleBase", "autoUpdateGoogleBase.productStore.actionType", delegator)
actionType = str.split(",")
str = EntityUtilProperties.getPropertyValue("autoUpdateToGoogleBase", "autoUpdateGoogleBase.productStore.statusId", delegator)
statusId = str.split(",")
str = EntityUtilProperties.getPropertyValue("autoUpdateToGoogleBase", "autoUpdateGoogleBase.productStore.testMode", delegator)
testMode = str.split(",")
str = EntityUtilProperties.getPropertyValue("autoUpdateToGoogleBase", "autoUpdateGoogleBase.productStore.webSiteMountPoint", delegator)
webSiteMountPoint = str.split(",")
str = EntityUtilProperties.getPropertyValue("autoUpdateToGoogleBase", "autoUpdateGoogleBase.productStore.countryCode", delegator)
countryCode = str.split(",")
str = EntityUtilProperties.getPropertyValue("autoUpdateToGoogleBase", "autoUpdateGoogleBase.productStore.trackingCodeId", delegator)
trackingCodeId = str.split(",")
str = EntityUtilProperties.getPropertyValue("autoUpdateToGoogleBase", "autoUpdateGoogleBase.productStore.allowRecommended", delegator)
allowRecommended = str.split(",")

productStoreIds.eachWithIndex{ productStoreId, i ->
    configMap = [:]
    configMap.productStoreId = productStoreId
    configMap.outOfStock = outOfStock[i]
    configMap.backInStock = backInStock[i]
    configMap.webSiteUrl = webSiteUrl[i]
    configMap.actionType = actionType[i]
    configMap.statusId = statusId[i]
    configMap.testMode = testMode[i]
    configMap.webSiteMountPoint = webSiteMountPoint[i]
    configMap.countryCode = countryCode[i]
    configMap.trackingCodeId = trackingCodeId[i]
    configMap.allowRecommended = allowRecommended[i]
    configList.add(configMap)
}
context.configList = configList
