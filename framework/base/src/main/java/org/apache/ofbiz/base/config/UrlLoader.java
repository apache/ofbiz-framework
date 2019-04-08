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
import java.io.Serializable;

/**
 * Loads resources from a URL
 *
 */
@SuppressWarnings("serial")
public class UrlLoader extends ResourceLoader implements Serializable {

    @Override
    public URL getURL(String location) throws GenericConfigException {
        String fullLocation = fullLocation(location);

        URL url = null;

        try {
            url = new URL(fullLocation);
        } catch (java.net.MalformedURLException e) {
            throw new GenericConfigException("Error with malformed URL while trying to load URL resource at location [" + fullLocation + "]", e);
        }
        return url;
    }

    @Override
    public InputStream loadResource(String location) throws GenericConfigException {
        URL url = getURL(location);

        try {
            return url.openStream();
        } catch (java.io.IOException e) {
            throw new GenericConfigException("Error opening URL resource at location [" + url.toExternalForm() + "]", e);
        }
    }
}
