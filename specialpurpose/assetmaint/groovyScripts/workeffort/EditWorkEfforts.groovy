/*
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
*/

maintHistSeqId = context.maintHistSeqId
fixedAssetId = context.fixedAssetId
workEffortId = context.workEffortId

if (!maintHistSeqId) {
    maintHistSeqId = parameters.maintHistSeqId
}
if (!fixedAssetId) {
    fixedAssetId = parameters.fixedAssetId
}
if (!workEffortId) {
    workEffortId = parameters.workEffortId
}

fixedAssetMaint = null
workEffort = null
fixedAsset = null
rootWorkEffortId = null

if (workEffortId) {
    workEffort = from("WorkEffort").where("workEffortId", workEffortId).queryOne()
    if (workEffort) {
        if (!fixedAssetId) {
            fixedAssetId = workEffort.fixedAssetId
        }
        // If this is a child workeffort, locate the "root" workeffort
        parentWorkEffort = from("WorkEffortAssoc").where("workEffortIdTo", workEffortId).queryFirst()
        while (parentWorkEffort) {
            rootWorkEffortId = parentWorkEffort.workEffortIdFrom
            parentWorkEffort = from("WorkEffortAssoc").where("workEffortIdTo", rootWorkEffortId).queryFirst()
        }
    }
}

if (!rootWorkEffortId) {
    rootWorkEffortId = workEffortId
}

if (rootWorkEffortId) {
    fixedAssetMaint = from("FixedAssetMaint").where("scheduleWorkEffortId", rootWorkEffortId).queryFirst()
    if (fixedAssetMaint) {
        maintHistSeqId = fixedAssetMaint.maintHistSeqId
        if (!fixedAssetId) {
            fixedAssetId = fixedAssetMaint.fixedAssetId
        }
    }
}

if (fixedAssetId) {
    fixedAsset = from("FixedAsset").where("fixedAssetId", fixedAssetId).queryOne()
}

context.fixedAssetMaint = fixedAssetMaint
context.workEffort = workEffort
context.fixedAsset = fixedAsset
context.maintHistSeqId = maintHistSeqId
context.fixedAssetId = fixedAssetId
context.workEffortId = workEffortId
