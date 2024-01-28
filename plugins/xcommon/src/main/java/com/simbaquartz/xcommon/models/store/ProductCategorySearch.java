package com.simbaquartz.xcommon.models.store;


import com.simbaquartz.xcommon.models.search.BeanSearchCriteria;
import javax.ws.rs.QueryParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class ProductCategorySearch extends BeanSearchCriteria {

    @QueryParam("productCategoryTypeId")
    private String productCategoryTypeId;
}
