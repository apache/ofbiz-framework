package com.simbaquartz.xapi.connect.api.me.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.simbaquartz.xparty.services.preference.UserPreferenceTypesEnum;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import lombok.Data;

/** Represents a user preference */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserPreference {

  /** Whether user would like to have weather widget enabled or not on the dashboard. */
  @JsonProperty("showWeatherWidget")
  private Boolean showWeatherWidget = true;

  /** Users's default timezone, this will be used to manage calendar events. */
  @JsonProperty("timeZone")
  private String timeZoneId = null;

  /**
   * Returns google config, indicating if google account is connected and sync services are enabled
   * or disabled.
   */
  @JsonProperty("googleConfig")
  private GoogleConfig googleConfig = new GoogleConfig();

  /** For managing google related configuration for an account. */
  @Data
  public static final class GoogleConfig {
    /** Whether google account is connected or not. */
    @JsonProperty("connected")
    private Boolean enabled = false;

    /** Whether email (gmail) sync is enabled. */
    @JsonProperty("emailSyncEnabled")
    private Boolean emailSyncEnabled = false;

    /** Whether google calendar sync is enabled */
    @JsonProperty("calendarSyncEnabled")
    private Boolean calendarSyncEnabled = false;
  }

  public static UserPreference buildModel(Map<String, String> preferenceValues) {
    UserPreference userPreference = new UserPreference();
    Iterator preferenceIterator = preferenceValues.entrySet().iterator();
    while (preferenceIterator.hasNext()) {
      Map.Entry pair = (Map.Entry) preferenceIterator.next();
      String preferenceKey = (String) pair.getKey();
      String preferenceValue = (String) pair.getValue();

      if (preferenceKey.equals(
          UserPreferenceTypesEnum.INTEGRATIONS_GOOGLE_CALENDAR_SYNC_ENABLED.getKey())) {
        Boolean calenarSyncEnabled = Boolean.parseBoolean(preferenceValue);
        userPreference.getGoogleConfig().setCalendarSyncEnabled(calenarSyncEnabled);
      } else if (preferenceKey.equals(
          UserPreferenceTypesEnum.INTEGRATIONS_GOOGLE_GMAIL_SYNC_ENABLED.getKey())) {
        Boolean gmailSyncEnabled = Boolean.parseBoolean(preferenceValue);
        userPreference.getGoogleConfig().setEmailSyncEnabled(gmailSyncEnabled);
      }
    }

    // implement self
    return userPreference;
  }
}
