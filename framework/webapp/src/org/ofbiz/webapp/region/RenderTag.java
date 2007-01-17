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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

import org.ofbiz.base.util.Debug;

/**
 * Tag to render a region
 */
public class RenderTag extends RegionTag {
    
    public static final String module = RenderTag.class.getName();
    
    private String sectionName = null;
    private String role = null;
    private String permission = null;
    private String action = null;

    public void setSection(String s) {
        this.sectionName = s;
    }

    public void setRole(String s) {
        this.role = s;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public void setAction(String action) {
        this.action = action;
    }

    protected boolean renderingRegion() {
        return sectionName == null;
    }

    protected boolean renderingSection() {
        return sectionName != null;
    }

    public int doStartTag() throws JspException {
        HttpServletRequest request = (HttpServletRequest)
            pageContext.getRequest();

        if (role != null && !request.isUserInRole(role))
            return SKIP_BODY;

        if (renderingRegion()) {
            if (!findRegionByKey()) {
                createRegionFromTemplate(null);
            }
            RegionStack.push(pageContext.getRequest(), regionObj);
        }
        return EVAL_BODY_INCLUDE;
    }

    public int doEndTag() throws JspException {
        Region regionEnd = null;

        try {
            regionEnd = RegionStack.peek(pageContext.getRequest());
        } catch (Exception e) {
            throw new JspException("Error finding region on stack: " + e.getMessage());
        }

        if (regionEnd == null)
            throw new JspException("Can't find region on stack");

        if (renderingSection()) {
            Section section = regionEnd.get(sectionName);

            if (section == null)
                return EVAL_PAGE; // ignore missing sections

            section.render(pageContext);
        } else if (renderingRegion()) {
            try {
                regionEnd.render(pageContext);
                RegionStack.pop(pageContext.getRequest());
            } catch (Exception ex) {
                Debug.logError(ex, "Error rendering region [" + regionEnd.getId() + "]: ", module);
                // IOException or ServletException
                throw new JspException("Error rendering region [" + regionEnd.getId() + "]: " + ex.getMessage());
            }
        }
        return EVAL_PAGE;
    }

    public void release() {
        super.release();
        sectionName = role = null;
    }
}
