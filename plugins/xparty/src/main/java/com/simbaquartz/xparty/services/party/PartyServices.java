package com.simbaquartz.xparty.services.party;

import com.fidelissd.zcp.xcommon.collections.FastList;
import com.fidelissd.zcp.xcommon.collections.FastMap;
import com.fidelissd.zcp.xcommon.services.contact.EmailTypesEnum;
import com.fidelissd.zcp.xcommon.util.hierarchy.HierarchyUtils;
import com.simbaquartz.xparty.helpers.AxPartyHelper;
import org.apache.ofbiz.base.util.*;
import org.apache.ofbiz.entity.*;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.service.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;

public class PartyServices {
    public static final String module = PartyServices.class.getName();
    public static final String resource = "PartyUiLabels";
    public static final String resourceError = "PartyErrorUiLabels";

    public static Map<String, Object> populateParentPartyDetails(
            Delegator delegator, Map<String, Object> partyMap, String partyId) {
        try {
            GenericValue parentParty =
                    EntityQuery.use(delegator)
                            .from("PartyRelationship")
                            .where("partyIdTo", partyId, "partyRelationshipTypeId", "ORG_ROLLUP")
                            .filterByDate()
                            .queryFirst();
            if (UtilValidate.isNotEmpty(parentParty)) {
                String parentPartyId = parentParty.getString("partyIdFrom");
                String parentPartyName = AxPartyHelper.getPartyName(delegator, parentPartyId);
                partyMap.put("parentPartyId", parentPartyId);
                partyMap.put("parentPartyName", parentPartyName);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return null;
    }

    /**
     * * Get Party Details
     *
     * @param context
     * @return
     */
    public static Map<String, Object> fsdGetPartyDetails(
            DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();

        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        Map<String, Object> partyMap = FastMap.newInstance();
        String partyId = (String) context.get("partyId");

        String groupName = AxPartyHelper.getPartyName(delegator, partyId);
        if (UtilValidate.isNotEmpty(partyId)) {
            Map<String, Object> generatePublicResourceUrlResp = null;
            try {
                generatePublicResourceUrlResp =
                        dispatcher.runSync(
                                "generatePublicResourceUrl",
                                UtilMisc.toMap("userLogin", userLogin, "partyId", partyId));
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
            }

            if (UtilValidate.isNotEmpty(generatePublicResourceUrlResp)) {
                String publicResourceUrl = (String) generatePublicResourceUrlResp.get("publicResourceUrl");
                partyMap.put("logoImageUrl", publicResourceUrl);
                String thumbNailUrl = (String) generatePublicResourceUrlResp.get("thumbNailUrl");
                partyMap.put("thumbNailUrl", thumbNailUrl);
            }
        }

        if (UtilValidate.isNotEmpty(groupName)) {
            partyMap.put("partyId", partyId);
            partyMap.put("partyName", groupName);
        }
        GenericValue partyRcd = HierarchyUtils.getPartyByPartyId(delegator, partyId);

        partyMap.put("partyObj", partyRcd);
        Map firstAndLastNameMap = new HashMap();
        if (UtilValidate.isNotEmpty(partyRcd)) {
            firstAndLastNameMap = AxPartyHelper.getPartyNameDetails(delegator, partyRcd);
        }

        if (UtilValidate.isNotEmpty(firstAndLastNameMap)) {
            String firstName = (String) firstAndLastNameMap.get("firstName");
            String middleName = (String) firstAndLastNameMap.get("middleName");
            String lastName = (String) firstAndLastNameMap.get("lastName");
            String personalTitle = (String) firstAndLastNameMap.get("personalTitle");
            String companyName = (String) firstAndLastNameMap.get("companyName");
            String displayName = (String) firstAndLastNameMap.get("displayName");
            partyMap.put("firstName", firstName);
            partyMap.put("middleName", middleName);
            partyMap.put("lastName", lastName);
            partyMap.put("personalTitle", personalTitle);
            partyMap.put("companyName", companyName);
            partyMap.put("displayName", displayName);
        }

        if (UtilValidate.isNotEmpty(partyRcd)) {

            // fetch postal Addresses
            List<GenericValue> postalAddresses = null;
            try {
                postalAddresses = AxPartyHelper.getPostalAddresses(delegator, partyRcd);
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
            partyMap.put("postalAddresses", postalAddresses);

            // fetch phone numbers
            List<GenericValue> phoneNumbers = null;
            try {
                phoneNumbers = AxPartyHelper.getPartyContactNumbers(delegator, partyRcd);
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
            partyMap.put("phoneNumbers", phoneNumbers);

            // fetch email addresses
            List<GenericValue> emailAddress = null;
            try {
                emailAddress = AxPartyHelper.getEmailAddresses(partyRcd);
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
            partyMap.put("emailAddress", emailAddress);

            // fetch web addresses
            List<GenericValue> webAddress = null;
            try {
                webAddress = AxPartyHelper.getPartyWebAddresses(delegator, partyRcd);
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
            partyMap.put("webAddress", webAddress);

            // fetch parent party Id
            Map<String, Object> ex = populateParentPartyDetails(delegator, partyMap, partyId);
            if (ex != null) return ex;
        }
        result.put("partyDetails", partyMap);
        return result;
    }

    /**
     * * create new customer
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> fsdCreateGovernmentCustomer(
            DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String parentPartyId = (String) context.get("parentPartyId");
        String parentRoleTypeId = (String) context.get("parentRoleTypeId");
        String groupName = (String) context.get("groupName");
        String countryCode = (String) context.get("countryCode");
        String areaCode = (String) context.get("areaCode");
        String contactNumber = (String) context.get("contactNumber");
        String extension = (String) context.get("extension");
        String contactMechPurposeTypeId = (String) context.get("contactMechPurposeTypeId");
        String emailAddress = (String) context.get("emailAddress");
        String infoString = (String) context.get("infoString");
        String industryType = (String) context.get("industryType");
        String officeSiteName = (String) context.get("siteName");
        Long numEmployees = (Long) context.get("numEmployees");
        BigDecimal annualRevenue = (BigDecimal) context.get("annualRevenue");

        Map<String, Object> createOrgCtx = FastMap.newInstance();
        createOrgCtx.put("parentPartyId", parentPartyId);
        createOrgCtx.put("parentRoleTypeId", parentRoleTypeId);
        createOrgCtx.put("groupName", groupName);
        createOrgCtx.put("userLogin", context.get("userLogin"));
        createOrgCtx.put("officeSiteName", officeSiteName);
        createOrgCtx.put("annualRevenue", annualRevenue);
        createOrgCtx.put("numEmployees", numEmployees);
        createOrgCtx.put("industryType", industryType);

        Map<String, Object> createOrgResult = null;
        try {
            createOrgResult = dispatcher.runSync("fsdCreateCustomerGovernmentOrg", createOrgCtx);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        String orgPartyId = (String) createOrgResult.get("partyId");

        // check if sales rep relationship needs to be established
        Map<String, Object> createPartyRelationshipCtx = FastMap.newInstance();
        createPartyRelationshipCtx.put("partyIdFrom", userLogin.get("partyId"));
        createPartyRelationshipCtx.put("partyIdTo", orgPartyId);
        createPartyRelationshipCtx.put("roleTypeIdFrom", "SALES_REP");
        createPartyRelationshipCtx.put("roleTypeIdTo", "CUSTOMER");
        createPartyRelationshipCtx.put("partyRelationshipTypeId", "CUSTOMER_REL");
        createPartyRelationshipCtx.put("userLogin", context.get("userLogin"));

        // build a sales rep relationship
        Map<String, Object> createSalesRepResult = null;
        try {
            createSalesRepResult =
                    dispatcher.runSync("createPartyRelationship", createPartyRelationshipCtx);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        // create primary phone number
        if (UtilValidate.isNotEmpty(contactNumber)) {
            Map<String, Object> createOrgPhoneCtx = FastMap.newInstance();
            createOrgPhoneCtx.put("partyId", orgPartyId);
            createOrgPhoneCtx.put("countryCode", countryCode);
            createOrgPhoneCtx.put("areaCode", areaCode);
            createOrgPhoneCtx.put("contactNumber", contactNumber);
            createOrgPhoneCtx.put("extension", extension);
            createOrgPhoneCtx.put("contactMechPurposeTypeId", "PRIMARY_PHONE");
            createOrgPhoneCtx.put("userLogin", context.get("userLogin"));

            Map<String, Object> createOrgPhoneResult = null;
            try {
                createOrgPhoneResult = dispatcher.runSync("createPartyTelecomNumber", createOrgPhoneCtx);
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
        }

        // create primary email address
        if (UtilValidate.isNotEmpty(emailAddress)) {
            Map<String, Object> createOrgEmailCtx = FastMap.newInstance();
            createOrgEmailCtx.put("partyId", orgPartyId);
            createOrgEmailCtx.put("emailAddress", emailAddress);
            createOrgEmailCtx.put("contactMechPurposeTypeId", EmailTypesEnum.PRIMARY.getTypeId());
            createOrgEmailCtx.put("userLogin", context.get("userLogin"));

            Map<String, Object> createOrgEmailResult = null;
            try {
                createOrgEmailResult = dispatcher.runSync("createPartyEmailAddress", createOrgEmailCtx);
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
        }

        // create web address if not empty
        if (UtilValidate.isNotEmpty(infoString)) {
            Map<String, Object> createOrgWebCtx = FastMap.newInstance();
            createOrgWebCtx.put("userLogin", context.get("userLogin"));
            createOrgWebCtx.put("partyId", orgPartyId);
            createOrgWebCtx.put("infoString", infoString);
            createOrgWebCtx.put("contactMechTypeId", "WEB_ADDRESS");
            createOrgWebCtx.put("contactMechPurposeTypeId", "PRIMARY_WEB_URL");

            Map<String, Object> createOrgWebResult = null;
            try {
                createOrgWebResult = dispatcher.runSync("createPartyContactMech", createOrgWebCtx);
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
        }

        result.put("partyId", orgPartyId);
        return result;
    }

    /**
     * * create new supplier
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> fsdCreateNewSupplier(
            DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String partyId = (String) context.get("partyId");
        String groupName = (String) context.get("groupName");
        String supplierAccountLeadPartyId = (String) context.get("supplierAccountLeadPartyId");
        String description = (String) context.get("description");
        String countryCode = (String) context.get("countryCode");
        String areaCode = (String) context.get("areaCode");
        String contactNumber = (String) context.get("contactNumber");
        String extension = (String) context.get("extension");
        String toName = (String) context.get("toName");
        String attnName = (String) context.get("attnName");
        String address1 = (String) context.get("address1");
        String address2 = (String) context.get("address2");
        String city = (String) context.get("city");
        String stateProvinceGeoId = (String) context.get("stateProvinceGeoId");
        String postalCode = (String) context.get("postalCode");
        String countryGeoId = (String) context.get("countryGeoId");
        List phonePurposes = (List) context.get("phone_purposes");
        List addressPurposes = (List) context.get("address_purposes");
        String emailAddress = (String) context.get("emailAddress");
        String webAddress = (String) context.get("webAddress");

        Map<String, Object> createSupplierCtx = FastMap.newInstance();
        createSupplierCtx.put("userLogin", context.get("userLogin"));
        createSupplierCtx.put("partyId", partyId);
        createSupplierCtx.put("groupName", groupName);
        createSupplierCtx.put("description", description);
        createSupplierCtx.put("countryCode", countryCode);
        createSupplierCtx.put("areaCode", areaCode);
        createSupplierCtx.put("contactNumber", contactNumber);
        createSupplierCtx.put("extension", extension);
        createSupplierCtx.put("toName", toName);
        createSupplierCtx.put("attnName", attnName);
        createSupplierCtx.put("address1", address1);
        createSupplierCtx.put("address2", address2);
        createSupplierCtx.put("stateProvinceGeoId", stateProvinceGeoId);
        createSupplierCtx.put("city", city);
        createSupplierCtx.put("postalCode", postalCode);
        createSupplierCtx.put("countryGeoId", countryGeoId);
        createSupplierCtx.put("phone_purposes", phonePurposes);
        createSupplierCtx.put("address_purposes", addressPurposes);

        Map<String, Object> createSupplierResponse = null;
        try {
            createSupplierResponse = dispatcher.runSync("fsdCreateSupplierPartyGroup", createSupplierCtx);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        String supplierPartyId = (String) createSupplierResponse.get("partyId");

        // Creating _NA_ role
        if (!HierarchyUtils.checkPartyRole(
                HierarchyUtils.getPartyByPartyId(delegator, supplierPartyId), "_NA_")) {
            Map<String, Object> createPartyRoleCtx = FastMap.newInstance();
            createPartyRoleCtx.put("partyId", supplierPartyId);
            createPartyRoleCtx.put("roleTypeId", "_NA_");
            createPartyRoleCtx.put("userLogin", userLogin);

            Map createPartyRoleResponse = null;
            try {
                createPartyRoleResponse = dispatcher.runSync("createPartyRole", createPartyRoleCtx);
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
        }

        if (UtilValidate.isNotEmpty(supplierAccountLeadPartyId)) {
            // ensure user has the role
            if (!HierarchyUtils.checkPartyRole(
                    HierarchyUtils.getPartyByPartyId(delegator, supplierAccountLeadPartyId),
                    "ACCOUNT_LEAD")) {
                Map<String, Object> createPartyRoleCtx = FastMap.newInstance();
                createPartyRoleCtx.put("partyId", supplierAccountLeadPartyId);
                createPartyRoleCtx.put("roleTypeId", "ACCOUNT_LEAD");
                createPartyRoleCtx.put("userLogin", userLogin);
                Map createPartyRoleResponse = null;
                try {
                    createPartyRoleResponse = dispatcher.runSync("createPartyRole", createPartyRoleCtx);
                } catch (GenericServiceException e) {
                    Debug.logError(e, module);
                    return ServiceUtil.returnError(e.getMessage());
                }
            }

            // ensure user has the partner role
            if (!HierarchyUtils.checkPartyRole(
                    HierarchyUtils.getPartyByPartyId(delegator, supplierPartyId), "PARTNER")) {
                Map<String, Object> createPartyRoleCtx = FastMap.newInstance();
                createPartyRoleCtx.put("partyId", supplierPartyId);
                createPartyRoleCtx.put("roleTypeId", "PARTNER");
                createPartyRoleCtx.put("userLogin", userLogin);

                Map createPartyRoleResponse = null;
                try {
                    createPartyRoleResponse = dispatcher.runSync("createPartyRole", createPartyRoleCtx);
                } catch (GenericServiceException e) {
                    Debug.logError(e, module);
                    return ServiceUtil.returnError(e.getMessage());
                }
            }

            // build a relationship
            Map<String, Object> createSupplierPartyRelationshipCtx = FastMap.newInstance();
            createSupplierPartyRelationshipCtx.put("partyIdFrom", supplierAccountLeadPartyId);
            createSupplierPartyRelationshipCtx.put("partyIdTo", supplierPartyId);
            createSupplierPartyRelationshipCtx.put("roleTypeIdFrom", "ACCOUNT_LEAD");
            createSupplierPartyRelationshipCtx.put("roleTypeIdTo", "SUPPLIER");
            createSupplierPartyRelationshipCtx.put("partyRelationshipTypeId", "ACCOUNT");
            createSupplierPartyRelationshipCtx.put("userLogin", userLogin);

            Map createSalesRepResult = null;
            try {
                createSalesRepResult =
                        dispatcher.runSync("createPartyRelationship", createSupplierPartyRelationshipCtx);
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
        }

        // create email Address for the supplier
        if (UtilValidate.isNotEmpty(emailAddress)) {
            Map<String, Object> createPartyEmailAddressCtx = FastMap.newInstance();
            createPartyEmailAddressCtx.put("partyId", supplierPartyId);
            createPartyEmailAddressCtx.put("emailAddress", emailAddress);
            createPartyEmailAddressCtx.put("contactMechPurposeTypeId", EmailTypesEnum.PRIMARY.getTypeId());
            createPartyEmailAddressCtx.put("userLogin", userLogin);

            Map createPartyEmailAddressResponse = null;
            try {
                createPartyEmailAddressResponse =
                        dispatcher.runSync("createUpdatePartyEmailAddress", createPartyEmailAddressCtx);
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
        }

        // create web Address for the supplier
        if (UtilValidate.isNotEmpty(webAddress)) {
            Map<String, Object> createPartyWebAddressCtx = FastMap.newInstance();
            createPartyWebAddressCtx.put("partyId", supplierPartyId);
            createPartyWebAddressCtx.put("infoString", webAddress);
            createPartyWebAddressCtx.put("contactMechPurposeTypeId", "PRIMARY_WEB_URL");
            createPartyWebAddressCtx.put("contactMechTypeId", "WEB_ADDRESS");
            createPartyWebAddressCtx.put("userLogin", userLogin);

            Map createPartyWebAddressResponse = null;
            try {
                createPartyWebAddressResponse =
                        dispatcher.runSync("createPartyContactMech", createPartyWebAddressCtx);
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
        }

        result.put("partyId", supplierPartyId);
        return result;
    }

    /**
     * * Get list of party ids found based on input list of email ids
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> getAllPartiesFromEmailList(
            DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<String, Object>();
        Delegator delegator = dctx.getDelegator();
        List<String> emailList = (List) context.get("emails");
        Locale locale = (Locale) context.get("locale");
        List<GenericValue> partiesList = FastList.newInstance();

        if (emailList.isEmpty()) {
            return ServiceUtil.returnError(
                    UtilProperties.getMessage(
                            resourceError, "partyservices.required_parameter_email_cannot_be_empty", locale));
        }

        try {
            List<EntityCondition> orCondList = new LinkedList<EntityCondition>();
            for (String email : emailList) {
                orCondList.add(EntityCondition.makeCondition("infoString", EntityOperator.EQUALS, email));
            }
            EntityCondition orCond = EntityCondition.makeCondition(orCondList, EntityOperator.OR);
            partiesList =
                    EntityQuery.use(delegator)
                            .select("infoString", "partyId")
                            .from("PartyAndContactMech")
                            .where(orCond)
                            .queryList();

        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(
                    UtilProperties.getMessage(
                            resourceError,
                            "partyservices.cannot_get_party_entities_read",
                            UtilMisc.toMap("errMessage", e.getMessage()),
                            locale));
        }

        result.put("parties", partiesList);
        return result;
    }

    /**
     * Create party id from email address.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> createPartyIdFromEmailAddress(
            DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String emailAddress = (String) context.get("emailAddress");
        String firstName = null;
        String lastName = null;
        List<GenericValue> toBeStored = new LinkedList<GenericValue>();
        Map<String, Object> findPartyFromEmailAddressResp = null;
        emailAddress = emailAddress.trim();

        if (UtilValidate.isNotEmpty(emailAddress)) {
            try {
                findPartyFromEmailAddressResp =
                        dispatcher.runSync(
                                "findPartyFromEmailAddress",
                                UtilMisc.<String, Object>toMap(
                                        "address", emailAddress, "caseInsensitive", "Y", "userLogin", userLogin));

                if (findPartyFromEmailAddressResp.get("partyId") == null) {
                    String name =
                            emailAddress.substring(
                                    0,
                                    emailAddress.contains("@")
                                            ? emailAddress.lastIndexOf("@")
                                            : emailAddress.length() - 1);
                    if (UtilValidate.isNotEmpty(name)) {
                        if (name.contains(".")) {
                            firstName = name.substring(0, name.lastIndexOf("."));
                            lastName = name.substring(name.lastIndexOf(".") + 1);
                        } else {
                            firstName = name;
                        }
                    }
                    Map<String, Object> createPersonCtx = FastMap.newInstance();
                    createPersonCtx.put("userLogin", userLogin);
                    if (UtilValidate.isNotEmpty(firstName)) {
                        createPersonCtx.put("firstName", firstName);
                    }
                    if (UtilValidate.isNotEmpty(lastName)) {
                        createPersonCtx.put("lastName", lastName);
                    }
                    Map<String, Object> createPersonResponse =
                            dispatcher.runSync("createPerson", createPersonCtx);
                    if (!ServiceUtil.isSuccess(createPersonResponse)) {
                        Debug.logError(
                                "An Error occurred while createPerson service call: "
                                        + ServiceUtil.getErrorMessage(createPersonResponse),
                                module);
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createPersonResponse));
                    }
                    String partyId = (String) createPersonResponse.get("partyId");
                    if (UtilValidate.isNotEmpty(partyId)) {
                        result.put("partyId", partyId);
                        String contactMechTypeId = "EMAIL_ADDRESS";
                        String contactMechId = delegator.getNextSeqId("ContactMech");
                        if (UtilValidate.isNotEmpty(contactMechId)) {
                            GenericValue tempContactMech =
                                    delegator.makeValue(
                                            "ContactMech",
                                            UtilMisc.toMap(
                                                    "contactMechId",
                                                    contactMechId,
                                                    "contactMechTypeId",
                                                    contactMechTypeId,
                                                    "infoString",
                                                    emailAddress));
                            toBeStored.add(tempContactMech);
                            GenericValue tempPartyContactMech =
                                    delegator.makeValue(
                                            "PartyContactMech",
                                            UtilMisc.toMap(
                                                    "partyId",
                                                    partyId,
                                                    "contactMechId",
                                                    contactMechId,
                                                    "fromDate",
                                                    UtilDateTime.nowTimestamp()));
                            toBeStored.add(tempPartyContactMech);
                        }
                        delegator.storeAll(toBeStored);

                        if (UtilValidate.isNotEmpty(contactMechId)) {
                            result.put("contactMechId", contactMechId);
                            result.put("infoString", emailAddress);
                        }
                    }

                    Map<String, Object> createPartyClassificationCtx = FastMap.newInstance();
                    createPartyClassificationCtx.put("partyId", partyId);
                    createPartyClassificationCtx.put("partyClassificationGroupId", "UNCLASSIFIED_CONTACT");
                    createPartyClassificationCtx.put("userLogin", userLogin);

                    Map createPartyClassificationResponse =
                            dispatcher.runSync("createPartyClassification", createPartyClassificationCtx);
                }
            } catch (GenericServiceException | GenericEntityException e) {
                Debug.logError(
                        "An Exception occurred while calling the createPartyIdFromEmailAddress service:"
                                + e.getMessage(),
                        module);
                return ServiceUtil.returnError(e.getMessage());
            }
        }
        return result;
    }

    /**
     * Returns true/false based on the Party Id if it is Internal Organization or Not.
     *
     * @param context
     * @return
     */
    public static Map<String, Object> isPartyInternalOrganization(
            DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String partyId = (String) context.get("partyId");
        boolean isInternalOrganization = false;
        try {
            Map<String, Object> getPartyGroupForPartyIdResp =
                    dispatcher.runSync(
                            "getPartyGroupForPartyId",
                            UtilMisc.toMap("userLogin", userLogin, "partyId", partyId));
            GenericValue parentPartyGroup =
                    (GenericValue) getPartyGroupForPartyIdResp.get("organizationPartyGroup");
            if (UtilValidate.isNotEmpty(parentPartyGroup)) {
                isInternalOrganization =
                        HierarchyUtils.checkPartyRole(parentPartyGroup, "INTERNAL_ORGANIZATIO");
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, e.getMessage(), module);
        }
        result.put("isInternalOrganization", isInternalOrganization);
        return result;
    }

    public static Map<String, Object> markUnrelatedPartiesToUnClassifiedGroup(
            DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Timestamp fromDate = (Timestamp) context.get("fromDate");
        List<String> partyIdFrom = FastList.newInstance();
        List<String> partyIdTo = FastList.newInstance();
        List<GenericValue> notRelatedParties = null;
        try {

            List<GenericValue> partyRelationShips = null;
            partyRelationShips =
                    EntityQuery.use(delegator)
                            .select("partyIdFrom", "partyIdTo")
                            .from("PartyRelationship")
                            .queryList();
            for (GenericValue partyRelationShip : partyRelationShips) {
                if (UtilValidate.isNotEmpty(partyRelationShip.get("partyIdFrom"))) {
                    partyIdFrom.add(partyRelationShip.get("partyIdFrom").toString());
                }
                if (UtilValidate.isNotEmpty(partyRelationShip.get("partyIdTo"))) {
                    partyIdTo.add(partyRelationShip.get("partyIdTo").toString());
                }
            }

            notRelatedParties =
                    EntityQuery.use(delegator)
                            .select("partyId", "createdStamp")
                            .from("Party")
                            .where(
                                    EntityCondition.makeCondition(
                                            EntityCondition.makeCondition("partyId", EntityOperator.NOT_IN, partyIdFrom),
                                            EntityOperator.AND,
                                            EntityCondition.makeCondition("partyId", EntityOperator.NOT_IN, partyIdTo)))
                            .queryList();

            int count = 0;
            for (GenericValue party : notRelatedParties) {

                Timestamp createdStamp = (Timestamp) party.get("createdStamp");
                if (createdStamp.after(fromDate)) {
                    List<GenericValue> partyClassificationList =
                            EntityQuery.use(delegator)
                                    .from("PartyClassification")
                                    .where(
                                            "partyId",
                                            party.get("partyId"),
                                            "partyClassificationGroupId",
                                            "UNCLASSIFIED_CONTACT")
                                    .queryList();
                    if (UtilValidate.isEmpty(partyClassificationList)) {
                        count++;
                        Map<String, Object> createPartyClassificationCtx = FastMap.newInstance();
                        createPartyClassificationCtx.put("partyId", party.get("partyId"));
                        createPartyClassificationCtx.put("partyClassificationGroupId", "UNCLASSIFIED_CONTACT");
                        createPartyClassificationCtx.put("userLogin", userLogin);
                        Map createPartyClassificationResponse =
                                dispatcher.runSync("createPartyClassification", createPartyClassificationCtx);
                    }
                }
            }
            Debug.logInfo("Not Related Parties are" + count, module);

        } catch (Exception e) {
            Debug.logError(e, e.getMessage(), module);
        }
        return result;
    }

    public static Map<String, Object> getPartyAgency(
            DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        String agencyPartyId = "";
        String partyId = (String) context.get("partyId");
        try {
            // if party is direct related to agency
            GenericValue directAgencyParty =
                    EntityQuery.use(delegator)
                            .from("PartyRelationship")
                            .where(
                                    "partyIdTo",
                                    partyId,
                                    "roleTypeIdFrom",
                                    "GOVERNMENT_AGENCY",
                                    "roleTypeIdTo",
                                    "GOVERNMENT_ORG",
                                    "partyRelationshipTypeId",
                                    "ORG_ROLLUP")
                            .filterByDate()
                            .queryFirst();
            if (UtilValidate.isNotEmpty(directAgencyParty)) {
                agencyPartyId = directAgencyParty.getString("partyIdFrom");
            } else { // party related with any government organization
                GenericValue party =
                        EntityQuery.use(delegator)
                                .from("PartyRelationship")
                                .where(
                                        "partyIdTo",
                                        partyId,
                                        "roleTypeIdFrom",
                                        "GOVERNMENT_ORG",
                                        "roleTypeIdTo",
                                        "GOVERNMENT_ORG",
                                        "partyRelationshipTypeId",
                                        "ORG_ROLLUP")
                                .filterByDate()
                                .queryFirst();
                if (UtilValidate.isNotEmpty(party)) {
                    String orgId = party.getString("partyIdFrom");
                    // party related with agency
                    GenericValue orgParty =
                            EntityQuery.use(delegator)
                                    .from("PartyRelationship")
                                    .where(
                                            "partyIdTo",
                                            orgId,
                                            "roleTypeIdFrom",
                                            "GOVERNMENT_AGENCY",
                                            "roleTypeIdTo",
                                            "GOVERNMENT_ORG",
                                            "partyRelationshipTypeId",
                                            "ORG_ROLLUP")
                                    .filterByDate()
                                    .queryFirst();
                    if (UtilValidate.isNotEmpty(orgParty)) {
                        agencyPartyId = orgParty.getString("partyIdFrom");
                    } else {
                        agencyPartyId = getAgencyId(delegator, agencyPartyId, orgId);
                    }
                } else {
                    // check party related with Government location
                    GenericValue locParty =
                            EntityQuery.use(delegator)
                                    .from("PartyRelationship")
                                    .where(
                                            "partyIdTo",
                                            partyId,
                                            "roleTypeIdFrom",
                                            "GOVERNMENT_ORG",
                                            "roleTypeIdTo",
                                            "GOVERNMENT_LOC",
                                            "partyRelationshipTypeId",
                                            "ORG_ROLLUP")
                                    .filterByDate()
                                    .queryFirst();
                    if (UtilValidate.isNotEmpty(locParty)) {
                        String orgId = locParty.getString("partyIdFrom");
                        // party related with any government organization
                        GenericValue orgParty =
                                EntityQuery.use(delegator)
                                        .from("PartyRelationship")
                                        .where(
                                                "partyIdTo",
                                                orgId,
                                                "roleTypeIdFrom",
                                                "GOVERNMENT_ORG",
                                                "roleTypeIdTo",
                                                "GOVERNMENT_ORG",
                                                "partyRelationshipTypeId",
                                                "ORG_ROLLUP")
                                        .filterByDate()
                                        .queryFirst();
                        if (UtilValidate.isNotEmpty(orgParty)) {
                            String orgIdFrom = orgParty.getString("partyIdFrom");
                            // party related with agency
                            GenericValue agencyParty =
                                    EntityQuery.use(delegator)
                                            .from("PartyRelationship")
                                            .where(
                                                    "partyIdTo",
                                                    orgIdFrom,
                                                    "roleTypeIdFrom",
                                                    "GOVERNMENT_AGENCY",
                                                    "roleTypeIdTo",
                                                    "GOVERNMENT_ORG",
                                                    "partyRelationshipTypeId",
                                                    "ORG_ROLLUP")
                                            .filterByDate()
                                            .queryFirst();
                            if (UtilValidate.isNotEmpty(agencyParty)) {
                                agencyPartyId = agencyParty.getString("partyIdFrom");
                            } else agencyPartyId = getAgencyId(delegator, agencyPartyId, orgIdFrom);
                        }
                    }
                }
            }
        } catch (GenericEntityException e) {
            e.printStackTrace();
        }

        result.put("agencyPartyId", agencyPartyId);
        return result;
    }

    private static String getAgencyId(Delegator delegator, String agencyPartyId, String orgIdFrom)
            throws GenericEntityException {
        // check if government organization related with other government organization
        GenericValue orgToOrgParty =
                EntityQuery.use(delegator)
                        .from("PartyRelationship")
                        .where(
                                "partyIdTo",
                                orgIdFrom,
                                "roleTypeIdFrom",
                                "GOVERNMENT_ORG",
                                "roleTypeIdTo",
                                "GOVERNMENT_ORG",
                                "partyRelationshipTypeId",
                                "ORG_ROLLUP")
                        .filterByDate()
                        .queryFirst();
        if (UtilValidate.isNotEmpty(orgToOrgParty)) {
            String orgPartyIdFrom = orgToOrgParty.getString("partyIdFrom");
            // party related with agency
            GenericValue agencyPartyRecd =
                    EntityQuery.use(delegator)
                            .from("PartyRelationship")
                            .where(
                                    "partyIdTo",
                                    orgPartyIdFrom,
                                    "roleTypeIdFrom",
                                    "GOVERNMENT_AGENCY",
                                    "roleTypeIdTo",
                                    "GOVERNMENT_ORG",
                                    "partyRelationshipTypeId",
                                    "ORG_ROLLUP")
                            .filterByDate()
                            .queryFirst();
            if (UtilValidate.isNotEmpty(agencyPartyRecd)) {
                agencyPartyId = agencyPartyRecd.getString("partyIdFrom");
            }
        }
        return agencyPartyId;
    }

    public static Map<String, Object> setCustomerDepartment(
            DispatchContext dctx, Map<String, ? extends Object> context)
            throws GenericEntityException, GenericServiceException {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String partyGroupId = (String) context.get("partyGroupId");
        String departmentName = (String) context.get("departmentName");

        GenericValue customerDepartment =
                EntityQuery.use(delegator)
                        .from("CustomerDepartment")
                        .where("departmentName", departmentName, "partyGroupId", partyGroupId)
                        .queryOne();
        String departmentId = "";
        if (UtilValidate.isEmpty(customerDepartment)) {
            departmentId = delegator.getNextSeqId("CustomerDepartment");
            GenericValue customerDepartmentRec = delegator.makeValue("CustomerDepartment");
            customerDepartmentRec.set("departmentId", departmentId);
            customerDepartmentRec.set("partyGroupId", partyGroupId);
            customerDepartmentRec.set("departmentName", departmentName);
            customerDepartmentRec.create();
        }

        result.put("departmentId", departmentId);

        return result;
    }

    public static Map<String, Object> fsdRegisterUser(
            DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        String firstName = (String) context.get("firstName");
        String lastName = (String) context.get("lastName");
        String emailAddress = (String) context.get("emailAddress");
        String currentPassword = (String) context.get("currentPassword");
        String organizationName = (String) context.get("organizationName");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Map<String, Object> findPartyFromEmailAddressResp = null;
        emailAddress = emailAddress.trim();
        List<GenericValue> toBeStored = new LinkedList<GenericValue>();
        String partyId = "";
        String partyGroupPartyId = "";

        try {

            findPartyFromEmailAddressResp =
                    dispatcher.runSync(
                            "findPartyFromEmailAddress",
                            UtilMisc.<String, Object>toMap(
                                    "address", emailAddress, "caseInsensitive", "Y", "userLogin", userLogin));

            if (findPartyFromEmailAddressResp.get("partyId") == null) {
                Map<String, Object> createPersonCtx = FastMap.newInstance();
                createPersonCtx.put("userLogin", userLogin);
                if (UtilValidate.isNotEmpty(firstName)) {
                    createPersonCtx.put("firstName", firstName);
                }
                if (UtilValidate.isNotEmpty(lastName)) {
                    createPersonCtx.put("lastName", lastName);
                }
                Map<String, Object> createPersonResponse =
                        dispatcher.runSync("createPerson", createPersonCtx);

                if (!ServiceUtil.isSuccess(createPersonResponse)) {

                    Debug.logError(
                            "An Error occurred while createPerson service call: "
                                    + ServiceUtil.getErrorMessage(createPersonResponse),
                            module);
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createPersonResponse));
                }
                partyId = (String) createPersonResponse.get("partyId");

                if (UtilValidate.isNotEmpty(partyId)) {

                    String contactMechTypeId = "EMAIL_ADDRESS";
                    String contactMechId = delegator.getNextSeqId("ContactMech");

                    if (UtilValidate.isNotEmpty(contactMechId)) {

                        GenericValue tempContactMech =
                                delegator.makeValue(
                                        "ContactMech",
                                        UtilMisc.toMap(
                                                "contactMechId",
                                                contactMechId,
                                                "contactMechTypeId",
                                                contactMechTypeId,
                                                "infoString",
                                                emailAddress));
                        toBeStored.add(tempContactMech);
                        GenericValue tempPartyContactMech =
                                delegator.makeValue(
                                        "PartyContactMech",
                                        UtilMisc.toMap(
                                                "partyId",
                                                partyId,
                                                "contactMechId",
                                                contactMechId,
                                                "fromDate",
                                                UtilDateTime.nowTimestamp()));
                        toBeStored.add(tempPartyContactMech);
                    }

                    delegator.storeAll(toBeStored);
                }

            } else {

                partyId = findPartyFromEmailAddressResp.get("partyId").toString();
            }

            // create party group
            GenericValue partyGroup =
                    EntityQuery.use(delegator)
                            .from("PartyGroup")
                            .where(UtilMisc.toMap("groupName", organizationName))
                            .queryFirst();
            if (UtilValidate.isEmpty(partyGroup)) {
                Map createPartyGroupCtx =
                        UtilMisc.toMap("userLogin", userLogin, "groupName", organizationName);
                Map<String, Object> createPartyGroupResponse = FastMap.newInstance();
                createPartyGroupResponse = dispatcher.runSync("createPartyGroup", createPartyGroupCtx);
                partyGroupPartyId = (String) createPartyGroupResponse.get("partyId");
            } else {
                partyGroupPartyId = partyGroup.getString("partyId");
            }

            // create user login
            Map<String, Object> fsdCreateUserLoginCtx = FastMap.newInstance();
            fsdCreateUserLoginCtx.put("userLogin", userLogin);
            fsdCreateUserLoginCtx.put("userLoginId", emailAddress);
            fsdCreateUserLoginCtx.put("partyId", partyId);
            fsdCreateUserLoginCtx.put("currentPassword", currentPassword);
            fsdCreateUserLoginCtx.put("currentPasswordVerify", currentPassword);
            fsdCreateUserLoginCtx.put("enabled", "Y");
            Map fsdCreateUserLoginResponse = dispatcher.runSync("createUserLogin", fsdCreateUserLoginCtx);

            // create party role
            GenericValue partyRole =
                    EntityQuery.use(delegator)
                            .from("PartyRole")
                            .where(UtilMisc.toMap("partyId", partyId, "roleTypeId", "GUEST"))
                            .queryOne();
            if (UtilValidate.isEmpty(partyRole)) {
                Map<String, Object> createPartyRoleCtx = FastMap.newInstance();
                createPartyRoleCtx.put("partyId", partyId);
                createPartyRoleCtx.put("roleTypeId", "GUEST");
                createPartyRoleCtx.put("userLogin", userLogin);
                Map createPartyRoleResponse = dispatcher.runSync("createPartyRole", createPartyRoleCtx);
            }

            // create party role for party group Id.
            GenericValue partyGroupRole =
                    EntityQuery.use(delegator)
                            .from("PartyRole")
                            .where(
                                    UtilMisc.toMap(
                                            "partyId", partyGroupPartyId, "roleTypeId", "INTERNAL_ORGANIZATIO"))
                            .queryOne();
            if (UtilValidate.isEmpty(partyGroupRole)) {
                Map<String, Object> createPartyGroupRoleCtx = FastMap.newInstance();
                createPartyGroupRoleCtx.put("partyId", partyGroupPartyId);
                createPartyGroupRoleCtx.put("roleTypeId", "INTERNAL_ORGANIZATIO");
                createPartyGroupRoleCtx.put("userLogin", userLogin);
                Map createPartyGroupRoleResponse =
                        dispatcher.runSync("createPartyRole", createPartyGroupRoleCtx);
            }

            // create party relationship
            Map createPartyRelationshipCtx =
                    UtilMisc.toMap(
                            "userLogin",
                            userLogin,
                            "partyIdFrom",
                            partyGroupPartyId,
                            "partyIdTo",
                            partyId,
                            "roleTypeIdTo",
                            "GUEST",
                            "roleTypeIdFrom",
                            "_NA_",
                            "partyRelationshipTypeId",
                            "AUTHENTICATED_USER");

            Map<String, Object> createPartyRelationshipResp =
                    dispatcher.runSync("createPartyRelationship", createPartyRelationshipCtx);
            if (!ServiceUtil.isSuccess(createPartyRelationshipResp)) {
                return createPartyRelationshipResp;
            }

        } catch (Exception e) {
            Debug.logError(e, e.getMessage(), module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return result;
    }

    public static Map<String, Object> getUserLoginDetails(
            DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> userLoginDetails = FastMap.newInstance();
        String userLoginId = (String) context.get("userLoginId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        LocalDispatcher dispatcher = dctx.getDispatcher();

        Map<String, Object> getUserLoginDetails = FastMap.newInstance();
        getUserLoginDetails.put("userLoginId", userLoginId);
        try {
            getUserLoginDetails = dispatcher.runSync("getPartyFromUserLogin", getUserLoginDetails);
            List<Map<String, GenericValue>> allParties =
                    (List<Map<String, GenericValue>>) getUserLoginDetails.get("parties");
            if (UtilValidate.isNotEmpty(allParties) && UtilValidate.isNotEmpty(allParties.get(0))) {
                String partyId = allParties.get(0).get("party").getString("partyId");
                GenericValue personRecord =
                        EntityQuery.use(delegator)
                                .from("Person")
                                .where("partyId", partyId)
                                .cache(true)
                                .queryOne();
                Map<String, Object> getPartyGroupForPartyIdCtx = FastMap.newInstance();
                getPartyGroupForPartyIdCtx.put("userLogin", userLogin);
                getPartyGroupForPartyIdCtx.put("partyId", partyId);
                userLoginDetails =
                        dispatcher.runSync("getPartyGroupForPartyId", getPartyGroupForPartyIdCtx);
                userLoginDetails.put("firstName", personRecord.getString("firstName"));
                userLoginDetails.put("lastName", personRecord.getString("lastName"));
                result.put("userLoginDetails", userLoginDetails);
            }

        } catch (GenericServiceException | GenericEntityException e) {
            Debug.logError(e, e.getMessage(), module);
            return ServiceUtil.returnError(e.getMessage());
        }
        return result;
    }

    /**
     * Fetch the Internal Organization for its respective tenant Id.
     *
     * @param context
     * @return
     */
    public static Map<String, Object> fetchInternalOrganization(
            DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();

        String partyId = (String) context.get("partyId");
        Delegator delegator = dctx.getDelegator();
        delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");

        try {
            if (UtilValidate.isNotEmpty(delegator)) {
                GenericValue orgPartyRelation =
                        EntityQuery.use(delegator)
                                .from("PartyRelationship")
                                .where("partyIdTo", partyId, "roleTypeIdFrom", "INTERNAL_ORGANIZATIO")
                                .queryFirst();
                if (UtilValidate.isNotEmpty(orgPartyRelation)) {
                    String orgPartyId = orgPartyRelation.getString("partyIdFrom");
                    GenericValue partyGroup =
                            EntityQuery.use(delegator)
                                    .select("groupName")
                                    .from("PartyGroup")
                                    .where("partyId", orgPartyId)
                                    .queryOne();
                    if (UtilValidate.isNotEmpty(partyGroup)) {
                        result.put("organizationPartyId", orgPartyId);
                        result.put("organizationName", partyGroup.getString("groupName"));
                    }
                }
            }

        } catch (GenericEntityException e) {
            Debug.logError(e, e.getMessage(), module);
            return ServiceUtil.returnError(e.getMessage());
        }
        return result;
    }

    /**
     * Extended PartyContactMechPurpose service Creates a PartyContactMechPurpose with label
     * <b>security check</b>: userLogin partyId must equal partyId, or must have PARTYMGR_CREATE
     * permission
     *
     * @param ctx     The DispatchContext that this service is operating in
     * @param context Map containing the input parameters
     * @return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> axCreatePartyContactMechPurpose(
            DispatchContext ctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String partyId =
                ServiceUtil.getPartyIdCheckSecurity(
                        userLogin, security, context, result, "PARTYMGR", "_PCM_CREATE");
        String errMsg = null;
        Locale locale = (Locale) context.get("locale");

        String label = (String) context.get("label");

        if (result.size() > 0) {
            return result;
        }

        // required parameters
        String contactMechId = (String) context.get("contactMechId");
        String contactMechPurposeTypeId = (String) context.get("contactMechPurposeTypeId");
        Timestamp fromDate = (Timestamp) context.get("fromDate");

        GenericValue tempVal = null;
        //    try {
        //      tempVal = EntityQuery.use(delegator).from("PartyContactWithPurpose")
        //              .where("partyId", partyId, "contactMechId", contactMechId,
        // "contactMechPurposeTypeId", contactMechPurposeTypeId)
        //              .filterByDate("contactFromDate", "contactThruDate", "purposeFromDate",
        // "purposeThruDate")
        //              .queryFirst();
        //    } catch (GenericEntityException e) {
        //      Debug.logWarning(e.getMessage(), module);
        //      tempVal = null;
        //    }

        if (UtilValidate.isEmpty(fromDate)) {
            fromDate = UtilDateTime.nowTimestamp();
        }

        //    if (tempVal != null) {
        //      // exists already with valid date, show warning
        //      errMsg = UtilProperties.getMessage(resourceError,
        //              "contactmechservices.could_not_create_new_purpose_already_exists", locale);
        //      errMsg += ": " + tempVal.getPrimaryKey().toString();
        //      return ServiceUtil.returnError(errMsg);
        //    }
        try {
            // preparing conditions
            List<EntityCondition> cond = new LinkedList<EntityCondition>();
            cond.add(EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId));
            cond.add(
                    EntityCondition.makeCondition("contactMechId", EntityOperator.EQUALS, contactMechId));
            cond.add(
                    EntityCondition.makeCondition(
                            "contactMechPurposeTypeId", EntityOperator.EQUALS, EmailTypesEnum.PRIMARY.getTypeId()));
            cond.add(EntityCondition.makeCondition("thruDate", EntityOperator.EQUALS, null));
            GenericValue partyContactMechPurpose =
                    EntityQuery.use(delegator).from("PartyContactMechPurpose").where(cond).queryFirst();
            if (UtilValidate.isNotEmpty(partyContactMechPurpose)) {
                partyContactMechPurpose.set("label", label);
                partyContactMechPurpose.store();
            } else {
                // no entry with a valid date range exists, create new with open thruDate
                GenericValue newPartyContactMechPurpose =
                        delegator.makeValue(
                                "PartyContactMechPurpose",
                                UtilMisc.toMap(
                                        "partyId",
                                        partyId,
                                        "contactMechId",
                                        contactMechId,
                                        "contactMechPurposeTypeId",
                                        contactMechPurposeTypeId,
                                        "fromDate",
                                        fromDate,
                                        "label",
                                        label));

                delegator.create(newPartyContactMechPurpose);
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
            return ServiceUtil.returnError(
                    UtilProperties.getMessage(
                            resourceError,
                            "contactmechservices.could_not_add_purpose_write",
                            UtilMisc.toMap("errMessage", e.getMessage()),
                            locale));
        }

        result.put("fromDate", fromDate);
        result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
        return result;
    }

    public static Map<String, Object> changeAppUserPermissions(
            DispatchContext ctx, Map<String, ? extends Object> context)
            throws GenericEntityException, GenericServiceException {
        //
        String accountId = (String) context.get("accountId");

        String groupId = (String) context.get("groupId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();

        List<EntityCondition> orCondition = new LinkedList<>();
        orCondition.add(EntityCondition.makeCondition("groupId", EntityOperator.EQUALS, "ORG_ADMIN"));
        orCondition.add(EntityCondition.makeCondition("groupId", EntityOperator.EQUALS, "ORG_MANAGER"));
        orCondition.add(EntityCondition.makeCondition("groupId", EntityOperator.EQUALS, "ORG_MEMBER"));

        EntityCondition orCond = EntityCondition.makeCondition(orCondition, EntityOperator.OR);
        List<EntityCondition> andCondition =
                UtilMisc.toList(
                        EntityCondition.makeCondition("userLoginId", EntityOperator.EQUALS, accountId));

        EntityCondition andCond = EntityCondition.makeCondition(andCondition, EntityOperator.AND);

        List<GenericValue> userLoginSecurityGroupList =
                EntityQuery.use(delegator)
                        .from("UserLoginSecurityGroup")
                        .where(orCond, andCond)
                        .queryList();

        if (UtilValidate.isNotEmpty(userLoginSecurityGroupList)) {
            for (GenericValue userLoginSecurityGroup : userLoginSecurityGroupList) {
                Timestamp fromDate = (Timestamp) userLoginSecurityGroup.get("fromDate");
                String groupIdToRemove = userLoginSecurityGroup.getString("groupId");
                dispatcher.runSync(
                        "removeUserLoginToSecurityGroup",
                        UtilMisc.toMap(
                                "userLogin",
                                userLogin,
                                "fromDate",
                                fromDate,
                                "groupId",
                                groupIdToRemove,
                                "userLoginId",
                                accountId));
            }
        }

        dispatcher.runSync(
                "addUserLoginToSecurityGroup",
                UtilMisc.toMap("userLogin", userLogin, "groupId", groupId, "userLoginId", accountId));
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> getAppUserPermission(
            DispatchContext ctx, Map<String, ? extends Object> context)
            throws GenericEntityException, GenericServiceException {
        String accountId = (String) context.get("accountId");
        // get user login from party id
        String userLoginId = null;
        Delegator delegator = ctx.getDelegator();
        try {
            GenericValue existingUserLogin =
                    EntityQuery.use(delegator).from("UserLogin").where("partyId", accountId).queryFirst();
            if (UtilValidate.isNotEmpty(existingUserLogin)) {
                userLoginId = (String) existingUserLogin.get("userLoginId");
            }
        } catch (GenericEntityException e) {
            // Handle error here
            e.printStackTrace();
            return ServiceUtil.returnError("Unable to locate User");
        }
        Map<String, Object> result = ServiceUtil.returnSuccess();
        List<GenericValue> userLoginSecurityGroupData = new LinkedList<GenericValue>();
        LocalDispatcher dispatcher = ctx.getDispatcher();

        List<EntityCondition> orCondition = new LinkedList<EntityCondition>();
        orCondition.add(EntityCondition.makeCondition("groupId", EntityOperator.EQUALS, "ORG_ADMIN"));
        orCondition.add(EntityCondition.makeCondition("groupId", EntityOperator.EQUALS, "ORG_MANAGER"));
        orCondition.add(EntityCondition.makeCondition("groupId", EntityOperator.EQUALS, "ORG_MEMBER"));

        EntityCondition orCond = EntityCondition.makeCondition(orCondition, EntityOperator.OR);
        List<EntityCondition> andCondition =
                UtilMisc.toList(
                        EntityCondition.makeCondition("userLoginId", EntityOperator.EQUALS, userLoginId));

        EntityCondition andCond = EntityCondition.makeCondition(andCondition, EntityOperator.AND);

        List<GenericValue> userLoginSecurityGroupList =
                EntityQuery.use(delegator)
                        .from("UserLoginSecurityGroup")
                        .where(orCond, andCond)
                        .queryList();

        result.put("permissions", userLoginSecurityGroupList);
        return result;
    }

    public static Map<String, Object> getPartyDepartment(
            DispatchContext dctx, Map<String, ? extends Object> context) throws GenericEntityException {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String partyId = (String) context.get("partyId");

        GenericValue partyAttribute =
                EntityQuery.use(delegator)
                        .from("PartyAttribute")
                        .where(UtilMisc.toMap("partyId", partyId, "attrName", "Department"))
                        .queryOne();
        if (UtilValidate.isNotEmpty(partyAttribute)) {
            serviceResult.put("department", partyAttribute.getString("attrValue"));
        }
        return serviceResult;
    }
}
