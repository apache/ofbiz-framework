package com.fidelissd.zcp.xcommon.models.store;


import com.fidelissd.zcp.xcommon.models.search.BeanSearchCriteria;

import javax.ws.rs.QueryParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class ProductCategorySearch extends BeanSearchCriteria {

    @QueryParam("productCategoryTypeId")
    private String productCategoryTypeId;
}
