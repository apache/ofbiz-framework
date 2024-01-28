package com.simbaquartz.xcommon.models;

import com.simbaquartz.xcommon.collections.FastList;
import java.util.List;
import java.util.Map;
import org.apache.ofbiz.base.util.UtilValidate;

/** Created by mande on 1/3/2022. */
public class WebAddressModelBuilder {
  public static final String module = WebAddressModelBuilder.class.getName();

  public static List<WebAddress> build(List<Map> webAddressMaps) {
    List<WebAddress> webList = FastList.newInstance();
    for (Map webAddress : webAddressMaps) {
      WebAddress web = new WebAddress();
      if (UtilValidate.isNotEmpty(webAddress.get("infoString"))) {
        web.setWebAddress((String) webAddress.get("infoString"));
      }
      if (UtilValidate.isNotEmpty(webAddress.get("contactMechId"))) {
        web.setContactMechId((String) webAddress.get("contactMechId"));
      }
      webList.add(web);
    }
    return webList;
  }

  public static WebAddress build(Map webAddress) {
    WebAddress web = new WebAddress();
    if (UtilValidate.isNotEmpty(webAddress.get("infoString"))) {
      web.setWebAddress((String) webAddress.get("infoString"));
    }
    if (UtilValidate.isNotEmpty(webAddress.get("contactMechId"))) {
      web.setContactMechId((String) webAddress.get("contactMechId"));
    }
    return web;
  }
}
