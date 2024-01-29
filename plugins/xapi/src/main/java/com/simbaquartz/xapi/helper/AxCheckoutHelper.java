package com.simbaquartz.xapi.helper;

import org.apache.tika.parser.dwg.DWGParser;
import org.apache.ofbiz.base.util.*;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.order.shoppingcart.ShoppingCart;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.service.LocalDispatcher;

import java.math.BigDecimal;
import java.util.*;

/**
 * Created by Admin on 10/26/17.
 */
public class AxCheckoutHelper {
    private static final String module = AxCheckoutHelper.class.getName();
    public static final String resource = "OrderUiLabels";
    public static final String resource_error = "OrderErrorUiLabels";

    protected LocalDispatcher dispatcher = null;
    protected Delegator delegator = null;
    protected ShoppingCart cart = null;

    public AxCheckoutHelper(LocalDispatcher dispatcher, Delegator delegator, ShoppingCart cart) {
        this.delegator = delegator;
        this.dispatcher = dispatcher;
        this.cart = cart;
    }
    public Map<String, Object> createOrder(GenericValue userLogin, String cartId, boolean areOrderItemsExploded, String visitId, String webSiteId) {
        if (this.cart == null) {
            return null;
        }
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        String orderId = this.cart.getOrderId();
        String originOrderId = (String) this.cart.getAttribute("originOrderId");
        String supplierPartyId = (String) this.cart.getAttribute("supplierPartyId");

        this.cart.clearAllItemStatus();

        BigDecimal grandTotal = this.cart.getGrandTotal();
        String orderType = this.cart.getOrderType();
        String currencyUom = this.cart.getCurrency();
        List<GenericValue> orderItems = this.cart.makeOrderItems(this.dispatcher);

        // store the order - build the context
        Map<String, Object> context = this.cart.makeCartMap(this.dispatcher, areOrderItemsExploded);
        //get the TrackingCodeOrder List
//        context.put("trackingCodeOrders", trackingCodeOrders);
//
//        if (distributorId != null) context.put("distributorId", distributorId);
//        if (affiliateId != null) context.put("affiliateId", affiliateId);

        context.put("cartId", cartId);
        context.put("orderId", orderId);
        context.put("supplierPartyId", supplierPartyId);
        context.put("grandTotal", grandTotal);
        context.put("userLogin", userLogin);
        context.put("visitId", visitId);
        if (UtilValidate.isEmpty(webSiteId)) {
            webSiteId = cart.getWebSiteId();
        }
        context.put("webSiteId", webSiteId);
        String partyId = cart.getBillToCustomerPartyId();
        context.put("partyId", partyId);
        context.put("orderTypeId", orderType);
        context.put("orderItems", orderItems);
        context.put("currencyUom", currencyUom);

        String productStoreId = cart.getProductStoreId();

        // store the order - invoke the service
        Map<String, Object> storeResult = null;

        try {
            storeResult = dispatcher.runSync("axStoreOrder", context);
            orderId = (String) storeResult.get("orderId");
            if (UtilValidate.isNotEmpty(orderId)) {
                this.cart.setOrderId(orderId);
                if (this.cart.getFirstAttemptOrderId() == null) {
                    this.cart.setFirstAttemptOrderId(orderId);
                }
            }
        } catch (GenericServiceException e) {
            String service = e.getMessage();
            Map<String, Object> messageMap = UtilMisc.<String, Object>toMap("service", service);
            String errMsg = UtilProperties.getMessage(resource_error, "checkhelper.could_not_create_order_invoking_service", messageMap, (cart != null ? cart.getLocale() : Locale.getDefault()));
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }

        // check for error message(s)
        if (ServiceUtil.isError(storeResult)) {
            String errMsg = UtilProperties.getMessage(resource_error, "checkhelper.did_not_complete_order_following_occurred", (cart != null ? cart.getLocale() : Locale.getDefault()));
            List<String> resErrorMessages = new LinkedList<String>();
            resErrorMessages.add(errMsg);
            resErrorMessages.add(ServiceUtil.getErrorMessage(storeResult));
            return ServiceUtil.returnError(resErrorMessages);
        }
        serviceResult.put("orderId", orderId);
        // ----------
        // If needed, the production runs are created and linked to the order lines.
        //
//        List<GenericValue> orderItems = UtilGenerics.checkList(context.get("orderItems"));
//        int counter = 0;
//        for (GenericValue orderItem : orderItems) {
//            String productId = orderItem.getString("productId");
//            if (productId != null) {
//                try {
//                    // do something tricky here: run as the "system" user
//                    // that can actually create and run a production run
//                    GenericValue permUserLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "system").cache().queryOne();
//                    GenericValue productStore = ProductStoreWorker.getProductStore(productStoreId, delegator);
//                    GenericValue product = EntityQuery.use(delegator).from("Product").where("productId", productId).queryOne();
//                    if (EntityTypeUtil.hasParentType(delegator, "ProductType", "productTypeId", product.getString("productTypeId"), "parentTypeId", "AGGREGATED")) {
//                        org.apache.ofbiz.product.config.ProductConfigWrapper config = this.cart.findCartItem(counter).getConfigWrapper();
//                        Map<String, Object> inputMap = new HashMap<String, Object>();
//                        inputMap.put("config", config);
//                        inputMap.put("facilityId", productStore.getString("inventoryFacilityId"));
//                        inputMap.put("orderItemSeqId", orderItem.getString("orderItemSeqId"));
//                        inputMap.put("orderId", orderId);
//                        inputMap.put("quantity", orderItem.getBigDecimal("quantity"));
//                        inputMap.put("userLogin", permUserLogin);
//
//                        Map<String, Object> prunResult = dispatcher.runSync("createProductionRunFromConfiguration", inputMap);
//                        if (ServiceUtil.isError(prunResult)) {
//                            Debug.logError(ServiceUtil.getErrorMessage(prunResult) + " for input:" + inputMap, module);
//                        }
//                    }
//                } catch (Exception e) {
//                    String service = e.getMessage();
//                    Map<String, String> messageMap = UtilMisc.toMap("service", service);
//                    String errMsg = UtilProperties.getMessage(resource_error, "checkhelper.could_not_create_order_invoking_service", messageMap, (cart != null ? cart.getLocale() : Locale.getDefault()));
//                    Debug.logError(e, errMsg, module);
//                    return ServiceUtil.returnError(errMsg);
//                }
//            }
//            counter++;
//        }
        // ----------

        // ----------
        // The status of the requirement associated to the shopping cart lines is set to "ordered".
        //
//        for (ShoppingCartItem shoppingCartItem : this.cart.items()) {
//            String requirementId = shoppingCartItem.getRequirementId();
//            if (requirementId != null) {
//                try {
//                     /* OrderRequirementCommitment records will map which POs which are created from which requirements. With the help of this mapping requirements will be updated to Ordered when POs will be approved.  */
//                    Map<String, Object> inputMap = UtilMisc.toMap("userLogin", userLogin, "orderId", orderId, "orderItemSeqId", shoppingCartItem.getOrderItemSeqId(), "requirementId", requirementId, "quantity", shoppingCartItem.getQuantity());
//                    dispatcher.runSync("createOrderRequirementCommitment", inputMap);
//                } catch (Exception e) {
//                    String service = e.getMessage();
//                    Map<String, String> messageMap = UtilMisc.toMap("service", service);
//                    String errMsg = UtilProperties.getMessage(resource_error, "checkhelper.could_not_create_order_invoking_service", messageMap, (cart != null ? cart.getLocale() : Locale.getDefault()));
//                    Debug.logError(e, errMsg, module);
//                    return ServiceUtil.returnError(errMsg);
//                }
//            }
//        }
//        // ----------
//
//        // set the orderId for use by chained events
//        Map<String, Object> result = ServiceUtil.returnSuccess();
//        result.put("orderId", orderId);
//        result.put("orderAdditionalEmails", this.cart.getOrderAdditionalEmails());
//
//        // save the emails to the order
//        List<GenericValue> toBeStored = new LinkedList<GenericValue>();
//
//        GenericValue party = null;
//        try {
//            party = EntityQuery.use(delegator).from("Party").where("partyId", partyId).queryOne();
//        } catch (GenericEntityException e) {
//            Debug.logWarning(e, UtilProperties.getMessage(resource_error,"OrderProblemsGettingPartyRecord", cart.getLocale()), module);
//        }
//
//        // create order contact mechs for the email address(s)
//        if (party != null) {
//            Iterator<GenericValue> emailIter = UtilMisc.toIterator(ContactHelper.getContactMechByType(party, "EMAIL_ADDRESS", false));
//            while (emailIter != null && emailIter.hasNext()) {
//                GenericValue email = emailIter.next();
//                GenericValue orderContactMech = this.delegator.makeValue("OrderContactMech",
//                        UtilMisc.toMap("orderId", orderId, "contactMechId", email.getString("contactMechId"), "contactMechPurposeTypeId", "ORDER_EMAIL"));
//                toBeStored.add(orderContactMech);
//                if (UtilValidate.isEmpty(ContactHelper.getContactMechByPurpose(party, "ORDER_EMAIL", false))) {
//                    GenericValue partyContactMechPurpose = this.delegator.makeValue("PartyContactMechPurpose",
//                            UtilMisc.toMap("partyId", party.getString("partyId"), "contactMechId", email.getString("contactMechId"), "contactMechPurposeTypeId", "ORDER_EMAIL", "fromDate", UtilDateTime.nowTimestamp()));
//                    toBeStored.add(partyContactMechPurpose);
//                }
//            }
//        }
//
//        // create dummy contact mechs and order contact mechs for the additional emails
//        String additionalEmails = this.cart.getOrderAdditionalEmails();
//        List<String> emailList = StringUtil.split(additionalEmails, ",");
//        if (emailList == null) emailList = new ArrayList<String>();
//        for (String email : emailList) {
//            String contactMechId = this.delegator.getNextSeqId("ContactMech");
//            GenericValue contactMech = this.delegator.makeValue("ContactMech",
//                    UtilMisc.toMap("contactMechId", contactMechId, "contactMechTypeId", "EMAIL_ADDRESS", "infoString", email));
//
//            GenericValue orderContactMech = this.delegator.makeValue("OrderContactMech",
//                    UtilMisc.toMap("orderId", orderId, "contactMechId", contactMechId, "contactMechPurposeTypeId", "ORDER_EMAIL"));
//            toBeStored.add(contactMech);
//            toBeStored.add(orderContactMech);
//        }
//
//        if (toBeStored.size() > 0) {
//            try {
//                if (Debug.verboseOn()) Debug.logVerbose("To Be Stored: " + toBeStored, module);
//                this.delegator.storeAll(toBeStored);
//            } catch (GenericEntityException e) {
//                // not a fatal error; so just print a message
//                Debug.logWarning(e, UtilProperties.getMessage(resource_error,"OrderProblemsStoringOrderEmailContactInformation", cart.getLocale()), module);
//            }
//        }

        return serviceResult;
    }
}
