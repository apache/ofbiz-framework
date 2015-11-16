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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.ofbiz.base.location.FlexibleLocation;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilCodec;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.cache.UtilCache;

import freemarker.cache.TemplateLoader;
import freemarker.core.Environment;
import freemarker.ext.beans.BeanModel;
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.template.Configuration;
import freemarker.template.SimpleHash;
import freemarker.template.SimpleScalar;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.Version;

/** FreeMarkerWorker - Freemarker Template Engine Utilities.
 *
 */
public class FreeMarkerWorker {

    public static final String module = FreeMarkerWorker.class.getName();

    public static final Version version = new Version(2, 3, 22);

    // use soft references for this so that things from Content records don't kill all of our memory, or maybe not for performance reasons... hmmm, leave to config file...
    private static final UtilCache<String, Template> cachedTemplates = UtilCache.createUtilCache("template.ftl.general", 0, 0, false);
    private static final BeansWrapper defaultOfbizWrapper = new BeansWrapperBuilder(version).build();
    private static final Configuration defaultOfbizConfig = makeConfiguration(defaultOfbizWrapper);

    public static BeansWrapper getDefaultOfbizWrapper() {
        return defaultOfbizWrapper;
    }

    public static Configuration makeConfiguration(BeansWrapper wrapper) {
        Configuration newConfig = new Configuration(version);

        newConfig.setObjectWrapper(wrapper);
        TemplateHashModel staticModels = wrapper.getStaticModels();
        newConfig.setSharedVariable("Static", staticModels);
        try {
            newConfig.setSharedVariable("EntityQuery", staticModels.get("org.ofbiz.entity.util.EntityQuery"));
        } catch (TemplateModelException e) {
            Debug.logError(e, module);
        }
        newConfig.setLocalizedLookup(false);
        newConfig.setSharedVariable("StringUtil", new BeanModel(StringUtil.INSTANCE, wrapper));
        newConfig.setTemplateLoader(new FlexibleTemplateLoader());
        newConfig.setAutoImports(UtilProperties.getProperties("freemarkerImports"));
        newConfig.setTemplateExceptionHandler(new FreeMarkerWorker.OFBizTemplateExceptionHandler());
        try {
            newConfig.setSetting("datetime_format", "yyyy-MM-dd HH:mm:ss.SSS");
            newConfig.setSetting("number_format", "0.##########");
        } catch (TemplateException e) {
            Debug.logError("Unable to set date/time and number formats in FreeMarker: " + e, module);
        }
        // Transforms properties file set up as key=transform name, property=transform class name
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources;
        try {
            resources = loader.getResources("freemarkerTransforms");
        } catch (IOException e) {
            Debug.logError(e, "Could not load list of freemarkerTransforms.properties", module);
            throw UtilMisc.initCause(new InternalError(e.getMessage()), e);
        }
        while (resources.hasMoreElements()) {
            URL propertyURL = resources.nextElement();
            Debug.logInfo("loading properties: " + propertyURL, module);
            Properties props = UtilProperties.getProperties(propertyURL);
            if (UtilValidate.isEmpty(props)) {
                Debug.logError("Unable to locate properties file " + propertyURL, module);
            } else {
                loadTransforms(loader, props, newConfig);
            }
        }

        return newConfig;
    }

    /**
     * Protected helper method.
     */
    protected static void loadTransforms(ClassLoader loader, Properties props, Configuration config) {
        for (Iterator<Object> i = props.keySet().iterator(); i.hasNext();) {
            String key = (String) i.next();
            String className = props.getProperty(key);
            if (Debug.verboseOn()) {
                Debug.logVerbose("Adding FTL Transform " + key + " with class " + className, module);
            }
            try {
                config.setSharedVariable(key, loader.loadClass(className).newInstance());
            } catch (Exception e) {
                Debug.logError(e, "Could not pre-initialize dynamically loaded class: " + className + ": " + e, module);
            }
        }
    }

    /**
     * Renders a template at the specified location.
     * @param templateLocation Location of the template - file path or URL
     * @param context The context Map
     * @param outWriter The Writer to render to
     */
    public static void renderTemplateAtLocation(String templateLocation, Map<String, Object> context, Appendable outWriter) throws MalformedURLException, TemplateException, IOException {
        renderTemplate(templateLocation, context, outWriter);
    }

    /**
     * Renders a template contained in a String.
     * @param templateLocation A unique ID for this template - used for caching
     * @param templateString The String containing the template
     * @param context The context Map
     * @param outWriter The Writer to render to
     */
    public static void renderTemplate(String templateLocation, String templateString, Map<String, Object> context, Appendable outWriter) throws TemplateException, IOException {
        renderTemplate(templateLocation, templateString, context, outWriter, true);
    }

    /**
     * Renders a template contained in a String.
     * @param templateLocation A unique ID for this template - used for caching
     * @param templateString The String containing the template
     * @param context The context Map
     * @param outWriter The Writer to render to
     * @param useCache try to get template from cache
     */
    public static void renderTemplate(String templateLocation, String templateString, Map<String, Object> context, Appendable outWriter, boolean useCache) throws TemplateException, IOException {
        if (templateString == null) {
            renderTemplate(templateLocation, context, outWriter);
        } else {
            renderTemplateFromString(templateString, templateLocation, context, outWriter, useCache);
        }
    }

    /**
     * Renders a template from a Reader.
     * @param templateLocation A unique ID for this template - used for caching
     * @param context The context Map
     * @param outWriter The Writer to render to
     */
    public static void renderTemplate(String templateLocation, Map<String, Object> context, Appendable outWriter) throws TemplateException, IOException {
        Template template = getTemplate(templateLocation);
        renderTemplate(template, context, outWriter);
    }

    /**
     * @deprecated Renamed to {@link #renderTemplateFromString(String, String, Map, Appendable, boolean)}
     */
    @Deprecated
    public static Environment renderTemplateFromString(String templateString, String templateLocation, Map<String, Object> context, Appendable outWriter) throws TemplateException, IOException {
        Template template = cachedTemplates.get(templateLocation);
        if (template == null) {
            Reader templateReader = new StringReader(templateString);
            template = new Template(templateLocation, templateReader, defaultOfbizConfig);
            templateReader.close();
            template = cachedTemplates.putIfAbsentAndGet(templateLocation, template);
        }
        return renderTemplate(template, context, outWriter);
    }

    public static Environment renderTemplateFromString(String templateString, String templateLocation, Map<String, Object> context, Appendable outWriter, boolean useCache) throws TemplateException, IOException {
        Template template = null;
        if (useCache) {
            template = cachedTemplates.get(templateLocation);
            if (template == null) {
                Reader templateReader = new StringReader(templateString);
                template = new Template(templateLocation, templateReader, defaultOfbizConfig);
                templateReader.close();
                template = cachedTemplates.putIfAbsentAndGet(templateLocation, template);
            }
        } else {
            Reader templateReader = new StringReader(templateString);
            template = new Template(templateLocation, templateReader, defaultOfbizConfig);
            templateReader.close();
        }
        return renderTemplate(template, context, outWriter);
    }

    public static void clearTemplateFromCache(String templateLocation) {
        cachedTemplates.remove(templateLocation);
    }

    /**
     * Renders a Template instance.
     * @param template A Template instance
     * @param context The context Map
     * @param outWriter The Writer to render to
     */
    public static Environment renderTemplate(Template template, Map<String, Object> context, Appendable outWriter) throws TemplateException, IOException {
        // make sure there is no "null" string in there as FreeMarker will try to use it
        context.remove("null");
        // Since the template cache keeps a single instance of a Template that is shared among users,
        // and since that Template instance is immutable, we need to create an Environment instance and
        // use it to process the template with the user's settings.
        //
        // FIXME: the casting from Appendable to Writer is a temporary fix that could cause a
        //        run time error if in the future we will pass a different class to the method
        //        (such as a StringBuffer).
        Environment env = template.createProcessingEnvironment(context, (Writer) outWriter);
        applyUserSettings(env, context);
        env.process();
        return env;
    }

    /**
     * Apply user settings to an Environment instance.
     * @param env An Environment instance
     * @param context The context Map containing the user settings
     */
    public static void applyUserSettings(Environment env, Map<String, Object> context) throws TemplateException {
        Locale locale = (Locale) context.get("locale");
        if (locale == null) {
            locale = Locale.getDefault();
        }
        env.setLocale(locale);

        TimeZone timeZone = (TimeZone) context.get("timeZone");
        if (timeZone == null) {
            timeZone = TimeZone.getDefault();
        }
        env.setTimeZone(timeZone);
    }

    /**
     * Returns a <code>Configuration</code> instance initialized to OFBiz defaults. Client code should
     * call this method instead of creating its own <code>Configuration</code> instance. The instance
     * returned by this method includes the <code>component://</code> resolver and the OFBiz custom
     * transformations.
     * 
     * @return A <code>Configuration</code> instance.
     */
    public static Configuration getDefaultOfbizConfig() {
        return defaultOfbizConfig;
    }

    /** Make sure to close the reader when you're done! That's why this method is private, BTW. */
    private static Reader makeReader(String templateLocation) throws IOException {
        if (UtilValidate.isEmpty(templateLocation)) {
            throw new IllegalArgumentException("FreeMarker template location null or empty");
        }

        URL locationUrl = null;
        try {
            locationUrl = FlexibleLocation.resolveLocation(templateLocation);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        if (locationUrl == null) {
            throw new IllegalArgumentException("FreeMarker file not found at location: " + templateLocation);
        }

        InputStream locationIs = locationUrl.openStream();
        Reader templateReader = new InputStreamReader(locationIs);

        String locationProtocol = locationUrl.getProtocol();
        if ("file".equals(locationProtocol) && Debug.verboseOn()) {
            String locationFile = locationUrl.getFile();
            int lastSlash = locationFile.lastIndexOf("/");
            String locationDir = locationFile.substring(0, lastSlash);
            String filename = locationFile.substring(lastSlash + 1);
            Debug.logVerbose("FreeMarker render: filename=" + filename + ", locationDir=" + locationDir, module);
        }

        return templateReader;
    }

    /**
     * Gets a Template instance from the template cache. If the Template instance isn't
     * found in the cache, then one will be created.
     * @param templateLocation Location of the template - file path or URL
     */
    public static Template getTemplate(String templateLocation) throws TemplateException, IOException {
        return getTemplate(templateLocation, cachedTemplates, defaultOfbizConfig);
    }

    public static Template getTemplate(String templateLocation, UtilCache<String, Template> cache, Configuration config) throws TemplateException, IOException {
        Template template = cache.get(templateLocation);
        if (template == null) {
            // only make the reader if we need it, and then close it right after!
            Reader templateReader = makeReader(templateLocation);
            template = new Template(templateLocation, templateReader, config);
            templateReader.close();
            template = cache.putIfAbsentAndGet(templateLocation, template);
        }
        return template;
    }

    public static String getArg(Map<String, ? extends Object> args, String key, Environment env) {
        Map<String, ? extends Object> templateContext = FreeMarkerWorker.getWrappedObject("context", env);
        return getArg(args, key, templateContext);
    }

    public static String getArg(Map<String, ? extends Object> args, String key, Map<String, ? extends Object> templateContext) {
        //SimpleScalar s = null;
        Object o = args.get(key);
        String returnVal = (String) unwrap(o);
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

    public static Object getArgObject(Map<String, ? extends Object> args, String key, Map<String, ? extends Object> templateContext) {
        //SimpleScalar s = null;
        Object o = args.get(key);
        Object returnVal = unwrap(o);
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
    public static <T> T getWrappedObject(String varName, Environment env) {
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
        return UtilGenerics.<T>cast(obj);
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
        Object o = null;
        try {
            o = args.get(key);
        } catch (TemplateModelException e) {
            Debug.logVerbose(e.getMessage(), module);
            return null;
        }

        Object returnObj = unwrap(o);

        if (returnObj == null) {
            Object ctxObj = null;
            try {
                ctxObj = args.get("context");
            } catch (TemplateModelException e) {
                Debug.logInfo(e.getMessage(), module);
                return returnObj;
            }
            Map<String, ?> ctx = null;
            if (ctxObj instanceof BeanModel) {
                ctx = UtilGenerics.cast(((BeanModel) ctxObj).getWrappedObject());
                returnObj = ctx.get(key);
            }
            /*
            try {
                Map templateContext = (Map) FreeMarkerWorker.getWrappedObject("context", env);
                if (templateContext != null) {
                    returnObj = (String) templateContext.get(key);
                }
            } catch (ClassCastException e2) {
                //return null;
            }
            */
        }
        return returnObj;
    }

    @SuppressWarnings("unchecked")
    public static <T> T unwrap(Object o) {
        Object returnObj = null;

        if (o == TemplateModel.NOTHING) {
            returnObj = null;
        } else if (o instanceof SimpleScalar) {
            returnObj = o.toString();
        } else if (o instanceof BeanModel) {
            returnObj = ((BeanModel) o).getWrappedObject();
        }

        return (T) returnObj;
    }

    public static void checkForLoop(String path, Map<String, Object> ctx) throws IOException {
        List<String> templateList = UtilGenerics.checkList(ctx.get("templateList"));
        if (templateList == null) {
            templateList = new LinkedList<String>();
        } else {
            if (templateList.contains(path)) {
                throw new IOException(path + " has already been visited.");
            }
        }
        templateList.add(path);
        ctx.put("templateList", templateList);
    }

    public static Map<String, Object> createEnvironmentMap(Environment env) {
        Map<String, Object> templateRoot = new HashMap<String, Object>();
        Set<String> varNames = null;
        try {
            varNames = UtilGenerics.checkSet(env.getKnownVariableNames());
        } catch (TemplateModelException e1) {
            Debug.logError(e1, "Error getting FreeMarker variable names, will not put pass current context on to sub-content", module);
        }
        if (varNames != null) {
            for (String varName: varNames) {
                //freemarker.ext.beans.StringModel varObj = (freemarker.ext.beans.StringModel) varNameIter.next();
                //Object varObj =  varNameIter.next();
                //String varName = varObj.toString();
                templateRoot.put(varName, FreeMarkerWorker.getWrappedObject(varName, env));
            }
        }
        return templateRoot;
    }

    public static void saveContextValues(Map<String, Object> context, String [] saveKeyNames, Map<String, Object> saveMap) {
        //Map saveMap = new HashMap();
        for (String key: saveKeyNames) {
            Object o = context.get(key);
            if (o instanceof Map<?, ?>) {
                o = UtilMisc.makeMapWritable(UtilGenerics.checkMap(o));
            } else if (o instanceof List<?>) {
                o = UtilMisc.makeListWritable(UtilGenerics.checkList(o));
            }
            saveMap.put(key, o);
        }
    }

    public static Map<String, Object> saveValues(Map<String, Object> context, String [] saveKeyNames) {
        Map<String, Object> saveMap = new HashMap<String, Object>();
        for (String key: saveKeyNames) {
            Object o = context.get(key);
            if (o instanceof Map<?, ?>) {
                o = UtilMisc.makeMapWritable(UtilGenerics.checkMap(o));
            } else if (o instanceof List<?>) {
                o = UtilMisc.makeListWritable(UtilGenerics.checkList(o));
            }
            saveMap.put(key, o);
        }
        return saveMap;
    }


    public static void reloadValues(Map<String, Object> context, Map<String, Object> saveValues, Environment env) {
        for (Map.Entry<String, Object> entry: saveValues.entrySet()) {
            String key = entry.getKey();
            Object o = entry.getValue();
            if (o instanceof Map<?, ?>) {
                context.put(key, UtilMisc.makeMapWritable(UtilGenerics.checkMap(o)));
            } else if (o instanceof List<?>) {
                List<Object> list = new ArrayList<Object>();
                list.addAll(UtilGenerics.checkList(o));
                context.put(key, list);
            } else {
                context.put(key, o);
            }
            env.setVariable(key, autoWrap(o, env));
        }
    }

    public static void removeValues(Map<String, ?> context, String... removeKeyNames) {
        for (String key: removeKeyNames) {
            context.remove(key);
        }
    }

    public static void overrideWithArgs(Map<String, Object> ctx, Map<String, Object> args) {
        for (Map.Entry<String, Object> entry: args.entrySet()) {
            String key = entry.getKey();
            Object obj = entry.getValue();
            //if (Debug.infoOn()) Debug.logInfo("in overrideWithArgs, key(3):" + key + " obj:" + obj + " class:" + obj.getClass().getName() , module);
            if (obj != null) {
                if (obj == TemplateModel.NOTHING) {
                    ctx.put(key, null);
                } else {
                    Object unwrappedObj = unwrap(obj);
                    if (unwrappedObj == null) {
                        unwrappedObj = obj;
                    }
                    ctx.put(key, unwrappedObj.toString());
                }
            } else {
                ctx.put(key, null);
            }
        }
    }

    public static void convertContext(Map<String, Object> ctx) {
        for (Map.Entry<String, Object> entry: ctx.entrySet()) {
            Object obj = entry.getValue();
            if (obj != null) {
                Object unwrappedObj = unwrap(obj);
                if (unwrappedObj != null) {
                    entry.setValue(unwrappedObj);
                }
            }
        }
    }

    public static void getSiteParameters(HttpServletRequest request, Map<String, Object> ctx) {
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
       TemplateModel templateModelObj = null;
       try {
           templateModelObj = getDefaultOfbizWrapper().wrap(obj);
       } catch (TemplateModelException e) {
           throw new RuntimeException(e.getMessage());
       }
       return templateModelObj;
    }

    /**
     * OFBiz Template Source. This class is used by FlexibleTemplateLoader.
     */
    static class FlexibleTemplateSource {
        protected String templateLocation = null;
        protected Date createdDate = new Date();

        protected FlexibleTemplateSource() {}
        public FlexibleTemplateSource(String templateLocation) {
            this.templateLocation = templateLocation;
        }

        @Override
        public int hashCode() {
            return templateLocation.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof FlexibleTemplateSource && obj.hashCode() == this.hashCode();
        }

        public String getTemplateLocation() {
            return templateLocation;
        }

        public long getLastModified() {
            return createdDate.getTime();
        }
    }

    /**
     * OFBiz Template Loader. This template loader uses the FlexibleLocation
     * class to locate and load Freemarker templates.
     */
    static class FlexibleTemplateLoader implements TemplateLoader {
        public Object findTemplateSource(String name) throws IOException {
            return new FlexibleTemplateSource(name);
        }

        public long getLastModified(Object templateSource) {
            FlexibleTemplateSource fts = (FlexibleTemplateSource) templateSource;
            return fts.getLastModified();
        }

        public Reader getReader(Object templateSource, String encoding) throws IOException {
            FlexibleTemplateSource fts = (FlexibleTemplateSource) templateSource;
            return makeReader(fts.getTemplateLocation());
        }

        public void closeTemplateSource(Object templateSource) throws IOException {
            // do nothing
        }
    }

    /**
     * OFBiz specific TemplateExceptionHandler.  Sanitizes any error messages present in
     * the stack trace prior to printing to the output writer.
     */
    static class OFBizTemplateExceptionHandler implements TemplateExceptionHandler {
        public void handleTemplateException(TemplateException te, Environment env, Writer out) throws TemplateException {
            StringWriter tempWriter = new StringWriter();
            PrintWriter pw = new PrintWriter(tempWriter, true);
            te.printStackTrace(pw);
            String stackTrace = tempWriter.toString();

            UtilCodec.SimpleEncoder simpleEncoder = FreeMarkerWorker.getWrappedObject("simpleEncoder", env);
            if (simpleEncoder != null) {
                stackTrace = simpleEncoder.encode(stackTrace);
            }
            try {
                out.write(stackTrace);
            } catch (IOException e) {
                Debug.logError(e, module);
            }
        }
    }

    public static String encodeDoubleQuotes(String htmlString) {
        return htmlString.replaceAll("\"", "\\\\\"");
    }
}
