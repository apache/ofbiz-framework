package com.simbaquartz.xapi.helper;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.cache.UtilCache;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.product.config.ProductConfigWrapper;
import org.apache.ofbiz.product.config.ProductConfigWrapperException;
import org.apache.ofbiz.service.LocalDispatcher;

import java.util.Locale;

/**
 * Created by Mandeep on 8/19/2017.
 */
public class AxProductConfigHelper {
    public static final String module = AxProductConfigHelper.class.getName();
    public static final String SEPARATOR = "::";    // cache key separator

    private static final UtilCache<String, ProductConfigWrapper> productConfigCache = UtilCache.createUtilCache("product.config", true);     // use soft reference to free up memory if needed

    public static ProductConfigWrapper getProductConfigWrapper(Delegator delegator, LocalDispatcher dispatcher,
                                                               Locale locale, String productStoreId, String productId,
                                                               String currencyUomId, String catalogId, String webSiteId,
                                                               GenericValue userLogin) {
        ProductConfigWrapper configWrapper = null;
        try {
            /* caching: there is one cache created, "product.config"  Each product's config wrapper is cached with a key of
             * productId::catalogId::webSiteId::currencyUomId, or whatever the SEPARATOR is defined above to be.
             */
            String cacheKey = productId + SEPARATOR + productStoreId + SEPARATOR + catalogId + SEPARATOR + webSiteId + SEPARATOR + currencyUomId + SEPARATOR + delegator;
            configWrapper = productConfigCache.get(cacheKey);
            if (configWrapper == null) {
                configWrapper = new ProductConfigWrapper(delegator,
                        dispatcher,
                        productId, productStoreId, catalogId, webSiteId,
                        currencyUomId, locale,
                        userLogin);
                configWrapper = productConfigCache.putIfAbsentAndGet(cacheKey, new ProductConfigWrapper(configWrapper));
            } else {
                configWrapper = new ProductConfigWrapper(configWrapper);
            }
        } catch (ProductConfigWrapperException we) {
            configWrapper = null;
        } catch (Exception e) {
            Debug.logWarning(e.getMessage(), module);
        }
        return configWrapper;
    }

    public static GenericValue getProductConfigProductRecord(Delegator delegator, String configItemId, String configOptionId) {
        GenericValue productConfigProductRecord = null;
        try {
            productConfigProductRecord = EntityQuery.use(delegator).from("ProductConfigProduct").where("configItemId", configItemId, "configOptionId", configOptionId).cache().queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return productConfigProductRecord;
    }
}
