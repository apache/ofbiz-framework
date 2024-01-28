package com.simbaquartz.xcommon.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * For managing photo data for a person, company etc.
 */
@Data
public class Photo {
    @JsonProperty("id")
    private String id = null;

    @JsonProperty("publicUrl")
    private String publicUrl = null;

    @JsonProperty("sizeInKb")
    private String sizeInKb = null;

    @JsonProperty("sizeInMb")
    private String sizeInMb = null;

    @JsonProperty("thumbnailUrl")
    private String thumbnailUrl = null;

    @JsonProperty("uploadedAt")
    private String uploadedAt = null;

    @JsonProperty("uploadedBy")
    private String uploadedBy = null;
}
