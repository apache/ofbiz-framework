package com.simbaquartz.xapi.connect.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.sql.Timestamp;
import java.util.Map;

import com.fidelissd.zcp.xcommon.models.CreatedModifiedBy;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents a client's content information.
 **/
@Data
@EqualsAndHashCode(callSuper=false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Content extends CreatedModifiedBy {

    @JsonProperty("id")
    private String id = null;

    @JsonProperty("typeId")
    private String typeId = null;

    @JsonProperty("isVerified")
    private String isVerified = null;

    @JsonProperty("documentType")
    private String documentType = null;

    @JsonProperty("mimeTypeId")
    private String mimeTypeId = null;

    @JsonProperty("createdByParty")
    private ContentCreator createdByParty = null;

    @JsonProperty("name")
    private String name = null;

    @JsonProperty("createdDate")
    private Timestamp createdDate = null;

    @JsonProperty("fileSize")
    private Long fileSize = null;

    @JsonProperty("isPublic")
    private Boolean isPublic = null;

    @JsonProperty("formattedFileSize")
    private String formattedFileSize = null;

    @JsonProperty("url")
    private String url = null;

    @JsonProperty("comment")
    private String comment = null;

    @JsonProperty("isRejected")
    private String isRejected = null;

    @JsonProperty("verifiedBy")
    private String verifiedBy = null;

    @JsonProperty("verificationDate")
    private Timestamp verificationDate = null;

    @JsonProperty("type")
    private Map<String,Object> type;

    @JsonProperty("subType")
    private Map<String,Object> subType;

    @JsonProperty("documentCategory")
    private Map<String,Object> documentCategory;

    @JsonProperty("description")
    private String description = null;

    @JsonProperty("documentDate")
    private Timestamp documentDate = null;

    @JsonProperty("documentNumber")
    private String documentNumber = null;

    @JsonProperty("encodedFile")
    private String encodedFile = null;

    @JsonProperty("subTypeId")
    private String subTypeId = null;

    @JsonProperty("media_type")
    private String mediaType = null;

    @JsonProperty("productContentTypeId")
    private String productContentTypeId = null;

    @JsonProperty("isModifiedByAdmin")
    private Boolean isModifiedByAdmin = null;

    @JsonProperty("serialNumber")
    private String serialNumber = null;

    @JsonProperty("typeLabel")
    private String typeLabel = null;

    /** The content creator. Read-only. */
    @Data
    public static final class ContentCreator {
        /** Identifier of the creator. Represents the creators party id. */
        @JsonProperty("id")
        private String id;

        /** Display name of the creator. */
        @JsonProperty("displayName")
        private String displayName;

        /** Email of the creator. */
        @JsonProperty("email")
        private String email;

        /** Photo Url name of the creator. */
        @JsonProperty("photoUrl")
        private String photoUrl;

        /**
         * Whether the creator corresponds to the calendar on which this copy of the event appears.
         * Read-only. The default is False. The value may be {@code null}.
         */
        @JsonProperty("self")
        private Boolean self = false;
    }
}