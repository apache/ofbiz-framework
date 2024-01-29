package com.simbaquartz.xparty.services;

import org.apache.ofbiz.base.util.*;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

import java.util.*;

import org.apache.ofbiz.entity.util.EntityQuery;

import java.sql.Timestamp;
import org.apache.ofbiz.entity.GenericEntityException;

import java.io.File;
import java.nio.ByteBuffer;

import com.fidelissd.zcp.xcommon.collections.FastList;
import com.fidelissd.zcp.xcommon.collections.FastMap;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class PartyContentServices {

  public static final String module = PartyContentServices.class.getName();
  public static final String ABOUT_PARTY_CONTENT_ID = "ABOUT_PARTY";

  /**
   * Delete party content and associated quote content id. *
   *
   * @param dctx The DispatchContext that this service is operating in.
   * @param context Map containing the input parameters.
   * @return Map with the result of the service, the output parameters.
   */

  public static Map<String, Object> fsdDeletePartyContent(DispatchContext dctx,
      Map<String, ? extends Object> context)
      throws GenericEntityException, GenericServiceException {
    Map<String, Object> result = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    LocalDispatcher dispatcher = dctx.getDispatcher();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String contentId = (String) context.get("contentId");
    String partyContentTypeId = (String) context.get("partyContentTypeId");
    String partyId = (String) context.get("partyId");
    String sequenceNum = (String) context.get("sequenceNum");
    String removeFromQuote = (String) context.get("removeFromQuote");
    Timestamp fromDate = (Timestamp) context.get("fromDate");
    Timestamp thruDate = (Timestamp) context.get("thruDate");

    // check content id in PartyContent entity
    GenericValue partyContent = EntityQuery.use(delegator).from("PartyContent")
        .where("partyId", partyId, "contentId", contentId).queryOne();

    if (UtilValidate.isNotEmpty(partyContent)) {
      //check if content is associated with any quote
      List<GenericValue> quoteContents = EntityQuery.use(delegator)
          .from("FsdQuoteContent")
          .where("contentId", contentId).filterByDate()
          .queryList();
      //set thru date of content if association to quote exists
      if (UtilValidate.isNotEmpty(quoteContents)) {
        if (removeFromQuote.equals("Y")) {
          for (GenericValue quoteContent : quoteContents) {
            String quoteId = quoteContent.getString("quoteId");
            if (UtilValidate.isNotEmpty(quoteContent)) {
              quoteContent.set("thruDate", UtilDateTime.nowTimestamp());
              GenericValue quote = delegator
                  .findOne("Quote", UtilMisc.toMap("quoteId", quoteId), false);
              quote.set("lastUpdatedStamp", UtilDateTime.nowTimestamp());
              quote.set("lastUpdatedTxStamp", UtilDateTime.nowTimestamp());
              delegator.store(quoteContent);
              delegator.store(quote);
            }
          }
        }
      }
      //now delete content from party
      partyContent.set("thruDate", UtilDateTime.nowTimestamp());
      partyContent.store();
    }
    return result;
  }


  /**
   * Method to handle multiple file upload functionality.
   */
  public static String uploadMultiplePartyContent(HttpServletRequest request,
      HttpServletResponse response) {
    LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
    GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");

    boolean bError = false;

    String partyIdField = "partyId";
    String uploadField = "dataResourceName";
    String partyContentTypeIdField = "partyContentTypeId";
    String isPublicField = "isPublic";

    String partyId = null;
    String partyContentTypeId = null;
    String isPublic = null;

    Map<String, Object> results = new HashMap<String, Object>();
    Map<String, String> formInput = new HashMap<String, String>();
    results.put("formInput", formInput);
    ServletFileUpload fu = new ServletFileUpload(
        new DiskFileItemFactory(10240, new File(new File("runtime"), "tmp")));
    List<FileItem> lst = null;
    try {
      lst = UtilGenerics.checkList(fu.parseRequest(request));
    } catch (FileUploadException e) {
      return "error";
    }

    if (lst.size() == 0) {
      return "error";
    }

    // This code finds the idField and the upload FileItems
    FileItem fi = null;
    int countImageFields = 0;
    List<Map<String, Object>> uploadResults = FastList.newInstance();
    for (int i = 0; i < lst.size(); i++) {
      fi = lst.get(i);
      String fieldName = fi.getFieldName();
      String fieldStr = fi.getString();
      if (fi.isFormField()) {
        formInput.put(fieldName, fieldStr);
      }
      if (fieldName.equals(partyIdField)) {
        partyId = fieldStr;
      } else if (fieldName.equals(partyContentTypeIdField)) {
        partyContentTypeId = fieldStr;
      } else if (fieldName.equals(isPublicField)) {
        isPublic = fieldStr;
      } else if (fieldName.equals(uploadField)) {
        countImageFields++;

        //MimeType of upload file
        Map<String, Object> map = FastMap.newInstance();
        map.put("uploadMimeType", fi.getContentType());

        byte[] imageBytes = fi.get();
        ByteBuffer byteWrap = ByteBuffer.wrap(imageBytes);
        map.put("imageData", byteWrap);
        map.put("imageFileName", fi.getName());
        uploadResults.add(map);
      }
    }

    if (UtilValidate.isEmpty(partyId)) {
      String errMsg = "Empty party Id";
      Debug.logError(errMsg, module);
      return "error";
    }

    if (countImageFields == 0) {
      String errMsg = "No files to upload";
      Debug.logWarning(errMsg, module);
      return "error";
    }

    for (Map<String, Object> uploadResult : uploadResults) {
      try {
        Map<String, Object> serviceResult;
        Map<String, Object> serviceContext = FastMap.newInstance();
        serviceContext.put("userLogin", userLogin);
        serviceContext.put("_uploadedFile_fileName", uploadResult.get("imageFileName"));
        serviceContext.put("uploadedFile", uploadResult.get("imageData"));
        serviceContext.put("_uploadedFile_contentType", uploadResult.get("uploadMimeType"));
        if ("N".equalsIgnoreCase(isPublic)) {
          serviceContext.put("isPublic", "N");
        }
        serviceResult = dispatcher.runSync("extCreateContentFromUploadedFile", serviceContext);
        if (!ServiceUtil.isSuccess(serviceResult)) {
          String errMsg = "extCreateContentFromUploadedFile failed";
          Debug.logError(errMsg, module);
          return "error";
        }
        String contentId = (String) serviceResult.get("contentId");

        if (UtilValidate.isNotEmpty(contentId)) {

          Map<String, Object> partyContentDuplicationCtx = FastMap.newInstance();
          partyContentDuplicationCtx.put("userLogin", userLogin);
          partyContentDuplicationCtx.put("partyId", partyId);
          partyContentDuplicationCtx.put("contentId", contentId);
          Map<String, Object> partyContentDuplication = dispatcher
              .runSync("checkForPartyContentDuplication", partyContentDuplicationCtx);
          if (!ServiceUtil.isSuccess(partyContentDuplication)) {
            String errMsg = ServiceUtil.getErrorMessage(partyContentDuplication);
            Debug.logError(errMsg, module);
            return "error";
          }

          Map<String, Object> createPartyContentCtx = FastMap.newInstance();
          createPartyContentCtx.put("userLogin", userLogin);
          createPartyContentCtx.put("partyId", partyId);
          createPartyContentCtx.put("contentId", contentId);
          createPartyContentCtx.put("partyContentTypeId", partyContentTypeId);
          createPartyContentCtx.put("fromDate", UtilDateTime.nowTimestamp());
          Map<String, Object> createPartyContentResp = dispatcher
              .runSync("createPartyContent", createPartyContentCtx);
          if (!ServiceUtil.isSuccess(createPartyContentResp)) {
            String errMsg = "createPartyContent failed";
            Debug.logError(errMsg, module);
            return "error";
          }
        }
      } catch (GenericServiceException e) {
        Debug.logError(e.getMessage(), module);
        bError = true;
      }

    }

    if (bError) {
      return "error";
    }
    return "success";
  }

  /**
   * Delete party content.
   */
  public static Map<String, Object> deletePartyContent(DispatchContext dctx,
      Map<String, ? extends Object> context)
      throws GenericEntityException, GenericServiceException {
    Map<String, Object> result = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    LocalDispatcher dispatcher = dctx.getDispatcher();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String contentId = (String) context.get("contentId");
    String partyId = (String) context.get("partyId");

    // check content id in PartyContent entity
    GenericValue partyContent = EntityQuery.use(delegator).from("PartyContent")
        .where("partyId", partyId, "contentId", contentId).queryOne();

    if (UtilValidate.isNotEmpty(partyContent)) {
      //now delete content from party
      partyContent.set("thruDate", UtilDateTime.nowTimestamp());
      partyContent.store();
    }
    return result;
  }

  /**
   * Create about party content.
   */
  public static Map<String, Object> createAboutPartyContent(DispatchContext dctx,
      Map<String, Object> context) throws GenericEntityException, GenericServiceException {

    if (Debug.verboseOn()) {
      Debug.logVerbose("Entering service method createAboutPartyContent.", module);
    }

    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    //setting default operation as of current service will be overridden on any other operation
    serviceResult.put("operation", "create");

    Delegator delegator = dctx.getDelegator();
    LocalDispatcher dispatcher = dctx.getDispatcher();

    GenericValue userLogin = (GenericValue) context.get("userLogin");

    String partyId = (String) context.get("partyId");
    String aboutText = (String) context.get("aboutText");

    List<EntityCondition> conditions = new LinkedList<>();
    conditions.add(EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId));
    conditions.add(EntityCondition
        .makeCondition("partyContentTypeId", EntityOperator.EQUALS, ABOUT_PARTY_CONTENT_ID));

    conditions.add(EntityCondition.makeCondition(EntityCondition.makeCondition(
        EntityCondition.makeCondition("thruDate",
            EntityOperator.GREATER_THAN_EQUAL_TO, UtilDateTime.nowTimestamp()),
        EntityOperator.OR,
        EntityCondition.makeCondition("thruDate", EntityOperator.EQUALS, null))));

    //Checking party content
    GenericValue partyContent = EntityQuery.use(delegator).from("PartyContent")
        .where(conditions).orderBy("-createdStamp").queryFirst();

    if (UtilValidate.isEmpty(partyContent)) {

      // createContent
      Map<String, Object> createTextContentCtx = UtilMisc.toMap(
          "userLogin", userLogin,
          "contentTypeId", ABOUT_PARTY_CONTENT_ID,
          "textData", aboutText);

      Map<String, Object> createTextContentResult = dispatcher
          .runSync("createTextContent", createTextContentCtx);

      if (!ServiceUtil.isSuccess(createTextContentResult)) {
        return createTextContentResult;
      }

      String contentId = (String) createTextContentResult.get("contentId");

      Map<String, Object> createPartyContentCtx = UtilMisc.toMap(
          "userLogin", userLogin,
          "partyId", partyId,
          "contentId", contentId,
          "createdByUserLogin", userLogin,
          "partyContentTypeId", ABOUT_PARTY_CONTENT_ID);
      Map<String, Object> createPartyContentResult = dispatcher
          .runSync("createPartyContent", createPartyContentCtx);

      if (!ServiceUtil.isSuccess(createPartyContentResult)) {
        return createPartyContentResult;
      }

      serviceResult.put("contentId", contentId);
    } else {

      Map<String, Object> updateAboutPartyContentCtx = UtilMisc.toMap(
          "userLogin", userLogin,
          "partyId", partyId,
          "aboutText", aboutText);

      Map<String, Object> updateAboutPartyContentResult = dispatcher
          .runSync("updateAboutPartyContent", updateAboutPartyContentCtx);

      if (!ServiceUtil.isSuccess(updateAboutPartyContentResult)) {
        return updateAboutPartyContentResult;
      }

      serviceResult.put("operation", "update");
    }

    if (Debug.verboseOn()) {
      Debug.logVerbose("Exit service method createAboutPartyContent.", module);
    }

    return serviceResult;
  }

  /**
   * Fetch about party content.
   */
  public static Map<String, Object> getAboutPartyContent(DispatchContext dctx,
      Map<String, Object> context) {

    if (Debug.verboseOn()) {
      Debug.logVerbose("Entering service method getAboutPartyContent.", module);
    }

    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    String partyId = (String) context.get("partyId");
    Delegator delegator = dctx.getDelegator();

    try {

      List<EntityCondition> conditions = new LinkedList<>();

      conditions
          .add(EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId));
      conditions.add(EntityCondition
          .makeCondition("partyContentTypeId", EntityOperator.EQUALS,
              ABOUT_PARTY_CONTENT_ID));

      conditions.add(EntityCondition.makeCondition(EntityCondition.makeCondition(
          EntityCondition.makeCondition("thruDate",
              EntityOperator.GREATER_THAN_EQUAL_TO, UtilDateTime.nowTimestamp()),
          EntityOperator.OR,
          EntityCondition.makeCondition("thruDate", EntityOperator.EQUALS, null))));

      GenericValue aboutPartyDetails = EntityQuery.use(delegator).from("PartyContent")
          .where(conditions)
          .queryFirst();

      if (UtilValidate.isNotEmpty(aboutPartyDetails)) {
        String contentId = aboutPartyDetails.getString("contentId");

        if (UtilValidate.isNotEmpty(contentId)) {
          serviceResult.put("contentId", contentId);
          try {
            // get the party Content
            GenericValue content = delegator
                .findOne("Content", UtilMisc.toMap("contentId", contentId), false);

            if (UtilValidate.isNotEmpty(content)) {
              String contentDescription = content.getString("description");
              serviceResult.put("contentDescription", contentDescription);

              // get the electronic Text
              String dataResourceId = content.getString("dataResourceId");

              GenericValue electronicText = delegator.findOne("ElectronicText",
                  UtilMisc.toMap("dataResourceId", dataResourceId), false);

              String textData = electronicText.getString("textData");

              if (UtilValidate.isNotEmpty(textData)) {
                serviceResult.put("aboutText", textData);
              }

            }
          } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
          }
        }
      }
    } catch (Exception e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }

    if (Debug.verboseOn()) {
      Debug.logVerbose("Exit service method getAboutPartyContent.", module);
    }

    return serviceResult;
  }

  /**
   * Update about party content.
   */
  public static Map<String, Object> updateAboutPartyContent(DispatchContext dctx,
      Map<String, Object> context) {

    if (Debug.verboseOn()) {
      Debug.logVerbose("Entering service method updateAboutPartyContent.", module);
    }

    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    //setting default operation as of current service will be overridden on any other operation
    serviceResult.put("operation", "update");
    LocalDispatcher dispatcher = dctx.getDispatcher();

    String partyId = (String) context.get("partyId");
    String aboutText = (String) context.get("aboutText");

    GenericValue userLogin = (GenericValue) context.get("userLogin");
    Delegator delegator = dctx.getDelegator();

    try {

      List<EntityCondition> conditions = new LinkedList<>();

      conditions
          .add(EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId));
      conditions.add(EntityCondition
          .makeCondition("partyContentTypeId", EntityOperator.EQUALS,
              ABOUT_PARTY_CONTENT_ID));

      conditions.add(EntityCondition.makeCondition(EntityCondition.makeCondition(
          EntityCondition.makeCondition("thruDate",
              EntityOperator.GREATER_THAN_EQUAL_TO, UtilDateTime.nowTimestamp()),
          EntityOperator.OR,
          EntityCondition.makeCondition("thruDate", EntityOperator.EQUALS, null))));

      GenericValue partyContent = EntityQuery.use(delegator).from("PartyContent")
          .where(conditions).orderBy("-createdStamp").queryFirst();

      if (UtilValidate.isNotEmpty(partyContent)) {
        String contentId = partyContent.getString("contentId");
        //deleting party about content in case passed text is empty
        if (UtilValidate.isEmpty(aboutText)) {

          partyContent.set("thruDate", UtilDateTime.nowTimestamp());
          partyContent.store();

          serviceResult.put("operation", "delete");

          return serviceResult;
        }

        // get the party Content
        GenericValue content = delegator
            .findOne("Content", UtilMisc.toMap("contentId", contentId), false);
        if (UtilValidate.isNotEmpty(content)) {

          String dataResourceId = content.getString("dataResourceId");

            try {
              if (UtilValidate.isNotEmpty(dataResourceId)) {
                Map<String, Object> updateElectronicTextCtx = new HashMap<>();
                updateElectronicTextCtx.put("userLogin", userLogin);
                updateElectronicTextCtx.put("dataResourceId", dataResourceId);
                updateElectronicTextCtx.put("textData", aboutText);
                dispatcher.runSync("updateElectronicText", updateElectronicTextCtx);
              }
            } catch (GenericServiceException e) {
              Debug.logError("An Exception occurred while calling the updateElectronicText service" + e.getMessage(), module);
              return ServiceUtil.returnError(e.getMessage());
            }
        }
      } else {
        Map<String, Object> createPartyContentCtx = UtilMisc.toMap(
            "userLogin", userLogin,
            "partyId", partyId,
            "aboutText", aboutText);

        Map<String, Object> createPartyContentResult = dispatcher
            .runSync("createAboutPartyContent", createPartyContentCtx);

        if (!ServiceUtil.isSuccess(createPartyContentResult)) {
          return createPartyContentResult;
        }

        serviceResult.put("operation", "create");
      }
    } catch (GenericEntityException | GenericServiceException e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }

    if (Debug.verboseOn()) {
      Debug.logVerbose("Exit service method updateAboutPartyContent.", module);
    }

    return serviceResult;
  }

}
