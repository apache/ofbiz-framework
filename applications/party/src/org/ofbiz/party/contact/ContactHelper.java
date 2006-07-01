/*
 * $Id: ContactHelper.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2001, 2002 The Open For Business Project - www.ofbiz.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
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
 * 
 * @author     <a href="mailto:epabst@bigfoot.com">Eric Pabst</a>
 * @version    $Rev$
 * @since      2.0
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
