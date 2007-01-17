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

package org.ofbiz.accounting.finaccount;

import java.sql.Timestamp;
import java.util.Map;

import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;

import org.ofbiz.order.finaccount.FinAccountHelper;

public class FinAccountServices {
    
    public static final String module = FinAccountServices.class.getName();
    
    public static Map createFinAccountForStore(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String productStoreId = (String) context.get("productStoreId");
        String finAccountTypeId = (String) context.get("finAccountTypeId");
        
        try {
            // get the product store id and use it to generate a unique fin account code
            GenericValue productStoreFinAccountSetting = delegator.findByPrimaryKeyCache("ProductStoreFinActSetting", UtilMisc.toMap("productStoreId", productStoreId, "finAccountTypeId", finAccountTypeId));
            if (productStoreFinAccountSetting == null) {
                return ServiceUtil.returnError("No settings found for store [" + productStoreId + "] for fin account type [" + finAccountTypeId + "]");
            }
            
            Long accountCodeLength = productStoreFinAccountSetting.getLong("accountCodeLength");
            Long accountValidDays = productStoreFinAccountSetting.getLong("accountValidDays");
            
            // automatically set the parameters for the create fin account service
            ModelService createService = dctx.getModelService("createFinAccount");
            Map inContext = createService.makeValid(context, "IN");
            Timestamp now = UtilDateTime.nowTimestamp();
            // now use our values
            inContext.put("fromDate", now);
            inContext.put("thruDate", UtilDateTime.getDayEnd(now, accountValidDays.intValue()));
            String finAccountCode = FinAccountHelper.getNewFinAccountCode(accountCodeLength.intValue(), delegator);
            inContext.put("finAccountCode", finAccountCode);
            inContext.put("userLogin", userLogin);
            Map createResult = dispatcher.runSync("createFinAccount", inContext);
            
            if (ServiceUtil.isError(createResult)) {
                return createResult;
            } else {
                Map result = ServiceUtil.returnSuccess();
                result.put("finAccountId", createResult.get("finAccountId"));
                result.put("finAccountCode", finAccountCode);
                return result;
            }
        } catch (GenericEntityException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        } catch (GenericServiceException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }
        
    }
}
