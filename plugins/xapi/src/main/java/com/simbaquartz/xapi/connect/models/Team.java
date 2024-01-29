package com.simbaquartz.xapi.connect.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fidelissd.zcp.xcommon.models.people.Person;
import lombok.Data;

import java.util.List;

@Data
public class Team {

    @JsonProperty("id")
    private String id = null;

    @JsonProperty("name")
    private String name = null;

    @JsonProperty("description")
    private String description = null;

    @JsonProperty("photo")
    private Photo photo = null;

    @JsonProperty("member_party_id")
    private String memberPartyId = null;

    @JsonProperty("role_type_id")
    private String roleTypeId = null;

    @JsonProperty("members")
    private List members = null;

    @JsonProperty("created_by")
    private Person createdBy = null;

}

