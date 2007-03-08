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
package org.ofbiz.base.util.template;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import javolution.util.FastMap;

import org.ofbiz.base.location.FlexibleLocation;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.cache.UtilCache;

import freemarker.core.Environment;
import freemarker.ext.beans.BeanModel;
import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Configuration;
import freemarker.template.SimpleHash;
import freemarker.template.SimpleScalar;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
//import com.clarkware.profiler.Profiler;


/**
 * FreemarkerViewHandler - Freemarker Template Engine Util
 *
 */
public class FreeMarkerWorker {
    
    public static final String module = FreeMarkerWorker.class.getName();
    
    // use soft references for this so that things from Content records don't kill all of our memory, or maybe not for performance reasons... hmmm, leave to config file...
    public static UtilCache cachedTemplates = new UtilCache("template.ftl.general", 0, 0, false);
    // these are mode "code" oriented so don't use soft references
    public static UtilCache cachedLocationTemplates = new UtilCache("template.ftl.location", 0, 0, false);

    public static Map ftlTransforms = new HashMap();
    
    static {
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();

            // note: loadClass is necessary for these since this class doesn't know anything about them at compile time
            // double note: may want to make this more dynamic and configurable in the future
            ftlTransforms.put("ofbizUrl", loader.loadClass("org.ofbiz.webapp.ftl.OfbizUrlTransform").newInstance());
            ftlTransforms.put("ofbizContentUrl", loader.loadClass("org.ofbiz.webapp.ftl.OfbizContentTransform").newInstance());
            ftlTransforms.put("ofbizCurrency", loader.loadClass("org.ofbiz.webapp.ftl.OfbizCurrencyTransform").newInstance());
            ftlTransforms.put("ofbizAmount", loader.loadClass("org.ofbiz.webapp.ftl.OfbizAmountTransform").newInstance());
            ftlTransforms.put("setRequestAttribute", loader.loadClass("org.ofbiz.webapp.ftl.SetRequestAttributeMethod").newInstance());
            ftlTransforms.put("renderWrappedText", loader.loadClass("org.ofbiz.webapp.ftl.RenderWrappedTextTransform").newInstance());

            ftlTransforms.put("menuWrap", loader.loadClass("org.ofbiz.widget.menu.MenuWrapTransform").newInstance());
        } catch (ClassNotFoundException e) {
            Debug.logError(e, "Could not pre-initialize dynamically loaded class: ", module);
        } catch (IllegalAccessException e) {
            Debug.logError(e, "Could not pre-initialize dynamically loaded class: ", module);
        } catch (InstantiationException e) {
            Debug.logError(e, "Could not pre-initialize dynamically loaded class: ", module);
        }

        // do the applications ones in a separate pass so the framework ones can load even if the applications are not present
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();

            ftlTransforms.put("editRenderSubContent", loader.loadClass("org.ofbiz.content.webapp.ftl.EditRenderSubContentTransform").newInstance());
            ftlTransforms.put("renderSubContent", loader.loadClass("org.ofbiz.content.webapp.ftl.RenderSubContentTransform").newInstance());
            ftlTransforms.put("loopSubContent", loader.loadClass("org.ofbiz.content.webapp.ftl.LoopSubContentTransform").newInstance());
            ftlTransforms.put("traverseSubContent", loader.loadClass("org.ofbiz.content.webapp.ftl.TraverseSubContentTransform").newInstance());

            ftlTransforms.put("checkPermission", loader.loadClass("org.ofbiz.content.webapp.ftl.CheckPermissionTransform").newInstance());
            ftlTransforms.put("injectNodeTrailCsv", loader.loadClass("org.ofbiz.content.webapp.ftl.InjectNodeTrailCsvTransform").newInstance());
            
            ftlTransforms.put("editRenderSubContentCache", loader.loadClass("org.ofbiz.content.webapp.ftl.EditRenderSubContentCacheTransform").newInstance());
            ftlTransforms.put("renderSubContentCache", loader.loadClass("org.ofbiz.content.webapp.ftl.RenderSubContentCacheTransform").newInstance());
            ftlTransforms.put("loopSubContentCache", loader.loadClass("org.ofbiz.content.webapp.ftl.LoopSubContentCacheTransform").newInstance());
            ftlTransforms.put("traverseSubContentCache", loader.loadClass("org.ofbiz.content.webapp.ftl.TraverseSubContentCacheTransform").newInstance());
            ftlTransforms.put("wrapSubContentCache", loader.loadClass("org.ofbiz.content.webapp.ftl.WrapSubContentCacheTransform").newInstance());
            ftlTransforms.put("limitedSubContent", loader.loadClass("org.ofbiz.content.webapp.ftl.LimitedSubContentCacheTransform").newInstance());
            ftlTransforms.put("renderSubContentAsText", loader.loadClass("org.ofbiz.content.webapp.ftl.RenderSubContentAsText").newInstance());
            ftlTransforms.put("renderContentAsText", loader.loadClass("org.ofbiz.content.webapp.ftl.RenderContentAsText").newInstance());
            ftlTransforms.put("renderContent", loader.loadClass("org.ofbiz.content.webapp.ftl.RenderContentTransform").newInstance());
        } catch (ClassNotFoundException e) {
            Debug.logError("Could not pre-initialize dynamically loaded class: " + e.toString(), module);
        } catch (IllegalAccessException e) {
            Debug.logError("Could not pre-initialize dynamically loaded class: " + e.toString(), module);
        } catch (InstantiationException e) {
            Debug.logError("Could not pre-initialize dynamically loaded class: " + e.toString(), module);
        }
    }

    public static void renderTemplateAtLocation(String location, Map context, Writer outWriter) throws MalformedURLException, TemplateException, IOException {
        Template template = (Template) cachedTemplates.get(location);
        if (template == null) {
            synchronized (FreeMarkerWorker.class) {
                template = (Template) cachedTemplates.get(location);
                if (template == null) {
                    URL locationUrl = FlexibleLocation.resolveLocation(location);
                    if (locationUrl == null) {
                        throw new IllegalArgumentException("FreeMarker file not found at location: " + location);
                    }
                    Reader locationReader = new InputStreamReader(locationUrl.openStream());
                    
                    String locationProtocol = locationUrl.getProtocol();
                    String filename = null;
                    Configuration config = null;
                    if ("file".equals(locationProtocol)) {
                        String locationFile = locationUrl.getFile();
                        int lastSlash = locationFile.lastIndexOf("/");
                        String locationDir = locationFile.substring(0, lastSlash);
                        filename = locationFile.substring(lastSlash + 1);
                        if (Debug.verboseOn()) Debug.logVerbose("FreeMarker render: filename=" + filename + ", locationDir=" + locationDir, module);
                        //DEJ20050104 Don't know what to do here, FreeMarker does some funky stuff when loading includes and can't find a way to make it happy...
                        config = makeSingleUseOfbizFtlConfig(locationDir);
                    } else {
                        filename = locationUrl.toExternalForm();
                        config = makeDefaultOfbizConfig();
                    }
                    template = new Template(filename, locationReader, config);
                    
                    cachedTemplates.put(location, template);
                    
                    // ensure that freemarker uses locale to display locale sensitive data
                    Locale locale = (Locale) context.get("locale");
                    if (locale == null)
                        locale = Locale.getDefault();
                    template.setSetting("locale", locale.toString());                    
                }
            }
        }
        
        if (context == null) {
            context = FastMap.newInstance();
        }
        
        // add the OFBiz transforms/methods
        addAllOfbizTransforms(context);
        
        // make sure there is no "null" string in there as FreeMarker will try to use it
        context.remove("null");

        // process the template with the given data
        template.process(context, outWriter);
    }
    
    public static void renderTemplate(String templateIdString, String template, Map context, Writer outWriter) throws TemplateException, IOException {
        //if (Debug.infoOn()) Debug.logInfo("template:" + template.toString(), "");        
        Reader templateReader = new StringReader(template);
        renderTemplate(templateIdString, templateReader, context, outWriter);
    }
    
    public static void renderTemplate(String templateIdString, Reader templateReader, Map context, Writer outWriter) throws TemplateException, IOException {
        if (context == null) {
            context = new HashMap();
        }
        
        Configuration config = makeDefaultOfbizConfig();            
        Template template = new Template(templateIdString, templateReader, config); 
        
        // ensure that freemarker uses locale to display locale sensitive data
        Locale locale = (Locale) context.get("locale");
        if (locale == null)
            locale = Locale.getDefault();
        template.setSetting("locale", locale.toString());
        
        // add the OFBiz transforms/methods
        addAllOfbizTransforms(context);
        
        cachedTemplates.put(templateIdString, template);
        // process the template with the given data and write
        // the email body to the String buffer
        template.process(context, outWriter);
    }
 
    public static Template getTemplateCached(String dataResourceId) {
        Template t = (Template)cachedTemplates.get("DataResource:" + dataResourceId);
        return t;
    }

    public static void renderTemplateCached(Template template, Map context, Writer outWriter) throws TemplateException, IOException {
        template.process(context, outWriter);
    }
    
    public static void addAllOfbizTransforms(Map context) {
        BeansWrapper wrapper = BeansWrapper.getDefaultInstance();
        TemplateHashModel staticModels = wrapper.getStaticModels();
        context.put("Static", staticModels);

        context.putAll(ftlTransforms);
    }

    private static Configuration defaultOfbizConfig = null;
    public static Configuration makeDefaultOfbizConfig() throws TemplateException, IOException {
        if (defaultOfbizConfig == null) {
            synchronized (FreeMarkerWorker.class) {
                if (defaultOfbizConfig == null) {
                    Configuration config = new Configuration();            
                    config.setObjectWrapper(BeansWrapper.getDefaultInstance());
                    config.setSetting("datetime_format", "yyyy-MM-dd HH:mm:ss.SSS");
                    config.setSetting("number_format", "0.##########");
                    defaultOfbizConfig = config;
                }
            }
        }
        return defaultOfbizConfig;
    }
    
    public static Configuration makeSingleUseOfbizFtlConfig(String locationDir) throws TemplateException, IOException {
        Configuration config = new Configuration();            
        config.setObjectWrapper(BeansWrapper.getDefaultInstance());
        config.setSetting("datetime_format", "yyyy-MM-dd HH:mm:ss.SSS");
        config.setSetting("number_format", "0.##########");
        if (locationDir != null) {
            File locationDirFile = new File(locationDir);
            if (locationDirFile != null) {
                if (locationDirFile.isFile()) {
                    /* maybe best not to do this, maybe best to throw an exception
                    String realDir = locationDir.substring(0, locationDir.lastIndexOf("/"));
                    locationDirFile = new File(realDir);
                    */
                    throw new IllegalArgumentException("Could not create FTL Configuration object because locationDir is a file: " + locationDir);
                }
                if (locationDirFile != null && locationDirFile.isDirectory()) {
                    config.setDirectoryForTemplateLoading(locationDirFile);
                }
            }
        }
        return config;
    }
    
    public static String getArg(Map args, String key, Environment env) {
        Map templateContext = (Map) FreeMarkerWorker.getWrappedObject("context", env);
        return getArg(args, key, templateContext);
    }

    public static String getArg(Map args, String key, Map templateContext) {
        //SimpleScalar s = null;
        Object o = null;
        String returnVal = null;
        o = args.get(key);
        returnVal = (String) unwrap(o);
        if (returnVal == null) {
            try {
                if (templateContext != null) {
                    returnVal = (String) templateContext.get(key);
                }
            } catch (ClassCastException e2) {
                //return null;
            }
        }
        return returnVal;
    }

    public static Object getArgObject(Map args, String key, Map templateContext) {
        //SimpleScalar s = null;
        Object o = null;
        Object returnVal = null;
        o = args.get(key);
        returnVal = unwrap(o);
        if (returnVal == null) {
            try {
                if (templateContext != null) {
                    returnVal = templateContext.get(key);
                }
            } catch (ClassCastException e2) {
                //return null;
            }
        }
        return returnVal;
    }


   /**
    * Gets BeanModel from FreeMarker context and returns the object that it wraps.
    * @param varName the name of the variable in the FreeMarker context.
    * @param env the FreeMarker Environment
    */
    public static Object getWrappedObject(String varName, Environment env) {
        Object obj = null;
        try {
            obj = env.getVariable(varName);
            if (obj != null) {
                if (obj == TemplateModel.NOTHING) {
                    obj = null;
                } else if (obj instanceof BeanModel) {
                    BeanModel bean = (BeanModel) obj;
                    obj = bean.getWrappedObject();
                } else if (obj instanceof SimpleScalar) {
                    obj = obj.toString();
                }
            }
        } catch (TemplateModelException e) {
            Debug.logInfo(e.getMessage(), module);
        }
        return obj;
    }

   /**
    * Gets BeanModel from FreeMarker context and returns the object that it wraps.
    * @param varName the name of the variable in the FreeMarker context.
    * @param env the FreeMarker Environment
    */
    public static BeanModel getBeanModel(String varName, Environment env) {
        BeanModel bean = null;
        try {
            bean = (BeanModel) env.getVariable(varName);
        } catch (TemplateModelException e) {
            Debug.logInfo(e.getMessage(), module);
        }
        return bean;
    }

    public static Object get(SimpleHash args, String key) {
        Object returnObj = null;
        Object o = null;
        try {
            o = args.get(key);
        } catch(TemplateModelException e) {
            Debug.logVerbose(e.getMessage(), module);
            return returnObj;
        }

        returnObj = unwrap(o);

        if (returnObj == null) {
            Object ctxObj = null;
            try {
                ctxObj = args.get("context");
            } catch(TemplateModelException e) {
                Debug.logInfo(e.getMessage(), module);
                return returnObj;
            }
            Map ctx = null;
            if (ctxObj instanceof BeanModel) {
                ctx = (Map)((BeanModel)ctxObj).getWrappedObject();
            returnObj = ctx.get(key);
            }
            /*
            try {
                Map templateContext = (Map)FreeMarkerWorker.getWrappedObject("context", env);
                if (templateContext != null) {
                    returnObj = (String)templateContext.get(key);
                }
            } catch(ClassCastException e2) {
                //return null;
            }
            */
        }
        return returnObj;
    }

    public static Object unwrap(Object o) {
        Object returnObj = null;

        if (o == TemplateModel.NOTHING) {
            returnObj = null;
        } else if (o instanceof SimpleScalar) {
            returnObj = o.toString();
        } else if (o instanceof BeanModel) {
            returnObj = ((BeanModel)o).getWrappedObject();
        }
    
        return returnObj;
    }

    public static void checkForLoop(String path, Map ctx) throws IOException {
        List templateList = (List)ctx.get("templateList");
        if (templateList == null) {
            templateList = new ArrayList();
        } else {
            if (templateList.contains(path)) {
                throw new IOException(path + " has already been visited.");
            }
        }
        templateList.add(path);
        ctx.put("templateList", templateList);
    }

    public static Map createEnvironmentMap(Environment env) {
        Map templateRoot = new HashMap();
        Set varNames = null;
        try {
            varNames = env.getKnownVariableNames();
        } catch (TemplateModelException e1) {
            Debug.logError(e1, "Error getting FreeMarker variable names, will not put pass current context on to sub-content", module);
        }
        if (varNames != null) {
            Iterator varNameIter = varNames.iterator();
            while (varNameIter.hasNext()) {
                String varName = (String) varNameIter.next();
                //freemarker.ext.beans.StringModel varObj = (freemarker.ext.beans.StringModel ) varNameIter.next();
                //Object varObj =  varNameIter.next();
                //String varName = varObj.toString();
                templateRoot.put(varName, FreeMarkerWorker.getWrappedObject(varName, env));
            }
        }
        return templateRoot;
    }
    
    public static void saveContextValues(Map context, String [] saveKeyNames, Map saveMap ) {
        //Map saveMap = new HashMap();
        for (int i=0; i<saveKeyNames.length; i++) {
            String key = saveKeyNames[i];
            Object o = context.get(key);
            if (o instanceof Map)
                o = new HashMap((Map)o);
            else if (o instanceof List)
                o = new ArrayList((List)o);
            saveMap.put(key, o);
        }
    }

    public static Map saveValues(Map context, String [] saveKeyNames) {
        Map saveMap = new HashMap();
        for (int i=0; i<saveKeyNames.length; i++) {
            String key = saveKeyNames[i];
            Object o = context.get(key);
            if (o instanceof Map)
                o = new HashMap((Map)o);
            else if (o instanceof List)
                o = new ArrayList((List)o);
            saveMap.put(key, o);
        }
        return saveMap;
    }


    public static void reloadValues(Map context, Map saveValues, Environment env ) {
        Set keySet = saveValues.keySet();
        Iterator it = keySet.iterator();
        while (it.hasNext()) {
            String key = (String)it.next();
            Object o = saveValues.get(key);
            if (o instanceof Map) {
                Map map = new HashMap();
                map.putAll((Map)o);
                context.put(key, map);
            } else if (o instanceof List) {
                List list = new ArrayList();
                list.addAll((List)o);
                context.put(key, list);
            } else {
                context.put(key, o);
            }
            env.setVariable(key, autoWrap(o, env));
        }
    }

    public static void removeValues(Map context, String [] removeKeyNames ) {
        for (int i=0; i<removeKeyNames.length; i++) {
            String key = removeKeyNames[i];
            context.remove(key);
        }
    }

    public static void overrideWithArgs(Map ctx, Map args) {
        Set keySet = args.keySet();
        Iterator it = keySet.iterator();
        while (it.hasNext()) {
            String key = (String)it.next();
            Object obj = args.get(key);
            //if (Debug.infoOn()) Debug.logInfo("in overrideWithArgs, key(3):" + key + " obj:" + obj + " class:" + obj.getClass().getName() , module);
            if (obj != null) {
                if (obj == TemplateModel.NOTHING) {
                    ctx.put(key, null);
                } else {
                    Object unwrappedObj = unwrap(obj);
                    if (unwrappedObj == null)
                        unwrappedObj = obj;
                    ctx.put(key, unwrappedObj.toString());
                }
            } else {
                ctx.put(key, null);
            }
        }
    }

    public static void convertContext(Map ctx) {
        Set keySet = ctx.keySet();
        Iterator it = keySet.iterator();
        while (it.hasNext()) {
            String key = (String)it.next();
            Object obj = ctx.get(key);
            if (obj != null) {
                Object unwrappedObj = unwrap(obj);
                if (unwrappedObj != null) {
                    ctx.put(key, unwrappedObj);
                }
            }
        }
    }

    public static void getSiteParameters(HttpServletRequest request, Map ctx) {
        if (request == null) {
            return;
        }
        if (ctx == null) {
            throw new IllegalArgumentException("Error in getSiteParameters, context/ctx cannot be null");
        }
        ServletContext servletContext = request.getSession().getServletContext();
        String rootDir = (String)ctx.get("rootDir");
        String webSiteId = (String)ctx.get("webSiteId");
        String https = (String)ctx.get("https");
        if (UtilValidate.isEmpty(rootDir)) {
            rootDir = servletContext.getRealPath("/");
            ctx.put("rootDir", rootDir);
        }
        if (UtilValidate.isEmpty(webSiteId)) {
            webSiteId = (String) servletContext.getAttribute("webSiteId");
            ctx.put("webSiteId", webSiteId);
        }
        if (UtilValidate.isEmpty(https)) {
            https = (String) servletContext.getAttribute("https");
            ctx.put("https", https);
        }
    }

    public static TemplateModel autoWrap(Object obj, Environment env) {
       BeansWrapper wrapper = BeansWrapper.getDefaultInstance();
       TemplateModel templateModelObj = null;
       try {
           templateModelObj = wrapper.wrap(obj);
       } catch(TemplateModelException e) {
           throw new RuntimeException(e.getMessage());
       }
       return templateModelObj;
    }
}
