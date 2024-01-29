package com.fidelissd.zcp.xcommon.models.search;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import lombok.Data;

/**
 * Generic search criteria to be used for end points that offer search capabilities.
 * Use this to search using query parameters and not the parameters posted in the body of the request.
 *
 * Prefer using this over SearchCriteria, all searches should be GET based.
 */
@Data
public class BeanSearchCriteria {
    /**
     * Generic search criteria that matches any of the keywords.
     */
    @QueryParam("keyword")
    private String keyword;

    /**
     * Sort by field, prefix with '-' to signify descending order.
     */
    @QueryParam("sortBy")
    private String sortBy;

    /**
     * Represents beginning of the record index. For 50 records to fetch last 10 records use
     * startIndex=40 and fetchSize=10
     */
    @QueryParam("startIndex")
    @DefaultValue("0")
    private Integer startIndex = 0;

    /**
     * How many records to be fetched. Defaults to 10 records per page.
     */
    @QueryParam("fetchSize")
    @DefaultValue("10")
    private Integer viewSize = 10;
}