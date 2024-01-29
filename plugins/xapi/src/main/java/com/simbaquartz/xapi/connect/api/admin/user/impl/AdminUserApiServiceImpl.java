package com.simbaquartz.xapi.connect.api.admin.user.impl;

import com.fidelissd.zcp.xcommon.util.hierarchy.HierarchyUtils;
import com.simbaquartz.xapi.connect.api.NotFoundException;
import com.simbaquartz.xapi.connect.api.admin.user.AdminUserApiService;
import com.simbaquartz.xapi.connect.api.common.ApiMessageConstants;
import com.simbaquartz.xapi.connect.api.security.LoggedInUser;
import com.simbaquartz.xapi.connect.utils.ApiResponseUtil;
import com.simbaquartz.xapi.connect.models.Userlogin;
import com.simbaquartz.xapi.connect.validation.EmailValidator;
import com.fidelissd.zcp.xcommon.collections.FastMap;
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
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.ServiceUtil;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.Map;

public class AdminUserApiServiceImpl extends AdminUserApiService {

    private static final String module = AdminUserApiServiceImpl.class.getName();


    @Override
    public Response createAdminUser(Userlogin userlogin, SecurityContext securityContext) throws NotFoundException {
        if (Debug.verboseOn())
            Debug.logVerbose("Entering method createAdminUser", module);

        LoggedInUser loggedInUser = (LoggedInUser) securityContext.getUserPrincipal();
        Map <String, Object> registerUserContext = FastMap.newInstance();

        Delegator localDelegator = DelegatorFactory.getDelegator("default");
        registerUserContext.put("userLogin", HierarchyUtils.getSysUserLogin(localDelegator));

        if (validateUserLoginId(userlogin))
            return ApiResponseUtil.prepareDefaultResponse(Response.Status.FORBIDDEN, "UserLoginId Already Exists.");

        if(UtilValidate.isNotEmpty(userlogin.getEmailAddress()) && !EmailValidator.isValidEmail(userlogin.getEmailAddress())) {
            return ApiResponseUtil.prepareDefaultResponse(Response.Status.BAD_REQUEST, ApiMessageConstants.MSG_INVALID_EMAIL_FORMAT + userlogin.getEmailAddress());
        }

        String sha1AdminPassword = HashCrypt.cryptUTF8(LoginServices.getHashType(), null, userlogin.getPassword());
        String partyId = null;
        try {
            partyId = createAdminUserLogin(userlogin.getEmailAddress(), sha1AdminPassword, userlogin.getFirstName(), userlogin.getLastName());
        } catch (GenericServiceException | GenericEntityException e) {
            //handle error here
            Debug.logError("An error occurred while invoking registerUser service, details: " + e.getMessage(), module);
            if (Debug.verboseOn())
                Debug.logVerbose("Exiting method createAdminUser", module);

            return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR);
        }
        return ApiResponseUtil.prepareOkResponse(UtilMisc.toMap("partyId", partyId));
    }

    private boolean validateUserLoginId(Userlogin userlogin) {
        if(UtilValidate.isNotEmpty(userlogin.getEmailAddress())) {
            try {
                GenericValue existingUserLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", userlogin.getEmailAddress()).queryOne();
                if(UtilValidate.isNotEmpty(existingUserLogin))
                {
                    return true;
                }
            }catch (GenericEntityException ex)
            {
                ex.printStackTrace();
            }
        }
        return false;
    }

    /** Create an Admin User Login entry in Main DB */
    private String createAdminUserLogin(String userLoginId, String password,
                                        String firstName, String lastName) throws GenericServiceException, GenericEntityException {
        String partyId = null;
        try {
            Map <String, Object> createPersonCtx = UtilMisc.toMap();
            createPersonCtx.put("firstName", firstName);
            createPersonCtx.put("lastName", lastName);
            createPersonCtx.put("userLogin", HierarchyUtils.getSysUserLogin(delegator));
            Map <String, Object> createPersonResp = dispatcher.runSync("createPerson", createPersonCtx);
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
            adminUserLoginSecGroup.setString("groupId", "SUPER");
            adminUserLoginSecGroup.set("fromDate", UtilDateTime.nowTimestamp());
            delegator.create(adminUserLoginSecGroup);
        } catch (GenericEntityException | GenericServiceException e) {
            e.printStackTrace();
            throw e;
        }
        return partyId;
    }
}
