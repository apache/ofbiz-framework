package com.fidelissd.zcp.xcommon.models.search;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import java.util.List;

/**
 * Generic search criteria to be used for end points that offer search capabilities.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchCriteria {
    /**
     * Generic search criteria that matches any of the keywords.
     */
    @JsonProperty("keyword")
    private String keyword;

    /**
     * Sort by field, prefix with '-' to signify descending order.
     */
    @JsonProperty("sortBy")
    private String sortBy;

    /**
     * Represents beginning of the record index. For 50 records to fetch last 10 records use
     * startIndex=40 and fetchSize=10
     */
    @JsonProperty("startIndex")
    @QueryParam("startIndex")
    private Integer startIndex = 0;

    /**
     * How many records to be fetched.
     */
    @JsonProperty("fetchSize")
    @QueryParam("fetchSize")
    private Integer viewSize = 10;
}