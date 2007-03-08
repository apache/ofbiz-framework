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
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.GeneralRuntimeException;
import org.ofbiz.content.content.ContentWorker;
import org.ofbiz.content.content.ContentWrapper;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelUtil;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.LocalDispatcher;

/**
 * Category Content Worker: gets category content to display
 */
public class CategoryContentWrapper implements ContentWrapper {
    
    public static final String module = CategoryContentWrapper.class.getName();

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
        this.mimeTypeId = "text/html";
    }
    
    public String get(String prodCatContentTypeId) {
        return getProductCategoryContentAsText(productCategory, prodCatContentTypeId, locale, mimeTypeId, productCategory.getDelegator(), dispatcher);
    }
    
    public static String getProductCategoryContentAsText(GenericValue productCategory, String prodCatContentTypeId, HttpServletRequest request) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        return getProductCategoryContentAsText(productCategory, prodCatContentTypeId, UtilHttp.getLocale(request), "text/html", productCategory.getDelegator(), dispatcher);
    }

    public static String getProductCategoryContentAsText(GenericValue productCategory, String prodCatContentTypeId, Locale locale, LocalDispatcher dispatcher) {
        return getProductCategoryContentAsText(productCategory, prodCatContentTypeId, locale, null, null, dispatcher);
    }
    
    public static String getProductCategoryContentAsText(GenericValue productCategory, String prodCatContentTypeId, Locale locale, String mimeTypeId, GenericDelegator delegator, LocalDispatcher dispatcher) {
        String candidateFieldName = ModelUtil.dbNameToVarName(prodCatContentTypeId);
        try {
            Writer outWriter = new StringWriter();
            getProductCategoryContentAsText(null, productCategory, prodCatContentTypeId, locale, mimeTypeId, delegator, dispatcher, outWriter);
            String outString = outWriter.toString();
            if (outString.length() > 0) {
                return outString;
            } else {
                return null;
            }
        } catch (GeneralException e) {
            Debug.logError(e, "Error rendering CategoryContent, inserting empty String", module);
            return productCategory.getString(candidateFieldName);
        } catch (IOException e) {
            Debug.logError(e, "Error rendering CategoryContent, inserting empty String", module);
            return productCategory.getString(candidateFieldName);
        }
    }
    
    public static void getProductCategoryContentAsText(String productCategoryId, GenericValue productCategory, String prodCatContentTypeId, Locale locale, String mimeTypeId, GenericDelegator delegator, LocalDispatcher dispatcher, Writer outWriter) throws GeneralException, IOException {
        if (productCategoryId == null && productCategory != null) {
            productCategoryId = productCategory.getString("productCategoryId");
        }
        
        if (delegator == null && productCategory != null) {
            delegator = productCategory.getDelegator();
        }
        
        if (UtilValidate.isEmpty(mimeTypeId)) {
            mimeTypeId = "text/html";
        }

        if (delegator == null) {
            throw new GeneralRuntimeException("Unable to find a delegator to use!");
        }
        
        String candidateFieldName = ModelUtil.dbNameToVarName(prodCatContentTypeId);
        ModelEntity categoryModel = delegator.getModelEntity("ProductCategory");
        if (categoryModel.isField(candidateFieldName)) {
            if (productCategory == null) {
                productCategory = delegator.findByPrimaryKeyCache("ProductCategory", UtilMisc.toMap("productCategoryId", productCategoryId));
            }
            if (productCategory != null) {
                String candidateValue = productCategory.getString(candidateFieldName);
                if (UtilValidate.isNotEmpty(candidateValue)) {
                    outWriter.write(candidateValue);
                    return;
                }
            }
        }
        
        List categoryContentList = delegator.findByAndCache("ProductCategoryContent", UtilMisc.toMap("productCategoryId", productCategoryId, "prodCatContentTypeId", prodCatContentTypeId), UtilMisc.toList("-fromDate"));
        categoryContentList = EntityUtil.filterByDate(categoryContentList);
        GenericValue categoryContent = EntityUtil.getFirst(categoryContentList);
        if (categoryContent != null) {
            // when rendering the category content, always include the Product Category and ProductCategoryContent records that this comes from
            Map inContext = new HashMap();
            inContext.put("productCategory", productCategory);
            inContext.put("categoryContent", categoryContent);
            ContentWorker.renderContentAsText(dispatcher, delegator, categoryContent.getString("contentId"), outWriter, inContext, locale, mimeTypeId, false);
        }
    }
}
