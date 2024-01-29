package com.simbaquartz.xapi.connect.api.account.impl;

import com.simbaquartz.xapi.connect.api.NotFoundException;
import com.simbaquartz.xapi.connect.api.account.AccountApiService;
import com.simbaquartz.xapi.connect.api.account.utils.AccountUtils;
import com.simbaquartz.xapi.connect.api.security.LoggedInUser;
import com.simbaquartz.xapi.connect.utils.ApiResponseUtil;
import com.simbaquartz.xapi.connect.utils.XapiUtil;
import com.simbaquartz.xapi.helper.SubscriptionHelper;
import com.simbaquartz.xapi.services.FileStorageTypesEnum;
import com.fidelissd.zcp.xcommon.collections.FastMap;
import com.fidelissd.zcp.xcommon.enums.AccountUserRoleTypesEnum;
import com.fidelissd.zcp.xcommon.models.account.AccountStorage;
import com.fidelissd.zcp.xcommon.models.account.ApplicationAccount;
import com.fidelissd.zcp.xcommon.models.account.ApplicationUser;
import com.fidelissd.zcp.xcommon.models.account.ApplicationUserSearchCriteria;
import com.fidelissd.zcp.xcommon.models.client.billing.BillingPlans;
import com.fidelissd.zcp.xcommon.models.geo.builder.GeoModelBuilder;
import com.fidelissd.zcp.xcommon.models.search.SearchResults;
import com.fidelissd.zcp.xcommon.util.AppConfigUtil;
import com.fidelissd.zcp.xcommon.util.AppConfigUtil.ApplicationPreferences;
import com.fidelissd.zcp.xcommon.util.AxUtilFormat;
import com.fidelissd.zcp.xcommon.util.hierarchy.HierarchyUtils;
import org.apache.commons.io.IOUtils;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.*;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.GenericDispatcherFactory;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class AccountApiServiceImpl extends AccountApiService {

    private static final String module = AccountApiServiceImpl.class.getName();

    private static final String INVALID_APP_ACCOUNT_CHANGE_REQUEST =
            "We were not able to change the role to :accountType";

    /**
     * Creates an application account using input details, creates a person record and user login as
     * well for the user signing up for an account.
     *
     * @param userAccount
     * @return
     * @throws NotFoundException
     */
    @Override
    public Response createApplicationAccount(ApplicationAccount userAccount)
            throws NotFoundException {
        Map<String, Object> createUserAccountContext = new HashMap<String, Object>();

        Delegator delegator = DelegatorFactory.getDelegator("default");
        createUserAccountContext.put("userLogin", HierarchyUtils.getSysUserLogin(delegator));
        LocalDispatcher dispatcher =
                new GenericDispatcherFactory().createLocalDispatcher("default", delegator);

        createUserAccountContext.put(
                "email", userAccount.getEmail().toLowerCase()); // auto lowercase emails
        createUserAccountContext.put("fullName", userAccount.getFullName());
        createUserAccountContext.put("password", userAccount.getPassword());

        createUserAccountContext.put("acceptsMarketing", userAccount.getAcceptsMarketing());

        String accountId;

        if (UtilValidate.isNotEmpty(userAccount.getOrganizationName())
                && UtilValidate.isNotEmpty(userAccount.getNumberOfEmployees())) {
            createUserAccountContext.put("companyName", userAccount.getOrganizationName());
            createUserAccountContext.put("numberOfEmployees", userAccount.getNumberOfEmployees());
        }

        try {
            Map createUserAccountResp =
                    dispatcher.runSync("createApplicationAccount", createUserAccountContext);
            if (ServiceUtil.isError(createUserAccountResp)) {
                Debug.logError(
                        "An error occurred while invoking createUserAccount service",
                        ServiceUtil.getErrorMessage(createUserAccountResp),
                        module);
                return ApiResponseUtil.prepareDefaultResponse(
                        Response.Status.INTERNAL_SERVER_ERROR,
                        ServiceUtil.getErrorMessage(createUserAccountResp));
            }
            accountId = (String) createUserAccountResp.get("accountId");
        } catch (GenericServiceException e) {
            Debug.logError(e, "An error occurred while invoking createUserAccount service", module);
            return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR);
        }

        // check if subscription is enabled, if so set up Stripe subscription
        String appSubscriptionEnabled =
                EntityUtilProperties.getPropertyValue("appconfig", "app.subscription.enabled", delegator);

        if (UtilValidate.isNotEmpty(appSubscriptionEnabled)
                && "Y".equalsIgnoreCase(appSubscriptionEnabled)) {
            // app subscription is enabled set up stripe stuff
            Map<String, Object> createStripeCustomerCtx =
                    UtilMisc.toMap(
                            "customerPartyId", accountId,
                            "userLogin", HierarchyUtils.getSysUserLogin(delegator),
                            "email", userAccount.getEmail().toLowerCase(),
                            "organizationName", userAccount.getOrganizationName());

            String stripeCustomerId = null;
            try {
                Debug.logInfo("Creating Stripe Customer for party Id: " + accountId, module);
                Map<String, Object> createCustomerInStripeResp =
                        dispatcher.runSync("createCustomerInStripe", createStripeCustomerCtx);
                Debug.logInfo(
                        "Response from Service for creating stripe customer: " + createCustomerInStripeResp,
                        module);
                stripeCustomerId = (String) createCustomerInStripeResp.get("stripeCustomerId");
            } catch (GenericServiceException e) {
                Debug.logError("Error creating customer in Stripe. Error: " + e.getMessage(), module);
            }

            String stripePlanId =
                    AppConfigUtil.getAppPreference(delegator, ApplicationPreferences.STRIPE_BASIC_PLAN);

            if (UtilValidate.isNotEmpty(stripePlanId)) {
                // Activating the basic plan for the user
                Map<String, Object> createSubscriptionInStripeCtx =
                        UtilMisc.toMap(
                                "stripeCustomerId", stripeCustomerId,
                                "userLogin", HierarchyUtils.getSysUserLogin(delegator),
                                "stripePlanId", stripePlanId);

                try {
                    Debug.logInfo("Creating Stripe Customer Subscription for party Id: " + accountId, module);
                    Map<String, Object> createSubscriptionInStripeResp =
                            dispatcher.runSync("createSubscriptionInStripe", createSubscriptionInStripeCtx);
                    Debug.logInfo(
                            "Response from Service for creating stripe customer subscription: "
                                    + createSubscriptionInStripeResp,
                            module);
                } catch (GenericServiceException e) {
                    Debug.logError("Error creating subscription in Stripe. Error: " + e.getMessage(), module);
                }
                AccountUtils.setUserLicenseActive(dispatcher, delegator, accountId);
            } else {
                // failing silently
                Debug.logInfo(
                        "Unable to complete Stripe Customer Subscription activation, not able to "
                                + "find any plan"
                                + accountId,
                        module);
            }
        } else {
            Debug.logInfo(
                    "Subscription is disabled, skipping setting up Stripe subscription plan and customer, to enable set app.subscription.enabled=Y in appconfig.properties or SystemPropertyOverride.xml",
                    module);
        }

        // remove the password
        userAccount.setPassword(null);
        userAccount.setId(accountId);

        return ApiResponseUtil.prepareOkResponse(userAccount);
    }

    @Override
    public Response checkDuplicate(String emailId) throws NotFoundException {
        Map<String, Object> responseMap = new HashMap<>();
        Map<String, Object> accountEmailContext = new HashMap<String, Object>();

        Delegator delegator = DelegatorFactory.getDelegator("default");
        accountEmailContext.put("userLogin", HierarchyUtils.getSysUserLogin(delegator));
        LocalDispatcher dispatcher =
                new GenericDispatcherFactory().createLocalDispatcher("default", delegator);

        accountEmailContext.put("email", emailId.toLowerCase());

        try {
            Map accountEmailResp = dispatcher.runSync("validateAccountWithEmail", accountEmailContext);
            if (ServiceUtil.isError(accountEmailResp)) {
                Debug.logError(
                        "An error occurred while invoking createUserAccount service",
                        ServiceUtil.getErrorMessage(accountEmailResp),
                        module);
                return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR);
            }
            responseMap = accountEmailResp;

        } catch (GenericServiceException e) {
            Debug.logError(
                    e, "An error occurred while invoking validateAccountWithEmail service", module);
            return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR);
        }

        return ApiResponseUtil.prepareOkResponse(responseMap);
    }

    /**
     * Returns the list of account members available under the account with their licensing details.
     * Looks for @AccountRoles
     *
     * @param accountId
     * @param securityContext
     * @return
     * @throws NotFoundException
     */
    @Override
    public Response listAccountMembers(
            String accountId,
            ApplicationUserSearchCriteria applicationUserSearchCriteria,
            SecurityContext securityContext)
            throws NotFoundException {
        LoggedInUser loggedInUser = (LoggedInUser) securityContext.getUserPrincipal();

        List<ApplicationUser> accountMembers = new ArrayList<>();
        int totalAccountMembers;

        Map searchAccountMembersCtx =
                UtilMisc.toMap(
                        "accountId", accountId,
                        "startIndex", applicationUserSearchCriteria.getStartIndex(),
                        "viewSize", applicationUserSearchCriteria.getViewSize(),
                        "sortBy", applicationUserSearchCriteria.getSortBy(),
                        "userLogin", loggedInUser.getUserLogin());

        String keyword = applicationUserSearchCriteria.getKeyword();
        if (UtilValidate.isNotEmpty(keyword)) {
            searchAccountMembersCtx.put("keyword", keyword);
        }
        String email = applicationUserSearchCriteria.getEmail();
        if (UtilValidate.isNotEmpty(email)) {
            searchAccountMembersCtx.put("email", email);
        }
        String name = applicationUserSearchCriteria.getName();
        if (UtilValidate.isNotEmpty(name)) {
            searchAccountMembersCtx.put("name", name);
        }

        Debug.logInfo("Invoking searchAccountMembers with input: " + searchAccountMembersCtx, module);

        try {
            Map searchAccountMembersResponse =
                    dispatcher.runSync("searchAccountMembers", searchAccountMembersCtx);
            totalAccountMembers = (Integer) searchAccountMembersResponse.get("totalAccountMembers");
            if (totalAccountMembers > 0) {
                List<Map> accountMemberRecords = (List) searchAccountMembersResponse.get("accountMembers");
                // populate beans
                for (Map accountMemberRecord : accountMemberRecords) {
                    ApplicationUser applicationUser = new ApplicationUser();
                    String accountMemberPartyId = (String) accountMemberRecord.get("partyId");
                    applicationUser.setId(accountMemberPartyId);
                    if (accountMemberPartyId.equals(loggedInUser.getPartyId())) {
                        applicationUser.setSelf(true);
                    }
                    applicationUser.setDisplayName((String) accountMemberRecord.get("displayName"));
                    applicationUser.setFirstName((String) accountMemberRecord.get("firstName"));
                    applicationUser.setLastName((String) accountMemberRecord.get("lastName"));
                    applicationUser.setPhotoUrl((String) accountMemberRecord.get("photoUrl"));
                    applicationUser.setEmail((String) accountMemberRecord.get("email"));
                    applicationUser.setExternalId((String) accountMemberRecord.get("externalId"));
                    applicationUser.setCreatedAt((Timestamp) accountMemberRecord.get("createdDate"));
                    applicationUser.setLastModifiedAt(
                            (Timestamp) accountMemberRecord.get("lastModifiedDate"));
                    applicationUser.setLastLoggedInAt((Timestamp) accountMemberRecord.get("lastLoggedInAt"));

                    Map memberLocationAddress = (Map) accountMemberRecord.get("locationAddress");
                    if (UtilValidate.isNotEmpty(memberLocationAddress)) {
                        applicationUser.setLocation(GeoModelBuilder.buildPostalAddress(memberLocationAddress));
                    }

                    applicationUser.setTimezone((String) accountMemberRecord.get("lastTimeZone"));

                    accountMembers.add(applicationUser);
                }
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR);
        }

        SearchResults searchResults = new SearchResults();
        searchResults.setStartIndex(applicationUserSearchCriteria.getStartIndex());
        searchResults.setViewSize(applicationUserSearchCriteria.getViewSize());
        searchResults.setTotalNumberOfRecords(totalAccountMembers);
        searchResults.setRecords(accountMembers);

        return ApiResponseUtil.prepareOkResponse(searchResults);
    }

    /**
     * Update the permissions of the member.
     *
     * @param partyId         partyId is the id of the user whose Permissions you want to change
     * @param securityContext
     * @return
     * @throws NotFoundException
     */
    @Override
    public Response changePermissions(
            String partyId, ApplicationAccount applicationAccount, SecurityContext securityContext)
            throws NotFoundException {

        LoggedInUser loggedInUser = (LoggedInUser) securityContext.getUserPrincipal();
        GenericValue userLogin = loggedInUser.getUserLogin();
        // Get required data from the bean
        String applicationAccountType = applicationAccount.getRole();
        String applicationAccountTypeId = null;
        if (UtilValidate.areEqual(
                AccountUserRoleTypesEnum.ADMIN.getRoleName(), applicationAccountType)) {
            // user wants to change the permissions to admin
            applicationAccountTypeId = AccountUserRoleTypesEnum.ADMIN.getRole();
        } else if (UtilValidate.areEqual(
                AccountUserRoleTypesEnum.MANAGER.getRoleName(), applicationAccountType)) {
            // user wants to change the permissions to manager
            applicationAccountTypeId = AccountUserRoleTypesEnum.MANAGER.getRole();
        } else if (UtilValidate.areEqual(
                AccountUserRoleTypesEnum.MEMBER.getRoleName(), applicationAccountType)) {
            // user wants to change the permissions to member
            applicationAccountTypeId = AccountUserRoleTypesEnum.MEMBER.getRole();
        } else {
            // return error. 4xx, due to invalid input
            return ApiResponseUtil.prepareErrorResponse(
                    Response.Status.BAD_REQUEST,
                    INVALID_APP_ACCOUNT_CHANGE_REQUEST.replace(":accountType", applicationAccountType));
        }

        // get user login from party id
        String userLoginId = null;
        try {
            GenericValue existingUserLogin =
                    EntityQuery.use(delegator).from("UserLogin").where("partyId", partyId).queryFirst();
            if (UtilValidate.isNotEmpty(existingUserLogin)) {
                userLoginId = (String) existingUserLogin.get("userLoginId");
            }
        } catch (GenericEntityException e) {
            // Handle error here
            e.printStackTrace();
            return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR);
        }

        Map<String, Object> changeAppUserPermissionsServiceResponse = null;
        if (UtilValidate.isNotEmpty(userLoginId) && UtilValidate.isNotEmpty(applicationAccountTypeId)) {
            try {
                changeAppUserPermissionsServiceResponse =
                        dispatcher.runSync(
                                "changeAppUserPermissions",
                                UtilMisc.toMap(
                                        "userLogin", userLogin,
                                        "accountId", userLoginId,
                                        "groupId", applicationAccountTypeId));
            } catch (GenericServiceException e) {
                return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR);
            }
        }
        return ApiResponseUtil.prepareOkResponse("Success");
    }

    /**
     * get the permissions of the member.
     *
     * @param partyId
     * @param securityContext
     * @return
     * @throws NotFoundException
     */
    @Override
    public Response getPermissions(String partyId, SecurityContext securityContext) {

        ApplicationAccount applicationAccount = new ApplicationAccount();
        LoggedInUser loggedInUser = (LoggedInUser) securityContext.getUserPrincipal();

        LocalDispatcher tenantDispatcher = loggedInUser.getDispatcher();
        // Get Party Top Level Permission
        Map<String, Object> getAppUserPermissionResponse = null;
        try {
            getAppUserPermissionResponse =
                    tenantDispatcher.runSync(
                            "getAppUserPermission",
                            UtilMisc.toMap("userLogin", loggedInUser.getUserLogin(), "accountId", partyId));
        } catch (GenericServiceException e) {
            e.printStackTrace();
        }
        List<GenericValue> permissions =
                (List<GenericValue>) getAppUserPermissionResponse.get("permissions");

        if (UtilValidate.isNotEmpty(permissions)) {
            String getAppUserPermissionResponseRoleId = (String) permissions.get(0).getString("groupId");
            if (UtilValidate.isNotEmpty(getAppUserPermissionResponseRoleId)) {
                // get from enum
                String applicationAccountTypeName = null;
                if (UtilValidate.areEqual(
                        AccountUserRoleTypesEnum.ADMIN.getRole(), getAppUserPermissionResponseRoleId)) {
                    // user wants to change the permissions to admin
                    applicationAccountTypeName = AccountUserRoleTypesEnum.ADMIN.getRoleName();
                } else if (UtilValidate.areEqual(
                        AccountUserRoleTypesEnum.MANAGER.getRole(), getAppUserPermissionResponseRoleId)) {
                    // user wants to change the permissions to manager
                    applicationAccountTypeName = AccountUserRoleTypesEnum.MANAGER.getRoleName();
                } else if (UtilValidate.areEqual(
                        AccountUserRoleTypesEnum.MEMBER.getRole(), getAppUserPermissionResponseRoleId)) {
                    // user wants to change the permissions to member
                    applicationAccountTypeName = AccountUserRoleTypesEnum.MEMBER.getRoleName();
                }
                applicationAccount.setRole(applicationAccountTypeName);
            }
        }

        return ApiResponseUtil.prepareOkResponse(applicationAccount);
    }

    public static class AccountErrorMessages {
        public static final String MSG_DUPLICATE_EMAIL_ACCOUNT =
                "An account with the input email already exists.";
    }

    @Override
    public Response getAllMembers(SecurityContext securityContext) throws NotFoundException {

        if (Debug.verboseOn()) Debug.logVerbose("Entering method getAllMembers", module);

        LoggedInUser loggedInUser = (LoggedInUser) securityContext.getUserPrincipal();
        LocalDispatcher tenantDispatcher = loggedInUser.getDispatcher();
        GenericDelegator tenantDelegator = loggedInUser.getDelegator();
        String partyId = loggedInUser.getPartyId();
        String orgGroupPartyId = loggedInUser.getAccountPartyId();

        List<Map> result = new LinkedList<>();
        try {
            List<GenericValue> appMembers =
                    EntityQuery.use(tenantDelegator)
                            .from("PartyRole")
                            .where("roleTypeId", "APP_MEMBER")
                            .queryList();
            if (UtilValidate.isNotEmpty(appMembers)) {
                for (GenericValue appMember : appMembers) {
                    String employee = appMember.getString("partyId");
                    GenericValue custRequestStatus =
                            EntityQuery.use(delegator)
                                    .from("PartyInvitation")
                                    .where("partyId", employee, "groupPartyId", orgGroupPartyId)
                                    .cache(true)
                                    .queryOne();
                    if (UtilValidate.isNotEmpty(custRequestStatus)
                            && !("PARTYINV_PENDING".equalsIgnoreCase(custRequestStatus.getString("statusId")))) {
                        Map<String, Object> getPartyCtx = FastMap.newInstance();
                        getPartyCtx.put("userLogin", loggedInUser.getUserLogin());
                        getPartyCtx.put("partyId", employee);

                        Map getPartyResponse = null;
                        try {
                            getPartyResponse = tenantDispatcher.runSync("fsdGetPartyDetails", getPartyCtx);
                        } catch (GenericServiceException e) {
                            // handle error here
                            Debug.logError(
                                    "An error occurred while invoking fsdGetPartyDetails service, details: "
                                            + e.getMessage(),
                                    "PartyApiServiceImpl");
                            if (Debug.verboseOn()) Debug.logVerbose("Exiting method getAllMembers", module);

                            return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR);
                        }
                        Map getPartyDetails = (Map) getPartyResponse.get("partyDetails");
                        result.add(getPartyDetails);
                    }
                }
            }
        } catch (GenericEntityException e) {
            // handle error here
            Debug.logError(
                    "An error occurred while invoking fsdGetPartyDetails service, details: " + e.getMessage(),
                    "PartyApiServiceImpl");
            if (Debug.verboseOn()) Debug.logVerbose("Exiting method getAllMembers", module);

            return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR);
        }

        return ApiResponseUtil.prepareOkResponse(result);
    }

    @Override
    public Response removeAccountMember(String partyId, SecurityContext securityContext)
            throws NotFoundException {

        if (Debug.verboseOn()) Debug.logVerbose("Entering method removeAccountMember", module);

        LoggedInUser loggedInUser = (LoggedInUser) securityContext.getUserPrincipal();
        LocalDispatcher tenantDispatcher = loggedInUser.getDispatcher();
        String orgGroupPartyId = loggedInUser.getAccountPartyId();

        Map<String, Object> removeTeamMemberCtx = FastMap.newInstance();
        Long updatedNumberOfAccountMembers = 0l;
        try {
            removeTeamMemberCtx.put("memberPartyId", partyId);
            removeTeamMemberCtx.put("accountPartyId", orgGroupPartyId);
            removeTeamMemberCtx.put("userLogin", loggedInUser.getUserLogin());

            Map<String, Object> removeTeamMemberResp =
                    tenantDispatcher.runSync("removeAccountMember", removeTeamMemberCtx);
            if (ServiceUtil.isError(removeTeamMemberResp)) {
                String errorMessage = ServiceUtil.getErrorMessage(removeTeamMemberResp);
                Debug.logError("An error occurred while invoking removeTeamMember service", module);
                if (Debug.verboseOn()) Debug.logVerbose("Exiting method deactivateUser", module);

                return ApiResponseUtil.prepareDefaultResponse(Response.Status.BAD_REQUEST, errorMessage);
            }

            updatedNumberOfAccountMembers =
                    (Long) removeTeamMemberResp.get("updatedNumberOfAccountMembers");

        } catch (GenericServiceException e) {
            // handle error here
            Debug.logError(
                    "An error occurred while invoking removeTeamMember service, details: " + e.getMessage(),
                    "PartyApiServiceImpl");
            if (Debug.verboseOn()) Debug.logVerbose("Exiting method getAllMembers", module);

            return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR);
        }

        return ApiResponseUtil.prepareOkResponse(
                UtilMisc.toMap(
                        "message",
                        "Team member removed successfully!",
                        "newAccountMembersCount",
                        updatedNumberOfAccountMembers));
    }

    @Override
    public Response getAccountStorageDetails(SecurityContext securityContext)
            throws NotFoundException {

        AccountStorage accountStorage = new AccountStorage();
        LoggedInUser loggedInUser = (LoggedInUser) securityContext.getUserPrincipal();
        BigDecimal totalStorageAvailable = BigDecimal.ZERO;
        BigDecimal totalStorageUsed = BigDecimal.ZERO;
        BigDecimal totalTaskStorageUsed = BigDecimal.ZERO;
        String storageUsedFormatted = "0 KB";
        try {
            Map getStorageUsedCtx =
                    UtilMisc.toMap(
                            "accountId", loggedInUser.getAccountPartyId(),
                            "userLogin", loggedInUser.getUserLogin());

            Map<String, Object> getStorageUsedResp =
                    dispatcher.runSync("getStorageUsed", getStorageUsedCtx);

            BigDecimal totalStorageUsedForAccount =
                    (BigDecimal) getStorageUsedResp.get("totalStorageUsed");
            if (UtilValidate.isNotEmpty(totalStorageUsedForAccount)) {
                totalStorageUsed = totalStorageUsedForAccount;
            }

            BigDecimal totalTaskStorageUsedForAccount =
                    (BigDecimal) getStorageUsedResp.get(FileStorageTypesEnum.TASK.getTypeId());
            if (UtilValidate.isNotEmpty(totalTaskStorageUsedForAccount)) {
                totalTaskStorageUsed = totalTaskStorageUsedForAccount;
            }

            // get the current plan and available storage for the account
            BillingPlans activeBillingPlan =
                    SubscriptionHelper.getActiveBillingPlanForAccount(
                            loggedInUser.getAccountPartyId(), delegator, dispatcher);
            if (UtilValidate.isNotEmpty(activeBillingPlan)) {
                totalStorageAvailable =
                        new BigDecimal((Integer) activeBillingPlan.getPlanLimits().get("storageInGb"));
            }

            accountStorage.setAvailable(totalStorageAvailable);
            accountStorage.setUsed(totalStorageUsed);

            String storageUsedInGb =
                    AxUtilFormat.formatFileSizeFromBtyesToGB(totalStorageUsed.longValue(), true);
            accountStorage.setUsedFormatted(storageUsedInGb + "GB");

            // calculate storage percentage used
            short percentageStorageUsed =
                    new BigDecimal(storageUsedInGb)
                            .divide(totalStorageAvailable)
                            .multiply(new BigDecimal(100))
                            .shortValue();
            accountStorage.setUsedPercentage(percentageStorageUsed);

            // task storage
            accountStorage.setTaskStorage(totalTaskStorageUsed);
            String taskStorageUsedInGb =
                    AxUtilFormat.formatFileSizeFromBtyesToGB(totalTaskStorageUsed.longValue(), true);
            accountStorage.setTaskStorageFormatted(taskStorageUsedInGb + "GB");
            // calculate task storage percentage used
            if ((Integer.parseInt(storageUsedInGb) > 0) && (totalTaskStorageUsed.intValue() > 0)) {
                short percentageTaskStorageUsed =
                        totalTaskStorageUsed
                                .divide(totalStorageUsed)
                                .multiply(new BigDecimal(100))
                                .shortValue();
                accountStorage.setTaskUsedPercentage(percentageTaskStorageUsed);
            }

        } catch (GenericServiceException e) {
            // handle error here
            Debug.logError(
                    "An error occurred while invoking removeTeamMember service, details: " + e.getMessage(),
                    "PartyApiServiceImpl");
            if (Debug.verboseOn()) Debug.logVerbose("Exiting method getAllMembers", module);

            return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR);
        }

        return ApiResponseUtil.prepareOkResponse(accountStorage);
    }

    @Override
    public Response bulkImportCustomerRequests(String accountId, MultipartFormDataInput attachment,
                                               SecurityContext securityContext) throws NotFoundException {
        Debug.logInfo("Entering bulkImportCustomerRequests method for account Id " + accountId, module);

        Map<String, List<InputPart>> uploadForm = attachment.getFormDataMap();
        List<InputPart> inputParts = uploadForm.get("file");
        Debug.logInfo("Input parts " + inputParts, module);

        Delegator delegator = DelegatorFactory.getDelegator("default");
        LoggedInUser loggedInUser = (LoggedInUser) securityContext.getUserPrincipal();
        if(UtilValidate.isEmpty(loggedInUser)) {
            return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR, "Valid user access token is required");
        }
        LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
        Map<String, String> jsonFileContentMap = new HashMap<>();
        // 1: Extract the zip file, to get list of JSON files
        if (null != inputParts) {
            for (InputPart inputPart : inputParts) {
                try {
                    MultivaluedMap<String, String> header = inputPart.getHeaders();
                    String fileName = XapiUtil.getFileName(header);
                    Debug.logInfo("Input zip file found " + fileName, module);

                    InputStream inputStream = inputPart.getBody(InputStream.class, null);
                    byte[] bytes = IOUtils.toByteArray(inputStream);
                    ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bytes));
                    ZipEntry zipEntry = zis.getNextEntry();

                    while (zipEntry != null) {
                        Debug.logInfo("Extracting Zip Entry (JSON file) " + zipEntry.getName(), module);
                        String entryName = zipEntry.getName();

                        StringBuilder jsonContent = new StringBuilder();
                        Scanner sc = new Scanner(zis);
                        while (sc.hasNextLine()) {
                            jsonContent.append(sc.nextLine());
                        }
                        jsonFileContentMap.put(entryName, jsonContent.toString());

                        zipEntry = zis.getNextEntry();
                    }
                    zis.closeEntry();
                    zis.close();
                } catch (IOException e) {
                    Debug.logError("An error occurred during importing customer requests, details: " + e.getMessage(), module);
                    if (Debug.verboseOn()) Debug.logVerbose("Exiting method bulkImportCustomerRequests", module);
                    return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
                }
            }
        }

        // 2: Process each JSON file
        if (UtilValidate.isNotEmpty(jsonFileContentMap)) {
            for (Map.Entry<String, String> entry : jsonFileContentMap.entrySet()) {
                Debug.logInfo("Processing file " + entry.getKey(), module);

                Map<String, Object> importRequestCtx = UtilMisc.toMap("accountId", accountId,
                        "salesForceCustomerRequestJson", entry.getValue(),
                        "userLogin", loggedInUser.getUserLogin());
                try {
                    dispatcher.runAsync("importSalesForceCustomerRequest", importRequestCtx);
                } catch (GenericServiceException e) {
                    Debug.logError("There was a problem importing sales force JSON as customer request. Error: " + e.getMessage(), module);
                    e.printStackTrace();
                }
            }
        }
        Debug.logInfo("Exiting bulkImportCustomerRequests method for account Id " + accountId, module);
        return ApiResponseUtil.prepareOkResponse("Import Completed");
    }
}
