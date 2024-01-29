package com.simbaquartz.xapi.connect.api.application.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fidelissd.zcp.xcommon.models.search.BeanSearchCriteria;
import java.util.List;
import javax.ws.rs.QueryParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Represents a loan application search criteria */
@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchApplicationCriteria extends BeanSearchCriteria {
  @QueryParam("lookingFor")
  @JsonProperty("lookingFor")
  private List<String> lookingFor = null;
}
