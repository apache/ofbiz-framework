package org.ofbiz.ebaystore;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

public class EbayBestOfferAutoPref {

    public static final String module = EbayBestOfferAutoPref.class.getName();

    public static Map<String, Object> ebayBestOfferPrefCond(DispatchContext dctx, Map<String, ? extends Object> context) {

            Map<String, Object> result = FastMap.newInstance();
            LocalDispatcher dispatcher = dctx.getDispatcher();
            GenericValue userLogin = (GenericValue) context.get("userLogin");
            Delegator delegator = dctx.getDelegator();
            Locale locale = (Locale) context.get("locale");
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
                Map ebayCondition1 = UtilMisc.toMap("userLogin", userLogin);
                ebayCondition1.put("acceptanceCondition", condition1);

                Map ebayCondition2 = UtilMisc.toMap("userLogin", userLogin);
                ebayCondition2.put("acceptanceCondition", condition2);

                Map ebayCondition3 = UtilMisc.toMap("userLogin", userLogin);
                ebayCondition3.put("acceptanceCondition", condition3);

                Map ebayCondition4 = UtilMisc.toMap("userLogin", userLogin);
                ebayCondition4.put("acceptanceCondition", condition4);

                Map ebayCondition5 = UtilMisc.toMap("userLogin", userLogin);
                ebayCondition5.put("acceptanceCondition", condition5);

                Map ebayCondition6 = UtilMisc.toMap("userLogin", userLogin);
                ebayCondition6.put("acceptanceCondition", condition6);

                Map ebayCondition7 = UtilMisc.toMap("userLogin", userLogin);
                ebayCondition7.put("acceptanceCondition", condition7);

                Map ebayCondition8 = UtilMisc.toMap("userLogin", userLogin);
                ebayCondition8.put("acceptanceCondition", condition8);

                Map ebayCondition9 = UtilMisc.toMap("userLogin", userLogin);
                ebayCondition9.put("acceptanceCondition", condition9);

                Map ebayCondition10 = UtilMisc.toMap("userLogin", userLogin);
                ebayCondition10.put("acceptanceCondition", condition10);

                Map ebayCondition11 = UtilMisc.toMap("userLogin", userLogin);
                ebayCondition11.put("acceptanceCondition", condition11);

            GenericValue productStorePref = delegator.findByPrimaryKey("EbayProductStorePref", UtilMisc.toMap("productStoreId", productStoreId, "autoPrefEnumId", "EBAY_AUTO_BEST_OFFER"));
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

                Map ebayPref = UtilMisc.toMap("userLogin", userLogin, "serviceName", "autoBestOffer");
                ebayPref.put("parentPrefCondId",parentPrefCondId);
                ebayPref.put("enabled", enabled);
                ebayPref.put("autoPrefEnumId", "EBAY_AUTO_BEST_OFFER");
                ebayPref.put("productStoreId",productStoreId);
                dispatcher.runSync("createEbayProductStorePref",ebayPref);
            } else {
                Map ebayPref = UtilMisc.toMap("userLogin", userLogin, "serviceName", "autoBestOffer");
                ebayPref.put("enabled", enabled);
                ebayPref.put("autoPrefEnumId", "EBAY_AUTO_BEST_OFFER");
                ebayPref.put("productStoreId",productStoreId);
                dispatcher.runSync("updateEbayProductStorePref",ebayPref);

                String parentPrefCondId = productStorePref.getString("parentPrefCondId");
                List<GenericValue> productPref = delegator.findByAnd("EbayProductStorePrefCond", UtilMisc.toMap("parentPrefCondId",parentPrefCondId));
                if (productPref.size() != 0) {
                    String[] condition = {condition1, condition2, condition3, condition4, condition5, condition6, condition7, condition8, condition9, condition10, condition11};
                    Map ebayPrefCond = UtilMisc.toMap("userLogin", userLogin);
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
        String successMsg = "Store best offer to entity Successfull.";
        result = ServiceUtil.returnSuccess(successMsg);
        return result;
    }
}
