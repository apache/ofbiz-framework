package com.fidelissd.zcp.xcommon.models.search;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Map;
import lombok.Data;

import java.util.List;

/**
 * Generic search results modal for use with search related API end points.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchResults {
    @JsonProperty("totalCount")
    private int totalNumberOfRecords;
    @JsonProperty("startIndex")
    private int startIndex;
    @JsonProperty("fetchSize")
    private int viewSize;
    @JsonProperty("records")
    private List<? extends Object> records = new ArrayList<>();

    /**
     * Property to define the totals and calculated amounts
     */
    @JsonProperty("stats")
    private Map<String, ? extends Object> stats;

}
