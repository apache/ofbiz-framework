package com.simbaquartz.xparty.services.person;


import com.fidelissd.zcp.xcommon.models.geo.TimeZoneList;
import com.fidelissd.zcp.xcommon.models.geo.Timezone;
import java.util.Map;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * Created by mande on 6/20/2021.
 */
public class PersonHelper {
  private static final String module = PersonHelper.class.getName();

  /**
   * Returns timezone for the person.
   * @param personPartyId
   * @param dispatcher
   * @return
   */
  public static Timezone getPersonTimeZone(String personPartyId, LocalDispatcher dispatcher){
    Map getPartyTimezoneResp;
    try {
      getPartyTimezoneResp =
          dispatcher.runSync(
              "getPartyTimezone",
              UtilMisc.toMap("partyId", personPartyId));
    } catch (GenericServiceException e) {
      Debug.logError(
          "An error occurred while invoking getPartyTimezone service, details: " + e.getMessage(),
          "PartyApiServiceImpl");
      if (Debug.verboseOn()) Debug.logVerbose("Exiting method getPartyTimezone", module);

      return null;
    }

    if (ServiceUtil.isError(getPartyTimezoneResp)) {
      Debug.logError(
          "An error occurred while invoking getAllPartyContent service, details: "
              + ServiceUtil.getErrorMessage(getPartyTimezoneResp),
          "PartyApiServiceImpl");
      if (Debug.verboseOn()) Debug.logVerbose("Exiting method getPartyTimezone", module);
      return null;
    }

    Map timeZoneDetails = (Map) getPartyTimezoneResp.get("timeZoneDetails");
    String timeZoneId = null;
    if (UtilValidate.isNotEmpty(timeZoneDetails)) {
      timeZoneId = (String) (timeZoneDetails).get("id");
    }
    // prepare the timezone response bean
    Timezone timezone = new Timezone();
    if (UtilValidate.isNotEmpty(timeZoneId)) {
      timezone = TimeZoneList.getTimezoneModalUsingId(timeZoneId);
    }

    return timezone;
  }

}
