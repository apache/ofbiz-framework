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

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Locale;

/**
 * General output formatting functions - mainly for helping in JSPs
 */
public class UtilFormatOut {

    public static final String module = UtilFormatOut.class.getName();

    public static String safeToString(Object obj) {
        if (obj != null) {
            return obj.toString();
        } else {
            return "";
        }
    }
    
    // ------------------- price format handlers -------------------
    static DecimalFormat priceDecimalFormat = new DecimalFormat("#,##0.00");
    static DecimalFormat priceNumberFormat = new DecimalFormat("##0.00");

    /** Formats a Double representing a price into a string
     * @param price The price Double to be formatted
     * @return A String with the formatted price
     */
    public static String formatPrice(Double price) {
        if (price == null) return "";
        return formatPrice(price.doubleValue());
    }

    /** Formats a double representing a price into a string
     * @param price The price double to be formatted
     * @return A String with the formatted price
     */
    public static String formatPrice(double price) {
        return priceDecimalFormat.format(price);
    }

    public static Double formatPriceNumber(double price) {
        try {
            return new Double(priceDecimalFormat.parse(formatPrice(price)).doubleValue());
        } catch (ParseException e) {
            Debug.logError(e, module);
            return new Double(price);
        }
    }

    /** Formats a double into a properly formatted currency string based on isoCode and Locale
     * @param price The price double to be formatted
     * @param isoCode the currency ISO code
     * @param locale The Locale used to format the number
     * @param maximumFractionDigits The maximum number of fraction digits used; if set to -1 than the default value for the locale is used
     * @return A String with the formatted price
     */
    public static String formatCurrency(double price, String isoCode, Locale locale, int maximumFractionDigits) {
        //Debug.logInfo("formatting currency: " + price + ", isoCode: " + isoCode + ", locale: " + locale, module);
        com.ibm.icu.text.NumberFormat nf = com.ibm.icu.text.NumberFormat.getCurrencyInstance(locale);
        if (isoCode != null && isoCode.length() > 1) {
            nf.setCurrency(com.ibm.icu.util.Currency.getInstance(isoCode));
        } else {
            Debug.logWarning("No isoCode specified to format currency value:" + price, module);
        }
        if (maximumFractionDigits >= 0) {
            nf.setMaximumFractionDigits(maximumFractionDigits);
        }
        return nf.format(price);
    }

    /** Formats a double into a properly formatted currency string based on isoCode and Locale
     * @param price The price double to be formatted
     * @param isoCode the currency ISO code
     * @param locale The Locale used to format the number
     * @return A String with the formatted price
     */
    public static String formatCurrency(double price, String isoCode, Locale locale) {
        return formatCurrency(price, isoCode, locale, -1);
    }

    /** Formats a double into a properly formatted currency string based on isoCode and Locale
     * @param price The price Double to be formatted
     * @param isoCode the currency ISO code
     * @param locale The Locale used to format the number
     * @param maximumFractionDigits The maximum number of fraction digits used; if set to -1 than the default value for the locale is used
     * @return A String with the formatted price
     */
    public static String formatCurrency(Double price, String isoCode, Locale locale, int maximumFractionDigits) {
        return formatCurrency(price.doubleValue(), isoCode, locale, maximumFractionDigits);
    }

    /** Formats a double into a properly formatted currency string based on isoCode and Locale
     * @param price The price Double to be formatted
     * @param isoCode the currency ISO code
     * @param locale The Locale used to format the number
     * @return A String with the formatted price
     */
    public static String formatCurrency(Double price, String isoCode, Locale locale) {
        return formatCurrency(price.doubleValue(), isoCode, locale, -1);
    }

    /** Formats a Double into a properly spelled out number string based on Locale
     * @param amount The amount Double to be formatted
     * @param locale The Locale used to format the number
     * @return A String with the formatted number
     */
    public static String formatSpelledOutAmount(Double amount, Locale locale) {
        return formatSpelledOutAmount(amount.doubleValue(), locale);
    }
    /** Formats a double into a properly spelled out number string based on Locale
     * @param amount The amount double to be formatted
     * @param locale The Locale used to format the number
     * @return A String with the formatted number
     */
    public static String formatSpelledOutAmount(double amount, Locale locale) {
        //Debug.logInfo("formatting currency: " + price + ", isoCode: " + isoCode + ", locale: " + locale, module);
        com.ibm.icu.text.NumberFormat nf = new com.ibm.icu.text.RuleBasedNumberFormat(locale, com.ibm.icu.text.RuleBasedNumberFormat.SPELLOUT);
        return nf.format(amount);
    }

    /** Formats a double into a properly formatted string, with two decimals, based on Locale
     * @param amount The amount double to be formatted
     * @param locale The Locale used to format the number
     * @return A String with the formatted amount
     */
    // This method should be used in place of formatPrice because it is locale aware.
    public static String formatAmount(double amount, Locale locale) {
        com.ibm.icu.text.NumberFormat nf = com.ibm.icu.text.NumberFormat.getInstance(locale);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(amount);
    }

    // ------------------- percentage format handlers -------------------
    static DecimalFormat percentageDecimalFormat = new DecimalFormat("##0.##%");

    /** Formats a Double representing a percentage into a string
     * @param percentage The percentage Double to be formatted
     * @return A String with the formatted percentage
     */
    public static String formatPercentage(Double percentage) {
        if (percentage == null) return "";
        return formatPercentage(percentage.doubleValue());
    }

    /** Formats a double representing a percentage into a string
     * @param percentage The percentage double to be formatted
     * @return A String with the formatted percentage
     */
    public static String formatPercentage(double percentage) {
        return percentageDecimalFormat.format(percentage);
    }

    // ------------------- quantity format handlers -------------------
    static DecimalFormat quantityDecimalFormat = new DecimalFormat("#,##0.###");

    /** Formats an Long representing a quantity into a string
     * @param quantity The quantity Long to be formatted
     * @return A String with the formatted quantity
     */
    public static String formatQuantity(Long quantity) {
        if (quantity == null)
            return "";
        else
            return formatQuantity(quantity.doubleValue());
    }

    /** Formats an int representing a quantity into a string
     * @param quantity The quantity long to be formatted
     * @return A String with the formatted quantity
     */
    public static String formatQuantity(long quantity) {
        return formatQuantity((double) quantity);
    }

    /** Formats an Integer representing a quantity into a string
     * @param quantity The quantity Integer to be formatted
     * @return A String with the formatted quantity
     */
    public static String formatQuantity(Integer quantity) {
        if (quantity == null)
            return "";
        else
            return formatQuantity(quantity.doubleValue());
    }

    /** Formats an int representing a quantity into a string
     * @param quantity The quantity int to be formatted
     * @return A String with the formatted quantity
     */
    public static String formatQuantity(int quantity) {
        return formatQuantity((double) quantity);
    }

    /** Formats a Float representing a quantity into a string
     * @param quantity The quantity Float to be formatted
     * @return A String with the formatted quantity
     */
    public static String formatQuantity(Float quantity) {
        if (quantity == null)
            return "";
        else
            return formatQuantity(quantity.doubleValue());
    }

    /** Formats a float representing a quantity into a string
     * @param quantity The quantity float to be formatted
     * @return A String with the formatted quantity
     */
    public static String formatQuantity(float quantity) {
        return formatQuantity((double) quantity);
    }

    /** Formats an Double representing a quantity into a string
     * @param quantity The quantity Double to be formatted
     * @return A String with the formatted quantity
     */
    public static String formatQuantity(Double quantity) {
        if (quantity == null)
            return "";
        else
            return formatQuantity(quantity.doubleValue());
    }

    /** Formats an double representing a quantity into a string
     * @param quantity The quantity double to be formatted
     * @return A String with the formatted quantity
     */
    public static String formatQuantity(double quantity) {
        return quantityDecimalFormat.format(quantity);
    }
    
    public static String formatPaddedNumber(long number, int numericPadding) {
        StringBuffer outStrBfr = new StringBuffer(Long.toString(number));
        while (numericPadding > outStrBfr.length()) {
            outStrBfr.insert(0, '0');
        }
        return outStrBfr.toString();
    }
    
    public static String formatPaddingRemove(String original) {
        if (original == null) return null;
        StringBuffer orgBuf = new StringBuffer(original);
        while (orgBuf.length() > 0 && orgBuf.charAt(0) == '0') {
            orgBuf.deleteCharAt(0);
        }
        return orgBuf.toString();
    }
    
    
    // ------------------- date handlers -------------------          
    /** Formats a String timestamp into a nice string
     * @param timestamp String timestamp to be formatted
     * @return A String with the formatted date/time
     */
    public static String formatDate(java.sql.Timestamp timestamp) {
        if (timestamp == null)
            return "";
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.FULL);
        java.util.Date date = timestamp;
        return df.format(date);        
    }

    // ------------------- null string handlers -------------------
    /** Checks to see if the passed Object is null, if it is returns an empty but non-null string, otherwise calls toString() on the object
     * @param obj1 The passed Object
     * @return The toString() of the passed Object if not null, otherwise an empty non-null String
     */
    public static String makeString(Object obj1) {
        if (obj1 != null)
            return obj1.toString();
        else
            return "";
    }

    /** Checks to see if the passed string is null, if it is returns an empty but non-null string.
     * @param string1 The passed String
     * @return The passed String if not null, otherwise an empty non-null String
     */
    public static String checkNull(String string1) {
        if (string1 != null)
            return string1;
        else
            return "";
    }

    /** Returns the first passed String if not null, otherwise the second if not null, otherwise an empty but non-null String.
     * @param string1 The first passed String
     * @param string2 The second passed String
     * @return The first passed String if not null, otherwise the second if not null, otherwise an empty but non-null String
     */
    public static String checkNull(String string1, String string2) {
        if (string1 != null)
            return string1;
        else if (string2 != null)
            return string2;
        else
            return "";
    }

    /** Returns the first passed String if not null, otherwise the second if not null, otherwise the third if not null, otherwise an empty but non-null String.
     * @param string1 The first passed String
     * @param string2 The second passed String
     * @param string3 The third passed String
     * @return The first passed String if not null, otherwise the second if not null, otherwise the third if not null, otherwise an empty but non-null String
     */
    public static String checkNull(String string1, String string2, String string3) {
        if (string1 != null)
            return string1;
        else if (string2 != null)
            return string2;
        else if (string3 != null)
            return string3;
        else
            return "";
    }

    /** Returns the first passed String if not null, otherwise the second if not null, otherwise the third if not null, otherwise the fourth if not null, otherwise an empty but non-null String.
     * @param string1 The first passed String
     * @param string2 The second passed String
     * @param string3 The third passed String
     * @param string4 The fourth passed String
     * @return The first passed String if not null, otherwise the second if not null, otherwise the third if not null, otherwise the fourth if not null, otherwise an empty but non-null String
     */
    public static String checkNull(String string1, String string2, String string3, String string4) {
        if (string1 != null)
            return string1;
        else if (string2 != null)
            return string2;
        else if (string3 != null)
            return string3;
        else if (string4 != null)
            return string4;
        else
            return "";
    }

    /** Returns <code>pre + base + post</code> if base String is not null or empty, otherwise an empty but non-null String.
     * @param base The base String
     * @param pre The pre String
     * @param post The post String
     * @return <code>pre + base + post</code> if base String is not null or empty, otherwise an empty but non-null String.
     */
    public static String ifNotEmpty(String base, String pre, String post) {
        if (base != null && base.length() > 0)
            return pre + base + post;
        else
            return "";
    }

    /** Returns the first passed String if not empty, otherwise the second if not empty, otherwise an empty but non-null String.
     * @param string1 The first passed String
     * @param string2 The second passed String
     * @return The first passed String if not empty, otherwise the second if not empty, otherwise an empty but non-null String
     */
    public static String checkEmpty(String string1, String string2) {
        if (string1 != null && string1.length() > 0)
            return string1;
        else if (string2 != null && string2.length() > 0)
            return string2;
        else
            return "";
    }

    /** Returns the first passed String if not empty, otherwise the second if not empty, otherwise the third if not empty, otherwise an empty but non-null String.
     * @param string1 The first passed String
     * @param string2 The second passed String
     * @param string3 The third passed String
     * @return The first passed String if not empty, otherwise the second if not empty, otherwise the third if not empty, otherwise an empty but non-null String
     */
    public static String checkEmpty(String string1, String string2, String string3) {
        if (string1 != null && string1.length() > 0)
            return string1;
        else if (string2 != null && string2.length() > 0)
            return string2;
        else if (string3 != null && string3.length() > 0)
            return string3;
        else
            return "";
    }

    // ------------------- web encode handlers -------------------
    /** Encodes an HTTP URL query String, replacing characters used for other things in HTTP URL query strings, but not touching the separator characters '?', '=', and '&'
     * @param query The plain query String
     * @return The encoded String
     */
    public static String encodeQuery(String query) {
        String retString;

        retString = replaceString(query, "%", "%25");
        retString = replaceString(retString, " ", "%20");
        return retString;
    }

    /** Encodes a single HTTP URL query value, replacing characters used for other things in HTTP URL query strings
     * @param query The plain query value String
     * @return The encoded String
     */
    public static String encodeQueryValue(String query) {
        String retString;

        retString = replaceString(query, "%", "%25");
        retString = replaceString(retString, " ", "%20");
        retString = replaceString(retString, "&", "%26");
        retString = replaceString(retString, "?", "%3F");
        retString = replaceString(retString, "=", "%3D");
        return retString;
    }

    /** Replaces all occurances of oldString in mainString with newString
     * @param mainString The original string
     * @param oldString The string to replace
     * @param newString The string to insert in place of the old
     * @return mainString with all occurances of oldString replaced by newString
     */
    public static String replaceString(String mainString, String oldString, String newString) {
        return StringUtil.replaceString(mainString, oldString, newString);
    }

    /** Decodes a single query value from an HTTP URL parameter, replacing %ASCII values with characters
     * @param query The encoded query value String
     * @return The plain, decoded String
     */
    public static String decodeQueryValue(String query) {
        String retString;

        retString = replaceString(query, "%25", "%");
        retString = replaceString(retString, "%20", " ");
        retString = replaceString(retString, "%26", "&");
        retString = replaceString(retString, "%3F", "?");
        retString = replaceString(retString, "%3D", "=");
        return retString;
    }

    // ------------------- web encode handlers -------------------
    /** Encodes an XML string replacing the characters '<', '>', '"', ''', '&'
     * @param inString The plain value String
     * @return The encoded String
     */
    public static String encodeXmlValue(String inString) {
        String retString = inString;

        retString = StringUtil.replaceString(retString, "&", "&amp;");
        retString = StringUtil.replaceString(retString, "<", "&lt;");
        retString = StringUtil.replaceString(retString, ">", "&gt;");
        retString = StringUtil.replaceString(retString, "\"", "&quot;");
        retString = StringUtil.replaceString(retString, "'", "&apos;");
        return retString;
    }

    public static String padString(String str, int setLen, boolean padEnd, char padChar) {
        if (str == null) {
            return null;
        }
        if (setLen == 0) {
            return str;
        }
        int stringLen = str.length();
        int diff = setLen - stringLen;
        if (diff < 0) {
            return str.substring(0, setLen);
        } else {
            String newString = new String();
            if (padEnd) {
                newString = newString + str;
            }
            for (int i = 0; i < diff; i++) {
                newString = newString + padChar;
            }
            if (!padEnd) {
                newString = newString + str;
            }
            return newString;
        }
    }
    public static String makeSqlSafe(String unsafeString) {
        return unsafeString.replaceAll("'","''");
    }
}
