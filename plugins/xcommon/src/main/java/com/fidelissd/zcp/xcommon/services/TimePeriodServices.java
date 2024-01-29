package com.fidelissd.zcp.xcommon.services;

import java.sql.Time;
import java.util.List;
import java.util.Map;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;

/** Created by mande on 7/11/2021. */
public class TimePeriodServices {
  public static final String module = TimePeriodServices.class.getName();

  /**
   * Create a time period entry.
   *
   * @param dctx
   * @param context
   * @return
   */
  public static Map<String, Object> createTimePeriod(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    String startDayOfWeek = (String) context.get("startDayOfWeek");
    Time startTime = (Time) context.get("startTime");
    String endDayOfWeek = (String) context.get("endDayOfWeek");
    Time endTime = (Time) context.get("endTime");
    String forPartyId = (String) context.get("forPartyId");
    String forProductStoreId = (String) context.get("forProductStoreId");

    try {
      GenericValue timePeriod = delegator.makeValue("TimePeriod");
      String timePeriodId = delegator.getNextSeqId("TimePeriod");
      timePeriod.set("timePeriodId", timePeriodId);
      timePeriod.set("forPartyId", forPartyId);
      timePeriod.set("forProductStoreId", forProductStoreId);
      timePeriod.set("startDayOfWeek", startDayOfWeek);
      timePeriod.set("startTime", startTime);
      timePeriod.set("endDayOfWeek", endDayOfWeek);
      timePeriod.set("endTime", endTime);

      delegator.create(timePeriod);
      serviceResult.put("timePeriodId", timePeriodId);
    } catch (GenericEntityException e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }

    return serviceResult;
  }

  public static Map<String, Object> updateTimePeriod(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    String timePeriodId = (String) context.get("timePeriodId");
    String startDayOfWeek = (String) context.get("startDayOfWeek");
    Time startTime = (Time) context.get("startTime");
    String endDayOfWeek = (String) context.get("endDayOfWeek");
    Time endTime = (Time) context.get("endTime");
    String forPartyId = (String) context.get("forPartyId");
    String forProductStoreId = (String) context.get("forProductStoreId");

    try {
      GenericValue timePeriod =
          EntityQuery.use(delegator)
              .from("TimePeriod")
              .where("timePeriodId", timePeriodId)
              .queryOne();

      if (UtilValidate.isNotEmpty(timePeriod)) {
        if (UtilValidate.isNotEmpty(forProductStoreId)) {
          timePeriod.set("forProductStoreId", forProductStoreId);
        }
        if (UtilValidate.isNotEmpty(forPartyId)) {
          timePeriod.set("forPartyId", forPartyId);
        }
        if (UtilValidate.isNotEmpty(startDayOfWeek)) {
          timePeriod.set("startDayOfWeek", startDayOfWeek);
        }
        if (UtilValidate.isNotEmpty(startTime)) {
          timePeriod.set("startTime", startTime);
        }
        if (UtilValidate.isNotEmpty(endDayOfWeek)) {
          timePeriod.set("endDayOfWeek", endDayOfWeek);
        }
        if (UtilValidate.isNotEmpty(endTime)) {
          timePeriod.set("endTime", endTime);
        }
        delegator.store(timePeriod);
      }

    } catch (GenericEntityException e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }

    return serviceResult;
  }

  public static Map<String, Object> deleteTimePeriod(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    String timePeriodId = (String) context.get("timePeriodId");
    try {
      GenericValue timePeriod =
          EntityQuery.use(delegator)
              .from("TimePeriod")
              .where("timePeriodId", timePeriodId)
              .queryOne();

      if (UtilValidate.isNotEmpty(timePeriod)) {
        delegator.removeValue(timePeriod);
      }

    } catch (GenericEntityException e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }

    return serviceResult;
  }

  public static Map<String, Object> deleteAllTimePeriodsForParty(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    String forPartyId = (String) context.get("forPartyId");
    try {
      List<GenericValue> timePeriodsToDelete =
          EntityQuery.use(delegator)
              .from("TimePeriod")
              .where("forPartyId", forPartyId)
              .queryList();

      if (UtilValidate.isNotEmpty(timePeriodsToDelete)) {
        delegator.removeAll(timePeriodsToDelete);
      }

    } catch (GenericEntityException e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }

    return serviceResult;
  }

  public static Map<String, Object> deleteAllTimePeriodsForProductStore(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    String forProductStoreId = (String) context.get("forProductStoreId");
    try {
      List<GenericValue> timePeriodsToDelete =
          EntityQuery.use(delegator)
              .from("TimePeriod")
              .where("forProductStoreId", forProductStoreId)
              .queryList();

      if (UtilValidate.isNotEmpty(timePeriodsToDelete)) {
        delegator.removeAll(timePeriodsToDelete);
      }

    } catch (GenericEntityException e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }

    return serviceResult;
  }
}
