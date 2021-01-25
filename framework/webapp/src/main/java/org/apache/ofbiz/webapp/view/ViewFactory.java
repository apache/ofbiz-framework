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
package org.apache.ofbiz.webapp.view;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralRuntimeException;
import org.apache.ofbiz.base.util.ObjectType;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.webapp.control.ConfigXMLReader;

/**
 * ViewFactory - View Handler Factory
 */
public class ViewFactory {

    private static final String MODULE = ViewFactory.class.getName();

    private final Map<String, ViewHandler> handlers = new HashMap<>();

    public ViewFactory(ServletContext context, URL controllerConfigURL) {
        // load all the view handlers
        try {
            Set<Map.Entry<String, String>> handlerEntries = ConfigXMLReader.getControllerConfig(controllerConfigURL).getViewHandlerMap().entrySet();
            if (handlerEntries != null) {
                for (Map.Entry<String, String> handlerEntry: handlerEntries) {
                    ViewHandler handler = (ViewHandler) ObjectType.getInstance(handlerEntry.getValue());
                    handler.setName(handlerEntry.getKey());
                    handler.init(context);
                    this.handlers.put(handlerEntry.getKey(), handler);
                }
            }
            // load the "default" type
            if (!this.handlers.containsKey("default")) {
                ViewHandler defaultHandler = (ViewHandler) ObjectType.getInstance("org.apache.ofbiz.webapp.view.JspViewHandler");
                defaultHandler.init(context);
                this.handlers.put("default", defaultHandler);
            }
        } catch (Exception e) {
            Debug.logError(e, MODULE);
            throw new GeneralRuntimeException(e);
        }
    }

    /**
     * Gets view handler.
     * @param type the type
     * @return the view handler
     * @throws ViewHandlerException the view handler exception
     */
    public ViewHandler getViewHandler(String type) throws ViewHandlerException {
        if (UtilValidate.isEmpty(type)) {
            type = "default";
        }
        // get the view handler by type from the contextHandlers
        ViewHandler handler = handlers.get(type);
        if (handler == null) {
            throw new ViewHandlerException("No handler found for type: " + type);
        }
        return handler;
    }
}
