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

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTagSupport;

/**
 * IterateNextTag - JSP Tag to get the next element of the IteratorTag.
 */
public class IterateNextTag extends BodyTagSupport {

    protected String name = null;
    protected Class type = null;
    protected Object element = null;
    protected boolean expandMap = false;

    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) throws ClassNotFoundException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        this.type = loader.loadClass(type);
    }

    public void setExpandMap(String expMap) {
        // defaults to false, so if anything but true will be false:
        expandMap = "true".equals(expMap);
    }

    public String getName() {
        return name;
    }

    public String getExpandMap() {
        return expandMap ? "true" : "false";
    }

    public Object getElement() {
        return element;
    }

    public int doStartTag() throws JspTagException {
        IteratorTag iteratorTag =
            (IteratorTag) findAncestorWithClass(this, IteratorTag.class);

        if (iteratorTag == null)
            throw new JspTagException("IterateNextTag not inside IteratorTag.");

        Iterator iterator = iteratorTag.getIterator();

        if (iterator == null || !iterator.hasNext())
            return SKIP_BODY;

        if (name == null)
            name = "next";

        // get the next element from the iterator
        Object element = iterator.next();

        pageContext.setAttribute(name, element);

        // expand a map element here if requested
        if (expandMap) {
            Map tempMap = (Map) element;
            Iterator mapEntries = tempMap.entrySet().iterator();

            while (mapEntries.hasNext()) {
                Map.Entry entry = (Map.Entry) mapEntries.next();
                Object value = entry.getValue();

                if (value == null) value = new String();
                pageContext.setAttribute((String) entry.getKey(), value);
            }
        }

        // give the updated iterator back.
        iteratorTag.setIterator(iterator);

        return EVAL_BODY_AGAIN;
    }

    public int doAfterBody() {
        return SKIP_BODY;
    }

    public int doEndTag() {
        try {
            BodyContent body = getBodyContent();

            if (body != null) {
                JspWriter out = body.getEnclosingWriter();
                String bodyString = body.getString();
                body.clearBody();
                out.print(bodyString);
            }
        } catch (IOException e) {
            System.out.println("IterateNext Tag error: " + e);
        }
        return EVAL_PAGE;
    }
}

