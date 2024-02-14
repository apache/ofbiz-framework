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

import com.fidelissd.zcp.xcommon.util.InvalidTokenException;
import com.fidelissd.zcp.xcommon.util.JWTUtils;
import com.simbaquartz.xapi.connect.api.common.ApiMessageConstants;
import com.simbaquartz.xapi.connect.utils.ApiResponseUtil;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.entity.GenericDelegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.GenericDispatcherFactory;
import org.apache.ofbiz.service.LocalDispatcher;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * Filters all incoming api calls to check for a valid Authorization header.
 * Also checks if the user belongs to FSD (main db)
 * To be used for Admin Module
 */
@AdminSecured
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AdminAuthenticationFilter implements ContainerRequestFilter {
    private final String module = AdminAuthenticationFilter.class.getName();
    private static final String ACCESSTOKEN = "AccessToken";

    private static GenericDelegator delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
    private static LocalDispatcher dispatcher = new GenericDispatcherFactory().createLocalDispatcher("default", delegator);

    @Override
    @Produces("application/json")
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (!requestContext.getHeaders().containsKey(ACCESSTOKEN)) {
            Debug.logError("accessToken header key is missing", module);
            requestContext.abortWith(ApiResponseUtil.prepareDefaultResponse(Response.Status.UNAUTHORIZED, ApiMessageConstants.MSG_MISSING_ACCESS_TOKEN_KEY));
        }
        String accessToken = requestContext.getHeaderString(ACCESSTOKEN);
        if (UtilValidate.isEmpty(accessToken)) {
            Debug.logError("accessToken header value is missing", module);
            requestContext.abortWith(ApiResponseUtil.prepareDefaultResponse(Response.Status.UNAUTHORIZED, ApiMessageConstants.MSG_MISSING_ACCESS_TOKEN_VALUE));
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
            } else {
                Debug.logVerbose("The access token is active.", module);
                String userLoginId = (String) jwtMap.get("userLoginId");
                // Verify if userLoginId belongs to main db (FSD)
                // TODO: Check if logged-in user is FSD employee & and has ONBOARD_ADMIN or APP_FULL_ADMIN permission

                GenericValue userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", userLoginId).queryFirst();
                if (UtilValidate.isEmpty(userLogin)) {
                    requestContext.abortWith(ApiResponseUtil.prepareDefaultResponse(Response.Status.UNAUTHORIZED, ApiMessageConstants.MSG_VALID_ACCESS_TOKEN_VALUE));
                    return;
                }
                List<EntityExpr> conds = new LinkedList<>();
                conds.add(EntityCondition.makeCondition("partyIdTo", EntityOperator.EQUALS, userLogin.getString("partyId")));
                conds.add(EntityCondition.makeCondition(EntityCondition.makeCondition("partyRelationshipTypeId", EntityOperator.EQUALS, "EMPLOYMENT"),
                        EntityOperator.OR, EntityCondition.makeCondition("partyRelationshipTypeId", EntityOperator.EQUALS, "OWNER")));
                List<EntityExpr> exprs = UtilMisc.toList(conds);
                GenericValue partyRelationshipRecord = EntityQuery.use(delegator).select("partyIdFrom").from("PartyRelationship").where(exprs).queryFirst();
                String orgGroupPartyId = partyRelationshipRecord.getString("partyIdFrom");

                //TODO: if necessary add ADMIN role check
                AuthenticationFilter.prepareUserInfo(requestContext, dispatcher, delegator, userLogin.getString("userLoginId"), userLogin.getString("partyId"), orgGroupPartyId, null, userLogin, accessToken, false, null);
            }
        } catch (IllegalArgumentException | InvalidTokenException e) {
            Debug.logError("Unable to authorise the logged in user. Please validate the access token header value.", module);
            requestContext.abortWith(ApiResponseUtil.prepareDefaultResponse(Response.Status.UNAUTHORIZED, ApiMessageConstants.MSG_VALID_ACCESS_TOKEN_VALUE));
        } catch (GenericEntityException e) {
            Debug.logError("An Error occurred while trying to validate the access token: " + e.getMessage(), module);
        }
    }
}