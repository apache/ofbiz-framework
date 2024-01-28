package com.simbaquartz.xcommon.models.store;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.simbaquartz.xcommon.models.store.TimePeriod.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
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
public class BusinessHours {
  /**
   * Represents the time periods that this location is open for business. Holds a collection of
   * TimePeriod instances.
   */
  @JsonProperty("periods")
  private List<TimePeriod> periods = null;

  public static BusinessHours getDefaultBusinessHours() {
    BusinessHours businessHours = new BusinessHours();

    List<TimePeriod> timePeriods = new ArrayList<>();

    // Sunday hours
    TimePeriod sundayHours = new TimePeriod();
    sundayHours.setOpenDay(DayOfWeek.SUNDAY);
    sundayHours.setOpenTime("10:00"); // opens at 10 am
    sundayHours.setCloseDay(DayOfWeek.SUNDAY);
    sundayHours.setCloseTime("19:00"); // closes at 6 pm

    timePeriods.add(sundayHours);

    // Monday hours
    TimePeriod mondayHours = new TimePeriod();
    mondayHours.setOpenDay(DayOfWeek.MONDAY);
    mondayHours.setOpenTime("10:00"); // opens at 10 am
    mondayHours.setCloseDay(DayOfWeek.MONDAY);
    mondayHours.setCloseTime("19:00"); // closes at 6 pm

    timePeriods.add(mondayHours);

    // Tuesday hours
    TimePeriod tuesdayHours = new TimePeriod();
    tuesdayHours.setOpenDay(DayOfWeek.TUESDAY);
    tuesdayHours.setOpenTime("10:00"); // opens at 10 am
    tuesdayHours.setCloseDay(DayOfWeek.TUESDAY);
    tuesdayHours.setCloseTime("19:00"); // closes at 6 pm

    timePeriods.add(tuesdayHours);

    businessHours.setPeriods(timePeriods);
    return businessHours;
  }
}
