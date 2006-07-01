/*
 * $Id: CategoryContentWrapper.java 7765 2006-06-10 15:48:12Z jonesde $
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
import org.ofbiz.content.content.ContentWorker;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelUtil;
import org.ofbiz.entity.util.EntityUtil;

/**
 * Category Content Worker: gets category content to display
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @author     <a href="mailto:arukala@gmx.de">Arukala</a>
 * @version    $Rev$
 * @since      3.0
 */
public class CategoryContentWrapper {
    
    public static final String module = CategoryContentWrapper.class.getName();
    
    protected GenericValue productCategory;
    protected Locale locale;
    protected String mimeTypeId;

    public static CategoryContentWrapper makeCategoryContentWrapper(GenericValue productCategory, HttpServletRequest request) {
        return new CategoryContentWrapper(productCategory, request);
    }
    
    public CategoryContentWrapper(GenericValue productCategory, Locale locale, String mimeTypeId) {
        this.productCategory = productCategory;
        this.locale = locale;
        this.mimeTypeId = mimeTypeId;       
    }
    
    public CategoryContentWrapper(GenericValue productCategory, HttpServletRequest request) {
        this.productCategory = productCategory;
        this.locale = UtilHttp.getLocale(request);
        this.mimeTypeId = "text/html";
        System.out.println(productCategory);
         System.out.println(locale);
         System.out.println(mimeTypeId);
    }
    
    public String get(String prodCatContentTypeId) {
        return getProductCategoryContentAsText(productCategory, prodCatContentTypeId, locale, mimeTypeId, productCategory.getDelegator());
    }
    
    public static String getProductCategoryContentAsText(GenericValue productCategory, String prodCatContentTypeId, HttpServletRequest request) {
        return getProductCategoryContentAsText(productCategory, prodCatContentTypeId, UtilHttp.getLocale(request), "text/html", productCategory.getDelegator());
    }

    public static String getProductCategoryContentAsText(GenericValue productCategory, String prodCatContentTypeId, Locale locale) {
        return getProductCategoryContentAsText(productCategory, prodCatContentTypeId, locale, null, null);
    }
    
    public static String getProductCategoryContentAsText(GenericValue productCategory, String prodCatContentTypeId, Locale locale, String mimeTypeId, GenericDelegator delegator) {
        String candidateFieldName = ModelUtil.dbNameToVarName(prodCatContentTypeId);
        try {
            Writer outWriter = new StringWriter();
            getProductCategoryContentAsText(null, productCategory, prodCatContentTypeId, locale, mimeTypeId, delegator, outWriter);
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
    
    public static void getProductCategoryContentAsText(String productCategoryId, GenericValue productCategory, String prodCatContentTypeId, Locale locale, String mimeTypeId, GenericDelegator delegator, Writer outWriter) throws GeneralException, IOException {
        if (productCategoryId == null && productCategory != null) {
            productCategoryId = productCategory.getString("productCategoryId");
        }
        
        if (delegator == null && productCategory != null) {
            delegator = productCategory.getDelegator();
        }
        
        if (UtilValidate.isEmpty(mimeTypeId)) {
            mimeTypeId = "text/html";
        }
        
        String candidateFieldName = ModelUtil.dbNameToVarName(prodCatContentTypeId);
//        Debug.logInfo("candidateFieldName=" + candidateFieldName, module);
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
            ContentWorker.renderContentAsText(delegator, categoryContent.getString("contentId"), outWriter, inContext, null, locale, mimeTypeId);
        }
    }
}
