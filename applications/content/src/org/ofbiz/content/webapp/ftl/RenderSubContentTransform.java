/*
 * $Id: RenderSubContentTransform.java 5462 2005-08-05 18:35:48Z jonesde $
 * 
 * Copyright (c) 2001-2003 The Open For Business Project - www.ofbiz.org
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *  
 */
package org.ofbiz.content.webapp.ftl;

import java.io.IOException;
import java.io.Writer;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.template.FreeMarkerWorker;
import org.ofbiz.content.content.ContentWorker;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;

import freemarker.core.Environment;
import freemarker.template.TemplateTransformModel;

/**
 * RenderSubContentTransform - Freemarker Transform for Content rendering
 *
 * @author <a href="mailto:byersa@automationgroups.com">Al Byers</a>
 * @version $Rev$
 * @since 3.0
 *
 * This transform cannot be called recursively (at this time).
 */
public class RenderSubContentTransform implements TemplateTransformModel {

    public static final String module = RenderSubContentTransform.class.getName();

    /**
     * Does a conditional search to return a value for a parameter with the passed name. Looks first to see if it was passed as an argument to the transform.
     * Secondly, it looks to see if it is passed as a parameter in the template context object.
     * <p/>
     * Note that this is different from the getArg method of EditRenderDataResourceTransform, which checks the request object instead of the template context
     * object.
     */
    public static String getArg(Map args, String key, Environment env) {
        return FreeMarkerWorker.getArg(args, key, env);
    }

    public static String getArg(Map args, String key, Map ctx) {
        return FreeMarkerWorker.getArg(args, key, ctx);
    }

    public Writer getWriter(final Writer out, Map args) {
        //final StringBuffer buf = new StringBuffer();
        final Environment env = Environment.getCurrentEnvironment();
        Map ctx = (Map) FreeMarkerWorker.getWrappedObject("context", env);
        if (ctx == null) {
            ctx = new HashMap();
        }
        final String mapKey = getArg(args, "mapKey", ctx);
        final String subContentId = getArg(args, "subContentId", ctx);
        final String subDataResourceTypeId = getArg(args, "subDataResourceTypeId", ctx);
        final String contentId = getArg(args, "contentId", ctx);
        final String mimeTypeId = getArg(args, "mimeTypeId", ctx);
        final Locale locale = (Locale) FreeMarkerWorker.getWrappedObject("locale", env);
        final HttpServletRequest request = (HttpServletRequest) FreeMarkerWorker.getWrappedObject("request", env);
        final GenericDelegator delegator = (GenericDelegator) FreeMarkerWorker.getWrappedObject("delegator", env);
        final GenericValue userLogin = (GenericValue) FreeMarkerWorker.getWrappedObject("userLogin", env);
        GenericValue subContentDataResourceViewTemp = (GenericValue) FreeMarkerWorker.getWrappedObject("subContentDataResourceView", env);
        if (subContentDataResourceViewTemp == null) {
            List assocTypes = UtilMisc.toList("SUB_CONTENT");
            Timestamp fromDate = UtilDateTime.nowTimestamp();
            try {
                subContentDataResourceViewTemp = ContentWorker.getSubContent(delegator, contentId, mapKey, subContentId, userLogin, assocTypes, fromDate);
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage());
            }
        }

        final GenericValue subContentDataResourceView = subContentDataResourceViewTemp;


        final Map templateContext = ctx;

        return new Writer(out) {

            public void write(char cbuf[], int off, int len) {
            }

            public void flush() throws IOException {
                out.flush();
            }

            public void close() throws IOException {
                try {
                    renderSubContent();
                } catch (IOException e) {
                    throw new IOException(e.getMessage());
                }
            }

            public void renderSubContent() throws IOException {
                //TemplateHashModel dataRoot = env.getDataModel();
                Timestamp fromDate = UtilDateTime.nowTimestamp();
                ServletContext servletContext = request.getSession().getServletContext();
                String rootDir = servletContext.getRealPath("/");
                String webSiteId = (String) servletContext.getAttribute("webSiteId");
                String https = (String) servletContext.getAttribute("https");
                templateContext.put("webSiteId", webSiteId);
                templateContext.put("https", https);
                templateContext.put("rootDir", rootDir);

                Map templateRoot = FreeMarkerWorker.createEnvironmentMap(env);

                templateRoot.put("context", templateContext);
                if (subContentDataResourceView != null) {
                }
                try {
                    Map results = ContentWorker.renderSubContentAsText(delegator, contentId, out, mapKey, subContentId, subContentDataResourceView, templateRoot, locale, mimeTypeId, userLogin, fromDate);
                } catch (GeneralException e) {
                    Debug.logError(e, "Error rendering content", module);
                    throw new IOException("Error rendering content" + e.toString());
                }

                Map resultCtx = (Map) FreeMarkerWorker.getWrappedObject("context", env);
                templateContext.put("mapKey", null);
                templateContext.put("subContentId", null);
                templateContext.put("subDataResourceTypeId", null);
                templateContext.put("contentId", contentId);
                templateContext.put("mimeTypeId", null);
                templateContext.put("locale", locale);

                return;
            }
        };
    }

}
