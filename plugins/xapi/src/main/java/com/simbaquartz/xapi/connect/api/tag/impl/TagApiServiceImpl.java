package com.simbaquartz.xapi.connect.api.tag.impl;

import com.simbaquartz.xapi.connect.api.common.ApiMessageConstants;
import com.simbaquartz.xapi.connect.api.security.LoggedInUser;
import com.simbaquartz.xapi.connect.api.tag.TagApiService;
import com.simbaquartz.xapi.connect.utils.ApiColorUtils;
import com.simbaquartz.xapi.connect.utils.ApiResponseUtil;
import com.simbaquartz.xapi.connect.models.Tag;
import com.simbaquartz.xapi.connect.models.common.Color;
import com.simbaquartz.xapi.helper.AxProductHelper;
import com.fidelissd.zcp.xcommon.collections.FastMap;
import com.fidelissd.zcp.xcommon.models.search.SearchResults;
import com.fidelissd.zcp.xcommon.util.ColorUtils;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TagApiServiceImpl extends TagApiService {

    private static final String module = TagApiServiceImpl.class.getName();
    private static final Integer VIEW_SIZE = 1000;
    private static final Integer START_INDEX = 0;


    @Override
    public Response getAllTags(String tagTypeId, SecurityContext securityContext) {

        if (Debug.verboseOn())
            Debug.logVerbose("Entering method getAllTags", module);

        LoggedInUser loggedInUser = (LoggedInUser) securityContext.getUserPrincipal();
        LocalDispatcher tenantDispatcher = loggedInUser.getDispatcher();
        String accountPartyId = loggedInUser.getAccountPartyId();

        Map<String, Object> getAllTagsCtx = new HashMap<>();
        if (UtilValidate.isNotEmpty(tagTypeId)) getAllTagsCtx.put("tagTypeId", tagTypeId);
        getAllTagsCtx.put("userLogin", loggedInUser.getUserLogin());

        Map<String, Object> getAllTagsResp = null;
        try {
            getAllTagsResp = tenantDispatcher.runSync("getAllTags", getAllTagsCtx);
        } catch (GenericServiceException e) {
            Debug.logError("An error occurred while invoking getAllTags service, details: " + e.getMessage(), module);
            if (Debug.verboseOn())
                Debug.logVerbose("Exiting method getAllTags", module);

            return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR);
        }

        if (ServiceUtil.isError(getAllTagsResp)) {
            Debug.logError("An error occurred while invoking getAllTags service, details: " + ServiceUtil.getErrorMessage(getAllTagsResp), module);
            if (Debug.verboseOn())
                Debug.logVerbose("Exiting method getAllTags", module);

            return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR);
        }

        List<Map<String, Object>> result = (List<Map<String, Object>>) getAllTagsResp.get("result");

        List<Tag> tags = result.stream()
                        .filter(x -> accountPartyId.equals(x.get("accountPartyId")))
                        .map(x -> Tag.builder()
                            .id((String) x.get("tagId"))
                            .name((String) x.get("tagName"))
                            .type((String) x.get("tagTypeId"))
                            .color(ApiColorUtils.getColor(delegator, (String) x.get("colorId")))
                            .build())
                        .collect(Collectors.toList());

        SearchResults searchResults = new SearchResults();
        searchResults.setRecords(tags);
        searchResults.setViewSize(VIEW_SIZE);
        searchResults.setStartIndex(START_INDEX);
        searchResults.setTotalNumberOfRecords(tags.size());
        Debug.logVerbose("Ending method getAllTags", module);
        return ApiResponseUtil.prepareOkResponse(searchResults);
    }

    @Override
    public Response createTag(Tag tag, SecurityContext securityContext) throws GenericEntityException {

        if (Debug.verboseOn())
            Debug.logVerbose("Entering method createTag", module);

        LoggedInUser loggedInUser = (LoggedInUser) securityContext.getUserPrincipal();
        LocalDispatcher tenantDispatcher = loggedInUser.getDispatcher();
        GenericValue userLogin = loggedInUser.getUserLogin();
        String accountPartyId = loggedInUser.getAccountPartyId();

        if (UtilValidate.isEmpty(tag.getName())) {
            return ApiResponseUtil.prepareDefaultResponse(
                    Response.Status.BAD_REQUEST,
                    ApiMessageConstants.MSG_MISSING_TAG_NAME
            );
        }

        if (UtilValidate.isEmpty(tag.getType())) {
            return ApiResponseUtil.prepareDefaultResponse(
                    Response.Status.BAD_REQUEST,
                    ApiMessageConstants.MSG_MISSING_TAG_TYPE
            );
        }

        GenericValue type = EntityQuery.use(delegator).from("TagType")
                .where("tagTypeId", tag.getType())
                .queryOne();
        if (UtilValidate.isEmpty(type)) {
            return ApiResponseUtil.prepareDefaultResponse(
                    Response.Status.BAD_REQUEST,
                    ApiMessageConstants.MSG_INVALID_TAG_TYPE
            );
        }

        Map<String, Object> createTagCtx = FastMap.newInstance();
        createTagCtx.put("tagName", tag.getName());
        if (UtilValidate.isEmpty(tag.getColorId())) {
            tag.setColorId(Color.defaultColor().getId());
        }
        GenericValue colorValue = ColorUtils.getColor(delegator, tag.getColorId());
        if (UtilValidate.isEmpty(colorValue)) {
            return ApiResponseUtil.prepareDefaultResponse(
                    Response.Status.BAD_REQUEST,
                    ApiMessageConstants.MSG_INVALID_COLOR + tag.getColorId()
            );
        }


        createTagCtx.put("tagTypeId", tag.getType());
        createTagCtx.put("colorId", tag.getColorId());
        createTagCtx.put("accountPartyId", accountPartyId);
        createTagCtx.put("createdByUserLogin", userLogin.getString("userLoginId"));
        createTagCtx.put("lastUpdatedByUserLoginId", userLogin.getString("userLoginId"));
        createTagCtx.put("userLogin", userLogin);

        Map<String, Object> createTagResp = null;
        try {
            createTagResp = tenantDispatcher.runSync("createTag", createTagCtx);
        } catch (GenericServiceException e) {
            Debug.logError("An error occurred while invoking createTag service, details: " + e.getMessage(), module);
            if (Debug.verboseOn())
                Debug.logVerbose("Exiting method createTag", module);

            return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR);
        }

        if (ServiceUtil.isError(createTagResp)) {
            Debug.logError("An error occurred while invoking createTag service, details: " + ServiceUtil.getErrorMessage(createTagResp), module);
            if (Debug.verboseOn())
                Debug.logVerbose("Exiting method createTag", module);

            return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR);
        }

        tag.setId((String) createTagResp.get("tagId"));
        tag.setColor(ApiColorUtils.getColor(delegator, tag.getColorId()));


        Debug.logVerbose("Ending method createTag", module);
        return ApiResponseUtil.prepareOkResponse(tag);

    }

    @Override
    public Response updateTag(String tagId, Tag tag, SecurityContext securityContext) {

        if (Debug.verboseOn())
            Debug.logVerbose("Entering method updateTag", module);

        LoggedInUser loggedInUser = (LoggedInUser) securityContext.getUserPrincipal();
        LocalDispatcher tenantDispatcher = loggedInUser.getDispatcher();
        GenericValue userLogin = loggedInUser.getUserLogin();

        // check tagId for existence.
        if ((!AxProductHelper.isExistingTagId(delegator, tagId))) {
            return ApiResponseUtil.prepareDefaultResponse(
                    Response.Status.BAD_REQUEST,
                    ApiMessageConstants.MSG_INVALID_TAG_ID + tagId
            );
        }

        Map<String, Object> updateTagCtx = FastMap.newInstance();
        updateTagCtx.put("tagId", tagId);
        if(UtilValidate.isNotEmpty(tag.getName())) updateTagCtx.put("tagName", tag.getName());
        if(UtilValidate.isNotEmpty(tag.getColorId())) updateTagCtx.put("colorId", tag.getColorId());
        updateTagCtx.put("lastUpdatedByUserLoginId", userLogin.getString("userLoginId"));
        updateTagCtx.put("userLogin", userLogin);

        Map<String, Object> updateTagResp = null;
        try {
            updateTagResp = tenantDispatcher.runSync("updateTag", updateTagCtx);
        } catch (GenericServiceException e) {
            Debug.logError("An error occurred while invoking updateTag service, details: " + e.getMessage(), module);
            if (Debug.verboseOn())
                Debug.logVerbose("Exiting method updateTag", module);

            return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR);
        }

        if (ServiceUtil.isError(updateTagResp)) {
            Debug.logError("An error occurred while invoking updateTag service, details: " + ServiceUtil.getErrorMessage(updateTagResp), module);
            if (Debug.verboseOn())
                Debug.logVerbose("Exiting method updateTag", module);

            return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR);
        }

        Debug.logVerbose("Ending method updateTag", module);
        return ApiResponseUtil.prepareOkResponse(
                UtilMisc.toMap(
                        "message",
                        "Tag updated successfully!"
                )
        );

    }

    @Override
    public Response deleteTag(String tagId, SecurityContext securityContext) {

        if (Debug.verboseOn())
            Debug.logVerbose("Entering method deleteTag", module);

        LoggedInUser loggedInUser = (LoggedInUser) securityContext.getUserPrincipal();
        LocalDispatcher tenantDispatcher = loggedInUser.getDispatcher();
        GenericValue userLogin = loggedInUser.getUserLogin();
        String accountPartyId = loggedInUser.getAccountPartyId();

        try {
            // Remove Tag from all related entities.
            GenericValue tag = EntityQuery.use(delegator).from("Tag")
                    .where("tagId", tagId, "accountPartyId", accountPartyId)
                    .queryOne();
            if (UtilValidate.isEmpty(tag)) {
                return ApiResponseUtil.prepareDefaultResponse(
                        Response.Status.BAD_REQUEST,
                        ApiMessageConstants.MSG_INVALID_TAG_ID + tagId
                );
            }
            tag.removeRelated("CustRequestTag");
            tag.removeRelated("OrderTags");
        } catch(GenericEntityException e) {
            return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR);
        }
        Map<String, Object> deleteTagCxt = FastMap.newInstance();
        deleteTagCxt.put("tagId", tagId);
        deleteTagCxt.put("userLogin", userLogin);
        Map<String, Object> deleteTagResp = null;
        try {
            deleteTagResp = tenantDispatcher.runSync("deleteTag", deleteTagCxt);
        } catch (GenericServiceException e) {
            Debug.logError("An error occurred while invoking deleteTag service, details: " + e.getMessage(), module);
            if (Debug.verboseOn())
                Debug.logVerbose("Exiting method updateTag", module);

            return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR);
        }

        if (ServiceUtil.isError(deleteTagResp)) {
            Debug.logError("An error occurred while invoking deleteTag service, details: " + ServiceUtil.getErrorMessage(deleteTagResp), module);
            if (Debug.verboseOn())
                Debug.logVerbose("Exiting method createTag", module);

            return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR);
        }

        Debug.logVerbose("Ending method deleteTag", module);
        return ApiResponseUtil.prepareOkResponse(
                UtilMisc.toMap(
                        "message",
                        "Tag removed successfully!"
                )
        );

    }

}
