package com.simbaquartz.xapi.services.tag;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TagServices {

    public static final String module = TagServices.class.getName();

    public static Map<String, Object> getAllTags(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        List<Map<String, Object>> tagDetails = new LinkedList<>();
        String tagTypeId = (String) context.get("tagTypeId");

        try {

            List<GenericValue> tags;
            if (UtilValidate.isNotEmpty(tagTypeId)) {
                tags = EntityQuery.use(delegator).from("Tag").where("tagTypeId", tagTypeId).queryList();
            } else {
                tags = EntityQuery.use(delegator).from("Tag").queryList();
            }

            for (GenericValue tag : tags) {
                tagDetails.add(
                        UtilMisc.toMap(
                                "tagId", tag.getString("tagId"),
                                "tagTypeId", tag.getString("tagTypeId"),
                                "tagName", tag.getString("tagName"),
                                "colorId", tag.getString("colorId"),
                                "accountPartyId", tag.getString("accountPartyId")
                        )
                );
            }
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        serviceResult.put("result", tagDetails);
        return serviceResult;
    }

}
