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
    public String name = "";

    /** The position of the field in the record - byte number for fixed-length, or field number for delimited */
    public int position = -1;

    /** The length of the Field in bytes, if applicable (mostly for fixed-length) */
    public int length = -1;

    /** The type of the Field */
    public String type = "";

    /** The format of the Field */
    public String format = "";

    /** The valid-exp of the Field */
    public String validExp = "";

    /** Free form description of the Field */
    public String description = "";

    /** Default value for the Field */
    public Object defaultValue = null;

    /** boolean which specifies whether or not the Field is a Primary Key */
    public boolean isPk = false;

    /** boolean which specifies whether or not the Field is ignored */
    public boolean ignored = false;

    /** boolean which specifies whether or not the Field is taken from the input file */
    public boolean expression = false;

    /** Referenced field */
    public String refField = null;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
