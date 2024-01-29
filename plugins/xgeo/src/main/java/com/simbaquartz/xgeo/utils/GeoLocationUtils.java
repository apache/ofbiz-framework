package com.simbaquartz.xgeo.utils;

import com.fidelissd.zcp.xcommon.models.geo.PostalAddress;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;

public class GeoLocationUtils {
  private static final String module = GeoLocationUtils.class.getName();

  /**
   * Expects a PostalAddress record.
   *
   * @param postalAddress
   * @return Formatted string of the postal address.
   */
  public static String formattedPostalAddress(GenericValue postalAddress) {
    String resp = postalAddress.getString("formattedAddress");
    return UtilValidate.isNotEmpty(resp) ? resp : formatPostalAddress(postalAddress);
  }

  /**
   * Generates navigatable google map url that can be opened in a browser window.
   *
   * @param latitude
   * @param longitude
   * @param placeId
   * @return
   */
  public static String buildGoogleMapsUrl(Double latitude, Double longitude, String placeId) {
    String baseUrl = "https://www.google.com/maps/search/?api=1&";
    return baseUrl + "query=" + latitude + "," + longitude + "&query_place_id=" + placeId;
  }

  /**
   * Generates static map image url that can then be downloaded and saved.
   *
   * @param latitude (Latitude of the place)
   * @param longitude (Longitude of the place)
   * @param placeId (Optional place id from google)
   * @param apiKey (Google api key)
   * @param optionalImageDimensions (Example 640x640 width times height, max 640)
   * @return
   */
  public static String buildGoogleMapsStaticImageUrl(
      Double latitude,
      Double longitude,
      String placeId,
      String apiKey,
      String optionalImageDimensions) {
    String imageDimensions640x640 = "640x640";
    String imageDimensions640x240 = "640x240"; // for wider and narrow aspect ratios

    if (UtilValidate.isEmpty(optionalImageDimensions))
      optionalImageDimensions = imageDimensions640x640;

    String latAndLng = latitude + "," + longitude;
    String baseUrl =
        "https://maps.googleapis.com/maps/api/staticmap?zoom=13&size="
            + optionalImageDimensions
            + "&scale=2&center=";
    return baseUrl + latAndLng + "&key=" + apiKey;
  }

  public static String buildGoogleMapsStaticImageUrl(
      Double latitude, Double longitude, String placeId, String apiKey) {
    return buildGoogleMapsStaticImageUrl(latitude, longitude, placeId, apiKey, null);
  }

  /**
   * Returns string representation of the address object. Populates Address modal and uses its
   * formatter.
   *
   * @param postalAddress GenericValue of type PostalAddress
   * @return
   */
  public static String formatPostalAddress(GenericValue postalAddress) {
    PostalAddress address = new PostalAddress();

    address.setId(postalAddress.getString("contactMechId"));
    address.setAddressLine1(postalAddress.getString("address1"));
    address.setAddressLine2(postalAddress.getString("address2"));
    address.setCity(postalAddress.getString("city"));
    address.setPostalCode(postalAddress.getString("postalCode"));
    address.setStateCode(postalAddress.getString("stateProvinceGeoId"));
    address.setCountryCode(postalAddress.getString("countryGeoId"));

    // populate state and country name
    try {
      GenericValue stateRecord = postalAddress.getRelatedOne("StateProvinceGeo", true);
      if (UtilValidate.isNotEmpty(stateRecord)) {
        address.setStateName(stateRecord.getString("geoName"));
      }

      GenericValue countryRecord = postalAddress.getRelatedOne("CountryGeo", true);
      if (UtilValidate.isNotEmpty(countryRecord)) {
        address.setCountryName(countryRecord.getString("geoName"));
      }
    } catch (GenericEntityException e) {
      Debug.logError(e, module);
    }

    return address.getFormattedAddressString();
  }
}
