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
package org.apache.ofbiz.base.config;

import java.net.URL;
import java.io.InputStream;
import org.apache.ofbiz.base.util.UtilURL;

/**
 * Loads resources from the file system
 *
 */
@SuppressWarnings("serial")
public class FileLoader extends ResourceLoader implements java.io.Serializable {

    @Override
    public URL getURL(String location) throws GenericConfigException {
        String fullLocation = fullLocation(location);
        URL fileUrl = null;

        fileUrl = UtilURL.fromFilename(fullLocation);
        if (fileUrl == null) {
            throw new GenericConfigException("File Resource not found: " + fullLocation);
        }
        return fileUrl;
    }

    @Override
    public InputStream loadResource(String location) throws GenericConfigException {
        URL fileUrl = getURL(location);
        try {
            return fileUrl.openStream();
        } catch (java.io.IOException e) {
            throw new GenericConfigException("Error opening file at location [" + fileUrl.toExternalForm() + "]", e);
        }
    }
}
