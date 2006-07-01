/*
 * $Id: ModelIndex.java 6327 2005-12-14 18:56:54Z jaz $
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

import java.util.*;
import org.w3c.dom.*;

import org.ofbiz.base.util.*;

/**
 * Generic Entity - Relation model class
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class ModelIndex extends ModelChild {

    /** the index name, used for the database index name */
    protected String name;

    /** specifies whether or not this index should include the unique constraint */
    protected boolean unique;

    /** list of the field names included in this index */
    protected List fieldNames = new ArrayList();

    /** Default Constructor */
    public ModelIndex() {
        name = "";
        unique = false;
    }

    /** Direct Create Constructor */
    public ModelIndex(ModelEntity mainEntity, String name, boolean unique) {
        super(mainEntity);
        this.name = name;
        this.unique = unique;
    }

    /** XML Constructor */
    public ModelIndex(ModelEntity mainEntity, Element indexElement) {
        super(mainEntity);

        this.name = UtilXml.checkEmpty(indexElement.getAttribute("name"));
        this.unique = "true".equals(UtilXml.checkEmpty(indexElement.getAttribute("unique")));

        NodeList indexFieldList = indexElement.getElementsByTagName("index-field");
        for (int i = 0; i < indexFieldList.getLength(); i++) {
            Element indexFieldElement = (Element) indexFieldList.item(i);

            if (indexFieldElement.getParentNode() == indexElement) {
                String fieldName = indexFieldElement.getAttribute("name");
                this.fieldNames.add(fieldName);            }
        }
    }

    /** the index name, used for the database index name */
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /** specifies whether or not this index should include the unique constraint */
    public boolean getUnique() {
        return this.unique;
    }

    public void setUnique(boolean unique) {
        this.unique = unique;
    }

    /** @deprecated
      * the main entity of this relation */
    public ModelEntity getMainEntity() {
        return getModelEntity();
    }

    /** @deprecated */
    public void setMainEntity(ModelEntity mainEntity) {
        setModelEntity(mainEntity);
    }

    public Iterator getIndexFieldsIterator() {
        return this.fieldNames.iterator();
    }

    public int getIndexFieldsSize() {
        return this.fieldNames.size();
    }

    public String getIndexField(int index) {
        return (String) this.fieldNames.get(index);
    }

    public void addIndexField(String fieldName) {
        this.fieldNames.add(fieldName);
    }

    public String removeIndexField(int index) {
        return (String) this.fieldNames.remove(index);
    }

    public Element toXmlElement(Document document) {
        Element root = document.createElement("index");
        root.setAttribute("name", this.getName());
        if (this.getUnique()) {
            root.setAttribute("unique", "true");
        }

        Iterator fnIter = this.fieldNames.iterator();
        while (fnIter != null && fnIter.hasNext()) {
            String fieldName = (String) fnIter.next();
            Element fn = document.createElement("index-field");
            fn.setAttribute("name", fieldName);
            root.appendChild(fn);
        }

        return root;
    }
}
