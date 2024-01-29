package com.simbaquartz.xparty.services.account;

import com.fidelissd.zcp.xcommon.collections.FastList;
import com.fidelissd.zcp.xcommon.collections.FastMap;
import com.fidelissd.zcp.xcommon.enums.AccountUserRoleTypesEnum;
import com.fidelissd.zcp.xcommon.services.contact.EmailTypesEnum;
import com.fidelissd.zcp.xcommon.util.hierarchy.HierarchyUtils;
import com.simbaquartz.xgeo.utils.GeoUtil;
import com.simbaquartz.xparty.hierarchy.PartyGroupForPartyUtils;
import com.simbaquartz.xparty.hierarchy.role.AccountRoles;
import com.simbaquartz.xparty.services.invitation.PartyInvitationStatusEnum;
import com.simbaquartz.xparty.utils.PartyTypesEnum;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.ofbiz.base.util.*;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.party.party.PartyWorker;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccountServices {
    public static final String module = AccountServices.class.getName();

    public static class AccountServicesErrorMessages {
        public static final String SYSTEM_ERROR =
                "Something went wrong, please check log for more details.";
        public static final String SYSTEM_ERROR_RESP = "Something went wrong, please try later.";
        public static final String USER_ALREADY_EXIST = "User with same email is already exists";
        public static final String ERROR_OCCURED = "Error occured:";
        public static final String ACCOUNT_ID_ERROR =
                "Error occured while getting next sequencial account id:";
        public static final String PERSON_CREATE = "Error creating person ";
        public static final String ERROR_CREATE_RELATION =
                "Error while creating relationship in party and company account";
        public static final String ERROR_ADD_SECURITY_GROUP =
                "Error while adding user to security group";
        public static final String ERROR_VALIDATING_USER = "Error while validating user account";
        public static final String ERROR_CREATING_PRODUCT_STORE =
                "Error while creating product " + "store for account : ";
        public static final String ERROR_CREATING_USER = "Error while creating person and user login";
    }

    /**
     * Checks if MMO Account is already setup
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> isAccountAlreadySetup(
            DispatchContext dctx, Map<String, Object> context) {

        if (Debug.verboseOn()) Debug.logVerbose("Entering method isAccountAlreadySetup", module);
        Delegator delegator = dctx.getDelegator();
        GenericValue existingAdministration = null;
        GenericValue existingAppAccount = null;

        try {
            existingAdministration =
                    EntityQuery.use(delegator)
                            .from("RoleManager")
                            .where("roleTypeId", "ADMINISTRATION", "roleType", "PROFILE")
                            .queryOne();
            existingAppAccount =
                    EntityQuery.use(delegator).from("Party").where("partyTypeId", PartyTypesEnum.APP_ACCOUNT.getPartyTypeId()).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }

        return UtilMisc.toMap(
                "isAccountSetup",
                UtilValidate.isNotEmpty(existingAppAccount)
                        || UtilValidate.isNotEmpty(existingAdministration));
    }

    /**
     * Creates an application account
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> createApplicationAccount(
            DispatchContext dctx, Map<String, Object> context) {

        if (Debug.verboseOn()) Debug.logVerbose("Entering method createApplicationAccount", module);

        Map<String, Object> result = ServiceUtil.returnSuccess();

        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        try {
            // input details from the user
            String fullName = (String) context.get("fullName");
            String email = ((String) context.get("email")).toLowerCase();
            String companyName = (String) context.get("companyName");
            String password = (String) context.get("password");
            Integer numberOfEmployees = (Integer) context.get("numberOfEmployees");
            Boolean acceptsMarketing = (Boolean) context.get("acceptsMarketing");
            Boolean skipStoreSetup = (Boolean) context.get("skipStoreSetup");

            GenericValue sysUserLogin =
                    HierarchyUtils.getSysUserLogin(delegator);

            // validating user account with input email id
            Map<String, Object> checkEmailResponse =
                    dispatcher.runSync(
                            "validateAccountWithEmail",
                            UtilMisc.toMap("userLogin", sysUserLogin, "email", email));
            if (UtilValidate.isNotEmpty(checkEmailResponse.get("alreadyExist"))
                    && ((Boolean) checkEmailResponse.get("alreadyExist"))) {
                Debug.logError(AccountServicesErrorMessages.USER_ALREADY_EXIST, module);
                return ServiceUtil.returnError(AccountServicesErrorMessages.USER_ALREADY_EXIST);
            }

            // check if company name not provided then using the format e.g. John Doe's Company
            if (UtilValidate.isEmpty(companyName)) {
                companyName = fullName + "'s Company";
            }

            // creating a person using full name as first name & person user login
            Map<String, Object> createPersonLoginCtx =
                    UtilMisc.toMap(
                            "userLoginId",
                            email,
                            "userLogin",
                            sysUserLogin,
                            "currentPassword",
                            password,
                            "currentPasswordVerify",
                            password,
                            "requirePasswordChange",
                            "N",
                            "firstName",
                            fullName,
                            "displayName",
                            fullName);

            Map<String, Object> createPersonResp =
                    dispatcher.runSync("createPersonAndUserLogin", createPersonLoginCtx);

            String accountOwnerPartyId = null;
            if (ServiceUtil.isError(createPersonResp)) {
                String errorMessage = ServiceUtil.getErrorMessage(createPersonResp);
                Debug.logError(errorMessage, module);
                return ServiceUtil.returnError(AccountServicesErrorMessages.ERROR_CREATING_USER);
            }

            accountOwnerPartyId = (String) createPersonResp.get("partyId");

            // give admin access to the account owner.
            dispatcher.runSync(
                    "changeAppUserPermissions",
                    UtilMisc.toMap(
                            "userLogin", sysUserLogin,
                            "accountId", email,
                            "groupId", AccountUserRoleTypesEnum.ADMIN.getRole()));

            // creating party attribute domain after extracting from email id
            // todo: add check on the domain if it's general one
            String domain = email.substring(email.indexOf("@") + 1);
            Map<String, Object> createPartyAttrCtx =
                    UtilMisc.toMap(
                            "userLoginId",
                            email,
                            "userLogin",
                            sysUserLogin,
                            "partyId",
                            accountOwnerPartyId,
                            "attrName",
                            "DOMAIN_NAME",
                            "attrValue",
                            domain);

            Map<String, Object> createPartyAttrResp =
                    dispatcher.runSync("createPartyAttribute", createPartyAttrCtx);
            if (ServiceUtil.isError(createPartyAttrResp)) {
                Debug.logError(ServiceUtil.getErrorMessage(createPartyAttrResp), module);
                return ServiceUtil.returnError(AccountServicesErrorMessages.ERROR_CREATING_USER);
            }
            // get the organization party id for company account of user
            String accountId;
            try {
                accountId = "ACCT_" + delegator.getNextSeqId("Party");
            } catch (IllegalArgumentException e) {
                Debug.logError(e, AccountServicesErrorMessages.ACCOUNT_ID_ERROR, module);
                return ServiceUtil.returnError(
                        AccountServicesErrorMessages.ACCOUNT_ID_ERROR + e.getMessage());
            }

            // create party group
            Map<String, Object> createPartyGroupCtx =
                    UtilMisc.toMap(
                            "groupName",
                            companyName,
                            "partyId",
                            accountId,
                            "userLogin",
                            sysUserLogin,
                            "partyTypeId",
                            "APP_ACCOUNT");

            if (UtilValidate.isNotEmpty(numberOfEmployees)) {
                createPartyGroupCtx.put("numEmployees", Long.valueOf(numberOfEmployees));
            }

            Map<String, Object> createPartyGroupCtxResponse =
                    dispatcher.runSync("createPartyGroup", createPartyGroupCtx);
            String accountCompanyPartyId = (String) createPartyGroupCtxResponse.get("partyId");

            if (ServiceUtil.isError(createPartyGroupCtxResponse)) {
                Debug.logError(ServiceUtil.getErrorMessage(createPartyGroupCtxResponse), module);
                return ServiceUtil.returnError(AccountServicesErrorMessages.SYSTEM_ERROR_RESP);
            }

            // create the relationship of OWNER between person and account party group
            Map<String, Object> createAccountOwnerRelationshipCtx = new HashMap<String, Object>();
            createAccountOwnerRelationshipCtx.put("partyIdFrom", accountCompanyPartyId);
            createAccountOwnerRelationshipCtx.put("partyIdTo", accountOwnerPartyId);
            createAccountOwnerRelationshipCtx.put(
                    "roleTypeIdFrom", AccountRoles.INTERNAL_ORG.getPartyRelationshipTypeId());
            createAccountOwnerRelationshipCtx.put(
                    "roleTypeIdTo", AccountRoles.OWNER.getPartyRelationshipTypeId());
            createAccountOwnerRelationshipCtx.put(
                    "partyRelationshipTypeId", AccountRoles.OWNER.getPartyRelationshipTypeId());
            createAccountOwnerRelationshipCtx.put("userLogin", sysUserLogin);

            // Created relationship and role of person and company i.e. person is OWNER of the
            // INTERNAL_ORGANIZATIO with type as OWNER
            Map<String, Object> createAccountOwnerRelationshipResult = null;
            createAccountOwnerRelationshipResult =
                    dispatcher.runSync("createPartyRelationshipAndRole", createAccountOwnerRelationshipCtx);
            if (ServiceUtil.isError(createAccountOwnerRelationshipResult)) {
                Debug.logError(
                        AccountServicesErrorMessages.ERROR_CREATE_RELATION,
                        ServiceUtil.getErrorMessage(createAccountOwnerRelationshipResult),
                        module);
                return ServiceUtil.returnError(AccountServicesErrorMessages.SYSTEM_ERROR_RESP);
            }

            // create the relationship of EMPLOYMENT between person and account party group
            Map<String, Object> createAccountEmployeeRelationshipCtx = new HashMap<String, Object>();
            createAccountEmployeeRelationshipCtx.put("partyIdFrom", accountCompanyPartyId);
            createAccountEmployeeRelationshipCtx.put("partyIdTo", accountOwnerPartyId);
            createAccountEmployeeRelationshipCtx.put(
                    "roleTypeIdFrom", AccountRoles.INTERNAL_ORG.getPartyRelationshipTypeId());
            createAccountEmployeeRelationshipCtx.put(
                    "roleTypeIdTo", AccountRoles.EMPLOYMENT.getPartyRelationshipTypeId());
            createAccountEmployeeRelationshipCtx.put(
                    "partyRelationshipTypeId", AccountRoles.EMPLOYMENT.getPartyRelationshipTypeId());
            createAccountEmployeeRelationshipCtx.put("userLogin", sysUserLogin);

            // Created relationship and role of person and company i.e. person is OWNER of the
            // INTERNAL_ORGANIZATIO with type as OWNER
            Map<String, Object> createAccountEmployeeRelationshipResult = null;
            createAccountEmployeeRelationshipResult =
                    dispatcher.runSync(
                            "createPartyRelationshipAndRole", createAccountEmployeeRelationshipCtx);
            if (ServiceUtil.isError(createAccountEmployeeRelationshipResult)) {
                Debug.logError(
                        AccountServicesErrorMessages.ERROR_CREATE_RELATION,
                        ServiceUtil.getErrorMessage(createAccountEmployeeRelationshipResult),
                        module);
                return ServiceUtil.returnError(AccountServicesErrorMessages.SYSTEM_ERROR_RESP);
            }

            Map<String, Object> addSecurityGroupResult = null;
            addSecurityGroupResult =
                    dispatcher.runSync(
                            "addUserLoginToSecurityGroup",
                            UtilMisc.toMap("userLoginId", email, "userLogin", sysUserLogin, "groupId", "SUPER"));
            if (ServiceUtil.isError(addSecurityGroupResult)) {
                Debug.logError(
                        AccountServicesErrorMessages.ERROR_ADD_SECURITY_GROUP,
                        ServiceUtil.getErrorMessage(addSecurityGroupResult),
                        module);
                return ServiceUtil.returnError(AccountServicesErrorMessages.SYSTEM_ERROR_RESP);
            }

            // Checking the email contact mech exists with the email
            GenericValue emailContactMech =
                    EntityQuery.use(delegator).from("ContactMech").where("infoString", email).queryFirst();

            // if email contact mech does no exists simply calling createPartyEmailAddress and
            // creating new one
            if (UtilValidate.isEmpty(emailContactMech)) {
                Map<String, Object> createEmailAddressContext = new HashMap<String, Object>();
                createEmailAddressContext.put("userLogin", sysUserLogin);
                createEmailAddressContext.put("partyId", accountOwnerPartyId);
                createEmailAddressContext.put(
                        "contactMechPurposeTypeId", EmailTypesEnum.PRIMARY.getTypeId());
                createEmailAddressContext.put("emailAddress", email);
                if (acceptsMarketing) {
                    createEmailAddressContext.put("allowSolicitation", "Y");
                } else {
                    createEmailAddressContext.put("allowSolicitation", "N");
                }
                createEmailAddressContext.put("isVerified", "N");
                createEmailAddressContext.put("createdByUserLogin", sysUserLogin.get("partyId"));
                createEmailAddressContext.put("lastModifiedByUserLogin", sysUserLogin.get("partyId"));
                createEmailAddressContext.put("createdDate", UtilDateTime.nowTimestamp());
                createEmailAddressContext.put("lastModifiedDate", UtilDateTime.nowTimestamp());

                Map<String, Object> createEmailAddressResp = null;
                createEmailAddressResp =
                        dispatcher.runSync("createPartyEmailAddress", createEmailAddressContext);
                if (ServiceUtil.isError(createEmailAddressResp)) {
                    Debug.logError(
                            "An error occurred while invoking createPartyEmailAddress service", module);
                    return ServiceUtil.returnError(AccountServicesErrorMessages.SYSTEM_ERROR_RESP);
                }

            }
            // if email contact mech exists, adding entries and relations to the PartyContactMech
            // and PartyContactMechPurpose to utilize the same contact mech
            else {
                // creating party contact mech
                GenericValue partyContactMech = delegator.makeValue("PartyContactMech");
                partyContactMech.set("partyId", accountOwnerPartyId);
                partyContactMech.set("contactMechId", emailContactMech.get("contactMechId"));
                partyContactMech.set("fromDate", UtilDateTime.nowTimestamp());
                delegator.createOrStore(partyContactMech);

                // creating party contact mech purpose
                GenericValue partyContactMechPurpose = delegator.makeValue("PartyContactMechPurpose");
                partyContactMechPurpose.set("partyId", accountOwnerPartyId);
                partyContactMechPurpose.set("contactMechId", emailContactMech.get("contactMechId"));
                partyContactMechPurpose.set("contactMechPurposeTypeId", EmailTypesEnum.PRIMARY.getTypeId());
                partyContactMechPurpose.set("fromDate", UtilDateTime.nowTimestamp());
                delegator.createOrStore(partyContactMechPurpose);
            }

            GenericValue roleTypeManager = delegator.makeValue("RoleManager");
            roleTypeManager.set("roleTypeId", "ADMIN");
            roleTypeManager.set("groupPartyId", accountCompanyPartyId);
            roleTypeManager.set("roleType", "ROLE");
            roleTypeManager.create();

            GenericValue profileRoleTypeManager = delegator.makeValue("RoleManager");
            profileRoleTypeManager.set("roleTypeId", "ADMINISTRATION");
            profileRoleTypeManager.set("groupPartyId", accountCompanyPartyId);
            profileRoleTypeManager.set("roleType", "PROFILE");
            profileRoleTypeManager.create();

            dispatcher.runAsync(
                    "indexPartyAccountInSolr",
                    UtilMisc.<String, Object>toMap(
                            "partyId",
                            accountOwnerPartyId,
                            "userLogin",
                            sysUserLogin,
                            "isDataPassed",
                            true,
                            "partyAccountData",
                            UtilMisc.toMap(
                                    "userLogin",
                                    sysUserLogin,
                                    "partyId",
                                    accountOwnerPartyId,
                                    "fullName",
                                    fullName,
                                    "accountGroupPartyId",
                                    accountCompanyPartyId,
                                    "companyName",
                                    companyName,
                                    "email",
                                    email)));

            // Sending verification email
            Map<String, Object> verifyEmailContext = new HashMap<String, Object>();
            verifyEmailContext.put("userLogin", sysUserLogin);
            verifyEmailContext.put("emailAddress", email);
            verifyEmailContext.put("fullName", fullName);

            dispatcher.runAsync("sendEmailVerificationLink", verifyEmailContext);

            // fix the display name for the accountOwner and accountCompany
            dispatcher.runSync(
                    "populateBasicInformationForParty",
                    UtilMisc.toMap("partyId", accountOwnerPartyId, "userLogin", sysUserLogin));
            dispatcher.runSync(
                    "populateBasicInformationForParty",
                    UtilMisc.toMap("partyId", accountCompanyPartyId, "userLogin", sysUserLogin));
            // index party in solr
            Map<String, Object> indexPartyContext = new HashMap<String, Object>();
            indexPartyContext.put("userLogin", sysUserLogin);
            indexPartyContext.put("partyId", accountOwnerPartyId);
            Map<String, Object> indexPartyContextResp = null;
            indexPartyContextResp = dispatcher.runSync("indexPartyInSolr", indexPartyContext);
            if (ServiceUtil.isError(indexPartyContextResp)) {
                Debug.logError("An error occurred while invoking indexPartyInSolr service", module);
                return ServiceUtil.returnError(AccountServicesErrorMessages.SYSTEM_ERROR_RESP);
            }

            if (!skipStoreSetup) {
                // create default store
                dispatcher.runSync(
                        "axCreateProductStoreForAccount",
                        UtilMisc.toMap(
                                "userLogin",
                                HierarchyUtils.getSysUserLogin(delegator),
                                "accountId",
                                accountCompanyPartyId));
            }

            // Add Default Task Template (SYS_MTG_READY_CHK) to account with purpose as "TSK_TPL_MORTGAGE"
            // addAccountDefaultTaskTemplate(dispatcher, sysUserLogin, accountCompanyPartyId);

            result.put("accountId", accountCompanyPartyId);
        } catch (GenericServiceException | GenericEntityException e) {
            Debug.logError(
                    e,
                    "An error occurred while setting up OrganizationParty details in Main DB: "
                            + e.getMessage(),
                    module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return result;
    }

    /**
     * Add Default Task Template (SYS_MTG_READY_CHK) to account with purpose as "TSK_TPL_MORTGAGE"
     *
     * @param accountCompanyPartyId account id
     */
    public static void addAccountDefaultTaskTemplate(LocalDispatcher dispatcher, GenericValue userLogin,
                                                      String accountCompanyPartyId)
            throws GenericServiceException, GenericEntityException {
        // 1. Clone task template - SYS_MTG_READY_CHK
        Map<String, Object> cloneTaskCtx = UtilMisc.toMap("userLogin", userLogin,
                "taskId", "SYS_MTG_READY_CHK",
                "taskTypeId", "TASK_TYPE_TEMPLATE");
        Map<String, Object> cloneTaskResp = dispatcher.runSync("cloneTask", cloneTaskCtx);
        if (!ServiceUtil.isSuccess(cloneTaskResp)) {
            Debug.logError("There was a problem cloning task template", module);
            return;
        }
        // 2. associate cloned task template to account with purpose as "TSK_TPL_MORTGAGE"
        String clonedTaskId = (String) cloneTaskResp.get("clonedTaskId");
        Delegator delegator = dispatcher.getDelegator();
        GenericValue accountTemplateAssocGv = delegator.makeValue("AccountTaskTemplateAssoc",
                UtilMisc.toMap("taskId", clonedTaskId, "partyId", accountCompanyPartyId, "templatePurposeId", "TSK_TPL_MORTGAGE"));
        delegator.createOrStore(accountTemplateAssocGv);
        dispatcher.runSync("indexTaskInSolr", UtilMisc.toMap("userLogin", userLogin, "taskId", clonedTaskId));
    }

    /**
     * Checks if a user login already exists with the input email, if so returns alreadyExist as true.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> validateAccountWithEmail(
            DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();

        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        String email = (String) context.get("email");
        result.put("email", email);
        boolean isAlreadyExist = false;
        try {
            GenericValue existingUserLogin =
                    EntityQuery.use(delegator).from("UserLogin").where("userLoginId", email).queryOne();
            if (UtilValidate.isNotEmpty(existingUserLogin)) {
                isAlreadyExist = true;
                String partyId = existingUserLogin.getString("partyId");
                GenericValue accountPartyGroupForEmail =
                        PartyGroupForPartyUtils.getPartyGroupForPartyId(
                                HierarchyUtils.getPartyByPartyId(delegator, partyId));
                if (UtilValidate.isNotEmpty(accountPartyGroupForEmail)) {
                    result.put("accountId", accountPartyGroupForEmail.getString("partyId"));
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(AccountServicesErrorMessages.ERROR_VALIDATING_USER, e.getMessage(), module);
        }
        result.put("alreadyExist", isAlreadyExist);
        return result;
    }

    /**
     * Searches and returns paginated response of found account members for the input account id
     * (partyId). Looks up active (expired records are omitted via fromDate/thruDate) relationship
     * from account party id to other party ids matching any of @EmployerPersonRoles(ENUM class).
     * Allows case insensitive search by their name, email. Returns a List of Maps containing every
     * persons rich details. For members who have not accepted the invitation yet, also returns
     * invitationPending as true.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> searchAccountMembers(
            DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();

        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        String accountId = (String) context.get("accountId");
        String keyword = (String) context.get("keyword");
        String email = (String) context.get("email");
        String name = (String) context.get("name");
        String memberPartyIdToFilterBy = (String) context.get("memberPartyId");
        String externalId = (String) context.get("externalId");
        String sortBy = (String) context.get("sortBy");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        Integer startIndex = (Integer) context.get("startIndex");
        Integer viewSize = (Integer) context.get("viewSize");

        Map searchAccountMembersCtx =
                UtilMisc.toMap(
                        "noConditionFind", "Y",
                        "partyRelationshipTypeId", AccountRoles.EMPLOYMENT.getPartyRelationshipTypeId(),
                        "roleTypeIdTo", AccountRoles.EMPLOYEE.getPartyRelationshipTypeId(),
                        "partyTypeId", PartyTypesEnum.PERSON.getPartyTypeId(),
                        "partyIdFrom", accountId);

        searchAccountMembersCtx.put("invitationStatusId_op", "notEqual");
        searchAccountMembersCtx.put("invitationStatusId_ic", "Y");
        searchAccountMembersCtx.put(
                "invitationStatusId", PartyInvitationStatusEnum.PENDING.getStatusId());

        if (UtilValidate.isNotEmpty(keyword)) {
            searchAccountMembersCtx.put("searchText_op", "contains");
            searchAccountMembersCtx.put("searchText_ic", "Y");
            searchAccountMembersCtx.put("searchText", keyword);
        }
        if (UtilValidate.isNotEmpty(email)) {
            searchAccountMembersCtx.put("email_op", "contains");
            searchAccountMembersCtx.put("email_ic", "Y");
            searchAccountMembersCtx.put("email", email);
        }
        if (UtilValidate.isNotEmpty(name)) {
            searchAccountMembersCtx.put("displayName_op", "contains");
            searchAccountMembersCtx.put("displayName_ic", "Y");
            searchAccountMembersCtx.put("displayName", name);
        }
        if (UtilValidate.isNotEmpty(externalId)) {
            searchAccountMembersCtx.put("externalId_op", "contains");
            searchAccountMembersCtx.put("externalId_ic", "Y");
            searchAccountMembersCtx.put("externalId", externalId);
        }
        if (UtilValidate.isNotEmpty(memberPartyIdToFilterBy)) {
            searchAccountMembersCtx.put("partyId_op", "equals");
            searchAccountMembersCtx.put("partyId_ic", "Y");
            searchAccountMembersCtx.put("partyId", memberPartyIdToFilterBy);
        }

        Debug.logInfo("Invoking performFind with inputFields : " + searchAccountMembersCtx, module);

        Map performFindCtx =
                UtilMisc.toMap(
                        "inputFields", searchAccountMembersCtx,
                        "entityName", "ExtPartyRelationshipAndDetail",
                        "filterByDate", "Y",
                        "filterByDateValue", UtilDateTime.nowTimestamp(),
                        "orderBy", sortBy,
                        "viewIndex", startIndex,
                        "viewSize", viewSize);

        int totalAccountMembers;
        List<Map> accountMembers = new ArrayList<>();
        try {
            Map<String, Object> searchPartyNotesResult =
                    dispatcher.runSync("performFindList", performFindCtx);
            List<GenericValue> resultPartialList =
                    (List<GenericValue>) searchPartyNotesResult.get("list");

            // get total count for pagination
            totalAccountMembers = (Integer) searchPartyNotesResult.get("listSize");

            for (GenericValue record : CollectionUtils.emptyIfNull(resultPartialList)) {
                String memberPartyId = record.getString("partyId");
                Map<String, Object> memberRecord =
                        UtilMisc.toMap(
                                "partyId", memberPartyId,
                                "partyTypeId", record.getString("partyTypeId"),
                                "positionTitle", record.getString("positionTitle"), //
                                "relationshipName",
                                record.getString(
                                        "relationshipName"), // Official name of relationship, such as title in a
                                // company
                                "comments", record.getString("comments"), // any comments about the relationship
                                "externalId", record.getString("externalId"),
                                "description", record.getString("description"), // any description on the party
                                "photoUrl", record.getString("photoUrl"),
                                "displayName", record.getString("displayName"),
                                "email", record.getString("email"),
                                "firstName", record.getString("firstName"),
                                "middleName", record.getString("middleName"),
                                "lastName", record.getString("lastName"),
                                "firstNameLocal", record.getString("firstNameLocal"),
                                "lastNameLocal", record.getString("lastNameLocal"),
                                "suffix", record.getString("suffix"),
                                "groupName", record.getString("groupName"),
                                "groupNameLocal", record.getString("groupNameLocal"),
                                "createdDate", record.getTimestamp("createdDate"),
                                "lastModifiedDate", record.getTimestamp("lastModifiedDate"),
                                "lastTimeZone", record.getString("lastTimeZone"),
                                "lastLoggedInAt", record.getTimestamp("lastLoggedInAt"));

                String locationContactMechId = record.getString("locationContactMechId");

                if (UtilValidate.isNotEmpty(locationContactMechId)) {
                    String countryGeoId = record.getString("locationCountryGeoId");
                    GenericValue countryGeo = GeoUtil.getCountryGeo(countryGeoId, delegator);
                    String countryGeoCode = "";
                    String countryGeoName = "";
                    String countryAbbreviation = "";
                    if (UtilValidate.isNotEmpty(countryGeo)) {
                        countryGeoCode = countryGeo.getString("geoCode");
                        countryGeoName = countryGeo.getString("geoName");
                        countryAbbreviation = countryGeo.getString("abbreviation");
                    }

                    String stateGeoId = record.getString("locationStateProvinceGeoId");
                    GenericValue stateGeo = GeoUtil.getCountryGeo(stateGeoId, delegator);
                    String stateGeoCode = "";
                    String stateGeoName = "";
                    String stateAbbreviation = "";
                    if (UtilValidate.isNotEmpty(stateGeo)) {
                        stateGeoCode = stateGeo.getString("geoCode");
                        stateGeoName = stateGeo.getString("geoName");
                        stateAbbreviation = stateGeo.getString("abbreviation");
                    }

                    Map memberLocationPostalAddressMap =
                            UtilMisc.toMap(
                                    "contactMechId",
                                    record.getString("locationContactMechId"),
                                    "toName",
                                    record.getString("locationToName"),
                                    "attnName",
                                    record.getString("locationAttnName"),
                                    "address1",
                                    record.getString("locationAddress1"),
                                    "address2",
                                    record.getString("locationAddress2"),
                                    "city",
                                    record.getString("locationCity"),
                                    "stateProvinceGeoId",
                                    stateGeoId,
                                    "stateName",
                                    stateGeoName,
                                    "stateCode",
                                    stateGeoCode,
                                    "stateAbbr",
                                    stateAbbreviation,
                                    "countryGeoId",
                                    countryGeoId,
                                    "countryGeoCode",
                                    countryGeoCode,
                                    "countryName",
                                    countryGeoName,
                                    "countryAbbr",
                                    countryAbbreviation,
                                    "postalCode",
                                    record.getString("locationPostalCode"),
                                    "building",
                                    record.getString("locationBuilding"),
                                    "room",
                                    record.getString("locationRoom"),
                                    "apartment",
                                    record.getString("locationApartment"),
                                    "entryCode",
                                    record.getString("locationEntryCode"),
                                    "googlePlaceId",
                                    record.getString("locationGooglePlaceId"),
                                    "formattedAddress",
                                    record.getString("locationFormattedAddress"),
                                    "adrAddress",
                                    record.getString("locationAdrAddress"),
                                    "googleUrl",
                                    record.getString("locationGoogleUrl"),
                                    "staticMapUrl",
                                    record.getString("locationStaticMapUrl"),
                                    "staticMapUrl2",
                                    record.getString("locationStaticMapUrl2"),
                                    "latitude",
                                    record.getDouble("locationLatitude"),
                                    "longitude",
                                    record.getDouble("locationLongitude"),
                                    "timeZoneId",
                                    record.getString("locationTimeZoneId"),
                                    "directions",
                                    record.getString("locationDirections"));

                    memberRecord.put("locationAddress", memberLocationPostalAddressMap);
                }
                memberRecord.put("lastTimeZone", record.getString("lastTimeZone"));

                String displayName = record.getString("displayName");
                if (UtilValidate.isEmpty(displayName)) {
                    // fix the display name
                    dispatcher.runSync(
                            "populateBasicInformationForParty",
                            UtilMisc.toMap("partyId", memberPartyId, "userLogin", userLogin));
                }

                memberRecord.put("displayName", displayName);

                accountMembers.add(memberRecord);
            }

        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(AccountServicesErrorMessages.SYSTEM_ERROR);
        }

        result.put("accountMembers", accountMembers);
        result.put("totalAccountMembers", totalAccountMembers);
        return result;
    }

    /**
     * Checks if a user login already exists with the input email, if so returns alreadyExist as true.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> validateEmail(
            DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();

        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        String email = (String) context.get("email");
        result.put("email", email);
        boolean isAlreadyExist = false;
        try {
            GenericValue existingContactInfo =
                    EntityQuery.use(delegator).from("ContactMech").where("infoString", email).queryOne();
            if (UtilValidate.isNotEmpty(existingContactInfo)) {
                isAlreadyExist = true;
            }
        } catch (GenericEntityException e) {
            Debug.logError(AccountServicesErrorMessages.ERROR_VALIDATING_USER, e.getMessage(), module);
        }
        result.put("alreadyExist", isAlreadyExist);
        return result;
    }

    /**
     * Service to create product store for an application account. Uses the name of the company
     * (PartyGroup.name) to create the store name, example Acme Inc will create a store with name Acme
     * Inc Store. Also checks for existing stores, if account already has an existing store returns
     * the existing store id.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> axCreateProductStoreForAccount(
            DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();

        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        String accountId = (String) context.get("accountId");
        try {

            // check if a store doesn't exist already to avoid duplicates
            /*String existingStoreId = StoreHelper.getStoreForPartyId(delegator, accountId);
            if (UtilValidate.isNotEmpty(existingStoreId)) {
                Debug.logWarning(
                        "A store with id # "
                                + existingStoreId
                                + " already exists for the account, doing nothing.",
                        module);
                result.put("storeId", existingStoreId);
                return result;
            }*/

            Map<String, Object> createProductStoreCtx = new HashMap<>();

            GenericValue partyAccount =
                    EntityQuery.use(delegator).from("PartyGroup").where("partyId", accountId).queryOne();
            if (UtilValidate.isNotEmpty(partyAccount)) {
                createProductStoreCtx.put("userLogin", HierarchyUtils.getSysUserLogin(delegator));
                String accountName = partyAccount.getString("groupName");
                String storeName = accountName + "'s store (auto generated)";
                createProductStoreCtx.put("storeName", storeName); // update
                // on profile update
                createProductStoreCtx.put("companyName", accountName);
                createProductStoreCtx.put("title", storeName);
                createProductStoreCtx.put("isDemoStore", "N");
                createProductStoreCtx.put("requireInventory", "N");
                createProductStoreCtx.put("defaultCurrencyUomId", ""); // populate on update
                createProductStoreCtx.put("defaultTimeZoneString", ""); // populate on update
                createProductStoreCtx.put("headerApprovedStatus", "ORDER_APPROVED");
                createProductStoreCtx.put("itemApprovedStatus", "ITEM_APPROVED");
                createProductStoreCtx.put("digitalItemApprovedStatus", "ITEM_APPROVED");
                createProductStoreCtx.put("headerDeclinedStatus", "ORDER_REJECTED");
                createProductStoreCtx.put("itemDeclinedStatus", "ITEM_REJECTED");
                createProductStoreCtx.put("headerCancelStatus", "ORDER_CANCELLED");
                createProductStoreCtx.put("itemCancelStatus", "ITEM_CANCELLED");
                createProductStoreCtx.put("visualThemeId", "EC_DEFAULT");
                createProductStoreCtx.put("storeCreditAccountEnumId", "FIN_ACCOUNT");
                createProductStoreCtx.put("autoApproveInvoice", "Y");
                createProductStoreCtx.put("autoApproveOrder", "Y");
                createProductStoreCtx.put("shipCaptureFails", "Y");
                createProductStoreCtx.put("showOutOfStockProducts", "Y");
                createProductStoreCtx.put("payToPartyId", accountId);

                Map<String, Object> createProductStoreResp;

                createProductStoreResp = dispatcher.runSync("createProductStore", createProductStoreCtx);
                if (ServiceUtil.isError(createProductStoreResp)) {
                    Debug.logError(
                            "An error occurred while invoking createProductStoreResp "
                                    + "service"
                                    + ServiceUtil.getErrorMessage(createProductStoreResp),
                            module);
                    return ServiceUtil.returnError(AccountServicesErrorMessages.SYSTEM_ERROR_RESP);
                }

                String productStoreId = (String) createProductStoreResp.get("productStoreId");

                result.put("storeId", productStoreId);
            }
        } catch (GenericEntityException | GenericServiceException e) {
            Debug.logError(
                    AccountServicesErrorMessages.ERROR_CREATING_PRODUCT_STORE + accountId,
                    e.getMessage(),
                    module);
        }
        return result;
    }

    /**
     * Updates the total number of active account members PartyGroup.numEmployees for an account
     * (PartyGroup). Calls searchAccountMembers service to fetch the fresh count and updates
     * accordingly. Returns the new count.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> updateAccountMembersCountForAccount(
            DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();

        LocalDispatcher dispatcher = dctx.getDispatcher();

        String accountPartyGroupId = (String) context.get("accountPartyGroupId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        Long totalAccountMembers = 0l;
        Map searchAccountMembersResp;
        try {
            // make fresh call to get just the number of members for the account, fetch only one record
            // (viewSize) since we are only interested in the count.
            searchAccountMembersResp =
                    dispatcher.runSync(
                            "searchAccountMembers",
                            UtilMisc.toMap("accountId", accountPartyGroupId, "viewSize", 1));

            Integer totalAccountMembersCount =
                    (Integer) searchAccountMembersResp.get("totalAccountMembers");
            totalAccountMembers = new Long(totalAccountMembersCount);

            dispatcher.runSync(
                    "updatePartyGroup",
                    UtilMisc.toMap(
                            "partyId", accountPartyGroupId,
                            "numEmployees", totalAccountMembers,
                            "userLogin", userLogin));
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        result.put("totalAccountMembers", totalAccountMembers);
        return result;
    }

    /**
     * Validate first if team member exists or not, if not returns success silently Disables the
     * userlogin, party, updates the employee count of the organization, expires the party relationships.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> removeAccountMember(
            DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String userLoginId = userLogin.getString("userLoginId");

        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String memberPartyId = (String) context.get("memberPartyId");
        String orgGroupPartyId = (String) context.get("accountPartyId");

        Map<String, Object> disableUserContext = FastMap.newInstance();
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();

        Long updatedNumberOfAccountMembers = 0l;

        try {
            // validate if the team member exists or not
            Map<String, Object> searchAccountMembersResp =
                    dispatcher.runSync(
                            "searchAccountMembers",
                            UtilMisc.toMap("accountId", orgGroupPartyId, "memberPartyId", memberPartyId));
            List<Map> accountMembers =
                    UtilGenerics.toList(searchAccountMembersResp.get("accountMembers"));
            if (UtilValidate.isEmpty(accountMembers)) {
                String message =
                        "Party with partyId ["
                                + memberPartyId
                                + "] does not belong to the organisation with account Id [ "
                                + orgGroupPartyId
                                + " ]";
                Debug.logInfo(message, module);

                return ServiceUtil.returnSuccess();
            }

            GenericValue partyUserLogin = PartyWorker.findPartyLatestUserLogin(memberPartyId, delegator);
            // disable userlogin
            disableUserContext.put("partyId", memberPartyId);
            String memberUserLoginId = partyUserLogin.getString("userLoginId");
            disableUserContext.put("userLoginId", memberUserLoginId);
            disableUserContext.put("enabled", "N");
            disableUserContext.put("disabledBy", userLogin.getString("userLoginId"));
            disableUserContext.put("disabledDateTime", nowTimestamp);
            disableUserContext.put("userLogin", userLogin);

            dispatcher.runSync("updateUserLoginSecurity", disableUserContext);

            // disable party
            dispatcher.runSync(
                    "setPartyStatus",
                    UtilMisc.toMap(
                            "partyId", memberPartyId,
                            "statusId", "PARTY_DISABLED",
                            "disabledBy", userLoginId,
                            "userLogin", userLogin));

            // expire any available relationships
            List<GenericValue> availableRelationships =
                    EntityQuery.use(delegator)
                            .from("PartyRelationship")
                            .where("partyIdTo", memberPartyId)
                            .queryList();

            // expire the relationships
            List<GenericValue> recordsToUpdate = FastList.newInstance();
            for (GenericValue availableRelationship : availableRelationships) {

                availableRelationship.set("thruDate", UtilDateTime.nowTimestamp());
                recordsToUpdate.add(availableRelationship);
            }

            // remove any party invitations
            List<GenericValue> partyInvitations =
                    EntityQuery.use(delegator)
                            .from("PartyInvitation")
                            .where("partyId", memberPartyId)
                            .queryList();
            if (UtilValidate.isNotEmpty(partyInvitations)) {
                delegator.removeAll(partyInvitations);
            }

            delegator.storeAll(recordsToUpdate);

            // make fresh call to update the number of members for the account.
            Map updateAccountMembersCountForAccountResp =
                    dispatcher.runSync(
                            "updateAccountMembersCountForAccount",
                            UtilMisc.toMap(
                                    "accountPartyGroupId", orgGroupPartyId,
                                    "userLogin", userLogin));

            updatedNumberOfAccountMembers =
                    (Long) updateAccountMembersCountForAccountResp.get("totalAccountMembers");

            // expire any active acccess tokens as active tokens are valid for an year.
            dispatcher.runSync(
                    "expireAllAccessTokensForUserlogin", UtilMisc.toMap("userLoginId", memberUserLoginId));

        } catch (GenericServiceException | GenericEntityException e) {
            Debug.logError(e, "An error occurred while removing team member", module);
            return ServiceUtil.returnError(e.getMessage());
        }

        serviceResult.put("updatedNumberOfAccountMembers", updatedNumberOfAccountMembers);

        return serviceResult;
    }
}
