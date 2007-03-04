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

package org.ofbiz.securityext.thirdparty.truition;

import java.util.Collection;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Cookie;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.party.contact.ContactHelper;

public class TruitionCoReg {

    public static final String module =  TruitionCoReg.class.getName();
    public static final String logPrefix = "Truition Cookie Info: ";

    public static String truitionReg(HttpServletRequest req, HttpServletResponse resp) {
        HttpSession session = req.getSession();
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        StringBuffer cookieNameB = new StringBuffer();
        StringBuffer cookieValue = new StringBuffer();
        if (!truitionEnabled()) {
            return "success";
        }

        boolean cookieOk = false;
        if (userLogin != null) {
            cookieOk = TruitionCoReg.makeTruitionCookie(userLogin, cookieNameB, cookieValue);
        }

        // locate the domain/cookie name setting
        String domainName = UtilProperties.getPropertyValue("truition.properties", "truition.domain.name");
        String cookiePath = UtilProperties.getPropertyValue("truition.properties", "truition.cookie.path");
        String cookieName = UtilProperties.getPropertyValue("truition.properties", "truition.cookie.name");
        int time = (int) UtilProperties.getPropertyNumber("truition.properties", "truition.cookie.time");
        if (UtilValidate.isEmpty(domainName)) {
            Debug.logError("Truition is not properly configured; domainName missing; see truition.properties", module);
            return "error";
        }
        if (UtilValidate.isEmpty(cookiePath)) {
            Debug.logError("Truition is not properly configured; cookiePath missing; see truition.properties", module);
            return "error";
        }
        if (UtilValidate.isEmpty(cookieName)) {
            Debug.logError("Truition is not properly configured; cookieName missing; see truition.properties", module);
            return "error";
        }
        if (time == 0) {
            Debug.logError("Truition is not properly configured; cookieTime missing; see trution.properties", module);
            return "error";
        }

        // strip out CR/LF from cookie value
        String thisValue = cookieValue.toString();
        thisValue = thisValue.replaceAll("\n", "");
        thisValue = thisValue.replaceAll("\r", "");

        // create the cookie
        if (cookieOk) {
            try {
                Cookie tru = new Cookie(cookieName, URLEncoder.encode(thisValue, "UTF-8"));
                tru.setDomain(domainName);
                tru.setPath(cookiePath);
                tru.setMaxAge(time);
                resp.addCookie(tru);
                Debug.log("Set Truition Cookie [" + tru.getName() + "/" + tru.getDomain() + " @ " + tru.getPath() + "] - " + tru.getValue(), module);
            } catch (UnsupportedEncodingException e) {
                Debug.logError(e, module);
                return "error";
            }
        }
        return "success";
    }

    public static String truitionLogoff(HttpServletRequest req, HttpServletResponse resp) {
        // locate the domain/cookie name setting
        String domainName = UtilProperties.getPropertyValue("truition.properties", "truition.domain.name");
        String cookieName = UtilProperties.getPropertyValue("truition.properties", "truition.cookie.name");
        if (UtilValidate.isEmpty(domainName)) {
            Debug.logError("Truition is not properly configured; domainName missing; see truition.properties", module);
            return "error";
        }
        if (UtilValidate.isEmpty(cookieName)) {
            Debug.logError("Truition is not properly configured; cookieName missing; see truition.properties", module);
            return "error";
        }

        if (truitionEnabled()) {
        Cookie[] cookies = req.getCookies();
            for (int i = 0; i < cookies.length; i++) {
                if (cookieName.equals(cookies[i].getName())) {
                    cookies[i].setMaxAge(0);
                    resp.addCookie(cookies[i]);
                    Debug.log("Set truition cookie [" + cookieName + " to expire now.", module);
                }
            }
        }

        return "success";
    }

    public static String truitionRedirect(HttpServletRequest req, HttpServletResponse resp) {
        // redirect URL form field
        String redirectUrlName = UtilProperties.getPropertyValue("truition.properties", "truition.redirect.urlName");
        String redirectUrl = req.getParameter(redirectUrlName);
        Debug.log("Redirect to : " + redirectUrl, module);
        if (truitionEnabled() && redirectUrl != null) {
            try {
                resp.sendRedirect(redirectUrl);
            } catch (IOException e) {
                Debug.logError(e, module);
                req.setAttribute("_ERROR_MESSAGE_", e.getMessage());
                return "error";
            }

            Debug.log("Sending truition redirect - " + redirectUrl, module);
            return "redirect";
        }
        return "success";
    }

    public static boolean makeTruitionCookie(GenericValue userLogin, StringBuffer cookieName, StringBuffer cookieValue) {
        String domainName = UtilProperties.getPropertyValue("truition.properties", "truition.domain.name");
        String siteId = UtilProperties.getPropertyValue("truition.properties", "truition.siteId");

        if (UtilValidate.isEmpty(domainName)) {
            Debug.logError("Truition is not properly configured; domainName missing; see truition.properties!", module);
            return false;
        }
        if (UtilValidate.isEmpty(siteId)) {
            Debug.logError("Truition is not properly configured; siteId missing; see truition.properties!", module);
            return false;
        }

        // user login information
        String nickName = userLogin.getString("userLoginId");
        String password = userLogin.getString("currentPassword");
        String partyId = userLogin.getString("partyId");
        Debug.log(logPrefix + "nickName: " + nickName, module);
        Debug.log(logPrefix + "password: " + password, module);
        Debug.log(logPrefix + "partyId: " + partyId, module);

        GenericValue party = null;
        try {
            party = userLogin.getRelatedOne("Party");
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }

        if (party != null) {
            String title = null;
            String firstName = null;
            String lastName = null;
            if ("PERSON".equals(party.getString("partyTypeId"))) {
                GenericValue person = null;
                try {
                    person = party.getRelatedOne("Person");
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                }

                // first/last name
                if (person != null) {
                    title = person.getString("personalTitle");
                    firstName = person.getString("firstName");
                    lastName = person.getString("lastName");
                    if (title == null) {
                        title = "";
                    }
                }
                Debug.log(logPrefix + "title: " + title, module);
                Debug.log(logPrefix + "firstName: " + firstName, module);
                Debug.log(logPrefix + "lastName: " + lastName, module);

                // email address
                String emailAddress = null;
                Collection emCol = ContactHelper.getContactMech(party, "PRIMARY_EMAIL", "EMAIL_ADDRESS", false);
                if (UtilValidate.isEmpty(emCol)) {
                    emCol = ContactHelper.getContactMech(party, null, "EMAIL_ADDRESS", false);
                }
                if (!UtilValidate.isEmpty(emCol)) {
                    GenericValue emVl = (GenericValue) emCol.iterator().next();
                    if (emVl != null) {
                        emailAddress = emVl.getString("infoString");
                    }
                } else {
                    emailAddress = "";
                }
                Debug.log(logPrefix + "emailAddress: " + emailAddress, module);

                // shipping address
                String address1 = null;
                String address2 = null;
                String city = null;
                String state = null;
                String zipCode = null;
                String country = null;
                Collection adCol = ContactHelper.getContactMech(party, "SHIPPING_LOCATION", "POSTAL_ADDRESS", false);
                if (UtilValidate.isEmpty(adCol)) {
                    adCol = ContactHelper.getContactMech(party, null, "POSTAL_ADDRESS", false);
                }
                if (!UtilValidate.isEmpty(adCol)) {
                    GenericValue adVl = (GenericValue) adCol.iterator().next();
                    if (adVl != null) {
                        GenericValue addr = null;
                        try {
                            addr = adVl.getDelegator().findByPrimaryKey("PostalAddress", UtilMisc.toMap("contactMechId",
                                    adVl.getString("contactMechId")));
                        } catch (GenericEntityException e) {
                            Debug.logError(e, module);
                        }
                        if (addr != null) {
                            address1 = addr.getString("address1");
                            address2 = addr.getString("address2");
                            city = addr.getString("city");
                            state = addr.getString("stateProvinceGeoId");
                            zipCode = addr.getString("postalCode");
                            country = addr.getString("countryGeoId");
                            if (address2 == null) {
                                address2 = "";
                            }
                        }
                    }
                }
                Debug.log(logPrefix + "address1: " + address1, module);
                Debug.log(logPrefix + "address2: " + address2, module);
                Debug.log(logPrefix + "city: " + city, module);
                Debug.log(logPrefix + "state: " + state, module);
                Debug.log(logPrefix + "zipCode: " + zipCode, module);
                Debug.log(logPrefix + "country: " + country, module);

                // phone number
                String phoneNumber = null;
                Collection phCol = ContactHelper.getContactMech(party, "PHONE_HOME", "TELECOM_NUMBER", false);
                if (UtilValidate.isEmpty(phCol)) {
                    phCol = ContactHelper.getContactMech(party, null, "TELECOM_NUMBER", false);
                }
                if (!UtilValidate.isEmpty(phCol)) {
                    GenericValue phVl = (GenericValue) phCol.iterator().next();
                    if (phVl != null) {
                        GenericValue tele = null;
                        try {
                            tele = phVl.getDelegator().findByPrimaryKey("TelecomNumber", UtilMisc.toMap("contactMechId",
                                    phVl.getString("contactMechId")));
                        } catch (GenericEntityException e) {
                            Debug.logError(e, module);
                        }
                        if (tele != null) {
                            phoneNumber = ""; // reset the string
                            String cc = tele.getString("countryCode");
                            String ac = tele.getString("areaCode");
                            String nm = tele.getString("contactNumber");
                            if (UtilValidate.isNotEmpty(cc)) {
                                phoneNumber = phoneNumber + cc + "-";
                            }
                            if (UtilValidate.isNotEmpty(ac)) {
                                phoneNumber = phoneNumber + ac + "-";
                            }
                            phoneNumber = phoneNumber + nm;
                        } else {
                            phoneNumber = "";
                        }
                    }
                }
                Debug.log(logPrefix + "phoneNumber: " + phoneNumber, module);

                int retCode = -1;

                if (lastName != null && address1 != null) {
                    retCode = edeal.coreg.EdCoReg.ed_create_cookie_nvp(nickName, "pwd", title, firstName,
                        lastName, emailAddress, address1, address2, city, state, zipCode, country, phoneNumber,
                        siteId, cookieName, cookieValue, "", "", "", partyId, "", "");
                }

                if (retCode < 0) {
                    Debug.logError("EDeal cookie not set; API return code: " + retCode, module);
                    return false;
                } else {
                    Debug.logInfo("EDeal cookie success; API return code: " + retCode, module);
                }
            } else {
                Debug.logError("Truition requires a Person to be logged in. First/Last name required!", module);
                return false;
            }
        }
        return true;
    }

    public static boolean truitionEnabled() {
        return "Y".equalsIgnoreCase(UtilProperties.getPropertyValue("truition.properties", "truition.enabled", "N"));
    }
}
