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
package org.apache.ofbiz.product.product;

import java.math.BigDecimal;
import java.math.MathContext;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.common.geo.GeoWorker;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityTypeUtil;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.product.config.ProductConfigWrapper;
import org.apache.ofbiz.product.config.ProductConfigWrapper.ConfigOption;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;

/**
 * Product Worker class to reduce code in JSPs.
 */
public final class ProductWorker {

    public static final String module = ProductWorker.class.getName();

    private static final MathContext generalRounding = new MathContext(10);

    private ProductWorker () {}

    public static boolean shippingApplies(GenericValue product) {
        String errMsg = "";
        if (product != null) {
            String productTypeId = product.getString("productTypeId");
            if ("SERVICE".equals(productTypeId) || "SERVICE_PRODUCT".equals(productTypeId) || (ProductWorker.isDigital(product) && !ProductWorker.isPhysical(product))) {
                // don't charge shipping on services or digital goods
                return false;
            }
            Boolean chargeShipping = product.getBoolean("chargeShipping");

            if (chargeShipping == null) {
                return true;
            }
            return chargeShipping.booleanValue();
        }
        throw new IllegalArgumentException(errMsg);
    }

    public static boolean isBillableToAddress(GenericValue product, GenericValue postalAddress) {
        return isAllowedToAddress(product, postalAddress, "PG_PURCH_");
    }
    public static boolean isShippableToAddress(GenericValue product, GenericValue postalAddress) {
        return isAllowedToAddress(product, postalAddress, "PG_SHIP_");
    }
    private static boolean isAllowedToAddress(GenericValue product, GenericValue postalAddress, String productGeoPrefix) {
        if (product != null && postalAddress != null) {
            Delegator delegator = product.getDelegator();
            List<GenericValue> productGeos = null;
            try {
                productGeos = product.getRelated("ProductGeo", null, null, false);
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
            List<GenericValue> excludeGeos = EntityUtil.filterByAnd(productGeos, UtilMisc.toMap("productGeoEnumId", productGeoPrefix + "EXCLUDE"));
            List<GenericValue> includeGeos = EntityUtil.filterByAnd(productGeos, UtilMisc.toMap("productGeoEnumId", productGeoPrefix + "INCLUDE"));
            if (UtilValidate.isEmpty(excludeGeos) && UtilValidate.isEmpty(includeGeos)) {
                // If no GEOs are configured the default is TRUE
                return true;
            }
            // exclusion
            for (GenericValue productGeo: excludeGeos) {
                List<GenericValue> excludeGeoGroup = GeoWorker.expandGeoGroup(productGeo.getString("geoId"), delegator);
                if (GeoWorker.containsGeo(excludeGeoGroup, postalAddress.getString("countryGeoId"), delegator) ||
                      GeoWorker.containsGeo(excludeGeoGroup, postalAddress.getString("stateProvinceGeoId"), delegator) ||
                      GeoWorker.containsGeo(excludeGeoGroup, postalAddress.getString("postalCodeGeoId"), delegator)) {
                    return false;
                }
            }
            if (UtilValidate.isEmpty(includeGeos)) {
                // If no GEOs are configured the default is TRUE
                return true;
            }
            // inclusion
            for (GenericValue productGeo: includeGeos) {
                List<GenericValue> includeGeoGroup = GeoWorker.expandGeoGroup(productGeo.getString("geoId"), delegator);
                if (GeoWorker.containsGeo(includeGeoGroup, postalAddress.getString("countryGeoId"), delegator) ||
                      GeoWorker.containsGeo(includeGeoGroup, postalAddress.getString("stateProvinceGeoId"), delegator) ||
                      GeoWorker.containsGeo(includeGeoGroup, postalAddress.getString("postalCodeGeoId"), delegator)) {
                    return true;
                }
            }

        } else {
            throw new IllegalArgumentException("product and postalAddress cannot be null.");
        }
        return false;
    }
    public static boolean isSerialized (Delegator delegator, String productId) {
        try {
            GenericValue product = EntityQuery.use(delegator).from("Product").where("productId", productId).cache().queryOne();
            if (product != null) {
                return "SERIALIZED_INV_ITEM".equals(product.getString("inventoryItemTypeId"));
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
        }
        return false;
    }

    public static boolean taxApplies(GenericValue product) {
        String errMsg = "";
        if (product != null) {
            Boolean taxable = product.getBoolean("taxable");

            if (taxable == null) {
                return true;
            }
            return taxable.booleanValue();
        }
        throw new IllegalArgumentException(errMsg);
    }

    public static String getInstanceAggregatedId(Delegator delegator, String instanceProductId) throws GenericEntityException {
        GenericValue instanceProduct = EntityQuery.use(delegator).from("Product").where("productId", instanceProductId).queryOne();

        if (instanceProduct != null && EntityTypeUtil.hasParentType(delegator, "ProductType", "productTypeId", instanceProduct.getString("productTypeId"), "parentTypeId", "AGGREGATED")) {
            GenericValue productAssoc = EntityUtil.getFirst(EntityUtil.filterByDate(instanceProduct.getRelated("AssocProductAssoc", UtilMisc.toMap("productAssocTypeId", "PRODUCT_CONF"), null, false)));
            if (productAssoc != null) {
                return productAssoc.getString("productId");
            }
        }
        return null;
    }

    public static String getAggregatedInstanceId(Delegator delegator, String  aggregatedProductId, String configId) throws GenericEntityException {
        List<GenericValue> productAssocs = getAggregatedAssocs(delegator, aggregatedProductId);
        if (UtilValidate.isNotEmpty(productAssocs) && UtilValidate.isNotEmpty(configId)) {
            for (GenericValue productAssoc: productAssocs) {
                GenericValue product = productAssoc.getRelatedOne("AssocProduct", false);
                if (configId.equals(product.getString("configId"))) {
                    return productAssoc.getString("productIdTo");
                }
            }
        }
        return null;
    }

    public static List<GenericValue> getAggregatedAssocs(Delegator delegator, String  aggregatedProductId) throws GenericEntityException {
        GenericValue aggregatedProduct = EntityQuery.use(delegator).from("Product").where("productId", aggregatedProductId).queryOne();

        if (aggregatedProduct != null && ("AGGREGATED".equals(aggregatedProduct.getString("productTypeId")) || "AGGREGATED_SERVICE".equals(aggregatedProduct.getString("productTypeId")))) {
            List<GenericValue> productAssocs = EntityUtil.filterByDate(aggregatedProduct.getRelated("MainProductAssoc", UtilMisc.toMap("productAssocTypeId", "PRODUCT_CONF"), null, false));
            return productAssocs;
        }
        return null;
    }

    public static String getVariantVirtualId(GenericValue variantProduct) throws GenericEntityException {
        List<GenericValue> productAssocs = getVariantVirtualAssocs(variantProduct);
        if (productAssocs == null) {
            return null;
        }
        GenericValue productAssoc = EntityUtil.getFirst(productAssocs);
        if (productAssoc != null) {
            return productAssoc.getString("productId");
        }
        return null;
    }

    public static List<GenericValue> getVariantVirtualAssocs(GenericValue variantProduct) throws GenericEntityException {
        if (variantProduct != null && "Y".equals(variantProduct.getString("isVariant"))) {
            List<GenericValue> productAssocs = EntityUtil.filterByDate(variantProduct.getRelated("AssocProductAssoc", UtilMisc.toMap("productAssocTypeId", "PRODUCT_VARIANT"), null, true));
            return productAssocs;
        }
        return null;
    }

    /**
     * invokes the getInventoryAvailableByFacility service, returns true if specified quantity is available, else false
     * this is only used in the related method that uses a ProductConfigWrapper, until that is refactored into a service as well...
     */
    private static boolean isProductInventoryAvailableByFacility(String productId, String inventoryFacilityId, BigDecimal quantity, LocalDispatcher dispatcher) {
        BigDecimal availableToPromise = null;

        try {
            Map<String, Object> result = dispatcher.runSync("getInventoryAvailableByFacility",
                                            UtilMisc.toMap("productId", productId, "facilityId", inventoryFacilityId));

            availableToPromise = (BigDecimal) result.get("availableToPromiseTotal");

            if (availableToPromise == null) {
                Debug.logWarning("The getInventoryAvailableByFacility service returned a null availableToPromise, the error message was:\n" + result.get(ModelService.ERROR_MESSAGE), module);
                return false;
            }
        } catch (GenericServiceException e) {
            Debug.logWarning(e, "Error invoking getInventoryAvailableByFacility service in isCatalogInventoryAvailable", module);
            return false;
        }

        // check to see if we got enough back...
        if (availableToPromise.compareTo(quantity) >= 0) {
            if (Debug.infoOn()) {
                Debug.logInfo("Inventory IS available in facility with id " + inventoryFacilityId + " for product id " + productId + "; desired quantity is " + quantity + ", available quantity is " + availableToPromise, module);
            }
            return true;
        }
        if (Debug.infoOn()) {
            Debug.logInfo("Returning false because there is insufficient inventory available in facility with id "
                    + inventoryFacilityId + " for product id " + productId + "; desired quantity is " + quantity
                    + ", available quantity is " + availableToPromise, module);
        }
        return false;
    }

    /**
     * Invokes the getInventoryAvailableByFacility service, returns true if specified quantity is available for all the selected parts, else false.
     * Also, set the available flag for all the product configuration's options.
     **/
    public static boolean isProductInventoryAvailableByFacility(ProductConfigWrapper productConfig, String inventoryFacilityId, BigDecimal quantity, LocalDispatcher dispatcher) {
        boolean available = true;
        List<ConfigOption> options = productConfig.getSelectedOptions();
        for (ConfigOption ci: options) {
            List<GenericValue> products = ci.getComponents();
            for (GenericValue product: products) {
                String productId = product.getString("productId");
                BigDecimal cmpQuantity = product.getBigDecimal("quantity");
                BigDecimal neededQty = BigDecimal.ZERO;
                if (cmpQuantity != null) {
                    neededQty = quantity.multiply(cmpQuantity);
                }
                if (!isProductInventoryAvailableByFacility(productId, inventoryFacilityId, neededQty, dispatcher)) {
                    ci.setAvailable(false);
                }
            }
            if (!ci.isAvailable()) {
                available = false;
            }
        }
        return available;
    }

    /**
     * Gets ProductFeature GenericValue for all distinguishing features of a variant product.
     * Distinguishing means all features that are selectable on the corresponding virtual product and standard on the variant plus all DISTINGUISHING_FEAT assoc type features on the variant.
     */
    public static Set<GenericValue> getVariantDistinguishingFeatures(GenericValue variantProduct) throws GenericEntityException {
        if (variantProduct == null) {
            return new HashSet<>();
        }
        if (!"Y".equals(variantProduct.getString("isVariant"))) {
            throw new IllegalArgumentException("Cannot get distinguishing features for a product that is not a variant (ie isVariant!=Y).");
        }
        Delegator delegator = variantProduct.getDelegator();
        String virtualProductId = getVariantVirtualId(variantProduct);

        // find all selectable features on the virtual product that are also standard features on the variant
        Set<GenericValue> distFeatures = new HashSet<>();

        List<GenericValue> variantDistinguishingFeatures = EntityQuery.use(delegator).from("ProductFeatureAndAppl").where("productId", variantProduct.get("productId"), "productFeatureApplTypeId", "DISTINGUISHING_FEAT").cache(true).queryList();

        for (GenericValue variantDistinguishingFeature: EntityUtil.filterByDate(variantDistinguishingFeatures)) {
            GenericValue dummyFeature = delegator.makeValue("ProductFeature");
            dummyFeature.setAllFields(variantDistinguishingFeature, true, null, null);
            distFeatures.add(dummyFeature);
        }

        List<GenericValue> virtualSelectableFeatures = EntityQuery.use(delegator).from("ProductFeatureAndAppl").where("productId", virtualProductId, "productFeatureApplTypeId", "SELECTABLE_FEATURE").cache(true).queryList();

        Set<String> virtualSelectableFeatureIds = new HashSet<>();
        for (GenericValue virtualSelectableFeature: EntityUtil.filterByDate(virtualSelectableFeatures)) {
            virtualSelectableFeatureIds.add(virtualSelectableFeature.getString("productFeatureId"));
        }

        List<GenericValue> variantStandardFeatures = EntityQuery.use(delegator).from("ProductFeatureAndAppl").where("productId", variantProduct.get("productId"), "productFeatureApplTypeId", "STANDARD_FEATURE").cache(true).queryList();

        for (GenericValue variantStandardFeature: EntityUtil.filterByDate(variantStandardFeatures)) {
            if (virtualSelectableFeatureIds.contains(variantStandardFeature.get("productFeatureId"))) {
                GenericValue dummyFeature = delegator.makeValue("ProductFeature");
                dummyFeature.setAllFields(variantStandardFeature, true, null, null);
                distFeatures.add(dummyFeature);
            }
        }

        return distFeatures;
    }

    /**
     *  Get the name to show to the customer for GWP alternative options.
     *  If the alternative is a variant, find the distinguishing features and show those instead of the name; if it is not a variant then show the PRODUCT_NAME content.
     */
    public static String getGwpAlternativeOptionName(LocalDispatcher dispatcher, Delegator delegator, String alternativeOptionProductId, Locale locale) {
        try {
            GenericValue alternativeOptionProduct = EntityQuery.use(delegator).from("Product").where("productId", alternativeOptionProductId).cache().queryOne();
            if (alternativeOptionProduct != null) {
                if ("Y".equals(alternativeOptionProduct.getString("isVariant"))) {
                    Set<GenericValue> distFeatures = getVariantDistinguishingFeatures(alternativeOptionProduct);
                    if (UtilValidate.isNotEmpty(distFeatures)) {
                        StringBuilder nameBuf = new StringBuilder();
                        for (GenericValue productFeature: distFeatures) {
                            if (nameBuf.length() > 0) {
                                nameBuf.append(", ");
                            }
                            GenericValue productFeatureType = productFeature.getRelatedOne("ProductFeatureType", true);
                            if (productFeatureType != null) {
                                nameBuf.append(productFeatureType.get("description", locale));
                                nameBuf.append(":");
                            }
                            nameBuf.append(productFeature.get("description", locale));
                        }
                        return nameBuf.toString();
                    }
                }

                // got to here, default to PRODUCT_NAME
                String alternativeProductName = ProductContentWrapper.getProductContentAsText(alternativeOptionProduct, "PRODUCT_NAME", locale, dispatcher, "html");
                return alternativeProductName;
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        // finally fall back to the ID in square braces
        return "[" + alternativeOptionProductId + "]";
    }

    /**
     * gets productFeatures given a productFeatureApplTypeId
     * @param delegator
     * @param productId
     * @param productFeatureApplTypeId - if null, returns ALL productFeatures, regardless of applType
     * @return List
     */
    public static List<GenericValue> getProductFeaturesByApplTypeId(Delegator delegator, String productId, String productFeatureApplTypeId) {
        if (productId == null) {
            return null;
        }
        try {
            return getProductFeaturesByApplTypeId(EntityQuery.use(delegator).from("Product").where("productId", productId).queryOne(),
                    productFeatureApplTypeId);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return null;
    }

    public static List<GenericValue> getProductFeaturesByApplTypeId(GenericValue product, String productFeatureApplTypeId) {
        if (product == null) {
            return null;
        }
        List<GenericValue> features = null;
        try {
            List<GenericValue> productAppls;
            List<EntityCondition> condList = UtilMisc.toList(
                    EntityCondition.makeCondition("productId", product.getString("productId")),
                    EntityUtil.getFilterByDateExpr()
            );
            if (productFeatureApplTypeId != null) {
                condList.add(EntityCondition.makeCondition("productFeatureApplTypeId", productFeatureApplTypeId));
            }
            EntityCondition cond = EntityCondition.makeCondition(condList);
            productAppls = product.getDelegator().findList("ProductFeatureAppl", cond, null, null, null, false);
            features = EntityUtil.getRelated("ProductFeature", null, productAppls, false);
            features = EntityUtil.orderBy(features, UtilMisc.toList("description"));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            features = new LinkedList<>();
        }
        return features;
    }

    public static String getProductVirtualVariantMethod(Delegator delegator, String productId) {
        GenericValue product = null;
        try {
            product = EntityQuery.use(delegator).from("Product").where("productId", productId).cache().queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }

        if (product != null) {
            return product.getString("virtualVariantMethodEnum");
        }
        return null;
    }

    /**
     *
     * @param product
     * @return list featureType and related featuresIds, description and feature price for this product ordered by type and sequence
     */
    public static List<List<Map<String,String>>> getSelectableProductFeaturesByTypesAndSeq(GenericValue product) {
        if (product == null) {
            return null;
        }
        List <List<Map<String,String>>> featureTypeFeatures = new LinkedList<>();
        try {
            Delegator delegator = product.getDelegator();
            List<GenericValue> featuresSorted = EntityQuery.use(delegator)
                                                    .from("ProductFeatureAndAppl")
                                                    .where("productId", product.getString("productId"), "productFeatureApplTypeId", "SELECTABLE_FEATURE")
                                                    .orderBy("productFeatureTypeId", "sequenceNum")
                                                    .cache(true)
                                                    .queryList();
            String oldType = null;
            List<Map<String,String>> featureList = new LinkedList<>();
            for (GenericValue productFeatureAppl: featuresSorted) {
                if (oldType == null || !oldType.equals(productFeatureAppl.getString("productFeatureTypeId"))) {
                    // use first entry for type and description
                    if (oldType != null) {
                        featureTypeFeatures.add(featureList);
                        featureList = new LinkedList<>();
                    }
                    GenericValue productFeatureType = EntityQuery.use(delegator).from("ProductFeatureType").where("productFeatureTypeId", productFeatureAppl.getString("productFeatureTypeId")).queryOne();
                    featureList.add(UtilMisc.<String, String>toMap("productFeatureTypeId", productFeatureAppl.getString("productFeatureTypeId"),
                            "description", productFeatureType.getString("description")));
                    oldType = productFeatureAppl.getString("productFeatureTypeId");
                }
                // fill other entries with featureId, description and default price and currency
                Map<String,String> featureData = UtilMisc.toMap("productFeatureId", productFeatureAppl.getString("productFeatureId"));
                if (UtilValidate.isNotEmpty(productFeatureAppl.get("description"))) {
                    featureData.put("description", productFeatureAppl.getString("description"));
                } else {
                    featureData.put("description", productFeatureAppl.getString("productFeatureId"));
                }
                List<GenericValue> productFeaturePrices = EntityQuery.use(delegator).from("ProductFeaturePrice")
                        .where("productFeatureId", productFeatureAppl.getString("productFeatureId"), "productPriceTypeId", "DEFAULT_PRICE")
                        .filterByDate()
                        .queryList();
                if (UtilValidate.isNotEmpty(productFeaturePrices)) {
                    GenericValue productFeaturePrice = productFeaturePrices.get(0);
                    if (UtilValidate.isNotEmpty(productFeaturePrice.get("price"))) {
                        featureData.put("price", productFeaturePrice.getBigDecimal("price").toString());
                        featureData.put("currencyUomId", productFeaturePrice.getString("currencyUomId"));
                    }
                }
                featureList.add(featureData);
            }
            if (oldType != null) {
                // last map
                featureTypeFeatures.add(featureList);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return featureTypeFeatures;
    }

    /**
     * For a given variant product, returns the list of features that would qualify it for
     * selection from the virtual product
     * @param variantProduct - the variant from which to derive the selection features
     * @return a List of ProductFeature GenericValues
     */
    public static List<GenericValue> getVariantSelectionFeatures(GenericValue variantProduct) {
        if (variantProduct == null || !"Y".equals(variantProduct.getString("isVariant"))) {
            return null;
        }
        GenericValue virtualProduct = ProductWorker.getParentProduct(variantProduct.getString("productId"), variantProduct.getDelegator());
        if (virtualProduct == null || !"Y".equals(virtualProduct.getString("isVirtual"))) {
            return null;
        }
        // The selectable features from the virtual product
        List<GenericValue> selectableFeatures = ProductWorker.getProductFeaturesByApplTypeId(virtualProduct, "SELECTABLE_FEATURE");
        // A list of distinct ProductFeatureTypes derived from the selectable features
        List<String> selectableTypes = EntityUtil.getFieldListFromEntityList(selectableFeatures, "productFeatureTypeId", true);
        // The standard features from the variant product
        List<GenericValue> standardFeatures = ProductWorker.getProductFeaturesByApplTypeId(variantProduct, "STANDARD_FEATURE");
        List<GenericValue> result = new LinkedList<>();
        for (GenericValue standardFeature : standardFeatures) {
            // For each standard variant feature check it is also a virtual selectable feature and
            // if a feature of the same type hasn't already been added to the list
            if (selectableTypes.contains(standardFeature.getString("productFeatureTypeId")) && selectableFeatures.contains(standardFeature)) {
                result.add(standardFeature);
                selectableTypes.remove(standardFeature.getString("productFeatureTypeId"));
            }
        }
        return result;
    }

    public static Map<String, List<GenericValue>> getOptionalProductFeatures(Delegator delegator, String productId) {
        Map<String, List<GenericValue>> featureMap = new LinkedHashMap<>();

        List<GenericValue> productFeatureAppls = null;
        try {
            productFeatureAppls = EntityQuery.use(delegator).from("ProductFeatureAndAppl").where("productId", productId, "productFeatureApplTypeId", "OPTIONAL_FEATURE").orderBy("productFeatureTypeId", "sequenceNum").queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }

        if (productFeatureAppls != null) {
            for (GenericValue appl: productFeatureAppls) {
                String featureType = appl.getString("productFeatureTypeId");
                List<GenericValue> features = featureMap.get(featureType);
                if (features == null) {
                    features = new LinkedList<>();
                }
                features.add(appl);
                featureMap.put(featureType, features);
            }
        }

        return featureMap;
    }

    // product calc methods

    public static BigDecimal calcOrderAdjustments(List<GenericValue> orderHeaderAdjustments, BigDecimal subTotal, boolean includeOther, boolean includeTax, boolean includeShipping) {
        BigDecimal adjTotal = BigDecimal.ZERO;

        if (UtilValidate.isNotEmpty(orderHeaderAdjustments)) {
            List<GenericValue> filteredAdjs = filterOrderAdjustments(orderHeaderAdjustments, includeOther, includeTax, includeShipping, false, false);
            for (GenericValue orderAdjustment: filteredAdjs) {
                adjTotal = adjTotal.add(calcOrderAdjustment(orderAdjustment, subTotal));
            }
        }
        return adjTotal;
    }

    public static BigDecimal calcOrderAdjustment(GenericValue orderAdjustment, BigDecimal orderSubTotal) {
        BigDecimal adjustment = BigDecimal.ZERO;

        if (orderAdjustment.get("amount") != null) {
            adjustment = adjustment.add(orderAdjustment.getBigDecimal("amount"));
        }
        else if (orderAdjustment.get("sourcePercentage") != null) {
            adjustment = adjustment.add(orderAdjustment.getBigDecimal("sourcePercentage").multiply(orderSubTotal));
        }
        return adjustment;
    }

    public static List<GenericValue> filterOrderAdjustments(List<GenericValue> adjustments, boolean includeOther, boolean includeTax, boolean includeShipping, boolean forTax, boolean forShipping) {
        List<GenericValue> newOrderAdjustmentsList = new LinkedList<>();

        if (UtilValidate.isNotEmpty(adjustments)) {
            for (GenericValue orderAdjustment: adjustments) {
                boolean includeAdjustment = false;

                if ("SALES_TAX".equals(orderAdjustment.getString("orderAdjustmentTypeId"))) {
                    if (includeTax) {
                        includeAdjustment = true;
                    }
                } else if ("SHIPPING_CHARGES".equals(orderAdjustment.getString("orderAdjustmentTypeId"))) {
                    if (includeShipping) {
                        includeAdjustment = true;
                    }
                } else {
                    if (includeOther) {
                        includeAdjustment = true;
                    }
                }

                // default to yes, include for shipping; so only exclude if includeInShipping is N, or false; if Y or null or anything else it will be included
                if (forTax && "N".equals(orderAdjustment.getString("includeInTax"))) {
                    includeAdjustment = false;
                }

                // default to yes, include for shipping; so only exclude if includeInShipping is N, or false; if Y or null or anything else it will be included
                if (forShipping && "N".equals(orderAdjustment.getString("includeInShipping"))) {
                    includeAdjustment = false;
                }

                if (includeAdjustment) {
                    newOrderAdjustmentsList.add(orderAdjustment);
                }
            }
        }
        return newOrderAdjustmentsList;
    }

    public static BigDecimal getAverageProductRating(Delegator delegator, String productId) {
        return getAverageProductRating(delegator, productId, null);
    }

    public static BigDecimal getAverageProductRating(Delegator delegator, String productId, String productStoreId) {
        GenericValue product = null;
        try {
            product = EntityQuery.use(delegator).from("Product").where("productId", productId).cache().queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return ProductWorker.getAverageProductRating(product, productStoreId);
    }

    public static BigDecimal getAverageProductRating(GenericValue product, String productStoreId) {
        return getAverageProductRating(product, null, productStoreId);
    }

    public static BigDecimal getAverageProductRating(GenericValue product, List<GenericValue> reviews, String productStoreId) {
        if (product == null) {
            Debug.logWarning("Invalid product entity passed; unable to obtain valid product rating", module);
            return BigDecimal.ZERO;
        }

        BigDecimal productRating = BigDecimal.ZERO;
        BigDecimal productEntityRating = product.getBigDecimal("productRating");
        String entityFieldType = product.getString("ratingTypeEnum");

        // null check
        if (productEntityRating == null) {
            productEntityRating = BigDecimal.ZERO;
        }
        if (entityFieldType == null) {
            entityFieldType = "";
        }

        if ("PRDR_FLAT".equals(entityFieldType)) {
            productRating = productEntityRating;
        } else {
            // get the product rating from the ProductReview entity; limit by product store if ID is passed
            Map<String, String> reviewByAnd = UtilMisc.toMap("statusId", "PRR_APPROVED");
            if (productStoreId != null) {
                reviewByAnd.put("productStoreId", productStoreId);
            }

            // lookup the reviews if we didn't pass them in
            if (reviews == null) {
                try {
                    reviews = product.getRelated("ProductReview", reviewByAnd, UtilMisc.toList("-postedDateTime"), true);
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                }
            }

            // tally the average
            BigDecimal ratingTally = BigDecimal.ZERO;
            BigDecimal numRatings = BigDecimal.ZERO;
            if (reviews != null) {
                for (GenericValue productReview: reviews) {
                    BigDecimal rating = productReview.getBigDecimal("productRating");
                    if (rating != null) {
                        ratingTally = ratingTally.add(rating);
                        numRatings = numRatings.add(BigDecimal.ONE);
                    }
                }
            }
            if (ratingTally.compareTo(BigDecimal.ZERO) > 0 && numRatings.compareTo(BigDecimal.ZERO) > 0) {
                productRating = ratingTally.divide(numRatings, generalRounding);
            }

            if ("PRDR_MIN".equals(entityFieldType)) {
                // check for min
                if (productEntityRating.compareTo(productRating) > 0) {
                    productRating = productEntityRating;
                }
            } else if ("PRDR_MAX".equals(entityFieldType)) {
                // check for max
                if (productRating.compareTo(productEntityRating) > 0) {
                    productRating = productEntityRating;
                }
            }
        }

        return productRating;
    }

    public static List<GenericValue> getCurrentProductCategories(Delegator delegator, String productId) {
        GenericValue product = null;
        try {
            product = EntityQuery.use(delegator).from("Product").where("productId", productId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return getCurrentProductCategories(product);
    }

    public static List<GenericValue> getCurrentProductCategories(GenericValue product) {
        if (product == null) {
            return null;
        }
        List<GenericValue> categories = new LinkedList<>();
        try {
            List<GenericValue> categoryMembers = product.getRelated("ProductCategoryMember", null, null, false);
            categoryMembers = EntityUtil.filterByDate(categoryMembers);
            categories = EntityUtil.getRelated("ProductCategory", null, categoryMembers, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return categories;
    }

    //get parent product
    public static GenericValue getParentProduct(String productId, Delegator delegator) {
        GenericValue _parentProduct = null;
        if (productId == null) {
            Debug.logWarning("Bad product id", module);
        }

        try {
            List<GenericValue> virtualProductAssocs = EntityQuery.use(delegator).from("ProductAssoc")
                    .where("productIdTo", productId, "productAssocTypeId", "PRODUCT_VARIANT")
                    .orderBy("-fromDate")
                    .cache(true)
                    .filterByDate()
                    .queryList();
            if (UtilValidate.isEmpty(virtualProductAssocs)) {
                //okay, not a variant, try a UNIQUE_ITEM
                virtualProductAssocs = EntityQuery.use(delegator).from("ProductAssoc")
                        .where("productIdTo", productId, "productAssocTypeId", "UNIQUE_ITEM")
                        .orderBy("-fromDate")
                        .cache(true)
                        .filterByDate()
                        .queryList();
            }
            if (UtilValidate.isNotEmpty(virtualProductAssocs)) {
                //found one, set this first as the parent product
                GenericValue productAssoc = EntityUtil.getFirst(virtualProductAssocs);
                _parentProduct = productAssoc.getRelatedOne("MainProduct", true);
            }
        } catch (GenericEntityException e) {
            throw new RuntimeException("Entity Engine error getting Parent Product (" + e.getMessage() + ")");
        }
        return _parentProduct;
    }

    public static boolean isDigital(GenericValue product) {
        boolean isDigital = false;
        if (product != null) {
            GenericValue productType = null;
            try {
                productType = product.getRelatedOne("ProductType", true);
            } catch (GenericEntityException e) {
                Debug.logWarning(e.getMessage(), module);
            }
            String isDigitalValue = (productType != null? productType.getString("isDigital"): null);
            isDigital = isDigitalValue != null && "Y".equalsIgnoreCase(isDigitalValue);
        }
        return isDigital;
    }

    public static boolean isPhysical(GenericValue product) {
        boolean isPhysical = false;
        if (product != null) {
            GenericValue productType = null;
            try {
                productType = product.getRelatedOne("ProductType", true);
            } catch (GenericEntityException e) {
                Debug.logWarning(e.getMessage(), module);
            }
            String isPhysicalValue = (productType != null? productType.getString("isPhysical"): null);
            isPhysical = isPhysicalValue != null && "Y".equalsIgnoreCase(isPhysicalValue);
        }
        return isPhysical;
    }

    public static boolean isVirtual(Delegator delegator, String productI) {
        try {
            GenericValue product = EntityQuery.use(delegator).from("Product").where("productId", productI).cache().queryOne();
            if (product != null) {
                return "Y".equals(product.getString("isVirtual"));
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
        }

        return false;
    }

    public static boolean isAmountRequired(Delegator delegator, String productI) {
        try {
            GenericValue product = EntityQuery.use(delegator).from("Product").where("productId", productI).cache().queryOne();
            if (product != null) {
                return "Y".equals(product.getString("requireAmount"));
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
        }

        return false;
    }

    public static String getProductTypeId(Delegator delegator, String productId) {
        try {
            GenericValue product = EntityQuery.use(delegator).from("Product").where("productId", productId).cache().queryOne();
            if (product != null) {
                return product.getString("productTypeId");
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
        }

        return null;
    }

    /*
     * Returns the product's unit weight converted to the desired Uom.  If the weight is null,
     * then a check is made for an associated virtual product to retrieve the weight from.  If the
     * weight is still null then null is returned.  If a weight is found and a desiredUomId has
     * been supplied and the product specifies a weightUomId then an attempt will be made to
     * convert the value otherwise the weight is returned as is.
     */
    public static BigDecimal getProductWeight(GenericValue product, String desiredUomId, Delegator delegator, LocalDispatcher dispatcher) {
        BigDecimal weight = product.getBigDecimal("productWeight");
        String weightUomId = product.getString("weightUomId");

        if (weight == null) {
            GenericValue parentProduct = getParentProduct(product.getString("productId"), delegator);
            if (parentProduct != null) {
                weight = parentProduct.getBigDecimal("productWeight");
                weightUomId = parentProduct.getString("weightUomId");
            }
        }

        if (weight == null) {
            return null;
        }
        // attempt a conversion if necessary
        if (desiredUomId != null && product.get("weightUomId") != null && !desiredUomId.equals(product.get("weightUomId"))) {
            Map<String, Object> result = new HashMap<>();
            try {
                result = dispatcher.runSync("convertUom", UtilMisc.<String, Object>toMap("uomId", weightUomId, "uomIdTo", desiredUomId, "originalValue", weight));
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
            }

            if (result.get(ModelService.RESPONSE_MESSAGE).equals(ModelService.RESPOND_SUCCESS) && result.get("convertedValue") != null) {
                weight = (BigDecimal) result.get("convertedValue");
            } else {
                Debug.logError("Unsupported conversion from [" + weightUomId + "] to [" + desiredUomId + "]", module);
                return null;
            }
        }
        return weight;
    }



    /**
     * Generic service to find product by id.
     * By default return the product find by productId
     * but you can pass searchProductFirst at false if you want search in goodIdentification before
     * or pass searchAllId at true to find all product with this id (product.productId and goodIdentification.idValue)
     * @param delegator the delegator
     * @param idToFind the product id to find
     * @param goodIdentificationTypeId the good identification type id to use
     * @param searchProductFirst search first by product id
     * @param searchAllId search all product ids
     * @return return the list of products founds
     * @throws GenericEntityException
     */
    public static List<GenericValue> findProductsById(Delegator delegator,
            String idToFind, String goodIdentificationTypeId,
            boolean searchProductFirst, boolean searchAllId) throws GenericEntityException {

        if (Debug.verboseOn()) {
            Debug.logVerbose("Analyze goodIdentification: entered id = " + idToFind + ", goodIdentificationTypeId = " + goodIdentificationTypeId, module);
        }

        GenericValue product = null;
        List<GenericValue> productsFound = null;

        // 1) look if the idToFind given is a real productId
        if (searchProductFirst) {
            product = EntityQuery.use(delegator).from("Product").where("productId", idToFind).cache().queryOne();
        }

        if (searchAllId || (searchProductFirst && UtilValidate.isEmpty(product))) {
            // 2) Retrieve product in GoodIdentification
            Map<String, String> conditions = UtilMisc.toMap("idValue", idToFind);
            if (UtilValidate.isNotEmpty(goodIdentificationTypeId)) {
                conditions.put("goodIdentificationTypeId", goodIdentificationTypeId);
            }
            productsFound = EntityQuery.use(delegator).from("GoodIdentificationAndProduct").where(conditions).orderBy("productId").cache(true).queryList();
        }

        if (! searchProductFirst) {
            product = EntityQuery.use(delegator).from("Product").where("productId", idToFind).cache().queryOne();
        }

        if (product != null) {
            if (UtilValidate.isNotEmpty(productsFound)) {
                productsFound.add(product);
            } else {
                productsFound = UtilMisc.toList(product);
            }
        }
        if (Debug.verboseOn()) {
            Debug.logVerbose("Analyze goodIdentification: found product.productId = " + product + ", and list : " + productsFound, module);
        }
        return productsFound;
    }

    public static List<GenericValue> findProductsById(Delegator delegator, String idToFind, String goodIdentificationTypeId)
    throws GenericEntityException {
        return findProductsById(delegator, idToFind, goodIdentificationTypeId, true, false);
    }

    public static String findProductId(Delegator delegator, String idToFind, String goodIdentificationTypeId) throws GenericEntityException {
        GenericValue product = findProduct(delegator, idToFind, goodIdentificationTypeId);
        if (product != null) {
            return product.getString("productId");
        } else {
            return null;
        }
    }

    public static String findProductId(Delegator delegator, String idToFind) throws GenericEntityException {
        return findProductId(delegator, idToFind, null);
    }

    public static GenericValue findProduct(Delegator delegator, String idToFind, String goodIdentificationTypeId) throws GenericEntityException {
        List<GenericValue> products = findProductsById(delegator, idToFind, goodIdentificationTypeId);
        GenericValue product = EntityUtil.getFirst(products);
        return product;
    }

    public static List<GenericValue> findProducts(Delegator delegator, String idToFind, String goodIdentificationTypeId) throws GenericEntityException {
        List<GenericValue> productsByIds = findProductsById(delegator, idToFind, goodIdentificationTypeId);
        List<GenericValue> products = null;
        if (UtilValidate.isNotEmpty(productsByIds)) {
            for (GenericValue product : productsByIds) {
                GenericValue productToAdd = product;
                //retreive product GV if the actual genericValue came from viewEntity
                if (! "Product".equals(product.getEntityName())) {
                    productToAdd = EntityQuery.use(delegator).from("Product").where("productId", product.get("productId")).cache().queryOne();
                }

                if (UtilValidate.isEmpty(products)) {
                    products = UtilMisc.toList(productToAdd);
                }
                else {
                    products.add(productToAdd);
                }
            }
        }
        return products;
    }

    public static List<GenericValue> findProducts(Delegator delegator, String idToFind) throws GenericEntityException {
        return findProducts(delegator, idToFind, null);
    }

    public static GenericValue findProduct(Delegator delegator, String idToFind) throws GenericEntityException {
        return findProduct(delegator, idToFind, null);
    }

    public static boolean isSellable(Delegator delegator, String productId, Timestamp atTime) throws GenericEntityException {
        return isSellable(findProduct(delegator, productId), atTime);
    }

    public static boolean isSellable(Delegator delegator, String productId) throws GenericEntityException {
        return isSellable(findProduct(delegator, productId));
    }

    public static boolean isSellable(GenericValue product) {
        return isSellable(product, UtilDateTime.nowTimestamp());
    }

    public static boolean isSellable(GenericValue product, Timestamp atTime) {
        if (product != null) {
            Timestamp introDate = product.getTimestamp("introductionDate");
            Timestamp discDate = product.getTimestamp("salesDiscontinuationDate");
            if (introDate == null || introDate.before(atTime)) {
                if (discDate == null || discDate.after(atTime)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static Set<String> getRefurbishedProductIdSet(String productId, Delegator delegator) throws GenericEntityException {
        Set<String> productIdSet = new HashSet<>();

        // find associated refurb items, we want serial number for main item or any refurb items too
        List<GenericValue> refubProductAssocs = EntityQuery.use(delegator).from("ProductAssoc").where("productId", productId, "productAssocTypeId", "PRODUCT_REFURB").filterByDate().queryList();
        for (GenericValue refubProductAssoc: refubProductAssocs) {
            productIdSet.add(refubProductAssoc.getString("productIdTo"));
        }

        // see if this is a refurb productId to, and find product(s) it is a refurb of
        List<GenericValue> refubProductToAssocs = EntityQuery.use(delegator).from("ProductAssoc").where("productIdTo", productId, "productAssocTypeId", "PRODUCT_REFURB").filterByDate().queryList();
        for (GenericValue refubProductToAssoc: refubProductToAssocs) {
            productIdSet.add(refubProductToAssoc.getString("productId"));
        }

        return productIdSet;
    }

    public static String getVariantFromFeatureTree(String productId, List<String> selectedFeatures, Delegator delegator) {

        //  all method code moved here from ShoppingCartEvents.addToCart event
        String variantProductId = null;
        try {

            for (String paramValue: selectedFeatures) {
                // find incompatibilities..
                List<GenericValue> incompatibilityVariants = EntityQuery.use(delegator).from("ProductFeatureIactn")
                        .where("productId", productId, "productFeatureIactnTypeId","FEATURE_IACTN_INCOMP").cache(true).queryList();
                for (GenericValue incompatibilityVariant: incompatibilityVariants) {
                    String featur = incompatibilityVariant.getString("productFeatureId");
                    if (paramValue.equals(featur)) {
                        String featurTo = incompatibilityVariant.getString("productFeatureIdTo");
                        for (String paramValueTo: selectedFeatures) {
                            if (featurTo.equals(paramValueTo)) {
                                Debug.logWarning("Incompatible features", module);
                                return null;
                            }
                        }

                    }
                }
                // find dependencies..
                List<GenericValue> dependenciesVariants = EntityQuery.use(delegator).from("ProductFeatureIactn")
                        .where("productId", productId, "productFeatureIactnTypeId","FEATURE_IACTN_DEPEND").cache(true).queryList();
                for (GenericValue dpVariant: dependenciesVariants) {
                    String featur = dpVariant.getString("productFeatureId");
                    if (paramValue.equals(featur)) {
                        String featurTo = dpVariant.getString("productFeatureIdTo");
                        boolean found = false;
                        for (String paramValueTo: selectedFeatures) {
                            if (featurTo.equals(paramValueTo)) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            Debug.logWarning("Dependency features", module);
                            return null;
                        }
                    }
                }
            }
            // find variant
            List<GenericValue> productAssocs = EntityQuery.use(delegator).from("ProductAssoc").where("productId", productId, "productAssocTypeId","PRODUCT_VARIANT").filterByDate().queryList();
            boolean productFound = false;
nextProd:
            for (GenericValue productAssoc: productAssocs) {
                for (String featureId: selectedFeatures) {
                    List<GenericValue> pAppls = EntityQuery.use(delegator).from("ProductFeatureAppl").where("productId", productAssoc.getString("productIdTo"), "productFeatureId", featureId, "productFeatureApplTypeId","STANDARD_FEATURE").cache(true).queryList();
                    if (UtilValidate.isEmpty(pAppls)) {
                        continue nextProd;
                    }
                }
                productFound = true;
                variantProductId = productAssoc.getString("productIdTo");
                break;
            }

            /**
             * 1. variant not found so create new variant product and use the virtual product as basis, new one  is a variant type and not a virtual type.
             *    adjust the prices according the selected features
             */
            if (!productFound) {
                // copy product to be variant
                GenericValue product = EntityQuery.use(delegator).from("Product").where("productId",  productId).queryOne();
                product.put("isVariant", "Y");
                product.put("isVirtual", "N");
                product.put("productId", delegator.getNextSeqId("Product"));
                product.remove("virtualVariantMethodEnum"); // not relevant for a non virtual product.
                product.create();
                // add the selected/standard features as 'standard features' to the 'ProductFeatureAppl' table
                GenericValue productFeatureAppl = delegator.makeValue("ProductFeatureAppl",
                        UtilMisc.toMap("productId", product.getString("productId"), "productFeatureApplTypeId", "STANDARD_FEATURE"));
                productFeatureAppl.put("fromDate", UtilDateTime.nowTimestamp());
                for (String productFeatureId: selectedFeatures) {
                    productFeatureAppl.put("productFeatureId",  productFeatureId);
                    productFeatureAppl.create();
                }
                //add standard features too
                List<GenericValue> stdFeaturesAppls = EntityQuery.use(delegator).from("ProductFeatureAppl").where("productId", productId, "productFeatureApplTypeId", "STANDARD_FEATURE").filterByDate().queryList();
                for (GenericValue stdFeaturesAppl: stdFeaturesAppls) {
                    stdFeaturesAppl.put("productId",  product.getString("productId"));
                    stdFeaturesAppl.create();
                }
                /* 3. use the price of the virtual product(Entity:ProductPrice) as a basis and adjust according the prices in the feature price table.
                 *  take the default price from the vitual product, go to the productfeature table and retrieve all the prices for the difFerent features
                 *  add these to the price of the virtual product, store the result as the default price on the variant you created.
                 */
                List<GenericValue> productPrices = EntityQuery.use(delegator).from("ProductPrice").where("productId", productId).filterByDate().queryList();
                for (GenericValue productPrice: productPrices) {
                    for (String selectedFeaturedId: selectedFeatures) {
                        List<GenericValue> productFeaturePrices = EntityQuery.use(delegator).from("ProductFeaturePrice")
                                .where("productFeatureId", selectedFeaturedId, "productPriceTypeId", productPrice.getString("productPriceTypeId"))
                                .filterByDate().queryList();
                        if (UtilValidate.isNotEmpty(productFeaturePrices)) {
                            GenericValue productFeaturePrice = productFeaturePrices.get(0);
                            if (productFeaturePrice != null) {
                                productPrice.put("price", productPrice.getBigDecimal("price").add(productFeaturePrice.getBigDecimal("price")));
                            }
                        }
                    }
                    if (productPrice.get("price") == null) {
                        productPrice.put("price", productPrice.getBigDecimal("price"));
                    }
                    productPrice.put("productId",  product.getString("productId"));
                    productPrice.create();
                }
                // add the product association
                GenericValue productAssoc = delegator.makeValue("ProductAssoc", UtilMisc.toMap("productId", productId, "productIdTo", product.getString("productId"), "productAssocTypeId", "PRODUCT_VARIANT"));
                productAssoc.put("fromDate", UtilDateTime.nowTimestamp());
                productAssoc.create();
                Debug.logInfo("set the productId to: " + product.getString("productId"), module);

                // copy the supplier
                List<GenericValue> supplierProducts = EntityQuery.use(delegator).from("SupplierProduct").where("productId", productId).cache(true).queryList();
                for (GenericValue supplierProduct: supplierProducts) {
                    supplierProduct = (GenericValue) supplierProduct.clone();
                    supplierProduct.set("productId",  product.getString("productId"));
                    supplierProduct.create();
                }

                // copy the content
                List<GenericValue> productContents = EntityQuery.use(delegator).from("ProductContent").where("productId", productId).cache(true).queryList();
                for (GenericValue productContent: productContents) {
                    productContent = (GenericValue) productContent.clone();
                    productContent.set("productId",  product.getString("productId"));
                    productContent.create();
                }

                // finally use the new productId to be added to the cart
                variantProductId = product.getString("productId"); // set to the new product
            }

        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }

        return variantProductId;
    }

    public static boolean isAlternativePacking(Delegator delegator, String productId, String virtualVariantId) {
        boolean isAlternativePacking = false;
        if(productId != null || virtualVariantId != null){
            List<GenericValue> alternativePackingProds = null;
            try {
                List<EntityCondition> condList = new LinkedList<>();

                if (UtilValidate.isNotEmpty(productId)) {
                    condList.add(EntityCondition.makeCondition("productIdTo", productId));
                }
                if (UtilValidate.isNotEmpty(virtualVariantId)) {
                    condList.add(EntityCondition.makeCondition("productId", virtualVariantId));
                }
                condList.add(EntityCondition.makeCondition("productAssocTypeId", "ALTERNATIVE_PACKAGE"));
                alternativePackingProds = EntityQuery.use(delegator).from("ProductAssoc").where(condList).cache(true).queryList();
                if(UtilValidate.isNotEmpty(alternativePackingProds)) {
                    isAlternativePacking = true;
                }
            } catch (GenericEntityException e) {
                Debug.logWarning(e, "Could not found alternative product: " + e.getMessage(), module);
            }
        }
        return isAlternativePacking;
    }

    public static String getOriginalProductId(Delegator delegator, String productId){
        boolean isAlternativePacking = isAlternativePacking(delegator, null, productId);
        if (isAlternativePacking) {
            List<GenericValue> productAssocs = null;
            try {
                productAssocs = EntityQuery.use(delegator).from("ProductAssoc").where("productId", productId , "productAssocTypeId", "ALTERNATIVE_PACKAGE").filterByDate().queryList();
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }

            if (productAssocs != null) {
                GenericValue productAssoc = EntityUtil.getFirst(productAssocs);
                return productAssoc.getString("productIdTo");
            }
            return null;
        }
        return null;

    }
    /**
     * worker to test if product can be order with a decimal quantity
     * @param delegator : access to DB
     * @param productId : ref. of product
     * * @param productStoreId : ref. of store
     * @return true if it can be ordered by decimal quantity
     * @throws GenericEntityException to catch
     */
    public static Boolean isDecimalQuantityOrderAllowed(Delegator delegator, String productId, String productStoreId) throws GenericEntityException{
        //sometime productStoreId may be null (ie PO), then return default value which is TRUE
        if(UtilValidate.isEmpty(productStoreId)){
            return Boolean.TRUE;
        }
        String allowDecimalStore = EntityQuery.use(delegator).from("ProductStore").where("productStoreId", productStoreId).cache(true).queryOne().getString("orderDecimalQuantity");
        String allowDecimalProduct = EntityQuery.use(delegator).from("Product").where("productId", productId).cache(true).queryOne().getString("orderDecimalQuantity");

        if("N".equals(allowDecimalProduct) || (UtilValidate.isEmpty(allowDecimalProduct) && "N".equals(allowDecimalStore))){
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    public static boolean isAggregateService(Delegator delegator, String aggregatedProductId) {
        try {
            GenericValue aggregatedProduct = EntityQuery.use(delegator).from("Product").where("productId", aggregatedProductId).cache().queryOne();
            if (UtilValidate.isNotEmpty(aggregatedProduct) && "AGGREGATED_SERVICE".equals(aggregatedProduct.getString("productTypeId"))) {
                return true;
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
        }

        return false;
    }

    // Method to filter-out out of stock products
    public static List<GenericValue> filterOutOfStockProducts (List<GenericValue> productsToFilter, LocalDispatcher dispatcher, Delegator delegator) throws GeneralException {
        List<GenericValue> productsInStock = new ArrayList<>();
        if (UtilValidate.isNotEmpty(productsToFilter)) {
            for (GenericValue genericRecord : productsToFilter) {
                String productId = genericRecord.getString("productId");
                GenericValue product = null;
                product = EntityQuery.use(delegator).from("Product").where("productId", productId).cache(true).queryOne();
                Boolean isMarketingPackage = EntityTypeUtil.hasParentType(delegator, "ProductType", "productTypeId", product.getString("productTypeId"), "parentTypeId", "MARKETING_PKG");

                if ( UtilValidate.isNotEmpty(isMarketingPackage) && isMarketingPackage) {
                    Map<String, Object> resultOutput = new HashMap<>();
                    resultOutput = dispatcher.runSync("getMktgPackagesAvailable", UtilMisc.toMap("productId" ,productId));
                    Debug.logWarning("Error getting available marketing package.", module);

                    BigDecimal availableInventory = (BigDecimal) resultOutput.get("availableToPromiseTotal");
                    if(availableInventory.compareTo(BigDecimal.ZERO) > 0) {
                        productsInStock.add(genericRecord);
                    }
                } else {
                    List<GenericValue> facilities = EntityQuery.use(delegator).from("ProductFacility").where("productId", productId).queryList();
                    BigDecimal availableInventory = BigDecimal.ZERO;
                    if (UtilValidate.isNotEmpty(facilities)) {
                        for (GenericValue facility : facilities) {
                            BigDecimal lastInventoryCount = facility.getBigDecimal("lastInventoryCount");
                            if (lastInventoryCount != null) {
                                availableInventory = lastInventoryCount.add(availableInventory);
                            }
                        }
                        if (availableInventory.compareTo(BigDecimal.ZERO) > 0) {
                            productsInStock.add(genericRecord);
                        }
                    }
                }
            }
        }
        return productsInStock;
    }
}
