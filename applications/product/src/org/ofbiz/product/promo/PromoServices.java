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
package org.ofbiz.product.promo;

import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

/**
 * Promotions Services
 */
public class PromoServices {

    public final static String module = PromoServices.class.getName();

    public static Map createProductPromoCodeSet(DispatchContext dctx, Map context) {
        //GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Long quantity = (Long) context.get("quantity");
        //Long useLimitPerCode = (Long) context.get("useLimitPerCode");
        //Long useLimitPerCustomer = (Long) context.get("useLimitPerCustomer");
        //GenericValue promoItem = null;
        //GenericValue newItem = null;

        StringBuffer bankOfNumbers = new StringBuffer();
        for (long i = 0; i < quantity.longValue(); i++) {
            Map createProductPromoCodeMap = null;
            try {
                createProductPromoCodeMap = dispatcher.runSync("createProductPromoCode", dctx.makeValidContext("createProductPromoCode", "IN", context));
            } catch (GenericServiceException err) {
                return ServiceUtil.returnError("Could not create a bank of promo codes", null, null, createProductPromoCodeMap);
            }
            if (ServiceUtil.isError(createProductPromoCodeMap)) {
                // what to do here? try again?
                return ServiceUtil.returnError("Could not create a bank of promo codes", null, null, createProductPromoCodeMap);
            }
            bankOfNumbers.append((String) createProductPromoCodeMap.get("productPromoCodeId"));
            bankOfNumbers.append("<br/>");
        }

        return ServiceUtil.returnSuccess(bankOfNumbers.toString());
    }

    public static Map purgeOldStoreAutoPromos(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        String productStoreId = (String) context.get("productStoreId");
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        
        List condList = new LinkedList();
        if (UtilValidate.isEmpty(productStoreId)) {
            condList.add(new EntityExpr("productStoreId", EntityOperator.EQUALS, productStoreId));
        }
        condList.add(new EntityExpr("userEntered", EntityOperator.EQUALS, "Y"));
        condList.add(new EntityExpr("thruDate", EntityOperator.NOT_EQUAL, null));
        condList.add(new EntityExpr("thruDate", EntityOperator.LESS_THAN, nowTimestamp));
        EntityCondition cond = new EntityConditionList(condList, EntityOperator.AND);
        
        try {
            EntityListIterator eli = delegator.findListIteratorByCondition("ProductStorePromoAndAppl", cond, null, null);
            GenericValue productStorePromoAndAppl = null;
            while ((productStorePromoAndAppl = (GenericValue) eli.next()) != null) {
                GenericValue productStorePromo = delegator.makeValue("ProductStorePromoAppl", null);
                productStorePromo.setAllFields(productStorePromoAndAppl, true, null, null);
                productStorePromo.remove();
            }
            eli.close();
        } catch (GenericEntityException e) {
            String errMsg = "Error removing expired ProductStorePromo records: " + e.toString();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }
        
        return ServiceUtil.returnSuccess();
    }
}
