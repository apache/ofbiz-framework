package com.simbaquartz.xapi.services;

import com.fidelissd.zcp.xcommon.util.JWTUtils;
import com.fidelissd.zcp.xcommon.util.hierarchy.HierarchyUtils;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.fidelissd.zcp.xcommon.collections.FastList;
import com.fidelissd.zcp.xcommon.collections.FastMap;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityConditionList;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;

public class AccessTokenServices {

  public static final String module = AccessTokenServices.class.getName();

  private static int ACCESS_TOKEN_VALIDITY_IN_MINS =
      UtilProperties.getPropertyAsInteger("xapi.properties", "xapi.thru.date.token", 0);

  /**
   * Generates an access token valid for the next 60 days and refresh token expires in next 1 year.
   *
   * @param dctx
   * @param context
   * @return
   */
  public static Map<String, Object> createAccessToken(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    String userLoginId = (String) context.get("userLoginId");
    String assumedBy = (String) context.get("assumedBy");
    Boolean isAssumed = (Boolean) context.get("isAssumed");

    String encodedAccessToken = null;
    String encodedRefreshToken = null;
    // current date
    Timestamp currentDate = UtilDateTime.nowTimestamp();
    long currentTimeInMilisecond = currentDate.getTime();

    // Add minutes to the currentDate
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(currentTimeInMilisecond);
    cal.add(Calendar.MINUTE, ACCESS_TOKEN_VALIDITY_IN_MINS);
    Timestamp accessThruDate = new Timestamp(cal.getTime().getTime());

    // To create encodeAccessToken
    String accessTokenUuid = UUID.randomUUID().toString();

    // assume account
    boolean isOrgAdmin = false;
    Security security = dctx.getSecurity();
    GenericValue userLogin = HierarchyUtils.getUserLogin(delegator, userLoginId);
    if (security.hasPermission("ZCP_ADMIN", userLogin)) {
      isOrgAdmin = Boolean.TRUE;
    } else {
      isOrgAdmin = Boolean.FALSE;
    }
    Map<String, Object> claims = FastMap.newInstance();
    if (UtilValidate.isNotEmpty(isAssumed)) {
      claims.put("isAssumed", isAssumed);
    }

    String accessToken =
        generateAccessToken(
            userLoginId, accessThruDate, "ACCESS", accessTokenUuid, isAssumed, assumedBy);
    encodedAccessToken = Base64.getEncoder().encodeToString(accessToken.getBytes());

    // To create the encodeRefreshToken
    cal.add(
        Calendar.YEAR,
        1); // Give 1 year extra for refreshToken - providing way to get new access-token
    Timestamp refreshThruDate = new Timestamp(cal.getTime().getTime());

    String refreshTokenUuid = UUID.randomUUID().toString();

    String refreshToken =
        generateAccessToken(
            userLoginId, refreshThruDate, "REFRESH", refreshTokenUuid, isAssumed, assumedBy);
    encodedRefreshToken = Base64.getEncoder().encodeToString(refreshToken.getBytes());

    // To create new record
    List<GenericValue> store = FastList.newInstance();
    GenericValue newEntity = delegator.makeValue("AccessToken");
    newEntity.set("userLoginId", userLoginId);
    newEntity.set("accessToken", accessTokenUuid);
    newEntity.set("refreshToken", refreshTokenUuid);
    newEntity.set("fromDate", currentDate);
    newEntity.set(
        "thruDate",
        accessThruDate); // Update the thruDate by adding 30 mins every time user access system
    newEntity.set("isActive", "Y");
    newEntity.set("isAssumed", isAssumed); // setting flag to true if the account is assumed
    store.add(newEntity);
    if (UtilValidate.isNotEmpty(isAssumed) && isAssumed) {
      GenericValue userLoginHistory = delegator.makeValue("UserLoginHistory");
      userLoginHistory.set("userLoginId", userLoginId);
      userLoginHistory.set("fromDate", UtilDateTime.nowTimestamp());
      userLoginHistory.set("lastAssumedAt", UtilDateTime.nowTimestamp());
      userLoginHistory.set("lastAssumedBy", assumedBy);
      store.add(userLoginHistory);
    }

    try {
      delegator.create(newEntity);
    } catch (GenericEntityException e) {
      e.printStackTrace();
    }

    // Deactivate older tokens for this userLogin, uncomment to allow only one login per session
    // expireActiveAccessTokensForUserlogin(delegator, userLoginId, accessTokenUuid);

    serviceResult.put("accessToken", encodedAccessToken);
    serviceResult.put("refreshToken", encodedRefreshToken);
    serviceResult.put("expiresOn", accessThruDate);
    return serviceResult;
  }

  /**
   * Expires all active access tokens for a user login, used when an account is removed.
   * @param dctx
   * @param context
   * @return
   */
  public static Map<String, Object> expireAllAccessTokensForUserlogin(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    String userLoginId = (String) context.get("userLoginId");

    expireActiveAccessTokensForUserlogin(delegator, userLoginId, null);
    return serviceResult;
  }

  /**
   * Disables all active tokens for the input user login
   *
   * @param delegator
   * @param userLoginId
   * @param accessTokenUuid (Optional)
   */
  private static void expireActiveAccessTokensForUserlogin(
      Delegator delegator, String userLoginId, String accessTokenUuid) {
    // Find Active tokens for this userLoginId, apart from current accessToken, and disable them
    EntityConditionList<EntityExpr> mainCond;

    if (UtilValidate.isNotEmpty(accessTokenUuid)) {
      mainCond =
          EntityCondition.makeCondition(
              UtilMisc.toList(
                  EntityCondition.makeCondition(
                      "accessToken", EntityOperator.NOT_EQUAL, accessTokenUuid),
                  EntityCondition.makeCondition("isActive", EntityOperator.EQUALS, "Y"),
                  EntityCondition.makeCondition("userLoginId", EntityOperator.EQUALS, userLoginId)),
              EntityOperator.AND);

    } else {
      mainCond =
          EntityCondition.makeCondition(
              UtilMisc.toList(
                  EntityCondition.makeCondition("isActive", EntityOperator.EQUALS, "Y"),
                  EntityCondition.makeCondition("userLoginId", EntityOperator.EQUALS, userLoginId)),
              EntityOperator.AND);
    }
    try {
      List<GenericValue> activeTokens =
          EntityQuery.use(delegator).from("AccessToken").where(mainCond).queryList();
      if (UtilValidate.isNotEmpty(activeTokens)) {
        for (GenericValue activeToken : activeTokens) {
          activeToken.setString("isActive", "N");
        }
        delegator.storeAll(activeTokens);
      }
    } catch (GenericEntityException e) {
      Debug.logError(e, module);
    }
  }

  /**
   * Generate Access Token by using UUID, userLoginId and TenantId use JWT to generate jwt token
   *
   * @param userLoginId
   * @return
   */
  private static String generateAccessToken(
      String userLoginId,
      Date thruDate,
      String tokenType,
      String tokenUuid,
      Boolean isAssumed,
      String assumedBy) {
    Map<String, Object> inputMap = new HashMap<>();
    inputMap.put("tokenUuid", tokenUuid);
    inputMap.put("userLoginId", userLoginId);
    inputMap.put("tokenType", tokenType);
    inputMap.put("thruDate", thruDate);
    inputMap.put("isAssumed", isAssumed);

    if (UtilValidate.isNotEmpty(isAssumed)) {
      inputMap.put("isAssumed", isAssumed);
    }
    if (UtilValidate.isNotEmpty(assumedBy)) {
      inputMap.put("assumedBy", assumedBy);
    }
    // Make token valid for infinite (since token inactivity is handled via DB)
    Timestamp currentDate = UtilDateTime.nowTimestamp();
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(currentDate.getTime());
    cal.add(Calendar.MINUTE, 43200); // 1 yr
    Date validTillDate = new Timestamp(cal.getTime().getTime());
    return JWTUtils.generateJwt(inputMap, validTillDate);
  }
}
