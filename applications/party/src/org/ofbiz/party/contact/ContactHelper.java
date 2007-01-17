/*******************************************************************************
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
 *******************************************************************************/

package org.ofbiz.party.contact;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;

/**
 * Accessors for Contact Mechanisms
 */
public class ContactHelper {
    
    public static final String module = ContactHelper.class.getName();
    
    public static Collection getContactMech(GenericValue party, boolean includeOld) {
        return getContactMech(party, null, null, includeOld);
    }

    public static Collection getContactMechByType(GenericValue party, String contactMechTypeId, boolean includeOld) {
        return getContactMech(party, null, contactMechTypeId, includeOld);
    }

    public static Collection getContactMechByPurpose(GenericValue party, String contactMechPurposeTypeId, boolean includeOld) {
        return getContactMech(party, contactMechPurposeTypeId, null, includeOld);
    }

    public static Collection getContactMech(GenericValue party, String contactMechPurposeTypeId, String contactMechTypeId, boolean includeOld) {
        if (party == null) return null;
        try {
            List partyContactMechList;

            if (contactMechPurposeTypeId == null) {
                partyContactMechList = party.getRelated("PartyContactMech");
            } else {
                List list;

                list = party.getRelatedByAnd("PartyContactMechPurpose", UtilMisc.toMap("contactMechPurposeTypeId", contactMechPurposeTypeId));
                if (!includeOld) {
                    list = EntityUtil.filterByDate(list, true);
                }
                partyContactMechList = EntityUtil.getRelated("PartyContactMech", list);
            }
            if (!includeOld) {
                partyContactMechList = EntityUtil.filterByDate(partyContactMechList, true);
            }
            partyContactMechList = EntityUtil.orderBy(partyContactMechList, UtilMisc.toList("fromDate DESC"));
            if (contactMechTypeId == null) {
                return EntityUtil.getRelated("ContactMech", partyContactMechList);
            } else {
                return EntityUtil.getRelatedByAnd("ContactMech", UtilMisc.toMap("contactMechTypeId", contactMechTypeId), partyContactMechList);
            }
        } catch (GenericEntityException gee) {
            Debug.logWarning(gee, module);
            return Collections.EMPTY_LIST;
        }
    }

    public static String formatCreditCard(GenericValue creditCardInfo) {
        StringBuffer result = new StringBuffer(16);

        result.append(creditCardInfo.getString("cardType"));
        String cardNumber = creditCardInfo.getString("cardNumber");

        if (cardNumber != null && cardNumber.length() > 4) {
            result.append(' ').append(cardNumber.substring(cardNumber.length() - 4));
        }
        result.append(' ').append(creditCardInfo.getString("expireDate"));
        return result.toString();
    }

}
