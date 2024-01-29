package com.fidelissd.zcp.xcommon.models.geo.builder;

import com.fidelissd.zcp.xcommon.enums.PostalAddressTypesEnum;
import com.fidelissd.zcp.xcommon.models.geo.TimeZoneList;
import com.fidelissd.zcp.xcommon.models.geo.PostalAddress;
import com.fidelissd.zcp.xcommon.models.geo.Timezone;
import java.util.HashMap;
import java.util.Map;

import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.GenericValue;

public class GeoModelBuilder {
  private static final String module = GeoModelBuilder.class.getName();

  public static Timezone prepareTimezoneModel(Map timezoneMap) {
    Timezone timezone = new Timezone();
    if (UtilValidate.isNotEmpty(timezoneMap)) {
      if (UtilValidate.isNotEmpty(timezoneMap.get("id"))) {
        timezone.setId((String) timezoneMap.get("id"));
      }
      if (UtilValidate.isNotEmpty(timezoneMap.get("name"))) {
        timezone.setName((String) timezoneMap.get("name"));
      }
      if (UtilValidate.isNotEmpty(timezoneMap.get("dstOffset"))) {
        timezone.setDaylightSavingsTimeOffset((int) timezoneMap.get("dstOffset"));
      }
      if (UtilValidate.isNotEmpty(timezoneMap.get("rawOffset"))) {
        timezone.setRawOffset((int) timezoneMap.get("rawOffset"));
      }
      if (UtilValidate.isNotEmpty(timezoneMap.get("gmtOffset"))) {
        timezone.setGmtOffset((String) timezoneMap.get("gmtOffset"));
      }
      if (UtilValidate.isNotEmpty(timezoneMap.get("gmtOffsetInHours"))) {
        timezone.setGmtOffsetInHours((int) timezoneMap.get("gmtOffsetInHours"));
      }
      if (UtilValidate.isNotEmpty(timezoneMap.get("code"))) {
        timezone.setCode((String) timezoneMap.get("code"));
      }
      if (UtilValidate.isNotEmpty(timezoneMap.get("formattedName"))) {
        timezone.setFormattedName((String) timezoneMap.get("formattedName"));
      }
    }
    return timezone;
  }

  public static PostalAddress buildPostalAddress(Map postalAddress) {
    PostalAddress address = new PostalAddress();
    if (UtilValidate.isNotEmpty(postalAddress)) {
      if (UtilValidate.isNotEmpty(postalAddress.get("contactMechId"))) {
        address.setId((String) postalAddress.get("contactMechId"));
      }
      if (UtilValidate.isNotEmpty(postalAddress.get("toName"))) {
        address.setToName((String) postalAddress.get("toName"));
      }
      if (UtilValidate.isNotEmpty(postalAddress.get("attnName"))) {
        address.setAttnName((String) postalAddress.get("attnName"));
      }
      if (UtilValidate.isNotEmpty(postalAddress.get("address1"))) {
        address.setAddressLine1((String) postalAddress.get("address1"));
      }
      if (UtilValidate.isNotEmpty(postalAddress.get("address2"))) {
        address.setAddressLine2((String) postalAddress.get("address2"));
      }
      if (UtilValidate.isNotEmpty(postalAddress.get("city"))) {
        address.setCity((String) postalAddress.get("city"));
      }
      if (UtilValidate.isNotEmpty(postalAddress.get("stateProvinceGeoId"))) {
        address.setStateCode((String) postalAddress.get("stateProvinceGeoId"));
      }
      if (UtilValidate.isNotEmpty(postalAddress.get("stateCode"))) {
        address.setStateCode((String) postalAddress.get("stateCode"));
      }
      if (UtilValidate.isNotEmpty(postalAddress.get("stateName"))) {
        address.setStateName((String) postalAddress.get("stateName"));
      }
      if (UtilValidate.isNotEmpty(postalAddress.get("stateAbbr"))) {
        address.setStateAbbr((String) postalAddress.get("stateAbbr"));
      }
      if (UtilValidate.isNotEmpty(postalAddress.get("countryName"))) {
        address.setCountryName((String) postalAddress.get("countryName"));
      }
      if (UtilValidate.isNotEmpty(postalAddress.get("countryAbbr"))) {
        address.setCountryAbbr((String) postalAddress.get("countryAbbr"));
      }
      if (UtilValidate.isNotEmpty(postalAddress.get("countryGeoId"))) {
        address.setCountryCode((String) postalAddress.get("countryGeoId"));
      }
      if (UtilValidate.isNotEmpty(postalAddress.get("countryGeoCode"))) {
        address.setCountryIsoCode((String) postalAddress.get("countryGeoCode"));
      }
      if (UtilValidate.isNotEmpty(postalAddress.get("postalCode"))) {
        address.setPostalCode((String) postalAddress.get("postalCode"));
      }
      if (UtilValidate.isNotEmpty(postalAddress.get("formattedAddress"))) {
        address.setFormattedAddress((String) postalAddress.get("formattedAddress"));
      }
      if (UtilValidate.isNotEmpty(postalAddress.get("adrAddress"))) {
        address.setAdrAddress((String) postalAddress.get("adrAddress"));
      }
      if (UtilValidate.isNotEmpty(postalAddress.get("googleUrl"))) {
        address.setGoogleUrl((String) postalAddress.get("googleUrl"));
      }
      if (UtilValidate.isNotEmpty(postalAddress.get("staticMapUrl"))) {
        address.setStaticMapUrl((String) postalAddress.get("staticMapUrl"));
      }
      if (UtilValidate.isNotEmpty(postalAddress.get("staticMapUrl2"))) {
        address.setStaticMapUrl2((String) postalAddress.get("staticMapUrl2"));
      }
      if (UtilValidate.isNotEmpty(postalAddress.get("latitude"))) {
        address.setLatitude((Double) postalAddress.get("latitude"));
      }
      if (UtilValidate.isNotEmpty(postalAddress.get("longitude"))) {
        address.setLongitude((Double) postalAddress.get("longitude"));
      }
      if (UtilValidate.isNotEmpty(postalAddress.get("timezone"))) {
        Map<String, Object> timeZoneMap = UtilGenerics.toMap(postalAddress.get("timezone"));
        address.setTimezone(prepareTimezoneModel(timeZoneMap));
      }
      String addressTimeZoneId = (String) postalAddress.get("timeZoneId");
      if (UtilValidate.isNotEmpty(addressTimeZoneId)) {
        // prepare the timezone response bean
        Timezone timezone = TimeZoneList.getTimezoneModalUsingId(addressTimeZoneId);
        address.setTimezone(timezone);
      }
    }
    return address;
  }

  public static PostalAddress buildPostalAddress(GenericValue postalAddress) {
    PostalAddress address = new PostalAddress();
    if (UtilValidate.isNotEmpty(postalAddress)) {
      if (UtilValidate.isNotEmpty(postalAddress.get("contactMechId"))) {
        address.setId(postalAddress.getString("contactMechId"));
      }
      if (UtilValidate.isNotEmpty(postalAddress.get("toName"))) {
        address.setToName(postalAddress.getString("toName"));
      }
      if (UtilValidate.isNotEmpty(postalAddress.get("attnName"))) {
        address.setAttnName(postalAddress.getString("attnName"));
      }
      if (UtilValidate.isNotEmpty(postalAddress.get("address1"))) {
        address.setAddressLine1(postalAddress.getString("address1"));
      }
      if (UtilValidate.isNotEmpty(postalAddress.get("address2"))) {
        address.setAddressLine2(postalAddress.getString("address2"));
      }
      if (UtilValidate.isNotEmpty(postalAddress.get("city"))) {
        address.setCity(postalAddress.getString("city"));
      }
      if (UtilValidate.isNotEmpty(postalAddress.get("stateProvinceGeoId"))) {
        address.setStateCode(postalAddress.getString("stateProvinceGeoId"));
      }
      if (UtilValidate.isNotEmpty(postalAddress.get("countryGeoId"))) {
        address.setCountryCode(postalAddress.getString("countryGeoId"));
      }
      if (UtilValidate.isNotEmpty(postalAddress.get("postalCode"))) {
        address.setPostalCode(postalAddress.getString("postalCode"));
      }
      if (UtilValidate.isNotEmpty(postalAddress.get("formattedAddress"))) {
        address.setFormattedAddress(postalAddress.getString("formattedAddress"));
      }
      if (UtilValidate.isNotEmpty(postalAddress.get("adrAddress"))) {
        address.setAdrAddress(postalAddress.getString("adrAddress"));
      }
      if (UtilValidate.isNotEmpty(postalAddress.get("googleUrl"))) {
        address.setGoogleUrl(postalAddress.getString("googleUrl"));
      }
      if (UtilValidate.isNotEmpty(postalAddress.get("staticMapUrl"))) {
        address.setStaticMapUrl(postalAddress.getString("staticMapUrl"));
      }
      if (UtilValidate.isNotEmpty(postalAddress.get("latitude"))) {
        address.setLatitude(postalAddress.getDouble("latitude"));
      }
      if (UtilValidate.isNotEmpty(postalAddress.get("longitude"))) {
        address.setLongitude(postalAddress.getDouble("longitude"));
      }
      if (UtilValidate.isNotEmpty(postalAddress.get("timeZoneId"))) {
        address.setTimezoneId(postalAddress.getString("timeZoneId"));
      }
    }
    return address;
  }

  /**
   * Prepares a Map that can be used to invoke service 'extCreatePartyPostalAddress' Example usage:
   * <code>
   * Map<String, Object> addrMap = PostalAddressApiHelper.preparePostalAddressMap(address);
   * addrMap.put("userLogin", userLogin);
   * addrMap.put("partyId", partyId);
   *
   * // invoke the create address service
   * Map<String, Object> addrResp = FastMap.newInstance();
   * try {
   * addrResp = tenantDispatcher.runSync("extCreatePartyPostalAddress", addrMap);
   * } catch (GenericServiceException ex) {
   * Debug.logError(ex, module);
   * }
   * </code>
   *
   * @param address
   * @return
   */
  public static Map buildPostalAddressMap(PostalAddress address) {
    Map<String, Object> addressMap = new HashMap<>();
    if (UtilValidate.isNotEmpty(address.getToName())) {
      addressMap.put("toName", address.getToName());
    }
    if (UtilValidate.isNotEmpty(address.getAttnName())) {
      addressMap.put("attnName", address.getAttnName());
    }
    if (UtilValidate.isNotEmpty(address.getAddressLine1())) {
      addressMap.put("address1", address.getAddressLine1());
    }
    if (UtilValidate.isNotEmpty(address.getAddressLine2())) {
      addressMap.put("address2", address.getAddressLine2());
    }
    if (UtilValidate.isNotEmpty(address.getCity())) {
      addressMap.put("city", address.getCity());
    }
    if (UtilValidate.isNotEmpty(address.getStateCode())) {
      addressMap.put("stateProvinceGeoId", address.getStateCode());
    }
    if (UtilValidate.isNotEmpty(address.getCountryCode())) {
      addressMap.put("countryGeoId", address.getCountryCode());
    }
    if (UtilValidate.isNotEmpty(address.getPostalCode())) {
      addressMap.put("postalCode", address.getPostalCode());
    }
    if (UtilValidate.isNotEmpty(address.getBuilding())) {
      addressMap.put("building", address.getBuilding());
    }
    if (UtilValidate.isNotEmpty(address.getRoom())) {
      addressMap.put("room", address.getRoom());
    }
    if (UtilValidate.isNotEmpty(address.getApartment())) {
      addressMap.put("apartment", address.getApartment());
    }
    if (UtilValidate.isNotEmpty(address.getEntryCode())) {
      addressMap.put("entryCode", address.getEntryCode());
    }
    if (UtilValidate.isNotEmpty(address.getDirections())) {
      addressMap.put("directions", address.getDirections());
    }
    if (UtilValidate.isNotEmpty(address.getGooglePlaceId())) {
      addressMap.put("googlePlaceId", address.getGooglePlaceId());
    }
    if (UtilValidate.isNotEmpty(address.getFormattedAddress())) {
      addressMap.put("formattedAddress", address.getFormattedAddress());
    }
    if (UtilValidate.isNotEmpty(address.getGoogleUrl())) {
      addressMap.put("googleUrl", address.getGoogleUrl());
    }
    if (UtilValidate.isNotEmpty(address.getGoogleData())) {
      addressMap.put("googleData", address.getGoogleData());
    }
    if (UtilValidate.isNotEmpty(address.getStaticMapUrl())) {
      addressMap.put("staticMapUrl", address.getStaticMapUrl());
    }
    if (UtilValidate.isNotEmpty(address.getStaticMapUrl2())) {
      addressMap.put("staticMapUrl2", address.getStaticMapUrl2());
    }
    if (UtilValidate.isNotEmpty(address.getLatitude())) {
      addressMap.put("latitude", address.getLatitude());
    }
    if (UtilValidate.isNotEmpty(address.getLongitude())) {
      addressMap.put("longitude", address.getLongitude());
    }
    if (UtilValidate.isNotEmpty(address.getTimezoneId())) {
      addressMap.put("timeZoneId", address.getTimezoneId());
    }

    addressMap.put("addressPurposes", UtilMisc.toList(address.getAddressPurposes()));

    String purposeTypeId = PostalAddressTypesEnum.PRIMARY.getTypeId();
    String label = PostalAddressTypesEnum.PRIMARY.getLabel();
    if (UtilValidate.isNotEmpty(address.getLabel())) {
      label = address.getLabel();
    }
    if (PostalAddressTypesEnum.HOME.getLabel().equalsIgnoreCase(label)) {
      purposeTypeId = PostalAddressTypesEnum.PRIMARY.getTypeId();
    } else if (PostalAddressTypesEnum.WORK.getLabel().equalsIgnoreCase(label)) {
      purposeTypeId = PostalAddressTypesEnum.WORK.getTypeId();
    } else if (PostalAddressTypesEnum.PRIMARY.getLabel().equalsIgnoreCase(label)) {
      purposeTypeId = PostalAddressTypesEnum.PRIMARY.getTypeId();
    }

    addressMap.put("contactMechPurposeTypeId", purposeTypeId);
    addressMap.put("label", label);

    return addressMap;
  }

}
