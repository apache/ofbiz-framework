/*
 *
 *  Copyright (c) 2001, 2002 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.ofbiz.product.config;

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
import org.ofbiz.content.content.ContentWorker;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelUtil;
import org.ofbiz.entity.util.EntityUtil;

/**
 * Product Config Item Content Worker: gets product content to display
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @author     <a href="mailto:tiz@sastau.it">Jacopo Cappellato</a>
 */
public class ProductConfigItemContentWrapper implements java.io.Serializable {
    
    public static final String module = ProductConfigItemContentWrapper.class.getName();
    
    protected GenericValue productConfigItem;
    protected Locale locale;
    protected String mimeTypeId;
    
    public static ProductConfigItemContentWrapper makeProductConfigItemContentWrapper(GenericValue productConfigItem, HttpServletRequest request) {
        return new ProductConfigItemContentWrapper(productConfigItem, request);
    }
    
    public ProductConfigItemContentWrapper(GenericValue productConfigItem, Locale locale, String mimeTypeId) {
        this.productConfigItem = productConfigItem;
        this.locale = locale;
        this.mimeTypeId = mimeTypeId;
    }
    
    public ProductConfigItemContentWrapper(GenericValue productConfigItem, HttpServletRequest request) {
        this.productConfigItem = productConfigItem;
        this.locale = UtilHttp.getLocale(request);
        this.mimeTypeId = "text/html";
    }
    
    public String get(String confItemContentTypeId) {
        return getProductConfigItemContentAsText(productConfigItem, confItemContentTypeId, locale, mimeTypeId, productConfigItem.getDelegator());
    }
    
    public static String getProductConfigItemContentAsText(GenericValue productConfigItem, String confItemContentTypeId, HttpServletRequest request) {
        return getProductConfigItemContentAsText(productConfigItem, confItemContentTypeId, UtilHttp.getLocale(request), "text/html", productConfigItem.getDelegator());
    }

    public static String getProductConfigItemContentAsText(GenericValue productConfigItem, String confItemContentTypeId, Locale locale) {
        return getProductConfigItemContentAsText(productConfigItem, confItemContentTypeId, locale, null, null);
    }
    
    public static String getProductConfigItemContentAsText(GenericValue productConfigItem, String confItemContentTypeId, Locale locale, String mimeTypeId, GenericDelegator delegator) {
        String candidateFieldName = ModelUtil.dbNameToVarName(confItemContentTypeId);
        try {
            Writer outWriter = new StringWriter();
            getProductConfigItemContentAsText(null, productConfigItem, confItemContentTypeId, locale, mimeTypeId, delegator, outWriter);
            String outString = outWriter.toString();
            if (outString.length() > 0) {
                return outString;
            } else {
                return null;
            }
        } catch (GeneralException e) {
            Debug.logError(e, "Error rendering ProdConfItemContent, inserting empty String", module);
            return productConfigItem.getString(candidateFieldName);
        } catch (IOException e) {
            Debug.logError(e, "Error rendering ProdConfItemContent, inserting empty String", module);
            return productConfigItem.getString(candidateFieldName);
        }
    }
    
    public static void getProductConfigItemContentAsText(String configItemId, GenericValue productConfigItem, String confItemContentTypeId, Locale locale, String mimeTypeId, GenericDelegator delegator, Writer outWriter) throws GeneralException, IOException {
        if (configItemId == null && productConfigItem != null) {
            configItemId = productConfigItem.getString("configItemId");
        }
        
        if (delegator == null && productConfigItem != null) {
            delegator = productConfigItem.getDelegator();
        }
        
        if (UtilValidate.isEmpty(mimeTypeId)) {
            mimeTypeId = "text/html";
        }
        
        String candidateFieldName = ModelUtil.dbNameToVarName(confItemContentTypeId);
        //Debug.logInfo("candidateFieldName=" + candidateFieldName, module);
        ModelEntity productConfigItemModel = delegator.getModelEntity("ProductConfigItem");
        if (productConfigItemModel.isField(candidateFieldName)) {
            if (productConfigItem == null) {
                productConfigItem = delegator.findByPrimaryKeyCache("ProductConfigItem", UtilMisc.toMap("configItemId", configItemId));
            }
            if (productConfigItem != null) {
                String candidateValue = productConfigItem.getString(candidateFieldName);
                if (UtilValidate.isNotEmpty(candidateValue)) {
                    outWriter.write(candidateValue);
                    return;
                }
            }
        }
        
        List productConfigItemContentList = delegator.findByAndCache("ProdConfItemContent", UtilMisc.toMap("configItemId", configItemId, "confItemContentTypeId", confItemContentTypeId), UtilMisc.toList("-fromDate"));
        productConfigItemContentList = EntityUtil.filterByDate(productConfigItemContentList);
        GenericValue productConfigItemContent = EntityUtil.getFirst(productConfigItemContentList);
        if (productConfigItemContent != null) {
            // when rendering the product config item content, always include the ProductConfigItem and ProdConfItemContent records that this comes from
            Map inContext = new HashMap();
            inContext.put("productConfigItem", productConfigItem);
            inContext.put("productConfigItemContent", productConfigItemContent);
            ContentWorker.renderContentAsText(delegator, productConfigItemContent.getString("contentId"), outWriter, inContext, null, locale, mimeTypeId);
        }
    }
}

