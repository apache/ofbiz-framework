package com.fidelissd.zcp.xcommon.enums;

/** Represents the role types available in the application. */
public enum AccountUserRoleTypesEnum {
  ADMIN("ORG_ADMIN", "admin"),
  MANAGER("ORG_MANAGER", "manager"),
  MEMBER("ORG_MEMBER", "member");

  private String role;
  private String roleName;

  AccountUserRoleTypesEnum(String role, String roleName) {
    this.role = role;
    this.roleName = roleName;
  }

  public String getRole() {
    return role;
  }

  public String getRoleName() {
    return roleName;
  }
}
