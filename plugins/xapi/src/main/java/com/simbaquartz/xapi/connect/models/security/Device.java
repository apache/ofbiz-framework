package com.simbaquartz.xapi.connect.models.security;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Represents a device a user can use to log into their account.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Device {
    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("operating_system")
    private String operatingSystem;

    /**
     * Stores the Geo co-ordinates, lat and long
     */
    @JsonProperty("last_accessed_location")
    private String lastAccessedLocation;

    /**
     * Last accessed date and time in UTC format.
     */
    @JsonProperty("last_accessed_at")
    private String lastAccessedAt;

    /**
     * Is a login session active on this device
     */
    @JsonProperty("is_active")
    private boolean isActive;

    /**
     * IP address of the device from where the access was made
     */
    @JsonProperty("last_accessed_ip")
    private String lastAccessedIp;
}