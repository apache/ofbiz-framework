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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import java.util.stream.Collectors;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceContainer;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * Product Config Wrapper: gets product config to display
 */
@SuppressWarnings("serial")
public class ProductConfigWrapper implements Serializable {

    private static final String MODULE = ProductConfigWrapper.class.getName();

    private transient LocalDispatcher dispatcher;
    private String dispatcherName;
    private String productStoreId;
    private String catalogId;
    private String webSiteId;
    private String currencyUomId;
    private transient Delegator delegator;
    private String delegatorName = null;
    private GenericValue product = null; // the aggregated product
    private GenericValue autoUserLogin = null;
    private BigDecimal listPrice = BigDecimal.ZERO;
    private BigDecimal basePrice = BigDecimal.ZERO;
    private BigDecimal defaultPrice = BigDecimal.ZERO;
    private String configId = null; // Id of persisted ProductConfigWrapper
    private List<ConfigItem> questions = null; // ProductConfigs

    /** Creates a new instance of ProductConfigWrapper */
    public ProductConfigWrapper() {
    }

    /**
     * Sets config id.
     * @param configId the config id
     */
    public void setConfigId(String configId) {
        this.configId = configId;
    }

    public ProductConfigWrapper(Delegator delegator, LocalDispatcher dispatcher, String productId, String productStoreId, String catalogId,
                                String webSiteId, String currencyUomId, Locale locale, GenericValue autoUserLogin) throws Exception {
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

    private void init(Delegator delegator, LocalDispatcher dispatcher, String productId, String productStoreId, String catalogId, String webSiteId,
                      String currencyUomId, Locale locale, GenericValue autoUserLogin) throws Exception {
        product = EntityQuery.use(delegator).from("Product").where("productId", productId).queryOne();
        if (product == null || !"AGGREGATED".equals(product.getString("productTypeId"))
                && !"AGGREGATED_SERVICE".equals(product.getString("productTypeId"))) {
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
        Map<String, Object> priceContext = UtilMisc.toMap("product", product, "prodCatalogId", catalogId, "webSiteId", webSiteId,
                "productStoreId", productStoreId, "currencyUomId", currencyUomId, "autoUserLogin", autoUserLogin);
        Map<String, Object> priceMap = dispatcher.runSync("calculateProductPrice", priceContext);
        if (ServiceUtil.isError(priceMap)) {
            String errorMessage = ServiceUtil.getErrorMessage(priceMap);
            throw new GeneralException(errorMessage);
        }
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
            List<GenericValue> questionsValues = EntityQuery.use(delegator).from("ProductConfig").where("productId", productId)
                    .orderBy("sequenceNum").filterByDate().queryList();
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
                List<GenericValue> configOptions = EntityQuery.use(delegator).from("ProductConfigOption").where("configItemId",
                        oneQuestion.getConfigItemAssoc().getString("configItemId")).orderBy("sequenceNum").filterByDate().queryList();
                for (GenericValue configOption: configOptions) {
                    ConfigOption option = new ConfigOption(delegator, dispatcher, configOption, oneQuestion, catalogId, webSiteId,
                            currencyUomId, autoUserLogin);
                    oneQuestion.addOption(option);
                }
            }
            this.setDefaultPrice();
        }
    }

    /**
     * Load config.
     * @param delegator the delegator
     * @param configId the config id
     * @throws Exception the exception
     */
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

    /**
     * Sets selected.
     * @param configItemId the config item id
     * @param sequenceNum the sequence num
     * @param configOptionId the config option id
     * @param comments the comments
     * @throws Exception the exception
     */
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

    /**
     * Reset config.
     */
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

    /**
     * Sets default config.
     */
    public void setDefaultConfig() {
        resetConfig();
        for (ConfigItem ci: questions) {
            if (ci.isMandatory()) {
                ConfigOption co = ci.getDefault();
                if (co != null) {
                    co.setSelected(true);
                } else if (!ci.getOptions().isEmpty()) {
                    co = ci.getOptions().get(0);
                    co.setSelected(true);
                }
            }
        }
    }

    /**
     * Gets config id.
     * @return the config id
     */
    public String getConfigId() {
        return configId;
    }

    /**
     * Gets delegator.
     * @return the delegator
     */
    public Delegator getDelegator() {
        if (delegator == null) {
            delegator = DelegatorFactory.getDelegator(delegatorName);
        }
        return delegator;
    }

    /**
     * Gets dispatcher.
     * @return the dispatcher
     */
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
        ProductConfigWrapper cw = (ProductConfigWrapper) obj;
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

    /**
     * Gets questions.
     * @return the questions
     */
    public List<ConfigItem> getQuestions() {
        return questions;
    }

    /**
     * Gets product.
     * @return the product
     */
    public GenericValue getProduct() {
        return product;
    }

    /**
     * Sets selected.
     * @param question the question
     * @param option the option
     * @param comments the comments
     * @throws Exception the exception
     */
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

    /**
     * Sets selected.
     * @param question the question
     * @param option the option
     * @param component the component
     * @param componentOption the component option
     * @throws Exception the exception
     */
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

    /**
     * Gets selected options.
     * @return the selected options
     */
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

    /**
     * Gets default options.
     * @return the default options
     */
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

    /**
     * Gets total list price.
     * @return the total list price
     */
    public BigDecimal getTotalListPrice() {
        BigDecimal totalListPrice = listPrice;
        List<ConfigOption> options = getSelectedOptions();
        for (ConfigOption oneOption: options) {
            totalListPrice = totalListPrice.add(oneOption.getListPrice());
        }
        return totalListPrice;
    }

    /**
     * Gets total price.
     * @return the total price
     */
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

    /**
     * Gets default price.
     * @return the default price
     */
    public BigDecimal getDefaultPrice() {
        return defaultPrice;
    }

    /**
     * Is completed boolean.
     * @return the boolean
     */
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

    /**
     * Gets item otion.
     * @param itemIndex the item index
     * @param optionIndex the option index
     * @return the item otion
     */
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
        private GenericValue configItem = null;
        private GenericValue configItemAssoc = null;
        private ProductConfigItemContentWrapper content = null;
        private List<ConfigOption> options = null;
        private boolean first = true;

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

        /**
         * Sets content.
         * @param locale     the locale
         * @param mimeTypeId the mime type id
         */
        public void setContent(Locale locale, String mimeTypeId) {
            content = new ProductConfigItemContentWrapper(dispatcher, configItem, locale, mimeTypeId);
        }

        /**
         * Gets content.
         * @return the content
         */
        public ProductConfigItemContentWrapper getContent() {
            return content;
        }

        /**
         * Gets config item.
         * @return the config item
         */
        public GenericValue getConfigItem() {
            return configItem;
        }

        /**
         * Gets config item assoc.
         * @return the config item assoc
         */
        public GenericValue getConfigItemAssoc() {
            return configItemAssoc;
        }

        /**
         * Is standard boolean.
         * @return the boolean
         */
        public boolean isStandard() {
            return "STANDARD".equals(configItemAssoc.getString("configTypeId"));
        }

        /**
         * Is single choice boolean.
         * @return the boolean
         */
        public boolean isSingleChoice() {
            return "SINGLE".equals(configItem.getString("configItemTypeId"));
        }

        /**
         * Is mandatory boolean.
         * @return the boolean
         */
        public boolean isMandatory() {
            return configItemAssoc.getString("isMandatory") != null && "Y".equals(configItemAssoc.getString("isMandatory"));
        }

        /**
         * Is first boolean.
         * @return the boolean
         */
        public boolean isFirst() {
            return first;
        }

        /**
         * Sets first.
         * @param newValue the new value
         */
        public void setFirst(boolean newValue) {
            first = newValue;
        }

        /**
         * Add option.
         * @param option the option
         */
        public void addOption(ConfigOption option) {
            options.add(option);
        }

        /**
         * Gets options.
         * @return the options
         */
        public List<ConfigOption> getOptions() {
            return options;
        }

        /**
         * Gets question.
         * @return the question
         */
        public String getQuestion() {
            String question = "";
            if (UtilValidate.isNotEmpty(configItemAssoc.getString("description"))) {
                question = configItemAssoc.getString("description");
            } else {
                if (content != null) {
                    question = content.get("DESCRIPTION", "html").toString();
                } else {
                    question = (configItem.getString("description") != null ? configItem.getString("description") : "");
                }
            }
            return question;
        }

        /**
         * Gets description.
         * @return the description
         */
        public String getDescription() {
            String description = "";
            if (UtilValidate.isNotEmpty(configItemAssoc.getString("longDescription"))) {
                description = configItemAssoc.getString("longDescription");
            } else {
                if (content != null) {
                    description = content.get("LONG_DESCRIPTION", "html").toString();
                } else {
                    description = (configItem.getString("longDescription") != null ? configItem.getString("longDescription") : "");
                }
            }
            return description;
        }

        /**
         * Is selected boolean.
         * @return the boolean
         */
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

        /**
         * Gets selected.
         * @return the selected
         */
        public ConfigOption getSelected() {
            for (ConfigOption oneOption: getOptions()) {
                if (oneOption.isSelected()) {
                    return oneOption;
                }
            }
            return null;
        }

        /**
         * Gets default.
         * @return the default
         */
        public ConfigOption getDefault() {
            String defaultConfigOptionId = configItemAssoc.getString("defaultConfigOptionId");
            if (UtilValidate.isNotEmpty(defaultConfigOptionId)) {
                for (ConfigOption oneOption : getOptions()) {
                    String currentConfigOptionId = oneOption.getId();
                    if (defaultConfigOptionId.compareToIgnoreCase(currentConfigOptionId) == 0) {
                        return oneOption;
                    }
                }
            }
            return null;
        }

        private boolean isConfigOptionsSelectionEqual(ConfigItem other) {
            List<ConfigOption> mineOptions = getOptions().stream().filter(x -> x.isSelected()).collect(Collectors.toList());
            List<ConfigOption> otherOptions = other.getOptions().stream().filter(x -> x.isSelected()).collect(Collectors.toList());

            if (otherOptions != null && mineOptions != null
                    && otherOptions.size() != mineOptions.size()) {
                return false;
            }
            for (int i = 0; i < mineOptions.size(); i++) {
                if (!mineOptions.get(i).equals(otherOptions.get(i))) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ConfigItem that = (ConfigItem) o;
            return Objects.equals(getConfigItem(), that.getConfigItem())
                    && Objects.equals(getConfigItemAssoc(), that.getConfigItemAssoc())
                    && isConfigOptionsSelectionEqual(that);
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

    /**
     * The type Config option.
     */
    public class ConfigOption implements java.io.Serializable {
        private BigDecimal optionListPrice = BigDecimal.ZERO;
        private BigDecimal optionPrice = BigDecimal.ZERO;
        private Date availabilityDate = null;
        private List<GenericValue> componentList = null; // lists of ProductConfigProduct
        private Map<String, String> componentOptions = null;
        private GenericValue configOption = null;
        private boolean selected = false;
        private boolean available = true;
        private ConfigItem parentConfigItem = null;
        private String comments = null;  //  comments for production run entered during ordering

        /**
         * Gets config option.
         * @return the config option
         */
        public GenericValue getConfigOption() {
            return configOption;
        }

        public ConfigOption(Delegator delegator, LocalDispatcher dispatcher, GenericValue option, ConfigItem configItem, String catalogId,
                            String webSiteId, String currencyUomId, GenericValue autoUserLogin) throws Exception {
            configOption = option;
            parentConfigItem = configItem;
            componentList = option.getRelated("ConfigOptionProductConfigProduct", null, null, false);
            for (GenericValue oneComponent: componentList) {
                BigDecimal listPrice = BigDecimal.ZERO;
                BigDecimal price = BigDecimal.ZERO;
                // Get the component's price
                Map<String, Object> fieldMap = UtilMisc.toMap("product", oneComponent.getRelatedOne("ProductProduct", false),
                        "prodCatalogId", catalogId, "webSiteId", webSiteId, "currencyUomId", currencyUomId, "productPricePurposeId",
                        "COMPONENT_PRICE", "autoUserLogin", autoUserLogin, "productStoreId", productStoreId);
                Map<String, Object> priceMap = dispatcher.runSync("calculateProductPrice", fieldMap);
                if (ServiceUtil.isError(priceMap)) {
                    String errorMessage = ServiceUtil.getErrorMessage(priceMap);
                    throw new GeneralException(errorMessage);
                }
                BigDecimal componentListPrice = (BigDecimal) priceMap.get("listPrice");
                BigDecimal componentPrice = (BigDecimal) priceMap.get("price");
                Boolean validPriceFound = (Boolean) priceMap.get("validPriceFound");
                BigDecimal mult = BigDecimal.ONE;
                if (oneComponent.getBigDecimal("quantity") != null) {
                    mult = oneComponent.getBigDecimal("quantity");
                }
                if (mult.compareTo(BigDecimal.ZERO) == 0) {
                    mult = BigDecimal.ONE;
                }
                if (validPriceFound) {
                    if (componentListPrice != null) {
                        listPrice = componentListPrice;
                    }
                    if (componentPrice != null) {
                        price = componentPrice;
                    }
                } else {
                    fieldMap.put("productPricePurposeId", "PURCHASE");
                    Map<String, Object> purchasePriceResultMap = dispatcher.runSync("calculateProductPrice", fieldMap);
                    if (ServiceUtil.isError(purchasePriceResultMap)) {
                        String errorMessage = ServiceUtil.getErrorMessage(purchasePriceResultMap);
                        throw new GeneralException(errorMessage);
                    }
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

        /**
         * Recalculate option price.
         * @param pcw the pcw
         * @throws Exception the exception
         */
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
                Map<String, Object> fieldMap = UtilMisc.toMap("product", oneComponentProduct, "prodCatalogId", pcw.catalogId, "webSiteId",
                        pcw.webSiteId, "currencyUomId", pcw.currencyUomId, "productPricePurposeId", "COMPONENT_PRICE", "autoUserLogin",
                        pcw.autoUserLogin, "productStoreId", productStoreId);
                Map<String, Object> priceMap = pcw.getDispatcher().runSync("calculateProductPrice", fieldMap);
                Map<String, Object> purchasePriceResultMap = dispatcher.runSync("calculateProductPrice", fieldMap);
                if (ServiceUtil.isError(purchasePriceResultMap)) {
                    String errorMessage = ServiceUtil.getErrorMessage(purchasePriceResultMap);
                    throw new GeneralException(errorMessage);
                }
                BigDecimal componentListPrice = (BigDecimal) priceMap.get("listPrice");
                BigDecimal componentPrice = (BigDecimal) priceMap.get("price");
                Boolean validPriceFound = (Boolean) priceMap.get("validPriceFound");
                BigDecimal mult = BigDecimal.ONE;
                if (oneComponent.getBigDecimal("quantity") != null) {
                    mult = oneComponent.getBigDecimal("quantity");
                }
                if (mult.compareTo(BigDecimal.ZERO) == 0) {
                    mult = BigDecimal.ONE;
                }
                if (validPriceFound) {
                    if (componentListPrice != null) {
                        listPrice = componentListPrice;
                    }
                    if (componentPrice != null) {
                        price = componentPrice;
                    }
                } else {
                    fieldMap.put("productPricePurposeId", "PURCHASE");
                    purchasePriceResultMap = pcw.getDispatcher().runSync("calculateProductPrice", fieldMap);
                    if (ServiceUtil.isError(purchasePriceResultMap)) {
                        String errorMessage = ServiceUtil.getErrorMessage(purchasePriceResultMap);
                        throw new GeneralException(errorMessage);
                    }
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

        /**
         * Gets option name.
         * @return the option name
         */
        public String getOptionName() {
            return (configOption.getString("configOptionName") != null ? configOption.getString("configOptionName") : "no option name");
        }

        /**
         * Gets option name.
         * @param locale the locale
         * @return the option name
         */
        public String getOptionName(Locale locale) {

            return (configOption.getString("configOptionName") != null ? (String) configOption.get("configOptionName", locale) : "no option name");
        }

        /**
         * Gets description.
         * @return the description
         */
        public String getDescription() {
            return (configOption.getString("description") != null ? configOption.getString("description") : "no description");
        }

        /**
         * Gets description.
         * @param locale the locale
         * @return the description
         */
        public String getDescription(Locale locale) {
            return (configOption.getString("description") != null ? (String) configOption.get("description", locale) : "no description");
        }

        /**
         * Gets id.
         * @return the id
         */
        public String getId() {
            return configOption.getString("configOptionId");
        }

        /**
         * Gets comments.
         * @return the comments
         */
        public String getComments() {
            return comments;
        }

        /**
         * Sets comments.
         * @param comments the comments
         */
        public void setComments(String comments) {
            this.comments = comments;
        }

        /**
         * Gets list price.
         * @return the list price
         */
        public BigDecimal getListPrice() {
            return optionListPrice;
        }

        /**
         * Gets price.
         * @return the price
         */
        public BigDecimal getPrice() {
            return optionPrice;
        }

        /**
         * Gets offset list price.
         * @return the offset list price
         */
        public BigDecimal getOffsetListPrice() {
            ConfigOption defaultConfigOption = parentConfigItem.getDefault();
            if (parentConfigItem.isSingleChoice() && UtilValidate.isNotEmpty(defaultConfigOption)) {
                return optionListPrice.subtract(defaultConfigOption.getListPrice());
            }
            // can select multiple or no default; show full price
            return optionListPrice;
        }

        /**
         * Gets offset price.
         * @return the offset price
         */
        public BigDecimal getOffsetPrice() {
            ConfigOption defaultConfigOption = parentConfigItem.getDefault();
            if (parentConfigItem.isSingleChoice() && UtilValidate.isNotEmpty(defaultConfigOption)) {
                return optionPrice.subtract(defaultConfigOption.getPrice());
            }
            // can select multiple or no default; show full price
            return optionPrice;
        }

        /**
         * Is default boolean.
         * @return the boolean
         */
        public boolean isDefault() {
            ConfigOption defaultConfigOption = parentConfigItem.getDefault();
            return this.equals(defaultConfigOption);
        }

        /**
         * Has virtual component boolean.
         * @return the boolean
         */
        public boolean hasVirtualComponent() {
            List<GenericValue> components = getComponents();
            if (UtilValidate.isNotEmpty(components)) {
                for (GenericValue component : components) {
                    if (isVirtualComponent(component)) {
                        return true;
                    }
                }
            }

            return false;
        }

        /**
         * Is virtual component boolean.
         * @param component the component
         * @return the boolean
         */
        public boolean isVirtualComponent(GenericValue component) {
            int index = getComponents().indexOf(component);
            if (index != -1) {
                try {
                    GenericValue product = component.getRelatedOne("ProductProduct", false);
                    return "Y".equals(product.getString("isVirtual"));
                } catch (GenericEntityException e) {
                    Debug.logWarning(e.getMessage(), MODULE);
                }
            }
            return false;
        }

        /**
         * Is selected boolean.
         * @return the boolean
         */
        public boolean isSelected() {
            return selected;
        }

        /**
         * Sets selected.
         * @param newValue the new value
         */
        public void setSelected(boolean newValue) {
            selected = newValue;
        }

        /**
         * Is available boolean.
         * @return the boolean
         */
        public boolean isAvailable() {
            return available;
        }

        /**
         * Sets available.
         * @param newValue the new value
         */
        public void setAvailable(boolean newValue) {
            available = newValue;
        }

        /**
         * Gets components.
         * @return the components
         */
        public List<GenericValue> getComponents() {
            return componentList;
        }

        /**
         * Gets component options.
         * @return the component options
         */
        public Map<String, String> getComponentOptions() {
            return componentOptions;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ConfigOption that = (ConfigOption) o;
            return that.getId() == getId()
                    && that.isSelected() == isSelected()
                    && Objects.equals(availabilityDate, that.availabilityDate)
                    && Objects.equals(componentList, that.componentList)
                    && Objects.equals(getComponentOptions(), that.getComponentOptions())
                    && Objects.equals(configOption, that.configOption);
        }

        @Override
        public int hashCode() {
            return Objects.hash(availabilityDate, componentList, getComponentOptions(), configOption);
        }

        @Override
        public String toString() {
            return configOption.getString("configItemId") + "/" + configOption.getString("configOptionId") + (isSelected() ? "*" : "");
        }

    }

}
