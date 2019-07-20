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
package org.apache.ofbiz.base.util.template;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.ofbiz.base.location.FlexibleLocation;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.cache.UtilCache;

import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.StringTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.cache.URLTemplateLoader;
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
import org.apache.ofbiz.widget.model.ModelWidget;

/**
 * FreeMarkerWorker - Freemarker Template Engine Utilities.
 */
public final class FreeMarkerWorker {

    public static final String module = FreeMarkerWorker.class.getName();

    public static final Version version = Configuration.VERSION_2_3_28;

    private FreeMarkerWorker () {}

    // use soft references for this so that things from Content records don't kill all of our memory, or maybe not for performance reasons... hmmm, leave to config file...
    private static final UtilCache<String, Template> cachedTemplates = UtilCache.createUtilCache("template.ftl.general", 0, 0, false);
    private static final BeansWrapper defaultOfbizWrapper = new BeansWrapperBuilder(version).build();
    private static final Configuration defaultOfbizConfig = makeConfiguration(defaultOfbizWrapper);

    public static BeansWrapper getDefaultOfbizWrapper() {
        return defaultOfbizWrapper;
    }

    public static Configuration newConfiguration() {
        return new Configuration(version);
    }

    public static Configuration makeConfiguration(BeansWrapper wrapper) {
        Configuration newConfig = newConfiguration();

        newConfig.setObjectWrapper(wrapper);
        TemplateHashModel staticModels = wrapper.getStaticModels();
        newConfig.setSharedVariable("Static", staticModels);
        try {
            newConfig.setSharedVariable("EntityQuery", staticModels.get("org.apache.ofbiz.entity.util.EntityQuery"));
        } catch (TemplateModelException e) {
            Debug.logError(e, module);
        }
        newConfig.setLocalizedLookup(false);
        newConfig.setSharedVariable("StringUtil", new BeanModel(StringUtil.INSTANCE, wrapper));
        TemplateLoader[] templateLoaders = {new FlexibleTemplateLoader(), new StringTemplateLoader()};
        MultiTemplateLoader multiTemplateLoader = new MultiTemplateLoader(templateLoaders);
        newConfig.setTemplateLoader(multiTemplateLoader);
        Map<?, ?> freemarkerImports = UtilProperties.getProperties("freemarkerImports");
        if (freemarkerImports != null) {
            newConfig.setAutoImports(freemarkerImports);
        }
        newConfig.setLogTemplateExceptions(false);
        boolean verboseTemplate = ModelWidget.widgetBoundaryCommentsEnabled(null)
                || UtilProperties.getPropertyAsBoolean("widget", "widget.freemarker.template.verbose", false);
        newConfig.setTemplateExceptionHandler(verboseTemplate
                ? OFBizTemplateExceptionHandler.OFBIZ_DEBUG_HANDLER
                : OFBizTemplateExceptionHandler.OFBIZ_DEFAULT_HANDLER);
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
            resources = loader.getResources("freemarkerTransforms.properties");
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

    private static void loadTransforms(ClassLoader loader, Properties props, Configuration config) {
        for (Object object : props.keySet()) {
            String key = (String) object;
            String className = props.getProperty(key);
            if (Debug.verboseOn()) {
                Debug.logVerbose("Adding FTL Transform " + key + " with class " + className, module);
            }
            try {
                config.setSharedVariable(key, loader.loadClass(className).getDeclaredConstructor().newInstance());
            } catch (Exception e) {
                Debug.logError(e, "Could not pre-initialize dynamically loaded class: " + className + ": " + e, module);
            }
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

    public static void renderTemplateFromString(String templateName, String templateString, Map<String, Object> context, Appendable outWriter, long lastModificationTime, boolean useCache) throws TemplateException, IOException {
        Template template = null;
        if (useCache) {
            template = cachedTemplates.get(templateName);
        }
        if (template == null) {
            StringTemplateLoader stringTemplateLoader = (StringTemplateLoader)((MultiTemplateLoader)defaultOfbizConfig.getTemplateLoader()).getTemplateLoader(1);
            Object templateSource = stringTemplateLoader.findTemplateSource(templateName);
            if (templateSource == null || stringTemplateLoader.getLastModified(templateSource) < lastModificationTime) {
                stringTemplateLoader.putTemplate(templateName, templateString, lastModificationTime);
            }
        }

        template = getTemplate(templateName);
        renderTemplate(template, context, outWriter);
    }

    public static void clearTemplateFromCache(String templateLocation) {
        cachedTemplates.remove(templateLocation);
        try {
            defaultOfbizConfig.removeTemplateFromCache(templateLocation);
        } catch(Exception e) {
            Debug.logInfo("Template not found in Fremarker cache with name: " + templateLocation, module);
        }
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
    private static void applyUserSettings(Environment env, Map<String, Object> context) {
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

    /**
     * Gets a Template instance from the template cache. If the Template instance isn't
     * found in the cache, then one will be created.
     * @param templateLocation Location of the template - file path or URL
     */
    public static Template getTemplate(String templateLocation) throws IOException {
        return getTemplate(templateLocation, cachedTemplates, defaultOfbizConfig);
    }

    public static Template getTemplate(String templateLocation, UtilCache<String, Template> cache, Configuration config) throws IOException {
        Template template = cache.get(templateLocation);
        if (template == null) {
            template = config.getTemplate(templateLocation);
            template = cache.putIfAbsentAndGet(templateLocation, template);
        }
        return template;
    }

    public static String getArg(Map<String, ? extends Object> args, String key, Environment env) {
        Map<String, ? extends Object> templateContext = FreeMarkerWorker.getWrappedObject("context", env);
        return getArg(args, key, templateContext);
    }

    public static String getArg(Map<String, ? extends Object> args, String key, Map<String, ? extends Object> templateContext) {
        Object o = args.get(key);
        String returnVal = (String) unwrap(o);
        if (returnVal == null) {
            try {
                if (templateContext != null) {
                    returnVal = (String) templateContext.get(key);
                }
            } catch (ClassCastException e2) {
                Debug.logInfo(e2.getMessage(), module);
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
        }
        return returnObj;
    }

    @SuppressWarnings("unchecked")
    public static <T> T unwrap(Object o) {
        Object returnObj;

        if (o == TemplateModel.NOTHING) {
            returnObj = null;
        } else if (o instanceof SimpleScalar) {
            returnObj = o.toString();
        } else if (o instanceof BeanModel) {
            returnObj = ((BeanModel) o).getWrappedObject();
        } else {
            returnObj = null;
        }

        return (T) returnObj;
    }

    public static Map<String, Object> createEnvironmentMap(Environment env) {
        Map<String, Object> templateRoot = new HashMap<>();
        Set<String> varNames = null;
        try {
            varNames = UtilGenerics.cast(env.getKnownVariableNames());
        } catch (TemplateModelException e1) {
            Debug.logError(e1, "Error getting FreeMarker variable names, will not put pass current context on to sub-content", module);
        }
        if (varNames != null) {
            for (String varName: varNames) {
                templateRoot.put(varName, FreeMarkerWorker.getWrappedObject(varName, env));
            }
        }
        return templateRoot;
    }

    public static void saveContextValues(Map<String, Object> context, String [] saveKeyNames, Map<String, Object> saveMap) {
        for (String key: saveKeyNames) {
            Object o = context.get(key);
            if (o instanceof Map<?, ?>) {
                o = UtilMisc.makeMapWritable(UtilGenerics.cast(o));
            } else if (o instanceof List<?>) {
                o = UtilMisc.makeListWritable(UtilGenerics.checkList(o));
            }
            saveMap.put(key, o);
        }
    }

    public static Map<String, Object> saveValues(Map<String, Object> context, String [] saveKeyNames) {
        Map<String, Object> saveMap = new HashMap<>();
        for (String key: saveKeyNames) {
            Object o = context.get(key);
            if (o instanceof Map<?, ?>) {
                o = UtilMisc.makeMapWritable(UtilGenerics.cast(o));
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
                context.put(key, UtilMisc.makeMapWritable(UtilGenerics.cast(o)));
            } else if (o instanceof List<?>) {
                List<Object> list = new ArrayList<>();
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

    /*
     * Custom TemplateLoader for Freemarker to locate templates by resource identifier
     * following the format:
     *  component://componentname/path/to/some/file.ftl
     */
    static class FlexibleTemplateLoader extends URLTemplateLoader {
        @Override
        protected URL getURL(String name) {
            if (name != null && name.startsWith("delegator:")) {
                return null; // this is a template stored in the database
            }
            URL locationUrl = null;
            try {
                locationUrl = FlexibleLocation.resolveLocation(name);
            } catch (Exception e) {
                Debug.logWarning("Unable to locate the template: " + name, module);
            }
            return locationUrl != null && new File(locationUrl.getFile()).exists()? locationUrl: null;
        }
    }

    /**
     * OFBiz specific {@link TemplateExceptionHandler} interface.
     */
    interface OFBizTemplateExceptionHandler {

        /**
         * {@link TemplateExceptionHandler} that suppresses the exception and keep the rendering going on.
         * It sanitizes any messages present in the stack trace prior to printing to the output writer.
         */
        TemplateExceptionHandler OFBIZ_DEBUG_HANDLER = (te, env, out) -> {
            try {
                out.write(te.getMessage());
                Debug.logError(te, module);
            } catch (IOException e) {
                Debug.logError(e, module);
            }
        };

        /**
         * {@link TemplateExceptionHandler} that suppresses the exception and replace by a generic char for quiet alert.
         * As mentioned in the doc, the stack trace is still logged {@link TemplateExceptionHandler#IGNORE_HANDLER}
         */
        TemplateExceptionHandler OFBIZ_DEFAULT_HANDLER = (te, env, out) -> {
            try {
                out.write(UtilProperties.getPropertyValue("widget", "widget.freemarker.template.exception.message","∎"));
                Debug.logError(te, module);
            } catch (IOException e) {
                Debug.logError(e, module);
            }
        };
    }
}
