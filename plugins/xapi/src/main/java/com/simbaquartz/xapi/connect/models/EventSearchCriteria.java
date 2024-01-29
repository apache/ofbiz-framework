package com.simbaquartz.xapi.connect.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fidelissd.zcp.xcommon.models.search.BeanSearchCriteria;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.ws.rs.QueryParam;
import java.sql.Timestamp;
import java.util.TimeZone;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper = false)
public class EventSearchCriteria extends BeanSearchCriteria {
    @QueryParam("eventId")
    private String eventId;

    @QueryParam("eventTypeId")
    private String eventTypeId;

    @QueryParam("eventName")
    private String eventName;

    @QueryParam("eventsAfter")
    private String createdEventDateRangeFrom;

    @QueryParam("eventsBefore")
    private String createdEventDateRangeTo;

    //@QueryParam("timeZone")
    // TODO: needs to be accepted as string, and then convert to timezone
    private TimeZone timeZone = null;

    @QueryParam("custRequestId")
    private String custRequestId;

    @QueryParam("leadPartyId")
    private String leadPartyId = null;

    @QueryParam("includePublic")
    private String includePublic = null;

    private String calendarId;
}
