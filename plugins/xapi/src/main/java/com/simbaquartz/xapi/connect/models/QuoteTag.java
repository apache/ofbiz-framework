package com.simbaquartz.xapi.connect.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotEmpty;

import java.sql.Timestamp;


/**
 * Created by Neeraj on 8/10/19.
 */
public class QuoteTag {

    @JsonProperty("tag_id")
    private String tagId = null;

    @NotEmpty(message = "Please provide a Tag Name")
    @JsonProperty("tag_name")
    private String tagName = null;

    @NotEmpty(message = "Please provide a Tag color code")
    @JsonProperty("tag_color_code")
    private String tagColorCode = null;

    @JsonProperty("created_time_stamp")
    private Timestamp createdTimeStamp = null;

    @JsonProperty("last_updated_time_stamp")
    private Timestamp lastUpdatedTimeStamp = null;


    /**
     * Unique identifier representing a specific quote tag.
     **/

    public String getTagId() {
        return tagId;
    }
    public void setTagId(String tagId) {
        this.tagId = tagId;
    }


    /**
     * The name of the quote tag.
     **/
    public String getTagName() {
        return tagName;
    }
    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    /**
     * The color code of the quote tag.
     **/
    public String getTagColorCode() {
        return tagColorCode;
    }
    public void setTagColorCode(String tagColorCode) {
        this.tagColorCode = tagColorCode;
    }


    /**
     * created time stamp for the quote tag.
     **/
    public Timestamp getCreatedTimeStamp() {
        return createdTimeStamp;
    }

    public void setCreatedTimeStamp(Timestamp createdTimeStamp) {
        this.createdTimeStamp = createdTimeStamp;
    }

    /**
     * last updated time stamp for the quote tag.
     **/
    public Timestamp getLastUpdatedTimeStamp() {
        return lastUpdatedTimeStamp;
    }

    public void setLastUpdatedTimeStamp(Timestamp lastUpdatedTimeStamp) {
        this.lastUpdatedTimeStamp = lastUpdatedTimeStamp;
    }
}
