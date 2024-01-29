package com.simbaquartz.xapi.connect.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fidelissd.zcp.xcommon.models.people.Person;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuoteContact {
  @JsonProperty("id")
  private String id = null; //partyId
  @JsonProperty("person")
  private Person person = null;
  @JsonProperty("name")
  private String name = null;
  @JsonProperty("role_type_id")
  private String roleTypeId = null;
  @JsonProperty("role_type")
  private String roleTypeName = null;
  @JsonProperty("display_name")
  private String displayName = null;

}

