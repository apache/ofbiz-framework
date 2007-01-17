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
