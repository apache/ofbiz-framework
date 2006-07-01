/*
 * $Id: PartyHelper.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 *  Copyright (c) 2001, 2002 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.ofbiz.party.party;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.model.ModelEntity;

/**
 * PartyHelper
 *
 * @author     <a href="mailto:epabst@bigfoot.com">Eric Pabst</a>
 * @version    $Rev$
 * @since      2.0
 */
public class PartyHelper {
    
    public static final String module = PartyHelper.class.getName();
    
    public static String getPartyName(GenericValue partyObject) {
        return getPartyName(partyObject, false);
    }

    public static String getPartyName(GenericDelegator delegator, String partyId, boolean lastNameFirst) {
        GenericValue partyObject = null;
        try {
            partyObject = delegator.findByPrimaryKey("PartyNameView", UtilMisc.toMap("partyId", partyId));
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error finding PartyNameView in getPartyName", module);
        }
        if (partyObject == null) {
            return partyId;
        } else {
            return formatPartyNameObject(partyObject, lastNameFirst);
        }        
    }

    public static String getPartyName(GenericValue partyObject, boolean lastNameFirst) {
        if (partyObject == null) {
            return "";
        }
        if ("PartyGroup".equals(partyObject.getEntityName()) || "Person".equals(partyObject.getEntityName())) {
            return formatPartyNameObject(partyObject, lastNameFirst);
        } else {
            String partyId = null;
            try {
                partyId = partyObject.getString("partyId");
            } catch (IllegalArgumentException e) {
                Debug.logError(e, "Party object does not contain a party ID", module);
            }

            if (partyId == null) {
                Debug.logWarning("No party ID found; cannot get name based on entity: " + partyObject.getEntityName(), module);
                return "";
            } else {
                return getPartyName(partyObject.getDelegator(), partyId, lastNameFirst);
            }
        }
    }
    
    public static String formatPartyNameObject(GenericValue partyValue, boolean lastNameFirst) {
        if (partyValue == null) {
            return "";
        }
        StringBuffer result = new StringBuffer();
        ModelEntity modelEntity = partyValue.getModelEntity();
        if (modelEntity.isField("firstName") && modelEntity.isField("middleName") && modelEntity.isField("lastName")) {
            if (lastNameFirst) {
                if (UtilFormatOut.checkNull(partyValue.getString("lastName")) != null) {
                    result.append(UtilFormatOut.checkNull(partyValue.getString("lastName")));
                    if (partyValue.getString("firstName") != null) {
                        result.append(", ");
                    }
                }
                result.append(UtilFormatOut.checkNull(partyValue.getString("firstName")));
            } else {
                result.append(UtilFormatOut.ifNotEmpty(partyValue.getString("firstName"), "", " "));
                result.append(UtilFormatOut.ifNotEmpty(partyValue.getString("middleName"), "", " "));
                result.append(UtilFormatOut.checkNull(partyValue.getString("lastName")));
            }
        }
        if (modelEntity.isField("groupName") && partyValue.get("groupName") != null) {
            result.append(partyValue.getString("groupName"));
        }
        return result.toString();
    }
}
