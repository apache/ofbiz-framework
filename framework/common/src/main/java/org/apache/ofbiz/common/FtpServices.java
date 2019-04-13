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

import static org.apache.ofbiz.base.util.UtilGenerics.checkList;

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

    public final static String module = FtpServices.class.getName();
    public static final String resource = "CommonUiLabels";

    public static Map<String, Object> putFile(DispatchContext dctx, Map<String, ?> context) {
        Locale locale = (Locale) context.get("locale");
        Debug.logInfo("[putFile] starting...", module);
        InputStream localFile = null;
        try {
            localFile = new FileInputStream((String) context.get("localFilename"));
        } catch (IOException ioe) {
            Debug.logError(ioe, "[putFile] Problem opening local file", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "CommonFtpFileCannotBeOpen", locale));
        }
        List<String> errorList = new LinkedList<>();
        FTPClient ftp = new FTPClient();
        try {
            Integer defaultTimeout = (Integer) context.get("defaultTimeout");
            if (UtilValidate.isNotEmpty(defaultTimeout)) {
                Debug.logInfo("[putFile] set default timeout to: " + defaultTimeout.intValue() + " milliseconds", module);
                ftp.setDefaultTimeout(defaultTimeout.intValue());
            }
            Debug.logInfo("[putFile] connecting to: " + (String) context.get("hostname"), module);
            ftp.connect((String) context.get("hostname"));
            if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
                Debug.logInfo("[putFile] Server refused connection", module);
                errorList.add(UtilProperties.getMessage(resource, "CommonFtpConnectionRefused", locale));
            } else {
                String username = (String) context.get("username");
                String password = (String) context.get("password");
                Debug.logInfo("[putFile] logging in: username=" + username + ", password=" + password, module);
                if (!ftp.login(username, password)) {
                    Debug.logInfo("[putFile] login failed", module);
                    errorList.add(UtilProperties.getMessage(resource, "CommonFtpLoginFailure", UtilMisc.toMap("username", username, "password", password), locale));
                } else {
                    Boolean binaryTransfer = (Boolean) context.get("binaryTransfer");
                    boolean binary = (binaryTransfer == null) ? false : binaryTransfer.booleanValue();
                    if (binary) {
                        ftp.setFileType(FTP.BINARY_FILE_TYPE);
                    }
                    Boolean passiveMode = (Boolean) context.get("passiveMode");
                    boolean passive = (passiveMode == null) ? true : passiveMode.booleanValue();
                    if (passive) {
                        ftp.enterLocalPassiveMode();
                    }
                    Debug.logInfo("[putFile] storing local file remotely as: " + context.get("remoteFilename"), module);
                    if (!ftp.storeFile((String) context.get("remoteFilename"), localFile)) {
                        Debug.logInfo("[putFile] store was unsuccessful", module);
                        errorList.add(UtilProperties.getMessage(resource, "CommonFtpFileNotSentSuccesfully", UtilMisc.toMap("replyString", ftp.getReplyString()), locale));
                    } else {
                        Debug.logInfo("[putFile] store was successful", module);
                        List<String> siteCommands = checkList(context.get("siteCommands"), String.class);
                        if (siteCommands != null) {
                            for (String command : siteCommands) {
                                Debug.logInfo("[putFile] sending SITE command: " + command, module);
                                if (!ftp.sendSiteCommand(command)) {
                                    errorList.add(UtilProperties.getMessage(resource, "CommonFtpSiteCommandFailed", UtilMisc.toMap("command", command, "replyString", ftp.getReplyString()), locale));
                                }
                            }
                        }
                    }
                }
                ftp.logout();
            }
        } catch (IOException ioe) {
            Debug.logWarning(ioe, "[putFile] caught exception: " + ioe.getMessage(), module);
            errorList.add(UtilProperties.getMessage(resource, "CommonFtpProblemWithTransfer", UtilMisc.toMap("errorString", ioe.getMessage()), locale));
        } finally {
            try {
                if (ftp.isConnected()) {
                    ftp.disconnect();
                }
            } catch (Exception e) {
                Debug.logWarning(e, "[putFile] Problem with FTP disconnect: ", module);
            }
        }
        if (errorList.size() > 0) {
            Debug.logError("[putFile] The following error(s) (" + errorList.size() + ") occurred: " + errorList, module);
            return ServiceUtil.returnError(errorList);
        }
        Debug.logInfo("[putFile] finished successfully", module);
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> getFile(DispatchContext dctx, Map<String, ?> context) {
        Locale locale = (Locale) context.get("locale");
        String localFilename = (String) context.get("localFilename");
        OutputStream localFile = null;
        try {
            localFile = new FileOutputStream(localFilename);
        } catch (IOException ioe) {
            Debug.logError(ioe, "[getFile] Problem opening local file", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "CommonFtpFileCannotBeOpen", locale));
        }
        List<String> errorList = new LinkedList<>();
        FTPClient ftp = new FTPClient();
        try {
            Integer defaultTimeout = (Integer) context.get("defaultTimeout");
            if (UtilValidate.isNotEmpty(defaultTimeout)) {
                Debug.logInfo("[getFile] Set default timeout to: " + defaultTimeout.intValue() + " milliseconds", module);
                ftp.setDefaultTimeout(defaultTimeout.intValue());
            }
            ftp.connect((String) context.get("hostname"));
            if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
                errorList.add(UtilProperties.getMessage(resource, "CommonFtpConnectionRefused", locale));
            } else {
                String username = (String) context.get("username");
                String password = (String) context.get("password");
                if (!ftp.login(username, password)) {
                    errorList.add(UtilProperties.getMessage(resource, "CommonFtpLoginFailure", UtilMisc.toMap("username", username, "password", password), locale));
                } else {
                    Boolean binaryTransfer = (Boolean) context.get("binaryTransfer");
                    boolean binary = (binaryTransfer == null) ? false : binaryTransfer.booleanValue();
                    if (binary) {
                        ftp.setFileType(FTP.BINARY_FILE_TYPE);
                    }
                    Boolean passiveMode = (Boolean) context.get("passiveMode");
                    boolean passive = (passiveMode == null) ? false : passiveMode.booleanValue();
                    if (passive) {
                        ftp.enterLocalPassiveMode();
                    }
                    if (!ftp.retrieveFile((String) context.get("remoteFilename"), localFile)) {
                        errorList.add(UtilProperties.getMessage(resource, "CommonFtpFileNotSentSuccesfully", UtilMisc.toMap("replyString", ftp.getReplyString()), locale));
                    }
                }
                ftp.logout();
            }
        } catch (IOException ioe) {
            Debug.logWarning(ioe, "[getFile] caught exception: " + ioe.getMessage(), module);
            errorList.add(UtilProperties.getMessage(resource, "CommonFtpProblemWithTransfer", UtilMisc.toMap("errorString", ioe.getMessage()), locale));
        } finally {
            try {
                if (ftp.isConnected()) {
                    ftp.disconnect();
                }
            } catch (Exception e) {
                Debug.logWarning(e, "[getFile] Problem with FTP disconnect: ", module);
            }
        }
        if (errorList.size() > 0) {
            Debug.logError("[getFile] The following error(s) (" + errorList.size() + ") occurred: " + errorList, module);
            return ServiceUtil.returnError(errorList);
        }
        return ServiceUtil.returnSuccess();
    }
}
