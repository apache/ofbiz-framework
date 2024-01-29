package com.fidelissd.zcp.xcommon.models.media;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fidelissd.zcp.xcommon.models.CreatedModifiedBy;

import java.sql.Timestamp;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.ofbiz.base.util.UtilValidate;

/**
 * Represents a file stored on the server, media can be of type image/PDF or any other document.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class File extends CreatedModifiedBy {
  @JsonProperty("id")
  private String id = null;

  @JsonProperty("name")
  private String fileName = null;

  @JsonProperty("size")
  private Long fileSize = null;

  @JsonProperty("sizeFormatted")
  private String sizeFormatted = null;

  @JsonProperty("thumbnailUrl")
  private String thumbnailUrl = null;

  @JsonProperty("downloadUrl")
  private String downloadUrl = null;

  @JsonProperty("fileType")
  private String fileType = null;

  /**
   * The type of the document e.g if it is SUPPORTING DOCUMENT or GENERAL DOCUMENT, by default it will be 'GENERAl_DOC'
   * Type id for the customer request document must be one of type ids from {@link CustomerRequestDocumentTypeEnum}
   *
   */
  @JsonProperty("typeId")
  private String typeId = "GENERAL_DOC";

  @JsonProperty("typeDescription")
  private String typeDescription = null;

  /**
   * Extension of the file, example pdf, png
   */
  @JsonProperty("extension")
  private String fileExtension = null;

  @JsonProperty("mimeType")
  private String mimeType = null;

  /**
   * If the file type is image this will be returned, read only.
   */
  @JsonProperty("imageWidthInPx")
  private Long imageWidthInPx = null;
  /**
   * If the file type is image this will be returned, read only.
   */
  @JsonProperty("imageHeightInPx")
  private Long imageHeightInPx = null;

  /**
   * The date of the document when it is active or in general terms issue date of the document
   */
  @JsonProperty("activeSince")
  private Timestamp fromDate;

  /**
   * The date on which the document is going to expire
   */
  @JsonProperty("expiresFrom")
  private Timestamp thruDate;

  /**
   * Flag just to check if the document is active or not
   */
  @JsonProperty("isExpired")
  private Boolean isExpired;

  /** Returns File model populated. */
  public static File buildModel(Map fileDetails) {
    File attachment = new File();
    if (UtilValidate.isNotEmpty(fileDetails)) {
      attachment.setId((String) fileDetails.get("contentId"));
      attachment.setFileName((String) fileDetails.get("fileName"));
      attachment.setMimeType((String) fileDetails.get("fileType"));
      attachment.setFileExtension((String) fileDetails.get("fileExtension"));
      attachment.setImageWidthInPx((Long) fileDetails.get("imageWidthInPx"));
      attachment.setImageHeightInPx((Long) fileDetails.get("imageHeightInPx"));

      attachment.setDownloadUrl((String) fileDetails.get("downloadUrl"));
      attachment.setThumbnailUrl((String) fileDetails.get("thumbNailUrl"));
      attachment.setSizeFormatted((String) fileDetails.get("formattedFileSize"));
      attachment.setFileSize((Long) fileDetails.get("fileSize"));

    }
    return attachment;
  }

}
