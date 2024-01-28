package com.simbaquartz.xcommon.models.store;

import java.sql.Timestamp;
import java.util.Map;
import org.apache.ofbiz.base.util.UtilValidate;

public class ProductAttachmentModelBuilder {

  /**
   * Returns Product attachment.
   */
  public static ProductAttachment build(Map productAttachment) {
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

