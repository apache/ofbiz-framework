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

package org.apache.ofbiz.workeffort.workeffort;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.webapp.webdav.RequestHandler;
import org.apache.ofbiz.webapp.webdav.RequestHandlerFactory;

/**
 * WebDAV request handler factory for iCalendar.
 *
 * This class is a simple connector between the WebDAV servlet and
 * the {@code ICalWorker} class.
 */
public class ICalHandlerFactory implements RequestHandlerFactory {

    public static final String module = ICalHandlerFactory.class.getName();
    protected final Map<String, RequestHandler> handlerMap;

    public ICalHandlerFactory() {
        handlerMap = new HashMap<>();
        handlerMap.put("COPY", ICalHandlerFactory::doNothing);
        handlerMap.put("DELETE", ICalHandlerFactory::doNothing);
        handlerMap.put("GET", ICalHandlerFactory::doGet);
        handlerMap.put("HEAD", ICalHandlerFactory::doNothing);
        handlerMap.put("LOCK", ICalHandlerFactory::doNothing);
        handlerMap.put("MKCOL", ICalHandlerFactory::doNothing);
        handlerMap.put("MOVE", ICalHandlerFactory::doNothing);
        handlerMap.put("POST", ICalHandlerFactory::doNothing);
        handlerMap.put("PROPFIND", ICalHandlerFactory::doPropFind);
        handlerMap.put("PROPPATCH", ICalHandlerFactory::doNothing);
        handlerMap.put("PUT", ICalHandlerFactory::doPut);
        handlerMap.put("UNLOCK", ICalHandlerFactory::doNothing);
    }

    @Override
    public RequestHandler getHandler(String method) {
        RequestHandler handler = handlerMap.get(method);
        if (handler == null) {
            return ICalHandlerFactory::handleInvalidMethod;
        }
        return handler;
    }

    protected static void handleInvalidMethod (HttpServletRequest req, HttpServletResponse resp, ServletContext ctx)
            throws ServletException, IOException {
        Debug.logInfo("[InvalidMethodHandler] method = " + req.getMethod(), module);
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    protected static void doNothing(HttpServletRequest req, HttpServletResponse resp, ServletContext ctx)
            throws ServletException, IOException {
        Debug.logInfo("[DoNothingHandler] method = " + req.getMethod(), module);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    protected static void doGet(HttpServletRequest req, HttpServletResponse resp, ServletContext ctx)
            throws ServletException, IOException {
        Debug.logInfo("[GetHandler] starting request", module);
        ICalWorker.handleGetRequest(req, resp, ctx);
        Debug.logInfo("[GetHandler] finished request", module);
    }

    protected static void doPut(HttpServletRequest req, HttpServletResponse resp, ServletContext ctx)
            throws ServletException, IOException {
        Debug.logInfo("[PutHandler] starting request", module);
        ICalWorker.handlePutRequest(req, resp, ctx);
        Debug.logInfo("[PutHandler] finished request", module);
    }

    protected static void doPropFind(HttpServletRequest req, HttpServletResponse resp, ServletContext ctx)
            throws ServletException, IOException {
        Debug.logInfo("[PropFindHandler] starting request", module);
        ICalWorker.handlePropFindRequest(req, resp, ctx);
        Debug.logInfo("[PropFindHandler] finished request", module);
    }
}
