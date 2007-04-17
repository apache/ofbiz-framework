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

import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Misc String Utility Functions
 *
 */
public class StringUtil {
    
    public static final String module = StringUtil.class.getName();

    /** 
     * Replaces all occurances of oldString in mainString with newString
     * @param mainString The original string
     * @param oldString The string to replace
     * @param newString The string to insert in place of the old
     * @return mainString with all occurances of oldString replaced by newString
     */
    public static String replaceString(String mainString, String oldString, String newString) {
        if (mainString == null) {
            return null;
        }
        if (oldString == null || oldString.length() == 0) {
            return mainString;
        }
        if (newString == null) {
            newString = "";
        }

        int i = mainString.lastIndexOf(oldString);

        if (i < 0) return mainString;

        StringBuffer mainSb = new StringBuffer(mainString);

        while (i >= 0) {
            mainSb.replace(i, i + oldString.length(), newString);
            i = mainString.lastIndexOf(oldString, i - 1);
        }
        return mainSb.toString();
    }

    /**
     * Creates a single string from a List of strings seperated by a delimiter.
     * @param list a list of strings to join
     * @param delim the delimiter character(s) to use. (null value will join with no delimiter)
     * @return a String of all values in the list seperated by the delimiter
     */
    public static String join(List list, String delim) {
        if (list == null || list.size() < 1)
            return null;
        StringBuffer buf = new StringBuffer();
        Iterator i = list.iterator();

        while (i.hasNext()) {
            buf.append((String) i.next());
            if (i.hasNext())
                buf.append(delim);
        }
        return buf.toString();
    }

    /**
     * Splits a String on a delimiter into a List of Strings.
     * @param str the String to split
     * @param delim the delimiter character(s) to join on (null will split on whitespace)
     * @return a list of Strings
     */
    public static List split(String str, String delim) {
        List splitList = null;
        StringTokenizer st = null;

        if (str == null)
            return splitList;

        if (delim != null)
            st = new StringTokenizer(str, delim);
        else
            st = new StringTokenizer(str);

        if (st != null && st.hasMoreTokens()) {
            splitList = FastList.newInstance();

            while (st.hasMoreTokens())
                splitList.add(st.nextToken());
        }
        return splitList;
    }

    /**
     * Encloses each of a List of Strings in quotes.
     * @param list List of String(s) to quote.
     */
    public static List quoteStrList(List list) {
        List tmpList = list;

        list = FastList.newInstance();
        Iterator i = tmpList.iterator();

        while (i.hasNext()) {
            String str = (String) i.next();

            str = "'" + str + "''";
            list.add(str);
        }
        return list;
    }

    /**
     * Creates a Map from an encoded name/value pair string
     * @param str The string to decode and format
     * @param trim Trim whitespace off fields
     * @return a Map of name/value pairs
     */
    public static Map strToMap(String str, boolean trim) {
        if (str == null) return null;
        Map decodedMap = FastMap.newInstance();
        List elements = split(str, "|");
        Iterator i = elements.iterator();

        while (i.hasNext()) {
            String s = (String) i.next();
            List e = split(s, "=");

            if (e.size() != 2) {
                continue;
            }
            String name = (String) e.get(0);
            String value = (String) e.get(1);
            if (trim) {
                if (name != null) {
                    name = name.trim();
                }
                if (value != null) {
                    value = value.trim();
                }
            }

            try {
                decodedMap.put(URLDecoder.decode(name, "UTF-8"), URLDecoder.decode(value, "UTF-8"));
            } catch (UnsupportedEncodingException e1) {                
                Debug.logError(e1, module);
            }
        }
        return decodedMap;
    }

    /**
     * Creates a Map from an encoded name/value pair string
     * @param str The string to decode and format
     * @return a Map of name/value pairs
     */
    public static Map strToMap(String str) {
        return strToMap(str, false);
    }

    /**
     * Creates an encoded String from a Map of name/value pairs (MUST BE STRINGS!)
     * @param map The Map of name/value pairs
     * @return String The encoded String
     */
    public static String mapToStr(Map map) {
        if (map == null) return null;
        StringBuffer buf = new StringBuffer();
        Set keySet = map.keySet();
        Iterator i = keySet.iterator();
        boolean first = true;

        while (i.hasNext()) {
            Object key = i.next();
            Object value = map.get(key);

            if (!(key instanceof String) || !(value instanceof String))
                continue;
            String encodedName = null;
            try {
                encodedName = URLEncoder.encode((String) key, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                Debug.logError(e, module);              
            }
            String encodedValue = null;
            try {
                encodedValue = URLEncoder.encode((String) value, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                Debug.logError(e, module);                
            }
            
            if (first)
                first = false;
            else
                buf.append("|");

            buf.append(encodedName);
            buf.append("=");
            buf.append(encodedValue);
        }
        return buf.toString();
    }

    /**
     * Reads a String version of a Map (should contain only strings) and creates a new Map
     *
     * @param s String value of a Map ({n1=v1, n2=v2})
     * @return new Map
     */
    public static Map toMap(String s) {
        Map newMap = FastMap.newInstance();
        if (s.startsWith("{") && s.endsWith("}")) {
            s = s.substring(1, s.length() - 1);
            String[] entry = s.split("\\,\\s");
            for (int i = 0; i < entry.length; i++) {
                String[] nv = entry[i].split("\\=");
                newMap.put(nv[0], nv[1]);
            }
        } else {
            throw new IllegalArgumentException("String is not from Map.toString()");
        }

        return newMap;
    }

    /**
     * Reads a String version of a List (should contain only strings) and creates a new List
     *
     * @param s String value of a Map ({n1=v1, n2=v2})
     * @return new List
     */
    public static List toList(String s) {
        List newList = FastList.newInstance();
        if (s.startsWith("[") && s.endsWith("]")) {
            s = s.substring(1, s.length() - 1);
            String[] entry = s.split("\\,\\s");
            for (int i = 0; i < entry.length; i++) {
                newList.add(entry[i]);
            }
        } else {
            throw new IllegalArgumentException("String is not from List.toString()");
        }

        return newList;
    }

    /**
     * Reads a String version of a Set (should contain only strings) and creates a new Set
     *
     * @param s String value of a Map ({n1=v1, n2=v2})
     * @return new List
     */
    public static Set toSet(String s) {
        Set newSet = FastSet.newInstance();
        if (s.startsWith("[") && s.endsWith("]")) {
            s = s.substring(1, s.length() - 1);
            String[] entry = s.split("\\,\\s");
            for (int i = 0; i < entry.length; i++) {
                newSet.add(entry[i]);
            }
        } else {
            throw new IllegalArgumentException("String is not from Set.toString()");
        }

        return newSet;    
    }

    /**
     * Create a Map from a List of keys and a List of values
     * @param keys List of keys
     * @param values List of values
     * @return Map of combined lists
     * @throws IllegalArgumentException When either List is null or the sizes do not equal
     */
    public static Map createMap(List keys, List values) {
        if (keys == null || values == null || keys.size() != values.size()) {
            throw new IllegalArgumentException("Keys and Values cannot be null and must be the same size");
        }
        Map newMap = FastMap.newInstance();
        for (int i = 0; i < keys.size(); i++) {
            newMap.put(keys.get(i), values.get(i));
        }
        return newMap;
    }

    /** Make sure the string starts with a forward slash but does not end with one; converts back-slashes to forward-slashes; if in String is null or empty, returns zero length string. */
    public static String cleanUpPathPrefix(String prefix) {
        if (prefix == null || prefix.length() == 0) return "";

        StringBuffer cppBuff = new StringBuffer(prefix.replace('\\', '/'));

        if (cppBuff.charAt(0) != '/') {
            cppBuff.insert(0, '/');
        }
        if (cppBuff.charAt(cppBuff.length() - 1) == '/') {
            cppBuff.deleteCharAt(cppBuff.length() - 1);
        }
        return cppBuff.toString();
    }
    
    /** Removes all spaces from a string */
    public static String removeSpaces(String str) {
        return removeRegex(str,"[\\ ]");
    }

    public static String toHexString(byte[] bytes) {
        StringBuffer buf = new StringBuffer(bytes.length * 2);
        for (int i = 0; i < bytes.length; i++) {
            buf.append(hexChar[(bytes[i] & 0xf0) >>> 4]);
            buf.append(hexChar[bytes[i] & 0x0f]);
        }
        return buf.toString();

    }

    public static String cleanHexString(String str) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) != 32 && str.charAt(i) != ':') {
                buf.append(str.charAt(i));
            }
        }
        return buf.toString();
    }

    public static byte[] fromHexString(String str) {
        str = cleanHexString(str);
        int stringLength = str.length();
        if ((stringLength & 0x1) != 0) {
            throw new IllegalArgumentException("fromHexString requires an even number of hex characters");
        }
        byte[] b = new byte[stringLength / 2];

        for (int i = 0, j = 0; i < stringLength; i+= 2, j++) {
            int high = convertChar(str.charAt(i));
            int low = convertChar(str.charAt(i+1));
            b[j] = (byte) ((high << 4) | low);
        }
        return b;
    }

    private static char[] hexChar = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
    public static int convertChar(char c) {
        if ( '0' <= c && c <= '9' ) {
            return c - '0' ;
        } else if ( 'a' <= c && c <= 'f' ) {
            return c - 'a' + 0xa ;
        } else if ( 'A' <= c && c <= 'F' ) {
            return c - 'A' + 0xa ;
        } else {
            throw new IllegalArgumentException("Invalid hex character: [" + c + "]");
        }
    }

    public static char[] encodeInt(int i, int j, char digestChars[]) {
        if (i < 16) {
            digestChars[j] = '0';
        }
        j++;
        do {
            digestChars[j--] = hexChar[i & 0xf];
            i >>>= 4;
        } while (i != 0);
        return digestChars;
    }

    /** Removes all non-numbers from str */
    public static String removeNonNumeric(String str) {
        return removeRegex(str,"[\\D]");
    }
    
    /** Removes all numbers from str */
    public static String removeNumeric(String str) {
        return removeRegex(str,"[\\d]");
    }

    /**
     * @param str
     * @param regex
     * Removes all matches of regex from a str
     */
    private static String removeRegex(String str, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(str);
        return matcher.replaceAll("");
    }
    
    /**
     * Add the number to the string, keeping (padding to min of original length)
     * 
     * @return
     */
    public static String addToNumberString(String numberString, long addAmount) {
	if (numberString == null) return null;
	
	int origLength = numberString.length();
	long number = Long.parseLong(numberString);
        return padNumberString(Long.toString(number + addAmount), origLength);
    }
    
    public static String padNumberString(String numberString, int targetMinLength) {
        StringBuffer outStrBfr = new StringBuffer(numberString); 
        while (targetMinLength > outStrBfr.length()) {
            outStrBfr.insert(0, '0');
        }
        return outStrBfr.toString();
    }

    /**
     * Translates various HTML characters in a string so that the string can be displayed in a browser safely
     * <p>
     * This function is useful in preventing user-supplied text from containing HTML markup, such as in a message board or
     * guest book application. The optional arguments doubleQuotes and singleQuotes allow the control of the substitution of
     * the quote characters.  The default is to translate them with the HTML equivalent.
     * </p>
     * The translations performed are: <ol>
     *    <li>'&' (ampersand) becomes '&amp;'
     *    <li>'"' (double quote) becomes '&quot;' when doubleQuotes is true.
     *    <li>''' (single quote) becomes '&#039;' when singleQuotes is true.
     *    <li>'<' (less than) becomes '&lt;'
     *    <li>'>' (greater than) becomes '&gt;'
     *    <li>\n (Carriage Return) becomes '&lt;br&gt;gt;'
     * </ol>
     */
    public static String htmlSpecialChars(String html, boolean doubleQuotes, boolean singleQuotes, boolean insertBR) {
        html = StringUtil.replaceString(html, "&", "&amps;");
        html = StringUtil.replaceString(html, "<", "&lt;");
        html = StringUtil.replaceString(html, ">", "&gt;");
        if (doubleQuotes) {
            html = StringUtil.replaceString(html, "\"", "&quot;");
        }
        if (singleQuotes) {
            html = StringUtil.replaceString(html, "'", "&#039");
        }
        if (insertBR) {
            html = StringUtil.replaceString(html, "\n", "<br>");
        }

        return html;
    }
    public static String htmlSpecialChars(String html) {
        return htmlSpecialChars(html, true, true, true);
    }    
    
}
