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
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.product.store.ProductStoreWorker;
import org.apache.ofbiz.service.LocalDispatcher;

/** It represents an (in-memory) bill of materials (in which each
  * component is an BOMNode)
  * Useful for tree traversal (breakdown, explosion, implosion).
  */
public class BOMTree {
    public static final int EXPLOSION = 0;
    public static final int EXPLOSION_SINGLE_LEVEL = 1;
    public static final int EXPLOSION_MANUFACTURING = 2;
    public static final int IMPLOSION = 3;

    protected LocalDispatcher dispatcher = null;
    protected Delegator delegator = null;

    BOMNode root;
    BigDecimal rootQuantity;
    BigDecimal rootAmount;
    Date inDate;
    String bomTypeId;
    GenericValue inputProduct;

    /** Creates a new instance of BOMTree by reading downward
     * the productId's bill of materials (explosion).
     * If virtual products are found, it tries to configure them by running
     * the Product Configurator.
     * @param productId The product for which we want to get the bom.
     * @param bomTypeId The bill of materials type (e.g. manufacturing, engineering, ...)
     * @param inDate Validity date (if null, today is used).
     *
     * @param delegator The delegator used.
     * @throws GenericEntityException If a db problem occurs.
     *
     */
    public BOMTree(String productId, String bomTypeId, Date inDate, Delegator delegator, LocalDispatcher dispatcher, GenericValue userLogin) throws GenericEntityException {
        this(productId, bomTypeId, inDate, EXPLOSION, delegator, dispatcher, userLogin);
    }

    /** Creates a new instance of BOMTree by reading
     * the productId's bill of materials (upward or downward).
     * If virtual products are found, it tries to configure them by running
     * the Product Configurator.
     * @param productId The product for which we want to get the bom.
     * @param bomTypeId The bill of materials type (e.g. manufacturing, engineering, ...)
     * @param inDate Validity date (if null, today is used).
     *
     * @param type if equals to EXPLOSION, a downward visit is performed (explosion);
     * if equals to EXPLOSION_SINGLE_LEVEL, a single level explosion is performed;
     * if equals to EXPLOSION_MANUFACTURING, a downward visit is performed (explosion), including only the product that needs manufacturing;
     * if equals to IMPLOSION an upward visit is done (implosion);
     *
     * @param delegator The delegator used.
     * @throws GenericEntityException If a db problem occurs.
     *
     */
    public BOMTree(String productId, String bomTypeId, Date inDate, int type, Delegator delegator, LocalDispatcher dispatcher, GenericValue userLogin) throws GenericEntityException {
        // If the parameters are not valid, return.
        if (productId == null || bomTypeId == null || delegator == null || dispatcher == null) return;
        // If the date is null, set it to today.
        if (inDate == null) inDate = new Date();

        this.delegator = delegator;
        this.dispatcher = dispatcher;

        inputProduct = EntityQuery.use(delegator).from("Product").where("productId", productId).queryOne();

        String productIdForRules = productId;
        // The selected product features are loaded
        List<GenericValue> productFeaturesAppl = EntityQuery.use(delegator).from("ProductFeatureAppl")
                .where("productId", productId, 
                        "productFeatureApplTypeId", "STANDARD_FEATURE")
                .queryList();
        List<GenericValue> productFeatures = new LinkedList<GenericValue>();
        GenericValue oneProductFeatureAppl = null;
        for (int i = 0; i < productFeaturesAppl.size(); i++) {
            oneProductFeatureAppl = productFeaturesAppl.get(i);
            productFeatures.add(oneProductFeatureAppl.getRelatedOne("ProductFeature", false));
        }
        // If the product is manufactured as a different product,
        // load the new product
        GenericValue manufacturedAsProduct = manufacturedAsProduct(productId, inDate);
        // We load the information about the product that needs to be manufactured
        // from Product entity
        GenericValue product = EntityQuery.use(delegator).from("Product")
                .where("productId", (manufacturedAsProduct != null? manufacturedAsProduct.getString("productIdTo"): productId))
                .queryOne();
        if (product == null) return;
        BOMNode originalNode = new BOMNode(product, dispatcher, userLogin);
        originalNode.setTree(this);
        // If the product hasn't a bill of materials we try to retrieve
        // the bill of materials of its virtual product (if the current
        // product is variant).
        if (!hasBom(product, inDate)) {
            List<GenericValue> virtualProducts = product.getRelated("AssocProductAssoc", UtilMisc.toMap("productAssocTypeId", "PRODUCT_VARIANT"), null, false);
            virtualProducts = EntityUtil.filterByDate(virtualProducts, inDate);
            GenericValue virtualProduct = EntityUtil.getFirst(virtualProducts);
            if (virtualProduct != null) {
                // If the virtual product is manufactured as a different product,
                // load the new product
                productIdForRules = virtualProduct.getString("productId");
                manufacturedAsProduct = manufacturedAsProduct(virtualProduct.getString("productId"), inDate);
                product = EntityQuery.use(delegator).from("Product")
                        .where("productId", (manufacturedAsProduct != null? manufacturedAsProduct.getString("productIdTo"): virtualProduct.get("productId")))
                        .queryOne();
            }
        }
        if (product == null) return;
        try {
            root = new BOMNode(product, dispatcher, userLogin);
            root.setTree(this);
            root.setProductForRules(productIdForRules);
            root.setSubstitutedNode(originalNode);
            if (type == IMPLOSION) {
                root.loadParents(bomTypeId, inDate, productFeatures);
            } else {
                root.loadChildren(bomTypeId, inDate, productFeatures, type);
            }
        } catch (GenericEntityException gee) {
            root = null;
        }
        this.bomTypeId = bomTypeId;
        this.inDate = inDate;
        rootQuantity = BigDecimal.ONE;
        rootAmount = BigDecimal.ZERO;
    }

    public GenericValue getInputProduct() {
        return inputProduct;
    }

    private GenericValue manufacturedAsProduct(String productId, Date inDate) throws GenericEntityException {
        return EntityQuery.use(delegator).from("ProductAssoc")
                .where("productId", productId,
                        "productAssocTypeId", "PRODUCT_MANUFACTURED")
                .filterByDate(inDate).queryFirst();
    }

    private boolean hasBom(GenericValue product, Date inDate) throws GenericEntityException {
        List<GenericValue> children = product.getRelated("MainProductAssoc", UtilMisc.toMap("productAssocTypeId", bomTypeId), null, false);
        children = EntityUtil.filterByDate(children, inDate);
        return UtilValidate.isNotEmpty(children);
    }

    /** It tells if the current (in-memory) tree representing
     * a product's bill of materials is completely configured
     * or not.
     * @return true if no virtual nodes (products) are present in the tree.
     *
     */
    public boolean isConfigured() {
        List<BOMNode> notConfiguredParts = new LinkedList<BOMNode>();
        root.isConfigured(notConfiguredParts);
        return (notConfiguredParts.size() == 0);
    }

    /** Getter for property rootQuantity.
     * @return Value of property rootQuantity.
     *
     */
    public BigDecimal getRootQuantity() {
        return rootQuantity;
    }

    /** Setter for property rootQuantity.
     * @param rootQuantity New value of property rootQuantity.
     *
     */
    public void setRootQuantity(BigDecimal rootQuantity) {
        this.rootQuantity = rootQuantity;
    }

    /** Getter for property rootAmount.
     * @return Value of property rootAmount.
     *
     */
    public BigDecimal getRootAmount() {
        return rootAmount;
    }

    /** Setter for property rootAmount.
     * @param rootAmount New value of property rootAmount.
     *
     */
    public void setRootAmount(BigDecimal rootAmount) {
        this.rootAmount = rootAmount;
    }

    /** Getter for property root.
     * @return Value of property root.
     *
     */
    public BOMNode getRoot() {
        return root;
    }

    /** Getter for property inDate.
     * @return Value of property inDate.
     *
     */
    public Date getInDate() {
        return inDate;
    }

    /** Getter for property bomTypeId.
     * @return Value of property bomTypeId.
     *
     */
    public String getBomTypeId() {
        return bomTypeId;
    }

    /** It visits the in-memory tree that represents a bill of materials
     * and it collects info of its nodes in the StringBuffer.
     * Method used for debug purposes.
     * @param sb The StringBuffer used to collect tree info.
     */
    public void print(StringBuffer sb) {
        if (root != null) {
            root.print(sb, getRootQuantity(), 0);
        }
    }

    /** It visits the in-memory tree that represents a bill of materials
     * and it collects info of its nodes in the List.
     * Method used for bom breakdown (explosion/implosion).
     * @param arr The List used to collect tree info.
     * @param initialDepth The depth of the root node.
     */
    public void print(List<BOMNode> arr, int initialDepth) {
        print(arr, initialDepth, true);
    }

    public void print(List<BOMNode> arr, int initialDepth, boolean excludeWIPs) {
        if (root != null) {
            root.print(arr, getRootQuantity(), initialDepth, excludeWIPs);
        }
    }

    /** It visits the in-memory tree that represents a bill of materials
     * and it collects info of its nodes in the List.
     * Method used for bom breakdown (explosion/implosion).
     * @param arr The List used to collect tree info.
     */
    public void print(List<BOMNode> arr) {
        print(arr, 0, false);
    }

    public void print(List<BOMNode> arr, boolean excludeWIPs) {
        print(arr, 0, excludeWIPs);
    }

    /** It visits the in-memory tree that represents a bill of materials
     * and it collects info of its nodes in the Map.
     * Method used for bom summarized explosion.
     * @param quantityPerNode The Map that will contain the summarized quantities per productId.
     */
    public void sumQuantities(Map<String, BOMNode> quantityPerNode) {
        if (root != null) {
            root.sumQuantity(quantityPerNode);
        }
    }

    /** It visits the in-memory tree that represents a bill of materials
     * and it collects all the productId it contains.
     * @return List containing all the tree's productId.
     */
    public List<String> getAllProductsId() {
        List<BOMNode> nodeArr = new LinkedList<BOMNode>();
        List<String> productsId = new LinkedList<String>();
        print(nodeArr);
        for (int i = 0; i < nodeArr.size(); i++) {
            productsId.add((nodeArr.get(i)).getProduct().getString("productId"));
        }
        return productsId;
    }

    /** It visits the in-memory tree that represents a bill of materials
     * and it creates a manufacturing order for each of the nodes that needs
     * to be manufactured.
     * @param facilityId the facility id
     * @param date the context date
     * @param workEffortName the work effort name
     * @param description the description
     * @param routingId the routing id
     * @param orderId the order id
     * @param orderItemSeqId the order item id
     * @param shipGroupSeqId the shipment group item id
     * @param shipmentId the shipment id delegator used
     * @param userLogin the GenericValue object of userLogin
     * @return returns the work effort id
     * @throws GenericEntityException If a db problem occurs.
     */
    public String createManufacturingOrders(String facilityId, Date date, String workEffortName, String description, String routingId, String orderId, String orderItemSeqId, String shipGroupSeqId, String shipmentId, GenericValue userLogin)  throws GenericEntityException {
        String workEffortId = null;
        if (root != null) {
            if (UtilValidate.isEmpty(facilityId)) {
                if (orderId != null) {
                    GenericValue order = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
                    String productStoreId = order.getString("productStoreId");
                    if (productStoreId != null) {
                        GenericValue productStore = ProductStoreWorker.getProductStore(productStoreId, delegator);
                        if (productStore != null) {
                            facilityId = productStore.getString("inventoryFacilityId");
                        }
                    }

                }
                if (facilityId == null && shipmentId != null) {
                    GenericValue shipment = EntityQuery.use(delegator).from("Shipment").where("shipmentId", shipmentId).queryOne();
                    facilityId = shipment.getString("originFacilityId");
                }
            }
            Map<String, Object> tmpMap = root.createManufacturingOrder(facilityId, date, workEffortName, description, routingId, orderId, orderItemSeqId, shipGroupSeqId, shipmentId, true, true);
            workEffortId = (String)tmpMap.get("productionRunId");
        }
        return workEffortId;
    }

    public void getProductsInPackages(List<BOMNode> arr) {
        if (root != null) {
            root.getProductsInPackages(arr, getRootQuantity(), 0, false);
        }
    }

}
