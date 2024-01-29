package com.fidelissd.zcp.xcommon.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fidelissd.zcp.xcommon.config.AppConfiguration;

import java.util.Map;
import lombok.Data;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilFormatOut;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtilProperties;

/**
 * Utility methods for managing application level configurations like logo, name of the app address
 * etc. All these properties can be overridden in the file below
 * plugins/xcommon/data/XcommonAppData.xml
 *
 */
public class AppConfigUtil {
  private static final String module = AppConfigUtil.class.getName();
  private static AppConfigSettings appConfigSettings;

  private static AppConfigUtil instance = null;

  private AppConfigUtil() {}

  private AppConfigUtil(Delegator delegator) {
    init(delegator);
    instance = new AppConfigUtil();
  }

  public static AppConfigUtil getInstance(Delegator delegator) {
    if (instance == null) {
      new AppConfigUtil(delegator);
    }

    return instance;
  }

  public AppConfigSettings getConfig() {
    return appConfigSettings;
  }

  private GenericValue getPartyByPartyId(Delegator delegator, String partyId, boolean useCache) {
    GenericValue party = null;
    try {
      party = delegator.findOne("Party", UtilMisc.toMap("partyId", partyId), useCache);
    } catch (GenericEntityException e) {
      Debug.logError(e, "Error finding party in getPartyByPartyId", module);
    }
    return party;
  }
  private String getPartyName(GenericValue partyObject) {
    return getPartyName(partyObject, false, true, true);
  }

  private static String formatPartyNameObject(
      GenericValue partyValue, boolean lastNameFirst, boolean usePersonalTitle, boolean useSuffix) {
    if (partyValue == null) {
      return "";
    }
    StringBuilder result = new StringBuilder();
    ModelEntity modelEntity = partyValue.getModelEntity();
    if (modelEntity.isField("firstName")
        && modelEntity.isField("middleName")
        && modelEntity.isField("lastName")) {
      if (usePersonalTitle)
        result.append(UtilFormatOut.ifNotEmpty(partyValue.getString("personalTitle"), "", " "));
      if (lastNameFirst) {
        if (UtilFormatOut.checkNull(partyValue.getString("lastName")) != null) {
          result.append(UtilFormatOut.checkNull(partyValue.getString("lastName")));
          if (partyValue.getString("firstName") != null) {
            result.append(", ");
          }
        }
        result.append(UtilFormatOut.checkNull(partyValue.getString("firstName")));
      } else {
        result.append(UtilFormatOut.ifNotEmpty(partyValue.getString("firstName"), "", " "));
        result.append(UtilFormatOut.ifNotEmpty(partyValue.getString("middleName"), "", " "));
        result.append(UtilFormatOut.checkNull(partyValue.getString("lastName")));
        if (modelEntity.isField("nickname")) {
          if (UtilValidate.isNotEmpty(partyValue.getString("nickname")))
            result.append(" (" + partyValue.getString("nickname") + ")");
        }
      }
      if (useSuffix)
        result.append(UtilFormatOut.ifNotEmpty(partyValue.getString("suffix"), " ", ""));
    }
    if (modelEntity.isField("groupName") && partyValue.get("groupName") != null) {
      result.append(partyValue.getString("groupName"));
    }
    return result.toString();
  }
  private String getPartyName(
      Delegator delegator,
      String partyId,
      boolean lastNameFirst,
      boolean usePersonalTitle,
      boolean useSuffix) {
    GenericValue partyObject = null;
    try {
      partyObject =
          EntityQuery.use(delegator).from("PartyNameView").where("partyId", partyId).queryOne();
    } catch (GenericEntityException e) {
      Debug.logError(e, "Error finding PartyNameView in getPartyName", module);
    }
    if (partyObject == null) {
      return partyId;
    } else {
      return formatPartyNameObject(partyObject, lastNameFirst, usePersonalTitle, useSuffix);
    }
  }
  private String getPartyName(
      GenericValue partyObject,
      boolean lastNameFirst,
      boolean usePersonalTitle,
      boolean useSuffix) {
    if (partyObject == null) {
      return "";
    }
    if ("PartyGroup".equals(partyObject.getEntityName())
        || "Person".equals(partyObject.getEntityName())) {
      return formatPartyNameObject(partyObject, lastNameFirst, usePersonalTitle, useSuffix);
    } else {
      String partyId = null;
      try {
        partyId = partyObject.getString("partyId");
      } catch (IllegalArgumentException e) {
        Debug.logError(e, "Party object does not contain a party ID", module);
      }

      if (partyId == null) {
        Debug.logWarning(
            "No party ID found; cannot get name based on entity: " + partyObject.getEntityName(),
            module);
        return "";
      } else {
        return getPartyName(
            partyObject.getDelegator(), partyId, lastNameFirst, usePersonalTitle, useSuffix);
      }
    }
  }

  /**
   * Returns string representation of the address object.
   * Populates Address modal and uses its formatter.
   * @param postalAddress GenericValue of type PostalAddress
   * @return
   */
  private String formatPostalAddress(GenericValue postalAddress){
    PostalAddress address = new PostalAddress();

    address.setId(postalAddress.getString("contactMechId"));
    address.setAddressLine1(postalAddress.getString("address1"));
    address.setAddressLine2(postalAddress.getString("address2"));
    address.setCity(postalAddress.getString("city"));
    address.setPostalCode(postalAddress.getString("postalCode"));
    address.setStateCode(postalAddress.getString("stateProvinceGeoId"));
    address.setCountryCode(postalAddress.getString("countryGeoId"));

    //populate state and country name
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

  private void init(Delegator delegator) {
    appConfigSettings = new AppConfigSettings();
    String cdnUrl =
        EntityUtilProperties.getPropertyValue("url", "content.url.prefix.secure", delegator) + "/";
    String appPartyId = AppConfiguration.APP_PARTY_ID;
    appConfigSettings.setAppOwnerOrgId(appPartyId);

    GenericValue appPartyObj = getPartyByPartyId(delegator, appPartyId, true);
    String appOrgName = getPartyName(appPartyObj);
    appConfigSettings.setAppOwnerOrgName(appOrgName);

    String appDomainUrl =
        EntityUtilProperties.getPropertyValue(
            "appconfig",
            "app.domain.url",
            delegator); // where the client side is hosted, example app.example.com

    String appName = EntityUtilProperties.getPropertyValue("appconfig", "app.name", delegator);

    String appColorPrimary =
        EntityUtilProperties.getPropertyValue("appconfig", "app.color.primary", delegator);

    String appColorSecondary =
        EntityUtilProperties.getPropertyValue("appconfig", "app.color.secondary", delegator);

    // Used when sending out emails, makes for the via text, example John Doe
    // via MMO
    String appEmailViaText =
        EntityUtilProperties.getPropertyValue("appconfig", "app.email.via", delegator);

    String appLogoUrl = "";
    String appLogoEmailUrl = "";
    String appLogoEmailFooterUrl = "";

    String appLinkedInUrl = "";
    String appFacebookUrl = "";
    String appInstagramUrl = "";
    String appTwitterUrl = "";
    String appPhone = "";
    String appEmail = "";
    String appWebsiteUrl = "";
    String appWebsitePrivacyUrl = "";
    String appWebsiteHelpCenterUrl = "";
    String appContactUsUrl = "";
    String appTermsUrl = "";
    String appYoutubeUrl = "";

    String appAddress = "";

    try {
      GenericValue logoDataResourceObj =
          EntityQuery.use(delegator)
              .from("DataResource")
              .where("dataResourceId", AppConfiguration.APP_LOGO_ID)
              .cache(true)
              .queryOne();

      if (UtilValidate.isNotEmpty(logoDataResourceObj)) {
        appLogoUrl = cdnUrl + logoDataResourceObj.getString("objectInfo");
        Debug.logInfo("Application logo url being served is: " + appLogoUrl, module);
      } else {
        Debug.logWarning(
            "Application logo seed data is missing, please set it up by loading XCommonAppData.xml.",
            module);
      }

      GenericValue logoEmailDataResourceObj =
          EntityQuery.use(delegator)
              .from("DataResource")
              .where("dataResourceId", AppConfiguration.APP_LOGO_EMAIL_ID)
              .cache(true)
              .queryOne();

      if (UtilValidate.isNotEmpty(logoEmailDataResourceObj)) {
        appLogoEmailUrl = cdnUrl + logoEmailDataResourceObj.getString("objectInfo");
        Debug.logInfo("Application email logo url being served is: " + appLogoEmailUrl, module);
      } else {
        Debug.logWarning(
            "Application email logo seed data is missing, please set it up by loading XCommonAppData.xml.",
            module);
      }

      GenericValue logoEmailFooterDataResourceObj =
          EntityQuery.use(delegator)
              .from("DataResource")
              .where("dataResourceId", AppConfiguration.APP_LOGO_EMAIL_FOOTER_ID)
              .cache(true)
              .queryOne();

      if (UtilValidate.isNotEmpty(logoEmailFooterDataResourceObj)) {
        appLogoEmailFooterUrl = cdnUrl + logoEmailFooterDataResourceObj.getString("objectInfo");
        Debug.logInfo(
            "Application email footer logo url being served is: " + appLogoEmailFooterUrl, module);
      } else {
        Debug.logWarning(
            "Application email footer logo seed data is missing, please set it up by loading XCommonAppData.xml.",
            module);
      }

      // email
      GenericValue appEmailContactMech =
          EntityQuery.use(delegator)
              .from("ContactMech")
              .where("contactMechId", AppConfiguration.APP_EMAIL_ID)
              .cache(true)
              .queryOne();

      if (UtilValidate.isNotEmpty(appEmailContactMech)) {
        appEmail = appEmailContactMech.getString("infoString");
        Debug.logInfo("Application support email is : " + appEmail, module);
      } else {
        Debug.logWarning(
            "Application support email is missing, please set it up by loading XCommonAppData.xml.",
            module);
      }

      // phone
      GenericValue appTelecomNumber =
          EntityQuery.use(delegator)
              .from("TelecomNumber")
              .where("contactMechId", AppConfiguration.APP_PHONE_ID)
              .cache(true)
              .queryOne();

      if (UtilValidate.isNotEmpty(appTelecomNumber)) {
        String tenDigitPhone =
            appTelecomNumber.getString("areaCode") + appTelecomNumber.getString("contactNumber");
        int phoneCountryCode = Integer.parseInt(appTelecomNumber.getString("countryCode"));

        Map formattedPhoneNumberInfo =
            AxPhoneNumberUtil.preparePhoneNumberInfo(
                tenDigitPhone,
                AxPhoneNumberUtil.getPhoneRegionCodeFromCountryCode(phoneCountryCode));
        appPhone = (String) formattedPhoneNumberInfo.get("internationalFormat");
        Debug.logInfo("Application support phone is : " + appPhone, module);
      } else {
        Debug.logWarning(
            "Application support phone is missing, please set it up by loading XCommonAppData.xml.",
            module);
      }

      // postal address
      GenericValue appPostalAddress =
          EntityQuery.use(delegator)
              .from("PostalAddress")
              .where("contactMechId", AppConfiguration.APP_ADDRESS_ID)
              .cache(true)
              .queryOne();

      if (UtilValidate.isNotEmpty(appPostalAddress)) {
        appAddress = formatPostalAddress(appPostalAddress);
        Debug.logInfo("Application address is : " + appAddress, module);
      } else {
        Debug.logWarning(
            "Application address is missing, please set it up by loading XCommonAppData.xml.",
            module);
      }
      
      // website
      GenericValue appWebsiteContactMech =
          EntityQuery.use(delegator)
              .from("ContactMech")
              .where("contactMechId", AppConfiguration.APP_WEBSITE_ID)
              .cache(true)
              .queryOne();

      if (UtilValidate.isNotEmpty(appWebsiteContactMech)) {
        appWebsiteUrl = appWebsiteContactMech.getString("infoString");
        Debug.logInfo("Application Website url is : " + appWebsiteUrl, module);
      }

      // privacy
      GenericValue appWebsitePrivacyContactMech =
          EntityQuery.use(delegator)
              .from("ContactMech")
              .where("contactMechId", AppConfiguration.APP_WEBSITE_PRIVACY_ID)
              .cache(true)
              .queryOne();

      if (UtilValidate.isNotEmpty(appWebsitePrivacyContactMech)) {
        appWebsitePrivacyUrl = appWebsitePrivacyContactMech.getString("infoString");
        Debug.logInfo("Application WebsitePrivacy url is : " + appWebsitePrivacyUrl, module);
      }

      // Help center
      GenericValue appWebsiteHelpCenterContactMech =
          EntityQuery.use(delegator)
              .from("ContactMech")
              .where("contactMechId", AppConfiguration.APP_WEBSITE_HELP_CENTER_ID)
              .cache(true)
              .queryOne();

      if (UtilValidate.isNotEmpty(appWebsiteHelpCenterContactMech)) {
        appWebsiteHelpCenterUrl = appWebsiteHelpCenterContactMech.getString("infoString");
        Debug.logInfo(
            "Application Website Help Center url is : " + appWebsiteHelpCenterUrl, module);
      }

      // contact us
      GenericValue appContactUsContactMech =
          EntityQuery.use(delegator)
              .from("ContactMech")
              .where("contactMechId", AppConfiguration.APP_WEBSITE_CONTACT_ID)
              .cache(true)
              .queryOne();

      if (UtilValidate.isNotEmpty(appContactUsContactMech)) {
        appContactUsUrl = appContactUsContactMech.getString("infoString");
        Debug.logInfo("Application ContactUs url is : " + appContactUsUrl, module);
      }

      // terms
      GenericValue appTermsContactMech =
          EntityQuery.use(delegator)
              .from("ContactMech")
              .where("contactMechId", AppConfiguration.APP_WEBSITE_TERMS_ID)
              .cache(true)
              .queryOne();

      if (UtilValidate.isNotEmpty(appTermsContactMech)) {
        appTermsUrl = appTermsContactMech.getString("infoString");
        Debug.logInfo("Application Terms url is : " + appTermsUrl, module);
      }

      // social media links
      GenericValue appLinkedInContactMech =
          EntityQuery.use(delegator)
              .from("ContactMech")
              .where("contactMechId", AppConfiguration.APP_SOCIAL_LINKEDIN_ID)
              .cache(true)
              .queryOne();

      if (UtilValidate.isNotEmpty(appLinkedInContactMech)) {
        appLinkedInUrl = appLinkedInContactMech.getString("infoString");
        Debug.logInfo("Application LinkedIn url is : " + appLinkedInUrl, module);
      }

      GenericValue appFacebookContactMech =
          EntityQuery.use(delegator)
              .from("ContactMech")
              .where("contactMechId", AppConfiguration.APP_SOCIAL_FACEBOOK_ID)
              .cache(true)
              .queryOne();

      if (UtilValidate.isNotEmpty(appFacebookContactMech)) {
        appFacebookUrl = appFacebookContactMech.getString("infoString");
        Debug.logInfo("Application Facebook url is : " + appFacebookUrl, module);
      }

      GenericValue appTwitterContactMech =
          EntityQuery.use(delegator)
              .from("ContactMech")
              .where("contactMechId", AppConfiguration.APP_SOCIAL_TWITTER_ID)
              .cache(true)
              .queryOne();

      if (UtilValidate.isNotEmpty(appTwitterContactMech)) {
        appTwitterUrl = appTwitterContactMech.getString("infoString");
        Debug.logInfo("Application Twitter url is : " + appTwitterUrl, module);
      }

      GenericValue appInstagramContactMech =
          EntityQuery.use(delegator)
              .from("ContactMech")
              .where("contactMechId", AppConfiguration.APP_SOCIAL_INSTAGRAM_ID)
              .cache(true)
              .queryOne();

      if (UtilValidate.isNotEmpty(appInstagramContactMech)) {
        appInstagramUrl = appInstagramContactMech.getString("infoString");
        Debug.logInfo("Application Instagram url is : " + appInstagramUrl, module);
      }

      GenericValue appYoutubeContactMech =
          EntityQuery.use(delegator)
              .from("ContactMech")
              .where("contactMechId", AppConfiguration.APP_SOCIAL_YOUTUBE_ID)
              .cache(true)
              .queryOne();

      if (UtilValidate.isNotEmpty(appYoutubeContactMech)) {
        appYoutubeUrl = appYoutubeContactMech.getString("infoString");
        Debug.logInfo("Application Instagram url is : " + appInstagramUrl, module);
      }

    } catch (GenericEntityException e) {
      Debug.logError(e, module);
    }

    appConfigSettings.setCdnUrl(cdnUrl);
    appConfigSettings.setEmailVia(appEmailViaText);
    appConfigSettings.setAppName(appName);
    appConfigSettings.setAppColorPrimary(appColorPrimary);
    appConfigSettings.setAppColorSecondary(appColorSecondary);
    appConfigSettings.setAppDomainUrl(appDomainUrl);
    appConfigSettings.setAppLogoUrl(appLogoUrl);
    appConfigSettings.setAppLogoEmailUrl(appLogoEmailUrl);
    appConfigSettings.setAppLogoEmailFooterUrl(appLogoEmailFooterUrl);
    appConfigSettings.setAppLinkedInUrl(appLinkedInUrl);
    appConfigSettings.setAppInstagramUrl(appInstagramUrl);
    appConfigSettings.setAppFacebookUrl(appFacebookUrl);
    appConfigSettings.setAppTwitterUrl(appTwitterUrl);
    appConfigSettings.setAppPhone(appPhone);
    appConfigSettings.setAppEmail(appEmail);
    appConfigSettings.setAppAddress(appAddress);
    appConfigSettings.setAppWebsiteUrl(appWebsiteUrl);
    appConfigSettings.setAppWebsiteUrl(appWebsiteUrl);
    appConfigSettings.setAppWebsitePrivacyUrl(appWebsitePrivacyUrl);
    appConfigSettings.setAppTermsUrl(appTermsUrl);
    appConfigSettings.setAppContactUsUrl(appContactUsUrl);
    appConfigSettings.setAppYoutubeUrl(appYoutubeUrl);
    appConfigSettings.setAppWebsiteHelpCenterUrl(appWebsiteHelpCenterUrl);
  }

  @Data
  public class AppConfigSettings {

    /** ID (Party I)D of the SAAS app owner organization */
    private String appOwnerOrgId;

    /** Name of the SAAS app owner organization, example Manage the day */
    private String appOwnerOrgName;

    /** CDN URL of the application, to make CDN accessible redirects */
    private String cdnUrl;

    /**
     * Name to be used when sending out emails from the system. Example for an application named
     * "MMO" when an email goes out from a user's profile the email from sender will read as "John
     * Doe via MMO".
     */
    private String emailVia;

    /** Name of the application. Example Manage The Day */
    private String appName;

    /** Primary color code of the application of the application. Example #756123 */
    private String appColorPrimary;

    /** Secondary color code of the application. Example #715649 */
    private String appColorSecondary;

    /** Domain URL of the client facing application. Example https://app.pnp.works */
    private String appDomainUrl;

    /** Name of the organization, example manage the day*/
    private String appOrgName;

    /** URL to download the logo for the application */
    private String appLogoUrl;

    /** URL to view the logo for the application used in emails 50x50 */
    private String appLogoEmailUrl;

    /** URL to download the logo for the application used in footer of emails, usually grayscale */
    private String appLogoEmailFooterUrl;

    /** LinkedIn url of the application */
    private String appLinkedInUrl;

    /** Facebook URL of the application */
    private String appFacebookUrl;

    /** Twitter URL of the application */
    private String appTwitterUrl;

    /** Instagram URL of the application */
    private String appInstagramUrl;

    /** Instagram URL of the application */
    private String appYoutubeUrl;

    /** Primary phone for contact/support of the application */
    private String appPhone;

    /** Primary email for contact/support of the application */
    private String appEmail;

    private String appWebsiteUrl;
    private String appWebsitePrivacyUrl;
    private String appWebsiteHelpCenterUrl;
    private String appContactUsUrl;
    private String appTermsUrl;

    /** Formatted address of the application office/headquarters. */
    private String appAddress;
  }

  public enum ApplicationPreferences {
    STRIPE_API_PUBLISHABLE_KEY("STRIPE_PUB_KEY", "Stripe API publishable key for test"),
    STRIPE_API_SECRET_KEY("STRIPE_SECRET_KEY", "Stripe API secret key for test"),
    STRIPE_BASIC_PLAN("STRIPE_BASIC_PLAN", "Stripe basic plan, to be activated default on "
        + "on-boarding "),
    GOOGLE_API_KEY("GOOGLE_API_KEY", "Google API key for Maps, Timezone etc.");

    private String preferenceId;
    private String description;

    ApplicationPreferences(String preferenceId, String description) {
      this.preferenceId = preferenceId;
      this.description = description;
    }

    public String getPreferenceId() {
      return preferenceId;
    }

    public String getDescription() {
      return description;
    }
  }

  /**
   * Returns the application level configurations populated in a Map, contains
   *
   * <p>id (Party ID of the organization) cdnUrl (CDN URL of the application, to make CDN accessible
   * redirects.) emailVia (Name to be used when sending out emails from the system. Example for an
   * application named "MMO" when an email goes out from a user's profile the email from sender will
   * read as "John Doe via MMO" ) appName (Name of the application. Example Manage The Day)
   * appColorPrimary (Primary color code of the application of the application. Example #756123)
   * appColorSecondary (Secondary color code of the application. Example #715649) appDomainUrl
   * (Domain URL of the client facing application. Example https://app.pnp.works) appOrgName
   * (Name of the organization, example manage the day) appLogoUrl (URL to download the logo for the
   * application) appLogoEmailUrl (URL to view the logo for the application used in emails 50x50)
   * appLogoEmailFooterUrl (URL to download the logo for the application used in footer of emails,
   * usually grayscale) appLinkedInUrl appFacebookUrl appTwitterUrl appInstagramUrl appYoutubeUrl
   * appPhone appEmail appWebsiteUrl appWebsitePrivacyUrl appWebsiteHelpCenterUrl appContactUsUrl
   * appTermsUrl appAddress
   *
   * @return
   */
  public Map getAppConfiguration() {
    ObjectMapper objectMapper = new ObjectMapper();
    Map<String, Object> toMapConfig = objectMapper.convertValue(appConfigSettings, Map.class);

    return toMapConfig;
  }

  /**
   * Returns the application level preference value using the passed key. Use it to get API Keys and
   * other application level credentials.
   *
   * @param delegator
   * @return
   */
  public static String getAppPreference(Delegator delegator, ApplicationPreferences preference) {
    String appPreferenceValue = "";
    try {
      GenericValue userPreference =
          EntityQuery.use(delegator)
              .from("UserPreference")
              .where(
                  "userLoginId",
                  AppConfiguration.APP_USER_LOGIN_ID,
                  "userPrefTypeId",
                  preference.preferenceId)
              .cache(true)
              .queryOne();
      if (UtilValidate.isEmpty(userPreference)) {
        Debug.logWarning(
            "Google API key is missing, please load it under UserPreference for <"
                + AppConfiguration.APP_USER_LOGIN_ID
                + "> to fix this.",
            module);
      }

      appPreferenceValue = userPreference.getString("userPrefValue");
    } catch (GenericEntityException e) {
      Debug.logError(e, module);
    }
    return appPreferenceValue;
  }
}

@Data
class PostalAddress{
  private String id = null;
  private String addressLine1 = null;
  private String addressLine2 = null;
  private String city = null;
  private String stateName = null;
  private String stateCode = null;
  private String countryName = null;
  private String countryCode = null;
  private String postalCode = null;

  public String getFormattedAddressString() {
    StringBuffer formattedAddress = new StringBuffer();
    if (UtilValidate.isNotEmpty(this.addressLine1)) {
      formattedAddress.append(this.addressLine1);
    }
    if (UtilValidate.isNotEmpty(this.addressLine2)) {
      formattedAddress.append(", " + this.addressLine2);
    }
    if (UtilValidate.isNotEmpty(this.city)) {
      formattedAddress.append(", " + this.city);
    }
    if (UtilValidate.isNotEmpty(this.stateCode)) {
      formattedAddress.append(", " + this.stateCode);
      if (UtilValidate.isNotEmpty(this.postalCode)) {
        formattedAddress.append(" " + this.postalCode);
      }
    }
    if (UtilValidate.isNotEmpty(this.countryCode)) {
      formattedAddress.append(", " + this.countryCode);
    }
    return formattedAddress.toString();
  }
}
