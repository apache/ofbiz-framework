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

/** 
 * This is the interface for the report classes which are used for the output
 * during operations that run on a separate Thread in OFBiz,
 * like import, export etc.<p>
 * 
 */
public interface InterfaceReport {

    /** Indicates default formatting. */
    int FORMAT_DEFAULT = 0;

    /** Indicates error formatting. */
    int FORMAT_ERROR = 5;

    /** Indicates headline formatting. */
    int FORMAT_HEADLINE = 2;

    /** Indicates note formatting. */
    int FORMAT_NOTE = 3;

    /** Indicates OK formatting. */
    int FORMAT_OK = 4;

    /** Indicates warning formatting. */
    int FORMAT_WARNING = 1;

    /** Request parameter value that this report should create an "extended" output. */
    String REPORT_TYPE_EXTENDED = "extended";

    /** Request parameter value that this report should create a "simple" output. */
    String REPORT_TYPE_SIMPLE = "simple";

    /**
     * Adds an error object to the list of errors that occured during the report.<p>
     * 
     * @param obj the error object
     */
    void addError(Object obj);

    /**
     * Adds a warning object to the list of warnings that occured during the report.<p>
     * 
     * @param obj the error object
     */
    void addWarning(Object obj);

    /**
     * Formats the runtime formatted as "hh:mm:ss".<p>
     * 
     * @return the runtime formatted as "hh:mm:ss"
     */
    String formatRuntime();

    /**
     * Returns a list of all errors that occured during the report.<p>
     * 
     * @return an error list that occured during the report
     */
    List<Object> getErrors();

    /**
     * Returns the locale this report was initialized with.<p>
     * 
     * @return the locale this report was initialized with
     */
    Locale getLocale();

    /**
     * Updates this report, this processes all new output added since 
     * the last call to this method.<p>
     * 
     * This is only required in case the output is written to a HTML page,
     * if the shell output is used, this will just return an empty String.<p>
     * 
     * @return new elements that have been added to the report and not yet processed.
     */
    String getReportUpdate();

    /** 
     * Returns the time this report has been running.<p>
     * 
     * @return the time this report has been running
     */
    long getRuntime();

    /**
     * Returns a list of all warnings that occured during the report.<p>
     * 
     * @return a warning list that occured during the report
     */
    List<Object> getWarnings();

    /**
     * Returns if the report generated an error output.<p>
     * 
     * @return true if the report generated an error, otherwise false
     */
    boolean hasError();

    /**
     * Returns if the report generated a warning output.<p>
     * 
     * @return true if the report generated a warning, otherwise false
     */
    boolean hasWarning();

    /**
     * Prints a localized message to the report.<p>
     * 
     * @param uiLabel the String to add
     */
    void print(String uiLabel);

    /**
     * Prints a localized message to the report, using the indicated formatting.<p>
     * 
     * Use the contants starting with <code>FORMAT</code> from this interface
     * to indicate which formatting to use.<p>
     *
     * @param uiLabel the String to add
     * @param format the formatting to use for the output
     */
    void print(String uiLabel, int format);

    /**
     * Adds a line break to the report.<p>
     */
    void println();

    /**
     * Prints a localized message to the report.<p>
     * 
     * @param uiLabel the message to add
     */
    void println(String uiLabel);

    /**
     * Prints a localized message to the report, using the indicated formatting.<p>
     *
     * Use the contents starting with <code>FORMAT</code> from this interface
     * to indicate which formatting to use.
     *
     * @param uiLabel the message container to add
     * @param format the formatting to use for the output
     */
    void println(String uiLabel, int format);

    /**
     * Adds an Exception to the report, ensuring that the Exception content is
     * processed to generate a valid output esp. for HTML pages.<p>
     * 
     * The exception will be stored and the output will later be processed
     * in a special way.<p>    
     * 
     * @param t the exception to add
     * 
     */
    void println(Throwable t);

    /**
     * Prints a localized message followed by a parameter and dots to the report.<p>
     * 
     * @param uiLabel the Message to add
     * @param param the Parameter to add
     */
    void printMessageWithParam(String uiLabel, Object param);

    /**
     * Convenience method to print a localized message, followed by a parameter and dots to the report.<p>
     * 
     * The output follows the pattern: ( 3 / 8 ) Deleting filename.txt ...
     * 
     * @param m the number of the report output
     * @param n the total number of report outputs
     * @param uiLabel the Message to add
     * @param param the Parameter to add
     * 
     */
    void printMessageWithParam(int m, int n, String uiLabel, Object param);

    /**
     * Resets the runtime to 0 milliseconds.<p>
     */
    void resetRuntime();

    /**
     * Add a log file to the report.
     * 
     * @param logFileName
     */
    void addLogFile(String logFileName);

    /**
     * Close log file if necessary.
     * 
     */
    String closeLogFile();
    
    /**
     * Set log's sequence number.
     * @param sequenceNum
     */
    void setSequenceNum(long sequenceNum);
    
    long getSequenceNum();

}