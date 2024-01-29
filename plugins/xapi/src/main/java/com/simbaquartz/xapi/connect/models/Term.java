package com.simbaquartz.xapi.connect.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fidelissd.zcp.xcommon.models.CreatedModifiedBy;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.sql.Timestamp;

/**
 * Represents a party term.
 **/
@Data
@EqualsAndHashCode(callSuper=false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Term extends CreatedModifiedBy {
    @JsonProperty("id")
    private String id = null;

    @JsonProperty("party_id")
    private String partyId = null;

    @JsonProperty("status_id")
    private String statusId = null;

    @JsonProperty("party_content_type_id")
    private String partyContentTypeId    = null;

    @JsonProperty("name")
    private String name = null;

    @JsonProperty("description")
    private String description = null;

    @JsonProperty("data_resource_id")
    private String dataResourceId = null;

    @JsonProperty("content_id")
    private String contentId = null;

    @JsonProperty("from_date")
    private Timestamp fromDate = null;
}