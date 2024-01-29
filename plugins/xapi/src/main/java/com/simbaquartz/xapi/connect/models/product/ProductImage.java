package com.simbaquartz.xapi.connect.models.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents a product's image, stores meta data about the image as well like width, height
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ProductImage  extends ProductAttachment {
  /**
   * Unique identifier representing a specific id for the image.
   **/
  @JsonProperty("id")
  private String id = null;

  /**
   * Unique identifier representing the product associated with the image.
   **/
  @JsonProperty("productId")
  private String productId = null;

  /**
   * The order of the product image in the list. The first product image is at position 1 and is the main image for the product.
   **/
  @JsonProperty("positionIndex")
  private Integer positionIndex = null;

  /**
   * An array of variant ids associated with the image.
   **/
  @JsonProperty("variantIds")
  private List<String> variantIds = new ArrayList<String>();

  /**
   * Specifies the location of the product image.
   **/
  @JsonProperty("src")
  private String src = null;

  @JsonProperty("productIdFrom")
  private String productIdFrom = null;

  @JsonProperty("product_id_to")
  private String productIdTo = null;

  @JsonProperty("is_primary_image")
  private String isPrimaryImage = null;

  @JsonProperty("data_resource_id")
  private String dataResourceId = null;

}

