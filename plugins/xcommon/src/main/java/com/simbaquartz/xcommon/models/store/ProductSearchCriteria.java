package com.simbaquartz.xcommon.models.store;

import com.simbaquartz.xcommon.models.search.BeanSearchCriteria;
import java.math.BigDecimal;
import javax.ws.rs.QueryParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class ProductSearchCriteria extends BeanSearchCriteria {

  @QueryParam("productId")
  private String productId = null;

  @QueryParam("productType")
  private String productType = null;

  @QueryParam("productStatus")
  private String productStatus = null;

  @QueryParam("projectId")
  private String projectId = null;

  @QueryParam("supplierPartyId")
  private String supplierPartyId = null;

  @QueryParam("lowPrice")
  private BigDecimal lowPrice = null;

  @QueryParam("highPrice")
  private BigDecimal highPrice = null;

  @QueryParam("categoryId")
  private String categoryId = null;
}
