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

package org.ofbiz.hhfacility;

import java.util.Map;
import java.util.List;
import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.webapp.control.RequestHandler;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

public class FacilitySession {

    public static final String module = FacilitySession.class.getName();

    public static final String findProductsById(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        String idValueStr = request.getParameter("idValue");
        String facilityIdStr = request.getParameter("facilityId");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");

        if (UtilValidate.isEmpty(idValueStr)) {
            return "success";
        }

        Map productsMap = null;
        try {
            productsMap = dispatcher.runSync("findProductsById", UtilMisc.toMap("idValue", idValueStr, "facilityId", facilityIdStr));
        } catch (GenericServiceException e) {
            Debug.logError(e, "Problem in findProductsById", module);
            return "error";
        }

        if (ServiceUtil.isError(productsMap)) {
            return "error";
        }

        List productList = (List)productsMap.get("productList");
        if (productList != null && productList.size() == 1) {
            // Found only one product so go get it and redirect to the edit page
            ServletContext ctx = (ServletContext) request.getAttribute("servletContext");
            RequestHandler rh = (RequestHandler) ctx.getAttribute("_REQUEST_HANDLER_");
            GenericValue product = EntityUtil.getFirst(productList);
            String requestName = "/productstocktake?facilityId=" + facilityIdStr + "&productId=" + product.getString("productId");;
            String target = rh.makeLink(request, response, requestName, false, false, false);
            try {
                response.sendRedirect(target);
                return "none";
            } catch (IOException e) {
                Debug.logError(e, "Could not send redirect to: " + target, module);
            }
        }
        request.setAttribute("productList", productList);
        return "success";
    }
}
