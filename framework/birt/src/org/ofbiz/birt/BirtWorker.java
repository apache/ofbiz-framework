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
package org.ofbiz.birt;

import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;

import org.eclipse.birt.report.engine.api.EXCELRenderOption;
import org.eclipse.birt.report.engine.api.EngineException;
import org.eclipse.birt.report.engine.api.HTMLRenderOption;
import org.eclipse.birt.report.engine.api.HTMLServerImageHandler;
import org.eclipse.birt.report.engine.api.IPDFRenderOption;
import org.eclipse.birt.report.engine.api.IReportEngine;
import org.eclipse.birt.report.engine.api.IReportRunnable;
import org.eclipse.birt.report.engine.api.IRunAndRenderTask;
import org.eclipse.birt.report.engine.api.PDFRenderOption;
import org.eclipse.birt.report.engine.api.RenderOption;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.birt.container.BirtContainer;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.jdbc.ConnectionFactory;

public class BirtWorker {
    
    public final static String module = BirtWorker.class.getName();
    
    public final static String BIRT_PARAMETERS = "birtParameters";
    public final static String REPORT_ENGINE = "reportEngine";
    public final static String BIRT_LOCALE = "birtLocale";
    public final static String BIRT_IMAGE_DIRECTORY = "birtImageDirectory";
    public final static String BIRT_CONTENT_TYPE = "birtContentType";
    
    private static HTMLServerImageHandler imageHandler = new HTMLServerImageHandler();
    
    /**
     * export report
     * @param design
     * @param context
     * @param contentType
     * @param output
     * @throws EngineException
     * @throws GeneralException
     * @throws SQLException
     */
    public static void exportReport(IReportRunnable design, Map context, String contentType, OutputStream output)
        throws EngineException, GeneralException, SQLException {
    
        Locale birtLocale = (Locale)context.get(BIRT_LOCALE);
        String birtImageDirectory = (String)context.get(BIRT_IMAGE_DIRECTORY);

        if (contentType == null) {
            contentType = "text/html";
        }
        if (birtImageDirectory == null) {
             birtImageDirectory = "/";
        }
        Debug.logInfo("Get report engine", module);
        IReportEngine engine = BirtContainer.getReportEngine();
    
        /*
        --- DISABLE JDBC FEATURE
        // set the jdbc connection
        String delegatorGroupHelperName = BirtContainer.getDelegatorGroupHelperName();
        Delegator delegator = BirtContainer.getDelegator();
        Debug.logInfo("Get the JDBC connection from group helper's name:" + delegatorGroupHelperName, module);
        String helperName = delegator.getGroupHelperName(delegatorGroupHelperName);    // gets the helper (localderby, localmysql, localpostgres, etc.) for your entity group org.ofbiz
        Connection connection = ConnectionFactory.getConnection(helperName); 
        engine.getConfig().getAppContext().put("OdaJDBCDriverPassInConnection", connection);
        */
        
        IRunAndRenderTask task = engine.createRunAndRenderTask(design);
        if (birtLocale != null) {
            Debug.logInfo("Set birt locale:" + birtLocale, module);
            task.setLocale(birtLocale);
        }
        
        // set parameters if exists
        Map parameters = (Map)context.get(BirtWorker.BIRT_PARAMETERS);
        if (parameters != null) {
            Debug.logInfo("Set birt parameters:" + parameters, module);
            task.setParameterValues(parameters);
        }
         
        // set output options
        RenderOption options = new RenderOption();
        if ("text/html".equalsIgnoreCase(contentType)) {
            options.setOutputFormat(RenderOption.OUTPUT_FORMAT_HTML);
        } else if ("application/pdf".equalsIgnoreCase(contentType)) {
            options.setOutputFormat(RenderOption.OUTPUT_FORMAT_PDF);
        } else if ("application/vnd.ms-word".equalsIgnoreCase(contentType)) {
            options.setOutputFormat("doc");
        }  else if ("application/vnd.ms-excel".equalsIgnoreCase(contentType)) {
            options.setOutputFormat("xls");
        } else if ("application/vnd.ms-powerpoint".equalsIgnoreCase(contentType)) {
            options.setOutputFormat("ppt");
        } else {
            throw new GeneralException("Unknown content type : " + contentType);
        }
        
        if (options.getOutputFormat().equalsIgnoreCase(RenderOption.OUTPUT_FORMAT_HTML)) {
            // set html render options
            HTMLRenderOption htmlOptions = new HTMLRenderOption(options);
            htmlOptions.setImageDirectory(birtImageDirectory);
            htmlOptions.setBaseImageURL(birtImageDirectory);
            options.setImageHandler(imageHandler);
        } else if (options.getOutputFormat().equalsIgnoreCase(RenderOption.OUTPUT_FORMAT_PDF)) {
            // set pdf render options
            PDFRenderOption pdfOptions = new PDFRenderOption(options);
            pdfOptions.setOption(IPDFRenderOption.PAGE_OVERFLOW, new Boolean(true) );
        } else if (options.getOutputFormat().equalsIgnoreCase("xls")) {
            // set excel render options
            EXCELRenderOption excelOptions = new EXCELRenderOption(options);
        } 
        options.setOutputStream(output);
        task.setRenderOption(options);
        
        // run report
        Debug.logInfo("Birt's locale is: " + task.getLocale(), module);
        Debug.logInfo("Run report's task", module);
        task.run();
        task.close();
    }
}
