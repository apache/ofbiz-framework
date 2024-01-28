package com.simbaquartz.xcommon.services.preferences;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;

public class PreferenceServices {
  public static final String module = PreferenceServices.class.getName();

  /**
   * Creates SupplierfacilityType, saves records to it and also checks duplicates on the basis of
   * facilityTypeId
   *
   * @param dctx
   * @param context
   * @return
   */
  public static Map<String, Object> saveSupplierFacilityTypes(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    String partyId = (String) context.get("partyId");
    String facilityTypes = (String) context.get("facilityTypes");
    List<String> facilityTypesList = Arrays.asList(facilityTypes.split(","));

    try {
      if (UtilValidate.isNotEmpty(facilityTypesList)) {
        for (String facilityTypeId : facilityTypesList) {
          if (!facilityTypeId.equals("")) {
            GenericValue existingCustomerFacilityType =
                EntityQuery.use(delegator)
                    .from("SupplierFacilityType")
                    .where("facilityTypeId", facilityTypeId, "partyId", partyId)
                    .queryOne();
            if (UtilValidate.isEmpty(existingCustomerFacilityType)) {
              GenericValue SupplierFacilityType = delegator.makeValue("SupplierFacilityType");
              SupplierFacilityType.set("facilityTypeId", facilityTypeId);
              SupplierFacilityType.set("partyId", partyId);
              SupplierFacilityType.create();
            }
          }
        }
      }
    } catch (GenericEntityException e) {
      return ServiceUtil.returnError(e.getMessage());
    }
    return serviceResult;
  }

  public static Map<String, Object> deleteSupplierFacilityTypes(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String partyId = (String) context.get("partyId");
    String facilityTypeId = (String) context.get("facilityTypeId");
    String facilityTypes = (String) context.get("facilityTypes");

    try {

      GenericValue suppFacilityTypes =
          EntityQuery.use(delegator)
              .from("SupplierFacilityType")
              .where("partyId", partyId, "facilityTypeId", facilityTypeId)
              .queryFirst();

      if (UtilValidate.isEmpty(suppFacilityTypes)) {
        String errorMsg = "No such party exists!";
        return ServiceUtil.returnError(errorMsg);
      }
      delegator.removeValue(suppFacilityTypes);
    } catch (GenericEntityException e) {
      return ServiceUtil.returnError(e.getMessage());
    }
    return serviceResult;
  }

  /**
   * save Supplier Payment Preference
   *
   * @param dctx
   * @param context
   * @return
   */
  public static Map<String, Object> saveSupplierPaymentPreference(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    String supplierPartyId = (String) context.get("supplierPartyId");
    String paymentMethodTypeId = (String) context.get("paymentMethodTypeId");
    String minimumMarkup = (String) context.get("minimumMarkup");
    String maximumMarkup = (String) context.get("maximumMarkup");
    String flatMarkup = (String) context.get("flatMarkup");
    BigDecimal minimumAmount = (BigDecimal) context.get("minimumAmount");
    BigDecimal maximumAmount = (BigDecimal) context.get("maximumAmount");
    BigDecimal flatAmount = (BigDecimal) context.get("flatAmount");
    String limitType = (String) context.get("limitType");

    try {
      GenericValue supplierPaymentPreference = delegator.makeValue("SupplierPaymentPreference");
      supplierPaymentPreference.set("supplierPartyId", supplierPartyId);
      supplierPaymentPreference.set("paymentMethodTypeId", paymentMethodTypeId);
      supplierPaymentPreference.set("limitType", limitType);
      supplierPaymentPreference.set("minimumMarkup", minimumMarkup);
      supplierPaymentPreference.set("maximumMarkup", maximumMarkup);
      supplierPaymentPreference.set("flatMarkup", flatMarkup);
      supplierPaymentPreference.set("minimumAmount", minimumAmount);
      supplierPaymentPreference.set("maximumAmount", maximumAmount);
      supplierPaymentPreference.set("flatAmount", flatAmount);

      delegator.create(supplierPaymentPreference);
    } catch (GenericEntityException e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }

    return serviceResult;
  }

  /**
   * Update Supplier Payment Preference
   *
   * @param dctx
   * @param context
   * @return
   */
  public static Map<String, Object> updateSupplierPaymentPreference(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    String supplierPartyId = (String) context.get("supplierPartyId");
    String paymentMethodTypeId = (String) context.get("paymentMethodTypeId");
    String limitType = (String) context.get("limitType");
    String minimumMarkup = (String) context.get("minimumMarkup");
    String maximumMarkup = (String) context.get("maximumMarkup");
    String flatMarkup = (String) context.get("flatMarkup");
    BigDecimal minimumAmount = (BigDecimal) context.get("minimumAmount");
    BigDecimal maximumAmount = (BigDecimal) context.get("maximumAmount");
    BigDecimal flatAmount = (BigDecimal) context.get("flatAmount");

    try {
      GenericValue supplierPaymentPreference =
          EntityQuery.use(delegator)
              .from("SupplierPaymentPreference")
              .where("supplierPartyId", supplierPartyId, "paymentMethodTypeId", paymentMethodTypeId)
              .queryOne();
      if (UtilValidate.isNotEmpty(supplierPaymentPreference)) {
        supplierPaymentPreference.set("supplierPartyId", supplierPartyId);
        supplierPaymentPreference.set("paymentMethodTypeId", paymentMethodTypeId);
        supplierPaymentPreference.set("limitType", limitType);
        supplierPaymentPreference.set("minimumMarkup", minimumMarkup);
        supplierPaymentPreference.set("maximumMarkup", maximumMarkup);
        supplierPaymentPreference.set("flatMarkup", flatMarkup);
        supplierPaymentPreference.set("minimumAmount", minimumAmount);
        supplierPaymentPreference.set("maximumAmount", maximumAmount);
        supplierPaymentPreference.set("flatAmount", flatAmount);

        delegator.store(supplierPaymentPreference);
      }
    } catch (GenericEntityException e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }

    return serviceResult;
  }
}
