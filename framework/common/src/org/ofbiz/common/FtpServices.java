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
package org.ofbiz.common;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

/**
 *  FTP Services
 *
 */
public class FtpServices {

    public final static String module = FtpServices.class.getName();

    public static Map putFile(DispatchContext dctx, Map context) {
        Debug.logInfo("[putFile] starting...", module);

        InputStream localFile = null;
        try {
            localFile = new FileInputStream((String) context.get("localFilename"));
        } catch (IOException ioe) {
            Debug.logError(ioe, "[putFile] Problem opening local file", module);
            return ServiceUtil.returnError("ERROR: Could not open local file");
        }

        List errorList = new ArrayList();

        FTPClient ftp = new FTPClient();
        try {
            Debug.logInfo("[putFile] connecting to: " + (String) context.get("hostname"), module);
            ftp.connect((String) context.get("hostname"));
            if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
                Debug.logInfo("[putFile] Server refused connection", module);
                errorList.add("connection refused");
            } else {
                String username = (String) context.get("username");
                String password = (String) context.get("password");
                Debug.logInfo("[putFile] logging in: username=" + username + ", password=" + password, module);
                if (!ftp.login(username, password)) {
                    Debug.logInfo("[putFile] login failed", module);
                    errorList.add("Login failed (" + username + ", " + password + ")");
                } else {
                    Boolean binaryTransfer = (Boolean) context.get("binaryTransfer");
                    boolean binary = (binaryTransfer == null) ? false : binaryTransfer.booleanValue();
                    if (binary) { ftp.setFileType(FTP.BINARY_FILE_TYPE); }

                    Boolean passiveMode = (Boolean) context.get("passiveMode");
                    boolean passive = (passiveMode == null) ? true : passiveMode.booleanValue();
                    if (passive) { ftp.enterLocalPassiveMode(); }

                    Debug.logInfo("[putFile] storing local file remotely as: " + context.get("remoteFilename"), module);
                    if (!ftp.storeFile((String) context.get("remoteFilename"), localFile)) {
                        Debug.logInfo("[putFile] store was unsuccessful", module);
                        errorList.add("File not sent succesfully: " + ftp.getReplyString());
                    } else {
                        Debug.logInfo("[putFile] store was successful", module);
                        List siteCommands = (List) context.get("siteCommands");
                        if (siteCommands != null) {
                            Iterator ci = siteCommands.iterator();
                            while (ci.hasNext()) {
                                String command = (String) ci.next();
                                Debug.logInfo("[putFile] sending SITE command: " + command, module);
                                if (!ftp.sendSiteCommand(command)) {
                                    errorList.add("SITE command (" + command + ") failed: " + ftp.getReplyString());
                                }
                            }
                        }
                    }
                }
                ftp.logout();
            }
        } catch (IOException ioe) {
            Debug.log(ioe, "[putFile] caught exception: " + ioe.getMessage(), module);
            errorList.add("Problem with FTP transfer: " + ioe.getMessage());
        } finally {
            if (ftp.isConnected()) {
                try {
                    ftp.disconnect();
                } catch (IOException dce) {
                    Debug.logWarning(dce, "[putFile] Problem with FTP disconnect", module);
                }
            }
        }

        try {
            localFile.close();
        } catch (IOException ce) {
            Debug.logWarning(ce, "[putFile] Problem closing local file", module);
        }

        if (errorList.size() > 0) {
            Debug.logError("[putFile] The following error(s) (" + errorList.size() + ") occurred: " + errorList, module);
            return ServiceUtil.returnError(errorList);
        }

        Debug.logInfo("[putFile] finished successfully", module);

        return ServiceUtil.returnSuccess();
    }

    public static Map getFile(DispatchContext dctx, Map context) {

        String localFilename = (String) context.get("localFilename");

        OutputStream localFile = null;
        try {
            localFile = new FileOutputStream(localFilename);
        } catch (IOException ioe) {
            Debug.logError(ioe, "[getFile] Problem opening local file", module);
            return ServiceUtil.returnError("ERROR: Could not open local file");
        }

        List errorList = new ArrayList();

        FTPClient ftp = new FTPClient();
        try {
            ftp.connect((String) context.get("hostname"));
            if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
                errorList.add("Server refused connection");
            } else {
                String username = (String) context.get("username");
                String password = (String) context.get("password");

                if (!ftp.login(username, password)) {
                    errorList.add("Login failed (" + username + ", " + password + ")");
                } else {
                    Boolean binaryTransfer = (Boolean) context.get("binaryTransfer");
                    boolean binary = (binaryTransfer == null) ? false : binaryTransfer.booleanValue();
                    if (binary) { ftp.setFileType(FTP.BINARY_FILE_TYPE); }

                    Boolean passiveMode = (Boolean) context.get("passiveMode");
                    boolean passive = (passiveMode == null) ? false : passiveMode.booleanValue();
                    if (passive) { ftp.enterLocalPassiveMode(); }

                    if (!ftp.retrieveFile((String) context.get("remoteFilename"), localFile)) {
                        errorList.add("File not received succesfully: " + ftp.getReplyString());
                    }
                }
                ftp.logout();
            }
        } catch (IOException ioe) {
            errorList.add("Problem with FTP transfer: " + ioe.getMessage());
        } finally {
            if (ftp.isConnected()) {
                try {
                    ftp.disconnect();
                } catch (IOException dce) {
                    Debug.logWarning(dce, "[getFile] Problem with FTP disconnect", module);
                }
            }
        }

        try {
            localFile.close();
        } catch (IOException ce) {
            Debug.logWarning(ce, "[getFile] Problem closing local file", module);
        }

        if (errorList.size() > 0) {
            Debug.logError("[getFile] The following error(s) (" + errorList.size() + ") occurred: " + errorList, module);
            return ServiceUtil.returnError(errorList);
        }

        return ServiceUtil.returnSuccess();
    }
}
