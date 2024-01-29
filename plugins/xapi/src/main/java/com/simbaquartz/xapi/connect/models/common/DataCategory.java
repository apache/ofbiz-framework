package com.simbaquartz.xapi.connect.models.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.simbaquartz.xapi.connect.api.security.LoggedInUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.GenericDelegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.LocalDispatcher;
import java.util.Map;
import org.apache.ofbiz.base.util.UtilValidate;

/**
 * Represents a Category object with Color object insted it that can be used with tasks/projects
 * etc.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataCategory {
    /** Parent Category */
    @JsonProperty("parent")
    private DataCategory parent;

    /**
     * A unique id for the Category
     */
    @JsonProperty("id")
    private String id;

    /**
     * Name of the category
     */
    @JsonProperty("name")
    private String name;

    /**
     * {@link Color} object for the category
     */
    @JsonProperty("color")
    private Color color;

    /**
     * @param partyCategory GenericValue of entity name PartyCategoryAndColor
     * @param dispatcher
     * @param delegator
     * @param loggedInUser
     * @return
     */
    public static DataCategory prepareCategoryForParty(
            GenericValue partyCategory,
            LocalDispatcher dispatcher,
            GenericDelegator delegator,
            LoggedInUser loggedInUser) {
        DataCategory category = new DataCategory();
        if (UtilValidate.isNotEmpty(partyCategory)) {
            category.setId(partyCategory.getString("categoryId"));
            String categoryName = partyCategory.getString("categoryOverride");
            //      If category name is empty try fetching from DataCategory.
            if (UtilValidate.isEmpty(categoryName)) {
                categoryName = partyCategory.getString("categoryName");
            }
            category.setName(categoryName);

            Color color = Color.builder().build();
            color.setId(partyCategory.getString("colorId"));
            color.setBackground(partyCategory.getString("backgroundColor"));
            color.setBackground(partyCategory.getString("foregroundColor"));
            category.setColor(color);
        }

        return category;
    }
 


  /**
   * Populates and returns the model for DataCategory.
   *
   * @param dataCategoryMap
   * @return
   */
  public static DataCategory buildModel(Map dataCategoryMap) {
    DataCategory category = new DataCategory();
    if (UtilValidate.isNotEmpty(dataCategoryMap)) {
      category.setId((String) dataCategoryMap.get("dataCategoryId"));
      category.setName((String) dataCategoryMap.get("categoryName"));
      String parentCategoryId = (String) dataCategoryMap.get("parentCategoryId");
      if (UtilValidate.isNotEmpty(parentCategoryId)) {
        DataCategory parent = new DataCategory();
        parent.setId(parentCategoryId);
        category.setParent(parent);
      }
      Map colorDetails = (Map) dataCategoryMap.get("color");
      Color color = Color.builder().build();
      if (UtilValidate.isNotEmpty(colorDetails)) {

        color.setId((String) colorDetails.get("id"));
        color.setBackground((String) colorDetails.get("backgroundColor"));
        color.setForeground((String) colorDetails.get("foregroundColor"));
      } else {
        color = Color.defaultColor();
      }
      category.setColor(color);
    }

    return category;
  }
}
