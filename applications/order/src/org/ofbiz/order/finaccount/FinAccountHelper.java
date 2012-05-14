/*
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
 */

package org.ofbiz.order.finaccount;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilNumber;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;

/**
 * A package of methods for improving efficiency of financial accounts services
 *
 */
public class FinAccountHelper {

     public static final String module = FinAccountHelper.class.getName();
     /**
      * A word on precision: since we're just adding and subtracting, the interim figures should have one more decimal place of precision than the final numbers.
      */
     public static int decimals = UtilNumber.getBigDecimalScale("finaccount.decimals");
     public static int rounding = UtilNumber.getBigDecimalRoundingMode("finaccount.rounding");
     public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(decimals, rounding);

     public static final String giftCertFinAccountTypeId = "GIFTCERT_ACCOUNT";
     public static final boolean defaultPinRequired = false;

     // pool of available characters for account codes, here numbers plus uppercase characters
     static char[] char_pool = new char[10+26];
     static {
         int j = 0;
         for (int i = "0".charAt(0); i <= "9".charAt(0); i++) {
             char_pool[j++] = (char) i;
         }
         for (int i = "A".charAt(0); i <= "Z".charAt(0); i++) {
             char_pool[j++] = (char) i;
         }
     }


     /**
      * A convenience method which adds transactions.get(0).get(fieldName) to initialValue, all done in BigDecimal to decimals and rounding
      * @param initialValue the initial value 
      * @param transactions a List of GenericValue objects of transactions
      * @param fieldName the field name to get the value from the transaction
      * @param decimals number of decimals
      * @param rounding how to rounding
      * @return the new value in a BigDecimal field
      * @throws GenericEntityException
      */
     public static BigDecimal addFirstEntryAmount(BigDecimal initialValue, List<GenericValue> transactions, String fieldName, int decimals, int rounding) throws GenericEntityException {
          if ((transactions != null) && (transactions.size() == 1)) {
              GenericValue firstEntry = transactions.get(0);
              if (firstEntry.get(fieldName) != null) {
                  BigDecimal valueToAdd = firstEntry.getBigDecimal(fieldName);
                  return initialValue.add(valueToAdd).setScale(decimals, rounding);
              } else {
                  return initialValue;
              }
          } else {
              return initialValue;
          }
     }

     /**
      * Returns a unique randomly generated account code for FinAccount.finAccountCode composed of uppercase letters and numbers
      * @param codeLength length of code in number of characters
      * @param delegator the delegator
      * @return returns a unique randomly generated account code for FinAccount.finAccountCode composed of uppercase letters and numbers
      * @throws GenericEntityException
      */
     public static String getNewFinAccountCode(int codeLength, Delegator delegator) throws GenericEntityException {

         // keep generating new account codes until a unique one is found
         Random r = new Random();
         boolean foundUniqueNewCode = false;
         StringBuilder newAccountCode = null;
         long count = 0;

         while (!foundUniqueNewCode) {
             newAccountCode = new StringBuilder(codeLength);
             for (int i = 0; i < codeLength; i++) {
                 newAccountCode.append(char_pool[r.nextInt(char_pool.length)]);
             }

             List<GenericValue> existingAccountsWithCode = delegator.findByAnd("FinAccount", UtilMisc.toMap("finAccountCode", newAccountCode.toString()), null, false);
             if (existingAccountsWithCode.size() == 0) {
                 foundUniqueNewCode = true;
             }

             count++;
             if (count > 999999) {
                 throw new GenericEntityException("Unable to locate unique FinAccountCode! Length [" + codeLength + "]");
             }
         }

         return newAccountCode.toString();
     }

     /**
      * Gets the first (and should be only) FinAccount based on finAccountCode, which will be cleaned up to be only uppercase and alphanumeric
      * @param finAccountCode the financial account code
      * @param delegator the delegator
      * @return gets the first financial account by code
      * @throws GenericEntityException
      */
     public static GenericValue getFinAccountFromCode(String finAccountCode, Delegator delegator) throws GenericEntityException {
         // regex magic to turn all letters in code to uppercase and then remove all non-alphanumeric letters
         if (finAccountCode == null) {
             return null;
         }

         Pattern filterRegex = Pattern.compile("[^0-9A-Z]");
         finAccountCode = finAccountCode.toUpperCase().replaceAll(filterRegex.pattern(), "");

         // now we need to get the encrypted version of the fin account code the user passed in to look up against FinAccount
         // we do this by making a temporary generic entity with same finAccountCode and then doing a match
         GenericValue encryptedFinAccount = delegator.makeValue("FinAccount", UtilMisc.toMap("finAccountCode", finAccountCode));
         delegator.encryptFields(encryptedFinAccount);
         String encryptedFinAccountCode = encryptedFinAccount.getString("finAccountCode");

         // now look for the account
         List<GenericValue> accounts = delegator.findByAnd("FinAccount", UtilMisc.toMap("finAccountCode", encryptedFinAccountCode), null, false);
         accounts = EntityUtil.filterByDate(accounts);

         if (UtilValidate.isEmpty(accounts)) {
             // OK to display - not a code anyway
             Debug.logWarning("No fin account found for account code ["  + finAccountCode + "]", module);
             return null;
         } else if (accounts.size() > 1) {
             // This should never happen, but don't display the code if it does -- it is supposed to be encrypted!
             Debug.logError("Multiple fin accounts found", module);
             return null;
         } else {
             return accounts.get(0);
         }
     }


     /**
      * Sum of all DEPOSIT and ADJUSTMENT transactions minus all WITHDRAWAL transactions whose transactionDate is before asOfDateTime
      * @param finAccountId the financial account id
      * @param asOfDateTime the validity date
      * @param delegator the delegator
      * @return returns the sum of all DEPOSIT and ADJUSTMENT transactions minus all WITHDRAWAL transactions
      * @throws GenericEntityException
      */
     public static BigDecimal getBalance(String finAccountId, Timestamp asOfDateTime, Delegator delegator) throws GenericEntityException {
        if (asOfDateTime == null) asOfDateTime = UtilDateTime.nowTimestamp();

        BigDecimal incrementTotal = ZERO;  // total amount of transactions which increase balance
        BigDecimal decrementTotal = ZERO;  // decrease balance

        // find the sum of all transactions which increase the value
        EntityConditionList<EntityCondition> incrementConditions = EntityCondition.makeCondition(UtilMisc.toList(
                EntityCondition.makeCondition("finAccountId", EntityOperator.EQUALS, finAccountId),
                EntityCondition.makeCondition("transactionDate", EntityOperator.LESS_THAN_EQUAL_TO, asOfDateTime),
                EntityCondition.makeCondition(UtilMisc.toList(
                        EntityCondition.makeCondition("finAccountTransTypeId", EntityOperator.EQUALS, "DEPOSIT"),
                        EntityCondition.makeCondition("finAccountTransTypeId", EntityOperator.EQUALS, "ADJUSTMENT")),
                    EntityOperator.OR)),
                EntityOperator.AND);
        List<GenericValue> transSums = delegator.findList("FinAccountTransSum", incrementConditions, UtilMisc.toSet("amount"), null, null, false);
        incrementTotal = addFirstEntryAmount(incrementTotal, transSums, "amount", (decimals+1), rounding);

        // now find sum of all transactions with decrease the value
        EntityConditionList<EntityExpr> decrementConditions = EntityCondition.makeCondition(UtilMisc.toList(
                EntityCondition.makeCondition("finAccountId", EntityOperator.EQUALS, finAccountId),
                EntityCondition.makeCondition("transactionDate", EntityOperator.LESS_THAN_EQUAL_TO, asOfDateTime),
                EntityCondition.makeCondition("finAccountTransTypeId", EntityOperator.EQUALS, "WITHDRAWAL")),
            EntityOperator.AND);
        transSums = delegator.findList("FinAccountTransSum", decrementConditions, UtilMisc.toSet("amount"), null, null, false);
        decrementTotal = addFirstEntryAmount(decrementTotal, transSums, "amount", (decimals+1), rounding);

        // the net balance is just the incrementTotal minus the decrementTotal
        return incrementTotal.subtract(decrementTotal).setScale(decimals, rounding);
    }

     /**
      * Returns the net balance (see above) minus the sum of all authorization amounts which are not expired and were authorized by the as of date
      * @param finAccountId the financial account id
      * @param asOfDateTime the validity date
      * @param delegator the delegator
      * @return returns the net balance (see above) minus the sum of all authorization amounts which are not expired 
      * @throws GenericEntityException
      */
    public static BigDecimal getAvailableBalance(String finAccountId, Timestamp asOfDateTime, Delegator delegator) throws GenericEntityException {
        if (asOfDateTime == null) asOfDateTime = UtilDateTime.nowTimestamp();

        BigDecimal netBalance = getBalance(finAccountId, asOfDateTime, delegator);

        // find sum of all authorizations which are not expired and which were authorized before as of time
        EntityConditionList<EntityCondition> authorizationConditions = EntityCondition.makeCondition(UtilMisc.toList(
                EntityCondition.makeCondition("finAccountId", EntityOperator.EQUALS, finAccountId),
                EntityCondition.makeCondition("authorizationDate", EntityOperator.LESS_THAN_EQUAL_TO, asOfDateTime),
                EntityUtil.getFilterByDateExpr(asOfDateTime)),
            EntityOperator.AND);

        List<GenericValue> authSums = delegator.findList("FinAccountAuthSum", authorizationConditions, UtilMisc.toSet("amount"), null, null, false);

        BigDecimal authorizationsTotal = addFirstEntryAmount(ZERO, authSums, "amount", (decimals+1), rounding);

        // the total available balance is transactions total minus authorizations total
        return netBalance.subtract(authorizationsTotal).setScale(decimals, rounding);
    }

    public static boolean validateFinAccount(GenericValue finAccount) {
        return false;
    }

    /**
     * Validates a FinAccount's PIN number
     * @param delegator the delegator
     * @param finAccountId the financial account id
     * @param pinNumber a pin number
     * @return true if the bin is valid
     */
    public static boolean validatePin(Delegator delegator, String finAccountId, String pinNumber) {
        GenericValue finAccount = null;
        try {
            finAccount = delegator.findOne("FinAccount", UtilMisc.toMap("finAccountId", finAccountId), false);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }

        if (finAccount != null) {
            String dbPin = finAccount.getString("finAccountCode");
            Debug.logInfo("FinAccount Pin Validation: [Sent: " + pinNumber + "] [Actual: " + dbPin + "]", module);
            if (dbPin != null && dbPin.equals(pinNumber)) {
                return true;
            }
        } else {
            Debug.logInfo("FinAccount record not found (" + finAccountId + ")", module);
        }
        return false;
    }

    /**
     * Generate a random financial number
     * @param delegator the delegator
     * @param length length of the number to generate (up to 19 digits)
     * @param isId to be used as an ID (will check the DB to make sure it doesn't already exist)
     * @return Generated number
     * @throws GenericEntityException
     */
    public static String generateRandomFinNumber(Delegator delegator, int length, boolean isId) throws GenericEntityException {
        if (length > 19) {
            length = 19;
        }

        Random rand = new Random();
        boolean isValid = false;
        String number = null;
        while (!isValid) {
            number = "";
            for (int i = 0; i < length; i++) {
                int randInt = rand.nextInt(9);
                number = number + randInt;
            }

            if (isId) {
                int check = UtilValidate.getLuhnCheckDigit(number);
                number = number + check;

                // validate the number
                if (checkFinAccountNumber(number)) {
                    // make sure this number doens't already exist
                    isValid = checkIsNumberInDatabase(delegator, number);
                }
            } else {
                isValid = true;
            }
        }
        return number;
    }

    private static boolean checkIsNumberInDatabase(Delegator delegator, String number) throws GenericEntityException {
        GenericValue finAccount = delegator.findOne("FinAccount", UtilMisc.toMap("finAccountId", number), false);
        return finAccount == null;
    }

    public static boolean checkFinAccountNumber(String number) {
        number = number.replaceAll("\\D", "");
        return UtilValidate.sumIsMod10(UtilValidate.getLuhnSum(number));
    }
}
