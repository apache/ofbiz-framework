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
package org.apache.ofbiz.widget.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.widget.renderer.FormStringRenderer;
import org.w3c.dom.Element;

/**
 * Form field abstract class.
 */
public abstract class FieldInfo {

    private static final String MODULE = FieldInfo.class.getName();

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
    public static final int CONTAINER = 21;
    public static final int MENU = 22;
    public static final int FORM = 23;
    public static final int GRID = 24;
    public static final int SCREEN = 25;
    // the numbering here represents the priority of the source;
    //when setting a new fieldInfo on a modelFormField it will only set
    //the new one if the fieldSource is less than or equal to the existing
    //fieldSource, which should always be passed as one of the following...
    public static final int SOURCE_EXPLICIT = 1;
    public static final int SOURCE_AUTO_ENTITY = 2;
    public static final int SOURCE_AUTO_SERVICE = 3;
    private static Map<String, Integer> fieldTypeByName = createFieldTypeMap();
    private static List<Integer> nonInputFieldTypeList = createNonInputFieldTypeList();

    private static Map<String, Integer> createFieldTypeMap() {
        Map<String, Integer> fieldTypeByName = new HashMap<>();
        fieldTypeByName.put("display", 1);
        fieldTypeByName.put("hyperlink", 2);
        fieldTypeByName.put("text", 3);
        fieldTypeByName.put("textarea", 4);
        fieldTypeByName.put("date-time", 5);
        fieldTypeByName.put("drop-down", 6);
        fieldTypeByName.put("check", 7);
        fieldTypeByName.put("radio", 8);
        fieldTypeByName.put("submit", 9);
        fieldTypeByName.put("reset", 10);
        fieldTypeByName.put("hidden", 11);
        fieldTypeByName.put("ignored", 12);
        fieldTypeByName.put("text-find", 13);
        fieldTypeByName.put("date-find", 14);
        fieldTypeByName.put("range-find", 15);
        fieldTypeByName.put("lookup", 16);
        fieldTypeByName.put("file", 17);
        fieldTypeByName.put("password", 18);
        fieldTypeByName.put("image", 19);
        fieldTypeByName.put("display-entity", 20);
        fieldTypeByName.put("container", 21);
        fieldTypeByName.put("include-menu", 22);
        fieldTypeByName.put("include-form", 23);
        fieldTypeByName.put("include-grid", 24);
        fieldTypeByName.put("include-screen", 25);
        return Collections.unmodifiableMap(fieldTypeByName);
    }

    private static List<Integer> createNonInputFieldTypeList() {
        List<Integer> nonInputFieldTypeList = new ArrayList<>();
        nonInputFieldTypeList.add(FieldInfo.IGNORED);
        nonInputFieldTypeList.add(FieldInfo.HIDDEN);
        nonInputFieldTypeList.add(FieldInfo.DISPLAY);
        nonInputFieldTypeList.add(FieldInfo.DISPLAY_ENTITY);
        nonInputFieldTypeList.add(FieldInfo.HYPERLINK);
        nonInputFieldTypeList.add(FieldInfo.MENU);
        nonInputFieldTypeList.add(FieldInfo.FORM);
        nonInputFieldTypeList.add(FieldInfo.GRID);
        nonInputFieldTypeList.add(FieldInfo.SCREEN);
        return Collections.unmodifiableList(nonInputFieldTypeList);
    }

    public static int findFieldTypeFromName(String name) {
        Integer fieldTypeInt = FieldInfo.fieldTypeByName.get(name);
        if (fieldTypeInt != null) {
            return fieldTypeInt;
        }
        throw new IllegalArgumentException("Could not get fieldType for field type name " + name);
    }

    public static boolean isInputFieldType(Integer fieldType) {
        return !nonInputFieldTypeList.contains(fieldType);
    }

    private final int fieldType;
    private final int fieldSource;
    private final ModelFormField modelFormField;

    /** XML Constructor */
    protected FieldInfo(Element element, ModelFormField modelFormField) {
        this.fieldSource = FieldInfo.SOURCE_EXPLICIT;
        this.fieldType = findFieldTypeFromName(UtilXml.getTagNameIgnorePrefix(element));
        this.modelFormField = modelFormField;
    }

    /** Value Constructor */
    protected FieldInfo(int fieldSource, int fieldType, ModelFormField modelFormField) {
        this.fieldType = fieldType;
        this.fieldSource = fieldSource;
        this.modelFormField = modelFormField;
    }

    public abstract void accept(ModelFieldVisitor visitor) throws Exception;

    /**
     * Returns a new instance of this object.
     * @param modelFormField
     */
    public abstract FieldInfo copy(ModelFormField modelFormField);

    /**
     * Gets field source.
     * @return the field source
     */
    public int getFieldSource() {
        return fieldSource;
    }

    /**
     * Gets field type.
     * @return the field type
     */
    public int getFieldType() {
        return fieldType;
    }

    /**
     * Gets model form field.
     * @return the model form field
     */
    public ModelFormField getModelFormField() {
        return modelFormField;
    }

    public abstract void renderFieldString(Appendable writer, Map<String, Object> context, FormStringRenderer formStringRenderer)
            throws IOException;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        ModelFieldVisitor visitor = new XmlWidgetFieldVisitor(sb);
        try {
            accept(visitor);
        } catch (Exception e) {
            Debug.logWarning(e, "Exception thrown in XmlWidgetFieldVisitor: ", MODULE);
        }
        return sb.toString();
    }
}
