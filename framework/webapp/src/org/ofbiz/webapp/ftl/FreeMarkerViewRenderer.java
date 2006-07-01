/*
 * $Id: FreeMarkerViewRenderer.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2003 The Open For Business Project - www.ofbiz.org
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
package org.ofbiz.webapp.ftl;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import freemarker.ext.beans.BeansWrapper;
import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.WrappingTemplateModel;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilHttp;

import org.jpublish.JPublishContext;
import org.jpublish.Page;
import org.jpublish.SiteContext;
import org.jpublish.page.PageInstance;
import org.jpublish.view.ViewRenderException;

/**
 * JPublish View Renderer For Freemarker Template Engine
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.1
 */
public class FreeMarkerViewRenderer extends org.jpublish.view.freemarker.FreeMarkerViewRenderer {

    public static final String module = FreeMarkerViewRenderer.class.getName();

    public void init() throws Exception {
        super.init();
        //TODO: find some way of getting the site identifier... hmmm...
        String id = "unknown";
        fmConfig.setCacheStorage(new OfbizCacheStorage(id));
        fmConfig.setSetting("datetime_format", "yyyy-MM-dd HH:mm:ss.SSS");
    }

    protected Object createViewContext(JPublishContext context, String path) throws ViewRenderException {
        HttpServletRequest request = context.getRequest();
        HttpServletResponse response = context.getResponse();

        BeansWrapper wrapper = BeansWrapper.getDefaultInstance();
        WrappingTemplateModel.setDefaultObjectWrapper(wrapper);
        Map contextMap = new HashMap();
        SimpleHash root = new SimpleHash(wrapper);
        try {
            Object[] keys = context.getKeys();
            for (int i = 0; i < keys.length; i++) {
                String key = (String) keys[i];
                Object value = context.get(key);
                if (value != null) {
                    contextMap.put(key, value);
                    //no longer wrapping; let FM do it if needed, more efficient
                    //root.put(key, wrapper.wrap(value));
                    root.put(key, value);
                }
            }
            root.put("context", wrapper.wrap(contextMap));
            root.put("cachedInclude", new JpCacheIncludeTransform()); // only adding this in for JP! 
            //root.put("jpublishContext", wrapper.wrap(context));
            FreeMarkerViewHandler.prepOfbizRoot(root, request, response);
        } catch (Exception e) {
            throw new ViewRenderException(e);
        }
        return root;
    }

    public void render(JPublishContext context, String path, Reader in, Writer out) throws IOException, ViewRenderException {
        try {
            Page page = (Page) context.get(JPublishContext.JPUBLISH_PAGE);
            Object viewContext = createViewContext(context, path);

            Template template = fmConfig.getTemplate(path, UtilHttp.getLocale(context.getRequest()));
            template.setObjectWrapper(BeansWrapper.getDefaultInstance());

            /* NEVER add content to the beginning of templates; this effects XML processing which requires the
               first line remain intact.                 
            boolean showTemplateId = UtilProperties.propertyValueEquals("content", "freemarker.showTemplateId", "Y");
            String templateIdPrefix = UtilProperties.getPropertyValue("content", "freemarker.templateIdPrefix", "[system]");
            if (showTemplateId) {
                out.write("\n<!-- " + templateIdPrefix + " begin: " + template.getName() + " -->\n");
            }
            */

            template.process(viewContext, out);

            /*
            if (showTemplateId) {
                out.write("\n<!-- " + templateIdPrefix + " end: " + template.getName() + " -->\n");
            }
            */
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            Debug.logError(e, "Exception from FreeMarker", module);
            throw new ViewRenderException(e);
        }
    }

    private Page getPage(String path, JPublishContext context) {
        Page page = null;
        try {
            SiteContext siteContext = (SiteContext) context.get("site");
            PageInstance pi = siteContext.getPageManager().getPage(path.substring(path.lastIndexOf(":") + 1));
            if (pi != null)
                page = new Page(pi);
        } catch (Exception e) {
        }
        return page;
    }

}
