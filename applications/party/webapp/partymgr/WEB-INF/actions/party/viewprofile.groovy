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

import org.ofbiz.base.util.*;

partyId = parameters.get("partyId");
if (partyId == null)
    partyId = parameters.get("party_id");

userLoginId = parameters.get("userlogin_id");
if (UtilValidate.isEmpty(userLoginId)) {
    userLoginId = parameters.get("userLoginId");
}
if (UtilValidate.isEmpty(partyId) && UtilValidate.isNotEmpty(userLoginId)) {
    thisUserLogin = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", userLoginId));
    if (thisUserLogin != null) {
        partyId = thisUserLogin.getString("partyId");
        parameters.put("partyId", partyId);
    }
}

boolean showOld = "true".equals(parameters.get("SHOW_OLD"));
context.put("showOld", new Boolean(showOld));

context.put("partyId", partyId); 
context.put("party", delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", partyId)));
context.put("nowStr", UtilDateTime.nowTimestamp().toString());


