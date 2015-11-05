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
package org.ofbiz.birt.webapp.view;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.report.engine.api.IReportEngine;
import org.eclipse.birt.report.engine.api.IReportRunnable;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.birt.BirtFactory;
import org.ofbiz.birt.BirtWorker;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.util.EntityUtilProperties;
import org.ofbiz.webapp.view.ViewHandler;
import org.ofbiz.webapp.view.ViewHandlerException;
import org.xml.sax.SAXException;

public class BirtViewHandler implements ViewHandler {

    public static final String module = BirtViewHandler.class.getName();

    protected ServletContext servletContext = null;

    private String name = "birt";

    public void init(ServletContext context) throws ViewHandlerException {
        this.servletContext = context;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void render(String name, String page, String info,
            String contentType, String encoding, HttpServletRequest request,
            HttpServletResponse response) throws ViewHandlerException {
        
        try {
            IReportEngine engine = org.ofbiz.birt.BirtFactory.getReportEngine();
            // open report design
            IReportRunnable design = null;
            if (page.startsWith("component://")) {
                InputStream reportInputStream = BirtFactory.getReportInputStreamFromLocation(page);
                design = engine.openReportDesign(reportInputStream);
            } else {
                design = engine.openReportDesign(servletContext.getRealPath(page));
            }
            
            Map<String, Object> appContext = UtilGenerics.cast(engine.getConfig().getAppContext());
            BirtWorker.setWebContextObjects(appContext, request, response);

            Map<String, Object> context = new HashMap<String, Object>();
            // set parameters from request
             Map<String, Object> parameters = UtilGenerics.cast(request.getAttribute(BirtWorker.BIRT_PARAMETERS));
            if (parameters != null) {
                context.put(BirtWorker.BIRT_PARAMETERS, parameters);
            } else {
                context.put(BirtWorker.BIRT_PARAMETERS, UtilHttp.getParameterMap(request));
            }
            // set locale from request
            Locale locale = (Locale)request.getAttribute(BirtWorker.BIRT_LOCALE);
            if (locale == null) {
                locale = UtilHttp.getLocale(request);
            }
            
            // set output file name
            String outputFileName = (String) request.getAttribute(BirtWorker.BIRT_OUTPUT_FILE_NAME);
            if (UtilValidate.isNotEmpty(outputFileName)) {
                UtilHttp.setContentDisposition(response, outputFileName);
            }
            
            // set override content type
            String overrideContentType = (String) request.getAttribute(BirtWorker.BIRT_CONTENT_TYPE);
            if (UtilValidate.isNotEmpty(overrideContentType)) {
                contentType = overrideContentType;
            }
            
            context.put(BirtWorker.BIRT_LOCALE, locale);
            Delegator delegator = (Delegator) request.getAttribute("delegator");
            String birtImageDirectory = EntityUtilProperties.getPropertyValue("birt", "birt.html.image.directory", delegator);
            context.put(BirtWorker.BIRT_IMAGE_DIRECTORY, birtImageDirectory);
            BirtWorker.exportReport(design, context, contentType, response.getOutputStream());
        } catch (BirtException e) {
            throw new ViewHandlerException("Birt Error create engine: " + e.toString(), e);
        } catch (IOException e) {
            throw new ViewHandlerException("Error in the response writer/output stream: " + e.toString(), e);
        } catch (SQLException e) {
            throw new ViewHandlerException("get connection error: " + e.toString(), e);
        } catch (GenericEntityException e) {
            throw new ViewHandlerException("generic entity error: " + e.toString(), e);
        } catch (GeneralException e) {
            throw new ViewHandlerException("general error: " + e.toString(), e);
        } catch (SAXException se) {
            String errMsg = "Error SAX rendering " + page + " view handler: " + se.toString();
            Debug.logError(se, errMsg, module);
            throw new ViewHandlerException(errMsg, se);
        } catch (ParserConfigurationException pe) {
            String errMsg = "Error parser rendering " + page + " view handler: " + pe.toString();
            Debug.logError(pe, errMsg, module);
            throw new ViewHandlerException(errMsg, pe);
        }
    }
}
