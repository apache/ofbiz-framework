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
package org.ofbiz.base.util.collections;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpSession;

import org.ofbiz.base.util.string.FlexibleStringExpander;

/**
 * Used to flexibly access Map values, supporting the "." (dot) syntax for
 * accessing sub-map values and the "[]" (square bracket) syntax for accessing
 * list elements. See individual Map operations for more information.
 *
 */
public class FlexibleServletAccessor implements Serializable {

    protected String name;
    protected String attributeName;
    protected FlexibleMapAccessor fma;
    protected boolean needsExpand;
    protected boolean empty;

    public FlexibleServletAccessor(String name) {
        init(name);
    }
    
    public FlexibleServletAccessor(String name, String defaultName) {
        if (name == null || name.length() == 0) {
            init(defaultName);
        } else {
            init(name);
        }
    }
    
    protected void init(String name) {
        this.name = name;
        if (name == null || name.length() == 0) {
            empty = true;
            needsExpand = false;
            fma = new FlexibleMapAccessor(name);
            attributeName = name;
        } else {
            empty = false;
            int openPos = name.indexOf("${");
            if (openPos != -1 && name.indexOf("}", openPos) != -1) {
                fma = null;
                attributeName = null;
                needsExpand = true;
            } else {
                int dotIndex = name.indexOf('.');
                if (dotIndex != -1) {
                    attributeName = name.substring(0, dotIndex);
                    fma = new FlexibleMapAccessor(name.substring(dotIndex+1));
                } else {
                    attributeName = name;
                    fma = null;
                }
                
                needsExpand = false;
            }
        }
    }
    
    public boolean isEmpty() {
        return this.empty;
    }

    /** Based on name get from ServletRequest or from List in ServletRequest
     * @param request request to get the value from
     * @param expandContext the context to use for name expansion
     * @return the object corresponding to this getter class
     */
    public Object get(ServletRequest request, Map expandContext) {
        AttributeAccessor aa = new AttributeAccessor(name, expandContext, this.attributeName, this.fma, this.needsExpand);
        return aa.get(request);
    }

    /** Based on name get from HttpSession or from List in HttpSession
     * @param session
     * @param expandContext
     * @return
     */
    public Object get(HttpSession session, Map expandContext) {
        AttributeAccessor aa = new AttributeAccessor(name, expandContext, this.attributeName, this.fma, this.needsExpand);
        return aa.get(session);
    }

    /** Based on name put in ServletRequest or from List in ServletRequest;
     * If the brackets for a list are empty the value will be appended to the list,
     * otherwise the value will be set in the position of the number in the brackets.
     * If a "+" (plus sign) is included inside the square brackets before the index 
     * number the value will inserted/added at that point instead of set at the point.
     * @param request
     * @param value
     * @param expandContext
     */
    public void put(ServletRequest request, Object value, Map expandContext) {
        AttributeAccessor aa = new AttributeAccessor(name, expandContext, this.attributeName, this.fma, this.needsExpand);
        aa.put(request, value);
    }
    
    /** Based on name put in HttpSession or from List in HttpSession;
     * If the brackets for a list are empty the value will be appended to the list,
     * otherwise the value will be set in the position of the number in the brackets.
     * If a "+" (plus sign) is included inside the square brackets before the index 
     * number the value will inserted/added at that point instead of set at the point.
     * @param session
     * @param value
     * @param expandContext
     */
    public void put(HttpSession session, Object value, Map expandContext) {
        AttributeAccessor aa = new AttributeAccessor(name, expandContext, this.attributeName, this.fma, this.needsExpand);
        aa.put(session, value);
    }
    
    /** Based on name remove from ServletRequest or from List in ServletRequest
     * @param request
     * @param expandContext
     * @return
     */
    public Object remove(ServletRequest request, Map expandContext) {
        AttributeAccessor aa = new AttributeAccessor(name, expandContext, this.attributeName, this.fma, this.needsExpand);
        return aa.remove(request);
    }
    
    /** Based on name remove from HttpSession or from List in HttpSession
     * @param session
     * @param expandContext
     * @return
     */
    public Object remove(HttpSession session, Map expandContext) {
        AttributeAccessor aa = new AttributeAccessor(name, expandContext, this.attributeName, this.fma, this.needsExpand);
        return aa.remove(session);
    }
    
    /** The equals and hashCode methods are imnplemented just case this object is ever accidently used as a Map key * 
     * @return
     */    
    public int hashCode() {
        return this.name.hashCode();
    }

    /** The equals and hashCode methods are imnplemented just case this object is ever accidently used as a Map key 
     * @param obj
     * @return
     */    
    public boolean equals(Object obj) {
        if (obj instanceof FlexibleServletAccessor) {
            FlexibleServletAccessor flexibleServletAccessor = (FlexibleServletAccessor) obj;
            if (this.name == null) {
                return flexibleServletAccessor.name == null;
            }
            return this.name.equals(flexibleServletAccessor.name);
        } else {
            String str = (String) obj;
            if (this.name == null) {
                return str == null;
            }
            return this.name.equals(str);
        }
    }

    /** To be used for a string representation of the accessor, returns the original name. 
     * @return
     */    
    public String toString() {
        return this.name;
    }
    
    protected static class AttributeAccessor implements Serializable {
        protected Map expandContext;
        protected String attributeName;
        protected FlexibleMapAccessor fma;
        protected boolean isListReference;
        protected boolean isAddAtIndex;
        protected boolean isAddAtEnd;
        protected int listIndex;
        protected int openBrace;
        protected int closeBrace;
        
        public AttributeAccessor(String origName, Map expandContext, String defAttributeName, FlexibleMapAccessor defFma, boolean needsExpand) {
            attributeName = defAttributeName;
            fma = defFma;
            
            if (needsExpand) {
                String name = FlexibleStringExpander.expandString(origName, expandContext);
                int dotIndex = name.indexOf('.');
                if (dotIndex != -1) {
                    attributeName = name.substring(0, dotIndex);
                    fma = new FlexibleMapAccessor(name.substring(dotIndex+1));
                } else {
                    attributeName = name;
                    fma = null;
                }
            }

            isListReference = false;
            isAddAtIndex = false;
            isAddAtEnd = false;
            listIndex = -1;
            openBrace = attributeName.indexOf('[');
            closeBrace = (openBrace == -1 ? -1 : attributeName.indexOf(']', openBrace));
            if (openBrace != -1 && closeBrace != -1) {
                String liStr = attributeName.substring(openBrace+1, closeBrace);
                //if brackets are empty, append to list
                if (liStr.length() == 0) {
                    isAddAtEnd = true;
                } else {
                    if (liStr.charAt(0) == '+') {
                        liStr = liStr.substring(1);
                        listIndex = Integer.parseInt(liStr);
                        isAddAtIndex = true;
                    } else {
                        listIndex = Integer.parseInt(liStr);
                    }
                }
                attributeName = attributeName.substring(0, openBrace);
                isListReference = true;
            }
        
        }

        public Object get(ServletRequest request) {
            Object theValue = null;
            if (isListReference) {
                List lst = (List) request.getAttribute(attributeName);
                theValue = lst.get(listIndex);
            } else {
                theValue = request.getAttribute(attributeName);
            }

            if (fma != null) {
                return fma.get((Map) theValue);
            } else {
                return theValue;
            }
        }

        public Object get(HttpSession session) {
            Object theValue = null;
            if (isListReference) {
                List lst = (List) session.getAttribute(attributeName);
                theValue = lst.get(listIndex);
            } else {
                theValue = session.getAttribute(attributeName);
            }

            if (fma != null) {
                return fma.get((Map) theValue);
            } else {
                return theValue;
            }
        }

        protected void putInList(List lst, Object value) {
            //if brackets are empty, append to list
            if (isAddAtEnd) {
                lst.add(value);
            } else {
                if (isAddAtIndex) {
                    lst.add(listIndex, value);
                } else {
                    lst.set(listIndex, value);
                }
            }
        }
        
        public void put(ServletRequest request, Object value) {
            if (fma == null) {
                if (isListReference) {
                    List lst = (List) request.getAttribute(attributeName);
                    putInList(lst, value);
                } else {
                    request.setAttribute(attributeName, value);
                }
            } else {
                Object theObj = request.getAttribute(attributeName);
                if (isListReference) {
                    List lst = (List) theObj;
                    fma.put((Map) lst.get(listIndex), value);
                } else {
                    fma.put((Map) theObj, value);
                }
            }
        }
        
        public void put(HttpSession session, Object value) {
            if (fma == null) {
                if (isListReference) {
                    List lst = (List) session.getAttribute(attributeName);
                    putInList(lst, value);
                } else {
                    session.setAttribute(attributeName, value);
                }
            } else {
                Object theObj = session.getAttribute(attributeName);
                if (isListReference) {
                    List lst = (List) theObj;
                    fma.put((Map) lst.get(listIndex), value);
                } else {
                    fma.put((Map) theObj, value);
                }
            }
        }

        public Object remove(ServletRequest request) {
            if (fma != null) {
                Object theObj = request.getAttribute(attributeName);
                if (isListReference) {
                    List lst = (List) theObj;
                    return fma.remove((Map) lst.get(listIndex));
                } else {
                    return fma.remove((Map) theObj);
                }
            } else {
                if (isListReference) {
                    List lst = (List) request.getAttribute(attributeName);
                    return lst.remove(listIndex);
                } else {
                    Object theValue = request.getAttribute(attributeName);
                    request.removeAttribute(attributeName);
                    return theValue;
                }
            }
        }

        public Object remove(HttpSession session) {
            if (fma != null) {
                Object theObj = session.getAttribute(attributeName);
                if (isListReference) {
                    List lst = (List) theObj;
                    return fma.remove((Map) lst.get(listIndex));
                } else {
                    return fma.remove((Map) theObj);
                }
            } else {
                if (isListReference) {
                    List lst = (List) session.getAttribute(attributeName);
                    return lst.remove(listIndex);
                } else {
                    Object theValue = session.getAttribute(attributeName);
                    session.removeAttribute(attributeName);
                    return theValue;
                }
            }
        }
    }
}
