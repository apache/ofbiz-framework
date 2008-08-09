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
package org.ofbiz.base.component;

import java.io.InputStream;
import java.net.URL;

import org.ofbiz.base.config.GenericConfigException;
import org.ofbiz.base.config.ResourceHandler;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.Debug;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Contains resource information and provides for loading data
 *
 */
public class ComponentResourceHandler implements ResourceHandler {

    public static final String module = ComponentResourceHandler.class.getName();
    protected String componentName;
    protected String loaderName;
    protected String location;

    public ComponentResourceHandler(String componentName, Element element) {
        this.componentName = componentName;
        this.loaderName = element.getAttribute("loader");
        this.location = element.getAttribute("location");
    }

    public ComponentResourceHandler(String componentName, String loaderName, String location) {
        this.componentName = componentName;
        this.loaderName = loaderName;
        this.location = location;
        if (Debug.verboseOn()) Debug.logVerbose("Created " + this.toString(), module);
    }

    public String getLoaderName() {
        return this.loaderName;
    }

    public String getLocation() {
        return this.location;
    }

    public Document getDocument() throws GenericConfigException {
        try {
            return UtilXml.readXmlDocument(this.getStream(), this.getFullLocation());
        } catch (org.xml.sax.SAXException e) {
            throw new GenericConfigException("Error reading " + this.toString(), e);
        } catch (javax.xml.parsers.ParserConfigurationException e) {
            throw new GenericConfigException("Error reading " + this.toString(), e);
        } catch (java.io.IOException e) {
            throw new GenericConfigException("Error reading " + this.toString(), e);
        }
    }

    public InputStream getStream() throws GenericConfigException {
        return ComponentConfig.getStream(componentName, loaderName, location);
    }

    public URL getURL() throws GenericConfigException {
        return ComponentConfig.getURL(componentName, loaderName, location);
    }

    public boolean isFileResource() throws GenericConfigException {
        return ComponentConfig.isFileResourceLoader(componentName, loaderName);
    }

    public String getFullLocation() throws GenericConfigException {
        return ComponentConfig.getFullLocation(componentName, loaderName, location);
    }

    public boolean equals(Object obj) {
        if (obj instanceof ComponentResourceHandler) {
            ComponentResourceHandler other = (ComponentResourceHandler) obj;

            if (this.loaderName.equals(other.loaderName) &&
                this.componentName.equals(other.componentName) &&
                this.location.equals(other.location)) {
                return true;
            }
        }
        return false;
    }

    public int hashCode() {
        // the hashCode will weight by a combination componentName and the combination of loaderName and location
        return (this.componentName.hashCode() + ((this.loaderName.hashCode() + this.location.hashCode()) >> 1)) >> 1;
    }

    public String toString() {
        return "ComponentResourceHandler from XML file [" + this.componentName + "] with loaderName [" + loaderName + "] and location [" + location + "]";
    }
}
