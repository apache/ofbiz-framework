package com.simbaquartz.xapi.services.product;

import com.fidelissd.zcp.xcommon.collections.FastList;
import com.fidelissd.zcp.xcommon.collections.FastMap;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;

import java.util.List;
import java.util.Map;

public class ProductCategoryServices {

    public static Map<String, Object> getProductsForCategory(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        String categoryId = (String) context.get("productCategoryId");
        Integer viewSize = (Integer) context.get("viewSize");
        Integer startIndex = (Integer) context.get("startIndex");
        List<Map<String, Object>> productsList = FastList.newInstance();

        if (startIndex == null) {
            startIndex = 0;
        }
        int highIndex = (startIndex + 1) * viewSize;

        EntityCondition cond1 = EntityCondition.makeCondition(UtilMisc.toList(
                            EntityCondition.makeCondition("productCategoryId", EntityOperator.EQUALS, categoryId),
                            EntityCondition.makeCondition("isVirtual", EntityOperator.EQUALS, "Y")), EntityOperator.AND);
        EntityCondition cond2 = EntityCondition.makeCondition(UtilMisc.toList(
                            EntityCondition.makeCondition("primaryProductCategoryId", EntityOperator.EQUALS, "GEMSTONES"),
                            EntityCondition.makeCondition("isVirtual", EntityOperator.EQUALS, "N"),
                            EntityCondition.makeCondition("productCategoryId", EntityOperator.EQUALS, categoryId)), EntityOperator.AND);
        EntityCondition ecl1 = EntityCondition.makeCondition(
            EntityCondition.makeCondition(cond1),
            EntityOperator.OR,
            EntityCondition.makeCondition(cond2));
        EntityQuery eq = EntityQuery.use(delegator).from("ProductAndCategoryMember")
                .where(ecl1)
                // .orderBy("-lastUpdatedStamp")
                .maxRows(highIndex)
                .cursorScrollInsensitive();
        try (EntityListIterator pli = eq.queryIterator()) {
            List<GenericValue> categoryProducts = pli.getPartialList(startIndex, viewSize);
            if(UtilValidate.isNotEmpty(categoryProducts)) {
                for (GenericValue categoryProduct : categoryProducts) {
                    Map<String, Object> productDetails = FastMap.newInstance();
                    GenericValue product = EntityQuery.use(delegator).from("Product").where("productId", categoryProduct.get("productId")).queryOne();
                    if (UtilValidate.isNotEmpty(product)) {
                        productDetails.put("productId", product.getString("productId"));
                        productDetails.put("productTypeId", product.getString("productTypeId"));
                        productDetails.put("description", product.getString("description"));
                        productDetails.put("productName", product.getString("productName"));
                        productsList.add(productDetails);
                    }
                }
            }
            int count = 0;
            count = (int) eq.queryCount();
            serviceResult.put("totalNumberOfRecords",count);
        } catch (GenericEntityException e) {
            e.printStackTrace();
            return ServiceUtil.returnError("Error trying to get products for category: " + categoryId);
        }
        serviceResult.put("products", productsList);
        return serviceResult;
    }

}
