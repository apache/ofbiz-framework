package com.simbaquartz.xapi.connect.models.collection;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fidelissd.zcp.xcommon.models.people.Person;
import lombok.Data;

import java.sql.Timestamp;

/**
 * Represents a Collection object.
 **/
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CollectionSearch {
  @JsonProperty("id")
  private String collectionId = null;

  @JsonProperty("type")
  private String collectionTypeId = null;//customer/product/vendor/employee

  @JsonProperty("owner_id")
  private String ownerId = null;//owner of collection, vendor, customer, product id

  @JsonProperty("name")
  private String name = null;

  @JsonProperty("description")
  private String description = null;

  @JsonProperty("parent_collection_id")
  private String parentCollectionId = null;

  @JsonProperty("created_at")
  private Timestamp createdAt;

  @JsonProperty("created_by")
  private Person createdBy;

  @JsonProperty("updated_by")
  private Person updatedBy;

  @JsonProperty("updated_at")
  private Timestamp updatedAt;

}

