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
import java.lang.*;
import org.ofbiz.entity.*;
import org.ofbiz.entity.condition.*;
import org.ofbiz.base.util.*;
import org.ofbiz.base.util.UtilMisc;

// Optional prefix parameter to filter InvoiceItemTypes by (i.e. "INV" or "PINV") defaults to INV
invItemTypePrefix = context.invItemTypePrefix ?: "INV";
invItemTypePrefix += "_%";

organizationPartyId = parameters.organizationPartyId;

invoiceItemTypes = delegator.findList("InvoiceItemType", EntityCondition.makeCondition("invoiceItemTypeId", EntityOperator.LIKE, invItemTypePrefix), null, null, null, false);
allTypes = [];
invoiceItemTypes.each { invoiceItemType ->
    activeGlDescription = "";
    remove = " ";
    glAccounts = null;
    glAccount = null;
    invoiceItemTypeOrgs = invoiceItemType.getRelatedByAnd("InvoiceItemTypeGlAccount", [organizationPartyId : organizationPartyId]);
    overrideGlAccountId = " ";
    if (invoiceItemTypeOrgs) {
        invoiceItemTypeOrg = invoiceItemTypeOrgs[0];
        overrideGlAccountId = invoiceItemTypeOrg.glAccountId;

        glAccounts = invoiceItemTypeOrg.getRelated("GlAccount");
        if (glAccounts) {
            glAccount = glAccounts[0];
        }
    } else {
        glAccount = invoiceItemType.getRelatedOne("DefaultGlAccount");
    }

    if (glAccount) {
        activeGlDescription = glAccount.accountName;
        remove = "Remove";
    }

    allTypes.add([invoiceItemTypeId : invoiceItemType.invoiceItemTypeId,
                  description : invoiceItemType.description,
                  defaultGlAccountId : invoiceItemType.defaultGlAccountId,
                  overrideGlAccountId : overrideGlAccountId,
                  remove : remove,
                  activeGlDescription : activeGlDescription]);
}
context.invoiceItemTypes = allTypes;
