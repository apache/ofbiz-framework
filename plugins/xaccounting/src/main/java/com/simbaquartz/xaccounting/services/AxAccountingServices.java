package com.simbaquartz.xaccounting.services;

//import com.simbaquartz.xorder.services.orderentity.builders.QuoteBuilder;
//import com.simbaquartz.xorder.services.util.FinanceUtil;
import com.simbaquartz.xparty.helpers.AxPartyHelper;
import java.io.File;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.fidelissd.zcp.xcommon.collections.FastList;
import com.fidelissd.zcp.xcommon.collections.FastMap;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.party.contact.ContactHelper;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

/** Created by simbaquartz on 23/3/17. */
public class AxAccountingServices {

  public static final String module = AxAccountingServices.class.getName();

  /* currently handling only PURCHASE_INVOICE */
  private static long getPartyTermNetDays(DispatchContext dctx, GenericValue invoice) {
    long retVal = 30; // 30 days by default

    try {
      String strDefaultTermDays =
          UtilProperties.getPropertyValue(
              "fsdAccounting.properties", "fsdaccounting.purchaseinvoice.defaulttermdays");
      if (UtilValidate.isNotEmpty(strDefaultTermDays)) {
        retVal = Long.parseLong(strDefaultTermDays);
      }
    } catch (Exception e) {
      Debug.logError(e, module);
    }

    Delegator delegator = dctx.getDelegator();
    String partyIdFrom = (String) invoice.get("partyIdFrom");
    String partyId = (String) invoice.get("partyId");
    String invoiceTypeId = (String) invoice.get("invoiceTypeId");
    try {
      if (UtilValidate.isNotEmpty(invoiceTypeId) && invoiceTypeId.equals("PURCHASE_INVOICE")) {
        List<GenericValue> agreements =
            EntityQuery.use(delegator)
                .from("Agreement")
                .where(
                    UtilMisc.toMap(
                        "partyIdFrom", partyId,
                        "partyIdTo", partyIdFrom))
                .filterByDate()
                .queryList();

        for (GenericValue agreement : agreements) {
          String agreementId = (String) agreement.get("agreementId");

          List<GenericValue> agreementTerms =
              EntityQuery.use(delegator)
                  .from("AgreementTerm")
                  .where(
                      UtilMisc.toMap("agreementId", agreementId, "termTypeId", "FIN_PAYMENT_TERM"))
                  .filterByDate()
                  .queryList();
          for (GenericValue agreementTerm : agreementTerms) {
            String invoiceItemTypeId = (String) agreementTerm.get("invoiceItemTypeId");
            if (UtilValidate.isNotEmpty(invoiceItemTypeId)
                && invoiceItemTypeId.equals("PINV_ADD_FEATURE")) {
              Long termDays = (Long) agreementTerm.get("termDays");
              if (UtilValidate.isNotEmpty(termDays)) {
                retVal = termDays;
                break; // ideally one term is enough
              }
            }
          }
        }
      }
    } catch (Exception e) {
      Debug.logError(e, module);
    }

    return retVal;
  }

  /* currently handling only PURCHASE_INVOICE */
  public static Map<String, Object> setInvoiceDueDate(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    LocalDispatcher dispatcher = dctx.getDispatcher();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String invoiceId = (String) context.get("invoiceId");

    try {
      GenericValue invoice =
          EntityQuery.use(delegator)
              .from("Invoice")
              .where(UtilMisc.toMap("invoiceId", invoiceId))
              .queryOne();

      if (UtilValidate.isEmpty(invoice)) {
        String errorMsg = "Invalid invoiceId";
        Debug.logError(errorMsg, module);
        return ServiceUtil.returnError(errorMsg);
      }

      Timestamp invoiceDate = (Timestamp) invoice.get("invoiceDate");
      if (UtilValidate.isEmpty(invoiceDate)) {
        // TODO: ideally this should be the same time as when a shipment is sent and be passed in as
        // a parameter
        invoiceDate = UtilDateTime.nowTimestamp();
      }

      Timestamp dueDate = (Timestamp) invoice.get("dueDate");

      if (UtilValidate.isEmpty(dueDate)) {
        long partyTermNetDays = getPartyTermNetDays(dctx, invoice);
        dueDate = UtilDateTime.getDayEnd(invoiceDate, partyTermNetDays);
        Map<String, Object> updateInvoiceContext = new HashMap<String, Object>();
        updateInvoiceContext.put("invoiceId", invoiceId);
        updateInvoiceContext.put("dueDate", dueDate);
        updateInvoiceContext.put("userLogin", userLogin);

        // update the invoice
        Map<String, Object> updateInvoiceResult =
            dispatcher.runSync("updateInvoice", updateInvoiceContext);
        if (ServiceUtil.isError(updateInvoiceResult)) {
          return updateInvoiceResult;
        }
      }
    } catch (Exception e) {
      Debug.logInfo(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }

    return serviceResult;
  }

  /**
   * Given a list of contact mechs, get address
   *
   * @param dctx
   * @param contactMechs
   * @return map containing address
   * @throws GenericEntityException
   */
  private static Map<String, Object> getAddress(
      DispatchContext dctx, List<GenericValue> contactMechs) throws GenericEntityException {
    Delegator delegator = dctx.getDelegator();

    Map<String, Object> addressMap = FastMap.newInstance();

    // Note: outerContactMech refers to a CatactMech using contactMechId.
    // It is a GenericEntity such as a PartyContactMech or an InvoiceCpntactMech.
    for (GenericValue outerContactMech : contactMechs) {
      String contactMechId = (String) outerContactMech.get("contactMechId");
      GenericValue contactMech =
          EntityQuery.use(delegator)
              .from("ContactMech")
              .where("contactMechId", contactMechId)
              .queryOne();
      if (UtilValidate.isNotEmpty(contactMech)) {
        GenericValue postalAddress =
            EntityQuery.use(delegator)
                .from("PostalAddress")
                .where("contactMechId", contactMechId)
                .queryOne();
        if (UtilValidate.isNotEmpty(postalAddress)) {
          addressMap.put("toName", postalAddress.get("toName"));
          addressMap.put("attnName", postalAddress.get("attnName"));
          addressMap.put("address1", postalAddress.get("address1"));
          addressMap.put("address2", postalAddress.get("address2"));
          addressMap.put("directions", postalAddress.get("directions"));
          addressMap.put("city", postalAddress.get("city"));
          addressMap.put("postalCode", postalAddress.get("postalCode"));
          addressMap.put("countryGeoId", postalAddress.get("countryGeoId"));
          addressMap.put("stateProvinceGeoId", postalAddress.get("stateProvinceGeoId"));
          break; // one address is sufficient
        }
      }
    }

    return addressMap;
  }

  /**
   * Get the details of the given party as a map
   *
   * @param dctx
   * @param partyId
   * @return map
   * @throws GenericEntityException
   */
  private static Map<String, Object> getPartyDetails(DispatchContext dctx, String partyId)
      throws GenericEntityException {
    Delegator delegator = dctx.getDelegator();

    Map<String, Object> addressMap = FastMap.newInstance();
    Map<String, Object> telecomMap = FastMap.newInstance();
    Map<String, Object> retMap = FastMap.newInstance();

    String name = AxPartyHelper.getPartyName(delegator, partyId);
    String email = null;

    GenericValue party =
        EntityQuery.use(delegator)
            .from("Party")
            .where(UtilMisc.toMap("partyId", partyId))
            .queryOne();
    if (UtilValidate.isNotEmpty(party)) {
      GenericValue emailRec =
          EntityUtil.getFirst(ContactHelper.getContactMechByType(party, "EMAIL_ADDRESS", false));
      if (UtilValidate.isNotEmpty(emailRec)) {
        email = (String) emailRec.get("infoString");
      }

      // get telephone number
      GenericValue telRec =
          EntityUtil.getFirst(ContactHelper.getContactMechByType(party, "TELECOM_NUMBER", false));
      if (UtilValidate.isNotEmpty(telRec)) {
        String contactMechId = (String) telRec.get("contactMechId");
        GenericValue telecomNumber =
            EntityQuery.use(delegator)
                .from("TelecomNumber")
                .where("contactMechId", contactMechId)
                .queryOne();
        if (UtilValidate.isNotEmpty(telecomNumber)) {
          String countryCode = (String) telecomNumber.get("countryCode");
          String areaCode = (String) telecomNumber.get("areaCode");
          String contactNumber = (String) telecomNumber.get("contactNumber");
          String askForName = (String) telecomNumber.get("askForName");

          telecomMap.put("countryCode", countryCode);
          telecomMap.put("areaCode", areaCode);
          telecomMap.put("contactNumber", contactNumber);
          telecomMap.put("askForName", askForName);
        }
      }

      List<GenericValue> partyContactMechs =
          EntityQuery.use(delegator)
              .from("PartyContactMech")
              .where(UtilMisc.toMap("partyId", partyId))
              .filterByDate()
              .queryList();
      addressMap = getAddress(dctx, partyContactMechs);
    }

    retMap.put("partyId", partyId);
    retMap.put("name", name);
    retMap.put("email", email);
    retMap.put("addressMap", addressMap);
    retMap.put("telecomMap", telecomMap);

    return retMap;
  }

  /**
   * Get Role Details for the given roleTypeId
   *
   * @param dctx
   * @param quoteId
   * @param roleTypeId
   * @return list of maps containing details
   * @throws GenericEntityException
   */
  private static List<Map<String, Object>> getRoleDetails(
      DispatchContext dctx, String quoteId, String roleTypeId) throws GenericEntityException {
    List<Map<String, Object>> roleMaps = FastList.newInstance();
    Delegator delegator = dctx.getDelegator();

    List<GenericValue> roles =
        EntityUtil.filterByDate(
            EntityQuery.use(delegator)
                .from("QuoteRole")
                .where(UtilMisc.toMap("quoteId", quoteId, "roleTypeId", roleTypeId))
                .queryList());
    for (GenericValue role : roles) {
      Map partyDetails = getPartyDetails(dctx, (String) role.get("partyId"));
      String displayName = role.getString("displayName");
      if (UtilValidate.isEmpty(displayName)) {
        // get the display name from role name
        GenericValue roleType = role.getRelatedOne("RoleType", true);
        displayName = roleType.getString("description");
      }

      partyDetails.put("quoteRoleDisplayName", displayName);
      roleMaps.add(partyDetails);
    }

    return roleMaps;
  }

  /**
   * Get an attribute of a quote.
   *
   * @param dctx
   * @param quoteId
   * @param attrName
   * @return
   * @throws GenericEntityException
   */
  private static String getQuoteAttribute(DispatchContext dctx, String quoteId, String attrName)
      throws GenericEntityException {
    Delegator delegator = dctx.getDelegator();
    GenericValue quoteAttribute =
        EntityQuery.use(delegator)
            .from("QuoteAttribute")
            .where(UtilMisc.toMap("quoteId", quoteId, "attrName", attrName))
            .queryOne();
    if (UtilValidate.isEmpty(quoteAttribute)) {
      return "";
    } else {
      return (String) quoteAttribute.get("attrValue");
    }
  }

  /**
   * Get attributes of a quote based on the quoteId.
   *
   * @param dctx
   * @param context
   * @return
   */
  public static Map<String, Object> fetchQuoteAttributes(
      DispatchContext dctx, Map<String, Object> context) {
    Delegator delegator = dctx.getDelegator();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String quoteId = (String) context.get("quoteId");
    List<Map> quoteAttributeList = FastList.newInstance();
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    try {
      List<GenericValue> getQuoteAttributes =
          EntityQuery.use(delegator)
              .from("QuoteAttribute")
              .where(UtilMisc.toMap("quoteId", quoteId))
              .queryList();
      if (UtilValidate.isNotEmpty(getQuoteAttributes)) {
        for (GenericValue getQuoteAttribute : getQuoteAttributes) {
          quoteAttributeList.add(getQuoteAttribute);
        }
        serviceResult.put("quoteAttributeList", quoteAttributeList);
      }
    } catch (GenericEntityException e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }
    return serviceResult;
  }

  /**
   * Get the finance report for yhe given supplierPartyId (in context) This is a service.
   *
   * @param dctx
   * @param context
   * @return
   */
  public static Map<String, Object> getFinanceReport(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    LocalDispatcher dispatcher = dctx.getDispatcher();

    GenericValue userLogin = (GenericValue) context.get("userLogin");

    List reports = FastList.newInstance();

    try {
      // get all the AWARDED quotes matching the search conditio
      Map<String, Object> searchQuotesCtx = FastMap.newInstance();
      searchQuotesCtx.put("userLogin", userLogin);
      searchQuotesCtx.put("startIndex", context.get("startIndex"));
      searchQuotesCtx.put("viewSize", context.get("viewSize"));
      searchQuotesCtx.put("dateRangeFrom", context.get("dateRangeFrom"));
      searchQuotesCtx.put("dateRangeTo", context.get("dateRangeTo"));
      searchQuotesCtx.put("customerPartyId", context.get("customerPartyId"));
      searchQuotesCtx.put("solicitationNumber", context.get("solicitationNumber"));
      searchQuotesCtx.put("quotePurchaseOrderNumber", context.get("quotePurchaseOrderNumber"));
      searchQuotesCtx.put(
          "quoteRequisitionPurchaseRequestNumber",
          context.get("quoteRequisitionPurchaseRequestNumber"));
      searchQuotesCtx.put("contractStatedAwardDate", context.get("contractStatedAwardDate"));
      searchQuotesCtx.put("quoteOrderNumber", context.get("quoteOrderNumber"));
      searchQuotesCtx.put(
          "quoteContractReferenceNumber", context.get("quoteContractReferenceNumber"));
      searchQuotesCtx.put("quoteTypeIds", context.get("quoteTypeIds"));
      searchQuotesCtx.put("quoteId", context.get("quoteId"));
      searchQuotesCtx.put("partyId", context.get("partyId"));
      searchQuotesCtx.put("supplierPartyId", context.get("supplierPartyId"));
      searchQuotesCtx.put("supplierContactPartyId", context.get("supplierContactPartyId"));
      searchQuotesCtx.put("statusIds", context.get("statusIds"));
      searchQuotesCtx.put("productId", context.get("productId"));
      searchQuotesCtx.put("sortBy", context.get("sortBy"));
      searchQuotesCtx.put("awardStatusIds", context.get("awardStatusIds"));
      searchQuotesCtx.put("billingStatusId", context.get("billingStatusId"));
      searchQuotesCtx.put("quoteInvoiceTypeId", context.get("quoteInvoiceTypeId"));
      searchQuotesCtx.put("quoteInvoiceStatusId", context.get("quoteInvoiceStatusId"));
      searchQuotesCtx.put("shipmentStatusId", context.get("shipmentStatusId"));
      searchQuotesCtx.put("fsdToCMInvoiceStatusId", context.get("fsdToCMInvoiceStatusId"));
      searchQuotesCtx.put("quoteOrderedDateRangeFrom", context.get("quoteOrderedDateRangeFrom"));
      searchQuotesCtx.put("quoteOrderedDateRangeTo", context.get("quoteOrderedDateRangeTo"));
      searchQuotesCtx.put("endOfWarrantyServiceFrom", context.get("endOfWarrantyServiceFrom"));
      searchQuotesCtx.put("endOfWarrantyServiceTo", context.get("endOfWarrantyServiceTo"));
      searchQuotesCtx.put("onlyWarrantyService", context.get("onlyWarrantyService"));
      searchQuotesCtx.put("thankYouCardSent", context.get("thankYouCardSent"));
      searchQuotesCtx.put("awardServiceTracked", context.get("awardServiceTracked"));
      searchQuotesCtx.put("shippingState", context.get("shippingState"));
      searchQuotesCtx.put("shippingCity", context.get("shippingCity"));
      searchQuotesCtx.put("tagIds", context.get("tagIds"));
      searchQuotesCtx.put("quoteInvoiceId", context.get("quoteInvoiceId"));
      searchQuotesCtx.put("quotePaymentStatusId", context.get("quotePaymentStatusId"));
      searchQuotesCtx.put("quoteFlagStatus", context.get("quoteFlagStatus"));

      Map<String, Object> searchQuotesResult = dispatcher.runSync("searchQuotes", searchQuotesCtx);

      if (!ServiceUtil.isSuccess(searchQuotesResult)) {
        return searchQuotesResult;
      }

      List<Map<String, Object>> quoteEntries =
          (List<Map<String, Object>>) searchQuotesResult.get("searchResults");
      int recordsSize = (int) searchQuotesResult.get("totalResultSize");
      // amount totals
      BigDecimal quotePriceTotal = (BigDecimal) searchQuotesResult.get("quotePriceTotal");
      BigDecimal quoteFsdCostTotal = (BigDecimal) searchQuotesResult.get("quoteFsdCostTotal");
      BigDecimal quoteProfitTotal = (BigDecimal) searchQuotesResult.get("quoteProfitTotal");
      String quoteAverageProfitMarginPercentage =
          (String) searchQuotesResult.get("quoteAverageProfitMarginPercentage");

      for (Map<String, Object> quoteEntry : quoteEntries) {

        Map<String, Object> report = FastMap.newInstance();
        String quoteId = (String) quoteEntry.get("quoteId");

        List<String> quotePurchaseInvoicesIds = (List) quoteEntry.get("quotePurchaseInvoiceIds");
        // fetch purchase Order Ids
        if (UtilValidate.isNotEmpty(quoteEntry.get("purchaseOrderIdsForQuote"))) {
          List<String> purchaseOrderIdsForQuote = (List) quoteEntry.get("purchaseOrderIdsForQuote");
          report.put("purchaseOrderIdsForQuote", purchaseOrderIdsForQuote);
        }

        BigDecimal totalPurchaseInvoiceOutstanding = BigDecimal.ZERO;
        BigDecimal totalPurchaseInvoiceAmount = BigDecimal.ZERO;

        if (UtilValidate.isNotEmpty(quotePurchaseInvoicesIds)) {
          for (Object quotePurchaseInvoiceId : quotePurchaseInvoicesIds) {
            Map purchaseInvoiceDetails =
                AxAccountingHelper.splitMergedRecords((String) quotePurchaseInvoiceId);
            String purchaseInvoiceOutstandingString =
                (String) purchaseInvoiceDetails.get("purchaseInvoiceOutstanding");
            String purchaseInvoiceAmountString =
                (String) purchaseInvoiceDetails.get("purchaseInvoiceAmount");
            BigDecimal purchaseInvoiceOutstanding =
                new BigDecimal(purchaseInvoiceOutstandingString);
            totalPurchaseInvoiceOutstanding =
                totalPurchaseInvoiceOutstanding.add(purchaseInvoiceOutstanding);

            BigDecimal purchaseInvoiceAmount = new BigDecimal(purchaseInvoiceAmountString);
            totalPurchaseInvoiceAmount = totalPurchaseInvoiceAmount.add(purchaseInvoiceAmount);
          }
        }

        GenericValue quote =
            EntityQuery.use(delegator)
                .from("Quote")
                .where(UtilMisc.toMap("quoteId", quoteId))
                .queryOne();

        String quoteCurrencyUomId = (String) quoteEntry.get("currencyUomId");
        String partyId = (String) quoteEntry.get("partyId");
        String salesOrderId = null;
        String salesInvoiceId = null;
        GenericValue salesInvoice = null;
        String invoiceStatus = null;
        BigDecimal salesInvoiceAmount = null;
        BigDecimal purchaseInvoiceAmount = null;

        Timestamp awardDate = null;
        Date quoteOrderedDate = (Date) quoteEntry.get("quoteOrderedDate");
        if (UtilValidate.isNotEmpty(quoteOrderedDate)) {
          awardDate = UtilDateTime.toTimestamp(quoteOrderedDate);
        }

        Timestamp endOfWarrantyService = (Timestamp) quoteEntry.get("endOfWarrantyService");
        String awardStatus = (String) quoteEntry.get("awardStatus");
        String quoteAwardProcessingStatus = (String) quoteEntry.get("quoteAwardProcessingStatus");

        List<Map<String, Object>> quoteShippingDestinations =
            (List) quoteEntry.get("quoteShippingDestinations");
        List<Map<String, Object>> quotePurchaseInvoiceIds =
            (List) quoteEntry.get("quotePurchaseInvoiceIds");
        List<Map<String, Object>> quoteTags = (List) quoteEntry.get("quoteTags");

        String awardServiceTracked = (String) quoteEntry.get("quoteAwardServiceTracked");
        List<Map<String, String>> quoteProducts =
            (List<Map<String, String>>) quoteEntry.get("quoteProducts");
        Timestamp paymentDate = null;
        String invoicePerShipment = null;
        String purchasingEntity = null;
        Timestamp receivedDate = null;
        GenericValue purchaseOrder = null;
        String purchaseOrderId = null;
        GenericValue purchaseInvoice = null;
        String purchaseInvoiceId = null;
        Timestamp shipOrInstallDate = null;
        Timestamp invoiceDate = null;
        String notes = null;
        String purchaseOrderNumber = "";
        String requisitionPurchaseRequestNumber = "";
        String contractStatedAwardDate = "";
        String orderNumber = "";
        String contractReferenceNumber = "";
        String cmSalesInvoiceId = "";
        String govSalesInvoiceDate = "";
        String cmPurchaseInvoiceId = "";
        String govPurchaseInvoiceDate = "";

        Map<String, Object> partyDetails = null;
        List<Map<String, Object>> customerDetails = null;
        List<Map<String, Object>> supplierDetails = null;
        List<Map<String, Object>> supplierPOCDetails = null;
        List<Map<String, Object>> coDetails = null;
        List<Map<String, Object>> reqTakerDetails = null;
        List<Map<String, Object>> orderClerkDetails = null;
        List<Map<String, Object>> mainSalesRepDetails = null;
        List<Map<String, Object>> salesRepDetails = null;
        List<Map<String, Object>> billToDetails = null;
        List<Map<String, Object>> shipToDetails = null;
        Set<String> linkedPurchaseOrderIds = new HashSet<String>();

        // PARTY
        partyDetails = getPartyDetails(dctx, partyId);

        // Request Taker
        reqTakerDetails = getRoleDetails(dctx, quoteId, "REQ_TAKER");

        // Order Clerk
        orderClerkDetails = getRoleDetails(dctx, quoteId, "ORDER_CLERK");

        // CUSTOMER
        customerDetails = getRoleDetails(dctx, quoteId, "CUSTOMER");

        // SUPPLIER
        supplierDetails = getRoleDetails(dctx, quoteId, "SUPPLIER");

        // SUPPLIER_POC
        supplierPOCDetails = getRoleDetails(dctx, quoteId, "SUPPLIER_POC");

        // CONTRACTING_OFFICER
        coDetails = getRoleDetails(dctx, quoteId, "CONTRACTING_OFFICER");

        // MAIN_SALES_REP
        mainSalesRepDetails = getRoleDetails(dctx, quoteId, "MAIN_SALES_REP");

        // SALES_REP
        salesRepDetails = getRoleDetails(dctx, quoteId, "SALES_REP");

        // BILL_TO_CUSTOMER
        billToDetails = getRoleDetails(dctx, quoteId, "BILL_TO_CUSTOMER");

        // SHIP_TO_CUSTOMER
        shipToDetails = getRoleDetails(dctx, quoteId, "SHIP_TO_CUSTOMER");

        List<GenericValue> orderItems =
            EntityQuery.use(delegator)
                .from("OrderItem")
                .where(UtilMisc.toMap("quoteId", quoteId))
                .queryList();
        for (GenericValue orderItem : orderItems) {
          salesOrderId = (String) orderItem.get("orderId");
          break; // looking for 1 order
        }
        if (UtilValidate.isNotEmpty(salesOrderId)) {
          GenericValue salesOrder =
              EntityQuery.use(delegator)
                  .from("OrderHeader")
                  .where(UtilMisc.toMap("orderId", salesOrderId))
                  .queryOne();
          if (UtilValidate.isEmpty(salesOrder)) {
            Debug.logWarning("Sales order for id " + salesOrderId + " not found.", module);
            continue;
          }
          invoicePerShipment = (String) salesOrder.get("invoicePerShipment");

          // get award related order attributes
          List<GenericValue> salesOrderAttributes =
              salesOrder.getRelated("OrderAttribute", null, null, false);
          if (UtilValidate.isNotEmpty(salesOrderAttributes)) {
            for (GenericValue salesOrderAttribute : salesOrderAttributes) {
              String attrName = salesOrderAttribute.getString("attrName");

              switch (attrName) {
                case "orderNumber":
                  orderNumber = salesOrderAttribute.getString("attrValue");
                  break;
                case "purchaseOrderNumber":
                  purchaseOrderNumber = salesOrderAttribute.getString("attrValue");
                  break;
                case "requisitionPurchaseRequestNumber":
                  requisitionPurchaseRequestNumber = salesOrderAttribute.getString("attrValue");
                  break;
                case "contractStatedAwardDate":
                  contractStatedAwardDate = salesOrderAttribute.getString("attrValue");
                  break;
                case "contractReferenceNumber":
                  contractReferenceNumber = salesOrderAttribute.getString("attrValue");
                  break;
                default:
                  break;
              }
            }
          }

          // get the sales invoice
          List<GenericValue> salesOrderItemBillings =
              salesOrder.getRelated("OrderItemBilling", null, null, false);
          if (salesOrderItemBillings.size() > 0) {
            salesInvoiceId = (String) salesOrderItemBillings.get(0).get("invoiceId");
            if (UtilValidate.isNotEmpty(salesInvoiceId)) {
              salesInvoice =
                  EntityQuery.use(delegator)
                      .from("Invoice")
                      .where("invoiceId", salesInvoiceId)
                      .queryOne();
              if (UtilValidate.isNotEmpty(salesInvoice)) {
                salesInvoiceAmount = AxInvoiceWorker.getInvoiceTotal(salesInvoice);
                paymentDate = (Timestamp) salesInvoice.get("paidDate");
                purchasingEntity = (String) salesInvoice.get("partyId");
                receivedDate = (Timestamp) salesInvoice.get("dueDate");
                invoiceStatus = (String) salesInvoice.get("statusId");
              } else {
                Debug.logError("Invoice not found for sales invoice " + salesInvoiceId, module);
              }

              // get invoice attributes
              List<GenericValue> salesInvoiceAttributes =
                  EntityQuery.use(delegator)
                      .from("InvoiceAttribute")
                      .where("invoiceId", salesInvoiceId)
                      .queryList();
              if (UtilValidate.isNotEmpty(salesInvoiceAttributes)) {
                for (GenericValue salesInvoiceAttribute : salesInvoiceAttributes) {
                  String attrName = salesInvoiceAttribute.getString("attrName");
                  if (attrName.equals("cmInvoiceId")) {
                    cmSalesInvoiceId = salesInvoiceAttribute.getString("attrValue");
                  }
                }
              }

              // salesGovInvoicedDate
              Timestamp salesGovInvoicedDateTs = null;
              List<GenericValue> invoiceBillings =
                  EntityQuery.use(delegator)
                      .from("InvoiceBilling")
                      .where(UtilMisc.toMap("invoiceId", salesInvoiceId))
                      .queryList();
              for (GenericValue invoiceBilling : invoiceBillings) {
                Timestamp invoiceBillingDateTs = (Timestamp) invoiceBilling.get("invoicedDate");
                if (UtilValidate.isEmpty(salesGovInvoicedDateTs)) {
                  salesGovInvoicedDateTs = invoiceBillingDateTs;
                } else if (UtilValidate.isNotEmpty(invoiceBillingDateTs)
                    && salesGovInvoicedDateTs.before(invoiceBillingDateTs)) {
                  salesGovInvoicedDateTs = invoiceBillingDateTs;
                }
              }
              if (UtilValidate.isNotEmpty(salesGovInvoicedDateTs)) {
                govSalesInvoiceDate =
                    UtilDateTime.timeStampToString(
                        salesGovInvoicedDateTs,
                        "MM/dd/yyyy",
                        TimeZone.getDefault(),
                        Locale.getDefault());
              }
            }
          }

          // get the corresponding purchase order
          List<GenericValue> orderItemAssocs =
              EntityQuery.use(delegator)
                  .from("OrderItemAssoc")
                  .where("orderId", salesOrderId)
                  .queryList();
          for (GenericValue orderItemAssoc : orderItemAssocs) {
            purchaseOrderId = (String) orderItemAssoc.get("toOrderId");
            if (UtilValidate.isNotEmpty(purchaseOrderId)) {
              // get related purchase invoice
              List<GenericValue> purchaseOrderItemBillings =
                  EntityQuery.use(delegator)
                      .from("OrderItemBilling")
                      .where(UtilMisc.toMap("orderId", purchaseOrderId))
                      .queryList();

              if (purchaseOrderItemBillings.size() > 0) {
                purchaseInvoiceId = (String) purchaseOrderItemBillings.get(0).get("invoiceId");
                purchaseInvoice =
                    EntityQuery.use(delegator)
                        .from("Invoice")
                        .where(UtilMisc.toMap("invoiceId", purchaseInvoiceId))
                        .queryOne();
                if (UtilValidate.isNotEmpty(purchaseInvoice)) {
                  purchaseInvoiceAmount = AxInvoiceWorker.getInvoiceTotal(purchaseInvoice);
                  shipOrInstallDate = (Timestamp) purchaseInvoice.get("dueDate");
                  invoiceDate = (Timestamp) purchaseInvoice.get("invoiceDate");
                }
                break;
              }

              // get invoice attributes
              List<GenericValue> purchaseInvoiceAttributes =
                  EntityQuery.use(delegator)
                      .from("InvoiceAttribute")
                      .where("invoiceId", purchaseInvoiceId)
                      .queryList();
              if (UtilValidate.isNotEmpty(purchaseInvoiceAttributes)) {
                for (GenericValue purchaseInvoiceAttribute : purchaseInvoiceAttributes) {
                  String attrName = purchaseInvoiceAttribute.getString("attrName");
                  if (attrName.equals("cmInvoiceId")) {
                    cmPurchaseInvoiceId = purchaseInvoiceAttribute.getString("attrValue");
                  }
                  if (attrName.equals("invoicedDate")) {
                    govPurchaseInvoiceDate = purchaseInvoiceAttribute.getString("attrValue");
                  }
                }
              }
            }
            linkedPurchaseOrderIds.add(purchaseOrderId);
          }
        }

        // get shipFrom location using sales invoice
        Map<String, Object> shipFromAddressMap = null;

        if (UtilValidate.isNotEmpty(salesInvoiceId)) {
          List<GenericValue> salesInvoiceContactMechs =
              EntityQuery.use(delegator)
                  .from("InvoiceContactMech")
                  .where(UtilMisc.toMap("invoiceId", salesInvoiceId))
                  .queryList();
          // if no address, give warning
          if (salesInvoiceContactMechs.size() == 0) {
            Debug.logWarning("No address for sales invoice " + salesInvoiceId, module);
          } else {
            shipFromAddressMap = getAddress(dctx, salesInvoiceContactMechs);
          }
        }

        String quoteProductIds = "";
        List<GenericValue> quoteItem =
            EntityQuery.use(delegator).from("QuoteItem").where("quoteId", quoteId).queryList();
        Set productCategoryList = new HashSet<>();
        if (UtilValidate.isNotEmpty(quoteItem)) {
          for (GenericValue items : quoteItem) {
            quoteProductIds = items.getString("productId");
            List<GenericValue> productCategoryMember =
                EntityQuery.use(delegator)
                    .from("ProductCategoryMember")
                    .where("productId", quoteProductIds)
                    .queryList();
            if (UtilValidate.isNotEmpty(productCategoryMember)) {
              for (GenericValue productCate : productCategoryMember) {
                List<GenericValue> productCategory =
                    EntityQuery.use(delegator)
                        .from("ProductCategory")
                        .where("productCategoryId", productCate.getString("productCategoryId"))
                        .queryList();
                if (UtilValidate.isNotEmpty(productCategory)) {
                  for (GenericValue category : productCategory) {
                    Map product = FastMap.newInstance();
                    product.put("categoryName", category.getString("categoryName"));
                    productCategoryList.add(product);
                  }
                }
              }
            } else {
              Map product = FastMap.newInstance();
              String categoryName =
                  UtilProperties.getPropertyValue("SellercentralConfig", "productCategoryName");
              product.put("categoryName", categoryName);
              productCategoryList.add(product);
            }
          }
          report.put("productCategoryList", productCategoryList);
        }

        report.put("quoteProducts", quoteProducts);
        report.put("quote", quote);
        report.put("quoteId", quoteId);
        report.put("quoteCurrencyUomId", quoteCurrencyUomId);
        report.put("orderNumber", orderNumber);
        report.put("purchaseOrderNumber", purchaseOrderNumber);
        report.put("requisitionPurchaseRequestNumber", requisitionPurchaseRequestNumber);
        report.put("contractStatedAwardDate", contractStatedAwardDate);
        report.put("contractReferenceNumber", contractReferenceNumber);
        report.put("partyId", partyId);
        report.put("partyDetails", partyDetails);
        report.put("purchasingEntity", purchasingEntity);
        report.put("awardDate", awardDate);
        report.put("endOfWarrantyService", endOfWarrantyService);
        report.put("awardStatus", awardStatus);
        report.put("quoteAwardProcessingStatus", quoteAwardProcessingStatus);
        report.put("customerDetails", customerDetails);
        report.put("supplierDetails", supplierDetails);
        report.put("supplierPOCDetails", supplierPOCDetails);
        report.put("coDetails", coDetails);
        report.put("reqTakerDetails", reqTakerDetails);
        report.put("orderClerkDetails", orderClerkDetails);
        report.put("mainSalesRepDetails", mainSalesRepDetails);
        report.put("salesRepDetails", salesRepDetails);
        report.put("salesOrderId", salesOrderId);
        report.put("salesInvoiceId", salesInvoiceId);
        report.put("quoteTags", quoteTags);
        report.put("purchaseOrderId", purchaseOrderId);
        report.put("pruchaseOrderIds", linkedPurchaseOrderIds);
        report.put("purchaseInvoiceId", purchaseInvoiceId);
        report.put("invoicePerShipment", invoicePerShipment);
        report.put("paymentDate", paymentDate);
        report.put("salesInvoiceAmount", salesInvoiceAmount);
        report.put("purchaseInvoiceAmount", totalPurchaseInvoiceAmount);
        report.put("totalPurchaseInvoiceOutstanding", totalPurchaseInvoiceOutstanding);
        report.put("invoiceStatus", invoiceStatus);
        report.put("receivedDate", receivedDate);
        report.put("shipOrInstallDate", shipOrInstallDate);
        report.put("invoiceDate", invoiceDate);
        report.put("quoteShippingDestinations", quoteShippingDestinations);
        report.put("quotePurchaseInvoiceIds", quotePurchaseInvoiceIds);
        report.put("awardServiceTracked", awardServiceTracked);

        // Billing Address
        report.put("billToDetails", billToDetails);
        // Shiping Address
        report.put("shipToDetails", shipToDetails);

        // shipFrom Address
        report.put("shipFromAddress", shipFromAddressMap);

        // installation details
        // install date
        String installDateStr = getQuoteAttribute(dctx, quoteId, "installDate");
        report.put("installDate", installDateStr);

        report.put("installPOCName", getQuoteAttribute(dctx, quoteId, "installPOCName"));
        report.put("installPOCEmail", getQuoteAttribute(dctx, quoteId, "installPOCEmail"));
        report.put("installPOCPhone", getQuoteAttribute(dctx, quoteId, "installPOCPhone"));
        report.put("installNotes", getQuoteAttribute(dctx, quoteId, "installNotes"));

        // manufacturer sales rep details
        report.put(
            "manufacturerSalesRepName",
            getQuoteAttribute(dctx, quoteId, "manufacturerSalesRepName"));
        report.put(
            "manufacturerSalesRepThankYouCardwWineName",
            getQuoteAttribute(dctx, quoteId, "manufacturerSalesRepThankYouCardwWineName"));
        report.put(
            "manufacturerSalesRepMailingAddress",
            getQuoteAttribute(dctx, quoteId, "manufacturerSalesRepMailingAddress"));
        report.put(
            "manufacturerSalesRepNotes",
            getQuoteAttribute(dctx, quoteId, "manufacturerSalesRepNotes"));

        // billing center group details
        report.put(
            "governmentInvoiceBillingLocation",
            getQuoteAttribute(dctx, quoteId, "BILLING_LOCATION_NAME"));
        report.put(
            "governmentInvoiceLink", getQuoteAttribute(dctx, quoteId, "BILLING_LOCATION_URL"));

        // billing invoice details
        // invoice billing invoice date
        String invoiceBillingInvoiceDateStr =
            getQuoteAttribute(dctx, quoteId, "invoiceBillingInvoiceDate");
        report.put("invoiceBillingInvoiceDate", invoiceBillingInvoiceDateStr);
        report.put(
            "invoiceBillingLocation", getQuoteAttribute(dctx, quoteId, "invoiceBillingLocation"));
        report.put("invoiceBillAccepted", getQuoteAttribute(dctx, quoteId, "invoiceBillAccepted"));
        report.put("invoiceBillingNotes", getQuoteAttribute(dctx, quoteId, "invoiceBillingNotes"));

        // multiple invoice required details
        report.put(
            "multipleInvoiceRequired", getQuoteAttribute(dctx, quoteId, "multipleInvoiceRequired"));
        report.put(
            "multipleInvoiceRequiredNotes",
            getQuoteAttribute(dctx, quoteId, "multipleInvoiceRequiredNotes"));

        // received date details
        String receivedDateStr = getQuoteAttribute(dctx, quoteId, "receivedDate");
        report.put(
            "receivedDate2",
            receivedDateStr); // using a different variable receivedDate2 (conflicts elsewhere)
        report.put("receivedDateNotes", getQuoteAttribute(dctx, quoteId, "receivedDateNotes"));

        // payment  date details
        String paymentDateStr = getQuoteAttribute(dctx, quoteId, "paymentDate");
        report.put("paymentDate2", paymentDateStr);
        report.put("paymentDateNotes", getQuoteAttribute(dctx, quoteId, "paymentDateNotes"));

        // loan details
        report.put("lender", getQuoteAttribute(dctx, quoteId, "lender"));

        // issue date
        String loanIssueDateStr = getQuoteAttribute(dctx, quoteId, "loanIssueDate");
        report.put("loanIssueDate", loanIssueDateStr);
        report.put("loanOutstanding", getQuoteAttribute(dctx, quoteId, "loanOutstanding"));

        // closed date
        String loanClosedDateStr = getQuoteAttribute(dctx, quoteId, "loanClosedDate");
        report.put("loanClosedDate", loanClosedDateStr);
        report.put("loanNotes", getQuoteAttribute(dctx, quoteId, "loanNotes"));

        report.put("notes", notes);

        report.put("cmSalesInvoiceId", cmSalesInvoiceId);
        report.put("cmPurchaseInvoiceId", cmPurchaseInvoiceId);
        report.put("govSalesInvoiceDate", govSalesInvoiceDate);
        report.put("govPurchaseInvoiceDate", govPurchaseInvoiceDate);

        reports.add(report);
      }
      serviceResult.put("reports", reports);
      serviceResult.put("totalResultSize", recordsSize);

      serviceResult.put("quotePriceTotal", quotePriceTotal);
      serviceResult.put("quoteFsdCostTotal", quoteFsdCostTotal);
      serviceResult.put("quoteProfitTotal", quoteProfitTotal);
      serviceResult.put("quoteAverageProfitMarginPercentage", quoteAverageProfitMarginPercentage);
    } catch (Exception e) {
      Debug.logInfo(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }

    return serviceResult;
  }

  /**
   * Get the finance status for the given parameters (in context) This is a service.
   *
   * @param dctx
   * @param context
   * @return
   */
  public static Map<String, Object> getFinanceStats(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    LocalDispatcher dispatcher = dctx.getDispatcher();

    GenericValue userLogin = (GenericValue) context.get("userLogin");

    List reports = FastList.newInstance();

    try {
      // get all the AWARDED quotes matching the search conditio
      Map<String, Object> searchQuotesCtx = FastMap.newInstance();
      searchQuotesCtx.put("userLogin", userLogin);
      searchQuotesCtx.put("startIndex", context.get("startIndex"));
      searchQuotesCtx.put("viewSize", context.get("viewSize"));
      searchQuotesCtx.put("dateRangeFrom", context.get("dateRangeFrom"));
      searchQuotesCtx.put("dateRangeTo", context.get("dateRangeTo"));
      searchQuotesCtx.put("customerPartyId", context.get("customerPartyId"));
      searchQuotesCtx.put("solicitationNumber", context.get("solicitationNumber"));
      searchQuotesCtx.put("quotePurchaseOrderNumber", context.get("quotePurchaseOrderNumber"));
      searchQuotesCtx.put(
          "quoteRequisitionPurchaseRequestNumber",
          context.get("quoteRequisitionPurchaseRequestNumber"));
      searchQuotesCtx.put("contractStatedAwardDate", context.get("contractStatedAwardDate"));
      searchQuotesCtx.put("quoteOrderNumber", context.get("quoteOrderNumber"));
      searchQuotesCtx.put(
          "quoteContractReferenceNumber", context.get("quoteContractReferenceNumber"));
      searchQuotesCtx.put("quoteTypeIds", context.get("quoteTypeIds"));
      searchQuotesCtx.put("quoteId", context.get("quoteId"));
      searchQuotesCtx.put("partyId", context.get("partyId"));
      searchQuotesCtx.put("supplierPartyId", context.get("supplierPartyId"));
      searchQuotesCtx.put("supplierContactPartyId", context.get("supplierContactPartyId"));
      searchQuotesCtx.put("statusIds", context.get("statusIds"));
      searchQuotesCtx.put("productId", context.get("productId"));
      searchQuotesCtx.put("sortBy", context.get("sortBy"));
      searchQuotesCtx.put("awardStatusIds", context.get("awardStatusIds"));
      searchQuotesCtx.put("billingStatusId", context.get("billingStatusId"));
      searchQuotesCtx.put("quoteInvoiceTypeId", context.get("quoteInvoiceTypeId"));
      searchQuotesCtx.put("quoteInvoiceStatusId", context.get("quoteInvoiceStatusId"));
      searchQuotesCtx.put("shipmentStatusId", context.get("shipmentStatusId"));
      searchQuotesCtx.put("fsdToCMInvoiceStatusId", context.get("fsdToCMInvoiceStatusId"));
      searchQuotesCtx.put("quoteOrderedDateRangeFrom", context.get("quoteOrderedDateRangeFrom"));
      searchQuotesCtx.put("quoteOrderedDateRangeTo", context.get("quoteOrderedDateRangeTo"));
      searchQuotesCtx.put("endOfWarrantyServiceFrom", context.get("endOfWarrantyServiceFrom"));
      searchQuotesCtx.put("endOfWarrantyServiceTo", context.get("endOfWarrantyServiceTo"));
      searchQuotesCtx.put("onlyWarrantyService", context.get("onlyWarrantyService"));
      searchQuotesCtx.put("thankYouCardSent", context.get("thankYouCardSent"));
      searchQuotesCtx.put("awardServiceTracked", context.get("awardServiceTracked"));
      searchQuotesCtx.put("shippingState", context.get("shippingState"));
      searchQuotesCtx.put("shippingCity", context.get("shippingCity"));
      searchQuotesCtx.put("tagIds", context.get("tagIds"));
      searchQuotesCtx.put("quoteInvoiceId", context.get("quoteInvoiceId"));
      searchQuotesCtx.put("quotePaymentStatusId", context.get("quotePaymentStatusId"));
      searchQuotesCtx.put("quoteFlagStatus", context.get("quoteFlagStatus"));

      Map<String, Object> searchQuotesResult = dispatcher.runSync("searchQuotes", searchQuotesCtx);

      if (!ServiceUtil.isSuccess(searchQuotesResult)) {
        return searchQuotesResult;
      }

      int recordsSize = (int) searchQuotesResult.get("totalResultSize");
      // amount totals
      BigDecimal quotePriceTotal = (BigDecimal) searchQuotesResult.get("quotePriceTotal");
      BigDecimal quoteFsdCostTotal = (BigDecimal) searchQuotesResult.get("quoteFsdCostTotal");
      BigDecimal quoteProfitTotal = (BigDecimal) searchQuotesResult.get("quoteProfitTotal");
      String quoteAverageProfitMarginPercentage =
          (String) searchQuotesResult.get("quoteAverageProfitMarginPercentage");

      serviceResult.put("totalResultSize", recordsSize);
      serviceResult.put("quotePriceTotal", quotePriceTotal);
      serviceResult.put("quoteFsdCostTotal", quoteFsdCostTotal);
      serviceResult.put("quoteProfitTotal", quoteProfitTotal);
      serviceResult.put("quoteAverageProfitMarginPercentage", quoteAverageProfitMarginPercentage);
    } catch (Exception e) {
      Debug.logInfo(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }

    return serviceResult;
  }

  /**
   * Delete the payment for the given payment Id This is a service.
   *
   * @param dctx
   * @param context
   * @return
   */
  public static Map<String, Object> fsdDeletePayment(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    LocalDispatcher dispatcher = dctx.getDispatcher();

    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String paymentId = (String) context.get("paymentId");
    String invoiceId = (String) context.get("invoiceId");

    try {
      // remove references from AcctgTrans and AcctgTransEntry entities
      List<GenericValue> accountTransactions =
          EntityQuery.use(delegator).from("AcctgTrans").where("paymentId", paymentId).queryList();
      if (UtilValidate.isNotEmpty(accountTransactions)) {
        for (GenericValue accTrans : accountTransactions) {
          List<GenericValue> acctTransactionEnteries =
              EntityQuery.use(delegator)
                  .from("AcctgTransEntry")
                  .where("acctgTransId", accTrans.getString("acctgTransId"))
                  .queryList();
          if (UtilValidate.isNotEmpty(acctTransactionEnteries)) {
            for (GenericValue accEntry : acctTransactionEnteries) {
              delegator.removeValue(accEntry);
            }
          }
          delegator.removeValue(accTrans);
        }
      }
      // remove reference from payment application entity
      List<GenericValue> pmtApplications =
          EntityQuery.use(delegator)
              .from("PaymentApplication")
              .where("paymentId", paymentId)
              .queryList();
      if (UtilValidate.isNotEmpty(pmtApplications)) {
        for (GenericValue pmtApplication : pmtApplications) {
          delegator.removeValue(pmtApplication);
        }
      }

      // remove payment note as well if exists
      List<GenericValue> paymentNotes =
          EntityQuery.use(delegator).from("PaymentNote").where("paymentId", paymentId).queryList();
      if (UtilValidate.isNotEmpty(paymentNotes)) {
        for (GenericValue paymentNote : paymentNotes) {
          delegator.removeValue(paymentNote);
        }
      }
      GenericValue payment =
          EntityQuery.use(delegator).from("Payment").where("paymentId", paymentId).queryOne();
      if (UtilValidate.isNotEmpty(payment)) {
        delegator.removeValue(payment);
      }

      // change the status of Invoice from paid to ready
      GenericValue invoice =
          EntityQuery.use(delegator).from("Invoice").where("invoiceId", invoiceId).queryOne();
      invoice.set("statusId", "INVOICE_READY");
      invoice.store();
    } catch (Exception e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }
    return serviceResult;
  }

  public static String uploadInvoiceDocuments(
      HttpServletRequest request, HttpServletResponse response) {
    LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
    GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");

    boolean bError = false;

    String invoiceIdField = "invoiceId";
    String uploadField = "dataResourceName";

    String invoiceId = null;
    String invoiceContentTypeId = null;

    Map<String, Object> results = new HashMap<String, Object>();
    Map<String, String> formInput = new HashMap<String, String>();
    results.put("formInput", formInput);
    ServletFileUpload fu =
        new ServletFileUpload(new DiskFileItemFactory(10240, new File(new File("runtime"), "tmp")));
    List<FileItem> lst = null;
    try {
      lst = UtilGenerics.checkList(fu.parseRequest(request));
    } catch (FileUploadException e) {
      return "error";
    }

    if (lst.size() == 0) {
      return "error";
    }

    // This code finds the idField and the upload FileItems
    FileItem fi = null;
    int countImageFields = 0;
    List<Map<String, Object>> uploadResults = FastList.newInstance();
    for (int i = 0; i < lst.size(); i++) {
      fi = lst.get(i);
      String fieldName = fi.getFieldName();
      String fieldStr = fi.getString();
      if (fi.isFormField()) {
        formInput.put(fieldName, fieldStr);
        request.setAttribute(fieldName, fieldStr);
      }
      if (fieldName.equals(invoiceIdField)) {
        invoiceId = fieldStr;
      } else if (fieldName.equals("invoiceContentTypeId")) {
        invoiceContentTypeId = fieldStr;
      } else if (fieldName.equals(uploadField)) {
        countImageFields++;

        // MimeType of upload file
        Map<String, Object> map = FastMap.newInstance();
        map.put("uploadMimeType", fi.getContentType());

        byte[] imageBytes = fi.get();
        ByteBuffer byteWrap = ByteBuffer.wrap(imageBytes);
        map.put("imageData", byteWrap);
        map.put("imageFileName", fi.getName());
        uploadResults.add(map);
      }
    }

    if (UtilValidate.isEmpty(invoiceId)) {
      String errMsg = "Empty Invoice Id";
      request.setAttribute("_ERROR_MESSAGE_", errMsg);
      Debug.logError(errMsg, module);
      return "error";
    }
    if (UtilValidate.isEmpty(invoiceContentTypeId)) {
      String errMsg = "Empty Invoice content type Id";
      request.setAttribute("_ERROR_MESSAGE_", errMsg);
      Debug.logError(errMsg, module);
      return "error";
    }

    if (countImageFields == 0) {
      String errMsg = "No files to upload";
      request.setAttribute("_ERROR_MESSAGE_", errMsg);
      Debug.logWarning(errMsg, module);
      return "error";
    }

    for (Map<String, Object> uploadResult : uploadResults) {
      try {
        Map<String, Object> serviceResult;
        Map<String, Object> serviceContext = FastMap.newInstance();
        serviceContext.put("userLogin", userLogin);
        serviceContext.put("_uploadedFile_fileName", uploadResult.get("imageFileName"));
        serviceContext.put("uploadedFile", uploadResult.get("imageData"));
        serviceContext.put("_uploadedFile_contentType", uploadResult.get("uploadMimeType"));
        serviceResult = dispatcher.runSync("extCreateContentFromUploadedFile", serviceContext);
        if (!ServiceUtil.isSuccess(serviceResult)) {
          String errMsg = "extCreateContentFromUploadedFile failed";
          request.setAttribute("_ERROR_MESSAGE_", errMsg);
          Debug.logError(errMsg, module);
          return "error";
        }
        String contentId = (String) serviceResult.get("contentId");

        serviceContext = FastMap.newInstance();
        serviceContext.put("userLogin", userLogin);
        serviceContext.put("invoiceId", invoiceId);
        serviceContext.put("invoiceContentTypeId", invoiceContentTypeId);
        serviceContext.put("contentId", contentId);
        serviceContext.put("fromDate", UtilDateTime.nowTimestamp());
        serviceResult = dispatcher.runSync("createInvoiceContent", serviceContext);
        if (!ServiceUtil.isSuccess(serviceResult)) {
          String errMsg =
              "createInvoiceContent failed with details: "
                  + ServiceUtil.getErrorMessage(serviceResult);
          request.setAttribute("_ERROR_MESSAGE_", errMsg);
          Debug.logError(errMsg, module);
          return "error";
        }
      } catch (GenericServiceException e) {
        Debug.logError(e, module);
        request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
        bError = true;
      }
    }

    if (bError) {
      return "error";
    }
    return "success";
  }

  /**
   * Update the payment details for the given payment Id This is a service.
   *
   * @param dctx
   * @param context
   * @return
   */
  public static Map<String, Object> fsdUpdatePayment(
      DispatchContext dctx, Map<String, Object> context) throws Exception {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    LocalDispatcher dispatcher = dctx.getDispatcher();

    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String paymentId = (String) context.get("paymentId");
    String paymentRefNum = (String) context.get("paymentRefNum");
    String effectiveDate = (String) context.get("effectiveDate");
    Timestamp effectiveFormattedDate = null;
    SimpleDateFormat sdf1 = new SimpleDateFormat("EEE MMM dd yyyy");
    if (UtilValidate.isNotEmpty(effectiveDate)) {
      Date dtEffectiveDate = sdf1.parse(effectiveDate);
      effectiveFormattedDate = UtilDateTime.toTimestamp(dtEffectiveDate);
    }

    Timestamp currentDate = UtilDateTime.nowTimestamp();
    Boolean isInvalidEffectiveDate = false;
    long t1 = currentDate.getTime();
    long t2 = effectiveFormattedDate.getTime();
    if (t2 > t1) {
      isInvalidEffectiveDate = true;
    }

    try {
      // change the status of Invoice from paid to ready
      GenericValue payment =
          EntityQuery.use(delegator).from("Payment").where("paymentId", paymentId).queryOne();
      payment.set("paymentRefNum", paymentRefNum);
      payment.set("effectiveDate", effectiveFormattedDate);
      if (isInvalidEffectiveDate) {
        payment.set("statusId", "PMNT_PENDING");
        // change the status of Invoice from pending to pending as well
        GenericValue paymentAppl =
            EntityQuery.use(delegator)
                .from("PaymentApplication")
                .where("paymentId", paymentId)
                .queryOne();
        String invoiceId = (String) paymentAppl.getString("invoiceId");
        GenericValue invoiceRecord =
            EntityQuery.use(delegator).from("Invoice").where("invoiceId", invoiceId).queryFirst();
        // mark invoice as pending
        invoiceRecord.set("statusId", "INVOICE_PENDING");
        delegator.store(invoiceRecord);
      } else {
        payment.set("statusId", "PMNT_RECEIVED");
        BigDecimal totalAmountApplied = BigDecimal.ZERO;
        // change the status of Invoice from pending to paid as well
        GenericValue paymentAppl =
            EntityQuery.use(delegator)
                .from("PaymentApplication")
                .where("paymentId", paymentId)
                .queryOne();
        String invoiceId = (String) paymentAppl.getString("invoiceId");
        List<GenericValue> paymentApplications =
            EntityQuery.use(delegator)
                .from("PaymentApplication")
                .where("invoiceId", invoiceId)
                .queryList();
        for (GenericValue paymentApplication : paymentApplications) {
          totalAmountApplied =
              totalAmountApplied.add(paymentApplication.getBigDecimal("amountApplied"));
        }
        // get invoice total
        GenericValue invoiceRecord =
            EntityQuery.use(delegator).from("Invoice").where("invoiceId", invoiceId).queryFirst();
        BigDecimal invoiceTotal = AxInvoiceWorker.getInvoiceTotal(invoiceRecord);
        if (invoiceTotal.equals(totalAmountApplied)) {
          // mark invoice as paid
          invoiceRecord.set("statusId", "INVOICE_PAID");
          delegator.store(invoiceRecord);
        }
      }
      payment.store();

    } catch (Exception e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }
    return serviceResult;
  }

  /**
   * Capture the payment for the given invoice Id This is a service.
   *
   * @param dctx
   * @param context
   * @return
   */
  public static Map<String, Object> fsdMakePayment(
      DispatchContext dctx, Map<String, Object> context) throws Exception {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    LocalDispatcher dispatcher = dctx.getDispatcher();

    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String invoiceId = (String) context.get("invoiceId");
    String paymentRefNum = (String) context.get("paymentRefNum");
    String currencyUomId = (String) context.get("currencyUomId");
    String partyIdFrom = (String) context.get("partyIdFrom");
    String partyIdTo = (String) context.get("partyIdTo");
    String amountStr = (String) context.get("amount");
    String paymentTypeId = (String) context.get("paymentTypeId");
    String paymentMethodId = (String) context.get("paymentMethodId");
    String paymentMethodTypeId = (String) context.get("paymentMethodTypeId");
    String statusId = (String) context.get("statusId");
    String effectiveDate = (String) context.get("effectiveDate");
    BigDecimal amount = new BigDecimal(amountStr);

    if (UtilValidate.isEmpty(paymentTypeId)) {
      String _ERROR_MESSAGE_ = "Please capture payment type to proceed";
      return ServiceUtil.returnError(_ERROR_MESSAGE_);
    }
    if (UtilValidate.isEmpty(paymentMethodId)) {
      String _ERROR_MESSAGE_ = "Please capture payment method to proceed";
      return ServiceUtil.returnError(_ERROR_MESSAGE_);
    }

    Timestamp effectiveFormattedDate = null;
    SimpleDateFormat sdf1 = new SimpleDateFormat("EEE MMM dd yyyy");
    if (UtilValidate.isNotEmpty(effectiveDate)) {
      Date dtEffectiveDate = sdf1.parse(effectiveDate);
      effectiveFormattedDate = UtilDateTime.toTimestamp(dtEffectiveDate);
    }

    Timestamp currentDate = UtilDateTime.nowTimestamp();
    Boolean isInvalidEffectiveDate = false;
    long t1 = currentDate.getTime();
    long t2 = effectiveFormattedDate.getTime();
    if (t2 > t1) {
      isInvalidEffectiveDate = true;
    }
    try {

      // change the status of invoice to INVOICE_READY if it is INVOICE_IN_PROCESS (for purchase
      // invoices)
      GenericValue invoice =
          EntityQuery.use(delegator).from("Invoice").where("invoiceId", invoiceId).queryOne();
      if (UtilValidate.isNotEmpty(invoice)) {
        String invoiceStatus = (String) invoice.get("statusId");
        if (invoiceStatus.equals("INVOICE_IN_PROCESS")) {
          invoice.set("statusId", "INVOICE_READY");
          invoice.store();
        }
      }

      Map<String, Object> createPaymentAndApplicationCtx = FastMap.newInstance();
      createPaymentAndApplicationCtx.put("userLogin", userLogin);
      createPaymentAndApplicationCtx.put("invoiceId", invoiceId);
      createPaymentAndApplicationCtx.put("paymentRefNum", paymentRefNum);
      createPaymentAndApplicationCtx.put("currencyUomId", currencyUomId);
      createPaymentAndApplicationCtx.put("partyIdFrom", partyIdFrom);
      createPaymentAndApplicationCtx.put("partyIdTo", partyIdTo);
      createPaymentAndApplicationCtx.put("amount", amount);
      createPaymentAndApplicationCtx.put("effectiveDate", effectiveFormattedDate);
      createPaymentAndApplicationCtx.put("paymentTypeId", paymentTypeId);
      createPaymentAndApplicationCtx.put("paymentMethodId", paymentMethodId);
      createPaymentAndApplicationCtx.put("paymentMethodTypeId", paymentMethodTypeId);
      createPaymentAndApplicationCtx.put("statusId", statusId);

      Map<String, Object> createPaymentAndApplicationResp =
          dispatcher.runSync("fsdCreatePaymentAndApplication", createPaymentAndApplicationCtx);
      if (ServiceUtil.isError(createPaymentAndApplicationResp)) {
        return createPaymentAndApplicationResp;
      }
      String paymentId = (String) createPaymentAndApplicationResp.get("paymentId");

      // mark payment as received automatically
      Map<String, Object> setPaymentStatusCtx = FastMap.newInstance();
      setPaymentStatusCtx.put("userLogin", userLogin);
      setPaymentStatusCtx.put("paymentId", paymentId);
      if (isInvalidEffectiveDate) {
        setPaymentStatusCtx.put("statusId", "PMNT_PENDING");
      } else {
        setPaymentStatusCtx.put("statusId", "PMNT_RECEIVED");
      }
      Map<String, Object> setPaymentStatusResp =
          dispatcher.runSync("setFsdPaymentStatus", setPaymentStatusCtx);
      if (ServiceUtil.isError(setPaymentStatusResp)) {
        return setPaymentStatusResp;
      }

      // handles future date
      if (isInvalidEffectiveDate) {
        // change status of invoice to pending
        if (UtilValidate.isNotEmpty(invoice)) {
          String invoiceStatus = (String) invoice.get("statusId");
          if (invoiceStatus.equals("INVOICE_READY")) {
            invoice.set("statusId", "INVOICE_PENDING");
            invoice.store();
          }
        }

        Map<String, Object> updatePaymentDetailsCtx = FastMap.newInstance();
        updatePaymentDetailsCtx.put("userLogin", userLogin);
        updatePaymentDetailsCtx.put("paymentId", paymentId);
        updatePaymentDetailsCtx.put("effectiveDate", effectiveDate);

        Map<String, Object> fsdUpdatePaymentResp =
            dispatcher.runSync("fsdUpdatePayment", updatePaymentDetailsCtx);
        if (ServiceUtil.isError(fsdUpdatePaymentResp)) {
          return fsdUpdatePaymentResp;
        }
      }

    } catch (Exception e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }
    return serviceResult;
  }

  /**
   * Capture the payment and apply it for a Quote This is a service.
   *
   * @param dctx
   * @param context
   * @return
   */
  public static Map<String, Object> fsdMakePaymentForQuote(
      DispatchContext dctx, Map<String, Object> context) throws Exception {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    LocalDispatcher dispatcher = dctx.getDispatcher();

    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String quoteId = (String) context.get("quoteId");
    String paymentRefNum = (String) context.get("paymentRefNum");
    String currencyUomId = (String) context.get("currencyUomId");
    String partyIdFrom = (String) context.get("partyIdFrom");
    String partyIdTo = (String) context.get("partyIdTo");
    String amountStr = (String) context.get("amount");
    String paymentTypeId = (String) context.get("paymentTypeId");
    String paymentMethodId = (String) context.get("paymentMethodId");
    String paymentMethodTypeId = (String) context.get("paymentMethodTypeId");
    String statusId = (String) context.get("statusId");
    String effectiveDate = (String) context.get("effectiveDate");
    BigDecimal amount = new BigDecimal(amountStr);

    if (UtilValidate.isEmpty(paymentTypeId)) {
      String _ERROR_MESSAGE_ = "Please capture payment type to proceed";
      return ServiceUtil.returnError(_ERROR_MESSAGE_);
    }
    if (UtilValidate.isEmpty(paymentMethodId)) {
      String _ERROR_MESSAGE_ = "Please capture payment method to proceed";
      return ServiceUtil.returnError(_ERROR_MESSAGE_);
    }

    Timestamp effectiveFormattedDate = null;
    SimpleDateFormat sdf1 = new SimpleDateFormat("EEE MMM dd yyyy");
    if (UtilValidate.isNotEmpty(effectiveDate)) {
      Date dtEffectiveDate = sdf1.parse(effectiveDate);
      effectiveFormattedDate = UtilDateTime.toTimestamp(dtEffectiveDate);
    }

    Timestamp currentDate = UtilDateTime.nowTimestamp();
    Boolean isInvalidEffectiveDate = false;
    long t1 = currentDate.getTime();
    long t2 = effectiveFormattedDate.getTime();
    if (t2 > t1) {
      isInvalidEffectiveDate = true;
    }
    Timestamp effectiveFormattedDateNS = null;
    if (isInvalidEffectiveDate) {
      effectiveFormattedDateNS = UtilDateTime.nowTimestamp();
    }

    try {

      // change the status of invoice to INVOICE_READY if it is INVOICE_IN_PROCESS (for purchase
      // invoices)
      /*GenericValue invoice = EntityQuery.use(delegator).from("Invoice").where("invoiceId", invoiceId).queryOne();
      if(UtilValidate.isNotEmpty(invoice)){
          String invoiceStatus = (String) invoice.get("statusId");
          if(invoiceStatus.equals("INVOICE_IN_PROCESS")) {
              invoice.set("statusId", "INVOICE_READY");
              invoice.store();
          }
      }*/

      Map<String, Object> createPaymentAndApplicationCtx = FastMap.newInstance();
      createPaymentAndApplicationCtx.put("userLogin", userLogin);
      createPaymentAndApplicationCtx.put("paymentRefNum", paymentRefNum);
      createPaymentAndApplicationCtx.put("currencyUomId", currencyUomId);
      createPaymentAndApplicationCtx.put("partyIdFrom", partyIdFrom);
      createPaymentAndApplicationCtx.put("partyIdTo", partyIdTo);
      createPaymentAndApplicationCtx.put("amount", amount);
      if (UtilValidate.isNotEmpty(effectiveFormattedDateNS)) {
        createPaymentAndApplicationCtx.put(
            "effectiveDate",
            effectiveFormattedDateNS); // if user enters future date, use nowTimeStamp (handle
                                       // future date in the next step).
      } else {
        createPaymentAndApplicationCtx.put("effectiveDate", effectiveFormattedDate);
      }
      createPaymentAndApplicationCtx.put("paymentTypeId", paymentTypeId);
      createPaymentAndApplicationCtx.put("paymentMethodId", paymentMethodId);
      createPaymentAndApplicationCtx.put("paymentMethodTypeId", paymentMethodTypeId);
      createPaymentAndApplicationCtx.put("statusId", statusId);
      createPaymentAndApplicationCtx.put("quoteId", quoteId);

      Map<String, Object> createPaymentAndApplicationResp =
          dispatcher.runSync("fsdCreatePaymentAndApplication", createPaymentAndApplicationCtx);
      if (ServiceUtil.isError(createPaymentAndApplicationResp)) {
        return createPaymentAndApplicationResp;
      }
      String paymentId = (String) createPaymentAndApplicationResp.get("paymentId");
      // String paymentApplicationId = (String)
      // createPaymentAndApplicationResp.get("paymentApplicationId");

      // After creating PaymentApplication --- update it with quoteId
      /*try {
          GenericValue paymentApplication = delegator.findOne("PaymentApplication", UtilMisc.toMap("paymentApplicationId",paymentApplicationId), false);
          if(UtilValidate.isNotEmpty(paymentApplication)) {
              paymentApplication.set("quoteId", quoteId);
              delegator.store(paymentApplication);
          }
      } catch (Exception ex) {
          return ServiceUtil.returnFailure("Unable to update PaymentApplication with quote Id for PaymentApplication: " + paymentApplicationId);
      }*/

      // mark payment as received automatically
      Map<String, Object> setPaymentStatusCtx = FastMap.newInstance();
      setPaymentStatusCtx.put("userLogin", userLogin);
      setPaymentStatusCtx.put("paymentId", paymentId);
      setPaymentStatusCtx.put("statusId", "PMNT_RECEIVED");
      Map<String, Object> setPaymentStatusResp =
          dispatcher.runSync("setPaymentStatus", setPaymentStatusCtx);
      if (ServiceUtil.isError(setPaymentStatusResp)) {
        return setPaymentStatusResp;
      }

      // handles future date
      if (isInvalidEffectiveDate) {
        Map<String, Object> updatePaymentDetailsCtx = FastMap.newInstance();
        updatePaymentDetailsCtx.put("userLogin", userLogin);
        updatePaymentDetailsCtx.put("paymentId", paymentId);
        updatePaymentDetailsCtx.put("effectiveDate", effectiveDate);

        Map<String, Object> fsdUpdatePaymentResp =
            dispatcher.runSync("fsdUpdatePayment", updatePaymentDetailsCtx);
        if (ServiceUtil.isError(fsdUpdatePaymentResp)) {
          return fsdUpdatePaymentResp;
        }
      }

    } catch (Exception e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }
    return serviceResult;
  }

  /**
   * Applies the Payment received on Quote to its invoices, used for future date payments that do
   * not get applied immediately. Runs via scheduled job every 15 minutes to validate.
   * <TemporalExpression tempExprId="checkPaymentPending" tempExprTypeId="FREQUENCY"
   * description="Every 15 Minutes" integer1="12" integer2="15"/> <JobSandbox jobName="Payment
   * Status job" runTime="2018-05-29 00:00:00.000" serviceName="checkPaymentPendingStatus"
   * poolId="pool" runAsUser="system" tempExprId="checkPaymentPending" maxRecurrenceCount="-1"/>
   *
   * @param dctx
   * @param context
   * @return
   * @throws Exception
   */
  public static Map<String, Object> applyQuotePaymentToInvoice(
      DispatchContext dctx, Map<String, Object> context) throws Exception {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    LocalDispatcher dispatcher = dctx.getDispatcher();

    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String quoteId = (String) context.get("quoteId");

    try {

      // 1. Get Payment for Quote (if Any)
      GenericValue payment = getPaymentApplicationForQuote(delegator, quoteId);
      if (UtilValidate.isEmpty(payment)) {
        // No Payment found -- return
        return ServiceUtil.returnSuccess();
      }

      String paymentId = payment.getString("paymentId");
      removePaymentApplicationForQuote(dispatcher, userLogin, quoteId);

      // 2. Get Invoices for the quote
      GenericValue salesInvoice = null;
      // TODO: to be done after implement xorder plugin.
      /*QuoteBuilder quoteBuilder = QuoteBuilder.quote(delegator, quoteId);
      if (UtilValidate.isNotEmpty(quoteBuilder)) {
        salesInvoice = quoteBuilder.salesInvoice();
      }*/

      String salesInvoiceId = salesInvoice.getString("invoiceId");
      if (UtilValidate.isNotEmpty(salesInvoiceId)) {
        // Apply Payment
        Map<String, Object> paymentAppInputs = UtilMisc.toMap("paymentId", paymentId);
        paymentAppInputs.put("userLogin", userLogin);
        paymentAppInputs.put("invoiceId", salesInvoiceId);
        Map<String, Object> applyPaymentResp =
            dispatcher.runSync("fsdCreatePaymentApplication", paymentAppInputs);
        if (ServiceUtil.isFailure(applyPaymentResp)) {
          return applyPaymentResp;
        }

        // mark invoice as paid if payment is also paid
        GenericValue invoiceRecord =
            EntityQuery.use(delegator)
                .from("Invoice")
                .where("invoiceId", salesInvoiceId)
                .queryFirst();
        BigDecimal invoiceTotal = AxInvoiceWorker.getInvoiceTotal(invoiceRecord);
        if (invoiceTotal.equals(payment.getBigDecimal("amountApplied"))) {
          // mark invoice as paid
          invoiceRecord.set("statusId", "INVOICE_PAID");
          delegator.store(invoiceRecord);
        }

        // set billing location and billed amount
        GenericValue paymentRec = payment.getRelatedOne("Payment", false);
        Timestamp paymentDate = paymentRec.getTimestamp("effectiveDate");
        String invoicedDate =
            UtilDateTime.toDateString(new java.util.Date(paymentDate.getTime()), "MM/dd/yyyy");
        Map<String, Object> setBillingDetailsCtx =
            UtilMisc.toMap(
                "invoiceId",
                salesInvoiceId,
                "billingParty",
                "BILLING_LOC_03",
                "quoteId",
                quoteId,
                "invoicedDate",
                invoicedDate,
                "isBillAccepted",
                "Y",
                "billedAmount",
                payment.getBigDecimal("amountApplied"),
                "billingLocation",
                "https://account.authorize.net/",
                "userLogin",
                userLogin);
        Map<String, Object> setBillingDetailsCtxResp =
            dispatcher.runSync("setInvoiceBillingDetails", setBillingDetailsCtx);
        if (ServiceUtil.isFailure(setBillingDetailsCtxResp)) {
          return setBillingDetailsCtxResp;
        }
      }
    } catch (Exception e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }

    return serviceResult;
  }

  private static GenericValue getPaymentApplicationForQuote(Delegator delegator, String quoteId)
      throws GenericEntityException {
    List<GenericValue> paymentApplications =
        delegator.findByAnd("PaymentApplication", UtilMisc.toMap("quoteId", quoteId), null, false);
    if (UtilValidate.isEmpty(paymentApplications)) return null;
    return paymentApplications.get(0);
  }

  private static void removePaymentApplicationForQuote(
      LocalDispatcher dispatcher, GenericValue userLogin, String quoteId)
      throws GenericEntityException, GenericServiceException {
    Delegator delegator = dispatcher.getDelegator();
    List<GenericValue> paymentApplications =
        delegator.findByAnd("PaymentApplication", UtilMisc.toMap("quoteId", quoteId), null, false);
    if (UtilValidate.isNotEmpty(paymentApplications)) {
      GenericValue paymentApplication = paymentApplications.get(0);
      Map<String, Object> removePaymentApplResp =
          dispatcher.runSync(
              "removePaymentApplication",
              UtilMisc.toMap(
                  "userLogin",
                  userLogin,
                  "paymentApplicationId",
                  paymentApplication.getString("paymentApplicationId")));
      if (ServiceUtil.isFailure(removePaymentApplResp)) {
        // throw exception
      }
    }
  }

  public static Map<String, Object> getSumOfFees(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    LocalDispatcher dispatcher = dctx.getDispatcher();
    Delegator delegator = dctx.getDelegator();

    GenericValue userLogin = (GenericValue) context.get("userLogin");

    Timestamp startDate = (Timestamp) context.get("startDate");
    Timestamp endDate = (Timestamp) context.get("endDate");

    SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    isoDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    String startDateString = isoDateFormat.format(new Date(startDate.getTime()));
    String endDateString = isoDateFormat.format(new Date(endDate.getTime()));

    BigDecimal sumOfLoanFinanceCharges = BigDecimal.ZERO;
    BigDecimal sumOfGeneralFinanceCharges = BigDecimal.ZERO;
    BigDecimal sumOfGeneralCostOfBusiness = BigDecimal.ZERO;
    BigDecimal sumOfThirdPartyBillingCharges = BigDecimal.ZERO;

    try {

      Map<String, Object> searchQuotesCtx = FastMap.newInstance();

      List<String> statusIds = FastList.newInstance();
      statusIds.add("QUO_ORDERED");

      searchQuotesCtx.put("userLogin", userLogin);
      searchQuotesCtx.put("quoteOrderedDateRangeFrom", startDateString);
      searchQuotesCtx.put("quoteOrderedDateRangeTo", endDateString);
      searchQuotesCtx.put("statusIds", statusIds);
      searchQuotesCtx.put("startIndex", 0);
      searchQuotesCtx.put("viewSize", 10000000); // Huge

      Map<String, Object> searchQuotesResp = dispatcher.runSync("searchQuotes", searchQuotesCtx);

      if (!ServiceUtil.isSuccess(searchQuotesResp)) {
        return searchQuotesResp;
      }

      List<Map<String, Object>> quotes =
          (List<Map<String, Object>>) searchQuotesResp.get("searchResults");

      for (Map<String, Object> quote : quotes) {
        String quoteId = (String) quote.get("quoteId");
        List<GenericValue> quoteLoanAssocs =
            EntityQuery.use(delegator)
                .from("QuoteLoanAssoc")
                .where(UtilMisc.toMap("quoteId", quoteId))
                .queryList();
        for (GenericValue quoteLoanAssoc : quoteLoanAssocs) {
          String loanId = (String) quoteLoanAssoc.get("loanId");
          GenericValue loan =
              EntityQuery.use(delegator)
                  .from("Loan")
                  .where(UtilMisc.toMap("loanId", loanId))
                  .queryOne();
          if (UtilValidate.isNotEmpty(loan)) {
            BigDecimal loanFinanceCharge = loan.getBigDecimal("loanFinanceCharge");
            sumOfLoanFinanceCharges = sumOfLoanFinanceCharges.add(loanFinanceCharge);
          }
        }

        GenericValue generalFinanceFee =
            EntityQuery.use(delegator)
                .from("GeneralFinanceFee")
                .where(UtilMisc.toMap("quoteId", quoteId))
                .queryOne();
        if (UtilValidate.isNotEmpty(generalFinanceFee)) {
          BigDecimal generalFinanceCharge = generalFinanceFee.getBigDecimal("financeCharge");
          sumOfGeneralFinanceCharges = sumOfGeneralFinanceCharges.add(generalFinanceCharge);
        }

        GenericValue generalCostOfBusiness =
            EntityQuery.use(delegator)
                .from("GeneralCostOfBusiness")
                .where(UtilMisc.toMap("quoteId", quoteId))
                .queryOne();
        if (UtilValidate.isNotEmpty(generalCostOfBusiness)) {
          BigDecimal generalCostOfBusinessCharge =
              generalCostOfBusiness.getBigDecimal("financeCharge");
          sumOfGeneralCostOfBusiness = sumOfGeneralCostOfBusiness.add(generalCostOfBusinessCharge);
        }

        // TODO: to be implemented after integrated xorder plugin
        /*String thirdPartyCharge = FinanceUtil.getQuoteAttribute(delegator, quoteId, "thirdPartyCharge");
        if (UtilValidate.isNotEmpty(thirdPartyCharge)) {
          BigDecimal bdThirdPartyCharge = new BigDecimal(thirdPartyCharge);
          sumOfThirdPartyBillingCharges = sumOfThirdPartyBillingCharges.add(bdThirdPartyCharge);
        }*/
      }

      serviceResult.put("sumOfLoanFinanceCharges", sumOfLoanFinanceCharges);
      serviceResult.put("sumOfGeneralFinanceCharges", sumOfGeneralFinanceCharges);
      serviceResult.put("sumOfGeneralCostOfBusiness", sumOfGeneralCostOfBusiness);
      serviceResult.put("sumOfThirdPartyBillingCharges", sumOfThirdPartyBillingCharges);
    } catch (Exception e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }

    return serviceResult;
  }

  public static Map<String, Object> fsdCreateFinanceAccount(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    String finSortCode = (String) context.get("finSortCode");
    String finBankName = (String) context.get("finBankName");
    String finBankAddress = (String) context.get("finBankAddress");
    String finAccountName = (String) context.get("finAccountName");
    String finAccountCode = (String) context.get("finAccountCode");
    String currencyUomId = (String) context.get("currencyUomId");
    String organizationPartyId = (String) context.get("organizationPartyId");
    String ownerPartyId = (String) context.get("ownerPartyId");
    String finAccountTypeId = (String) context.get("finAccountTypeId");

    try {
      String finAccountId = delegator.getNextSeqId("FinAccount");
      GenericValue finAccount = delegator.makeValue("FinAccount");
      finAccount.set("finAccountId", finAccountId);
      finAccount.set("finAccountTypeId", finAccountTypeId);
      finAccount.set("statusId", "Active");
      finAccount.set("finSortCode", finSortCode);
      finAccount.set("finBankName", finBankName);
      finAccount.set("finBankAddress", finBankAddress);
      finAccount.set("currencyUomId", currencyUomId);
      finAccount.set("finAccountName", finAccountName);
      finAccount.set("finAccountCode", finAccountCode);
      finAccount.set("organizationPartyId", organizationPartyId);
      finAccount.set("ownerPartyId", ownerPartyId);
      finAccount.set("fromDate", UtilDateTime.nowTimestamp());
      finAccount.create();

      serviceResult.put("finAccountId", finAccountId);
    } catch (GenericEntityException e) {
      Debug.logError(e.getMessage(), module);
      return ServiceUtil.returnError(e.getMessage());
    }
    return serviceResult;
  }

  public static Map<String, Object> fsdUpdateFinanceAccount(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    String finAccountId = (String) context.get("finAccountId");
    String finSortCode = (String) context.get("finSortCode");
    String finBankName = (String) context.get("finBankName");
    String finBankAddress = (String) context.get("finBankAddress");
    String finAccountName = (String) context.get("finAccountName");
    String finAccountCode = (String) context.get("finAccountCode");
    String currencyUomId = (String) context.get("currencyUomId");
    String organizationPartyId = (String) context.get("organizationPartyId");
    String ownerPartyId = (String) context.get("ownerPartyId");
    String finAccountTypeId = (String) context.get("finAccountTypeId");

    try {
      GenericValue finAccount =
          EntityQuery.use(delegator)
              .from("FinAccount")
              .where("finAccountId", finAccountId)
              .queryOne();
      if (UtilValidate.isNotEmpty(finAccount)) {
        finAccount.set("finAccountId", finAccountId);
        if (UtilValidate.isNotEmpty(finAccountTypeId)) {
          finAccount.set("finAccountTypeId", finAccountTypeId);
        }
        finAccount.set("statusId", "Active");
        if (UtilValidate.isNotEmpty(finSortCode)) {
          finAccount.set("finSortCode", finSortCode);
        }
        if (UtilValidate.isNotEmpty(finBankName)) {
          finAccount.set("finBankName", finBankName);
        }
        if (UtilValidate.isNotEmpty(finBankAddress)) {
          finAccount.set("finBankAddress", finBankAddress);
        }
        if (UtilValidate.isNotEmpty(currencyUomId)) {
          finAccount.set("currencyUomId", currencyUomId);
        }
        if (UtilValidate.isNotEmpty(finAccountName)) {
          finAccount.set("finAccountName", finAccountName);
        }
        if (UtilValidate.isNotEmpty(finAccountCode)) {
          finAccount.set("finAccountCode", finAccountCode);
        }
        if (UtilValidate.isNotEmpty(organizationPartyId)) {
          finAccount.set("organizationPartyId", organizationPartyId);
        }
        if (UtilValidate.isNotEmpty(ownerPartyId)) {
          finAccount.set("ownerPartyId", ownerPartyId);
        }

        finAccount.set("fromDate", finAccount.getTimestamp("fromDate"));
        finAccount.store();
      }
    } catch (GenericEntityException e) {
      Debug.logError(e.getMessage(), module);
      return ServiceUtil.returnError(e.getMessage());
    }
    return serviceResult;
  }

  public static Map<String, Object> fsdRemoveFinanceAccount(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    String finAccountId = (String) context.get("finAccountId");

    try {
      GenericValue partyAttribute =
          EntityQuery.use(delegator)
              .from("PartyAttribute")
              .where("attrName", "WirePaymentInstruction", "attrValue", finAccountId)
              .queryOne();
      if (UtilValidate.isNotEmpty(partyAttribute)) {
        partyAttribute.remove();
      }
      GenericValue finAccount =
          EntityQuery.use(delegator)
              .from("FinAccount")
              .where("finAccountId", finAccountId)
              .queryOne();
      if (UtilValidate.isNotEmpty(finAccount)) {
        finAccount.remove();
      }
    } catch (GenericEntityException e) {
      Debug.logError(e.getMessage(), module);
      return ServiceUtil.returnError(e.getMessage());
    }
    return serviceResult;
  }
}
