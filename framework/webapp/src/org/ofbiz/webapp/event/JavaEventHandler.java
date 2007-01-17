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
package org.ofbiz.webapp.event;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletContext;

import org.ofbiz.base.util.Debug;

/**
 * JavaEventHandler - Static Method Java Event Handler
 */
public class JavaEventHandler implements EventHandler {

    public static final String module = JavaEventHandler.class.getName();

    private Map eventClassMap = new HashMap();

    /**
     * @see org.ofbiz.webapp.event.EventHandler#init(javax.servlet.ServletContext)
     */
    public void init(ServletContext context) throws EventHandlerException {
    }
    
    /**
     * @see org.ofbiz.webapp.event.EventHandler#invoke(java.lang.String, java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public String invoke(String eventPath, String eventMethod, HttpServletRequest request, HttpServletResponse response) throws EventHandlerException {
        Class eventClass = (Class) this.eventClassMap.get(eventPath);

        if (eventClass == null) {
            synchronized (this) {
                eventClass = (Class) this.eventClassMap.get(eventPath);
                if (eventClass == null) {
                    try {
                        ClassLoader loader = Thread.currentThread().getContextClassLoader();
                        eventClass = loader.loadClass(eventPath);
                    } catch (ClassNotFoundException e) {
                        Debug.logError(e, "Error loading class with name: " + eventPath + ", will not be able to run event...", module);
                    }
                    if (eventClass != null) {
                        eventClassMap.put(eventPath, eventClass);
                    }
                }
            }
        }
        if (Debug.verboseOn()) Debug.logVerbose("[Set path/method]: " + eventPath + " / " + eventMethod, module);

        Class[] paramTypes = new Class[] {HttpServletRequest.class, HttpServletResponse.class};

        Debug.logVerbose("*[[Event invocation]]*", module);
        Object[] params = new Object[] {request, response};

        return invoke(eventPath, eventMethod, eventClass, paramTypes, params);
    }

    private String invoke(String eventPath, String eventMethod, Class eventClass, Class[] paramTypes, Object[] params) throws EventHandlerException {
        if (eventClass == null) {
            throw new EventHandlerException("Error invoking event, the class " + eventPath + " was not found");
        }
        if (eventPath == null || eventMethod == null) {
            throw new EventHandlerException("Invalid event method or path; call initialize()");
        }

        Debug.logVerbose("[Processing]: JAVA Event", module);
        try {
            Method m = eventClass.getMethod(eventMethod, paramTypes);
            String eventReturn = (String) m.invoke(null, params);

            if (Debug.verboseOn()) Debug.logVerbose("[Event Return]: " + eventReturn, module);
            return eventReturn;
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable t = e.getTargetException();

            if (t != null) {
                Debug.logError(t, "Problems Processing Event", module);
                throw new EventHandlerException("Problems processing event: " + t.toString(), t);
            } else {
                Debug.logError(e, "Problems Processing Event", module);
                throw new EventHandlerException("Problems processing event: " + e.toString(), e);
            }
        } catch (Exception e) {
            Debug.logError(e, "Problems Processing Event", module);
            throw new EventHandlerException("Problems processing event: " + e.toString(), e);
        }
    }
}
