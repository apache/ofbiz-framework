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

import java.util.StringTokenizer
import org.apache.ofbiz.base.util.UtilValidate

String carrierShipmentString = request.getParameter("carrierShipmentString")
if (UtilValidate.isNotEmpty(carrierShipmentString)) {
    StringTokenizer st = new StringTokenizer(carrierShipmentString, "|")
    if (st.countTokens() != 3) {
        return "error"
    }
    request.setAttribute("addCarrierShipMeth", "Y")
    request.setAttribute("partyId", st.nextToken())
    request.setAttribute("roleTypeId", st.nextToken())
    request.setAttribute("shipmentMethodTypeId", st.nextToken())
    return "success"
} else {
    return "error"
}
