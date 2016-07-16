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
package org.apache.ofbiz.webapp;

import java.io.IOException;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;

import org.apache.ofbiz.base.component.ComponentConfig.WebappInfo;
import org.apache.ofbiz.base.util.Assert;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.webapp.control.ConfigXMLReader;
import org.apache.ofbiz.webapp.control.ConfigXMLReader.ControllerConfig;
import org.apache.ofbiz.webapp.control.ConfigXMLReader.RequestMap;
import org.apache.ofbiz.webapp.control.WebAppConfigurationException;
import org.apache.ofbiz.webapp.website.WebSiteProperties;
import org.xml.sax.SAXException;

/**
 * OFBiz URL builder.
 */
public final class OfbizUrlBuilder {

    public static final String module = OfbizUrlBuilder.class.getName();

    /**
     * Returns an <code>OfbizUrlBuilder</code> instance.
     * 
     * @param request
     * @throws GenericEntityException
     * @throws WebAppConfigurationException
     */
    public static OfbizUrlBuilder from(HttpServletRequest request) throws GenericEntityException, WebAppConfigurationException {
        Assert.notNull("request", request);
        OfbizUrlBuilder builder = (OfbizUrlBuilder) request.getAttribute("_OFBIZ_URL_BUILDER_");
        if (builder == null) {
            WebSiteProperties webSiteProps = WebSiteProperties.from(request);
            URL url = ConfigXMLReader.getControllerConfigURL(request.getServletContext());
            ControllerConfig config = ConfigXMLReader.getControllerConfig(url);
            String servletPath = (String) request.getAttribute("_CONTROL_PATH_");
            builder = new OfbizUrlBuilder(config, webSiteProps, servletPath);
            request.setAttribute("_OFBIZ_URL_BUILDER_", builder);
        }
        return builder;
    }

    /**
     * Returns an <code>OfbizUrlBuilder</code> instance. Use this method when you
     * don't have a <code>HttpServletRequest</code> object - like in scheduled jobs.
     * 
     * @param webAppInfo Optional - if <code>null</code>, the builder can only build the host part,
     * and that will be based only on the settings in <code>url.properties</code> (the WebSite
     * entity will be ignored).
     * @param delegator
     * @throws WebAppConfigurationException
     * @throws IOException
     * @throws SAXException
     * @throws GenericEntityException
     */
    public static OfbizUrlBuilder from(WebappInfo webAppInfo, Delegator delegator) throws WebAppConfigurationException, IOException, SAXException, GenericEntityException {
        WebSiteProperties webSiteProps = null;
        ControllerConfig config = null;
        String servletPath = null;
        if (webAppInfo != null) {
            Assert.notNull("delegator", delegator);
            String webSiteId = WebAppUtil.getWebSiteId(webAppInfo);
            if (webSiteId != null) {
                GenericValue webSiteValue = EntityQuery.use(delegator).from("WebSite").where("webSiteId", webSiteId).cache().queryOne();
                if (webSiteValue != null) {
                    webSiteProps = WebSiteProperties.from(webSiteValue);
                }
            }
            config = ConfigXMLReader.getControllerConfig(webAppInfo);
            servletPath = WebAppUtil.getControlServletPath(webAppInfo);
        }
        if (webSiteProps == null) {
            webSiteProps = WebSiteProperties.defaults(delegator);
        }
        return new OfbizUrlBuilder(config, webSiteProps, servletPath);
    }

    private final ControllerConfig config;
    private final WebSiteProperties webSiteProps;
    private final String servletPath;

    private OfbizUrlBuilder(ControllerConfig config, WebSiteProperties webSiteProps, String servletPath) {
        this.config = config;
        this.webSiteProps = webSiteProps;
        this.servletPath = servletPath;
    }

    /**
     * Builds a full URL - including scheme, host, servlet path and resource.
     * 
     * @param buffer
     * @param url
     * @param useSSL Default value to use - will be replaced by request-map setting
     * if one is found.
     * @return <code>true</code> if the URL uses https
     * @throws WebAppConfigurationException
     * @throws IOException
     */
    public boolean buildFullUrl(Appendable buffer, String url, boolean useSSL) throws WebAppConfigurationException, IOException {
        boolean makeSecure = buildHostPart(buffer, url, useSSL);
        buildPathPart(buffer, url);
        return makeSecure;
    }
    
    /**
     * Builds a partial URL - including the scheme and host, but not the servlet path or resource.
     * 
     * @param buffer
     * @param url
     * @param useSSL Default value to use - will be replaced by request-map setting
     * if one is found with security=true set.
     * @return <code>true</code> if the URL uses https
     * @throws WebAppConfigurationException
     * @throws IOException
     */
    public boolean buildHostPart(Appendable buffer, String url, boolean useSSL) throws WebAppConfigurationException, IOException {
        boolean makeSecure = useSSL;
        String[] pathElements = url.split("/");
        String requestMapUri = pathElements[0];
        int queryIndex = requestMapUri.indexOf("?");
        if (queryIndex != -1) {
            requestMapUri = requestMapUri.substring(0, queryIndex);
        }
        RequestMap requestMap = null;
        if (config != null) {
            requestMap = config.getRequestMapMap().get(requestMapUri);
        }
        if (!makeSecure && requestMap != null) { // if the request has security="true" then use it
            makeSecure = requestMap.securityHttps;
        }
        makeSecure = webSiteProps.getEnableHttps() & makeSecure;
        if (makeSecure) {
            String server = webSiteProps.getHttpsHost();
            if (server.isEmpty()) {
                server = "localhost";
            }
            buffer.append("https://");
            buffer.append(server);
            if (!webSiteProps.getHttpsPort().isEmpty()) {
                buffer.append(":").append(webSiteProps.getHttpsPort());
            }
        } else {
            String server = webSiteProps.getHttpHost();
            if (server.isEmpty()) {
                server = "localhost";
            }
            buffer.append("http://");
            buffer.append(server);
            if (!webSiteProps.getHttpPort().isEmpty()) {
                buffer.append(":").append(webSiteProps.getHttpPort());
            }
        }
        return makeSecure;
    }

    /**
     * Builds a partial URL - including the servlet path and resource, but not the scheme or host.
     * 
     * @param buffer
     * @param url
     * @throws WebAppConfigurationException
     * @throws IOException
     */
    public void buildPathPart(Appendable buffer, String url) throws WebAppConfigurationException, IOException {
        if (servletPath == null) {
            throw new IllegalStateException("Servlet path is unknown");
        }
        buffer.append(servletPath);
        if (!url.startsWith("/")) {
            buffer.append("/");
        }
        buffer.append(url);
    }
}
