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
package org.apache.ofbiz.webapp.ftl;

import java.io.IOException;
import java.io.Writer;
import java.net.URLEncoder;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.component.ComponentConfig.WebappInfo;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.template.FreeMarkerWorker;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.webapp.OfbizUrlBuilder;
import org.apache.ofbiz.webapp.WebAppUtil;
import org.apache.ofbiz.webapp.control.RequestHandler;

import freemarker.core.Environment;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateTransformModel;

/**
 * Freemarker Transform for creating OFBiz URLs (links).
 * <p>This transform accepts several arguments:</p>
 * <ul>
 * <li><b>fullPath</b> (true/false) - generate a full URL including scheme and host, defaults to false.</li>
 * <li><b>secure</b> (true/false) - generate a secure (https) URL, defaults to false. Server settings will
 * override this argument.</li>
 * <li><b>encode</b> (true/false) - encode the URL, defaults to true. Encoding is UTF-8.</li>
 * <li><b>webSiteId</b> - generate a full URL using the web site settings found in the WebSite entity.</li>
 * </ul>
 * <p>In addition, this transform accepts an environment variable - <b>urlPrefix</b>. If the variable
 * exists, it is prepended to the contents of the transform (the part between
 * <code>&lt;@ofbizUrl&gt;</code> and <code>&lt;/@ofbizUrl&gt;</code>), and all transform arguments are
 * ignored.</p>
 * 
 */
public class OfbizUrlTransform implements TemplateTransformModel {

    public final static String module = OfbizUrlTransform.class.getName();

    @SuppressWarnings("rawtypes")
    private static boolean checkBooleanArg(Map args, String key, boolean defaultValue) {
        Object o = args.get(key);
        if (o instanceof SimpleScalar) {
            SimpleScalar s = (SimpleScalar) o;
            return "true".equalsIgnoreCase(s.getAsString());
        }
        return defaultValue;
    }

    private static String convertToString(Object o) {
        String result = "";
        if (o != null) {
            if (Debug.verboseOn()) {
                 Debug.logVerbose("Arg Object : " + o.getClass().getName(), module);
            }
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

    @Override
    @SuppressWarnings("rawtypes")
    public Writer getWriter(final Writer out, Map args) {
        final StringBuilder buf = new StringBuilder();
        final boolean fullPath = checkBooleanArg(args, "fullPath", false);
        final boolean secure = checkBooleanArg(args, "secure", false);
        final boolean encode = checkBooleanArg(args, "encode", true);
        final String webSiteId = convertToString(args.get("webSiteId"));

        return new Writer(out) {

            @Override
            public void close() throws IOException {
                try {
                    Environment env = Environment.getCurrentEnvironment();
                    // Handle prefix.
                    String prefixString = convertToString(env.getVariable("urlPrefix"));
                    if (!prefixString.isEmpty()) {
                        String bufString = buf.toString();
                        boolean prefixSlash = prefixString.endsWith("/");
                        boolean bufSlash = bufString.startsWith("/");
                        if (prefixSlash && bufSlash) {
                            bufString = bufString.substring(1);
                        } else if (!prefixSlash && !bufSlash) {
                            bufString = "/" + bufString;
                        }
                        out.write(prefixString + bufString);
                        return;
                    }
                    HttpServletRequest request = FreeMarkerWorker.unwrap(env.getVariable("request"));
                    // Handle web site ID.
                    if (!webSiteId.isEmpty()) {
                        Delegator delegator = FreeMarkerWorker.unwrap(env.getVariable("delegator"));
                        if (request != null && delegator == null) {
                            delegator = (Delegator) request.getAttribute("delegator");
                        }
                        if (delegator == null) {
                            throw new IllegalStateException("Delegator not found");
                        }
                        WebappInfo webAppInfo = WebAppUtil.getWebappInfoFromWebsiteId(webSiteId);
                        StringBuilder newUrlBuff = new StringBuilder(250);
                        OfbizUrlBuilder builder = OfbizUrlBuilder.from(webAppInfo, delegator);
                        builder.buildFullUrl(newUrlBuff, buf.toString(), secure);
                        String newUrl = newUrlBuff.toString();
                        if (encode) {
                            newUrl = URLEncoder.encode(newUrl, "UTF-8");
                        }
                        out.write(newUrl);
                        return;
                    }
                    if (request != null) {
                        ServletContext ctx = request.getServletContext();
                        HttpServletResponse response = FreeMarkerWorker.unwrap(env.getVariable("response"));
                        String requestUrl = buf.toString();
                        RequestHandler rh = (RequestHandler) ctx.getAttribute("_REQUEST_HANDLER_");
                        out.write(rh.makeLink(request, response, requestUrl, fullPath, secure, encode));
                    } else {
                        out.write(buf.toString());
                    }
                } catch (Exception e) {
                    Debug.logWarning(e, "Exception thrown while running ofbizUrl transform", module);
                    throw new IOException(e);
                }
            }

            @Override
            public void flush() throws IOException {
                out.flush();
            }

            @Override
            public void write(char cbuf[], int off, int len) {
                buf.append(cbuf, off, len);
            }
        };
    }
}
