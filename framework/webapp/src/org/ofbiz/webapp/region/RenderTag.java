/*
 * $Id: RenderTag.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2001-2003 Sun Microsystems Inc., published in "Advanced Java Server Pages" by Prentice Hall PTR
 * Copyright (c) 2001-2002 The Open For Business Project - www.ofbiz.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.ofbiz.webapp.region;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

import org.ofbiz.base.util.Debug;

/**
 * Tag to render a region
 *
 * @author     David M. Geary in the book "Advanced Java Server Pages"
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
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
