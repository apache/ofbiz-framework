package org.apache.ofbiz.base.util;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Locale;

public class UtilFormatOutBase {

    private static final String MODULE = UtilFormatOutBase.class.getName();

    static String safeToString(Object obj) {
        if (obj != null) {
            return obj.toString();
        }
        return "";
    }

    /**
     * Encodes an XML string replacing the characters '&lt;', '&gt;', '&quot;', '&#39;', '&amp;'
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

    /** Returns the first passed String if not empty, otherwise the second if not empty, otherwise an empty but non-null String.
     * @param string1 The first passed String
     * @param string2 The second passed String
     * @return The first passed String if not empty, otherwise the second if not empty, otherwise an empty but non-null String
     */
    public static String checkEmpty(String string1, String string2) {
        if (UtilValidate.isNotEmpty(string1)) {
            return string1;
        } else if (UtilValidate.isNotEmpty(string2)) {
            return string2;
        } else {
            return "";
        }
    }

    public static String formatPaddedNumber(long number, int numericPadding) {
        StringBuilder outStrBfr = new StringBuilder(Long.toString(number));
        while (numericPadding > outStrBfr.length()) {
            outStrBfr.insert(0, '0');
        }
        return outStrBfr.toString();
    }

    /**
     * Formats a double into a properly formatted currency string based on isoCode and Locale
     *
     * @param price                 The price double to be formatted
     * @param isoCode               the currency ISO code
     * @param locale                The Locale used to format the number
     * @param maximumFractionDigits The maximum number of fraction digits used; if
     *                              set to -1 than the default value for the locale
     * @return A String with the formatted price
     */
    public static String formatCurrency(double price, String isoCode, Locale locale, int maximumFractionDigits) {
        com.ibm.icu.text.NumberFormat nf = com.ibm.icu.text.NumberFormat.getCurrencyInstance(locale);
        if (isoCode != null && isoCode.length() > 1) {
            nf.setCurrency(com.ibm.icu.util.Currency.getInstance(isoCode));
        } else {
            if (Debug.verboseOn()) {
                Debug.logVerbose("No isoCode specified to format currency value:" + price, MODULE);
            }
        }
        if (maximumFractionDigits >= 0) {
            nf.setMaximumFractionDigits(maximumFractionDigits);
        }
        return nf.format(price);
    }

    /**
     * Formats a double into a properly formatted currency string based on isoCode and Locale
     *
     * @param price                 The price BigDecimal to be formatted
     * @param isoCode               the currency ISO code
     * @param locale                The Locale used to format the number
     * @param maximumFractionDigits The maximum number of fraction digits used; if
     *                              set to -1 than the default value for the locale
     *                              is used
     * @return A String with the formatted price
     */
    public static String formatCurrency(BigDecimal price, String isoCode, Locale locale, int maximumFractionDigits) {
        return formatCurrency(price.doubleValue(), isoCode, locale, maximumFractionDigits);
    }

    /** Formats a BigDecimal into a properly formatted currency string based on isoCode and Locale
     * @param price The price BigDecimal to be formatted
     * @param isoCode the currency ISO code
     * @param locale The Locale used to format the number
     * @return A String with the formatted price
     */
    public static String formatCurrency(BigDecimal price, String isoCode, Locale locale) {
        return formatCurrency(price, isoCode, locale, -1);
    }

    /**
     * Generates a String from given values delimited by delimiter.
     * @param values
     * @param delimiter
     * @return String
     */
    public static String collectionToString(Collection<? extends Object> values, String delimiter) {
        if (UtilValidate.isEmpty(values)) {
            return null;
        }
        if (delimiter == null) {
            delimiter = "";
        }
        StringBuilder out = new StringBuilder();

        for (Object val : values) {
            out.append(safeToString(val)).append(delimiter);
        }
        return out.toString();
    }
}
