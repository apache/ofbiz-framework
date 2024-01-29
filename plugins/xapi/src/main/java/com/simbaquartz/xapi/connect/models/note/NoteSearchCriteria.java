package com.simbaquartz.xapi.connect.models.note;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fidelissd.zcp.xcommon.models.search.SearchCriteria;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jboss.resteasy.annotations.jaxrs.QueryParam;


/**
 * For searching Notes
 */
@Data
@EqualsAndHashCode(callSuper=false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class NoteSearchCriteria extends SearchCriteria{
    /**
     * Searches all notes for the given party id
     */
    @JsonProperty("for_id")
    private String notesForPartyId;



}
