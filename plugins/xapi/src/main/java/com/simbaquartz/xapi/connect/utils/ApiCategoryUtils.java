package com.simbaquartz.xapi.connect.utils;

import com.simbaquartz.xapi.connect.models.common.Color;
import com.simbaquartz.xapi.connect.models.common.DataCategory;
import com.fidelissd.zcp.xcommon.util.CategoryUtils;
import java.util.Map;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;

/** API level Category utils to help populate bean objects. */
public class ApiCategoryUtils {

  /**
   * Populates and returns the {{@link com.simbaquartz.xapi.connect.models.common.DataCategory}}
   * bean.
   *
   * @param delegator
   * @param categoryId
   * @param projectId if provided fetches project category details.
   * @return
   */
  public static DataCategory getDataCategory(
      Delegator delegator, String categoryId, String projectId) {

    DataCategory dataCategory = new DataCategory();

    Map categoryAndColorDetails;

    if (UtilValidate.isNotEmpty(projectId)) {
      categoryAndColorDetails = CategoryUtils.getCategoryDetails(delegator, categoryId, projectId);
      if (UtilValidate.isEmpty(categoryAndColorDetails)) {
        categoryAndColorDetails = CategoryUtils.getCategoryDetails(delegator, categoryId);
      }
    } else {
      categoryAndColorDetails = CategoryUtils.getCategoryDetails(delegator, categoryId);
    }

    if (UtilValidate.isNotEmpty(categoryAndColorDetails)) {
      dataCategory.setId(categoryId);
      dataCategory.setName((String) categoryAndColorDetails.get("name"));

      Map colorMap = (Map) categoryAndColorDetails.get("color");
      Color color = Color.builder().build();
      if (UtilValidate.isNotEmpty(colorMap)) {
        color.setId((String) colorMap.get("id"));
        color.setBackground((String) colorMap.get("backgroundColor"));
        color.setForeground((String) colorMap.get("foregroundColor"));
      }

      dataCategory.setColor(color);
    }

    return dataCategory;
  }

  public static DataCategory getDataCategory(Delegator delegator, String categoryId) {
    return getDataCategory(delegator, categoryId, null);
  }
}
