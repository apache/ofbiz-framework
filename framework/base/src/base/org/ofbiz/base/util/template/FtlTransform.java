/*
 * $Id: FtlTransform.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2005 The Open For Business Project - www.ofbiz.org
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
 */
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
 * @author     <a href="mailto:byersa@automationgroups.com">Al Byers</a>
 * @version    $Rev$
 * @since      3.2
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
        return; 
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
