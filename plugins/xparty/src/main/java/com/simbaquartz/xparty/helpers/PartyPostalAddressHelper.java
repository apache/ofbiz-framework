package com.simbaquartz.xparty.helpers;

import com.simbaquartz.xgeo.utils.GeoUtil;
import com.simbaquartz.xparty.ContactMethodTypesEnum;
import com.simbaquartz.xparty.services.location.PostalAddressTypesEnum;
import java.util.Map;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;

/**
 * Party Postal address helper. Offers party's location related services, primary location, timezone
 * etc..
 */
public class PartyPostalAddressHelper {

  public static final String module = PartyPostalAddressHelper.class.getName();

  /**
   * Returns the default location for a party. Uses PRIMARY_LOCATION to fetch the primary postal
   * address and related details.
   *
   * @param partyId
   * @param delegator
   * @return
   */
  public static Map getPrimaryAddress(String partyId, Delegator delegator) {
    return getPartyAddress(partyId, PostalAddressTypesEnum.PRIMARY, null, delegator);
  }

  public static Map getPrimaryAddressById(String addressId, Delegator delegator) {
    return getPartyAddress(null, PostalAddressTypesEnum.PRIMARY, addressId, delegator);
  }

  /**
   * Returns a postal address record based on input criteria for the input party .
   *
   * @param partyId optional.
   * @param addressTypesEnum optional
   * @param addressId optional
   * @param delegator required
   * @return
   */
  public static Map getPartyAddress(
      String partyId,
      PostalAddressTypesEnum addressTypesEnum,
      String addressId,
      Delegator delegator) {
    try {
      Map searchCriteria =
          UtilMisc.toMap("contactMechTypeId", ContactMethodTypesEnum.ADDRESS.getTypeId());

      if (UtilValidate.isNotEmpty(partyId)) {
        searchCriteria.put("partyId", partyId);
      }
      if (UtilValidate.isNotEmpty(addressTypesEnum)) {
        searchCriteria.put("contactMechPurposeTypeId", addressTypesEnum.getTypeId());
      }
      if (UtilValidate.isNotEmpty(addressId)) {
        searchCriteria.put("contactMechId", addressId);
      }

      GenericValue primaryPostalAddress =
          EntityQuery.use(delegator)
              .from("PartyContactDetailByPurpose")
              .where(searchCriteria)
              .orderBy("-fromDate")
              .filterByDate("fromDate", "thruDate", "purposeFromDate", "purposeThruDate")
              .queryFirst();

      if (UtilValidate.isNotEmpty(primaryPostalAddress)) {
        String countryGeoId = primaryPostalAddress.getString("countryGeoId");
        GenericValue countryGeo = GeoUtil.getCountryGeo(countryGeoId, delegator);
        String countryGeoCode = "";
        String countryGeoName = "";
        String countryAbbreviation = "";
        if (UtilValidate.isNotEmpty(countryGeo)) {
          countryGeoCode = countryGeo.getString("geoCode");
          countryGeoName = countryGeo.getString("geoName");
          countryAbbreviation = countryGeo.getString("abbreviation");
        }

        String stateGeoId = primaryPostalAddress.getString("stateProvinceGeoId");
        GenericValue stateGeo = GeoUtil.getCountryGeo(stateGeoId, delegator);
        String stateGeoCode = "";
        String stateGeoName = "";
        String stateAbbreviation = "";
        if (UtilValidate.isNotEmpty(stateGeo)) {
          stateGeoCode = stateGeo.getString("geoCode");
          stateGeoName = stateGeo.getString("geoName");
          stateAbbreviation = stateGeo.getString("abbreviation");
        }

        Map defaultLocation =
            UtilMisc.toMap(
                "contactMechId",
                primaryPostalAddress.getString("contactMechId"),
                "toName",
                primaryPostalAddress.getString("toName"),
                "attnName",
                primaryPostalAddress.getString("attnName"),
                "address1",
                primaryPostalAddress.getString("address1"),
                "address2",
                primaryPostalAddress.getString("address2"),
                "city",
                primaryPostalAddress.getString("city"),
                "stateProvinceGeoId",
                primaryPostalAddress.getString("stateProvinceGeoId"),
                "stateName",
                stateGeoName,
                "stateCode",
                stateGeoCode,
                "stateAbbr",
                stateAbbreviation,
                "stateAbbreviation",
                stateAbbreviation,
                "countryGeoId",
                countryGeoId,
                "countryGeoCode",
                countryGeoCode,
                "countryName",
                countryGeoName,
                "countryAbbr",
                countryAbbreviation,
                "countryAbbreviation",
                countryAbbreviation,
                "postalCode",
                primaryPostalAddress.getString("postalCode"),
                "building",
                primaryPostalAddress.getString("building"),
                "room",
                primaryPostalAddress.getString("room"),
                "apartment",
                primaryPostalAddress.getString("apartment"),
                "entryCode",
                primaryPostalAddress.getString("entryCode"),
                "googlePlaceId",
                primaryPostalAddress.getString("googlePlaceId"),
                "formattedAddress",
                primaryPostalAddress.getString("formattedAddress"),
                "adrAddress",
                primaryPostalAddress.getString("adrAddress"),
                "googleUrl",
                primaryPostalAddress.getString("googleUrl"),
                "staticMapUrl",
                primaryPostalAddress.getString("staticMapUrl"),
                "staticMapUrl2",
                primaryPostalAddress.getString("staticMapUrl2"),
                "latitude",
                primaryPostalAddress.getDouble("latitude"),
                "longitude",
                primaryPostalAddress.getDouble("longitude"),
                "timeZoneId",
                primaryPostalAddress.getString("timeZoneId"),
                "directions",
                primaryPostalAddress.getString("directions"));

        return defaultLocation;
      }
    } catch (GenericEntityException e) {
      Debug.logError(e, module);
    }

    return null;
  }
}
