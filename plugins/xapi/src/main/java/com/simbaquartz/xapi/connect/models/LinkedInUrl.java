package com.simbaquartz.xapi.connect.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotEmpty;


@JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2017-05-09T10:47:15.854-07:00")
public class LinkedInUrl {

    @NotEmpty(message = "Please provide a Linked In Url")
    @JsonProperty("linked_in_url")
    private String linkedInUrl = null;

    @JsonProperty("contact_mech_id")
    private String contactMechId = null;

    public String getLinkedInUrl() {
        return linkedInUrl;
    }

    public void setLinkedInUrl(String linkedInUrl) {
        this.linkedInUrl = linkedInUrl;
    }

    public String getContactMechId() {
        return contactMechId;
    }

    public void setContactMechId(String contactMechId) {
        this.contactMechId = contactMechId;
    }


}
