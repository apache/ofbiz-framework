package com.simbaquartz.xapi.connect.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Workflow {

    @JsonProperty("status_id")
    private String statusId = null;

    @JsonProperty("status_id_from")
    private String statusIdFrom = null;

    @JsonProperty("status_id_to")
    private String statusIdTo = null;

    @JsonProperty("reason")
    private String reason = null;

    @JsonProperty("is_public")
    private String isPublic = null;

    @JsonProperty("type_id")
    private String typeId = null;

    @JsonProperty("name")
    private String name = null;

    @JsonProperty("description")
    private String description = null;

    @JsonProperty("color_id")
    private String colorId = null;

    @JsonProperty("status_type")
    private String statusType = null;

    @JsonProperty("default_status")
    private Boolean defaultStatus = false;

}

