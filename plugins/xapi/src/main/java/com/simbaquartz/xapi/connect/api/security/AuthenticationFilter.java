/*
 *
 *  * *****************************************************************************************
 *  *  Copyright (c) SimbaQuartz  2016. - All Rights Reserved                                 *
 *  *  Unauthorized copying of this file, via any medium is strictly prohibited               *
 *  *  Proprietary and confidential                                                           *
 *  *  Written by Mandeep Sidhu <mandeep.sidhu@fidelissd.com>,  May, 2017                    *
 *  * ****************************************************************************************
 *
 */

package com.simbaquartz.xapi.connect.api.security;

import com.fidelissd.zcp.xcommon.collections.FastMap;
import com.fidelissd.zcp.xcommon.enums.AccountUserRoleTypesEnum;
import com.fidelissd.zcp.xcommon.util.InvalidTokenException;
import com.fidelissd.zcp.xcommon.util.JWTUtils;
import com.fidelissd.zcp.xcommon.util.hierarchy.HierarchyUtils;
import com.simbaquartz.xapi.connect.api.common.ApiMessageConstants;
import com.simbaquartz.xapi.connect.utils.ApiResponseUtil;
import com.simbaquartz.xparty.helpers.AxPartyHelper;
import com.simbaquartz.xparty.hierarchy.PartyGroupForPartyUtils;
import org.apache.ofbiz.base.util.*;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.entity.GenericDelegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityConditionList;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.service.GenericDispatcherFactory;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.*;

/**
 * Filters all incoming api calls to check for a valid Authorization header. Follows the below
 * pattern while validating provided authorization header; Bearer USER_LOGIN_ID:PARTY_ID
 */
@Secured
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {
    private static final String module = AuthenticationFilter.class.getName();
    private static final String ACCESSTOKEN = "AccessToken";
    private static GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
    private static LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);
    private static int TOKEN_INACTIVITY_MINS = UtilProperties.getPropertyAsInteger("xapi.properties", "xapi.thru.date.token", 0);

    @Override
    @Produces("application/json")
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (!requestContext.getHeaders().containsKey(ACCESSTOKEN)) {
            String pathRequested = requestContext.getUriInfo().getRequestUri().getPath();
            Debug.logError("Access Token header key is missing for requested URL: " + pathRequested, module);
            requestContext.abortWith(ApiResponseUtil.prepareDefaultResponse(Response.Status.UNAUTHORIZED, ApiMessageConstants.MSG_MISSING_ACCESS_TOKEN_KEY));
            return;
        }
        String accessToken = requestContext.getHeaderString(ACCESSTOKEN);
        if (UtilValidate.isEmpty(accessToken)) {
            Debug.logError("accessToken header value is missing", module);
            requestContext.abortWith(ApiResponseUtil.prepareDefaultResponse(Response.Status.UNAUTHORIZED, ApiMessageConstants.MSG_MISSING_ACCESS_TOKEN_VALUE));
            return;
        }

        try {
            byte[] decodedAccessTokenBytes = Base64.getDecoder().decode(accessToken.getBytes());
            String decodedAccessToken = new String(decodedAccessTokenBytes);

            Map<String, Object> jwtMap = JWTUtils.parseJwt(decodedAccessToken);
            if (UtilValidate.isEmpty(jwtMap)) {
                Debug.logError("Unable to authorise the logged in user. Please validate the access token header value.", module);
                requestContext.abortWith(ApiResponseUtil.prepareDefaultResponse(Response.Status.UNAUTHORIZED, ApiMessageConstants.MSG_VALID_ACCESS_TOKEN_VALUE));
                return;
            }
            String tokenType = (String) jwtMap.get("tokenType");
            if (UtilValidate.isEmpty(tokenType) || !"ACCESS".equalsIgnoreCase(tokenType)) {
                Debug.logError("Invalid Access Token, please ensure you are not passing in a refresh token instead.", module);
                requestContext.abortWith(ApiResponseUtil.prepareDefaultResponse(Response.Status.UNAUTHORIZED, ApiMessageConstants.MSG_VALID_ACCESS_TOKEN_VALUE));
                return;
            }
            Debug.logVerbose("The access token is active.", module);
            String userLoginId = (String) jwtMap.get("userLoginId");
            String tenantId = (String) jwtMap.get("tenantId");
            String accessTokenUuid = (String) jwtMap.get("tokenUuid");
            Boolean isAssumed = (Boolean) jwtMap.get("isAssumed");
            String assumedBy = (String) jwtMap.get("assumedBy");

            GenericDelegator tenantDelegator = delegator;
            LocalDispatcher tenantDispatcher = dispatcher;
            if (EntityUtil.isMultiTenantEnabled() && UtilValidate.isNotEmpty(tenantId)) {
                tenantDelegator = (GenericDelegator) DelegatorFactory.getDelegator("default#" + tenantId);
                if (UtilValidate.isEmpty(tenantDelegator)) {
                    Debug.logError("Invalid tenantId found " + tenantId, module);
                    requestContext.abortWith(ApiResponseUtil.prepareDefaultResponse(Response.Status.UNAUTHORIZED, ApiMessageConstants.MSG_VALID_ACCESS_TOKEN_VALUE));
                }
                tenantDispatcher = new GenericDispatcherFactory().createLocalDispatcher("default#" + tenantId, tenantDelegator);
            }

            // Check if token is still valid
            boolean isTokenUuidValid = checkIfAccessTokenUuidIsValid(tenantDelegator, userLoginId, accessTokenUuid);
            if (!isTokenUuidValid) {
                Debug.logError("Access Token is no longer valid.", module);
                requestContext.abortWith(ApiResponseUtil.prepareDefaultResponse(Response.Status.UNAUTHORIZED, ApiMessageConstants.MSG_VALID_ACCESS_TOKEN_VALUE));
                return;
            }
            updateTokenExpirationTime(tenantDelegator, userLoginId, accessTokenUuid);

            GenericValue userLogin =
                    EntityQuery.use(tenantDelegator)
                            .from("UserLogin")
                            .where("userLoginId", userLoginId)
                            .queryFirst();
            List<EntityExpr> conds = new LinkedList<>();
            String loggedInUserPartyId = userLogin.getString("partyId");
            conds.add(
                    EntityCondition.makeCondition(
                            "partyIdTo", EntityOperator.EQUALS, loggedInUserPartyId));
            conds.add(
                    EntityCondition.makeCondition(
                            EntityCondition.makeCondition(
                                    "partyRelationshipTypeId", EntityOperator.EQUALS, "EMPLOYMENT"),
                            EntityOperator.OR,
                            EntityCondition.makeCondition(
                                    "partyRelationshipTypeId", EntityOperator.EQUALS, "OWNER")));
            List<EntityExpr> exprs = UtilMisc.toList(conds);

            Map<String, Object> getPartyOrganizationForPartyIdCtx = FastMap.newInstance();
            getPartyOrganizationForPartyIdCtx.put("userLogin", userLogin);
            getPartyOrganizationForPartyIdCtx.put("partyId", loggedInUserPartyId);
            GenericValue loggedInPartiesPartyGroup =
                    PartyGroupForPartyUtils.getPartyGroupForPartyId(
                            HierarchyUtils.getPartyByPartyId(delegator, loggedInUserPartyId));

            String orgGroupPartyId = "";
            if (UtilValidate.isNotEmpty(loggedInPartiesPartyGroup)) {
                orgGroupPartyId = loggedInPartiesPartyGroup.getString("partyId");
            }

            // check if the account has a store associated, if so return the store details as well, if not create one.
            //String storeId = StoreHelper.getStoreForPartyId(delegator, orgGroupPartyId);
            String storeId = null; // TODO: to be checked above line
            if (UtilValidate.isEmpty(storeId)) {
                Map createProductStoreResponse = null;
                try {
                    createProductStoreResponse = dispatcher.runSync("axCreateProductStoreForAccount", UtilMisc
                            .toMap("accountId", orgGroupPartyId));
                } catch (GenericServiceException e) {
                    Debug.logError(e, module);
                }

                if (!ServiceUtil.isError(createProductStoreResponse)) {
                    storeId = (String) createProductStoreResponse.get("storeId");
                }
            }

            prepareUserInfo(
                    requestContext,
                    tenantDispatcher,
                    tenantDelegator,
                    userLogin.getString("userLoginId"),
                    userLogin.getString("partyId"),
                    orgGroupPartyId,
                    storeId,
                    userLogin,
                    accessToken,
                    isAssumed,
                    assumedBy);


        } catch (IllegalArgumentException | InvalidTokenException e) {
            Debug.logError(e, "Unable to authorise the logged in user. Please validate the access token header value.", module);
            requestContext.abortWith(ApiResponseUtil.prepareDefaultResponse(Response.Status.UNAUTHORIZED, ApiMessageConstants.MSG_VALID_ACCESS_TOKEN_VALUE));
        } catch (GenericEntityException e) {
            Debug.logError(e, "An Error occurred while trying to validate the access token: " + e.getMessage(), module);
        }
    }

    /**
     * Add mins (inactivity) to access token thruDate since user performed some action
     *
     * @param tenantDelegator
     * @param userLoginId
     * @param accessTokenUuid
     */
    private void updateTokenExpirationTime(
            GenericDelegator tenantDelegator, String userLoginId, String accessTokenUuid) {
        // Add x mins (eg. 30) to accessToken thruDate ...
        try {
            GenericValue accessTokenGv =
                    tenantDelegator.findOne(
                            "AccessToken",
                            UtilMisc.toMap("userLoginId", userLoginId, "accessToken", accessTokenUuid),
                            false);
            if (UtilValidate.isNotEmpty(accessTokenGv)) {
                Timestamp currentDate = UtilDateTime.nowTimestamp();

                // Add minutes to the currentDate
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(currentDate.getTime());
                cal.add(Calendar.MINUTE, TOKEN_INACTIVITY_MINS);
                Timestamp thruDate = new Timestamp(cal.getTime().getTime());

                accessTokenGv.set("thruDate", thruDate);
                tenantDelegator.store(accessTokenGv);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
    }

    public static boolean checkIfAccessTokenUuidIsValid(
            GenericDelegator tenantDelegator, String userLoginId, String accessTokenUuid) {
        EntityConditionList<EntityExpr> mainCond =
                EntityCondition.makeCondition(
                        UtilMisc.toList(
                                EntityCondition.makeCondition(
                                        "accessToken", EntityOperator.EQUALS, accessTokenUuid),
                                EntityCondition.makeCondition("isActive", EntityOperator.EQUALS, "Y"),
                                EntityCondition.makeCondition(
                                        "thruDate", EntityOperator.GREATER_THAN_EQUAL_TO, UtilDateTime.nowTimestamp()),
                                EntityCondition.makeCondition("userLoginId", EntityOperator.EQUALS, userLoginId)),
                        EntityOperator.AND);
        try {
            List<GenericValue> activeTokens =
                    EntityQuery.use(tenantDelegator).from("AccessToken").where(mainCond).queryList();
            if (UtilValidate.isNotEmpty(activeTokens)) {
                return true;
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return false;
    }

    public static void prepareUserInfo(
            ContainerRequestContext requestContext,
            LocalDispatcher tenantDispatcher,
            GenericDelegator tenantDelegator,
            String username,
            String partyId,
            String orgGroupPartyId,
            String storeId,
            GenericValue userLogin,
            String accessToken,
            Boolean isAssumed,
            String assumedBy) {
        // set the user provider
        Map userBasicDetails = AxPartyHelper.getPartyBasicDetails(tenantDelegator, partyId);
        Locale userLocale = requestContext.getLanguage();
        if (UtilValidate.isEmpty(userLocale)) {
            Debug.logWarning(
                    "Locale is null. Proper locale should be passed. Switching to Default", module);
            userLocale = Locale.getDefault();
        }

        // populate role for logged in user
        // Get Party Top Level Permission
        Map<String, Object> getAppUserPermissionResponse = null;
        try {
            getAppUserPermissionResponse =
                    dispatcher.runSync(
                            "getAppUserPermission", UtilMisc.toMap("userLogin", userLogin, "accountId", partyId));
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
        }

        List<GenericValue> permissions =
                (List<GenericValue>) getAppUserPermissionResponse.get("permissions");

        String loggedInUserRole = "";

        if (UtilValidate.isNotEmpty(permissions)) {
            String getAppUserPermissionResponseRoleId = permissions.get(0).getString("groupId");
            if (UtilValidate.isNotEmpty(getAppUserPermissionResponseRoleId)) {
                // get from enum
                if (UtilValidate.areEqual(
                        AccountUserRoleTypesEnum.ADMIN.getRole(), getAppUserPermissionResponseRoleId)) {
                    // user wants to change the permissions to admin
                    loggedInUserRole = AccountUserRoleTypesEnum.ADMIN.getRoleName();
                } else if (UtilValidate.areEqual(
                        AccountUserRoleTypesEnum.MANAGER.getRole(), getAppUserPermissionResponseRoleId)) {
                    // user wants to change the permissions to manager
                    loggedInUserRole = AccountUserRoleTypesEnum.MANAGER.getRoleName();
                } else if (UtilValidate.areEqual(
                        AccountUserRoleTypesEnum.MEMBER.getRole(), getAppUserPermissionResponseRoleId)) {
                    // user wants to change the permissions to member
                    loggedInUserRole = AccountUserRoleTypesEnum.MEMBER.getRoleName();
                }
            }
        }

        String loggedInUserDisplayName = (String) userBasicDetails.get("displayName");
        String loggedInUserEmail = (String) userBasicDetails.get("email");
        String loggedInUserPhotoUrl = (String) userBasicDetails.get("photoUrl");
        LoggedInUser loggedInUser =
                new LoggedInUser(
                        userLogin,
                        loggedInUserDisplayName,
                        loggedInUserEmail,
                        loggedInUserPhotoUrl,
                        username,
                        partyId,
                        orgGroupPartyId,
                        storeId,
                        loggedInUserRole,
                        userLocale,
                        tenantDispatcher,
                        tenantDelegator);
        loggedInUser.setTenantId(tenantDelegator.getDelegatorTenantId());
        // set server's url
        String serverHostNameWithProtocol =
                requestContext.getUriInfo().getBaseUri().getScheme()
                        + "://"
                        + requestContext.getUriInfo().getBaseUri().getHost();
        loggedInUser.setServerHost(serverHostNameWithProtocol);
        loggedInUser.setAccessToken(accessToken);

        // for setting user information in the context
        final SecurityContext currentSecurityContext = requestContext.getSecurityContext();
        requestContext.setSecurityContext(
                new ApiSecurityContext(loggedInUser, currentSecurityContext.isSecure(), "Bearer"));
    }
}
