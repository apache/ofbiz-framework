/*
 * $Id: ModelRecord.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2001-2003 The Open For Business Project - www.ofbiz.org
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
package org.ofbiz.datafile;


import java.util.ArrayList;
import java.util.List;


/**
 *  ModelRecord
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */

public class ModelRecord {

    public static final String LIMIT_ONE = "one";
    public static final String LIMIT_MANY = "many";

    /** The name of the Record */
    public String name = "";

    /** The type-code of the Record */
    public String typeCode = "";

    /** The minimum type-code of the Record, an alternative to the single type code */
    public String tcMin = "";
    public long tcMinNum = -1;

    /** The maximum type-code of the Record, an alternative to the single type code */
    public String tcMax = "";
    public long tcMaxNum = -1;

    /** specifies whether or not the type min and max are numbers, if so does a number compare, otherwise a String compare */
    public boolean tcIsNum = true;

    /** The position of the type-code of the Record */
    public int tcPosition = -1;

    /** The length of the type-code of the Record - optional */
    public int tcLength = -1;

    /** A free form description of the Record */
    public String description = "";

    /** The name of the parent record for this record, if any */
    public String parentName = "";

    /** The number limit of records to go under the parent, may be one or many */
    public String limit = "";

    public ModelRecord parentRecord = null;
    public List childRecords = new ArrayList();

    /** List of the fields that compose this record */
    public List fields = new ArrayList();

    ModelField getModelField(String fieldName) {
        for (int i = 0; i < fields.size(); i++) {
            ModelField curField = (ModelField) fields.get(i);

            if (curField.name.equals(fieldName)) {
                return curField;
            }
        }
        return null;
    }
}

