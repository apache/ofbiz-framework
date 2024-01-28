package com.simbaquartz.xcommon.models.store;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Represents a span of time. Can be used to set:
 *
 * <p>Business hours: that the business is open, starting on the specified open day/time and closing
 * on the specified close day/time. The closing time must occur after the opening time, for example
 * later in the same day, or on a subsequent day.
 *
 * <p>Working hours:Enable working hours to let people know what times you're working and when
 * you're available for meetings. This will warn people if they try to invite you to a meeting
 * outside of these hours.
 */
@Data
public class TimePeriod {

  /** enum ({@link DayOfWeek}) Indicates the day of the week this period starts on. */
  @JsonProperty("openDay")
  private DayOfWeek openDay = null;

  /**
   * Time in 24hr ISO 8601 extended format (hh:mm). Valid values are 00:00-24:00, where 24:00
   * represents midnight at the end of the specified day field.
   */
  @JsonProperty("openTime")
  private String openTime = null;

  /** enum ({@link DayOfWeek}) Indicates the day of the week this period ends on. */
  @JsonProperty("closeDay")
  private DayOfWeek closeDay = null;
  /**
   * Time in 24hr ISO 8601 extended format (hh:mm). Valid values are 00:00-24:00, where 24:00
   * represents midnight at the end of the specified day field.
   */
  @JsonProperty("closeTime")
  private String closeTime = null;

  // Represents a day of the week.
  public enum DayOfWeek {
    DAY_OF_WEEK_UNSPECIFIED("Not specified"),
    MONDAY("Monday"),
    TUESDAY("Tuesday"),
    WEDNESDAY("Wednesday"),
    THURSDAY("Thursday"),
    FRIDAY("Friday"),
    SATURDAY("Saturday"),
    SUNDAY("Sunday");

    private String dayName;

    DayOfWeek(String dayName) {
      this.dayName = dayName;
    }

    public String getDayName() {
      return dayName;
    }

    @Override
    public String toString() {
      return dayName;
    }
  }
}
