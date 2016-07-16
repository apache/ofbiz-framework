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

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralRuntimeException;
import org.apache.ofbiz.base.util.ObjectType;
import org.apache.ofbiz.webapp.control.ConfigXMLReader;

/**
 * EventFactory - Event Handler Factory
 */
public class EventFactory {

    public static final String module = EventFactory.class.getName();

    private final Map<String, EventHandler> handlers = new HashMap<String, EventHandler>();

    public EventFactory(ServletContext context, URL controllerConfigURL) {
        // load all the event handlers
        try {
            Set<Map.Entry<String,String>> handlerEntries = ConfigXMLReader.getControllerConfig(controllerConfigURL).getEventHandlerMap().entrySet();
            if (handlerEntries != null) {
                for (Map.Entry<String,String> handlerEntry: handlerEntries) {
                    EventHandler handler = (EventHandler) ObjectType.getInstance(handlerEntry.getValue());
                    handler.init(context);
                    this.handlers.put(handlerEntry.getKey(), handler);
                }
            }
        } catch (Exception e) {
            Debug.logError(e, module);
            throw new GeneralRuntimeException(e);
        }
    }

    public EventHandler getEventHandler(String type) throws EventHandlerException {
        EventHandler handler = handlers.get(type);
        if (handler == null) {
            throw new EventHandlerException("No handler found for type: " + type);
        }
        return handler;
    }
}
