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
public class ConnectedAccounts {
    @JsonProperty("google")
    private ConnectedAccountGoogle google;

    @JsonProperty("slack")
    private ConnectedAccountSlack slack;
}