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

package org.apache.ofbiz.manufacturing.bom;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.order.order.OrderReadHelper;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

/** Bills of Materials' services implementation.
 * These services are useful when dealing with product's
 * bills of materials.
 */
public class BOMServices {

    public static final String module = BOMServices.class.getName();
    public static final String resource = "ManufacturingUiLabels";

    /** Returns the product's low level code (llc) i.e. the maximum depth
     * in which the productId can be found in any of the
     * bills of materials of bomType type.
     * If the bomType input field is not passed then the depth is searched for all the bom types and the lowest depth is returned.
     * @param dctx the dispatch context
     * @param context the context
     * @return returns the product's low level code (llc) i.e. the maximum depth
     */
    public static Map<String, Object> getMaxDepth(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        String productId = (String) context.get("productId");
        String fromDateStr = (String) context.get("fromDate");
        String bomType = (String) context.get("bomType");
        Locale locale = (Locale) context.get("locale");

        Date fromDate = null;
        if (UtilValidate.isNotEmpty(fromDateStr)) {
            try {
                fromDate = Timestamp.valueOf(fromDateStr);
            } catch (Exception e) {
            }
        }
        if (fromDate == null) {
            fromDate = new Date();
        }
        List<String> bomTypes = new LinkedList<>();
        if (bomType == null) {
            try {
                List<GenericValue> bomTypesValues = EntityQuery.use(delegator).from("ProductAssocType")
                        .where("parentTypeId", "PRODUCT_COMPONENT").queryList();
                for (GenericValue bomTypesValue : bomTypesValues) {
                    bomTypes.add(bomTypesValue.getString("productAssocTypeId"));
                }
            } catch (GenericEntityException gee) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingBomErrorRunningMaxDethAlgorithm", UtilMisc.toMap("errorString", gee.getMessage()), locale));
            }
        } else {
            bomTypes.add(bomType);
        }

        int depth = 0;
        int maxDepth = 0;
        try {
            for (String oneBomType : bomTypes) {
                depth = BOMHelper.getMaxDepth(productId, oneBomType, fromDate, delegator);
                if (depth > maxDepth) {
                    maxDepth = depth;
                }
            }
        } catch (GenericEntityException gee) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingBomErrorRunningMaxDethAlgorithm", UtilMisc.toMap("errorString", gee.getMessage()), locale));
        }
        result.put("depth", (long) maxDepth);

        return result;
    }

    /** Updates the product's low level code (llc)
     * Given a product id, computes and updates the product's low level code (field billOfMaterialLevel in Product entity).
     * It also updates the llc of all the product's descendants.
     * For the llc only the manufacturing bom ("MANUF_COMPONENT") is considered.
     * @param dctx the distach context
     * @param context the context 
     * @return the results of the updates the product's low level code 
    */
    public static Map<String, Object> updateLowLevelCode(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String productId = (String) context.get("productIdTo");
        Boolean alsoComponents = (Boolean) context.get("alsoComponents");
        Locale locale = (Locale) context.get("locale");
        if (alsoComponents == null) {
            alsoComponents = Boolean.TRUE;
        }
        Boolean alsoVariants = (Boolean) context.get("alsoVariants");
        if (alsoVariants == null) {
            alsoVariants = Boolean.TRUE;
        }

        Long llc = null;
        try {
            GenericValue product = EntityQuery.use(delegator).from("Product").where("productId", productId).queryOne();
            Map<String, Object> depthResult = dispatcher.runSync("getMaxDepth", 
                    UtilMisc.toMap("productId", productId, "bomType", "MANUF_COMPONENT"));
            if (ServiceUtil.isError(depthResult)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(depthResult));
            }
            llc = (Long)depthResult.get("depth");
            // If the product is a variant of a virtual, then the billOfMaterialLevel cannot be
            // lower than the billOfMaterialLevel of the virtual product.
            List<GenericValue> virtualProducts = EntityQuery.use(delegator).from("ProductAssoc")
                    .where("productIdTo", productId,
                            "productAssocTypeId", "PRODUCT_VARIANT")
                    .filterByDate().queryList();
            int virtualMaxDepth = 0;
            for (GenericValue oneVirtualProductAssoc : virtualProducts) {
                int virtualDepth = 0;
                GenericValue virtualProduct = EntityQuery.use(delegator).from("Product").where("productId", oneVirtualProductAssoc.getString("productId")).queryOne();
                if (virtualProduct.get("billOfMaterialLevel") != null) {
                    virtualDepth = virtualProduct.getLong("billOfMaterialLevel").intValue();
                } else {
                    virtualDepth = 0;
                }
                if (virtualDepth > virtualMaxDepth) {
                    virtualMaxDepth = virtualDepth;
                }
            }
            if (virtualMaxDepth > llc.intValue()) {
                llc = (long) virtualMaxDepth;
            }
            product.set("billOfMaterialLevel", llc);
            product.store();
            if (alsoComponents) {
                Map<String, Object> treeResult = dispatcher.runSync("getBOMTree", UtilMisc.toMap("productId", productId, "bomType", "MANUF_COMPONENT"));
                if (ServiceUtil.isError(treeResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(treeResult));
                }
                BOMTree tree = (BOMTree)treeResult.get("tree");
                List<BOMNode> products = new LinkedList<>();
                tree.print(products, llc.intValue());
                for (int i = 0; i < products.size(); i++) {
                    BOMNode oneNode = products.get(i);
                    GenericValue oneProduct = oneNode.getProduct();
                    int lev = 0;
                    if (oneProduct.get("billOfMaterialLevel") != null) {
                        lev = oneProduct.getLong("billOfMaterialLevel").intValue();
                    }
                    if (lev < oneNode.getDepth()) {
                        oneProduct.set("billOfMaterialLevel", (long) oneNode.getDepth());
                        oneProduct.store();
                    }
                }
            }
            if (alsoVariants) {
                List<GenericValue> variantProducts = EntityQuery.use(delegator).from("ProductAssoc")
                        .where("productId", productId, 
                                "productAssocTypeId", "PRODUCT_VARIANT")
                        .filterByDate().queryList();
                for (GenericValue oneVariantProductAssoc : variantProducts) {
                    GenericValue variantProduct = EntityQuery.use(delegator).from("Product").where("productId", oneVariantProductAssoc.getString("productId")).queryOne();
                    variantProduct.set("billOfMaterialLevel", llc);
                    variantProduct.store();
                }
            }
        } catch (GenericEntityException|GenericServiceException ge) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingBomErrorRunningUpdateLowLevelCode", UtilMisc.toMap("errorString", ge.getMessage()), locale));
        } catch (Exception e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingBomErrorRunningUpdateLowLevelCode", UtilMisc.toMap("errorString", e.getMessage()), locale));
        }
        result.put("lowLevelCode", llc);
        return result;
    }

    /** Updates the product's low level code (llc) for all the products in the Product entity.
     * For the llc only the manufacturing bom ("MANUF_COMPONENT") is considered.
     * @param dctx the distach context
     * @param context the context 
     * @return the results of the updates the product's low level code 
    */
    public static Map<String, Object> initLowLevelCode(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");

        try {
            List<GenericValue> products = EntityQuery.use(delegator).from("Product").orderBy("isVirtual DESC").queryList();
            Long zero = 0L;
            List<GenericValue> allProducts = new LinkedList<>();
            for (GenericValue product : products) {
                product.set("billOfMaterialLevel", zero);
                allProducts.add(product);
            }
            delegator.storeAll(allProducts);
            Debug.logInfo("Low Level Code set to 0 for all the products", module);

            for (GenericValue product : products) {
                try {
                    Map<String, Object> depthResult = dispatcher.runSync("updateLowLevelCode", UtilMisc.<String, Object>toMap("productIdTo", product.getString("productId"), "alsoComponents", Boolean.FALSE, "alsoVariants", Boolean.FALSE));
                    if (ServiceUtil.isError(depthResult)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(depthResult));
                    }
                    Debug.logInfo("Product [" + product.getString("productId") + "] Low Level Code [" + depthResult.get("lowLevelCode") + "]", module);
                } catch (Exception exc) {
                    Debug.logWarning(exc.getMessage(), module);
                }
            }
            // FIXME: also all the variants llc should be updated?
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingBomErrorRunningInitLowLevelCode", UtilMisc.toMap("errorString", e.getMessage()), locale));
        } catch (Exception e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingBomErrorRunningInitLowLevelCode", UtilMisc.toMap("errorString", e.getMessage()), locale));
        }
        return result;
    }

    /** Returns the ProductAssoc generic value for a duplicate productIdKey
     * ancestor if present, null otherwise.
     * Useful to avoid loops when adding new assocs (components)
     * to a bill of materials.
     * @param dctx the distach context
     * @param context the context 
     * @return returns the ProductAssoc generic value for a duplicate productIdKey ancestor if present 
     */
    public static Map<String, Object> searchDuplicatedAncestor(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue)context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        String productId = (String) context.get("productId");
        String productIdKey = (String) context.get("productIdTo");
        Timestamp fromDate = (Timestamp) context.get("fromDate");
        String bomType = (String) context.get("productAssocTypeId");
        if (fromDate == null) {
            fromDate = Timestamp.valueOf((new Date()).toString());
        }
        GenericValue duplicatedProductAssoc = null;
        try {
            duplicatedProductAssoc = BOMHelper.searchDuplicatedAncestor(productId, productIdKey, bomType, fromDate, delegator, dispatcher, userLogin);
        } catch (GenericEntityException gee) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingBomErrorRunningDuplicatedAncestorSearch", UtilMisc.toMap("errorString", gee.getMessage()), locale));
        }
        result.put("duplicatedProductAssoc", duplicatedProductAssoc);
        return result;
    }

    /** It reads the product's bill of materials,
     * if necessary configures it, and it returns
     * an object (see {@link BOMTree}
     * and {@link BOMNode}) that represents a
     * configured bill of material tree.
     * Useful for tree traversal (breakdown, explosion, implosion).
     * @param dctx the distach context
     * @param context the context 
     * @return return the bill of material tree
     */
    public static Map<String, Object> getBOMTree(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue)context.get("userLogin");
        String productId = (String) context.get("productId");
        String fromDateStr = (String) context.get("fromDate");
        String bomType = (String) context.get("bomType");
        Integer type = (Integer) context.get("type");
        BigDecimal quantity = (BigDecimal) context.get("quantity");
        BigDecimal amount = (BigDecimal) context.get("amount");
        Locale locale = (Locale) context.get("locale");
        if (type == null) {
            type = 0;
        }

        Date fromDate = null;
        if (UtilValidate.isNotEmpty(fromDateStr)) {
            try {
                fromDate = Timestamp.valueOf(fromDateStr);
            } catch (Exception e) {
            }
        }
        if (fromDate == null) {
            fromDate = new Date();
        }

        BOMTree tree;
        try {
            tree = new BOMTree(productId, bomType, fromDate, type, delegator, dispatcher, userLogin);
        } catch (GenericEntityException gee) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingBomErrorCreatingBillOfMaterialsTree", UtilMisc.toMap("errorString", gee.getMessage()), locale));
        }
        if (quantity != null) {
            tree.setRootQuantity(quantity);
        }
        if (amount != null) {
            tree.setRootAmount(amount);
        }
        result.put("tree", tree);

        return result;
    }

    /** It reads the product's bill of materials,
     * if necessary configures it, and it returns its (possibly configured) components in
     * a List of {@link BOMNode}).
     * @param dctx the distach context
     * @param context the context 
     * @return return the list of manufacturing components
     */
    public static Map<String, Object> getManufacturingComponents(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue)context.get("userLogin");
        String productId = (String) context.get("productId");
        BigDecimal quantity = (BigDecimal) context.get("quantity");
        BigDecimal amount = (BigDecimal) context.get("amount");
        String fromDateStr = (String) context.get("fromDate");
        Boolean excludeWIPs = (Boolean) context.get("excludeWIPs");
        Locale locale = (Locale) context.get("locale");

        if (quantity == null) {
            quantity = BigDecimal.ONE;
        }
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }

        Date fromDate = null;
        if (UtilValidate.isNotEmpty(fromDateStr)) {
            try {
                fromDate = Timestamp.valueOf(fromDateStr);
            } catch (Exception e) {
            }
        }
        if (fromDate == null) {
            fromDate = new Date();
        }
        if (excludeWIPs == null) {
            excludeWIPs = Boolean.TRUE;
        }

        //
        // Components
        //
        BOMTree tree = null;
        List<BOMNode> components = new LinkedList<>();
        try {
            tree = new BOMTree(productId, "MANUF_COMPONENT", fromDate, BOMTree.EXPLOSION_SINGLE_LEVEL, delegator, dispatcher, userLogin);
            tree.setRootQuantity(quantity);
            tree.setRootAmount(amount);
            tree.print(components, excludeWIPs);
            if (components.size() > 0) components.remove(0);
        } catch (GenericEntityException gee) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingBomErrorCreatingBillOfMaterialsTree", UtilMisc.toMap("errorString", gee.getMessage()), locale));
        }
        //
        // Product routing
        //
        String workEffortId = null;
        try {
            Map<String, Object> routingInMap = UtilMisc.toMap("productId", productId, "ignoreDefaultRouting", "Y", "userLogin", userLogin);
            Map<String, Object> routingOutMap = dispatcher.runSync("getProductRouting", routingInMap);
            if (ServiceUtil.isError(routingOutMap)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(routingOutMap));
            }
            GenericValue routing = (GenericValue)routingOutMap.get("routing");
            if (routing == null) {
                // try to find a routing linked to the virtual product
                routingInMap = UtilMisc.toMap("productId", tree.getRoot().getProduct().getString("productId"), "userLogin", userLogin);
                routingOutMap = dispatcher.runSync("getProductRouting", routingInMap);
                if (ServiceUtil.isError(routingOutMap)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(routingOutMap));
                }
                routing = (GenericValue)routingOutMap.get("routing");
            }
            if (routing != null) {
                workEffortId = routing.getString("workEffortId");
            }
        } catch (GenericServiceException gse) {
            Debug.logWarning(gse.getMessage(), module);
        }
        if (workEffortId != null) {
            result.put("workEffortId", workEffortId);
        }
        result.put("components", components);

        // also return a componentMap (useful in scripts and simple language code)
        List<Map<String, Object>> componentsMap = new LinkedList<>();
        for (BOMNode node : components) {
            Map<String, Object> componentMap = new HashMap<>();
            componentMap.put("product", node.getProduct());
            componentMap.put("quantity", node.getQuantity());
            componentsMap.add(componentMap);
        }
        result.put("componentsMap", componentsMap);
        return result;
    }

    public static Map<String, Object> getNotAssembledComponents(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String productId = (String) context.get("productId");
        BigDecimal quantity = (BigDecimal) context.get("quantity");
        BigDecimal amount = (BigDecimal) context.get("amount");
        String fromDateStr = (String) context.get("fromDate");
        GenericValue userLogin = (GenericValue)context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        if (quantity == null) {
            quantity = BigDecimal.ONE;
        }
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }

        Date fromDate = null;
        if (UtilValidate.isNotEmpty(fromDateStr)) {
            try {
                fromDate = Timestamp.valueOf(fromDateStr);
            } catch (Exception e) {
            }
        }
        if (fromDate == null) {
            fromDate = new Date();
        }

        BOMTree tree = null;
        List<BOMNode> components = new LinkedList<>();
        List<BOMNode> notAssembledComponents = new LinkedList<>();
        try {
            tree = new BOMTree(productId, "MANUF_COMPONENT", fromDate, BOMTree.EXPLOSION_MANUFACTURING, delegator, dispatcher, userLogin);
            tree.setRootQuantity(quantity);
            tree.setRootAmount(amount);
            tree.print(components);
        } catch (GenericEntityException gee) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingBomErrorCreatingBillOfMaterialsTree", UtilMisc.toMap("errorString", gee.getMessage()), locale));
        }
        for (BOMNode oneComponent : components) {
            if (!oneComponent.isManufactured()) {
                notAssembledComponents.add(oneComponent);
            }
        }
        result.put("notAssembledComponents" , notAssembledComponents);
        return result;
    }

    // ---------------------------------------------
    // Service for the Product (Shipment) component
    //
    public static Map<String, Object> createShipmentPackages(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue)context.get("userLogin");
        String shipmentId = (String) context.get("shipmentId");

        try {
            List<GenericValue> packages = EntityQuery.use(delegator).from("ShipmentPackage").where("shipmentId", shipmentId).queryList();
            if (UtilValidate.isNotEmpty(packages)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingBomPackageAlreadyFound", locale));
            }
        } catch (GenericEntityException gee) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingBomErrorLoadingShipmentPackages", locale));
        }
        // ShipmentItems are loaded
        List<GenericValue> shipmentItems = null;
        try {
            shipmentItems = EntityQuery.use(delegator).from("ShipmentItem").where("shipmentId", shipmentId).queryList();
        } catch (GenericEntityException gee) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingBomErrorLoadingShipmentItems", locale));
        }
        Map<String, Object> orderReadHelpers = new HashMap<>();
        Map<String, Object> partyOrderShipments = new HashMap<>();
        for (GenericValue shipmentItem : shipmentItems) {
            // Get the OrderShipments
            GenericValue orderShipment = null;
            try {
                orderShipment = EntityQuery.use(delegator).from("OrderShipment")
                        .where("shipmentId", shipmentId, 
                                "shipmentItemSeqId", shipmentItem.get("shipmentItemSeqId"))
                        .queryFirst();
            } catch (GenericEntityException e) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingPackageConfiguratorError", locale));
            }
            if (orderShipment != null && !orderReadHelpers.containsKey(orderShipment.getString("orderId"))) {
                orderReadHelpers.put(orderShipment.getString("orderId"), new OrderReadHelper(delegator, orderShipment.getString("orderId")));
            }
            OrderReadHelper orderReadHelper = (OrderReadHelper)orderReadHelpers.get(orderShipment.getString("orderId"));
            if (orderReadHelper != null) {
                Map<String, Object> orderShipmentReadMap = UtilMisc.toMap("orderShipment", orderShipment, "orderReadHelper", orderReadHelper);
                String partyId = (orderReadHelper.getPlacingParty() != null? orderReadHelper.getPlacingParty().getString("partyId"): null); // FIXME: is it the customer?
                if (partyId != null) {
                    if (!partyOrderShipments.containsKey(partyId)) {
                        List<Map<String, Object>> orderShipmentReadMapList = new LinkedList<>();
                        partyOrderShipments.put(partyId, orderShipmentReadMapList);
                    }
                    List<Map<String, Object>> orderShipmentReadMapList = UtilGenerics.checkList(partyOrderShipments.get(partyId));
                    orderShipmentReadMapList.add(orderShipmentReadMap);
                }
            }
        }
        // For each party: try to expand the shipment item products
        // (search for components that needs to be packaged).
        for (Map.Entry<String, Object> partyOrderShipment : partyOrderShipments.entrySet()) {
            List<Map<String, Object>> orderShipmentReadMapList = UtilGenerics.checkList(partyOrderShipment.getValue());
            for (int i = 0; i < orderShipmentReadMapList.size(); i++) {
                Map<String, Object> orderShipmentReadMap = UtilGenerics.checkMap(orderShipmentReadMapList.get(i));
                GenericValue orderShipment = (GenericValue)orderShipmentReadMap.get("orderShipment");
                OrderReadHelper orderReadHelper = (OrderReadHelper)orderShipmentReadMap.get("orderReadHelper");
                GenericValue orderItem = orderReadHelper.getOrderItem(orderShipment.getString("orderItemSeqId"));
                // getProductsInPackages
                Map<String, Object> serviceContext = new HashMap<>();
                serviceContext.put("productId", orderItem.getString("productId"));
                serviceContext.put("quantity", orderShipment.getBigDecimal("quantity"));
                Map<String, Object> serviceResult = null;
                try {
                    serviceResult = dispatcher.runSync("getProductsInPackages", serviceContext);
                    if (ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingPackageConfiguratorError", locale));
                    }
                } catch (GenericServiceException e) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingPackageConfiguratorError", locale));
                }
                List<BOMNode> productsInPackages = UtilGenerics.checkList(serviceResult.get("productsInPackages"));
                if (productsInPackages.size() == 1) {
                    BOMNode root = productsInPackages.get(0);
                    String rootProductId = (root.getSubstitutedNode() != null? root.getSubstitutedNode().getProduct().getString("productId"): root.getProduct().getString("productId"));
                    if (orderItem.getString("productId").equals(rootProductId)) {
                        productsInPackages = null;
                    }
                }
                if (productsInPackages != null && productsInPackages.size() == 0) {
                    productsInPackages = null;
                }
                if (UtilValidate.isNotEmpty(productsInPackages)) {
                    orderShipmentReadMap.put("productsInPackages", productsInPackages);
                }
            }
        }
        // Group together products and components
        // of the same box type.
        Map<String, GenericValue> boxTypes = new HashMap<>();
        for (Map.Entry<String, Object> partyOrderShipment : partyOrderShipments.entrySet()) {
            Map<String, List<Map<String, Object>>> boxTypeContent = new HashMap<>();
            List<Map<String, Object>> orderShipmentReadMapList = UtilGenerics.checkList(partyOrderShipment.getValue());
            for (int i = 0; i < orderShipmentReadMapList.size(); i++) {
                Map<String, Object> orderShipmentReadMap = UtilGenerics.checkMap(orderShipmentReadMapList.get(i));
                GenericValue orderShipment = (GenericValue)orderShipmentReadMap.get("orderShipment");
                OrderReadHelper orderReadHelper = (OrderReadHelper)orderShipmentReadMap.get("orderReadHelper");
                List<BOMNode> productsInPackages = UtilGenerics.checkList(orderShipmentReadMap.get("productsInPackages"));
                if (productsInPackages != null) {
                    // there are subcomponents:
                    // this is a multi package shipment item
                    for (int j = 0; j < productsInPackages.size(); j++) {
                        BOMNode component = productsInPackages.get(j);
                        Map<String, Object> boxTypeContentMap = new HashMap<>();
                        boxTypeContentMap.put("content", orderShipmentReadMap);
                        boxTypeContentMap.put("componentIndex", j);
                        GenericValue product = component.getProduct();
                        String boxTypeId = product.getString("shipmentBoxTypeId");
                        if (boxTypeId != null) {
                            if (!boxTypes.containsKey(boxTypeId)) {
                                GenericValue boxType = null;
                                try {
                                    boxType = EntityQuery.use(delegator).from("ShipmentBoxType").where("shipmentBoxTypeId", boxTypeId).queryOne();
                                } catch (GenericEntityException e) {
                                    return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingPackageConfiguratorError", locale));
                                }
                                boxTypes.put(boxTypeId, boxType);
                                List<Map<String, Object>> box = new LinkedList<>();
                                boxTypeContent.put(boxTypeId, box);
                            }
                            List<Map<String, Object>> boxTypeContentList = UtilGenerics.checkList(boxTypeContent.get(boxTypeId));
                            boxTypeContentList.add(boxTypeContentMap);
                        }
                    }
                } else {
                    // no subcomponents, the product has its own package:
                    // this is a single package shipment item
                    Map<String, Object> boxTypeContentMap = new HashMap<>();
                    boxTypeContentMap.put("content", orderShipmentReadMap);
                    GenericValue orderItem = orderReadHelper.getOrderItem(orderShipment.getString("orderItemSeqId"));
                    GenericValue product = null;
                    try {
                        product = orderItem.getRelatedOne("Product", false);
                    } catch (GenericEntityException e) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingPackageConfiguratorError", locale));
                    }
                    String boxTypeId = product.getString("shipmentBoxTypeId");
                    if (boxTypeId != null) {
                        if (!boxTypes.containsKey(boxTypeId)) {
                            GenericValue boxType = null;
                            try {
                                boxType = EntityQuery.use(delegator).from("ShipmentBoxType").where("shipmentBoxTypeId", boxTypeId).queryOne();
                            } catch (GenericEntityException e) {
                                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingPackageConfiguratorError", locale));
                            }

                            boxTypes.put(boxTypeId, boxType);
                            List<Map<String, Object>> box = new LinkedList<>();
                            boxTypeContent.put(boxTypeId, box);
                        }
                        List<Map<String, Object>> boxTypeContentList = UtilGenerics.checkList(boxTypeContent.get(boxTypeId));
                        boxTypeContentList.add(boxTypeContentMap);
                    }
                }
            }
            // The packages and package contents are created.
            for (Map.Entry<String, List<Map<String, Object>>> boxTypeContentEntry : boxTypeContent.entrySet()) {
                String boxTypeId = boxTypeContentEntry.getKey();
                List<Map<String, Object>> contentList = UtilGenerics.checkList(boxTypeContentEntry.getValue());
                GenericValue boxType = boxTypes.get(boxTypeId);
                BigDecimal boxWidth = boxType.getBigDecimal("boxLength");
                BigDecimal totalWidth = BigDecimal.ZERO;
                if (boxWidth == null) {
                    boxWidth = BigDecimal.ZERO;
                }
                String shipmentPackageSeqId = null;
                for (int i = 0; i < contentList.size(); i++) {
                    Map<String, Object> contentMap = UtilGenerics.checkMap(contentList.get(i));
                    Map<String, Object> content = UtilGenerics.checkMap(contentMap.get("content"));
                    OrderReadHelper orderReadHelper = (OrderReadHelper)content.get("orderReadHelper");
                    List<BOMNode> productsInPackages = UtilGenerics.checkList(content.get("productsInPackages"));
                    GenericValue orderShipment = (GenericValue)content.get("orderShipment");

                    GenericValue product = null;
                    BigDecimal quantity = BigDecimal.ZERO;
                    boolean subProduct = contentMap.containsKey("componentIndex");
                    if (subProduct) {
                        // multi package
                        Integer index = (Integer)contentMap.get("componentIndex");
                        BOMNode component = productsInPackages.get(index);
                        product = component.getProduct();
                        quantity = component.getQuantity();
                    } else {
                        // single package
                        GenericValue orderItem = orderReadHelper.getOrderItem(orderShipment.getString("orderItemSeqId"));
                        try {
                            product = orderItem.getRelatedOne("Product", false);
                        } catch (GenericEntityException e) {
                            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingPackageConfiguratorError", locale));
                        }
                        quantity = orderShipment.getBigDecimal("quantity");
                    }

                    BigDecimal productDepth = product.getBigDecimal("shippingDepth");
                    if (productDepth == null) {
                        productDepth = product.getBigDecimal("productDepth");
                    }
                    if (productDepth == null) {
                        productDepth = BigDecimal.ONE;
                    }

                    BigDecimal firstMaxNumOfProducts = boxWidth.subtract(totalWidth).divide(productDepth, 0, RoundingMode.FLOOR);
                    if (firstMaxNumOfProducts.compareTo(BigDecimal.ZERO) == 0) firstMaxNumOfProducts = BigDecimal.ONE;
                    //
                    BigDecimal maxNumOfProducts = boxWidth.divide(productDepth, 0, RoundingMode.FLOOR);
                    if (maxNumOfProducts.compareTo(BigDecimal.ZERO) == 0) maxNumOfProducts = BigDecimal.ONE;

                    BigDecimal remQuantity = quantity;
                    boolean isFirst = true;
                    while (remQuantity.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal maxQuantity = BigDecimal.ZERO;
                        if (isFirst) {
                            maxQuantity = firstMaxNumOfProducts;
                            isFirst = false;
                        } else {
                            maxQuantity = maxNumOfProducts;
                        }
                        BigDecimal qty = (remQuantity.compareTo(maxQuantity) < 0 ? remQuantity : maxQuantity);
                        // If needed, create the package
                        if (shipmentPackageSeqId == null) {
                            try {
                                Map<String, Object> serviceResult = dispatcher.runSync("createShipmentPackage", UtilMisc.<String, Object>toMap("shipmentId", orderShipment.getString("shipmentId"), "shipmentBoxTypeId", boxTypeId, "userLogin", userLogin));
                                if (ServiceUtil.isError(serviceResult)) {
                                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(serviceResult));
                                }
                                shipmentPackageSeqId = (String)serviceResult.get("shipmentPackageSeqId");
                            } catch (GenericServiceException e) {
                                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingPackageConfiguratorError", locale));
                            }
                            totalWidth = BigDecimal.ZERO;
                        }
                        try {
                            Map<String, Object> inputMap = null;
                            if (subProduct) {
                                inputMap = UtilMisc.toMap("shipmentId", orderShipment.getString("shipmentId"),
                                "shipmentPackageSeqId", shipmentPackageSeqId,
                                "shipmentItemSeqId", orderShipment.getString("shipmentItemSeqId"),
                                "subProductId", product.getString("productId"),
                                "userLogin", userLogin,
                                "subProductQuantity", qty);
                            } else {
                                inputMap = UtilMisc.toMap("shipmentId", orderShipment.getString("shipmentId"),
                                "shipmentPackageSeqId", shipmentPackageSeqId,
                                "shipmentItemSeqId", orderShipment.getString("shipmentItemSeqId"),
                                "userLogin", userLogin,
                                "quantity", qty);
                            }
                            Map<String, Object> serviceResult = dispatcher.runSync("createShipmentPackageContent", inputMap);
                            if (ServiceUtil.isError(serviceResult)) {
                                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingPackageConfiguratorError", locale));
                            }
                        } catch (GenericServiceException e) {
                            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingPackageConfiguratorError", locale));
                        }
                        totalWidth = totalWidth.add(qty.multiply(productDepth));
                        if (qty.compareTo(maxQuantity) == 0) shipmentPackageSeqId = null;
                        remQuantity = remQuantity.subtract(qty);
                    }
                }
            }
        }
        return result;
    }

    /** It reads the product's bill of materials,
     * if necessary configures it, and it returns its (possibly configured) components in
     * a List of {@link BOMNode}).
     * @param dctx the distach context
     * @param context the context 
     * @return returns the list of products in packages
     */
    public static Map<String, Object> getProductsInPackages(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue)context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        String productId = (String) context.get("productId");
        BigDecimal quantity = (BigDecimal) context.get("quantity");
        String fromDateStr = (String) context.get("fromDate");

        if (quantity == null) {
            quantity = BigDecimal.ONE;
        }
        Date fromDate = null;
        if (UtilValidate.isNotEmpty(fromDateStr)) {
            try {
                fromDate = Timestamp.valueOf(fromDateStr);
            } catch (Exception e) {
            }
        }
        if (fromDate == null) {
            fromDate = new Date();
        }

        //
        // Components
        //
        BOMTree tree = null;
        List<BOMNode> components = new LinkedList<>();
        try {
            tree = new BOMTree(productId, "MANUF_COMPONENT", fromDate, BOMTree.EXPLOSION_MANUFACTURING, delegator, dispatcher, userLogin);
            tree.setRootQuantity(quantity);
            tree.getProductsInPackages(components);
        } catch (GenericEntityException gee) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingBomErrorCreatingBillOfMaterialsTree", UtilMisc.toMap("errorString", gee.getMessage()), locale));
        }

        result.put("productsInPackages", components);

        return result;
    }

}
