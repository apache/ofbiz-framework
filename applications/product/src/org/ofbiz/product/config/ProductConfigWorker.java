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
package org.ofbiz.product.config;

import javax.servlet.http.HttpServletRequest;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.product.catalog.CatalogWorker;
import org.ofbiz.product.store.ProductStoreWorker;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.base.util.cache.UtilCache;

/**
 * Product Config Worker class to reduce code in templates.
 */
public class ProductConfigWorker {
    
    public static final String module = ProductConfigWorker.class.getName();
    public static final String resource = "ProductUiLabels";
    public static final String SEPARATOR = "::";    // cache key separator

    public static UtilCache productConfigCache = new UtilCache("product.config", true);     // use soft reference to free up memory if needed

    public static ProductConfigWrapper getProductConfigWrapper(String productId, String currencyUomId, HttpServletRequest request) {
        ProductConfigWrapper configWrapper = null;
        String catalogId = CatalogWorker.getCurrentCatalogId(request);
        String webSiteId = CatalogWorker.getWebSiteId(request);
        String productStoreId = ProductStoreWorker.getProductStoreId(request);
        GenericValue autoUserLogin = (GenericValue)request.getSession().getAttribute("autoUserLogin");
        try {
            /* caching: there is one cache created, "product.config"  Each product's config wrapper is cached with a key of
             * productId::catalogId::webSiteId::currencyUomId, or whatever the SEPARATOR is defined above to be.
             */
            String cacheKey = productId + SEPARATOR + productStoreId + SEPARATOR + catalogId + SEPARATOR + webSiteId + SEPARATOR + currencyUomId;
            if (!productConfigCache.containsKey(cacheKey)) {
                configWrapper = new ProductConfigWrapper((GenericDelegator)request.getAttribute("delegator"),
                                                         (LocalDispatcher)request.getAttribute("dispatcher"),
                                                         productId, productStoreId, catalogId, webSiteId,
                                                         currencyUomId, UtilHttp.getLocale(request),
                                                         autoUserLogin);
                productConfigCache.put(cacheKey, new ProductConfigWrapper(configWrapper));
            } else {
                configWrapper = new ProductConfigWrapper((ProductConfigWrapper)productConfigCache.get(cacheKey));
            }
        } catch(ProductConfigWrapperException we) {
            configWrapper = null;
        } catch(Exception e) {
            Debug.logWarning(e.getMessage(), module);
        }
        return configWrapper;
    }
    
    public static void fillProductConfigWrapper(ProductConfigWrapper configWrapper, HttpServletRequest request) {
        int numOfQuestions = configWrapper.getQuestions().size();
        for (int k = 0; k < numOfQuestions; k++) {
            String[] opts = request.getParameterValues("" + k);
            if (opts == null) {
                continue;
            }
            for (int h = 0; h < opts.length; h++) {
                int cnt = -1;
                try {
                    cnt = Integer.parseInt(opts[h]);
                    configWrapper.setSelected(k, cnt);
                } catch(Exception e) {
                    Debug.logWarning(e.getMessage(), module);
                }
            }
        }
    }
}

