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
package org.apache.ofbiz.webapp.website;

import javax.servlet.http.HttpServletRequest;

import org.apache.ofbiz.base.lang.ThreadSafe;
import org.apache.ofbiz.base.start.Start;
import org.apache.ofbiz.base.util.Assert;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtilProperties;

/**
 * Web site properties.
 */
@ThreadSafe
public final class WebSiteProperties {

    private final String httpPort;
    private final String httpHost;
    private final String httpsPort;
    private final String httpsHost;
    private final String webappPath;
    private final boolean enableHttps;

    private WebSiteProperties(String httpPort, String httpHost, String httpsPort, String httpsHost, String webappPath, boolean enableHttps) {
        this.httpPort = httpPort;
        this.httpHost = httpHost;
        this.httpsPort = httpsPort;
        this.httpsHost = httpsHost;
        this.webappPath = webappPath;
        this.enableHttps = enableHttps;
    }

    private WebSiteProperties(Delegator delegator) {
        this(
                EntityUtilProperties.getPropertyValue("url", "port.http", delegator),
                EntityUtilProperties.getPropertyValue("url", "force.http.host", delegator),
                EntityUtilProperties.getPropertyValue("url", "port.https", delegator),
                EntityUtilProperties.getPropertyValue("url", "force.https.host", delegator),
                null,
                EntityUtilProperties.propertyValueEqualsIgnoreCase("url", "port.https.enabled", "Y", delegator));
    }

    /**
     * Returns a <code>WebSiteProperties</code> instance initialized to the settings found
     * in the <code>url.properties</code> file.
     */
    public static WebSiteProperties defaults(Delegator delegator) {
        return new WebSiteProperties(delegator);
    }

    /**
     * Returns a <code>WebSiteProperties</code> instance initialized to the settings found
     * in the application's WebSite entity value. If the application does not have a
     * WebSite entity value then the instance is initialized to the settings found
     * in the <code>url.properties</code> file.
     * @param request
     * @throws GenericEntityException
     */
    public static WebSiteProperties from(HttpServletRequest request) throws GenericEntityException {
        Assert.notNull("request", request);
        WebSiteProperties webSiteProps = (WebSiteProperties) request.getAttribute("_WEBSITE_PROPS_");
        if (webSiteProps == null) {
            Boolean addPortoffset = true;
            Delegator delegator = (Delegator) request.getAttribute("delegator");
            if (delegator != null) {
                String webSiteId = WebSiteWorker.getWebSiteId(request);
                if (webSiteId != null) {
                    GenericValue webSiteValue = EntityQuery.use(delegator).from("WebSite").where("webSiteId", webSiteId).cache().queryOne();
                    if (webSiteValue != null) {
                        webSiteProps = WebSiteProperties.from(webSiteValue);
                    }
                }
            }
            if (webSiteProps == null) {
                webSiteProps = new WebSiteProperties(delegator);
            }
            if (webSiteProps.getHttpPort().isEmpty() && !request.isSecure()) {
                webSiteProps = webSiteProps.updateHttpPort(String.valueOf(request.getServerPort()));
            }
            if (webSiteProps.getHttpHost().isEmpty()) {
                webSiteProps = webSiteProps.updateHttpHost(request.getServerName());
            }
            if (webSiteProps.getHttpsPort().isEmpty() && request.isSecure()) {
                webSiteProps = webSiteProps.updateHttpsPort(String.valueOf(request.getServerPort()));
                addPortoffset = false; // We take the port from the request, don't add the portOffset
            }
            if (webSiteProps.getHttpsHost().isEmpty()) {
                webSiteProps = webSiteProps.updateHttpsHost(request.getServerName());
            }
            webSiteProps = webSiteProps.addPortOffset(addPortoffset);
            request.setAttribute("_WEBSITE_PROPS_", webSiteProps);
        }
        return webSiteProps;
    }

    /**
     * Returns a <code>WebSiteProperties</code> instance initialized to the settings found
     * in the WebSite entity value.
     * @param webSiteValue
     */
    public static WebSiteProperties from(GenericValue webSiteValue) {
        Assert.notNull("webSiteValue", webSiteValue);
        if (!"WebSite".equals(webSiteValue.getEntityName())) {
            throw new IllegalArgumentException("webSiteValue is not a WebSite entity value");
        }
        WebSiteProperties defaults = new WebSiteProperties(webSiteValue.getDelegator());
        if (webSiteValue.get("httpPort") != null) {
            defaults = defaults.updateHttpPort(webSiteValue.getString("httpPort"));
        }
        if (webSiteValue.get("httpHost") != null) {
            defaults = defaults.updateHttpHost(webSiteValue.getString("httpHost"));
        }
        if (webSiteValue.get("httpsPort") != null) {
            defaults = defaults.updateHttpsPort(webSiteValue.getString("httpsPort"));
        }
        if (webSiteValue.get("httpsHost") != null) {
            defaults = defaults.updateHttpsHost(webSiteValue.getString("httpsHost"));
        }
        if (webSiteValue.get("webappPath") != null) {
            defaults = defaults.updateWebappPath(webSiteValue.getString("webappPath"));
        }
        if (webSiteValue.get("enableHttps") != null) {
            defaults = defaults.updateEnableHttps(webSiteValue.getBoolean("enableHttps"));
        }
        // Here unlike above we trust the user and don't rely on the request, so addPortoffset
        defaults = defaults.addPortOffset(true);
        return defaults;
    }

    /**
     * Returns a <code>WebSiteProperties</code> instance offset by the portOffset
     * that is defined in properties. Offset of https is optionally and only done
     * when addHttpsOffset is <code>true</code>
     *
     * @param addHttpsOffset
     * @return
     */
    private WebSiteProperties addPortOffset(Boolean addHttpsOffset) {
        int portOffset = Start.getInstance().getConfig().getPortOffset();
        String newHttpPort = this.httpPort;
        String newHttpsPort = this.httpsPort;
        if (portOffset != 0) {
            newHttpPort = addPortOffset(newHttpPort, portOffset);
            if (addHttpsOffset) {
                newHttpsPort = addPortOffset(newHttpsPort, portOffset);
            }
        }
        return new WebSiteProperties(newHttpPort, this.httpHost, newHttpsPort, this.httpsHost, this.webappPath, this.enableHttps);
    }

    private String addPortOffset(String port, int offset) {
        Integer value = Integer.valueOf(port);
        value += offset;
        return value.toString();
    }

    private WebSiteProperties updateHttpPort(String newHttpPort) {
        return new WebSiteProperties(newHttpPort, this.httpHost, this.httpsPort, this.httpsHost, this.webappPath, this.enableHttps);
    }

    private WebSiteProperties updateHttpHost(String newHttpHost) {
        return new WebSiteProperties(this.httpPort, newHttpHost, this.httpsPort, this.httpsHost, this.webappPath, this.enableHttps);
    }

    private WebSiteProperties updateHttpsPort(String newHttpsPort) {
        return new WebSiteProperties(this.httpPort, this.httpHost, newHttpsPort, this.httpsHost, this.webappPath, this.enableHttps);
    }

    private WebSiteProperties updateHttpsHost(String newHttpsHost) {
        return new WebSiteProperties(this.httpPort, this.httpHost, this.httpsPort, newHttpsHost, this.webappPath, this.enableHttps);
    }

    private WebSiteProperties updateWebappPath(String newWebappPath) {
        if (newWebappPath != null && newWebappPath.endsWith("/")) {
            newWebappPath = newWebappPath.substring(0, newWebappPath.length() - 1);
        }
        return new WebSiteProperties(this.httpPort, this.httpHost, this.httpsPort, this.httpsHost, newWebappPath, this.enableHttps);
    }

    private WebSiteProperties updateEnableHttps(boolean newEnableHttps) {
        return new WebSiteProperties(this.httpPort, this.httpHost, this.httpsPort, this.httpsHost, this.webappPath, newEnableHttps);
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

    /**
     * Returns the configured webapp path on website linked to webapp or null.
     */
    public String getWebappPath() {
        return webappPath;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{httpPort=");
        sb.append(httpPort).append(", ");
        sb.append("httpHost=").append(httpHost).append(", ");
        sb.append("httpsPort=").append(httpsPort).append(", ");
        sb.append("httpsHost=").append(httpsHost).append(", ");
        sb.append("webappPath=").append(webappPath).append(", ");
        sb.append("enableHttps=").append(enableHttps).append("}");
        return sb.toString();
    }
}
