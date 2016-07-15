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
package org.ofbiz.webapp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map.Entry;

import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.descriptor.DigesterFactory;
import org.apache.tomcat.util.descriptor.web.ServletDef;
import org.apache.tomcat.util.descriptor.web.WebRuleSet;
import org.apache.tomcat.util.descriptor.web.WebXml;
import org.ofbiz.base.component.ComponentConfig;
import org.ofbiz.base.component.ComponentConfig.WebappInfo;
import org.ofbiz.base.util.Assert;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilXml.LocalErrorHandler;
import org.ofbiz.base.util.UtilXml.LocalResolver;
import org.ofbiz.base.util.cache.UtilCache;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Web application utilities.
 * <p>This class reuses some of the Tomcat/Catalina classes for convenience, but
 * OFBiz does not need to be running on Tomcat for this to work.</p>
 */
public final class WebAppUtil {

    public static final String module = WebAppUtil.class.getName();
    private static final String webAppFileName = "/WEB-INF/web.xml";
    private static final UtilCache<String, WebXml> webXmlCache = UtilCache.createUtilCache("webapp.WebXml");

    /**
     * Returns the control servlet path. The path consists of the web application's mount-point
     * specified in the <code>ofbiz-component.xml</code> file and the servlet mapping specified
     * in the web application's <code>web.xml</code> file.
     * 
     * @param webAppInfo
     * @throws IOException
     * @throws SAXException
     */
    public static String getControlServletPath(WebappInfo webAppInfo) throws IOException, SAXException {
        Assert.notNull("webAppInfo", webAppInfo);
        String servletMapping = null;
        WebXml webXml = getWebXml(webAppInfo);
        for (ServletDef servletDef : webXml.getServlets().values()) {
            if ("org.ofbiz.webapp.control.ControlServlet".equals(servletDef.getServletClass()) || "org.ofbiz.product.category.SeoControlServlet".equals(servletDef.getServletClass())) {
                String servletName = servletDef.getServletName();
                // Catalina servlet mappings: key = url-pattern, value = servlet-name.
                for (Entry<String, String> entry : webXml.getServletMappings().entrySet()) {
                    if (servletName.equals(entry.getValue())) {
                        servletMapping = entry.getKey();
                        break;
                    }
                }
                break;
            }
        }
        if (servletMapping == null) {
            throw new IllegalArgumentException("org.ofbiz.webapp.control.ControlServlet mapping not found in " + webAppInfo.getLocation() + webAppFileName);
        }
        servletMapping = servletMapping.replace("*", "");
        String servletPath = webAppInfo.contextRoot.concat(servletMapping);
        return servletPath;
    }

    /**
     * Returns the <code>WebappInfo</code> instance associated to the specified web site ID.
     * Throws <code>IllegalArgumentException</code> if the web site ID was not found.
     * 
     * @param webSiteId
     * @throws IOException
     * @throws SAXException
     */
    public static WebappInfo getWebappInfoFromWebsiteId(String webSiteId) throws IOException, SAXException {
        Assert.notNull("webSiteId", webSiteId);
        for (WebappInfo webAppInfo : ComponentConfig.getAllWebappResourceInfos()) {
            if (webSiteId.equals(WebAppUtil.getWebSiteId(webAppInfo))) {
                return webAppInfo;
            }
        }
        throw new IllegalArgumentException("Web site ID '" + webSiteId + "' not found.");
    }

    /**
     * Returns the web site ID - as configured in the web application's <code>web.xml</code> file,
     * or <code>null</code> if no web site ID was found.
     * 
     * @param webAppInfo
     * @throws IOException
     * @throws SAXException
     */
    public static String getWebSiteId(WebappInfo webAppInfo) throws IOException, SAXException {
        Assert.notNull("webAppInfo", webAppInfo);
        WebXml webXml = getWebXml(webAppInfo);
        return webXml.getContextParams().get("webSiteId");
    }

    /**
     * Returns a <code>WebXml</code> instance that models the web application's <code>web.xml</code> file.
     * 
     * @param webAppInfo
     * @throws IOException
     * @throws SAXException
     */
    public static WebXml getWebXml(WebappInfo webAppInfo) throws IOException, SAXException {
        Assert.notNull("webAppInfo", webAppInfo);
        String webXmlFileLocation = webAppInfo.getLocation().concat(webAppFileName);
        return parseWebXmlFile(webXmlFileLocation, true);
    }

    /**
     * Parses the specified <code>web.xml</code> file into a <code>WebXml</code> instance.
     * 
     * @param webXmlFileLocation
     * @param validate
     * @throws IOException
     * @throws SAXException
     */
    public static WebXml parseWebXmlFile(String webXmlFileLocation, boolean validate) throws IOException, SAXException {
        Assert.notEmpty("webXmlFileLocation", webXmlFileLocation);
        WebXml result = webXmlCache.get(webXmlFileLocation);
        if (result == null) {
            File file = new File(webXmlFileLocation);
            if (!file.exists()) {
                throw new IllegalArgumentException(webXmlFileLocation + " does not exist.");
            }
            boolean namespaceAware = true;
            InputStream is = new FileInputStream(file);
            result = new WebXml();
            LocalResolver lr = new LocalResolver(new DefaultHandler());
            ErrorHandler handler = new LocalErrorHandler(webXmlFileLocation, lr);
            Digester digester = DigesterFactory.newDigester(validate, namespaceAware, new WebRuleSet(), false);
            digester.getParser();
            digester.push(result);
            digester.setErrorHandler(handler);
            try {
                digester.parse(new InputSource(is));
            } finally {
                digester.reset();
                if (is != null) {
                    try {
                        is.close();
                    } catch (Throwable t) {
                        Debug.logError(t, "Exception thrown while parsing " + webXmlFileLocation + ": ", module);
                    }
                }
            }
            result = webXmlCache.putIfAbsentAndGet(webXmlFileLocation, result);
        }
        return result;
    }

    private WebAppUtil() {}
}
