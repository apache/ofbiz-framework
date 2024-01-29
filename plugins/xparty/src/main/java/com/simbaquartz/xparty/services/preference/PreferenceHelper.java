package com.simbaquartz.xparty.services.preference;

import java.util.Iterator;
import java.util.Map;
import org.apache.ofbiz.base.util.UtilValidate;

public class PreferenceHelper {
  // maps to db key values for preferences
  public static final String APP_DAILY_AGENDA_EMAIL = "APP_DAILY_AGENDA_EMAIL";
  public static final String APP_PAST_DUE_TASK_NOTIFY = "tasks.reminders.pastDueNotifications";
  public static final String APP_GOOGLE_CALENDAR_SYNC_ENABLED =
      "integrations.google.calendar.syncEnabled";
  public static final String APP_GOOGLE_ADD_INBOX_LABEL_TO_SENT_ITEMS =
      "integrations.google.gmail.addInboxLabelToSentItems";

  /**
   * Helps build the party preference class instance using PartyPreference db record values.
   *
   * @param preferenceValues List of PartyPreference records from db
   * @return
   */
  public static PartyAppPreferences buildPartyPreferences(Map<String, String> preferenceValues) {
    Iterator preferenceIterator = preferenceValues.entrySet().iterator();
    PartyAppPreferences partyAppPreferences = new PartyAppPreferences();
    PartyAppPreferences.NotificationPreferences partyNotificationPreferences =
        new PartyAppPreferences.NotificationPreferences();

    while (preferenceIterator.hasNext()) {
      Map.Entry pair = (Map.Entry) preferenceIterator.next();
      String preferenceKey = (String) pair.getKey();
      String preferenceValue = (String) pair.getValue();

      switch (preferenceKey) {
        case APP_DAILY_AGENDA_EMAIL:
          boolean isSendDailyAgendaEmailNotificationEnabled = false;
          if (UtilValidate.isNotEmpty(preferenceValue)) {
            isSendDailyAgendaEmailNotificationEnabled = Boolean.parseBoolean(preferenceValue);
          }
          partyNotificationPreferences.setDailyAgendaEmailNotificationEnabled(
              isSendDailyAgendaEmailNotificationEnabled);
          break;

        case APP_PAST_DUE_TASK_NOTIFY:
          boolean isTaskSettingsEnabled = false;
          if (UtilValidate.isNotEmpty(preferenceValue)) {
            isTaskSettingsEnabled = Boolean.parseBoolean(preferenceValue);
          }
          partyNotificationPreferences.setTaskAssignedNotificationEnabled(isTaskSettingsEnabled);
          break;
        case APP_GOOGLE_CALENDAR_SYNC_ENABLED:
          boolean isGoogleCalendarSyncEnabled = false;
          if (UtilValidate.isNotEmpty(preferenceValue)) {
            isGoogleCalendarSyncEnabled = Boolean.parseBoolean(preferenceValue);
          }
          partyNotificationPreferences.setGoogleCalendarSyncEnabled(isGoogleCalendarSyncEnabled);
          break;
        case APP_GOOGLE_ADD_INBOX_LABEL_TO_SENT_ITEMS:
          boolean isGoogleAddInboxToSentItems = false;
          if (UtilValidate.isNotEmpty(preferenceValue)) {
            isGoogleAddInboxToSentItems = Boolean.parseBoolean(preferenceValue);
          }
          partyNotificationPreferences.setGoogleAddInboxLabelToSentItems(
              isGoogleAddInboxToSentItems);
          break;
        default:
          break;
      }
    }

    partyAppPreferences.setNotificationPreferences(partyNotificationPreferences);

    return partyAppPreferences;
  }
}
