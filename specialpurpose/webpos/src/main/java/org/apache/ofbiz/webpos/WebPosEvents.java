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
package org.apache.ofbiz.webpos;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.order.shoppingcart.ShoppingCart;
import org.apache.ofbiz.order.shoppingcart.ShoppingCartEvents;
import org.apache.ofbiz.order.shoppinglist.ShoppingListEvents;
import org.apache.ofbiz.product.product.ProductWorker;
import org.apache.ofbiz.product.store.ProductStoreWorker;
import org.apache.ofbiz.securityext.login.LoginEvents;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.webpos.session.WebPosSession;

public class WebPosEvents {

    public static String module = WebPosEvents.class.getName();

    public static String posLogin(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(true);
        
        // get the posTerminalId
        String posTerminalId = request.getParameter("posTerminalId");
        session.removeAttribute("shoppingCart");
        session.removeAttribute("webPosSession");
        WebPosSession webPosSession = WebPosEvents.getWebPosSession(request, posTerminalId);
        String responseString = LoginEvents.storeLogin(request, response);
        GenericValue userLoginNew = (GenericValue)session.getAttribute("userLogin");
        
        if (userLoginNew != null && UtilValidate.isNotEmpty(posTerminalId)) {
            webPosSession.setUserLogin(userLoginNew);
        }
        return responseString;
    }

    public static String existsWebPosSession(HttpServletRequest request, HttpServletResponse response) {
        String responseString = "success";
        HttpSession session = request.getSession(true);
        WebPosSession webPosSession = (WebPosSession) session.getAttribute("webPosSession");

        if (UtilValidate.isEmpty(webPosSession)) {
            responseString = "error";
        }
        return responseString;
    }
    
    public static WebPosSession getWebPosSession(HttpServletRequest request, String posTerminalId) {
        HttpSession session = request.getSession(true);
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        WebPosSession webPosSession = (WebPosSession) session.getAttribute("webPosSession");
        ShoppingCart cart = (ShoppingCart) session.getAttribute("shoppingCart");
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");

        if (UtilValidate.isEmpty(webPosSession)) {
            String productStoreId = ProductStoreWorker.getProductStoreId(request);
            GenericValue productStore = ProductStoreWorker.getProductStore(productStoreId, delegator);
            String facilityId = null;
            String currencyUomId = request.getParameter("currencyUomId");

            if (productStore != null) {
                facilityId = productStore.getString("inventoryFacilityId");
                if (UtilValidate.isEmpty(currencyUomId)) {
                    currencyUomId = productStore.getString("defaultCurrencyUomId");
                }
            }
            
            if (userLogin != null) {
                session.setAttribute("userLogin", userLogin);
            }

            if (UtilValidate.isEmpty(cart)) {
                cart = new ShoppingCart(delegator, productStoreId, request.getLocale(), currencyUomId);
                session.setAttribute("shoppingCart", cart);
            }

            if (UtilValidate.isNotEmpty(posTerminalId)) {
                webPosSession = new WebPosSession(posTerminalId, null, userLogin, request.getLocale(), productStoreId, facilityId, currencyUomId, delegator, dispatcher, cart);
                session.setAttribute("webPosSession", webPosSession);
            } else {
                Debug.logError("PosTerminalId is empty cannot create a webPosSession", module);
            }  
        }
        return webPosSession;
    }

    public static void removeWebPosSession(HttpServletRequest request, String posTerminalId) {
        HttpSession session = request.getSession(true);
        session.removeAttribute("shoppingCart");
        session.removeAttribute("webPosSession");
        getWebPosSession(request, posTerminalId);
    }

    public static String completeSale(HttpServletRequest request, HttpServletResponse response) throws GeneralException {
        HttpSession session = request.getSession(true);
        WebPosSession webPosSession = (WebPosSession) session.getAttribute("webPosSession");
        if (UtilValidate.isNotEmpty(webPosSession)) {
            webPosSession.getCurrentTransaction().processSale();
            emptyCartAndClearAutoSaveList(request, response);
            String posTerminalId = webPosSession.getId();
            removeWebPosSession(request, posTerminalId);
        }
        return "success";
    }
    
    public static String emptyCartAndClearAutoSaveList(HttpServletRequest request, HttpServletResponse response) throws GeneralException {
        HttpSession session = request.getSession(true);
        WebPosSession webPosSession = (WebPosSession) session.getAttribute("webPosSession");
        ShoppingCartEvents.clearCart(request, response);
        
        if (UtilValidate.isNotEmpty(webPosSession)) {
            String autoSaveListId = ShoppingListEvents.getAutoSaveListId(webPosSession.getDelegator(), webPosSession.getDispatcher(), null, webPosSession.getUserLogin(), webPosSession.getProductStoreId());
            ShoppingListEvents.clearListInfo(webPosSession.getDelegator(), autoSaveListId);
        }
        return "success";
    }    
    
    public static String getProductType(HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> featureMap = null;
        Map<String, Object> variantTreeMap = null;
        Map<String, Object> featureTypes = new HashMap<String, Object>();
        WebPosSession webPosSession = getWebPosSession(request, null);
        if (webPosSession != null) {
            Delegator delegator = webPosSession.getDelegator();
            LocalDispatcher dispatcher = webPosSession.getDispatcher();
            GenericValue product = null;
            try {
                String productId = request.getParameter("productId");
                product = EntityQuery.use(delegator).from("Product").where("productId", productId).queryOne();
                if (product != null) {
                    request.setAttribute("product", product);
                    if (UtilValidate.isNotEmpty(product.getString("isVirtual")) && "Y".equalsIgnoreCase(product.getString("isVirtual"))) {
                        String virtualVariantMethodEnum = product.getString("virtualVariantMethodEnum");
                        if (UtilValidate.isEmpty(virtualVariantMethodEnum)) {
                            virtualVariantMethodEnum = "VV_VARIANTTREE";
                        }
                        if ("VV_VARIANTTREE".equalsIgnoreCase(virtualVariantMethodEnum)) {
                            String productStoreId = webPosSession.getProductStoreId();
                            try {
                                featureMap = dispatcher.runSync("getProductFeatureSet", UtilMisc.toMap("productId", productId));
                                Set<String> featureSet = UtilGenerics.cast(featureMap.get("featureSet"));
                                if (UtilValidate.isNotEmpty(featureSet)) {
                                    request.setAttribute("featureSet", featureSet);
                                    try {
                                        variantTreeMap = dispatcher.runSync("getProductVariantTree", 
                                                         UtilMisc.toMap("productId", productId, "featureOrder", featureSet, "productStoreId", productStoreId));
                                        Map<String, Object> variantTree = UtilGenerics.cast(variantTreeMap.get("variantTree"));
                                        if (UtilValidate.isNotEmpty(variantTree)) {
                                            request.setAttribute("variantTree", variantTree);
                                            request.setAttribute("variantTreeSize", variantTree.size());
                                            List<String> featureOrder = new LinkedList<String>();
                                            featureOrder = UtilMisc.toList(featureSet);
                                            for (int i=0; i < featureOrder.size(); i++) {
                                                String featureKey = featureOrder.get(i);
                                                GenericValue featureValue = EntityQuery.use(delegator).from("ProductFeatureType").where("productFeatureTypeId", featureOrder.get(i)).cache().queryOne();
                                                if (featureValue != null && 
                                                    UtilValidate.isNotEmpty(featureValue.get("description"))) {
                                                    featureTypes.put(featureKey, featureValue.get("description"));
                                                } else {
                                                    featureTypes.put(featureKey, featureValue.get("productFeatureTypeId"));
                                                }
                                            }
                                            request.setAttribute("featureTypes", featureTypes);
                                            request.setAttribute("featureOrder", featureOrder);
                                            if (UtilValidate.isNotEmpty(featureOrder)) {
                                                request.setAttribute("featureOrderFirst", featureOrder.get(0));
                                            }
                                        }
                                        Map<String, Object> imageMap = UtilGenerics.cast(variantTreeMap.get("variantSample"));
                                        if (UtilValidate.isNotEmpty(imageMap)) {
                                            List<String> variantSampleList = new LinkedList<String>();
                                            variantSampleList = UtilMisc.toList(imageMap.keySet());
                                            request.setAttribute("variantSample", imageMap);
                                            request.setAttribute("variantSampleList", variantSampleList);
                                            request.setAttribute("variantSampleSize", imageMap.size());
                                        }
                                    } catch (GenericServiceException e) {
                                        Debug.logError(e, module);
                                        return "error";
                                    }
                                }
                            } catch (GenericServiceException e) {
                                Debug.logError(e, module);
                                return "error";
                            }
                        }
                        if ("VV_FEATURETREE".equalsIgnoreCase(virtualVariantMethodEnum)) {
                            request.setAttribute("featureLists", ProductWorker.getSelectableProductFeaturesByTypesAndSeq(product));
                        }
                    }
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                return "error";
            }
        }
        return "success";
    }
}