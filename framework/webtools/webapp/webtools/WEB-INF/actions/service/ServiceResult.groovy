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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.sql.Timestamp;

import org.ofbiz.entity.GenericEntity;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.Debug;
import org.ofbiz.webapp.event.CoreEvents;

if (session.getAttribute("_RUN_SYNC_RESULT_")) {
    serviceResultList = [];
    serviceResult = session.getAttribute("_RUN_SYNC_RESULT_");

    if (parameters.servicePath) {
        servicePath = parameters.servicePath;
        newServiceResult = CoreEvents.getObjectFromServicePath(servicePath, serviceResult);
        if (newServiceResult && newServiceResult instanceof Map) {
            serviceResult = newServiceResult;
        }
        context.servicePath = servicePath;
    }

    serviceResult.each { key, value ->
        valueMap = [key : key, value : value.toString()];
        if (value instanceof Map || value instanceof Collection) {
            valueMap.hasChild = "Y";
        } else {
            valueMap.hasChild = "N";
        }
        serviceResultList.add(valueMap);
    }

    context.serviceResultList = serviceResultList;
}


