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
package org.apache.ofbiz.webapp.event;

import java.util.Locale;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.entity.transaction.GenericTransactionException;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.apache.ofbiz.minilang.MiniLangException;
import org.apache.ofbiz.minilang.SimpleMethod;
import org.apache.ofbiz.webapp.control.ConfigXMLReader.Event;
import org.apache.ofbiz.webapp.control.ConfigXMLReader.RequestMap;

/**
 * SimpleEventHandler - Simple Event Mini-Lang Handler
 */
public class SimpleEventHandler implements EventHandler {

    public static final String module = SimpleEventHandler.class.getName();
    /** Contains the property file name for translation of error messages. */
    public static final String err_resource = "WebappUiLabels";

    @Override
    public void init(ServletContext context) throws EventHandlerException {
    }

    @Override
    public String invoke(Event event, RequestMap requestMap, HttpServletRequest request, HttpServletResponse response) throws EventHandlerException {
        boolean beganTransaction = false;

        String xmlResource = event.path;
        String eventName = event.invoke;
        Locale locale = UtilHttp.getLocale(request);

        if (Debug.verboseOn()) Debug.logVerbose("[Set path/method]: " + xmlResource + " / " + eventName, module);

        if (xmlResource == null) {
            throw new EventHandlerException("XML Resource (eventPath) cannot be null");
        }
        if (eventName == null) {
            throw new EventHandlerException("Event Name (eventMethod) cannot be null");
        }

        if (Debug.verboseOn()) Debug.logVerbose("[Processing]: SIMPLE Event", module);
        try {
            beganTransaction = TransactionUtil.begin();
            String eventReturn = SimpleMethod.runSimpleEvent(xmlResource, eventName, request, response);
            if (Debug.verboseOn()) Debug.logVerbose("[Event Return]: " + eventReturn, module);
            return eventReturn;
        } catch (MiniLangException e) {
            Debug.logError(e, module);
            String errMsg = UtilProperties.getMessage(SimpleEventHandler.err_resource, "simpleEventHandler.event_not_completed", (locale != null ? locale : Locale.getDefault())) + ": ";
            request.setAttribute("_ERROR_MESSAGE_", errMsg + e.getMessage());
            return "error";
        } catch (GenericTransactionException e) {
            Debug.logError(e, module);
            return "error";
        } finally {
            try {
                TransactionUtil.commit(beganTransaction);
            } catch (GenericTransactionException e) {
                Debug.logError(e, module);
            }
        }
    }
}
