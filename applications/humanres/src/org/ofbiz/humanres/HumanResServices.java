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

package org.ofbiz.humanres;

import java.sql.Timestamp;
import java.util.Locale;
import java.util.Map;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.security.Security;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import javolution.util.FastMap;

/**
 * Services for Human Resources
 */

public class HumanResServices {
    
    public static final String module = HumanResServices.class.getName();
    public static final String resource = "HumanResUiLabels";
    
    /**
     * Create a PartyQual entity.
     * @param ctx The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map createPartyQual(DispatchContext ctx, Map context) {
        Map result = FastMap.newInstance();
        GenericDelegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        
        String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, "PARTYMGR", "_QAL_CREATE");
        if (result.size() > 0)
            return result;
        
        String partyQualTypeId = (String) context.get("partyQualTypeId");
        String statusId = (String) context.get("statusId");
        String verifStatusId = (String) context.get("verifStatusId");
        Timestamp fromDate = (Timestamp) context.get("fromDate");
        String errMsg = null;
        
        
        try {
            String title = (String) context.get("title");
            String qualificationDesc = (String) context.get("qualificationDesc");
            Timestamp thruDate = (Timestamp) context.get("thruDate");
            if (fromDate == null) {
                errMsg = UtilProperties.getMessage(resource,"HumanResServices.PartyQualFailureMissingParam", locale);
                return ServiceUtil.returnError(errMsg);
            }
            GenericValue partyQual = delegator.makeValue("PartyQual", UtilMisc.toMap(new Object[] {
                "partyId", partyId, 
                "partyQualTypeId", partyQualTypeId,
                "title", title,
                "statusId", statusId,
                "qualificationDesc", qualificationDesc,
                "verifStatusId", verifStatusId,
                "fromDate", fromDate,
                "thruDate", thruDate
            }));
            partyQual.setNonPKFields(context);
            partyQual.create();
            
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
            Map messageMap = UtilMisc.toMap("errMessage", e.getMessage());
            errMsg = UtilProperties.getMessage(resource, "HumanResServices.PartyQualFailureDataSource", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }
        return UtilMisc.toMap(
                "partyId", partyId,
                "partyQualTypeId", partyQualTypeId,
                "fromDate", fromDate,
                ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
    }
}
