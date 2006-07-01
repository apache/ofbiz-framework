/*
 * $Id: InputValue.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 *  Copyright (c) 2001-2003 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.ofbiz.webapp.pseudotag;

import java.io.IOException;
import java.util.Map;
import javax.servlet.jsp.PageContext;

import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.entity.GenericValue;

/**
 * InputValue Pseudo-Tag
 * Outputs a string for an input box from either an entity field or
 *  a request parameter. Decides which to use by checking to see if the entityattr exist and
 *  using the specified field if it does. If the Boolean object referred to by the tryentityattr
 *  attribute is false, always tries to use the request parameter and ignores the entity field.
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class InputValue {

    PageContext pageContextInternal = null;

    public InputValue(PageContext pageContextInternal) {
        this.pageContextInternal = pageContextInternal;
    }

    public void run(String field, String entityAttr)
        throws IOException {
        run(field, null, entityAttr, null, null, null, pageContextInternal);
    }

    public void run(String field, String entityAttr, String tryEntityAttr)
        throws IOException {
        run(field, null, entityAttr, tryEntityAttr, null, null, pageContextInternal);
    }

    public void run(String field, String entityAttr, String tryEntityAttr,
        String fullattrsStr) throws IOException {
        run(field, null, entityAttr, tryEntityAttr, null, fullattrsStr, pageContextInternal);
    }

    /** Run the InputValue Pseudo-Tag, all fields except field, and entityAttr can be null */
    public void run(String field, String param, String entityAttr, String tryEntityAttr,
        String defaultStr, String fullattrsStr) throws IOException {
        run(field, param, entityAttr, tryEntityAttr, defaultStr, fullattrsStr, pageContextInternal);
    }

    /* --- STATIC METHODS --- */

    public static void run(String field, String entityAttr,
        PageContext pageContext) throws IOException {
        run(field, null, entityAttr, null, null, null, pageContext);
    }

    public static void run(String field, String entityAttr, String tryEntityAttr,
        PageContext pageContext) throws IOException {
        run(field, null, entityAttr, tryEntityAttr, null, null, pageContext);
    }

    public static void run(String field, String entityAttr, String tryEntityAttr,
        String fullattrsStr, PageContext pageContext) throws IOException {
        run(field, null, entityAttr, tryEntityAttr, null, fullattrsStr, pageContext);
    }

    /** Run the InputValue Pseudo-Tag, all fields except field, entityAttr, and pageContext can be null */
    public static void run(String field, String param, String entityAttr, String tryEntityAttr,
        String defaultStr, String fullattrsStr, PageContext pageContext) throws IOException {
        if (field == null || entityAttr == null || pageContext == null) {
            throw new RuntimeException("Required parameter (field or entityAttr or pageContext) missing");
        }

        if (defaultStr == null) defaultStr = "";
        String inputValue = null;
        boolean tryEntity = true;
        boolean fullattrs = false;

        String paramName = param;

        if (paramName == null || paramName.length() == 0)
            paramName = field;

        Boolean tempBool = null;

        if (tryEntityAttr != null)
            tempBool = (Boolean) pageContext.findAttribute(tryEntityAttr);
        if (tempBool != null)
            tryEntity = tempBool.booleanValue();

        // if anything but true, it will be false, ie default is false
        fullattrs = "true".equals(fullattrsStr);

        if (tryEntity) {
            Object entTemp = pageContext.findAttribute(entityAttr);

            if (entTemp != null) {
                if (entTemp instanceof GenericValue) {
                    GenericValue entity = (GenericValue) entTemp;
                    Object fieldVal = entity.get(field);

                    if (fieldVal != null)
                        inputValue = fieldVal.toString();
                } else if (entTemp instanceof Map) {
                    Map map = (Map) entTemp;
                    Object fieldVal = map.get(field);

                    if (fieldVal != null)
                        inputValue = fieldVal.toString();
                } // else do nothing
            }
        } else {
            // Current code will only get a parameter if we are not trying to get
            // fields from the entity/map
            // OLD WAY:
            // if nothing found in entity, or if not checked, try a parameter
            // if (inputValue == null) {
            inputValue = pageContext.getRequest().getParameter(paramName);
        }

        if (inputValue == null || inputValue.length() == 0)
            inputValue = defaultStr;

        if (fullattrs) {
            inputValue = UtilFormatOut.replaceString(inputValue, "\"", "&quot;");
            pageContext.getOut().print("name=\"" + paramName + "\" value=\"" +
                inputValue + "\"");
        } else {
            pageContext.getOut().print(inputValue);
        }
    }
}
