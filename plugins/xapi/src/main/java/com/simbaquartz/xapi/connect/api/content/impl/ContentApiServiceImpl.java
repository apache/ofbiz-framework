package com.simbaquartz.xapi.connect.api.content.impl;

import com.simbaquartz.xapi.connect.api.NotFoundException;
import com.simbaquartz.xapi.connect.api.content.ContentApiService;
import com.simbaquartz.xapi.connect.api.security.LoggedInUser;
import com.simbaquartz.xapi.connect.utils.ApiResponseUtil;
import com.simbaquartz.xapi.connect.utils.XapiUtil;
import com.simbaquartz.xapi.utils.ByteBufferBackedInputStream;
import com.fidelissd.zcp.xcommon.collections.FastMap;
import org.apache.commons.io.IOUtils;
import org.apache.ofbiz.base.util.*;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2018-05-18T14:37:04.465+05:30")
public class ContentApiServiceImpl extends ContentApiService {

    private static final String module = ContentApiServiceImpl.class.getName();
    private static final String THUMBNAIL_ASSOCIATION_TYPE_ID = "THUMBNAIL_URL";

    @Override
    public Response getThumbNailUrl(String contentId, SecurityContext securityContext) throws NotFoundException {

        Map <String, Object> getThumbNailUrlResp = FastMap.newInstance();

        try {

            contentId = Base64.base64Decode(contentId);

            // check content id in PartyContent entity
            GenericValue existingContentThumbnailAssociation = EntityQuery.use(delegator).from("ContentAssoc").where("contentId", contentId, "contentAssocTypeId", THUMBNAIL_ASSOCIATION_TYPE_ID).queryFirst();

            if (UtilValidate.isEmpty(existingContentThumbnailAssociation)) {

                String errorMessage = "There exist no Content Id " + contentId;
                Debug.logError(errorMessage, module);
                return ApiResponseUtil.prepareDefaultResponse(Response.Status.BAD_REQUEST, errorMessage);

            } else {

                // get the content record
                GenericValue thumbNailContent;
                thumbNailContent = EntityQuery.use(delegator).from("Content").where("contentId", existingContentThumbnailAssociation.getString("contentIdTo")).queryOne();

                // make sure content exists
                if (thumbNailContent == null) {

                    String errorMsg = "No content found for Content ID: " + contentId;
                    Debug.logError(errorMsg, module);
                    return ApiResponseUtil.prepareDefaultResponse(Response.Status.BAD_REQUEST, errorMsg);

                }

                // make sure there is a DataResource for this content
                String dataResourceId = thumbNailContent.getString("dataResourceId");

                if (UtilValidate.isEmpty(dataResourceId)) {

                    String errorMsg = "No Data Resource found for Content ID: " + contentId + ". Doing nothing.";
                    Debug.logError(errorMsg, module);
                    //failing silently so transaction doesn't get rolled back
                    return ApiResponseUtil.prepareDefaultResponse(Response.Status.BAD_REQUEST, errorMsg);

                }

                // get the data resource
                GenericValue dataResource;
                dataResource = EntityQuery.use(delegator).from("DataResource").where("dataResourceId", dataResourceId).queryOne();
                String dataResourceName = (String) dataResource.get("dataResourceName");

                String newThumbNailFilePath = System.getProperty("ofbiz.home") + "/plugins/xstatic/webapp/xstatic/static/pub/party/content/" + dataResourceName;
                String thumbNailUrl="";
                String oldThumbNailFilePath = dataResource.getString("objectInfo");

                Path filePath = Paths.get(newThumbNailFilePath);
                boolean fileExists = Files.exists(filePath);

                String cdnHostUrl = EntityUtilProperties.getPropertyValue("url", "content.url.prefix.secure", delegator);

                if (fileExists) {
                    Debug.logInfo("File exists!", module);
                    thumbNailUrl = cdnHostUrl + "/pub/party/content/" + dataResourceName;
                    getThumbNailUrlResp.put("thumbNailUrl", thumbNailUrl);
                } else {
                    boolean result = copyFile(newThumbNailFilePath, oldThumbNailFilePath);
                    Debug.logInfo("Image copied successfully.", module);
                    if (result) {
                        thumbNailUrl = cdnHostUrl + "/pub/party/content/" + dataResourceName;
                        getThumbNailUrlResp.put("thumbNailUrl", thumbNailUrl);
                    } else {
                        Debug.logInfo("Could not copy image.", module);
                    }
                }

            }

        } catch (GenericEntityException e) {
            Debug.logError("An error occurred while invoking getThumbNailUrl service, details: " + e.getMessage(), "PartyApiServiceImpl");
            if (Debug.verboseOn())
                Debug.logVerbose("Exiting method downloadPartyContent", module);

            return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR);
        }
        return ApiResponseUtil.prepareOkResponse(getThumbNailUrlResp);
    }

    public static Boolean copyFile(String newFilePath, String oldFilePath) {
        try {
            FileInputStream Fread = new FileInputStream(oldFilePath);
            FileOutputStream Fwrite = new FileOutputStream(newFilePath);
            Debug.logInfo("File is Copied", module);
            int c;
            while ((c = Fread.read()) != -1)
                Fwrite.write((char) c);
            Fread.close();
            Fwrite.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public Response uploadAttachment(MultipartFormDataInput attachment, javax.ws.rs.core.SecurityContext securityContext)
            throws NotFoundException {

        if (Debug.verboseOn())
            Debug.logVerbose("Entering method uploadImage", module);

        Map<String, List<InputPart>> uploadForm = attachment.getFormDataMap();
        List<InputPart> inputParts = uploadForm.get("file");

        LoggedInUser loggedInUser = (LoggedInUser) securityContext.getUserPrincipal();
        LocalDispatcher tenantDispatcher = loggedInUser.getDispatcher();
        Map<String, Object> uploadedFileResponseJSON = FastMap.newInstance();

        if (null != inputParts) {
            for (InputPart inputPart : inputParts) {
                try {
                    MultivaluedMap<String, String> header = inputPart.getHeaders();
                    String fileName = XapiUtil.getFileName(header);

                    InputStream inputStream = inputPart.getBody(InputStream.class, null);
                    byte[] bytes = IOUtils.toByteArray(inputStream);
                    ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
                    Map fileFormMap = FastMap.newInstance();
                    fileFormMap.put("imageFileName", fileName);
                    fileFormMap.put("uploadMimeType", inputPart.getMediaType().toString());
                    fileFormMap.put("imageData", byteBuffer);

                    Map<String, Object> serviceContext = FastMap.newInstance();
                    serviceContext.put("userLogin", loggedInUser.getUserLogin());
                    serviceContext.put("_uploadedFile_fileName", fileName);
                    serviceContext.put("uploadedFile", byteBuffer);
                    serviceContext.put("_uploadedFile_contentType", inputPart.getMediaType().toString());
                    Map<String, Object> fileUploadServiceResponse = null;
                    try {
                        fileUploadServiceResponse = dispatcher.runSync("extCreateContentFromUploadedFile", serviceContext);
                    } catch (Exception e) {
                        Debug.logError(e, module);
                        return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
                    }
                    if (ServiceUtil.isError(fileUploadServiceResponse)) {
                        return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR, ServiceUtil.getErrorMessage(fileUploadServiceResponse));
                    }
                    if(UtilValidate.isNotEmpty(fileUploadServiceResponse)){
                        String contentId = (String) fileUploadServiceResponse.get("contentId");
                        if(UtilValidate.isNotEmpty(contentId)) {
                            Map fsdEnrichContentCtx =
                                    UtilMisc.toMap("userLogin", loggedInUser.getUserLogin(), "contentId", contentId);
                            Map fsdEnrichContentCtxResponse = FastMap.newInstance();
                            try {
                                fsdEnrichContentCtxResponse =
                                        dispatcher.runSync("extEnrichContent", fsdEnrichContentCtx, 300, true);
                            } catch (Exception ex) {
                                Debug.logError(ex, module);
                            }
                        }
                    }

                    String cdnUrl = UtilProperties.getPropertyValue("url", "content.url.prefix.secure") + "/";

                    String mediaDirectory = EntityUtilProperties.getPropertyValue( "appconfig.properties", "image.upload.dir", delegator);
                    String uploadUrlPrefix = EntityUtilProperties.getPropertyValue("appconfig.properties", "image.upload.url.prefix", delegator);
                    if(UtilValidate.isEmpty(mediaDirectory)){
                        Debug.logWarning("mediaDirectory value is empty!, please ensure appconfig.properties has valid value", module);
                    }else{
                        Debug.logInfo("mediaDirectory value is : " + mediaDirectory, module);
                    }

                    String uploadedFileUrl = "";
                    if (UtilValidate.isNotEmpty(fileName)) {
                        //trim and remove any spaces from the file name
                        fileName = fileName.trim().replaceAll(" ", "");
                        //split and fetch the extension of the file name and the actual file name
                        String uploadedFileNameWithoutExtension = fileName.substring(0, fileName.lastIndexOf("."));
                        String uploadedFileNameExtension = fileName.substring(fileName.lastIndexOf("."), fileName.length());

                        // Add the timestamp to it as in most cases the image name doesn't change, to avoid overwriting the existing file
                        // contents, append current timestamp (in ms) to the file name for uniqueness of file name.
                        fileName = uploadedFileNameWithoutExtension + "-" + UtilDateTime.nowTimestamp().getTime() + uploadedFileNameExtension;

                        try {
                            String ofbizHome = System.getProperty("ofbiz.home");
                            String targetFileName = ofbizHome + "/" + mediaDirectory + "/" + fileName;
                            IOUtils.copy(new ByteBufferBackedInputStream(byteBuffer), new FileOutputStream(targetFileName));

                            uploadedFileUrl = targetFileName;
                        } catch (Exception e) {
                            e.printStackTrace();
                            if (Debug.verboseOn())
                                Debug.logVerbose("Unable to upload file, something went wrong, please check logs", module);
                            return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
                        }
                    }else{
                    Debug.logWarning("File name is empty", module);

                    }

                    //need to return successful response for client consumption

                    String cdnUrlForUploadedImage = cdnUrl + uploadUrlPrefix + "/" + fileName;
                    Debug.logInfo("Image Uploaded Successfullly: Available at : " + cdnUrlForUploadedImage, module);

                    uploadedFileResponseJSON.put("uploaded", 1);
                    uploadedFileResponseJSON.put("fileName", fileName);
                    uploadedFileResponseJSON.put("url", cdnUrlForUploadedImage);
                    String contentId = (String) fileUploadServiceResponse.get("contentId");
                    uploadedFileResponseJSON.put("contentId", contentId);

                    // fetching thumbNail Url
                    try {

                        Map getThumbNailUrlCtx = FastMap.newInstance();
                        getThumbNailUrlCtx.put("contentId", contentId);
                        Map getThumbNailUrlResult = dispatcher
                            .runSync("getThumbNailUrl", getThumbNailUrlCtx);
                        String thumbNailUrl = (String) getThumbNailUrlResult.get("thumbNailUrl");
                        uploadedFileResponseJSON.put("thumbNailUrl", thumbNailUrl);

                        if (ServiceUtil.isError(getThumbNailUrlResult)) {
                            String serviceError = ServiceUtil
                                .getErrorMessage(getThumbNailUrlResult);
                            Debug.logError(
                                "An error occured while generating thumbnail url, details: "
                                    + "" + serviceError, module);
                            if (Debug.verboseOn()) {
                                Debug.logVerbose("Exiting method uploadImage", module);
                            }
                            return ApiResponseUtil
                                .prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR,
                                    serviceError);
                        }
                    } catch (Exception e) {
                        Debug.logError(e, module);
                        return ApiResponseUtil
                            .prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR,
                                e.getMessage());
                    }
                } catch (IOException e) {
                    Debug.logError("An error occurred during addAttachments call, details: " + e.getMessage(), module);
                    if (Debug.verboseOn())
                        Debug.logVerbose("Exiting method addAttachments", module);

                    return ApiResponseUtil.prepareDefaultResponse(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
                }

            }
        }
        return ApiResponseUtil.prepareOkResponse(uploadedFileResponseJSON);
    }

}

