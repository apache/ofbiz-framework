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

package org.ofbiz.manufacturing.bom;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;

import org.ofbiz.manufacturing.mrp.ProposedOrder;

/** An ItemCoinfigurationNode represents a component in a bill of materials.
 */

public class BOMNode {
    public static final String module = BOMNode.class.getName();

    protected LocalDispatcher dispatcher = null;
    protected Delegator delegator = null;
    protected GenericValue userLogin = null;

    private BOMTree tree; // the tree to which this node belongs
    private BOMNode parentNode; // the parent node (null if it's not present)
    private BOMNode substitutedNode; // The virtual node (if any) that this instance substitutes
    private GenericValue ruleApplied; // The rule (if any) that that has been applied to configure the current node
    private String productForRules;
    private GenericValue product; // the current product (from Product entity)
    private GenericValue productAssoc; // the product assoc record (from ProductAssoc entity) in which the current product is in productIdTo
    private ArrayList children; // current node's children (ProductAssocs)
    private ArrayList childrenNodes; // current node's children nodes (BOMNode)
    private BigDecimal quantityMultiplier; // the necessary quantity as declared in the bom (from ProductAssocs or ProductManufacturingRule)
    private BigDecimal scrapFactor; // the scrap factor as declared in the bom (from ProductAssocs)
    // Runtime fields
    private int depth; // the depth of this node in the current tree
    private BigDecimal quantity; // the quantity of this node in the current tree
    private String bomTypeId; // the type of the current tree

    public BOMNode(GenericValue product, LocalDispatcher dispatcher, GenericValue userLogin) {
        this.product = product;
        this.delegator = product.getDelegator();
        this.dispatcher = dispatcher;
        this.userLogin = userLogin;
        children = new ArrayList();
        childrenNodes = new ArrayList();
        parentNode = null;
        productForRules = null;
        bomTypeId = null;
        quantityMultiplier = BigDecimal.ONE;
        scrapFactor = BigDecimal.ONE;
        // Now we initialize the fields used in breakdowns
        depth = 0;
        quantity = BigDecimal.ZERO;
    }

    public BOMNode(String productId, Delegator delegator, LocalDispatcher dispatcher, GenericValue userLogin) throws GenericEntityException {
        this(delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", productId)), dispatcher, userLogin);
    }

    protected void loadChildren(String partBomTypeId, Date inDate, List productFeatures, int type) throws GenericEntityException {
        if (product == null) {
            throw new GenericEntityException("product is null");
        }
        // If the date is null, set it to today.
        if (inDate == null) inDate = new Date();
        bomTypeId = partBomTypeId;
//        Delegator delegator = product.getDelegator();
        List rows = delegator.findByAnd("ProductAssoc",
                                            UtilMisc.toMap("productId", product.get("productId"),
                                                       "productAssocTypeId", partBomTypeId),
                                            UtilMisc.toList("sequenceNum","productIdTo ASC"));
        rows = EntityUtil.filterByDate(rows, inDate);
        if ((UtilValidate.isEmpty(rows)) && substitutedNode != null) {
            // If no child is found and this is a substituted node
            // we try to search for substituted node's children.
            rows = delegator.findByAnd("ProductAssoc",
                                        UtilMisc.toMap("productId", substitutedNode.getProduct().get("productId"),
                                                       "productAssocTypeId", partBomTypeId),
                                        UtilMisc.toList("sequenceNum"));
            rows = EntityUtil.filterByDate(rows, inDate);
        }
        children = new ArrayList(rows);
        childrenNodes = new ArrayList();
        Iterator childrenIterator = children.iterator();
        GenericValue oneChild = null;
        BOMNode oneChildNode = null;
        while (childrenIterator.hasNext()) {
            oneChild = (GenericValue)childrenIterator.next();
            // Configurator
            oneChildNode = configurator(oneChild, productFeatures, getRootNode().getProductForRules(), inDate);
            // If the node is null this means that the node has been discarded by the rules.
            if (oneChildNode != null) {
                oneChildNode.setParentNode(this);
                switch (type) {
                    case BOMTree.EXPLOSION:
                        oneChildNode.loadChildren(partBomTypeId, inDate, productFeatures, BOMTree.EXPLOSION);
                    break;
                    case BOMTree.EXPLOSION_MANUFACTURING:
                        // for manufacturing trees, do not look through and create production runs for children unless there is no warehouse stocking of this node item
                        if (!oneChildNode.isWarehouseManaged(null)) { // FIXME: we will need to pass a facilityId here
                            oneChildNode.loadChildren(partBomTypeId, inDate, productFeatures, type);
                        }
                    break;
                }
            }
            childrenNodes.add(oneChildNode);
        }
    }

    private BOMNode substituteNode(BOMNode oneChildNode, List productFeatures, List productPartRules) throws GenericEntityException {
        if (productPartRules != null) {
            GenericValue rule = null;
            for (int i = 0; i < productPartRules.size(); i++) {
                rule = (GenericValue)productPartRules.get(i);
                String ruleCondition = (String)rule.get("productFeature");
                String ruleOperator = (String)rule.get("ruleOperator");
                String newPart = (String)rule.get("productIdInSubst");
                BigDecimal ruleQuantity = BigDecimal.ZERO;
                try {
                    ruleQuantity = rule.getBigDecimal("quantity");
                } catch (Exception exc) {
                    ruleQuantity = BigDecimal.ZERO;
                }

                GenericValue feature = null;
                boolean ruleSatisfied = false;
                if (ruleCondition == null || ruleCondition.equals("")) {
                    ruleSatisfied = true;
                } else {
                    if (productFeatures != null) {
                        for (int j = 0; j < productFeatures.size(); j++) {
                            feature = (GenericValue)productFeatures.get(j);
                            if (ruleCondition.equals((String)feature.get("productFeatureId"))) {
                                ruleSatisfied = true;
                                break;
                            }
                        }
                    }
                }
                if (ruleSatisfied && ruleOperator.equals("OR")) {
                    BOMNode tmpNode = oneChildNode;
                    if (newPart == null || newPart.equals("")) {
                        oneChildNode = null;
                    } else {
                        BOMNode origNode = oneChildNode;
                        oneChildNode = new BOMNode(newPart, delegator, dispatcher, userLogin);
                        oneChildNode.setTree(tree);
                        oneChildNode.setSubstitutedNode(tmpNode);
                        oneChildNode.setRuleApplied(rule);
                        oneChildNode.setProductAssoc(origNode.getProductAssoc());
                        oneChildNode.setScrapFactor(origNode.getScrapFactor());
                        if (ruleQuantity.compareTo(BigDecimal.ZERO) > 0) {
                            oneChildNode.setQuantityMultiplier(ruleQuantity);
                        } else {
                            oneChildNode.setQuantityMultiplier(origNode.getQuantityMultiplier());
                        }
                    }
                    break;
                }
                // FIXME: AND operator still not implemented
            } // end of for

        }
        return oneChildNode;
    }

    private BOMNode configurator(GenericValue node, List productFeatures, String productIdForRules, Date inDate) throws GenericEntityException {
        BOMNode oneChildNode = new BOMNode((String)node.get("productIdTo"), delegator, dispatcher, userLogin);
        oneChildNode.setTree(tree);
        oneChildNode.setProductAssoc(node);
        try {
            oneChildNode.setQuantityMultiplier(node.getBigDecimal("quantity"));
        } catch (Exception nfe) {
            oneChildNode.setQuantityMultiplier(BigDecimal.ONE);
        }
        try {
            BigDecimal percScrapFactor = node.getBigDecimal("scrapFactor");

            // A negative scrap factor is a salvage factor
            BigDecimal bdHundred = new BigDecimal("100");
            if (percScrapFactor.compareTo(bdHundred.negate()) > 0 && percScrapFactor.compareTo(bdHundred) < 0) {
                percScrapFactor = BigDecimal.ONE.add(percScrapFactor.movePointLeft(2));
            } else {
                Debug.logWarning("A scrap factor of [" + percScrapFactor + "] was ignored", module);
                percScrapFactor = BigDecimal.ONE;
            }
            oneChildNode.setScrapFactor(percScrapFactor);
        } catch (Exception nfe) {
            oneChildNode.setScrapFactor(BigDecimal.ONE);
        }
        BOMNode newNode = oneChildNode;
        // CONFIGURATOR
        if (oneChildNode.isVirtual()) {
            // If the part is VIRTUAL and
            // productFeatures and productPartRules are not null
            // we have to substitute the part with the right part's variant
            List productPartRules = delegator.findByAnd("ProductManufacturingRule",
                                                    UtilMisc.toMap("productId", productIdForRules,
                                                    "productIdFor", node.get("productId"),
                                                    "productIdIn", node.get("productIdTo")));
            if (substitutedNode != null) {
                productPartRules.addAll(delegator.findByAnd("ProductManufacturingRule",
                                                    UtilMisc.toMap("productId", productIdForRules,
                                                    "productIdFor", substitutedNode.getProduct().getString("productId"),
                                                    "productIdIn", node.get("productIdTo"))));
            }
            productPartRules = EntityUtil.filterByDate(productPartRules, inDate);
            newNode = substituteNode(oneChildNode, productFeatures, productPartRules);
            if (newNode.equals(oneChildNode)) {
                // If no substitution has been done (no valid rule applied),
                // we try to search for a generic link-rule
                List genericLinkRules = delegator.findByAnd("ProductManufacturingRule",
                                                        UtilMisc.toMap("productIdFor", node.get("productId"),
                                                        "productIdIn", node.get("productIdTo")));
                if (substitutedNode != null) {
                    genericLinkRules.addAll(delegator.findByAnd("ProductManufacturingRule",
                                                        UtilMisc.toMap("productIdFor", substitutedNode.getProduct().getString("productId"),
                                                        "productIdIn", node.get("productIdTo"))));
                }
                genericLinkRules = EntityUtil.filterByDate(genericLinkRules, inDate);
                newNode = substituteNode(oneChildNode, productFeatures, genericLinkRules);
                if (newNode.equals(oneChildNode)) {
                    // If no substitution has been done (no valid rule applied),
                    // we try to search for a generic node-rule
                    List genericNodeRules = delegator.findByAnd("ProductManufacturingRule",
                                                            UtilMisc.toMap("productIdIn", node.get("productIdTo")),
                                                            UtilMisc.toList("ruleSeqId"));
                    genericNodeRules = EntityUtil.filterByDate(genericNodeRules, inDate);
                    newNode = null;
                    newNode = substituteNode(oneChildNode, productFeatures, genericNodeRules);
                    if (newNode.equals(oneChildNode)) {
                        // If no substitution has been done (no valid rule applied),
                        // we try to set the default (first) node-substitution
                        if (UtilValidate.isNotEmpty(genericNodeRules)) {
                            // FIXME
                            //...
                        }
                        // -----------------------------------------------------------
                        // We try to apply directly the selected features
                        if (newNode.equals(oneChildNode)) {
                            Map selectedFeatures = new HashMap();
                            if (productFeatures != null) {
                                GenericValue feature = null;
                                for (int j = 0; j < productFeatures.size(); j++) {
                                    feature = (GenericValue)productFeatures.get(j);
                                    selectedFeatures.put((String)feature.get("productFeatureTypeId"), (String)feature.get("productFeatureId")); // FIXME
                                }
                            }

                            if (selectedFeatures.size() > 0) {
                                Map context = new HashMap();
                                context.put("productId", node.get("productIdTo"));
                                context.put("selectedFeatures", selectedFeatures);
                                Map storeResult = null;
                                GenericValue variantProduct = null;
                                try {
                                    storeResult = dispatcher.runSync("getProductVariant", context);
                                    List variantProducts = (List) storeResult.get("products");
                                    if (variantProducts.size() == 1) {
                                        variantProduct = (GenericValue)variantProducts.get(0);
                                    }
                                } catch (GenericServiceException e) {
                                    String service = e.getMessage();
                                    if (Debug.infoOn()) Debug.logInfo("Error calling getProductVariant service", module);
                                }
                                if (variantProduct != null) {
                                    newNode = new BOMNode(variantProduct, dispatcher, userLogin);
                                    newNode.setTree(tree);
                                    newNode.setSubstitutedNode(oneChildNode);
                                    newNode.setQuantityMultiplier(oneChildNode.getQuantityMultiplier());
                                    newNode.setScrapFactor(oneChildNode.getScrapFactor());
                                    newNode.setProductAssoc(oneChildNode.getProductAssoc());
                                }
                            }

                        }
                        // -----------------------------------------------------------
                    }
                }
            }
        } // end of if (isVirtual())
        return newNode;
    }

    protected void loadParents(String partBomTypeId, Date inDate, List productFeatures) throws GenericEntityException {
        if (product == null) {
            throw new GenericEntityException("product is null");
        }
        // If the date is null, set it to today.
        if (inDate == null) inDate = new Date();

        bomTypeId = partBomTypeId;
//        Delegator delegator = product.getDelegator();
        List rows = delegator.findByAnd("ProductAssoc",
                                            UtilMisc.toMap("productIdTo", product.get("productId"),
                                                       "productAssocTypeId", partBomTypeId),
                                            UtilMisc.toList("sequenceNum"));
        rows = EntityUtil.filterByDate(rows, inDate);
        if ((UtilValidate.isEmpty(rows)) && substitutedNode != null) {
            // If no parent is found and this is a substituted node
            // we try to search for substituted node's parents.
            rows = delegator.findByAnd("ProductAssoc",
                                        UtilMisc.toMap("productIdTo", substitutedNode.getProduct().get("productId"),
                                                       "productAssocTypeId", partBomTypeId),
                                        UtilMisc.toList("sequenceNum"));
            rows = EntityUtil.filterByDate(rows, inDate);
        }
        children = new ArrayList(rows);
        childrenNodes = new ArrayList();
        Iterator childrenIterator = children.iterator();
        GenericValue oneChild = null;
        BOMNode oneChildNode = null;
        while (childrenIterator.hasNext()) {
            oneChild = (GenericValue)childrenIterator.next();
            oneChildNode = new BOMNode(oneChild.getString("productId"), delegator, dispatcher, userLogin);
            // Configurator
            //oneChildNode = configurator(oneChild, productFeatures, getRootNode().getProductForRules(), delegator);
            // If the node is null this means that the node has been discarded by the rules.
            if (oneChildNode != null) {
                oneChildNode.setParentNode(this);
                oneChildNode.setTree(tree);
                oneChildNode.loadParents(partBomTypeId, inDate, productFeatures);
            }
            childrenNodes.add(oneChildNode);
        }
    }


    /** Getter for property parentNode.
     * @return Value of property parentNode.
     *
     */
    public BOMNode getParentNode() {
        return parentNode;
    }

    public BOMNode getRootNode() {
        return (parentNode != null? getParentNode(): this);
    }
    /** Setter for property parentNode.
     * @param parentNode New value of property parentNode.
     *
     */
    public void setParentNode(BOMNode parentNode) {
        this.parentNode = parentNode;
    }
    // ------------------------------------
    // Method used for TEST and DEBUG purposes
    public void print(StringBuffer sb, BigDecimal quantity, int depth) {
        for (int i = 0; i < depth; i++) {
            sb.append("<b>&nbsp;*&nbsp;</b>");
        }
        sb.append(product.get("productId"));
        sb.append(" - ");
        sb.append(quantity);
        GenericValue oneChild = null;
        BOMNode oneChildNode = null;
        depth++;
        for (int i = 0; i < children.size(); i++) {
            oneChild = (GenericValue)children.get(i);
            BigDecimal bomQuantity = BigDecimal.ZERO;
            try {
                bomQuantity = oneChild.getBigDecimal("quantity");
            } catch (Exception exc) {
                bomQuantity = BigDecimal.ONE;
            }
            oneChildNode = (BOMNode)childrenNodes.get(i);
            sb.append("<br/>");
            if (oneChildNode != null) {
                oneChildNode.print(sb, quantity.multiply(bomQuantity), depth);
            }
        }
    }

    public void print(ArrayList arr, BigDecimal quantity, int depth, boolean excludeWIPs) {
        // Now we set the depth and quantity of the current node
        // in this breakdown.
        this.depth = depth;
        String serviceName = null;
        if (this.productAssoc != null && this.productAssoc.getString("estimateCalcMethod") != null) {
            try {
                GenericValue genericService = productAssoc.getRelatedOne("CustomMethod");
                if (genericService != null && genericService.getString("customMethodName") != null) {
                    serviceName = genericService.getString("customMethodName");
                }
            } catch (Exception exc) {
            }
        }
        if (serviceName != null) {
            Map resultContext = null;
            Map arguments = UtilMisc.toMap("neededQuantity", quantity.multiply(quantityMultiplier), "amount", tree != null ? tree.getRootAmount() : BigDecimal.ZERO);
            BigDecimal width = null;
            if (getProduct().get("productWidth") != null) {
                width = getProduct().getBigDecimal("productWidth");
            }
            if (width == null) {
                width = BigDecimal.ZERO;
            }
            arguments.put("width", width);
            Map inputContext = UtilMisc.toMap("arguments", arguments, "userLogin", userLogin);
            try {
                resultContext = dispatcher.runSync(serviceName, inputContext);
                BigDecimal calcQuantity = (BigDecimal)resultContext.get("quantity");
                if (calcQuantity != null) {
                    this.quantity = calcQuantity;
                }
            } catch (GenericServiceException e) {
                //Debug.logError(e, "Problem calling the getManufacturingComponents service", module);
            }
        } else {
            this.quantity = quantity.multiply(quantityMultiplier).multiply(scrapFactor);
        }
        // First of all we visit the current node.
        arr.add(this);
        // Now (recursively) we visit the children.
        GenericValue oneChild = null;
        BOMNode oneChildNode = null;
        depth++;
        for (int i = 0; i < children.size(); i++) {
            oneChild = (GenericValue)children.get(i);
            oneChildNode = (BOMNode)childrenNodes.get(i);
            if (excludeWIPs && "WIP".equals(oneChildNode.getProduct().getString("productTypeId"))) {
                continue;
            }
            if (oneChildNode != null) {
                oneChildNode.print(arr, this.quantity, depth, excludeWIPs);
            }
        }
    }

    public void getProductsInPackages(ArrayList arr, BigDecimal quantity, int depth, boolean excludeWIPs) {
        // Now we set the depth and quantity of the current node
        // in this breakdown.
        this.depth = depth;
        this.quantity = quantity.multiply(quantityMultiplier).multiply(scrapFactor);
        // First of all we visit the current node.
        if (this.getProduct().getString("defaultShipmentBoxTypeId") != null) {
            arr.add(this);
        } else {
            GenericValue oneChild = null;
            BOMNode oneChildNode = null;
            depth++;
            for (int i = 0; i < children.size(); i++) {
                oneChild = (GenericValue)children.get(i);
                oneChildNode = (BOMNode)childrenNodes.get(i);
                if (excludeWIPs && "WIP".equals(oneChildNode.getProduct().getString("productTypeId"))) {
                    continue;
                }
                if (oneChildNode != null) {
                    oneChildNode.getProductsInPackages(arr, this.quantity, depth, excludeWIPs);
                }
            }
        }
    }

    public void sumQuantity(HashMap nodes) {
        // First of all, we try to fetch a node with the same partId
        BOMNode sameNode = (BOMNode)nodes.get(product.getString("productId"));
        // If the node is not found we create a new node for the current product
        if (sameNode == null) {
            sameNode = new BOMNode(product, dispatcher, userLogin);
            nodes.put(product.getString("productId"), sameNode);
        }
        // Now we add the current quantity to the node
        sameNode.setQuantity(sameNode.getQuantity().add(quantity));
        // Now (recursively) we visit the children.
        BOMNode oneChildNode = null;
        for (int i = 0; i < childrenNodes.size(); i++) {
            oneChildNode = (BOMNode)childrenNodes.get(i);
            if (oneChildNode != null) {
                oneChildNode.sumQuantity(nodes);
            }
        }
    }

    public Map createManufacturingOrder(String facilityId, Date date, String workEffortName, String description, String routingId, String orderId, String orderItemSeqId, String shipmentId, boolean useSubstitute, boolean ignoreSupplierProducts) throws GenericEntityException {
        String productionRunId = null;
        Timestamp endDate = null;
        if (isManufactured(ignoreSupplierProducts)) {
            BOMNode oneChildNode = null;
            ArrayList childProductionRuns = new ArrayList();
            Timestamp maxEndDate = null;
            for (int i = 0; i < childrenNodes.size(); i++) {
                oneChildNode = (BOMNode)childrenNodes.get(i);
                if (oneChildNode != null) {
                    Map tmpResult = oneChildNode.createManufacturingOrder(facilityId, date, null, null, null, null, null, shipmentId, false, false);
                    String childProductionRunId = (String)tmpResult.get("productionRunId");
                    Timestamp childEndDate = (Timestamp)tmpResult.get("endDate");
                    if (maxEndDate == null) {
                        maxEndDate = childEndDate;
                    }
                    if (childEndDate != null && maxEndDate.compareTo(childEndDate) < 0) {
                        maxEndDate = childEndDate;
                    }

                    if (childProductionRunId != null) {
                        childProductionRuns.add(childProductionRunId);
                    }
                }
            }

            Timestamp startDate = UtilDateTime.toTimestamp(UtilDateTime.toDateTimeString(date));
            Map serviceContext = new HashMap();
            if (!useSubstitute) {
                serviceContext.put("productId", getProduct().getString("productId"));
                serviceContext.put("facilityId", getProduct().getString("facilityId"));
            } else {
                serviceContext.put("productId", getSubstitutedNode().getProduct().getString("productId"));
                serviceContext.put("facilityId", getSubstitutedNode().getProduct().getString("facilityId"));
            }
            if (!UtilValidate.isEmpty(facilityId)) {
                serviceContext.put("facilityId", facilityId);
            }
            if (!UtilValidate.isEmpty(workEffortName)) {
                serviceContext.put("workEffortName", workEffortName);
            }
            if (!UtilValidate.isEmpty(description)) {
                serviceContext.put("description", description);
            }
            if (!UtilValidate.isEmpty(routingId)) {
                serviceContext.put("routingId", routingId);
            }
            if (!UtilValidate.isEmpty(shipmentId) && UtilValidate.isEmpty(workEffortName)) {
                serviceContext.put("workEffortName", "SP_" + shipmentId + "_" + serviceContext.get("productId"));
            }

            serviceContext.put("pRQuantity", getQuantity());
            if (UtilValidate.isNotEmpty(maxEndDate)) {
                serviceContext.put("startDate", maxEndDate);
            } else {
                serviceContext.put("startDate", startDate);
            }
            serviceContext.put("userLogin", userLogin);
            Map resultService = null;
            try {
                resultService = dispatcher.runSync("createProductionRun", serviceContext);
                productionRunId = (String)resultService.get("productionRunId");
                endDate = (Timestamp)resultService.get("estimatedCompletionDate");
            } catch (GenericServiceException e) {
                Debug.logError("Problem calling the createProductionRun service", module);
            }
            try {
                if (productionRunId != null) {
                    if (orderId != null && orderItemSeqId != null) {
                        delegator.create("WorkOrderItemFulfillment", UtilMisc.toMap("workEffortId", productionRunId, "orderId", orderId, "orderItemSeqId", orderItemSeqId));
                    }
                    for (int i = 0; i < childProductionRuns.size(); i++) {
                        delegator.create("WorkEffortAssoc", UtilMisc.toMap("workEffortIdFrom", (String)childProductionRuns.get(i), "workEffortIdTo", productionRunId, "workEffortAssocTypeId", "WORK_EFF_PRECEDENCY", "fromDate", startDate));
                    }
                }
            } catch (GenericEntityException e) {
                //Debug.logError(e, "Problem calling the getManufacturingComponents service", module);
            }
        }
        return UtilMisc.toMap("productionRunId", productionRunId, "endDate", endDate);
    }

    public Timestamp getStartDate(String facilityId, Timestamp requiredBydate, boolean allNodes) {
        Timestamp minStartDate = requiredBydate;
        if ("WIP".equals(getProduct().getString("productTypeId")) || allNodes) {
            ProposedOrder proposedOrder = new ProposedOrder(getProduct(), facilityId, facilityId, true, requiredBydate, getQuantity());
            proposedOrder.calculateStartDate(0, null, delegator, dispatcher, userLogin);
            Timestamp startDate = proposedOrder.getRequirementStartDate();
            minStartDate = startDate;
            for (int i = 0; i < childrenNodes.size(); i++) {
                BOMNode oneChildNode = (BOMNode)childrenNodes.get(i);
                if (oneChildNode != null) {
                    Timestamp childStartDate = oneChildNode.getStartDate(facilityId, startDate, false);
                    if (childStartDate.compareTo(minStartDate) < 0) {
                        minStartDate = childStartDate;
                    }
                }
            }
        }
        return minStartDate;
    }

    /**
     * Returns false if the product of this BOM Node is of type "WIP" or if it has no ProductFacility records defined for it,
     * meaning that no active stock targets are set for this product.
     */
    public boolean isWarehouseManaged(String facilityId) {
        boolean isWarehouseManaged = false;
        try {
            if ("WIP".equals(getProduct().getString("productTypeId"))) {
                return false;
            }
            List pfs = null;
            if (UtilValidate.isEmpty(facilityId)) {
                pfs = getProduct().getRelatedCache("ProductFacility");
            } else {
                pfs = getProduct().getRelatedCache("ProductFacility", UtilMisc.toMap("facilityId", facilityId), null);
            }
            if (UtilValidate.isEmpty(pfs)) {
                if (getSubstitutedNode() != null && getSubstitutedNode().getProduct() != null) {
                    if (UtilValidate.isEmpty(facilityId)) {
                        pfs = getSubstitutedNode().getProduct().getRelatedCache("ProductFacility");
                    } else {
                        pfs = getSubstitutedNode().getProduct().getRelatedCache("ProductFacility", UtilMisc.toMap("facilityId", facilityId), null);
                    }
                }
            }
            if (UtilValidate.isNotEmpty(pfs)) {
                for (int i = 0; i < pfs.size(); i++) {
                    GenericValue pf = (GenericValue)pfs.get(i);
                    if (UtilValidate.isNotEmpty(pf.get("minimumStock")) && UtilValidate.isNotEmpty(pf.get("reorderQuantity"))) {
                        isWarehouseManaged = true;
                        break;
                    }
                }
            }
        } catch (GenericEntityException gee) {
            Debug.logError("Problem in BOMNode.isWarehouseManaged()", module);
        }
    return isWarehouseManaged;
    }

    /**
     * A part is considered manufactured if it has child nodes AND unless ignoreSupplierProducts is set, if it also has no unexpired SupplierProducts defined
     * @param ignoreSupplierProducts
     * @return
     */
    public boolean isManufactured(boolean ignoreSupplierProducts) {
        List supplierProducts = null;
        try {
            supplierProducts = product.getRelated("SupplierProduct", UtilMisc.toMap("supplierPrefOrderId", "10_MAIN_SUPPL"), UtilMisc.toList("minimumOrderQuantity"));
        } catch (GenericEntityException gee) {
            Debug.logError("Problem in BOMNode.isManufactured()", module);
        }
        supplierProducts = EntityUtil.filterByDate(supplierProducts, UtilDateTime.nowTimestamp(), "availableFromDate", "availableThruDate", true);
        return childrenNodes.size() > 0 && (ignoreSupplierProducts || UtilValidate.isEmpty(supplierProducts));
    }

    /**
     * By default, a part is manufactured if it has child nodes and it has NO SupplierProducts defined
     * @return
     */
    public boolean isManufactured() {
        return isManufactured(false);
    }

    public boolean isVirtual() {
        return (product.get("isVirtual") != null? product.get("isVirtual").equals("Y"): false);
    }

    public void isConfigured(ArrayList arr) {
        // First of all we visit the current node.
        if (isVirtual()) {
            arr.add(this);
        }
        // Now (recursively) we visit the children.
        GenericValue oneChild = null;
        BOMNode oneChildNode = null;
        for (int i = 0; i < children.size(); i++) {
            oneChild = (GenericValue)children.get(i);
            oneChildNode = (BOMNode)childrenNodes.get(i);
            if (oneChildNode != null) {
                oneChildNode.isConfigured(arr);
            }
        }
    }


    /** Getter for property quantity.
     * @return Value of property quantity.
     *
     */
    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    /** Getter for property depth.
     * @return Value of property depth.
     *
     */

    public int getDepth() {
        return depth;
    }

    public GenericValue getProduct() {
        return product;
    }

    /** Getter for property substitutedNode.
     * @return Value of property substitutedNode.
     *
     */
    public BOMNode getSubstitutedNode() {
        return substitutedNode;
    }

    /** Setter for property substitutedNode.
     * @param substitutedNode New value of property substitutedNode.
     *
     */
    public void setSubstitutedNode(BOMNode substitutedNode) {
        this.substitutedNode = substitutedNode;
    }

    public String getRootProductForRules() {
        return getParentNode().getProductForRules();
    }

    /** Getter for property productForRules.
     * @return Value of property productForRules.
     *
     */
    public String getProductForRules() {
        return productForRules;
    }

    /** Setter for property productForRules.
     * @param productForRules New value of property productForRules.
     *
     */
    public void setProductForRules(String productForRules) {
        this.productForRules = productForRules;
    }

    /** Getter for property bomTypeId.
     * @return Value of property bomTypeId.
     *
     */
    public java.lang.String getBomTypeId() {
        return bomTypeId;
    }

    /** Getter for property quantityMultiplier.
     * @return Value of property quantityMultiplier.
     *
     */
    public BigDecimal getQuantityMultiplier() {
        return quantityMultiplier;
    }

    /** Setter for property quantityMultiplier.
     * @param quantityMultiplier New value of property quantityMultiplier.
     *
     */
    public void setQuantityMultiplier(BigDecimal quantityMultiplier) {
        if (quantityMultiplier != null) {
            this.quantityMultiplier = quantityMultiplier;
        }
    }

    /** Getter for property ruleApplied.
     * @return Value of property ruleApplied.
     *
     */
    public org.ofbiz.entity.GenericValue getRuleApplied() {
        return ruleApplied;
    }

    /** Setter for property ruleApplied.
     * @param ruleApplied New value of property ruleApplied.
     *
     */
    public void setRuleApplied(org.ofbiz.entity.GenericValue ruleApplied) {
        this.ruleApplied = ruleApplied;
    }

    /** Getter for property scrapFactor.
     * @return Value of property scrapFactor.
     *
     */
    public BigDecimal getScrapFactor() {
        return scrapFactor;
    }

    /** Setter for property scrapFactor.
     * @param scrapFactor New value of property scrapFactor.
     *
     */
    public void setScrapFactor(BigDecimal scrapFactor) {
        if (scrapFactor != null) {
            this.scrapFactor = scrapFactor;
        }
    }

    /** Getter for property childrenNodes.
     * @return Value of property childrenNodes.
     *
     */
    public java.util.ArrayList getChildrenNodes() {
        return childrenNodes;
    }

    /** Setter for property childrenNodes.
     * @param childrenNodes New value of property childrenNodes.
     *
     */
    public void setChildrenNodes(java.util.ArrayList childrenNodes) {
        this.childrenNodes = childrenNodes;
    }

    /** Getter for property productAssoc.
     * @return Value of property productAssoc.
     *
     */
    public org.ofbiz.entity.GenericValue getProductAssoc() {
        return productAssoc;
    }

    /** Setter for property productAssoc.
     * @param productAssoc New value of property productAssoc.
     *
     */
    public void setProductAssoc(org.ofbiz.entity.GenericValue productAssoc) {
        this.productAssoc = productAssoc;
    }

    public void setTree(BOMTree tree) {
        this.tree = tree;
    }

    public BOMTree getTree() {
        return tree;
    }

}

