package com.simbaquartz.xparty.services;

import com.fidelissd.zcp.xcommon.util.AxUtilFormat;
import com.fidelissd.zcp.xcommon.util.FileUtils;
import com.fidelissd.zcp.xcommon.util.hierarchy.HierarchyUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import com.fidelissd.zcp.xcommon.collections.FastMap;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * Created by Jaskaran on 13/05/2021.
 */
public class PartyFeedbackServices {

  public static final String module = PartyFeedbackServices.class.getName();

  public static class PartyFeedbackServicesErrorMessages {

    public static final String SYSTEM_ERROR_RESP = "Something went wrong, please try later.";
  }

  /**
   * Service to submit party feedBack(ratings)
   *
   * @param context input params : partyId - for whom loggedIn user is submitting feedback(rating),
   * comments- comments given by the loggedIn user while submitting feedback(rating),
   * experienceRating - given to the party 0, 1, 2, 3, 4, 5 attachments: attachment with the object
   * params name and encodedFile (ecnoded file is the base 64 conversion for the attached file)
   * @return : feedbackId
   */

  public static Map<String, Object> submitPartyFeedback(DispatchContext dctx,
      Map<String, Object> context) {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();

    GenericValue userLogin = (GenericValue) context.get("userLogin");
    LocalDispatcher dispatcher = dctx.getDispatcher();
    Delegator delegator = dctx.getDelegator();

    String partyId = (String) context.get("partyId");
    String comments = (String) context.get("comments");
    String submittedByParty = (String) context.get("submittedByParty");
    List attachments = (List) context.get("attachments");
    long experienceRating = 0L;
    if (UtilValidate.isNotEmpty(context.get("experienceRating"))) {
      experienceRating = (long) context.get("experienceRating");
    }

    List<String> feedbackNotes = UtilGenerics.toList(context.get("feedbackNotes"));

    try {
      GenericValue feedbackGv = delegator.makeValue("PartyFeedback");
      String feedbackId = delegator.getNextSeqId("PartyFeedback");
      feedbackGv.set("feedbackId", feedbackId);
      feedbackGv.set("partyId", partyId);
      feedbackGv.set("submittedBy", userLogin.get("partyId"));

      feedbackGv.set("comments", comments);
      feedbackGv.set("experienceRating", experienceRating);

      delegator.createOrStore(feedbackGv);

      List<GenericValue> feedbackAttributes = new LinkedList<>();
      if (UtilValidate.isNotEmpty(feedbackNotes)) {
        for (String feedbackNote : feedbackNotes) {
          GenericValue feedbackNoteGv = delegator.makeValue("FeedbackNote");
          String feedbackNoteId = delegator.getNextSeqId("FeedbackNote");
          feedbackNoteGv.set("feedbackId", feedbackId);
          feedbackNoteGv.set("note", feedbackNote);
          feedbackNoteGv.set("noteId", feedbackNoteId);
          feedbackAttributes.add(feedbackNoteGv);
        }
      }
      if (UtilValidate.isNotEmpty(feedbackAttributes)) {
        delegator.storeAll(feedbackAttributes);
      }

      //saving the feedback attachments
      if (UtilValidate.isNotEmpty(attachments)) {
        try {
          Map<String, Object> addPartyFeedbackAttachmentsResp =
              dispatcher.runSync("addPartyFeedbackAttachments",
                  UtilMisc.toMap("userLogin", userLogin, "feedbackId", feedbackId,
                      "attachments", attachments));

          if (ServiceUtil.isError(addPartyFeedbackAttachmentsResp)) {
            String errorMsg = ServiceUtil.getErrorMessage(addPartyFeedbackAttachmentsResp);
            Debug.logError(errorMsg, module);
            return ServiceUtil.returnError(errorMsg);
          }
        } catch (GenericServiceException e) {
          Debug.logError(e, module);
          return ServiceUtil.returnError(e.getMessage());
        }
      }

      serviceResult.put("feedbackId", feedbackId);
    } catch (GenericEntityException e) {
      Debug.logError(
          "An error occurred while trying to submit party feedback, details: " + e.getMessage(),
          module);
      return ServiceUtil.returnError(e.getMessage());
    }

    //updating party net ratings
    //populating party basic details
    try {
      dispatcher.runSync(
          "populateBasicInformationForParty",
          UtilMisc.toMap(
              "partyId", partyId,
              "overrideExistingValues", true,
              "userLogin", userLogin));
    } catch (GenericServiceException e) {
      Debug.logError(e, "An error occurred while updating party details.", module);
    }

    return serviceResult;
  }

  public static Map<String, Object> getPartyFeedbackList(DispatchContext dctx,
      Map<String, Object> context) {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
  LocalDispatcher dispatcher = dctx.getDispatcher();
    Delegator delegator = dctx.getDelegator();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String partyId = (String) context.get("partyId");

    try {

      List<GenericValue> feedbackList = EntityQuery.use(delegator).from("PartyFeedback")
          .where("partyId", partyId).queryList();

      List<Map<String, Object>> feedbackMapList = new LinkedList<>();
      for (GenericValue feedbackObj : feedbackList) {

        if (UtilValidate.isNotEmpty(feedbackObj)) {
          String feedbackId = feedbackObj.getString("feedbackId");
          List<String> feedbackNotes = EntityQuery.use(delegator).from("FeedbackNote")
              .where("feedbackId", feedbackId).getFieldList("note");

          Map<String, Object> feedback = new HashMap<>();

          feedback.put("feedbackId", feedbackId);
          feedback.put("partyId", feedbackObj.get("partyId"));
          feedback.put("experienceRating", feedbackObj.get("experienceRating"));
          feedback.put("comments", feedbackObj.get("comments"));

          GenericValue submitter =
              HierarchyUtils.getPartyByPartyId(delegator, feedbackObj.getString("submittedBy"));

          feedback.put("submittedBy", submitter);
          feedback.put("feedbackNotes", feedbackNotes);

          Map<String, Object> getFeedbackAttachmentsResp;
          try {
            getFeedbackAttachmentsResp = dispatcher.runSync("getFeedbackAttachments",
                UtilMisc.toMap("userLogin", userLogin, "feedbackId", feedbackId));
          } catch (GenericServiceException e) {
            Debug
                .logError("An error occurred while invoking getPartyFeedbackList service, details: "
                    + e.getMessage(), module);
            return ServiceUtil.returnError(e.getMessage());
          }
          if (ServiceUtil.isError(getFeedbackAttachmentsResp)) {
            String serviceError = ServiceUtil.getErrorMessage(getFeedbackAttachmentsResp);
            Debug.logError("An error occurred while invoking getFeedbackAttachments service, "
                + "details: " + serviceError, module);
            return ServiceUtil.returnError(serviceError);
          }

          feedback.put("attachments", getFeedbackAttachmentsResp.get("attachments"));

          feedbackMapList.add(feedback);
        }

      }
      serviceResult.put("feedbackList", feedbackMapList);
    } catch (GenericEntityException e) {
      Debug.logError(
          "An error occurred while trying to get party feedback, details: " + e.getMessage(),
          module);
      return ServiceUtil.returnError(e.getMessage());
    }
    return serviceResult;
  }

  public static Map<String, Object> deletePartyFeedback(DispatchContext dctx,
      Map<String, Object> context) {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    String feedbackId = (String) context.get("feedbackId");
    try {
      GenericValue partyFeedback = EntityQuery.use(delegator).from("PartyFeedback")
          .where("feedbackId", feedbackId).queryFirst();
      if (UtilValidate.isNotEmpty(partyFeedback)) {
        List<GenericValue> dataToDelete = new LinkedList<>();
        List<GenericValue> feedbackNotes = EntityQuery.use(delegator).from("FeedbackNote").where
            ("feedbackId", feedbackId).queryList();
        dataToDelete.addAll(feedbackNotes);
        List<GenericValue> partyFeedbackContents = EntityQuery.use(delegator).from
            ("PartyFeedbackContent").where("feedbackId", feedbackId).queryList();
        dataToDelete.addAll(partyFeedbackContents);
        if (UtilValidate.isNotEmpty(dataToDelete)) {
          delegator.removeAll(dataToDelete);
        }

        if (UtilValidate.isNotEmpty(partyFeedback)) {
          partyFeedback.remove();
        }
      }
    } catch (GenericEntityException e) {
      Debug.logError(e, module);
      ServiceUtil.returnError(e.getMessage());
    }
    return serviceResult;
  }

  public static Map<String, Object> addPartyFeedbackAttachments(DispatchContext ctx,
      Map<String, Object> context) {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();

    LocalDispatcher dispatcher = ctx.getDispatcher();
    Delegator delegator = ctx.getDelegator();

    GenericValue userLogin = (GenericValue) context.get("userLogin");

    String feedbackId = (String) context.get("feedbackId");
    List<Map> attachments = (List<Map>) context.get("attachments");
    try {

      for (Map attachment : attachments) {

        String encodedFile = (String) attachment.get("encodedFile");
        String fileName = (String) attachment.get("name");

        // Note preferred way of declaring an array variable
        String delims = "[,]";
        String[] parts = encodedFile.split(delims);
        String imageString = parts[1];
        byte[] data = Base64.getDecoder().decode(imageString);
        InputStream is = new ByteArrayInputStream(data);
        //Find out image type
        String mimeType = null;
        String fileExtension = null;
        try {
          mimeType = URLConnection
              .guessContentTypeFromStream(is); //mimeType is something like "image/jpeg"
          String delimiter = "[/]";
          String[] tokens = mimeType.split(delimiter);
          fileExtension = tokens[1];
        } catch (IOException ioException) {
          Debug.logError(ioException, module);
          if (Debug.verboseOn()) {
            Debug.logVerbose("Exiting method addAttachment", module);
          }
          return ServiceUtil.returnError(ioException.getMessage());
        }

        Timestamp now = UtilDateTime.nowTimestamp();
        try {
          //convert the uploaded file to inputstream
          ByteBuffer byteBuffer = ByteBuffer.wrap(data);

          Map<String, Object> serviceContext = FastMap.newInstance();
          serviceContext.put("userLogin", userLogin);
          serviceContext.put("_uploadedFile_fileName", fileName);
          serviceContext.put("uploadedFile", byteBuffer);
          serviceContext.put("_uploadedFile_contentType", mimeType);
          Map<String, Object> createContentFromUploadedFileResp = dispatcher
              .runSync("createContentFromUploadedFile", serviceContext);
          String contentId = (String) createContentFromUploadedFileResp.get("contentId");

          Map<String, Object> fileUploadServiceResponse = null;

          //creating content relation with the party feedback
          GenericValue partyFeedbackContent = delegator.makeValue("PartyFeedbackContent");
          partyFeedbackContent.set("feedbackId", feedbackId);
          partyFeedbackContent.set("contentId", contentId);
          partyFeedbackContent.set("fromDate", now);
          delegator.createOrStore(partyFeedbackContent);

        } catch (GenericServiceException e) {
          Debug.logError(e, module);
          if (Debug.verboseOn()) {
            Debug.logVerbose("Exiting method addPartyFeedbackAttachments", module);
          }
          return ServiceUtil.returnError(e.getMessage());
        }
      }
    } catch (GenericEntityException e) {
      Debug.logError(e, module);
      ServiceUtil.returnError(e.getMessage());
    }
    return serviceResult;
  }

  /**
   * To get feedback attachments.
   */
  public static Map<String, Object> getFeedbackAttachments(DispatchContext dctx,
      Map<String, Object> context) throws GenericEntityException {

    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();

    String feedbackId = (String) context.get("feedbackId");
    List<Map> feedbackContents = new LinkedList<>();

    List<EntityCondition> cond = new LinkedList<>();

    cond.add(EntityCondition.makeCondition("feedbackId", EntityOperator.EQUALS, feedbackId));
    cond.add(EntityCondition
        .makeCondition("fromDate", EntityOperator.LESS_THAN_EQUAL_TO, UtilDateTime.nowTimestamp()));
    cond.add(EntityCondition.makeCondition(EntityCondition
            .makeCondition("thruDate", EntityOperator.GREATER_THAN_EQUAL_TO,
                UtilDateTime.nowTimestamp()),
        EntityOperator.OR, EntityCondition.makeCondition("thruDate", EntityOperator.EQUALS, null)));

    List<GenericValue> feedbackContentList = EntityQuery.use(delegator).from
        ("PartyFeedbackContent").where(cond).queryList();

    if (feedbackContentList != null) {
      for (GenericValue feedbackContent : feedbackContentList) {

        String contentId = (String) feedbackContent.get("contentId");

        Map<String, Object> feedbackContentInfo = FastMap.newInstance();

        feedbackContentInfo.putAll(feedbackContent);

        GenericValue contentRecd = EntityQuery.use(delegator).from("Content")
            .where("contentId", contentId).queryFirst();

        // make sure there is a DataResource for this content
        String dataResourceId = contentRecd.getString("dataResourceId");
        if (UtilValidate.isEmpty(dataResourceId)) {
          String errorMsg =
              "No Data Resource found for Content ID: " + contentId + ". Doing nothing.";
          Debug.logError(errorMsg, module);

          //failing silently so transaction doesn't get rolled back
          return ServiceUtil.returnSuccess(errorMsg);
        }

        // get the data resource
        GenericValue dataResource;
        try {

          dataResource = EntityQuery.use(delegator).from("DataResource")
              .where("dataResourceId", dataResourceId).queryOne();

        } catch (GenericEntityException e) {
          Debug.logError(e, module);
          return ServiceUtil.returnError(e.getMessage());
        }

        // make sure the data resource exists
        if (dataResource == null) {
          String errorMsg = "No Data Resource found for ID: " + dataResourceId;
          Debug.logError(errorMsg, module);
          return ServiceUtil.returnError(errorMsg);
        }

        String dataResourceName = (String) dataResource.get("dataResourceName");
        dataResourceName = dataResourceName.replaceAll("[ -()]", "_");

        //generate publicly accessible thumbnail url
        // get the data resource
        String newThumbNailFilePath = System.getProperty("ofbiz.home") +
            "/plugins/xstatic/webapp/xstatic/static/pub/party/content/feedback/" + dataResourceName;
        String thumbNailUrl = "";
        String oldThumbNailFilePath = dataResource.getString("objectInfo");
        Path filePath = Paths.get(newThumbNailFilePath);
        boolean fileExists = Files.exists(filePath);

        if (fileExists) {
          Debug.logInfo("File exists!", module);
          thumbNailUrl = EntityUtilProperties
              .getPropertyValue("url.properties", "content.url.prefix.secure", delegator) +
              "/pub/party/content/feedback/" + dataResourceName;
        } else {

          boolean result = FileUtils.copyFile(newThumbNailFilePath, oldThumbNailFilePath);
          Debug.logInfo("Image copied successfully.", module);
          if (result) {
            thumbNailUrl = EntityUtilProperties
                .getPropertyValue("url.properties", "content.url.prefix.secure", delegator)
                + "/pub/party/feedback/" + dataResourceName;
          } else {
            Debug.logInfo("Could not copy image.", module);
          }
        }

        feedbackContentInfo.put("name", dataResourceName);
        feedbackContentInfo.put("thumbNailUrl", thumbNailUrl);

        GenericValue taskFileSize = EntityQuery.use(delegator).select("fileSize").from("Content")
            .where("contentId", contentId).queryOne();

        Long fileSize = taskFileSize.getLong("fileSize");

        if (UtilValidate.isNotEmpty(fileSize)) {

          feedbackContentInfo.put("fileSize", fileSize);
          feedbackContentInfo.put("formattedFileSize", AxUtilFormat.formatFileSize(fileSize));

        }

        feedbackContents.add(feedbackContentInfo);
      }
    }
    serviceResult.put("attachments", feedbackContents);
    return serviceResult;
  }

}
