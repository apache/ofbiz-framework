package com.simbaquartz.xcommon.util;

import java.util.ArrayList;
import java.util.List;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;

/** Color related utility methods. */
public class ColorUtils {
  private static final String module = ColorUtils.class.getName();
  private static final String MASTER_COLOR_PALETTE_ID = "MASTER_CLR_PLT";

  /**
   * Returns the color entity object @GenericValue, uses cache. {@code <Color colorId="blue"
   * backgroundColor="#039be5" foregroundColor="#ffffff"/>}
   *
   * @param delegator
   * @param colorId
   * @return @GenericValue object of the color resource if found.
   */
  public static GenericValue getColor(Delegator delegator, String colorId) {
    GenericValue color = null;

    try {
      color =
          EntityQuery.use(delegator).from("Color").where("colorId", colorId).cache(true).queryOne();
    } catch (GenericEntityException e) {
      Debug.logError(e, module);
    }

    return color;
  }

  /**
   * Returns the list of colors from the master color palette. For master color palette
   * @see plugins/xcommon/data/XcommonTypeData.xml
   *
   * @param delegator
   * @return List<GenericValue>
   */
  public static List<GenericValue> getMasterColors(Delegator delegator) {
    List<GenericValue> masterColors = new ArrayList<>();
    try {
      masterColors =
          EntityQuery.use(delegator)
              .from("ColorPaletteAndColor")
              .where("paletteId", MASTER_COLOR_PALETTE_ID)
              .orderBy("sequenceId")
              .cache(true)
              .queryList();
    } catch (GenericEntityException e) {
      Debug.logError(e, module);
    }

    return masterColors;
  }
}
