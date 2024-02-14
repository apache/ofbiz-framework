package com.simbaquartz.xapi.connect.api.admin.auth.impl;

import com.fidelissd.zcp.xcommon.collections.FastMap;
import com.fidelissd.zcp.xcommon.util.hierarchy.HierarchyUtils;
import com.simbaquartz.xapi.connect.api.NotFoundException;
import com.simbaquartz.xapi.connect.api.access.impl.AccessApiServiceImpl.AccessApiCustomErrorMessages;
import com.simbaquartz.xapi.connect.api.admin.auth.AdminAuthApiService;
import com.simbaquartz.xapi.connect.api.common.ApiMessageConstants;
import com.simbaquartz.xapi.connect.api.security.LoggedInUser;
import com.simbaquartz.xapi.connect.models.errors.Error;
import com.simbaquartz.xapi.connect.utils.ApiResponseUtil;
import com.simbaquartz.xparty.helpers.AxPartyHelper;
import org.apache.ofbiz.base.crypto.HashCrypt;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.common.login.LoginServices;
import org.apache.ofbiz.entity.GenericDelegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminAuthApiServiceImpl extends AdminAuthApiService {
    private static final String module = AdminAuthApiServiceImpl.class.getName();
    private static final String API_KEY = "apiKey";

    @Override
    public Response authenticateAdmin(HttpHeaders httpHeaders) throws NotFoundException {
        if (!httpHeaders.getRequestHeaders().containsKey(API_KEY)) {
            Debug.logError("apiKey header value is missing", module);
            return ApiResponseUtil.prepareDefaultResponse(Response.Status.BAD_REQUEST, ApiMessageConstants.MSG_MISSING_APIKEY_VALUE);
        }
        String apiKey = httpHeaders.getRequestHeader(API_KEY).get(0);
        if (UtilValidate.isEmpty(apiKey)) {
            Debug.logError("apiKey header value is missing", module);
            return ApiResponseUtil.prepareDefaultResponse(Response.Status.BAD_REQUEST, ApiMessageConstants.MSG_MISSING_APIKEY_VALUE);
        }
        try {
            // extract userlogin id from the encrypted authentication token
            byte[] bytes = Base64.getDecoder().decode(apiKey.getBytes());

            String userPass = new String(bytes);
            String userName = userPass.substring(0, userPass.indexOf(":"));
            String password = userPass.substring(userPass.indexOf(":") + 1);
            GenericValue userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", userName).cache(true).queryOne();
            if (UtilValidate.isEmpty(userLogin)) {
                String prepareErrorMessage = String.format(ApiMessageConstants.MSG_INVALID_USER_lOGIN_ID, userName);
                Error invalidLoginIdError = new Error(Error.CodeEnum.ACCOUNT_NOT_FOUND, Error.CategoryEnum.AUTHENTICATION_ERROR, prepareErrorMessage, "username");
                List<Error> errors = UtilMisc.toList(invalidLoginIdError);
                return ApiResponseUtil.prepareStandardResponse(Response.Status.UNAUTHORIZED, errors);
            }

            boolean isAuthenticated = checkPassword(userLogin.getString("currentPassword"), true, password);
            if (!isAuthenticated) {
                return ApiResponseUtil.prepareDefaultResponse(Response.Status.BAD_REQUEST, ApiMessageConstants.MSG_INVALID_PASSWORD);
            }
            Map<String, Object> contextMap = new HashMap<String, Object>();
            GenericValue sysUserLogin = HierarchyUtils.getSysUserLogin(delegator);
            contextMap.put("userLoginId", userName);

            // Calling the createAccessToken service
            Map<String, Object> createAccessTokenResponse = dispatcher.runSync("createAccessToken", contextMap);
            String partyId = userLogin.getString("partyId");
            if (UtilValidate.isEmpty(partyId)) {
                return ApiResponseUtil.prepareDefaultResponse(Response.Status.UNAUTHORIZED, ApiMessageConstants.MSG_INVALID_USER_lOGIN_ID);
            }

            Map<String, Object> fsdGetAllPartyDetailsCtx = FastMap.newInstance();
            fsdGetAllPartyDetailsCtx.put("partyId", userLogin.get("partyId"));
            fsdGetAllPartyDetailsCtx.put("userLogin", sysUserLogin);

            Map fsdGetCustomerResponse = dispatcher.runSync("getAllPartyDetails", fsdGetAllPartyDetailsCtx);
            Map partyDetails = (Map) fsdGetCustomerResponse.get("partyDetails");
            createAccessTokenResponse.put("partyId", partyDetails.get("partyId"));
            createAccessTokenResponse.put("partyName", partyDetails.get("partyName"));
            createAccessTokenResponse.put("postalAddresses", partyDetails.get("postalAddresses"));
            createAccessTokenResponse.put("phoneNumbers", partyDetails.get("phoneNumbers"));
            createAccessTokenResponse.put("emailAddress", partyDetails.get("emailAddress"));
            createAccessTokenResponse.put("webAddress", partyDetails.get("webAddress"));
            createAccessTokenResponse.put("publicResourceUrl", partyDetails.get("publicResourceUrl"));
            createAccessTokenResponse.put("thumbNailUrl", partyDetails.get("thumbNailUrl"));
            createAccessTokenResponse.put("organizationPartyId", partyDetails.get("organizationPartyId"));
            String partyInitials = AxPartyHelper.getPartyInitials(delegator, userName, false);
            createAccessTokenResponse.put("partyInitials", partyInitials);
            return ApiResponseUtil.prepareOkResponse(createAccessTokenResponse);
        } catch (Exception e) {
            Debug.logError(e, "An Exception occurred: " + e.getMessage(), module);
            return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private static boolean checkPassword(String oldPassword, boolean useEncryption, String currentPassword) {
        boolean passwordMatches = false;
        if (oldPassword != null) {
            if (useEncryption) {
                passwordMatches = HashCrypt.comparePassword(oldPassword, LoginServices.getHashType(), currentPassword);
            } else {
                passwordMatches = oldPassword.equals(currentPassword);
            }
        }
        if (!passwordMatches && "true".equals(UtilProperties.getPropertyValue("security", "password.accept.encrypted.and.plain"))) {
            passwordMatches = currentPassword.equals(oldPassword);
        }
        return passwordMatches;
    }

    @Override
    public Response createAssumeToken(String accountId, String userLoginId, SecurityContext securityContext) throws NotFoundException {
        Map<String, Object> createAccessTokenResponse = FastMap.newInstance();

        LoggedInUser loggedInUser = (LoggedInUser) securityContext.getUserPrincipal();
        GenericDelegator tenantDelegator = loggedInUser.getDelegator();
        LocalDispatcher tenantDispatcher = loggedInUser.getDispatcher();

        try {

            //pull user login information from database.
            GenericValue userLogin = EntityQuery.use(tenantDelegator).from("UserLogin").where("userLoginId", userLoginId).cache(true).queryOne();
            if (UtilValidate.isEmpty(userLogin)) {
                String prepareErrorMessage = String.format(ApiMessageConstants.INVALID_USER_lOGIN_ID, userLoginId);
                Error invalidLoginIdError = new Error(Error.CodeEnum.ACCOUNT_NOT_FOUND, Error.CategoryEnum.AUTHENTICATION_ERROR, prepareErrorMessage, "userLoginId");
                List<Error> errors = UtilMisc.toList(invalidLoginIdError);
                return ApiResponseUtil.prepareStandardResponse(Response.Status.UNAUTHORIZED, errors);
            }

            if (UtilValidate.isNotEmpty(userLogin)) {
                Map<String, Object> contextMap = new HashMap<String, Object>();
                GenericValue sysUserLogin = HierarchyUtils.getSysUserLogin(tenantDelegator);
                contextMap.put("userLoginId", userLoginId);
                contextMap.put("isAssumed", true);
                contextMap.put("assumedBy", loggedInUser.getUserLoginId());

                // Calling the createAccessToken service
                createAccessTokenResponse = tenantDispatcher.runSync("createAccessToken", contextMap);

                Map<String, Object> getAllPartyDetailsCtx = FastMap.newInstance();

                getAllPartyDetailsCtx.put("partyId", userLogin.get("partyId"));
                getAllPartyDetailsCtx.put("userLogin", sysUserLogin);

                Map getCustomerResponse = null;

                boolean isEmailVerificationComplete =
                        AxPartyHelper.isEmailVerified(delegator, userLoginId, userLogin.getString("partyId"));
                if (!isEmailVerificationComplete) {
                    return ApiResponseUtil.prepareDefaultResponse(
                            Response.Status.UNAUTHORIZED, AccessApiCustomErrorMessages.EMAIL_NOT_VERIFIED);
                }

                Debug.log(
                        "Invoking service getAllPartyDetails with input context" + getAllPartyDetailsCtx,
                        module);
                try {
                    getCustomerResponse = dispatcher.runSync("getAllPartyDetails", getAllPartyDetailsCtx);
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Error while invoking getAllPartyDetails ", module);
                    if (Debug.verboseOn()) Debug.logVerbose("Exiting method createToken", module);

                    return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR);
                }
                if (ServiceUtil.isError(getCustomerResponse)) {
                    String getAllPartyDetailsServiceError =
                            ServiceUtil.getErrorMessage(getCustomerResponse);
                    Debug.logError(
                            "Error while invoking getAllPartyDetails: " + getAllPartyDetailsServiceError,
                            module);
                    if (Debug.verboseOn()) Debug.logVerbose("Exiting method getAllPartyDetails", module);

                    return ApiResponseUtil.prepareDefaultResponse(
                            Response.Status.INTERNAL_SERVER_ERROR, getAllPartyDetailsServiceError);
                }
                Map partyDetails = (Map) getCustomerResponse.get("partyDetails");
                createAccessTokenResponse.put("party", partyDetails);
            } else {
                Debug.logError("Unable to retrieve login details for the logged in user. Please validate the accessToken header value.", module);
                return ApiResponseUtil.prepareDefaultResponse(Response.Status.BAD_REQUEST, ApiMessageConstants.INVALID_USER_lOGIN_ID);
            }
        } catch (Exception e) {
            Debug.logError("An Exception occurred: " + e.getMessage(), module);
            return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }
        return ApiResponseUtil.prepareOkResponse(createAccessTokenResponse);
    }

}
