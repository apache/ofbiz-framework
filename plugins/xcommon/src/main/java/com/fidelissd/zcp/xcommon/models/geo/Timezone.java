package com.fidelissd.zcp.xcommon.models.geo;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.TimeZone;
import lombok.Data;

/**
 * Represents a timezone, Formatted name as an IANA Time Zone Database name, e.g. "Europe/Zurich".
 *
 * <p>Ref: https://developers.google.com/maps/documentation/timezone/overview
 */
@Data
public class Timezone {

  /** Index (for unique keys to help with UI) */
  @JsonProperty("index")
  private Integer index;

  /** Raw Timezone */
  @JsonProperty("timeZone")
  private TimeZone timeZone;

  /** Timezone ID */
  @JsonProperty("id")
  private String id;

  /** Timezone name */
  @JsonProperty("name")
  private String name;

  /**
   * The offset for daylight-savings time in seconds. This will be zero if the time zone is not in
   * Daylight Savings Time during the specified timestamp.
   */
  @JsonProperty("dstOffset")
  private int daylightSavingsTimeOffset;

  /**
   * The offset from UTC (in seconds) for the given location. This does not take into effect
   * daylight savings.
   */
  @JsonProperty("rawOffset")
  private int rawOffset;

  /** Gets the custom time zone ID based on the GMT offset of the platform. (e.g., "GMT+08:00") */
  @JsonProperty("gmtOffset")
  private String gmtOffset;

  /** Returns the GMT offset in hours */
  @JsonProperty("gmtOffsetInHours")
  private int gmtOffsetInHours;

  /** Returns the formatted name like (GMT+05:30) India Standard Time - Kolkata */
  @JsonProperty("formattedName")
  private String formattedName;

  /** Returns the short code, PST, IST etc. */
  @JsonProperty("code")
  private String code;
}
