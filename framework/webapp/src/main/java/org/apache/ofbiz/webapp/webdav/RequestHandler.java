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

public interface RequestHandler {
    /**
     *  Called by the the WebDAV servlet to handle a WebDAV request.
     *  <table cellspacing="0" cellpadding="2" border="1">
     *    <caption>HTTPServletRequest attributes</caption>
     *    <tr><td>delegator</td><td>A <code>GenericDelgator</code> instance</td></tr>
     *    <tr><td>dispatcher</td><td>A <code>LocalDispatcher</code> instance</td></tr>
     *    <tr><td>security</td><td>A <code>Security</code> instance</td></tr>
     *  </table>
     */
    public void handleRequest(HttpServletRequest request, HttpServletResponse response, ServletContext context) throws ServletException, IOException;
}
