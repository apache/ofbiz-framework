package com.simbaquartz.xparty.services.recent;

import java.sql.Timestamp;
import java.util.Map;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.ServiceUtil;

public class RecentlyViewedServices {

  /**
   * Creates or updates an existing resource view entity record.
   * Seed data is managed in plugins/xparty/data/XpartyTypeData.xml
   * @param dctx
   * @param context
   * @return
   * @throws GenericEntityException
   * @throws GenericServiceException
   */
  public static Map<String, Object> createOrUpdateResourceViewHistory(
      DispatchContext dctx, Map<String, ? extends Object> context)
      throws GenericEntityException, GenericServiceException {

    Delegator delegator = dctx.getDelegator();
    String userLoginId = (String) context.get("userLoginId");
    String resourceTypeId = (String) context.get("resourceTypeId");
    String resourceId = (String) context.get("resourceId");
    Timestamp lastViewedAt = (Timestamp) context.get("lastViewedAt");

    GenericValue resourceViewHistory = delegator.makeValue("ResourceViewHistory");
    resourceViewHistory.set("userLoginId", userLoginId);
    resourceViewHistory.set("resourceTypeId", resourceTypeId);
    resourceViewHistory.set("resourceId", resourceId);
    resourceViewHistory.set("lastViewedAt", lastViewedAt);

    delegator.createOrStore(resourceViewHistory);
    return ServiceUtil.returnSuccess();
  }
}
