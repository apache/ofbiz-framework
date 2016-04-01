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
package org.ofbiz.product.category;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.GeneralRuntimeException;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilCodec;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.cache.UtilCache;
import org.ofbiz.content.content.ContentWorker;
import org.ofbiz.content.content.ContentWrapper;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelUtil;
import org.ofbiz.entity.util.EntityQuery;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.entity.util.EntityUtilProperties;
import org.ofbiz.service.LocalDispatcher;

/**
 * Category Content Worker: gets category content to display
 */
public class CategoryContentWrapper implements ContentWrapper {

    public static final String module = CategoryContentWrapper.class.getName();
    public static final String SEPARATOR = "::";    // cache key separator
    private static final UtilCache<String, String> categoryContentCache = UtilCache.createUtilCache("category.content", true); // use soft reference to free up memory if needed

    protected LocalDispatcher dispatcher;
    protected GenericValue productCategory;
    protected Locale locale;
    protected String mimeTypeId;

    public static CategoryContentWrapper makeCategoryContentWrapper(GenericValue productCategory, HttpServletRequest request) {
        return new CategoryContentWrapper(productCategory, request);
    }

    public CategoryContentWrapper(LocalDispatcher dispatcher, GenericValue productCategory, Locale locale, String mimeTypeId) {
        this.dispatcher = dispatcher;
        this.productCategory = productCategory;
        this.locale = locale;
        this.mimeTypeId = mimeTypeId;
    }

    public CategoryContentWrapper(GenericValue productCategory, HttpServletRequest request) {
        this.dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        this.productCategory = productCategory;
        this.locale = UtilHttp.getLocale(request);
        this.mimeTypeId = EntityUtilProperties.getPropertyValue("content", "defaultMimeType", "text/html; charset=utf-8", (Delegator) request.getAttribute("delegator"));
    }

    public StringUtil.StringWrapper get(String prodCatContentTypeId, String encoderType) {
        return StringUtil.makeStringWrapper(getProductCategoryContentAsText(productCategory, prodCatContentTypeId, locale, mimeTypeId, productCategory.getDelegator(), dispatcher, encoderType));
    }

    public static String getProductCategoryContentAsText(GenericValue productCategory, String prodCatContentTypeId, HttpServletRequest request, String encoderType) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        String mimeTypeId = EntityUtilProperties.getPropertyValue("content", "defaultMimeType", "text/html; charset=utf-8", productCategory.getDelegator());
        return getProductCategoryContentAsText(productCategory, prodCatContentTypeId, UtilHttp.getLocale(request), mimeTypeId, productCategory.getDelegator(), dispatcher, encoderType);
    }

    public static String getProductCategoryContentAsText(GenericValue productCategory, String prodCatContentTypeId, Locale locale, LocalDispatcher dispatcher, String encoderType) {
        return getProductCategoryContentAsText(productCategory, prodCatContentTypeId, locale, null, null, dispatcher, encoderType);
    }

    public static String getProductCategoryContentAsText(GenericValue productCategory, String prodCatContentTypeId, Locale locale, String mimeTypeId, Delegator delegator, LocalDispatcher dispatcher, String encoderType) {
        String candidateFieldName = ModelUtil.dbNameToVarName(prodCatContentTypeId);
        UtilCodec.SimpleEncoder encoder = UtilCodec.getEncoder(encoderType);
        String cacheKey = prodCatContentTypeId + SEPARATOR + locale + SEPARATOR + mimeTypeId + SEPARATOR + productCategory.get("productCategoryId");
        try {
            String cachedValue = categoryContentCache.get(cacheKey);
            if (cachedValue != null) {
                return cachedValue;
            }
            Writer outWriter = new StringWriter();
            getProductCategoryContentAsText(null, productCategory, prodCatContentTypeId, locale, mimeTypeId, delegator, dispatcher, outWriter, false);
            String outString = outWriter.toString();
            if (UtilValidate.isEmpty(outString)) {
                outString = productCategory.getModelEntity().isField(candidateFieldName) ? productCategory.getString(candidateFieldName): "";
                outString = outString == null? "" : outString;
            }
            outString = encoder.sanitize(outString);
            if (categoryContentCache != null) {
                categoryContentCache.put(cacheKey, outString);
            }
            return outString;
        } catch (GeneralException e) {
            Debug.logError(e, "Error rendering CategoryContent, inserting empty String", module);
            return productCategory.getString(candidateFieldName);
        } catch (IOException e) {
            Debug.logError(e, "Error rendering CategoryContent, inserting empty String", module);
            return productCategory.getString(candidateFieldName);
        }
    }

    public static void getProductCategoryContentAsText(String productCategoryId, GenericValue productCategory, String prodCatContentTypeId, Locale locale, String mimeTypeId, Delegator delegator, LocalDispatcher dispatcher, Writer outWriter) throws GeneralException, IOException {
        getProductCategoryContentAsText(null, productCategory, prodCatContentTypeId, locale, mimeTypeId, delegator, dispatcher, outWriter, true);
    }

    public static void getProductCategoryContentAsText(String productCategoryId, GenericValue productCategory, String prodCatContentTypeId, Locale locale, String mimeTypeId, Delegator delegator, LocalDispatcher dispatcher, Writer outWriter, boolean cache) throws GeneralException, IOException {
        if (productCategoryId == null && productCategory != null) {
            productCategoryId = productCategory.getString("productCategoryId");
        }

        if (delegator == null && productCategory != null) {
            delegator = productCategory.getDelegator();
        }

        if (UtilValidate.isEmpty(mimeTypeId)) {
            mimeTypeId = EntityUtilProperties.getPropertyValue("content", "defaultMimeType", "text/html; charset=utf-8", delegator);
        }

        if (delegator == null) {
            throw new GeneralRuntimeException("Unable to find a delegator to use!");
        }

        String candidateFieldName = ModelUtil.dbNameToVarName(prodCatContentTypeId);
        ModelEntity categoryModel = delegator.getModelEntity("ProductCategory");
        if (categoryModel.isField(candidateFieldName)) {
            if (productCategory == null) {
                productCategory = EntityQuery.use(delegator).from("ProductCategory").where("productCategoryId", productCategoryId).cache().queryOne();
            }
            if (productCategory != null) {
                String candidateValue = productCategory.getString(candidateFieldName);
                if (UtilValidate.isNotEmpty(candidateValue)) {
                    outWriter.write(candidateValue);
                    return;
                }
            }
        }

        List<GenericValue> categoryContentList = EntityQuery.use(delegator).from("ProductCategoryContent").where("productCategoryId", productCategoryId, "prodCatContentTypeId", prodCatContentTypeId).orderBy("-fromDate").cache(cache).queryList();
        categoryContentList = EntityUtil.filterByDate(categoryContentList);
        
        GenericValue categoryContent = null;
        String sessionLocale = locale.toString();
        String fallbackLocale = UtilProperties.getFallbackLocale().toString();
        if ( sessionLocale == null ) sessionLocale = fallbackLocale;
        // look up all content found for locale
        for( GenericValue currentContent: categoryContentList ) {
            GenericValue content = EntityQuery.use(delegator).from("Content").where("contentId", currentContent.getString("contentId")).cache(cache).queryOne();
            if ( sessionLocale.equals(content.getString("localeString")) ) {
              // valid locale found
              categoryContent = currentContent;
              break;
            } else if ( fallbackLocale.equals(content.getString("localeString")) ) {
              // fall back to default locale
              categoryContent = currentContent;
            }
        }
        if (categoryContent != null) {
            // when rendering the category content, always include the Product Category and ProductCategoryContent records that this comes from
            Map<String, Object> inContext = new HashMap<String, Object>();
            inContext.put("productCategory", productCategory);
            inContext.put("categoryContent", categoryContent);
            ContentWorker.renderContentAsText(dispatcher, delegator, categoryContent.getString("contentId"), outWriter, inContext, locale, mimeTypeId, null, null, cache);
        }
    }
}
