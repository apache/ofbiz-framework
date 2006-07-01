/*
 * $Id: SimpleEventHandler.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2001-2003 The Open For Business Project - www.ofbiz.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.ofbiz.webapp.event;

import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletContext;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.minilang.MiniLangException;
import org.ofbiz.minilang.SimpleMethod;

/**
 * SimpleEventHandler - Simple Event Mini-Lang Handler
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      2.0
 */
public class SimpleEventHandler implements EventHandler {

    public static final String module = SimpleEventHandler.class.getName();
    /** Contains the property file name for translation of error messages. */
    public static final String err_resource = "WebappUiLabels";

    /**
     * @see org.ofbiz.webapp.event.EventHandler#init(javax.servlet.ServletContext)
     */
    public void init(ServletContext context) throws EventHandlerException {
    }
    
    /**
     * Invoke the web event
     *@param eventPath The path or location of this event
     *@param eventMethod The method to invoke
     *@param request The servlet request object
     *@param response The servlet response object
     *@return String Result code
     *@throws EventHandlerException
     */
    public String invoke(String eventPath, String eventMethod, HttpServletRequest request, HttpServletResponse response) throws EventHandlerException {
        String xmlResource = eventPath;
        String eventName = eventMethod;
        Locale locale = UtilHttp.getLocale(request); 
        
        if (Debug.verboseOn()) Debug.logVerbose("[Set path/method]: " + xmlResource + " / " + eventName, module);

        if (xmlResource == null) {
            throw new EventHandlerException("XML Resource (eventPath) cannot be null");
        }
        if (eventName == null) {
            throw new EventHandlerException("Event Name (eventMethod) cannot be null");
        }

        Debug.logVerbose("[Processing]: SIMPLE Event", module);
        try {
            String eventReturn = SimpleMethod.runSimpleEvent(xmlResource, eventName, request, response);
            if (Debug.verboseOn()) Debug.logVerbose("[Event Return]: " + eventReturn, module);
            return eventReturn;
        } catch (MiniLangException e) {
            Debug.logError(e, module);
            String errMsg = UtilProperties.getMessage(SimpleEventHandler.err_resource, "simpleEventHandler.event_not_completed", (locale != null ? locale : Locale.getDefault())) + ": ";            
            request.setAttribute("_ERROR_MESSAGE_", errMsg + e.getMessage());
            return "error";
        }
    }
}
