/*
 * $Id: MainResourceHandler.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.ofbiz.base.config;

import java.io.InputStream;
import java.net.URL;

import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.Debug;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Contains resource information and provides for loading data
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class MainResourceHandler implements ResourceHandler {

    public static final String module = MainResourceHandler.class.getName();
    protected String xmlFilename;
    protected String loaderName;
    protected String location;

    public MainResourceHandler(String xmlFilename, Element element) {
        this.xmlFilename = xmlFilename;
        this.loaderName = element.getAttribute("loader");
        this.location = element.getAttribute("location");
        if (Debug.verboseOn()) Debug.logVerbose("Created " + this.toString(), module);
    }

    public MainResourceHandler(String xmlFilename, String loaderName, String location) {
        this.xmlFilename = xmlFilename;
        this.loaderName = loaderName;
        this.location = location;
    }

    public String getLoaderName() {
        return this.loaderName;
    }

    public String getLocation() {
        return this.location;
    }

    public Document getDocument() throws GenericConfigException {
        try {
            return UtilXml.readXmlDocument(this.getStream(), this.xmlFilename);
        } catch (org.xml.sax.SAXException e) {
            throw new GenericConfigException("Error reading " + this.toString(), e);
        } catch (javax.xml.parsers.ParserConfigurationException e) {
            throw new GenericConfigException("Error reading " + this.toString(), e);
        } catch (java.io.IOException e) {
            throw new GenericConfigException("Error reading " + this.toString(), e);
        }
    }

    public InputStream getStream() throws GenericConfigException {
        return ResourceLoader.loadResource(this.xmlFilename, this.location, this.loaderName);
    }

    public URL getURL() throws GenericConfigException {
        return ResourceLoader.getURL(this.xmlFilename, this.location, this.loaderName);
    }

    public boolean isFileResource() throws GenericConfigException {
        ResourceLoader loader = ResourceLoader.getLoader(this.xmlFilename, this.loaderName);

        if (loader instanceof FileLoader) {
            return true;
        } else {
            return false;
        }
    }

    public String getFullLocation() throws GenericConfigException {
        ResourceLoader loader = ResourceLoader.getLoader(this.xmlFilename, this.loaderName);

        return loader.fullLocation(location);
    }

    public boolean equals(Object obj) {
        if (obj instanceof MainResourceHandler) {
            MainResourceHandler other = (MainResourceHandler) obj;

            if (this.loaderName.equals(other.loaderName) &&
                this.xmlFilename.equals(other.xmlFilename) &&
                this.location.equals(other.location)) {
                return true;
            }
        }
        return false;
    }

    public int hashCode() {
        // the hashCode will weight by a combination xmlFilename and the combination of loaderName and location
        return (this.xmlFilename.hashCode() + ((this.loaderName.hashCode() + this.location.hashCode()) >> 1)) >> 1;
    }

    public String toString() {
        return "ResourceHandler from XML file [" + this.xmlFilename + "] with loaderName [" + loaderName + "] and location [" + location + "]";
    }
}
