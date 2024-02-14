package com.simbaquartz.xapi.connect.api.admin.orgOnboard.impl;

import com.fidelissd.zcp.xcommon.collections.FastMap;
import com.fidelissd.zcp.xcommon.helpers.PartyAttributeHelper;
import com.fidelissd.zcp.xcommon.models.client.Invitation;
import com.fidelissd.zcp.xcommon.models.geo.PostalAddress;
import com.fidelissd.zcp.xcommon.services.contact.EmailTypesEnum;
import com.fidelissd.zcp.xcommon.util.hierarchy.HierarchyUtils;
import com.simbaquartz.xapi.connect.api.admin.models.Onboard;
import com.simbaquartz.xapi.connect.api.admin.orgOnboard.AdminOrgOnboardApiService;
import com.simbaquartz.xapi.connect.api.common.ApiMessageConstants;
import com.simbaquartz.xapi.connect.utils.ApiResponseUtil;
import com.simbaquartz.xapi.connect.validation.EmailValidator;
import com.simbaquartz.xparty.hierarchy.role.AccountRoles;
import com.simbaquartz.xparty.services.account.AccountServices;
import org.apache.ofbiz.base.crypto.HashCrypt;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.common.login.LoginServices;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.GenericDispatcherFactory;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminOrgOnboardApiServiceImpl extends AdminOrgOnboardApiService {
    private static final String module = AdminOrgOnboardApiServiceImpl.class.getName();

    @Override
    public Response onboardNewOrg(Onboard onboard) {
        Map<String, Object> responseMap = new HashMap<>();
        if (validateUserLoginId(onboard)) {
            return ApiResponseUtil.prepareDefaultResponse(Response.Status.FORBIDDEN, "UserLoginId Already Exists.");
        }
        Onboard.OrgAdminInfo adminInfo = onboard.getAdmin();
        if (UtilValidate.isNotEmpty(adminInfo.getEmail()) && !EmailValidator.isValidEmail(adminInfo.getEmail())) {
            return ApiResponseUtil.prepareDefaultResponse(Response.Status.BAD_REQUEST, ApiMessageConstants.MSG_INVALID_EMAIL_FORMAT + adminInfo.getEmail());
        }

        String sha1AdminPassword = HashCrypt.cryptUTF8(LoginServices.getHashType(), null, adminInfo.getPassword());
        Map<String, Object> createOrgAdminResp;
        String adminPersonPartyId = null;
        try {
            createOrgAdminResp = createOrgAdminUserLogin(adminInfo.getEmail(), sha1AdminPassword, adminInfo.getFirstName(), adminInfo.getLastName());
            adminPersonPartyId = (String) createOrgAdminResp.get("partyId");
        } catch (GenericServiceException | GenericEntityException e) {
            Debug.logError("An error occurred while invoking registerUser service, details: " + e.getMessage(), module);

            return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR);
        }

        String orgPartyId;
        try {
            orgPartyId = setupOrganization(onboard, adminPersonPartyId);
        } catch (GenericServiceException | GenericEntityException e) {
            Debug.logError("An error occurred while invoking org on-boarding service, details: " + e.getMessage(), module);
            return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR);
        }
        if (UtilValidate.isEmpty(orgPartyId)) {
            return ApiResponseUtil.prepareDefaultResponse(Response.Status.BAD_REQUEST, "Unable to onboard the new org, please make sure all details are present");
        }

        // Adding admin (first user) to the org as employee
        Invitation employeeInvite = new Invitation();
        employeeInvite.setEmail(adminInfo.getEmail());
        employeeInvite.setFullName(adminInfo.getFirstName() + " " + adminInfo.getLastName());
        employeeInvite.setRoleIds(UtilMisc.toList("ORG_ADMIN", "ORG_MANAGER"));
        // inviteAdminEmployee(UtilMisc.toList(employeeInvite)); // TODO: enable the email sending service after SendGrid Integration

        // TODO: Prepare response object with org object & admin object in it
        responseMap.put("partyId", createOrgAdminResp.get("partyId"));
        responseMap.put("orgPartyId", orgPartyId);
        return ApiResponseUtil.prepareOkResponse(responseMap);
    }

    private boolean validateUserLoginId(Onboard onboard) {
        if (UtilValidate.isNotEmpty(onboard.getAdmin().getEmail())) {
            try {
                GenericValue existingUserLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", onboard.getAdmin().getEmail()).queryOne();
                if (UtilValidate.isNotEmpty(existingUserLogin)) {
                    return true;
                }
            } catch (GenericEntityException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * Create an Org Admin User Login entry
     */
    private Map<String, Object> createOrgAdminUserLogin(String userLoginId, String password,
                                                        String firstName, String lastName) throws GenericServiceException, GenericEntityException {
        String partyId = null;
        Map<String, Object> createPersonResp;
        try {
            Map<String, Object> createPersonCtx = UtilMisc.toMap();
            createPersonCtx.put("userLogin", HierarchyUtils.getSysUserLogin(delegator));
            createPersonCtx.put("firstName", firstName);
            createPersonCtx.put("lastName", lastName);
            createPersonResp = dispatcher.runSync("createPerson", createPersonCtx);
            if (ServiceUtil.isSuccess(createPersonResp))
                partyId = (String) createPersonResp.get("partyId");
            else {
                Debug.logError("Error creating admin person " + createPersonResp.get("message"), module);
            }

            GenericValue adminUserLogin = delegator.makeValue("UserLogin");
            adminUserLogin.setString("userLoginId", userLoginId);
            adminUserLogin.setString("currentPassword", password);
            adminUserLogin.setString("requirePasswordChange", "N");
            if (UtilValidate.isNotEmpty(partyId)) adminUserLogin.setString("partyId", partyId);
            delegator.create(adminUserLogin);

            GenericValue adminUserLoginSecGroup = delegator.makeValue("UserLoginSecurityGroup");
            adminUserLoginSecGroup.setString("userLoginId", userLoginId);
            adminUserLoginSecGroup.setString("groupId", "ORG_ADMIN");
            adminUserLoginSecGroup.set("fromDate", UtilDateTime.nowTimestamp());
            delegator.create(adminUserLoginSecGroup);

            // Add Party Contact mechs (Email)
            Map<String, Object> createEmailAddressContext = new HashMap<String, Object>();
            createEmailAddressContext.put("userLogin", sysUserLogin);
            createEmailAddressContext.put("partyId", partyId);
            createEmailAddressContext.put("contactMechPurposeTypeId", EmailTypesEnum.PRIMARY.getTypeId());
            createEmailAddressContext.put("emailAddress", userLoginId);
            createEmailAddressContext.put("allowSolicitation", "N");
            createEmailAddressContext.put("isVerified", "N");
            createEmailAddressContext.put("createdByUserLogin", sysUserLogin.get("userLoginId"));
            createEmailAddressContext.put("lastModifiedByUserLogin", sysUserLogin.get("userLoginId"));
            createEmailAddressContext.put("createdDate", UtilDateTime.nowTimestamp());
            createEmailAddressContext.put("lastModifiedDate", UtilDateTime.nowTimestamp());
            Map<String, Object> createEmailAddressResp = dispatcher.runSync("createPartyEmailAddress", createEmailAddressContext);
            if (ServiceUtil.isError(createEmailAddressResp)) {
                Debug.logError("An error occurred while invoking createPartyEmailAddress service", module);
                //return ServiceUtil.returnError(AccountServices.AccountServicesErrorMessages.SYSTEM_ERROR_RESP);
                // TODO: throw exception? so that main method can fail
            }

        } catch (GenericEntityException | GenericServiceException e) {
            e.printStackTrace();
            throw e;
        }
        return createPersonResp;
    }

    /**
     * Setup Organization Party and Address & Contact details
     */
    private String setupOrganization(Onboard onboard, String adminPersonPartyId) throws GenericServiceException, GenericEntityException {
        String organizationName = onboard.getOrganizationName();
        String orgPartyId;

        try {
            if (UtilValidate.isEmpty(organizationName)) {
                Debug.logError("Organization name is missing", module);
                return null;
            }
            GenericValue partyGroup = EntityQuery.use(delegator).from("PartyGroup").where(UtilMisc.toMap("groupName", organizationName)).queryFirst();
            if (UtilValidate.isEmpty(partyGroup)) {
                orgPartyId = "ACCT_" + delegator.getNextSeqId("Party");
                Map createPartyGroupCtx = UtilMisc.toMap("userLogin", sysUserLogin, "partyId", orgPartyId,
                        "partyTypeId", "APP_ACCOUNT",
                        "groupName", organizationName);
                Map<String, Object> createPartyGroupResponse = dispatcher.runSync("createPartyGroup", createPartyGroupCtx);
                orgPartyId = (String) createPartyGroupResponse.get("partyId");
            } else {
                orgPartyId = partyGroup.getString("partyId");
                Debug.logInfo("Existing Party Group id is: " + orgPartyId, module);
            }
            Debug.logInfo("orgPartyId for Client in main DB is" + orgPartyId, module);

            // add party role for party group Id - APP_CLIENT
            GenericValue partyGroupRole = EntityQuery.use(delegator).from("PartyRole").where(UtilMisc.toMap("partyId", orgPartyId, "roleTypeId", "APP_CLIENT")).queryOne();
            if (UtilValidate.isEmpty(partyGroupRole)) {
                Map<String, Object> createPartyGroupRoleCtx = FastMap.newInstance();
                createPartyGroupRoleCtx.put("partyId", orgPartyId);
                createPartyGroupRoleCtx.put("roleTypeId", "APP_CLIENT");
                createPartyGroupRoleCtx.put("userLogin", HierarchyUtils.getSysUserLogin(delegator));
                dispatcher.runSync("createPartyRole", createPartyGroupRoleCtx);
            }

            // Relationship - person is OWNER of the APP_CLIENT with type as OWNER
            Map<String, Object> createAccountOwnerRelationshipCtx = new HashMap<String, Object>();
            createAccountOwnerRelationshipCtx.put("partyIdFrom", orgPartyId);
            createAccountOwnerRelationshipCtx.put("partyIdTo", adminPersonPartyId);
            createAccountOwnerRelationshipCtx.put("roleTypeIdFrom", AccountRoles.ORGANIZATION_ROLE.getPartyRelationshipTypeId());
            createAccountOwnerRelationshipCtx.put("roleTypeIdTo", AccountRoles.OWNER.getPartyRelationshipTypeId());
            createAccountOwnerRelationshipCtx.put("partyRelationshipTypeId", AccountRoles.MEMBER.getPartyRelationshipTypeId());
            createAccountOwnerRelationshipCtx.put("userLogin", sysUserLogin);
            Map<String, Object> createAccountOwnerRelationshipResult = dispatcher.runSync("createPartyRelationshipAndRole", createAccountOwnerRelationshipCtx);
            if (ServiceUtil.isError(createAccountOwnerRelationshipResult)) {
                Debug.logError(AccountServices.AccountServicesErrorMessages.ERROR_CREATE_RELATION, ServiceUtil.getErrorMessage(createAccountOwnerRelationshipResult), module);
            }
            // Relationship - person is EMPLOYEE of the ORG with type as EMPLOYMENT
            Map<String, Object> createEmployeeRelationshipCtx = new HashMap<String, Object>();
            createEmployeeRelationshipCtx.put("partyIdFrom", orgPartyId);
            createEmployeeRelationshipCtx.put("partyIdTo", adminPersonPartyId);
            createEmployeeRelationshipCtx.put("roleTypeIdFrom", AccountRoles.ORGANIZATION_ROLE.getPartyRelationshipTypeId());
            createEmployeeRelationshipCtx.put("roleTypeIdTo", AccountRoles.EMPLOYEE.getPartyRelationshipTypeId());
            createEmployeeRelationshipCtx.put("partyRelationshipTypeId", AccountRoles.EMPLOYMENT.getPartyRelationshipTypeId());
            createEmployeeRelationshipCtx.put("userLogin", sysUserLogin);
            Map<String, Object> createEmployeeRelationshipResult = dispatcher.runSync("createPartyRelationshipAndRole", createEmployeeRelationshipCtx);
            if (ServiceUtil.isError(createEmployeeRelationshipResult)) {
                Debug.logError(AccountServices.AccountServicesErrorMessages.ERROR_CREATE_RELATION, ServiceUtil.getErrorMessage(createEmployeeRelationshipResult), module);
            }
            // Relationship - Internal Org (FSD) as APP_OWNER and Org as APP_CLIENT with relationship type as SUBSCRIBER
            Map<String, Object> createFsdAndOrgSubscriberRelationshipCtx = new HashMap<String, Object>();
            createFsdAndOrgSubscriberRelationshipCtx.put("partyIdFrom", "FSD");
            createFsdAndOrgSubscriberRelationshipCtx.put("partyIdTo", orgPartyId);
            createFsdAndOrgSubscriberRelationshipCtx.put("roleTypeIdFrom", AccountRoles.INTERNAL_ORG.getPartyRelationshipTypeId());
            createFsdAndOrgSubscriberRelationshipCtx.put("roleTypeIdTo", AccountRoles.APP_CLIENT.name());
            createFsdAndOrgSubscriberRelationshipCtx.put("partyRelationshipTypeId", AccountRoles.APP_CLIENT.getPartyRelationshipTypeId());
            createFsdAndOrgSubscriberRelationshipCtx.put("userLogin", sysUserLogin);
            Map<String, Object> createOrgSubscriberRelationshipResult = dispatcher.runSync("createPartyRelationshipAndRole", createFsdAndOrgSubscriberRelationshipCtx);
            if (ServiceUtil.isError(createOrgSubscriberRelationshipResult)) {
                Debug.logError(AccountServices.AccountServicesErrorMessages.ERROR_CREATE_RELATION, ServiceUtil.getErrorMessage(createOrgSubscriberRelationshipResult), module);
            }

            // Add org address
            PostalAddress orgAddress = onboard.getOrganizationAddress();
            if (UtilValidate.isNotEmpty(orgAddress)) {
                Map<String, Object> addrMap = new HashMap<String, Object>();
                addrMap.put("partyId", orgPartyId);
                addrMap.put("toName", orgAddress.getToName());
                addrMap.put("address1", orgAddress.getAddressLine1());
                addrMap.put("address2", orgAddress.getAddressLine2());
                addrMap.put("city", orgAddress.getCity());
                addrMap.put("stateProvinceGeoId", orgAddress.getStateCode());
                addrMap.put("countryGeoId", orgAddress.getCountryCode());
                addrMap.put("postalCode", orgAddress.getPostalCode());
                addrMap.put("contactMechTypeId", "POSTAL_ADDRESS");
                addrMap.put("userLogin", sysUserLogin);

                Map<String, Object> createAddressResp = dispatcher.runSync("createPartyPostalAddress", addrMap);
                String contactMechId = (String) createAddressResp.get("contactMechId");
                if (UtilValidate.isNotEmpty(contactMechId)) {
                    Map<String, Object> input = UtilMisc.toMap("partyId", orgPartyId, "contactMechId", contactMechId, "userLogin", HierarchyUtils.getSysUserLogin(delegator));
                    input.put("contactMechPurposeTypeId", "PRIMARY_LOCATION");
                    dispatcher.runSync("createPartyContactMechPurpose", input);
                }
            }

            // create telecom number
            if (UtilValidate.isNotEmpty(onboard.getOrgPhone()) && UtilValidate.isNotEmpty(orgPartyId)) {
                Map<String, Object> createPartyTelecomCtx = new HashMap<String, Object>();
                createPartyTelecomCtx.put("countryCode", onboard.getOrgPhone().getAreaCode());
                createPartyTelecomCtx.put("areaCode", onboard.getOrgPhone().getAreaCode());
                createPartyTelecomCtx.put("contactNumber", onboard.getOrgPhone().getPhone());
                createPartyTelecomCtx.put("extension", onboard.getOrgPhone().getExtension());
                createPartyTelecomCtx.put("partyId", orgPartyId);
                createPartyTelecomCtx.put("userLogin", HierarchyUtils.getSysUserLogin(delegator));
                createPartyTelecomCtx.put("contactMechPurposeTypeId", "PRIMARY_PHONE");
                dispatcher.runSync("createPartyTelecomNumber", createPartyTelecomCtx);
            }

            // create email address contact mech
            Map<String, Object> createEmailAddressContext = new HashMap<String, Object>();
            createEmailAddressContext.put("userLogin", sysUserLogin);
            createEmailAddressContext.put("partyId", orgPartyId);
            createEmailAddressContext.put("contactMechPurposeTypeId", EmailTypesEnum.PRIMARY.getTypeId());
            createEmailAddressContext.put("emailAddress", onboard.getOrgEmail());
            createEmailAddressContext.put("allowSolicitation", onboard.isAcceptsMarketing() ? "Y" : "N");
            createEmailAddressContext.put("isVerified", "N");
            createEmailAddressContext.put("createdByUserLogin", sysUserLogin.get("userLoginId"));
            createEmailAddressContext.put("lastModifiedByUserLogin", sysUserLogin.get("userLoginId"));
            createEmailAddressContext.put("createdDate", UtilDateTime.nowTimestamp());
            createEmailAddressContext.put("lastModifiedDate", UtilDateTime.nowTimestamp());
            Map<String, Object> createEmailAddressResp = dispatcher.runSync("createPartyEmailAddress", createEmailAddressContext);
            if (ServiceUtil.isError(createEmailAddressResp)) {
                Debug.logError("An error occurred while invoking createPartyEmailAddress service", module);
                //return ServiceUtil.returnError(AccountServices.AccountServicesErrorMessages.SYSTEM_ERROR_RESP);
                // TODO: throw exception? so that main method can fail
            }

            // Add org attributes (PartyAttribute) organizationCustomer, businessLocation, shouldLoadGovtData
            PartyAttributeHelper partyAttributeHelper = PartyAttributeHelper.builder().dispatcher(dispatcher).userLogin(sysUserLogin).build();
            partyAttributeHelper.createOrUpdateAttributes(orgPartyId, UtilMisc.toMap("BusinessLocation", onboard.getBusinessLocation(),
                    "OrganizationCustomer", onboard.getOrganizationCustomer(),
                    "shouldLoadGovtData", onboard.isShouldLoadGovtData() ? "Y" : "N"));

            // Add Bank wiring details for the organization
            if (UtilValidate.isNotEmpty(onboard.getBankName()) && UtilValidate.isNotEmpty(onboard.getBankName())) {
                Map<String, Object> createBankWiringCtx = new HashMap<String, Object>();
                createBankWiringCtx.put("finBankName", onboard.getBankName());
                createBankWiringCtx.put("finSortCode", onboard.getRoutingNumber());
                createBankWiringCtx.put("finAccountCode", onboard.getAccountNumber());
                createBankWiringCtx.put("organizationPartyId", orgPartyId);
                createBankWiringCtx.put("ownerPartyId", orgPartyId);
                createBankWiringCtx.put("finAccountTypeId", "BANK_ACCOUNT");
                createBankWiringCtx.put("userLogin", sysUserLogin);
                dispatcher.runSync("fsdCreateFinanceAccount", createBankWiringCtx);
            }

            // Index parties (both Org and Admin) to solr
            dispatcher.runSync("indexPartyInSolr", UtilMisc.toMap("userLogin", sysUserLogin, "partyId", orgPartyId));
            dispatcher.runSync("indexPartyInSolr", UtilMisc.toMap("userLogin", sysUserLogin, "partyId", adminPersonPartyId));
        } catch (GenericServiceException | GenericEntityException e) {
            Debug.logError(" An error occurred while setting up OrganizationParty details in Main DB: " + e.getMessage(), module);
            throw e;
        }
        return orgPartyId;
    }

    public Response inviteAdminEmployee(List<Invitation> employeeInvites) {
        Map<String, Object> responseMap = new HashMap<>();
        Delegator localDelegator = DelegatorFactory.getDelegator("default");
        LocalDispatcher localDispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", localDelegator);

        Map<String, Object> fsdFetchInternalOrganizationResp;
        String organizationName = "";
        try {
            fsdFetchInternalOrganizationResp = localDispatcher.runSync("fsdFetchInternalOrganization", UtilMisc.toMap("userLogin", HierarchyUtils.getSysUserLogin(localDelegator)));
        } catch (GenericServiceException e) {
            Debug.logError(e, " An error occurred while trying to fetch the Organization Details", module);
            if (Debug.verboseOn())
                Debug.logVerbose("Exiting method sendInvitationEmail", module);

            return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR);
        }

        if (ServiceUtil.isSuccess(fsdFetchInternalOrganizationResp)) {
            organizationName = (String) fsdFetchInternalOrganizationResp.get("organizationName");
        }
        Debug.logInfo("Inviting Employees for org " + organizationName, module);


        for (Invitation employeeInvite : employeeInvites) {
            Map<String, Object> inviteEmpContext = FastMap.newInstance();
            inviteEmpContext.put("userLogin", HierarchyUtils.getSysUserLogin(localDelegator));

            if (UtilValidate.isNotEmpty(employeeInvite.getEmail())) {
                inviteEmpContext.put("emailAddress", employeeInvite.getEmail());
            }

            if (UtilValidate.isNotEmpty(employeeInvite.getFullName())) {
                inviteEmpContext.put("firstName", employeeInvite.getFullName());
            }

            if (UtilValidate.isNotEmpty(employeeInvite.getRoleIds())) {
                inviteEmpContext.put("roleIds", employeeInvite.getRoleIds());
            }

            inviteEmpContext.put("organizationName", organizationName);

            try {
                Map fsdInviteColleagueContextResp = dispatcher.runSync("fsdSendEmailLink", inviteEmpContext);

                if (ServiceUtil.isError(fsdInviteColleagueContextResp)) {
                    Debug.logError(" An error occurred while invoking fsdSendEmailLink service, details: " + ServiceUtil.getErrorMessage(fsdInviteColleagueContextResp), module);
                    if (Debug.verboseOn())
                        Debug.logVerbose("Exiting method fsdSendEmailLink", module);

                    return ApiResponseUtil.prepareDefaultResponse(Response.Status.BAD_REQUEST, ServiceUtil.getErrorMessage(fsdInviteColleagueContextResp));
                }
            } catch (GenericServiceException e) {
                Debug.logError("An error occurred while invoking fsdSendEmailLink service, details: " + e.getMessage(), module);
                if (Debug.verboseOn())
                    Debug.logVerbose("Exiting method fsdSendEmailLink", module);

                return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR);
            }
        }

        return ApiResponseUtil.prepareOkResponse(responseMap);
    }

}


