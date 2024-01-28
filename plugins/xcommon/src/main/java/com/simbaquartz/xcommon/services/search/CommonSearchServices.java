package com.simbaquartz.xcommon.services.search;

import com.simbaquartz.xcommon.services.CommonHelper;
import com.simbaquartz.xcommon.util.JsonUtils;
import java.net.URLDecoder;
import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityFindOptions;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * Common search related services, save/update/fetch saved searches.
 */
public class CommonSearchServices {

  private static final String module = CommonSearchServices.class.getName();

  /**
   * Adds a new or updates SavedSearch entry. Also checks if an matching searchQueryJson exists, if
   * found returns the existing record details instead.
   */
  public static Map<String, Object> createUpdateSavedSearch(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String searchId = (String) context.get("searchId");
    String name = (String) context.get("name");
    String isPublic = (String) context.get("isPublic");
    String searchQueryUrl = (String) context.get("searchQueryUrl");
    String searchQueryJson = (String) context.get("searchQueryJson");
    String typeId = (String) context.get("typeId");
    String organizationId = (String) context.get("organizationId");

    boolean createMode = true;

    GenericValue savedSearchRec = null;

    try {
      // validate if an existing search matching the criteria already exists
      if (UtilValidate.isNotEmpty(searchQueryUrl)) {
        // getting sorted query string
        searchQueryUrl = CommonHelper.getSortedQueryString(searchQueryUrl);

        // get sorted query map
        Map sortedQueryMap = CommonHelper.getSortedQueryMap(searchQueryUrl);
        searchQueryJson = JsonUtils.toJson(sortedQueryMap);
        EntityCondition entityCondition = EntityCondition.makeCondition(
            EntityCondition.makeCondition("searchQueryUrl", searchQueryUrl),
            EntityCondition.makeCondition(EntityCondition.makeCondition(
                "ownerPartyId", userLogin.getString("partyId")), EntityOperator.OR,
                EntityCondition.makeCondition(
                    "isPublic", "Y"
                )));

        savedSearchRec =
            EntityQuery.use(delegator)
                .from("SavedSearch")
                .where(entityCondition)
                .queryFirst();
        if (UtilValidate.isNotEmpty(savedSearchRec)) {
          searchId = savedSearchRec.getString("searchId");
          Debug.logInfo(
              "Found an existing search matching the input criteria, returning the found record instead.",
              module);
          serviceResult.put("searchId", searchId);
          serviceResult.put("savedSearch", savedSearchRec);

          //          return serviceResult;
        }
      }

      if (UtilValidate.isEmpty(searchId)) {
        searchId = delegator.getNextSeqId("SavedSearch");
        savedSearchRec = delegator.makeValue("SavedSearch");
      } else {
        createMode = false; // update mode
      }

      savedSearchRec.set("searchId", searchId);
      savedSearchRec.set("name", name);
      savedSearchRec.set("isPublic", isPublic);
      savedSearchRec.set("searchQueryUrl", searchQueryUrl);
      savedSearchRec.set("searchQueryJson", searchQueryJson);
      savedSearchRec.set("typeId", typeId);
      savedSearchRec.set("organizationId", organizationId);

      String loggedInUserId = userLogin.getString("userLoginId");

      Timestamp now = UtilDateTime.nowTimestamp();
      if (createMode) {
        savedSearchRec.set("createdDate", now);
        savedSearchRec.set("createdByUserLogin", loggedInUserId);
        savedSearchRec.set("ownerPartyId", userLogin.getString("partyId"));
      }

      savedSearchRec.set("lastModifiedDate", now);
      savedSearchRec.set("lastModifiedByUserLogin", loggedInUserId);

      delegator.createOrStore(savedSearchRec);

    } catch (Exception e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }

    serviceResult.put("searchId", searchId);
    serviceResult.put("savedSearch", savedSearchRec);
    return serviceResult;
  }

  public static Map<String, Object> searchSavedSearches(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    LocalDispatcher dispatcher = dctx.getDispatcher();
    Delegator delegator = dctx.getDelegator();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String ownerPartyId = (String) context.get("ownerPartyId");
    String isPublic = (String) context.get("isPublic");
    String sortBy = (String) context.get("sortBy");

    String query = (String) context.get("query");
    String searchId = (String) context.get("searchId");
    String keyword = (String) context.get("keyword");
    String typeId = (String) context.get("typeId");
    Integer viewSize = (Integer) context.get("viewSize");
    Integer startIndex = (Integer) context.get("startIndex");
    String organizationId = (String) context.get("organizationId");

    List<GenericValue> savedSearchRecords;
    long resultSize;

    try {

      if (UtilValidate.isNotEmpty(query)) {

        // decoding query string
        query = URLDecoder.decode(query, "UTF-8");
        query = CommonHelper.getSortedQueryString(query);
      }

      if (viewSize == null || viewSize < 0) {
        viewSize = 100;
      }

      if (startIndex == null || startIndex <= 0) {
        startIndex = 0;
      }

      int lowIndex = startIndex + 1;
      int highIndex = (startIndex) + viewSize;

      EntityFindOptions efo = new EntityFindOptions();
      efo.setMaxRows(highIndex);
      efo.setResultSetType(EntityFindOptions.TYPE_SCROLL_INSENSITIVE);
      efo.setDistinct(true);

      // search using entity query
      List<EntityCondition> mainCondList = new LinkedList<EntityCondition>();

      if (UtilValidate.isNotEmpty(organizationId)) {
        mainCondList.add(
            EntityCondition.makeCondition("organizationId", EntityOperator.EQUALS, organizationId));
      }

      if (UtilValidate.isNotEmpty(searchId)) {
        mainCondList.add(
            EntityCondition.makeCondition("searchId", EntityOperator.EQUALS, searchId));
      }
      if (UtilValidate.isNotEmpty(typeId)) {
        mainCondList.add(EntityCondition.makeCondition("typeId", EntityOperator.EQUALS, typeId));
      }
      if (UtilValidate.isNotEmpty(query)) {
        mainCondList.add(
            EntityCondition.makeCondition("searchQueryUrl", EntityOperator.EQUALS, query));
      }
      if (UtilValidate.isNotEmpty(ownerPartyId)) {
        mainCondList.add(
            EntityCondition.makeCondition(
                UtilMisc.toList(
                    EntityCondition.makeCondition(
                        "ownerPartyId", EntityOperator.EQUALS, ownerPartyId),
                    EntityCondition.makeCondition("isPublic", EntityOperator.EQUALS, "Y")),
                EntityOperator.OR));
      }

      EntityCondition mainCond = EntityCondition.makeCondition(mainCondList, EntityOperator.AND);
      EntityQuery eq =
          EntityQuery.use(delegator)
              .from("SavedSearch")
              .where(mainCond)
              .orderBy(sortBy)
              .cursorScrollInsensitive()
              .maxRows(highIndex);

      try (EntityListIterator pli = eq.queryIterator()) {
        savedSearchRecords = pli.getPartialList(lowIndex, viewSize);
        resultSize = pli.getResultsSizeAfterPartialList();
      }
    } catch (Exception e) {
      Debug.logError(e, e.getMessage(), module);
      return ServiceUtil.returnError(e.getMessage());
    }

    serviceResult.put("savedSearches", savedSearchRecords);
    serviceResult.put("resultSize", resultSize);
    return serviceResult;
  }
}
