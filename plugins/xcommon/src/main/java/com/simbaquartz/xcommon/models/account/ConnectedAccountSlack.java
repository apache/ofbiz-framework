package com.simbaquartz.xcommon.models.account;

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
public class ConnectedAccountSlack {
    @JsonProperty("connected")
    @Builder.Default
    private Boolean isConnected = false;

    @JsonProperty("teamId")
    private String slackTeamId;

    @JsonProperty("teamName")
    private String slackTeamName;
}