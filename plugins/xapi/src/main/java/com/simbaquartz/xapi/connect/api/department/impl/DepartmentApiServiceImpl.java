package com.simbaquartz.xapi.connect.api.department.impl;

import com.simbaquartz.xapi.connect.api.NotFoundException;
import com.simbaquartz.xapi.connect.api.department.DepartmentApiService;
import com.fidelissd.zcp.xcommon.models.company.department.Department;
import com.fidelissd.zcp.xcommon.models.company.department.DepartmentModelBuilder;
import com.fidelissd.zcp.xcommon.models.company.department.DepartmentSerachBean;
import com.simbaquartz.xapi.connect.api.security.LoggedInUser;
import com.simbaquartz.xapi.connect.utils.ApiResponseUtil;
import com.fidelissd.zcp.xcommon.collections.FastList;
import com.fidelissd.zcp.xcommon.collections.FastMap;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.ServiceUtil;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.List;
import java.util.Map;

public class DepartmentApiServiceImpl extends DepartmentApiService {
  private static final String module = DepartmentApiServiceImpl.class.getName();

  @Override
  public Response createDepartment(Department department, SecurityContext securityContext)
          throws NotFoundException {
    if (Debug.verboseOn())
      Debug.logVerbose("Entering method individual createDepartment", module);

    LoggedInUser loggedInUser = (LoggedInUser) securityContext.getUserPrincipal();
    GenericValue userLogin = loggedInUser.getUserLogin();

    Map<String, Object> departmentCtx = prepareDepartmentCtx(department);
    departmentCtx.put("userLogin", userLogin);
    departmentCtx.put("orgGroupPartyId", loggedInUser.getAccountPartyId());

    Map createDepartmentResp = null;
    try {
      if (Debug.verboseOn())
        Debug.logVerbose("Invoking service createDepartment with input context : " + departmentCtx, module);

      createDepartmentResp = dispatcher.runSync("createDepartment", departmentCtx);
    } catch (GenericServiceException e) {
      //handle error here
      Debug.logError("An error occured while invoking createDepartment service, details: " + e.getMessage(), module);
      if (Debug.verboseOn())
        Debug.logVerbose("Exiting method createDepartment", module);

      return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR);
    }
    if (ServiceUtil.isError(createDepartmentResp)) {
      //handle error here
      String serviceError = ServiceUtil.getErrorMessage(createDepartmentResp);
      Debug.logError("An error occured while creating department, details: " + serviceError, module);
      if (Debug.verboseOn())
        Debug.logVerbose("Exiting method createDepartmentResp", module);

      return ApiResponseUtil.prepareDefaultResponse(Response.Status.BAD_REQUEST, serviceError);
    }
    return ApiResponseUtil.prepareOkResponse(createDepartmentResp);
  }

  private Map<String,Object> prepareDepartmentCtx(Department department){
    Map<String,Object> departmentCtx= FastMap.newInstance();
    departmentCtx.put("name",department.getName());
    if(UtilValidate.isNotEmpty(department.getOwner()))
      departmentCtx.put("ownerPartyId",department.getOwner().getId());
    departmentCtx.put("parentDepartmentId",department.getParentDepartmentId());
    departmentCtx.put("description",department.getDescription());
    departmentCtx.put("members",department.getMembers());
    return  departmentCtx;
  }

  @Override
  public Response getDepartments(DepartmentSerachBean departmentSerachBean,SecurityContext securityContext)
          throws NotFoundException {
    if (Debug.verboseOn())
      Debug.logVerbose("Entering method getDepartments", module);

    LoggedInUser loggedInUser = (LoggedInUser) securityContext.getUserPrincipal();
    GenericValue userLogin = loggedInUser.getUserLogin();

    Map<String, Object> departmentCtx = FastMap.newInstance();
    departmentCtx.put("userLogin", userLogin);
    departmentCtx.put("orgGroupPartyId", loggedInUser.getAccountPartyId());
    departmentCtx.put("startIndex",departmentSerachBean.getStartIndex());
    departmentCtx.put("viewSize",departmentSerachBean.getViewSize());
    departmentCtx.put("keyword",departmentSerachBean.getKeyword());

    Map getDepartmentsResp = null;
    try {
      if (Debug.verboseOn())
        Debug.logVerbose("Invoking service getDepartments with input context : " + departmentCtx, module);

      getDepartmentsResp = dispatcher.runSync("getCompanyDepartments", departmentCtx);
    } catch (GenericServiceException e) {
      //handle error here
      Debug.logError("An error occured while invoking getDepartments service, details: " + e.getMessage(), module);
      if (Debug.verboseOn())
        Debug.logVerbose("Exiting method getDepartments", module);

      return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR);
    }
    if (ServiceUtil.isError(getDepartmentsResp)) {
      //handle error here
      String serviceError = ServiceUtil.getErrorMessage(getDepartmentsResp);
      Debug.logError("An error occured while fetching department, details: " + serviceError, module);
      if (Debug.verboseOn())
        Debug.logVerbose("Exiting method getDepartments", module);

      return ApiResponseUtil.prepareDefaultResponse(Response.Status.BAD_REQUEST, serviceError);
    }
    List<Department> departments=FastList.newInstance();
    Map responseMap=FastMap.newInstance();
    long totalCount=0;
    if(UtilValidate.isNotEmpty(getDepartmentsResp) && UtilValidate.isNotEmpty(getDepartmentsResp.get("departments"))) {
      departments = DepartmentModelBuilder.build((List) getDepartmentsResp.get("departments"));
      totalCount= (long) getDepartmentsResp.get("resultSize");
    }
    responseMap.put("totalCount", totalCount);
    responseMap.put("departments",departments);
    return ApiResponseUtil.prepareOkResponse(responseMap);
  }

  @Override
  public Response getDepartment(String departmentId, SecurityContext securityContext)
          throws NotFoundException {
    if (Debug.verboseOn())
      Debug.logVerbose("Entering method getDepartment", module);

    LoggedInUser loggedInUser = (LoggedInUser) securityContext.getUserPrincipal();
    GenericValue userLogin = loggedInUser.getUserLogin();

    Map<String, Object> departmentCtx = FastMap.newInstance();
    departmentCtx.put("userLogin", userLogin);
    departmentCtx.put("departmentId",departmentId);

    Map getDepartmentsResp = null;
    try {
      if (Debug.verboseOn())
        Debug.logVerbose("Invoking service getDepartment with input context : " + departmentCtx, module);

      getDepartmentsResp = dispatcher.runSync("getCompanyDepartment", departmentCtx);
    } catch (GenericServiceException e) {
      //handle error here
      Debug.logError("An error occured while invoking getDepartment service, details: " + e.getMessage(), module);
      if (Debug.verboseOn())
        Debug.logVerbose("Exiting method getDepartment", module);

      return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR);
    }
    if (ServiceUtil.isError(getDepartmentsResp)) {
      //handle error here
      String serviceError = ServiceUtil.getErrorMessage(getDepartmentsResp);
      Debug.logError("An error occured while fetching department, details: " + serviceError, module);
      if (Debug.verboseOn())
        Debug.logVerbose("Exiting method getDepartment", module);

      return ApiResponseUtil.prepareDefaultResponse(Response.Status.BAD_REQUEST, serviceError);
    }
    Department department = new Department();
    if(UtilValidate.isNotEmpty(getDepartmentsResp) && UtilValidate.isNotEmpty(getDepartmentsResp.get("departmentDetails"))){
      department = DepartmentModelBuilder.build((Map)getDepartmentsResp.get("departmentDetails"));
    }
    return ApiResponseUtil.prepareOkResponse(department);
  }

  @Override
  public Response updateDepartment(String departmentId, Department department, SecurityContext securityContext)
          throws NotFoundException {
    if (Debug.verboseOn())
      Debug.logVerbose("Entering method updateDepartment", module);

    LoggedInUser loggedInUser = (LoggedInUser) securityContext.getUserPrincipal();
    GenericValue userLogin = loggedInUser.getUserLogin();

    Map<String, Object> departmentCtx = prepareDepartmentCtx(department);
    departmentCtx.put("userLogin", userLogin);
    departmentCtx.put("departmentId",departmentId);

    Map updateDepartmentResp = null;
    try {
      if (Debug.verboseOn())
        Debug.logVerbose("Invoking service updateDepartment with input context : " + departmentCtx, module);

      updateDepartmentResp = dispatcher.runSync("updateCompanyDepartment", departmentCtx);
    } catch (GenericServiceException e) {
      //handle error here
      Debug.logError("An error occurred while invoking updateDepartment service, details: " + e.getMessage(), module);
      if (Debug.verboseOn())
        Debug.logVerbose("Exiting method updateDepartment", module);

      return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR);
    }
    if (ServiceUtil.isError(updateDepartmentResp)) {
      //handle error here
      String serviceError = ServiceUtil.getErrorMessage(updateDepartmentResp);
      Debug.logError("An error occured while fetching department, details: " + serviceError, module);
      if (Debug.verboseOn())
        Debug.logVerbose("Exiting method getDepartment", module);

      try {
        dispatcher.runSync("populateBasicInformationForParty", UtilMisc.toMap("partyId",departmentId , "overrideExistingValues", true, "userLogin", loggedInUser.getUserLogin()));
      } catch (GenericServiceException e) {
        Debug.logError(e, module);
        return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR);
      }

      return ApiResponseUtil.prepareDefaultResponse(Response.Status.BAD_REQUEST, serviceError);
    }


    return ApiResponseUtil.prepareOkResponse(updateDepartmentResp);
  }
}