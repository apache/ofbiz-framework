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

import java.net.URL;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * Base tag for other region tags, uses "/WEB-INF/regions.xml" file
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
