/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.ofbiz.content;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpSession;

import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.cache.UtilCache;
import org.ofbiz.content.content.ContentServices;
import org.ofbiz.content.content.ContentWorker;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericPK;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.model.ModelUtil;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.security.Security;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceAuthException;
import org.ofbiz.service.ServiceUtil;

import com.ibm.icu.util.Calendar;

/**
 * ContentManagementServices Class
 */
public class ContentManagementServices {

    public static final String module = ContentManagementServices.class.getName();
    public static final String resource = "ContentUiLabels";

    /**
     * getSubContent
     * Finds the related subContent given the template Content and the mapKey.
     * This service calls a same-named method in ContentWorker to do the work.
     */
    public static Map<String, Object> getSubContent(DispatchContext dctx, Map<String, ? extends Object> context) {
        //Security security = dctx.getSecurity();
        Delegator delegator = dctx.getDelegator();
        //LocalDispatcher dispatcher = dctx.getDispatcher();
        String contentId = (String) context.get("contentId");
        String subContentId = (String) context.get("subContentId");
        String mapKey = (String) context.get("mapKey");
        GenericValue userLogin = (GenericValue)context.get("userLogin");
        Timestamp fromDate = (Timestamp)context.get("fromDate");
        List<String> assocTypes = UtilGenerics.checkList(context.get("assocTypes"));
        String assocTypesString = (String)context.get("assocTypesString");
        if (UtilValidate.isNotEmpty(assocTypesString)) {
            List<String> lst = StringUtil.split(assocTypesString, "|");
            if (assocTypes == null) {
                assocTypes = FastList.newInstance();
            }
            assocTypes.addAll(lst);
        }
        GenericValue content = null;
        GenericValue view = null;

        try {
            view = ContentWorker.getSubContentCache(delegator, contentId, mapKey, subContentId, userLogin, assocTypes, fromDate, Boolean.FALSE, null);
            content = ContentWorker.getContentFromView(view);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.toString());
        }

        Map<String, Object> results = ServiceUtil.returnSuccess();
        results.put("view", view);
        results.put("content", content);
        return results;
    }

    /**
     * getContent
     * This service calls a same-named method in ContentWorker to do the work.
     */
    public static Map<String, Object> getContent(DispatchContext dctx, Map<String, ? extends Object> context) {
        //Security security = dctx.getSecurity();
        Delegator delegator = dctx.getDelegator();
        String contentId = (String) context.get("contentId");
        //GenericValue userLogin = (GenericValue)context.get("userLogin");
        GenericValue view = null;

        try {
            view = ContentWorker.getContentCache(delegator, contentId);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.toString());
        }

        Map<String, Object> results = ServiceUtil.returnSuccess();
        results.put("view", view);
        return results;
    }

    /**
     * addMostRecent
     * A service for adding the most recently used of an entity class to the cache.
     * Entities make it to the most recently used list primarily by being selected for editing,
     * either by being created or being selected from a list.
     */
    public static Map<String, Object> addMostRecent(DispatchContext dctx, Map<String, ? extends Object> context) {
        //Security security = dctx.getSecurity();
        //Delegator delegator = dctx.getDelegator();
        //LocalDispatcher dispatcher = dctx.getDispatcher();
        //HttpServletRequest request = (HttpServletRequest)context.get("request");
        //String suffix = (String) context.get("suffix");
        GenericValue val = (GenericValue)context.get("pk");
        GenericPK pk = val.getPrimaryKey();
        HttpSession session = (HttpSession)context.get("session");

        ContentManagementWorker.mruAdd(session, pk);
        return ServiceUtil.returnSuccess();
    }


    /**
     * persistContentAndAssoc
     * A combination method that will create or update all or one of the following:
     * a Content entity, a ContentAssoc related to the Content, and
     * the ElectronicText that may be associated with the Content.
     * The keys for determining if each entity is created is the presence
     * of the contentTypeId, contentAssocTypeId and dataResourceTypeId.
     * This service tries to handle DataResource fields with and
     * without "dr" prefixes.
     * Assumes binary data is always in field, "imageData".
     *
     * This service does not accept straight ContentAssoc parameters. They must be prefaced with "ca" + cap first letter
     */
    public static Map<String, Object> persistContentAndAssoc(DispatchContext dctx, Map<String, ? extends Object> rcontext) throws GenericServiceException {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Map<String, Object> context = UtilMisc.makeMapWritable(rcontext);
        Locale locale = (Locale) context.get("locale");

        Debug.logInfo("=========== type:" + (String)context.get("dataresourceTypeId") , module);
        // Knowing why a request fails permission check is one of the more difficult
        // aspects of content management. Setting "displayFailCond" to true will
        // put an html table in result.errorMessage that will show what tests were performed
        Boolean bDisplayFailCond = (Boolean)context.get("displayFailCond");
        String mapKey = (String) context.get("mapKey");

        // If "deactivateExisting" is set, other Contents that are tied to the same
        // contentIdTo will be deactivated (thruDate set to now)
        String deactivateString = (String) context.get("deactivateExisting");
        boolean deactivateExisting = "true".equalsIgnoreCase(deactivateString);

        if (Debug.infoOn()) Debug.logInfo("in persist... mapKey(0):" + mapKey, null);

        // ContentPurposes can get passed in as a delimited string or a list. Combine.
        List<String> contentPurposeList = UtilGenerics.checkList(context.get("contentPurposeList"));
        if (contentPurposeList == null) {
            contentPurposeList = FastList.newInstance();
        }
        String contentPurposeString = (String) context.get("contentPurposeString");
        if (UtilValidate.isNotEmpty(contentPurposeString)) {
            List<String> tmpPurposes = StringUtil.split(contentPurposeString, "|");
            contentPurposeList.addAll(tmpPurposes);
        }
        if (contentPurposeList != null) {
            context.put("contentPurposeList", contentPurposeList);
            context.put("contentPurposeString", null);
        }
        
        if (Debug.infoOn()) {
            Debug.logInfo("in persist... contentPurposeList(0):" + contentPurposeList, null);
            Debug.logInfo("in persist... textData(0):" + context.get("textData"), null);
        }

        GenericValue content = delegator.makeValue("Content");
        content.setPKFields(context);
        content.setNonPKFields(context);
        String contentId = (String) content.get("contentId");
        String contentTypeId = (String) content.get("contentTypeId");
        String origContentId = (String) content.get("contentId");
        String origDataResourceId = (String) content.get("dataResourceId");
        
        if (Debug.infoOn()) {
            Debug.logInfo("in persist... contentId(0):" + contentId, null);
        }

        GenericValue dataResource = delegator.makeValue("DataResource");
        dataResource.setPKFields(context);
        dataResource.setNonPKFields(context);
        dataResource.setAllFields(context, false, "dr", null);
        context.putAll(dataResource);
        String dataResourceId = (String) dataResource.get("dataResourceId");
        String dataResourceTypeId = (String) dataResource.get("dataResourceTypeId");
        if (Debug.infoOn()) {
            Debug.logInfo("in persist... dataResourceId(0):" + dataResourceId, null);
        }

        GenericValue contentAssoc = delegator.makeValue("ContentAssoc");
        String contentAssocTypeId = (String)context.get("contentAssocTypeId");
        if (UtilValidate.isNotEmpty(contentAssocTypeId)) {
            context.put("caContentAssocTypeId", contentAssocTypeId);
        }
        contentAssocTypeId = (String)context.get("caContentAssocTypeId");
        contentAssoc.setAllFields(context, false, "ca", null);
        contentAssoc.put("contentId", context.get("caContentId"));
        context.putAll(contentAssoc);

        GenericValue electronicText = delegator.makeValue("ElectronicText");
        electronicText.setPKFields(context);
        electronicText.setNonPKFields(context);

        // save expected primary keys on result now in case there is no operation that uses them
        Map<String, Object> results = ServiceUtil.returnSuccess();
        results.put("contentId", content.get("contentId"));
        results.put("dataResourceId", dataResource.get("dataResourceId"));
        results.put("drDataResourceId", dataResource.get("dataResourceId"));
        results.put("drDataResourceId", dataResource.get("dataResourceId"));
        results.put("caContentIdTo", contentAssoc.get("contentIdTo"));
        results.put("caContentId", contentAssoc.get("contentId"));
        results.put("caFromDate", contentAssoc.get("fromDate"));
        results.put("caContentAssocTypeId", contentAssoc.get("contentAssocTypeId"));

        // get user info for multiple use
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        // TODO: DEJ20060221 Should these be used somewhere?
        //String textData = (String)electronicText.get("textData");

        //String userLoginId = (String)userLogin.get("userLoginId");

        //String createdByUserLogin = userLoginId;
        //String lastModifiedByUserLogin = userLoginId;
        //Timestamp createdDate = UtilDateTime.nowTimestamp();
        //Timestamp lastModifiedDate = UtilDateTime.nowTimestamp();

        // Do update and create permission checks on DataResource if warranted.
        //boolean updatePermOK = false;
        //boolean createPermOK = false;


        boolean dataResourceExists = true;
        if (Debug.infoOn()) {
            Debug.logInfo("in persist... dataResourceTypeId(0):" + dataResourceTypeId, null);
        }
        if (UtilValidate.isNotEmpty(dataResourceTypeId)) {
            Map<String, Object> dataResourceResult = FastMap.newInstance();
            try {
                dataResourceResult = persistDataResourceAndDataMethod(dctx, context);
            } catch (GenericServiceException e) {
                Debug.logError(e, e.toString(), module);
                return ServiceUtil.returnError(e.toString());
            } catch (GenericEntityException e) {
                Debug.logError(e, e.toString(), module);
                return ServiceUtil.returnError(e.toString());
            } catch (Exception e) {
                Debug.logError(e, e.toString(), module);
                return ServiceUtil.returnError(e.toString());
            }
            String errorMsg = ServiceUtil.getErrorMessage(dataResourceResult);
            if (UtilValidate.isNotEmpty(errorMsg)) {
                return ServiceUtil.returnError(errorMsg);
            }
            dataResourceId = (String)dataResourceResult.get("dataResourceId");
            results.put("dataResourceId", dataResourceId);
            results.put("drDataResourceId", dataResourceId);
            context.put("dataResourceId", dataResourceId);
            content.put("dataResourceId", dataResourceId);
            context.put("drDataResourceId", dataResourceId);
        }
        // Do update and create permission checks on Content if warranted.

        context.put("skipPermissionCheck", null);  // Force check here
        boolean contentExists = true;
        if (Debug.infoOn()) {
            Debug.logInfo("in persist... contentTypeId:" +  contentTypeId + " dataResourceTypeId:" + dataResourceTypeId + " contentId:" + contentId + " dataResourceId:" + dataResourceId, null);
        }
        if (UtilValidate.isNotEmpty(contentTypeId)) {
            if (UtilValidate.isEmpty(contentId)) {
                contentExists = false;
            } else {
                try {
                    GenericValue val = delegator.findByPrimaryKey("Content", UtilMisc.toMap("contentId", contentId));
                    if (val == null) contentExists = false;
                } catch (GenericEntityException e) {
                    return ServiceUtil.returnError(e.toString());
                }
            }
            //List targetOperations = FastList.newInstance();
            //context.put("targetOperations", targetOperations);
            context.putAll(content);
            if (contentExists) {
                //targetOperations.add("CONTENT_UPDATE");
                Map<String, Object> contentContext = FastMap.newInstance();
                ModelService contentModel = dispatcher.getDispatchContext().getModelService("updateContent");
                contentContext.putAll(contentModel.makeValid(content, "IN"));
                contentContext.put("userLogin", userLogin);
                contentContext.put("displayFailCond", bDisplayFailCond);
                contentContext.put("skipPermissionCheck", context.get("skipPermissionCheck"));
                Debug.logInfo("In persistContentAndAssoc calling updateContent with content: " + contentContext, module);
                Map<String, Object> thisResult = dispatcher.runSync("updateContent", contentContext);
                if (ServiceUtil.isError(thisResult) || ServiceUtil.isFailure(thisResult)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentContentUpdatingError", UtilMisc.toMap("serviceName", "persistContentAndAssoc"), locale), null, null, thisResult);
                }
                //Map thisResult = ContentServices.updateContentMethod(dctx, context);
            } else {
                //targetOperations.add("CONTENT_CREATE");
                Map<String, Object> contentContext = FastMap.newInstance();
                ModelService contentModel = dispatcher.getDispatchContext().getModelService("createContent");
                contentContext.putAll(contentModel.makeValid(content, "IN"));
                contentContext.put("userLogin", userLogin);
                contentContext.put("displayFailCond", bDisplayFailCond);
                contentContext.put("skipPermissionCheck", context.get("skipPermissionCheck"));
                Debug.logInfo("In persistContentAndAssoc calling createContent with content: " + contentContext, module);
                Map<String, Object> thisResult = dispatcher.runSync("createContent", contentContext);
                if (ServiceUtil.isError(thisResult) || ServiceUtil.isFailure(thisResult)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentContentCreatingError", UtilMisc.toMap("serviceName", "persistContentAndAssoc"), locale), null, null, thisResult);
                }
                //Map thisResult = ContentServices.createContentMethod(dctx, context);

                contentId = (String) thisResult.get("contentId");
            }
            results.put("contentId", contentId);
            context.put("contentId", contentId);
            context.put("caContentIdTo", contentId);
            contentAssoc.put("contentIdTo", contentId);

            // Add ContentPurposes if this is a create operation
            if (contentId != null && !contentExists) {
                try {
                    if (contentPurposeList != null) {
                        Set<String> contentPurposeSet = UtilMisc.makeSetWritable(contentPurposeList);
                        Iterator<String> iter = contentPurposeSet.iterator();
                        while (iter.hasNext()) {
                            String contentPurposeTypeId = iter.next();
                            GenericValue contentPurpose = delegator.makeValue("ContentPurpose", UtilMisc.toMap("contentId", contentId, "contentPurposeTypeId", contentPurposeTypeId));
                            contentPurpose.create();
                        }
                    }
                } catch (GenericEntityException e) {
                    return ServiceUtil.returnError(e.toString());
                }
            }

        } else if (UtilValidate.isNotEmpty(dataResourceTypeId) && UtilValidate.isNotEmpty(contentId)) {
            // If dataResource was not previously existing, then update the associated content with its id
            if (UtilValidate.isNotEmpty(dataResourceId) && !dataResourceExists) {
                Map<String, Object> map = FastMap.newInstance();
                map.put("userLogin", userLogin);
                map.put("dataResourceId", dataResourceId);
                map.put("contentId", contentId);
                if (Debug.infoOn()) Debug.logInfo("in persist... context:" + context, module);
                Map<String, Object> r = ContentServices.updateContentMethod(dctx, map);
                boolean isError = ModelService.RESPOND_ERROR.equals(r.get(ModelService.RESPONSE_MESSAGE));
                if (isError)
                    return ServiceUtil.returnError((String)r.get(ModelService.ERROR_MESSAGE));
            }
        }

        // If parentContentIdTo or parentContentIdFrom exists, create association with newly created content
        if (Debug.infoOn()) {
            Debug.logInfo("CREATING contentASSOC contentAssocTypeId:" + contentAssocTypeId, null);
        }
        // create content assoc if the key values are present....
        Debug.logInfo("contentAssoc: " + contentAssoc.toString(), null);
        if (UtilValidate.isNotEmpty(contentAssocTypeId) && contentAssoc.get("contentId") != null && contentAssoc.get("contentIdTo") != null) {
            if (Debug.infoOn())
                Debug.logInfo("in persistContentAndAssoc, deactivateExisting:" + deactivateExisting, null);
            Map<String, Object> contentAssocContext = FastMap.newInstance();
            contentAssocContext.put("userLogin", userLogin);
            contentAssocContext.put("displayFailCond", bDisplayFailCond);
            contentAssocContext.put("skipPermissionCheck", context.get("skipPermissionCheck"));
            Map<String, Object> thisResult = null;
            try {
                GenericValue contentAssocExisting = delegator.findByPrimaryKey("ContentAssoc", contentAssoc.getPrimaryKey());
                if (contentAssocExisting == null) {
                    ModelService contentAssocModel = dispatcher.getDispatchContext().getModelService("createContentAssoc");
                    Map<String, Object> ctx = contentAssocModel.makeValid(contentAssoc, "IN");
                    contentAssocContext.putAll(ctx);
                    thisResult = dispatcher.runSync("createContentAssoc", contentAssocContext);
                    String errMsg = ServiceUtil.getErrorMessage(thisResult);
                    if (ServiceUtil.isError(thisResult) || ServiceUtil.isFailure(thisResult) || UtilValidate.isNotEmpty(errMsg)) {
                        return ServiceUtil.returnError(errMsg);
                    }
                    // results.put("contentIdTo",
                    // thisResult.get("contentIdTo"));
                    // results.put("contentIdFrom",
                    // thisResult.get("contentIdFrom"));
                    // results.put("contentId",
                    // thisResult.get("contentIdFrom"));
                    // results.put("contentAssocTypeId",
                    // thisResult.get("contentAssocTypeId"));
                    // results.put("fromDate", thisResult.get("fromDate"));
                    // results.put("sequenceNum",
                    // thisResult.get("sequenceNum"));

                    results.put("caContentIdTo", thisResult.get("contentIdTo"));
                    results.put("caContentId", thisResult.get("contentIdFrom"));
                    results.put("caContentAssocTypeId", thisResult.get("contentAssocTypeId"));
                    results.put("caFromDate", thisResult.get("fromDate"));
                    results.put("caSequenceNum", thisResult.get("sequenceNum"));
                } else {
                    if (deactivateExisting) {
                        contentAssoc.put("thruDate", UtilDateTime.nowTimestamp());
                    }
                    ModelService contentAssocModel = dispatcher.getDispatchContext().getModelService("updateContentAssoc");
                    Map<String, Object> ctx = contentAssocModel.makeValid(contentAssoc, "IN");
                    contentAssocContext.putAll(ctx);
                    thisResult = dispatcher.runSync("updateContentAssoc", contentAssocContext);
                    String errMsg = ServiceUtil.getErrorMessage(thisResult);
                    if (ServiceUtil.isError(thisResult) || ServiceUtil.isFailure(thisResult) || UtilValidate.isNotEmpty(errMsg)) {
                        return ServiceUtil.returnError(errMsg);
                    }
                }
            } catch (GenericEntityException e) {
                throw new GenericServiceException(e.toString());
            } catch (Exception e2) {
                throw new GenericServiceException(e2.toString());
            }
            String errMsg = ServiceUtil.getErrorMessage(thisResult);
            if (UtilValidate.isNotEmpty(errMsg)) {
                return ServiceUtil.returnError(errMsg);
            }
        }
        context.remove("skipPermissionCheck");
        context.put("contentId", origContentId);
        context.put("dataResourceId", origDataResourceId);
        context.remove("dataResource");
        Debug.logInfo("results:" + results, module);
        return results;
    }

    /**
    Service for update publish sites with a ContentRole that will tie them to the passed
    in party.
   */
    public static Map<String, Object> updateSiteRoles(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue)context.get("userLogin");
        //String userLoginPartyId = userLogin.getString("partyId");
      Map<String, Object> results = FastMap.newInstance();
      // siteContentId will equal "ADMIN_MASTER", "AGINC_MASTER", etc.
      // Remember that this service is called in the "multi" mode,
      // with a new siteContentId each time.
      // siteContentId could also have been name deptContentId, since this same
      // service is used for updating department roles, too.
      String siteContentId = (String)context.get("contentId");
      String partyId = (String)context.get("partyId");

      if (UtilValidate.isEmpty(siteContentId) || UtilValidate.isEmpty(partyId))
          return results;

      //Debug.logInfo("updateSiteRoles, context(0):" + context, module);

      List<GenericValue> siteRoles = null;
      try {
          siteRoles = delegator.findByAndCache("RoleType", UtilMisc.toMap("parentTypeId", "BLOG"));
      } catch (GenericEntityException e) {
          return ServiceUtil.returnError(e.toString());
      }

      Iterator<GenericValue> siteRoleIter = siteRoles.iterator();
      while (siteRoleIter.hasNext()) {
          Map<String, Object> serviceContext = FastMap.newInstance();
          serviceContext.put("partyId", partyId);
          serviceContext.put("contentId", siteContentId);
          serviceContext.put("userLogin", userLogin);
          Debug.logInfo("updateSiteRoles, serviceContext(0):" + serviceContext, module);
            GenericValue roleType = siteRoleIter.next();
          String siteRole = (String)roleType.get("roleTypeId"); // BLOG_EDITOR, BLOG_ADMIN, etc.
          String cappedSiteRole = ModelUtil.dbNameToVarName(siteRole);
          if (Debug.infoOn()) {
              Debug.logInfo("updateSiteRoles, cappediteRole(1):" + cappedSiteRole, module);
          }
          String siteRoleVal = (String)context.get(cappedSiteRole);
          if (Debug.infoOn()) {
              Debug.logInfo("updateSiteRoles, siteRoleVal(1):" + siteRoleVal, module);
              Debug.logInfo("updateSiteRoles, context(1):" + context, module);
          }
          Object fromDate = context.get(cappedSiteRole + "FromDate");
          if (Debug.infoOn()) {
              Debug.logInfo("updateSiteRoles, fromDate(1):" + fromDate, module);
          }
          serviceContext.put("roleTypeId", siteRole);
          if (siteRoleVal != null && siteRoleVal.equalsIgnoreCase("Y")) {
              // for now, will assume that any error is due to duplicates - ignore
              //return ServiceUtil.returnError(e.toString());
              if (fromDate == null) {
                  try {
                      Map<String, Object> newContext = FastMap.newInstance();
                      newContext.put("contentId", serviceContext.get("contentId"));
                      newContext.put("partyId", serviceContext.get("partyId"));
                      newContext.put("roleTypeId", serviceContext.get("roleTypeId"));
                      newContext.put("userLogin", userLogin);
                      Map<String, Object> permResults = dispatcher.runSync("deactivateAllContentRoles", newContext);
                      serviceContext.put("fromDate", UtilDateTime.nowTimestamp());
                      if (Debug.infoOn()) Debug.logInfo("updateSiteRoles, serviceContext(1):" + serviceContext, module);
                      permResults = dispatcher.runSync("createContentRole", serviceContext);
                      String errMsg = ServiceUtil.getErrorMessage(permResults);
                      if (UtilValidate.isNotEmpty(errMsg)) {
                          return ServiceUtil.returnError(errMsg);
                      } 
                      //addRoleToUser(delegator, dispatcher, serviceContext);
                  } catch (GenericServiceException e) {
                      Debug.logError(e, e.toString(), module);
                      return ServiceUtil.returnError(e.toString());
                  } catch (Exception e2) {
                      Debug.logError(e2, e2.toString(), module);
                      return ServiceUtil.returnError(e2.toString());
                  }
              }
          } else {
              if (fromDate != null) {
                  // for now, will assume that any error is due to non-existence - ignore
                  //return ServiceUtil.returnError(e.toString());
                  try {
                      Debug.logInfo("updateSiteRoles, serviceContext(2):" + serviceContext, module);
                      //Timestamp thruDate = UtilDateTime.nowTimestamp();
                      //serviceContext.put("thruDate", thruDate);
                      //serviceContext.put("fromDate", fromDate);
                      Map<String, Object> newContext = FastMap.newInstance();
                      newContext.put("contentId", serviceContext.get("contentId"));
                      newContext.put("partyId", serviceContext.get("partyId"));
                      newContext.put("roleTypeId", serviceContext.get("roleTypeId"));
                      newContext.put("userLogin", userLogin);
                      Map<String, Object> permResults = dispatcher.runSync("deactivateAllContentRoles", newContext);
                      String errMsg = ServiceUtil.getErrorMessage(permResults);
                      if (UtilValidate.isNotEmpty(errMsg))
                        return ServiceUtil.returnError(errMsg);
                  } catch (GenericServiceException e) {
                      Debug.logError(e, e.toString(), module);
                      return ServiceUtil.returnError(e.toString());
                  } catch (Exception e2) {
                      Debug.logError(e2, e2.toString(), module);
                      return ServiceUtil.returnError(e2.toString());
                  }
              }
          }
      }
      return results;
  }

    public static Map<String, Object> persistDataResourceAndData(DispatchContext dctx, Map<String, ? extends Object> context) {
      //Delegator delegator = dctx.getDelegator();
      LocalDispatcher dispatcher = dctx.getDispatcher();
      //String contentId = (String)context.get("contentId");
      Locale locale = (Locale) context.get("locale");
      Map<String, Object> result = FastMap.newInstance();
      try {
          //GenericValue content = delegator.findByPrimaryKey("Content", UtilMisc.toMap("contentId", contentId));
          ModelService checkPermModel = dispatcher.getDispatchContext().getModelService("checkContentPermission");
          Map<String, Object> ctx = checkPermModel.makeValid(context, "IN");
          Map<String, Object> thisResult = dispatcher.runSync("checkContentPermission", ctx);
          String permissionStatus = (String)thisResult.get("permissionStatus");
          if (UtilValidate.isNotEmpty(permissionStatus) && permissionStatus.equalsIgnoreCase("granted")) {
              result = persistDataResourceAndDataMethod(dctx, context);
          }
          else {
              return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentContentNoAccessToUploadImage", locale));
          }
      } catch (GenericServiceException e) {
          Debug.logError(e, e.toString(), module);
          return ServiceUtil.returnError(e.toString());
      } catch (GenericEntityException e) {
          Debug.logError(e, e.toString(), module);
          return ServiceUtil.returnError(e.toString());
      } catch (Exception e) {
          Debug.logError(e, e.toString(), module);
          return ServiceUtil.returnError(e.toString());
      }
      String errorMsg = ServiceUtil.getErrorMessage(result);
      if (UtilValidate.isNotEmpty(errorMsg)) {
          return ServiceUtil.returnError(errorMsg);
      }
      return result;
    }

    public static Map<String, Object> persistDataResourceAndDataMethod(DispatchContext dctx, Map<String, ? extends Object> rcontext) throws GenericServiceException, GenericEntityException, Exception {
      Delegator delegator = dctx.getDelegator();
      LocalDispatcher dispatcher = dctx.getDispatcher();
      Map<String, Object> context = UtilMisc.makeMapWritable(rcontext);
      Map<String, Object> result = FastMap.newInstance();
      Map<String, Object> newDrContext = FastMap.newInstance();
      GenericValue dataResource = delegator.makeValue("DataResource");
      dataResource.setPKFields(context);
      dataResource.setNonPKFields(context);
      dataResource.setAllFields(context, false, "dr", null);
      context.putAll(dataResource);

      GenericValue electronicText = delegator.makeValue("ElectronicText");
      electronicText.setPKFields(context);
      electronicText.setNonPKFields(context);
      String textData = (String)electronicText.get("textData");


      String dataResourceId = (String)dataResource.get("dataResourceId");
      String dataResourceTypeId = (String)dataResource.get("dataResourceTypeId");
      if (Debug.infoOn()) {
          Debug.logInfo("in persist... dataResourceId(0):" + dataResourceId, null);
      }
      context.put("skipPermissionCheck", "granted"); // TODO: a temp hack because I don't want to bother with DataResource permissions at this time.
      boolean dataResourceExists = true;
      if (UtilValidate.isEmpty(dataResourceId)) {
          dataResourceExists = false;
      } else {
          try {
              GenericValue val = delegator.findByPrimaryKey("DataResource", UtilMisc.toMap("dataResourceId", dataResourceId));
              if (val == null) {
                  dataResourceExists = false;
              }
          } catch (GenericEntityException e) {
              return ServiceUtil.returnError(e.toString());
          }
      }
      GenericValue userLogin = (GenericValue) context.get("userLogin");
      //String userLoginId = (String)userLogin.get("userLoginId");
      ModelService dataResourceModel = dispatcher.getDispatchContext().getModelService("updateDataResource");
      Map<String, Object> ctx = dataResourceModel.makeValid(dataResource, "IN");
      newDrContext.putAll(ctx);
      newDrContext.put("userLogin", userLogin);
      newDrContext.put("skipPermissionCheck", context.get("skipPermissionCheck"));
      ByteBuffer imageDataBytes = (ByteBuffer) context.get("imageData");
      String mimeTypeId = (String) newDrContext.get("mimeTypeId");
      if (imageDataBytes != null && (mimeTypeId == null || (mimeTypeId.indexOf("image") >= 0) || (mimeTypeId.indexOf("application") >= 0))) {
          mimeTypeId = (String) context.get("_imageData_contentType");
          if (dataResourceTypeId.equals("IMAGE_OBJECT")) {
              String fileName = (String) context.get("_imageData_fileName");
              newDrContext.put("objectInfo", fileName);
          }
          newDrContext.put("mimeTypeId", mimeTypeId);
      }

      if (!dataResourceExists) {
          Map<String, Object> thisResult = dispatcher.runSync("createDataResource", newDrContext);
          String errorMsg = ServiceUtil.getErrorMessage(thisResult);
          if (UtilValidate.isNotEmpty(errorMsg)) {
              throw(new Exception(errorMsg));
          }
          dataResourceId = (String)thisResult.get("dataResourceId");
          if (Debug.infoOn()) {
              Debug.logInfo("in persist... dataResourceId(0):" + dataResourceId, null);
          }
          dataResource = (GenericValue)thisResult.get("dataResource");
          Map<String, Object> fileContext = FastMap.newInstance();
          fileContext.put("userLogin", userLogin);
          if (dataResourceTypeId.indexOf("_FILE") >=0) {
              boolean hasData = false;
              if (textData != null) {
                  fileContext.put("textData", textData);
                  hasData = true;
              }
              if (imageDataBytes != null) {
                  fileContext.put("binData", imageDataBytes);
                  hasData = true;
              }
              if (hasData) {
                  fileContext.put("rootDir", context.get("rootDir"));
                  fileContext.put("dataResourceTypeId", dataResourceTypeId);
                  if (UtilValidate.isNotEmpty(dataResource) && UtilValidate.isNotEmpty(dataResource.get("objectInfo"))) {
                      fileContext.put("objectInfo", dataResource.get("objectInfo"));
                  }
                  thisResult = dispatcher.runSync("createFile", fileContext);
                  errorMsg = ServiceUtil.getErrorMessage(thisResult);
                  if (UtilValidate.isNotEmpty(errorMsg)) {
                      return ServiceUtil.returnError(errorMsg);
                  }
              }
          } else if (dataResourceTypeId.equals("IMAGE_OBJECT")) {
              if (imageDataBytes != null) {
                  fileContext.put("dataResourceId", dataResourceId);
                  fileContext.put("imageData", imageDataBytes);
                  thisResult = dispatcher.runSync("createImage", fileContext);
                  errorMsg = ServiceUtil.getErrorMessage(thisResult);
                  if (UtilValidate.isNotEmpty(errorMsg)) {
                      return ServiceUtil.returnError(errorMsg);
                  }
              }
          } else if (dataResourceTypeId.equals("SHORT_TEXT")) {
          } else if (dataResourceTypeId.startsWith("SURVEY")) {
          } else {
              // assume ELECTRONIC_TEXT
              if (UtilValidate.isNotEmpty(textData)) {
                  fileContext.put("dataResourceId", dataResourceId);
                  fileContext.put("textData", textData);
                  thisResult = dispatcher.runSync("createElectronicText", fileContext);
                  errorMsg = ServiceUtil.getErrorMessage(thisResult);
                  if (UtilValidate.isNotEmpty(errorMsg)) {
                      return ServiceUtil.returnError(errorMsg);
                  }
              }
          }
      } else {
          Map<String, Object> thisResult = dispatcher.runSync("updateDataResource", newDrContext);
          String errorMsg = ServiceUtil.getErrorMessage(thisResult);
          if (UtilValidate.isNotEmpty(errorMsg)) {
              return ServiceUtil.returnError(errorMsg);
          }
          //Map thisResult = DataServices.updateDataResourceMethod(dctx, context);
          if (Debug.infoOn()) {
              Debug.logInfo("====in persist... thisResult.permissionStatus(0):" + thisResult.get("permissionStatus"), null);
          }
          //thisResult = DataServices.updateElectronicTextMethod(dctx, context);
          Map<String, Object> fileContext = FastMap.newInstance();
          fileContext.put("userLogin", userLogin);
          String forceElectronicText = (String)context.get("forceElectronicText");
          Debug.logInfo("====dataResourceType" + dataResourceTypeId , module);
          if (dataResourceTypeId.indexOf("_FILE") >=0) {
              boolean hasData = false;
              if (textData != null) {
                  fileContext.put("textData", textData);
                  hasData = true;
              }
              if (imageDataBytes != null) {
                  fileContext.put("binData", imageDataBytes);
                  hasData = true;
              }
              if (hasData || "true".equalsIgnoreCase(forceElectronicText)) {
                  fileContext.put("rootDir", context.get("rootDir"));
                  fileContext.put("dataResourceTypeId", dataResourceTypeId);
                  fileContext.put("objectInfo", dataResource.get("objectInfo"));
                  thisResult = dispatcher.runSync("updateFile", fileContext);
                  errorMsg = ServiceUtil.getErrorMessage(thisResult);
                  if (UtilValidate.isNotEmpty(errorMsg)) {
                      return ServiceUtil.returnError(errorMsg);
                  }
              }
          } else if (dataResourceTypeId.equals("IMAGE_OBJECT")) {
              if (imageDataBytes != null || "true".equalsIgnoreCase(forceElectronicText)) {
                  fileContext.put("dataResourceId", dataResourceId);
                  fileContext.put("imageData", imageDataBytes);
                  Debug.logInfo("====trying to update image", module);
                  thisResult = dispatcher.runSync("updateImage", fileContext);
                  errorMsg = ServiceUtil.getErrorMessage(thisResult);
                  if (UtilValidate.isNotEmpty(errorMsg)) {
                      return ServiceUtil.returnError(errorMsg);
                  }
              }
          } else if (dataResourceTypeId.equals("SHORT_TEXT")) {
          } else if (dataResourceTypeId.startsWith("SURVEY")) {
          } else {
              if (UtilValidate.isNotEmpty(textData) || "true".equalsIgnoreCase(forceElectronicText)) {
                  fileContext.put("dataResourceId", dataResourceId);
                  fileContext.put("textData", textData);
                  thisResult = dispatcher.runSync("updateElectronicText", fileContext);
                  errorMsg = ServiceUtil.getErrorMessage(thisResult);
                  if (UtilValidate.isNotEmpty(errorMsg)) {
                      return ServiceUtil.returnError(errorMsg);
                  }
              }
          }
      }
      result.put("dataResourceId", dataResourceId);
      result.put("drDataResourceId", dataResourceId);
      context.put("dataResourceId", dataResourceId);
      return result;
    }

    public static void addRoleToUser(Delegator delegator, LocalDispatcher dispatcher, Map<String, Object> serviceContext) throws GenericServiceException, GenericEntityException {
        String partyId = (String)serviceContext.get("partyId");
        Map<String, Object> findMap = UtilMisc.<String, Object>toMap("partyId", partyId);
        List<GenericValue> userLoginList = delegator.findByAnd("UserLogin", findMap);
        Iterator<GenericValue> iter = userLoginList.iterator();
        while (iter.hasNext()) {
            GenericValue partyUserLogin = iter.next();
            String partyUserLoginId = partyUserLogin.getString("userLoginId");
            serviceContext.put("contentId", partyUserLoginId); // author contentId
            dispatcher.runSync("createContentRole", serviceContext);
        }
    }

    public static Map<String, Object> updateSiteRolesDyn(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> results = FastMap.newInstance();
        Map<String, Object> serviceContext = FastMap.newInstance();
        // siteContentId will equal "ADMIN_MASTER", "AGINC_MASTER", etc.
        // Remember that this service is called in the "multi" mode,
        // with a new siteContentId each time.
        // siteContentId could also have been name deptContentId, since this same
        // service is used for updating department roles, too.
        String siteContentId = (String)context.get("contentId");
        String partyId = (String)context.get("partyId");
        serviceContext.put("partyId", partyId);
        serviceContext.put("contentId", siteContentId);
        //Debug.logInfo("updateSiteRoles, serviceContext(0):" + serviceContext, module);
        //Debug.logInfo("updateSiteRoles, context(0):" + context, module);

        List<GenericValue> siteRoles = null;
        try {
            siteRoles = delegator.findByAndCache("RoleType", UtilMisc.toMap("parentTypeId", "BLOG"));
        } catch (GenericEntityException e) {
          return ServiceUtil.returnError(e.toString());
        }
        Iterator<GenericValue> siteRoleIter = siteRoles.iterator();
        while (siteRoleIter.hasNext()) {
            GenericValue roleType = siteRoleIter.next();
            String siteRole = (String)roleType.get("roleTypeId"); // BLOG_EDITOR, BLOG_ADMIN, etc.
            String cappedSiteRole = ModelUtil.dbNameToVarName(siteRole);
            //if (Debug.infoOn()) Debug.logInfo("updateSiteRoles, cappediteRole(1):" + cappedSiteRole, module);

            String siteRoleVal = (String)context.get(cappedSiteRole);
            Object fromDate = context.get(cappedSiteRole + "FromDate");
            serviceContext.put("roleTypeId", siteRole);
            if (siteRoleVal != null && siteRoleVal.equalsIgnoreCase("Y")) {
                // for now, will assume that any error is due to duplicates - ignore
                //return ServiceUtil.returnError(e.toString());
                if (fromDate == null) {
                    try {
                        serviceContext.put("fromDate", UtilDateTime.nowTimestamp());
                        if (Debug.infoOn()) {
                            Debug.logInfo("updateSiteRoles, serviceContext(1):" + serviceContext, module);
                        }
                        addRoleToUser(delegator, dispatcher, serviceContext);
                        dispatcher.runSync("createContentRole", serviceContext);
                    } catch (GenericServiceException e) {
                        Debug.logError(e, e.toString(), module);
                    } catch (Exception e2) {
                        Debug.logError(e2, e2.toString(), module);
                    }
                }
            } else {
                if (fromDate != null) {
                    // for now, will assume that any error is due to non-existence - ignore
                    //return ServiceUtil.returnError(e.toString());
                    try {
                        Debug.logInfo("updateSiteRoles, serviceContext(2):" + serviceContext, module);
                        //Timestamp thruDate = UtilDateTime.nowTimestamp();
                        //serviceContext.put("thruDate", thruDate);
                        //serviceContext.put("fromDate", fromDate);
                        Map<String, Object> newContext = FastMap.newInstance();
                        newContext.put("contentId", serviceContext.get("contentId"));
                        newContext.put("partyId", serviceContext.get("partyId"));
                        newContext.put("roleTypeId", serviceContext.get("roleTypeId"));
                        dispatcher.runSync("deactivateAllContentRoles", newContext);
                    } catch (GenericServiceException e) {
                        Debug.logError(e, e.toString(), module);
                    } catch (Exception e2) {
                        Debug.logError(e2, e2.toString(), module);
                    }
                }
            }
        }
        return results;
    }

    public static Map<String, Object> updateOrRemove(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> results = FastMap.newInstance();
        Delegator delegator = dctx.getDelegator();
        String entityName = (String)context.get("entityName");
        String action = (String)context.get("action");
        String pkFieldCount = (String)context.get("pkFieldCount");
        Map<String, String> pkFields = FastMap.newInstance();
        int fieldCount = Integer.parseInt(pkFieldCount);
        for (int i=0; i<fieldCount; i++) {
            String fieldName = (String)context.get("fieldName" + i);
            String fieldValue = (String)context.get("fieldValue" + i);
            if (UtilValidate.isEmpty(fieldValue)) {
                // It may be the case that the last row in a form is "empty" waiting for
                // someone to enter a value, in which case we do not want to throw an
                // error, we just want to ignore it.
                return results;
            }
            pkFields.put(fieldName, fieldValue);
        }
        boolean doLink = (action != null && action.equalsIgnoreCase("Y")) ? true : false;
        if (Debug.infoOn()) {
            Debug.logInfo("in updateOrRemove, context:" + context, module);
        }
        try {
            GenericValue entityValuePK = delegator.makeValue(entityName, pkFields);
            if (Debug.infoOn()) {
                Debug.logInfo("in updateOrRemove, entityValuePK:" + entityValuePK, module);
            }
            GenericValue entityValueExisting = delegator.findByPrimaryKeyCache(entityName, entityValuePK);
            if (Debug.infoOn()) {
                Debug.logInfo("in updateOrRemove, entityValueExisting:" + entityValueExisting, module);
            }
            if (entityValueExisting == null) {
                if (doLink) {
                    entityValuePK.create();
                    if (Debug.infoOn()) {
                        Debug.logInfo("in updateOrRemove, entityValuePK: CREATED", module);
                    }
                }
            } else {
                if (!doLink) {
                    entityValueExisting.remove();
                    if (Debug.infoOn()) {
                        Debug.logInfo("in updateOrRemove, entityValueExisting: REMOVED", module);
                    }
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.toString());
        }
        return results;
    }

    public static Map<String, Object> resequence(DispatchContext dctx, Map<String, ? extends Object> context) throws GenericServiceException{
        Map<String, Object> result = FastMap.newInstance();
        Delegator delegator = dctx.getDelegator();
        String contentIdTo = (String)context.get("contentIdTo");
        Integer seqInc = (Integer)context.get("seqInc");
        if (seqInc == null) {
            seqInc = Integer.valueOf(100);
        }
        int seqIncrement = seqInc.intValue();
        List<String> typeList = UtilGenerics.checkList(context.get("typeList"));
        if (typeList == null) {
            typeList = FastList.newInstance();
        }
        String contentAssocTypeId = (String)context.get("contentAssocTypeId");
        if (UtilValidate.isNotEmpty(contentAssocTypeId)) {
            typeList.add(contentAssocTypeId);
        }
        if (UtilValidate.isEmpty(typeList)) {
            typeList = UtilMisc.toList("PUBLISH_LINK", "SUB_CONTENT");
        }
        EntityCondition conditionType = EntityCondition.makeCondition("contentAssocTypeId", EntityOperator.IN, typeList);
        EntityCondition conditionMain = EntityCondition.makeCondition(UtilMisc.toList(EntityCondition.makeCondition("contentIdTo", EntityOperator.EQUALS, contentIdTo), conditionType), EntityOperator.AND);
        try {
            List<GenericValue> listAll = delegator.findList("ContentAssoc", conditionMain, null, UtilMisc.toList("sequenceNum", "fromDate", "createdDate"), null, false);
            List<GenericValue> listFiltered = EntityUtil.filterByDate(listAll);
            String contentId = (String)context.get("contentId");
            String dir = (String)context.get("dir");
            int seqNum = seqIncrement;
            String thisContentId = null;
            for (int i=0; i < listFiltered.size(); i++) {
                GenericValue contentAssoc = listFiltered.get(i);
                if (UtilValidate.isNotEmpty(contentId) && UtilValidate.isNotEmpty(dir)) {
                    // move targeted entry up or down
                    thisContentId = contentAssoc.getString("contentId");
                    if (contentId.equals(thisContentId)) {
                        if (dir.startsWith("up")) {
                            if (i > 0) {
                                // Swap with previous entry
                                try {
                                    GenericValue prevValue = listFiltered.get(i-1);
                                    Long prevSeqNum = (Long)prevValue.get("sequenceNum");
                                    prevValue.put("sequenceNum", Long.valueOf(seqNum));
                                    prevValue.store();
                                    contentAssoc.put("sequenceNum", prevSeqNum);
                                    contentAssoc.store();
                                } catch (Exception e) {
                                    return ServiceUtil.returnError(e.toString());
                                }
                            }
                        } else {
                            if (i < listFiltered.size()) {
                                // Swap with next entry
                                GenericValue nextValue = listFiltered.get(i+1);
                                nextValue.put("sequenceNum", Long.valueOf(seqNum));
                                nextValue.store();
                                seqNum += seqIncrement;
                                contentAssoc.put("sequenceNum", Long.valueOf(seqNum));
                                contentAssoc.store();
                                i++; // skip next one
                            }
                        }
                    } else {
                        contentAssoc.put("sequenceNum", Long.valueOf(seqNum));
                        contentAssoc.store();
                    }
                } else {
                    contentAssoc.put("sequenceNum", Long.valueOf(seqNum));
                    contentAssoc.store();
                }
                seqNum += seqIncrement;
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.toString());
        }
        return result;
    }

    public static Map<String, Object> changeLeafToNode(DispatchContext dctx, Map<String, ? extends Object> context) throws GenericServiceException{
        Map<String, Object> result = FastMap.newInstance();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String contentId = (String)context.get("contentId");
        GenericValue userLogin = (GenericValue)context.get("userLogin");
        String userLoginId = userLogin.getString("userLoginId");
        Locale locale = (Locale) context.get("locale");
        //int seqNum = 9999;
        try {
            GenericValue content = delegator.findByPrimaryKey("Content", UtilMisc.toMap("contentId", contentId));
            if (content == null) {
                Debug.logError("content was null", module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentNoContentFound", UtilMisc.toMap("contentId", ""), locale));
            }
            String dataResourceId = content.getString("dataResourceId");
            //String contentTypeIdTo = content.getString("contentTypeId");
            /* this does not seem to be correct or needed
            if (UtilValidate.isNotEmpty(contentTypeIdTo)) {
                if (contentTypeIdTo.equals("OUTLINE_NODE")) {
                    content.put("contentTypeId", "OUTLINE_NODE");
                } else if (contentTypeIdTo.equals("PAGE_NODE")) {
                    content.put("contentTypeId", "SUBPAGE_NODE");
                } else
                    content.put("contentTypeId", "PAGE_NODE");
            }
            */

            content.set("dataResourceId", null);
            content.set("lastModifiedDate", UtilDateTime.nowTimestamp());
            content.set("lastModifiedByUserLogin", userLoginId);
            content.store();

            if (UtilValidate.isNotEmpty(dataResourceId)) {
                // add previous DataResource as part of new subcontent
                GenericValue contentClone = (GenericValue)content.clone();
                contentClone.set("dataResourceId", dataResourceId);
                content.set("lastModifiedDate", UtilDateTime.nowTimestamp());
                content.set("lastModifiedByUserLogin", userLoginId);
                content.set("createdDate", UtilDateTime.nowTimestamp());
                content.set("createdByUserLogin", userLoginId);

                contentClone.set("contentId", null);
                ModelService modelService = dctx.getModelService("persistContentAndAssoc");
                Map<String, Object> serviceIn = modelService.makeValid(contentClone, "IN");
                serviceIn.put("userLogin", userLogin);
                serviceIn.put("contentIdTo", contentId);
                serviceIn.put("contentAssocTypeId", "SUB_CONTENT");
                serviceIn.put("sequenceNum", Long.valueOf(50));
                try {
                    dispatcher.runSync("persistContentAndAssoc", serviceIn);
                } catch (ServiceAuthException e) {
                    return ServiceUtil.returnError(e.toString());
                }

                List<String> typeList = UtilMisc.toList("SUB_CONTENT");
                ContentManagementWorker.updateStatsTopDown(delegator, contentId, typeList);
            }

        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.toString());
        }
        return result;
    }

    public static Map<String, Object> updateLeafCount(DispatchContext dctx, Map<String, ? extends Object> context) throws GenericServiceException{
        Map<String, Object> result = FastMap.newInstance();
        Delegator delegator = dctx.getDelegator();
        List<String> typeList = UtilGenerics.checkList(context.get("typeList"));
        if (typeList == null) {
            typeList = UtilMisc.toList("PUBLISH_LINK", "SUB_CONTENT");
        }
        String startContentId = (String)context.get("contentId");
        try {
            int leafCount = ContentManagementWorker.updateStatsTopDown(delegator, startContentId, typeList);
            result.put("leafCount", Integer.valueOf(leafCount));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.toString());
        }
        return result;
    }

/*
    public static Map<String, Object> updateLeafChange(DispatchContext dctx, Map<String, ? extends Object> context) throws GenericServiceException{

        Map result = FastMap.newInstance();
        Delegator delegator = dctx.getDelegator();
        List typeList = (List)context.get("typeList");
        if (typeList == null)
            typeList = UtilMisc.toList("PUBLISH_LINK", "SUB_CONTENT");
        String contentId = (String)context.get("contentId");

        try {
            GenericValue thisContent = delegator.findByPrimaryKey("Content", UtilMisc.toMap("contentId", contentId));
            if (thisContent == null)
                throw new RuntimeException("No entity found for id=" + contentId);

            String thisContentId = thisContent.getString("contentId");
            Long leafCount = (Long)thisContent.get("nodeLeafCount");
            int subLeafCount = (leafCount == null) ? 1 : leafCount.intValue();
            String mode = (String)context.get("mode");
            if (mode != null && mode.equalsIgnoreCase("remove")) {
                subLeafCount *= -1;
            } else {
                // TODO: ??? what is this supposed to do:
                //subLeafCount = subLeafCount;
            }

           EntityCondition conditionType = EntityCondition.makeCondition("contentAssocTypeId", EntityOperator.IN, typeList);
           EntityCondition conditionMain = EntityCondition.makeCondition(UtilMisc.toList(EntityCondition.makeCondition("contentId", EntityOperator.EQUALS, thisContentId), conditionType), EntityOperator.AND);
            List listAll = delegator.findByConditionCache("ContentAssoc", conditionMain, null, null);
            List listFiltered = EntityUtil.filterByDate(listAll);
            Iterator iter = listFiltered.iterator();
            while (iter.hasNext()) {
                GenericValue contentAssoc = (GenericValue)iter.next();
                String subContentId = contentAssoc.getString("contentId");
                GenericValue contentTo = delegator.findByPrimaryKeyCache("Content", UtilMisc.toMap("contentId", subContentId));
                Integer childBranchCount = (Integer)contentTo.get("childBranchCount");
                int branchCount = (childBranchCount == null) ? 1 : childBranchCount.intValue();
                if (mode != null && mode.equalsIgnoreCase("remove"))
                    branchCount += -1;
                else
                    branchCount += 1;
                // For the level just above only, update the branch count
                contentTo.put("childBranchCount", Integer.valueOf(branchCount));

                // Start the updating of leaf counts above
                ContentManagementWorker.updateStatsBottomUp(delegator, subContentId, typeList, subLeafCount);
            }


        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.toString());
        }
        return result;
    }
    */

    /**
     * This service changes the contentTypeId of the current content and its children depending on the pageMode.
     * if pageMode == "outline" then if the contentTypeId of children is not "OUTLINE_NODE" or "PAGE_NODE"
     * (it could be DOCUMENT or SUBPAGE_NODE) then it will get changed to PAGE_NODE.`
     * if pageMode == "page" then if the contentTypeId of children is not "PAGE_NODE" or "SUBPAGE_NODE"
     * (it could be DOCUMENT or OUTLINE_NODE) then it will get changed to SUBPAGE_NODE.`
     */
    public static Map<String, Object> updatePageType(DispatchContext dctx, Map<String, ? extends Object> rcontext) throws GenericServiceException{
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> context = UtilMisc.makeMapWritable(rcontext);
        Map<String, Object> results = FastMap.newInstance();
        Locale locale = (Locale) context.get("locale");
        Set<String> visitedSet = UtilGenerics.checkSet(context.get("visitedSet"));
        if (visitedSet == null) {
            visitedSet = FastSet.newInstance();
            context.put("visitedSet", visitedSet);
        }
        String pageMode = (String)context.get("pageMode");
        String contentId = (String)context.get("contentId");
        visitedSet.add(contentId);
        String contentTypeId = "PAGE_NODE";
        if (pageMode != null && pageMode.toLowerCase().indexOf("outline") >= 0)
            contentTypeId = "OUTLINE_NODE";
        GenericValue thisContent = null;
        try {
            thisContent = delegator.findByPrimaryKey("Content", UtilMisc.toMap("contentId", contentId));
            if (thisContent == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentNoContentFound", UtilMisc.toMap("contentId", contentId), locale));
            }
            thisContent.set("contentTypeId", contentTypeId);
            thisContent.store();
            List<GenericValue> kids = ContentWorker.getAssociatedContent(thisContent, "from", UtilMisc.toList("SUB_CONTENT"), null, null, null);
            Iterator<GenericValue> iter = kids.iterator();
            while (iter.hasNext()) {
                GenericValue kidContent = iter.next();
                if (contentTypeId.equals("OUTLINE_NODE")) {
                    updateOutlineNodeChildren(kidContent, false, context);
                } else {
                    updatePageNodeChildren(kidContent, context);
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.toString());
        }

        return results;
    }

    public static Map<String, Object> resetToOutlineMode(DispatchContext dctx, Map<String, ? extends Object> rcontext) throws GenericServiceException{
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> context = UtilMisc.makeMapWritable(rcontext);
        Map<String, Object> results = FastMap.newInstance();
        Locale locale = (Locale) context.get("locale");
        Set<String> visitedSet = UtilGenerics.checkSet(context.get("visitedSet"));
        if (visitedSet == null) {
            visitedSet = FastSet.newInstance();
            context.put("visitedSet", visitedSet);
        }
        String contentId = (String)context.get("contentId");
        String pageMode = (String)context.get("pageMode");
        String contentTypeId = "OUTLINE_NODE";
        if (pageMode != null && pageMode.toLowerCase().indexOf("page") >= 0) {
            contentTypeId = "PAGE_NODE";
        }
        GenericValue thisContent = null;
        try {
            thisContent = delegator.findByPrimaryKey("Content", UtilMisc.toMap("contentId", contentId));
            if (thisContent == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                        "ContentNoContentFound", UtilMisc.toMap("contentId", contentId), locale));
            }
            thisContent.set("contentTypeId", "OUTLINE_NODE");
            thisContent.store();
            List<GenericValue> kids = ContentWorker.getAssociatedContent(thisContent, "from", UtilMisc.toList("SUB_CONTENT"), null, null, null);
            Iterator<GenericValue> iter = kids.iterator();
            while (iter.hasNext()) {
                GenericValue kidContent = iter.next();
                if (contentTypeId.equals("OUTLINE_NODE")) {
                    updateOutlineNodeChildren(kidContent, true, context);
                } else {
                    kidContent.put("contentTypeId", "PAGE_NODE");
                    kidContent.store();
                    List<GenericValue> kids2 = ContentWorker.getAssociatedContent(kidContent, "from", UtilMisc.toList("SUB_CONTENT"), null, null, null);
                    Iterator<GenericValue> iter2 = kids2.iterator();
                    while (iter2.hasNext()) {
                        GenericValue kidContent2 = iter2.next();
                        updatePageNodeChildren(kidContent2, context);
                    }
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.toString());
        }
        return results;
    }

    public static Map<String, Object> clearContentAssocViewCache(DispatchContext dctx, Map<String, ? extends Object> context) throws GenericServiceException{
        Map<String, Object> results = FastMap.newInstance();
        UtilCache<?, ?> utilCache = UtilCache.findCache("entitycache.entity-list.default.ContentAssocViewFrom");

        if (utilCache != null) {
            utilCache.clear();
        }

        utilCache = UtilCache.findCache("entitycache.entity-list.default.ContentAssocViewTo");
        if (utilCache != null) {
            utilCache.clear();
        }

        return results;
    }

    public static Map<String, Object> clearContentAssocDataResourceViewCache(DispatchContext dctx, Map<String, ? extends Object> context) throws GenericServiceException{
        Map<String, Object> results = FastMap.newInstance();

        UtilCache<?, ?> utilCache = UtilCache.findCache("entitycache.entity-list.default.ContentAssocViewDataResourceFrom");
        if (utilCache != null) {
            utilCache.clear();
        }

        utilCache = UtilCache.findCache("entitycache.entity-list.default.ContentAssocViewDataResourceTo");
        if (utilCache != null) {
            utilCache.clear();
        }

        return results;
    }

    public static void updatePageNodeChildren(GenericValue content, Map<String, Object> context) throws GenericEntityException {
        String contentId = content.getString("contentId");
        Set<String> visitedSet = UtilGenerics.checkSet(context.get("visitedSet"));
        if (visitedSet == null) {
            visitedSet = FastSet.newInstance();
            context.put("visitedSet", visitedSet);
        } else {
            if (visitedSet.contains(contentId)) {
                Debug.logWarning("visitedSet already contains:" + contentId, module);
                return;
            } else {
                visitedSet.add(contentId);
            }
        }
        // String contentTypeId = content.getString("contentTypeId");
        String newContentTypeId = "SUBPAGE_NODE";
//        if (contentTypeId == null || contentTypeId.equals("DOCUMENT")) {
//            newContentTypeId = "SUBPAGE_NODE";
//        } else if (contentTypeId.equals("OUTLINE_NODE")) {
//            newContentTypeId = "PAGE_NODE";
//        }

        content.put("contentTypeId", newContentTypeId);
        content.store();

        //if (contentTypeId == null || contentTypeId.equals("OUTLINE_DOCUMENT") || contentTypeId.equals("DOCUMENT")) {
        List<GenericValue> kids = ContentWorker.getAssociatedContent(content, "from", UtilMisc.toList("SUB_CONTENT"), null, null, null);
        Iterator<GenericValue> iter = kids.iterator();
        while (iter.hasNext()) {
            GenericValue kidContent = iter.next();
            updatePageNodeChildren(kidContent, context);
        }
        //}
    }

    public static void updateOutlineNodeChildren(GenericValue content, boolean forceOutline, Map<String, Object> context) throws GenericEntityException {
        String contentId = content.getString("contentId");
        Set<String> visitedSet = UtilGenerics.checkSet(context.get("visitedSet"));
        if (visitedSet == null) {
            visitedSet = FastSet.newInstance();
            context.put("visitedSet", visitedSet);
        } else {
            if (visitedSet.contains(contentId)) {
                Debug.logWarning("visitedSet already contains:" + contentId, module);
                return;
            } else {
                visitedSet.add(contentId);
            }
        }
        String contentTypeId = content.getString("contentTypeId");
        String newContentTypeId = contentTypeId;
        String dataResourceId = content.getString("dataResourceId");
        Long branchCount = (Long)content.get("childBranchCount");
        if (forceOutline) {
            newContentTypeId = "OUTLINE_NODE";
        } else if (contentTypeId == null || contentTypeId.equals("DOCUMENT")) {
            if (UtilValidate.isEmpty(dataResourceId) || (branchCount != null && branchCount.intValue() > 0)) {
                newContentTypeId = "OUTLINE_NODE";
            } else {
                newContentTypeId = "PAGE_NODE";
            }
        } else if (contentTypeId.equals("SUBPAGE_NODE")) {
            newContentTypeId = "PAGE_NODE";
        }

        content.put("contentTypeId", newContentTypeId);
        content.store();

        if (contentTypeId == null || contentTypeId.equals("DOCUMENT") || contentTypeId.equals("OUTLINE_NODE")) {
        //if (contentTypeId == null || contentTypeId.equals("DOCUMENT")) {
            List<GenericValue> kids = ContentWorker.getAssociatedContent(content, "from", UtilMisc.toList("SUB_CONTENT"), null, null, null);
            Iterator<GenericValue> iter = kids.iterator();
            while (iter.hasNext()) {
                GenericValue kidContent = iter.next();
                updateOutlineNodeChildren(kidContent, forceOutline, context);
            }
        }
    }

    public static Map<String, Object> findSubNodes(DispatchContext dctx, Map<String, ? extends Object> context) throws GenericServiceException{
        Map<String, Object> results = FastMap.newInstance();
        Delegator delegator = dctx.getDelegator();
        String contentIdTo = (String)context.get("contentId");
        List<EntityExpr> condList = FastList.newInstance();
        EntityExpr expr = EntityCondition.makeCondition("caContentIdTo", EntityOperator.EQUALS, contentIdTo);
        condList.add(expr);
        expr = EntityCondition.makeCondition("caContentAssocTypeId", EntityOperator.EQUALS, "SUB_CONTENT");
        condList.add(expr);
        expr = EntityCondition.makeCondition("caThruDate", EntityOperator.EQUALS, null);
        condList.add(expr);
        EntityConditionList<EntityExpr> entityCondList = EntityCondition.makeCondition(condList, EntityOperator.AND);
         try {
             List<GenericValue> lst = delegator.findList("ContentAssocDataResourceViewFrom", entityCondList, null, UtilMisc.toList("caSequenceNum", "caFromDate", "createdDate"), null, false);
             results.put("_LIST_", lst);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.toString());
        }
        return results;
    }

    public static String updateTypeAndFile(GenericValue dataResource, Map<String, Object> context) {
        String retVal = null;
        String mimeTypeId = (String) context.get("_imageData_contentType");
        String fileName = (String) context.get("_imageData_fileName");
        try {
            if (UtilValidate.isNotEmpty(fileName))
                dataResource.set("objectInfo", fileName);
            if (UtilValidate.isNotEmpty(mimeTypeId))
                dataResource.set("mimeTypeId", mimeTypeId);
            dataResource.store();
        } catch (GenericEntityException e) {
            retVal = "Unable to update the DataResource record";
        }
        return retVal;
    }

    public static Map<String, Object> initContentChildCounts(DispatchContext dctx, Map<String, ? extends Object> context) throws GenericServiceException{
        Map<String, Object> result = FastMap.newInstance();
        Locale locale = (Locale) context.get("locale");
        GenericValue content = (GenericValue)context.get("content");
        if (content == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentNoContentFound", UtilMisc.toMap("contentId", ""), locale));
        }
        Long leafCount = (Long)content.get("childLeafCount");
        if (leafCount == null) {
            content.set("childLeafCount", Long.valueOf(0));
        }
        Long branchCount = (Long)content.get("childBranchCount");
        if (branchCount == null) {
            content.set("childBranchCount", Long.valueOf(0));
        }

        //content.store();

        return result;
    }

    public static Map<String, Object> incrementContentChildStats(DispatchContext dctx, Map<String, ? extends Object> context) throws GenericServiceException{
        Map<String, Object> result = FastMap.newInstance();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        String contentId = (String)context.get("contentId");
        String contentAssocTypeId = (String)context.get("contentAssocTypeId");

        try {
            GenericValue content = delegator.findByPrimaryKeyCache("Content", UtilMisc.toMap("contentId", contentId));
            if (content == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentNoContentFound", UtilMisc.toMap("contentId", contentId), locale));
            }
            Long leafCount = (Long)content.get("childLeafCount");
            if (leafCount == null) {
                leafCount = Long.valueOf(0);
            }
            int changeLeafCount = leafCount.intValue() + 1;
            int changeBranchCount = 1;

            ContentManagementWorker.updateStatsBottomUp(delegator, contentId, UtilMisc.toList(contentAssocTypeId), changeBranchCount, changeLeafCount);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.toString());
        }
        return result;
    }

    public static Map<String, Object> decrementContentChildStats(DispatchContext dctx, Map<String, ? extends Object> context) throws GenericServiceException{
        Map<String, Object> result = FastMap.newInstance();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        String contentId = (String)context.get("contentId");
        String contentAssocTypeId = (String)context.get("contentAssocTypeId");

        try {
            GenericValue content = delegator.findByPrimaryKeyCache("Content", UtilMisc.toMap("contentId", contentId));
            if (content == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentNoContentFound", UtilMisc.toMap("contentId", contentId), locale));
            }
            Long leafCount = (Long)content.get("childLeafCount");
            if (leafCount == null) {
                leafCount = Long.valueOf(0);
            }
            int changeLeafCount = -1 * leafCount.intValue() - 1;
            int changeBranchCount = -1;

            ContentManagementWorker.updateStatsBottomUp(delegator, contentId, UtilMisc.toList(contentAssocTypeId), changeBranchCount, changeLeafCount);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.toString());
        }
        return result;
    }

    public static Map<String, Object> updateContentChildStats(DispatchContext dctx, Map<String, ? extends Object> context) throws GenericServiceException{
        Map<String, Object> result = FastMap.newInstance();
        Delegator delegator = dctx.getDelegator();

        String contentId = (String)context.get("contentId");
        String contentAssocTypeId = (String)context.get("contentAssocTypeId");
        List<String> typeList = FastList.newInstance();
        if (UtilValidate.isNotEmpty(contentAssocTypeId)) {
            typeList.add(contentAssocTypeId);
        } else {
            typeList = UtilMisc.toList("PUBLISH_LINK", "SUB_CONTENT");
        }

        try {
            ContentManagementWorker.updateStatsTopDown(delegator, contentId, typeList);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.toString());
        }
        return result;
    }

    public static Map<String, Object> updateContentSubscription(DispatchContext dctx, Map<String, ? extends Object> context) throws GenericServiceException{
        Map<String, Object> result = FastMap.newInstance();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();

        String partyId = (String) context.get("partyId");
        String webPubPt = (String) context.get("contentId");
        String roleTypeId = (String) context.get("useRoleTypeId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Integer useTime = (Integer) context.get("useTime");
        String useTimeUomId = (String) context.get("useTimeUomId");
        boolean hasExistingContentRole = false;
        GenericValue contentRole = null;
        try {
            List<GenericValue> contentRoleList = delegator.findByAndCache("ContentRole", UtilMisc.toMap("partyId", partyId, "contentId", webPubPt, "roleTypeId", roleTypeId));
            List<GenericValue> listFiltered = EntityUtil.filterByDate(contentRoleList, true);
            List<GenericValue> listOrdered = EntityUtil.orderBy(listFiltered, UtilMisc.toList("fromDate DESC"));
            if (listOrdered.size() > 0) {
                contentRole = listOrdered.get(0);
                hasExistingContentRole = true;
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.toString());
        }

        if (contentRole == null) {
            contentRole = delegator.makeValue("ContentRole");
            contentRole.set("contentId", webPubPt);
            contentRole.set("partyId", partyId);
            contentRole.set("roleTypeId", roleTypeId);
            contentRole.set("fromDate", nowTimestamp);
        }

        Timestamp thruDate = (Timestamp) contentRole.get("thruDate");
        if (thruDate == null) {
            // no thruDate? start with NOW
            thruDate = nowTimestamp;
        } else {
            // there is a thru date... if it is in the past, bring it up to NOW before adding on the time period
            //don't want to penalize for skipping time, in other words if they had a subscription last year for a month and buy another month, we want that second month to start now and not last year
            if (thruDate.before(nowTimestamp)) {
                thruDate = nowTimestamp;
            }
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(thruDate);
        int field = Calendar.MONTH;
        if ("TF_day".equals(useTimeUomId)) {
            field = Calendar.DAY_OF_YEAR;
        } else if ("TF_wk".equals(useTimeUomId)) {
            field = Calendar.WEEK_OF_YEAR;
        } else if ("TF_mon".equals(useTimeUomId)) {
            field = Calendar.MONTH;
        } else if ("TF_yr".equals(useTimeUomId)) {
            field = Calendar.YEAR;
        } else {
            Debug.logWarning("Don't know anything about useTimeUomId [" + useTimeUomId + "], defaulting to month", module);
        }
        calendar.add(field, useTime.intValue());
        thruDate = new Timestamp(calendar.getTimeInMillis());
        contentRole.set("thruDate", thruDate);
        try {
            if (hasExistingContentRole) {
                contentRole.store();
            } else {
                Map<String, Object> map = FastMap.newInstance();
                map.put("partyId", partyId);
                map.put("roleTypeId", roleTypeId);
                map.put("userLogin", userLogin);
                dispatcher.runSync("createPartyRole", map);
                contentRole.create();
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.toString());
        }
        return result;
    }

    public static Map<String, Object> updateContentSubscriptionByProduct(DispatchContext dctx, Map<String, ? extends Object> rcontext) throws GenericServiceException{
        Map<String, Object> context = UtilMisc.makeMapWritable(rcontext);
        Map<String, Object> result = FastMap.newInstance();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String productId = (String) context.get("productId");
        Integer qty = (Integer) context.get("quantity");
        if (qty == null) {
            qty = Integer.valueOf(1);
        }

        Timestamp orderCreatedDate = (Timestamp) context.get("orderCreatedDate");
        if (orderCreatedDate == null) {
            orderCreatedDate = UtilDateTime.nowTimestamp();
        }
        GenericValue productContent = null;
           try {
            List<GenericValue> lst = delegator.findByAndCache("ProductContent", UtilMisc.toMap("productId", productId, "productContentTypeId", "ONLINE_ACCESS"));
            List<GenericValue> listFiltered = EntityUtil.filterByDate(lst, orderCreatedDate, "purchaseFromDate", "purchaseThruDate", true);
            List<GenericValue> listOrdered = EntityUtil.orderBy(listFiltered, UtilMisc.toList("purchaseFromDate", "purchaseThruDate"));
            List<GenericValue> listThrusOnly = EntityUtil.filterOutByCondition(listOrdered, EntityCondition.makeCondition("purchaseThruDate", EntityOperator.EQUALS, null));
            if (listThrusOnly.size() > 0) {
                productContent = listThrusOnly.get(0);
            } else if (listOrdered.size() > 0) {
                productContent = listOrdered.get(0);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e.toString(), module);
            return ServiceUtil.returnError(e.toString());
        }
        if (productContent == null) {
            String msg = "No ProductContent found for productId:" + productId;
            Debug.logError(msg, module);
            return ServiceUtil.returnError(msg);
        }
        Long useTime = (Long)productContent.get("useTime");
        Integer newUseTime = Integer.valueOf(useTime.intValue() * qty.intValue());
        context.put("useTime", newUseTime);
        context.put("useTimeUomId", productContent.get("useTimeUomId"));
        context.put("useRoleTypeId", productContent.get("useRoleTypeId"));
        context.put("contentId", productContent.get("contentId"));
        ModelService subscriptionModel = dispatcher.getDispatchContext().getModelService("updateContentSubscription");
        Map<String, Object> ctx = subscriptionModel.makeValid(context, "IN");
        result = dispatcher.runSync("updateContentSubscription", ctx);
        return result;
    }

    public static Map<String, Object> updateContentSubscriptionByOrder(DispatchContext dctx, Map<String, ? extends Object> rcontext) throws GenericServiceException{
        Map<String, Object> context = UtilMisc.makeMapWritable(rcontext);
        Map<String, Object> result = FastMap.newInstance();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String orderId = (String) context.get("orderId");

        Debug.logInfo("In updateContentSubscriptionByOrder service with orderId: " + orderId, module);

        GenericValue orderHeader = null;
        try {
            List<GenericValue> orderRoleList = delegator.findByAnd("OrderRole", UtilMisc.toMap("orderId", orderId, "roleTypeId", "END_USER_CUSTOMER"));
            if (orderRoleList.size() > 0) {
                GenericValue orderRole = orderRoleList.get(0);
                String partyId = (String) orderRole.get("partyId");
                context.put("partyId", partyId);
            } else {
                String msg = "No OrderRole found for orderId:" + orderId;
                return ServiceUtil.returnFailure(msg);

            }
            orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
            if (orderHeader == null) {
                String msg = "No OrderHeader found for orderId:" + orderId;
                return ServiceUtil.returnError(msg);
            }
            Timestamp orderCreatedDate = (Timestamp) orderHeader.get("orderDate");
            context.put("orderCreatedDate", orderCreatedDate);
            List<GenericValue> orderItemList = orderHeader.getRelated("OrderItem");
            Iterator<GenericValue> orderItemIter = orderItemList.iterator();
            ModelService subscriptionModel = dispatcher.getDispatchContext().getModelService("updateContentSubscriptionByProduct");
            while (orderItemIter.hasNext()) {
                GenericValue orderItem = orderItemIter.next();
                BigDecimal qty = orderItem.getBigDecimal("quantity");
                String productId = (String) orderItem.get("productId");
                List<GenericValue> productContentList = delegator.findByAnd("ProductContent", UtilMisc.toMap("productId", productId, "productContentTypeId", "ONLINE_ACCESS"));
                List<GenericValue> productContentListFiltered = EntityUtil.filterByDate(productContentList);
                if (productContentListFiltered.size() > 0) {
                    context.put("productId", productId);
                    context.put("quantity", Integer.valueOf(qty.intValue()));
                    Map<String, Object> ctx = subscriptionModel.makeValid(context, "IN");
                    dispatcher.runSync("updateContentSubscriptionByProduct", ctx);
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e.toString(), module);
            return ServiceUtil.returnError(e.toString());
        }
        return result;
    }

    public static Map<String, Object> followNodeChildren(DispatchContext dctx, Map<String, ? extends Object> context) throws GenericServiceException{
        Map<String, Object> result = null;
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue)context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        if (!security.hasEntityPermission("CONTENTMGR", "_ADMIN", userLogin)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentPermissionNotGranted", locale));
        }
        String contentId = (String)context.get("contentId");
        String serviceName = (String)context.get("serviceName");
        String contentAssocTypeId = (String)context.get("contentAssocTypeId");
        List<String> contentAssocTypeIdList = FastList.newInstance();
        if (UtilValidate.isNotEmpty(contentAssocTypeId)) {
             contentAssocTypeIdList = StringUtil.split(contentAssocTypeId, "|");
        }
        if (contentAssocTypeIdList.size() == 0) {
            contentAssocTypeIdList.add("SUB_CONTENT");
        }
        Map<String, Object> ctx = FastMap.newInstance();
        ctx.put("userLogin", userLogin);
        ctx.put("contentAssocTypeIdList", contentAssocTypeIdList);
        try {

            GenericValue content = delegator.findByPrimaryKey("Content", UtilMisc.toMap("contentId", contentId));
            result = followNodeChildrenMethod(content, dispatcher, serviceName, ctx);
        } catch (GenericEntityException e) {
            Debug.logError(e.toString(), module);
            return ServiceUtil.returnError(e.toString());
        }
        return result;
    }
    public static Map<String, Object> followNodeChildrenMethod(GenericValue content,  LocalDispatcher dispatcher, String serviceName, Map<String, Object> context) throws GenericEntityException, GenericServiceException {
        Map<String, Object> result = null;
        String contentId = content.getString("contentId");
        List<String> contentAssocTypeIdList = UtilGenerics.checkList(context.get("contentAssocTypeIdList"));
        Locale locale = (Locale) context.get("locale");
        Set<String> visitedSet = UtilGenerics.checkSet(context.get("visitedSet"));
        if (visitedSet == null) {
            visitedSet = FastSet.newInstance();
            context.put("visitedSet", visitedSet);
        } else {
            if (visitedSet.contains(contentId)) {
                Debug.logWarning("visitedSet already contains:" + contentId, module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentVisitedSet", locale) + contentId);
            } else {
                visitedSet.add(contentId);
            }
        }

        GenericValue userLogin = (GenericValue)context.get("userLogin");
        result = dispatcher.runSync(serviceName, UtilMisc.toMap("content", content, "userLogin", userLogin));

        List<GenericValue> kids = ContentWorker.getAssociatedContent(content, "from", contentAssocTypeIdList, null, null, null);
        Iterator<GenericValue> iter = kids.iterator();
        while (iter.hasNext()) {
            GenericValue kidContent = iter.next();
            followNodeChildrenMethod(kidContent, dispatcher, serviceName, context);
        }
        return result;
    }

    /**
   */
  public static Map<String, Object> persistContentWithRevision(DispatchContext dctx, Map<String, ? extends Object> context) {
      Map<String, Object> result = null;
      Delegator delegator = dctx.getDelegator();
      LocalDispatcher dispatcher = dctx.getDispatcher();
      GenericValue dataResource = null;
      String masterRevisionContentId = (String)context.get("masterRevisionContentId");
      String oldDataResourceId = (String)context.get("drDataResourceId");
      if (UtilValidate.isEmpty(oldDataResourceId)) {
          oldDataResourceId = (String)context.get("dataResourceId");
      }
      if (UtilValidate.isNotEmpty(oldDataResourceId)) {
          try {
              dataResource = delegator.findByPrimaryKey("DataResource", UtilMisc.toMap("dataResourceId", oldDataResourceId));
          } catch (GenericEntityException e) {
              Debug.logError(e.toString(), module);
              return ServiceUtil.returnError(e.toString());
          }
      }

      try {
          ModelService persistContentAndAssocModel = dispatcher.getDispatchContext().getModelService("persistContentAndAssoc");
          Map<String, Object> ctx = persistContentAndAssocModel.makeValid(context, "IN");
          if (dataResource != null) {
              ctx.remove("dataResourceId");
              ctx.remove("drDataResourceId");
          }
          result = dispatcher.runSync("persistContentAndAssoc", ctx);
          String errorMsg = ServiceUtil.getErrorMessage(result);
          if (UtilValidate.isNotEmpty(errorMsg)) {
              return ServiceUtil.returnError(errorMsg);
          }
          String contentId = (String)result.get("contentId");
          List<String> parentList = FastList.newInstance();
          if (UtilValidate.isEmpty(masterRevisionContentId)) {
              Map<String, Object> traversMap = FastMap.newInstance();
              traversMap.put("contentId", contentId);
              traversMap.put("direction", "To");
              traversMap.put("contentAssocTypeId", "COMPDOC_PART");
              Map<String, Object> traversResult = dispatcher.runSync("traverseContent", traversMap);
              parentList = UtilGenerics.checkList(traversResult.get("parentList"));
          } else {
              parentList.add(masterRevisionContentId);
          }

          // Update ContentRevision and ContentRevisonItem
          Map<String, Object> contentRevisionMap = FastMap.newInstance();
          contentRevisionMap.put("itemContentId", contentId);
          contentRevisionMap.put("newDataResourceId", result.get("dataResourceId"));
          contentRevisionMap.put("oldDataResourceId", oldDataResourceId);
          // need committedByPartyId
          for (int i=0; i < parentList.size(); i++) {
              String thisContentId = parentList.get(i);
              contentRevisionMap.put("contentId", thisContentId);
              result = dispatcher.runSync("persistContentRevisionAndItem", contentRevisionMap);
              errorMsg = ServiceUtil.getErrorMessage(result);
              if (UtilValidate.isNotEmpty(errorMsg)) {
                  return ServiceUtil.returnError(errorMsg);
              }
          }
      } catch (GenericServiceException e) {
          Debug.logError(e.toString(), module);
          return ServiceUtil.returnError(e.toString());
      }
      return result;
  }

}
