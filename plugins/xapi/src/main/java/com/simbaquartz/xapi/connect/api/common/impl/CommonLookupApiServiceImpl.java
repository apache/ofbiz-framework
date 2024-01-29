package com.simbaquartz.xapi.connect.api.common.impl;

import static com.simbaquartz.xapi.connect.api.BaseApiService.delegator;

import com.fidelissd.zcp.xcommon.models.geo.Province;
import com.simbaquartz.xapi.connect.api.ApiResponseMessage;
import com.simbaquartz.xapi.connect.api.NotFoundException;
import com.simbaquartz.xapi.connect.api.common.CommonLookupApiService;
import com.simbaquartz.xapi.connect.utils.ApiResponseUtil;
import com.simbaquartz.xapi.connect.models.common.Color;
import com.fidelissd.zcp.xcommon.collections.FastList;
import com.fidelissd.zcp.xcommon.collections.FastMap;
import com.fidelissd.zcp.xcommon.models.Phone;
import com.fidelissd.zcp.xcommon.models.geo.Country;
import com.fidelissd.zcp.xcommon.models.geo.PostalAddress;
import com.fidelissd.zcp.xcommon.models.geo.Timezone;
import com.fidelissd.zcp.xcommon.models.search.SearchResults;
import com.fidelissd.zcp.xcommon.util.AxPhoneNumberUtil;
import com.fidelissd.zcp.xcommon.util.ColorUtils;
import com.simbaquartz.xgeo.utils.TimeZoneList;
import com.simbaquartz.xgeo.utils.TimeZoneList.TimeZoneWithDisplayName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.common.CommonWorkers;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.ServiceUtil;

public class CommonLookupApiServiceImpl extends CommonLookupApiService {
  private static final String module = CommonLookupApiServiceImpl.class.getName();

  @Override
  public Response getColorPalette() throws NotFoundException {
    List<Color> colors = new ArrayList<>();
    List<GenericValue> masterColors = ColorUtils.getMasterColors(delegator);
    for (GenericValue masterColor : masterColors) {
      Color color = Color.builder().build();
      color.setId(masterColor.getString("colorId"));
      color.setBackground(masterColor.getString("backgroundColor"));
      color.setForeground(masterColor.getString("foregroundColor"));
      colors.add(color);
    }

    return ApiResponseUtil.prepareOkResponse(colors);
  }

  @Override
  public Response validatePhoneNumber(
      String phoneNumber, String regionCode, SecurityContext securityContext)
      throws NotFoundException {
    Map phoneValidationResponseMap =
        AxPhoneNumberUtil.preparePhoneNumberInfo(phoneNumber, regionCode);

    if (ServiceUtil.isError(phoneValidationResponseMap)) {
      String errorMessage = ServiceUtil.getErrorMessage(phoneValidationResponseMap);
      return ApiResponseUtil.badRequestResponse(errorMessage);
    }

    Phone validatedPhone = new Phone();
    validatedPhone.setCountryCode((String) phoneValidationResponseMap.get("countryCode"));
    validatedPhone.setAreaCode((String) phoneValidationResponseMap.get("areaCode"));
    validatedPhone.setPhone((String) phoneValidationResponseMap.get("contactNumber"));
    validatedPhone.setValidPhone((Boolean) phoneValidationResponseMap.get("isValidNumber"));
    validatedPhone.setLocation((String) phoneValidationResponseMap.get("location"));
    validatedPhone.setRegionCode((String) phoneValidationResponseMap.get("regionCode"));
    validatedPhone.setPhoneType((String) phoneValidationResponseMap.get("type"));
    validatedPhone.setTimeZone((String) phoneValidationResponseMap.get("timeZone"));
    validatedPhone.setPossibleNumber((Boolean) phoneValidationResponseMap.get("isPossibleNumber"));
    validatedPhone.setE164Format((String) phoneValidationResponseMap.get("e164Format"));
    validatedPhone.setNationalFormat((String) phoneValidationResponseMap.get("nationalFormat"));
    validatedPhone.setAsYouTypeFormat((String) phoneValidationResponseMap.get("asYouTypeFormat"));
    validatedPhone.setInternationalFormat(
        (String) phoneValidationResponseMap.get("internationalFormat"));
    validatedPhone.setValidNumberForRegion(
        (Boolean) phoneValidationResponseMap.get("isValidNumberForRegion"));

    return ApiResponseUtil.prepareOkResponse(validatedPhone);
  }

  @Override
  public Response validateAddress(PostalAddress address, SecurityContext securityContext)
      throws NotFoundException {

    Map addressMap = new HashMap<>();

    addressMap.put("postalCode", address.getPostalCode());
    addressMap.put("city", address.getCity());
    addressMap.put("countryGeoId", address.getCountryCode());

    //        Boolean isValid = AvalaraTaxHelper.isValidAddress(delegator, dispatcher,
    // HierarchyUtils.getSysUserLogin(delegator), addressMap, locale);
    //
    //        if(!isValid){
    //            return ApiResponseUtil.badRequestResponse(ApiMessageConstants.MSG_INVALID_ADDRESS
    // + address);
    //        }
    return ApiResponseUtil.prepareOkResponse(address);
  }

  @Override
  public Response getCountryById(String countryId, SecurityContext securityContext)
      throws NotFoundException {
    // do some magic!
    return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
  }

  @Override
  public Response getCountryList(String storeId, SecurityContext securityContext)
      throws NotFoundException {
    if (Debug.verboseOn()) Debug.logVerbose("Entering method getCountryList", module);

    List<Country> countryList = FastList.newInstance();
    try {
      List<GenericValue> countries =
          EntityQuery.use(delegator)
              .from("Geo")
              .where("geoTypeId", "COUNTRY")
              .orderBy("-geoName")
              .cache(true)
              .queryList();
      if (UtilValidate.isNotEmpty(countries)) {
        for (GenericValue country : countries) {
          Map countryDetails = FastMap.newInstance();
          countryDetails.putAll(country);
          Country countryObj = new Country();
          String countryId = (String) countryDetails.get("geoId");
          countryObj.setId((String) countryDetails.get("geoId"));
          countryObj.setCode((String) countryDetails.get("geoCode"));
          countryObj.setName((String) countryDetails.get("geoName"));
          countryList.add(countryObj);

          // get provinces
          List<Province> provinceList = FastList.newInstance();
          List<GenericValue> provinces = CommonWorkers.getAssociatedStateList(delegator, countryId);
          for (GenericValue province : provinces) {
            Province provinceObj = new Province();
            provinceObj.setId(province.getString("geoId"));
            provinceObj.setCountryId(province.getString("geoIdFrom"));
            provinceObj.setCode(province.getString("geoCode"));
            provinceObj.setName(province.getString("geoName"));
            provinceList.add(provinceObj);
          }
          countryObj.setProvinces(provinceList);
        }
      }
    } catch (GenericEntityException e) {
      Debug.logError(e, module);
      if (Debug.verboseOn()) Debug.logVerbose("Exiting method getCountryList", module);

      return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR);
    }
    if (Debug.verboseOn()) Debug.logVerbose("Exiting method getCountryList", module);

    return ApiResponseUtil.prepareOkResponse(countryList);
  }

  // Timezone List
  @Override
  public Response getTimeZoneList(String storeId, SecurityContext securityContext)
      throws NotFoundException {
    if (Debug.verboseOn()) Debug.logVerbose("Entering method getTimeZoneList", module);

    List<TimeZoneWithDisplayName> returnedZones = TimeZoneList.getInstance().getTimeZones();
    List<Timezone> timeZoneList = FastList.newInstance();

    int index = 1;
    for (TimeZoneWithDisplayName zone : returnedZones) {
      Timezone timeZoneModel = zone.getTimeZoneModel();
      timeZoneModel.setIndex(index);
      timeZoneList.add(timeZoneModel);
      index++;
    }

    // sort the list by GMT offset
//    List<Timezone> sortedList =
//        timeZoneList
//            .stream()
//            .sorted(Comparator.comparingInt(Timezone::getGmtOffsetInHours))
//            .collect(Collectors.toList());

    if (Debug.verboseOn()) Debug.logVerbose("Exiting method getTimeZoneList", module);

    return ApiResponseUtil.prepareOkResponse(timeZoneList);
  }

  // Currency List
  @Override
  public Response getCurrencyList(String storeId, SecurityContext securityContext)
      throws NotFoundException {
    if (Debug.verboseOn()) Debug.logVerbose("Entering method getCurrencyList", module);

    List<Map> currencyRecord = FastList.newInstance();

    List<GenericValue> currencyList = null;
    try {
      currencyList =
          EntityQuery.use(delegator)
              .from("Uom")
              .where("uomTypeId", "CURRENCY_MEASURE")
              .cache(true)
              .queryList();
    } catch (GenericEntityException e) {
      Debug.logError(e, module);
    }
    ;
    for (GenericValue currency : currencyList) {
      Map currencyListResponse = FastMap.newInstance();
      currencyListResponse.put("currency_code", currency.getString("abbreviation"));
      currencyListResponse.put("currency_name", currency.getString("description"));
      currencyRecord.add(currencyListResponse);
    }
    if (Debug.verboseOn()) Debug.logVerbose("Exiting method getCurrencyList", module);

    return ApiResponseUtil.prepareOkResponse(currencyRecord);
  }

  // Weight List
  public Response getWeightList(String storeId, SecurityContext securityContext)
      throws NotFoundException {
    if (Debug.verboseOn()) Debug.logVerbose("Entering method getWeightList", module);

    List<Map> weightRecord = FastList.newInstance();

    List<GenericValue> weightList = null;
    try {
      weightList =
          EntityQuery.use(delegator)
              .from("Uom")
              .where("uomTypeId", "WEIGHT_MEASURE")
              .cache(true)
              .queryList();
    } catch (GenericEntityException e) {
      Debug.logError(e, module);
    }
    ;
    for (GenericValue weight : weightList) {
      Map weightListResponse = FastMap.newInstance();
      weightListResponse.put("weight_code", weight.getString("abbreviation"));
      weightListResponse.put("weight_name", weight.getString("description"));
      weightRecord.add(weightListResponse);
    }
    if (Debug.verboseOn()) Debug.logVerbose("Exiting method getWeightList", module);

    return ApiResponseUtil.prepareOkResponse(weightRecord);
  }

  public Response getCountryStateList(String countryGeoId, SecurityContext securityContext) {
    if (Debug.verboseOn()) Debug.logVerbose("Entering method getCountryStateList", module);

    /*if (!StoreHelper.isValidGeoIdByTypeId(delegator, countryGeoId, "COUNTRY")) {
      Debug.logError(
          Response.Status.NOT_FOUND
              + "\n Unable to find the Country Code passed in the Geo Entity.",
          module);
      return ApiResponseUtil.prepareDefaultResponse(
          Response.Status.NOT_FOUND, "Invalid Country Code passed.");
    }*/

    List<GenericValue> countryStates =
        CommonWorkers.getAssociatedStateList(delegator, countryGeoId);

    if (Debug.verboseOn()) Debug.logVerbose("Exiting method getCountryStateList", module);

    return ApiResponseUtil.prepareOkResponse(countryStates);
  }

  // Telephonic Country Code List
  @Override
  public Response getCountryTelephonicCode(SecurityContext securityContext)
      throws NotFoundException {
    if (Debug.verboseOn()) Debug.logVerbose("Entering method getCountryTelephonicCode", module);

    List<Map> countryCodes = FastList.newInstance();
    try {
      String countryName = "";
      List<GenericValue> number =
          EntityQuery.use(delegator).from("CountryTeleCodeAndName").queryList();
      for (GenericValue productStoreCatalog : number) {
        Map geoMap = FastMap.newInstance();
        countryName =
            productStoreCatalog.get("countryCode")
                + " (+"
                + productStoreCatalog.get("teleCode")
                + ")";
        geoMap.put("countryName", countryName);
        geoMap.put("countryCode", productStoreCatalog.get("countryCode"));
        geoMap.put("teleCode", productStoreCatalog.get("teleCode"));
        countryCodes.add(geoMap);
      }
    } catch (GenericEntityException e) {
      // handle error here
      Debug.logError(
          "An error occurred while invoking getCountryTelephonicCode service, details: "
              + e.getMessage(),
          "CommonLookupApiServiceImpl");
      if (Debug.verboseOn()) Debug.logVerbose("Exiting method fetchQuoteRole", module);

      return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR);
    }

    if (Debug.verboseOn()) Debug.logVerbose("Exiting method getCountryTelephonicCode", module);

    return ApiResponseUtil.prepareOkResponse(countryCodes);
  }

  @Override
  public Response getDepartments(SecurityContext securityContext) throws NotFoundException {
    if (Debug.verboseOn()) Debug.logVerbose("Entering method getDepartments", module);

    List<GenericValue> customerDepartmentList = null;
    try {
      customerDepartmentList =
          EntityQuery.use(delegator)
              .select("attrValue")
              .from("PartyAttribute")
              .where("attrName", "Department")
              .distinct()
              .queryList();
    } catch (GenericEntityException e) {
      Debug.logError(e, module);
      if (Debug.verboseOn()) Debug.logVerbose("Exiting method getDepartments", module);

      return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR);
    }

    List<Map<String, Object>> recordList = FastList.newInstance();
    for (GenericValue customerDepartment : customerDepartmentList) {
      String attrValue = customerDepartment.getString("attrValue");
      if (UtilValidate.isNotEmpty(attrValue)) {
        String name = customerDepartment.getString("attrValue");
        Map<String, Object> recordDetails = UtilMisc.toMap("name", name);

        recordList.add(recordDetails);
      }
    }

    return ApiResponseUtil.prepareOkResponse(recordList);
  }

  @Override
  public Response getEmailTypes(SecurityContext securityContext) throws NotFoundException {
    if (Debug.verboseOn()) Debug.logVerbose("Entering method getEmailTypes", module);

    List<GenericValue> emailTypesList = null;
    try {
      emailTypesList =
          EntityQuery.use(delegator)
              .from("ContactMechPurposeType")
              .where("parentTypeId", "APP_EMAIL_TYPES")
              .orderBy("createdStamp")
              .distinct()
              .queryList();
    } catch (GenericEntityException e) {
      Debug.logError(e, module);
      if (Debug.verboseOn()) Debug.logVerbose("Exiting method getEmailTypes", module);

      return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR);
    }

    List<Map<String, Object>> recordList = FastList.newInstance();
    for (GenericValue emailType : emailTypesList) {
      Map<String, Object> recordDetails =
          UtilMisc.toMap(
              "value", emailType.getString("contactMechPurposeTypeId"),
              "description", emailType.getString("description"));

      recordList.add(recordDetails);
    }

    return ApiResponseUtil.prepareOkResponse(recordList);
  }

  @Override
  public Response getPhoneTypes(SecurityContext securityContext) throws NotFoundException {
    if (Debug.verboseOn()) Debug.logVerbose("Entering method getPhoneTypes", module);

    List<GenericValue> emailTypesList = null;
    try {
      emailTypesList =
          EntityQuery.use(delegator)
              .from("ContactMechPurposeType")
              .where("parentTypeId", "APP_PHONE_TYPES")
              .orderBy("createdStamp")
              .distinct()
              .queryList();
    } catch (GenericEntityException e) {
      Debug.logError(e, module);
      if (Debug.verboseOn()) Debug.logVerbose("Exiting method getPhoneTypes", module);

      return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR);
    }

    List<Map<String, Object>> recordList = FastList.newInstance();
    for (GenericValue emailType : emailTypesList) {
      Map<String, Object> recordDetails =
          UtilMisc.toMap(
              "value", emailType.getString("contactMechPurposeTypeId"),
              "description", emailType.getString("description"));

      recordList.add(recordDetails);
    }

    return ApiResponseUtil.prepareOkResponse(recordList);
  }

  @Override
  public Response getIndustryTypes(SecurityContext securityContext) throws NotFoundException {
    if (Debug.verboseOn()) Debug.logVerbose("Entering method getIndustryTypes", module);

    List<GenericValue> records;
    try {
      records =
          EntityQuery.use(delegator)
              .from("ProductCategory")
              .where("productCategoryTypeId", "INDUSTRY_CATEGORY")
              .orderBy("categoryName")
              .distinct()
              .cache(true)
              .queryList();
    } catch (GenericEntityException e) {
      Debug.logError(e, module);
      if (Debug.verboseOn()) Debug.logVerbose("Exiting method getIndustryTypes", module);

      return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR);
    }

    List<Map<String, Object>> recordList = FastList.newInstance();
    for (GenericValue record : records) {
      Map<String, Object> recordDetails =
          UtilMisc.toMap(
              "id", record.getString("productCategoryId"),
              "name", record.getString("categoryName"),
              "description", record.getString("longDescription")
              );

      recordList.add(recordDetails);
    }

    if (Debug.verboseOn()) Debug.logVerbose("Exiting method getIndustryTypes", module);

    return ApiResponseUtil.prepareOkResponse(recordList);
  }

  @Override
  public Response getPersonalTitles(String keyword, SecurityContext securityContext)
      throws NotFoundException {
    if (Debug.verboseOn()) Debug.logVerbose("Entering method getPersonalTitles", module);

    List<String> uniqueTitles = null;
    List<String> uniqueSuffixes = null;

    try {

      uniqueTitles =
          EntityQuery.use(delegator)
              .from("Person")
              .distinct()
              .orderBy("personalTitle")
              .getFieldList("personalTitle");

      uniqueSuffixes =
          EntityQuery.use(delegator)
              .from("Person")
              .distinct()
              .orderBy("suffix")
              .getFieldList("suffix");

    } catch (GenericEntityException e) {
      Debug.logError(e, module);
      if (Debug.verboseOn()) Debug.logVerbose("Exiting method getPersonalTitles", module);

      return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR);
    }

    Map<String, Object> titles = FastMap.newInstance();
    titles.put("titles", uniqueTitles);
    titles.put("suffixes", uniqueSuffixes);

    return ApiResponseUtil.prepareOkResponse(titles);
  }

  @Override
  public Response getCustRequestDocTypes(SecurityContext securityContext) {
    List<GenericValue> custRequestContentTypeGvs = null;
    try {
      custRequestContentTypeGvs = EntityQuery.use(delegator).from("CustRequestContentType").where("parentTypeId", "CUST_REQ_DOCS").queryList();
    } catch (GenericEntityException e) {
      return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
    }

    List<Map<String, Object>> custRequestContentTypes = new ArrayList<>();
    Map<String, Object> result = new HashMap<>();
    if (UtilValidate.isNotEmpty(custRequestContentTypeGvs)) {
      for (GenericValue custRequestContentType : custRequestContentTypeGvs) {
        Map<String, Object> recordDetails =
                UtilMisc.toMap(
                        "id", custRequestContentType.getString("custRequestContentTypeId"),
                              "description", custRequestContentType.getString("description"));
        custRequestContentTypes.add(recordDetails);
      }
    }
    result.put("records", custRequestContentTypes);
    return ApiResponseUtil.prepareOkResponse(result);
  }

  @Override
  public Response getAllLanguages(SecurityContext securityContext) {
    List<GenericValue> languages;
    try {
      languages = EntityQuery.use(delegator).from("StandardLanguage").queryList();
    } catch (GenericEntityException e) {
      return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
    }

    List<Map<String, Object>> languageList = new ArrayList<>();
    Map<String, Object> result = new HashMap<>();
    if (UtilValidate.isNotEmpty(languages)) {
      for (GenericValue language : languages) {
        Map<String, Object> recordDetails =
                UtilMisc.toMap(
                        "id", language.getString("standardLanguageId"),
                        "name", language.getString("langName"),
                        "code2", language.getString("langCode2"));
        languageList.add(recordDetails);
      }
    }
    SearchResults searchResults = new SearchResults();
    searchResults.setRecords(languageList);
    searchResults.setStartIndex(0);
    searchResults.setViewSize(languageList.size());
    searchResults.setTotalNumberOfRecords(languageList.size());
    return ApiResponseUtil.prepareOkResponse(searchResults);
  }
}
