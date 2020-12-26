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

package org.apache.ofbiz.securityext.login;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.party.contact.ContactHelper;
import org.apache.ofbiz.product.product.ProductEvents;
import org.apache.ofbiz.product.store.ProductStoreWorker;
import org.apache.ofbiz.security.SecurityUtil;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.webapp.control.LoginWorker;
import org.apache.ofbiz.webapp.website.WebSiteWorker;

/**
 * LoginEvents - Events for UserLogin and Security handling.
 */
public class LoginEvents {

    private static final String MODULE = LoginEvents.class.getName();
    private static final String RESOURCE = "SecurityextUiLabels";
    public static final String USERNAME_COOKIE_NAME = "OFBiz.Username";
    /**
     * Save USERNAME and PASSWORD for use by auth pages even if we start in non-auth pages.
     * @param request The HTTP request object for the current JSP or Servlet request.
     * @param response The HTTP response object for the current JSP or Servlet request.
     * @return String
     */
    public static String saveEntryParams(HttpServletRequest request, HttpServletResponse response) {
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
        HttpSession session = request.getSession();
        Delegator delegator = (Delegator) request.getAttribute("delegator");

        // save entry login parameters if we don't have a valid login object
        if (userLogin == null) {

            String username = request.getParameter("USERNAME");
            String password = request.getParameter("PASSWORD");

            if ((username != null) && ("true".equalsIgnoreCase(EntityUtilProperties.getPropertyValue("security", "username.lowercase", delegator)))) {
                username = username.toLowerCase(Locale.getDefault());
            }
            if ((password != null) && ("true".equalsIgnoreCase(EntityUtilProperties.getPropertyValue("security", "password.lowercase", delegator)))) {
                password = password.toLowerCase(Locale.getDefault());
            }

            // save parameters into the session - so they can be used later, if needed
            if (username != null) {
                session.setAttribute("USERNAME", username);
            }
            if (password != null) {
                session.setAttribute("PASSWORD", password);
            }

        } else {
            // if the login object is valid, remove attributes
            session.removeAttribute("USERNAME");
            session.removeAttribute("PASSWORD");
        }

        return "success";
    }

    /**
     * The user forgot his/her password.  This will call showPasswordHint, emailPassword or simply returns "success" in case
     * no operation has been specified.
     * @param request The HTTPRequest object for the current request
     * @param response The HTTPResponse object for the current request
     * @return String specifying the exit status of this event
     */
    public static String forgotPassword(HttpServletRequest request, HttpServletResponse response) {
        if (UtilValidate.isNotEmpty(request.getParameter("GET_PASSWORD_HINT"))
                || UtilValidate.isNotEmpty(request.getParameter("GET_PASSWORD_HINT.x"))) {
            return showPasswordHint(request, response);
        } else if ((UtilValidate.isNotEmpty(request.getParameter("EMAIL_PASSWORD")))
                || (UtilValidate.isNotEmpty(request.getParameter("EMAIL_PASSWORD.x")))) {
            return emailPasswordRequest(request, response);
        }

        return "success";
    }

    /** Show the password hint for the userLoginId specified in the request object.
     *@param request The HTTPRequest object for the current request
     *@param response The HTTPResponse object for the current request
     *@return String specifying the exit status of this event
     */
    public static String showPasswordHint(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");

        String userLoginId = request.getParameter("USERNAME");
        String errMsg = null;

        if ((userLoginId != null) && ("true".equals(EntityUtilProperties.getPropertyValue("security", "username.lowercase", delegator)))) {
            userLoginId = userLoginId.toLowerCase(Locale.getDefault());
        }

        if (UtilValidate.isEmpty(userLoginId)) {
            // the password was incomplete
            errMsg = UtilProperties.getMessage(RESOURCE, "loginevents.username_was_empty_reenter", UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }

        GenericValue supposedUserLogin = null;
        String passwordHint = null;
        try {
            supposedUserLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", userLoginId).queryOne();
        } catch (GenericEntityException gee) {
            Debug.logWarning(gee, "", MODULE);
        }
        if (supposedUserLogin != null) {
            passwordHint = supposedUserLogin.getString("passwordHint");
        }

        if (supposedUserLogin == null || UtilValidate.isEmpty(passwordHint)) {
            // the Username was not found or there was no hint for the Username
            errMsg = UtilProperties.getMessage(RESOURCE, "loginevents.no_password_hint_specified_try_password_emailed", UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }

        Map<String, String> messageMap = UtilMisc.toMap("passwordHint", passwordHint);
        errMsg = UtilProperties.getMessage(RESOURCE, "loginevents.password_hint_is", messageMap, UtilHttp.getLocale(request));
        request.setAttribute("_EVENT_MESSAGE_", errMsg);
        return "auth";
    }

    /**
     * event to send an email with a link to change password
     * @param request The HTTPRequest object for the current request
     * @param response The HTTPResponse object for the current request
     * @return String specifying the exit status of this event
     */
    public static String emailPasswordRequest(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        String productStoreId = ProductStoreWorker.getProductStoreId(request);
        String defaultScreenLocation = "component://securityext/widget/EmailSecurityScreens.xml#PasswordEmail";

        // get userloginId
        String userLoginId = request.getParameter("USERNAME");
        if (UtilValidate.isEmpty(userLoginId)) {
            String errMsg = UtilProperties.getMessage(RESOURCE, "loginevents.username_was_empty_reenter",
                    UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }
        if (UtilValidate.isNotEmpty(request.getParameter("token"))) {
            return "success";
        }

        GenericValue userLogin;
        try {
            // test if user exist and is active
            userLogin = EntityQuery.use(delegator)
                    .from("UserLogin")
                    .where("userLoginId", userLoginId)
                    .queryOne();
            if (userLogin == null || "N".equals(userLogin.getString("enabled"))) {
                Debug.logError("userlogin uknown or disabled " + userLogin, MODULE);
                //giving a "sent email to associated email-address" response, to suppress feedback on in-/valid usernames
                String errMsg = UtilProperties.getMessage(RESOURCE, "loginevents.new_password_sent_check_email",
                        UtilHttp.getLocale(request));
                request.setAttribute("_EVENT_MESSAGE_", errMsg);
                return "success";
            }

            // check login is associated to a party
            GenericValue userParty = userLogin.getRelatedOne("Party", false);
            if (userParty == null) {
                String errMsg = UtilProperties.getMessage(RESOURCE, "loginevents.username_not_found_reenter", UtilHttp.getLocale(request));
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                return "error";
            }

            // check there is an email to send to
            List<GenericValue> contactMechs = (List<GenericValue>) ContactHelper.getContactMechByPurpose(userParty, "PRIMARY_EMAIL", false);
            if (UtilValidate.isEmpty(contactMechs)) {
                // the email was not found
                String errMsg = UtilProperties.getMessage(RESOURCE,
                        "loginevents.no_primary_email_address_set_contact_customer_service",
                        UtilHttp.getLocale(request));
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                return "error";
            }
            String emails = contactMechs.stream()
                    .map(email -> email.getString("infoString"))
                    .collect(Collectors.joining(","));

            //Generate a JWT with default retention time
            String jwtToken = SecurityUtil.generateJwtToAuthenticateUserLogin(delegator, userLoginId);

            // get the ProductStore email settings
            GenericValue productStoreEmail = null;
            try {
                productStoreEmail = EntityQuery.use(delegator).from("ProductStoreEmailSetting").where("productStoreId",
                        productStoreId, "emailType", "PRDS_PWD_RETRIEVE").queryOne();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Problem getting ProductStoreEmailSetting", MODULE);
            }

            String bodyScreenLocation = null;
            if (productStoreEmail != null) {
                bodyScreenLocation = productStoreEmail.getString("bodyScreenLocation");
            }
            if (UtilValidate.isEmpty(bodyScreenLocation)) {
                bodyScreenLocation = defaultScreenLocation;
            }

            // set the needed variables in new context
            Map<String, Object> bodyParameters = new HashMap<>();
            bodyParameters.put("token", jwtToken);
            bodyParameters.put("locale", UtilHttp.getLocale(request));
            bodyParameters.put("userLogin", userLogin);
            bodyParameters.put("productStoreId", productStoreId);

            Map<String, Object> serviceContext = new HashMap<>();
            serviceContext.put("bodyScreenUri", bodyScreenLocation);
            serviceContext.put("bodyParameters", bodyParameters);
            serviceContext.put("webSiteId", WebSiteWorker.getWebSiteId(request));
            if (productStoreEmail != null) {
                serviceContext.put("subject", productStoreEmail.getString("subject"));
                serviceContext.put("sendFrom", productStoreEmail.get("fromAddress"));
                serviceContext.put("sendCc", productStoreEmail.get("ccAddress"));
                serviceContext.put("sendBcc", productStoreEmail.get("bccAddress"));
                serviceContext.put("contentType", productStoreEmail.get("contentType"));
            } else {
                GenericValue emailTemplateSetting = null;
                try {
                    emailTemplateSetting = EntityQuery.use(delegator).from("EmailTemplateSetting").where("emailTemplateSettingId",
                            "EMAIL_PASSWORD").cache().queryOne();
                } catch (GenericEntityException e) {
                    Debug.logError(e, MODULE);
                }
                if (emailTemplateSetting != null) {
                    String subject = emailTemplateSetting.getString("subject");
                    subject = FlexibleStringExpander.expandString(subject, UtilMisc.toMap("userLoginId", userLoginId));
                    serviceContext.put("subject", subject);
                    serviceContext.put("sendFrom", emailTemplateSetting.get("fromAddress"));
                } else {
                    serviceContext.put("subject", UtilProperties.getMessage(RESOURCE, "loginservices.password_reminder_subject",
                            UtilMisc.toMap("userLoginId", userLoginId), UtilHttp.getLocale(request)));
                    serviceContext.put("sendFrom", EntityUtilProperties.getPropertyValue("general", "defaultFromEmailAddress", delegator));
                }
            }
            serviceContext.put("sendTo", emails);
            serviceContext.put("partyId", userParty.getString("partyId"));

            Map<String, Object> result = dispatcher.runSync("sendMailHiddenInLogFromScreen", serviceContext);
            if (ServiceUtil.isError(result)) {
                String errorMessage = ServiceUtil.getErrorMessage(result);
                request.setAttribute("_ERROR_MESSAGE_", errorMessage);
                Debug.logError(errorMessage, MODULE);
                return "error";
            }

            if (ServiceUtil.isError(result)) {
                Map<String, Object> messageMap = UtilMisc.toMap("errorMessage", result.get(ModelService.ERROR_MESSAGE));
                String errMsg = UtilProperties.getMessage(RESOURCE, "loginevents.error_unable_email_password_contact_customer_service_errorwas",
                        messageMap, UtilHttp.getLocale(request));
                request.setAttribute("_ERROR_MESSAGE_", errMsg);
                return "error";
            }
        } catch (GeneralException e) {
            Debug.logWarning(e, "", MODULE);
            String errMsg = UtilProperties.getMessage(RESOURCE, "loginevents.error_unable_email_password_contact_customer_service",
                    UtilHttp.getLocale(request));
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }

        String msg = UtilProperties.getMessage(RESOURCE, "loginevents.new_password_sent_check_email", UtilHttp.getLocale(request));
        request.setAttribute("_EVENT_MESSAGE_", msg);
        return "success";
    }

    public static String storeCheckLogin(HttpServletRequest request, HttpServletResponse response) {
        String responseString = LoginWorker.checkLogin(request, response);
        if ("error".equals(responseString)) {
            return responseString;
        }
        // if we are logged in okay, do the check store customer role
        return ProductEvents.checkStoreCustomerRole(request, response);
    }

    public static String storeLogin(HttpServletRequest request, HttpServletResponse response) throws UnsupportedEncodingException {
        String responseString = LoginWorker.login(request, response);
        if (!"success".equals(responseString)) {
            return responseString;
        }
        // if we logged in okay, do the check store customer role
        return ProductEvents.checkStoreCustomerRole(request, response);
    }
}
