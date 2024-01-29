package com.simbaquartz.xgeo.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.maps.DirectionsApi;
import com.google.maps.DistanceMatrixApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.PlaceAutocompleteRequest;
import com.google.maps.PlaceDetailsRequest;
import com.google.maps.TimeZoneApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DirectionsLeg;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.Distance;
import com.google.maps.model.DistanceMatrix;
import com.google.maps.model.DistanceMatrixElementStatus;
import com.google.maps.model.DistanceMatrixRow;
import com.google.maps.model.Duration;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;
import com.google.maps.model.PlaceDetails;
import com.google.maps.model.TravelMode;
import com.google.maps.model.Unit;
import com.fidelissd.zcp.xcommon.util.AppConfigUtil;
import com.fidelissd.zcp.xcommon.util.AppConfigUtil.ApplicationPreferences;
import com.simbaquartz.xgeo.utils.GeoLocationUtils;
import com.fidelissd.zcp.xcommon.util.hierarchy.HierarchyUtils;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.TimeZone;
import org.apache.commons.io.FileUtils;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * Offers geoCoding related apis. Reference: https://github.com/googlemaps/google-maps-services-java
 */
public class GeoLocationServices {
  private static final String module = GeoLocationServices.class.getName();

  /**
   * Enriches the details of a postal address by adding, lat/long details. Also generates and
   * downloads static map of the location from google maps api.
   *
   * @param dctx
   * @param context
   * @return
   */
  public static Map<String, Object> enrichGeoDetailsForPostalAddress(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    LocalDispatcher dispatcher = dctx.getDispatcher();

    String contactMechId = (String) context.get("contactMechId");

    String googleApiKey =
        AppConfigUtil.getAppPreference(delegator, ApplicationPreferences.GOOGLE_API_KEY);
    if (UtilValidate.isEmpty(googleApiKey)) {
      String errorMessage = "Google API Key is missing, please configure the API key first.";
      Debug.logError(errorMessage, module);
      return ServiceUtil.returnError(errorMessage);
    }

    GeoApiContext geoApiContext = new GeoApiContext.Builder().apiKey(googleApiKey).build();

    // String exampleAddress = "123, Queen Street,Melbourne, Victoria,3000"; // use it for testing.
    try {
      GenericValue postalAddress =
          EntityQuery.use(delegator)
              .from("PostalAddress")
              .where("contactMechId", contactMechId)
              .queryOne();

      LatLng addressLatitudeLongitude;
      Double latitude = 0d;
      Double longitude = 0d;
      String googlePlaceId = "";
      String formattedAddress = "";
      String adrAddress = "";
      String googleData = "";

      Gson gson = new GsonBuilder().setPrettyPrinting().create();
      // check for place id first
      String placeId = postalAddress.getString("googlePlaceId");
      if (UtilValidate.isNotEmpty(placeId)) {
        googlePlaceId = placeId;
        PlaceDetails placeDetails = placeDetails(geoApiContext, placeId).await();
        addressLatitudeLongitude = placeDetails.geometry.location;
        latitude = addressLatitudeLongitude.lat;
        longitude = addressLatitudeLongitude.lng;
        googlePlaceId = placeId;
        formattedAddress = placeDetails.formattedAddress;
        adrAddress = placeDetails.adrAddress;
        googleData = gson.toJson(placeDetails);
      } else {
        String addressToEnrich = GeoLocationUtils.formatPostalAddress(postalAddress);
        GeocodingResult[] results = GeocodingApi.geocode(geoApiContext, addressToEnrich).await();
        // Geocoding response sample
        // https://developers.google.com/maps/documentation/geocoding/overview#GeocodingResponses
        if (results.length > 0) {
          GeocodingResult geocodingResult = results[0];
          addressLatitudeLongitude = geocodingResult.geometry.location;
          latitude = addressLatitudeLongitude.lat;
          longitude = addressLatitudeLongitude.lng;
          googlePlaceId = geocodingResult.placeId;
          formattedAddress = geocodingResult.formattedAddress;
          googleData = gson.toJson(geocodingResult);
        }
      }

      String googleUrl = GeoLocationUtils.buildGoogleMapsUrl(latitude, longitude, googlePlaceId);

      // download and save the image.
      String staticMapImageUrlFullWidth =
          GeoLocationUtils.buildGoogleMapsStaticImageUrl(
              latitude, longitude, googlePlaceId, googleApiKey);

      // 640x640 dimensions
      String staticMapFileName = contactMechId + "-" + System.currentTimeMillis() + "-640x640.png";
      String toFile =
          System.getProperty("ofbiz.home")
              + "/plugins/xstatic/webapp/xstatic/static/pub/geo/"
              + staticMapFileName;

      // 640x240 dimensions
      String staticMapImageUrlWider =
          GeoLocationUtils.buildGoogleMapsStaticImageUrl(
              latitude, longitude, googlePlaceId, googleApiKey, "640x240");
      String staticMapFileNameWider =
          contactMechId + "-" + System.currentTimeMillis() + "-640x240.png";
      String toFileWider =
          System.getProperty("ofbiz.home")
              + "/plugins/xstatic/webapp/xstatic/static/pub/geo/"
              + staticMapFileNameWider;

      try {
        // connectionTimeout, readTimeout = 10 seconds
        FileUtils.copyURLToFile(
            new URL(staticMapImageUrlFullWidth), new File(toFile), 10000, 10000);
        FileUtils.copyURLToFile(
            new URL(staticMapImageUrlWider), new File(toFileWider), 10000, 10000);
      } catch (IOException e) {
        Debug.logError(e, module);
        return ServiceUtil.returnError(e.getMessage());
      }

      String cdnUrlPrefix =
          UtilProperties.getPropertyValue("url.properties", "content.url.prefix.secure");

      String staticMapCdnUrl = cdnUrlPrefix + "/pub/geo/" + staticMapFileName;
      String staticMapCdnUrl2 = cdnUrlPrefix + "/pub/geo/" + staticMapFileNameWider;

      Debug.logInfo("Found the address latitude as: " + latitude, module);
      Debug.logInfo("Found the address longitude as: " + longitude, module);

      // check and update the postal address.
      GenericValue sysUserLogin = HierarchyUtils.getSysUserLogin(delegator);
      Map updatePostalAddressCtx =
          UtilMisc.toMap(
              "userLogin", sysUserLogin,
              "contactMechId", contactMechId,
              "formattedAddress", formattedAddress,
              "adrAddress", adrAddress,
              "googleUrl", googleUrl,
              "googleData", googleData,
              "staticMapUrl", staticMapCdnUrl,
              "staticMapUrl2", staticMapCdnUrl2,
              "latitude", latitude,
              "longitude", longitude);

      Map updateAddressResponse =
          dispatcher.runSync("extUpdatePostalAddress", updatePostalAddressCtx);

      GenericValue updatedPostalAddress =
          (GenericValue) updateAddressResponse.get("updatedPostalAddress");
      serviceResult.put("updatedPostalAddress", updatedPostalAddress);
    } catch (ApiException e) {
      Debug.logInfo("Using the google api key: " + googleApiKey, module);
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    } catch (InterruptedException e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    } catch (IOException e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    } catch (GenericEntityException e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    } catch (GenericServiceException e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }

    return serviceResult;
  }

  /**
   * Requests the details of a Place.
   *
   * <p>We are only enabling looking up Places by placeId as the older Place identifier, reference,
   * is deprecated. Please see the <a
   * href="https://web.archive.org/web/20170521070241/https://developers.google.com/places/web-service/details#deprecation">
   * deprecation warning</a>.
   *
   * @param context The context on which to make Geo API requests.
   * @param placeId The PlaceID to request details on.
   * @return Returns a PlaceDetailsRequest that you can configure and execute.
   */
  public static PlaceDetailsRequest placeDetails(GeoApiContext context, String placeId) {
    PlaceDetailsRequest request = new PlaceDetailsRequest(context);
    request.placeId(placeId);
    return request;
  }

  /**
   * Requests the details of a Place.
   *
   * <p>We are only enabling looking up Places by placeId as the older Place identifier, reference,
   * is deprecated. Please see the <a
   * href="https://web.archive.org/web/20170521070241/https://developers.google.com/places/web-service/details#deprecation">
   * deprecation warning</a>.
   *
   * @param context The context on which to make Geo API requests.
   * @param placeId The PlaceID to request details on.
   * @param sessionToken The Session Token for this request.
   * @return Returns a PlaceDetailsRequest that you can configure and execute.
   */
  public static PlaceDetailsRequest placeDetails(
      GeoApiContext context, String placeId, PlaceAutocompleteRequest.SessionToken sessionToken) {
    PlaceDetailsRequest request = new PlaceDetailsRequest(context);
    request.placeId(placeId);
    request.sessionToken(sessionToken);
    return request;
  }

  private static Map enrichPostalAddress(
      String contactMechId, LocalDispatcher dispatcher, Delegator delegator)
      throws GenericServiceException {
    Map enrichGeoDetailsForPostalAddressResponse =
        dispatcher.runSync(
            "enrichGeoDetailsForPostalAddress",
            UtilMisc.toMap(
                "contactMechId",
                contactMechId,
                "userLogin",
                HierarchyUtils.getSysUserLogin(delegator)));

    return enrichGeoDetailsForPostalAddressResponse;
  }
  /**
   * Retrieves the timezone based on lat/long for a location (PostalAddress) and saves in the postal
   * address.
   *
   * @param dctx
   * @param context
   * @return
   */
  public static Map<String, Object> populateTimezoneForGeoLocation(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    LocalDispatcher dispatcher = dctx.getDispatcher();

    String contactMechId = (String) context.get("contactMechId");

    String googleApiKey =
        AppConfigUtil.getAppPreference(delegator, ApplicationPreferences.GOOGLE_API_KEY);
    if (UtilValidate.isEmpty(googleApiKey)) {
      String errorMessage = "Google API Key is missing, please configure the API key first.";
      Debug.logError(errorMessage, module);
      return ServiceUtil.returnError(errorMessage);
    }

    GeoApiContext geoApiContext = new GeoApiContext.Builder().apiKey(googleApiKey).build();

    // String exampleAddress = "123, Queen Street,Melbourne, Victoria,3000"; // use it for testing.
    try {
      GenericValue postalAddress =
          EntityQuery.use(delegator)
              .from("PostalAddress")
              .where("contactMechId", contactMechId)
              .queryOne();
      Double addressLat = 0D;
      Double addressLng = 0D;

      if (UtilValidate.isNotEmpty(postalAddress)) {
        // check to make sure it has co-ordinates
        addressLat = postalAddress.getDouble("latitude");
        addressLng = postalAddress.getDouble("longitude");
        if (UtilValidate.isEmpty(addressLat)) {
          // details not available, try enriching the postal address.
          Map enrichGeoDetailsForPostalAddressResponse =
              enrichPostalAddress(contactMechId, dispatcher, delegator);
          GenericValue updatedOriginPostalAddress =
              (GenericValue) enrichGeoDetailsForPostalAddressResponse.get("updatedPostalAddress");
          addressLat = updatedOriginPostalAddress.getDouble("latitude");
          addressLng = updatedOriginPostalAddress.getDouble("longitude");
        }
      }

      TimeZone addressTimezone =
          TimeZoneApi.getTimeZone(geoApiContext, new LatLng(addressLat, addressLng)).await();

      Debug.logInfo(
          "Found timezone for postal address as: "
              + addressTimezone.getID()
              + "("
              + addressTimezone.getDisplayName()
              + ")",
          module);

      // update the postal address timezone
      GenericValue sysUserLogin = HierarchyUtils.getSysUserLogin(delegator);
      Map updatePostalAddressCtx =
          UtilMisc.toMap(
              "userLogin", sysUserLogin,
              "contactMechId", contactMechId,
              "timeZoneId", addressTimezone.getID());

      Map updateAddressResponse =
          dispatcher.runSync("extUpdatePostalAddress", updatePostalAddressCtx);

      GenericValue updatedPostalAddress =
          (GenericValue) updateAddressResponse.get("updatedPostalAddress");

      serviceResult.put("updatedPostalAddress", updatedPostalAddress);
    } catch (Exception e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }

    return serviceResult;
  }

  /**
   * Returns the distance between two locations. Ref:
   * https://github.com/googlemaps/google-maps-services-java/blob/master/src/test/java/com/google/maps/DistanceMatrixApiTest.java
   *
   * @param dctx
   * @param context
   * @return
   */
  public static Map<String, Object> calculateDistanceBetweenLocations(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    LocalDispatcher dispatcher = dctx.getDispatcher();

    // Origin / destiination latitude and longitude values.
    Double originLat = (Double) context.get("originLat");
    Double originLong = (Double) context.get("originLong");

    Double destinationLat = (Double) context.get("destinationLat");
    Double destinationLong = (Double) context.get("destinationLong");

    // metric or imperial
    // Reference:
    // https://ceskdata.com/cost-data-differences-between-the-imperial-and-metric-knowledgebases#:~:text=Units%20of%20measurement&text=Whereas%20most%20countries%20use%20the,feet%2C%20inches%2C%20and%20pounds.
    String unitsType = (String) context.get("unitsType");

    Unit unit = Unit.IMPERIAL;

    if (Unit.METRIC.toString().equalsIgnoreCase(unitsType)) {
      unit = Unit.METRIC;
    }

    String originPostalAddressContactMechId =
        (String) context.get("originPostalAddressContactMechId");
    String destinationPostalAddressContactMechId =
        (String) context.get("destinationPostalAddressContactMechId");

    boolean hasOriginAndDestinationCoordinates = false;

    if (UtilValidate.isNotEmpty(originLat) && UtilValidate.isNotEmpty(originLong)) {
      if (UtilValidate.isNotEmpty(destinationLat) && UtilValidate.isNotEmpty(destinationLong)) {
        hasOriginAndDestinationCoordinates = true;
      }
    }

    boolean hasOriginAndDestinationPostalAddressIds = false;

    if (UtilValidate.isNotEmpty(originPostalAddressContactMechId)
        && UtilValidate.isNotEmpty(destinationPostalAddressContactMechId)) {
      hasOriginAndDestinationPostalAddressIds = true;
    }

    if (!hasOriginAndDestinationCoordinates && !hasOriginAndDestinationPostalAddressIds) {
      return ServiceUtil.returnError(
          "At least one of lat/long or postal address contact mech ids is required.");
    }

    String googleApiKey =
        AppConfigUtil.getAppPreference(delegator, ApplicationPreferences.GOOGLE_API_KEY);
    if (UtilValidate.isEmpty(googleApiKey)) {
      String errorMessage = "Google API Key is missing, please configure the API key first.";
      Debug.logError(errorMessage, module);
      return ServiceUtil.returnError(errorMessage);
    }

    GeoApiContext geoApiContext = new GeoApiContext.Builder().apiKey(googleApiKey).build();
    DistanceMatrixApiRequest request = new DistanceMatrixApiRequest(geoApiContext);

    String distanceHumanReadable = "Not found";
    long distanceInMeters = 0l;

    String durationHumanReadable = "Not found";
    long durationInSeconds = 0l;
    try {

      if (hasOriginAndDestinationPostalAddressIds && !hasOriginAndDestinationCoordinates) {
        // prepare origin co-ordinates
        GenericValue originPostalAddress =
            EntityQuery.use(delegator)
                .from("PostalAddress")
                .where("contactMechId", originPostalAddressContactMechId)
                .queryOne();
        if (UtilValidate.isNotEmpty(originPostalAddress)) {
          // check to make sure it has co-ordinates
          Double originAddressLat = originPostalAddress.getDouble("latitude");
          Double originAddressLng = originPostalAddress.getDouble("longitude");
          if (UtilValidate.isEmpty(originAddressLat)) {
            // details not available, try enriching the postal address.
            Map enrichGeoDetailsForPostalAddressResponse =
                enrichPostalAddress(originPostalAddressContactMechId, dispatcher, delegator);

            GenericValue updatedOriginPostalAddress =
                (GenericValue) enrichGeoDetailsForPostalAddressResponse.get("updatedPostalAddress");
            originAddressLat = updatedOriginPostalAddress.getDouble("latitude");
            originAddressLng = updatedOriginPostalAddress.getDouble("longitude");
          }

          originLat = originAddressLat;
          originLong = originAddressLng;
        }

        // prepare destination co-ordinates
        GenericValue destinationPostalAddress =
            EntityQuery.use(delegator)
                .from("PostalAddress")
                .where("contactMechId", destinationPostalAddressContactMechId)
                .queryOne();
        if (UtilValidate.isNotEmpty(destinationPostalAddress)) {
          // check to make sure it has co-ordinates
          Double destinationAddressLat = destinationPostalAddress.getDouble("latitude");
          Double destinationAddressLng = destinationPostalAddress.getDouble("longitude");
          if (UtilValidate.isEmpty(destinationAddressLat)) {
            // details not available, try enriching the postal address.
            Map enrichGeoDetailsForPostalAddressResponse =
                enrichPostalAddress(destinationPostalAddressContactMechId, dispatcher, delegator);
            GenericValue updatedOriginPostalAddress =
                (GenericValue) enrichGeoDetailsForPostalAddressResponse.get("updatedPostalAddress");
            destinationAddressLat = updatedOriginPostalAddress.getDouble("latitude");
            destinationAddressLng = updatedOriginPostalAddress.getDouble("longitude");
          }

          destinationLat = destinationAddressLat;
          destinationLong = destinationAddressLng;
        }
      }

      //    String exampleAddress = "123, Queen Street,Melbourne, Victoria,3000"; // use it for
      // testing.
      DistanceMatrix trix =
          request
              .origins(new LatLng(originLat, originLong))
              .destinations(new LatLng(destinationLat, destinationLong))
              .units(unit)
              .awaitIgnoreError();

      DistanceMatrixRow responseRows = trix.rows[0];
      DistanceMatrixElementStatus status = responseRows.elements[0].status;

      if (DistanceMatrixElementStatus.OK.equals(status)) {
        Distance distance = responseRows.elements[0].distance;
        distanceHumanReadable = distance.humanReadable;
        distanceInMeters = distance.inMeters;

        Duration duration = responseRows.elements[0].duration;
        durationHumanReadable = duration.humanReadable;
        durationInSeconds = duration.inSeconds;
      } else {
        Debug.logWarning(
            "Could not calculate the distance, API returned response code : " + status, module);
      }
    } catch (Exception e) {
      Debug.logError(e, module);
      ServiceUtil.returnError(e.getMessage());
    }

    serviceResult.put("distanceHumanReadable", distanceHumanReadable);
    serviceResult.put("distanceInMeters", distanceInMeters);

    serviceResult.put("durationHumanReadable", durationHumanReadable);
    serviceResult.put("durationInSeconds", durationInSeconds);

    return serviceResult;
  }

  /**
   * Returns the directions between two locations. Ref:
   * https://github.com/googlemaps/google-maps-services-java/blob/master/src/test/java/com/google/maps/DirectionsApiTest.java.
   *
   * <p>Calls directions api to fetch the route path then prepares and downloads static map url
   * image.
   *
   * @param dctx
   * @param context
   * @return
   */
  public static Map<String, Object> getDirectionsBetweenTwoGeoPoints(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    LocalDispatcher dispatcher = dctx.getDispatcher();

    // Origin / destiination latitude and longitude values.
    Double originLat = (Double) context.get("originLat");
    Double originLong = (Double) context.get("originLong");

    Double destinationLat = (Double) context.get("destinationLat");
    Double destinationLong = (Double) context.get("destinationLong");
    String travelModeText = (String) context.get("travelMode");

    // defaults to driving, others are

    // make api call to get directions
    // https://maps.googleapis.com/maps/api/directions/json?origin=-37.8157528,144.9607103&destination=-27.5695533,153.0341796&mode=driving&key=AIzaSyBWNPdHPBzg9FH6U7TXZgrlYq1YU4nu4ls
    String googleApiKey =
        AppConfigUtil.getAppPreference(delegator, ApplicationPreferences.GOOGLE_API_KEY);
    GeoApiContext geoApiContext = new GeoApiContext.Builder().apiKey(googleApiKey).build();

    String originCoordinates = originLat + "," + originLong;
    String destinationCoordinates = destinationLat + "," + destinationLong;

    TravelMode travelMode;
    if (TravelMode.BICYCLING.toString().equalsIgnoreCase(travelModeText)) {
      travelMode = TravelMode.BICYCLING;
    } else if (TravelMode.WALKING.toString().equalsIgnoreCase(travelModeText)) {
      travelMode = TravelMode.WALKING;
    } else if (TravelMode.TRANSIT.toString().equalsIgnoreCase(travelModeText)) {
      travelMode = TravelMode.TRANSIT;
    } else if (TravelMode.DRIVING.toString().equalsIgnoreCase(travelModeText)) {
      travelMode = TravelMode.DRIVING;
    } else {
      travelMode = TravelMode.UNKNOWN;
    }

    String staticMapCdnUrl = "";

    try {
      DirectionsResult result =
          DirectionsApi.getDirections(geoApiContext, originCoordinates, destinationCoordinates)
              .mode(travelMode)
              .await();

      // See the sample response here
      // https://maps.googleapis.com/maps/api/directions/json?origin=-37.8157528,144.9607103&destination=-27.5695533,153.0341796&mode=driving&key=AIzaSyBWNPdHPBzg9FH6U7TXZgrlYq1YU4nu4ls
      if (UtilValidate.isNotEmpty(result.routes) && result.routes.length > 0) {
        DirectionsRoute directionsRoute = result.routes[0];
        String routePathEncoded = directionsRoute.overviewPolyline.getEncodedPath();
        DirectionsLeg directionsLeg = directionsRoute.legs[0]; // get the first leg

        String endAddress = directionsLeg.endAddress;
        Double endLocationLat = directionsLeg.endLocation.lat;
        Double endLocationLng = directionsLeg.endLocation.lng;

        String startAddress = directionsLeg.startAddress;
        Double startLocationLat = directionsLeg.startLocation.lat;
        Double startLocationLng = directionsLeg.startLocation.lng;

        // markers
        String startCoordinates = startLocationLat + "," + startLocationLng;
        String originMarker = "markers=size:large|color:orange|label:P|" + startCoordinates;
        String endCoordinates = endLocationLat + "," + endLocationLng;
        String destinationMarker = "markers=size:large|color:green|label:D|" + endCoordinates;

        String routePathConfig = "path=color:blue|weight:2|enc:" + routePathEncoded;

        String baseStaticMapUrl =
            "https://maps.googleapis.com/maps/api/staticmap?size=640x640&scale=2&key="
                + googleApiKey;

        String staticMapWithMarkersAndRoutePathUrl =
            baseStaticMapUrl + "&" + routePathConfig + "&" + originMarker + "&" + destinationMarker;

        Debug.logInfo(
            "Generated Google Maps Static URL : " + staticMapWithMarkersAndRoutePathUrl, module);

        String staticMapFileName = "DRCTNS-" + System.currentTimeMillis() + ".png";
        String toFile =
            System.getProperty("ofbiz.home")
                + "/plugins/xstatic/webapp/xstatic/static/pub/geo/"
                + staticMapFileName;
        try {
          // connectionTimeout, readTimeout = 10 seconds
          FileUtils.copyURLToFile(
              new URL(staticMapWithMarkersAndRoutePathUrl), new File(toFile), 10000, 10000);
        } catch (IOException e) {
          Debug.logError(e, module);
          return ServiceUtil.returnError(e.getMessage());
        }

        staticMapCdnUrl =
            UtilProperties.getPropertyValue("url.properties", "content.url.prefix.secure")
                + "/pub/geo/"
                + staticMapFileName;

        Debug.logInfo("Generated Maps URL : " + staticMapCdnUrl, module);
      }
    } catch (ApiException e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    } catch (InterruptedException e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    } catch (IOException e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }

    serviceResult.put("staticMapCdnUrl", staticMapCdnUrl);

    return serviceResult;
  }
}
