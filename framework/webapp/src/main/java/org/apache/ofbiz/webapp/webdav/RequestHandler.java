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
package org.apache.ofbiz.webapp.webdav;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@FunctionalInterface
public interface RequestHandler {
    /**
     * Method called by the the WebDAV servlet to handle a WebDAV request.
     * @param req
     *            the HTTP request to handle which contains the delegator, dispatcher}, Security attributes
     * @param resp
     *            the HTTP response to send
     * @param ctx
     *            the context of the current servlet
     * @throws ServletException
     *             if servlet execution failed
     * @throws IOException
     *             if communication with the HTTP request/response buffers failed
     */
    void handleRequest(HttpServletRequest req, HttpServletResponse resp, ServletContext ctx)
            throws ServletException, IOException;
}
