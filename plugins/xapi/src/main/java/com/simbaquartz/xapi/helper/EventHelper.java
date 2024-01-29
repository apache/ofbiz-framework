package com.simbaquartz.xapi.helper;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;

public class EventHelper {

  private static final String module = EventHelper.class.getName();

  public static Boolean isExistingEvent(Delegator delegator, String eventId) {
    GenericValue eventRecord = null;

    try {
      eventRecord = EntityQuery.use(delegator).from("Event").where("eventId", eventId).queryOne();
    } catch (GenericEntityException e) {
      Debug.logError(e, module);
    }
    if (UtilValidate.isEmpty(eventRecord)) {
      return false;
    }
    return true;
  }
}
