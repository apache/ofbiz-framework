/*
 * $Id: PromoServices.java 5462 2005-08-05 18:35:48Z jonesde $
 * 
 * Copyright (c) 2003-2005 The Open For Business Project - www.ofbiz.org
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
 * 
 * @author Nathan De Graw
 * @version $Rev$
 * @since 3.0
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
                GenericValue productStorePromo = delegator.makeValue("ProductStorePromo", null);
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
