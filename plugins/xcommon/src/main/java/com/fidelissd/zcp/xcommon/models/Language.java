package com.fidelissd.zcp.xcommon.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@Builder
@EqualsAndHashCode(callSuper=false)
@AllArgsConstructor
@NoArgsConstructor
public class Language {

    @JsonProperty("id")
    private  String id;

    @JsonProperty("name")
    private  String name;
    
    @JsonProperty("fluency")
    private Integer fluency;

    @JsonProperty("read")
    private Integer read;

    @JsonProperty("write")
    private Integer write;

    @JsonProperty("spoken")
    private Integer spoken;
}
