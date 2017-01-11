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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilProperties;

import org.apache.ofbiz.htmlreport.AbstractReportThread;
import org.apache.ofbiz.htmlreport.InterfaceReport;

/**
 * Thread for running sample html report.
 *  
 */
public class SampleHtmlThread extends AbstractReportThread {

    public static final String COUNT_DOWN = "countdown";
    
    public static final String COUNT_UP = "countup";
    
    public static final String CONFIRM = "confirm_action";
    
    public static final String[] messageLables = new String[] {"FORMAT_DEFAULT", "FORMAT_WARNING", "FORMAT_HEADLINE", "FORMAT_NOTE", "FORMAT_OK", "FORMAT_ERROR", "FORMAT_THROWABLE"};
    
    public static final List<String> messages = Collections.unmodifiableList(Arrays.asList(messageLables));
    
    private static final String resource = "PricatUiLabels";
    
    /**
     * Constructor, creates a new HtmlImportThreat.
     * 
     */
    public SampleHtmlThread(HttpServletRequest request, HttpServletResponse response, String name) {
        super(request, response, name);
        initHtmlReport(request, response);
    }

    public String getReportUpdate() {
        return getReport().getReportUpdate();
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {
        try {
            if (getName().startsWith(COUNT_DOWN)) {
                getReport().println(UtilProperties.getMessage(resource, "START_COUNT_DOWN", getLocale()), InterfaceReport.FORMAT_HEADLINE);
                Random random = new Random();
                int j = 0;
                for (int i=1000; i>0; i--) {
                    sleep(20);
                    j = random.nextInt(7);
                    if (j == 6) {
                        getReport().println(new Throwable(UtilProperties.getMessage(resource, messages.get(j), new Object[] {i}, getLocale())));
                    } else {
                        getReport().println(UtilProperties.getMessage(resource, messages.get(j), new Object[] {i}, getLocale()), j);
                    }
                }
                getReport().println(UtilProperties.getMessage(resource, "COUNT_COMPLETED", getLocale()), InterfaceReport.FORMAT_HEADLINE);
            } else if (getName().startsWith(COUNT_UP)) {
                getReport().println(UtilProperties.getMessage(resource, "START_COUNT_UP", getLocale()), InterfaceReport.FORMAT_HEADLINE);
                Random random = new Random();
                int j = 0;
                for (int i=1; i<=1000; i++) {
                    sleep(20);
                    j = random.nextInt(7);
                    if (j == 6) {
                        getReport().println(new Throwable(UtilProperties.getMessage(resource, messages.get(j), new Object[] {i}, getLocale())));
                    } else {
                        getReport().println(UtilProperties.getMessage(resource, messages.get(j), new Object[] {i}, getLocale()), j);
                    }
                }
                getReport().println(UtilProperties.getMessage(resource, "COUNT_COMPLETED", getLocale()), InterfaceReport.FORMAT_HEADLINE);
            } else {
                getReport().println(getName(), InterfaceReport.FORMAT_ERROR);
            }
        } catch (Exception e) {
            getReport().println(e);
            if (Debug.errorOn()) {
                Debug.log(e);
            }
        }
    }
}
