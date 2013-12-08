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
package org.ofbiz.webapp.website;

import javax.servlet.http.HttpServletRequest;

import org.ofbiz.base.container.ClassLoaderContainer;
import org.ofbiz.base.lang.ThreadSafe;
import org.ofbiz.base.util.Assert;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;

/**
 * Web site properties.
 */
@ThreadSafe
public final class WebSiteProperties {


    /**
     * Returns a <code>WebSiteProperties</code> instance initialized to the settings found
     * in the <code>url.properties</code> file.
     */
    public static WebSiteProperties defaults() {
        return new WebSiteProperties();
    }

    /**
     * Returns a <code>WebSiteProperties</code> instance initialized to the settings found
     * in the application's WebSite entity value. If the application does not have a
     * WebSite entity value then the instance is initialized to the settings found
     * in the <code>url.properties</code> file.
     * 
     * @param request
     * @throws GenericEntityException
     */
    public static WebSiteProperties from(HttpServletRequest request) throws GenericEntityException {
        Assert.notNull("request", request);
        WebSiteProperties webSiteProps = (WebSiteProperties) request.getAttribute("_WEBSITE_PROPS_");
        if (webSiteProps == null) {
            WebSiteProperties defaults = new WebSiteProperties();
            String httpPort = defaults.getHttpPort();
            String httpHost = defaults.getHttpHost();
            String httpsPort = defaults.getHttpsPort();
            String httpsHost = defaults.getHttpsHost();
            boolean enableHttps = defaults.getEnableHttps();
            Delegator delegator = (Delegator) request.getAttribute("delegator");
            if (delegator != null) {
                String webSiteId = WebSiteWorker.getWebSiteId(request);
                if (webSiteId != null) {
                    GenericValue webSiteValue = delegator.findOne("WebSite", UtilMisc.toMap("webSiteId", webSiteId), true);
                    if (webSiteValue != null) {
                        if (webSiteValue.get("httpPort") != null) {
                            httpPort = webSiteValue.getString("httpPort");
                        }
                        if (webSiteValue.get("httpHost") != null) {
                            httpHost = webSiteValue.getString("httpHost");
                        }
                        if (webSiteValue.get("httpsPort") != null) {
                            httpsPort = webSiteValue.getString("httpsPort");
                        }
                        if (webSiteValue.get("httpsHost") != null) {
                            httpsHost = webSiteValue.getString("httpsHost");
                        }
                        if (webSiteValue.get("enableHttps") != null) {
                            enableHttps = webSiteValue.getBoolean("enableHttps");
                        }
                    }
                }
            }
            if (httpPort.isEmpty() && !request.isSecure()) {
                httpPort = String.valueOf(request.getServerPort());
            }
            if (httpHost.isEmpty()) {
                httpHost = request.getServerName();
            }
            if (httpsPort.isEmpty() && request.isSecure()) {
                httpsPort = String.valueOf(request.getServerPort());
            }
            if (httpsHost.isEmpty()) {
                httpsHost = request.getServerName();
            }
            
            if (ClassLoaderContainer.portOffset != 0) {
                Integer httpPortValue = Integer.valueOf(httpPort);
                httpPortValue += ClassLoaderContainer.portOffset;
                httpPort = httpPortValue.toString();
                Integer httpsPortValue = Integer.valueOf(httpsPort);
                httpsPortValue += ClassLoaderContainer.portOffset;
                httpsPort = httpsPortValue.toString();
            }                
            
            webSiteProps = new WebSiteProperties(httpPort, httpHost, httpsPort, httpsHost, enableHttps);
            request.setAttribute("_WEBSITE_PROPS_", webSiteProps);
        }
        return webSiteProps;
    }

    /**
     * Returns a <code>WebSiteProperties</code> instance initialized to the settings found
     * in the WebSite entity value.
     * 
     * @param webSiteValue
     */
    public static WebSiteProperties from(GenericValue webSiteValue) {
        Assert.notNull("webSiteValue", webSiteValue);
        if (!"WebSite".equals(webSiteValue.getEntityName())) {
            throw new IllegalArgumentException("webSiteValue is not a WebSite entity value");
        }
        WebSiteProperties defaults = new WebSiteProperties();
        String httpPort = (webSiteValue.get("httpPort") != null) ? webSiteValue.getString("httpPort") : defaults.getHttpPort();
        String httpHost = (webSiteValue.get("httpHost") != null) ? webSiteValue.getString("httpHost") : defaults.getHttpHost();
        String httpsPort = (webSiteValue.get("httpsPort") != null) ? webSiteValue.getString("httpsPort") : defaults.getHttpsPort();
        String httpsHost = (webSiteValue.get("httpsHost") != null) ? webSiteValue.getString("httpsHost") : defaults.getHttpsHost();
        boolean enableHttps = (webSiteValue.get("enableHttps") != null) ? webSiteValue.getBoolean("enableHttps") : defaults.getEnableHttps();

        if (ClassLoaderContainer.portOffset != 0) {
            Integer httpPortValue = Integer.valueOf(httpPort);
            httpPortValue += ClassLoaderContainer.portOffset;
            httpPort = httpPortValue.toString();
            Integer httpsPortValue = Integer.valueOf(httpsPort);
            httpsPortValue += ClassLoaderContainer.portOffset;
            httpsPort = httpsPortValue.toString();
        }                
        
        return new WebSiteProperties(httpPort, httpHost, httpsPort, httpsHost, enableHttps);
    }

    private final String httpPort;
    private final String httpHost;
    private final String httpsPort;
    private final String httpsHost;
    private final boolean enableHttps;

    private WebSiteProperties() {
        this.httpPort = UtilProperties.getPropertyValue("url.properties", "port.http");
        this.httpHost = UtilProperties.getPropertyValue("url.properties", "force.http.host");
        this.httpsPort = UtilProperties.getPropertyValue("url.properties", "port.https");
        this.httpsHost = UtilProperties.getPropertyValue("url.properties", "force.https.host");
        this.enableHttps = UtilProperties.propertyValueEqualsIgnoreCase("url.properties", "port.https.enabled", "Y");
    }

    private WebSiteProperties(String httpPort, String httpHost, String httpsPort, String httpsHost, boolean enableHttps) {
        this.httpPort = httpPort;
        this.httpHost = httpHost;
        this.httpsPort = httpsPort;
        this.httpsHost = httpsHost;
        this.enableHttps = enableHttps;
    }

    /**
     * Returns the configured http port, or an empty <code>String</code> if not configured.
     */
    public String getHttpPort() {
        return httpPort;
    }

    /**
     * Returns the configured http host, or an empty <code>String</code> if not configured.
     */
    public String getHttpHost() {
        return httpHost;
    }

    /**
     * Returns the configured https port, or an empty <code>String</code> if not configured.
     */
    public String getHttpsPort() {
        return httpsPort;
    }

    /**
     * Returns the configured https host, or an empty <code>String</code> if not configured.
     */
    public String getHttpsHost() {
        return httpsHost;
    }

    /**
     * Returns <code>true</code> if https is enabled.
     */
    public boolean getEnableHttps() {
        return enableHttps;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{httpPort=");
        sb.append(httpPort).append(", ");
        sb.append("httpHost=").append(httpHost).append(", ");
        sb.append("httpsPort=").append(httpsPort).append(", ");
        sb.append("httpsHost=").append(httpsHost).append(", ");
        sb.append("enableHttps=").append(enableHttps).append("}");
        return sb.toString();
    }
}
