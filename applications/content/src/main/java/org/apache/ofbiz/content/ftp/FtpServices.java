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
package org.apache.ofbiz.content.ftp;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.FileUtil;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.content.data.DataResourceWorker;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;


/**
 * FtpServices class
 * This class provide Ftp transfer services for content
 */
public class FtpServices {

    private static final String MODULE = FtpServices.class.getName();
    private static final String RESOURCE = "ContentUiLabels";

    private static FtpClientInterface createFtpClient(String serverType)
            throws GeneralException {
        FtpClientInterface ftpClient = null;
        switch (serverType) {
        case "ftp":
            ftpClient = new SimpleFtpClient();
            break;
        case "ftps":
            //TODO : to implements
            throw new GeneralException("Ftp secured transfer protocol not yet implemented");
        case "sftp":
            ftpClient = new SshFtpClient();
            break;
        }
        return ftpClient;
    }

    public static Map<String, Object> sendContentToFtp(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        String contactMechId = (String) context.get("contactMechId");
        String contentId = (String) context.get("contentId");
        String communicationEventId = (String) context.get("communicationEventId");
        boolean forceTransferControlSuccess = EntityUtilProperties.propertyValueEqualsIgnoreCase("ftp",
                "ftp.force.transfer.control", "Y", delegator);
        boolean ftpNotificationEnabled = EntityUtilProperties.propertyValueEqualsIgnoreCase("ftp",
                "ftp.notifications.enabled", "Y", delegator);

        if (!ftpNotificationEnabled) return ServiceUtil.returnSuccess();

        // for ECA communicationEvent process
        Map<String, Object> resultMap = ServiceUtil.returnSuccess();
        resultMap.put("communicationEventId", communicationEventId);

        FtpClientInterface ftpClient = null;

        try {
            //Retrieve and check contactMechType
            GenericValue contactMech = EntityQuery.use(delegator).from("ContactMech").where("contactMechId", contactMechId).cache().queryOne();
            GenericValue ftpAddress = EntityQuery.use(delegator).from("FtpAddress").where("contactMechId", contactMechId).cache().queryOne();
            if (null == contactMech || null == ftpAddress || !"FTP_ADDRESS".equals(contactMech.getString("contactMechTypeId"))) {
                String errMsg = UtilProperties.getMessage("ContentErrorUiLabels", "ftpservices.contact_mech_must_be_ftp", locale);
                return ServiceUtil.returnError(errMsg + " " + contactMechId);
            }

            //Validate content
            GenericValue content = EntityQuery.use(delegator).from("Content").where("contentId", contentId).cache().queryOne();
            if (null == content) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ContentNoContentFound",
                        UtilMisc.toMap("contentId", contentId), locale));
            }

            //ftp redirection
            if ("Y".equalsIgnoreCase(UtilProperties.getPropertyValue("ftp", "ftp.notifications.redirectTo.enabled"))) {
                ftpAddress = delegator.makeValue("FtpAddress");
                ftpAddress.put("defaultTimeout", UtilProperties.getPropertyAsLong("ftp", "ftp.notifications.redirectTo.defaultTimeout", 30000));
                ftpAddress.put("hostname", UtilProperties.getPropertyValue("ftp", "ftp.notifications.redirectTo.hostname"));
                ftpAddress.put("filePath", UtilProperties.getPropertyValue("ftp", "ftp.notifications.redirectTo.filePath"));
                ftpAddress.put("port", UtilProperties.getPropertyAsLong("ftp", "ftp.notifications.redirectTo.port", 65535));
                ftpAddress.put("username", UtilProperties.getPropertyValue("ftp", "ftp.notifications.redirectTo.username"));
                ftpAddress.put("ftpPassword", UtilProperties.getPropertyValue("ftp", "ftp.notifications.redirectTo.ftpPassword"));
                ftpAddress.put("binaryTransfer", UtilProperties.getPropertyValue("ftp", "ftp.notifications.redirectTo.binaryTransfer"));
                ftpAddress.put("passiveMode", UtilProperties.getPropertyValue("ftp", "ftp.notifications.redirectTo.passiveMode"));
                ftpAddress.put("zipFile", UtilProperties.getPropertyValue("ftp", "ftp.notifications.redirectTo.zipFile"));
            }

            String hostname = ftpAddress.getString("hostname");
            if (UtilValidate.isEmpty(hostname)) {
                return ServiceUtil.returnError("Ftp destination server is null");
            } else if (hostname.indexOf("://") == -1) {
                return ServiceUtil.returnError("No protocol defined in ftp destination address");
            }

            String serverType = hostname.split("://")[0];
            hostname = hostname.split("://")[1];

            ftpClient = createFtpClient(serverType);
            if (null == ftpClient) {
                return ServiceUtil.returnError("Server type : " + serverType + ", not supported for hostname " + hostname);
            }

            Long defaultTimeout = ftpAddress.getLong("defaultTimeout");
            Long port = ftpAddress.getLong("port");
            String username = ftpAddress.getString("username");
            String password = ftpAddress.getString("ftpPassword");

            if (Debug.infoOn()) {
                Debug.logInfo("connecting to: " + username + "@" + ftpAddress.getString("hostname") + ":" + port, MODULE);
            }
            ftpClient.connect(hostname, username, password, port, defaultTimeout);
            boolean binary = "Y".equalsIgnoreCase(ftpAddress.getString("binaryTransfer"));
            ftpClient.setBinaryTransfer(binary);
            boolean passive = "Y".equalsIgnoreCase(ftpAddress.getString("passiveMode"));
            ftpClient.setPassiveMode(passive);

            GenericValue dataResource = delegator.findOne("DataResource", true, "dataResourceId", content.getString("dataResourceId"));
            Map<String, Object> resultStream = DataResourceWorker.getDataResourceStream(dataResource, null, null, locale, null, true);
            InputStream contentStream = (InputStream) resultStream.get("stream");
            if (contentStream == null) {
                return ServiceUtil.returnError("DataResource " + content.getString("dataResourceId") + " return an empty stream");
            }

            String path = ftpAddress.getString("filePath");
            if (Debug.infoOn()) {
                Debug.logInfo("storing local file remotely as: " + (UtilValidate.isNotEmpty(path) ? path + "/" : "")
                        + content.getString("contentName"), MODULE);
            }
            String fileName = content.getString("contentName");
            String remoteFileName = fileName;
            boolean zipFile = "Y".equalsIgnoreCase(ftpAddress.getString("zipFile"));
            if (zipFile) {
                //Create zip file from content input stream
                ByteArrayInputStream zipStream = FileUtil.zipFileStream(contentStream, fileName);
                remoteFileName = fileName + (fileName.endsWith("zip") ? "" : ".zip");
                ftpClient.copy(path, remoteFileName, zipStream);

                zipStream.close();
            } else {
                ftpClient.copy(path, remoteFileName, contentStream);
            }
            contentStream.close();

            //test if the file is correctly sent
            if (forceTransferControlSuccess) {
                if (Debug.infoOn()) {
                    Debug.logInfo(" Control if service really success the transfer", MODULE);
                }

                //recreate the connection
                ftpClient.closeConnection();
                ftpClient = createFtpClient(serverType);
                ftpClient.connect(hostname, username, password, port, defaultTimeout);
                ftpClient.setBinaryTransfer(binary);
                ftpClient.setPassiveMode(passive);

                //check the file name previously copy
                List<String> fileNames = ftpClient.list(path);
                if (Debug.infoOn()) {
                    Debug.logInfo(" For the path " + path + " we found " + fileNames, MODULE);
                }

                if (fileNames == null || !fileNames.contains(remoteFileName)) {
                    return ServiceUtil.returnError("DataResource " + content.getString("dataResourceId") + " return an empty stream");
                }
                if (Debug.infoOn()) {
                    Debug.logInfo(" Ok the file " + content.getString("contentName") + " is present", MODULE);
                }
            }
        } catch (GeneralException | IOException e) {
            return ServiceUtil.returnError(e.getMessage());
        } finally {
            try {
                if (ftpClient != null) {
                    ftpClient.closeConnection();
                }
            } catch (Exception e) {
                Debug.logWarning(e, "[getFile] Problem with FTP disconnect: ", MODULE);
            }
        }
        return resultMap;
    }

}
