package com.simbaquartz.xcommon.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;


@JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2017-05-09T10:47:15.854-07:00")
public class LinkedInAddress {

    @JsonProperty("linkedIn_address")
    private String linkedInAddress = null;

    @JsonProperty("contact_mech_id")
    private String contactMechId = null;

    /**
     * linkedIn Address
     **/

    public String getLinkedInAddress() {
        return linkedInAddress;
    }

    public void setLinkedInAddress(String linkedInAddress) {
        this.linkedInAddress = linkedInAddress;
    }

    /**
     * Unique identifier of an email Address.
     **/
    public String getContactMechId() {
        return contactMechId;
    }

    public void setContactMechId(String contactMechId) {
        this.contactMechId = contactMechId;
    }


}