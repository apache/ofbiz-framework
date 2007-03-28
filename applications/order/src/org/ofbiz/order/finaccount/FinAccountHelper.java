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

package org.ofbiz.order.finaccount;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

import org.ofbiz.base.util.*;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.model.ModelEntity;
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
     public static final BigDecimal ZERO = (new BigDecimal("0.0")).setScale(decimals, rounding);
     
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
      * @param initialValue
      * @param transactions
      * @param fieldName
      * @param decimals
      * @param rounding
      * @return
      * @throws GenericEntityException
      */
     public static BigDecimal addFirstEntryAmount(BigDecimal initialValue, List transactions, String fieldName, int decimals, int rounding) throws GenericEntityException {
          if ((transactions != null) && (transactions.size() == 1)) {
              GenericValue firstEntry = (GenericValue) transactions.get(0);
              if (firstEntry.get(fieldName) != null) {
                  BigDecimal valueToAdd = new BigDecimal(firstEntry.getDouble(fieldName).doubleValue());
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
      * @param delegator
      * @return
      * @throws GenericEntityException
      */
     public static String getNewFinAccountCode(int codeLength, GenericDelegator delegator) throws GenericEntityException {

         // keep generating new account codes until a unique one is found
         Random r = new Random();
         boolean foundUniqueNewCode = false;
         StringBuffer newAccountCode = null;
         long count = 0;

         while (!foundUniqueNewCode) {
             newAccountCode = new StringBuffer(codeLength);
             for (int i = 0; i < codeLength; i++) {
                 newAccountCode.append(char_pool[r.nextInt(char_pool.length)]);
             }

             List existingAccountsWithCode = delegator.findByAnd("FinAccount", UtilMisc.toMap("finAccountCode", newAccountCode.toString()));
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
      * @param finAccountCode
      * @param delegator
      * @return
      * @throws GenericEntityException
      */
     public static GenericValue getFinAccountFromCode(String finAccountCode, GenericDelegator delegator) throws GenericEntityException {
         // regex magic to turn all letters in code to uppercase and then remove all non-alphanumeric letters
         if (finAccountCode == null) {
             return null;
         }
         
         Pattern filterRegex = Pattern.compile("[^0-9A-Z]");
         finAccountCode = finAccountCode.toUpperCase().replaceAll(filterRegex.pattern(), "");
         
         // now we need to get the encrypted version of the fin account code the user passed in to look up against FinAccount
         // we do this by making a temporary generic entity with same finAccountCode and then doing a match
         ModelEntity finAccountEntity = delegator.getModelEntity("FinAccount");
         GenericEntity encryptedFinAccount = GenericEntity.createGenericEntity(finAccountEntity, UtilMisc.toMap("finAccountCode", finAccountCode));
         delegator.encryptFields(encryptedFinAccount);
         String encryptedFinAccountCode = encryptedFinAccount.getString("finAccountCode");
         
         // now look for the account
         List accounts = delegator.findByAnd("FinAccount", UtilMisc.toMap("finAccountCode", encryptedFinAccountCode));
         accounts = EntityUtil.filterByDate(accounts);
         
         if ((accounts == null) || (accounts.size() == 0)) {
             // OK to display - not a code anyway
             Debug.logWarning("No fin account found for account code ["  + finAccountCode + "]", module);
             return null;
         } else if (accounts.size() > 1) {
             // This should never happen, but don't display the code if it does -- it is supposed to be encrypted!
             Debug.logError("Multiple fin accounts found", module);
             return null;
         } else {
             return (GenericValue) accounts.get(0);
         }
     }
     
 
     /**
      * Sum of all DEPOSIT and ADJUSTMENT transactions minus all WITHDRAWAL transactions whose transactionDate is before asOfDateTime
      * @param finAccountId      
      * @param asOfDateTime
      * @param delegator
      * @return
      * @throws GenericEntityException
      */
     public static BigDecimal getBalance(String finAccountId, Timestamp asOfDateTime, GenericDelegator delegator) throws GenericEntityException {
        if (asOfDateTime == null) asOfDateTime = UtilDateTime.nowTimestamp();
         
        BigDecimal incrementTotal = ZERO;  // total amount of transactions which increase balance
        BigDecimal decrementTotal = ZERO;  // decrease balance

        GenericValue finAccount = delegator.findByPrimaryKeyCache("FinAccount", UtilMisc.toMap("finAccountId", finAccountId));
        String currencyUomId = finAccount.getString("currencyUomId");
         
        // find the sum of all transactions which increase the value
        EntityConditionList incrementConditions = new EntityConditionList(UtilMisc.toList(
                new EntityExpr("finAccountId", EntityOperator.EQUALS, finAccountId),
                new EntityExpr("transactionDate", EntityOperator.LESS_THAN_EQUAL_TO, asOfDateTime),
                new EntityConditionList(UtilMisc.toList(
                        new EntityExpr("finAccountTransTypeId", EntityOperator.EQUALS, "DEPOSIT"),
                        new EntityExpr("finAccountTransTypeId", EntityOperator.EQUALS, "ADJUSTMENT")),
                    EntityOperator.OR)),
                EntityOperator.AND);
        List transSums = delegator.findByCondition("FinAccountTransSum", incrementConditions, UtilMisc.toList("amount"), null);
        incrementTotal = addFirstEntryAmount(incrementTotal, transSums, "amount", (decimals+1), rounding);

        // now find sum of all transactions with decrease the value
        EntityConditionList decrementConditions = new EntityConditionList(UtilMisc.toList(
                new EntityExpr("finAccountId", EntityOperator.EQUALS, finAccountId),
                new EntityExpr("transactionDate", EntityOperator.LESS_THAN_EQUAL_TO, asOfDateTime),
                new EntityExpr("currencyUomId", EntityOperator.EQUALS, currencyUomId),
                new EntityExpr("finAccountTransTypeId", EntityOperator.EQUALS, "WITHDRAWAL")),
            EntityOperator.AND);
        transSums = delegator.findByCondition("FinAccountTransSum", decrementConditions, UtilMisc.toList("amount"), null);
        decrementTotal = addFirstEntryAmount(decrementTotal, transSums, "amount", (decimals+1), rounding);
        
        // the net balance is just the incrementTotal minus the decrementTotal
        return incrementTotal.subtract(decrementTotal).setScale(decimals, rounding);
    }

     /**
      * Returns the net balance (see above) minus the sum of all authorization amounts which are not expired and were authorized by the as of date
      * @param finAccountId
      * @param asOfDateTime
      * @param delegator
      * @return
      * @throws GenericEntityException
      */
    public static BigDecimal getAvailableBalance(String finAccountId, Timestamp asOfDateTime, GenericDelegator delegator) throws GenericEntityException {
        if (asOfDateTime == null) asOfDateTime = UtilDateTime.nowTimestamp();

        BigDecimal netBalance = getBalance(finAccountId, asOfDateTime, delegator);
         
        // find sum of all authorizations which are not expired and which were authorized before as of time
        EntityConditionList authorizationConditions = new EntityConditionList(UtilMisc.toList(
                new EntityExpr("finAccountId", EntityOperator.EQUALS, finAccountId),
                new EntityExpr("authorizationDate", EntityOperator.LESS_THAN_EQUAL_TO, asOfDateTime),
                EntityUtil.getFilterByDateExpr(asOfDateTime)),
            EntityOperator.AND);
         
        List authSums = delegator.findByCondition("FinAccountAuthSum", authorizationConditions, UtilMisc.toList("amount"), null);
         
        BigDecimal authorizationsTotal = addFirstEntryAmount(ZERO, authSums, "amount", (decimals+1), rounding);
         
        // the total available balance is transactions total minus authorizations total
        return netBalance.subtract(authorizationsTotal).setScale(decimals, rounding);
    }

    public static boolean validateFinAccount(GenericValue finAccount) {
        return false;    
    }

    /**
     * Validates a FinAccount's PIN number
     * @param delegator
     * @param finAccountId
     * @param pinNumber
     * @return true if the bin is valid
     */
    public static boolean validatePin(GenericDelegator delegator, String finAccountId, String pinNumber) {
        GenericValue finAccount = null;
        try {
            finAccount = delegator.findByPrimaryKey("FinAccount", UtilMisc.toMap("finAccountId", finAccountId));
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
     *
     * @param delegator
     * @param length length of the number to generate (up to 19 digits)
     * @param isId to be used as an ID (will check the DB to make sure it doesn't already exist)
     * @return String generated number
     * @throws GenericEntityException
     */
    public static String generateRandomFinNumber(GenericDelegator delegator, int length, boolean isId) throws GenericEntityException {
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

    private static boolean checkIsNumberInDatabase(GenericDelegator delegator, String number) throws GenericEntityException {
        GenericValue finAccount = delegator.findByPrimaryKey("FinAccount", UtilMisc.toMap("finAccountId", number));
        return finAccount == null;
    }

    public static boolean checkFinAccountNumber(String number) {
        number = number.replaceAll("\\D", "");
        return UtilValidate.sumIsMod10(UtilValidate.getLuhnSum(number));
    }
}
