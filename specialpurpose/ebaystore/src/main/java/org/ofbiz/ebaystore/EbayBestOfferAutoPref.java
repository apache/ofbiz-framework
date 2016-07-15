/*
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
 */
package org.ofbiz.ebaystore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityQuery;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

public class EbayBestOfferAutoPref {

    public static final String module = EbayBestOfferAutoPref.class.getName();

    public static Map<String, Object> ebayBestOfferPrefCond(DispatchContext dctx, Map<String, ? extends Object> context) {

            Map<String, Object> result = new HashMap<String, Object>();
            LocalDispatcher dispatcher = dctx.getDispatcher();
            GenericValue userLogin = (GenericValue) context.get("userLogin");
            Delegator delegator = dctx.getDelegator();
            String productStoreId = (String) context.get("productStoreId");
            String enabled = (String) context.get("enabled");
            String condition1 = (String) context.get("condition1");
            String condition2 = (String) context.get("condition2");
            String condition3 = (String) context.get("condition3");
            String condition4 = (String) context.get("condition4");
            String condition5 = (String) context.get("condition5");
            String condition6 = (String) context.get("condition6");
            String condition7 = (String) context.get("condition7");
            String condition8 = (String) context.get("condition8");
            String condition9 = (String) context.get("condition9");
            String condition10 = (String) context.get("condition10");
            String condition11 = (String) context.get("condition11");
            try {
                Map<String, Object> ebayCondition1 = UtilMisc.<String, Object>toMap("userLogin", userLogin);
                ebayCondition1.put("acceptanceCondition", condition1);

                Map<String, Object> ebayCondition2 = UtilMisc.<String, Object>toMap("userLogin", userLogin);
                ebayCondition2.put("acceptanceCondition", condition2);

                Map<String, Object> ebayCondition3 = UtilMisc.<String, Object>toMap("userLogin", userLogin);
                ebayCondition3.put("acceptanceCondition", condition3);

                Map<String, Object> ebayCondition4 = UtilMisc.<String, Object>toMap("userLogin", userLogin);
                ebayCondition4.put("acceptanceCondition", condition4);

                Map<String, Object> ebayCondition5 = UtilMisc.<String, Object>toMap("userLogin", userLogin);
                ebayCondition5.put("acceptanceCondition", condition5);

                Map<String, Object> ebayCondition6 = UtilMisc.<String, Object>toMap("userLogin", userLogin);
                ebayCondition6.put("acceptanceCondition", condition6);

                Map<String, Object> ebayCondition7 = UtilMisc.<String, Object>toMap("userLogin", userLogin);
                ebayCondition7.put("acceptanceCondition", condition7);

                Map<String, Object> ebayCondition8 = UtilMisc.<String, Object>toMap("userLogin", userLogin);
                ebayCondition8.put("acceptanceCondition", condition8);

                Map<String, Object> ebayCondition9 = UtilMisc.<String, Object>toMap("userLogin", userLogin);
                ebayCondition9.put("acceptanceCondition", condition9);

                Map<String, Object> ebayCondition10 = UtilMisc.<String, Object>toMap("userLogin", userLogin);
                ebayCondition10.put("acceptanceCondition", condition10);

                Map<String, Object> ebayCondition11 = UtilMisc.<String, Object>toMap("userLogin", userLogin);
                ebayCondition11.put("acceptanceCondition", condition11);

            GenericValue productStorePref = EntityQuery.use(delegator).from("EbayProductStorePref").where("productStoreId", productStoreId, "autoPrefEnumId", "EBAY_AUTO_BEST_OFFER").queryOne();
            if (UtilValidate.isEmpty(productStorePref)) {
                 String prefCondId1 = delegator.getNextSeqId("EbayProductStorePrefCond");
                 String parentPrefCondId = prefCondId1;

                ebayCondition1.put("prefCondId", prefCondId1);
                ebayCondition1.put("parentPrefCondId", parentPrefCondId);
                ebayCondition1.put("description", "Kind of Price Field");
                dispatcher.runSync("createEbayProductStorePrefCond", ebayCondition1);

                String prefCondId2 = delegator.getNextSeqId("EbayProductStorePrefCond");
                ebayCondition2.put("prefCondId", prefCondId2);
                ebayCondition2.put("parentPrefCondId", parentPrefCondId);
                ebayCondition2.put("description", "acceptBestOfferValue Field");
                dispatcher.runSync("createEbayProductStorePrefCond", ebayCondition2);

                String prefCondId3 = delegator.getNextSeqId("EbayProductStorePrefCond");
                ebayCondition3.put("prefCondId", prefCondId3);
                ebayCondition3.put("parentPrefCondId", parentPrefCondId);
                ebayCondition3.put("description", "rejectOffer Field");
                dispatcher.runSync("createEbayProductStorePrefCond", ebayCondition3);

                String prefCondId4 = delegator.getNextSeqId("EbayProductStorePrefCond");
                ebayCondition4.put("prefCondId", prefCondId4);
                ebayCondition4.put("parentPrefCondId", parentPrefCondId);
                ebayCondition4.put("description", "ignoreOfferMessage Field");
                dispatcher.runSync("createEbayProductStorePrefCond", ebayCondition4);

                String prefCondId5 = delegator.getNextSeqId("EbayProductStorePrefCond");
                ebayCondition5.put("prefCondId", prefCondId5);
                ebayCondition5.put("parentPrefCondId", parentPrefCondId);
                ebayCondition5.put("description", "rejectGreaterEnable Field");
                dispatcher.runSync("createEbayProductStorePrefCond", ebayCondition5);

                String prefCondId6 = delegator.getNextSeqId("EbayProductStorePrefCond");
                ebayCondition6.put("prefCondId", prefCondId6);
                ebayCondition6.put("parentPrefCondId", parentPrefCondId);
                ebayCondition6.put("description", "greaterValue Field");
                dispatcher.runSync("createEbayProductStorePrefCond", ebayCondition6);

                String prefCondId7 = delegator.getNextSeqId("EbayProductStorePrefCond");
                ebayCondition7.put("prefCondId", prefCondId7);
                ebayCondition7.put("parentPrefCondId", parentPrefCondId);
                ebayCondition7.put("description", "lessValue Field");
                dispatcher.runSync("createEbayProductStorePrefCond", ebayCondition7);

                String prefCondId8 = delegator.getNextSeqId("EbayProductStorePrefCond");
                ebayCondition8.put("prefCondId", prefCondId8);
                ebayCondition8.put("parentPrefCondId", parentPrefCondId);
                ebayCondition8.put("description", "rejectGreaterMsg Field");
                dispatcher.runSync("createEbayProductStorePrefCond", ebayCondition8);

                String prefCondId9 = delegator.getNextSeqId("EbayProductStorePrefCond");
                ebayCondition9.put("prefCondId", prefCondId9);
                ebayCondition9.put("parentPrefCondId", parentPrefCondId);
                ebayCondition9.put("description", "rejectLessEnable Field");
                dispatcher.runSync("createEbayProductStorePrefCond", ebayCondition9);

                String prefCondId10 = delegator.getNextSeqId("EbayProductStorePrefCond");
                ebayCondition10.put("prefCondId", prefCondId10);
                ebayCondition10.put("parentPrefCondId", parentPrefCondId);
                ebayCondition10.put("description", "lessThanValue Field");
                dispatcher.runSync("createEbayProductStorePrefCond", ebayCondition10);

                String prefCondId11 = delegator.getNextSeqId("EbayProductStorePrefCond");
                ebayCondition11.put("prefCondId", prefCondId11);
                ebayCondition11.put("parentPrefCondId", parentPrefCondId);
                ebayCondition11.put("description", "rejectLessMsg Field");
                dispatcher.runSync("createEbayProductStorePrefCond", ebayCondition11);

                Map<String, Object> ebayPref = UtilMisc.<String, Object>toMap("userLogin", userLogin, "serviceName", "autoBestOffer");
                ebayPref.put("parentPrefCondId",parentPrefCondId);
                ebayPref.put("enabled", enabled);
                ebayPref.put("autoPrefEnumId", "EBAY_AUTO_BEST_OFFER");
                ebayPref.put("productStoreId",productStoreId);
                dispatcher.runSync("createEbayProductStorePref",ebayPref);
            } else {
                Map<String, Object> ebayPref = UtilMisc.<String, Object>toMap("userLogin", userLogin, "serviceName", "autoBestOffer");
                ebayPref.put("enabled", enabled);
                ebayPref.put("autoPrefEnumId", "EBAY_AUTO_BEST_OFFER");
                ebayPref.put("productStoreId",productStoreId);
                dispatcher.runSync("updateEbayProductStorePref",ebayPref);

                String parentPrefCondId = productStorePref.getString("parentPrefCondId");
                List<GenericValue> productPref = EntityQuery.use(delegator).from("EbayProductStorePrefCond").where("parentPrefCondId",parentPrefCondId).queryList();
                if (productPref.size() != 0) {
                    String[] condition = {condition1, condition2, condition3, condition4, condition5, condition6, condition7, condition8, condition9, condition10, condition11};
                    Map<String, Object> ebayPrefCond = UtilMisc.<String, Object>toMap("userLogin", userLogin);
                    for (int i = 0; i < productPref.size(); i++) {
                        ebayPrefCond.put("prefCondId",productPref.get(i).getString("prefCondId"));
                        ebayPrefCond.put("acceptanceCondition",condition[i]);
                        dispatcher.runSync("updateEbayProductStorePrefCond",ebayPrefCond);
                    }
                }
                
            }
            
        } catch (GenericServiceException e) {
            String errorMessage = "Store best offer to entity failed.";
            result = ServiceUtil.returnError(errorMessage);
            return result;
        } catch (GenericEntityException e) {
            String errorMessage = "Store best offer to entity failed.";
            result = ServiceUtil.returnError(errorMessage);
            return result;
        }
        String successMsg = "Store best offer to entity successful.";
        result = ServiceUtil.returnSuccess(successMsg);
        return result;
    }
}
