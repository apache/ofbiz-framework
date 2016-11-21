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
package org.apache.ofbiz.htmlreport;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;

import org.apache.ofbiz.htmlreport.util.ReportStringUtil;

/**
 * HTML report output to be used for database create tables / drop tables operations.
 * 
 */
public abstract class AbstractHtmlReport extends HtmlReport {
	
	public static final String module = AbstractHtmlReport.class.getName();

	public final static String THREAD_TYPE = "thread_type";
	
	public final static String RUN_CREATETABLE_SCRIPT = "runcreatetablescript";
	
	public final static String RUN_DROPTABLE_SCRIPT = "rundroptablescript";
	
	public final static String FILE_REPORT_OUTPUT = "specialpurpose/pricat/webapp/pricat/ftl/report.ftl";
	
    /**
     * Constructs a new report using the provided locale for the output language.
     * 
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     */
    public AbstractHtmlReport(HttpServletRequest request, HttpServletResponse response) {
        this(request, response, false, false);
    }

    /**
     * Constructs a new report using the provided locale for the output language.
     *  
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @param writeHtml if <code>true</code>, this report should generate HTML instead of JavaScript output
     * @param isTransient If set to <code>true</code> nothing is kept in memory
     */
    public AbstractHtmlReport(HttpServletRequest request, HttpServletResponse response, boolean writeHtml, boolean isTransient) {
        super(request, response, writeHtml, isTransient);
    }
    
    /**
     * Prepare display an html report.<p>
     * 
     * @throws JspException if dialog actions fail
     * @throws IOException 
     * @throws ServletException 
     * @throws TemplateException 
     * @throws IOException 
     */
    public void prepareDisplayReport(HttpServletRequest request, HttpServletResponse response, String name, String dialogUri) throws IOException {

    	if (ReportStringUtil.isNotEmpty(dialogUri)) {
    		setDialogRealUri(request, dialogUri);
    	}
        
        String action = getParamAction(request);
        if (action == null) action = "";
        if (action.equals("reportend") || action.equals("cancel")) {
            setParamAction("reportend");
        } else if (action.equals("reportupdate")) {
            setParamAction("reportupdate");
        } else {
            InterfaceReportThread thread = initializeThread(request, response, name);
            thread.start();
            setParamAction("reportbegin");
            setParamThread(thread.getUUID().toString());
        }
    }

    /**
     * Initializes the report thread to use for this report.<p>
     * 
     * @return the reported thread to use for this report.
     */
    public abstract InterfaceReportThread initializeThread(HttpServletRequest request, HttpServletResponse response, String name);

    /**
     * Set the report dialog uri.
     * 
     * @param dialogUri
     */
    public void setDialogRealUri(HttpServletRequest request, String dialogUri) {
    	request.setAttribute(DIALOG_URI, dialogUri);
    }

    public static String checkButton(HttpServletRequest request, HttpServletResponse response) {
    	String action = request.getParameter("action");
    	if (ReportStringUtil.isNotEmpty(action)) {
    		if (action.equalsIgnoreCase("ok")) {
    			request.removeAttribute(SESSION_REPORT_CLASS);
    			request.removeAttribute(DIALOG_URI);
    			return "ok";
    		} else if (action.equalsIgnoreCase("cancel")) {
    			request.removeAttribute(SESSION_REPORT_CLASS);
    			request.removeAttribute(DIALOG_URI);
    			return "cancel";
    		}
    	}
    	action = request.getParameter("ok");
    	if (ReportStringUtil.isNotEmpty(action)) {
    		if (action.equalsIgnoreCase("ok")) {
    			request.removeAttribute(SESSION_REPORT_CLASS);
    			request.removeAttribute(DIALOG_URI);
    			return "ok";
    		}
    	}
        action = request.getParameter("cancel");
        if (ReportStringUtil.isNotEmpty(action)) {
        	if (action.equalsIgnoreCase("cancel")) {
    			request.removeAttribute(SESSION_REPORT_CLASS);
    			request.removeAttribute(DIALOG_URI);
        		return "cancel";
        	}
        }
        
    	return "success";
    }
}