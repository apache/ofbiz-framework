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

import org.ofbiz.entity.condition.*
import org.ofbiz.base.util.*

facilityId = parameters.facilityId;
context.facilityId = facilityId;

lookup = request.getParameter("look_up");
itemId = request.getParameter("inventoryItemId");
if (itemId) {
    session.setAttribute("inventoryItemId", itemId);
}

itemId = session.getAttribute("inventoryItemId");
context.itemId = itemId;

facility = from("Facility").where("facilityId", facilityId).queryOne();
context.facility = facility;

UtilHttp.parametersToAttributes(request);
if (lookup) {
    reqParamMap = UtilHttp.getParameterMap(request);
    paramMap = new HashMap(reqParamMap);
    paramMap.remove("look_up");
    reqParamMap.keySet().each { key ->
        value = paramMap.get(key);
        if (!value || value.length() == 0) {
            paramMap.remove(key);
        }
    }
    foundLocations = from("FacilityLocation").where(paramMap).queryList();
    if (foundLocations) {
        context.foundLocations = foundLocations;
    }
}
