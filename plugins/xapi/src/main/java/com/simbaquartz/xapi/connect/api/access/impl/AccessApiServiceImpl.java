package com.simbaquartz.xapi.connect.api.access.impl;


import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.simbaquartz.xapi.connect.api.NotFoundException;
import com.simbaquartz.xapi.connect.api.access.AccessApiService;
import com.simbaquartz.xapi.connect.api.common.ApiMessageConstants;
import com.simbaquartz.xapi.connect.api.me.impl.MeApiServiceImpl;
import com.simbaquartz.xapi.connect.api.security.AuthenticationFilter;
import com.simbaquartz.xapi.connect.api.security.LoggedInUser;
import com.simbaquartz.xapi.connect.utils.ApiResponseUtil;
import com.simbaquartz.xapi.connect.models.errors.Error;
import com.fidelissd.zcp.xcommon.collections.FastMap;
import com.fidelissd.zcp.xcommon.models.account.ApplicationUser;
import com.fidelissd.zcp.xcommon.util.InvalidTokenException;
import com.fidelissd.zcp.xcommon.util.JWTUtils;
import com.fidelissd.zcp.xcommon.util.hierarchy.HierarchyUtils;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.ofbiz.base.crypto.HashCrypt;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.FileUtil;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.common.login.LoginServices;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericDelegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.security.SecurityConfigurationException;
import org.apache.ofbiz.security.SecurityFactory;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.webapp.control.LoginWorker;

public class AccessApiServiceImpl extends AccessApiService {

  private static final String module = AccessApiServiceImpl.class.getName();
  private static final String API_KEY = "apiKey";
  private static final String REFRESH_TOKEN = "refreshToken";
  private static final String ACCESS_TOKEN = "accessToken";
  public static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

  public static class AccessApiCustomErrorMessages {
    public static final String EMAIL_NOT_VERIFIED = "Email id not verified";
  }

  @Override
  public Response logoutUser(HttpHeaders httpHeaders) throws NotFoundException {
    Map<String, Object> createAccessTokenResponse = new HashMap<>();

    if (httpHeaders.getRequestHeaders().containsKey(API_KEY)) {
      // Get the apiKey header value from the request
      String apiKey = httpHeaders.getRequestHeader(API_KEY).get(0);
      if (UtilValidate.isNotEmpty(apiKey)) {
        try {
          // extract userlogin id from the encrypted authentication token
          byte[] bytes = java.util.Base64.getDecoder().decode(apiKey.getBytes());

          String userPass = new String(bytes);
          String userName = userPass.substring(0, userPass.indexOf(":")).toLowerCase();
          // log the user out
          LoginWorker.setLoggedOut(userName, delegator);

        } catch (Exception e) {
          Debug.logError(e, "An Exception occurred: " + e.getMessage(), module);
          return ApiResponseUtil.prepareDefaultResponse(
              Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }
      } else {
        Debug.logError("apiKey header value is missing", module);
        Error invalidLoginIdError =
            new Error(
                Error.CodeEnum.ACCESS_TOKEN_MISSING,
                Error.CategoryEnum.AUTHENTICATION_ERROR,
                ApiMessageConstants.MSG_MISSING_APIKEY_KEY,
                "accessToken");
        List<Error> errors = UtilMisc.toList(invalidLoginIdError);
        return ApiResponseUtil.prepareStandardResponse(Response.Status.UNAUTHORIZED, errors);
      }
    } else {
      Debug.logError("apiKey header key is missing", module);
      Error invalidLoginIdError =
          new Error(
              Error.CodeEnum.ACCESS_TOKEN_MISSING,
              Error.CategoryEnum.AUTHENTICATION_ERROR,
              ApiMessageConstants.MSG_MISSING_APIKEY_KEY,
              "accessToken");
      List<Error> errors = UtilMisc.toList(invalidLoginIdError);
      return ApiResponseUtil.prepareStandardResponse(Response.Status.UNAUTHORIZED, errors);
    }
    return ApiResponseUtil.prepareOkResponse(createAccessTokenResponse);
  }

  @Override
  public Response createToken(HttpHeaders httpHeaders) throws NotFoundException {
    if (httpHeaders.getRequestHeaders().containsKey(API_KEY)) {
      // Get the apiKey header value from the request
      String apiKey = httpHeaders.getRequestHeader(API_KEY).get(0);
      if (UtilValidate.isNotEmpty(apiKey)) {
        try {
          // extract userlogin id from the encrypted authentication token
          byte[] bytes = Base64.decodeBase64(apiKey);
          String userPass = new String(bytes);
          String userName = userPass.substring(0, userPass.indexOf(":"));
          String password = userPass.substring(userPass.indexOf(":") + 1);

          return createToken(userName, password);
        } catch (Exception e) {
          Debug.logError("An Exception occurred: " + e.getMessage(), module);
          return ApiResponseUtil.prepareDefaultResponse(
              Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }
      } else {
        Debug.logError("apiKey header value is missing", module);
        return ApiResponseUtil.prepareDefaultResponse(
            Response.Status.BAD_REQUEST, ApiMessageConstants.MSG_MISSING_APIKEY_VALUE);
      }
    } else {
      Debug.logError("apiKey header key is missing", module);
      return ApiResponseUtil.prepareDefaultResponse(
          Response.Status.BAD_REQUEST, ApiMessageConstants.MSG_MISSING_APIKEY_KEY);
    }
  }

  private Response createToken(String userName, String password) throws NotFoundException {
    return createToken(userName, password, false);
  }

  /**
   * Generates a token for the input username and password, if isGoogleAuth is true, password is
   * ignored.
   */
  private Response createToken(String userName, String password, boolean isGoogleAuth)
      throws NotFoundException {
    Map<String, Object> createAccessTokenResponse;
    LoggedInUser loggedInUser;
    boolean isAuthenticated = false;
    // Get the apiKey header value from the request
    try {
      // pull user login information from database.
      GenericValue userLogin =
          EntityQuery.use(delegator)
              .from("UserLogin")
              .where("userLoginId", userName)
              .cache(true)
              .queryOne();

      if (UtilValidate.isNotEmpty(userLogin)) {
        // a valid user login was found, now validate password
        if (!isGoogleAuth) {
          isAuthenticated = checkPassword(userLogin.getString("currentPassword"), true, password);
          if (!isAuthenticated) {
            return ApiResponseUtil.prepareDefaultResponse(
                Status.UNAUTHORIZED,
                "Wrong password. Try again or use Forgot password to reset it.",
                "password",
                Error.CodeEnum.INVALID_PASSWORD);
          }
        } else {
          // google authentication, mark user as authenticated
          isAuthenticated = true;
        }
      }

      if (isAuthenticated && UtilValidate.isNotEmpty(userLogin)) {
        String partyId = userLogin.getString("partyId");
        loggedInUser = new LoggedInUser(userLogin, partyId, dispatcher, delegator);
        Map<String, Object> contextMap = new HashMap<String, Object>();
        GenericValue sysUserLogin = HierarchyUtils.getSysUserLogin(delegator);
        contextMap.put("userLoginId", userName);
        // Calling the createAccessToken service
        createAccessTokenResponse = dispatcher.runSync("createAccessToken", contextMap);

        Map<String, Object> fsdGetAllPartyDetailsCtx = FastMap.newInstance();

        fsdGetAllPartyDetailsCtx.put("partyId", partyId);
        fsdGetAllPartyDetailsCtx.put("userLogin", sysUserLogin);

      } else {
        Debug.logError(
            "Unable to retrieve login details for the logged in user. Please validate the apiKey header value.",
            module);
        return ApiResponseUtil.prepareDefaultResponse(
            Response.Status.BAD_REQUEST, ApiMessageConstants.MSG_VALID_APIKEY_VALUE);
      }
    } catch (Exception e) {
      Debug.logError("An Exception occurred: " + e.getMessage(), module);
      return ApiResponseUtil.prepareDefaultResponse(
          Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
    }

    ApplicationUser loggedInUserDetails =
        MeApiServiceImpl.getUserDetails(
            loggedInUser.getPartyId(), loggedInUser.getUserLogin(), delegator, dispatcher);
    loggedInUserDetails.setSelf(true);
    loggedInUser.setAccountPartyId(loggedInUserDetails.getPersonalDetails().getOrganization().getId());
    if(UtilValidate.isNotEmpty(loggedInUserDetails.getStore())) {
      loggedInUser.setStoreId(loggedInUserDetails.getStore().getId());
    }
    return ApiResponseUtil.prepareOkResponse(
        UtilMisc.toMap(
            "accessToken", createAccessTokenResponse.get("accessToken"),
            "refreshToken", createAccessTokenResponse.get("refreshToken"),
            "user", loggedInUserDetails));
  }

  /** Identify if given userlogin's org setup has been completed or not */
  private boolean isOrgSetupComplete(GenericDelegator delegator, String userName) {
    try {
      GenericValue onboardingProgressGv =
          delegator.findOne(
              "OnBoardingProgress",
              UtilMisc.toMap("setUpName", "isOrgSetupComplete", "userLoginId", userName),
              false);
      if (UtilValidate.isEmpty(onboardingProgressGv)) return false;
      if ("Y".equalsIgnoreCase(onboardingProgressGv.getString("flag"))) {
        return true;
      }
    } catch (GenericEntityException e) {
      e.printStackTrace();
    }
    return false;
  }

  /** Identify if given userlogin's email verification setup has been completed or not */
  private boolean isEmailVerificationComplete(GenericDelegator delegator, String userName) {
    try {
      GenericValue onboardingProgressGv =
          delegator.findOne(
              "OnBoardingProgress",
              UtilMisc.toMap("setUpName", "isEmailVerificationComplete", "userLoginId", userName),
              false);
      if (UtilValidate.isEmpty(onboardingProgressGv)) return false;
      if ("Y".equalsIgnoreCase(onboardingProgressGv.getString("flag"))) {
        return true;
      }
    } catch (GenericEntityException e) {
      e.printStackTrace();
    }
    return false;
  }

  // Get TenantId for the given User Login
  private String getTenantForUserLogin(GenericDelegator delegatorToUse, String userLoginId) {
    String tenantId = null;
    if (UtilValidate.isNotEmpty(userLoginId)) {
      try {
        GenericValue tenantUserLogin =
            EntityQuery.use(delegatorToUse)
                .from("TenantUserLogin")
                .where("userLoginId", userLoginId)
                .filterByDate()
                .queryFirst();
        if (UtilValidate.isNotEmpty(tenantUserLogin)) {
          tenantId = tenantUserLogin.getString("tenantId");
        }
      } catch (GenericEntityException e) {
        e.printStackTrace();
      }
    }
    return tenantId;
  }

  private static boolean checkPassword(
      String oldPassword, boolean useEncryption, String currentPassword) {
    boolean passwordMatches = false;
    if (oldPassword != null) {
      if (useEncryption) {
        passwordMatches =
            HashCrypt.comparePassword(oldPassword, LoginServices.getHashType(), currentPassword);
      } else {
        passwordMatches = oldPassword.equals(currentPassword);
      }
    }
    if (!passwordMatches
        && "true"
            .equals(
                UtilProperties.getPropertyValue(
                    "security", "password.accept.encrypted.and.plain"))) {
      passwordMatches = currentPassword.equals(oldPassword);
    }
    return passwordMatches;
  }

  @Override
  public Response createRefreshToken(HttpHeaders httpHeaders) throws NotFoundException {
    Map<String, Object> createRefreshTokenResponse = FastMap.newInstance();

    if (httpHeaders.getRequestHeaders().containsKey(REFRESH_TOKEN)) {
      // Get the refreshToken header value from the request
      String refreshToken = httpHeaders.getRequestHeader(REFRESH_TOKEN).get(0);
      if (UtilValidate.isNotEmpty(refreshToken)) {
        // pull refreshToken information from database.
        try {
          byte[] decodedAccessTokenBytes =
              java.util.Base64.getDecoder().decode(refreshToken.getBytes());
          String decodedAccessToken = new String(decodedAccessTokenBytes);
          // GenericValue userAccessViewRecord =
          // EntityQuery.use(delegator).from("UserAccessView").where("accessToken",
          // accessToken).filterByDate().queryFirst();

          Map<String, Object> jwtMap = JWTUtils.parseJwt(decodedAccessToken);
          // GenericValue accessToken =
          // EntityQuery.use(delegator).from("AccessToken").where("refreshToken",
          // refreshToken).cache(true).queryOne();
          if (UtilValidate.isNotEmpty(jwtMap)) {
            String userName = (String) jwtMap.get("userLoginId");

            // pull user login information from database.
            try {
              GenericValue userLogin =
                  EntityQuery.use(delegator)
                      .from("UserLogin")
                      .where("userLoginId", userName)
                      .cache(true)
                      .queryOne();

              Map<String, Object> contextMap = new HashMap<String, Object>();
              if (UtilValidate.isNotEmpty(userLogin)) {
                contextMap.put("userLoginId", userName);
                // Calling the createAccessToken service
                createRefreshTokenResponse = dispatcher.runSync("createAccessToken", contextMap);
              } else {
                Debug.logWarning(
                    "A valid login was not found for the user login: " + userName, module);
              }
            } catch (GenericEntityException e) {
              Debug.logError(e, module);
              String prepareErrorMessage =
                  String.format(ApiMessageConstants.MSG_INVALID_USER_lOGIN_ID, userName);
              Error invalidLoginIdError =
                  new Error(
                      Error.CodeEnum.ACCOUNT_NOT_FOUND,
                      Error.CategoryEnum.AUTHENTICATION_ERROR,
                      prepareErrorMessage,
                      "username");
              List<Error> errors = UtilMisc.toList(invalidLoginIdError);
              return ApiResponseUtil.prepareStandardResponse(Response.Status.UNAUTHORIZED, errors);
            }

          } else {
            Debug.logError(
                "Unable to retrieve refreshToken details. Please validate the refreshToken header value.",
                module);
            return ApiResponseUtil.prepareDefaultResponse(
                Response.Status.BAD_REQUEST, ApiMessageConstants.MSG_VALID_EFRESH_TOKEN_VALUE);
          }
        } catch (IllegalArgumentException e) {
          Debug.logError(e, "An exception occurred: " + e.getMessage(), module);
          return ApiResponseUtil.prepareDefaultResponse(
              Status.BAD_REQUEST, "Invalid refresh token passed. Unable to authenticate.");
        } catch (GenericServiceException | InvalidTokenException e) {
          Debug.logError(e, "An exception occurred: " + e.getMessage(), module);
          return ApiResponseUtil.serverErrorResponse();
        }
      } else {
        Debug.logError("refreshToken header value is missing", module);
        return ApiResponseUtil.prepareDefaultResponse(
            Response.Status.BAD_REQUEST, ApiMessageConstants.MSG_MISSING_REFRESH_TOKEN_VALUE);
      }
    } else {
      Debug.logError("refreshToken header key is missing", module);
      return ApiResponseUtil.prepareDefaultResponse(
          Response.Status.BAD_REQUEST, ApiMessageConstants.MSG_MISSING_REFRESH_TOKEN_KEY);
    }
    return ApiResponseUtil.prepareOkResponse(createRefreshTokenResponse);
  }

  @Override
  public Response verifyAccessToken(HttpHeaders httpHeaders) throws NotFoundException {
    Map<String, Object> verifyAccessTokenResponse = FastMap.newInstance();
    verifyAccessTokenResponse.put("isValid", false);

    if (httpHeaders.getRequestHeaders().containsKey(ACCESS_TOKEN)) {
      String accessToken = httpHeaders.getRequestHeader(ACCESS_TOKEN).get(0);
      if (UtilValidate.isNotEmpty(accessToken)) {
        try {
          byte[] decodedAccessTokenBytes =
              java.util.Base64.getDecoder().decode(accessToken.getBytes());
          String decodedAccessToken = new String(decodedAccessTokenBytes);

          Map<String, Object> jwtMap = JWTUtils.parseJwt(decodedAccessToken);
          if (UtilValidate.isNotEmpty(jwtMap)) {
            String userLoginId = (String) jwtMap.get("userLoginId");
            String accessTokenUuid = (String) jwtMap.get("tokenUuid");
            GenericDelegator tenantDelegator = delegator;

            boolean isTokenUuidValid =
                AuthenticationFilter.checkIfAccessTokenUuidIsValid(
                    tenantDelegator, userLoginId, accessTokenUuid);
            if (isTokenUuidValid) {
              verifyAccessTokenResponse.put("isValid", true);
            }
          } else {
            Debug.logError(
                "Unable to retrieve accessToken details. Please validate the AccessToken header value.",
                module);
            return ApiResponseUtil.prepareDefaultResponse(
                Response.Status.BAD_REQUEST, ApiMessageConstants.MSG_VALID_ACCESS_TOKEN_VALUE);
          }
        } catch (InvalidTokenException | IllegalArgumentException e) {
          Debug.logError(e, "An exception occurred: " + e.getMessage(), module);
          return ApiResponseUtil.prepareDefaultResponse(
              Response.Status.UNAUTHORIZED, ApiMessageConstants.MSG_VALID_ACCESS_TOKEN_VALUE);
        }
      } else {
        Debug.logError("AccessToken is missing from the Input request", module);
        return ApiResponseUtil.prepareDefaultResponse(
            Response.Status.BAD_REQUEST, ApiMessageConstants.MSG_MISSING_ACCESS_TOKEN_KEY);
      }
    } else {
      Debug.logError("AccessToken is missing from the Input request", module);
      return ApiResponseUtil.prepareDefaultResponse(
          Response.Status.BAD_REQUEST, ApiMessageConstants.MSG_MISSING_ACCESS_TOKEN_KEY);
    }
    return ApiResponseUtil.prepareOkResponse(verifyAccessTokenResponse);
  }

  /**
   * Verifies the token acquired from a web based sign in session by checking with google for
   * validity. This token is for web based accounts so credentials are different as well.
   */
  @Override
  public Response verifyGoogleAccessToken(
      String tokenToVerify,
      HttpServletRequest request,
      HttpServletResponse response,
      SecurityContext securityContext)
      throws NotFoundException {

    GoogleIdToken idToken = null;
    try {
      final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

      String credentialsFilePath =
          EntityUtilProperties.getPropertyValue(
              "appconfig", "google.credentials.json.path", delegator);

      if (UtilValidate.isEmpty(credentialsFilePath)) {
        Debug.logError(
            "Google credentials file path(appconfig.properties#google.credentials.json.path) is missing, please load it via hot-deploy/sellercentral/data/SystemPropertyOverride.xml",
            module);
        return ApiResponseUtil.serverErrorResponse(
            "Google credentials json file was not found on the server, please configure it properly.");
      }

      java.io.File secretsFile = FileUtil.getFile(credentialsFilePath);
      InputStream in = FileUtils.openInputStream(secretsFile);
      GoogleClientSecrets clientSecrets =
          GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

      GoogleIdTokenVerifier verifier =
          new GoogleIdTokenVerifier.Builder(HTTP_TRANSPORT, JSON_FACTORY)
              .setAudience(Collections.singletonList(clientSecrets.getDetails().getClientId()))
              .build();

      idToken = verifier.verify(tokenToVerify);
      if (idToken != null) {
        Payload payload = idToken.getPayload();

        // Print user identifier
        String userId = payload.getSubject();
        System.out.println("User ID: " + userId);

        // Get profile information from payload for future usage.
        String email = payload.getEmail();
        boolean emailVerified = Boolean.valueOf(payload.getEmailVerified());
        String name = (String) payload.get("name");
        String pictureUrl = (String) payload.get("picture");
        String locale = (String) payload.get("locale");
        String familyName = (String) payload.get("family_name");
        String givenName = (String) payload.get("given_name");

        // Use or store profile information
        // check if we have a userLogin associated with that email address
        GenericValue userLogin = HierarchyUtils.getUserLogin(delegator, email);
        if (UtilValidate.isEmpty(userLogin)) {
          Debug.logInfo(
              "Didn't find the user login with email, trying to find using registered email addresses.",
              module);
          // try to find a party with the email as the primary email
          String partyId = HierarchyUtils.getPartyByEmail(delegator, email);
          userLogin = HierarchyUtils.getUserLoginByPartyId(delegator, partyId);
        }

        if (UtilValidate.isNotEmpty(userLogin)) {
          // found it, log the user in
          String userLoginId = userLogin.getString("userLoginId");
          Debug.log(
              "Found the user login with the input email, logging in the user with userLoginId: "
                  + userLoginId,
              module);

          setWebContextObjects(request, userLoginId, userLogin, delegator, dispatcher);

          // @MSS: This is disabled for the time being, it fails for google based login, works fine
          // without it for now. Leaving it here for future reference in case we want to track user
          // login the traditional way.
          //          String responseString =
          //              LoginWorker.loginUserWithUserLoginId(request, response, userLoginId);
          //          if (!"success".equals(responseString)) {
          //            Debug.logError("Login work returned error code: " + responseString, module);
          //            return ApiResponseUtil.prepareUnauthorizedResponse(
          //                "Unable to authenticate with error: " + responseString);
          //          }

          // set the username is session.
          //          LoginEvents.setUsername(request, response);

          return createToken(userLoginId, null, true);
        } else {
          return ApiResponseUtil.notFoundResponse(
              "A valid account was not found for the provided email <" + email + ">");
        }

      } else {
        System.out.println("Invalid ID token.");
        return ApiResponseUtil.badRequestResponse("Invalid token");
      }
    } catch (Exception e) {
      Debug.logError(e, module);
      return ApiResponseUtil.serverErrorResponse(
          "Something went wrong, please try again or request support.");
    }
  }

  private static void setWebContextObjects(
      HttpServletRequest request,
      String userLoginId,
      GenericValue userLogin,
      Delegator delegator,
      LocalDispatcher dispatcher) {
    HttpSession session = request.getSession();
    Security security = null;
    try {
      security = SecurityFactory.getInstance(delegator);
    } catch (SecurityConfigurationException e) {
      Debug.logError(e, module);
    }

    Debug.log("Setting session id: " + session.getId(), module);
    // NOTE: we do NOT want to set this in the servletContext, only in the request and session
    request.setAttribute("delegator", delegator);
    request.setAttribute("dispatcher", dispatcher);
    request.setAttribute("security", security);
    request.setAttribute("USERNAME", userLoginId);
    request.setAttribute("PASSWORD", "API_TOKEN_LOGIN");

    session.setAttribute("delegatorName", delegator.getDelegatorName());
    session.setAttribute("delegator", delegator);
    session.setAttribute("dispatcher", dispatcher);
    session.setAttribute("USERNAME", userLoginId);
    session.setAttribute("PASSWORD", "API_TOKEN_LOGIN");
    session.setAttribute("userLogin", userLogin);
    session.setAttribute("security", security);
  }
}
