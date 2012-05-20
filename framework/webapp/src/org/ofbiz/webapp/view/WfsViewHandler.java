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
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javolution.util.FastMap;

import org.ofbiz.base.location.FlexibleLocation;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.template.FreeMarkerWorker;
import org.ofbiz.entity.GenericValue;

import freemarker.template.Configuration;
import freemarker.template.SimpleSequence;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * WfsViewHandler - View Handler
 */
public class WfsViewHandler extends AbstractViewHandler {

    public static final String module = WfsViewHandler.class.getName();
    public static final String FormatTemplateUrl ="component://webapp/script/org/ofbiz/webapp/event/formatWfs.ftl";

    protected ServletContext context;

    public void init(ServletContext context) throws ViewHandlerException {
        this.context = context;
    }

    public void render(String name, String page, String info, String contentType, String encoding, HttpServletRequest request, HttpServletResponse response) throws ViewHandlerException {
        // some containers call filters on EVERY request, even forwarded ones,
        // so let it know that it came from the control servlet

        if (request == null)
            throw new ViewHandlerException("Null HttpServletRequest object");
        if (UtilValidate.isEmpty(page))
            throw new ViewHandlerException("Null or empty source");

        if (Debug.infoOn()) Debug.logInfo("Retreiving HTTP resource at: " + page, module);
        try {
            String result = null;

            List<GenericValue> entityList = UtilGenerics.cast(request.getAttribute("entityList"));
            SimpleSequence simpleList = new SimpleSequence(entityList);
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("entityList", simpleList);
            StringWriter outWriter = new StringWriter();
            Template template = getDocTemplate(page);
            template.process(ctx, outWriter);
            outWriter.close();
            result = outWriter.toString();
            Debug.logInfo(result, result);
            response.getWriter().print(result);
        } catch (FileNotFoundException e) {
            throw new ViewHandlerException(e.getMessage(), e);
        } catch (IOException e) {
            throw new ViewHandlerException("IO Error in view", e);
        } catch (URISyntaxException e) {
            throw new ViewHandlerException(e.getMessage(), e);
        } catch (TemplateException e) {
            throw new ViewHandlerException(e.getMessage(), e);
        }
    }

    public static Template getDocTemplate(String fileUrl)  throws FileNotFoundException, IOException, TemplateException, URISyntaxException {
        Template template = null;
        URL screenFileUrl = FlexibleLocation.resolveLocation(fileUrl, null);
        String urlStr = screenFileUrl.toString();
        URI uri = new URI(urlStr);
        File f = new File(uri);
        FileReader templateReader = new FileReader(f);
        Configuration conf = makeDefaultOfbizConfig();
        template = new Template("FMImportFilter", templateReader, conf);
        return template;
    }

    public static Configuration makeDefaultOfbizConfig() throws TemplateException, IOException {
        Configuration config = new Configuration();
        config.setObjectWrapper(FreeMarkerWorker.getDefaultOfbizWrapper());
        config.setSetting("datetime_format", "yyyy-MM-dd HH:mm:ss.SSS");
        Configuration defaultOfbizConfig = config;
        return defaultOfbizConfig;
    }
}
