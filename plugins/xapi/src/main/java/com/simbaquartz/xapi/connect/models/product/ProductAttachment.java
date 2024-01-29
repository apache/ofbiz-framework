package com.simbaquartz.xapi.connect.models.product;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fidelissd.zcp.xcommon.models.media.File;

import java.sql.Timestamp;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.ofbiz.base.util.UtilValidate;

/**
 * Represents a file media for a product. Example product images, supporting documents/brochures etc.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductAttachment extends File {

    @JsonProperty("contentId")
    private String contentId = null;

    @JsonProperty("contentName")
    private String contentName = null;

    @JsonProperty("product_content_type_id")
    private String productContentTypeId = null;

    @JsonProperty("from_date")
    private Timestamp fromDate = null;

  /** Returns Product attachment. */
  public static ProductAttachment buildModel(Map productAttachment) {
    ProductAttachment attachment = new ProductAttachment();
    if (UtilValidate.isNotEmpty(productAttachment)) {
      attachment.setId((String) productAttachment.get("contentId"));
      attachment.setProductContentTypeId((String) productAttachment.get("productContentTypeId"));
      Timestamp fromDate = (Timestamp) productAttachment.get("fromDate");
      if (UtilValidate.isNotEmpty(fromDate)) {
        attachment.setFromDate(fromDate);
      }
      attachment.setFileName((String) productAttachment.get("name"));
      attachment.setProductContentTypeId((String) productAttachment.get("fileType"));
      attachment.setMimeType((String) productAttachment.get("fileType"));
      attachment.setFileExtension((String) productAttachment.get("fileExtension"));
      attachment.setImageWidthInPx((Long) productAttachment.get("imageWidthInPx"));
      attachment.setImageHeightInPx((Long) productAttachment.get("imageHeightInPx"));

      attachment.setDownloadUrl((String) productAttachment.get("downloadUrl"));
      attachment.setThumbnailUrl((String) productAttachment.get("thumbNailUrl"));
      attachment.setSizeFormatted((String) productAttachment.get("formattedFileSize"));
      attachment.setFileSize((Long) productAttachment.get("fileSize"));

    }
    return attachment;
  }


}