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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.FileUtil;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.htmlreport.util.ReportEncoder;
import org.apache.ofbiz.htmlreport.util.ReportStringUtil;

/**
 * HTML report output to be used in report.ftl.<p>
 * 
 */
public class HtmlReport extends AbstractReport {

    public static final String module = HtmlReport.class.getName();

    /** The delimiter that is used in the resource list request parameter. */
    public static final String DELIMITER_RESOURCES = "|";

    /** Request parameter name for the resource list. */
    public static final String PARAM_RESOURCELIST = "resourcelist";

    /** Constant for a HTML linebreak with added "real" line break. */
    protected static final String LINEBREAK = "<br>";

    /** 
     * Constant for a HTML linebreak with added "real" line break- 
     * traditional style for report threads that still use XML templates for their output.
     */
    protected static final String LINEBREAK_TRADITIONAL = "<br>\n";

    /** The list of report objects e.g. String, Exception ... */
    protected List<Serializable> content;

    /** The list of report objects e.g. String, Exception ... */
    protected List<Serializable> logContent;

    /**
     * Counter to remember what is already shown,
     * indicates the next index of the content list that has to be reported.
     */
    protected int indexNext;

    /** Flag to indicate if an exception should be displayed long or short. */
    protected boolean showExceptionStackTrace;

    /** If set to <code>true</code> nothing is kept in memory. */
    protected boolean isTransient;

    /** Boolean flag indicating whether this report should generate HTML or JavaScript output. */
    protected boolean writeHtml;
    
    /** Helper variable to deliver the html end part. */
    public static final int HTML_END = 1;

    /** Helper variable to deliver the html start part. */
    public static final int HTML_START = 0;

    /** The thread to display in this report. */
    protected String paramThread;

    /** The next thread to display after this report. */
    protected String paramThreadHasNext;
    
    protected String paramAction;
    
    protected String paramTitle;
    
    protected String paramResource;

    /** Flag for refreching workplace .*/
    protected String paramRefreshWorkplace;

    /** Constant for the "OK" button in the build button methods. */
    public static final int BUTTON_OK = 0;

    /** Constant for the "Cancel" button in the build button methods. */
    public static final int BUTTON_CANCEL = 1;

    /** Constant for the "Close" button in the build button methods. */
    public static final int BUTTON_CLOSE = 2;

    /** Constant for the "Advanced" button in the build button methods. */
    public static final int BUTTON_ADVANCED = 3;

    /** Constant for the "Set" button in the build button methods. */
    public static final int BUTTON_SET = 4;

    /** Constant for the "Details" button in the build button methods. */
    public static final int BUTTON_DETAILS = 5;

    /** Constant for the "OK" button in the build button methods (without form submission). */
    public static final int BUTTON_OK_NO_SUBMIT = 6;

    /** Constant for the "Edit" button in the build button methods (same function as "Ok" button but different text on button. */
    public static final int BUTTON_EDIT = 7;

    /** Constant for the "Discard" button in the build button methods (same function as "Cancel" button but different text on button. */
    public static final int BUTTON_DISCARD = 8;

    /** Constant for the "Back" button in the build button methods. */
    public static final int BUTTON_BACK = 9;

    /** Constant for the "Continue" button in the build button methods. */
    public static final int BUTTON_CONTINUE = 10;

    /** Constant for the "Download" button in the build button methods. */
    public static final int BUTTON_DOWNLOAD = 11;

    /** Request parameter value for the action: back. */
    public static final String DIALOG_BACK = "back";

    /** Request parameter value for the action: cancel. */
    public static final String DIALOG_CANCEL = "cancel";

    /** Request parameter value for the action: continue. */
    public static final String DIALOG_CONTINUE = "continue";

    /** Request parameter value for the action: set. */
    public static final String DIALOG_SET = "set";

    /** The resource list parameter value. */
    protected String paramResourcelist;

    /** The list of resource names for the multi operation. */
    protected List<String> resourceList;

    /** The key name which contains the localized message for the continue checkbox. */
    protected String paramReportContinueKey;

    public static final String DIALOG_URI = "dialoguri";
    
    public static final String FORM_URI = "formuri";
    
    public static final String resource = "PricatUiLabels";
    
    /** Log file. */
    protected File logFile;
    
    /** Log file name. */
    protected String logFileName;
    
    /** Log file output stream. */
    protected FileOutputStream logFileOutputStream;
    
    protected long sequenceNum = -1;

    /**
     * Constructs a new report using the provided locale for the output language.<p>
     * 
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     */
    public HtmlReport(HttpServletRequest request, HttpServletResponse response) {

        this(request, response, false, false);
    }

    /**
     * Constructs a new report using the provided locale for the output language.<p>
     *  
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @param writeHtml if <code>true</code>, this report should generate HTML instead of JavaScript output
     * @param isTransient If set to <code>true</code> nothing is kept in memory
     */
    public HtmlReport(HttpServletRequest request, HttpServletResponse response, boolean writeHtml, boolean isTransient) {

        init(UtilHttp.getLocale(request));
        content = new ArrayList<Serializable>(256);
        logContent = new ArrayList<Serializable>(256);
        showExceptionStackTrace = true;
        this.writeHtml = writeHtml;
        this.isTransient = isTransient;
    }
    
    public static HtmlReport getInstance(HttpServletRequest request, HttpServletResponse response) {
        HtmlReport wp = (HtmlReport) request.getSession().getAttribute(SESSION_REPORT_CLASS);
        if (wp == null) {
            wp = new HtmlReport(request, response, true, true);
            request.getSession().setAttribute(SESSION_REPORT_CLASS, wp);
        }
        return wp;
    }
    
    public static HtmlReport getInstance(HttpServletRequest request, HttpServletResponse response, boolean writeHtml, boolean isTransient) {
        HtmlReport wp = (HtmlReport) request.getSession().getAttribute(SESSION_REPORT_CLASS);
        if (wp == null) {
            wp = new HtmlReport(request, response, writeHtml, isTransient);
            request.getSession().setAttribute(SESSION_REPORT_CLASS, wp);
        }
        return wp;
    }
    
    public static HtmlReport getInstance(HttpServletRequest request, HttpServletResponse response, boolean writeHtml, boolean isTransient, String logFileName) {
        HtmlReport wp = (HtmlReport) request.getSession().getAttribute(SESSION_REPORT_CLASS);
        if (wp == null || UtilValidate.isEmpty(wp.getLogFileName()) || !wp.getLogFileName().equals(logFileName)) {
            wp = new HtmlReport(request, response, writeHtml, isTransient);
            request.getSession().setAttribute(SESSION_REPORT_CLASS, wp);
        }
        return wp;
    }
    
    public String getParamAction(HttpServletRequest request) {
        paramAction = request.getParameter("action");
        return paramAction != null ? paramAction : "reportbegin";
    }
    
    public void setParamAction(String action) {
        paramAction = action;
    }

    public void setParamThread(String name) {
        paramThread = name;
    }

    public synchronized String getReportUpdate() {
        StringBuffer result = new StringBuffer();
        StringBuffer logResult = new StringBuffer();
        int indexEnd = content.size();
        for (int i = indexNext; i < indexEnd; i++) {
            int pos = isTransient ? 0 : i;
            Object obj = content.get(pos);
            if ((obj instanceof String) || (obj instanceof StringBuffer)) {
                result.append(obj);
            } else if (obj instanceof Throwable) {
                result.append(getExceptionElementJS((Throwable)obj));
            }
            if (isTransient) {
                content.remove(indexNext);
            }
            if (UtilValidate.isNotEmpty(logContent)) {
                Object logObj = logContent.get(pos);
                if ((logObj instanceof String) || (logObj instanceof StringBuffer)) {
                    logResult.append(logObj);
                } else if (logObj instanceof Throwable) {
                    result.append(getExceptionElementHtml((Throwable) logObj));
                }
                if (isTransient) {
                    logContent.remove(indexNext);
                }
            }
        }
        
        indexNext = isTransient ? 0 : indexEnd;
        
        if (isTransient && logFileOutputStream != null && logResult.toString().length() > 0) {
            try {
                logFileOutputStream.write((logResult.toString() + "\n").getBytes());
                logFileOutputStream.flush();
            } catch (IOException e) {
                Debug.logError(e.getMessage(), module);
            }
        }
        return result.toString();
    }

    /**
     * Returns if the report writes html or javascript code.<p> 
     * 
     * @return <code>true</code> if the report writes html, and <code>false</code> if the report writes javascript code
     */
    public boolean isWriteHtml() {
        return writeHtml;
    }

    public synchronized void print(String value, int format) {
        StringBuffer buf = null;
        value = ReportStringUtil.escapeJavaScript(value);
        switch (format) {
            case FORMAT_HEADLINE:
                buf = new StringBuffer();
                buf.append("aH('");
                buf.append(value);
                buf.append("'); ");
                break;
            case FORMAT_WARNING:
                buf = new StringBuffer();
                buf.append("aW('");
                buf.append(value);
                buf.append("'); ");
                addWarning(value);
                break;
            case FORMAT_ERROR:
                buf = new StringBuffer();
                buf.append("aE('");
                buf.append(value);
                buf.append("'); ");
                addError(value);
                break;
            case FORMAT_NOTE:
                buf = new StringBuffer();
                buf.append("aN('");
                buf.append(value);
                buf.append("'); ");
                break;
            case FORMAT_OK:
                buf = new StringBuffer();
                buf.append("aO('");
                buf.append(value);
                buf.append("'); ");
                break;
            case FORMAT_DEFAULT:
            default:
                buf = new StringBuffer();
                buf.append("a('");
                buf.append(value);
                buf.append("'); ");
        }
        if (value.trim().endsWith(getLineBreak())) {
            buf.append("aB(); ");
        }
        content.add(buf.toString());

        switch (format) {
            case FORMAT_HEADLINE:
                buf = new StringBuffer();
                buf.append("<span class='head'>");
                buf.append(value);
                buf.append("</span>");
                break;
            case FORMAT_WARNING:
                buf = new StringBuffer();
                buf.append("<span class='warn'>");
                buf.append(value);
                buf.append("</span>");
                addWarning(value);
                break;
            case FORMAT_ERROR:
                buf = new StringBuffer();
                buf.append("<span class='err'>");
                buf.append(value);
                buf.append("</span>");
                addError(value);
                break;
            case FORMAT_NOTE:
                buf = new StringBuffer();
                buf.append("<span class='note'>");
                buf.append(value);
                buf.append("</span>");
                break;
            case FORMAT_OK:
                buf = new StringBuffer();
                buf.append("<span class='ok'>");
                buf.append(value);
                buf.append("</span>");
                break;
            case FORMAT_DEFAULT:
            default:
                buf = new StringBuffer(value);
        }
        if (value.trim().endsWith(getLineBreak())) {
            buf.append("\n");
        }
        logContent.add(buf.toString());
    }

    public void println() {
        print(getLineBreak());
    }

    public synchronized void println(Throwable t) {
        addError(t.getMessage());
        content.add(getExceptionElementJS(t));
        logContent.add(getExceptionElementHtml(t));
    }
    
    /**
     * Returns the correct line break notation depending on the output style of this report.
     * 
     * @return the correct line break notation
     */
    protected String getLineBreak() {
        return writeHtml ? LINEBREAK_TRADITIONAL : LINEBREAK;
    }

    /**
     * Output helper method to format a reported <code>Throwable</code> element.<p>
     * 
     * This method ensures that exception stack traces are properly escaped
     * when they are added to the report.<p>
     * 
     * There is a member variable {@link #showExceptionStackTrace} in this
     * class that controls if the stack track is shown or not.
     * In a later version this might be configurable on a per-user basis.<p>
     *      
     * @param throwable the exception to format
     * @return the formatted StringBuffer
     */
    private StringBuffer getExceptionElementJS(Throwable throwable) {
        StringBuffer buf = new StringBuffer(256);
        if (showExceptionStackTrace) {
            buf.append("aT('");
            buf.append(UtilProperties.getMessage(resource, "REPORT_EXCEPTION", getLocale()));
            String exception = ReportEncoder.escapeXml(throwable.getLocalizedMessage());
            if (UtilValidate.isEmpty(exception)) {
                exception = ReportEncoder.escapeXml(throwable.getMessage());
            }
            if (UtilValidate.isNotEmpty(exception)) {
                exception = exception.replaceAll("[\r\n]+", LINEBREAK);
                buf.append(ReportStringUtil.escapeJavaScript(exception) + LINEBREAK);
            } else {
                buf.append(throwable.toString());
            }
            buf.append("'); ");
        } else {
            buf.append("aT('");
            buf.append(UtilProperties.getMessage(resource, "REPORT_EXCEPTION", getLocale()));
            buf.append(ReportStringUtil.escapeJavaScript(throwable.toString()));
            buf.append("'); ");
        }
        return buf;
    }

    private StringBuffer getExceptionElementHtml(Throwable throwable) {
        StringBuffer buf = new StringBuffer(256);
        if (showExceptionStackTrace) {
            buf.append("<span class='throw'>");
            buf.append(UtilProperties.getMessage(resource, "REPORT_EXCEPTION", getLocale()));
            String exception = ReportEncoder.escapeXml(throwable.getLocalizedMessage());
            if (UtilValidate.isEmpty(exception)) {
                exception = ReportEncoder.escapeXml(throwable.getMessage());
            }
            if (UtilValidate.isNotEmpty(exception)) {
                exception = exception.replaceAll("[\r\n]+", LINEBREAK);
                buf.append(exception);
            } else {
                buf.append(throwable.toString());
            }
            buf.append("</span>");
        } else {
            buf.append("<span class='throw'>");
            buf.append(UtilProperties.getMessage(resource, "REPORT_EXCEPTION", getLocale()));
            buf.append(throwable.toString());
            buf.append("</span>");
            buf.append(getLineBreak());
        }
        return buf;
    }

    public void printMessageWithParam(String uiLabel, Object param) {
        print(uiLabel, InterfaceReport.FORMAT_NOTE);
    }

    public void printMessageWithParam(int m, int n, String uiLabel, Object param) {
        print(uiLabel, InterfaceReport.FORMAT_NOTE);
    }

    /**
     * Builds the start html of the page, including setting of DOCTYPE and 
     * inserting a header with the content-type.<p>
     * 
     * This overloads the default method of the parent class.<p>
     * 
     * @return the start html of the page
     */
    public String htmlStart() {

        return pageHtml(HTML_START, true);
    }

    /**
     * Builds the start html of the page, including setting of DOCTYPE and 
     * inserting a header with the content-type.<p>
     * 
     * This overloads the default method of the parent class.<p>
     * 
     * @param loadStyles if true, the defaul style sheet will be loaded
     * @return the start html of the page
     */
    public String htmlStart(boolean loadStyles) {

        return pageHtml(HTML_START, loadStyles);
    }

    /**
     * Builds the start html of the page, including setting of DOCTYPE and 
     * inserting a header with the content-type.<p>
     * 
     * This overloads the default method of the parent class.<p>
     * 
     * @param segment the HTML segment (START / END)
     * @param loadStyles if true, the defaul style sheet will be loaded
     * @return the start html of the page
     */
    public String pageHtml(int segment, boolean loadStyles) {
        if (segment == HTML_START) {
            StringBuffer result = new StringBuffer(512);
            result.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\">\n");
            result.append("<html>\n<head>\n");
            result.append("<meta HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=UTF-8\">\n");
            if (loadStyles) {
                result.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"");
                result.append("/pricat/includes/pricat.css");
                result.append("\">\n");
                result.append("<script type=\"text/javascript\">\n");
                result.append(dialogScriptSubmit());
                result.append("</script>\n");
            }
            return result.toString();
        } else {
            return "</html>";
        }
    }

    /**
     * Builds the standard javascript for submitting the dialog.<p>
     * 
     * @return the standard javascript for submitting the dialog
     */
    public String dialogScriptSubmit() {
        StringBuffer result = new StringBuffer(512);
        result.append("function submitAction(actionValue, theForm, formName) {\n");
        result.append("\tif (theForm == null) {\n");
        result.append("\t\ttheForm = document.forms[formName];\n");
        result.append("\t}\n");
        result.append("\ttheForm.action.value = actionValue;\n");
        result.append("\ttheForm.submit();\n");
        result.append("\treturn false;\n");
        result.append("}\n");
        return result.toString();
    }

    /**
     * Returns true if the report Thread is still alive (i.e. running), false otherwise.<p>
     *  
     * @return true if the report Thread is still alive
     */
    public boolean isAlive(HttpServletRequest request) {
        ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
        int i = threadGroup.activeCount();
        Thread[] threads = new Thread[i];
        threadGroup.enumerate(threads, true);
        AbstractReportThread thread = null;
        for (int j=0; j<threads.length; j++) {
            Thread threadInstance = threads[j];
            if (threadInstance instanceof AbstractReportThread) {
                if(((AbstractReportThread)threadInstance).getUUID().toString().equals(getParamThread(request))) {
                    thread = (AbstractReportThread) threadInstance;
                    break;
                }
            }
        }
        if (thread != null) {
            return thread.isAlive();
        } else {
            return false;
        }
    }

    /**
     * Returns the thread parameter value.<p>
     *
     * @return the thread parameter value
     */
    public String getParamThread(HttpServletRequest request) {
        String thread = request.getParameter("thread");
        return ReportStringUtil.isNotEmptyOrWhitespaceOnly(thread) ? thread : (paramThread == null? "" : paramThread);
    }

    /**
     * Returns the threadhasnext parameter value.<p>
     *
     * @return the threadhasnext parameter value
     */
    public String getParamThreadHasNext(HttpServletRequest request) {
        String threadhasnext = request.getParameter("threadhasnext");
        return ReportStringUtil.isNotEmptyOrWhitespaceOnly(threadhasnext) ? threadhasnext : "false";
    }

    /**
     * Builds the start html of the body.<p>
     * 
     * @param className optional class attribute to add to the body tag
     * @param parameters optional parameters to add to the body tag
     * @return the start html of the body
     */
    public String bodyStart(String className, String parameters) {
        return pageBody(HTML_START, className, parameters);
    }

    /**
     * Builds the html of the body.<p>
     * 
     * @param segment the HTML segment (START / END)
     * @param className optional class attribute to add to the body tag
     * @param parameters optional parameters to add to the body tag
     * @return the html of the body
     */
    public String pageBody(int segment, String className, String parameters) {
        if (segment == HTML_START) {
            StringBuffer result = new StringBuffer(128);
            result.append("</head>\n<body unselectable=\"on\"");
            if (ReportStringUtil.isNotEmptyOrWhitespaceOnly(className)) {
                result.append(" class=\"");
                result.append(className);
                result.append("\"");
            }
            if (ReportStringUtil.isNotEmpty(parameters)) {
                result.append(" ");
                result.append(parameters);
            }
            result.append(">\n");
            return result.toString();
        } else {
            return "</body>";
        }
    }

    /**
     * Builds the end html of the body.<p>
     * 
     * @return the end html of the body
     */
    public String bodyEnd() {
        return pageBody(HTML_END, null, null);
    }

    /**
     * Builds the end html of the page.<p>
     * 
     * @return the end html of the page
     */
    public String htmlEnd() {
        return pageHtml(HTML_END, null);
    }

    /**
     * Returns the default html for a workplace page, including setting of DOCTYPE and 
     * inserting a header with the content-type.<p>
     * 
     * @param segment the HTML segment (START / END)
     * @param title the title of the page, if null no title tag is inserted
     * @return the default html for a workplace page
     */
    public String pageHtml(int segment, String title) {
        return pageHtmlStyle(segment, title, null);
    }

    /**
     * Returns the default html for a workplace page, including setting of DOCTYPE and 
     * inserting a header with the content-type, allowing the selection of an individual style sheet.<p>
     * 
     * @param segment the HTML segment (START / END)
     * @param title the title of the page, if null no title tag is inserted
     * @param stylesheet the used style sheet, if null the default stylesheet 'workplace.css' is inserted
     * @return the default html for a workplace page
     */
    public String pageHtmlStyle(int segment, String title, String stylesheet) {
        if (segment == HTML_START) {
            StringBuffer result = new StringBuffer(512);
//            result.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\">\n");
//            result.append("<html>\n<head>\n");
//            if (title != null) {
//                result.append("<title>");
//                result.append(title);
//                result.append("</title>\n");
//            }
//            result.append("<meta HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=UTF-8\">\n");
            result.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"");
            result.append("/pricat/includes/pricat.css");
            result.append("\">\n");
            return result.toString();
        } else {
            return "";
//            return "</html>";
        }
    }

    /**
     * Returns the start html for the outer dialog window border.
     * 
     * @return the start html for the outer dialog window border
     */
    public String dialogStart() {
        return dialog(HTML_START, null);
    }

    /**
     * Builds the outer dialog window border.
     * 
     * @param segment the HTML segment (START / END)
     * @param attributes optional additional attributes for the opening dialog table
     * 
     * @return a dialog window start / end segment
     */
    public String dialog(int segment, String attributes) {
        if (segment == HTML_START) {
            StringBuffer html = new StringBuffer(512);
            html.append("<table class=\"dialog\" cellpadding=\"0\" cellspacing=\"0\"");
            if (attributes != null) {
                html.append(" ");
                html.append(attributes);
            }
            html.append("><tr><td>\n<table class=\"dialogbox\" cellpadding=\"0\" cellspacing=\"0\">\n");
            html.append("<tr><td>\n");
            return html.toString();
        } else {
            return "</td></tr></table>\n</td></tr></table>\n<p>&nbsp;</p>\n";
        }
    }

    /**
     * Returns the start html for the content area of the dialog window.<p>
     * 
     * @param title the title for the dialog
     * 
     * @return the start html for the content area of the dialog window
     */
    public String dialogContentStart(String title) {
        return dialogContent(HTML_START, title);
    }

    /**
     * Builds the content area of the dialog window.<p>
     * 
     * @param segment the HTML segment (START / END)
     * @param title the title String for the dialog window
     * 
     * @return a content area start / end segment
     */
    public String dialogContent(int segment, String title) {
        if (segment == HTML_START) {
            StringBuffer result = new StringBuffer(512);
            // null title is ok, we always want the title headline
            result.append(dialogHead(title));
            result.append("<div class=\"dialogcontent\" unselectable=\"on\">\n");
            result.append("<!-- dialogcontent start -->\n");
            return result.toString();
        } else {
            return "<!-- dialogcontent end -->\n</div>\n";
        }
    }

    /**
     * Builds the title of the dialog window.<p>
     * 
     * @param title the title String for the dialog window
     * 
     * @return the HTML title String for the dialog window
     */
    public String dialogHead(String title) {
        return "<div class=\"dialoghead\" unselectable=\"on\">" + (title == null ? "" : title) + "</div>";
    }

    /**
     * Returns the value of the title parameter, 
     * or null if this parameter was not provided.<p>
     * 
     * This parameter is used to build the title 
     * of the dialog. It is a parameter so that the title 
     * can be passed to included elements.<p>
     * 
     * @return the value of the title parameter
     */
    public String getParamTitle(HttpServletRequest request) {
        if (paramTitle == null) {
            paramTitle = request.getParameter("title");
        }
        return paramTitle != null ? paramTitle : "";
    }

    /**
     * Returns all initialized parameters of the current workplace class 
     * as hidden field tags that can be inserted in a form.<p>
     * 
     * @return all initialized parameters of the current workplace class
     * as hidden field tags that can be inserted in a html form
     */
    public String paramsAsHidden(HttpServletRequest request) {
        return paramsAsHidden(request, null);
    }

    /**
     * Returns all initialized parameters of the current workplace class 
     * that are not in the given exclusion list as hidden field tags that can be inserted in a form.<p>
     * 
     * @param excludes the parameters to exclude 
     * 
     * @return all initialized parameters of the current workplace class
     * that are not in the given exclusion list as hidden field tags that can be inserted in a form
     */
    public String paramsAsHidden(HttpServletRequest request, Collection<?> excludes) {
        StringBuffer result = new StringBuffer(512);
        Map<String, Object> params = paramValues(request);
        Iterator<?> i = params.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry entry = (Map.Entry)i.next();
            String param = (String)entry.getKey();
            if ((excludes == null) || (!excludes.contains(param))) {
                result.append("<input type=\"hidden\" name=\"");
                result.append(param);
                result.append("\" value=\"");
                String encoded = ReportEncoder.encode(
                    entry.getValue().toString(),
                    "UTF-8");
                result.append(encoded);
                result.append("\">\n");
            }
        }
        
        return result.toString();
    }

    /**
     * Returns the values of all parameter methods of this workplace class instance.<p>
     * 
     * @return the values of all parameter methods of this workplace class instance
     */
    protected Map<String, Object> paramValues(HttpServletRequest request) {
        List<Method> methods = paramGetMethods();
        Map<String, Object> map = new HashMap<String, Object>(methods.size());
        Iterator<Method> i = methods.iterator();
        while (i.hasNext()) {
            Method m = (Method)i.next();
            Object o = null;
            try {
                o = m.invoke(this, new Object[0]);
            } catch (InvocationTargetException ite) {
                // can usually be ignored
            } catch (IllegalAccessException eae) {
                // can usually be ignored
            }
            if (o != null) {
                map.put(m.getName().substring(8).toLowerCase(), o);
            }
        }
        return map;
    }

    /**
     * Returns a list of all methods of the current class instance that 
     * start with "getParam" and have no parameters.<p> 
     * 
     * @return a list of all methods of the current class instance that 
     * start with "getParam" and have no parameters
     */
    private List<Method> paramGetMethods() {
        List<Method> list = new ArrayList<Method>();
        Method[] methods = this.getClass().getMethods();
        int length = methods.length;
        for (int i = 0; i < length; i++) {
            Method method = methods[i];
            if (method.getName().startsWith("getParam") && (method.getParameterTypes().length == 0)) {
                // Debug.logInfo("getMethod: " + method.getName(), module);
                list.add(method);
            }
        }
        return list;
    }

    /**
     * Returns an optional introduction text to be displayed above the report output.<p>
     * 
     * @return an optional introduction text
     */
    public String reportIntroductionText() {
        return "";
    }

    /**
     * Returns an optional conclusion text to be displayed below the report output.<p>
     * 
     * @return an optional conclusion text
     */
    public String reportConclusionText() {
        return "";
    }

    /**
     * Returns the end html for the content area of the dialog window.<p>
     * 
     * @return the end html for the content area of the dialog window
     */
    public String dialogContentEnd() {
        return dialogContent(HTML_END, null);
    }

    /**
     * Builds a button row with an "Ok" and a "Cancel" button.<p>
     * 
     * This row is displayed when the first report is running.<p>
     * 
     * @param okAttrs optional attributes for the ok button
     * @param cancelAttrs optional attributes for the cancel button
     * @return the button row
     */
    public String dialogButtonsContinue(String okAttrs, String cancelAttrs) {
        return dialogButtons(new int[] {BUTTON_OK, BUTTON_CANCEL}, new String[] {
            okAttrs,
            cancelAttrs});
    }

    /**
     * Builds a button row with an "OK" and a "Cancel" button.<p>
     * 
     * This row is used when a single report is running or after the first report has finished.<p>
     * 
     * @param okAttrs optional attributes for the ok button
     * @param cancelAttrs optional attributes for the cancel button
     * @return the button row
     */
    public String dialogButtonsOkCancel(HttpServletRequest request, String okAttrs, String cancelAttrs) {
        if (Boolean.valueOf(getParamThreadHasNext(request)).booleanValue()
            && ReportStringUtil.isNotEmpty(getParamReportContinueKey())) {
            return dialogButtons(new int[] {BUTTON_OK, BUTTON_CANCEL}, new String[] {
                okAttrs,
                cancelAttrs});
        }
        return dialogButtons(new int[] {BUTTON_OK}, new String[] {okAttrs});
    }

    /**
     * Builds a button row with an "OK", a "Cancel" and a "Download" button.<p>
     * 
     * This row is used when a single report is running or after the first report has finished.<p>
     * 
     * @param okAttrs optional attributes for the ok button
     * @param cancelAttrs optional attributes for the cancel button
     * @param downloadAttrs optional attributes for the download button
     * @return the button row
     */
    public String dialogButtonsOkCancelDownload(HttpServletRequest request, String okAttrs, String cancelAttrs, String downloadAttrs) {
        if (ReportStringUtil.isEmptyOrWhitespaceOnly(downloadAttrs)) {
            downloadAttrs = "";
        } else {
            downloadAttrs += " ";
        }
        if (Boolean.valueOf(getParamThreadHasNext(request)).booleanValue()
            && ReportStringUtil.isNotEmpty(getParamReportContinueKey())) {
            return dialogButtons(new int[] {BUTTON_OK, BUTTON_CANCEL, BUTTON_DOWNLOAD}, new String[] {
                okAttrs,
                cancelAttrs,
                downloadAttrs + "onclick=\"downloadPricat(" + (sequenceNum > 0 ? sequenceNum : "") + ");\""});
        }
        return dialogButtons(new int[] {BUTTON_OK, BUTTON_DOWNLOAD}, new String[] {
            okAttrs,
            downloadAttrs + "onclick=\"downloadPricat(" + (sequenceNum > 0 ? sequenceNum : "") + ");\""});
    }

    /**
     * Builds the html for the button row under the dialog content area, including buttons.<p>
     * 
     * @param buttons array of constants of which buttons to include in the row
     * @param attributes array of Strings for additional button attributes
     * 
     * @return the html for the button row under the dialog content area, including buttons
     */
    public String dialogButtons(int[] buttons, String[] attributes) {
        StringBuffer result = new StringBuffer(256);
        result.append(dialogButtonRow(HTML_START));
        for (int i = 0; i < buttons.length; i++) {
            dialogButtonsHtml(result, buttons[i], attributes[i]);
        }
        result.append(dialogButtonRow(HTML_END));
        return result.toString();
    }

    /**
     * Builds the button row under the dialog content area without the buttons.<p>
     * 
     * @param segment the HTML segment (START / END)
     * 
     * @return the button row start / end segment
     */
    public String dialogButtonRow(int segment) {
        if (segment == HTML_START) {
            return "<!-- button row start -->\n<div class=\"dialogbuttons\" unselectable=\"on\">\n";
        } else {
            return "</div>\n<!-- button row end -->\n";
        }
    }

    /**
     * Renders the HTML for a single input button of a specified type.<p>
     * 
     * @param result a string buffer where the rendered HTML gets appended to
     * @param button a integer key to identify the button
     * @param attribute an optional string with possible tag attributes, or null
     */
    protected void dialogButtonsHtml(StringBuffer result, int button, String attribute) {
        attribute = appendDelimiter(attribute);
        switch (button) {
            case BUTTON_OK:
                result.append("<input name=\"ok\" value=\"");
                result.append(UtilProperties.getMessage(resource, "DIALOG_BUTTON_OK", getLocale()) + "\"");
                if (attribute.toLowerCase().indexOf("onclick") == -1) {
                    result.append(" type=\"submit\"");
                } else {
                    result.append(" type=\"button\"");
                }
                result.append(" class=\"dialogbutton\"");
                result.append(attribute);
                result.append(">\n");
                break;
            case BUTTON_CANCEL:
                result.append("<input name=\"cancel\" type=\"button\" value=\"");
                result.append(UtilProperties.getMessage(resource, "DIALOG_BUTTON_CANCEL", getLocale()) + "\"");
                if (attribute.toLowerCase().indexOf("onclick") == -1) {
                    result.append(" onclick=\"submitAction('" + DIALOG_CANCEL + "', form);\"");
                }
                result.append(" class=\"dialogbutton\"");
                result.append(attribute);
                result.append(">\n");
                break;
            case BUTTON_EDIT:
                result.append("<input name=\"ok\" value=\"");
                result.append(UtilProperties.getMessage(resource, "DIALOG_BUTTON_EDIT", getLocale()) + "\"");
                if (attribute.toLowerCase().indexOf("onclick") == -1) {
                    result.append(" type=\"submit\"");
                } else {
                    result.append(" type=\"button\"");
                }
                result.append(" class=\"dialogbutton\"");
                result.append(attribute);
                result.append(">\n");
                break;
            case BUTTON_DISCARD:
                result.append("<input name=\"cancel\" type=\"button\" value=\"");
                result.append(UtilProperties.getMessage(resource, "DIALOG_BUTTON_DISCARD", getLocale()) + "\"");
                if (attribute.toLowerCase().indexOf("onclick") == -1) {
                    result.append(" onclick=\"submitAction('" + DIALOG_CANCEL + "', form);\"");
                }
                result.append(" class=\"dialogbutton\"");
                result.append(attribute);
                result.append(">\n");
                break;
            case BUTTON_CLOSE:
                result.append("<input name=\"close\" type=\"button\" value=\"");
                result.append(UtilProperties.getMessage(resource, "DIALOG_BUTTON_CLOSE", getLocale()) + "\"");
                if (attribute.toLowerCase().indexOf("onclick") == -1) {
                    result.append(" onclick=\"submitAction('" + DIALOG_CANCEL + "', form);\"");
                }
                result.append(" class=\"dialogbutton\"");
                result.append(attribute);
                result.append(">\n");
                break;
            case BUTTON_ADVANCED:
                result.append("<input name=\"advanced\" type=\"button\" value=\"");
                result.append(UtilProperties.getMessage(resource, "DIALOG_BUTTON_ADVANCE", getLocale()) + "\"");
                result.append(" class=\"dialogbutton\"");
                result.append(attribute);
                result.append(">\n");
                break;
            case BUTTON_SET:
                result.append("<input name=\"set\" type=\"button\" value=\"");
                result.append(UtilProperties.getMessage(resource, "DIALOG_BUTTON_SET", getLocale()) + "\"");
                if (attribute.toLowerCase().indexOf("onclick") == -1) {
                    result.append(" onclick=\"submitAction('" + DIALOG_SET + "', form);\"");
                }
                result.append(" class=\"dialogbutton\"");
                result.append(attribute);
                result.append(">\n");
                break;
            case BUTTON_BACK:
                result.append("<input name=\"set\" type=\"button\" value=\"");
                result.append(UtilProperties.getMessage(resource, "DIALOG_BUTTON_BACK", getLocale()) + "\"");
                if (attribute.toLowerCase().indexOf("onclick") == -1) {
                    result.append(" onclick=\"submitAction('" + DIALOG_BACK + "', form);\"");
                }
                result.append(" class=\"dialogbutton\"");
                result.append(attribute);
                result.append(">\n");
                break;
            case BUTTON_CONTINUE:
                result.append("<input name=\"set\" type=\"button\" value=\"");
                result.append(UtilProperties.getMessage(resource, "DIALOG_BUTTON_CONTINUE", getLocale()) + "\"");
                if (attribute.toLowerCase().indexOf("onclick") == -1) {
                    result.append(" onclick=\"submitAction('" + DIALOG_CONTINUE + "', form);\"");
                }
                result.append(" class=\"dialogbutton\"");
                result.append(attribute);
                result.append(">\n");
                break;
            case BUTTON_DETAILS:
                result.append("<input name=\"details\" type=\"button\" value=\"");
                result.append(UtilProperties.getMessage(resource, "DIALOG_BUTTON_DETAIL", getLocale()) + "\"");
                result.append(" class=\"dialogbutton\"");
                result.append(attribute);
                result.append(">\n");
                break;
            case BUTTON_DOWNLOAD:
                result.append("<input name=\"download\" type=\"button\" value=\"");
                result.append(UtilProperties.getMessage(resource, "DIALOG_BUTTON_DOWNLOAD", getLocale()) + "\"");
                result.append(" class=\"dialogbutton\"");
                result.append(attribute);
                result.append(">\n");
                break;
            default:
                // not a valid button code, just insert a warning in the HTML
                result.append("<!-- invalid button code: ");
                result.append(button);
                result.append(" -->\n");
        }
    }

    /**
     * Appends a space char. between tag attributes.<p>
     * 
     * @param attribute a tag attribute
     * 
     * @return the tag attribute with a leading space char
     */
    protected String appendDelimiter(String attribute) {
        if (ReportStringUtil.isNotEmpty(attribute)) {
            if (!attribute.startsWith(" ")) {
                // add a delimiter space between the beginning button HTML and the button tag attributes
                return " " + attribute;
            } else {
                return attribute;
            }
        }
        return "";
    }

    /**
     * Returns true if the dialog operation has to be performed on multiple resources.<p>
     * 
     * @return true if the dialog operation has to be performed on multiple resources, otherwise false
     */
    public boolean isMultiOperation(HttpServletRequest request) {
        return (getResourceList(request).size() > 1);
    }

    /**
     * Returns the resources that are defined for the dialog operation.
     * 
     * For single resource operations, the list contains one item: the resource name found 
     * in the request parameter value of the "resource" parameter.
     * 
     * @return the resources that are defined for the dialog operation
     */
    public List<String> getResourceList(HttpServletRequest request) {
        if (resourceList == null) {
            // use lazy initializing
            if (getParamResourcelist(request) != null) {
                // found the resourcelist parameter
                resourceList = StringUtil.split(getParamResourcelist(request), DELIMITER_RESOURCES);
                Collections.sort(resourceList);
            } else {
                // this is a single resource operation, create list containing the resource name
                resourceList = new ArrayList<String>(1);
                String resource = getParamResource(request);
                if (ReportStringUtil.isNotEmptyOrWhitespaceOnly(resource)) {
                    resourceList.add(resource);
                } else {
                    resourceList.add("");
                }
            }
        }
        return resourceList;
    }

    /**
     * Returns the value of the resource list parameter, or null if the parameter is not provided.<p>
     * 
     * This parameter selects the resources to perform operations on.<p>
     *  
     * @return the value of the resource list parameter or null, if the parameter is not provided
     */
    public String getParamResourcelist(HttpServletRequest request) {
        if (ReportStringUtil.isNotEmpty(paramResourcelist) && !"null".equals(paramResourcelist)) {
            return paramResourcelist;
        } else {
            return null;
        }
    }

    /**
     * Returns the value of the file parameter, 
     * or null if this parameter was not provided.<p>
     * 
     * The file parameter selects the file on which the dialog action
     * is to be performed.<p>
     * 
     * @return the value of the file parameter
     */
    public String getParamResource(HttpServletRequest request) {
        paramResource = request.getParameter("resource");
        if ((paramResource != null) && !"null".equals(paramResource)) {
            return paramResource;
        } else {
            return null;
        }
    }

    /**
     * Returns if the workplace must be refreshed.<p>
     * 
     * @return <code>"true"</code> if the workplace must be refreshed.
     */
    public String getParamRefreshWorkplace() {
        return paramRefreshWorkplace;
    }

    /**
     * Returns the key name which contains the localized message for the continue checkbox.<p>
     * 
     * @return the key name which contains the localized message for the continue checkbox
     */
    public String getParamReportContinueKey() {
        if (paramReportContinueKey == null) {
            paramReportContinueKey = "";
        }
        return paramReportContinueKey;
    }

    /**
     * Returns the value of the resourcelist parameter in form of a String separated 
     * with {@link #DELIMITER_RESOURCES}, or the value of the  resource parameter if the 
     * first parameter is not provided (no multiple choice has been done.<p>
     * 
     * This may be used for jsps as value for the parameter for resources {@link #PARAM_RESOURCELIST}.<p>
     *  
     * @return the value of the resourcelist parameter or null, if the parameter is not provided
     */
    public String getResourceListAsParam(HttpServletRequest request) {
        String result = getParamResourcelist(request);
        if (ReportStringUtil.isEmptyOrWhitespaceOnly(result)) {
            result = getParamResource(request);
        }
        return result;
    }

    /**
     * Returns the end html for the outer dialog window border.<p>
     * 
     * @return the end html for the outer dialog window border
     */
    public String dialogEnd() {
        return dialog(HTML_END, null);
    }
    
    /**
     * Returns the http URI of the current dialog, to be used
     * as value for the "action" attribute of a html form.<p>
     *
     * This URI is the real one.<p>
     *  
     * @return the http URI of the current dialog
     */
    public String getDialogRealUri(HttpServletRequest request) {
        return String.valueOf(request.getAttribute(DIALOG_URI));
    }

    /**
     * Set the report form uri.
     * 
     * @param request
     * @param formUri
     */
    public void setFormRealUri(HttpServletRequest request, String formUri) {
        request.setAttribute(FORM_URI, formUri);
    }

    /**
     * Get the report form uri.
     * 
     * @param request
     * @return
     */
    public String getFormRealUri(HttpServletRequest request) {
        return (String) request.getAttribute(FORM_URI);
    }

    public void addLogFile(String logFileName) {
        if (logFile == null || logFileOutputStream == null) {
            this.logFileName = logFileName;
            logFile = FileUtil.getFile(logFileName);
            try {
                logFileOutputStream = new FileOutputStream(logFile);
            } catch (FileNotFoundException e) {
                // do nothing
            }
        }
    }
    
    public String closeLogFile() {
        if (logFileOutputStream != null) {
            try {
                logFileOutputStream.flush();
            } catch (IOException e) {
                // do nothing
            } finally {
                if (logFileOutputStream != null) {
                    try {
                        logFileOutputStream.close();
                    } catch (IOException e) {
                        // do nothing
                        Debug.logError(e, HtmlReport.module);
                    }
                }
            }
        }
        return logFileName;
    }
    
    public String getLogFileName() {
        return logFileName;
    }
    
    public long getSequenceNum() {
        return sequenceNum;
    }

    public void setSequenceNum(long sequenceNum) {
        this.sequenceNum = sequenceNum;
    }
}
