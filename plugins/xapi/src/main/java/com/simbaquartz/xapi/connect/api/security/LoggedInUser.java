package com.simbaquartz.xapi.connect.api.security;

import com.fidelissd.zcp.xcommon.enums.AccountUserRoleTypesEnum;
import com.fidelissd.zcp.xcommon.models.account.User;
import java.security.Principal;
import java.util.Locale;
import lombok.Getter;
import lombok.Setter;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.GenericDelegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.LocalDispatcher;

/**
 * Captures logged in users' information. Account being used to access the API. Being populated in
 * AuthenticationFilter class's filter method.
 *
 * @author mssidhu
 */
public class LoggedInUser implements Principal {
  @Getter @Setter private GenericValue userLogin;

  @Getter @Setter private String displayName;

  @Getter @Setter private String email;

  @Getter @Setter private String photoUrl;

  @Getter @Setter private String userLoginId;

  @Getter @Setter private String partyId;

  /** Org/Account party id for the logged in user. The org user belongs to. */
  @Getter @Setter private String accountPartyId;

  @Getter @Setter private Locale locale;

  @Getter @Setter private String serverHost;
  @Getter @Setter private String accessToken;

  @Getter @Setter private String tenantId;
  @Getter @Setter private String role;

  @Getter @Setter private Boolean isAssumed;
  @Getter @Setter private String assumedBy;

  @Getter @Setter private User user;
  /** Default store id set for the logged in user. */
  @Getter @Setter private String storeId;

  @Getter private GenericDelegator delegator;
  @Getter private LocalDispatcher dispatcher;
  @Getter private Security security;

  public boolean isAdminRole() {
    return UtilValidate.isNotEmpty(role)
        && AccountUserRoleTypesEnum.ADMIN.getRoleName().equals(role);
  }

  public boolean isManagerRole() {
    return UtilValidate.isNotEmpty(role)
        && AccountUserRoleTypesEnum.MANAGER.getRoleName().equals(role);
  }

  public boolean isMemberRole() {
    return UtilValidate.isNotEmpty(role)
        && AccountUserRoleTypesEnum.MEMBER.getRoleName().equals(role);
  }

  public LoggedInUser(
      GenericValue userLogin,
      String displayName,
      String email,
      String photoUrl,
      String userLoginId,
      String partyId,
      String accountPartyId,
      String storeId,
      String role,
      Locale locale,
      LocalDispatcher dispatcher,
      GenericDelegator delegator) {
    this.userLogin = userLogin;
    this.displayName = displayName;
    this.email = email;
    this.photoUrl = photoUrl;
    this.userLoginId = userLoginId;
    this.partyId = partyId;
    this.locale = locale;
    this.accountPartyId = accountPartyId;
    this.storeId = storeId;
    this.role = role;
    this.delegator = delegator;
    this.dispatcher = dispatcher;
    DispatchContext dctx = dispatcher.getDispatchContext();
    this.security = dctx.getSecurity();
    this.user = new User(partyId, displayName, email, photoUrl, true, false);
  }

  public LoggedInUser(
      GenericValue userLogin,
      String partyId,
      LocalDispatcher dispatcher,
      GenericDelegator delegator) {
    this.userLogin = userLogin;
    this.partyId = partyId;
    this.delegator = delegator;
    this.dispatcher = dispatcher;
  }

  @Override
  public String getName() {
    return displayName;
  }
}
