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
package org.apache.ofbiz.product.category;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.util.EntityQuery;

/**
 * SeoCatalogUrlServlet.java
 */
@SuppressWarnings("serial")
public class SeoCatalogUrlServlet extends HttpServlet {

    public static final String module = SeoCatalogUrlServlet.class.getName();
    public static final String CATALOG_URL_MOUNT_POINT = "products";
    public static final String PRODUCT_REQUEST = "product";
    public static final String CATEGORY_REQUEST = "category";

    public SeoCatalogUrlServlet() {
        super();
    }

    /**
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    /**
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Delegator delegator = (Delegator) getServletContext().getAttribute("delegator");

        String pathInfo = request.getPathInfo();
        List<String> pathElements = StringUtil.split(pathInfo, "/");

        // look for productId
        String productId = null;
        try {
            String lastPathElement = pathElements.get(pathElements.size() - 1);
            if (lastPathElement.startsWith("p_") || EntityQuery.use(delegator).from("Product").where("productId", lastPathElement).cache().queryOne() != null) {
                if (lastPathElement.startsWith("p_")) {
                    productId = lastPathElement.substring(2);
                } else {
                    productId = lastPathElement;
                }
                pathElements.remove(pathElements.size() - 1);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error looking up product info for ProductUrl with path info [" + pathInfo + "]: " + e.toString(), module);
        }

        // get category info going with the IDs that remain
        String categoryId = null;
        if (pathElements.size() == 1) {
            CategoryWorker.setTrail(request, pathElements.get(0), null);
            categoryId = pathElements.get(0);
        } else if (pathElements.size() == 2) {
            CategoryWorker.setTrail(request, pathElements.get(1), pathElements.get(0));
            categoryId = pathElements.get(1);
        } else if (pathElements.size() > 2) {
            List<String> trail = CategoryWorker.getTrail(request);
            if (trail == null) {
                trail = new LinkedList<String>();
            }

            if (trail.contains(pathElements.get(0))) {
                // first category is in the trail, so remove it everything after that and fill it in with the list from the pathInfo
                int firstElementIndex = trail.indexOf(pathElements.get(0));
                while (trail.size() > firstElementIndex) {
                    trail.remove(firstElementIndex);
                }
                trail.addAll(pathElements);
            } else {
                // first category is NOT in the trail, so clear out the trail and use the pathElements list
                trail.clear();
                trail.addAll(pathElements);
            }
            CategoryWorker.setTrail(request, trail);
            categoryId = pathElements.get(pathElements.size() - 1);
        }
        if (categoryId != null) {
            request.setAttribute("productCategoryId", categoryId);
        }

        String rootCategoryId = null;
        if (pathElements.size() >= 1) {
            rootCategoryId = pathElements.get(0);
        }
        if (rootCategoryId != null) {
            request.setAttribute("rootCategoryId", rootCategoryId);
        }

        if (productId != null) {
            request.setAttribute("product_id", productId);
            request.setAttribute("productId", productId);
        }

        RequestDispatcher rd = request.getRequestDispatcher("/" + (UtilValidate.isEmpty(SeoControlServlet.getControlServlet()) ? "" : (SeoControlServlet.getControlServlet() + "/"))
                + (productId != null ? PRODUCT_REQUEST : CATEGORY_REQUEST));
        rd.forward(request, response);
    }


    public static String makeCatalogUrl(HttpServletRequest request, String productId, String currentCategoryId, String previousCategoryId) {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(request.getSession().getServletContext().getContextPath());
        if (urlBuilder.length() == 0 || urlBuilder.charAt(urlBuilder.length() - 1) != '/') {
            urlBuilder.append("/");
        }
        urlBuilder.append(CATALOG_URL_MOUNT_POINT);

        if (UtilValidate.isNotEmpty(currentCategoryId)) {
            List<String> trail = CategoryWorker.getTrail(request);
            trail = CategoryWorker.adjustTrail(trail, currentCategoryId, previousCategoryId);
            for (String trailCategoryId : trail) {
                if ("TOP".equals(trailCategoryId)) continue;
                urlBuilder.append("/");
                urlBuilder.append(trailCategoryId);
            }
        }

        if (UtilValidate.isNotEmpty(productId)) {
            urlBuilder.append("/p_");
            urlBuilder.append(productId);
        }

        return urlBuilder.toString();
    }

    public static String makeCatalogUrl(String contextPath, List<String> crumb, String productId, String currentCategoryId, String previousCategoryId) {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(contextPath);
        if (urlBuilder.charAt(urlBuilder.length() - 1) != '/') {
            urlBuilder.append("/");
        }
        urlBuilder.append(CATALOG_URL_MOUNT_POINT);

        if (UtilValidate.isNotEmpty(currentCategoryId)) {
            crumb = CategoryWorker.adjustTrail(crumb, currentCategoryId, previousCategoryId);
            for (String trailCategoryId : crumb) {
                if ("TOP".equals(trailCategoryId)) continue;
                urlBuilder.append("/");
                urlBuilder.append(trailCategoryId);
            }
        }

        if (UtilValidate.isNotEmpty(productId)) {
            urlBuilder.append("/p_");
            urlBuilder.append(productId);
        }

        return urlBuilder.toString();
    }
}
