package com.simbaquartz.xcommon.models.account;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Data;

/**
 * Represents the account preferences/settings for an account (MMO customer/client in this case).
 * This is the account that manages the settings/configurations for all account members.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountPreferences {
    /**
     * Whether slack is enabled or not for the user
     */
    @JsonProperty("sales")
    private SalesSettings salesSettings;

    /**
     * Whether slack is enabled or not for the user
     */
    @JsonProperty("slack")
    private SlackConfig slackConfig;

    /** For managing sales related configuration for an account. */
    @Data
    public static final class SalesSettings {
        /** List of people to notify by default when a new lead is added. */
        @JsonProperty("defaultListofPeopleToNotifyAboutLead")
        private List<User> defaultListofPeopleToNotifyAboutLead;
        private String accessToken;
    }

    /** For managing slack related configuration for an account. */
    @Data
    public static final class SlackConfig {
        /** Whether slack integration is enabled or not. */
        @JsonProperty("enabled")
        private Boolean enabled;

        /** Slack workspace url of the account if enabled. */
        @JsonProperty("workspaceUrl")
        private String workspaceUrl;

        /** Access token. */
        @JsonProperty("accessToken")
        private String accessToken;
    }
}