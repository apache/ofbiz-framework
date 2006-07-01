/*
 * $Id: IteratorTag.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2002-2003 The Open For Business Project - www.ofbiz.org
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
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    1.0
 * @created    August 4, 2001
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

