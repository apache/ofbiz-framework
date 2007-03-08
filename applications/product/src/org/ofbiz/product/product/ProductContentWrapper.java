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
package org.ofbiz.product.product;

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
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.GeneralRuntimeException;
import org.ofbiz.base.util.cache.UtilCache;
import org.ofbiz.content.content.ContentWorker;
import org.ofbiz.content.content.ContentWrapper;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelUtil;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.LocalDispatcher;

/**
 * Product Content Worker: gets product content to display
 */
public class ProductContentWrapper implements ContentWrapper {
    
    public static final String module = ProductContentWrapper.class.getName();
    public static final String SEPARATOR = "::";    // cache key separator
    
    public static UtilCache productContentCache = new UtilCache("product.content.rendered", true);
    
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
        this.mimeTypeId = "text/html";
    }
    
    public String get(String productContentTypeId) {
        return getProductContentAsText(product, productContentTypeId, locale, mimeTypeId, product.getDelegator(), dispatcher);
    }
    
    public static String getProductContentAsText(GenericValue product, String productContentTypeId, HttpServletRequest request) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        return getProductContentAsText(product, productContentTypeId, UtilHttp.getLocale(request), "text/html", product.getDelegator(), dispatcher);
    }

    public static String getProductContentAsText(GenericValue product, String productContentTypeId, Locale locale, LocalDispatcher dispatcher) {
        return getProductContentAsText(product, productContentTypeId, locale, null, null, dispatcher);
    }
    
    public static String getProductContentAsText(GenericValue product, String productContentTypeId, Locale locale, String mimeTypeId, GenericDelegator delegator, LocalDispatcher dispatcher) {
        if (product == null) {
            return null;
        }
        
        String candidateFieldName = ModelUtil.dbNameToVarName(productContentTypeId);
        /* caching: there is one cache created, "product.content"  Each product's content is cached with a key of
         * contentTypeId::locale::mimeType::productId, or whatever the SEPARATOR is defined above to be.
         */  
        String cacheKey = productContentTypeId + SEPARATOR + locale + SEPARATOR + mimeTypeId + SEPARATOR + product.get("productId");
        try {
            if (productContentCache.get(cacheKey) != null) {
                return (String) productContentCache.get(cacheKey);
            }
            
            Writer outWriter = new StringWriter();
            getProductContentAsText(null, product, productContentTypeId, locale, mimeTypeId, delegator, dispatcher, outWriter);
            String outString = outWriter.toString();
            if (outString.length() > 0) {
                if (productContentCache != null) {
                    productContentCache.put(cacheKey, outString);
                }
                return outString;
            } else {
                String candidateOut = product.getModelEntity().isField(candidateFieldName) ? product.getString(candidateFieldName): "";
                return candidateOut == null? "" : candidateOut;
            }
        } catch (GeneralException e) {
            Debug.logError(e, "Error rendering ProductContent, inserting empty String", module);
            String candidateOut = product.getModelEntity().isField(candidateFieldName) ? product.getString(candidateFieldName): "";
            return candidateOut == null? "" : candidateOut;
        } catch (IOException e) {
            Debug.logError(e, "Error rendering ProductContent, inserting empty String", module);
            String candidateOut = product.getModelEntity().isField(candidateFieldName) ? product.getString(candidateFieldName): "";
            return candidateOut == null? "" : candidateOut;
        }
    }
    
    public static void getProductContentAsText(String productId, GenericValue product, String productContentTypeId, Locale locale, String mimeTypeId, GenericDelegator delegator, LocalDispatcher dispatcher, Writer outWriter) throws GeneralException, IOException {
        if (productId == null && product != null) {
            productId = product.getString("productId");
        }
        
        if (delegator == null && product != null) {
            delegator = product.getDelegator();
        }
        
        if (UtilValidate.isEmpty(mimeTypeId)) {
            mimeTypeId = "text/html";
        }

        if (delegator == null) {
            throw new GeneralRuntimeException("Unable to find a delegator to use!");
        }
        
        String candidateFieldName = ModelUtil.dbNameToVarName(productContentTypeId);        
        ModelEntity productModel = delegator.getModelEntity("Product");
        if (productModel.isField(candidateFieldName)) {
            if (product == null) {
                product = delegator.findByPrimaryKeyCache("Product", UtilMisc.toMap("productId", productId));
            }
            if (product != null) {
                String candidateValue = product.getString(candidateFieldName);
                if (UtilValidate.isNotEmpty(candidateValue)) {
                    outWriter.write(candidateValue);
                    return;
                }
            }
        }
        
        List productContentList = delegator.findByAndCache("ProductContent", UtilMisc.toMap("productId", productId, "productContentTypeId", productContentTypeId), UtilMisc.toList("-fromDate"));
        productContentList = EntityUtil.filterByDate(productContentList);
        GenericValue productContent = EntityUtil.getFirst(productContentList);
        if (productContent != null) {
            // when rendering the product content, always include the Product and ProductContent records that this comes from
            Map inContext = new HashMap();
            inContext.put("product", product);
            inContext.put("productContent", productContent);
            ContentWorker.renderContentAsText(dispatcher, delegator, productContent.getString("contentId"), outWriter, inContext, locale, mimeTypeId, false);
        }
    }
}
