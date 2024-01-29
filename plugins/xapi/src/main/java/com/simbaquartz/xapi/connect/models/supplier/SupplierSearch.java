package com.simbaquartz.xapi.connect.models.supplier;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Created by Neeraj on 10/7/19.
 */
public class SupplierSearch {

    @JsonProperty("id")
    private String id = null;

    @JsonProperty("name")
    private String supplierName = null;

    @JsonProperty("start_index")
    private Integer startIndex = null;

    @JsonProperty("view_size")
    private Integer viewSize = null;

    @JsonProperty("sort_by")
    private String sortBy = null;



    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * Name of the Supplier group.
     **/
    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName.trim();
    }


    /**
     * Sort by Field name
     *
     * @return
     */
    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }


    /**
     * start index
     **/
    public Integer getStartIndex() {
        return startIndex;
    }

    public void setStartIndex(Integer startIndex) {
        this.startIndex = startIndex;
    }

    /**
     * Customer list size
     **/
    public Integer getViewSize() {
        return viewSize;
    }

    public void setViewSize(Integer viewSize) {
        this.viewSize = viewSize;
    }


}
