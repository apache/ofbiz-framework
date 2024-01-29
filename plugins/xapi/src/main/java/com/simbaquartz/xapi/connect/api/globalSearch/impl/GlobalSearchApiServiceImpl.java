package com.simbaquartz.xapi.connect.api.globalSearch.impl;

import com.simbaquartz.xapi.connect.api.NotFoundException;
import com.simbaquartz.xapi.connect.api.common.ApiMessageConstants;
import com.simbaquartz.xapi.connect.api.globalSearch.GlobalSearchApiService;
import com.simbaquartz.xapi.connect.api.globalSearch.model.GlobalSearchCriteria;
import com.simbaquartz.xapi.connect.api.globalSearch.model.GlobalSearchRecordTypeEnum;
import com.simbaquartz.xapi.connect.api.security.LoggedInUser;
import com.simbaquartz.xapi.connect.utils.ApiResponseUtil;
import com.fidelissd.zcp.xcommon.models.Phone;
import com.fidelissd.zcp.xcommon.models.geo.PostalAddress;
import com.fidelissd.zcp.xcommon.models.geo.builder.GeoModelBuilder;
import com.simbaquartz.xparty.helpers.AxPartyHelper;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import com.fidelissd.zcp.xcommon.collections.FastList;
import com.fidelissd.zcp.xcommon.collections.FastMap;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.GenericDelegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.party.party.PartyWorker;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;


public class GlobalSearchApiServiceImpl extends GlobalSearchApiService {

    private static final String module = GlobalSearchApiServiceImpl.class.getName();

    @Override
    public Response globalSearch(GlobalSearchCriteria globalSearchCriteria, SecurityContext securityContext) throws NotFoundException {
        Debug.logVerbose("Entering method globalSearch", module);

        if (UtilValidate.isEmpty(globalSearchCriteria) || UtilValidate.isEmpty(globalSearchCriteria.getKeyword())){
            Debug.logError("Enter the query parameter to search for lead, deal, customer, project etc.", module);
            return ApiResponseUtil.prepareDefaultResponse(Response.Status.BAD_REQUEST, ApiMessageConstants.MSG_MISSING_KEYWORD);
        }
        LoggedInUser loggedInUser = (LoggedInUser) securityContext.getUserPrincipal();

        GenericDelegator tenantDelegator = loggedInUser.getDelegator();
        LocalDispatcher tenantDispatcher = loggedInUser.getDispatcher();

        Map<String, Object> globalSearchContext = FastMap.newInstance();
        globalSearchContext.put("userLogin", loggedInUser.getUserLogin());

        String keyword=globalSearchCriteria.getKeyword();
        Integer startIndex=globalSearchCriteria.getStartIndex();
        Integer viewSize=globalSearchCriteria.getViewSize();
        if (UtilValidate.isNotEmpty(keyword)){
            globalSearchContext.put("keyword", keyword);
        }
        if (UtilValidate.isNotEmpty(startIndex)){
            globalSearchContext.put("startIndex", startIndex);
        }
        if (UtilValidate.isNotEmpty(viewSize)){
            globalSearchContext.put("viewSize",viewSize);
        }
        if (UtilValidate.isNotEmpty(globalSearchCriteria.getDocType())){
            globalSearchContext.put("docType",globalSearchCriteria.getDocType());
        }

        Map<String, Object> globalSearchResponse = FastMap.newInstance();
        Map<String, Object> globalSearchResult = FastMap.newInstance();
        List<Map> resultList = FastList.newInstance();

        try {
            //calling the solrGlobalSearch service
            globalSearchResponse = tenantDispatcher.runSync("solrGlobalSearch", globalSearchContext);
        } catch (GenericServiceException e) {
            Debug.logError("An Exception occurred while calling the solrGlobalSearch service" + e.getMessage(), module);
            return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage(), module);
        }

        if (ServiceUtil.isError(globalSearchResponse)){
            Debug.logError("An Error occurred while calling the solrGlobalSearch service" + ServiceUtil.getErrorMessage(globalSearchResponse), module);
            return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR, ServiceUtil.getErrorMessage(globalSearchResponse));
        }
        List<Map> globalSearchResultsList = (List) globalSearchResponse.get("searchResult");


        for(Map resultEntry: globalSearchResultsList) {

            String docType = (String) resultEntry.get("docType");

            if(docType.equalsIgnoreCase(GlobalSearchRecordTypeEnum.CONTACT.roleTypeId) || docType.equalsIgnoreCase(GlobalSearchRecordTypeEnum.LEAD.roleTypeId) || docType.equalsIgnoreCase(GlobalSearchRecordTypeEnum.CUSTOMER.roleTypeId)) {
               String partyId = (String) resultEntry.get("partyId");
                Map<String, Object> contactResult = prepareContactRecord(partyId,resultEntry);
                if (UtilValidate.isNotEmpty(contactResult)){
                    resultList.add(contactResult);
                }
            } else if (docType.equalsIgnoreCase(GlobalSearchRecordTypeEnum.PROJECT.roleTypeId)){
                String projectId = (String) resultEntry.get("projectId");
                Map<String, Object> projectResult = prepareProjectRecord(projectId,resultEntry);
                if (UtilValidate.isNotEmpty(projectResult)){
                    resultList.add(projectResult);
                }
            } else if (docType.equalsIgnoreCase(GlobalSearchRecordTypeEnum.DEAL.roleTypeId)){
                String dealId = (String) resultEntry.get("dealId");
                Map<String, Object> dealResult = prepareDealRecord(dealId,resultEntry);
                if (UtilValidate.isNotEmpty(dealResult)){
                    resultList.add(dealResult);
                }
            }  else if (docType.equalsIgnoreCase(GlobalSearchRecordTypeEnum.TASK.roleTypeId)){
                String taskId = (String) resultEntry.get("taskId");
                Map<String, Object> taskResult = prepareTaskRecord(taskId,resultEntry);
                if (UtilValidate.isNotEmpty(taskResult)){
                    resultList.add(taskResult);
                }
            }
        }

        globalSearchResult.put("results_count",resultList.size());
        globalSearchResult.put("results",resultList);
        globalSearchResult.put("totalDocumentsFound", globalSearchResponse.get("totalDocumentsFound"));
        globalSearchResult.put("filteredContactsCount", globalSearchResponse.get("filteredContactsCount"));
        globalSearchResult.put("filteredLeadCount", globalSearchResponse.get("filteredLeadCount"));
        globalSearchResult.put("filteredCustomersCount", globalSearchResponse.get("filteredCustomersCount"));
        globalSearchResult.put("filteredDealCount", globalSearchResponse.get("filteredDealCount"));
        globalSearchResult.put("filteredProjectsCount", globalSearchResponse.get("filteredProjectsCount"));
        globalSearchResult.put("filteredTasksCount", globalSearchResponse.get("filteredTasksCount"));

        return ApiResponseUtil.prepareOkResponse(globalSearchResult);
    }


    private Map<String, Object> prepareTaskRecord(String taskId,Map resultEntry) {
        Map<String, Object> taskResult = FastMap.newInstance();

        taskResult.put("type",resultEntry.get("docType"));
        if (UtilValidate.isNotEmpty(taskId)){
            taskResult.put("id",taskId);
        }
        String taskName = (String) resultEntry.get("taskName");
        if (UtilValidate.isNotEmpty(taskName)){
            taskResult.put("taskName",taskName);
        }
        Timestamp taskDueDate = (Timestamp) resultEntry.get("taskDueDate");
        if (UtilValidate.isNotEmpty(taskDueDate)){
            taskResult.put("taskDueDate",taskDueDate);
        }
        try {
            String taskStatusId = (String) resultEntry.get("taskStatus");
            if (UtilValidate.isNotEmpty(taskStatusId)) {
                taskResult.put("taskStatusId", taskStatusId);
                GenericValue taskStatus = EntityQuery.use(delegator).from("Enumeration").where("enumId", taskStatusId).queryFirst();
                if(UtilValidate.isNotEmpty(taskStatus)){
                       taskResult.put("taskStatus", taskStatus.get("description"));
                }
            }
        }catch (GenericEntityException e){
            Debug.logError("An Exception occurred while preparing the task record" + e.getMessage(), module);
        }
        return taskResult;
    }

    private Map<String, Object> prepareDealRecord(String dealId,Map resultEntry) {
        Map<String, Object> dealResult = FastMap.newInstance();

        dealResult.put("type", resultEntry.get("docType"));
        if (UtilValidate.isNotEmpty(dealId)){
            dealResult.put("id",dealId);
        }
        String dealName = (String) resultEntry.get("dealName");
        if (UtilValidate.isNotEmpty(dealName)){
            dealResult.put("dealName",dealName);
        }
       if (UtilValidate.isNotEmpty(resultEntry.get("stage"))){
           dealResult.put("stage",resultEntry.get("stage"));
        }
        BigDecimal estimatedAmount = (BigDecimal) resultEntry.get("estimatedAmount");
        if (UtilValidate.isNotEmpty(estimatedAmount)){
            dealResult.put("estimatedAmount",estimatedAmount);
        }
        return dealResult;
    }

    private Map<String, Object> prepareContactRecord(String partyId,Map resultEntry) {
    Map<String, Object> contactResult = FastMap.newInstance();
    contactResult.put("type", resultEntry.get("docType"));
    if (UtilValidate.isNotEmpty(partyId)) {
      contactResult.put("id", partyId);
    }
    if (UtilValidate.isNotEmpty(resultEntry.get("fullName"))) {
      contactResult.put("fullName", resultEntry.get("fullName"));
    }
    if (UtilValidate.isNotEmpty(resultEntry.get("email"))) {
      contactResult.put("email", resultEntry.get("email"));
    }
    GenericValue partyPostalAddress = PartyWorker.findPartyLatestPostalAddress(partyId, delegator);
    if (UtilValidate.isNotEmpty(partyPostalAddress)) {
      PostalAddress address = GeoModelBuilder.buildPostalAddress(partyPostalAddress);
      if (UtilValidate.isNotEmpty(address)) {
        contactResult.put("address", resultEntry.get("address"));
      }
    }
        GenericValue partyPhone = PartyWorker.findPartyLatestTelecomNumber(partyId, delegator);
        if (UtilValidate.isNotEmpty(partyPhone)) {
            Phone phone = new Phone();
            if (UtilValidate.isNotEmpty(partyPhone.get("contactNumber"))) {
                phone.setPhone((String) partyPhone.get("contactNumber"));
            }
            if (UtilValidate.isNotEmpty(partyPhone.get("countryCode"))) {
                phone.setCountryCode((String) partyPhone.get("countryCode"));
            }
            if (UtilValidate.isNotEmpty(partyPhone.get("areaCode"))) {
                phone.setAreaCode((String) partyPhone.get("areaCode"));
            }
            if (UtilValidate.isNotEmpty(partyPhone.get("contactMechId"))) {
                phone.setId((String) partyPhone.get("contactMechId"));
            }
            contactResult.put("phone", phone);
        }
        return contactResult;
    }
    private Map<String, Object> prepareProjectRecord(String projectId,Map resultEntry) {
        Map<String, Object> projectResult = FastMap.newInstance();

        projectResult.put("type", resultEntry.get("docType"));
        if (UtilValidate.isNotEmpty(projectId)){
            projectResult.put("id",projectId);
        }
        String projectName = (String) resultEntry.get("projectName");
        if (UtilValidate.isNotEmpty(projectName)){
            projectResult.put("projectName",projectName);
        }
        Timestamp projectStartDate = (Timestamp) resultEntry.get("projectStartDate");
        if (UtilValidate.isNotEmpty(projectStartDate)){
            projectResult.put("projectStartDate",projectStartDate);
        }
        Timestamp projectEndDate = (Timestamp) resultEntry.get("projectEndDate");
        if (UtilValidate.isNotEmpty(projectEndDate)){
            projectResult.put("projectEndDate",projectEndDate);
        }
        String projectOwnerId = (String) resultEntry.get("projectOwner");
        if (UtilValidate.isNotEmpty(projectOwnerId)){
            Map<String, Object> projectOwner= FastMap.newInstance();
            projectOwner.put("id",projectOwnerId);
            projectOwner.put("name", AxPartyHelper.getPartyName(delegator,projectOwnerId));
            projectOwner.put("email",AxPartyHelper.getPartyPrimaryEmail(delegator,projectOwnerId));
            projectResult.put("projectOwner",projectOwner);
        }
        return projectResult;
    }
}
