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
package org.apache.ofbiz.product.store;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.common.geo.GeoWorker;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.party.contact.ContactMechWorker;
import org.apache.ofbiz.product.config.ProductConfigWrapper;
import org.apache.ofbiz.product.product.ProductWorker;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.webapp.website.WebSiteWorker;

/**
 * ProductStoreWorker - Worker class for store related functionality
 */
public final class ProductStoreWorker {

    public static final String module = ProductStoreWorker.class.getName();
    private static Map<String, String> defaultProductStoreEmailScreenLocation = new HashMap<String, String>();

    static {
        defaultProductStoreEmailScreenLocation.put("PRDS_ODR_CONFIRM", "component://ecommerce/widget/EmailOrderScreens.xml#OrderConfirmNotice");
        defaultProductStoreEmailScreenLocation.put("PRDS_ODR_COMPLETE", "component://ecommerce/widget/EmailOrderScreens.xml#OrderCompleteNotice");
        defaultProductStoreEmailScreenLocation.put("PRDS_ODR_BACKORDER", "component://ecommerce/widget/EmailOrderScreens.xml#BackorderNotice");
        defaultProductStoreEmailScreenLocation.put("PRDS_ODR_CHANGE", "component://ecommerce/widget/EmailOrderScreens.xml#OrderChangeNotice");

        defaultProductStoreEmailScreenLocation.put("PRDS_ODR_PAYRETRY", "component://ecommerce/widget/EmailOrderScreens.xml#PaymentRetryNotice");

        defaultProductStoreEmailScreenLocation.put("PRDS_RTN_ACCEPT", "component://ecommerce/widget/EmailReturnScreens.xml#ReturnAccept");
        defaultProductStoreEmailScreenLocation.put("PRDS_RTN_COMPLETE", "component://ecommerce/widget/EmailReturnScreens.xml#ReturnComplete");
        defaultProductStoreEmailScreenLocation.put("PRDS_RTN_CANCEL", "component://ecommerce/widget/EmailReturnScreens.xml#ReturnCancel");

        defaultProductStoreEmailScreenLocation.put("PRDS_GC_PURCHASE", "component://ecommerce/widget/EmailGiftCardScreens.xml#GiftCardPurchase");
        defaultProductStoreEmailScreenLocation.put("PRDS_GC_RELOAD", "component://ecommerce/widget/EmailGiftCardScreens.xml#GiftCardReload");

        defaultProductStoreEmailScreenLocation.put("PRDS_QUO_CONFIRM", "component://order/widget/ordermgr/QuoteScreens.xml#ViewQuoteSimple");

        defaultProductStoreEmailScreenLocation.put("PRDS_PWD_RETRIEVE", "component://securityext/widget/EmailSecurityScreens.xml#PasswordEmail");

        defaultProductStoreEmailScreenLocation.put("PRDS_TELL_FRIEND", "component://ecommerce/widget/EmailProductScreens.xml#TellFriend");

        defaultProductStoreEmailScreenLocation.put("PRDS_CUST_REGISTER", "component://securityext/widget/EmailSecurityScreens.xml#PasswordEmail");
    }

    private ProductStoreWorker() {}

    public static GenericValue getProductStore(String productStoreId, Delegator delegator) {
        if (productStoreId == null || delegator == null) {
            return null;
        }
        GenericValue productStore = null;
        try {
            productStore = EntityQuery.use(delegator).from("ProductStore").where("productStoreId", productStoreId).cache().queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem getting ProductStore entity", module);
        }
        return productStore;
    }

    public static GenericValue getProductStore(ServletRequest request) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        String productStoreId = ProductStoreWorker.getProductStoreId(request);
        return ProductStoreWorker.getProductStore(productStoreId, delegator);
    }

    public static String getProductStoreId(ServletRequest request) {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpSession session = httpRequest.getSession(false);
        if (session != null && session.getAttribute("productStoreId") != null) {
            return (String) session.getAttribute("productStoreId");
        } else {
            GenericValue webSite = WebSiteWorker.getWebSite(httpRequest);
            if (webSite != null) {
                String productStoreId = webSite.getString("productStoreId");
                // might be nice to do this, but not needed and has a problem with dependencies: setSessionProductStore(productStoreId, httpRequest);
                return productStoreId;
            }
        }
        return null;
    }

    public static String getStoreCurrencyUomId(HttpServletRequest request) {
        GenericValue productStore = getProductStore(request);
        if (UtilValidate.isEmpty(productStore)) {
            Debug.logError(
                    "No product store found in request, cannot set CurrencyUomId!", module);
            return null;
        } else {
            return UtilHttp.getCurrencyUom(request.getSession(), productStore.getString("defaultCurrencyUomId"));
        }
    }

    public static Locale getStoreLocale(HttpServletRequest request) {
        GenericValue productStore = getProductStore(request);
        if (UtilValidate.isEmpty(productStore)) {
            Debug.logError(
                    "No product store found in request, cannot set locale!", module);
            return null;
        } else {
            return UtilHttp.getLocale(request, request.getSession(), productStore.getString("defaultLocaleString"));
        }
    }
    
    public static TimeZone getStoreTimeZone(HttpServletRequest request) {
        GenericValue productStore = getProductStore(request);
        if (UtilValidate.isEmpty(productStore)) {
            Debug.logError("No product store found in request, cannot set timezone!", module);
            return null;
        } else {
            return UtilHttp.getTimeZone(request, request.getSession(), productStore.getString("defaultTimeZoneString"));
        }
    }

    public static String determineSingleFacilityForStore(Delegator delegator, String productStoreId) {
        GenericValue productStore = null;
        try {
            productStore = EntityQuery.use(delegator).from("ProductStore").where("productStoreId", productStoreId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        if (productStore != null) {
            if ("Y".equalsIgnoreCase(productStore.getString("oneInventoryFacility"))) {
                return productStore.getString("inventoryFacilityId");
            }
        }
        return null;
    }

    public static boolean autoSaveCart(Delegator delegator, String productStoreId) {
        return autoSaveCart(getProductStore(productStoreId, delegator));
    }

    public static boolean autoSaveCart(GenericValue productStore) {
        return productStore == null ? false : "Y".equalsIgnoreCase(productStore.getString("autoSaveCart"));
    }

    public static String getProductStorePayToPartyId(String productStoreId, Delegator delegator) {
        return getProductStorePayToPartyId(getProductStore(productStoreId, delegator));
    }

    public static String getProductStorePayToPartyId(GenericValue productStore) {
        String payToPartyId = "Company"; // default value
        if (productStore != null && productStore.get("payToPartyId") != null) {
            payToPartyId = productStore.getString("payToPartyId");
        }
        return payToPartyId;
    }

    public static String getProductStorePaymentProperties(ServletRequest request, String paymentMethodTypeId, String paymentServiceTypeEnumId, boolean anyServiceType) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        String productStoreId = ProductStoreWorker.getProductStoreId(request);
        return ProductStoreWorker.getProductStorePaymentProperties(delegator, productStoreId, paymentMethodTypeId, paymentServiceTypeEnumId, anyServiceType);
    }

    public static String getProductStorePaymentProperties(Delegator delegator, String productStoreId, String paymentMethodTypeId, String paymentServiceTypeEnumId, boolean anyServiceType) {
        GenericValue setting = ProductStoreWorker.getProductStorePaymentSetting(delegator, productStoreId, paymentMethodTypeId, paymentServiceTypeEnumId, anyServiceType);

        String payProps = "payment.properties";
        if (setting != null && setting.get("paymentPropertiesPath") != null) {
            payProps =  setting.getString("paymentPropertiesPath");
        }
        return payProps;
    }

    public static GenericValue getProductStorePaymentSetting(Delegator delegator, String productStoreId, String paymentMethodTypeId, String paymentServiceTypeEnumId, boolean anyServiceType) {
        GenericValue storePayment = null;
        try {
            storePayment = EntityQuery.use(delegator).from("ProductStorePaymentSetting").where("productStoreId", productStoreId, "paymentMethodTypeId", paymentMethodTypeId, "paymentServiceTypeEnumId", paymentServiceTypeEnumId).cache().queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problems looking up store payment settings", module);
        }

        if (anyServiceType) {
            if (storePayment == null) {
                try {
                    storePayment = EntityQuery.use(delegator).from("ProductStorePaymentSetting").where("productStoreId", productStoreId, "paymentMethodTypeId", paymentMethodTypeId).queryFirst();
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Problems looking up store payment settings", module);
                }
            }

            if (storePayment == null) {
                try {
                    storePayment = EntityQuery.use(delegator).from("ProductStorePaymentSetting").where("productStoreId", productStoreId).queryFirst();
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Problems looking up store payment settings", module);
                }
            }
        }

        return storePayment;
    }

    public static List<GenericValue> getProductStoreShipmentMethods(Delegator delegator, String productStoreId,
                                                             String shipmentMethodTypeId, String carrierPartyId, String carrierRoleTypeId) {
        // check for an external service call

        List<GenericValue> storeShipMethods = null;
        try {
            storeShipMethods = EntityQuery.use(delegator).from("ProductStoreShipmentMeth")
                                   .where("productStoreId", productStoreId, "shipmentMethodTypeId", shipmentMethodTypeId,
                                           "partyId", carrierPartyId, "roleTypeId", carrierRoleTypeId)
                                   .cache(true)
                                   .queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }

        return storeShipMethods;
    }

    public static GenericValue getProductStoreShipmentMethod(Delegator delegator, String productStoreId,
                                                             String shipmentMethodTypeId, String carrierPartyId, String carrierRoleTypeId) {
        // TODO: selecting the first record is a far from optimal solution but, since the productStoreShipmentMethod
        //       is currently used to get the service name to get the online estimate, this should not be a huge deal for now.
        return EntityUtil.getFirst(getProductStoreShipmentMethods(delegator, productStoreId, shipmentMethodTypeId, carrierPartyId, carrierRoleTypeId));
    }

    public static List<GenericValue> getAvailableStoreShippingMethods(Delegator delegator, String productStoreId, GenericValue shippingAddress, List<BigDecimal> itemSizes, Map<String, BigDecimal> featureIdMap, BigDecimal weight, BigDecimal orderTotal) {
        if (featureIdMap == null) {
            featureIdMap = new HashMap<String, BigDecimal>();
        }
        List<GenericValue> shippingMethods = null;
        try {
            shippingMethods = EntityQuery.use(delegator).from("ProductStoreShipmentMethView").where("productStoreId", productStoreId).orderBy("sequenceNumber").cache(true).queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to get ProductStore shipping methods", module);
            return null;
        }

        // clone the list for concurrent modification
        List<GenericValue> returnShippingMethods = UtilMisc.makeListWritable(shippingMethods);

        if (shippingMethods != null) {
            for (GenericValue method: shippingMethods) {

                // test min/max weight first
                BigDecimal minWeight = method.getBigDecimal("minWeight");
                BigDecimal maxWeight = method.getBigDecimal("maxWeight");
                if (minWeight != null && minWeight.compareTo(BigDecimal.ZERO) > 0 && minWeight.compareTo(weight) > 0) {
                    returnShippingMethods.remove(method);
                    continue;
                }
                if (maxWeight != null && maxWeight.compareTo(BigDecimal.ZERO) > 0 && maxWeight.compareTo(weight) < 0) {
                    returnShippingMethods.remove(method);
                    continue;
                }

                // test order total
                BigDecimal minTotal = method.getBigDecimal("minTotal");
                BigDecimal maxTotal = method.getBigDecimal("maxTotal");
                if (minTotal != null && minTotal.compareTo(BigDecimal.ZERO) > 0 && minTotal.compareTo(orderTotal) > 0) {
                    returnShippingMethods.remove(method);
                    continue;
                }
                if (maxTotal != null && maxTotal.compareTo(BigDecimal.ZERO) > 0 && maxTotal.compareTo(orderTotal) < 0) {
                    returnShippingMethods.remove(method);
                    continue;
                }

                // test product sizes
                BigDecimal minSize = method.getBigDecimal("minSize");
                BigDecimal maxSize = method.getBigDecimal("maxSize");
                if (minSize != null && minSize.compareTo(BigDecimal.ZERO) > 0) {
                    boolean allMatch = false;
                    if (itemSizes != null) {
                        allMatch = true;
                        for (BigDecimal size: itemSizes) {
                            if (size.compareTo(minSize) < 0) {
                                allMatch = false;
                            }
                        }
                    }
                    if (!allMatch) {
                        returnShippingMethods.remove(method);
                        continue;
                    }
                }
                if (maxSize != null && maxSize.compareTo(BigDecimal.ZERO) > 0) {
                    boolean allMatch = false;
                    if (itemSizes != null) {
                        allMatch = true;
                        for (BigDecimal size: itemSizes) {
                            if (size.compareTo(maxSize) > 0) {
                                allMatch = false;
                            }
                        }
                    }
                    if (!allMatch) {
                        returnShippingMethods.remove(method);
                        continue;
                    }
                }

                // check USPS address
                String allowUspsAddr = method.getString("allowUspsAddr");
                String requireUspsAddr = method.getString("requireUspsAddr");
                boolean isUspsAddress = ContactMechWorker.isUspsAddress(shippingAddress);
                if ("N".equals(allowUspsAddr) && isUspsAddress) {
                    returnShippingMethods.remove(method);
                    continue;
                }
                if ("Y".equals(requireUspsAddr) && !isUspsAddress) {
                    returnShippingMethods.remove(method);
                    continue;
                }

                // check company address
                String companyPartyId = method.getString("companyPartyId");
                String allowCompanyAddr = method.getString("allowCompanyAddr");
                String requireCompanyAddr = method.getString("requireCompanyAddr");
                boolean isCompanyAddress = ContactMechWorker.isCompanyAddress(shippingAddress, companyPartyId);
                if ("N".equals(allowCompanyAddr) && isCompanyAddress) {
                    returnShippingMethods.remove(method);
                    continue;
                }
                if ("Y".equals(requireCompanyAddr) && !isCompanyAddress) {
                    returnShippingMethods.remove(method);
                    continue;
                }

                // check the items excluded from shipping
                String includeFreeShipping = method.getString("includeNoChargeItems");
                if (includeFreeShipping != null && "N".equalsIgnoreCase(includeFreeShipping)) {
                    if (UtilValidate.isEmpty(itemSizes) && orderTotal.compareTo(BigDecimal.ZERO) == 0) {
                        returnShippingMethods.remove(method);
                        continue;
                    }
                }

                // check the geos
                String includeGeoId = method.getString("includeGeoId");
                String excludeGeoId = method.getString("excludeGeoId");
                if (UtilValidate.isNotEmpty(includeGeoId) || UtilValidate.isNotEmpty(excludeGeoId)) {
                    if (shippingAddress == null) {
                        returnShippingMethods.remove(method);
                        continue;
                    }
                }
                if (UtilValidate.isNotEmpty(includeGeoId)) {
                    List<GenericValue> includeGeoGroup = GeoWorker.expandGeoGroup(includeGeoId, delegator);
                    if (!GeoWorker.containsGeo(includeGeoGroup, shippingAddress.getString("countryGeoId"), delegator) &&
                            !GeoWorker.containsGeo(includeGeoGroup, shippingAddress.getString("stateProvinceGeoId"), delegator) &&
                            !GeoWorker.containsGeo(includeGeoGroup, shippingAddress.getString("postalCodeGeoId"), delegator)) {
                        // not in required included geos
                        returnShippingMethods.remove(method);
                        continue;
                    }
                }
                if (UtilValidate.isNotEmpty(excludeGeoId)) {
                    List<GenericValue> excludeGeoGroup = GeoWorker.expandGeoGroup(excludeGeoId, delegator);
                    if (GeoWorker.containsGeo(excludeGeoGroup, shippingAddress.getString("countryGeoId"), delegator) ||
                            GeoWorker.containsGeo(excludeGeoGroup, shippingAddress.getString("stateProvinceGeoId"), delegator) ||
                            GeoWorker.containsGeo(excludeGeoGroup, shippingAddress.getString("postalCodeGeoId"), delegator)) {
                        // in excluded geos
                        returnShippingMethods.remove(method);
                        continue;
                    }
                }

                // check the features
                String includeFeatures = method.getString("includeFeatureGroup");
                String excludeFeatures = method.getString("excludeFeatureGroup");
                if (UtilValidate.isNotEmpty(includeFeatures)) {
                    List<GenericValue> includedFeatures = null;
                    try {
                        includedFeatures = EntityQuery.use(delegator).from("ProductFeatureGroupAppl").where("productFeatureGroupId", includeFeatures).cache(true).queryList();
                    } catch (GenericEntityException e) {
                        Debug.logError(e, "Unable to lookup ProductFeatureGroupAppl records for group : " + includeFeatures, module);
                    }
                    if (includedFeatures != null) {
                        boolean foundOne = false;
                        for (GenericValue appl: includedFeatures) {
                            if (featureIdMap.containsKey(appl.getString("productFeatureId"))) {
                                foundOne = true;
                                break;
                            }
                        }
                        if (!foundOne) {
                            returnShippingMethods.remove(method);
                            continue;
                        }
                    }
                }
                if (UtilValidate.isNotEmpty(excludeFeatures)) {
                    List<GenericValue> excludedFeatures = null;
                    try {
                        excludedFeatures = EntityQuery.use(delegator).from("ProductFeatureGroupAppl").where("productFeatureGroupId", excludeFeatures).cache(true).queryList();
                    } catch (GenericEntityException e) {
                        Debug.logError(e, "Unable to lookup ProductFeatureGroupAppl records for group : " + excludeFeatures, module);
                    }
                    if (excludedFeatures != null) {
                        for (GenericValue appl: excludedFeatures) {
                            if (featureIdMap.containsKey(appl.getString("productFeatureId"))) {
                                returnShippingMethods.remove(method);
                                continue;
                            }
                        }
                    }
                }
            }
        }

        return returnShippingMethods;
    }

    public static ProductStoreSurveyWrapper getRandomSurveyWrapper(HttpServletRequest request, String groupName) {
        GenericValue productStore = getProductStore(request);
        HttpSession session = request.getSession();
        if (productStore == null) {
            return null;
        }

        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        if (userLogin == null) {
            userLogin = (GenericValue) session.getAttribute("autoUserLogin");
        }

        String partyId = userLogin != null ? userLogin.getString("partyId") : null;
        String origParamMapId = UtilHttp.stashParameterMap(request);
        Map<String, Object> passThruFields = UtilMisc.<String, Object>toMap("_ORIG_PARAM_MAP_ID_", origParamMapId);

        return getRandomSurveyWrapper(productStore.getDelegator(), productStore.getString("productStoreId"), groupName, partyId, passThruFields);
    }

    public static ProductStoreSurveyWrapper getRandomSurveyWrapper(Delegator delegator, String productStoreId, String groupName, String partyId, Map<String, Object> passThruFields) {
        List<GenericValue> randomSurveys = getSurveys(delegator, productStoreId, groupName, null, "RANDOM_POLL", null);
        if (UtilValidate.isNotEmpty(randomSurveys)) {
            Random rand = new Random();
            int index = rand.nextInt(randomSurveys.size());
            GenericValue appl = randomSurveys.get(index);
            return new ProductStoreSurveyWrapper(appl, partyId, passThruFields);
        } else {
            return null;
        }
    }

    public static List<GenericValue> getProductSurveys(Delegator delegator, String productStoreId, String productId, String surveyApplTypeId) {
        return getSurveys(delegator, productStoreId, null, productId, surveyApplTypeId, null);
    }

    public static List<GenericValue> getProductSurveys(Delegator delegator, String productStoreId, String productId, String surveyApplTypeId, String parentProductId) {
        return getSurveys(delegator, productStoreId, null, productId, surveyApplTypeId,parentProductId);
    }

    public static List<GenericValue> getSurveys(Delegator delegator, String productStoreId, String groupName, String productId, String surveyApplTypeId, String parentProductId) {
        List<GenericValue> surveys = new LinkedList<GenericValue>();
        List<GenericValue> storeSurveys = null;
        try {
            storeSurveys = EntityQuery.use(delegator).from("ProductStoreSurveyAppl").where("productStoreId", productStoreId, "surveyApplTypeId", surveyApplTypeId).orderBy("sequenceNum").cache(true).queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to get ProductStoreSurveyAppl for store : " + productStoreId, module);
            return surveys;
        }

        // limit by date
        storeSurveys = EntityUtil.filterByDate(storeSurveys);

        // limit based on group name
        if (UtilValidate.isNotEmpty(groupName)) {
            storeSurveys = EntityUtil.filterByAnd(storeSurveys, UtilMisc.toMap("groupName", groupName));
        }

         Debug.logInfo("getSurvey for product " + productId,module);
        // limit by product
        if (UtilValidate.isNotEmpty(productId) && UtilValidate.isNotEmpty(storeSurveys)) {
            for (GenericValue surveyAppl: storeSurveys) {
                GenericValue product = null;
                String virtualProductId = null;

                // if the item is a variant, get its virtual productId
                try {
                    product = EntityQuery.use(delegator).from("Product").where("productId", productId).cache().queryOne();
                    if ((product != null) && ("Y".equals(product.get("isVariant")))) {
                        if (parentProductId != null) {
                            virtualProductId = parentProductId;
                        }
                        else {
                            virtualProductId = ProductWorker.getVariantVirtualId(product);
                        }
                        Debug.logInfo("getSurvey for virtual product " + virtualProductId,module);
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Problem finding product from productId " + productId, module);
                }

                // use survey if productId or virtualProductId of the variant product is in the ProductStoreSurveyAppl
                if (surveyAppl.get("productId") != null) {
                    if (surveyAppl.get("productId").equals(productId)) {
                        surveys.add(surveyAppl);
                    } else if ((virtualProductId != null) && (surveyAppl.getString("productId").equals(virtualProductId))) {
                        surveys.add(surveyAppl);
                    }
                } else if (surveyAppl.get("productCategoryId") != null) {
                    List<GenericValue> categoryMembers = null;
                    try {
                        categoryMembers = EntityQuery.use(delegator).from("ProductCategoryMember").where("productCategoryId", surveyAppl.get("productCategoryId")).cache(true).queryList();
                    } catch (GenericEntityException e) {
                        Debug.logError(e, "Unable to get ProductCategoryMember records for survey application : " + surveyAppl, module);
                    }
                    if (categoryMembers != null) {
                        for (GenericValue member: categoryMembers) {
                            if (productId != null && productId.equals(member.getString("productId"))) {
                                surveys.add(surveyAppl);
                                break;
                            } else if ((virtualProductId != null) && (virtualProductId.equals(member.getString("productId")))) { // similarly, check if virtual productId is in category
                                surveys.add(surveyAppl);
                                break;
                            }
                        }
                    }
                }
            }
        } else if (storeSurveys != null) {
            surveys.addAll(storeSurveys);
        }

        return surveys;
    }

    /** Returns the number of responses for this survey by party */
    public static int checkSurveyResponse(HttpServletRequest request, String surveyId) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
        String productStoreId = getProductStoreId(request);
        if (userLogin == null) {
            return -1;
        }

        return checkSurveyResponse(delegator, userLogin.getString("partyId"), productStoreId, surveyId);
    }

    /** Returns the number of responses for this survey by party */
    public static int checkSurveyResponse(Delegator delegator, String partyId, String productStoreId, String surveyId) {
        if (delegator == null || partyId == null || productStoreId == null) {
            return -1;
        }

        List<GenericValue> surveyResponse = null;
        try {
            surveyResponse = EntityQuery.use(delegator).from("SurveyResponse").where("surveyId", surveyId, "partyId", partyId).queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return -1;
        }

        if (UtilValidate.isEmpty(surveyResponse)) {
            return 0;
        } else {
            return surveyResponse.size();
        }
    }

    public static boolean isStoreInventoryRequired(ServletRequest request, GenericValue product) {
        return isStoreInventoryRequiredAndAvailable(request, product, null, Boolean.TRUE, null);
    }

    public static boolean isStoreInventoryAvailable(ServletRequest request, GenericValue product, BigDecimal quantity) {
        return isStoreInventoryRequiredAndAvailable(request, product, quantity, null, Boolean.TRUE);
    }

    /**
     * This method is used in the showcart pages to determine whether or not to show the inventory message and
     * in the productdetail pages to determine whether or not to show the item as out of stock.
     *
     * @param request ServletRequest (or HttpServletRequest of course)
     * @param product GenericValue representing the product in question
     * @param quantity Quantity desired.
     * @param wantRequired If true then inventory required must be true for the result to be true, if false must be false; if null don't care
     * @param wantAvailable If true then inventory avilable must be true for the result to be true, if false must be false; if null don't care
     */
    public static boolean isStoreInventoryRequiredAndAvailable(ServletRequest request, GenericValue product, BigDecimal quantity, Boolean wantRequired, Boolean wantAvailable) {
        GenericValue productStore = getProductStore(request);
        if (productStore == null) {
            Debug.logWarning("No ProductStore found, return false for inventory check", module);
            return false;
        }
        if (product == null) {
            Debug.logWarning("No Product passed, return false for inventory check", module);
            return false;
        }

        if (quantity == null) quantity = BigDecimal.ONE;

        String productStoreId = productStore.getString("productStoreId");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");

        try {
            Boolean requiredOkay = null;
            if (wantRequired != null) {
                Map<String, Object> invReqResult = dispatcher.runSync("isStoreInventoryRequired", UtilMisc.toMap("productStoreId", productStoreId, "productId", product.get("productId"), "product", product, "productStore", productStore));
                if (ServiceUtil.isError(invReqResult)) {
                    Debug.logError("Error calling isStoreInventoryRequired service, result is: " + invReqResult, module);
                    return false;
                }
                requiredOkay = Boolean.valueOf(wantRequired.booleanValue() == "Y".equals(invReqResult.get("requireInventory")));
            }

            Boolean availableOkay = null;
            if (wantAvailable != null) {
                Map<String, Object> invAvailResult = dispatcher.runSync("isStoreInventoryAvailable", UtilMisc.toMap("productStoreId", productStoreId, "productId", product.get("productId"), "product", product, "productStore", productStore, "quantity", quantity));
                if (ServiceUtil.isError(invAvailResult)) {
                    Debug.logError("Error calling isStoreInventoryAvailable service, result is: " + invAvailResult, module);
                    return false;
                }
                availableOkay = Boolean.valueOf(wantAvailable.booleanValue() == "Y".equals(invAvailResult.get("available")));
            }

            if ((requiredOkay == null || requiredOkay.booleanValue()) && (availableOkay == null || availableOkay.booleanValue())) {
                return true;
            } else {
                return false;
            }
        } catch (GenericServiceException e) {
            String errMsg = "Fatal error calling inventory checking services: " + e.toString();
            Debug.logError(e, errMsg, module);
            return false;
        }
    }

    public static boolean isStoreInventoryAvailable(ServletRequest request, ProductConfigWrapper productConfig, BigDecimal quantity) {
        GenericValue productStore = getProductStore(request);

        if (productStore == null) {
            Debug.logWarning("No ProductStore found, return false for inventory check", module);
            return false;
        }

        String productStoreId = productStore.getString("productStoreId");
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        return isStoreInventoryAvailable(productStoreId, productConfig, quantity, delegator, dispatcher);
    }

    /** check inventory availability for the given catalog, product, quantity, etc */
    public static boolean isStoreInventoryAvailable(String productStoreId, ProductConfigWrapper productConfig, BigDecimal quantity, Delegator delegator, LocalDispatcher dispatcher) {
        GenericValue productStore = getProductStore(productStoreId, delegator);

        if (productStore == null) {
            Debug.logWarning("No ProductStore found with id " + productStoreId + ", returning false for inventory available check", module);
            return false;
        }

        // if prodCatalog is set to not check inventory break here
        if ("N".equals(productStore.getString("checkInventory"))) {
            // note: if not set, defaults to yes, check inventory
            if (Debug.verboseOn()) Debug.logVerbose("ProductStore with id " + productStoreId + ", is set to NOT check inventory, returning true for inventory available check", module);
            return true;
        }
        boolean isInventoryAvailable = false;

        if ("Y".equals(productStore.getString("oneInventoryFacility"))) {
            String inventoryFacilityId = productStore.getString("inventoryFacilityId");

            if (UtilValidate.isEmpty(inventoryFacilityId)) {
                Debug.logWarning("ProductStore with id " + productStoreId + " has Y for oneInventoryFacility but inventoryFacilityId is empty, returning false for inventory check", module);
                return false;
            }

            return ProductWorker.isProductInventoryAvailableByFacility(productConfig, inventoryFacilityId, quantity, dispatcher);

        } else {
            GenericValue product = productConfig.getProduct();
            List<GenericValue> productFacilities = null;

            try {
                productFacilities = product.getRelated("ProductFacility", null, null, true);
            } catch (GenericEntityException e) {
                Debug.logWarning(e, "Error invoking getRelated in isCatalogInventoryAvailable", module);
                return false;
            }

            if (UtilValidate.isNotEmpty(productFacilities)) {
                for (GenericValue pfValue: productFacilities) {
                    isInventoryAvailable = ProductWorker.isProductInventoryAvailableByFacility(productConfig, pfValue.getString("facilityId"), quantity, dispatcher);
                    if (isInventoryAvailable == true) {
                        return isInventoryAvailable;
                    }
                }
            }
            return false;
        }
    }

    public static String getDefaultProductStoreEmailScreenLocation(String emailType) {
        return defaultProductStoreEmailScreenLocation.get(emailType);
    }
}
