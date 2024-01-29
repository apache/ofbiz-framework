package com.simbaquartz.xapi.connect.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fidelissd.zcp.xcommon.models.CreatedModifiedBy;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Represents a party territory.
 **/
@Data
@EqualsAndHashCode(callSuper=false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Territory extends CreatedModifiedBy {

    @JsonProperty("ids")
    private List<String> ids = null;

    @JsonProperty("type_id")
    private String typeId = null;

}