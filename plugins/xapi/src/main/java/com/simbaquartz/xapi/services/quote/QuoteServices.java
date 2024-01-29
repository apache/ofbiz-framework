package com.simbaquartz.xapi.services.quote;

import com.fidelissd.zcp.xcommon.collections.FastList;
import com.fidelissd.zcp.xcommon.collections.FastMap;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class QuoteServices {

    private static final String module = QuoteServices.class.getName();

    public static Map<String, Object> calculateAndApplyTaxOnQuote(DispatchContext dctx, Map<String, ? extends Object> context)
            throws GenericEntityException, GenericServiceException {

        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        String quoteId = (String) context.get("quoteId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        List<Map<String, Object>> calcTaxCtxList = FastList.newInstance();
        List<GenericValue> itemAdjustments = FastList.newInstance();
        List<GenericValue> headerAdjustments = FastList.newInstance();

        GenericValue quote = EntityQuery.use(delegator).from("Quote").where("quoteId", quoteId).queryOne();
        if (UtilValidate.isEmpty(quote)) {
            return ServiceUtil.returnError("Quote does not exist for quoteId : " + quoteId);
        }

        List<GenericValue> quoteRoles = EntityQuery.use(delegator).from("QuoteRole").where("quoteId", quoteId).queryList();
        GenericValue customerQuoteRole = EntityUtil.getFirst(EntityUtil.filterByAnd(quoteRoles, UtilMisc.toMap("roleTypeId", "SHIP_TO_CUSTOMER")));
        if (UtilValidate.isEmpty(customerQuoteRole)) {
            return ServiceUtil.returnError("Quote customer does not exist for quoteId : " + quoteId);
        }

        GenericValue supplierQuoteRole = EntityUtil.getFirst(EntityUtil.filterByAnd(quoteRoles, UtilMisc.toMap("roleTypeId", "SUPPLIER")));
        if (UtilValidate.isEmpty(supplierQuoteRole)) {
            return ServiceUtil.returnError("Quote supplier does not exist for quoteId : " + quoteId);
        }

        String customerPartyId = customerQuoteRole.getString("partyId");
        String supplierPartyId = supplierQuoteRole.getString("partyId");

        GenericValue supplierPostalAddress = getSupplierPostalAddressFromQuote(delegator, quoteId, supplierPartyId);
        if (UtilValidate.isEmpty(supplierPostalAddress)) {
            return ServiceUtil.returnError("Address does not exist for supplier : " + supplierPartyId);
        }

        List<GenericValue> quoteItemShipGroups = EntityQuery.use(delegator).from("QuoteItemShipGroup")
                .where("quoteId", quoteId, "supplierPartyId", supplierPartyId, "customerPartyId", customerPartyId)
                .queryList();


        GenericValue customerPostalAddress = null;
        Boolean isTaxCalculatedFromShipGroupItems = false;
        if (UtilValidate.isNotEmpty(quoteItemShipGroups)) {
            for (GenericValue quoteItemShipGroup : quoteItemShipGroups) {

                Map<String, Object> calcTaxCtx = FastMap.newInstance();
                List<EntityCondition> ecList = new LinkedList<EntityCondition>();
                ecList.add(EntityCondition.makeCondition("quoteId", EntityOperator.EQUALS, quoteId));
                ecList.add(EntityCondition.makeCondition("shipGroupSeqId", EntityOperator.EQUALS, quoteItemShipGroup.getString("shipGroupSeqId")));
                List<GenericValue> quoteItems = EntityQuery.use(delegator).from("QuoteItemAndShipGroupAssoc")
                        .where("quoteId", quoteId, "shipGroupSeqId", quoteItemShipGroup.getString("shipGroupSeqId"))
                        .queryList();


                customerPostalAddress = EntityQuery.use(delegator).from("PostalAddress").where("contactMechId", quoteItemShipGroup.getString("contactMechId")).queryOne();

                if (UtilValidate.isNotEmpty(quoteItems)) {
                    isTaxCalculatedFromShipGroupItems = true;
                    for (GenericValue quoteItem : quoteItems) {
                        addItemToTaxCtx(quoteItem, calcTaxCtx);
                    }
                } else {
                    break;
                }

                calcTaxCtx.put("billToPartyId", customerPartyId);
                calcTaxCtx.put("billToPartyId", customerPartyId);
                calcTaxCtx.put("shippingAddress", customerPostalAddress);
                calcTaxCtx.put("shipFromAddress", supplierPostalAddress);
                calcTaxCtx.put("userLogin", userLogin);
                calcTaxCtxList.add(calcTaxCtx);
            }
        }

        if (!isTaxCalculatedFromShipGroupItems) {

            if (UtilValidate.isEmpty(customerPostalAddress)) {
                customerPostalAddress = getPartyRecentPostalAddressFromProfile(delegator, customerPartyId);
            }

            if (UtilValidate.isEmpty(customerPostalAddress)) {
                return ServiceUtil.returnError("Address does not exist for customer : " + customerPartyId);

            }

            Map<String, Object> calcTaxCtx = FastMap.newInstance();
            List<GenericValue> quoteItems = quote.getRelated("QuoteItem");
            if (UtilValidate.isEmpty(quoteItems)) {
                Debug.logError("QuoteItems are empty for quoteId : " + quoteId, module);
                return ServiceUtil.returnError("QuoteItems are empty for quoteId : " + quoteId);
            }

            for (GenericValue quoteItem : quoteItems) {
                addItemToTaxCtx(quoteItem, calcTaxCtx);
            }

            calcTaxCtx.put("billToPartyId", customerPartyId);
            calcTaxCtx.put("shipFromAddress", supplierPostalAddress);
            calcTaxCtx.put("shippingAddress", customerPostalAddress);
            calcTaxCtx.put("userLogin", userLogin);
            calcTaxCtxList.add(calcTaxCtx);
        }

        // Now take the avalara call and prepare the adjustment list
        for (Map<String, Object> calcTaxCtx : calcTaxCtxList) {

            Map<String, Object> calcTaxByAvalaraResult = dispatcher.runSync("calcTaxByAvalara", calcTaxCtx);

            if (ServiceUtil.isError(calcTaxByAvalaraResult)) {
                Debug.logError("An error occured while invoking calcTaxByAvalara service, details: " +
                        ServiceUtil.getErrorMessage(calcTaxByAvalaraResult), module);

                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(calcTaxByAvalaraResult));
            }

            itemAdjustments.addAll((List<GenericValue>) calcTaxByAvalaraResult.get("itemAdjustments"));
            headerAdjustments.addAll((List<GenericValue>) calcTaxByAvalaraResult.get("orderAdjustments"));
        }

        // Wipe out all the SALES_TAX Adjustment
        List<GenericValue> quoteTaxAdjustments = EntityQuery.use(delegator).from("QuoteAdjustment")
                .where("quoteId", quoteId, "quoteAdjustmentTypeId", "SALES_TAX").queryList();

        for (GenericValue quoteTaxAdjustment : quoteTaxAdjustments) {
            delegator.removeValue(quoteTaxAdjustment);
        }

        // Apply new tax adjustments over
        for(GenericValue itemAdjustment : itemAdjustments) {
            String key = itemAdjustment.getString("description");
            String[] keys = key.split("\\|");
            String quoteItemId = keys[1];
            String shipGroupSeqId = null;
            if (keys.length > 2) {
                shipGroupSeqId = keys[2];
            }

            GenericValue quoteItemAdjustment = delegator.makeValue("QuoteAdjustment");
            quoteItemAdjustment.set("quoteAdjustmentId", delegator.getNextSeqId("QuoteAdjustment"));
            quoteItemAdjustment.set("quoteId", quoteId);
            quoteItemAdjustment.set("quoteItemSeqId", quoteItemId);
            if (UtilValidate.isNotEmpty(shipGroupSeqId)) {
                quoteItemAdjustment.set("shipGroupSeqId", shipGroupSeqId);
            }
            quoteItemAdjustment.set("quoteAdjustmentTypeId", "SALES_TAX");
            quoteItemAdjustment.set("comments", itemAdjustment.getString("comments"));
            quoteItemAdjustment.set("amount", itemAdjustment.getBigDecimal("amount"));
            quoteItemAdjustment.set("overrideGlAccountId", itemAdjustment.getString("overrideGlAccountId"));
            quoteItemAdjustment.set("taxAuthGeoId", itemAdjustment.getString("taxAuthGeoId"));
            quoteItemAdjustment.set("taxAuthPartyId", itemAdjustment.getString("taxAuthPartyId"));
            quoteItemAdjustment.set("sourcePercentage", itemAdjustment.getBigDecimal("sourcePercentage"));

            delegator.create(quoteItemAdjustment);

        }

        for(GenericValue headerAdjustment : headerAdjustments) {
            // Assuming we have only one header level adjustment of SALES_TAX type.
            GenericValue quoteAdjustment = delegator.makeValue("QuoteAdjustment");
            quoteAdjustment.set("quoteAdjustmentId", delegator.getNextSeqId("QuoteAdjustment"));
            quoteAdjustment.set("quoteId", quoteId);
            quoteAdjustment.set("quoteItemSeqId", "_NA_");
            quoteAdjustment.set("quoteAdjustmentTypeId", "SALES_TAX");
            quoteAdjustment.set("comments", headerAdjustment.getString("comments"));
            quoteAdjustment.set("amount", headerAdjustment.getBigDecimal("amount"));
            quoteAdjustment.set("overrideGlAccountId", headerAdjustment.getString("overrideGlAccountId"));
            quoteAdjustment.set("taxAuthGeoId", headerAdjustment.getString("taxAuthGeoId"));
            quoteAdjustment.set("taxAuthPartyId", headerAdjustment.getString("taxAuthPartyId"));
            quoteAdjustment.set("sourcePercentage", headerAdjustment.getBigDecimal("sourcePercentage"));

            delegator.create(quoteAdjustment);
        }
        return ServiceUtil.returnSuccess();
    }

    public static GenericValue getSupplierPostalAddressFromQuote(Delegator delegator, String quoteId, String partyId) throws GenericEntityException {

        GenericValue supplierShipGroup = EntityQuery.use(delegator).from("QuoteItemShipGroup")
                .where("quoteId", quoteId, "supplierPartyId", partyId, "vendorPartyId", partyId)
                .orderBy("-createdStamp")
                .queryFirst();

        if (UtilValidate.isNotEmpty(supplierShipGroup)) {
            return  EntityQuery.use(delegator).from("PostalAddress").where("contactMechId", supplierShipGroup.getString("contactMechId")).queryOne();
        } else {
            return getPartyRecentPostalAddressFromProfile(delegator, partyId);
        }
    }

    public static GenericValue getPartyRecentPostalAddressFromProfile(Delegator delegator, String partyId) throws GenericEntityException {

        GenericValue partyContactMechPurpose = EntityQuery.use(delegator).from("PartyContactMechPurpose")
                .where("partyId", partyId, "contactMechPurposeTypeId", "SHIPPING_LOCATION")
                .filterByDate()
                .orderBy("-createdStamp")
                .queryFirst();

        if (UtilValidate.isNotEmpty(partyContactMechPurpose)) {
            return EntityQuery.use(delegator).from("PostalAddress").where("contactMechId", partyContactMechPurpose.getString("contactMechId")).queryOne();
        }

        return null;
    }

    public static void addItemToTaxCtx(GenericValue quoteItem, Map<String, Object> calcTaxCtx) throws GenericEntityException {

        List<GenericValue> itemProductList = FastList.newInstance();
        if (calcTaxCtx.containsKey("itemProductList")) {
            itemProductList = (List<GenericValue>) calcTaxCtx.get("itemProductList");
        }

        List<BigDecimal> itemPriceList = FastList.newInstance();
        if (calcTaxCtx.containsKey("itemPriceList")) {
            itemPriceList = (List<BigDecimal>) calcTaxCtx.get("itemPriceList");
        }

        List<BigDecimal> itemAmountList = FastList.newInstance();
        if (calcTaxCtx.containsKey("itemAmountList")) {
            itemAmountList = (List<BigDecimal>) calcTaxCtx.get("itemAmountList");
        }

        List<BigDecimal> itemQuantityList = FastList.newInstance();
        if (calcTaxCtx.containsKey("itemQuantityList")) {
            itemQuantityList = (List<BigDecimal>) calcTaxCtx.get("itemQuantityList");
        }

        BigDecimal totalShippingAmount = BigDecimal.ZERO;
        if (calcTaxCtx.containsKey("orderShippingAmount")) {
            totalShippingAmount = (BigDecimal) calcTaxCtx.get("orderShippingAmount");
        }

        List<String> ref1List = FastList.newInstance();
        if (calcTaxCtx.containsKey("ref1List")) {
            ref1List = (List<String>) calcTaxCtx.get("ref1List");
        }


        String productId = quoteItem.getString("productId");
        GenericValue product = EntityQuery.use(quoteItem.getDelegator()).from("Product").where("productId", productId).queryOne();

        BigDecimal qty = BigDecimal.ZERO;
        if ("QuoteItemAndShipGroupAssoc".equals(quoteItem.getEntityName())) {
            qty = quoteItem.getBigDecimal("shipGroupQuantity");
        } else {
            qty = quoteItem.getBigDecimal("quantity");
        }
        BigDecimal unitPrice = quoteItem.getBigDecimal("quoteUnitPrice");
        BigDecimal quoteUnitShippingPrice = quoteItem.getBigDecimal("quoteUnitShippingPrice");
        totalShippingAmount = totalShippingAmount.add(qty.multiply(quoteUnitShippingPrice));
        String ref1 = quoteItem.getString("quoteId") + "|" +quoteItem.getString("quoteItemSeqId");
        if (quoteItem.containsKey("shipGroupSeqId")) {
            ref1 += "|"+quoteItem.getString("shipGroupSeqId");
        }

        itemProductList.add(product);
        itemPriceList.add(unitPrice);
        itemQuantityList.add(qty);
        itemAmountList.add(getQuoteItemSubTotal(quoteItem));
        ref1List.add(ref1);

        calcTaxCtx.put("itemProductList", itemProductList);
        calcTaxCtx.put("ref1List", ref1List);
        calcTaxCtx.put("itemPriceList", itemPriceList);
        calcTaxCtx.put("itemAmountList", itemAmountList);
        calcTaxCtx.put("itemQuantityList", itemQuantityList);
        calcTaxCtx.put("orderShippingAmount", totalShippingAmount);


    }

    public static BigDecimal getQuoteItemSubTotal(GenericValue quoteItem) throws GenericEntityException {
        BigDecimal unitPrice = quoteItem.getBigDecimal("quoteUnitPrice");
        BigDecimal quantity = quoteItem.getBigDecimal("quantity");
        BigDecimal amount = unitPrice.multiply(quantity);

        EntityCondition ec = EntityCondition.makeCondition(
                UtilMisc.toList(
                        EntityCondition.makeCondition("quoteId", EntityOperator.EQUALS, quoteItem.getString("quoteId")),
                        EntityCondition.makeCondition("quoteAdjustmentTypeId", EntityOperator.IN, UtilMisc.toList("TRADE_IN_DISCOUNT", "DISCOUNT_ADJUSTMENT")),
                        EntityCondition.makeCondition("quoteItemSeqId", EntityOperator.EQUALS, quoteItem.getString("quoteItemSeqId"))
                )
        );

        List<GenericValue> itemAdjustments = EntityQuery.use(quoteItem.getDelegator()).from("QuoteAdjustment")
                .where(ec).queryList();

        for (GenericValue itemAdjustment : itemAdjustments) {
            amount = amount.add(itemAdjustment.getBigDecimal("amount"));
        }

        return amount;
    }

}
