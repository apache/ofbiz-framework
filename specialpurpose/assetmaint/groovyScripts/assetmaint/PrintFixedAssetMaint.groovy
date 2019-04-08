/**
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
**/

facility = fixedAsset.getRelatedOne("LocatedAtFacility", false)
context.locatedAtFacility = facility

fixedAssetIdents = from("FixedAssetIdent").where("fixedAssetId", fixedAssetId).queryList()
fixedAssetIdentValue = ""
if (fixedAssetIdents) {
    fixedAssetIdents.each { ident ->
        fixedAssetIdentValue = fixedAssetIdentValue + " " + ident.idValue
    }
}
context.fixedAssetIdentValue = fixedAssetIdentValue

status = fixedAssetMaint.getRelatedOne("StatusItem", false)
if (status) {
    context.statusItemDesc = status.description
}
//context.put("fixedAssetMaint",fixedAssetMaint)

intervalUom = fixedAssetMaint.getRelatedOne("IntervalUom", false)
if (intervalUom) {
    context.intervalUomDesc = intervalUom.description
}

instanceOfProductId = fixedAsset.instanceOfProductId
productMaintSeqId = fixedAssetMaint.productMaintSeqId
if (productMaintSeqId) {
    productMaint = from("ProductMaint").where("productId", instanceOfProductId, "productMaintSeqId", productMaintSeqId).queryOne()
    context.productMaintName = productMaint.maintName
}

productMaintTypeId = fixedAssetMaint.productMaintTypeId
if (productMaintTypeId) {
    productMaintType = from("ProductMaintType").where("productMaintTypeId", productMaintTypeId).queryOne()
    if (productMaintType) {
        productMaintTypeDesc = productMaintType.description
        context.productMaintTypeDesc = productMaintTypeDesc
    }
}

intervalMeterTypeId = fixedAssetMaint.intervalMeterTypeId
productMeterTypeDesc = ""
if (intervalMeterTypeId) {
    productMeterType = from("ProductMeterType").where("productMeterTypeId", intervalMeterTypeId).queryOne()
    productMeterTypeDesc  = productMeterType.description
}
context.productMeterTypeDesc = productMeterTypeDesc

scheduleWorkEffort = fixedAssetMaint.getRelatedOne("ScheduleWorkEffort", false)
context.scheduleWorkEffort = scheduleWorkEffort
