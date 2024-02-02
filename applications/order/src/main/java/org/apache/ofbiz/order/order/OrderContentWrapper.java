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

    private static final String MODULE = OrderContentWrapper.class.getName();

    private static final UtilCache<String, String> ORDER_CONTENT_CACHE = UtilCache.createUtilCache(
            "order.content.rendered", true); // use soft reference to free up memory if needed

    public static OrderContentWrapper makeOrderContentWrapper(GenericValue order, HttpServletRequest request) {
        return new OrderContentWrapper(order, request);
    }

    private LocalDispatcher dispatcher;
    private GenericValue order;
    private Locale locale;
    private String mimeTypeId;

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
        this.mimeTypeId = ContentWrapper.getDefaultMimeTypeId((Delegator) request.getAttribute("delegator"));
    }

    @Override
    public StringUtil.StringWrapper get(String orderContentTypeId, String encoderType) {
        return StringUtil.makeStringWrapper(getOrderContentAsText(order, orderContentTypeId, locale, mimeTypeId, order.getDelegator(), dispatcher,
                encoderType));
    }

    public static String getOrderContentAsText(GenericValue order, String orderContentTypeId, HttpServletRequest request, String encoderType) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        String mimeTypeId = ContentWrapper.getDefaultMimeTypeId(order.getDelegator());
        return getOrderContentAsText(order, orderContentTypeId, UtilHttp.getLocale(request), mimeTypeId, order.getDelegator(), dispatcher,
                encoderType);
    }

    public static String getOrderContentAsText(GenericValue order, String orderContentTypeId, Locale locale, LocalDispatcher dispatcher,
                                               String encoderType) {
        return getOrderContentAsText(order, orderContentTypeId, locale, null, null, dispatcher, encoderType);
    }

    public static String getOrderContentAsText(GenericValue order, String orderContentTypeId, Locale locale, String mimeTypeId, Delegator delegator,
                                               LocalDispatcher dispatcher, String encoderType) {
        if (order == null) {
            return null;
        }
        String orderItemSeqId = ("OrderItem".equals(order.getEntityName()) ? order.getString("orderItemSeqId")
                : "_NA_");

        /* Look for a previously cached entry (may also be an entry with null value if
         * there was no content to retrieve)
         */
        /* caching: there is one cache created, "order.content"  Each order's content is cached with a key of
         * contentTypeId::locale::mimeType::orderId::orderItemSeqId, or whatever the CACHE_KEY_SEPARATOR is defined above to be.
         */

        String cacheKey = orderContentTypeId + CACHE_KEY_SEPARATOR + locale + CACHE_KEY_SEPARATOR + mimeTypeId + CACHE_KEY_SEPARATOR + order.get(
                "orderId") + CACHE_KEY_SEPARATOR + orderItemSeqId + CACHE_KEY_SEPARATOR + encoderType + CACHE_KEY_SEPARATOR + delegator;
        String cachedValue = ORDER_CONTENT_CACHE.get(cacheKey);
        if (cachedValue != null || ORDER_CONTENT_CACHE.containsKey(cacheKey)) {
            return cachedValue;
        }

        // Get content of given contentTypeId
        String outString = null;

        try {
            Writer outWriter = new StringWriter();
            getOrderContentAsText(null, orderItemSeqId, order, orderContentTypeId, locale, mimeTypeId, delegator,
                    dispatcher, outWriter, false);
            // Encode found content via given encoderType
            outString = ContentWrapper.encodeContentValue(outWriter.toString(), encoderType);
            ORDER_CONTENT_CACHE.put(cacheKey, outString);
        } catch (GeneralException | IOException e) {
            Debug.logError(e, "Error rendering OrderContent", MODULE);
        }
        return outString;
    }

    public static void getOrderContentAsText(String orderId, String orderItemSeqId, GenericValue order, String orderContentTypeId, Locale locale,
            String mimeTypeId, Delegator delegator, LocalDispatcher dispatcher, Writer outWriter) throws GeneralException, IOException {
        getOrderContentAsText(orderId, orderItemSeqId, order, orderContentTypeId, locale, mimeTypeId, delegator, dispatcher, outWriter, true);
    }

    public static void getOrderContentAsText(String orderId, String orderItemSeqId, GenericValue order, String orderContentTypeId, Locale locale,
            String mimeTypeId, Delegator delegator, LocalDispatcher dispatcher, Writer outWriter, boolean cache)
            throws GeneralException, IOException {
        if (orderId == null && order != null) {
            orderId = order.getString("orderId");
        }
        if (orderItemSeqId == null && order != null) {
            orderItemSeqId = ("OrderItem".equals(order.getEntityName()) ? order.getString("orderItemSeqId") : "_NA_");
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
            Map<String, Object> inContext = new HashMap<>();
            inContext.put("order", order);
            inContext.put("orderContent", orderContent);
            ContentWorker.renderContentAsText(dispatcher, orderContent.getString("contentId"), outWriter, inContext, locale,
                    mimeTypeId, null, null, cache);
        }
    }
}
