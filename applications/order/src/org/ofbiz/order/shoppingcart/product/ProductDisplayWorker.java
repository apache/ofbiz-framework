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
package org.ofbiz.order.shoppingcart.product;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.order.shoppingcart.ShoppingCart;
import org.ofbiz.order.shoppingcart.ShoppingCartItem;
import org.ofbiz.product.catalog.CatalogWorker;
import org.ofbiz.product.category.CategoryWorker;


public class ProductDisplayWorker {

    public static final String module = ProductDisplayWorker.class.getName();

    /* ========================================================================================*/

    /* ============================= Special Data Retreival Methods ===========================*/

    public static List getRandomCartProductAssoc(ServletRequest request, boolean checkViewAllow) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        ShoppingCart cart = (ShoppingCart) httpRequest.getSession().getAttribute("shoppingCart");

        if (cart == null || cart.size() <= 0) return null;

        ArrayList cartAssocs = null;
        try {
            Map products = new HashMap();

            Iterator cartiter = cart.iterator();

            while (cartiter != null && cartiter.hasNext()) {
                ShoppingCartItem item = (ShoppingCartItem) cartiter.next();
                // Collection upgradeProducts = delegator.findByAndCache("ProductAssoc", UtilMisc.toMap("productId", item.getProductId(), "productAssocTypeId", "PRODUCT_UPGRADE"), null);
                List complementProducts = delegator.findByAndCache("ProductAssoc", UtilMisc.toMap("productId", item.getProductId(), "productAssocTypeId", "PRODUCT_COMPLEMENT"), null);
                // since ProductAssoc records have a fromDate and thruDate, we can filter by now so that only assocs in the date range are included
                complementProducts = EntityUtil.filterByDate(complementProducts);

                List productsCategories = delegator.findByAndCache("ProductCategoryMember", UtilMisc.toMap("productId", item.getProductId()), null);
                productsCategories = EntityUtil.filterByDate(productsCategories, true);
                if (productsCategories != null) {
                    Iterator productsCategoriesIter = productsCategories.iterator();
                    while (productsCategoriesIter.hasNext()) {
                        GenericValue productsCategoryMember = (GenericValue) productsCategoriesIter.next();
                        GenericValue productsCategory = productsCategoryMember.getRelatedOneCache("ProductCategory");
                        if ("CROSS_SELL_CATEGORY".equals(productsCategory.getString("productCategoryTypeId"))) {
                            List curPcms = productsCategory.getRelatedCache("ProductCategoryMember");
                            if (curPcms != null) {
                                Iterator curPcmsIter = curPcms.iterator();
                                while (curPcmsIter.hasNext()) {
                                    GenericValue curPcm = (GenericValue) curPcmsIter.next();
                                    if (!products.containsKey(curPcm.getString("productId"))) {
                                        GenericValue product = curPcm.getRelatedOneCache("Product");
                                        products.put(product.getString("productId"), product);
                                    }
                                }
                            }
                        }
                    }
                }

                if (UtilValidate.isNotEmpty(complementProducts)) {
                    Iterator complIter = complementProducts.iterator();
                    while (complIter.hasNext()) {
                        GenericValue productAssoc = (GenericValue) complIter.next();
                        if (!products.containsKey(productAssoc.getString("productIdTo"))) {
                            GenericValue product = productAssoc.getRelatedOneCache("AssocProduct");
                            products.put(product.getString("productId"), product);
                        }
                    }
                }
            }

            // remove all products that are already in the cart
            cartiter = cart.iterator();
            while (cartiter != null && cartiter.hasNext()) {
                ShoppingCartItem item = (ShoppingCartItem) cartiter.next();
                products.remove(item.getProductId());
            }

            // if desired check view allow category
            if (checkViewAllow) {
                String currentCatalogId = CatalogWorker.getCurrentCatalogId(request);
                String viewProductCategoryId = CatalogWorker.getCatalogViewAllowCategoryId(delegator, currentCatalogId);
                if (viewProductCategoryId != null) {
                    List tempList = new ArrayList(products.values());
                    tempList = CategoryWorker.filterProductsInCategory(delegator, tempList, viewProductCategoryId, "productId");
                    cartAssocs = new ArrayList(tempList);
                }
            }

            if (cartAssocs == null) {
                cartAssocs = new ArrayList(products.values());
            }

            // randomly remove products while there are more than 3
            while (cartAssocs.size() > 3) {
                int toRemove = (int) (Math.random() *  cartAssocs.size());
                cartAssocs.remove(toRemove);
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
        }

        if (UtilValidate.isNotEmpty(cartAssocs)) {
            return cartAssocs;
        } else {
            return null;
        }
    }

    public static Map getQuickReorderProducts(ServletRequest request) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        GenericValue userLogin = (GenericValue) httpRequest.getSession().getAttribute("userLogin");
        Map results = new HashMap();

        if (userLogin == null) userLogin = (GenericValue) httpRequest.getSession().getAttribute("autoUserLogin");
        if (userLogin == null) return results;

        try {
            Map products = (Map) httpRequest.getSession().getAttribute("_QUICK_REORDER_PRODUCTS_");
            Map productQuantities = (Map) httpRequest.getSession().getAttribute("_QUICK_REORDER_PRODUCT_QUANTITIES_");
            Map productOccurances = (Map) httpRequest.getSession().getAttribute("_QUICK_REORDER_PRODUCT_OCCURANCES_");

            if (products == null || productQuantities == null || productOccurances == null) {
                products = new HashMap();
                productQuantities = new HashMap();
                // keep track of how many times a product occurs in order to find averages and rank by purchase amount
                productOccurances = new HashMap();

                // get all order role entities for user by customer role type
                // final String[] USER_ORDER_ROLE_TYPES = {"END_USER_CUSTOMER", "SHIP_TO_CUSTOMER", "BILL_TO_CUSTOMER", "PLACING_CUSTOMER"};
                final String[] USER_ORDER_ROLE_TYPES = {"PLACING_CUSTOMER"};

                for (int i = 0; i < USER_ORDER_ROLE_TYPES.length; i++) {
                    Collection orderRoles = delegator.findByAnd("OrderRole", UtilMisc.toMap("partyId", userLogin.get("partyId"), "roleTypeId", USER_ORDER_ROLE_TYPES[i]), null);
                    Iterator ordersIter = UtilMisc.toIterator(orderRoles);

                    while (ordersIter != null && ordersIter.hasNext()) {
                        GenericValue orderRole = (GenericValue) ordersIter.next();
                        // for each order role get all order items
                        Collection orderItems = orderRole.getRelated("OrderItem");
                        Iterator orderItemsIter = UtilMisc.toIterator(orderItems);

                        while (orderItemsIter != null && orderItemsIter.hasNext()) {
                            GenericValue orderItem = (GenericValue) orderItemsIter.next();
                            String productId = orderItem.getString("productId");
                            if (UtilValidate.isNotEmpty(productId)) {
                                // for each order item get the associated product
                                GenericValue product = orderItem.getRelatedOneCache("Product");

                                products.put(product.get("productId"), product);

                                BigDecimal curQuant = (BigDecimal) productQuantities.get(product.get("productId"));

                                if (curQuant == null) curQuant = BigDecimal.ZERO;
                                BigDecimal orderQuant = orderItem.getBigDecimal("quantity");

                                if (orderQuant == null) orderQuant = BigDecimal.ZERO;
                                productQuantities.put(product.get("productId"), curQuant.add(orderQuant));

                                Integer curOcc = (Integer) productOccurances.get(product.get("productId"));

                                if (curOcc == null) curOcc = Integer.valueOf(0);
                                productOccurances.put(product.get("productId"), Integer.valueOf(curOcc.intValue() + 1));
                            }
                        }
                    }
                }

                // go through each product quantity and divide it by the occurances to get the average
                Iterator quantEntries = productQuantities.entrySet().iterator();

                while (quantEntries.hasNext()) {
                    Map.Entry entry = (Map.Entry) quantEntries.next();
                    Object prodId = entry.getKey();
                    BigDecimal quantity = (BigDecimal) entry.getValue();
                    Integer occs = (Integer) productOccurances.get(prodId);
                    BigDecimal nqint = quantity.divide(new BigDecimal(occs), new MathContext(10));

                    if (nqint.compareTo(BigDecimal.ONE) < 0) nqint = BigDecimal.ONE;
                    productQuantities.put(prodId, nqint);
                }

                httpRequest.getSession().setAttribute("_QUICK_REORDER_PRODUCTS_", new HashMap(products));
                httpRequest.getSession().setAttribute("_QUICK_REORDER_PRODUCT_QUANTITIES_", new HashMap(productQuantities));
                httpRequest.getSession().setAttribute("_QUICK_REORDER_PRODUCT_OCCURANCES_", new HashMap(productOccurances));
            } else {
                // make a copy since we are going to change them
                products = new HashMap(products);
                productQuantities = new HashMap(productQuantities);
                productOccurances = new HashMap(productOccurances);
            }

            // remove all products that are already in the cart
            ShoppingCart cart = (ShoppingCart) httpRequest.getSession().getAttribute("shoppingCart");
            if (UtilValidate.isNotEmpty(cart)) {
                Iterator cartiter = cart.iterator();
                while (cartiter.hasNext()) {
                    ShoppingCartItem item = (ShoppingCartItem) cartiter.next();
                    String productId = item.getProductId();
                    products.remove(productId);
                    productQuantities.remove(productId);
                    productOccurances.remove(productId);
                }
            }

            // if desired check view allow category
            //if (checkViewAllow) {
                List prodKeyList = new ArrayList(products.keySet());
                //Set prodKeySet = products.keySet();
                String currentCatalogId = CatalogWorker.getCurrentCatalogId(request);
                String viewProductCategoryId = CatalogWorker.getCatalogViewAllowCategoryId(delegator, currentCatalogId);
                if (viewProductCategoryId != null) {
                    Iterator valIter = prodKeyList.iterator();
                    while (valIter.hasNext()) {
                        String productId = (String) valIter.next();
                        if (!CategoryWorker.isProductInCategory(delegator, productId, viewProductCategoryId)) {
                            products.remove(productId);
                            productQuantities.remove(productId);
                            productOccurances.remove(productId);
                        }
                    }
                }
            //}

            List reorderProds = new ArrayList(products.values());

            /*
             //randomly remove products while there are more than 5
             while (reorderProds.size() > 5) {
             int toRemove = (int)(Math.random()*(double)(reorderProds.size()));
             reorderProds.remove(toRemove);
             }
             */

            // sort descending by new metric...
            BigDecimal occurancesModifier = BigDecimal.ONE;
            BigDecimal quantityModifier = BigDecimal.ONE;
            Map newMetric = new HashMap();
            Iterator occurEntries = productOccurances.entrySet().iterator();

            while (occurEntries.hasNext()) {
                Map.Entry entry = (Map.Entry) occurEntries.next();
                Object prodId = entry.getKey();
                Integer quantity = (Integer) entry.getValue();
                BigDecimal occs = (BigDecimal) productQuantities.get(prodId);
                BigDecimal nqdbl = quantityModifier.multiply(new BigDecimal(quantity)).add(occs.multiply(occurancesModifier));

                newMetric.put(prodId, nqdbl);
            }
            reorderProds = productOrderByMap(reorderProds, newMetric, true);

            // remove extra products - only return 5
            while (reorderProds.size() > 5) {
                reorderProds.remove(reorderProds.size() - 1);
            }

            results.put("products", reorderProds);
            results.put("quantities", productQuantities);
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
        }

        return results;
    }

    public static List productOrderByMap(Collection values, Map orderByMap, boolean descending) {
        if (values == null)  return null;
        if (values.size() == 0) return UtilMisc.toList(values);

        List result = new ArrayList(values);

        Collections.sort(result, new ProductByMapComparator(orderByMap, descending));
        return result;
    }

    static class ProductByMapComparator implements Comparator {
        private Map orderByMap;
        private boolean descending;

        ProductByMapComparator(Map orderByMap, boolean descending) {
            this.orderByMap = orderByMap;
            this.descending = descending;
        }

        public int compare(java.lang.Object prod1, java.lang.Object prod2) {
            int result = compareAsc((GenericEntity) prod1, (GenericEntity) prod2);

            if (descending) {
                result = -result;
            }
            return result;
        }

        private int compareAsc(GenericEntity prod1, GenericEntity prod2) {
            Object value = orderByMap.get(prod1.get("productId"));
            Object value2 = orderByMap.get(prod2.get("productId"));

            // null is defined as the smallest possible value
            if (value == null) return value2 == null ? 0 : -1;
            return ((Comparable) value).compareTo(value2);
        }

        @Override
        public boolean equals(java.lang.Object obj) {
            if ((obj != null) && (obj instanceof ProductByMapComparator)) {
                ProductByMapComparator that = (ProductByMapComparator) obj;

                return this.orderByMap.equals(that.orderByMap) && this.descending == that.descending;
            } else {
                return false;
            }
        }
    }
}
