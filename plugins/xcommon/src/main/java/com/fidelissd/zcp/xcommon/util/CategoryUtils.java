package com.fidelissd.zcp.xcommon.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;

/** Category related utility methods. */
public class CategoryUtils {
  private static final String module = CategoryUtils.class.getName();

  /**
   * Returns the DataCategory entity object @GenericValue, uses cache. Use for task category details
   *
   * @param delegator
   * @param categoryId
   * @return @Map object of the {id:categoryId, name:categoryName, parentCategoryId :
   *     parentCategoryId, color: {id:colorId, backgroundColor: backgroundColor, foregroundColor :
   *     foregroundColor}}if found.
   */
  public static Map getCategoryDetails(Delegator delegator, String categoryId) {
    Map categoryAndColorDetails = UtilMisc.toMap("id", categoryId);

    try {

      GenericValue categoryAndColor =
          EntityQuery.use(delegator)
              .from("DataCategoryAndColor")
              .where("dataCategoryId", categoryId)
              .cache(true)
              .queryOne();

      if (UtilValidate.isNotEmpty(categoryAndColor)) {
        // prepare task category bean
        categoryAndColorDetails.put("name", categoryAndColor.getString("categoryName"));
        categoryAndColorDetails.put("categoryName", categoryAndColor.getString("categoryName"));
        categoryAndColorDetails.put(
            "parentCategoryId", categoryAndColor.getString("parentCategoryId"));

        String categoryColorId = categoryAndColor.getString("colorId");
        if (UtilValidate.isNotEmpty(categoryColorId)) {
          categoryAndColorDetails.put(
              "color",
              UtilMisc.toMap(
                  "id",
                  categoryColorId,
                  "backgroundColor",
                  categoryAndColor.getString("backgroundColor"),
                  "foregroundColor",
                  categoryAndColor.getString("foregroundColor")));
        }
      }

    } catch (GenericEntityException e) {
      Debug.logError(e, module);
    }

    return categoryAndColorDetails;
  }

  /**
   * Returns the ProjectCategoryAndColor entity object @GenericValue, uses cache. Use for project
   * category details.
   *
   * @param delegator
   * @param categoryId
   * @param projectId
   * @return @Map object of the {id:categoryId, name:categoryName, parentCategoryId :
   *     parentCategoryId, color: {id:colorId, backgroundColor: backgroundColor, foregroundColor :
   *     foregroundColor}}if found.
   */
  public static Map getCategoryDetails(Delegator delegator, String categoryId, String projectId) {
    Map categoryAndColorDetails = UtilMisc.toMap("id", categoryId);

    try {
      GenericValue categoryAndColor =
          EntityQuery.use(delegator)
              .from("ProjectCategoryAndColor")
              .where("categoryId", categoryId, "projectId", projectId)
              .cache(true)
              .queryOne();

      if (UtilValidate.isNotEmpty(categoryAndColor)) {
        // prepare task category bean
        categoryAndColorDetails.put("name", categoryAndColor.getString("categoryName"));
        categoryAndColorDetails.put("categoryName", categoryAndColor.getString("categoryName"));
        categoryAndColorDetails.put(
            "parentCategoryId", categoryAndColor.getString("parentCategoryId"));

        String categoryColorId = categoryAndColor.getString("colorId");
        if (UtilValidate.isNotEmpty(categoryColorId)) {
          categoryAndColorDetails.put(
              "color",
              UtilMisc.toMap(
                  "id",
                  categoryColorId,
                  "backgroundColor",
                  categoryAndColor.getString("backgroundColor"),
                  "foregroundColor",
                  categoryAndColor.getString("foregroundColor")));
        }
      } else {
        // try fetching using category details
        return getCategoryDetails(delegator, categoryId);
      }

    } catch (GenericEntityException e) {
      Debug.logError(e, module);
    }

    return categoryAndColorDetails;
  }

  /**
   * Use this to fetch datacategories with a parentcategoryId, like TASK_CATEGORY
   *
   * @param delegator
   * @param parentCategoryId
   * @return
   */
  public static List<Map> getCategories(Delegator delegator, String parentCategoryId) {
    List<Map> categoriesWithColor = new ArrayList<>();

    try {

      List<GenericValue> categoriesAndColor =
          EntityQuery.use(delegator)
              .from("DataCategoryAndColor")
              .where("parentCategoryId", parentCategoryId)
              .cache(true)
              .queryList();

      for (GenericValue categoryAndColor : categoriesAndColor) {
        String dataCategoryId = categoryAndColor.getString("dataCategoryId");
        Map categoryAndColorDetails =
            UtilMisc.toMap("id", dataCategoryId, "dataCategoryId", dataCategoryId);
        // prepare task category bean
        categoryAndColorDetails.put("name", categoryAndColor.getString("categoryName"));
        categoryAndColorDetails.put("categoryName", categoryAndColor.getString("categoryName"));
        categoryAndColorDetails.put(
            "parentCategoryId", categoryAndColor.getString("parentCategoryId"));

        String categoryColorId = categoryAndColor.getString("colorId");
        if (UtilValidate.isNotEmpty(categoryColorId)) {
          categoryAndColorDetails.put(
              "color",
              UtilMisc.toMap(
                  "id",
                  categoryColorId,
                  "backgroundColor",
                  categoryAndColor.getString("backgroundColor"),
                  "foregroundColor",
                  categoryAndColor.getString("foregroundColor")));
        }

        categoriesWithColor.add(categoryAndColorDetails);
      }

    } catch (GenericEntityException e) {
      Debug.logError(e, module);
    }

    return categoriesWithColor;
  }
}
