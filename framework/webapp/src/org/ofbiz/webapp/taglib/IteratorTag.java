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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.ObjectType;


/**
 * IterateTag - JSP Tag to iterate over a Collection or any object with an iterator() method.
 */
public class IteratorTag extends BodyTagSupport {

    public static final String module = IteratorTag.class.getName();

    protected Iterator iterator = null;
    protected String name = null;
    protected String property = null;
    protected Object element = null;
    protected Class type = null;
    protected int limit = 0;
    protected int offset = 0;
    protected boolean expandMap = false;

    public void setName(String name) {
        this.name = name;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public void setType(String type) throws ClassNotFoundException {
        this.type = ObjectType.loadClass(type);
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public void setExpandMap(String expMap) {
        // defaults to false, so if anything but true will be false:
        expandMap = "true".equals(expMap);
    }

    public void setIterator(Iterator iterator) {
        this.iterator = iterator;
    }

    public String getName() {
        return name;
    }

    public String getProperty() {
        return property;
    }

    public Object getElement() {
        return element;
    }

    public Iterator getIterator() {
        return this.iterator;
    }

    public String getType() {
        return type.getName();
    }

    public int getLimit() {
        return this.limit;
    }

    public int getOffset() {
        return this.offset;
    }

    public String getExpandMap() {
        return expandMap ? "true" : "false";
    }

    public int doStartTag() throws JspTagException {
        Debug.logVerbose("Starting Iterator Tag...", module);

        if (!defineIterator())
            return SKIP_BODY;

        Debug.logVerbose("We now have an iterator.", module);

        if (defineElement())
            return EVAL_BODY_AGAIN;
        else
            return SKIP_BODY;
    }

    public int doAfterBody() {
        if (defineElement()) {
            return EVAL_BODY_AGAIN;
        } else {
            return SKIP_BODY;
        }
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
            Debug.logInfo("IteratorTag IO Error", module);
            Debug.logInfo(e, module);
        }
        return EVAL_PAGE;
    }

    private boolean defineIterator() {
        // clear the iterator, after this it may be set directly
        Iterator newIterator = null;
        Collection thisCollection = null;

        if (property != null) {
            if (Debug.verboseOn()) Debug.logVerbose("Getting iterator from property: " + property, module);
            Object propertyObject = pageContext.findAttribute(property);

            if (propertyObject instanceof Iterator) {
                newIterator = (Iterator) propertyObject;
            } else {
                // if ClassCastException, it should indicate looking for a Collection
                thisCollection = (Collection) propertyObject;
            }
        } else {
            // Debug.logInfo("No property, check for Object Tag.", module);
            ObjectTag objectTag =
                (ObjectTag) findAncestorWithClass(this, ObjectTag.class);

            if (objectTag == null)
                return false;
            if (objectTag.getType().equals("java.util.Collection")) {
                thisCollection = (Collection) objectTag.getObject();
            } else {
                try {
                    ClassLoader loader = Thread.currentThread().getContextClassLoader();
                    Method[] m = loader.loadClass(objectTag.getType()).getDeclaredMethods();

                    for (int i = 0; i < m.length; i++) {
                        if (m[i].getName().equals("iterator")) {
                            Debug.logVerbose("Found iterator method. Using it.", module);
                            newIterator = (Iterator) m[i].invoke(
                                        objectTag.getObject(), (Object[]) null);
                            break;
                        }
                    }
                } catch (Exception e) {
                    return false;
                }
            }
        }

        if (newIterator == null) {
            if (thisCollection == null || thisCollection.size() < 1)
                return false;

            if (limit > 0 || offset > 0) {
                ArrayList colList = new ArrayList(thisCollection);
                int startIndex = offset > 0 ? offset : 0;
                int endIndex = limit > 0 ? offset + limit : colList.size();

                endIndex = endIndex > colList.size() ? colList.size() : endIndex;
                List subList = colList.subList(startIndex, endIndex);

                newIterator = subList.iterator();
            } else {
                newIterator = thisCollection.iterator();
            }

            Debug.logVerbose("Got iterator.", module);
        } else {// already set
            Debug.logVerbose("iterator already set.", module);
        }
        this.iterator = newIterator;
        return true;
    }

    private boolean defineElement() {
        element = null;
        pageContext.removeAttribute(name, PageContext.PAGE_SCOPE);
        boolean verboseOn = Debug.verboseOn();

        if (this.iterator.hasNext()) {
            element = this.iterator.next();
            if (verboseOn) Debug.logVerbose("iterator has another object: " + element, module);
        } else {
            if (verboseOn) Debug.logVerbose("iterator has no more objects", module);
        }
        if (element != null) {
            if (verboseOn) Debug.logVerbose("set attribute " + name + " to be " + element + " as next value from iterator", module);
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

            return true;
        }
        if (verboseOn) Debug.logVerbose("no more iterations; element = " + element, module);
        return false;
    }
}

