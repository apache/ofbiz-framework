package com.simbaquartz.xapi.connect.api.globalSearch.model;

import com.fidelissd.zcp.xcommon.models.search.BeanSearchCriteria;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.ws.rs.QueryParam;

@Data
@EqualsAndHashCode(callSuper = false)
public class GlobalSearchCriteria extends BeanSearchCriteria{

    /**
     *  search criteria that matches the type.
     *  Supported types includes CONTACT/DEAL/PROJECT/CUSTOMER/LEAD/TASK
     */
    @QueryParam("type")
    private String docType;
}
