/*
 * $Id: ModelDataFile.java 5462 2005-08-05 18:35:48Z jonesde $
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


import java.util.*;


/**
 *  ModelDataFile
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */

public class ModelDataFile {

    public static final String SEP_FIXED_LENGTH = "fixed-length";
    public static final String SEP_FIXED_RECORD = "fixed-record";
    public static final String SEP_DELIMITED = "delimited";

    /** The name of the File */
    public String name = "";

    /** The type code of the File, ususally contained somewhere in the file and can be used to identify it */
    public String typeCode = "";

    /** The party that generally sends the file */
    public String sender = "";

    /** The party that generally receives the file */
    public String receiver = "";

    /** The length in bytes of a single record, ONLY if it uses fixed length records */
    public int recordLength = -1;

    /** The delimiter used in the file, if delimiter separated fields are used */
    public char delimiter = '|';

    /** The field serparator style, either fixed-length, or delimited */
    public String separatorStyle = "";

    /** A free form description of the file */
    public String description = "";

    /** List of record definitions for the file */
    public List records = new ArrayList();

    public ModelRecord getModelRecord(String recordName) {
        for (int i = 0; i < records.size(); i++) {
            ModelRecord curRecord = (ModelRecord) records.get(i);

            if (curRecord.name.equals(recordName)) {
                return curRecord;
            }
        }
        return null;
    }
}
