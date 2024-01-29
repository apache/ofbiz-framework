package com.simbaquartz.xapi.connect.models.supplier;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fidelissd.zcp.xcommon.models.media.File;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SupplierAttachment extends File {
    @JsonProperty("is_attachable")
    private boolean attachable = false;

    @JsonProperty("default_attachment")
    private String defaultAttachment = "";
}
