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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.net.URL;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.bsf.BSFException;
import org.apache.bsf.BSFManager;
import org.apache.bsf.util.IOUtils;
import org.ofbiz.base.location.FlexibleLocation;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.cache.UtilCache;
import org.ofbiz.webapp.control.ConfigXMLReader;

/**
 * BsfEventHandler - BSF Event Handler
 */
public class BsfEventHandler implements EventHandler {

    public static final String module = BsfEventHandler.class.getName();
    private static final UtilCache<String, String> eventCache = UtilCache.createUtilCache("webapp.BsfEvents");

    /**
     * @see org.ofbiz.webapp.event.EventHandler#init(javax.servlet.ServletContext)
     */
    public void init(ServletContext context) throws EventHandlerException {
    }

    /**
     * @see org.ofbiz.webapp.event.EventHandler#invoke(ConfigXMLReader.Event, ConfigXMLReader.RequestMap, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public String invoke(ConfigXMLReader.Event event, ConfigXMLReader.RequestMap requestMap, HttpServletRequest request, HttpServletResponse response) throws EventHandlerException {
        ServletContext context = (ServletContext) request.getAttribute("servletContext");
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null)
            cl = this.getClass().getClassLoader();

        if (context == null) {
            throw new EventHandlerException("Problem getting ServletContext");
        }

        try {
            // create the BSF manager
            BSFManager bsfManager = new BSFManager();
            bsfManager.setClassLoader(cl);

            // expose the event objects to the script
            bsfManager.declareBean("request", request, HttpServletRequest.class);
            bsfManager.declareBean("response", response, HttpServletResponse.class);

            // get the script type
            String scriptType = BSFManager.getLangFromFilename(event.invoke);

            // load the script
            InputStream scriptStream = null;
            String scriptString = null;
            String cacheName = null;
            if (UtilValidate.isEmpty(event.path)) {
                // we are a resource to be loaded off the classpath
                cacheName = event.invoke;
                scriptString = eventCache.get(cacheName);
                if (scriptString == null) {
                    if (Debug.verboseOn()) {
                        Debug.logVerbose("Loading BSF Script at location: " + cacheName, module);
                    }
                    URL scriptUrl = FlexibleLocation.resolveLocation(cacheName);
                    if (scriptUrl == null) {
                        throw new EventHandlerException("BSF script not found at location [" + cacheName + "]");
                    }
                    scriptStream = scriptUrl.openStream();
                    scriptString = IOUtils.getStringFromReader(new InputStreamReader(scriptStream));
                    scriptStream.close();
                    scriptString = eventCache.putIfAbsentAndGet(cacheName, scriptString);
                }
            } else {
                // we are a script in the webapp - load by resource
                cacheName = context.getServletContextName() + ":" + event.path + event.invoke;
                scriptString = eventCache.get(cacheName);
                if (scriptString == null) {
                    scriptStream = context.getResourceAsStream(event.path + event.invoke);
                    if (scriptStream == null) {
                        throw new EventHandlerException("Could not find BSF script file in webapp context: " + event.path + event.invoke);
                    }
                    scriptString = IOUtils.getStringFromReader(new InputStreamReader(scriptStream));
                    scriptStream.close();
                    scriptString = eventCache.putIfAbsentAndGet(cacheName, scriptString);
                }
            }

            // execute the script
            Object result = bsfManager.eval(scriptType, cacheName, 0, 0, scriptString);

            // check the result
            if (result != null && !(result instanceof String)) {
                throw new EventHandlerException("Event did not return a String result, it returned a " + result.getClass().getName());
            }

            return (String) result;
        } catch (BSFException e) {
            throw new EventHandlerException("BSF Error", e);
        } catch (IOException e) {
            throw new EventHandlerException("Problems reading script", e);
        }
    }
}
