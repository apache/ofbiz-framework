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
package org.apache.ofbiz.accounting.ledger;

import java.math.BigDecimal;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

public class GeneralLedgerServices {

    private static final String MODULE = GeneralLedgerServices.class.getName();

    public static Map<String, Object> createUpdateCostCenter(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Map<String, Object> createGlAcctCatMemFromCostCentersMap = null;
        String glAccountId = (String) context.get("glAccountId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Map<String, String> amountPercentageMap = UtilGenerics.cast(context.get("amountPercentageMap"));
        BigDecimal totalAmountPercentage = GeneralLedgerServices.calculateCostCenterTotal(amountPercentageMap);
        Map<String, Object> result = ServiceUtil.returnSuccess();
        for (Map.Entry<String, String> rowEntry : amountPercentageMap.entrySet()) {
            String rowValue = rowEntry.getValue();
            if (UtilValidate.isNotEmpty(rowValue)) {
                createGlAcctCatMemFromCostCentersMap = UtilMisc.toMap("glAccountId", glAccountId,
                        "glAccountCategoryId", rowEntry.getKey(), "amountPercentage", new BigDecimal(rowValue),
                        "userLogin", userLogin, "totalAmountPercentage", totalAmountPercentage);
            } else {
                createGlAcctCatMemFromCostCentersMap = UtilMisc.toMap("glAccountId", glAccountId,
                        "glAccountCategoryId", rowEntry.getKey(), "amountPercentage", new BigDecimal(0),
                        "userLogin", userLogin, "totalAmountPercentage", totalAmountPercentage);
            }
            try {
                result = dispatcher.runSync("createGlAcctCatMemFromCostCenters", createGlAcctCatMemFromCostCentersMap);
                if (ServiceUtil.isError(result)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }
        }
        return result;
    }

    public static BigDecimal calculateCostCenterTotal(Map<String, String> amountPercentageMap) {
        BigDecimal totalAmountPercentage = BigDecimal.ZERO;
        for (Map.Entry<String, String> rowEntry : amountPercentageMap.entrySet()) {
            if (UtilValidate.isNotEmpty(rowEntry.getValue())) {
                BigDecimal rowValue = new BigDecimal(rowEntry.getValue());
                totalAmountPercentage = totalAmountPercentage.add(rowValue);
            }
        }
        return totalAmountPercentage;
    }
}
