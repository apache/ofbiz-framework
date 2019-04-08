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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;

import com.ibm.icu.text.RuleBasedNumberFormat;

public final class UtilNumber {

    public static String module = UtilNumber.class.getName();

    // properties file name for arithmetic configuration
    private static final String arithmeticPropertiesFile = "arithmetic.properties";

    // default scale and rounding mode for BigDecimals
    private static final int DEFAULT_BD_SCALE = 2;
    private static final int DEFAULT_BD_ROUNDING_MODE = BigDecimal.ROUND_HALF_UP;

    // ICU4J rule sets for the en_US locale. To add more rules, expand this string.
    // For reference, see the RbnfSampleRuleSets.java file distributed with ICU4J
    private static final String ruleSet_en_US =
        /*
         * These rules format a number in one of the two styles often used
         * on checks.  %dollars-and-hundredths formats cents as hundredths of
         * a dollar (23.40 comes out as "twenty-three and 40/100 dollars").
         * %dollars-and-cents formats in dollars and cents (23.40 comes out as
         * "twenty-three dollars and forty cents")
         */
        "%dollars-and-cents:\n"
        + "    x.0: << [and >%%cents>];\n"
        + "    0.x: >%%cents>;\n"
        + "    0: zero dollars; one dollar; =%%main= dollars;\n"
        + "%%main:\n"
        + "    zero; one; two; three; four; five; six; seven; eight; nine;\n"
        + "    ten; eleven; twelve; thirteen; fourteen; fifteen; sixteen;\n"
        + "        seventeen; eighteen; nineteen;\n"
        + "    20: twenty[->>];\n"
        + "    30: thirty[->>];\n"
        + "    40: forty[->>];\n"
        + "    50: fifty[->>];\n"
        + "    60: sixty[->>];\n"
        + "    70: seventy[->>];\n"
        + "    80: eighty[->>];\n"
        + "    90: ninety[->>];\n"
        + "    100: << hundred[ >>];\n"
        + "    1000: << thousand[ >>];\n"
        + "    1,000,000: << million[ >>];\n"
        + "    1,000,000,000: << billion[ >>];\n"
        + "    1,000,000,000,000: << trillion[ >>];\n"
        + "    1,000,000,000,000,000: =#,##0=;\n"
        + "%%cents:\n"
        + "    100: <%%main< cent[s];\n"
        + "%dollars-and-hundredths:\n"
        + "    x.0: <%%main< and >%%hundredths>/100;\n" // this used to end in 'dollars' but that should be added later
        + "%%hundredths:\n"
        + "    100: <00<;\n";

    // ICU4J rule sets for the th_TH locale. To add more rules, expand this string.
    // For reference, see the RbnfSampleRuleSets.java file distributed with ICU4J
    private static final String ruleSet_th_TH =
        /*
         * These rules format a number in one of the two styles often used
         * on checks.  %bahts-and-hundredths formats stangs as hundredths of
         * a baht (23.40 comes out as "twenty-three and 40/100 bahts").
         * %bahts-and-stangs formats in bahts and stangs (23.40 comes out as
         * "twenty-three bahts and forty stangs")
         */
        "%bahts-and-stangs:\n"
        + "    x.0: << [and >%%stangs>];\n"
        + "    0.x: >%%stangs>;\n"
        + "    0: zero bahts; one baht; =%%main= bahts;\n"
        + "%%main:\n"
        + "    zero; one; two; three; four; five; six; seven; eight; nine;\n"
        + "    ten; eleven; twelve; thirteen; fourteen; fifteen; sixteen;\n"
        + "        seventeen; eighteen; nineteen;\n"
        + "    20: twenty[->>];\n"
        + "    30: thirty[->>];\n"
        + "    40: forty[->>];\n"
        + "    50: fifty[->>];\n"
        + "    60: sixty[->>];\n"
        + "    70: seventy[->>];\n"
        + "    80: eighty[->>];\n"
        + "    90: ninety[->>];\n"
        + "    100: << hundred[ >>];\n"
        + "    1000: << thousand[ >>];\n"
        + "    1,000,000: << million[ >>];\n"
        + "    1,000,000,000: << billion[ >>];\n"
        + "    1,000,000,000,000: << trillion[ >>];\n"
        + "    1,000,000,000,000,000: =#,##0=;\n"
        + "%%stangs:\n"
        + "    100: <%%main< stang[s];\n"
        + "%bahts-and-hundredths:\n"
        + "    x.0: <%%main< and >%%hundredths>/100;\n" // this used to end in 'bahts' but that should be added later
        + "%%hundredths:\n"
        + "    100: <00<;\n";

    // hash map to store ICU4J rule sets keyed to Locale
    private static HashMap<Locale, String> rbnfRuleSets;
    static {
        rbnfRuleSets = new HashMap<Locale, String>();
        rbnfRuleSets.put(Locale.US, ruleSet_en_US);
        rbnfRuleSets.put(new Locale("th"), ruleSet_th_TH);
    }

    private UtilNumber() {}

    /**
     * Method to get BigDecimal scale factor from a property
     * @param   file     - Name of the property file
     * @param   property - Name of the config property from arithmeticPropertiesFile (e.g., "invoice.decimals")
     * @return  int - Scale factor to pass to BigDecimal's methods. Defaults to DEFAULT_BD_SCALE (2)
     */
    public static int getBigDecimalScale(String file, String property) {
        if (UtilValidate.isEmpty(file) || UtilValidate.isEmpty(property)) {
            return DEFAULT_BD_SCALE;
        }
        
        int scale = -1;
        String value = UtilProperties.getPropertyValue(file, property);
        if (value != null) {
            try {
                scale = Integer.parseInt(value);
            } catch (NumberFormatException e) {
            }
        }
        if (scale == -1) {
            Debug.logWarning("Could not set decimal precision from " + property + "=" + value + ". Using default scale of " + DEFAULT_BD_SCALE + ".", module);
            scale = DEFAULT_BD_SCALE;
        }
        return scale;
    }

    /**
     * As above, but use the default properties file
     */
    public static int getBigDecimalScale(String property) {
        return getBigDecimalScale(arithmeticPropertiesFile, property);
    }

    /**
     * Method to get BigDecimal rounding mode from a property
     * @param   file     - Name of the property file
     * @param   property - Name of the config property from arithmeticPropertiesFile (e.g., "invoice.rounding")
     * @return  int - Rounding mode to pass to BigDecimal's methods. Defaults to DEFAULT_BD_ROUNDING_MODE (BigDecimal.ROUND_HALF_UP)
     */
    public static int getBigDecimalRoundingMode(String file, String property) {
        if (UtilValidate.isEmpty(file) || UtilValidate.isEmpty(property)) {
            return DEFAULT_BD_ROUNDING_MODE;
        }

        String value = UtilProperties.getPropertyValue(file, property);
        int mode = roundingModeFromString(value);
        if (mode == -1) {
            Debug.logWarning("Could not set decimal rounding mode from " + property + "=" + value + ". Using default mode of " + DEFAULT_BD_SCALE + ".", module);
            return DEFAULT_BD_ROUNDING_MODE;
        }
        return mode;
    }

    /**
     * As above, but use the default properties file
     */
    public static int getBigDecimalRoundingMode(String property) {
        return getBigDecimalRoundingMode(arithmeticPropertiesFile, property);
    }

    /**
     * Method to get the BigDecimal rounding mode int value from a string name.
     * @param   value - The name of the mode (e.g., "ROUND_HALF_UP")
     * @return  int - The int value of the mode (e.g, BigDecimal.ROUND_HALF_UP) or -1 if the input was bad.
     */
    public static int roundingModeFromString(String value) {
        if (value == null) return -1;
        value = value.trim();
        if ("ROUND_HALF_UP".equals(value)) return BigDecimal.ROUND_HALF_UP;
        else if ("ROUND_HALF_DOWN".equals(value)) return BigDecimal.ROUND_HALF_DOWN;
        else if ("ROUND_HALF_EVEN".equals(value)) return BigDecimal.ROUND_HALF_EVEN;
        else if ("ROUND_UP".equals(value)) return BigDecimal.ROUND_UP;
        else if ("ROUND_DOWN".equals(value)) return BigDecimal.ROUND_DOWN;
        else if ("ROUND_CEILING".equals(value)) return BigDecimal.ROUND_CEILING;
        else if ("ROUND_FLOOR".equals(value)) return BigDecimal.ROUND_FLOOR;
        else if ("ROUND_UNNECCESSARY".equals(value)) return BigDecimal.ROUND_UNNECESSARY;
        return -1;
    }

    /**
     * Method to format an amount using a custom rule set.
     * Current rule sets available:
     *
     * en_US
     * %dollars-and-cents - 1,225.25 becomes "one thousand two hundred twenty five dollars and twenty five cents" (useful for checks)
     * %dollars-and-hundreths - 1,225.25 becomes "one thousand two hundred twenty five and 25/00" (alternate for checks)
     *
     * @param   amount - the amount to format
     * @param   rule - the name of the rule set to use (e.g., %dollars-and-hundredths)
     * @param   locale - the Locale
     * @return  formatted string or an empty string if there was an error
     */
    public static String formatRuleBasedAmount(double amount, String rule, Locale locale) {
        String ruleSet = rbnfRuleSets.get(locale);
        if (ruleSet == null) {
            Debug.logWarning("Cannot format rule based amount for locale " + locale.toString() + " because rule set for that locale does not exist", module);
            return "";
        }
        RuleBasedNumberFormat formatter = new RuleBasedNumberFormat(ruleSet, locale);
        String result = "";
        try {
            result = formatter.format(amount, rule);
        } catch (Exception e) {
            Debug.logError(e, "Failed to format amount " + amount + " using rule " + rule, module);
        }
        return result;
    }

    /**
     * Method to turn a number such as "0.9853" into a nicely formatted percent, "98.53%".
     *
     * @param number    The number object to format
     * @param scale     How many places after the decimal to include
     * @param roundingMode  The BigDecimal rounding mode to apply
     * @return          The formatted string or "" if there were errors.
     */
    public static String toPercentString(Number number, int scale, int roundingMode) {
        // convert to BigDecimal
        if (!(number instanceof BigDecimal)) {
            number = new BigDecimal(number.doubleValue());
        }

        // cast it so we can use BigDecimal methods
        BigDecimal bd = (BigDecimal) number;

        // multiply by 100 and set the scale
        bd = bd.multiply(new BigDecimal(100.0)).setScale(scale, roundingMode);

        return (bd.toString() + "%");
    }

    /**
     * A null-aware method for adding BigDecimal, but only for the right operand.
     *
     * @param left      The number to add to
     * @param right     The number being added; if null, then nothing will be added
     * @return          The result of the addition, or left if right is null.
     */
    public static BigDecimal safeAdd(BigDecimal left, BigDecimal right) {
        return right != null ? left.add(right) : left;
    }
}
