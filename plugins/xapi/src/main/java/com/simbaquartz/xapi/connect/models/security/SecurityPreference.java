package com.simbaquartz.xapi.connect.models.security;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SecurityPreference {
    @JsonProperty("id")
    private String id;

    @JsonProperty("enforce_strong_password")
    private boolean enforceStrongPassword;

    @JsonProperty("password_length_min")
    private int passwordLengthMin;

    @JsonProperty("password_length_max")
    private int passwordLengthMax;

    @JsonProperty("enforce_at_next_sign_in")
    private boolean enforceAtNextSignIn;

    @JsonProperty("password_reset_frequency")
    private int passwordResetFrequency;

    @JsonProperty("mfa_enable")
    private boolean mfaEnable;

    @JsonProperty("mfa_enable_from")
    private boolean mfaEnableFromDate;

    @JsonProperty("mfa_grace_period")
    private boolean mfaGracePeriod;

    @JsonProperty("mfa_default_auth_type")
    private boolean mfaDefaultAuthType;

    @JsonProperty("allow_device_trust")
    private boolean allowDeviceTrust;

    @JsonProperty("trusted_devices")
    private List<Device> trustedDevices;

}
