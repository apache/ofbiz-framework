/*
 * $Id: ModelKeyMap.java 6327 2005-12-14 18:56:54Z jaz $
 *
 *  Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
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
package org.ofbiz.entity.model;

import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilXml;


/**
 * Generic Entity - KeyMap model class
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class ModelKeyMap implements java.io.Serializable {

    /** name of the field in this entity */
    protected String fieldName = "";

    /** name of the field in related entity */
    protected String relFieldName = "";

    /** Default Constructor */
    public ModelKeyMap() {}

    /** Data Constructor, if relFieldName is null defaults to fieldName */
    public ModelKeyMap(String fieldName, String relFieldName) {
        this.fieldName = fieldName;
        this.relFieldName = UtilXml.checkEmpty(relFieldName, this.fieldName);
    }

    /** XML Constructor */
    public ModelKeyMap(Element keyMapElement) {
        this.fieldName = UtilXml.checkEmpty(keyMapElement.getAttribute("field-name"));
        // if no relFieldName is specified, use the fieldName; this is convenient for when they are named the same, which is often the case
        this.relFieldName = UtilXml.checkEmpty(keyMapElement.getAttribute("rel-field-name"), this.fieldName);
    }

    /** name of the field in this entity */
    public String getFieldName() {
        return this.fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    /** name of the field in related entity */
    public String getRelFieldName() {
        return this.relFieldName;
    }

    public void setRelFieldName(String relFieldName) {
        this.relFieldName = relFieldName;
    }

    // ======= Some Convenience Oriented Factory Methods =======
    public static List makeKeyMapList(String fieldName1) {
        return UtilMisc.toList(new ModelKeyMap(fieldName1, null));
    }
    public static List makeKeyMapList(String fieldName1, String relFieldName1) {
        return UtilMisc.toList(new ModelKeyMap(fieldName1, relFieldName1));
    }
    public static List makeKeyMapList(String fieldName1, String relFieldName1, String fieldName2, String relFieldName2) {
        return UtilMisc.toList(new ModelKeyMap(fieldName1, relFieldName1), new ModelKeyMap(fieldName2, relFieldName2));
    }
    public static List makeKeyMapList(String fieldName1, String relFieldName1, String fieldName2, String relFieldName2, String fieldName3, String relFieldName3) {
        return UtilMisc.toList(new ModelKeyMap(fieldName1, relFieldName1), new ModelKeyMap(fieldName2, relFieldName2), new ModelKeyMap(fieldName3, relFieldName3));
    }

    public int hashCode() {
        return this.fieldName.hashCode() + this.relFieldName.hashCode();
    }

    public boolean equals(Object other) {
        ModelKeyMap otherKeyMap = (ModelKeyMap) other;

        if (!otherKeyMap.fieldName.equals(this.fieldName)) return false;
        if (!otherKeyMap.relFieldName.equals(this.relFieldName)) return false;

        return true;
    }

    public Element toXmlElement(Document document) {
        Element root = document.createElement("key-map");
        root.setAttribute("field-name", this.getFieldName());
        if (!this.getFieldName().equals(this.getRelFieldName())) {
            root.setAttribute("rel-field-name", this.getRelFieldName());
        }

        return root;
    }
}
