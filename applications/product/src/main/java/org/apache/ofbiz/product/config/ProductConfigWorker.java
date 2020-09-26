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
package org.apache.ofbiz.product.config;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.cache.UtilCache;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.product.catalog.CatalogWorker;
import org.apache.ofbiz.product.config.ProductConfigWrapper.ConfigItem;
import org.apache.ofbiz.product.config.ProductConfigWrapper.ConfigOption;
import org.apache.ofbiz.product.product.ProductWorker;
import org.apache.ofbiz.product.store.ProductStoreWorker;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.webapp.website.WebSiteWorker;

/**
 * Product Config Worker class to reduce code in templates.
 */
public final class ProductConfigWorker {

    private static final String MODULE = ProductConfigWorker.class.getName();
    private static final String SEPARATOR = "::";    // cache key separator

    private ProductConfigWorker() { }

    private static final UtilCache<String, ProductConfigWrapper> PRODUCT_CONFIG_CACHE = UtilCache.createUtilCache("product.config", true);
    // use soft reference to free up memory if needed

    public static ProductConfigWrapper getProductConfigWrapper(String productId, String currencyUomId, HttpServletRequest request) {
        ProductConfigWrapper configWrapper = null;
        String catalogId = CatalogWorker.getCurrentCatalogId(request);
        String webSiteId = WebSiteWorker.getWebSiteId(request);
        String productStoreId = ProductStoreWorker.getProductStoreId(request);
        GenericValue autoUserLogin = (GenericValue) request.getSession().getAttribute("autoUserLogin");
        try {
            /* caching: there is one cache created, "product.config"  Each product's config wrapper is cached with a key of
             * productId::catalogId::webSiteId::currencyUomId, or whatever the SEPARATOR is defined above to be.
             */
            Delegator delegator = (Delegator) request.getAttribute("delegator");
            String cacheKey = productId + SEPARATOR + productStoreId + SEPARATOR + catalogId + SEPARATOR + webSiteId + SEPARATOR
                    + currencyUomId + SEPARATOR + delegator;
            configWrapper = PRODUCT_CONFIG_CACHE.get(cacheKey);
            if (configWrapper == null) {
                configWrapper = new ProductConfigWrapper((Delegator) request.getAttribute("delegator"),
                                                         (LocalDispatcher) request.getAttribute("dispatcher"),
                                                         productId, productStoreId, catalogId, webSiteId,
                                                         currencyUomId, UtilHttp.getLocale(request),
                                                         autoUserLogin);
                configWrapper = PRODUCT_CONFIG_CACHE.putIfAbsentAndGet(cacheKey, new ProductConfigWrapper(configWrapper));
            } else {
                configWrapper = new ProductConfigWrapper(configWrapper);
            }
        } catch (Exception e) {
            Debug.logWarning(e.getMessage(), MODULE);
            configWrapper = null;
        }
        return configWrapper;
    }

    public static void fillProductConfigWrapper(ProductConfigWrapper configWrapper, HttpServletRequest request) {
        int numOfQuestions = configWrapper.getQuestions().size();
        Map<String, Object> combinedMap = UtilHttp.getCombinedMap(request);
        for (int k = 0; k < numOfQuestions; k++) {
            String[] opts = new String[0];
            Object o = combinedMap.get(Integer.toString(k));
            if (o instanceof String) {
                opts = new String[]{(String) o};
            } else if (o instanceof List) {
                List<?> list = (List<?>) o;
                opts = list.toArray(new String[list.size()]);
            }
            if (opts == null) {

                //  check for standard item comments
                ProductConfigWrapper.ConfigItem question = configWrapper.getQuestions().get(k);
                if (question.isStandard()) {
                    int i = 0;
                    while (i <= (question.getOptions().size() - 1)) {
                        String comments = (String) combinedMap.get("comments_" + k + "_" + i);
                        if (UtilValidate.isNotEmpty(comments)) {
                            try {
                                configWrapper.setSelected(k, i, comments);
                            } catch (Exception e) {
                                Debug.logWarning(e.getMessage(), MODULE);
                            }
                        }
                        i++;
                    }
                }
                continue;
            }
            for (String opt: opts) {
                int cnt = -1;
                try {
                    cnt = Integer.parseInt(opt);
                    String comments = null;
                    ProductConfigWrapper.ConfigItem question = configWrapper.getQuestions().get(k);
                    if (question.isSingleChoice()) {
                        comments = (String) combinedMap.get("comments_" + k + "_" + "0");
                    } else {
                        comments = (String) combinedMap.get("comments_" + k + "_" + cnt);
                    }

                    configWrapper.setSelected(k, cnt, comments);
                    ProductConfigWrapper.ConfigOption option = configWrapper.getItemOtion(k, cnt);

                    //  set selected variant products
                    if (UtilValidate.isNotEmpty(option) && (option.hasVirtualComponent())) {
                        List<GenericValue> components = option.getComponents();
                        int variantIndex = 0;
                        for (int i = 0; i < components.size(); i++) {
                            GenericValue component = components.get(i);
                            if (option.isVirtualComponent(component)) {
                                String productParamName = "add_product_id" + k + "_" + cnt + "_" + variantIndex;
                                String selectedProductId = (String) combinedMap.get(productParamName);
                                if (UtilValidate.isEmpty(selectedProductId)) {
                                    Debug.logWarning("ERROR: Request param [" + productParamName + "] not found!", MODULE);
                                } else {

                                    //  handle also feature tree virtual variant methods
                                    if (ProductWorker.isVirtual((Delegator) request.getAttribute("delegator"), selectedProductId)) {
                                        if ("VV_FEATURETREE".equals(ProductWorker.getProductVirtualVariantMethod((Delegator)
                                                request.getAttribute("delegator"), selectedProductId))) {
                                            // get the selected features
                                            List<String> selectedFeatures = new LinkedList<>();
                                            String prefix = "FT" + k + "_" + cnt + "_" + variantIndex;
                                            request.getParameterMap().forEach((name, values) -> {
                                                if (name.startsWith(prefix)) {
                                                    selectedFeatures.add(values[0]);
                                                }
                                            });

                                            // check if features are selected
                                            if (UtilValidate.isEmpty(selectedFeatures)) {
                                                Debug.logWarning("ERROR: No features selected for productId [" + selectedProductId + "]", MODULE);
                                            }

                                            String variantProductId = ProductWorker.getVariantFromFeatureTree(selectedProductId, selectedFeatures,
                                                    (Delegator) request.getAttribute("delegator"));
                                            if (UtilValidate.isNotEmpty(variantProductId)) {
                                                selectedProductId = variantProductId;
                                            } else {
                                                Debug.logWarning("ERROR: Variant product not found!", MODULE);
                                                request.setAttribute("_EVENT_MESSAGE_", UtilProperties.getMessage("OrderErrorUiLabels",
                                                        "cart.addToCart.incompatibilityVariantFeature", UtilHttp.getLocale(request)));
                                            }
                                        }
                                    }
                                    configWrapper.setSelected(k, cnt, i, selectedProductId);
                                }
                                variantIndex++;
                            }
                        }
                    }
                } catch (Exception e) {
                    Debug.logWarning(e.getMessage(), MODULE);
                }
            }
        }
    }

    /**
     * First search persisted configurations and update configWrapper.configId if found.
     * Otherwise store ProductConfigWrapper to ProductConfigConfig entity and updates configWrapper.configId with new configId
     * This method persists only the selected options, price data is lost.
     * @param configWrapper the ProductConfigWrapper object
     * @param delegator the delegator
     */
    public static void storeProductConfigWrapper(ProductConfigWrapper configWrapper, Delegator delegator) {
        if (configWrapper == null || (!configWrapper.isCompleted())) {
            return;
        }
        String configId = null;
        List<ConfigItem> questions = configWrapper.getQuestions();
        List<GenericValue> configsToCheck = new LinkedList<>();
        int selectedOptionSize = 0;
        for (ConfigItem ci: questions) {
            String configItemId = null;
            Long sequenceNum = null;
            List<ProductConfigWrapper.ConfigOption> selectedOptions = new LinkedList<>();
            List<ConfigOption> options = ci.getOptions();
            if (ci.isStandard()) {
                selectedOptions.addAll(options);
            } else {
                for (ConfigOption oneOption: options) {
                    if (oneOption.isSelected()) {
                        selectedOptions.add(oneOption);
                    }
                }
            }

            if (!selectedOptions.isEmpty()) {
                selectedOptionSize += selectedOptions.size();
                configItemId = ci.getConfigItemAssoc().getString("configItemId");
                sequenceNum = ci.getConfigItemAssoc().getLong("sequenceNum");
                try {
                    List<GenericValue> configs = EntityQuery.use(delegator).from("ProductConfigConfig").where("configItemId", configItemId,
                            "sequenceNum", sequenceNum).queryList();
                    for (GenericValue productConfigConfig: configs) {
                        for (ConfigOption oneOption: selectedOptions) {
                            String configOptionId = oneOption.getConfigOption().getString("configOptionId");
                            if (productConfigConfig.getString("configOptionId").equals(configOptionId)) {
                                String comments = oneOption.getComments() != null ? oneOption.getComments() : "";
                                if ((UtilValidate.isEmpty(comments) && UtilValidate.isEmpty(productConfigConfig.getString("description")))
                                        || comments.equals(productConfigConfig.getString("description"))) {
                                    configsToCheck.add(productConfigConfig);
                                }
                            }
                        }
                    }

                } catch (GenericEntityException e) {
                    Debug.logError(e, MODULE);
                }

            }
        }
        if (UtilValidate.isNotEmpty(configsToCheck)) {
            for (GenericValue productConfigConfig: configsToCheck) {
                String tempConfigId = productConfigConfig.getString("configId");
                try {
                    List<GenericValue> tempResult = EntityQuery.use(delegator).from("ProductConfigConfig").where("configId", tempConfigId)
                            .queryList();
                    if (tempResult.size() == selectedOptionSize && configsToCheck.containsAll(tempResult)) {
                        List<GenericValue> configOptionProductOptions = EntityQuery.use(delegator).from("ConfigOptionProductOption")
                                .where("configId", tempConfigId).queryList();
                        if (UtilValidate.isNotEmpty(configOptionProductOptions)) {

                            //  check for variant product equality
                            for (ConfigItem ci: questions) {
                                String configItemId = null;
                                Long sequenceNum = null;
                                List<ProductConfigWrapper.ConfigOption> selectedOptions = new LinkedList<>();
                                List<ConfigOption> options = ci.getOptions();
                                if (ci.isStandard()) {
                                    selectedOptions.addAll(options);
                                } else {
                                    for (ConfigOption oneOption: options) {
                                        if (oneOption.isSelected()) {
                                            selectedOptions.add(oneOption);
                                        }
                                    }
                                }

                                boolean match = true;
                                for (ProductConfigWrapper.ConfigOption anOption : selectedOptions) {
                                    if (match && anOption.hasVirtualComponent()) {
                                        List<GenericValue> components = anOption.getComponents();
                                        for (GenericValue aComponent : components) {
                                            if (anOption.isVirtualComponent(aComponent)) {
                                                Map<String, String> componentOptions = anOption.getComponentOptions();
                                                String optionProductId = aComponent.getString("productId");
                                                String optionProductOptionId = null;
                                                if (UtilValidate.isNotEmpty(componentOptions)) {
                                                    optionProductOptionId = componentOptions.get(optionProductId);
                                                }
                                                String configOptionId = anOption.getConfigOption().getString("configOptionId");
                                                configItemId = ci.getConfigItemAssoc().getString("configItemId");
                                                sequenceNum = ci.getConfigItemAssoc().getLong("sequenceNum");

                                                GenericValue configOptionProductOption = delegator.makeValue("ConfigOptionProductOption");
                                                configOptionProductOption.set("configId", tempConfigId);
                                                configOptionProductOption.set("configItemId", configItemId);
                                                configOptionProductOption.set("sequenceNum", sequenceNum);
                                                configOptionProductOption.set("configOptionId", configOptionId);
                                                configOptionProductOption.set("productId", optionProductId);
                                                configOptionProductOption.set("productOptionId", optionProductOptionId);
                                                if (!configOptionProductOptions.remove(configOptionProductOption)) {
                                                    match = false;
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }

                                if (match && (UtilValidate.isEmpty(configOptionProductOptions))) {
                                    configWrapper.setConfigId(tempConfigId);
                                    Debug.logInfo("Existing configuration found with configId:" + tempConfigId, MODULE);
                                    return;
                                }
                            }

                        } else {
                            configWrapper.setConfigId(tempConfigId);
                            Debug.logInfo("Existing configuration found with configId:" + tempConfigId, MODULE);
                            return;
                        }
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, MODULE);
                }

            }
        }

        //Current configuration is not found in ProductConfigConfig entity. So lets store this one
        boolean nextId = true;
        for (ConfigItem ci: questions) {
            String configItemId = null;
            Long sequenceNum = null;
            List<ProductConfigWrapper.ConfigOption> selectedOptions = new LinkedList<>();
            List<ConfigOption> options = ci.getOptions();
            if (ci.isStandard()) {
                selectedOptions.addAll(options);
            } else {
                for (ConfigOption oneOption: options) {
                    if (oneOption.isSelected()) {
                        selectedOptions.add(oneOption);
                    }
                }
            }

            if (!selectedOptions.isEmpty()) {
                if (nextId) {
                    configId = delegator.getNextSeqId("ProductConfigConfig");
                    //get next configId only once and only if there are selectedOptions
                    nextId = false;
                }
                configItemId = ci.getConfigItemAssoc().getString("configItemId");
                sequenceNum = ci.getConfigItemAssoc().getLong("sequenceNum");
                for (ConfigOption oneOption: selectedOptions) {
                    Map<String, String> componentOptions = oneOption.getComponentOptions();
                    List<GenericValue> toBeStored = new LinkedList<>();
                    String configOptionId = oneOption.getConfigOption().getString("configOptionId");
                    String description = oneOption.getComments();
                    GenericValue productConfigConfig = delegator.makeValue("ProductConfigConfig");
                    productConfigConfig.put("configId", configId);
                    productConfigConfig.put("configItemId", configItemId);
                    productConfigConfig.put("sequenceNum", sequenceNum);
                    productConfigConfig.put("configOptionId", configOptionId);
                    productConfigConfig.put("description", description);
                    toBeStored.add(productConfigConfig);

                    if (oneOption.hasVirtualComponent()) {
                        List<GenericValue> components = oneOption.getComponents();
                        for (GenericValue component: components) {
                            if (oneOption.isVirtualComponent(component) && UtilValidate.isNotEmpty(componentOptions)) {
                                String componentOption = componentOptions.get(component.getString("productId"));
                                GenericValue configOptionProductOption = delegator.makeValue("ConfigOptionProductOption");
                                configOptionProductOption.put("configId", configId);
                                configOptionProductOption.put("configItemId", configItemId);
                                configOptionProductOption.put("sequenceNum", sequenceNum);
                                configOptionProductOption.put("configOptionId", configOptionId);
                                configOptionProductOption.put("productId", component.getString("productId"));
                                configOptionProductOption.put("productOptionId", componentOption);
                                toBeStored.add(configOptionProductOption);
                            }
                        }
                    }
                    try {
                        delegator.storeAll(toBeStored);
                    } catch (GenericEntityException e) {
                        configId = null;
                        Debug.logWarning(e.getMessage(), MODULE);
                    }
                }
            }
        }

        //save  configId to configWrapper, so we can use it in shopping cart operations
        configWrapper.setConfigId(configId);
        Debug.logInfo("New configId created:" + configId, MODULE);
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
    public static ProductConfigWrapper loadProductConfigWrapper(Delegator delegator, LocalDispatcher dispatcher, String configId, String productId,
            String productStoreId, String catalogId, String webSiteId, String currencyUomId, Locale locale, GenericValue autoUserLogin) {
        ProductConfigWrapper configWrapper = null;
        try {
            configWrapper = new ProductConfigWrapper(delegator, dispatcher, productId, productStoreId, catalogId, webSiteId, currencyUomId,
                    locale, autoUserLogin);
            if (UtilValidate.isNotEmpty(configId)) {
                configWrapper.loadConfig(delegator, configId);
            }
        } catch (Exception e) {
            Debug.logWarning(e.getMessage(), MODULE);
            configWrapper = null;
        }
        return configWrapper;
    }

}

