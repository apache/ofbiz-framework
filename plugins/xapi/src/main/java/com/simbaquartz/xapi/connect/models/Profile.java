package com.simbaquartz.xapi.connect.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2017-06-30T20:56:17.408-07:00")
public class Profile {

    @JsonProperty("profile_id")
    private String profileId = null; //profileId

    @JsonProperty("permissions")
    private List<String> permissions = null; //permissions

    @JsonProperty("profile_name")
    private String profileName = null; //profileName

    @JsonProperty("clone_permissions_from")
    private String clonePermissionsFrom = null; //clonePermissionsFrom
}

