package com.simbaquartz.xapi.helper;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.ServiceUtil;

import java.math.BigDecimal;

import static org.apache.ofbiz.base.util.UtilValidate.module;

/**
 * Created by Admin on 7/15/17.
 */
public class AxProductHelper {

    public static Boolean isExistingProduct(Delegator delegator, String productName) {
        GenericValue productRecord = null;
        try {
            productRecord = EntityQuery.use(delegator).from("Product").where("productName", productName).cache(true).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        if (UtilValidate.isNotEmpty(productRecord)) {
            return true;
        }
        return false;
    }

    public static Boolean isExistingTag(Delegator delegator, String name) {
        GenericValue tagRecord = null;
        try {
            tagRecord = EntityQuery.use(delegator).from("Tag").where("tagName", name).cache(true).queryFirst();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }

        if (UtilValidate.isNotEmpty(tagRecord)) {
            return true;
        }
        return false;
    }

    public static Boolean isExistingTagId(Delegator delegator, String tagId) {
        GenericValue tagRecord = null;
        try {
            tagRecord = EntityQuery.use(delegator).from("Tag").where("tagId", tagId).cache(true).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        if (UtilValidate.isNotEmpty(tagRecord)) {
            return true;
        }
        return false;
    }

    public static GenericValue getTagRecord(Delegator delegator, String tagId) {
        GenericValue tagRecord = null;
        try {
            tagRecord = EntityQuery.use(delegator).from("Tag").where("tagId", tagId).cache(true).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return tagRecord;
    }

    public static GenericValue getProductTagRecord(Delegator delegator, String tagId) {
        GenericValue productTagRecord = null;
        try {
            productTagRecord = EntityQuery.use(delegator).from("ProductTag").where("tagId", tagId).cache(true).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return productTagRecord;
    }

    public static String prepareProductUrlHandle(String productName) {
        //TODO:Implement self
        return "";
    }

    public static Boolean isProductConfigurable(Delegator delegator, String productId) {
        GenericValue productConfiguration = null;
        try {
            productConfiguration = EntityQuery.use(delegator).from("ProductConfigProduct").where("productId", productId).queryFirst();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }

        if (UtilValidate.isEmpty(productConfiguration)) {
            return false;
        }

        return true;
    }

    public static Boolean isProductAddedToCategory(Delegator delegator, String productCategoryId, String productId) {
        GenericValue storeProducts = null;
        try {
            storeProducts = EntityQuery.use(delegator).from("ProductCategoryMember").where("productCategoryId", productCategoryId, "productId", productId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        if (UtilValidate.isNotEmpty(storeProducts)) {
            return false;
        }
        return true;
    }

    public static String getProductUrl(Delegator delegator, String productId) {
        String productUrl = "";
        GenericValue productContent = null;
        try {
            productContent = EntityQuery.use(delegator).from("ProductContent").where("productId", productId).cache(true).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        if (UtilValidate.isNotEmpty(productContent)) {
            String contentId = productContent.getString("contentId");
            GenericValue content = null;
            try {
                content = EntityQuery.use(delegator).from("Content").where("contentId", contentId).cache().queryOne();
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
            if (UtilValidate.isNotEmpty(content)) {
                String dataResourceId = content.getString("dataResourceId");
                GenericValue electronicText = null;
                try {
                    electronicText = EntityQuery.use(delegator).from("ElectronicText").where("dataResourceId", dataResourceId).cache().queryOne();
                    if (UtilValidate.isNotEmpty(electronicText)) {
                        productUrl = electronicText.getString("textData");
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                    return productUrl;
                }
            }
        }
        return productUrl;
    }

    public static GenericValue getProductDefaultPriceRecord(Delegator delegator, String productId) {
        GenericValue productDefaultPrice = null;
        try {
            productDefaultPrice = EntityQuery.use(delegator).from("ProductPrice").where("productId", productId, "productPriceTypeId", "DEFAULT_PRICE").cache().queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return productDefaultPrice;
    }

    public static GenericValue getProductSku(Delegator delegator, String productId) {
        GenericValue existingSku = null;
        try {
            existingSku = EntityQuery.use(delegator).from("GoodIdentification").where("goodIdentificationTypeId", "SKU", "productId", productId).cache(true).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return existingSku;
    }

    public static GenericValue getProductBarcode(Delegator delegator, String productId) {
        GenericValue existingBarcode = null;
        try {
            existingBarcode = EntityQuery.use(delegator).from("GoodIdentification").where("goodIdentificationTypeId", "BARCODE", "productId", productId).cache(true).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return existingBarcode;
    }

    public static GenericValue getProductListPriceRecord(Delegator delegator, String productId) {
        GenericValue productListPrice = null;
        try {
            productListPrice = EntityQuery.use(delegator).from("ProductPrice").where("productId", productId, "productPriceTypeId", "LIST_PRICE").cache().queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return productListPrice;
    }

    public static GenericValue getProductCompetitivePriceRecord(Delegator delegator, String productId) {
        GenericValue productCompetitivePrice = null;
        try {
            productCompetitivePrice = EntityQuery.use(delegator).from("ProductPrice").where("productId", productId, "productPriceTypeId", "COMPETITIVE_PRICE").cache().queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return productCompetitivePrice;
    }

    public static BigDecimal getProductCompetitivePrice(Delegator delegator, String productId) {
        BigDecimal competitivePrice = null;
        GenericValue productCompetitivePrice = null;
        try {
            productCompetitivePrice = EntityQuery.use(delegator).from("ProductPrice").where("productId", productId, "productPriceTypeId", "COMPETITIVE_PRICE").cache().queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        if (UtilValidate.isNotEmpty(productCompetitivePrice)) {
            competitivePrice = productCompetitivePrice.getBigDecimal("price");
        }
        return competitivePrice;
    }

    public static BigDecimal getProductListPrice(Delegator delegator, String productId) {
        BigDecimal listPrice = null;
        GenericValue productListPrice = null;
        try {
            productListPrice = EntityQuery.use(delegator).from("ProductPrice").where("productId", productId, "productPriceTypeId", "LIST_PRICE").cache().queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        if (UtilValidate.isNotEmpty(productListPrice)) {
            listPrice = productListPrice.getBigDecimal("price");
        }
        return listPrice;
    }

    public static String getProductConfigProductId(Delegator delegator, String configItemId) {
        String productId = null;
        GenericValue productConfigRecord = null;
        try {
            productConfigRecord = EntityQuery.use(delegator).from("ProductConfig").where("configItemId", configItemId).cache().queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        if (UtilValidate.isNotEmpty(productConfigRecord)) {
            productId = productConfigRecord.getString("productId");
        }
        return productId;
    }

}
