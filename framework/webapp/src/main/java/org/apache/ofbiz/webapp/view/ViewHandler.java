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
package org.apache.ofbiz.webapp.view;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * ViewHandler - View Handler Interface
 */
public interface ViewHandler {

    /**
     * Sets the name of the view handler as declared in the controller configuration file.
     * @param name String The name of the view handler as declared in the controller configuration file.
     */
    public void setName(String name);

    /**
     * Gets the name of the view handler as declared in the controller configuration file.
     * @return name String The name of the view handler as declared in the controller configuration file.
     */
    public String getName();

    /**
     * Initializes the handler. Since handlers use the singleton pattern this method should only be called
     * the first time the handler is used.
     *
     * @param context ServletContext This may be needed by the handler in order to lookup properties or XML
     * definition files for rendering pages or handler options.
     * @throws ViewHandlerException
     */
    public void init(ServletContext context) throws ViewHandlerException;

    /**
     * Render the page.
     *
     * @param name The name of the view.
     * @param page The source of the view; could be a page, url, etc depending on the type of handler.
     * @param info An info string attached to this view
     * @param request The HttpServletRequest object used when requesting this page.
     * @param response The HttpServletResponse object to be used to present the page.
     * @throws ViewHandlerException
     */
    public void render(String name, String page, String info, String contentType, String encoding, HttpServletRequest request, HttpServletResponse response) throws ViewHandlerException;
}
