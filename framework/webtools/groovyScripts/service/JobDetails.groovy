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

import org.apache.ofbiz.entity.Delegator
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.base.util.UtilGenerics
import org.apache.ofbiz.entity.serialize.XmlSerializer

GenericValue job = ((Delegator)delegator).findOne("JobSandbox", [jobId:parameters.jobId], false)
context.job = job
if (job) {
    GenericValue runtimeData = job.getRelatedOne("RuntimeData", false)
    if (runtimeData) {
        runtimeInfoMap = UtilGenerics.checkMap(XmlSerializer.deserialize(runtimeData.getString("runtimeInfo"), delegator), String.class, Object.class)
        runtimeInfoList = []
        runtimeInfoMap.each { key, value ->
            valueMap = [key : key, value : value.toString()]
            runtimeInfoList.add(valueMap)
        }
        context.runtimeInfoList = runtimeInfoList
    }
}
