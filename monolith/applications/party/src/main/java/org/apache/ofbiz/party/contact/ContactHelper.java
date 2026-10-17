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

package org.apache.ofbiz.party.contact;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityUtil;

/**
 * Accessors for Contact Mechanisms
 */
public final class ContactHelper {

    private static final String MODULE = ContactHelper.class.getName();

    private ContactHelper() { }

    public static Collection<GenericValue> getContactMech(GenericValue party, boolean includeOld) {
        return getContactMech(party, null, null, includeOld);
    }

    public static Collection<GenericValue> getContactMechByType(GenericValue party, String contactMechTypeId, boolean includeOld) {
        return getContactMech(party, null, contactMechTypeId, includeOld);
    }

    public static Collection<GenericValue> getContactMechByPurpose(GenericValue party, String contactMechPurposeTypeId, boolean includeOld) {
        return getContactMech(party, contactMechPurposeTypeId, null, includeOld);
    }

    public static Collection<GenericValue> getContactMech(GenericValue party, String contactMechPurposeTypeId, String
            contactMechTypeId, boolean includeOld) {
        if (party == null) {
            return null;
        }
        try {
            List<GenericValue> partyContactMechList;

            if (contactMechPurposeTypeId == null) {
                partyContactMechList = party.getRelated("PartyContactMech", null, null, false);
            } else {
                List<GenericValue> list;

                list = party.getRelated("PartyContactMechPurpose", UtilMisc.toMap("contactMechPurposeTypeId", contactMechPurposeTypeId), null, false);
                if (!includeOld) {
                    list = EntityUtil.filterByDate(list, true);
                }
                partyContactMechList = EntityUtil.getRelated("PartyContactMech", null, list, false);
            }
            if (!includeOld) {
                partyContactMechList = EntityUtil.filterByDate(partyContactMechList, true);
            }
            partyContactMechList = EntityUtil.orderBy(partyContactMechList, UtilMisc.toList("fromDate DESC"));
            if (contactMechTypeId == null) {
                return EntityUtil.getRelated("ContactMech", null, partyContactMechList, false);
            }
            return EntityUtil.getRelated("ContactMech", UtilMisc.toMap("contactMechTypeId", contactMechTypeId), partyContactMechList, false);
        } catch (GenericEntityException gee) {
            Debug.logWarning(gee, MODULE);
            return Collections.emptyList();
        }
    }

    public static String formatCreditCard(GenericValue creditCardInfo) {
        StringBuilder result = new StringBuilder(16);

        result.append(creditCardInfo.getString("cardType"));
        String cardNumber = creditCardInfo.getString("cardNumber");

        if (cardNumber != null && cardNumber.length() > 4) {
            result.append(' ').append(cardNumber.substring(cardNumber.length() - 4));
        }
        result.append(' ').append(creditCardInfo.getString("expireDate"));
        return result.toString();
    }

}
