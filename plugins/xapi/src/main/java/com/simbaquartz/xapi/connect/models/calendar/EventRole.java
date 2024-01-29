package com.simbaquartz.xapi.connect.models.calendar;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.sql.Timestamp;

/**
 * Roles of the event
 */
@Data
public class EventRole {

    @JsonProperty("partyId")
    private String partyId = null;

    @JsonProperty("role")
    private String role = null;

    @JsonProperty("email")
    private String email = null;

    @JsonProperty("fromDate")
    private String fromDate;
}
