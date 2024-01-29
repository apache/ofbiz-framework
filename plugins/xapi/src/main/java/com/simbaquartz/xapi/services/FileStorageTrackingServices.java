package com.simbaquartz.xapi.services;

import com.fidelissd.zcp.xcommon.collections.FastMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.content.data.DataResourceWorker;
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

/** Services related to File Storage Tracking services. */
public class FileStorageTrackingServices {
  public static final String module = FileStorageTrackingServices.class.getName();
  private static BigDecimal DEFAULT_STORAGE_LIMIT =
      new BigDecimal(20000); // 20GB - fallback if SystemProperty is missing for tenant
  public static final String TENANT_MAX_STORAGE_KEY = "max.storage.limit";

  /**
   * Returns the file size of a file stored as a content. Locates the file using stored path and
   * calculates the file size, also returns the {@link File} object store in response.file.
   *
   * <p>In case the file is not found returns the fileSize as {@link BigDecimal.ZERO} and null for
   * file.
   *
   * @param dctx
   * @param context
   * @return
   */
  public static Map<String, Object> getFileSizeFromContent(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> result = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    String contentId = (String) context.get("contentId");

    GenericValue content;
    try {
      content = EntityQuery.use(delegator).from("Content").where("contentId", contentId).queryOne();

      if (content == null) {
        String errorMsg = "No content found for Content ID: " + contentId;
        Debug.logError(errorMsg, module);
        return ServiceUtil.returnError(
            "An error occurred while trying to fetch content record. See Logs for details.");
      }

      Long fileSizeValue = content.getLong("fileSize");

      // check if the record already has the file size, if the file was uploaded to a cloud bucket
      // most likely the calculation of file size using the file will fail.
      if (UtilValidate.isNotEmpty(fileSizeValue)) {
        BigDecimal fileSize = new BigDecimal(fileSizeValue);
        BigDecimal fileSizeInBytes = fileSize.divide(new BigDecimal(1000000));
        Debug.logInfo("file size in mb: " + fileSizeInBytes + " mb", module);
        result.put("fileSize", fileSizeInBytes);
        result.put("file", null);
      } else {
        String dataResourceId = content.getString("dataResourceId");
        if (UtilValidate.isEmpty(dataResourceId)) {
          String errorMsg =
              "No Data Resource found for Content ID: " + contentId + ". Doing nothing.";
          Debug.logError(errorMsg, module);
        }
        GenericValue dataResource =
            EntityQuery.use(delegator)
                .from("DataResource")
                .where("dataResourceId", dataResourceId)
                .queryOne();
        String objectInfo = dataResource.getString("objectInfo");
        String dataResourceTypeId = dataResource.getString("dataResourceTypeId");

        if (UtilValidate.isNotEmpty(objectInfo)) {
          try {
            File fileToUpload =
                DataResourceWorker.getContentFile(dataResourceTypeId, objectInfo, "");
            BigDecimal fileSize = new BigDecimal(fileToUpload.length());
            BigDecimal fileSizeInBytes = fileSize.divide(new BigDecimal(1000000));
            Debug.logInfo("file size in mb: " + fileSizeInBytes + " mb", module);
            result.put("fileSize", fileSizeInBytes);
            result.put("file", fileToUpload);
          } catch (FileNotFoundException e) {
            Debug.logError(e, "Unable to open the file. Returning size as 0.", module);
            result.put("fileSize", BigDecimal.ZERO);
            result.put("file", null);
          }
        }
      }

    } catch (Exception e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(
          "An error occurred while trying to fetch content record. See Logs for details.");
    }

    return result;
  }

  /**
   * Updates and records the storage used for a given document type (storageType)
   *
   * @param dctx
   * @param context
   * @return
   */
  public static Map<String, Object> recordStorageUsed(
      DispatchContext dctx, Map<String, Object> context) {
    Delegator delegator = dctx.getDelegator();
    LocalDispatcher dispatcher = dctx.getDispatcher();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String accountId = (String) context.get("accountId");
    String attributeName = (String) context.get("storageType");
    BigDecimal uploadedFileSize = (BigDecimal) context.get("uploadedFileSize");

    createUpdateStorage(
        delegator, dispatcher, attributeName, uploadedFileSize, userLogin, accountId);

    // Update total storage
    createUpdateStorage(
        delegator, dispatcher, "totalStorageUsed", uploadedFileSize, userLogin, accountId);

    Map<String, Object> getStorageUsedCtx = FastMap.newInstance();
    getStorageUsedCtx.put("accountId", accountId);
    getStorageUsedCtx.put("userLogin", userLogin);

    Map<String, Object> getStorageUsedResponse;
    try {
      getStorageUsedResponse = dispatcher.runSync("getStorageUsed", getStorageUsedCtx);
    } catch (GenericServiceException e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError("Unable to update storage used info");
    }

    getStorageUsedResponse.put("storageType", attributeName);
    return getStorageUsedResponse;
  }

  /**
   * Creates or updates the storage for an account
   *
   * @param delegator
   * @param dispatcher
   * @param attributeName example storageType (orderStorage/taskStorage)
   * @param uploadedFileSize
   * @param userLogin
   * @param accountId partyId of the owner account
   */
  public static void createUpdateStorage(
      Delegator delegator,
      LocalDispatcher dispatcher,
      String attributeName,
      BigDecimal uploadedFileSize,
      GenericValue userLogin,
      String accountId) {

    try {
      GenericValue storageUsed =
          EntityQuery.use(delegator)
              .from("PartyAttribute")
              .where(UtilMisc.toMap("partyId", accountId, "attrName", attributeName))
              .queryOne();

      if (UtilValidate.isEmpty(storageUsed)) {
        Map<String, Object> createPartyAttributeCtx = FastMap.newInstance();
        createPartyAttributeCtx.put("userLogin", userLogin);
        createPartyAttributeCtx.put("partyId", accountId);
        createPartyAttributeCtx.put("attrName", attributeName);
        createPartyAttributeCtx.put("attrValue", String.valueOf(uploadedFileSize));

        dispatcher.runSync("createPartyAttribute", createPartyAttributeCtx);
      } else {
        Map<String, Object> updatePartyAttributeCtx = FastMap.newInstance();
        updatePartyAttributeCtx.put("userLogin", userLogin);
        updatePartyAttributeCtx.put("partyId", accountId);
        updatePartyAttributeCtx.put("attrName", attributeName);
        BigDecimal updatedFileSize =
            new BigDecimal((String) storageUsed.get("attrValue")).add(uploadedFileSize);
        if (updatedFileSize.compareTo(BigDecimal.ZERO) == 0) {
          updatePartyAttributeCtx.put("attrValue", String.valueOf(BigDecimal.ZERO));
        } else {
          updatePartyAttributeCtx.put("attrValue", String.valueOf(updatedFileSize));
        }

        dispatcher.runSync("updatePartyAttribute", updatePartyAttributeCtx);
      }
    } catch (GenericEntityException | GenericServiceException e) {
      Debug.logError("An error occurred while trying to add/update storage.", module);
    }
  }

  /**
   * Archives/deletes a file.
   *
   * @param dctx
   * @param context
   * @return
   */
  public static Map<String, Object> moveFileToArchive(
      DispatchContext dctx, Map<String, Object> context) {
    Delegator delegator = dctx.getDelegator();

    File file = (File) context.get("file");

    boolean moveToArchiveDirectoryEnabled =
        false; // enable this to not physically delete the file and move to archieve folder instead.
    if (!moveToArchiveDirectoryEnabled) {
      if (file.delete()) {
        Debug.logInfo("File [" + file.getName() + "] deleted successfully!", module);
      } else {
        Debug.logWarning("File [" + file.getName() + "] deletion failed!", module);
      }
    } else {
      String initialPath =
          EntityUtilProperties.getPropertyValue("content", "content.upload.path.prefix", delegator);
      String ofbizHome = System.getProperty("ofbiz.home");

      if (!initialPath.startsWith("/")) {
        initialPath = "/" + initialPath;
      }
      String parentDir = ofbizHome + initialPath + "/archieve/";
      // Create if the archieve dirctory doesn't already exist
      if (!new File(parentDir).exists()) {
        new File(parentDir).mkdir();
      }
      if (file.renameTo(new File(parentDir + file.getName()))) {
        Debug.log("File archived successfully!", module);
      } else {
        Debug.log("Failed to archive file!", module);
      }
    }
    return ServiceUtil.returnSuccess();
  }

  public static Map<String, Object> isStorageLimitCrossed(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> result = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    LocalDispatcher dispatcher = dctx.getDispatcher();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String planId = (String) context.get("planId");
    Boolean isStorageLimitCrossed = Boolean.FALSE;
    BigDecimal uploadedFileSize = (BigDecimal) context.get("uploadedFileSize");

    BigDecimal maxStorageLimit =
        new BigDecimal(
            EntityUtilProperties.getPropertyValue(
                "subscription",
                TENANT_MAX_STORAGE_KEY,
                String.valueOf(DEFAULT_STORAGE_LIMIT),
                delegator));
    Map<String, Object> getStorageUsedCtx = FastMap.newInstance();
    getStorageUsedCtx.put("userLogin", userLogin);
    Map<String, Object> getStorageUsedResponse;
    try {
      getStorageUsedResponse = dispatcher.runSync("getStorageUsed", getStorageUsedCtx);

      BigDecimal totalStorageUsed = BigDecimal.ZERO;
      if (UtilValidate.isNotEmpty(getStorageUsedResponse.get("totalStorageUsed"))) {
        totalStorageUsed = (BigDecimal) getStorageUsedResponse.get("totalStorageUsed");
      }

      if (UtilValidate.isNotEmpty(maxStorageLimit) && UtilValidate.isNotEmpty(totalStorageUsed)) {
        BigDecimal remainingStorageLeft =
            maxStorageLimit.subtract(totalStorageUsed.add(uploadedFileSize));
        if (UtilValidate.isNotEmpty(remainingStorageLeft)) {
          if (remainingStorageLeft.compareTo(BigDecimal.ZERO) < 0) {
            isStorageLimitCrossed = Boolean.TRUE;
          }
        }
      }

      result.put("isStorageLimitCrossed", isStorageLimitCrossed);
      result.put("totalStorageUsed", totalStorageUsed);
    } catch (GenericServiceException e) {
      return ServiceUtil.returnError("An error occurred while trying to check storage limit.");
    }

    return result;
  }

  /**
   * Returns the total storage available and total used with breakdown of types of storage
   *
   * @param dctx
   * @param context
   * @return
   */
  public static Map<String, Object> getStorageUsed(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> result = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    String accountId = (String) context.get("accountId");

    try {
      List<GenericValue> storageStats =
          EntityQuery.use(delegator)
              .from("PartyAttribute")
              .where(
                  EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, accountId),
                  EntityCondition.makeCondition(
                      "attrName", EntityOperator.IN, FileStorageTypesEnum.getTypeIds()))
              .queryList();

      if (UtilValidate.isNotEmpty(storageStats)) {
        for (GenericValue storageStat : storageStats) {
          result.put(
              (String) storageStat.get("attrName"),
              new BigDecimal((String) storageStat.get("attrValue")));
        }
      }
    } catch (GenericEntityException e) {
      return ServiceUtil.returnError("An error occurred while trying to get storage size.");
    }
    return result;
  }
}
