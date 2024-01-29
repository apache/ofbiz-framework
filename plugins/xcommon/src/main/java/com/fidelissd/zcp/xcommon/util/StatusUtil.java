package com.fidelissd.zcp.xcommon.util;

import java.util.Map;

import com.fidelissd.zcp.xcommon.models.Color;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;

/**
 * Offers StatusItem utilities, to get status name, id, color etc.
 */
public class StatusUtil {
  private static final String module = StatusUtil.class.getName();

  public static Map getStatusDetails(Delegator delegator, String statusId){
    Map statusDetails = null;

    try{
      GenericValue statusItemAndColor = EntityQuery.use(delegator).from("StatusItemAndColor").where("statusId", statusId).cache(true).queryOne();

      if(UtilValidate.isNotEmpty(statusItemAndColor)){
        statusDetails = UtilMisc.toMap(
            "statusId", statusId,
            "statusDescription", statusItemAndColor.getString("description"),
            "sequenceId", statusItemAndColor.getString("sequenceId"),
            "statusCode", statusItemAndColor.getString("statusCode")
            );

        String colorId = statusItemAndColor.getString("colorId");
        if(UtilValidate.isEmpty(colorId)){
          colorId = Color.defaultColor().getId();
        }
        statusDetails.put("colorId", colorId);

        String backgroundColor = statusItemAndColor.getString("backgroundColor");
        if(UtilValidate.isEmpty(backgroundColor)){
          backgroundColor = Color.defaultColor().getBackground();
        }
        statusDetails.put("backgroundColor", backgroundColor);

        String foregroundColor = statusItemAndColor.getString("foregroundColor");
        if(UtilValidate.isEmpty(foregroundColor)){
          foregroundColor = Color.defaultColor().getBackground();
        }
        statusDetails.put("foregroundColor", foregroundColor);
      }
    }catch (GenericEntityException e){
      Debug.logError(e, module);
    }
    return statusDetails;
  }
}
