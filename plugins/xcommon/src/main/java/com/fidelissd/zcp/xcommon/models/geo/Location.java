package com.fidelissd.zcp.xcommon.models.geo;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * Represents one of a business locations.
 **/
@Data
public class Location   {

  @JsonProperty("id")
  private String id = null;

  @JsonProperty("name")
  private String name = null;

  @JsonProperty("address")
  private PostalAddress address = null;

  @JsonProperty("phone")
  private String phone = null;

  @JsonProperty("locale")
  private String locale = null;

  @JsonProperty("timezone")
  private String timezone = null;

  @JsonProperty("lattitude")
  private String lattitude = null;

  @JsonProperty("longitude")
  private String longitude = null;

  @JsonProperty("features")
  private List<String> features = new ArrayList<String>();
}

