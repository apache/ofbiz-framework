package com.simbaquartz.xapi.connect.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CategorySearch {

    private String id = null;
    private String name = null;
    private String description = null;
    private String createdAfter = null;
    private String createdBefore = null;
    private Integer startIndex = null;
    private Integer viewSize = null;
    private String sortBy = null;

    /**
     * Unique identifier of a category.
     **/

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * The category name.
     **/

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * The category description.
     **/

    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Sort by Field name
     * @return
     */
    public String getSortBy() {return sortBy;}
    public void setSortBy(String sortBy) {this.sortBy = sortBy;}

    /**
     * The customer's created after date.
     **/
    @JsonProperty("created_after")
    public String getCreatedAfter() {
        return createdAfter;
    }
    public void setCreatedAfter(String createdAfter) {
        this.createdAfter = createdAfter;
    }

    /**
     * The customer's created before date.
     **/
    @JsonProperty("created_before")
    public String getCreatedBefore() {
        return createdBefore;
    }
    public void setCreatedBefore(String createdBefore) {
        this.createdBefore = createdBefore;
    }

    /**
     * start index
     **/
    @JsonProperty("start_index")
    public Integer getStartIndex() {
        return startIndex;
    }
    public void setStartIndex(Integer startIndex) {
        this.startIndex = startIndex;
    }

    /**
     * Customer list size
     **/
    @JsonProperty("view_size")
    public Integer getViewSize() {
        return viewSize;
    }
    public void setViewSize(Integer viewSize) {
        this.viewSize = viewSize;
    }

}
