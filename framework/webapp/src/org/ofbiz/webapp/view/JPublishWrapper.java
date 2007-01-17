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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Iterator;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;

import org.jpublish.JPublishContext;
import org.jpublish.Page;
import org.jpublish.Repository;
import org.jpublish.RepositoryWrapper;
import org.jpublish.SiteContext;
import org.jpublish.StaticResourceManager;
import org.jpublish.Template;
import org.jpublish.action.ActionManager;
import org.jpublish.component.ComponentMap;
import org.jpublish.page.PageInstance;
import org.jpublish.util.CharacterEncodingMap;
import org.jpublish.util.DateUtilities;
import org.jpublish.util.NumberUtilities;
import org.jpublish.util.URLUtilities;

/**
 * JPublishWrapper - Used for calling pages through JPublish
 */
public class JPublishWrapper {
    
    public static final String module = JPublishWrapper.class.getName();
    
    protected ServletContext servletContext = null;
    protected SiteContext siteContext = null;
    
    public JPublishWrapper(ServletContext context) {
        this.servletContext = context;
        // find the WEB-INF root
        String rootDir = servletContext.getRealPath("/");
        File contextRoot = new File(rootDir);
        File webInfPath = new File(contextRoot, "WEB-INF");

        // configure the classpath for scripting support
        configureClasspath(webInfPath);

        // create the site context
        try {
            //siteContext = new SiteContext(contextRoot, servletConfig.getInitParameter("config"));
            siteContext = new SiteContext(contextRoot, "WEB-INF/jpublish.xml");
            siteContext.setWebInfPath(webInfPath);            
        } catch (Exception e) {
            Debug.logError(e, "Cannot load SiteContext", module);            
        }

        // execute startup actions
        try {
            ActionManager actionManager = siteContext.getActionManager();
            actionManager.executeStartupActions();
        } catch (Exception e) {
            Debug.logError(e, "Problems executing JPublish startup actions", module);            
        }
        
        // set this wrapper in the ServletContext for use by the ViewHandler
        servletContext.setAttribute("jpublishWrapper", this);
    }
    
    protected void configureClasspath(File webInfPath) {
        File webLibPath = new File(webInfPath, "lib");
        File webClassPath = new File(webInfPath, "classes");

        // add WEB-INF/classes to the classpath
        StringBuffer classPath = new StringBuffer();
        classPath.append(System.getProperty("java.class.path"));

        if (webClassPath.exists()) {
            classPath.append(System.getProperty("path.separator"));
            classPath.append(webClassPath);
        }

        // add WEB-INF/lib files to the classpath
        if (webLibPath.exists()) {
            File[] files = webLibPath.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].getName().toLowerCase().endsWith(".jar")
                    || files[i].getName().toLowerCase().endsWith(".zip")) {
                    classPath.append(System.getProperty("path.separator"));
                    classPath.append(files[i]);
                }
            }
        }

        AccessController.doPrivileged(new SetClassPathAction(classPath.toString()));
    }

    protected boolean executeGlobalActions(HttpServletRequest request, HttpServletResponse response, JPublishContext context, String path, boolean allowRedirect) throws Exception {
        ActionManager actionManager = siteContext.getActionManager();
        return optionalRedirect(actionManager.executeGlobalActions(context), path, response, allowRedirect);
    }

    protected boolean executePathActions(HttpServletRequest request, HttpServletResponse response, JPublishContext context, String path, boolean allowRedirect) throws Exception {
        ActionManager actionManager = siteContext.getActionManager();
        return optionalRedirect(actionManager.executePathActions(path, context), path, response, allowRedirect);
    }

    protected boolean executeParameterActions(HttpServletRequest request, HttpServletResponse response, JPublishContext context, String path, boolean allowRedirect) throws Exception {
        if (!siteContext.isParameterActionsEnabled()) {
            return false;
        }

        ActionManager actionManager = siteContext.getActionManager();
        String[] actionNames = request.getParameterValues(siteContext.getActionIdentifier());
        if (actionNames != null) {
            for (int i = 0; i < actionNames.length; i++) {
                return optionalRedirect(actionManager.execute(actionNames[i], context), path, response, allowRedirect);
            }
        }
        return false;
    }

    protected boolean executePreEvaluationActions(HttpServletRequest request, HttpServletResponse response, JPublishContext context, String path) throws Exception {
        ActionManager actionManager = siteContext.getActionManager();
        return actionManager.executePreEvaluationActions(path, context);
    }

    protected boolean executePostEvaluationActions(HttpServletRequest request, HttpServletResponse response, JPublishContext context, String path) throws Exception {
        ActionManager actionManager = siteContext.getActionManager();
        actionManager.executePostEvaluationActions(path, context);
        return false;
    }

    private boolean optionalRedirect(String redirect, String path, HttpServletResponse response, boolean allowRedirect) throws IOException {
        if (redirect != null && allowRedirect) {
            response.sendRedirect(redirect);
            return true;
        }
        return false;
    }
    
    /**
     * Renders a page and returns the string containing the content of the rendered page
     * @param path Path to the page
     * @param request HttpServletRequest object for page prep
     * @param response HttpServletResponse object (not used for writing).
     * @return a String containing the rendered page
     * @throws GeneralException
     */
    public String render(String path, HttpServletRequest request, HttpServletResponse response) throws GeneralException {
        Writer writer = new StringWriter();   
        String content = null;    
        render(path, request, response, writer);
        try {                    
            content = writer.toString();
            writer.close();
        } catch (IOException e) {
            throw new GeneralException("Problems closing the Writer", e);
        }
        return content;
    }
    
    public void render(String path, HttpServletRequest request, HttpServletResponse response, Writer writer) throws GeneralException {
        render(path, request, response, writer, null, false);
    }
    
    public void render(String path, HttpServletRequest request, HttpServletResponse response, Writer writer, OutputStream outputStream) throws GeneralException {
        render(path, request, response, writer, outputStream, false);
    }

    public void render(String path, HttpServletRequest request, HttpServletResponse response, Writer writer, OutputStream outputStream, boolean allowRedirect) throws GeneralException {
        HttpSession session = request.getSession();
        //String path = servletContext.getRealPath(pagePath);
        //Debug.logError("Path:" + path, module);

        // get the character encoding map
        CharacterEncodingMap characterEncodingMap = siteContext.getCharacterEncodingManager().getMap(path);

        // put standard servlet stuff into the context
        JPublishContext context = new JPublishContext(this);
        context.put("request", request);
        context.put("response", response);
        context.put("session", session);
        context.put("application", servletContext);

        // add the character encoding map to the context
        context.put("characterEncodingMap", characterEncodingMap);

        // add the URLUtilities to the context
        URLUtilities urlUtilities = new URLUtilities(request, response);
        context.put("urlUtilities", urlUtilities);

        // add the DateUtilities to the context
        context.put("dateUtilities", DateUtilities.getInstance());

        // add the NumberUtilities to the context
        context.put("numberUtilities", NumberUtilities.getInstance());

        // add the messages log to the context
        context.put("syslog", SiteContext.syslog);

        // expose the SiteContext
        context.put("site", siteContext);

        if (siteContext.isProtectReservedNames()) {
            context.enableCheckReservedNames(this);
        }

        // add the repositories to the context
        Iterator repositories = siteContext.getRepositories().iterator();
        while (repositories.hasNext()) {
            Repository repository = (Repository) repositories.next();
            context.put(repository.getName(), new RepositoryWrapper(repository, context));
            // add the fs_repository also as the name 'pages' so we can use existing logic in pages
            // note this is a hack and we should look at doing this a different way; but first need
            // to investigate how to get content from different repositories
            if (repository.getName().equals("fs_repository")) {
                context.put("pages", new RepositoryWrapper(repository, context));
            }
        }

        try {
            if (executePreEvaluationActions(request, response, context, path))
                return;

            // if the page is static
            StaticResourceManager staticResourceManager = siteContext.getStaticResourceManager();
            if (staticResourceManager.resourceExists(path)) {
                if (outputStream != null) {                
                    // execute the global actions
                    if (executeGlobalActions(request, response, context, path, allowRedirect))
                        return;

                    // execute path actions
                    if (executePathActions(request, response, context, path, allowRedirect))
                        return;
    
                    // execute parameter actions
                    if (executeParameterActions(request, response, context, path, allowRedirect))
                        return;

                    // load and return the static resource                                    
                    staticResourceManager.load(path, outputStream);
                    outputStream.flush();
                    return;
                } else {
                    throw new GeneralException("Cannot load static resource with a null OutputStream");
                }
            }
            
            // check and make sure we have a writer
            if (writer == null)
                throw new GeneralException("Cannot load dynamic content with a null Writer");

            // load the page          
            PageInstance pageInstance = siteContext.getPageManager().getPage(path);
            Page page = new Page(pageInstance);

            context.disableCheckReservedNames(this);

            // expose the page in the context
            context.put("page", page);

            // expose components in the context
            context.put("components", new ComponentMap(context));

            if (siteContext.isProtectReservedNames()) {
                context.enableCheckReservedNames(this);
            }

            // execute the global actions
            if (executeGlobalActions(request, response, context, path, allowRedirect))
                return;

            // execute path actions
            if (executePathActions(request, response, context, path, allowRedirect))
                return;

            // execute parameter actions
            if (executeParameterActions(request, response, context, path, allowRedirect))
                return;

            // execute the page actions           
            if (optionalRedirect(page.executeActions(context), path, response, allowRedirect))
                return;

            // get the template
            Template template = siteContext.getTemplateManager().getTemplate(page.getFullTemplateName());
           
            // merge the template           
            template.merge(context, page, writer);
            writer.flush();            
        } catch (FileNotFoundException e) {
            throw new GeneralException("File not found", e);
        } catch (Exception e) {
            throw new GeneralException("JPublish execution error", e);
        } finally {
            try {
                executePostEvaluationActions(request, response, context, path);
            } catch (Exception e) {
                throw new GeneralException("Error executing JPublish post evaluation actions", e);
            }
        }
    }

    /**      
     * Privleged action for setting the class path.  This is used to get around
     * the Java security system to set the class path so scripts have full 
     * access to all loaded Java classes.
     *  
     * <p>Note: This functionality is untested.</p>
     */
    class SetClassPathAction implements PrivilegedAction {
        private String classPath;

        /** 
         * Construct the action to set the class path.        
         *   @param classPath The new class path
         */
        public SetClassPathAction(String classPath) {
            this.classPath = classPath;
        }

        /** 
         * Set the "java.class.path" property.               
         * @return Returns null
         */
        public Object run() {
            System.setProperty("java.class.path", classPath);
            return null; // nothing to return
        }
    }
}
