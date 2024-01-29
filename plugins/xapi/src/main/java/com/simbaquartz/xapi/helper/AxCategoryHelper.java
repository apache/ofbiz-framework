package com.simbaquartz.xapi.helper;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import java.util.List;

/**
 * Created by Admin on 10/4/17.
 */
public class AxCategoryHelper {
    private static final String module = AxCategoryHelper.class.getName();

    public static Boolean isExistingCategoryName(Delegator delegator, String storeId, String name) {
        List<GenericValue> productCategories = null;
        try {
            productCategories = EntityQuery.use(delegator).from("ProductStoreCategory").where("productStoreId", storeId).queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        for(GenericValue productCategory : productCategories) {
           String categoryId = productCategory.getString("productCategoryId");

            GenericValue productCategoryRecord = null;
            try {
                productCategoryRecord = EntityQuery.use(delegator).from("ProductCategory").where("productCategoryId", categoryId).cache(true).queryOne();
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
            if (UtilValidate.isNotEmpty(productCategoryRecord)) {
                String existingCategoryName = productCategoryRecord.getString("categoryName");
                if (UtilValidate.isNotEmpty(existingCategoryName)) {
                    if(existingCategoryName.equals(name))
                    return true;
                }
            }
        }
        return false;
    }

    public static Boolean isExistingStoreCategory(Delegator delegator, String productStoreId, String productCategoryId) {
        GenericValue storeCategory = null;
        try {
            storeCategory = EntityQuery.use(delegator).from("ProductStoreCategory").where("productStoreId", productStoreId, "productCategoryId", productCategoryId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        if (UtilValidate.isEmpty(storeCategory)) {
            return false;
        }
        return true;
    }
}
