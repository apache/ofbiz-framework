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

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;
import java.net.URL;
import java.net.URLConnection;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.ofbiz.base.util.URLConnector;
import org.ofbiz.base.util.cache.UtilCache;
import org.ofbiz.base.location.FlexibleLocation;
import java.io.IOException;
import java.io.Writer;

import freemarker.ext.beans.BeansWrapper;
import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.WrappingTemplateModel;
import freemarker.template.Configuration;


/**
 * FtlTransform
 * 
 * This utility takes the URL to a Freemarker template and some parameters
 * and runs the template and writes to the writer that was passed.
 * It keeps its own cache for storing the fetched template.
 * 
 */
public final class FtlTransform {

    public static final String module = FtlTransform.class.getName();
    public static UtilCache ftlTemplatesCache = new UtilCache("FtlTemplates", 0, 0);
    private static Configuration cfg = new Configuration();
    private static Map templateRoot = new HashMap();

    static {
        
        WrappingTemplateModel.setDefaultObjectWrapper(BeansWrapper.getDefaultInstance());
        try {        
            cfg.setObjectWrapper(BeansWrapper.getDefaultInstance());
            cfg.setSetting("datetime_format", "yyyy-MM-dd HH:mm:ss.SSS");
        } catch (TemplateException e) {
            throw new RuntimeException("Freemarker TemplateException", e.getCause());
        }        
        prepOfbizRoot(templateRoot);
    }
    
    public FtlTransform() {
    }
 
    public static void transform(Writer writer, String path, Map params) 
        throws TemplateException, IOException {
        
        Template template = (Template)ftlTemplatesCache.get(path);
        if (template == null) {
            template = getTemplate(path);
            if (template != null) {
                ftlTemplatesCache.put(path, template);   
            }
        }
        
        SimpleHash templateContext = new SimpleHash();
        templateContext.putAll(templateRoot);
    	if (params != null) {
           Set entrySet = params.entrySet(); 
           Iterator iter = entrySet.iterator();
           while (iter.hasNext()) {
           	    Map.Entry entry = (Map.Entry)iter.next(); 
           	    String key = (String)entry.getKey();
                Object val = entry.getValue();
                if (val != null) {
                    templateContext.put(key, val);
                }
           }
        }
        template.process(templateContext, writer);
    }
    
    private static Template getTemplate( String inputUrl) throws IOException {
        
        URL url = FlexibleLocation.resolveLocation(inputUrl);
        URLConnection conn = URLConnector.openConnection(url);
        InputStream in = conn.getInputStream();
        InputStreamReader rdr = new InputStreamReader(in);
        Template template  = new Template(inputUrl, rdr, cfg);
        template.setObjectWrapper(BeansWrapper.getDefaultInstance());
        return template;
    }
    
    public static void prepOfbizRoot(Map root) {
        FreeMarkerWorker.addAllOfbizTransforms(root);
    }
}
