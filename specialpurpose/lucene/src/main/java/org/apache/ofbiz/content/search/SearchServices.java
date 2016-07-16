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
package org.apache.ofbiz.content.search;

import java.lang.Object;
import java.lang.String;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.service.LocalDispatcher;

/**
 * SearchServices Class
 */
public class SearchServices {

    public static final String module = SearchServices.class.getName();
    public static final String resource = "ContentUiLabels";

    public static Map<String, Object> indexContentTree(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        String siteId = (String) context.get("contentId");
        Locale locale = (Locale) context.get("locale");
        try {
            SearchWorker.indexContentTree(dispatcher, delegator, siteId);
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                    "ContentIndexingTreeError", UtilMisc.toMap("errorString", e.toString()), locale));
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> indexProduct(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String productId = (String) context.get("productId");
        DocumentIndexer indexer = DocumentIndexer.getInstance(delegator, "products");
        indexer.queue(new ProductDocument(productId));
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> indexProductsFromFeature(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        try {
             // Only re-index the active appls, future dated ones will get picked up on that product's re-index date
             List<GenericValue> productFeatureAppls = EntityQuery.use(delegator).from("ProductFeatureAppl").where("productFeatureId", context.get("productFeatureId")).filterByDate().queryList();

            for (GenericValue productFeatureAppl : productFeatureAppls) {
                try {
                    dispatcher.runSync("indexProduct", UtilMisc.toMap("productId", productFeatureAppl.get("productId")));
                } catch (GenericServiceException e) {
                    Debug.logError(e, module);
                }
            }

        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> indexProductsFromProductAssoc(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        try {
            dispatcher.runSync("indexProduct", UtilMisc.toMap("productId", context.get("productId")));
            dispatcher.runSync("indexProduct", UtilMisc.toMap("productId", context.get("productIdTo")));
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> indexProductsFromDataResource(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        try {
            List<GenericValue> contents = EntityQuery.use(delegator).from("Content").where("dataResourceId", context.get("dataResourceId")).queryList();
            for (GenericValue content : contents) {
                dispatcher.runSync("indexProductsFromContent",
                        UtilMisc.toMap(
                                "userLogin", context.get("userLogin"),
                                "contentId", content.get("contentId")));
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
        }
        return ServiceUtil.returnSuccess();
    }
    public static Map<String, Object> indexProductsFromContent(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        try {
            List<GenericValue> productContents = EntityQuery.use(delegator).from("ProductContent").where("contentId", context.get("contentId")).queryList();
            for (GenericValue productContent : productContents) {
                try {
                    dispatcher.runSync("indexProduct", UtilMisc.toMap("productId", productContent.get("productId")));
                } catch (GenericServiceException e) {
                    Debug.logError(e, module);
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return ServiceUtil.returnSuccess();
    }
    public static Map<String, Object> indexProductsFromCategory(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        try {
            String productCategoryId = (String) context.get("productCategoryId");
            indexProductCategoryMembers(productCategoryId, delegator, dispatcher);
            indexProductCategoryRollup(productCategoryId, delegator, dispatcher, UtilMisc.<String>toSet(productCategoryId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return ServiceUtil.returnSuccess();
    }
    private static void indexProductCategoryRollup(String parentProductCategoryId, Delegator delegator, LocalDispatcher dispatcher, Set<String> excludeProductCategoryIds) throws GenericEntityException {
        List<GenericValue> productCategoryRollups = EntityQuery.use(delegator).from("ProductCategoryRollup").where("parentProductCategoryId", parentProductCategoryId).queryList();
        for (GenericValue productCategoryRollup : productCategoryRollups) {
            String productCategoryId = productCategoryRollup.getString("productCategoryId");
            // Avoid infinite recursion
            if (!excludeProductCategoryIds.add(productCategoryId)) {
                continue;
            }
            indexProductCategoryMembers(productCategoryId, delegator, dispatcher);
            indexProductCategoryRollup(productCategoryId, delegator, dispatcher, excludeProductCategoryIds);
        }
    }

    private static void indexProductCategoryMembers(String productCategoryId, Delegator delegator, LocalDispatcher dispatcher) throws GenericEntityException {
        List<GenericValue> productCategoryMembers = EntityQuery.use(delegator).from("ProductCategoryMember").where("productCategoryId", productCategoryId).queryList();
        for (GenericValue productCategoryMember : productCategoryMembers) {
            try {
                dispatcher.runSync("indexProduct", UtilMisc.toMap("productId", productCategoryMember.get("productId")));
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
            }
        }
    }

}
