package com.simbaquartz.xparty.services.person;

import com.fidelissd.zcp.xcommon.collections.FastList;
import com.fidelissd.zcp.xcommon.collections.FastMap;
import com.fidelissd.zcp.xcommon.models.Phone;
import com.fidelissd.zcp.xcommon.models.account.ApplicationUser;
import com.fidelissd.zcp.xcommon.models.account.ConnectedAccountSlack;
import com.fidelissd.zcp.xcommon.models.account.ConnectedAccounts;
import com.fidelissd.zcp.xcommon.models.geo.PostalAddress;
import com.fidelissd.zcp.xcommon.models.geo.Timezone;
import com.fidelissd.zcp.xcommon.models.geo.builder.GeoModelBuilder;
import com.fidelissd.zcp.xcommon.models.people.Organization;
import com.fidelissd.zcp.xcommon.models.people.Person;
import com.fidelissd.zcp.xcommon.models.people.PersonModelBuilder;
import com.fidelissd.zcp.xcommon.util.AxPhoneNumberUtil;
import com.fidelissd.zcp.xcommon.util.hierarchy.HierarchyUtils;
import com.simbaquartz.xparty.helpers.AxPartyHelper;
import com.simbaquartz.xparty.helpers.PartyContactHelper;
import com.simbaquartz.xparty.helpers.PartyPostalAddressHelper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericDelegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

/**
 * Created by mande on 10/26/2019.
 */
public class PersonServices {
    private static final String module = PersonServices.class.getName();

    public static Person getPersonDetails(
            GenericDelegator delegator,
            LocalDispatcher dispatcher,
            String partyId,
            GenericValue userLogin) {
        return getPersonDetails(delegator, dispatcher, partyId, userLogin, true, true);
    }

    public static Person getPersonDetails(
            GenericDelegator delegator,
            LocalDispatcher dispatcher,
            String partyId,
            GenericValue userLogin,
            boolean includeOrgDetails,
            boolean includeBasicDetailsOnly) {
        // prepare person information
        Person person = new Person();
        try {
            person.setId(partyId);
            Map partyBasicDetails = AxPartyHelper.getPartyBasicDetails(delegator, partyId);
            person = PersonModelBuilder.build(partyBasicDetails);
            person.setLocation((String) partyBasicDetails.get("location"));
            person.setPreferredCurrencyUomId((String) partyBasicDetails.get("defaultCurrency"));
            person.setCreatedAt((Timestamp) partyBasicDetails.get("createdDate"));

            person.setId(partyId);

            String designationOfPerson = "";
            String departmentOfPerson = "";
            // fetch Party designation
            if (!includeBasicDetailsOnly) {
                String designation = AxPartyHelper.getPartyAttribute(delegator, partyId, "Designation");
                if (UtilValidate.isNotEmpty(designation)) {
                    designationOfPerson = designation;
                }
                person.setCompanyDesignation(designationOfPerson);

                // fetch Party department
                Map fsdGetPartyDepartmentResponse =
                        dispatcher.runSync(
                                "fsdGetPartyDepartment",
                                UtilMisc.toMap("partyId", partyId, "userLogin", userLogin));
                if (ServiceUtil.isSuccess(fsdGetPartyDepartmentResponse)) {
                    departmentOfPerson = (String) fsdGetPartyDepartmentResponse.get("department");
                }
                person.setCompanyDepartment(departmentOfPerson);

                // get latest timezone
                Timezone timezone = PersonHelper.getPersonTimeZone(partyId, dispatcher);
                if (UtilValidate.isNotEmpty(timezone)) {
                    person.setTimezone(timezone);
                }

                // get latest address
                Map personPrimaryAddress = PartyPostalAddressHelper.getPrimaryAddress(partyId, delegator);
                if (UtilValidate.isNotEmpty(personPrimaryAddress)) {
                    PostalAddress personAddress = GeoModelBuilder.buildPostalAddress(personPrimaryAddress);
                    person.setPrimaryAddress(personAddress);

                    // if timezone is empty set it via primary address
                    if (UtilValidate.isEmpty(person.getTimezone())
                            && UtilValidate.isNotEmpty(personAddress.getTimezone())) {
                        person.setTimezone(personAddress.getTimezone());
                    }
                }

                // phone number
                Map customerPrimaryPhoneObj =
                        PartyContactHelper.getLatestPrimaryTelecomNumber(delegator, dispatcher, partyId, true);
                if (UtilValidate.isNotEmpty(customerPrimaryPhoneObj)) {
                    String phoneId = (String) customerPrimaryPhoneObj.get("contactMechId");
                    String countryCode = (String) customerPrimaryPhoneObj.get("countryCode");
                    String areaCode = (String) customerPrimaryPhoneObj.get("areaCode");
                    String contactNumber = (String) customerPrimaryPhoneObj.get("contactNumber");
                    String extension = (String) customerPrimaryPhoneObj.get("extension");
                    // make sure a valid number exists (country code and contact number are required.
                    if (UtilValidate.isNotEmpty(countryCode) && UtilValidate.isNotEmpty(contactNumber)) {
                        // in US format, this should be based on input locale though
                        String formattedPhoneNumber =
                                AxPartyHelper.getFormattedPhoneNumber(
                                        delegator, phoneId, countryCode, areaCode, contactNumber, extension);

                        Phone personPrimaryPhone = new Phone();
                        if (UtilValidate.isEmpty(areaCode)) {
                            areaCode = "";
                        }
                        if (UtilValidate.isEmpty(contactNumber)) {
                            contactNumber = "";
                        }

                        Map phoneInfoMap =
                                AxPhoneNumberUtil.preparePhoneNumberInfo(
                                        areaCode + contactNumber, Integer.parseInt(countryCode.replace("+", "")));

                        personPrimaryPhone.setId(phoneId);
                        personPrimaryPhone.setCountryCode(countryCode);
                        personPrimaryPhone.setRegionCode((String) phoneInfoMap.get("regionCode"));
                        personPrimaryPhone.setAreaCode(areaCode);
                        personPrimaryPhone.setPhone(contactNumber);
                        personPrimaryPhone.setExtension(extension);
                        personPrimaryPhone.setPhoneFormatted(formattedPhoneNumber);
                        person.setPrimaryPhone(personPrimaryPhone);
                    }
                }
            }

            // organization details
            if (includeOrgDetails) {
                Map<String, Object> organizationDetails = FastMap.newInstance();

                Map<String, Object> getPartyOrganizationForPartyIdCtx = FastMap.newInstance();
                getPartyOrganizationForPartyIdCtx.put("userLogin", userLogin);
                getPartyOrganizationForPartyIdCtx.put("partyId", partyId);
                organizationDetails =
                        dispatcher.runSync("getPartyGroupForPartyId", getPartyOrganizationForPartyIdCtx);

                if (ServiceUtil.isSuccess(organizationDetails)) {

                    GenericValue orgPartyGroup =
                            (GenericValue) organizationDetails.get("organizationPartyGroup");
                    if (UtilValidate.isNotEmpty(orgPartyGroup)) {
                        String orgPartyId = orgPartyGroup.getString("partyId");
                        Organization orgDetails = new Organization();
                        orgDetails.setOrganizationName(orgPartyGroup.getString("groupName"));
                        orgDetails.setId(orgPartyId);
                        orgDetails.setIndustryType(orgPartyGroup.getString("industryType"));
                        orgDetails.setDuns(orgPartyGroup.getString("Duns"));
                        orgDetails.setCage(orgPartyGroup.getString("Cage"));
                        orgDetails.setTaxId(orgPartyGroup.getString("taxId"));
                        orgDetails.setOfficeSiteName(orgPartyGroup.getString("officeSiteName"));
                        orgDetails.setAnnualRevenue(orgPartyGroup.getString("annualRevenue"));

                        GenericValue finAccount =
                                EntityQuery.use(delegator)
                                        .from("FinAccount")
                                        .where("organizationPartyId", orgPartyId)
                                        .queryOne();
                        if (UtilValidate.isNotEmpty(finAccount)) {
                            orgDetails.setAccountNumber(finAccount.getString("finAccountCode"));
                            orgDetails.setBank(finAccount.getString("finBankName"));
                            orgDetails.setRouting(finAccount.getString("finSortCode"));
                        }

                        GenericValue partyAcctgPreference =
                                EntityQuery.use(delegator)
                                        .from("PartyAcctgPreference")
                                        .where("partyId", orgPartyId)
                                        .queryOne();
                        if (UtilValidate.isNotEmpty(partyAcctgPreference)) {
                            orgDetails.setQuoteIdPrefix(partyAcctgPreference.getString("quoteIdPrefix"));
                        }

                        // Set Org Phone Number
                        Map orgPrimaryPhoneObj =
                                PartyContactHelper.getLatestPrimaryTelecomNumber(
                                        delegator, dispatcher, orgPartyId, true);
                        if (UtilValidate.isNotEmpty(orgPrimaryPhoneObj)) {
                            String phoneId = (String) orgPrimaryPhoneObj.get("contactMechId");
                            String countryCode = (String) orgPrimaryPhoneObj.get("countryCode");
                            String areaCode = (String) orgPrimaryPhoneObj.get("areaCode");
                            String contactNumber = (String) orgPrimaryPhoneObj.get("contactNumber");
                            String extension = (String) orgPrimaryPhoneObj.get("extension");
                            // in US format, this should be based on input locale though
                            String formattedPhoneNumber =
                                    AxPartyHelper.getFormattedPhoneNumber(
                                            delegator, phoneId, countryCode, areaCode, contactNumber, extension);

                            Phone orgPrimaryPhone = new Phone();
                            orgPrimaryPhone.setId(phoneId);
                            orgPrimaryPhone.setCountryCode(countryCode);
                            orgPrimaryPhone.setAreaCode(areaCode);
                            orgPrimaryPhone.setPhone(contactNumber);
                            orgPrimaryPhone.setExtension(extension);
                            orgPrimaryPhone.setPhoneFormatted(formattedPhoneNumber);
                            orgDetails.setPrimaryPhone(orgPrimaryPhone);
                        }

                        Map orgPrimaryAddressMap =
                                PartyPostalAddressHelper.getPrimaryAddress(orgPartyId, delegator);
                        if (UtilValidate.isNotEmpty(orgPrimaryAddressMap)
                                && UtilValidate.isNotEmpty(orgPrimaryAddressMap.get("countryGeoId"))) {
                            orgDetails.setPrimaryAddress(
                                    GeoModelBuilder.buildPostalAddress(orgPrimaryAddressMap));
                        } else {
                            // falback
                            List<GenericValue> postalAddresses = null;

                            postalAddresses = AxPartyHelper.getPostalAddresses(delegator, orgPartyGroup);

                            List<PostalAddress> postalAddressList = FastList.newInstance();
                            for (Map postalAddress : postalAddresses) {
                                postalAddressList.add(GeoModelBuilder.buildPostalAddress(postalAddress));
                            }
                            orgDetails.setAddress(postalAddressList);
                            if (UtilValidate.isNotEmpty(postalAddressList)) {
                                orgDetails.setPrimaryAddress(postalAddressList.get(0));
                            }
                        }

                        // Check if Slack account is connected to Org
                        /*Map<String, Object> slackConnectedResp =
                                dispatcher.runSync("checkIfSlackConnected", UtilMisc.toMap("userLogin", userLogin, "accountPartyId", orgPartyId));
                        Boolean isSlackConnected = false;
                        ConnectedAccountSlack connectedAccountSlack = ConnectedAccountSlack.builder().build();
                        if (ServiceUtil.isSuccess(slackConnectedResp)) {
                            isSlackConnected = (Boolean) slackConnectedResp.get("isConnected");
                            GenericValue slackAuthDetails =
                                    (GenericValue) slackConnectedResp.get("slackAuthDetails");
                            if (UtilValidate.isNotEmpty(slackAuthDetails)) {
                                connectedAccountSlack.setSlackTeamId(slackAuthDetails.getString("slackTeamId"));
                                connectedAccountSlack.setSlackTeamName(slackAuthDetails.getString("slackTeamName"));
                            }
                        }
                        connectedAccountSlack.setIsConnected(isSlackConnected);
                        orgDetails.setConnectedAccounts(
                                ConnectedAccounts.builder().slack(connectedAccountSlack).build());*/

                        person.setOrganization(orgDetails);
                    }
                }
            }

            // Prepare attributes
            List<Map> attributes = FastList.newInstance();
            List<GenericValue> partyAttributes = EntityQuery.use(delegator).from("PartyAttribute").where("partyId", partyId).queryList();
            for (Map partyAttribute : CollectionUtils.emptyIfNull(partyAttributes)) {
                Map attribute = FastMap.newInstance();
                attribute.put(partyAttribute.get("attrName"), partyAttribute.get("attrValue"));
                attributes.add(attribute);
            }
            person.setAttributes(attributes);

        } catch (GenericServiceException | GenericEntityException e) {
            Debug.logError(e, module);
        }

        return person;
    }

    public static ApplicationUser updatePersonDetails(
            LocalDispatcher dispatcher,
            String partyId,
            GenericValue userLogin,
            ApplicationUser userDetailsToUpdate) {
        // prepare person information
        Person personDetails = userDetailsToUpdate.getPersonalDetails();
        try {

            if (UtilValidate.isNotEmpty(personDetails)) {

                if (UtilValidate.isNotEmpty(personDetails.getFirstName())
                        || UtilValidate.isNotEmpty(personDetails.getLastName())) {
                    // update basic details
                    Map updatePersonCtx =
                            UtilMisc.toMap(
                                    "userLogin",
                                    userLogin,
                                    "partyId",
                                    partyId,
                                    "firstName",
                                    personDetails.getFirstName(),
                                    "lastName",
                                    personDetails.getLastName(),
                                    "displayName",
                                    personDetails.getDisplayName(),
                                    "gender",
                                    personDetails.getGender());

                    if (UtilValidate.isNotEmpty(personDetails.getRaceList())) {
                        updatePersonCtx.put("race", String.join("|", personDetails.getRaceList()));
                    }

                    dispatcher.runSync("updatePerson", updatePersonCtx);
                }

                // update designation
                if (UtilValidate.isNotEmpty(personDetails.getCompanyDesignation())) {
                    Map fsdSetPartyDesignationResponse =
                            dispatcher.runSync(
                                    "createOrUpdatePartyAttribute",
                                    UtilMisc.toMap(
                                            "partyId",
                                            partyId,
                                            "attrName",
                                            "Designation",
                                            "attrValue",
                                            personDetails.getCompanyDesignation(),
                                            "userLogin",
                                            userLogin));
                }
            }

        } catch (GenericServiceException e) {
            Debug.logError(e, module);
        }

        return userDetailsToUpdate;
    }

    /**
     * Creates a primary telecom number for the party and returns the id of the created phone contact
     * mech id.
     *
     * @param partyId
     * @param phone
     * @param userLogin
     * @param dispatcher
     * @return
     */
    public static String createPersonPhone(
            String partyId, Phone phone, GenericValue userLogin, LocalDispatcher dispatcher) {
        String phoneId = "";
        Map<String, Object> createPartyPhoneContext = FastMap.newInstance();
        createPartyPhoneContext.put("userLogin", userLogin);
        createPartyPhoneContext.put("partyId", partyId);
        createPartyPhoneContext.put("contactMechTypeId", "TELECOM_NUMBER");
        if (UtilValidate.isNotEmpty(phone.getCountryCode())) {
            createPartyPhoneContext.put("countryCode", phone.getCountryCode());
        }
        if (UtilValidate.isNotEmpty(phone.getPhonePurposes())) {
            createPartyPhoneContext.put("contactMechPurposeTypeId", phone.getPhonePurposes().get(0));
        }
        if (UtilValidate.isNotEmpty(phone.getAreaCode())) {
            createPartyPhoneContext.put("areaCode", phone.getAreaCode());
        }
        if (UtilValidate.isNotEmpty(phone.getPhone())) {
            createPartyPhoneContext.put("contactNumber", phone.getPhone());
        }
        if (UtilValidate.isNotEmpty(phone.getExtension())) {
            createPartyPhoneContext.put("extension", phone.getExtension());
        }
        Map createPartyPhoneServiceResponse = null;
        try {
            if (Debug.verboseOn())
                Debug.logVerbose(
                        "Invoking service createPersonPhone with input context : " + createPartyPhoneContext,
                        module);
            createPartyPhoneContext.put("createdByUserLogin", userLogin.get("userLoginId"));
            createPartyPhoneContext.put("lastModifiedByUserLogin", userLogin.get("userLoginId"));
            createPartyPhoneContext.put("createdDate", UtilDateTime.nowTimestamp());
            createPartyPhoneContext.put("lastModifiedDate", UtilDateTime.nowTimestamp());
            createPartyPhoneServiceResponse =
                    dispatcher.runSync("createPartyTelecomNumber", createPartyPhoneContext);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            if (Debug.verboseOn()) Debug.logVerbose("Exiting method createPersonPhone", module);
            return null;
        }
        if (ServiceUtil.isError(createPartyPhoneServiceResponse)) {
            String errorMessage = ServiceUtil.getErrorMessage(createPartyPhoneServiceResponse);
            Debug.logError(errorMessage, module);
            if (Debug.verboseOn()) Debug.logVerbose("Exiting method createPersonPhone", module);
            return null;
        }

        phoneId = (String) createPartyPhoneServiceResponse.get("contactMechId");

        Map createContactMechCtx =
                UtilMisc.toMap(
                        "userLogin",
                        userLogin,
                        "partyId",
                        partyId,
                        "contactMechId",
                        phoneId,
                        "contactMechPurposeTypeId",
                        "PRIMARY_PHONE");
        Map<String, Object> phonePurpose = null;
        try {
            phonePurpose = dispatcher.runSync("createPartyContactMechPurpose", createContactMechCtx);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            if (Debug.verboseOn()) Debug.logVerbose("Exiting method createPersonPhone", module);
            return null;
        }

        return phoneId;
    }

    /**
     * Creates a primary telecom number for the party and returns the id of the created phone contact
     * mech id.
     *
     * @param partyId
     * @param address
     * @param userLogin
     * @param dispatcher
     * @return
     */
    public static String createPersonAddress(
            String partyId, PostalAddress address, GenericValue userLogin, LocalDispatcher dispatcher) {
        String addressId;
        Map<String, Object> addrMap = GeoModelBuilder.buildPostalAddressMap(address);
        addrMap.put("userLogin", userLogin);
        addrMap.put("partyId", partyId);

        // invoke the create address service
        Map<String, Object> addrResp = FastMap.newInstance();
        try {
            addrResp = dispatcher.runSync("extCreatePartyPostalAddress", addrMap);

            // update location and locationContactMechId
            GenericValue createdPostalAddress = (GenericValue) addrResp.get("createdPostalAddress");
            Delegator delegator = userLogin.getDelegator();
            GenericValue personPartyRecord = HierarchyUtils.getPartyByPartyId(delegator, partyId);

            personPartyRecord.set("location", createdPostalAddress.getString("formattedAddress"));
            personPartyRecord.set(
                    "locationContactMechId", createdPostalAddress.getString("contactMechId"));
            delegator.store(personPartyRecord);

        } catch (GenericServiceException | GenericEntityException ex) {
            Debug.logError(ex, module);
        }

        addressId = (String) addrResp.get("contactMechId");

        return addressId;
    }
}
