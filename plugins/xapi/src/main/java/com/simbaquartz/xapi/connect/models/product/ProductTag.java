package com.simbaquartz.xapi.connect.models.product;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by Admin on 9/29/17.
 */
public class ProductTag {
    private String id = null;
    private String name = null;

    /**
     * Unique identifier representing a specific product tag for a given store.
     **/

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


    /**
     * The name of the product tag.
     **/

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
