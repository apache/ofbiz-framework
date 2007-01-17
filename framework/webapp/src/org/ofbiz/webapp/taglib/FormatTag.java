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
package org.ofbiz.webapp.taglib;

import java.text.DateFormat;
import java.text.NumberFormat;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilJ2eeCompat;

/**
 * FormatTag - JSP Tag to format numbers and dates.
 */
public class FormatTag extends BodyTagSupport {
    
    public static final String module = FormatTag.class.getName();

    private String type = "N";
    private String defaultStr = "";

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public String getDefault() {
        return defaultStr;
    }

    public void setDefault(String defaultStr) {
        this.defaultStr = defaultStr;
    }

    public int doAfterBody() throws JspException {
        NumberFormat nf = null;
        DateFormat df = null;
        BodyContent body = getBodyContent();
        String value = body.getString();
        body.clearBody();

        if (value == null || value.length() == 0)
            return SKIP_BODY;

        if (type.charAt(0) == 'C' || type.charAt(0) == 'c')
            nf = NumberFormat.getCurrencyInstance();
        if (type.charAt(0) == 'N' || type.charAt(0) == 'n')
            nf = NumberFormat.getNumberInstance();
        if (type.charAt(0) == 'D' || type.charAt(0) == 'd')
            df = DateFormat.getDateInstance();

        try {
            if (nf != null) {
                // do the number formatting
                NumberFormat strFormat = NumberFormat.getInstance();

                getPreviousOut().print(nf.format(strFormat.parse(value.trim())));
            } else if (df != null) {
                // do the date formatting
                getPreviousOut().print(df.format(df.parse(value.trim())));
            } else {
                // just return the value
                getPreviousOut().print(value);
            }
        } catch (Exception e) {
            if (UtilJ2eeCompat.useNestedJspException(pageContext.getServletContext())) {
                throw new JspException(e.getMessage(), e);
            } else {
                Debug.logError(e, "Server does not support nested exceptions, here is the exception", module);
                throw new JspException(e.toString());
            }
        }

        return SKIP_BODY;
    }

}

