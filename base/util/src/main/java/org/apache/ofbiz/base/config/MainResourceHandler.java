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

import java.io.InputStream;
import java.net.URL;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilXml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Contains resource information and provides for loading data
 *
 */
@SuppressWarnings("serial")
public final class MainResourceHandler implements ResourceHandler {

    private static final String MODULE = MainResourceHandler.class.getName();
    private final String xmlFilename;
    private final String loaderName;
    private final String location;

    public MainResourceHandler(String xmlFilename, Element element) {
        this.xmlFilename = xmlFilename;
        this.loaderName = element.getAttribute("loader");
        this.location = element.getAttribute("location");
        if (Debug.verboseOn()) {
            Debug.logVerbose("Created " + this.toString(), MODULE);
        }
    }

    public MainResourceHandler(String xmlFilename, String loaderName, String location) {
        this.xmlFilename = xmlFilename;
        this.loaderName = loaderName;
        this.location = location;
    }

    @Override
    public String getLoaderName() {
        return this.loaderName;
    }

    @Override
    public String getLocation() {
        return this.location;
    }

    @Override
    public Document getDocument() throws GenericConfigException {
        try {
            return UtilXml.readXmlDocument(this.getStream(), this.xmlFilename, true);
        } catch (org.xml.sax.SAXException | javax.xml.parsers.ParserConfigurationException | java.io.IOException e) {
            throw new GenericConfigException("Error reading " + this.toString(), e);
        }
    }

    @Override
    public InputStream getStream() throws GenericConfigException {
        return ResourceLoader.loadResource(this.xmlFilename, this.location, this.loaderName);
    }

    @Override
    public URL getURL() throws GenericConfigException {
        return ResourceLoader.getURL(this.xmlFilename, this.location, this.loaderName);
    }

    @Override
    public boolean isFileResource() throws GenericConfigException {
        ResourceLoader loader = ResourceLoader.getLoader(this.xmlFilename, this.loaderName);

        return loader instanceof FileLoader;
    }

    @Override
    public String getFullLocation() throws GenericConfigException {
        ResourceLoader loader = ResourceLoader.getLoader(this.xmlFilename, this.loaderName);

        return loader.fullLocation(location);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MainResourceHandler)) {
            return false;
        }

        MainResourceHandler other = (MainResourceHandler) obj;
        return this.loaderName.equals(other.loaderName) && this.xmlFilename.equals(other.xmlFilename) && this.location.equals(other.location);
    }

    @Override
    public int hashCode() {
        // the hashCode will weight by a combination xmlFilename and the combination of loaderName and location
        return (this.xmlFilename.hashCode() + ((this.loaderName.hashCode() + this.location.hashCode()) >> 1)) >> 1;
    }

    @Override
    public String toString() {
        return "ResourceHandler from XML file [" + this.xmlFilename + "] with loaderName [" + loaderName + "] and location [" + location + "]";
    }
}
