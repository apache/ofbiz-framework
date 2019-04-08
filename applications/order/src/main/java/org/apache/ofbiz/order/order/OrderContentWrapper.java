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
package org.apache.ofbiz.order.order;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilCodec;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.cache.UtilCache;
import org.apache.ofbiz.content.content.ContentWorker;
import org.apache.ofbiz.content.content.ContentWrapper;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.LocalDispatcher;

/**
 * Order Content Worker: gets order content to display
 *
 */
public class OrderContentWrapper implements ContentWrapper {

    public static final String module = OrderContentWrapper.class.getName();
    public static final String SEPARATOR = "::";    // cache key separator

    private static final UtilCache<String, String> orderContentCache = UtilCache.createUtilCache("order.content", true); // use soft reference to free up memory if needed

    public static OrderContentWrapper makeOrderContentWrapper(GenericValue order, HttpServletRequest request) {
        return new OrderContentWrapper(order, request);
    }

    protected LocalDispatcher dispatcher;
    protected GenericValue order;
    protected Locale locale;
    protected String mimeTypeId;

    public OrderContentWrapper(LocalDispatcher dispatcher, GenericValue order, Locale locale, String mimeTypeId) {
        this.dispatcher = dispatcher;
        this.order = order;
        this.locale = locale;
        this.mimeTypeId = mimeTypeId;
    }

    public OrderContentWrapper(GenericValue order, HttpServletRequest request) {
        this.dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        this.order = order;
        this.locale = UtilHttp.getLocale(request);
        this.mimeTypeId = EntityUtilProperties.getPropertyValue("content", "defaultMimeType", "text/html; charset=utf-8", (Delegator) request.getAttribute("delegator"));
    }

    public StringUtil.StringWrapper get(String orderContentTypeId, String encoderType) {
        return StringUtil.makeStringWrapper(getOrderContentAsText(order, orderContentTypeId, locale, mimeTypeId, order.getDelegator(), dispatcher, encoderType));
    }

    public static String getOrderContentAsText(GenericValue order, String orderContentTypeId, HttpServletRequest request, String encoderType) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        String mimeTypeId = EntityUtilProperties.getPropertyValue("content", "defaultMimeType", "text/html; charset=utf-8", order.getDelegator());
        return getOrderContentAsText(order, orderContentTypeId, UtilHttp.getLocale(request), mimeTypeId, order.getDelegator(), dispatcher, encoderType);
    }

    public static String getOrderContentAsText(GenericValue order, String orderContentTypeId, Locale locale, LocalDispatcher dispatcher, String encoderType) {
        return getOrderContentAsText(order, orderContentTypeId, locale, null, null, dispatcher, encoderType);
    }

    public static String getOrderContentAsText(GenericValue order, String orderContentTypeId, Locale locale, String mimeTypeId, Delegator delegator, LocalDispatcher dispatcher, String encoderType) {
        /* caching: there is one cache created, "order.content"  Each order's content is cached with a key of
         * contentTypeId::locale::mimeType::orderId::orderItemSeqId, or whatever the SEPARATOR is defined above to be.
         */
        UtilCodec.SimpleEncoder encoder = UtilCodec.getEncoder(encoderType);

        String orderItemSeqId = (order.getEntityName().equals("OrderItem")? order.getString("orderItemSeqId"): "_NA_");

        String cacheKey = orderContentTypeId + SEPARATOR + locale + SEPARATOR + mimeTypeId + SEPARATOR + order.get("orderId") + SEPARATOR + orderItemSeqId + SEPARATOR + encoderType + SEPARATOR + delegator;
        try {
            String cachedValue = orderContentCache.get(cacheKey);
            if (cachedValue != null) {
                return cachedValue;
            }

            Writer outWriter = new StringWriter();
            getOrderContentAsText(null, null, order, orderContentTypeId, locale, mimeTypeId, delegator, dispatcher, outWriter, false);
            String outString = outWriter.toString();
            if (UtilValidate.isEmpty(outString)) {
                outString = outString == null? "" : outString;
            }
            outString = encoder.sanitize(outString);
            if (orderContentCache != null) {
                orderContentCache.put(cacheKey, outString);
            }
            return outString;

        } catch (GeneralException e) {
            Debug.logError(e, "Error rendering OrderContent, inserting empty String", module);
            return "";
        } catch (IOException e) {
            Debug.logError(e, "Error rendering OrderContent, inserting empty String", module);
            return "";
        }
    }

    public static void getOrderContentAsText(String orderId, String orderItemSeqId, GenericValue order, String orderContentTypeId, Locale locale, String mimeTypeId, Delegator delegator, LocalDispatcher dispatcher, Writer outWriter) throws GeneralException, IOException {
        getOrderContentAsText(orderId, orderItemSeqId, order, orderContentTypeId, locale, mimeTypeId, delegator, dispatcher, outWriter, true);
    }

    public static void getOrderContentAsText(String orderId, String orderItemSeqId, GenericValue order, String orderContentTypeId, Locale locale, String mimeTypeId, Delegator delegator, LocalDispatcher dispatcher, Writer outWriter, boolean cache) throws GeneralException, IOException {
        if (orderId == null && order != null) {
            orderId = order.getString("orderId");
        }
        if (orderItemSeqId == null && order != null) {
            orderItemSeqId = (order.getEntityName().equals("OrderItem")? order.getString("orderItemSeqId"): "_NA_");
        }

        if (delegator == null && order != null) {
            delegator = order.getDelegator();
        }

        if (UtilValidate.isEmpty(mimeTypeId)) {
            mimeTypeId = EntityUtilProperties.getPropertyValue("content", "defaultMimeType", "text/html; charset=utf-8", delegator);
        }

        GenericValue orderContent = EntityQuery.use(delegator).from("OrderContent")
                .where("orderId", orderId,
                        "orderItemSeqId", orderItemSeqId,
                        "orderContentTypeId", orderContentTypeId)
                .orderBy("-fromDate")
                .cache(cache).filterByDate().queryFirst();
        if (orderContent != null) {
            // when rendering the order content, always include the OrderHeader/OrderItem and OrderContent records that this comes from
            Map<String, Object> inContext = new HashMap<String, Object>();
            inContext.put("order", order);
            inContext.put("orderContent", orderContent);
            ContentWorker.renderContentAsText(dispatcher, delegator, orderContent.getString("contentId"), outWriter, inContext, locale, mimeTypeId, null, null, cache);
        }
    }
}

