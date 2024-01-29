package com.simbaquartz.xgeo.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;

/** Geo (Location, Postal Address etc.) related utility functions. */
public class GeoUtil {
  private static final String module = GeoUtil.class.getName();

  /**
   * Returns the name of the State/Province/Territory.
   *
   * @param postalAddress
   * @return
   */
  public static String getStateProvinceName(GenericValue postalAddress) {
    String stateProvinceName = "";
    if (UtilValidate.isEmpty(postalAddress.getEntityName())) {
      Debug.logWarning("Invalid (empty) record provided. Returning nothing.", module);
      return stateProvinceName;
    }
    if (!"PostalAddress".equalsIgnoreCase(postalAddress.getEntityName())) {
      Debug.logWarning(
          "Invalid entity type provided. Returning nothing. Only PostalAddress is allowed, you provided: "
              + postalAddress.getEntityName(),
          module);
      return stateProvinceName;
    }

    String stateProvinceGeoId = postalAddress.getString("stateProvinceGeoId");
    if (UtilValidate.isNotEmpty(stateProvinceGeoId)) {
      try {
        GenericValue stateProvinceGeo = postalAddress.getRelatedOne("StateProvinceGeo", true);
        if (UtilValidate.isNotEmpty(stateProvinceGeo)) {
          stateProvinceName = stateProvinceGeo.getString("geoName");
        }
      } catch (GenericEntityException e) {
        Debug.logError(e, module);
      }
    }

    return stateProvinceName;
  }

  /**
   * Returns the name of the Country.
   *
   * @param postalAddress
   * @return
   */
  public static String getCountryName(GenericValue postalAddress) {
    String countryName = "";
    if (UtilValidate.isEmpty(postalAddress.getEntityName())) {
      Debug.logWarning("Invalid (empty) record provided. Returning nothing.", module);
      return countryName;
    }
    if (!"PostalAddress".equalsIgnoreCase(postalAddress.getEntityName())) {
      Debug.logWarning(
          "Invalid entity type provided. Returning nothing. Only PostalAddress is allowed, you provided: "
              + postalAddress.getEntityName(),
          module);
      return countryName;
    }

    String countryGeoId = postalAddress.getString("countryGeoId");
    if (UtilValidate.isNotEmpty(countryGeoId)) {
      try {
        GenericValue countryGeo = postalAddress.getRelatedOne("CountryGeo", true);
        if (UtilValidate.isNotEmpty(countryGeo)) {
          countryName = countryGeo.getString("geoName");
        }
      } catch (GenericEntityException e) {
        Debug.logError(e, module);
      }
    }

    return countryName;
  }

  /**
   * Returns the name of the Country by the geoCode
   *
   * @param geoId the countryCode
   * @return countryName
   */
  public static String getCountryName(String geoId, Delegator delegator) {
    String countryName = "";
    GenericValue countryGeo = getCountryGeo(geoId, delegator);
    if (UtilValidate.isNotEmpty(countryGeo)) {
      countryName = countryGeo.getString("geoName");
    }
    return countryName;
  }

  /**
   * Returns the name of the Country by the geoCode
   *
   * @param geoId the countryCode
   * @return countryName
   */
  public static GenericValue getCountryGeo(String geoId,Delegator delegator) {
    GenericValue countryGeo = null;
    try {
      countryGeo =
          EntityQuery.use(delegator)
              .from("Geo")
              .where("geoTypeId", "COUNTRY", "geoId", geoId)
              .cache(true)
              .queryFirst();
    } catch (GenericEntityException e) {
      Debug.logError(e, module);
    }
    return countryGeo;
  }

  /**
   * Returns the ISO code of a Country by the geoCode
   *
   * @param geoId the countryCode
   * @return countryName
   */
  public static String getCountryGeoIsoCode(String geoId, Delegator delegator) {
    GenericValue countryGeo = getCountryGeo(geoId, delegator);

    if (UtilValidate.isNotEmpty(countryGeo)) {
      return countryGeo.getString("geoCode");
    }
    return "";
  }

  /**
   * Returns the name of the State/province by the geoCode
   *
   * @param geoId the countryCode
   * @return countryName
   */
  public static GenericValue getStateOrProvinceGeo(String geoId,Delegator delegator) {
    GenericValue stateGeo = null;
    try {
      stateGeo =
          EntityQuery.use(delegator)
              .from("Geo")
              .where("geoTypeId", "STATE", "geoId", geoId)
              .cache(true)
              .queryFirst();
    } catch (GenericEntityException e) {
      Debug.logError(e, module);
    }
    return stateGeo;
  }


  /**
   * Returns the ISO code of a State/Province by the geoCode
   *
   * @param geoId the countryCode
   * @return countryName
   */
  public static String getStateOrProvinceGeoIsoCode(String geoId, Delegator delegator) {
    GenericValue countryGeo = getStateOrProvinceGeo(geoId, delegator);

    if (UtilValidate.isNotEmpty(countryGeo)) {
      return countryGeo.getString("geoCode");
    }
    return "";
  }

  /**
   * Returns the name of the city.
   *
   * @param postalAddress
   * @return
   */
  public static String getCityName(GenericValue postalAddress) {
    String cityName = "";
    if (UtilValidate.isEmpty(postalAddress.getEntityName())) {
      Debug.logWarning("Invalid (empty) record provided. Returning nothing.", module);
      return cityName;
    }
    if (!"PostalAddress".equalsIgnoreCase(postalAddress.getEntityName())) {
      Debug.logWarning(
          "Invalid entity type provided. Returning nothing. Only PostalAddress is allowed, you provided: "
              + postalAddress.getEntityName(),
          module);
      return cityName;
    }

    String city = postalAddress.getString("city");
    if (UtilValidate.isNotEmpty(city)) {
      cityName = city;
    }

    return cityName;
  }
  public static Map<String, Object> getTimeZoneObjectMapById(String timeZoneId) {
    Map<String, Object> timeZoneMap = new HashMap<>();
    int displayStyle = java.util.TimeZone.LONG;
    Locale locale = Locale.getDefault();
    TimeZone timezone = UtilDateTime.toTimeZone(timeZoneId);
    String name = timezone.getDisplayName(timezone.useDaylightTime(), displayStyle, locale);
    timeZoneMap.put("id", timezone.getID());
    timeZoneMap.put("name", name);
    timeZoneMap.put("dstOffset", timezone.getDSTSavings());
    timeZoneMap.put("rawOffset", timezone.getRawOffset());
    Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"),
        Locale.getDefault());
    Date currentLocalTime = calendar.getTime();
    DateFormat date = new SimpleDateFormat("z", Locale.getDefault());
    String gmtOffset = displayTimeZone(timezone);//timezone.getDisplayName(false, TimeZone.SHORT);//date.format(currentLocalTime);//Returns the time zone offset like this: GMT+05:30
    timeZoneMap.put("gmtOffset", gmtOffset);
    int gmtOffsetInHours = (int) TimeUnit.MILLISECONDS.toHours(timezone.getOffset(currentLocalTime.getTime()));
    timeZoneMap.put("gmtOffsetInHours", gmtOffsetInHours);
    String zoneId = timezone.toZoneId().getId();
    String cityName = zoneId.substring(zoneId.lastIndexOf("/") + 1, zoneId.length()).replaceAll("_", " ");
    String timezoneCode = timezone.getDisplayName(false, TimeZone.SHORT);//returns short code, PST, IST etc.
    timeZoneMap.put("code", timezoneCode);
    String formattedTimezoneName = "(" + gmtOffset + ") " + timezone.getDisplayName(false, TimeZone.LONG);
    if (UtilValidate.isNotEmpty(cityName)) {
      formattedTimezoneName = formattedTimezoneName + " - " + cityName;
    }
    timeZoneMap.put("formattedName", formattedTimezoneName);
    return timeZoneMap;
  }

  /**
   * Returns the format GMT+05:30
   * @param tz
   * @return
   */
  private static String displayTimeZone(TimeZone tz) {
    String result;
    long hours = TimeUnit.MILLISECONDS.toHours(tz.getRawOffset());
    long minutes = TimeUnit.MILLISECONDS.toMinutes(tz.getRawOffset())
        - TimeUnit.HOURS.toMinutes(hours);
    // avoid -4:-30 issue
    minutes = Math.abs(minutes);
    if (hours >= 0) {
      result = String.format("GMT+%02d:%02d", hours, minutes);
    } else {
      result = String.format("GMT%02d:%02d", hours, minutes);
    }
    return result;
  }

}
