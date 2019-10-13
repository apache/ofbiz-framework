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
package org.apache.ofbiz.product.category.ftl;

import java.io.IOException;
import java.io.Writer;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.ofbiz.base.component.ComponentConfig;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.template.FreeMarkerWorker;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.product.category.SeoConfigUtil;
import org.apache.ofbiz.webapp.OfbizUrlBuilder;
import org.apache.ofbiz.webapp.WebAppUtil;
import org.apache.ofbiz.webapp.control.RequestHandler;
import org.apache.ofbiz.webapp.control.WebAppConfigurationException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Matcher;
import org.xml.sax.SAXException;

import freemarker.core.Environment;
import freemarker.ext.beans.BeanModel;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateTransformModel;

/**
 * UrlRegexpTransform - Freemarker Transform for Products URLs (links)
 * 
 */
public class UrlRegexpTransform implements TemplateTransformModel {

    private static final String module = UrlRegexpTransform.class.getName();


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

    public boolean checkArg(Map<?, ?> args, String key, boolean defaultValue) {
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

    public Writer getWriter(final Writer out, Map args) {
        final StringBuffer buf = new StringBuffer();
        final boolean fullPath = checkArg(args, "fullPath", false);
        final boolean secure = checkArg(args, "secure", false);
        final boolean encode = checkArg(args, "encode", true);
        final String webSiteId = convertToString(args.get("webSiteId"));

        return new Writer(out) {

            public void write(char cbuf[], int off, int len) {
                buf.append(cbuf, off, len);
            }

            public void flush() throws IOException {
                out.flush();
            }

            public void close() throws IOException {
                try {
                    Environment env = Environment.getCurrentEnvironment();
                    BeanModel req = (BeanModel) env.getVariable("request");
                    BeanModel res = (BeanModel) env.getVariable("response");
                    Object prefix = env.getVariable("urlPrefix");
                    if (req != null) {
                        HttpServletRequest request = (HttpServletRequest) req.getWrappedObject();
                        ServletContext ctx = (ServletContext) request.getAttribute("servletContext");
                        HttpServletResponse response = null;
                        if (res != null) {
                            response = (HttpServletResponse) res.getWrappedObject();
                        }
                        HttpSession session = request.getSession();
                        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");

                        // anonymous shoppers are not logged in
                        if (userLogin != null && "anonymous".equals(userLogin.getString("userLoginId"))) {
                            userLogin = null;
                        }

                        RequestHandler rh = (RequestHandler) ctx.getAttribute("_REQUEST_HANDLER_");
                        out.write(seoUrl(rh.makeLink(request, response, buf.toString(), fullPath, secure || request.isSecure() , encode), userLogin == null));
                    } else if (!webSiteId.isEmpty()) {
                        Delegator delegator = FreeMarkerWorker.unwrap(env.getVariable("delegator"));
                        if (delegator == null) {
                            throw new IllegalStateException("Delegator not found");
                        }
                        ComponentConfig.WebappInfo webAppInfo = WebAppUtil.getWebappInfoFromWebsiteId(webSiteId);
                        StringBuilder newUrlBuff = new StringBuilder(250);
                        OfbizUrlBuilder builder = OfbizUrlBuilder.from(webAppInfo, delegator);
                        builder.buildFullUrl(newUrlBuff, buf.toString(), secure);
                        String newUrl = newUrlBuff.toString();
                        if (encode) {
                            newUrl = URLEncoder.encode(newUrl, "UTF-8");
                        }
                        out.write(newUrl);
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
                } catch (IOException |
                        SAXException |
                        TemplateModelException |
                        GenericEntityException |
                        WebAppConfigurationException e) {
                    throw new IOException(e.getMessage());
                }
            }
        };
    }

    /**
     * Transform a url according to seo pattern regular expressions.
     * 
     * @param url
     *            , String to do the seo transform
     * @param isAnon
     *            , boolean to indicate whether it's an anonymous visit.
     * 
     * @return String, the transformed url.
     */
    public static String seoUrl(String url, boolean isAnon) {
        Perl5Matcher matcher = new Perl5Matcher();
        if (SeoConfigUtil.checkUseUrlRegexp() && matcher.matches(url, SeoConfigUtil.getGeneralRegexpPattern())) {
            Iterator<String> keys = SeoConfigUtil.getSeoPatterns().keySet().iterator();
            boolean foundMatch = false;
            while (keys.hasNext()) {
                String key = keys.next();
                Pattern pattern = SeoConfigUtil.getSeoPatterns().get(key);
                if (pattern.getPattern().contains(";jsessionid=")) {
                    if (isAnon) {
                        if (SeoConfigUtil.isJSessionIdAnonEnabled()) {
                            continue;
                        }
                    } else {
                        if (SeoConfigUtil.isJSessionIdUserEnabled()) {
                            continue;
                        } else {
                            boolean foundException = false;
                            for (int i = 0; i < SeoConfigUtil.getUserExceptionPatterns().size(); i++) {
                                if (matcher.matches(url, SeoConfigUtil.getUserExceptionPatterns().get(i))) {
                                    foundException = true;
                                    break;
                                }
                            }
                            if (foundException) {
                                continue;
                            }
                        }
                    }
                }
                String replacement = SeoConfigUtil.getSeoReplacements().get(key);
                if (matcher.matches(url, pattern)) {
                    for (int i = 1; i < matcher.getMatch().groups(); i++) {
                        replacement = replacement.replaceAll("\\$" + i, matcher.getMatch().group(i));
                    }
                    // break if found any matcher
                    url = replacement;
                    foundMatch = true;
                    break;
                }
            }
            if (!foundMatch) {
                if (Debug.verboseOn()) Debug.logVerbose("Can NOT find a seo transform pattern for this url: " + url, module);
            }
        }
        return url;
    }

    static {
        SeoConfigUtil.init();
    }

    /**
     * Forward a uri according to forward pattern regular expressions. Note: this is developed for Filter usage.
     * 
     * @param uri
     *            String to reverse transform
     * @return String
     */
    public static boolean forwardUri(HttpServletResponse response, String uri) {
        Perl5Matcher matcher = new Perl5Matcher();
        boolean foundMatch = false;
        Integer responseCodeInt = null;
        if (SeoConfigUtil.checkUseUrlRegexp() && SeoConfigUtil.getSeoPatterns() != null && SeoConfigUtil.getForwardReplacements() != null) {
            Iterator<String> keys = SeoConfigUtil.getSeoPatterns().keySet().iterator();
            while (keys.hasNext()) {
                String key = keys.next();
                Pattern pattern = SeoConfigUtil.getSeoPatterns().get(key);
                String replacement = SeoConfigUtil.getForwardReplacements().get(key);
                if (matcher.matches(uri, pattern)) {
                    for (int i = 1; i < matcher.getMatch().groups(); i++) {
                        replacement = replacement.replaceAll("\\$" + i, matcher.getMatch().group(i));
                    }
                    // break if found any matcher
                    uri = replacement;
                    responseCodeInt = SeoConfigUtil.getForwardResponseCodes().get(key);
                    foundMatch = true;
                    break;
                }
            }
        }
        if (foundMatch) {
            if (responseCodeInt == null) {
                response.setStatus(SeoConfigUtil.getDefaultResponseCode());
            } else {
                response.setStatus(responseCodeInt.intValue());
            }
            response.setHeader("Location", uri);
        } else {
            Debug.logInfo("Can NOT forward this url: " + uri, module);
        }
        return foundMatch;
    }
}
