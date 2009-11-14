/*
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */

package org.ofbiz.webapp.event;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.DelegatorFactory;
import org.ofbiz.service.GenericDispatcher;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.webapp.control.ConfigXMLReader.Event;
import org.ofbiz.webapp.control.ConfigXMLReader.RequestMap;

/**
 * ServiceStreamHandler
 */
public class ServiceStreamHandler implements EventHandler {

    public static final String module = ServiceStreamHandler.class.getName();
    public static final String dispatcherName = "sstream-dispatcher";
    protected LocalDispatcher dispatcher;
    protected Delegator delegator;

    public void init(ServletContext context) throws EventHandlerException {
        String delegatorName = context.getInitParameter("entityDelegatorName");
        this.delegator = DelegatorFactory.getDelegator(delegatorName);
        this.dispatcher = GenericDispatcher.getLocalDispatcher(dispatcherName, delegator);
    }

    public String invoke(Event event, RequestMap requestMap, HttpServletRequest request, HttpServletResponse response) throws EventHandlerException {
        InputStream in;
        try {
            in = request.getInputStream();
        } catch (IOException e) {
            throw new EventHandlerException(e.getMessage(), e);
        }
        OutputStream out;
        try {
            out = response.getOutputStream();
        } catch (IOException e) {
            throw new EventHandlerException(e.getMessage(), e);
        }

        Map<String, Object> context = FastMap.newInstance();
        context.put("inputStream", in);
        context.put("outputStream", out);

        if (Debug.infoOn()) Debug.logInfo("Running service with context: " + context, module);

        Map<String, Object> resp;
        try {
            resp = dispatcher.runSync(event.invoke, context);
        } catch (GenericServiceException e) {
            outputError(out, e, "Exception thrown in runSync()");
            throw new EventHandlerException(e.getMessage(), e);
        }
        Debug.log("Received respone: " + resp, module);
        if (ServiceUtil.isError(resp)) {
            outputError(out, null, ServiceUtil.getErrorMessage(resp));
            throw new EventHandlerException(ServiceUtil.getErrorMessage(resp));
        }
        String contentType = (String) resp.get("contentType");
        if (contentType != null) {
            response.setContentType(contentType);
        }

        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                throw new EventHandlerException(ServiceUtil.getErrorMessage(resp));
            }
        }

        return null;
    }

    private void outputError(OutputStream stream, Exception error, String message) {
        PrintStream out = new PrintStream(stream);
        if (message != null)
            out.println("Error message: " + message);
        if (error != null)
            out.println("Exception occured: " + error.toString());
        out.flush();
        out.close();
        try {
            stream.close();
        } catch (IOException e) {
            Debug.logError(e, module);
        }
    }
}
