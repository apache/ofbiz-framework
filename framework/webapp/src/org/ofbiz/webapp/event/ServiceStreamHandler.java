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

import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.service.GenericDispatcher;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.base.util.Debug;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Map;

import javolution.util.FastMap;

/**
 * ServiceStreamHandler
 */
public class ServiceStreamHandler implements EventHandler {

    public static final String module = ServiceStreamHandler.class.getName();
    public static final String dispatcherName = "sstream-dispatcher";
    protected LocalDispatcher dispatcher;
    protected GenericDelegator delegator;

    public void init(ServletContext context) throws EventHandlerException {
        String delegatorName = context.getInitParameter("delegatorName");
        this.delegator = GenericDelegator.getGenericDelegator(delegatorName);
        this.dispatcher = GenericDispatcher.getLocalDispatcher(dispatcherName, delegator);
    }

    public String invoke(String eventPath, String eventMethod, HttpServletRequest request, HttpServletResponse response) throws EventHandlerException {        
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

        Map context = FastMap.newInstance();
        context.put("inputStream", in);
        context.put("outputStream", out);

        Debug.log("Running service with context: " + context, module);
        Map resp;
        try {
            resp = dispatcher.runSync(eventMethod, context);
        } catch (GenericServiceException e) {
            throw new EventHandlerException(e.getMessage(), e);
        }
        Debug.log("Received respone: " + resp, module);
        if (ServiceUtil.isError(resp)) {
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
}
