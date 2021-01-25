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
package org.apache.ofbiz.order.shoppingcart.product;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilNumber;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntity;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.order.shoppingcart.ShoppingCart;
import org.apache.ofbiz.order.shoppingcart.ShoppingCartItem;
import org.apache.ofbiz.product.catalog.CatalogWorker;
import org.apache.ofbiz.product.category.CategoryWorker;
import org.apache.ofbiz.product.product.ProductWorker;


public final class ProductDisplayWorker {

    private static final String MODULE = ProductDisplayWorker.class.getName();

    private ProductDisplayWorker() { }

    /* ========================================================================================*/

    /* ============================= Special Data Retrieval Methods ===========================*/

    public static List<GenericValue> getRandomCartProductAssoc(ServletRequest request, boolean checkViewAllow) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        ShoppingCart cart = (ShoppingCart) httpRequest.getSession().getAttribute("shoppingCart");

        if (cart == null || cart.size() <= 0) {
            return null;
        }

        List<GenericValue> cartAssocs = null;
        try {
            Map<String, GenericValue> products = new HashMap<>();

            Iterator<ShoppingCartItem> cartiter = cart.iterator();

            while (cartiter != null && cartiter.hasNext()) {
                ShoppingCartItem item = cartiter.next();
                // since ProductAssoc records have a fromDate and thruDate, we can filter by now so that only assocs in the date range are included
                List<GenericValue> complementProducts = EntityQuery.use(delegator).from("ProductAssoc").where("productId", item.getProductId(),
                        "productAssocTypeId", "PRODUCT_COMPLEMENT").cache(true).filterByDate().queryList();

                List<GenericValue> productsCategories = EntityQuery.use(delegator).from("ProductCategoryMember").where("productId",
                        item.getProductId()).cache(true).filterByDate().queryList();
                if (productsCategories != null) {
                    for (GenericValue productsCategoryMember : productsCategories) {
                        GenericValue productsCategory = productsCategoryMember.getRelatedOne("ProductCategory", true);
                        if ("CROSS_SELL_CATEGORY".equals(productsCategory.getString("productCategoryTypeId"))) {
                            List<GenericValue> curPcms = productsCategory.getRelated("ProductCategoryMember", null, null, true);
                            if (curPcms != null) {
                                for (GenericValue curPcm : curPcms) {
                                    if (!products.containsKey(curPcm.getString("productId"))) {
                                        GenericValue product = curPcm.getRelatedOne("Product", true);
                                        products.put(product.getString("productId"), product);
                                    }
                                }
                            }
                        }
                    }
                }

                if (UtilValidate.isNotEmpty(complementProducts)) {
                    for (GenericValue productAssoc : complementProducts) {
                        if (!products.containsKey(productAssoc.getString("productIdTo"))) {
                            GenericValue product = productAssoc.getRelatedOne("AssocProduct", true);
                            products.put(product.getString("productId"), product);
                        }
                    }
                }
            }

            // remove all products that are already in the cart
            cartiter = cart.iterator();
            while (cartiter != null && cartiter.hasNext()) {
                ShoppingCartItem item = cartiter.next();
                products.remove(item.getProductId());
            }

            // if desired check view allow category
            if (checkViewAllow) {
                String currentCatalogId = CatalogWorker.getCurrentCatalogId(request);
                String viewProductCategoryId = CatalogWorker.getCatalogViewAllowCategoryId(delegator, currentCatalogId);
                if (viewProductCategoryId != null) {
                    List<GenericValue> tempList = new LinkedList<>();
                    tempList.addAll(products.values());
                    tempList = CategoryWorker.filterProductsInCategory(delegator, tempList, viewProductCategoryId, "productId");
                    cartAssocs = new LinkedList<>();
                    cartAssocs.addAll(tempList);
                }
            }

            if (cartAssocs == null) {
                cartAssocs = new LinkedList<>();
                cartAssocs.addAll(products.values());
            }

            // randomly remove products while there are more than 3
            while (cartAssocs.size() > 3) {
                int toRemove = (int) (Math.random() * cartAssocs.size());
                cartAssocs.remove(toRemove);
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e, MODULE);
        }

        if (UtilValidate.isNotEmpty(cartAssocs)) {
            return cartAssocs;
        }
        return null;
    }

    public static Map<String, Object> getQuickReorderProducts(ServletRequest request) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        GenericValue userLogin = (GenericValue) httpRequest.getSession().getAttribute("userLogin");
        Map<String, Object> results = new HashMap<>();

        if (userLogin == null) {
            userLogin = (GenericValue) httpRequest.getSession().getAttribute("autoUserLogin");
        }
        if (userLogin == null) {
            return results;
        }

        try {
            Map<String, GenericValue> products = UtilGenerics.cast(httpRequest.getSession().getAttribute("_QUICK_REORDER_PRODUCTS_"));
            Map<String, BigDecimal> productQuantities =
                    UtilGenerics.cast(httpRequest.getSession().getAttribute("_QUICK_REORDER_PRODUCT_QUANTITIES_"));
            Map<String, Integer> productOccurances = UtilGenerics.cast(httpRequest.getSession().getAttribute("_QUICK_REORDER_PRODUCT_OCCURANCES_"));

            if (products == null || productQuantities == null || productOccurances == null) {
                products = new HashMap<>();
                productQuantities = new HashMap<>();
                // keep track of how many times a product occurs in order to find averages and rank by purchase amount
                productOccurances = new HashMap<>();

                // get all order role entities for user by customer role type : PLACING_CUSTOMER
                List<GenericValue> orderRoles = EntityQuery.use(delegator).from("OrderRole").where("partyId", userLogin.get("partyId"),
                        "roleTypeId", "PLACING_CUSTOMER").queryList();
                Iterator<GenericValue> ordersIter = UtilMisc.toIterator(orderRoles);

                while (ordersIter != null && ordersIter.hasNext()) {
                    GenericValue orderRole = ordersIter.next();
                    // for each order role get all order items
                    List<GenericValue> orderItems = orderRole.getRelated("OrderItem", null, null, false);
                    Iterator<GenericValue> orderItemsIter = UtilMisc.toIterator(orderItems);

                    while (orderItemsIter != null && orderItemsIter.hasNext()) {
                        GenericValue orderItem = orderItemsIter.next();
                        String productId = orderItem.getString("productId");
                        if (UtilValidate.isNotEmpty(productId)) {
                            // for each order item get the associated product
                            GenericValue product = orderItem.getRelatedOne("Product", true);

                            products.put(product.getString("productId"), product);

                            BigDecimal curQuant = productQuantities.get(product.get("productId"));

                            if (curQuant == null) {
                                curQuant = BigDecimal.ZERO;
                            }
                            BigDecimal orderQuant = orderItem.getBigDecimal("quantity");

                            if (orderQuant == null) {
                                orderQuant = BigDecimal.ZERO;
                            }
                            productQuantities.put(product.getString("productId"), curQuant.add(orderQuant));

                            Integer curOcc = productOccurances.get(product.get("productId"));

                            if (curOcc == null) {
                                curOcc = 0;
                            }
                            productOccurances.put(product.getString("productId"), curOcc + 1);
                        }
                    }
                }

                // go through each product quantity and divide it by the occurances to get the average
                for (Map.Entry<String, BigDecimal> entry : productQuantities.entrySet()) {
                    String prodId = entry.getKey();
                    BigDecimal quantity = entry.getValue();
                    Integer occs = productOccurances.get(prodId);
                    BigDecimal nqint = quantity.divide(new BigDecimal(occs), new MathContext(10));

                    if (nqint.compareTo(BigDecimal.ONE) < 0) {
                        nqint = BigDecimal.ONE;
                    }
                    productQuantities.put(prodId, nqint);
                }

                httpRequest.getSession().setAttribute("_QUICK_REORDER_PRODUCTS_", new HashMap<>(products));
                httpRequest.getSession().setAttribute("_QUICK_REORDER_PRODUCT_QUANTITIES_", new HashMap<>(productQuantities));
                httpRequest.getSession().setAttribute("_QUICK_REORDER_PRODUCT_OCCURANCES_", new HashMap<>(productOccurances));
            } else {
                // make a copy since we are going to change them
                products = new HashMap<>(products);
                productQuantities = new HashMap<>(productQuantities);
                productOccurances = new HashMap<>(productOccurances);
            }

            // remove all products that are already in the cart
            ShoppingCart cart = (ShoppingCart) httpRequest.getSession().getAttribute("shoppingCart");
            if (UtilValidate.isNotEmpty(cart)) {
                for (ShoppingCartItem item : cart) {
                    String productId = item.getProductId();
                    products.remove(productId);
                    productQuantities.remove(productId);
                    productOccurances.remove(productId);
                }
            }

            // if desired check view allow category
            String currentCatalogId = CatalogWorker.getCurrentCatalogId(request);
            String viewProductCategoryId = CatalogWorker.getCatalogViewAllowCategoryId(delegator, currentCatalogId);
            if (viewProductCategoryId != null) {
                for (Map.Entry<String, GenericValue> entry : products.entrySet()) {
                    String productId = entry.getKey();
                    if (!CategoryWorker.isProductInCategory(delegator, productId, viewProductCategoryId)) {
                        products.remove(productId);
                        productQuantities.remove(productId);
                        productOccurances.remove(productId);
                    }
                }
            }

            List<GenericValue> reorderProds = new LinkedList<>();
            reorderProds.addAll(products.values());

            // sort descending by new metric...
            BigDecimal occurancesModifier = BigDecimal.ONE;
            BigDecimal quantityModifier = BigDecimal.ONE;
            Map<String, Object> newMetric = new HashMap<>();
            for (Map.Entry<String, Integer> entry : productOccurances.entrySet()) {
                String prodId = entry.getKey();
                Integer quantity = entry.getValue();
                BigDecimal occs = productQuantities.get(prodId);
                //For quantity we should test if we allow to add decimal quantity for this product an productStore : if not then round to 0
                if (!ProductWorker.isDecimalQuantityOrderAllowed(delegator, prodId, cart.getProductStoreId())) {
                    occs = occs.setScale(0, UtilNumber.getRoundingMode("order.rounding"));
                } else {
                    occs = occs.setScale(UtilNumber.getBigDecimalScale("order.decimals"), UtilNumber.getRoundingMode("order.rounding"));
                }
                productQuantities.put(prodId, occs);
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
            Debug.logWarning(e, MODULE);
        }

        return results;
    }

    public static List<GenericValue> productOrderByMap(List<GenericValue> values, Map<String, Object> orderByMap, boolean descending) {
        if (values == null) {
            return null;
        }
        if (values.isEmpty()) {
            return values;
        }

        List<GenericValue> result = new LinkedList<>(values);
        result.sort(new ProductByMapComparator(orderByMap, descending));
        return result;
    }

    private static class ProductByMapComparator implements Comparator<Object> {
        private Map<String, Object> orderByMap;
        private boolean descending;

        ProductByMapComparator(Map<String, Object> orderByMap, boolean descending) {
            this.orderByMap = orderByMap;
            this.descending = descending;
        }

        @Override
        public int compare(java.lang.Object prod1, java.lang.Object prod2) {
            int result = compareAsc((GenericEntity) prod1, (GenericEntity) prod2);

            if (descending) {
                result = -result;
            }
            return result;
        }

        @SuppressWarnings("unchecked")
        private int compareAsc(GenericEntity prod1, GenericEntity prod2) {
            Object value = orderByMap.get(prod1.get("productId"));
            Object value2 = orderByMap.get(prod2.get("productId"));

            // null is defined as the smallest possible value
            if (value == null) {
                return value2 == null ? 0 : -1;
            }
            return ((Comparable<Object>) value).compareTo(value2);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (descending ? 1231 : 1237);
            result = prime * result + ((orderByMap == null) ? 0 : orderByMap.hashCode());
            return result;
        }

        @Override
        public boolean equals(java.lang.Object obj) {
            if ((obj != null) && (obj instanceof ProductByMapComparator)) {
                ProductByMapComparator that = (ProductByMapComparator) obj;

                return this.orderByMap.equals(that.orderByMap) && this.descending == that.descending;
            }
            return false;
        }
    }
}
