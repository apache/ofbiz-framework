/*
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */

package org.apache.ofbiz.order.order;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.ObjectType;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.collections.PagedList;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityComparisonOperator;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityConditionList;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.model.DynamicViewEntity;
import org.apache.ofbiz.entity.model.ModelKeyMap;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.widget.renderer.Paginator;

/**
 * OrderLookupServices
 */
public class OrderLookupServices {

    private static final String MODULE = OrderLookupServices.class.getName();

    public static Map<String, Object> findOrders(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Integer viewIndex = Paginator.getViewIndex(context, "viewIndex", 1);
        Integer viewSize = Paginator.getViewSize(context, "viewSize");

        String showAll = (String) context.get("showAll");
        String useEntryDate = (String) context.get("useEntryDate");
        Locale locale = (Locale) context.get("locale");
        if (showAll == null) {
            showAll = "N";
        }

        // list of fields to select (initial list)
        Set<String> fieldsToSelect = new LinkedHashSet<>();
        fieldsToSelect.add("orderId");
        fieldsToSelect.add("orderName");
        fieldsToSelect.add("statusId");
        fieldsToSelect.add("orderTypeId");
        fieldsToSelect.add("orderDate");
        fieldsToSelect.add("currencyUom");
        fieldsToSelect.add("grandTotal");
        fieldsToSelect.add("remainingSubTotal");

        // sorting by order date newest first
        List<String> orderBy = UtilMisc.toList("-orderDate", "-orderId");

        // list to hold the parameters
        List<String> paramList = new LinkedList<>();

        // list of conditions
        List<EntityCondition> conditions = new LinkedList<>();

        // check security flag for purchase orders
        boolean canViewPo = security.hasEntityPermission("ORDERMGR", "_PURCHASE_VIEW", userLogin);
        if (!canViewPo) {
            conditions.add(EntityCondition.makeCondition("orderTypeId", EntityOperator.NOT_EQUAL, "PURCHASE_ORDER"));
        }

        // dynamic view entity
        DynamicViewEntity dve = new DynamicViewEntity();
        dve.addMemberEntity("OH", "OrderHeader");
        dve.addAliasAll("OH", "", null); // no prefix
        dve.addRelation("one-nofk", "", "OrderType", UtilMisc.toList(new ModelKeyMap("orderTypeId", "orderTypeId")));
        dve.addRelation("one-nofk", "", "StatusItem", UtilMisc.toList(new ModelKeyMap("statusId", "statusId")));

        // start the lookup
        String orderId = (String) context.get("orderId");
        if (UtilValidate.isNotEmpty(orderId)) {
            paramList.add("orderId=" + orderId);
            conditions.add(makeExpr("orderId", orderId));
        }

        // the base order header fields
        List<String> orderTypeList = UtilGenerics.cast(context.get("orderTypeId"));
        if (orderTypeList != null) {
            List<EntityExpr> orExprs = new LinkedList<>();
            for (String orderTypeId : orderTypeList) {
                paramList.add("orderTypeId=" + orderTypeId);

                if (!("PURCHASE_ORDER".equals(orderTypeId)) || (("PURCHASE_ORDER".equals(orderTypeId) && canViewPo))) {
                    orExprs.add(EntityCondition.makeCondition("orderTypeId", EntityOperator.EQUALS, orderTypeId));
                }
            }
            conditions.add(EntityCondition.makeCondition(orExprs, EntityOperator.OR));
        }

        String orderName = (String) context.get("orderName");
        if (UtilValidate.isNotEmpty(orderName)) {
            paramList.add("orderName=" + orderName);
            conditions.add(makeExpr("orderName", orderName, true));
        }

        List<String> orderStatusList = UtilGenerics.cast(context.get("orderStatusId"));
        if (orderStatusList != null) {
            List<EntityCondition> orExprs = new LinkedList<>();
            for (String orderStatusId : orderStatusList) {
                paramList.add("orderStatusId=" + orderStatusId);
                if ("PENDING".equals(orderStatusId)) {
                    List<EntityExpr> pendExprs = new LinkedList<>();
                    pendExprs.add(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ORDER_CREATED"));
                    pendExprs.add(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ORDER_PROCESSING"));
                    pendExprs.add(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ORDER_APPROVED"));
                    orExprs.add(EntityCondition.makeCondition(pendExprs, EntityOperator.OR));
                } else {
                    orExprs.add(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, orderStatusId));
                }
            }
            conditions.add(EntityCondition.makeCondition(orExprs, EntityOperator.OR));
        }

        List<String> productStoreList = UtilGenerics.cast(context.get("productStoreId"));
        if (productStoreList != null) {
            List<EntityExpr> orExprs = new LinkedList<>();
            for (String productStoreId : productStoreList) {
                paramList.add("productStoreId=" + productStoreId);
                orExprs.add(EntityCondition.makeCondition("productStoreId", EntityOperator.EQUALS, productStoreId));
            }
            conditions.add(EntityCondition.makeCondition(orExprs, EntityOperator.OR));
        }

        List<String> webSiteList = UtilGenerics.cast(context.get("orderWebSiteId"));
        if (webSiteList != null) {
            List<EntityExpr> orExprs = new LinkedList<>();
            for (String webSiteId : webSiteList) {
                paramList.add("webSiteId=" + webSiteId);
                orExprs.add(EntityCondition.makeCondition("webSiteId", EntityOperator.EQUALS, webSiteId));
            }
            conditions.add(EntityCondition.makeCondition(orExprs, EntityOperator.OR));
        }

        List<String> saleChannelList = UtilGenerics.cast(context.get("salesChannelEnumId"));
        if (saleChannelList != null) {
            List<EntityExpr> orExprs = new LinkedList<>();
            for (String salesChannelEnumId : saleChannelList) {
                paramList.add("salesChannelEnumId=" + salesChannelEnumId);
                orExprs.add(EntityCondition.makeCondition("salesChannelEnumId", EntityOperator.EQUALS, salesChannelEnumId));
            }
            conditions.add(EntityCondition.makeCondition(orExprs, EntityOperator.OR));
        }

        String createdBy = (String) context.get("createdBy");
        if (UtilValidate.isNotEmpty(createdBy)) {
            paramList.add("createdBy=" + createdBy);
            conditions.add(makeExpr("createdBy", createdBy));
        }

        String terminalId = (String) context.get("terminalId");
        if (UtilValidate.isNotEmpty(terminalId)) {
            paramList.add("terminalId=" + terminalId);
            conditions.add(makeExpr("terminalId", terminalId));
        }

        String transactionId = (String) context.get("transactionId");
        if (UtilValidate.isNotEmpty(transactionId)) {
            paramList.add("transactionId=" + transactionId);
            conditions.add(makeExpr("transactionId", transactionId));
        }

        String externalId = (String) context.get("externalId");
        if (UtilValidate.isNotEmpty(externalId)) {
            paramList.add("externalId=" + externalId);
            conditions.add(makeExpr("externalId", externalId));
        }

        String internalCode = (String) context.get("internalCode");
        if (UtilValidate.isNotEmpty(internalCode)) {
            paramList.add("internalCode=" + internalCode);
            conditions.add(makeExpr("internalCode", internalCode));
        }

        String dateField = "Y".equals(useEntryDate) ? "entryDate" : "orderDate";
        String minDate = (String) context.get("minDate");
        if (UtilValidate.isNotEmpty(minDate) && minDate.length() > 8) {
            minDate = minDate.trim();
            if (minDate.length() < 14) {
                minDate = minDate + " " + "00:00:00.000";
            }
            paramList.add("minDate=" + minDate);

            try {
                Object converted = ObjectType.simpleTypeOrObjectConvert(minDate, "Timestamp", null, null);
                if (converted != null) {
                    conditions.add(EntityCondition.makeCondition(dateField, EntityOperator.GREATER_THAN_EQUAL_TO, converted));
                }
            } catch (GeneralException e) {
                Debug.logWarning(e.getMessage(), MODULE);
            }
        }

        String maxDate = (String) context.get("maxDate");
        if (UtilValidate.isNotEmpty(maxDate) && maxDate.length() > 8) {
            maxDate = maxDate.trim();
            if (maxDate.length() < 14) {
                maxDate = maxDate + " " + "23:59:59.999";
            }
            paramList.add("maxDate=" + maxDate);

            try {
                Object converted = ObjectType.simpleTypeOrObjectConvert(maxDate, "Timestamp", null, null);
                if (converted != null) {
                    conditions.add(EntityCondition.makeCondition("orderDate", EntityOperator.LESS_THAN_EQUAL_TO, converted));
                }
            } catch (GeneralException e) {
                Debug.logWarning(e.getMessage(), MODULE);
            }
        }

        // party (role) fields
        String userLoginId = (String) context.get("userLoginId");
        String partyId = (String) context.get("partyId");
        List<String> roleTypeList = UtilGenerics.cast(context.get("roleTypeId"));

        if (UtilValidate.isNotEmpty(userLoginId) && UtilValidate.isEmpty(partyId)) {
            GenericValue ul = null;
            try {
                ul = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", userLoginId).cache().queryOne();
            } catch (GenericEntityException e) {
                Debug.logWarning(e.getMessage(), MODULE);
            }
            if (ul != null) {
                partyId = ul.getString("partyId");
            }
        }

        String isViewed = (String) context.get("isViewed");
        if (UtilValidate.isNotEmpty(isViewed)) {
            paramList.add("isViewed=" + isViewed);
            conditions.add(makeExpr("isViewed", isViewed));
        }

        // Shipment Method
        String shipmentMethod = (String) context.get("shipmentMethod");
        if (UtilValidate.isNotEmpty(shipmentMethod)) {
            String carrierPartyId = shipmentMethod.substring(0, shipmentMethod.indexOf('@'));
            String shippingMethodTypeId = shipmentMethod.substring(shipmentMethod.indexOf('@') + 1);
            dve.addMemberEntity("OISG", "OrderItemShipGroup");
            dve.addAlias("OISG", "shipmentMethodTypeId");
            dve.addAlias("OISG", "carrierPartyId");
            dve.addViewLink("OH", "OISG", Boolean.FALSE, UtilMisc.toList(new ModelKeyMap("orderId", "orderId")));

            if (UtilValidate.isNotEmpty(carrierPartyId)) {
                paramList.add("carrierPartyId=" + carrierPartyId);
                conditions.add(makeExpr("carrierPartyId", carrierPartyId));
            }

            if (UtilValidate.isNotEmpty(shippingMethodTypeId)) {
                paramList.add("shippingMethodTypeId=" + shippingMethodTypeId);
                conditions.add(makeExpr("shipmentMethodTypeId", shippingMethodTypeId));
            }
        }
        // PaymentGatewayResponse
        String gatewayAvsResult = (String) context.get("gatewayAvsResult");
        String gatewayScoreResult = (String) context.get("gatewayScoreResult");
        if (UtilValidate.isNotEmpty(gatewayAvsResult) || UtilValidate.isNotEmpty(gatewayScoreResult)) {
            dve.addMemberEntity("OPP", "OrderPaymentPreference");
            dve.addMemberEntity("PGR", "PaymentGatewayResponse");
            dve.addAlias("OPP", "orderPaymentPreferenceId");
            dve.addAlias("PGR", "gatewayAvsResult");
            dve.addAlias("PGR", "gatewayScoreResult");
            dve.addViewLink("OH", "OPP", Boolean.FALSE, UtilMisc.toList(new ModelKeyMap("orderId", "orderId")));
            dve.addViewLink("OPP", "PGR", Boolean.FALSE, UtilMisc.toList(new ModelKeyMap("orderPaymentPreferenceId", "orderPaymentPreferenceId")));
        }

        if (UtilValidate.isNotEmpty(gatewayAvsResult)) {
            paramList.add("gatewayAvsResult=" + gatewayAvsResult);
            conditions.add(EntityCondition.makeCondition("gatewayAvsResult", gatewayAvsResult));
        }

        if (UtilValidate.isNotEmpty(gatewayScoreResult)) {
            paramList.add("gatewayScoreResult=" + gatewayScoreResult);
            conditions.add(EntityCondition.makeCondition("gatewayScoreResult", gatewayScoreResult));
        }

        // add the role data to the view
        if (roleTypeList != null || partyId != null) {
            dve.addMemberEntity("OT", "OrderRole");
            dve.addAlias("OT", "partyId");
            dve.addAlias("OT", "roleTypeId");
            dve.addViewLink("OH", "OT", Boolean.FALSE, UtilMisc.toList(new ModelKeyMap("orderId", "orderId")));
        }

        if (UtilValidate.isNotEmpty(partyId)) {
            paramList.add("partyId=" + partyId);
            fieldsToSelect.add("partyId");
            conditions.add(makeExpr("partyId", partyId));
        }

        if (roleTypeList != null) {
            fieldsToSelect.add("roleTypeId");
            List<EntityExpr> orExprs = new LinkedList<>();
            for (String roleTypeId : roleTypeList) {
                paramList.add("roleTypeId=" + roleTypeId);
                orExprs.add(makeExpr("roleTypeId", roleTypeId));
            }
            conditions.add(EntityCondition.makeCondition(orExprs, EntityOperator.OR));
        }

        // order item fields
        String correspondingPoId = (String) context.get("correspondingPoId");
        String subscriptionId = (String) context.get("subscriptionId");
        String productId = (String) context.get("productId");
        String budgetId = (String) context.get("budgetId");
        String quoteId = (String) context.get("quoteId");

        String goodIdentificationTypeId = (String) context.get("goodIdentificationTypeId");
        String goodIdentificationIdValue = (String) context.get("goodIdentificationIdValue");
        boolean hasGoodIdentification = UtilValidate.isNotEmpty(goodIdentificationTypeId) && UtilValidate.isNotEmpty(goodIdentificationIdValue);

        if (correspondingPoId != null || subscriptionId != null || productId != null || budgetId != null || quoteId != null
                || hasGoodIdentification) {
            dve.addMemberEntity("OI", "OrderItem");
            dve.addAlias("OI", "correspondingPoId");
            dve.addAlias("OI", "subscriptionId");
            dve.addAlias("OI", "productId");
            dve.addAlias("OI", "budgetId");
            dve.addAlias("OI", "quoteId");
            dve.addViewLink("OH", "OI", Boolean.FALSE, UtilMisc.toList(new ModelKeyMap("orderId", "orderId")));

            if (hasGoodIdentification) {
                dve.addMemberEntity("GOODID", "GoodIdentification");
                dve.addAlias("GOODID", "goodIdentificationTypeId");
                dve.addAlias("GOODID", "idValue");
                dve.addViewLink("OI", "GOODID", Boolean.FALSE, UtilMisc.toList(new ModelKeyMap("productId", "productId")));
                paramList.add("goodIdentificationTypeId=" + goodIdentificationTypeId);
                conditions.add(makeExpr("goodIdentificationTypeId", goodIdentificationTypeId));
                paramList.add("goodIdentificationIdValue=" + goodIdentificationIdValue);
                conditions.add(makeExpr("idValue", goodIdentificationIdValue));
            }
        }

        if (UtilValidate.isNotEmpty(correspondingPoId)) {
            paramList.add("correspondingPoId=" + correspondingPoId);
            conditions.add(makeExpr("correspondingPoId", correspondingPoId));
        }

        if (UtilValidate.isNotEmpty(subscriptionId)) {
            paramList.add("subscriptionId=" + subscriptionId);
            conditions.add(makeExpr("subscriptionId", subscriptionId));
        }

        if (UtilValidate.isNotEmpty(productId)) {
            paramList.add("productId=" + productId);
            if (productId.startsWith("%") || productId.startsWith("*") || productId.endsWith("%") || productId.endsWith("*")) {
                conditions.add(makeExpr("productId", productId));
            } else {
                GenericValue product = null;
                try {
                    product = EntityQuery.use(delegator).from("Product").where("productId", productId).queryOne();
                } catch (GenericEntityException e) {
                    Debug.logWarning(e.getMessage(), MODULE);
                }
                if (product != null) {
                    String isVirtual = product.getString("isVirtual");
                    if (isVirtual != null && "Y".equals(isVirtual)) {
                        List<EntityExpr> orExprs = new LinkedList<>();
                        orExprs.add(EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId));

                        Map<String, Object> varLookup = null;
                        List<GenericValue> variants = null;
                        try {
                            varLookup = dispatcher.runSync("getAllProductVariants", UtilMisc.toMap("productId", productId));
                            if (ServiceUtil.isError(varLookup)) {
                                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(varLookup));
                            }
                            variants = UtilGenerics.cast(varLookup.get("assocProducts"));

                        } catch (GenericServiceException e) {
                            Debug.logWarning(e.getMessage(), MODULE);
                        }
                        if (variants != null) {
                            for (GenericValue v : variants) {
                                orExprs.add(EntityCondition.makeCondition("productId", EntityOperator.EQUALS, v.getString("productIdTo")));
                            }
                        }
                        conditions.add(EntityCondition.makeCondition(orExprs, EntityOperator.OR));
                    } else {
                        conditions.add(EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId));
                    }
                } else {
                    String failMsg = UtilProperties.getMessage("OrderErrorUiLabels", "OrderFindOrderProductInvalid",
                            UtilMisc.toMap("productId", productId), locale);
                    return ServiceUtil.returnFailure(failMsg);
                }
            }
        }

        if (UtilValidate.isNotEmpty(budgetId)) {
            paramList.add("budgetId=" + budgetId);
            conditions.add(makeExpr("budgetId", budgetId));
        }

        if (UtilValidate.isNotEmpty(quoteId)) {
            paramList.add("quoteId=" + quoteId);
            conditions.add(makeExpr("quoteId", quoteId));
        }

        // payment preference fields
        String billingAccountId = (String) context.get("billingAccountId");
        String finAccountId = (String) context.get("finAccountId");
        String cardNumber = (String) context.get("cardNumber");
        String accountNumber = (String) context.get("accountNumber");
        String paymentStatusId = (String) context.get("paymentStatusId");

        if (UtilValidate.isNotEmpty(paymentStatusId)) {
            paramList.add("paymentStatusId=" + paymentStatusId);
            conditions.add(makeExpr("paymentStatusId", paymentStatusId));
        }
        if (finAccountId != null || cardNumber != null || accountNumber != null || paymentStatusId != null) {
            dve.addMemberEntity("OP", "OrderPaymentPreference");
            dve.addAlias("OP", "finAccountId");
            dve.addAlias("OP", "paymentMethodId");
            dve.addAlias("OP", "paymentStatusId", "statusId", null, false, false, null);
            dve.addViewLink("OH", "OP", Boolean.FALSE, UtilMisc.toList(new ModelKeyMap("orderId", "orderId")));
        }

        // search by billing account ID
        if (UtilValidate.isNotEmpty(billingAccountId)) {
            paramList.add("billingAccountId=" + billingAccountId);
            conditions.add(makeExpr("billingAccountId", billingAccountId));
        }

        // search by fin account ID
        if (UtilValidate.isNotEmpty(finAccountId)) {
            paramList.add("finAccountId=" + finAccountId);
            conditions.add(makeExpr("finAccountId", finAccountId));
        }

        // search by card number
        if (UtilValidate.isNotEmpty(cardNumber)) {
            dve.addMemberEntity("CC", "CreditCard");
            dve.addAlias("CC", "cardNumber");
            dve.addViewLink("OP", "CC", Boolean.FALSE, UtilMisc.toList(new ModelKeyMap("paymentMethodId", "paymentMethodId")));

            paramList.add("cardNumber=" + cardNumber);
            conditions.add(makeExpr("cardNumber", cardNumber));
        }

        // search by eft account number
        if (UtilValidate.isNotEmpty(accountNumber)) {
            dve.addMemberEntity("EF", "EftAccount");
            dve.addAlias("EF", "accountNumber");
            dve.addViewLink("OP", "EF", Boolean.FALSE, UtilMisc.toList(new ModelKeyMap("paymentMethodId", "paymentMethodId")));

            paramList.add("accountNumber=" + accountNumber);
            conditions.add(makeExpr("accountNumber", accountNumber));
        }

        // shipment/inventory item
        String inventoryItemId = (String) context.get("inventoryItemId");
        String softIdentifier = (String) context.get("softIdentifier");
        String serialNumber = (String) context.get("serialNumber");
        String shipmentId = (String) context.get("shipmentId");

        if (shipmentId != null || inventoryItemId != null || softIdentifier != null || serialNumber != null) {
            dve.addMemberEntity("II", "ItemIssuance");
            dve.addAlias("II", "shipmentId");
            dve.addAlias("II", "inventoryItemId");
            dve.addViewLink("OH", "II", Boolean.FALSE, UtilMisc.toList(new ModelKeyMap("orderId", "orderId")));

            if (softIdentifier != null || serialNumber != null) {
                dve.addMemberEntity("IV", "InventoryItem");
                dve.addAlias("IV", "softIdentifier");
                dve.addAlias("IV", "serialNumber");
                dve.addViewLink("II", "IV", Boolean.FALSE, UtilMisc.toList(new ModelKeyMap("inventoryItemId", "inventoryItemId")));
            }
        }

        if (UtilValidate.isNotEmpty(inventoryItemId)) {
            paramList.add("inventoryItemId=" + inventoryItemId);
            conditions.add(makeExpr("inventoryItemId", inventoryItemId));
        }

        if (UtilValidate.isNotEmpty(softIdentifier)) {
            paramList.add("softIdentifier=" + softIdentifier);
            conditions.add(makeExpr("softIdentifier", softIdentifier, true));
        }

        if (UtilValidate.isNotEmpty(serialNumber)) {
            paramList.add("serialNumber=" + serialNumber);
            conditions.add(makeExpr("serialNumber", serialNumber, true));
        }

        if (UtilValidate.isNotEmpty(shipmentId)) {
            paramList.add("shipmentId=" + shipmentId);
            conditions.add(makeExpr("shipmentId", shipmentId));
        }

        // back order checking
        String hasBackOrders = (String) context.get("hasBackOrders");
        if (UtilValidate.isNotEmpty(hasBackOrders)) {
            dve.addMemberEntity("IR", "OrderItemShipGrpInvRes");
            dve.addAlias("IR", "quantityNotAvailable");
            dve.addViewLink("OH", "IR", Boolean.FALSE, UtilMisc.toList(new ModelKeyMap("orderId", "orderId")));

            paramList.add("hasBackOrders=" + hasBackOrders);
            if ("Y".equals(hasBackOrders)) {
                conditions.add(EntityCondition.makeCondition("quantityNotAvailable", EntityOperator.NOT_EQUAL, null));
                conditions.add(EntityCondition.makeCondition("quantityNotAvailable", EntityOperator.GREATER_THAN, BigDecimal.ZERO));
            } else if ("N".equals(hasBackOrders)) {
                List<EntityExpr> orExpr = new LinkedList<>();
                orExpr.add(EntityCondition.makeCondition("quantityNotAvailable", EntityOperator.EQUALS, null));
                orExpr.add(EntityCondition.makeCondition("quantityNotAvailable", EntityOperator.EQUALS, BigDecimal.ZERO));
                conditions.add(EntityCondition.makeCondition(orExpr, EntityOperator.OR));
            }
        }

        // Get all orders according to specific ship to country with "Only Include" or "Do not Include".
        String countryGeoId = (String) context.get("countryGeoId");
        String includeCountry = (String) context.get("includeCountry");
        if (UtilValidate.isNotEmpty(countryGeoId) && UtilValidate.isNotEmpty(includeCountry)) {
            paramList.add("countryGeoId=" + countryGeoId);
            paramList.add("includeCountry=" + includeCountry);
            // add condition to dynamic view
            dve.addMemberEntity("OCM", "OrderContactMech");
            dve.addMemberEntity("PA", "PostalAddress");
            dve.addAlias("OCM", "contactMechId");
            dve.addAlias("OCM", "contactMechPurposeTypeId");
            dve.addAlias("PA", "countryGeoId");
            dve.addViewLink("OH", "OCM", Boolean.FALSE, ModelKeyMap.makeKeyMapList("orderId"));
            dve.addViewLink("OCM", "PA", Boolean.FALSE, ModelKeyMap.makeKeyMapList("contactMechId"));

            EntityConditionList<EntityExpr> exprs = null;
            if ("Y".equals(includeCountry)) {
                exprs = EntityCondition.makeCondition(UtilMisc.toList(
                            EntityCondition.makeCondition("contactMechPurposeTypeId", "SHIPPING_LOCATION"),
                            EntityCondition.makeCondition("countryGeoId", countryGeoId)), EntityOperator.AND);
            } else {
                exprs = EntityCondition.makeCondition(UtilMisc.toList(
                            EntityCondition.makeCondition("contactMechPurposeTypeId", "SHIPPING_LOCATION"),
                            EntityCondition.makeCondition("countryGeoId", EntityOperator.NOT_EQUAL, countryGeoId)), EntityOperator.AND);
            }
            conditions.add(exprs);
        }

        // create the main condition
        EntityCondition cond = null;
        if (!conditions.isEmpty() || "Y".equalsIgnoreCase(showAll)) {
            cond = EntityCondition.makeCondition(conditions, EntityOperator.AND);
        }

        if (Debug.verboseOn()) {
            Debug.logInfo("Find order query: " + cond.toString(), MODULE);
        }

        List<GenericValue> orderList = new LinkedList<>();
        int orderCount = 0;

        // get the index for the partial list
        int lowIndex = 0;
        int highIndex = 0;

        if (cond != null) {
            PagedList<GenericValue> pagedOrderList = null;
            try {
                // do the lookup
                pagedOrderList = EntityQuery.use(delegator)
                        .select(fieldsToSelect)
                        .from(dve)
                        .where(cond)
                        .orderBy(orderBy)
                        .distinct() // set distinct on so we only get one row per order
                        .cursorScrollInsensitive()
                        .queryPagedList(viewIndex - 1, viewSize);

                orderCount = pagedOrderList.getSize();
                lowIndex = pagedOrderList.getStartIndex();
                highIndex = pagedOrderList.getEndIndex();
                orderList = pagedOrderList.getData();
            } catch (GenericEntityException e) {
                Debug.logError(e.getMessage(), MODULE);
                return ServiceUtil.returnError(e.getMessage());
            }
        }

        // create the result map
        Map<String, Object> result = ServiceUtil.returnSuccess();

        // filter out requested inventory problems
        filterInventoryProblems(context, result, orderList, paramList);

        // format the param list
        String paramString = StringUtil.join(paramList, "&amp;");

        result.put("highIndex", highIndex);
        result.put("lowIndex", lowIndex);
        result.put("viewIndex", viewIndex);
        result.put("viewSize", viewSize);
        result.put("showAll", showAll);

        result.put("paramList", (paramString != null ? paramString : ""));
        result.put("orderList", orderList);
        result.put("orderListSize", orderCount);

        return result;
    }

    public static void filterInventoryProblems(Map<String, ? extends Object> context, Map<String, Object> result, List<GenericValue>
            orderList, List<String> paramList) {
        List<String> filterInventoryProblems = new LinkedList<>();

        String doFilter = (String) context.get("filterInventoryProblems");
        if (doFilter == null) {
            doFilter = "N";
        }

        if ("Y".equals(doFilter) && !orderList.isEmpty()) {
            paramList.add("filterInventoryProblems=Y");
            for (GenericValue orderHeader : orderList) {
                OrderReadHelper orh = new OrderReadHelper(orderHeader);
                BigDecimal backorderQty = orh.getOrderBackorderQuantity();
                if (backorderQty.compareTo(BigDecimal.ZERO) == 1) {
                    filterInventoryProblems.add(orh.getOrderId());
                }
            }
        }

        List<String> filterPOsOpenPastTheirETA = new LinkedList<>();
        List<String> filterPOsWithRejectedItems = new LinkedList<>();
        List<String> filterPartiallyReceivedPOs = new LinkedList<>();

        String filterPOReject = (String) context.get("filterPOsWithRejectedItems");
        String filterPOPast = (String) context.get("filterPOsOpenPastTheirETA");
        String filterPartRec = (String) context.get("filterPartiallyReceivedPOs");
        if (filterPOReject == null) {
            filterPOReject = "N";
        }
        if (filterPOPast == null) {
            filterPOPast = "N";
        }
        if (filterPartRec == null) {
            filterPartRec = "N";
        }

        boolean doPoFilter = false;
        if ("Y".equals(filterPOReject)) {
            paramList.add("filterPOsWithRejectedItems=Y");
            doPoFilter = true;
        }
        if ("Y".equals(filterPOPast)) {
            paramList.add("filterPOsOpenPastTheirETA=Y");
            doPoFilter = true;
        }
        if ("Y".equals(filterPartRec)) {
            paramList.add("filterPartiallyReceivedPOs=Y");
            doPoFilter = true;
        }

        if (doPoFilter && !orderList.isEmpty()) {
            for (GenericValue orderHeader : orderList) {
                OrderReadHelper orh = new OrderReadHelper(orderHeader);
                String orderType = orh.getOrderTypeId();
                String orderId = orh.getOrderId();

                if ("PURCHASE_ORDER".equals(orderType)) {
                    if ("Y".equals(filterPOReject) && orh.getRejectedOrderItems()) {
                        filterPOsWithRejectedItems.add(orderId);
                    } else if ("Y".equals(filterPOPast) && orh.getPastEtaOrderItems(orderId)) {
                        filterPOsOpenPastTheirETA.add(orderId);
                    } else if ("Y".equals(filterPartRec) && orh.getPartiallyReceivedItems()) {
                        filterPartiallyReceivedPOs.add(orderId);
                    }
                }
            }
        }

        result.put("filterInventoryProblemsList", filterInventoryProblems);
        result.put("filterPOsWithRejectedItemsList", filterPOsWithRejectedItems);
        result.put("filterPOsOpenPastTheirETAList", filterPOsOpenPastTheirETA);
        result.put("filterPartiallyReceivedPOsList", filterPartiallyReceivedPOs);
    }

    protected static EntityExpr makeExpr(String fieldName, String value) {
        return makeExpr(fieldName, value, false);
    }

    protected static EntityExpr makeExpr(String fieldName, String value, boolean forceLike) {
        EntityComparisonOperator<?, ?> op = forceLike ? EntityOperator.LIKE : EntityOperator.EQUALS;

        if (value.startsWith("*")) {
            op = EntityOperator.LIKE;
            value = "%" + value.substring(1);
        } else if (value.startsWith("%")) {
            op = EntityOperator.LIKE;
        }

        if (value.endsWith("*")) {
            op = EntityOperator.LIKE;
            value = value.substring(0, value.length() - 1) + "%";
        } else if (value.endsWith("%")) {
            op = EntityOperator.LIKE;
        }

        if (forceLike) {
            if (!value.startsWith("%")) {
                value = "%" + value;
            }
            if (!value.endsWith("%")) {
                value = value + "%";
            }
        }

        return EntityCondition.makeCondition(fieldName, op, value);
    }
}
