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

import org.ofbiz.entity.*;
import org.ofbiz.entity.condition.*;

facility = fixedAsset.getRelatedOne("LocatedAtFacility");
context.locatedAtFacility = facility;

fixedAssetIdents = delegator.findList("FixedAssetIdent", EntityCondition.makeCondition([fixedAssetId : fixedAssetId]), null, null, null, false);
fixedAssetIdentValue = "";
if (fixedAssetIdents) {
    fixedAssetIdents.each { ident ->
        fixedAssetIdentValue = fixedAssetIdentValue + " " + ident.idValue;
    }
}
context.fixedAssetIdentValue = fixedAssetIdentValue;

status = fixedAssetMaint.getRelatedOne("StatusItem");
if (status) {
    context.statusItemDesc = status.description;
}
//context.put("fixedAssetMaint",fixedAssetMaint);

intervalUom = fixedAssetMaint.getRelatedOne("IntervalUom");
if (intervalUom) {
    context.intervalUomDesc = intervalUom.description;
}

instanceOfProductId = fixedAsset.instanceOfProductId;
productMaintSeqId = fixedAssetMaint.productMaintSeqId;
if (productMaintSeqId) {
    productMaint = delegator.findOne("ProductMaint", [productId : instanceOfProductId, productMaintSeqId : productMaintSeqId], false);
    context.productMaintName = productMaint.maintName;
}

productMaintTypeId = fixedAssetMaint.productMaintTypeId;
if (productMaintTypeId) {
    productMaintType = delegator.findOne("ProductMaintType", [productMaintTypeId : productMaintTypeId], false);
    if (productMaintType) {
        productMaintTypeDesc = productMaintType.description;
        context.productMaintTypeDesc = productMaintTypeDesc;
    }
}

intervalMeterTypeId = fixedAssetMaint.intervalMeterTypeId;
productMeterTypeDesc = "";
if (intervalMeterTypeId) {
    productMeterType = delegator.findOne("ProductMeterType", [productMeterTypeId : intervalMeterTypeId], false);
    productMeterTypeDesc  = productMeterType.description;
}
context.productMeterTypeDesc = productMeterTypeDesc;

scheduleWorkEffort = fixedAssetMaint.getRelatedOne("ScheduleWorkEffort");
context.scheduleWorkEffort = scheduleWorkEffort;