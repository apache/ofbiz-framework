

package com.fidelissd.zcp.xcommon.models.media;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LinkUrl {
    /**
     * Unique identifier for the link.
     */
    @JsonProperty("id")
    private String id;

    /**
     * URL represented by the url, e.g. https://www.google.com
     */
    @JsonProperty("url")
    private String url;

    /**
     * Description for the URL, example "Example Website" for https://www.example.com
     */
    @JsonProperty("description")
    private String description;

    /**
     * Type of url, one of
     * web (default)
     * linkedin
     * facebook
     * twitter
     */
    @JsonProperty("type")
    private String type = "web";

}
