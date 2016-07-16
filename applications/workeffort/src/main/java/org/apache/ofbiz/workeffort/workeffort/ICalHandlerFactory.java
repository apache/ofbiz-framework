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

/** WebDAV request handler factory for iCalendar. This class is a simple connector
 * between the WebDAV servlet and the <code>ICalWorker</code> class.
 */
public class ICalHandlerFactory implements RequestHandlerFactory {

    public static final String module = ICalHandlerFactory.class.getName();

    protected final Map<String, RequestHandler> handlerMap;
    protected final RequestHandler invalidMethodHandler = new InvalidMethodHandler();
    protected final RequestHandler doNothingHandler = new DoNothingHandler();

    public ICalHandlerFactory() {
        this.handlerMap = new HashMap<String, RequestHandler>();
        this.handlerMap.put("COPY", doNothingHandler);
        this.handlerMap.put("DELETE", doNothingHandler);
        this.handlerMap.put("GET", new GetHandler());
        this.handlerMap.put("HEAD", doNothingHandler);
        this.handlerMap.put("LOCK", doNothingHandler);
        this.handlerMap.put("MKCOL", doNothingHandler);
        this.handlerMap.put("MOVE", doNothingHandler);
        this.handlerMap.put("POST", doNothingHandler);
        this.handlerMap.put("PROPFIND", new PropFindHandler());
        this.handlerMap.put("PROPPATCH", doNothingHandler);
        this.handlerMap.put("PUT", new PutHandler());
        this.handlerMap.put("UNLOCK", doNothingHandler);
    }

    public RequestHandler getHandler(String method) {
        RequestHandler handler = this.handlerMap.get(method);
        if (handler == null) {
            return invalidMethodHandler;
        }
        return handler;
    }

    protected static class InvalidMethodHandler implements RequestHandler {
        public void handleRequest(HttpServletRequest request, HttpServletResponse response, ServletContext context) throws ServletException, IOException {
            Debug.logInfo("[InvalidMethodHandler] method = " + request.getMethod(), module);
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }
    }

    protected static class DoNothingHandler implements RequestHandler {
        public void handleRequest(HttpServletRequest request, HttpServletResponse response, ServletContext context) throws ServletException, IOException {
            Debug.logInfo("[DoNothingHandler] method = " + request.getMethod(), module);
            response.setStatus(HttpServletResponse.SC_OK);
        }
    }

    protected static class GetHandler implements RequestHandler {
        public void handleRequest(HttpServletRequest request, HttpServletResponse response, ServletContext context) throws ServletException, IOException {
            Debug.logInfo("[GetHandler] starting request", module);
            ICalWorker.handleGetRequest(request, response, context);
            Debug.logInfo("[GetHandler] finished request", module);
        }
    }

    protected static class PutHandler implements RequestHandler {
        public void handleRequest(HttpServletRequest request, HttpServletResponse response, ServletContext context) throws ServletException, IOException {
            Debug.logInfo("[PutHandler] starting request", module);
            ICalWorker.handlePutRequest(request, response, context);
            Debug.logInfo("[PutHandler] finished request", module);
        }
    }

    protected static class PropFindHandler implements RequestHandler {
        public void handleRequest(HttpServletRequest request, HttpServletResponse response, ServletContext context) throws ServletException, IOException {
            Debug.logInfo("[PropFindHandler] starting request", module);
            ICalWorker.handlePropFindRequest(request, response, context);
            Debug.logInfo("[PropFindHandler] finished request", module);
        }
    }

}
