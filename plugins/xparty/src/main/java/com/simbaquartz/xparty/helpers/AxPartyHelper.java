package com.simbaquartz.xparty.helpers;

import com.fidelissd.zcp.xcommon.util.AxUtilValidate;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberToTimeZonesMapper;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.google.i18n.phonenumbers.geocoding.PhoneNumberOfflineGeocoder;
import com.fidelissd.zcp.xcommon.collections.FastList;
import com.fidelissd.zcp.xcommon.collections.FastMap;
import com.fidelissd.zcp.xcommon.enums.CommonContentTypesEnum;
import com.fidelissd.zcp.xcommon.models.Phone;
import com.fidelissd.zcp.xcommon.models.email.EmailAddress;
import com.fidelissd.zcp.xcommon.services.contact.EmailTypesEnum;
import com.fidelissd.zcp.xcommon.services.contact.PhoneTypesEnum;
import com.fidelissd.zcp.xcommon.util.AxPhoneNumberUtil;
import com.fidelissd.zcp.xcommon.util.AxUtilFormat;
import com.simbaquartz.xparty.ContactMethodTypesEnum;
import com.fidelissd.zcp.xcommon.util.hierarchy.HierarchyUtils;
import com.simbaquartz.xparty.services.location.PostalAddressTypesEnum;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilFormatOut;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericDelegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.party.contact.ContactHelper;
import org.apache.ofbiz.party.contact.ContactMechWorker;
import org.apache.ofbiz.party.content.PartyContentWrapper;
import org.apache.ofbiz.party.party.PartyHelper;
import org.apache.ofbiz.party.party.PartyWorker;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;


/**
 * AxPartyHelper TOOD This whole thing needs to be replaced by the recursive logic in
 * WEB-INF/actions/order/GetCustomerLogo.groovy
 */
public class AxPartyHelper {

  public static final String module = AxPartyHelper.class.getName();

  public static final String PARTY_CONTACT_PURPOSE_PHONE_PRIMARY =
      PhoneTypesEnum.PRIMARY.getTypeId();
  public static final String PARTY_CONTACT_PURPOSE_EMAIL_PRIMARY =
      EmailTypesEnum.PRIMARY.getTypeId();
  public static final String PARTY_CONTACT_PURPOSE_POSTAL_ADDRESS_PRIMARY =
      PostalAddressTypesEnum.PRIMARY.getTypeId();
  public static final String PARTY_CONTACT_PURPOSE_POSTAL_ADDRESS_GENERAL_LOCATION =
      PostalAddressTypesEnum.GENERAL.getTypeId();

  private AxPartyHelper() {}

  public static enum PartyRoles {
    STORE_CUSTOMER("CUSTOMER");

    private String partyRoleId;

    PartyRoles(String partyRoleId) {
      this.partyRoleId = partyRoleId;
    }

    public String getId() {
      return partyRoleId;
    }
  }

  public static boolean isPartyEnabled(String statusId) {
    return "PARTY_ENABLED".equals(statusId);
  }

  public static String getPartyName(Delegator delegator, String partyId) {
    return getPartyName(delegator, partyId, false, true, true);
  }

  public static String getPartyName(
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

  public static String getPartyName(GenericValue partyObject) {
    return getPartyName(partyObject, false, true, true);
  }

  public static String getPartyName(
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
   * Checks if party is of type person or party group, populates firstName, middleName, lastName,
   * companyName(groupName) accordingly.
   *
   * @param delegator
   * @param party
   * @return
   */
  public static Map<String, String> getPartyNameDetails(Delegator delegator, GenericValue party) {
    Map<String, String> partyNameDetailsMap = FastMap.newInstance();

    String partyId = party.getString("partyId");

    GenericValue partyObject = null;
    try {
      partyObject =
          EntityQuery.use(delegator).from("PartyNameView").where("partyId", partyId).queryOne();
    } catch (GenericEntityException e) {
      Debug.logError(e, "Error finding PartyNameView in getPartyName", module);
    }

    if (UtilValidate.isNotEmpty(partyObject)) {
      String firstName = "";
      String middleName = "";
      String lastName = "";
      String personalTitle = "";
      String companyName = "";
      String displayName = PartyHelper.getPartyName(partyObject);

      if (HierarchyUtils.isPerson(party)) {
        firstName = partyObject.getString("firstName");
        middleName = partyObject.getString("middleName");
        lastName = partyObject.getString("lastName");
        personalTitle = partyObject.getString("personalTitle");
      } else {
        companyName = partyObject.getString("groupName");
      }

      partyNameDetailsMap.put("firstName", firstName);
      partyNameDetailsMap.put("middleName", middleName);
      partyNameDetailsMap.put("lastName", lastName);
      partyNameDetailsMap.put("personalTitle", personalTitle);
      partyNameDetailsMap.put("companyName", companyName);
      partyNameDetailsMap.put("displayName", displayName);
    }

    return partyNameDetailsMap;
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

  public static String formatPersonName(
      String firstName,
      String middleName,
      String lastName,
      String personalTitle,
      String suffix,
      String nickname,
      boolean lastNameFirst,
      boolean usePersonalTitle,
      boolean useSuffix) {
    StringBuilder result = new StringBuilder();
    if (UtilValidate.isNotEmpty(firstName)
        || UtilValidate.isNotEmpty(middleName)
        || UtilValidate.isNotEmpty(lastName)) {
      if (usePersonalTitle) result.append(UtilFormatOut.ifNotEmpty(personalTitle, "", " "));
      if (lastNameFirst) {
        result.append(UtilFormatOut.checkNull(lastName));
        if (firstName != null) {
          result.append(", ");
        }
        result.append(UtilFormatOut.checkNull(firstName));
      } else {
        result.append(UtilFormatOut.ifNotEmpty(firstName, "", " "));
        result.append(UtilFormatOut.ifNotEmpty(middleName, "", " "));
        result.append(UtilFormatOut.checkNull(lastName));

        if (UtilValidate.isNotEmpty(nickname)) result.append(" (" + nickname + ")");
      }

      if (useSuffix) result.append(UtilFormatOut.ifNotEmpty(suffix, " ", ""));
    }

    return result.toString().trim();
  }
  /**
   * Returns Party initials for the supplied party id.
   *
   * @param delegator
   * @param partyId
   * @param includeMiddleName includes middle names' first letter if set to true, only name initials
   *     otherwise
   * @return
   */
  public static String getPartyInitials(
      Delegator delegator, String partyId, boolean includeMiddleName) {
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
      return formatPartyNameInitials(partyObject, includeMiddleName);
    }
  }

  /**
   * Returns Party initials for the supplied party object.
   *
   * @param partyObject
   * @param includeMiddleName
   * @return
   */
  public static String getPartyInitials(GenericValue partyObject, boolean includeMiddleName) {
    if (partyObject == null) {
      return "";
    }
    if ("PartyGroup".equals(partyObject.getEntityName())
        || "Person".equals(partyObject.getEntityName())) {
      return formatPartyNameInitials(partyObject, includeMiddleName);
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
        return getPartyInitials(partyObject.getDelegator(), partyId, includeMiddleName);
      }
    }
  }

  /**
   * Formats Party name, for person with name "John Smith" returns JS, for company name returns
   * first two characters of company name.
   *
   * @param partyValue
   * @param includeMiddleName
   * @return
   */
  public static String formatPartyNameInitials(GenericValue partyValue, boolean includeMiddleName) {
    if (partyValue == null) {
      return "";
    }
    StringBuilder result = new StringBuilder();
    ModelEntity modelEntity = partyValue.getModelEntity();
    if (modelEntity.isField("firstName")
        && modelEntity.isField("middleName")
        && modelEntity.isField("lastName")) {
      if (includeMiddleName) {
        result.append(
            getFirstLetter(UtilFormatOut.checkNull(partyValue.getString("firstName")), 1));
        result.append(
            getFirstLetter(UtilFormatOut.checkNull(partyValue.getString("middleName")), 1));
        result.append(getFirstLetter(UtilFormatOut.checkNull(partyValue.getString("lastName")), 1));
      } else {
        result.append(
            getFirstLetter(UtilFormatOut.checkNull(partyValue.getString("firstName")), 1));
        result.append(getFirstLetter(UtilFormatOut.checkNull(partyValue.getString("lastName")), 1));
      }
    }
    if (modelEntity.isField("groupName") && partyValue.get("groupName") != null) {
      result.append(getFirstLetter(partyValue.getString("groupName"), 2));
    }
    return result.toString();
  }

  /**
   * Returns first letter of the
   *
   * @param value
   * @param numberOfLetters
   * @return
   */
  private static String getFirstLetter(String value, int numberOfLetters) {
    // make sure length is within bounds to avoid IndexOutOfBoundException
    if (UtilValidate.isEmpty(value) || (value.length() < numberOfLetters)) {
      return value;
    }

    return value.substring(0, numberOfLetters);
  }

  /**
   * Returns logo image content id for the party, filters based on date as well.
   *
   * @param delegator
   * @param partyId
   * @return
   */
  public static String getPartyLogo(Delegator delegator, String partyId) {
    String partyLogoContentId = null;
    GenericValue partyLogoContent =
        PartyContentWrapper.getFirstPartyContentByType(
            partyId, null, CommonContentTypesEnum.PHOTO.getTypeId(), delegator);

    if (UtilValidate.isNotEmpty(partyLogoContent)) {
      partyLogoContentId = partyLogoContent.getString("contentId");
    }

    return partyLogoContentId;
  }

  /**
   * Returns logo image content id for the party, filters based on date as well.
   *
   * @param party
   * @return
   */
  public static String getPartyLogo(GenericValue party) {
    String partyLogoContentId = null;
    GenericValue partyLogoContent =
        PartyContentWrapper.getFirstPartyContentByType(
            party.getString("partyId"),
            null,
            CommonContentTypesEnum.PHOTO.getTypeId(),
            party.getDelegator());

    if (UtilValidate.isNotEmpty(partyLogoContent)) {
      partyLogoContentId = partyLogoContent.getString("contentId");
    }

    return partyLogoContentId;
  }

  /**
   * @Deprecated use getPartyBasicDetails instead.
   *
   * @param delegator
   * @param partyId
   * @param includeMiddleName
   * @return
   */
  @Deprecated
  public static Map getPartyNameAndLogoDetails(
      Delegator delegator, String partyId, boolean includeMiddleName) {
    Map partyNameAndLogoDetails = FastMap.newInstance();

    GenericValue party = HierarchyUtils.getPartyByPartyId(delegator, partyId);

    if (UtilValidate.isNotEmpty(party)) {
      partyNameAndLogoDetails.put("partyId", partyId);
      partyNameAndLogoDetails.put("partyName", AxPartyHelper.getPartyName(party));
      partyNameAndLogoDetails.put("partyInitials", getPartyInitials(party, includeMiddleName));
      partyNameAndLogoDetails.put("partyLogoContentId", getPartyLogo(delegator, partyId));

      String photoUrl = party.getString("photoUrl");
      if (UtilValidate.isNotEmpty(photoUrl)) {
        partyNameAndLogoDetails.put("photoUrl", photoUrl);
      }

      String displayName = party.getString("displayName");
      if (UtilValidate.isNotEmpty(displayName)) {
        partyNameAndLogoDetails.put("displayName", displayName);
      }

      String email = party.getString("email");
      if (UtilValidate.isNotEmpty(email)) {
        partyNameAndLogoDetails.put("email", email);
      }
    }

    return partyNameAndLogoDetails;
  }

  /**
   * Returns party photoUrl, displayName, email, initials for the party. Fetches details from Party
   * Object, is very fast.
   *
   * @param delegator
   * @param partyId
   * @return
   */
  public static Map getPartyBasicDetails(Delegator delegator, String partyId) {
    GenericValue party = HierarchyUtils.getPartyByPartyId(delegator, partyId);

    return getPartyBasicDetails(party);
  }

  public static Map getPartyBasicDetails(GenericValue party) {
    return getPartyBasicDetails(party, true);
  }
  /**
   * Returns party photoUrl, displayName, email, initials for the party. Fetches details from Party
   * Object, is very fast.
   *
   * @param party {@link GenericValue} object for Party
   * @param personDetailsRequired flag indicating whether person details are required or not, if
   *     true also makes a DB call to fetch additional person details , name, suffix, salutation,
   *     dob etc.
   * @return
   */
  public static Map getPartyBasicDetails(GenericValue party, boolean personDetailsRequired) {
    Map partyBasicDetails = FastMap.newInstance();

    if (UtilValidate.isNotEmpty(party)) {
      partyBasicDetails.put("location", party.getString("location"));
      partyBasicDetails.put("defaultCurrency", party.getString("preferredCurrencyUomId"));
      partyBasicDetails.put("createdDate", party.getTimestamp("createdDate"));

      String partyId = party.getString("partyId");
      try {
        if (personDetailsRequired && HierarchyUtils.isPerson(party)) {
          GenericValue person = party.getRelatedOne("Person", false);
          if (UtilValidate.isNotEmpty(person)) {
            partyBasicDetails.put("isPerson", true);

            String firstName = person.getString("firstName");
            partyBasicDetails.put("firstName", firstName);

            String middleName = person.getString("middleName");
            partyBasicDetails.put("middleName", middleName);

            String lastName = person.getString("lastName");
            partyBasicDetails.put("lastName", lastName);

            String personalTitle = person.getString("personalTitle");
            partyBasicDetails.put("personalTitle", personalTitle);

            String nickname = person.getString("nickname");
            partyBasicDetails.put("nickname", nickname);

            String suffix = person.getString("suffix");
            partyBasicDetails.put("suffix", suffix);

            String salutation = person.getString("salutation");
            partyBasicDetails.put("salutation", salutation);

            String fullName =
                formatPersonName(
                    firstName,
                    middleName,
                    lastName,
                    personalTitle,
                    suffix,
                    nickname,
                    false,
                    true,
                    true);
            partyBasicDetails.put("fullName", fullName);

            partyBasicDetails.put("createdStamp", person.getTimestamp("createdStamp"));
            partyBasicDetails.put("lastUpdatedStamp", person.getTimestamp("lastUpdatedStamp"));

            String genderValue = person.getString("gender");
            String gender = "not-specified";
            if (UtilValidate.isNotEmpty(genderValue)) {
              if ("M".equals(genderValue)) {
                gender = "male";
              } else if ("F".equals(genderValue)) {
                gender = "female";
              }

              partyBasicDetails.put("gender", gender);
            }

            partyBasicDetails.put("birthday", person.getDate("birthDate"));
          }
        }
      } catch (GenericEntityException e) {
        Debug.logError(e, "Error getting person info, ignoring...", module);
      }

      String photoUrl = party.getString("photoUrl");
      if (UtilValidate.isNotEmpty(photoUrl)) {
        partyBasicDetails.put("photoUrl", photoUrl);
      }

      String displayName = party.getString("displayName");
      if (UtilValidate.isNotEmpty(displayName)) {
        partyBasicDetails.put("displayName", displayName.trim());
        partyBasicDetails.put("initials", getPartyInitials(party, false));
      }

      String email = party.getString("email");
      if (UtilValidate.isNotEmpty(email)) {
        partyBasicDetails.put("email", email);
      }

      String statusId = party.getString("statusId");
      if (UtilValidate.isNotEmpty(statusId)) {
        partyBasicDetails.put("statusId", statusId);
      }

      partyBasicDetails.put("partyId", partyId);
      partyBasicDetails.put("id", partyId);
    }

    return partyBasicDetails;
  }

  /**
   * Returns the primary email of the party
   *
   * @param delegator
   * @param partyId
   * @return
   */
  public static String getPartyEmail(Delegator delegator, String partyId) {
    Map partyBasicDetails = getPartyBasicDetails(delegator, partyId);

    if (UtilValidate.isNotEmpty(partyBasicDetails)) {
      return (String) partyBasicDetails.get("email");
    }

    return null;
  }

  /**
   * Returns the photo url of the party
   *
   * @param delegator
   * @param partyId
   * @return
   */
  public static String getPartyPhotoUrl(Delegator delegator, String partyId) {
    Map partyBasicDetails = getPartyBasicDetails(delegator, partyId);

    if (UtilValidate.isNotEmpty(partyBasicDetails)) {
      return (String) partyBasicDetails.get("photoUrl");
    }

    return null;
  }

  /**
   * Sets the photo url of the party at Party entity level using Party.photoUrl
   *
   * @param delegator
   * @param partyId
   * @param publicPhotoUrl
   * @return
   */
  public static void setPartyPhotoUrl(Delegator delegator, String partyId, String publicPhotoUrl) {
    GenericValue partyGv = HierarchyUtils.getPartyByPartyId(delegator, partyId);

    if (UtilValidate.isNotEmpty(partyGv)) {
      partyGv.set("photoUrl", publicPhotoUrl);
      try {
        delegator.store(partyGv);
      } catch (GenericEntityException e) {
        Debug.logError(e, module);
      }
    }
  }
  /**
   * Returns the display name of the party
   *
   * @param delegator
   * @param partyId
   * @return
   */
  public static String getPartyDisplayName(Delegator delegator, String partyId) {
    Map partyBasicDetails = getPartyBasicDetails(delegator, partyId);

    if (UtilValidate.isNotEmpty(partyBasicDetails)) {
      return (String) partyBasicDetails.get("displayName");
    }

    return null;
  }

  /**
   * Returns party name, initials, logo image content id for the party.
   *
   * @param party
   * @param includeMiddleName
   * @return
   */
  public static Map getPartyNameAndLogoDetails(GenericValue party, boolean includeMiddleName) {
    Map partyNameAndLogoDetails = FastMap.newInstance();

    if (UtilValidate.isNotEmpty(party)) {
      partyNameAndLogoDetails.put("partyId", party.getString("partyId"));
      partyNameAndLogoDetails.put("partyName", PartyHelper.getPartyName(party));
      partyNameAndLogoDetails.put("partyInitials", getPartyInitials(party, includeMiddleName));
      partyNameAndLogoDetails.put("partyLogoContentId", getPartyLogo(party));
    }

    return partyNameAndLogoDetails;
  }

  /**
   * Returns the most latest (ordered by fromDate) party phone number.
   *
   * @param delegator
   * @param partyId String
   * @return
   */
  public static String getPartyLatestPhoneFormatted(Delegator delegator, String partyId) {
    GenericValue party = HierarchyUtils.getPartyByPartyId(delegator, partyId);
    return getPartyLatestPhoneFormatted(delegator, party);
  }

  /**
   * Returns the most latest (ordered by fromDate) party phone number.
   *
   * @param delegator
   * @param party GenericValue
   * @return
   */
  public static String getPartyLatestPhoneFormatted(Delegator delegator, GenericValue party) {
    String partyFormattedPhoneNumber = null;

    try {
      List<GenericValue> partyContactMechList;
      List<GenericValue> list =
          EntityUtil.filterByDate(
              party.getRelated(
                  "PartyContactMechPurpose",
                  UtilMisc.toMap("contactMechPurposeTypeId", "PRIMARY_PHONE"),
                  null,
                  false),
              true);
      partyContactMechList = EntityUtil.getRelated("PartyContactMech", null, list, false);
      partyContactMechList =
          EntityUtil.orderBy(partyContactMechList, UtilMisc.toList("fromDate DESC"));

      GenericValue partyPhoneContactMech = EntityUtil.getFirst(partyContactMechList);

      if (UtilValidate.isNotEmpty(partyPhoneContactMech)) {
        String contactMechId = (String) partyPhoneContactMech.get("contactMechId");
        String extension = (String) partyPhoneContactMech.get("extension");
        GenericValue telecomNumber =
            EntityQuery.use(delegator)
                .from("TelecomNumber")
                .where("contactMechId", contactMechId)
                .queryOne();

        if (UtilValidate.isNotEmpty(telecomNumber)) {
          String countryCode = (String) telecomNumber.get("countryCode");
          String areaCode = (String) telecomNumber.get("areaCode");
          String contactNumber = (String) telecomNumber.get("contactNumber");
          String askForName = (String) telecomNumber.get("askForName");

          partyFormattedPhoneNumber =
              getFormattedPhoneNumber(
                  delegator, contactMechId, countryCode, areaCode, contactNumber, extension);
          // partyFormattedPhoneNumber = getPhoneNumberInUSFormat(countryCode, areaCode,
          // contactNumber, extension);
        }
      }

    } catch (GenericEntityException gee) {
      Debug.logWarning(gee, module);
      return null;
    }

    return partyFormattedPhoneNumber;
  }

  public static List<GenericValue> getPartyPhoneNumbers(
      Delegator delegator, GenericValue party, String contactMechPurposeTypeId)
      throws GenericEntityException {
    List telecomNumbers = FastList.newInstance();
    String contactMechTypeId = "TELECOM_NUMBER";
    Collection<GenericValue> partyTelecomNumbers =
        ContactHelper.getContactMechByType(party, contactMechTypeId, false);
    for (GenericValue partyTelecomNumber : partyTelecomNumbers) {

      GenericValue telecomNumber = partyTelecomNumber.getRelatedOne("TelecomNumber", false);
      GenericValue partyContactMech = null;

      partyContactMech =
          EntityQuery.use(delegator)
              .from("PartyContactMech")
              .where(
                  "contactMechId",
                  partyTelecomNumber.getString("contactMechId"),
                  "partyId",
                  party.getString("partyId"))
              .cache(false)
              .queryFirst();
      if (UtilValidate.isNotEmpty(telecomNumber)) {
        List<GenericValue> partyContactMechPurposes = FastList.newInstance();
        if (UtilValidate.isNotEmpty(contactMechPurposeTypeId)) {
          partyContactMechPurposes =
              EntityUtil.filterByDate(
                  partyContactMech.getRelated(
                      "PartyContactMechPurpose",
                      UtilMisc.toMap("contactMechPurposeTypeId", contactMechPurposeTypeId),
                      null,
                      false),
                  true);
        } else {
          partyContactMechPurposes =
              EntityUtil.filterByDate(
                  partyContactMech.getRelated("PartyContactMechPurpose", null, null, false), true);
        }

        String contactMechId = (String) telecomNumber.get("contactMechId");
        GenericValue partyRegionDetails =
            EntityQuery.use(delegator)
                .from("UserPreference")
                .where(
                    "userLoginId",
                    contactMechId,
                    "userPrefTypeId",
                    "defaultCountryGeoId",
                    "userPrefGroupTypeId",
                    "APPL_PREFERENCES")
                .queryOne();
        String partyRegion = "";
        if (UtilValidate.isNotEmpty(partyRegionDetails)) {
          partyRegion = partyRegionDetails.getString("userPrefValue");
        }
        if (UtilValidate.isEmpty(partyRegion)) {
          partyRegion = "US";
        }
        String countryCode = (String) telecomNumber.get("countryCode");
        String areaCode = (String) telecomNumber.get("areaCode");
        String contactNumber = (String) telecomNumber.get("contactNumber");
        String extension = partyContactMech.getString("extension");

        Map telecomNumberMap = FastMap.newInstance();
        telecomNumberMap.put("contactMechId", contactMechId);
        telecomNumberMap.put("formattedPhone", "(" + areaCode + ")" + " " + contactNumber);
        telecomNumberMap.put("countryCode", countryCode);
        telecomNumberMap.put("areaCode", areaCode);
        telecomNumberMap.put("contactNumber", contactNumber);
        telecomNumberMap.put("extension", extension);
        telecomNumberMap.put("partyContactMechPurposes", partyContactMechPurposes);

        // String formattedPhoneNumberInUSFormat = getPhoneNumberInUSFormat(countryCode, areaCode,
        // contactNumber, extension);
        String formattedPhoneNumberInUSFormat =
            getFormattedPhoneNumber(
                delegator, contactMechId, countryCode, areaCode, contactNumber, extension);
        telecomNumberMap.put("formattedPhoneNumberInUSFormat", formattedPhoneNumberInUSFormat);
        telecomNumberMap.put("partyRegion", partyRegion);

        telecomNumbers.add(telecomNumberMap);
      }
    }
    return telecomNumbers;
  }

  public static List<EmailAddress> getEmailAddresses(Delegator delegator, String partyId)
      throws GenericEntityException {
    GenericValue party = HierarchyUtils.getPartyByPartyId(delegator, partyId);
    List<EmailAddress> emailAddresses = FastList.newInstance();
    String contactMechTypeId = "EMAIL_ADDRESS";

    Collection<GenericValue> emailAddressesList =
        ContactHelper.getContactMechByType(party, contactMechTypeId, false);
    for (GenericValue emailAddress : emailAddressesList) {

      EmailAddress emailAddressMap = new EmailAddress();
      emailAddressMap.setId(emailAddress.getString("contactMechId"));
      emailAddressMap.setEmailAddress(emailAddress.getString("infoString"));
      emailAddresses.add(emailAddressMap);
    }

    return emailAddresses;
  }

  public static List<Phone> getPartyContactNumbers(Delegator delegator, String partyId)
      throws GenericEntityException {
    GenericValue party = HierarchyUtils.getPartyByPartyId(delegator, partyId);
    List<Phone> telecomNumbers = FastList.newInstance();
    String contactMechTypeId = "TELECOM_NUMBER";
    Collection<GenericValue> partyTelecomNumbers =
        ContactHelper.getContactMechByType(party, contactMechTypeId, false);
    for (GenericValue partyTelecomNumber : partyTelecomNumbers) {

      GenericValue telecomNumber = partyTelecomNumber.getRelatedOne("TelecomNumber", false);
      GenericValue partyContactMech = null;

      partyContactMech =
          EntityQuery.use(delegator)
              .from("PartyContactMech")
              .where(
                  "contactMechId",
                  partyTelecomNumber.getString("contactMechId"),
                  "partyId",
                  party.getString("partyId"))
              .cache(false)
              .queryFirst();
      if (UtilValidate.isNotEmpty(telecomNumber)) {

        String contactMechId = (String) telecomNumber.get("contactMechId");
        String countryCode = (String) telecomNumber.get("countryCode");
        String areaCode = (String) telecomNumber.get("areaCode");
        String contactNumber = (String) telecomNumber.get("contactNumber");
        String extension = partyContactMech.getString("extension");

        Phone telecomNumberMap = new Phone();
        telecomNumberMap.setId(contactMechId);
        telecomNumberMap.setCountryCode(countryCode);
        telecomNumberMap.setAreaCode(areaCode);
        telecomNumberMap.setPhone(contactNumber);
        telecomNumberMap.setExtension(extension);

        telecomNumbers.add(telecomNumberMap);
      }
    }
    return telecomNumbers;
  }

  public static List<GenericValue> getPartyContactNumbers(Delegator delegator, GenericValue party)
      throws GenericEntityException {
    List telecomNumbers = FastList.newInstance();
    String contactMechTypeId = "TELECOM_NUMBER";
    Collection<GenericValue> partyTelecomNumbers =
        ContactHelper.getContactMechByType(party, contactMechTypeId, false);
    for (GenericValue partyTelecomNumber : partyTelecomNumbers) {

      GenericValue telecomNumber = partyTelecomNumber.getRelatedOne("TelecomNumber", false);
      GenericValue partyContactMech = null;

      partyContactMech =
          EntityQuery.use(delegator)
              .from("PartyContactMech")
              .where(
                  "contactMechId",
                  partyTelecomNumber.getString("contactMechId"),
                  "partyId",
                  party.getString("partyId"))
              .cache(false)
              .queryFirst();
      if (UtilValidate.isNotEmpty(telecomNumber)) {

        String contactMechId = (String) telecomNumber.get("contactMechId");
        String countryCode = (String) telecomNumber.get("countryCode");
        String areaCode = (String) telecomNumber.get("areaCode");
        String contactNumber = (String) telecomNumber.get("contactNumber");
        String extension = partyContactMech.getString("extension");

        Map telecomNumberMap = FastMap.newInstance();
        telecomNumberMap.put("contactMechId", contactMechId);
        telecomNumberMap.put("countryCode", countryCode);
        telecomNumberMap.put("areaCode", areaCode);
        telecomNumberMap.put("contactNumber", contactNumber);
        telecomNumberMap.put("extension", extension);

        // String formattedPhoneNumberInUSFormat = getPhoneNumberInUSFormat(countryCode, areaCode,
        // contactNumber, extension);
        String formattedPhoneNumberInUSFormat =
            getFormattedPhoneNumber(
                delegator, contactMechId, countryCode, areaCode, contactNumber, extension);

        telecomNumberMap.put("formattedPhoneNumberInUSFormat", formattedPhoneNumberInUSFormat);

        telecomNumbers.add(telecomNumberMap);
      }
    }
    return telecomNumbers;
  }

  public static String getPhoneNumberInUSFormat(
      String countryCode, String areaCode, String contactNumber, String extension) {
    String formattedPhoneNumber = "";

    if (UtilValidate.isEmpty(areaCode) || UtilValidate.isEmpty(contactNumber)) return null;

    String rawString = countryCode + "-" + areaCode + "-" + contactNumber;
    PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
    PhoneNumber number = null;
    try {
      number = phoneUtil.parse(rawString, "US");
    } catch (NumberParseException e) {
      Debug.logWarning(e, module);
      return null;
    }
    if (UtilValidate.isNotEmpty(extension)) number.setExtension(extension);

    formattedPhoneNumber =
        PhoneNumberUtil.getInstance().formatNationalNumberWithPreferredCarrierCode(number, "");

    return formattedPhoneNumber;
  }

  public static String getFormattedPhoneNumber(
      Delegator delegator,
      String contactMechId,
      String countryCode,
      String areaCode,
      String contactNumber,
      String extension) {
    String formattedPhoneNumber = "";
    if (UtilValidate.isEmpty(countryCode)) {
      countryCode = "1";
    }
    String partyRegion = "";
    if (UtilValidate.isEmpty(areaCode) || UtilValidate.isEmpty(contactNumber)) {
      return null;
    }
    try {
      GenericValue partyRegionDetails =
          EntityQuery.use(delegator)
              .from("UserPreference")
              .where(
                  "userLoginId",
                  contactMechId,
                  "userPrefTypeId",
                  "defaultCountryGeoId",
                  "userPrefGroupTypeId",
                  "APPL_PREFERENCES")
              .queryOne();
      if (UtilValidate.isNotEmpty(partyRegionDetails)) {
        partyRegion = partyRegionDetails.getString("userPrefValue");
      }
      if (UtilValidate.isEmpty(partyRegion)) {
        partyRegion = "US"; // default
      }
    } catch (GenericEntityException e) {
      Debug.logError(e, module);
    }
    String rawString = countryCode + "-" + areaCode + "-" + contactNumber;
    PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
    PhoneNumber number = null;
    try {
      number = phoneUtil.parse(rawString, partyRegion);
    } catch (NumberParseException e) {
      Debug.logWarning(e, module);
      return null;
    }
    if (UtilValidate.isNotEmpty(extension)) {
      number.setExtension(extension);
    }
    formattedPhoneNumber =
        phoneUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);
    return formattedPhoneNumber;
  }

  public static List<GenericValue> getPartyEmailAddresses(
      Delegator delegator, GenericValue party, String contactMechPurposeTypeId)
      throws GenericEntityException {
    List emailAddresses = FastList.newInstance();
    String contactMechTypeId = "EMAIL_ADDRESS";

    Collection<GenericValue> emailAddressesList =
        ContactHelper.getContactMechByType(party, contactMechTypeId, false);
    for (GenericValue emailAddress : emailAddressesList) {
      GenericValue partyContactMech = null;

      partyContactMech =
          EntityQuery.use(delegator)
              .from("PartyContactMech")
              .where(
                  "contactMechId",
                  emailAddress.getString("contactMechId"),
                  "partyId",
                  party.getString("partyId"))
              .cache(false)
              .queryFirst();

      List<GenericValue> partyContactMechPurposes = FastList.newInstance();
      if (UtilValidate.isNotEmpty(contactMechPurposeTypeId)) {
        partyContactMechPurposes =
            EntityUtil.filterByDate(
                partyContactMech.getRelated(
                    "PartyContactMechPurpose",
                    UtilMisc.toMap("contactMechPurposeTypeId", contactMechPurposeTypeId),
                    null,
                    false),
                true);
      } else {
        partyContactMechPurposes =
            EntityUtil.filterByDate(
                partyContactMech.getRelated("PartyContactMechPurpose", null, null, false), true);
      }
      Map emailAddressMap = FastMap.newInstance();
      emailAddressMap.put("contactMechId", emailAddress.getString("contactMechId"));
      emailAddressMap.put("infoString", emailAddress.getString("infoString"));
      emailAddressMap.put("partyContactMechPurposes", partyContactMechPurposes);

      emailAddresses.add(emailAddressMap);
    }

    return emailAddresses;
  }

  public static List<GenericValue> getEmailAddresses(GenericValue party)
      throws GenericEntityException {
    List emailAddresses = FastList.newInstance();
    String contactMechTypeId = "EMAIL_ADDRESS";

    Collection<GenericValue> emailAddressesList =
        ContactHelper.getContactMechByType(party, contactMechTypeId, false);
    for (GenericValue emailAddress : emailAddressesList) {

      Map emailAddressMap = FastMap.newInstance();
      emailAddressMap.put("contactMechId", emailAddress.getString("contactMechId"));
      emailAddressMap.put("email", emailAddress.getString("infoString"));
      emailAddresses.add(emailAddressMap);
    }

    return emailAddresses;
  }

  public static List<GenericValue> getPartyPostalAddresses(
      Delegator delegator, GenericValue party, String contactMechPurposeTypeId)
      throws GenericEntityException {
    List postalAddresses = FastList.newInstance();
    String contactMechTypeId = "POSTAL_ADDRESS";
    Collection<GenericValue> partyPostalAddresses =
        ContactHelper.getContactMechByType(party, contactMechTypeId, false);
    for (GenericValue partyPostalAddress : CollectionUtils.emptyIfNull(partyPostalAddresses)) {

      GenericValue postalAddress = partyPostalAddress.getRelatedOne("PostalAddress", false);
      GenericValue partyContactMech = null;

      partyContactMech =
          EntityQuery.use(delegator)
              .from("PartyContactMech")
              .where(
                  "contactMechId",
                  partyPostalAddress.getString("contactMechId"),
                  "partyId",
                  party.getString("partyId"))
              .cache(false)
              .queryFirst();
      if (UtilValidate.isNotEmpty(postalAddress)) {
        List<GenericValue> partyContactMechPurposes = FastList.newInstance();
        if (UtilValidate.isNotEmpty(contactMechPurposeTypeId)) {
          partyContactMechPurposes =
              EntityUtil.filterByDate(
                  partyContactMech.getRelated(
                      "PartyContactMechPurpose",
                      UtilMisc.toMap("contactMechPurposeTypeId", contactMechPurposeTypeId),
                      null,
                      false),
                  true);
        } else {
          partyContactMechPurposes =
              EntityUtil.filterByDate(
                  partyContactMech.getRelated("PartyContactMechPurpose", null, null, false), true);
        }

        Map postalAddressMap = FastMap.newInstance();
        postalAddressMap.put("contactMechId", postalAddress.getString("contactMechId"));
        postalAddressMap.put("toName", postalAddress.getString("toName"));
        postalAddressMap.put("attnName", postalAddress.getString("attnName"));
        postalAddressMap.put("address1", postalAddress.getString("address1"));
        postalAddressMap.put("address2", postalAddress.getString("address2"));
        postalAddressMap.put("city", postalAddress.getString("city"));
        postalAddressMap.put("stateProvinceGeoId", postalAddress.getString("stateProvinceGeoId"));
        postalAddressMap.put("postalCode", postalAddress.getString("postalCode"));
        postalAddressMap.put("countryGeoId", postalAddress.getString("countryGeoId"));
        postalAddressMap.put("partyContactMechPurposes", partyContactMechPurposes);

        postalAddressMap.put("formattedAddress", postalAddress.getString("formattedAddress"));
        postalAddressMap.put("googleUrl", postalAddress.getString("googleUrl"));
        postalAddressMap.put("timeZoneId", postalAddress.getString("timeZoneId"));

        postalAddresses.add(postalAddressMap);
      }
    }
    return postalAddresses;
  }

  public static List<GenericValue> getPartyWebAddresses(Delegator delegator, GenericValue party)
      throws GenericEntityException {
    List webAddresses = FastList.newInstance();
    String contactMechTypeId = "WEB_ADDRESS";
    Collection<GenericValue> partyWebAddresses =
        ContactHelper.getContactMechByType(party, contactMechTypeId, false);
    for (GenericValue webAddress : partyWebAddresses) {
      GenericValue partyContactMech = null;
      partyContactMech =
          EntityQuery.use(delegator)
              .from("ContactMech")
              .where("contactMechId", webAddress.getString("contactMechId"))
              .cache(false)
              .queryFirst();
      if (UtilValidate.isNotEmpty(partyContactMech)) {

        Map webAddressMap = FastMap.newInstance();
        webAddressMap.put("contactMechId", partyContactMech.getString("contactMechId"));
        webAddressMap.put("infoString", partyContactMech.getString("infoString"));
        webAddresses.add(webAddressMap);
      }
    }
    return webAddresses;
  }

  public static List<GenericValue> getPartylinkedInAddresses(
      Delegator delegator, GenericValue party) throws GenericEntityException {
    List linkedInAddress = FastList.newInstance();
    String contactMechTypeId = "LINKED_ID";
    Collection<GenericValue> partyLinkedInAddresses =
        ContactHelper.getContactMechByType(party, contactMechTypeId, false);
    for (GenericValue linkedAddress : partyLinkedInAddresses) {
      GenericValue partyContactMech = null;
      partyContactMech =
          EntityQuery.use(delegator)
              .from("ContactMech")
              .where("contactMechId", linkedAddress.getString("contactMechId"))
              .cache(false)
              .queryFirst();
      if (UtilValidate.isNotEmpty(partyContactMech)) {

        Map linkedInAddressMap = FastMap.newInstance();
        linkedInAddressMap.put("contactMechId", partyContactMech.getString("contactMechId"));
        linkedInAddressMap.put("infoString", partyContactMech.getString("infoString"));
        linkedInAddress.add(linkedInAddressMap);
      }
    }
    return linkedInAddress;
  }

  public static List<GenericValue> getPartyFacebookAddresses(
      Delegator delegator, GenericValue party) throws GenericEntityException {
    List facebookAddress = FastList.newInstance();
    String contactMechPurposeTypeId = "FACEBOOK_URL";
    Collection<GenericValue> partyFacebookAddresses =
        ContactHelper.getContactMechByPurpose(party, contactMechPurposeTypeId, false);
    for (GenericValue fbAddress : partyFacebookAddresses) {
      GenericValue partyContactMech = null;
      partyContactMech =
          EntityQuery.use(delegator)
              .from("ContactMech")
              .where("contactMechId", fbAddress.getString("contactMechId"))
              .cache(false)
              .queryFirst();
      if (UtilValidate.isNotEmpty(partyContactMech)) {

        Map fbAddressMap = FastMap.newInstance();
        fbAddressMap.put("contactMechId", partyContactMech.getString("contactMechId"));
        fbAddressMap.put("infoString", partyContactMech.getString("infoString"));
        facebookAddress.add(fbAddressMap);
      }
    }
    return facebookAddress;
  }

  public static List<GenericValue> getPostalAddresses(Delegator delegator, GenericValue party)
      throws GenericEntityException {
    List postalAddresses = FastList.newInstance();
    String contactMechTypeId = "POSTAL_ADDRESS";
    Collection<GenericValue> partyPostalAddresses =
        ContactHelper.getContactMechByType(party, contactMechTypeId, false);
    for (GenericValue partyPostalAddress : partyPostalAddresses) {

      GenericValue postalAddress = partyPostalAddress.getRelatedOne("PostalAddress", false);
      if (UtilValidate.isNotEmpty(postalAddress)) {
        Map postalAddressMap = FastMap.newInstance();
        postalAddressMap.put("contactMechId", postalAddress.getString("contactMechId"));
        postalAddressMap.put("toName", postalAddress.getString("toName"));
        postalAddressMap.put("attnName", postalAddress.getString("attnName"));
        postalAddressMap.put("address1", postalAddress.getString("address1"));
        postalAddressMap.put("address2", postalAddress.getString("address2"));
        postalAddressMap.put("city", postalAddress.getString("city"));
        postalAddressMap.put("stateProvinceGeoId", postalAddress.getString("stateProvinceGeoId"));
        postalAddressMap.put("postalCode", postalAddress.getString("postalCode"));
        postalAddressMap.put("countryGeoId", postalAddress.getString("countryGeoId"));
        postalAddressMap.put("directions", postalAddress.getString("directions"));

        GenericValue partyContactMechPurpose =
            EntityQuery.use(delegator)
                .select("contactMechPurposeTypeId")
                .from("PartyContactMechPurpose")
                .where(
                    "partyId",
                    party.getString("partyId"),
                    "contactMechId",
                    postalAddress.getString("contactMechId"))
                .filterByDate()
                .queryFirst();
        if (UtilValidate.isNotEmpty(partyContactMechPurpose)) {
          postalAddressMap.put(
              "contactMechPurposeTypeId",
              partyContactMechPurpose.getString("contactMechPurposeTypeId"));
        }

        // check whether phone no is primary or not
        GenericValue primaryPurposeRecord =
            EntityQuery.use(delegator)
                .from("PartyContactMechPurpose")
                .where(
                    "partyId",
                    party.getString("partyId"),
                    "contactMechId",
                    postalAddress.getString("contactMechId"),
                    "contactMechPurposeTypeId",
                    PostalAddressTypesEnum.PRIMARY.getTypeId())
                .filterByDate()
                .queryOne();

        if (UtilValidate.isNotEmpty(primaryPurposeRecord)) {
          postalAddressMap.put("isPrimary", true);
        } else {
          postalAddressMap.put("isPrimary", false);
        }
        postalAddresses.add(postalAddressMap);
      }
    }
    return postalAddresses;
  }

  public static GenericValue findPartyLatestTelecomNumber(String partyId, Delegator delegator) {
    GenericValue pcm = PartyWorker.findPartyLatestContactMech(partyId, "TELECOM_NUMBER", delegator);
    return pcm;
  }

  public static GenericValue findPartyLatestEmailAddress(String partyId, Delegator delegator) {
    GenericValue pcm = PartyWorker.findPartyLatestContactMech(partyId, "EMAIL_ADDRESS", delegator);
    return pcm;
  }

  public static GenericValue findPartyLatestWebAddress(String partyId, Delegator delegator) {
    GenericValue pcm = PartyWorker.findPartyLatestContactMech(partyId, "WEB_ADDRESS", delegator);
    return pcm;
  }

  /**
   * Returns ClassificationCode description for the code.
   *
   * @param delegator
   * @param code
   * @return
   */
  public static String getClassificationCodeDescription(Delegator delegator, String code) {
    GenericValue classCodeDesc = null;
    try {
      classCodeDesc =
          EntityQuery.use(delegator)
              .select("description")
              .from("FarClassificationCode")
              .where("code", code)
              .queryOne();
    } catch (GenericEntityException e) {
      Debug.logError(e, "Error finding FarClassificationCode", module);
    }
    if (classCodeDesc == null) {
      return null;
    } else {
      return classCodeDesc.getString("description");
    }
  }

  /**
   * Returns ClassificationCode description for the code.
   *
   * @param delegator
   * @param code
   * @return
   */
  public static String getNaicsCodeDescription(Delegator delegator, String code) {
    GenericValue naicsCodeDesc = null;
    try {
      naicsCodeDesc =
          EntityQuery.use(delegator)
              .select("description")
              .from("NaicsCode")
              .where("code", code)
              .queryOne();
    } catch (GenericEntityException e) {
      Debug.logError(e, "Error finding NaicsCode", module);
    }
    if (naicsCodeDesc == null) {
      return null;
    } else {
      return naicsCodeDesc.getString("description");
    }
  }

  /**
   * Returns TaskCategory and Color for the taskCategoryId.
   *
   * @param delegator
   * @param taskCategoryId
   * @return
   */
  public static Map getTaskCategoryAndColor(Delegator delegator, String taskCategoryId)
      throws GenericEntityException {
    String taskCategoryName = "Undefined Task Category";
    String taskCategoryColor = "fsd-label-neutralTertiary";
    String taskCategoryColorHashCode = "#a6a6a6";
    Map taskDetailsMap = FastMap.newInstance();

    if (UtilValidate.isNotEmpty(taskCategoryId)) {
      GenericValue dataCategory =
          EntityQuery.use(delegator)
              .from("DataCategory")
              .where("dataCategoryId", taskCategoryId)
              .queryOne();
      if (UtilValidate.isNotEmpty(dataCategory)) {
        if (UtilValidate.isNotEmpty(dataCategory.get("categoryName"))) {
          taskCategoryName = dataCategory.get("categoryName").toString();
        }

        if (taskCategoryId.equals("TC_HR")) {
          taskCategoryColor = "fsd-label-tealLight";
          taskCategoryColorHashCode = "#00B294";
        } else if (taskCategoryId.equals("TC_MARKETING")) {
          taskCategoryColor = "fsd-label-primary";
          taskCategoryColorHashCode = "#0078d7";
        } else if (taskCategoryId.equals("TC_IT")) {
          taskCategoryColor = "fsd-label-purple";
          taskCategoryColorHashCode = "#5c2d91";
        } else if (taskCategoryId.equals("TC_SALES")) {
          taskCategoryColor = "fsd-label-green";
          taskCategoryColorHashCode = "#107c10";
        } else if (taskCategoryId.equals("TC_FINANCE")) {
          taskCategoryColor = "fsd-label-orangeLighter";
          taskCategoryColorHashCode = "#ff8c00";
        } else if (taskCategoryId.equals("TC_MISC")) {
          taskCategoryColor = "fsd-label-orange";
          taskCategoryColorHashCode = "#d83b01";
        } else if (taskCategoryId.equals("TC_ADMIN")) {
          taskCategoryColor = "fsd-label-magenta";
          taskCategoryColorHashCode = "#b4009e";
        } else if (taskCategoryId.equals("TC_PRSNL")) {
          taskCategoryColor = "fsd-label-olive";
          taskCategoryColorHashCode = "#808000";
        } else if (taskCategoryId.equals("TC_TRNG")) {
          taskCategoryColor = "fsd-label-blueDark";
          taskCategoryColorHashCode = "#002050";
        } else if (taskCategoryId.equals("10000")) {
          taskCategoryColor = "fsd-label-greenLight";
          taskCategoryColorHashCode = "#bad80a";
        }
      }
    }

    taskDetailsMap.put("taskCategoryName", taskCategoryName);
    taskDetailsMap.put("taskCategoryColor", taskCategoryColor);
    taskDetailsMap.put("taskCategoryColorHashCode", taskCategoryColorHashCode);
    return taskDetailsMap;
  }

  /**
   * To get category color based on categoryId.
   *
   * @param delegator
   * @param taskCategoryId
   * @return
   * @throws GenericEntityException
   */
  public static String getTaskCategoryColor(Delegator delegator, String taskCategoryId)
      throws GenericEntityException {

    GenericValue categories =
        EntityQuery.use(delegator)
            .from("DataCategory")
            .where("parentCategoryId", "TASK_CATEGORY", "dataCategoryId", taskCategoryId)
            .queryOne();

    String colorId = "";

    if (UtilValidate.isNotEmpty(categories)) {

      colorId = (String) categories.get("colorId");
    }

    return colorId;
  }

  /**
   * Returns List of fsd employees.
   *
   * @param delegator
   * @param dispatcher
   * @return
   */
  public static List<Map> getFsdEmployees(
      GenericValue userLogin, Delegator delegator, LocalDispatcher dispatcher)
      throws GenericEntityException, GenericServiceException {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    List<Map> fsdEmployees = FastList.newInstance();
    List<String> employees = FastList.newInstance();
    String userLoginId = (String) userLogin.get("userLoginId");

    String partyGroupPartyId = UtilProperties.getPropertyValue("general", "ORGANIZATION_PARTY");

    Map<String, Object> ctx = FastMap.newInstance();
    ctx.put("userLogin", userLogin);
    ctx.put("partyGroupPartyId", partyGroupPartyId);
    Map<String, Object> employeePersonsServiceResponse =
        dispatcher.runSync("getEmployeePersons", ctx);
    if (ServiceUtil.isSuccess(employeePersonsServiceResponse)) {
      List<GenericValue> personsList =
          (List<GenericValue>) employeePersonsServiceResponse.get("employeePersonsList");
      for (GenericValue person : personsList) {
        if (!userLoginId.equals((String) person.get("partyId"))) {
          employees.add((String) person.get("partyId"));
        }
      }
    }

    for (String employee : employees) {
      String displayName = AxPartyHelper.getPartyName(delegator, employee);
      GenericValue party =
          EntityQuery.use(delegator).from("Party").where("partyId", employee).queryOne();
      GenericValue partyEmail =
          EntityUtil.getFirst(ContactHelper.getContactMechByType(party, "EMAIL_ADDRESS", false));
      String email = "";
      Map<String, Object> personMap = FastMap.newInstance();
      if (UtilValidate.isNotEmpty(partyEmail)) {
        email = partyEmail.getString("infoString");
        personMap.put("partyId", employee);
        personMap.put("displayName", displayName);
        personMap.put("email", email);
      }
      fsdEmployees.add(personMap);
    }

    return fsdEmployees;
  }

  /**
   * Returns employees of an Org
   *
   * @param orgPartyId
   * @param userLogin
   * @param delegator
   * @param dispatcher
   * @return
   * @throws GenericEntityException
   * @throws GenericServiceException
   */
  public static List<Map> getOrgEmployees(
      String orgPartyId, GenericValue userLogin, Delegator delegator, LocalDispatcher dispatcher)
      throws GenericEntityException, GenericServiceException {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    List<Map> fsdEmployees = FastList.newInstance();
    List<String> employees = FastList.newInstance();
    String userLoginId = (String) userLogin.get("userLoginId");

    String partyGroupPartyId = orgPartyId;

    Map<String, Object> ctx = FastMap.newInstance();
    ctx.put("userLogin", userLogin);
    ctx.put("partyGroupPartyId", partyGroupPartyId);
    Map<String, Object> employeePersonsServiceResponse =
        dispatcher.runSync("getEmployeePersons", ctx);
    if (ServiceUtil.isSuccess(employeePersonsServiceResponse)) {
      List<GenericValue> personsList =
          (List<GenericValue>) employeePersonsServiceResponse.get("employeePersonsList");
      for (GenericValue person : personsList) {
        if (userLoginId.equals((String) person.get("partyId"))) {
          employees.add(0, (String) person.get("partyId"));
        } else {
          employees.add((String) person.get("partyId"));
        }
      }
    }

    for (String employee : employees) {
      String displayName = AxPartyHelper.getPartyName(delegator, employee);
      GenericValue party =
          EntityQuery.use(delegator).from("Party").where("partyId", employee).queryOne();
      GenericValue partyEmail =
          EntityUtil.getFirst(ContactHelper.getContactMechByType(party, "EMAIL_ADDRESS", false));
      String email = "";
      Map<String, Object> personMap = FastMap.newInstance();
      if (UtilValidate.isNotEmpty(partyEmail)) {
        email = partyEmail.getString("infoString");
        Map partyInfo = getPartyNameAndLogoDetails(party, false);
        personMap.put("partyId", employee);
        personMap.put("displayName", partyInfo.get("partyName"));
        personMap.put("partyName", partyInfo.get("partyName"));
        personMap.put("partyInitials", partyInfo.get("partyInitials"));
        personMap.put("partyLogoContentId", partyInfo.get("partyLogoContentId"));
        personMap.put("email", email);
      }
      fsdEmployees.add(personMap);
    }

    return fsdEmployees;
  }

  /**
   * Returns application support recipients
   *
   * @param userLogin
   * @param delegator
   * @param dispatcher
   * @return
   * @throws GenericEntityException
   * @throws GenericServiceException
   */
  public static List<Map> getAppSupportRecipients(
      GenericValue userLogin, Delegator delegator, LocalDispatcher dispatcher)
      throws GenericEntityException, GenericServiceException {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    List<Map> supportRecipients = FastList.newInstance();
    List<String> parties = FastList.newInstance();
    String userLoginId = (String) userLogin.get("userLoginId");

    List<GenericValue> supportParties =
        EntityQuery.use(delegator)
            .from("PartyRole")
            .where("roleTypeId", "APP_SUPPORT_PARTY")
            .queryList();
    if (UtilValidate.isNotEmpty(supportParties)) {
      for (GenericValue employee : supportParties) {
        String displayName = AxPartyHelper.getPartyName(delegator, employee.getString("partyId"));
        GenericValue party =
            EntityQuery.use(delegator)
                .from("Party")
                .where("partyId", employee.getString("partyId"))
                .queryOne();
        GenericValue partyEmail =
            EntityUtil.getFirst(ContactHelper.getContactMechByType(party, "EMAIL_ADDRESS", false));
        String email = "";
        Map<String, Object> personMap = FastMap.newInstance();
        if (UtilValidate.isNotEmpty(partyEmail)) {
          email = partyEmail.getString("infoString");
          Map partyInfo = getPartyNameAndLogoDetails(party, false);
          personMap.put("partyId", employee.getString("partyId"));
          personMap.put("displayName", partyInfo.get("partyName"));
          personMap.put("partyName", partyInfo.get("partyName"));
          personMap.put("partyInitials", partyInfo.get("partyInitials"));
          personMap.put("partyLogoContentId", partyInfo.get("partyLogoContentId"));
          personMap.put("email", email);
        }
        supportRecipients.add(personMap);
      }
    }

    return supportRecipients;
  }

  static final String[] colorCodes = {
    "#039BE5", "#3F51B5", "#7986CB", "#8E24AA", "#616161", "#D50000", "#E67C73", "#F4511E",
    "#F6BF26", "#33B679", "#0B8043"
  };

  public static Map generateRandomColorsForParties(List<String> partyIds) {
    Map partyToColorCodeMap = FastMap.newInstance();

    int index = 0;
    for (String partyId : partyIds) {
      partyToColorCodeMap.put(partyId, colorCodes[index]);
      index++;
      if (index >= colorCodes.length) {
        index = 0;
      }
    }

    return partyToColorCodeMap;
  }

  /**
   * Returns last engaged formatted date.
   *
   * @param partyId
   * @return
   */
  public static String getLastEngagedDateWithParty(LocalDispatcher dispatcher, String partyId)
      throws GenericServiceException {
    String lastEngagedFormattedDateStr = "";
    List<String> searchFields = FastList.newInstance();
    String partyIdConstraint = "partyId:" + partyId;
    List<String> filterQueryFieldsForParty = UtilMisc.toList("docType:party", partyIdConstraint);
    Map performSolrSearchForParty =
        UtilMisc.toMap(
            "searchFields", searchFields, "filterQueryFields", filterQueryFieldsForParty);
    Map performSolrSearchForPartyResponse =
        dispatcher.runSync("performSolrSearch", performSolrSearchForParty);

    if (ServiceUtil.isError(performSolrSearchForPartyResponse)) {
      Debug.logError(
          "An error occurred while performing solr search, details: "
              + ServiceUtil.getErrorMessage(performSolrSearchForPartyResponse),
          module);
    }
    List<Map> partyEntries = (List) performSolrSearchForPartyResponse.get("records");
    for (Map record : partyEntries) {
      Date lastEngagedDate = (Date) record.get("lastEngagedDate");
      if (UtilValidate.isNotEmpty(lastEngagedDate)) {
        Timestamp lastEngagedDateTs = new Timestamp(lastEngagedDate.getTime());
        if (UtilValidate.isNotEmpty(lastEngagedDateTs)) {
          lastEngagedFormattedDateStr = AxUtilFormat.formatDate(lastEngagedDateTs);
        }
      }
    }
    return lastEngagedFormattedDateStr;
  }

  /**
   * Returns last engaged event type.
   *
   * @param partyId
   * @return
   */
  public static String getLastEngagedEventWithParty(LocalDispatcher dispatcher, String partyId)
      throws GenericServiceException {
    String lastEngagedEventStr = "";
    List<String> searchFields = FastList.newInstance();
    String partyIdConstraint = "partyId:" + partyId;
    List<String> filterQueryFieldsForParty = UtilMisc.toList("docType:party", partyIdConstraint);
    Map performSolrSearchForParty =
        UtilMisc.toMap(
            "searchFields", searchFields, "filterQueryFields", filterQueryFieldsForParty);
    Map performSolrSearchForPartyResponse =
        dispatcher.runSync("performSolrSearch", performSolrSearchForParty);

    if (ServiceUtil.isError(performSolrSearchForPartyResponse)) {
      Debug.logError(
          "An error occurred while performing solr search, details: "
              + ServiceUtil.getErrorMessage(performSolrSearchForPartyResponse),
          module);
    }
    List<Map> partyEntries = (List) performSolrSearchForPartyResponse.get("records");
    for (Map record : partyEntries) {
      lastEngagedEventStr = (String) record.get("lastEngagedEventTypeId");
    }
    return lastEngagedEventStr;
  }

  /**
   * Returns last engaged party.
   *
   * @param partyId
   * @return
   */
  public static String getLastEngagedParty(LocalDispatcher dispatcher, String partyId)
      throws GenericServiceException {
    String lastEngagedWithParty = "";
    List<String> searchFields = FastList.newInstance();
    String partyIdConstraint = "partyId:" + partyId;
    List<String> filterQueryFieldsForParty = UtilMisc.toList("docType:party", partyIdConstraint);
    Map performSolrSearchForParty =
        UtilMisc.toMap(
            "searchFields", searchFields, "filterQueryFields", filterQueryFieldsForParty);
    Map performSolrSearchForPartyResponse =
        dispatcher.runSync("performSolrSearch", performSolrSearchForParty);

    if (ServiceUtil.isError(performSolrSearchForPartyResponse)) {
      Debug.logError(
          "An error occurred while performing solr search, details: "
              + ServiceUtil.getErrorMessage(performSolrSearchForPartyResponse),
          module);
    }
    List<Map> partyEntries = (List) performSolrSearchForPartyResponse.get("records");
    for (Map record : partyEntries) {
      lastEngagedWithParty = (String) record.get("lastEngagedWithParty");
    }
    return lastEngagedWithParty;
  }

  public static String checkPhoneNumberExist(
      Delegator delegator,
      String countryCode,
      String areaCode,
      String contactNumber,
      String extension) {
    List<GenericValue> phones = null;
    try {
      phones =
          EntityQuery.use(delegator)
              .from("TelecomNumber")
              .where(
                  "countryCode", countryCode, "areaCode", areaCode, "contactNumber", contactNumber)
              .queryList();
    } catch (GenericEntityException e) {
      Debug.logError(e, module);
    }
    if (UtilValidate.isNotEmpty(phones)) {
      if (UtilValidate.isNotEmpty(extension)) {
        for (GenericValue phone : phones) {
          String contactMechId = phone.getString("contactMechId");
          GenericValue partyContactMech = null;
          try {
            partyContactMech =
                EntityQuery.use(delegator)
                    .from("PartyContactMech")
                    .where("contactMechId", contactMechId, "extension", extension)
                    .queryFirst();
          } catch (GenericEntityException e) {
            Debug.logError(e, module);
          }
          if (UtilValidate.isNotEmpty(partyContactMech)) {
            String existingExtension = partyContactMech.getString("extension");
            if (existingExtension.equals(extension)) {
              return "Phone number with this extension is already exist, Please try another.";
            }
          }
        }
      } else {
        return "Phone number already exist, Please try another.";
      }
    }
    return null;
  }

  public static String isPhoneNumberExist(
      Delegator delegator,
      String countryCode,
      String areaCode,
      String contactNumber,
      String extension) {
    List<GenericValue> phones = null;
    GenericValue partyContactMech = null;
    String partyId = "";
    String contactMechId = "";
    try {
      phones =
          EntityQuery.use(delegator)
              .from("TelecomNumber")
              .where(
                  "countryCode", countryCode, "areaCode", areaCode, "contactNumber", contactNumber)
              .queryList();
    } catch (GenericEntityException e) {
      Debug.logError(e, module);
    }
    if (UtilValidate.isNotEmpty(phones)) {
      if (UtilValidate.isNotEmpty(extension)) {
        for (GenericValue phone : phones) {
          contactMechId = phone.getString("contactMechId");
          try {
            partyContactMech =
                EntityQuery.use(delegator)
                    .from("PartyContactMech")
                    .where("contactMechId", contactMechId, "extension", extension)
                    .filterByDate()
                    .queryOne();
          } catch (GenericEntityException e) {
            Debug.logError(e, module);
          }
          if (UtilValidate.isNotEmpty(partyContactMech)) {
            String existingExtension = partyContactMech.getString("extension");
            partyId = partyContactMech.getString("partyId");
            if (existingExtension.equals(extension)) {
              return partyId;
            }
          }
        }
      } else {
        contactMechId = phones.get(0).getString("contactMechId");
        try {
          partyContactMech =
              EntityQuery.use(delegator)
                  .from("PartyContactMech")
                  .where("contactMechId", contactMechId)
                  .filterByDate()
                  .queryOne();
        } catch (GenericEntityException e) {
          Debug.logError(e, module);
        }
        if (UtilValidate.isNotEmpty(partyContactMech)) {
          partyId = partyContactMech.getString("partyId");
          return partyId;
        }
      }
    }
    return null;
  }

  public static String isEmailExist(Delegator delegator, String email) {
    GenericValue custEmail = null;
    String partyId = "";
    try {
      custEmail =
          EntityQuery.use(delegator)
              .from("ContactMech")
              .where("contactMechTypeId", "EMAIL_ADDRESS", "infoString", email)
              .queryFirst();
    } catch (GenericEntityException e) {
      Debug.logError(e, module);
    }
    if (UtilValidate.isNotEmpty(custEmail)) {
      String contactMechId = custEmail.getString("contactMechId");
      GenericValue partyDetails = null;
      try {
        partyDetails =
            EntityQuery.use(delegator)
                .from("PartyContactMech")
                .where("contactMechId", contactMechId)
                .filterByDate()
                .queryOne();
        if (UtilValidate.isNotEmpty(partyDetails)) {
          partyId = partyDetails.getString("partyId");
        }
      } catch (GenericEntityException e) {
        Debug.logError(e, module);
      }
      return partyId;
    }
    return null;
  }

  /**
   * Returns the type of party.
   *
   * @param partyId
   * @return
   */
  public static String getPartyType(LocalDispatcher dispatcher, String partyId)
      throws GenericServiceException {
    String partyType = "";
    List<String> searchFields = FastList.newInstance();
    String partyIdConstraint = "partyId:" + partyId;
    List<String> filterQueryFieldsForParty = UtilMisc.toList("docType:party", partyIdConstraint);
    Map<String, Object> performSolrSearchForParty =
        UtilMisc.toMap(
            "searchFields", searchFields, "filterQueryFields", filterQueryFieldsForParty);
    Map<String, Object> performSolrSearchForPartyResponse =
        dispatcher.runSync("performSolrSearch", performSolrSearchForParty);

    if (ServiceUtil.isError(performSolrSearchForPartyResponse)) {
      Debug.logError(
          "An error occurred while performing solr search, details: "
              + ServiceUtil.getErrorMessage(performSolrSearchForPartyResponse),
          module);
    }
    List<Map> partyEntries = (List) performSolrSearchForPartyResponse.get("records");
    for (Map record : partyEntries) {
      Boolean isSupplier = (Boolean) record.get("isSupplier");
      if (UtilValidate.isNotEmpty(isSupplier)) {
        if (isSupplier) partyType = "supplier";
      }
      if (UtilValidate.isEmpty(partyType)) {
        Boolean isCustomer = (Boolean) record.get("isCustomer");
        if (UtilValidate.isNotEmpty(isCustomer)) {
          if (isCustomer) partyType = "customer";
        }
      }

      if (UtilValidate.isEmpty(partyType)) {
        Boolean isSupplierPoc = (Boolean) record.get("isSupplierPoc");
        if (UtilValidate.isNotEmpty(isSupplierPoc)) {
          if (isSupplierPoc) partyType = "supplierPoc";
        }
      }

      if (UtilValidate.isEmpty(partyType)) {
        Boolean isCustomerPoc = (Boolean) record.get("isCustomerPoc");
        if (UtilValidate.isNotEmpty(isCustomerPoc)) {
          if (isCustomerPoc) partyType = "customerPoc";
        }
      }

      if (UtilValidate.isEmpty(partyType)) {
        Boolean isPersonalPoc = (Boolean) record.get("isPersonalPoc");
        if (UtilValidate.isNotEmpty(isPersonalPoc)) {
          if (isPersonalPoc) partyType = "personalPoc";
        }
      }
      return partyType;
    }
    return null;
  }

  /**
   * Returns party's company name, if exists
   *
   * @param delegator
   * @param partyId
   * @return
   */
  public static String getPartyGroupId(Delegator delegator, String partyId) {
    String partyGroupId = "";
    try {
      GenericValue partyGroupRelation =
          EntityQuery.use(delegator)
              .from("PartyRelationship")
              .where(
                  "partyIdTo",
                  partyId,
                  "roleTypeIdFrom",
                  "_NA_",
                  "roleTypeIdFrom",
                  "APP_ORGANIZATION")
              .queryFirst();
      if (UtilValidate.isNotEmpty(partyGroupRelation)) {
        partyGroupId = partyGroupRelation.getString("partyIdFrom");
      }
    } catch (GenericEntityException e) {
      e.printStackTrace();
    }
    return partyGroupId;
  }

  /**
   * Get Person Name from email id - to be used to append in To/CC/From address while sending email
   */
  public static String getPartyNameFromEmailAddress(
      LocalDispatcher dispatcher, GenericValue userLogin, String emailAddress) {
    String partyName = "";
    try {
      Map<String, Object> result =
          dispatcher.runSync(
              "findPartyFromEmailAddress",
              UtilMisc.<String, Object>toMap("address", emailAddress, "userLogin", userLogin));

      if (ServiceUtil.isSuccess(result)) {
        String partyId = (String) result.get("partyId");
        partyName = AxPartyHelper.getPartyName(dispatcher.getDelegator(), partyId);
      }
    } catch (GenericServiceException e) {
    }
    return partyName;
  }

  /**
   * Get Person Name from email id - to be used to append in To/CC/From address while sending email
   */
  public static String getPartyIdFromEmailAddress(
      LocalDispatcher dispatcher, GenericValue userLogin, String emailAddress) {
    String partyId = "";
    try {
      Map<String, Object> result =
          dispatcher.runSync(
              "findPartyFromEmailAddress",
              UtilMisc.<String, Object>toMap("address", emailAddress, "userLogin", userLogin));

      if (ServiceUtil.isSuccess(result)) {
        partyId = (String) result.get("partyId");
      }
    } catch (GenericServiceException e) {
    }
    return partyId;
  }

  public static boolean validateSocialLink(
      Delegator delegator, String partyId, String contactMechPurposeTypeId, String userName)
      throws GenericEntityException {
    GenericValue party = HierarchyUtils.getPartyByPartyId(delegator, partyId);
    Collection<GenericValue> contactMechByPurposes =
        ContactHelper.getContactMechByPurpose(party, contactMechPurposeTypeId, false);
    if (UtilValidate.isNotEmpty(contactMechByPurposes)) {
      if (UtilValidate.isEmpty(userName)) {
        return true;
      }
      for (GenericValue contactMechByPurpose : contactMechByPurposes) {
        GenericValue partyContactMech = null;
        partyContactMech =
            EntityQuery.use(delegator)
                .from("ContactMech")
                .where("contactMechId", contactMechByPurpose.getString("contactMechId"))
                .cache(true)
                .queryFirst();
        if (UtilValidate.isNotEmpty(partyContactMech)) {
          if (userName.equals(partyContactMech.getString("infoString"))) return true;
        }
      }
    }
    return false;
  }

  /** Identify if given userlogin's email verification has been completed or not */
  public static boolean isEmailVerified(
      GenericDelegator delegator, String userLoginId, String partyId) {
    try {
      GenericValue contactMechEmailVerificationResp =
          EntityQuery.use(delegator)
              .from("PartyAndContactMech")
              .where(
                  "contactMechTypeId",
                  "EMAIL_ADDRESS",
                  "infoString",
                  userLoginId,
                  "partyId",
                  partyId)
              .queryOne();
      if (UtilValidate.isNotEmpty(contactMechEmailVerificationResp) && "Y".equalsIgnoreCase(contactMechEmailVerificationResp.getString("isVerified"))) {
        return true;
      }
    } catch (GenericEntityException e) {
      Debug.logError(e, module);
    }

    return false;
  }

  public static Boolean isExistingStoreCustomerEmail(
      Delegator delegator, String storeId, String email) throws GenericEntityException {
    String partyId = null;
    String storePartyEmail = null;
    List<GenericValue> productStoreRoles =
        EntityQuery.use(delegator)
            .from("ProductStoreRole")
            .where("productStoreId", storeId, "roleTypeId", "CUSTOMER")
            .queryList();
    for (GenericValue productStoreRole : productStoreRoles) {
      partyId = (productStoreRole.get("partyId").toString());
      storePartyEmail = getPartyPrimaryEmail(delegator, partyId);
      if (UtilValidate.isNotEmpty(storePartyEmail)) {
        if (storePartyEmail.equals(email)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Finds phone number using provided 10 digit number, must be a valid US number.
   *
   * @param delegator
   * @param phone
   * @return
   */
  public static GenericValue findPhone(Delegator delegator, String phone) {
    if (UtilValidate.isEmpty(phone) || !AxUtilValidate.isValidPhoneNumber(phone, delegator)) {
      return null;
    }

    Map phoneMetaData = AxPhoneNumberUtil.preparePhoneNumberInfo(phone, "");
    GenericValue phoneNumber = null;
    try {
      phoneNumber =
          EntityQuery.use(delegator)
              .from("TelecomNumber")
              .where(
                  "areaCode",
                  phoneMetaData.get("areaCode"),
                  "contactNumber",
                  phoneMetaData.get("areaCode"))
              .queryFirst();
    } catch (GenericEntityException e) {
      Debug.logError(e, module);
    }

    return phoneNumber;
  }

  public static Map getPersonDetails(Delegator delegator, String partyId) {
    return getPartyBasicDetails(delegator, partyId);
  }

  public static Map getPersonDetails(GenericValue party) {
    return getPartyBasicDetails(party);
  }

  public static String getPostalAddressCountryGeoCode(GenericValue postalAddress) {
    String countryRegionCode = "";
    if (UtilValidate.isNotEmpty(postalAddress)) {
      GenericValue countryGeo = null;
      try {
        countryGeo = postalAddress.getRelatedOne("CountryGeo", true);
      } catch (GenericEntityException e) {
        Debug.logError(e, module);
      }
      countryRegionCode = countryGeo.getString("geoCode"); // two digit country code.
    }

    return countryRegionCode;
  }

  public static Map getPartyPrimaryContactDetails(Delegator delegator, GenericValue party) {
    Map partyContactDetailsMap = getPartyContactDetails(delegator, party);

    Map<String, List<Phonenumber.PhoneNumber>> phoneNumberMap =
        (Map<String, List<Phonenumber.PhoneNumber>>) partyContactDetailsMap.get("phoneNumbers");
    if (UtilValidate.isNotEmpty(phoneNumberMap)) {
      List<Phonenumber.PhoneNumber> phoneNumbers =
          (List<Phonenumber.PhoneNumber>) phoneNumberMap.get(PARTY_CONTACT_PURPOSE_PHONE_PRIMARY);
      if (UtilValidate.isNotEmpty(phoneNumbers)) {
        Phonenumber.PhoneNumber phoneNumber = phoneNumbers.get(0);
        partyContactDetailsMap.put(
            "phoneFormatted",
            PhoneNumberUtil.getInstance().formatNationalNumberWithCarrierCode(phoneNumber, ""));
        String phoneNationalFormat = Long.toString(phoneNumber.getNationalNumber());
        partyContactDetailsMap.put("phone", phoneNationalFormat);
      }
    }

    Map<String, List<Map>> emailAddressMap =
        (Map<String, List<Map>>) partyContactDetailsMap.get("emails");
    if (UtilValidate.isNotEmpty(emailAddressMap)) {
      List<Map> emailAddresses =
          (List<Map>) emailAddressMap.get(PARTY_CONTACT_PURPOSE_EMAIL_PRIMARY);
      if (UtilValidate.isNotEmpty(emailAddresses)) {
        Map emailAddress = emailAddresses.get(0);
        InternetAddress email = (InternetAddress) emailAddress.get("emailAddress");
        partyContactDetailsMap.put("email", email.getAddress());
        String verified = (String) emailAddress.get("verified");
        partyContactDetailsMap.put("emailVerified", verified);
        String acceptsMarketing = (String) emailAddress.get("acceptsMarketing");
        partyContactDetailsMap.put("emailAcceptsMarketing", acceptsMarketing);
      }
    }

    Map<String, List<GenericValue>> postalAddressMap =
        (Map<String, List<GenericValue>>) partyContactDetailsMap.get("addresses");
    if (UtilValidate.isNotEmpty(postalAddressMap)) {
      List<GenericValue> postalAddresses =
          (List<GenericValue>) postalAddressMap.get(PARTY_CONTACT_PURPOSE_POSTAL_ADDRESS_PRIMARY);
      if (UtilValidate.isNotEmpty(postalAddresses)) {
        GenericValue postalAddress = postalAddresses.get(0);
        partyContactDetailsMap.put("address", postalAddress);
      }
    }

    return partyContactDetailsMap;
  }

  public static Map getPartyContactDetails(Delegator delegator, GenericValue party) {
    Map partyContactDetailsMap = new HashMap<>();
    if (UtilValidate.isEmpty(party)) {
      Debug.logWarning("Empty party object passed, returning empty map.", module);
      return partyContactDetailsMap;
    }

    String partyId = party.getString("partyId");
    /*
     * ContactMechs are stored as a Map of Lists.  Map key is the contactMechPurposeTypeId
     */
    Map<String, List<Phonenumber.PhoneNumber>> phoneNumberMap = new HashMap<>();
    Map<String, List<Map>> emailAddressMap = new HashMap<>();
    Map<String, List<GenericValue>> postalAddressMap = new HashMap<>();

    List<Map<String, Object>> partyContactMechValueMapList =
        ContactMechWorker.getPartyContactMechValueMaps(delegator, partyId, false);
    for (Map<String, Object> partyContactMechValueMap : partyContactMechValueMapList) {
      GenericValue contactMech = (GenericValue) partyContactMechValueMap.get("contactMech");
      String contactMechTypeId = contactMech.getString("contactMechTypeId");
      GenericValue partyContactMech =
          (GenericValue) partyContactMechValueMap.get("partyContactMech");
      List<GenericValue> partyContactMechPurposes =
          UtilGenerics.toList(partyContactMechValueMap.get("partyContactMechPurposes"));

      for (GenericValue partyContactMechPurpose : partyContactMechPurposes) {
        String contactMechPurposeTypeId =
            partyContactMechPurpose.getString("contactMechPurposeTypeId");

        switch (contactMechTypeId) {
          case "EMAIL_ADDRESS":
            List<Map> emailAddresses = emailAddressMap.get(contactMechPurposeTypeId);
            if (UtilValidate.isEmpty(emailAddresses)) // If this is the first time, initialize.
            {
              emailAddresses = new ArrayList<>();
              emailAddressMap.put(
                  contactMechPurposeTypeId,
                  emailAddresses); // this is so the replace call below works.
            }

            String infoString = contactMech.getString("infoString");
            if (UtilValidate.isEmpty(infoString)) break;

            InternetAddress emailAddress = null;
            try {
              emailAddress = new InternetAddress(infoString);
            } catch (AddressException e) {
              Debug.logWarning(e, module);
              break;
            }
            if (UtilValidate.isNotEmpty(emailAddress)) {
              Map emailMap = new HashMap<>();
              emailMap.put("emailAddress", emailAddress);
              emailMap.put("acceptsMarketing", partyContactMech.getString("allowSolicitation"));
              emailMap.put("verified", partyContactMech.getString("verified"));

              emailAddresses.add(emailMap);
            }

            emailAddressMap.replace(contactMechPurposeTypeId, emailAddresses);
            break;
          case "TELECOM_NUMBER":
            List<Phonenumber.PhoneNumber> phoneNumbers =
                phoneNumberMap.get(contactMechPurposeTypeId);
            if (UtilValidate.isEmpty(phoneNumbers)) // If this is the first time, initialize.
            {
              phoneNumbers = new ArrayList<>();
              phoneNumberMap.put(
                  contactMechPurposeTypeId,
                  phoneNumbers); // this is so the replace call below works.
            }

            GenericValue telecomNumber =
                (GenericValue) partyContactMechValueMap.get("telecomNumber");
            if (UtilValidate.isEmpty(telecomNumber.getString("areaCode"))
                || UtilValidate.isEmpty(telecomNumber.getString("contactNumber"))) break;

            String rawString =
                telecomNumber.getString("countryCode")
                    + "-"
                    + telecomNumber.getString("areaCode")
                    + "-"
                    + telecomNumber.getString("contactNumber");
            PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
            Phonenumber.PhoneNumber number = null;
            try {
              number = phoneUtil.parse(rawString, "US");
            } catch (NumberParseException e) {
              Debug.logWarning(e, module);
              break;
            }
            if (UtilValidate.isNotEmpty(partyContactMech.getString("extension")))
              number.setExtension(partyContactMech.getString("extension"));

            phoneNumbers.add(number);
            phoneNumberMap.replace(contactMechPurposeTypeId, phoneNumbers);
            break;
          case "POSTAL_ADDRESS":
            List<GenericValue> postalAddresses = postalAddressMap.get(contactMechPurposeTypeId);
            if (UtilValidate.isEmpty(postalAddresses)) // If this is the first time, initialize.
            {
              postalAddresses = new ArrayList<>();
              postalAddressMap.put(
                  contactMechPurposeTypeId,
                  postalAddresses); // this is so the replace call below works.
            }

            GenericValue postalAddress =
                (GenericValue) partyContactMechValueMap.get("postalAddress");
            if (UtilValidate.isEmpty(postalAddress)) break;

            if (UtilValidate.isNotEmpty(postalAddress)) postalAddresses.add(postalAddress);

            postalAddressMap.replace(contactMechPurposeTypeId, postalAddresses);
            break;

          default:
            break;
        }
      }
    }

    partyContactDetailsMap.put("phoneNumbers", phoneNumberMap);
    partyContactDetailsMap.put("emails", emailAddressMap);
    partyContactDetailsMap.put("addresses", postalAddressMap);

    return partyContactDetailsMap;
  }

  public static String preparePartyDisplayName(
      String firstName, String middleName, String lastName, boolean lastNameFirst) {
    StringBuilder result = new StringBuilder();
    if (lastNameFirst) {
      if (UtilFormatOut.checkNull(lastName) != null) {
        result.append(UtilFormatOut.checkNull(lastName));
        if (firstName != null) {
          result.append(", ");
        }
      }
      result.append(UtilFormatOut.checkNull(firstName));
    } else {
      result.append(UtilFormatOut.ifNotEmpty(firstName, "", " "));
      result.append(UtilFormatOut.ifNotEmpty(middleName, "", " "));
      result.append(UtilFormatOut.checkNull(lastName));
    }

    return result.toString();
  }

  public static GenericValue getLatestPrimaryPostalAddress(Delegator delegator, String partyId) {
    GenericValue postalAddress = null;
    try {
      postalAddress =
          EntityQuery.use(delegator)
              .from("PartyContactMechPurpose")
              .where(
                  "partyId",
                  partyId,
                  "contactMechPurposeTypeId",
                  PostalAddressTypesEnum.PRIMARY.getTypeId())
              .filterByDate()
              .queryFirst();

    } catch (GenericEntityException e) {
      e.printStackTrace();
    }
    return postalAddress;
  }

  public static GenericValue getLatestPrimaryPostalAddressV2(
      Delegator delegator, LocalDispatcher dispatcher, GenericValue userLogin) {
    GenericValue postalAddress = null;
    try {
      String partyId = userLogin.getString("partyId");
      GenericValue partyContactMech =
          EntityQuery.use(delegator)
              .from("PartyContactMechPurpose")
              .where(
                  "partyId",
                  partyId,
                  "contactMechPurposeTypeId",
                  PostalAddressTypesEnum.PRIMARY.getTypeId())
              .filterByDate()
              .orderBy("-fromDate")
              .queryFirst();
      if (UtilValidate.isNotEmpty(partyContactMech)) {
        postalAddress =
            EntityQuery.use(delegator)
                .from("PostalAddress")
                .where("contactMechId", partyContactMech.get("contactMechId"))
                .queryFirst();
        if (UtilValidate.isNotEmpty(postalAddress)) {
          // checking of the lat long values, calling the enrichGeoDetailsForPostalAddress in case
          // th
          if (UtilValidate.isEmpty(postalAddress.get("longitude"))
              || UtilValidate.isEmpty(postalAddress.get("latitude"))) {
            Map<String, Object> enrichGeoDetailsForPostalAddressResp =
                dispatcher.runSync(
                    "enrichGeoDetailsForPostalAddress",
                    UtilMisc.toMap(
                        "contactMechId",
                        postalAddress.get("contactMechId"),
                        "userLogin",
                        userLogin));

            if (ServiceUtil.isError(enrichGeoDetailsForPostalAddressResp)) {
              String serviceError =
                  ServiceUtil.getErrorMessage(enrichGeoDetailsForPostalAddressResp);
              Debug.logError(
                  "An error occurred while enrichGeoDetailsForPostalAddress, details: "
                      + serviceError,
                  module);
            }
            if (UtilValidate.isNotEmpty(
                enrichGeoDetailsForPostalAddressResp.get("updatedPostalAddress")))
              postalAddress =
                  (GenericValue) enrichGeoDetailsForPostalAddressResp.get("updatedPostalAddress");
          }

          // checking for the timezone in the postal address, if empty calling service
          // populateTimezoneForGeoLocation
          if (UtilValidate.isEmpty(postalAddress.get("timeZoneId"))) {

            Map<String, Object> populateTimezoneForGeoLocationResp =
                dispatcher.runSync(
                    "populateTimezoneForGeoLocation",
                    UtilMisc.toMap(
                        "contactMechId",
                        postalAddress.get("contactMechId"),
                        "userLogin",
                        userLogin));

            if (ServiceUtil.isError(populateTimezoneForGeoLocationResp)) {
              String serviceError = ServiceUtil.getErrorMessage(populateTimezoneForGeoLocationResp);
              Debug.logError(
                  "An error occurred while calling populateTimezoneForGeoLocation, details: "
                      + serviceError,
                  module);
            }
            postalAddress =
                (GenericValue) populateTimezoneForGeoLocationResp.get("updatedPostalAddress");
          }
        }
      }
    } catch (GenericEntityException | GenericServiceException e) {
      e.printStackTrace();
    }
    return postalAddress;
  }

  public static List<GenericValue> getCompanySuppliers(Delegator delegator, String partyId) {
    List<GenericValue> partySuppliers = null;
    try {
      partySuppliers =
          EntityQuery.use(delegator)
              .from("PartyRelationship")
              .where("partyIdFrom", partyId, "roleTypeIdTo", "SUPPLIER")
              .queryList();
      if (partySuppliers != null) {
        return partySuppliers;
      }
    } catch (GenericEntityException e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Returns party's latest primary email. Empty string if nothing exists;
   *
   * @param delegator
   * @param partyId
   * @return
   */
  public static String getPartyPrimaryEmail(Delegator delegator, String partyId) {
    String partyPrimaryEmail = "";
    GenericValue partyLatestCOntactMech =
        PartyWorker.findPartyLatestContactMech(
            partyId, ContactMethodTypesEnum.EMAIL.getTypeId(), delegator);
    if (UtilValidate.isNotEmpty(partyLatestCOntactMech)) {
      partyPrimaryEmail = partyLatestCOntactMech.getString("infoString");
    }

    return partyPrimaryEmail;
  }

  /**
   * Returns a map containing, phone's areaCode, contactNumber, countryCode, phoneNumber
   * (Phonenumber.PhoneNumber object)
   *
   * @param phone 10 digit phone number without any spaces.
   * @param countryRegionCode 2 digit country code, like US for USA IN for India
   * @return @External References http://libphonenumber.appspot.com/
   *     https://github.com/googlei18n/libphonenumber/blob/master/java/demo/src/com/google/phonenumbers/PhoneNumberParserServlet.java
   */
  public static Map preparePhoneNumberInfo(String phone, String countryRegionCode) {
    Map phoneInfoMap = new HashMap<>();
    PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
    Phonenumber.PhoneNumber number = null;
    try {
      number = phoneUtil.parseAndKeepRawInput(phone, countryRegionCode);
    } catch (NumberParseException e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }

    // country code
    String countryCode = "+" + phoneUtil.getCountryCodeForRegion(countryRegionCode);

    // area code
    // get area code : Save in areaCode
    String nationalSignificantNumber = phoneUtil.getNationalSignificantNumber(number);
    int nationalDestinationCodeLength = phoneUtil.getLengthOfNationalDestinationCode(number);

    String phoneAreaCode = "";
    if (nationalDestinationCodeLength > 0) {
      phoneAreaCode = nationalSignificantNumber.substring(0, nationalDestinationCodeLength);
    }

    // contact number
    String phoneContactNumber =
        nationalSignificantNumber
            .substring(nationalDestinationCodeLength, nationalSignificantNumber.length())
            .trim();

    String languageCode = "en"; // Default languageCode to English if nothing is entered.
    Locale geocodingLocale = new Locale(languageCode, countryRegionCode);

    phoneInfoMap.put("countryCode", countryCode);
    phoneInfoMap.put("areaCode", phoneAreaCode);
    phoneInfoMap.put("contactNumber", phoneContactNumber);
    phoneInfoMap.put("phoneNumber", number);
    phoneInfoMap.put("isValidNumber", phoneUtil.isValidNumber(number));

    String location =
        PhoneNumberOfflineGeocoder.getInstance().getDescriptionForNumber(number, geocodingLocale);
    phoneInfoMap.put("location", location);

    String regionCode = phoneUtil.getRegionCodeForNumber(number);
    phoneInfoMap.put("regionCode", regionCode);

    PhoneNumberUtil.PhoneNumberType numberType = phoneUtil.getNumberType(number);
    phoneInfoMap.put("type", numberType.toString());

    String timeZone =
        PhoneNumberToTimeZonesMapper.getInstance().getTimeZonesForNumber(number).toString();
    phoneInfoMap.put("timeZone", timeZone);

    boolean isPossibleNumber = phoneUtil.isPossibleNumber(number);
    phoneInfoMap.put("isPossibleNumber", isPossibleNumber);

    return phoneInfoMap;
  }

  public static GenericValue getLatestPrimaryTelecomNumber(Delegator delegator, String partyId) {
    GenericValue telecomNumber = null;
    try {
      telecomNumber =
          EntityQuery.use(delegator)
              .from("PartyContactMechPurpose")
              .where("partyId", partyId, "contactMechPurposeTypeId", "PRIMARY_PHONE")
              .filterByDate()
              .queryFirst();
    } catch (GenericEntityException e) {
      e.printStackTrace();
    }
    return telecomNumber;
  }

  public static String getPartyRecentContactMechPurpose(Delegator delegator, String partyId)
      throws GenericEntityException {

    String contactMechId = "";
    GenericValue partyContactMechPurpose =
        EntityQuery.use(delegator)
            .from("PartyContactMechPurpose")
            .where("partyId", partyId, "contactMechPurposeTypeId", "SHIPPING_LOCATION")
            .filterByDate()
            .orderBy("-createdStamp")
            .queryFirst();

    if (UtilValidate.isNotEmpty(partyContactMechPurpose)) {
      contactMechId = partyContactMechPurpose.getString("contactMechId");
    }

    return contactMechId;
  }

  public static Map getPartyEmployees(Delegator delegator, String partyId) {
    Map partyEmployessMap = FastMap.newInstance();

    try {
      GenericValue partyEmployees =
          EntityQuery.use(delegator)
              .from("PartyRelationship")
              .where(UtilMisc.toMap("partyIdFrom", partyId))
              .orderBy("createdStamp DESC")
              .queryFirst();
      if (UtilValidate.isEmpty(partyEmployees)) {
        return null;
      } else {
        String empId = partyEmployees.getString("partyIdTo");
        partyEmployessMap =
            EntityQuery.use(delegator)
                .from("Person")
                .where(UtilMisc.toMap("partyId", empId))
                .queryOne();
      }
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
    return partyEmployessMap;
  }

  public static List<Map> getPartyAttributes(Delegator delegator, String partyId)
      throws GenericEntityException {
    List<Map> partyAttributes = FastList.newInstance();
    List<GenericValue> partyAttributesGv =
        EntityQuery.use(delegator)
            .from("PartyAttribute")
            .where("partyId", "partyId")
            .queryList();
    Map partyAttributeMap;
    for (GenericValue partyAttribute : CollectionUtils.emptyIfNull(partyAttributesGv)) {
      partyAttributeMap = FastMap.newInstance();
      partyAttributeMap.put("attrName", partyAttribute.getString("attrName"));
      partyAttributeMap.put("attrValue", partyAttribute.getString("attrValue"));
      partyAttributes.add(partyAttributeMap);
    }
    return partyAttributes;
  }

  public static List<Map> getPartyAttributes(Delegator delegator, GenericValue partyRecord)
      throws GenericEntityException {
    return getPartyAttributes(delegator, partyRecord.getString("partyId"));
  }

  /**
   * Get an attribute of a party.
   *
   * @param delegator
   * @param partyId
   * @param attrName
   * @return
   */
  public static String getPartyAttribute(Delegator delegator, String partyId, String attrName)  {
    try {
      GenericValue partyAttribute = EntityQuery.use(delegator)
          .from("PartyAttribute")
          .where(UtilMisc.toMap("partyId", partyId, "attrName", attrName))
          .queryOne();
      if (UtilValidate.isEmpty(partyAttribute)) {
        return null;
      } else {
        return (String) partyAttribute.get("attrValue");
      }
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Returns party's latest primary email contact mech.Null if nothing exists
   *
   * @param delegator
   * @param partyId
   * @return
   */
  public static GenericValue getPartyPrimaryEmailContactMech(Delegator delegator, String partyId) {
    GenericValue partyLatestContactMech =
        PartyWorker.findPartyLatestContactMech(
            partyId, ContactMethodTypesEnum.EMAIL.getTypeId(), delegator);

    return partyLatestContactMech;
  }

  public static Map getCustomerOwnedByAccount(String customerPartyId, LocalDispatcher dispatcher) {
    List<Map> customerOwnerAccountList =
        ExtPartyRelationshipHelper.getActivePartyRelationshipsFromParty(
            dispatcher, customerPartyId, "CUSTOMER");
    if (UtilValidate.isNotEmpty(customerOwnerAccountList)) {
      return customerOwnerAccountList.get(0);
    }

    return null;
  }

  public static String getCustomerOwnedByAccountId(
      String customerPartyId, LocalDispatcher dispatcher) {
    Map customerOwnerAccount = getCustomerOwnedByAccount(customerPartyId, dispatcher);

    if (UtilValidate.isNotEmpty(customerOwnerAccount)) {
      return (String) customerOwnerAccount.get("partyId");
    }
    return null;
  }

  /**
   * Returns party photoUrl, displayName, email, initials for the party. Fetches details from Party
   * Object, is very fast.
   *
   * @param delegator
   * @param dispatcher
   * @param emailAddress
   * @param userLogin
   * @return
   */
  public static Map getPartyBasicDetailsFromUserLogin(
      Delegator delegator,
      LocalDispatcher dispatcher,
      String emailAddress,
      GenericValue userLogin) {
    Map partyBasicDetails = FastMap.newInstance();
    String partyId = "";
    try {
      Map<String, Object> result =
          dispatcher.runSync(
              "findPartyFromEmailAddress",
              UtilMisc.<String, Object>toMap("address", emailAddress, "userLogin", userLogin));

      if (ServiceUtil.isSuccess(result)) {
        partyId = (String) result.get("partyId");
      }
    } catch (GenericServiceException e) {
      Debug.logError(e, "Error getting person info, ignoring...", module);
    }
    GenericValue party = HierarchyUtils.getPartyByPartyId(delegator, partyId);

    if (UtilValidate.isNotEmpty(party)) {
      GenericValue person = null;

      try {
        person = party.getRelatedOne("Person", false);
      } catch (GenericEntityException e) {
        Debug.logError(e, "Error getting person info, ignoring...", module);
      }

      String photoUrl = party.getString("photoUrl");
      if (UtilValidate.isNotEmpty(photoUrl)) {
        partyBasicDetails.put("photoUrl", photoUrl);
      }

      String displayName = party.getString("displayName");
      if (UtilValidate.isNotEmpty(displayName)) {
        partyBasicDetails.put("displayName", displayName.trim());
        partyBasicDetails.put("initials", getPartyInitials(party, false));
      }

      String email = party.getString("email");
      if (UtilValidate.isNotEmpty(email)) {
        partyBasicDetails.put("email", email);
      }

      if (UtilValidate.isNotEmpty(person)) {
        partyBasicDetails.put("firstName", person.getString("firstName"));
        partyBasicDetails.put("lastName", person.getString("lastName"));
      }
    }

    return partyBasicDetails;
  }

  public static String getPartyDefaultCurrency(Delegator delegator, String partyId, String storeId)
      throws GenericEntityException {
    String currencyUomId = "";
    GenericValue partyContactMechPurpose =
        EntityQuery.use(delegator)
            .from("PartyContactMechPurposeAndAddressAndGeo")
            .where(
                "partyId",
                partyId,
                "contactMechPurposeTypeId",
                PostalAddressTypesEnum.PRIMARY.getTypeId())
            .filterByDate()
            .orderBy("-fromDate")
            .queryFirst();

    if (UtilValidate.isNotEmpty(partyContactMechPurpose)) {
      String countryCode = partyContactMechPurpose.getString("geoCode");
      if (UtilValidate.isNotEmpty(countryCode)) {
        try {
          currencyUomId = Currency.getInstance(new Locale("en", countryCode)).getCurrencyCode();
        } catch (IllegalArgumentException e) {
          Debug.logError(
              e,
              "An error occurred while trying to fetch currency code for " + countryCode + ". ",
              module);
        }
      }
    }

    return currencyUomId;
  }
}
