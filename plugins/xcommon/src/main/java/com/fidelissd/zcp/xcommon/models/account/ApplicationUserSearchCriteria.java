package com.fidelissd.zcp.xcommon.models.account;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fidelissd.zcp.xcommon.models.search.BeanSearchCriteria;
import javax.ws.rs.QueryParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents an application user search criteria.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper=false)
public class ApplicationUserSearchCriteria extends BeanSearchCriteria {
    /**
     * Email address associated with the application user account.
     */
    @QueryParam("email")
    private String email = null;

    /**
     * Name of the application account user. E.g. John Doe
     */
    @QueryParam("name")
    private String name = null;
}