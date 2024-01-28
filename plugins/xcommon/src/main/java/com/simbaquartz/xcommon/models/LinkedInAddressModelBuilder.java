package com.simbaquartz.xcommon.models;

import com.simbaquartz.xcommon.collections.FastList;
import java.util.List;
import java.util.Map;
import org.apache.ofbiz.base.util.UtilValidate;

/** Created by mande on 1/3/2022. */
public class LinkedInAddressModelBuilder {
  public static final String module = LinkedInAddressModelBuilder.class.getName();

  public static List<LinkedInAddress> build(List<Map> linkedInAddressMaps) {
    List<LinkedInAddress> linkedInList = FastList.newInstance();
    for (Map linkedInAddress : linkedInAddressMaps) {
      LinkedInAddress linkedIn = new LinkedInAddress();
      if (UtilValidate.isNotEmpty(linkedInAddress.get("infoString"))) {
        linkedIn.setLinkedInAddress((String) linkedInAddress.get("infoString"));
      }
      if (UtilValidate.isNotEmpty(linkedInAddress.get("contactMechId"))) {
        linkedIn.setContactMechId((String) linkedInAddress.get("contactMechId"));
      }
      linkedInList.add(linkedIn);
    }
    return linkedInList;
  }
}
