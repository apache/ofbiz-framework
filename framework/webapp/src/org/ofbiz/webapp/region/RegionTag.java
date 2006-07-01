/*
 * $Id: RegionTag.java 5462 2005-08-05 18:35:48Z jonesde $
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

import java.net.URL;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * Base tag for other region tags, uses "/WEB-INF/regions.xml" file
 *
 * @author     David M. Geary in the book "Advanced Java Server Pages"
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class RegionTag extends TagSupport {
    protected Region regionObj = null;
    protected String template = null;
    private String region = null;

    public void setTemplate(String template) {
        this.template = template;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    protected boolean findRegionByKey() throws JspException {
        URL regionFile = null;

        try {
            regionFile = pageContext.getServletContext().getResource("/WEB-INF/regions.xml");
        } catch (java.net.MalformedURLException e) {
            throw new IllegalArgumentException("regions.xml file URL invalid: " + e.getMessage());
        }

        if (region != null) {
            regionObj = RegionManager.getRegion(regionFile, region);
            if (regionObj == null) {
                throw new JspException("can't find page definition attribute with this key: " + region);
            }
        }
        return regionObj != null;
    }

    protected void createRegionFromTemplate(String id) throws JspException {
        if (template == null)
            throw new JspException("can't find template");

        regionObj = new Region(id, template);
    }

    protected void createRegionFromRegion(String id) throws JspException {
        findRegionByKey();

        if (regionObj == null)
            return;

        // made from template and sections
        regionObj = new Region(id, regionObj.getContent(), regionObj.getSections());
    }

    public void put(Section section) {
        regionObj.put(section);
    }

    public void release() {
        super.release();
        regionObj = null;
        region = null;
        template = null;
    }
}
