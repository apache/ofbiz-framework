package com.fidelissd.zcp.xcommon.models.store;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fidelissd.zcp.xcommon.models.media.File;

import java.sql.Timestamp;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class ProductAttachment extends File {

  @JsonProperty("typeId")
  private String productContentTypeId = null;

  @JsonProperty("from_date")
  private Timestamp fromDate = null;
}
