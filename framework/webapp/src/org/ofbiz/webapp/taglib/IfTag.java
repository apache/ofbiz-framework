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
import java.util.Collection;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.ofbiz.base.util.Debug;

/**
 * IfTag - Conditional Tag.
 */
public class IfTag extends BodyTagSupport {
    
    public static final String module = IfTag.class.getName();

    private String name = null;
    private String value = null;
    private String type = null;
    private Integer size = null;

    public void setName(String name) {
        this.name = name;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setSize(String size) throws NumberFormatException {
        this.size = Integer.valueOf(size);
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public String getType() {
        return type;
    }

    public String getSize() {
        if (size == null)
            return null;
        return size.toString();
    }

    public int doStartTag() throws JspTagException {
        Object object = null;

        try {
            object = pageContext.findAttribute(name);
            if (object == null) {
                object = pageContext.getRequest().getParameter(name);
            }
            if (object == null) {
                return SKIP_BODY;
            }
        } catch (RuntimeException e) {
            Debug.logError(e, module);
            return SKIP_BODY;
        }

        if (size != null) {
            int localSize = size.intValue();

            try {
                if (object instanceof Collection) {
                    // the object is a Collection so compare the size.
                    if (((Collection) object).size() > localSize)
                        return EVAL_BODY_AGAIN;
                } else if (object instanceof String) {
                    // the object is a Collection so compare the size.
                    if (((String) object).length() > localSize)
                        return EVAL_BODY_AGAIN;
                } else {
                    // use reflection to find a size() method
                    try {
                        Method sizeMethod = object.getClass().getMethod("size", (Class[]) null);
                        int objectSize = ((Integer) sizeMethod.invoke(object, (Object[]) null)).intValue();

                        if (objectSize > localSize)
                            return EVAL_BODY_AGAIN;
                    } catch (Exception e) {
                        Debug.logError(e, module);
                        return SKIP_BODY;
                    }
                }
            } catch (RuntimeException e) {
                Debug.logError(e, module);
                return SKIP_BODY;
            }
        } else if (object instanceof Boolean || "Boolean".equalsIgnoreCase(type)) {
            // Assume the object is a Boolean and compare to the Boolean value of value.
            try {
                Boolean b = (Boolean) object;

                if (value != null) {
                    Boolean v = new Boolean(value);

                    if (b.equals(v))
                        return EVAL_BODY_AGAIN;
                } else {
                    if (b.booleanValue())
                        return EVAL_BODY_AGAIN;
                }
            } catch (RuntimeException e) {
                Debug.logError(e, module);
                return SKIP_BODY;
            }
        } else if (value != null) {
            if (object instanceof String || "String".equalsIgnoreCase(type)) {
                // Assume the object is a string and compare to the String value of value.
                try {
                    String s = (String) object;

                    if (s.equals(value))
                        return EVAL_BODY_AGAIN;
                } catch (RuntimeException e) {
                    Debug.logError(e, module);
                    return SKIP_BODY;
                }
            } else if (object instanceof Integer || "Integer".equalsIgnoreCase(type)) {
                // Assume the object is a Integer and compare to the Integer value of value.
                try {
                    Integer i = (Integer) object;
                    Integer v = Integer.valueOf(value);

                    if (i.equals(v))
                        return EVAL_BODY_AGAIN;
                } catch (RuntimeException e) {
                    Debug.logError(e, module);
                    return SKIP_BODY;
                }
            } else if (object instanceof Long || "Long".equalsIgnoreCase(type)) {
                // Assume the object is a Integer and compare to the Integer value of value.
                try {
                    Long i = (Long) object;
                    Long v = Long.valueOf(value);

                    if (i.equals(v))
                        return EVAL_BODY_AGAIN;
                } catch (RuntimeException e) {
                    Debug.logError(e, module);
                    return SKIP_BODY;
                }
            } else if (object instanceof Float || "Float".equalsIgnoreCase(type)) {
                // Assume the object is a Double and compare to the Double value of value.
                try {
                    Float d = (Float) object;
                    Float v = Float.valueOf(value);

                    if (d.equals(v))
                        return EVAL_BODY_AGAIN;
                } catch (RuntimeException e) {
                    Debug.logError(e, module);
                    return SKIP_BODY;
                }
            } else if (object instanceof Double || "Double".equalsIgnoreCase(type)) {
                // Assume the object is a Double and compare to the Double value of value.
                try {
                    Double d = (Double) object;
                    Double v = Double.valueOf(value);

                    if (d.equals(v))
                        return EVAL_BODY_AGAIN;
                } catch (RuntimeException e) {
                    Debug.logError(e, module);
                    return SKIP_BODY;
                }
            } else {
                // Assume the object is an Object and compare to the Object named value.
                Object valueObject = null;

                try {
                    valueObject = pageContext.findAttribute(value);
                    if (valueObject != null && valueObject.equals(object))
                        return EVAL_BODY_AGAIN;
                } catch (RuntimeException e) {
                    Debug.logError(e, module);
                    return SKIP_BODY;
                }
            }
        } else {
            // basicly if no other comparisons available, just check to see if
            // the thing is null or not, and since we've already checked that,
            // treat as true here
            return EVAL_BODY_AGAIN;
        }

        return SKIP_BODY;
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
                //Debug.logInfo("printing string: " + bodyString, module);
                out.print(bodyString);
            }
        } catch (IOException e) {
            Debug.logError(e, "IfTag Error.", module);
        }
        return EVAL_PAGE;
    }
}

