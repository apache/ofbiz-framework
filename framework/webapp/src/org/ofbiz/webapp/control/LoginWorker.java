/*
 * Copyright 2001-2006 The Apache Software Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.ofbiz.webapp.control;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;
import javax.transaction.Transaction;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.transaction.GenericTransactionException;
import org.ofbiz.entity.transaction.TransactionUtil;

/**
 * Common Workers
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class LoginWorker {
    
    public final static String module = LoginWorker.class.getName();

    public static final String EXTERNAL_LOGIN_KEY_ATTR = "externalLoginKey";

    /** This Map is keyed by the randomly generated externalLoginKey and the value is a UserLogin GenericValue object */
    public static Map externalLoginKeys = new HashMap();
    
    public static String makeLoginUrl(PageContext pageContext) {
        return makeLoginUrl(pageContext, "checkLogin");
    }

    public static String makeLoginUrl(ServletRequest request) {
        return makeLoginUrl(request, "checkLogin");
    }
	
    public static String makeLoginUrl(PageContext pageContext, String requestName) {
        return makeLoginUrl(pageContext.getRequest(), requestName);
    }
    public static String makeLoginUrl(ServletRequest request, String requestName) {
        String queryString = null;

        Enumeration parameterNames = request.getParameterNames();

        while (parameterNames != null && parameterNames.hasMoreElements()) {
            String paramName = (String) parameterNames.nextElement();

            if (paramName != null) {
                if (queryString == null) queryString = paramName + "=" + request.getParameter(paramName);
                else queryString = queryString + "&" + paramName + "=" + request.getParameter(paramName);
            }
        }

        String loginUrl = "/" + requestName + "/" + UtilFormatOut.checkNull((String) request.getAttribute("_CURRENT_VIEW_"));

        if (queryString != null) loginUrl = loginUrl + "?" + UtilFormatOut.checkNull(queryString);

        return loginUrl;
    }
    
    /**
     * Gets (and creates if necessary) a key to be used for an external login parameter
     */
    public static String getExternalLoginKey(HttpServletRequest request) {
        //Debug.logInfo("Running getExternalLoginKey, externalLoginKeys.size=" + externalLoginKeys.size(), module);
        GenericValue userLogin = (GenericValue) request.getAttribute("userLogin");

        String externalKey = (String) request.getAttribute(EXTERNAL_LOGIN_KEY_ATTR);
        if (externalKey != null) return externalKey;

        HttpSession session = request.getSession();
        synchronized (session) {
            // if the session has a previous key in place, remove it from the master list
            String sesExtKey = (String) session.getAttribute(EXTERNAL_LOGIN_KEY_ATTR);
            if (sesExtKey != null) {
                externalLoginKeys.remove(sesExtKey);
            }

            //check the userLogin here, after the old session setting is set so that it will always be cleared
            if (userLogin == null) return "";

            //no key made yet for this request, create one
            while (externalKey == null || externalLoginKeys.containsKey(externalKey)) {
                externalKey = "EL" + Long.toString(Math.round(Math.random() * 1000000)) + Long.toString(Math.round(Math.random() * 1000000));
            }

            request.setAttribute(EXTERNAL_LOGIN_KEY_ATTR, externalKey);
            session.setAttribute(EXTERNAL_LOGIN_KEY_ATTR, externalKey);
            externalLoginKeys.put(externalKey, userLogin);
            return externalKey;
        }
    }

    public static void cleanupExternalLoginKey(HttpSession session) {
        String sesExtKey = (String) session.getAttribute(EXTERNAL_LOGIN_KEY_ATTR);
        if (sesExtKey != null) {
            externalLoginKeys.remove(sesExtKey);
        }
    }

    public static void setLoggedOut(String userLoginId, GenericDelegator delegator) {
        Transaction parentTx = null;
        boolean beganTransaction = false;

        try {
            try {
                parentTx = TransactionUtil.suspend();
            } catch (GenericTransactionException e) {
                Debug.logError(e, "Cannot suspend current transaction: " + e.getMessage(), module);
            }

            try {
                beganTransaction = TransactionUtil.begin();

                GenericValue userLogin = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", userLoginId));
                userLogin.set("hasLoggedOut", "Y");
                userLogin.store();
            } catch (GenericEntityException e) {
                String errMsg = "Unable to set logged out flag on UserLogin";
                Debug.logError(e, errMsg, module);
                try {
                    TransactionUtil.rollback(beganTransaction, errMsg, e);
                } catch (GenericTransactionException e2) {
                    Debug.logError(e2, "Could not rollback nested transaction: " + e.getMessage(), module);
                }
            } finally {
                try {
                    TransactionUtil.commit(beganTransaction);
                } catch (GenericTransactionException e) {
                    Debug.logError(e, "Could not commit nested transaction: " + e.getMessage(), module);
                }
            }
        } finally {
            // resume/restore parent transaction
            if (parentTx != null) {
                try {
                    TransactionUtil.resume(parentTx);
                    Debug.logVerbose("Resumed the parent transaction.", module);
                } catch (GenericTransactionException ite) {
                    Debug.logError(ite, "Cannot resume transaction: " + ite.getMessage(), module);
                }
            }
        }
    }
}
