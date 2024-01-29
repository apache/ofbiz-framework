package com.simbaquartz.xapi.helper;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;


/**
 * Created by Admin on 10/3/17.
 */
public class AxDiscountHelper {
    private static final String module = AxDiscountHelper.class.getName();


    public static GenericValue getProductPromoRecord(Delegator delegator, String productPromoId) {
        GenericValue productPromoRecord = null;
        try {
            productPromoRecord = EntityQuery.use(delegator).from("ProductPromo").where("productPromoId", productPromoId).cache(true).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return productPromoRecord;
    }

    public static Boolean isExistingDiscountName (Delegator delegator, String name) {
        GenericValue productPromoRecord = null;
        try {
            productPromoRecord = EntityQuery.use(delegator).from("ProductPromo").where("promoName",name).cache(true).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }

        if (UtilValidate.isNotEmpty(productPromoRecord)) {
            return true;
        }
        return false;
    }

    public static Boolean isExistingDiscountCode (Delegator delegator, String code) {
        GenericValue productPromoCodeRecord = null;
        try {
            productPromoCodeRecord = EntityQuery.use(delegator).from("ProductPromoCode").where("productPromoCodeId",code).cache(true).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }

        if (UtilValidate.isNotEmpty(productPromoCodeRecord)) {
            return true;
        }
        return false;
    }

    public static GenericValue getProductPromoCodeRecord(Delegator delegator, String productPromoId) {
        GenericValue productPromoCodeRecord = null;
        try {
            productPromoCodeRecord = EntityQuery.use(delegator).from("ProductPromoCode").where("productPromoId", productPromoId).cache(true).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return productPromoCodeRecord;
    }

    public static GenericValue getProductPromoRuleRecord(Delegator delegator, String productPromoId) {
        GenericValue productPromoRuleRecord = null;
        try {
            productPromoRuleRecord = EntityQuery.use(delegator).from("ProductPromoRule").where("productPromoId", productPromoId).cache(true).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return productPromoRuleRecord;
    }

    public static GenericValue getProductPromoCondRecord(Delegator delegator, String productPromoId) {
        GenericValue productPromoCondRecord = null;
        try {
            productPromoCondRecord = EntityQuery.use(delegator).from("ProductPromoCond").where("productPromoId", productPromoId).cache(true).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return productPromoCondRecord;
    }

    public static GenericValue getProductPromoApplRecord(Delegator delegator, String productPromoId) {
        GenericValue productStorePromoApplRecord = null;
        try {
            productStorePromoApplRecord = EntityQuery.use(delegator).from("ProductStorePromoAppl").where("productPromoId", productPromoId).cache(true).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return productStorePromoApplRecord;
    }

    public static GenericValue getProductPromoActionRecord(Delegator delegator, String productPromoId) {
        GenericValue productPromoActionRecord = null;
        try {
            productPromoActionRecord = EntityQuery.use(delegator).from("ProductPromoAction").where("productPromoId", productPromoId).cache(true).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return productPromoActionRecord;
    }

    public static Long getProductPromoUseLimitCode(Delegator delegator, String productPromoId) {

        GenericValue productPromoCodeRecord = null;
        try {
            productPromoCodeRecord = EntityQuery.use(delegator).from("ProductPromo").where("productPromoId", productPromoId).cache(true).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        Long useLimitPerCode = productPromoCodeRecord.getLong("useLimitPerPromotion");
        return useLimitPerCode;
    }
}
