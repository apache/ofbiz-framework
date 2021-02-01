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
package org.apache.ofbiz.service;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.security.Security;


/**
 * Generic Service Utility Class
 */
public final class ServiceUtil {

    private static final String MODULE = ServiceUtil.class.getName();
    private static final String RESOURCE = "ServiceErrorUiLabels";

    private ServiceUtil() { }

    /** A little short-cut method to check to see if a service returned an error */
    public static boolean isError(Map<String, ? extends Object> results) {
        return !(results == null || results.get(ModelService.RESPONSE_MESSAGE) == null)
                && ModelService.RESPOND_ERROR.equals(results.get(ModelService.RESPONSE_MESSAGE));
    }

    public static boolean isFailure(Map<String, ? extends Object> results) {
        return !(results == null || results.get(ModelService.RESPONSE_MESSAGE) == null)
                && ModelService.RESPOND_FAIL.equals(results.get(ModelService.RESPONSE_MESSAGE));
    }

    /** A little short-cut method to check to see if a service was successful (neither error or failed) */
    public static boolean isSuccess(Map<String, ? extends Object> results) {
        return !(ServiceUtil.isError(results) || ServiceUtil.isFailure(results));
    }

    /** A small routine used all over to improve code efficiency, make a result map with the message and the error response code */
    public static Map<String, Object> returnError(String errorMessage) {
        return returnProblem(ModelService.RESPOND_ERROR, errorMessage, null, null, null);
    }

    /** A small routine used all over to improve code efficiency, make a result map with the message and the error response code */
    public static Map<String, Object> returnError(String errorMessage, List<? extends Object> errorMessageList) {
        return returnProblem(ModelService.RESPOND_ERROR, errorMessage, errorMessageList, null, null);
    }

    /** A small routine used all over to improve code efficiency, make a result map with the message and the error response code */
    public static Map<String, Object> returnError(List<? extends Object> errorMessageList) {
        return returnProblem(ModelService.RESPOND_ERROR, null, errorMessageList, null, null);
    }

    public static Map<String, Object> returnFailure(String errorMessage) {
        return returnProblem(ModelService.RESPOND_FAIL, errorMessage, null, null, null);
    }

    public static Map<String, Object> returnFailure(List<? extends Object> errorMessageList) {
        return returnProblem(ModelService.RESPOND_FAIL, null, errorMessageList, null, null);
    }

    public static Map<String, Object> returnFailure() {
        return returnProblem(ModelService.RESPOND_FAIL, null, null, null, null);
    }

    /** A small routine used all over to improve code efficiency, make a result map with the message and the error response code,
     * also forwards any error messages from the nestedResult */
    public static Map<String, Object> returnError(String errorMessage, List<? extends Object> errorMessageList, Map<String, ? extends Object>
            errorMessageMap, Map<String, ? extends Object> nestedResult) {
        return returnProblem(ModelService.RESPOND_ERROR, errorMessage, errorMessageList, errorMessageMap, nestedResult);
    }

    public static Map<String, Object> returnProblem(String returnType, String errorMessage, List<? extends Object> errorMessageList,
                                                    Map<String, ? extends Object> errorMessageMap, Map<String, ? extends Object> nestedResult) {
        Map<String, Object> result = new HashMap<>();
        result.put(ModelService.RESPONSE_MESSAGE, returnType);
        if (errorMessage != null) {
            result.put(ModelService.ERROR_MESSAGE, errorMessage);
        }

        List<Object> errorList = new LinkedList<>();
        if (errorMessageList != null) {
            errorList.addAll(errorMessageList);
        }

        Map<String, Object> errorMap = new HashMap<>();
        if (errorMessageMap != null) {
            errorMap.putAll(errorMessageMap);
        }

        if (nestedResult != null) {
            if (nestedResult.get(ModelService.ERROR_MESSAGE) != null) {
                errorList.add(nestedResult.get(ModelService.ERROR_MESSAGE));
            }
            if (nestedResult.get(ModelService.ERROR_MESSAGE_LIST) != null) {
                errorList.addAll(UtilGenerics.cast(nestedResult.get(ModelService.ERROR_MESSAGE_LIST)));
            }
            if (nestedResult.get(ModelService.ERROR_MESSAGE_MAP) != null) {
                errorMap.putAll(UtilGenerics.cast(nestedResult.get(ModelService.ERROR_MESSAGE_MAP)));
            }
        }

        if (!errorList.isEmpty()) {
            result.put(ModelService.ERROR_MESSAGE_LIST, errorList);
        }
        if (!errorMap.isEmpty()) {
            result.put(ModelService.ERROR_MESSAGE_MAP, errorMap);
        }
        Debug.logError(result.toString(), MODULE);
        return result;
    }

    /** A small routine used all over to improve code efficiency, make a result map with the message and the success response code */
    public static Map<String, Object> returnSuccess(String successMessage) {
        return returnMessage(ModelService.RESPOND_SUCCESS, successMessage);
    }

    /** A small routine used all over to improve code efficiency, make a result map with the message and the success response code */
    public static Map<String, Object> returnSuccess() {
        return returnMessage(ModelService.RESPOND_SUCCESS, null);
    }

    /** A small routine used all over to improve code efficiency, make a result map with the message and the success response code */
    public static Map<String, Object> returnSuccess(List<String> successMessageList) {
        Map<String, Object> result = returnMessage(ModelService.RESPOND_SUCCESS, null);
        result.put(ModelService.SUCCESS_MESSAGE_LIST, successMessageList);
        return result;
    }

    /** A small routine to make a result map with the message and the response code
     * NOTE: This brings out some bad points to our message convention: we should be using a single message or message list
     *  and what type of message that is should be determined by the RESPONSE_MESSAGE (and there's another annoyance, it should be RESPONSE_CODE)
     */
    public static Map<String, Object> returnMessage(String code, String message) {
        Map<String, Object> result = new HashMap<>();
        if (code != null) {
            result.put(ModelService.RESPONSE_MESSAGE, code);
        }
        if (message != null) {
            result.put(ModelService.SUCCESS_MESSAGE, message);
        }
        return result;
    }

    /** A small routine used all over to improve code efficiency, get the partyId and does a security check
     *<b>security check</b>: userLogin partyId must equal partyId, or must have [secEntity][secOperation] permission
     */
    public static String getPartyIdCheckSecurity(GenericValue userLogin, Security security, Map<String, ? extends Object> context,
                                                 Map<String, Object> result, String secEntity, String secOperation) {
        return getPartyIdCheckSecurity(userLogin, security, context, result, secEntity, secOperation, null, null);
    }
    public static String getPartyIdCheckSecurity(GenericValue userLogin, Security security, Map<String, ? extends Object> context,
                                                 Map<String, Object> result, String secEntity, String secOperation, String adminSecEntity,
                                                 String adminSecOperation) {
        String partyId = (String) context.get("partyId");
        Locale locale = getLocale(context);
        if (UtilValidate.isEmpty(partyId)) {
            partyId = userLogin.getString("partyId");
        }

        // partyId might be null, so check it
        if (UtilValidate.isEmpty(partyId)) {
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            String errMsg = UtilProperties.getMessage(ServiceUtil.RESOURCE, "serviceUtil.party_id_missing", locale) + ".";
            result.put(ModelService.ERROR_MESSAGE, errMsg);
            return partyId;
        }

        // <b>security check</b>: userLogin partyId must equal partyId, or must have either of the two permissions
        if (!partyId.equals(userLogin.getString("partyId"))) {
            if (!security.hasEntityPermission(secEntity, secOperation, userLogin) && !(adminSecEntity != null && adminSecOperation != null
                    && security.hasEntityPermission(adminSecEntity, adminSecOperation, userLogin))) {
                result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
                String errMsg = UtilProperties.getMessage(ServiceUtil.RESOURCE, "serviceUtil.no_permission_to_operation", locale) + ".";
                result.put(ModelService.ERROR_MESSAGE, errMsg);
                return partyId;
            }
        }
        return partyId;
    }

    public static void setMessages(HttpServletRequest request, String errorMessage, String eventMessage, String defaultMessage) {
        if (UtilValidate.isNotEmpty(errorMessage)) {
            request.setAttribute("_ERROR_MESSAGE_", errorMessage);
        }

        if (UtilValidate.isNotEmpty(eventMessage)) {
            request.setAttribute("_EVENT_MESSAGE_", eventMessage);
        }

        if (UtilValidate.isEmpty(errorMessage) && UtilValidate.isEmpty(eventMessage) && UtilValidate.isNotEmpty(defaultMessage)) {
            request.setAttribute("_EVENT_MESSAGE_", defaultMessage);
        }

    }

    public static void getMessages(HttpServletRequest request, Map<String, ? extends Object> result, String defaultMessage) {
        getMessages(request, result, defaultMessage, null, null, null, null, null, null);
    }

    public static void getMessages(HttpServletRequest request, Map<String, ? extends Object> result, String defaultMessage,
            String msgPrefix, String msgSuffix, String errorPrefix, String errorSuffix, String successPrefix, String successSuffix) {
        String errorMessage = ServiceUtil.makeErrorMessage(result, msgPrefix, msgSuffix, errorPrefix, errorSuffix);
        String successMessage = ServiceUtil.makeSuccessMessage(result, msgPrefix, msgSuffix, successPrefix, successSuffix);
        setMessages(request, errorMessage, successMessage, defaultMessage);
    }

    public static String getErrorMessage(Map<String, ? extends Object> result) {
        StringBuilder errorMessage = new StringBuilder();

        if (result.get(ModelService.ERROR_MESSAGE) != null) {
            errorMessage.append((String) result.get(ModelService.ERROR_MESSAGE));
        }

        if (result.get(ModelService.ERROR_MESSAGE_LIST) != null) {
            List<? extends Object> errors = UtilGenerics.cast(result.get(ModelService.ERROR_MESSAGE_LIST));
            for (Object message: errors) {
                // NOTE: this MUST use toString and not cast to String because it may be a MessageString object
                String curMessage = message.toString();
                if (errorMessage.length() > 0) {
                    errorMessage.append(", ");
                }
                errorMessage.append(curMessage);
            }
        }

        return errorMessage.toString();
    }

    public static String makeErrorMessage(Map<String, ? extends Object> result, String msgPrefix, String msgSuffix,
                                          String errorPrefix, String errorSuffix) {
        if (result == null) {
            Debug.logWarning("A null result map was passed", MODULE);
            return null;
        }
        String errorMsg = (String) result.get(ModelService.ERROR_MESSAGE);
        List<? extends Object> errorMsgList = UtilGenerics.cast(result.get(ModelService.ERROR_MESSAGE_LIST));
        Map<String, ? extends Object> errorMsgMap = UtilGenerics.cast(result.get(ModelService.ERROR_MESSAGE_MAP));
        StringBuilder outMsg = new StringBuilder();

        if (errorMsg != null) {
            if (msgPrefix != null) {
                outMsg.append(msgPrefix);
            }
            outMsg.append(errorMsg);
            if (msgSuffix != null) {
                outMsg.append(msgSuffix);
            }
        }

        outMsg.append(makeMessageList(errorMsgList, msgPrefix, msgSuffix));

        if (errorMsgMap != null) {
            for (Map.Entry<String, ? extends Object> entry: errorMsgMap.entrySet()) {
                outMsg.append(msgPrefix);
                outMsg.append(entry.getKey());
                outMsg.append(": ");
                outMsg.append(entry.getValue());
                outMsg.append(msgSuffix);
            }
        }

        if (outMsg.length() > 0) {
            StringBuilder strBuf = new StringBuilder();

            if (errorPrefix != null) {
                strBuf.append(errorPrefix);
            }
            strBuf.append(outMsg.toString());
            if (errorSuffix != null) {
                strBuf.append(errorSuffix);
            }
            return strBuf.toString();
        }
        return null;
    }

    public static String makeSuccessMessage(Map<String, ? extends Object> result, String msgPrefix, String msgSuffix,
                                            String successPrefix, String successSuffix) {
        if (result == null) {
            return "";
        }
        String successMsg = (String) result.get(ModelService.SUCCESS_MESSAGE);
        List<? extends Object> successMsgList = UtilGenerics.cast(result.get(ModelService.SUCCESS_MESSAGE_LIST));
        StringBuilder outMsg = new StringBuilder();

        outMsg.append(makeMessageList(successMsgList, msgPrefix, msgSuffix));

        if (successMsg != null) {
            if (msgPrefix != null) {
                outMsg.append(msgPrefix);
            }
            outMsg.append(successMsg);
            if (msgSuffix != null) {
                outMsg.append(msgSuffix);
            }
        }

        if (outMsg.length() > 0) {
            StringBuilder strBuf = new StringBuilder();
            if (successPrefix != null) {
                strBuf.append(successPrefix);
            }
            strBuf.append(outMsg.toString());
            if (successSuffix != null) {
                strBuf.append(successSuffix);
            }
            return strBuf.toString();
        }
        return null;
    }

    public static String makeMessageList(List<? extends Object> msgList, String msgPrefix, String msgSuffix) {
        StringBuilder outMsg = new StringBuilder();
        if (UtilValidate.isNotEmpty(msgList)) {
            for (Object msg: msgList) {
                if (msg == null) {
                    continue;
                }
                String curMsg = msg.toString();
                if (msgPrefix != null) {
                    outMsg.append(msgPrefix);
                }
                outMsg.append(curMsg);
                if (msgSuffix != null) {
                    outMsg.append(msgSuffix);
                }
            }
        }
        return outMsg.toString();
    }

    /**
     * Takes the result of an invocation and extracts any error messages
     * and adds them to the targetList or targetMap. This will handle both List and String
     * error messags.
     * @param targetList    The List to add the error messages to
     * @param targetMap The Map to add any Map error messages to
     * @param callResult The result from an invocation
     */
    public static void addErrors(List<String> targetList, Map<String, Object> targetMap, Map<String, ? extends Object> callResult) {
        List<String> newList;
        Map<String, Object> errorMsgMap;

        //See if there is a single message
        if (callResult.containsKey(ModelService.ERROR_MESSAGE)) {
            targetList.add((String) callResult.get(ModelService.ERROR_MESSAGE));
        }

        //See if there is a message list
        if (callResult.containsKey(ModelService.ERROR_MESSAGE_LIST)) {
            newList = UtilGenerics.cast(callResult.get(ModelService.ERROR_MESSAGE_LIST));
            targetList.addAll(newList);
        }

        //See if there are an error message map
        if (callResult.containsKey(ModelService.ERROR_MESSAGE_MAP)) {
            errorMsgMap = UtilGenerics.cast(callResult.get(ModelService.ERROR_MESSAGE_MAP));
            targetMap.putAll(errorMsgMap);
        }
    }

    public static Map<String, Object> genericDateCondition(DispatchContext dctx, Map<String, ? extends Object> context) {
        Timestamp fromDate = (Timestamp) context.get("fromDate");
        Timestamp thruDate = (Timestamp) context.get("thruDate");
        Timestamp now = UtilDateTime.nowTimestamp();
        boolean reply = true;

        if (fromDate != null && fromDate.after(now)) {
            reply = false;
        }
        if (thruDate != null && thruDate.before(now)) {
            reply = false;
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("conditionReply", reply);
        return result;
    }

    public static GenericValue getUserLogin(DispatchContext dctx, Map<String, ? extends Object> context, String runAsUser) {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Delegator delegator = dctx.getDelegator();
        if (UtilValidate.isNotEmpty(runAsUser)) {
            try {
                GenericValue runAs = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", runAsUser).cache().queryOne();
                if (runAs != null) {
                    userLogin = runAs;
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
            }
        }
        return userLogin;
    }

    public static Locale getLocale(Map<String, ? extends Object> context) {
        Locale locale = (Locale) context.get("locale");
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return locale;
    }

    @SafeVarargs
    public static <T extends Object> Map<String, Object> makeContext(T... args) {
        if (args == null) {
            throw new IllegalArgumentException("args is null in makeContext, this would throw a NullPointerExcption.");
        }
        for (int i = 0; i < args.length; i += 2) {
            if (!(args[i] instanceof String)) {
                throw new IllegalArgumentException("Arg(" + i + "), value(" + args[i] + ") is not a string.");
            }
        }
        return UtilGenerics.cast(UtilMisc.toMap(args));
    }

    /**
     * Checks all incoming service attributes and look for fields with the same
     * name in the incoming map and copy those onto the outgoing map. Also
     * includes a userLogin if service requires one.
     * @param dispatcher
     * @param serviceName
     * @param fromMap
     * @param userLogin
     *            (optional) - will be added to the map if is required
     * @param timeZone
     * @param locale
     * @return filled Map or null on error
     * @throws GeneralServiceException
     */
    public static Map<String, Object> setServiceFields(LocalDispatcher dispatcher, String serviceName, Map<String, Object> fromMap,
            GenericValue userLogin, TimeZone timeZone, Locale locale) throws GeneralServiceException {
        Map<String, Object> outMap = new HashMap<>();

        ModelService modelService = null;
        try {
            modelService = dispatcher.getDispatchContext().getModelService(serviceName);
        } catch (GenericServiceException e) {
            String errMsg = "Could not get service definition for service name [" + serviceName + "]: ";
            Debug.logError(e, errMsg, MODULE);
            throw new GeneralServiceException(e);
        }
        outMap.putAll(modelService.makeValid(fromMap, ModelService.IN_PARAM, true, null, timeZone, locale));

        if (userLogin != null && modelService.isAuth()) {
            outMap.put("userLogin", userLogin);
        }

        return outMap;
    }

    public static String getResource() {
        return RESOURCE;
    }
}
