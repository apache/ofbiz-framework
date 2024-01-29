package com.simbaquartz.xparty.services.invitation;

/**
 * Created by mande on 5/12/2021.
 */
public enum PartyInvitationStatusEnum {
  ANY("", "any"),
  PENDING("PARTYINV_PENDING", "pending"),
  ACCEPTED("PARTYINV_ACCEPTED", "accepted"),
  DECLINED("PARTYINV_DECLINED", "declined"),
  CANCELLED("PARTYINV_CANCELLED", "cancelled");

  private String statusId;
  private String code;

  PartyInvitationStatusEnum(String statusId, String code) {
    this.statusId = statusId;
    this.code = code;
  }

  public String getStatusId() {
    return statusId;
  }

  public String getCode() {
    return code;
  }
}
