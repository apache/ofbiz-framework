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
package org.ofbiz.content.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.tika.Tika;
import org.ofbiz.base.location.FlexibleLocation;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.FileUtil;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.StringUtil.StringWrapper;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilIO;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.collections.MapStack;
import org.ofbiz.base.util.template.FreeMarkerWorker;
import org.ofbiz.base.util.template.XslTransform;
import org.ofbiz.common.email.NotificationServices;
import org.ofbiz.content.content.UploadContentAndImage;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityQuery;
import org.ofbiz.entity.util.EntityUtilProperties;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.widget.model.ModelScreen;
import org.ofbiz.widget.model.ScreenFactory;
import org.ofbiz.widget.renderer.ScreenRenderer;
import org.ofbiz.widget.renderer.ScreenStringRenderer;
import org.ofbiz.widget.renderer.macro.MacroScreenRenderer;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * DataResourceWorker Class
 */
public class DataResourceWorker  implements org.ofbiz.widget.content.DataResourceWorkerInterface {

    public static final String module = DataResourceWorker.class.getName();
    public static final String err_resource = "ContentErrorUiLabels";

    /**
     * Traverses the DataCategory parent/child structure and put it in categoryNode. Returns non-null error string if there is an error.
     * @param depth The place on the categoryTypesIds to start collecting.
     * @param getAll Indicates that all descendants are to be gotten. Used as "true" to populate an
     *     indented select list.
     */
    public static String getDataCategoryMap(Delegator delegator, int depth, Map<String, Object> categoryNode, List<String> categoryTypeIds, boolean getAll) throws GenericEntityException {
        String errorMsg = null;
        String parentCategoryId = (String) categoryNode.get("id");
        String currentDataCategoryId = null;
        int sz = categoryTypeIds.size();

        // The categoryTypeIds has the most senior types at the end, so it is necessary to
        // work backwards. As "depth" is incremented, that is the effect.
        // The convention for the topmost type is "ROOT".
        if (depth >= 0 && (sz - depth) > 0) {
            currentDataCategoryId = categoryTypeIds.get(sz - depth - 1);
        }

        // Find all the categoryTypes that are children of the categoryNode.
        List<GenericValue> categoryValues = EntityQuery.use(delegator).from("DataCategory")
                .where("parentCategoryId", parentCategoryId)
                .cache().queryList();
        categoryNode.put("count", Integer.valueOf(categoryValues.size()));
        List<Map<String, Object>> subCategoryIds = new LinkedList<Map<String,Object>>();
        for (GenericValue category : categoryValues) {
            String id = (String) category.get("dataCategoryId");
            String categoryName = (String) category.get("categoryName");
            Map<String, Object> newNode = new HashMap<String, Object>();
            newNode.put("id", id);
            newNode.put("name", categoryName);
            errorMsg = getDataCategoryMap(delegator, depth + 1, newNode, categoryTypeIds, getAll);
            if (errorMsg != null)
                break;
            subCategoryIds.add(newNode);
        }

        // The first two parentCategoryId test just make sure that the first level of children
        // is gotten. This is a hack to make them available for display, but a more correct
        // approach should be formulated.
        // The "getAll" switch makes sure all descendants make it into the tree, if true.
        // The other test is to only get all the children if the "leaf" node where all the
        // children of the leaf are wanted for expansion.
        if (parentCategoryId == null
            || parentCategoryId.equals("ROOT")
            || (currentDataCategoryId != null && currentDataCategoryId.equals(parentCategoryId))
            || getAll) {
            categoryNode.put("kids", subCategoryIds);
        }
        return errorMsg;
    }

    /**
     * Finds the parents of DataCategory entity and puts them in a list, the start entity at the top.
     */
    public static void getDataCategoryAncestry(Delegator delegator, String dataCategoryId, List<String> categoryTypeIds) throws GenericEntityException {
        categoryTypeIds.add(dataCategoryId);
        GenericValue dataCategoryValue = EntityQuery.use(delegator).from("DataCategory").where("dataCategoryId", dataCategoryId).queryOne();
        if (dataCategoryValue == null)
            return;
        String parentCategoryId = (String) dataCategoryValue.get("parentCategoryId");
        if (parentCategoryId != null) {
            getDataCategoryAncestry(delegator, parentCategoryId, categoryTypeIds);
        }
    }

    /**
     * Takes a DataCategory structure and builds a list of maps, one value (id) is the dataCategoryId value and the other is an indented string suitable for
     * use in a drop-down pick list.
     */
    public static void buildList(Map<String, Object> nd, List<Map<String, Object>> lst, int depth) {
        String id = (String) nd.get("id");
        String nm = (String) nd.get("name");
        String spc = "";
        for (int i = 0; i < depth; i++)
            spc += "&nbsp;&nbsp;";
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("dataCategoryId", id);
        map.put("categoryName", spc + nm);
        if (id != null && !id.equals("ROOT") && !id.equals("")) {
            lst.add(map);
        }
        List<Map<String, Object>> kids = UtilGenerics.checkList(nd.get("kids"));
        for (Map<String, Object> kidNode : kids) {
            buildList(kidNode, lst, depth + 1);
        }
    }

    /**
     * Uploads image data from a form and stores it in ImageDataResource. Expects key data in a field identified by the "idField" value and the binary data
     * to be in a field id'd by uploadField.
     */
    // TODO: This method is not used and should be removed. amb
    public static String uploadAndStoreImage(HttpServletRequest request, String idField, String uploadField) {
        //Delegator delegator = (Delegator) request.getAttribute("delegator");

        //String idFieldValue = null;
        ServletFileUpload fu = new ServletFileUpload(new DiskFileItemFactory(10240, FileUtil.getFile("runtime/tmp")));
        List<FileItem> lst = null;
        Locale locale = UtilHttp.getLocale(request);

        try {
            lst = UtilGenerics.checkList(fu.parseRequest(request));
        } catch (FileUploadException e) {
            request.setAttribute("_ERROR_MESSAGE_", e.toString());
            return "error";
        }

        if (lst.size() == 0) {
            String errMsg = UtilProperties.getMessage(DataResourceWorker.err_resource, "dataResourceWorker.no_files_uploaded", locale);
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            Debug.logWarning("[DataEvents.uploadImage] No files uploaded", module);
            return "error";
        }

        // This code finds the idField and the upload FileItems
        FileItem fi = null;
        FileItem imageFi = null;
        String imageFileName = null;
        Map<String, Object> passedParams = new HashMap<String, Object>();
        HttpSession session = request.getSession();
        GenericValue userLogin = (GenericValue)session.getAttribute("userLogin");
        passedParams.put("userLogin", userLogin);
        byte[] imageBytes = null;
        for (int i = 0; i < lst.size(); i++) {
            fi = lst.get(i);
            //String fn = fi.getName();
            String fieldName = fi.getFieldName();
            if (fi.isFormField()) {
                String fieldStr = fi.getString();
                passedParams.put(fieldName, fieldStr);
            } else if (fieldName.startsWith("imageData")) {
                imageFi = fi;
                imageBytes = imageFi.get();
                passedParams.put(fieldName, imageBytes);
                imageFileName = imageFi.getName();
                passedParams.put("drObjectInfo", imageFileName);
                if (Debug.infoOn()) Debug.logInfo("[UploadContentAndImage]imageData: " + imageBytes.length, module);
            }
        }

        if (imageBytes != null && imageBytes.length > 0) {
            String mimeType = getMimeTypeFromImageFileName(imageFileName);
            if (UtilValidate.isNotEmpty(mimeType)) {
                passedParams.put("drMimeTypeId", mimeType);
                try {
                    String returnMsg = UploadContentAndImage.processContentUpload(passedParams, "", request);
                    if (returnMsg.equals("error")) {
                        return "error";
                    }
                } catch (GenericServiceException e) {
                    request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
                    return "error";
                }
            } else {
                request.setAttribute("_ERROR_MESSAGE_", "mimeType is empty.");
                return "error";
            }
        }
        return "success";
    }

    public static String getMimeTypeFromImageFileName(String imageFileName) {
        String mimeType = null;
        if (UtilValidate.isEmpty(imageFileName))
           return mimeType;

        int pos = imageFileName.lastIndexOf(".");
        if (pos < 0)
           return mimeType;

        String suffix = imageFileName.substring(pos + 1);
        String suffixLC = suffix.toLowerCase();
        if (suffixLC.equals("jpg"))
            mimeType = "image/jpeg";
        else
            mimeType = "image/" + suffixLC;

        return mimeType;
    }

    /**
     * callDataResourcePermissionCheck Formats data for a call to the checkContentPermission service.
     */
    public static String callDataResourcePermissionCheck(Delegator delegator, LocalDispatcher dispatcher, Map<String, Object> context) {
        Map<String, Object> permResults = callDataResourcePermissionCheckResult(delegator, dispatcher, context);
        String permissionStatus = (String) permResults.get("permissionStatus");
        return permissionStatus;
    }

    /**
     * callDataResourcePermissionCheck Formats data for a call to the checkContentPermission service.
     */
    public static Map<String, Object> callDataResourcePermissionCheckResult(Delegator delegator, LocalDispatcher dispatcher, Map<String, Object> context) {

        Map<String, Object> permResults = new HashMap<String, Object>();
        String skipPermissionCheck = (String) context.get("skipPermissionCheck");
            if (Debug.infoOn()) Debug.logInfo("in callDataResourcePermissionCheckResult, skipPermissionCheck:" + skipPermissionCheck,"");

        if (UtilValidate.isEmpty(skipPermissionCheck) 
                || (!"true".equalsIgnoreCase(skipPermissionCheck) && !"granted".equalsIgnoreCase(skipPermissionCheck))) {
            GenericValue userLogin = (GenericValue) context.get("userLogin");
            Map<String, Object> serviceInMap = new HashMap<String, Object>();
            serviceInMap.put("userLogin", userLogin);
            serviceInMap.put("targetOperationList", context.get("targetOperationList"));
            serviceInMap.put("contentPurposeList", context.get("contentPurposeList"));
            serviceInMap.put("entityOperation", context.get("entityOperation"));

            // It is possible that permission to work with DataResources will be controlled
            // by an external Content entity.
            String ownerContentId = (String) context.get("ownerContentId");
            if (UtilValidate.isNotEmpty(ownerContentId)) {
                try {
                    GenericValue content = EntityQuery.use(delegator).from("Content").where("contentId", ownerContentId).queryOne();
                    if (content != null)
                        serviceInMap.put("currentContent", content);
                } catch (GenericEntityException e) {
                    Debug.logError(e, "e.getMessage()", "ContentServices");
                }
            }
            try {
                permResults = dispatcher.runSync("checkContentPermission", serviceInMap);
            } catch (GenericServiceException e) {
                Debug.logError(e, "Problem checking permissions", "ContentServices");
            }
        } else {
            permResults.put("permissionStatus", "granted");
        }
        return permResults;
    }

    /**
     * Gets image data from ImageDataResource and returns it as a byte array.
     */
    public static byte[] acquireImage(Delegator delegator, String dataResourceId) throws GenericEntityException {

        byte[] b = null;
        GenericValue dataResource = EntityQuery.use(delegator).from("DataResource").where("dataResourceId", dataResourceId).cache().queryOne();
        if (dataResource == null)
            return b;

        b = acquireImage(delegator, dataResource);
        return b;
    }

    public static byte[] acquireImage(Delegator delegator, GenericValue dataResource) throws  GenericEntityException {
        byte[] b = null;
        String dataResourceId = dataResource.getString("dataResourceId");
        GenericValue imageDataResource = EntityQuery.use(delegator).from("ImageDataResource").where("dataResourceId", dataResourceId).queryOne();
        if (imageDataResource != null) {
            //b = (byte[]) imageDataResource.get("imageData");
            b = imageDataResource.getBytes("imageData");
        }
        return b;
    }

    public static String getMimeType(GenericValue dataResource) {
        String mimeTypeId = null;
        if (dataResource != null) {
            mimeTypeId = (String) dataResource.get("mimeTypeId");
            if (UtilValidate.isEmpty(mimeTypeId)) {
                String fileName = (String) dataResource.get("objectInfo");
                if (fileName != null && fileName.indexOf('.') > -1) {
                    String fileExtension = fileName.substring(fileName.lastIndexOf('.') + 1);
                    if (UtilValidate.isNotEmpty(fileExtension)) {
                        GenericValue ext = null;
                        try {
                            ext = dataResource.getDelegator().findOne("FileExtension",
                                    UtilMisc.toMap("fileExtensionId", fileExtension), false);
                        } catch (GenericEntityException e) {
                            Debug.logError(e, module);
                        }
                        if (ext != null) {
                            mimeTypeId = ext.getString("mimeTypeId");
                        }
                    }
                }

                // check one last time
                if (UtilValidate.isEmpty(mimeTypeId)) {
                    // use a default mime type
                    mimeTypeId = "application/octet-stream";
                }
            }
        }
        return mimeTypeId;
    }

    public static String getMimeTypeWithByteBuffer(java.nio.ByteBuffer buffer) throws IOException {
        byte[] b = buffer.array();

        Tika tika = new Tika();
        return tika.detect(b);
    }

    public static String buildRequestPrefix(Delegator delegator, Locale locale, String webSiteId, String https) {
        Map<String, Object> prefixValues = new HashMap<String, Object>();
        String prefix;

        NotificationServices.setBaseUrl(delegator, webSiteId, prefixValues);
        if (https != null && https.equalsIgnoreCase("true")) {
            prefix = (String) prefixValues.get("baseSecureUrl");
        } else {
            prefix = (String) prefixValues.get("baseUrl");
        }
        if (UtilValidate.isEmpty(prefix)) {
            if (https != null && https.equalsIgnoreCase("true")) {
                prefix = UtilProperties.getMessage("content", "baseSecureUrl", locale);
            } else {
                prefix = UtilProperties.getMessage("content", "baseUrl", locale);
            }
        }

        return prefix;
    }

    public static File getContentFile(String dataResourceTypeId, String objectInfo, String contextRoot)  throws GeneralException, FileNotFoundException{
        File file = null;

        if (dataResourceTypeId.equals("LOCAL_FILE") || dataResourceTypeId.equals("LOCAL_FILE_BIN")) {
            file = FileUtil.getFile(objectInfo);
            if (!file.exists()) {
                throw new FileNotFoundException("No file found: " + (objectInfo));
            }
            if (!file.isAbsolute()) {
                throw new GeneralException("File (" + objectInfo + ") is not absolute");
            }
        } else if (dataResourceTypeId.equals("OFBIZ_FILE") || dataResourceTypeId.equals("OFBIZ_FILE_BIN")) {
            String prefix = System.getProperty("ofbiz.home");

            String sep = "";
            if (objectInfo.indexOf("/") != 0 && prefix.lastIndexOf("/") != (prefix.length() - 1)) {
                sep = "/";
            }
            file = FileUtil.getFile(prefix + sep + objectInfo);
            if (!file.exists()) {
                throw new FileNotFoundException("No file found: " + (prefix + sep + objectInfo));
            }
        } else if (dataResourceTypeId.equals("CONTEXT_FILE") || dataResourceTypeId.equals("CONTEXT_FILE_BIN")) {
            if (UtilValidate.isEmpty(contextRoot)) {
                throw new GeneralException("Cannot find CONTEXT_FILE with an empty context root!");
            }

            String sep = "";
            if (objectInfo.indexOf("/") != 0 && contextRoot.lastIndexOf("/") != (contextRoot.length() - 1)) {
                sep = "/";
            }
            file = FileUtil.getFile(contextRoot + sep + objectInfo);
            if (!file.exists()) {
                throw new FileNotFoundException("No file found: " + (contextRoot + sep + objectInfo));
            }
        }

        return file;
    }


    public static String getDataResourceMimeType(Delegator delegator, String dataResourceId, GenericValue view) throws GenericEntityException {

        String mimeType = null;
        if (view != null)
            mimeType = view.getString("drMimeTypeId");
            //if (Debug.infoOn()) Debug.logInfo("getDataResourceMimeType, mimeType(2):" + mimeType, "");
        if (UtilValidate.isEmpty(mimeType) && UtilValidate.isNotEmpty(dataResourceId)) {
                GenericValue dataResource = EntityQuery.use(delegator).from("DataResource").where("dataResourceId", dataResourceId).cache().queryOne();
                //if (Debug.infoOn()) Debug.logInfo("getDataResourceMimeType, dataResource(2):" + dataResource, "");
                mimeType = dataResource.getString("mimeTypeId");

        }
        return mimeType;
    }

    public static String getDataResourceContentUploadPath() {
        return getDataResourceContentUploadPath(true);
    }

    public static String getDataResourceContentUploadPath(boolean absolute) {
        String initialPath = UtilProperties.getPropertyValue("content.properties", "content.upload.path.prefix");
        double maxFiles = UtilProperties.getPropertyNumber("content.properties", "content.upload.max.files");
        if (maxFiles < 1) {
            maxFiles = 250;
        }

        return getDataResourceContentUploadPath(initialPath, maxFiles, absolute);
    }

    public static String getDataResourceContentUploadPath(Delegator delegator, boolean absolute) {
        String initialPath = EntityUtilProperties.getPropertyValue("content", "content.upload.path.prefix", delegator);
        double maxFiles = UtilProperties.getPropertyNumber("content.properties", "content.upload.max.files");
        if (maxFiles < 1) {
            maxFiles = 250;
        }

        return getDataResourceContentUploadPath(initialPath, maxFiles, absolute);
    }

    public static String getDataResourceContentUploadPath(String initialPath, double maxFiles) {
        return getDataResourceContentUploadPath(initialPath, maxFiles, true);
    }

    /**
     * Handles creating sub-directories for file storage; using a max number of files per directory
     * @param initialPath the top level location where all files should be stored
     * @param maxFiles the max number of files to place in a directory
     * @return the absolute path to the directory where the file should be placed
     */
    public static String getDataResourceContentUploadPath(String initialPath, double maxFiles, boolean absolute) {
        String ofbizHome = System.getProperty("ofbiz.home");

        if (!initialPath.startsWith("/")) {
            initialPath = "/" + initialPath;
        }

        // descending comparator
        Comparator<Object> desc = new Comparator<Object>() {
            public int compare(Object o1, Object o2) {
                if (((Long) o1).longValue() > ((Long) o2).longValue()) {
                    return -1;
                } else if (((Long) o1).longValue() < ((Long) o2).longValue()) {
                    return 1;
                }
                return 0;
            }
        };

        // check for the latest subdirectory
        String parentDir = ofbizHome + initialPath;
        File parent = FileUtil.getFile(parentDir);
        TreeMap<Long, File> dirMap = new TreeMap<Long, File>(desc);
        if (parent.exists()) {
            File[] subs = parent.listFiles();
            for (int i = 0; i < subs.length; i++) {
                if (subs[i].isDirectory()) {
                    dirMap.put(Long.valueOf(subs[i].lastModified()), subs[i]);
                }
            }
        } else {
            // if the parent doesn't exist; create it now
            boolean created = parent.mkdir();
            if (!created) {
                Debug.logWarning("Unable to create top level upload directory [" + parentDir + "].", module);
            }
        }

        // first item in map is the most current directory
        File latestDir = null;
        if (UtilValidate.isNotEmpty(dirMap)) {
            latestDir = dirMap.values().iterator().next();
            if (latestDir != null) {
                File[] dirList = latestDir.listFiles();
                if (dirList.length >= maxFiles) {
                    latestDir = makeNewDirectory(parent);
                }
            }
        } else {
            latestDir = makeNewDirectory(parent);
        }

        Debug.logInfo("Directory Name : " + latestDir.getName(), module);
        if (absolute) {
            return latestDir.getAbsolutePath().replace('\\','/');
        } else {
            return initialPath + "/" + latestDir.getName();

        }
    }

    private static File makeNewDirectory(File parent) {
        File latestDir = null;
        boolean newDir = false;
        while (!newDir) {
            latestDir = new File(parent, "" + System.currentTimeMillis());
            if (!latestDir.exists()) {
                latestDir.mkdir();
                newDir = true;
            }
        }
        return latestDir;
    }

    // -------------------------------------
    // DataResource rendering methods
    // -------------------------------------

    public static void clearAssociatedRenderCache(Delegator delegator, String dataResourceId) throws GeneralException {
        if (dataResourceId == null) {
            throw new GeneralException("Cannot clear dataResource related cache for a null dataResourceId");
        }

        GenericValue dataResource = EntityQuery.use(delegator).from("DataResource").where("dataResourceId", dataResourceId).cache().queryOne();
        if (dataResource != null) {
            String dataTemplateTypeId = dataResource.getString("dataTemplateTypeId");
            if ("FTL".equals(dataTemplateTypeId)) {
                FreeMarkerWorker.clearTemplateFromCache(delegator.getDelegatorName() + ":DataResource:" + dataResourceId);
            }
        }
    }

    public static String renderDataResourceAsText(Delegator delegator, String dataResourceId, Map<String, Object> templateContext,
             Locale locale, String targetMimeTypeId, boolean cache) throws GeneralException, IOException {
        Writer writer = new StringWriter();
        renderDataResourceAsText(delegator, dataResourceId, writer, templateContext, locale, targetMimeTypeId, cache, null);
        return writer.toString();
    }

    public static String renderDataResourceAsText(Delegator delegator, String dataResourceId, Appendable out,
            Map<String, Object> templateContext, Locale locale, String targetMimeTypeId, boolean cache) throws GeneralException, IOException {
       renderDataResourceAsText(delegator, dataResourceId, out, templateContext, locale, targetMimeTypeId, cache, null);
       return out.toString();
   }

    public static void renderDataResourceAsText(Delegator delegator, String dataResourceId, Appendable out,
            Map<String, Object> templateContext, Locale locale, String targetMimeTypeId, boolean cache, List<GenericValue> webAnalytics) throws GeneralException, IOException {
        if (dataResourceId == null) {
            throw new GeneralException("Cannot lookup data resource with for a null dataResourceId");
        }
        if (templateContext == null) {
            templateContext = new HashMap<String, Object>();
        }
        if (UtilValidate.isEmpty(targetMimeTypeId)) {
            targetMimeTypeId = "text/html";
        }
        if (locale == null) {
            locale = Locale.getDefault();
        }

        // if the target mimeTypeId is not a text type, throw an exception
        if (!targetMimeTypeId.startsWith("text/")) {
            throw new GeneralException("The desired mime-type is not a text type, cannot render as text: " + targetMimeTypeId);
        }

        // get the data resource object
        GenericValue dataResource = EntityQuery.use(delegator).from("DataResource")
                .where("dataResourceId", dataResourceId)
                .cache(cache).queryOne();

        if (dataResource == null) {
            throw new GeneralException("No data resource object found for dataResourceId: [" + dataResourceId + "]");
        }

        // a data template attached to the data resource
        String dataTemplateTypeId = dataResource.getString("dataTemplateTypeId");

        // no template; or template is NONE; render the data
        if (UtilValidate.isEmpty(dataTemplateTypeId) || "NONE".equals(dataTemplateTypeId)) {
            DataResourceWorker.writeDataResourceText(dataResource, targetMimeTypeId, locale, templateContext, delegator, out, cache);
        } else {
            // a template is defined; render the template first
            templateContext.put("mimeTypeId", targetMimeTypeId);

            // FTL template
            if ("FTL".equals(dataTemplateTypeId)) {
                try {
                    // get the template data for rendering
                    String templateText = getDataResourceText(dataResource, targetMimeTypeId, locale, templateContext, delegator, cache);

                    // if use web analytics.
                    if (UtilValidate.isNotEmpty(webAnalytics)) {
                        StringBuffer newTemplateText = new StringBuffer(templateText);
                        String webAnalyticsCode = "<script language=\"JavaScript\" type=\"text/javascript\">";
                        for (GenericValue webAnalytic : webAnalytics) {
                            StringWrapper wrapString = StringUtil.wrapString((String) webAnalytic.get("webAnalyticsCode"));
                            webAnalyticsCode += wrapString.toString();
                        }
                        webAnalyticsCode += "</script>";
                        newTemplateText.insert(templateText.lastIndexOf("</head>"), webAnalyticsCode);
                        templateText = newTemplateText.toString();
                    }

                    // render the FTL template
                    boolean useTemplateCache = cache && !UtilProperties.getPropertyAsBoolean("content", "disable.ftl.template.cache", false);
                    FreeMarkerWorker.renderTemplate(delegator.getDelegatorName() + ":DataResource:" + dataResourceId, templateText, templateContext, out, useTemplateCache);
                } catch (TemplateException e) {
                    throw new GeneralException("Error rendering FTL template", e);
                }

            } else if ("XSLT".equals(dataTemplateTypeId)) {
                File sourceFileLocation = null;
                File targetFileLocation = new File(System.getProperty("ofbiz.home")+"/runtime/tempfiles/docbook.css");
                if (templateContext.get("visualThemeId") != null) {
                    Map<String, Object> layoutSettings  = UtilGenerics.checkMap(templateContext.get("layoutSettings"));
                    List<String> docbookStyleSheets = UtilGenerics.checkList(layoutSettings.get("VT_DOCBOOKSTYLESHEET"));
                    String docbookStyleLocation = docbookStyleSheets.get(0);
                    sourceFileLocation = new File(System.getProperty("ofbiz.home")+"/themes"+docbookStyleLocation);
                }
                if (sourceFileLocation != null && sourceFileLocation.exists()) {
                    UtilMisc.copyFile(sourceFileLocation,targetFileLocation);
                } else {
                    String defaultVisualThemeId = EntityUtilProperties.getPropertyValue("general", "VISUAL_THEME", delegator);
                    if (defaultVisualThemeId != null) {
                        GenericValue themeValue = EntityQuery.use(delegator).from("VisualThemeResource").where("visualThemeId", defaultVisualThemeId, "resourceTypeEnumId", "VT_DOCBOOKSTYLESHEET", "sequenceId", "01").cache().queryOne();
                        sourceFileLocation = new File(System.getProperty("ofbiz.home") + "/themes" + themeValue.get("resourceValue"));
                        UtilMisc.copyFile(sourceFileLocation,targetFileLocation);
                    }
                }
                // get the template data for rendering
                String templateLocation = DataResourceWorker.getContentFile(dataResource.getString("dataResourceTypeId"), dataResource.getString("objectInfo"), (String) templateContext.get("contextRoot")).toString();
                // render the XSLT template and file
                String outDoc = null;
                try {
                    outDoc = XslTransform.renderTemplate(templateLocation, (String) templateContext.get("docFile"));
                } catch (TransformerException c) {
                    Debug.logError("XSL TransformerException: " + c.getMessage(), module);
                }
                out.append(outDoc);

            // Screen Widget template
            } else if ("SCREEN_COMBINED".equals(dataTemplateTypeId)) {
                try {
                    MapStack<String> context = MapStack.create(templateContext);
                    context.put("locale", locale);
                    // prepare the map for preRenderedContent
                    String textData = (String) context.get("textData");
                    if (UtilValidate.isNotEmpty(textData)) {
                        Map<String, Object> prc = new HashMap<String, Object>();
                        String mapKey = (String) context.get("mapKey");
                        if (mapKey != null) {
                            prc.put(mapKey, mapKey);
                        }
                        prc.put("body", textData); // used for default screen defs
                        context.put("preRenderedContent", prc);
                    }
                    // get the screen renderer; or create a new one
                    ScreenRenderer screens = (ScreenRenderer) context.get("screens");
                    if (screens == null) {
                     // TODO: replace "screen" to support dynamic rendering of different output
                        ScreenStringRenderer screenStringRenderer = new MacroScreenRenderer(EntityUtilProperties.getPropertyValue("widget", "screen.name", delegator), EntityUtilProperties.getPropertyValue("widget", "screen.screenrenderer", delegator));
                        screens = new ScreenRenderer(out, context, screenStringRenderer);
                        screens.getContext().put("screens", screens);
                    }
                    // render the screen
                    ModelScreen modelScreen = null;
                    ScreenStringRenderer renderer = screens.getScreenStringRenderer();
                    String combinedName = dataResource.getString("objectInfo");
                    if ("URL_RESOURCE".equals(dataResource.getString("dataResourceTypeId")) && UtilValidate.isNotEmpty(combinedName) && combinedName.startsWith("component://")) {
                        modelScreen = ScreenFactory.getScreenFromLocation(combinedName);
                    } else { // stored in  a single file, long or short text
                        Document screenXml = UtilXml.readXmlDocument(getDataResourceText(dataResource, targetMimeTypeId, locale, templateContext, delegator, cache), true, true);
                        Map<String, ModelScreen> modelScreenMap = ScreenFactory.readScreenDocument(screenXml, "DataResourceId: " + dataResource.getString("dataResourceId"));
                        if (UtilValidate.isNotEmpty(modelScreenMap)) {
                            Map.Entry<String, ModelScreen> entry = modelScreenMap.entrySet().iterator().next(); // get first entry, only one screen allowed per file
                            modelScreen = entry.getValue();
                        }
                    }
                    if (UtilValidate.isNotEmpty(modelScreen)) {
                        modelScreen.renderScreenString(out, context, renderer);
                    } else {
                        throw new GeneralException("The dataResource file [" + dataResourceId + "] could not be found");
                    }
                } catch (SAXException e) {
                    throw new GeneralException("Error rendering Screen template", e);
                } catch (ParserConfigurationException e) {
                    throw new GeneralException("Error rendering Screen template", e);
                } catch (TemplateException e) {
                    throw new GeneralException("Error creating Screen renderer", e);
                }
            } else {
                throw new GeneralException("The dataTemplateTypeId [" + dataTemplateTypeId + "] is not yet supported");
            }
        }
    }

    // ----------------------------
    // Data Resource Data Gathering
    // ----------------------------

    public static String getDataResourceText(GenericValue dataResource, String mimeTypeId, Locale locale, Map<String, Object> context,
            Delegator delegator, boolean cache) throws IOException, GeneralException {
        Writer out = new StringWriter();
        writeDataResourceText(dataResource, mimeTypeId, locale, context, delegator, out, cache);
        return out.toString();
    }

    public static void writeDataResourceText(GenericValue dataResource, String mimeTypeId, Locale locale, Map<String, Object> templateContext,
            Delegator delegator, Appendable out, boolean cache) throws IOException, GeneralException {
        Map<String, Object> context = UtilGenerics.checkMap(templateContext.get("context"));
        if (context == null) {
            context = new HashMap<String, Object>();
        }
        String webSiteId = (String) templateContext.get("webSiteId");
        if (UtilValidate.isEmpty(webSiteId)) {
            if (context != null)
                webSiteId = (String) context.get("webSiteId");
        }

        String https = (String) templateContext.get("https");
        if (UtilValidate.isEmpty(https)) {
            if (context != null)
                https = (String) context.get("https");
        }

        String rootDir = (String) templateContext.get("rootDir");
        if (UtilValidate.isEmpty(rootDir)) {
            if (context != null)
                rootDir = (String) context.get("rootDir");
        }

        String dataResourceId = dataResource.getString("dataResourceId");
        String dataResourceTypeId = dataResource.getString("dataResourceTypeId");

        // default type
        if (UtilValidate.isEmpty(dataResourceTypeId)) {
            dataResourceTypeId = "SHORT_TEXT";
        }

        // text types
        if ("SHORT_TEXT".equals(dataResourceTypeId) || "LINK".equals(dataResourceTypeId)) {
            String text = dataResource.getString("objectInfo");
            writeText(dataResource, text, templateContext, mimeTypeId, locale, out);
        } else if ("ELECTRONIC_TEXT".equals(dataResourceTypeId)) {
            GenericValue electronicText = EntityQuery.use(delegator).from("ElectronicText")
                    .where("dataResourceId", dataResourceId)
                    .cache(cache).queryOne();
            if (electronicText != null) {
                String text = electronicText.getString("textData");
                writeText(dataResource, text, templateContext, mimeTypeId, locale, out);
            }

        // object types
        } else if (dataResourceTypeId.endsWith("_OBJECT")) {
            String text = (String) dataResource.get("dataResourceId");
            writeText(dataResource, text, templateContext, mimeTypeId, locale, out);

        // resource type
        } else if (dataResourceTypeId.equals("URL_RESOURCE")) {
            String text = null;
            URL url = FlexibleLocation.resolveLocation(dataResource.getString("objectInfo"));

            if (url.getHost() != null) { // is absolute
                InputStream in = url.openStream();
                int c;
                StringWriter sw = new StringWriter();
                while ((c = in.read()) != -1) {
                    sw.write(c);
                }
                sw.close();
                text = sw.toString();
            } else {
                String prefix = DataResourceWorker.buildRequestPrefix(delegator, locale, webSiteId, https);
                String sep = "";
                if (url.toString().indexOf("/") != 0 && prefix.lastIndexOf("/") != (prefix.length() - 1)) {
                    sep = "/";
                }
                String fixedUrlStr = prefix + sep + url.toString();
                URL fixedUrl = new URL(fixedUrlStr);
                text = (String) fixedUrl.getContent();
            }
            out.append(text);

        // file types
        } else if (dataResourceTypeId.endsWith("_FILE_BIN")) {
            writeText(dataResource, dataResourceId, templateContext, mimeTypeId, locale, out);
        } else if (dataResourceTypeId.endsWith("_FILE")) {
            String dataResourceMimeTypeId = dataResource.getString("mimeTypeId");
            String objectInfo = dataResource.getString("objectInfo");

            if (dataResourceMimeTypeId == null || dataResourceMimeTypeId.startsWith("text")) {
                renderFile(dataResourceTypeId, objectInfo, rootDir, out);
            } else {
                writeText(dataResource, dataResourceId, templateContext, mimeTypeId, locale, out);
            }
        } else {
            throw new GeneralException("The dataResourceTypeId [" + dataResourceTypeId + "] is not supported in renderDataResourceAsText");
        }
    }

    public static void writeText(GenericValue dataResource, String textData, Map<String, Object> context, String targetMimeTypeId, Locale locale, Appendable out) throws GeneralException, IOException {
        String dataResourceMimeTypeId = dataResource.getString("mimeTypeId");
        Delegator delegator = dataResource.getDelegator();

        // assume HTML as data resource data
        if (UtilValidate.isEmpty(dataResourceMimeTypeId)) {
            dataResourceMimeTypeId = "text/html";
        }

        // assume HTML for target
        if (UtilValidate.isEmpty(targetMimeTypeId)) {
            targetMimeTypeId = "text/html";
        }

        // we can only render text
        if (!targetMimeTypeId.startsWith("text")) {
            throw new GeneralException("Method writeText() only supports rendering text content : " + targetMimeTypeId + " is not supported");
        }

        if ("text/html".equals(targetMimeTypeId)) {
            // get the default mime type template
            GenericValue mimeTypeTemplate = EntityQuery.use(delegator).from("MimeTypeHtmlTemplate").where("mimeTypeId", dataResourceMimeTypeId).cache().queryOne();

            if (mimeTypeTemplate != null && mimeTypeTemplate.get("templateLocation") != null) {
                // prepare the context
                Map<String, Object> mimeContext = new HashMap<String, Object>();
                mimeContext.putAll(context);
                mimeContext.put("dataResource", dataResource);
                mimeContext.put("textData", textData);

                String mimeString = DataResourceWorker.renderMimeTypeTemplate(mimeTypeTemplate, mimeContext);
                if (mimeString != null) {
                    out.append(mimeString);
                }
            } else {
                if (textData != null) {
                    out.append(textData);
                }
            }
        } else {
            out.append(textData);
        }
    }

    public static String renderMimeTypeTemplate(GenericValue mimeTypeTemplate, Map<String, Object> context) throws GeneralException, IOException {
        String location = mimeTypeTemplate.getString("templateLocation");
        StringWriter writer = new StringWriter();
        try {
            FreeMarkerWorker.renderTemplateAtLocation(location, context, writer);
        } catch (TemplateException e) {
            throw new GeneralException(e.getMessage(), e);
        }

        return writer.toString();
    }

    public static void renderFile(String dataResourceTypeId, String objectInfo, String rootDir, Appendable out) throws GeneralException, IOException {
        // TODO: this method assumes the file is a text file, if it is an image we should respond differently, see the comment above for IMAGE_OBJECT type data resource

        if (dataResourceTypeId.equals("LOCAL_FILE") && UtilValidate.isNotEmpty(objectInfo)) {
            File file = FileUtil.getFile(objectInfo);
            if (!file.isAbsolute()) {
                throw new GeneralException("File (" + objectInfo + ") is not absolute");
            }
            FileReader in = new FileReader(file);
            UtilIO.copy(in, true, out);
        } else if (dataResourceTypeId.equals("OFBIZ_FILE") && UtilValidate.isNotEmpty(objectInfo)) {
            String prefix = System.getProperty("ofbiz.home");
            String sep = "";
            if (objectInfo.indexOf("/") != 0 && prefix.lastIndexOf("/") != (prefix.length() - 1)) {
                sep = "/";
            }
            File file = FileUtil.getFile(prefix + sep + objectInfo);
            FileReader in = new FileReader(file);
            UtilIO.copy(in, true, out);
        } else if (dataResourceTypeId.equals("CONTEXT_FILE") && UtilValidate.isNotEmpty(objectInfo)) {
            String prefix = rootDir;
            String sep = "";
            if (objectInfo.indexOf("/") != 0 && prefix.lastIndexOf("/") != (prefix.length() - 1)) {
                sep = "/";
            }
            File file = FileUtil.getFile(prefix + sep + objectInfo);
            FileReader in = null;
            try {
                in = new FileReader(file);
                String enc = in.getEncoding();
                if (Debug.infoOn()) Debug.logInfo("in serveImage, encoding:" + enc, module);

            } catch (FileNotFoundException e) {
                Debug.logError(e, " in renderDataResourceAsHtml(CONTEXT_FILE), in FNFexception:", module);
                throw new GeneralException("Could not find context file to render", e);
            } catch (Exception e) {
                Debug.logError(" in renderDataResourceAsHtml(CONTEXT_FILE), got exception:" + e.getMessage(), module);
            }
            UtilIO.copy(in, true, out);
            //out.flush();
        }
    }

    // ----------------------------
    // Data Resource Streaming
    // ----------------------------

    /**
     * getDataResourceStream - gets an InputStream and Content-Length of a DataResource
     *
     * @param dataResource
     * @param https
     * @param webSiteId
     * @param locale
     * @param contextRoot
     * @return Map containing 'stream': the InputStream and 'length' a Long containing the content-length
     * @throws IOException
     * @throws GeneralException
     */
    public static Map<String, Object> getDataResourceStream(GenericValue dataResource, String https, String webSiteId, Locale locale, String contextRoot, boolean cache) throws IOException, GeneralException {
        if (dataResource == null) {
            throw new GeneralException("Cannot stream null data resource!");
        }

        String dataResourceTypeId = dataResource.getString("dataResourceTypeId");
        String dataResourceId = dataResource.getString("dataResourceId");
        Delegator delegator = dataResource.getDelegator();

        // first text based data
        if (dataResourceTypeId.endsWith("_TEXT") || "LINK".equals(dataResourceTypeId)) {
            String text = "";

            if ("SHORT_TEXT".equals(dataResourceTypeId) || "LINK".equals(dataResourceTypeId)) {
                text = dataResource.getString("objectInfo");
            } else if ("ELECTRONIC_TEXT".equals(dataResourceTypeId)) {
                GenericValue electronicText = EntityQuery.use(delegator).from("ElectronicText")
                        .where("dataResourceId", dataResourceId)
                        .cache(cache).queryOne();
                if (electronicText != null) {
                    text = electronicText.getString("textData");
                }
            } else {
                throw new GeneralException("Unsupported TEXT type; cannot stream");
            }

            byte[] bytes = text.getBytes();
            return UtilMisc.toMap("stream", new ByteArrayInputStream(bytes), "length", Long.valueOf(bytes.length));

        // object (binary) data
        } else if (dataResourceTypeId.endsWith("_OBJECT")) {
            byte[] bytes = new byte[0];
            GenericValue valObj;

            if ("IMAGE_OBJECT".equals(dataResourceTypeId)) {
                valObj = EntityQuery.use(delegator).from("ImageDataResource").where("dataResourceId", dataResourceId).cache(cache).queryOne();
                if (valObj != null) {
                    bytes = valObj.getBytes("imageData");
                }
            } else if ("VIDEO_OBJECT".equals(dataResourceTypeId)) {
                valObj = EntityQuery.use(delegator).from("VideoDataResource").where("dataResourceId", dataResourceId).cache(cache).queryOne();
                if (valObj != null) {
                    bytes = valObj.getBytes("videoData");
                }
            } else if ("AUDIO_OBJECT".equals(dataResourceTypeId)) {
                valObj = EntityQuery.use(delegator).from("AudioDataResource").where("dataResourceId", dataResourceId).cache(cache).queryOne();
                if (valObj != null) {
                    bytes = valObj.getBytes("audioData");
                }
            } else if ("OTHER_OBJECT".equals(dataResourceTypeId)) {
                valObj = EntityQuery.use(delegator).from("OtherDataResource").where("dataResourceId", dataResourceId).cache(cache).queryOne();
                if (valObj != null) {
                    bytes = valObj.getBytes("dataResourceContent");
                }
            } else {
                throw new GeneralException("Unsupported OBJECT type [" + dataResourceTypeId + "]; cannot stream");
            }

            return UtilMisc.toMap("stream", new ByteArrayInputStream(bytes), "length", Long.valueOf(bytes.length));

        // file data
        } else if (dataResourceTypeId.endsWith("_FILE") || dataResourceTypeId.endsWith("_FILE_BIN")) {
            String objectInfo = dataResource.getString("objectInfo");
            if (UtilValidate.isNotEmpty(objectInfo)) {
                File file = DataResourceWorker.getContentFile(dataResourceTypeId, objectInfo, contextRoot);
                return UtilMisc.toMap("stream", new FileInputStream(file), "length", Long.valueOf(file.length()));
            } else {
                throw new GeneralException("No objectInfo found for FILE type [" + dataResourceTypeId + "]; cannot stream");
            }

        // URL resource data
        } else if ("URL_RESOURCE".equals(dataResourceTypeId)) {
            String objectInfo = dataResource.getString("objectInfo");
            if (UtilValidate.isNotEmpty(objectInfo)) {
                URL url = new URL(objectInfo);
                if (url.getHost() == null) { // is relative
                    String newUrl = DataResourceWorker.buildRequestPrefix(delegator, locale, webSiteId, https);
                    if (!newUrl.endsWith("/")) {
                        newUrl = newUrl + "/";
                    }
                    newUrl = newUrl + url.toString();
                    url = new URL(newUrl);
                }

                URLConnection con = url.openConnection();
                return UtilMisc.toMap("stream", con.getInputStream(), "length", Long.valueOf(con.getContentLength()));
            } else {
                throw new GeneralException("No objectInfo found for URL_RESOURCE type; cannot stream");
            }
        }

        // unsupported type
        throw new GeneralException("The dataResourceTypeId [" + dataResourceTypeId + "] is not supported in getDataResourceStream");
    }

    // TODO: remove this method in favor of getDataResourceStream
    public static void streamDataResource(OutputStream os, Delegator delegator, String dataResourceId, String https, String webSiteId, Locale locale, String rootDir) throws IOException, GeneralException {
        try {
            GenericValue dataResource = EntityQuery.use(delegator).from("DataResource").where("dataResourceId", dataResourceId).cache().queryOne();
            if (dataResource == null) {
                throw new GeneralException("Error in streamDataResource: DataResource with ID [" + dataResourceId + "] was not found.");
            }
            String dataResourceTypeId = dataResource.getString("dataResourceTypeId");
            if (UtilValidate.isEmpty(dataResourceTypeId)) {
                dataResourceTypeId = "SHORT_TEXT";
            }
            String mimeTypeId = dataResource.getString("mimeTypeId");
            if (UtilValidate.isEmpty(mimeTypeId)) {
                mimeTypeId = "text/html";
            }

            if (dataResourceTypeId.equals("SHORT_TEXT")) {
                String text = dataResource.getString("objectInfo");
                os.write(text.getBytes());
            } else if (dataResourceTypeId.equals("ELECTRONIC_TEXT")) {
                GenericValue electronicText = EntityQuery.use(delegator).from("ElectronicText").where("dataResourceId", dataResourceId).cache().queryOne();
                if (electronicText != null) {
                    String text = electronicText.getString("textData");
                    if (text != null) os.write(text.getBytes());
                }
            } else if (dataResourceTypeId.equals("IMAGE_OBJECT")) {
                byte[] imageBytes = acquireImage(delegator, dataResource);
                if (imageBytes != null) os.write(imageBytes);
            } else if (dataResourceTypeId.equals("LINK")) {
                String text = dataResource.getString("objectInfo");
                os.write(text.getBytes());
            } else if (dataResourceTypeId.equals("URL_RESOURCE")) {
                URL url = new URL(dataResource.getString("objectInfo"));
                if (url.getHost() == null) { // is relative
                    String prefix = buildRequestPrefix(delegator, locale, webSiteId, https);
                    String sep = "";
                    //String s = "";
                    if (url.toString().indexOf("/") != 0 && prefix.lastIndexOf("/") != (prefix.length() - 1)) {
                        sep = "/";
                    }
                    String s2 = prefix + sep + url.toString();
                    url = new URL(s2);
                }
                InputStream in = url.openStream();
                UtilIO.copy(in, true, os, false);
            } else if (dataResourceTypeId.indexOf("_FILE") >= 0) {
                String objectInfo = dataResource.getString("objectInfo");
                File inputFile = getContentFile(dataResourceTypeId, objectInfo, rootDir);
                //long fileSize = inputFile.length();
                FileInputStream fis = new FileInputStream(inputFile);
                UtilIO.copy(fis, true, os, false);
            } else {
                throw new GeneralException("The dataResourceTypeId [" + dataResourceTypeId + "] is not supported in streamDataResource");
            }
        } catch (GenericEntityException e) {
            throw new GeneralException("Error in streamDataResource", e);
        }
    }

    public static ByteBuffer getContentAsByteBuffer(Delegator delegator, String dataResourceId, String https, String webSiteId, Locale locale, String rootDir) throws IOException, GeneralException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        streamDataResource(baos, delegator, dataResourceId, https, webSiteId, locale, rootDir);
        ByteBuffer byteBuffer = ByteBuffer.wrap(baos.toByteArray());
        return byteBuffer;
    }

    public String renderDataResourceAsTextExt(Delegator delegator, String dataResourceId, Map<String, Object> templateContext,
            Locale locale, String targetMimeTypeId, boolean cache) throws GeneralException, IOException {
        return renderDataResourceAsText(delegator, dataResourceId, templateContext, locale, targetMimeTypeId, cache);
    }

    public void renderDataResourceAsTextExt(Delegator delegator, String dataResourceId, Appendable out, Map<String, Object> templateContext,
            Locale locale, String targetMimeTypeId, boolean cache) throws GeneralException, IOException {
        renderDataResourceAsText(delegator, dataResourceId, out, templateContext, locale, targetMimeTypeId, cache);
    }
}
