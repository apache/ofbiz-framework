package google

import org.codehaus.jackson.map.ObjectMapper;

import org.apache.ofbiz.base.util.UtilMisc
import org.apache.ofbiz.service.ServiceUtil

mapper = new ObjectMapper();

// 1. Check if auto-sync user preferences enabled
Map<String, Object> autoSyncEnabledResp = dispatcher.runSync("isAutoSyncEnabledConditionService", UtilMisc.toMap("userLogin", userLogin));
if(!ServiceUtil.isSuccess(autoSyncEnabledResp)) {
    return "success";
}
boolean isAutoSyncEnabled = autoSyncEnabledResp.get("conditionReply");

// 2. Sync Google events to FSD
int syncEventsCount = 0;
if(isAutoSyncEnabled) {
    Map<String, Object> syncServiceResp = dispatcher.runSync("syncGoogleCalendarToFsdCalendar", UtilMisc.toMap("userLogin", userLogin));
    if(ServiceUtil.isSuccess(syncServiceResp)) {
        syncEventsCount = syncServiceResp.get("syncEventsCount");
    }
}

def output = [:]
output.put("syncEventsCount", syncEventsCount);
mapper.writeValue(response.getWriter(), output );
return "success"
