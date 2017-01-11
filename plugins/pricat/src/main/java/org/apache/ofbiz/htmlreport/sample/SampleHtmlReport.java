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
package org.apache.ofbiz.htmlreport.sample;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.htmlreport.AbstractHtmlReport;
import org.apache.ofbiz.htmlreport.InterfaceReportThread;

/**
 * Provides a report for running sample html report.
 * 
 */
public class SampleHtmlReport extends AbstractHtmlReport {
    
    /**
     * Public constructor with report variables.<p>
     * 
     * @param request the HttpServletRequest request
     * @param response the HttpServletResponse response
     */
    public SampleHtmlReport(HttpServletRequest request, HttpServletResponse response) {

        super(request, response);
    }
    
    public static SampleHtmlReport getReport(HttpServletRequest request, HttpServletResponse response) {
        
        SampleHtmlReport wp = (SampleHtmlReport) request.getAttribute(SESSION_REPORT_CLASS);
        if (wp == null) {
            wp = new SampleHtmlReport(request, response);
            request.setAttribute(SESSION_REPORT_CLASS, wp);
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
            if (threadInstance instanceof SampleHtmlThread) {
                thread = (InterfaceReportThread) threadInstance;
                break;
            }
        }

        if (thread == null) {
            thread = new SampleHtmlThread(request, response, name.toLowerCase());
        }
        return thread;
    }
}
