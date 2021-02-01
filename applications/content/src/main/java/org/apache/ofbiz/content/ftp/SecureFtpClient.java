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
import java.util.List;

public class SecureFtpClient implements FtpClientInterface {

    private static final String MODULE = SecureFtpClient.class.getName();

    /**
     * TODO : to implements
     */
    @Override
    public void connect(String hostname, String username, String password, Long port, Long timeout) throws IOException {

    }

    @Override
    public void copy(String path, String fileName, InputStream file) throws IOException {

    }

    @Override
    public List<String> list(String path) throws IOException {
        return null;
    }

    @Override
    public void setBinaryTransfer(boolean isBinary) throws IOException {

    }

    @Override
    public void setPassiveMode(boolean isPassive) throws IOException {

    }

    @Override
    public void closeConnection() {

    }
}
