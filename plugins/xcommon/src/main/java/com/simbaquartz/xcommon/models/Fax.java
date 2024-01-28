package com.simbaquartz.xcommon.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;


/**
 * Represents a store fax, which can have one or more cards on file associated with it.
 **/

@JsonIgnoreProperties(ignoreUnknown = true)
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2017-05-09T10:47:15.854-07:00")
public class Fax {

    private String id = null;

    private String fax = null;
    private String countryCode = null;
    private String areaCode = null;
    private String extension = null;
    private List faxPurposes = null;

    private String createdAt = null;
    private String createdAtPretty = null;
    private String updatedAt = null;
    private String updatedAtPretty = null;


    /**
     * Unique identifier of a fax.
     **/

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


    /**
     * The fax's fax number in raw format (7 digit).
     **/
    @JsonProperty("fax")
    public String getFax() {
        return fax;
    }
    public void setFax(String fax) {
        this.fax = fax;
    }

    /**
     * The fax's fax countryCode.
     **/

    @JsonProperty("country_code")
    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    /**
     * The fax's fax areaCode.
     **/

    @JsonProperty("area_code")
    public String getAreaCode() {
        return areaCode;
    }

    public void setAreaCode(String areaCode) {
        this.areaCode = areaCode;
    }

    /**
     * The fax's fax Purposes.
     **/

    @JsonProperty("fax_purposes")
    public List getFaxPurposes() {
        return faxPurposes;
    }

    public void setFaxPurposes(List faxPurposes) {
        this.faxPurposes = faxPurposes;
    }

    /**
     * The fax's extension.
     **/

    @JsonProperty("extension")
    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }


    /**
     * The time when the fax was created, in RFC 3339 format.
     **/

    @JsonProperty("created_at")
    public String getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * The time when the fax was last updated, in RFC 3339 format.
     **/

    @JsonProperty("updated_at")
    public String getUpdatedAt() {
        return updatedAt;
    }
    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Created at date in pretty format
     * @return
     */
    @JsonProperty("created_at_pretty")
    public String getCreatedAtPretty() {return createdAtPretty;}
    public void setCreatedAtPretty(String createdAtPretty) {this.createdAtPretty = createdAtPretty;}

    /**
     * Last Updated at date in pretty format
     * @return
     */
    @JsonProperty("updated_at_pretty")
    public String getUpdatedAtPretty() {return updatedAtPretty;}
    public void setUpdatedAtPretty(String updatedAtPretty) {this.updatedAtPretty = updatedAtPretty;}



    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Fax fax = (Fax) o;
        return Objects.equals(id, fax.id) &&
                Objects.equals(fax, fax.fax) &&
                Objects.equals(countryCode, fax.countryCode) &&
                Objects.equals(areaCode, fax.areaCode) &&
                Objects.equals(createdAt, fax.createdAt) &&
                Objects.equals(updatedAt, fax.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, fax, countryCode, areaCode, createdAt, updatedAt);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Fax {\n");

        sb.append("    id: ").append(toIndentedString(id)).append("\n");
        sb.append("    fax: ").append(toIndentedString(fax)).append("\n");
        sb.append("    areaCode: ").append(toIndentedString(areaCode)).append("\n");
        sb.append("    countryCode: ").append(toIndentedString(countryCode)).append("\n");
        sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
        sb.append("    updatedAt: ").append(toIndentedString(updatedAt)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}