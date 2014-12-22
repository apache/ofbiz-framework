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

import org.ofbiz.party.party.PartyWorker;

match = parameters.match;
if (match) {
    context.match = match;

    lastName = parameters.lastName ?: null;
    firstName = parameters.firstName ?: null;
    address1 = parameters.address1 ?: null;
    address2 = parameters.address2 ?: null;
    city = parameters.city ?: null;
    state = parameters.stateProvinceGeoId ?: null;
    if ("ANY".equals(state)) state = null;
    postalCode = parameters.postalCode ?: null;

    if (state) {
        context.currentStateGeo = from("Geo").where("geoId", state).queryOne();
    }

    if (!firstName || !lastName || !address1 || !city || !postalCode) {
        request.setAttribute("_ERROR_MESSAGE_", "Required fields not set!");
        return;
    }

    context.matches = PartyWorker.findMatchingPersonPostalAddresses(delegator, address1, address2, city,
            state, postalCode, null, null, firstName, null, lastName);

    context.addressString = PartyWorker.makeMatchingString(delegator, address1);
    context.lastName = lastName;
    context.firstName = firstName;
}
