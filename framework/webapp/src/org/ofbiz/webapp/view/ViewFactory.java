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
package org.ofbiz.webapp.view;

import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralRuntimeException;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.webapp.control.RequestHandler;

/**
 * ViewFactory - View Handler Factory
 */
public class ViewFactory {

    public static final String module = ViewFactory.class.getName();

    protected RequestHandler requestHandler = null;
    protected ServletContext context = null;
    protected Map<String, ViewHandler> handlers = null;

    public ViewFactory(RequestHandler requestHandler) {
        this.handlers = FastMap.newInstance();
        this.requestHandler = requestHandler;
        this.context = requestHandler.getServletContext();

        // pre-load all the view handlers
        try {
            this.preLoadAll();
        } catch (ViewHandlerException e) {
            Debug.logError(e, module);
            throw new GeneralRuntimeException(e);
        }
    }

    private void preLoadAll() throws ViewHandlerException {
        Set<String> handlers = this.requestHandler.getControllerConfig().viewHandlerMap.keySet();
        if (handlers != null) {
            for (String type: handlers) {
                this.handlers.put(type, this.loadViewHandler(type));
            }
        }

        // load the "default" type
        if (!this.handlers.containsKey("default")) {
            try {
                ViewHandler h = (ViewHandler) ObjectType.getInstance("org.ofbiz.webapp.view.JspViewHandler");
                h.init(context);
                this. handlers.put("default", h);
            } catch (Exception e) {
                throw new ViewHandlerException(e);
            }
        }
    }

    public ViewHandler getViewHandler(String type) throws ViewHandlerException {
        if (UtilValidate.isEmpty(type)) {
            type = "default";
        }

        // check if we are new / empty and add the default handler in
        if (handlers.size() == 0) {
            this.preLoadAll();
        }

        // get the view handler by type from the contextHandlers
        ViewHandler handler = handlers.get(type);

        // if none found lets create it and add it in
        if (handler == null) {
            synchronized (ViewFactory.class) {
                handler = handlers.get(type);
                if (handler == null) {
                    handler = this.loadViewHandler(type);
                    handlers.put(type, handler);
                }
            }
            if (handler == null) {
                throw new ViewHandlerException("No handler found for type: " + type);
            }
        }
        return handler;
    }

    public void clear() {
        handlers.clear();
    }

    private ViewHandler loadViewHandler(String type) throws ViewHandlerException {
        ViewHandler handler = null;
        String handlerClass = this.requestHandler.getControllerConfig().viewHandlerMap.get(type);
        if (handlerClass == null) {
            throw new ViewHandlerException("Unknown handler type: " + type);
        }

        try {
            handler = (ViewHandler) ObjectType.getInstance(handlerClass);
            handler.setName(type);
            handler.init(context);
        } catch (ClassNotFoundException cnf) {
            //throw new ViewHandlerException("Cannot load handler class", cnf);
            Debug.logWarning("Warning: could not load view handler class because it was not found; note that some views may not work: " + cnf.toString(), module);
        } catch (InstantiationException ie) {
            throw new ViewHandlerException("Cannot get instance of the handler", ie);
        } catch (IllegalAccessException iae) {
            throw new ViewHandlerException(iae.getMessage(), iae);
        }

        return handler;
    }
}
