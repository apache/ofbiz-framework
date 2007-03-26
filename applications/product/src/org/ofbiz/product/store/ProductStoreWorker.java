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
package org.ofbiz.product.store;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.common.geo.GeoWorker;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.party.contact.ContactMechWorker;
import org.ofbiz.product.catalog.CatalogWorker;
import org.ofbiz.product.config.ProductConfigWrapper;
import org.ofbiz.product.product.ProductWorker;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

/**
 * ProductStoreWorker - Worker class for store related functionality
 */
public class ProductStoreWorker {

    public static final String module = ProductStoreWorker.class.getName();

    public static GenericValue getProductStore(String productStoreId, GenericDelegator delegator) {
        if (productStoreId == null || delegator == null) {
            return null;
        }
        GenericValue productStore = null;
        try {
            productStore = delegator.findByPrimaryKeyCache("ProductStore", UtilMisc.toMap("productStoreId", productStoreId));
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem getting ProductStore entity", module);
        }
        return productStore;
    }

    public static GenericValue getProductStore(ServletRequest request) {
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        String productStoreId = ProductStoreWorker.getProductStoreId(request);
        return ProductStoreWorker.getProductStore(productStoreId, delegator);
    }

    public static String getProductStoreId(ServletRequest request) {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpSession session = httpRequest.getSession();
        if (session.getAttribute("productStoreId") != null) {
            return (String) session.getAttribute("productStoreId");
        } else {
            GenericValue webSite = CatalogWorker.getWebSite(request);
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
        return UtilHttp.getCurrencyUom(request.getSession(), productStore.getString("defaultCurrencyUomId"));
    }

    public static Locale getStoreLocale(HttpServletRequest request) {
        GenericValue productStore = getProductStore(request);
        if (UtilValidate.isEmpty(productStore)) {
            Debug.logError("No product store found in request, cannot set locale!", module);
            return null;
	} else {
	    return UtilHttp.getLocale(request, request.getSession(), productStore.getString("defaultLocaleString"));
	}
    }

    public static String determineSingleFacilityForStore(GenericDelegator delegator, String productStoreId) {
        GenericValue productStore = null;
        try {
            productStore = delegator.findByPrimaryKey("ProductStore", UtilMisc.toMap("productStoreId", productStoreId));
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

    public static boolean autoSaveCart(GenericDelegator delegator, String productStoreId) {
        return autoSaveCart(getProductStore(productStoreId, delegator));
    }

    public static boolean autoSaveCart(GenericValue productStore) {
        return productStore == null ? false : "Y".equalsIgnoreCase(productStore.getString("autoSaveCart"));
    }

    public static String getProductStorePayToPartyId(String productStoreId, GenericDelegator delegator) {
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
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        String productStoreId = ProductStoreWorker.getProductStoreId(request);
        return ProductStoreWorker.getProductStorePaymentProperties(delegator, productStoreId, paymentMethodTypeId, paymentServiceTypeEnumId, anyServiceType);
    }

    public static String getProductStorePaymentProperties(GenericDelegator delegator, String productStoreId, String paymentMethodTypeId, String paymentServiceTypeEnumId, boolean anyServiceType) {
        GenericValue setting = ProductStoreWorker.getProductStorePaymentSetting(delegator, productStoreId, paymentMethodTypeId, paymentServiceTypeEnumId, anyServiceType);

        String payProps = "payment.properties";
        if (setting != null && setting.get("paymentPropertiesPath") != null) {
            payProps =  setting.getString("paymentPropertiesPath");
        }
        return payProps;
    }

    public static GenericValue getProductStorePaymentSetting(GenericDelegator delegator, String productStoreId, String paymentMethodTypeId, String paymentServiceTypeEnumId, boolean anyServiceType) {
        GenericValue storePayment = null;
        try {
            storePayment = delegator.findByPrimaryKeyCache("ProductStorePaymentSetting", UtilMisc.toMap("productStoreId", productStoreId, "paymentMethodTypeId", paymentMethodTypeId, "paymentServiceTypeEnumId", paymentServiceTypeEnumId));
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problems looking up store payment settings", module);
        }

        if (anyServiceType) {
            if (storePayment == null) {
                try {
                    List storePayments = delegator.findByAnd("ProductStorePaymentSetting", UtilMisc.toMap("productStoreId", productStoreId, "paymentMethodTypeId", paymentMethodTypeId));
                    storePayment = EntityUtil.getFirst(storePayments);
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Problems looking up store payment settings", module);
                }
            }

            if (storePayment == null) {
                try {
                    List storePayments = delegator.findByAnd("ProductStorePaymentSetting", UtilMisc.toMap("productStoreId", productStoreId));
                    storePayment = EntityUtil.getFirst(storePayments);
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Problems looking up store payment settings", module);
                }
            }
        }

        return storePayment;
    }

    public static List getProductStoreShipmentMethods(GenericDelegator delegator, String productStoreId,
                                                             String shipmentMethodTypeId, String carrierPartyId, String carrierRoleTypeId) {
        // check for an external service call
        Map storeFields = UtilMisc.toMap("productStoreId", productStoreId, "shipmentMethodTypeId", shipmentMethodTypeId,
                "partyId", carrierPartyId, "roleTypeId", carrierRoleTypeId);

        List storeShipMethods = null;
        try {
            storeShipMethods = delegator.findByAndCache("ProductStoreShipmentMeth", storeFields);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }

        return storeShipMethods;
    }

    public static GenericValue getProductStoreShipmentMethod(GenericDelegator delegator, String productStoreId,
                                                             String shipmentMethodTypeId, String carrierPartyId, String carrierRoleTypeId) {
        // TODO: selecting the first record is a far from optimal solution but, since the productStoreShipmentMethod
        //       is currently used to get the service name to get the online estimate, this should not be a huge deal for now.
        return EntityUtil.getFirst(getProductStoreShipmentMethods(delegator, productStoreId, shipmentMethodTypeId, carrierPartyId, carrierRoleTypeId));
    }

    public static List getAvailableStoreShippingMethods(GenericDelegator delegator, String productStoreId, GenericValue shippingAddress, List itemSizes, Map featureIdMap, double weight, double orderTotal) {
        if (featureIdMap == null) {
            featureIdMap = new HashMap();
        }
        List shippingMethods = null;
        try {
            shippingMethods = delegator.findByAndCache("ProductStoreShipmentMethView", UtilMisc.toMap("productStoreId", productStoreId), UtilMisc.toList("sequenceNumber"));
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to get ProductStore shipping methods", module);
            return null;
        }

        // clone the list for concurrent modification
        List returnShippingMethods = new LinkedList(shippingMethods);

        if (shippingMethods != null) {
            Iterator i = shippingMethods.iterator();
            while (i.hasNext()) {
                GenericValue method = (GenericValue) i.next();
                //Debug.logInfo("Checking Shipping Method : " + method.getString("shipmentMethodTypeId"), module);

                // test min/max weight first
                Double minWeight = method.getDouble("minWeight");
                Double maxWeight = method.getDouble("maxWeight");
                if (minWeight != null && minWeight.doubleValue() > 0 && minWeight.doubleValue() > weight) {
                    returnShippingMethods.remove(method);
                    //Debug.logInfo("Removed shipping method due to not enough weight", module);
                    continue;
                }
                if (maxWeight != null && maxWeight.doubleValue() > 0 && maxWeight.doubleValue() < weight) {
                    returnShippingMethods.remove(method);
                    //Debug.logInfo("Removed shipping method due to too much weight", module);
                    continue;
                }

                // test order total
                Double minTotal = method.getDouble("minTotal");
                Double maxTotal = method.getDouble("maxTotal");
                if (minTotal != null && minTotal.doubleValue() > 0 && minTotal.doubleValue() > orderTotal) {
                    returnShippingMethods.remove(method);
                    //Debug.logInfo("Removed shipping method due to not enough order total", module);
                    continue;
                }
                if (maxTotal != null && maxTotal.doubleValue() > 0 && maxTotal.doubleValue() < orderTotal) {
                    returnShippingMethods.remove(method);
                    //Debug.logInfo("Removed shipping method due to too much shipping total", module);
                    continue;
                }

                // test product sizes
                Double minSize = method.getDouble("minSize");
                Double maxSize = method.getDouble("maxSize");
                if (minSize != null && minSize.doubleValue() > 0) {
                    boolean allMatch = false;
                    if (itemSizes != null) {
                        allMatch = true;
                        Iterator isi = itemSizes.iterator();
                        while (isi.hasNext()) {
                            Double size = (Double) isi.next();
                            if (size.doubleValue() < minSize.doubleValue()) {
                                allMatch = false;
                            }
                        }
                    }
                    if (!allMatch) {
                        returnShippingMethods.remove(method);
                        //Debug.logInfo("Removed shipping method because not all products are less then min size", module);
                        continue;
                    }
                }
                if (maxSize != null && maxSize.doubleValue() > 0) {
                    boolean allMatch = false;
                    if (itemSizes != null) {
                        allMatch = true;
                        Iterator isi = itemSizes.iterator();
                        while (isi.hasNext()) {
                            Double size = (Double) isi.next();
                            if (size.doubleValue() > maxSize.doubleValue()) {
                                allMatch = false;
                            }
                        }
                    }
                    if (!allMatch) {
                        returnShippingMethods.remove(method);
                        //Debug.logInfo("Removed shipping method because one or more products were more then max size", module);
                        continue;
                    }
                }

                // check USPS address
                String allowUspsAddr = method.getString("allowUspsAddr");
                String requireUspsAddr = method.getString("requireUspsAddr");
                boolean isUspsAddress = ContactMechWorker.isUspsAddress(shippingAddress);
                if ("N".equals(allowUspsAddr) && isUspsAddress) {
                    returnShippingMethods.remove(method);
                    //Debug.logInfo("Remove shipping method due to USPS address", module);
                    continue;
                }
                if ("Y".equals(requireUspsAddr) && !isUspsAddress) {
                    returnShippingMethods.remove(method);
                    //Debug.logInfo("Removed shipping method due to NON-USPS address", module);
                    continue;
                }

                // check company address
                String companyPartyId = method.getString("companyPartyId");
                String allowCompanyAddr = method.getString("allowCompanyAddr");
                String requireCompanyAddr = method.getString("requireCompanyAddr");
                boolean isCompanyAddress = ContactMechWorker.isCompanyAddress(shippingAddress, companyPartyId);
                if ("N".equals(allowCompanyAddr) && isCompanyAddress) {
                    returnShippingMethods.remove(method);
                    //Debug.logInfo("Removed shipping method due to Company address", module);
                    continue;
                }
                if ("Y".equals(requireCompanyAddr) && !isCompanyAddress) {
                    returnShippingMethods.remove(method);
                    //Debug.logInfo("Removed shipping method due to NON-Company address", module);
                    continue;
                }

                // check the items excluded from shipping
                String includeFreeShipping = method.getString("includeNoChargeItems");
                if (includeFreeShipping != null && "N".equalsIgnoreCase(includeFreeShipping)) {
                    if ((itemSizes == null || itemSizes.size() == 0) && orderTotal == 0) {
                        returnShippingMethods.remove(method);
                        //Debug.logInfo("Removed shipping method due to all items being exempt from shipping", module);
                        continue;
                    }
                }

                // check the geos
                String includeGeoId = method.getString("includeGeoId");
                String excludeGeoId = method.getString("excludeGeoId");
                if ((includeGeoId != null && includeGeoId.length() > 0) || (excludeGeoId != null && excludeGeoId.length() > 0)) {
                    if (shippingAddress == null) {
                        returnShippingMethods.remove(method);
                        //Debug.logInfo("Removed shipping method due to empty shipping adresss (may not have been selected yet)", module);
                        continue;
                    }
                }
                if (includeGeoId != null && includeGeoId.length() > 0) {
                    List includeGeoGroup = GeoWorker.expandGeoGroup(includeGeoId, delegator);
                    if (!GeoWorker.containsGeo(includeGeoGroup, shippingAddress.getString("countryGeoId"), delegator) &&
                            !GeoWorker.containsGeo(includeGeoGroup, shippingAddress.getString("stateProvinceGeoId"), delegator) &&
                            !GeoWorker.containsGeo(includeGeoGroup, shippingAddress.getString("postalCodeGeoId"), delegator)) {
                        // not in required included geos
                        returnShippingMethods.remove(method);
                        //Debug.logInfo("Removed shipping method due to being outside the included GEO", module);
                        continue;
                    }
                }
                if (excludeGeoId != null && excludeGeoId.length() > 0) {
                    List excludeGeoGroup = GeoWorker.expandGeoGroup(excludeGeoId, delegator);
                    if (GeoWorker.containsGeo(excludeGeoGroup, shippingAddress.getString("countryGeoId"), delegator) ||
                            GeoWorker.containsGeo(excludeGeoGroup, shippingAddress.getString("stateProvinceGeoId"), delegator) ||
                            GeoWorker.containsGeo(excludeGeoGroup, shippingAddress.getString("postalCodeGeoId"), delegator)) {
                        // in excluded geos
                        returnShippingMethods.remove(method);
                        //Debug.logInfo("Removed shipping method due to being inside the excluded GEO", module);
                        continue;
                    }
                }

                // check the features
                String includeFeatures = method.getString("includeFeatureGroup");
                String excludeFeatures = method.getString("excludeFeatureGroup");
                if (includeFeatures != null && includeFeatures.length() > 0) {
                    List includedFeatures = null;
                    try {
                        includedFeatures = delegator.findByAndCache("ProductFeatureGroupAppl", UtilMisc.toMap("productFeatureGroupId", includeFeatures));
                    } catch (GenericEntityException e) {
                        Debug.logError(e, "Unable to lookup ProductFeatureGroupAppl records for group : " + includeFeatures, module);
                    }
                    if (includedFeatures != null) {
                        boolean foundOne = false;
                        Iterator ifet = includedFeatures.iterator();
                        while (ifet.hasNext()) {
                            GenericValue appl = (GenericValue) ifet.next();
                            if (featureIdMap.containsKey(appl.getString("productFeatureId"))) {
                                foundOne = true;
                                break;
                            }
                        }
                        if (!foundOne) {
                            returnShippingMethods.remove(method);
                            //Debug.logInfo("Removed shipping method due to no required features found", module);
                            continue;
                        }
                    }
                }
                if (excludeFeatures != null && excludeFeatures.length() > 0) {
                    List excludedFeatures = null;
                    try {
                        excludedFeatures = delegator.findByAndCache("ProductFeatureGroupAppl", UtilMisc.toMap("productFeatureGroupId", excludeFeatures));
                    } catch (GenericEntityException e) {
                        Debug.logError(e, "Unable to lookup ProductFeatureGroupAppl records for group : " + excludeFeatures, module);
                    }
                    if (excludedFeatures != null) {
                        Iterator ifet = excludedFeatures.iterator();
                        while (ifet.hasNext()) {
                            GenericValue appl = (GenericValue) ifet.next();
                            if (featureIdMap.containsKey(appl.getString("productFeatureId"))) {
                                returnShippingMethods.remove(method);
                                //Debug.logInfo("Removed shipping method due to an exluded feature being found : " + appl.getString("productFeatureId"), module);
                                continue;
                            }
                        }
                    }
                }
            }
        }

        return returnShippingMethods;
    }

    public static ProductStoreSurveyWrapper getRandomSurveyWrapper(ServletRequest request, String groupName) {
        GenericValue productStore = getProductStore(request);
        HttpSession session = ((HttpServletRequest)request).getSession();
        if (productStore == null) {
            return null;
        }

        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        if (userLogin == null) {
            userLogin = (GenericValue) session.getAttribute("autoUserLogin");
        }

        String partyId = userLogin != null ? userLogin.getString("partyId") : null;
        Map passThruFields = UtilHttp.getParameterMap(((HttpServletRequest)request));

        return getRandomSurveyWrapper(productStore.getDelegator(), productStore.getString("productStoreId"), groupName, partyId, passThruFields);
    }

    public static ProductStoreSurveyWrapper getRandomSurveyWrapper(GenericDelegator delegator, String productStoreId, String groupName, String partyId, Map passThruFields) {
        List randomSurveys = getSurveys(delegator, productStoreId, groupName, null, "RANDOM_POLL", null);
        if (!UtilValidate.isEmpty(randomSurveys)) {
            Random rand = new Random();
            int index = rand.nextInt(randomSurveys.size());
            GenericValue appl = (GenericValue) randomSurveys.get(index);
            return new ProductStoreSurveyWrapper(appl, partyId, passThruFields);
        } else {
            return null;
        }
    }

    public static List getProductSurveys(GenericDelegator delegator, String productStoreId, String productId, String surveyApplTypeId) {
        return getSurveys(delegator, productStoreId, null, productId, surveyApplTypeId, null);
    }

    public static List getProductSurveys(GenericDelegator delegator, String productStoreId, String productId, String surveyApplTypeId, String parentProductId) {
        return getSurveys(delegator, productStoreId, null, productId, surveyApplTypeId,parentProductId);
    }

    public static List getSurveys(GenericDelegator delegator, String productStoreId, String groupName, String productId, String surveyApplTypeId, String parentProductId) {
        List surveys = new LinkedList();
        List storeSurveys = null;
        try {
            storeSurveys = delegator.findByAndCache("ProductStoreSurveyAppl", UtilMisc.toMap("productStoreId", productStoreId, "surveyApplTypeId", surveyApplTypeId), UtilMisc.toList("sequenceNum"));
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to get ProductStoreSurveyAppl for store : " + productStoreId, module);
            return surveys;
        }

        // limit by date
        storeSurveys = EntityUtil.filterByDate(storeSurveys);

        // limit based on group name
        if (!UtilValidate.isEmpty(groupName)) {
            storeSurveys = EntityUtil.filterByAnd(storeSurveys, UtilMisc.toMap("groupName", groupName));
        }

         Debug.log("getSurvey for product " + productId,module);
        // limit by product
        if (!UtilValidate.isEmpty(productId) && !UtilValidate.isEmpty(storeSurveys)) {
            Iterator ssi = storeSurveys.iterator();
            while (ssi.hasNext()) {
                GenericValue surveyAppl = (GenericValue) ssi.next();
                GenericValue product = null;
                String virtualProductId = null;

                // if the item is a variant, get its virtual productId 
                try {
                    product = delegator.findByPrimaryKeyCache("Product", UtilMisc.toMap("productId", productId));
                    if ((product != null) && ("Y".equals(product.get("isVariant")))) {
                        if (parentProductId != null) {
                            virtualProductId = parentProductId;
                        }
                        else {
                            virtualProductId = ProductWorker.getVariantVirtualId(product);
                        }
                        Debug.log("getSurvey for virtual product " + virtualProductId,module);
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
                    List categoryMembers = null;
                    try {
                        categoryMembers = delegator.findByAnd("ProductCategoryMember", UtilMisc.toMap("productCategoryId", surveyAppl.get("productCategoryId")));
                    } catch (GenericEntityException e) {
                        Debug.logError(e, "Unable to get ProductCategoryMemebr records for survey application : " + surveyAppl, module);
                    }
                    if (categoryMembers != null) {
                        Iterator cmi = categoryMembers.iterator();
                        while (cmi.hasNext()) {
                            GenericValue member = (GenericValue) cmi.next();
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
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
        String productStoreId = getProductStoreId(request);
        if (userLogin == null) {
            return -1;
        }

        return checkSurveyResponse(delegator, userLogin.getString("partyId"), productStoreId, surveyId);
    }

    /** Returns the number of responses for this survey by party */
    public static int checkSurveyResponse(GenericDelegator delegator, String partyId, String productStoreId, String surveyId) {
        if (delegator == null || partyId == null || productStoreId == null) {
            return -1;
        }

        List surveyResponse = null;
        try {
            surveyResponse = delegator.findByAnd("SurveyResponse", UtilMisc.toMap("surveyId", surveyId, "partyId", partyId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return -1;
        }

        if (surveyResponse == null || surveyResponse.size() == 0) {
            return 0;
        } else {
            return surveyResponse.size();
        }
    }

    public static boolean isStoreInventoryRequired(ServletRequest request, GenericValue product) {
        return isStoreInventoryRequiredAndAvailable(request, product, null, Boolean.TRUE, null);
    }

    public static boolean isStoreInventoryAvailable(ServletRequest request, GenericValue product, Double quantity) {
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
    public static boolean isStoreInventoryRequiredAndAvailable(ServletRequest request, GenericValue product, Double quantity, Boolean wantRequired, Boolean wantAvailable) {
        GenericValue productStore = getProductStore(request);
        if (productStore == null) {
            Debug.logWarning("No ProductStore found, return false for inventory check", module);
            return false;
        }
        if (product == null) {
            Debug.logWarning("No Product passed, return false for inventory check", module);
            return false;
        }

        if (quantity == null) quantity = new Double(1);

        String productStoreId = productStore.getString("productStoreId");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");

        try {
            Boolean requiredOkay = null;
            if (wantRequired != null) {
                Map invReqResult = dispatcher.runSync("isStoreInventoryRequired", UtilMisc.toMap("productStoreId", productStoreId, "productId", product.get("productId"), "product", product, "productStore", productStore));
                if (ServiceUtil.isError(invReqResult)) {
                    Debug.logError("Error calling isStoreInventoryRequired service, result is: " + invReqResult, module);
                    return false;
                }
                requiredOkay = new Boolean(wantRequired.booleanValue() == "Y".equals((String) invReqResult.get("requireInventory")));
            }

            Boolean availableOkay = null;
            if (wantAvailable != null) {
                Map invAvailResult = dispatcher.runSync("isStoreInventoryAvailable", UtilMisc.toMap("productStoreId", productStoreId, "productId", product.get("productId"), "product", product, "productStore", productStore, "quantity", quantity));
                if (ServiceUtil.isError(invAvailResult)) {
                    Debug.logError("Error calling isStoreInventoryAvailable service, result is: " + invAvailResult, module);
                    return false;
                }
                availableOkay = new Boolean(wantAvailable.booleanValue() == "Y".equals((String) invAvailResult.get("available")));
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

    public static boolean isStoreInventoryAvailable(ServletRequest request, ProductConfigWrapper productConfig, double quantity) {
        GenericValue productStore = getProductStore(request);

        if (productStore == null) {
            Debug.logWarning("No ProductStore found, return false for inventory check", module);
            return false;
        }

        String productStoreId = productStore.getString("productStoreId");
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        return isStoreInventoryAvailable(productStoreId, productConfig, quantity, delegator, dispatcher);
    }

    /** check inventory availability for the given catalog, product, quantity, etc */
    public static boolean isStoreInventoryAvailable(String productStoreId, ProductConfigWrapper productConfig, double quantity, GenericDelegator delegator, LocalDispatcher dispatcher) {
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

            try {
                isInventoryAvailable = ProductWorker.isProductInventoryAvailableByFacility(productConfig, inventoryFacilityId, quantity, dispatcher);
            } catch (GenericServiceException e) {
                Debug.logWarning(e, "Error invoking isProductInventoryAvailableByFacility in isCatalogInventoryAvailable", module);
                return false;
            }
            return isInventoryAvailable;

        } else {
            GenericValue product = productConfig.getProduct();;
            List productFacilities = null;

            try {
                productFacilities = delegator.getRelatedCache("ProductFacility", product);
            } catch (GenericEntityException e) {
                Debug.logWarning(e, "Error invoking getRelatedCache in isCatalogInventoryAvailable", module);
                return false;
            }

            if (productFacilities != null && productFacilities.size() > 0) {
                Iterator pfIter = productFacilities.iterator();

                while (pfIter.hasNext()) {
                    try {
                        GenericValue pfValue = (GenericValue) pfIter.next();

                        isInventoryAvailable = ProductWorker.isProductInventoryAvailableByFacility(productConfig, pfValue.getString("facilityId"), quantity, dispatcher);
                        if (isInventoryAvailable == true) {
                            return isInventoryAvailable;
                        }
                    } catch (GenericServiceException e) {
                        Debug.logWarning(e, "Error invoking isProductInventoryAvailableByFacility in isCatalogInventoryAvailable", module);
                        return false;
                    }
                }
            }
            return false;
        }
    }

    protected static Map defaultProductStoreEmailScreenLocation = FastMap.newInstance();

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
    }

    public static String getDefaultProductStoreEmailScreenLocation(String emailType) {
        return (String) defaultProductStoreEmailScreenLocation.get(emailType);
    }
}
