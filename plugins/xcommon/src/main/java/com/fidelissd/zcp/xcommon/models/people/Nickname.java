package com.fidelissd.zcp.xcommon.models.people;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * A person's nickname.
 * Ref: https://developers.google.com/people/api/rest/v1/people#Person.Nickname
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Nickname {
    /**
     * The nickname
     */
    @JsonProperty("value")
    private String value = null;

    /**
     * The nickname of the nickname
     */
    @JsonProperty("type")
    private NicknameTypeEnum type = null;

}
