package com.simbaquartz.xcommon.models.people;

import static com.simbaquartz.xcommon.models.PhoneModelBuilder.preparePhoneModel;

import com.simbaquartz.xcommon.collections.FastList;
import com.simbaquartz.xcommon.models.LinkedInAddress;
import com.simbaquartz.xcommon.models.LinkedInAddressModelBuilder;
import com.simbaquartz.xcommon.models.Phone;
import com.simbaquartz.xcommon.models.Photo;
import com.simbaquartz.xcommon.models.WebAddress;
import com.simbaquartz.xcommon.models.WebAddressModelBuilder;
import com.simbaquartz.xcommon.models.email.EmailAddress;
import com.simbaquartz.xcommon.models.email.EmailAddressModelBuilder;
import com.simbaquartz.xcommon.models.geo.PostalAddress;
import com.simbaquartz.xcommon.models.geo.builder.GeoModelBuilder;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.ofbiz.base.util.UtilValidate;

/** Created by mande on 1/3/2022. */
public class PersonModelBuilder {
  /**
   * Prepares a person model along with postal address, email, web, linkedinaddress and phone
   * numbers. Needs a valid partyId or id to work.
   *
   * @param personMap
   * @return
   */
  public static Person build(Map personMap) {
    Person person = null;
    if (UtilValidate.isNotEmpty(personMap)
        && (UtilValidate.isNotEmpty(personMap.get("partyId"))
            || UtilValidate.isNotEmpty(personMap.get("id")))) {
      person = new Person();
      if (UtilValidate.isNotEmpty(personMap.get("id"))) {
        person.setId((String) personMap.get("id"));
      }else{
        person.setId((String) personMap.get("partyId"));
      }

      if (UtilValidate.isNotEmpty(personMap.get("fullName"))) {
        person.setFullName((String) personMap.get("fullName"));
      }
      if (UtilValidate.isNotEmpty(personMap.get("displayName"))) {
        person.setDisplayName((String) personMap.get("displayName"));
      }
      if (UtilValidate.isNotEmpty(personMap.get("firstName"))) {
        person.setFirstName((String) personMap.get("firstName"));
      }
      if (UtilValidate.isNotEmpty(personMap.get("middleName"))) {
        person.setMiddleName((String) personMap.get("middleName"));
      }
      if (UtilValidate.isNotEmpty(personMap.get("lastName"))) {
        person.setLastName((String) personMap.get("lastName"));
      }
      if (UtilValidate.isNotEmpty(personMap.get("personalTitle"))) {
        person.setTitle((String) personMap.get("personalTitle"));
      }
      if (UtilValidate.isNotEmpty(personMap.get("suffix"))) {
        person.setSuffix((String) personMap.get("suffix"));
      }
      if (UtilValidate.isNotEmpty(personMap.get("nickname"))) {
        person.setNickName((String) personMap.get("nickname"));
      }
      if (UtilValidate.isNotEmpty(personMap.get("salutation"))) {
        person.setSalutation((String) personMap.get("salutation"));
      }
      if (UtilValidate.isNotEmpty(personMap.get("companyName"))) {
        person.setCompanyName((String) personMap.get("companyName"));
      }
      if (UtilValidate.isNotEmpty(personMap.get("initials"))) {
        person.setPartyInitials((String) personMap.get("initials"));
      }
      if (UtilValidate.isNotEmpty(personMap.get("gender"))) {
        person.setGender((String) personMap.get("gender"));
      }
      if (UtilValidate.isNotEmpty(personMap.get("email"))) {
        person.setEmail((String) personMap.get("email"));
      }
      if (UtilValidate.isNotEmpty(personMap.get("partyInitials"))) {
        person.setPartyInitials((String) personMap.get("partyInitials"));
      }
      if (UtilValidate.isNotEmpty(personMap.get("designation"))) {
        person.setCompanyDesignation((String) personMap.get("designation"));
      }
      if (UtilValidate.isNotEmpty(personMap.get("department"))) {
        person.setCompanyDepartment((String) personMap.get("department"));
      }
      if (UtilValidate.isNotEmpty(personMap.get("parentPartyId"))) {
        person.setParentPartyId((String) personMap.get("parentPartyId"));
      }
      if (UtilValidate.isNotEmpty(personMap.get("parentPartyName"))) {
        person.setParentPartyName((String) personMap.get("parentPartyName"));
      }
      if (UtilValidate.isNotEmpty(personMap.get("photoUrl"))) {
        Photo personPhoto = new Photo();
        String photoUrl = (String) personMap.get("photoUrl");
        personPhoto.setPublicUrl(photoUrl);
        person.setPhoto(personPhoto);
        person.setPhotoUrl(photoUrl);
      }
      if (UtilValidate.isNotEmpty(personMap.get("createdStamp"))) {
        Timestamp createdAt = (Timestamp) personMap.get("createdStamp");
        person.setCreatedAt(createdAt);
      }
      if (UtilValidate.isNotEmpty(personMap.get("birthday"))) {
        Date birthday = (Date) personMap.get("birthday");
        person.setBirthday(birthday);
      }
      if (UtilValidate.isNotEmpty(personMap.get("lastUpdatedStamp"))) {
        Timestamp lastUpdatedStamp = (Timestamp) personMap.get("lastUpdatedStamp");
        person.setLastModifiedDate(lastUpdatedStamp);
      }

      List<Map> postalAddresses = (List<Map>) personMap.get("postalAddresses");
      if (UtilValidate.isNotEmpty(postalAddresses)) {
        List<PostalAddress> allAddresses = FastList.newInstance();
        for (Map address : postalAddresses) {
          PostalAddress addressModel = GeoModelBuilder.buildPostalAddress(address);
          allAddresses.add(addressModel);
        }
        person.setAddresses(allAddresses);
      }

      List<Map> phoneNumbers = (List<Map>) personMap.get("phoneNumbers");
      if (UtilValidate.isNotEmpty(phoneNumbers)) {
        List<Phone> phones = preparePhoneModel(phoneNumbers);
        person.setPhones(phones);
      }

      List<Map> emailAddress = (List<Map>) personMap.get("emailAddress");
      if (UtilValidate.isNotEmpty(emailAddress)) {
        List<EmailAddress> emailAddresses = EmailAddressModelBuilder.build(emailAddress);
        person.setEmailAddress(emailAddresses);
      }

      List<Map> webAddress = (List<Map>) personMap.get("webAddress");
      if (UtilValidate.isNotEmpty(webAddress)) {

        List<WebAddress> webAddresses = WebAddressModelBuilder.build(webAddress);
        person.setWebAddress(webAddresses);
      }

      List<Map> linkedInAddress = (List<Map>) personMap.get("linkedInAddress");
      if (UtilValidate.isNotEmpty(linkedInAddress)) {
        List<LinkedInAddress> linkedInAddresses =
            LinkedInAddressModelBuilder.build(linkedInAddress);
        person.setLinkedInAddresses(linkedInAddresses);
      }
    }

    return person;
  }
}
