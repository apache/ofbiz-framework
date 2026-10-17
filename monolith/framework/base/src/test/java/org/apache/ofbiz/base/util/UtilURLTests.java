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
package org.apache.ofbiz.base.util;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;

import org.junit.jupiter.api.Test;

public class UtilURLTests {

    @Test
    public void rejectsFileScheme() {
        GeneralException e = assertThrows(GeneralException.class, () ->
                UtilURL.fromCheckedUrlString("file:///etc/passwd"));
        assertTrue(e.getMessage().contains("http/https"));
    }

    @Test
    public void rejectsLoopbackAddress() {
        GeneralException e = assertThrows(GeneralException.class, () ->
                UtilURL.fromCheckedUrlString("http://127.0.0.1:8080/internal"));
        assertTrue(e.getMessage().contains("loopback"));
    }

    @Test
    public void rejectsLoopbackHostname() {
        GeneralException e = assertThrows(GeneralException.class, () ->
                UtilURL.fromCheckedUrlString("http://localhost:8080/internal"));
        assertTrue(e.getMessage().contains("loopback"));
    }

    @Test
    public void rejectsLinkLocalCloudMetadataAddress() {
        GeneralException e = assertThrows(GeneralException.class, () ->
                UtilURL.fromCheckedUrlString("http://169.254.169.254/latest/meta-data/"));
        assertTrue(e.getMessage().contains("link-local"));
    }

    @Test
    public void rejectsPrivateAddress() {
        GeneralException e = assertThrows(GeneralException.class, () ->
                UtilURL.fromCheckedUrlString("http://10.0.0.5/internal"));
        assertTrue(e.getMessage().contains("private"));
    }

    @Test
    public void rejectsFtpScheme() {
        assertThrows(GeneralException.class, () -> UtilURL.fromCheckedUrlString("ftp://example.com/file"));
    }

    @Test
    public void allowsPubliclyRoutableHttpsHost() throws Exception {
        URL url = UtilURL.fromCheckedUrlString("https://93.184.216.34/index.html");
        assertTrue(url != null && "93.184.216.34".equals(url.getHost()));
    }

    @Test
    public void returnsNullForNullInput() throws Exception {
        assertNull(UtilURL.fromCheckedUrlString(null));
    }

    @Test
    public void returnsNullForUnparsableInput() throws Exception {
        assertNull(UtilURL.fromCheckedUrlString("not a url at all ::"));
    }
}
