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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilProperties;

public class SimpleFtpClient implements FtpClientInterface {

    private static final String MODULE = SimpleFtpClient.class.getName();
    private FTPClient client;

    public SimpleFtpClient() {
        client = new FTPClient();
    }

    @Override
    public void connect(String hostname, String username, String password, Long port, Long timeout) throws IOException, GeneralException {
        if (client == null) return;
        if (client.isConnected()) return;
        if (port != null) {
            client.connect(hostname, port.intValue());
        } else {
            client.connect(hostname);
        }
        if (timeout != null) client.setDefaultTimeout(timeout.intValue());

        if (!FTPReply.isPositiveCompletion(client.getReplyCode())) {
            Debug.logError("Server refused connection", MODULE);
            throw new GeneralException(UtilProperties.getMessage("CommonUiLabels", "CommonFtpConnectionRefused", Locale.getDefault()));
        }
        if (!client.login(username, password)) {
            Debug.logError("login failed", MODULE);
            throw new GeneralException(UtilProperties.getMessage("CommonUiLabels", "CommonFtpLoginFailure", Locale.getDefault()));
        }
    }

    @Override
    public List<String> list(String path) throws IOException {
        FTPFile[] files = client.listFiles(path);
        List<String> fileNames = new ArrayList<>();
        for (FTPFile file : files) {
            fileNames.add(file.getName());
        }
        return fileNames;
    }

    @Override
    public void setBinaryTransfer(boolean isBinary) throws IOException {
        if (isBinary) {
            client.setFileType(FTP.BINARY_FILE_TYPE);
        } else {
            client.setFileType(FTP.ASCII_FILE_TYPE);
        }
    }

    @Override
    public void setPassiveMode(boolean isPassive) {
        if (isPassive) {
            client.enterLocalPassiveMode();
        } else {
            client.enterLocalActiveMode();
        }
    }

    @Override
    public void copy(String path, String fileName, InputStream file) throws IOException {
        client.changeWorkingDirectory(path);
        client.storeFile(fileName, file);
    }

    @Override
    public void closeConnection() throws IOException {
        if (client != null && client.isConnected()) {
            client.logout();
        }
    }
}
