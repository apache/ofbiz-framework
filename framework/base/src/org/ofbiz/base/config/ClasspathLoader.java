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
package org.ofbiz.base.config;

import java.net.*;
import java.io.*;
import org.ofbiz.base.util.*;

/**
 * Loads resources from the classpath
 *
 */
public class ClasspathLoader extends ResourceLoader implements java.io.Serializable {

    public URL getURL(String location) throws GenericConfigException {
        String fullLocation = fullLocation(location);
        URL url = UtilURL.fromResource(fullLocation);
        if (url == null) {
            throw new GenericConfigException("Classpath Resource not found: " + fullLocation);
        }
        return url;
    }
    
    public InputStream loadResource(String location) throws GenericConfigException {
        URL url = getURL(location);
        try {
            return url.openStream();
        } catch (java.io.IOException e) {
            throw new GenericConfigException("Error opening classpath resource at location [" + url.toExternalForm() + "]", e);
        }
    }
}
