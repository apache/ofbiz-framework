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

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.transaction.GenericTransactionException;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.apache.ofbiz.webapp.control.ConfigXMLReader.Event;
import org.apache.ofbiz.webapp.control.ConfigXMLReader.RequestMap;

/**
 * JavaEventHandler - Static Method Java Event Handler
 */
public class JavaEventHandler implements EventHandler {

    private static final String MODULE = JavaEventHandler.class.getName();

    /* Cache for event handler classes. */
    private ConcurrentHashMap<String, Class<?>> classes = new ConcurrentHashMap<>();

    /* Return class corresponding to path or null. */
    private static Class<?> loadClass(String path) {
        try {
            ClassLoader l = Thread.currentThread().getContextClassLoader();
            return l.loadClass(path);
        } catch (ClassNotFoundException e) {
            Debug.logError(e, "Error loading class with name: " + path
                    + ", will not be able to run event...", MODULE);
            return null;
        }
    }

    @Override
    public void init(ServletContext context) throws EventHandlerException {
    }

    @Override
    public String invoke(Event event, RequestMap requestMap,
            HttpServletRequest request, HttpServletResponse response)
                    throws EventHandlerException {
        Class<?> k = classes.computeIfAbsent(event.getPath(), JavaEventHandler::loadClass);
        if (Debug.verboseOn()) {
            Debug.logVerbose("*[[Event invocation]]*", MODULE);
        }
        if (k == null) {
            throw new EventHandlerException("Error invoking event, the class "
                                            + event.getPath() + " was not found");
        }
        if (event.getInvoke() == null || event.getInvoke() == null) {
            throw new EventHandlerException("Invalid event method or path; call initialize()");
        }

        if (Debug.verboseOn()) {
            Debug.logVerbose("[Processing]: Java Event", MODULE);
        }
        boolean began = false;
        try {
            int timeout = Integer.max(event.getTransactionTimeout(), 0);
            began = TransactionUtil.begin(timeout);
            Method m = k.getMethod(event.getInvoke(), HttpServletRequest.class,
                                   HttpServletResponse.class);
            String ret = (String) m.invoke(null, request, response);
            if (Debug.verboseOn()) {
                Debug.logVerbose("[Event Return]: " + ret, MODULE);
            }
            return ret;
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable t = e.getTargetException();

            if (t != null) {
                Debug.logError(t, "Problems Processing Event", MODULE);
                throw new EventHandlerException("Problems processing event: " + t.toString(), t);
            } else {
                Debug.logError(e, "Problems Processing Event", MODULE);
                throw new EventHandlerException("Problems processing event: " + e.toString(), e);
            }
        } catch (Exception e) {
            Debug.logError(e, "Problems Processing Event", MODULE);
            throw new EventHandlerException("Problems processing event: " + e.toString(), e);
        } finally {
            try {
                TransactionUtil.commit(began);
            } catch (GenericTransactionException e) {
                Debug.logError(e, MODULE);
            }
        }
    }
}
