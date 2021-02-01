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
package org.apache.ofbiz.datafile;

import java.io.Serializable;

/**
 * ModelField
 */
@SuppressWarnings("serial")
public class ModelField implements Serializable {
    /** The name of the Field */
    private String name = "";

    /** The position of the field in the record - byte number for fixed-length, or field number for delimited */
    private int position = -1;

    /** The length of the Field in bytes, if applicable (mostly for fixed-length) */
    private int length = -1;

    /** The type of the Field */
    private String type = "";

    /** The format of the Field */
    private String format = "";

    /** The valid-exp of the Field */
    private String validExp = "";

    /** Free form description of the Field */
    private String description = "";

    /** Default value for the Field */
    private Object defaultValue = null;

    /** boolean which specifies whether or not the Field is a Primary Key */
    private boolean isPk = false;

    /** boolean which specifies whether or not the Field is ignored */
    private boolean ignored = false;

    /** boolean which specifies whether or not the Field is taken from the input file */
    private boolean expression = false;

    /** Referenced field */
    private String refField = null;

    /**
     * Gets name.
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets name.
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets position.
     * @return the position
     */
    public int getPosition() {
        return position;
    }

    /**
     * Gets length.
     * @return the length
     */
    public int getLength() {
        return length;
    }

    /**
     * Sets position.
     * @param position the position
     */
    public void setPosition(int position) {
        this.position = position;
    }

    /**
     * Sets length.
     * @param length the length
     */
    public void setLength(int length) {
        this.length = length;
    }

    /**
     * Sets type.
     * @param type the type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Gets type.
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * Sets format.
     * @param format the format
     */
    public void setFormat(String format) {
        this.format = format;
    }

    /**
     * Gets format.
     * @return the format
     */
    public String getFormat() {
        return format;
    }

    /**
     * Sets valid exp.
     * @param validExp the valid exp
     */
    public void setValidExp(String validExp) {
        this.validExp = validExp;
    }

    /**
     * Sets description.
     * @param description the description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Sets default value.
     * @param defaultValue the default value
     */
    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    /**
     * Sets pk.
     * @param pk the pk
     */
    public void setPk(boolean pk) {
        isPk = pk;
    }

    /**
     * Sets ignored.
     * @param ignored the ignored
     */
    public void setIgnored(boolean ignored) {
        this.ignored = ignored;
    }

    /**
     * Sets expression.
     * @param expression the expression
     */
    public void setExpression(boolean expression) {
        this.expression = expression;
    }

    /**
     * Sets ref field.
     * @param refField the ref field
     */
    public void setRefField(String refField) {
        this.refField = refField;
    }

    /**
     * Gets default value.
     * @return the default value
     */
    public Object getDefaultValue() {
        return defaultValue;
    }

    /**
     * Is expression boolean.
     * @return the boolean
     */
    public boolean isExpression() {
        return expression;
    }

    /**
     * Gets ref field.
     * @return the ref field
     */
    public String getRefField() {
        return refField;
    }

    /**
     * Is ignored boolean.
     * @return the boolean
     */
    public boolean isIgnored() {
        return ignored;
    }
}
