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
package org.ofbiz.widget.form;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.ofbiz.widget.ModelFieldVisitor;
import org.w3c.dom.Element;

/**
 * Form field abstract class.
 */
public abstract class FieldInfo {

    public static final String module = FieldInfo.class.getName();
    public static final int DISPLAY = 1;
    public static final int HYPERLINK = 2;
    public static final int TEXT = 3;
    public static final int TEXTAREA = 4;
    public static final int DATE_TIME = 5;
    public static final int DROP_DOWN = 6;
    public static final int CHECK = 7;
    public static final int RADIO = 8;
    public static final int SUBMIT = 9;
    public static final int RESET = 10;
    public static final int HIDDEN = 11;
    public static final int IGNORED = 12;
    public static final int TEXTQBE = 13;
    public static final int DATEQBE = 14;
    public static final int RANGEQBE = 15;
    public static final int LOOKUP = 16;
    public static final int FILE = 17;
    public static final int PASSWORD = 18;
    public static final int IMAGE = 19;
    public static final int DISPLAY_ENTITY = 20;
    // the numbering here represents the priority of the source;
    //when setting a new fieldInfo on a modelFormField it will only set
    //the new one if the fieldSource is less than or equal to the existing
    //fieldSource, which should always be passed as one of the following...
    public static final int SOURCE_EXPLICIT = 1;
    public static final int SOURCE_AUTO_ENTITY = 2;
    public static final int SOURCE_AUTO_SERVICE = 3;
    private static Map<String, Integer> fieldTypeByName = createFieldTypeMap();

    private static Map<String, Integer> createFieldTypeMap() {
        Map<String, Integer> fieldTypeByName = new HashMap<String, Integer>();
        fieldTypeByName.put("display", Integer.valueOf(1));
        fieldTypeByName.put("hyperlink", Integer.valueOf(2));
        fieldTypeByName.put("text", Integer.valueOf(3));
        fieldTypeByName.put("textarea", Integer.valueOf(4));
        fieldTypeByName.put("date-time", Integer.valueOf(5));
        fieldTypeByName.put("drop-down", Integer.valueOf(6));
        fieldTypeByName.put("check", Integer.valueOf(7));
        fieldTypeByName.put("radio", Integer.valueOf(8));
        fieldTypeByName.put("submit", Integer.valueOf(9));
        fieldTypeByName.put("reset", Integer.valueOf(10));
        fieldTypeByName.put("hidden", Integer.valueOf(11));
        fieldTypeByName.put("ignored", Integer.valueOf(12));
        fieldTypeByName.put("text-find", Integer.valueOf(13));
        fieldTypeByName.put("date-find", Integer.valueOf(14));
        fieldTypeByName.put("range-find", Integer.valueOf(15));
        fieldTypeByName.put("lookup", Integer.valueOf(16));
        fieldTypeByName.put("file", Integer.valueOf(17));
        fieldTypeByName.put("password", Integer.valueOf(18));
        fieldTypeByName.put("image", Integer.valueOf(19));
        fieldTypeByName.put("display-entity", Integer.valueOf(20));
        fieldTypeByName.put("container", Integer.valueOf(21));
        return Collections.unmodifiableMap(fieldTypeByName);
    }

    public static int findFieldTypeFromName(String name) {
        Integer fieldTypeInt = FieldInfo.fieldTypeByName.get(name);
        if (fieldTypeInt != null) {
            return fieldTypeInt;
        } else {
            throw new IllegalArgumentException("Could not get fieldType for field type name " + name);
        }
    }

    private final int fieldType;
    private final int fieldSource;
    private final ModelFormField modelFormField;

    /** XML Constructor */
    protected FieldInfo(Element element, ModelFormField modelFormField) {
        this.fieldSource = FieldInfo.SOURCE_EXPLICIT;
        this.fieldType = findFieldTypeFromName(element.getTagName());
        this.modelFormField = modelFormField;
    }

    /** Value Constructor */
    protected FieldInfo(int fieldSource, int fieldType, ModelFormField modelFormField) {
        this.fieldType = fieldType;
        this.fieldSource = fieldSource;
        this.modelFormField = modelFormField;
    }

    public abstract void accept(ModelFieldVisitor visitor);

    public abstract FieldInfo copy(ModelFormField modelFormField);

    public int getFieldSource() {
        return fieldSource;
    }

    public int getFieldType() {
        return fieldType;
    }

    public ModelFormField getModelFormField() {
        return modelFormField;
    }

    public abstract void renderFieldString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer)
            throws IOException;
}
