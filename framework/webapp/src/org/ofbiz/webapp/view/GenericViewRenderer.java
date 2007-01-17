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
package org.ofbiz.webapp.view;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.anthonyeden.lib.config.Configuration;
import com.anthonyeden.lib.config.ConfigurationException;
import com.anthonyeden.lib.config.XMLConfiguration;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.security.Security;

import org.jpublish.JPublishContext;
import org.jpublish.Page;
import org.jpublish.SiteContext;
import org.jpublish.Template;
import org.jpublish.page.PageInstance;
import org.jpublish.view.ViewRenderException;
import org.jpublish.view.ViewRenderer;

/**
 * Generic JPublish View Renderer - This is in testing; for use in wrapping other renderers
 */
public class GenericViewRenderer implements ViewRenderer {
        
    public static final String module = GenericViewRenderer.class.getName();  
    public static final String DEFAULT_RENDERER = "freemarker";
    public Map renderers = null;      
        
    protected SiteContext siteContext = null;

    /**
     * @see org.jpublish.view.ViewRenderer#setSiteContext(org.jpublish.SiteContext)
     */
    public void setSiteContext(SiteContext siteContext) {
        this.siteContext = siteContext;
        this.renderers = new HashMap();
        try {
            loadCustom();
        } catch (Exception e) {
            Debug.logError(e, "Problems loading custom settings", module);
            throw new RuntimeException(e.toString());
        }                
    }

    /* (non-Javadoc)
     * @see org.jpublish.view.ViewRenderer#init()
     */
    public void init() throws Exception {                         
    }

    /**
     * @see org.jpublish.view.ViewRenderer#render(org.jpublish.JPublishContext, java.io.Reader, java.io.Writer)
     */
    public void render(JPublishContext context, String path, Reader in, Writer out) throws IOException, ViewRenderException {
        HttpServletRequest request = context.getRequest();
        HttpSession session = context.getSession();
        Security security = (Security) request.getAttribute("security");
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        
        Page parent = (Page) context.get("page");                       
        Page page = getPage(path);
            
        // decorate the content w/ edit images if we have permission              
        if (userLogin != null && security.hasEntityPermission("CONTENTMGR", "_UPDATE", userLogin)) {    
            out.write("<a href='/content/control/editContent?filePath=" + path + "'>*</a>");
        }
        
        /* this loops -- not good
        // if this page has a template, lets render the template                
        if (page != null && parent != null && page.getPath() != parent.getPath()) {
            Debug.logInfo("Parent: " + parent.getPath(), module);
            Debug.logInfo("Page: " + page.getPath(), module);
            Debug.logInfo("Template: " + page.getFullTemplateName(), module);
            if (!page.getTemplateName().equals("basic")) {                
                renderTemplate(cloneContext(context), page, out);
                return;
            }
        } 
        */       
        
        // get the view renderer for this page
        if (Debug.verboseOn()) Debug.logVerbose("Getting renderer for: " + path, module);
        String rendererName = DEFAULT_RENDERER;
        if (page != null) {            
            rendererName = page.getProperty("page-renderer");               
            if (rendererName == null)
                rendererName = DEFAULT_RENDERER;
        }
                                                        
        ViewRenderer renderer = (ViewRenderer) renderers.get(rendererName);
        if (renderer == null)
            renderer = (ViewRenderer) renderers.get(DEFAULT_RENDERER);   
                            
        // call the renderer to render the rest of the page.
        Debug.logVerbose("Calling render", module);
        renderer.render(context, path, in, out);                   
    }
    
    private void renderTemplate(JPublishContext context, Page page, Writer out) throws IOException, ViewRenderException {
        context.disableCheckReservedNames(this);
        context.put("page", page);
        if (siteContext.isProtectReservedNames()) {
            context.enableCheckReservedNames(this);
        }                
        try {        
            Debug.logInfo("Merging template", module);
            Template template = siteContext.getTemplateManager().getTemplate(page.getFullTemplateName());
            template.merge(context, page, out);
        } catch (Exception e) {
            throw new ViewRenderException(e);     
        }
    }    
    
    private JPublishContext cloneContext(JPublishContext context) {
        JPublishContext newContext = new JPublishContext(this);
        context.disableCheckReservedNames(this);
        Object keys[] = context.getKeys();
        for (int i = 0; i < keys.length; i++) 
            newContext.put((String) keys[i], context.get((String) keys[i]));
        if (siteContext.isProtectReservedNames()) {
            context.enableCheckReservedNames(this);
        }            
        return newContext;
    }

    /**
     * @see org.jpublish.view.ViewRenderer#render(org.jpublish.JPublishContext, java.io.InputStream, java.io.OutputStream)
     */
    public void render(JPublishContext context, String path, InputStream in, OutputStream out) throws IOException, ViewRenderException {
        render(context, path, new InputStreamReader(in), new OutputStreamWriter(out));               
    }

    /**
     * @see org.jpublish.view.ViewRenderer#loadConfiguration(com.anthonyeden.lib.config.Configuration)
     */
    public void loadConfiguration(Configuration config) throws ConfigurationException {               
    }
    
    private Page getPage(String path) {
        Page page = null;      
        try {        
            PageInstance pi = siteContext.getPageManager().getPage(path.substring(path.lastIndexOf(":")+1));
            if (pi != null)
                page = new Page(pi);            
        } catch (Exception e) {}      
        return page;
    }
    
    private void loadCustom() throws Exception {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        InputStream in = new FileInputStream(siteContext.getConfigurationFile());
        Configuration configuration = new XMLConfiguration(in);
        
        Iterator renderElements = configuration.getChildren("page-renderer").iterator();
        while (renderElements.hasNext()) {
            Configuration viewRendererConfiguration = (Configuration) renderElements.next();
            String renderName = viewRendererConfiguration.getAttribute("name");
            String className = viewRendererConfiguration.getAttribute("classname");
            ViewRenderer renderer = (ViewRenderer) cl.loadClass(className).newInstance();
            renderer.setSiteContext(siteContext);
            renderer.loadConfiguration(viewRendererConfiguration);
            renderer.init();
            Debug.logInfo("Added renderer [" + renderName + "] - [" + className + "]", module);
            renderers.put(renderName, renderer);           
        }                   
    }       
}
