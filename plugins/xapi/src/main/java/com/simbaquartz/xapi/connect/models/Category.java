package com.simbaquartz.xapi.connect.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.sql.Timestamp;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Category {
    @JsonProperty("id")
    private String id = null;
    @JsonProperty("handle")
    private String handleUrl = null;
    @JsonProperty("name")
    private String name = null;
    @JsonProperty("description")
    private String description = null;
    @JsonProperty("quantity")
    private String quantity = null;
    @JsonProperty("sequence_num")
    private Long sequenceNum = null;
    @JsonProperty("from_date")
    private Timestamp fromDate = null;
    @JsonProperty("thru_date")
    private Timestamp thruDate = null;
    @JsonProperty("created_at")
    private String createdAt = null;
    @JsonProperty("created_at_pretty")
    private String createdAtPretty = null;
    @JsonProperty("updated_at")
    private String updatedAt = null;
    @JsonProperty("updated_at_pretty")
    private String updatedAtPretty = null;
    @JsonProperty("url_handle")
    private String urlHandle = null;

    @JsonProperty("primary_parent_category_id")
    private String primaryParentCategoryId = null;
}
