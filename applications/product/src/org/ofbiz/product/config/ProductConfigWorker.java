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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.tagext.TryCatchFinally;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.product.catalog.CatalogWorker;
import org.ofbiz.product.config.ProductConfigWrapper.ConfigItem;
import org.ofbiz.product.config.ProductConfigWrapper.ConfigOption;
import org.ofbiz.product.store.ProductStoreWorker;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
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
    
    /**
     * First search persisted configurations and update configWrapper.configId if found.
     * Otherwise store ProductConfigWrapper to ProductConfigConfig entity and updates configWrapper.configId with new configId
     * This method persists only the selected options, price data is lost.
     * @param ProductConfigWrapper
     * @param delegator
     */    
    public static void storeProductConfigWrapper(ProductConfigWrapper configWrapper, GenericDelegator delegator) {
        if (configWrapper == null || (!configWrapper.isCompleted()))  return;
        String configId = null;
        List questions = configWrapper.getQuestions();
        List configsToCheck = new LinkedList();
        int selectedOptionSize = 0;
        for (int i = 0; i < questions.size(); i++) {
            String configItemId = null;
            Long sequenceNum = null;
            List <ProductConfigWrapper.ConfigOption> selectedOptions = new ArrayList <ProductConfigWrapper.ConfigOption>();        
            ConfigItem ci = (ConfigItem)questions.get(i);
            List options = ci.getOptions();
            if (ci.isStandard()) {
                selectedOptions.addAll(options);
            } else {
                Iterator availOptions = options.iterator();
                while (availOptions.hasNext()) {
                    ConfigOption oneOption = (ConfigOption)availOptions.next();
                    if (oneOption.isSelected()) {
                        selectedOptions.add(oneOption);
                    }
                }
            }

            if (selectedOptions.size() > 0) {
                selectedOptionSize += selectedOptions.size();
                configItemId = ci.getConfigItemAssoc().getString("configItemId");
                sequenceNum = ci.getConfigItemAssoc().getLong("sequenceNum");
                try {
                    List <GenericValue> configs = delegator.findByAnd("ProductConfigConfig", UtilMisc.toMap("configItemId",configItemId,"sequenceNum", sequenceNum));
                    Iterator <GenericValue> configIt = configs.iterator(); 
                    while (configIt.hasNext()) {
                        GenericValue productConfigConfig = configIt.next();
                        Iterator selOpIt = selectedOptions.iterator();
                        while (selOpIt.hasNext()) {
                            ConfigOption oneOption = (ConfigOption)selOpIt.next();
                            String configOptionId = oneOption.configOption.getString("configOptionId");
                            if (productConfigConfig.getString("configOptionId").equals(configOptionId)) {
                                configsToCheck.add(productConfigConfig);
                            }
                        } 
                    }

                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                }
                           
            }  
        }
        if (UtilValidate.isNotEmpty(configsToCheck)) {
            Iterator <GenericValue> ctci = configsToCheck.iterator();
            while (ctci.hasNext()) {
                GenericValue productConfigConfig =  ctci.next();
                String tempConfigId = productConfigConfig.getString("configId");
                try {
                    List tempResult = delegator.findByAnd("ProductConfigConfig", UtilMisc.toMap("configId",tempConfigId));
                    if (tempResult.size() == selectedOptionSize && configsToCheck.containsAll(tempResult)) {
                            configWrapper.configId = tempConfigId;
                            Debug.logInfo("Existing configuration found with configId:"+ tempConfigId,  module);
                            return;
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                }
                
            }
        }
        
        //Current configuration is not found in ProductConfigConfig entity. So lets store this one
        boolean nextId = true;
        for (int i = 0; i < questions.size(); i++) {
            String configItemId = null;
            Long sequenceNum = null;
            List <ProductConfigWrapper.ConfigOption> selectedOptions = new ArrayList <ProductConfigWrapper.ConfigOption>();        
            ConfigItem ci = (ConfigItem)questions.get(i);
            List options = ci.getOptions();
           if (ci.isStandard()) {
                selectedOptions.addAll(options);
            } else {
                Iterator availOptions = options.iterator();
                while (availOptions.hasNext()) {
                    ConfigOption oneOption = (ConfigOption)availOptions.next();
                    if (oneOption.isSelected()) {
                        selectedOptions.add(oneOption);
                    }
                }
            }
            
            if (selectedOptions.size() > 0) {
                if (nextId) {
                    configId = delegator.getNextSeqId("ProductConfigConfig");
                    //get next configId only once and only if there are selectedOptions
                    nextId = false;
                }
                configItemId = ci.getConfigItemAssoc().getString("configItemId");
                sequenceNum = ci.getConfigItemAssoc().getLong("sequenceNum");
                Iterator selOpIt = selectedOptions.iterator();
                while (selOpIt.hasNext()) {
                    ConfigOption oneOption = (ConfigOption)selOpIt.next();
                    String configOptionId = oneOption.configOption.getString("configOptionId");
                    GenericValue productConfigConfig = delegator.makeValue("ProductConfigConfig");
                    productConfigConfig.put("configId", configId);
                    productConfigConfig.put("configItemId", configItemId);
                    productConfigConfig.put("sequenceNum", sequenceNum);
                    productConfigConfig.put("configOptionId", configOptionId);
                    try {
                        productConfigConfig.create();
                    } catch (GenericEntityException e) {
                        configId = null;
                        Debug.logWarning(e.getMessage(), module);
                    }
                }                            
            }  
        }
        
        //save  configId to configWrapper, so we can use it in shopping cart operations
        configWrapper.configId = configId;
        Debug.logInfo("New configId created:"+ configId,  module);
        return;
    }
    
    /**
     * Creates a new ProductConfigWrapper for productId and configures it according to ProductConfigConfig entity with configId
     * ProductConfigConfig entity stores only the selected options, and the product price is calculated from input params
     * @param delegator
     * @param dispatcher
     * @param configId configuration Id
     * @param productId AGGRAGATED productId
     * @param productStoreId needed for price calculations
     * @param catalogId needed for price calculations
     * @param webSiteId needed for price calculations
     * @param currencyUomId needed for price calculations
     * @param locale
     * @param autoUserLogin
     * @return ProductConfigWrapper
     */
    public static ProductConfigWrapper loadProductConfigWrapper(GenericDelegator delegator, LocalDispatcher dispatcher, String configId, String productId, String productStoreId, String catalogId, String webSiteId, String currencyUomId, Locale locale, GenericValue autoUserLogin) {
        ProductConfigWrapper configWrapper = null;
        try {
             configWrapper = new ProductConfigWrapper(delegator, dispatcher, productId, productStoreId, catalogId, webSiteId, currencyUomId, locale, autoUserLogin);
            if (configWrapper != null && UtilValidate.isNotEmpty(configId)) {
                configWrapper.loadConfig(delegator, configId);
            }
        } catch (Exception e) {
            Debug.logWarning(e.getMessage(), module);
            configWrapper = null;
        }
        return configWrapper;
    }

}

