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

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceContainer;

/**
 * Product Config Wrapper: gets product config to display
 */
@SuppressWarnings("serial")
public class ProductConfigWrapper implements Serializable {

    public static final String module = ProductConfigWrapper.class.getName();

    protected transient LocalDispatcher dispatcher;
    protected String dispatcherName;
    protected String productStoreId;
    protected String catalogId;
    protected String webSiteId;
    protected String currencyUomId;
    protected transient Delegator delegator;
    protected String delegatorName = null;
    protected GenericValue product = null; // the aggregated product
    protected GenericValue autoUserLogin = null;
    protected BigDecimal listPrice = BigDecimal.ZERO;
    protected BigDecimal basePrice = BigDecimal.ZERO;
    protected BigDecimal defaultPrice = BigDecimal.ZERO;
    protected String configId = null; // Id of persisted ProductConfigWrapper
    protected List<ConfigItem> questions = null; // ProductConfigs

    /** Creates a new instance of ProductConfigWrapper */
    public ProductConfigWrapper() {
    }

    public ProductConfigWrapper(Delegator delegator, LocalDispatcher dispatcher, String productId, String productStoreId, String catalogId, String webSiteId, String currencyUomId, Locale locale, GenericValue autoUserLogin) throws Exception {
        init(delegator, dispatcher, productId, productStoreId, catalogId, webSiteId, currencyUomId, locale, autoUserLogin);
    }

    public ProductConfigWrapper(ProductConfigWrapper pcw) {
        product = GenericValue.create(pcw.product);
        listPrice = pcw.listPrice;
        basePrice = pcw.basePrice;
        defaultPrice = pcw.defaultPrice;
        questions = new LinkedList<>();
        delegator = pcw.getDelegator();
        delegatorName = delegator.getDelegatorName();
        dispatcher = pcw.getDispatcher();
        dispatcherName = dispatcher.getName();
        productStoreId = pcw.productStoreId;
        catalogId = pcw.catalogId;
        webSiteId = pcw.webSiteId;
        currencyUomId = pcw.currencyUomId;
        autoUserLogin = pcw.autoUserLogin;
        for (ConfigItem ci: pcw.questions) {
            questions.add(new ConfigItem(ci));
        }
    }

    private void init(Delegator delegator, LocalDispatcher dispatcher, String productId, String productStoreId, String catalogId, String webSiteId, String currencyUomId, Locale locale, GenericValue autoUserLogin) throws Exception {
        product = EntityQuery.use(delegator).from("Product").where("productId", productId).queryOne();
        if (product == null || !"AGGREGATED".equals(product.getString("productTypeId")) && !"AGGREGATED_SERVICE".equals(product.getString("productTypeId"))) {
            throw new ProductConfigWrapperException("Product " + productId + " is not an AGGREGATED product.");
        }
        this.dispatcher = dispatcher;
        this.dispatcherName = dispatcher.getName();
        this.productStoreId = productStoreId;
        this.catalogId = catalogId;
        this.webSiteId = webSiteId;
        this.currencyUomId = currencyUomId;
        this.delegator = delegator;
        this.delegatorName = delegator.getDelegatorName();
        this.autoUserLogin = autoUserLogin;

        // get the list Price, the base Price
        Map<String, Object> priceContext = UtilMisc.toMap("product", product, "prodCatalogId", catalogId, "webSiteId", webSiteId, "productStoreId", productStoreId,
                                      "currencyUomId", currencyUomId, "autoUserLogin", autoUserLogin);
        Map<String, Object> priceMap = dispatcher.runSync("calculateProductPrice", priceContext);
        BigDecimal originalListPrice = (BigDecimal) priceMap.get("listPrice");
        BigDecimal price = (BigDecimal) priceMap.get("price");
        if (originalListPrice != null) {
            listPrice = originalListPrice;
        }
        if (price != null) {
            basePrice = price;
        }
        questions = new LinkedList<>();
        if ("AGGREGATED".equals(product.getString("productTypeId")) || "AGGREGATED_SERVICE".equals(product.getString("productTypeId"))) {
            List<GenericValue> questionsValues = EntityQuery.use(delegator).from("ProductConfig").where("productId", productId).orderBy("sequenceNum").filterByDate().queryList();
            Set<String> itemIds = new HashSet<>();
            for (GenericValue questionsValue: questionsValues) {
                ConfigItem oneQuestion = new ConfigItem(questionsValue);
                oneQuestion.setContent(locale, "text/html"); // TODO: mime-type shouldn't be hardcoded
                if (itemIds.contains(oneQuestion.getConfigItem().getString("configItemId"))) {
                    oneQuestion.setFirst(false);
                } else {
                    itemIds.add(oneQuestion.getConfigItem().getString("configItemId"));
                }
                questions.add(oneQuestion);
                List<GenericValue> configOptions = EntityQuery.use(delegator).from("ProductConfigOption").where("configItemId", oneQuestion.getConfigItemAssoc().getString("configItemId")).orderBy("sequenceNum").queryList();
                for (GenericValue configOption: configOptions) {
                    ConfigOption option = new ConfigOption(delegator, dispatcher, configOption, oneQuestion, catalogId, webSiteId, currencyUomId, autoUserLogin);
                    oneQuestion.addOption(option);
                }
            }
            this.setDefaultPrice();
        }
    }

    public void loadConfig(Delegator delegator, String configId) throws Exception {
        //configure ProductConfigWrapper according to ProductConfigConfig entity
        if (UtilValidate.isNotEmpty(configId)) {
            this.configId = configId;
            List<GenericValue> productConfigConfig = EntityQuery.use(delegator).from("ProductConfigConfig").where("configId", configId).queryList();
            if (UtilValidate.isNotEmpty(productConfigConfig)) {
                for (GenericValue pcc: productConfigConfig) {
                    String configItemId = pcc.getString("configItemId");
                    String configOptionId = pcc.getString("configOptionId");
                    Long sequenceNum = pcc.getLong("sequenceNum");
                    String comments = pcc.getString("description");
                    this.setSelected(configItemId, sequenceNum, configOptionId, comments);
                }
            }
        }
    }

    public void setSelected(String configItemId, Long sequenceNum, String configOptionId, String comments) throws Exception {
        for (int i = 0; i < questions.size(); i++) {
            ConfigItem ci = questions.get(i);
            if (ci.configItemAssoc.getString("configItemId").equals(configItemId) && ci.configItemAssoc.getLong("sequenceNum").equals(sequenceNum)) {
                List<ConfigOption> avalOptions = ci.getOptions();
                for (int j = 0; j < avalOptions.size(); j++) {
                    ConfigOption oneOption = avalOptions.get(j);
                    if (oneOption.configOption.getString("configOptionId").equals(configOptionId)) {
                        setSelected(i, j, comments);
                        break;
                    }
                }
            }
        }
    }

    public void resetConfig() {
        for (ConfigItem ci: questions) {
            if (!ci.isStandard()) {
                List<ConfigOption> options = ci.getOptions();
                for (ConfigOption co: options) {
                    co.setSelected(false);
                    co.setComments(null);
                }
            }
        }
    }

    public void setDefaultConfig() {
        resetConfig();
        for (ConfigItem ci: questions) {
            if (ci.isMandatory()) {
                ConfigOption co = ci.getDefault();
                if (co != null) {
                    co.setSelected(true);
                } else if (ci.getOptions().size() > 0) {
                    co = ci.getOptions().get(0);
                    co.setSelected(true);
                }
            }
        }
    }

    public String getConfigId() {
        return configId;
    }

    public Delegator getDelegator() {
        if (delegator == null) {
            delegator = DelegatorFactory.getDelegator(delegatorName);
        }
        return delegator;
    }

    public LocalDispatcher getDispatcher() {
        if (dispatcher == null) {
            dispatcher = ServiceContainer.getLocalDispatcher(dispatcherName, this.getDelegator());
        }
        return dispatcher;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((delegatorName == null) ? 0 : delegatorName.hashCode());
        result = prime * result + ((product == null) ? 0 : product.hashCode());
        result = prime * result + ((questions == null) ? 0 : questions.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ProductConfigWrapper)) {
            return false;
        }
        ProductConfigWrapper cw = (ProductConfigWrapper)obj;
        if (!product.getString("productId").equals(cw.getProduct().getString("productId"))) {
            return false;
        }
        List<ConfigItem> cwq = cw.getQuestions();
        if (questions.size() != cwq.size()) {
            return false;
        }
        for (int i = 0; i < questions.size(); i++) {
            ConfigItem ci = questions.get(i);
            if (!ci.equals(cwq.get(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return questions.toString();
    }

    public List<ConfigItem> getQuestions() {
        return questions;
    }

    public GenericValue getProduct() {
        return product;
    }

    public void setSelected(int question, int option, String comments) throws Exception {
        ConfigItem ci = questions.get(question);
        List<ConfigOption> avalOptions = ci.getOptions();
        if (ci.isSingleChoice()) {
            for (int j = 0; j < avalOptions.size(); j++) {
                ConfigOption oneOption = avalOptions.get(j);
                oneOption.setSelected(false);
                oneOption.setComments(null);
            }
        }
        ConfigOption theOption = null;
        if (option >= 0 && option < avalOptions.size()) {
            theOption = avalOptions.get(option);
        }
        if (theOption != null) {
            theOption.setSelected(true);
            theOption.setComments(comments);
        }
    }

    public void setSelected(int question, int option, int component, String componentOption) throws Exception {
        //  set variant products
        ConfigOption theOption = getItemOtion(question, option);
        List<GenericValue> components = theOption.getComponents();
        GenericValue oneComponent = components.get(component);
        if (theOption.isVirtualComponent(oneComponent)) {
            if (theOption.componentOptions == null) {
                theOption.componentOptions = new HashMap<>();
            }
            theOption.componentOptions.put(oneComponent.getString("productId"), componentOption);

            //  recalculate option price
            theOption.recalculateOptionPrice(this);
        }
    }

    public List<ConfigOption> getSelectedOptions() {
        List<ConfigOption> selectedOptions = new LinkedList<>();
        for (ConfigItem ci: questions) {
            if (ci.isStandard()) {
                selectedOptions.addAll(ci.getOptions());
            } else {
                for (ConfigOption oneOption: ci.getOptions()) {
                    if (oneOption.isSelected()) {
                        selectedOptions.add(oneOption);
                    }
                }
            }
        }
        return selectedOptions;
    }

    public List<ConfigOption> getDefaultOptions() {
        List<ConfigOption> defaultOptions = new LinkedList<>();
        for (ConfigItem ci: questions) {
            ConfigOption co = ci.getDefault();
            if (co != null) {
                defaultOptions.add(co);
            }
        }
        return defaultOptions;
    }

    public BigDecimal getTotalListPrice() {
        BigDecimal totalListPrice = listPrice;
        List<ConfigOption> options = getSelectedOptions();
        for (ConfigOption oneOption: options) {
            totalListPrice = totalListPrice.add(oneOption.getListPrice());
        }
        return totalListPrice;
    }

    public BigDecimal getTotalPrice() {
        BigDecimal totalPrice = basePrice;
        List<ConfigOption> options = getSelectedOptions();
        for (ConfigOption oneOption: options) {
            totalPrice = totalPrice.add(oneOption.getPrice());
        }
        return totalPrice;
    }

    private void setDefaultPrice() {
        BigDecimal totalPrice = basePrice;
        List<ConfigOption> options = getDefaultOptions();
        for (ConfigOption oneOption: options) {
            totalPrice = totalPrice.add(oneOption.getPrice());
        }
        defaultPrice = totalPrice;
    }

    public BigDecimal getDefaultPrice() {
        return defaultPrice;
    }

    public boolean isCompleted() {
        boolean completed = true;
        for (ConfigItem ci: questions) {
            if (!ci.isStandard() && ci.isMandatory()) {
                List<ConfigOption> availOptions = ci.getOptions();
                for (ConfigOption oneOption: availOptions) {
                    if (oneOption.isSelected()) {
                        completed = true;
                        break;
                    }
                    completed = false;
                }
                if (!completed) {
                    break;
                }
            }
        }
        return completed;
    }

    public ConfigOption getItemOtion(int itemIndex, int optionIndex) {
        if (questions.size() > itemIndex) {
            ConfigItem ci = questions.get(itemIndex);
            List<ConfigOption> options = ci.getOptions();
            if (options.size() > optionIndex) {
                ConfigOption co = options.get(optionIndex);
                return co;
            }
        }

        return null;
    }

    public class ConfigItem implements java.io.Serializable {
        GenericValue configItem = null;
        GenericValue configItemAssoc = null;
        ProductConfigItemContentWrapper content = null;
        List<ConfigOption> options = null;
        boolean first = true;

        public ConfigItem(GenericValue questionAssoc) throws Exception {
            configItemAssoc = questionAssoc;
            configItem = configItemAssoc.getRelatedOne("ConfigItemProductConfigItem", false);
            options = new LinkedList<>();
        }

        public ConfigItem(ConfigItem ci) {
            configItem = GenericValue.create(ci.configItem);
            configItemAssoc = GenericValue.create(ci.configItemAssoc);
            options = new LinkedList<>();
            for (ConfigOption co: ci.options) {
                options.add(new ConfigOption(co));
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
            return "STANDARD".equals(configItemAssoc.getString("configTypeId"));
        }

        public boolean isSingleChoice() {
            return "SINGLE".equals(configItem.getString("configItemTypeId"));
        }

        public boolean isMandatory() {
            return configItemAssoc.getString("isMandatory") != null && "Y".equals(configItemAssoc.getString("isMandatory"));
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

        public List<ConfigOption> getOptions() {
            return options;
        }

        public String getQuestion() {
            String question = "";
            if (UtilValidate.isNotEmpty(configItemAssoc.getString("description"))) {
                question = configItemAssoc.getString("description");
            } else {
                if (content != null) {
                    question = content.get("DESCRIPTION", "html").toString();
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
                    description = content.get("LONG_DESCRIPTION", "html").toString();
                } else {
                    description = (configItem.getString("longDescription") != null? configItem.getString("longDescription"): "");
                }
            }
            return description;
        }

        public boolean isSelected() {
            if (isStandard()) {
                return true;
            }
            for (ConfigOption oneOption: getOptions()) {
                if (oneOption.isSelected()) {
                    return true;
                }
            }
            return false;
        }

        public ConfigOption getSelected() {
            for (ConfigOption oneOption: getOptions()) {
                if (oneOption.isSelected()) {
                    return oneOption;
                }
            }
            return null;
        }

        public ConfigOption getDefault() {
            String defaultConfigOptionId = configItemAssoc.getString("defaultConfigOptionId");
            if (UtilValidate.isNotEmpty(defaultConfigOptionId)) {
                for (ConfigOption oneOption : getOptions()) {
                    String currentConfigOptionId = oneOption.getId();
                    if (defaultConfigOptionId.compareToIgnoreCase(currentConfigOptionId) == 0 ) {
                        return oneOption;
                    }
                }
            }
            return null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ConfigItem that = (ConfigItem) o;
            return Objects.equals(getConfigItem(), that.getConfigItem()) &&
                    Objects.equals(getConfigItemAssoc(), that.getConfigItemAssoc()) &&
                    Objects.equals(getOptions(), that.getOptions());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getConfigItem(), getConfigItemAssoc(), getOptions());
        }

        @Override
        public String toString() {
            return configItem.getString("configItemId");
        }
    }

    public class ConfigOption implements java.io.Serializable {
        BigDecimal optionListPrice = BigDecimal.ZERO;
        BigDecimal optionPrice = BigDecimal.ZERO;
        Date availabilityDate = null;
        List<GenericValue> componentList = null; // lists of ProductConfigProduct
        Map<String, String> componentOptions = null;
        GenericValue configOption = null;
        boolean selected = false;
        boolean available = true;
        ConfigItem parentConfigItem = null;
        String comments = null;  //  comments for production run entered during ordering

        public ConfigOption(Delegator delegator, LocalDispatcher dispatcher, GenericValue option, ConfigItem configItem, String catalogId, String webSiteId, String currencyUomId, GenericValue autoUserLogin) throws Exception {
            configOption = option;
            parentConfigItem = configItem;
            componentList = option.getRelated("ConfigOptionProductConfigProduct", null, null, false);
            for (GenericValue oneComponent: componentList) {
                BigDecimal listPrice = BigDecimal.ZERO;
                BigDecimal price = BigDecimal.ZERO;
                // Get the component's price
                Map<String, Object> fieldMap = UtilMisc.toMap("product", oneComponent.getRelatedOne("ProductProduct", false), "prodCatalogId", catalogId, "webSiteId", webSiteId, "currencyUomId", currencyUomId, "productPricePurposeId", "COMPONENT_PRICE", "autoUserLogin", autoUserLogin, "productStoreId",productStoreId);
                Map<String, Object> priceMap = dispatcher.runSync("calculateProductPrice", fieldMap);
                BigDecimal componentListPrice = (BigDecimal) priceMap.get("listPrice");
                BigDecimal componentPrice = (BigDecimal) priceMap.get("price");
                Boolean validPriceFound = (Boolean)priceMap.get("validPriceFound");
                BigDecimal mult = BigDecimal.ONE;
                if (oneComponent.getBigDecimal("quantity") != null) {
                    mult = oneComponent.getBigDecimal("quantity");
                }
                if (mult.compareTo(BigDecimal.ZERO) == 0) {
                    mult = BigDecimal.ONE;
                }
                if (validPriceFound.booleanValue()) {
                    if (componentListPrice != null) {
                        listPrice = componentListPrice;
                    }
                    if (componentPrice != null) {
                        price = componentPrice;
                    }
                } else {
                    fieldMap.put("productPricePurposeId", "PURCHASE");
                    Map<String, Object> purchasePriceResultMap = dispatcher.runSync("calculateProductPrice", fieldMap);
                    BigDecimal purchaseListPrice = (BigDecimal) purchasePriceResultMap.get("listPrice");
                    BigDecimal purchasePrice = (BigDecimal) purchasePriceResultMap.get("price");
                    if (purchaseListPrice != null) {
                        listPrice = purchaseListPrice;
                    }
                    if (purchasePrice != null) {
                        price = purchasePrice;
                    }
                }
                optionListPrice = optionListPrice.add(listPrice.multiply(mult));
                optionPrice = optionPrice.add(price.multiply(mult));
                // TODO: get the component's availability date
            }
        }

        public ConfigOption(ConfigOption co) {
            configOption = GenericValue.create(co.configOption);
            componentList = new LinkedList<>();
            for (GenericValue component: co.componentList) {
                componentList.add(GenericValue.create(component));
            }
            parentConfigItem = co.parentConfigItem;
            componentOptions = co.componentOptions;
            optionListPrice = co.optionListPrice;
            optionPrice = co.optionPrice;
            available = co.available;
            selected = co.selected;
            comments = co.getComments();
        }

        public void recalculateOptionPrice(ProductConfigWrapper pcw) throws Exception {
            optionListPrice = BigDecimal.ZERO;
            optionPrice = BigDecimal.ZERO;
            for (GenericValue oneComponent: componentList) {
                BigDecimal listPrice = BigDecimal.ZERO;
                BigDecimal price = BigDecimal.ZERO;
                GenericValue oneComponentProduct = oneComponent.getRelatedOne("ProductProduct", false);
                String variantProductId = componentOptions.get(oneComponent.getString("productId"));

                if (UtilValidate.isNotEmpty(variantProductId)) {
                    oneComponentProduct = EntityQuery.use(delegator).from("Product").where("productId", variantProductId).queryOne();
                }

                // Get the component's price
                Map<String, Object> fieldMap = UtilMisc.toMap("product", oneComponentProduct, "prodCatalogId", pcw.catalogId, "webSiteId", pcw.webSiteId, "currencyUomId", pcw.currencyUomId, "productPricePurposeId", "COMPONENT_PRICE", "autoUserLogin", pcw.autoUserLogin, "productStoreId",productStoreId);
                Map<String, Object> priceMap = pcw.getDispatcher().runSync("calculateProductPrice", fieldMap);
                BigDecimal componentListPrice = (BigDecimal) priceMap.get("listPrice");
                BigDecimal componentPrice = (BigDecimal) priceMap.get("price");
                Boolean validPriceFound = (Boolean)priceMap.get("validPriceFound");
                BigDecimal mult = BigDecimal.ONE;
                if (oneComponent.getBigDecimal("quantity") != null) {
                    mult = oneComponent.getBigDecimal("quantity");
                }
                if (mult.compareTo(BigDecimal.ZERO) == 0) {
                    mult = BigDecimal.ONE;
                }
                if (validPriceFound.booleanValue()) {
                    if (componentListPrice != null) {
                        listPrice = componentListPrice;
                    }
                    if (componentPrice != null) {
                        price = componentPrice;
                    }
                } else {
                    fieldMap.put("productPricePurposeId", "PURCHASE");
                    Map<String, Object> purchasePriceResultMap = pcw.getDispatcher().runSync("calculateProductPrice", fieldMap);
                    BigDecimal purchaseListPrice = (BigDecimal) purchasePriceResultMap.get("listPrice");
                    BigDecimal purchasePrice = (BigDecimal) purchasePriceResultMap.get("price");
                    if (purchaseListPrice != null) {
                        listPrice = purchaseListPrice;
                    }
                    if (purchasePrice != null) {
                        price = purchasePrice;
                    }
                }
                optionListPrice = optionListPrice.add(listPrice.multiply(mult));
                optionPrice = optionPrice.add(price.multiply(mult));
            }
        }

        public String getOptionName() {
            return (configOption.getString("configOptionName") != null? configOption.getString("configOptionName"): "no option name");
        }

        public String getOptionName(Locale locale) {

            return (configOption.getString("configOptionName") != null? (String) configOption.get("configOptionName", locale): "no option name");
        }

        public String getDescription() {
            return (configOption.getString("description") != null? configOption.getString("description"): "no description");
        }

        public String getDescription(Locale locale) {
            return (configOption.getString("description") != null? (String) configOption.get("description", locale): "no description");
        }

        public String getId() {
            return configOption.getString("configOptionId");
        }

        public String getComments() {
            return comments;
        }

        public void setComments(String comments) {
            this.comments = comments;
        }

        public BigDecimal getListPrice() {
            return optionListPrice;
        }

        public BigDecimal getPrice() {
            return optionPrice;
        }

        public BigDecimal getOffsetListPrice() {
            ConfigOption defaultConfigOption = parentConfigItem.getDefault();
            if (parentConfigItem.isSingleChoice() && UtilValidate.isNotEmpty(defaultConfigOption)) {
                return optionListPrice.subtract(defaultConfigOption.getListPrice());
            }
            // can select multiple or no default; show full price
            return optionListPrice;
        }

        public BigDecimal getOffsetPrice() {
            ConfigOption defaultConfigOption = parentConfigItem.getDefault();
            if (parentConfigItem.isSingleChoice() && UtilValidate.isNotEmpty(defaultConfigOption)) {
                return optionPrice.subtract(defaultConfigOption.getPrice());
            }
            // can select multiple or no default; show full price
            return optionPrice;
        }

        public boolean isDefault() {
            ConfigOption defaultConfigOption = parentConfigItem.getDefault();
            if (this.equals(defaultConfigOption)) {
                return true;
            }
            return false;
        }

        public boolean hasVirtualComponent () {
           List <GenericValue> components = getComponents();
           if (UtilValidate.isNotEmpty(components)) {
               for (GenericValue component : components) {
                   if (isVirtualComponent(component)) {
                       return true;
                   }
               }
           }

           return false;
       }

        public boolean isVirtualComponent (GenericValue component) {
            int index = getComponents().indexOf(component);
            if (index != -1) {
                try {
                    GenericValue product = component.getRelatedOne("ProductProduct", false);
                    return "Y".equals(product.getString("isVirtual"));
                } catch (GenericEntityException e) {
                    Debug.logWarning(e.getMessage(), module);
                }
            }
            return false;
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

        public List<GenericValue> getComponents() {
            return componentList;
        }

        public Map<String, String> getComponentOptions() {
            return componentOptions;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ConfigOption that = (ConfigOption) o;
            return Objects.equals(availabilityDate, that.availabilityDate) &&
                    Objects.equals(componentList, that.componentList) &&
                    Objects.equals(getComponentOptions(), that.getComponentOptions()) &&
                    Objects.equals(configOption, that.configOption);
        }

        @Override
        public int hashCode() {
            return Objects.hash(availabilityDate, componentList, getComponentOptions(), configOption);
        }

        @Override
        public String toString() {
            return configOption.getString("configItemId") + "/" + configOption.getString("configOptionId") + (isSelected()? "*": "");
        }

    }

}
