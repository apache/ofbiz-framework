package com.simbaquartz.xcommon.enums;

/** Represents the role types available in the application. */
public enum AccountUserRoleTypesEnum {
  ADMIN("APP_ADMIN", "admin"),
  MANAGER("APP_MANAGER", "manager"),
  MEMBER("APP_MEMBER", "member");

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
