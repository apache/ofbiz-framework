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

import org.apache.ofbiz.base.util.GeneralException;

public interface FtpClientInterface {

    /**
     * Initialization of a file transfer client, and connection to the given server
     * @param hostname hostname to connect to
     * @param username username to login with
     * @param password password to login with
     * @param port     port to connect to the server, optional
     * @param timeout  timeout for connection process, optional
     * @throws IOException
     */
    void connect(String hostname, String username, String password, Long port, Long timeout) throws IOException, GeneralException;

    /**
     * Copy of the give file to the connected server into the path.
     * @param path     path to copy the file to
     * @param fileName name of the copied file
     * @param file     data to copy
     * @throws IOException
     */
    void copy(String path, String fileName, InputStream file) throws IOException;

    List<String> list(String path) throws IOException;

    void setBinaryTransfer(boolean isBinary) throws IOException;

    void setPassiveMode(boolean isPassive) throws IOException;

    /**
     * Close opened connection
     */
    void closeConnection() throws IOException;
}
