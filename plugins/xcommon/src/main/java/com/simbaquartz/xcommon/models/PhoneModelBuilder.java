package com.simbaquartz.xcommon.models;

import com.simbaquartz.xcommon.collections.FastList;
import com.simbaquartz.xcommon.collections.FastMap;
import java.util.List;
import java.util.Map;
import org.apache.ofbiz.base.util.UtilValidate;

public class PhoneModelBuilder {

  public static void preparePhoneNumberMap(Phone phone, Map<String, Object> createUserMap) {
    Map<String, Object> phoneMap = preparePhoneNumberMap(phone);
    createUserMap.put("phone", phoneMap);
  }

  public static Map<String, Object> preparePhoneNumberMap(Phone phone) {
    Map<String, Object> phoneMap = FastMap.newInstance();
    if (UtilValidate.isNotEmpty(phone)) {
      String extension = phone.getExtension();
      String phoneNum = phone.getPhone();
      String areaCode = phone.getAreaCode();
      String countryCode = phone.getCountryCode();
      String regionCode = phone.getRegionCode();
      Boolean phoneVerified = phone.getPhoneVerified();

      if (UtilValidate.isNotEmpty(extension)) {
        phoneMap.put("extension", extension);
      }
      if (UtilValidate.isNotEmpty(phoneNum)) {
        phoneMap.put("phone", phoneNum);
      }
      if (UtilValidate.isNotEmpty(areaCode)) {
        phoneMap.put("areaCode", areaCode);
      }
      if (UtilValidate.isNotEmpty(countryCode)) {
        phoneMap.put("countryCode", countryCode);
      }
      if (UtilValidate.isNotEmpty(regionCode)) {
        phoneMap.put("regionCode", regionCode);
      }

      if (UtilValidate.isNotEmpty(phoneVerified) && phoneVerified) {
        phoneMap.put("phoneVerified", "Y");
      } else {
        phoneMap.put("phoneVerified", "N");
      }
    }
    return phoneMap;
  }

  public static List<Phone> preparePhoneModel(List<Map> phoneMaps) {
    List<Phone> phoneList = FastList.newInstance();
    for (Map phoneMap : phoneMaps) {
      Phone phone = new Phone();
      if (UtilValidate.isNotEmpty(phoneMap.get("contactNumber"))) {
        phone.setPhone((String) phoneMap.get("contactNumber"));
      }
      if (UtilValidate.isNotEmpty(phoneMap.get("countryCode"))) {
        phone.setCountryCode((String) phoneMap.get("countryCode"));
      }
      if (UtilValidate.isNotEmpty(phoneMap.get("areaCode"))) {
        phone.setAreaCode((String) phoneMap.get("areaCode"));
      }
      if (UtilValidate.isNotEmpty(phoneMap.get("extension"))) {
        phone.setExtension((String) phoneMap.get("extension"));
      }
      if (UtilValidate.isNotEmpty(phoneMap.get("formattedPhoneNumberInUSFormat"))) {
        phone.setPhoneFormatted((String) phoneMap.get("formattedPhoneNumberInUSFormat"));
      }
      if (UtilValidate.isNotEmpty(phoneMap.get("contactMechId"))) {
        phone.setId((String) phoneMap.get("contactMechId"));
      }
      phoneList.add(phone);
    }
    return phoneList;
  }

  public static Phone preparePhoneModel(Map phoneMap) {
    Phone phone = new Phone();
    if (UtilValidate.isNotEmpty(phoneMap.get("contactNumber"))) {
      phone.setPhone((String) phoneMap.get("contactNumber"));
    }
    if (UtilValidate.isNotEmpty(phoneMap.get("countryCode"))) {
      phone.setCountryCode((String) phoneMap.get("countryCode"));
    }
    if (UtilValidate.isNotEmpty(phoneMap.get("areaCode"))) {
      phone.setAreaCode((String) phoneMap.get("areaCode"));
    }
    if (UtilValidate.isNotEmpty(phoneMap.get("contactMechId"))) {
      phone.setId((String) phoneMap.get("contactMechId"));
    }
    if (UtilValidate.isNotEmpty(phoneMap.get("regionCode"))) {
      phone.setRegionCode((String) phoneMap.get("regionCode"));
    }
    if (UtilValidate.isNotEmpty(phoneMap.get("phoneFormatted"))) {
      phone.setPhoneFormatted((String) phoneMap.get("phoneFormatted"));
    }

    return phone;
  }
}
