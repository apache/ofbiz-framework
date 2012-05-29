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
package org.ofbiz.order.order;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.cache.UtilCache;
import org.ofbiz.content.content.ContentWorker;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.LocalDispatcher;

/**
 * Order Content Worker: gets order content to display
 *
 */
public class OrderContentWrapper {

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
        this.mimeTypeId = "text/html";
    }

    public String get(String orderContentTypeId) {
        return getOrderContentAsText(order, orderContentTypeId, locale, mimeTypeId, order.getDelegator(), dispatcher);
    }

    public static String getOrderContentAsText(GenericValue order, String orderContentTypeId, HttpServletRequest request) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        return getOrderContentAsText(order, orderContentTypeId, UtilHttp.getLocale(request), "text/html", order.getDelegator(), dispatcher);
    }

    public static String getOrderContentAsText(GenericValue order, String orderContentTypeId, Locale locale, LocalDispatcher dispatcher) {
        return getOrderContentAsText(order, orderContentTypeId, locale, null, null, dispatcher);
    }

    public static String getOrderContentAsText(GenericValue order, String orderContentTypeId, Locale locale, String mimeTypeId, Delegator delegator, LocalDispatcher dispatcher) {
        /* caching: there is one cache created, "order.content"  Each order's content is cached with a key of
         * contentTypeId::locale::mimeType::orderId::orderItemSeqId, or whatever the SEPARATOR is defined above to be.
         */
        String orderItemSeqId = (order.getEntityName().equals("OrderItem")? order.getString("orderItemSeqId"): "_NA_");

        String cacheKey = orderContentTypeId + SEPARATOR + locale + SEPARATOR + mimeTypeId + SEPARATOR + order.get("orderId") + SEPARATOR + orderItemSeqId;
        try {
            String cachedValue = orderContentCache.get(cacheKey);
            if (cachedValue != null) {
                return cachedValue;
            }

            Writer outWriter = new StringWriter();
            getOrderContentAsText(null, null, order, orderContentTypeId, locale, mimeTypeId, delegator, dispatcher, outWriter);
            String outString = outWriter.toString();
            if (outString.length() > 0) {
                outString = orderContentCache.putIfAbsentAndGet(cacheKey, outString);
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
            mimeTypeId = "text/html";
        }

        List<GenericValue> orderContentList = delegator.findByAnd("OrderContent", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItemSeqId, "orderContentTypeId", orderContentTypeId), UtilMisc.toList("-fromDate"), true);
        orderContentList = EntityUtil.filterByDate(orderContentList);
        GenericValue orderContent = EntityUtil.getFirst(orderContentList);
        if (orderContent != null) {
            // when rendering the order content, always include the OrderHeader/OrderItem and OrderContent records that this comes from
            Map<String, Object> inContext = FastMap.newInstance();
            inContext.put("order", order);
            inContext.put("orderContent", orderContent);
            ContentWorker.renderContentAsText(dispatcher, delegator, orderContent.getString("contentId"), outWriter, inContext, locale, mimeTypeId, null, null, false);
        }
    }
}

