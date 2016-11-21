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
package org.apache.ofbiz.pricat;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.ofbiz.htmlreport.AbstractHtmlReport;
import org.apache.ofbiz.htmlreport.InterfaceReportThread;
import org.apache.ofbiz.htmlreport.util.ReportStringUtil;

/**
 * Provides a html report for running parse a PriCat file.<p> 
 * 
 */
public class PricatParseExcelHtmlReport extends AbstractHtmlReport {
	
	public static final String PRICAT_REPORT_CLASS = "PRICAT_HTML_REPORT";
	
    /**
     * Public constructor with report variables.<p>
     * 
     * @param req the HttpServletRequest request
     * @param res the HttpServletResponse response
     */
    public PricatParseExcelHtmlReport(HttpServletRequest request, HttpServletResponse response) {

        super(request, response, true, true);
    }
    
    public static PricatParseExcelHtmlReport getReport(HttpServletRequest request, HttpServletResponse response) {
    	
    	PricatParseExcelHtmlReport wp = (PricatParseExcelHtmlReport) request.getAttribute(PRICAT_REPORT_CLASS);
    	if (wp == null) {
    		wp = new PricatParseExcelHtmlReport(request, response);
    		request.setAttribute(PRICAT_REPORT_CLASS, wp);
    	}
    	return wp;
    }
    
    public InterfaceReportThread initializeThread(HttpServletRequest request, HttpServletResponse response, String name) {

		if (name == null) {
			name = "";
		}
        ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
        int i = threadGroup.activeCount();
        Thread[] threads = new Thread[i];
        threadGroup.enumerate(threads, true);
        InterfaceReportThread thread = null;
        for (int j=0; j<threads.length; j++) {
        	Thread threadInstance = threads[j];
        	if (threadInstance instanceof PricatParseExcelHtmlThread) {
        		thread = (PricatParseExcelHtmlThread) threadInstance;
        		break;
        	}
        }
        if (thread == null) {
            thread = new PricatParseExcelHtmlThread(request, response, name);
        }

        return thread;
    }

    public static String checkButton(HttpServletRequest request, HttpServletResponse response) {
    	String action = request.getParameter("action");
    	if (ReportStringUtil.isNotEmpty(action)) {
    		if (action.equalsIgnoreCase("ok")) {
    			request.removeAttribute(PRICAT_REPORT_CLASS);
    			request.removeAttribute(DIALOG_URI);
    			return "ok";
    		} else if (action.equalsIgnoreCase("cancel")) {
    			request.removeAttribute(PRICAT_REPORT_CLASS);
    			request.removeAttribute(DIALOG_URI);
    			return "cancel";
    		}
    	}
    	action = request.getParameter("ok");
    	if (ReportStringUtil.isNotEmpty(action)) {
    		if (action.equalsIgnoreCase("ok")) {
    			request.removeAttribute(PRICAT_REPORT_CLASS);
    			request.removeAttribute(DIALOG_URI);
    			return "ok";
    		}
    	}
        action = request.getParameter("cancel");
        if (ReportStringUtil.isNotEmpty(action)) {
        	if (action.equalsIgnoreCase("cancel")) {
    			request.removeAttribute(PRICAT_REPORT_CLASS);
    			request.removeAttribute(DIALOG_URI);
        		return "cancel";
        	}
        }
        
    	return "success";
    }

    /**
     * Performs the dialog actions depending on the initialized action.<p>
     * 
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
            setDialogRealUri(request, dialogUri);
        } else if (action.equals("reportupdate")) {
            setParamAction("reportupdate");
        } else {
            InterfaceReportThread thread = initializeThread(request, response, name);
            thread.start();
            setParamAction("reportbegin");
            setParamThread(thread.getUUID().toString());
        }
    }
}
