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
package org.apache.ofbiz.base.util;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.validator.routines.EmailValidator;
import org.apache.ofbiz.base.lang.IsEmpty;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtilProperties;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.ibm.icu.util.Calendar;

/**
 * General input/data validation methods
 * Utility methods for validating data, especially input.
 * See detailed description below.
 *
 * <br> SUMMARY
 * <br>
 * <br> This is a set of meethods for validating input. Functions are provided to validate:
 * <br>    - U.S. and international phone/fax numbers
 * <br>    - U.S. ZIP codes(5 or 9 digit postal codes)
 * <br>    - U.S. Postal Codes(2 letter abbreviations for names of states)
 * <br>    - U.S. Social Security Numbers(abbreviated as SSNs)
 * <br>    - email addresses
 * <br>       - dates(entry of year, month, and day and validity of combined date)
 * <br>       - credit card numbers
 * <br>
 * <br> Supporting utility functions validate that:
 * <br>    - characters are Letter, Digit, or LetterOrDigit
 * <br>    - strings are a Signed, Positive, Negative, Nonpositive, or Nonnegative integer
 * <br>    - strings are a Float or a SignedFloat
 * <br>    - strings are Alphabetic, Alphanumeric, or Whitespace
 * <br>    - strings contain an integer within a specified range
 * <br>
 * <br> Other utility functions are provided to:
 * <br>    - remove from a string characters which are/are not in a "bag" of selected characters
 * <br>       - strip whitespace/leading whitespace from a string
 * <br>
 * <br> ==============================================================================
 * <br> NOTE: This code was adapted from the Netscape JavaScript form validation code,
 * <br> usually found in "FormChek.js". Credit card verification functions Originally
 * <br> included as Starter Application 1.0.0 in LivePayment.
 * <br> ==============================================================================
 */
public final class UtilValidate {

    public static final String module = UtilValidate.class.getName();

    private UtilValidate() {}

    /** boolean specifying by default whether or not it is okay for a String to be empty */
    private static final boolean defaultEmptyOK = true;

    /** digit characters */
    public static final String digits = "0123456789";

    /** hex digit characters */
    private static final String hexDigits = digits + "abcdefABCDEF";

    /** lower-case letter characters */
    public static final String lowercaseLetters = "abcdefghijklmnopqrstuvwxyz";

    /** upper-case letter characters */
    public static final String uppercaseLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    /** letter characters */
    public static final String letters = lowercaseLetters + uppercaseLetters;

    /** whitespace characters */
    private static final String whitespace = " \t\n\r";

    /** decimal point character differs by language and culture */
    public static final String decimalPointDelimiter = ".";

    /** non-digit characters which are allowed in phone numbers */
    public static final String phoneNumberDelimiters = "()- ";

    /** characters which are allowed in US phone numbers */
    public static final String validUSPhoneChars = digits + phoneNumberDelimiters;

    /** characters which are allowed in international phone numbers(a leading + is OK) */
    public static final String validWorldPhoneChars = digits + phoneNumberDelimiters + "+";

    /** non-digit characters which are allowed in Social Security Numbers */
    public static final String SSNDelimiters = "- ";

    /** characters which are allowed in Social Security Numbers */
    public static final String validSSNChars = digits + SSNDelimiters;

    /** U.S. Social Security Numbers have 9 digits. They are formatted as 123-45-6789. */
    public static final int digitsInSocialSecurityNumber = 9;

    /** U.S. phone numbers have 10 digits. They are formatted as 123 456 7890 or(123) 456-7890. */
    public static final int digitsInUSPhoneNumber = 10;
    public static final int digitsInUSPhoneAreaCode = 3;
    public static final int digitsInUSPhoneMainNumber = 7;

    /** non-digit characters which are allowed in ZIP Codes */
    public static final String ZipCodeDelimiters = "-";

    /** our preferred delimiter for reformatting ZIP Codes */
    public static final String ZipCodeDelimeter = "-";

    /** characters which are allowed in Social Security Numbers */
    public static final String validZipCodeChars = digits + ZipCodeDelimiters;

    /** U.S. ZIP codes have 5 or 9 digits. They are formatted as 12345 or 12345-6789. */
    public static final int digitsInZipCode1 = 5;

    /** U.S. ZIP codes have 5 or 9 digits. They are formatted as 12345 or 12345-6789. */
    public static final int digitsInZipCode2 = 9;

    /** non-digit characters which are allowed in credit card numbers */
    public static final String creditCardDelimiters = " -";

    /** An array of ints representing the number of days in each month of the year.
     *  Note: February varies depending on the year */
    static final int[] daysInMonth = { 31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };

    /** Delimiter for USStateCodes String */
    public static final String USStateCodeDelimiter = "|";

    /** Valid U.S. Postal Codes for states, territories, armed forces, etc.
     * See http://www.usps.gov/ncsc/lookups/abbr_state.txt. */
    public static final String USStateCodes = "AL|AK|AS|AZ|AR|CA|CO|CT|DE|DC|FM|FL|GA|GU|HI|ID|IL|IN|IA|KS|KY|LA|ME|MH|MD|MA|MI|MN|MS|MO|MT|NE|NV|NH|NJ|NM|NY|NC|ND|MP|OH|OK|OR|PW|PA|PR|RI|SC|SD|TN|TX|UT|VT|VI|VA|WA|WV|WI|WY|AE|AA|AE|AE|AP";

    /** Valid contiguous U.S. postal codes */
    public static final String ContiguousUSStateCodes = "AL|AZ|AR|CA|CO|CT|DE|DC|FL|GA|ID|IL|IN|IA|KS|KY|LA|ME|MD|MA|MI|MN|MS|MO|MT|NE|NV|NH|NJ|NM|NY|NC|ND|OH|OK|OR|PA|RI|SC|SD|TN|TX|UT|VT|VA|WA|WV|WI|WY";

    /** Check whether an object is empty, will see if it is a String, Map, Collection, etc. */
    public static boolean isEmpty(Object o) {
        return ObjectType.isEmpty(o);
    }

    /** Check whether an object is NOT empty, will see if it is a String, Map, Collection, etc. */
    public static boolean isNotEmpty(Object o) {
        return !ObjectType.isEmpty(o);
    }

    /** Check whether IsEmpty o is empty. */
    public static boolean isEmpty(IsEmpty o) {
        return o == null || o.isEmpty();
    }

    /** Check whether IsEmpty o is NOT empty. */
    public static boolean isNotEmpty(IsEmpty o) {
        return o != null && !o.isEmpty();
    }

    /** Check whether collection c is empty. */
    public static <E> boolean isEmpty(Collection<E> c) {
        return (c == null) || c.isEmpty();
    }

    /** Check whether map m is empty. */
    public static <K,E> boolean isEmpty(Map<K,E> m) {
        return (m == null) || m.isEmpty();
    }

    /** Check whether charsequence c is empty. */
    public static boolean isEmpty(CharSequence c) {
        return (c == null) || c.length() == 0;
    }

    /** Check whether collection c is NOT empty. */
    public static <E> boolean isNotEmpty(Collection<E> c) {
        return (c != null) && !c.isEmpty();
    }

    /** Check whether charsequence c is NOT empty. */
    public static boolean isNotEmpty(CharSequence c) {
        return ((c != null) && (c.length() > 0));
    }

    public static boolean isString(Object obj) {
        return ((obj != null) && (obj instanceof java.lang.String));
    }

    /** Returns true if string s is empty or whitespace characters only. */
    public static boolean isWhitespace(String s) {
        // Is s empty?
        if (isEmpty(s)) {
            return true;
        }

        // Search through string's characters one by one
        // until we find a non-whitespace character.
        // When we do, return false; if we don't, return true.
        for (int i = 0; i < s.length(); i++) {
            // Check that current character isn't whitespace.
            char c = s.charAt(i);

            if (whitespace.indexOf(c) == -1) {
                return false;
            }
        }
        // All characters are whitespace.
        return true;
    }

    /** Removes all characters which appear in string bag from string s. */
    public static String stripCharsInBag(String s, String bag) {
        int i;
        StringBuilder stringBuilder = new StringBuilder("");

        // Search through string's characters one by one.
        // If character is not in bag, append to returnString.
        for (i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (bag.indexOf(c) == -1) {
                stringBuilder.append(c);
            }
        }
        return stringBuilder.toString();
    }

    /** Removes all characters which do NOT appear in string bag from string s. */
    public static String stripCharsNotInBag(String s, String bag) {
        int i;
        StringBuilder stringBuilder = new StringBuilder("");

        // Search through string's characters one by one.
        // If character is in bag, append to returnString.
        for (i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (bag.indexOf(c) != -1) {
                stringBuilder.append(c);
            }
        }
        return stringBuilder.toString();
    }

    /** Removes all whitespace characters from s.
     *  Member whitespace(see above) defines which characters are considered whitespace. */
    public static String stripWhitespace(String s) {
        return stripCharsInBag(s, whitespace);
    }

    /** Returns true if single character c(actually a string) is contained within string s. */
    public static boolean charInString(char c, String s) {
        return (s.indexOf(c) != -1);
    }

    /** Removes initial(leading) whitespace characters from s.
     *  Member whitespace(see above) defines which characters are considered whitespace. */
    public static String stripInitialWhitespace(String s) {
        int i = 0;

        while ((i < s.length()) && charInString(s.charAt(i), whitespace)) {
            i++;
        }
        return s.substring(i);
    }

    /** Returns true if character c is an English letter (A .. Z, a..z).
     *
     *  NOTE: Need i18n version to support European characters.
     *  This could be tricky due to different character
     *  sets and orderings for various languages and platforms. */
    public static boolean isLetter(char c) {
        return Character.isLetter(c);
    }

    /** Returns true if character c is a digit (0 .. 9). */
    public static boolean isDigit(char c) {
        return Character.isDigit(c);
    }

    /** Returns true if character c is a letter or digit. */
    public static boolean isLetterOrDigit(char c) {
        return Character.isLetterOrDigit(c);
    }

    /** Returns true if character c is a letter or digit. */
    public static boolean isHexDigit(char c) {
        return hexDigits.indexOf(c) >= 0;
    }

    /** Returns true if all characters in string s are numbers.
     *
     *  Accepts non-signed integers only. Does not accept floating
     *  point, exponential notation, etc.
     */
    public static boolean isInteger(String s) {
        if (isEmpty(s)) {
            return defaultEmptyOK;
        }

        // Search through string's characters one by one
        // until we find a non-numeric character.
        // When we do, return false; if we don't, return true.
        for (int i = 0; i < s.length(); i++) {
            // Check that current character is number.
            char c = s.charAt(i);

            if (!isDigit(c)) {
                return false;
            }
        }

        // All characters are numbers.
        return true;
    }

    /** Returns true if all characters are numbers;
     *  first character is allowed to be + or - as well.
     *
     *  Does not accept floating point, exponential notation, etc.
     */
    public static boolean isSignedInteger(String s) {
        if (isEmpty(s)) {
            return defaultEmptyOK;
        }
        try {
            Integer.parseInt(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Returns true if all characters are numbers;
     *  first character is allowed to be + or - as well.
     *
     *  Does not accept floating point, exponential notation, etc.
     */
    public static boolean isSignedLong(String s) {
        if (isEmpty(s)) {
            return defaultEmptyOK;
        }
        try {
            Long.parseLong(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns true if string s is an integer &gt; 0.
     * NOTE: using the Java Long object for greatest precision
     */
    public static boolean isPositiveInteger(String s) {
        if (isEmpty(s)) {
            return defaultEmptyOK;
        }

        try {
            long temp = Long.parseLong(s);
            return temp > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns true if string s is an integer &gt;= 0
     */
    public static boolean isNonnegativeInteger(String s) {
        if (isEmpty(s)) {
            return defaultEmptyOK;
        }

        try {
            int temp = Integer.parseInt(s);
            return temp >= 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns true if string s is an integer &lt; 0
     */
    public static boolean isNegativeInteger(String s) {
        if (isEmpty(s)) {
            return defaultEmptyOK;
        }

        try {
            int temp = Integer.parseInt(s);
            return temp < 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns true if string s is an integer &lt;= 0
     */
    public static boolean isNonpositiveInteger(String s) {
        if (isEmpty(s)) {
            return defaultEmptyOK;
        }

        try {
            int temp = Integer.parseInt(s);
            return temp <= 0;
        } catch (Exception e) {
            return false;
        }
    }

    /** True if string s is an unsigned floating point(real) number.
     *
     *  Also returns true for unsigned integers. If you wish
     *  to distinguish between integers and floating point numbers,
     *  first call isInteger, then call isFloat.
     *
     *  Does not accept exponential notation.
     */
    public static boolean isFloat(String s) {
        if (isEmpty(s)) {
            return defaultEmptyOK;
        }

        boolean seenDecimalPoint = false;

        if (s.startsWith(decimalPointDelimiter)) {
            return false;
        }

        // Search through string's characters one by one
        // until we find a non-numeric character.
        // When we do, return false; if we don't, return true.
        for (int i = 0; i < s.length(); i++) {
            // Check that current character is number.
            char c = s.charAt(i);

            if (c == decimalPointDelimiter.charAt(0)) {
                if (!seenDecimalPoint) {
                    seenDecimalPoint = true;
                } else {
                    return false;
                }
            } else {
                if (!isDigit(c)) {
                    return false;
                }
            }
        }
        // All characters are numbers.
        return true;
    }

    /** General routine for testing whether a string is a float.
     */
    public static boolean isFloat(String s, boolean allowNegative, boolean allowPositive, int minDecimal, int maxDecimal) {
        if (isEmpty(s)) {
            return defaultEmptyOK;
        }

        try {
            float temp = Float.parseFloat(s);
            if ((!allowNegative && temp < 0) || (!allowPositive && temp > 0)) {
                return false;
            }
            int decimalPoint = s.indexOf(".");
            if (decimalPoint == -1) {
                return !(minDecimal > 0);
            }
            // 1.2345; length=6; point=1; num=4
            int numDecimals = s.length() - decimalPoint - 1;
            return !(minDecimal >= 0 && numDecimals < minDecimal) && !(maxDecimal >= 0 && numDecimals > maxDecimal);
        } catch (Exception e) {
            return false;
        }
    }

    /** General routine for testing whether a string is a double.
     */
    public static boolean isDouble(String s, boolean allowNegative, boolean allowPositive, int minDecimal, int maxDecimal) {
        if (isEmpty(s)) {
            return defaultEmptyOK;
        }

        try {
            double temp = Double.parseDouble(s);
            if ((!allowNegative && temp < 0) || (!allowPositive && temp > 0)) {
                return false;
            }
            int decimalPoint = s.indexOf(".");
            if (decimalPoint == -1) {
                return !(minDecimal > 0);
            }
            // 1.2345; length=6; point=1; num=4
            int numDecimals = s.length() - decimalPoint - 1;
            return !(minDecimal >= 0 && numDecimals < minDecimal) && !(maxDecimal >= 0 && numDecimals > maxDecimal);
        } catch (Exception e) {
            return false;
        }
    }

    /** True if string s is a signed or unsigned floating point
     *  (real) number. First character is allowed to be + or -.
     *
     *  Also returns true for unsigned integers. If you wish
     *  to distinguish between integers and floating point numbers,
     *  first call isSignedInteger, then call isSignedFloat.
     */
    public static boolean isSignedFloat(String s) {
        if (isEmpty(s)) {
            return defaultEmptyOK;
        }
        try {
            Float.parseFloat(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** True if string s is a signed or unsigned floating point
     *  (real) number. First character is allowed to be + or -.
     *
     *  Also returns true for unsigned integers. If you wish
     *  to distinguish between integers and floating point numbers,
     *  first call isSignedInteger, then call isSignedFloat.
     */
    public static boolean isSignedDouble(String s) {
        if (isEmpty(s)) {
            return defaultEmptyOK;
        }
        try {
            Double.parseDouble(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Returns true if string s is letters only.
     *
     *  NOTE: This should handle i18n version to support European characters, etc.
     *  since it now uses Character.isLetter()
     */
    public static boolean isAlphabetic(String s) {
        if (isEmpty(s)) {
            return defaultEmptyOK;
        }

        // Search through string's characters one by one
        // until we find a non-alphabetic character.
        // When we do, return false; if we don't, return true.
        for (int i = 0; i < s.length(); i++) {
            // Check that current character is letter.
            char c = s.charAt(i);

            if (!isLetter(c)) {
                return false;
            }
        }

        // All characters are letters.
        return true;
    }

    /** Returns true if string s is English letters (A .. Z, a..z) and numbers only.
     *
     *  NOTE: Need i18n version to support European characters.
     *  This could be tricky due to different character
     *  sets and orderings for various languages and platforms.
     */
    public static boolean isAlphanumeric(String s) {
        if (isEmpty(s)) {
            return defaultEmptyOK;
        }

        // Search through string's characters one by one
        // until we find a non-alphanumeric character.
        // When we do, return false; if we don't, return true.
        for (int i = 0; i < s.length(); i++) {
            // Check that current character is number or letter.
            char c = s.charAt(i);

            if (!isLetterOrDigit(c)) {
                return false;
            }
        }

        // All characters are numbers or letters.
        return true;
    }

    /* ================== METHODS TO CHECK VARIOUS FIELDS. ==================== */

    /** isSSN returns true if string s is a valid U.S. Social Security Number.  Must be 9 digits. */
    public static boolean isSSN(String s) {
        if (isEmpty(s)) {
            return defaultEmptyOK;
        }

        String normalizedSSN = stripCharsInBag(s, SSNDelimiters);

        return (isInteger(normalizedSSN) && normalizedSSN.length() == digitsInSocialSecurityNumber);
    }

    /** isZIPCode returns true if string s is a valid U.S. ZIP code.  Must be 5 or 9 digits only. */
    public static boolean isZipCode(String s) {
        if (isEmpty(s)) {
            return defaultEmptyOK;
        }

        String normalizedZip = stripCharsInBag(s, ZipCodeDelimiters);

        return (isInteger(normalizedZip) && ((normalizedZip.length() == digitsInZipCode1) || (normalizedZip.length() == digitsInZipCode2)));
    }

    /** Returns true if string s is a valid contiguous U.S. Zip code.  Must be 5 or 9 digits only. */
    public static boolean isContiguousZipCode(String s) {
        boolean retval = false;
        if (isZipCode(s)) {
            if (isEmpty(s)) {
                retval = defaultEmptyOK;
            } else {
                String normalizedZip = s.substring(0,5);
                int iZip = Integer.parseInt(normalizedZip);
                if ((iZip >= 96701 && iZip <= 96898) || (iZip >= 99501 && iZip <= 99950)) {
                    retval = false;
                } else {
                    retval = true;
                }
            }
        }
        return retval;
    }

    /** Return true if s is a valid U.S. Postal Code (abbreviation for state). */
    public static boolean isStateCode(String s) {
        if (isEmpty(s)) {
            return defaultEmptyOK;
        }
        return ((USStateCodes.indexOf(s) != -1) && (s.indexOf(USStateCodeDelimiter) == -1));
    }

    /** Return true if s is a valid contiguous U.S. Postal Code (abbreviation for state). */
    public static boolean isContiguousStateCode(String s) {
        if (isEmpty(s)) {
            return defaultEmptyOK;
        }
        return ((ContiguousUSStateCodes.indexOf(s) != -1) && (s.indexOf(USStateCodeDelimiter) == -1));
    }

    public static boolean isEmail(String s) {
        if (isEmpty(s)) {
            return defaultEmptyOK;
        }
        return EmailValidator.getInstance().isValid(s);
    }

    /**
     * Checks a String for a valid Email-List seperated by ",".
     */
    public static boolean isEmailList(String s) {
        if (isEmpty(s)) {
            return defaultEmptyOK;
        }
        String[] emails = s.split(",");
        for (String email : emails) {
            if (!EmailValidator.getInstance().isValid(email)) {
                return false;
            }
        }
        return true;
    }

    /** isUrl returns true if the string contains ://
     * @param s String to validate
     * @return true if s contains ://
     */
    public static boolean isUrl(String s) {
        if (isEmpty(s)) {
            return defaultEmptyOK;
        }
        return s.indexOf("://") != -1;
    }

    /** isYear returns true if string s is a valid
     *  Year number.  Must be 2 or 4 digits only.
     *
     *  For Year 2000 compliance, you are advised
     *  to use 4-digit year numbers everywhere.
     */
    public static boolean isYear(String s) {
        if (isEmpty(s)) {
            return defaultEmptyOK;
        }
        return isNonnegativeInteger(s) && ((s.length() == 2) || (s.length() == 4));
    }

    /** isIntegerInRange returns true if string s is an integer
     *  within the range of integer arguments a and b, inclusive.
     */
    public static boolean isIntegerInRange(String s, int a, int b) {
        if (isEmpty(s)) {
            return defaultEmptyOK;
        }
        // Catch non-integer strings to avoid creating a NaN below,
        // which isn't available on JavaScript 1.0 for Windows.
        if (!isSignedInteger(s)) {
            return false;
        }
        // Now, explicitly change the type to integer via parseInt
        // so that the comparison code below will work both on
        // JavaScript 1.2(which typechecks in equality comparisons)
        // and JavaScript 1.1 and before(which doesn't).
        int num = Integer.parseInt(s);

        return ((num >= a) && (num <= b));
    }

    /** isMonth returns true if string s is a valid month number between 1 and 12. */
    public static boolean isMonth(String s) {
        if (isEmpty(s)) {
            return defaultEmptyOK;
        }
        return isIntegerInRange(s, 1, 12);
    }

    /** isDay returns true if string s is a valid day number between 1 and 31. */
    public static boolean isDay(String s) {
        if (isEmpty(s)) {
            return defaultEmptyOK;
        }
        return isIntegerInRange(s, 1, 31);
    }

    /** Given integer argument year, returns number of days in February of that year. */
    public static int daysInFebruary(int year) {
        // February has 29 days in any year evenly divisible by four,
        // EXCEPT for centurial years which are not also divisible by 400.
        return (((year % 4 == 0) && ((!(year % 100 == 0)) || (year % 400 == 0))) ? 29 : 28);
    }

    /** isHour returns true if string s is a valid number between 0 and 23. */
    public static boolean isHour(String s) {
        if (isEmpty(s)) {
            return defaultEmptyOK;
        }
        return isIntegerInRange(s, 0, 23);
    }

    /** isMinute returns true if string s is a valid number between 0 and 59. */
    public static boolean isMinute(String s) {
        if (isEmpty(s)) {
            return defaultEmptyOK;
        }
        return isIntegerInRange(s, 0, 59);
    }

    /** isSecond returns true if string s is a valid number between 0 and 59. */
    public static boolean isSecond(String s) {
        if (isEmpty(s)) {
            return defaultEmptyOK;
        }
        return isIntegerInRange(s, 0, 59);
    }

    /** isDate returns true if string arguments year, month, and day form a valid date. */
    public static boolean isDate(String year, String month, String day) {
        // catch invalid years(not 2- or 4-digit) and invalid months and days.
        if (!(isYear(year) && isMonth(month) && isDay(day))) {
            return false;
        }

        int intYear = Integer.parseInt(year);
        int intMonth = Integer.parseInt(month);
        int intDay = Integer.parseInt(day);

        // catch invalid days, except for February, so intDay > daysInMonth[intMonth - 1]
        return !(intDay > daysInMonth[intMonth - 1]) && !((intMonth == 2) && (intDay > daysInFebruary(intYear)));
    }

    /** isDate returns true if string argument date forms a valid date. */
    public static boolean isDate(String date) {
        if (isEmpty(date)) {
            return defaultEmptyOK;
        }
        String month;
        String day;
        String year;

        int dateSlash1 = date.indexOf("/");
        int dateSlash2 = date.lastIndexOf("/");

        if (dateSlash1 <= 0 || dateSlash1 == dateSlash2) {
            return false;
        }
        month = date.substring(0, dateSlash1);
        day = date.substring(dateSlash1 + 1, dateSlash2);
        year = date.substring(dateSlash2 + 1);

        return isDate(year, month, day);
    }

    /** isDate returns true if string argument date forms a valid date and is after today. */
    public static boolean isDateAfterToday(String date) {
        if (isEmpty(date)) {
            return defaultEmptyOK;
        }
        int dateSlash1 = date.indexOf("/");
        int dateSlash2 = date.lastIndexOf("/");

        if (dateSlash1 <= 0) {
            return false;
        }

        java.util.Date passed = null;
        if (dateSlash1 == dateSlash2) {
            // consider the day to be optional; use the first day of the following month for comparison since this is an is after test
            String month = date.substring(0, dateSlash1);
            String day = "28";
            String year = date.substring(dateSlash1 + 1);
            if (!isDate(year, month, day)) {
                return false;
            }

            try {
                int monthInt = Integer.parseInt(month);
                int yearInt = Integer.parseInt(year);
                Calendar calendar = Calendar.getInstance();
                calendar.set(yearInt, monthInt - 1, 0, 0, 0, 0);
                calendar.add(Calendar.MONTH, 1);
                passed = new java.util.Date(calendar.getTime().getTime());
            } catch (NumberFormatException e) {
                passed = null;
            }
        } else {
            String month = date.substring(0, dateSlash1);
            String day = date.substring(dateSlash1 + 1, dateSlash2);
            String year = date.substring(dateSlash2 + 1);
            if (!isDate(year, month, day)) {
                return false;
            }
            passed = UtilDateTime.toDate(month, day, year, "0", "0", "0");
        }

        java.util.Date now = UtilDateTime.nowDate();
        if (passed != null) {
            return passed.after(now);
        }
        return false;
    }

    /** isDate returns true if string argument date forms a valid date and is before today. */
    public static boolean isDateBeforeToday(String date) {
        if (isEmpty(date)) {
            return defaultEmptyOK;
        }
        int dateSlash1 = date.indexOf("/");
        int dateSlash2 = date.lastIndexOf("/");

        if (dateSlash1 <= 0)
         {
            return defaultEmptyOK; // In this case an issue number has been provided (requires a javascript check in template!)
        }

        java.util.Date passed = null;
        if (dateSlash1 == dateSlash2) {
            // consider the day to be optional; use the first day of the following month for comparison since this is an is after test
            String month = date.substring(0, dateSlash1);
            String day = "28";
            String year = date.substring(dateSlash1 + 1);
            if (!isDate(year, month, day)) {
                return false;
            }

            try {
                int monthInt = Integer.parseInt(month);
                int yearInt = Integer.parseInt(year);
                Calendar calendar = Calendar.getInstance();
                calendar.set(yearInt, monthInt - 1, 0, 0, 0, 0);
                calendar.add(Calendar.MONTH, 1);
                passed = new java.util.Date(calendar.getTime().getTime());
            } catch (NumberFormatException e) {
                passed = null;
            }
        } else {
            String month = date.substring(0, dateSlash1);
            String day = date.substring(dateSlash1 + 1, dateSlash2);
            String year = date.substring(dateSlash2 + 1);
            if (!isDate(year, month, day)) {
                return false;
            }
            passed = UtilDateTime.toDate(month, day, year, "0", "0", "0");
        }

        java.util.Date now = UtilDateTime.nowDate();
        if (passed != null) {
            return passed.before(now);
        }
        return false;
    }

    public static boolean isDateBeforeNow(Timestamp  date) {
        Timestamp now = UtilDateTime.nowTimestamp();
        if (date != null) {
            return date.before(now);
        }
        return false;
    }

    public static boolean isDateAfterNow(Timestamp  date) {
        Timestamp now = UtilDateTime.nowTimestamp();
        if (date != null) {
            return date.after(now);
        }
        return false;
    }
    /** isTime returns true if string arguments hour, minute, and second form a valid time. */
    public static boolean isTime(String hour, String minute, String second) {
        // catch invalid years(not 2- or 4-digit) and invalid months and days.
        return isHour(hour) && isMinute(minute) && isSecond(second);
    }

    /** isTime returns true if string argument time forms a valid time. */
    public static boolean isTime(String time) {
        if (isEmpty(time)) {
            return defaultEmptyOK;
        }

        String hour;
        String minute;
        String second;

        int timeColon1 = time.indexOf(":");
        int timeColon2 = time.lastIndexOf(":");

        if (timeColon1 <= 0) {
            return false;
        }
        hour = time.substring(0, timeColon1);
        if (timeColon1 == timeColon2) {
            minute = time.substring(timeColon1 + 1);
            second = "0";
        } else {
            minute = time.substring(timeColon1 + 1, timeColon2);
            second = time.substring(timeColon2 + 1);
        }
        return isTime(hour, minute, second);
    }

    /** Check to see if a card number is a valid ValueLink Gift Card
     *
     * @param stPassed a string representing a valuelink gift card
     * @return true, if the number passed simple checks
     */
    public static boolean isValueLinkCard(String stPassed) {
        if (isEmpty(stPassed)) {
            return defaultEmptyOK;
        }
        String st = stripCharsInBag(stPassed, creditCardDelimiters);
        if (st.length() == 16 && (st.startsWith("7") || st.startsWith("6"))) {
            return true;
        }
        return false;
    }

    /** Check to see if a card number is a valid OFB Gift Card (Certifiicate)
     *
     * @param stPassed a string representing a gift card
     * @return tru, if the number passed simple checks
     */
    public static boolean isOFBGiftCard(String stPassed) {
        if (isEmpty(stPassed)) {
            return defaultEmptyOK;
        }
        String st = stripCharsInBag(stPassed, creditCardDelimiters);
        return st.length() == 15 && sumIsMod10(getLuhnSum(st));
    }

    /** Check to see if a card number is a supported Gift Card
     *
     * @param stPassed a string representing a gift card
     * @return true, if the number passed simple checks
     */
    public static boolean isGiftCard(String stPassed) {
        return isOFBGiftCard(stPassed) || isValueLinkCard(stPassed);
    }

    public static int getLuhnSum(String stPassed) {
        stPassed = stPassed.replaceAll("\\D", ""); // nuke any non-digit characters

        int len = stPassed.length();
        int sum = 0;
        int mul = 1;
        for (int i = len - 1; i >= 0; i--) {
            int digit = Character.digit(stPassed.charAt(i), 10);
            digit *= (mul == 1) ? mul++ : mul--;
            sum += (digit >= 10) ? (digit % 10) + 1 : digit;
        }

        return sum;
    }

    public static int getLuhnCheckDigit(String stPassed) {
        int sum = getLuhnSum(stPassed);
        int mod = ((sum / 10 + 1) * 10 - sum) % 10;
        return (10 - mod);
    }

    public static boolean sumIsMod10(int sum) {
        return ((sum % 10) == 0);
    }

    public static String appendCheckDigit(String stPassed) {
        String checkDigit = Integer.toString(getLuhnCheckDigit(stPassed));
        return stPassed + checkDigit;
    }

    /** Checks credit card number with Luhn Mod-10 test
     *
     * @param stPassed a string representing a credit card number
     * @return true, if the credit card number passes the Luhn Mod-10 test, false otherwise
     */
    public static boolean isCreditCard(String stPassed) {
        if (isEmpty(stPassed)) {
            return defaultEmptyOK;
        }
        String st = stripCharsInBag(stPassed, creditCardDelimiters);

        // encoding only works on cars with less the 19 digits, so st.length() > 19
        return isInteger(st) && !(st.length() > 19) && sumIsMod10(getLuhnSum(st));
    }

    /** Checks to see if the cc number is a valid Visa number
     *
     * @param cc a string representing a credit card number; Sample number: 4111 1111 1111 1111(16 digits)
     * @return true, if the credit card number is a valid VISA number, false otherwise
     */
    public static boolean isVisa(String cc) {
        if (((cc.length() == 16) || (cc.length() == 13)) && ("4".equals(cc.substring(0, 1)))) {
            return isCreditCard(cc);
        }
        return false;
    }

    /** Checks to see if the cc number is a valid Master Card number
     *
     * @param cc a string representing a credit card number; MasterCard numbers either start with the numbers 51 through 55 or with the numbers 2221 through 2720. All have 16 digits; Sample number: 5500 0000 0000 0004(16 digits)
     * @return true, if the credit card number is a valid MasterCard  number, false otherwise
     */
    public static boolean isMasterCard(String cc) {
        int first2digs = Integer.parseInt(cc.substring(0, 2));
        int first4digs = Integer.parseInt(cc.substring(0, 4));

        if ((Integer.compare(cc.length(), 16) == 0) && ((first2digs >= 51 && first2digs <= 55) || (first4digs >= 2221 && first4digs <= 2720))) {
            return isCreditCard(cc);
        }
        return false;

    }

    /** Checks to see if the cc number is a valid American Express number
     *   @param    cc - a string representing a credit card number; Sample number: 340000000000009(15 digits)
     *   @return  true, if the credit card number is a valid American Express number, false otherwise
     */
    public static boolean isAmericanExpress(String cc) {
        int firstdig = Integer.parseInt(cc.substring(0, 1));
        int seconddig = Integer.parseInt(cc.substring(1, 2));

        if ((cc.length() == 15) && (firstdig == 3) && ((seconddig == 4) || (seconddig == 7))) {
            return isCreditCard(cc);
        }
        return false;

    }

    /** Checks to see if the cc number is a valid Diners Club number
     *   @param    cc - a string representing a credit card number; Sample number: 30000000000004(14 digits)
     *   @return  true, if the credit card number is a valid Diner's Club number, false otherwise
     */
    public static boolean isDinersClub(String cc) {
        int firstdig = Integer.parseInt(cc.substring(0, 1));
        int seconddig = Integer.parseInt(cc.substring(1, 2));

        if ((cc.length() == 14) && (firstdig == 3) && ((seconddig == 0) || (seconddig == 6) || (seconddig == 8))) {
            return isCreditCard(cc);
        }
        return false;
    }

    /** Checks to see if the cc number is a valid Carte Blanche number
     *   @param    cc - a string representing a credit card number; Sample number: 30000000000004(14 digits)
     *   @return  true, if the credit card number is a valid Carte Blanche number, false otherwise
     */
    public static boolean isCarteBlanche(String cc) {
        return isDinersClub(cc);
    }

    /** Checks to see if the cc number is a valid Discover number
     *   @param    cc - a string representing a credit card number; Discover card numbers begin with 6011 or 65. All have 16 digits; Sample number: 6011000000000004(16 digits)
     *   @return  true, if the credit card number is a valid Discover card number, false otherwise
     */
    public static boolean isDiscover(String cc) {
        String first4digs = cc.substring(0, 4);
        String first2digs = cc.substring(0, 2);

        if ((Integer.compare(cc.length(), 16) == 0) && ("6011".equals(first4digs) || "65".equals(first2digs))) {
            return isCreditCard(cc);
        }
        return false;
    }

    /** Checks to see if the cc number is a valid EnRoute number
     *   @param    cc - a string representing a credit card number; Sample number: 201400000000009(15 digits)
     *   @return  true, if the credit card number is a valid enRoute card number, false, otherwise
     */
    public static boolean isEnRoute(String cc) {
        String first4digs = cc.substring(0, 4);

        if ((cc.length() == 15) && ("2014".equals(first4digs) || "2149".equals(first4digs))) {
            return isCreditCard(cc);
        }
        return false;
    }

    /** Checks to see if the cc number is a valid JCB number
     *   @param     cc - a string representing a credit card number; JCB cards beginning with 2131 or 1800 have 15 digits. JCB cards beginning with 35 have 16 digits;Sample number: 3088000000000009(16 digits)
     *   @return  true, if the credit card number is a valid JCB card number, false otherwise
     */
    public static boolean isJCB(String cc) {
        String first4digs = cc.substring(0, 4);
        String first2digs = cc.substring(0, 2);

        if(((Integer.compare(cc.length(), 16) == 0) && "35".equals(first2digs)) || ((Integer.compare(cc.length(), 15) == 0) && ("2131".equals(first4digs) || "1800".equals(first4digs)))) {
            return isCreditCard(cc);
        }
        return false;
    }

    /** Checks to see if the cc number is a valid Switch number
     *   @param     cc - a string representing a credit card number; Sample number: 6331100000000096(16 digits)
     *   @return  true, if the credit card number is a valid Switch card number, false otherwise
     */
    public static boolean isSwitch(String cc) {
        String first4digs = cc.substring(0, 4);
        String first6digs = cc.substring(0, 6);

        if (((cc.length() == 16) || (cc.length() == 18) || (cc.length() == 19)) &&
            ("4903".equals(first4digs) ||
                "4905".equals(first4digs) ||
                "4911".equals(first4digs) ||
                "4936".equals(first4digs) ||
                "564182".equals(first6digs) ||
                "633110".equals(first6digs) ||
                "6333".equals(first4digs) ||
                "6759".equals(first4digs))) {
            return isCreditCard(cc);
        }
        return false;
    }

    /** Checks to see if the cc number is a valid Solo number
     *   @param     cc - a string representing a credit card number; Sample number: 6331100000000096 (16 digits)
     *   @return  true, if the credit card number is a valid Solo card number, false otherwise
     */
    public static boolean isSolo(String cc) {
        String first4digs = cc.substring(0, 4);
        String first2digs = cc.substring(0, 2);
        if (((cc.length() == 16) || (cc.length() == 18) || (cc.length() == 19)) &&
                ("63".equals(first2digs) || "6767".equals(first4digs))) {
            return isCreditCard(cc);
        }
        return false;
    }

    /** Checks to see if the cc number is a valid Visa Electron number
     *   @param    cc - a string representing a credit card number; Sample number: 4175000000000001(16 digits)
     *   @return  true, if the credit card number is a valid Visa Electron card number, false otherwise
     */
    public static boolean isVisaElectron(String cc) {
        String first6digs = cc.substring(0, 6);
        String first4digs = cc.substring(0, 4);

        if ((cc.length() == 16) &&
            ("417500".equals(first6digs) ||
                "4917".equals(first4digs) ||
                "4913".equals(first4digs) ||
                "4508".equals(first4digs) ||
                "4844".equals(first4digs) ||
                "4027".equals(first4digs))) {
            return isCreditCard(cc);
        }
        return false;
    }


    /** Checks to see if the cc number is a valid number for any accepted credit card
     *   @param     ccPassed - a string representing a credit card number
     *   @return  true, if the credit card number is any valid credit card number for any of the accepted card types, false otherwise
     */
    public static boolean isAnyCard(String ccPassed) {
        if (isEmpty(ccPassed)) {
            return defaultEmptyOK;
        }

        String cc = stripCharsInBag(ccPassed, creditCardDelimiters);
        return isCreditCard(cc) && (isMasterCard(cc) || isVisa(cc) || isAmericanExpress(cc) || isDinersClub(cc) ||
                        isDiscover(cc) || isEnRoute(cc) || isJCB(cc) || isSolo(cc)|| isSwitch (cc)|| isVisaElectron(cc));
    }

    /** Checks to see if the cc number is a valid number for any accepted credit card, and return the name of that type
     *   @param     ccPassed - a string representing a credit card number
     *   @return  true, if the credit card number is any valid credit card number for any of the accepted card types, false otherwise
     */
    public static String getCardType(String ccPassed) {
        if (isEmpty(ccPassed)) {
            return "Unknown";
        }
        String cc = stripCharsInBag(ccPassed, creditCardDelimiters);

        if (!isCreditCard(cc)) {
            return "Unknown";
        }

        if (isMasterCard(cc)) {
            return "CCT_MASTERCARD";
        }
        if (isVisa(cc)) {
            return "CCT_VISA";
        }
        if (isAmericanExpress(cc)) {
            return "CCT_AMERICANEXPRESS";
        }
        if (isDinersClub(cc)) {
            return "CCT_DINERSCLUB";
        }
        if (isDiscover(cc)) {
            return "CCT_DISCOVER";
        }
        if (isEnRoute(cc)) {
            return "CCT_ENROUTE";
        }
        if (isJCB(cc)) {
            return "CCT_JCB";
        }
        if (isSolo(cc)) {
            return "CCT_SOLO";
        }
        if (isSwitch (cc)) {
            return "CCT_SWITCH";
        }
        if (isVisaElectron(cc)) {
            return "CCT_VISAELECTRON";
        }
        return "Unknown";
    }

    /** Checks to see if the cc number is a valid number for the specified type
     *   @param    cardType - a string representing the credit card type
     *   @param    cardNumberPassed - a string representing a credit card number
     *   @return  true, if the credit card number is valid for the particular credit card type given in "cardType", false otherwise
     */
    public static boolean isCardMatch(String cardType, String cardNumberPassed) {
        if (isEmpty(cardType) || isEmpty(cardNumberPassed)) {
            return defaultEmptyOK;
        }
        String cardNumber = stripCharsInBag(cardNumberPassed, creditCardDelimiters);

        if (("CCT_VISA".equalsIgnoreCase(cardType)) && (isVisa(cardNumber))) {
            return true;
        }
        if (("CCT_MASTERCARD".equalsIgnoreCase(cardType)) && (isMasterCard(cardNumber))) {
            return true;
        }
        if ((("CCT_AMERICANEXPRESS".equalsIgnoreCase(cardType)) || ("CCT_AMEX".equalsIgnoreCase(cardType))) && (isAmericanExpress(cardNumber))) {
            return true;
        }
        if (("CCT_DISCOVER".equalsIgnoreCase(cardType)) && (isDiscover(cardNumber))) {
            return true;
        }
        if (("CCT_JCB".equalsIgnoreCase(cardType)) && (isJCB(cardNumber))) {
            return true;
        }
        if ((("CCT_DINERSCLUB".equalsIgnoreCase(cardType)) || ("CCT_DINERS".equalsIgnoreCase(cardType))) && (isDinersClub(cardNumber))) {
            return true;
        }
        if (("CCT_CARTEBLANCHE".equalsIgnoreCase(cardType)) && (isCarteBlanche(cardNumber))) {
            return true;
        }
        if (("CCT_ENROUTE".equalsIgnoreCase(cardType)) && (isEnRoute(cardNumber))) {
            return true;
        }
        if (("CCT_SOLO".equalsIgnoreCase(cardType)) && (isSolo(cardNumber))) {
            return true;
        }
        if (("CCT_SWITCH".equalsIgnoreCase(cardType)) && (isSwitch (cardNumber))) {
            return true;
        }
        if (("CCT_VISAELECTRON".equalsIgnoreCase(cardType)) && (isVisaElectron(cardNumber))) {
            return true;
        }
        return false;
    }


    /** isNotPoBox returns true if address argument does not contain anything that looks like a a PO Box. */
    public static boolean isNotPoBox(String s) {
        if (isEmpty(s)) {
            return defaultEmptyOK;
        }

        // strings to check from Greg's program
        // "P.O. B"
        // "P.o.B"
        // "P.O B"
        // "PO. B"
        // "P O B"
        // "PO B"
        // "P.0. B"
        // "P0 B"

        String sl = s.toLowerCase(Locale.getDefault());
        if (sl.indexOf("p.o. b") != -1) {
            return false;
        }
        if (sl.indexOf("p.o.b") != -1) {
            return false;
        }
        if (sl.indexOf("p.o b") != -1) {
            return false;
        }
        if (sl.indexOf("p o b") != -1) {
            return false;
        }
        if (sl.indexOf("po b") != -1) {
            return false;
        }
        if (sl.indexOf("pobox") != -1) {
            return false;
        }
        if (sl.indexOf("po#") != -1) {
            return false;
        }
        if (sl.indexOf("po #") != -1) {
            return false;
        }

        // now with 0's for them sneaky folks
        if (sl.indexOf("p.0. b") != -1) {
            return false;
        }
        if (sl.indexOf("p.0.b") != -1) {
            return false;
        }
        if (sl.indexOf("p.0 b") != -1) {
            return false;
        }
        if (sl.indexOf("p 0 b") != -1) {
            return false;
        }
        if (sl.indexOf("p0 b") != -1) {
            return false;
        }
        if (sl.indexOf("p0box") != -1) {
            return false;
        }
        if (sl.indexOf("p0#") != -1) {
            return false;
        }
        if (sl.indexOf("p0 #") != -1) {
            return false;
        }
        return true;
    }

    public static boolean isValidUpc(String upc) {
        if (upc == null || upc.length() != 12) {
            throw new IllegalArgumentException("Invalid UPC length; must be 12 characters");
        }

        char csum = upc.charAt(11);
        char calcSum = calcUpcChecksum(upc);
        return csum == calcSum;
    }

    public static char calcUpcChecksum(String upc) {
        return calcChecksum(upc, 12);
    }

    public static boolean isValidEan(String ean) {
        if (ean == null || ean.length() != 13) {
            throw new IllegalArgumentException("Invalid EAN length; must be 13 characters");
        }
        char csum = ean.charAt(12);
        char calcSum = calcChecksum(ean, 12);
        return csum == calcSum;
    }

    public static char calcChecksum(String value, int length) {
        if (value != null && value.length() == length + 1) {
            value = value.substring(0, length);
        }
        if (value == null || value.length() != length) {
            throw new IllegalArgumentException("Illegal size of value; must be either" + length + " or " + (length + 1) + " characters");
        }
        int oddsum = 0;
        int evensum = 0;
        for (int i = value.length() - 1; i >= 0; i--) {
            if ((value.length() - i) % 2 == 0) {
                evensum += Character.digit(value.charAt(i), 10);
            } else {
                oddsum += Character.digit(value.charAt(i), 10);
            }
        }
        int check = 10 - ((evensum + 3 * oddsum) % 10);
        if (check >= 10) {
            check = 0;
        }
        return Character.forDigit(check, 10);
    }

    public static String checkValidDatabaseId(String fieldStr) {
        if (fieldStr.indexOf(' ') >= 0) {
            return "[space found at position " + (fieldStr.indexOf(' ') + 1) + "]";
        }
        if (fieldStr.indexOf('"') >= 0) {
            return "[double-quote found at position " + (fieldStr.indexOf('"') + 1) + "]";
        }
        if (fieldStr.indexOf('\'') >= 0) {
            return "[single-quote found at position " + (fieldStr.indexOf('\'') + 1) + "]";
        }
        if (fieldStr.indexOf('&') >= 0) {
            return "[ampersand found at position " + (fieldStr.indexOf('&') + 1) + "]";
        }
        if (fieldStr.indexOf('?') >= 0) {
            return "[question mark found at position " + (fieldStr.indexOf('?') + 1) + "]";
        }
        if (fieldStr.indexOf('<') >= 0) {
            return "[less-than sign found at position " + (fieldStr.indexOf('<') + 1) + "]";
        }
        if (fieldStr.indexOf('>') >= 0) {
            return "[greater-than sign found at position " + (fieldStr.indexOf('>') + 1) + "]";
        }
        if (fieldStr.indexOf('\\') >= 0) {
            return "[back-slash found at position " + (fieldStr.indexOf('\\') + 1) + "]";
        }
        if (fieldStr.indexOf('/') >= 0) {
            return "[forward-slash found at position " + (fieldStr.indexOf('/') + 1) + "]";
        }
        return null;
    }

    public static boolean isValidDatabaseId(String fieldStr, StringBuffer errorDetails) {
        boolean isValid = true;
        String checkMessage = checkValidDatabaseId(fieldStr);
        if (checkMessage != null) {
            isValid = false;
            errorDetails.append(checkMessage);
        }
        return isValid;
    }

    public static boolean isValidDatabaseId(String fieldStr, StringBuilder errorDetails) {
        boolean isValid = true;
        String checkMessage = checkValidDatabaseId(fieldStr);
        if (checkMessage != null) {
            isValid = false;
            errorDetails.append(checkMessage);
        }
        return isValid;
    }

    public static boolean isValidPhoneNumber(String phoneNumber, Delegator delegator) {
        String geoId = EntityUtilProperties.getPropertyValue("general", "country.geo.id.default", delegator);
        return isValidPhoneNumber(phoneNumber, geoId, delegator);
    }

    public static boolean isValidPhoneNumber(String phoneNumber, String geoId, Delegator delegator) {
        boolean isValid = false;
        try {
            GenericValue geo = EntityQuery.use(delegator).from("Geo").where("geoId", geoId).cache().queryOne();
            PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
            String geoCode = geo != null ? geo.getString("geoCode") : "US";
            PhoneNumber phNumber = phoneUtil.parse(phoneNumber, geoCode);
            if (phoneUtil.isValidNumber(phNumber) || phoneUtil.isPossibleNumber(phNumber)) {
                isValid = true;
            }
        } catch (GenericEntityException | NumberParseException ex) {
            Debug.logError(ex, module);
        }
        return isValid;
    }
}