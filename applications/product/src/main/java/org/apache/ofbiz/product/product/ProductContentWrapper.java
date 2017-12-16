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

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.GeneralRuntimeException;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilCodec;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.cache.UtilCache;
import org.apache.ofbiz.content.content.ContentWorker;
import org.apache.ofbiz.content.content.ContentWrapper;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.model.ModelUtil;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.LocalDispatcher;

/**
 * Product Content Worker: gets product content to display
 */
public class ProductContentWrapper implements ContentWrapper {

    public static final String module = ProductContentWrapper.class.getName();
    public static final String SEPARATOR = "::";    // cache key separator

    private static final UtilCache<String, String> productContentCache = UtilCache.createUtilCache("product.content.rendered", true);

    public static ProductContentWrapper makeProductContentWrapper(GenericValue product, HttpServletRequest request) {
        return new ProductContentWrapper(product, request);
    }

    LocalDispatcher dispatcher;
    protected GenericValue product;
    protected Locale locale;
    protected String mimeTypeId;

    public ProductContentWrapper(LocalDispatcher dispatcher, GenericValue product, Locale locale, String mimeTypeId) {
        this.dispatcher = dispatcher;
        this.product = product;
        this.locale = locale;
        this.mimeTypeId = mimeTypeId;
    }

    public ProductContentWrapper(GenericValue product, HttpServletRequest request) {
        this.dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        this.product = product;
        this.locale = UtilHttp.getLocale(request);
        this.mimeTypeId = EntityUtilProperties.getPropertyValue("content", "defaultMimeType", "text/html; charset=utf-8", (Delegator) request.getAttribute("delegator"));
    }

    @Override
    public StringUtil.StringWrapper get(String productContentTypeId, String encoderType) {
        if (this.product == null) {
            Debug.logWarning("Tried to get ProductContent for type [" + productContentTypeId + "] but the product field in the ProductContentWrapper is null", module);
            return null;
        }
        return StringUtil.makeStringWrapper(getProductContentAsText(this.product, productContentTypeId, locale, mimeTypeId, null, null, this.product.getDelegator(), dispatcher, encoderType));
    }

    public static String getProductContentAsText(GenericValue product, String productContentTypeId, HttpServletRequest request, String encoderType) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        String mimeTypeId = EntityUtilProperties.getPropertyValue("content", "defaultMimeType", "text/html; charset=utf-8", product.getDelegator());
        return getProductContentAsText(product, productContentTypeId, UtilHttp.getLocale(request), mimeTypeId, null, null, product.getDelegator(), dispatcher, encoderType);
    }

    public static String getProductContentAsText(GenericValue product, String productContentTypeId, Locale locale, LocalDispatcher dispatcher, String encoderType) {
        return getProductContentAsText(product, productContentTypeId, locale, null, null, null, null, dispatcher, encoderType);
    }

    public static String getProductContentAsText(GenericValue product, String productContentTypeId, Locale locale, String mimeTypeId, String partyId,
            String roleTypeId, Delegator delegator, LocalDispatcher dispatcher, String encoderType) {
        if (product == null) {
            return null;
        }

        UtilCodec.SimpleEncoder encoder = UtilCodec.getEncoder(encoderType);
        String candidateFieldName = ModelUtil.dbNameToVarName(productContentTypeId);
        /* caching: there is one cache created, "product.content"  Each product's content is cached with a key of
         * contentTypeId::locale::mimeType::productId, or whatever the SEPARATOR is defined above to be.
         */
        String cacheKey = productContentTypeId + SEPARATOR + locale + SEPARATOR + mimeTypeId + SEPARATOR + product.get("productId") + SEPARATOR + encoderType + SEPARATOR + delegator;
        try {
            String cachedValue = productContentCache.get(cacheKey);
            if (cachedValue != null) {
                return cachedValue;
            }

            Writer outWriter = new StringWriter();
            getProductContentAsText(null, product, productContentTypeId, locale, mimeTypeId, partyId, roleTypeId, delegator, dispatcher, outWriter, false);
            String outString = outWriter.toString();
            if (UtilValidate.isEmpty(outString)) {
                outString = product.getModelEntity().isField(candidateFieldName) ? product.getString(candidateFieldName): "";
                outString = outString == null? "" : outString;
            }
            outString = encoder.sanitize(outString, null);
            productContentCache.put(cacheKey, outString);
            return outString;
        } catch (GeneralException | IOException e) {
            Debug.logError(e, "Error rendering ProductContent, inserting empty String", module);
            String candidateOut = product.getModelEntity().isField(candidateFieldName) ? product.getString(candidateFieldName): "";
            return candidateOut == null? "" : encoder.sanitize(candidateOut, null);
        }
    }

    public static void getProductContentAsText(String productId, GenericValue product, String productContentTypeId, Locale locale, String mimeTypeId, String partyId, String roleTypeId, Delegator delegator, LocalDispatcher dispatcher, Writer outWriter) throws GeneralException, IOException {
        getProductContentAsText(productId, product, productContentTypeId, locale, mimeTypeId, partyId, roleTypeId, delegator, dispatcher, outWriter, true);
    }

    public static void getProductContentAsText(String productId, GenericValue product, String productContentTypeId, Locale locale, String mimeTypeId, String partyId, String roleTypeId, Delegator delegator, LocalDispatcher dispatcher, Writer outWriter, boolean cache) throws GeneralException, IOException {
        if (productId == null && product != null) {
            productId = product.getString("productId");
        }

        if (delegator == null && product != null) {
            delegator = product.getDelegator();
        }

        if (UtilValidate.isEmpty(mimeTypeId)) {
            mimeTypeId = EntityUtilProperties.getPropertyValue("content", "defaultMimeType", "text/html; charset=utf-8", delegator);
        }

        if (delegator == null) {
            throw new GeneralRuntimeException("Unable to find a delegator to use!");
        }

        List<GenericValue> productContentList = EntityQuery.use(delegator).from("ProductContent").where("productId", productId, "productContentTypeId", productContentTypeId).orderBy("-fromDate").cache(cache).filterByDate().queryList();
        if (UtilValidate.isEmpty(productContentList) && ("Y".equals(product.getString("isVariant")))) {
            GenericValue parent = ProductWorker.getParentProduct(productId, delegator);
            if (parent != null) {
                productContentList = EntityQuery.use(delegator).from("ProductContent").where("productId", parent.get("productId"), "productContentTypeId", productContentTypeId).orderBy("-fromDate").cache(cache).filterByDate().queryList();
            }
        }
        GenericValue productContent = EntityUtil.getFirst(productContentList);
        if (productContent != null) {
            // when rendering the product content, always include the Product and ProductContent records that this comes from
            Map<String, Object> inContext = new HashMap<>();
            inContext.put("product", product);
            inContext.put("productContent", productContent);
            ContentWorker.renderContentAsText(dispatcher, productContent.getString("contentId"), outWriter, inContext, locale, mimeTypeId, partyId, roleTypeId, cache);
            return;
        }

        String candidateFieldName = ModelUtil.dbNameToVarName(productContentTypeId);
        ModelEntity productModel = delegator.getModelEntity("Product");
        if (product == null) {
            product = EntityQuery.use(delegator).from("Product").where("productId", productId).cache().queryOne();
        }
        if (UtilValidate.isEmpty(product)) {
            Debug.logWarning("No Product entity found for productId: " + productId, module);
            return;
        }

        if (productModel.isField(candidateFieldName)) {
                String candidateValue = product.getString(candidateFieldName);
                if (UtilValidate.isNotEmpty(candidateValue)) {
                    outWriter.write(candidateValue);
                    return;
                } else if ("Y".equals(product.getString("isVariant"))) {
                    // look up the virtual product
                    GenericValue parent = ProductWorker.getParentProduct(productId, delegator);
                    if (parent != null) {
                        candidateValue = parent.getString(candidateFieldName);
                        if (UtilValidate.isNotEmpty(candidateValue)) {
                            outWriter.write(candidateValue);
                            return;
                        }
                    }
                }
        }

    }
}
