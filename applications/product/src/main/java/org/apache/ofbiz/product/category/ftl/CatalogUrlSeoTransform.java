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
package org.apache.ofbiz.product.category.ftl;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.StringUtil.StringWrapper;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.common.UrlServletHelper;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.product.category.CatalogUrlServlet;
import org.apache.ofbiz.product.category.CategoryContentWrapper;
import org.apache.ofbiz.product.category.CategoryWorker;
import org.apache.ofbiz.product.category.SeoConfigUtil;
import org.apache.ofbiz.product.category.SeoUrlUtil;
import org.apache.ofbiz.product.product.ProductContentWrapper;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;

import freemarker.core.Environment;
import freemarker.ext.beans.BeanModel;
import freemarker.ext.beans.StringModel;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateTransformModel;
import org.apache.ofbiz.entity.util.EntityQuery;

public class CatalogUrlSeoTransform implements TemplateTransformModel {
    public final static String module = CatalogUrlSeoTransform.class.getName();

    private static Map<String, String> categoryNameIdMap = null;
    private static Map<String, String> categoryIdNameMap = null;
    private static boolean categoryMapInitialed = false;
    private static final String asciiRegexp = "^[0-9-_a-zA-Z]*$";
    private static Pattern asciiPattern = null;
    public static final String URL_HYPHEN = "-";

    static {
        if (!SeoConfigUtil.isInitialed()) {
            SeoConfigUtil.init();
        }
        try {
            Perl5Compiler perlCompiler = new Perl5Compiler();
            asciiPattern = perlCompiler.compile(asciiRegexp, Perl5Compiler.READ_ONLY_MASK);
        } catch (MalformedPatternException e1) {
            Debug.logWarning(e1, module);
        }
    }

    public String getStringArg(Map<?, ?> args, String key) {
        Object o = args.get(key);
        if (o instanceof SimpleScalar) {
            return ((SimpleScalar) o).getAsString();
        } else if (o instanceof StringModel) {
            return ((StringModel) o).getAsString();
        }
        return null;
    }

    @Override
    public Writer getWriter(Writer out, @SuppressWarnings("rawtypes") Map args)
            throws TemplateModelException, IOException {
        final StringBuilder buf = new StringBuilder();

        return new Writer(out) {

            @Override
            public void write(char[] cbuf, int off, int len) throws IOException {
                buf.append(cbuf, off, len);
            }

            @Override
            public void flush() throws IOException {
                out.flush();
            }

            @Override
            public void close() throws IOException {
                try {
                    Environment env = Environment.getCurrentEnvironment();
                    BeanModel req = (BeanModel) env.getVariable("request");
                    if (req != null) {
                        String productId = getStringArg(args, "productId");
                        String currentCategoryId = getStringArg(args, "currentCategoryId");
                        String previousCategoryId = getStringArg(args, "previousCategoryId");
                        HttpServletRequest request = (HttpServletRequest) req.getWrappedObject();

                        if (!isCategoryMapInitialed()) {
                            initCategoryMap(request);
                        }

                        String catalogUrl = "";
                        if (SeoConfigUtil.isCategoryUrlEnabled(request.getContextPath())) {
                            if (UtilValidate.isEmpty(productId)) {
                                catalogUrl = makeCategoryUrl(request, currentCategoryId, previousCategoryId, null, null, null, null);
                            } else {
                                catalogUrl = makeProductUrl(request, productId, currentCategoryId, previousCategoryId);
                            }
                        } else {
                            catalogUrl = CatalogUrlServlet.makeCatalogUrl(request, productId, currentCategoryId, previousCategoryId);
                        }
                        out.write(catalogUrl);
                    }
                } catch (TemplateModelException e) {
                    throw new IOException(e.getMessage());
                }
            }
        };
    }

    /**
     * Check whether the category map is initialed.
     *
     * @return a boolean value to indicate whether the category map has been initialized.
     */
    public static boolean isCategoryMapInitialed() {
        return categoryMapInitialed;
    }

    /**
     * Get the category name/id map.
     *
     * @return the category name/id map
     */
    public static Map<String, String> getCategoryNameIdMap() {
        return categoryNameIdMap;
    }

    /**
     * Get the category id/name map.
     *
     * @return the category id/name map
     */
    public static Map<String, String> getCategoryIdNameMap() {
        return categoryIdNameMap;
    }

    /**
     * Initial category-name/category-id map.
     * Note: as a key, the category-name should be:
     *         1. ascii
     *         2. lower cased and use hyphen between the words.
     *       If not, the category id will be used.
     *
     */
    public static synchronized void initCategoryMap(HttpServletRequest request) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        initCategoryMap(request, delegator);
    }

    public static synchronized void initCategoryMap(HttpServletRequest request, Delegator delegator) {
        if (SeoConfigUtil.checkCategoryUrl()) {
            categoryNameIdMap = new Hashtable<>();
            categoryIdNameMap = new Hashtable<>();
            Perl5Matcher matcher = new Perl5Matcher();

            try {
                Collection<GenericValue> allCategories = delegator.findList("ProductCategory", null, UtilMisc.toSet("productCategoryId", "categoryName"), null, null, false);
                for (GenericValue category : allCategories) {
                    String categoryName = category.getString("categoryName");
                    String categoryNameId = null;
                    String categoryIdName = null;
                    String categoryId = category.getString("productCategoryId");
                    if (UtilValidate.isNotEmpty(categoryName)) {
                        categoryName = SeoUrlUtil.replaceSpecialCharsUrl(categoryName.trim());
                        if (matcher.matches(categoryName, asciiPattern)) {
                            categoryIdName = categoryName.replaceAll(" ", URL_HYPHEN);
                            categoryNameId = categoryIdName + URL_HYPHEN + categoryId.trim().replaceAll(" ", URL_HYPHEN);
                        } else {
                            categoryIdName = categoryId.trim().replaceAll(" ", URL_HYPHEN);
                            categoryNameId = categoryIdName;
                        }
                    } else {
                        GenericValue productCategory = EntityQuery.use(delegator).from("ProductCategory").where("productCategoryId", categoryId).cache().queryOne();
                        CategoryContentWrapper wrapper = new CategoryContentWrapper(productCategory, request);
                        StringWrapper alternativeUrl = wrapper.get("ALTERNATIVE_URL", "url");
                        if (UtilValidate.isNotEmpty(alternativeUrl) && UtilValidate.isNotEmpty(alternativeUrl.toString())) {
                            categoryIdName = SeoUrlUtil.replaceSpecialCharsUrl(alternativeUrl.toString());
                            categoryNameId = categoryIdName + URL_HYPHEN + categoryId.trim().replaceAll(" ", URL_HYPHEN);
                        } else {
                            categoryNameId = categoryId.trim().replaceAll(" ", URL_HYPHEN);
                            categoryIdName = categoryNameId;
                        }
                    }
                    if (categoryNameIdMap.containsKey(categoryNameId)) {
                        categoryNameId = categoryId.trim().replaceAll(" ", URL_HYPHEN);
                        categoryIdName = categoryNameId;
                    }
                    if (!matcher.matches(categoryNameId, asciiPattern) || categoryNameIdMap.containsKey(categoryNameId)) {
                        continue;
                    }
                    categoryNameIdMap.put(categoryNameId, categoryId);
                    categoryIdNameMap.put(categoryId, categoryIdName);
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
        }
        categoryMapInitialed = true;
    }

    /**
     * Make product url according to the configurations.
     *
     * @return String a catalog url
     */
    public static String makeProductUrl(HttpServletRequest request, String productId, String currentCategoryId, String previousCategoryId) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        if (!isCategoryMapInitialed()) {
            initCategoryMap(request);
        }

        String contextPath = request.getContextPath();
        StringBuilder urlBuilder = new StringBuilder();
        GenericValue product = null;
        urlBuilder.append((request.getSession().getServletContext()).getContextPath());
        if (urlBuilder.length() == 0 || urlBuilder.charAt(urlBuilder.length() - 1) != '/') {
            urlBuilder.append("/");
        }
        if (UtilValidate.isNotEmpty(productId)) {
            try {
                product = EntityQuery.use(delegator).from("Product").where("productId", productId).cache().queryOne();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error looking up product info for productId [" + productId + "]: " + e.toString(), module);
            }
        }
        if (product != null) {
            urlBuilder.append(CatalogUrlServlet.PRODUCT_REQUEST + "/");
        }

        if (UtilValidate.isNotEmpty(currentCategoryId)) {
            List<String> trail = CategoryWorker.getTrail(request);
            trail = CategoryWorker.adjustTrail(trail, currentCategoryId, previousCategoryId);
            if (!SeoConfigUtil.isCategoryUrlEnabled(contextPath)) {
                for (String trailCategoryId: trail) {
                    if ("TOP".equals(trailCategoryId)) {
                        continue;
                    }
                    urlBuilder.append("/");
                    urlBuilder.append(trailCategoryId);
                }
            } else {
                if (trail.size() > 1) {
                    String lastCategoryId = trail.get(trail.size() - 1);
                    if (!"TOP".equals(lastCategoryId)) {
                        if (SeoConfigUtil.isCategoryNameEnabled()) {
                            String categoryName = CatalogUrlSeoTransform.getCategoryIdNameMap().get(lastCategoryId);
                            if (UtilValidate.isNotEmpty(categoryName)) {
                                urlBuilder.append(categoryName);
                                if (product != null) {
                                    urlBuilder.append(URL_HYPHEN);
                                }
                            }
                        }
                    }
                }
            }
        }

        if (UtilValidate.isNotEmpty(productId)) {
            if (product != null) {
                String productName = product.getString("productName");
                productName = SeoUrlUtil.replaceSpecialCharsUrl(productName);
                if (UtilValidate.isNotEmpty(productName)) {
                    urlBuilder.append(productName + URL_HYPHEN);
                } else {
                    ProductContentWrapper wrapper = new ProductContentWrapper(product, request);
                    StringWrapper alternativeUrl = wrapper.get("ALTERNATIVE_URL", "url");
                    if (UtilValidate.isNotEmpty(alternativeUrl) && UtilValidate.isNotEmpty(alternativeUrl.toString())) {
                        productName = SeoUrlUtil.replaceSpecialCharsUrl(alternativeUrl.toString());
                        if (UtilValidate.isNotEmpty(productName)) {
                            urlBuilder.append(productName + URL_HYPHEN);
                        }
                    }
                }
            }
            try {
                urlBuilder.append(productId);
            } catch (Exception e) {
                urlBuilder.append(productId);
            }
        }

        if (!urlBuilder.toString().endsWith("/") && UtilValidate.isNotEmpty(SeoConfigUtil.getCategoryUrlSuffix())) {
            urlBuilder.append(SeoConfigUtil.getCategoryUrlSuffix());
        }

        return urlBuilder.toString();
    }

    /**
     * Make category url according to the configurations.
     *
     * @return String a category url
     */
    public static String makeCategoryUrl(HttpServletRequest request, String currentCategoryId, String previousCategoryId, String viewSize, String viewIndex, String viewSort, String searchString) {

        if (!isCategoryMapInitialed()) {
            initCategoryMap(request);
        }

        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append((request.getSession().getServletContext()).getContextPath());
        if (urlBuilder.length() == 0 || urlBuilder.charAt(urlBuilder.length() - 1) != '/') {
            urlBuilder.append("/");
        }
        urlBuilder.append(CatalogUrlServlet.CATEGORY_REQUEST + "/");

        if (UtilValidate.isNotEmpty(currentCategoryId)) {
            List<String> trail = CategoryWorker.getTrail(request);
            trail = CategoryWorker.adjustTrail(trail, currentCategoryId, previousCategoryId);
            if (trail.size() > 1) {
                String lastCategoryId = trail.get(trail.size() - 1);
                if (!"TOP".equals(lastCategoryId)) {
                    String categoryName = CatalogUrlSeoTransform.getCategoryIdNameMap().get(lastCategoryId);
                    if (UtilValidate.isNotEmpty(categoryName)) {
                        urlBuilder.append(categoryName);
                        urlBuilder.append(URL_HYPHEN);
                        urlBuilder.append(lastCategoryId.trim().replaceAll(" ", URL_HYPHEN));
                    } else {
                        urlBuilder.append(lastCategoryId.trim().replaceAll(" ", URL_HYPHEN));
                    }
                }
            }
        }

        if (!urlBuilder.toString().endsWith("/") && UtilValidate.isNotEmpty(SeoConfigUtil.getCategoryUrlSuffix())) {
            urlBuilder.append(SeoConfigUtil.getCategoryUrlSuffix());
        }

        // append view index
        if (UtilValidate.isNotEmpty(viewIndex)) {
            if (!urlBuilder.toString().endsWith("?") && !urlBuilder.toString().endsWith("&")) {
                urlBuilder.append("?");
            }
            urlBuilder.append("viewIndex=" + viewIndex + "&");
        }
        // append view size
        if (UtilValidate.isNotEmpty(viewSize)) {
            if (!urlBuilder.toString().endsWith("?") && !urlBuilder.toString().endsWith("&")) {
                urlBuilder.append("?");
            }
            urlBuilder.append("viewSize=" + viewSize + "&");
        }
        // append view sort
        if (UtilValidate.isNotEmpty(viewSort)) {
            if (!urlBuilder.toString().endsWith("?") && !urlBuilder.toString().endsWith("&")) {
                urlBuilder.append("?");
            }
            urlBuilder.append("viewSort=" + viewSort + "&");
        }
        // append search string
        if (UtilValidate.isNotEmpty(searchString)) {
            if (!urlBuilder.toString().endsWith("?") && !urlBuilder.toString().endsWith("&")) {
                urlBuilder.append("?");
            }
            urlBuilder.append("searchString=" + searchString + "&");
        }
        if (urlBuilder.toString().endsWith("&")) {
            return urlBuilder.toString().substring(0, urlBuilder.toString().length()-1);
        }

        return urlBuilder.toString();
    }

    /**
     * Make product url according to the configurations.
     *
     * @return String a catalog url
     */
    public static String makeProductUrl(String contextPath, List<String> trail, String productId, String productName, String currentCategoryId, String previousCategoryId) {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(contextPath);
        if (urlBuilder.charAt(urlBuilder.length() - 1) != '/') {
            urlBuilder.append("/");
        }
        if (!SeoConfigUtil.isCategoryUrlEnabled(contextPath)) {
            urlBuilder.append(CatalogUrlServlet.CATALOG_URL_MOUNT_POINT);
        } else {
            urlBuilder.append(CatalogUrlServlet.PRODUCT_REQUEST + "/");
        }

        if (UtilValidate.isNotEmpty(currentCategoryId)) {
            trail = CategoryWorker.adjustTrail(trail, currentCategoryId, previousCategoryId);
            if (!SeoConfigUtil.isCategoryUrlEnabled(contextPath)) {
                for (String trailCategoryId: trail) {
                    if ("TOP".equals(trailCategoryId)) {
                        continue;
                    }
                    urlBuilder.append("/");
                    urlBuilder.append(trailCategoryId);
                }
            } else {
                if (trail.size() > 1) {
                    String lastCategoryId = trail.get(trail.size() - 1);
                    if (!"TOP".equals(lastCategoryId)) {
                        if (SeoConfigUtil.isCategoryNameEnabled()) {
                            String categoryName = CatalogUrlSeoTransform.getCategoryIdNameMap().get(lastCategoryId);
                            if (UtilValidate.isNotEmpty(categoryName)) {
                                urlBuilder.append(categoryName + URL_HYPHEN);
                            }
                        }
                    }
                }
            }
        }

        if (UtilValidate.isNotEmpty(productId)) {
            if (!SeoConfigUtil.isCategoryUrlEnabled(contextPath)) {
                urlBuilder.append("/p_");
            } else {
                productName = SeoUrlUtil.replaceSpecialCharsUrl(productName);
                if (UtilValidate.isNotEmpty(productName)) {
                    urlBuilder.append(productName + URL_HYPHEN);
                }
            }
            urlBuilder.append(productId);
        }

        if (!urlBuilder.toString().endsWith("/") && UtilValidate.isNotEmpty(SeoConfigUtil.getCategoryUrlSuffix())) {
            urlBuilder.append(SeoConfigUtil.getCategoryUrlSuffix());
        }

        return urlBuilder.toString();
    }

    /**
     * Get a string lower cased and hyphen connected.
     *
     * @param name a String to be transformed
     * @return String nice name
     */
    protected static String getNiceName(String name) {
        Perl5Matcher matcher = new Perl5Matcher();
        String niceName = null;
        if (UtilValidate.isNotEmpty(name)) {
            name = name.trim().replaceAll(" ", URL_HYPHEN);
            if (UtilValidate.isNotEmpty(name) && matcher.matches(name, asciiPattern)) {
                niceName = name;
            }
        }
        return niceName;
    }

    public static boolean forwardProductUri(HttpServletRequest request, HttpServletResponse response, Delegator delegator) throws ServletException, IOException {
        return forwardProductUri(request, response, delegator, null);
    }

    public static boolean forwardProductUri(HttpServletRequest request, HttpServletResponse response, Delegator delegator, String controlServlet) throws ServletException, IOException {
        return forwardUri(request, response, delegator, controlServlet);
    }

    /**
     * Forward a uri according to forward pattern regular expressions.
     * @param request
     * @param response
     * @param delegator
     * @param controlServlet
     * @return boolean to indicate whether the uri is forwarded.
     * @throws ServletException
     * @throws IOException
     */
    public static boolean forwardUri(HttpServletRequest request, HttpServletResponse response, Delegator delegator, String controlServlet) throws ServletException, IOException {
        String pathInfo = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (!isCategoryMapInitialed()) {
            initCategoryMap(request, delegator);
        }

        if (!SeoConfigUtil.isCategoryUrlEnabled(contextPath)) {
            return false;
        }
        List<String> pathElements = StringUtil.split(pathInfo, "/");
        if (UtilValidate.isEmpty(pathElements)) {
            return false;
        }
        // remove context path
        pathInfo = SeoUrlUtil.removeContextPath(pathInfo, contextPath);
        // remove servlet path
        pathInfo = SeoUrlUtil.removeContextPath(pathInfo, request.getServletPath());
        if (pathInfo.startsWith("/" + CatalogUrlServlet.CATEGORY_REQUEST + "/")) {
            return forwardCategoryUri(request, response, delegator, controlServlet);
        }

        String lastPathElement = pathElements.get(pathElements.size() - 1);
        String categoryId = null;
        String productId = null;
        if (UtilValidate.isNotEmpty(lastPathElement)) {
            if (UtilValidate.isNotEmpty(SeoConfigUtil.getCategoryUrlSuffix())) {
                if (lastPathElement.endsWith(SeoConfigUtil.getCategoryUrlSuffix())) {
                    lastPathElement = lastPathElement.substring(0, lastPathElement.length() - SeoConfigUtil.getCategoryUrlSuffix().length());
                } else {
                    return false;
                }
            }
            if (SeoConfigUtil.isCategoryNameEnabled() || pathInfo.startsWith("/" + CatalogUrlServlet.CATEGORY_REQUEST + "/")) {
                for (Entry<String, String> entry : categoryNameIdMap.entrySet()) {
                    String categoryName = entry.getKey();
                    if (lastPathElement.startsWith(categoryName)) {
                        categoryId = entry.getValue();
                        if (!lastPathElement.equals(categoryName)) {
                            lastPathElement = lastPathElement.substring(categoryName.length() + URL_HYPHEN.length());
                        }
                        break;
                    }
                }
                if (UtilValidate.isEmpty(categoryId)) {
                    categoryId = lastPathElement;
                }
            }

            if (UtilValidate.isNotEmpty(lastPathElement)) {
                List<String> urlElements = StringUtil.split(lastPathElement, URL_HYPHEN);
                if (UtilValidate.isEmpty(urlElements)) {
                    try {
                        if (EntityQuery.use(delegator).from("Product").where("productId", lastPathElement).cache().queryOne() != null) {
                            productId = lastPathElement;
                        }
                    } catch (GenericEntityException e) {
                        Debug.logError(e, "Error looking up product info for ProductUrl with path info [" + pathInfo + "]: " + e.toString(), module);
                    }
                } else {
                    int i = urlElements.size() - 1;
                    String tempProductId = urlElements.get(i);
                    while (i >= 0) {
                        try {
                            List<EntityExpr> exprs = new LinkedList<>();
                            exprs.add(EntityCondition.makeCondition("productId", EntityOperator.EQUALS, lastPathElement));
                            exprs.add(EntityCondition.makeCondition("productId", EntityOperator.EQUALS, tempProductId));
                            List<GenericValue> products = delegator.findList("Product", EntityCondition.makeCondition(exprs, EntityOperator.OR), UtilMisc.toSet("productId", "productName"), null, null, true);

                            if (products != null && products.size() > 0) {
                                if (products.size() == 1) {
                                    productId = products.get(0).getString("productId");
                                    break;
                                }
                                productId = tempProductId;
                                break;
                            } else if (i > 0) {
                                tempProductId = urlElements.get(i - 1) + URL_HYPHEN + tempProductId;
                            }
                        } catch (GenericEntityException e) {
                            Debug.logError(e, "Error looking up product info for ProductUrl with path info [" + pathInfo + "]: " + e.toString(), module);
                        }
                        i--;
                    }
                }
            }
        }

        if (UtilValidate.isNotEmpty(productId) || UtilValidate.isNotEmpty(categoryId)) {
            if (categoryId != null) {
                request.setAttribute("productCategoryId", categoryId);
            }

            if (productId != null) {
                request.setAttribute("product_id", productId);
                request.setAttribute("productId", productId);
            }

            StringBuilder urlBuilder = new StringBuilder();
            if (UtilValidate.isNotEmpty(controlServlet)) {
                urlBuilder.append("/" + controlServlet);
            }
            urlBuilder.append("/" + (productId != null ? CatalogUrlServlet.PRODUCT_REQUEST : CatalogUrlServlet.CATEGORY_REQUEST));
            UrlServletHelper.setViewQueryParameters(request, urlBuilder);
            Debug.logInfo("[Filtered request]: " + pathInfo + " (" + urlBuilder + ")", module);
            RequestDispatcher rd = request.getRequestDispatcher(urlBuilder.toString());
            rd.forward(request, response);
            return true;
        }
        return false;
    }

    /**
     * Forward a category uri according to forward pattern regular expressions.
     * @param request
     * @param response
     * @param delegator
     * @param controlServlet
     * @return
     * @throws ServletException
     * @throws IOException
     */
    public static boolean forwardCategoryUri(HttpServletRequest request, HttpServletResponse response, Delegator delegator, String controlServlet) throws ServletException, IOException {
        String pathInfo = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (!isCategoryMapInitialed()) {
            initCategoryMap(request);
        }
        if (!SeoConfigUtil.isCategoryUrlEnabled(contextPath)) {
            return false;
        }
        List<String> pathElements = StringUtil.split(pathInfo, "/");
        if (UtilValidate.isEmpty(pathElements)) {
            return false;
        }
        String lastPathElement = pathElements.get(pathElements.size() - 1);
        String categoryId = null;
        if (UtilValidate.isNotEmpty(lastPathElement)) {
            if (UtilValidate.isNotEmpty(SeoConfigUtil.getCategoryUrlSuffix())) {
                if (lastPathElement.endsWith(SeoConfigUtil.getCategoryUrlSuffix())) {
                    lastPathElement = lastPathElement.substring(0, lastPathElement.length() - SeoConfigUtil.getCategoryUrlSuffix().length());
                } else {
                    return false;
                }
            }
            for (Entry<String, String> entry : categoryNameIdMap.entrySet()) {
                String categoryName = entry.getKey();
                if (lastPathElement.startsWith(categoryName)) {
                    categoryId = entry.getValue();
                    break;
                }
            }
            if (UtilValidate.isEmpty(categoryId)) {
                categoryId = lastPathElement.trim();
            }
        }
        if (UtilValidate.isNotEmpty(categoryId)) {
            request.setAttribute("productCategoryId", categoryId);
            StringBuilder urlBuilder = new StringBuilder();
            if (UtilValidate.isNotEmpty(controlServlet)) {
                urlBuilder.append("/" + controlServlet);
            }
            urlBuilder.append("/" + CatalogUrlServlet.CATEGORY_REQUEST);
            UrlServletHelper.setViewQueryParameters(request, urlBuilder);
            Debug.logInfo("[Filtered request]: " + pathInfo + " (" + urlBuilder + ")", module);
            RequestDispatcher rd = request.getRequestDispatcher(urlBuilder.toString());
            rd.forward(request, response);
            return true;
        }
        return false;
    }

    /**
     * This is used when building product url in services.
     *
     * @param delegator
     * @param wrapper
     * @param prefix
     * @param contextPath
     * @param currentCategoryId
     * @param previousCategoryId
     * @param productId
     * @return
     */
    public static String makeProductUrl(Delegator delegator, ProductContentWrapper wrapper, String prefix, String contextPath, String currentCategoryId, String previousCategoryId,
            String productId) {
        StringBuilder urlBuilder = new StringBuilder();
        GenericValue product = null;
        urlBuilder.append(prefix);
        if (urlBuilder.charAt(urlBuilder.length() - 1) != '/') {
            urlBuilder.append("/");
        }
        if (UtilValidate.isNotEmpty(productId)) {
            try {
                product = EntityQuery.use(delegator).from("Product").where("productId", productId).cache().queryOne();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error looking up product info for productId [" + productId + "]: " + e.toString(), module);
            }
        }
        if (product != null) {
            urlBuilder.append(CatalogUrlServlet.PRODUCT_REQUEST + "/");
        }

        if (UtilValidate.isNotEmpty(currentCategoryId)) {
            List<String> trail = null;
            trail = CategoryWorker.adjustTrail(null, currentCategoryId, previousCategoryId);
            if (!SeoConfigUtil.isCategoryUrlEnabled(contextPath)) {
                for (String trailCategoryId: trail) {
                    if ("TOP".equals(trailCategoryId)) {
                        continue;
                    }
                    urlBuilder.append("/");
                    urlBuilder.append(trailCategoryId);
                }
            } else {
                if (trail != null && trail.size() > 1) {
                    String lastCategoryId = trail.get(trail.size() - 1);
                    if (!"TOP".equals(lastCategoryId)) {
                        if (SeoConfigUtil.isCategoryNameEnabled()) {
                            String categoryName = CatalogUrlSeoTransform.getCategoryIdNameMap().get(lastCategoryId);
                            if (UtilValidate.isNotEmpty(categoryName)) {
                                urlBuilder.append(categoryName);
                                if (product != null) {
                                    urlBuilder.append(URL_HYPHEN);
                                }
                            }
                        }
                    }
                }
            }
        }

        if (UtilValidate.isNotEmpty(productId)) {
            if (product != null) {
                String productName = product.getString("productName");
                productName = SeoUrlUtil.replaceSpecialCharsUrl(productName);
                if (UtilValidate.isNotEmpty(productName)) {
                    urlBuilder.append(productName + URL_HYPHEN);
                } else {
                    StringWrapper alternativeUrl = wrapper.get("ALTERNATIVE_URL", "url");
                    if (UtilValidate.isNotEmpty(alternativeUrl) && UtilValidate.isNotEmpty(alternativeUrl.toString())) {
                        productName = SeoUrlUtil.replaceSpecialCharsUrl(alternativeUrl.toString());
                        if (UtilValidate.isNotEmpty(productName)) {
                            urlBuilder.append(productName + URL_HYPHEN);
                        }
                    }
                }
            }
            try {
                urlBuilder.append(productId);
            } catch (Exception e) {
                urlBuilder.append(productId);
            }
        }

        if (!urlBuilder.toString().endsWith("/") && UtilValidate.isNotEmpty(SeoConfigUtil.getCategoryUrlSuffix())) {
            urlBuilder.append(SeoConfigUtil.getCategoryUrlSuffix());
        }

        return urlBuilder.toString();
    }

    /**
     * This is used when building category url in services.
     *
     * @param delegator
     * @param wrapper
     * @param prefix
     * @param currentCategoryId
     * @param previousCategoryId
     * @param productId
     * @param viewSize
     * @param viewIndex
     * @param viewSort
     * @param searchString
     * @return
     */
    public static String makeCategoryUrl(Delegator delegator, CategoryContentWrapper wrapper, String prefix,
            String currentCategoryId, String previousCategoryId, String productId, String viewSize, String viewIndex,
            String viewSort, String searchString) {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(prefix);
        if (urlBuilder.charAt(urlBuilder.length() - 1) != '/') {
            urlBuilder.append("/");
        }
        urlBuilder.append(CatalogUrlServlet.CATEGORY_REQUEST + "/");

        if (UtilValidate.isNotEmpty(currentCategoryId)) {
            List<String> trail = null;
            trail = CategoryWorker.adjustTrail(null, currentCategoryId, previousCategoryId);
            if (trail != null && trail.size() > 1) {
                String lastCategoryId = trail.get(trail.size() - 1);
                if (!"TOP".equals(lastCategoryId)) {
                    String categoryName = CatalogUrlSeoTransform.getCategoryIdNameMap().get(lastCategoryId);
                    if (UtilValidate.isNotEmpty(categoryName)) {
                        urlBuilder.append(categoryName);
                        urlBuilder.append(URL_HYPHEN);
                        urlBuilder.append(lastCategoryId.trim().replaceAll(" ", URL_HYPHEN));
                    } else {
                        urlBuilder.append(lastCategoryId.trim().replaceAll(" ", URL_HYPHEN));
                    }
                }
            }
        }

        if (!urlBuilder.toString().endsWith("/") && UtilValidate.isNotEmpty(SeoConfigUtil.getCategoryUrlSuffix())) {
            urlBuilder.append(SeoConfigUtil.getCategoryUrlSuffix());
        }

        // append view index
        if (UtilValidate.isNotEmpty(viewIndex)) {
            if (!urlBuilder.toString().endsWith("?") && !urlBuilder.toString().endsWith("&")) {
                urlBuilder.append("?");
            }
            urlBuilder.append("viewIndex=" + viewIndex + "&");
        }
        // append view size
        if (UtilValidate.isNotEmpty(viewSize)) {
            if (!urlBuilder.toString().endsWith("?") && !urlBuilder.toString().endsWith("&")) {
                urlBuilder.append("?");
            }
            urlBuilder.append("viewSize=" + viewSize + "&");
        }
        // append view sort
        if (UtilValidate.isNotEmpty(viewSort)) {
            if (!urlBuilder.toString().endsWith("?") && !urlBuilder.toString().endsWith("&")) {
                urlBuilder.append("?");
            }
            urlBuilder.append("viewSort=" + viewSort + "&");
        }
        // append search string
        if (UtilValidate.isNotEmpty(searchString)) {
            if (!urlBuilder.toString().endsWith("?") && !urlBuilder.toString().endsWith("&")) {
                urlBuilder.append("?");
            }
            urlBuilder.append("searchString=" + searchString + "&");
        }
        if (urlBuilder.toString().endsWith("&")) {
            return urlBuilder.toString().substring(0, urlBuilder.toString().length()-1);
        }

        return urlBuilder.toString();
    }
}
