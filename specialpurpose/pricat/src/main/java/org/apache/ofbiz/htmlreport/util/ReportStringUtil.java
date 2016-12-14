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
package org.apache.ofbiz.htmlreport.util;

import java.util.Collection;
import java.util.Iterator;

/**
 * Provides String utility functions.<p>
 * 
 */
public final class ReportStringUtil {

    /** Constant for <code>"false"</code>. */
    public static final String FALSE = Boolean.toString(false);

    /** a convenient shorthand to the line separator constant. */
    public static final String LINE_SEPARATOR = System.getProperty("line.separator");

    /** Contains all chars that end a sentence in the {@link #trimToSize(String, int, int, String)} method. */
    public static final char[] SENTENCE_ENDING_CHARS = {'.', '!', '?'};

    /** a convenient shorthand for tabulations.  */
    public static final String TABULATOR = "  ";

    /** Constant for <code>"true"</code>. */
    public static final String TRUE = Boolean.toString(true);

    /** Day constant. */
    private static final long DAYS = 1000 * 60 * 60 * 24;

    /** Hour constant. */
    private static final long HOURS = 1000 * 60 * 60;

    /** Minute constant. */
    private static final long MINUTES = 1000 * 60;

    /** Second constant. */
    private static final long SECONDS = 1000;

    /** 
     * Default constructor (empty), private because this class has only 
     * static methods.<p>
     */
    private ReportStringUtil() {
        // empty
    }

    /**
     * Changes the filename suffix. 
     * 
     * @param filename the filename to be changed
     * @param suffix the new suffix of the file
     * 
     * @return the filename with the replaced suffix
     */
    public static String changeFileNameSuffixTo(String filename, String suffix) {

        int dotPos = filename.lastIndexOf('.');
        if (dotPos != -1) {
            return filename.substring(0, dotPos + 1) + suffix;
        } else {
            // the string has no suffix
            return filename;
        }
    }

    /**
     * Returns a string representation for the given collection using the given separator.<p>
     * 
     * @param collection the collection to print
     * @param separator the item separator
     * 
     * @return the string representation for the given collection
     */
    public static String collectionAsString(Collection<String> collection, String separator) {

        StringBuffer string = new StringBuffer(128);
        Iterator<String> it = collection.iterator();
        while (it.hasNext()) {
            string.append(it.next());
            if (it.hasNext()) {
                string.append(separator);
            }
        }
        return string.toString();
    }

    /**
     * Replaces occurrences of special control characters in the given input with 
     * a HTML representation.<p>
     * 
     * This method currently replaces line breaks to <code>&lt;br/&gt;</code> and special HTML chars 
     * like <code>&lt; &gt; &amp; &quot;</code> with their HTML entity representation.<p>
     * 
     * @param source the String to escape
     * 
     * @return the escaped String
     */
    public static String escapeHtml(String source) {

        if (source == null) {
            return null;
        }
        source = ReportEncoder.escapeXml(source);
        source = substitute(source, "\r", "");
        source = substitute(source, "\n", "<br/>\n");
        return source;
    }

    /**
     * Escapes a String so it may be used in JavaScript String definitions.<p>
     * 
     * This method replaces line breaks, quotation marks and \ characters.<p>
     * 
     * @param source the String to escape
     * 
     * @return the escaped String
     */
    public static String escapeJavaScript(String source) {

        source = substitute(source, "\\", "\\\\");
        source = substitute(source, "\"", "\\\"");
        source = substitute(source, "\'", "\\\'");
        source = substitute(source, "\r\n", "\\n");
        source = substitute(source, "\n", "\\n");
        return source;
    }

    /**
     * Escapes a String so it may be used as a Perl5 regular expression.<p>
     * 
     * This method replaces the following characters in a String:<br>
     * <code>{}[]()\$^.*+/</code><p>
     * 
     * @param source the string to escape
     * 
     * @return the escaped string
     */
    public static String escapePattern(String source) {

        if (source == null) {
            return null;
        }
        StringBuffer result = new StringBuffer(source.length() * 2);
        for (int i = 0; i < source.length(); ++i) {
            char ch = source.charAt(i);
            switch (ch) {
                case '\\':
                    result.append("\\\\");
                    break;
                case '/':
                    result.append("\\/");
                    break;
                case '$':
                    result.append("\\$");
                    break;
                case '^':
                    result.append("\\^");
                    break;
                case '.':
                    result.append("\\.");
                    break;
                case '*':
                    result.append("\\*");
                    break;
                case '+':
                    result.append("\\+");
                    break;
                case '|':
                    result.append("\\|");
                    break;
                case '?':
                    result.append("\\?");
                    break;
                case '{':
                    result.append("\\{");
                    break;
                case '}':
                    result.append("\\}");
                    break;
                case '[':
                    result.append("\\[");
                    break;
                case ']':
                    result.append("\\]");
                    break;
                case '(':
                    result.append("\\(");
                    break;
                case ')':
                    result.append("\\)");
                    break;
                default:
                    result.append(ch);
            }
        }
        return new String(result);
    }

    /**
     * Formats a runtime in the format hh:mm:ss, to be used e.g. in reports.<p>
     * 
     * If the runtime is greater then 24 hours, the format dd:hh:mm:ss is used.<p> 
     * 
     * @param runtime the time to format
     * 
     * @return the formatted runtime
     */
    public static String formatRuntime(long runtime) {

        long seconds = (runtime / SECONDS) % 60;
        long minutes = (runtime / MINUTES) % 60;
        long hours = (runtime / HOURS) % 24;
        long days = runtime / DAYS;
        StringBuffer strBuf = new StringBuffer();

        if (days > 0) {
            if (days < 10) {
                strBuf.append('0');
            }
            strBuf.append(days);
            strBuf.append(':');
        }

        if (hours < 10) {
            strBuf.append('0');
        }
        strBuf.append(hours);
        strBuf.append(':');

        if (minutes < 10) {
            strBuf.append('0');
        }
        strBuf.append(minutes);
        strBuf.append(':');

        if (seconds < 10) {
            strBuf.append('0');
        }
        strBuf.append(seconds);

        return strBuf.toString();
    }

    /**
     * Returns <code>true</code> if the provided String is either <code>null</code>
     * or the empty String <code>""</code>.<p> 
     * 
     * @param value the value to check
     * 
     * @return true, if the provided value is null or the empty String, false otherwise
     */
    public static boolean isEmpty(String value) {

        return (value == null) || (value.length() == 0);
    }

    /**
     * Returns <code>true</code> if the provided String is either <code>null</code>
     * or contains only white spaces.<p> 
     * 
     * @param value the value to check
     * 
     * @return true, if the provided value is null or contains only white spaces, false otherwise
     */
    public static boolean isEmptyOrWhitespaceOnly(String value) {

        return isEmpty(value) || (value.trim().length() == 0);
    }

    /**
     * Returns <code>true</code> if the provided Objects are either both <code>null</code> 
     * or equal according to {@link Object#equals(Object)}.<p>
     * 
     * @param value1 the first object to compare
     * @param value2 the second object to compare
     * 
     * @return <code>true</code> if the provided Objects are either both <code>null</code> 
     *              or equal according to {@link Object#equals(Object)} 
     */
    public static boolean isEqual(Object value1, Object value2) {

        if (value1 == null) {
            return (value2 == null);
        }
        return value1.equals(value2);
    }

    /**
     * Returns <code>true</code> if the provided String is neither <code>null</code>
     * nor the empty String <code>""</code>.<p> 
     * 
     * @param value the value to check
     * 
     * @return true, if the provided value is not null and not the empty String, false otherwise
     */
    public static boolean isNotEmpty(String value) {

        return (value != null) && (value.length() != 0);
    }

    /**
     * Returns <code>true</code> if the provided String is neither <code>null</code>
     * nor contains only white spaces.<p> 
     * 
     * @param value the value to check
     * 
     * @return <code>true</code>, if the provided value is <code>null</code> 
     *          or contains only white spaces, <code>false</code> otherwise
     */
    public static boolean isNotEmptyOrWhitespaceOnly(String value) {

        return (value != null) && (value.trim().length() > 0);
    }

    /**
     * Returns the last index of any of the given chars in the given source.<p> 
     * 
     * If no char is found, -1 is returned.<p>
     * 
     * @param source the source to check
     * @param chars the chars to find
     * 
     * @return the last index of any of the given chars in the given source, or -1
     */
    public static int lastIndexOf(String source, char[] chars) {

        // now try to find an "sentence ending" char in the text in the "findPointArea"
        int result = -1;
        for (int i = 0; i < chars.length; i++) {
            int pos = source.lastIndexOf(chars[i]);
            if (pos > result) {
                // found new last char
                result = pos;
            }
        }
        return result;
    }

    /**
     * Returns the last index a whitespace char the given source.<p> 
     * 
     * If no whitespace char is found, -1 is returned.<p>
     * 
     * @param source the source to check
     * 
     * @return the last index a whitespace char the given source, or -1
     */
    public static int lastWhitespaceIn(String source) {

        if (isEmpty(source)) {
            return -1;
        }
        int pos = -1;
        for (int i = source.length() - 1; i >= 0; i--) {
            if (Character.isWhitespace(source.charAt(i))) {
                pos = i;
                break;
            }
        }
        return pos;
    }

    /**
     * Substitutes <code>searchString</code> in the given source String with <code>replaceString</code>.<p>
     * 
     * This is a high-performance implementation which should be used as a replacement for 
     * <code>{@link String#replaceAll(java.lang.String, java.lang.String)}</code> in case no
     * regular expression evaluation is required.<p>
     * 
     * @param source the content which is scanned
     * @param searchString the String which is searched in content
     * @param replaceString the String which replaces <code>searchString</code>
     * 
     * @return the substituted String
     */
    public static String substitute(String source, String searchString, String replaceString) {

        if (source == null) {
            return null;
        }

        if (isEmpty(searchString)) {
            return source;
        }

        if (replaceString == null) {
            replaceString = "";
        }
        int len = source.length();
        int sl = searchString.length();
        int rl = replaceString.length();
        int length;
        if (sl == rl) {
            length = len;
        } else {
            int c = 0;
            int s = 0;
            int e;
            while ((e = source.indexOf(searchString, s)) != -1) {
                c++;
                s = e + sl;
            }
            if (c == 0) {
                return source;
            }
            length = len - (c * (sl - rl));
        }

        int s = 0;
        int e = source.indexOf(searchString, s);
        if (e == -1) {
            return source;
        }
        StringBuffer sb = new StringBuffer(length);
        while (e != -1) {
            sb.append(source.substring(s, e));
            sb.append(replaceString);
            s = e + sl;
            e = source.indexOf(searchString, s);
        }
        e = len;
        sb.append(source.substring(s, e));
        return sb.toString();
    }

    /**
     * Returns the java String literal for the given String. <p>
     *  
     * This is the form of the String that had to be written into source code 
     * using the unicode escape sequence for special characters. <p> 
     * 
     * Example: "�" would be transformed to "\\u00C4".<p>
     * 
     * @param s a string that may contain non-ascii characters 
     * 
     * @return the java unicode escaped string Literal of the given input string
     */
    public static String toUnicodeLiteral(String s) {

        StringBuffer result = new StringBuffer();
        char[] carr = s.toCharArray();

        String unicode;
        for (int i = 0; i < carr.length; i++) {
            result.append("\\u");
            // append leading zeros
            unicode = Integer.toHexString(carr[i]).toUpperCase();
            for (int j = 4 - unicode.length(); j > 0; j--) {
                result.append("0");
            }
            result.append(unicode);
        }
        return result.toString();
    }

    /**
     * Returns a substring of the source, which is at most length characters long.<p>
     * 
     * This is the same as calling {@link #trimToSize(String, int, String)} with the 
     * parameters <code>(source, length, " ...")</code>.<p>
     * 
     * @param source the string to trim
     * @param length the maximum length of the string to be returned
     * 
     * @return a substring of the source, which is at most length characters long
     */
    public static String trimToSize(String source, int length) {

        return trimToSize(source, length, length, " ...");
    }

    /**
     * Returns a substring of the source, which is at most length characters long.<p>
     * 
     * If a char is cut, the given <code>suffix</code> is appended to the result.<p>
     * 
     * This is almost the same as calling {@link #trimToSize(String, int, int, String)} with the 
     * parameters <code>(source, length, length*, suffix)</code>. If <code>length</code>
     * if larger then 100, then <code>length* = length / 2</code>,
     * otherwise <code>length* = length</code>.<p>
     * 
     * @param source the string to trim
     * @param length the maximum length of the string to be returned
     * @param suffix the suffix to append in case the String was trimmed
     * 
     * @return a substring of the source, which is at most length characters long
     */
    public static String trimToSize(String source, int length, String suffix) {

        int area = (length > 100) ? length / 2 : length;
        return trimToSize(source, length, area, suffix);
    }

    /**
     * Returns a substring of the source, which is at most length characters long, cut 
     * in the last <code>area</code> chars in the source at a sentence ending char or whitespace.<p>
     * 
     * If a char is cut, the given <code>suffix</code> is appended to the result.<p>
     * 
     * @param source the string to trim
     * @param length the maximum length of the string to be returned
     * @param area the area at the end of the string in which to find a sentence ender or whitespace
     * @param suffix the suffix to append in case the String was trimmed
     * 
     * @return a substring of the source, which is at most length characters long
     */
    public static String trimToSize(String source, int length, int area, String suffix) {

        if ((source == null) || (source.length() <= length)) {
            // no operation is required
            return source;
        }
        if (isEmpty(suffix)) {
            // we need an empty suffix
            suffix = "";
        }
        // must remove the length from the after sequence chars since these are always added in the end
        int modLength = length - suffix.length();
        if (modLength <= 0) {
            // we are to short, return beginning of the suffix
            return suffix.substring(0, length);
        }
        int modArea = area + suffix.length();
        if ((modArea > modLength) || (modArea < 0)) {
            // area must not be longer then max length
            modArea = modLength;
        }

        // first reduce the String to the maximum allowed length
        String findPointSource = source.substring(modLength - modArea, modLength);

        String result;
        // try to find an "sentence ending" char in the text
        int pos = lastIndexOf(findPointSource, SENTENCE_ENDING_CHARS);
        if (pos >= 0) {
            // found a sentence ender in the lookup area, keep the sentence ender
            result = source.substring(0, modLength - modArea + pos + 1) + suffix;
        } else {
            // no sentence ender was found, try to find a whitespace
            pos = lastWhitespaceIn(findPointSource);
            if (pos >= 0) {
                // found a whitespace, don't keep the whitespace
                result = source.substring(0, modLength - modArea + pos) + suffix;
            } else {
                // not even a whitespace was found, just cut away what's to long
                result = source.substring(0, modLength) + suffix;
            }
        }

        return result;
    }
}