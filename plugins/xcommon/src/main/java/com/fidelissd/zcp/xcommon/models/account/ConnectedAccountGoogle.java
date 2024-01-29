package com.fidelissd.zcp.xcommon.models.account;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConnectedAccountGoogle {
    @JsonProperty("connected")
    @Builder.Default
    private Boolean isConnected = false;

    @JsonProperty("email")
    private String email;

    @JsonProperty("defaultCalendarId")
    private String defaultCalendarId;
}