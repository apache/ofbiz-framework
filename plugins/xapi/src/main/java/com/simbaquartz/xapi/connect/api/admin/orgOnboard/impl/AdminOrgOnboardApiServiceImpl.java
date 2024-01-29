package com.simbaquartz.xapi.connect.api.admin.orgOnboard.impl;

import com.simbaquartz.xapi.connect.api.NotFoundException;
import com.simbaquartz.xapi.connect.api.admin.orgOnboard.AdminOrgOnboardApiService;
import com.simbaquartz.xapi.connect.api.common.ApiMessageConstants;
import com.simbaquartz.xapi.connect.utils.ApiResponseUtil;
import com.fidelissd.zcp.xcommon.models.client.Invitation;
import com.fidelissd.zcp.xcommon.models.client.Onboard;
import com.simbaquartz.xapi.connect.validation.EmailValidator;
import com.fidelissd.zcp.xcommon.util.hierarchy.HierarchyUtils;
import com.fidelissd.zcp.xcommon.collections.FastMap;
import org.apache.ofbiz.base.crypto.HashCrypt;
import org.apache.ofbiz.base.util.*;
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

    enum InvitationStatusIds {
        SENT("PARTYINV_SENT"),
        PENDING("PARTYINV_PENDING"),
        ACCEPTED("PARTYINV_ACCEPTED"),
        DECLINED("PARTYINV_DECLINED"),
        CANCELLED("PARTYINV_CANCELLED");

        private String statusId;

        InvitationStatusIds(String statusId) {
            this.statusId = statusId;
        }

        public String getStatusId() {
            return statusId;
        }
    }

    @Override
    public Response onboardOrgAdmin(Onboard onboard) throws NotFoundException {
        Map<String, Object> responseMap = new HashMap<>();
        Map<String, Object> registerUserContext = FastMap.newInstance();

        Delegator localDelegator = DelegatorFactory.getDelegator("default");
        registerUserContext.put("userLogin", HierarchyUtils.getSysUserLogin(localDelegator));

        if (validateUserLoginId(onboard))
            return ApiResponseUtil.prepareDefaultResponse(Response.Status.FORBIDDEN, " UserLoginId Already Exists.");

        try {
            GenericValue partyGroupData = EntityQuery.use(delegator).from("PartyRole").where("roleTypeId", "APP_CLIENT").queryOne();
            if(UtilValidate.isNotEmpty(partyGroupData))
            {
                return ApiResponseUtil.prepareDefaultResponse(Response.Status.BAD_REQUEST, ApiMessageConstants.MSG_ORG_ALREADY_EXIST);
            }
        }catch( GenericEntityException e) {
            Debug.logError(" An error occurred while onboarding admin and organization details in Main DB: " + e.getMessage(), module);
            return ApiResponseUtil.prepareDefaultResponse(Response.Status.BAD_REQUEST,  e.getMessage());
        }

        if (UtilValidate.isNotEmpty(onboard.getAdminEmail()) && !EmailValidator.isValidEmail(onboard.getAdminEmail())) {
            return ApiResponseUtil.prepareDefaultResponse(Response.Status.BAD_REQUEST, ApiMessageConstants.MSG_INVALID_EMAIL_FORMAT + onboard.getAdminEmail());
        }

        String sha1AdminPassword = HashCrypt.cryptUTF8(LoginServices.getHashType(), null, onboard.getAdminPassword());
        String partyId = null;
        Map<String, Object> createOrgAdminResp;
        try {
            createOrgAdminResp = createOrgAdminUserLogin(onboard.getAdminEmail(), sha1AdminPassword, onboard.getAdminFirstName(), onboard.getAdminLastName());
        } catch (GenericServiceException | GenericEntityException e) {
            //handle error here
            Debug.logError("An error occurred while invoking registerUser service, details: " + e.getMessage(), module);

            return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR);
        }

        //org onboard
        Map<String, Object> createOrgResp;
        String orgPartyId;
        try {
            orgPartyId = setupAdminOrgOnboard(onboard);
        } catch (GenericServiceException | GenericEntityException e) {
            //handle error here
            Debug.logError("An error occurred while invoking org on-boarding service, details: " + e.getMessage(), module);

            return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR);
        }

        //Adding admin (first user) to the org as employee
        Invitation employeeInvite = new Invitation();
        employeeInvite.setEmail(onboard.getAdminEmail());
        employeeInvite.setFullName(onboard.getAdminFirstName());
        employeeInvite.setRoleIds(UtilMisc.toList("ORG_MANAGER"));
        inviteAdminEmployee(UtilMisc.toList(employeeInvite));

        responseMap.put("partyId", createOrgAdminResp.get("partyId"));
        responseMap.put("orgPartyId", orgPartyId);
        return ApiResponseUtil.prepareOkResponse(responseMap);
    }

    private boolean validateUserLoginId(Onboard onboard) {
        if (UtilValidate.isNotEmpty(onboard.getAdminEmail())) {
            try {
                GenericValue existingUserLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", onboard.getAdminEmail()).queryOne();
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
     * Create an Org Admin User Login entry in Main DB
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
                Debug.logError(" Error creating admin person " + createPersonResp.get("message"), module);
            }

            GenericValue adminUserLogin = delegator.makeValue("UserLogin");
            adminUserLogin.setString("userLoginId", userLoginId);
            adminUserLogin.setString("currentPassword", password);
            adminUserLogin.setString("requirePasswordChange", "N");
            if (UtilValidate.isNotEmpty(partyId)) adminUserLogin.setString("partyId", partyId);
            delegator.create(adminUserLogin);

            GenericValue adminUserLoginSecGroup = delegator.makeValue("UserLoginSecurityGroup");
            adminUserLoginSecGroup.setString("userLoginId", userLoginId);
            adminUserLoginSecGroup.setString("groupId", "SUPER");
            adminUserLoginSecGroup.set("fromDate", UtilDateTime.nowTimestamp());
            delegator.create(adminUserLoginSecGroup);
        } catch (GenericEntityException | GenericServiceException e) {
            e.printStackTrace();
            throw e;
        }
        return createPersonResp;
    }

    /**
     * Setup Org Party and Address & Contact details in Main DB
     */
    private String setupAdminOrgOnboard(Onboard onboard) throws GenericServiceException, GenericEntityException {
        String orgPartyId = null;
        try {
            //create party group
            if (UtilValidate.isNotEmpty(onboard.getOrganizationName())) {
                GenericValue partyGroup = EntityQuery.use(delegator).from("PartyGroup").where(UtilMisc.toMap("groupName", onboard.getOrganizationName())).queryFirst();
                if (UtilValidate.isEmpty(partyGroup)) {
                    Map createPartyGroupCtx = UtilMisc.toMap(
                            "userLogin", HierarchyUtils.getSysUserLogin(delegator),
                            "groupName", onboard.getOrganizationName());
                    Map<String, Object> createPartyGroupResponse = dispatcher.runSync("createPartyGroup", createPartyGroupCtx);
                    Debug.logInfo(" createPartyGroupResponse is: " + createPartyGroupResponse, module);
                    orgPartyId = (String) createPartyGroupResponse.get("partyId");
                } else {
                    Debug.logInfo("Existing Party Group is: " + partyGroup, module);
                    orgPartyId = partyGroup.getString("partyId");
                    Map<String, Object> updatePartyGroup = UtilMisc.toMap(
                            "partyId", orgPartyId,
                            "userLogin", HierarchyUtils.getSysUserLogin(delegator),
                            "groupName", onboard.getOrganizationName());
                    Map<String, Object> updatePartyGroupResponse = dispatcher.runSync("updatePartyGroup", updatePartyGroup);
                }

                //create party role for party group Id - APP_CLIENT
                GenericValue partyGroupRole = EntityQuery.use(delegator).from("PartyRole").where(UtilMisc.toMap("partyId", orgPartyId, "roleTypeId", "APP_CLIENT")).queryOne();
                if (UtilValidate.isEmpty(partyGroupRole)) {
                    Map<String, Object> createPartyGroupRoleCtx = FastMap.newInstance();
                    createPartyGroupRoleCtx.put("partyId", orgPartyId);
                    createPartyGroupRoleCtx.put("roleTypeId", "APP_CLIENT");
                    createPartyGroupRoleCtx.put("userLogin", HierarchyUtils.getSysUserLogin(delegator));
                    Map createPartyGroupRoleResponse = dispatcher.runSync("createPartyRole", createPartyGroupRoleCtx);
                }
            }

            Debug.logInfo("orgPartyId for Client in main DB is" + orgPartyId, module);

            //create telecom number
            if (UtilValidate.isNotEmpty(onboard.getPhoneNumber()) && UtilValidate.isNotEmpty(orgPartyId)) {
                Map<String, Object> summaryResult = new HashMap<String, Object>();
                Map<String, Object> createPartyTelecomCtx = new HashMap<String, Object>();
                createPartyTelecomCtx.put("countryCode", onboard.getPhoneNumber().getAreaCode());
                createPartyTelecomCtx.put("areaCode", onboard.getPhoneNumber().getAreaCode());
                createPartyTelecomCtx.put("contactNumber", onboard.getPhoneNumber().getPhone());
                createPartyTelecomCtx.put("extension", onboard.getPhoneNumber().getExtension());
                createPartyTelecomCtx.put("partyId", orgPartyId);
                createPartyTelecomCtx.put("userLogin", HierarchyUtils.getSysUserLogin(delegator));
                createPartyTelecomCtx.put("contactMechPurposeTypeId", "PRIMARY_PHONE");
                summaryResult = dispatcher.runSync("createPartyTelecomNumber", createPartyTelecomCtx);

            }

        } catch (GenericServiceException | GenericEntityException e) {
            Debug.logError(" An error occurred while setting up OrganizationParty details in Main DB: " + e.getMessage(), module);
            throw e;
        }
        return orgPartyId;
    }

    public Response inviteAdminEmployee(List<Invitation> employeeInvites) throws NotFoundException {
        Map<String, Object> responseMap = new HashMap<>();
        Delegator localDelegator = DelegatorFactory.getDelegator("default");
        ;
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
                //handle error here
                Debug.logError("An error occurred while invoking fsdSendEmailLink service, details: " + e.getMessage(), module);
                if (Debug.verboseOn())
                    Debug.logVerbose("Exiting method fsdSendEmailLink", module);

                return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR);
            }
        }

        return ApiResponseUtil.prepareOkResponse(responseMap);
    }


}


