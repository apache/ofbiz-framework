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
package org.ofbiz.webpos;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.order.shoppingcart.ShoppingCart;
import org.ofbiz.product.store.ProductStoreWorker;
import org.ofbiz.securityext.login.LoginEvents;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.webpos.session.WebPosSession;

public class WebPosEvents {

    public static String module = WebPosEvents.class.getName();

    public static String posLogin(HttpServletRequest request, HttpServletResponse response) {
        String responseString = LoginEvents.storeLogin(request, response);

        if ("success".equals(responseString)) {
            HttpSession session = request.getSession(true);

            // get the posTerminalId
            String posTerminalId = (String) request.getParameter("posTerminalId");
            session.removeAttribute("shoppingCart");
            session.removeAttribute("webPosSession");
            WebPosEvents.getWebPosSession(request, posTerminalId);
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

            if (UtilValidate.isNotEmpty(productStore)) {
                facilityId = productStore.getString("inventoryFacilityId");
                if (UtilValidate.isEmpty(currencyUomId)) {
                    currencyUomId = productStore.getString("defaultCurrencyUomId");
                }
            }

            if (UtilValidate.isEmpty(cart)) {
                cart = new ShoppingCart(delegator, productStoreId, request.getLocale(), currencyUomId);
                session.setAttribute("shoppingCart", cart);

            }

            if (UtilValidate.isNotEmpty(posTerminalId)) {
                webPosSession = new WebPosSession(posTerminalId, null, userLogin, request.getLocale(), productStoreId, facilityId, currencyUomId, delegator, dispatcher, cart);
                session.setAttribute("webPosSession", webPosSession);
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
            String posTerminalId = webPosSession.getId();
            removeWebPosSession(request, posTerminalId);
        }
        return "success";
    }
}
