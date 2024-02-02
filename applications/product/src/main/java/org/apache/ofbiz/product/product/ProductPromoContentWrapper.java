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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.GeneralRuntimeException;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.cache.UtilCache;
import org.apache.ofbiz.content.content.ContentWorker;
import org.apache.ofbiz.content.content.ContentWrapper;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.service.LocalDispatcher;

/**
 * Product Promo Content Worker: gets product promo content to display
 */
public class ProductPromoContentWrapper implements ContentWrapper {

    private static final String MODULE = ProductPromoContentWrapper.class.getName();

    private static final UtilCache<String, String> PRODUCT_PROMO_CONTENT_CACHE =
            UtilCache.createUtilCache("product.promo.content.rendered", true);

    public static ProductPromoContentWrapper makeProductPromoContentWrapper(GenericValue productPromo, HttpServletRequest request) {
        return new ProductPromoContentWrapper(productPromo, request);
    }

    private LocalDispatcher dispatcher;
    private GenericValue productPromo;
    private Locale locale;
    private String mimeTypeId;

    public ProductPromoContentWrapper(LocalDispatcher dispatcher, GenericValue productPromo, Locale locale, String mimeTypeId) {
        this.dispatcher = dispatcher;
        this.productPromo = productPromo;
        this.locale = locale;
        this.mimeTypeId = mimeTypeId;
    }

    public ProductPromoContentWrapper(GenericValue productPromo, HttpServletRequest request) {
        this.dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        this.productPromo = productPromo;
        this.locale = UtilHttp.getLocale(request);
        this.mimeTypeId = ContentWrapper.getDefaultMimeTypeId((Delegator) request.getAttribute("delegator"));
    }

    @Override
    public StringUtil.StringWrapper get(String productPromoContentTypeId, String encoderType) {
        if (UtilValidate.isEmpty(this.productPromo)) {
            Debug.logWarning("Tried to get ProductPromoContent for type [" + productPromoContentTypeId
                    + "] but the productPromo field in the ProductPromoContentWrapper is null", MODULE);
            return null;
        }
        return StringUtil.makeStringWrapper(getProductPromoContentAsText(this.productPromo, productPromoContentTypeId, locale, mimeTypeId, null,
                null, this.productPromo.getDelegator(), dispatcher, encoderType));
    }

    public static String getProductPromoContentAsText(GenericValue productPromo, String productPromoContentTypeId, HttpServletRequest request,
                                                      String encoderType) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        return getProductPromoContentAsText(productPromo, productPromoContentTypeId, UtilHttp.getLocale(request),
                ContentWrapper.getDefaultMimeTypeId(delegator),
                null, null, productPromo.getDelegator(), dispatcher, encoderType);
    }

    public static String getProductContentAsText(GenericValue productPromo, String productPromoContentTypeId, Locale locale, LocalDispatcher
            dispatcher, String encoderType) {
        return getProductPromoContentAsText(productPromo, productPromoContentTypeId, locale, null, null, null, null, dispatcher, encoderType);
    }

    public static String getProductPromoContentAsText(GenericValue productPromo, String productPromoContentTypeId, Locale locale, String mimeTypeId,
               String partyId, String roleTypeId, Delegator delegator, LocalDispatcher dispatcher, String encoderType) {
        if (UtilValidate.isEmpty(productPromo)) {
            return null;
        }

        /* Look for a previously cached entry (may also be an entry with null value if
         * there was no content to retrieve)
         */
        /* caching: there is one cache created, "product.promo.content.rendered"  Each productPromo's content is cached with a key of
         * contentTypeId::locale::mimeType::productPromoId, or whatever the CACHE_KEY_SEPARATOR is defined above to be.
         */
        String cacheKey = productPromoContentTypeId + CACHE_KEY_SEPARATOR + locale + CACHE_KEY_SEPARATOR + mimeTypeId
                + CACHE_KEY_SEPARATOR + productPromo.get("productPromoId")
                + CACHE_KEY_SEPARATOR + encoderType + CACHE_KEY_SEPARATOR + delegator;
        String cachedValue = PRODUCT_PROMO_CONTENT_CACHE.get(cacheKey);
        if (cachedValue != null || PRODUCT_PROMO_CONTENT_CACHE.containsKey(cacheKey)) {
            return cachedValue;
        }

        // Get content of given contentTypeId
        boolean doCache = true;
        String outString = null;
        try {
            Writer outWriter = new StringWriter();
            // Use cache == true to have entity-cache managed content from cache while (not managed) rendered cache above
            // may be configured with short expire time
            getProductPromoContentAsText(null, productPromo, productPromoContentTypeId, locale, mimeTypeId, partyId, roleTypeId,
                    delegator, dispatcher, outWriter, true);
            outString = outWriter.toString();
        } catch (GeneralException | IOException e) {
            Debug.logError(e, "Error rendering ProductPromoContent", MODULE);
            doCache = false;
        }

        /* If we did not found any content (or got an error), get the content of a
         * candidateFieldName matching the given contentTypeId
         */
        if (UtilValidate.isEmpty(outString)) {
            outString = ContentWrapper.getCandidateFieldValue(productPromo, productPromoContentTypeId);
        }
        // Encode found content via given encoderType
        outString = ContentWrapper.encodeContentValue(outString, encoderType);

        if (doCache) {
            PRODUCT_PROMO_CONTENT_CACHE.put(cacheKey, outString);
        }
        return outString;
    }

    public static void getProductPromoContentAsText(String productPromoId, GenericValue productPromo, String productPromoContentTypeId,
            Locale locale, String mimeTypeId, String partyId, String roleTypeId, Delegator delegator, LocalDispatcher dispatcher, Writer outWriter)
            throws GeneralException, IOException {
        getProductPromoContentAsText(productPromoId, productPromo, productPromoContentTypeId, locale, mimeTypeId, partyId, roleTypeId, delegator,
                dispatcher, outWriter, true);
    }

    public static void getProductPromoContentAsText(String productPromoId, GenericValue productPromo, String productPromoContentTypeId,
            Locale locale, String mimeTypeId, String partyId, String roleTypeId, Delegator delegator, LocalDispatcher dispatcher, Writer outWriter,
                                                    boolean cache) throws GeneralException, IOException {
        if (UtilValidate.isEmpty(productPromoId) && productPromo != null) {
            productPromoId = productPromo.getString("productPromoId");
        }

        if (UtilValidate.isEmpty(delegator) && productPromo != null) {
            delegator = productPromo.getDelegator();
        }

        if (UtilValidate.isEmpty(mimeTypeId)) {
            mimeTypeId = ContentWrapper.getDefaultMimeTypeId(delegator);
        }

        if (UtilValidate.isEmpty(delegator)) {
            throw new GeneralRuntimeException("Unable to find a delegator to use!");
        }

        List<EntityExpr> exprs = new ArrayList<>();
        exprs.add(EntityCondition.makeCondition("productPromoId", EntityOperator.EQUALS, productPromoId));
        exprs.add(EntityCondition.makeCondition("productPromoContentTypeId", EntityOperator.EQUALS, productPromoContentTypeId));

        List<GenericValue> productPromoContentList = EntityQuery.use(delegator).from("ProductPromoContent").where(EntityCondition
                .makeCondition(exprs, EntityOperator.AND)).orderBy("-fromDate").cache(cache).queryList();
        GenericValue productPromoContent = null;
        if (UtilValidate.isNotEmpty(productPromoContentList)) {
            productPromoContent = EntityUtil.getFirst(EntityUtil.filterByDate(productPromoContentList));
        }

        if (productPromoContent != null) {
            // when rendering the product promo content, always include the ProductPromo and ProductPromoContent records that this comes from
            Map<String, Object> inContext = new HashMap<>();
            inContext.put("productPromo", productPromo);
            inContext.put("productPromoContent", productPromoContent);
            ContentWorker.renderContentAsText(dispatcher, productPromoContent.getString("contentId"), outWriter,
                    inContext, locale, mimeTypeId, partyId, roleTypeId, cache);
        } else {
            String candidateValue = null;
            if (productPromo != null) {
                candidateValue = ContentWrapper.getCandidateFieldValue(productPromo, productPromoContentTypeId);
            } else {
                candidateValue = ContentWrapper.getCandidateFieldValue(delegator, "ProductPromo", EntityCondition
                        .makeCondition("productPromoId", productPromoId), productPromoContentTypeId, cache);
            }
            if (UtilValidate.isNotEmpty(candidateValue)) {
                outWriter.write(candidateValue);
            }
        }
    }
}
