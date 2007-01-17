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
package org.ofbiz.base.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * UtilName
 *
 */
public class UtilName {

    public static final String module = UtilName.class.getName();
    public static final String[] prefix = { "MR", "MS", "MISS", "MRS","DR" };
    public static final String[] suffix = { "JR", "SR", "III", "PHD", "MD" };

    public static final int PREFIX = 1;
    public static final int FIRST = 2;
    public static final int MIDDLE = 3;
    public static final int LAST = 4;
    public static final int SUFFIX = 5;

    protected boolean middleIsInitial = false;
    protected String name[] = null;
    protected String raw = null;

    public UtilName(String name, boolean initial) {
        this.middleIsInitial = initial;
        this.raw = name;

        // check the name for empty elements
        String[] splitStr = name.split(" ");
        List goodElements = new ArrayList();
        for (int i = 0; i < splitStr.length; i++) {
            if (splitStr[i] != null && splitStr[i].length() > 0 && !splitStr[i].matches("\\W+")) {
                goodElements.add(splitStr[i]);
            }
        }

        // fill in the name array
        this.name = new String[goodElements.size()];
        for (int i = 0; i < this.name.length; i++) {
            this.name[i] = (String) goodElements.get(i);
        }
    }

    public UtilName(String name) {
        this(name, false);
    }

    protected UtilName() {
    }

    public String getRawString() {
        return this.raw;
    }
    
    public String getPrefix() {
        return this.getField(PREFIX);
    }

    public String getFirstName() {
        return this.getField(FIRST);
    }

    public String getMiddleName() {
        return this.getField(MIDDLE);
    }

    public String getLastName() {
        return this.getField(LAST);
    }

    public String getSuffix() {
        return this.getField(SUFFIX);
    }

    public String getField(int field) {
        int index[] = this.getFieldIndex(field);
        //System.out.println("Field : " + field + " - Index : " + indexString(index));
        if (index != null && index.length > 0) {
            if (index.length == 1) {
                return name[index[0]];
            } else {
                StringBuffer nameBuf = new StringBuffer();
                for (int i = 0; i < index.length; i++) {
                    if (nameBuf.length() > 0) {
                        nameBuf.append(" ");
                    }
                    nameBuf.append(name[index[i]]);
                }
                return nameBuf.toString();
            }
        }
        return null;
    }

    public Map getNameMap() {
        Map nameMap = new HashMap();
        nameMap.put("personalTitle", this.getPrefix());
        nameMap.put("firstName", this.getFirstName());
        nameMap.put("middleName", this.getMiddleName());
        nameMap.put("lastName", this.getLastName());
        nameMap.put("suffix", this.getSuffix());
        return nameMap;
    }

    protected String indexString(int[] index) {
        String str = "";
        if (index != null) {
            for (int i = 0; i < index.length; i++) {
                if (!"".equals(str)) {
                    str = str + ", ";
                }
                str = str + index[0];
            }
        }

        return str;
    }

    protected int[] getFieldIndex(int field) {
        if (name == null || name.length == 0) {
            return null;
        }
        switch(field) {
            case 1:
                // prefix
                String prefixChk = name[0].toUpperCase();
                prefixChk = prefixChk.replaceAll("\\W", "");
                if (this.checkValue(prefixChk, prefix)) {
                    return new int[] { 0 };
                }
                return null;
            case 2:
                // first name
                if (name.length == 2 || this.getFieldIndex(PREFIX) == null) {
                    return new int[] { 0 };
                } else {
                    for (int i = 1; i < name.length; i++) {
                        String s = name[i].toUpperCase().replaceAll("\\W", "");
                        if (!this.checkValue(s, prefix)) {
                            return new int[] { i };
                        }
                    }
                    return null;
                }
            case 3:
                // middle name

                int prefixIdx[] = this.getFieldIndex(PREFIX);
                int suffixIdx[] = this.getFieldIndex(SUFFIX);
                int minLength = 3;
                if (prefixIdx != null) {
                    minLength++;
                }
                if (suffixIdx != null) {
                    minLength++;
                }

                if (name.length >= minLength) {
                    int midIdx = prefixIdx != null ? 2 : 1;
                    if (middleIsInitial) {
                        String value = name[midIdx];
                        value = value.replaceAll("\\W", "");
                        if (value.length() == 1) {
                            return new int[] { midIdx };
                        }
                    } else {
                        return new int[] { midIdx };
                    }
                }
                return null;
            case 4:
                // last name
                if (name.length > 2) {
                    int firstIndex[] = this.getFieldIndex(FIRST);
                    int middleIndex[] = this.getFieldIndex(MIDDLE);
                    int suffixIndex[] = this.getFieldIndex(SUFFIX);

                    int lastBegin, lastEnd;
                    if (middleIndex != null) {
                        lastBegin = (middleIndex[middleIndex.length - 1]) + 1;
                    } else {
                        lastBegin = (firstIndex[firstIndex.length - 1]) + 1;
                    }
                    if (suffixIndex != null) {
                        lastEnd = (suffixIndex[0]) - 1;
                    } else {
                        lastEnd = name.length - 1;
                    }

                    if (lastEnd > lastBegin) {
                        return new int[] { lastBegin, lastEnd };
                    } else {
                        return new int[] { lastBegin };
                    }
                }
                return new int[] { name.length - 1 };
            case 5:
                // suffix
                int suffixIndex = name.length - 1;
                String suffixChk = name[suffixIndex].toUpperCase();
                suffixChk = suffixChk.replaceAll("\\W", "");

                for (int i = 0; i < suffix.length; i++) {
                    if (suffixChk.equals(suffix[i])) {
                        return new int[] { suffixIndex };
                    }
                }
                return null;
            default:
                return null;
        }
    }

    protected boolean checkValue(String field, String[] values) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(field)) {
                return true;
            }
        }
        return false;
    }

    public static Map parseName(String name, boolean middleIsInitial) {
        return (new UtilName(name, middleIsInitial)).getNameMap();
    }

    public static Map parseName(String name) {
        return parseName(name, false);
    }

    public static void main(String[] args) throws Exception {
        String name = "";
        for (int i = 0; i < args.length; i++) {
            if (!"".equals(name)) {
                name = name + " ";
            }
            name = name + args[i];
        }

        Map nameMap = parseName(name, true);
        Iterator i = nameMap.keySet().iterator();
        while (i.hasNext()) {
            String f = (String) i.next();
            System.out.println(f + " - " + nameMap.get(f));
        }
    }
}
