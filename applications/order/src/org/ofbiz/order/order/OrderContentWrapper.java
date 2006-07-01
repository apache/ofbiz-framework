/*
 * $Id: OrderContentWrapper.java 5462 2005-08-05 18:35:48Z jonesde $
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
package org.ofbiz.order.order;

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
import org.ofbiz.base.util.cache.UtilCache;
import org.ofbiz.content.content.ContentWorker;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;

/**
 * Order Content Worker: gets order content to display
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @author     <a href="mailto:tiz@sastau.it">Jacopo Cappellato</a>
 * @version    $Rev$
 * @since      3.0
 */
public class OrderContentWrapper {
    
    public static final String module = OrderContentWrapper.class.getName();
    public static final String SEPARATOR = "::";    // cache key separator
    
    public static UtilCache orderContentCache;
    
    public static OrderContentWrapper makeOrderContentWrapper(GenericValue order, HttpServletRequest request) {
        return new OrderContentWrapper(order, request);
    }
    
    protected GenericValue order;
    protected Locale locale;
    protected String mimeTypeId;
    
    public OrderContentWrapper(GenericValue order, Locale locale, String mimeTypeId) {
        this.order = order;
        this.locale = locale;
        this.mimeTypeId = mimeTypeId;
        if (orderContentCache == null) {
            orderContentCache = new UtilCache("order.content", true);     // use soft reference to free up memory if needed
        }
    }
    
    public OrderContentWrapper(GenericValue order, HttpServletRequest request) {
        this.order = order;
        this.locale = UtilHttp.getLocale(request);
        this.mimeTypeId = "text/html";
        if (orderContentCache == null) {
            orderContentCache = new UtilCache("order.content", true);     // use soft reference to free up memory if needed
        }
    }
    
    public String get(String orderContentTypeId) {
        return getOrderContentAsText(order, orderContentTypeId, locale, mimeTypeId, order.getDelegator());
    }
    
    public static String getOrderContentAsText(GenericValue order, String orderContentTypeId, HttpServletRequest request) {
        return getOrderContentAsText(order, orderContentTypeId, UtilHttp.getLocale(request), "text/html", order.getDelegator());
    }

    public static String getOrderContentAsText(GenericValue order, String orderContentTypeId, Locale locale) {
        return getOrderContentAsText(order, orderContentTypeId, locale, null, null);
    }
    
    public static String getOrderContentAsText(GenericValue order, String orderContentTypeId, Locale locale, String mimeTypeId, GenericDelegator delegator) {
        /* caching: there is one cache created, "order.content"  Each order's content is cached with a key of
         * contentTypeId::locale::mimeType::orderId::orderItemSeqId, or whatever the SEPARATOR is defined above to be.
         */
        String orderItemSeqId = (order.getEntityName().equals("OrderItem")? order.getString("orderItemSeqId"): "_NA_");
        
        String cacheKey = orderContentTypeId + SEPARATOR + locale + SEPARATOR + mimeTypeId + SEPARATOR + order.get("orderId") + SEPARATOR + orderItemSeqId;
        try {
            if (orderContentCache != null && orderContentCache.get(cacheKey) != null) {
                return (String) orderContentCache.get(cacheKey);
            }
            
            Writer outWriter = new StringWriter();
            getOrderContentAsText(null, null, order, orderContentTypeId, locale, mimeTypeId, delegator, outWriter);
            String outString = outWriter.toString();
            if (outString.length() > 0) {
                if (orderContentCache != null) {
                    orderContentCache.put(cacheKey, outString);
                }
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
    
    public static void getOrderContentAsText(String orderId, String orderItemSeqId, GenericValue order, String orderContentTypeId, Locale locale, String mimeTypeId, GenericDelegator delegator, Writer outWriter) throws GeneralException, IOException {
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
        
        List orderContentList = delegator.findByAndCache("OrderContent", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItemSeqId, "orderContentTypeId", orderContentTypeId), UtilMisc.toList("-fromDate"));
        orderContentList = EntityUtil.filterByDate(orderContentList);
        GenericValue orderContent = EntityUtil.getFirst(orderContentList);
        if (orderContent != null) {
            // when rendering the order content, always include the OrderHeader/OrderItem and OrderContent records that this comes from
            Map inContext = new HashMap();
            inContext.put("order", order);
            inContext.put("orderContent", orderContent);
            ContentWorker.renderContentAsText(delegator, orderContent.getString("contentId"), outWriter, inContext, null, locale, mimeTypeId);
        }
    }
}

