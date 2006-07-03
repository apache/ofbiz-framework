/*
 * $Id: PartyRelationshipServices.java 6449 2005-12-30 16:18:57Z sichen $
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
 */
package org.ofbiz.party.party;

import java.util.HashMap;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.security.Security;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;

/**
 * Services for Party Relationship maintenance
 *
 * @author     <a href="mailto:cworley@chris-n-april.com">Christopher Worley</a>
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class PartyRelationshipServices {

    public static final String module = PartyRelationshipServices.class.getName();
    public static final String resource = "PartyUiLabels";

    /** Creates a PartyRelationshipType
     *@param ctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map createPartyRelationshipType(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        GenericDelegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, "PARTYMGR", "_CREATE");

        if (result.size() > 0)
            return result;

        GenericValue partyRelationshipType = delegator.makeValue("PartyRelationshipType", UtilMisc.toMap("partyRelationshipTypeId", context.get("partyRelationshipTypeId")));

        partyRelationshipType.set("parentTypeId", context.get("parentTypeId"), false);
        partyRelationshipType.set("hasTable", context.get("hasTable"), false);
        partyRelationshipType.set("roleTypeIdValidFrom", context.get("roleTypeIdValidFrom"), false);
        partyRelationshipType.set("roleTypeIdValidTo", context.get("roleTypeIdValidTo"), false);
        partyRelationshipType.set("description", context.get("description"), false);
        partyRelationshipType.set("partyRelationshipName", context.get("partyRelationshipName"), false);

        try {
            if (delegator.findByPrimaryKey(partyRelationshipType.getPrimaryKey()) != null) {
                return ServiceUtil.returnError("Could not create party relationship type: already exists");
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
            return ServiceUtil.returnError("Could not create party relationship type (read failure): " + e.getMessage());
        }

        try {
            partyRelationshipType.create();
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
            return ServiceUtil.returnError("Could not create party relationship type (write failure): " + e.getMessage());
        }

        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }
}
