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
package org.ofbiz.product.config;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.StringUtil.StringWrapper;
import org.ofbiz.base.util.UtilCodec;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.content.content.ContentWorker;
import org.ofbiz.content.content.ContentWrapper;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.DelegatorFactory;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelUtil;
import org.ofbiz.entity.util.EntityQuery;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceContainer;

/**
 * Product Config Item Content Worker: gets product content to display
 */
@SuppressWarnings("serial")
public class ProductConfigItemContentWrapper implements ContentWrapper {

    public static final String module = ProductConfigItemContentWrapper.class.getName();

    protected transient LocalDispatcher dispatcher;
    protected String dispatcherName;
    protected transient Delegator delegator;
    protected String delegatorName;
    protected GenericValue productConfigItem;
    protected Locale locale;
    protected String mimeTypeId;


    public static ProductConfigItemContentWrapper makeProductConfigItemContentWrapper(GenericValue productConfigItem, HttpServletRequest request) {
        return new ProductConfigItemContentWrapper(productConfigItem, request);
    }

    public ProductConfigItemContentWrapper(LocalDispatcher dispatcher, GenericValue productConfigItem, Locale locale, String mimeTypeId) {
        this.dispatcher = dispatcher;
        this.dispatcherName = dispatcher.getName();
        this.delegator = productConfigItem.getDelegator();
        this.delegatorName = delegator.getDelegatorName();
        this.productConfigItem = productConfigItem;
        this.locale = locale;
        this.mimeTypeId = mimeTypeId;
    }

    public ProductConfigItemContentWrapper(GenericValue productConfigItem, HttpServletRequest request) {
        this.dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        this.dispatcherName = dispatcher.getName();
        this.delegator = (Delegator) request.getAttribute("delegator");
        this.delegatorName = delegator.getDelegatorName();
        this.productConfigItem = productConfigItem;
        this.locale = UtilHttp.getLocale(request);
        this.mimeTypeId = "text/html";
    }

    public StringWrapper get(String confItemContentTypeId, String encoderType) {
        return StringUtil.makeStringWrapper(getProductConfigItemContentAsText(productConfigItem, confItemContentTypeId, locale, mimeTypeId, getDelegator(), getDispatcher(), encoderType));
    }

    public Delegator getDelegator() {
        if (delegator == null) {
            delegator = DelegatorFactory.getDelegator(delegatorName);
        }
        return delegator;
    }

    public LocalDispatcher getDispatcher() {
        if (dispatcher == null) {
            dispatcher = ServiceContainer.getLocalDispatcher(dispatcherName, this.getDelegator());
        }
        return dispatcher;
    }

    public static String getProductConfigItemContentAsText(GenericValue productConfigItem, String confItemContentTypeId, HttpServletRequest request, String encoderType) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        return getProductConfigItemContentAsText(productConfigItem, confItemContentTypeId, UtilHttp.getLocale(request), "text/html", productConfigItem.getDelegator(), dispatcher, encoderType);
    }

    public static String getProductConfigItemContentAsText(GenericValue productConfigItem, String confItemContentTypeId, Locale locale, LocalDispatcher dispatcher, String encoderType) {
        return getProductConfigItemContentAsText(productConfigItem, confItemContentTypeId, locale, null, null, dispatcher, encoderType);
    }

    public static String getProductConfigItemContentAsText(GenericValue productConfigItem, String confItemContentTypeId, Locale locale, String mimeTypeId, Delegator delegator, LocalDispatcher dispatcher, String encoderType) {
        UtilCodec.SimpleEncoder encoder = UtilCodec.getEncoder(encoderType);
        String candidateFieldName = ModelUtil.dbNameToVarName(confItemContentTypeId);
        try {
            Writer outWriter = new StringWriter();
            getProductConfigItemContentAsText(null, productConfigItem, confItemContentTypeId, locale, mimeTypeId, delegator, dispatcher, outWriter);
            String outString = outWriter.toString();
            if (outString.length() > 0) {
                return encoder.encode(outString);
            } else {
                String candidateOut = productConfigItem.getModelEntity().isField(candidateFieldName) ? productConfigItem.getString(candidateFieldName): "";
                return candidateOut == null? "" : encoder.encode(candidateOut);
            }
        } catch (GeneralException e) {
            Debug.logError(e, "Error rendering ProdConfItemContent, inserting empty String", module);
            String candidateOut = productConfigItem.getModelEntity().isField(candidateFieldName) ? productConfigItem.getString(candidateFieldName): "";
            return candidateOut == null? "" : encoder.encode(candidateOut);
        } catch (IOException e) {
            Debug.logError(e, "Error rendering ProdConfItemContent, inserting empty String", module);
            String candidateOut = productConfigItem.getModelEntity().isField(candidateFieldName) ? productConfigItem.getString(candidateFieldName): "";
            return candidateOut == null? "" : encoder.encode(candidateOut);
        }
    }

    public static void getProductConfigItemContentAsText(String configItemId, GenericValue productConfigItem, String confItemContentTypeId, Locale locale, String mimeTypeId, Delegator delegator, LocalDispatcher dispatcher, Writer outWriter) throws GeneralException, IOException {
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
                productConfigItem = EntityQuery.use(delegator).from("ProductConfigItem").where("configItemId", configItemId).cache().queryOne();
            }
            if (productConfigItem != null) {
                String candidateValue = productConfigItem.getString(candidateFieldName);
                if (UtilValidate.isNotEmpty(candidateValue)) {
                    outWriter.write(candidateValue);
                    return;
                }
            }
        }

        GenericValue productConfigItemContent = EntityQuery.use(delegator).from("ProdConfItemContent")
                .where("configItemId", configItemId, "confItemContentTypeId", confItemContentTypeId)
                .orderBy("-fromDate")
                .cache(true)
                .filterByDate()
                .queryFirst();
        if (productConfigItemContent != null) {
            // when rendering the product config item content, always include the ProductConfigItem and ProdConfItemContent records that this comes from
            Map<String, Object> inContext = new HashMap<String, Object>();
            inContext.put("productConfigItem", productConfigItem);
            inContext.put("productConfigItemContent", productConfigItemContent);
            ContentWorker.renderContentAsText(dispatcher, delegator, productConfigItemContent.getString("contentId"), outWriter, inContext, locale, mimeTypeId, null, null, false);
        }
    }
}

