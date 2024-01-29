package com.simbaquartz.xapi.helper;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;

public class AxProjectItemHelper {
    private static final String module = AxProjectItemHelper.class.getName();

    public static Boolean isExistingAssignee(Delegator delegator, String projectItemId, String partyId) {
        GenericValue projectItemRole = null;
        try {
            projectItemRole = EntityQuery.use(delegator).from("ProjectItemRole").where("projectItemId", projectItemId, "partyId", partyId, "roleTypeId", "ASSIGNEE").filterByDate().queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        if (UtilValidate.isEmpty(projectItemRole)) {
            return false;
        }
        return true;
    }

    /**
     * To get category color based on categoryId.
     * @param delegator
     * @param taskCategoryId
     * @return
     * @throws GenericEntityException
     */
    public static String getTaskCategoryColor(Delegator delegator, String taskCategoryId) throws GenericEntityException {

        GenericValue categories = EntityQuery.use(delegator).from("DataCategory")
                .where("parentCategoryId", "TASK_CATEGORY", "dataCategoryId", taskCategoryId).queryOne();

        String colorId="";

        if (UtilValidate.isNotEmpty(categories)){

            colorId=(String)categories.get("colorId");
        }

        return colorId;
    }

}
