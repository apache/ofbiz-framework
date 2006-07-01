/*
 * $Id: EventFactory.java 6236 2005-12-02 10:48:53Z jonesde $
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralRuntimeException;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.webapp.control.RequestHandler;
import org.ofbiz.webapp.control.RequestManager;

/**
 * EventFactory - Event Handler Factory
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      2.0
 */
public class EventFactory {

    public static final String module = EventFactory.class.getName();

    protected RequestHandler requestHandler = null;
    protected RequestManager requestManager = null;
    protected ServletContext context = null;
    protected Map handlers = null;

    public EventFactory(RequestHandler requestHandler) {
        handlers = new HashMap();
        this.requestHandler = requestHandler;
        this.requestManager = requestHandler.getRequestManager();
        this.context = requestHandler.getServletContext();

        // pre-load all event handlers
        try {
            this.preLoadAll();
        } catch (EventHandlerException e) {
            Debug.logError(e, module);
            throw new GeneralRuntimeException(e);
        }
    }

    private void preLoadAll() throws EventHandlerException {
        List handlers = requestManager.getHandlerKeys(RequestManager.EVENT_HANDLER_KEY);
        if (handlers != null) {
            Iterator i = handlers.iterator();
            while (i.hasNext()) {
                String type = (String) i.next();
                this.handlers.put(type, this.loadEventHandler(type));
            }
        }
    }

    public EventHandler getEventHandler(String type) throws EventHandlerException {
        // check if we are new / empty and add the default handler in
        if (handlers.size() == 0) {
            this.preLoadAll();
        }

        // attempt to get a pre-loaded handler
        EventHandler handler = (EventHandler) handlers.get(type);

        if (handler == null) {
            synchronized (EventHandler.class) {
                handler = (EventHandler) handlers.get(type);
                if (handler == null) {
                    handler = this.loadEventHandler(type);
                    handlers.put(type, handler);
                }
            }
            if (handler == null)
                throw new EventHandlerException("No handler found for type: " + type);
        }
        return handler;
    }

    public void clear() {
        handlers.clear();
    }

    private EventHandler loadEventHandler(String type) throws EventHandlerException {
        EventHandler handler = null;
        String handlerClass = requestManager.getHandlerClass(type, RequestManager.EVENT_HANDLER_KEY);
        if (handlerClass == null) {
            throw new EventHandlerException("Unknown handler type: " + type);
        }

        try {
            handler = (EventHandler) ObjectType.getInstance(handlerClass);
            handler.init(context);
        } catch (NoClassDefFoundError e) {
            throw new EventHandlerException("No class def found for handler [" + handlerClass + "]", e);
        } catch (ClassNotFoundException cnf) {
            throw new EventHandlerException("Cannot load handler class [" + handlerClass + "]", cnf);
        } catch (InstantiationException ie) {
            throw new EventHandlerException("Cannot get instance of the handler [" + handlerClass + "]", ie);
        } catch (IllegalAccessException iae) {
            throw new EventHandlerException(iae.getMessage(), iae);
        }
        return handler;
    }

    public static String runRequestEvent(HttpServletRequest request, HttpServletResponse response, String requestUri)
            throws EventHandlerException {
        ServletContext application = ((ServletContext) request.getAttribute("servletContext"));
        RequestHandler handler = (RequestHandler) application.getAttribute("_REQUEST_HANDLER_");
        RequestManager rm = handler.getRequestManager();
        String eventType = rm.getEventType(requestUri);
        String eventPath = rm.getEventPath(requestUri);
        String eventMethod = rm.getEventMethod(requestUri);
        return handler.runEvent(request, response, eventType, eventPath, eventMethod);        
    }
}
