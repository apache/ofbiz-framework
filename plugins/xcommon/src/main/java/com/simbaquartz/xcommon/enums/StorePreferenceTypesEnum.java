package com.simbaquartz.xcommon.enums;

/** FOr managing various types of store preferences available. */
public enum StorePreferenceTypesEnum {
  ORDER_NOTIFICATION_EMAIL("order.received.notification.email"),
  STORE_CONTACT_NAME("store.contact.name"),
  STORE_CONTACT_EMAIL("store.contact.email"),
  STORE_CONTACT_PHONE("store.contact.phone"),
  CHARGE_TAXES("chargeTaxes"),
  HAS_DISCOUNTS("hasDiscounts"),
  WEIGHT_UNIT("weightUnit"),
  SUBSCRIPTION_PLAN("subscriptionPlan"),
  MONEY_FORMAT("moneyFormat"),
  HAS_GIFT_CARDS("hasGiftCards"),
  IS_LIVE("isLive"),
  TAX_SHIPPING("taxShipping"),
  TAXES_INCLUDED("taxesIncluded"),
  TIMEZONE("timeZone"),
  TIMEZONE_ID("timeZoneId"),
  SETUP_REQUIRED("setupRequired"),
  IS_OPEN("isOpen"),
  LOGO_IMAGE_URL("logoImageUrl"),
  DOMAIN("domain"),
  INDUSTRY("industryCategoryId"),
  BUSINESS_NUMBER("businessNumber"),
  CITY("city");

  private String typeId;

  StorePreferenceTypesEnum(String typeId) {
    this.typeId = typeId;
  }

  public String getTypeId() {
    return typeId;
  }
}
