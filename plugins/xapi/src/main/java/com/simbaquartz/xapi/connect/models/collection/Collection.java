package com.simbaquartz.xapi.connect.models.collection;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Represents a Collection object.
 **/
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Collection {
  @JsonProperty("id")
  private String collectionId = null;

  @JsonProperty("type")
  private String collectionTypeId = null;//customer/product/vendor/employee

  @JsonProperty("collection_item_id")
  private String collectionItemId = null;

  @JsonProperty("owner_id")
  private String ownerId = null;//owner of collection, vendor, customer, product id

  @JsonProperty("name")
  private String name = null;

  @JsonProperty("description")
  private String description = null;

  @JsonProperty("parent_collection_id")
  private String parentCollectionId = null;

  @JsonProperty("updated_by")
  private String updatedBy = null;

  @JsonProperty("sequence_id")
  private String sequenceId = null;

  @JsonProperty("items")
  List<CollectionItem> itemsInCollection = null;
}

