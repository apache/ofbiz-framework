/*
 * $Id: ModelField.java 5720 2005-09-13 03:10:59Z jonesde $
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

import java.io.Serializable;

/**
 * ModelField
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
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
}
