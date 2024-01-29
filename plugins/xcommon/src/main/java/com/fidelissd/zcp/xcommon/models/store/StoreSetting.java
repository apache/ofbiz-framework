package com.fidelissd.zcp.xcommon.models.store;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

import com.fidelissd.zcp.xcommon.enums.StorePreferenceTypesEnum;
import lombok.Data;
import org.apache.ofbiz.base.util.UtilValidate;

/** Store's configuration e.g. chargeTaxes, allowDiscounts */
@Data
public class StoreSetting {
  /** Whether the store needs to charge taxes. */
  @JsonProperty("chargeTaxes")
  private Boolean chargeTaxes = false;

  /** Default unit of measure for weights for the store.? */
  @JsonProperty("weightUnit")
  private String weightUnit = null;

  /**
   * Email to be used for sending incoming store order notifications via email. When a customer
   * places an order for this store this is the email used to send an incoming sales order
   * notification to store contacts.
   */
  @JsonProperty("orderNotificationEmail")
  private String orderNotificationEmail = null;

  /**
   * Store contact person name
   */
  @JsonProperty("contactName")
  private String contactName = null;

  /**
   * Store contact person email
   */
  @JsonProperty("contactEmail")
  private String contactEmail = null;

  /**
   * Store contact person phone
   */
  @JsonProperty("contactPhone")
  private String contactPhone = null;

  /** What subscription plan the store has. * */
  @JsonProperty("subscription_plan")
  private String subscriptionPlan = null;

  /** Whether the store has discounts or not * */
  @JsonProperty("hasDiscounts")
  private Boolean hasDiscounts = false;

  /** What are the money formats that store follows. * */
  @JsonProperty("moneyFormat")
  private String moneyFormat = null;

  /** Whether the store has gift cards or not * */
  @JsonProperty("hasGiftCards")
  private Boolean hasGiftCards = false;

  /** Whether the store is live or not * */
  @JsonProperty("isLive")
  private Boolean live = false;

  /** Tax on shipping * */
  @JsonProperty("taxShipping")
  private Boolean taxShipping = false;

  /** Are the taxes included * */
  @JsonProperty("taxesIncluded")
  private Boolean taxesIncluded = false;

  /** Preferred Time zone for the store. * */
  @JsonProperty("timezone")
  private String timeZone = null;

  /** check if the setup is required for the store. * */
  @JsonProperty("setupRequired")
  private Boolean setupRequired = false;

  /** store is open/close */
  @JsonProperty("isOpen")
  private Boolean open = false;

  @JsonProperty("logoImageUrl")
  private String logoImageUrl = null;

  /**
   * Domain associated with the store if any, example google.com
   */
  @JsonProperty("domain")
  private String domain = null;

  /**
   * This is the organization number used for tax purposes.
   */
  @JsonProperty("businessNumber")
  private String businessNumber = null;

  /**
   * Name of the city where store is.
   */
  @JsonProperty("cityName")
  private String cityName = null;

  /**
   * Category id of the industry.
   */
  @JsonProperty("industry")
  private String industry = null;

  public static StoreSetting populateModel(List<Map> storeSettings) {
    StoreSetting storeSetting = new StoreSetting();
    if (UtilValidate.isNotEmpty(storeSettings)) {
      for (Map storeSettingMap : storeSettings) {
        String storePrefTypeId = (String) storeSettingMap.get("storePrefTypeId");
        String storePrefValue = (String) storeSettingMap.get("storePrefValue");
        if (StorePreferenceTypesEnum.ORDER_NOTIFICATION_EMAIL.getTypeId().equals(storePrefTypeId)) {
          storeSetting.setOrderNotificationEmail(storePrefValue);
        } else if (storePrefTypeId.equals(StorePreferenceTypesEnum.WEIGHT_UNIT.getTypeId())) {
          storeSetting.setWeightUnit(storePrefValue);
        } else if (storePrefTypeId.equals(StorePreferenceTypesEnum.STORE_CONTACT_NAME.getTypeId())) {
          storeSetting.setContactName(storePrefValue);
        } else if (storePrefTypeId.equals(StorePreferenceTypesEnum.STORE_CONTACT_EMAIL.getTypeId())) {
          storeSetting.setContactEmail(storePrefValue);
        } else if (storePrefTypeId.equals(StorePreferenceTypesEnum.STORE_CONTACT_PHONE.getTypeId())) {
          storeSetting.setContactPhone(storePrefValue);
        } else if (storePrefTypeId.equals(StorePreferenceTypesEnum.BUSINESS_NUMBER.getTypeId())) {
          storeSetting.setBusinessNumber(storePrefValue);
        } else if (storePrefTypeId.equals(StorePreferenceTypesEnum.CITY.getTypeId())) {
          storeSetting.setCityName(storePrefValue);
        } else if (storePrefTypeId.equals(StorePreferenceTypesEnum.INDUSTRY.getTypeId())) {
          storeSetting.setIndustry(storePrefValue);
        } else if (storePrefTypeId.equals(StorePreferenceTypesEnum.IS_OPEN.getTypeId())) {
          if("Y".equalsIgnoreCase(storePrefValue)){
            storeSetting.setOpen(true);
          }
        } else if (storePrefTypeId.equals(StorePreferenceTypesEnum.IS_LIVE.getTypeId())) {
          if("Y".equalsIgnoreCase(storePrefValue)){
            storeSetting.setLive(true);
          }
        }
      }
    }
    return storeSetting;
  }

}
