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
package org.ofbiz.service;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transaction;

import javolution.util.FastMap;
import javolution.util.FastList;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.entity.transaction.GenericTransactionException;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityFindOptions;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.security.Security;
import org.ofbiz.service.config.ServiceConfigUtil;

/**
 * Generic Service Utility Class
 */
public class ServiceUtil {

    public static final String module = ServiceUtil.class.getName();
    public static final String resource = "ServiceErrorUiLabels";

    /** A little short-cut method to check to see if a service returned an error */
    public static boolean isError(Map results) {
        if (results == null || results.get(ModelService.RESPONSE_MESSAGE) == null) {
            return false;
        }
        return ModelService.RESPOND_ERROR.equals(results.get(ModelService.RESPONSE_MESSAGE));
    }

    public static boolean isFailure(Map results) {
        if (results == null || results.get(ModelService.RESPONSE_MESSAGE) == null) {
            return false;
        }
        return ModelService.RESPOND_FAIL.equals(results.get(ModelService.RESPONSE_MESSAGE));
    }

    /** A small routine used all over to improve code efficiency, make a result map with the message and the error response code */
    public static Map returnError(String errorMessage) {
        return returnProblem(ModelService.RESPOND_ERROR, errorMessage, null, null, null);
    }

    /** A small routine used all over to improve code efficiency, make a result map with the message and the error response code */
    public static Map returnError(String errorMessage, List errorMessageList) {
        return returnProblem(ModelService.RESPOND_ERROR, errorMessage, errorMessageList, null, null);
    }

    /** A small routine used all over to improve code efficiency, make a result map with the message and the error response code */
    public static Map returnError(List errorMessageList) {
        return returnProblem(ModelService.RESPOND_ERROR, null, errorMessageList, null, null);
    }

    public static Map returnFailure(String errorMessage) {
        return returnProblem(ModelService.RESPOND_FAIL, errorMessage, null, null, null);
    }

    public static Map returnFailure(List errorMessageList) {
        return returnProblem(ModelService.RESPOND_FAIL, null, errorMessageList, null, null);
    }

    public static Map returnFailure() {
        return returnProblem(ModelService.RESPOND_FAIL, null, null, null, null);
    }

    /** A small routine used all over to improve code efficiency, make a result map with the message and the error response code, also forwards any error messages from the nestedResult */
    public static Map returnError(String errorMessage, List errorMessageList, Map errorMessageMap, Map nestedResult) {
        return returnProblem(ModelService.RESPOND_ERROR, errorMessage, errorMessageList, errorMessageMap, nestedResult);
    }

    public static Map returnProblem(String returnType, String errorMessage, List errorMessageList, Map errorMessageMap, Map nestedResult) {
        Map result = FastMap.newInstance();
        result.put(ModelService.RESPONSE_MESSAGE, returnType);
        if (errorMessage != null) {
            result.put(ModelService.ERROR_MESSAGE, errorMessage);
        }

        List errorList = new LinkedList();
        if (errorMessageList != null) {
            errorList.addAll(errorMessageList);
        }

        Map errorMap = FastMap.newInstance();
        if (errorMessageMap != null) {
            errorMap.putAll(errorMessageMap);
        }

        if (nestedResult != null) {
            if (nestedResult.get(ModelService.ERROR_MESSAGE) != null) {
                errorList.add(nestedResult.get(ModelService.ERROR_MESSAGE));
            }
            if (nestedResult.get(ModelService.ERROR_MESSAGE_LIST) != null) {
                errorList.addAll((List) nestedResult.get(ModelService.ERROR_MESSAGE_LIST));
            }
            if (nestedResult.get(ModelService.ERROR_MESSAGE_MAP) != null) {
                errorMap.putAll((Map) nestedResult.get(ModelService.ERROR_MESSAGE_MAP));
            }
        }

        if (errorList.size() > 0) {
            result.put(ModelService.ERROR_MESSAGE_LIST, errorList);
        }
        if (errorMap.size() > 0) {
            result.put(ModelService.ERROR_MESSAGE_MAP, errorMap);
        }
        return result;
    }

    /** A small routine used all over to improve code efficiency, make a result map with the message and the success response code */
    public static Map returnSuccess(String successMessage) {
        return returnMessage(ModelService.RESPOND_SUCCESS, successMessage);
    }

    /** A small routine used all over to improve code efficiency, make a result map with the message and the success response code */
    public static Map returnSuccess() {
        return returnMessage(ModelService.RESPOND_SUCCESS, null);
    }

    /** A small routine to make a result map with the message and the response code
     * NOTE: This brings out some bad points to our message convention: we should be using a single message or message list
     *  and what type of message that is should be determined by the RESPONSE_MESSAGE (and there's another annoyance, it should be RESPONSE_CODE)
     */
    public static Map returnMessage(String code, String message) {
        Map result = FastMap.newInstance();
        if (code != null) result.put(ModelService.RESPONSE_MESSAGE, code);
        if (message != null) result.put(ModelService.SUCCESS_MESSAGE, message);
        return result;
    }

    /** A small routine used all over to improve code efficiency, get the partyId and does a security check
     *<b>security check</b>: userLogin partyId must equal partyId, or must have [secEntity][secOperation] permission
     */
    public static String getPartyIdCheckSecurity(GenericValue userLogin, Security security, Map context, Map result, String secEntity, String secOperation) {
        String partyId = (String) context.get("partyId");
        Locale locale = getLocale(context);
        if (partyId == null || partyId.length() == 0) {
            partyId = userLogin.getString("partyId");
        }

        // partyId might be null, so check it
        if (partyId == null || partyId.length() == 0) {
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            String errMsg = UtilProperties.getMessage(ServiceUtil.resource, "serviceUtil.party_id_missing", locale) + ".";
            result.put(ModelService.ERROR_MESSAGE, errMsg);
            return partyId;
        }

        // <b>security check</b>: userLogin partyId must equal partyId, or must have PARTYMGR_CREATE permission
        if (!partyId.equals(userLogin.getString("partyId"))) {
            if (!security.hasEntityPermission(secEntity, secOperation, userLogin)) {
                result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
                String errMsg = UtilProperties.getMessage(ServiceUtil.resource, "serviceUtil.no_permission_to_operation", locale) + ".";
                result.put(ModelService.ERROR_MESSAGE, errMsg);
                return partyId;
            }
        }
        return partyId;
    }

    public static void setMessages(HttpServletRequest request, String errorMessage, String eventMessage, String defaultMessage) {
        if (UtilValidate.isNotEmpty(errorMessage))
            request.setAttribute("_ERROR_MESSAGE_", errorMessage);

        if (UtilValidate.isNotEmpty(eventMessage))
            request.setAttribute("_EVENT_MESSAGE_", eventMessage);

        if (UtilValidate.isEmpty(errorMessage) && UtilValidate.isEmpty(eventMessage) && UtilValidate.isNotEmpty(defaultMessage))
            request.setAttribute("_EVENT_MESSAGE_", defaultMessage);

    }

    public static void getMessages(HttpServletRequest request, Map result, String defaultMessage) {
        getMessages(request, result, defaultMessage, null, null, null, null, null, null);
    }

    public static void getMessages(HttpServletRequest request, Map result, String defaultMessage,
                                   String msgPrefix, String msgSuffix, String errorPrefix, String errorSuffix, String successPrefix, String successSuffix) {
        String errorMessage = ServiceUtil.makeErrorMessage(result, msgPrefix, msgSuffix, errorPrefix, errorSuffix);
        String successMessage = ServiceUtil.makeSuccessMessage(result, msgPrefix, msgSuffix, successPrefix, successSuffix);
        setMessages(request, errorMessage, successMessage, defaultMessage);
    }

    public static String getErrorMessage(Map result) {
        StringBuffer errorMessage = new StringBuffer();

        if (result.get(ModelService.ERROR_MESSAGE) != null) errorMessage.append((String) result.get(ModelService.ERROR_MESSAGE));

        if (result.get(ModelService.ERROR_MESSAGE_LIST) != null) {
            List errors = (List) result.get(ModelService.ERROR_MESSAGE_LIST);
            Iterator errorIter = errors.iterator();
            while (errorIter.hasNext()) {
                // NOTE: this MUST use toString and not cast to String because it may be a MessageString object
                String curMessage = errorIter.next().toString();
                if (errorMessage.length() > 0) {
                    errorMessage.append(", ");
                }
                errorMessage.append(curMessage);
            }
        }

        return errorMessage.toString();
    }

    public static String makeErrorMessage(Map result, String msgPrefix, String msgSuffix, String errorPrefix, String errorSuffix) {
        if (result == null) {
            Debug.logWarning("A null result map was passed", module);
            return null;
        }
        String errorMsg = (String) result.get(ModelService.ERROR_MESSAGE);
        List errorMsgList = (List) result.get(ModelService.ERROR_MESSAGE_LIST);
        Map errorMsgMap = (Map) result.get(ModelService.ERROR_MESSAGE_MAP);
        StringBuffer outMsg = new StringBuffer();

        if (errorMsg != null) {
            if (msgPrefix != null) outMsg.append(msgPrefix);
            outMsg.append(errorMsg);
            if (msgSuffix != null) outMsg.append(msgSuffix);
        }

        outMsg.append(makeMessageList(errorMsgList, msgPrefix, msgSuffix));

        if (errorMsgMap != null) {
            Iterator mapIter = errorMsgMap.entrySet().iterator();

            while (mapIter.hasNext()) {
                Map.Entry entry = (Map.Entry) mapIter.next();

                outMsg.append(msgPrefix);
                outMsg.append(entry.getKey());
                outMsg.append(": ");
                outMsg.append(entry.getValue());
                outMsg.append(msgSuffix);
            }
        }

        if (outMsg.length() > 0) {
            StringBuffer strBuf = new StringBuffer();

            if (errorPrefix != null) strBuf.append(errorPrefix);
            strBuf.append(outMsg.toString());
            if (errorSuffix != null) strBuf.append(errorSuffix);
            return strBuf.toString();
        } else {
            return null;
        }
    }

    public static String makeSuccessMessage(Map result, String msgPrefix, String msgSuffix, String successPrefix, String successSuffix) {
        if (result == null) {
            return "";
        }
        String successMsg = (String) result.get(ModelService.SUCCESS_MESSAGE);
        List successMsgList = (List) result.get(ModelService.SUCCESS_MESSAGE_LIST);
        StringBuffer outMsg = new StringBuffer();

        outMsg.append(makeMessageList(successMsgList, msgPrefix, msgSuffix));

        if (successMsg != null) {
            if (msgPrefix != null) outMsg.append(msgPrefix);
            outMsg.append(successMsg);
            if (msgSuffix != null) outMsg.append(msgSuffix);
        }

        if (outMsg.length() > 0) {
            StringBuffer strBuf = new StringBuffer();
            if (successPrefix != null) strBuf.append(successPrefix);
            strBuf.append(outMsg.toString());
            if (successSuffix != null) strBuf.append(successSuffix);
            return strBuf.toString();
        } else {
            return null;
        }
    }

    public static String makeMessageList(List msgList, String msgPrefix, String msgSuffix) {
        StringBuffer outMsg = new StringBuffer();
        if (msgList != null && msgList.size() > 0) {
            Iterator iter = msgList.iterator();
            while (iter.hasNext()) {
                Object msg = iter.next();
                if (msg == null) continue;
                String curMsg = msg.toString();
                if (msgPrefix != null) outMsg.append(msgPrefix);
                outMsg.append(curMsg);
                if (msgSuffix != null) outMsg.append(msgSuffix);
            }
        }
        return outMsg.toString();
    }

    /**
     * Takes the result of an invocation and extracts any error messages
     * and adds them to the targetList or targetMap. This will handle both List and String
     * error messags.
     *
     * @param targetList    The List to add the error messages to
     * @param targetMap The Map to add any Map error messages to
     * @param callResult The result from an invocation
     */
    public static void addErrors(List targetList, Map targetMap, Map callResult) {
        List newList;
        Map errorMsgMap;

        //See if there is a single message
        if (callResult.containsKey(ModelService.ERROR_MESSAGE)) {
            targetList.add(callResult.get(ModelService.ERROR_MESSAGE));
        }

        //See if there is a message list
        if (callResult.containsKey(ModelService.ERROR_MESSAGE_LIST)) {
            newList = (List) callResult.get(ModelService.ERROR_MESSAGE_LIST);
            targetList.addAll(newList);
        }

        //See if there are an error message map
        if (callResult.containsKey(ModelService.ERROR_MESSAGE_MAP)) {
            errorMsgMap = (Map) callResult.get(ModelService.ERROR_MESSAGE_MAP);
            targetMap.putAll(errorMsgMap);
        }
    }

    public static Map purgeOldJobs(DispatchContext dctx, Map context) {
        String sendPool = ServiceConfigUtil.getSendPool();
        int daysToKeep = ServiceConfigUtil.getPurgeJobDays();
        GenericDelegator delegator = dctx.getDelegator();

        Timestamp now = UtilDateTime.nowTimestamp();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(now.getTime());
        cal.add(Calendar.DAY_OF_YEAR, daysToKeep * -1);
        Timestamp purgeTime = new Timestamp(cal.getTimeInMillis());

        // create the conditions to query
        EntityCondition pool = new EntityExpr("poolId", EntityOperator.EQUALS, sendPool);

        List finExp = UtilMisc.toList(new EntityExpr("finishDateTime", EntityOperator.NOT_EQUAL, null));
        finExp.add(new EntityExpr("finishDateTime", EntityOperator.LESS_THAN, purgeTime));

        List canExp = UtilMisc.toList(new EntityExpr("cancelDateTime", EntityOperator.NOT_EQUAL, null));
        canExp.add(new EntityExpr("cancelDateTime", EntityOperator.LESS_THAN, purgeTime));

        EntityCondition cancelled = new EntityConditionList(canExp, EntityOperator.AND);
        EntityCondition finished = new EntityConditionList(finExp, EntityOperator.AND);

        EntityCondition doneCond = new EntityConditionList(UtilMisc.toList(cancelled, finished), EntityOperator.OR);
        EntityCondition mainCond = new EntityConditionList(UtilMisc.toList(doneCond, pool), EntityOperator.AND);

        // configure the find options
        EntityFindOptions findOptions = new EntityFindOptions();
        findOptions.setResultSetType(EntityFindOptions.TYPE_SCROLL_INSENSITIVE);
        findOptions.setMaxRows(1000);

        // always suspend the current transaction; use the one internally
        Transaction parent = null;
        try {
            if (TransactionUtil.getStatus() != TransactionUtil.STATUS_NO_TRANSACTION) {
                parent = TransactionUtil.suspend();
            }

            // lookup the jobs - looping 1000 at a time to avoid problems with cursors
            // also, using unique transaction to delete as many as possible even with errors
            boolean noMoreResults = false;
            boolean beganTx1 = false;
            while (!noMoreResults) {
                // current list of records
                List curList = null;
                try {
                    // begin this transaction
                    beganTx1 = TransactionUtil.begin();

                    EntityListIterator foundJobs = delegator.findListIteratorByCondition("JobSandbox", mainCond, null, null, null, findOptions);
                    curList = foundJobs.getPartialList(1, 1000);
                    foundJobs.close();

                } catch (GenericEntityException e) {
                    Debug.logError(e, "Cannot obtain job data from datasource", module);
                    try {
                        TransactionUtil.rollback(beganTx1, e.getMessage(), e);
                    } catch (GenericTransactionException e1) {
                        Debug.logWarning(e1, module);
                    }
                    return ServiceUtil.returnError(e.getMessage());
                } finally {
                    try {
                        TransactionUtil.commit(beganTx1);
                    } catch (GenericTransactionException e) {
                        Debug.logWarning(e, module);
                    }
                }

                // remove each from the list in its own transaction
                if (curList != null && curList.size() > 0) {
                    // list of runtime data IDs to attempt to delete
                    List runtimeToDelete = FastList.newInstance();
                    
                    Iterator curIter = curList.iterator();
                    while (curIter.hasNext()) {
                        GenericValue job = (GenericValue) curIter.next();
                        String jobId = job.getString("jobId");
                        boolean beganTx2 = false;
                        try {
                            beganTx2 = TransactionUtil.begin();
                            job.remove();
                        } catch (GenericEntityException e) {
                            Debug.logInfo("Cannot remove job data for ID: " + jobId, module);
                            try {
                                TransactionUtil.rollback(beganTx2, e.getMessage(), e);
                            } catch (GenericTransactionException e1) {
                                Debug.logWarning(e1, module);
                            }
                        } finally {
                            try {
                                TransactionUtil.commit(beganTx2);
                            } catch (GenericTransactionException e) {
                                Debug.logWarning(e, module);
                            }
                        }
                    }

                    // delete the runtime data - in a new transaction for each delete
                    // we do this so that the ones which cannot be deleted to not cause
                    // the entire group to rollback; some may be attached to multiple jobs.
                    if (runtimeToDelete.size() > 0) {
                        Iterator delIter = runtimeToDelete.iterator();
                        while (delIter.hasNext()) {
                            String runtimeId = (String) delIter.next();
                            boolean beganTx3 = false;
                            try {
                                beganTx3 = TransactionUtil.begin();
                                delegator.removeByAnd("RuntimeData", UtilMisc.toMap("runtimeDataId", runtimeId));

                            } catch (GenericEntityException e) {
                                Debug.logInfo("Cannot remove runtime data for ID: " + runtimeId, module);
                                try {
                                    TransactionUtil.rollback(beganTx3, e.getMessage(), e);
                                } catch (GenericTransactionException e1) {
                                    Debug.logWarning(e1, module);
                                }
                            } finally {
                                try {
                                    TransactionUtil.commit(beganTx3);
                                } catch (GenericTransactionException e) {
                                    Debug.logWarning(e, module);
                                }
                            }
                        }
                    }
                } else {
                    noMoreResults = true;
                }
            }
        } catch (GenericTransactionException e) {
            Debug.logError(e, "Unable to suspend transaction; cannot purge jobs!", module);
            return ServiceUtil.returnError(e.getMessage());
        } finally {
            if (parent != null) {
                try {
                    TransactionUtil.resume(parent);
                } catch (GenericTransactionException e) {
                    Debug.logWarning(e, module);
                }
            }
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map cancelJob(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = getLocale(context);

        if (!security.hasPermission("SERVICE_INVOKE_ANY", userLogin)) {
            String errMsg = UtilProperties.getMessage(ServiceUtil.resource, "serviceUtil.no_permission_to_run", locale) + ".";
            return ServiceUtil.returnError(errMsg);
        }

        String jobId = (String) context.get("jobId");
        Map fields = UtilMisc.toMap("jobId", jobId);

        GenericValue job = null;
        try {
            job = delegator.findByPrimaryKey("JobSandbox", fields);
            if (job != null) {
                job.set("cancelDateTime", UtilDateTime.nowTimestamp());
                job.set("statusId", "SERVICE_CANCELLED");
                job.store();
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            String errMsg = UtilProperties.getMessage(ServiceUtil.resource, "serviceUtil.unable_to_cancel_job", locale) + " : " + fields;
            return ServiceUtil.returnError(errMsg);
        }

        Timestamp cancelDate = job.getTimestamp("cancelDateTime");
        if (cancelDate != null) {
            Map result = ServiceUtil.returnSuccess();
            result.put("cancelDateTime", cancelDate);
            return result;
        } else {
            String errMsg = UtilProperties.getMessage(ServiceUtil.resource, "serviceUtil.unable_to_cancel_job", locale) + " : " + job;
            return ServiceUtil.returnError(errMsg);
        }
    }

    public static Map cancelJobRetries(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = getLocale(context);
        if (!security.hasPermission("SERVICE_INVOKE_ANY", userLogin)) {
            String errMsg = UtilProperties.getMessage(ServiceUtil.resource, "serviceUtil.no_permission_to_run", locale) + ".";
            return ServiceUtil.returnError(errMsg);
        }

        String jobId = (String) context.get("jobId");
        Map fields = UtilMisc.toMap("jobId", jobId);

        GenericValue job = null;
        try {
            job = delegator.findByPrimaryKey("JobSandbox", fields);
            if (job != null) {
                job.set("maxRetry", new Long(0));
                job.store();
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            String errMsg = UtilProperties.getMessage(ServiceUtil.resource, "serviceUtil.unable_to_cancel_job_retries", locale) + " : " + fields;
            return ServiceUtil.returnError(errMsg);
        }

        Timestamp cancelDate = job.getTimestamp("cancelDateTime");
        if (cancelDate != null) {
            return ServiceUtil.returnSuccess();
        } else {
            String errMsg = UtilProperties.getMessage(ServiceUtil.resource, "serviceUtil.unable_to_cancel_job_retries", locale) + " : " + job;
            return ServiceUtil.returnError(errMsg);
        }
    }

    public static GenericValue getUserLogin(DispatchContext dctx, Map context, String runAsUser) {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        GenericDelegator delegator = dctx.getDelegator();
        if (runAsUser != null) {
            try {
                GenericValue runAs = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", runAsUser));
                if (runAs != null) {
                    userLogin = runAs;
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
        }
        return userLogin;
    }

    private static Locale getLocale(Map context) {
        Locale locale = (Locale) context.get("locale");
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return locale;
    }
}
