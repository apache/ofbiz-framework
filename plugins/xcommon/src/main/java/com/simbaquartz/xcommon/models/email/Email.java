package com.simbaquartz.xcommon.models.email;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.simbaquartz.xcommon.models.CreatedModifiedBy;
import com.simbaquartz.xcommon.models.media.File;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Email extends CreatedModifiedBy {

    @JsonProperty("id")
    private String id = null;

    @JsonProperty("body")
    private String body = null;

    @JsonProperty("body_html")
    private String bodyHtml = null;

    @JsonProperty("subject")
    private String subject = null;

    /**
     * Send from email address, must be verified email address
     */
    @JsonProperty("send_from")
    private EmailAddress sendFrom = null;

    /**
     * Optional reply to email address, if none provided uses the send from. Must be a verified email.
     */
    @JsonProperty("reply_to")
    private EmailAddress replyTo = null;

    /**
     * List of To recipients
     */
    @JsonProperty("send_to")
    private List<EmailAddress> sendTo;

    /**
     * List of Cc recipients
     */
    @JsonProperty("send_cc")
    private List<EmailAddress> sendCc;

    /**
     * List of Bcc recipients
     */
    @JsonProperty("send_bcc")
    private List<EmailAddress> sendBcc;

    /**
     * If true, delivery tracking will be enabled by inserting tracking pixel in the email body.
     */
    @JsonProperty("track_delivery")
    private boolean trackDelivery;

    /**
     * Scheduled to be sent at, must be in the future.
     */
    @JsonProperty("schedule_send_at")
    private Timestamp sendScheduledAt;

    /**
     * Sent at
     */
    @JsonProperty("sent_at")
    private Timestamp sentAt;

    /**
     * Delivered at
     */
    @JsonProperty("delivered_at")
    private Timestamp deliveredAt;

    /**
     * List of attachments
     */
    @JsonProperty("attachments")
    private List<File> attachments;

    @JsonProperty("server_root_url")
    private String serverRootUrl = null;

    /**
     * Ids for association
     */
    @JsonProperty("association_ids")
    private Map<String, Object> associationIds = null;
}