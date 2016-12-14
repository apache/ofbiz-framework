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

import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.UtilHttp;
import org.safehaus.uuid.EthernetAddress;
import org.safehaus.uuid.UUID;
import org.safehaus.uuid.UUIDGenerator;

/** 
 * Provides a common Thread class for the reports.
 * 
 */
public abstract class AbstractReportThread extends Thread implements InterfaceReportThread {

    /** Indicates if the thread was already checked by the grim reaper. */
    private boolean doomed;
    
    /** The report that belongs to the thread. */
    private InterfaceReport report;

    /** The time this report is running. */
    private long startTime;
    
    private UUID uuid;

    private Locale locale;

    /**
     * Constructs a new report Thread with the given name.
     */
    protected AbstractReportThread(HttpServletRequest request, HttpServletResponse response, String name) {

        super(Thread.currentThread().getThreadGroup(), name);
        // report Threads are never daemon Threads
        setDaemon(false);
        // the session must not be updated when it is used in a report
        EthernetAddress ethernetAddress = UUIDGenerator.getInstance().getDummyAddress();
        uuid = UUIDGenerator.getInstance().generateTimeBasedUUID(ethernetAddress);

        setName(name + " [" + uuid.toString() + "]");
        // new Threads are not doomed
        doomed = false;
        // set start time
        startTime = System.currentTimeMillis();
        locale = UtilHttp.getLocale(request);
    }
    
    
    public UUID getUUID() {
        return uuid;
    }

    /**
     * Adds an error object to the list of errors that occured during the report.
     * 
     * @param obj the error object
     */
    public void addError(Object obj) {

        if (getReport() != null) {
            getReport().addError(obj);
        }
    }

    /**
     * Returns the error exception in case there was an error during the execution of
     * this Thread, null otherwise.
     * 
     * @return the error exception in case there was an error, null otherwise
     */
    public Throwable getError() {

        return null;
    }

    /**
     * Returns a list of all errors that occured during the report.
     * 
     * @return an error list that occured during the report
     */
    public List<?> getErrors() {

        if (getReport() != null) {
            return getReport().getErrors();
        } else {
            return null;
        }
    }

    /**
     * Returns the part of the report that is ready for output.
     * 
     * @return the part of the report that is ready for output
     */
    public abstract String getReportUpdate();

    /** 
     * Returns the time this report has been running.
     * 
     * @return the time this report has been running
     */
    public synchronized long getRuntime() {

        if (doomed) {
            return startTime;
        } else {
            return System.currentTimeMillis() - startTime;
        }
    }

    /**
     * Returns if the report generated an error output.
     * 
     * @return true if the report generated an error, otherwise false
     */
    public boolean hasError() {

        if (getReport() != null) {
            return (getReport().getErrors().size() > 0);
        } else {
            return false;
        }
    }

    /**
     * Returns true if this thread is already "doomed" to be deleted.
     * 
     * A OFBiz deamon Thread (the "Grim Reaper") will collect all 
     * doomed Threads, i.e. threads that are not longer active for some
     * time.
     * 
     * @return true if this thread is already "doomed" to be deleted
     */
    public synchronized boolean isDoomed() {

        if (isAlive()) {
            // as long as the Thread is still active it is never doomed
            return false;
        }
        if (doomed) {
            // not longer active, and already doomed, so rest in peace...
            return true;
        }
        // condemn the Thread to be collected by the grim reaper next time  
        startTime = getRuntime();
        doomed = true;
        return false;
    }

    /**
     * Returns the report where the output of this Thread is written to.
     * 
     * @return the report where the output of this Thread is written to
     */
    protected InterfaceReport getReport() {

        return report;
    }

    /**
     * Initialize a HTML report for this Thread.
     * 
     */
    protected void initHtmlReport(HttpServletRequest request, HttpServletResponse response) {

        report = HtmlReport.getInstance(request, response);
        ((HtmlReport) report).setParamThread(getUUID().toString());
    }
    
    /**
     * Initialize a HTML report for this Thread.
     * 
     */
    protected void initHtmlReport(HttpServletRequest request, HttpServletResponse response, boolean writeHtml, boolean isTransient) {

        report = HtmlReport.getInstance(request, response, writeHtml, isTransient);
        ((HtmlReport) report).setParamThread(getUUID().toString());
    }
    
    protected void initHtmlReport(HttpServletRequest request, HttpServletResponse response, boolean writeHtml, boolean isTransient, String logFileName) {

        report = HtmlReport.getInstance(request, response, writeHtml, isTransient, logFileName);
        ((HtmlReport) report).setParamThread(getUUID().toString());
    }
    
    protected Locale getLocale() {
        return locale;
    }

}
