package com.simbaquartz.xapi.connect.api.me.impl;

import com.fidelissd.zcp.xcommon.models.Workspace;
import com.fidelissd.zcp.xcommon.models.account.ApplicationUser;
import com.fidelissd.zcp.xcommon.models.account.ConnectedAccountGoogle;
import com.fidelissd.zcp.xcommon.models.account.ConnectedAccounts;
import com.fidelissd.zcp.xcommon.models.account.User;
import com.fidelissd.zcp.xcommon.models.client.billing.BillingInvoice;
import com.fidelissd.zcp.xcommon.models.client.billing.BillingPlans;
import com.fidelissd.zcp.xcommon.models.client.billing.Subscription;
import com.fidelissd.zcp.xcommon.models.geo.PostalAddress;
import com.fidelissd.zcp.xcommon.models.people.Person;
import com.fidelissd.zcp.xcommon.models.store.Store;
import com.simbaquartz.xapi.connect.api.NotFoundException;
import com.simbaquartz.xapi.connect.api.me.MeApiService;
import com.simbaquartz.xapi.connect.api.security.LoggedInUser;
import com.simbaquartz.xapi.connect.utils.ApiResponseUtil;
import com.simbaquartz.xapi.connect.utils.ModelBuilderUtil;
import com.simbaquartz.xparty.helpers.AxPartyHelper;
import com.simbaquartz.xparty.hierarchy.employee.EmployeeUtils;
import com.simbaquartz.xparty.services.person.PersonServices;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.GenericDelegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.*;

@javax.annotation.Generated(
        value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen",
        date = "2017-05-09T10:47:15.854-07:00"
)
public class MeApiServiceImpl extends MeApiService {
    private static final String module = MeApiServiceImpl.class.getName();

    /**
     * Returns, name , contact details (phone, email) and addresses for logged in user.
     *
     * @param securityContext
     * @return
     * @throws NotFoundException
     */
    @Override
    public Response meGet(SecurityContext securityContext) throws NotFoundException {
        if (Debug.verboseOn()) Debug.logVerbose("Entering method meGet", module);

        LoggedInUser loggedInUser = (LoggedInUser) securityContext.getUserPrincipal();
        // Use this tenant delegtor and dispatcher to fetch all the Quotes
        GenericDelegator delegator = loggedInUser.getDelegator();
        LocalDispatcher dispatcher = loggedInUser.getDispatcher();

        Boolean isAssumed = loggedInUser.getIsAssumed();
        String assumedBy = loggedInUser.getAssumedBy();

        ApplicationUser loggedInUserDetails = new ApplicationUser();
        String partyId = loggedInUser.getPartyId();
        loggedInUserDetails.setId(partyId);

        // prepare person information
        Person person =
                PersonServices.getPersonDetails(
                        delegator, dispatcher, partyId, loggedInUser.getUserLogin(), true, false);
        loggedInUserDetails.setPersonalDetails(person);

        // Get Party Top Level Permission
        loggedInUserDetails.setRole(loggedInUser.getRole());

        // get last used workspace
        Workspace workspace = getLastWorkspaceInfo(delegator, partyId);
        loggedInUserDetails.setWorkspace(workspace);

        // get store details
        /*String storeId = loggedInUser.getStoreId();
        if (UtilValidate.isNotEmpty(storeId)) {
            GenericValue storeGv = StoreHelper.getStore(delegator, loggedInUser.getStoreId());
            Store store = StoreModelBuilder.build(storeGv, dispatcher);
            loggedInUserDetails.setStore(store);
        }*/
        // get email verification status
        boolean isUserEmailVerified =
                AxPartyHelper.isEmailVerified(delegator, loggedInUser.getUserLoginId(), partyId);
        loggedInUserDetails.setEmailVerified(isUserEmailVerified);

        // get reporting manager
        String reportingManagerPartyId = EmployeeUtils.getEmployeeManagerPartyId(delegator, partyId);
        if (UtilValidate.isNotEmpty(reportingManagerPartyId)) {
            Map reportingManagerDetailsMap =
                    AxPartyHelper.getPartyBasicDetails(delegator, reportingManagerPartyId);
            User reportingManagerUser =
                    ModelBuilderUtil.prepareUserModel(delegator, reportingManagerPartyId, loggedInUser);
            loggedInUserDetails.setReportingManager(reportingManagerUser);
        }

        // subscription details
        try {
            loggedInUserDetails.setSubscription(prepareSubscription(loggedInUser));
        } catch (Exception e) {
            Debug.logError(e, module);
            return ApiResponseUtil.serverErrorResponse(e.getMessage());
        }

        if (UtilValidate.isNotEmpty(isAssumed)) {
            loggedInUserDetails.setIsAssumed(isAssumed);
        }
        if (UtilValidate.isNotEmpty(assumedBy)) {
            loggedInUserDetails.setAssumedBy(assumedBy);
        }

        //  populate connected accounts details, eg. Google (isConnected, defaultCalendarId, gmail-id, etc)
        ConnectedAccountGoogle connectedAccountGoogle = ConnectedAccountGoogle.builder()
                .isConnected(false)
                .build();
        try {
            Map<String, Object> isGoogleAuthorizedCtx = new HashMap<>();
            isGoogleAuthorizedCtx.put("userLogin", loggedInUser.getUserLogin());
            isGoogleAuthorizedCtx.put("partyId", partyId);

            Map<String, Object> isGoogleAuthorizedCtxResp = dispatcher.runSync("checkIfGoogleAuthorized", isGoogleAuthorizedCtx);
            if (ServiceUtil.isSuccess(isGoogleAuthorizedCtxResp)) {
                Boolean isGoogleConnected = (Boolean) isGoogleAuthorizedCtxResp.get("isAuthorized");
                connectedAccountGoogle.setIsConnected(isGoogleConnected);
                if (isGoogleConnected) {
                    // default calendar id
                    Map listAllCalendarsResponse = dispatcher.runSync("listAllCalendars",
                            UtilMisc.toMap("partyId", loggedInUser.getPartyId(), "userLogin", loggedInUser.getUserLogin()));
                    if (ServiceUtil.isSuccess(listAllCalendarsResponse)) {
                        List<Map<String, Object>> calendars =
                                (List<Map<String, Object>>) listAllCalendarsResponse.get("calendars");
                        if (UtilValidate.isNotEmpty(calendars)) {
                            for (Map<String, Object> calendarRec : calendars) {
                                Boolean isPrimaryCalendar = (Boolean) calendarRec.get("primary");
                                if (isPrimaryCalendar != null && isPrimaryCalendar) {
                                    connectedAccountGoogle.setDefaultCalendarId((String) calendarRec.get("id"));
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        } catch (GenericServiceException e) {
            e.printStackTrace();
        }
        loggedInUserDetails.setConnectedAccounts(ConnectedAccounts.builder()
                .google(connectedAccountGoogle)
                .build());

        return ApiResponseUtil.prepareOkResponse(loggedInUserDetails);
    }

    private static Workspace getLastWorkspaceInfo(GenericDelegator delegator, String partyId) {
        Workspace workspace = new Workspace();
        String workspaceId = "";
        GenericValue workspaceAssoc = null;
        try {
            workspaceAssoc =
                    EntityQuery.use(delegator)
                            .from("WorkspaceAssoc")
                            .where("createdByUserLogin", partyId)
                            .orderBy("-lastUpdatedStamp")
                            .queryFirst();

            if (UtilValidate.isNotEmpty(workspaceAssoc)) {
                workspaceId = workspaceAssoc.getString("workspaceId");
                GenericValue workspaceRecord =
                        EntityQuery.use(delegator)
                                .from("Workspace")
                                .where("workspaceId", workspaceId)
                                .queryOne();
                workspace.setWorkspaceId(workspaceId);
                if (UtilValidate.isNotEmpty(workspaceRecord)) {
                    workspace.setName(workspaceRecord.getString("name"));
                    workspace.setCreatedAt(workspaceRecord.getTimestamp("createdStamp"));
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }

        return workspace;
    }

    @Override
    public Response meUpdate(ApplicationUser userDetailsToUpdate, SecurityContext securityContext)
            throws NotFoundException {

        if (Debug.verboseOn()) Debug.logVerbose("Entering method meUpdate", module);

        LoggedInUser loggedInUser = (LoggedInUser) securityContext.getUserPrincipal();
        // Use this tenant delegtor and dispatcher update login details
        GenericDelegator delegator = loggedInUser.getDelegator();
        LocalDispatcher dispatcher = loggedInUser.getDispatcher();

        String partyId = loggedInUser.getPartyId();

        // update person information
        ApplicationUser updatePersonDetails =
                PersonServices.updatePersonDetails(
                        dispatcher, partyId, loggedInUser.getUserLogin(), userDetailsToUpdate);
        // check if id exists for phone, if it doesn't, we need to add a new phone

        // phone doesn't exist already, let's add one
        /*if (UtilValidate.isNotEmpty(userDetailsToUpdate.getPersonalDetails())) {
            Phone phoneToCreate = userDetailsToUpdate.getPersonalDetails().getPrimaryPhone();
            if (UtilValidate.isNotEmpty(phoneToCreate)
                    && UtilValidate.isNotEmpty(phoneToCreate.getPhone())
                    && UtilValidate.isEmpty(phoneToCreate.getId())) {
                partyContactDetailsApiService.createPartyPhone(partyId, phoneToCreate, securityContext);
            } else if (UtilValidate.isNotEmpty(phoneToCreate)
                    && UtilValidate.isNotEmpty(phoneToCreate.getPhone())
                    && UtilValidate.isNotEmpty(phoneToCreate.getId())) {
                partyContactDetailsApiService.updatePartyPhone(partyId, phoneToCreate, securityContext);
            }
        }*/

        // address doesn't exist already, let's add one
        if (UtilValidate.isNotEmpty(userDetailsToUpdate.getPersonalDetails())) {
            PostalAddress postalAddress = userDetailsToUpdate.getPersonalDetails().getPrimaryAddress();
            if (UtilValidate.isNotEmpty(postalAddress)) {
                String contactMechId =
                        PersonServices.createPersonAddress(
                                partyId, postalAddress, loggedInUser.getUserLogin(), dispatcher);
                if (UtilValidate.isEmpty(contactMechId)) {
                    String message = "Unable to create a address.Please contact support.";
                    Debug.logError(message, module);
                    if (Debug.verboseOn()) Debug.logVerbose("Exiting method createPartyAddress", module);
                    return ApiResponseUtil.prepareDefaultResponse(
                            Response.Status.INTERNAL_SERVER_ERROR, message, module);
                }

                updatePersonDetails.getPersonalDetails().getPrimaryAddress().setId(contactMechId);

                // also update the timezone of the user
            }
        }

        // refresh the self get call
        return meGet(securityContext);
    }

    @Override
    public Response createSubscription(Subscription userSubscription, SecurityContext securityContext)
            throws NotFoundException {
        if (Debug.verboseOn()) Debug.logVerbose("Entering method meUpdate", module);

        return ApiResponseUtil.prepareOkResponse("ok");
    }

    @Override
    public Response connectGoogleAccount(ApplicationUser user, SecurityContext securityContext)
            throws NotFoundException {
        Debug.log("Entered connectGoogleAccount api method", module);

        LoggedInUser loggedInUser = (LoggedInUser) securityContext.getUserPrincipal();
        LocalDispatcher dispatcher = loggedInUser.getDispatcher();
        String serverRootUrl = user.getHostUrl();

        Map<String, Object> connectGoogleAccountCtx = new HashMap<>();
        connectGoogleAccountCtx.put("userLogin", loggedInUser.getUserLogin());
        connectGoogleAccountCtx.put("partyId", loggedInUser.getPartyId());
        connectGoogleAccountCtx.put("serverRootUrl", serverRootUrl);

        Map<String, Object> connectGoogleAccountCtxResp = null;
        try {
            connectGoogleAccountCtxResp =
                    dispatcher.runSync("initializeGoogleAuth", connectGoogleAccountCtx);
        } catch (GenericServiceException e) {
            // handle error here
            Debug.logError(
                    "An error occurred while invoking initializeGoogleAuth service, details: "
                            + e.getMessage(),
                    module);
            if (Debug.verboseOn()) Debug.logVerbose("Exiting method connectGoogleAccount", module);

            return ApiResponseUtil.prepareDefaultResponse(
                    Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }

        if (ServiceUtil.isError(connectGoogleAccountCtxResp)) {
            Debug.logError(
                    "An error occurred while invoking initializeGoogleAuth service, details: "
                            + ServiceUtil.getErrorMessage(connectGoogleAccountCtxResp),
                    module);
            if (Debug.verboseOn()) {
                Debug.logVerbose("Exiting method connectGoogleAccount", module);
            }

            return ApiResponseUtil.prepareDefaultResponse(
                    Response.Status.INTERNAL_SERVER_ERROR,
                    ServiceUtil.getErrorMessage(connectGoogleAccountCtxResp));
        }

        if (Debug.verboseOn()) Debug.logVerbose("Exiting method connectGoogleAccount", module);
        Debug.log("Response from the initializeGoogleAuth: " + connectGoogleAccountCtxResp, module);
        return ApiResponseUtil.prepareOkResponse(connectGoogleAccountCtxResp);
    }

    @Override
    public Response isGoogleAuthorized(SecurityContext securityContext) throws NotFoundException {

        LoggedInUser loggedInUser = (LoggedInUser) securityContext.getUserPrincipal();
        LocalDispatcher dispatcher = loggedInUser.getDispatcher();

        Map<String, Object> isGoogleAuthorizedCtx = new HashMap<>();
        isGoogleAuthorizedCtx.put("userLogin", loggedInUser.getUserLogin());
        isGoogleAuthorizedCtx.put("partyId", loggedInUser.getPartyId());

        Map<String, Object> isGoogleAuthorizedCtxResp = null;
        try {
            isGoogleAuthorizedCtxResp =
                    dispatcher.runSync("checkIfGoogleAuthorized", isGoogleAuthorizedCtx);
        } catch (GenericServiceException e) {
            // handle error here
            Debug.logError(
                    "An error occurred while invoking initializeGoogleAuth service, details: "
                            + e.getMessage(),
                    module);
            if (Debug.verboseOn()) Debug.logVerbose("Exiting method isGoogleAuthorized", module);

            return ApiResponseUtil.prepareDefaultResponse(
                    Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }

        if (ServiceUtil.isError(isGoogleAuthorizedCtxResp)) {
            Debug.logError(
                    "An error occurred while invoking initializeGoogleAuth service, details: "
                            + ServiceUtil.getErrorMessage(isGoogleAuthorizedCtxResp),
                    module);
            if (Debug.verboseOn()) {
                Debug.logVerbose("Exiting method isGoogleAuthorized", module);
            }

            return ApiResponseUtil.prepareDefaultResponse(
                    Response.Status.INTERNAL_SERVER_ERROR,
                    ServiceUtil.getErrorMessage(isGoogleAuthorizedCtxResp));
        }

        if (Debug.verboseOn()) Debug.logVerbose("Exiting method isGoogleAuthorized", module);

        return ApiResponseUtil.prepareOkResponse(isGoogleAuthorizedCtxResp);
    }

    @Override
    public Response disconnectGoogleAuth(SecurityContext securityContext) throws NotFoundException {

        LoggedInUser loggedInUser = (LoggedInUser) securityContext.getUserPrincipal();
        LocalDispatcher dispatcher = loggedInUser.getDispatcher();

        Map<String, Object> disconnectGoogleAuthCtx = new HashMap<>();
        disconnectGoogleAuthCtx.put("userLogin", loggedInUser.getUserLogin());
        disconnectGoogleAuthCtx.put("partyId", loggedInUser.getPartyId());

        Map<String, Object> disconnectGoogleAuthCtxResp = null;
        try {
            disconnectGoogleAuthCtxResp =
                    dispatcher.runSync("disconnectGoogleAuth", disconnectGoogleAuthCtx);
        } catch (GenericServiceException e) {
            // handle error here
            Debug.logError(
                    "An error occurred while invoking disconnectGoogleAuth service, details: "
                            + e.getMessage(),
                    module);
            if (Debug.verboseOn()) Debug.logVerbose("Exiting method disconnectGoogleAuth", module);

            return ApiResponseUtil.prepareDefaultResponse(
                    Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }

        if (ServiceUtil.isError(disconnectGoogleAuthCtxResp)) {
            Debug.logError(
                    "An error occurred while invoking disconnectGoogleAuth service, details: "
                            + ServiceUtil.getErrorMessage(disconnectGoogleAuthCtxResp),
                    module);
            if (Debug.verboseOn()) {
                Debug.logVerbose("Exiting method disconnectGoogleAuth", module);
            }

            return ApiResponseUtil.prepareDefaultResponse(
                    Response.Status.INTERNAL_SERVER_ERROR,
                    ServiceUtil.getErrorMessage(disconnectGoogleAuthCtxResp));
        }

        if (Debug.verboseOn()) Debug.logVerbose("Exiting method disconnectGoogleAuth", module);

        return ApiResponseUtil.prepareOkResponse(disconnectGoogleAuthCtxResp);
    }

    @Override
    public Response disconnectSlackAuth(SecurityContext securityContext) throws NotFoundException {
        LoggedInUser loggedInUser = (LoggedInUser) securityContext.getUserPrincipal();
        LocalDispatcher dispatcher = loggedInUser.getDispatcher();

        Map<String, Object> disconnectGoogleAuthCtx = new HashMap<>();
        disconnectGoogleAuthCtx.put("userLogin", loggedInUser.getUserLogin());
        disconnectGoogleAuthCtx.put("accountPartyId", loggedInUser.getAccountPartyId());
        Map<String, Object> disconnectSlackResp = null;

        try {
            disconnectSlackResp = dispatcher.runSync("disconnectSlackAuth", disconnectGoogleAuthCtx);
        } catch (GenericServiceException e) {
            // handle error here
            Debug.logError("An error occurred while invoking disconnectSlackAuth service, details: " + e.getMessage(), module);
            if (Debug.verboseOn()) Debug.logVerbose("Exiting method disconnectSlackAuth", module);
            return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }
        if (ServiceUtil.isError(disconnectSlackResp)) {
            Debug.logError(
                    "An error occurred while invoking disconnectSlackAuth service, details: " + ServiceUtil.getErrorMessage(disconnectSlackResp), module);
            if (Debug.verboseOn()) {
                Debug.logVerbose("Exiting method disconnectGoogleAuth", module);
            }
            return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR, ServiceUtil.getErrorMessage(disconnectSlackResp));
        }
        return ApiResponseUtil.prepareOkResponse(disconnectSlackResp);
    }

    /**
     * Send a test message to logged-in user to verify slack integration
     *
     * @param securityContext
     * @return
     */
    @Override
    public Response sendSlackTestMessage(SecurityContext securityContext) {
        LoggedInUser loggedInUser = (LoggedInUser) securityContext.getUserPrincipal();
        LocalDispatcher dispatcher = loggedInUser.getDispatcher();

        Map<String, Object> sendSlackMessageCtx = new HashMap<>();
        sendSlackMessageCtx.put("userLogin", loggedInUser.getUserLogin());
        sendSlackMessageCtx.put("messageToSend", "This is a test message to verify Slack Integration, at: " + new Date());
        sendSlackMessageCtx.put("sendToUserWithEmail", loggedInUser.getUserLoginId());
        Map<String, Object> disconnectSlackResp = null;
        try {
            dispatcher.runSync("sendMessageViaSlack", sendSlackMessageCtx);
        } catch (GenericServiceException e) {
            // handle error here
            Debug.logError("An error occurred while send Slack test message, details: " + e.getMessage(), module);
            if (Debug.verboseOn()) Debug.logVerbose("Exiting method sendSlackTestMessage", module);
            return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }
        return ApiResponseUtil.prepareOkResponse("Test Message Sent");
    }

    private static Subscription prepareSubscription(LoggedInUser loggedInUser) throws Exception {
        Subscription subscription = new Subscription();
        GenericDelegator delegator = loggedInUser.getDelegator();
        LocalDispatcher dispatcher = loggedInUser.getDispatcher();

        // billing and subscription handling

        // default subscription needed - unless we find subscriptions
        subscription.setNeedsSubscription(true);
        subscription.setActive(false);
        subscription.setPromptUpgrade(true); // set this to false, if user is already at top plan

        // Retrieve Active Subscriptions
        String stripeCustomerId;

        String accountPartyId = loggedInUser.getAccountPartyId();
        try {
            GenericValue orgParty =
                    delegator.findOne("Party", UtilMisc.toMap("partyId", accountPartyId), false);
            stripeCustomerId = orgParty.getString("externalId");
        } catch (GenericEntityException e) {
            Debug.logError(e.getMessage(), module);
            throw new Exception(
                    "Unable to retrieve account's stripe customer . Error: " + e.getMessage());
        }

        if (UtilValidate.isEmpty(stripeCustomerId)) {
            return subscription;
        }

        /*List<com.stripe.model.Subscription> stripeActiveSubscriptions = null;
        if (UtilValidate.isNotEmpty(stripeCustomerId)) {
            subscription.setCustomerId(stripeCustomerId);

            try {
                Map<String, Object> stripeSubscriptionsResp =
                        dispatcher.runSync(
                                "retrieveSubscriptionsFromStripe",
                                UtilMisc.toMap(
                                        "stripeCustomerId",
                                        stripeCustomerId,
                                        "subscriptionStatus",
                                        "active",
                                        "userLogin",
                                        HierarchyUtils.getSysUserLogin(delegator)));

                stripeActiveSubscriptions =
                        (List<com.stripe.model.Subscription>)
                                stripeSubscriptionsResp.get("stripeSubscriptions");
                if (!ServiceUtil.isSuccess(stripeSubscriptionsResp)) {
                    Debug.logError(
                            "Problem retrieving subscriptions from Stripe. Resp: " + stripeSubscriptionsResp,
                            module);
                    throw new Exception(
                            "Problem retrieving subscriptions from Stripe. Resp: " + stripeSubscriptionsResp);
                }
            } catch (GenericServiceException e) {
                Debug.logError(
                        "Problem retrieving subscriptions from Stripe. Error: " + e.getMessage(), module);
                e.printStackTrace();
                throw new Exception(
                        "Unable to retrieve subscriptions from Stripe. Error: " + e.getMessage());
            }
        }

        if (UtilValidate.isNotEmpty(stripeActiveSubscriptions)) {
            com.stripe.model.Subscription stripeSubscription = stripeActiveSubscriptions.get(0);
            subscription.setActive(true);
            subscription.setNeedsSubscription(false);

            subscription.setSubscriptionStartDate(
                    new Timestamp(stripeSubscription.getCurrentPeriodStart() * 1000));
            subscription.setSubscriptionEndDate(
                    new Timestamp(stripeSubscription.getCurrentPeriodEnd() * 1000));
            subscription.setSubscriptionId(stripeSubscription.getId());
            subscription.setCancelAtEndOfPeriod(stripeSubscription.getCancelAtPeriodEnd());

            String subscribedPlanId = stripeSubscription.getPlan().getId();
            subscription.setBillingPlanId(subscribedPlanId);

            BillingPlans plan = BillingPlans.fromValue(subscribedPlanId);
            subscription.setBillingPlanId(subscribedPlanId);

            if (UtilValidate.isNotEmpty(plan)) {
                subscription.setPromptUpgradeToPlanId(plan.getUpgradeToPlanId());

                subscription.setBillingPlanPrice(plan.getPlanPrice());
                subscription.setMaxUsersAllowed(plan.getMaxUsers());

                subscription.setBillingPlanPrice(plan.getPlanPrice());
                subscription.setBillingPlanName(plan.getPlanName());
                subscription.setBillingPlanDetails(plan.getPlanDetails());
                subscription.setBillingPlanfeatures(plan.getFeatures());
                subscription.setBillingFrequency(stripeSubscription.getPlan().getInterval());

                String interval = stripeSubscription.getPlan().getInterval();
                if ("month".equalsIgnoreCase(interval)) {
                    subscription.setBillingFrequency(
                            BillingPlans.BillingFrequency.PER_MONTH.getBillingFrequency());
                } else {
                    subscription.setBillingFrequency(
                            BillingPlans.BillingFrequency.PER_YEAR.getBillingFrequency());
                }
            }
            if (stripeSubscription.getCancelAtPeriodEnd() != null
                    && stripeSubscription.getCancelAtPeriodEnd()) {
                subscription.setDelayedCancelAt(new Date(stripeSubscription.getCancelAt() * 1000));
            }
            boolean isUserLicenseActive =
                    AccountUtils.isUserLicenseActive(delegator, dispatcher, loggedInUser.getAccountPartyId());
            subscription.setHasActiveLicense(isUserLicenseActive);
        }*/

        // set available and all billing plans
        List<BillingPlans> billingPlans = Arrays.asList(BillingPlans.values());
        subscription.setAllPlans(billingPlans);

        // list of invoices due
        List<BillingInvoice> dueInvoices = new ArrayList<>();
        subscription.setDueInvoices(dueInvoices);

        return subscription;
    }

    public static ApplicationUser getUserDetails(
            String partyId,
            GenericValue userLogin,
            GenericDelegator delegator,
            LocalDispatcher dispatcher) {
        ApplicationUser loggedInUserDetails = new ApplicationUser();
        loggedInUserDetails.setId(partyId);

        // prepare person information
        Person person =
                PersonServices.getPersonDetails(delegator, dispatcher, partyId, userLogin, true, false);
        loggedInUserDetails.setPersonalDetails(person);

        boolean isUserEmailVerified =
                AxPartyHelper.isEmailVerified(delegator, userLogin.getString("userLoginId"), partyId);
        loggedInUserDetails.setEmailVerified(isUserEmailVerified);

        return loggedInUserDetails;
    }
}
