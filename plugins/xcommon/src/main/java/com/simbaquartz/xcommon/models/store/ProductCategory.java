package com.simbaquartz.xcommon.models.store;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.simbaquartz.xcommon.models.media.File;
import com.simbaquartz.xcommon.models.people.Person;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class ProductCategory {
  @JsonProperty("id")
  private String id = null;

  /** Name of the Category. */
  @JsonProperty("name")
  private String name = null;

  /** Description of the Category. */
  @JsonProperty("description")
  private String description = null;

  /** Long description of the category */
  @JsonProperty("longDescription")
  private String longDescription = null;

  @JsonProperty("createdBy")
  private Person createdBy = null;

  @JsonProperty("updatedBy")
  private Person updatedBy = null;

  @JsonProperty("productCategoryTypeId")
  private String productCategoryTypeId = null;

  @JsonProperty("categoryContent")
  private List<File> categoryContent = null;

  @JsonProperty("gst")
  private BigDecimal gst = null;

}
