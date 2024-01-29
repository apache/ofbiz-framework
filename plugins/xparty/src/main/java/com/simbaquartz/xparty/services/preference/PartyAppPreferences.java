package com.simbaquartz.xparty.services.preference;

import lombok.Data;

/** Application level preferences for a party. Theme selection, email notifications etc. */
@Data
public class PartyAppPreferences {
  // general preferences
  /** Whether the Sidebar on UI is collapsed or not. */
  private boolean sidebarCollapsed = false;

  /** Whether dark theme is enabled or not. */
  private boolean darkThemeEnabled = false;
  /** Notification preferences for the party. */
  private NotificationPreferences notificationPreferences = null;

  // notification preferences
  /** Represents the notifications preferences (email, push) for a party. */
  @Data
  public static final class NotificationPreferences {

    /**
     * Whether to send daily agenda email notification. If true an email is send at the EOD to the
     * party about their tasks done today and what's due the next day.
     */
    private boolean dailyAgendaEmailNotificationEnabled = false;

    /** Whether to notify the user when they have been assigned a new task. */
    private boolean taskAssignedNotificationEnabled = false;

    /**
     * Whether google sync is enabled or not, if enabled emails/calendar events will be synced
     * automatically.
     */
    private boolean googleCalendarSyncEnabled = false;

    /**
     * If enabled, when an email is sent using connected google account, it also adds inbox label to
     * the email so it shows up on the gmail inbox of the user making sure the email went out.
     */
    private boolean googleAddInboxLabelToSentItems = true;
  }
}
