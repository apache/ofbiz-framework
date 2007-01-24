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
import org.ofbiz.base.util.*;
import org.ofbiz.securityext.login.*;
import org.ofbiz.common.*;
import org.ofbiz.party.contact.*;
import org.ofbiz.webapp.control.*;

String partyId = parameters.get("partyId");
context.put("partyId", partyId);

Map mechMap = new HashMap();
ContactMechWorker.getContactMechAndRelated(request, partyId, mechMap);
context.put("mechMap", mechMap);

String contactMechId = (String) mechMap.get("contactMechId");
context.put("contactMechId", contactMechId);

preContactMechTypeId = parameters.get("preContactMechTypeId");
context.put("preContactMechTypeId", preContactMechTypeId);

paymentMethodId = parameters.get("paymentMethodId");
context.put("paymentMethodId", paymentMethodId);

cmNewPurposeTypeId = parameters.get("contactMechPurposeTypeId");
if (cmNewPurposeTypeId != null) {
    contactMechPurposeType = delegator.findByPrimaryKey("ContactMechPurposeType", UtilMisc.toMap("contactMechPurposeTypeId", cmNewPurposeTypeId));
    if (contactMechPurposeType != null) {
        context.put("contactMechPurposeType", contactMechPurposeType);
    } else {
        cmNewPurposeTypeId = null;
    }
    context.put("cmNewPurposeTypeId", cmNewPurposeTypeId);
}

String donePage = parameters.get("DONE_PAGE");
if (donePage == null || donePage.length() <= 0) donePage = "viewprofile?party_id=" + partyId + "&partyId=" + partyId;
context.put("donePage", donePage);
