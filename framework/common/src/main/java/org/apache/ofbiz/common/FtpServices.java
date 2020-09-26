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
package org.apache.ofbiz.common;

import static org.apache.ofbiz.base.util.UtilGenerics.checkCollection;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * FTP Services.
 *
 */
public class FtpServices {

    private static final String MODULE = FtpServices.class.getName();
    private static final String RESOURCE = "CommonUiLabels";

    public static Map<String, Object> putFile(DispatchContext dctx, Map<String, ?> context) {
        Locale locale = (Locale) context.get("locale");
        Debug.logInfo("[putFile] starting...", MODULE);
        InputStream localFile = null;
        try {
            localFile = new FileInputStream((String) context.get("localFilename"));
        } catch (IOException ioe) {
            Debug.logError(ioe, "[putFile] Problem opening local file", MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "CommonFtpFileCannotBeOpen", locale));
        }
        List<String> errorList = new LinkedList<>();
        FTPClient ftp = new FTPClient();
        try {
            Integer defaultTimeout = (Integer) context.get("defaultTimeout");
            if (UtilValidate.isNotEmpty(defaultTimeout)) {
                Debug.logInfo("[putFile] set default timeout to: " + defaultTimeout + " milliseconds", MODULE);
                ftp.setDefaultTimeout(defaultTimeout);
            }
            Debug.logInfo("[putFile] connecting to: " + (String) context.get("hostname"), MODULE);
            ftp.connect((String) context.get("hostname"));
            if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
                Debug.logInfo("[putFile] Server refused connection", MODULE);
                errorList.add(UtilProperties.getMessage(RESOURCE, "CommonFtpConnectionRefused", locale));
            } else {
                String username = (String) context.get("username");
                String password = (String) context.get("password");
                Debug.logInfo("[putFile] logging in: username=" + username + ", password=" + password, MODULE);
                if (!ftp.login(username, password)) {
                    Debug.logInfo("[putFile] login failed", MODULE);
                    errorList.add(UtilProperties.getMessage(RESOURCE, "CommonFtpLoginFailure", UtilMisc.toMap("username",
                            username, "password", password), locale));
                } else {
                    Boolean binaryTransfer = (Boolean) context.get("binaryTransfer");
                    boolean binary = (binaryTransfer == null) ? false : binaryTransfer;
                    if (binary) {
                        ftp.setFileType(FTP.BINARY_FILE_TYPE);
                    }
                    Boolean passiveMode = (Boolean) context.get("passiveMode");
                    boolean passive = (passiveMode == null) ? true : passiveMode;
                    if (passive) {
                        ftp.enterLocalPassiveMode();
                    }
                    Debug.logInfo("[putFile] storing local file remotely as: " + context.get("remoteFilename"), MODULE);
                    if (!ftp.storeFile((String) context.get("remoteFilename"), localFile)) {
                        Debug.logInfo("[putFile] store was unsuccessful", MODULE);
                        errorList.add(UtilProperties.getMessage(RESOURCE, "CommonFtpFileNotSentSuccesfully",
                                UtilMisc.toMap("replyString", ftp.getReplyString()), locale));
                    } else {
                        Debug.logInfo("[putFile] store was successful", MODULE);
                        List<String> siteCommands = checkCollection(context.get("siteCommands"), String.class);
                        if (siteCommands != null) {
                            for (String command : siteCommands) {
                                Debug.logInfo("[putFile] sending SITE command: " + command, MODULE);
                                if (!ftp.sendSiteCommand(command)) {
                                    errorList.add(UtilProperties.getMessage(RESOURCE, "CommonFtpSiteCommandFailed",
                                            UtilMisc.toMap("command", command, "replyString", ftp.getReplyString()), locale));
                                }
                            }
                        }
                    }
                }
                ftp.logout();
            }
        } catch (IOException ioe) {
            Debug.logWarning(ioe, "[putFile] caught exception: " + ioe.getMessage(), MODULE);
            errorList.add(UtilProperties.getMessage(RESOURCE, "CommonFtpProblemWithTransfer", UtilMisc.toMap("errorString",
                    ioe.getMessage()), locale));
        } finally {
            try {
                if (ftp.isConnected()) {
                    ftp.disconnect();
                }
            } catch (Exception e) {
                Debug.logWarning(e, "[putFile] Problem with FTP disconnect: ", MODULE);
            }
        }
        if (!errorList.isEmpty()) {
            Debug.logError("[putFile] The following error(s) (" + errorList.size() + ") occurred: " + errorList, MODULE);
            return ServiceUtil.returnError(errorList);
        }
        Debug.logInfo("[putFile] finished successfully", MODULE);
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> getFile(DispatchContext dctx, Map<String, ?> context) {
        Locale locale = (Locale) context.get("locale");
        String localFilename = (String) context.get("localFilename");
        OutputStream localFile = null;
        try {
            localFile = new FileOutputStream(localFilename);
        } catch (IOException ioe) {
            Debug.logError(ioe, "[getFile] Problem opening local file", MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "CommonFtpFileCannotBeOpen", locale));
        }
        List<String> errorList = new LinkedList<>();
        FTPClient ftp = new FTPClient();
        try {
            Integer defaultTimeout = (Integer) context.get("defaultTimeout");
            if (UtilValidate.isNotEmpty(defaultTimeout)) {
                Debug.logInfo("[getFile] Set default timeout to: " + defaultTimeout + " milliseconds", MODULE);
                ftp.setDefaultTimeout(defaultTimeout);
            }
            ftp.connect((String) context.get("hostname"));
            if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
                errorList.add(UtilProperties.getMessage(RESOURCE, "CommonFtpConnectionRefused", locale));
            } else {
                String username = (String) context.get("username");
                String password = (String) context.get("password");
                if (!ftp.login(username, password)) {
                    errorList.add(UtilProperties.getMessage(RESOURCE, "CommonFtpLoginFailure", UtilMisc.toMap("username",
                            username, "password", password), locale));
                } else {
                    Boolean binaryTransfer = (Boolean) context.get("binaryTransfer");
                    boolean binary = (binaryTransfer == null) ? false : binaryTransfer;
                    if (binary) {
                        ftp.setFileType(FTP.BINARY_FILE_TYPE);
                    }
                    Boolean passiveMode = (Boolean) context.get("passiveMode");
                    boolean passive = (passiveMode == null) ? false : passiveMode;
                    if (passive) {
                        ftp.enterLocalPassiveMode();
                    }
                    if (!ftp.retrieveFile((String) context.get("remoteFilename"), localFile)) {
                        errorList.add(UtilProperties.getMessage(RESOURCE, "CommonFtpFileNotSentSuccesfully",
                                UtilMisc.toMap("replyString", ftp.getReplyString()), locale));
                    }
                }
                ftp.logout();
            }
        } catch (IOException ioe) {
            Debug.logWarning(ioe, "[getFile] caught exception: " + ioe.getMessage(), MODULE);
            errorList.add(UtilProperties.getMessage(RESOURCE, "CommonFtpProblemWithTransfer", UtilMisc.toMap("errorString",
                    ioe.getMessage()), locale));
        } finally {
            try {
                if (ftp.isConnected()) {
                    ftp.disconnect();
                }
            } catch (Exception e) {
                Debug.logWarning(e, "[getFile] Problem with FTP disconnect: ", MODULE);
            }
        }
        if (!errorList.isEmpty()) {
            Debug.logError("[getFile] The following error(s) (" + errorList.size() + ") occurred: " + errorList, MODULE);
            return ServiceUtil.returnError(errorList);
        }
        return ServiceUtil.returnSuccess();
    }
}
