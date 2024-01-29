package com.simbaquartz.xparty.services.preference;

/**
 * User preferences (settings) for a user. Use this to set up preferences (settings) for a user with
 * valid login account (email/phone based)
 */
public enum UserPreferenceTypesEnum {
  INTEGRATIONS_GOOGLE_CONNECTED(
      "integrations.google.account.connected", "Whether google account is connected or not."),
  INTEGRATIONS_GOOGLE_CALENDAR_SYNC_ENABLED(
      "integrations.google.calendar.syncEnabled",
      "Whether google calendar sync for the connected account is enabled or not."),
  INTEGRATIONS_GOOGLE_GMAIL_SYNC_ENABLED(
      "integrations.google.gmail.syncEnabled",
      "Whether google gmail sync for the connected account is enabled or not.");

  private String key;
  private String description;
  /**
   * Group id used to group application preferences.
   */
  public static final String groupId = "APPL_PREFERENCES";

  UserPreferenceTypesEnum(String key, String description) {
    this.key = key;
    this.description = description;
  }

  public String getKey() {
    return key;
  }

  public String getDescription() {
    return description;
  }

}
