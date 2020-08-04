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
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.product.category.SeoConfigUtil;
import org.apache.ofbiz.webapp.control.RequestHandler;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Matcher;

import freemarker.core.Environment;
import freemarker.ext.beans.BeanModel;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateTransformModel;

/**
 * SeoTransform - Freemarker Transform for URLs (links)
 *
 */
public class SeoTransform implements TemplateTransformModel {

    private static final String MODULE = SeoTransform.class.getName();

    public boolean checkArg(Map<?, ?> args, String key, boolean defaultValue) {
        if (!args.containsKey(key)) {
            return defaultValue;
        }
        Object o = args.get(key);
        if (o instanceof SimpleScalar) {
            SimpleScalar s = (SimpleScalar) o;
            return "true".equalsIgnoreCase(s.getAsString());
        }
        return defaultValue;
    }

    @Override
    public Writer getWriter(Writer out, @SuppressWarnings("rawtypes") Map args) {
        final StringBuffer buf = new StringBuffer();
        final boolean fullPath = checkArg(args, "fullPath", false);
        final boolean secure = checkArg(args, "secure", false);
        final boolean encode = checkArg(args, "encode", true);

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
                    if (req != null) {
                        HttpServletRequest request = (HttpServletRequest) req.getWrappedObject();
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

                        RequestHandler rh = RequestHandler.from(request);
                        out.write(seoUrl(rh.makeLink(request, response, buf.toString(), fullPath, secure, encode), userLogin == null));
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
                } catch (IOException | TemplateModelException e) {
                    throw new IOException(e.getMessage());
                }
            }
        };
    }

    /**
     * Transform a url according to seo pattern regular expressions.
     *
     * @param url, String to do the seo transform
     * @param isAnon, boolean to indicate whether it's an anonymous visit.
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
                        }
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
                if (Debug.verboseOn()) {
                    Debug.logVerbose("Can NOT find a seo transform pattern for this url: " + url, MODULE);
                }
            }
        }
        return url;
    }

    static {
        if (!SeoConfigUtil.isInitialed()) {
            SeoConfigUtil.init();
        }
    }
}
