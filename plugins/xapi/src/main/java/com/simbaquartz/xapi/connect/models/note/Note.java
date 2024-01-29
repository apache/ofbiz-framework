package com.simbaquartz.xapi.connect.models.note;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.simbaquartz.xapi.connect.models.common.Author;
import com.fidelissd.zcp.xcommon.models.account.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;

import java.sql.Timestamp;
import java.util.List;


/**
 * Represents a note. Can be associated with a person, company, order, invoice etc.
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Note {
    /**
     * Unique identifier of a customer note id.
     **/
    @JsonProperty("id")
    private String id;

    /**
     * Text representation of the note.
     */
    @NotEmpty(message = "Please provide the note description")
    @JsonProperty("noteInfo")
    private String noteInfo;

    /**
     * The content of the comment with HTML formatting.
     */
    @JsonProperty("htmlContent")
    private String htmlContent;

    /**
     * Unique identifier of a customer note title.
     **/
    @JsonProperty("title")
    private String noteTitle;

    /**
     * Unique identifier of a customer note name.
     **/
    @JsonProperty("name")
    private String note;

    /**
     * The time when the note was Created, in RFC 3339 format.
     **/
    @JsonProperty("createdAt")
    private Timestamp createdAt;

    /**
     * The time when the note was updated, in RFC 3339 format.
     **/
    @JsonProperty("lastModifiedAt")
    private Timestamp lastModifiedAt;

    /**
     * Who created the note.
     **/
    @JsonProperty("createdBy")
    private User createdBy;
    private User lastModifiedBy;

    /**
     * The author of the note.
     **/
    @JsonProperty("author")
    private Author author;

    @JsonProperty("internalNote")
    private String internalNote;

    /**
     * Whether the note is public or private.
     */
    @JsonProperty("isPublic")
    private String isInternal;

    /**
     * Type of note
     */
    @JsonProperty("note_type_id")
    private String noteTypeId;

    @JsonProperty("orderId")
    private String orderId;

    @JsonProperty("notePartyId")
    private String noteParty;

    @JsonProperty("quote_id")
    private String noteQuoteId;

    @JsonProperty("avatarUrl")
    private String avatarUrl;

    @JsonProperty("notifyAssigness")
    private List<String> notifyAssigness;

    /** List of the users to notify about the note. */
    @JsonProperty("notifyPeople")
    private List<String> notifyPeople;

    /**
     * For storing rich text editor configurations and retrievable states.
     */
    @JsonProperty("jsonText")
    private String jsonText;

}

