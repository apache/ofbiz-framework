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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.LocalDispatcher;


/**
 * Product Config Wrapper: gets product config to display
 */

public class ProductConfigWrapper implements Serializable {
    
    public static final String module = ProductConfigWrapper.class.getName();

    protected LocalDispatcher dispatcher;
    protected GenericValue product = null; // the aggregated product
    protected double basePrice = 0.0;
    protected List questions = null; // ProductConfigs
    
    /** Creates a new instance of ProductConfigWrapper */
    public ProductConfigWrapper() {
    }
    
    public ProductConfigWrapper(GenericDelegator delegator, LocalDispatcher dispatcher, String productId, String productStoreId, String catalogId, String webSiteId, String currencyUomId, Locale locale, GenericValue autoUserLogin) throws Exception {
        init(delegator, dispatcher, productId, productStoreId, catalogId, webSiteId, currencyUomId, locale, autoUserLogin);
    }

    public ProductConfigWrapper(ProductConfigWrapper pcw) {
        product = GenericValue.create(pcw.product);
        basePrice = pcw.basePrice;
        questions = new ArrayList();
        for (int i = 0; i < pcw.questions.size(); i++) {
            questions.add(new ConfigItem((ConfigItem)pcw.questions.get(i)));
        }
    }

    private void init(GenericDelegator delegator, LocalDispatcher dispatcher, String productId, String productStoreId, String catalogId, String webSiteId, String currencyUomId, Locale locale, GenericValue autoUserLogin) throws Exception {
        product = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", productId));
        if (product == null || !product.getString("productTypeId").equals("AGGREGATED")) {
            throw new ProductConfigWrapperException("Product " + productId + " is not an AGGREGATED product.");
        }
        this.dispatcher = dispatcher;
        
        // get the base price
        Map priceContext = UtilMisc.toMap("product", product, "prodCatalogId", catalogId, "webSiteId", webSiteId, "productStoreId", productStoreId,
                                      "currencyUomId", currencyUomId, "autoUserLogin", autoUserLogin);
        Map priceMap = dispatcher.runSync("calculateProductPrice", priceContext);
        Double price = (Double)priceMap.get("price");
        if (price != null) {
            basePrice = price.doubleValue();
        }
        questions = new ArrayList();
        List questionsValues = new ArrayList();
        if (product.getString("productTypeId") != null && product.getString("productTypeId").equals("AGGREGATED")) {
            questionsValues = delegator.findByAnd("ProductConfig", UtilMisc.toMap("productId", productId), UtilMisc.toList("sequenceNum"));
            questionsValues = EntityUtil.filterByDate(questionsValues);
            Iterator questionsValuesIt = questionsValues.iterator();
            HashMap itemIds = new HashMap();
            while (questionsValuesIt.hasNext()) {
                ConfigItem oneQuestion = new ConfigItem((GenericValue)questionsValuesIt.next());
                oneQuestion.setContent(locale, "text/html"); // TODO: mime-type shouldn't be hardcoded
                if (itemIds.containsKey(oneQuestion.getConfigItem().getString("configItemId"))) {
                    oneQuestion.setFirst(false);
                } else {
                    itemIds.put(oneQuestion.getConfigItem().getString("configItemId"), null);
                }
                questions.add(oneQuestion);
                List configOptions = delegator.findByAnd("ProductConfigOption", UtilMisc.toMap("configItemId", oneQuestion.getConfigItemAssoc().getString("configItemId")), UtilMisc.toList("sequenceNum"));
                Iterator configOptionsIt = configOptions.iterator();
                while (configOptionsIt.hasNext()) {
                    ConfigOption option = new ConfigOption(delegator, dispatcher, (GenericValue)configOptionsIt.next(), catalogId, webSiteId, currencyUomId, autoUserLogin);
                    oneQuestion.addOption(option);
                }
            }
        }
    }
    
    public void resetConfig() {
        for (int i = 0; i < questions.size(); i++) {
            ConfigItem ci = (ConfigItem)questions.get(i);
            if (!ci.isStandard()) {
                List options = ci.getOptions();
                for (int j = 0; j < options.size(); j++) {
                    ConfigOption co = (ConfigOption)options.get(j);
                    co.setSelected(false);
                }
            }
        }
    }
    
    public void setDefaultConfig() {
        resetConfig();
        for (int i = 0; i < questions.size(); i++) {
            ConfigItem ci = (ConfigItem)questions.get(i);
            if (ci.isMandatory()) {
                if (ci.getOptions().size() > 0) {
                    ConfigOption co = (ConfigOption)ci.getOptions().get(0);
                    co.setSelected(true);
                }
            }
        }
    }
    
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof ProductConfigWrapper)) {
            return false;
        }
        ProductConfigWrapper cw = (ProductConfigWrapper)obj;
        if (!product.getString("productId").equals(cw.getProduct().getString("productId"))) {
            return false;
        }
        List cwq = cw.getQuestions();
        if (questions.size() != cwq.size()) {
            return false;
        }
        for (int i = 0; i < questions.size(); i++) {
            ConfigItem ci = (ConfigItem)questions.get(i);
            if (!ci.equals(cwq.get(i))) {
                return false;
            }
        }
        return true;
    }

    public String toString() {
        return "" + questions;
    }

    public List getQuestions() {
        return questions;
    }
    
    public GenericValue getProduct() {
        return product;
    }
    
    public void setSelected(int question, int option) throws Exception {
        ConfigItem ci = (ConfigItem)questions.get(question);
        List avalOptions = ci.getOptions();
        if (ci.isSingleChoice()) {
            for (int j = 0; j < avalOptions.size(); j++) {
                ConfigOption oneOption = (ConfigOption)avalOptions.get(j);
                oneOption.setSelected(false);
            }
        }
        ConfigOption theOption = null;
        if (option >= 0 && option < avalOptions.size()) {
            theOption = (ConfigOption)avalOptions.get(option);
        }
        if (theOption != null) {
            theOption.setSelected(true);
        }
    }
    
    public List getSelectedOptions() {
        List selectedOptions = new ArrayList();
        for (int i = 0; i < questions.size(); i++) {
            ConfigItem ci = (ConfigItem)questions.get(i);
            if (ci.isStandard()) {
                selectedOptions.addAll(ci.getOptions());
            } else {
                Iterator availOptions = ci.getOptions().iterator();
                while (availOptions.hasNext()) {
                    ConfigOption oneOption = (ConfigOption)availOptions.next();
                    if (oneOption.isSelected()) {
                        selectedOptions.add(oneOption);
                    }
                }
            }
        }
        return selectedOptions;
    }
    
    public double getTotalPrice() {
        double totalPrice = basePrice;
        List options = getSelectedOptions();
        for (int i = 0; i < options.size(); i++) {
            ConfigOption oneOption = (ConfigOption)options.get(i);
            totalPrice += oneOption.getPrice();
        }
        return totalPrice;
    }
    
    public boolean isCompleted() {
        boolean completed = true;
        for (int i = 0; i < questions.size(); i++) {
            ConfigItem ci = (ConfigItem)questions.get(i);
            if (!ci.isStandard() && ci.isMandatory()) {
                Iterator availOptions = ci.getOptions().iterator();
                while (availOptions.hasNext()) {
                    ConfigOption oneOption = (ConfigOption)availOptions.next();
                    if (oneOption.isSelected()) {
                        completed = true;
                        break;
                    } else {
                        completed = false;
                    }
                }
                if (!completed) {
                    break;
                }
            }
        }
        return completed;
    }
    
    public class ConfigItem implements java.io.Serializable {
        GenericValue configItem = null;
        GenericValue configItemAssoc = null;
        ProductConfigItemContentWrapper content = null;
        List options = null;
        boolean first = true;
        
        public ConfigItem(GenericValue questionAssoc) throws Exception {
            configItemAssoc = questionAssoc;
            configItem = configItemAssoc.getRelatedOne("ConfigItemProductConfigItem");
            options = new ArrayList();
        }
        
        public ConfigItem(ConfigItem ci) {
            configItem = GenericValue.create(ci.configItem);
            configItemAssoc = GenericValue.create(ci.configItemAssoc);
            options = new ArrayList();
            for (int i = 0; i < ci.options.size(); i++) {
                options.add(new ConfigOption((ConfigOption)ci.options.get(i)));
            }
            first = ci.first;
            content = ci.content; // FIXME: this should be cloned
        }

        public void setContent(Locale locale, String mimeTypeId) {
            content = new ProductConfigItemContentWrapper(dispatcher, configItem, locale, mimeTypeId);
        }
        
        public ProductConfigItemContentWrapper getContent() {
            return content;
        }
        
        public GenericValue getConfigItem() {
            return configItem;
        }
        
        public GenericValue getConfigItemAssoc() {
            return configItemAssoc;
        }

        public boolean isStandard() {
            return configItemAssoc.getString("configTypeId").equals("STANDARD");
        }
        
        public boolean isSingleChoice() {
            return configItem.getString("configItemTypeId").equals("SINGLE");
        }
        
        public boolean isMandatory() {
            return configItemAssoc.getString("isMandatory") != null && configItemAssoc.getString("isMandatory").equals("Y");
        }
        
        public boolean isFirst() {
            return first;
        }
        
        public void setFirst(boolean newValue) {
            first = newValue;
        }
        
        public void addOption(ConfigOption option) {
            options.add(option);
        }
        
        public List getOptions() {
            return options;
        }
        
        public String getQuestion() {
            String question = "";
            if (UtilValidate.isNotEmpty(configItemAssoc.getString("description"))) {
                question = configItemAssoc.getString("description");
            } else {
                if (content != null) {
                    question = content.get("DESCRIPTION");
                } else {
                    question = (configItem.getString("description") != null? configItem.getString("description"): "");
                }
            }
            return question;
        }

        public String getDescription() {
            String description = "";
            if (UtilValidate.isNotEmpty(configItemAssoc.getString("longDescription"))) {
                description = configItemAssoc.getString("longDescription");
            } else {
                if (content != null) {
                    description = content.get("LONG_DESCRIPTION");
                } else {
                    description = (configItem.getString("longDescription") != null? configItem.getString("longDescription"): "");
                }
            }
            return description;
        }

        public boolean isSelected() {
            if (isStandard()) return true;
            Iterator availOptions = getOptions().iterator();
            while (availOptions.hasNext()) {
                ConfigOption oneOption = (ConfigOption)availOptions.next();
                if (oneOption.isSelected()) {
                    return true;
                }
            }
            return false;
        }
        
        public ConfigOption getSelected() {
            Iterator availOptions = getOptions().iterator();
            while (availOptions.hasNext()) {
                ConfigOption oneOption = (ConfigOption)availOptions.next();
                if (oneOption.isSelected()) {
                    return oneOption;
                }
            }
            return null;
        }
        
        public boolean equals(Object obj) {
            if (obj == null || !(obj instanceof ConfigItem)) {
                return false;
            }
            ConfigItem ci = (ConfigItem)obj;
            if (!configItem.getString("configItemId").equals(ci.getConfigItem().getString("configItemId"))) {
                return false;
            }
            List opts = ci.getOptions();
            if (options.size() != opts.size()) {
                return false;
            }
            for (int i = 0; i < options.size(); i++) {
                ConfigOption co = (ConfigOption)options.get(i);
                if (!co.equals(opts.get(i))) {
                    return false;
                }
            }
            return true;
        }

        public String toString() {
            return configItem.getString("configItemId");
        }
    }
    
    public class ConfigOption implements java.io.Serializable {
        double optionPrice = 0;
        Date availabilityDate = null;
        List componentList = null; // lists of ProductConfigProduct
        GenericValue configOption = null;
        boolean selected = false;
        boolean available = true;
        
        public ConfigOption(GenericDelegator delegator, LocalDispatcher dispatcher, GenericValue option, String catalogId, String webSiteId, String currencyUomId, GenericValue autoUserLogin) throws Exception {
            configOption = option;
            componentList = option.getRelated("ConfigOptionProductConfigProduct");
            Iterator componentsIt = componentList.iterator();
            while (componentsIt.hasNext()) {
                double price = 0;
                GenericValue oneComponent = (GenericValue)componentsIt.next();
                // Get the component's price
                Map fieldMap = UtilMisc.toMap("product", oneComponent.getRelatedOne("ProductProduct"), "prodCatalogId", catalogId, "webSiteId", webSiteId,
                        "currencyUomId", currencyUomId, "productPricePurposeId", "COMPONENT_PRICE", "autoUserLogin", autoUserLogin);
                Map priceMap = dispatcher.runSync("calculateProductPrice", fieldMap);
                Double componentPrice = (Double) priceMap.get("price");
                double mult = 1;
                if (oneComponent.getDouble("quantity") != null) {
                    mult = oneComponent.getDouble("quantity").doubleValue();
                }
                if (mult == 0) {
                    mult = 1;
                }
                if (componentPrice != null) {
                    price = componentPrice.doubleValue();
                } else {
                    fieldMap.put("productPricePurposeId", "PURCHASE");
                    Map purchasePriceResultMap = dispatcher.runSync("calculateProductPrice", fieldMap);
                    Double purchasePrice = (Double) purchasePriceResultMap.get("price");
                    if (purchasePrice != null) {
                        price = purchasePrice.doubleValue();
                    }
                }
                optionPrice += (price * mult);
                // TODO: get the component's availability date
            }
        }
        
        public ConfigOption(ConfigOption co) {
            configOption = GenericValue.create(co.configOption);
            componentList = new ArrayList();
            for (int i = 0; i < co.componentList.size(); i++) {
                componentList.add(GenericValue.create((GenericValue)co.componentList.get(i)));
            }
            optionPrice = co.optionPrice;
            available = co.available;
            selected = co.selected;
        }

        public String getDescription() {
            return (configOption.getString("description") != null? configOption.getString("description"): "no description");
        }
        
        public double getPrice() {
            return optionPrice;
        }
        
        public boolean isSelected() {
            return selected;
        }
        
        public void setSelected(boolean newValue) {
            selected = newValue;
        }
        
        public boolean isAvailable() {
            return available;
        }
        
        public void setAvailable(boolean newValue) {
            available = newValue;
        }

        public List getComponents() {
            return componentList;
        }
        
        public boolean equals(Object obj) {
            if (obj == null || !(obj instanceof ConfigOption)) {
                return false;
            }
            ConfigOption co = (ConfigOption)obj;
            // TODO: we should compare also the GenericValues
            
            return isSelected() == co.isSelected();
        }
        
        public String toString() {
            return configOption.getString("configItemId") + "/" + configOption.getString("configOptionId") + (isSelected()? "*": "");
        }

    }
    
}
