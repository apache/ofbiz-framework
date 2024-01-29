package com.fidelissd.zcp.xcommon.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fidelissd.zcp.xcommon.models.account.User;
import lombok.Data;

import java.sql.Timestamp;

/**
 * Extend this class to include created/updated timestamp information for an entity.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreatedModifiedBy {
    @JsonProperty("createdAt")
    private Timestamp createdAt;

    @JsonProperty("createdAtPretty")
    private String createdAtPretty;

    @JsonProperty("createdBy")
    private User createdBy;

    @JsonProperty("updatedAt")
    private Timestamp updatedAt;

    @JsonProperty("updatedAtPretty")
    private String updatedAtPretty;

    @JsonProperty("lastModifiedBy")
    private User lastUpdatedBy;

    @JsonProperty("lastModifiedDate")
    private Timestamp lastModifiedDate;

    @JsonProperty("createdByUserLogin")
    private String createdByUserLogin;
}
