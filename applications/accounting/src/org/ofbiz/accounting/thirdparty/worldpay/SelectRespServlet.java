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
package org.ofbiz.accounting.thirdparty.worldpay;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpSession;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.webapp.view.JPublishWrapper;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.transaction.GenericTransactionException;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.order.order.OrderChangeHelper;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceDispatcher;

import com.worldpay.select.SelectDefs;
import com.worldpay.select.merchant.SelectServlet;
import com.worldpay.select.merchant.SelectServletRequest;
import com.worldpay.select.merchant.SelectServletResponse;

/**
 * WorldPay Select Pro Response Servlet
 */
public class SelectRespServlet extends SelectServlet implements SelectDefs {
    
    public static final String module = SelectRespServlet.class.getName();
    protected JPublishWrapper jp = null;

    protected void doRequest(SelectServletRequest request, SelectServletResponse response) throws ServletException, IOException {
        Debug.logInfo("Response received from worldpay..", module);
                
        String localLocaleStr = request.getParameter("M_localLocale");
        String webSiteId = request.getParameter("M_webSiteId");
        String delegatorName = request.getParameter("M_delegatorName");
        String dispatchName = request.getParameter("M_dispatchName");
        String userLoginId = request.getParameter("M_userLoginId");
        String confirmTemplate = request.getParameter("M_confirmTemplate");
        
        // get the ServletContext
        ServletContext context = (ServletContext) request.getAttribute("servletContext");
        if (this.jp == null) {
            this.jp = (JPublishWrapper) context.getAttribute("jpublishWrapper");
            if (this.jp == null) {
                this.jp = new JPublishWrapper(context);
            }
        }                
        
        // get the delegator
        GenericDelegator delegator = GenericDelegator.getGenericDelegator(delegatorName);
        
        // get the dispatcher
        ServiceDispatcher serviceDisp = ServiceDispatcher.getInstance(dispatchName, delegator);
        DispatchContext dctx = serviceDisp.getLocalContext(dispatchName);
        LocalDispatcher dispatcher = dctx.getDispatcher();  
        
        // get the userLogin
        GenericValue userLogin = null;  
        try {
            userLogin = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", userLoginId));      
        } catch (GenericEntityException e) {
            Debug.logError(e, "Cannot get admin UserLogin entity", module);
            callError(request);
        }
        
        // get the client locale
        List localeSplit = StringUtil.split(localLocaleStr, "_");
        Locale localLocale = new Locale((String) localeSplit.get(0), (String) localeSplit.get(1));
                
        // get the properties file       
        String configString = null;
        try {
            GenericValue webSitePayment = delegator.findByPrimaryKey("WebSitePaymentSetting", UtilMisc.toMap("webSiteId", webSiteId, "paymentMethodTypeId", "EXT_WORLDPAY"));
            if (webSitePayment != null)
                configString = webSitePayment.getString("paymentConfiguration");
        } catch (GenericEntityException e) {
            Debug.logWarning(e, "Cannot find webSitePayment Settings", module);
        }
        if (configString == null)
        configString = "payment.properties";    
        Debug.logInfo("Got the payment configuration", module);    
        
        String orderId = request.getParameter(SelectDefs.SEL_cartId);
        String authAmount = request.getParameter(SelectDefs.SEL_authAmount);
        String transStatus = request.getParameter(SelectDefs.SEL_transStatus);
        
        // get the order header
        GenericValue orderHeader = null;
        try {
            orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
        } catch (GenericEntityException e) {
            Debug.logError(e, "Cannot get the order header for the returned orderId", module);
            callError(request);
        }
        
        // the order total MUST match the auth amount or we do not process
        Double wpTotal = new Double(authAmount);
        Double orderTotal = orderHeader != null ? orderHeader.getDouble("grandTotal") : null;
        if (orderTotal != null && wpTotal != null) {
            if (orderTotal.doubleValue() != wpTotal.doubleValue()) {
                Debug.logError("AuthAmount (" + wpTotal + ") does not match OrderTotal (" + orderTotal + ")", module);
                callError(request);
            }                
        }
        
        // store some stuff for calling existing events
        HttpSession session = request.getSession(true);
        session.setAttribute("userLogin", userLogin);
        
        request.setAttribute("delegator", delegator);
        request.setAttribute("dispatcher", dispatcher);
        request.setAttribute("orderId", orderId);
        request.setAttribute("notifyEmail", request.getParameter("M_notifyEmail"));
        request.setAttribute("confirmEmail", request.getParameter("M_confirmEmail"));        
        request.setAttribute("_CONTROL_PATH_", request.getParameter("M_controlPath"));
                
        // attempt to start a transaction
        boolean beganTransaction = false;
        try {
            beganTransaction = TransactionUtil.begin();
        } catch (GenericTransactionException gte) {
            Debug.logError(gte, "Unable to begin transaction", module);
        }                
        
        boolean okay = false;
        if (transStatus.equalsIgnoreCase("Y")) {
            // order was approved
            Debug.logInfo("Order #" + orderId + " approved", module);
            okay = OrderChangeHelper.approveOrder(dispatcher, userLogin, orderId);                  
        } else {
            // order was cancelled
            Debug.logInfo("Order #" + orderId + " cancelled", module);
            okay = OrderChangeHelper.cancelOrder(dispatcher, userLogin, orderId);
        }
        
        if (okay) {        
            // set the payment preference
            okay = setPaymentPreferences(delegator, userLogin, orderId, request);
        }
        
        if (okay) {                
            try {
                TransactionUtil.commit(beganTransaction);
            } catch (GenericTransactionException gte) {
                Debug.logError(gte, "Unable to commit transaction", module);
            }
        } else {
            try {
                TransactionUtil.rollback(beganTransaction, "Failure in Worldpay callback/response processing.", null);
            } catch (GenericTransactionException gte) {
                Debug.logError(gte, "Unable to rollback transaction", module);
            }
        }
        
        // attempt to release the offline hold on the order (workflow)
        OrderChangeHelper.releaseInitialOrderHold(dispatcher, orderId); 
                        
        // call the email confirm service
        Map emailContext = UtilMisc.toMap("orderId", orderId);
        try {
            Map emailResult = dispatcher.runSync("sendOrderConfirmation", emailContext);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Problems sending email confirmation", module);
        }                                 
                                                              
        // set up the output stream for the response
        response.setContentType("text/html");
        ServletOutputStream out = response.getOutputStream();  
        String content = "Error getting confirm content";
        if (confirmTemplate != null) {                                    
            // render the thank-you / confirm page            
            try {
                content = jp.render(confirmTemplate, request, response);
            } catch (GeneralException e) {                
                Debug.logError(e, "Trouble rendering confirm page", module);
            }                         
        }
        out.println(content);
        out.flush();                                                  
    }
               
    private boolean setPaymentPreferences(GenericDelegator delegator, GenericValue userLogin, String orderId, ServletRequest request) {
        List paymentPrefs = null;
        boolean okay = true;
        try {
            Map paymentFields = UtilMisc.toMap("orderId", orderId, "statusId", "PAYMENT_NOT_RECEIVED");
            paymentPrefs = delegator.findByAnd("OrderPaymentPreference", paymentFields);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Cannot get payment preferences for order #" + orderId, module);
        }
        if (paymentPrefs != null && paymentPrefs.size() > 0) {
            Iterator i = paymentPrefs.iterator();            
            while (okay && i.hasNext()) {
                GenericValue pref = (GenericValue) i.next();
                okay = setPaymentPreference(pref, userLogin, request);
            }
        }
        return okay;
    }
        
    private boolean setPaymentPreference(GenericValue paymentPreference, GenericValue userLogin, ServletRequest request) {
        String transId = request.getParameter(SelectDefs.SEL_transId);       
        String transTime = request.getParameter(SelectDefs.SEL_transTime);
        String transStatus = request.getParameter(SelectDefs.SEL_transStatus);
        String avsCode = request.getParameter("AVS");  // why is this not in SelectDefs??
        String authCode = request.getParameter(SelectDefs.SEL_authCode);
        String authAmount = request.getParameter(SelectDefs.SEL_authAmount); 
        String rawAuthMessage = request.getParameter(SelectDefs.SEL_rawAuthMessage);
        
        // Need these for create payment service
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        
        if (transStatus.equalsIgnoreCase("Y")) {
            paymentPreference.set("authCode", authCode);
            paymentPreference.set("statusId", "PAYMENT_RECEIVED");
        } else {
            paymentPreference.set("statusId", "PAYMENT_CANCELLED");
        }
        Long transTimeLong = new Long(transTime);
        java.sql.Timestamp authDate = new java.sql.Timestamp(transTimeLong.longValue());
        
        paymentPreference.set("avsCode", avsCode);
        paymentPreference.set("authRefNum", transId);
        paymentPreference.set("authDate", authDate);
        paymentPreference.set("authFlag", transStatus);
        paymentPreference.set("authMessage", rawAuthMessage);
        paymentPreference.set("maxAmount", new Double(authAmount));
        
        // create a payment record too -- this method does not store the object so we must here
        Map results = null;
        try {
            results = dispatcher.runSync("createPaymentFromPreference", UtilMisc.toMap("userLogin", userLogin, 
                    "orderPaymentPreferenceId", paymentPreference.get("orderPaymentPreferenceId"), "comments", "Payment received via WorldPay"));
        } catch (GenericServiceException e) {
            Debug.logError(e, "Failed to execute service createPaymentFromPreference", module);
            request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
            return false;
        }

        if ((results == null) || (results.get(ModelService.RESPONSE_MESSAGE).equals(ModelService.RESPOND_ERROR))) {
            Debug.logError((String) results.get(ModelService.ERROR_MESSAGE), module);
            request.setAttribute("_ERROR_MESSAGE_", (String) results.get(ModelService.ERROR_MESSAGE));
            return false;
        }
        
        try {
            paymentPreference.store();
            paymentPreference.getDelegator().create(paymentPreference);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Cannot set payment preference/payment info", module);
            return false;
        } 
        return true;                  
    }  
    
    private void callError(ServletRequest request) throws ServletException {
        Enumeration e = request.getParameterNames();
        Debug.logError("###### SelectRespServlet Error:", module);
        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();
            String value = request.getParameter(name);
            Debug.logError("### Parameter: " + name + " => " + value, module);  
        }
        Debug.logError("###### The order was not processed!", module);
        throw new ServletException("Order Error");
    }
}
