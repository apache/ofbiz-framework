package com.simbaquartz.xparty.services.location;

import com.simbaquartz.xparty.ContactMethodTypesEnum;
import com.simbaquartz.xparty.helpers.PartyPostalAddressHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

/** Exposes all postal address related services. */
public class PostalAddressServices {
  private static final String module = PostalAddressServices.class.getName();

  /**
   * Returns the postal address based on the input contactMechId.
   *
   * @param dctx
   * @param context
   * @return
   */
  public Map<String, Object> mmoGetPartyAddressDetails(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> serviceResult = new HashMap<>();
    Delegator delegator = dctx.getDelegator();

    String contactMechId = (String) context.get("contactMechId");
    Map address = new HashMap<>();

    Map postalAddress =
        PartyPostalAddressHelper.getPartyAddress(null, null, contactMechId, delegator);

    if (UtilValidate.isEmpty(postalAddress)) {
      String errorMessage = "A valid postal address for the input id was not found.";
      return ServiceUtil.returnError(errorMessage);
    }

    serviceResult.put("addressDetails", address);
    return serviceResult;
  }

  public Map<String, Object> axGetPartyAddresses(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> serviceResult = new HashMap<>();
    Delegator delegator = dctx.getDelegator();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    LocalDispatcher dispatcher = dctx.getDispatcher();

    String partyId = (String) context.get("partyId");

    GenericValue addressContactMech = null;
    List<GenericValue> partyContactMechs = new ArrayList<>();

    try {
      partyContactMechs = EntityQuery.use(delegator).from("PartyContactMech").queryList();
    } catch (GenericEntityException e) {
      Debug.logError(e.getMessage(), module);
      return ServiceUtil.returnError(e.getMessage());
    }

    GenericValue postalAddress = null;
    List addresses = new ArrayList<>();

    for (GenericValue partyContactMech : partyContactMechs) {
      String partyContactMechId = partyContactMech.getString("contactMechId");
      try {
        addressContactMech =
            EntityQuery.use(delegator)
                .from("ContactMech")
                .where("contactMechId", partyContactMechId, "contactMechTypeId", "POSTAL_ADDRESS")
                .queryOne();

        if (UtilValidate.isNotEmpty(addressContactMech)) {
          Map axGetAddressCtx =
              UtilMisc.toMap(
                  "contactMechId", partyContactMechId,
                  "userLogin", userLogin);

          Map axGetAddressResponse = null;
          try {
            axGetAddressResponse = dispatcher.runSync("mmoGetPartyAddressDetails", axGetAddressCtx);
          } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
          }
          if (ServiceUtil.isError(axGetAddressResponse)) {
            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(axGetAddressResponse));
          }
          Map addressDetails = (Map) axGetAddressResponse.get("addressDetails");

          addresses.add(addressDetails);
        }
      } catch (GenericEntityException e) {
        Debug.logError("There was a problem with the addresses \n" + e.getMessage(), module);
        return ServiceUtil.returnError(e.getMessage());
      }
    }
    serviceResult.put("addresses", addresses);
    return serviceResult;
  }

  /**
   * Extended create postal address service with support for geo location meta data, also allows an
   * address to be created by using country only. Expects at least a countryGeoId as mandatory
   * input. Expects at least a countryGeoId as mandatory input. Honors input party id, if provided
   * sets the address as users' primary address (PRIMARY_LOCATION) if purposeTypeId is empty, else
   * uses the input purposeTypeId.
   *
   * <p>Also enriches the input postal address via Google Places API adding static map image,
   * formatted address, lattitude, longitude etc.
   *
   * @param dctx
   * @param context
   * @return
   */
  public static Map<String, Object> extCreatePostalAddress(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    LocalDispatcher dispatcher = dctx.getDispatcher();

    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String toName = (String) context.get("toName");
    String attnName = (String) context.get("attnName");
    String address1 = (String) context.get("address1");
    String address2 = (String) context.get("address2");
    String city = (String) context.get("city");
    String postalCode = (String) context.get("postalCode");
    String stateProvinceGeoId = (String) context.get("stateProvinceGeoId");
    String countryGeoId = (String) context.get("countryGeoId");
    String building = (String) context.get("building");
    String room = (String) context.get("room");
    String apartment = (String) context.get("apartment");
    String entryCode = (String) context.get("entryCode");

    String formattedAddress = (String) context.get("formattedAddress");
    String googlePlaceId = (String) context.get("googlePlaceId");
    String googleUrl = (String) context.get("googleUrl");
    String googleData = (String) context.get("googleData");
    String staticMapUrl = (String) context.get("staticMapUrl");
    String staticMapUrl2 = (String) context.get("staticMapUrl2");
    Double latitude = (Double) context.get("latitude");
    Double longitude = (Double) context.get("longitude");
    String timeZoneId = (String) context.get("timeZoneId");

    GenericValue postalAddress;
    String contactMechId;
    try {
      Map<String, Object> createContactMechCtx = UtilMisc.toMap("userLogin", userLogin,
              "contactMechTypeId", ContactMethodTypesEnum.ADDRESS.getTypeId());
      createContactMechCtx.put("createdByUserLogin", userLogin.get("userLoginId"));
      createContactMechCtx.put("lastModifiedByUserLogin", userLogin.get("userLoginId"));
      createContactMechCtx.put("createdDate", UtilDateTime.nowTimestamp());
      createContactMechCtx.put("lastModifiedDate", UtilDateTime.nowTimestamp());

      Map createContactMechResponse =
          dispatcher.runSync("createContactMech",createContactMechCtx);
      contactMechId = (String) createContactMechResponse.get("contactMechId");

      postalAddress = delegator.makeValue("PostalAddress");
      postalAddress.set("contactMechId", contactMechId);

      if (UtilValidate.isNotEmpty(toName)) {
        postalAddress.set("toName", toName);
      }
      if (UtilValidate.isNotEmpty(attnName)) {
        postalAddress.set("attnName", attnName);
      }
      if (UtilValidate.isNotEmpty(address1)) {
        postalAddress.set("address1", address1);
      }
      if (UtilValidate.isNotEmpty(address2)) {
        postalAddress.set("address2", address2);
      }
      if (UtilValidate.isNotEmpty(city)) {
        postalAddress.set("city", city);
      }
      if (UtilValidate.isNotEmpty(postalCode)) {
        postalAddress.set("postalCode", postalCode);
      }
      if (UtilValidate.isNotEmpty(stateProvinceGeoId)) {
        postalAddress.set("stateProvinceGeoId", stateProvinceGeoId);
      }
      if (UtilValidate.isNotEmpty(countryGeoId)) {
        postalAddress.set("countryGeoId", countryGeoId);
      }
      if (UtilValidate.isNotEmpty(building)) {
        postalAddress.set("building", building);
      }
      if (UtilValidate.isNotEmpty(room)) {
        postalAddress.set("room", room);
      }
      if (UtilValidate.isNotEmpty(apartment)) {
        postalAddress.set("apartment", apartment);
      }
      if (UtilValidate.isNotEmpty(entryCode)) {
        postalAddress.set("entryCode", entryCode);
      }
      if (UtilValidate.isNotEmpty(googlePlaceId)) {
        postalAddress.set("googlePlaceId", googlePlaceId);
      }
      if (UtilValidate.isNotEmpty(formattedAddress)) {
        postalAddress.set("formattedAddress", formattedAddress);
      }
      if (UtilValidate.isNotEmpty(googleUrl)) {
        postalAddress.set("googleUrl", googleUrl);
      }
      if (UtilValidate.isNotEmpty(googleData)) {
        postalAddress.set("googleData", googleData);
      }
      if (UtilValidate.isNotEmpty(staticMapUrl)) {
        postalAddress.set("staticMapUrl", staticMapUrl);
      }
      if (UtilValidate.isNotEmpty(staticMapUrl2)) {
        postalAddress.set("staticMapUrl2", staticMapUrl2);
      }
      if (UtilValidate.isNotEmpty(latitude)) {
        postalAddress.set("latitude", latitude);
      }
      if (UtilValidate.isNotEmpty(longitude)) {
        postalAddress.set("longitude", longitude);
      }
      if (UtilValidate.isNotEmpty(timeZoneId)) {
        postalAddress.set("timeZoneId", timeZoneId);
      }
      postalAddress.create();

      // enrich the postal address
      dispatcher.runSync(
          "enrichGeoDetailsForPostalAddress",
          UtilMisc.toMap("contactMechId", contactMechId, "userLogin", userLogin));

    } catch (Exception e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }

    serviceResult.put("createdPostalAddress", postalAddress);
    serviceResult.put("contactMechId", contactMechId);

    return serviceResult;
  }

  public static Map<String, Object> extCreatePartyPostalAddress(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    LocalDispatcher dispatcher = dctx.getDispatcher();

    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String toName = (String) context.get("toName");
    String attnName = (String) context.get("attnName");
    String address1 = (String) context.get("address1");
    String address2 = (String) context.get("address2");
    String city = (String) context.get("city");
    String postalCode = (String) context.get("postalCode");
    String stateProvinceGeoId = (String) context.get("stateProvinceGeoId");
    String countryGeoId = (String) context.get("countryGeoId");
    String building = (String) context.get("building");
    String room = (String) context.get("room");
    String apartment = (String) context.get("apartment");
    String entryCode = (String) context.get("entryCode");

    String formattedAddress = (String) context.get("formattedAddress");
    String googlePlaceId = (String) context.get("googlePlaceId");
    String googleUrl = (String) context.get("googleUrl");
    String googleData = (String) context.get("googleData");
    String staticMapUrl = (String) context.get("staticMapUrl");
    String staticMapUrl2 = (String) context.get("staticMapUrl2");
    Double latitude = (Double) context.get("latitude");
    Double longitude = (Double) context.get("longitude");
    String timeZoneId = (String) context.get("timeZoneId");

    String partyId = (String) context.get("partyId");
    String purposeTypeId = (String) context.get("purposeTypeId");

    String contactMechId;
    GenericValue createdPostalAddress;
    try {
      Map postalAddressCtx =
          UtilMisc.toMap(
              "toName",
              toName,
              "countryGeoId",
              countryGeoId,
              "attnName",
              attnName,
              "address1",
              address1,
              "address2",
              address2,
              "city",
              city,
              "stateProvinceGeoId",
              stateProvinceGeoId,
              "countryGeoId",
              countryGeoId,
              "building",
              building,
              "room",
              room,
              "apartment",
              apartment,
              "entryCode",
              entryCode,
              "googlePlaceId",
              googlePlaceId,
              "formattedAddress",
              formattedAddress,
              "googleUrl",
              googleUrl,
              "googleData",
              googleData,
              "staticMapUrl",
              staticMapUrl,
              "staticMapUrl2",
              staticMapUrl2,
              "latitude",
              latitude,
              "longitude",
              longitude,
              "timeZoneId",
              timeZoneId,
              "userLogin",
              userLogin);

      // create the postal address
      Map extCreatePostalAddressResponse =
          dispatcher.runSync("extCreatePostalAddress", postalAddressCtx);

      contactMechId = (String) extCreatePostalAddressResponse.get("contactMechId");
      createdPostalAddress =
          (GenericValue) extCreatePostalAddressResponse.get("createdPostalAddress");

      dispatcher.runSync(
          "createPartyContactMech",
          UtilMisc.toMap(
              "userLogin", userLogin,
              "partyId", partyId,
              "contactMechId", contactMechId,
              "contactMechTypeId", ContactMethodTypesEnum.ADDRESS.getTypeId()));

      // set purpose type id for the address, defaults to primary
      String addressLabel = "other";
      if (purposeTypeId.equals(PostalAddressTypesEnum.PRIMARY.getTypeId())) {
        addressLabel = PostalAddressTypesEnum.PRIMARY.getLabel();
      }

      dispatcher.runSync(
          "createPartyContactMechPurpose",
          UtilMisc.toMap(
              "userLogin", userLogin,
              "partyId", partyId,
              "contactMechId", contactMechId,
              "contactMechPurposeTypeId", purposeTypeId,
              "label", addressLabel));

      Map populateTimezoneForGeoLocationResponse =
          dispatcher.runSync(
              "populateTimezoneForGeoLocation",
              UtilMisc.toMap(
                  "userLogin", userLogin,
                  "partyId", partyId,
                  "contactMechId", contactMechId));

      if (!ServiceUtil.isError(populateTimezoneForGeoLocationResponse)) {
        createdPostalAddress =
            (GenericValue) populateTimezoneForGeoLocationResponse.get("updatedPostalAddress");
        String postalAddressTimezoneId = createdPostalAddress.getString("timeZoneId");
        if (UtilValidate.isNotEmpty(postalAddressTimezoneId)) {
          // set party timezone

          dispatcher.runSync(
              "setPartyTimezone",
              UtilMisc.toMap(
                  "partyId",
                  partyId,
                  "timezoneId",
                  postalAddressTimezoneId,
                  "userLogin",
                  userLogin));
        }
      }

    } catch (Exception e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }

    Debug.log("Created party #" + partyId + " postal address # " + contactMechId, module);

    serviceResult.put("createdPostalAddress", createdPostalAddress);
    serviceResult.put("contactMechId", contactMechId);

    return serviceResult;
  }

  /**
   * Updates the postal address, also sets the timezone of the party
   *
   * @param dctx
   * @param context
   * @return
   */
  public static Map<String, Object> extUpdatePostalAddress(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    LocalDispatcher dispatcher = dctx.getDispatcher();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String contactMechId = (String) context.get("contactMechId");
    String toName = (String) context.get("toName");
    String attnName = (String) context.get("attnName");
    String address1 = (String) context.get("address1");
    String address2 = (String) context.get("address2");
    String city = (String) context.get("city");
    String postalCode = (String) context.get("postalCode");
    String stateProvinceGeoId = (String) context.get("stateProvinceGeoId");
    String countryGeoId = (String) context.get("countryGeoId");
    String building = (String) context.get("building");
    String room = (String) context.get("room");
    String apartment = (String) context.get("apartment");
    String entryCode = (String) context.get("entryCode");

    String googlePlaceId = (String) context.get("googlePlaceId");
    String formattedAddress = (String) context.get("formattedAddress");
    String googleUrl = (String) context.get("googleUrl");
    String googleData = (String) context.get("googleData");
    String staticMapUrl = (String) context.get("staticMapUrl");
    String staticMapUrl2 = (String) context.get("staticMapUrl2");
    Double latitude = (Double) context.get("latitude");
    Double longitude = (Double) context.get("longitude");
    String timeZoneId = (String) context.get("timeZoneId");

    GenericValue postalAddress;
    try {
      postalAddress =
          EntityQuery.use(delegator)
              .from("PostalAddress")
              .where("contactMechId", contactMechId)
              .queryOne();
      if (UtilValidate.isNotEmpty(postalAddress)) {
        if (UtilValidate.isNotEmpty(toName)) {
          postalAddress.set("toName", toName);
        }
        if (UtilValidate.isNotEmpty(attnName)) {
          postalAddress.set("attnName", attnName);
        }
        if (UtilValidate.isNotEmpty(address1)) {
          postalAddress.set("address1", address1);
        }
        if (UtilValidate.isNotEmpty(address2)) {
          postalAddress.set("address2", address2);
        }
        if (UtilValidate.isNotEmpty(city)) {
          postalAddress.set("city", city);
        }
        if (UtilValidate.isNotEmpty(postalCode)) {
          postalAddress.set("postalCode", postalCode);
        }
        if (UtilValidate.isNotEmpty(stateProvinceGeoId)) {
          postalAddress.set("stateProvinceGeoId", stateProvinceGeoId);
        }
        if (UtilValidate.isNotEmpty(countryGeoId)) {
          postalAddress.set("countryGeoId", countryGeoId);
        }
        if (UtilValidate.isNotEmpty(building)) {
          postalAddress.set("building", building);
        }
        if (UtilValidate.isNotEmpty(room)) {
          postalAddress.set("room", room);
        }
        if (UtilValidate.isNotEmpty(apartment)) {
          postalAddress.set("apartment", apartment);
        }
        if (UtilValidate.isNotEmpty(entryCode)) {
          postalAddress.set("entryCode", entryCode);
        }
        if (UtilValidate.isNotEmpty(googlePlaceId)) {
          postalAddress.set("googlePlaceId", googlePlaceId);
        }
        if (UtilValidate.isNotEmpty(formattedAddress)) {
          postalAddress.set("formattedAddress", formattedAddress);
        }
        if (UtilValidate.isNotEmpty(googleUrl)) {
          postalAddress.set("googleUrl", googleUrl);
        }
        if (UtilValidate.isNotEmpty(googleData)) {
          postalAddress.set("googleData", googleData);
        }
        if (UtilValidate.isNotEmpty(staticMapUrl)) {
          postalAddress.set("staticMapUrl", staticMapUrl);
        }
        if (UtilValidate.isNotEmpty(staticMapUrl2)) {
          postalAddress.set("staticMapUrl2", staticMapUrl2);
        }
        if (UtilValidate.isNotEmpty(latitude)) {
          postalAddress.set("latitude", latitude);
        }
        if (UtilValidate.isNotEmpty(longitude)) {
          postalAddress.set("longitude", longitude);
        }
        if (UtilValidate.isNotEmpty(timeZoneId)) {
          postalAddress.set("timeZoneId", timeZoneId);
        }
        postalAddress.store();

      }
    } catch (Exception e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }

    serviceResult.put("updatedPostalAddress", postalAddress);

    return serviceResult;
  }
}
