package com.simbaquartz.xapi.connect.utils;

import com.simbaquartz.xapi.connect.models.common.Color;
import com.fidelissd.zcp.xcommon.util.ColorUtils;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;

/** API level color utils to help populate bean objects. */
public class ApiColorUtils {

  /**
   * Populates and returns the color bean.
   *
   * @param delegator
   * @param colorId
   * @return
   */
  public static Color getColor(Delegator delegator, String colorId) {
    GenericValue colorGv = ColorUtils.getColor(delegator, colorId);
    Color color = Color.builder().build();
    if (UtilValidate.isNotEmpty(colorGv)) {
      color.setId(colorId);
      color.setBackground(colorGv.getString("backgroundColor"));
      color.setForeground(colorGv.getString("foregroundColor"));
    }

    return color;
  }
}
