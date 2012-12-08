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
package org.ofbiz.webapp.ftl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;

import org.ofbiz.base.component.ComponentConfig;
import org.ofbiz.base.component.ComponentConfig.WebappInfo;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.webapp.control.RequestHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import freemarker.core.Environment;
import freemarker.ext.beans.BeanModel;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateTransformModel;

/**
 * OfbizUrlTransform - Freemarker Transform for URLs (links)
 */
public class OfbizUrlTransform implements TemplateTransformModel {

    public final static String module = OfbizUrlTransform.class.getName();

    @SuppressWarnings("unchecked")
    public boolean checkArg(Map args, String key, boolean defaultValue) {
        if (!args.containsKey(key)) {
            return defaultValue;
        } else {
            Object o = args.get(key);
            if (o instanceof SimpleScalar) {
                SimpleScalar s = (SimpleScalar) o;
                return "true".equalsIgnoreCase(s.getAsString());
            }
            return defaultValue;
        }
    }

    @SuppressWarnings("unchecked")
    public Writer getWriter(final Writer out, Map args) {
        final StringBuilder buf = new StringBuilder();
        final boolean fullPath = checkArg(args, "fullPath", false);
        final boolean secure = checkArg(args, "secure", false);
        final boolean encode = checkArg(args, "encode", true);
        final String webSiteId = getArg(args, "webSiteId");

        return new Writer(out) {
            @Override
            public void write(char cbuf[], int off, int len) {
                buf.append(cbuf, off, len);
            }

            @Override
            public void flush() throws IOException {
                out.flush();
            }

            @Override
            public void close() throws IOException {
                try {
                    Environment env = Environment.getCurrentEnvironment();
                    BeanModel req = (BeanModel) env.getVariable("request");
                    BeanModel res = (BeanModel) env.getVariable("response");
                    Object prefix = env.getVariable("urlPrefix");
                    if (UtilValidate.isNotEmpty(webSiteId)) {
                        HttpServletRequest request = (HttpServletRequest) req.getWrappedObject();
                        Delegator delegator = (Delegator) request.getAttribute("delegator");
                        String httpsPort = null;
                        String httpsServer = null;
                        String httpPort = null;
                        String httpServer = null;
                        Boolean enableHttps = null;
                        StringBuilder newURL = new StringBuilder();
                        // make prefix url
                        try {
                            GenericValue webSite = delegator.findOne("WebSite", UtilMisc.toMap("webSiteId", webSiteId), true);
                            if (webSite != null) {
                                httpsPort = webSite.getString("httpsPort");
                                httpsServer = webSite.getString("httpsHost");
                                httpPort = webSite.getString("httpPort");
                                httpServer = webSite.getString("httpHost");
                                enableHttps = webSite.getBoolean("enableHttps");
                            }
                        } catch (GenericEntityException e) {
                            Debug.logWarning(e, "Problems with WebSite entity; using global defaults", module);
                        }
                        // fill in any missing properties with fields from the global file
                        if (UtilValidate.isEmpty(httpsPort)) {
                            httpsPort = UtilProperties.getPropertyValue("url.properties", "port.https", "443");
                        }
                        if (UtilValidate.isEmpty(httpsServer)) {
                            httpsServer = UtilProperties.getPropertyValue("url.properties", "force.https.host");
                        }
                        if (UtilValidate.isEmpty(httpPort)) {
                            httpPort = UtilProperties.getPropertyValue("url.properties", "port.http", "80");
                        }
                        if (UtilValidate.isEmpty(httpServer)) {
                            httpServer = UtilProperties.getPropertyValue("url.properties", "force.http.host");
                        }
                        if (enableHttps == null) {
                            enableHttps = UtilProperties.propertyValueEqualsIgnoreCase("url.properties", "port.https.enabled", "Y");
                        }
                        if (secure && enableHttps) {
                            String server = httpsServer;
                            if (UtilValidate.isEmpty(server)) {
                                server = request.getServerName();
                            }
                            newURL.append("https://");
                            newURL.append(httpsServer);
                            newURL.append(":").append(httpsPort);
                        } else {
                            newURL.append("http://");
                            newURL.append(httpServer);
                            if (!"80".equals(httpPort)) {
                                newURL.append(":").append(httpPort);
                            }
                        }
                        // make mount point
                        String mountPoint = null;
                        for (WebappInfo webAppInfo : ComponentConfig.getAllWebappResourceInfos()) {
                            File file = new File(webAppInfo.getLocation() + "/WEB-INF/web.xml");
                            if (!file.exists()) {
                                continue;
                            }
                            InputStream is = new FileInputStream(file);
                            try {
                                Document doc = UtilXml.readXmlDocument(is, true, null);
                                NodeList nList = doc.getElementsByTagName("context-param");
                                for (int temp = 0; temp < nList.getLength(); temp++) {
                                    Node nNode = nList.item(temp);
                                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                                        Element eElement = (Element) nNode;
                                        String paramName = getTagValue("param-name",eElement);
                                        String paramValue = getTagValue("param-value",eElement);
                                        if ("webSiteId".equals(paramName) && webSiteId.equals(paramValue)) {
                                            mountPoint = webAppInfo.getContextRoot();
                                            break;
                                        }
                                    }
                                }
                            } catch (SAXException e) {
                                Debug.logWarning(e, e.getMessage(), module);
                            } catch (ParserConfigurationException e) {
                                Debug.logWarning(e, e.getMessage(), module);
                            }
                            if (UtilValidate.isNotEmpty(mountPoint)) {
                            if (mountPoint.length() > 1) newURL.append(mountPoint);
                                break;
                            }
                        }
                        // make the path the the control servlet
                        String controlPath = (String) request.getAttribute("_CONTROL_PATH_");
                        String[] patch = controlPath.split("/");
                        String patchStr = null;
                        if (patch.length > 0) {
                        patchStr = patch[patch.length-1];
                        }
                        if (UtilValidate.isNotEmpty(patchStr)) {
                        newURL.append("/");
                        newURL.append(patchStr);
                        }
                        newURL.append("/");
                        // make requestUrl
                        String requestUrl = buf.toString();
                        newURL.append(requestUrl);
                        out.write(newURL.toString());
                    } else if (req != null) {
                        HttpServletRequest request = (HttpServletRequest) req.getWrappedObject();
                        ServletContext ctx = (ServletContext) request.getAttribute("servletContext");
                        HttpServletResponse response = null;
                        if (res != null) {
                            response = (HttpServletResponse) res.getWrappedObject();
                        }

                        String requestUrl = buf.toString();

                        // make the link
                        RequestHandler rh = (RequestHandler) ctx.getAttribute("_REQUEST_HANDLER_");
                        out.write(rh.makeLink(request, response, requestUrl, fullPath, secure, encode));
                    } else if (prefix != null) {
                        if (prefix instanceof TemplateScalarModel) {
                            TemplateScalarModel s = (TemplateScalarModel) prefix;
                            String prefixString = s.getAsString();
                            String bufString = buf.toString();
                            boolean prefixSlash = prefixString.endsWith("/");
                            boolean bufSlash = bufString.startsWith("/");
                            if (prefixSlash && bufSlash) {
                                bufString = bufString.substring(1);
                            } else if (!prefixSlash && !bufSlash) {
                                bufString = "/" + bufString;
                            }
                            out.write(prefixString + bufString);
                        }
                    } else {
                        out.write(buf.toString());
                    }
                } catch (TemplateModelException e) {
                    throw new IOException(e.getMessage());
                }
            }
        };
    }
    private static String getArg(Map args, String key) {
        String  result = "";
        Object o = args.get(key);
        if (o != null) {
            if (Debug.verboseOn()) Debug.logVerbose("Arg Object : " + o.getClass().getName(), module);
            if (o instanceof TemplateScalarModel) {
                TemplateScalarModel s = (TemplateScalarModel) o;
                try {
                    result = s.getAsString();
                } catch (TemplateModelException e) {
                    Debug.logError(e, "Template Exception", module);
                }
            } else {
              result = o.toString();
            }
        }
        return result;
    }
    private static String getTagValue(String sTag, Element eElement){
    String value = "";
        try{
            NodeList nlList= eElement.getElementsByTagName(sTag).item(0).getChildNodes();
            Node nValue = nlList.item(0);
            return value = nValue.getNodeValue();
        } catch (Exception e) {
            return value;
        }
    }
}
