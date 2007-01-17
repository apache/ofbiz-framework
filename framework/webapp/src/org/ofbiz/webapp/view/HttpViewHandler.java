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

import java.io.IOException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.HttpClient;
import org.ofbiz.base.util.HttpClientException;

/**
 * ViewHandlerException - View Handler Exception
 */
public class HttpViewHandler implements ViewHandler {
    
    public static final String module = HttpViewHandler.class.getName();

    protected ServletContext context;

    public void init(ServletContext context) throws ViewHandlerException {
        this.context = context;
    }

    public void render(String name, String page, String info, String contentType, String encoding, HttpServletRequest request, HttpServletResponse response) throws ViewHandlerException {
        // some containers call filters on EVERY request, even forwarded ones,
        // so let it know that it came from the control servlet

        if (request == null)
            throw new ViewHandlerException("Null HttpServletRequest object");
        if (page == null || page.length() == 0)
            throw new ViewHandlerException("Null or empty source");

        if (Debug.infoOn()) Debug.logInfo("Retreiving HTTP resource at: " + page, module);
        try {
            HttpClient httpClient = new HttpClient(page);
            String pageText = httpClient.get();

            // TODO: parse page and remove harmful tags like <HTML>, <HEAD>, <BASE>, etc - look into the OpenSymphony piece for an example
            response.getWriter().print(pageText);
        } catch (IOException e) {
            throw new ViewHandlerException("IO Error in view", e);
        } catch (HttpClientException e) {
            throw new ViewHandlerException(e.getNonNestedMessage(), e.getNested());
        }
    }
}
