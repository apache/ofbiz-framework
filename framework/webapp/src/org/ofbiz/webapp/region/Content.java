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
package org.ofbiz.webapp.region;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

/**
 * Abstract base class for Section and Region
 * <br/>Subclasses of Content must implement render(PageContext)
 */
public abstract class Content implements java.io.Serializable {
    protected final String content;

    /** type can be:
     * <br/>- direct (for direct inline content)
     * <br/>- region (for a nested region)
     * <br/>- default (for region if matches region name OR JSP/Servlet resource otherwise)
     * <br/>- resource (for JSP/Servlet resource)
     * <br/>- or any ViewHandler defined in the corresponding controller.xml file 
     */
    protected final String type;

    // Render this content in a JSP page
    abstract void render(PageContext pc) throws JspException;

    abstract void render(HttpServletRequest request, HttpServletResponse response) throws java.io.IOException, ServletException;

    public Content(String content, String type) {
        this.content = content;
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public String getType() {
        return type;
    }

    public String toString() {
        return "Content: " + content + ", type: " + type;
    }
}
