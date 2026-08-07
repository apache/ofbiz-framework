package org.apache.ofbiz.ws.rs.model;


public class ModelQueryParam {

    private String name;
    private String description;
    private String type;

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @param name
     * @return
     */
    public ModelQueryParam name(String name) {
        this.name = name;
        return this;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }


    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @param description
     * @return
     */
    public ModelQueryParam description(String description) {
        this.description = description;
        return this;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type != null ? type : "string";
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @param type
     * @return
     */
    public ModelQueryParam type(String type) {
        this.type = type;
        return this;
    }

}
