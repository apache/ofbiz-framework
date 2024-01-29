package com.simbaquartz.xapi.connect.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by Admin on 9/29/17.
 */
public class CopyQuote {

    private String copyQuoteRoles = null;
    private String copyQuoteAttributes = null;
    private String copyQuoteCoefficients = null;
    private String copyQuoteAdjustments = null;
    private String copyQuoteShipGroups = null;
    private String copyQuoteNotes = null;
    private String copyQuoteContents = null;
    private String copyQuoteItems = null;



    @JsonProperty("copy_quote_roles")
    public String getCopyQuoteRoles() {
        return copyQuoteRoles;
    }

    public void setCopyQuoteRoles(String copyQuoteRoles) {
        this.copyQuoteRoles = copyQuoteRoles;
    }



    @JsonProperty("copy_quote_attributes")
    public String getCopyQuoteAttributes() {
        return copyQuoteAttributes;
    }

    public void setCopyQuoteAttributes(String copyQuoteAttributes) {
        this.copyQuoteAttributes = copyQuoteAttributes;
    }


    @JsonProperty("copy_quote_coefficients")
    public String getCopyQuoteCoefficients() {
        return copyQuoteCoefficients;
    }

    public void setCopyQuoteCoefficients(String copyQuoteCoefficients) {
        this.copyQuoteCoefficients = copyQuoteCoefficients;
    }


    @JsonProperty("copy_quote_items")
    public String getCopyQuoteItems() {
        return copyQuoteItems;
    }

    public void setCopyQuoteItems(String copyQuoteItems) {
        this.copyQuoteItems = copyQuoteItems;
    }

    @JsonProperty("copy_quote_adjustments")
    public String getCopyQuoteAdjustments() {
        return copyQuoteAdjustments;
    }

    public void setCopyQuoteAdjustments(String copyQuoteAdjustments) {
        this.copyQuoteAdjustments = copyQuoteAdjustments;
    }


    @JsonProperty("copy_quote_notes")
    public String getCopyQuoteNotes() {
        return copyQuoteNotes;
    }

    public void setCopyQuoteNotes(String copyQuoteNotes) {
        this.copyQuoteNotes = copyQuoteNotes;
    }


    @JsonProperty("copy_quote_ship_groups")
    public String getCopyQuoteShipGroups() {
        return copyQuoteShipGroups;
    }

    public void setCopyQuoteShipGroups(String copyQuoteShipGroups) {
        this.copyQuoteShipGroups = copyQuoteShipGroups;
    }


    @JsonProperty("copy_quote_contents")
    public String getCopyQuoteContents() {
        return copyQuoteContents;
    }

    public void setCopyQuoteContents(String copyQuoteContents) {
        this.copyQuoteContents = copyQuoteContents;
    }
    
}
