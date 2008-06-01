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

import java.util.*;
import org.ofbiz.entity.*;
import org.ofbiz.entity.condition.*;
import org.ofbiz.base.util.*;
import org.ofbiz.party.party.*;

match = request.getParameter("match");
if (match != null) {
    context.put("match", match);
    
    lastName = request.getParameter("lastName");
    if (UtilValidate.isEmpty(lastName)) lastName = null;

    firstName = request.getParameter("firstName");
    if (UtilValidate.isEmpty(firstName)) firstName = null;

    address1 = request.getParameter("address1");
    if (UtilValidate.isEmpty(address1)) address1 = null;

    address2 = request.getParameter("address2");
    if (UtilValidate.isEmpty(address2)) address2 = null;

    city = request.getParameter("city");
    if (UtilValidate.isEmpty(city)) city = null;

    state = request.getParameter("stateProvinceGeoId");
    if (UtilValidate.isEmpty(state)) state = null;
    if ("ANY".equals(state)) state = null;

    postalCode = request.getParameter("postalCode");
    if (UtilValidate.isEmpty(postalCode)) postalCode = null;

    if (state != null) {
        context.put("currentStateGeo", delegator.findByPrimaryKey("Geo", UtilMisc.toMap("geoId", state)));
    }

    if (firstName == null || lastName == null || address1 == null || city == null || postalCode == null) {
        request.setAttribute("_ERROR_MESSAGE_", "Required fields not set!");
        return;
    }
    
    context.put("matches", PartyWorker.findMatchingPartyAndPostalAddress(delegator, address1, address2, city,
            state, postalCode, null, null, firstName, null, lastName));

    context.put("addressString", PartyWorker.makeMatchingString(delegator, address1));
    context.put("lastName", lastName);
    context.put("firstName", firstName);
}
