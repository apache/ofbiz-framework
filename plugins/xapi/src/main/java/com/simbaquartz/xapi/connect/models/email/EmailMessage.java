package com.simbaquartz.xapi.connect.models.email;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fidelissd.zcp.xcommon.models.media.File;
import lombok.Data;
import javax.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Represents a Email object.
 **/
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmailMessage {

  @JsonProperty("id")
  private String id = null;

  @JsonProperty("send_from")
  private String sendFrom = null;

  @JsonProperty("to")
  private List<String> sendTo = null;

  @JsonProperty("cc")
  private List<String> sendCc = null;

  @JsonProperty("bcc")
  private List<String> sendBcc = null;

  @NotEmpty(message = "Email subject cannot be blank, please provide a valid email subject.")
  @JsonProperty("subject")
  private String sendSubject = null;

  @JsonProperty("body")
  private String sendEmailBody = null;

  @JsonProperty("attachments")
  private List<File> sendAttachments = null;

}

