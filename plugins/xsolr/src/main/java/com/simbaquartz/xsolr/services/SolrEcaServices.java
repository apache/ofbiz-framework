/*
 *
 *  * *****************************************************************************************
 *  *  Copyright (c) SimbaQuartz  2016. - All Rights Reserved                                 *
 *  *  Unauthorized copying of this file, via any medium is strictly prohibited               *
 *  *  Proprietary and confidential                                                           *
 *  *  Written by Mandeep Sidhu <mandeep.sidhu@simbacart.com>,  December, 2016                    *
 *  * ****************************************************************************************
 *
 */

package com.simbaquartz.xsolr.services;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by Mandeep on 4/26/2016.
 */
public class SolrEcaServices {
    public static final String module = SolrEcaServices.class.getName();

    /**
     * Invoked when a quote entity is created/modified, creates/updates solr indexes for configured SOLR CORE
     * @param dctx
     * @param context
     * @return
     */
    public static Map onQuoteCreateUpdate (DispatchContext dctx, Map<String, ?> context) {
        Debug.logInfo("Quote Updated " + context, module);

        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");

        GenericValue quote = (GenericValue) context.get("quoteInstance");
        if(UtilValidate.isNotEmpty(quote)) {
            //index Quote Data
            Map<String, Object> inputMap = new HashMap<String,Object>();
            inputMap.put("quoteId", quote.getString("quoteId"));
            inputMap.put("userLogin", context.get("userLogin"));

            try {
                Map<String, Object> servResult = dispatcher.runSync("indexQuoteInSolr", inputMap);
                Debug.logInfo("Index Quote in Solr Service Response: " + servResult, module);
            } catch (GenericServiceException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /* On addition/updation of product category. perform following functions:
     * 1) Update categoryUri in ProductCategory (for SEO friendly url)
     * 2) Update Solr Index
     * */
    public static Map onProductCategoryUpdate (DispatchContext dctx, Map<String, ?> context) {
        Debug.logInfo("Product Category Updated " + context, module);

        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");

        GenericValue productCategoryGv = (GenericValue) context.get("productCategoryInstance");
        if(productCategoryGv!=null) {

            Debug.logInfo("Current category Uri : " + productCategoryGv.getString("categoryUri"), module);
            //dispatcher.s
            Map<String, Object> inputMap = new HashMap<String,Object>();
            inputMap.put("rootCategoryId", productCategoryGv.getString("productCategoryId"));

            try {
                Map<String, Object> servResult = dispatcher.runSync("updateCategorySeoUri", inputMap);
                Debug.logInfo("Service Response: " + servResult, module);
            } catch (GenericServiceException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /* When product is updated - index the product in solr */
    public static Map onProductUpdate (DispatchContext dctx, Map<String, ?> context) {
        Debug.logInfo("Product Updated....", module);
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");

        GenericValue productGv = (GenericValue) context.get("productInstance");
        if(productGv!=null) {
            Map<String, Object> inputMap = new HashMap<String,Object>();
            inputMap.put("productId", productGv.getString("productId"));
            inputMap.put("productStoreId", "SC_STORE");

            try {
                Map<String, Object> servResult = dispatcher.runSync("indexProductInSolr", inputMap);
                Debug.logInfo("Index Product in Solr Service Response: " + servResult, module);
            } catch (GenericServiceException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /* When prroduct price is modified, index it in solr */

    public static Map onProductPriceUpdate(DispatchContext dctx, Map<String, ?> context) {
        Debug.logInfo("Product Price Updated....", module);
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");

        GenericValue productPriceGv = (GenericValue) context.get("productPriceInstance");
        System.out.println("Product price: " + productPriceGv);
        if(productPriceGv!=null) {
            Map<String, Object> inputMap = new HashMap<String,Object>();
            inputMap.put("productId", productPriceGv.getString("productId"));
            inputMap.put("productStoreId", "SC_STORE");

            try {
                Map<String, Object> servResult = dispatcher.runSync("indexProductInSolr", inputMap);
                Debug.logInfo("Index Product in Solr Service Response: " + servResult, module);
            } catch (GenericServiceException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /* When supplier product mapping info is updated */
    public static Map onSupplierProductUpdate(DispatchContext dctx, Map<String, ?> context) {
        Debug.logInfo("Supplier Product Info Updated....", module);
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");

        GenericValue supplierProductGv = (GenericValue) context.get("supplierProductInstance");
        System.out.println("Supplier Product: " + supplierProductGv);
        if(supplierProductGv!=null) {
            Map<String, Object> inputMap = new HashMap<String,Object>();
            inputMap.put("productId", supplierProductGv.getString("productId"));
            inputMap.put("productStoreId", "SC_STORE");

            try {
                Map<String, Object> servResult = dispatcher.runSync("indexProductInSolr", inputMap);
                Debug.logInfo("Index Product in Solr Service Response: " + servResult, module);
            } catch (GenericServiceException e) {
                e.printStackTrace();
            }
        }
        return result;
    }


    /* When product Assoc changes */
    public static Map onProductAssocUpdate(DispatchContext dctx, Map<String, ?> context) {
        Debug.logInfo("Product Assoc Info Updated....", module);
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");

        GenericValue prodAssoc = (GenericValue) context.get("productAssocInstance");
        if(prodAssoc!=null) {
            Map<String, Object> inputMap = new HashMap<String,Object>();
            inputMap.put("productId", prodAssoc.getString("productId"));
            inputMap.put("productStoreId", "SC_STORE");
            try {
                Map<String, Object> servResult = dispatcher.runSync("indexProductInSolr", inputMap);
                Debug.logInfo("Index Product in Solr Service Response: " + servResult, module);
            } catch (GenericServiceException e) {
                e.printStackTrace();
            }
        }
        return result;
    }
}
